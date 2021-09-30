package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.request.UntinkerRequest;

public class UntinkerCommand extends AbstractCommand {
  public UntinkerCommand() {
    this.usage = " [ <item>... ] - complete quest, or untinker items.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (parameters.equals("")) {
      UntinkerRequest.completeQuest();
      return;
    }

    AdventureResult[] itemList =
        ItemFinder.getMatchingItemList(parameters, true, null, Match.UNTINKER);
    for (AdventureResult item : itemList) {
      RequestThread.postRequest(new UntinkerRequest(item.getItemId(), item.getCount()));
    }
  }
}
