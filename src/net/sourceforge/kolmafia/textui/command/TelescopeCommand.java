package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.TelescopeRequest;
import net.sourceforge.kolmafia.session.SorceressLairManager;

public class TelescopeCommand extends AbstractCommand {
  public TelescopeCommand() {
    this.usage = " [look] high | low - get daily buff, or Lair hints from your telescope.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    // Find out how good our telescope is.
    KoLCharacter.setTelescope(false);
    int upgrades = KoLCharacter.getTelescopeUpgrades();

    if (KoLCharacter.inBadMoon() && !KoLCharacter.kingLiberated() && upgrades > 0) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Your telescope is unavailable in Bad Moon.");
      return;
    }

    if (upgrades < 1) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You don't have a telescope.");
      return;
    }

    String[] split = parameters.split(" ");
    String command = split[0];

    if (command.equals("look")) {
      if (split.length < 2) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Syntax: telescope [look] high|low");
        return;
      }

      command = split[1];
    }

    if (command.equals("high")) {
      RequestThread.postRequest(new TelescopeRequest(TelescopeRequest.HIGH));
      return;
    }

    if (KoLCharacter.inBugcore()) {
      KoLmafia.updateDisplay("You see the base of the Bugbear Mothership.");
      return;
    }

    if (command.equals("low")) {
      RequestThread.postRequest(new TelescopeRequest(TelescopeRequest.LOW));
      upgrades = KoLCharacter.getTelescopeUpgrades();
    } else {
      // Make sure we've looked through the telescope since we last ascended
      KoLCharacter.checkTelescope();
    }

    RequestLogger.printLine("You have a telescope with " + (upgrades - 1) + " additional upgrades");

    int max = Math.min(upgrades, 5);
    for (int i = 0; i <= max; ++i) {
      String challenge = SorceressLairManager.getChallengeName(i);
      String test = i == 0 ? "" : Preferences.getString("nsChallenge" + i);
      String description = SorceressLairManager.getChallengeDescription(i, test);
      RequestLogger.printLine(challenge + ": " + description);
    }
  }
}
