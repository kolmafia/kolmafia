package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.session.LimitMode;

public class EdShopRequest extends CoinMasterRequest {
  public static final String master = "Everything Under the World";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) Ka coin");
  public static final AdventureResult KA = ItemPool.get(ItemPool.KA_COIN, 1);

  public static final CoinmasterData EDSHOP =
      new CoinmasterData(master, "Everything Under the World", EdShopRequest.class)
          .withToken("Ka coin")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(KA)
          .withShopRowFields(master, "edunder_shopshop");

  public EdShopRequest() {
    super(EDSHOP);
  }

  public EdShopRequest(final boolean buying, final AdventureResult[] attachments) {
    super(EDSHOP, buying, attachments);
  }

  public EdShopRequest(final boolean buying, final AdventureResult attachment) {
    super(EDSHOP, buying, attachment);
  }

  public EdShopRequest(final boolean buying, final int itemId, final int quantity) {
    super(EDSHOP, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.startsWith("shop.php") || !location.contains("whichshop=edunder_shopshop")) {
      return;
    }

    CoinmasterData data = EDSHOP;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    if (!KoLCharacter.isEd()) {
      return "Only Ed can come here.";
    }
    if (KoLCharacter.getLimitMode() != LimitMode.ED) {
      return "You must be in the Underworld to shop here.";
    }
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=edunder_shopshop")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(EDSHOP, urlString, true);
  }
}
