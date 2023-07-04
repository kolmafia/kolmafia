package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.session.PingManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PingCommand extends AbstractCommand {
  public PingCommand() {
    this.usage =
        " [count [(api|council|main) [verbose]]] - run a ping test with specified number of pings";
  }

  @Override
  public void run(final String cmd, String parameters) {
    parameters = parameters.trim();
    int count = PingManager.MINIMUM_HISTORY_PINGS;
    String page = PingManager.DEFAULT_PAGE;
    boolean verbose = false;

    if (!parameters.equals("")) {
      String[] split = parameters.split(" +");
      if (split.length > 0) {
        String countString = split[0];
        count = StringUtilities.parseInt(countString);
      }
      if (split.length > 1) {
        switch (split[1]) {
          case "api", "council", "main" -> {
            page = split[1] + ".php";
          }
          default -> {
            KoLmafia.updateDisplay(
                MafiaState.ERROR, "'" + split[1] + "' is not a valid page to ping.");
            return;
          }
        }
      }
      if (split.length > 2) {
        verbose = split[2].equals("verbose") || split[2].equals("true");
      }
    }

    var result = PingManager.runPingTest(count, page, verbose);
    RequestLogger.printLine(
        result.getCount()
            + " pings to "
            + page
            + " at "
            + result.getLow()
            + "-"
            + result.getHigh()
            + " msec apiece (total = "
            + result.getTotal()
            + ", average = "
            + result.getAverage()
            + ") = "
            + result.getBPS()
            + " bytes/second");
  }
}
