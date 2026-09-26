package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;

public class SnapperCommand extends AbstractCommand {
  public SnapperCommand() {
    this.usage = " [PHYLUM] - guide your Red Snapper to a certain phylum";
  }

  @Override
  public void run(final String cmd, String parameter) {
    FamiliarData current = KoLCharacter.getFamiliar();
    if (current == null || current.getId() != FamiliarPool.RED_SNAPPER) {
      KoLmafia.updateDisplay("You need to take your Red-Nosed Snapper with you");
      return;
    }

    parameter = parameter.trim();

    if (parameter.equals("")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Which monster phylum do you want?");
      return;
    }

    Phylum phylum = Phylum.find(parameter);
    if (phylum == Phylum.NONE) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "What kind of random monster is a " + parameter + "?");
      return;
    }

    if (phylum == Phylum.find(Preferences.getString("redSnapperPhylum"))) {
      KoLmafia.updateDisplay(
          "Your Red-Nosed Snapper is already hot on the tail of any "
              + phylum.toString()
              + " it can see");
      return;
    }

    RequestThread.postRequest(new GenericRequest("familiar.php?action=guideme"));
    RequestThread.postRequest(
        new GenericRequest("choice.php?whichchoice=1396&option=1&cat=" + phylum.toToken()));
    KoLmafia.updateDisplay(
        "Your Red-Nosed Snapper is now guiding you towards " + phylum.getPlural());
    KoLCharacter.updateStatus();
  }
}
