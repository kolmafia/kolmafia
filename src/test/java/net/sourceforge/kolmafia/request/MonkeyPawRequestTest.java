package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class MonkeyPawRequestTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("MonkeyPawRequest");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("MonkeyPawRequest");
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3, 4, 5})
  public void usingMonkeyPawDetectsWishesUsed(int wishesUsed) {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withItem(ItemPool.BAG_OF_FOREIGN_BRIBES, 0),
            withProperty("_monkeyPawWishesUsed", 0));
    try (cleanups) {
      client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
      client.addResponse(200, html("request/test_monkey_paw_wish_" + wishesUsed + ".html"));
      if (wishesUsed > 0) {
        client.addResponse(200, ""); // api.php
      }
      if (wishesUsed < 5) {
        client.addResponse(200, html("request/test_monkey_paw_wish_" + (wishesUsed + 1) + ".html"));
        client.addResponse(200, ""); // api.php
      }

      var pawRequest = new GenericRequest("main.php?action=cmonk", false);
      pawRequest.run();

      // KoL redirects to choice.php even if there are no choice options.
      // We know how to work around that.

      // Since we parsed the state of the paw (how many fingers are
      // raised), we know how many wishes have been used.
      assertThat("_monkeyPawWishesUsed", isSetTo(wishesUsed));

      if (wishesUsed < 5) {
        // We are in a choice.
        assertTrue(ChoiceManager.handlingChoice);
        assertEquals(1501, ChoiceManager.lastChoice);

        // Reset the number of wishes used.
        Preferences.setInteger("_monkeyPawWishesUsed", 0);

        // Submit a wish.
        var wishRequest =
            new GenericRequest("choice.php?whichchoice=1501&option=1&wish=bag+of+foreign+bribes");
        wishRequest.run();

        // Our wish was granted
        assertTrue(InventoryManager.hasItem(ItemPool.BAG_OF_FOREIGN_BRIBES));

        // Since we parsed the duration of the curse, we know how many
        // wishes have been used.
        assertThat("_monkeyPawWishesUsed", isSetTo(wishesUsed + 1));
      }

      var requests = client.getRequests();

      int i = 0;
      assertGetRequest(requests.get(i++), "/main.php", "action=cmonk");
      assertGetRequest(requests.get(i++), "/choice.php", "forceoption=0");
      if (wishesUsed > 0) {
        assertPostRequest(requests.get(i++), "/api.php", "what=status&for=KoLmafia");
      }
      if (wishesUsed < 5) {
        assertPostRequest(
            requests.get(i++),
            "/choice.php",
            "whichchoice=1501&option=1&wish=bag+of+foreign+bribes");
      }
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3, 4})
  public void makingSuccessfulWishIncrementsWishesUsed(int wishesUsed) {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withItem(ItemPool.CURSED_MONKEY_PAW, 1),
            withItem(ItemPool.BAG_OF_FOREIGN_BRIBES, 0),
            withProperty("_monkeyPawWishesUsed", 0));
    try (cleanups) {
      client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
      client.addResponse(200, html("request/test_monkey_paw_wish_" + wishesUsed + ".html"));
      if (wishesUsed > 0) {
        client.addResponse(200, ""); // api.php
      }
      client.addResponse(200, html("request/test_monkey_paw_wish_" + (wishesUsed + 1) + ".html"));
      client.addResponse(200, ""); // api.php

      var pawRequest = new MonkeyPawRequest("bag of foreign bribes");
      pawRequest.run();

      // Our wish was granted
      assertTrue(InventoryManager.hasItem(ItemPool.BAG_OF_FOREIGN_BRIBES));

      // We know how many wishes have been used
      assertThat("_monkeyPawWishesUsed", isSetTo(wishesUsed + 1));

      var requests = client.getRequests();

      int i = 0;
      assertGetRequest(requests.get(i++), "/main.php", "action=cmonk");
      assertGetRequest(requests.get(i++), "/choice.php", "forceoption=0");
      if (wishesUsed > 0) {
        assertPostRequest(requests.get(i++), "/api.php", "what=status&for=KoLmafia");
      }
      assertPostRequest(
          requests.get(i++), "/choice.php", "whichchoice=1501&wish=bag of foreign bribes&option=1");
    }
  }
}
