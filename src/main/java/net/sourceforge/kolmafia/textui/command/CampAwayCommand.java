package net.sourceforge.kolmafia.textui.command;

import java.util.List;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CampAwayRequest;

public class CampAwayCommand extends AbstractCommand {
  public CampAwayCommand() {
    this.usage = " cloud | smile - get a getaway campsite buff";
  }

  @Override
  public void run(String cmd, String parameters) {
    if (!List.of("cloud", "smile").contains(parameters)) {
      KoLmafia.updateDisplay(KoLConstants.MafiaState.ERROR, "Campaway command not recognized");
      return;
    }

    if (!Preferences.getBoolean("getawayCampsiteUnlocked")) {
      KoLmafia.updateDisplay(KoLConstants.MafiaState.ERROR, "You need a Getaway Campsite");
      return;
    }

    if (parameters.equals("cloud") && Preferences.getInteger("_campAwayCloudBuffs") >= 1) {
      KoLmafia.updateDisplay(KoLConstants.MafiaState.ERROR, "Already got a cloud buff today");
      return;
    } else if (parameters.equals("smile") && Preferences.getInteger("_campAwaySmileBuffs") >= 3) {
      KoLmafia.updateDisplay(KoLConstants.MafiaState.ERROR, "Already used all smile buffs today");
      return;
    }

    new CampAwayRequest(CampAwayRequest.SKY).run();
  }
}
