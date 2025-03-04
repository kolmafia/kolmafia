package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withDisabledCoinmaster;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withZonelessCoinmaster;
import static internal.helpers.Player.withoutCoinmasterBuyItem;
import static internal.helpers.Player.withoutCoinmasterSellItem;
import static internal.helpers.Player.withoutSkill;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.helpers.SessionLoggerOutput;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo23ElfArmoryRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.GeneticFiddlingRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.shop.ShopRow;
import net.sourceforge.kolmafia.shop.ShopRowDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
  class LearnCoinmasterItems {
    // Some Coinmasters have variable items for sale:
    // Mr. Store, Swagger Shop, etc.
    //
    // Some Coinmasters have items that have to be unlocked.
    // Until the community has discovered them, we can't put them
    // in coinmasters.txt.
    // SpinMaster Lathe, Fun-a-Log, etc.
    //
    // And some Coinmasters are brand new and we want to report on what
    // they will buy or sell.
    // All Crimbo23 shops started that way.
    //
    // shop.php stores that we model as NPCStores or Coinmasters have a
    // standard format for displaying items on offer; each row contains
    // an item (including quantity) that you exchange for a currency
    // (including quantity) and an associated ROW number to request it.
    //
    // If we already have a class to handle a shop, requests made from
    // KoLmafia will generally use that class. But requests can be made
    // from the Relay Browser (RelayRequest) or scripts (GenericRequest)
    // and shop.php requests will be handled in
    // NPCPurchaseRequest.parseShopResponse.
    //
    // That method will attempt to learn new items and log what needs to
    // be added to npcstores.txt or coinmasters.txt
    //
    // This test class attempts to exercise learning new Coinmaster items.
    //
    // We'll use the Crimbo23 Elf and Pirate Armories since those have
    // both buy and sell items available.
    //
    // The "currency" in the Elf Guard Armory is Elf Army machine parts
    // Elf Guard Armory	sell	3	Elf Guard commandeering gloves	ROW1412
    // Elf Guard Armory	sell	3	Elf Guard officer's sidearm	ROW1413
    // Elf Guard Armory	sell	3	Kelflar vest	ROW1415
    // Elf Guard Armory	sell	3	Elf Guard mouthknife	ROW1416
    // Elf Guard Armory	buy	200	Elf Guard honor present	ROW1411
    //
    // The "currency" in the Crimbuccaneer Junkworks is Crimbuccaneer flotsam
    // Crimbuccaneer Junkworks	sell	3	sawed-off blunderbuss	ROW1420
    // Crimbuccaneer Junkworks	sell	3	Crimbuccaneer shirt	ROW1418
    // Crimbuccaneer Junkworks	sell	3	pegfinger	ROW1422
    // Crimbuccaneer Junkworks	sell	3	shipwright's hammer	ROW1421
    // Crimbuccaneer Junkworks	buy	200	Crimbuccaneer premium booty sack	ROW1417

    @Test
    void canDetectUnknownElfArmory() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      SessionLoggerOutput.startStream();

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPath(Path.STANDARD),
              withDisabledCoinmaster(Crimbo23ElfArmoryRequest.DATA),
              withZonelessCoinmaster(Crimbo23ElfArmoryRequest.DATA),
              withProperty("crimbo23ArmoryControl", "elf"));
      try (cleanups) {
        client.addResponse(200, html("request/test_armory_elf_visit.html"));

        var visit = new GenericRequest("shop.php?whichshop=crimbo23_elf_armory");
        visit.run();

        var text = SessionLoggerOutput.stopStream();

        var expected =
            """
    Visiting Elf Guard Armory
    --------------------
    1411	crimbo23_elf_armory	Elf Guard honor present	Elf Army machine parts (200)
    1412	crimbo23_elf_armory	Elf Army machine parts (3)	Elf Guard commandeering gloves
    1413	crimbo23_elf_armory	Elf Army machine parts (3)	Elf Guard officer's sidearm
    1415	crimbo23_elf_armory	Elf Army machine parts (3)	Kelflar vest
    1416	crimbo23_elf_armory	Elf Army machine parts (3)	Elf Guard mouthknife
    --------------------
    Elf Guard Armory	ROW1412	Elf Army machine parts (3)	Elf Guard commandeering gloves
    Elf Guard Armory	ROW1415	Elf Army machine parts (3)	Kelflar vest
    Elf Guard Armory	ROW1416	Elf Army machine parts (3)	Elf Guard mouthknife
    Elf Guard Armory	ROW1413	Elf Army machine parts (3)	Elf Guard officer's sidearm
    Elf Guard Armory	ROW1411	Elf Guard honor present	Elf Army machine parts (200)
    --------------------""";

        assertThat(text, containsString(expected));

        var requests = client.getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(requests.get(0), "/shop.php", "whichshop=crimbo23_elf_armory");
      }
    }

    @Test
    void canDetectUnknownElfArmoryItems() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      SessionLoggerOutput.startStream();

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPath(Path.STANDARD),
              withoutCoinmasterSellItem(
                  Crimbo23ElfArmoryRequest.DATA, ItemPool.get(ItemPool.KELFLAR_VEST)),
              withoutCoinmasterBuyItem(
                  Crimbo23ElfArmoryRequest.DATA, ItemPool.get(ItemPool.ELF_GUARD_HONOR_PRESENT)),
              withZonelessCoinmaster(Crimbo23ElfArmoryRequest.DATA),
              withProperty("crimbo23ArmoryControl", "elf"));
      try (cleanups) {
        client.addResponse(200, html("request/test_armory_elf_visit.html"));

        var visit = new GenericRequest("shop.php?whichshop=crimbo23_elf_armory");
        visit.run();

        var text = SessionLoggerOutput.stopStream();

        var expected =
            """
    Visiting Elf Guard Armory
    --------------------
    1411	crimbo23_elf_armory	Elf Guard honor present	Elf Army machine parts (200)
    1415	crimbo23_elf_armory	Elf Army machine parts (3)	Kelflar vest
    --------------------
    Elf Guard Armory	buy	200	Elf Guard honor present	ROW1411
    --------------------
    Elf Guard Armory	sell	3	Kelflar vest	ROW1415
    --------------------""";

        assertThat(text, containsString(expected));

        var requests = client.getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(requests.get(0), "/shop.php", "whichshop=crimbo23_elf_armory");
      }
    }
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
    // another item - but KoLmafia can't currently model it that way.
    //
    // For example here are the items available in the Elf Guard Armory:
    //
    // Elf Guard Armory	sell	3	Elf Guard commandeering gloves	ROW1412
    // Elf Guard Armory	sell	3	Elf Guard officer's sidearm	ROW1413
    // Elf Guard Armory	sell	3	Kelflar vest	ROW1415
    // Elf Guard Armory	sell	3	Elf Guard mouthknife	ROW1416
    // Elf Guard Armory	buy	200	Elf Guard honor present	ROW1411

    @Test
    void canVisitElfArmoryUsingCoinMasterRequest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      SessionLoggerOutput.startStream();

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPath(Path.STANDARD),
              withZonelessCoinmaster(Crimbo23ElfArmoryRequest.DATA),
              withProperty("crimbo23ArmoryControl", "elf"),
              withItem(ItemPool.ELF_GUARD_COMMANDEERING_GLOVES, 14),
              withItem(ItemPool.KELFLAR_VEST, 25),
              withItem(ItemPool.ELF_GUARD_MOUTHKNIFE, 21),
              withItem(ItemPool.ELF_GUARD_OFFICERS_SIDEARM, 22),
              withItem(ItemPool.ELF_ARMY_MACHINE_PARTS, 49));
      try (cleanups) {
        client.addResponse(200, html("request/test_armory_elf_visit.html"));

        var visit = Crimbo23ElfArmoryRequest.DATA.getRequest();
        visit.run();

        var text = SessionLoggerOutput.stopStream();

        var expected = """
    Created an empty checkpoint.

    Visiting Elf Guard Armory""";

        assertThat(text, containsString(expected));

        var requests = client.getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(requests.get(0), "/shop.php", "whichshop=crimbo23_elf_armory");
      }
    }

    @Test
    void canVisitElfArmoryUsingGenericRequest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      SessionLoggerOutput.startStream();

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPath(Path.STANDARD),
              withZonelessCoinmaster(Crimbo23ElfArmoryRequest.DATA),
              withProperty("crimbo23ArmoryControl", "elf"),
              withItem(ItemPool.ELF_GUARD_COMMANDEERING_GLOVES, 14),
              withItem(ItemPool.KELFLAR_VEST, 25),
              withItem(ItemPool.ELF_GUARD_MOUTHKNIFE, 21),
              withItem(ItemPool.ELF_GUARD_OFFICERS_SIDEARM, 22),
              withItem(ItemPool.ELF_ARMY_MACHINE_PARTS, 49));
      try (cleanups) {
        client.addResponse(200, html("request/test_armory_elf_visit.html"));

        var visit = new GenericRequest("shop.php?whichshop=crimbo23_elf_armory");
        visit.run();

        var text = SessionLoggerOutput.stopStream();
        var expected = """
    Visiting Elf Guard Armory""";

        assertThat(text, containsString(expected));

        var requests = client.getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(requests.get(0), "/shop.php", "whichshop=crimbo23_elf_armory");
      }
    }

    @Test
    void canBuyFromElfArmoryUsingCoinMasterRequest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      SessionLoggerOutput.startStream();

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPath(Path.STANDARD),
              withZonelessCoinmaster(Crimbo23ElfArmoryRequest.DATA),
              withProperty("crimbo23ArmoryControl", "elf"),
              withItem(ItemPool.ELF_GUARD_HONOR_PRESENT, 0),
              withItem(ItemPool.ELF_ARMY_MACHINE_PARTS, 277));
      try (cleanups) {
        client.addResponse(200, html("request/test_armory_elf_buy.html"));
        client.addResponse(200, "");

        var buy =
            Crimbo23ElfArmoryRequest.DATA.getRequest(
                true, new AdventureResult[] {ItemPool.get(ItemPool.ELF_GUARD_HONOR_PRESENT, 1)});
        buy.run();

        var text = SessionLoggerOutput.stopStream();

        var expected =
            """
    Created an empty checkpoint.

    Trade 200 piles of Elf Army machine parts for 1 Elf Guard honor present
    You acquire an item: Elf Guard honor present""";

        assertThat(text, containsString(expected));

        assertEquals(1, InventoryManager.getCount(ItemPool.ELF_GUARD_HONOR_PRESENT));
        assertEquals(77, InventoryManager.getCount(ItemPool.ELF_ARMY_MACHINE_PARTS));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0),
            "/shop.php",
            "whichshop=crimbo23_elf_armory&action=buyitem&ajax=1&quantity=1&whichrow=1411");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void canBuyFromElfArmoryUsingGenericRequest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      SessionLoggerOutput.startStream();

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPath(Path.STANDARD),
              withZonelessCoinmaster(Crimbo23ElfArmoryRequest.DATA),
              withProperty("crimbo23ArmoryControl", "elf"),
              withItem(ItemPool.ELF_GUARD_HONOR_PRESENT, 0),
              withItem(ItemPool.ELF_ARMY_MACHINE_PARTS, 277));
      try (cleanups) {
        client.addResponse(200, html("request/test_armory_elf_buy.html"));
        client.addResponse(200, "");

        var buy =
            new GenericRequest(
                "shop.php?whichshop=crimbo23_elf_armory&action=buyitem&quantity=1&whichrow=1411");
        buy.run();

        var text = SessionLoggerOutput.stopStream();

        var expected =
            """
    Trade 200 piles of Elf Army machine parts for 1 Elf Guard honor present
    You acquire an item: Elf Guard honor present""";

        assertThat(text, containsString(expected));

        assertEquals(1, InventoryManager.getCount(ItemPool.ELF_GUARD_HONOR_PRESENT));
        assertEquals(77, InventoryManager.getCount(ItemPool.ELF_ARMY_MACHINE_PARTS));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0),
            "/shop.php",
            "whichshop=crimbo23_elf_armory&action=buyitem&quantity=1&whichrow=1411");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void canSellToElfArmoryUsingCoinMasterRequest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      SessionLoggerOutput.startStream();

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPath(Path.STANDARD),
              withZonelessCoinmaster(Crimbo23ElfArmoryRequest.DATA),
              withProperty("crimbo23ArmoryControl", "elf"),
              withItem(ItemPool.ELF_GUARD_COMMANDEERING_GLOVES, 14),
              withItem(ItemPool.ELF_ARMY_MACHINE_PARTS, 49));
      try (cleanups) {
        client.addResponse(200, html("request/test_armory_elf_sell.html"));
        client.addResponse(200, "");

        var buy =
            Crimbo23ElfArmoryRequest.DATA.getRequest(
                false,
                new AdventureResult[] {ItemPool.get(ItemPool.ELF_GUARD_COMMANDEERING_GLOVES, 13)});
        buy.run();

        var text = SessionLoggerOutput.stopStream();

        var expected =
            """
    Created an empty checkpoint.

    Trade 13 pairs of Elf Guard commandeering gloves for 39 piles of Elf Army machine parts
    You acquire Elf Army machine parts (39)""";

        assertThat(text, containsString(expected));

        assertEquals(1, InventoryManager.getCount(ItemPool.ELF_GUARD_COMMANDEERING_GLOVES));
        assertEquals(88, InventoryManager.getCount(ItemPool.ELF_ARMY_MACHINE_PARTS));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0),
            "/shop.php",
            "whichshop=crimbo23_elf_armory&action=buyitem&ajax=1&quantity=13&whichrow=1412");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void canSellToElfArmoryUsingGenericRequest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      SessionLoggerOutput.startStream();

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPath(Path.STANDARD),
              withZonelessCoinmaster(Crimbo23ElfArmoryRequest.DATA),
              withProperty("crimbo23ArmoryControl", "elf"),
              withItem(ItemPool.ELF_GUARD_COMMANDEERING_GLOVES, 14),
              withItem(ItemPool.ELF_ARMY_MACHINE_PARTS, 49));
      try (cleanups) {
        client.addResponse(200, html("request/test_armory_elf_sell.html"));
        client.addResponse(200, "");

        var buy =
            new GenericRequest(
                "shop.php?whichshop=crimbo23_elf_armory&action=buyitem&quantity=13&whichrow=1412");
        buy.run();

        var text = SessionLoggerOutput.stopStream();

        var expected =
            """
    Trade 13 pairs of Elf Guard commandeering gloves for 39 piles of Elf Army machine parts
    You acquire Elf Army machine parts (39)""";

        assertThat(text, containsString(expected));

        assertEquals(1, InventoryManager.getCount(ItemPool.ELF_GUARD_COMMANDEERING_GLOVES));
        assertEquals(88, InventoryManager.getCount(ItemPool.ELF_ARMY_MACHINE_PARTS));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0),
            "/shop.php",
            "whichshop=crimbo23_elf_armory&action=buyitem&quantity=13&whichrow=1412");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }

  @Nested
  class SkillCoinmasters {
    @Test
    void canVisitGeneticFiddlingUsingCoinMasterRequest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      SessionLoggerOutput.startStream();

      var cleanups = new Cleanups(withHttpClientBuilder(builder), withPath(Path.NUCLEAR_AUTUMN));
      try (cleanups) {
        client.addResponse(200, html("request/test_shop_mutate_visit.html"));

        var visit = GeneticFiddlingRequest.DATA.getRequest();
        visit.run();

        var text = SessionLoggerOutput.stopStream();
        assertTrue(text.contains("Visiting Genetic Fiddling"));

        var requests = client.getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(requests.get(0), "/shop.php", "whichshop=mutate");
      }
    }

    @Test
    void canVisitGeneticFiddlingUsingGenericRequest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      SessionLoggerOutput.startStream();

      var cleanups = new Cleanups(withHttpClientBuilder(builder), withPath(Path.NUCLEAR_AUTUMN));
      try (cleanups) {
        client.addResponse(200, html("request/test_shop_mutate_visit.html"));

        var visit = new GenericRequest("shop.php?whichshop=mutate");
        visit.run();

        var text = SessionLoggerOutput.stopStream();
        assertTrue(text.contains("Visiting Genetic Fiddling"));

        var requests = client.getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(requests.get(0), "/shop.php", "whichshop=mutate");
      }
    }

    @Test
    void canBuyFromGeneticFiddlingUsingCoinMasterRequest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      SessionLoggerOutput.startStream();

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPath(Path.NUCLEAR_AUTUMN),
              withItem(ItemPool.RAD, 120),
              withoutSkill("Extra Muscles"));
      try (cleanups) {
        client.addResponse(200, html("request/test_shop_mutate_bought_skill.html"));
        client.addResponse(200, "");

        int row = 861;
        ShopRow shopRow = ShopRowDatabase.getShopRow(row);

        var buy = GeneticFiddlingRequest.DATA.getRequest(shopRow, 1);
        buy.run();

        var text = SessionLoggerOutput.stopStream();
        assertTrue(text.contains("Trade 90 rads to learn Extra Muscles"));
        assertTrue(text.contains("You learned a new skill: Extra Muscles"));

        assertEquals(30, InventoryManager.getCount(ItemPool.RAD));
        assertTrue(KoLCharacter.hasSkill("Extra Muscles"));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0),
            "/shop.php",
            "whichshop=mutate&action=buyitem&whichrow=861&quantity=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void canBuyFromGeneticFiddlingUsingGenericRequest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      SessionLoggerOutput.startStream();

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPath(Path.NUCLEAR_AUTUMN),
              withItem(ItemPool.RAD, 120),
              withoutSkill("Extra Muscles"));
      try (cleanups) {
        client.addResponse(200, html("request/test_shop_mutate_bought_skill.html"));
        client.addResponse(200, "");

        assertEquals(120, InventoryManager.getCount(ItemPool.RAD));

        var buy =
            new GenericRequest("shop.php?whichshop=mutate&action=buyitem&quantity=1&whichrow=861");
        buy.run();

        var text = SessionLoggerOutput.stopStream();
        assertTrue(text.contains("Trade 90 rads to learn Extra Muscles"));
        assertTrue(text.contains("You learned a new skill: Extra Muscles"));

        assertEquals(30, InventoryManager.getCount(ItemPool.RAD));
        assertTrue(KoLCharacter.hasSkill("Extra Muscles"));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0),
            "/shop.php",
            "whichshop=mutate&action=buyitem&quantity=1&whichrow=861");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }
}
