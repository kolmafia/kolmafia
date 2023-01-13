package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withGender;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withPasswordHash;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLCharacter.Gender;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class JuneCleaverManagerTest {

  @BeforeAll
  public static void beforeAll() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("June Cleaver");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("June Cleaver");
    JuneCleaverManager.reset();
  }

  @Nested
  class Automation {

    private static final KoLAdventure NOOB_CAVE = AdventureDatabase.getAdventureByName("Noob Cave");

    @Test
    public void canAutomateJuneCleaverAdventure() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("choiceAdventure1474", 1),
              withProperty("juneCleaverQueue"),
              withProperty("_juneCleaverEncounters", 0),
              withProperty("_juneCleaverFightsLeft", 0),
              // Needed when automating AdventureRequest -> CHOICE_HANDLER
              withPasswordHash("june"),
              // No need to look at vinyl boots
              withGender(Gender.FEMALE));
      try (cleanups) {
        builder.client.addResponse(
            302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        // choice.php?forceoption=0
        builder.client.addResponse(200, html("request/test_june_cleaver_choice.html"));
        // api.php?what=status&for=KoLmafia
        builder.client.addResponse(200, ""); // api.php
        // choice.php?pwd&whichchoice=1474&option=1
        builder.client.addResponse(200, html("request/test_june_cleaver_taken.html"));
        builder.client.addResponse(200, ""); // api.php

        var adventure = NOOB_CAVE.getRequest();
        adventure.run();
        assertThat("juneCleaverQueue", isSetTo("1474"));
        assertThat("_juneCleaverEncounters", isSetTo(1));
        assertThat("_juneCleaverFightsLeft", isSetTo(6));

        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(5));
        assertPostRequest(
            requests.get(0),
            "/adventure.php",
            "snarfblat=" + NOOB_CAVE.getAdventureId() + "&pwd=june");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(3), "/choice.php", "whichchoice=1474&option=1&pwd=june");
        assertPostRequest(requests.get(4), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }
}
