package net.sourceforge.kolmafia.textui.command;

import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.request.DisplayCaseRequest;
import net.sourceforge.kolmafia.session.DisplayCaseManager;

public class DisplayCaseCommand extends AbstractCommand {
  public DisplayCaseCommand() {
    this.usage =
        " [<filter>] | put <item>... | take <item>... - list or manipulate your display case.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (!DisplayCaseManager.collectionRetrieved) {
      RequestThread.postRequest(new DisplayCaseRequest());
    }

    if (parameters.length() == 0) {
      RequestLogger.printList(KoLConstants.collection);
      return;
    }

    String itemName = parameters;
    List<AdventureResult> sourceList = null;

    if (parameters.startsWith("put ")) {
      itemName = parameters.substring(4);
      sourceList = KoLConstants.inventory;
    }

    if (parameters.startsWith("take ")) {
      itemName = parameters.substring(5);
      sourceList = KoLConstants.collection;
    }

    if (sourceList == null) {
      ShowDataCommand.show("display " + parameters);
      return;
    }

    AdventureResult[] items = ItemFinder.getMatchingItemList(itemName, sourceList);

    if (items.length == 0) {
      return;
    }

    RequestThread.postRequest(
        new DisplayCaseRequest(items, (sourceList == KoLConstants.inventory)));
  }
}
