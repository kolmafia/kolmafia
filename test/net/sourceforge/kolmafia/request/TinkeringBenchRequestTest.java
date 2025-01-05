package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withIntrinsicEffect;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.helpers.SessionLoggerOutput;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.concoction.CreateItemRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class TinkeringBenchRequestTest {
  @BeforeAll
  public static void init() {
    KoLCharacter.reset("TinkeringBenchRequestTest");
  }

  @BeforeEach
  public void beforeEach() {}

  @Nested
  class CreatableAmount {
    private int creatableAmount(final int itemId) {
      CreateItemRequest item = CreateItemRequest.getInstance(itemId);
      if (item == null) {
        // This happens if the creation method is not permitted
        return 0;
      }
      return item.getQuantityPossible();
    }

    private void printCreatableAmounts() {
      System.out.println(creatableAmount(ItemPool.BIPHASIC_MOLECULAR_OCULUS));
      System.out.println(creatableAmount(ItemPool.TRIPHASIC_MOLECULAR_OCULUS));
      System.out.println(creatableAmount(ItemPool.HIGH_TENSION_EXOSKELETON));
      System.out.println(creatableAmount(ItemPool.ULTRA_HIGH_TENSION_EXOSKELETON));
      System.out.println(creatableAmount(ItemPool.IRRESPONSIBLE_TENSION_EXOSKELETON));
      System.out.println(creatableAmount(ItemPool.QUICK_RELEASE_BELT_POUCH));
      System.out.println(creatableAmount(ItemPool.QUICK_RELEASE_FANNYPACK));
      System.out.println(creatableAmount(ItemPool.QUICK_RELEASE_UTILITY_BELT));
      System.out.println(creatableAmount(ItemPool.MOTION_SENSOR));
      System.out.println(creatableAmount(ItemPool.FOCUSED_MAGNETRON_PISTOL));
    }

    @Test
    void savageBeastCannotCreate() {
      var cleanups =
          new Cleanups(
              withPath(Path.WEREPROFESSOR),
              withIntrinsicEffect(EffectPool.SAVAGE_BEAST),
              withItem(ItemPool.SMASHED_SCIENTIFIC_EQUIPMENT, 10));
      try (cleanups) {
        assertEquals(0, creatableAmount(ItemPool.BIPHASIC_MOLECULAR_OCULUS));
        assertEquals(0, creatableAmount(ItemPool.TRIPHASIC_MOLECULAR_OCULUS));
        assertEquals(0, creatableAmount(ItemPool.HIGH_TENSION_EXOSKELETON));
        assertEquals(0, creatableAmount(ItemPool.ULTRA_HIGH_TENSION_EXOSKELETON));
        assertEquals(0, creatableAmount(ItemPool.IRRESPONSIBLE_TENSION_EXOSKELETON));
        assertEquals(0, creatableAmount(ItemPool.QUICK_RELEASE_BELT_POUCH));
        assertEquals(0, creatableAmount(ItemPool.QUICK_RELEASE_FANNYPACK));
        assertEquals(0, creatableAmount(ItemPool.QUICK_RELEASE_UTILITY_BELT));
        assertEquals(0, creatableAmount(ItemPool.MOTION_SENSOR));
        assertEquals(0, creatableAmount(ItemPool.FOCUSED_MAGNETRON_PISTOL));
      }
    }

    @Test
    void mildManneredProcessorCanCreateOne() {
      var cleanups =
          new Cleanups(
              withPath(Path.WEREPROFESSOR),
              withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR),
              withItem(ItemPool.SMASHED_SCIENTIFIC_EQUIPMENT, 1));
      try (cleanups) {
        assertEquals(1, creatableAmount(ItemPool.BIPHASIC_MOLECULAR_OCULUS));
        assertEquals(0, creatableAmount(ItemPool.TRIPHASIC_MOLECULAR_OCULUS));
        assertEquals(1, creatableAmount(ItemPool.HIGH_TENSION_EXOSKELETON));
        assertEquals(0, creatableAmount(ItemPool.ULTRA_HIGH_TENSION_EXOSKELETON));
        assertEquals(0, creatableAmount(ItemPool.IRRESPONSIBLE_TENSION_EXOSKELETON));
        assertEquals(1, creatableAmount(ItemPool.QUICK_RELEASE_BELT_POUCH));
        assertEquals(0, creatableAmount(ItemPool.QUICK_RELEASE_FANNYPACK));
        assertEquals(0, creatableAmount(ItemPool.QUICK_RELEASE_UTILITY_BELT));
        assertEquals(1, creatableAmount(ItemPool.MOTION_SENSOR));
        assertEquals(1, creatableAmount(ItemPool.FOCUSED_MAGNETRON_PISTOL));
      }
    }

    @Test
    void mildManneredProcessorCanCreateTwo() {
      var cleanups =
          new Cleanups(
              withPath(Path.WEREPROFESSOR),
              withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR),
              withItem(ItemPool.SMASHED_SCIENTIFIC_EQUIPMENT, 2));
      try (cleanups) {
        assertEquals(1, creatableAmount(ItemPool.BIPHASIC_MOLECULAR_OCULUS));
        assertEquals(1, creatableAmount(ItemPool.TRIPHASIC_MOLECULAR_OCULUS));
        assertEquals(1, creatableAmount(ItemPool.HIGH_TENSION_EXOSKELETON));
        assertEquals(1, creatableAmount(ItemPool.ULTRA_HIGH_TENSION_EXOSKELETON));
        assertEquals(0, creatableAmount(ItemPool.IRRESPONSIBLE_TENSION_EXOSKELETON));
        assertEquals(1, creatableAmount(ItemPool.QUICK_RELEASE_BELT_POUCH));
        assertEquals(1, creatableAmount(ItemPool.QUICK_RELEASE_FANNYPACK));
        assertEquals(0, creatableAmount(ItemPool.QUICK_RELEASE_UTILITY_BELT));
        assertEquals(1, creatableAmount(ItemPool.MOTION_SENSOR));
        assertEquals(1, creatableAmount(ItemPool.FOCUSED_MAGNETRON_PISTOL));
      }
    }

    @Test
    void mildManneredProcessorCanCreateThree() {
      var cleanups =
          new Cleanups(
              withPath(Path.WEREPROFESSOR),
              withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR),
              withItem(ItemPool.SMASHED_SCIENTIFIC_EQUIPMENT, 3));
      try (cleanups) {
        assertEquals(1, creatableAmount(ItemPool.BIPHASIC_MOLECULAR_OCULUS));
        assertEquals(1, creatableAmount(ItemPool.TRIPHASIC_MOLECULAR_OCULUS));
        assertEquals(1, creatableAmount(ItemPool.HIGH_TENSION_EXOSKELETON));
        assertEquals(1, creatableAmount(ItemPool.ULTRA_HIGH_TENSION_EXOSKELETON));
        assertEquals(1, creatableAmount(ItemPool.IRRESPONSIBLE_TENSION_EXOSKELETON));
        assertEquals(1, creatableAmount(ItemPool.QUICK_RELEASE_BELT_POUCH));
        assertEquals(1, creatableAmount(ItemPool.QUICK_RELEASE_FANNYPACK));
        assertEquals(1, creatableAmount(ItemPool.QUICK_RELEASE_UTILITY_BELT));
        assertEquals(1, creatableAmount(ItemPool.MOTION_SENSOR));
        assertEquals(1, creatableAmount(ItemPool.FOCUSED_MAGNETRON_PISTOL));
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
        assertEquals(0, creatableAmount(ItemPool.BIPHASIC_MOLECULAR_OCULUS));
        assertEquals(1, creatableAmount(ItemPool.TRIPHASIC_MOLECULAR_OCULUS));
        assertEquals(0, creatableAmount(ItemPool.HIGH_TENSION_EXOSKELETON));
        assertEquals(1, creatableAmount(ItemPool.ULTRA_HIGH_TENSION_EXOSKELETON));
        assertEquals(1, creatableAmount(ItemPool.IRRESPONSIBLE_TENSION_EXOSKELETON));
        assertEquals(0, creatableAmount(ItemPool.QUICK_RELEASE_BELT_POUCH));
        assertEquals(1, creatableAmount(ItemPool.QUICK_RELEASE_FANNYPACK));
        assertEquals(1, creatableAmount(ItemPool.QUICK_RELEASE_UTILITY_BELT));
        assertEquals(0, creatableAmount(ItemPool.MOTION_SENSOR));
        assertEquals(0, creatableAmount(ItemPool.FOCUSED_MAGNETRON_PISTOL));
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
        // printCreatableAmounts();
        assertEquals(0, creatableAmount(ItemPool.BIPHASIC_MOLECULAR_OCULUS));
        assertEquals(1, creatableAmount(ItemPool.TRIPHASIC_MOLECULAR_OCULUS));
        assertEquals(0, creatableAmount(ItemPool.HIGH_TENSION_EXOSKELETON));
        assertEquals(1, creatableAmount(ItemPool.ULTRA_HIGH_TENSION_EXOSKELETON));
        assertEquals(1, creatableAmount(ItemPool.IRRESPONSIBLE_TENSION_EXOSKELETON));
        assertEquals(0, creatableAmount(ItemPool.QUICK_RELEASE_BELT_POUCH));
        assertEquals(1, creatableAmount(ItemPool.QUICK_RELEASE_FANNYPACK));
        assertEquals(1, creatableAmount(ItemPool.QUICK_RELEASE_UTILITY_BELT));
        assertEquals(0, creatableAmount(ItemPool.MOTION_SENSOR));
        assertEquals(0, creatableAmount(ItemPool.FOCUSED_MAGNETRON_PISTOL));
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
            withEquipped(Slot.ACCESSORY2, ItemPool.QUICK_RELEASE_FANNYPACK),
            withItem(ItemPool.QUICK_RELEASE_UTILITY_BELT, 0));

    try (cleanups) {
      builder.client.addResponse(200, html("request/test_remove_fannypack.html"));
      builder.client.addResponse(200, "");
      builder.client.addResponse(200, html("request/test_tinkering_bench_buy.html"));
      builder.client.addResponse(200, "");

      assertThat(InventoryManager.getCount(ItemPool.SMASHED_SCIENTIFIC_EQUIPMENT), is(2));
      assertThat(InventoryManager.getCount(ItemPool.QUICK_RELEASE_FANNYPACK), is(0));
      assertThat(InventoryManager.getEquippedCount(ItemPool.QUICK_RELEASE_FANNYPACK), is(1));
      assertThat(InventoryManager.getCount(ItemPool.QUICK_RELEASE_UTILITY_BELT), is(0));

      var request = CreateItemRequest.getInstance(ItemPool.QUICK_RELEASE_UTILITY_BELT);
      assertNotNull(request);
      request.run();

      var text = SessionLoggerOutput.stopStream();
      assertTrue(text.contains("Trade 1 quick-release fannypack, 1 smashed scientific equipment"));

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
      assertTrue(text.contains("Trade 1 quick-release fannypack, 1 smashed scientific equipment"));

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
