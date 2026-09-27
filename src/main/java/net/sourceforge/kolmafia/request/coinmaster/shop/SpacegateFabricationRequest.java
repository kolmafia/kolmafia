package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public abstract class SpacegateFabricationRequest extends CoinMasterShopRequest {
  public static final String master = "Spacegate Fabrication Facility";
  public static final String SHOPID = "spacegate";

  private static final Pattern RESEARCH_PATTERN =
      Pattern.compile("([\\d,]+) pages? of Spacegate Research");
  public static final AdventureResult RESEARCH = ItemPool.get(ItemPool.SPACEGATE_RESEARCH, 1);

  public static final CoinmasterData SPACEGATE_STORE =
      new CoinmasterData(master, "spacegate", SpacegateFabricationRequest.class)
          .withToken("Spacegate Research")
          .withTokenTest("no pages of Spacegate Research")
          .withTokenPattern(RESEARCH_PATTERN)
          .withItem(RESEARCH)
          .withShopRowFields(master, SHOPID)
          .withAccessible(SpacegateFabricationRequest::accessible);

  public static String accessible() {
    if (!Preferences.getBoolean("_spacegateToday") && !Preferences.getBoolean("spacegateAlways")) {
      return "You can't get to the Spacegate.";
    }
    return null;
  }
}
