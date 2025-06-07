package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.CampAwayRequest;

public abstract class YourCampfireRequest extends CoinMasterShopRequest {
  public static final String master = "Your Campfire";
  public static final String SHOPID = "campfire";

  private static final Pattern FIREWOOD_PATTERN = Pattern.compile("([\\d,]+) sticks? of firewood");
  public static final AdventureResult STICK_OF_FIREWOOD =
      ItemPool.get(ItemPool.STICK_OF_FIREWOOD, 1);

  public static final CoinmasterData YOUR_CAMPFIRE =
      new CoinmasterData(master, "campfire", YourCampfireRequest.class)
          .withToken("stick of firewood")
          .withTokenTest("no sticks of firewood")
          .withTokenPattern(FIREWOOD_PATTERN)
          .withItem(STICK_OF_FIREWOOD)
          .withShopRowFields(master, SHOPID)
          .withAccessible(YourCampfireRequest::accessible);

  public static String accessible() {
    if (CampAwayRequest.campAwayTentAvailable()) {
      return null;
    }
    return "Need access to your Getaway Campsite";
  }
}
