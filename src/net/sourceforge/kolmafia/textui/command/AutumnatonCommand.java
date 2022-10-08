package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class AutumnatonCommand extends AbstractCommand {
  public AutumnatonCommand() {
    this.usage = " <blank> | send [location] - deal with your autumn-aton";
  }

  @Override
  public void run(final String cmd, String parameters) {
    if (!InventoryManager.hasItem(ItemPool.AUTUMNATON)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You need an autumn-aton to send.");
      return;
    }

    String[] params = parameters.split(" ", 2);

    switch (params[0]) {
      case "send" -> send(params);
      default -> KoLmafia.updateDisplay(MafiaState.ERROR, "autumnaton " + this.usage);
    }
  }

  public void send(String[] params) {
    String parameter;
    if (params.length < 2 || (parameter = params[1].trim()).equals("")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Where do you want to send the little guy?");
      return;
    }

    KoLAdventure adventure = AdventureDatabase.getAdventure(parameter);
    if (adventure == null) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "I don't understand where " + parameter + " is.");
      return;
    }

    var advName = adventure.getAdventureName();
    KoLmafia.updateDisplay("Sending autumn-aton to " + advName);

    GenericRequest request =
        new GenericRequest("inv_use.php?which=3&whichitem=" + ItemPool.AUTUMNATON);
    RequestThread.postRequest(request);

    request.constructURLString(
        "choice.php?whichchoice=1483&option=2&heythereprogrammer=" + adventure.getAdventureId());
    RequestThread.postRequest(request);

    var sentTo = Preferences.getString("autumnatonQuestLocation");
    if ("".equals(sentTo)) {
      // perhaps tried to access a location that was inaccessible
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "Failed to send autumnaton to " + advName + ". Is it accessible?");
    } else {
      KoLmafia.updateDisplay("Sent autumn-aton to " + sentTo + ".");
    }
  }
}
