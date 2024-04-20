package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.InventoryManager;

public class AprilBandCommand extends AbstractCommand {
  public AprilBandCommand() {
    this.usage =
        " effect [nc|c|drop] | item [instrument] | play [instrument] - participate in the apriling band";
  }

  private boolean lacksHelmet() {
    if (!InventoryManager.equippedOrInInventory(ItemPool.APRILING_BAND_HELMET)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You need an Apriling band helmet.");
      return true;
    }
    return false;
  }

  @Override
  public void run(final String cmd, String parameters) {
    String[] params = parameters.split(" ", 2);

    switch (params[0]) {
      case "effect", "conduct" -> effect(params);
      case "item" -> item(params);
      case "play", "twirl" -> play(params);
      default -> KoLmafia.updateDisplay(MafiaState.ERROR, "Usage: aprilband " + this.usage);
    }
  }

  private int turnsToNextConduct() {
    var nextConduct = Preferences.getInteger("nextAprilBandTurn");
    return nextConduct - KoLCharacter.getTurnsPlayed();
  }

  private int parameterToInstrument(String parameter) {
    if (parameter.startsWith("sax") || parameter.startsWith("luck")) {
      return ItemPool.APRIL_BAND_SAXOPHONE;
    } else if (parameter.startsWith("quad") || parameter.startsWith("tom")) {
      return ItemPool.APRIL_BAND_TOM;
    } else if (parameter.startsWith("tuba") || parameter.startsWith("nc")) {
      return ItemPool.APRIL_BAND_TUBA;
    } else if (parameter.startsWith("staff")) {
      return ItemPool.APRIL_BAND_STAFF;
    } else if (parameter.startsWith("picc") || parameter.startsWith("fam")) {
      return ItemPool.APRIL_BAND_PICCOLO;
    } else {
      return -1;
    }
  }

  private void conduct(int choice) {
    KoLmafia.updateDisplay("Conducting!");
    GenericRequest request = new GenericRequest("inventory.php?action=apriling");
    RequestThread.postRequest(request);
    ChoiceManager.processChoiceAdventure(choice, "", false);
    ChoiceManager.processChoiceAdventure(9, "", true);
  }

  private void effect(String[] params) {
    if (lacksHelmet()) return;

    int turns = turnsToNextConduct();
    if (turns > 0) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "You cannot change your conduct (" + turns + " turns to go).");
      return;
    }

    String parameter;
    if (params.length < 2 || (parameter = params[1].trim()).isEmpty()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Which effect do you want?");
      return;
    }

    int choice;
    if (parameter.startsWith("nc") || parameter.startsWith("non")) {
      choice = 1;
    } else if (parameter.startsWith("c")) {
      choice = 2;
    } else if (parameter.startsWith("drop")) {
      choice = 3;
    } else {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "I don't understand what effect " + parameter + " is.");
      return;
    }

    conduct(choice);
  }

  private void item(String[] params) {
    if (lacksHelmet()) return;

    int instruments = Preferences.getInteger("_aprilBandInstruments");
    if (instruments == 2) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You cannot get any more instruments.");
      return;
    }

    String parameter;
    if (params.length < 2 || (parameter = params[1].trim()).isEmpty()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Which instrument do you want?");
      return;
    }

    int instrument = parameterToInstrument(parameter);
    int choice;
    switch (instrument) {
      case ItemPool.APRIL_BAND_SAXOPHONE -> choice = 4;
      case ItemPool.APRIL_BAND_TOM -> choice = 5;
      case ItemPool.APRIL_BAND_TUBA -> choice = 6;
      case ItemPool.APRIL_BAND_STAFF -> choice = 7;
      case ItemPool.APRIL_BAND_PICCOLO -> choice = 8;
      default -> {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "I don't understand what instrument " + parameter + " is.");
        return;
      }
    }

    conduct(choice);
  }

  private void play(String[] params) {
    String parameter;
    if (params.length < 2 || (parameter = params[1].trim()).isEmpty()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Which instrument do you want to play?");
      return;
    }

    int instrument = parameterToInstrument(parameter);

    var item = ItemPool.get(instrument);
    if (!InventoryManager.equippedOrInInventory(instrument)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You don't have an " + item.getName() + ".");
      return;
    }

    KoLmafia.updateDisplay("Playing " + item.getName());

    var request =
        new GenericRequest(
            "inventory.php?action=aprilplay&iid="
                + instrument
                + "&pwd="
                + GenericRequest.passwordHash,
            false);
    RequestThread.postRequest(request);

    KoLCharacter.updateStatus();
  }
}
