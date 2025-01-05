package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withCurrentRun;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.helpers.Player;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.concoction.CreateItemRequest;
import net.sourceforge.kolmafia.session.ChoiceManager;
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

  @Nested
  class FreeCrafts {
    @Test
    public void recognisesThorsPliers() {
      var cleanups = Player.withProperty("_thorsPliersCrafting", 1);

      try (cleanups) {
        CreateItemRequest.parseCrafting(
            "craft.php?action=craft&mode=smith&ajax=1&a=95&b=11733&qty=1",
            html("request/test_create_thorspliers.html"));

        assertThat("_thorsPliersCrafting", isSetTo(2));
      }
    }

    @Test
    public void recognisesRapidPrototyping() {
      var cleanups = Player.withProperty("_rapidPrototypingUsed", 1);

      try (cleanups) {
        CreateItemRequest.parseCrafting(
            "craft.php?action=craft&mode=smith&ajax=1&a=95&b=11733&qty=1",
            html("request/test_create_rapidprototyping.html"));

        assertThat("_rapidPrototypingUsed", isSetTo(2));
      }
    }

    @Test
    public void recognisesCornerCutter() {
      var cleanups = Player.withProperty("_expertCornerCutterUsed", 1);

      try (cleanups) {
        CreateItemRequest.parseCrafting(
            "craft.php?action=craft&mode=smith&ajax=1&a=95&b=11733&qty=1",
            html("request/test_create_cornercutter.html"));

        assertThat("_expertCornerCutterUsed", isSetTo(2));
      }
    }

    @Test
    public void recognisesHolidayMultitasking() {
      var cleanups = Player.withProperty("_holidayMultitaskingUsed", 1);

      try (cleanups) {
        CreateItemRequest.parseCrafting(
            "craft.php?action=craft&mode=smith&ajax=1&a=95&b=11733&qty=1",
            html("request/test_create_holidaymulti.html"));

        assertThat("_holidayMultitaskingUsed", isSetTo(2));
      }
    }

    @Test
    public void recognisesCookbookBat() {
      var cleanups = Player.withProperty("_cookbookbatCrafting");

      try (cleanups) {
        CreateItemRequest.parseCrafting(
            "craft.php?action=craft&qty=1&mode=cook&target=423&ajax=1",
            html("request/test_create_cookbookbat.html"));

        assertThat("_cookbookbatCrafting", isSetTo(1));
      }
    }

    @Test
    public void recognisesElfGuard() {
      var cleanups = Player.withProperty("_elfGuardCookingUsed", 1);

      try (cleanups) {
        CreateItemRequest.parseCrafting(
            "craft.php?action=craft&mode=cook&ajax=1&a=304&b=8&qty=1",
            html("request/test_create_elfguard.html"));

        assertThat("_elfGuardCookingUsed", isSetTo(2));
      }
    }

    @Test
    public void recognisesOldSchoolCocktailCrafting() {
      var cleanups = Player.withProperty("_oldSchoolCocktailCraftingUsed", 1);

      try (cleanups) {
        CreateItemRequest.parseCrafting(
            "craft.php?action=craft&mode=cocktail&ajax=1&a=10534&b=9908&qty=1",
            html("request/test_create_oldschool.html"));

        assertThat("_oldSchoolCocktailCraftingUsed", isSetTo(2));
      }
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

  @Nested
  class ShadowForge {
    @Test
    void openingShadowForgeAllowsCreation() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("lastShadowForgeUnlockAdventure", -1),
              withCurrentRun(666),
              withItem(ItemPool.SHADOW_FLUID),
              withItem(ItemPool.SHADOW_FLAME),
              withItem(ItemPool.RUFUS_SHADOW_LODESTONE));

      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, html("request/test_follow_rufus_lodestone.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(302, Map.of("location", List.of("shop.php?whichshop=shadowforge")), "");
        client.addResponse(200, html("request/test_visit_shadow_forge.html"));

        // We have the ingredients for a shadow pill, but the forge is not open
        var concoction = ConcoctionPool.get(ItemPool.SHADOW_PILL);
        assertTrue(concoction.creatable == 0);

        var req = new GenericRequest("adventure.php?snarfblat=" + AdventurePool.SHADOW_RIFT);
        req.run();

        // We followed the lodestone and are now in choice 1500.
        assertTrue(ChoiceManager.handlingChoice);
        assertEquals(1500, ChoiceManager.lastChoice);
        assertFalse(InventoryManager.hasItem(ItemPool.RUFUS_SHADOW_LODESTONE));

        // Go to The Shadow Forge
        var choice = new GenericRequest("choice.php?whichchoice=1500&option=1");
        choice.run();

        // We have unlocked the forge and can craft things
        assertThat("lastShadowForgeUnlockAdventure", isSetTo(666));
        assertTrue(concoction.creatable == 1);
      }
    }
  }
}
