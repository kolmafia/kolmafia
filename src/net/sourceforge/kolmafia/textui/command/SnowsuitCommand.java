package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;

public class SnowsuitCommand extends AbstractCommand {
  public static final String[][] DECORATION = {
    {"eyebrows", "1"},
    {"smirk", "2"},
    {"nose", "3"},
    {"goatee", "4"},
    {"hat", "5"},
  };

  public SnowsuitCommand() {
    this.usage = "[?] <decoration> - decorate Snowsuit (and equip it if unequipped)";
  }

  @Override
  public void run(final String cmd, String parameters) {
    String currentDecoration = Preferences.getString("snowsuit");

    if (parameters.length() == 0) {
      KoLmafia.updateDisplay("Current decoration on Snowsuit is " + currentDecoration);
      return;
    }

    String decoration = parameters;
    String choice = "0";

    for (int i = 0; i < SnowsuitCommand.DECORATION.length; i++) {
      if (decoration.equalsIgnoreCase(SnowsuitCommand.DECORATION[i][0])) {
        choice = SnowsuitCommand.DECORATION[i][1];
        decoration = SnowsuitCommand.DECORATION[i][0];
        break;
      }
    }

    if (choice.equals("0")) {
      KoLmafia.updateDisplay(
          "Decoration "
              + decoration
              + " not recognised. Valid values are eyebrows, goatee, hat, nose and smirk");
      return;
    }

    if (EquipmentManager.getEquipment(EquipmentManager.FAMILIAR).getItemId()
        != ItemPool.SNOW_SUIT) {
      AdventureResult snowsuit = ItemPool.get(ItemPool.SNOW_SUIT);
      RequestThread.postRequest(new EquipmentRequest(snowsuit, EquipmentManager.FAMILIAR));
    }

    if (decoration.equalsIgnoreCase(currentDecoration)) {
      KoLmafia.updateDisplay("Decoration " + decoration + " already equipped.");
      return;
    }

    if (KoLmafia.permitsContinue()) {
      RequestThread.postRequest(new GenericRequest("inventory.php?action=decorate"));
    }
    if (KoLmafia.permitsContinue()) {
      RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=640&option=" + choice));
    }
    if (KoLmafia.permitsContinue()) {
      KoLmafia.updateDisplay("Snowsuit decorated with " + decoration + ".");
    }
  }
}
