package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.*;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ScrapheapRequest extends PlaceRequest {
  public ScrapheapRequest() {
    super("scrapheap");
  }

  public ScrapheapRequest(final String action) {
    super("scrapheap", action);
  }

  public static void refresh() {
    if (KoLCharacter.inRobocore()) {
      RequestThread.postRequest(new ScrapheapRequest());
    }
  }

  @Override
  public void processResults() {
    ScrapheapRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    String action = GenericRequest.getAction(urlString);

    if (action == null || action.startsWith("sh_chrono")) {
      parseChronolith(responseText);
      return;
    }

    if (action.startsWith("sh_getpower")) {
      parseCollectEnergy(responseText);
      return;
    }

    if (action.startsWith("sh_scrounge")) {
      Preferences.setBoolean("youRobotScavenged", true);
      return;
    }
  }

  private static final Pattern CHRONOLITH_COST = Pattern.compile("title=\"\\((\\d+) Energy\\)\"");

  private static void parseChronolith(final String responseText) {
    Matcher m = CHRONOLITH_COST.matcher(responseText);

    if (m.find()) {
      int cost = StringUtilities.parseInt(m.group(1));

      if (cost > 148) {
        cost /= 10;
      } else if (cost > 47) {
        cost /= 2;
      }

      cost -= 10;

      Preferences.setInteger("_chronolithActivations", cost);
    }
  }

  private static final Pattern ENERGY_GAIN = Pattern.compile("You gain (\\d+) Energy.");

  private static void parseCollectEnergy(final String responseText) {
    Matcher m = ENERGY_GAIN.matcher(responseText);

    if (m.find()) {
      // Progression went 25, 21, 18, 13, 11, 10, 8, 6 ,6 ,5?
      // int gain = StringUtilities.parseInt( m.group( 1 ) );
      Preferences.increment("_energyCollected");
    }
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("place.php") || !urlString.contains("whichplace=scrapheap")) {
      return false;
    }

    String action = GenericRequest.getAction(urlString);
    if (action == null) {
      // Nothing to log for simple visits
      return true;
    }

    String message = null;

    if (action.startsWith("sh_chrono")) {
      message = "Activating the Chronolith";
    }
    if (action.startsWith("sh_upgrade")) {
      // Visiting the Reassembly Station
      return true;
    }
    if (action.startsWith("sh_getpower")) {
      message = "[" + KoLAdventure.getAdventureCount() + "] Collecting energy";
    }
    if (action.startsWith("sh_scrounge")) {
      message = "[" + KoLAdventure.getAdventureCount() + "] Scavenging scrap";
    } else if (action.startsWith("sh_configure")) {
      return true;
    }

    if (message == null) {
      // Log URL for anything else
      return false;
    }

    RequestLogger.printLine();
    RequestLogger.printLine(message);

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(message);

    return true;
  }
}
