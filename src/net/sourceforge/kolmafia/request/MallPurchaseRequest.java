package net.sourceforge.kolmafia.request;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.Limitmode;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MallPurchaseRequest extends PurchaseRequest {
  private static final Pattern YIELD_PATTERN =
      Pattern.compile(
          "You may only buy ([\\d,]+) of this item per day from this store\\.You have already purchased ([\\d,]+)");

  public static final Set<Integer> disabledStores = new HashSet<Integer>();
  public static final Set<Integer> ignoringStores = new HashSet<Integer>();

  private final int shopId;

  private static final Pattern STOREID_PATTERN = Pattern.compile("whichstore\\d?=(\\d+)");

  public static final int getStoreId(final String urlString) {
    return GenericRequest.getNumericField(urlString, MallPurchaseRequest.STOREID_PATTERN);
  }

  public int getShopId() {
    return this.shopId;
  }

  public static void reset() {
    // Stores which are ignoring one character may not be ignoring
    // another player
    MallPurchaseRequest.ignoringStores.clear();
  }

  public static List<String> getForbiddenStores() {
    // We want to return a mutable list.
    // String.split returns a fixed-size list
    // String.split returns a list with an empty element if the input string is empty
    String input = Preferences.getString("forbiddenStores").trim();
    if (input.equals("")) {
      return new ArrayList<String>();
    }
    return new ArrayList(Arrays.asList(input.split("\\s*,\\s*")));
  }

  public static void addForbiddenStore(int shopId) {
    List<String> forbidden = MallPurchaseRequest.getForbiddenStores();
    String shopIdString = String.valueOf(shopId);
    if (!forbidden.contains(shopIdString)) {
      forbidden.add(shopIdString);
      Preferences.setString("forbiddenStores", String.join(",", forbidden));
    }
  }

  /**
   * Constructs a new <code>MallPurchaseRequest</code> with the given values. Note that the only
   * value which can be modified at a later time is the quantity of items being purchases; all
   * others are consistent through the time when the purchase is actually executed.
   *
   * @param itemName The name of the item to be purchased
   * @param itemId The database Id for the item to be purchased
   * @param quantity The quantity of items to be purchased
   * @param shopId The integer identifier for the shop from which the item will be purchased
   * @param shopName The name of the shop
   * @param price The price at which the item will be purchased
   * @param limit The maximum number of items that can be purchased per day
   * @param canPurchase Whether or not this purchase request is possible
   */
  public MallPurchaseRequest(
      final int itemId,
      final int quantity,
      final int shopId,
      final String shopName,
      final int price,
      final int limit,
      final boolean canPurchase) {
    this(ItemPool.get(itemId), quantity, shopId, shopName, price, limit, canPurchase);
  }

  public MallPurchaseRequest(
      final int itemId,
      final int quantity,
      final int shopId,
      final String shopName,
      final int price,
      final int limit) {
    this(ItemPool.get(itemId), quantity, shopId, shopName, price, limit, true);
  }

  public MallPurchaseRequest(
      final AdventureResult item,
      final int quantity,
      final int shopId,
      final String shopName,
      final int price,
      final int limit,
      final boolean canPurchase) {
    super("mallstore.php");

    this.isMallStore = true;
    this.hashField = "pwd";
    this.item = item;

    this.shopName = shopName;
    this.shopId = shopId;

    this.quantity = quantity;
    this.price = price;
    this.limit = Math.min(quantity, limit);
    this.canPurchase = canPurchase;

    this.addFormField("whichstore", String.valueOf(shopId));
    this.addFormField("buying", "1");
    this.addFormField("ajax", "1");
    this.addFormField("whichitem", MallPurchaseRequest.getStoreString(item.getItemId(), price));

    this.timestamp = System.currentTimeMillis();
  }

  public static final String getStoreString(final int itemId, final int price) {
    // whichitem=2272000000246

    StringBuilder whichItem = new StringBuilder();
    whichItem.append(itemId);

    int originalLength = whichItem.length();
    whichItem.append(price);

    while (whichItem.length() < originalLength + 9) {
      whichItem.insert(originalLength, '0');
    }

    return whichItem.toString();
  }

  @Override
  public long getAvailableMeat() {
    return KoLCharacter.canInteract()
        ? KoLCharacter.getAvailableMeat()
        : KoLCharacter.getStorageMeat();
  }

  @Override
  public String color() {
    return !this.canPurchase
        ? "gray"
        : KoLCharacter.canInteract()
            ? (KoLCharacter.getAvailableMeat() >= this.price ? null : "gray")
            : (KoLCharacter.getStorageMeat() >= this.price ? "blue" : "gray");
  }

  @Override
  public void run() {
    if (this.shopId == KoLCharacter.getUserId()) {
      return;
    }

    if (MallPurchaseRequest.disabledStores.contains(this.shopId)) {
      KoLmafia.updateDisplay(
          "This shop "
              + this.shopName
              + ", owned by #"
              + this.shopId
              + ") is disabled. Skipping...");
      return;
    }

    if (MallPurchaseRequest.ignoringStores.contains(this.shopId)) {
      KoLmafia.updateDisplay(
          "This shop ("
              + this.shopName
              + ", owned by #"
              + this.shopId
              + ") is ignoring you. Skipping...");
      return;
    }

    if (MallPurchaseRequest.getForbiddenStores().contains(String.valueOf(this.shopId))) {
      KoLmafia.updateDisplay(
          "This shop ("
              + this.shopName
              + ", owned by #"
              + this.shopId
              + ") is on your forbidden list. Skipping...");
      return;
    }

    if (Limitmode.limitMall()) {
      return;
    }

    this.addFormField("quantity", String.valueOf(this.limit));

    super.run();
  }

  @Override
  public int getCurrentCount() {
    List<AdventureResult> list =
        KoLCharacter.canInteract() ? KoLConstants.inventory : KoLConstants.storage;
    return this.item.getCount(list);
  }

  private static int extractItemId(final String urlString) {
    Matcher itemMatcher = TransferItemRequest.ITEMID_PATTERN.matcher(urlString);
    if (!itemMatcher.find()) {
      return -1;
    }

    // whichitem=2272000000246
    // the last 9 characters of idString are the price, with leading zeros

    String idString = itemMatcher.group(1);
    return StringUtilities.parseInt(idString.substring(0, idString.length() - 9));
  }

  @Override
  public void processResults() {
    MallPurchaseRequest.parseResponse(this.getURLString(), this.responseText);

    int quantityAcquired = this.getCurrentCount() - this.initialCount;
    if (quantityAcquired > 0) {
      return;
    }

    int startIndex = this.responseText.indexOf("<center>");
    int stopIndex = this.responseText.indexOf("</table>");

    if (startIndex == -1 || stopIndex == -1) {
      KoLmafia.updateDisplay("Store unavailable.  Skipping...");
      return;
    }

    String result = this.responseText.substring(startIndex, stopIndex);

    // One error is that the item price changed, or the item
    // is no longer available because someone was faster at
    // purchasing the item.	 If that's the case, just return
    // without doing anything; nothing left to do.

    if (this.responseText.contains("You can't afford")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Not enough funds.");
      return;
    }

    // If you are on a player's ignore list, you can't buy from his store

    if (this.responseText.contains("That player will not sell to you")) {
      KoLmafia.updateDisplay(
          "You are on this shop's ignore list (#" + this.shopId + "). Skipping...");
      RequestLogger.updateSessionLog(
          "You are on this shop's ignore list (#" + this.shopId + "). Skipping...");
      MallPurchaseRequest.ignoringStores.add(shopId);
      if (Preferences.getBoolean("autoForbidIgnoringStores")) {
        MallPurchaseRequest.addForbiddenStore(this.shopId);
      }
      StoreManager.flushCache(-1, this.shopId);
      return;
    }

    // This store belongs to a player whose account has been disabled for policy violation. Its
    // inventory is frozen.

    if (this.responseText.contains("Its inventory is frozen")) {
      KoLmafia.updateDisplay("This shop's inventory is frozen (#" + this.shopId + "). Skipping...");
      RequestLogger.updateSessionLog(
          "This shop's inventory is frozen (#" + this.shopId + "). Skipping...");
      MallPurchaseRequest.disabledStores.add(shopId);
      StoreManager.flushCache(-1, this.shopId);
      return;
    }

    // Another thing to search for is to see if the person
    // swapped the price on the item, or you got a "failed
    // to yield" message.  In that case, you may wish to
    // re-attempt the purchase.

    if (this.responseText.contains("This store doesn't")
        || this.responseText.contains("failed to yield")) {
      Matcher itemChangedMatcher =
          Pattern.compile(
                  "<td valign=center><b>"
                      + this.item.getName()
                      + "</b> \\(([\\d,]+)\\) </td><td>([\\d,]+) Meat")
              .matcher(result);

      if (itemChangedMatcher.find()) {
        int limit = StringUtilities.parseInt(itemChangedMatcher.group(1));
        int newPrice = StringUtilities.parseInt(itemChangedMatcher.group(2));

        // If the item exists at a lower or equivalent
        // price, then you should re-attempt the purchase
        // of the item.

        if (this.price >= newPrice) {
          KoLmafia.updateDisplay("Failed to yield.  Attempting repurchase...");
          (new MallPurchaseRequest(
                  this.item,
                  Math.min(limit, this.quantity),
                  this.shopId,
                  this.shopName,
                  newPrice,
                  Math.min(limit, this.quantity),
                  true))
              .run();
        } else {
          KoLmafia.updateDisplay("Price switch detected (#" + this.shopId + "). Skipping...");
        }
      } else {
        KoLmafia.updateDisplay("Failed to yield. Skipping...");
      }

      return;
    }

    // One error that might be encountered is that the user
    // already purchased the item; if that's the case, and
    // the user hasn't exhausted their limit, then make a
    // second request to the server containing the correct
    // number of items to buy.

    Matcher quantityMatcher = MallPurchaseRequest.YIELD_PATTERN.matcher(result);

    if (quantityMatcher.find()) {
      int limit = StringUtilities.parseInt(quantityMatcher.group(1));
      int alreadyPurchased = StringUtilities.parseInt(quantityMatcher.group(2));

      if (limit != alreadyPurchased) {
        (new MallPurchaseRequest(
                this.item,
                limit - alreadyPurchased,
                this.shopId,
                this.shopName,
                this.price,
                limit,
                true))
            .run();
      }

      this.canPurchase = false;
      return;
    }
  }

  private static final Pattern TABLE_PATTERN =
      Pattern.compile("<table>.*?</table>", Pattern.DOTALL);

  // (You spent 1,900 meat from Hagnk's.<br />You have XXX meat left.)
  private static final Pattern MEAT_PATTERN =
      Pattern.compile(
          "You spent ([\\d,]+) [Mm]eat( from Hagnk's.*?You have ([\\d,]+) [Mm]eat left)?",
          Pattern.DOTALL);

  public static final void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("mallstore.php") || !urlString.contains("whichitem")) {
      return;
    }

    if (responseText.contains("That player will not sell to you")) {
      // This store is unavailable to you.
      int shopId = MallPurchaseRequest.getStoreId(urlString);
      if (shopId == -1) {
        return;
      }

      // Ignore it for the rest of the session.
      MallPurchaseRequest.ignoringStores.add(shopId);
      StoreManager.flushCache(-1, shopId);

      return;
    }

    if (responseText.contains("Its inventory is frozen")) {
      // This store is unavailable to you.
      int shopId = MallPurchaseRequest.getStoreId(urlString);
      if (shopId == -1) {
        return;
      }

      // Ignore it for the rest of the session.
      MallPurchaseRequest.disabledStores.add(shopId);
      StoreManager.flushCache(-1, shopId);

      return;
    }

    // Mall stores themselves can only contain processable results
    // when actually buying an item, and then only at the very top
    // of the page.

    Matcher tableMatcher = MallPurchaseRequest.TABLE_PATTERN.matcher(responseText);
    if (!tableMatcher.find()) {
      return;
    }

    AdventureResult result = MallPurchaseRequest.processItemFromMall(tableMatcher.group(0));

    Matcher meatMatcher = MallPurchaseRequest.MEAT_PATTERN.matcher(responseText);
    if (!meatMatcher.find()) {
      return;
    }

    int cost = StringUtilities.parseInt(meatMatcher.group(1));
    if (meatMatcher.group(2) != null) {
      int balance = StringUtilities.parseInt(meatMatcher.group(3));
      KoLCharacter.setStorageMeat(balance);
    } else {
      ResultProcessor.processMeat(-cost);
      KoLCharacter.updateStatus();
    }
  }

  // You acquire an item: <b>tiny plastic Charity the Zombie Hunter</b> (stored in Hagnk's Ancestral
  // Mini-Storage)
  // You acquire <b>2 tiny plastic Charities the Zombie Hunters</b> (stored in Hagnk's Ancestral
  // Mini-Storage)

  // You acquire <b>11 limes</b><br>(That's ridiculous.  It's not even funny.) (stored in Hagnk's
  // Ancestral Mini-Storage)
  // You acquire <b>23 limes</b><font color=white>FNORD</font> (stored in Hagnk's Ancestral
  // Mini-Storage)
  // You acquire <b>37 limes</b><br>(In a row?!) (stored in Hagnk's Ancestral Mini-Storage)

  public static Pattern ITEM_PATTERN =
      Pattern.compile(
          "<table class=\"item\".*?rel=\".*?\".*?( \\(stored in Hagnk's Ancestral Mini-Storage\\))?</td></tr></table>",
          Pattern.DOTALL);

  public static final AdventureResult processItemFromMall(final String text) {
    // Items are now wrapped in KoL's standard "relstring" table"

    Matcher itemMatcher = MallPurchaseRequest.ITEM_PATTERN.matcher(text);
    if (!itemMatcher.find()) {
      return null;
    }

    String result = itemMatcher.group(0);
    boolean storage = itemMatcher.group(1) != null;
    if (storage) {
      result = result.replaceFirst("\\(stored in Hagnk's Ancestral Mini-Storage\\)", "");
    }

    ArrayList<AdventureResult> results = new ArrayList<AdventureResult>();
    ResultProcessor.processResults(false, result, results);

    if (results.isEmpty()) {
      // Shouldn't happen
      return null;
    }

    AdventureResult item = results.get(0);
    if (storage) {
      // Add the item to storage
      AdventureResult.addResultToList(KoLConstants.storage, item);
    } else {
      // Add the item to inventory
      ResultProcessor.processResult(item);
    }

    return item;
  }

  public static final boolean registerRequest(final String urlString) {
    // mallstore.php?whichstore=294980&buying=1&ajax=1&whichitem=2272000000246&quantity=9

    if (!urlString.startsWith("mallstore.php")) {
      return false;
    }

    Matcher itemMatcher = TransferItemRequest.ITEMID_PATTERN.matcher(urlString);
    if (!itemMatcher.find()) {
      return true;
    }

    Matcher quantityMatcher = TransferItemRequest.QUANTITY_PATTERN.matcher(urlString);
    if (!quantityMatcher.find()) {
      return true;
    }

    int quantity = StringUtilities.parseInt(quantityMatcher.group(1));

    // whichitem=2272000000246
    // the last 9 characters of idString are the price, with leading zeros
    String idString = itemMatcher.group(1);
    int idStringLength = idString.length();
    String priceString = idString.substring(idStringLength - 9, idStringLength);
    idString = idString.substring(0, idStringLength - 9);

    // In a perfect world where I was not so lazy, I'd verify that
    // the price string was really an int and might find another
    // way to effectively strip leading zeros from the display

    int priceVal = StringUtilities.parseInt(priceString);
    int itemId = StringUtilities.parseInt(idString);
    String itemName = ItemDatabase.getItemName(itemId);

    // store ID is embedded in the URL.  Extract it and get
    // the store name for logging

    int shopId = MallPurchaseRequest.getStoreId(urlString);
    String storeName = shopId != -1 ? ("shop #" + shopId) : "a PC store";

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(
        "buy "
            + quantity
            + " "
            + itemName
            + " for "
            + priceVal
            + " each from "
            + storeName
            + " on "
            + KoLConstants.DAILY_FORMAT.format(new Date()));

    return true;
  }
}
