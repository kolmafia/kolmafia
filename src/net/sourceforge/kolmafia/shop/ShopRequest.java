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
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.request.concoction.shop.FiveDPrinterRequest;
import net.sourceforge.kolmafia.request.concoction.shop.JunkMagazineRequest;
import net.sourceforge.kolmafia.request.concoction.shop.StillRequest;
import net.sourceforge.kolmafia.request.concoction.shop.SugarSheetRequest;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.shop.ShopDatabase.SHOP;
import net.sourceforge.kolmafia.shop.ShopRowDatabase.ShopRowData;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ShopRequest extends GenericRequest {

  private final String shopId;
  private int row = 0;
  private int quantity = 0;

  public ShopRequest(final String shopId) {
    super("shop.php");
    this.shopId = shopId;
    this.addFormField("whichshop", shopId);
  }

  public ShopRequest(final String shopId, final String action) {
    this(shopId);
    this.addFormField("action", action);
  }

  public static ShopRequest getInstance(final String shopId, final int row, final int quantity) {
    ShopRowData data = ShopRowDatabase.getShopRowData(row);
    if (data == null || !data.shopId().equals(shopId)) {
      return null;
    }

    ShopRequest request = new ShopRequest(shopId, "buyitem");
    request.row = row;
    request.quantity = quantity;
    request.addFormField("whichRow", String.valueOf(data.row()));
    request.addFormField("quantity", String.valueOf(quantity));
    request.addFormField("ajax", "1");
    return request;
  }

  public String getShopId() {
    return this.shopId;
  }

  public int getRow() {
    return this.row;
  }

  public int getQuantity() {
    return this.quantity;
  }

  public void setQuantity(final int quantity) {
    this.quantity = quantity;
    this.addFormField("quantity", String.valueOf(quantity));
  }

  public static final Pattern WHICHSHOP_PATTERN = Pattern.compile("whichshop=([^&]*)");

  public static String parseShopId(final String urlString) {
    Matcher m = WHICHSHOP_PATTERN.matcher(urlString);
    return m.find() ? m.group(1) : null;
  }

  public static final Pattern WHICHROW_PATTERN = Pattern.compile("whichrow=(\\d+)");

  public static final int parseWhichRow(final String urlString) {
    Matcher m = WHICHROW_PATTERN.matcher(urlString);
    return m.find() ? StringUtilities.parseInt(m.group(1)) : 0;
  }

  public static final Pattern QUANTITY_PATTERN = Pattern.compile("quantity=(\\d+)");

  public static int parseQuantity(final String urlString) {
    // Absence of a quantity field - or 0 - means 1.
    Matcher m = QUANTITY_PATTERN.matcher(urlString);
    if (m.find()) {
      int quantity = StringUtilities.parseInt(m.group(1));
      return (quantity <= 0) ? 1 : quantity;
    }
    return 1;
  }

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), this.responseText);
  }

  // name=whichshop value="grandma"
  private static final Pattern SHOP_ID_PATTERN = Pattern.compile("name=whichshop value=\"(.*?)\"");

  public static String parseShopIdInResponse(final String html) {
    Matcher m = SHOP_ID_PATTERN.matcher(html);
    return m.find() ? m.group(1) : "";
  }

  // <b style="color: white">Crimbo Factory</b>
  private static final Pattern SHOP_PATTERN = Pattern.compile("<table.*?<b.*?>(.*?)</b>");

  public static String parseShopNameInResponse(final String html) {
    Matcher m = SHOP_PATTERN.matcher(html);
    return m.find() ? m.group(1) : "";
  }

  /*
   * ResponseTextParser support
   */

  public static final void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("shop.php")) {
      return;
    }

    String shopId = parseShopId(urlString);
    if (shopId == null) {
      return;
    }

    // Visiting a shop you currently can't access fails.
    // <b style="color: white">Uh Oh!</b>
    if (responseText.contains(">Uh Oh!</b>")) {
      return;
    }

    boolean ajax = urlString.contains("ajax=1");

    // An Ajax request shows purchase results without the inventory.
    if (!ajax) {
      // Parse the inventory and learn new items for "row" (modern) shops.
      // Print npcstores.txt or coinmasters.txt entries for new rows.
      // If it is a coinmaster, let it do extra parsing.
      parseShopInventory(shopId, responseText, false);
    }

    int row = parseWhichRow(urlString);
    ShopRow shopRow = ShopRowDatabase.getShopRow(row);

    SHOP shopType = ShopDatabase.getShopType(shopId);
    if (shopType == SHOP.CONC) {
      // Let concoctions do what they need even for visits
      handleConcoction(shopId, shopRow, urlString, responseText);
      return;
    }

    // If shopRow is null, we are just visiting.
    // Coinmasters have already parsed the inventory, in that case.
    // Give NPC shops the opportunity to examine the responseText.
    if (shopType == SHOP.NPC || (shopRow != null && shopRow.isMeatPurchase())) {
      // A shop.php store that sells items for Meat will say
      //     You spent XX Meat.
      // Result processing handles that, so we need not process it.

      // However, NPCPurchaseRequest may want to do additional actions
      // on a per-store basis.  Punt to them.
      NPCPurchaseRequest.parseShopResponse(shopId, shopRow, urlString, responseText);
      return;
    }

    CoinmasterData cd = ShopDatabase.getCoinmasterData(shopId);
    if (cd != null) {
      // A coinmaster may also be an NPC store, but we handled that
      // above. We know we are trading for items.
      CoinMasterRequest.parseResponse(cd, urlString, responseText);
      return;
    }
  }

  public static final void handleConcoction(
      final String shopId,
      final ShopRow shopRow,
      final String urlString,
      final String responseText) {

    if (responseText.contains("You don't have enough") || responseText.contains("Huh?")) {
      return;
    }

    // At least one shop allows you to "multi-make" but always makes one.
    int quantity = ShopRequest.parseQuantity(urlString);

    // Certain shops want to handle Preferences and Quests.  Give them a
    // chance to do so and finish removing ingredients when they return.
    switch (shopId) {
      case "starchart" -> {
        quantity = 1;
        break;
      }
      case "junkmagazine" -> {
        JunkMagazineRequest.parseResponse(urlString, responseText);
        break;
      }
      case "still" -> {
        StillRequest.parseResponse(urlString, responseText);
        break;
      }
      case "5dprinter" -> {
        FiveDPrinterRequest.parseResponse(urlString, responseText);
        break;
      }
      case "sugarsheets" -> {
        // Sugar Sheet folding always removes exactly one.
        SugarSheetRequest.parseResponse(urlString, responseText);
        return;
      }
    }

    if (shopRow == null) {
      // This was just a visit
      return;
    }

    // Remove the consumed ingredients.
    for (AdventureResult ingredient : shopRow.getCosts()) {
      ResultProcessor.processResult(ingredient.getInstance(-1 * ingredient.getCount() * quantity));
    }
  }

  /*
   * Shop Inventory parsing
   */

  public static final List<ShopRow> parseShopInventory(
      final String shopId, final String responseText, boolean force) {

    // Parse the entire shop inventory, including items that sell for Meat
    // This will register all previously unknown items.

    String shopName = parseShopNameInResponse(responseText);

    // Register this shop, in case it is unknown
    if (ShopDatabase.registerShop(shopId, shopName, SHOP.NONE)) {
      String printMe = "New shop: (" + shopId + ", \"" + shopName + "\")";
      RequestLogger.printLine(printMe);
      RequestLogger.updateSessionLog(printMe);
    }

    // If this is a coinmaster, give it a chance to make its own
    // observations of the inventory
    CoinmasterData cd = ShopDatabase.getCoinmasterData(shopId);
    if (cd != null) {
      cd.visitShop(responseText);
    }

    // Find all the ShopRow objects. Register any new items seen.
    List<ShopRow> shopRows = ShopRow.parseShop(responseText, true);

    // Certain existing shops are implemented as mixing methods.  Since
    // KoL could add new items to such shops, detect them.
    boolean concoction = ShopDatabase.getShopType(shopId) == SHOP.CONC;
    boolean coinmaster = (cd != null);

    boolean newShopItems = false;

    for (ShopRow shopRow : shopRows) {
      int row = shopRow.getRow();
      AdventureResult item = shopRow.getItem();
      AdventureResult[] costs = shopRow.getCosts();

      // There should be from 1-5 costs.  If KoL included none (KoL
      // bug), or parsing failed (KoLmafia bug), skip.
      if (costs.length == 0) {
        // *** Perhaps log the error?
        continue;
      }

      // Shops can sell skills
      if (item.isSkill()) {
        // If we know this row
        ShopRow existing = ShopRowDatabase.getShopRow(row);
        if (existing == null || force) {
          newShopItems |= learnSkill(shopId, shopName, item, costs, row, newShopItems, force);
        }
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

      // *** NPCStoreDatabase assumes that multiple stores can sell a particular item.
      if (NPCStoreDatabase.contains(id, false) && !force) {
        continue;
      }

      // *** CoinmastersDatabase assumes that multiple stores can sell a particular item.
      // The following does not account for "disabled" coinmasters - a testing feature.
      //    if (CoinmastersDatabase.contains(id, false) && !force) {
      if (CoinmastersDatabase.getAllPurchaseRequests(id).size() > 0 && !force) {
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

  public static final boolean learnSkill(
      final String shopId,
      final String shopName,
      final AdventureResult item,
      final AdventureResult[] costs,
      final int row,
      final boolean newShopItems,
      boolean force) {
    String printMe;
    if (!newShopItems) {
      printMe = "--------------------";
      RequestLogger.printLine(printMe);
      RequestLogger.updateSessionLog(printMe);
    }
    // Assume this will be a coinmaster
    ShopRow shopRow = new ShopRow(row, item, costs);
    printMe = shopRow.toData(shopName);
    RequestLogger.printLine(printMe);
    RequestLogger.updateSessionLog(printMe);
    return true;
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

  /*
   * RequestLogger support
   */

  public static final boolean buyStuff(final ShopRow shopRow, final int count) {
    StringBuilder buf = new StringBuilder();
    buf.append("Trade ");

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
    AdventureResult item = shopRow.getItem();
    if (item.isItem()) {
      buf.append(" for ");
      buf.append(count * item.getCount());
      buf.append(" ");
      buf.append(item.getPluralName(count));
    } else if (item.isSkill()) {
      buf.append(" to learn ");
      buf.append(item.getName());
    }

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(buf.toString());

    return true;
  }

  public static final boolean buyWithMeat(
      final String shopId, final ShopRow shopRow, final int count) {
    AdventureResult item = shopRow.getItem();
    AdventureResult[] costs = shopRow.getCosts();
    int price = NPCPurchaseRequest.currentDiscountedPrice(shopId, costs[0].getCount());

    StringBuilder buf = new StringBuilder();
    buf.append("buy ");
    buf.append(count);
    buf.append(" ");
    buf.append(item.getPluralName(count));
    buf.append(" for ");
    buf.append(price);
    buf.append(" each from ");
    buf.append(ShopDatabase.getShopName(shopId));

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(buf.toString());

    return true;
  }

  public static final boolean registerConcoction(
      final String shopId, final ShopRow shopRow, final int quantity) {
    int itemId = shopRow.getItem().getItemId();

    StringBuilder buffer = new StringBuilder();
    buffer.append("Use ");

    AdventureResult[] ingredients = shopRow.getCosts();
    for (int i = 0; i < ingredients.length; ++i) {
      if (i > 0) {
        buffer.append(", ");
      }

      int count = ingredients[i].getCount() * quantity;
      buffer.append(count);
      buffer.append(" ");
      buffer.append(ingredients[i].getPluralName(count));
    }

    buffer.append(" to make ");
    buffer.append(quantity);
    buffer.append(" ");
    buffer.append(shopRow.getItem().getPluralName(quantity));

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(buffer.toString());

    return true;
  }

  public static final boolean registerShop(
      final String shopId, final ShopRow shopRow, final int quantity) {
    var costs = shopRow.getCosts();
    if (costs != null && costs.length == 1 && costs[0].isMeat()) {
      return buyWithMeat(shopId, shopRow, quantity);
    }
    return buyStuff(shopRow, quantity);
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php")) {
      return false;
    }

    String shopId = parseShopId(urlString);
    if (shopId == null) {
      return false;
    }

    String action = GenericRequest.getAction(urlString);
    if (action == null || !action.equals("buyitem")) {
      // Just visiting the shop
      if (ShopDatabase.logVisits(shopId)) {
        String shopName = ShopDatabase.getShopName(shopId);
        RequestLogger.updateSessionLog();
        RequestLogger.updateSessionLog("Visiting " + shopName);
      }
      return true;
    }

    int row = parseWhichRow(urlString);
    ShopRow shopRow = ShopRowDatabase.getShopRow(row);
    if (shopRow == null) {
      return false;
    }

    int quantity = parseQuantity(urlString);
    SHOP shopType = ShopDatabase.getShopType(shopId);

    if (shopType == SHOP.CONC) {
      return registerConcoction(shopId, shopRow, quantity);
    }

    // log as an NPC Store or as a Coinmaster depending on costs.
    return registerShop(shopId, shopRow, quantity);
  }
}
