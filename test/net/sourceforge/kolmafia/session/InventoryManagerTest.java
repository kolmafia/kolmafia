package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withMeat;
import static internal.helpers.Player.withNPCStoreReset;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
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
            withHttpClientBuilder(builder), withEquipped(Slot.OFFHAND, "hobo code binder"));

    try (cleanups) {
      // The offhand item is equipped as desired
      assertEquals(HOBO_CODE_BINDER, EquipmentManager.getEquipment(Slot.OFFHAND));

      // InventoryManager says it is not in inventory, but can be placed there
      assertEquals(0, InventoryManager.getCount(HOBO_CODE_BINDER));
      assertEquals(1, InventoryManager.getAccessibleCount(HOBO_CODE_BINDER));

      // Checkpoint our current equipment
      Checkpoint checkpoint = new Checkpoint();
      assertEquals(HOBO_CODE_BINDER, checkpoint.get(Slot.OFFHAND));

      // Tell the InventoryManager to "retrieve" the item into inventory
      builder.client.addResponse(200, html("request/test_unequip_offhand.html"));
      assertTrue(InventoryManager.retrieveItem(HOBO_CODE_BINDER));

      // It is now in inventory and not equipped
      assertEquals(1, InventoryManager.getCount(HOBO_CODE_BINDER));
      assertEquals(UNEQUIP, EquipmentManager.getEquipment(Slot.OFFHAND));

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
      assertEquals(HOBO_CODE_BINDER, EquipmentManager.getEquipment(Slot.OFFHAND));
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

  @Nested
  class LimitedNPCItems {
    @Test
    public void willRetrieveIfCanUseMall() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withContinuationState(),
              withNPCStoreReset(),
              withInteractivity(true),
              withProperty("autoSatisfyWithMall", true),
              withProperty("autoSatisfyWithNPCs", true),
              withMeat(1000),
              withItem(ItemPool.FEDORA_MOUNTED_FOUNTAIN, 0),
              withProperty("_fireworksShop", true),
              withProperty("_fireworksShopHatBought", false));

      try (cleanups) {
        client.addResponse(200, html("request/test_firework_shop_hat_purchase.html"));
        client.addResponse(200, ""); // api.php

        InventoryManager.retrieveItem(ItemPool.FEDORA_MOUNTED_FOUNTAIN);

        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.CONTINUE));
        assertTrue(InventoryManager.hasItem(ItemPool.FEDORA_MOUNTED_FOUNTAIN));
        assertEquals(1000 - 475, KoLCharacter.getAvailableMeat());
        assertThat("_fireworksShopHatBought", isSetTo("true"));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0),
            "/shop.php",
            "whichshop=fwshop&action=buyitem&whichrow=1247&ajax=1&quantity=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void willRetrieveIfCanUseNPCStore() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withContinuationState(),
              withNPCStoreReset(),
              withInteractivity(false),
              withProperty("autoSatisfyWithMall", false),
              withProperty("autoSatisfyWithNPCs", true),
              withMeat(1000),
              withItem(ItemPool.FEDORA_MOUNTED_FOUNTAIN, 0),
              withProperty("_fireworksShop", true),
              withProperty("_fireworksShopHatBought", false));

      try (cleanups) {
        client.addResponse(200, html("request/test_firework_shop_hat_purchase.html"));
        client.addResponse(200, ""); // api.php

        InventoryManager.retrieveItem(ItemPool.FEDORA_MOUNTED_FOUNTAIN);

        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.CONTINUE));
        assertTrue(InventoryManager.hasItem(ItemPool.FEDORA_MOUNTED_FOUNTAIN));
        assertEquals(1000 - 475, KoLCharacter.getAvailableMeat());
        assertThat("_fireworksShopHatBought", isSetTo("true"));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0),
            "/shop.php",
            "whichshop=fwshop&action=buyitem&whichrow=1247&ajax=1&quantity=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void willNotRetrieveIfCannotUseMallOrNPCStore() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withContinuationState(),
              withNPCStoreReset(),
              withInteractivity(false),
              withProperty("autoSatisfyWithMall", false),
              withProperty("autoSatisfyWithNPCs", false),
              withMeat(1000),
              withItem(ItemPool.FEDORA_MOUNTED_FOUNTAIN, 0),
              withProperty("_fireworksShop", true),
              withProperty("_fireworksShopHatBought", false));

      try (cleanups) {
        client.addResponse(200, html("request/test_firework_shop_hat_purchase.html"));
        client.addResponse(200, ""); // api.php

        InventoryManager.retrieveItem(ItemPool.FEDORA_MOUNTED_FOUNTAIN);

        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.ERROR));
        assertFalse(InventoryManager.hasItem(ItemPool.FEDORA_MOUNTED_FOUNTAIN));
        assertEquals(1000, KoLCharacter.getAvailableMeat());
        assertThat("_fireworksShopHatBought", isSetTo("false"));

        var requests = client.getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void willNotRetrieveIfAlreadyBoughtFireworkHat() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withContinuationState(),
              withNPCStoreReset(),
              withInteractivity(true),
              withProperty("autoSatisfyWithMall", true),
              withProperty("autoSatisfyWithNPCs", true),
              withMeat(1000),
              withItem(ItemPool.FEDORA_MOUNTED_FOUNTAIN, 0),
              withItem(ItemPool.SOMBRERO_MOUNTED_SPARKLER, 1),
              withProperty("_fireworksShop", true),
              withProperty("_fireworksShopHatBought", true));

      try (cleanups) {
        client.addResponse(200, html("request/test_firework_shop_hat_purchase.html"));
        client.addResponse(200, ""); // api.php

        InventoryManager.retrieveItem(ItemPool.FEDORA_MOUNTED_FOUNTAIN);

        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.ERROR));
        assertFalse(InventoryManager.hasItem(ItemPool.FEDORA_MOUNTED_FOUNTAIN));
        assertEquals(1000, KoLCharacter.getAvailableMeat());

        var requests = client.getRequests();
        assertThat(requests, hasSize(0));
      }
    }
  }

  @Nested
  class PerUserVariableItems {
    @Test
    public void RingWillSetModsForUser() {
      var builder = new FakeHttpClientBuilder();
      builder.client.addResponse(200, html("request/test_desc_item_ring_2crs.html"));

      var cleanups = new Cleanups(withHttpClientBuilder(builder), withItem(ItemPool.RING, 1));
      try (cleanups) {
        Modifiers mods = ModifierDatabase.getModifiers(ModifierType.ITEM, ItemPool.RING);
        assertEquals(
            0, mods.getDouble(DoubleModifier.MONSTER_LEVEL), "Value should not be set before desc");

        InventoryManager.checkRing();
        mods = ModifierDatabase.getModifiers(ModifierType.ITEM, ItemPool.RING);

        // Spot checking combination properties, negative property, percent.
        assertEquals(-3.0, mods.getDouble(DoubleModifier.MONSTER_LEVEL), "ML Failure");
        assertEquals(+12.0, mods.getDouble(DoubleModifier.DAMAGE_ABSORPTION), "DA Failure");
        assertEquals(+4.0, mods.getDouble(DoubleModifier.WEAPON_DAMAGE), "Weapon Damage Failure");
        assertEquals(0, mods.getDouble(DoubleModifier.MOX), "No Moxie Change Expected");
        assertEquals(10.0, mods.getDouble(DoubleModifier.MOX_PCT), "No Moxie Change Expected");
      }
    }
  }

  @Nested
  class VampireVintnerWine {
    // From items.txt
    private static String WINE_NAME = "1950 Vampire Vintner wine";
    // From inebriety.txt
    private static String WINE_NOTES = "Unspaded, WINE";

    public Cleanups withWineProperties() {
      ItemDatabase.resetVampireVintnerWine();
      return new Cleanups(
          withProperty("vintnerWineName", ""),
          withProperty("vintnerWineEffect", ""),
          withProperty("vintnerWineLevel", 0),
          withProperty("vintnerWineType", ""),
          new Cleanups(ItemDatabase::resetVampireVintnerWine));
    }

    @Test
    public void willLookAtWineOnStartup() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.VAMPIRE_VINTNER_WINE, 1),
              withWineProperties());
      try (cleanups) {
        client.addResponse(200, html("request/test_desc_item_vampire_vintner_wine.html"));
        client.addResponse(200, html("request/test_desc_effect_wine_hot.html"));
        String expectedNotes =
            "12 turns of Wine-Hot (Hot Damage: +39, Hot Resistance: +11, Maximum HP: +130, Item Drop: +65), WINE";

        assertEquals(WINE_NOTES, ConsumablesDatabase.getNotes(WINE_NAME));
        InventoryManager.checkVampireVintnerWine();

        assertEquals("1966 Paul Blister Merlot", Preferences.getString("vintnerWineName"));
        assertEquals("Wine-Hot", Preferences.getString("vintnerWineEffect"));
        assertEquals(13, Preferences.getInteger("vintnerWineLevel"));
        assertEquals("hot", Preferences.getString("vintnerWineType"));
        assertEquals(expectedNotes, ConsumablesDatabase.getNotes(WINE_NAME));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/desc_item.php", "whichitem=140977937");
        assertPostRequest(
            requests.get(1), "/desc_effect.php", "whicheffect=f562dd161fe50cc22b3ab10f04e1f26a");
      }
    }

    @Test
    public void willLookAtWineOnItemAcquisition() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.VAMPIRE_VINTNER_WINE, 0),
              withWineProperties());
      try (cleanups) {
        client.addResponse(200, html("request/test_desc_item_vampire_vintner_wine.html"));
        client.addResponse(200, html("request/test_desc_effect_wine_hot.html"));
        String expectedNotes =
            "12 turns of Wine-Hot (Hot Damage: +39, Hot Resistance: +11, Maximum HP: +130, Item Drop: +65), WINE";

        assertEquals(WINE_NOTES, ConsumablesDatabase.getNotes(WINE_NAME));
        ResultProcessor.processResult(true, ItemPool.get(ItemPool.VAMPIRE_VINTNER_WINE));

        assertEquals("1966 Paul Blister Merlot", Preferences.getString("vintnerWineName"));
        assertEquals("Wine-Hot", Preferences.getString("vintnerWineEffect"));
        assertEquals(13, Preferences.getInteger("vintnerWineLevel"));
        assertEquals("hot", Preferences.getString("vintnerWineType"));
        assertEquals(expectedNotes, ConsumablesDatabase.getNotes(WINE_NAME));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/desc_item.php", "whichitem=140977937");
        assertPostRequest(
            requests.get(1), "/desc_effect.php", "whicheffect=f562dd161fe50cc22b3ab10f04e1f26a");
      }
    }
  }
}
