package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;

/**
 * Codpiece-specific plan/cache state and canonical gem search for a single {@link
 * MaximizerSpeculation}. Reused across that owner's recursive try* traversal so expensive per-item
 * analysis (gem modifiers, familiar contributions, late-adjustment prefixes) happens at most once.
 */
final class CodpieceSpeculation {
  private static final Slot[] CODPIECE_SLOTS = SlotSet.CODPIECE_SLOTS.toArray(Slot[]::new);

  private final MaximizerSpeculation owner;
  private final Map<Integer, Modifiers> codpieceGemModifiers = new HashMap<>();
  private final Map<Integer, Boolean> safeLateCodpieceGems = new HashMap<>();
  private CodpiecePlan codpiecePlan;
  private CodpiecePlan prioritizedCodpiecePlan;
  private CodpieceSearch search;

  CodpieceSpeculation(MaximizerSpeculation owner) {
    this.owner = owner;
  }

  private record CodpiecePlan(
      List<CheckedItem> gems, Modifiers[] gemModifiers, boolean[] familiarDependentGems) {}

  /** Modifiers built from the active search's cache, applied via {@code applyAdjustmentSuffix}. */
  record CalculatedModifiers(Modifiers modifiers, Modifiers fightMods) {}

  /** Reuses the expensive adjustment prefix while the search changes only late-safe gem effects. */
  private static final class LateCodpieceCache {
    private final Modifiers baseline;
    private final Modifiers fightMods;
    private final List<Slot> slots;
    private final Modifiers[] gemModifiers;
    private final boolean[] familiarDependentGems;
    private final CodpiecePruning.FamiliarScoreContributions familiarScoreContributions;
    private final Modifiers[] slotModifiers;
    private final int[] slotGemIndexes;
    private final Map<List<Integer>, KoLCharacter.AdjustmentPrefix> prefixes;
    private int familiarSlots;

    private LateCodpieceCache(
        Modifiers baseline,
        Modifiers fightMods,
        List<Slot> slots,
        Modifiers[] gemModifiers,
        boolean[] familiarDependentGems,
        CodpiecePruning.FamiliarScoreContributions familiarScoreContributions,
        Map<List<Integer>, KoLCharacter.AdjustmentPrefix> prefixes) {
      this.baseline = baseline;
      this.fightMods = fightMods;
      this.slots = slots;
      this.gemModifiers = gemModifiers;
      this.familiarDependentGems = familiarDependentGems;
      this.familiarScoreContributions = familiarScoreContributions;
      this.slotModifiers = new Modifiers[slots.size()];
      this.slotGemIndexes = new int[slots.size()];
      this.prefixes = prefixes;
    }

    private void select(int slotIndex, int gemIndex) {
      this.slotModifiers[slotIndex] = this.gemModifiers[gemIndex];
      this.slotGemIndexes[slotIndex] = gemIndex + 1;
      if (this.familiarDependentGems[gemIndex]) {
        this.familiarSlots++;
      }
    }

    private void deselect(int slotIndex) {
      int gemIndex = this.slotGemIndexes[slotIndex] - 1;
      if (gemIndex >= 0 && this.familiarDependentGems[gemIndex]) {
        this.familiarSlots--;
      }
      this.slotModifiers[slotIndex] = null;
      this.slotGemIndexes[slotIndex] = 0;
    }

    private KoLCharacter.AdjustmentPrefix getPrefix(
        Supplier<KoLCharacter.AdjustmentPrefix> factory) {
      List<Integer> familiarGemIndexes = new ArrayList<>();
      for (int encodedGemIndex : this.slotGemIndexes) {
        if (encodedGemIndex == 0) {
          continue;
        }
        int gemIndex = encodedGemIndex - 1;
        if (this.familiarDependentGems[gemIndex]) {
          familiarGemIndexes.add(gemIndex);
        }
      }
      return this.prefixes.computeIfAbsent(
          List.copyOf(familiarGemIndexes), ignored -> factory.get());
    }
  }

