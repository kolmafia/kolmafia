package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;

/**
 * Determines whether a partial equipment branch can still improve on the current best result.
 *
 * <p>The bound combines optimistic per-slot contributions with copy-aware interchangeable groups,
 * delegating capped and signed numeric expressions to {@link MaximizerNumericBound}. Unsupported
 * interactions decline bound construction so the Maximizer falls back to exact traversal.
 */
final class MaximizerBranchBound {
  private static final List<Slot> OUTFIT_SLOTS =
      List.of(Slot.CONTAINER, Slot.HAT, Slot.SHIRT, Slot.PANTS);

  enum Stage {
    ROOT,
    OUTFIT,
    FAMILIAR_ITEM,
    CONTAINER,
    ACCESSORY,
    HAT,
    SHIRT,
    PANTS,
    HOLSTER,
    WEAPON,
    OFFHAND
  }

  enum CapacityMode {
    UNRESOLVED_SLOTS,
    FIXED
  }

  // Whole-equipment bonuses whose score can depend on the combined equipped set.
  enum Category {
    HOBO_POWER,
    SMITHSNESS,
    BRIMSTONE,
    CLOATHING,
    SLIME_HATES_IT,
    MCHUGELARGE
  }

  record InterchangeableSlotGroup(
      List<Slot> slots,
      List<CheckedItem> candidates,
      Map<Integer, Integer> copiesByItem,
      Map<Integer, Modifiers> projectionDeltas,
      CapacityMode capacityMode) {
    InterchangeableSlotGroup {
      slots = List.copyOf(slots);
      candidates = List.copyOf(candidates);
      copiesByItem = Map.copyOf(copiesByItem);
      projectionDeltas = Map.copyOf(projectionDeltas);
    }

    int capacity() {
      return this.slots.size();
    }

    int remainingCapacity(MaximizerSpeculation speculation) {
      if (this.capacityMode == CapacityMode.FIXED) return this.capacity();
      return (int)
          this.slots.stream().filter(slot -> speculation.equipment.get(slot) == null).count();
    }
  }

  private record CategoryBounds(
      double upper, double[] byCategory, double[][] jointBrimstoneCloathingUpper) {}

  record OutfitEnvelope(double upper, Map<Slot, Map<Integer, Double>> bySlot) {
    double upper(MaximizerSpeculation speculation) {
      double result = this.upper;
      for (var entry : this.bySlot.entrySet()) {
        AdventureResult selected = speculation.equipment.get(entry.getKey());
        if (selected == null) continue;
        var bounds = entry.getValue();
        result =
            Math.min(
                result,
                Math.max(
                    bounds.getOrDefault(-1, 0.0), bounds.getOrDefault(selected.getItemId(), 0.0)));
      }
      return result;
    }
  }

  private record SynergyCombination(int[] itemIds, double upper) {}

  record SynergyEnvelope(double upper, List<SynergyCombination> combinations) {
    double upper(MaximizerSpeculation baseline, SlotList<CheckedItem> possibles) {
      var selected = new java.util.HashSet<Integer>();
      for (var item : baseline.equipment.values()) {
        if (item != null && item.getItemId() > 0) selected.add(item.getItemId());
      }
      var available = new java.util.HashSet<>(selected);
      for (var slot : baseline.equipment.keySet()) {
        for (var item : candidates(possibles, slot)) {
          if (item.getItemId() > 0) available.add(item.getItemId());
        }
      }
      long freeAccessories =
          SlotSet.ACCESSORY_SLOTS.stream()
              .filter(slot -> baseline.equipment.get(slot) == null)
              .count();
      int freeWeapons =
          (baseline.equipment.get(Slot.WEAPON) == null ? 1 : 0)
              + (baseline.equipment.get(Slot.OFFHAND) == null ? 1 : 0);

      double result = 0.0;
      for (var combination : this.combinations) {
        if (synergyFeasible(
            combination.itemIds(), baseline, selected, available, freeAccessories, freeWeapons)) {
          result = Math.max(result, combination.upper());
        }
      }
      return result;
    }
  }

