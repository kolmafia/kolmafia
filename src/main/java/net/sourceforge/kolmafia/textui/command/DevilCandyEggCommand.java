package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CandyDatabase;
import net.sourceforge.kolmafia.persistence.CandyDatabase.CandyType;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class DevilCandyEggCommand extends AbstractCommand {
  public DevilCandyEggCommand() {
    this.usage = " <item> - devil a candy";
  }

  private boolean lacksCandyEggDeviler() {
    if (InventoryManager.getCount(ItemPool.CANDY_EGG_DEVILER) == 0) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You don't have a candy egg deviler.");
      return true;
    }
    return false;
  }

  @Override
  public void run(String command, String parameters) {
    if (lacksCandyEggDeviler()) return;

    AdventureResult item = ItemFinder.getFirstMatchingItem(parameters, Match.ANY);
    if (item != null) {
      if (CandyDatabase.getCandyType(item.getItemId()) == CandyType.NONE) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You can only devil candy.");
        return;
      }
      if (!InventoryManager.retrieveItem(item)) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Cannot retrieve " + item.getName() + ".");
        return;
      }
      RequestThread.postRequest(new GenericRequest("inventory.php?action=eggdevil"));
      RequestThread.postRequest(
          new GenericRequest("choice.php?whichchoice=1544&option=1&a=" + item.getItemId()));
    }
  }
}
