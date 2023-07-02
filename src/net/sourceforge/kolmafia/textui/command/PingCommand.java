package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.session.PingManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PingCommand extends AbstractCommand {
  public PingCommand() {
    this.usage = "[count] - run a ping test with specified number of pings)";
  }

  @Override
  public void run(final String cmd, String parameters) {
    parameters = parameters.trim();
    int count = 10;
    boolean verbose = false;

    if (!parameters.equals("")) {
      String[] split = parameters.split(" +");
      if (split.length > 0) {
        String countString = split[0];
        count = StringUtilities.parseInt(countString);
      }
      if (split.length > 1) {
        verbose = split[1].equals("true");
      }
    }

    var result = PingManager.runPingTest(count, verbose);

    RequestLogger.printLine(
        result.getCount()
            + " pings in "
            + result.getLow()
            + "-"
            + result.getHigh()
            + " (total = "
            + result.getTotal()
            + ") msec (average = "
            + result.getAverage()
            + ")");

    if (count >= 10) {
      result.save();
    }
  }
}
