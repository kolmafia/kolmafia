package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.request.AutoMallRequest;
import net.sourceforge.kolmafia.request.ManageStoreRequest;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.session.StoreManager.SoldItem;
import net.sourceforge.kolmafia.utilities.IntegerArray;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ShopCommand extends AbstractCommand {
  public ShopCommand() {
    this.usage =
        " put [using storage] <item> [@ <price> [limit <num>]] [, <another>] | reprice <item> @ price [limit <num>] [, <another>] | take [all|<num>] <item> [, <another>] - sell, reprice or remove from Mall.";
  }

  @Override
  public void run(final String cmd, String parameters) {
    parameters = parameters.toLowerCase();

    if (!parameters.startsWith("take ")
        && !parameters.startsWith("put ")
        && !parameters.startsWith("reprice ")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Invalid shop command.");
      return;
    }

    if (parameters.startsWith("put ")) {
      ShopCommand.put(parameters.substring(3).trim());
    } else if (parameters.startsWith("take ")) {
      ShopCommand.take(parameters.substring(4).trim());
    } else {
      ShopCommand.reprice(parameters.substring(7).trim());
    }
  }

  public static void put(String parameters) {
    boolean storage = false;

    String TEST = "using storage ";
    if (parameters.startsWith(TEST)) {
      storage = true;
      parameters = parameters.substring(TEST.length()).trim();
    }

    List<AdventureResult> items = new ArrayList<>();
    IntegerArray prices = new IntegerArray();
    IntegerArray limits = new IntegerArray();

    for (String itemName : parameters.split("\\s*,\\s*")) {
      int price = 0;
      int limit = 0;

      int separatorIndex = itemName.indexOf('@');

      if (separatorIndex != -1) {
        String description = itemName.substring(separatorIndex + 1).trim();
        itemName = itemName.substring(0, separatorIndex);

        separatorIndex = description.indexOf("limit");

        if (separatorIndex != -1) {
          limit = StringUtilities.parseInt(description.substring(separatorIndex + 5).trim());
          description = description.substring(0, separatorIndex).trim();
        }

        price = StringUtilities.parseInt(description);
      }

      // workaround issue: user types in something like "mallsell pail @ 20,000,000
      if (StringUtilities.isNumeric(itemName)) {
        RequestLogger.printLine(
            "'"
                + itemName
                + "' is not an item.  Did you use a comma in the middle of a number?  Quitting...");
        return;
      }

      AdventureResult item = ItemFinder.getFirstMatchingItem(itemName, true);

      if (item == null) {
        RequestLogger.printLine("Skipping '" + itemName + "'.");
        continue;
      }

      int inventoryCount = item.getCount(storage ? KoLConstants.storage : KoLConstants.inventory);

      if (item.getCount() > inventoryCount) {
        item = item.getInstance(inventoryCount);
      }

      if (item.getCount() == 0) {
        RequestLogger.printLine(
            "Skipping '" + itemName + "', none found in " + (storage ? "storage." : "inventory."));
        continue;
      }

      items.add(item);
      prices.add(price);
      limits.add(limit);
    }

    if (items.size() > 0) {
      if (storage) {
        RequestThread.postRequest(
            new ManageStoreRequest(
                items.toArray(new AdventureResult[0]),
                prices.toArray(),
                limits.toArray(),
                storage));
      } else {
        RequestThread.postRequest(
            new AutoMallRequest(
                items.toArray(new AdventureResult[0]), prices.toArray(), limits.toArray()));
      }
    }
  }

  public static void take(String parameters) {
    boolean takeAll = false;
    int qty = 1;

    parameters = parameters.trim();

    int space = parameters.indexOf(" ");
    if (space != -1) {
      String token = parameters.substring(0, space);
      if (token.equals("all")) {
        takeAll = true;
        parameters = parameters.substring(space + 1).trim();
      } else if (StringUtilities.isNumeric(token)) {
        qty = StringUtilities.parseInt(token);
        parameters = parameters.substring(space + 1).trim();
      }
    }

    if (!StoreManager.soldItemsRetrieved) {
      RequestThread.postRequest(new ManageStoreRequest());
    }

    List<SoldItem> list = StoreManager.getSoldItemList();

    for (String itemName : parameters.split("\\s*,\\s*")) {
      AdventureResult item = ItemFinder.getFirstMatchingItem(itemName, true);

      if (item == null) {
        RequestLogger.printLine("Skipping '" + itemName + "'.");
        continue;
      }

      int itemId = item.getItemId();

      SoldItem it = new SoldItem(itemId, 0, 0, 0, 0);
      int index = list.indexOf(it);

      if (index < 0) {
        RequestLogger.printLine(itemName + " not found in shop.");
        continue;
      }

      SoldItem soldItem = list.get(index);

      int count = takeAll ? soldItem.getQuantity() : qty;
      RequestThread.postRequest(new ManageStoreRequest(itemId, count));
    }
  }

  public static void reprice(String parameters) {
    if (!StoreManager.soldItemsRetrieved) {
      RequestThread.postRequest(new ManageStoreRequest());
    }

    List<SoldItem> list = StoreManager.getSoldItemList();

    IntegerArray itemIds = new IntegerArray();
    IntegerArray prices = new IntegerArray();
    IntegerArray limits = new IntegerArray();

    for (String itemName : parameters.split("\\s*,\\s*")) {
      AdventureResult item = null;
      int price = 0;
      Integer limit = null;

      int separatorIndex = itemName.indexOf('@');
      if (separatorIndex == -1) {
        RequestLogger.printLine("Skipping '" + itemName + "', no price provided");
        continue;
      }

      String description = itemName.substring(separatorIndex + 1).trim();
      itemName = itemName.substring(0, separatorIndex);

      separatorIndex = description.indexOf("limit");

      if (separatorIndex != -1) {
        limit = StringUtilities.parseInt(description.substring(separatorIndex + 5).trim());
        description = description.substring(0, separatorIndex).trim();
      }

      price = StringUtilities.parseInt(description);

      // workaround issue: user types in something like "shop reprice pail @ 20,000,000
      if (StringUtilities.isNumeric(itemName)) {
        RequestLogger.printLine(
            "'"
                + itemName
                + "' is not an item.  Did you use a comma in the middle of a number?  Quitting...");
        return;
      }

      item = ItemFinder.getFirstMatchingItem(itemName, true);

      if (item == null) {
        RequestLogger.printLine("Skipping '" + itemName + "'.");
        continue;
      }

      int itemId = item.getItemId();

      if (!list.contains(new SoldItem(itemId, 0, 0, 0, 0))) {
        RequestLogger.printLine(itemName + " not found in shop.");
        continue;
      }

      if (limit == null) {
        limit = StoreManager.getLimit(itemId);
      }

      itemIds.add(itemId);
      prices.add(price);
      limits.add(limit);
    }

    if (itemIds.size() > 0) {
      RequestThread.postRequest(
          new ManageStoreRequest(itemIds.toArray(), prices.toArray(), limits.toArray()));
    }
  }
}
