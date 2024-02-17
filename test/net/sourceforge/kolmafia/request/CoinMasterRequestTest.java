package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
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
        assertFalse(text.contains("Elf Guard Armory\tbuy\t1\tElf Army machine parts (3)\tROW1412"));

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
        // System.out.println(text);
        assertTrue(
            text.contains(
                "trading 200 piles of Elf Army machine parts for 1 Elf Guard honor present"));
        /*
        assertFalse(
            text.contains(
                "Elf Guard Armory\tbuy\t1\tElf Army machine parts (3)\tROW1412"));
        */

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
        assertFalse(text.contains("Elf Guard Armory\tbuy\t1\tElf Army machine parts (3)\tROW1412"));

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
        // System.out.println(text);
        assertTrue(
            text.contains(
                "trading 13 pairs of Elf Guard commandeering gloves for 39 piles of Elf Army machine parts"));
        /*
        assertFalse(
            text.contains(
                "Elf Guard Armory\tbuy\t1\tElf Army machine parts (3)\tROW1412"));
        */

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
