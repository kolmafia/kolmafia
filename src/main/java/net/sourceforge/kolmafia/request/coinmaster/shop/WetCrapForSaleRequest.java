package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public abstract class WetCrapForSaleRequest extends CoinMasterShopRequest {
  public static final String master = "Wet Crap For Sale";
  public static final String SHOPID = "sandpenny";

  public static final AdventureResult TOKEN = ItemPool.get(ItemPool.SAND_PENNY, 1);
  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) sand penn");

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "sandpenny", WetCrapForSaleRequest.class)
          .withToken("sand penny")
          .withPluralToken("sand pennies")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(TOKEN)
          .withNewShopRowFields(master, SHOPID)
          .withAccessible(WetCrapForSaleRequest::accessible);

  public static String accessible() {
    if (!KoLCharacter.inSeaPath()) {
      return "You can't buy with sand pennies outside 11,037 Leagues Under the Sea";
    }
    return null;
  }
}
