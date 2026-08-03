package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withAdjustmentsRecalculated;
import static internal.helpers.Player.withAdventuresLeft;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withHP;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withLimitMode;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.LimitMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class CampgroundCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("CampgroundCommandTest");
    Preferences.reset("CampgroundCommandTest");
    ChoiceManager.handlingChoice = false;
    FightRequest.currentRound = 0;
  }

  public CampgroundCommandTest() {
    this.command = "campground";
  }

  private Cleanups withCampaway() {
    return new Cleanups(
        withProperty("getawayCampsiteUnlocked", true), withProperty("restUsingCampaway", true));
  }

  private Cleanups withChateau() {
    return new Cleanups(
        withProperty("chateauAvailable", true), withProperty("restUsingChateau", true));
  }

  @Nested
  class Resting {
    @Test
    void respectsFreeRest() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHP(1, 100, 100),
              withHttpClientBuilder(builder),
              withItem(ItemPool.MOTHERS_NECKLACE),
              withProperty("timesRested", 0),
              withAdjustmentsRecalculated());

      try (cleanups) {
        execute("rest free 8");
        assertContinueState();
        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(5));
      }
    }

    @Test
    void restsAtCampaway() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHP(1, 100, 100),
              withHttpClientBuilder(builder),
              withAdventuresLeft(1),
              withCampaway());

      try (cleanups) {
        execute("rest campaway 3");
        assertContinueState();
        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(3));
        assertPostRequest(
            requests.getFirst(),
            "/place.php",
            both(containsString("whichplace=campaway"))
                .and(containsString("action=campaway_tentclick")));
      }
    }

    @Test
    void restsAtChateau() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHP(1, 100, 100),
              withHttpClientBuilder(builder),
              withAdventuresLeft(1),
              withChateau());

      try (cleanups) {
        execute("rest chateau 2");
        assertContinueState();
        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.getFirst(),
            "/place.php",
            both(containsString("whichplace=chateau"))
                .and(containsString("action=chateau_restbox")));
      }
    }

    @Test
    void failsToRestIfSpecifiedLocationUnavailable() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(withHP(1, 100, 100), withHttpClientBuilder(builder), withAdventuresLeft(1));

      try (cleanups) {
        var output = execute("rest campaway");
        assertErrorState();
        assertThat(
            output,
            containsString("You cannot rest at your Campsite Away From Your Campsite right now."));
        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    void restsAtCampgroundDespiteBetterOptions() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHP(1, 100, 100),
              withHttpClientBuilder(builder),
              withAdventuresLeft(1),
              withCampaway(),
              withChateau());

      try (cleanups) {
        execute("rest campground");
        assertContinueState();
        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(requests.getFirst(), "/campground.php", containsString("action=rest"));
      }
    }

    @Test
    void failsWithNoOptionsDueToPath() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHP(1, 100, 100),
              withHttpClientBuilder(builder),
              withPath(AscensionPath.Path.ACTUALLY_ED_THE_UNDYING),
              withClass(AscensionClass.ED),
              withAdventuresLeft(1));

      try (cleanups) {
        var output = execute("rest");
        assertErrorState();
        assertThat(output, containsString("You have no available resting spots right now."));
        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    void failsWithNoOptionsDueToLimitMode() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHP(1, 100, 100),
              withHttpClientBuilder(builder),
              withLimitMode(LimitMode.BATMAN),
              withAdventuresLeft(1));

      try (cleanups) {
        var output = execute("rest");
        assertErrorState();
        assertThat(output, containsString("You have no available resting spots right now."));
        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(0));
      }
    }
  }

  @Test
  void errorsWithNoCampgroundLimitMode() {
    var cleanups = new Cleanups(withLimitMode(LimitMode.SPELUNKY), withAdventuresLeft(1));

    try (cleanups) {
      var output = execute("workshed");
      assertErrorState();
      assertThat(output, containsString("You don't have a campground right now."));
    }
  }

  @Test
  void errorsWithNoCampgroundPath() {
    var cleanups =
        new Cleanups(
            withPath(AscensionPath.Path.ACTUALLY_ED_THE_UNDYING),
            withClass(AscensionClass.ED),
            withAdventuresLeft(1));

    try (cleanups) {
      var output = execute("workshed");
      assertErrorState();
      assertThat(output, containsString("You don't have a campground right now."));
    }
  }

  @ParameterizedTest
  @CsvSource({"vault5, vault5", "terminal, vault_term"})
  void directsToShelterInNuclearAutumn(final String params, final String action) {
    var builder = new FakeHttpClientBuilder();
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withPath(AscensionPath.Path.NUCLEAR_AUTUMN),
            withAdventuresLeft(1));

    try (cleanups) {
      execute(params);
      assertContinueState();
      var requests = builder.client.getRequests();
      assertThat(requests, hasSize(1));
      assertPostRequest(
          requests.getFirst(),
          "/place.php",
          both(containsString("whichplace=falloutshelter"))
              .and(containsString("action=" + action)));
    }
  }

  @Test
  void handlesNormalCampgroundRequest() {
    var builder = new FakeHttpClientBuilder();
    var cleanups = new Cleanups(withHttpClientBuilder(builder), withAdventuresLeft(1));

    try (cleanups) {
      execute("workshed 2");
      assertContinueState();
      var requests = builder.client.getRequests();
      assertThat(requests, hasSize(2));
      assertPostRequest(requests.getFirst(), "/campground.php", containsString("action=workshed"));
    }
  }
}
