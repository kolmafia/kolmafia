package net.sourceforge.kolmafia.maximizer;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.request.EquipmentRequest;

/**
 * Tracks equipment sources of required booleans to reject impossible partial outfits before further
 * branching.
 */
class BooleanRequirements {
  private final Map<BooleanModifier, EnumSet<Slot>> sources = new EnumMap<>(BooleanModifier.class);

  BooleanRequirements(
      Set<BooleanModifier> required,
      MaximizerSpeculation current,
      SlotList<CheckedItem> possibles,
      AdventureResult bestCard,
      List<FamiliarData> carriedFamiliars,
      FamiliarData crownFamiliar,
      FamiliarData bjornFamiliar)
      throws MaximizerInterruptedException {
    var missing = EnumSet.noneOf(BooleanModifier.class);
    missing.addAll(required);
    if (missing.isEmpty()) return;

    var baseline = withoutUnassignedSlots(current);
    missing.removeAll(calculate(baseline).getBooleans(missing));
    if (missing.isEmpty()) return;

    // Actual speculative modifiers account for modes, intrinsic effects, nested equipment,
    // and familiar-specific equipment behavior, just as the final outfit evaluation does.
    Map<Integer, EnumSet<Slot>> itemSlots = new HashMap<>();
    for (var slot : SlotSet.SLOTS) {
      var fixed = current.equipment.get(slot);
      if (fixed != null) {
        itemSlots.computeIfAbsent(fixed.getItemId(), k -> EnumSet.noneOf(Slot.class)).add(slot);
        continue;
      }
      var candidates =
          new HashSet<>(
              possibles.get(SlotSet.ACCESSORY_SLOTS.contains(slot) ? Slot.ACCESSORY1 : slot));
      if (slot == Slot.OFFHAND) {
        candidates.addAll(possibles.get(Evaluator.OFFHAND_MELEE));
        candidates.addAll(possibles.get(Evaluator.OFFHAND_RANGED));
      }
      for (var item : candidates) {
        itemSlots.computeIfAbsent(item.getItemId(), k -> EnumSet.noneOf(Slot.class)).add(slot);
        var spec = baseline.clone();
        spec.equipment.put(slot, item);
        if (item.getItemId() == ItemPool.CARD_SLEEVE && slot == Slot.OFFHAND) {
          spec.equipment.put(Slot.CARDSLEEVE, bestCard);
        }
        addSources(calculate(spec), missing, EnumSet.of(slot));
        if (item.getItemId() == ItemPool.HATSEAT || item.getItemId() == ItemPool.BUDDY_BJORN) {
          boolean crown = item.getItemId() == ItemPool.HATSEAT;
          var chosen = crown ? crownFamiliar : bjornFamiliar;
          for (var familiar : chosen == null ? carriedFamiliars : List.of(chosen)) {
            if (crown) spec.setEnthroned(familiar);
            else spec.setBjorned(familiar);
            addSources(calculate(spec), missing, EnumSet.of(slot));
          }
        }
      }
    }

    // A combination can supply a boolean even when none of its individual pieces does.
    // Availability deliberately overestimates: conflicting slots or insufficient copies
    // may keep an impossible branch alive, but must never eliminate a possible one.
    for (var outfit : EquipmentDatabase.normalOutfits.values()) {
      addCombination(
          ModifierDatabase.getModifiers(ModifierType.OUTFIT, outfit.getName()),
          Arrays.stream(outfit.getPieces()).map(AdventureResult::getItemId).toList(),
          itemSlots,
          missing);
    }
    for (var synergy : ModifierDatabase.getSynergies()) {
      addCombination(
          ModifierDatabase.getModifiers(ModifierType.SYNERGY, synergy.getKey()),
          Arrays.stream(synergy.getKey().split("/")).map(ItemDatabase::getItemId).toList(),
          itemSlots,
          missing);
    }
  }

  private void addSources(
      Modifiers modifiers, EnumSet<BooleanModifier> missing, EnumSet<Slot> slots) {
    if (modifiers == null) return;
    for (var modifier : modifiers.getBooleans(missing)) {
      sources.computeIfAbsent(modifier, k -> EnumSet.noneOf(Slot.class)).addAll(slots);
    }
  }

  private void addCombination(
      Modifiers modifiers,
      List<Integer> pieces,
      Map<Integer, EnumSet<Slot>> itemSlots,
      EnumSet<BooleanModifier> missing) {
    if (modifiers == null || modifiers.getBooleans(missing).isEmpty()) return;
    var slots = EnumSet.noneOf(Slot.class);
    for (var piece : pieces) {
      var locations = itemSlots.get(piece);
      if (locations == null) return;
      slots.addAll(locations);
    }
    addSources(modifiers, missing, slots);
  }

  private static MaximizerSpeculation withoutUnassignedSlots(MaximizerSpeculation spec) {
    var copy = spec.clone();
    copy.equipment.replaceAll((slot, item) -> item == null ? EquipmentRequest.UNEQUIP : item);
    return copy;
  }

  private static Modifiers calculate(MaximizerSpeculation spec)
      throws MaximizerInterruptedException {
    MaximizerSpeculation.countEvaluation();
    return spec.calculate();
  }

  boolean possible(MaximizerSpeculation spec, Slot assigned) throws MaximizerInterruptedException {
    if (sources.isEmpty()) return true;
    var check = EnumSet.noneOf(BooleanModifier.class);
    for (var source : sources.entrySet()) {
      var slots = source.getValue();
      if ((slots.contains(assigned)
              || (assigned == Slot.ACCESSORY1
                  && slots.stream().anyMatch(SlotSet.ACCESSORY_SLOTS::contains)))
          && slots.stream().allMatch(slot -> spec.equipment.get(slot) != null)) {
        check.add(source.getKey());
      }
    }
    return check.isEmpty()
        || calculate(withoutUnassignedSlots(spec)).getBooleans(check).containsAll(check);
  }
}
