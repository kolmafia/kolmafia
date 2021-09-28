package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.MindControlRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MonsterLevelCommand extends AbstractCommand {
  public MonsterLevelCommand() {
    this.usage = " <number> - set mind control device (or equivalent) to new value.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (!KoLCharacter.mcdAvailable()) {
      return;
    }
    int setting = StringUtilities.parseInt(parameters);
    RequestThread.postRequest(new MindControlRequest(setting));
  }
}
