package net.sourceforge.kolmafia.request;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SewerRequest extends CreateItemRequest {
  public SewerRequest(final Concoction conc) {
    super("inv_use.php", conc);
  }

  @Override
  public void reconstructFields() {
    // Even though chewing gum on a string is multi-usable, since
    // we are going for a specific result, inventory/closet
    // manipulation is needed between uses

    this.constructURLString("inv_use.php");
    this.addFormField("which", "3");
    this.addFormField("whichitem", String.valueOf(ItemPool.CHEWING_GUM));
    this.addFormField("ajax", "1");
  }

  @Override
  public void run() {
    AdventureResult item = this.concoction.getItem();
    int itemId = item.getItemId();
    int need = this.getQuantityNeeded();
    int have =
        (itemId == ItemPool.WORTHLESS_ITEM)
            ? SewerRequest.currentWorthlessItemCount()
            : item.getCount(KoLConstants.inventory);

    // Retrieve "need" additional items without using closet or storage.
    AdventureResult goal = item.getInstance(have + need);
    SewerRequest.retrieveSewerItems(goal, false, false);
  }

  @Override
  public void processResults() {
    UseItemRequest.lastItemUsed = null;
  }

  private static final AdventureResult[] STARTER_ITEMS =
      new AdventureResult[] {
        // A hat and a weapon for all six classes
        ItemPool.get(ItemPool.SEAL_HELMET, 1),
        ItemPool.get(ItemPool.SEAL_CLUB, 1),
        ItemPool.get(ItemPool.HELMET_TURTLE, 1),
        ItemPool.get(ItemPool.TURTLE_TOTEM, 1),
        ItemPool.get(ItemPool.RAVIOLI_HAT, 1),
        ItemPool.get(ItemPool.PASTA_SPOON, 1),
        ItemPool.get(ItemPool.HOLLANDAISE_HELMET, 1),
        ItemPool.get(ItemPool.SAUCEPAN, 1),
        ItemPool.get(ItemPool.DISCO_MASK, 1),
        ItemPool.get(ItemPool.DISCO_BALL, 1),
        ItemPool.get(ItemPool.MARIACHI_HAT, 1),
        ItemPool.get(ItemPool.STOLEN_ACCORDION, 1),
        // One pair of pants
        ItemPool.get(ItemPool.OLD_SWEATPANTS, 1),
      };

  private static final AdventureResult[] WORTHLESS_ITEMS =
      new AdventureResult[] {
        ItemPool.get(ItemPool.WORTHLESS_TRINKET, 1),
        ItemPool.get(ItemPool.WORTHLESS_GEWGAW, 1),
        ItemPool.get(ItemPool.WORTHLESS_KNICK_KNACK, 1),
      };

  private static int neededItemCount(final AdventureResult[] items) {
    int count = 0;
    for (AdventureResult item : items) {
      count += item.getCount();
    }
    return count;
  }

  private static int currentItemCount(
      final AdventureResult[] items, final List<AdventureResult> list) {
    int count = 0;
    for (AdventureResult item : items) {
      count += item.getCount(list);
      if (list == KoLConstants.inventory && KoLCharacter.hasEquipped(item)) {
        count += 1;
      }
    }
    return count;
  }

  private static int uniqueItemCount(
      final AdventureResult[] items, final List<AdventureResult> list) {
    int count = 0;
    for (AdventureResult item : items) {
      if ((item.getCount(list) > 0)
          || (list == KoLConstants.inventory && KoLCharacter.hasEquipped(item))) {
        count += 1;
      }
    }
    return count;
  }

  private static int currentWorthlessItemCount() {
    return SewerRequest.currentItemCount(SewerRequest.WORTHLESS_ITEMS, KoLConstants.inventory);
  }

  private static int uniqueStarterItemCount() {
    return SewerRequest.uniqueItemCount(SewerRequest.STARTER_ITEMS, KoLConstants.inventory);
  }

  // This is the method used by InventoryManager.retrieveItem. Which is
  // to say, the argument is the total number of items that should be in
  // inventory at the end.
  //
  // If used via SewerRequest, which is a creation method, the argument
  // must be greater than the number currently in inventory in order to
  // force creation

  public static boolean retrieveSewerItems(
      final AdventureResult item, final boolean canUseCloset, final boolean canUseStorage) {
    // Calculate how many items are needed
    int needed = item.getCount();

    // If looking for "worthless items", that is an alias for three other items
    boolean worthless = (item.getItemId() == ItemPool.WORTHLESS_ITEM);

    AdventureResult[] items;

    if (worthless) {
      items = SewerRequest.WORTHLESS_ITEMS;
    } else {
      items = new AdventureResult[1];
      items[0] = item;
    }

    // Calculate how many we have of all the requested items
    int count = SewerRequest.currentItemCount(items, KoLConstants.inventory);

    if (count >= needed) {
      return true;
    }

    // Pull from closet and see if you have enough
    if (canUseCloset) {
      count = SewerRequest.unclosetSewerItems(items, count - needed);

      if (count >= needed) {
        return true;
      }
    }

    // Pull from storage and see if you have enough
    if (canUseStorage) {
      count = SewerRequest.pullSewerItems(items, count - needed);

      if (count >= needed) {
        return true;
      }
    }

    // If we are looking for worthless items, hermit scrolls can help out
    if (worthless) {
      while (count < needed && InventoryManager.hasItem(HermitRequest.SUMMON_SCROLL)) {
        // Read a single 31337 scroll
        RequestThread.postRequest(UseItemRequest.getInstance(HermitRequest.SUMMON_SCROLL));

        // If we now have a hermit script in inventory, read it
        if (InventoryManager.hasItem(HermitRequest.HACK_SCROLL)) {
          RequestThread.postRequest(UseItemRequest.getInstance(HermitRequest.HACK_SCROLL));
        }

        count = SewerRequest.currentItemCount(items, KoLConstants.inventory);
      }

      if (count >= needed) {
        return true;
      }
    }

    // Do not refresh concoctions while we transfer sewer items around.
    ConcoctionDatabase.deferRefresh(true);

    // If the character has any of the starter items, retrieve them to improve
    // the probability of getting worthless items.

    if (canUseCloset && SewerRequest.STARTER_ITEMS.length > SewerRequest.uniqueStarterItemCount()) {
      SewerRequest.transferStarterItems(items, KoLConstants.closet);
    }

    if (canUseStorage
        && SewerRequest.STARTER_ITEMS.length > SewerRequest.uniqueStarterItemCount()) {
      SewerRequest.transferStarterItems(items, KoLConstants.storage);
    }

    // Save outfit and closet around item purchase
    AdventureResult[] initialCloset = SewerRequest.currentClosetItems(items);

    try {
      int initialClosetCount = SewerRequest.currentItemCount(items, KoLConstants.inventory);
      while (needed > count) {
        int remaining = needed - count;
        int gumUseCount = Math.min(remaining, items.length);

        AdventureResult gum = ItemPool.get(ItemPool.CHEWING_GUM, gumUseCount);

        // Get enough gum for all remaining items to fish out of the sewer
        if (gum.getCount(KoLConstants.inventory) < gumUseCount
            && !InventoryManager.checkpointedRetrieveItem(ItemPool.CHEWING_GUM, remaining)) {
          break;
        }

        // Closet your existing goal items (since they will reduce
        // the probability of you getting more) and then use the gum.
        SewerRequest.closetSewerItems(items);

        // Use chewing gums on a string
        RequestThread.postRequest(UseItemRequest.getInstance(gum));

        // If we got our goal item,
        int currentItemCount = SewerRequest.currentItemCount(items, KoLConstants.inventory);
        count += currentItemCount;
      }
    } finally {
      // Pull the goal items back out of the closet.
      SewerRequest.unclosetSewerItems(SewerRequest.currentClosetItems(items), initialCloset);
    }

    // Now that we have (possibly) gotten more sewer items, refresh
    ConcoctionDatabase.deferRefresh(false);

    if (count < needed) {
      KoLmafia.updateDisplay(
          MafiaState.ABORT, "Unable to acquire " + (needed - count) + " sewer items.");
    }

    return count >= needed;
  }

  private static void transferStarterItems(
      final AdventureResult[] goals, final List<AdventureResult> source) {
    ArrayList<AdventureResult> attachments = new ArrayList<AdventureResult>();
    List<AdventureResult> destination = KoLConstants.inventory;
    List<AdventureResult> skip = Arrays.asList(goals);

    for (AdventureResult item : SewerRequest.STARTER_ITEMS) {
      // If it's a goal item, keep out of inventory
      if (skip.contains(item)) {
        continue;
      }

      // If it's already in inventory, cool
      if (item.getCount(destination) > 0) {
        continue;
      }

      // If we have it equipped, cool
      if (KoLCharacter.hasEquipped(item)) {
        continue;
      }

      // If there are none in the source, oh well
      if (item.getCount(source) == 0) {
        continue;
      }

      attachments.add(ItemPool.get(item.getItemId(), 1));
    }

    if (attachments.isEmpty()) {
      return;
    }

    AdventureResult[] transfers = attachments.toArray(new AdventureResult[attachments.size()]);
    SewerRequest.transferSewerItems(transfers, source, destination);
  }

  private static void transferSewerItems(
      final AdventureResult[] transfers,
      final List<AdventureResult> source,
      final List<AdventureResult> destination) {
    TransferItemRequest request =
        (destination == KoLConstants.closet)
            ? new ClosetRequest(ClosetRequest.INVENTORY_TO_CLOSET, transfers)
            : (source == KoLConstants.storage)
                ? new StorageRequest(StorageRequest.STORAGE_TO_INVENTORY, transfers)
                : new ClosetRequest(ClosetRequest.CLOSET_TO_INVENTORY, transfers);

    RequestThread.postRequest(request);
  }

  private static AdventureResult[] currentClosetItems(final AdventureResult[] items) {
    int count = items.length;
    AdventureResult[] result = new AdventureResult[count];
    for (int index = 0; index < count; index++) {
      AdventureResult item = items[index];
      result[index] = item.getInstance(item.getCount(KoLConstants.closet));
    }
    return result;
  }

  private static int closetSewerItems(final AdventureResult[] items) {
    ArrayList<AdventureResult> transfers = new ArrayList<AdventureResult>();
    for (AdventureResult item : items) {
      int available = item.getCount(KoLConstants.inventory);
      if (available > 0) {
        transfers.add(item.getInstance(available));
      }
    }
    int count = transfers.size();
    if (count > 0) {
      AdventureResult[] transfer = transfers.toArray(new AdventureResult[count]);
      SewerRequest.transferSewerItems(transfer, KoLConstants.inventory, KoLConstants.closet);
    }
    return SewerRequest.currentItemCount(items, KoLConstants.closet);
  }

  private static int unclosetSewerItems(final AdventureResult[] items, int needed) {
    ArrayList<AdventureResult> transfers = new ArrayList<AdventureResult>();
    for (AdventureResult item : items) {
      int available = Math.min(needed, item.getCount(KoLConstants.closet));
      if (available > 0) {
        transfers.add(item.getInstance(available));
        needed -= available;
      }
    }
    int count = transfers.size();
    if (count > 0) {
      AdventureResult[] transfer = transfers.toArray(new AdventureResult[count]);
      SewerRequest.transferSewerItems(transfer, KoLConstants.closet, KoLConstants.inventory);
    }
    return SewerRequest.currentItemCount(items, KoLConstants.inventory);
  }

  private static int unclosetSewerItems(
      final AdventureResult[] currentCloset, final AdventureResult[] initialCloset) {
    ArrayList<AdventureResult> transfers = new ArrayList<AdventureResult>();
    for (int index = 0; index < initialCloset.length; ++index) {
      AdventureResult original = initialCloset[index];
      AdventureResult current = currentCloset[index];
      int available = current.getCount() - original.getCount();
      if (available > 0) {
        transfers.add(original.getInstance(available));
      }
    }
    int count = transfers.size();
    if (count > 0) {
      AdventureResult[] transfer = transfers.toArray(new AdventureResult[count]);
      SewerRequest.transferSewerItems(transfer, KoLConstants.closet, KoLConstants.inventory);
    }
    return SewerRequest.currentItemCount(currentCloset, KoLConstants.inventory);
  }

  private static int pullSewerItems(final AdventureResult[] items, int needed) {
    ArrayList<AdventureResult> transfers = new ArrayList<AdventureResult>();
    for (AdventureResult item : items) {
      int available = Math.min(needed, item.getCount(KoLConstants.storage));
      if (available > 0) {
        transfers.add(item.getInstance(available));
        needed -= available;
      }
    }
    int count = transfers.size();
    if (count > 0) {
      AdventureResult[] transfer = transfers.toArray(new AdventureResult[count]);
      SewerRequest.transferSewerItems(transfer, KoLConstants.storage, KoLConstants.inventory);
    }
    return SewerRequest.currentItemCount(items, KoLConstants.inventory);
  }

  /*
   16 possible sewer items:

   6 classes * 1 hat
   6 classes * 1 weapon
   1 pants
   3 worthless items

   Items can be in inventory or equipped.

   Unless you have all 16 possible items, using a piece of gum will
   retrieve one you don't have yet. If you have all the non-worthless
   items, you are guaranteed to get a worthless item. If are missing
   some non-worthless items, whether you get a worthless item or one of
   the missing non-worthless-items is probabilistic.

   Assume you have no worthless items in inventory
   Let X = number of non-worthless sewer items you have.
   Given X, what is the expected # of gums needed to get a worthless item?

   Consider X = 13. Of the ( 16 - 13 ) = 3 possible items, 3 are your
   goal and ( 3 - 3 ) = 0 are not your goal.

   E(13) = 3/3 * 1 + 0/3 = 1.0

   Consider X = 12. Of the ( 16 - 12 ) = 4 possible items, 3 are your
   goal and ( 4 - 3 ) = 1 are not your goal. You have a 3/4 chance of
   getting your goal with the first piece of gum. If you don't get one,
   you have used 1 gum, now have 13 sewer items and will use another
   piece of gum.

   E(12) = 3/4 * 1 + 1/4 * ( 1 + E(13) ) = .75 + 0.50 = 1.25

   Consider X = 11. Of the ( 16 - 11 ) = 5 possible items, 3 are your
   goal and ( 5 - 3 ) = 2 are not your goal. You have a 3/5 chance of
   getting your goal with the first piece of gum. If you don't get one,
   you have used 1 gum, now have 12 sewer items and will use another
   piece of gum.

   E(11) = 3/5 * 1 + 2/5 * ( 1 + E(12) ) = .60 + 0.90 = 1.50

   This generalizes:

   E(X) = 3/(16-X) + (13-X)/(16-X) * ( 1 + E(X + 1 ) )

   Rearranging terms:

   E(X) = 1 + ( 13 - X ) * E( X + 1 ) / ( 16 - X )

   This little ASH program calculates this:

   float [14] factors;

   factors[ 13 ] = 1.0;
   for x from 12 downto 0
   {
  float f2 = ( 13.0 - x ) * factors[ x + 1] / (16.0 - x );
  factors[ x ] = 1.0 + f2;
   }

   for i from 0 to 13
   {
  float px = factors[ i ] ;
  print( i + ": " + px + " gum = " + ceil( 50.0 * px ) + " Meat" );
   }

   Resulting in this:

   0: 4.25 gum = 213 Meat
   1: 4.0 gum = 200 Meat
   2: 3.75 gum = 188 Meat
   3: 3.5 gum = 175 Meat
   4: 3.25 gum = 163 Meat
   5: 3.0 gum = 150 Meat
   6: 2.75 gum = 138 Meat
   7: 2.5 gum = 125 Meat
   8: 2.25 gum = 113 Meat
   9: 2.0 gum = 100 Meat
   10: 1.75 gum = 88 Meat
   11: 1.5 gum = 75 Meat
   12: 1.25 gum = 63 Meat
   13: 1.0 gum = 50 Meat

   From this table, I derive the following formula for expected # of
   chewing gum needed to retrieve a worthless item:

   E(X) = ( 17 - X ) / 4
   Cost(X) = 12.5 * ( 17 - X ) Meat
  */

  public static PurchaseRequest CHEWING_GUM =
      NPCStoreDatabase.getPurchaseRequest(ItemPool.CHEWING_GUM);

  public static int currentWorthlessItemCost() {
    int x = SewerRequest.uniqueStarterItemCount();
    int gumPrice = SewerRequest.CHEWING_GUM.getPrice();
    return (int) Math.ceil((gumPrice / 4.0) * (17 - x));
  }

  // *** End of worthless item handling

  public static final boolean registerRequest(final String urlString) {
    // Call only from SingleUseRequest or MultiUseRequest

    Matcher quantityMatcher = GenericRequest.QUANTITY_PATTERN.matcher(urlString);
    int count = quantityMatcher.find() ? StringUtilities.parseInt(quantityMatcher.group(1)) : 1;

    AdventureResult gum = ItemPool.get(ItemPool.CHEWING_GUM, count);
    UseItemRequest.setLastItemUsed(gum);

    RequestLogger.updateSessionLog();
    String text = "Use " + count + " " + gum.getPluralName() + " to retrieve items from the sewer.";
    RequestLogger.updateSessionLog(text);

    return true;
  }
}
