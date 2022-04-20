package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.isDay;
import static internal.helpers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    BanishManager.clearCache();
  }

  private static AdventureResult MAGNIFICENT_OYSTER_EGG =
      ItemPool.get(ItemPool.MAGNIFICENT_OYSTER_EGG);

  @Test
  public void obtainOysterEggAppropriately() {
    HolidayDatabase.guessPhaseStep();
    EquipmentManager.setEquipment(EquipmentManager.OFFHAND, ItemPool.get(ItemPool.OYSTER_BASKET));
    // This was an Oyster Egg Day.
    final var cleanups = isDay(new GregorianCalendar(2022, 0, 29, 12, 0));
    try (cleanups) {
      ResultProcessor.processResult(true, MAGNIFICENT_OYSTER_EGG);
    }
  }

  @Test
  public void obtainOysterEggOnWrongDay() {
    EquipmentManager.setEquipment(EquipmentManager.OFFHAND, ItemPool.get(ItemPool.OYSTER_BASKET));
    // This was not an Oyster Egg Day.
    final var cleanups = isDay(new GregorianCalendar(2022, 0, 30, 12, 0));
    try (cleanups) {
      ResultProcessor.processResult(true, MAGNIFICENT_OYSTER_EGG);
      assertEquals(Preferences.getInteger("_oysterEggsFound"), 0);
    }
  }

  @Test
  public void obtainOysterEggWithoutBasket() {
    // This was an Oyster Egg Day.
    final var cleanups = isDay(new GregorianCalendar(2022, 0, 29, 12, 0));
    try (cleanups) {
      ResultProcessor.processResult(true, MAGNIFICENT_OYSTER_EGG);
      assertEquals(Preferences.getInteger("_oysterEggsFound"), 0);
    }
  }

  @Test
  public void obtainOysterEggOnWrongDayAndWithoutBasket() {
    // This was not an Oyster Egg Day.
    final var cleanups = isDay(new GregorianCalendar(2022, 0, 30, 12, 0));
    try (cleanups) {
      ResultProcessor.processResult(true, MAGNIFICENT_OYSTER_EGG);
      assertEquals(Preferences.getInteger("_oysterEggsFound"), 0);
    }
  }

  private static AdventureResult COSMIC_BOWLING_BALL = ItemPool.get(ItemPool.COSMIC_BOWLING_BALL);

  @Test
  public void gettingCosmicBowlingBallInCombatResetsBanishes() {
    Preferences.setInteger("cosmicBowlingBallReturnCombats", 20);
    BanishManager.banishMonster("zmobie", BanishManager.Banisher.BOWL_A_CURVEBALL);
    assertTrue(BanishManager.isBanished("zmobie"));

    ResultProcessor.processResult(true, COSMIC_BOWLING_BALL);

    assertFalse(BanishManager.isBanished("zmobie"));
  }

  @Test
  public void gettingCosmicBowlingBallInCombatResetsReturnCombats() {
    Preferences.setInteger("cosmicBowlingBallReturnCombats", 20);
    BanishManager.banishMonster("zmobie", BanishManager.Banisher.BOWL_A_CURVEBALL);
    ResultProcessor.processResult(true, COSMIC_BOWLING_BALL);

    assertThat("cosmicBowlingBallReturnCombats", isSetTo(-1));
  }
}
