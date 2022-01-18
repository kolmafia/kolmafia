package net.sourceforge.kolmafia.textui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.textui.ScriptData.InvalidScriptData;
import net.sourceforge.kolmafia.textui.ScriptData.InvalidScriptDataWithErrorFilterTest;
import net.sourceforge.kolmafia.textui.ScriptData.ValidScriptData;
import net.sourceforge.kolmafia.textui.ScriptData.ValidScriptDataWithLocationTests;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tries to parse valid and invalid ASH programs. */
public class ParserTest {

  public static void testScriptValidity(final ScriptData script) {
    // Wait until now to throw errors thrown during parsing, so that we can more easily tell which
    // test/script threw it
    if (script.parsingException != null) {
      throw new RuntimeException(script.parsingException.getMessage(), script.parsingException);
    }

    if (script instanceof InvalidScriptData) {
      testInvalidScript((InvalidScriptData) script);
      return;
    }

    testValidScript((ValidScriptData) script);
  }

  private static void testInvalidScript(final InvalidScriptData script) {
    assertFalse(script.errors.isEmpty(), script.desc);

    // The error that changed the state of the script from "valid" to "invalid"
    final String firstError = script.errors.get(0);

    assertThat(
        script.desc,
        firstError,
        startsWith(script.errorText + " (" + script.errorLocationString + ")"));

    if (script instanceof InvalidScriptDataWithErrorFilterTest) {
      final InvalidScriptData filteredScript =
          ((InvalidScriptDataWithErrorFilterTest) script).filteredScript;

      // Trigger the normal tests on that version
      ParserTest.testScriptValidity(filteredScript);

      // Confirm that the modified version doesn't contain the initial version's first error
      if (filteredScript.errors.stream().anyMatch(error -> error.startsWith(script.errorText))) {
        final StringBuilder message = new StringBuilder();

        message.append(filteredScript.desc);
        message.append(KoLConstants.LINE_BREAK);
        message.append("The generated list of errors");
        message.append(KoLConstants.LINE_BREAK);
        message.append("[");
        message.append(KoLConstants.LINE_BREAK);
        message.append("  ");
        message.append(String.join("," + KoLConstants.LINE_BREAK + "  ", filteredScript.errors));
        message.append(KoLConstants.LINE_BREAK);
        message.append("]");
        message.append(KoLConstants.LINE_BREAK);
        message.append("should not contain ");
        message.append(script.errorText);

        fail(message.toString());
      }
    }
  }

  private static void testValidScript(final ValidScriptData script) {
    assertTrue(script.errors.isEmpty(), script.desc);

    assertEquals(script.tokens, getTokensContents(script.parser), script.desc);
    assertEquals(script.positions, getTokensPositions(script.parser), script.desc);

    if (script instanceof ValidScriptDataWithLocationTests) {
      ((ValidScriptDataWithLocationTests) script).locationTests.accept(script.scope);
    }
  }

  private static List<String> getTokensContents(final Parser parser) {
    return parser.getTokens().stream().map(token -> token.content).collect(Collectors.toList());
  }

  private static List<String> getTokensPositions(final Parser parser) {
    return parser.getTokens().stream()
        .map(token -> token.getStart().getLine() + 1 + "-" + (token.getStart().getCharacter() + 1))
        .collect(Collectors.toList());
  }

  public static void assertLocationEquals(
      final int expectedStartLine,
      final int expectedStartCharacter,
      final int expectedEndLine,
      final int expectedEndCharacter,
      final Location location) {
    Range expectedRange =
        new Range(
            new Position(expectedStartLine - 1, expectedStartCharacter - 1),
            new Position(expectedEndLine - 1, expectedEndCharacter - 1));
    Range actualRange = location.getRange();

    if (actualRange instanceof Line.Token) {
      // Range.equals(Object) checks for the class, so we can't just submit the Token
      actualRange = new Range(actualRange.getStart(), actualRange.getEnd());
    }

    assertEquals(expectedRange, actualRange);
  }

  @Test
  public void testInterruption() {
    assertFalse(Thread.currentThread().isInterrupted());

    Thread.currentThread().interrupt();
    assertTrue(Thread.currentThread().isInterrupted());

    // Doesn't matter if we use valid() or invalid(); the parsing gets interrupted anyway
    final ScriptData script = ScriptData.valid("Will not parse", "foobar();", null, null);

    assertNull(script.scope);
    assertNull(script.errors);
    assertInstanceOf(InterruptedException.class, script.parsingException);

    // The interrupted state of the thread was cleared
    assertFalse(Thread.currentThread().isInterrupted());
  }

