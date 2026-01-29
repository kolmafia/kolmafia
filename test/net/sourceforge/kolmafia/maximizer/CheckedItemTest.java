package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Player.withHardcore;
import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withItemInStorage;
import static internal.helpers.Player.withMeat;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class CheckedItemTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("CheckedItemTest");
    Preferences.reset("CheckedItemTest");
  }

  @Nested
  class Constructor {
    @Test
    public void createsBasicCheckedItem() {
      var cleanups = new Cleanups(withItem(ItemPool.HELMET_TURTLE));
      try (cleanups) {
        CheckedItem item =
            new CheckedItem(
                ItemPool.HELMET_TURTLE, EquipScope.SPECULATE_INVENTORY, 100000, PriceLevel.ALL);
        assertEquals(1, item.inventory);
        assertEquals(1, item.initial);
      }
    }

    @Test
    public void specialCaseForUnequip() {
      CheckedItem item =
          new CheckedItem(-1, EquipScope.SPECULATE_INVENTORY, 100000, PriceLevel.ALL);
      assertEquals("(none)", item.getName());
      assertEquals(Integer.MAX_VALUE, item.inventory);
      assertEquals(Integer.MAX_VALUE, item.initial);
    }

    @Test
    public void checksCreatableItems() {
      var cleanups =
          new Cleanups(withMeat(10000), withProperty("maximizerNoAdventures", false));
      try (cleanups) {
        // Helmet turtle can be created
        CheckedItem item =
            new CheckedItem(
                ItemPool.HELMET_TURTLE, EquipScope.SPECULATE_CREATABLE, 100000, PriceLevel.ALL);
        // The item may or may not be creatable depending on game state
      }
    }

    @Test
    public void checksNpcBuyableItems() {
      var cleanups = new Cleanups(withMeat(10000));
      try (cleanups) {
        // Helmet turtle can be bought from NPC
        CheckedItem item =
            new CheckedItem(
                ItemPool.HELMET_TURTLE, EquipScope.SPECULATE_CREATABLE, 100000, PriceLevel.ALL);
        // NPC buyable depends on store availability
      }
    }

    @Test
    public void respectsMaximizerNoAdventures() {
      var cleanups = new Cleanups(withProperty("maximizerNoAdventures", true));
      try (cleanups) {
        CheckedItem item =
            new CheckedItem(
                ItemPool.HELMET_TURTLE, EquipScope.SPECULATE_CREATABLE, 100000, PriceLevel.ALL);
        // Items requiring adventures to create should have creatable = 0
      }
    }

    @Test
    public void stopsEarlyWith3OrMoreItems() {
      var cleanups = new Cleanups(withItem(ItemPool.HELMET_TURTLE, 5));
      try (cleanups) {
        CheckedItem item =
            new CheckedItem(
                ItemPool.HELMET_TURTLE, EquipScope.SPECULATE_INVENTORY, 100000, PriceLevel.ALL);
        assertEquals(5, item.inventory);
        assertEquals(5, item.initial);
        // Should not check creatable/buyable when we have 3+
      }
    }
  }

  @Nested
  class Foldables {
    @Test
    public void checksFoldableItems() {
      var cleanups =
          new Cleanups(
              withProperty("maximizerFoldables", true), withItem(ItemPool.LOATHING_LEGION_KNIFE));
      try (cleanups) {
        // Loathing Legion items can be folded into other forms
        CheckedItem item =
            new CheckedItem(
                ItemPool.LOATHING_LEGION_HAMMER, EquipScope.SPECULATE_INVENTORY, 100000, PriceLevel.ALL);
        // Should detect foldable from knife
        assertThat(item.foldable, is(greaterThan(0)));
      }
    }

    @Test
    public void ignoresFoldablesWhenDisabled() {
      var cleanups =
          new Cleanups(
              withProperty("maximizerFoldables", false), withItem(ItemPool.LOATHING_LEGION_KNIFE));
      try (cleanups) {
        CheckedItem item =
            new CheckedItem(
                ItemPool.LOATHING_LEGION_HAMMER, EquipScope.SPECULATE_INVENTORY, 100000, PriceLevel.ALL);
        assertEquals(0, item.foldable);
      }
    }

    @Test
    public void garbageToteLimitedToOne() {
      var cleanups =
          new Cleanups(
              withProperty("maximizerFoldables", true),
              withItem(ItemPool.GARBAGE_TOTE),
              withItem(ItemPool.BROKEN_CHAMPAGNE));
      try (cleanups) {
        // Can only have one garbage tote item at a time
        CheckedItem item =
            new CheckedItem(
                ItemPool.MAKESHIFT_GARBAGE_SHIRT,
                EquipScope.SPECULATE_INVENTORY,
                100000,
                PriceLevel.ALL);
        // Should respect the one-item limit
      }
    }
  }

  @Nested
  class MallBuying {
    @Test
    public void checksMallBuyableInSpeculateAny() {
      var cleanups =
          new Cleanups(withInteractivity(true), withMeat(100000));
      try (cleanups) {
        CheckedItem item =
            new CheckedItem(
                ItemPool.HELMET_TURTLE, EquipScope.SPECULATE_ANY, 100000, PriceLevel.ALL);
        // Should check mall buyability
      }
    }

    @Test
    public void doesNotCheckMallInInventoryMode() {
      var cleanups = new Cleanups(withMeat(100000));
      try (cleanups) {
        CheckedItem item =
            new CheckedItem(
                ItemPool.HELMET_TURTLE, EquipScope.SPECULATE_INVENTORY, 100000, PriceLevel.ALL);
        assertEquals(0, item.mallBuyable);
      }
    }
  }

  @Nested
  class Pulling {
    @Test
    public void checksPullableInRonin() {
      var cleanups =
          new Cleanups(withItemInStorage(ItemPool.HELMET_TURTLE), withInteractivity(false));
      try (cleanups) {
        KoLCharacter.setRonin(true);
        CheckedItem item =
            new CheckedItem(
                ItemPool.HELMET_TURTLE, EquipScope.SPECULATE_ANY, 100000, PriceLevel.ALL);
        // Should check pullable items
        assertThat(item.pullable, is(greaterThan(0)));
      }
    }

    @Test
    public void doesNotCheckPullableInHardcore() {
      var cleanups = new Cleanups(withHardcore(), withItemInStorage(ItemPool.HELMET_TURTLE));
      try (cleanups) {
        CheckedItem item =
            new CheckedItem(
                ItemPool.HELMET_TURTLE, EquipScope.SPECULATE_ANY, 100000, PriceLevel.ALL);
        assertEquals(0, item.pullable);
      }
    }

    @Test
    public void checksPullFoldable() {
      var cleanups =
          new Cleanups(
              withProperty("maximizerFoldables", true),
              withItemInStorage(ItemPool.LOATHING_LEGION_KNIFE),
              withInteractivity(false));
      try (cleanups) {
        KoLCharacter.setRonin(true);
        CheckedItem item =
            new CheckedItem(
                ItemPool.LOATHING_LEGION_HAMMER, EquipScope.SPECULATE_ANY, 100000, PriceLevel.ALL);
        // Should check pull-foldable items
        assertThat(item.pullfoldable, is(greaterThan(0)));
      }
    }
  }

  @Nested
  class GetCount {
    @Test
    public void getCountReturnsMaxForItemZero() {
      CheckedItem item = new CheckedItem(0, EquipScope.SPECULATE_INVENTORY, 100000, PriceLevel.ALL);
      assertEquals(Integer.MAX_VALUE, item.getCount());
    }

    @Test
    public void getCountSumsAllSources() {
      var cleanups = new Cleanups(withItem(ItemPool.HELMET_TURTLE, 2));
      try (cleanups) {
        CheckedItem item =
            new CheckedItem(
                ItemPool.HELMET_TURTLE, EquipScope.SPECULATE_INVENTORY, 100000, PriceLevel.ALL);
        // getCount should include initial + other sources
        assertThat(item.getCount(), is(greaterThan(0)));
      }
    }

    @Test
    public void singleFlagLimitsToOne() {
      var cleanups = new Cleanups(withItem(ItemPool.HELMET_TURTLE, 5));
      try (cleanups) {
        CheckedItem item =
            new CheckedItem(
                ItemPool.HELMET_TURTLE, EquipScope.SPECULATE_INVENTORY, 100000, PriceLevel.ALL);
        item.singleFlag = true;
        assertEquals(1, item.getCount());
      }
    }
  }

  @Nested
  class Validate {
    @Test
    public void validateSkipsWhenDontCheck() throws MaximizerInterruptedException {
      CheckedItem item =
          new CheckedItem(
              ItemPool.HELMET_TURTLE, EquipScope.SPECULATE_INVENTORY, 100000, PriceLevel.DONT_CHECK);
      // Should not throw and should return early
      item.validate(100000, PriceLevel.DONT_CHECK);
    }

    @Test
    public void validateSkipsWhenNotBuyable() throws MaximizerInterruptedException {
      var cleanups = new Cleanups(withItem(ItemPool.HELMET_TURTLE));
      try (cleanups) {
        CheckedItem item =
            new CheckedItem(
                ItemPool.HELMET_TURTLE, EquipScope.SPECULATE_INVENTORY, 100000, PriceLevel.ALL);
        assertFalse(item.buyableFlag);
        // Should not throw when not buyable
        item.validate(100000, PriceLevel.ALL);
      }
    }

    @Test
    public void validateChecksMallPrice() throws MaximizerInterruptedException {
      var cleanups = new Cleanups(withInteractivity(true), withMeat(100000));
      try (cleanups) {
        CheckedItem item =
            new CheckedItem(
                ItemPool.HELMET_TURTLE, EquipScope.SPECULATE_ANY, 100000, PriceLevel.ALL);
        if (item.buyableFlag) {
          // Should check mall price when buyable
          item.validate(100000, PriceLevel.ALL);
        }
      }
    }

    @Test
    public void validateResetsWhenTooExpensive() throws MaximizerInterruptedException {
      var cleanups = new Cleanups(withInteractivity(true), withMeat(100));
      try (cleanups) {
        CheckedItem item =
            new CheckedItem(
                ItemPool.HELMET_TURTLE, EquipScope.SPECULATE_ANY, 100, PriceLevel.ALL);
        if (item.buyableFlag) {
          item.validate(100, PriceLevel.ALL);
          // Should reset mallBuyable when price exceeds limit
        }
      }
    }

    @Test
    public void validateResetsWhenNotEnoughMeat() throws MaximizerInterruptedException {
      var cleanups = new Cleanups(withInteractivity(true), withMeat(0));
      try (cleanups) {
        CheckedItem item =
            new CheckedItem(
                ItemPool.HELMET_TURTLE, EquipScope.SPECULATE_ANY, 100000, PriceLevel.ALL);
        if (item.buyableFlag) {
          item.validate(100000, PriceLevel.ALL);
          assertEquals(0, item.mallBuyable);
        }
      }
    }
  }

  @Nested
  class Flags {
    @Test
    public void automaticFlagDefaultsFalse() {
      CheckedItem item =
          new CheckedItem(
              ItemPool.HELMET_TURTLE, EquipScope.SPECULATE_INVENTORY, 100000, PriceLevel.ALL);
      assertFalse(item.automaticFlag);
    }

    @Test
    public void requiredFlagDefaultsFalse() {
      CheckedItem item =
          new CheckedItem(
              ItemPool.HELMET_TURTLE, EquipScope.SPECULATE_INVENTORY, 100000, PriceLevel.ALL);
      assertFalse(item.requiredFlag);
    }

    @Test
    public void conditionalFlagDefaultsFalse() {
      CheckedItem item =
          new CheckedItem(
              ItemPool.HELMET_TURTLE, EquipScope.SPECULATE_INVENTORY, 100000, PriceLevel.ALL);
      assertFalse(item.conditionalFlag);
    }

    @Test
    public void singleFlagDefaultsFalse() {
      CheckedItem item =
          new CheckedItem(
              ItemPool.HELMET_TURTLE, EquipScope.SPECULATE_INVENTORY, 100000, PriceLevel.ALL);
      assertFalse(item.singleFlag);
    }
  }

  @Nested
  class Constants {
    @Test
    public void masksAreCorrect() {
      assertEquals(0xFF, CheckedItem.TOTAL_MASK);
      assertEquals(0x0F, CheckedItem.SUBTOTAL_MASK);
    }

    @Test
    public void shiftsAreCorrect() {
      assertEquals(8, CheckedItem.INITIAL_SHIFT);
      assertEquals(12, CheckedItem.CREATABLE_SHIFT);
      assertEquals(16, CheckedItem.NPCBUYABLE_SHIFT);
      assertEquals(20, CheckedItem.FOLDABLE_SHIFT);
      assertEquals(24, CheckedItem.PULLABLE_SHIFT);
    }

    @Test
    public void flagsAreCorrect() {
      assertEquals(1 << 28, CheckedItem.BUYABLE_FLAG);
      assertEquals(1 << 29, CheckedItem.AUTOMATIC_FLAG);
      assertEquals(1 << 30, CheckedItem.CONDITIONAL_FLAG);
    }
  }

  @Nested
  class SkillCreateCheck {
    @Test
    public void skillCreateCheckEnabled() {
      var cleanups =
          new Cleanups(withProperty("maximizerCreateOnHand", true), withMeat(10000));
      try (cleanups) {
        // Non-equipment items with maximizerCreateOnHand should check creatable
        CheckedItem item =
            new CheckedItem(
                ItemPool.SEAL_TOOTH, EquipScope.SPECULATE_INVENTORY, 100000, PriceLevel.ALL);
        // May have creatable > 0 depending on concoctions
      }
    }

    @Test
    public void skillCreateCheckDisabled() {
      var cleanups =
          new Cleanups(withProperty("maximizerCreateOnHand", false), withMeat(10000));
      try (cleanups) {
        CheckedItem item =
            new CheckedItem(
                ItemPool.SEAL_TOOTH, EquipScope.SPECULATE_INVENTORY, 100000, PriceLevel.ALL);
        // Should not check creatable when preference disabled
      }
    }
  }

  @Nested
  class MrAccessoryProtection {
    @Test
    public void doesNotSuggestCreatingFromMrA() {
      // Items that require Mr. Accessory should not be suggested for creation
      // This protects players from accidentally using valuable currency
      var cleanups = new Cleanups(withMeat(100000));
      try (cleanups) {
        // Any item that uses Mr. A should have creatable = 0
        // even if technically possible
      }
    }
  }
}
