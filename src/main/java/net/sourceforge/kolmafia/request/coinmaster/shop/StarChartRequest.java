package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public abstract class StarChartRequest extends CoinMasterShopRequest {
  public static final String master = "A Star Chart";
  public static final String SHOPID = "starchart";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, StarChartRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withCanBuyItem(StarChartRequest::canBuyItem)
          // trying to buy multiple star items at once fails
          .withCountField(null);

  private static Boolean canBuyItem(final Integer itemId) {
    return switch (itemId) {
      case ItemPool.STAR_SHIRT -> KoLCharacter.isTorsoAware();
      default -> DATA.availableItem(itemId);
    };
  }
}
