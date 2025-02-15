package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public abstract class DiscoGiftCoRequest extends CoinMasterShopRequest {
  public static final String master = "Disco GiftCo";
  public static final String SHOPID = "infernodisco";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) Volcoino");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.VOLCOINO, 1);

  public static final CoinmasterData DISCO_GIFTCO =
      new CoinmasterData(master, "DiscoGiftCo", DiscoGiftCoRequest.class)
          .withToken("Volcoino")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, SHOPID)
          .withAccessible(DiscoGiftCoRequest::accessible);

  public static String accessible() {
    if (!Preferences.getBoolean("_hotAirportToday")
        && !Preferences.getBoolean("hotAirportAlways")) {
      return "You don't have access to That 70s Volcano";
    }
    if (KoLCharacter.getLimitMode().limitZone("That 70s Volcano")) {
      return "You cannot currently access That 70s Volcano";
    }
    return null;
  }
}
