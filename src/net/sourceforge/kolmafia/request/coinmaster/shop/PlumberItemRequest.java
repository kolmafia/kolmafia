package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;

public class PlumberItemRequest extends CoinMasterRequest {
  public static final String master = "Mushroom District Item Shop";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("([\\d,]+) coin");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.COIN, 1);

  public static final CoinmasterData PLUMBER_ITEMS =
      new CoinmasterData(master, "marioitems", PlumberItemRequest.class)
          .withToken("coin")
          .withTokenTest("no coins")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, "marioitems")
          .withNeedsPasswordHash(true);

  public PlumberItemRequest() {
    super(PLUMBER_ITEMS);
  }

  public PlumberItemRequest(final boolean buying, final AdventureResult[] attachments) {
    super(PLUMBER_ITEMS, buying, attachments);
  }

  public PlumberItemRequest(final boolean buying, final AdventureResult attachment) {
    super(PLUMBER_ITEMS, buying, attachment);
  }

  public PlumberItemRequest(final boolean buying, final int itemId, final int quantity) {
    super(PLUMBER_ITEMS, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    CoinmasterData data = PLUMBER_ITEMS;
    String action = GenericRequest.getAction(location);
    if (action == null) {
      if (location.contains("whichshop=marioitems")) {
        // Parse current coin balances
        CoinMasterRequest.parseBalance(data, responseText);
      }

      return;
    }

    CoinMasterRequest.parseResponse(data, location, responseText);
  }

  public static String accessible() {
    if (!KoLCharacter.isPlumber()) {
      return "You are not a plumber.";
    }
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=marioitems")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(PLUMBER_ITEMS, urlString, true);
  }
}
