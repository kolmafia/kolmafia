package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;

import internal.helpers.Cleanups;
import internal.helpers.RequestLoggerOutput;
import internal.helpers.SessionLoggerOutput;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class FleaMarketRequestTest {
  @BeforeAll
  protected static void beforeEach() {
    KoLCharacter.reset("FleaMarketRequestTest");
    Preferences.reset("FleaMarketRequestTest");
  }

  @Test
  public void testLoggedFleaMarketBuy() {
    RequestLoggerOutput.startStream();
    SessionLoggerOutput.startStream();
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;

    var cleanups = new Cleanups(withHttpClientBuilder(builder));

    try (cleanups) {
      var req =
          new GenericRequest(
              "town_fleamarket.php?pwd&buying=Yep.&which=13&whichitem=823&howmuch=125");
      client.addResponse(200, html("request/test_flea_market_buy.html"));
      req.run();

      var requestText = RequestLoggerOutput.stopStream();
      var sessionText = SessionLoggerOutput.stopStream();

      assertThat(
          requestText,
          containsString(
              "Purchasing cloudy potion from the Flea Market for 125 meat.\nYou acquire an item: cloudy potion"));
      assertThat(
          sessionText,
          containsString(
              "Purchased cloudy potion from Daryl Alenko ( #2395865 ) at the Flea Market for 125 meat."));
    }
  }

  @Test
  public void testFleaMarketTracksSell() {
    RequestLoggerOutput.startStream();
    SessionLoggerOutput.startStream();
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;

    var cleanups =
        new Cleanups(withHttpClientBuilder(builder), withItem(ItemPool.ELEVEN_LEAF_CLOVER, 2));

    try (cleanups) {
      var req =
          new GenericRequest("town_sellflea.php?pwd&whichitem=10881&sellprice=18000&selling=Yep.");
      client.addResponse(200, html("request/test_flea_market_sell.html"));
      req.run();

      var requestText = RequestLoggerOutput.stopStream();
      var sessionText = SessionLoggerOutput.stopStream();

      assertThat(InventoryManager.getCount(ItemPool.ELEVEN_LEAF_CLOVER), is(1));
      assertThat(
          requestText,
          containsString("Placing 11-leaf clover up for sale at the Flea Market for 18000 meat."));
      assertThat(
          sessionText,
          containsString("Placed 11-leaf clover up for sale at the Flea Market for 18000 meat."));
    }
  }
}
