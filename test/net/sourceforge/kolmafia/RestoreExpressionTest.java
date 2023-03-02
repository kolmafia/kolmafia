package net.sourceforge.kolmafia;

import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withSkill;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

public class RestoreExpressionTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset(true);
    KoLCharacter.reset("Expression");
    Preferences.reset("Expression");
  }

  @ParameterizedTest
  @EnumSource(AscensionClass.class)
  public void canDetectClass(AscensionClass ascensionClass) {
    var cleanups = withClass(AscensionClass.ACCORDION_THIEF);

    try (cleanups) {
      double expected = ascensionClass == AscensionClass.ACCORDION_THIEF ? 1.0 : 0.0;

      var exp = new RestoreExpression("class(" + ascensionClass.toString() + ")", "Detect class");
      assertThat(ascensionClass.toString(), exp.eval(), is(expected));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "Confused, 1",
    "Embarrassed, 0",
    "3, 1",
    "4, 0",
  })
  public void canDetectEffect(String effect, String expected) {
    var cleanups = withEffect("Confused");

    try (cleanups) {
      var exp = new RestoreExpression("effect(" + effect + ")", "Detect effect");
      assertEquals(Double.parseDouble(expected), exp.eval());
    }
  }

  @ParameterizedTest
  @CsvSource({
    "Natural Born Scrabbler, 1",
    "Thrift and Grift, 0",
    "38, 1",
    "39, 0",
  })
  public void canDetectSkill(String skill, double expected) {
    var cleanups = withSkill("Natural Born Scrabbler");

    try (cleanups) {
      var exp = new RestoreExpression("skill(" + skill + ")", "Detect skill");
      assertEquals(expected, exp.eval());
    }
  }

  @ParameterizedTest
  @CsvSource({
    "seal-clubbing club, 1",
    "turtle totem, 0",
  })
  public void canDetectEquip(String item, double expected) {
    var cleanups = withEquipped(Slot.WEAPON, "seal-clubbing club");

    try (cleanups) {
      var exp = new RestoreExpression("equipped(" + item + ")", "Detect equip");
      assertEquals(expected, exp.eval());
    }
  }

  @ParameterizedTest
  @EnumSource(AscensionPath.Path.class)
  public void canDetectPath(AscensionPath.Path path) {
    var cleanups = withPath(AscensionPath.Path.YOU_ROBOT);

    try (cleanups) {
      double expected = path == AscensionPath.Path.YOU_ROBOT ? 1.0 : 0.0;

      var exp = new RestoreExpression("path(" + path.toString() + ")", "Detect class");
      assertThat(path.toString(), exp.eval(), is(expected));
    }
  }

  @Test
  public void canDetectMaximumHP() {
    KoLCharacter.setHP(10, 50, 40);

    var exp = new RestoreExpression("HP", "Maximum HP");
    assertThat(exp.eval(), is(50.0));
  }

  @Test
  public void canDetectMaximumMP() {
    KoLCharacter.setMP(20, 100, 80);

    var exp = new RestoreExpression("MP", "Maximum MP");
    assertThat(exp.eval(), is(100.0));
  }

  @Test
  public void canDetectCurrentHP() {
    KoLCharacter.setHP(139, 200, 10);

    var exp = new RestoreExpression("CURHP", "Current HP");
    assertThat(exp.eval(), is(139.0));
  }
}
