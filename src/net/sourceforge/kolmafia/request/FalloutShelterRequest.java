package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public class FalloutShelterRequest extends GenericRequest {
  public static final int[] falloutShelterItems = {
    ItemPool.SOURCE_TERMINAL,
  };

  private static final Pattern SHELTER_PATTERN = Pattern.compile("falloutshelter/shelter(\\d+)");

  public static void reset() {
    KoLConstants.falloutShelter.clear();
  }

  private final String action;

  /** Constructs a new <code>FalloutShelterRequest</code> with the specified action in mind. */

  // place.php?whichplace=falloutshelter&action=vault1

  public FalloutShelterRequest(final String action) {
    super("place.php");
    this.addFormField("whichplace", "falloutshelter");
    if (!action.equals("")) {
      this.addFormField("action", action);
    }
    this.action = action;
  }

  /** Constructs a new <code>FalloutShelterRequest</code>. */
  public FalloutShelterRequest() {
    this("");
  }

  @Override
  public int getAdventuresUsed() {
    return this.action.equals("vault1") ? 1 : 0;
  }

  public static void setFalloutShelterItem(final int itemId, int count) {
    FalloutShelterRequest.setFalloutShelterItem(ItemPool.get(itemId, count));
  }

  private static void setFalloutShelterItem(final AdventureResult item) {
    int i = KoLConstants.falloutShelter.indexOf(item);
    if (i != -1) {
      AdventureResult old = KoLConstants.falloutShelter.get(i);
      if (old.getCount() == item.getCount()) {
        return;
      }
      KoLConstants.falloutShelter.remove(i);
    }
    KoLConstants.falloutShelter.add(item);
  }

  public static void removeFalloutShelterItem(AdventureResult item) {
    int i = KoLConstants.falloutShelter.indexOf(item);
    if (i != -1) {
      KoLConstants.falloutShelter.remove(i);
    }
  }

  @Override
  public void run() {
    // Cryo-Sleep Chamber
    if (this.action.equals("vault1")
        && KoLCharacter.getCurrentHP() == KoLCharacter.getMaximumHP()
        && KoLCharacter.getCurrentMP() == KoLCharacter.getMaximumMP()
        && !KoLConstants.activeEffects.contains(KoLAdventure.BEATEN_UP)) {
      KoLmafia.updateDisplay(MafiaState.PENDING, "You don't need to rest.");
      return;
    }

    if (this.getAdventuresUsed() > KoLCharacter.getAdventuresLeft()) {
      KoLmafia.updateDisplay(MafiaState.PENDING, "You don't have any time left for that");
      return;
    }

    super.run();
  }

  @Override
  public void processResults() {
    FalloutShelterRequest.parseResponse(this.getURLString(), this.responseText);
  }

  @Override
  protected boolean shouldFollowRedirect() {
    // May be redirected to terminal choice
    return action != null && action.equals("vault_term");
  }

  public static final void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("place.php")) {
      return;
    }

    Matcher matcher = FalloutShelterRequest.SHELTER_PATTERN.matcher(responseText);
    int shelterLevel = -1;
    while (matcher.find()) {
      shelterLevel = Integer.parseInt(matcher.group(1));
    }
    if (shelterLevel > 0) {
      Preferences.setInteger("falloutShelterLevel", shelterLevel);
      // We're in a Fallout Shelter, there must be nothing left of our campground!
      KoLConstants.campground.clear();
    }

    // Using Spa Smulation Chamber
    if (urlString.contains("action=vault3") && responseText.contains("entire day")) {
      Preferences.setBoolean("_falloutShelterSpaUsed", true);
    }

    // Using Chronodyamics Laboratory
    if (urlString.contains("action=vault5")
        && (responseText.contains("more ominous shade of green")
            || responseText.contains("heat death of the universe"))) {
      Preferences.setBoolean("falloutShelterChronoUsed", true);
    }

    // Using Main Reactor
    if (urlString.contains("action=vault8")
        && (responseText.contains("quick dip in the cooling tank")
            || responseText.contains("already bathed"))) {
      Preferences.setBoolean("falloutShelterCoolingTankUsed", true);
    }

    matcher = GenericRequest.ACTION_PATTERN.matcher(urlString);
    if (!matcher.find()) {
      FalloutShelterRequest.parseFalloutShelter(responseText);
      return;
    }
  }

  private static void parseFalloutShelter(final String responseText) {
    boolean hasTerminal = findImage(responseText, "vaultterminal.gif", ItemPool.SOURCE_TERMINAL);

    if (hasTerminal && Preferences.getString("sourceTerminalEducateKnown").equals("")) {
      // There is a Terminal, but we don't know what upgrades it has, so find out
      RequestThread.postRequest(new TerminalRequest("status"));
      RequestThread.postRequest(new TerminalRequest("educate"));
      RequestThread.postRequest(new TerminalRequest("enhance"));
      RequestThread.postRequest(new TerminalRequest("enquiry"));
      RequestThread.postRequest(new TerminalRequest("extrude"));
    }
  }

  private static boolean findImage(
      final String responseText, final String filename, final int itemId) {
    return FalloutShelterRequest.findImage(responseText, filename, itemId, false);
  }

  private static boolean findImage(
      final String responseText, final String filename, final int itemId, boolean allowMultiple) {
    int count = 0;
    int i = responseText.indexOf(filename);
    while (i != -1) {
      ++count;
      i = responseText.indexOf(filename, i + 1);
    }

    if (count > 0) {
      FalloutShelterRequest.setFalloutShelterItem(itemId, allowMultiple ? count : 1);
    }

    return (count > 0);
  }

  private static boolean findImage(
      final String responseText, final String filename, final int itemId, int count) {
    if (!responseText.contains(filename)) {
      return false;
    }

    FalloutShelterRequest.setFalloutShelterItem(itemId, count);

    return true;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("place.php")) {
      return false;
    }

    Matcher matcher = GenericRequest.ACTION_PATTERN.matcher(urlString);
    if (!matcher.find()) {
      // Simple visit. Nothing to log.
      return true;
    }

    String action = matcher.group(1);
    String message = null;

    if (action.equals("vault1")) {
      message = "[" + KoLAdventure.getAdventureCount() + "] Rest in your Cryo-Sleep Chamber";
    } else if (action.equals("vault2")) {
      // shop.php?whichshop=vault1
      // message = "Visiting your Fallout Shelter Medical Supply";
    } else if (action.equals("vault3")) {
      message = "Visiting your Spa Simulation Chamber";
    } else if (action.equals("vault4")) {
      // shop.php?whichshop=vault2
      // message = "Visiting your Fallout Shelter Electronics Supply";
    } else if (action.equals("vault5")) {
      message = "Visiting your Chronodynamics Laboratory";
    } else if (action.equals("vault6")) {
      // shop.php?whichshop=mutate
      // message = "Visiting your Gene-Sequencing Laboratory";
    } else if (action.equals("vault7")) {
      // shop.php?whichshop=vault3
      // message = "Visiting your Underground Record Store";
    } else if (action.equals("vault8")) {
      message = "Visiting your Main Reactor";
    } else {
      // Unknown action.
      return false;
    }

    if (message != null) {
      RequestLogger.printLine("");
      RequestLogger.printLine(message);

      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog(message);
    }

    return true;
  }
}
