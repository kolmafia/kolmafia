package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class FaxCommand extends AbstractCommand {
  public FaxCommand() {
    this.usage = " send | put | receive | get - use the fax machine in your clan's VIP lounge";
  }

  @Override
  public void run(final String cmd, String parameter) {
    parameter = parameter.trim();
    if (parameter.equals("")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "What do you want to do with the fax machine?");
      return;
    }

    int option = ClanLoungeRequest.findFaxOption(parameter);
    if (option == 0) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "I don't understand what it means to '" + parameter + "' a fax.");
      return;
    }

    boolean hasPhotocopy = InventoryManager.hasItem(ItemPool.PHOTOCOPIED_MONSTER);
    if (option == 1 && !hasPhotocopy) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR,
          "You cannot send a fax without a photocopied monster in your inventory");
      return;
    }
    if (option == 2 && hasPhotocopy) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR,
          "You cannot receive a fax with a photocopied monster in your inventory");
      return;
    }

    RequestThread.postRequest(new ClanLoungeRequest(ClanLoungeRequest.FAX_MACHINE, option));
  }
}
