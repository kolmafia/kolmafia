package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.preferences.Preferences;

public class LedCandleRequest extends GenericRequest {
  public LedCandleRequest() {
    super("choice.php");
  }

  public static final void parseUpgrade(final String urlString, final String responseText) {
    if (!urlString.contains("whichchoice=1509")) {
      return;
    }

    // choice 1, item
    // Disco ball activated.
    if (urlString.contains("option=1") && responseText.contains("Disco")) {
      Preferences.setString("ledCandleMode", "disco");
    }

    // choice 2, meat
    // Ultraviolet light activated, meat will be revealed.
    else if (urlString.contains("option=2") && responseText.contains("Ultraviolet")) {
      Preferences.setString("ledCandleMode", "ultraviolet");
    }

    // choice 3, stats
    // Reading light activated, studying optimized.
    else if (urlString.contains("option=3") && responseText.contains("Reading light")) {
      Preferences.setString("ledCandleMode", "reading");
    }

    // choice 4, attacks
    // You don't have to put on the red light! Except you did and now your Jill-of-All-Trades is
    // raring for a fight.
    else if (urlString.contains("option=4") && responseText.contains("red light")) {
      Preferences.setString("ledCandleMode", "red light");
    }
  }
}
