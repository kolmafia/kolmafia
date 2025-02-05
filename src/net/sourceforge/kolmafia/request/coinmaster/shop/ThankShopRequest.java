package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class ThankShopRequest extends CoinMasterRequest {
  public static final String master = "A traveling Thanksgiving salesman";
  public static final String SHOPID = "thankshop";

  private static final Pattern CASHEW_PATTERN = Pattern.compile("([\\d,]+) cashews");
  public static final AdventureResult CASHEW = ItemPool.get(ItemPool.CASHEW, 1);

  public static final CoinmasterData CASHEW_STORE =
      new CoinmasterData(master, "thankshop", ThankShopRequest.class)
          .withToken("cashew")
          .withTokenPattern(CASHEW_PATTERN)
          .withItem(CASHEW)
          .withShopRowFields(master, SHOPID);

  public ThankShopRequest() {
    super(CASHEW_STORE);
  }

  public ThankShopRequest(final boolean buying, final AdventureResult[] attachments) {
    super(CASHEW_STORE, buying, attachments);
  }

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), this.responseText);
  }
}
