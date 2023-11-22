package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withFullness;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath;
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
import org.junit.jupiter.params.provider.ValueSource;

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

  @Nested
  class MaximumUses {
    @ParameterizedTest
    @CsvSource({
      ItemPool.CONSUMMATE_BAGEL + ", true",
      ItemPool.TOAST + ", false",
    })
    void jarlsbergOnlyAllowsCertainFood(final int itemId, final boolean allowed) {
      try (var cleanups =
          new Cleanups(
              withPath(AscensionPath.Path.AVATAR_OF_JARLSBERG),
              withClass(AscensionClass.AVATAR_OF_JARLSBERG),
              withFullness(0))) {
        assertThat(EatItemRequest.maximumUses(itemId), allowed ? greaterThan(0) : is(0));
        if (!allowed) {
          assertThat(EatItemRequest.limiter, is("its non-Jarlsbergian nature"));
        }
      }
    }

    @ParameterizedTest
    @CsvSource({
      ItemPool.STEEL_STOMACH + ", true",
      ItemPool.BOSS_BRAIN + ", true",
      ItemPool.TOAST + ", false",
    })
    void zombieOnlyAllowsCertainFood(final int itemId, final boolean allowed) {
      try (var cleanups =
          new Cleanups(
              withPath(AscensionPath.Path.ZOMBIE_SLAYER),
              withClass(AscensionClass.ZOMBIE_MASTER),
              withFullness(0))) {
        assertThat(EatItemRequest.maximumUses(itemId), allowed ? greaterThan(0) : is(0));
        if (!allowed) {
          assertThat(EatItemRequest.limiter, is("it not being a brain"));
        }
      }
    }

    @Test
    void vampyresCanOnlyEatBloodFood() {
      try (var cleanups =
          new Cleanups(
              withPath(AscensionPath.Path.DARK_GYFFTE),
              withClass(AscensionClass.VAMPYRE),
              withFullness(0))) {
        assertThat(DrinkItemRequest.maximumUses(ItemPool.BLOOD_SNOWCONE), is(5));
        assertThat(DrinkItemRequest.maximumUses(ItemPool.MAGICAL_SAUSAGE), is(23));
        assertThat(DrinkItemRequest.maximumUses(ItemPool.TOAST), is(0));
        assertThat(DrinkItemRequest.limiter, is("your lust for blood"));
      }
    }

    @Test
    void nonVampyresCannotEatBloodFood() {
      try (var cleanups = new Cleanups(withFullness(0))) {
        assertThat(DrinkItemRequest.maximumUses(ItemPool.BLOOD_SNOWCONE), is(0));
        assertThat(DrinkItemRequest.limiter, is("not being a Vampyre"));
      }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void nuclearAutumnLimitsFoodSizeToOne(final boolean inNA) {
      var cleanups = new Cleanups(withFullness(0));
      if (inNA) cleanups.add(withPath(AscensionPath.Path.NUCLEAR_AUTUMN));
      try (cleanups) {
        assertThat(EatItemRequest.maximumUses(ItemPool.TOAST), is(inNA ? 3 : 15));
        if (inNA) {
          assertThat(EatItemRequest.maximumUses(ItemPool.BROWSER_COOKIE), is(0));
          assertThat(DrinkItemRequest.limiter, is("your narrow, mutated throat"));
        }
      }
    }

    @Test
    void magicalSausagesDayLimited() {
      try (var cleanups = withProperty("_sausagesEaten", 3)) {
        assertThat(EatItemRequest.maximumUses(ItemPool.MAGICAL_SAUSAGE), is(20));
      }
    }

    @Test
    void cbbFoodsAreAscensionLimited() {
      try (var cleanups = withProperty("deepDishOfLegendEaten", true)) {
        assertThat(EatItemRequest.maximumUses(ItemPool.DEEP_DISH_OF_LEGEND), is(0));
        assertThat(EatItemRequest.limiter, is("lifetime limit"));
      }
    }
  }
}
