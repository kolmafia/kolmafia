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
    this.usage = " [location] - send your autumn-aton off somewhere";
  }

  @Override
  public void run(final String cmd, String parameter) {
    if (!InventoryManager.hasItem(ItemPool.AUTUMNATON)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You need an autumn-aton to send.");
      return;
    }

    parameter = parameter.trim();

    if (parameter.equals("")) {
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

    var request = new GenericRequest("choice.php");
    request.addFormField("whichchoice", "1483");
    request.addFormField("option", "2");
    request.addFormField("pwd", GenericRequest.passwordHash);
    request.addFormField("heythereprogrammer", adventure.getAdventureId());
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
