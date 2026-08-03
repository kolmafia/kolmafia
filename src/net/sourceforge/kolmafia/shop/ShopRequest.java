package net.sourceforge.kolmafia.shop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.NPCPurchaseRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.request.concoction.shop.StillRequest;
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
      case "still" -> {
        StillRequest.parseResponse(urlString, responseText);
        break;
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

  static class SortByRow implements Comparator<ShopRow> {
    public int compare(ShopRow a, ShopRow b) {
      return a.getRow() - b.getRow();
    }
  }

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
    } else {
      // Otherwise, use the name we assigned to it.
      shopName = ShopDatabase.getShopName(shopId);
    }

    // Find all the ShopRow objects. Register any new items seen.
    List<ShopRow> shopRows = ShopRow.parseShop(responseText, true);

    // If this is a coinmaster, give it a chance to make its own
    // observations of the inventory
    CoinmasterData cd = ShopDatabase.getCoinmasterData(shopId);
    if (cd != null) {
      cd.visitShopRows(shopRows, force);
      cd.visitShop(responseText);
    }

    // KoL can add new items to existing coinmasters, npc stores, or
    // concoctions. We will log such items in the appropriate format.

    SHOP shopType = ShopDatabase.getShopType(shopId);
    boolean unknown = shopType == SHOP.NONE;
    boolean concoction = shopType == SHOP.CONC;

    boolean disabled = cd != null && cd.isDisabled();
    boolean newStyle = unknown || disabled || (cd != null && cd.getShopRows() != null);
    Set<AdventureResult> currencies = (cd == null) ? new HashSet<>() : cd.currencies();

    // All new rows will be logged in shoprows.txt format
    List<ShopRow> newShopRows = new ArrayList<>();

    // All new meat purchases will be logged in npcstores.txt format
    List<ShopRow> newMeatRows = new ArrayList<>();

    // All new item concoctions will be logged in concoctions.txt format
    List<ShopRow> newConcoctionRows = new ArrayList<>();

    // All new buy/sell items will be logged in old coinmaster format
    List<ShopRow> newBuyRows = new ArrayList<>();
    List<ShopRow> newSellRows = new ArrayList<>();

    // All new shoprow item coinmasters will be logged in new coinmaster format
    List<ShopRow> newCoinmasterRows = new ArrayList<>();

    for (ShopRow shopRow : shopRows) {
      int row = shopRow.getRow();

      // See if we know this row.
      ShopRow existing = disabled ? null : ShopRowDatabase.getShopRow(row);

      // If so, unless we are forcing all rows to be logged, skip it.
      if (existing != null && !force) {
        continue;
      }

      // All new rows get logged in shoprows.txt format
      newShopRows.add(shopRow);

      AdventureResult item = shopRow.getItem();
      AdventureResult[] costs = shopRow.getCosts();

      // There should be from 1-5 costs.  If KoL included none (KoL
      // bug), or parsing failed (KoLmafia bug), skip.
      if (costs.length == 0) {
        // *** Perhaps log the error?
        continue;
      }

      // Shops can sell skills. Assume all such are new-style coinmasters
      if (item != null && item.isSkill()) {
        newCoinmasterRows.add(shopRow);
        continue;
      }

      // If there is a single cost, the shop could be anything
      if (costs.length == 1) {
        AdventureResult cost = costs[0];

        if (cost.isMeat()) {
          // We'll handle this as an NPC store.
          newMeatRows.add(shopRow);
          continue;
        }

        // Buy/Sell coinmaster
        if (!disabled && cd != null && cd.getBuyItems() != null && currencies.contains(cost)) {
          // If the price is a currency, this is a "buy" request.
          newBuyRows.add(shopRow);
          continue;
        }

        if (!disabled && cd != null && cd.getSellItems() != null && currencies.contains(item)) {
          // If the item is a currency, this is a "sell" request.
          newSellRows.add(shopRow);
          continue;
        }
      }

      // Existing concoction shop
      if (concoction) {
        newConcoctionRows.add(shopRow);
        continue;
      }

      // Unknown shop or ShopRow coinmaster
      if (newStyle) {
        newCoinmasterRows.add(shopRow);
        continue;
      }
    }

    // We have now categorized all the rows.
    // If we detected nothing new (and are not forcing), we're done.
    if (newShopRows.size() == 0) {
      return shopRows;
    }

    String divider = "--------------------";
    RequestLogger.printLine(divider);
    RequestLogger.updateSessionLog(divider);

    Collections.sort(newShopRows, new SortByRow());
    for (ShopRow shopRow : newShopRows) {
      // Log in shoprows.txt format
      ShopRowData data =
          new ShopRowData(shopRow.getRow(), shopId, shopRow.getItem(), shopRow.getCosts());
      String printMe = data.dataString();
      RequestLogger.printLine(printMe);
      RequestLogger.updateSessionLog(printMe);
    }

    RequestLogger.printLine(divider);
    RequestLogger.updateSessionLog(divider);

    if (newMeatRows.size() > 0) {
      // Log newMeatRows in npcstores.txt format
      for (ShopRow shopRow : newMeatRows) {
        AdventureResult item = shopRow.getItem();
        int cost = shopRow.getCosts()[0].getCount();
        String printMe =
            shopName + "\t" + shopId + "\t" + item + "\t" + cost + "\tROW" + shopRow.getRow();
        RequestLogger.printLine(printMe);
        RequestLogger.updateSessionLog(printMe);
      }
      RequestLogger.printLine(divider);
      RequestLogger.updateSessionLog(divider);
      // An npcstore can also be a coinmaster - or even a concoction
    }

    if (newConcoctionRows.size() > 0) {
      // Log newConcoctionRows in concoctions.txt format
      var craftingType = ShopDatabase.getCraftingType(shopId);
      String type = craftingType != null ? craftingType.name() : "UNKNOWN";
      for (ShopRow shopRow : newConcoctionRows) {
        // star boomerang	STARCHART, ROW144	star chart	star (4)	line (5)
        StringBuilder buffer = new StringBuilder();
        buffer.append(shopRow.getItem());
        buffer.append("\t");
        buffer.append(type);
        buffer.append(", ROW");
        buffer.append(String.valueOf(shopRow.getRow()));
        for (AdventureResult cost : shopRow.getCosts()) {
          buffer.append("\t");
          buffer.append(cost);
        }
        String printMe = buffer.toString();
        RequestLogger.printLine(printMe);
        RequestLogger.updateSessionLog(printMe);
      }
      RequestLogger.printLine(divider);
      RequestLogger.updateSessionLog(divider);
      return shopRows;
    }

    if (newCoinmasterRows.size() > 0) {
      // Log newCoinmasterRows in coinmasters.txt ShopRow format
      for (ShopRow shopRow : newCoinmasterRows) {
        String printMe = shopRow.toData(shopName);
        RequestLogger.printLine(printMe);
        RequestLogger.updateSessionLog(printMe);
      }
      RequestLogger.printLine(divider);
      RequestLogger.updateSessionLog(divider);
      // Currently, a new style coinmaster cannot also have buy/sell rows
      return shopRows;
    }

    if (newBuyRows.size() > 0) {
      // Log newBuyRows in coinmasters.txt "buy" format
      for (ShopRow shopRow : newBuyRows) {
        AdventureResult item = shopRow.getItem();
        AdventureResult price = shopRow.getCosts()[0];
        String printMe =
            shopName + "\tbuy\t" + price.getCount() + "\t" + item + "\tROW" + shopRow.getRow();
        RequestLogger.printLine(printMe);
        RequestLogger.updateSessionLog(printMe);
      }
      RequestLogger.printLine(divider);
      RequestLogger.updateSessionLog(divider);
    }

    if (newSellRows.size() > 0) {
      // Log newSellRows in coinmasters.txt "sell" format
      for (ShopRow shopRow : newSellRows) {
        AdventureResult item = shopRow.getItem();
        AdventureResult price = shopRow.getCosts()[0];
        String printMe =
            shopName + "\tsell\t" + item.getCount() + "\t" + price + "\tROW" + shopRow.getRow();
        RequestLogger.printLine(printMe);
        RequestLogger.updateSessionLog(printMe);
      }
      RequestLogger.printLine(divider);
      RequestLogger.updateSessionLog(divider);
    }

    return shopRows;
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
