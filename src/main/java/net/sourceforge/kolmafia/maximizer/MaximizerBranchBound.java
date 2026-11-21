package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;

/**
 * Bounds the boolean modifiers reachable from a partial equipment choice. Independent candidate
 * projections are cached once per familiar; unresolved slots contribute their optimistic union.
 */
final class MaximizerBranchBound {
  private static final List<Slot> OUTFIT_SLOTS =
      List.of(Slot.CONTAINER, Slot.HAT, Slot.SHIRT, Slot.PANTS);

  record OutfitEnvelope(
      EnumSet<BooleanModifier> upper, Map<Slot, Map<Integer, EnumSet<BooleanModifier>>> bySlot) {}

  private record PreparedRole(
      Slot slot,
      Map<Integer, EnumSet<BooleanModifier>> candidates,
      EnumSet<BooleanModifier> possible) {}

  private final EnumSet<BooleanModifier> required;
  private final EnumSet<BooleanModifier> forbidden;
  private final EnumSet<BooleanModifier> baseline;
  private final EnumSet<BooleanModifier> interactions;
  private final OutfitEnvelope outfitEnvelope;
  private final PreparedRole[] roles;
  private final EnumSet<BooleanModifier> lower = EnumSet.noneOf(BooleanModifier.class);
  private final EnumSet<BooleanModifier> upper = EnumSet.noneOf(BooleanModifier.class);
  private final EnumSet<BooleanModifier> outfitScratch = EnumSet.noneOf(BooleanModifier.class);
  private final EnumSet<BooleanModifier> slotScratch = EnumSet.noneOf(BooleanModifier.class);

  private MaximizerBranchBound(
      EnumSet<BooleanModifier> required,
      EnumSet<BooleanModifier> forbidden,
      EnumSet<BooleanModifier> baseline,
      EnumSet<BooleanModifier> interactions,
      OutfitEnvelope outfitEnvelope,
      PreparedRole[] roles) {
    this.required = required;
    this.forbidden = forbidden;
    this.baseline = baseline;
    this.interactions = interactions.clone();
    this.outfitEnvelope = outfitEnvelope;
    this.roles = roles;
  }

  static MaximizerBranchBound create(
      Evaluator evaluator,
      MaximizerSpeculation baseline,
      SlotList<CheckedItem> possibles,
      EnumSet<BooleanModifier> interactions,
      OutfitEnvelope outfitEnvelope) {
    var required = evaluator.booleanRequirements(true);
    var forbidden = evaluator.booleanRequirements(false);
    var mask = required.clone();
    mask.addAll(forbidden);
    if (mask.isEmpty()) return null;

    var projectionBaseline = baseline.clone();
    if (projectionBaseline == null) throw new IllegalStateException("Unable to clone speculation");
    var baselineBooleans = calculateProjection(projectionBaseline.clone()).getBooleans(mask);
    var projections = new EnumMap<Slot, Map<Integer, EnumSet<BooleanModifier>>>(Slot.class);
    var roles = new ArrayList<PreparedRole>();
    for (var slot : SlotSet.SLOTS) {
      if (baseline.equipment.get(slot) != null) continue;
      Slot role = SlotSet.ACCESSORY_SLOTS.contains(slot) ? Slot.ACCESSORY1 : slot;
      var projected = projections.get(role);
      if (projected == null) {
        projected = new HashMap<>();
        projected.put(EquipmentRequest.UNEQUIP.getItemId(), baselineBooleans);
        for (var item : candidates(possibles, role)) {
          var projection = projectionBaseline.clone();
          if (projection == null) throw new IllegalStateException("Unable to clone speculation");
          projection.equipment.put(slot, item);
          projected.put(item.getItemId(), calculateProjection(projection).getBooleans(mask));
        }
        projected = Map.copyOf(projected);
        projections.put(role, projected);
      }
      var possible = EnumSet.noneOf(BooleanModifier.class);
      projected.values().forEach(possible::addAll);
      roles.add(new PreparedRole(slot, projected, possible));
    }
    return new MaximizerBranchBound(
        required,
        forbidden,
        baselineBooleans,
        interactions,
        outfitEnvelope,
        roles.toArray(PreparedRole[]::new));
  }

