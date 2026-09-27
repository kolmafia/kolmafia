package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.utilities.PauseObject;

public class WinGameCommand extends AbstractCommand {
  public WinGameCommand() {
    this.usage = " - I'm as surprised as you!  I didn't think it was possible.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    String[] messages =
        KoLConstants.WIN_GAME_TEXT[KoLConstants.RNG.nextInt(KoLConstants.WIN_GAME_TEXT.length)];

    PauseObject pauser = new PauseObject();
    KoLmafia.updateDisplay("Executing top-secret 'win game' script...");
    pauser.pause(3000);

    for (int i = 0; i < messages.length - 1; ++i) {
      KoLmafia.updateDisplay(messages[i]);
      pauser.pause(3000);
    }

    KoLmafia.updateDisplay(MafiaState.ERROR, messages[messages.length - 1]);
  }
}