  private static final java.util.EnumSet<DoubleModifier> CONTEXTUAL_MODIFIERS =
      java.util.EnumSet.of(
          DoubleModifier.FAMILIAR_WEIGHT,
          DoubleModifier.HIDDEN_FAMILIAR_WEIGHT,
          DoubleModifier.FAMILIAR_WEIGHT_PCT,
          DoubleModifier.FAIRY_WEIGHT,
          DoubleModifier.FAIRY_EFFECTIVENESS,
          DoubleModifier.LEPRECHAUN_WEIGHT,
          DoubleModifier.LEPRECHAUN_EFFECTIVENESS,
          DoubleModifier.VOLLEYBALL_WEIGHT,
          DoubleModifier.VOLLEYBALL_EFFECTIVENESS,
          DoubleModifier.SOMBRERO_WEIGHT,
          DoubleModifier.SOMBRERO_EFFECTIVENESS,
          DoubleModifier.SOMBRERO_BONUS,
          DoubleModifier.EXPERIENCE,
          DoubleModifier.MONSTER_LEVEL,
          DoubleModifier.MONSTER_LEVEL_PERCENT,
          DoubleModifier.MUS_EXPERIENCE,
          DoubleModifier.MYS_EXPERIENCE,
          DoubleModifier.MOX_EXPERIENCE,
          DoubleModifier.MUS_EXPERIENCE_PCT,
          DoubleModifier.MYS_EXPERIENCE_PCT,
          DoubleModifier.MOX_EXPERIENCE_PCT);

  private final OutfitEnvelope outfitEnvelope;
  private final MaximizerNumericBound numericBound;
  private double currentOutfitReduction;
  private MaximizerNumericBound.BoundResult partialTorsoResult;

  private MaximizerBranchBound(OutfitEnvelope outfitEnvelope, MaximizerNumericBound numericBound) {
    this.outfitEnvelope = outfitEnvelope;
    this.numericBound = numericBound;
  }

  static MaximizerBranchBound create(
      Evaluator evaluator,
      MaximizerSpeculation baseline,
      SlotList<CheckedItem> possibles,
      double interactionUpper,
      OutfitEnvelope outfitEnvelope,
      SynergyEnvelope synergyEnvelope,
      java.util.EnumSet<Category> categories) {
    var projectionBaseline = baseline.clone();
    if (projectionBaseline == null) throw new IllegalStateException("Unable to clone speculation");
    var projections = new EnumMap<Slot, Map<Integer, Modifiers>>(Slot.class);
    for (var slot : baseline.equipment.keySet()) {
      if (slot == Slot.ACCESSORY1 || Slot.ACCESSORY2.equals(slot) || Slot.ACCESSORY3.equals(slot)) {
        continue;
      }
      if (baseline.equipment.get(slot) != null) continue;
      var projected = new HashMap<Integer, Modifiers>();
      for (var item : candidates(possibles, slot)) {
        var projection = projectionBaseline.clone();
        if (projection != null) {
          projection.equipment.put(slot, item);
          projected.put(item.getItemId(), calculateProjection(projection));
        }
      }
      projections.put(slot, Map.copyOf(projected));
    }
    var accessoryProjections = new HashMap<Integer, Modifiers>();
    var accessoryCopies = new HashMap<Integer, Integer>();
    Modifiers emptyProjection = calculateProjection(projectionBaseline.clone());
    Slot accessoryProjectionSlot =
        baseline.equipment.entrySet().stream()
            .filter(entry -> SlotSet.ACCESSORY_SLOTS.contains(entry.getKey()))
            .filter(entry -> entry.getValue() == null)
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    for (var item : possibles.get(Slot.ACCESSORY1)) {
      if (item.getItemId() > 0) {
        accessoryCopies.merge(item.getItemId(), Math.max(0, item.getCount()), Math::max);
      }
      if (accessoryProjectionSlot == null) continue;
      var projection = projectionBaseline.clone();
      if (projection != null) {
        projection.equipment.put(accessoryProjectionSlot, item);
        Modifiers candidateProjection = calculateProjection(projection);
        Modifiers delta = projectionDelta(emptyProjection, candidateProjection);
        accessoryProjections.put(item.getItemId(), delta);
      }
    }

    var accessoryGroup =
        accessoryGroup(
            baseline, possibles.get(Slot.ACCESSORY1), accessoryCopies, accessoryProjections);
    double contextualUpper = contextualUpper(evaluator, baseline, projections, accessoryGroup);
    var categoryBounds =
        categoryUpper(evaluator, baseline, possibles, projections, categories, accessoryGroup);
    // The interaction allowance includes the root synergy envelope. Remove the part that fixed
    // equipment has made unreachable instead of carrying it through every descendant branch.
    double synergyReduction =
        Math.max(
            0.0,
            Math.nextDown(synergyEnvelope.upper() - synergyEnvelope.upper(baseline, possibles)));
    double tightenedInteractionUpper = Math.max(0.0, interactionUpper - synergyReduction);
    var numericBound =
        MaximizerNumericBound.create(
            evaluator,
            emptyProjection,
            projectionBaseline.equipment,
            projectionBaseline.getModeables(),
            projections,
            List.of(accessoryGroup),
            saturatingAdd(
                saturatingAdd(categoryBounds.upper(), tightenedInteractionUpper), contextualUpper),
            categoryBounds.byCategory()[Category.BRIMSTONE.ordinal()]
                + categoryBounds.byCategory()[Category.CLOATHING.ordinal()],
            categoryBounds.jointBrimstoneCloathingUpper());
    return new MaximizerBranchBound(outfitEnvelope, numericBound);
  }

