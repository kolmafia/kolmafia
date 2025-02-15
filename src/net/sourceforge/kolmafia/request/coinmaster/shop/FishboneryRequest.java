package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public abstract class FishboneryRequest extends CoinMasterShopRequest {
  public static final String master = "Freshwater Fishbonery";
  public static final String SHOPID = "fishbones";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) freshwater fishbone");
  public static final AdventureResult FRESHWATER_FISHBONE =
      ItemPool.get(ItemPool.FRESHWATER_FISHBONE);

  public static final CoinmasterData FISHBONERY =
      new CoinmasterData(master, "Fishbonery", FishboneryRequest.class)
          .withToken("freshwater fishbone")
          .withTokenTest("no freshwater fishbones")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(FRESHWATER_FISHBONE)
          .withShopRowFields(master, SHOPID)
          .withAccessible(FishboneryRequest::accessible);

  public static String accessible() {
    if (FRESHWATER_FISHBONE.getCount(KoLConstants.inventory) == 0) {
      return "You do not have a freshwater fishbone in inventory";
    }
    return null;
  }
}
