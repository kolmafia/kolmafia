package net.sourceforge.kolmafia.maximizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.request.EquipmentRequest;

/**
 * Determines whether a partial equipment branch can still improve on the current best result.
 *
 * <p>The bound combines optimistic per-slot contributions with copy-aware interchangeable groups,
 * delegating capped and signed numeric expressions to {@link MaximizerNumericBound}. Unsupported
 * interactions decline bound construction so the Maximizer falls back to exact traversal.
 */
final class MaximizerBranchBound {
  enum CapacityMode {
    UNRESOLVED_SLOTS,
    FIXED
  }

  record InterchangeableSlotGroup(
      List<Slot> slots,
      Map<Integer, Modifiers> projectionDeltas,
      Map<Integer, Integer> copiesByItem,
      CapacityMode capacityMode) {
    InterchangeableSlotGroup {
      slots = List.copyOf(slots);
      projectionDeltas = Map.copyOf(projectionDeltas);
      copiesByItem = Map.copyOf(copiesByItem);
    }

    int capacity() {
      return this.slots.size();
    }

    int remainingCapacity(MaximizerSpeculation speculation) {
      if (this.capacityMode == CapacityMode.FIXED) return this.capacity();
      int remaining = 0;
      for (var slot : this.slots) {
        if (speculation.equipment.get(slot) == null) remaining++;
      }
      return remaining;
    }
  }

  private record GroupBound(InterchangeableSlotGroup group, double[] upperByCapacity) {}

  private final Evaluator evaluator;
  private final Map<Slot, Double> roleMaxima;
  private final List<GroupBound> groupBounds;
  private final double contextualUpper;
  private final double interactionUpper;
  private final MaximizerNumericBound numericBound;

  private MaximizerBranchBound(
      Evaluator evaluator,
      Map<Slot, Double> roleMaxima,
      List<GroupBound> groupBounds,
      double contextualUpper,
      double interactionUpper,
      MaximizerNumericBound numericBound) {
    this.evaluator = evaluator;
    this.roleMaxima = roleMaxima;
    this.groupBounds = groupBounds;
    this.contextualUpper = contextualUpper;
    this.interactionUpper = interactionUpper;
    this.numericBound = numericBound;
  }

  static Optional<MaximizerBranchBound> create(
      Evaluator evaluator, MaximizerSpeculation baseline, SlotList<CheckedItem> possibles) {
    return create(evaluator, baseline, possibles, 0.0);
  }

