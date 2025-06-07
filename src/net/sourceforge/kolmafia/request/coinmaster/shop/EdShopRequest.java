package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.LimitMode;

public abstract class EdShopRequest extends CoinMasterShopRequest {
  public static final String master = "Everything Under the World";
  public static final String SHOPID = "edunder_shopshop";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) Ka coin");
  public static final AdventureResult KA = ItemPool.get(ItemPool.KA_COIN, 1);

  public static final CoinmasterData EDSHOP =
      new CoinmasterData(master, "Everything Under the World", EdShopRequest.class)
          .withToken("Ka coin")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(KA)
          .withShopRowFields(master, SHOPID)
          .withAccessible(EdShopRequest::accessible);

  public static String accessible() {
    if (!KoLCharacter.isEd()) {
      return "Only Ed can come here.";
    }
    if (KoLCharacter.getLimitMode() != LimitMode.ED) {
      return "You must be in the Underworld to shop here.";
    }
    return null;
  }
}
