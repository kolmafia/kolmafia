package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public class Crimbo14Request extends CoinMasterRequest {
  public static final String master = "Crimbo 2014";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("<td>(no|[\\d,]) Crimbo Credit", Pattern.DOTALL);
  public static final AdventureResult CRIMBO_CREDIT = ItemPool.get(ItemPool.CRIMBO_CREDIT, 1);

  public static final CoinmasterData CRIMBO14 =
      new CoinmasterData(master, "crimbo14", Crimbo14Request.class)
          .withToken("Crimbo Credit")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(CRIMBO_CREDIT)
          .withShopRowFields(master, "crimbo14")
          .withSellURL("shop.php?whichshop=crimbo14turnin")
          .withSellAction("buyitem")
          .withSellItems(master)
          .withSellPrices(master);

  public Crimbo14Request() {
    super(CRIMBO14);
  }

  public Crimbo14Request(final boolean buying, final AdventureResult[] attachments) {
    super(CRIMBO14, buying, attachments);
  }

  public Crimbo14Request(final boolean buying, final AdventureResult attachment) {
    super(CRIMBO14, buying, attachment);
  }

  public Crimbo14Request(final boolean buying, final int itemId, final int quantity) {
    super(CRIMBO14, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=crimbo14")) {
      return;
    }

    CoinmasterData data = CRIMBO14;

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, urlString, responseText);
    }
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=crimbo14")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(CRIMBO14, urlString);
  }

  public static String accessible() {
    return "Crimbo Credits are no longer exchangeable";
  }
}
