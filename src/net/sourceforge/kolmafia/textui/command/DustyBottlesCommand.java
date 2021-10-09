package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

public class DustyBottlesCommand extends AbstractCommand {
  public DustyBottlesCommand() {
    this.usage = " - list the dusty bottles of wine.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    for (int i = 2271; i <= 2276; ++i) {
      String bottle = ItemDatabase.getItemName(i);
      String type = ConsumablesDatabase.dustyBottleType(i);
      RequestLogger.printLine(bottle + ": " + type);
    }
  }
}
