package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public class ShadowForgeRequest extends CreateItemRequest {
  public ShadowForgeRequest(final Concoction conc) {
    super("shop.php", conc);

    this.addFormField("whichshop", "shadowforge");
    this.addFormField("action", "buyitem");
    int row = ConcoctionPool.idToRow(this.getItemId());
    this.addFormField("whichrow", String.valueOf(row));
  }

  @Override
  public void run() {
    if (Preferences.getInteger("lastShadowForgeUnlockAdventure") != KoLCharacter.getCurrentRun()) {
      KoLmafia.updateDisplay(
          KoLConstants.MafiaState.ERROR, "You have adventured since visiting The Shadow Forge.");
      return;
    }

    // As long as we don't adventure, we can fetch ingredients.
    if (!this.makeIngredients()) {
      return;
    }

    KoLmafia.updateDisplay("Creating " + this.getQuantityNeeded() + " " + this.getName() + "...");
    this.addFormField("quantity", String.valueOf(this.getQuantityNeeded()));
    super.run();
  }

  @Override
  public void processResults() {
    String urlString = this.getURLString();
    String responseText = this.responseText;

    if (urlString.contains("action=buyitem") && !responseText.contains("You acquire")) {
      KoLmafia.updateDisplay(
          KoLConstants.MafiaState.ERROR, "Buying from The Shadow Forge was unsuccessful.");
      return;
    }

    ShadowForgeRequest.parseResponse(urlString, responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=shadowforge")) {
      return;
    }

    NPCPurchaseRequest.parseShopRowResponse(urlString, responseText);
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=shadowforge")) {
      return false;
    }

    return NPCPurchaseRequest.registerShopRowRequest(urlString);
  }
}
