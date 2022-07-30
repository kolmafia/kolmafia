package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.InventoryManager;

public class BurningNewspaperRequest extends CreateItemRequest {
  public BurningNewspaperRequest(final Concoction conc) {
    super("choice.php", conc);
    this.addFormField("whichchoice", "1277");
    this.addFormField("option", BurningNewspaperRequest.itemIdToOption(conc.getItemId()));
  }

  private static String itemIdToOption(final int itemId) {
    return itemId == ItemPool.BURNING_HAT
        ? "1"
        : itemId == ItemPool.BURNING_CAPE
            ? "2"
            : itemId == ItemPool.BURNING_SLIPPERS
                ? "3"
                : itemId == ItemPool.BURNING_JORTS
                    ? "4"
                    : itemId == ItemPool.BURNING_CRANE ? "5" : "6";
  }

  @Override
  protected boolean shouldFollowRedirect() {
    return true;
  }

  @Override
  public void run() {
    int count = this.getQuantityNeeded();
    if (count == 0) {
      return;
    }

    // Attempt to retrieve the ingredients
    if (!this.makeIngredients()) {
      return;
    }

    String name = this.getName();

    KoLmafia.updateDisplay("Creating " + count + " " + name + "...");

    GenericRequest useRequest = new GenericRequest("inv_use.php");
    useRequest.addFormField("whichitem", String.valueOf(ItemPool.BURNING_NEWSPAPER));
    useRequest.run();

    for (int i = 0; i < count; ++i) {
      super.run();
    }

    // If you still have unused burning newspapers, you are still in the choice.
    // If you folded your last one, you are no longer in the choice.
    // You can walk away from the choice, so we don't really need to exit it.

    if (InventoryManager.getCount(ItemPool.BURNING_NEWSPAPER) > 0) {
      GenericRequest closeRequest = new GenericRequest("choice.php");
      closeRequest.addFormField("whichchoice", "1277");
      closeRequest.addFormField("option", "6");
      closeRequest.run();
    }
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("choice.php") || !urlString.contains("whichchoice=1277")) {
      return false;
    }

    return true;
  }
}
