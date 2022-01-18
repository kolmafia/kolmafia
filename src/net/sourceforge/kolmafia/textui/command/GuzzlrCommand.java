package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class GuzzlrCommand extends AbstractCommand {
  public GuzzlrCommand() {
    this.usage = " [abandon | accept <bronze|gold|platinum>] - Use the Guzzlr tablet";
  }

  @Override
  public void run(final String cmd, String parameters) {
    if (!InventoryManager.hasItem(ItemPool.GUZZLR_TABLET)
        && !KoLCharacter.hasEquipped(ItemPool.GUZZLR_TABLET)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You don't have a Guzzlr tablet.");
      return;
    }

    if (parameters.startsWith("abandon")) {
      if ("unstarted".equals(Preferences.getString("questGuzzlr"))) {
        KoLmafia.updateDisplay(MafiaState.CONTINUE, "You don't have a client.");
        return;
      }
      if (Preferences.getBoolean("_guzzlrQuestAbandoned")) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You already abandoned a client today.");
        return;
      }
      KoLmafia.updateDisplay("Abandoning client");
      tap();
      runChoice(1);
      runChoice(5);
    } else if (parameters.startsWith("accept ")) {
      if ("started".equals(Preferences.getString("questGuzzlr"))) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "You already have a client, and need to abandon that client first.");
        return;
      }
      parameters = parameters.substring(7);
      Integer option = null;
      switch (parameters) {
        case "bronze":
          option = 2;
          break;
        case "gold":
          if (Preferences.getInteger("guzzlrBronzeDeliveries") < 5) {
            KoLmafia.updateDisplay(
                MafiaState.ERROR, "You need to make 5 bronze deliveries to serve gold clients.");
            return;
          }
          option = 3;
          break;
        case "platinum":
          if (Preferences.getInteger("guzzlrGoldDeliveries") < 5) {
            KoLmafia.updateDisplay(
                MafiaState.ERROR, "You need to make 5 gold deliveries to serve platinum clients.");
            return;
          }
          option = 4;
          break;
      }
      if (option == null) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "Use command 'guzzlr accept [bronze | gold | platinum]'");
        return;
      }

      KoLmafia.updateDisplay("Accepting a " + parameters + " client");
      tap();
      runChoice(option);
    } else {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Use command guzzlr " + this.usage);
    }
  }

  private void tap() {
    GenericRequest request = new GenericRequest("inventory.php?tap=guzzlr", false);
    RequestThread.postRequest(request);
  }

  private void runChoice(Integer option) {
    GenericRequest request = new GenericRequest("choice.php");
    request.addFormField("whichchoice", "1412");
    request.addFormField("option", option.toString());
    request.addFormField("pwd", GenericRequest.passwordHash);
    RequestThread.postRequest(request);
  }
}
