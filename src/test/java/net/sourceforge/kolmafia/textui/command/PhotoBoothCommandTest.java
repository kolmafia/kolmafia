package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withClanLoungeItem;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
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

public class PhotoBoothCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("PhotoBoothCommandTest");
    Preferences.reset("PhotoBoothCommandTest");
    ChoiceManager.handlingChoice = false;
    FightRequest.currentRound = 0;
  }

  public PhotoBoothCommandTest() {
    this.command = "photobooth";
  }

  private Cleanups withBooth() {
    return withClanLoungeItem(ItemPool.CLAN_PHOTO_BOOTH);
  }

  @Test
  void usage() {
    String output = execute("");
    assertThat(output, containsString("Usage: photobooth"));
  }

  @Nested
  class Effect {
    @Test
    void requiresBooth() {
      String output = execute("effect");
      assertThat(output, containsString("Your clan needs a photo booth."));
    }

    @Test
    void requiresEffectsAvailable() {
      var cleanups = new Cleanups(withBooth(), withProperty("_photoBoothEffects", 3));

      try (cleanups) {
        String output = execute("effect");
        assertThat(output, containsString("You cannot get any more effects."));
      }
    }

    @Test
    void requiresEffect() {
      var cleanups = withBooth();

      try (cleanups) {
        String output = execute("effect");
        assertThat(output, containsString("Which effect do you want?"));
      }
    }

    @Test
    void requiresValidEffect() {
      var cleanups = withBooth();

      try (cleanups) {
        String output = execute("effect luck");
        assertThat(output, containsString("I don't understand what effect luck is"));
      }
    }
  }

  @Nested
  class Item {
    @Test
    void requiresBooth() {
      String output = execute("item");
      assertThat(output, containsString("Your clan needs a photo booth."));
    }

    @Test
    void requiresPropsAvailable() {
      var cleanups = new Cleanups(withBooth(), withProperty("_photoBoothEquipment", 3));

      try (cleanups) {
        String output = execute("item");
        assertThat(output, containsString("You cannot get any more props."));
      }
    }

    @Test
    void requiresItem() {
      var cleanups = withBooth();

      try (cleanups) {
        String output = execute("item");
        assertThat(output, containsString("Which item do you want?"));
      }
    }

    @Test
    void requiresValidItem() {
      var cleanups = withBooth();

      try (cleanups) {
        String output = execute("item fancy hat");
        assertThat(output, containsString("I don't understand what item fancy hat is."));
      }
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
      "effect wild, 1",
      "effect tower, 2",
      "effect space, 3",
    })
    void effect(String params, int choice) {
      var cleanups = withBooth();

      try (cleanups) {
        execute(params);
        var requests = getRequests();

        assertThat(requests, hasSize(4));

        assertPostRequest(requests.get(0), "/clan_viplounge.php", "action=photobooth");
        assertPostRequest(requests.get(1), "/choice.php", containsString("&option=1"));
        assertPostRequest(requests.get(2), "/choice.php", containsString("&option=" + choice));
        assertPostRequest(requests.get(3), "/choice.php", containsString("&option=6"));
        assertContinueState();
      }
    }

    @ParameterizedTest
    @CsvSource({
      "item photo booth supply list, 1",
      "item fake arrow-through-the-head, 2",
      "item fake huge beard, 3",
      "item astronaut helmet, 4",
      "item cheap plastic pipe, 5",
      "item oversized monocle on a stick, 6",
      "item giant bow tie, 7",
      "item feather boa, 8",
      "item Sheriff badge, 9",
      "item Sheriff pistol, 10",
      "item Sheriff moustache, 11",
    })
    void item(String params, int choice) {
      var cleanups = withBooth();

      try (cleanups) {
        execute(params);
        var requests = getRequests();

        assertThat(requests, hasSize(4));

        assertPostRequest(requests.get(0), "/clan_viplounge.php", "action=photobooth");
        assertPostRequest(requests.get(1), "/choice.php", containsString("&option=2"));
        assertPostRequest(requests.get(2), "/choice.php", containsString("&option=" + choice));
        assertPostRequest(requests.get(3), "/choice.php", containsString("&option=6"));
        assertContinueState();
      }
    }
  }
}
