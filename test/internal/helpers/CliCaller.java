package internal.helpers;

import net.sourceforge.kolmafia.KoLmafiaCLI;

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
}
