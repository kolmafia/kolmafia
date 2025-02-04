package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ShopRequestTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("ShopRequest");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("ShopRequest");
  }

  @Nested
  class Concoctions {
    @Test
    void starChartAlwaysMakesOne() {
      // star throwing star	STAR, ROW139	star chart	star (4)	line (2)

      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.STAR_CHART, 2),
              withItem("star", 8),
              withItem("line", 4));
      try (cleanups) {
        client.addResponse(200, html("request/test_starchart_creation.html"));

        String url = "shop.php?whichshop=starchart&action=buyitem&quantity=2&whichrow=139";
        var request = new GenericRequest(url);
        request.run();
        assertEquals(InventoryManager.getCount(ItemPool.STAR_CHART), 1);
        assertEquals(InventoryManager.getCount(ItemPool.STAR), 4);
        assertEquals(InventoryManager.getCount(ItemPool.LINE), 2);
      }
    }
  }
}
