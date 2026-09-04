package net.sourceforge.kolmafia.maximizer;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.session.EquipmentManager;

/**
 * Character facts that must remain stable while one equipment search is running.
 *
 * <p>Candidate evaluation uses this snapshot instead of repeatedly consulting mutable global
 * character state. Shared-resource definitions live here so every loadout measures resources in the
 * same order.
 */
record CharacterSnapshot(
    int allowedMutexViolations,
    Map<Slot, AdventureResult> currentEquipment,
    List<SharedResource> resources) {

  static CharacterSnapshot capture() {
    return capture(null);
  }

  static CharacterSnapshot capture(Evaluator evaluator) {
    var equipment = new EnumMap<Slot, AdventureResult>(Slot.class);
    for (var slot : SlotSet.ALL_SLOTS) {
      equipment.put(slot, EquipmentManager.getEquipment(slot));
    }
    List<SharedResource> resources =
        KoLCharacter.inBeecore()
            ? List.of(
                new SharedResource(
                    "beeosity",
                    evaluator == null ? 2 : evaluator.beeosityLimit(),
                    KoLCharacter::getBeeosity,
                    KoLCharacter::getBeeosity))
            : List.of();
    return new CharacterSnapshot(
        KoLCharacter.currentRawBitmapModifier(BitmapModifier.MUTEX_VIOLATIONS),
        Collections.unmodifiableMap(equipment),
        resources);
  }

  ResourceUsage resourceUsage(Map<Slot, AdventureResult> equipment) {
    return ResourceUsage.forEquipment(this.resources, equipment);
  }

  ResourceUsage resourceUsage(String itemName) {
    return ResourceUsage.forItem(this.resources, itemName);
  }

  boolean resourcesExceeded(ResourceUsage usage) {
    return usage.exceeds(this.resources);
  }

  boolean hasRemainingCapacityFor(ResourceUsage usage, ResourceUsage candidate) {
    return usage.hasRemainingCapacityFor(candidate, this.resources);
  }
}
