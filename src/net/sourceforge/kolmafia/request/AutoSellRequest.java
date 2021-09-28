package net.sourceforge.kolmafia.request;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.AdventureResultArray;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AutoSellRequest extends TransferItemRequest {
  public static final Pattern AUTOSELL_PATTERN = Pattern.compile("for ([\\d,]+) [Mm]eat");
  private static final Pattern EMBEDDED_ID_PATTERN = Pattern.compile("item(\\d+)");

  private boolean setMode = false;

  public AutoSellRequest(final AdventureResult item) {
    this(new AdventureResult[] {item});
  }

  public AutoSellRequest(final AdventureResult[] items) {
    super("sellstuff.php", items);
    this.addFormField("action", "sell");
    this.addFormField("ajax", "1");
  }

  @Override
  public String getItemField() {
    return "whichitem";
  }

  @Override
  public String getQuantityField() {
    return "quantity";
  }

  @Override
  public String getMeatField() {
    return "";
  }

  @Override
  public void attachItem(final AdventureResult item, final int index) {
    this.attachCompactItem(item);
  }

  public void attachCompactItem(final AdventureResult item) {
    if (!this.setMode) {
      int count = item.getCount();
      int icount = item.getCount(KoLConstants.inventory);

      if (count == icount) {
        // As of 2/1/2006, must specify a quantity
        // field for this - but the value is ignored

        this.addFormField("type", "all");
        this.addFormField("howmany", "1");
      } else if (count == icount - 1) {
        // As of 2/1/2006, must specify a quantity
        // field for this - but the value is ignored

        this.addFormField("type", "allbutone");
        this.addFormField("howmany", "1");
      } else {
        this.addFormField("type", "quant");
        this.addFormField("howmany", String.valueOf(count));
      }

      this.setMode = true;
    }

    // This is a multiple selection input field.
    // Therefore, you can give it multiple items.

    this.addFormField("whichitem[]", String.valueOf(item.getItemId()), true);
  }

  @Override
  public int getCapacity() {
    return Integer.MAX_VALUE;
  }

  @Override
  public ArrayList<TransferItemRequest> generateSubInstances() {
    ArrayList<TransferItemRequest> subinstances = new ArrayList<>();

    if (KoLmafia.refusesContinue()) {
      return subinstances;
    }

    // Autosell singleton items only if we buy them again
    boolean allowSingleton = KoLCharacter.canInteract();

    // Autosell memento items only if player doesn't care
    boolean allowMemento = !Preferences.getBoolean("mementoListActive");

    // Look at all of the attachments and divide them into groups:
    // all, all but one, another quantity

    AdventureResultArray all = new AdventureResultArray();
    AdventureResultArray allButOne = new AdventureResultArray();
    Set<AdventureResult> others = new HashSet<AdventureResult>();

    for (int index = 0; index < this.attachments.length; ++index) {
      AdventureResult item = this.attachments[index];

      if (item == null) {
        continue;
      }

      if (ItemDatabase.getPriceById(item.getItemId()) <= 0) {
        continue;
      }

      // Do not try to autosell items that cannot be discarded
      if (!ItemDatabase.isDiscardable(item.getItemId())) {
        continue;
      }

      // If this item is already on the "sell all" list, skip
      if (all.contains(item)) {
        continue;
      }

      if (!allowMemento && KoLConstants.mementoList.contains(item)) {
        continue;
      }

      int inventoryCount = item.getCount(KoLConstants.inventory);
      int availableCount = inventoryCount;

      if (!allowSingleton && KoLConstants.singletonList.contains(item)) {
        availableCount = TransferItemRequest.keepSingleton(item, availableCount);
      }

      if (availableCount <= 0) {
        continue;
      }

      int desiredCount = Math.min(item.getCount(), availableCount);
      AdventureResult desiredItem = item.getInstance(desiredCount);

      if (desiredCount == inventoryCount) {
        all.add(desiredItem);
      } else if (desiredCount == inventoryCount - 1) {
        allButOne.add(desiredItem);
      } else {
        others.add(desiredItem);
      }
    }

    // For each group - individual quantities, all but one, all -
    // create a subinstance.

    // Iterate over remaining items. Each distinct count goes into
    // its own subinstance
    while (others.size() > 0) {
      AdventureResultArray sublist = new AdventureResultArray();
      Iterator<AdventureResult> it = others.iterator();

      int count = -1;
      while (it.hasNext()) {
        AdventureResult item = it.next();
        int icount = item.getCount();
        if (count == -1) {
          count = icount;
        }
        if (count == icount) {
          it.remove();
          sublist.add(item);
        }
      }

      TransferItemRequest subinstance = this.getSubInstance(sublist.toArray());
      subinstance.isSubInstance = true;
      subinstances.add(subinstance);
    }

    if (allButOne.size() > 0) {
      TransferItemRequest subinstance = this.getSubInstance(allButOne.toArray());
      subinstance.isSubInstance = true;
      subinstances.add(subinstance);
    }

    if (all.size() > 0) {
      TransferItemRequest subinstance = this.getSubInstance(all.toArray());
      subinstance.isSubInstance = true;
      subinstances.add(subinstance);
    }

    return subinstances;
  }

  @Override
  public TransferItemRequest getSubInstance(final AdventureResult[] attachments) {
    return new AutoSellRequest(attachments);
  }

  @Override
  public void processResults() {
    super.processResults();
    KoLmafia.updateDisplay("Items sold.");
  }

  @Override
  public boolean parseTransfer() {
    return AutoSellRequest.parseTransfer(this.getURLString(), this.responseText);
  }

  public static final boolean parseTransfer(final String urlString, final String responseText) {
    if (urlString.startsWith("sellstuff.php")) {
      return AutoSellRequest.parseCompactAutoSell(urlString, responseText);
    }
    if (urlString.startsWith("sellstuff_ugly.php")) {
      return AutoSellRequest.parseDetailedAutoSell(urlString, responseText);
    }
    return false;
  }

  public static final boolean parseCompactAutoSell(
      final String urlString, final String responseText) {
    int quantity = 1;

    Matcher quantityMatcher = TransferItemRequest.HOWMANY_PATTERN.matcher(urlString);
    if (quantityMatcher.find()) {
      quantity = StringUtilities.parseInt(quantityMatcher.group(1));
    }

    if (urlString.indexOf("type=allbutone") != -1) {
      quantity = -1;
    } else if (urlString.indexOf("type=all") != -1) {
      quantity = 0;
    }

    AdventureResultArray itemList =
        TransferItemRequest.getItemList(
            urlString, TransferItemRequest.ITEMID_PATTERN, null, KoLConstants.inventory, quantity);

    if (!itemList.isEmpty()) {
      AutoSellRequest.processMeat(itemList, null);
      TransferItemRequest.transferItems(itemList, KoLConstants.inventory, null);
      KoLCharacter.updateStatus();
    }

    return true;
  }

  public static final boolean parseDetailedAutoSell(
      final String urlString, final String responseText) {
    int quantity = 1;

    Matcher quantityMatcher = TransferItemRequest.QUANTITY_PATTERN.matcher(urlString);
    if (quantityMatcher.find()) {
      quantity = StringUtilities.parseInt(quantityMatcher.group(1));
    }

    if (urlString.indexOf("mode=1") != -1) {
      quantity = 0;
    } else if (urlString.indexOf("mode=2") != -1) {
      quantity = -1;
    }

    AdventureResultArray itemList =
        TransferItemRequest.getItemList(
            urlString, AutoSellRequest.EMBEDDED_ID_PATTERN, null, KoLConstants.inventory, quantity);

    if (!itemList.isEmpty()) {
      AutoSellRequest.processMeat(itemList, responseText);
      TransferItemRequest.transferItems(itemList, KoLConstants.inventory, null);
      KoLCharacter.updateStatus();
    }

    return true;
  }

  private static void processMeat(AdventureResultArray itemList, String responseText) {
    if (KoLCharacter.inFistcore()) {
      int donation = 0;

      for (int i = 0; i < itemList.size(); ++i) {
        AdventureResult item = itemList.get(i);
        int price = ItemDatabase.getPriceById(item.getItemId());
        int count = item.getCount();
        donation += price * count;
      }

      KoLCharacter.makeCharitableDonation(donation);
      return;
    }

    if (responseText == null) {
      return;
    }

    // "You sell your 2 disturbing fanfics to an organ
    // grinder's monkey for 264 Meat."

    Matcher matcher = AutoSellRequest.AUTOSELL_PATTERN.matcher(responseText);
    if (!matcher.find()) {
      return;
    }

    int amount = StringUtilities.parseInt(matcher.group(1));
    ResultProcessor.processMeat(amount);

    String message = "You gain " + KoLConstants.COMMA_FORMAT.format(amount) + " Meat";
    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);
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
    return true;
  }

  @Override
  public boolean allowUndisplayableTransfer() {
    return true;
  }

  @Override
  public boolean allowUngiftableTransfer() {
    return true;
  }

  @Override
  public String getStatusMessage() {
    return "Autoselling items to NPCs";
  }

  public static final boolean registerRequest(final String urlString) {
    Pattern itemPattern = null;
    Pattern quantityPattern = null;
    int quantity = 1;

    if (urlString.startsWith("sellstuff.php")) {
      Matcher quantityMatcher = TransferItemRequest.HOWMANY_PATTERN.matcher(urlString);
      if (quantityMatcher.find()) {
        quantity = StringUtilities.parseInt(quantityMatcher.group(1));
      }

      if (urlString.indexOf("type=allbutone") != -1) {
        quantity = -1;
      } else if (urlString.indexOf("type=all") != -1) {
        quantity = 0;
      }

      itemPattern = TransferItemRequest.ITEMID_PATTERN;
    } else if (urlString.startsWith("sellstuff_ugly.php")) {
      Matcher quantityMatcher = TransferItemRequest.QUANTITY_PATTERN.matcher(urlString);
      if (quantityMatcher.find()) {
        quantity = StringUtilities.parseInt(quantityMatcher.group(1));
      }

      if (urlString.indexOf("mode=1") != -1) {
        quantity = 0;
      } else if (urlString.indexOf("mode=2") != -1) {
        quantity = -1;
      }

      itemPattern = AutoSellRequest.EMBEDDED_ID_PATTERN;
    } else {
      return false;
    }

    return TransferItemRequest.registerRequest(
        "autosell", urlString, itemPattern, quantityPattern, KoLConstants.inventory, quantity);
  }
}
