package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.session.InventoryManager;

public class AcquireCommand extends AbstractCommand {
  public AcquireCommand() {
    this.usage =
        "[?] <item>[, <item>]... - ensure that you have item, creating or buying it if needed.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    boolean checking = KoLmafiaCLI.isExecutingCheckOnlyCommand;
    KoLmafiaCLI.isExecutingCheckOnlyCommand = false;

    AdventureResult[] items = ItemFinder.getMatchingItemList(parameters);

    try (Checkpoint checkpoint = new Checkpoint(checking)) {
      for (int i = 0; i < items.length; ++i) {
        AdventureResult item = items[i];

        if (checking) {
          RequestLogger.printLine(item + ": " + InventoryManager.simRetrieveItem(item, true));
        } else {
          InventoryManager.retrieveItem(item, true);
        }
      }
    }
  }
}
