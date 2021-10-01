package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.session.RabbitHoleManager;

public class ChessCommand extends AbstractCommand {
  public ChessCommand() {
    this.usage = " load config | board | test | solve - play on the Great Big Chessboard.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    String[] split = parameters.split(" ");
    String command = split[0];

    if (command.equals("reset")) {
      RabbitHoleManager.reset();
      return;
    }

    if (command.equals("load")) {
      if (split.length < 2) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Load what board?");
        return;
      }

      RabbitHoleManager.load(split[1], true);
      return;
    }

    if (command.equals("board")) {
      RabbitHoleManager.board();
      return;
    }

    if (command.equals("test")) {
      RabbitHoleManager.test();
      return;
    }

    if (command.equals("solve")) {
      RabbitHoleManager.solve();
      return;
    }

    KoLmafia.updateDisplay(MafiaState.ERROR, "What do you want to do with the chessboard?");
  }
}
