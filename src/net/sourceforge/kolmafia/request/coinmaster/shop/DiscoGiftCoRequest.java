package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;

public class DiscoGiftCoRequest extends CoinMasterRequest {
  public static final String master = "Disco GiftCo";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) Volcoino");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.VOLCOINO, 1);

  public static final CoinmasterData DISCO_GIFTCO =
      new CoinmasterData(master, "DiscoGiftCo", DiscoGiftCoRequest.class)
          .withToken("Volcoino")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, "infernodisco");

  public DiscoGiftCoRequest() {
    super(DISCO_GIFTCO);
  }

  public DiscoGiftCoRequest(final boolean buying, final AdventureResult[] attachments) {
    super(DISCO_GIFTCO, buying, attachments);
  }

  public DiscoGiftCoRequest(final boolean buying, final AdventureResult attachment) {
    super(DISCO_GIFTCO, buying, attachment);
  }

  public DiscoGiftCoRequest(final boolean buying, final int itemId, final int quantity) {
    super(DISCO_GIFTCO, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=infernodisco")) {
      return;
    }

    CoinmasterData data = DISCO_GIFTCO;

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=infernodisco")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(DISCO_GIFTCO, urlString, true);
  }

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
