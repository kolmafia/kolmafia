package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;

public class FishboneryRequest extends CoinMasterRequest {
  public static final String master = "Freshwater Fishbonery";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) freshwater fishbone");
  public static final AdventureResult FRESHWATER_FISHBONE =
      ItemPool.get(ItemPool.FRESHWATER_FISHBONE);

  public static final CoinmasterData FISHBONERY =
      new CoinmasterData(master, "Fishbonery", FishboneryRequest.class)
          .withToken("freshwater fishbone")
          .withTokenTest("no freshwater fishbones")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(FRESHWATER_FISHBONE)
          .withShopRowFields(master, "fishbones");

  public FishboneryRequest() {
    super(FISHBONERY);
  }

  public FishboneryRequest(final boolean buying, final AdventureResult[] attachments) {
    super(FISHBONERY, buying, attachments);
  }

  public FishboneryRequest(final boolean buying, final AdventureResult attachment) {
    super(FISHBONERY, buying, attachment);
  }

  public FishboneryRequest(final boolean buying, final int itemId, final int quantity) {
    super(FISHBONERY, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.startsWith("shop.php") || !location.contains("whichshop=fishbones")) {
      return;
    }

    CoinmasterData data = FISHBONERY;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    if (FRESHWATER_FISHBONE.getCount(KoLConstants.inventory) == 0) {
      return "You do not have a freshwater fishbone in inventory";
    }
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    // shop.php?pwd&whichshop=fishbones
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=fishbones")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(FISHBONERY, urlString, true);
  }
}
