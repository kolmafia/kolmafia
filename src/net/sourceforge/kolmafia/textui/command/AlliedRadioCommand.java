package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.AlliedRadioRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class AlliedRadioCommand extends AbstractCommand {
  public AlliedRadioCommand() {
    this.usage =
        " effect [ ellipsoidtine | intel | boon ] | item [ fuel | ordnance | rations | radio | chroner ] | misc [ sniper ] | request [ request ] - use the Allied radio";
  }

  @Override
  public void run(String cmd, String parameters) {
    String[] params = parameters.split(" ", 2);

    switch (params[0]) {
      case "effect" -> effect(params);
      case "item" -> item(params);
      case "misc" -> misc(params);
      case "request" -> request(params);
      default -> KoLmafia.updateDisplay(MafiaState.ERROR, "Usage: alliedradio" + this.usage);
    }
  }

  public static boolean lacksRadioAndBackpack() {
    return InventoryManager.getCount(ItemPool.HANDHELD_ALLIED_RADIO) == 0
        && (!InventoryManager.equippedOrInInventory(ItemPool.ALLIED_RADIO_BACKPACK)
            || Preferences.getInteger("_alliedRadioDropsUsed") >= 3);
  }

  public static int usesRemaining() {
    var uses = 0;
    uses += InventoryManager.getCount(ItemPool.HANDHELD_ALLIED_RADIO);
    if (InventoryManager.equippedOrInInventory(ItemPool.ALLIED_RADIO_BACKPACK)) {
      uses += 3 - Preferences.getInteger("_alliedRadioDropsUsed");
    }
    return uses;
  }

  private boolean lacksRadio() {
    var lack = lacksRadioAndBackpack();
    if (lack) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You need a handheld radio, or a charged backpack.");
    }
    return lack;
  }

  private void effect(String[] params) {
    if (lacksRadio()) return;

    String parameter;
    if (params.length < 2 || (parameter = params[1].trim().toLowerCase()).isEmpty()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Which effect do you want?");
      return;
    }

    String request;
    if (parameter.startsWith("ell")) {
      request = "ellipsoidtine";
    } else if (parameter.contains("intel")
        || parameter.startsWith("mat")
        || parameter.equals("item")) {
      request = "materiel intel";
    } else if (parameter.contains("sun") || parameter.contains("boon")) {
      request = "wildsun boon";
    } else {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "I don't understand what effect " + parameter + " is.");
      return;
    }
    request(request);
  }

  private void item(String[] params) {
    if (lacksRadio()) return;

    String parameter;
    if (params.length < 2 || (parameter = params[1].trim().toLowerCase()).isEmpty()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Which item do you want?");
      return;
    }

    String request;
    if (parameter.contains("fuel") || parameter.equals("booze")) {
      request = "fuel";
    } else if (parameter.equals("ordnance") || parameter.contains("grenade")) {
      request = "ordnance";
    } else if (parameter.contains("ration") || parameter.equals("food")) {
      request = "rations";
    } else if (parameter.contains("radio")) {
      request = "radio";
    } else if (parameter.contains("chroner") || parameter.contains("salary")) {
      request = "salary";
    } else {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "I don't understand what item " + parameter + " is.");
      return;
    }
    request(request);
  }

  private void misc(String[] params) {
    if (lacksRadio()) return;

    String parameter;
    if (params.length < 2 || (parameter = params[1].trim().toLowerCase()).isEmpty()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Which miscellaneous supplies do you want?");
      return;
    }

    String request;
    if (parameter.startsWith("sniper") || parameter.contains("support")) {
      request = "sniper support";
    } else {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "I don't understand what supplies " + parameter + " is.");
      return;
    }
    request(request);
  }

  private void request(String[] params) {
    if (lacksRadio()) return;

    String parameter;
    if (params.length < 2) {
      parameter = "";
    } else {
      parameter = params[1];
    }
    request(parameter);
  }

  private void request(String request) {
    (new AlliedRadioRequest(request)).run();
  }
}
