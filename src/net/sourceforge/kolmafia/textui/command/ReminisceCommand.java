package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.LocketManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ReminisceCommand extends AbstractCommand {
  public ReminisceCommand() {
    this.usage = "[?] <monster> - reminisce monster.";
  }

  @Override
  public void run(final String cmd, String parameters) {
    if (!LocketManager.own()) {
      KoLmafia.updateDisplay(
          KoLConstants.MafiaState.ERROR, "You do not have a combat lover's locket to hand.");
      return;
    }

    if (LocketManager.getFoughtMonsters().size() >= 3) {
      KoLmafia.updateDisplay(KoLConstants.MafiaState.ERROR, "You can only reminisce thrice daily.");
      return;
    }

    if (parameters.length() == 0) {
      KoLmafia.updateDisplay(KoLConstants.MafiaState.ERROR, "No monster specified.");
      return;
    }

    parameters = parameters.trim();

    MonsterData monster;

    if (StringUtilities.isNumeric(parameters)) {
      monster = MonsterDatabase.findMonsterById(Integer.parseInt(parameters));
    } else {
      monster = MonsterDatabase.findMonster(parameters, true, false);
    }

    if (monster == null) {
      KoLmafia.updateDisplay(
          KoLConstants.MafiaState.ERROR, parameters + " does not match a monster.");
      return;
    }

    if (LocketManager.foughtMonster(monster)) {
      KoLmafia.updateDisplay(
          KoLConstants.MafiaState.ERROR,
          "You've already reminisced "
              + monster.getArticle()
              + " "
              + monster.getName()
              + " today.");
      return;
    }

    if (!LocketManager.remembersMonster(monster)) {
      KoLmafia.updateDisplay(
          KoLConstants.MafiaState.ERROR,
          "You do not have a picture of "
              + monster.getArticle()
              + " "
              + monster.getName()
              + " in your locket.");
      return;
    }

    if (KoLmafiaCLI.isExecutingCheckOnlyCommand) {
      RequestLogger.printLine(monster.toString());
      return;
    }

    RequestThread.postRequest(new GenericRequest("inventory.php?reminisce=1", false));

    RequestThread.postRequest(
        new GenericRequest("choice.php?pwd&whichchoice=1463&option=1&mid=" + monster.getId()));
  }
}
