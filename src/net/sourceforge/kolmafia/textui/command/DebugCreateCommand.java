package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;

public class DebugCreateCommand extends AbstractCommand {
  public DebugCreateCommand() {
    this.usage = null;
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (parameters.equals("")) {
      return;
    }

    String[] itemNames = parameters.split("\\s*,\\s*");

    AdventureResult item;

    for (int i = 0; i < itemNames.length; ++i) {
      item = ItemFinder.getFirstMatchingItem(itemNames[i], Match.ANY);

      if (item != null) {
        Concoction.debugId = item.getItemId();
        ConcoctionDatabase.refreshConcoctionsNow();
        Concoction.debugId = Integer.MAX_VALUE;
      }
    }
  }
}
