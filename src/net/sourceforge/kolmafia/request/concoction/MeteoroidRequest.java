package net.sourceforge.kolmafia.request.concoction;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;

public class MeteoroidRequest extends CreateItemRequest {
  public MeteoroidRequest(final Concoction conc) {
    super("choice.php", conc);
    this.addFormField("whichchoice", "1264");
    this.addFormField("option", MeteoroidRequest.itemIdToOption(conc.getItemId()));
  }

  private static String itemIdToOption(final int itemId) {
    return switch (itemId) {
      case ItemPool.METEORTARBOARD -> "1";
      case ItemPool.METEORITE_GUARD -> "2";
      case ItemPool.METEORB -> "3";
      case ItemPool.ASTEROID_BELT -> "4";
      case ItemPool.METEORTHOPEDIC_SHOES -> "5";
      case ItemPool.SHOOTING_MORNING_STAR -> "6";
      default -> "7";
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

    String name = this.getName();

    KoLmafia.updateDisplay("Creating " + count + " " + name + "...");

    int yield = this.getYield();

    while (count > 0 && KoLmafia.permitsContinue()) {
      GenericRequest useRequest = new GenericRequest("inv_use.php");
      useRequest.addFormField("whichitem", String.valueOf(ItemPool.METAL_METEOROID));
      useRequest.run();
      this.setQuantityNeeded(Math.min(count, yield));
      super.run();
      count -= yield;
    }
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("choice.php") || !urlString.contains("whichchoice=1264")) {
      return false;
    }

    return true;
  }
}
