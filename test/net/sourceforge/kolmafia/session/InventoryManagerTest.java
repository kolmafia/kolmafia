package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class InventoryManagerTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("InventoryManagerTest");
  }

  @BeforeEach
  void beforeEach() {
    Preferences.reset("InventoryManagerTest");
  }

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
      assertTrue(InventoryManager.retrieveItem(HOBO_CODE_BINDER));

      // It is now in inventory and not equipped
      assertEquals(1, InventoryManager.getCount(HOBO_CODE_BINDER));
      assertEquals(UNEQUIP, EquipmentManager.getEquipment(EquipmentManager.OFFHAND));

      var requests = builder.client.getRequests();
      assertThat(requests, hasSize(2));
      assertPostRequest(
          requests.get(0), "/inv_equip.php", "which=2&ajax=1&action=unequip&type=offhand");
      assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");

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

  @Nested
  class CrimboTrainingManual {
    @Test
    public void willDetectCrimboTrainingSkillFromItemDescription() {
      var builder = new FakeHttpClientBuilder();

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.CRIMBO_TRAINING_MANUAL),
              withProperty("crimboTrainingSkill", 0));

      try (cleanups) {
        builder.client.addResponse(200, html("request/test_check_crimbo_training_manual.html"));

        InventoryManager.checkCrimboTrainingManual();
        assertThat("crimboTrainingSkill", isSetTo(7));

        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(requests.get(0), "/desc_item.php", "whichitem=990145553");
      }
    }

    @Test
    public void willNotLookAtDescriptionWithValidSkill() {
      var builder = new FakeHttpClientBuilder();

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.CRIMBO_TRAINING_MANUAL),
              withProperty("crimboTrainingSkill", 7));

      try (cleanups) {
        builder.client.addResponse(200, html("request/test_check_crimbo_training_manual.html"));

        InventoryManager.checkCrimboTrainingManual();

        var requests = builder.client.getRequests();

        assertThat(requests, hasSize(0));
      }
    }
  }
}
