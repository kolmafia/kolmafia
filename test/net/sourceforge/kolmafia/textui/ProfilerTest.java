package net.sourceforge.kolmafia.textui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.textui.command.CallScriptCommand;
import org.junit.jupiter.api.Test;

class ProfilerTest {

  @Test
  public void itShouldRunTheProfilerOnAScript() {
    String script = "Excluded/CountItems.ash";
    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
    try (PrintStream out = new PrintStream(ostream, true)) {
      // Inject custom output stream.
      RequestLogger.openCustom(out);
      CallScriptCommand command = new CallScriptCommand();
      command.run("profile", script);
      RequestLogger.closeCustom();
    }
    String output =
        ostream
            .toString()
            .trim()
            // try to avoid environment-specific paths in stacktraces
            .replaceAll("\\bfile:.*?([^\\\\/\\s]+#\\d+)\\b", "file:%%STACKTRACE_LOCATION%%/$1");
    assertTrue(output.contains("toplevel"));
    assertTrue(output.contains("Name (sorted by total time)"));
    assertTrue(output.contains("Name (sorted by net time)"));
  }
}
