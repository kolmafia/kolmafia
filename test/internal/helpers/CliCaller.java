package internal.helpers;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;

public class CliCaller {
  public static String callCli(final String command, final String params) {
    return callCli(command, params, false);
  }

  public static String callCli(final String command, final String params, final boolean check) {
    RequestLoggerOutput.startStream();
    var cli = new KoLmafiaCLI(System.in);
    KoLmafiaCLI.isExecutingCheckOnlyCommand = check;
    cli.executeCommand(command, params);
    KoLmafiaCLI.isExecutingCheckOnlyCommand = false;
    return RequestLoggerOutput.stopStream();
  }

  public static Cleanups withCliOutput(ByteArrayOutputStream ostream) {
    PrintStream out = new PrintStream(ostream, true);

    RequestLogger.openCustom(out);
    return new Cleanups(
        () -> {
          RequestLogger.closeCustom();
        });
  }
}