  static InterchangeableSlotGroup accessoryGroup(
      MaximizerSpeculation baseline,
      List<CheckedItem> candidates,
      Map<Integer, Integer> copiesByItem,
      Map<Integer, Modifiers> projectionDeltas) {
    return new InterchangeableSlotGroup(
        baseline.equipment.keySet().stream()
            .filter(SlotSet.ACCESSORY_SLOTS::contains)
            .filter(slot -> baseline.equipment.get(slot) == null)
            .toList(),
        candidates,
        copiesByItem,
        projectionDeltas,
        CapacityMode.UNRESOLVED_SLOTS);
  }

  boolean cannotBeat(
      MaximizerSpeculation speculation, MaximizerSpeculation incumbent, Stage stage) {
    if (incumbent == null || incumbent.failed) {
      return this.numericBound.evaluate(speculation).minimumFailed();
    }
    double incumbentScore = incumbent.getScore();
    boolean torsoComplete = torsoComplete(speculation);
    double outfitReduction = this.outfitReduction(speculation, stage);
    if (torsoComplete
        && this.partialTorsoResult != null
        && (stage == Stage.SHIRT || stage == Stage.PANTS)) {
      double margin = this.partialTorsoResult.upper() - outfitReduction - incumbentScore;
      if (this.partialTorsoResult.minimumFailed() || margin < 0.0) {
        return true;
      }
    }
    var result = this.numericBound.evaluate(speculation);
    if (!torsoComplete && (stage == Stage.SHIRT || stage == Stage.PANTS)) {
      this.partialTorsoResult = result;
    }
    double margin = result.upper() - outfitReduction - incumbentScore;
    return result.minimumFailed() || margin < 0.0;
  }

  MaximizerNumericBound.BoundResult numericEvaluation(MaximizerSpeculation speculation) {
    return this.numericBound.evaluate(speculation);
  }

  private double outfitReduction(MaximizerSpeculation speculation, Stage stage) {
    boolean terminal = stage == Stage.HOLSTER || stage == Stage.WEAPON || stage == Stage.OFFHAND;
    boolean torsoComplete = torsoComplete(speculation);
    if ((stage == Stage.SHIRT || stage == Stage.PANTS) && torsoComplete) {
      this.currentOutfitReduction =
          Math.max(0.0, this.outfitEnvelope.upper() - this.outfitEnvelope.upper(speculation));
      return this.currentOutfitReduction;
    }
    return terminal ? this.currentOutfitReduction : 0.0;
  }

  private static boolean torsoComplete(MaximizerSpeculation speculation) {
    return speculation.equipment.get(Slot.CONTAINER) != null
        && speculation.equipment.get(Slot.HAT) != null
        && speculation.equipment.get(Slot.SHIRT) != null
        && speculation.equipment.get(Slot.PANTS) != null;
  }

  private static double[] boundedCopyUpper(
      Map<Integer, Double> contributions, int capacity, Map<Integer, Integer> copiesByItem) {
    double[] totals = new double[capacity + 1];
    for (var entry : copiesByItem.entrySet()) {
      double contribution = contributions.getOrDefault(entry.getKey(), 0.0);
      double[] next = totals.clone();
      int copies = Math.min(entry.getValue(), capacity);
      for (int used = 0; used <= capacity; used++) {
        for (int selected = 1; selected <= Math.min(copies, capacity - used); selected++) {
          next[used + selected] =
              Math.max(next[used + selected], saturatingAdd(totals[used], selected * contribution));
        }
      }
      totals = next;
    }
    for (int used = 1; used <= capacity; used++) {
      totals[used] = Math.max(totals[used], totals[used - 1]);
    }
    return totals;
  }

