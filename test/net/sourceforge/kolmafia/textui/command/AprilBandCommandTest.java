package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withTurnsPlayed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class AprilBandCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("AprilBandCommandTest");
    Preferences.reset("AprilBandCommandTest");
    ChoiceManager.handlingChoice = false;
    FightRequest.currentRound = 0;
  }

  public AprilBandCommandTest() {
    this.command = "aprilband";
  }

  private Cleanups withHelmet() {
    return withItem(ItemPool.APRILING_BAND_HELMET);
  }

  @Test
  void usage() {
    String output = execute("");
    assertThat(output, containsString("Usage: aprilband"));
  }

  @Nested
  class Effect {
    @Test
    void requiresHelmet() {
      String output = execute("effect");
      assertThat(output, containsString("You need an Apriling band helmet"));
    }

    @Test
    void requiresConductAvailable() {
      var cleanups =
          new Cleanups(withHelmet(), withTurnsPlayed(0), withProperty("nextAprilBandTurn", 10));

      try (cleanups) {
        String output = execute("effect");
        assertThat(output, containsString("You cannot change your conduct (10 turns to go)"));
      }
    }

    @Test
    void requiresEffect() {
      var cleanups = withHelmet();

      try (cleanups) {
        String output = execute("effect");
        assertThat(output, containsString("Which effect do you want?"));
      }
    }

    @Test
    void requiresValidEffect() {
      var cleanups = withHelmet();

      try (cleanups) {
        String output = execute("effect luck");
        assertThat(output, containsString("I don't understand what effect luck is"));
      }
    }
  }

  @Nested
  class Item {
    @Test
    void requiresHelmet() {
      String output = execute("item");
      assertThat(output, containsString("You need an Apriling band helmet"));
    }

    @Test
    void requiresInstrumentsAvailable() {
      var cleanups = new Cleanups(withHelmet(), withProperty("_aprilBandInstruments", 2));

      try (cleanups) {
        String output = execute("item");
        assertThat(output, containsString("You cannot get any more instruments."));
      }
    }

    @Test
    void requiresInstrument() {
      var cleanups = withHelmet();

      try (cleanups) {
        String output = execute("item");
        assertThat(output, containsString("Which instrument do you want?"));
      }
    }

    @Test
    void requiresValidInstrument() {
      var cleanups = withHelmet();

      try (cleanups) {
        String output = execute("item clarinet");
        assertThat(output, containsString("I don't understand what instrument clarinet is."));
      }
    }
  }

  @Nested
  class Play {
    @Test
    void requiresParameter() {
      String output = execute("play");
      assertThat(output, containsString("Which instrument do you want to play?"));
    }

    @Test
    void requiresInstrument() {
      String output = execute("play sax");
      assertThat(output, containsString("You don't have an Apriling band saxophone."));
    }
  }

  @Nested
  class Success {
    @BeforeEach
    public void beforeEach() {
      HttpClientWrapper.setupFakeClient();
    }

    @ParameterizedTest
    @CsvSource({
      "effect noncombat, 1",
      "effect combat, 2",
      "effect drop, 3",
      "item sax, 4",
      "item quad tom, 5",
      "item tuba, 6",
      "item staff, 7",
      "item piccolo, 8"
    })
    void conduct(String params, int choice) {
      var cleanups = withHelmet();

      try (cleanups) {
        execute(params);
        var requests = getRequests();

        assertThat(requests, hasSize(3));

        assertPostRequest(requests.get(0), "/inventory.php", "action=apriling");
        assertPostRequest(requests.get(1), "/choice.php", containsString("&option=" + choice));
        assertPostRequest(requests.get(2), "/choice.php", containsString("&option=9"));
        assertContinueState();
      }
    }

    @ParameterizedTest
    @CsvSource({
      "play sax, 11566",
      "play quad tom, 11567",
      "play tuba, 11568",
      "play staff, 11569",
      "play piccolo, 11570"
    })
    void play(String params, int id) {
      var cleanups = withItem(id);

      try (cleanups) {
        execute(params);
        var requests = getRequests();

        assertThat(requests, hasSize(1));

        assertGetRequest(
            requests.get(0),
            equalTo("/inventory.php"),
            containsString("action=aprilplay&iid=" + id));
        assertContinueState();
      }
    }
  }
}
