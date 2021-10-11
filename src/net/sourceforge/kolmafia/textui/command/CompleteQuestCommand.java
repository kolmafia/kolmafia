package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.request.PandamoniumRequest;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.DvorakManager;
import net.sourceforge.kolmafia.session.GourdManager;
import net.sourceforge.kolmafia.session.GuildUnlockManager;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.session.TavernManager;
import net.sourceforge.kolmafia.session.TowerDoorManager;

public class CompleteQuestCommand extends AbstractCommand {
  public CompleteQuestCommand() {
    this.usage = " - automatically complete quest.";
  }

  @Override
  public void run(final String command, final String parameters) {
    if (command.equals("baron")) {
      TavernManager.locateBaron();
      return;
    }

    if (command.equals("choice-goal")) {
      ChoiceManager.gotoGoal();
      return;
    }

    if (command.equals("door")) {
      TowerDoorManager.towerDoorScript();
      return;
    }

    if (command.equals("dvorak")) {
      DvorakManager.solve();
      return;
    }

    if (command.equals("gourd")) {
      GourdManager.tradeGourdItems();
      return;
    }

    if (command.equals("guild")) {
      GuildUnlockManager.unlockGuild();
      return;
    }

    if (command.equals("maze")) {
      SorceressLairManager.hedgeMazeScript(parameters.trim());
      return;
    }

    if (command.equals("sven")) {
      PandamoniumRequest.solveSven(parameters);
      return;
    }

    if (command.equals("tavern")) {
      TavernManager.locateTavernFaucet();
      return;
    }

    KoLmafia.updateDisplay("What... is your quest?  [internal error]");
  }
}
