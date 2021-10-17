package net.sourceforge.kolmafia;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.textui.command.CallScriptCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class CustomScriptTest {
  // Directory containing expected output.
  private static final File EXPECTED_LOCATION = new File(KoLConstants.ROOT_LOCATION, "expected/");

  private static class ScriptNameFilter implements FilenameFilter {
    public boolean accept(File dir, String name) {
      return name.endsWith(".ash")
          || name.endsWith(".txt")
          || name.endsWith(".cli")
          || name.endsWith(".js");
    }
  }

  private static Stream<Arguments> data() {
    return Arrays.asList(KoLConstants.SCRIPT_LOCATION.list(new ScriptNameFilter())).stream()
        .map(Arguments::of);
  }

  // Looks for the file "test/root/expected/" + script + ".out".
  private static String getExpectedOutput(String script) throws IOException {
    return Files.readString(new File(EXPECTED_LOCATION, script + ".out").toPath());
  }

  @ParameterizedTest
  @MethodSource("data")
  void testScript(String script) throws IOException {
    String expectedOutput = getExpectedOutput(script);
    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
    try (PrintStream out = new PrintStream(ostream, true)) {
      // Inject custom output stream.
      RequestLogger.openCustom(out);

      CallScriptCommand command = new CallScriptCommand();
      command.run("call", script);
    }

    String output = ostream.toString();
    assertEquals(expectedOutput, output, script + " output does not match: ");
  }

  @BeforeEach
  void setUp() {
    ContactManager.registerPlayerId("heeheehee", "354981");
    StaticEntity.overrideRevision(10000);
    TurnCounter.clearCounters();
    KoLmafia.forceContinue();
  }

  @AfterEach
  void tearDown() {
    StaticEntity.overrideRevision(null);
  }
}
