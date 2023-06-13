package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public class BurningNewspaperRequest extends CreateItemRequest {
  public BurningNewspaperRequest(final Concoction conc) {
    super("choice.php", conc);
    this.addFormField("whichchoice", "1277");
    this.addFormField("option", BurningNewspaperRequest.itemIdToOption(conc.getItemId()));
  }

  private static String itemIdToOption(final int itemId) {
    return switch (itemId) {
      case ItemPool.BURNING_HAT -> "1";
      case ItemPool.BURNING_CAPE -> "2";
      case ItemPool.BURNING_SLIPPERS -> "3";
      case ItemPool.BURNING_JORTS -> "4";
      case ItemPool.BURNING_CRANE -> "5";
      default -> "6";
    };
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

    KoLmafia.updateDisplay("Creating " + count + " " + this.getName() + "...");

    int yield = this.getYield();

    while (count > 0 && KoLmafia.permitsContinue()) {
      GenericRequest useRequest = new GenericRequest("inv_use.php");
      useRequest.addFormField("whichitem", String.valueOf(ItemPool.BURNING_NEWSPAPER));
      useRequest.run();
      this.setQuantityNeeded(Math.min(count, yield));
      super.run();
      count -= yield;
    }
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("choice.php") || !urlString.contains("whichchoice=1277")) {
      return false;
    }

    return true;
  }
}