  private static double contextualUpper(
      Evaluator evaluator,
      MaximizerSpeculation baseline,
      Map<Slot, Map<Integer, Modifiers>> projections,
      InterchangeableSlotGroup group) {
    if (!evaluator.hasPositiveContextualModifier()) return 0.0;
    var completed = baseline.clone();
    if (completed == null) return 0.0;
    completed.setFamiliar(FamiliarData.NO_FAMILIAR);
    Modifiers base = calculateProjection(completed);
    Modifiers upper = new Modifiers(base);
    // Per-role maxima may come from different items; combining them is intentionally optimistic.
    for (var modifier : CONTEXTUAL_MODIFIERS) {
      double value = base.getDouble(modifier);
      for (var entry : projections.entrySet()) {
        if (SlotSet.ACCESSORY_SLOTS.contains(entry.getKey())) continue;
        value +=
            entry.getValue().values().stream()
                .mapToDouble(
                    projected ->
                        Math.max(0.0, projected.getDouble(modifier) - base.getDouble(modifier)))
                .max()
                .orElse(0.0);
      }
      var values = new HashMap<Integer, Double>();
      for (var entry : group.projectionDeltas().entrySet()) {
        values.put(entry.getKey(), Math.max(0.0, entry.getValue().getDouble(modifier)));
      }
      value =
          saturatingAdd(
              value, maximum(boundedCopyUpper(values, group.capacity(), group.copiesByItem())));
      upper.setDouble(modifier, value);
    }
    // Familiar formulas combine weight and effectiveness, so evaluate them after building the
    // optimistic modifier envelope.
    base.applyFamiliarModifiers(baseline.getFamiliar(), EquipmentRequest.UNEQUIP);
    upper.applyFamiliarModifiers(baseline.getFamiliar(), EquipmentRequest.UNEQUIP);
    return finite(evaluator.getScore(upper) - evaluator.getScore(base));
  }

  static OutfitEnvelope outfitEnvelope(Map<Integer, Double> outfitUppers) {
    return outfitEnvelope(outfitUppers, false);
  }

  static OutfitEnvelope outfitEnvelope(
      Map<Integer, Double> outfitUppers, boolean preserveHoboPowerOutfits) {
    if (preserveHoboPowerOutfits && !outfitUppers.isEmpty()) {
      double upper =
          outfitUppers.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
      var protectedUppers = new HashMap<>(outfitUppers);
      for (var entry : EquipmentDatabase.normalOutfits.entrySet()) {
        var outfit = entry.getValue();
        if (outfit == null) continue;
        Modifiers modifiers = ModifierDatabase.getModifiers(ModifierType.OUTFIT, outfit.getName());
        if (modifiers != null && modifiers.getDouble(DoubleModifier.HOBO_POWER) > 0.0) {
          protectedUppers.put(entry.getKey(), upper);
        }
      }
      outfitUppers = protectedUppers;
    }
    double upper = 0.0;
    var bySlot = new EnumMap<Slot, Map<Integer, Double>>(Slot.class);
    for (var slot : OUTFIT_SLOTS) bySlot.put(slot, new HashMap<>());
    for (var entry : outfitUppers.entrySet()) {
      if (entry.getValue() <= 0.0) continue;
      var outfit = EquipmentDatabase.getOutfit(entry.getKey());
      if (outfit == null) continue;
      upper = Math.max(upper, entry.getValue());
      var required = new EnumMap<Slot, Integer>(Slot.class);
      for (var piece : outfit.getPieces()) {
        Slot slot = EquipmentManager.itemIdToEquipmentType(piece.getItemId());
        if (OUTFIT_SLOTS.contains(slot)) required.put(slot, piece.getItemId());
      }
      for (var slot : OUTFIT_SLOTS) {
        bySlot.get(slot).merge(required.getOrDefault(slot, -1), entry.getValue(), Math::max);
      }
    }
    return new OutfitEnvelope(upper, Map.copyOf(bySlot));
  }

  static SynergyEnvelope synergyEnvelope(Map<String, Double> synergyUppers) {
    var combinations = new ArrayList<SynergyCombination>();
    combinations.add(new SynergyCombination(new int[0], 0.0));
    double upper = 0.0;
    for (var entry : synergyUppers.entrySet()) {
      if (entry.getValue() <= 0.0) continue;
      int[] itemIds =
          Arrays.stream(entry.getKey().split("/"))
              .mapToInt(ItemDatabase::getItemId)
              .filter(itemId -> itemId > 0)
              .sorted()
              .toArray();
      int size = combinations.size();
      for (int index = 0; index < size; index++) {
        var combination = combinations.get(index);
        int[] merged =
            java.util.stream.IntStream.concat(
                    Arrays.stream(combination.itemIds()), Arrays.stream(itemIds))
                .distinct()
                .sorted()
                .toArray();
        combinations.add(
            new SynergyCombination(merged, Math.nextUp(combination.upper() + entry.getValue())));
      }
      upper = Math.nextUp(upper + entry.getValue());
    }
    return new SynergyEnvelope(upper, List.copyOf(combinations));
  }

