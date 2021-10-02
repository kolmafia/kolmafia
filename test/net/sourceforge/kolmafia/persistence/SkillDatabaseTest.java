package net.sourceforge.kolmafia.persistence;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

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
}
