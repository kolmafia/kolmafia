package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EatItemRequestTest {
  @BeforeEach
  public void beforeEach() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("EatItemRequest");
    Preferences.reset("EatItemRequest");
  }

  @Nested
  class MilkOfMagnesium {
    private Cleanups cleanups = new Cleanups();

    @BeforeEach
    public void beforeEach() {
      cleanups.add(withItem(ItemPool.MILK_OF_MAGNESIUM));
      cleanups.add(withItem(ItemPool.TOMATO));
    }

    @AfterEach
    public void afterEach() {
      cleanups.close();
    }

    @Test
    void skipMilkNagIfAlreadyUsedToday() {
      assertFalse(Preferences.getBoolean("_milkOfMagnesiumUsed"));
      assertTrue(EatItemRequest.askAboutMilk("tomato", 1));
    }

    @Test
    void skipMilkNagIfAlreadyActive() {
      Preferences.setBoolean("milkOfMagnesiumActive", true);
      assertTrue(EatItemRequest.askAboutMilk("tomato", 1));
    }

    @Test
    void milkResponseSetsPreference() {
      assertFalse(Preferences.getBoolean("_milkOfMagnesiumUsed"));
      Preferences.setBoolean("milkOfMagnesiumActive", true);

      var req = new EatItemRequest(ItemPool.get(ItemPool.TOMATO));
      req.responseText = "Satisfied, you let loose a nasty magnesium-flavored belch.";
      req.processResults();

      assertFalse(Preferences.getBoolean("_milkOfMagnesiumUsed"));
      assertFalse(Preferences.getBoolean("milkOfMagnesiumActive"));
    }
  }

  @Nested
  class Cookbookbat {
    @ParameterizedTest
    @CsvSource({
      "10991, pizzaOfLegendEaten",
      "10992, calzoneOfLegendEaten",
      "11000, deepDishOfLegendEaten"
    })
    public void canTrackCookbookbatFoodsSuccess(Integer itemId, String prefname) {
      var cleanups = withProperty(prefname, false);
      try (cleanups) {
        assertFalse(Preferences.getBoolean(prefname));
        var req = new EatItemRequest(ItemPool.get(itemId));
        req.responseText = "";
        req.processResults();
        assertTrue(Preferences.getBoolean(prefname));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "10991, pizzaOfLegendEaten",
      "10992, calzoneOfLegendEaten",
      "11000, deepDishOfLegendEaten"
    })
    public void canTrackCookbookbatFoodsFailure(Integer itemId, String prefname) {
      var cleanups = withProperty(prefname, false);
      try (cleanups) {
        assertFalse(Preferences.getBoolean(prefname));
        var req = new EatItemRequest(ItemPool.get(itemId));
        req.responseText = "You may only eat one of those per lifetime";
        req.processResults();
        assertTrue(Preferences.getBoolean(prefname));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "10991, pizzaOfLegendEaten",
      "10992, calzoneOfLegendEaten",
      "11000, deepDishOfLegendEaten"
    })
    public void canPredictCookbookbatFoodsLimit(Integer itemId, String prefname) {
      var cleanups = withProperty(prefname, false);
      try (cleanups) {
        assertEquals(1, EatItemRequest.maximumUses(itemId));
        Preferences.setBoolean(prefname, true);
        assertEquals(0, EatItemRequest.maximumUses(itemId));
      }
    }
  }

  @Nested
  class GhostPepper {
    @Test
    public void tracksSuccessfulConsumption() {
      var cleanups =
          new Cleanups(withProperty("ghostPepperTurnsLeft"), withItem(ItemPool.GHOST_PEPPER));
      try (cleanups) {
        var req = new EatItemRequest(ItemPool.get(ItemPool.GHOST_PEPPER));
        req.responseText = html("request/test_eat_ghost_pepper_success.html");
        req.processResults();
        assertThat("ghostPepperTurnsLeft", isSetTo(4));
        assertThat(InventoryManager.getCount(ItemPool.GHOST_PEPPER), is(0));
      }
    }

    @Test
    public void tracksUnsuccessfulConsumption() {
      var cleanups =
          new Cleanups(withProperty("ghostPepperTurnsLeft", 3), withItem(ItemPool.GHOST_PEPPER));
      try (cleanups) {
        var req = new EatItemRequest(ItemPool.get(ItemPool.GHOST_PEPPER));
        req.responseText = html("request/test_eat_ghost_pepper_failure.html");
        req.processResults();
        assertThat("ghostPepperTurnsLeft", isSetTo(3));
        assertThat(InventoryManager.getCount(ItemPool.GHOST_PEPPER), is(1));
      }
    }

    @Test
    public void guessesOldTimer() {
      var cleanups = withProperty("ghostPepperTurnsLeft", 0);
      try (cleanups) {
        var req = new EatItemRequest(ItemPool.get(ItemPool.GHOST_PEPPER));
        req.responseText = html("request/test_eat_ghost_pepper_failure.html");
        req.processResults();
        assertThat("ghostPepperTurnsLeft", isSetTo(4));
      }
    }

    @Test
    public void maximumUsesOneNormally() {
      var cleanups = withProperty("ghostPepperTurnsLeft", 0);
      try (cleanups) {
        var uses = EatItemRequest.maximumUses(ItemPool.GHOST_PEPPER);
        assertThat(uses, is(1));
      }
    }

    @Test
    public void maximumUsesZeroWhenTimerGoing() {
      var cleanups = withProperty("ghostPepperTurnsLeft", 1);
      try (cleanups) {
        var uses = EatItemRequest.maximumUses(ItemPool.GHOST_PEPPER);
        assertThat(uses, is(0));
      }
    }
  }

  @ParameterizedTest
  @CsvSource({
    "You're too full to eat that., false",
    "You slather the jelly all over your skin. You feel all stinky and bothered and you're sure nobody is coming near you. , true"
  })
  public void stenchJellySetsNCForcerFlag(String responseText, String result) {
    var cleanups = withProperty("noncombatForcerActive", false);
    try (cleanups) {
      var req = new EatItemRequest(ItemPool.get(ItemPool.STENCH_TOAST));
      req.responseText = responseText;
      req.processResults();
      assertThat("noncombatForcerActive", isSetTo(Boolean.parseBoolean(result)));
    }
  }
}
