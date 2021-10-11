package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.session.ResultProcessor;

public class TrophyHutRequest extends GenericRequest {
  private static final Pattern WHICHTROPHY_PATTERN = Pattern.compile("whichtrophy=(\\d*)");

  public TrophyHutRequest() {
    super("trophy.php");
  }

  @Override
  public void processResults() {
    TrophyHutRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static final void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("trophy.php")) {
      return;
    }

    String action = GenericRequest.getAction(urlString);
    if (action == null || !action.equals("buytrophy")) {
      return;
    }

    // You can't afford to have a trophy installed.
    // Your trophy has been installed at your campsite.
    if (responseText.indexOf("Your trophy has been installed at your campsite") != -1) {
      RequestLogger.updateSessionLog("You spent 10,000 Meat");
      ResultProcessor.processMeat(-10000);
    }
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("trophy.php")) {
      return false;
    }

    String action = GenericRequest.getAction(urlString);
    if (action == null) {
      // Don't log simple visits
      return true;
    }

    if (!action.equals("buytrophy")) {
      // Don't claim unknown actions
      return false;
    }

    Matcher matcher = TrophyHutRequest.WHICHTROPHY_PATTERN.matcher(urlString);
    if (!matcher.find()) {
      // Missing trophy field
      return true;
    }

    String message = "Buying trophy #" + matcher.group(1) + " at the Trophy Hut";

    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);

    return true;
  }
}
