package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.QuestManager;

public abstract class NeandermallRequest extends CoinMasterShopRequest {
  public static final String master = "The Neandermall";
  public static final String SHOPID = "caveshop";

  private static final Pattern CHRONER_PATTERN = Pattern.compile("([\\d,]+) Chroner");
  public static final AdventureResult CHRONER = ItemPool.get(ItemPool.CHRONER, 1);

  public static final CoinmasterData NEANDERMALL =
      new CoinmasterData(master, "caveshop", NeandermallRequest.class)
          .withToken("Chroner")
          .withTokenTest("no Chroner")
          .withTokenPattern(CHRONER_PATTERN)
          .withItem(CHRONER)
          .withShopRowFields(master, SHOPID)
          .withVisitShop(NeandermallRequest::visitShop)
          .withAccessible(NeandermallRequest::accessible);

  public static void visitShop(final String responseText) {
    QuestManager.handleTimeTower(!responseText.contains("That store isn't there anymore."));
  }

  public static String accessible() {
    if (!Preferences.getBoolean("timeTowerAvailable")) {
      return "You can't get to the Neandermall";
    }
    return null;
  }
}
