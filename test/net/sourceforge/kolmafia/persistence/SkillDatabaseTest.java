package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Player.withClass;
import static org.junit.jupiter.api.Assertions.*;

import net.sourceforge.kolmafia.AscensionClass;
import org.junit.jupiter.api.Test;

public class SkillDatabaseTest {
  // This is just one simple test to verify before and after behavior for an implicit narrowing cast
  // that was replaced by an alternative calculation.

  @Test
  public void itShouldCalculateCostCorrectlyAsAFunctionOfCasts() {
    assertEquals(SkillDatabase.stackLumpsCost(-10), 1);
    assertEquals(SkillDatabase.stackLumpsCost(0), 11);
    assertEquals(SkillDatabase.stackLumpsCost(1), 111);
    assertEquals(SkillDatabase.stackLumpsCost(2), 1111);
    assertEquals(SkillDatabase.stackLumpsCost(14), 1111111111111111L);
    // The previous calculation using Pow actually gives incorrect results for more than 14 casts
    // More than 17 casts overflows a long
    assertEquals(SkillDatabase.stackLumpsCost(15), 11111111111111111L);
    assertEquals(SkillDatabase.stackLumpsCost(17), 1111111111111111111L);
    assertEquals(SkillDatabase.stackLumpsCost(18), Long.MAX_VALUE);
  }

  @Test
  public void thrallsLastTenTurnsWhenNotPasta() {
    var cleanups = withClass(AscensionClass.ACCORDION_THIEF);
    try (cleanups) {
      // Bind Lasagmbie
      assertEquals(SkillDatabase.getEffectDuration(3037), 10);
    }
  }

  @Test
  public void thrallsLastZeroTurnsWhenPasta() {
    var cleanups = withClass(AscensionClass.PASTAMANCER);
    try (cleanups) {
      // Bind Lasagmbie
      assertEquals(SkillDatabase.getEffectDuration(3037), 0);
    }
  }
}
