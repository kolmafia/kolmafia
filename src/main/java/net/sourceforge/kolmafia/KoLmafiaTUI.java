package net.sourceforge.kolmafia;

import java.io.PrintStream;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.textui.renderer.AnsiSerializer;
import net.sourceforge.kolmafia.utilities.NullStream;
import org.fusesource.jansi.AnsiConsole;

public class KoLmafiaTUI {
  private static final TUIOutputStream out = new TUIOutputStream();
  public static final PrintStream outputStream = out;

  private KoLmafiaTUI() {}

  static void initialize() {
    try {
      AnsiConsole.systemInstall();
    } catch (Exception e) {
      // Failed to install jansi. Continue as before.
    } catch (LinkageError e) {
      // Linking failed, but we can continue anyways.
    }
    out.openStandard();
  }

  private static class TUIOutputStream extends PrintStream {
    TUIOutputStream() {
      super(NullStream.INSTANCE);
    }

    void openStandard() {
      this.out = System.out;
    }

    @Override
    public void println(String line) {
      if (this.out == NullStream.INSTANCE) {
        return;
      }
      if (!Preferences.getBoolean("disableAnsiTerminal")) {
        line = AnsiSerializer.serializeHtml(line);
      }
      super.println(line);
    }
  }
}