  private static boolean synergyFeasible(
      int[] requiredItemIds,
      MaximizerSpeculation baseline,
      java.util.Set<Integer> selected,
      java.util.Set<Integer> available,
      long freeAccessories,
      int freeWeapons) {
    int accessoryNeeded = 0;
    int weaponNeeded = 0;
    var requiredBySlot = new EnumMap<Slot, Integer>(Slot.class);
    for (int itemId : requiredItemIds) {
      if (!available.contains(itemId)) return false;
      if (selected.contains(itemId)) continue;
      Slot slot = EquipmentManager.itemIdToEquipmentType(itemId);
      if (SlotSet.ACCESSORY_SLOTS.contains(slot)) {
        accessoryNeeded++;
      } else if (slot == Slot.WEAPON || slot == Slot.OFFHAND) {
        weaponNeeded++;
      } else if (requiredBySlot.putIfAbsent(slot, itemId) != null) {
        return false;
      }
    }

    if (accessoryNeeded > freeAccessories) return false;
    if (weaponNeeded > freeWeapons) return false;
    for (var entry : requiredBySlot.entrySet()) {
      AdventureResult fixed = baseline.equipment.get(entry.getKey());
      if (fixed != null && fixed.getItemId() != entry.getValue()) return false;
    }
    return true;
  }

  private static CategoryBounds categoryUpper(
      Evaluator evaluator,
      MaximizerSpeculation baseline,
      SlotList<CheckedItem> possibles,
      Map<Slot, Map<Integer, Modifiers>> projections,
      java.util.EnumSet<Category> categories,
      InterchangeableSlotGroup group) {
    double baselineScore = score(evaluator, baseline);
    double upper = 0.0;
    double[] byCategory = new double[Category.values().length];
    int[] categoryBitCounts = new int[Category.values().length];
    // Let each category choose its own ideal equipment. Those choices can conflict, so their sum is
    // deliberately optimistic; a joint bound below tightens the overlapping nonlinear pair.
    for (var category : Category.values()) {
      if (!categories.contains(category)) continue;
      var projection = baseline.clone();
      if (projection == null) continue;
      double ordinary = 0.0;
      for (var slot : baseline.equipment.keySet()) {
        if (projection.equipment.get(slot) != null) continue;
        if (group.slots().contains(slot)) continue;
        CheckedItem best = null;
        for (var item : candidates(possibles, slot)) {
          if (best == null || categoryValue(item, category) > categoryValue(best, category)) {
            best = item;
          }
        }
        if (best != null) {
          projection.equipment.put(slot, best);
          ordinary += contribution(evaluator, baseline, baselineScore, slot, best);
        }
      }
      double groupOrdinary = 0.0;
      var selected =
          groupCategorySelection(
              category,
              group,
              categoryBitmapValue(calculateProjection(projection.clone()), category));
      for (int index = 0; index < selected.size(); index++) {
        var slot = group.slots().get(index);
        var item = selected.get(index);
        projection.equipment.put(slot, item);
        groupOrdinary += contribution(evaluator, baseline, baselineScore, slot, item);
      }
      Modifiers categoryProjection = calculateProjection(projection.clone());
      double categoryContribution =
          Math.max(
              0.0,
              evaluator.getScore(
                      categoryProjection, projection.equipment, projection.getModeables())
                  - baselineScore
                  - ordinary
                  - groupOrdinary);
      upper += categoryContribution;
      byCategory[category.ordinal()] = categoryContribution;
      if (category == Category.BRIMSTONE
          || category == Category.CLOATHING
          || category == Category.MCHUGELARGE) {
        categoryBitCounts[category.ordinal()] =
            switch (category) {
              case BRIMSTONE ->
                  Integer.bitCount(categoryProjection.getRawBitmap(BitmapModifier.BRIMSTONE));
              case CLOATHING ->
                  Integer.bitCount(categoryProjection.getRawBitmap(BitmapModifier.CLOATHING));
              case MCHUGELARGE ->
                  Integer.bitCount(categoryProjection.getRawBitmap(BitmapModifier.MCHUGELARGE));
              default -> 0;
            };
      }
    }
    boolean jointBrimstoneCloathing =
        categories.contains(Category.BRIMSTONE)
            && categories.contains(Category.CLOATHING)
            && evaluator.supportsJointBrimstoneCloathingBound();
    double[][] jointBrimstoneCloathingUpper = new double[0][];
    if (jointBrimstoneCloathing) {
      // Both bonuses grow exponentially with distinct piece count, affect shared Item and Meat
      // objectives, and compete for slots. Score only pairs attainable by one equipment set rather
      // than summing two independently best, possibly incompatible outfits.
      double independent =
          byCategory[Category.BRIMSTONE.ordinal()] + byCategory[Category.CLOATHING.ordinal()];
      double joint =
          Math.min(
              independent,
              jointBitmapCategoryUpper(
                  evaluator,
                  baseline,
                  possibles,
                  projections,
                  group,
                  categoryBitCounts[Category.BRIMSTONE.ordinal()],
                  categoryBitCounts[Category.CLOATHING.ordinal()]));
      if (joint < independent) {
        double scale = independent == 0.0 ? 0.0 : joint / independent;
        byCategory[Category.BRIMSTONE.ordinal()] *= scale;
        byCategory[Category.CLOATHING.ordinal()] *= scale;
        upper -= independent - joint;
      }
      // Retain the residual for each count pair so deeper branches can remove root potential that
      // their fixed choices make unreachable.
      Modifiers baselineModifiers = calculateProjection(baseline.clone());
      int baselineBrimstone =
          Integer.bitCount(baselineModifiers.getRawBitmap(BitmapModifier.BRIMSTONE));
      int baselineCloathing =
          Integer.bitCount(baselineModifiers.getRawBitmap(BitmapModifier.CLOATHING));
      int maximumBrimstone = categoryBitCounts[Category.BRIMSTONE.ordinal()];
      int maximumCloathing = categoryBitCounts[Category.CLOATHING.ordinal()];
      jointBrimstoneCloathingUpper = new double[maximumBrimstone + 1][maximumCloathing + 1];
      for (int brimstone = baselineBrimstone; brimstone <= maximumBrimstone; brimstone++) {
        for (int cloathing = baselineCloathing; cloathing <= maximumCloathing; cloathing++) {
          jointBrimstoneCloathingUpper[brimstone][cloathing] =
              evaluator.jointBrimstoneCloathingUpper(
                  baselineBrimstone, baselineCloathing, brimstone, cloathing);
        }
      }
    }
    return new CategoryBounds(finite(upper), byCategory, jointBrimstoneCloathingUpper);
  }

