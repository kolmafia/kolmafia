package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.UntinkerRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

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

    var useLoathingLegion =
        InventoryManager.getAccessibleCount(ItemPool.LOATHING_LEGION_UNIVERSAL_SCREWDRIVER) > 0;
    if (useLoathingLegion) {
      InventoryManager.retrieveItem(ItemPool.LOATHING_LEGION_UNIVERSAL_SCREWDRIVER);
    }

    AdventureResult[] itemList =
        ItemFinder.getMatchingItemList(parameters, true, null, Match.UNTINKER);
    for (AdventureResult item : itemList) {
      var count = item.getCount();
      if (count <= 0) continue;

      if (useLoathingLegion) {
        for (int i = 1; i <= count && KoLmafia.permitsContinue(); ++i) {
          var req = new GenericRequest("");
          req.constructURLString(
              "inv_use.php?pwd="
                  + GenericRequest.passwordHash
                  + "&ajax=1&whichitem="
                  + ItemPool.LOATHING_LEGION_UNIVERSAL_SCREWDRIVER
                  + "&action=screw&dowhichitem="
                  + item.getItemId(),
              false);
          RequestThread.postRequest(req);
        }
        KoLmafia.updateDisplay("Unscrewing " + item + "...");
      } else {
        RequestThread.postRequest(new UntinkerRequest(item.getItemId(), count));
      }
    }
  }
}
