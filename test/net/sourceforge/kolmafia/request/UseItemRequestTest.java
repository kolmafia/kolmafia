package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFullness;
import static internal.helpers.Player.withHandlingChoice;
import static internal.helpers.Player.withInebriety;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withLimitMode;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSpleenUse;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junitpioneer.jupiter.cartesian.CartesianTest.Enum;
import static org.junitpioneer.jupiter.cartesian.CartesianTest.Values;

import internal.helpers.Cleanups;
import internal.network.FakeHttpResponse;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.LimitMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.cartesian.CartesianTest;

class UseItemRequestTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("UseItemRequestTest");
    Preferences.reset("UseItemRequestTest");
  }

  @Nested
  class LimitModes {
    @CartesianTest
    void maxZeroIfInLimitMode(
        @Enum(names = {"ASTRAL", "BIRD", "MOLE", "ROACH"}) LimitMode lm,
        @Values(ints = {ItemPool.ASTRAL_MUSHROOM, ItemPool.GONG}) int itemId) {
      var cleanups = new Cleanups(withLimitMode(lm), withItem(itemId));
      try (cleanups) {
        assertEquals(0, UseItemRequest.maximumUses(itemId));
      }
    }

    @ParameterizedTest
    @ValueSource(ints = {ItemPool.ASTRAL_MUSHROOM, ItemPool.GONG})
    void maxOneIfNotInLimitMode(int itemId) {
      var cleanups = new Cleanups(withLimitMode(LimitMode.NONE), withItem(itemId));
      try (cleanups) {
        assertEquals(1, UseItemRequest.maximumUses(itemId));
      }
    }
  }

  @Nested
  class Milk {
    private UseItemRequest getUseMilkRequest() {
      return UseItemRequest.getInstance(ItemPool.MILK_OF_MAGNESIUM);
    }

    @Test
    void successfulMilkUsageSetsPreferences() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.MILK_OF_MAGNESIUM),
              withProperty("_milkOfMagnesiumUsed", false),
              withProperty("milkOfMagnesiumActive", false),
              // Wiki claims that this message is indeed "You stomach ..."
              withNextResponse(200, "You stomach immediately begins to churn"));

      try (cleanups) {
        var req = getUseMilkRequest();
        req.run();

        assertThat("_milkOfMagnesiumUsed", isSetTo(true));
        assertThat("milkOfMagnesiumActive", isSetTo(true));
      }
    }

    @Test
    void unsuccessfulMilkUsageSetsPreference() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.MILK_OF_MAGNESIUM),
              withProperty("_milkOfMagnesiumUsed", false),
              withNextResponse(200, "it was pretty hard on the old gullet."));
      try (cleanups) {
        UseItemRequest req = getUseMilkRequest();
        req.run();
        assertThat("_milkOfMagnesiumUsed", isSetTo(true));
      }
    }

    @Test
    void milkPreferencePreventsWastedServerHit() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.MILK_OF_MAGNESIUM), withProperty("_milkOfMagnesiumUsed", true));
      try (cleanups) {
        Preferences.setBoolean("_milkOfMagnesiumUsed", true);

        UseItemRequest req = getUseMilkRequest();
        req.run();

        assertThat("_milkOfMagnesiumUsed", isSetTo(true));
      }
    }
  }

  @Nested
  class GreyYou {
    @Test
    void allConsumablesAreMaxUseOneInGreyYou() {
      var cleanups = new Cleanups(withClass(AscensionClass.GREY_GOO));

      try (cleanups) {
        assertThat(UseItemRequest.maximumUses(ItemPool.GRAPEFRUIT), equalTo(1));
        assertThat(UseItemRequest.maximumUses(ItemPool.COLD_WAD), equalTo(1));
        assertThat(UseItemRequest.maximumUses(ItemPool.MUSCHAT), equalTo(1));
      }
    }

    @Test
    void greyYouGivesWarningOnGcliWhenAlreadyAbsorbed() {
      // Lemon
      var cleanups = new Cleanups(withClass(AscensionClass.GREY_GOO));

      try (cleanups) {
        var req = UseItemRequest.getInstance(332);
        req.responseText = html("request/test_eat_already_absorbed.html");
        req.processResults();

        assertThat(UseItemRequest.lastUpdate, containsString("already absorbed"));
        assertThat(StaticEntity.getContinuationState(), equalTo(KoLConstants.MafiaState.ERROR));
      }

      KoLmafia.forceContinue();
    }

    @Test
    void greyYouGivesNoWarningWhenAbsorbed() {
      // Lemon
      var cleanups = new Cleanups(withClass(AscensionClass.GREY_GOO));

      try (cleanups) {
        var req = UseItemRequest.getInstance(332);
        req.responseText = html("request/test_eat_absorb_lemon.html");
        req.processResults();

        assertThat(UseItemRequest.lastUpdate, equalTo(""));
        assertThat(StaticEntity.getContinuationState(), equalTo(KoLConstants.MafiaState.CONTINUE));
      }
    }
  }

  @Nested
  class PileOfUselessRobotParts {
    @Test
    void incrementsPrefWhenPartsUsed() {
      var cleanups =
          new Cleanups(
              withProperty("homemadeRobotUpgrades", 2), withFamiliar(FamiliarPool.HOMEMADE_ROBOT));

      try (cleanups) {
        var fam = KoLCharacter.getFamiliar();
        fam.setExperience(1);

        assertThat(fam.getWeight(), equalTo(23));

        var req = UseItemRequest.getInstance(ItemPool.PILE_OF_USELESS_ROBOT_PARTS);
        req.responseText = html("request/test_use_pile_of_useless_robot_parts.html");
        req.processResults();

        assertThat("homemadeRobotUpgrades", isSetTo(3));
        assertThat(fam.getWeight(), equalTo(34));
      }
    }

    @Test
    void detectMaxedOutHomemadeRobot() {
      var cleanups =
          new Cleanups(
              withProperty("homemadeRobotUpgrades", 2), withFamiliar(FamiliarPool.HOMEMADE_ROBOT));

      try (cleanups) {
        var fam = KoLCharacter.getFamiliar();
        fam.setExperience(1);

        assertThat(fam.getWeight(), equalTo(23));

        var req = UseItemRequest.getInstance(ItemPool.PILE_OF_USELESS_ROBOT_PARTS);
        req.responseText = html("request/test_use_pile_of_useless_robot_parts_finished.html");
        req.processResults();

        assertThat("homemadeRobotUpgrades", isSetTo(9));
        assertThat(fam.getWeight(), equalTo(100));
      }
    }
  }

  @Nested
  class MaximumUses {
    @ParameterizedTest
    @CsvSource({"0, 5", "15, 0", "8, 2"})
    void maxUsesWorksForHtmlFood(int fullness, int maxUses) {
      var cleanups = withFullness(fullness);

      try (cleanups) {
        assertThat(UseItemRequest.maximumUses(ItemPool.BASH_OS_CEREAL), is(maxUses));
      }
    }

    @ParameterizedTest
    @CsvSource({"0, 5", "15, 0", "8, 2"})
    void maxUsesWorksForHtmlFoodWithConsumptionType(int fullness, int maxUses) {
      var cleanups = withFullness(fullness);

      try (cleanups) {
        assertThat(
            UseItemRequest.maximumUses(ItemPool.BASH_OS_CEREAL, ConsumptionType.EAT), is(maxUses));
      }
    }

    @ParameterizedTest
    @CsvSource({"0, 5", "15, 0", "8, 3"})
    void maxUsesWorksForHtmlBooze(int drunk, int maxUses) {
      var cleanups = withInebriety(drunk);

      try (cleanups) {
        assertThat(UseItemRequest.maximumUses(ItemPool.OREILLE_DIVISEE_BRANDY), is(maxUses));
      }
    }

    @ParameterizedTest
    @CsvSource({"0, 5", "15, 0", "8, 3"})
    void maxUsesWorksForHtmlBoozeWithConsumptionType(int drunk, int maxUses) {
      var cleanups = withInebriety(drunk);

      try (cleanups) {
        assertThat(
            UseItemRequest.maximumUses(ItemPool.OREILLE_DIVISEE_BRANDY, ConsumptionType.DRINK),
            is(maxUses));
      }
    }

    @ParameterizedTest
    @CsvSource({"0, 7", "15, 0", "8, 3"})
    void maxUsesWorksForHtmlSpleenItems(int spleenUsed, int maxUses) {
      var cleanups = withSpleenUse(spleenUsed);

      try (cleanups) {
        assertThat(UseItemRequest.maximumUses(ItemPool.EXTROVERMECTIN), is(maxUses));
      }
    }

    @ParameterizedTest
    @CsvSource({"0, 7", "15, 0", "8, 3"})
    void maxUsesWorksForHtmlSpleenItemsWithConsumptionType(int spleenUsed, int maxUses) {
      var cleanups = withSpleenUse(spleenUsed);

      try (cleanups) {
        assertThat(
            UseItemRequest.maximumUses(ItemPool.EXTROVERMECTIN, ConsumptionType.SPLEEN),
            is(maxUses));
      }
    }
  }

  @Test
  void setsBigBookPreference() {
    var cleanups =
        new Cleanups(
            withItem(ItemPool.THE_BIG_BOOK_OF_EVERY_SKILL),
            withProperty("_bookOfEverySkillUsed", false));

    try (cleanups) {
      var req = UseItemRequest.getInstance(ItemPool.THE_BIG_BOOK_OF_EVERY_SKILL);
      req.responseText = html("request/test_use_big_book_of_every_skill.html");
      req.processResults();

      assertThat("_bookOfEverySkillUsed", isSetTo(true));
      assertTrue(KoLCharacter.hasSkill(SkillPool.ANTIPHON));
    }
  }

  @Test
  void detectsBastilleLoanerVoucherUse() {
    var cleanups =
        new Cleanups(
            withItem(ItemPool.BASTILLE_LOANER_VOUCHER, 2),
            withNextResponse(
                new FakeHttpResponse<>(
                    200, html("request/test_use_item_bastille_loaner_voucher_ajax.html")),
                new FakeHttpResponse<>(
                    200, html("request/test_use_item_bastille_loaner_voucher_choice.html"))),
            withHandlingChoice(false));

    try (cleanups) {
      assertThat(InventoryManager.getCount(ItemPool.BASTILLE_LOANER_VOUCHER), is(2));

      var req = UseItemRequest.getInstance(ItemPool.BASTILLE_LOANER_VOUCHER);
      req.run();

      assertThat(InventoryManager.getCount(ItemPool.BASTILLE_LOANER_VOUCHER), is(1));
    }
  }
}