  @Test
  public void testMultipleDiagnosticsPerParser() {
    final ScriptData script =
        ScriptData.invalid(
            "multiple diagnostics per parser",
            "import fake/path\nvoid foobar(string... foo, int bar) {\n    continue;\n}",
            "fake/path could not be found",
            "char 1 to char 17");

    ParserTest.testScriptValidity(script);

    assertEquals(3, script.errors.size());

    assertEquals(
        "The vararg parameter must be the last one (line 2, char 28 to char 31)",
        script.errors.get(1));

    assertEquals(
        "Encountered 'continue' outside of loop (line 3, char 5 to char 13)", script.errors.get(2));
  }

  @Test
  public void testErrorFilter() {
    final ScriptData script =
        ScriptData.invalid(
            "error filter test", "int a; max(a, b)", "Unknown variable 'b'", "char 15 to char 16");

    ParserTest.testScriptValidity(script);

    assertEquals(1, script.errors.size());

    // Note the lack of "Function 'max( int, <unknown> )' undefined."
  }

  public static Stream<Arguments> mergeLocationsData() {
    return Stream.of(
        Arguments.of(
            "null start",
            (Location) null,
            new Location("foo", new Range(new Position(0, 0), new Position(0, 1))),
            new Location("foo", new Range(new Position(0, 0), new Position(0, 1)))),
        Arguments.of(
            "null end",
            new Location("foo", new Range(new Position(0, 0), new Position(0, 1))),
            (Location) null,
            new Location("foo", new Range(new Position(0, 0), new Position(0, 1)))),
        Arguments.of(
            "different URIs",
            new Location("foo", new Range(new Position(0, 0), new Position(0, 1))),
            new Location("bar", new Range(new Position(5, 6), new Position(5, 8))),
            new Location("foo", new Range(new Position(0, 0), new Position(0, 1)))),
        Arguments.of(
            "start's start coming after end's end",
            new Location("foo", new Range(new Position(2, 5), new Position(2, 6))),
            new Location("foo", new Range(new Position(2, 2), new Position(2, 3))),
            new Location("foo", new Range(new Position(2, 5), new Position(2, 6)))),
        Arguments.of(
            "Successful merge",
            new Location("foo", new Range(new Position(2, 5), new Position(2, 6))),
            new Location("foo", new Range(new Position(4, 2), new Position(7, 1))),
            new Location("foo", new Range(new Position(2, 5), new Position(7, 1)))));
  }

  @ParameterizedTest
  @MethodSource("mergeLocationsData")
  public void testMergeLocations(String desc, Location start, Location end, Location expected) {
    Location merged = Parser.mergeLocations(start, end);
    assertEquals(expected, merged, desc);
  }

  public static Stream<Arguments> getFileAndRangeData() {
    return Stream.of(
        Arguments.of("null range 1", null, null, null),
        Arguments.of("null range 2", "foo", null, null),
        Arguments.of(
            "illegal range", "foo", new Range(new Position(0, 1), new Position(0, 0)), null),
        Arguments.of(
            "0-width range",
            "foo",
            new Range(new Position(0, 0), new Position(0, 0)),
            "foo, line 1, char 1"),
        Arguments.of(
            "uni-line range",
            "foo",
            new Range(new Position(0, 0), new Position(0, 5)),
            "foo, line 1, char 1 to char 6"),
        Arguments.of(
            "multi-line range, end at char 0",
            "foo",
            new Range(new Position(0, 5), new Position(2, 0)),
            "foo, line 1, char 6 to line 3, char 1"),
        Arguments.of(
            "multi-line range, end at char > 0",
            "foo",
            new Range(new Position(0, 5), new Position(2, 3)),
            "foo, line 1, char 6 to line 3, char 4"),
        Arguments.of(
            "null file, start at line 0",
            null,
            new Range(new Position(0, 5), new Position(2, 3)),
            "char 6 to line 3, char 4"),
        Arguments.of(
            "null file, start at line > 0",
            null,
            new Range(new Position(1, 5), new Position(2, 3)),
            "line 2, char 6 to line 3, char 4"));
  }

  @ParameterizedTest
  @MethodSource("getFileAndRangeData")
  public void testGetFileAndRange(String desc, String fileName, Range range, String expected) {
    if (expected == null) {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            Parser.getFileAndRange(fileName, range);
          },
          desc);
      return;
    }

    String actual = Parser.getFileAndRange(fileName, range);
    assertEquals(expected, actual, desc);
  }

  @Test
  public void testGetFileAndRangeWithNamedNamespace() {
    Preferences.setString("commandLineNamespace", "bar");
    testGetFileAndRange(
        "null file with named command line namespace",
        null,
        new Range(new Position(0, 5), new Position(2, 3)),
        "bar, char 6 to line 3, char 4");
    Preferences.resetToDefault("commandLineNamespace");
  }
}
