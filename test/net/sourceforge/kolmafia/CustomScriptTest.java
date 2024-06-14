package net.sourceforge.kolmafia;

import static internal.helpers.Utilities.verboseDelete;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
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

class CustomScriptTest {
  @BeforeEach
  void setUp() {
    KoLmafia.forceContinue();
    ContactManager.registerPlayerId("heeheehee", "354981");
    StaticEntity.overrideRevision(10000);
    TurnCounter.clearCounters();
  }

  @AfterEach
  void tearDown() {
    StaticEntity.overrideRevision(null);
  }

  @Nested
  class TestScripts {
    @BeforeEach
    public void beforeEach() {
      KoLCharacter.reset("CustomScriptTest");
      Preferences.reset("CustomScriptTest");
    }

    // Directory containing expected output.
    private static final File EXPECTED_LOCATION = new File(KoLConstants.ROOT_LOCATION, "expected/");

    private static Stream<Arguments> data() {
      var files =
          KoLConstants.SCRIPT_LOCATION.list(
              (dir, name) ->
                  name.endsWith(".ash")
                      || name.endsWith(".txt")
                      || name.endsWith(".cli")
                      || name.endsWith(".js"));
      return Arrays.stream(files).map(Arguments::of);
    }

    // Looks for the file "test/root/expected/" + script + ".out".
    private static String getExpectedOutput(String script) throws IOException {
      return Files.readString(new File(EXPECTED_LOCATION, script + ".out").toPath());
    }

    @ParameterizedTest
    @MethodSource("data")
    void testScript(String script) throws IOException {
      String expectedOutput = getExpectedOutput(script).trim();
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      try (PrintStream out = new PrintStream(outputStream, true)) {
        // Inject custom output stream.
        RequestLogger.openCustom(out);

        CallScriptCommand command = new CallScriptCommand();
        command.run("call", script);

        RequestLogger.closeCustom();
      }

      String output =
          outputStream
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
  }

  @Test
  void enumeratedTypesAreCaseInsensitive() {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    try (PrintStream out = new PrintStream(outputStream, true)) {
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

        String output = outputStream.toString().trim();
        assertEquals("Returned: true", output, "Checking case insensitivity for $" + type);
        outputStream.reset();
      }

      RequestLogger.closeCustom();
    }
  }

  @Nested
  class DeprecationWarnings {

    @Test
    void deprecationWarnings() {
      /*
      This test is expected to generate a debug log.  If the log exists when this test is started then the log
      should be preserved because some other test generated it.  But otherwise this test should delete the log
      after running.
       */
      String debugLogName = "DEBUG_" + KoLConstants.DAILY_FORMAT.format(new Date()) + ".txt";
      File debugFile = new File(KoLConstants.ROOT_LOCATION, debugLogName);
      boolean debugFileExistsAtStart = debugFile.exists();
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      var newFunction =
          new LibraryFunction(
              "deprecated_function", DataTypes.VOID_TYPE, List.of(), "Deprecation warning");
      RuntimeLibrary.functions.add(newFunction);

      try (PrintStream out = new PrintStream(outputStream, true)) {
        // Inject custom output stream.
        RequestLogger.openCustom(out);

        var command = new AshSingleLineCommand();
        String script = "deprecated_function();";
        command.run("verify", script);

        String output = outputStream.toString().trim();
        assertThat(
            output,
            startsWith(
                "Function \"deprecated_function\" is deprecated (char 1 to char 22) Deprecation warning"));
        RequestLogger.closeCustom();
      }

      RuntimeLibrary.functions.remove(newFunction);
      if (!debugFileExistsAtStart) {
        verboseDelete(debugFile);
      }
    }
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
}
