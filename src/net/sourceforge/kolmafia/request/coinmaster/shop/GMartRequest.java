package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;

public class GMartRequest extends CoinMasterRequest {
  public static final String master = "G-Mart";

  private static final Pattern G_PATTERN = Pattern.compile("([\\d,]+) G");
  public static final AdventureResult G = ItemPool.get(ItemPool.G, 1);

  public static final CoinmasterData GMART =
      new CoinmasterData(master, "glover", GMartRequest.class)
          .withToken("G")
          .withTokenTest("no Gs")
          .withTokenPattern(G_PATTERN)
          .withItem(G)
          .withShopRowFields(master, "glover")
          .withNeedsPasswordHash(true);

  public GMartRequest() {
    super(GMART);
  }

  public GMartRequest(final boolean buying, final AdventureResult[] attachments) {
    super(GMART, buying, attachments);
  }

  public GMartRequest(final boolean buying, final AdventureResult attachment) {
    super(GMART, buying, attachment);
  }

  public GMartRequest(final boolean buying, final int itemId, final int quantity) {
    super(GMART, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=glover")) {
      return;
    }

    CoinmasterData data = GMART;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=glover")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(GMART, urlString, true);
  }
}
