package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

public class CoinMasterRequestTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("CoinMasterRequestTest");
  }

  @BeforeEach
  void beforeEach() {
    Preferences.reset("CoinMasterRequestTest");
  }

  @Nested
  class SingleBuySellAction {
    // Some Coinmasters will both buy (trade items for currency) and
    // sell (trade currency for items).
    //
    // One might expect the "action" field to be different (as it is for
    // the Dimemaster and QuartersMaster) - which are not shop.php - but
    // the Crimbo23 Elf and Pirate Armory CoinMasters ARE shop.php and
    // use the same action - "buyitem" - for both cases.
    //
    // Perhaps KoL sees them both as buying - you can buy currency or
    // another item - but KoLmafia can't model it that way.
    //
    // For example here are the items available in the Elf Guard Armory:
    //
    // Elf Guard Armory	sell	3	Elf Guard commandeering gloves	ROW1412
    // Elf Guard Armory	sell	3	Elf Guard officer's sidearm	ROW1413
    // Elf Guard Armory	sell	3	Kelflar vest	ROW1415
    // Elf Guard Armory	sell	3	Elf Guard mouthknife	ROW1416
    // Elf Guard Armory	buy	200	Elf Guard honor present	ROW1411

    /*
    @Test
    void visitingMrStore2020UsesCatalog() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPath(Path.STANDARD),
              withItem(ItemPool.MR_STORE_2002_CATALOG),
              withProperty("availableCoinMasterCredits", 0),
              withProperty("_2002MrStoreCreditsCollected", false));
      try (cleanups) {
        client.addResponse(200, html("request/test_use_mr_store_2002_catalog.html"));
        client.addResponse(200, html("request/test_visit_mr_store_2002.html"));

        var request = new CoinMasterRequest();
        request.run();

        assertThat("_2002MrStoreCreditsCollected", isSetTo(true));
        assertThat("availableCoinMasterCredits", isSetTo(3));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0),
            "/inv_use.php",
            "whichitem=" + ItemPool.MR_STORE_2002_CATALOG + "&ajax=1");
        assertPostRequest(requests.get(1), "/shop.php", "whichshop=mrstore2002");
      }
    }
    */
  }
}
