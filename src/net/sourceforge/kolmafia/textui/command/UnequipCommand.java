package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;

public class UnequipCommand extends AbstractCommand {
  public UnequipCommand() {
    this.usage = " <slot> | <name> - remove equipment in slot, or that matches name";
  }

  @Override
  public void run(final String cmd, String parameters) {
    // Look for name of slot
    String command = parameters.split(" ")[0];
    Slot slot = EquipmentRequest.slotNumber(command);

    if (slot != Slot.NONE) {
      RequestThread.postRequest(new EquipmentRequest(EquipmentRequest.UNEQUIP, slot));
      return;
    }

    parameters = parameters.toLowerCase();

    // Allow player to remove all of his fake hands
    if (parameters.equals("fake hand")) {
      if (EquipmentManager.getFakeHands() == 0) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You're not wearing any fake hands");
      } else {
        RequestThread.postRequest(new EquipmentRequest(EquipmentRequest.UNEQUIP, Slot.FAKEHAND));
      }

      return;
    }

    // The following loop removes all items with the
    // specified name.

    for (var s : SlotSet.SLOTS) {
      unequip(s, parameters);
    }
    for (var s : SlotSet.STICKER_SLOTS) {
      unequip(s, parameters);
    }
  }

  private void unequip(Slot slot, String name) {
    AdventureResult item = EquipmentManager.getEquipment(slot);
    if (item != null && item.getName().toLowerCase().contains(name)) {
      RequestThread.postRequest(new EquipmentRequest(EquipmentRequest.UNEQUIP, slot));
    }
  }
}
