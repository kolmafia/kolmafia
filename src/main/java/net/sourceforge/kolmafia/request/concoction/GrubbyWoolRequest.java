package net.sourceforge.kolmafia.request.concoction;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;

public class GrubbyWoolRequest extends CreateItemRequest {
  public GrubbyWoolRequest(final Concoction conc) {
    super("choice.php", conc);
    this.addFormField("whichchoice", "1490");
    this.addFormField("option", GrubbyWoolRequest.itemIdToOption(conc.getItemId()));
  }

  private static String itemIdToOption(final int itemId) {
    return switch (itemId) {
      case ItemPool.GRUBBY_WOOL_HAT -> "1";
      case ItemPool.GRUBBY_WOOL_SCARF -> "2";
      case ItemPool.GRUBBY_WOOL_TROUSERS -> "3";
      case ItemPool.GRUBBY_WOOL_GLOVES -> "4";
      case ItemPool.GRUBBY_WOOL_BEERWARMER -> "5";
      case ItemPool.GRUBBY_WOOLBALL -> "6";
      default -> "10";
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
      useRequest.addFormField("whichitem", String.valueOf(ItemPool.GRUBBY_WOOL));
      useRequest.run();
      this.setQuantityNeeded(Math.min(count, yield));
      super.run();
      count -= yield;
    }
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("choice.php") || !urlString.contains("whichchoice=1490")) {
      return false;
    }

    return true;
  }
}
