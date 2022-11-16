package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public class BuffJimmyRequest extends CoinMasterRequest {
  public static final String master = "Buff Jimmy's Souvenir Shop";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) Beach Bucks");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.BEACH_BUCK, 1);

  public static final CoinmasterData BUFF_JIMMY =
      new CoinmasterData(master, "BuffJimmy", BuffJimmyRequest.class)
          .withToken("Beach Buck")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, "sbb_jimmy");

  public BuffJimmyRequest() {
    super(BUFF_JIMMY);
  }

  public BuffJimmyRequest(final boolean buying, final AdventureResult[] attachments) {
    super(BUFF_JIMMY, buying, attachments);
  }

  public BuffJimmyRequest(final boolean buying, final AdventureResult attachment) {
    super(BUFF_JIMMY, buying, attachment);
  }

  public BuffJimmyRequest(final boolean buying, final int itemId, final int quantity) {
    super(BUFF_JIMMY, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=sbb_jimmy")) {
      return;
    }

    CoinmasterData data = BUFF_JIMMY;

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=sbb_jimmy")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(BUFF_JIMMY, urlString, true);
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