  static Optional<MaximizerBranchBound> create(
      Evaluator evaluator,
      MaximizerSpeculation baseline,
      SlotList<CheckedItem> possibles,
      double interactionUpper) {
    if (!evaluator.supportsNumericBranchBound(possibles)) return Optional.empty();
    boolean legacyBoundApplicable = evaluator.supportsLegacyAdditiveBranchBound(possibles);

    var projectionBaseline = baseline.clone();
    if (projectionBaseline == null) return Optional.empty();
    boolean needsNumericBound = !legacyBoundApplicable;
    boolean hasContextualModifier = evaluator.hasPositiveContextualModifier();
    boolean needsProjections = needsNumericBound || hasContextualModifier;
    var roleMaxima = new HashMap<Slot, Double>();
    var projections = new HashMap<Slot, Map<Integer, Modifiers>>();
    for (var slot : baseline.equipment.keySet()) {
      if (SlotSet.ACCESSORY_SLOTS.contains(slot) || baseline.equipment.get(slot) != null) continue;
      double maximum = 0.0;
      var projected = new HashMap<Integer, Modifiers>();
      for (var item : candidates(possibles, slot)) {
        maximum = Math.max(maximum, contribution(evaluator, projectionBaseline, slot, item));
        if (needsProjections) {
          var projection = projectionBaseline.clone();
          if (projection != null) {
            projection.equipment.put(slot, item);
            projected.put(item.getItemId(), calculateProjection(projection));
          }
        }
      }
      roleMaxima.put(slot, maximum);
      if (needsProjections) projections.put(slot, Map.copyOf(projected));
    }

    var accessoryProjections = new HashMap<Integer, Modifiers>();
    var accessoryContributions = new HashMap<Integer, Double>();
    var accessoryCopies = new HashMap<Integer, Integer>();
    Slot accessoryProjectionSlot =
        baseline.equipment.entrySet().stream()
            .filter(entry -> SlotSet.ACCESSORY_SLOTS.contains(entry.getKey()))
            .filter(entry -> entry.getValue() == null)
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    for (var item : possibles.get(Slot.ACCESSORY1)) {
      if (item.getItemId() <= 0) continue;
      if (needsProjections && accessoryProjectionSlot != null) {
        var projection = projectionBaseline.clone();
        if (projection != null) {
          projection.equipment.put(accessoryProjectionSlot, item);
          accessoryProjections.put(item.getItemId(), calculateProjection(projection));
        }
      }
      if (accessoryProjectionSlot != null) {
        accessoryContributions.merge(
            item.getItemId(),
            contribution(evaluator, projectionBaseline, accessoryProjectionSlot, item),
            Math::max);
      }
      accessoryCopies.merge(item.getItemId(), Math.max(0, item.getCount()), Math::max);
    }
    if (needsProjections) {
      projections.put(Slot.ACCESSORY1, Map.copyOf(accessoryProjections));
    }
    var accessorySlots =
        baseline.equipment.keySet().stream()
            .filter(SlotSet.ACCESSORY_SLOTS::contains)
            .filter(slot -> baseline.equipment.get(slot) == null)
            .toList();
    var accessoryDeltas = new HashMap<Integer, Modifiers>();
    if (needsProjections) {
      var empty = projectionBaseline.clone();
      if (empty == null) return Optional.empty();
      Modifiers emptyProjection = calculateProjection(empty);
      for (var entry : accessoryProjections.entrySet()) {
        accessoryDeltas.put(entry.getKey(), projectionDelta(emptyProjection, entry.getValue()));
      }
    }
    var accessoryGroup =
        new InterchangeableSlotGroup(
            accessorySlots, accessoryDeltas, accessoryCopies, CapacityMode.UNRESOLVED_SLOTS);
    var groupBounds =
        List.of(
            new GroupBound(
                accessoryGroup,
                capacityUpper(accessoryCopies, accessoryContributions, accessoryGroup.capacity())));
    double contextualUpper =
        contextualUpper(evaluator, baseline, projections, List.of(accessoryGroup));
    MaximizerNumericBound numericBound = null;
    if (needsNumericBound) {
      var empty = projectionBaseline.clone();
      if (empty == null) return Optional.empty();
      Modifiers emptyProjection = calculateProjection(empty);
      var numericProjections = new HashMap<>(projections);
      numericProjections.remove(Slot.ACCESSORY1);
      numericBound =
          MaximizerNumericBound.create(
              evaluator,
              emptyProjection,
              projectionBaseline.equipment,
              projectionBaseline.getModeables(),
              numericProjections,
              List.of(accessoryGroup),
              contextualUpper + interactionUpper);
    }
    return Optional.of(
        new MaximizerBranchBound(
            evaluator,
            Map.copyOf(roleMaxima),
            groupBounds,
            contextualUpper,
            interactionUpper,
            numericBound));
  }

  boolean cannotBeat(MaximizerSpeculation speculation, MaximizerSpeculation incumbent) {
    if (this.numericBound != null) {
      var result = this.numericBound.evaluate(speculation);
      if (result.minimumFailed()) return true;
      return incumbent != null && !incumbent.failed && result.upper() < incumbent.getScore();
    }
    if (incumbent == null || incumbent.failed) return false;
    return this.additiveUpperBound(speculation) < incumbent.getScore();
  }

  private double additiveUpperBound(MaximizerSpeculation speculation) {
    double upper = score(this.evaluator, speculation);
    for (var entry : this.roleMaxima.entrySet()) {
      if (speculation.equipment.get(entry.getKey()) == null) upper += entry.getValue();
    }
    for (var groupBound : this.groupBounds) {
      upper += groupBound.upperByCapacity()[groupBound.group().remainingCapacity(speculation)];
    }
    return upper + this.contextualUpper + this.interactionUpper;
  }

