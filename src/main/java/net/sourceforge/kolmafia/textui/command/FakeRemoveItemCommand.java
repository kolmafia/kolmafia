package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.session.ResultProcessor;

public class FakeRemoveItemCommand extends AbstractCommand {
  public FakeRemoveItemCommand() {
    this.usage = null;
  }

  @Override
  public void run(final String cmd, final String parameters) {
    AdventureResult item = ItemFinder.getFirstMatchingItem(parameters, Match.ANY);
    if (item != null) {
      ResultProcessor.processResult(item.getNegation());
    }
  }
}
