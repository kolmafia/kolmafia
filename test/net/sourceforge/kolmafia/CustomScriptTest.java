package net.sourceforge.kolmafia;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintStream;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.textui.command.CallScriptCommand;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

  private static String[] data() {
    return KoLConstants.SCRIPT_LOCATION.list(new ScriptNameFilter());
  }

  // Looks for the file "test/root/expected/" + script + ".out".
  private static String getExpectedOutput(String script) {
    BufferedReader reader = DataUtilities.getReader(new File(EXPECTED_LOCATION, script + ".out"));
    StringBuilder sb = new StringBuilder();
    for (Object line : reader.lines().toArray()) {
      sb.append(((String) line) + "\n");
    }
    return sb.toString();
  }

  private void testScript(String script) {
    String expectedOutput = getExpectedOutput(script);
    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(ostream);
    // Inject custom output stream.
    RequestLogger.openCustom(out);

    CallScriptCommand command = new CallScriptCommand();
    command.run("call", script);

    String output = ostream.toString();
    assertEquals(script + " output does not match: ", expectedOutput, output);
  }

  @Before
  public void setRevision() {
    StaticEntity.overrideRevision(10000);
  }

  @After
  public void clearRevision() {
    StaticEntity.overrideRevision(null);
  }

  @Test
  public void testScripts() {
    TurnCounter.clearCounters();
    for (String script : data()) {
      testScript(script);
    }
  }
}
