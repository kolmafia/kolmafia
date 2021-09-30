package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;

public class DaycareCommand extends AbstractCommand {
  public DaycareCommand() {
    this.usage = " [ item | muscle | mysticality | moxie | regen ] - get the item or buff";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (!Preferences.getBoolean("daycareOpen") && !Preferences.getBoolean("_daycareToday")) {
      KoLmafia.updateDisplay("You need a boxing daycare first.");
      return;
    }
    if (parameters.contains("item")) {
      if (Preferences.getBoolean("_daycareNap")) {
        KoLmafia.updateDisplay("You have already had a Boxing Daydream today");
        return;
      }
      RequestThread.postRequest(
          new GenericRequest("place.php?whichplace=town_wrong&action=townwrong_boxingdaycare"));
      RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1334&option=1"));
    } else {
      int choice = 0;
      if (parameters.contains("mus")) {
        choice = 1;
      } else if (parameters.contains("mox")) {
        choice = 2;
      } else if (parameters.contains("mys")) {
        choice = 3;
      } else if (parameters.contains("regen")) {
        choice = 4;
      }
      if (choice == 0) {
        KoLmafia.updateDisplay("Choice not recognised");
        return;
      }
      if (Preferences.getBoolean("_daycareSpa")) {
        KoLmafia.updateDisplay("You have already visited the Boxing Day Spa today");
        return;
      }
      RequestThread.postRequest(
          new GenericRequest("place.php?whichplace=town_wrong&action=townwrong_boxingdaycare"));
      RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1334&option=2"));
      RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1335&option=" + choice));
    }
  }
}
