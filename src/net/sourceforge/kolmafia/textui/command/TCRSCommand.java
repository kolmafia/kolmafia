package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.TCRSDatabase;
import net.sourceforge.kolmafia.persistence.TCRSDatabase.TCRS;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TCRSCommand extends AbstractCommand {
  public TCRSCommand() {
    this.usage =
        " fetch CLASS, SIGN | load | save | derive [#] | check # | apply | help - handle item modifiers for Two Crazy Random Summer.";
  }

  @Override
  public void run(final String cmd, String parameters) {
    String command;

    int index = parameters.indexOf(" ");
    if (index == -1) {
      command = parameters;
      parameters = "";
    } else {
      command = parameters.substring(0, index);
      parameters = parameters.substring(index + 1);
    }

    if (command.equals("help")) {
      RequestLogger.printLine(" ");
      RequestLogger.printLine("Some commands require being in a TCRS run and data will");
      RequestLogger.printLine("be for current CLASS and SIGN.");
      RequestLogger.printLine("fetch CLASS SIGN - fetch remote data for class and sign.");
      RequestLogger.printLine(
          "test CLASS SIGN - load and apply data for class and sign, regardless of current path, class, and sign.");
      RequestLogger.printLine("ring - display modifiers for ring.");
      RequestLogger.printLine("spoon - display modifiers for spoon.");
      RequestLogger.printLine("load - load current data.");
      RequestLogger.printLine("save = data to local disk.");
      RequestLogger.printLine(
          "derive [#] - derive data for specified item or all items for current CLASS and SIGN");
      RequestLogger.printLine("check # - display data for item.");
      RequestLogger.printLine("apply - apply current data.");
      RequestLogger.printLine("help - display this text.");
      return;
    }

    if (command.equals("fetch")) {
      String[] split = parameters.split(" *, *");
      if (split.length != 2) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "fetch CLASS SIGN");
        return;
      }
      String cclass = split[0];
      String sign = split[1];
      if (!TCRSDatabase.validate(cclass, sign)) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR,
            cclass + " is not a valid class or " + sign + " is not a valid sign.");
        return;
      }
      if (TCRSDatabase.anyLocalFileExists(cclass, sign, true)) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Will not overwrite. Aborting.");
        return;
      }
      TCRSDatabase.fetch(cclass, sign, true);
      TCRSDatabase.fetchCafe(cclass, sign, true);
      return;
    }

    if (command.equals("ring")) {
      TCRS tcrs = TCRSDatabase.deriveRing();
      if (tcrs == null) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "No item description for the 'ring'!");
        return;
      }

      RequestLogger.printLine("name = " + tcrs.name);
      RequestLogger.printLine("modifiers = '" + tcrs.modifiers + "'");

      return;
    }

    if (command.equals("spoon")) {
      TCRS tcrs = TCRSDatabase.deriveSpoon();
      if (tcrs == null) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "No item description for the 'spoon'!");
        return;
      }

      String line1 = "name = " + tcrs.name;
      RequestLogger.printLine(line1);
      RequestLogger.updateSessionLog(line1);
      String line2 = "modifiers = '" + tcrs.modifiers + "'";
      RequestLogger.printLine(line2);
      RequestLogger.updateSessionLog(line2);
      return;
    }

    if (command.equals("test")) {
      String[] split = parameters.split(" *, *");

      if (split.length == 1 && split[0].equals("reset")) {
        TCRSDatabase.resetModifiers();
        return;
      }

      if (split.length != 2) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "test CLASS, SIGN or test reset");
        return;
      }

      String cclass = split[0];
      String sign = split[1];
      TCRSDatabase.load(cclass, sign, true);
      TCRSDatabase.loadCafe(cclass, sign, true);
      TCRSDatabase.applyModifiers();
      return;
    }

    if (!KoLCharacter.isCrazyRandomTwo()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You are not in a Two Crazy Random Summer run");
      return;
    }

    String file = TCRSDatabase.filename();

    if (command.equals("load")) {
      TCRSDatabase.load(true);
      return;
    }

    if (command.equals("save")) {
      TCRSDatabase.save(true);
    }

    if (command.equals("derive")) {
      if (parameters.equals("")) {
        TCRSDatabase.derive(true);
      } else {
        TCRSDatabase.deriveAndSaveItem(StringUtilities.parseInt(parameters));
      }
      return;
    }

    if (command.equals("update")) {
      TCRSDatabase.update(true);
      TCRSDatabase.updateCafeBooze(true);
      TCRSDatabase.updateCafeFood(true);
      return;
    }

    if (command.equals("check")) {
      int itemId = StringUtilities.parseInt(parameters);
      TCRS tcrs = TCRSDatabase.deriveItem(itemId);
      if (tcrs == null) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "Item #" + itemId + " does not have a description");
        return;
      }

      RequestLogger.printLine("name = " + tcrs.name);
      RequestLogger.printLine("size = " + tcrs.size);
      RequestLogger.printLine("quality = " + tcrs.quality);
      RequestLogger.printLine("modifiers = '" + tcrs.modifiers + "'");

      return;
    }

    if (command.equals("apply")) {
      TCRSDatabase.applyModifiers();
      return;
    }

    if (command.equals("reset")) {
      TCRSDatabase.resetModifiers();
      return;
    }
  }
}
