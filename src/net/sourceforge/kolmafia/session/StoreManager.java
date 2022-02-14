package net.sourceforge.kolmafia.session;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.request.AutoMallRequest;
import net.sourceforge.kolmafia.request.AutoSellRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.ManageStoreRequest;
import net.sourceforge.kolmafia.swingui.StoreManageFrame;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class StoreManager {
  private static final Pattern LOGSPAN_PATTERN = Pattern.compile("<span class=small>.*?</span>");

  private static final Pattern ADDER_PATTERN =
      Pattern.compile(
          "<tr><td><img src.*?></td><td>(.*?)( *\\((\\d*)\\))?</td><td>([\\d,]+)</td><td>(.*?)</td><td.*?(\\d+)");

  private static final Pattern PRICER_PATTERN =
      Pattern.compile(
          "<tr><td><b>(.*?)&nbsp;.*?<td>([\\d,]+)</td>.*?\"(\\d+)\" name=price\\d+\\[(\\d+).*?value=\"(\\d+)\".*?<td>([\\d,]+)</td>");

  // <tr class="deets" rel="618679857" after="6"><td valign="center"><img
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/cocostraw.gif"></td><td
  // valign="center"><b>slip 'n' slide</b></td><td valign="center" align="center">1,081</td
  // valign="center"><td align="center"><span class="tohide">230</span><input type="text"
  // class="hideit price" rel="230" style="width:80px" name="price[681]" value="230" /></td><td
  // valign="center" align="center"><span class="tohide">&infin;</span><input type="text"
  // class="hideit lim" style="width:24px" name="limit[681]" value="0" /><input type="submit"
  // value="Save" class="button hideit pricejax" style="font-size: 8pt"/></td><td align="right"
  // valign="center">[<a href="#" class="update">update</a>][<a
  // href="/backoffice.php?pwd=90ef7aca1d45123f7abe567b758c5b89&iid=681&action=prices"
  // class="prices">prices</a>]<span class="tohide">[<a class="take"
  // href="backoffice.php?qty=1&pwd=90ef7aca1d45123f7abe567b758c5b89&action=removeitem&itemid=681">take&nbsp;1</a>][<a class="take" href="backoffice.php?qty=1081&pwd=90ef7aca1d45123f7abe567b758c5b89&action=removeitem&itemid=681">take&nbsp;&infin;</a>]</span><span class="hideit" style="font-size: .9em">  <span class="setp">min&nbsp;price:&nbsp;230</span><br /><span class="setp">cheapest: 230</span></span></td></tr>

  private static final Pattern INVENTORY_ROW_PATTERN =
      Pattern.compile("<tr class=\"deets\".*?</tr>");
  private static final Pattern INVENTORY_PATTERN =
      Pattern.compile(
          ".*?>([\\d,]+<).*name=\"price\\[(.*?)\\]\" value=\"(.*?)\".*name=\"limit\\[.*?\\]\" value=\"(.*?)\"");

  // Different formats of inventory table

  public static final int ADDER = 1;
  public static final int PRICER = 2;
  public static final int DEETS = 3;

  private static final int RECENT_FIRST = 1;
  private static final int OLDEST_FIRST = 2;
  private static final int GROUP_BY_NAME = 3;

  private static int currentLogSort = StoreManager.RECENT_FIRST;
  private static boolean sortItemsByName = false;

  private static final long REALISTIC_PRICE_THRESHOLD = 50000000;
  private static long potentialEarnings = 0;

  private static final LockableListModel<StoreLogEntry> storeLog =
      new LockableListModel<StoreLogEntry>();
  private static final LockableListModel<SoldItem> soldItemList = new LockableListModel<SoldItem>();
  private static final LockableListModel<SoldItem> sortedSoldItemList =
      new LockableListModel<SoldItem>();

  public static boolean soldItemsRetrieved = false;

  public static final void clearCache() {
    StoreManager.soldItemsRetrieved = false;
    StoreManager.storeLog.clear();
    StoreManageFrame.cancelTableEditing();
    StoreManager.soldItemList.clear();
    StoreManager.sortedSoldItemList.clear();
    StoreManager.potentialEarnings = 0;
  }

  public static long getPotentialEarnings() {
    return StoreManager.potentialEarnings;
  }

  public static void calculatePotentialEarnings() {
    long earnings = 0;
    for (SoldItem item : StoreManager.soldItemList) {
      int price = item.getPrice();
      if (price < REALISTIC_PRICE_THRESHOLD) {
        earnings += (long) item.getQuantity() * (long) price;
      }
    }
    StoreManager.potentialEarnings = earnings;
    StoreManageFrame.updateEarnings(StoreManager.potentialEarnings);
  }

  /**
   * Registers an item inside of the store manager. Note that this includes the price of the item
   * and the limit which is used to sell the item.
   */
  public static final SoldItem registerItem(
      final int itemId, final int quantity, final int price, final int limit, final int lowest) {
    if (price < REALISTIC_PRICE_THRESHOLD) {
      StoreManager.potentialEarnings += (long) price * (long) quantity;
    }

    SoldItem newItem = new SoldItem(itemId, quantity, price, limit, lowest);
    int itemIndex = StoreManager.soldItemList.indexOf(newItem);

    // If the item is brand-new, just return it

    if (itemIndex == -1) {
      return newItem;
    }

    // If the item already exists, check it against the one which
    // already exists in the list.	If there are any changes,
    // update.

    SoldItem oldItem = StoreManager.soldItemList.get(itemIndex);

    if (oldItem.getQuantity() != quantity
        || oldItem.getPrice() != price
        || oldItem.getLimit() != limit
        || lowest != 0 && oldItem.getLowest() != lowest) {
      return newItem;
    }

    return oldItem;
  }

  /**
   * Returns the current price of the item with the given item Id. This is useful for auto-adding at
   * the existing price.
   */
  public static final int getPrice(final int itemId) {
    int currentPrice = 999999999;
    for (int i = 0; i < StoreManager.soldItemList.size(); ++i) {
      if (StoreManager.soldItemList.get(i).getItemId() == itemId) {
        currentPrice = StoreManager.soldItemList.get(i).getPrice();
        break;
      }
    }

    return currentPrice;
  }

  public static final int getLimit(final int itemId) {
    int currentLimit = 0;
    for (int i = 0; i < StoreManager.soldItemList.size(); ++i) {
      if (StoreManager.soldItemList.get(i).getItemId() == itemId) {
        currentLimit = StoreManager.soldItemList.get(i).getLimit();
        break;
      }
    }

    return currentLimit;
  }

  public static final LockableListModel<SoldItem> getSoldItemList() {
    return StoreManager.soldItemList;
  }

  public static final LockableListModel<SoldItem> getSortedSoldItemList() {
    return StoreManager.sortedSoldItemList;
  }

  public static final LockableListModel<StoreLogEntry> getStoreLog() {
    return StoreManager.storeLog;
  }

  public static final void sortStoreLog(final boolean cycleSortType) {
    if (cycleSortType) {
      switch (StoreManager.currentLogSort) {
        case RECENT_FIRST:
          StoreManager.currentLogSort = StoreManager.OLDEST_FIRST;
          break;
        case OLDEST_FIRST:
          StoreManager.currentLogSort = StoreManager.GROUP_BY_NAME;
          break;
        case GROUP_BY_NAME:
          StoreManager.currentLogSort = StoreManager.RECENT_FIRST;
          break;
      }
    }

    // Because StoreLogEntry objects use the current
    // internal variable to decide how to sort, a simple
    // function call will suffice.

    StoreManager.storeLog.sort();
  }

  public static final void update(String storeText, final int type) {
    // Strip introductory "header" from the string so that we can simplify the matcher.
    String headerEnd = "in Mall:</b></td></tr>";
    int index = storeText.indexOf(headerEnd);
    if (index != -1) {
      storeText = storeText.substring(index + headerEnd.length());
    }

    StoreManager.potentialEarnings = 0;
    ArrayList<SoldItem> newItems = new ArrayList<SoldItem>();

    switch (type) {
      case ADDER:
        {
          AdventureResult item;
          int itemId, price, limit;

          // The item matcher here examines each row in the table
          // displayed in the standard item-addition page.

          Matcher itemMatcher = StoreManager.ADDER_PATTERN.matcher(storeText);

          while (itemMatcher.find()) {
            itemId = StringUtilities.parseInt(itemMatcher.group(6));
            if (ItemDatabase.getItemName(itemId) == null) {
              // Do not register new items discovered in your store,
              // since the descid is not available
              //
              // ItemDatabase.registerItem( itemId, itemMatcher.group( 1 ), descId );
              continue;
            }

            int count =
                itemMatcher.group(2) == null ? 1 : StringUtilities.parseInt(itemMatcher.group(3));

            // Register using item ID, since the name might have changed
            item = ItemPool.get(itemId, count);
            price = StringUtilities.parseInt(itemMatcher.group(4));

            // In this case, the limit could appear as
            // "unlimited", which equates to a limit of 0.

            limit =
                itemMatcher.group(5).startsWith("<")
                    ? 0
                    : StringUtilities.parseInt(itemMatcher.group(5));

            // Now that all the data has been retrieved,
            // register the item that was discovered.

            newItems.add(
                StoreManager.registerItem(item.getItemId(), item.getCount(), price, limit, 0));
          }
          break;
        }
      case PRICER:
        {
          int itemId, quantity, price, limit, lowest;

          // The item matcher here examines each row in the table
          // displayed in the price management page.

          Matcher priceMatcher = StoreManager.PRICER_PATTERN.matcher(storeText);

          while (priceMatcher.find()) {
            itemId = StringUtilities.parseInt(priceMatcher.group(4));
            if (ItemDatabase.getItemName(itemId) == null) {
              // Do not register new items discovered in your store,
              // since the descid is not available
              //
              // ItemDatabase.registerItem( itemId, priceMatcher.group( 1 ), descId );
              continue;
            }

            quantity = StringUtilities.parseInt(priceMatcher.group(2));

            price = StringUtilities.parseInt(priceMatcher.group(3));
            limit = StringUtilities.parseInt(priceMatcher.group(5));
            lowest = StringUtilities.parseInt(priceMatcher.group(6));

            // Now that all the data has been retrieved, register
            // the item that was discovered.

            newItems.add(StoreManager.registerItem(itemId, quantity, price, limit, lowest));
          }
          break;
        }
      case DEETS:
        {
          Matcher rowMatcher = StoreManager.INVENTORY_ROW_PATTERN.matcher(storeText);
          while (rowMatcher.find()) {
            Matcher matcher = StoreManager.INVENTORY_PATTERN.matcher(rowMatcher.group(0));
            if (!matcher.find()) {
              continue;
            }

            int itemId = StringUtilities.parseInt(matcher.group(2));
            int count = StringUtilities.parseInt(matcher.group(1));
            int price = StringUtilities.parseInt(matcher.group(3));
            int limit = StringUtilities.parseInt(matcher.group(4));

            newItems.add(StoreManager.registerItem(itemId, count, price, limit, 0));
          }
          break;
        }
    }

    StoreManageFrame.cancelTableEditing();

    StoreManager.sortItemsByName = true;
    Collections.sort(newItems);
    StoreManager.soldItemList.clear();
    StoreManager.soldItemList.addAll(newItems);

    StoreManager.sortItemsByName = false;
    Collections.sort(newItems);
    StoreManager.sortedSoldItemList.clear();
    StoreManager.sortedSoldItemList.addAll(newItems);

    StoreManager.soldItemsRetrieved = true;

    // Now, update the title of the store manage
    // frame to reflect the new price.

    StoreManageFrame.updateEarnings(StoreManager.potentialEarnings);
  }

  public static final void parseLog(final String logText) {
    StoreManager.storeLog.clear();
    Matcher logMatcher = StoreManager.LOGSPAN_PATTERN.matcher(logText);
    if (logMatcher.find()) {
      if (!logMatcher.group().contains("<br>")) {
        return;
      }

      ArrayList<StoreLogEntry> currentLog = new ArrayList<StoreLogEntry>();

      String[] entries = logMatcher.group().split("<br>");

      for (int i = 0; i < entries.length - 1; ++i) {
        String entryString = KoLConstants.ANYTAG_PATTERN.matcher(entries[i]).replaceAll("");
        StoreLogEntry entry = new StoreLogEntry(entries.length - i - 1, entryString);
        currentLog.add(entry);
      }

      StoreManager.storeLog.addAll(currentLog);
      StoreManager.sortStoreLog(false);
    }
  }

  public static class StoreLogEntry implements Comparable<StoreLogEntry> {
    private final int id;
    private final String text;
    private final String stringForm;

    public StoreLogEntry(final int id, final String text) {
      this.id = id;

      String[] pieces = text.split(" ");
      this.text = text.substring(pieces[0].length() + pieces[1].length() + 2);
      this.stringForm = id + ": " + text;
    }

    @Override
    public String toString() {
      return this.stringForm;
    }

    @Override
    public int compareTo(final StoreLogEntry o) {
      if (o == null) {
        return -1;
      }

      switch (StoreManager.currentLogSort) {
        case RECENT_FIRST:
          return o.id - this.id;
        case OLDEST_FIRST:
          return this.id - o.id;
        case GROUP_BY_NAME:
          return this.text.compareToIgnoreCase(o.text);
        default:
          return -1;
      }
    }
  }

  /**
   * Internal immutable class used to hold a single instance of an item sold in a player's store.
   */
  public static class SoldItem extends Vector<Serializable> implements Comparable<Object> {
    private final int itemId;
    private final String itemName;
    private final int quantity;
    private final int price;
    private final int limit;
    private final int lowest;

    public SoldItem(
        final int itemId, final int quantity, final int price, final int limit, final int lowest) {
      this.itemId = itemId;
      this.itemName = ItemDatabase.getItemDataName(itemId);
      this.quantity = quantity;
      this.price = price;
      this.limit = limit;
      this.lowest = lowest;

      super.add(this.itemName);
      super.add(IntegerPool.get(price));
      super.add(IntegerPool.get(lowest));
      super.add(IntegerPool.get(quantity));
      super.add(IntegerPool.get(limit));
    }

    public int getItemId() {
      return this.itemId;
    }

    public String getItemName() {
      return this.itemName;
    }

    public int getQuantity() {
      return this.quantity;
    }

    public int getPrice() {
      return this.price;
    }

    public int getLimit() {
      return this.limit;
    }

    public int getLowest() {
      return this.lowest;
    }

    @Override
    public synchronized boolean equals(final Object o) {
      return o instanceof SoldItem && ((SoldItem) o).itemId == this.itemId;
    }

    @Override
    public int hashCode() {
      return this.itemId;
    }

    @Override
    public int compareTo(final Object o) {
      if (!(o instanceof SoldItem)) {
        return -1;
      }

      if (this.price != 999999999 && ((SoldItem) o).price == 999999999) {
        return -1;
      }

      if (this.price == 999999999 && ((SoldItem) o).price != 999999999) {
        return 1;
      }

      if (this.price == 999999999 && ((SoldItem) o).price == 999999999) {
        return this.itemName.compareToIgnoreCase(((SoldItem) o).itemName);
      }

      return StoreManager.sortItemsByName
          ? this.itemName.compareToIgnoreCase(((SoldItem) o).itemName)
          : this.price - ((SoldItem) o).price;
    }

    @Override
    public synchronized String toString() {
      StringBuilder buffer = new StringBuilder();

      buffer.append(ItemDatabase.getItemName(this.itemId));
      buffer.append(" (");

      buffer.append(KoLConstants.COMMA_FORMAT.format(this.quantity));

      if (this.limit < this.quantity) {
        buffer.append(" limit ");
        buffer.append(KoLConstants.COMMA_FORMAT.format(this.limit));
      }

      buffer.append(" @ ");
      buffer.append(KoLConstants.COMMA_FORMAT.format(this.price));
      buffer.append(")");

      return buffer.toString();
    }
  }

  public static int shopAmount(int itemId) {
    SoldItem item = new SoldItem(itemId, 0, 0, 0, 0);

    int index = StoreManager.soldItemList.indexOf(item);
    if (index == -1) {
      // The item isn't in your store
      return 0;
    }

    return StoreManager.soldItemList.get(index).getQuantity();
  }

  public static void priceItemsAtLowestPrice(boolean avoidMinPrice) {
    if (GenericRequest.abortIfInFightOrChoice()) {
      return;
    }

    RequestThread.postRequest(new ManageStoreRequest());

    SoldItem[] sold = new SoldItem[StoreManager.soldItemList.size()];
    StoreManager.soldItemList.toArray(sold);

    int[] itemId = new int[sold.length];
    int[] prices = new int[sold.length];
    int[] limits = new int[sold.length];

    // Now determine the desired prices on items.

    for (int i = 0; i < sold.length; ++i) {
      itemId[i] = sold[i].getItemId();
      limits[i] = sold[i].getLimit();

      int minimumPrice =
          Math.max(100, Math.abs(ItemDatabase.getPriceById(sold[i].getItemId())) * 2);
      int desiredPrice = Math.max(minimumPrice, sold[i].getLowest() - sold[i].getLowest() % 100);

      if (sold[i].getPrice() == 999999999 && (!avoidMinPrice || desiredPrice > minimumPrice)) {
        prices[i] = desiredPrice;
      } else {
        prices[i] = sold[i].getPrice();
      }
    }

    RequestThread.postRequest(new ManageStoreRequest(itemId, prices, limits));
    KoLmafia.updateDisplay("Repricing complete.");
  }

  public static void endOfRunSale(boolean avoidMinPrice) {
    if (!KoLCharacter.canInteract()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You are not yet out of ronin.");
      return;
    }

    if (!InputFieldUtilities.confirm("Are you sure you'd like to host an end-of-run sale?")) {
      return;
    }

    if (GenericRequest.abortIfInFightOrChoice()) {
      return;
    }

    // Only place items in the mall which are not
    // sold in NPC stores and can be autosold.

    AdventureResult[] items = new AdventureResult[KoLConstants.inventory.size()];
    KoLConstants.inventory.toArray(items);

    List<AdventureResult> autosell = new ArrayList<>();
    List<AdventureResult> automall = new ArrayList<>();

    for (int i = 0; i < items.length; ++i) {
      int itemId = items[i].getItemId();
      if (itemId == ItemPool.MEAT_PASTE
          || itemId == ItemPool.MEAT_STACK
          || itemId == ItemPool.DENSE_STACK) {
        continue;
      }

      if (!ItemDatabase.isTradeable(itemId)) {
        continue;
      }

      if (ItemDatabase.getPriceById(itemId) <= 0) {
        continue;
      }

      if (NPCStoreDatabase.contains(itemId, false)) {
        autosell.add(items[i]);
      } else {
        automall.add(items[i]);
      }
    }

    // Now, place all the items in the mall at the
    // maximum possible price. This allows KoLmafia
    // to determine the minimum price.

    if (autosell.size() > 0 && KoLmafia.permitsContinue()) {
      RequestThread.postRequest(new AutoSellRequest(autosell.toArray(new AdventureResult[0])));
    }

    if (automall.size() > 0 && KoLmafia.permitsContinue()) {
      RequestThread.postRequest(new AutoMallRequest(automall.toArray(new AdventureResult[0])));
    }

    // Now, remove all the items that you intended
    // to remove from the store due to pricing issues.

    if (KoLmafia.permitsContinue()) {
      priceItemsAtLowestPrice(avoidMinPrice);
    }

    KoLmafia.updateDisplay("Undercutting sale complete.");
  }

  public static void addItems(AdventureResult[] items, int[] prices, int[] limits) {
    for (int i = 0; i < items.length; ++i) {
      StoreManager.addItem(items[i], prices[i], limits[i]);
    }

    StoreManager.sortItemsByName = true;
    Collections.sort(StoreManager.soldItemList);
    StoreManager.sortItemsByName = false;
    Collections.sort(StoreManager.sortedSoldItemList);
  }

  public static void addItem(int itemId, int quantity, int price, int limit) {
    StoreManager.addItem(ItemPool.get(itemId, quantity), price, limit);

    StoreManager.sortItemsByName = true;
    Collections.sort(StoreManager.soldItemList);
    StoreManager.sortItemsByName = false;
    Collections.sort(StoreManager.sortedSoldItemList);
  }

  private static void addItem(AdventureResult item, int price, int limit) {
    int itemId = item.getItemId();
    int quantity = item.getCount();

    SoldItem soldItem = new SoldItem(itemId, quantity, price, limit, 0);
    int index = StoreManager.soldItemList.indexOf(soldItem);

    if (index < 0) {
      StoreManager.soldItemList.add(soldItem);
      StoreManager.sortedSoldItemList.add(soldItem);
    } else {
      int sortedIndex = StoreManager.sortedSoldItemList.indexOf(soldItem);
      soldItem = soldItemList.get(index);

      int amount = soldItem.getQuantity() + quantity;
      int lowest = soldItem.getLowest();
      // The new price and limit override existing price and limit

      soldItem = new SoldItem(itemId, amount, price, limit, lowest);

      StoreManager.soldItemList.set(index, soldItem);
      StoreManager.sortedSoldItemList.set(sortedIndex, soldItem);
    }
  }

  public static void updateItem(int itemId, int quantity, int price, int limit) {
    StoreManager.updateItem(ItemPool.get(itemId, quantity), price, limit);

    StoreManager.sortItemsByName = true;
    Collections.sort(StoreManager.soldItemList);
    StoreManager.sortItemsByName = false;
    Collections.sort(StoreManager.sortedSoldItemList);
  }

  private static void updateItem(AdventureResult item, int price, int limit) {
    int itemId = item.getItemId();
    int quantity = item.getCount();

    SoldItem soldItem = new SoldItem(itemId, quantity, price, limit, 0);
    int index = StoreManager.soldItemList.indexOf(soldItem);

    if (index < 0) {
      StoreManager.soldItemList.add(soldItem);
      StoreManager.sortedSoldItemList.add(soldItem);
    } else {
      int sortedIndex = StoreManager.sortedSoldItemList.indexOf(soldItem);
      StoreManager.soldItemList.set(index, soldItem);
      StoreManager.sortedSoldItemList.set(sortedIndex, soldItem);
    }
  }

  public static void removeItem(int itemId, int quantity) {
    SoldItem item = new SoldItem(itemId, 0, 0, 0, 0);
    int index = StoreManager.soldItemList.indexOf(item);
    int sortedIndex = StoreManager.sortedSoldItemList.indexOf(item);

    if (index < 0) {
      // Something went wrong, give up
      return;
    }

    item = soldItemList.get(index);
    int amount = item.getQuantity() - quantity;
    if (amount == 0) {
      StoreManager.soldItemList.remove(index);
      StoreManager.sortedSoldItemList.remove(sortedIndex);
      return;
    }

    int price = item.getPrice();
    int limit = item.getLimit();
    int lowest = item.getLowest();

    item = new SoldItem(itemId, amount, price, limit, lowest);

    StoreManager.soldItemList.set(index, item);
    StoreManager.sortedSoldItemList.set(sortedIndex, item);
  }

  public static final void updateSomePrices(String storeText) {
    int startIndex = storeText.indexOf("<!-- U:{");
    if (startIndex == -1) return;
    startIndex += 7;
    int endIndex = storeText.indexOf("-->", startIndex);
    if (endIndex == -1) return;

    storeText = storeText.substring(startIndex, endIndex);

    JSONObject json;
    try {
      json = new JSONObject(storeText);

      String[] itemDescs = JSONObject.getNames(json);

      for (String itemDesc : itemDescs) {
        int itemId = ItemDatabase.getItemIdFromDescription(itemDesc);
        JSONObject item = json.getJSONObject(itemDesc);
        int newPrice = item.getInt("price");
        int newLimit = item.getInt("lim");

        StoreManager.SoldItem soldItem = new StoreManager.SoldItem(itemId, 0, 0, 0, 0);
        int index = StoreManager.soldItemList.indexOf(soldItem);
        int sortedIndex = StoreManager.sortedSoldItemList.indexOf(soldItem);

        // This should only happen if we have not built these lists.
        if (index == -1 || sortedIndex == -1) {
          continue;
        }

        soldItem = soldItemList.get(index);
        int quantity = soldItem.getQuantity();
        int lowest = Math.min(soldItem.getLowest(), newPrice);
        soldItem = new StoreManager.SoldItem(itemId, quantity, newPrice, newLimit, lowest);
        StoreManager.soldItemList.set(index, soldItem);
        StoreManager.sortedSoldItemList.set(sortedIndex, soldItem);
      }

      StoreManager.sortItemsByName = true;
      Collections.sort(StoreManager.soldItemList);
      StoreManager.sortItemsByName = false;
      Collections.sort(StoreManager.sortedSoldItemList);
    } catch (JSONException e) {
      RequestLogger.printLine("JSON failure while updating prices.");
      return;
    }
  }
}
