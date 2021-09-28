package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class StarChartRequest extends CreateItemRequest {
  public StarChartRequest(final Concoction conc) {
    // http://www.kingdomofloathing.com/shop.php?whichshop=starchart&action=buyitem&quantity=1&whichrow=139
    // quantity field is not needed and is not used
    super("shop.php", conc);

    this.addFormField("whichshop", "starchart");
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
    // Since we create one at a time, override processResults so
    // superclass method doesn't undo ingredient usage.

    String urlString = this.getURLString();
    String responseText = this.responseText;

    // You place the stars and lines on the chart -- the chart
    // bursts into flames and leaves behind a sweet star item!
    if (urlString.contains("action=buyitem") && !responseText.contains("You place the stars")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Star chart crafting was unsuccessful.");
      return;
    }

    StarChartRequest.parseResponse(urlString, responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=starchart")) {
      return;
    }

    NPCPurchaseRequest.parseShopRowResponse(urlString, responseText);
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=starchart")) {
      return false;
    }

    Matcher rowMatcher = GenericRequest.WHICHROW_PATTERN.matcher(urlString);
    if (!rowMatcher.find()) {
      return true;
    }

    int row = StringUtilities.parseInt(rowMatcher.group(1));
    int itemId = ConcoctionPool.rowToId(row);

    CreateItemRequest item = CreateItemRequest.getInstance(itemId);
    if (item == null) {
      return true; // this is an unknown item
    }

    // The quantity is always 1
    if (item.getQuantityPossible() < 1) {
      return true; // attempt will fail
    }

    StringBuilder buffer = new StringBuilder();
    buffer.append("Trade ");

    AdventureResult[] ingredients = ConcoctionDatabase.getIngredients(itemId);
    for (int i = 0; i < ingredients.length; ++i) {
      if (i > 0) {
        buffer.append(", ");
      }

      buffer.append(ingredients[i].getCount());
      buffer.append(" ");
      buffer.append(ingredients[i].getName());
    }

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(buffer.toString());

    return true;
  }
}