  /**
   * Builds the modifiers contributed by the active Codpiece search's cache, or {@code null} if no
   * search is currently active (in which case the owner should fall back to its default
   * calculation).
   */
  CalculatedModifiers calculateModifiers() {
    LateCodpieceCache cache = this.search == null ? null : this.search.cache;
    if (cache == null) {
      return null;
    }

    Modifiers newModifiers;
    Modifiers fightMods;
    if (cache.familiarSlots == 0) {
      newModifiers = new Modifiers(cache.baseline);
      for (Modifiers gemModifiers : cache.slotModifiers) {
        newModifiers.add(gemModifiers);
      }
      fightMods = cache.fightMods;
    } else {
      // Familiar-dependent gems must be present when familiar effects are calculated.
      var prefix = cache.getPrefix(this::primeFamiliarCodpiecePrefix);
      newModifiers = new Modifiers(prefix.modifiers());
      for (int encodedGemIndex : cache.slotGemIndexes) {
        if (encodedGemIndex == 0) {
          continue;
        }
        int gemIndex = encodedGemIndex - 1;
        if (!cache.familiarDependentGems[gemIndex]) {
          newModifiers.add(cache.gemModifiers[gemIndex]);
        }
      }
      fightMods = prefix.fightMods();
    }
    return new CalculatedModifiers(newModifiers, fightMods);
  }

  /**
   * Tail of {@code MaximizerSpeculation.tryOffhands} once the weapon/offhand slots are settled:
   * handles the Codpiece gem slots, restoring {@code mark} on every exit path.
   */
  void trySlots(EnumMap<Slot, AdventureResult> mark, List<CheckedItem> possibles)
      throws MaximizerInterruptedException {
    boolean wearingCodpiece =
        this.owner.equipment.values().stream()
            .anyMatch(item -> item != null && item.getItemId() == ItemPool.THE_ETERNITY_CODPIECE);
    if (!wearingCodpiece) {
      this.releaseCodpieceGemsNeededElsewhere();
      this.owner.checkBest();
      this.owner.restore(mark);
      return;
    }

    List<Slot> codpieceSlots =
        SlotSet.CODPIECE_SLOTS.stream().filter(Maximizer.evaluator()::slotEnabled).toList();
    for (Slot slot : codpieceSlots) {
      this.owner.equipment.put(slot, EquipmentRequest.UNEQUIP);
    }
    if (!this.hasEnoughCodpieceGems()) {
      this.owner.restore(mark);
      return;
    }

    CodpiecePlan plan = this.getCodpiecePlan(possibles);
    List<CheckedItem> codpieceGems = plan.gems();
    try {
      // Saturation does not model tiebreaks, shared resources, or removal of initially equipped
      // gems.
      boolean canCollapseSaturatedScore =
          !Maximizer.evaluator().isUsingTiebreaker()
              && !Maximizer.character().hasActiveResources()
              && codpieceSlots.stream()
                  .allMatch(
                      slot -> EquipmentManager.getEquipment(slot).equals(EquipmentRequest.UNEQUIP));
      LateCodpieceCache cache =
          this.canUseLateCodpieceCache(codpieceGems)
              ? this.primeLateCodpieceCache(plan, codpieceSlots)
              : null;
      CodpieceSearch search =
          new CodpieceSearch(codpieceGems, codpieceSlots, cache, canCollapseSaturatedScore);
      this.search = search;
      this.owner.setUnscored();
      if (Maximizer.evaluator().isUsingTiebreaker()
          && Maximizer.evaluator().areScoreModifiersSaturated(this.owner.calculate())) {
        if (this.prioritizedCodpiecePlan == null) {
          this.prioritizedCodpiecePlan =
              this.createCodpiecePlan(Maximizer.evaluator().prioritizeCodpieceGems(codpieceGems));
        }
        codpieceGems = this.prioritizedCodpiecePlan.gems();
        cache = this.primeLateCodpieceCache(this.prioritizedCodpiecePlan, codpieceSlots);
        search = new CodpieceSearch(codpieceGems, codpieceSlots, cache, false);
      }
      this.search = search;
      this.search.run();
    } finally {
      this.search = null;
      this.owner.restore(mark);
    }
  }

