package net.sourceforge.kolmafia.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ArmoryAndLeggeryRequest extends CoinMasterRequest {
  public static final String master = "Armory & Leggery";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) FDKOL commendation");

  // Since there are multiple, we need to have a map from itemId to
  // item/count of currency; an AdventureResult.
  private static final Map<Integer, AdventureResult> buyCosts = new TreeMap<>();

  public static final CoinmasterData ARMORY_AND_LEGGERY =
      new CoinmasterData(master, "armory", ArmoryAndLeggeryRequest.class)
          .withShopRowFields(master, "armory")
          .withItemRows(CoinmastersDatabase.getOrMakeRows(master))
          .withBuyItems()
          .withBuyPrices()
          .withItemBuyPrice(ArmoryAndLeggeryRequest::itemBuyPrice);

  private static AdventureResult itemBuyPrice(final int itemId) {
    return buyCosts.get(itemId);
  }

  public ArmoryAndLeggeryRequest() {
    super(ARMORY_AND_LEGGERY);
  }

  public ArmoryAndLeggeryRequest(final boolean buying, final AdventureResult[] attachments) {
    super(ARMORY_AND_LEGGERY, buying, attachments);
  }

  public ArmoryAndLeggeryRequest(final boolean buying, final AdventureResult attachment) {
    super(ARMORY_AND_LEGGERY, buying, attachment);
  }

  public ArmoryAndLeggeryRequest(final boolean buying, final int itemId, final int quantity) {
    super(ARMORY_AND_LEGGERY, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), responseText);
  }

  // <tr rel="7985"><td valign=center></td><td><img
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/polyparachute.gif"
  // class=hand onClick='javascript:descitem(973760204)'></td><td valign=center><a
  // onClick='javascript:descitem(973760204)'><b>polyester
  // parachute</b>&nbsp;&nbsp;&nbsp;&nbsp;</a></td><td><img
  // src=https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/wickerbits.gif width=30
  // height=30 onClick='javascript:descitem(134381888)' alt="wickerbits"
  // title="wickerbits"></td><td><b>1</b>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td valign=center><input class="button doit multibuy "  type=button rel='shop.php?whichshop=armory&action=buyitem&quantity=1&whichrow=804&pwd=' value='Buy'></td></tr>

  public static final Pattern ITEM_PATTERN =
      Pattern.compile(
          "<tr rel=\"(\\d+)\">.*?onClick='javascript:descitem\\((\\d+)\\)'>.*?<b>(.*?)</b>.*?title=\"(.*?)\".*?<b>([\\d,]+)</b>.*?whichrow=(\\d+)",
          Pattern.DOTALL);

  public static record CoinmasterItem(
      int itemId, String itemName, String currency, int price, int row) {}

  public static CoinmasterItem parseCoinmasterItem(Matcher matcher) {
    int itemId = StringUtilities.parseInt(matcher.group(1));
    Integer iitemId = itemId;
    String descId = matcher.group(2);
    String itemName = matcher.group(3).trim();
    String currency = matcher.group(4);
    int price = StringUtilities.parseInt(matcher.group(5));
    int row = StringUtilities.parseInt(matcher.group(6));

    // The currency must be an item
    if (currency.equals("Meat")) {
      return null;
    }

    // We can learn new items from this shop
    String match = ItemDatabase.getItemDataName(itemId);
    if (match == null || !match.equals(itemName)) {
      ItemDatabase.registerItem(itemId, itemName, descId);
    }

    return new CoinmasterItem(itemId, itemName, currency, price, row);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=armory")) {
      return;
    }

    // Learn new items by simply visiting the Armory & Leggery
    // Refresh the Coin Master inventory every time we visit.

    CoinmasterData data = ARMORY_AND_LEGGERY;

    List<AdventureResult> items = new ArrayList<>();
    Map<Integer, AdventureResult> costs = new TreeMap<>();
    Map<Integer, Integer> rows = new TreeMap<>();

    Matcher matcher = ITEM_PATTERN.matcher(responseText);
    while (matcher.find()) {
      CoinmasterItem reward = parseCoinmasterItem(matcher);

      AdventureResult item = ItemPool.get(reward.itemId, PurchaseRequest.MAX_QUANTITY);
      items.add(item);
      AdventureResult cost = ItemPool.get(reward.currency, reward.price);
      costs.put(reward.itemId, cost);
      rows.put(reward.itemId, reward.row);
    }

    data.getRows().clear();
    data.getRows().putAll(rows);
    data.getBuyItems().clear();
    data.getBuyItems().addAll(items);
    buyCosts.clear();
    buyCosts.putAll(costs);

    // Register the purchase requests, now that we know what is available
    data.registerPurchaseRequests();
    NamedListenerRegistry.fireChange("(coinmaster)");

    int itemId = CoinMasterRequest.extractItemId(data, location);

    if (itemId == -1) {
      // Purchase for Meat or a simple visit
      CoinMasterRequest.parseBalance(data, responseText);
      return;
    }

    CoinMasterRequest.parseResponse(data, location, responseText);
  }

  public static final boolean registerRequest(final String urlString, final boolean noMeat) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=armory")) {
      return false;
    }

    Matcher m = GenericRequest.WHICHROW_PATTERN.matcher(urlString);
    if (!m.find()) {
      // Just a visit
      return true;
    }

    int itemId = CoinMasterRequest.extractItemId(ARMORY_AND_LEGGERY, urlString);

    if (itemId == -1) {
      // Presumably this is a purchase for Meat.
      // If we've already checked Meat, this is an unknown item
      if (noMeat) {
        return false;
      }
      return NPCPurchaseRequest.registerShopRequest(urlString, true);
    }

    return CoinMasterRequest.registerRequest(ARMORY_AND_LEGGERY, urlString, true);
  }
}
