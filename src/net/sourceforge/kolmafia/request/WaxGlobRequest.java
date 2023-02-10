package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public class WaxGlobRequest extends CreateItemRequest {
  public WaxGlobRequest(final Concoction conc) {
    super("choice.php", conc);
    this.addFormField("whichchoice", "1218");
    this.addFormField("option", WaxGlobRequest.itemIdToOption(conc.getItemId()));
  }

  private static String itemIdToOption(final int itemId) {
    return switch (itemId) {
      case ItemPool.MINIATURE_CANDLE -> "1";
      case ItemPool.WAX_HAND -> "2";
      case ItemPool.WAX_FACE -> "3";
      case ItemPool.WAX_PANCAKE -> "4";
      case ItemPool.WAX_BOOZE -> "5";
      default -> "6";
    };
  }

  private static String optionToName(final int option) {
    return switch (option) {
      case 1 -> "miniature candle";
      case 2 -> "wax hand";
      case 3 -> "wax face";
      case 4 -> "wax pancake";
      case 5 -> "wax booze";
      default -> "unknown";
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

    // "using" the item redirects to a choice
    GenericRequest useRequest = new GenericRequest("inv_use.php");
    useRequest.addFormField("whichitem", String.valueOf(ItemPool.WAX_GLOB));
    useRequest.run();

    // You stay in the choice until you exit; you cannot walk away from it.
    // Let CreateItemRequest handle the looping.
    this.runCreateItemLoop();

    // Finish by exiting the choice
    GenericRequest closeRequest = new GenericRequest("choice.php");
    closeRequest.addFormField("whichchoice", "1218");
    closeRequest.addFormField("option", "6");
    closeRequest.run();
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("choice.php") || !urlString.contains("whichchoice=1218")) {
      return false;
    }

    return true;
  }
}
