package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BoomBoxCommand extends AbstractCommand {
  public BoomBoxCommand() {
    this.usage =
        " [giger | spooky | food | alive | dr | fists | damage | meat | silent | off | # ] - get the indicated buff";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (!InventoryManager.hasItem(ItemPool.BOOMBOX)) {
      KoLmafia.updateDisplay("You need a SongBoom&trade; BoomBox first.");
      return;
    }
    int choice = StringUtilities.parseInt(parameters);
    if (choice < 1 || choice > 6) {
      if (parameters.contains("giger") || parameters.contains("spooky")) {
        choice = 1;
      } else if (parameters.contains("food")) {
        choice = 2;
      } else if (parameters.contains("alive") || parameters.contains("dr")) {
        choice = 3;
      } else if (parameters.contains("fists") || parameters.contains("damage")) {
        choice = 4;
      } else if (parameters.contains("meat")) {
        choice = 5;
      } else if (parameters.contains("silent") || parameters.contains("off")) {
        choice = 6;
      }
    }
    if (choice < 1 || choice > 7) {
      KoLmafia.updateDisplay(MafiaState.ERROR, parameters + " is not a valid option.");
      return;
    }
    if (choice == 1 && Preferences.getString("boomBoxSong").equals("Eye of the Giger")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You have already got Eye of the Giger playing.");
      return;
    }
    if (choice == 2 && Preferences.getString("boomBoxSong").equals("Food Vibrations")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You have already got Food Vibrations playing.");
      return;
    }
    if (choice == 3 && Preferences.getString("boomBoxSong").equals("Remainin' Alive")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You have already got Remainin' Alive playing.");
      return;
    }
    if (choice == 4
        && Preferences.getString("boomBoxSong").equals("These Fists Were Made for Punchin'")) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "You have already got These Fists Were Made for Punchin' playing.");
      return;
    }
    if (choice == 5 && Preferences.getString("boomBoxSong").equals("Total Eclipse of Your Meat")) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "You have already got Total Eclipse of Your Meat playing.");
      return;
    }
    if (choice == 6 && Preferences.getString("boomBoxSong").equals("")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You have already switched it off.");
      return;
    }
    int previousChoice = Preferences.getInteger("choiceAdventure1312");
    Preferences.setInteger("choiceAdventure1312", choice);
    UseItemRequest useBoomBox = UseItemRequest.getInstance(ItemPool.BOOMBOX);
    RequestThread.postRequest(useBoomBox);
    Preferences.setInteger("choiceAdventure1312", previousChoice);
  }
}
