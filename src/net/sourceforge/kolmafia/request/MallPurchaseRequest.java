package net.sourceforge.kolmafia.request;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.UIManager;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.MallPriceManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MallPurchaseRequest extends PurchaseRequest {

  private final int shopId;

  public static final Set<Integer> disabledStores = new HashSet<>();
  public static final Set<Integer> ignoringStores = new HashSet<>();

  public boolean isDisabled() {
    return isDisabled(this.shopId);
  }

  public static boolean isDisabled(int shopId) {
    return disabledStores.contains(shopId);
  }

  public static void addDisabledStore(int shopId) {
    disabledStores.add(shopId);
    NamedListenerRegistry.fireChange("(disabledStore)");
    MallPriceManager.resetMallPrices(shopId);
  }

  public boolean isIgnoring() {
    return isIgnoring(this.shopId);
  }

  public static boolean isIgnoring(int shopId) {
    return ignoringStores.contains(shopId);
  }

  public static void addIgnoringStore(int shopId) {
    ignoringStores.add(shopId);
    NamedListenerRegistry.fireChange("(ignoringStore)");
    MallPriceManager.resetMallPrices(shopId);
  }

  private static class ForbiddenStoreManager implements Listener {
    // Lazy initialization: initialize only when first needed.
    private Set<Integer> forbiddenStores = new HashSet<>();
    private boolean loaded = false;

    ForbiddenStoreManager() {
      PreferenceListenerRegistry.registerPreferenceListener("forbiddenStores", this);
    }

    public void update() {
      this.load();
    }

    public void reset() {
      forbiddenStores.clear();
      loaded = false;
    }

    public Set<Integer> getForbiddenStores() {
      if (!loaded) {
        load();
      }
      return forbiddenStores;
    }

    public boolean isForbidden(int shopId) {
      if (!loaded) {
        load();
      }
      return forbiddenStores.contains(shopId);
    }

    public void add(int shopId) {
      if (!loaded) {
        load();
      }
      forbiddenStores.add(shopId);
      this.save();
    }

    public void remove(int shopId) {
      if (!loaded) {
        load();
      }
      forbiddenStores.remove(shopId);
      this.save();
    }

    public void save() {
      Preferences.setString(
          "forbiddenStores",
          String.join(
              ",", forbiddenStores.stream().map(String::valueOf).collect(Collectors.joining(","))));
    }

    public void load() {
      loaded = true;

      forbiddenStores.clear();

      String input = Preferences.getString("forbiddenStores");
      if (input.equals("")) {
        return;
      }

      Set<Integer> forbidden =
          Arrays.stream(input.split("\\s*,\\s*"))
              .filter(s -> s.matches("[0-9]+"))
              .mapToInt(Integer::parseInt)
              .boxed()
              .collect(Collectors.toSet());

      forbiddenStores.addAll(forbidden);
    }
  }

  private static ForbiddenStoreManager forbiddenManager = new ForbiddenStoreManager();

  public boolean isForbidden() {
    return isForbidden(this.shopId);
  }

  public static boolean isForbidden(int shopId) {
    return forbiddenManager.isForbidden(shopId);
  }

  public static Set<Integer> getForbiddenStores() {
    return forbiddenManager.getForbiddenStores();
  }

  public static void addForbiddenStore(int shopId) {
    forbiddenManager.add(shopId);
    MallPriceManager.resetMallPrices(shopId);
  }

  public static void removeForbiddenStore(int shopId) {
    forbiddenManager.remove(shopId);
    MallPriceManager.resetMallPrices(shopId);
  }

  public void toggleForbiddenStore() {
    toggleForbiddenStore(this.shopId);
  }

  public static void toggleForbiddenStore(int shopId) {
    if (isForbidden(shopId)) {
      forbiddenManager.remove(shopId);
    } else {
      forbiddenManager.add(shopId);
    }
    MallPriceManager.resetMallPrices(shopId);
  }

  public static void reset() {
    // Stores which ignore one player may not ignore another player
    ignoringStores.clear();
    // Each player chooses which stores to forbid
    forbiddenManager.reset();
  }

  @Override
  public boolean canPurchase() {
    return this.canPurchase && !this.isDisabled() && !this.isIgnoring() && !this.isForbidden();
  }

  private static final Pattern STOREID_PATTERN = Pattern.compile("whichstore\\d?=(\\d+)");

  public static int getStoreId(final String urlString) {
    return GenericRequest.getNumericField(urlString, MallPurchaseRequest.STOREID_PATTERN);
  }

  public int getShopId() {
    return this.shopId;
  }

  /**
   * Constructs a new <code>MallPurchaseRequest</code> with the given values. Note that the only
   * value which can be modified at a later time is the quantity of items being purchases; all
   * others are consistent through the time when the purchase is actually executed.
   *
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
      final long price,
      final int limit,
      final boolean canPurchase) {
    this(ItemPool.get(itemId), quantity, shopId, shopName, price, limit, canPurchase);
  }

  public MallPurchaseRequest(
      final int itemId,
      final int quantity,
      final int shopId,
      final String shopName,
      final long price,
      final int limit) {
    this(ItemPool.get(itemId), quantity, shopId, shopName, price, limit, true);
  }

  public MallPurchaseRequest(
      final AdventureResult item,
      final int quantity,
      final int shopId,
      final String shopName,
      final long price,
      final int limit,
      final boolean canPurchase) {
    super("mallstore.php");

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

    this.timestamp = MallPriceManager.currentTimeMillis();
  }

  @Override
  public boolean isMallStore() {
    return true;
  }

  public static String getStoreString(final int itemId, final long price) {
    // whichitem=2272.246
    return itemId + "." + price;
  }

  public static int itemFromStoreString(String storeString) {
    int index = storeString.indexOf('.');
    return StringUtilities.parseInt(storeString.substring(0, index));
  }

  public static long priceFromStoreString(String storeString) {
    int index = storeString.indexOf('.');
    return StringUtilities.parseLong(storeString.substring(index + 1));
  }

  @Override
  public long getAvailableMeat() {
    return KoLCharacter.canInteract()
        ? KoLCharacter.getAvailableMeat()
        : KoLCharacter.getStorageMeat();
  }

  @Override
  public String color() {
    if (this.isForbidden()) {
      // Try get the color from look and feel
      Color color = UIManager.getColor("InternalFrame.closePressedBackground");

      if (color == null) {
        return "red";
      }

      return "#" + Integer.toHexString(color.getRGB()).substring(2);
    }

    return !this.canPurchase()
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

    if (isDisabled(this.shopId)) {
      KoLmafia.updateDisplay(
          "This shop "
              + this.shopName
              + ", owned by #"
              + this.shopId
              + ") is disabled. Skipping...");
      return;
    }

    if (isIgnoring(this.shopId)) {
      KoLmafia.updateDisplay(
          "This shop ("
              + this.shopName
              + ", owned by #"
              + this.shopId
              + ") is ignoring you. Skipping...");
      return;
    }

    if (isForbidden(this.shopId)) {
      KoLmafia.updateDisplay(
          "This shop ("
              + this.shopName
              + ", owned by #"
              + this.shopId
              + ") is on your forbidden list. Skipping...");
      return;
    }

    if (KoLCharacter.getLimitMode().limitMall()) {
      return;
    }

    this.addFormField("quantity", String.valueOf(this.limit));

    super.run();
  }

  @Override
  public int getCurrentCount() {
    List<AdventureResult> list =
        KoLCharacter.canInteract()
            ? KoLConstants.inventory
            : StorageRequest.isFreePull(item) ? KoLConstants.freepulls : KoLConstants.storage;
    return this.item.getCount(list);
  }

  private static int extractItemId(final String urlString) {
    Matcher itemMatcher = TransferItemRequest.ITEMID_PATTERN.matcher(urlString);
    if (!itemMatcher.find()) {
      return -1;
    }

    // whichitem=2272.246
    // the characters after the dot of idString are the price

    String idString = itemMatcher.group(1);
    return itemFromStoreString(idString);
  }

  private static final Pattern YIELD_PATTERN =
      Pattern.compile(
          "You may only buy ([\\d,]+) of this item per day from this store\\. You have already purchased ([\\d,]+)");

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

    // If the item price changed, or the item is no longer available
    // because someone was faster at purchasing the item, just return
    // without doing anything; nothing left to do.

    if (this.responseText.contains("You can't afford")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Not enough funds.");
      return;
    }

    // If you are on a player's ignore list, you can't buy from his store

    if (this.responseText.contains("That player will not sell to you")) {
      KoLmafia.updateDisplay("You are on this shop's ignore list (#" + shopId + "). Skipping...");
      RequestLogger.updateSessionLog(
          "You are on this shop's ignore list (#" + shopId + "). Skipping...");
      addIgnoringStore(shopId);
      if (Preferences.getBoolean("autoForbidIgnoringStores")) {
        addForbiddenStore(shopId);
      }
      MallPriceManager.flushCache(-1, shopId);
      return;
    }

    // This store belongs to a player whose account has been disabled for policy violation.
    // Its inventory is frozen.

    if (this.responseText.contains("Its inventory is frozen")) {
      KoLmafia.updateDisplay("This shop's inventory is frozen (#" + shopId + "). Skipping...");
      RequestLogger.updateSessionLog(
          "This shop's inventory is frozen (#" + shopId + "). Skipping...");
      addDisabledStore(shopId);
      MallPriceManager.flushCache(-1, shopId);
      return;
    }

    // If the person swapped the price on the item, or you got a "failed
    // to yield" message, you may wish to re-attempt the purchase.

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
        long newPrice = StringUtilities.parseLong(itemChangedMatcher.group(2));

        // If the item exists at a lower or equivalent price, then you
        // should re-attempt the purchase of the item.

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

    // If the user already purchased the item, and the user hasn't
    // exhausted their limit, then make a second request to the server
    // containing the correct number of items to buy.

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
    }
  }

  private static final Pattern TABLE_PATTERN =
      Pattern.compile("<table>.*?</table>", Pattern.DOTALL);

  // (You spent 1,900 meat from Hagnk's.<br />You have XXX meat left.)
  private static final Pattern MEAT_PATTERN =
      Pattern.compile(
          "You spent ([\\d,]+) [Mm]eat( from Hagnk's.*?You have ([\\d,]+) [Mm]eat left)?",
          Pattern.DOTALL);

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("mallstore.php") || !urlString.contains("whichitem")) {
      return;
    }

    if (responseText.contains("That player will not sell to you")) {
      // This store is unavailable to you.
      int shopId = MallPurchaseRequest.getStoreId(urlString);
      if (shopId != -1) {
        // Ignore it for the rest of the session.
        addIgnoringStore(shopId);
        MallPriceManager.flushCache(-1, shopId);
      }

      return;
    }

    if (responseText.contains("Its inventory is frozen")) {
      // This store is unavailable to you.
      int shopId = MallPurchaseRequest.getStoreId(urlString);
      if (shopId != -1) {
        // Ignore it for the rest of the session.
        addDisabledStore(shopId);
        MallPriceManager.flushCache(-1, shopId);
      }

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
      long balance = StringUtilities.parseLong(meatMatcher.group(3));
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

  public static final Pattern ITEM_PATTERN =
      Pattern.compile(
          "<table class=\"item\".*?rel=\".*?\".*?( \\(stored in Hagnk's Ancestral Mini-Storage\\))?</td></tr></table>",
          Pattern.DOTALL);

  public static AdventureResult processItemFromMall(final String text) {
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

    ArrayList<AdventureResult> results = new ArrayList<>();
    ResultProcessor.processResults(false, result, results);

    if (results.isEmpty()) {
      // Shouldn't happen
      return null;
    }

    AdventureResult item = results.get(0);
    if (storage) {
      // Add the item to storage
      List<AdventureResult> list =
          StorageRequest.isFreePull(item) ? KoLConstants.freepulls : KoLConstants.storage;
      AdventureResult.addResultToList(list, item);
    } else {
      // Add the item to inventory
      ResultProcessor.processResult(item);
    }

    return item;
  }

  public static boolean registerRequest(final String urlString) {
    // mallstore.php?whichstore=294980&buying=1&ajax=1&whichitem=2272.246&quantity=9

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

    // whichitem=2272.246
    // the characters after the dot of idString are the price
    String idString = itemMatcher.group(1);

    long priceVal = priceFromStoreString(idString);
    int itemId = itemFromStoreString(idString);
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
