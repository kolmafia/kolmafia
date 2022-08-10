package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public class MeteoroidRequest extends CreateItemRequest {
  public MeteoroidRequest(final Concoction conc) {
    super("choice.php", conc);
    this.addFormField("whichchoice", "1264");
    this.addFormField("option", MeteoroidRequest.itemIdToOption(conc.getItemId()));
  }

  private static String itemIdToOption(final int itemId) {
    return itemId == ItemPool.METEORTARBOARD
        ? "1"
        : itemId == ItemPool.METEORITE_GUARD
            ? "2"
            : itemId == ItemPool.METEORB
                ? "3"
                : itemId == ItemPool.ASTEROID_BELT
                    ? "4"
                    : itemId == ItemPool.METEORTHOPEDIC_SHOES
                        ? "5"
                        : itemId == ItemPool.SHOOTING_MORNING_STAR ? "6" : "7";
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
    useRequest.addFormField("whichitem", String.valueOf(ItemPool.METAL_METEOROID));

    for (int i = 0; i < count; ++i) {
      useRequest.run();
      super.run();
    }
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("choice.php") || !urlString.contains("whichchoice=1264")) {
      return false;
    }

    return true;
  }
}
