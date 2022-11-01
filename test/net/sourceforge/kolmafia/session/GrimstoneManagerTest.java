package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withGender;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withPasswordHash;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withTurnsPlayed;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class GrimstoneManagerTest {

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("grimstone");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("grimstone");
  }

  @Nested
  class Wolf {
    @AfterAll
    public static void afterAll() {
      // Because we actually load a charpane.
      KoLCharacter.reset("");
    }

    @Test
    public void startingHouseRunTakesThreeTurns() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPasswordHash("grimstone"),
              withGender(KoLCharacter.FEMALE),
              withProperty("wolfTurnsUsed", 27),
              withTurnsPlayed(30257));
      try (cleanups) {
        client.addResponse(
            302, Map.of("location", List.of("fight.php?ireallymeanit=1667327836")), "");
        client.addResponse(200, html("request/test_unleash_inner_wolf.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_unleash_inner_wolf_charpane.html"));

        // I did this in the Relay Browser. We will simulate it being done in
        // via automation - which follows redirects and requests api.php.

        String URL = "place.php?whichplace=ioty2014_wolf&action=wolf_houserun";
        var request = new GenericRequest(URL);
        request.run();

        // That requested an api.php request. I collected HTML for a chrpane refresh.
        request.constructURLString("charpane.php", false);
        request.run();

        // A House Run takes 3 turns. This is charged when you initiate it
        // and it redirects to a fight, which is followed by a
        // fight-follows-fight chain.
        assertEquals(30, Preferences.getInteger("wolfTurnsUsed"));
        assertEquals(30260, KoLCharacter.getTurnsPlayed());

        var requests = client.getRequests();

        assertPostRequest(
            requests.get(0),
            "/place.php",
            "whichplace=ioty2014_wolf&action=wolf_houserun&pwd=grimstone");
        assertGetRequest(requests.get(1), "/fight.php", "ireallymeanit=1667327836");
        assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
        assertGetRequest(requests.get(3), "/charpane.php", null);
      }
    }
  }
}
