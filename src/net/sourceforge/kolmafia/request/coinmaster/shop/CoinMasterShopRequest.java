package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;
import net.sourceforge.kolmafia.shop.ShopRow;

public class CoinMasterShopRequest extends CoinMasterRequest {
  protected ShopRow row = null;

  public CoinMasterShopRequest() {}

  public CoinMasterShopRequest(final CoinmasterData data) {
    super(data);
  }

  public CoinMasterShopRequest(final CoinmasterData data, final String action) {
    super(data, action);
  }

  public CoinMasterShopRequest(final CoinmasterData data, final ShopRow row, final int quantity) {
    super(data);
    this.row = row;
    this.quantity = quantity;
    this.addFormField("whichrow", String.valueOf(row.getRow()));
  }

  public CoinMasterShopRequest(
      final CoinmasterData data, final boolean buying, final AdventureResult[] attachments) {
    super(data, buying, attachments);
  }

  // Convenience constructors, overridden by the handful of subclasses that need them.

  public static CoinMasterShopRequest getRequest(CoinmasterData data) {
    return new CoinMasterShopRequest(data);
  }

  public static CoinMasterShopRequest getRequest(
      final CoinmasterData data, final boolean buying, final AdventureResult attachment) {
    return new CoinMasterShopRequest(data, buying, new AdventureResult[] {attachment});
  }

  public static CoinMasterShopRequest getRequest(
      final CoinmasterData data, final boolean buying, final int itemId, final int quantity) {
    return CoinMasterShopRequest.getRequest(data, buying, ItemPool.get(itemId, quantity));
  }

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), this.responseText);
  }
}