  private void releaseCodpieceGemsNeededElsewhere() {
    for (Slot slot : CODPIECE_SLOTS) {
      if (!Maximizer.evaluator().slotEnabled(slot)) {
        continue;
      }

      AdventureResult gem = this.owner.equipment.get(slot);
      if (gem == null || gem.equals(EquipmentRequest.UNEQUIP)) {
        continue;
      }

      CheckedItem equippedElsewhere =
          this.owner.equipment.entrySet().stream()
              .filter(entry -> !SlotSet.CODPIECE_SLOTS.contains(entry.getKey()))
              .map(Map.Entry::getValue)
              .filter(gem::equals)
              .filter(CheckedItem.class::isInstance)
              .map(CheckedItem.class::cast)
              .findFirst()
              .orElse(null);
      if (equippedElsewhere == null) {
        continue;
      }

      long used = this.owner.equipment.values().stream().filter(gem::equals).count();
      if (used > equippedElsewhere.getAvailableCount()) {
        this.owner.equipment.put(slot, EquipmentRequest.UNEQUIP);
      }
    }
  }

  private boolean canUseLateCodpieceCache(List<CheckedItem> possibles) {
    for (CheckedItem possible : possibles) {
      if (!this.isSafeLateCodpieceGem(possible.getItemId())) {
        return false;
      }
    }

    return true;
  }

  private CodpiecePlan getCodpiecePlan(List<CheckedItem> possibles) {
    if (this.codpiecePlan == null) {
      this.codpiecePlan =
          this.createCodpiecePlan(
              possibles.stream()
                  .filter(
                      gem -> gem.getCount() > 0 && EquipmentRequest.isCodpieceGem(gem.getItemId()))
                  .toList());
    }
    return this.codpiecePlan;
  }

  private CodpiecePlan createCodpiecePlan(List<CheckedItem> gems) {
    Modifiers[] gemModifiers = new Modifiers[gems.size()];
    boolean[] familiarDependentGems = new boolean[gems.size()];
    for (int i = 0; i < gems.size(); i++) {
      gemModifiers[i] = this.getCodpieceGemModifiers(gems.get(i).getItemId());
      familiarDependentGems[i] = CodpiecePruning.affectsFamiliarCalculation(gemModifiers[i]);
    }
    return new CodpiecePlan(gems, gemModifiers, familiarDependentGems);
  }

  private LateCodpieceCache primeLateCodpieceCache(CodpiecePlan plan, List<Slot> slots) {
    var mark = this.owner.mark();
    try {
      for (Slot slot : slots) {
        this.owner.equipment.put(slot, EquipmentRequest.UNEQUIP);
      }

      var prefix = this.owner.recalculateCodpiecePrefix(this.owner.equipment);
      Map<List<Integer>, KoLCharacter.AdjustmentPrefix> prefixes = new HashMap<>();
      prefixes.put(List.of(), prefix);
      CodpiecePruning.FamiliarScoreContributions familiarScoreContributions =
          this.findFamiliarScoreContributions(
              plan.gems(),
              slots,
              prefix.modifiers(),
              prefix.familiarWeightInputs(),
              plan.gemModifiers(),
              plan.familiarDependentGems());
      return new LateCodpieceCache(
          prefix.modifiers(),
          prefix.fightMods(),
          slots,
          plan.gemModifiers(),
          plan.familiarDependentGems(),
          familiarScoreContributions,
          prefixes);
    } finally {
      this.owner.restore(mark);
    }
  }

