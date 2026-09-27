package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public abstract class PixelRequest extends CoinMasterShopRequest {
  public static final String master = "The Crackpot Mystic's Shed";
  public static final String SHOPID = "mystic";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, PixelRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withAccessible(PixelRequest::accessible)
          .withCanBuyItem(PixelRequest::canBuyItem)
          // implementation does not use ajax at all
          .withAjax(false);

  public static String accessible() {
    if (KoLCharacter.isKingdomOfExploathing()) {
      return "The Kingdom has exploded, and the mystic is nowhere to be found.";
    }
    return null;
  }

  private static Boolean canBuyItem(final Integer itemId) {
    // The following is not complete:
    // Items unlocked by beating all four bosses in the Crackpot Mystic's Psychoses:
    //    pixel energy tank
    //    pixel grappling hook
    //    pixel pill
    return switch (itemId) {
      case ItemPool.YELLOW_SUBMARINE -> !KoLCharacter.desertBeachAccessible();
      default -> DATA.availableItem(itemId);
    };
  }
}
