package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MoonPhaseRequest extends GenericRequest {
  private static final Pattern MOONS_PATTERN =
      Pattern.compile("moon(.)[ab]?\\.gif.*moon(.)[ab]?\\.gif");

  /**
   * The phases of the moons can be retrieved from the top menu, which varies based on whether or
   * not the player is using compact mode.
   */
  public MoonPhaseRequest() {
    super("topmenu.php");
  }

  @Override
  protected boolean retryOnTimeout() {
    return true;
  }

  @Override
  protected boolean shouldFollowRedirect() {
    return true;
  }

  /** Runs the moon phase request, updating as appropriate. */
  @Override
  public void run() {
    KoLmafia.updateDisplay("Synchronizing moon data...");
    super.run();
  }

  @Override
  public void processResults() {
    String text = this.responseText;

    // We can no longer count on knowing the menu style from api.php
    GenericRequest.topMenuStyle =
        text.indexOf("awesomemenu.php") != -1
            ? GenericRequest.MENU_FANCY
            : text.indexOf("Function:") != -1
                ? GenericRequest.MENU_COMPACT
                : GenericRequest.MENU_NORMAL;

    // Get current phase of Ronald and Grimace
    if (text.contains("minimoon")) {
      text = text.replaceAll("minimoon", "");
    }

    Matcher moonMatcher = MoonPhaseRequest.MOONS_PATTERN.matcher(text);
    if (moonMatcher.find()) {
      HolidayDatabase.setMoonPhases(
          StringUtilities.parseInt(moonMatcher.group(1)) - 1,
          StringUtilities.parseInt(moonMatcher.group(2)) - 1);
    }
  }
}
