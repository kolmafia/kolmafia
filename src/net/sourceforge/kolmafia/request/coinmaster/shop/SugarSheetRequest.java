package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public abstract class SugarSheetRequest extends CoinMasterShopRequest {
  public static final String master = "Sugar Sheet Folding";
  public static final String SHOPID = "sugarsheets";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, SugarSheetRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withCanBuyItem(SugarSheetRequest::canBuyItem);

  private static Boolean canBuyItem(final Integer itemId) {
    return switch (itemId) {
      case ItemPool.SUGAR_SHIRT -> KoLCharacter.isTorsoAware();
      default -> DATA.availableItem(itemId);
    };
  }
}
