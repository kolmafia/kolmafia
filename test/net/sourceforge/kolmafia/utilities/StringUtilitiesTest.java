package net.sourceforge.kolmafia.utilities;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StringUtilitiesTest {
  @Test
  void isVowel() {
    assertTrue(StringUtilities.isVowel('a'));
    assertTrue(StringUtilities.isVowel('e'));
    assertTrue(StringUtilities.isVowel('i'));
    assertTrue(StringUtilities.isVowel('o'));
    assertTrue(StringUtilities.isVowel('u'));
    assertFalse(StringUtilities.isVowel('y'));
    assertFalse(StringUtilities.isVowel('x'));
    assertFalse(StringUtilities.isVowel('p'));
  }

  // Tests for basicTestWrap

  private static final String BREAK = "\n";

  @Test
  public void itShouldDoNothingForVariousInputs() {
    // null
    assertNull(StringUtilities.basicTextWrap(null), "Null should neither wrap nor error.");
    // short
    String test = "1234567890";
    assertEquals(StringUtilities.basicTextWrap(test), test, "Short input should remain unchanged.");
    // html
    test = "<html> " + test;
    assertEquals(StringUtilities.basicTextWrap(test), test, "Short input should remain unchanged.");
  }

  @Test
  public void itShouldWrapForVariousInputs() {
    int testLength = 90;
    String testMe = buildTestString(testLength);
    assertEquals(testLength, testMe.length(), "Incorrectly constructed test string.");
    String expected = manuallyInsertBreak(testMe, 80);
    assertEquals(StringUtilities.basicTextWrap(testMe), expected, "Break not where expected.");
  }

  /*
  This builds a string of the specified length.  It uses the repeating pattern "0123456789" to
  make it easier for humans to debug.  The modulo 10 is just to get a single digit as a string;
   */
  private String buildTestString(int length) {
    StringBuilder retVal = new StringBuilder();
    for (int i = 0; i < length; i++) {
      retVal.append(i % 10);
    }
    return retVal.toString();
  }

  private String manuallyInsertBreak(String input, int breakPos) {
    return input.subSequence(0, breakPos) + BREAK + input.subSequence(breakPos, input.length());
  }
}
