package net.sourceforge.kolmafia.swingui.menu;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class LootHermitMenuItem extends ThreadedMenuItem {
  public LootHermitMenuItem() {
    super("Loot the Hermit", new LootHermitListener());
  }

  private static class LootHermitListener extends ThreadedListener {
    @Override
    protected void execute() {
      // See how many clovers are available today. This visits the
      // Hermit, if necessary, and sets the AdventureResult in
      // KoLConstants.hermitItems.
      int cloverCount = HermitRequest.cloverCount();

      AdventureResult selectedValue =
          (AdventureResult)
              InputFieldUtilities.input(
                  "I have worthless items!",
                  (LockableListModel<AdventureResult>) KoLConstants.hermitItems);

      if (selectedValue == null) {
        return;
      }

      int selected = selectedValue.getItemId();
      int maximumValue = HermitRequest.getAvailableWorthlessItemCount();

      String message = "(You have " + maximumValue + " worthless items retrievable)";

      if (selected == ItemPool.TEN_LEAF_CLOVER) {
        if (cloverCount <= maximumValue) {
          message = "(There are " + cloverCount + " clovers still available)";
          maximumValue = cloverCount;
        }
      }

      Integer value =
          InputFieldUtilities.getQuantity(
              "How many " + selectedValue.getName() + " to get?\n" + message, maximumValue, 1);
      int tradeCount = (value == null) ? 0 : value.intValue();

      if (tradeCount == 0) {
        return;
      }

      RequestThread.postRequest(new HermitRequest(selected, tradeCount));
    }
  }
}