  private static double jointBitmapCategoryUpper(
      Evaluator evaluator,
      MaximizerSpeculation baseline,
      SlotList<CheckedItem> possibles,
      Map<Slot, Map<Integer, Modifiers>> projections,
      InterchangeableSlotGroup group,
      int maximumBrimstone,
      int maximumCloathing) {
    Modifiers baselineModifiers = calculateProjection(baseline.clone());
    int baselineBrimstone = baselineModifiers.getRawBitmap(BitmapModifier.BRIMSTONE);
    int baselineCloathing = baselineModifiers.getRawBitmap(BitmapModifier.CLOATHING);
    // Track actual piece masks, not just counts: duplicate pieces add no level, and slot/group
    // capacity determines which Brimstone and Cloathing masks can coexist.
    var states =
        new java.util.HashSet<Long>(
            java.util.Set.of(packCategoryState(baselineBrimstone, baselineCloathing)));

    for (var slot : baseline.equipment.keySet()) {
      if (baseline.equipment.get(slot) != null) continue;
      if (group.slots().contains(slot)) continue;
      var next = new java.util.HashSet<>(states);
      for (var item : candidates(possibles, slot)) {
        Modifiers modifiers = projections.getOrDefault(slot, Map.of()).get(item.getItemId());
        if (modifiers == null) continue;
        int brimstone = modifiers.getRawBitmap(BitmapModifier.BRIMSTONE);
        int cloathing = modifiers.getRawBitmap(BitmapModifier.CLOATHING);
        if (brimstone == 0 && cloathing == 0) continue;
        for (long state : states) {
          next.add(
              packCategoryState(
                  unpackBrimstone(state) | brimstone, unpackCloathing(state) | cloathing));
        }
      }
      states = next;
    }

    var byCapacity = new ArrayList<java.util.Set<Long>>(group.capacity() + 1);
    for (int used = 0; used <= group.capacity(); used++) {
      byCapacity.add(new java.util.HashSet<>());
    }
    byCapacity.get(0).addAll(states);
    for (var item : group.candidates()) {
      if (group.copiesByItem().getOrDefault(item.getItemId(), 0) <= 0) continue;
      Modifiers modifiers = group.projectionDeltas().get(item.getItemId());
      if (modifiers == null) continue;
      int brimstone = modifiers.getRawBitmap(BitmapModifier.BRIMSTONE);
      int cloathing = modifiers.getRawBitmap(BitmapModifier.CLOATHING);
      if (brimstone == 0 && cloathing == 0) continue;
      for (int used = group.capacity() - 1; used >= 0; used--) {
        for (long state : List.copyOf(byCapacity.get(used))) {
          byCapacity
              .get(used + 1)
              .add(
                  packCategoryState(
                      unpackBrimstone(state) | brimstone, unpackCloathing(state) | cloathing));
        }
      }
    }
    states = new java.util.HashSet<>();
    for (var capacityStates : byCapacity) states.addAll(capacityStates);

    int baselineBrimstoneCount = Integer.bitCount(baselineBrimstone);
    int baselineCloathingCount = Integer.bitCount(baselineCloathing);
    double upper = 0.0;
    for (long state : states) {
      int brimstone = Integer.bitCount(unpackBrimstone(state));
      int cloathing = Integer.bitCount(unpackCloathing(state));
      if (brimstone > maximumBrimstone || cloathing > maximumCloathing) continue;
      double stateUpper =
          evaluator.jointBrimstoneCloathingUpper(
              baselineBrimstoneCount, baselineCloathingCount, brimstone, cloathing);
      if (stateUpper > upper) {
        upper = stateUpper;
      }
    }
    return finite(upper);
  }

