package net.sourceforge.kolmafia.request;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NPCPurchaseRequestTest {
  @BeforeEach
  public void beforeEach() {
    HttpClientWrapper.setupFakeClient();
  }

  @Test
  public void canGetPrice() {
    var req = new NPCPurchaseRequest("Bugbear Bakery", "bugbear", 159, 678, 50, 1);
    assertThat(req.getPrice(), equalTo(50));
  }

  @Nested
  class DiscountTrousers {
    @Test
    public void priceDiscountedByTravoltanTrousers() {
      var cleanups = new Cleanups(withEquipped(EquipmentManager.PANTS, "Travoltan trousers"));
      try (cleanups) {
        var req = new NPCPurchaseRequest("Hippy Store (Hippy)", "hippy", 242, 665, 70, 1);
        assertThat(req.getPrice(), equalTo(66));
      }
    }

    @Test
    public void hippyPriceDiscountedBySweatpants() {
      var cleanups = new Cleanups(withEquipped(EquipmentManager.PANTS, "designer sweatpants"));
      try (cleanups) {
        var req = new NPCPurchaseRequest("Hippy Store (Hippy)", "hippy", 242, 665, 70, 1);
        assertThat(req.getPrice(), equalTo(66));
      }
    }

    @Test
    public void giftPriceDiscountedByTravoltan() {
      var cleanups = new Cleanups(withEquipped(EquipmentManager.PANTS, "Travoltan trousers"));
      try (cleanups) {
        var req = new NPCPurchaseRequest("Gift Shop", "town_giftshop.php", 1179, 0, 100, 1);
        assertThat(req.getPrice(), equalTo(95));
      }
    }

    @Test
    public void giftShopUsesTravoltanOverSweatpants() {
      var cleanups =
          new Cleanups(
              withEquipped(EquipmentManager.PANTS, "designer sweatpants"),
              withEquippableItem("Travoltan trousers"));
      try (cleanups) {
        var req = new NPCPurchaseRequest("Gift Shop", "town_giftshop.php", 1179, 0, 100, 1);

        // The price is 100, as designer sweatpants do not have a discount
        assertThat(req.getPrice(), equalTo(100));

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
      var cleanups = new Cleanups(withEquipped(EquipmentManager.PANTS, "designer sweatpants"));
      try (cleanups) {
        var req = new NPCPurchaseRequest("Gift Shop", "town_giftshop.php", 1179, 0, 100, 1);
        assertThat(req.getPrice(), equalTo(100));
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
}