  private CodpiecePruning.FamiliarScoreContributions findFamiliarScoreContributions(
      List<CheckedItem> possibles,
      List<Slot> slots,
      Modifiers baseline,
      Modifiers.FamiliarWeightInputs familiarWeightInputs,
      Modifiers[] gemModifiers,
      boolean[] familiarDependentGems) {
    EnumSet<DoubleModifier> scored = Maximizer.evaluator().familiarDependentScoreModifiers();
    if (scored.isEmpty()) {
      return new CodpiecePruning.FamiliarScoreContributions(-1, Map.of());
    }
    int familiarGemIndex = -1;
    for (int i = 0; i < familiarDependentGems.length; i++) {
      if (!familiarDependentGems[i]) {
        continue;
      }
      if (familiarGemIndex != -1) {
        return null;
      }
      familiarGemIndex = i;
    }
    if (familiarGemIndex == -1) {
      return new CodpiecePruning.FamiliarScoreContributions(-1, Map.of());
    }

    Map<DoubleModifier, CodpiecePruning.ContributionRange> ranges =
        new EnumMap<>(DoubleModifier.class);
    Map<DoubleModifier, CodpiecePruning.ContributionRange> baselines =
        new EnumMap<>(DoubleModifier.class);
    CheckedItem gem = possibles.get(familiarGemIndex);
    double weightAdjustment =
        CodpiecePruning.familiarWeightAdjustment(gemModifiers[familiarGemIndex]);
    if (!Double.isFinite(weightAdjustment)) {
      return null;
    }
    int copies = Math.min(slots.size(), gem.getCount() - (int) this.countEquipped(gem.getItemId()));
    var familiar = this.owner.getFamiliar();
    var baselineEffect = baseline.familiarEffect(familiar, familiarWeightInputs, 0);
    var previous = baselineEffect;
    double[] directExperienceDeltas = new double[copies];
    double[] generalExperienceDeltas = new double[copies];
    for (int copy = 1; copy <= copies; copy++) {
      var current =
          baseline.familiarEffect(familiar, familiarWeightInputs, copy * weightAdjustment);
      directExperienceDeltas[copy - 1] =
          current.primeStatExperience() - previous.primeStatExperience();
      generalExperienceDeltas[copy - 1] =
          current.generalExperience() - previous.generalExperience();
      for (DoubleModifier modifier : scored) {
        double contribution =
            switch (modifier) {
              case ITEMDROP -> current.itemDrop() - previous.itemDrop();
              case MEATDROP -> current.meatDrop() - previous.meatDrop();
              case EXPERIENCE, MUS_EXPERIENCE, MYS_EXPERIENCE, MOX_EXPERIENCE -> 0.0;
              default -> 0.0;
            };
        if (contribution == 0.0) {
          continue;
        }
        ranges.merge(
            modifier,
            new CodpiecePruning.ContributionRange(contribution, contribution),
            (range, ignored) -> range.include(contribution));
      }
      previous = current;
    }
    for (DoubleModifier modifier : scored) {
      if (!CodpiecePruning.isExperienceScoreModifier(modifier)) {
        continue;
      }
      baselines.put(
          modifier,
          this.findFamiliarExperienceRange(
              modifier,
              possibles,
              slots.size(),
              baseline,
              gemModifiers,
              familiarGemIndex,
              new double[] {0.0},
              new double[] {baselineEffect.generalExperience()}));
      ranges.put(
          modifier,
          this.findFamiliarExperienceRange(
              modifier,
              possibles,
              slots.size(),
              baseline,
              gemModifiers,
              familiarGemIndex,
              directExperienceDeltas,
              generalExperienceDeltas));
    }
    return new CodpiecePruning.FamiliarScoreContributions(
        familiarGemIndex, Map.copyOf(ranges), Map.copyOf(baselines));
  }