  private static double[] capacityUpper(
      Map<Integer, Integer> copiesByItem, Map<Integer, Double> contributions, int capacity) {
    double[] totals = new double[capacity + 1];
    for (var entry : copiesByItem.entrySet()) {
      double contribution = contributions.getOrDefault(entry.getKey(), 0.0);
      double[] next = totals.clone();
      for (int used = 0; used <= capacity; used++) {
        int copies = Math.min(entry.getValue(), capacity - used);
        for (int count = 1; count <= copies; count++) {
          next[used + count] = Math.max(next[used + count], totals[used] + count * contribution);
        }
      }
      totals = next;
    }
    for (int used = 1; used <= capacity; used++) {
      totals[used] = Math.max(totals[used], totals[used - 1]);
    }
    return totals;
  }

  private static Modifiers calculateProjection(MaximizerSpeculation projection) {
    for (var slot : SlotSet.ALL_SLOTS) {
      projection.equipment.putIfAbsent(slot, EquipmentRequest.UNEQUIP);
    }
    for (var entry : projection.equipment.entrySet()) {
      if (entry.getValue() == null) entry.setValue(EquipmentRequest.UNEQUIP);
    }
    projection.setUnscored();
    return projection.calculate();
  }

  private static Modifiers projectionDelta(Modifiers baseline, Modifiers projection) {
    var delta = new Modifiers();
    for (var modifier : DoubleModifier.DOUBLE_MODIFIERS) {
      double value = projection.getRawDouble(modifier) - baseline.getRawDouble(modifier);
      if (value != 0.0) delta.setDouble(modifier, value);
    }
    for (var modifier : BitmapModifier.values()) {
      int value = projection.getRawBitmap(modifier) & ~baseline.getRawBitmap(modifier);
      if (value != 0) delta.setBitmap(modifier, value);
    }
    for (var modifier : BooleanModifier.values()) {
      if (projection.getBoolean(modifier) && !baseline.getBoolean(modifier)) {
        delta.setBoolean(modifier, true);
      }
    }
    return delta;
  }

  private static double contextualUpper(
      Evaluator evaluator,
      MaximizerSpeculation baseline,
      Map<Slot, Map<Integer, Modifiers>> projections,
      List<InterchangeableSlotGroup> groups) {
    if (!evaluator.hasPositiveContextualModifier()) return 0.0;
    var completed = baseline.clone();
    if (completed == null) return 0.0;
    completed.setFamiliar(FamiliarData.NO_FAMILIAR);
    Modifiers base = calculateProjection(completed);
    Modifiers upper = new Modifiers(base);
    // Per-role maxima may come from different items; combining them is intentionally optimistic.
    for (var modifier : Evaluator.CONTEXTUAL_MODIFIERS) {
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
      for (var group : groups) {
        var values = new HashMap<Integer, Double>();
        for (var entry : group.projectionDeltas().entrySet()) {
          values.put(entry.getKey(), Math.max(0.0, entry.getValue().getDouble(modifier)));
        }
        value += capacityUpper(group.copiesByItem(), values, group.capacity())[group.capacity()];
      }
      upper.setDouble(modifier, value);
    }
    // Familiar formulas combine weight and effectiveness, so evaluate them after building the
    // optimistic modifier envelope.
    base.applyFamiliarModifiers(baseline.getFamiliar(), EquipmentRequest.UNEQUIP);
    upper.applyFamiliarModifiers(baseline.getFamiliar(), EquipmentRequest.UNEQUIP);
    return Math.max(0.0, evaluator.getScore(upper) - evaluator.getScore(base));
  }

  private static double contribution(
      Evaluator evaluator, MaximizerSpeculation baseline, Slot slot, AdventureResult item) {
    var projection = baseline.clone();
    if (projection == null) return 0.0;
    projection.equipment.put(slot, item);
    return Math.max(0.0, score(evaluator, projection) - score(evaluator, baseline));
  }

  private static double score(Evaluator evaluator, MaximizerSpeculation speculation) {
    var complete = speculation.clone();
    if (complete == null) return Double.NEGATIVE_INFINITY;
    for (var entry : complete.equipment.entrySet()) {
      if (entry.getValue() == null) entry.setValue(EquipmentRequest.UNEQUIP);
    }
    complete.setUnscored();
    return evaluator.getScore(complete.calculate());
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
