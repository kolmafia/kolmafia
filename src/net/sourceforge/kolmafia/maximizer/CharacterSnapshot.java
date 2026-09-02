package net.sourceforge.kolmafia.maximizer;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.session.EquipmentManager;

record CharacterSnapshot(
    boolean beecore, int allowedMutexViolations, Map<Slot, AdventureResult> currentEquipment) {

  static CharacterSnapshot capture() {
    var equipment = new EnumMap<Slot, AdventureResult>(Slot.class);
    for (var slot : SlotSet.ALL_SLOTS) {
      equipment.put(slot, EquipmentManager.getEquipment(slot));
    }
    return new CharacterSnapshot(
        KoLCharacter.inBeecore(),
        KoLCharacter.currentRawBitmapModifier(BitmapModifier.MUTEX_VIOLATIONS),
        Collections.unmodifiableMap(equipment));
  }

  int beeosity(Map<Slot, AdventureResult> equipment) {
    return this.beecore ? KoLCharacter.getBeeosity(equipment) : 0;
  }

  int beeosity(String itemName) {
    return this.beecore ? KoLCharacter.getBeeosity(itemName) : 0;
  }
}
