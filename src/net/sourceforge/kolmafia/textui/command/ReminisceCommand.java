package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.request.LocketRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ReminisceCommand extends AbstractCommand {
  public ReminisceCommand() {
    this.usage = "[?] <monster> - reminisce monster.";
  }

  @Override
  public void run(final String cmd, String parameters) {
    parameters = parameters.trim();

    if (parameters.length() == 0) {
      KoLmafia.updateDisplay(KoLConstants.MafiaState.ERROR, "No monster specified.");
      return;
    }

    MonsterData monster =
        StringUtilities.isNumeric(parameters)
            ? MonsterDatabase.findMonsterById(Integer.parseInt(parameters))
            : MonsterDatabase.findMonster(parameters, true, false);

    if (monster == null) {
      KoLmafia.updateDisplay(
          KoLConstants.MafiaState.ERROR, parameters + " does not match a monster.");
      return;
    }

    if (KoLmafiaCLI.isExecutingCheckOnlyCommand) {
      RequestLogger.printLine(monster.toString());
      return;
    }

    RequestThread.postRequest(new LocketRequest(monster));
  }
}
