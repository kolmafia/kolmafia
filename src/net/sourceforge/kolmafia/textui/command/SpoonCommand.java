package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

public class SpoonCommand extends AbstractCommand {
  public SpoonCommand() {
    this.usage = " [SIGN] - use your spoon to change your moon sign";
  }

  @Override
  public void run(final String cmd, String parameter) {
    if (!InventoryManager.hasItem(ItemPool.HEWN_MOON_RUNE_SPOON)
        && !KoLCharacter.hasEquipped(ItemPool.HEWN_MOON_RUNE_SPOON)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You need a hewn moon-rune spoon first.");
      return;
    }

    if (Preferences.getBoolean("moonTuned")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You have already tuned the moon this ascension.");
      return;
    }

    parameter = parameter.trim();

    if (parameter.equals("")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Which sign do you want to change to?");
      return;
    }

    ZodiacSign currentSign = ZodiacSign.find(KoLCharacter.getSign());
    if (currentSign == ZodiacSign.BAD_MOON) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You can't escape the Bad Moon this way.");
      return;
    }

    ZodiacSign sign = ZodiacSign.find(parameter);
    if (sign == ZodiacSign.NONE) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "I don't understand what sign " + parameter + " is.");
      return;
    }
    if (sign == ZodiacSign.BAD_MOON) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You can't choose to be born under a Bad Moon.");
      return;
    }
    if (sign == currentSign) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "No need to change, you're already a " + currentSign + ".");
      return;
    }

    AdventureResult spoon = ItemPool.get(ItemPool.HEWN_MOON_RUNE_SPOON);
    int slot = KoLCharacter.equipmentSlot(spoon);
    if (slot != EquipmentManager.NONE) {
      RequestThread.postRequest(new EquipmentRequest(EquipmentRequest.UNEQUIP, slot));
    }

    String buf =
        "inv_use.php?whichitem="
            + ItemPool.HEWN_MOON_RUNE_SPOON
            + "&pwd="
            + GenericRequest.passwordHash
            + "&doit=96&whichsign="
            + sign.getId();
    RequestThread.postRequest(new GenericRequest(buf));

    KoLmafia.updateDisplay("Tuning moon to " + sign);
    if (slot != EquipmentManager.NONE) {
      RequestThread.postRequest(new EquipmentRequest(spoon, slot));
    }
    KoLCharacter.updateStatus();
  }
}
