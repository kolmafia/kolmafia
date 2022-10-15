package net.sourceforge.kolmafia.textui.command;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class AutumnatonCommand extends AbstractCommand {
  public AutumnatonCommand() {
    this.usage = " <blank> | upgrade | send [location] - deal with your autumn-aton";
  }

  private static final Pattern UPGRADE_PATTERN = Pattern.compile("Attach the (.+) that you found.");

  @Override
  public void run(final String cmd, String parameters) {
    if (!Preferences.getBoolean("hasAutumnaton")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You need an autumn-aton.");
      return;
    }

    String[] params = parameters.split(" ", 2);

    switch (params[0]) {
      case "" -> status();
      case "send" -> send(params);
      case "upgrade" -> upgrade();
      default -> KoLmafia.updateDisplay(MafiaState.ERROR, "Usage: autumnaton" + this.usage);
    }
  }

  public void status() {
    var autumnLocation = Preferences.getString("autumnatonQuestLocation");
    if (autumnLocation.equals("")) {
      if (!InventoryManager.hasItem(ItemPool.AUTUMNATON)) {
        RequestLogger.printLine("Your autumn-aton is in an unknown location.");
      } else {
        RequestLogger.printLine("Your autumn-aton is ready to be sent somewhere.");
        String response = useAutumnaton();
        var upgrades = UPGRADE_PATTERN.matcher(response);
        if (upgrades.find()) {
          RequestLogger.printLine("Your autumn-aton has upgrades available: " + upgrades.group(1));
        }
      }
    } else {
      RequestLogger.printLine("Your autumn-aton is plundering in " + autumnLocation + ".");
      RequestLogger.printLine(turnsRemainingString());
    }
  }

  public void send(String[] params) {
    if (!InventoryManager.hasItem(ItemPool.AUTUMNATON)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Your autumn-aton is away.");
      return;
    }

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

    if (!adventure.hasSnarfblat()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, advName + " is not a valid location");
      return;
    }

    KoLmafia.updateDisplay("Sending autumn-aton to " + advName);

    useAutumnaton();

    var request =
        new GenericRequest(
            "choice.php?whichchoice=1483&option=2&heythereprogrammer="
                + adventure.getAdventureId());
    RequestThread.postRequest(request);

    var sentTo = Preferences.getString("autumnatonQuestLocation");
    if ("".equals(sentTo)) {
      // perhaps tried to access a location that was inaccessible
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "Failed to send autumnaton to " + advName + ". Is it accessible?");
    } else {
      KoLmafia.updateDisplay("Sent autumn-aton to " + sentTo + ".");
      RequestLogger.printLine(turnsRemainingString());
    }
  }

  public void upgrade() {
    String response = useAutumnaton();

    var upgrades = UPGRADE_PATTERN.matcher(response);
    if (!upgrades.find()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "No upgrades available");
      return;
    }

    GenericRequest request = new GenericRequest("choice.php?whichchoice=1483&option=1");
    RequestThread.postRequest(request);

    KoLmafia.updateDisplay("Added upgrades " + upgrades.group(1));
  }

  private String useAutumnaton() {
    GenericRequest request =
        new GenericRequest("inv_use.php?which=3&whichitem=" + ItemPool.AUTUMNATON);
    RequestThread.postRequest(request);
    return request.responseText;
  }

  private String turnsRemainingString() {
    var turns = Preferences.getInteger("autumnatonQuestTurn") - KoLCharacter.getTurnsPlayed();
    if (turns > 0) {
      var s = turns == 1 ? "" : "s";
      return "Your autumn-aton will return after " + turns + " turn" + s + ".";
    } else {
      return "Your autumn-aton will return after your next combat.";
    }
  }
}
