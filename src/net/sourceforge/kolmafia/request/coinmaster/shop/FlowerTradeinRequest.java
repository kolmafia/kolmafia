package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.shop.ShopRow;

public abstract class FlowerTradeinRequest extends CoinMasterShopRequest {
  public static final String master = "The Central Loathing Floral Mercantile Exchange";
  public static final String SHOPID = "flowertradein";

  public static final AdventureResult TOKEN = ItemPool.get(ItemPool.CHRONER, 1);

  private static AdventureResult ROSE = ItemPool.get(ItemPool.ROSE, 1);
  private static AdventureResult WHITE_TULIP = ItemPool.get(ItemPool.WHITE_TULIP, 1);
  private static AdventureResult RED_TULIP = ItemPool.get(ItemPool.RED_TULIP, 1);
  private static AdventureResult BLUE_TULIP = ItemPool.get(ItemPool.BLUE_TULIP, 1);

  // This is conceptually a coinmaster which "sells" flowers for Chroners

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "flowertradein", FlowerTradeinRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withVisitShopRows(FlowerTradeinRequest::visitShopRows)
          .withAccessible(FlowerTradeinRequest::accessible);

  public static void visitShopRows(final List<ShopRow> shopRows, Boolean force) {
    for (ShopRow shopRow : shopRows) {
      for (ShopRow existing : DATA.getShopRows()) {
        if (shopRow.getRow() == existing.getRow()) {
          existing.setItem(shopRow.getItem());
        }
      }
    }
  }

  public static String accessible() {
    // All you need is a flower to get to the shop
    int flowers =
        ROSE.getCount(KoLConstants.inventory)
            + WHITE_TULIP.getCount(KoLConstants.inventory)
            + RED_TULIP.getCount(KoLConstants.inventory)
            + BLUE_TULIP.getCount(KoLConstants.inventory);
    if (flowers < 1) {
      return "You have no roses or tulips";
    }
    return null;
  }
}
