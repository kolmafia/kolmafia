package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.preferences.Preferences;

public class WitchessRequest extends GenericRequest {
  public WitchessRequest() {
    super("choice.php");
    this.addFormField("whichchoice", "1183");
    this.addFormField("option", "2");
  }

  @Override
  public void run() {
    if (!StandardRequest.isAllowed("Items", "Witchess Set")) {
      KoLmafia.updateDisplay("Witchess is too old to use in your current path.");
      return;
    }
    if (Preferences.getBoolean("_witchessBuff")) {
      KoLmafia.updateDisplay("You already got your Witchess buff today.");
      return;
    }
    if (Preferences.getInteger("puzzleChampBonus") != 20) {
      KoLmafia.updateDisplay(
          "You cannot automatically get a Witchess buff until all puzzles are solved.");
      return;
    }
    RequestThread.postRequest(new GenericRequest("campground.php?action=witchess"));
    RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1181&option=3"));
    super.run();
  }

  @Override
  public void processResults() {
    WitchessRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static final void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("witchess.php")) {
      return;
    }

    if (responseText.contains("Puzzle Champ")) {
      Preferences.setBoolean("_witchessBuff", true);
    }
  }
}
