package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class GuzzlrRequest extends CoinMasterRequest {
  public static final String master = "Guzzlr Company Store Website";

  private static final Pattern GUZZLR_PATTERN = Pattern.compile("([\\d,]+) Guzzlrbuck");
  public static final AdventureResult GUZZLRBUCK = ItemPool.get(ItemPool.GUZZLRBUCK, 1);

  public static final CoinmasterData GUZZLR =
      new CoinmasterData(master, "guzzlr", GuzzlrRequest.class)
          .withToken("Guzzlrbuck")
          .withTokenPattern(GUZZLR_PATTERN)
          .withItem(GUZZLRBUCK)
          .withShopRowFields(master, "guzzlr")
          .withNeedsPasswordHash(true);

  public GuzzlrRequest() {
    super(GUZZLR);
  }

  public GuzzlrRequest(final boolean buying, final AdventureResult[] attachments) {
    super(GUZZLR, buying, attachments);
  }

  public GuzzlrRequest(final boolean buying, final AdventureResult attachment) {
    super(GUZZLR, buying, attachment);
  }

  public GuzzlrRequest(final boolean buying, final int itemId, final int quantity) {
    super(GUZZLR, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=guzzlr")) {
      return;
    }

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(GUZZLR, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(GUZZLR, responseText);
  }

  public static String accessible() {
    return InventoryManager.getAccessibleCount(GUZZLRBUCK) > 0
        ? null
        : "Need access to your Getaway Campsite";
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=guzzlr")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(GUZZLR, urlString, true);
  }
}
