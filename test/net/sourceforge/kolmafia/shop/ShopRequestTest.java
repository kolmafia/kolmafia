package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withAscensions;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSkill;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.AscensionClass;
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

  @Test
  void visitingStillDetectsLights() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withClass(AscensionClass.DISCO_BANDIT),
            withSkill("Superhuman Cocktailcrafting"),
            withAscensions(10),
            withProperty("lastGuildStoreOpen", 10));

    try (cleanups) {
      client.addResponse(200, html("request/test_shop_still.html"));
      KoLCharacter.stillsAvailable = -1;
      assertThat(KoLCharacter.getStillsAvailable(), is(10));

      var requests = client.getRequests();
      assertThat(requests, hasSize(1));
      assertPostRequest(requests.get(0), "/shop.php", "whichshop=still");
    }
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
