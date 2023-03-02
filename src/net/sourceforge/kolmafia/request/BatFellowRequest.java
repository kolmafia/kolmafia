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

    switch (action) {
      case "batman_cave_rnd":
        location = "Bat-Research and Bat-Development";
        break;
      case "batman_cave_car":
        location = "The Bat-Sedan";
        break;

        // whichplace=batman_downtown
      case "batman_downtown_hospital":
        location = "Gotpork Memorial Hospital";
        break;
      case "batman_downtown_car":
        location = "The Bat-Sedan";
        break;

        // whichplace=batman_park
      case "batman_park_car":
        location = "The Bat-Sedan";
        break;

        // whichplace=batman_slums
      case "batman_slums_car":
        location = "The Bat-Sedan";
        break;

        // whichplace=batman_industrial
      case "batman_industrial_car":
        location = "The Bat-Sedan";
        break;
      default:
        return false;
    }

    String message = "{" + BatManager.getTimeLeftString() + "} " + location;

    RequestLogger.printLine();
    RequestLogger.printLine(message);

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(message);

    return true;
  }
}
