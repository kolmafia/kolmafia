package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.GenericRequest;

public class PhotoBoothCommand extends AbstractCommand {
  public PhotoBoothCommand() {
    this.usage = " effect [ wild | tower | space ] | item [ item ] - get an effect or item";
  }

  @Override
  public void run(final String cmd, String parameters) {
    String[] params = parameters.split(" ", 2);

    switch (params[0]) {
      case "effect" -> effect(params);
      case "item", "prop" -> item(params);
      default -> KoLmafia.updateDisplay(MafiaState.ERROR, "Usage: photobooth" + this.usage);
    }
  }

  private boolean lacksPhotoBooth() {
    if (!ClanLoungeRequest.hasClanLoungeItem(ItemPool.get(ItemPool.CLAN_PHOTO_BOOTH))) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Your clan needs a photo booth.");
      return true;
    }
    return false;
  }

  private void effect(String[] params) {
    if (lacksPhotoBooth()) return;

    int effects = Preferences.getInteger("_photoBoothEffects");
    if (effects >= 3) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You cannot get any more effects.");
      return;
    }

    String parameter;
    if (params.length < 2 || (parameter = params[1].trim()).isEmpty()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Which effect do you want?");
      return;
    }

    int choice;
    if (parameter.startsWith("wild")) {
      choice = 1;
    } else if (parameter.startsWith("tower")) {
      choice = 2;
    } else if (parameter.startsWith("space")) {
      choice = 3;
    } else {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "I don't understand what effect " + parameter + " is.");
      return;
    }

    RequestThread.postRequest(new GenericRequest("clan_viplounge.php?action=photobooth"));
    RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1533&option=1"));
    RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1534&option=" + choice));
    RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1533&option=6"));
  }

  private void item(String[] params) {
    if (lacksPhotoBooth()) return;

    int equip = Preferences.getInteger("_photoBoothEquipment");
    if (equip >= 3) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You cannot get any more props.");
      return;
    }

    String parameter;
    if (params.length < 2 || (parameter = params[1].trim()).isEmpty()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Which item do you want?");
      return;
    }

    int choice;
    if (parameter.startsWith("photo") || parameter.contains("list")) {
      choice = 1;
    } else if (parameter.contains("arrow")) {
      choice = 2;
    } else if (parameter.contains("beard")) {
      choice = 3;
    } else if (parameter.startsWith("astronaut") || parameter.contains("helmet")) {
      choice = 4;
    } else if (parameter.startsWith("cheap") || parameter.contains("pipe")) {
      choice = 5;
    } else if (parameter.startsWith("over") || parameter.contains("monocle")) {
      choice = 6;
    } else if (parameter.startsWith("giant") || parameter.contains("bow")) {
      choice = 7;
    } else if (parameter.startsWith("feather") || parameter.contains("boa")) {
      choice = 8;
    } else if (parameter.contains("badge")) {
      choice = 9;
    } else if (parameter.contains("pistol")) {
      choice = 10;
    } else if (parameter.contains("moustache")) {
      choice = 11;
    } else {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "I don't understand what item " + parameter + " is.");
      return;
    }

    RequestThread.postRequest(new GenericRequest("clan_viplounge.php?action=photobooth"));
    RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1533&option=2"));
    RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1535&option=" + choice));
    RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1533&option=6"));
  }
}
