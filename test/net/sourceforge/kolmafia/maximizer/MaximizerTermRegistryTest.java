package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withProperty;
import static net.sourceforge.kolmafia.maximizer.MaximizerTermRegistry.IntegerSetting.BEEOSITY;
import static net.sourceforge.kolmafia.maximizer.MaximizerTermRegistry.IntegerSetting.STINKYCHEESE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MaximizerTermRegistryTest {
  private static MaximizerTermRegistry parse(String expression) {
    var terms = new MaximizerTermRegistry();
    MaximizerExpressionParser.parse(expression, terms);
    return terms;
  }

  private static Evaluator.ScoreTerm scoreTerm(
      MaximizerTermRegistry terms, DoubleModifier modifier) {
    return terms.scoreTerms().stream()
        .filter(term -> term.modifier() == modifier)
        .findFirst()
        .orElseThrow(() -> new AssertionError("No score term for " + modifier));
  }

  @Test
  void appliesAggregateTerms() {
    var evaluator = new Evaluator("2 all resistance, -tie");
    var modifiers = new Modifiers();
    modifiers.setDouble(DoubleModifier.COLD_RESISTANCE, 1);
    modifiers.setDouble(DoubleModifier.HOT_RESISTANCE, 2);
    modifiers.setDouble(DoubleModifier.SLEAZE_RESISTANCE, 3);
    modifiers.setDouble(DoubleModifier.SPOOKY_RESISTANCE, 4);
    modifiers.setDouble(DoubleModifier.STENCH_RESISTANCE, 5);

    assertThat(evaluator.evaluate(modifiers).score(), is(30.0));
  }

  @Test
  void appliesPatternAbbreviations() {
    var evaluator = new Evaluator("3 muscle exp perc, -tie");
    var modifiers = new Modifiers();
    modifiers.setDouble(DoubleModifier.MUS_EXPERIENCE_PCT, 4);

    assertThat(evaluator.evaluate(modifiers).score(), is(12.0));
  }

  @Test
  void normalizesGenericAbbreviations() {
    assertThat(MaximizerTermRegistry.normalize("cold res"), is("cold resistance"));
    assertThat(MaximizerTermRegistry.normalize("weapon dmg percent"), is("weapon damage percent"));
    assertThat(MaximizerTermRegistry.normalize("mus exp"), is("mus experience"));
    assertThat(MaximizerTermRegistry.normalize("organ"), is("organ capacity"));
  }

  @Test
  void acceptsQuotedTermsAndRejectsMalformedSyntax() {
    assertThat(scoreTerm(parse("\"item drop\""), DoubleModifier.ITEMDROP).weight(), is(1.0));

    try (var cleanups = withContinuationState()) {
      MaximizerExpressionParser.parse("\"unterminated", new MaximizerTermRegistry());

      assertThat(KoLmafia.permitsContinue(), is(false));
    }
  }

  @Nested
  class TermSideEffects {
    @Test
    void emptySelectsSlotsByTheirCurrentOccupancy() {
      try (var cleanups = withEquipped(Slot.HAT, "helmet turtle")) {
        var empty = parse("empty");
        var occupied = parse("-empty");

        assertThat(empty.slots().get(Slot.HAT), is(-1));
        assertThat(empty.slots().get(Slot.PANTS), is(1));
        assertThat(occupied.slots().get(Slot.HAT), is(1));
        assertThat(occupied.slots().get(Slot.PANTS), is(-1));
      }
    }

    @Test
    void adventuresIgnoresBeesButKeepsTheTiebreaker() {
      var terms = parse("adv");

      assertThat(terms.integer(BEEOSITY), is(999));
      assertThat(terms.usesTiebreaker(), is(true));
    }

    @Test
    void randomMonsterModifiersIgnoresBeesAndDropsTheTiebreaker() {
      var terms = parse("ocrs");

      assertThat(terms.integer(BEEOSITY), is(999));
      assertThat(terms.usesTiebreaker(), is(false));
    }

    @Test
    void zeroWeightKeepsTheTiebreaker() {
      assertThat(parse("0 tie").usesTiebreaker(), is(true));
    }

    @Test
    void anExplicitBeeosityOutranksItsDefault() {
      var terms = parse("5 beeosity");

      assertThat(terms.integer(BEEOSITY), is(5));
    }
  }

  @Nested
  class Aliases {
    @Test
    void aliasedDirectivesShareOneAction() {
      assertThat(parse("3 stinky cheese").integer(STINKYCHEESE), is(3));
      assertThat(parse("3 stinkycheese").integer(STINKYCHEESE), is(3));
    }
  }

  @Nested
  class MinimumsAndMaximums {
    @Test
    void applyToWhicheverModifierTheExpressionLastMentioned() {
      var terms = parse("0.1 da 1000 max");

      assertThat(scoreTerm(terms, DoubleModifier.DAMAGE_ABSORPTION).max(), is(1000.0));
      assertThat(terms.totalMax(), is(Double.POSITIVE_INFINITY));
    }

    @Test
    void outliveTheTermThatNamedTheModifier() {
      var terms = parse("0.1 da, 1000 max, 20 min");

      var damageAbsorption = scoreTerm(terms, DoubleModifier.DAMAGE_ABSORPTION);
      assertThat(damageAbsorption.min(), is(20.0));
      assertThat(damageAbsorption.max(), is(1000.0));
    }

    @Test
    void applyToTheWholeScoreAfterAnAggregateTerm() {
      var terms = parse("1 all resistance, 5 max, -100 min");

      assertThat(terms.totalMax(), is(5.0));
      assertThat(terms.totalMin(), is(-100.0));
      assertThat(
          scoreTerm(terms, DoubleModifier.COLD_RESISTANCE).max(), is(Double.POSITIVE_INFINITY));
    }

    @Test
    void applyToTheWholeScoreWhenNoModifierHasBeenMentioned() {
      var terms = parse("100 max");

      assertThat(terms.totalMax(), is(100.0));
    }
  }

  @Nested
  class TiebreakerInheritance {
    @Test
    void anExpressionStartsFromTheMaximumsOfItsTiebreaker() {
      var tiebreaker = parse("0.1 da 1000 max");
      var terms = new MaximizerTermRegistry(tiebreaker);
      MaximizerExpressionParser.parse("1 da", terms);

      var damageAbsorption = scoreTerm(terms, DoubleModifier.DAMAGE_ABSORPTION);
      assertThat(damageAbsorption.weight(), is(1.0));
      assertThat(damageAbsorption.max(), is(1000.0));
    }

    @Test
    void anExpressionWithoutATiebreakerIsUnbounded() {
      var terms = parse("1 da");

      assertThat(
          scoreTerm(terms, DoubleModifier.DAMAGE_ABSORPTION).max(), is(Double.POSITIVE_INFINITY));
    }
  }

  @Nested
  class BonusFunctions {
    @Test
    void handleNullItems() {
      assertThat(MaximizerTermRegistry.letterBonus(null), is(0.0));
      assertThat(MaximizerTermRegistry.letterBonus(null, "X"), is(0.0));
      assertThat(MaximizerTermRegistry.numberBonus(null), is(0.0));
    }

    @Test
    void countLettersInItemNames() {
      AdventureResult item = AdventureResult.tallyItem("spiked femur");

      assertThat(MaximizerTermRegistry.letterBonus(item), is(12.0));
      assertThat(MaximizerTermRegistry.letterBonus(item, "e"), is(2.0));
    }

    @Test
    void ignoreItemModes() {
      try (var cleanups = withProperty("backupCameraMode", "meat")) {
        AdventureResult item = AdventureResult.tallyItem("backup camera");

        assertThat(MaximizerTermRegistry.letterBonus(item), is(13.0));
      }
    }

    @Test
    void countNumbersInItemNames() {
      AdventureResult item = AdventureResult.tallyItem("X-37 gun");

      assertThat(MaximizerTermRegistry.numberBonus(item), is(2.0));
    }
  }
}
