package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public class TacoDanRequest extends CoinMasterRequest {
  public static final String master = "Taco Dan's Taco Stand";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) Beach Bucks");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.BEACH_BUCK, 1);

  public static final CoinmasterData TACO_DAN =
      new CoinmasterData(master, "taco_dan", TacoDanRequest.class)
          .withToken("Beach Buck")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, "sbb_taco")
          .withCanBuyItem(TacoDanRequest::canBuyItem);

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

  public TacoDanRequest(final boolean buying, final AdventureResult attachment) {
    super(TACO_DAN, buying, attachment);
  }

  public TacoDanRequest(final boolean buying, final int itemId, final int quantity) {
    super(TACO_DAN, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=sbb_taco")) {
      return;
    }

    CoinmasterData data = TACO_DAN;

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=sbb_taco")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(TACO_DAN, urlString, true);
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
