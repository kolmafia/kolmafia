package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withDay;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withFullness;
import static internal.helpers.Player.withInebriety;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import java.time.Month;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class DrinkItemRequestTest {
  @BeforeAll
  public static void beforeAll() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("DrinkItemRequest");
  }

  @Test
  public void drinkingVintnerWineResetsVinterCharges() {
    var cleanups =
        new Cleanups(
            withFamiliarInTerrarium(FamiliarPool.VAMPIRE_VINTNER),
            withProperty("vintnerCharge", 13));
    try (cleanups) {
      DrinkItemRequest.parseConsumption(
          ItemPool.get(ItemPool.VAMPIRE_VINTNER_WINE),
          null,
          html("request/test_drink_vintner_wine.html"),
          false);
      assertThat("vintnerCharge", isSetTo(0));

      var vintner = new FamiliarData(FamiliarPool.VAMPIRE_VINTNER);
      assertThat(vintner.getCharges(), equalTo(0));
    }
  }

  @Nested
  class GetsYouDrunk {
    @Test
    public void tracksSuccessfulConsumption() {
      var cleanups =
          new Cleanups(withProperty("getsYouDrunkTurnsLeft"), withItem(ItemPool.GETS_YOU_DRUNK));
      try (cleanups) {
        var req = new DrinkItemRequest(ItemPool.get(ItemPool.GETS_YOU_DRUNK));
        req.responseText = html("request/test_drink_gets_you_drunk_success.html");
        req.processResults();
        assertThat("getsYouDrunkTurnsLeft", isSetTo(4));
        assertThat(InventoryManager.getCount(ItemPool.GETS_YOU_DRUNK), is(0));
      }
    }

    @Test
    public void tracksUnsuccessfulConsumption() {
      var cleanups =
          new Cleanups(withProperty("getsYouDrunkTurnsLeft", 3), withItem(ItemPool.GETS_YOU_DRUNK));
      try (cleanups) {
        var req = new DrinkItemRequest(ItemPool.get(ItemPool.GETS_YOU_DRUNK));
        req.responseText = html("request/test_drink_gets_you_drunk_failure.html");
        req.processResults();
        assertThat("getsYouDrunkTurnsLeft", isSetTo(3));
        assertThat(InventoryManager.getCount(ItemPool.GETS_YOU_DRUNK), is(1));
      }
    }

    @Test
    public void guessesOldTimer() {
      var cleanups = withProperty("getsYouDrunkTurnsLeft", 0);
      try (cleanups) {
        var req = new DrinkItemRequest(ItemPool.get(ItemPool.GETS_YOU_DRUNK));
        req.responseText = html("request/test_drink_gets_you_drunk_failure.html");
        req.processResults();
        assertThat("getsYouDrunkTurnsLeft", isSetTo(4));
      }
    }

    @Test
    public void maximumUsesOneNormally() {
      var cleanups = withProperty("getsYouDrunkTurnsLeft", 0);
      try (cleanups) {
        var uses = DrinkItemRequest.maximumUses(ItemPool.GETS_YOU_DRUNK);
        assertThat(uses, is(1));
      }
    }

    @Test
    public void maximumUsesZeroWhenTimerGoing() {
      var cleanups = withProperty("getsYouDrunkTurnsLeft", 1);
      try (cleanups) {
        var uses = DrinkItemRequest.maximumUses(ItemPool.GETS_YOU_DRUNK);
        assertThat(uses, is(0));
      }
    }
  }

  @Test
  public void setsDailyLimitOnSuccessfulConsumption() {
    var cleanups =
        new Cleanups(
            withProperty("_pickleJuiceDrunk", false), withItem(ItemPool.FERMENTED_PICKLE_JUICE));
    try (cleanups) {
      var req = new DrinkItemRequest(ItemPool.get(ItemPool.FERMENTED_PICKLE_JUICE));
      req.responseText = html("request/test_drink_pickle_juice_success.html");
      req.processResults();
      assertThat("_pickleJuiceDrunk", isSetTo(true));
    }
  }

  @Test
  public void setsDailyLimitOnFailedConsumption() {
    var cleanups =
        new Cleanups(
            withProperty("_pickleJuiceDrunk", false), withItem(ItemPool.FERMENTED_PICKLE_JUICE));
    try (cleanups) {
      var req = new DrinkItemRequest(ItemPool.get(ItemPool.FERMENTED_PICKLE_JUICE));
      req.responseText = html("request/test_drink_pickle_juice_limit_failure.html");
      req.processResults();
      assertThat("_pickleJuiceDrunk", isSetTo(true));
    }
  }

  @Test
  public void trackUseOfCinchoSaltAndLime() {
    var cleanups =
        new Cleanups(withProperty("cinchoSaltAndLime", 2), withItem(ItemPool.VODKA_MARTINI));
    try (cleanups) {
      var req = new DrinkItemRequest(ItemPool.get(ItemPool.VODKA_MARTINI));
      req.responseText = html("request/test_drink_with_salt_and_lime.html");
      req.processResults();
      assertThat("cinchoSaltAndLime", isSetTo(1));
      assertThat(InventoryManager.getCount(ItemPool.VODKA_MARTINI), is(0));
    }
  }

  @Nested
  class MaximumUses {
    @ParameterizedTest
    @CsvSource({
      ItemPool.STEEL_LIVER + ", true",
      ItemPool.MEDIOCRE_LAGER + ", true",
      ItemPool.VODKA_DOG + ", true",
      ItemPool.BOTTLE_OF_WINE + ", false",
    })
    void jarlsbergOnlyAllowsCertainBooze(final int itemId, final boolean allowed) {
      try (var cleanups =
          new Cleanups(
              withPath(Path.AVATAR_OF_JARLSBERG),
              withClass(AscensionClass.AVATAR_OF_JARLSBERG),
              withInebriety(0))) {
        assertThat(DrinkItemRequest.maximumUses(itemId), allowed ? greaterThan(0) : is(0));
        if (!allowed) {
          assertThat(DrinkItemRequest.limiter, is("its non-Jarlsbergian nature"));
        }
      }
    }

    @ParameterizedTest
    @CsvSource({
      ItemPool.STEEL_LIVER + ", true",
      ItemPool.FRUITY_WINE + ", true",
      ItemPool.BOTTLE_OF_WINE + ", false",
    })
    void kolhsOnlyAllowsCertainBooze(final int itemId, final boolean allowed) {
      try (var cleanups = new Cleanups(withPath(Path.KOLHS), withInebriety(0))) {
        assertThat(DrinkItemRequest.maximumUses(itemId), allowed ? greaterThan(0) : is(0));
        if (!allowed) {
          assertThat(DrinkItemRequest.limiter, is("your unrefined palate"));
        }
      }
    }

    @Test
    void vampyresCanOnlyDrinkBloodBooze() {
      try (var cleanups =
          new Cleanups(
              withPath(Path.DARK_GYFFTE), withClass(AscensionClass.VAMPYRE), withInebriety(0))) {
        assertThat(DrinkItemRequest.maximumUses(ItemPool.VAMPAGNE), is(5));
        assertThat(DrinkItemRequest.maximumUses(ItemPool.PERFECT_NEGRONI), is(0));
        assertThat(DrinkItemRequest.limiter, is("your lust for blood"));
      }
    }

    @Test
    void nonVampyresCannotDrinkBloodBooze() {
      try (var cleanups = new Cleanups(withInebriety(0))) {
        assertThat(DrinkItemRequest.maximumUses(ItemPool.VAMPAGNE), is(0));
        assertThat(DrinkItemRequest.limiter, is("not being a Vampyre"));
      }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void nuclearAutumnLimitsBoozeSizeToOne(final boolean inNA) {
      var cleanups = new Cleanups(withInebriety(0));
      if (inNA) cleanups.add(withPath(Path.NUCLEAR_AUTUMN));
      try (cleanups) {
        assertThat(
            DrinkItemRequest.maximumUses(ItemPool.GREEN_BEER, "green beer", 1, false),
            is(inNA ? 2 : 14));
        if (inNA) {
          assertThat(
              DrinkItemRequest.maximumUses(
                  ItemPool.DUSTY_BOTTLE_OF_MARSALA, "dusty bottle of Marsala", 2, false),
              is(0));
          assertThat(DrinkItemRequest.limiter, is("your narrow, mutated throat"));
        }
      }
    }

    @Test
    void ltaCanOnlyDrinkMartini() {
      var cleanups = new Cleanups(withPath(Path.LICENSE_TO_ADVENTURE), withInebriety(0));
      try (cleanups) {
        assertThat(DrinkItemRequest.maximumUses(ItemPool.MARTINI), is(2));
        assertThat(DrinkItemRequest.maximumUses(ItemPool.BOTTLE_OF_GIN), is(0));
        assertThat(DrinkItemRequest.limiter, is("it neither being shaken nor stirred"));
      }
    }

    @Test
    void correctlyIdentifyWhenInebrietyIsCausingLimit() {
      try (var cleanups =
          new Cleanups(
              withInebriety(11),
              withProperty("_speakeasyDrinksDrunk", 1),
              withItem(ItemPool.VIP_LOUNGE_KEY))) {
        var max = DrinkItemRequest.maximumUses(ItemPool.BEES_KNEES, "Bee's Knees", 2, false);
        assertThat(max, is(1));
        assertThat(DrinkItemRequest.limiter, is("inebriety"));
      }
    }

    @Test
    void correctlyIdentifyWhenDailyLimitIsCausingLimit() {
      try (var cleanups =
          new Cleanups(
              withInebriety(11),
              withProperty("_speakeasyDrinksDrunk", 3),
              withItem(ItemPool.VIP_LOUNGE_KEY))) {
        var max = DrinkItemRequest.maximumUses(ItemPool.BEES_KNEES, "Bee's Knees", 2, false);
        assertThat(max, is(0));
        assertThat(DrinkItemRequest.limiter, is("daily limit"));
      }
    }

    @Test
    void abilityToDrinkDrunkiBearIsNotAffectedByInebriety() {
      try (var cleanups = new Cleanups(withInebriety(10), withFullness(0))) {
        var max =
            DrinkItemRequest.maximumUses(ItemPool.GREEN_DRUNKI_BEAR, "green drunki-bear", 4, false);
        assertThat(max, is(3));
      }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void limitExtendedForGreenBeerOnSSPD(final boolean sspd) {
      try (var cleanups = new Cleanups(withDay(2023, Month.MAY, sspd ? 17 : 1), withInebriety(0))) {
        assertThat(DrinkItemRequest.maximumUses(ItemPool.GREEN_BEER), is(sspd ? 25 : 15));
      }
    }
  }
}
