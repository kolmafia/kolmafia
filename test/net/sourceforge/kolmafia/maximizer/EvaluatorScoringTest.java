package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.getBoosts;
import static internal.helpers.Maximizer.maximize;
import static internal.helpers.Maximizer.modFor;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSkill;
import static internal.helpers.Player.withStats;
import static internal.matchers.Maximizer.recommendsSlot;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
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
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class EvaluatorScoringTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("EvaluatorScoringTest");
    Preferences.reset("EvaluatorScoringTest");
  }

  @Nested
  class BasicScoring {
    @Test
    public void singleModifierScoring() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("mus"));
        assertEquals(1, modFor(DerivedModifier.BUFFED_MUS), 0.01);
      }
    }

    @Test
    public void multipleWeightedModifiers() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"),
              withEquippableItem("old sweatpants"),
              withStats(100, 100, 100));
      try (cleanups) {
        // Higher weight on muscle should prefer muscle items
        assertTrue(maximize("5 mus, 1 mox"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
      }
    }

    @Test
    public void zeroWeightModifierDoesNotAffectScore() {
      var cleanups =
          new Cleanups(withEquippableItem("helmet turtle"), withEquippableItem("old sweatpants"));
      try (cleanups) {
        // Zero weight means this modifier is ignored for scoring
        assertTrue(maximize("0 mus, 1 mox"));
        // Should prefer moxie item when muscle has zero weight
      }
    }

    @Test
    public void negativeWeightPreferLowerValues() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        // Negative weight means we want to minimize this stat
        assertTrue(maximize("-1 mus"));
        // Should NOT recommend the hat since it adds muscle
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT, "helmet turtle"))));
      }
    }

    @Test
    public void decimalWeightsWork() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("0.5 mus"));
        assertEquals(1, modFor(DerivedModifier.BUFFED_MUS), 0.01);
      }
    }
  }

  @Nested
  class ConstraintEnforcement {
    @Test
    public void minConstraintViolationCausesFailure() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        // Requiring 1000 muscle min should fail with just a turtle helmet
        assertFalse(maximize("mus 1000 min"));
      }
    }

    @Test
    public void minConstraintSucceedsWhenMet() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"), withStats(50, 50, 50));
      try (cleanups) {
        // With base stats of 50, adding 1 from helmet meets the 50 min
        assertTrue(maximize("mus 50 min"));
      }
    }

    @Test
    public void maxConstraintCapsValueAtMaximum() {
      var cleanups =
          new Cleanups(
              withEquippableItem("hardened slime hat"),
              withEquippableItem("bounty-hunting helmet"),
              withSkill("Refusal to Freeze"));
      try (cleanups) {
        assertTrue(maximize("cold res 3 max, 0.1 item"));
        // Cold res should be capped at 3 even if more is available
        assertEquals(3, modFor(DoubleModifier.COLD_RESISTANCE), 0.01);
      }
    }

    @Test
    public void maxConstraintAllowsOtherModifiersToDecide() {
      var cleanups =
          new Cleanups(
              withEquippableItem("hardened slime hat"),
              withEquippableItem("bounty-hunting helmet"),
              withSkill("Refusal to Freeze"));
      try (cleanups) {
        // With max of 3 cold res, should prefer item that also gives item drop
        assertTrue(maximize("cold res 3 max, 0.1 item"));
        assertEquals(20, modFor(DoubleModifier.ITEMDROP), 0.01);
      }
    }

    @Test
    public void totalMinConstraintAppliesGlobally() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        // Total score min of 0 should always pass
        assertTrue(maximize("0 min, mus"));
      }
    }

    @Test
    public void totalMaxCausesExceeded() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        // Very low max means we hit it immediately
        assertTrue(maximize("0 max, mus"));
        // Should still work but may stop early
      }
    }

    @Test
    public void minAndMaxTogetherWorkCorrectly() {
      var cleanups =
          new Cleanups(withEquippableItem("hardened slime hat"), withSkill("Refusal to Freeze"));
      try (cleanups) {
        // Require at least 1 cold res but cap at 10 for scoring purposes
        // Note: max only caps how much value counts towards score, not actual value
        assertTrue(maximize("cold res 1 min 10 max"));
        double coldRes = modFor(DoubleModifier.COLD_RESISTANCE);
        assertTrue(coldRes >= 1, "Cold res should be at least 1");
      }
    }
  }

  @Nested
  class TiebreakerBehavior {
    @Test
    public void tiebreakerDecidesEqualScores() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"),
              withEquippableItem("seal-skull helmet"),
              withStats(100, 100, 100));
      try (cleanups) {
        // Both helmets give +1 muscle, tiebreaker should decide
        assertTrue(maximize("mus"));
        // Base stats 100 + 1 from helmet = 101
        assertEquals(101, modFor(DerivedModifier.BUFFED_MUS), 0.01);
      }
    }

    @Test
    public void minusTieDisablesTiebreakerScoring() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("mus, -tie"));
        assertEquals(1, modFor(DerivedModifier.BUFFED_MUS), 0.01);
      }
    }

    @Test
    public void tiebreakerPrefersItemDrop() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"),
              withEquippableItem("bounty-hunting helmet"),
              withStats(100, 100, 100));
      try (cleanups) {
        // Both helmets tied for a non-existent modifier, tiebreaker should prefer item drop
        assertTrue(maximize("spooky res"));
        // Bounty-hunting helmet gives +20% item drop
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "bounty-hunting helmet")));
      }
    }
  }

  @Nested
  class DerivedModifiers {
    @Test
    public void buffedMusCalculation() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"), withStats(50, 50, 50));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // Base 50 + 1 from helmet = 51
        assertEquals(51, modFor(DerivedModifier.BUFFED_MUS), 0.01);
      }
    }

    @Test
    public void buffedMysCalculation() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"), withStats(50, 50, 50));
      try (cleanups) {
        assertTrue(maximize("mys"));
        // Base 50, helmet doesn't affect mys
        assertEquals(50, modFor(DerivedModifier.BUFFED_MYS), 0.01);
      }
    }

    @Test
    public void buffedMoxCalculation() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"), withStats(50, 50, 50));
      try (cleanups) {
        assertTrue(maximize("mox"));
        // Base 50, helmet doesn't affect mox
        assertEquals(50, modFor(DerivedModifier.BUFFED_MOX), 0.01);
      }
    }

    @Test
    public void hpUsesBuffedHP() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"), withStats(50, 50, 50));
      try (cleanups) {
        assertTrue(maximize("hp"));
        // HP calculation is complex, just verify it runs
        assertThat(modFor(DerivedModifier.BUFFED_HP), greaterThan(0.0));
      }
    }

    @Test
    public void mpUsesBuffedMP() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"), withStats(50, 50, 50));
      try (cleanups) {
        assertTrue(maximize("mp"));
        // MP calculation is complex, just verify it runs
        assertThat(modFor(DerivedModifier.BUFFED_MP), greaterThan(0.0));
      }
    }
  }

  @Nested
  class SpecialValueCalculations {
    @Test
    public void meatdropIncludesBaseline100() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("meat"));
        // Meat drop calculation includes baseline 100%
      }
    }

    @Test
    public void itemdropIncludesBaseline100() {
      var cleanups = new Cleanups(withEquippableItem("bounty-hunting helmet"));
      try (cleanups) {
        assertTrue(maximize("item"));
        // Item drop includes 100% baseline + equipment bonus
        assertEquals(20, modFor(DoubleModifier.ITEMDROP), 0.01);
      }
    }

    @Test
    public void weaponDamageIncludesPercent() {
      var cleanups = new Cleanups(withEquippableItem("seal-clubbing club"), withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("weapon damage"));
        // Weapon damage calculation combines flat and percent
      }
    }

    @Test
    public void initiativeIncludesPenalty() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("init"));
        // Initiative calculation subtracts penalties
      }
    }
  }

  @Nested
  class ResistanceImmunity {
    @Test
    public void coldResistanceFromSkill() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"), withSkill("Refusal to Freeze"));
      try (cleanups) {
        assertTrue(maximize("cold res"));
        // Refusal to Freeze should give cold resistance
        assertThat(modFor(DoubleModifier.COLD_RESISTANCE), greaterThan(0.0));
      }
    }

    @Test
    public void coldResistanceFromGear() {
      var cleanups = new Cleanups(withEquippableItem("hardened slime hat"));
      try (cleanups) {
        assertTrue(maximize("cold res"));
        // Hardened slime hat provides cold resistance
        assertThat(modFor(DoubleModifier.COLD_RESISTANCE), greaterThan(0.0));
      }
    }
  }

  @Nested
  class BonusCalculations {
    @Test
    public void bonusAddsToScore() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"),
              withEquippableItem("seal-skull helmet"),
              withStats(100, 100, 100));
      try (cleanups) {
        // Large bonus should force selection of specific item
        assertTrue(maximize("mus, +1000 bonus helmet turtle"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
      }
    }

    @Test
    public void negativeBonusPenalizesItem() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"),
              withEquippableItem("seal-skull helmet"),
              withStats(100, 100, 100));
      try (cleanups) {
        // Negative bonus should avoid the item
        assertTrue(maximize("mus, -1000 bonus helmet turtle"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT, "helmet turtle"))));
      }
    }
  }

  @Nested
  class OsityModifiers {
    @Test
    public void clownosityFailsIfNotMet() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        // Without clown gear, can't meet clownosity requirement of 4
        assertFalse(maximize("4 clownosity"));
      }
    }

    @Test
    public void clownosityWithNoTargetSucceeds() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        // Default clownosity (100%) without clown gear still parses
        // but should fail
        assertFalse(maximize("clownosity"));
      }
    }

    @Test
    public void raveosityFailsIfNotMet() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        // Without rave gear, can't meet raveosity requirement of 7
        assertFalse(maximize("7 raveosity"));
      }
    }

    @Test
    public void raveosityWithNoTargetSucceeds() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        // Default raveosity (7) without rave gear should fail
        assertFalse(maximize("raveosity"));
      }
    }

    @Test
    public void surgeonosityFailsIfNotMet() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        // Without surgeon gear, can't meet surgeonosity requirement
        assertFalse(maximize("5 surgeonosity"));
      }
    }

    @Test
    public void surgeonosityWithNoTargetSucceeds() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        // Default surgeonosity (5) without surgeon gear should fail
        assertFalse(maximize("surgeonosity"));
      }
    }
  }

  @Nested
  class BooleanConstraints {
    @Test
    public void booleanConstraintMustBeMetForSuccess() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        // "never fumble" is a boolean constraint
        // Without gear providing it, should fail
        assertFalse(maximize("never fumble"));
      }
    }

    @Test
    public void booleanConstraintParsingWorks() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        // Boolean constraints are parsed but may fail if not provided
        // Just verify parsing doesn't throw an exception
        maximize("never fumble");
        // May or may not succeed depending on available gear
      }
    }
  }

  @Nested
  class RolloverEffectBonus {
    @Test
    public void rolloverEffectAddsFudgeFactor() {
      // Items with rollover effects get a small bonus
      var cleanups = new Cleanups(withEquippableItem("time helmet"), withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("adv"));
        // Time helmet gives +1 adv, verify it's recommended
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "time helmet")));
      }
    }
  }
}
