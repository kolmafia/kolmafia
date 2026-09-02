package net.sourceforge.kolmafia.maximizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EvaluatorTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("EvaluatorTest");
  }

  @Test
  void calculatesSharedScoreValues() {
    var modifiers = new Modifiers();
    modifiers.setDouble(DoubleModifier.FAMILIAR_WEIGHT, 10.0);
    modifiers.setDouble(DoubleModifier.HIDDEN_FAMILIAR_WEIGHT, 4.0);
    modifiers.setDouble(DoubleModifier.FAMILIAR_WEIGHT_PCT, -1.0);
    modifiers.setDouble(DoubleModifier.INITIATIVE, 20.0);
    modifiers.setDouble(DoubleModifier.INITIATIVE_PENALTY, -5.0);
    modifiers.setDouble(DoubleModifier.MANA_COST, -3.0);
    modifiers.setDouble(DoubleModifier.STACKABLE_MANA_COST, -2.0);
    modifiers.setDouble(DoubleModifier.ITEMDROP, 25.0);
    modifiers.setDouble(DoubleModifier.ITEMDROP_PENALTY, -10.0);
    modifiers.setDouble(DoubleModifier.SPORADIC_ITEMDROP, 5.0);
    modifiers.setDouble(DoubleModifier.MEATDROP, 30.0);
    modifiers.setDouble(DoubleModifier.MEATDROP_PENALTY, -5.0);
    modifiers.setDouble(DoubleModifier.SPORADIC_MEATDROP, 10.0);
    modifiers.setDouble(DoubleModifier.MEAT_BONUS, 10000.0);
    modifiers.setDouble(DoubleModifier.WEAPON_DAMAGE, 4.0);
    modifiers.setDouble(DoubleModifier.WEAPON_DAMAGE_PCT, 6.0);
    modifiers.setDouble(DoubleModifier.DAMAGE_AURA, 7.0);
    modifiers.setDouble(DoubleModifier.SPORADIC_DAMAGE_AURA, 3.0);
    modifiers.setDouble(DoubleModifier.COLD_RESISTANCE, 8.0);
    modifiers.setBoolean(BooleanModifier.COLD_VULNERABILITY, true);

    var expected =
        Map.ofEntries(
            Map.entry(DoubleModifier.FAMILIAR_WEIGHT, 7.0),
            Map.entry(DoubleModifier.INITIATIVE, 15.0),
            Map.entry(DoubleModifier.MANA_COST, -5.0),
            Map.entry(DoubleModifier.ITEMDROP, 120.0),
            Map.entry(DoubleModifier.MEATDROP, 136.0),
            Map.entry(DoubleModifier.WEAPON_DAMAGE, 10.0),
            Map.entry(DoubleModifier.DAMAGE_AURA, 10.0),
            Map.entry(DoubleModifier.COLD_RESISTANCE, -92.0));

    var predicted = modifiers.predict();
    for (var entry : expected.entrySet()) {
      assertEquals(
          entry.getValue(),
          Evaluator.scoreValue(entry.getKey(), modifiers, predicted),
          0.0001,
          entry.getKey().getName());
    }
  }
}
