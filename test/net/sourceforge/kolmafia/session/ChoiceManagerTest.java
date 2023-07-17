package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withAllEquipped;
import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withHP;
import static internal.helpers.Player.withHandlingChoice;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPasswordHash;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ChoiceManagerTest {

  @BeforeAll
  public static void beforeAll() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("choice manager user");
  }

  @BeforeEach
  public void beforeEach() {
    KoLConstants.encounterList.clear();
    ChoiceManager.lastChoice = 0;
    ChoiceManager.lastDecision = 0;
  }

  @Test
  public void thatUnknownTombDynamicChoicesWork() {
    // There is a riddle and three possible answers. These are based on your character class.
    // The order of the answers is shuffled every time the choice page is reloaded.
    // One answer is right and two are wrong.
    //
    // Choice Manager has special handling to automatically choose the correct answer.
    //
    // These response texts are for an Accordion Thief.
    // The wrong answers are "Stealth" and "an accordion".
    // The right answer is "Music.".

    KoLCharacter.setAscensionClass(AscensionClass.ACCORDION_THIEF);

    String responseText = html("request/test_choice_manager_unknown_tomb_1.html");
    int choice = ChoiceUtilities.extractChoice(responseText);
    assertEquals(1049, choice);
    Map<Integer, String> choices = ChoiceUtilities.parseChoices(responseText);
    assertEquals(3, choices.size());
    assertEquals("&quot;Music.&quot;", choices.get(1));
    // specialChoiceDecision1(int choice, String decision, int stepCount, String responseText)
    String option = ChoiceManager.specialChoiceDecision1(choice, "", 0, responseText);
    assertEquals("1", option);

    responseText = html("request/test_choice_manager_unknown_tomb_2.html");
    choice = ChoiceUtilities.extractChoice(responseText);
    assertEquals(1049, choice);
    choices = ChoiceUtilities.parseChoices(responseText);
    assertEquals(3, choices.size());
    assertEquals("&quot;Music.&quot;", choices.get(2));
    // specialChoiceDecision1(int choice, String decision, int stepCount, String responseText)
    option = ChoiceManager.specialChoiceDecision1(choice, "", 0, responseText);
    assertEquals("2", option);

    responseText = html("request/test_choice_manager_unknown_tomb_3.html");
    choice = ChoiceUtilities.extractChoice(responseText);
    assertEquals(1049, choice);
    choices = ChoiceUtilities.parseChoices(responseText);
    assertEquals(3, choices.size());
    assertEquals("&quot;Music.&quot;", choices.get(3));
    // specialChoiceDecision1(int choice, String decision, int stepCount, String responseText)
    option = ChoiceManager.specialChoiceDecision1(choice, "", 0, responseText);
    assertEquals("3", option);
  }

  @Nested
  class BogusChoices {
    @Test
    public void returnsFalseWithNormalChoice() {
      var cleanup = new Cleanups(withHandlingChoice());

      try (cleanup) {
        String urlString = "choice.php?whichchoice=1";
        var request = new GenericRequest(urlString);
        request.responseText = "Some normal choice text";

        assertThat(ChoiceManager.bogusChoice(urlString, request), is(false));
      }
    }

    // "Whoops!" testing (i.e. where it returns true) handled in
    // GenericRequestTest.detectsBogusChoices

    @Test
    public void returnsFalseWithNonChoiceRequest() {
      var cleanup = new Cleanups(withHandlingChoice());

      try (cleanup) {
        String urlString = "adventure.php?snarfblat=100";
        var request = new GenericRequest(urlString);
        request.responseText = "";

        assertThat(ChoiceManager.bogusChoice(urlString, request), is(false));
      }
    }

    @Test
    public void returnsFalseWithNonExecutedRequest() {
      var cleanup = new Cleanups(withHandlingChoice());

      try (cleanup) {
        String urlString =
            "choice.php?whichchoice=999&pwd&option=1&topper=3&lights=5&garland=1&gift=2";
        var request = new GenericRequest(urlString);

        assertThat(ChoiceManager.bogusChoice(urlString, request), is(false));
      }
    }

    @Test
    public void returnsTrueWithAbortState() {
      var cleanup = new Cleanups(withHandlingChoice(), withContinuationState());

      try (cleanup) {
        String urlString = "choice.php?whichchoice=1234&pwd&option=1";
        var request = new GenericRequest(urlString);
        request.responseText = "Whoops!  You're not actually in a choice adventure.";

        assertThat(ChoiceManager.bogusChoice(urlString, request), is(true));
        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.ABORT));
      }
    }

    @Test
    public void returnsTrueWithoutAbortStateIfPreferenceFalse() {
      var cleanup =
          new Cleanups(
              withHandlingChoice(),
              withContinuationState(),
              withProperty("abortOnChoiceWhenNotInChoice", false));

      try (cleanup) {
        String urlString = "choice.php?whichchoice=1234&pwd&option=1";
        var request = new GenericRequest(urlString);
        request.responseText = "Whoops!  You're not actually in a choice adventure.";

        assertThat(ChoiceManager.bogusChoice(urlString, request), is(true));
        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.ERROR));
      }
    }

    @Test
    public void returnsTrueWithoutAbortState() {
      var cleanup = new Cleanups(withHandlingChoice(), withContinuationState());

      try (cleanup) {
        String urlString = "choice.php";
        var request = new GenericRequest(urlString);
        request.responseText = "Whoops!  You're not actually in a choice adventure.";

        assertThat(ChoiceManager.bogusChoice(urlString, request), is(true));
        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.CONTINUE));
      }
    }
  }

  @Nested
  class ChoiceRedirection {
    @Test
    public void canRedirectToChoiceInGenericRequest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPasswordHash("test"),
              // Avoid health warning
              withHP(100, 100, 100));
      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, "test1");

        var url = "inv_use.php?which=3&whichitem=4509&pwd=test";
        var request =
            new GenericRequest(url) {
              @Override
              protected boolean shouldFollowRedirect() {
                return true;
              }
            };
        request.run();
        assertEquals("test1", request.responseText);

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/inv_use.php", "which=3&whichitem=4509&pwd=test");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
      }
    }

    @Test
    public void canRedirectToChoiceWithoutForceOption() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("lastEncounter", "No Sects in the Potion Room"),
              withHandlingChoice(false));
      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php")), "");
        client.addResponse(200, html("request/test_leave_reincarnation.html"));
        client.addResponse(200, ""); // api.php

        var request = new GenericRequest("adventure.php?snarfblat=" + AdventurePool.MT_MOLEHILL);
        request.run();

        assertThat(KoLConstants.encounterList.size(), is(1));
        assertThat(KoLConstants.encounterList.get(0).getCount(), is(1));
        assertThat(KoLConstants.encounterList.get(0).getName(), is("Welcome Back!"));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));

        assertPostRequest(
            requests.get(0), "/adventure.php", "snarfblat=" + AdventurePool.MT_MOLEHILL);
        assertGetRequest(requests.get(1), "/choice.php", null);
      }
    }

    @Test
    public void canProcessChoiceInGenericRequest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPasswordHash("test"),
              // Avoid health warning
              withHP(100, 100, 100));
      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, "test2");

        var url = "inv_use.php?which=3&whichitem=4509&pwd=test";
        var request =
            new GenericRequest(url) {
              @Override
              protected boolean shouldFollowRedirect() {
                return false;
              }
            };
        request.run();
        assertEquals("test2", request.responseText);

        // Although shouldFollowRedirect() was false, since it redirected to
        // choice.php, GenericRequest let ChoiceManager handle it

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/inv_use.php", "which=3&whichitem=4509&pwd=test");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
      }
    }

    @Test
    public void canNotRedirectToChoiceInRelayRequest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPasswordHash("test"),
              // Avoid health warning
              withHP(100, 100, 100));
      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, "test3");

        var url = "inv_use.php?which=3&whichitem=4509&pwd=test";
        var request = new RelayRequest(false);
        request.constructURLString(url);
        request.run();
        assertEquals("", request.responseText);

        var choice = new GenericRequest("choice.php?forceoption=0", false);
        choice.run();
        assertEquals("test3", choice.responseText);

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/inv_use.php", "which=3&whichitem=4509&pwd=test");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
      }
    }
  }

  @Nested
  class ChoiceRefresh {
    @Test
    public void canRefreshChoiceWithoutReVisiting() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPasswordHash("refresh"),
              withProperty("_gingerbreadCityTurns", 19),
              withProperty("choiceAdventure1202", 0),
              withHandlingChoice(false),
              // Avoid health warning
              withHP(100, 100, 100));
      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, html("request/test_visit_gc_midnight_civic_center.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(302, Map.of("location", List.of("choice.php")), "");
        client.addResponse(200, html("request/test_visit_gc_midnight_civic_center_refresh.html"));

        assertThat(KoLConstants.encounterList.size(), is(0));

        var url = "adventure.php?snarfblat=477";
        var request = new GenericRequest(url);
        request.run();

        // Since it redirected to choice.php, GenericRequest let ChoiceManager
        // handle it. This is a visit to choice #1202, which is not automated.

        // We "visit" the choice page.
        assertThat(Preferences.getInteger("_gingerbreadCityTurns"), is(20));
        assertThat(KoLConstants.encounterList.size(), is(1));
        assertThat(KoLConstants.encounterList.get(0).getCount(), is(1));

        // If you refresh the page, it redirects to choice.php
        url = "place.php?whichplace=gingerbreadcity";
        request = new GenericRequest(url);
        request.run();

        // We do not "revisit" the choice page.
        assertThat(Preferences.getInteger("_gingerbreadCityTurns"), is(20));
        assertThat(KoLConstants.encounterList.size(), is(1));
        assertThat(KoLConstants.encounterList.get(0).getCount(), is(1));

        var requests = client.getRequests();
        assertThat(requests, hasSize(5));
        assertPostRequest(requests.get(0), "/adventure.php", "snarfblat=477&pwd=refresh");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(3), "/place.php", "whichplace=gingerbreadcity&pwd=refresh");
        assertGetRequest(requests.get(4), "/choice.php", null);
      }
    }
  }

  @Nested
  class SlaggingOff {
    private static final AdventureResult SHARDS = ItemPool.get(ItemPool.CRIMBO_CRYSTAL_SHARDS, 1);
    private static final AdventureResult GOBLET = ItemPool.get(ItemPool.CRYSTAL_CRIMBO_GOBLET, 1);
    private static final AdventureResult PLATTER = ItemPool.get(ItemPool.CRYSTAL_CRIMBO_PLATTER, 1);

    private static final int SLAGGING_OFF = 1489;
    private static final String property = "choiceAdventure1489";

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    public void noCrystalShardsAlwaysTakesOption3(int decision) {
      var cleanups = new Cleanups(withProperty(property, decision));
      try (cleanups) {
        String NO_SHARDS = html("request/test_slagging_off_no_shards.html");
        int option = ChoiceManager.getDecision(SLAGGING_OFF, NO_SHARDS);
        assertEquals(3, option);
      }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    public void crystalShardsWithSpecificGoalSelectsCorrectChoice(int decision) {
      var cleanups = new Cleanups(withProperty(property, decision), withItem(SHARDS));
      try (cleanups) {
        String ONE_SHARD = html("request/test_slagging_off_one_shard.html");
        int option = ChoiceManager.getDecision(SLAGGING_OFF, ONE_SHARD);
        assertEquals(decision, option);
      }
    }

    @Test
    public void crystalShardsWillSelectGoblet() {
      var cleanups =
          new Cleanups(
              withProperty(property, 3),
              withItem(SHARDS),
              withItem(GOBLET.getInstance(1)),
              withItem(PLATTER.getInstance(2)));
      try (cleanups) {
        String ONE_SHARD = html("request/test_slagging_off_one_shard.html");
        int option = ChoiceManager.getDecision(SLAGGING_OFF, ONE_SHARD);
        assertEquals(1, option);
      }
    }

    @Test
    public void crystalShardsWillSelectPlatter() {
      var cleanups =
          new Cleanups(
              withProperty(property, 3),
              withItem(SHARDS),
              withItem(GOBLET.getInstance(2)),
              withItem(PLATTER.getInstance(1)));
      try (cleanups) {
        String ONE_SHARD = html("request/test_slagging_off_one_shard.html");
        int option = ChoiceManager.getDecision(SLAGGING_OFF, ONE_SHARD);
        assertEquals(2, option);
      }
    }

    @Test
    public void crystalShardsWillSelectEitherGobletOrPlatter() {
      var cleanups =
          new Cleanups(
              withProperty(property, 3),
              withItem(SHARDS),
              withItem(GOBLET.getInstance(1)),
              withItem(PLATTER.getInstance(1)));
      try (cleanups) {
        String ONE_SHARD = html("request/test_slagging_off_one_shard.html");
        int option = ChoiceManager.getDecision(SLAGGING_OFF, ONE_SHARD);
        // Always returns 1 so choice spoilers in the relay browser work.
        assertEquals(1, option);
      }
    }
  }

  @Nested
  class TavernChoices {
    private static final int STARING_DOWN_THE_BARREL = 513;
    private static final String property = "choiceAdventure513";

    @Test
    public void sufficientElementalDamageAllowsOption2() {
      var cleanups =
          new Cleanups(
              withProperty(property, 2),
              withAllEquipped(ItemPool.SEAL_CLUB, ItemPool.SEVENTEEN_BALL));
      try (cleanups) {
        String responseText = html("request/test_explore_cellar_choice.html");
        int option = ChoiceManager.getDecision(STARING_DOWN_THE_BARREL, responseText);
        assertEquals(2, option);
      }
    }

    @Test
    public void insufficientElementalDamageForcesOption1() {
      var cleanups = new Cleanups(withProperty(property, 2), withAllEquipped(ItemPool.SEAL_CLUB));
      try (cleanups) {
        String responseText = html("request/test_explore_cellar_choice.html");
        int option = ChoiceManager.getDecision(STARING_DOWN_THE_BARREL, responseText);
        assertEquals(1, option);
      }
    }
  }

  @Nested
  class StillInChoice {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void noLongerInChoiceAfterGodLobsterSelection(final boolean onDevServer) {
      ChoiceManager.lastResponseText =
          html(
              "request/test_choice_god_lobster_result_"
                  + (onDevServer ? "dev" : "regular")
                  + ".html");
      assertThat(ChoiceManager.stillInChoice(), is(false));
      ChoiceManager.lastResponseText = "";
    }

    @Test
    void parsesNowWhat() {
      ChoiceManager.lastResponseText = html("request/test_automation_temple_0.html");
      assertThat(ChoiceManager.stillInChoice(), is(true));
      ChoiceManager.lastResponseText = "";
    }

    @Test
    void parseNightstand() {
      ChoiceManager.lastResponseText = html("request/test_adventures_spent_fight_1_1.html");
      assertThat(ChoiceManager.stillInChoice(), is(true));
      ChoiceManager.lastResponseText = "";
    }

    @Test
    void parsesDoingTheMaths() {
      ChoiceManager.lastResponseText = html("request/test_choice_doing_the_maths.html");
      assertThat(ChoiceManager.stillInChoice(), is(true));
      ChoiceManager.lastResponseText = "";
    }
  }
}
