package net.sourceforge.kolmafia.request;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.utilities.AdventureResultArray;
import net.sourceforge.kolmafia.utilities.IntegerArray;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AutoMallRequest extends TransferItemRequest {
  public static final Pattern PRICE_PATTERN = Pattern.compile("price[^=&]*\\d*=([\\d]+)?");
  public static final Pattern LIMIT_PATTERN = Pattern.compile("limit[^=&]*\\d*=([\\d]+)?");

  private final int[] prices;
  private final int[] limits;

  public AutoMallRequest(final AdventureResult item, final int price, final int limit) {
    this(new AdventureResult[] {item}, new int[] {price}, new int[] {limit});
  }

  public AutoMallRequest(final AdventureResult[] items) {
    this(items, new int[0], new int[0]);
  }

  public AutoMallRequest(final AdventureResult[] items, final int[] prices, final int[] limits) {
    super("managestore.php", items);

    this.prices = new int[prices.length];
    this.limits = new int[limits.length];

    this.addFormField("action", "additem");
    this.addFormField("ajax", "1");

    for (int i = 0; i < prices.length; ++i) {
      this.prices[i] = prices[i];
    }

    for (int i = 0; i < limits.length; ++i) {
      this.limits[i] = limits[i];
    }
  }

  @Override
  public String getItemField() {
    return "whichitem";
  }

  @Override
  public String getQuantityField() {
    return "qty";
  }

  @Override
  public String getMeatField() {
    return "";
  }

  @Override
  public void attachItem(final AdventureResult item, final int index) {
    this.addFormField("item" + index, String.valueOf(item.getItemId()));
    this.addFormField(this.getQuantityField() + index, String.valueOf(item.getCount()));

    int pos = Arrays.asList(this.attachments).indexOf(item) & 0xFFFF;

    this.addFormField(
        "price" + index,
        pos >= this.prices.length || this.prices[pos] == 0 ? "" : String.valueOf(this.prices[pos]));
    this.addFormField(
        "limit" + index,
        pos >= this.limits.length || this.limits[pos] == 0 ? "" : String.valueOf(this.limits[pos]));
  }

  @Override
  public int getCapacity() {
    return 11;
  }

  @Override
  public TransferItemRequest getSubInstance(final AdventureResult[] attachments) {
    int[] prices = new int[this.prices.length == 0 ? 0 : attachments.length];
    int[] limits = new int[this.prices.length == 0 ? 0 : attachments.length];

    for (int i = 0; i < prices.length; ++i) {
      for (int j = 0; j < this.attachments.length; ++j) {
        if (attachments[i].equals(this.attachments[j])) {
          prices[i] = this.prices[j];
          limits[i] = this.limits[j];
        }
      }
    }

    return new AutoMallRequest(attachments, prices, limits);
  }

  @Override
  public void processResults() {
    super.processResults();

    // We placed stuff in the mall.
    if (this.responseText.indexOf("You don't have a store.") != -1) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You don't have a store.");
      return;
    }

    KoLmafia.updateDisplay("Items offered up for sale.");
  }

  @Override
  public boolean parseTransfer() {
    return AutoMallRequest.parseTransfer(this.getURLString(), this.responseText);
  }

  public static final boolean parseTransfer(final String urlString, final String responseText) {
    if (urlString.indexOf("action=additem") == -1) {
      return false;
    }

    if (responseText.indexOf("You don't have a store.") != -1) {
      return false;
    }

    // Get the items we transferred
    AdventureResultArray items =
        TransferItemRequest.getItemList(
            urlString,
            TransferItemRequest.ITEMID_PATTERN,
            TransferItemRequest.QTY_PATTERN,
            KoLConstants.inventory,
            1);

    // Move them out of inventory
    TransferItemRequest.transferItems(items, KoLConstants.inventory, null);

    // Update the contents of your store with what you just moved in
    if (urlString.contains("ajax=1")) {
      // We cannot assume that the itemList includes
      // everything we asked for or is in the same order.
      AdventureResult[] rawItems = AutoMallRequest.getItems(urlString);
      int[] rawPrices = AutoMallRequest.getPrices(urlString);
      int[] rawLimits = AutoMallRequest.getLimits(urlString);

      IntegerArray prices = new IntegerArray();
      IntegerArray limits = new IntegerArray();
      for (int i = 0; i < rawItems.length; ++i) {
        if (items.contains(rawItems[i])) {
          prices.add(rawPrices[i]);
          limits.add(rawLimits[i]);
        }
      }

      StoreManager.addItems(items.toArray(), prices.toArray(), limits.toArray());
    } else {
      StoreManager.update(responseText, StoreManager.ADDER);
    }

    return true;
  }

  private static AdventureResult[] getItems(final String urlString) {
    AdventureResultArray items = new AdventureResultArray();
    Matcher matcher = TransferItemRequest.ITEMID_PATTERN.matcher(urlString);
    while (matcher.find()) {
      int itemId = StringUtilities.parseInt(matcher.group(1));
      items.add(ItemPool.get(itemId, 0));
    }
    return items.toArray();
  }

  private static int[] getPrices(final String urlString) {
    IntegerArray prices = new IntegerArray();
    Matcher matcher = AutoMallRequest.PRICE_PATTERN.matcher(urlString);
    while (matcher.find()) {
      int price = matcher.group(1) == null ? 999999999 : StringUtilities.parseInt(matcher.group(1));
      prices.add(price);
    }
    return prices.toArray();
  }

  private static int[] getLimits(final String urlString) {
    IntegerArray limits = new IntegerArray();
    Matcher matcher = AutoMallRequest.LIMIT_PATTERN.matcher(urlString);
    while (matcher.find()) {
      int limit = matcher.group(1) == null ? 0 : StringUtilities.parseInt(matcher.group(1));
      limits.add(limit);
    }
    return limits.toArray();
  }

  @Override
  public boolean allowMementoTransfer() {
    return false;
  }

  @Override
  public boolean allowSingletonTransfer() {
    return KoLCharacter.canInteract();
  }

  @Override
  public boolean allowUntradeableTransfer() {
    return false;
  }

  @Override
  public boolean allowUndisplayableTransfer() {
    return false;
  }

  @Override
  public boolean allowUngiftableTransfer() {
    return false;
  }

  @Override
  public String getStatusMessage() {
    return "Transferring items to store";
  }

  public static final boolean registerRequest(final String urlString) {
    Pattern itemPattern = null;
    Pattern quantityPattern = null;

    int quantity = 1;

    if (urlString.startsWith("managestore.php") && urlString.indexOf("action=additem") != -1) {
      itemPattern = TransferItemRequest.ITEMID_PATTERN;
      quantityPattern = TransferItemRequest.QTY_PATTERN;
    } else {
      return false;
    }

    return TransferItemRequest.registerRequest(
        "mallsell", urlString, itemPattern, quantityPattern, KoLConstants.inventory, quantity);
  }
}
