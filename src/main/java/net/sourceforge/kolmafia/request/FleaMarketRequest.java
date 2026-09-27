package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

public class FleaMarketRequest extends GenericRequest {
  public FleaMarketRequest() {
    super("town_fleamarket.php");
  }

  @Override
  public void processResults() {
    FleaMarketRequest.parseResponse(this.getURLString(), this.responseText);
  }

  private static final Pattern PURCHASED_ITEM_PATTERN =
      Pattern.compile("<center>You purchase the item from (.*? \\( #\\d+ \\))<center>");

  public static final void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("town_fleamarket.php")) {
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

    // >You purchase the item from XYZ ( #12345 )<
    int price = GenericRequest.getHowMuch(urlString);
    Matcher m = PURCHASED_ITEM_PATTERN.matcher(responseText);

    if (!m.find()) {
      return;
    }

    RequestLogger.updateSessionLog(
        "Purchased "
            + itemName
            + " from "
            + m.group(1)
            + " at the Flea Market for "
            + price
            + " meat.");
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("town_fleamarket.php")) {
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

    int price = GenericRequest.getHowMuch(urlString);

    String message = "Purchasing " + itemName + " from the Flea Market for " + price + " meat.";
    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);

    return true;
  }
}
