package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;

final class CodpieceBranchBound {
  private CodpieceBranchBound() {}

  static MaximizerBranchBound.InterchangeableSlotGroup create(
      Evaluator evaluator, MaximizerSpeculation baseline, int capacity) {
    var candidates = evaluator.codpiece().branchBoundCandidates();
    var copiesByItem = evaluator.codpiece().branchBoundCounts();
    var slots = List.copyOf(SlotSet.CODPIECE_SLOTS);
    if (candidates.isEmpty()) {
      return new MaximizerBranchBound.InterchangeableSlotGroup(
          slots,
          candidates,
          copiesByItem,
          Map.of(),
          capacity,
          MaximizerBranchBound.CapacityMode.FIXED);
    }

    var projectionBaseline = projectionBaseline(baseline);
    if (projectionBaseline == null) {
      return new MaximizerBranchBound.InterchangeableSlotGroup(
          slots,
          candidates,
          copiesByItem,
          Map.of(),
          capacity,
          MaximizerBranchBound.CapacityMode.FIXED);
    }

    var emptyModifiers = MaximizerBranchBound.calculateProjection(projectionBaseline.clone());
    var projectionDeltas = new HashMap<Integer, Modifiers>();
    for (var item : candidates) {
      var candidateProjections = new ArrayList<Modifiers>();
      for (var slot : slots) {
        var projection = projectionBaseline.clone();
        projection.equipment.put(slot, item);
        candidateProjections.add(MaximizerBranchBound.calculateProjection(projection));
      }
      projectionDeltas.put(
          item.getItemId(),
          MaximizerBranchBound.projectionUpperDelta(emptyModifiers, candidateProjections));
    }

    return new MaximizerBranchBound.InterchangeableSlotGroup(
        slots,
        candidates,
        copiesByItem,
        Map.copyOf(projectionDeltas),
        capacity,
        MaximizerBranchBound.CapacityMode.FIXED);
  }

  static MaximizerBranchBound.InterchangeableSlotGroup remaining(
      MaximizerBranchBound.InterchangeableSlotGroup group,
      List<CheckedItem> candidates,
      int[] available,
      int start,
      int capacity) {
    var copiesByItem = new HashMap<Integer, Integer>();
    for (int index = start; index < candidates.size(); index++) {
      copiesByItem.put(candidates.get(index).getItemId(), Math.min(available[index], capacity));
    }
    return new MaximizerBranchBound.InterchangeableSlotGroup(
        group.slots(),
        candidates.subList(start, candidates.size()),
        Map.copyOf(copiesByItem),
        group.projectionDeltas(),
        capacity,
        MaximizerBranchBound.CapacityMode.FIXED);
  }

  private static MaximizerSpeculation projectionBaseline(MaximizerSpeculation baseline) {
    var projectionBaseline = baseline.clone();
    for (var slot : SlotSet.CODPIECE_SLOTS) {
      projectionBaseline.equipment.put(slot, EquipmentRequest.UNEQUIP);
    }
    if (!KoLCharacter.hasEquipped(
        projectionBaseline.equipment, EquipmentManager.ETERNITY_CODPIECE)) {
      Slot accessory =
          SlotSet.ACCESSORY_SLOTS.stream()
              .filter(
                  slot -> {
                    var item = projectionBaseline.equipment.get(slot);
                    return item == null || item == EquipmentRequest.UNEQUIP;
                  })
              .findFirst()
              .orElse(null);
      if (accessory == null) return null;
      projectionBaseline.equipment.put(accessory, EquipmentManager.ETERNITY_CODPIECE);
    }
    return projectionBaseline;
  }
}
