package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class GMartRequest extends CoinMasterRequest {
  public static final String master = "G-Mart";
  public static final String SHOPID = "glover";

  private static final Pattern G_PATTERN = Pattern.compile("([\\d,]+) G");
  public static final AdventureResult G = ItemPool.get(ItemPool.G, 1);

  public static final CoinmasterData GMART =
      new CoinmasterData(master, "glover", GMartRequest.class)
          .withToken("G")
          .withTokenTest("no Gs")
          .withTokenPattern(G_PATTERN)
          .withItem(G)
          .withShopRowFields(master, SHOPID)
          .withAccessible(GMartRequest::accessible);

  public GMartRequest() {
    super(GMART);
  }

  public GMartRequest(final boolean buying, final AdventureResult[] attachments) {
    super(GMART, buying, attachments);
  }

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static String accessible() {
    // *** Finish this.
    return null;
  }
}
