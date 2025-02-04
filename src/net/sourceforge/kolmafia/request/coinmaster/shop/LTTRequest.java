package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class LTTRequest extends CoinMasterRequest {
  public static final String master = "LT&T Gift Shop";
  public static final String SHOPID = "ltt";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) buffalo dime");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.BUFFALO_DIME, 1);

  public static final CoinmasterData LTT =
      new CoinmasterData(master, "LT&T Gift Shop", LTTRequest.class)
          .withToken("buffalo dime")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, SHOPID)
          .withAccessible(LTTRequest::accessible);

  public LTTRequest() {
    super(LTT);
  }

  public LTTRequest(final boolean buying, final AdventureResult[] attachments) {
    super(LTT, buying, attachments);
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
