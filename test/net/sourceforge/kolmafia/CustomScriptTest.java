package net.sourceforge.kolmafia;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;
import net.sourceforge.kolmafia.textui.command.AshSingleLineCommand;
import net.sourceforge.kolmafia.textui.command.CallScriptCommand;
import net.sourceforge.kolmafia.textui.command.JavaScriptCommand;
import net.sourceforge.kolmafia.textui.javascript.JavascriptRuntime;
import net.sourceforge.kolmafia.textui.parsetree.LibraryFunction;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class CustomScriptTest {
  // Directory containing expected output.
  private static final File EXPECTED_LOCATION = new File(KoLConstants.ROOT_LOCATION, "expected/");

  private static class ScriptNameFilter implements FilenameFilter {
    @Override
    public boolean accept(File dir, String name) {
      return name.endsWith(".ash")
          || name.endsWith(".txt")
          || name.endsWith(".cli")
          || name.endsWith(".js");
    }
  }

  private static Stream<Arguments> data() {
    return Arrays.stream(KoLConstants.SCRIPT_LOCATION.list(new ScriptNameFilter()))
        .map(Arguments::of);
  }

  // Looks for the file "test/root/expected/" + script + ".out".
  private static String getExpectedOutput(String script) throws IOException {
    return Files.readString(new File(EXPECTED_LOCATION, script + ".out").toPath());
  }

  public void beforeEach() {
    KoLCharacter.reset("CustomScriptTest");
    Preferences.reset("CustomScriptTest");
  }

  @ParameterizedTest
  @MethodSource("data")
  void testScript(String script) throws IOException {
    beforeEach();
    String expectedOutput = getExpectedOutput(script).trim();
    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
    try (PrintStream out = new PrintStream(ostream, true)) {
      // Inject custom output stream.
      RequestLogger.openCustom(out);

      CallScriptCommand command = new CallScriptCommand();
      command.run("call", script);

      RequestLogger.closeCustom();
    }

    String output =
        ostream
            .toString()
            .trim()
            // try to avoid environment-specific paths in stacktraces
            .replaceAll("\\bfile:.*?([^\\\\/\\s]+#\\d+)\\b", "file:%%STACKTRACE_LOCATION%%/$1");
    if (!expectedOutput.equals(output)) {
      System.out.println("expected = '" + expectedOutput + "'");
      System.out.println("output = '" + output + "'");
    }
    assertEquals(expectedOutput, output, script + " output does not match: ");
  }

  @Test
  void enumeratedTypesAreCaseInsensitive() {
    ByteArrayOutputStream ostream = new ByteArrayOutputStream();

    try (PrintStream out = new PrintStream(ostream, true)) {
      // Inject custom output stream.
      RequestLogger.openCustom(out);

      for (Type type : DataTypes.enumeratedTypes) {
        Value[] values = (Value[]) type.allValues().content;
        String firstValue = values[1].toString();

        var command = new AshSingleLineCommand();
        String comparisonScript =
            "($"
                + type
                + "["
                + firstValue.toLowerCase()
                + "] == $"
                + type
                + "["
                + firstValue.toUpperCase()
                + "]);";
        command.run("ash", comparisonScript);

        String output = ostream.toString().trim();
        assertEquals("Returned: true", output, "Checking case insensitivity for $" + type);
        ostream.reset();
      }

      RequestLogger.closeCustom();
    }
  }

  @Test
  void deprecationWarnings() {
    ByteArrayOutputStream ostream = new ByteArrayOutputStream();

    var newFunction =
        new LibraryFunction(
            "deprecated_function", DataTypes.VOID_TYPE, new Type[] {}, "Deprecation warning");
    RuntimeLibrary.functions.add(newFunction);

    try (PrintStream out = new PrintStream(ostream, true)) {
      // Inject custom output stream.
      RequestLogger.openCustom(out);

      var command = new AshSingleLineCommand();
      String script = "deprecated_function();";
      command.run("verify", script);

      String output = ostream.toString().trim();
      assertThat(
          output,
          startsWith(
              "Function \"deprecated_function\" is deprecated (char 1 to char 22) Deprecation warning"));
      RequestLogger.closeCustom();
    }

    RuntimeLibrary.functions.remove(newFunction);
  }

  @Nested
  class SessionStorage {
    @Test
    void sessionStorageWorksInCli() {
      ByteArrayOutputStream ostream = new ByteArrayOutputStream();

      try (PrintStream out = new PrintStream(ostream, true)) {
        var command = new JavaScriptCommand();
        command.run("js", "sessionStorage.setItem(\"test\", \"value\");");

        // Inject custom output stream.
        RequestLogger.openCustom(out);
        command.run("js", "sessionStorage.getItem(\"test\");");

        String output = ostream.toString().trim();
        assertThat(output, startsWith("Returned: value"));
        RequestLogger.closeCustom();
      }

      JavascriptRuntime.clearSessionStorage();
    }

    @Test
    void sessionStorageDoesNotLeakAcrossScriptEntrypoints() {
      ByteArrayOutputStream ostream = new ByteArrayOutputStream();

      try (PrintStream out = new PrintStream(ostream, true)) {
        var jsCommand = new JavaScriptCommand();
        jsCommand.run("js", "sessionStorage.setItem(\"test\", \"value\");");

        // Inject custom output stream.
        RequestLogger.openCustom(out);
        var callCommand = new CallScriptCommand();
        callCommand.run("call", "Excluded/sessionStorageTest.js");

        String output = ostream.toString().trim();
        assertThat(output, startsWith("true"));
        RequestLogger.closeCustom();
      }

      JavascriptRuntime.clearSessionStorage();
    }
  }

  @BeforeEach
  void setUp() {
    KoLmafia.forceContinue();
    ContactManager.registerPlayerId("heeheehee", "354981");
    StaticEntity.overrideRevision(10000);
    TurnCounter.clearCounters();
    // This is needed to make sure js test scripts work on Ubuntu when data/ is cleaned
    KoLmafia.ensureContentTypes();
  }

  @AfterEach
  void tearDown() {
    StaticEntity.overrideRevision(null);
  }
}
