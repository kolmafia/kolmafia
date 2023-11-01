package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

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
    if (responseText.contains("You've absorbed the power of fire")) {
      Preferences.setBoolean("_leavesJumped", true);
      return;
    }

    if (!responseText.contains("You throw the leaves in the fire")) {
      return;
    }

    Preferences.increment("_leavesBurned", leaves);

    // Non-random amounts are limited daily, we'll need to handle that here.
  }
}
