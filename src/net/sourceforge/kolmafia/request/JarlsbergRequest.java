package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class JarlsbergRequest extends CreateItemRequest {
  public JarlsbergRequest(final Concoction conc) {
    // shop.php?pwd&whichshop=jarl&action=buyitem&whichrow=60&quantity=1
    super("shop.php", conc);

    this.addFormField("whichshop", "jarl");
    this.addFormField("action", "buyitem");
    int row = ConcoctionPool.idToRow(this.getItemId());
    this.addFormField("whichrow", String.valueOf(row));
  }

  @Override
  public void run() {
    if (!this.makeIngredients()) {
      return;
    }

    KoLmafia.updateDisplay("Creating " + this.getQuantityNeeded() + " " + this.getName() + "...");
    this.addFormField("quantity", String.valueOf(this.getQuantityNeeded()));
    super.run();
  }

  @Override
  public void processResults() {
    JarlsbergRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static final void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=jarl")) {
      return;
    }

    if (urlString.indexOf("action=buyitem") == -1) {
      return;
    }

    if (responseText.indexOf("You acquire") == -1) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Cosmic shopping was unsuccessful.");
      return;
    }

    Matcher rowMatcher = GenericRequest.WHICHROW_PATTERN.matcher(urlString);
    if (!rowMatcher.find()) {
      return;
    }

    int row = StringUtilities.parseInt(rowMatcher.group(1));
    int itemId = ConcoctionPool.rowToId(row);

    CreateItemRequest jarlsItem = CreateItemRequest.getInstance(itemId);
    if (jarlsItem == null) {
      return; // this is an unknown item
    }

    int quantity = 1;
    if (urlString.contains("buymax=")) {
      quantity = jarlsItem.getQuantityPossible();
    } else {
      Matcher quantityMatcher = GenericRequest.QUANTITY_PATTERN.matcher(urlString);
      if (quantityMatcher.find()) {
        String quantityString = quantityMatcher.group(1).trim();
        quantity = quantityString.length() == 0 ? 1 : StringUtilities.parseInt(quantityString);
      }
    }

    AdventureResult[] ingredients = ConcoctionDatabase.getIngredients(itemId);

    for (int i = 0; i < ingredients.length; ++i) {
      ResultProcessor.processResult(
          ingredients[i].getInstance(-1 * ingredients[i].getCount() * quantity));
    }
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=jarl")) {
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

    int quantity = 1;
    if (urlString.contains("buymax=")) {
      quantity = item.getQuantityPossible();
    } else {
      Matcher quantityMatcher = GenericRequest.QUANTITY_PATTERN.matcher(urlString);
      if (quantityMatcher.find()) {
        String quantityString = quantityMatcher.group(1).trim();
        quantity = quantityString.length() == 0 ? 1 : StringUtilities.parseInt(quantityString);
      }
    }

    AdventureResult[] ingredients = ConcoctionDatabase.getIngredients(itemId);

    StringBuilder buffer = new StringBuilder();
    buffer.append("Using ");

    for (int i = 0; i < ingredients.length; ++i) {
      if (i > 0) {
        buffer.append(" + ");
      }

      buffer.append(ingredients[i].getCount() * quantity);
      buffer.append(" ");
      buffer.append(ingredients[i].getName());
    }

    buffer.append(" to make ");
    buffer.append(quantity);
    buffer.append(" ");
    buffer.append(item.getName());

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(buffer.toString());

    return true;
  }
}