  private CodpiecePruning.ContributionRange findFamiliarExperienceRange(
      DoubleModifier scoreModifier,
      List<CheckedItem> possibles,
      int slotCount,
      Modifiers baseline,
      Modifiers[] gemModifiers,
      int familiarGemIndex,
      double[] directDeltas,
      double[] generalDeltas) {
    int[] remaining = new int[possibles.size()];
    var relevant = new ArrayList<Integer>();
    for (int i = 0; i < possibles.size(); i++) {
      remaining[i] =
          Math.min(
              slotCount,
              possibles.get(i).getCount() - (int) this.countEquipped(possibles.get(i).getItemId()));
      if (i != familiarGemIndex
          && CodpiecePruning.affectsExperience(gemModifiers[i], scoreModifier)) {
        relevant.add(i);
      }
    }
    var range =
        new CodpiecePruning.ContributionRange(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
    return collectFamiliarExperienceRange(
        scoreModifier,
        baseline,
        gemModifiers,
        remaining,
        relevant,
        new int[possibles.size()],
        directDeltas,
        generalDeltas,
        range,
        0,
        0,
        slotCount);
  }

  private static CodpiecePruning.ContributionRange collectFamiliarExperienceRange(
      DoubleModifier scoreModifier,
      Modifiers current,
      Modifiers[] gemModifiers,
      int[] remaining,
      List<Integer> relevant,
      int[] used,
      double[] directDeltas,
      double[] generalDeltas,
      CodpiecePruning.ContributionRange range,
      int start,
      int selected,
      int slotCount) {
    for (int copy = 0; copy < directDeltas.length; copy++) {
      range =
          range.include(
              CodpiecePruning.familiarExperienceContribution(
                  scoreModifier, current, directDeltas[copy], generalDeltas[copy]));
    }
    if (selected == slotCount) {
      return range;
    }
    for (int relevantIndex = start; relevantIndex < relevant.size(); relevantIndex++) {
      int gemIndex = relevant.get(relevantIndex);
      if (used[gemIndex] >= remaining[gemIndex]) {
        continue;
      }
      var next = new Modifiers(current);
      CodpiecePruning.addExperienceInputs(next, gemModifiers[gemIndex]);
      used[gemIndex]++;
      range =
          collectFamiliarExperienceRange(
              scoreModifier,
              next,
              gemModifiers,
              remaining,
              relevant,
              used,
              directDeltas,
              generalDeltas,
              range,
              relevantIndex,
              selected + 1,
              slotCount);
      used[gemIndex]--;
    }
    return range;
  }

  private KoLCharacter.AdjustmentPrefix primeFamiliarCodpiecePrefix() {
    LateCodpieceCache cache = this.search.cache;
    Map<Slot, AdventureResult> equipment = new EnumMap<>(this.owner.equipment);
    for (int slotIndex = 0; slotIndex < cache.slots.size(); slotIndex++) {
      int encodedGemIndex = cache.slotGemIndexes[slotIndex];
      if (encodedGemIndex == 0 || !cache.familiarDependentGems[encodedGemIndex - 1]) {
        equipment.put(cache.slots.get(slotIndex), EquipmentRequest.UNEQUIP);
      }
    }

    return this.owner.recalculateCodpiecePrefix(equipment);
  }

  private boolean isSafeLateCodpieceGem(int itemId) {
    return this.safeLateCodpieceGems.computeIfAbsent(
        itemId,
        id ->
            CodpiecePruning.hasOnlySupportedLateCalculationModifiers(
                this.getCodpieceGemModifiers(id)));
  }

  private Modifiers getCodpieceGemModifiers(int itemId) {
    return this.codpieceGemModifiers.computeIfAbsent(
        itemId, id -> ModifierDatabase.getModifiers(ModifierType.ETERNITY_CODPIECE, id));
  }

  /** Gem-count validation: rejects branches that over-commit a gem beyond its available count. */
  boolean hasEnoughCodpieceGems() {
    boolean hasSlottedGem = false;
    for (Slot slot : CODPIECE_SLOTS) {
      AdventureResult item = this.owner.equipment.get(slot);
      if (item != null && !item.equals(EquipmentRequest.UNEQUIP)) {
        hasSlottedGem = true;
        break;
      }
    }
    if (!hasSlottedGem) {
      return true;
    }

    for (AdventureResult item : this.owner.equipment.values()) {
      if (!(item instanceof CheckedItem checked)
          || !EquipmentRequest.isCodpieceGem(item.getItemId())) {
        continue;
      }

      int itemId = item.getItemId();
      int used = (int) this.countEquipped(itemId);
      if (used > checked.getAvailableCount()) {
        return false;
      }
    }
    return true;
  }

  private long countEquipped(int itemId) {
    return this.owner.equipment.values().stream()
        .filter(item -> item != null && item.getItemId() == itemId)
        .count();
  }

  private static int countEquipmentWith(
      MaximizerSpeculation speculation, BooleanModifier modifier) {
    int count = 0;
    for (AdventureResult item : speculation.equipment.values()) {
      if (item == null) {
        continue;
      }
      Modifiers modifiers = ModifierDatabase.getItemModifiers(item.getItemId());
      if (modifiers != null && modifiers.getBoolean(modifier)) {
        count++;
      }
    }
    return count;
  }

  /** Enumerates canonical gem multisets and rejects branches using conservative score ceilings. */
  private final class CodpieceSearch {
    private final List<CheckedItem> gems;
    private final List<Slot> slots;
    private final int[] remaining;
    private final int[] initialRemaining;
    private final boolean[] required;
    private final int requiredCount;
    private final LateCodpieceCache cache;
    private final boolean canCollapseSaturatedScore;
    private CodpiecePruning.BranchBounds bounds;
    private boolean tiebreakBoundInitialized;

    private CodpieceSearch(
        List<CheckedItem> gems,
        List<Slot> slots,
        LateCodpieceCache cache,
        boolean canCollapseSaturatedScore) {
      this.gems = gems;
      this.slots = slots;
      this.remaining = new int[gems.size()];
      this.required = new boolean[gems.size()];
      this.cache = cache;
      this.canCollapseSaturatedScore = canCollapseSaturatedScore;

      int requiredCount = 0;
      for (int i = 0; i < gems.size(); i++) {
        CheckedItem gem = gems.get(i);
        long used = countEquipped(gem.getItemId());
        this.remaining[i] = gem.getCount() - (int) used;
        if (gem.requiredFlag && used == 0) {
          this.required[i] = true;
          requiredCount++;
        }
      }
      this.initialRemaining = this.remaining.clone();
      this.requiredCount = requiredCount;
    }

    private void run() throws MaximizerInterruptedException {
      CodpieceScoreBound scoreUpperBound = this.createScoreUpperBound();
      this.bounds =
          new CodpiecePruning.BranchBounds(
              scoreUpperBound,
              null,
              new CodpiecePruning.BooleanUpperBound(
                  this.gems, this.remaining, this.slots.size(), BooleanModifier.DROPS_ITEMS),
              new CodpiecePruning.BooleanUpperBound(
                  this.gems, this.remaining, this.slots.size(), BooleanModifier.DROPS_MEAT));
      this.search(0, 0, this.requiredCount);
    }

    private CodpieceScoreBound createScoreUpperBound() {
      return this.cache == null
          ? null
          : Maximizer.evaluator()
              .createTheoreticalCodpieceScoreUpperBound(
                  this.cache.baseline,
                  this.cache.gemModifiers,
                  this.remaining,
                  this.slots.size(),
                  owner.equipment,
                  owner.getModeables(),
                  this.gems,
                  this.cache.familiarScoreContributions);
    }

    private void search(int start, int slotIndex, int requiredCount)
        throws MaximizerInterruptedException {
      boolean scoreSaturated = false;
      boolean canMeetRequirements = true;
      if (this.bounds.score() != null) {
        int remainingSlots = this.slots.size() - slotIndex;
        double upperScore = this.bounds.score().estimate(start, this.remaining, remainingSlots);
        canMeetRequirements &=
            this.bounds.score().canMeetMinimum(start, this.remaining, remainingSlots, upperScore);
        if (!Maximizer.best().failed || !canMeetRequirements) {
          double bestScore = Maximizer.best().getScore();
          if (upperScore < bestScore) {
            return;
          }
          if (Double.compare(upperScore, bestScore) == 0
              && !Maximizer.character().hasActiveResources()) {
            // Keep this tie pruning in compareTo order: item drops, meat drops, then tiebreak
            // score.
            CodpieceScoreBound tiebreakUpperBound = this.getTiebreakUpperBound(slotIndex);
            int bestItemDroppers =
                countEquipmentWith(Maximizer.best(), BooleanModifier.DROPS_ITEMS);
            int itemDropperCeiling =
                countEquipmentWith(owner, BooleanModifier.DROPS_ITEMS)
                    + this.bounds
                        .itemDroppers()
                        .estimateAdditional(start, this.remaining, remainingSlots);
            if (itemDropperCeiling < bestItemDroppers) {
              return;
            }
            if (itemDropperCeiling == bestItemDroppers && tiebreakUpperBound != null) {
              int bestMeatDroppers =
                  countEquipmentWith(Maximizer.best(), BooleanModifier.DROPS_MEAT);
              int meatDropperCeiling =
                  countEquipmentWith(owner, BooleanModifier.DROPS_MEAT)
                      + this.bounds
                          .meatDroppers()
                          .estimateAdditional(start, this.remaining, remainingSlots);
              if (meatDropperCeiling < bestMeatDroppers
                  || (meatDropperCeiling == bestMeatDroppers
                      && tiebreakUpperBound.estimate(start, this.remaining, remainingSlots)
                          < Maximizer.best().getTiebreaker())) {
                return;
              }
            }
          }
        }
        scoreSaturated = this.bounds.score().isScoreSaturated(start, this.remaining, upperScore);
      }
      if (requiredCount == 0) {
        owner.checkBest(true);
        if (this.canCollapseSaturatedScore
            && scoreSaturated
            && (!owner.failed || !canMeetRequirements)) {
          return;
        }
      }
      if (slotIndex == this.slots.size() || requiredCount > this.slots.size() - slotIndex) {
        return;
      }

      int firstRequired = -1;
      for (int i = start; i < this.required.length; i++) {
        if (this.required[i]) {
          firstRequired = i;
          break;
        }
      }

      Slot slot = this.slots.get(slotIndex);
      for (int i = start; i < this.gems.size(); i++) {
        if (firstRequired != -1 && i > firstRequired) {
          break;
        }
        if (this.remaining[i] == 0) {
          continue;
        }

        boolean satisfiesRequirement = this.required[i];
        this.remaining[i]--;
        this.required[i] = false;
        owner.equipment.put(slot, this.gems.get(i));
        if (this.cache != null) {
          this.cache.select(slotIndex, i);
        }
        this.bounds.select(i);
        this.search(i, slotIndex + 1, requiredCount - (satisfiesRequirement ? 1 : 0));
        this.bounds.deselect(i);
        owner.equipment.put(slot, EquipmentRequest.UNEQUIP);
        if (this.cache != null) {
          this.cache.deselect(slotIndex);
        }
        this.required[i] = satisfiesRequirement;
        this.remaining[i]++;
      }
    }

    private CodpieceScoreBound getTiebreakUpperBound(int selectedCount) {
      if (this.tiebreakBoundInitialized) {
        return this.bounds.tiebreaker();
      }

      this.tiebreakBoundInitialized = true;
      CodpieceScoreBound tiebreakUpperBound =
          Maximizer.evaluator()
              .createTheoreticalCodpieceTiebreakerUpperBound(
                  this.cache.baseline,
                  this.cache.gemModifiers,
                  this.initialRemaining,
                  this.slots.size(),
                  this.cache.familiarScoreContributions);
      for (int i = 0; tiebreakUpperBound != null && i < selectedCount; i++) {
        tiebreakUpperBound.select(this.cache.slotGemIndexes[i] - 1);
      }
      this.bounds =
          new CodpiecePruning.BranchBounds(
              this.bounds.score(),
              tiebreakUpperBound,
              this.bounds.itemDroppers(),
              this.bounds.meatDroppers());
      return tiebreakUpperBound;
    }
  }
}
