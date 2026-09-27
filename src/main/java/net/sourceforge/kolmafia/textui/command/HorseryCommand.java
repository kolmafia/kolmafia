package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class HorseryCommand extends AbstractCommand {
  public HorseryCommand() {
    this.usage =
        " [init | -combat | stat | resist | regen | meat | random | spooky | normal | dark | crazy | pale | # ] - get the indicated horse";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (!Preferences.getBoolean("horseryAvailable")) {
      KoLmafia.updateDisplay("You need a horsery first.");
      return;
    }
    int choice = StringUtilities.parseInt(parameters);
    if (choice < 1 || choice > 4) {
      if (parameters.contains("init")
          || parameters.contains("regen")
          || parameters.startsWith("normal")) {
        choice = 1;
      } else if (parameters.contains("-combat")
          || parameters.contains("meat")
          || parameters.startsWith("dark")) {
        choice = 2;
      } else if (parameters.contains("stat")
          || parameters.contains("random")
          || parameters.startsWith("crazy")) {
        choice = 3;
      } else if (parameters.contains("resist")
          || parameters.contains("spooky")
          || parameters.startsWith("pale")) {
        choice = 4;
      }
    }
    if (choice < 1 || choice > 7) {
      KoLmafia.updateDisplay(MafiaState.ERROR, parameters + " is not a valid option.");
      return;
    }
    if (choice == 1 && Preferences.getString("_horsery").equals("normal horse")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You have already have the normal horse.");
      return;
    }
    if (choice == 2 && Preferences.getString("_horsery").equals("dark horse")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You have already have the dark horse.");
      return;
    }
    if (choice == 3 && Preferences.getString("_horsery").equals("crazy horse")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You have already have the crazy horse.");
      return;
    }
    if (choice == 4 && Preferences.getString("_horsery").equals("pale horse")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You have already have the pale horse.");
      return;
    }
    RequestThread.postRequest(
        new GenericRequest("place.php?whichplace=town_right&action=town_horsery"));
    RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1266&option=" + choice));
  }
}
