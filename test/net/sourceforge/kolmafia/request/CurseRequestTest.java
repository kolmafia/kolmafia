package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withNoEffects;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CurseRequestTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("CurseRequestTest");
  }

  @BeforeEach
  void beforeEach() {
    Preferences.reset("CurseRequestTest");
  }

  @Nested
  class CrimboTraining {
    @Test
    public void canTrainYourselfFirstTime() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.CRIMBO_TRAINING_MANUAL),
              withProperty("crimboTrainingSkill", 0),
              withProperty("_crimboTraining", false));
      try (cleanups) {
        client.addResponse(200, html("request/test_use_crimbo_training_1.html"));
        client.addResponse(200, ""); // api.php

        var request = new GenericRequest("inv_use.php?which=3&whichitem=11046&ajax=1");
        request.run();

        // Does not use up daily use
        assertThat("_crimboTraining", isSetTo(false));
        assertTrue(KoLCharacter.hasSkill(SkillPool.TRACK_SWITCHER));
        int index = (SkillPool.TRACK_SWITCHER - SkillPool.FIRST_CRIMBO_TRAINING_SKILL) + 1;
        assertThat("crimboTrainingSkill", isSetTo(index));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));

        assertPostRequest(requests.get(0), "/inv_use.php", "which=3&whichitem=11046&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void canTrainAnotherPersonOncePerDay() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.CRIMBO_TRAINING_MANUAL),
              withProperty("_crimboTraining", false));
      try (cleanups) {
        client.addResponse(200, html("request/test_use_crimbo_training_1b.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_use_crimbo_training_2.html"));
        client.addResponse(200, html("request/test_use_crimbo_training_3.html"));
        client.addResponse(200, ""); // api.php

        assertThat(UseItemRequest.maximumUses(ItemPool.CRIMBO_TRAINING_MANUAL), is(1));

        var request = new GenericRequest("inv_use.php?which=3&whichitem=11046&ajax=1");
        request.run();
        request = new GenericRequest("curse.php?whichitem=11046", false);
        request.run();
        request = new GenericRequest("curse.php?action=use&whichitem=11046&targetplayer=121572");
        request.run();

        // Detects daily use
        assertThat("_crimboTraining", isSetTo(true));
        assertThat(UseItemRequest.maximumUses(ItemPool.CRIMBO_TRAINING_MANUAL), is(0));

        var requests = client.getRequests();
        assertThat(requests, hasSize(5));

        assertPostRequest(requests.get(0), "/inv_use.php", "which=3&whichitem=11046&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        assertGetRequest(requests.get(2), "/curse.php", "whichitem=11046");
        assertPostRequest(
            requests.get(3), "/curse.php", "action=use&whichitem=11046&targetplayer=121572");
        assertPostRequest(requests.get(4), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void canDetectOtherPlayerAlreadyTrained() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.CRIMBO_TRAINING_MANUAL),
              withProperty("_crimboTraining", false));
      try (cleanups) {
        client.addResponse(200, html("request/test_use_crimbo_training_1.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_use_crimbo_training_2.html"));
        client.addResponse(200, html("request/test_use_crimbo_training_3c.html"));
        client.addResponse(200, ""); // api.php

        assertThat(UseItemRequest.maximumUses(ItemPool.CRIMBO_TRAINING_MANUAL), is(1));

        var request = new GenericRequest("inv_use.php?which=3&whichitem=11046&ajax=1");
        request.run();
        request = new GenericRequest("curse.php?whichitem=11046", false);
        request.run();
        request = new GenericRequest("curse.php?action=use&whichitem=11046&targetplayer=121572");
        request.run();

        // Not used if target already knows skill
        assertThat("_crimboTraining", isSetTo(false));
        assertThat(UseItemRequest.maximumUses(ItemPool.CRIMBO_TRAINING_MANUAL), is(1));

        var requests = client.getRequests();
        assertThat(requests, hasSize(4));

        assertPostRequest(requests.get(0), "/inv_use.php", "which=3&whichitem=11046&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        assertGetRequest(requests.get(2), "/curse.php", "whichitem=11046");
        assertPostRequest(
            requests.get(3), "/curse.php", "action=use&whichitem=11046&targetplayer=121572");
      }
    }

    @Test
    public void canTrainOnlyOneOtherPersonPerDay() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.CRIMBO_TRAINING_MANUAL),
              withProperty("_crimboTraining", false));
      try (cleanups) {
        client.addResponse(200, html("request/test_use_crimbo_training_1c.html"));
        client.addResponse(200, ""); // api.php

        assertThat(UseItemRequest.maximumUses(ItemPool.CRIMBO_TRAINING_MANUAL), is(1));

        var request = new GenericRequest("inv_use.php?which=3&whichitem=11046&ajax=1");
        request.run();

        // Detects daily use
        assertThat("_crimboTraining", isSetTo(true));
        assertThat(UseItemRequest.maximumUses(ItemPool.CRIMBO_TRAINING_MANUAL), is(0));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));

        assertPostRequest(requests.get(0), "/inv_use.php", "which=3&whichitem=11046&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void canTrainOnlyOneOtherPersonPerDayFromCursePHP() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.CRIMBO_TRAINING_MANUAL),
              withProperty("_crimboTraining", false));
      try (cleanups) {
        client.addResponse(200, html("request/test_use_crimbo_training_3b.html"));
        client.addResponse(200, ""); // api.php

        assertThat(UseItemRequest.maximumUses(ItemPool.CRIMBO_TRAINING_MANUAL), is(1));

        var request =
            new GenericRequest("curse.php?action=use&whichitem=11046&targetplayer=115875");
        request.run();

        // Detects daily use
        assertThat("_crimboTraining", isSetTo(true));
        assertThat(UseItemRequest.maximumUses(ItemPool.CRIMBO_TRAINING_MANUAL), is(0));

        var requests = client.getRequests();
        assertThat(requests, hasSize(1));

        assertPostRequest(
            requests.get(0), "/curse.php", "action=use&whichitem=11046&targetplayer=115875");
      }
    }
  }

  @Nested
  class PingPong {
    private static AdventureResult PING_PONG_PROWESS = EffectPool.get(EffectPool.PING_PONG_PROWESS);
    private static AdventureResult PING_PONG_PERSISTENCE =
        EffectPool.get(EffectPool.PING_PONG_PERSISTENCE);

    @Test
    public void cantPlayWithSomebodyInRonin() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.PING_PONG_TABLE),
              withProperty("_pingPongGame", false));
      try (cleanups) {
        client.addResponse(200, html("request/test_use_ping_pong_table.html"));
        client.addResponse(200, html("request/test_use_ping_pong_table_ronin.html"));
        client.addResponse(200, ""); // api.php

        assertThat(UseItemRequest.maximumUses(ItemPool.PING_PONG_TABLE), is(1));

        // KoL's use-link in inventory goes right to curse.php, rather than inv_use.php
        var request = new GenericRequest("curse.php?whichitem=11059", false);
        request.run();
        request = new GenericRequest("curse.php?action=use&whichitem=11059&targetplayer=Hairy");
        request.run();

        // Not used if target in Hardcore or Ronin
        assertThat("_pingPongGame", isSetTo(false));
        assertThat(UseItemRequest.maximumUses(ItemPool.PING_PONG_TABLE), is(1));
        assertThat(InventoryManager.getCount(ItemPool.PING_PONG_TABLE), is(1));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));

        assertGetRequest(requests.get(0), "/curse.php", "whichitem=11059");
        assertPostRequest(
            requests.get(1), "/curse.php", "action=use&whichitem=11059&targetplayer=Hairy");
      }
    }

    @Test
    public void winningGameConsumesUse() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.PING_PONG_TABLE),
              withProperty("_pingPongGame", false),
              withNoEffects());
      try (cleanups) {
        client.addResponse(200, html("request/test_use_ping_pong_table_prowess.html"));
        client.addResponse(200, ""); // api.php

        assertThat(UseItemRequest.maximumUses(ItemPool.PING_PONG_TABLE), is(1));

        var request =
            new GenericRequest("curse.php?action=use&whichitem=11059&targetplayer=Blippy+Bloppy");
        request.run();

        // Successful game in records usage
        assertThat("_pingPongGame", isSetTo(true));
        assertThat(UseItemRequest.maximumUses(ItemPool.PING_PONG_TABLE), is(0));
        assertThat(PING_PONG_PROWESS.getCount(KoLConstants.activeEffects), is(30));
        assertThat(InventoryManager.getCount(ItemPool.PING_PONG_TABLE), is(1));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));

        assertPostRequest(
            requests.get(0), "/curse.php", "action=use&whichitem=11059&targetplayer=Blippy+Bloppy");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void losingGameConsumesUse() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.PING_PONG_TABLE),
              withProperty("_pingPongGame", false),
              withNoEffects());
      try (cleanups) {
        client.addResponse(200, html("request/test_use_ping_pong_table_persistence.html"));
        client.addResponse(200, ""); // api.php

        assertThat(UseItemRequest.maximumUses(ItemPool.PING_PONG_TABLE), is(1));

        var request =
            new GenericRequest("curse.php?action=use&whichitem=11059&targetplayer=121572");
        request.run();

        // Successful game in records usage
        assertThat("_pingPongGame", isSetTo(true));
        assertThat(UseItemRequest.maximumUses(ItemPool.PING_PONG_TABLE), is(0));
        assertThat(PING_PONG_PERSISTENCE.getCount(KoLConstants.activeEffects), is(30));
        assertThat(InventoryManager.getCount(ItemPool.PING_PONG_TABLE), is(1));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));

        assertPostRequest(
            requests.get(0), "/curse.php", "action=use&whichitem=11059&targetplayer=121572");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void canOnlyPlayOncePerDay() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.PING_PONG_TABLE),
              withProperty("_pingPongGame", false));
      try (cleanups) {
        client.addResponse(200, html("request/test_use_ping_pong_table_used.html"));

        assertThat(UseItemRequest.maximumUses(ItemPool.PING_PONG_TABLE), is(1));

        var request =
            new GenericRequest("curse.php?action=use&whichitem=11059&targetplayer=121572");
        request.run();

        // Not used if target in Hardcore or Ronin
        assertThat("_pingPongGame", isSetTo(true));
        assertThat(UseItemRequest.maximumUses(ItemPool.PING_PONG_TABLE), is(0));
        assertThat(InventoryManager.getCount(ItemPool.PING_PONG_TABLE), is(1));

        var requests = client.getRequests();
        assertThat(requests, hasSize(1));

        assertPostRequest(
            requests.get(0), "/curse.php", "action=use&whichitem=11059&targetplayer=121572");
      }
    }
  }
}
