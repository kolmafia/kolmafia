package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.isDay;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.GregorianCalendar;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public class ResultProcessorTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("ResultProcessorTest");
    Preferences.reset("ResultProcessorTest");
    FightRequest.clearInstanceData();
  }

  @Test
  public void obtainOysterEggAppropriatelyTest() {
    EquipmentManager.setEquipment(EquipmentManager.OFFHAND, ItemPool.get(ItemPool.OYSTER_BASKET));
    final var cleanups = isDay(new GregorianCalendar(2022, 1, 29, 12, 0));
    try (cleanups) {
      ResultProcessor.processResult(new AdventureResult("magnificent oyster egg", 1));
      assertEquals(Preferences.getInteger("_oysterEggsFound"), 1);
    }
  }

  @Test
  public void obtainOysterEggOnWrongDayTest() {
    EquipmentManager.setEquipment(EquipmentManager.OFFHAND, ItemPool.get(ItemPool.OYSTER_BASKET));
    ResultProcessor.processResult(new AdventureResult("magnificent oyster egg", 1));
    assertEquals(Preferences.getInteger("_oysterEggsFound"), 0);
  }

  @Test
  public void obtainOysterEggWithoutBasketTest() {
    final var cleanups = isDay(new GregorianCalendar(2022, 1, 29, 12, 0));
    try (cleanups) {
      ResultProcessor.processResult(new AdventureResult("magnificent oyster egg", 1));
      assertEquals(Preferences.getInteger("_oysterEggsFound"), 0);
    }
  }

  @Test
  public void obtainOysterEggOnWrongDayAndWithoutBasketTest() {
    ResultProcessor.processResult(new AdventureResult("magnificent oyster egg", 1));
    assertEquals(Preferences.getInteger("_oysterEggsFound"), 0);
  }
}
