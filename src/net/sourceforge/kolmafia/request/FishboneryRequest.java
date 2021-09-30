package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class FishboneryRequest extends CoinMasterRequest {
  public static final String master = "Freshwater Fishbonery";

  public static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(FishboneryRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(FishboneryRequest.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(FishboneryRequest.master);

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) freshwater fishbone");
  public static final AdventureResult FRESHWATER_FISHBONE =
      ItemPool.get(ItemPool.FRESHWATER_FISHBONE, 1);

  public static final CoinmasterData FISHBONERY =
      new CoinmasterData(
          FishboneryRequest.master,
          "Fishbonery",
          FishboneryRequest.class,
          "freshwater fishbone",
          "no freshwater fishbones",
          false,
          FishboneryRequest.TOKEN_PATTERN,
          FishboneryRequest.FRESHWATER_FISHBONE,
          null,
          FishboneryRequest.itemRows,
          "shop.php?whichshop=fishbones",
          "buyitem",
          FishboneryRequest.buyItems,
          FishboneryRequest.buyPrices,
          null,
          null,
          null,
          null,
          "whichrow",
          GenericRequest.WHICHROW_PATTERN,
          "quantity",
          GenericRequest.QUANTITY_PATTERN,
          null,
          null,
          true);

  public FishboneryRequest() {
    super(FishboneryRequest.FISHBONERY);
  }

  public FishboneryRequest(final boolean buying, final AdventureResult[] attachments) {
    super(FishboneryRequest.FISHBONERY, buying, attachments);
  }

  public FishboneryRequest(final boolean buying, final AdventureResult attachment) {
    super(FishboneryRequest.FISHBONERY, buying, attachment);
  }

  public FishboneryRequest(final boolean buying, final int itemId, final int quantity) {
    super(FishboneryRequest.FISHBONERY, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    FishboneryRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.startsWith("shop.php") || !location.contains("whichshop=fishbones")) {
      return;
    }

    CoinmasterData data = FishboneryRequest.FISHBONERY;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    if (FishboneryRequest.FRESHWATER_FISHBONE.getCount(KoLConstants.inventory) == 0) {
      return "You do not have a freshwater fishbone in inventory";
    }
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    // shop.php?pwd&whichshop=fishbones
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=fishbones")) {
      return false;
    }

    CoinmasterData data = FishboneryRequest.FISHBONERY;
    return CoinMasterRequest.registerRequest(data, urlString, true);
  }
}