  private static long packCategoryState(int brimstone, int cloathing) {
    return ((long) brimstone << 32) | (cloathing & 0xffffffffL);
  }

  private static int unpackBrimstone(long state) {
    return (int) (state >>> 32);
  }

  private static int unpackCloathing(long state) {
    return (int) state;
  }

  private static List<CheckedItem> groupCategorySelection(
      Category category, InterchangeableSlotGroup group, int baselineValue) {
    int capacity = group.capacity();
    if (category == Category.BRIMSTONE
        || category == Category.CLOATHING
        || category == Category.MCHUGELARGE) {
      // Bitmap categories reward distinct pieces, so enumerate reachable unions rather than
      // adding each candidate's apparent value.
      var states = new ArrayList<Map<Integer, List<CheckedItem>>>(capacity + 1);
      for (int used = 0; used <= capacity; used++) states.add(new HashMap<>());
      states.get(0).put(baselineValue, List.of());
      for (var item : group.candidates()) {
        int copies = Math.min(group.copiesByItem().getOrDefault(item.getItemId(), 0), capacity);
        int value = categoryBitmapValue(group.projectionDeltas().get(item.getItemId()), category);
        if (copies == 0 || value == 0) continue;
        var next = new ArrayList<Map<Integer, List<CheckedItem>>>(capacity + 1);
        for (int used = 0; used <= capacity; used++) next.add(new HashMap<>(states.get(used)));
        for (int used = 0; used <= capacity; used++) {
          for (var state : states.get(used).entrySet()) {
            for (int count = 1; count <= Math.min(copies, capacity - used); count++) {
              var selected = new ArrayList<>(state.getValue());
              for (int copy = 0; copy < count; copy++) selected.add(item);
              next.get(used + count).putIfAbsent(state.getKey() | value, List.copyOf(selected));
            }
          }
        }
        states = next;
      }
      List<CheckedItem> selected = List.of();
      int maximumBits = Integer.bitCount(baselineValue);
      for (var state : states) {
        for (var entry : state.entrySet()) {
          int bits = Integer.bitCount(entry.getKey());
          if (bits > maximumBits) {
            maximumBits = bits;
            selected = entry.getValue();
          }
        }
      }
      return selected;
    }

    var sorted =
        group.candidates().stream()
            .filter(item -> group.copiesByItem().getOrDefault(item.getItemId(), 0) > 0)
            .sorted(
                Comparator.comparingDouble(
                        (CheckedItem item) ->
                            categoryValue(group.projectionDeltas().get(item.getItemId()), category))
                    .reversed())
            .toList();
    var selected = new ArrayList<CheckedItem>();
    for (var item : sorted) {
      double value = categoryValue(group.projectionDeltas().get(item.getItemId()), category);
      if (value <= 0.0) break;
      int copies =
          Math.min(
              group.copiesByItem().getOrDefault(item.getItemId(), 0), capacity - selected.size());
      for (int copy = 0; copy < copies; copy++) selected.add(item);
      if (selected.size() == capacity) break;
    }
    return selected;
  }

