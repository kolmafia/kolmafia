package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;

public class FiveDPrinterRequest extends CreateItemRequest {
  public FiveDPrinterRequest(final Concoction conc) {
    // shop.php?whichshop=5dprinter&action=buyitem&quantity=1&whichrow=340&pwd=15a3ed7ce8a5e0c8a6c7e08a03fca040
    // quantity field is not needed and is not used
    super("shop.php", conc);

    this.addFormField("whichshop", "5dprinter");
    this.addFormField("action", "buyitem");
    int row = ConcoctionPool.idToRow(this.getItemId());
    this.addFormField("whichrow", String.valueOf(row));
  }

  @Override
  public void run() {
    // Attempting to make the ingredients will pull the
    // needed items from the closet if they are missing.

    if (!this.makeIngredients()) {
      return;
    }

    super.run();
  }

  @Override
  public void processResults() {
    String urlString = this.getURLString();
    String responseText = this.responseText;

    if (urlString.contains("action=buyitem") && !responseText.contains("You acquire")) {
      KoLmafia.updateDisplay(KoLConstants.MafiaState.ERROR, "5d printing was unsuccessful.");
      return;
    }

    FiveDPrinterRequest.parseResponse(urlString, responseText);
  }

  private static final Pattern DISCOVERY_PATTERN = Pattern.compile("descitem\\((\\d+)\\)");

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=5dprinter")) {
      return;
    }

    Matcher matcher = FiveDPrinterRequest.DISCOVERY_PATTERN.matcher(responseText);
    while (matcher.find()) {
      int id = ItemDatabase.getItemIdFromDescription(matcher.group(1));
      String pref = "unknownRecipe" + id;
      if (id > 0 && Preferences.getBoolean(pref)) {
        KoLmafia.updateDisplay("You know the recipe for " + ItemDatabase.getItemName(id));
        Preferences.setBoolean(pref, false);
        ConcoctionDatabase.setRefreshNeeded(true);
      }
    }

    NPCPurchaseRequest.parseShopRowResponse(urlString, responseText);
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=5dprinter")) {
      return false;
    }

    return NPCPurchaseRequest.registerShopRowRequest(urlString);
  }
}
