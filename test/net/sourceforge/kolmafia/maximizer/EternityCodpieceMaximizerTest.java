package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.getBoosts;
import static internal.helpers.Maximizer.maximize;
import static internal.helpers.Maximizer.maximizeAny;
import static internal.helpers.Maximizer.modFor;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withHardcore;
import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withItemInCloset;
import static internal.helpers.Player.withItemInStorage;
import static internal.helpers.Player.withMallPrice;
import static internal.helpers.Player.withMeat;
import static internal.helpers.Player.withNotAllowedInStandard;
import static internal.helpers.Player.withOverrideModifiers;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withRestricted;
import static internal.helpers.Player.withStats;
import static internal.matchers.Maximizer.recommends;
import static internal.matchers.Maximizer.recommendsSlot;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class EternityCodpieceMaximizerTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("EternityCodpieceMaximizerTest");
    Preferences.reset("EternityCodpieceMaximizerTest");
  }

  private static boolean maximizeExhaustively(String expression) {
    CodpiecePruning.forceExhaustiveForTests = true;
    try {
      return maximize(expression + ", 0.000001 enchantment count");
    } finally {
      CodpiecePruning.forceExhaustiveForTests = false;
    }
  }

  @Test
  void considersCodpieceBaseEnchantmentsAsAnAccessory() {
    var cleanups =
        new Cleanups(
            withEquippableItem("consolation ribbon", 3),
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE));

    try (cleanups) {
      assertTrue(maximize("mus, mys, mox, -tie"));
      assertThat(
          SlotSet.ACCESSORY_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .filter(item -> item.getName().equals("consolation ribbon"))
              .count(),
          equalTo(2L));
      assertTrue(
          SlotSet.ACCESSORY_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .anyMatch(item -> item.getItemId() == ItemPool.THE_ETERNITY_CODPIECE));
      assertTrue(
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .allMatch(EquipmentRequest.UNEQUIP::equals));
    }
  }

  @Test
  void prefersHeartstoneAsAccessoryForHpRegen() {
    var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withEquippableItem(ItemPool.HEARTSTONE));

    try (cleanups) {
      assertTrue(maximize("+equip heartstone, hp regen, -tie"));
      assertTrue(
          SlotSet.ACCESSORY_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .anyMatch(item -> item.getItemId() == ItemPool.HEARTSTONE));
      assertTrue(
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .noneMatch(item -> item.getItemId() == ItemPool.HEARTSTONE));
    }
  }

  @Test
  void removesSlottedHeartstoneWhenAccessoryScoresBetter() {
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.CODPIECE1, ItemPool.HEARTSTONE));

    try (cleanups) {
      assertTrue(maximize("hp regen, -tie"));
      assertTrue(
          SlotSet.ACCESSORY_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .anyMatch(item -> item.getItemId() == ItemPool.HEARTSTONE));
      assertTrue(
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .noneMatch(item -> item.getItemId() == ItemPool.HEARTSTONE));
    }
  }

  @Test
  void usesAdditionalSingleEquipGemCopiesInCodpieceSlots() {
    var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withEquippableItem(ItemPool.HEARTSTONE, 2));

    try (cleanups) {
      assertTrue(maximize("hp regen, familiar weight, -tie"));
      assertThat(
          SlotSet.ACCESSORY_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .filter(item -> item.getItemId() == ItemPool.HEARTSTONE)
              .count(),
          equalTo(1L));
      assertThat(
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .filter(item -> item.getItemId() == ItemPool.HEARTSTONE)
              .count(),
          equalTo(1L));
    }
  }

  @Test
  void accountsForFamiliarEffectsFromCodpieceGems() {
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY, 1),
            withItem(ItemPool.HEARTSTONE),
            withItem("control crystal"));

    try (cleanups) {
      assertTrue(
          maximize(
              "item, mys, +equip heartstone, +equip control crystal, -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, -acc1, -acc2, -acc3, -codpiece3, -codpiece4, -codpiece5, -tie"));
      assertTrue(
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .anyMatch(item -> item.getItemId() == ItemPool.HEARTSTONE));
      assertTrue(modFor(DoubleModifier.ITEMDROP) > 10.0);
      assertThat(modFor(DoubleModifier.MYS), equalTo(10.0));
    }
  }

  @ParameterizedTest
  @CsvSource({"0, 5", "2, 3"})
  void considersMultipleMallBuyableCodpieceGems(int owned, long expectedAcquisitions) {
    var onyx = ItemPool.get("unearthly onyx", 1);
    var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withItem(onyx.getItemId(), owned),
            withInteractivity(true),
            withProperty("autoSatisfyWithMall", true),
            withMeat(100_000));

    try (cleanups) {
      maximizeAny("spooky resistance, -tie");

      assertThat(
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .filter(onyx::equals)
              .count(),
          equalTo(5L));
      assertThat(
          getBoosts().stream()
              .filter(boost -> onyx.equals(boost.getItem()))
              .filter(boost -> boost.toString().startsWith("acquire & equip codpiece"))
              .count(),
          equalTo(expectedAcquisitions));
    }
  }

  @Test
  void buysMissingAdventureGemsAndReportsTheirScore() {
    var pearl = ItemPool.get("unblemished pearl", 1);
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, "grandfather watch"),
            withEquipped(Slot.ACCESSORY2, "Boots of Twilight Whispers"),
            withEquipped(Slot.ACCESSORY3, "Elf Guard insignia (general)"),
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withItem(pearl.getItemId(), 4),
            withInteractivity(true),
            withProperty("autoSatisfyWithMall", true),
            withMallPrice(pearl.getItemId(), 1_000),
            withMeat(100_000));

    try (cleanups) {
      assertTrue(
          Maximizer.maximize(
              "adventures, -tie",
              1_001,
              PriceLevel.BUYABLE_ONLY,
              EquipScope.SPECULATE_ANY,
              EnumSet.allOf(KoLConstants.filterType.class)));

      var codpieceBoost =
          getBoosts().stream()
              .filter(boost -> boost.getItem().getItemId() == ItemPool.THE_ETERNITY_CODPIECE)
              .findFirst()
              .orElseThrow();
      assertThat(codpieceBoost.getBoost(), equalTo(-3.0));
      assertThat(
          SlotSet.ACCESSORY_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .noneMatch(item -> item.getName().equals("Boots of Twilight Whispers")),
          is(true));
      assertThat(
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .filter(pearl::equals)
              .count(),
          equalTo(5L));

      var pearlBoosts =
          getBoosts().stream().filter(boost -> pearl.equals(boost.getItem())).toList();
      assertThat(pearlBoosts.size(), equalTo(5));
      assertThat(
          pearlBoosts.stream()
              .filter(boost -> boost.toString().startsWith("acquire & equip codpiece"))
              .count(),
          equalTo(1L));
      assertThat(
          pearlBoosts.stream()
              .filter(boost -> boost.toString().startsWith("acquire & equip codpiece"))
              .findFirst()
              .orElseThrow()
              .toString()
              .contains("1,000 meat"),
          is(true));
      assertThat(
          pearlBoosts.stream()
              .filter(boost -> !boost.toString().startsWith("acquire & equip codpiece"))
              .noneMatch(boost -> boost.toString().contains("meat")),
          is(true));
      assertThat(
          pearlBoosts.stream().map(Boost::getBoost).toList(),
          equalTo(java.util.List.of(1.0, 1.0, 1.0, 1.0, 1.0)));
    }
  }

  @Test
  void doesNotRecommendWorseAccessoriesAfterEquippingCodpieceGems() {
    var watch = ItemPool.get("grandfather watch");
    var boots = ItemPool.get("Boots of Twilight Whispers");
    var pearl = ItemPool.get("unblemished pearl");
    var cleanups =
        new Cleanups(
            withItem(boots),
            withEquipped(Slot.ACCESSORY1, "Elf Guard insignia (general)"),
            withEquipped(Slot.ACCESSORY2, watch),
            withEquipped(Slot.ACCESSORY3, ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.FAMILIAR, "solid shifting time weirdness"),
            withEquipped(Slot.CODPIECE1, pearl),
            withEquipped(Slot.CODPIECE2, pearl),
            withEquipped(Slot.CODPIECE3, pearl),
            withProperty("maximizerCombinationLimit", 1));

    try (cleanups) {
      var currentScore = new Evaluator("adv").getScore(KoLCharacter.getCurrentModifiers());

      assertTrue(maximize("adv"));
      assertThat(Maximizer.best.getScore(), greaterThanOrEqualTo(currentScore));
      assertThat(
          SlotSet.ACCESSORY_SLOTS.stream().map(Maximizer.best.equipment::get).toList(),
          hasItem(watch));
    }
  }

  @Test
  void keepsGrandfatherWatchWhenOtherUpgradesMaskWorseBoots() {
    var watch = ItemPool.get("grandfather watch");
    var boots = ItemPool.get("Boots of Twilight Whispers");
    var ninjammies = ItemPool.get("ninjammies");
    var pearl = ItemPool.get("unblemished pearl");
    var cleanups =
        new Cleanups(
            withStats(1_000, 1_000, 1_000),
            withEquipped(Slot.HAT, "time cop top hat"),
            withEquipped(Slot.WEAPON, "sword behind inappropriate prepositions"),
            withEquipped(Slot.CONTAINER, "Allied Radio Backpack"),
            withEquipped(Slot.SHIRT, "shoe ad T-shirt"),
            withEquipped(Slot.PANTS, "sea chaps"),
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.ACCESSORY2, watch),
            withEquipped(Slot.ACCESSORY3, "Elf Guard insignia (general)"),
            withEquipped(Slot.CODPIECE1, pearl),
            withEquipped(Slot.CODPIECE2, pearl),
            withEquipped(Slot.CODPIECE3, pearl),
            withItem(boots),
            withInteractivity(true),
            withProperty("autoSatisfyWithMall", true),
            withMallPrice(ninjammies.getItemId(), 840_664),
            withMallPrice(pearl.getItemId(), 78_000),
            withMeat(16_000_000),
            withRestricted(false));

    try (cleanups) {
      var currentScore = new Evaluator("adv").getScore(KoLCharacter.getCurrentModifiers());

      assertTrue(
          Maximizer.maximize(
              "adv",
              1_000_000,
              PriceLevel.BUYABLE_ONLY,
              EquipScope.SPECULATE_ANY,
              EnumSet.allOf(KoLConstants.filterType.class)));

      assertThat(Maximizer.best.getScore(), greaterThan(currentScore));
      assertThat(
          SlotSet.ACCESSORY_SLOTS.stream().map(Maximizer.best.equipment::get).toList(),
          hasItem(watch));
      assertThat(
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .filter(pearl::equals)
              .count(),
          equalTo(5L));
      assertThat(getBoosts().stream().noneMatch(boost -> boots.equals(boost.getItem())), is(true));
    }
  }

  @Test
  void pricesAdventureGemsBoughtAfterEquippedCopies() {
    var pearl = ItemPool.get("unblemished pearl");
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.CODPIECE1, pearl),
            withEquipped(Slot.CODPIECE2, pearl),
            withEquipped(Slot.CODPIECE3, pearl),
            withInteractivity(true),
            withProperty("autoSatisfyWithMall", true),
            withMallPrice(pearl.getItemId(), 1_000),
            withMeat(100_000));

    try (cleanups) {
      assertTrue(
          Maximizer.maximize(
              "adv, -tie",
              10_000,
              PriceLevel.BUYABLE_ONLY,
              EquipScope.SPECULATE_ANY,
              EnumSet.allOf(KoLConstants.filterType.class)));

      assertThat(
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .filter(pearl::equals)
              .count(),
          equalTo(5L));
      var boughtPearls =
          getBoosts().stream()
              .filter(boost -> pearl.equals(boost.getItem()))
              .filter(boost -> boost.toString().startsWith("acquire & equip codpiece"))
              .toList();
      assertThat(boughtPearls.size(), equalTo(2));
      assertThat(
          boughtPearls.stream().allMatch(boost -> boost.toString().contains("1,000 meat")),
          is(true));
    }
  }

  @Test
  void limitsMallCopiesForSingleEquipCodpieceGems() {
    var eye = ItemPool.get("crystalline seal eye", 1);
    var cleanups =
        new Cleanups(
            withInteractivity(true), withProperty("autoSatisfyWithMall", true), withMeat(100_000));

    try (cleanups) {
      var checked =
          new CheckedItem(eye.getItemId(), EquipScope.SPECULATE_ANY, 0, PriceLevel.DONT_CHECK);

      assertThat(checked.mallBuyable, equalTo(6));
    }
  }

  @Test
  void considersPullableCodpieceGems() {
    var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withItemInStorage(ItemPool.ALIEN_GEMSTONE),
            withInteractivity(false));

    try (cleanups) {
      maximizeAny("mus exp, -tie");

      var gemBoost =
          getBoosts().stream()
              .filter(boost -> boost.getItem() != null)
              .filter(boost -> boost.getItem().getItemId() == ItemPool.ALIEN_GEMSTONE)
              .findFirst()
              .orElseThrow();
      assertThat(gemBoost.getCmd(), startsWith("pull"));
    }
  }

  @Test
  void unclosetsAndEquipsCodpieceBeforeInsertingGem() {
    var cleanups =
        new Cleanups(
            withItemInCloset(ItemPool.THE_ETERNITY_CODPIECE),
            withProperty("autoSatisfyWithCloset", true),
            withItem(ItemPool.ALIEN_GEMSTONE));

    try (cleanups) {
      assertTrue(maximize("mus exp, -tie"));

      var boosts = getBoosts();
      var codpieceBoost =
          boosts.stream()
              .filter(boost -> boost.getItem() != null)
              .filter(boost -> boost.getItem().getItemId() == ItemPool.THE_ETERNITY_CODPIECE)
              .findFirst()
              .orElseThrow();
      var gemBoost =
          boosts.stream()
              .filter(boost -> boost.getItem() != null)
              .filter(boost -> boost.getItem().getItemId() == ItemPool.ALIEN_GEMSTONE)
              .findFirst()
              .orElseThrow();
      assertThat(codpieceBoost.getCmd(), startsWith("closet take"));
      assertTrue(boosts.indexOf(codpieceBoost) < boosts.indexOf(gemBoost));
    }
  }

  @Test
  void pullsAndEquipsCodpieceBeforeInsertingGem() {
    var cleanups =
        new Cleanups(
            withItemInStorage(ItemPool.THE_ETERNITY_CODPIECE),
            withItem(ItemPool.ALIEN_GEMSTONE),
            withInteractivity(false));

    try (cleanups) {
      maximizeAny("mus exp, -tie");

      var boosts = getBoosts();
      var codpieceBoost =
          boosts.stream()
              .filter(boost -> boost.getItem() != null)
              .filter(boost -> boost.getItem().getItemId() == ItemPool.THE_ETERNITY_CODPIECE)
              .findFirst()
              .orElseThrow();
      var gemBoost =
          boosts.stream()
              .filter(boost -> boost.getItem() != null)
              .filter(boost -> boost.getItem().getItemId() == ItemPool.ALIEN_GEMSTONE)
              .findFirst()
              .orElseThrow();
      assertThat(codpieceBoost.getCmd(), startsWith("pull"));
      assertTrue(boosts.indexOf(codpieceBoost) < boosts.indexOf(gemBoost));
    }
  }

  @Test
  void prefersBaseballDiamondAsOffhandForWeaponDamage() {
    var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withEquippableItem(ItemPool.BASEBALL_DIAMOND));

    try (cleanups) {
      assertTrue(maximize("+equip baseball diamond, weapon damage, -tie"));
      assertThat(
          Maximizer.best.equipment.get(Slot.OFFHAND).getItemId(),
          equalTo(ItemPool.BASEBALL_DIAMOND));
      assertTrue(
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .noneMatch(item -> item.getItemId() == ItemPool.BASEBALL_DIAMOND));
    }
  }

  @Test
  void movesSlottedBaseballDiamondToOffhandWhenCodpieceForbidden() {
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.CODPIECE1, ItemPool.BASEBALL_DIAMOND));

    try (cleanups) {
      assertTrue(maximize("+equip baseball diamond, -equip the eternity codpiece, -tie"));
      assertThat(
          Maximizer.best.equipment.get(Slot.OFFHAND).getItemId(),
          equalTo(ItemPool.BASEBALL_DIAMOND));
      assertTrue(
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .noneMatch(item -> item.getItemId() == ItemPool.BASEBALL_DIAMOND));
    }
  }

  @Test
  void removesForbiddenHeartstoneFromActiveCodpiece() {
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.CODPIECE1, ItemPool.HEARTSTONE));

    try (cleanups) {
      assertTrue(maximize("-equip heartstone, -tie"));
      boolean wearingCodpiece =
          SlotSet.ACCESSORY_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .anyMatch(item -> item.getItemId() == ItemPool.THE_ETERNITY_CODPIECE);
      boolean heartstoneSlotted =
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .anyMatch(item -> item.getItemId() == ItemPool.HEARTSTONE);
      assertFalse(wearingCodpiece && heartstoneSlotted);
    }
  }

  @Test
  void movesSlottedHeartstoneToAccessoryWhenCodpieceForbidden() {
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.CODPIECE1, ItemPool.HEARTSTONE));

    try (cleanups) {
      assertTrue(maximize("+equip heartstone, -equip the eternity codpiece, -tie"));
      assertTrue(
          SlotSet.ACCESSORY_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .anyMatch(item -> item.getItemId() == ItemPool.HEARTSTONE));
      assertTrue(
          SlotSet.ACCESSORY_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .noneMatch(item -> item.getItemId() == ItemPool.THE_ETERNITY_CODPIECE));
      assertTrue(
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .noneMatch(item -> item.getItemId() == ItemPool.HEARTSTONE));
    }
  }

  @Test
  void honorsForcedCodpieceAndGemSlots() {
    var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withEquippableItem(ItemPool.HEARTSTONE));

    try (cleanups) {
      assertTrue(
          maximize(
              "+equip heartstone, +equip the eternity codpiece, -acc1, -acc2, -codpiece1, -codpiece2, -codpiece3, -codpiece4, -tie"));
      assertThat(
          Maximizer.best.equipment.get(Slot.ACCESSORY3).getItemId(),
          equalTo(ItemPool.THE_ETERNITY_CODPIECE));
      assertThat(
          Maximizer.best.equipment.get(Slot.CODPIECE5).getItemId(), equalTo(ItemPool.HEARTSTONE));
    }
  }

  @Test
  void considersCodpieceWhenForcedGemsFillTheAccessoryPool() {
    int bloodCubicZirconia = ItemPool.get("blood cubic zirconia").getItemId();
    var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withEquippableItem(ItemPool.HEARTSTONE),
            withEquippableItem(ItemPool.PERIDOT_OF_PERIL),
            withEquippableItem(bloodCubicZirconia));

    try (cleanups) {
      assertTrue(maximize("+equip heartstone, +equip peridot, +equip blood cubic, -acc1, -tie"));
      assertTrue(
          SlotSet.ACCESSORY_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .anyMatch(item -> item.getItemId() == ItemPool.THE_ETERNITY_CODPIECE));
      assertTrue(
          SlotSet.ALL_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .anyMatch(item -> item.getItemId() == ItemPool.HEARTSTONE));
      assertTrue(
          SlotSet.ALL_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .anyMatch(item -> item.getItemId() == ItemPool.PERIDOT_OF_PERIL));
      assertTrue(
          SlotSet.ALL_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .anyMatch(item -> item.getItemId() == bloodCubicZirconia));
    }
  }

  @Test
  void considersCodpieceForNonAccessoryTypedGemWithOffhandForbidden() {
    int bloodCubicZirconia = ItemPool.get("blood cubic zirconia").getItemId();
    var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withEquippableItem(ItemPool.BASEBALL_DIAMOND),
            withEquippableItem(ItemPool.HEARTSTONE),
            withEquippableItem(ItemPool.PERIDOT_OF_PERIL),
            withEquippableItem(bloodCubicZirconia));

    try (cleanups) {
      // Baseball is an offhand, not accessory. We're forcing it to rely on the codpiece to be
      // equipped
      assertTrue(
          maximize(
              "+equip baseball diamond, +equip heartstone, +equip peridot, +equip blood cubic, -acc1, -offhand, -tie"));
      assertTrue(
          SlotSet.ACCESSORY_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .anyMatch(item -> item.getItemId() == ItemPool.THE_ETERNITY_CODPIECE));
      assertTrue(
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .anyMatch(item -> item.getItemId() == ItemPool.BASEBALL_DIAMOND));
    }
  }

  @Test
  void doesNotOverflowAccessoryShortlistWhenSlotIsUnequipped() {
    var cleanups =
        new Cleanups(
            withItem(ItemPool.THE_ETERNITY_CODPIECE),
            withItem(ItemPool.PERIDOT_OF_PERIL),
            withItem("spring shoes"),
            withItem("Portable Laughing Stock"),
            withItem(ItemPool.HEARTSTONE));

    try (cleanups) {
      assertTrue(maximize("mana cost -tie"));
      assertTrue(Maximizer.bestChecked < 100);
    }
  }

  @Test
  void picksOptimalMixOfCodpieceGemsAcrossDifferentCappedStats() {
    int massiveGemstone = ItemPool.get("massive gemstone").getItemId();
    int healingCrystal = ItemPool.get("New Age healing crystal").getItemId();
    var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withItem(massiveGemstone, 3),
            withItem(healingCrystal, 2));

    try (cleanups) {
      // 125 caps Item Drop only with all 3 gemstones; 15 caps HP Regen Min only with both
      // crystals. Either gem alone caps one stat and leaves the other unbuffed.
      assertTrue(maximize("0.1 item drop 125 max, 0.1 hp regen min 15 max, -tie"));
      long gemstonesUsed =
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .filter(item -> item.getItemId() == massiveGemstone)
              .count();
      long healingCrystalsUsed =
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .filter(item -> item.getItemId() == healingCrystal)
              .count();
      assertEquals(3, gemstonesUsed);
      assertEquals(2, healingCrystalsUsed);
    }
  }

  @Test
  void cappedStatsCanMakeGreedyCodpieceSelectionSuboptimal() {
    var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withItem("18-picohertz resonator crystal"),
            withItem("shard of double-ice"),
            withItem("kumquartz"));

    try (cleanups) {
      // The resonator scores 10 by itself, versus 9 for either specialized gem. Greedy selects
      // it first and reaches only 14, while the two specialized gems reach both caps for 18.
      assertTrue(
          maximize("cold damage 9 max, cold spell damage 9 max, codpiece1, codpiece2, -tie"));
      assertThat(Maximizer.best.getScore(), equalTo(18.0));
    }
  }

  @Test
  void individuallyBestCodpieceShortlistCanExcludeOptimalCombination() {
    var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withItem("rainbow pearl"),
            withItem("Rubee&trade;"),
            withItem("shard of double-ice"),
            withItem("Lapis Lazuli"),
            withItem("shadow glass"),
            withItem("Azurite"));

    try (cleanups) {
      // Rainbow pearl scores 25 by itself, while each specialized gem scores 9. Keeping the
      // pearl and only four specialists reaches 41; all five specialists reach 45.
      assertTrue(
          maximize(
              "hot damage 9 max, cold damage 9 max, stench damage 9 max, spooky damage 9 max, sleaze damage 9 max, -tie"));
      assertThat(Maximizer.best.getScore(), equalTo(45.0));
    }
  }

  @Test
  void placesRequiredDualUseGemWhereItScoresBestNotJustToSatisfyTheRequirement() {
    // Meat Drop is worth +60 as a plain accessory but only +30 as a codpiece gem, so a
    // required copy should land in accessory rather than being forced into the codpiece.
    int denseMeatGem = ItemPool.get("incredibly dense meat gem").getItemId();
    int massiveGemstone = ItemPool.get("massive gemstone").getItemId();
    var cleanups =
        new Cleanups(
            withItem(denseMeatGem, 2),
            withItem(massiveGemstone, 3),
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE));

    try (cleanups) {
      assertTrue(maximize("+equip incredibly dense meat gem, 0.1 meat drop, 0.1 item drop, -tie"));
      long meatGemsInAccessory =
          SlotSet.ACCESSORY_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .filter(item -> item.getItemId() == denseMeatGem)
              .count();
      long meatGemsInCodpiece =
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .filter(item -> item.getItemId() == denseMeatGem)
              .count();
      long gemstonesInCodpiece =
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .filter(item -> item.getItemId() == massiveGemstone)
              .count();
      assertEquals(2, meatGemsInAccessory);
      assertEquals(0, meatGemsInCodpiece);
      assertEquals(3, gemstonesInCodpiece);
    }
  }

  @ParameterizedTest
  @CsvSource({"hp regen, true", "familiar weight, false"})
  void placesSkillGrantingRequiredGemWhereItScoresBest(
      String expression, boolean heartstoneInAccessory) {
    var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.HEARTSTONE),
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE));

    try (cleanups) {
      assertTrue(maximize("+equip heartstone, " + expression + ", -tie"));
      assertThat(
          SlotSet.ACCESSORY_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .anyMatch(item -> item.getItemId() == ItemPool.HEARTSTONE),
          equalTo(heartstoneInAccessory));
      assertThat(
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .anyMatch(item -> item.getItemId() == ItemPool.HEARTSTONE),
          equalTo(!heartstoneInAccessory));
    }
  }

  @Test
  void failsWhenForcedCodpieceGemHasNoAvailableSlot() {
    var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withEquippableItem(ItemPool.HEARTSTONE));

    try (cleanups) {
      assertFalse(
          maximize(
              "+equip heartstone, +equip the eternity codpiece, -acc1, -acc2, -codpiece1, -codpiece2, -codpiece3, -codpiece4, -codpiece5, -tie"));
    }
  }

  @Test
  void prunesCodpieceConfigurationsThatCannotContainRequiredGems() {
    int bloodCubicZirconia = ItemPool.get("blood cubic zirconia").getItemId();
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withItem(ItemPool.HEARTSTONE),
            withItem(ItemPool.PERIDOT_OF_PERIL),
            withItem(bloodCubicZirconia),
            withItem(ItemPool.ALIEN_GEMSTONE),
            withItem("autumn years wisdom"));

    try (cleanups) {
      assertTrue(
          maximize(
              "+equip heartstone, +equip peridot, +equip blood cubic, -acc1, -acc2, -acc3, -tie"));
      assertTrue(Maximizer.bestChecked < 10);
      for (int itemId :
          new int[] {ItemPool.HEARTSTONE, ItemPool.PERIDOT_OF_PERIL, bloodCubicZirconia}) {
        assertTrue(
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .anyMatch(item -> item.getItemId() == itemId));
      }
    }
  }

  @Test
  void failsWhenCodpieceModifierIsRequiredButCodpieceIsForbidden() {
    var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE), withItem(ItemPool.ALIEN_GEMSTONE));

    try (cleanups) {
      assertFalse(maximize("mus exp 1 min, -equip the eternity codpiece, -tie"));
    }
  }

  @ParameterizedTest
  @CsvSource({"1, false", "3, false", "4, false", "6, true"})
  void wearsCodpieceOnlyWhenDenseMeatGemsMakeItOptimal(int copies, boolean expectedCodpiece) {
    var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withEquippableItem("incredibly dense meat gem", copies));

    try (cleanups) {
      assertTrue(maximize("meat, -tie"));
      assertThat(
          SlotSet.ACCESSORY_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .anyMatch(item -> item.getItemId() == ItemPool.THE_ETERNITY_CODPIECE),
          equalTo(expectedCodpiece));
      long used =
          SlotSet.ACCESSORY_SLOTS.stream()
                  .map(Maximizer.best.equipment::get)
                  .filter(item -> item.getName().equals("incredibly dense meat gem"))
                  .count()
              + SlotSet.CODPIECE_SLOTS.stream()
                  .map(Maximizer.best.equipment::get)
                  .filter(item -> item.getName().equals("incredibly dense meat gem"))
                  .count();
      assertTrue(used <= copies);
    }
  }

  @Test
  void slotsForcedPeridotWhenThatLeavesMoreMeatGemAccessories() {
    var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withEquippableItem("incredibly dense meat gem", 3),
            withEquippableItem(ItemPool.PERIDOT_OF_PERIL));

    try (cleanups) {
      assertTrue(maximize("+equip peridot of peril, meat, -tie"));
      assertTrue(
          SlotSet.ACCESSORY_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .anyMatch(item -> item.getItemId() == ItemPool.THE_ETERNITY_CODPIECE));
      assertTrue(
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .anyMatch(item -> item.getItemId() == ItemPool.PERIDOT_OF_PERIL));
    }
  }

  @Test
  void preservesGemsInExcludedCodpieceSlots() {
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.CODPIECE1, ItemPool.ALIEN_GEMSTONE),
            withItem("autumn years wisdom"));

    try (cleanups) {
      assertTrue(maximize("mus exp, mys exp, -codpiece1, -tie"));
      assertThat(
          Maximizer.best.equipment.get(Slot.CODPIECE1).getItemId(),
          equalTo(ItemPool.ALIEN_GEMSTONE));
      assertTrue(
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .anyMatch(item -> item.getName().equals("autumn years wisdom")));
    }
  }

  @Test
  void replacesShortlistedGemWhoseOnlyCopyIsUsedAsAccessory() {
    var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withEquippableItem("incredibly dense meat gem"),
            withItem(ItemPool.ALIEN_GEMSTONE));

    try (cleanups) {
      assertTrue(
          maximize(
              "+equip incredibly dense meat gem, +equip the eternity codpiece, meat, mus exp, -codpiece2, -codpiece3, -codpiece4, -codpiece5, -tie"));
      assertThat(
          Maximizer.best.equipment.get(Slot.CODPIECE1).getItemId(),
          equalTo(ItemPool.ALIEN_GEMSTONE));
    }
  }

  @Test
  void doesNotReuseGemLockedInExcludedCodpieceSlot() {
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.CODPIECE1, ItemPool.HEARTSTONE));

    try (cleanups) {
      assertTrue(maximize("+equip heartstone, hp regen, -codpiece1, -tie"));
      assertTrue(
          SlotSet.ACCESSORY_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .noneMatch(item -> item.getItemId() == ItemPool.HEARTSTONE));
      assertThat(
          Maximizer.best.equipment.get(Slot.CODPIECE1).getItemId(), equalTo(ItemPool.HEARTSTONE));
    }
  }

  @Test
  void failsWhenRequiredGemCannotBeReleasedFromExcludedSlot() {
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.CODPIECE1, ItemPool.HEARTSTONE));

    try (cleanups) {
      assertFalse(maximize("+equip heartstone, -equip the eternity codpiece, -codpiece1, -tie"));
    }
  }

  @Test
  void considersCodpieceGemConfigurations() {
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withItem(ItemPool.ALIEN_GEMSTONE),
            withItem("autumn years wisdom"));

    try (cleanups) {
      assertTrue(maximize("mus exp, mys exp, -tie"));
      assertThat(getBoosts(), hasItem(recommends("alien gemstone")));
      assertThat(getBoosts(), hasItem(recommends("autumn years wisdom")));
      assertThat(modFor(DoubleModifier.MUS_EXPERIENCE), equalTo(1.0));
      assertThat(modFor(DoubleModifier.MYS_EXPERIENCE), equalTo(1.0));
    }
  }

  @Test
  void considersGemsAlreadyInCodpiece() {
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.CODPIECE1, ItemPool.ALIEN_GEMSTONE),
            withItem("autumn years wisdom"));

    try (cleanups) {
      assertTrue(maximize("mus exp, mys exp, -tie"));
      assertThat(getBoosts(), not(hasItem(recommends("alien gemstone"))));
      assertThat(getBoosts(), hasItem(recommends("autumn years wisdom")));
      assertThat(modFor(DoubleModifier.MUS_EXPERIENCE), equalTo(1.0));
      assertThat(modFor(DoubleModifier.MYS_EXPERIENCE), equalTo(1.0));
    }
  }

  @Test
  void considersUnequippedCodpieceDespiteIrrelevantSlottedGems() {
    var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.CODPIECE1, "big bumboozer marble"),
            withEquipped(Slot.CODPIECE2, "black catseye marble"),
            withEquipped(Slot.CODPIECE3, "beach ball marble"),
            withEquipped(Slot.CODPIECE4, "steely marble"),
            withEquipped(Slot.CODPIECE5, "beige clambroth marble"),
            withItem(ItemPool.ALIEN_GEMSTONE),
            withItem("autumn years wisdom"));

    try (cleanups) {
      assertTrue(maximize("mus exp, mys exp, -tie"));
      assertThat(getBoosts(), hasItem(recommends(ItemPool.THE_ETERNITY_CODPIECE)));
      assertThat(getBoosts(), hasItem(recommends("alien gemstone")));
      assertThat(getBoosts(), hasItem(recommends("autumn years wisdom")));
      assertThat(modFor(DoubleModifier.MUS_EXPERIENCE), equalTo(1.0));
      assertThat(modFor(DoubleModifier.MYS_EXPERIENCE), equalTo(1.0));
    }
  }

  @Test
  void prunesNonmatchingGemsBeforeCheckingCombinations() {
    var cleanups = new Cleanups(withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE));

    try (cleanups) {
      assertTrue(maximize("mus exp, -tie"));
      int combinationsWithoutGems = Maximizer.bestChecked;

      try (var ignored =
          new Cleanups(
              withItem("big bumboozer marble"),
              withItem("black catseye marble"),
              withItem("beach ball marble"),
              withItem("steely marble"),
              withItem("beige clambroth marble"),
              withItem("jet bennie marble"),
              withItem("bumblebee marble"))) {
        assertTrue(maximize("mus exp, -tie"));
        assertThat(Maximizer.bestChecked, equalTo(combinationsWithoutGems));
        assertThat(modFor(DoubleModifier.MUS_EXPERIENCE), equalTo(0.0));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.CODPIECE1))));
      }
    }
  }

  @Test
  void choosesBestFiveWhenMoreThanFiveGemsMatch() {
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withItem("big bumboozer marble"),
            withItem("black catseye marble"),
            withItem("beach ball marble"),
            withItem("steely marble"),
            withItem("beige clambroth marble"));

    try (cleanups) {
      assertTrue(maximize("init, -tie"));

      try (var ignored =
          new Cleanups(withItem("jet bennie marble"), withItem("bumblebee marble"))) {
        assertTrue(maximize("init, -tie"));
        assertThat(modFor(DoubleModifier.INITIATIVE), equalTo(45.0));
        assertThat(getBoosts(), hasItem(recommends("big bumboozer marble")));
        assertThat(getBoosts(), hasItem(recommends("black catseye marble")));
        assertThat(getBoosts(), hasItem(recommends("beach ball marble")));
        assertThat(getBoosts(), hasItem(recommends("steely marble")));
        assertThat(getBoosts(), hasItem(recommends("beige clambroth marble")));
        assertThat(getBoosts(), not(hasItem(recommends("jet bennie marble"))));
        assertThat(getBoosts(), not(hasItem(recommends("bumblebee marble"))));
      }
    }
  }

  @Test
  void enumeratesCodpieceGemCombinationsRatherThanPermutations() {
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withItem("big bumboozer marble", 5),
            withItem("black catseye marble", 5),
            withItem("beach ball marble", 5),
            withItem("steely marble", 5),
            withItem("beige clambroth marble", 5),
            withItem("jet bennie marble", 5),
            withItem("bumblebee marble", 5),
            withItem("lemonade marble", 5),
            withItem("red China marble", 5),
            withItem("brown crock marble", 5));

    try (cleanups) {
      assertTrue(
          maximize("init, 1 bonus big bumboozer marble, -acc1, -acc2, -acc3, -offhand, -tie"));
      assertTrue(Maximizer.bestChecked < 3003);
      assertThat(modFor(DoubleModifier.INITIATIVE), equalTo(55.0));

      assertTrue(maximize("init, letter b, -acc1, -acc2, -acc3, -offhand, -tie"));
      assertTrue(Maximizer.bestChecked < 3003);

      assertTrue(
          maximize(
              "init, 0.000001 slime resistance, 1 bonus big bumboozer marble, -acc1, -acc2, -acc3, -offhand, -tie"));
      assertTrue(Maximizer.bestChecked < 3003);
      assertThat(modFor(DoubleModifier.INITIATIVE), equalTo(55.0));

      assertTrue(
          maximize(
              "init, 0.000001 smithsness, 1 bonus big bumboozer marble, -acc1, -acc2, -acc3, -offhand, -tie"));
      assertTrue(Maximizer.bestChecked < 3003);
      assertThat(modFor(DoubleModifier.INITIATIVE), equalTo(55.0));

      var exhaustiveExpression =
          "init, 1 bonus big bumboozer marble, -acc1, -acc2, -acc3, -offhand, -tie";
      assertTrue(maximizeExhaustively(exhaustiveExpression));
      // Exercise canonical enumeration of C'(10 + 1, 5) = 3,003 multisets rather than ordered
      // permutations.
      assertEquals(3003, Maximizer.bestChecked);
      assertThat(modFor(DoubleModifier.INITIATIVE), equalTo(55.0));

      try (var fixed = withEquipped(Slot.CODPIECE3, "big bumboozer marble")) {
        assertTrue(maximizeExhaustively(exhaustiveExpression + ", -codpiece3"));
        // Slot 3 remains fixed, leaving four interchangeable slots: C'(10 + 1, 4) = 1,001.
        assertEquals(1001, Maximizer.bestChecked);
        assertThat(
            Maximizer.best.equipment.get(Slot.CODPIECE3).getName(),
            equalTo("big bumboozer marble"));
        assertThat(modFor(DoubleModifier.INITIATIVE), equalTo(55.0));
      }
    }
  }

  @Test
  void prunesCodpieceBranchesThatCannotMeetHardRequirements() {
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.ACCESSORY2, "head mirror"),
            withEquipped(Slot.ACCESSORY3, "surgical mask"),
            withEquipped(Slot.WEAPON, "half-size scalpel"),
            withEquipped(Slot.PANTS, "bloodied surgical dungarees"),
            withItem("big bumboozer marble", 5),
            withItem("black catseye marble", 5),
            withItem("beach ball marble", 5),
            withItem("steely marble", 5),
            withItem("beige clambroth marble", 5),
            withItem("jet bennie marble", 5),
            withItem("bumblebee marble", 5),
            withItem("lemonade marble", 5),
            withItem("red China marble", 5),
            withItem("brown crock marble", 5));

    try (cleanups) {
      assertFalse(
          maximize(
              "5 surgeonosity, -hat, -weapon, -offhand, -back, -shirt, -pants, "
                  + "-familiar, -acc1, -acc2, -acc3, -tie"));
      assertEquals(1, Maximizer.bestChecked);
      assertThat(modFor(BitmapModifier.SURGEONOSITY), equalTo(4.0));
      assertTrue(
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .allMatch(EquipmentRequest.UNEQUIP::equals));
    }
  }

  @Test
  void preservesTiebreakingWhenHardRequirementsCannotBeMet() {
    int diamond = ItemPool.get("lump of diamond").getItemId();
    int pearl = ItemPool.get("unblemished pearl").getItemId();
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.ACCESSORY2, "head mirror"),
            withEquipped(Slot.ACCESSORY3, "surgical mask"),
            withEquipped(Slot.WEAPON, "half-size scalpel"),
            withEquipped(Slot.PANTS, "bloodied surgical dungarees"),
            withItem(diamond),
            withItem(pearl),
            withOverrideModifiers(ModifierType.ETERNITY_CODPIECE, diamond, "HP Regen Max: +100"),
            withOverrideModifiers(ModifierType.ETERNITY_CODPIECE, pearl, ""));

    try (cleanups) {
      assertFalse(
          maximize(
              "5 surgeonosity, -hat, -weapon, -offhand, -back, -shirt, -pants, "
                  + "-familiar, -acc1, -acc2, -acc3"));
      assertTrue(
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .anyMatch(item -> item.getItemId() == diamond));
    }
  }

  @Test
  void prunesWhenFamiliarWeightChangesDropScore() {
    try (var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY, 20),
            withItem("Heartstone", 5),
            withItem("massive gemstone", 5))) {
      String expression =
          "item drop, -hat, -weapon, -offhand, -back, -shirt, -pants, -acc1, -acc2, -acc3, -tie";
      assertTrue(maximize(expression));
      assertTrue(Maximizer.bestChecked < 21);
      double score = Maximizer.best.getScore();
      assertTrue(maximizeExhaustively(expression));
      assertEquals(21, Maximizer.bestChecked);
      assertThat(Maximizer.best.getScore(), equalTo(score));

      expression =
          "adventures, -hat, -weapon, -offhand, -back, -shirt, -pants, -acc1, -acc2, -acc3";
      assertTrue(maximize(expression));
      assertTrue(Maximizer.bestChecked < 21);
      score = Maximizer.best.getScore();
      double tiebreaker = Maximizer.best.getTiebreaker();
      assertTrue(maximizeExhaustively(expression));
      assertEquals(21, Maximizer.bestChecked);
      assertThat(Maximizer.best.getScore(), equalTo(score));
      assertThat(Maximizer.best.getTiebreaker(), equalTo(tiebreaker));
    }

    try (var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withFamiliar(FamiliarPool.LEPRECHAUN, 20),
            withItem("Heartstone", 5),
            withItem("incredibly dense meat gem", 5))) {
      String expression =
          "meat drop, -hat, -weapon, -offhand, -back, -shirt, -pants, -acc1, -acc2, -acc3, -tie";
      assertTrue(maximize(expression));
      assertTrue(Maximizer.bestChecked < 21);
      double score = Maximizer.best.getScore();
      assertTrue(maximizeExhaustively(expression));
      assertEquals(21, Maximizer.bestChecked);
      assertThat(Maximizer.best.getScore(), equalTo(score));
    }
  }

  @Test
  void familiarResponseRespectsScoreMaxima() {
    var hat = ItemPool.get("bounty-hunting helmet");
    try (var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.HAT, hat),
            withOverrideModifiers(
                ModifierType.ITEM,
                hat.getItemId(),
                "Familiar Weight Percent: -50, Familiar Weight Cap: 17, Fairy: 1"),
            withFamiliar(FamiliarPool.MOSQUITO, 20),
            withItem("Heartstone", 5),
            withItem("massive gemstone", 5))) {
      String expression =
          "item drop 160 max, familiar weight 15 max, "
              + "-hat, -weapon, -offhand, -back, -shirt, -pants, -acc1, -acc2, -acc3, -tie";
      assertTrue(maximize(expression));
      double score = Maximizer.best.getScore();

      assertTrue(maximizeExhaustively(expression));
      assertEquals(21, Maximizer.bestChecked);
      assertThat(Maximizer.best.getScore(), equalTo(score));
    }
  }

  @Test
  void preservesFamiliarBoundsAcrossOrdinaryEquipmentChanges() {
    try (var cleanups =
        new Cleanups(
            withStats(1_000, 1_000, 1_000),
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY, 20),
            withItem("Heartstone", 5),
            withItem("massive gemstone", 5),
            withEquippableItem("bounty-hunting helmet"),
            withEquippableItem("crumpled felt fedora"),
            withEquippableItem("hypnodisk"))) {
      String expression =
          "item drop, -weapon, -back, -shirt, -pants, -familiar, -acc1, -acc2, -acc3, -tie";
      assertTrue(maximize(expression));
      double score = Maximizer.best.getScore();
      int hat = Maximizer.best.equipment.get(Slot.HAT).getItemId();
      long heartstones =
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .filter(item -> item.getItemId() == ItemPool.HEARTSTONE)
              .count();

      assertTrue(maximizeExhaustively(expression));
      assertThat(Maximizer.best.getScore(), equalTo(score));
      assertThat(Maximizer.best.equipment.get(Slot.HAT).getItemId(), equalTo(hat));
      assertThat(
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .filter(item -> item.getItemId() == ItemPool.HEARTSTONE)
              .count(),
          equalTo(heartstones));
    }
  }

  @Test
  void prioritizesTiebreakerWhenPrimaryScoreIsSaturated() {
    int watch = ItemPool.get("Counterclockwise Watch").getItemId();
    var cleanups =
        new Cleanups(
            withStats(10_000, 10_000, 10_000),
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.ACCESSORY2, watch),
            withOverrideModifiers(ModifierType.ITEM, watch, "Damage Absorption: +1000"),
            withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY, 20),
            withProperty("maximizerCombinationLimit", 0));
    for (var entry : ModifierDatabase.getAllModifiersOfType(ModifierType.ETERNITY_CODPIECE)) {
      if (entry.getKey().isInt()) {
        cleanups.add(withItem(entry.getKey().getIntValue(), 8));
      }
    }

    try (cleanups) {
      assertTrue(
          maximize(
              "DA, -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, -acc1, -acc2, -acc3"));
      assertThat(Maximizer.best.getScore(), equalTo(1000.0));
      assertTrue(Maximizer.bestChecked < 500);
    }
  }

  @Test
  void boundsFamiliarWeightChangesToTunedVolleyballExperience() {
    try (var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withFamiliar(FamiliarPool.BANDER, 20),
            withEquipped(Slot.FAMILIAR, "crimsilion jub-jub bird"),
            withItem("Heartstone", 5),
            withItem("Tuesday's ruby", 5))) {
      assertTrue(
          maximize(
              "experience, -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, -acc1, -acc2, -acc3, -tie"));
      assertTrue(Maximizer.bestChecked < 21);
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {FamiliarPool.BLOOD_FACED_VOLLEYBALL, FamiliarPool.SOMBRERO})
  void handlesFamiliarWeightChangingUntunedExperience(int familiarId) {
    try (var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withFamiliar(familiarId, 400),
            withItem("Heartstone", 5),
            withItem("Tuesday's ruby", 5),
            withItem("lump of diamond", 5))) {
      String expression =
          "experience, 0.000001 dr, -hat, -weapon, -offhand, -back, -shirt, -pants, "
              + "-familiar, -acc1, -acc2, -acc3, -tie";
      assertTrue(maximize(expression));
      if (familiarId == FamiliarPool.SOMBRERO) {
        assertTrue(Maximizer.bestChecked < 56);
      }
      double score = Maximizer.best.getScore();

      assertTrue(maximizeExhaustively(expression));
      assertEquals(56, Maximizer.bestChecked);
      assertThat(Maximizer.best.getScore(), equalTo(score));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "12, -1 experience",
    "1, -1 item drop",
    "2, -1 meat drop",
  })
  void boundsNegativeFamiliarScores(int familiarId, String objective) {
    try (var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withFamiliar(familiarId, 400),
            withItem("Heartstone", 5),
            withItem("Tuesday's ruby", 5))) {
      String expression =
          objective
              + ", -hat, -weapon, -offhand, -back, -shirt, -pants, "
              + "-familiar, -acc1, -acc2, -acc3, -tie";
      assertTrue(maximize(expression));
      int boundedChecks = Maximizer.bestChecked;
      double score = Maximizer.best.getScore();

      assertTrue(maximizeExhaustively(expression));
      assertTrue(boundedChecks < Maximizer.bestChecked);
      assertThat(Maximizer.best.getScore(), equalTo(score));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "muscle experience, alien gemstone",
    "mysticality experience, autumn years wisdom",
    "moxie experience, rhinestone",
  })
  void boundsStatExperienceInBothDirections(String objective, String statGem) {
    try (var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withFamiliar(FamiliarPool.BLOOD_FACED_VOLLEYBALL, 400),
            withItem("Heartstone", 5),
            withItem("Tuesday's ruby", 5),
            withItem(statGem, 5))) {
      for (String sign : List.of("", "-1 ")) {
        String expression =
            sign
                + objective
                + ", -hat, -weapon, -offhand, -back, -shirt, -pants, "
                + "-familiar, -acc1, -acc2, -acc3, -tie";
        assertTrue(maximize(expression));
        int boundedChecks = Maximizer.bestChecked;
        double score = Maximizer.best.getScore();

        assertTrue(maximizeExhaustively(expression));
        assertTrue(boundedChecks <= Maximizer.bestChecked);
        assertThat(Maximizer.best.getScore(), equalTo(score));
      }
    }
  }

  @Test
  void boundsFamiliarExperienceWithLimitedGemCopies() {
    try (var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withFamiliar(FamiliarPool.BLOOD_FACED_VOLLEYBALL, 400),
            withItem("Heartstone"),
            withItem("Tuesday's ruby"),
            withItem(ItemPool.ALIEN_GEMSTONE))) {
      String expression =
          "muscle experience, -hat, -weapon, -offhand, -back, -shirt, -pants, "
              + "-familiar, -acc1, -acc2, -acc3, -tie";
      assertTrue(maximize(expression));
      double score = Maximizer.best.getScore();

      assertTrue(maximizeExhaustively(expression));
      assertThat(Maximizer.best.getScore(), equalTo(score));
    }
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "KOLMAFIA_CODPIECE_CHECK_BENCHMARK", matches = "true")
  void benchmarksCodpieceCombinationChecking() {
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withProperty("maximizerCombinationLimit", 0));
    int gemCount = 0;
    for (var entry : ModifierDatabase.getAllModifiersOfType(ModifierType.ETERNITY_CODPIECE)) {
      if (entry.getKey().isInt() && gemCount < 30) {
        cleanups.add(withItem(entry.getKey().getIntValue(), 5));
        gemCount++;
      }
    }

    try (cleanups) {
      var expression =
          "tie, enchantment count, mus percent, mys percent, mox percent, maximum mp percent, pvp fights, candy drop, damage vs. seals, damage vs. zombies, pool skill, pickpocket chance, fishing skill, damage vs. ghosts, familiar damage, damage vs. werewolves, adventures, damage vs. vampires, damage vs. bugbears, -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, -acc1, -acc2, -acc3";
      for (int run = 1; run <= 4; run++) {
        long start = System.nanoTime();
        assertTrue(maximizeExhaustively(expression));
        double elapsed = (System.nanoTime() - start) / 1_000_000.0;
        // Thirty candidates fill zero through five slots: C(30 + 5, 5) = 324,632.
        assertEquals(324_632, Maximizer.bestChecked);
        System.out.printf("CODPIECE_CHECK_BENCHMARK run=%d ms=%.3f%n", run, elapsed);
      }
    }
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "KOLMAFIA_CODPIECE_PRUNING_BENCHMARK", matches = "true")
  void benchmarksRecursiveCodpiecePruning() {
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withProperty("maximizerCombinationLimit", 0));
    for (var entry : ModifierDatabase.getAllModifiersOfType(ModifierType.ETERNITY_CODPIECE)) {
      if (entry.getKey().isInt()) {
        cleanups.add(withItem(entry.getKey().getIntValue(), 5));
      }
    }

    try (cleanups) {
      var expression =
          "adventures, pvp fights, booze drop, candy drop, damage absorption, damage reduction, familiar damage, familiar experience, fishing skill, food drop, damage vs. ghosts, hp regen, monster level, mp regen, pickpocket chance, pool skill, damage vs. seals, damage vs. vampires, damage vs. werewolves, damage vs. zombies, damage vs. bugbears, -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, -acc1, -acc2, -acc3, -tie";
      long start = System.nanoTime();
      assertTrue(maximize(expression));
      double elapsed = (System.nanoTime() - start) / 1_000_000.0;
      assertTrue(Maximizer.bestChecked < 65_780);
      System.out.printf(
          "CODPIECE_PRUNING_BENCHMARK combinations=%d ms=%.3f%n", Maximizer.bestChecked, elapsed);

      try (var springShoes = withEquippableItem("spring shoes")) {
        start = System.nanoTime();
        assertTrue(
            maximize(
                "5 item drop, 0.05 meat drop, 0.5 init 575 max, 0.1 da 1000 max, dr, "
                    + "0.5 all res, 1.5 Mainstat, Moxie, 0.4 hp, 0.2 mp 1000 max, 3 HP Regen, "
                    + "0.25 Spell Damage, 1.75 Spell Damage Percent, 2 Familiar Weight, "
                    + "5 Familiar Experience, 5 Experience, 2.5 Mysticality Experience Percent, "
                    + "49 Booze Drop 1900 max, 40 Pasta Thrall Experience, -fumble, "
                    + "+250\"bonus Baseball Diamond\", +30\"bonus Heartstone\", "
                    + "+1000\"bonus Peridot of Peril\", +400\"bonus legendary seal-clubbing club\", "
                    + "+40\"bonus Cup of 13s\", +40\"bonus Portable Laughing Stock\", "
                    + "+50\"bonus spring shoes\", +200\"bonus bat wings\", "
                    + "+200\"bonus mafia thumb ring\", +\"equip spring shoes\""));
        elapsed = (System.nanoTime() - start) / 1_000_000.0;
        System.out.printf(
            "CODPIECE_REPORTED_EXPRESSION_BENCHMARK combinations=%d ms=%.3f%n",
            Maximizer.bestChecked, elapsed);
        assertTrue(Maximizer.bestChecked < 1_000);
      }

      start = System.nanoTime();
      assertTrue(
          maximize(
              "adventures, 5 max, -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, -acc1, -acc2, -acc3"));
      elapsed = (System.nanoTime() - start) / 1_000_000.0;
      System.out.printf(
          "CODPIECE_ADVENTURE_MAX_BENCHMARK combinations=%d ms=%.3f%n",
          Maximizer.bestChecked, elapsed);
      assertTrue(Maximizer.bestChecked < 1_000);

      start = System.nanoTime();
      assertTrue(
          maximize(
              "familiar weight, -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, -acc1, -acc2, -acc3"));
      elapsed = (System.nanoTime() - start) / 1_000_000.0;
      System.out.printf(
          "CODPIECE_FAMILIAR_WEIGHT_BENCHMARK combinations=%d ms=%.3f%n",
          Maximizer.bestChecked, elapsed);
      assertTrue(Maximizer.bestChecked < 1_000);

      start = System.nanoTime();
      assertTrue(
          maximize(
              "initiative, -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, -acc1, -acc2, -acc3"));
      elapsed = (System.nanoTime() - start) / 1_000_000.0;
      System.out.printf(
          "CODPIECE_INITIATIVE_BENCHMARK combinations=%d ms=%.3f%n",
          Maximizer.bestChecked, elapsed);
      assertTrue(Maximizer.bestChecked < 1_000);

      start = System.nanoTime();
      assertTrue(
          maximize(
              "experience, -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, -acc1, -acc2, -acc3"));
      elapsed = (System.nanoTime() - start) / 1_000_000.0;
      System.out.printf(
          "CODPIECE_EXPERIENCE_BENCHMARK combinations=%d ms=%.3f%n",
          Maximizer.bestChecked, elapsed);
      assertTrue(Maximizer.bestChecked < 1_000);

      start = System.nanoTime();
      assertTrue(
          maximize(
              "item drop, -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, -acc1, -acc2, -acc3"));
      elapsed = (System.nanoTime() - start) / 1_000_000.0;
      System.out.printf(
          "CODPIECE_ITEM_DROP_BENCHMARK combinations=%d ms=%.3f%n", Maximizer.bestChecked, elapsed);
      assertTrue(Maximizer.bestChecked < 1_000);

      start = System.nanoTime();
      assertTrue(
          maximize(
              "meat drop, -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, -acc1, -acc2, -acc3"));
      elapsed = (System.nanoTime() - start) / 1_000_000.0;
      System.out.printf(
          "CODPIECE_MEAT_DROP_BENCHMARK combinations=%d ms=%.3f%n", Maximizer.bestChecked, elapsed);
      assertTrue(Maximizer.bestChecked < 1_000);

      try (var familiar = withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY, 20)) {
        start = System.nanoTime();
        assertTrue(
            maximize(
                "item drop, -hat, -weapon, -offhand, -back, -shirt, -pants, -acc1, -acc2, -acc3, -tie"));
        elapsed = (System.nanoTime() - start) / 1_000_000.0;
        System.out.printf(
            "CODPIECE_FAMILIAR_ITEM_DROP_BENCHMARK combinations=%d ms=%.3f%n",
            Maximizer.bestChecked, elapsed);
        assertTrue(Maximizer.bestChecked < 1_000);
      }

      try (var familiar = withFamiliar(FamiliarPool.LEPRECHAUN, 20)) {
        start = System.nanoTime();
        assertTrue(
            maximize(
                "meat drop, -hat, -weapon, -offhand, -back, -shirt, -pants, -acc1, -acc2, -acc3, -tie"));
        elapsed = (System.nanoTime() - start) / 1_000_000.0;
        System.out.printf(
            "CODPIECE_FAMILIAR_MEAT_DROP_BENCHMARK combinations=%d ms=%.3f%n",
            Maximizer.bestChecked, elapsed);
        assertTrue(Maximizer.bestChecked < 1_000);
      }

      var capCleanups = new Cleanups();
      int cappedGemCount = 0;
      for (var entry : ModifierDatabase.getAllModifiersOfType(ModifierType.ETERNITY_CODPIECE)) {
        if (entry.getKey().isInt() && cappedGemCount++ < 30) {
          capCleanups.add(
              withOverrideModifiers(
                  ModifierType.ETERNITY_CODPIECE,
                  entry.getKey().getIntValue(),
                  "Damage Reduction: +5"));
        }
      }
      try (capCleanups) {
        start = System.nanoTime();
        assertTrue(
            maximize(
                "damage reduction 5 max, -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, -acc1, -acc2, -acc3, -tie"));
        elapsed = (System.nanoTime() - start) / 1_000_000.0;
        assertTrue(Maximizer.bestChecked < 324_632);
        System.out.printf(
            "CODPIECE_CAP_BENCHMARK combinations=%d ms=%.3f%n", Maximizer.bestChecked, elapsed);
      }

      var muscleCleanups = new Cleanups();
      int muscleGemCount = 0;
      for (var entry : ModifierDatabase.getAllModifiersOfType(ModifierType.ETERNITY_CODPIECE)) {
        if (entry.getKey().isInt()) {
          int gemId = entry.getKey().getIntValue();
          String modifiers =
              muscleGemCount < 30 ? "Muscle: +" + (muscleGemCount == 0 ? 10 : 1) : "";
          muscleGemCount++;
          muscleCleanups.add(
              withOverrideModifiers(ModifierType.ETERNITY_CODPIECE, gemId, modifiers));
          muscleCleanups.add(withOverrideModifiers(ModifierType.ITEM, gemId, ""));
        }
      }
      try (muscleCleanups) {
        start = System.nanoTime();
        assertTrue(
            maximize(
                "muscle, -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, -acc1, -acc2, -acc3, -tie"));
        elapsed = (System.nanoTime() - start) / 1_000_000.0;
        System.out.printf(
            "CODPIECE_MUSCLE_BENCHMARK combinations=%d ms=%.3f%n", Maximizer.bestChecked, elapsed);
        assertTrue(Maximizer.bestChecked < 324_632);
      }

      var mysticalityCleanups = new Cleanups();
      int mysticalityGemCount = 0;
      for (var entry : ModifierDatabase.getAllModifiersOfType(ModifierType.ETERNITY_CODPIECE)) {
        if (entry.getKey().isInt()) {
          int gemId = entry.getKey().getIntValue();
          String modifiers =
              mysticalityGemCount < 30
                  ? "Mysticality: +" + (mysticalityGemCount == 0 ? 10 : 1)
                  : "";
          mysticalityGemCount++;
          mysticalityCleanups.add(
              withOverrideModifiers(ModifierType.ETERNITY_CODPIECE, gemId, modifiers));
          mysticalityCleanups.add(withOverrideModifiers(ModifierType.ITEM, gemId, ""));
        }
      }
      try (mysticalityCleanups) {
        start = System.nanoTime();
        assertTrue(
            maximize(
                "mysticality, -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, -acc1, -acc2, -acc3, -tie"));
        elapsed = (System.nanoTime() - start) / 1_000_000.0;
        System.out.printf(
            "CODPIECE_MYSTICALITY_BENCHMARK combinations=%d ms=%.3f%n",
            Maximizer.bestChecked, elapsed);
        assertTrue(Maximizer.bestChecked < 324_632);
      }

      var moxieCleanups = new Cleanups();
      int moxieGemCount = 0;
      for (var entry : ModifierDatabase.getAllModifiersOfType(ModifierType.ETERNITY_CODPIECE)) {
        if (entry.getKey().isInt()) {
          int gemId = entry.getKey().getIntValue();
          String modifiers = moxieGemCount < 30 ? "Moxie: +" + (moxieGemCount == 0 ? 10 : 1) : "";
          moxieGemCount++;
          moxieCleanups.add(
              withOverrideModifiers(ModifierType.ETERNITY_CODPIECE, gemId, modifiers));
          moxieCleanups.add(withOverrideModifiers(ModifierType.ITEM, gemId, ""));
        }
      }
      try (moxieCleanups) {
        start = System.nanoTime();
        assertTrue(
            maximize(
                "moxie, -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, -acc1, -acc2, -acc3, -tie"));
        elapsed = (System.nanoTime() - start) / 1_000_000.0;
        System.out.printf(
            "CODPIECE_MOXIE_BENCHMARK combinations=%d ms=%.3f%n", Maximizer.bestChecked, elapsed);
        assertTrue(Maximizer.bestChecked < 324_632);
      }

      var hitPointCleanups = new Cleanups();
      int hitPointGemCount = 0;
      for (var entry : ModifierDatabase.getAllModifiersOfType(ModifierType.ETERNITY_CODPIECE)) {
        if (entry.getKey().isInt()) {
          int gemId = entry.getKey().getIntValue();
          String modifiers =
              hitPointGemCount < 30 ? "Maximum HP: +" + (hitPointGemCount == 0 ? 20 : 1) : "";
          hitPointGemCount++;
          hitPointCleanups.add(
              withOverrideModifiers(ModifierType.ETERNITY_CODPIECE, gemId, modifiers));
          hitPointCleanups.add(withOverrideModifiers(ModifierType.ITEM, gemId, ""));
        }
      }
      try (hitPointCleanups) {
        start = System.nanoTime();
        assertTrue(
            maximize(
                "maximum hp, -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, -acc1, -acc2, -acc3, -tie"));
        elapsed = (System.nanoTime() - start) / 1_000_000.0;
        System.out.printf(
            "CODPIECE_HP_BENCHMARK combinations=%d ms=%.3f%n", Maximizer.bestChecked, elapsed);
        assertTrue(Maximizer.bestChecked < 324_632);
      }

      var manaPointCleanups = new Cleanups();
      int manaPointGemCount = 0;
      for (var entry : ModifierDatabase.getAllModifiersOfType(ModifierType.ETERNITY_CODPIECE)) {
        if (entry.getKey().isInt()) {
          int gemId = entry.getKey().getIntValue();
          String modifiers =
              manaPointGemCount < 30 ? "Maximum MP: +" + (manaPointGemCount == 0 ? 20 : 1) : "";
          manaPointGemCount++;
          manaPointCleanups.add(
              withOverrideModifiers(ModifierType.ETERNITY_CODPIECE, gemId, modifiers));
          manaPointCleanups.add(withOverrideModifiers(ModifierType.ITEM, gemId, ""));
        }
      }
      try (manaPointCleanups) {
        start = System.nanoTime();
        assertTrue(
            maximize(
                "maximum mp, -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, -acc1, -acc2, -acc3, -tie"));
        elapsed = (System.nanoTime() - start) / 1_000_000.0;
        System.out.printf(
            "CODPIECE_MP_BENCHMARK combinations=%d ms=%.3f%n", Maximizer.bestChecked, elapsed);
        assertTrue(Maximizer.bestChecked < 324_632);
      }

      var tiebreakCleanups = new Cleanups();
      int tiebreakGemCount = 0;
      for (var entry : ModifierDatabase.getAllModifiersOfType(ModifierType.ETERNITY_CODPIECE)) {
        if (entry.getKey().isInt()) {
          int gemId = entry.getKey().getIntValue();
          String modifiers = "";
          if (tiebreakGemCount < 30) {
            tiebreakGemCount++;
            modifiers = "Damage Reduction: +5, HP Regen Max: +" + tiebreakGemCount;
          }
          tiebreakCleanups.add(
              withOverrideModifiers(ModifierType.ETERNITY_CODPIECE, gemId, modifiers));
          tiebreakCleanups.add(withOverrideModifiers(ModifierType.ITEM, gemId, ""));
        }
      }
      try (tiebreakCleanups) {
        start = System.nanoTime();
        assertTrue(
            maximize(
                "damage reduction 5 max, -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, -acc1, -acc2, -acc3"));
        elapsed = (System.nanoTime() - start) / 1_000_000.0;
        assertTrue(Maximizer.bestChecked < 324_632);
        System.out.printf(
            "CODPIECE_TIEBREAK_BENCHMARK combinations=%d ms=%.3f%n",
            Maximizer.bestChecked, elapsed);
      }

      int watch = ItemPool.get("Counterclockwise Watch").getItemId();
      try (var ignored =
          new Cleanups(
              withOverrideModifiers(ModifierType.ITEM, watch, "Damage Reduction: +30"),
              withEquipped(Slot.ACCESSORY1, watch),
              withItem(ItemPool.THE_ETERNITY_CODPIECE))) {
        start = System.nanoTime();
        assertTrue(maximize("0 damage reduction 30 min, " + expression.replace("-acc1, ", "")));
        elapsed = (System.nanoTime() - start) / 1_000_000.0;
        assertTrue(
            Maximizer.bestChecked < 62_800,
            () -> "checked " + Maximizer.bestChecked + " combinations");
        System.out.printf(
            "CODPIECE_MINIMUM_BENCHMARK combinations=%d ms=%.3f%n", Maximizer.bestChecked, elapsed);
      }
    }
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "KOLMAFIA_CODPIECE_BENCHMARK", matches = "true")
  void benchmarksAllCodpieceGemCombinations() {
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY, 20),
            withProperty("maximizerCombinationLimit", 0));
    var scoredModifiers = new LinkedHashSet<String>();
    int gemCount = 0;
    for (var entry : ModifierDatabase.getAllModifiersOfType(ModifierType.ETERNITY_CODPIECE)) {
      if (!entry.getKey().isInt()) {
        continue;
      }

      int gemId = entry.getKey().getIntValue();
      cleanups.add(withItem(gemId, 5));
      var modifiers = ModifierDatabase.getModifiers(ModifierType.ETERNITY_CODPIECE, gemId);
      for (var modifier : DoubleModifier.values()) {
        if (modifiers.getDouble(modifier) != 0.0 || !modifiers.getDoubles(modifier).isEmpty()) {
          scoredModifiers.add(modifier.getName());
        }
      }
      gemCount++;
    }

    try (cleanups) {
      var expression =
          String.join(", ", scoredModifiers)
              + ", -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, -acc1, -acc2, -acc3, -tie";
      long start = System.nanoTime();
      assertTrue(maximize(expression));
      double elapsed = (System.nanoTime() - start) / 1_000_000.0;
      long expected = 1;
      for (int slot = 1; slot <= SlotSet.CODPIECE_SLOTS.size(); slot++) {
        expected = expected * (gemCount + slot) / slot;
      }
      assertEquals(expected, Maximizer.bestChecked);
      System.out.printf(
          "ALL_CODPIECE_GEMS_BENCHMARK gems=%d combinations=%d ms=%.3f%n",
          gemCount, Maximizer.bestChecked, elapsed);
    }
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "KOLMAFIA_MAXIMIZER_STRESS_BENCHMARK", matches = "true")
  void benchmarksAllAvailableEquipment() {
    var requestedExpressions =
        System.getenv().getOrDefault("KOLMAFIA_MAXIMIZER_STRESS_EXPRESSIONS", "");
    var expressions =
        Boolean.parseBoolean(
                System.getenv().getOrDefault("KOLMAFIA_MAXIMIZER_STRESS_DEFAULTS", "false"))
            ? List.of(Preferences.getString("maximizerList").split(" \\| "))
            : !requestedExpressions.isEmpty()
                ? List.of(requestedExpressions.split(" \\| "))
                : List.of(
                    System.getenv().getOrDefault("KOLMAFIA_MAXIMIZER_STRESS_EXPRESSION", "adv"));
    var combinationLimit =
        Long.parseLong(System.getenv().getOrDefault("KOLMAFIA_MAXIMIZER_STRESS_LIMIT", "0"));
    var cleanups =
        new Cleanups(
            withStats(10_000, 10_000, 10_000),
            withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY, 20),
            withHardcore(false),
            withInteractivity(true),
            withRestricted(false),
            withMeat(Integer.MAX_VALUE),
            withProperty("autoSatisfyWithMall", true),
            withProperty("maximizerCombinationLimit", combinationLimit));
    var availableItems = new LinkedHashSet<Integer>();
    int itemId = 0;
    while ((itemId = EquipmentDatabase.nextEquipmentItemId(itemId)) != -1) {
      availableItems.add(itemId);
    }
    for (var entry : ModifierDatabase.getAllModifiersOfType(ModifierType.ETERNITY_CODPIECE)) {
      if (entry.getKey().isInt()) {
        availableItems.add(entry.getKey().getIntValue());
      }
    }
    var previousInventory = new ArrayList<>(KoLConstants.inventory);
    KoLConstants.inventory.clear();
    availableItems.forEach(id -> KoLConstants.inventory.add(ItemPool.get(id, 8)));
    EquipmentManager.updateEquipmentLists();

    try (cleanups) {
      try {
        double totalElapsed = 0.0;
        for (var expression : expressions) {
          long start = System.nanoTime();
          maximizeAny(expression);
          double elapsed = (System.nanoTime() - start) / 1_000_000.0;
          totalElapsed += elapsed;
          assertFalse(Maximizer.best.failed);
          System.out.printf(
              "ALL_EQUIPMENT_BENCHMARK expression=%s items=%d combinations=%d score=%.3f ms=%.3f%n",
              expression,
              availableItems.size(),
              Maximizer.bestChecked,
              Maximizer.best.getScore(),
              elapsed);
        }
        System.out.printf(
            "ALL_EQUIPMENT_BENCHMARK_TOTAL expressions=%d ms=%.3f%n",
            expressions.size(), totalElapsed);
      } finally {
        KoLConstants.inventory.clear();
        KoLConstants.inventory.addAll(previousInventory);
        EquipmentManager.updateEquipmentLists();
      }
    }
  }

  @Test
  public void allCodpieceSlotsConsideredByDefault() {
    int massiveGemstone = ItemPool.get("massive gemstone", 1).getItemId();
    final var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withItem(ItemPool.HAMETHYST, 5),
            withItem(massiveGemstone, 5));

    try (cleanups) {
      assertTrue(maximize("tie"));
      assertThat(getBoosts(), hasItem(recommends(ItemPool.THE_ETERNITY_CODPIECE)));
      assertTrue(
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .allMatch(item -> item.getItemId() == massiveGemstone));
    }
  }

  @ParameterizedTest
  @CsvSource({"2, 22.0", "7, 55.0"})
  void respectsOwnedCopiesAndAvailableSlots(int copies, double expectedInitiative) {
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withItem("big bumboozer marble", copies));

    try (cleanups) {
      assertTrue(maximize("init, -tie"));
      assertThat(modFor(DoubleModifier.INITIATIVE), equalTo(expectedInitiative));
    }
  }

  @Test
  void honorsBooleanConstraintsOnCodpieceGems() {
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withItem(ItemPool.ALIEN_GEMSTONE),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, ItemPool.ALIEN_GEMSTONE, "Adventure Underwater"));

    try (cleanups) {
      assertTrue(maximize("adventure underwater, -tie"));
      assertThat(
          Maximizer.best.equipment.get(Slot.CODPIECE1).getItemId(),
          equalTo(ItemPool.ALIEN_GEMSTONE));

      assertTrue(maximize("-adventure underwater, -tie"));
      assertTrue(
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .allMatch(EquipmentRequest.UNEQUIP::equals));
    }
  }

  @Test
  void honorsBitmapTargetsOnCodpieceGems() {
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withItem(ItemPool.ALIEN_GEMSTONE),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, ItemPool.ALIEN_GEMSTONE, "Surgeonosity: +1"));

    try (cleanups) {
      assertTrue(maximize("1 surgeonosity, -tie"));
      assertThat(
          Maximizer.best.equipment.get(Slot.CODPIECE1).getItemId(),
          equalTo(ItemPool.ALIEN_GEMSTONE));
    }
  }

  @Test
  void codpieceGemsAreUsableEvenWhenNotAllowedInStandard() {
    int massiveGemstone = ItemPool.get("massive gemstone").getItemId();
    var cleanups =
        new Cleanups(
            withRestricted(true),
            withNotAllowedInStandard(RestrictedItemType.ITEMS, "massive gemstone"),
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withItem("massive gemstone", 5));

    try (cleanups) {
      assertTrue(maximize("item drop, -tie"));
      assertThat(getBoosts(), hasItem(recommends(ItemPool.THE_ETERNITY_CODPIECE)));
      assertTrue(
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .allMatch(item -> item.getItemId() == massiveGemstone));
    }
  }

  @Test
  void dualPurposeGemNeedsCodpieceToBypassStandardRestriction() {
    int bloodCubicZirconia = ItemPool.get("blood cubic zirconia").getItemId();
    var cleanups =
        new Cleanups(
            withRestricted(true),
            withNotAllowedInStandard(RestrictedItemType.ITEMS, "blood cubic zirconia"),
            withItem("blood cubic zirconia", 1));

    try (cleanups) {
      assertTrue(maximize("spooky resistance, -tie"));
      assertThat(getBoosts(), not(hasItem(recommends("blood cubic zirconia"))));

      try (var ignored = new Cleanups(withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE))) {
        assertTrue(maximize("spooky resistance, -tie"));
        assertThat(getBoosts(), hasItem(recommends("blood cubic zirconia")));
        assertTrue(
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .anyMatch(item -> item.getItemId() == bloodCubicZirconia));
      }
    }
  }

  @Nested
  class Tiebreaking {
    @Test
    void tieDisabledPrunesTiebreakerOnlyGems() {
      int massiveGemstone = ItemPool.get("massive gemstone", 1).getItemId();
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
              withItem(massiveGemstone, 5));

      try (cleanups) {
        assertTrue(maximize("-tie"));
        assertTrue(
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .allMatch(EquipmentRequest.UNEQUIP::equals));
      }
    }

    @Test
    void primaryScoreOutranksTiebreakerScore() {
      int massiveGemstone = ItemPool.get("massive gemstone", 1).getItemId();
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
              withItem(ItemPool.ALIEN_GEMSTONE),
              withItem(massiveGemstone));

      try (cleanups) {
        assertTrue(maximize("mus exp, codpiece1"));
        assertThat(
            Maximizer.best.equipment.get(Slot.CODPIECE1).getItemId(),
            equalTo(ItemPool.ALIEN_GEMSTONE));
      }
    }

    @Test
    void tiebreakerDoesNotRescueNegativePrimaryScore() {
      int massiveGemstone = ItemPool.get("massive gemstone", 1).getItemId();
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
              withItem(massiveGemstone));

      try (cleanups) {
        assertTrue(maximize("-item, tie"));
        assertThat(Maximizer.best.equipment.get(Slot.CODPIECE1), equalTo(EquipmentRequest.UNEQUIP));
      }
    }
  }

  @Nested
  class OptionsAndAcquisition {
    @Test
    void positiveSlotSelectorEnablesOnlyThatCodpieceSlot() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
              withItem(ItemPool.ALIEN_GEMSTONE, 5));

      try (cleanups) {
        assertTrue(maximize("mus exp, codpiece3, -tie"));
        assertThat(
            Maximizer.best.equipment.get(Slot.CODPIECE3).getItemId(),
            equalTo(ItemPool.ALIEN_GEMSTONE));
        assertTrue(
            SlotSet.CODPIECE_SLOTS.stream()
                .filter(slot -> slot != Slot.CODPIECE3)
                .map(Maximizer.best.equipment::get)
                .allMatch(EquipmentRequest.UNEQUIP::equals));
      }
    }

    @Test
    void offhandExclusionStillAllowsBaseballDiamondAsCodpieceGem() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
              withEquippableItem(ItemPool.BASEBALL_DIAMOND));

      try (cleanups) {
        assertTrue(maximize("weapon damage, -offhand, -tie"));
        assertThat(Maximizer.best.equipment.get(Slot.OFFHAND), equalTo(EquipmentRequest.UNEQUIP));
        assertTrue(
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .anyMatch(item -> item.getItemId() == ItemPool.BASEBALL_DIAMOND));
      }
    }

    @Test
    void equipmentFilterControlsCodpieceGemRecommendations() {
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
              withItem(ItemPool.ALIEN_GEMSTONE));

      try (cleanups) {
        assertTrue(
            Maximizer.maximize(
                "mus exp, -tie",
                0,
                PriceLevel.DONT_CHECK,
                EquipScope.SPECULATE_INVENTORY,
                EnumSet.of(KoLConstants.filterType.EQUIP)));
        assertThat(getBoosts(), hasItem(recommends(ItemPool.ALIEN_GEMSTONE)));

        Maximizer.maximize(
            "mus exp, -tie",
            0,
            PriceLevel.DONT_CHECK,
            EquipScope.SPECULATE_INVENTORY,
            EnumSet.of(KoLConstants.filterType.OTHER));
        assertThat(getBoosts(), not(hasItem(recommends(ItemPool.ALIEN_GEMSTONE))));
      }
    }

    @Test
    void currentControlsWhetherIrrelevantSlottedGemsArePreserved() {
      int massiveGemstone = ItemPool.get("massive gemstone", 1).getItemId();
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
              withEquipped(Slot.CODPIECE1, massiveGemstone));

      try (cleanups) {
        assertTrue(maximize("+equip the eternity codpiece, mus exp, current, -tie"));
        assertThat(
            Maximizer.best.equipment.get(Slot.CODPIECE1).getItemId(), equalTo(massiveGemstone));

        assertTrue(maximize("+equip the eternity codpiece, mus exp, -current, -tie"));
        assertThat(Maximizer.best.equipment.get(Slot.CODPIECE1), equalTo(EquipmentRequest.UNEQUIP));
      }
    }

    @Test
    void pullsEnoughCopiesToFillEveryCodpieceSlot() {
      var onyx = ItemPool.get("unearthly onyx", 1);
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
              withItemInStorage(onyx.getItemId(), 5),
              withInteractivity(false));

      try (cleanups) {
        maximizeAny("spooky resistance, -tie");

        assertTrue(
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .allMatch(onyx::equals));
        assertThat(
            getBoosts().stream()
                .filter(boost -> onyx.equals(boost.getItem()))
                .filter(boost -> boost.getCmd().startsWith("pull"))
                .count(),
            equalTo(5L));
      }
    }

    @Test
    void skipsCodpieceBranchWhenTheoreticalMaximumCannotBeatCurrentEquipment() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, "Counterclockwise Watch"),
              withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
              withItem("unblemished pearl", 5),
              withItem(ItemPool.HEARTSTONE, 5),
              withItem(ItemPool.ALIEN_GEMSTONE, 5),
              withItem("control crystal", 5),
              withItem("autumn years wisdom", 5),
              withItem("blood cubic zirconia", 5));

      try (cleanups) {
        assertTrue(
            maximize(
                "adv, -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, -acc2, -acc3"));

        assertThat(
            Maximizer.best.equipment.get(Slot.ACCESSORY1).getName(),
            equalTo("Counterclockwise Watch"));
        assertTrue(Maximizer.bestChecked < 100);
      }
    }

    @Test
    void skipsCodpieceBranchForIndependentAdditiveModifier() {
      int watch = ItemPool.get("Counterclockwise Watch").getItemId();
      var cleanups =
          new Cleanups(
              withOverrideModifiers(
                  ModifierType.ITEM, watch, "Damage Reduction: +30, Single Equip"),
              withEquipped(Slot.ACCESSORY1, watch),
              withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
              withItem("lump of diamond", 5),
              withItem(ItemPool.HEARTSTONE, 5),
              withItem(ItemPool.ALIEN_GEMSTONE, 5),
              withItem("control crystal", 5),
              withItem("autumn years wisdom", 5),
              withItem("blood cubic zirconia", 5));

      try (cleanups) {
        assertTrue(
            maximize(
                "damage reduction, -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, -acc2, -acc3"));

        assertThat(Maximizer.best.equipment.get(Slot.ACCESSORY1).getItemId(), equalTo(watch));
        assertTrue(Maximizer.bestChecked < 100);
      }
    }

    @Test
    void keepsBestFailedCodpieceConfigurationWhenMinimumIsImpossible() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
              withItem("lump of diamond", 5));

      try (cleanups) {
        assertFalse(
            maximize(
                "damage reduction 100 min, -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, -acc1, -acc2, -acc3, -tie"));

        assertThat(modFor(DoubleModifier.DAMAGE_REDUCTION), equalTo(25.0));
      }
    }

    @Test
    void searchesPastPrimaryCapForTiebreaker() {
      int diamond = ItemPool.get("lump of diamond").getItemId();
      int pearl = ItemPool.get("unblemished pearl").getItemId();
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
              withItem(diamond),
              withItem(pearl),
              withOverrideModifiers(
                  ModifierType.ETERNITY_CODPIECE, diamond, "Damage Reduction: +5"),
              withOverrideModifiers(
                  ModifierType.ETERNITY_CODPIECE, pearl, "Damage Reduction: +5, Item Drop: +100"));

      try (cleanups) {
        assertTrue(
            maximize(
                "damage reduction 5 max, -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, -acc1, -acc2, -acc3"));

        assertTrue(
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .anyMatch(item -> item.getItemId() == pearl));
      }
    }

    @Test
    void searchesPastTiebreakBoundForItemDropperFlag() {
      int diamond = ItemPool.get("lump of diamond").getItemId();
      int pearl = ItemPool.get("unblemished pearl").getItemId();
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
              withItem(diamond),
              withItem(pearl),
              withOverrideModifiers(
                  ModifierType.ETERNITY_CODPIECE,
                  diamond,
                  "Damage Reduction: +5, HP Regen Max: +100"),
              withOverrideModifiers(ModifierType.ETERNITY_CODPIECE, pearl, "Damage Reduction: +5"),
              withOverrideModifiers(ModifierType.ITEM, diamond, ""),
              withOverrideModifiers(ModifierType.ITEM, pearl, "Drops Items"));

      try (cleanups) {
        assertTrue(
            maximize(
                "damage reduction 5 max, -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, -acc1, -acc2, -acc3"));

        assertTrue(
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .anyMatch(item -> item.getItemId() == pearl));
      }
    }

    @Test
    void searchesPastPrimaryCapToPreserveCurrentGems() {
      int diamond = ItemPool.get("lump of diamond").getItemId();
      int pearl = ItemPool.get("unblemished pearl").getItemId();
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
              withEquipped(Slot.CODPIECE1, diamond),
              withEquipped(Slot.CODPIECE2, pearl),
              withOverrideModifiers(
                  ModifierType.ETERNITY_CODPIECE, diamond, "Damage Reduction: +5"),
              withOverrideModifiers(ModifierType.ETERNITY_CODPIECE, pearl, "Damage Reduction: +5"));

      try (cleanups) {
        assertTrue(
            maximize(
                "damage reduction 5 max, -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, -acc1, -acc2, -acc3, -tie"));

        assertThat(Maximizer.best.equipment.get(Slot.CODPIECE1).getItemId(), equalTo(diamond));
        assertThat(Maximizer.best.equipment.get(Slot.CODPIECE2).getItemId(), equalTo(pearl));
      }
    }

    @Test
    void buysOnlyMissingStorageCopiesForCodpieceSlots() {
      var onyx = ItemPool.get("unearthly onyx", 1);
      var previousStorageMeat = KoLCharacter.getStorageMeat();
      KoLCharacter.setStorageMeat(100_000);
      var cleanups =
          new Cleanups(
              withItemInStorage(onyx.getItemId(), 2),
              withHardcore(false),
              withInteractivity(false),
              withProperty("autoSatisfyWithMall", true),
              withMallPrice(onyx.getItemId(), 1_000),
              new Cleanups(() -> KoLCharacter.setStorageMeat(previousStorageMeat)));

      try (cleanups) {
        var checked =
            new CheckedItem(
                onyx.getItemId(), EquipScope.SPECULATE_ANY, 1_001, PriceLevel.BUYABLE_ONLY);

        assertThat(checked.pullable, equalTo(2));
        assertThat(checked.pullBuyable, equalTo(3));
        assertThat(checked.getAvailableCount(), equalTo(5));
      }
    }

    @ParameterizedTest
    @CsvSource({"999, 0", "1001, 5"})
    void mallPriceLimitControlsCodpieceGemAvailability(int maxPrice, long expectedGemCount) {
      var onyx = ItemPool.get("unearthly onyx", 1);
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
              withInteractivity(true),
              withProperty("autoSatisfyWithMall", true),
              withMallPrice(onyx.getItemId(), 1_000),
              withMeat(100_000));

      try (cleanups) {
        assertTrue(
            Maximizer.maximize(
                "spooky resistance, -tie",
                maxPrice,
                PriceLevel.BUYABLE_ONLY,
                EquipScope.SPECULATE_ANY,
                EnumSet.allOf(KoLConstants.filterType.class)));
        assertThat(
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .filter(onyx::equals)
                .count(),
            equalTo(expectedGemCount));
      }
    }
  }
}
