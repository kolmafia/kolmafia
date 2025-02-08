package net.sourceforge.kolmafia.swingui.menu;

import java.util.stream.Collectors;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.coinmaster.shop.TrapperRequest;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class LootTrapperMenuItem extends ThreadedMenuItem {
  public LootTrapperMenuItem() {
    super("Visit the Trapper", new LootTrapperListener());
  }

  private static class LootTrapperListener extends ThreadedListener {
    private static final LockableListModel<AdventureResult> skins =
        TrapperRequest.TRAPPER.getBuyItems().stream()
            .map(item -> item.getInstance(1))
            .collect(Collectors.toCollection(LockableListModel::new));

    @Override
    protected void execute() {
      AdventureResult selectedValue = InputFieldUtilities.input("I want skins!", skins);

      if (selectedValue == null) {
        return;
      }

      int selected = selectedValue.getItemId();
      int maximumValue = TrapperRequest.YETI_FUR.getCount(KoLConstants.inventory);
      String message = "(You have " + maximumValue + " furs available)";

      Integer value =
          InputFieldUtilities.getQuantity(
              "How many " + selectedValue.getName() + " to get?\n" + message,
              maximumValue,
              maximumValue);
      int tradeCount = (value == null) ? 0 : value.intValue();

      if (tradeCount == 0) {
        return;
      }

      KoLmafia.updateDisplay("Visiting the trapper...");
      RequestThread.postRequest(TrapperRequest.getRequest(selected, tradeCount));
    }
  }
}
