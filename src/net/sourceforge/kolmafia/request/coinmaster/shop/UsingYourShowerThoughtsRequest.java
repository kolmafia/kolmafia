package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public abstract class UsingYourShowerThoughtsRequest extends CoinMasterShopRequest {
  public static final String master = "Using your Shower Thoughts";
  public static final String SHOPID = "showerthoughts";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) globs? of wet paper");
  public static final AdventureResult GLOB_OF_WET_PAPER =
      ItemPool.get(ItemPool.GLOB_OF_WET_PAPER, 1);

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "showerthoughts", UsingYourShowerThoughtsRequest.class)
          .withToken("glob of wet paper")
          .withPluralToken("globs of wet paper")
          .withTokenTest("no globs of wet paper")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(GLOB_OF_WET_PAPER)
          .withNewShopRowFields(master, SHOPID)
          .withAccessible(UsingYourShowerThoughtsRequest::accessible);

  public static String accessible() {
    if (GLOB_OF_WET_PAPER.getCount(KoLConstants.inventory) == 0) {
      return "You do not have a glob of wet paper in inventory";
    }
    return null;
  }
}
