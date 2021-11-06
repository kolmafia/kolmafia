package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.request.DisplayCaseRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.CharacterEntities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class DisplayCaseManager {
  private static final GenericRequest SHELF_REORDER = new GenericRequest("managecollection.php");

  private static final Pattern OPTION_PATTERN =
      Pattern.compile(
          "</tr><tr[^>]*><td>(.*?)(?: \\((\\d+)\\))?</td>.*?javascript:addform\\((\\d+), (\\d+)\\)");
  private static final Pattern SHELVES_PATTERN =
      Pattern.compile("<script>.*?var shelves = \\{(.*?)\\};.*?</script>", Pattern.DOTALL);
  private static final Pattern SHELF_PATTERN = Pattern.compile("\"(\\d+)\":\"([^\"]*)\"");

  private static final LockableListModel<String> headers = new LockableListModel<>();
  private static final LockableListModel<SortedListModel<AdventureResult>> shelves =
      new LockableListModel<>();

  public static boolean collectionRetrieved = false;

  public static final void clearCache() {
    KoLConstants.collection.clear();
    DisplayCaseManager.collectionRetrieved = false;
    DisplayCaseManager.headers.clear();
    DisplayCaseManager.shelves.clear();
  }

  public static final LockableListModel<String> getHeaders() {
    return DisplayCaseManager.headers;
  }

  public static final String getHeader(final int shelf) {
    return DisplayCaseManager.headers.get(shelf);
  }

  public static final LockableListModel<SortedListModel<AdventureResult>> getShelves() {
    return DisplayCaseManager.shelves;
  }

  public static final void move(
      final List<AdventureResult> moving, final int sourceShelf, final int destinationShelf) {
    DisplayCaseManager.shelves.get(sourceShelf).removeAll(moving);
    DisplayCaseManager.shelves.get(destinationShelf).addAll(moving);

    RequestThread.postRequest(
        new DisplayCaseRequest(moving.toArray(new AdventureResult[0]), destinationShelf));
    KoLmafia.updateDisplay("Display case updated.");
  }

  public static final void reorder(final String[] headers) {
    headers[0] = "-none-";

    // Unfortunately, if there are deleted shelves, the
    // shelves cannot be re-ordered directly.  What has
    // to happen is that the number of deleted shelves
    // needs to be created with some dummy name and then
    // deleted afterwards.

    boolean containsDeletedShelf = false;
    boolean[] deleted = new boolean[headers.length];

    for (int i = 0; i < headers.length; ++i) {
      deleted[i] = headers[i].equals("(Deleted Shelf)");
      containsDeletedShelf |= deleted[i];
    }

    for (int i = 0; i < deleted.length; ++i) {
      if (deleted[i]) {
        DisplayCaseManager.SHELF_REORDER.addFormField("action", "newshelf");
        DisplayCaseManager.SHELF_REORDER.addFormField("pwd");
        DisplayCaseManager.SHELF_REORDER.addFormField("shelfname", "Deleted Shelf " + i);
        RequestThread.postRequest(DisplayCaseManager.SHELF_REORDER);
      }
    }

    // Determine where the headers are in the existing
    // list of headers to find out where the shelf contents
    // should be stored after the update.

    List<SortedListModel<AdventureResult>> shelforder = new ArrayList<>();
    for (String header : headers) {
      shelforder.add(DisplayCaseManager.shelves.get(DisplayCaseManager.headers.indexOf(header)));
    }

    // Save the lists to the server and update the display
    // on the shelf to reflect the change.

    DisplayCaseManager.save(shelforder);

    // Redelete the previously deleted shelves so that the
    // user isn't stuck with shelves they aren't going to use.

    DisplayCaseManager.SHELF_REORDER.clearDataFields();
    DisplayCaseManager.SHELF_REORDER.addFormField("action", "modifyshelves");
    DisplayCaseManager.SHELF_REORDER.addFormField("pwd");

    for (int i = 1; i < headers.length; ++i) {
      DisplayCaseManager.SHELF_REORDER.addFormField("newname" + i, headers[i]);
      if (deleted[i]) {
        DisplayCaseManager.SHELF_REORDER.addFormField("delete" + i, "on");
      }
    }

    RequestThread.postRequest(DisplayCaseManager.SHELF_REORDER);
    RequestThread.postRequest(new DisplayCaseRequest());

    KoLmafia.updateDisplay("Display case updated.");
  }

  private static void save(final List<SortedListModel<AdventureResult>> shelfOrder) {
    int elementCounter = 0;
    SortedListModel<AdventureResult> currentShelf;

    // In order to ensure that all data is saved with no
    // glitches server side, all items submit their state.
    // Store the data in two parallel arrays.

    int size = KoLConstants.collection.size();
    int[] newShelves = new int[size];
    AdventureResult[] newItems = new AdventureResult[size];

    // Iterate through each shelf and place the item into
    // the parallel arrays.

    for (int i = 0; i < shelfOrder.size(); ++i) {
      currentShelf = shelfOrder.get(i);
      for (int j = 0; j < currentShelf.size(); ++j, ++elementCounter) {
        newShelves[elementCounter] = i;
        newItems[elementCounter] = currentShelf.get(j);
      }
    }

    // Once the parallel arrays are properly initialized,
    // send the update request to the server.

    RequestThread.postRequest(new DisplayCaseRequest(newItems, newShelves));
  }

  public static final void update(final String data) {
    DisplayCaseManager.updateShelves(data);

    ArrayList<AdventureResult> items = new ArrayList<>();
    List<List<AdventureResult>> shelves = new ArrayList<>();
    for (int i = 0; i < DisplayCaseManager.shelves.size(); ++i) {
      shelves.add(new ArrayList<>());
    }

    Matcher optionMatcher = DisplayCaseManager.OPTION_PATTERN.matcher(data);
    while (optionMatcher.find()) {
      int itemId = StringUtilities.parseInt(optionMatcher.group(3));
      if (ItemDatabase.getItemName(itemId) == null) {
        // Do not register new items discovered in your
        // display case, since descid is not available
        //
        // String itemName = optionMatcher.group( 1 );
        // ItemDatabase.registerItem( itemId, itemName );
        continue;
      }

      String countString = optionMatcher.group(2);
      int itemCount = countString == null ? 1 : StringUtilities.parseInt(countString);
      AdventureResult item = ItemPool.get(itemId, itemCount);

      int shelf = StringUtilities.parseInt(optionMatcher.group(4));

      items.add(item);
      shelves.get(shelf).add(item);
    }

    KoLConstants.collection.addAll(items);
    for (int i = 0; i < DisplayCaseManager.shelves.size(); ++i) {
      DisplayCaseManager.shelves.get(i).addAll(shelves.get(i));
    }

    // Finally, we can account for Golden Mr. A's in your display case
    InventoryManager.countGoldenMrAccesories();

    DisplayCaseManager.collectionRetrieved = true;
  }

  private static void updateShelves(final String data) {
    DisplayCaseManager.clearCache();

    Matcher caseMatcher = DisplayCaseManager.SHELVES_PATTERN.matcher(data);
    if (caseMatcher.find()) {
      Matcher shelfMatcher = DisplayCaseManager.SHELF_PATTERN.matcher(caseMatcher.group(1));
      while (shelfMatcher.find()) {
        int shelf = StringUtilities.parseInt(shelfMatcher.group(1));
        String name = CharacterEntities.unescape(shelfMatcher.group(2));

        for (int i = DisplayCaseManager.headers.size(); i < shelf; ++i) {
          DisplayCaseManager.headers.add("(Deleted Shelf)");
        }
        DisplayCaseManager.headers.add(name);
      }
    }

    if (DisplayCaseManager.headers.size() == 0) {
      DisplayCaseManager.headers.add("-none-");
    }

    for (int i = 0; i < DisplayCaseManager.headers.size(); ++i) {
      DisplayCaseManager.shelves.add(new SortedListModel<>());
    }
  }
}
