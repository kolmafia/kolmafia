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
  @Test
  public void itShouldDoNothingForVariousInputs() {
    // null
    assertNull(StringUtilities.basicTextWrap(null), "Null should neither wrap nor error.");
    // short
    String test = "1234567890";
    assertEquals(StringUtilities.basicTextWrap(test), "Short input should remain unchanged.");
    // html
    test = "<html> " + test;
    assertEquals(StringUtilities.basicTextWrap(test), test, "Short input should remain unchanged.");
  }


}
