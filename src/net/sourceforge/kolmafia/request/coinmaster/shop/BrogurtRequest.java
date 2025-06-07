package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public abstract class BrogurtRequest extends CoinMasterShopRequest {
  public static final String master = "The Frozen Brogurt Stand";
  public static final String SHOPID = "sbb_brogurt";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) Beach Bucks");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.BEACH_BUCK, 1);

  public static final CoinmasterData BROGURT =
      new CoinmasterData(master, "brogurt", BrogurtRequest.class)
          .withToken("Beach Buck")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, SHOPID)
          .withAccessible(BrogurtRequest::accessible)
          .withCanBuyItem(BrogurtRequest::canBuyItem);

  private static Boolean canBuyItem(final Integer itemId) {
    return switch (itemId) {
      case ItemPool.BROBERRY_BROGURT,
          ItemPool.BROCOLATE_BROGURT,
          ItemPool.FRENCH_BRONILLA_BROGURT -> Preferences.getString("questESlBacteria")
          .equals("finished");
      default -> {
        AdventureResult item = ItemPool.get(itemId, 1);
        yield item.getCount(BROGURT.getBuyItems()) > 0;
      }
    };
  }

  public static String accessible() {
    if (!Preferences.getBoolean("_sleazeAirportToday")
        && !Preferences.getBoolean("sleazeAirportAlways")) {
      return "You don't have access to Spring Break Beach";
    }
    if (KoLCharacter.getLimitMode().limitZone("Spring Break Beach")) {
      return "You cannot currently access Spring Break Beach";
    }
    return null;
  }
}
