package net.sourceforge.kolmafia.shop;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.CoinmasterRegistry;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.NPCPurchaseRequest;
import net.sourceforge.kolmafia.shop.ShopDatabase.SHOP;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ShopRequest extends GenericRequest {
  public static final Pattern SHOPID_PATTERN = Pattern.compile("whichshop=([^&]*)");
  public static final Pattern WHICHROW_PATTERN = Pattern.compile("whichrow=(\\d+)");
  public static final Pattern QUANTITY_PATTERN = Pattern.compile("quantity=(\\d+)");

  private final String shopId;
  private ShopRow shopRow = null;
  private int quantity = 0;

  public ShopRequest(final String shopId) {
    super("shop.php");
    this.shopId = shopId;
    this.addFormField("whichshop", shopId);
  }

  public ShopRequest(final String shopId, final ShopRow row, final int quantity) {
    this(shopId);
    this.shopRow = shopRow;
    this.quantity = quantity;
    this.addFormField("action", "buyitem");
    this.addFormField("whichRow", String.valueOf(shopRow.getRow()));
    this.addFormField("quantity", String.valueOf(quantity));
    this.addFormField("ajax", "1");
  }

  public static String getShopId(final String urlString) {
    Matcher m = SHOPID_PATTERN.matcher(urlString);
    return m.find() ? m.group(1) : null;
  }

  public String getShopId() {
    return this.shopId;
  }

  public ShopRow getShopRow() {
    return this.shopRow;
  }

  public int getQuantity() {
    return this.quantity;
  }

  @Override
  public void run() {
    super.run();
  }

  @Override
  public void processResults() {
    String urlString = this.getURLString();
    ShopRequest.parseShopResponse(urlString, this.responseText);
  }

  // name=whichshop value="grandma"
  private static final Pattern SHOP_ID_PATTERN = Pattern.compile("name=whichshop value=\"(.*?)\"");

  public static String parseShopId(final String html) {
    Matcher m = SHOP_ID_PATTERN.matcher(html);
    return m.find() ? m.group(1) : "";
  }

  // <b style="color: white">Crimbo Factory</b>
  private static final Pattern SHOP_PATTERN = Pattern.compile("<table.*?<b.*?>(.*?)</b>");

  public static String parseShopName(final String html) {
    Matcher m = SHOP_PATTERN.matcher(html);
    return m.find() ? m.group(1) : "";
  }

  public static final int parseWhichRow(final String urlString) {
    Matcher matcher = WHICHROW_PATTERN.matcher(urlString);
    if (!matcher.find()) {
      return -1;
    }

    return StringUtilities.parseInt(matcher.group(1));
  }

  public static final int parseQuantity(final String urlString) {
    Matcher matcher = GenericRequest.QUANTITY_PATTERN.matcher(urlString);
    if (!matcher.find()) {
      return 1;
    }

    return StringUtilities.parseInt(matcher.group(1));
  }

  public static final void parseShopResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("shop.php")) {
      return;
    }

    String shopId = getShopId(urlString);
    if (shopId == null) {
      return;
    }

    // Parse the inventory and learn new items for "row" (modern) shops.
    // Print npcstores.txt or coinmasters.txt entries for new rows.

    parseShopInventory(shopId, responseText, false);

    parseShopRowResponse(urlString, responseText);
  }

  public static final List<ShopRow> parseShopInventory(
      final String shopId, final String responseText, boolean force) {

    // Parse the entire shop inventory, including items that sell for Meat
    // This will register all previously unknown items.

    String shopName = parseShopName(responseText);

    // Register this shop, in case it is unknown
    if (ShopDatabase.registerShop(shopId, shopName, SHOP.NONE)) {
      String printMe = "New shop: (" + shopId + ", \"" + shopName + "\")";
      RequestLogger.printLine(printMe);
      RequestLogger.updateSessionLog(printMe);
    }

    List<ShopRow> shopRows = ShopRow.parseShop(responseText, true);

    // Certain existing shops are implemented as mixing methods.  Since
    // KoL could add new items to such shops, detect them.
    boolean concoction = ShopDatabase.getShopType(shopId) == SHOP.CONC;

    boolean newShopItems = false;

    for (ShopRow shopRow : shopRows) {
      int row = shopRow.getRow();
      AdventureResult item = shopRow.getItem();
      AdventureResult[] costs = shopRow.getCosts();

      // There should be from 1-5 costs.  If KoL included none (KoL
      // bug), or parsing failed (KoLmafiq bug), skip.
      if (costs.length == 0) {
        // *** Perhaps log the error?
        continue;
      }

      // Shops can yield more than one of an item
      // Shops can yield the same item with multiple costs
      // Shops can accept up to five currencies per item.

      int id = item.getItemId();
      int count = item.getCount();

      // Current practice:
      //
      // A shop with a single currency which is Meat is an NPCStore
      // A shop with a single currency per item which is not Meat is a Coinmaster
      // A shop with multiple currencies per item is a Mixing method.

      // New practice:
      //
      // A shop with multiple currencies per item can be a Coinmaster

      // *** NPCStoreDatabase assumes that only a single store sells a particular item.
      if (NPCStoreDatabase.getPurchaseRequest(id) != null && !force) {
        continue;
      }

      // *** CoinmastersDatabase assumes that multiple stores can sell a particular item.
      if (CoinmastersDatabase.getAllPurchaseRequests(id) != null && !force) {
        continue;
      }

      // *** If an existing mixing method makes this item, skip it
      if (ConcoctionDatabase.hasNonCoinmasterMixingMethod(id) && !force) {
        continue;
      }

      // If this shop is implemented as a mixing method, we've detected
      // a new item for sale.
      if (concoction && !force) {
        continue;
      }

      if (costs.length == 1 && costs[0].isMeat()) {
        int cost = costs[0].getCount();
        newShopItems |= learnNPCStoreItem(shopId, shopName, item, cost, row, newShopItems, force);
        continue;
      }

      newShopItems |= learnCoinmasterItem(shopId, shopName, item, costs, row, newShopItems, force);
    }

    if (newShopItems) {
      String printMe = "--------------------";
      RequestLogger.printLine(printMe);
      RequestLogger.updateSessionLog(printMe);
    }

    return shopRows;
  }

  public static final boolean learnNPCStoreItem(
      final String shopId,
      final String shopName,
      final AdventureResult item,
      final int cost,
      final int row,
      final boolean newShopItems,
      boolean force) {
    String printMe;
    // Print what goes in npcstores.txt
    if (!newShopItems) {
      printMe = "--------------------";
      RequestLogger.printLine(printMe);
      RequestLogger.updateSessionLog(printMe);
    }
    printMe = shopName + "\t" + shopId + "\t" + item + "\t" + cost + "\tROW" + row;
    RequestLogger.printLine(printMe);
    RequestLogger.updateSessionLog(printMe);
    return true;
  }

  public static final boolean learnCoinmasterItem(
      final String shopId,
      String shopName,
      final AdventureResult item,
      final AdventureResult[] costs,
      final int row,
      final boolean newShopItems,
      boolean force) {

    // Sanity check: must be at least one cost
    if (costs.length == 0) {
      return false;
    }

    // See if this is a known Coinmaster
    CoinmasterData data = CoinmasterRegistry.findCoinmaster(shopId, shopName);
    String rowShop = CoinmastersDatabase.getRowShop(row);
    String type = "unknown";

    if (data != null && !data.isDisabled()) {
      // If we already know this row, nothing to learn.
      if ((data.getMaster().equals(rowShop) || data.hasRow(row)) && !force) {
        return false;
      }

      shopName = data.getMaster();

      if (costs.length == 1) {
        // we can categorize this as a buy or a sell
        AdventureResult price = costs[0];
        Set<AdventureResult> currencies = data.currencies();
        if (data.getBuyItems() != null && currencies.contains(price)) {
          // If the price is a currency, this is a "buy" request.
          type = "buy";
        } else if (data.getSellItems() != null && currencies.contains(item)) {
          // If the item is a currency, this is a "sell" request.
          type = "sell";
        } else {
          // Neither price nor item is a known currency.
          type = "unknown";
        }
      }
    }

    String printMe;
    // Print what goes in coinmasters.txt
    if (!newShopItems) {
      printMe = "--------------------";
      RequestLogger.printLine(printMe);
      RequestLogger.updateSessionLog(printMe);
    }
    switch (type) {
      case "buy" -> {
        AdventureResult price = costs[0];
        printMe = shopName + "\tbuy\t" + price.getCount() + "\t" + item + "\tROW" + row;
      }
      case "sell" -> {
        AdventureResult price = costs[0];
        printMe = shopName + "\tsell\t" + item.getCount() + "\t" + price + "\tROW" + row;
      }
      default -> {
        ShopRow shopRow = new ShopRow(row, item, costs);
        printMe = shopRow.toData(shopName);
      }
    }
    RequestLogger.printLine(printMe);
    RequestLogger.updateSessionLog(printMe);
    return true;
  }

  public static final void parseShopRowResponse(final String urlString, final String responseText) {
    // *** Do we want to put shop-specific stuff in here?
  }

  public static final void buyStuff(final ShopRow shopRow, final int count) {
    StringBuilder buf = new StringBuilder();
    buf.append("trading ");

    AdventureResult[] costs = shopRow.getCosts();
    for (int i = 0; i < costs.length; ++i) {
      AdventureResult cost = costs[i];
      if (i > 0) {
        buf.append(", ");
      }
      int price = cost.getCount() * count;
      if (cost.isMeat()) {
        price = NPCPurchaseRequest.currentDiscountedPrice(price);
      }
      buf.append(price);
      buf.append(" ");
      if (cost.isMeat()) {
        buf.append("Meat");
      } else {
        buf.append(cost.getPluralName(price));
      }
    }
    buf.append(" for ");
    AdventureResult item = shopRow.getItem();
    buf.append(count * item.getCount());
    buf.append(" ");
    buf.append(item.getPluralName(count));

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(buf.toString());
  }

  public static final boolean registerShopRequest(final String urlString, boolean meatOnly) {
    if (!urlString.startsWith("shop.php")) {
      return false;
    }

    String shopId = getShopId(urlString);
    if (shopId == null) {
      return false;
    }

    String action = GenericRequest.getAction(urlString);
    if (action == null || !action.equals("buyitems")) {
      // Just visiting the shop
      // String shopName = ShopDatabase.getShopName(shopId);
      return true;
    }

    // *** In order for this to work, ShopRowDatabase has to be in READ mode.
    int row = parseWhichRow(urlString);
    ShopRow shopRow = ShopRowDatabase.getShopRow(row);
    int count = parseQuantity(urlString);

    buyStuff(shopRow, count);

    return true;
  }
}
