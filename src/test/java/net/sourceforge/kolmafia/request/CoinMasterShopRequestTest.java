package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.helpers.SessionLoggerOutput;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.coinmaster.shop.MrStore2002Request;
import net.sourceforge.kolmafia.request.coinmaster.shop.SeptEmberCenserRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class CoinMasterShopRequestTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("CoinMasterShopRequestTest");
  }

  @BeforeEach
  void beforeEach() {
    Preferences.reset("CoinMasterShopRequestTest");
  }

  @Nested
  class Tokens {
    // shop.php coinmasters can have tokens for currency.
    // Test that this works and that the preference doesn't bobble

    @Test
    void canBuyFromMrStore2002() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      SessionLoggerOutput.startStream();

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.MR_STORE_2002_CATALOG, 1),
              withProperty("availableMrStore2002Credits", 2),
              withProperty("_2002MrStoreCreditsCollected", true),
              withProperty("lastKingLiberation", 0),
              withProperty("logPreferenceChange", true));
      try (cleanups) {
        client.addResponse(200, html("request/test_buy_from_mr_store_2002_2.html"));
        client.addResponse(200, "");

        var data = MrStore2002Request.MR_STORE_2002;
        var request =
            data.getRequest(true, new AdventureResult[] {ItemPool.get("Giant black monolith", 1)});
        request.run();
        assertThat("availableMrStore2002Credits", isSetTo(1));

        var text = SessionLoggerOutput.stopStream();
        var expected =
            """
            Trade 1 Mr. Store 2002 Credit for 1 Giant black monolith
            You acquire an item: Giant black monolith
            Preference availableMrStore2002Credits changed from 2 to 1
            Preference _concoctionDatabaseRefreshes changed from 1 to 2""";
        assertThat(text, containsString(expected));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));

        assertPostRequest(
            requests.get(0),
            "/shop.php",
            "whichshop=mrstore2002&action=buyitem&quantity=1&whichrow=1389");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void canBuyFromSeptEmberCenser() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      SessionLoggerOutput.startStream();

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.SEPTEMBER_CENSER, 1),
              withProperty("availableSeptEmbers", 3),
              withProperty("_septEmberBalanceChecked", true),
              withProperty("lastKingLiberation", 0),
              withProperty("logPreferenceChange", true));
      try (cleanups) {
        client.addResponse(200, html("request/test_buy_from_sept_ember_censer.html"));
        client.addResponse(200, "");

        var data = SeptEmberCenserRequest.SEPTEMBER_CENSER;
        var request =
            data.getRequest(true, new AdventureResult[] {ItemPool.get("wheel of camembert", 1)});
        request.run();
        assertThat("availableSeptEmbers", isSetTo(2));

        var text = SessionLoggerOutput.stopStream();
        var expected =
            """
            Trade 1 Ember for 1 wheel of camembert
            You acquire an item: wheel of camembert
            Preference availableSeptEmbers changed from 3 to 2
            Preference _concoctionDatabaseRefreshes changed from 1 to 2""";
        assertThat(text, containsString(expected));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));

        assertPostRequest(
            requests.get(0),
            "/shop.php",
            "whichshop=september&action=buyitem&quantity=1&whichrow=1517");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }
}
