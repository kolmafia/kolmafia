package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withHttpClientBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import org.junit.jupiter.api.Test;

public class InventoryManagerTest {

  @Test
  public void willLeaveCheckpointsIntact() {
    var builder = new FakeHttpClientBuilder();

    AdventureResult HOBO_CODE_BINDER = ItemPool.get(ItemPool.HOBO_CODE_BINDER);
    AdventureResult UNEQUIP = EquipmentRequest.UNEQUIP;

    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withEquipped(EquipmentManager.OFFHAND, "hobo code binder"));

    try (cleanups) {
      // The offhand item is equipped as desired
      assertEquals(HOBO_CODE_BINDER, EquipmentManager.getEquipment(EquipmentManager.OFFHAND));

      // InventoryManager says it is not in inventory, but can be placed there
      assertEquals(0, InventoryManager.getCount(HOBO_CODE_BINDER));
      assertEquals(1, InventoryManager.getAccessibleCount(HOBO_CODE_BINDER));

      // Checkpoint our current equipment
      Checkpoint checkpoint = new Checkpoint();
      assertEquals(HOBO_CODE_BINDER, checkpoint.get(EquipmentManager.OFFHAND));

      // Tell the InventoryManager to "retrieve" the item into inventory
      builder.client.addResponse(200, html("request/test_unequip_offhand.html"));
      InventoryManager.retrieveItem(HOBO_CODE_BINDER);

      var requests = builder.client.getRequests();
      assertThat(requests, hasSize(2));
      assertPostRequest(
          requests.get(0), "/inv_equip.php", "which=2&ajax=1&action=unequip&type=offhand");
      assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");

      // It is now in inventory and not equipped
      assertEquals(1, InventoryManager.getCount(HOBO_CODE_BINDER));
      assertEquals(UNEQUIP, EquipmentManager.getEquipment(EquipmentManager.OFFHAND));

      builder.client.clear();
      builder.client.addResponse(200, html("request/test_equip_offhand.html"));
      checkpoint.close();

      requests = builder.client.getRequests();
      assertThat(requests, hasSize(2));
      assertPostRequest(
          requests.get(0),
          "/inv_equip.php",
          "which=2&ajax=1&action=equip&whichitem=" + ItemPool.HOBO_CODE_BINDER);
      assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");

      // It is now equipped and not in inventory
      assertEquals(0, InventoryManager.getCount(HOBO_CODE_BINDER));
      assertEquals(HOBO_CODE_BINDER, EquipmentManager.getEquipment(EquipmentManager.OFFHAND));
    }
  }
}
