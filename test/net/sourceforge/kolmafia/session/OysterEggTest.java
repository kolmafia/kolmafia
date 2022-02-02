package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.isDay;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.GregorianCalendar;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public class OysterEggTest {
  public void OysterEggObtainingTest() {
    KoLCharacter.reset("the Tristero");
    ResultProcessor.processResult(new AdventureResult("magnificent oyster egg", 1));
    assertEquals(Preferences.getInteger("_oysterEggsFound"), 0);
    EquipmentManager.setEquipment(EquipmentManager.OFFHAND, ItemPool.get(ItemPool.OYSTER_BASKET));
    ResultProcessor.processResult(new AdventureResult("magnificent oyster egg", 1));
    assertEquals(Preferences.getInteger("_oysterEggsFound"), 0);
    final var cleanups = isDay(new GregorianCalendar(2022, 1, 29, 12, 0));
    try (cleanups) {
      ResultProcessor.processResult(new AdventureResult("magnificent oyster egg", 1));
      assertEquals(Preferences.getInteger("_oysterEggsFound"), 1);
      EquipmentManager.setEquipment(EquipmentManager.OFFHAND, ItemPool.get(ItemPool.SEAL_CLUB));
      ResultProcessor.processResult(new AdventureResult("magnificent oyster egg", 1));
      assertEquals(Preferences.getInteger("_oysterEggsFound"), 1);
    }
  }
}
