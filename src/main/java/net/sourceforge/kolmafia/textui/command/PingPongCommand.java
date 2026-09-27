package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.CurseRequest;

public class PingPongCommand extends AbstractCommand {
  public PingPongCommand() {
    this.usage = "<player> - Play ping-pong with somebody.";
  }

  @Override
  public void run(String command, String parameters) {
    if (parameters.equals("")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Play ping-pong with whom?");
      return;
    }

    AdventureResult item = ItemPool.get(ItemPool.PING_PONG_TABLE);
    String target = parameters.trim();
    RequestThread.postRequest(new CurseRequest(item, target, ""));
  }
}
