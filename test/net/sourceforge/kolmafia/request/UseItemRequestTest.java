package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFight;
import static internal.helpers.Player.withFullness;
import static internal.helpers.Player.withGender;
import static internal.helpers.Player.withHandlingChoice;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withInebriety;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withLimitMode;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withPasswordHash;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSkill;
import static internal.helpers.Player.withSpleenUse;
import static internal.helpers.Player.withSubStats;
import static internal.helpers.Player.withoutSkill;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junitpioneer.jupiter.cartesian.CartesianTest.Enum;
import static org.junitpioneer.jupiter.cartesian.CartesianTest.Values;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import internal.network.FakeHttpResponse;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLCharacter.Gender;
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
}
