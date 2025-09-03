package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public abstract class FixodentRequest extends CoinMasterShopRequest {
  public static final String master = "Craft with Teeth";
  public static final String SHOPID = "fixodent";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, FixodentRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withCanBuyItem(FixodentRequest::canBuyItem);

  private static Boolean canBuyItem(final Integer itemId) {
    return switch (itemId) {
      case ItemPool.DENTADENT -> KoLCharacter.hasEquipped(ItemPool.MONODENT_OF_THE_SEA);
      default -> DATA.availableItem(itemId);
    };
  }
}
