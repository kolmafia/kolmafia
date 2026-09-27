package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CacheCommand extends AbstractCommand {
  public CacheCommand() {
    this.usage = "[clear] - get image cache status or clear cache.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    String[] split = parameters.split(" ");
    String command = split[0];

    if (command.equals("")) {
      long date = Preferences.getLong("lastImageCacheClear");
      if (date == 0L) {
        RequestLogger.printLine("Image cache never cleared.");
      } else {
        RequestLogger.printLine("Image cache last cleared on " + StringUtilities.formatDate(date));
      }
      return;
    }

    if (command.equals("clear")) {
      RelayRequest.clearImageCache();
    }
  }
}
