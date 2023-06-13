package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest.Action;

public class HotTubCommand extends AbstractCommand {
  public HotTubCommand() {
    this.usage = " - soak in your clan's hot tub";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    RequestThread.postRequest(new ClanLoungeRequest(Action.HOTTUB));
  }
}
