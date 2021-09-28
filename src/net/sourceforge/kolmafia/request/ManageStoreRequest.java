package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ManageStoreRequest extends GenericRequest {
  private static final Pattern ITEMID_PATTERN = Pattern.compile("itemid=(h)?(\\d+)");
  private static final Pattern PRICE_PATTERN = Pattern.compile("price=(\\d+)?");
  private static final Pattern QUANTITY_PATTERN = Pattern.compile("quantity=(\\d+|\\*|)");
  private static final Pattern QTY_PATTERN = Pattern.compile("qty=(\\d+)");
  private static final Pattern LIMIT_PATTERN = Pattern.compile("limit=(\\d+)?");

  // (2) breath mints stocked for 999,999,999 meat each.
  private static final Pattern STOCKED_PATTERN =
      Pattern.compile(
          "\\(([\\d,]+)\\) (.*?) stocked for ([\\d,]+) meat each( \\(([\\d,]+)/day\\))?");

  private enum RequestType {
    ITEM_ADDITION,
    ITEM_REMOVAL,
    PRICE_UPDATE,
    REFRESH,
    VIEW_STORE_LOG,
  }

  private final RequestType requestType;

  // For action=removeitem
  private AdventureResult item;

  // For action=additem
  private AdventureResult[] items;
  private int[] prices, limits;
  private boolean storage;

  public ManageStoreRequest() {
    super("manageprices.php");
    this.requestType = RequestType.REFRESH;
  }

  public ManageStoreRequest(final boolean isStoreLog) {
    super("backoffice.php");
    this.addFormField("which", "3");
    this.requestType = RequestType.VIEW_STORE_LOG;
  }

  public ManageStoreRequest(final int itemId, int qty) {
    super("backoffice.php");
    this.addFormField("itemid", String.valueOf(itemId));
    this.addFormField("action", "removeitem");

    // Cannot ask for more to be removed than are really in the store
    qty = Math.min(qty, StoreManager.shopAmount(itemId));
    if (qty > 1) {
      AdventureResult item = ItemPool.get(itemId);
      KoLConstants.profitableList.remove(item);
    }

    this.addFormField("qty", String.valueOf(qty));
    this.addFormField("ajax", "1");

    this.requestType = RequestType.ITEM_REMOVAL;
    this.item = ItemPool.get(itemId, qty);
  }

  public ManageStoreRequest(final AdventureResult[] items, boolean storage) {
    this(items, null, null, storage);
  }

  public ManageStoreRequest(
      final AdventureResult[] items, final int[] prices, final int[] limits, boolean storage) {
    super("backoffice.php");
    this.addFormField("action", "additem");
    this.addFormField("ajax", "1");

    this.requestType = RequestType.ITEM_ADDITION;
    this.items = items;
    this.prices = prices;
    this.limits = limits;
    this.storage = storage;
  }

  public ManageStoreRequest(final int[] itemIds, final int[] prices, final int[] limits) {
    super("backoffice.php");
    this.addFormField("action", "updateinv");
    this.addFormField("ajax", "1");

    this.requestType = RequestType.PRICE_UPDATE;
    for (int i = 0; i < itemIds.length; ++i) {
      int price = prices[i];
      if (price == 0) {
        continue;
      }

      int itemId = itemIds[i];
      int limit = limits[i];
      if (price == StoreManager.getPrice(itemId) && limit == StoreManager.getLimit(itemId)) {
        continue;
      }

      int autosell = ItemDatabase.getPriceById(itemId);
      int actualPrice = Math.max(price, Math.max(autosell, 100));
      this.addFormField("price[" + itemId + "]", String.valueOf(actualPrice));
      if (limit != 0) {
        this.addFormField("limit[" + itemId + "]", String.valueOf(limit));
      }
    }
  }

  @Override
  protected boolean retryOnTimeout() {
    return true;
  }

  @Override
  public void run() {
    switch (this.requestType) {
      case ITEM_ADDITION:
        this.addItems();
        break;

      case ITEM_REMOVAL:
        this.removeItem();
        break;

      case PRICE_UPDATE:
        this.priceUpdate();
        break;

      case REFRESH:
        this.managePrices();
        break;

      case VIEW_STORE_LOG:
        this.viewStoreLogs();
        break;
    }
  }

  private void addItems() {
    for (int i = 0; KoLmafia.permitsContinue() && i < this.items.length; ++i) {
      // backoffice.php?itemid=h362&price=180&quantity=1&limit=&pwd&action=additem&ajax=1
      AdventureResult item = this.items[i];
      String name = item.getName();

      this.addFormField("itemid", (this.storage ? "h" : "") + item.getItemId());
      this.addFormField("price", (this.prices == null ? "" : "" + this.prices[i]));
      this.addFormField("quantity", String.valueOf(item.getCount()));
      this.addFormField("limit", (this.limits == null ? "" : "" + this.limits[i]));

      KoLmafia.updateDisplay("Adding " + name + " to store...");
      super.run();
    }
  }

  private void removeItem() {
    String name = this.item.getName();

    KoLmafia.updateDisplay("Removing " + name + " from store...");
    super.run();
    KoLmafia.updateDisplay(this.item.getCount() + " " + name + " removed from your store.");
  }

  private void managePrices() {
    KoLmafia.updateDisplay("Requesting store inventory...");

    super.run();

    if (this.responseText != null) {
      StoreManager.update(this.responseText, StoreManager.PRICER);
    }

    KoLmafia.updateDisplay("Store inventory request complete.");
  }

  private void viewStoreLogs() {
    KoLmafia.updateDisplay("Examining store logs...");

    super.run();

    if (this.responseText != null) {
      StoreManager.parseLog(this.responseText);
    }

    KoLmafia.updateDisplay("Store purchase logs retrieved.");
  }

  private void priceUpdate() {
    KoLmafia.updateDisplay("Updating store prices...");

    super.run();

    KoLmafia.updateDisplay("Store prices updated.");
  }

  @Override
  public void processResults() {
    ManageStoreRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static final void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("backoffice.php")) {
      return;
    }

    if (urlString.contains("which=3")) {
      // Store Log retrieval
      return;
    }

    String action = GenericRequest.getAction(urlString);
    if (action == null) {
      StoreManager.update(responseText, StoreManager.DEETS);
      StoreManager.calculatePotentialEarnings();
      return;
    }

    if (action.equals("additem")) {
      if (responseText.contains("Are you sure you want to sell this item for that little Meat?")) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "KOL's low price protection stopped you selling.");
        return;
      }

      // (2) breath mints stocked for 999,999,999 meat each.
      Matcher stockedMatcher = ManageStoreRequest.STOCKED_PATTERN.matcher(responseText);
      if (!stockedMatcher.find()) {
        return;
      }

      int quantity = StringUtilities.parseInt(stockedMatcher.group(1));
      int price = StringUtilities.parseInt(stockedMatcher.group(3));
      int limit =
          stockedMatcher.group(4) == null ? 0 : StringUtilities.parseInt(stockedMatcher.group(5));

      // backoffice.php?itemid=h362&price=180&quantity=1&limit=&pwd&action=additem&ajax=1
      // backoffice.php?itemid=362&price=180&quantity=1&limit=&pwd&action=additem&ajax=1

      // get the item ID - and whether it is from Hagnk's - from the URL submitted.
      // ignore price, quantity, and limit, since the response told us those

      Matcher itemMatcher = ManageStoreRequest.ITEMID_PATTERN.matcher(urlString);
      if (!itemMatcher.find()) {
        return;
      }

      boolean storage = itemMatcher.group(1) != null;
      int itemId = StringUtilities.parseInt(itemMatcher.group(2));

      AdventureResult item = ItemPool.get(itemId, -quantity);
      if (storage) {
        AdventureResult.addResultToList(KoLConstants.storage, item);
      } else {
        ResultProcessor.processItem(itemId, -quantity);
      }

      StoreManager.addItem(itemId, quantity, price, limit);

      KoLmafia.updateDisplay(quantity + " " + item.getName() + " added to your store.");

      return;
    }

    if (action.equals("removeitem")) {
      // backoffice.php?qty=1&pwd&action=removeitem&itemid=362&ajax=1

      AdventureResult item = MallPurchaseRequest.processItemFromMall(responseText);
      if (item != null) {
        StoreManager.removeItem(item.getItemId(), item.getCount());
      }

      return;
    }

    if (action.equals("updateinv")) {
      // backoffice.php?action=updateinv&price[682]=230&price[684]=230&price[797]=230&price[679]=230&price[680]=230&price[681]=230
      //
      // With ajax:
      // <table><tr><td>a little sump'm sump'm updated  (price: 231, limit: 0)<!--
      // U:{"116230030":{"price":231,"lim":0}} --></td></tr></table>
      //
      // That is nice and succinct and tells you only the items which changed price
      //
      // Without ajax, we can also get the quantity

      if (!urlString.contains("ajax=1")) {
        StoreManager.update(responseText, StoreManager.DEETS);
      } else {
        StoreManager.updateSomePrices(responseText);
      }

      StoreManager.calculatePotentialEarnings();

      return;
    }
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("backoffice.php")) {
      return false;
    }

    String action = GenericRequest.getAction(urlString);
    if (action == null) {
      return false;
    }

    if (action.equals("additem")) {
      // backoffice.php?itemid=h362&price=180&quantity=1&limit=&pwd&action=additem&ajax=1
      // backoffice.php?itemid=362&price=180&quantity=1&limit=&pwd&action=additem&ajax=1

      // get the item ID - and whether it is from Hagnk's - from the URL submitted.
      Matcher itemMatcher = ManageStoreRequest.ITEMID_PATTERN.matcher(urlString);
      if (!itemMatcher.find()) {
        return false;
      }
      boolean storage = itemMatcher.group(1) != null;
      int itemId = StringUtilities.parseInt(itemMatcher.group(2));

      Matcher quantityMatcher = ManageStoreRequest.QUANTITY_PATTERN.matcher(urlString);
      if (!quantityMatcher.find()) {
        return false;
      }
      String quantityString = quantityMatcher.group(1);
      String quantity =
          quantityString.equals("") ? "1" : quantityString.equals("*") ? "all" : quantityString;

      Matcher priceMatcher = ManageStoreRequest.PRICE_PATTERN.matcher(urlString);
      if (!priceMatcher.find()) {
        return false;
      }
      int price =
          priceMatcher.group(1) == null
              ? 999999999
              : StringUtilities.parseInt(priceMatcher.group(1));

      Matcher limitMatcher = ManageStoreRequest.LIMIT_PATTERN.matcher(urlString);
      if (!limitMatcher.find()) {
        return false;
      }
      int limit =
          limitMatcher.group(1) == null ? 0 : StringUtilities.parseInt(limitMatcher.group(1));

      StringBuilder buffer = new StringBuilder();

      buffer.append("Adding ");
      buffer.append(quantity);
      buffer.append(" ");
      buffer.append(ItemDatabase.getItemName(itemId));
      buffer.append(" to store from ");
      buffer.append(storage ? "storage" : "inventory");
      buffer.append(" for ");
      buffer.append(KoLConstants.COMMA_FORMAT.format(price));
      buffer.append(" Meat");
      if (limit > 0) {
        buffer.append(", limited to ");
        buffer.append(limit);
        buffer.append("/day");
      } else {
        buffer.append(" with no limit");
      }

      RequestLogger.updateSessionLog(buffer.toString());

      return true;
    }

    if (action.equals("removeitem")) {
      // backoffice.php?qty=1&pwd&action=removeitem&itemid=362&ajax=1

      Matcher itemMatcher = ManageStoreRequest.ITEMID_PATTERN.matcher(urlString);
      if (!itemMatcher.find()) {
        return false;
      }
      int itemId = StringUtilities.parseInt(itemMatcher.group(2));

      Matcher qtyMatcher = ManageStoreRequest.QTY_PATTERN.matcher(urlString);
      if (!qtyMatcher.find()) {
        return false;
      }
      int qty = StringUtilities.parseInt(qtyMatcher.group(1));

      String buffer = "Removing " + qty + " " + ItemDatabase.getItemName(itemId) + " from store";
      RequestLogger.updateSessionLog(buffer);

      return true;
    }

    return false;
  }
}
