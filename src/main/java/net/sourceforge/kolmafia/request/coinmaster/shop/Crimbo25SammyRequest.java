package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.List;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.shop.ShopRow;

public abstract class Crimbo25SammyRequest extends CoinMasterShopRequest {
  public static final String master = "The HMS Bounty Hunter";
  public static final String SHOPID = "crimbo25_sammy";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, Crimbo25SammyRequest.class)
          .inZone("Crimbo25")
          .withNewShopRowFields(master, SHOPID)
          .withVisitShopRows(Crimbo25SammyRequest::visitShopRows)
          .withAjax(false)
          .withUseCountField(Crimbo25SammyRequest::useCountField);

  // as you buy more currency with wads, the price increases
  public static void visitShopRows(final List<ShopRow> shopRows, Boolean force) {
    for (ShopRow shopRow : shopRows) {
      for (ShopRow existing : DATA.getShopRows()) {
        if (shopRow.getRow() == existing.getRow()) {
          existing.setCosts(shopRow.getCosts());
        }
      }
    }
  }

  private static boolean useCountField(ShopRow row) {
    var num = row.getRow();
    // we need to refresh costs when wads are traded
    if (num == 1649 || num == 1650) {
      return false;
    }
    return true;
  }
}
