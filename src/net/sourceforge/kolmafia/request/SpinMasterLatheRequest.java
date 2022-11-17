package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SpinMasterLatheRequest extends CoinMasterRequest {
  public static final String master = "Your SpinMaster&trade; lathe";

  // Since there are four different currencies, we need to have a map from
  // itemId to item/count of currency; an AdventureResult.
  private static final Map<Integer, AdventureResult> buyCosts = new TreeMap<>();

  public static final CoinmasterData YOUR_SPINMASTER_LATHE =
      new CoinmasterData(master, "lathe", SpinMasterLatheRequest.class)
          .withShopRowFields(master, "lathe")
          .withBuyPrices()
          .withItemBuyPrice(SpinMasterLatheRequest::itemBuyPrice)
          .withNeedsPasswordHash(true);

  private static AdventureResult itemBuyPrice(final Integer itemId) {
    return buyCosts.get(itemId);
  }

  public static final AdventureResult SPINMASTER = ItemPool.get(ItemPool.SPINMASTER, 1);
  public static final AdventureResult FLIMSY_HARDWOOD_SCRAPS =
      ItemPool.get(ItemPool.FLIMSY_HARDWOOD_SCRAPS, 1);
  public static final AdventureResult DREADSYLVANIAN_HEMLOCK =
      ItemPool.get(ItemPool.DREADSYLVANIAN_HEMLOCK, 1);
  public static final AdventureResult SWEATY_BALSAM = ItemPool.get(ItemPool.SWEATY_BALSAM, 1);
  public static final AdventureResult ANCIENT_REDWOOD = ItemPool.get(ItemPool.ANCIENT_REDWOOD, 1);
  public static final AdventureResult PURPLEHEART_LOGS = ItemPool.get(ItemPool.PURPLEHEART_LOGS, 1);
  public static final AdventureResult WORMWOOD_STICK = ItemPool.get(ItemPool.WORMWOOD_STICK, 1);
  public static final AdventureResult DRIPWOOD_SLAB = ItemPool.get(ItemPool.DRIPWOOD_SLAB, 1);

  // Manually set up the map and change the currency, as need
  static {
    Map<Integer, Integer> map = CoinmastersDatabase.getBuyPrices(master);
    for (Entry<Integer, Integer> entry : CoinmastersDatabase.getBuyPrices(master).entrySet()) {
      int itemId = entry.getKey().intValue();
      int price = entry.getValue().intValue();
      AdventureResult cost =
          switch (itemId) {
            default -> FLIMSY_HARDWOOD_SCRAPS.getInstance(price);
            case ItemPool.HEMLOCK_HELM -> DREADSYLVANIAN_HEMLOCK.getInstance(price);
            case ItemPool.BALSAM_BARREL -> SWEATY_BALSAM.getInstance(price);
            case ItemPool.REDWOOD_RAIN_STICK -> ANCIENT_REDWOOD.getInstance(price);
            case ItemPool.PURPLEHEART_PANTS -> PURPLEHEART_LOGS.getInstance(price);
            case ItemPool.WORMWOOD_WEDDING_RING -> WORMWOOD_STICK.getInstance(price);
            case ItemPool.DRIPPY_DIADEM -> DRIPWOOD_SLAB.getInstance(price);
          };
      buyCosts.put(itemId, cost);
    }
  }

  public SpinMasterLatheRequest() {
    super(YOUR_SPINMASTER_LATHE);
  }

  public SpinMasterLatheRequest(final String action) {
    super(YOUR_SPINMASTER_LATHE, action);
  }

  public SpinMasterLatheRequest(final boolean buying, final AdventureResult[] attachments) {
    super(YOUR_SPINMASTER_LATHE, buying, attachments);
  }

  public SpinMasterLatheRequest(final boolean buying, final AdventureResult attachment) {
    super(YOUR_SPINMASTER_LATHE, buying, attachment);
  }

  public SpinMasterLatheRequest(final boolean buying, final int itemId, final int quantity) {
    super(YOUR_SPINMASTER_LATHE, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  // <tr rel="10587"><td valign=center></td><td><img
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/latheblowgun.gif"
  // class="hand pop" rel="desc_item.php?whichitem=835216330"
  // onClick='javascript:descitem(835216330)'></td><td valign=center><a
  // onClick='javascript:descitem(835216330)'><b>beechwood
  // blowgun</b>&nbsp;&nbsp;&nbsp;&nbsp;</a></td><td><img
  // src=https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/lathescraps.gif width=30
  // height=30 onClick='javascript:descitem(303504638)' alt="flimsy hardwood scraps" title="flimsy
  // hardwood
  // scraps"></td><td><b>1</b>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td valign=center><input class="button doit multibuy "  type=button rel='shop.php?whichshop=lathe&action=buyitem&quantity=1&whichrow=1163&pwd=34f70eafe497cf6ce0dbb1524c8cb0ea' value='Lathe'></td></tr>

  private static final Pattern ITEM_PATTERN =
      Pattern.compile(
          "<tr rel=\"(\\d+)\">.*?onClick='javascript:descitem\\((\\d+)\\)'>.*?<b>(.*?)</b>.*?title=\"(.*?)\".*?<b>([\\d,]+)</b>.*?whichrow=(\\d+)",
          Pattern.DOTALL);

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=lathe")) {
      return;
    }

    // Learn new items by simply visiting the SpinMaster Lathe
    // Refresh the Coin Master inventory every time we visit.

    CoinmasterData data = YOUR_SPINMASTER_LATHE;

    Matcher matcher = ITEM_PATTERN.matcher(responseText);
    boolean changed = false;
    while (matcher.find()) {
      int itemId = StringUtilities.parseInt(matcher.group(1));
      String descId = matcher.group(2);
      String itemName = matcher.group(3).trim();
      String currency = matcher.group(4);
      int price = StringUtilities.parseInt(matcher.group(5));
      int row = StringUtilities.parseInt(matcher.group(6));

      String match = ItemDatabase.getItemDataName(itemId);
      if (match == null || !match.equals(itemName)) {
        ItemDatabase.registerItem(itemId, itemName, descId);
      }

      Integer iitemId = itemId;
      if (!data.getRows().containsKey(iitemId)) {
        data.getRows().put(iitemId, row);
        AdventureResult item = ItemPool.get(itemId, PurchaseRequest.MAX_QUANTITY);
        data.getBuyItems().add(item);
        AdventureResult cost = ItemPool.get(currency, price);
        buyCosts.put(iitemId, cost);
        NPCPurchaseRequest.learnCoinmasterItem(
            master, ItemPool.get(itemId, 1), String.valueOf(price), String.valueOf(row));
        CoinmastersDatabase.registerPurchaseRequest(data, item, cost);
        changed = true;
      }
    }

    if (changed) {
      NamedListenerRegistry.fireChange("(coinmaster)");
    }

    Preferences.setBoolean("_spinmasterLatheVisited", true);

    int itemId = CoinMasterRequest.extractItemId(data, location);
    if (itemId == -1) {
      // Simple visit
      CoinMasterRequest.parseBalance(data, responseText);
      return;
    }

    CoinMasterRequest.parseResponse(data, location, responseText);
  }

  public static String accessible() {
    if (!InventoryManager.hasItem(SPINMASTER)) {
      return "You don't own a " + SPINMASTER.getName();
    }

    return null;
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=lathe")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(YOUR_SPINMASTER_LATHE, urlString, true);
  }
}
