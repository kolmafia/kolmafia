package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.BatManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.LimitMode;

public abstract class GotporkOrphanageRequest extends CoinMasterShopRequest {
  public static final String master = "Gotpork Orphanage";
  public static final String SHOPID = "batman_orphanage";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) kidnapped orphan");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.KIDNAPPED_ORPHAN, 1);

  public static final CoinmasterData GOTPORK_ORPHANAGE =
      new CoinmasterData(master, "Gotpork Orphanage", GotporkOrphanageRequest.class)
          .withToken("kidnapped orphan")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, SHOPID)
          .withItemBuyPrice(GotporkOrphanageRequest::itemBuyPrice)
          .withAccessible(GotporkOrphanageRequest::accessible);

  private static AdventureResult itemBuyPrice(final Integer itemId) {
    int price = GOTPORK_ORPHANAGE.getBuyPrices().get(itemId);
    if (price == 1) {
      return COIN;
    }
    // price increased by 3 each time you buy one
    int count = InventoryManager.getCount(itemId);
    if (count > 0) {
      price = 3 * (count + 1);
    }
    return COIN.getInstance(price);
  }

  public static String accessible() {
    if (KoLCharacter.getLimitMode() != LimitMode.BATMAN) {
      return "Only Batfellow can go to the Gotpork Orphanage.";
    }
    if (BatManager.currentBatZone() != BatManager.DOWNTOWN) {
      return "Batfellow can only visit the Gotpork Orphanage while Downtown.";
    }
    return null;
  }
}
