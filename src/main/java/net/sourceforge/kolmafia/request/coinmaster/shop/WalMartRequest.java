package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public abstract class WalMartRequest extends CoinMasterShopRequest {
  public static final String master = "Wal-Mart";
  public static final String SHOPID = "glaciest";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("<td>([\\d,]+) Wal-Mart gift certificates");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.WALMART_GIFT_CERTIFICATE, 1);

  public static final CoinmasterData WALMART =
      new CoinmasterData(master, "Wal-Mart", WalMartRequest.class)
          .withToken("Wal-Mart gift certificate")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, SHOPID)
          .withAccessible(WalMartRequest::accessible);

  public static String accessible() {
    if (!Preferences.getBoolean("_coldAirportToday")
        && !Preferences.getBoolean("coldAirportAlways")) {
      return "You don't have access to The Glaciest";
    }
    if (KoLCharacter.getLimitMode().limitZone("The Glaciest")) {
      return "You cannot currently access The Glaciest";
    }
    return null;
  }
}
