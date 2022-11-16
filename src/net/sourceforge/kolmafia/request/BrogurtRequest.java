package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public class BrogurtRequest extends CoinMasterRequest {
  public static final String master = "The Frozen Brogurt Stand";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) Beach Bucks");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.BEACH_BUCK, 1);

  public static final CoinmasterData BROGURT =
      new CoinmasterData(master, "brogurt", BrogurtRequest.class)
          .withToken("Beach Buck")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, "sbb_brogurt")
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

  public BrogurtRequest() {
    super(BROGURT);
  }

  public BrogurtRequest(final boolean buying, final AdventureResult[] attachments) {
    super(BROGURT, buying, attachments);
  }

  public BrogurtRequest(final boolean buying, final AdventureResult attachment) {
    super(BROGURT, buying, attachment);
  }

  public BrogurtRequest(final boolean buying, final int itemId, final int quantity) {
    super(BROGURT, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=sbb_brogurt")) {
      return;
    }

    CoinmasterData data = BROGURT;

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=sbb_brogurt")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(BROGURT, urlString, true);
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
