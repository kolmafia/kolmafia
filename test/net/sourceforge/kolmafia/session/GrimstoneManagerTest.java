package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withFight;
import static internal.helpers.Player.withGender;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withLastLocation;
import static internal.helpers.Player.withPasswordHash;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
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
    @Test
    public void pigsEvictedDetectedAfterFight() {
      var cleanups =
          new Cleanups(
              withLastLocation("Unleash Your Inner Wolf"),
              withFight(4),
              withProperty("wolfPigsEvicted", 0));
      try (cleanups) {
        String URL = "fight.php?blah.x=8&blah.y=18&whichskill=7192&action=skill";
        String html = html("request/test_evict_pigs.html");
        FightRequest.registerRequest(true, URL);
        FightRequest.updateCombatData(URL, "brick icehouse", html);
        assertEquals(3, Preferences.getInteger("wolfPigsEvicted"));
      }
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
              withFight(0),
              withProperty("wolfTurnsUsed", 27));
      try (cleanups) {
        client.addResponse(
            302, Map.of("location", List.of("fight.php?ireallymeanit=1667327836")), "");
        client.addResponse(200, html("request/test_unleash_inner_wolf.html"));
        client.addResponse(200, ""); // api.php

        // I did this in the Relay Browser. We will simulate it being done in
        // via automation - which follows redirects and requests api.php.

        String URL = "place.php?whichplace=ioty2014_wolf&action=wolf_houserun";
        var request = new GenericRequest(URL);
        request.run();

        // A House Run takes 3 turns. This is charged when you initiate it
        // and it redirects to a fight, which is followed by a
        // fight-follows-fight chain.
        assertEquals(30, Preferences.getInteger("wolfTurnsUsed"));

        var requests = client.getRequests();
        assertThat(requests, hasSize(3));

        assertPostRequest(
            requests.get(0),
            "/place.php",
            "whichplace=ioty2014_wolf&action=wolf_houserun&pwd=grimstone");
        assertGetRequest(requests.get(1), "/fight.php", "ireallymeanit=1667327836");
        assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }

  @Nested
  class Witch {
    @Test
    public void candyGainedDetectedAfterFight() {
      var cleanups =
          new Cleanups(
              withLastLocation("Gumdrop Forest"),
              withFight(3),
              withProperty("candyWitchCandyTotal", 0));
      try (cleanups) {
        String URL = "fight.php?action=skill&whichskill=1003";
        String html = html("request/test_gain_candy.html");
        FightRequest.registerRequest(true, URL);
        FightRequest.updateCombatData(URL, "licorice snake", html);
        assertEquals(126, Preferences.getInteger("candyWitchCandyTotal"));
      }
    }

    @Test
    public void candyLostToThievesDetected() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      int initial = 3000;

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPasswordHash("grimstone"),
              withGender(KoLCharacter.FEMALE),
              withProperty("candyWitchCandyTotal", initial));
      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, html("request/test_wait_for_thieves.html"));
        client.addResponse(200, html("request/test_lose_candy.html"));
        client.addResponse(200, ""); // api.php

        String URL = "place.php?whichplace=ioty2014_candy&action=witch_house";
        var request = new GenericRequest(URL, false);
        request.run();

        URL = "choice.php?pwd&whichchoice=831&option=1";
        request = new GenericRequest(URL);
        request.run();

        // <b>-465 Candy</b>
        // <b>-962 Candy</b>
        // <b>-648 Candy</b>
        int lost = 465 + 962 + 648;

        assertEquals(initial - lost, Preferences.getInteger("candyWitchCandyTotal"));

        var requests = client.getRequests();
        assertThat(requests, hasSize(4));

        assertGetRequest(
            requests.get(0), "/place.php", "whichplace=ioty2014_candy&action=witch_house");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(2), "/choice.php", "whichchoice=831&option=1&pwd=grimstone");
        assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }

  @Nested
  class Hare {
    @Test
    public void secondsSavedDetectedAfterFight() {
      var cleanups =
          new Cleanups(
              withLastLocation("A Deserted Stretch of I-911"),
              withFight(3),
              withProperty("hareMillisecondsSaved", 0));
      try (cleanups) {
        String URL = "fight.php?action=attack";
        String html = html("request/test_save_seconds.html");
        FightRequest.registerRequest(true, URL);
        FightRequest.updateCombatData(URL, "sketchy van", html);
        assertEquals(12, Preferences.getInteger("hareMillisecondsSaved"));
      }
    }
  }
}
