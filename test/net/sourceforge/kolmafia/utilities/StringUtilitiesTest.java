package net.sourceforge.kolmafia.utilities;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StringUtilitiesTest {

  @Test
  void formatsDateFitForLastModified() {
    var formatted = StringUtilities.formatDate(0L);
    assertEquals("Thu, 01 Jan 1970 00:00:00 GMT", formatted);
  }

  @Test
  void readsLastModifiedDate() {
    var parsed = StringUtilities.parseDate("Wed, 21 Oct 2015 07:28:00 GMT");
    var date = ZonedDateTime.of(2015, 10, 21, 7, 28, 0, 0, ZoneId.of("GMT"));
    assertEquals(Instant.from(date).toEpochMilli(), parsed);
  }

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
    "'1234 67', '1234\n67'"
  })
  public void exerciseTextWrapWithShortWrapLength(String input, String expected) {
    assertEquals(expected, StringUtilities.basicTextWrap(input, 5), input);
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
    assertEquals(Integer.parseInt(id), StringUtilities.getBracketedId(name), name);
  }

  @ParameterizedTest
  @CsvSource({
    "'not an id', 'not an id'",
    "'[not an id]', '[not an id]'",
    "'[123', '[123'",
    "'123]', '123]'",
    "'123', '123'",
    "'[123]unreal item', 'unreal item'",
    "'[1337]2468 goggles', '2468 goggles'"
  })
  public void itShouldParseIDsWithBrackets(String name, String id) {
    assertEquals(id, StringUtilities.removeBracketedId(name), name);
  }

  @Test
  public void itShouldTokenizeStringsWithDefaults() throws Exception {
    // simple comma separated list
    String testString = "folder (cyan), folder (magenta), folder (yellow)";
    List<String> expected = List.of("folder (cyan)", "folder (magenta)", "folder (yellow)");
    List<String> result = StringUtilities.tokenizeString(testString);
    assertEquals(expected, result, "Lists are not the same.");
  }

  @Test
  public void itShouldHandleStringWithTab() throws Exception {
    String testString = "a,\tb";
    List<String> expected = List.of("a", "\tb");
    List<String> result = StringUtilities.tokenizeString(testString, ',', '\\', false);
    assertEquals(expected, result, "Lists are not the same.");
  }

  @Test
  public void itShouldHandleStringWithEscapedLetter() throws Exception {
    String testString = "a,\\g";
    List<String> expected = List.of("a", "g");
    List<String> result = StringUtilities.tokenizeString(testString);
    assertEquals(expected, result, "Lists are not the same.");
  }

  @Test
  public void itShouldThrowOnInvalidEscape() {
    String testString = "a,g\\";
    Exception thrown =
        Assertions.assertThrows(Exception.class, () -> StringUtilities.tokenizeString(testString));
    assertEquals("Invalid terminal escape", thrown.getMessage(), "Wrong exception.");
  }

  @Test
  public void itShouldHandleStringWithEscapedComma() throws Exception {
    String testString = "first, second\\, and third";
    List<String> expected = List.of("first", "second, and third");
    List<String> result = StringUtilities.tokenizeString(testString);
    assertEquals(expected, result, "Lists are not the same.");
  }

  @Test
  public void itShouldHandleCustomSeparator() throws Exception {
    String testString = "first : second, and third";
    List<String> expected = List.of("first", "second, and third");
    List<String> result = StringUtilities.tokenizeString(testString, ':');
    assertEquals(expected, result, "Lists are not the same.");
  }

  @Test
  public void itShouldHandleNotTrimmingString() throws Exception {
    String testString = "first : second, and third";
    List<String> expected = List.of("first ", " second, and third");
    List<String> result = StringUtilities.tokenizeString(testString, ':', '\\', false);
    assertEquals(expected, result, "Lists are not the same.");
  }

  @ParameterizedTest
  @CsvSource({
    "'not a title', 'Not A Title'",
    "'nOt a tItLe', 'Not A Title'",
    "'123NotmE yet', '123Notme Yet'",
    "'123NotmE     yet     ', '123Notme     Yet     '",
    "'123 456', '123 456'"
  })
  public void itShouldConvertToTitleCase(String input, String expected) {
    assertEquals(expected, StringUtilities.toTitleCase(input), input);
  }

  @ParameterizedTest
  @CsvSource({
    "'No prepositions', 'No prepositions', 'No prepositions'",
    "'Afterwards, he walked behind her.', 'Afterwards, he walked @ her.', 'Afterwards, he walked beside her.'"
  })
  public void itShouldRegisterAndDecode(String original, String template, String variant) {
    // Register the original
    StringUtilities.registerPrepositions(original);
    // Fetch the registration map
    Map<String, String> prepMap = StringUtilities.getCopyOfPrepositionsMap();
    // Confirm it contains the original and the expected template.
    if (!original.equals(template)) {
      assertTrue(prepMap.containsKey(template), "Original not registered.");
      assertEquals(original, prepMap.get(template));
    }
    // Confirm a variant maps back to the original
    assertEquals(original, StringUtilities.lookupPrepositions(variant));
  }

  @Test
  public void itShouldHandleAnPrepositionalEdgeCase() {
    String original = "@ before @ after itself";
    String expectedTemplate = "@ @ @ @ itself";
    StringUtilities.registerPrepositions(original);
    Map<String, String> prepMap = StringUtilities.getCopyOfPrepositionsMap();
    assertTrue(prepMap.containsKey(expectedTemplate));
    String aTest = "@ behind @ within itself";
    assertEquals(original, StringUtilities.lookupPrepositions(aTest));
    String anotherTest = "over under sideways down itself";
    assertEquals(anotherTest, StringUtilities.lookupPrepositions(anotherTest));
    String oneMoreTest = "over under inside down itself";
    assertEquals(original, StringUtilities.lookupPrepositions(oneMoreTest));
  }

  @Test
  public void itShouldHandleAnUnregisteredEdgeCase() {
    StringUtilities.unregisterPrepositions();
    String test = "over under sideways down itself";
    assertEquals(test, StringUtilities.lookupPrepositions(test));
  }

  @ParameterizedTest
  @CsvSource({
    "'+123,456', 123456",
    "'123,456', 123456",
    "'+123456', 123456",
    "'123456', 123456",
    "'+123, 456', 123456",
    "' , ', 0.0",
    "' a,bc ', 0.0"
  })
  public void itShouldExerciseSomeParseDoubleEdgeCases(String input, double expected) {
    assertEquals(expected, StringUtilities.parseDouble(input), input);
  }

  @ParameterizedTest
  @CsvSource({
    "'+123,456', 123456f",
    "'123,456', 123456f",
    "'+123456', 123456f",
    "'123456', 123456f",
    "'+123, 456', 123456f",
    "' , ', 0.0f",
    "' a,bc ', 0.0f"
  })
  public void itShouldExerciseSomeParseFloatEdgeCases(String input, float expected) {
    assertEquals(expected, StringUtilities.parseFloat(input), input);
  }

  @Test
  public void theyShouldParseNullAsZero() {
    assertEquals(0.0f, StringUtilities.parseFloat(null));
    assertEquals(0.0, StringUtilities.parseDouble(null));
  }

  @ParameterizedTest
  @CsvSource({
    "'',0",
    "'   ',0",
    "'12345', 12345",
    "'-12345', -12345",
    "'not a number', 0",
    "'not1a2 number', 12",
    "'9,223,372,036,854,775,807', 9223372036854775807",
    "'9,223,372,036,854,775,808', 0"
  })
  public void itShouldExerciseSomeLaxLongParsing(String input, long expected) {
    assertEquals(expected, StringUtilities.parseLongInternal2(input), input);
  }

  @ParameterizedTest
  @CsvSource({
    "'',0",
    "'   ',0",
    "'12345', 12345",
    "'-12345', -12345",
    "'not a number', 0",
    "'not1a2 number', 12",
    "'9,223,372,036,854,775,807', 9223372036854775807",
    "'9,223,372,036,854,775,808', 0",
    "'+', 0",
    "' +  , , , ', 0",
    "' + 12,345', 12345",
    "' +1,337k', 1337000",
    "' +1,337K', 1337000",
    "'1m', 1000000",
    "'1M', 1000000",
    "'1 m', 1",
    "'1 meg', 1",
    "'9,223,372,036,854,775,807k', -1000", // This is existing behavior but almost certainly wrong
    "'9,223,372,036,854,775,808k', 0"
  })
  public void itShouldExerciseSomeLongParsing(String input, long expected) {
    assertEquals(expected, StringUtilities.parseLongInternal1(input, false), input);
  }

  @ParameterizedTest
  @CsvSource({
    "'',0",
    "'   ',0",
    "'12345', 12345",
    "'-12345', -12345",
    "'not a number', 0",
    "'not1a2 number', 12",
    "'2,147,483,647', 2147483647",
    "'2147483648', 0",
    "'+', 0",
    "' +  , , , ', 0",
    "' + 12,345', 12345",
    "' +1,337k', 1337000",
    "' +1,337K', 1337000",
    "'1m', 1000000",
    "'1M', 1000000",
    "'1 m', 1",
    "'1 meg', 1",
    "'2147483647k', -1000", // This is existing behavior but almost certainly wrong
    "'2147483648k', 0"
  })
  public void itShouldExerciseSomeIntParsing(String input, long expected) {
    assertEquals(expected, StringUtilities.parseIntInternal1(input, false), input);
  }

  @Test
  public void exerciseParseLongWithNull() {
    assertEquals(0L, StringUtilities.parseLongInternal1(null, false));
    assertEquals(0L, StringUtilities.parseLongInternal2(null));
  }

  @Test
  public void itShouldThrowAnIntParsingExceptionWhenAllowedTo() {
    String test = "This is not a number but does appear in the exception message.";
    Exception thrown =
        Assertions.assertThrows(
            Exception.class, () -> StringUtilities.parseIntInternal1(test, true));
    assertEquals(test, thrown.getMessage(), "Wrong exception.");
  }

  @Test
  public void itShouldThrowAnLongParsingExceptionWhenAllowedTo() {
    String test = "This is not a number but does appear in the exception message.";
    Exception thrown =
        Assertions.assertThrows(
            Exception.class, () -> StringUtilities.parseLongInternal1(test, true));
    assertEquals(test, thrown.getMessage(), "Wrong exception.");
  }

  @ParameterizedTest
  @CsvSource({
    "'',0",
    "'   ',0",
    "'12345', 12345",
    "'-12345', -12345",
    "'not a number', 0",
    "'not1a2 number', 12",
    "'9,223,372,036,854,775,808', 0"
  })
  public void itShouldExerciseSomeLaxIntegerParsing(String input, long expected) {
    assertEquals(expected, StringUtilities.parseIntInternal2(input), input);
  }

  @ParameterizedTest
  @CsvSource({
    "'   ', false",
    "'+1', true",
    "'-1', true",
    "'.1', true",
    "'1.234', true",
    "'1.23a', false",
    "'1.2.3', false",
    "'1,234', true"
  })
  public void itShouldRecognizeFloats(String test, boolean expected) {
    assertEquals(expected, StringUtilities.isFloat(test), test);
  }

  @Test
  public void itShouldKnowNullIsNotAFloat() {
    assertFalse(StringUtilities.isFloat(null));
  }

  @Test
  public void itShouldKnowEmptyIsNotAFloat() {
    assertFalse(StringUtilities.isFloat(""));
  }

  @ParameterizedTest
  @CsvSource({
    "'1,234', true",
    "'+1,234', true",
    "'-1,234', true",
    "'1234', true",
    "'+1234', true",
    "'-1234', true",
    "'+not', false",
    "'-not', false",
    "'not', false"
  })
  public void itShouldRecognizeNumericStrings(String test, boolean expected) {
    assertEquals(expected, StringUtilities.isNumeric(test), test);
  }

  @ParameterizedTest
  @CsvSource({"'not*not', 'not*not'", "'string with blanks', 'string_with_blanks'"})
  public void itShouldReplaceInStrings(String test, String expected) {
    // For test, just replace space with underscore even though code supports more.
    // Note that the first parameter is returned with the changed string
    StringBuffer testBuffer = new StringBuffer();
    testBuffer.append(test);
    StringUtilities.globalStringReplace(testBuffer, " ", "_");
    assertEquals(expected, testBuffer.toString(), test);
  }

  @Test
  public void itShouldHandleInvalidParametersForStringReplacement() {
    StringBuffer input = null;
    StringUtilities.globalStringReplace(input, "", null);
    assertNull(input);
    input = new StringBuffer();
    input.append("test string");
    StringUtilities.globalStringReplace(input, "", null);
    assertEquals(input, input);
    StringUtilities.globalStringReplace(input, "", null);
    assertEquals(input, input);
    StringUtilities.globalStringReplace(input, " ", null);
    assertEquals("teststring", input.toString());
  }

  @Test
  public void itShouldReplaceWithAnInteger() {
    StringBuffer input = new StringBuffer();
    input.append("Substitute in me.");
    StringUtilities.globalStringReplace(input, "e", 3);
    assertEquals(input.toString(), "Substitut3 in m3.");
  }

  @ParameterizedTest
  @CsvSource({"'not*not', 'not*not'", "'string with blanks', 'string_with_blanks'"})
  public void itShouldReplaceInStringsWithDifferentSignature(String test, String expected) {
    // For test, just replace space with underscore even though code supports more.
    String result = StringUtilities.globalStringReplace(test, " ", "_");
    assertEquals(expected, result, test);
  }

  @Test
  public void itShouldRespondToUnexpectedInputs() {
    String arg = null;
    assertNull(StringUtilities.globalStringReplace(arg, " ", "_"));
    arg = "Something with characters.";
    assertEquals(arg, StringUtilities.globalStringReplace(arg, "", "_"));
  }

  @ParameterizedTest
  @CsvSource({"'not*not', 'not', '*not'", "'string with blanks', ' ', 'stringwith blanks'"})
  public void itShouldDeleteInStrings(String test, String delete, String expected) {
    StringBuffer testBuffer = new StringBuffer();
    testBuffer.append(test);
    StringUtilities.singleStringDelete(testBuffer, delete);
    assertEquals(expected, testBuffer.toString(), test);
  }

  @ParameterizedTest
  @CsvSource({"'not*not', 'not', '*not'", "'string with blanks', ' ', 'stringwith blanks'"})
  public void itShouldDeleteInStringsUsingDifferentSignature(
      String test, String delete, String expected) {
    String returnValue = StringUtilities.singleStringDelete(test, delete);
    assertEquals(expected, returnValue, test);
  }

  @Test
  public void itShouldHandleVariousArgumentsForDateParsing() {
    assertEquals(0L, StringUtilities.parseDate(null));
    assertEquals(0L, StringUtilities.parseDate("Not a date string."));
    assertEquals(1445412480000L, StringUtilities.parseDate("Wed, 21 Oct 2015 07:28:00 GMT"));
  }

  @Test
  public void itShouldHandleVariousArgumentsForDateFormatting() {
    Date testMe = new Date(1445412480000L);
    assertEquals("", StringUtilities.formatDate(null));
    assertEquals("Wed, 21 Oct 2015 07:28:00 GMT", StringUtilities.formatDate(testMe));
  }

  @Test
  public void itShouldNotAttemptAnInvalidStringEncoding() {
    byte[] testMe = new byte[0];
    assertEquals("", StringUtilities.getEncodedString(testMe, "NotARealEncoding"));
  }

  @Test
  public void itShouldNotAttemptAnInvalidByteEncoding() {
    byte[] result = StringUtilities.getEncodedBytes("Just a string.", "NotARealEncoding");
    assertEquals(0, result.length);
  }

  @Test
  public void itShouldEncodeAValidEncoding() {
    String test = "Just a string.";
    byte[] expected = test.getBytes(StandardCharsets.UTF_8);
    byte[] result = StringUtilities.getEncodedBytes(test, "UTF-8");
    assertEquals(expected.length, result.length);
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], result[i], "Not a match at element " + i);
    }
  }

  @Test
  public void itShouldNotTryAndReplaceInANullString() {
    String test = null;
    assertNull(StringUtilities.singleStringReplace(test, "cat", "dog"));
  }

  @Test
  public void itShouldNotInsertAfterSomethingThatIsNotThere() {
    String test = "exotic marigold";
    StringBuffer testBuffer = new StringBuffer();
    testBuffer.append(test);
    StringUtilities.insertAfter(testBuffer, "ecstatic", " blue");
    assertEquals(test, testBuffer.toString(), test);
    StringUtilities.insertAfter(testBuffer, "exotic", " blue");
    String expected = "exotic blue marigold";
    assertEquals(expected, testBuffer.toString(), test);
  }

  @Test
  public void itShouldNotInsertBeforeSomethingThatIsNotThere() {
    String test = "exotic marigold";
    StringBuffer testBuffer = new StringBuffer();
    testBuffer.append(test);
    StringUtilities.insertBefore(testBuffer, "ecstatic", "blue ");
    assertEquals(test, testBuffer.toString(), test);
    StringUtilities.insertBefore(testBuffer, "exotic", "blue ");
    String expected = "blue exotic marigold";
    assertEquals(expected, testBuffer.toString(), test);
  }

  @Test
  public void itShouldNotTryToFuzzyMatchWithNoRealInput() {
    String source = null;
    String match = null;
    assertFalse(StringUtilities.fuzzyMatches(source, match));
    source = "There is lint, not fuzz, here";
    assertTrue(StringUtilities.fuzzyMatches(source, match));
    match = "";
    assertTrue(StringUtilities.fuzzyMatches(source, match));
  }

  @ParameterizedTest
  @CsvSource({
    "'regal', 'gal', false",
    "'A & string', '           &   ', true",
    "'1 234 34 & a b999', 'a b', true",
    "'1 234 34 & 999', 'a', false"
  })
  public void itShouldExerciseSomeFuzzyMatching(String source, String match, boolean expected) {
    assertEquals(expected, StringUtilities.fuzzyMatches(source, match));
  }

  @Test
  public void itShouldNotSubstringMatchWithNoRealInput() {
    String input = null;
    String match = null;
    boolean bound = false;
    assertFalse(StringUtilities.substringMatches(input, match, bound));
    input = "A non-null string";
    assertTrue(StringUtilities.substringMatches(input, match, bound));
    match = "";
    assertTrue(StringUtilities.substringMatches(input, match, bound));
  }

  private static final String[] nameList = {
    "altar of bones",
    "game shop",
    "bone shop",
    "crimbo shop",
    "the other crimbo shop",
    "do you believe there is a third crimbo shop",
    "\"dairy\" farm",
    "&quot;dairy&quot; barn",
    "provider of bone altars",
    "&quot;debbie",
    "game shoppe"
  };

  @Test
  public void itShouldFindSomeMatchesInAMap() {
    List<String> results;
    String search = null;
    results = StringUtilities.getMatchingNames(nameList, search);
    assertTrue(results.isEmpty());
    search = "Altar of Bones";
    results = StringUtilities.getMatchingNames(nameList, search);
    assertEquals(1, results.size(), "Unexpected results");
    assertTrue(results.contains("altar of bones"));
    search = "shop";
    results = StringUtilities.getMatchingNames(nameList, search);
    assertEquals(6, results.size());
    assertTrue(results.contains("game shoppe"));
    search = "Shop";
    results = StringUtilities.getMatchingNames(nameList, search);
    assertEquals(6, results.size());
    assertTrue(results.contains("game shoppe"));
  }

  @Test
  public void itShouldFindExactMatchesOrNot() {
    List<String> results;
    String search = "\"altar of bones\"";
    results = StringUtilities.getMatchingNames(nameList, search);
    assertEquals(1, results.size(), "Unexpected matches");
    assertTrue(results.contains("altar of bones"));
    search = "\"Altar of Bones\"";
    results = StringUtilities.getMatchingNames(nameList, search);
    assertEquals(1, results.size(), "Unexpected matches");
    assertTrue(results.contains("altar of bones"));
    search = "\"Crimbo\"";
    results = StringUtilities.getMatchingNames(nameList, search);
    assertEquals(0, results.size(), "Unexpected matches");
    search = "Crimbo";
    results = StringUtilities.getMatchingNames(nameList, search);
    assertEquals(3, results.size(), "Unexpected matches");
  }

  @Test
  public void itShouldHelpExploreWhatAnExactMatchIs() {
    List<String> results;
    String search = "\"Debbie";
    Arrays.sort(nameList);
    results = StringUtilities.getMatchingNames(nameList, search);
    assertEquals(1, results.size(), "Unexpected matches");
    assertTrue(results.contains("&quot;debbie"));
    search = "\"Dabbie";
    results = StringUtilities.getMatchingNames(nameList, search);
    assertEquals(0, results.size(), "Unexpected matches");
  }

  @Test
  public void itShouldNotHavACanonicalNameForNull() {
    assertNull(StringUtilities.getCanonicalName(null));
  }

  @Test
  public void itShouldNotURLEncodeNull() {
    assertNull(StringUtilities.getURLEncode(null));
  }

  @Test
  public void itShouldNotEntityDecodeNull() {
    assertNull(StringUtilities.getEntityDecode(null));
  }

  @Test
  public void itShouldNotEntityEncodeNull() {
    assertNull(StringUtilities.getEntityEncode(null));
  }

  @Test
  public void itCanExerciseEntityEncoding() {
    String test = "\" starts with a quote";
    String expected = "&quot; starts with a quote";
    String returned = StringUtilities.getEntityEncode(test, false);
    assertEquals(expected, returned);
    returned = StringUtilities.getEntityEncode(test, true);
    assertEquals(expected, returned);
    test = "&1&2&3&4&5&6&7&8&9&0&1&2&3&4&5&6&7&8&9&0&";
    expected =
        "&amp;1&amp;2&amp;3&amp;4&amp;5&amp;6&amp;7&amp;8&amp;9&amp;"
            + "0&amp;1&amp;2&amp;3&amp;4&amp;5&amp;6&amp;7&amp;8&amp;9&amp;0&amp;";
    returned = StringUtilities.getEntityEncode(test, false);
    assertEquals(expected, returned);
    returned = StringUtilities.getEntityEncode(test, true);
    assertEquals(expected, returned);
  }

  @Test
  public void itShouldExerciseURLDecoding() {
    String input = null;
    String expected = null;
    assertEquals(expected, StringUtilities.getURLDecode(input));
    input = "https://www.kingdomofloathing.com";
    expected = "https://www.kingdomofloathing.com";
    assertEquals(expected, StringUtilities.getURLDecode(input));
    // For coverage - first call caches it, second finds it there
    assertEquals(expected, StringUtilities.getURLDecode(input));
  }
}
