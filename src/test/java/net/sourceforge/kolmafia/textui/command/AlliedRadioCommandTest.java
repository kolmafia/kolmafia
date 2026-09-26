package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClient;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class AlliedRadioCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("AlliedRadioCommandTest");
    Preferences.reset("AlliedRadioCommandTest");
    ChoiceManager.handlingChoice = false;
    FightRequest.currentRound = 0;
  }

  public AlliedRadioCommandTest() {
    this.command = "alliedradio";
  }

  private Cleanups withRadio() {
    return withItem(ItemPool.HANDHELD_ALLIED_RADIO);
  }

  @Test
  void usage() {
    String output = execute("");
    assertThat(output, containsString("Usage: alliedradio"));
  }

  @Test
  void usesRemaining() {
    var cleanups =
        new Cleanups(
            withItem(ItemPool.HANDHELD_ALLIED_RADIO, 6),
            withItem(ItemPool.ALLIED_RADIO_BACKPACK),
            withProperty("_alliedRadioDropsUsed", 1));

    try (cleanups) {
      assertThat(AlliedRadioCommand.usesRemaining(), equalTo(8));
    }
  }

  @Nested
  class Item {
    @Test
    void requiresRadio() {
      String output = execute("item");
      assertThat(output, containsString("You need a handheld radio, or a charged backpack."));
    }

    @Test
    void requiresParameter() {
      var cleanups = withRadio();

      try (cleanups) {
        String output = execute("item");
        assertThat(output, containsString("Which item do you want?"));
      }
    }

    @Test
    void requiresValidParameter() {
      var cleanups = withRadio();

      try (cleanups) {
        String output = execute("item mystery");
        assertThat(output, containsString("I don't understand what item mystery is."));
      }
    }
  }

  @Nested
  class Effect {
    @Test
    void requiresRadio() {
      String output = execute("effect");
      assertThat(output, containsString("You need a handheld radio, or a charged backpack."));
    }

    @Test
    void requiresParameter() {
      var cleanups = withRadio();

      try (cleanups) {
        String output = execute("effect");
        assertThat(output, containsString("Which effect do you want?"));
      }
    }

    @Test
    void requiresValidParameter() {
      var cleanups = withRadio();

      try (cleanups) {
        String output = execute("effect mystery");
        assertThat(output, containsString("I don't understand what effect mystery is."));
      }
    }
  }

  @Nested
  class Misc {
    @Test
    void requiresRadio() {
      String output = execute("misc");
      assertThat(output, containsString("You need a handheld radio, or a charged backpack."));
    }

    @Test
    void requiresParameter() {
      var cleanups = withRadio();

      try (cleanups) {
        String output = execute("misc");
        assertThat(output, containsString("Which miscellaneous supplies do you want?"));
      }
    }

    @Test
    void requiresValidParameter() {
      var cleanups = withRadio();

      try (cleanups) {
        String output = execute("misc mystery");
        assertThat(output, containsString("I don't understand what supplies mystery is."));
      }
    }
  }

  @Nested
  class Request {
    @Test
    void requiresRadio() {
      String output = execute("request");
      assertThat(output, containsString("You need a handheld radio, or a charged backpack."));
    }
  }

  @Nested
  class Success {
    @ParameterizedTest
    @CsvSource({
      "effect ellipsoid,ellipsoidtine",
      "effect Ellipsoidtined,ellipsoidtine",
      "effect intel,materiel intel",
      "effect material intel,materiel intel",
      "effect item,materiel intel",
      "effect boon,wildsun boon",
      "effect Wildsun Boon,wildsun boon",
    })
    void effect(String params, String request) {
      var setup = setupClient();
      var client = setup.client;
      var cleanups = setup.cleanups;

      try (cleanups) {
        addResponses(client);

        execute(params);

        assertRequest(client, request);
      }
    }

    @ParameterizedTest
    @CsvSource({
      "item food,rations",
      "item rations,rations",
      "item Skeleton Wars rations,rations",
      "item fuel,fuel",
      "item booze,fuel",
      "item skeleton war fuel can,fuel",
      "item ordnance,ordnance",
      "item skeleton war grenade,ordnance",
      "item radio,radio",
      "item handheld Allied radio,radio",
      "item salary,salary",
      "item Chroner,salary",
    })
    void item(String params, String request) {
      var setup = setupClient();
      var client = setup.client;
      var cleanups = setup.cleanups;

      try (cleanups) {
        addResponses(client);

        execute(params);

        assertRequest(client, request);
      }
    }

    @ParameterizedTest
    @CsvSource({
      "misc sniper,sniper support",
      "misc support,sniper support",
    })
    void misc(String params, String request) {
      var setup = setupClient();
      var client = setup.client;
      var cleanups = setup.cleanups;

      try (cleanups) {
        addResponses(client);

        execute(params);

        assertRequest(client, request);
      }
    }

    @ParameterizedTest
    @CsvSource({
      "request,''",
      "request ,''",
      "request radio,radio",
      "request anything,anything",
    })
    void request(String params, String request) {
      var setup = setupClient();
      var client = setup.client;
      var cleanups = setup.cleanups;

      try (cleanups) {
        addResponses(client);

        execute(params);

        assertRequest(client, request);
      }
    }

    private record SetupClient(FakeHttpClient client, Cleanups cleanups) {}

    private SetupClient setupClient() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups = new Cleanups(withRadio(), withHttpClientBuilder(builder));
      return new SetupClient(client, cleanups);
    }

    private void addResponses(FakeHttpClient client) {
      client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
      client.addResponse(200, html("request/test_allied_radio_grey_text.html"));
      client.addResponse(200, ""); // api.php
      client.addResponse(200, html("request/test_allied_radio_success.html"));
    }

    private void assertRequest(FakeHttpClient client, String request) {
      var requests = client.getRequests();

      assertPostRequest(
          requests.get(3), "/choice.php", "option=1&request=" + request + "&whichchoice=1563");
      assertContinueState();
    }
  }
}
