package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.isDay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.GregorianCalendar;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ResultProcessorTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("ResultProcessorTest");
    Preferences.reset("ResultProcessorTest");
  }

  @Test
  public void obtainOysterEggAppropriatelyTest() {
    HolidayDatabase.guessPhaseStep();
    EquipmentManager.setEquipment(EquipmentManager.OFFHAND, ItemPool.get(ItemPool.OYSTER_BASKET));
    final var cleanups = isDay(new GregorianCalendar(2022, 1, 29, 12, 0));
    try (cleanups) {
      assertTrue(HolidayDatabase.getHoliday().contains("Oyster Egg Day"));
      ResultProcessor.processResult(true, new AdventureResult(5, "magnificent oyster egg", 1));
      // assertEquals(Preferences.getInteger("_oysterEggsFound"), 1);
    }
  }

  @Test
  public void obtainOysterEggOnWrongDayTest() {
    EquipmentManager.setEquipment(EquipmentManager.OFFHAND, ItemPool.get(ItemPool.OYSTER_BASKET));
    ResultProcessor.processResult(true, new AdventureResult(5, "magnificent oyster egg", 1));
    assertEquals(Preferences.getInteger("_oysterEggsFound"), 0);
  }

  @Test
  public void obtainOysterEggWithoutBasketTest() {
    final var cleanups = isDay(new GregorianCalendar(2022, 1, 29, 12, 0));
    try (cleanups) {
      ResultProcessor.processResult(true, new AdventureResult(5, "magnificent oyster egg", 1));
      assertEquals(Preferences.getInteger("_oysterEggsFound"), 0);
    }
  }

  @Test
  public void obtainOysterEggOnWrongDayAndWithoutBasketTest() {
    ResultProcessor.processResult(true, new AdventureResult(5, "magnificent oyster egg", 1));
    assertEquals(Preferences.getInteger("_oysterEggsFound"), 0);
  }
}
