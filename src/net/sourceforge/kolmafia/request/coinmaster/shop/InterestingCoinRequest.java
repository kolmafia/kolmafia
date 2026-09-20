package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.List;
import java.util.Set;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.shop.ShopRow;

public abstract class InterestingCoinRequest extends CoinMasterShopRequest {
  public static final String master = "Spend your Interesting Coins";
  public static final String SHOPID = "interesting";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, InterestingCoinRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withVisitShopRows(InterestingCoinRequest::visitShopRows)
          .withCanBuyItem(InterestingCoinRequest::canBuyItem)
          .withVisitShop(InterestingCoinRequest::visitShop)
          .withPurchasedItem(InterestingCoinRequest::purchasedItem)
          .withCountField(null);

  private static String dailyProperty(final int itemId) {
    return "_itemBoughtPerDay" + itemId;
  }

  private static String ascensionProperty(final int itemId) {
    return "itemBoughtPerAscension" + itemId;
  }

  // Limited Daily Stock (3 per day)
  private static final Set<Integer> DAILY_ITEMS =
      Set.of(
          ItemPool.LIQUID_ASSET,
          ItemPool.INTANGIBLE_ASSET,
          ItemPool.TOXIC_ASSET,
          ItemPool.INVISIBLE_HAND,
          ItemPool.CIRCLE_OF_OVERDRAFT_PROTECTION_SCROLL,
          ItemPool.MINT,
          ItemPool.SOLVENT,
          ItemPool.ROTH_IPA,
          ItemPool.SAVINGS_BONDO,
          ItemPool.SOYBEAN_FUTURES);

  // Prices increase as items are purchased
  private static final Set<Integer> INFLATION_ITEMS =
      Set.of(
          ItemPool.HOMEOWNERS_LOAM,
          ItemPool.HOUSING_BUBBLE,
          ItemPool.BALANCE_SHEET,
          ItemPool.GROSS_PROPHET_CHRYSALIS);

  // Once in a Lifetime Deals (1 per ascension)
  private static final Set<Integer> ASCENSION_ITEMS =
      Set.of(
          ItemPool.UNDERDRAFT_PROTECTION,
          ItemPool.GOLD_401K_RING,
          ItemPool.HEDGE_FUND_CLIPPERS,
          ItemPool.FINANCIAL_INSTRUMENT,
          ItemPool.SELLING_SHORTS,
          ItemPool.BEAR_TATTOO);

  private static Boolean canBuyItem(final Integer itemId) {
    if (DAILY_ITEMS.contains(itemId)) {
      return Preferences.getInteger(dailyProperty(itemId)) < 3;
    }
    if (ASCENSION_ITEMS.contains(itemId)) {
      return !Preferences.getBoolean(ascensionProperty(itemId));
    }
    return DATA.availableItem(itemId);
  }

  public static void visitShop(final String responseText) {
    for (int itemId : DAILY_ITEMS) {
      String name = ItemDatabase.getItemName(itemId);
      if (!responseText.contains(name)) {
        Preferences.setInteger(dailyProperty(itemId), 3);
      }
    }
    for (int itemId : ASCENSION_ITEMS) {
      String name = ItemDatabase.getItemName(itemId);
      Preferences.setBoolean(ascensionProperty(itemId), !responseText.contains(name));
    }
  }

  public static void purchasedItem(final AdventureResult item, final Boolean storage) {
    int itemId = item.getItemId();
    if (DAILY_ITEMS.contains(itemId)) {
      Preferences.increment(dailyProperty(itemId), 1, 3, false);
    } else if (ASCENSION_ITEMS.contains(itemId)) {
      Preferences.setBoolean(ascensionProperty(itemId), true);
    }
  }

  public static void visitShopRows(final List<ShopRow> shopRows, Boolean force) {
    for (ShopRow shopRow : shopRows) {
      int itemId = shopRow.getItem().getItemId();
      if (INFLATION_ITEMS.contains(itemId)) {
        DATA.getShopRow(itemId).setCosts(shopRow.getCosts());
      }
    }
  }
}
