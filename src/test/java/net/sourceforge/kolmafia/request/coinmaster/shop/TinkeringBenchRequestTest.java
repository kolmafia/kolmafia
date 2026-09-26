package net.sourceforge.kolmafia.request.coinmaster.shop;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withIntrinsicEffect;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import internal.helpers.Cleanups;
import internal.helpers.SessionLoggerOutput;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class TinkeringBenchRequestTest {
  @BeforeAll
  public static void init() {
    KoLCharacter.reset("TinkeringBenchRequestTest");
    Preferences.reset("TinkeringBenchRequestTest");
  }

  @BeforeEach
  public void beforeEach() {}

  @Nested
  class canMake {
    @Test
    void savageBeastCannotCreate() {
      var cleanups =
          new Cleanups(
              withPath(Path.WEREPROFESSOR),
              withIntrinsicEffect(EffectPool.SAVAGE_BEAST),
              withItem(ItemPool.SMASHED_SCIENTIFIC_EQUIPMENT, 10));
      try (cleanups) {
        assertThat(TinkeringBenchRequest.DATA.isAccessible(), is(false));
      }
    }

    @Test
    void mildManneredProcessorCanCreate() {
      var cleanups =
          new Cleanups(
              withPath(Path.WEREPROFESSOR),
              withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR),
              withItem(ItemPool.SMASHED_SCIENTIFIC_EQUIPMENT, 1));
      try (cleanups) {
        assertThat(TinkeringBenchRequest.DATA.isAccessible(), is(true));
      }
    }

    @Test
    void cannotCreateDuplicateItems() {
      var cleanups =
          new Cleanups(
              withPath(Path.WEREPROFESSOR),
              withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR),
              withItem(ItemPool.BIPHASIC_MOLECULAR_OCULUS),
              withItem(ItemPool.HIGH_TENSION_EXOSKELETON),
              withItem(ItemPool.QUICK_RELEASE_BELT_POUCH),
              withItem(ItemPool.MOTION_SENSOR),
              withItem(ItemPool.FOCUSED_MAGNETRON_PISTOL),
              withItem(ItemPool.SMASHED_SCIENTIFIC_EQUIPMENT, 3));
      try (cleanups) {
        assertThat(TinkeringBenchRequest.canMake(ItemPool.BIPHASIC_MOLECULAR_OCULUS), is(false));
        assertThat(TinkeringBenchRequest.canMake(ItemPool.TRIPHASIC_MOLECULAR_OCULUS), is(true));
        assertThat(TinkeringBenchRequest.canMake(ItemPool.HIGH_TENSION_EXOSKELETON), is(false));
        assertThat(
            TinkeringBenchRequest.canMake(ItemPool.ULTRA_HIGH_TENSION_EXOSKELETON), is(true));
        assertThat(
            TinkeringBenchRequest.canMake(ItemPool.IRRESPONSIBLE_TENSION_EXOSKELETON), is(true));
        assertThat(TinkeringBenchRequest.canMake(ItemPool.QUICK_RELEASE_BELT_POUCH), is(false));
        assertThat(TinkeringBenchRequest.canMake(ItemPool.QUICK_RELEASE_FANNYPACK), is(true));
        assertThat(TinkeringBenchRequest.canMake(ItemPool.QUICK_RELEASE_UTILITY_BELT), is(true));
        assertThat(TinkeringBenchRequest.canMake(ItemPool.MOTION_SENSOR), is(false));
        assertThat(TinkeringBenchRequest.canMake(ItemPool.FOCUSED_MAGNETRON_PISTOL), is(false));
      }
    }

    @Test
    void canUpgradeEquippedItems() {
      var cleanups =
          new Cleanups(
              withPath(Path.WEREPROFESSOR),
              withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR),
              withEquipped(Slot.HAT, ItemPool.BIPHASIC_MOLECULAR_OCULUS),
              withEquipped(Slot.PANTS, ItemPool.HIGH_TENSION_EXOSKELETON),
              withEquipped(Slot.ACCESSORY1, ItemPool.QUICK_RELEASE_BELT_POUCH),
              withEquipped(Slot.ACCESSORY2, ItemPool.MOTION_SENSOR),
              withEquipped(Slot.WEAPON, ItemPool.FOCUSED_MAGNETRON_PISTOL),
              withItem(ItemPool.SMASHED_SCIENTIFIC_EQUIPMENT, 3));
      try (cleanups) {
        assertThat(TinkeringBenchRequest.canMake(ItemPool.BIPHASIC_MOLECULAR_OCULUS), is(false));
        assertThat(TinkeringBenchRequest.canMake(ItemPool.TRIPHASIC_MOLECULAR_OCULUS), is(true));
        assertThat(TinkeringBenchRequest.canMake(ItemPool.HIGH_TENSION_EXOSKELETON), is(false));
        assertThat(
            TinkeringBenchRequest.canMake(ItemPool.ULTRA_HIGH_TENSION_EXOSKELETON), is(true));
        assertThat(
            TinkeringBenchRequest.canMake(ItemPool.IRRESPONSIBLE_TENSION_EXOSKELETON), is(true));
        assertThat(TinkeringBenchRequest.canMake(ItemPool.QUICK_RELEASE_BELT_POUCH), is(false));
        assertThat(TinkeringBenchRequest.canMake(ItemPool.QUICK_RELEASE_FANNYPACK), is(true));
        assertThat(TinkeringBenchRequest.canMake(ItemPool.QUICK_RELEASE_UTILITY_BELT), is(true));
        assertThat(TinkeringBenchRequest.canMake(ItemPool.MOTION_SENSOR), is(false));
        assertThat(TinkeringBenchRequest.canMake(ItemPool.FOCUSED_MAGNETRON_PISTOL), is(false));
      }
    }
  }

  @Test
  void canCreateWithTinkeringBenchRequest() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    SessionLoggerOutput.startStream();

    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withPath(Path.WEREPROFESSOR),
            withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR),
            withItem(ItemPool.SMASHED_SCIENTIFIC_EQUIPMENT, 2),
            withItem(ItemPool.QUICK_RELEASE_FANNYPACK),
            withItem(ItemPool.QUICK_RELEASE_UTILITY_BELT, 0));

    try (cleanups) {
      builder.client.addResponse(200, html("request/test_tinkering_bench_buy.html"));
      builder.client.addResponse(200, "");

      var shopRow = TinkeringBenchRequest.DATA.getShopRow(ItemPool.QUICK_RELEASE_UTILITY_BELT);
      var request = TinkeringBenchRequest.DATA.getRequest(shopRow, 1);
      assertNotNull(request);
      request.run();

      var text = SessionLoggerOutput.stopStream();
      assertThat(
          text, containsString("Trade 1 quick-release fannypack, 1 smashed scientific equipment"));

      assertThat(InventoryManager.getCount(ItemPool.SMASHED_SCIENTIFIC_EQUIPMENT), is(1));
      assertThat(InventoryManager.getCount(ItemPool.QUICK_RELEASE_FANNYPACK), is(0));
      assertThat(InventoryManager.getEquippedCount(ItemPool.QUICK_RELEASE_FANNYPACK), is(0));
      assertThat(InventoryManager.getCount(ItemPool.QUICK_RELEASE_UTILITY_BELT), is(1));

      var requests = client.getRequests();
      assertThat(requests, hasSize(2));

      assertPostRequest(
          requests.get(0),
          "/shop.php",
          "whichshop=wereprofessor_tinker&action=buyitem&whichrow=1474&quantity=1");
      assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
    }
  }

  @Test
  void canCreateWithGenericRequest() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    SessionLoggerOutput.startStream();

    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withPath(Path.WEREPROFESSOR),
            withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR),
            withItem(ItemPool.SMASHED_SCIENTIFIC_EQUIPMENT, 2),
            withEquipped(Slot.ACCESSORY2, ItemPool.QUICK_RELEASE_FANNYPACK),
            withItem(ItemPool.QUICK_RELEASE_UTILITY_BELT, 0));

    try (cleanups) {
      builder.client.addResponse(200, html("request/test_remove_fannypack.html"));
      builder.client.addResponse(200, "");
      builder.client.addResponse(200, html("request/test_tinkering_bench_buy.html"));
      builder.client.addResponse(200, "");

      var remove = new GenericRequest("inv_equip.php?which=2&ajax=1&action=unequip&type=acc2");
      remove.run();

      assertThat(InventoryManager.getCount(ItemPool.SMASHED_SCIENTIFIC_EQUIPMENT), is(2));
      assertThat(InventoryManager.getEquippedCount(ItemPool.QUICK_RELEASE_FANNYPACK), is(0));
      assertThat(InventoryManager.getCount(ItemPool.QUICK_RELEASE_FANNYPACK), is(1));
      assertThat(InventoryManager.getCount(ItemPool.QUICK_RELEASE_UTILITY_BELT), is(0));

      var buy =
          new GenericRequest(
              "shop.php?whichshop=wereprofessor_tinker&action=buyitem&whichrow=1474&quantity=1");
      buy.run();

      var text = SessionLoggerOutput.stopStream();
      assertThat(
          text, containsString("Trade 1 quick-release fannypack, 1 smashed scientific equipment"));

      assertThat(InventoryManager.getCount(ItemPool.SMASHED_SCIENTIFIC_EQUIPMENT), is(1));
      assertThat(InventoryManager.getCount(ItemPool.QUICK_RELEASE_FANNYPACK), is(0));
      assertThat(InventoryManager.getEquippedCount(ItemPool.QUICK_RELEASE_FANNYPACK), is(0));
      assertThat(InventoryManager.getCount(ItemPool.QUICK_RELEASE_UTILITY_BELT), is(1));

      var requests = client.getRequests();
      assertThat(requests, hasSize(4));

      assertPostRequest(
          requests.get(0), "/inv_equip.php", "which=2&ajax=1&action=unequip&type=acc2");
      assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      assertPostRequest(
          requests.get(2),
          "/shop.php",
          "whichshop=wereprofessor_tinker&action=buyitem&whichrow=1474&quantity=1");
      assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
    }
  }
}
