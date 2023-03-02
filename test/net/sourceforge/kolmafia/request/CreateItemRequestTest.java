package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.helpers.Player;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class CreateItemRequestTest {
  @BeforeAll
  public static void init() {
    KoLCharacter.reset("CreateItemRequestTest");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("CreateItemRequestTest");
  }

  @Test
  public void recognisesCookbookBatFreeCraft() {
    var cleanups = Player.withProperty("_cookbookbatCrafting");

    try (cleanups) {
      CreateItemRequest.parseCrafting(
          "craft.php?action=craft&qty=1&mode=cook&target=423&ajax=1",
          html("request/test_create_cookbookbat.html"));

      assertThat("_cookbookbatCrafting", isSetTo(1));
    }
  }

  @Nested
  class NiceWarmBeer {
    @Test
    void warmBeerCraftedWithSchlitz() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("hasShaker", true),
              withItem(ItemPool.SCHLITZ),
              withItem(ItemPool.GRUBBY_WOOL_BEERWARMER));

      try (cleanups) {
        client.addResponse(200, html("request/test_warm_beer_schlitz.html"));
        client.addResponse(200, ""); // api.php

        // You can create a nice warm beer with Schlitz
        var concoction = ConcoctionPool.get(ItemPool.NICE_WARM_BEER);
        assertTrue(concoction.creatable == 1);

        // Force it.

        // craft.php?action=craft&mode=cocktail&a=81&b=11096&qty=1
        var req =
            new GenericRequest(
                "craft.php?action=craft&mode=cocktail&a="
                    + ItemPool.SCHLITZ
                    + "&b="
                    + ItemPool.GRUBBY_WOOL_BEERWARMER
                    + "&qty=1");
        req.run();

        // You mix up a refreshing cocktail.
        assertTrue(req.responseText.contains("You mix up a refreshing cocktail."));
        assertFalse(InventoryManager.hasItem(ItemPool.SCHLITZ));
        assertFalse(InventoryManager.hasItem(ItemPool.GRUBBY_WOOL_BEERWARMER));
        assertTrue(InventoryManager.hasItem(ItemPool.NICE_WARM_BEER));
      }
    }

    @Test
    void warmBeerNotCraftedWithWiller() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("hasShaker", true),
              withItem(ItemPool.WILLER),
              withItem(ItemPool.GRUBBY_WOOL_BEERWARMER));

      try (cleanups) {
        client.addResponse(200, html("request/test_warm_beer_willer.html"));
        client.addResponse(200, ""); // api.php

        // You can't create a nice warm beer with Willer
        var concoction = ConcoctionPool.get(ItemPool.NICE_WARM_BEER);
        assertTrue(concoction.creatable == 0);

        // Force it.

        // craft.php?action=craft&mode=cocktail&a=81&b=11096&qty=1
        var req =
            new GenericRequest(
                "craft.php?action=craft&mode=cocktail&a="
                    + ItemPool.WILLER
                    + "&b="
                    + ItemPool.GRUBBY_WOOL_BEERWARMER
                    + "&qty=1");
        req.run();

        // Those two items don't combine to make a refreshing cocktail.
        assertTrue(req.responseText.contains("Those two items don't combine"));
        assertTrue(InventoryManager.hasItem(ItemPool.WILLER));
        assertTrue(InventoryManager.hasItem(ItemPool.GRUBBY_WOOL_BEERWARMER));
        assertFalse(InventoryManager.hasItem(ItemPool.NICE_WARM_BEER));
      }
    }
  }
}
