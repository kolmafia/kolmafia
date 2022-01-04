package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class HermitCommand extends AbstractCommand {
  public HermitCommand() {
    this.usage = "[?] [<item>] - get clover status, or trade for item.";
  }

  @Override
  public void run(final String cmd, String parameters) {
    if (!KoLmafia.permitsContinue()) {
      return;
    }

    int cloverCount = HermitRequest.cloverCount();

    if (parameters.equals("")) {
      KoLmafia.updateDisplay(
          "The Hermit has "
              + cloverCount
              + " clover"
              + (cloverCount == 1 ? "" : "s")
              + " available today.");
      return;
    }

    int count = 1;

    if (Character.isDigit(parameters.charAt(0))) {
      int spaceIndex = parameters.indexOf(" ");
      count = StringUtilities.parseInt(parameters.substring(0, spaceIndex));
      parameters = parameters.substring(spaceIndex);
    } else if (parameters.charAt(0) == '*') {
      int spaceIndex = parameters.indexOf(" ");
      count = Integer.MAX_VALUE;
      parameters = parameters.substring(spaceIndex);
    }

    parameters = parameters.toLowerCase().trim();
    int itemId = -1;

    if (KoLCharacter.inZombiecore() && parameters.contains("clover")) {
      if (!Preferences.getBoolean("_zombieClover")) {
        itemId = ItemPool.ELEVEN_LEAF_CLOVER;
      }
    } else {
      for (AdventureResult item : KoLConstants.hermitItems) {
        String name = item.getName();
        if (name.toLowerCase().contains(parameters)) {
          if (KoLmafiaCLI.isExecutingCheckOnlyCommand) {
            RequestLogger.printLine(name);
            return;
          }

          itemId = item.getItemId();
          break;
        }
      }
    }

    if (itemId == -1) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "You can't get a " + parameters + " from the hermit today.");
      return;
    }

    // "*" for clovers means all the hermit has available today.
    // For any other item, it means as many as you can get with
    // the worthless items you currently have

    count =
        itemId == ItemPool.ELEVEN_LEAF_CLOVER
            ? Math.min(count, cloverCount)
            : count == Integer.MAX_VALUE
                ? Math.min(count, HermitRequest.getWorthlessItemCount())
                : count;

    if (count > 0) {
      if (KoLCharacter.inZombiecore()) {
        RequestThread.postRequest(new HermitRequest());
      } else {
        RequestThread.postRequest(new HermitRequest(itemId, count));
      }
    }
  }
}
