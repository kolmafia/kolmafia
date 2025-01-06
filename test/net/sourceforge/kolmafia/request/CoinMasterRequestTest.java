package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withDisabledCoinmaster;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withoutCoinmasterBuyItem;
import static internal.helpers.Player.withoutCoinmasterSellItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.helpers.SessionLoggerOutput;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo23ElfArmoryRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
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
              withProperty("crimbo23ArmoryControl", "elf"));
      try (cleanups) {
        client.addResponse(200, html("request/test_armory_elf_visit.html"));

        var visit = new GenericRequest("shop.php?whichshop=crimbo23_elf_armory");
        visit.run();

        var text = SessionLoggerOutput.stopStream();
        assertTrue(
            text.contains(
                "Elf Guard Armory\tROW1412\tElf Army machine parts (3)\tElf Guard commandeering gloves"));
        assertTrue(
            text.contains("Elf Guard Armory\tROW1415\tElf Army machine parts (3)\tKelflar vest"));
        assertTrue(
            text.contains(
                "Elf Guard Armory\tROW1416\tElf Army machine parts (3)\tElf Guard mouthknife"));
        assertTrue(
            text.contains(
                "Elf Guard Armory\tROW1413\tElf Army machine parts (3)\tElf Guard officer's sidearm"));
        assertTrue(
            text.contains(
                "Elf Guard Armory\tROW1411\tElf Guard honor present\tElf Army machine parts (200)"));

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
              withProperty("crimbo23ArmoryControl", "elf"));
      try (cleanups) {
        client.addResponse(200, html("request/test_armory_elf_visit.html"));

        var visit = new GenericRequest("shop.php?whichshop=crimbo23_elf_armory");
        visit.run();

        var text = SessionLoggerOutput.stopStream();
        assertFalse(
            text.contains("Elf Guard Armory\tsell\t3\tElf Guard commandeering gloves\tROW1412"));
        assertFalse(
            text.contains("Elf Guard Armory\tsell\t3\tElf Guard officer's sidearm\tROW1413"));
        assertTrue(text.contains("Elf Guard Armory\tsell\t3\tKelflar vest\tROW1415"));
        assertFalse(text.contains("Elf Guard Armory\tsell\t3\tElf Guard mouthknife\tROW1416"));
        assertTrue(text.contains("Elf Guard Armory\tbuy\t200\tElf Guard honor present\tROW1411"));

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
    // another item - but KoLmafia can't model it that way.
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
              withProperty("crimbo23ArmoryControl", "elf"),
              withItem(ItemPool.ELF_GUARD_COMMANDEERING_GLOVES, 14),
              withItem(ItemPool.KELFLAR_VEST, 25),
              withItem(ItemPool.ELF_GUARD_MOUTHKNIFE, 21),
              withItem(ItemPool.ELF_GUARD_OFFICERS_SIDEARM, 22),
              withItem(ItemPool.ELF_ARMY_MACHINE_PARTS, 49));
      try (cleanups) {
        client.addResponse(200, html("request/test_armory_elf_visit.html"));

        var visit = new Crimbo23ElfArmoryRequest();
        visit.run();

        var text = SessionLoggerOutput.stopStream();
        assertFalse(
            text.contains("Elf Guard Armory\tsell\t3\tElf Guard commandeering gloves\tROW1412"));
        assertFalse(
            text.contains("Elf Guard Armory\tsell\t3\tElf Guard officer's sidearm\tROW1413"));
        assertFalse(text.contains("Elf Guard Armory\tsell\t3\tKelflar vest\tROW1415"));
        assertFalse(text.contains("Elf Guard Armory\tsell\t3\tElf Guard mouthknife\tROW1416"));
        assertFalse(text.contains("Elf Guard Armory\tbuy\t200\tElf Guard honor present\tROW1411"));
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
        assertFalse(
            text.contains("Elf Guard Armory\tsell\t3\tElf Guard commandeering gloves\tROW1412"));
        assertFalse(
            text.contains("Elf Guard Armory\tsell\t3\tElf Guard officer's sidearm\tROW1413"));
        assertFalse(text.contains("Elf Guard Armory\tsell\t3\tKelflar vest\tROW1415"));
        assertFalse(text.contains("Elf Guard Armory\tsell\t3\tElf Guard mouthknife\tROW1416"));
        assertFalse(text.contains("Elf Guard Armory\tbuy\t200\tElf Guard honor present\tROW1411"));

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
              withProperty("crimbo23ArmoryControl", "elf"),
              withItem(ItemPool.ELF_GUARD_HONOR_PRESENT, 0),
              withItem(ItemPool.ELF_ARMY_MACHINE_PARTS, 277));
      try (cleanups) {
        client.addResponse(200, html("request/test_armory_elf_buy.html"));
        client.addResponse(200, "");

        var buy =
            new Crimbo23ElfArmoryRequest(true, ItemPool.get(ItemPool.ELF_GUARD_HONOR_PRESENT, 1));
        buy.run();

        var text = SessionLoggerOutput.stopStream();
        assertTrue(
            text.contains(
                "trading 200 piles of Elf Army machine parts for 1 Elf Guard honor present"));
        assertFalse(
            text.contains("Elf Guard Armory\tsell\t3\tElf Guard commandeering gloves\tROW1412"));
        assertFalse(
            text.contains("Elf Guard Armory\tsell\t3\tElf Guard officer's sidearm\tROW1413"));
        assertFalse(text.contains("Elf Guard Armory\tsell\t3\tKelflar vest\tROW1415"));
        assertFalse(text.contains("Elf Guard Armory\tsell\t3\tElf Guard mouthknife\tROW1416"));
        assertFalse(text.contains("Elf Guard Armory\tbuy\t200\tElf Guard honor present\tROW1411"));

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
    void canBuyFromElfArmoryUsingGenericRequest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      SessionLoggerOutput.startStream();

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPath(Path.STANDARD),
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
        assertTrue(
            text.contains(
                "trading 200 piles of Elf Army machine parts for 1 Elf Guard honor present"));
        assertFalse(
            text.contains("Elf Guard Armory\tsell\t3\tElf Guard commandeering gloves\tROW1412"));
        assertFalse(
            text.contains("Elf Guard Armory\tsell\t3\tElf Guard officer's sidearm\tROW1413"));
        assertFalse(text.contains("Elf Guard Armory\tsell\t3\tKelflar vest\tROW1415"));
        assertFalse(text.contains("Elf Guard Armory\tsell\t3\tElf Guard mouthknife\tROW1416"));
        assertFalse(text.contains("Elf Guard Armory\tbuy\t200\tElf Guard honor present\tROW1411"));

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
              withProperty("crimbo23ArmoryControl", "elf"),
              withItem(ItemPool.ELF_GUARD_COMMANDEERING_GLOVES, 14),
              withItem(ItemPool.ELF_ARMY_MACHINE_PARTS, 49));
      try (cleanups) {
        client.addResponse(200, html("request/test_armory_elf_sell.html"));
        client.addResponse(200, "");

        var buy =
            new Crimbo23ElfArmoryRequest(
                false, ItemPool.get(ItemPool.ELF_GUARD_COMMANDEERING_GLOVES, 13));
        buy.run();

        var text = SessionLoggerOutput.stopStream();
        assertTrue(
            text.contains(
                "trading 13 pairs of Elf Guard commandeering gloves for 39 piles of Elf Army machine parts"));
        assertFalse(
            text.contains("Elf Guard Armory\tsell\t3\tElf Guard commandeering gloves\tROW1412"));
        assertFalse(
            text.contains("Elf Guard Armory\tsell\t3\tElf Guard officer's sidearm\tROW1413"));
        assertFalse(text.contains("Elf Guard Armory\tsell\t3\tKelflar vest\tROW1415"));
        assertFalse(text.contains("Elf Guard Armory\tsell\t3\tElf Guard mouthknife\tROW1416"));
        assertFalse(text.contains("Elf Guard Armory\tbuy\t200\tElf Guard honor present\tROW1411"));

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

    @Test
    void canSellToElfArmoryUsingGenericRequest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      SessionLoggerOutput.startStream();

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPath(Path.STANDARD),
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
        assertTrue(
            text.contains(
                "trading 13 pairs of Elf Guard commandeering gloves for 39 piles of Elf Army machine parts"));
        assertFalse(
            text.contains("Elf Guard Armory\tsell\t3\tElf Guard commandeering gloves\tROW1412"));
        assertFalse(
            text.contains("Elf Guard Armory\tsell\t3\tElf Guard officer's sidearm\tROW1413"));
        assertFalse(text.contains("Elf Guard Armory\tsell\t3\tKelflar vest\tROW1415"));
        assertFalse(text.contains("Elf Guard Armory\tsell\t3\tElf Guard mouthknife\tROW1416"));
        assertFalse(text.contains("Elf Guard Armory\tbuy\t200\tElf Guard honor present\tROW1411"));

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
}
