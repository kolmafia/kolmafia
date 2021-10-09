package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class WaitCommand extends AbstractCommand {
  public WaitCommand() {
    this.usage = " [<seconds>] - pause script execution (default 1 second).";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    int seconds = StringUtilities.parseInt(parameters);

    if (seconds <= 0) {
      seconds = 1;
    }

    if (cmd.equals("waitq")) {
      PauseObject pauser = new PauseObject();
      for (int i = seconds; i > 0 && KoLmafia.permitsContinue(); --i) pauser.pause(1000);
    } else {
      StaticEntity.executeCountdown("Countdown: ", seconds);
      KoLmafia.updateDisplay("Waiting completed.");
    }
  }
}
