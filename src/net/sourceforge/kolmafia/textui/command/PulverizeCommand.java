package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.request.PulverizeRequest;

public class PulverizeCommand extends AbstractCommand {
  public PulverizeCommand() {
    this.usage = " <item> [, <item>]... - pulverize specified items";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    AdventureResult[] items = ItemFinder.getMatchingItemList(parameters, KoLConstants.inventory);

    for (AdventureResult item : items) {
      RequestThread.postRequest(new PulverizeRequest(item));
    }
  }
}
