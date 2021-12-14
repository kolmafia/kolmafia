package net.sourceforge.kolmafia.textui;

import static org.eclipse.lsp4j.DiagnosticSeverity.Error;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.textui.ScriptData.InvalidScriptData;
import net.sourceforge.kolmafia.textui.ScriptData.ValidScriptData;
import net.sourceforge.kolmafia.textui.ScriptData.ValidScriptDataWithLocationTests;
import net.sourceforge.kolmafia.textui.parsetree.Scope;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tries to parse valid and invalid ASH programs. */
public class ParserTest {

  public static void testScriptValidity(ScriptData script) {
    final Scope scope = script.parser.parse();

    String firstError = null;
    for (Parser.AshDiagnostic diagnostic : script.parser.getDiagnostics()) {
      if (diagnostic.severity == Error) {
        firstError = diagnostic.toString();
        break;
      }
    }

    if (script instanceof InvalidScriptData) {
      testInvalidScript((InvalidScriptData) script, scope, firstError);
      return;
    }

    testValidScript((ValidScriptData) script, scope, firstError);
  }

  private static void testInvalidScript(
      final InvalidScriptData script, final Scope scope, final String error) {
    assertThat(script.desc, error, startsWith(script.errorText));

    if (script.errorLocationString != null) {
      assertThat(script.desc, error, containsString(" (" + script.errorLocationString + ")"));
    }
  }

  private static void testValidScript(
      final ValidScriptData script, final Scope scope, final String error) {
    assertNull(error, script.desc);
    assertEquals(script.tokens, getTokensContents(script.parser), script.desc);
    assertEquals(script.positions, getTokensPositions(script.parser), script.desc);

    if (script instanceof ValidScriptDataWithLocationTests) {
      ((ValidScriptDataWithLocationTests) script).locationTests.accept(scope);
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
  public void testMultipleDiagnosticsPerParser() {
    final String script =
        "import fake/path"
            + "\nstring foobar(string... foo, int bar) {"
            + "\n    continue;"
            + "\n}";
    final ByteArrayInputStream istream =
        new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8));
    final Parser parser = new Parser(null, istream, null);

    parser.parse();

    final List<Parser.AshDiagnostic> diagnostics = parser.getDiagnostics();
    assertEquals(4, diagnostics.size());

    assertEquals("fake/path could not be found", diagnostics.get(0).message);
    ParserTest.assertLocationEquals(1, 1, 1, 17, diagnostics.get(0).location);

    assertEquals("The vararg parameter must be the last one", diagnostics.get(1).message);
    ParserTest.assertLocationEquals(2, 30, 2, 33, diagnostics.get(1).location);

    assertEquals("Encountered 'continue' outside of loop", diagnostics.get(2).message);
    ParserTest.assertLocationEquals(3, 5, 3, 13, diagnostics.get(2).location);

    assertEquals("Missing return value", diagnostics.get(3).message);
    ParserTest.assertLocationEquals(2, 8, 2, 38, diagnostics.get(3).location);
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
