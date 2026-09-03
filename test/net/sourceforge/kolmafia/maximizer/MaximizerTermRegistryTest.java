package net.sourceforge.kolmafia.maximizer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import org.junit.jupiter.api.Test;

class MaximizerTermRegistryTest {
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
  void recordsTermSideEffectsWithItsDefinition() {
    var adventures = MaximizerTermRegistry.find("adv");
    var ocrs = MaximizerTermRegistry.find("ocrs");

    assertThat(adventures.disablesBeeosity(), is(true));
    assertThat(ocrs.disablesBeeosity(), is(true));
    assertThat(ocrs.disablesTiebreaker(), is(true));
  }

  @Test
  void normalizesGenericAbbreviations() {
    assertThat(MaximizerTermRegistry.normalize("cold res"), is("cold resistance"));
    assertThat(MaximizerTermRegistry.normalize("weapon dmg percent"), is("weapon damage percent"));
    assertThat(MaximizerTermRegistry.normalize("mus exp"), is("mus experience"));
    assertThat(MaximizerTermRegistry.normalize("organ"), is("organ capacity"));
  }
}
