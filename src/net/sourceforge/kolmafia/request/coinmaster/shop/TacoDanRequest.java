package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class TacoDanRequest extends CoinMasterRequest {
  public static final String master = "Taco Dan's Taco Stand";
  public static final String SHOPID = "sbb_taco";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) Beach Bucks");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.BEACH_BUCK, 1);

  public static final CoinmasterData TACO_DAN =
      new CoinmasterData(master, "taco_dan", TacoDanRequest.class)
          .withToken("Beach Buck")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, SHOPID)
          .withCanBuyItem(TacoDanRequest::canBuyItem)
          .withAccessible(TacoDanRequest::accessible);

  private static Boolean canBuyItem(final Integer itemId) {
    return switch (itemId) {
      case ItemPool.TACO_DAN_FISH_TACO -> Preferences.getString("questESlFish").equals("finished");
      case ItemPool.TACO_DAN_TACO_SAUCE -> Preferences.getString("questESlSprinkles")
          .equals("finished");
      default -> ItemPool.get(itemId).getCount(TACO_DAN.getBuyItems()) > 0;
    };
  }

  public TacoDanRequest() {
    super(TACO_DAN);
  }

  public TacoDanRequest(final boolean buying, final AdventureResult[] attachments) {
    super(TACO_DAN, buying, attachments);
  }

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), this.responseText);
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
