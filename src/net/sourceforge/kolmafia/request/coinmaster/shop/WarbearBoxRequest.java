package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;

public class WarbearBoxRequest extends CoinMasterRequest {
  public static final String master = "Warbear Black Box";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) warbear whosit");
  public static final AdventureResult WHOSIT = ItemPool.get(ItemPool.WARBEAR_WHOSIT, 1);
  public static final AdventureResult BLACKBOX = ItemPool.get(ItemPool.WARBEAR_BLACK_BOX, 1);

  public static final CoinmasterData WARBEARBOX =
      new CoinmasterData(master, "warbear", WarbearBoxRequest.class)
          .withToken("warbear whosit")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(WHOSIT)
          .withShopRowFields(master, "warbear");

  public WarbearBoxRequest() {
    super(WARBEARBOX);
  }

  public WarbearBoxRequest(final boolean buying, final AdventureResult[] attachments) {
    super(WARBEARBOX, buying, attachments);
  }

  public WarbearBoxRequest(final boolean buying, final AdventureResult attachment) {
    super(WARBEARBOX, buying, attachment);
  }

  public WarbearBoxRequest(final boolean buying, final int itemId, final int quantity) {
    super(WARBEARBOX, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=warbear")) {
      return;
    }

    CoinmasterData data = WARBEARBOX;

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=warbear")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(WARBEARBOX, urlString, true);
  }

  public static String accessible() {
    int wand = BLACKBOX.getCount(KoLConstants.inventory);
    if (wand == 0) {
      return "You don't have a warbear black box";
    }
    return null;
  }
}
