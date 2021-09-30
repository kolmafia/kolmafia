package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.request.CreateItemRequest;

public class CreateItemCommand extends AbstractCommand {
  public CreateItemCommand() {
    this.usage = " [ <item>... ] - list creatables, or create specified items.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    try (Checkpoint checkpoint = new Checkpoint()) {
      CreateItemCommand.create(parameters);
    }
  }

  public static void create(final String parameters) {
    if (parameters.equals("")) {
      RequestLogger.printList(ConcoctionDatabase.getCreatables());
      return;
    }

    AdventureResult[] itemList =
        ItemFinder.getMatchingItemList(parameters, true, null, Match.CREATE);
    for (AdventureResult currentMatch : itemList) {
      CreateItemRequest irequest = CreateItemRequest.getInstance(currentMatch);

      if (irequest == null) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR,
            ConcoctionDatabase.excuse != null
                ? ConcoctionDatabase.excuse
                : "That item cannot be created.");
        return;
      }

      irequest.setQuantityNeeded(currentMatch.getCount());
      RequestThread.postRequest(irequest);
    }
  }
}
