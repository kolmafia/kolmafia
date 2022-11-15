package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withGender;
import static internal.helpers.Player.withHandlingChoice;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withLimitMode;
import static internal.helpers.Player.withNoEffects;
import static internal.helpers.Player.withPasswordHash;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.LimitMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

public class GongCommandTest extends AbstractCommandTestBase {

  public GongCommandTest() {
    this.command = "gong";
  }

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("gong");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("gong");
  }

  @Nested
  class Bird {
    @Test
    public void birdModeNotOnJourney() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.GONG),
              withHandlingChoice(false),
              withContinuationState(),
              withLimitMode(LimitMode.NONE),
              withPasswordHash("gong"),
              // If you have a password hash, KoL looks at your vinyl boots
              withGender(KoLCharacter.FEMALE));
      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, html("request/test_use_llama_lama_gong.html"));
        client.addResponse(200, html("request/test_choose_bird_form.html"));
        client.addResponse(200, ""); // api.php

        String output = execute("bird");
        assertThat(output, containsString("Gong path: bird"));
        assertThat(output, containsString("Using 1 llama lama gong"));
        assertThat(output, containsString("Encounter: The Gong Has Been Bung"));
        assertThat(output, containsString("You acquire an effect: Form of...Bird! (15)"));

        assertEquals(LimitMode.BIRD, KoLCharacter.getLimitMode());
        assertFalse(ChoiceManager.handlingChoice);

        var requests = client.getRequests();
        assertThat(requests, hasSize(4));

        assertPostRequest(
            requests.get(0), "/inv_use.php", "whichitem=" + ItemPool.GONG + "&ajax=1&pwd=gong");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(2), "/choice.php", "whichchoice=276&option=3&pwd=gong");
        assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"BIRD", "ROACH", "MOLE", "ASTRAL"})
    public void birdModeOnJourney(LimitMode lm) {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.GONG),
              withHandlingChoice(false),
              withContinuationState(),
              withLimitMode(lm),
              withNoEffects(),
              withEffect(lm.effectName(), 1));
      try (cleanups) {
        String output = execute("bird");
        assertThat(output, containsString("You can't use a gong right now."));
        assertErrorState();

        var requests = client.getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, AdventurePool.HAUNTED_KITCHEN})
    public void birdModeAtEndOfJourney(int snarfblat) {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.GONG),
              withHandlingChoice(false),
              withContinuationState(),
              withLimitMode(LimitMode.BIRD),
              withNoEffects(),
              withProperty("welcomeBackAdv", snarfblat),
              withPasswordHash("gong"),
              // If you have a password hash, KoL looks at your vinyl boots
              withGender(KoLCharacter.FEMALE));
      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php")), "");
        client.addResponse(200, html("request/test_leave_reincarnation.html"));
        client.addResponse(200, html("request/test_get_bird_reward.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, html("request/test_use_llama_lama_gong.html"));
        client.addResponse(200, html("request/test_choose_bird_form.html"));
        client.addResponse(200, ""); // api.php

        if (snarfblat == 0) snarfblat = AdventurePool.NOOB_CAVE;
        String locationName = AdventureDatabase.getAdventure(snarfblat).getAdventureName();

        String output = execute("bird");
        assertThat(output, containsString("Gong path: bird"));
        assertThat(output, containsString(locationName));
        assertThat(output, containsString("Welcome Back!"));
        assertThat(output, containsString("You acquire an item: glimmering roc feather"));
        assertThat(output, containsString("Using 1 llama lama gong"));
        assertThat(output, containsString("Encounter: The Gong Has Been Bung"));
        assertThat(output, containsString("You acquire an effect: Form of...Bird! (15)"));

        assertEquals(LimitMode.BIRD, KoLCharacter.getLimitMode());
        assertFalse(ChoiceManager.handlingChoice);

        var requests = client.getRequests();
        assertThat(requests, hasSize(8));

        assertPostRequest(
            requests.get(0), "/adventure.php", "snarfblat=" + snarfblat + "&pwd=gong");
        assertGetRequest(requests.get(1), "/choice.php", null);
        assertPostRequest(requests.get(2), "/choice.php", "whichchoice=277&option=1&pwd=gong");
        assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(
            requests.get(4), "/inv_use.php", "whichitem=" + ItemPool.GONG + "&ajax=1&pwd=gong");
        assertGetRequest(requests.get(5), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(6), "/choice.php", "whichchoice=276&option=3&pwd=gong");
        assertPostRequest(requests.get(7), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }
}
