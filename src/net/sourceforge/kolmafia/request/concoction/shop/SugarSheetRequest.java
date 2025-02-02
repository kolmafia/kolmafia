package net.sourceforge.kolmafia.request.concoction.shop;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.concoction.CreateItemRequest;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class SugarSheetRequest extends CreateItemRequest {
  public static final String SHOPID = "sugarsheets";

  public SugarSheetRequest(final Concoction conc) {
    // http://www.kingdomofloathing.com/shop.php?whichshop=sugarsheets&action=buyitem&quantity=1&whichrow=329
    // quantity field is not needed and is not used
    super("shop.php", conc);

    this.addFormField("whichshop", SHOPID);
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

    // You moisten your sugar sheet, and quickly fold it into a new
    // shape before it dries.

    if (urlString.contains("action=buyitem")
        && !responseText.contains("quickly fold it into a new shape")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You can't fold that.");
      return;
    }

    ShopRequest.parseResponse(urlString, responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!responseText.contains("quickly fold it into a new shape")) {
      return;
    }

    // Folding always uses exactly one sugar sheet
    ResultProcessor.processItem(ItemPool.SUGAR_SHEET, -1);
  }
}
