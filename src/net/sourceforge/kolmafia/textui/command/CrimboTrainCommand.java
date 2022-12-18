package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.CurseRequest;

public class CrimboTrainCommand extends AbstractCommand {
  public CrimboTrainCommand() {
    this.usage = "<player> - Offer Crimbo Training to someone else";
  }

  @Override
  public void run(String command, String parameters) {
    if (parameters.equals("")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Train who?");
      return;
    }

    AdventureResult item = ItemPool.get(ItemPool.CRIMBO_TRAINING_MANUAL);
    String target = parameters.trim();
    RequestThread.postRequest(new CurseRequest(item, target, ""));
  }
}
