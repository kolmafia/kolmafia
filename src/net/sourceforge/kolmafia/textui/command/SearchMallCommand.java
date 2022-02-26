package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.session.MallPriceManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SearchMallCommand extends AbstractCommand {
  public SearchMallCommand() {
    this.usage = " <item> [ with limit <number> ] - search the Mall.";
  }

  @Override
  public void run(final String cmd, String parameters) {
    List<String> results = new ArrayList<>();
    int desiredLimit = 0;

    if (parameters.indexOf("with limit") != -1) {
      String[] splitup = parameters.split("with limit");
      parameters = splitup[0];
      desiredLimit = StringUtilities.parseInt(splitup[1]);
    }

    MallPriceManager.searchMall(parameters, desiredLimit, results);
    RequestLogger.printList(results);
  }
}
