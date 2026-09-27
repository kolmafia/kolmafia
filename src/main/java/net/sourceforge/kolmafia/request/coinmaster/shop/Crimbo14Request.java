package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public abstract class Crimbo14Request extends CoinMasterShopRequest {
  public static final String master = "Crimbo 2014";
  public static final String SHOPID = "crimbo14";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("<td>(no|[\\d,]) Crimbo Credit", Pattern.DOTALL);
  public static final AdventureResult CRIMBO_CREDIT = ItemPool.get(ItemPool.CRIMBO_CREDIT, 1);

  public static final CoinmasterData CRIMBO14 =
      new CoinmasterData(master, "crimbo14", Crimbo14Request.class)
          .inZone("Crimbo14")
          .withToken("Crimbo Credit")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(CRIMBO_CREDIT)
          .withShopRowFields(master, SHOPID)
          .withSellURL("shop.php?whichshop=crimbo14turnin")
          .withSellAction("buyitem")
          .withSellItems(master)
          .withSellPrices(master);
}
