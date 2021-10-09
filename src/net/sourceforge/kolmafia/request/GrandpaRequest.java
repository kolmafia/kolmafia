package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

public class GrandpaRequest extends GenericRequest {
  private static final Pattern WHO_PATTERN = Pattern.compile("who=(\\d*)");
  private static final Pattern QUERY_PATTERN = Pattern.compile("topic=([^&]*)");

  public GrandpaRequest() {
    this(null);
  }

  public GrandpaRequest(final String story) {
    super("monkeycastle.php");
    this.addFormField("action", "grandpastory");
    if (story != null) {
      this.addFormField("topic", story);
    }
  }

  @Override
  public void processResults() {
    // You can't visit the Sea Monkees without some way of
    // breathing underwater.

    if (this.responseText.indexOf("can't visit the Sea Monkees") != -1) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You're not equipped to visit the Sea Monkees.");
    }
  }

  public static final String findNPC(final int npc) {
    switch (npc) {
      case 1:
        return "Little Brother";
      case 2:
        return "Big Brother";
      case 3:
        return "Grandpa";
      case 4:
        return "Grandma";
    }

    return "Unknown Sea Monkey";
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("monkeycastle.php")) {
      return false;
    }

    Matcher matcher = WHO_PATTERN.matcher(urlString);
    String action = GenericRequest.getAction(urlString);
    if (matcher.find() && action == null) {
      // Simple visit with no action
      String NPC = GrandpaRequest.findNPC(Integer.parseInt(matcher.group(1)));
      RequestLogger.updateSessionLog("Visiting " + NPC);
      return true;
    }

    if (action == null) {
      return false;
    }

    if (!action.equals("grandpastory")) {
      return false;
    }

    matcher = QUERY_PATTERN.matcher(urlString);
    if (!matcher.find()) {
      return true;
    }

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog("grandpa " + GenericRequest.decodeField(matcher.group(1)));

    return true;
  }
}
