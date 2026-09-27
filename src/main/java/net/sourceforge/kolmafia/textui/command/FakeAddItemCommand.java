package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AdventureResult.AdventureLongCountResult;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FakeAddItemCommand extends AbstractCommand {
  public FakeAddItemCommand() {
    this.usage = null;
  }

  @Override
  public void run(final String cmd, final String parameters) {
    AdventureResult item = null;

    if (parameters.endsWith(" meat")) {
      String amountString = parameters.substring(0, parameters.length() - 5).trim();

      if (StringUtilities.isNumeric(amountString)) {
        item =
            new AdventureLongCountResult(
                AdventureResult.MEAT, StringUtilities.parseLong(amountString));
      }
    }

    if (item == null) {
      item = ItemFinder.getFirstMatchingItem(parameters);
    }

    if (item != null) {
      RequestLogger.printLine("Faking acquisition: " + item);
      ResultProcessor.processResult(item);
    }
  }
}
