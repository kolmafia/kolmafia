package net.sourceforge.kolmafia;

import net.sourceforge.kolmafia.utilities.NullStream;

import java.io.PrintStream;

public class KoLmafiaTUI
{
    private static final TUIOutputStream out = new TUIOutputStream();
    public static final PrintStream outputStream = out;

    static void initialize()
    {
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
    }
}
