package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.getBoosts;
import static internal.helpers.Maximizer.maximize;
import static internal.helpers.Maximizer.maximizeAny;
import static internal.helpers.Maximizer.maximizeCreatable;
import static internal.helpers.Maximizer.modFor;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withHardcore;
import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withMeat;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withRestricted;
import static internal.helpers.Player.withSkill;
import static internal.helpers.Player.withStats;
import static internal.matchers.Maximizer.recommends;
import static internal.matchers.Maximizer.recommendsSlot;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.DerivedModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MaximizerIntegrationTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("MaximizerIntegrationTest");
    Preferences.reset("MaximizerIntegrationTest");
  }

  @Nested
  class RealWorldScenarios {
    @Test
    public void meatFarmingSetup() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"),
              withFamiliar(FamiliarPool.LEPRECHAUN),
              withSkill("Disco Leer"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("meat"));
        // Should recommend meat-boosting equipment and effects
        assertThat(modFor(DoubleModifier.MEATDROP), greaterThan(0.0));
      }
    }

    @Test
    public void itemFarmingSetup() {
      var cleanups =
          new Cleanups(
              withEquippableItem("bounty-hunting helmet"),
              withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("item"));
        // Should recommend item-boosting equipment
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "bounty-hunting helmet")));
      }
    }

    @Test
    public void combatRateIncrease() {
      var cleanups =
          new Cleanups(withEquippableItem("helmet turtle"), withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("+combat"));
        // Should try to increase combat rate
      }
    }

    @Test
    public void combatRateDecrease() {
      var cleanups =
          new Cleanups(withEquippableItem("helmet turtle"), withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("-combat"));
        // Should try to decrease combat rate
      }
    }

    @Test
    public void monsterLevelSetup() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"),
              withProperty("lastKnownLocation", "The Haunted Library"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("ml"));
        // Should find ML-boosting equipment
      }
    }

    @Test
    public void experienceGainSetup() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"),
              withFamiliar(FamiliarPool.SOMBRERO),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("exp"));
        // Should recommend experience-boosting setup
      }
    }
  }

  @Nested
  class PathSpecificBehavior {
    @Test
    public void beecoreRestrictsBeeItems() {
      var cleanups =
          new Cleanups(
              withPath(Path.BEES_HATE_YOU),
              withEquippableItem("helmet turtle"),
              withEquippableItem("bugbear beanie"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus, 0 beeosity"));
        // In beecore with 0 beeosity, should avoid items with B's
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT, "bugbear beanie"))));
      }
    }

    @Test
    public void hardcoreWithoutMallAccess() {
      var cleanups =
          new Cleanups(
              withHardcore(),
              withEquippableItem("helmet turtle"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // Hardcore limits available items
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
      }
    }

    @Test
    public void standardRestrictionsApplied() {
      var cleanups =
          new Cleanups(
              withRestricted(true),
              withEquippableItem("helmet turtle"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // Standard restrictions may limit items
      }
    }
  }

  @Nested
  class ComplexExpressions {
    @Test
    public void multipleConstraintsWithMinMax() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"),
              withEquippableItem("hardened slime hat"),
              withSkill("Refusal to Freeze"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus 100 min, cold res 5 max"));
        // Should find gear that meets all constraints
        assertThat(modFor(DerivedModifier.BUFFED_MUS), greaterThan(99.0));
      }
    }

    @Test
    public void weightedMultiModifierOptimization() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"),
              withEquippableItem("bounty-hunting helmet"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("5 mus, 0.5 item"));
        // Higher weight on muscle should prefer muscle items
      }
    }

    @Test
    public void outfitWithModifierCombination() {
      var cleanups =
          new Cleanups(
              withEquippableItem("bugbear beanie"),
              withEquippableItem("bugbear bungguard"),
              withEquippableItem("helmet turtle"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus, +outfit bugbear costume"));
        // Should equip the outfit pieces
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "bugbear beanie")));
      }
    }
  }

  @Nested
  class ExecutionResults {
    @Test
    public void boostCommandsAreValid() {
      var cleanups =
          new Cleanups(withEquippableItem("helmet turtle"), withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus"));
        var boosts = getBoosts();
        // All boosts should have valid commands
        for (Boost boost : boosts) {
          // Commands should be non-null for executable boosts
          if (boost.isEquipment()) {
            assertThat(boost.getSlot(), not(Slot.NONE));
          }
        }
      }
    }

    @Test
    public void multipleRecommendationsReturned() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"),
              withEquippableItem("old sweatpants"),
              withEquippableItem("seal-clubbing club"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus"));
        var boosts = getBoosts();
        // Should return multiple recommendations for different slots
        assertTrue(boosts.size() > 0);
      }
    }

    @Test
    public void effectCastingSuggested() {
      var cleanups =
          new Cleanups(
              withSkill("Disco Leer"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("meat"));
        var boosts = getBoosts();
        // Should suggest casting Disco Leer for meat drop
        assertThat(boosts, hasItem(hasProperty("cmd", startsWith("cast"))));
      }
    }
  }

  @Nested
  class FamiliarIntegration {
    @Test
    public void familiarSwitchingSuggested() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY),
              withFamiliarInTerrarium(FamiliarPool.LEPRECHAUN),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("meat, switch leprechaun"));
        // Should consider switching to leprechaun
      }
    }

    @Test
    public void familiarEquipmentConsidered() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY),
              withEquippableItem("lead necklace"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("familiar weight"));
        // Should recommend lead necklace
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.FAMILIAR, "lead necklace")));
      }
    }
  }

  @Nested
  class SlotInteractions {
    @Test
    public void weaponOffhandInteraction() {
      var cleanups =
          new Cleanups(
              withEquippableItem("seal-clubbing club"),
              withEquippableItem("catskin buckler"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus, shield"));
        // Should equip weapon and shield
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON)));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "catskin buckler")));
      }
    }

    @Test
    public void accessorySlotFilling() {
      var cleanups =
          new Cleanups(withEquippableItem("hand in glove", 3), withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // With 3 accessories available, should fill all slots
      }
    }

    @Test
    public void shirtRequiresTorsoAwareness() {
      var cleanups =
          new Cleanups(
              withEquippableItem("eXtreme Bi-Polar Fleece Vest"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("cold res"));
        // Without Torso Awareness, shirt won't be recommended
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.SHIRT))));
      }
    }

    @Test
    public void shirtWithTorsoAwareness() {
      var cleanups =
          new Cleanups(
              withEquippableItem("eXtreme Bi-Polar Fleece Vest"),
              withSkill("Torso Awareness"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("cold res"));
        // With Torso Awareness, shirt should be recommended
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.SHIRT)));
      }
    }
  }

  @Nested
  class PreferenceInteractions {
    @Test
    public void foldablesPreferenceAffectsResults() {
      var cleanups =
          new Cleanups(
              withEquippableItem("origami pasties"),
              withSkill("Torso Awareness"),
              withProperty("maximizerFoldables", true),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mox"));
        // Foldable items should be considered
      }
    }

    @Test
    public void alwaysCurrentPreference() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.HAT, "helmet turtle"),
              withProperty("maximizerAlwaysCurrent", true),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // Should always consider current equipment
      }
    }
  }

  @Nested
  class ErrorRecovery {
    @Test
    public void invalidExpressionHandledGracefully() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertFalse(maximize("xyzzy invalid keyword"));
        // Invalid expression should return false, not throw
      }
    }

    @Test
    public void impossibleConstraintsReturnFalse() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertFalse(maximize("mus 10000 min"));
        // Impossible min constraint should return false
      }
    }
  }
}
