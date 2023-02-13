package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;

public class StickersCommand extends AbstractCommand {
  public StickersCommand() {
    this.usage = " <sticker1> [, <sticker2> [, <sticker3>]] - replace worn stickers.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    String[] stickers = parameters.split("\\s*,\\s*");
    int i = 0;
    for (var slot : SlotSet.STICKER_SLOTS) {
      if (EquipmentManager.getEquipment(slot) == EquipmentRequest.UNEQUIP) {
        if (i >= stickers.length) break;
        String item = stickers[i++].toLowerCase();
        if (!item.contains("stick")) {
          item = item + " sticker";
        }
        EquipCommand.equip(slot.phpName + " " + item);
      }
    }
  }
}