  private static double categoryValue(Modifiers modifiers, Category category) {
    if (modifiers == null) return 0.0;
    return switch (category) {
      case HOBO_POWER -> modifiers.getDouble(DoubleModifier.HOBO_POWER);
      case SMITHSNESS -> modifiers.getDouble(DoubleModifier.SMITHSNESS);
      case BRIMSTONE -> modifiers.getBitmap(BitmapModifier.BRIMSTONE);
      case CLOATHING -> modifiers.getBitmap(BitmapModifier.CLOATHING);
      case SLIME_HATES_IT -> modifiers.getDouble(DoubleModifier.SLIME_HATES_IT);
      case MCHUGELARGE -> modifiers.getBitmap(BitmapModifier.MCHUGELARGE);
    };
  }

  private static int categoryBitmapValue(Modifiers modifiers, Category category) {
    if (modifiers == null) return 0;
    return switch (category) {
      case BRIMSTONE -> modifiers.getBitmap(BitmapModifier.BRIMSTONE);
      case CLOATHING -> modifiers.getBitmap(BitmapModifier.CLOATHING);
      case MCHUGELARGE -> modifiers.getBitmap(BitmapModifier.MCHUGELARGE);
      default -> 0;
    };
  }

  private static double categoryValue(AdventureResult item, Category category) {
    return categoryValue(ModifierDatabase.getItemModifiers(item.getItemId()), category);
  }

  private static Modifiers projectionDelta(Modifiers baseline, Modifiers projection) {
    var delta = new Modifiers();
    for (var modifier : DoubleModifier.DOUBLE_MODIFIERS) {
      double value = projection.getRawDouble(modifier) - baseline.getRawDouble(modifier);
      if (value != 0.0) delta.setDouble(modifier, value);
    }
    for (var modifier : java.util.EnumSet.allOf(BitmapModifier.class)) {
      int value = projection.getRawBitmap(modifier) & ~baseline.getRawBitmap(modifier);
      if (value != 0) delta.setBitmap(modifier, value);
    }
    for (var modifier :
        java.util.EnumSet.allOf(net.sourceforge.kolmafia.modifiers.BooleanModifier.class)) {
      if (projection.getBoolean(modifier) && !baseline.getBoolean(modifier)) {
        delta.setBoolean(modifier, true);
      }
    }
    return delta;
  }

  static Modifiers calculateProjection(MaximizerSpeculation projection) {
    for (var slot : SlotSet.ALL_SLOTS) {
      projection.equipment.putIfAbsent(slot, EquipmentRequest.UNEQUIP);
    }
    for (var entry : projection.equipment.entrySet()) {
      if (entry.getValue() == null) entry.setValue(EquipmentRequest.UNEQUIP);
    }
    projection.setUnscored();
    return projection.getModifiers();
  }

  private static double contribution(
      Evaluator evaluator,
      MaximizerSpeculation baseline,
      double baselineScore,
      Slot slot,
      AdventureResult item) {
    var projection = baseline.clone();
    if (projection == null) return 0.0;
    projection.equipment.put(slot, item);
    return Math.max(0.0, finite(score(evaluator, projection) - baselineScore));
  }

  private static double finite(double value) {
    return Double.isFinite(value) ? Math.max(0.0, value) : Double.MAX_VALUE;
  }

  private static double saturatingAdd(double left, double right) {
    if (left >= Double.MAX_VALUE || right >= Double.MAX_VALUE) return Double.MAX_VALUE;
    return finite(left + right);
  }

  private static double maximum(double[] values) {
    double maximum = 0.0;
    for (double value : values) maximum = Math.max(maximum, value);
    return maximum;
  }

  private static double score(Evaluator evaluator, MaximizerSpeculation speculation) {
    var complete = speculation.clone();
    if (complete == null) return 0.0;
    for (var entry : complete.equipment.entrySet()) {
      if (entry.getValue() == null) entry.setValue(EquipmentRequest.UNEQUIP);
    }
    complete.setUnscored();
    return evaluator.getScore(complete.getModifiers(), complete.equipment, complete.getModeables());
  }

  private static Iterable<CheckedItem> candidates(SlotList<CheckedItem> possibles, Slot slot) {
    if (slot == Slot.OFFHAND) {
      return java.util.stream.Stream.of(
              possibles.get(Slot.OFFHAND),
              possibles.get(Evaluator.OFFHAND_MELEE),
              possibles.get(Evaluator.OFFHAND_RANGED))
          .flatMap(java.util.Collection::stream)
          .toList();
    }
    return possibles.get(slot);
  }
}
