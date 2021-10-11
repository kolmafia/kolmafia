package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.session.StoreManager;

public class MallRepriceCommand extends AbstractCommand {
  public MallRepriceCommand() {
    this.usage =
        " [min] - price all max-priced items at or below current Mall minimum price. [List items even if the current lowest price is mall minimum.]";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (parameters.startsWith("min")) {
      StoreManager.priceItemsAtLowestPrice(false);
    } else {
      StoreManager.priceItemsAtLowestPrice(true);
    }
  }
}
