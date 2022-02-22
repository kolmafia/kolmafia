package net.sourceforge.kolmafia.utilities;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
  private static final String HTML_PREFIX = "<html>";

  @Test
  public void itShouldDoNothingForVariousSimpleInputs() {
    int testLength = 10;
    assertNull(StringUtilities.basicTextWrap(null), "Null should neither wrap nor error.");
    String test = buildTestString(testLength);
    assertEquals(StringUtilities.basicTextWrap(test), test, "Short input should remain unchanged.");
    test = HTML_PREFIX + test;
    assertEquals(
        test, StringUtilities.basicTextWrap(test), "Short html input should remain unchanged.");
    testLength = 123;
    test = buildTestString(testLength);
    test = HTML_PREFIX + test;
    assertEquals(
        test, StringUtilities.basicTextWrap(test), "Long html input should remain unchanged.");
  }

  @Test
  public void itShouldWrapForVariousSimpleInputs() {
    int testLength = 90;
    String testMe = buildTestString(testLength);
    String expected = manuallyInsertBreak(testMe, 80);
    assertEquals(expected, StringUtilities.basicTextWrap(testMe), "Break not where expected.");
    testLength = 200;
    testMe = buildTestString(testLength);
    expected = manuallyInsertBreak(testMe, 80);
    expected = manuallyInsertBreak(expected, 161);
    assertEquals(expected, StringUtilities.basicTextWrap(testMe), "Breaks not where expected.");
  }

  @Test
  public void itShouldHandleUserInsertedBreaks() {
    String testMe = buildTestString(159);
    testMe = manuallyInsertBreak(testMe, 40);
    String expected = manuallyInsertBreak(testMe, 80);
    expected = expected + BREAK;
    assertEquals(expected, StringUtilities.basicTextWrap(testMe), "Breaks not where expected.");
  }

  @ParameterizedTest
  @CsvSource({
    "'1234', '1234'",
    "'12345', '12345\n'",
    "'1234567890', '12345\n67890\n'",
    "'123456789012', '12345\n67890\n12'",
    "'123\n4567890123456789', '123\n4\n56789\n01234\n56789\n'",
    "'12 45', '12\n45'",
    "'1 3 5 7 9 1', '1 3 5\n7 9\n1'",
    "'1 2  \n  4 5\n 6 \n', '1 2\n4 5\n6'",
    "'1234 67', '1234\n67'",
  })
  public void exerciseTextWrapWithShortWrapLength(String input, String expected) {
    assertEquals(expected, StringUtilities.basicTextWrap(input, 5));
  }

  /*
  This builds a string of the specified length.  It uses the repeating pattern "1234567891" to
  make it easier for humans to debug.  The modulo 10 is just to get a single digit as a string;
   */
  private String buildTestString(int length) {
    StringBuilder retVal = new StringBuilder();
    for (int i = 1; i <= length; i++) {
      retVal.append(i % 10);
    }
    return retVal.toString();
  }
  /*
  This inserts a break into a string
   */
  private String manuallyInsertBreak(String input, int breakPos) {
    if (breakPos >= input.length()) {
      return input;
    } else {
      return input.subSequence(0, breakPos) + BREAK + input.subSequence(breakPos, input.length());
    }
  }
  // End tests for basicTestWrap

  @ParameterizedTest
  @CsvSource({
    "'not an id', '-1'",
    "'[not an id]', '-1'",
    "'123','-1'",
    "'[123', '-1'",
    "'123[', '-1'",
    "'123', '-1'",
    "'[1337]2468 googles', '1337'",
    "'[123]unreal item', '123'"
  })
  public void itShouldExerciseGetBracketedID(String name, String id) {
    assertEquals(Integer.parseInt(id), StringUtilities.getBracketedId(name));
  }

  @ParameterizedTest
  @CsvSource({
    "'not an id', 'not an id'",
    "'[not an id]', '[not an id]'",
    "'[123', '[123'",
    "'123]', '123]'",
    "'123', '123'",
    "'[123]unreal item', 'unreal item'",
    "'[1337]2468 goggles', '2468 goggles'",
  })
  public void itShouldExerciseRemoveBracketedID(String name, String id) {
    assertEquals(id, StringUtilities.removeBracketedId(name));
  }

  @Test
  public void itShouldTokenizeStringsWithDefaults() {
    // simple comma separated list
    String testString = "folder (cyan), folder (magenta), folder (yellow)";
    List<String> expected = new ArrayList<>();
    expected.add("folder (cyan)");
    expected.add("folder (magenta)");
    expected.add("folder (yellow)");
    try {
      List<String> result = StringUtilities.tokenizeString(testString);
      assertEquals(expected, result, "Lists are not the same.");
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void itShouldHandleGoodAndBadEscape() {
    String testString = "a,\tb";
    List<String> expected = new ArrayList<>();
    expected.add("a");
    expected.add("\tb");
    try {
      List<String> result = StringUtilities.tokenizeString(testString, ',', '\\', false);
      assertEquals(expected, result, "Lists are not the same.");
    } catch (Exception e) {
      fail(e.getMessage());
    }
    char[] test = new char[4];
    test[0] = 'a';
    test[1] = ',';
    test[2] = '\\';
    test[3] = 'g';
    testString = String.valueOf(test);
    expected.clear();
    expected.add("a");
    expected.add("g");
    try {
      List<String> result = StringUtilities.tokenizeString(testString);
      assertEquals(expected, result, "Lists are not the same.");
    } catch (Exception e) {
      fail(e.getMessage());
    }
    test[2] = 'g';
    test[3] = '\\';
    testString = String.valueOf(test);
    expected.clear();
    expected.add("a");
    expected.add("g");
    try {
      List<String> result = StringUtilities.tokenizeString(testString);
      fail("Expected exception not thrown.");
    } catch (Exception e) {
      assertEquals("Invalid terminal escape", e.getMessage(), "Wrong exception.");
    }
  }

  @Test
  public void itShouldHandleParameterVariations() {
    String testString = "first, second\\, and third";
    List<String> expected = new ArrayList<>();
    expected.add("first");
    expected.add("second, and third");
    try {
      List<String> result = StringUtilities.tokenizeString(testString);
      assertEquals(expected, result, "Lists are not the same.");
    } catch (Exception e) {
      fail(e.getMessage());
    }
    testString = "first : second, and third";
    try {
      List<String> result = StringUtilities.tokenizeString(testString, ':');
      assertEquals(expected, result, "Lists are not the same.");
    } catch (Exception e) {
      fail(e.getMessage());
    }
    expected.clear();
    expected.add("first ");
    expected.add(" second, and third");
    try {
      List<String> result = StringUtilities.tokenizeString(testString, ':', '\\', false);
      assertEquals(expected, result, "Lists are not the same.");
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @ParameterizedTest
  @CsvSource({
    "'not a title', 'Not A Title'",
    "'nOt a tItLe', 'Not A Title'",
    "'123NotmE yet', '123Notme Yet'",
    "'123NotmE     yet     ', '123Notme     Yet     '"
  })
  public void itShouldConvertToTitleCase(String input, String expected) {
    assertEquals(expected, StringUtilities.toTitleCase(input));
  }
}
