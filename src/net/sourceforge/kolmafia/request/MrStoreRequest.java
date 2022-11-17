package net.sourceforge.kolmafia.request;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MrStoreRequest extends CoinMasterRequest {
  public static final String master = "Mr. Store";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("You have (\\w+) Mr. Accessor(?:y|ies) to trade.");

  public static final AdventureResult MR_A = ItemPool.get(ItemPool.MR_ACCESSORY, 1);
  public static final AdventureResult UNCLE_B = ItemPool.get(ItemPool.UNCLE_BUCK, 1);

  // Since there are two different currencies, we need to have a map from
  // itemId to item/count of currency; an AdventureResult.
  private static final Map<Integer, AdventureResult> buyCosts = new TreeMap<>();

  public static final CoinmasterData MR_STORE =
      new CoinmasterData(master, "mrstore", MrStoreRequest.class)
          .withToken("Mr. A")
          .withTokenTest("You have no Mr. Accessories to trade")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(MR_A)
          .withBuyURL("mrstore.php")
          .withBuyAction("buy")
          .withBuyItems()
          .withBuyPrices()
          .withItemField("whichitem")
          .withItemPattern(GenericRequest.WHICHITEM_PATTERN)
          .withItemBuyPrice(MrStoreRequest::itemBuyPrice);

  private static AdventureResult itemBuyPrice(final Integer itemId) {
    return buyCosts.get(itemId);
  }

  public MrStoreRequest() {
    super(MR_STORE);
  }

  public MrStoreRequest(final String action) {
    super(MR_STORE, action);
  }

  public MrStoreRequest(final boolean buying, final AdventureResult[] attachments) {
    super(MR_STORE, buying, attachments);
  }

  public MrStoreRequest(final boolean buying, final AdventureResult attachment) {
    super(MR_STORE, buying, attachment);
  }

  public MrStoreRequest(final boolean buying, final int itemId, final int quantity) {
    super(MR_STORE, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    String responseText = this.responseText;
    if (this.action != null
        && (this.action.equals("pullmras") || this.action.equals("pullunclebs"))) {
      // You can't pull any more items out of storage today.
      if (responseText.indexOf("You can't pull any more items out of storage today") != -1) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "You can't pull any more items out of storage today.");
      }
    }

    parseResponse(this.getURLString(), responseText);
  }

  private static final Pattern ITEM_PATTERN =
      Pattern.compile(
          "onClick='javascript:descitem\\((\\d+)\\)' class=nounder>(.*?)</a></b>.*?title=\"(.*?)\".*?<font size=\\+1>(\\d)</font></b></td><form name=buy(\\d+)");

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("mrstore.php")) {
      return;
    }

    // Learn new Mr. Items by simply visiting Mr. Store
    // Refresh the Coin Master inventory every time we visit.

    CoinmasterData data = MR_STORE;
    List<AdventureResult> items = data.getBuyItems();
    Map<Integer, AdventureResult> costs = buyCosts;
    items.clear();
    costs.clear();

    Matcher matcher = ITEM_PATTERN.matcher(responseText);
    while (matcher.find()) {
      String descId = matcher.group(1);
      String itemName = matcher.group(2).trim();
      String currency = matcher.group(3);
      int price = StringUtilities.parseInt(matcher.group(4));
      int itemId = StringUtilities.parseInt(matcher.group(5));

      String match = ItemDatabase.getItemDataName(itemId);
      if (match == null || !match.equals(itemName)) {
        ItemDatabase.registerItem(itemId, itemName, descId);
      }

      // Add it to the Mr. Store inventory
      AdventureResult item = ItemPool.get(itemId, PurchaseRequest.MAX_QUANTITY);
      items.add(item);
      AdventureResult cost = ItemPool.get(currency, price);
      costs.put(itemId, cost);
    }

    // Register the purchase requests, now that we know what is available
    data.registerPurchaseRequests();

    // If we performed a Currency Exchange, account for it
    String action = GenericRequest.getAction(urlString);

    if (action != null && action.equals("a_to_b")) {
      if (responseText.contains("You acquire")) {
        ResultProcessor.processItem(ItemPool.MR_ACCESSORY, -1);
        CoinMasterRequest.parseBalance(data, responseText);
      }
      return;
    }

    if (action != null && action.equals("b_to_a")) {
      if (responseText.contains("You acquire")) {
        ResultProcessor.processItem(ItemPool.UNCLE_BUCK, -10);
        CoinMasterRequest.parseBalance(data, responseText);
      }
      return;
    }

    if (action != null && action.equals("pullmras")) {
      if (responseText.contains("You acquire")) {
        // We pulled a Mr. A from storage.
        AdventureResult remove = MR_A.getInstance(-1);
        AdventureResult.addResultToList(KoLConstants.storage, remove);
        CoinMasterRequest.parseBalance(data, responseText);
      }
      return;
    }

    if (action != null && action.equals("pullunclebs")) {
      if (responseText.contains("You acquire")) {
        // We pulled an Uncle B from storage.
        AdventureResult remove = UNCLE_B.getInstance(-1);
        AdventureResult.addResultToList(KoLConstants.storage, remove);
        CoinMasterRequest.parseBalance(data, responseText);
      }
      return;
    }

    CoinMasterRequest.parseResponse(data, urlString, responseText);

    // If we bought a Golden Mr. Accessory, it is now in inventory
    InventoryManager.countGoldenMrAccesories();
  }

  public static String accessible() {
    return null;
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("mrstore.php")) {
      return false;
    }

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      String message =
          action.equals("pullmras")
              ? "Pulling a Mr. Accessory from storage"
              : action.equals("pullunclebs") ? "Pulling an Uncle Buck from storage" : null;
      if (message != null) {
        RequestLogger.updateSessionLog();
        RequestLogger.updateSessionLog(message);
        return true;
      }
    }

    return CoinMasterRequest.registerRequest(MR_STORE, urlString, true);
  }
}
