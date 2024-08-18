package net.sourceforge.kolmafia.request;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withIntrinsicEffect;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withMeat;
import static internal.helpers.Player.withNPCStoreReset;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withQuestProgress;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.session.MallPriceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NPCPurchaseRequestTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("NPCPurchaseTest");
  }

  @BeforeEach
  public void beforeEach() {
    HttpClientWrapper.setupFakeClient();
  }

  @Test
  public void canGetPrice() {
    var req = new NPCPurchaseRequest("Bugbear Bakery", "bugbear", 159, 678, 50, 1);
    assertThat(req.getPrice(), equalTo(50L));
  }

  @Nested
  class DiscountTrousers {
    @Test
    public void priceDiscountedByTravoltanTrousers() {
      var cleanups = new Cleanups(withEquipped(Slot.PANTS, "Travoltan trousers"));
      try (cleanups) {
        var req = new NPCPurchaseRequest("Hippy Store (Hippy)", "hippy", 242, 665, 70, 1);
        assertThat(req.getPrice(), equalTo(66L));
      }
    }

    @Test
    public void hippyPriceDiscountedBySweatpants() {
      var cleanups = new Cleanups(withEquipped(Slot.PANTS, "designer sweatpants"));
      try (cleanups) {
        var req = new NPCPurchaseRequest("Hippy Store (Hippy)", "hippy", 242, 665, 70, 1);
        assertThat(req.getPrice(), equalTo(66L));
      }
    }

    @Test
    public void giftPriceDiscountedByTravoltan() {
      var cleanups = new Cleanups(withEquipped(Slot.PANTS, "Travoltan trousers"));
      try (cleanups) {
        var req = new NPCPurchaseRequest("Gift Shop", "town_giftshop.php", 1179, 0, 100, 1);
        assertThat(req.getPrice(), equalTo(95L));
      }
    }

    @Test
    public void giftShopUsesTravoltanOverSweatpants() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.PANTS, "designer sweatpants"),
              withEquippableItem("Travoltan trousers"));
      try (cleanups) {
        var req = new NPCPurchaseRequest("Gift Shop", "town_giftshop.php", 1179, 0, 100, 1);

        // The price is 100, as designer sweatpants do not have a discount
        assertThat(req.getPrice(), equalTo(100L));

        // Tell mafia to equip the trousers
        var result = req.ensureProperAttire();
        assertThat(result, equalTo(true));

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/inv_equip.php", "which=2&ajax=1&action=equip&whichitem=1792");
      }
    }

    @Test
    public void giftPriceNotDiscountedBySweatpants() {
      var cleanups = new Cleanups(withEquipped(Slot.PANTS, "designer sweatpants"));
      try (cleanups) {
        var req = new NPCPurchaseRequest("Gift Shop", "town_giftshop.php", 1179, 0, 100, 1);
        assertThat(req.getPrice(), equalTo(100L));
      }
    }

    @Test
    public void equipsTrousersIfNecessary() {
      var cleanups = new Cleanups(withEquippableItem("designer sweatpants"));

      try (cleanups) {
        var req = new NPCPurchaseRequest("Hippy Store (Hippy)", "hippy", 242, 665, 70, 1);
        var result = req.ensureProperAttire();
        assertThat(result, equalTo(true));

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/inv_equip.php", "which=2&ajax=1&action=equip&whichitem=10929");
      }
    }

    @Test
    public void doesntEquipTrousersIfGiftShop() {
      var cleanups = new Cleanups(withEquippableItem("designer sweatpants"));

      try (cleanups) {
        var req = new NPCPurchaseRequest("Gift Shop", "town_giftshop.php", 1179, 0, 100, 1);
        var result = req.ensureProperAttire();
        assertThat(result, equalTo(true));

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void respectsMissingPantsSlot() {
      var cleanups =
          new Cleanups(
              withEquippableItem("designer sweatpants"), withPath(AscensionPath.Path.YOU_ROBOT));

      try (cleanups) {
        var req = new NPCPurchaseRequest("Hippy Store (Hippy)", "hippy", 242, 665, 70, 1);
        var result = req.ensureProperAttire();
        assertThat(result, equalTo(true));

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }
  }

  @ParameterizedTest
  @CsvSource({"false", "true"})
  public void testLimitedQuantityNpcPurchases(boolean canInteract) {
    var cleanups =
        new Cleanups(
            withNPCStoreReset(),
            withInteractivity(canInteract),
            withMeat(50000),
            withItem(ItemPool.ZEPPELIN_TICKET, 0),
            withQuestProgress(QuestDatabase.Quest.MACGUFFIN, "finished"),
            withNextResponse(200, html("request/test_npc_purchase_zeppelin_ticket.html")));

    try (cleanups) {
      // Assert that the character is in the expected interaction state
      assertEquals(canInteract, KoLCharacter.canInteract());

      // Look up the item we desire to purchase
      var ticket = ItemPool.get(ItemPool.ZEPPELIN_TICKET);

      // Create a list of purchases
      var results = MallPriceManager.searchNPCs(ticket);

      // We expect to have a ticket available to purchase
      assertEquals(1, results.size());

      // The purchase we will be using
      var purchase = results.get(0);

      // Assure it is the right item
      assertEquals(ItemPool.ZEPPELIN_TICKET, purchase.getItemId());

      // Assure that we can purchase it
      assertTrue(purchase.canPurchase());
      // Assert there is 1 for sale
      assertEquals(1, purchase.getQuantity());
      // Assert there is a limit of 1
      assertEquals(1, purchase.getLimit());
      // Assert that we do not have a ticket yet
      assertEquals(0, ticket.getCount(KoLConstants.inventory));

      // Purchase the ticket, same method as the buy command uses
      // If there are no more items that can be purchased from a request, it is removed from the
      // results collection
      KoLmafia.makePurchases(results, results.toArray(new PurchaseRequest[0]), 1, false, 0);

      // Assert that we now have a ticket
      assertEquals(1, ticket.getCount(KoLConstants.inventory));

      // Assert that the ticket has been removed from available purchase targets
      assertEquals(0, results.size());
    }
  }

  @Nested
  class WereProfessor {
    @Test
    public void mildManneredProfessorCanUseNPCs() {
      var cleanups =
          new Cleanups(
              withPath(Path.WEREPROFESSOR),
              withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR),
              withMeat(100));
      try (cleanups) {
        var req = NPCStoreDatabase.getPurchaseRequest(ItemPool.CHEWING_GUM);
        assertNotNull(req);
        assertTrue(req.canPurchase());
      }
    }

    @Test
    public void savageBeastCannotUseNPCs() {
      var cleanups =
          new Cleanups(
              withPath(Path.WEREPROFESSOR),
              withIntrinsicEffect(EffectPool.SAVAGE_BEAST),
              withMeat(100));
      try (cleanups) {
        var req = NPCStoreDatabase.getPurchaseRequest(ItemPool.CHEWING_GUM);
        assertNotNull(req);
        assertFalse(req.canPurchase());
      }
    }
  }
}
