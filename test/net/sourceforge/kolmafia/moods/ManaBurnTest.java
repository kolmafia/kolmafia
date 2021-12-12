package net.sourceforge.kolmafia.moods;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ManaBurnTest {

  static ManaBurn testMe;

  @Test
  public void durationShouldLimitCast() {
    testMe = new ManaBurn(3010, "Leash of Linguini", 100, 0);
    assertFalse(testMe.isCastable(10), "Duration exceeds limit.");
  }

  @Test
  public void manaShouldLimitCast() {
    testMe = new ManaBurn(3010, "Leash of Linguini", 0, 10);
    assertFalse(testMe.isCastable(1), "MP cost exceeds allowed MP.");
  }

  @Test
  public void itCanBeCast() {
    testMe = new ManaBurn(3010, "Leash of Linguini", 0, 10);
    assertTrue(testMe.isCastable(1000), "MP and duration should allow cast.");
  }

  @Test
  public void itCanSimulateCast() {
    testMe = new ManaBurn(3010, "Leash of Linguini", 5, 10);
    assertTrue(testMe.isCastable(1000));
    assertEquals(12, testMe.simulateCast(), "Simulated MP cost incorrect.");
  }

  @Test
  public void itShouldHaveAName() {
    testMe = new ManaBurn(3010, "Leash of Linguini", 5, 10);
    for (int i = 0; i < 5; i++) {
      testMe.simulateCast();
    }
    assertEquals("cast 5 Leash of Linguini", testMe.toString(), "Unexpected toString value");
  }

  @Test
  public void itShouldCompareDuration() {
    ManaBurn d1 = new ManaBurn(3010, "Leash of Linguini", 1, 10);
    ManaBurn d5 = new ManaBurn(3010, "Leash of Linguini", 5, 10);
    assertEquals(0, d1.compareTo(d1), "Equal compareTo");
    assertEquals(-4, d1.compareTo(d5), "Unequal compareTo");
    assertEquals(4, d5.compareTo(d1), "Unequal compareTo");
  }
}
