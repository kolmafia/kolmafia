package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.session.BatManager;

public class BatFellowRequest extends GenericRequest {
  public BatFellowRequest() {
    super("place.php");
  }

  public static void parseResponse(final String urlString, final String responseText) {
    BatManager.parsePlaceResponse(urlString, responseText);
  }

  public static boolean registerRequest(final String place, final String urlString) {
    // place.php?whichplace=batman_xxx

    String action = GenericRequest.getAction(urlString);

    if (action == null) {
      return false;
    }

    String location = null;

    // whichplace=batman_cave

    if (action.equals("batman_cave_rnd")) {
      location = "Bat-Research and Bat-Development";
    } else if (action.equals("batman_cave_car")) {
      location = "The Bat-Sedan";
    }

    // whichplace=batman_downtown

    else if (action.equals("batman_downtown_hospital")) {
      location = "Gotpork Memorial Hospital";
    } else if (action.equals("batman_downtown_car")) {
      location = "The Bat-Sedan";
    }

    // whichplace=batman_park

    else if (action.equals("batman_park_car")) {
      location = "The Bat-Sedan";
    }

    // whichplace=batman_slums

    else if (action.equals("batman_slums_car")) {
      location = "The Bat-Sedan";
    }

    // whichplace=batman_industrial

    else if (action.equals("batman_industrial_car")) {
      location = "The Bat-Sedan";
    } else {
      return false;
    }

    String message = message = "{" + BatManager.getTimeLeftString() + "} " + location;

    RequestLogger.printLine();
    RequestLogger.printLine(message);

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(message);

    return true;
  }
}
