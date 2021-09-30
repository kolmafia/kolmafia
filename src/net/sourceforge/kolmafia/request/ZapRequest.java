package net.sourceforge.kolmafia.request;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.BooleanArray;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ZapRequest extends GenericRequest {
  private static final Pattern ZAP_PATTERN = Pattern.compile("whichitem=(\\d+)");
  private static final Pattern OPTION_PATTERN =
      Pattern.compile("<option value=(\\d+) descid='.*?'>.*?</option>");

  private static final BooleanArray isZappable = new BooleanArray();
  private static final SortedListModel<AdventureResult> zappableItems =
      new SortedListModel<AdventureResult>();
  private static final Map<Integer, String[]> zapGroups = new HashMap<>();

  private AdventureResult item;

  public ZapRequest(final AdventureResult item) {
    super("wand.php");

    this.item = null;

    if (KoLCharacter.getZapper() == null) {
      return;
    }

    this.item = item;

    this.addFormField("action", "zap");
    this.addFormField("whichwand", String.valueOf(KoLCharacter.getZapper().getItemId()));
    this.addFormField("whichitem", String.valueOf(item.getItemId()));
  }

  private static void initializeList() {
    if (!ZapRequest.zappableItems.isEmpty()) {
      return;
    }

    try {
      String line;
      BufferedReader reader =
          FileUtilities.getVersionedReader("zapgroups.txt", KoLConstants.ZAPGROUPS_VERSION);

      while ((line = FileUtilities.readLine(reader)) != null) {
        String[] list = line.split("\\s*,\\s*");
        for (int i = 0; i < list.length; ++i) {
          String name = list[i];
          int itemId = ItemDatabase.getItemId(name, 1, false);
          if (itemId < 0) {
            RequestLogger.printLine("Unknown item in zap group: " + name);
            continue;
          }
          ZapRequest.zappableItems.add(ItemPool.get(itemId));
          ZapRequest.isZappable.set(itemId, true);
          ZapRequest.zapGroups.put(IntegerPool.get(itemId), list);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static final LockableListModel<AdventureResult> getZappableItems() {
    ZapRequest.initializeList();

    SortedListModel<AdventureResult> matchingItems = new SortedListModel<AdventureResult>();
    matchingItems.addAll(KoLConstants.inventory);
    if (Preferences.getBoolean("relayTrimsZapList")) {
      matchingItems.retainAll(ZapRequest.zappableItems);
    }
    return matchingItems;
  }

  public static final String[] getZapGroup(int itemId) {
    ZapRequest.initializeList();

    String[] rv = ZapRequest.zapGroups.get(IntegerPool.get(itemId));
    if (rv == null) return new String[0];
    return rv;
  }

  @Override
  public void run() {
    if (this.item == null) {
      return;
    }

    if (KoLCharacter.getZapper() == null) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You don't have a wand.");
      return;
    }

    if (InventoryManager.hasItem(this.item, true)) {
      InventoryManager.retrieveItem(this.item);
    }

    if (!KoLConstants.inventory.contains(this.item)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You don't have a " + this.item.getName() + ".");
      return;
    }

    KoLmafia.updateDisplay("Zapping " + this.item.getName() + "...");
    super.run();
  }

  @Override
  public void processResults() {
    // Remove item if zap succeeded. Remove wand if it blew up.
    ZapRequest.parseResponse(this.getURLString(), this.responseText);

    // "The Crown of the Goblin King shudders for a moment, but
    // nothing happens."
    if (this.responseText.indexOf("nothing happens") != -1) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "The " + this.item.getName() + " is not zappable.");
      return;
    }

    // Notify the user of success.
    KoLmafia.updateDisplay(this.item.getName() + " has been transformed.");
  }

  public static final void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("wand.php")) {
      return;
    }

    if (responseText.indexOf("nothing happens") != -1) {
      return;
    }

    // If it blew up, remove wand and zero usages
    if (responseText.indexOf("abruptly explodes") != -1) {
      ResultProcessor.processResult(KoLCharacter.getZapper().getNegation());
      // set to -1 because will be incremented below
      Preferences.setInteger("_zapCount", -1);
    }

    Matcher itemMatcher = ZapRequest.ZAP_PATTERN.matcher(urlString);
    if (!itemMatcher.find()) {
      return;
    }

    // Remove the item which was transformed.
    int itemId = StringUtilities.parseInt(itemMatcher.group(1));
    ResultProcessor.removeItem(itemId);

    // increment zap count
    Preferences.increment("_zapCount");
  }

  public static final void decorate(final StringBuffer buffer) {
    // Don't trim the list if user wants to see all items
    if (!Preferences.getBoolean("relayTrimsZapList")) return;

    ZapRequest.initializeList();

    int selectIndex = buffer.indexOf("<select");
    if (selectIndex == -1) return;
    selectIndex = buffer.indexOf(">", selectIndex) + 1;
    int endSelectIndex = buffer.indexOf("</select>", selectIndex);
    Matcher optionMatcher =
        ZapRequest.OPTION_PATTERN.matcher(buffer.substring(selectIndex, endSelectIndex));
    buffer.delete(selectIndex, endSelectIndex);

    int itemId;
    StringBuffer zappableOptions = new StringBuffer();
    while (optionMatcher.find()) {
      itemId = Integer.parseInt(optionMatcher.group(1));
      if (itemId == 0 || ZapRequest.isZappable.get(itemId)) {
        zappableOptions.append(optionMatcher.group());
      }
    }

    buffer.insert(selectIndex, zappableOptions.toString());
    int pos = buffer.lastIndexOf("</center>");
    buffer.insert(
        pos,
        "KoLmafia trimmed this list to the items it knows to be zappable, which may not include recently discovered or modified items.  <a href=\"wand.php?whichwand="
            + KoLCharacter.getZapper().getItemId()
            + "&notrim=1\">Click here</a> for the full list.");
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("wand.php")) {
      return false;
    }

    Matcher itemMatcher = ZapRequest.ZAP_PATTERN.matcher(urlString);
    if (!itemMatcher.find()) {
      return true;
    }

    int itemId = StringUtilities.parseInt(itemMatcher.group(1));
    AdventureResult item = ItemPool.get(itemId);

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog("zap " + item.getName());

    return true;
  }
}
