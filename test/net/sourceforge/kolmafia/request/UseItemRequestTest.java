package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.*;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junitpioneer.jupiter.cartesian.CartesianTest.Enum;
import static org.junitpioneer.jupiter.cartesian.CartesianTest.Values;

import internal.helpers.Cleanups;
import internal.helpers.RequestLoggerOutput;
import internal.network.FakeHttpClientBuilder;
import internal.network.FakeHttpResponse;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLCharacter.Gender;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
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

  @Nested
  class zeroFullness {
    @ParameterizedTest
    @CsvSource({"5140, 1", "10883, 3"})
    void itShouldNotDivideByZeroWhenConsumingAstralEnergyDrink(int itemID, int maxUses) {
      int spleenUsed = 0;
      var cleanups =
          new Cleanups(
              withSpleenUse(spleenUsed),
              withHP(0, 10, 10),
              withMP(0, 10, 10),
              withPath(AscensionPath.Path.STANDARD));

      try (cleanups) {
        assertThat(UseItemRequest.maximumUses(itemID, ConsumptionType.SPLEEN), is(maxUses));
      }
    }

    @Test
    void itShouldHandleAstralEnergyDrinkAppropriately() {
      var cleanups = new Cleanups(withLevel(1), withSpleenUse(0));
      try (cleanups) {
        assertThat(ConsumablesDatabase.getRawSpleenHit("[5140]astral energy drink"), is(8));
        assertThat(ConsumablesDatabase.getRawSpleenHit("[10883]astral energy drink"), is(5));
        assertThat(ConsumablesDatabase.getRawSpleenHit("astral energy drink"), is(0));
        assertThat(UseItemRequest.maximumUses("[5140]astral energy drink"), is(1));
        assertThat(UseItemRequest.maximumUses(5140), is(1));
        assertThat(UseItemRequest.maximumUses("[10883]astral energy drink"), is(3));
        assertThat(UseItemRequest.maximumUses(10883), is(3));
        assertThat(UseItemRequest.maximumUses("astral energy drink"), is(Integer.MAX_VALUE));
      }
    }

    /**
     * The list ot tested items was derived from items with zero fullness in fullness.txt at the
     * time the test was written. Magical sausage 10060 has zero fullness but it is not tested here
     * because the fullness is controlled by a preference. Similarly for the glitch season reward
     * 10207. Given that the point of this test is to show a divide by zero does not occur, leaving
     * them out is deemed acceptable.
     */
    @ParameterizedTest
    @CsvSource({"572", "1266", "1432", "2149", "2188", "4412"})
    void itShouldNotDivideByZeroWhenConsumingZeroFullnessItems(int itemID) {
      int fullness = 0;
      int maxUses = Integer.MAX_VALUE;
      var cleanups = withFullness(fullness);

      try (cleanups) {
        assertThat(UseItemRequest.maximumUses(itemID, ConsumptionType.EAT), is(maxUses));
      }
    }

    @ParameterizedTest
    @CsvSource({"1047", "1276", "4413", "7491"})
    void itShouldNotDivideByZeroWhenDrinkingZeroInebrietyItems(int itemID) {
      int inebriety = 0;
      int maxUses = Integer.MAX_VALUE;
      var cleanups = withInebriety(inebriety);

      try (cleanups) {
        assertThat(UseItemRequest.maximumUses(itemID, ConsumptionType.DRINK), is(maxUses));
      }
    }
  }

  @Nested
  class MojoFilter {
    @Test
    void detectSuccessfulUse() {
      var cleanups = new Cleanups(withSpleenUse(10), withProperty("currentMojoFilters", 1));

      try (cleanups) {
        var req = UseItemRequest.getInstance(ItemPool.MOJO_FILTER);
        req.responseText = html("request/test_use_mojo_filter_success.html");
        req.processResults();

        assertThat("currentMojoFilters", isSetTo(2));
        assertThat(KoLCharacter.getSpleenUse(), is(9));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "0, 1, 3", "0, 2, 2", "0, 3, 1", "1, 1, 3", "1, 2, 2", "1, 3, 1", "2, 1, 3", "2, 2, 2",
      "2, 3, 2", "3, 1, 3", "3, 2, 3", "3, 3, 3"
    })
    void detectMaxMojoFilters(final int current, final int count, final int expected) {
      var cleanups = new Cleanups(withSpleenUse(10), withProperty("currentMojoFilters", current));

      try (cleanups) {
        var req = UseItemRequest.getInstance(ItemPool.MOJO_FILTER, count);
        req.responseText = html("request/test_use_mojo_filter_already_maxed.html");
        req.processResults();

        assertThat("currentMojoFilters", isSetTo(expected));
        assertThat(KoLCharacter.getSpleenUse(), is(10));
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

  @ParameterizedTest
  @ValueSource(strings = {"success", "failure"})
  void setsPunchingMirrorPreference(String htmlSource) {
    var path = "request/test_use_punching_mirror_" + htmlSource + ".html";
    var cleanups =
        new Cleanups(
            withItem(ItemPool.PUNCHING_MIRROR),
            withProperty("_punchingMirrorUsed", false),
            withHippyStoneBroken(),
            withNextResponse(200, html(path)));

    try (cleanups) {
      var req = UseItemRequest.getInstance(ItemPool.PUNCHING_MIRROR);
      req.run();

      assertThat("_punchingMirrorUsed", isSetTo(true));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"success", "failure"})
  void setsSnowballFactoryPreference(String htmlSource) {
    var path = "request/test_use_snowball_factory_" + htmlSource + ".html";
    var cleanups =
        new Cleanups(
            withItem(ItemPool.LIL_SNOWBALL_FACTORY),
            withProperty("_snowballFactoryUsed", false),
            withNextResponse(200, html(path)));

    try (cleanups) {
      var req = UseItemRequest.getInstance(ItemPool.LIL_SNOWBALL_FACTORY);
      req.run();

      assertThat("_snowballFactoryUsed", isSetTo(true));
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

  // @Test
  void detectsMolehillMountainUse() {
    var cleanups =
        new Cleanups(
            withFight(0), // Clean up the resulting fight
            withItem(ItemPool.MOLEHILL_MOUNTAIN, 1),
            withProperty("_molehillMountainUsed", false),
            withNextResponse(
                new FakeHttpResponse<>(
                    200, html("request/test_use_item_molehill_mountain_redirect.html")),
                new FakeHttpResponse<>(
                    200, html("request/test_use_item_molehill_mountain_fight.html"))));

    try (cleanups) {
      var req = UseItemRequest.getInstance(ItemPool.MOLEHILL_MOUNTAIN);
      req.run();

      assertThat(InventoryManager.getCount(ItemPool.MOLEHILL_MOUNTAIN), is(1));
      assertThat("_molehillMountainUsed", isSetTo(true));
    }
  }

  @Test
  void detectsStrangeStalagmiteUse() {
    var cleanups =
        new Cleanups(
            withItem(ItemPool.STRANGE_STALAGMITE, 1),
            withProperty("_strangeStalagmiteUsed", false),
            withNextResponse(
                new FakeHttpResponse<>(
                    200, html("request/test_use_item_strange_stalagmite_redirect.html")),
                new FakeHttpResponse<>(
                    200, html("request/test_use_item_strange_stalagmite_choice.html"))),
            withHandlingChoice(false));

    try (cleanups) {
      var req = UseItemRequest.getInstance(ItemPool.STRANGE_STALAGMITE);
      req.run();

      assertThat(InventoryManager.getCount(ItemPool.STRANGE_STALAGMITE), is(1));
      assertThat("_strangeStalagmiteUsed", isSetTo(true));
    }
  }

  @Test
  void detectsCanAlreadyAccessFantasyRealm() {
    var cleanups =
        new Cleanups(
            withItem(ItemPool.FR_GUEST),
            withProperty("_frToday", false),
            withNextResponse(200, html("request/test_use_item_fantasyrealm_guest_pass.html")));

    try (cleanups) {
      assertThat(InventoryManager.getCount(ItemPool.FR_GUEST), is(1));

      var req = UseItemRequest.getInstance(ItemPool.FR_GUEST);
      req.run();

      assertThat(InventoryManager.getCount(ItemPool.FR_GUEST), is(1));
      assertThat("_frToday", isSetTo(false));
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "request/test_use_item_absentee_voter_ballot_already_have_access.html",
        "request/test_use_item_absentee_voter_ballot_already_voted.html"
      })
  void detectsCanAlreadyAccessVoterRegistrationOrHasVotedAlready(String htmlSource) {
    var cleanups =
        new Cleanups(
            withItem(ItemPool.VOTER_BALLOT),
            withProperty("_voteToday", false),
            withNextResponse(200, html(htmlSource)));

    try (cleanups) {
      assertThat(InventoryManager.getCount(ItemPool.VOTER_BALLOT), is(1));

      var req = UseItemRequest.getInstance(ItemPool.VOTER_BALLOT);
      req.run();

      assertThat(InventoryManager.getCount(ItemPool.VOTER_BALLOT), is(1));
      assertThat("_voteToday", isSetTo(false));
    }
  }

  @Test
  void detectsAlreadyHasAccessToNeverendingParty() {
    var cleanups =
        new Cleanups(
            withItem(ItemPool.NEVERENDING_PARTY_INVITE_DAILY),
            withProperty("_neverendingPartyToday", false),
            withNextResponse(200, html("request/test_use_item_neverending_party_guest_pass.html")));

    try (cleanups) {
      assertThat(InventoryManager.getCount(ItemPool.NEVERENDING_PARTY_INVITE_DAILY), is(1));

      var req = UseItemRequest.getInstance(ItemPool.NEVERENDING_PARTY_INVITE_DAILY);
      req.run();

      assertThat(InventoryManager.getCount(ItemPool.NEVERENDING_PARTY_INVITE_DAILY), is(1));
      assertThat("_neverendingPartyToday", isSetTo(false));
    }
  }

  @Test
  void detectsAlreadyCanAccessBoxingDaycare() {
    var cleanups =
        new Cleanups(
            withItem(ItemPool.BOXING_DAY_PASS),
            withProperty("_daycareToday", false),
            withNextResponse(200, html("request/test_use_item_boxing_day_pass.html")));

    try (cleanups) {
      assertThat(InventoryManager.getCount(ItemPool.BOXING_DAY_PASS), is(1));

      var req = UseItemRequest.getInstance(ItemPool.BOXING_DAY_PASS);
      req.run();

      assertThat(InventoryManager.getCount(ItemPool.BOXING_DAY_PASS), is(1));
      assertThat("_daycareToday", isSetTo(false));
    }
  }

  @Test
  void detectsCanAlreadyAccessPirateRealm() {
    var cleanups =
        new Cleanups(
            withItem(ItemPool.PR_GUEST),
            withProperty("_prToday", false),
            withNextResponse(200, html("request/test_use_item_piraterealm_guest_pass.html")));

    try (cleanups) {
      assertThat(InventoryManager.getCount(ItemPool.PR_GUEST), is(1));

      var req = UseItemRequest.getInstance(ItemPool.PR_GUEST);
      req.run();

      assertThat(InventoryManager.getCount(ItemPool.PR_GUEST), is(1));
      assertThat("_prToday", isSetTo(false));
    }
  }

  @Nested
  class Milestone {
    static final AdventureResult MILESTONE = ItemPool.get(ItemPool.MILESTONE);

    @Test
    void milestoneBeforeDesertNotConsumed() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(MILESTONE),
              withProperty("desertExploration", 0));
      try (cleanups) {
        client.addResponse(200, html("request/test_milestone_pre_desert.html"));
        client.addResponse(200, ""); // api.php

        var request = new GenericRequest("inv_use.php?which=3&whichitem=11104&ajax=1");
        request.run();
        assertTrue(InventoryManager.hasItem(MILESTONE));
        assertThat("desertExploration", isSetTo(0));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));

        assertPostRequest(requests.get(0), "/inv_use.php", "which=3&whichitem=11104&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void milestoneWillExploreDesert() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(MILESTONE),
              withProperty("desertExploration", 92));
      try (cleanups) {
        client.addResponse(200, html("request/test_milestone_explore_desert.html"));
        client.addResponse(200, ""); // api.php

        var request = new GenericRequest("inv_use.php?which=3&whichitem=11104&ajax=1");
        request.run();
        assertFalse(InventoryManager.hasItem(MILESTONE));
        assertThat("desertExploration", isSetTo(97));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));

        assertPostRequest(requests.get(0), "/inv_use.php", "which=3&whichitem=11104&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void milestoneWillFinishDesert() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(MILESTONE),
              withProperty("desertExploration", 97));
      try (cleanups) {
        client.addResponse(200, html("request/test_milestone_finish_desert.html"));
        client.addResponse(200, ""); // api.php

        var request = new GenericRequest("inv_use.php?which=3&whichitem=11104&ajax=1");
        request.run();
        assertFalse(InventoryManager.hasItem(MILESTONE));
        assertThat("desertExploration", isSetTo(100));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));

        assertPostRequest(requests.get(0), "/inv_use.php", "which=3&whichitem=11104&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void milestoneAfterDesertGivesStats() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(MILESTONE),
              withSubStats(0, 0, 0),
              withProperty("desertExploration", 100));
      try (cleanups) {
        client.addResponse(200, html("request/test_milestone_post_desert.html"));
        client.addResponse(200, ""); // api.php

        assertEquals(0, KoLCharacter.getTotalMuscle());
        assertEquals(0, KoLCharacter.getTotalMysticality());
        assertEquals(0, KoLCharacter.getTotalMoxie());

        var request = new GenericRequest("inv_use.php?which=3&whichitem=11104&ajax=1");
        request.run();
        assertFalse(InventoryManager.hasItem(MILESTONE));
        assertThat("desertExploration", isSetTo(100));
        assertEquals(165, KoLCharacter.getTotalMuscle());
        assertEquals(244, KoLCharacter.getTotalMysticality());
        assertEquals(214, KoLCharacter.getTotalMoxie());

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));

        assertPostRequest(requests.get(0), "/inv_use.php", "which=3&whichitem=11104&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }

  @Nested
  class Stalagmite {
    private UseItemRequest getStalagmiteRequest() {
      return UseItemRequest.getInstance(ItemPool.STRANGE_STALAGMITE);
    }

    @Test
    void successfulStalagmiteUsageSetsPreferences() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.STRANGE_STALAGMITE),
              withProperty("_strangeStalagmiteUsed", false),
              withProperty("choiceAdventure1491", 2),
              // Need a password hash to automate choice adventures
              withPasswordHash("stalagmite"),
              // If you have a password hash, KoL looks at your vinyl boots
              withGender(Gender.FEMALE));

      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, html("request/test_stalagmite_first_use.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_stalagmite_select_solution.html"));
        client.addResponse(200, ""); // api.php

        var req = getStalagmiteRequest();
        req.run();

        assertThat("_strangeStalagmiteUsed", isSetTo(true));

        var requests = client.getRequests();
        assertThat(requests, hasSize(5));

        assertPostRequest(requests.get(0), "/inv_use.php", "whichitem=11109&ajax=1&pwd=stalagmite");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(
            requests.get(3), "/choice.php", "whichchoice=1491&option=2&pwd=stalagmite");
        assertPostRequest(requests.get(4), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void secondCertificateFails() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.STRANGE_STALAGMITE),
              withProperty("_strangeStalagmiteUsed", false));

      try (cleanups) {
        client.addResponse(200, html("request/test_stalagmite_second_use.html"));
        client.addResponse(200, ""); // api.php

        var req = getStalagmiteRequest();
        req.run();

        assertThat("_strangeStalagmiteUsed", isSetTo(true));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));

        assertPostRequest(requests.get(0), "/inv_use.php", "whichitem=11109&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }

  @Nested
  class ChocolateCoveredPingPongBall {
    @Test
    void incrementsCounterOnSuccess() {
      var cleanups =
          new Cleanups(
              withProperty("_chocolateCoveredPingPongBallsUsed", 1),
              withItem(ItemPool.CHOCOLATE_COVERED_PING_PONG_BALL),
              withItem(ItemPool.PING_PONG_BALL, 0),
              withNextResponse(
                  200,
                  html("request/test_use_item_chocolate_covered_ping_pong_ball_success.html")));

      try (cleanups) {
        var req = UseItemRequest.getInstance(ItemPool.CHOCOLATE_COVERED_PING_PONG_BALL);
        req.run();

        assertThat("_chocolateCoveredPingPongBallsUsed", isSetTo(2));
      }
    }

    @Test
    void maxesCounterOnFailure() {
      var cleanups =
          new Cleanups(
              withProperty("_chocolateCoveredPingPongBallsUsed", 1),
              withItem(ItemPool.CHOCOLATE_COVERED_PING_PONG_BALL),
              withItem(ItemPool.PING_PONG_BALL, 0),
              withNextResponse(
                  200,
                  html("request/test_use_item_chocolate_covered_ping_pong_ball_failure.html")));

      try (cleanups) {
        var req = UseItemRequest.getInstance(ItemPool.CHOCOLATE_COVERED_PING_PONG_BALL);
        req.run();

        assertThat("_chocolateCoveredPingPongBallsUsed", isSetTo(3));
      }
    }
  }

  @Nested
  class SIT {
    private UseItemRequest getCertificateRequest() {
      return UseItemRequest.getInstance(ItemPool.SIT_COURSE_COMPLETION_CERTIFICATE);
    }

    @Test
    void successfulCertificateUsageSetsPreferences() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.SIT_COURSE_COMPLETION_CERTIFICATE),
              withProperty("_sitCourseCompleted", false),
              withProperty("currentSITSkill", "Cryptobotanist"),
              withSkill(SkillPool.CRYPTOBOTANIST),
              withProperty("choiceAdventure1494", 2),
              withoutSkill(SkillPool.INSECTOLOGIST),
              // Need a password hash to automate choice adventures
              withPasswordHash("SIT"),
              // If you have a password hash, KoL looks at your vinyl boots
              withGender(Gender.FEMALE));

      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, html("request/test_sit_course_first_use.html"));
        client.addResponse(200, html("request/test_sit_course_select_course.html"));
        client.addResponse(200, ""); // api.php

        var req = getCertificateRequest();
        req.run();

        assertThat("_sitCourseCompleted", isSetTo(true));
        assertThat("currentSITSkill", isSetTo("Insectologist"));
        assertFalse(KoLCharacter.hasSkill(SkillPool.CRYPTOBOTANIST));
        assertTrue(KoLCharacter.hasSkill(SkillPool.INSECTOLOGIST));

        var requests = client.getRequests();
        assertThat(requests, hasSize(4));

        assertPostRequest(requests.get(0), "/inv_use.php", "whichitem=11116&ajax=1&pwd=SIT");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(2), "/choice.php", "whichchoice=1494&option=2&pwd=SIT");
        assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void secondCertificateFails() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.SIT_COURSE_COMPLETION_CERTIFICATE),
              withProperty("_sitCourseCompleted", false));

      try (cleanups) {
        client.addResponse(200, html("request/test_sit_course_second_use.html"));
        client.addResponse(200, ""); // api.php

        var req = getCertificateRequest();
        req.run();

        assertThat("_sitCourseCompleted", isSetTo(true));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));

        assertPostRequest(requests.get(0), "/inv_use.php", "whichitem=11116&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }

  @Nested
  class ReplicaTenDollars {
    @Test
    void successfulSingleUseIncrementsSetting() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.REPLICA_MR_ACCESSORY, 0),
              withItem(ItemPool.REPLICA_TEN_DOLLARS, 2),
              withProperty("legacyPoints", 0));

      try (cleanups) {
        client.addResponse(200, html("request/test_use_one_replica_ten_dollars.html"));
        client.addResponse(200, ""); // api.php

        var req = UseItemRequest.getInstance(ItemPool.REPLICA_TEN_DOLLARS, 1);
        req.run();

        assertThat(InventoryManager.getCount(ItemPool.REPLICA_TEN_DOLLARS), is(1));
        assertThat(InventoryManager.getCount(ItemPool.REPLICA_MR_ACCESSORY), is(1));
        assertThat("legacyPoints", isSetTo(1));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));

        assertPostRequest(requests.get(0), "/inv_use.php", "whichitem=11253&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void successfulMultiseIncrementsSetting() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.REPLICA_MR_ACCESSORY, 0),
              withItem(ItemPool.REPLICA_TEN_DOLLARS, 2),
              withProperty("legacyPoints", 0));

      try (cleanups) {
        client.addResponse(200, html("request/test_use_two_replica_ten_dollars.html"));
        client.addResponse(200, ""); // api.php

        var req = UseItemRequest.getInstance(ItemPool.REPLICA_TEN_DOLLARS, 2);
        req.run();

        assertThat(InventoryManager.getCount(ItemPool.REPLICA_TEN_DOLLARS), is(0));
        assertThat(InventoryManager.getCount(ItemPool.REPLICA_MR_ACCESSORY), is(2));
        assertThat("legacyPoints", isSetTo(2));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));

        assertPostRequest(
            requests.get(0), "/multiuse.php", "whichitem=11253&action=useitem&quantity=2&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }

  @ParameterizedTest
  @CsvSource({
    "your stomach drops and your ears pop as you are suddenly plunged into a horrifyingly dark and blurry version of the world you once knew, true",
    "You're gonna need a full night's sleep before you ring that thing again., false"
  })
  void clarasBellSetsNCForcerFlag(String responseText, String result) {
    var cleanups = withProperty("noncombatForcerActive", false);
    try (cleanups) {
      var req = UseItemRequest.getInstance(ItemPool.CLARA_BELL);
      req.responseText = responseText;
      req.processResponse();
      assertThat("noncombatForcerActive", isSetTo(Boolean.parseBoolean(result)));
    }
  }

  @Nested
  class LoathingIdolMicrophone {
    @ParameterizedTest
    @CsvSource({"100, 1", "75, 2", "50, 3", "25, 4"})
    void usingMicrophoneUsesCharges(int charge, int option) {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      int itemId = 0;
      int nextId = 0;
      switch (charge) {
        case 100:
          itemId = ItemPool.LOATHING_IDOL_MICROPHONE;
          nextId = ItemPool.LOATHING_IDOL_MICROPHONE_75;
          break;
        case 75:
          itemId = ItemPool.LOATHING_IDOL_MICROPHONE_75;
          nextId = ItemPool.LOATHING_IDOL_MICROPHONE_50;
          break;
        case 50:
          itemId = ItemPool.LOATHING_IDOL_MICROPHONE_50;
          nextId = ItemPool.LOATHING_IDOL_MICROPHONE_25;
          break;
        case 25:
          itemId = ItemPool.LOATHING_IDOL_MICROPHONE_25;
          nextId = 0;
          break;
      }

      String effectName = "none";
      switch (option) {
        case 1:
          effectName = "Poppy Performance";
          break;
        case 2:
          effectName = "Romantically Roused ";
          break;
        case 3:
          effectName = "Spitting Rhymes";
          break;
        case 4:
          effectName = "Twangy";
          break;
      }
      AdventureResult effect = EffectPool.get(EffectDatabase.getEffectId(effectName));

      String path = "request/test_microphone_" + charge + ".html";

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withNoItems(),
              withItem(itemId, 1),
              withNoEffects(),
              withProperty("choiceAdventure1505", option),
              // Need a password hash to automate choice adventures
              withPasswordHash("microphone"),
              // If you have a password hash, KoL looks at your vinyl boots
              withGender(Gender.FEMALE));

      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, html("request/test_microphone_choice.html"));
        client.addResponse(200, html(path));
        client.addResponse(200, ""); // api.php

        var req = UseItemRequest.getInstance(itemId, 1);
        req.run();

        assertThat(InventoryManager.getCount(itemId), is(0));
        if (nextId != 0) {
          assertThat(InventoryManager.getCount(nextId), is(1));
        }
        assertThat(effect.getCount(KoLConstants.activeEffects), is(30));

        var requests = client.getRequests();
        assertThat(requests, hasSize(4));

        assertPostRequest(
            requests.get(0), "/inv_use.php", "whichitem=" + itemId + "&ajax=1&pwd=microphone");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
        assertPostRequest(
            requests.get(2),
            "/choice.php",
            "whichchoice=1505&option=" + option + "&pwd=microphone");
        assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }

  @Nested
  class GiftPackages {
    @Test
    void canDetectSenderOfGiftPackage() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups = new Cleanups(withHttpClientBuilder(builder), withNoItems());
      try (cleanups) {
        RequestLoggerOutput.startStream();
        client.addResponse(200, html("request/test_gift_package.html"));
        var request = new GenericRequest("inv_use.php?whichitem=1168&ajax=1");
        request.run();
        var output = RequestLoggerOutput.stopStream();

        String expected =
            """
            <font color="green">Opening less-than-three-shaped box from Veracity</font>
            You acquire 11-leaf clover (11)
            """;
        assertThat(output, startsWith(expected));
        assertThat(InventoryManager.getCount(ItemPool.ELEVEN_LEAF_CLOVER), is(11));
      }
    }
  }

  @Nested
  class ConsumptionTypes {
    @Test
    void potionsAreMultiusable() {
      assertEquals(
          UseItemRequest.getConsumptionType(ItemPool.get(ItemPool.VIAL_OF_PURPLE_SLIME)),
          ConsumptionType.USE_MULTIPLE);
    }

    @Test
    void reusablePotionsAreReusable() {
      assertEquals(
          UseItemRequest.getConsumptionType(ItemPool.get(ItemPool.BRASS_DREAD_FLASK)),
          ConsumptionType.USE_INFINITE);
    }

    @Test
    void singleUsePotionsAreUsable() {
      assertEquals(
          UseItemRequest.getConsumptionType(ItemPool.get(ItemPool.GOOFBALLS)), ConsumptionType.USE);
    }

    @Test
    void familiarHatchlingsAreTheirOwnType() {
      assertEquals(
          UseItemRequest.getConsumptionType(ItemPool.get(ItemPool.MOSQUITO_LARVA)),
          ConsumptionType.FAMILIAR_HATCHLING);
    }
  }

  @Nested
  class NEPBagsKeys {
    @ParameterizedTest
    @CsvSource({
      ItemPool.VAN_KEY + ", '', ''",
      ItemPool.VAN_KEY + ", 'food', ''",
      ItemPool.VAN_KEY + ", 'booze', ''",
      ItemPool.VAN_KEY + ", 'booze', '10 2063'",
      ItemPool.UNREMARKABLE_DUFFEL_BAG + ", '', ''",
      ItemPool.UNREMARKABLE_DUFFEL_BAG + ", 'booze', ''",
      ItemPool.UNREMARKABLE_DUFFEL_BAG + ", 'food', ''",
      ItemPool.UNREMARKABLE_DUFFEL_BAG + ", 'food', '10 2063'"
    })
    void propertyIsntIncrementedWithoutQuestActive(int itemId, String quest, String questProgress) {
      var html =
          itemId == ItemPool.VAN_KEY
              ? html("request/test_item_use_van_key.html")
              : html("request/test_item_use_unremarkable_duffel_bag.html");
      var cleanups =
          new Cleanups(
              withItem(itemId),
              withProperty("_questPartyFairItemsOpened", 0),
              withProperty("_questPartyFairQuest", quest),
              withProperty("_questPartyFairProgress", questProgress),
              withNextResponse(new FakeHttpResponse<>(200, html)));

      try (cleanups) {
        UseItemRequest.getInstance(itemId).run();
        assertThat("_questPartyFairItemsOpened", isSetTo(0));
      }
    }

    @Test
    void foodPropertyIsIncrementedWithQuestActive() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.VAN_KEY),
              withProperty("_questPartyFairItemsOpened", 0),
              withProperty("_questPartyFairQuest", "food"),
              withProperty("_questPartyFairProgress", "10 2063"),
              withNextResponse(
                  new FakeHttpResponse<>(200, html("request/test_item_use_van_key.html"))));

      try (cleanups) {
        // Verify that the correct item increments the quest
        UseItemRequest.getInstance(ItemPool.VAN_KEY).run();
        assertThat("_questPartyFairItemsOpened", isSetTo(1));
      }
    }

    @Test
    void boozePropertyIsIncrementedWithQuestActive() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.UNREMARKABLE_DUFFEL_BAG),
              withProperty("_questPartyFairItemsOpened", 0),
              withProperty("_questPartyFairQuest", "booze"),
              withProperty("_questPartyFairProgress", "10 2063"),
              withNextResponse(
                  new FakeHttpResponse<>(
                      200, html("request/test_item_use_unremarkable_duffel_bag.html"))));

      try (cleanups) {
        // Verify that the correct item increments the quest
        UseItemRequest.getInstance(ItemPool.UNREMARKABLE_DUFFEL_BAG).run();
        assertThat("_questPartyFairItemsOpened", isSetTo(1));
      }
    }
  }

  @Nested
  class Evilometer {
    @Test
    void detectsPartiallyEvilCyrpt() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.EVILOMETER),
              withProperty("cyrptTotalEvilness", 200),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptNicheEvilness", 50),
              withProperty("cyrptNookEvilness", 50),
              withNextResponse(
                  new FakeHttpResponse<>(200, html("request/test_evilometer_partial.html"))));

      try (cleanups) {
        UseItemRequest.getInstance(ItemPool.EVILOMETER).run();
        assertThat("cyrptTotalEvilness", isSetTo(89));
        assertThat("cyrptAlcoveEvilness", isSetTo(50));
        assertThat("cyrptCrannyEvilness", isSetTo(39));
        assertThat("cyrptNicheEvilness", isSetTo(0));
        assertThat("cyrptNookEvilness", isSetTo(0));
      }
    }

    @Test
    void detectsFullyEvilCyrpt() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.EVILOMETER),
              withProperty("cyrptTotalEvilness", 200),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptNicheEvilness", 50),
              withProperty("cyrptNookEvilness", 50),
              withNextResponse(
                  new FakeHttpResponse<>(200, html("request/test_evilometer_999.html"))));

      try (cleanups) {
        UseItemRequest.getInstance(ItemPool.EVILOMETER).run();
        assertThat("cyrptTotalEvilness", isSetTo(999));
        assertThat("cyrptAlcoveEvilness", isSetTo(0));
        assertThat("cyrptCrannyEvilness", isSetTo(0));
        assertThat("cyrptNicheEvilness", isSetTo(0));
        assertThat("cyrptNookEvilness", isSetTo(0));
      }
    }

    @Test
    void detectsUndefiledCyrpt() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.EVILOMETER),
              withProperty("cyrptTotalEvilness", 200),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptNicheEvilness", 50),
              withProperty("cyrptNookEvilness", 50),
              withNextResponse(
                  new FakeHttpResponse<>(200, html("request/test_evilometer_finished.html"))));

      try (cleanups) {
        UseItemRequest.getInstance(ItemPool.EVILOMETER).run();
        assertThat("cyrptTotalEvilness", isSetTo(0));
        assertThat("cyrptAlcoveEvilness", isSetTo(0));
        assertThat("cyrptCrannyEvilness", isSetTo(0));
        assertThat("cyrptNicheEvilness", isSetTo(0));
        assertThat("cyrptNookEvilness", isSetTo(0));
        assertFalse(InventoryManager.hasItem(ItemPool.EVILOMETER));
      }
    }
  }

  @Nested
  class LawOfAverages {
    @Test
    void increments() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.LAW_OF_AVERAGES),
              withProperty("_lawOfAveragesUsed", 0),
              withNextResponse(new FakeHttpResponse<>(200, "")));

      try (cleanups) {
        // Verify that the correct item increments the quest
        UseItemRequest.getInstance(ItemPool.LAW_OF_AVERAGES).run();
        assertThat("_lawOfAveragesUsed", isSetTo(1));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "0, 3",
      // Do not reduce the number, one might be in our closet for some reason
      "4, 4"
    })
    void setsToMaxIfRejected(final int startingValue, final int expectedValue) {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.LAW_OF_AVERAGES, 3),
              withProperty("_lawOfAveragesUsed", startingValue),
              withNextResponse(new FakeHttpResponse<>(200, "You already feel pretty average")));

      try (cleanups) {
        // Verify that the correct item increments the quest
        UseItemRequest.getInstance(ItemPool.LAW_OF_AVERAGES).run();
        assertThat("_lawOfAveragesUsed", isSetTo(expectedValue));
      }
    }
  }
}