  boolean cannotComplete(MaximizerSpeculation speculation) {
    this.lower.clear();
    this.lower.addAll(this.baseline);
    this.upper.clear();
    this.upper.addAll(this.baseline);
    for (var role : this.roles) {
      var selected = speculation.equipment.get(role.slot());
      if (selected == null) {
        this.upper.addAll(role.possible());
        continue;
      }
      var candidate = role.candidates().get(selected.getItemId());
      // An outfit can force a piece outside the shortlist. Keep exact traversal for that branch.
      if (candidate == null) return false;
      this.lower.addAll(candidate);
      this.upper.addAll(candidate);
    }
    // Cross-equipment grants may appear or disappear as other choices are resolved. Carry their
    // optimistic allowance without treating a single-item projection as a guarantee.
    this.lower.removeAll(this.interactions);
    this.lower.removeAll(this.outfitEnvelope.upper());
    this.upper.addAll(this.interactions);
    this.outfitScratch.clear();
    this.outfitScratch.addAll(this.outfitEnvelope.upper());
    for (var entry : this.outfitEnvelope.bySlot().entrySet()) {
      var selected = speculation.equipment.get(entry.getKey());
      if (selected == null) continue;
      this.slotScratch.clear();
      var unrestricted = entry.getValue().get(-1);
      if (unrestricted != null) this.slotScratch.addAll(unrestricted);
      var matching = entry.getValue().get(selected.getItemId());
      if (matching != null) this.slotScratch.addAll(matching);
      this.outfitScratch.retainAll(this.slotScratch);
    }
    this.upper.addAll(this.outfitScratch);
    return !this.upper.containsAll(this.required)
        || !Collections.disjoint(this.lower, this.forbidden);
  }

  static OutfitEnvelope outfitEnvelope(Map<Integer, EnumSet<BooleanModifier>> outfitUppers) {
    var upper = EnumSet.noneOf(BooleanModifier.class);
    if (outfitUppers.isEmpty()) return new OutfitEnvelope(upper, Map.of());
    var bySlot = new EnumMap<Slot, Map<Integer, EnumSet<BooleanModifier>>>(Slot.class);
    for (var slot : OUTFIT_SLOTS) bySlot.put(slot, new HashMap<>());
    for (var entry : outfitUppers.entrySet()) {
      if (entry.getValue().isEmpty()) continue;
      var outfit = EquipmentDatabase.getOutfit(entry.getKey());
      if (outfit == null) continue;
      upper.addAll(entry.getValue());
      var required = new EnumMap<Slot, Integer>(Slot.class);
      for (var piece : outfit.getPieces()) {
        Slot slot = EquipmentManager.itemIdToEquipmentType(piece.getItemId());
        if (OUTFIT_SLOTS.contains(slot)) required.put(slot, piece.getItemId());
      }
      for (var slot : OUTFIT_SLOTS) {
        bySlot
            .get(slot)
            .computeIfAbsent(
                required.getOrDefault(slot, -1), ignored -> EnumSet.noneOf(BooleanModifier.class))
            .addAll(entry.getValue());
      }
    }
    return new OutfitEnvelope(upper, Map.copyOf(bySlot));
  }

  static Modifiers calculateProjection(MaximizerSpeculation projection) {
    if (projection == null) throw new IllegalStateException("Unable to clone speculation");
    for (var slot : SlotSet.ALL_SLOTS) {
      projection.equipment.putIfAbsent(slot, EquipmentRequest.UNEQUIP);
    }
    for (var entry : projection.equipment.entrySet()) {
      if (entry.getValue() == null) entry.setValue(EquipmentRequest.UNEQUIP);
    }
    projection.setUnscored();
    return projection.getModifiers();
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
