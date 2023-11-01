package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BurningLeavesRequest extends GenericRequest {
  public BurningLeavesRequest(final int leaves) {
    super("choice.php");

    this.addFormField("whichchoice", "1510");
    this.addFormField("leaves", String.valueOf(leaves));
    this.addFormField("option", "1");
  }

  @Override
  protected boolean shouldFollowRedirect() {
    return true;
  }

  @Override
  public void run() {
    if (GenericRequest.abortIfInFightOrChoice()) {
      return;
    }

    if (!KoLConstants.campground.contains(ItemPool.get(ItemPool.A_GUIDE_TO_BURNING_LEAVES))) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You do not have a pile of burning leaves.");
      return;
    }

    GenericRequest req = new GenericRequest("campground.php?preaction=leaves", false);
    req.run();

    // Redirects to choice.php?forceoption=0
    super.run();
  }

  private static final Pattern LEAVES_BURNED_PATTERN =
      Pattern.compile("You've stoked the fire with <b>(\\d)</b> random lea(?:f|ves) today\\.");

  public static void visitChoice(final String responseText) {
    Preferences.setBoolean("_leavesJumped", !responseText.contains("Jump in the Flames"));

    Matcher matcher = BurningLeavesRequest.LEAVES_BURNED_PATTERN.matcher(responseText);
    if (matcher.find()) {
      int leavesBurned = Integer.parseInt(matcher.group(1));
      Preferences.setInteger("_leavesBurned", leavesBurned);
    }
  }

  public static void postChoice(final String responseText, final int leaves) {
    if (responseText.contains(
        "You jump in the blazing fire absorb some of the flames and jump out")) {
      Preferences.setBoolean("_leavesJumped", true);
      return;
    }

    if (responseText.contains("You can't thrown in none leaves.")
        || responseText.contains("You don't have that many leaves!")) {
      return;
    }

    Preferences.increment("_leavesBurned", leaves);
    ResultProcessor.processItem(ItemPool.INFLAMMABLE_LEAF, leaves * -1);

    // Non-random amounts are limited daily, we'll need to handle that here.
  }

  public static final Pattern URL_LEAVES_PATTERN = Pattern.compile("leaves=(\\d+)");

  public static int extractLeavesFromURL(final String urlString) {
    Matcher matcher = URL_LEAVES_PATTERN.matcher(urlString);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
  }

  public static void registerLeafFight(final String location) {
    Preferences.increment("_leafMonstersFought", 1, 5, false);
    var leaves = extractLeavesFromURL(location);
    ResultProcessor.processItem(ItemPool.INFLAMMABLE_LEAF, leaves * -1);
  }
}
