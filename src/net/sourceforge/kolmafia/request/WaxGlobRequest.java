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
    return itemId == ItemPool.MINIATURE_CANDLE
        ? "1"
        : itemId == ItemPool.WAX_HAND
            ? "2"
            : itemId == ItemPool.WAX_FACE
                ? "3"
                : itemId == ItemPool.WAX_PANCAKE ? "4" : itemId == ItemPool.WAX_BOOZE ? "5" : "6";
  }

  private static String optionToName(final int option) {
    return option == 1
        ? "miniature candle"
        : option == 2
            ? "wax hand"
            : option == 3
                ? "wax face"
                : option == 4 ? "wax pancake" : option == 4 ? "wax booze" : "unknown";
  }

  @Override
  protected boolean shouldFollowRedirect() {
    return true;
  }

  @Override
  public void run() {
    // Attempt to retrieve the ingredients
    if (!this.makeIngredients()) {
      return;
    }

    int count = this.getQuantityNeeded();
    String name = this.getName();

    KoLmafia.updateDisplay("Creating " + count + " " + name + "...");

    GenericRequest useRequest = new GenericRequest("inv_use.php");
    useRequest.addFormField("whichitem", String.valueOf(ItemPool.WAX_GLOB));
    useRequest.run();

    for (int i = 0; i < count; ++i) {
      super.run();
    }

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
