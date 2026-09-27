package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withItemInCloset;
import static internal.helpers.Player.withNoItems;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class BreakfastManagerTest {

  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("BreakfastManagerTest");
  }

  @BeforeEach
  void beforeEach() {
    Preferences.reset("BreakfastManagerTest");
  }

  @Nested
  class Toys {

    @Test
    public void shouldPumpHighTopsThrice() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withInteractivity(true),
              withProperty("useCrimboToysSoftcore", true),
              withProperty("_highTopPumps", 0),
              withProperty("highTopPumped", 0),
              withItem(ItemPool.PUMP_UP_HIGH_TOPS));
      try (cleanups) {
        client.addResponse(200, html("request/test_pump_up_high_tops.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_pump_up_high_tops.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_pump_up_high_tops.html"));
        client.addResponse(200, ""); // api.php

        BreakfastManager.useToys();

        assertThat("_highTopPumps", isSetTo(3));
        assertThat("highTopPumped", isSetTo(3));

        var requests = client.getRequests();
        assertThat(requests, hasSize(6));

        assertPostRequest(requests.get(0), "/inv_use.php", "whichitem=9961&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(2), "/inv_use.php", "whichitem=9961&ajax=1");
        assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(4), "/inv_use.php", "whichitem=9961&ajax=1");
        assertPostRequest(requests.get(5), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void shouldPumpHighTopsTwice() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withInteractivity(true),
              withProperty("useCrimboToysSoftcore", true),
              withProperty("_highTopPumps", 1),
              withProperty("highTopPumped", 1),
              withItem(ItemPool.PUMP_UP_HIGH_TOPS));
      try (cleanups) {
        client.addResponse(200, html("request/test_pump_up_high_tops.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_pump_up_high_tops.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_pump_up_high_tops.html"));
        client.addResponse(200, ""); // api.php

        BreakfastManager.useToys();

        assertThat("_highTopPumps", isSetTo(3));
        assertThat("highTopPumped", isSetTo(3));

        var requests = client.getRequests();
        assertThat(requests, hasSize(4));

        assertPostRequest(requests.get(0), "/inv_use.php", "whichitem=9961&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(2), "/inv_use.php", "whichitem=9961&ajax=1");
        assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void shouldPumpHighTopsOnce() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withInteractivity(true),
              withProperty("useCrimboToysSoftcore", true),
              withProperty("_highTopPumps", 2),
              withProperty("highTopPumped", 2),
              withItem(ItemPool.PUMP_UP_HIGH_TOPS));
      try (cleanups) {
        client.addResponse(200, html("request/test_pump_up_high_tops.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_pump_up_high_tops.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_pump_up_high_tops.html"));
        client.addResponse(200, ""); // api.php

        BreakfastManager.useToys();

        assertThat("_highTopPumps", isSetTo(3));
        assertThat("highTopPumped", isSetTo(3));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));

        assertPostRequest(requests.get(0), "/inv_use.php", "whichitem=9961&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void shouldNotOverpumpHighTops() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withInteractivity(true),
              withProperty("useCrimboToysSoftcore", true),
              withProperty("_highTopPumps", 3),
              withProperty("highTopPumped", 10),
              withItem(ItemPool.PUMP_UP_HIGH_TOPS));
      try (cleanups) {
        client.addResponse(200, html("request/test_pump_up_high_tops.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_pump_up_high_tops.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_pump_up_high_tops.html"));
        client.addResponse(200, ""); // api.php

        BreakfastManager.useToys();

        assertThat("_highTopPumps", isSetTo(3));
        assertThat("highTopPumped", isSetTo(10));

        var requests = client.getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void shouldUnclosetAndPumpHighTopsOnce() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withInteractivity(true),
              withProperty("useCrimboToysSoftcore", true),
              withProperty("_highTopPumps", 2),
              withProperty("highTopPumped", 2),
              withNoItems(),
              withItemInCloset(ItemPool.PUMP_UP_HIGH_TOPS));
      try (cleanups) {
        client.addResponse(200, html("request/test_uncloset_pump_up_high_tops.html"));
        client.addResponse(200, html("request/test_pump_up_high_tops.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_pump_up_high_tops.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_pump_up_high_tops.html"));
        client.addResponse(200, ""); // api.php

        BreakfastManager.useToys();

        assertThat("_highTopPumps", isSetTo(3));
        assertThat("highTopPumped", isSetTo(3));

        var requests = client.getRequests();
        assertThat(requests, hasSize(3));

        assertGetRequest(
            requests.get(0), "/inventory.php", "action=closetpull&ajax=1&whichitem=9961&qty=1");
        assertPostRequest(requests.get(1), "/inv_use.php", "whichitem=9961&ajax=1");
        assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }
}
