package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public abstract class ToxicChemistryRequest extends CoinMasterShopRequest {
  public static final String master = "Toxic Chemistry";
  public static final String SHOPID = "toxic";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) toxic globule");
  public static final AdventureResult TOXIC_GLOBULE = ItemPool.get(ItemPool.TOXIC_GLOBULE, 1);

  public static final CoinmasterData TOXIC_CHEMISTRY =
      new CoinmasterData(master, "ToxicChemistry", ToxicChemistryRequest.class)
          .withToken("toxic globule")
          .withTokenTest("no toxic globules")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(TOXIC_GLOBULE)
          .withShopRowFields(master, SHOPID)
          .withAccessible(ToxicChemistryRequest::accessible);

  public static String accessible() {
    if (TOXIC_GLOBULE.getCount(KoLConstants.inventory) == 0) {
      return "You do not have a toxic globule in inventory";
    }
    return null;
  }
}
