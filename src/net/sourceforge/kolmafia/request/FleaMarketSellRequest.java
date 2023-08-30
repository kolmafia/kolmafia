package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.session.ResultProcessor;

public class FleaMarketSellRequest extends GenericRequest {
  public FleaMarketSellRequest() {
    super("town_sellflea.php");
  }

  @Override
  public void processResults() {
    FleaMarketSellRequest.parseResponse(this.getURLString(), this.responseText);
  }

  private static final Pattern SELLPRICE_PATTERN = Pattern.compile("sellprice=(\\d+)");

  public static final void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("town_sellflea.php")) {
      return;
    }

    int itemId = GenericRequest.getWhichItem(urlString);

    if (itemId < 0) {
      return;
    }

    String itemName = ItemDatabase.getItemName(itemId);

    if (itemName == null) {
      return;
    }

    // You place your item for sale in the Flea Market.  It will be returned to you if it does not
    // sell within 48 hours.
    if (responseText.indexOf("You place your item for sale in the Flea Market.") == -1) {
      return;
    }

    ResultProcessor.removeItem(itemId);
    int sellPrice = GenericRequest.getNumericField(urlString, SELLPRICE_PATTERN);

    RequestLogger.updateSessionLog(
        "Placed " + itemName + " up for sale at the Flea Market for " + sellPrice + " meat.");
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("town_sellflea.php")) {
      return false;
    }

    int itemId = GenericRequest.getWhichItem(urlString);

    if (itemId < 0) {
      return false;
    }

    String itemName = ItemDatabase.getItemName(itemId);

    if (itemName == null) {
      return false;
    }

    int sellPrice = GenericRequest.getNumericField(urlString, SELLPRICE_PATTERN);

    String message =
        "Placing " + itemName + " up for sale at the Flea Market for " + sellPrice + " meat.";

    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);

    return true;
  }
}
