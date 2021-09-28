package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.SummoningChamberRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SummonDemonCommand extends AbstractCommand {
  public SummonDemonCommand() {
    this.usage = " <demonName> | <effect> | <location> | <number> - use the Summoning Chamber.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (parameters.length() == 0) {
      return;
    }

    if (Preferences.getBoolean("demonSummoned")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You've already summoned a demon today.");
      return;
    }

    if (!InventoryManager.retrieveItem(ItemPool.BLACK_CANDLE, 3)) {
      return;
    }

    if (!InventoryManager.retrieveItem(ItemPool.EVIL_SCROLL)) {
      return;
    }

    String demon = parameters;
    int demonNumber = -1;
    if (Character.isDigit(parameters.charAt(0))) {
      demon = Preferences.getString("demonName" + parameters);
      demonNumber = StringUtilities.parseInt(parameters);
    } else {
      for (int i = 0; i < KoLAdventure.DEMON_TYPES.length; ++i) {
        String location = KoLAdventure.DEMON_TYPES[i][0];
        if (location != null && parameters.equalsIgnoreCase(location)) {
          demon = Preferences.getString("demonName" + (i + 1));
          demonNumber = i + 1;
          break;
        }

        String effect = KoLAdventure.DEMON_TYPES[i][1];
        if (effect != null && parameters.equalsIgnoreCase(effect)) {
          demon = Preferences.getString("demonName" + (i + 1));
          demonNumber = i + 1;
          break;
        }

        String name = Preferences.getString("demonName" + (i + 1));
        if (parameters.equalsIgnoreCase(name)) {
          demon = name;
          demonNumber = i + 1;
          break;
        }
      }
    }

    if (demon.equals("")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You don't know the name of that demon.");
      return;
    }

    if (demonNumber == 12 && !demon.startsWith("Neil")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You don't know the full name of that demon.");
      return;
    }

    SummoningChamberRequest demonSummon = new SummoningChamberRequest(demon, demonNumber);

    RequestThread.postRequest(demonSummon);
  }
}
