package net.sourceforge.kolmafia;

import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.textui.renderer.AnsiSerializer;
import net.sourceforge.kolmafia.utilities.NullStream;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KoLmafiaTUI
{
    private static final TUIOutputStream out = new TUIOutputStream();
    public static final PrintStream outputStream = out;

    static void initialize()
    {
        AnsiConsole.systemInstall();
        out.openStandard();
    }

    private static class TUIOutputStream
        extends PrintStream
    {
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
            if (!Preferences.getBoolean("disableAnsiTerminal"))
            {
                line = AnsiSerializer.serializeHtml(line);
            }
            super.println(line);
        }
    }
}
