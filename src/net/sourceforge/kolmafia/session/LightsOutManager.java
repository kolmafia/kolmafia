package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;

public class LightsOutManager {
  public static void checkCounter() {
    if (!Preferences.getBoolean("trackLightsOut")) {
      return;
    }

    if (TurnCounter.isCounting("Spookyraven Lights Out")) {
      return;
    }

    if (Preferences.getString("nextSpookyravenElizabethRoom").equals("none")
        && Preferences.getString("nextSpookyravenStephenRoom").equals("none")) {
      return;
    }

    int turns = 37 - (KoLCharacter.getTurnsPlayed() % 37);
    TurnCounter.startCounting(turns, "Spookyraven Lights Out", "bulb.gif");
  }

  public static boolean lightsOutNow() {
    int totalTurns = KoLCharacter.getTurnsPlayed();
    return totalTurns % 37 == 0 && Preferences.getInteger("lastLightsOutTurn") != totalTurns;
  }

  public static void report() {
    String elizabethRoom = Preferences.getString("nextSpookyravenElizabethRoom");
    if (elizabethRoom.equals("none")) {
      RequestLogger.printLine("You have defeated Elizabeth Spookyraven");
    } else {
      RequestLogger.printLine("Elizabeth will next show up in " + elizabethRoom);
    }

    String stephenRoom = Preferences.getString("nextSpookyravenStephenRoom");
    if (stephenRoom.equals("none")) {
      RequestLogger.printLine("You have defeated Stephen Spookyraven");
    } else {
      RequestLogger.printLine("Stephen will next show up in " + stephenRoom);
    }
  }

  public static String message() {
    return LightsOutManager.message(false);
  }

  public static String message(boolean link) {
    String msg = "";
    String elizabethRoom = Preferences.getString("nextSpookyravenElizabethRoom");
    String stephenRoom = Preferences.getString("nextSpookyravenStephenRoom");
    if (!elizabethRoom.equals("none")) {
      if (link) {
        String url = AdventureDatabase.getAdventure(elizabethRoom).getRequest().getURLString();
        elizabethRoom = "<a href=\"" + url + "\">" + elizabethRoom + "</a>";
      }
      msg += "Elizabeth can be found in " + elizabethRoom + ".  ";
    }
    if (!stephenRoom.equals("none")) {
      if (link) {
        String url = AdventureDatabase.getAdventure(stephenRoom).getRequest().getURLString();
        stephenRoom = "<a href=\"" + url + "\">" + stephenRoom + "</a>";
      }
      msg += "Stephen can be found in " + stephenRoom + ".  ";
    }

    return msg;
  }
}
