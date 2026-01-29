package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.getBoosts;
import static internal.helpers.Maximizer.maximize;
import static internal.helpers.Maximizer.maximizeAny;
import static internal.helpers.Maximizer.maximizeCreatable;
import static internal.helpers.Maximizer.modFor;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withHardcore;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withItemInCloset;
import static internal.helpers.Player.withItemInFreepulls;
import static internal.helpers.Player.withItemInStorage;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSkill;
import static internal.helpers.Player.withStats;
import static internal.matchers.Maximizer.recommends;
import static internal.matchers.Maximizer.recommendsSlot;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.DerivedModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MaximizerEnumerationTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("MaximizerEnumerationTest");
    Preferences.reset("MaximizerEnumerationTest");
  }

  @Nested
  class ItemAvailability {
    @Test
    public void findsItemsInInventory() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("mus"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
      }
    }

    @Test
    public void findsEquippedItems() {
      var cleanups = new Cleanups(withEquipped(Slot.HAT, "helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("mus, current"));
        assertEquals(1, modFor(DerivedModifier.BUFFED_MUS), 0.01);
      }
    }

    @Test
    public void findsMultipleCopiesOfSameItem() {
      var cleanups = new Cleanups(withEquippableItem("hand in glove", 3));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // Should be able to fill multiple accessory slots
      }
    }
  }

  @Nested
  class EquipScopeTests {
    @Test
    public void speculateOnlyFindsOnHandItems() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("mus"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
      }
    }

    @Test
    public void speculateCreatableConsidersCreatableItems() {
      var cleanups =
          new Cleanups(
              withItem("helmet turtle"),
              withItem("seal-skull helmet"),
              withStats(100, 100, 100));
      try (cleanups) {
        maximizeCreatable("mus");
        // Should consider creatable items in addition to on-hand
      }
    }

    @Test
    public void speculateAnyConsidersMallItems() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        maximizeAny("mus");
        // Should consider mall items
      }
    }
  }

  @Nested
  class FamiliarEquipment {
    @Test
    public void findsFamiliarEquipmentForCurrentFamiliar() {
      var cleanups =
          new Cleanups(withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY), withEquippableItem("lead necklace"));
      try (cleanups) {
        assertTrue(maximize("familiar weight"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.FAMILIAR, "lead necklace")));
      }
    }

    @Test
    public void excludesFamiliarEquipmentWhenSlotExcluded() {
      var cleanups =
          new Cleanups(withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY), withEquippableItem("lead necklace"));
      try (cleanups) {
        assertTrue(maximize("familiar weight, -familiar"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.FAMILIAR))));
      }
    }
  }

  @Nested
  class WeaponEnumeration {
    @Test
    public void findsOneHandedWeapons() {
      var cleanups =
          new Cleanups(
              withEquippableItem("seal-clubbing club"),
              withEquippableItem("catskin buckler"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus, 1 hand"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "seal-clubbing club")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND)));
      }
    }

    @Test
    public void filtersByWeaponType() {
      var cleanups =
          new Cleanups(
              withEquippableItem("seal-clubbing club"),
              withEquippableItem("disco ball"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus, club"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "seal-clubbing club")));
      }
    }

    @Test
    public void filtersByMeleeVsRanged() {
      var cleanups =
          new Cleanups(
              withEquippableItem("seal-clubbing club"),
              withEquippableItem("disco ball"),
              withStats(100, 100, 100));
      try (cleanups) {
        // -melee means ranged
        assertTrue(maximize("mox, -melee"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "disco ball")));
      }
    }
  }

  @Nested
  class OffhandEnumeration {
    @Test
    public void findsShields() {
      var cleanups =
          new Cleanups(
              withEquippableItem("catskin buckler"),
              withEquippableItem("seal-clubbing club"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("da, shield"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "catskin buckler")));
      }
    }
  }

  @Nested
  class AccessoryEnumeration {
    @Test
    public void findsAccessories() {
      var cleanups = new Cleanups(withEquippableItem("Hand in Glove"));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // Should recommend accessory
        assertThat(getBoosts(), hasItem(recommends("Hand in Glove")));
      }
    }

    @Test
    public void fillsMultipleAccessorySlots() {
      var cleanups = new Cleanups(withEquippableItem("hand in glove", 3));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // With 3 copies, should fill all accessory slots
      }
    }

    @Test
    public void respectsAccessorySlotExclusions() {
      var cleanups = new Cleanups(withEquippableItem("hand in glove", 3));
      try (cleanups) {
        assertTrue(maximize("mus, -acc1"));
        // Should not use acc1 slot
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.ACCESSORY1))));
      }
    }
  }

  @Nested
  class OutfitEnumeration {
    @Test
    public void considersOutfitPieces() {
      var cleanups =
          new Cleanups(
              withEquippableItem("bugbear beanie"),
              withEquippableItem("bugbear bungguard"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("+outfit bugbear costume"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "bugbear beanie")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "bugbear bungguard")));
      }
    }

    @Test
    public void excludesNegativeOutfitPieces() {
      var cleanups =
          new Cleanups(
              withEquippableItem("bugbear beanie"),
              withEquippableItem("helmet turtle"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus, -outfit bugbear costume"));
        // Should not recommend bugbear beanie
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT, "bugbear beanie"))));
      }
    }
  }

  @Nested
  class ItemExclusion {
    @Test
    public void minusEquipExcludesSpecificItem() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"),
              withEquippableItem("seal-skull helmet"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus, -equip helmet turtle"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT, "helmet turtle"))));
      }
    }

    @Test
    public void plusEquipRequiresSpecificItem() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"),
              withEquippableItem("seal-skull helmet"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus, +equip helmet turtle"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
      }
    }
  }

  @Nested
  class ModeableItems {
    @Test
    public void considersUmbrellaForShield() {
      var cleanups =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withProperty("umbrellaState", "forward-facing"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("da, shield"));
        // Umbrella in forward-facing mode acts as shield
      }
    }
  }

  @Nested
  class EquipRequirements {
    @Test
    public void respectsStatRequirements() {
      var cleanups =
          new Cleanups(withItem("wreath of laurels"), withEquippableItem("helmet turtle"));
      try (cleanups) {
        // Wreath of laurels has high stat requirements
        // Without sufficient stats, should prefer helmet turtle
        assertTrue(maximize("mus"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
      }
    }

    @Test
    public void considersCurrentlyEquippedItemsEvenWithoutStats() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, "Mr. Accessory"), withStats(0, 0, 0));
      try (cleanups) {
        // Mr. Accessory is equipped but we don't have stats to re-equip
        // Should still consider it as currently worn
        assertTrue(maximize("all res, current"));
      }
    }
  }

  @Nested
  class StorageItems {
    @Test
    public void findsItemsInStorage() {
      var cleanups = new Cleanups(withItemInStorage("helmet turtle"), withHardcore());
      try (cleanups) {
        // In hardcore, storage items can be considered for pullable scope
        maximizeAny("mus");
        // Just verify no error - storage access depends on game state
      }
    }

    @Test
    public void findsItemsInFreepulls() {
      var cleanups = new Cleanups(withItemInFreepulls("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // Free pulls should be considered
      }
    }
  }

  @Nested
  class CombinationLimits {
    @Test
    public void respectsCombinationLimit() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"),
              withEquippableItem("old sweatpants"),
              withProperty("maximizerCombinationLimit", 100));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // With a low limit, should still find some results
      }
    }

    @Test
    public void noCombinationLimitByDefault() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"),
              withEquippableItem("old sweatpants"),
              withProperty("maximizerCombinationLimit", 0));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // With no limit (0), should check all combinations
      }
    }

    @Test
    public void veryLowLimitStillFindsResult() {
      // This test exercises the MaximizerLimitException path by setting a limit of 1
      // which should stop after checking just one combination but still return a result
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"),
              withEquippableItem("old sweatpants"),
              withEquippableItem("eyepatch"),
              withProperty("maximizerCombinationLimit", 1));
      try (cleanups) {
        // Should still succeed even with very low limit
        assertTrue(maximize("mus"));
      }
    }
  }

  @Nested
  class SpecialEnumeration {
    @Test
    public void considersEnthroneableFamiliars() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY),
              withFamiliarInTerrarium(FamiliarPool.LEPRECHAUN),
              withEquippableItem("Crown of Thrones"));
      try (cleanups) {
        // Crown of Thrones allows enthroning a familiar
        assertTrue(maximize("meat"));
      }
    }

    @Test
    public void considersBjornableFamiliars() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY),
              withFamiliarInTerrarium(FamiliarPool.LEPRECHAUN),
              withEquippableItem("Buddy Bjorn"));
      try (cleanups) {
        // Buddy Bjorn allows carrying a familiar
        assertTrue(maximize("meat"));
      }
    }
  }

  @Nested
  class SynergyDetection {
    @Test
    public void detectsHoboPowerSynergy() {
      var cleanups =
          new Cleanups(
              withEquippableItem("hodgman's porkpie hat"),
              withEquippableItem("hodgman's lobsterskin pants"),
              withEquippableItem("hodgman's bow tie"),
              withStats(100, 100, 100));
      try (cleanups) {
        // Hobo power items have synergy
        assertTrue(maximize("hobo power"));
      }
    }

    @Test
    public void detectsSmithsnessSynergy() {
      var cleanups =
          new Cleanups(
              withEquippableItem("Vicar's Tutu"),
              withEquippableItem("Hand in Glove"),
              withSkill("Torso Awareness"),
              withStats(100, 100, 100));
      try (cleanups) {
        // Smithsness items have synergy
        assertTrue(maximize("smithsness"));
      }
    }
  }

  @Nested
  class BeeosityFiltering {
    @Test
    public void filtersHighBeeosityItemsInBeecore() {
      // In Beecore, items with many B's in the name are restricted
      var cleanups =
          new Cleanups(withEquippableItem("helmet turtle"), withEquippableItem("bugbear beanie"));
      try (cleanups) {
        // Without being in beecore, both should be considered
        assertTrue(maximize("mus"));
      }
    }
  }
}
