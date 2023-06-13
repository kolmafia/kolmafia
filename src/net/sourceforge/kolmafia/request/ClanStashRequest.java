package net.sourceforge.kolmafia.request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AdventureResult.AdventureLongCountResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ClanStashRequest extends TransferItemRequest {
  private static final Pattern LIST_PATTERN = Pattern.compile("<form name=takegoodies.*?</select>");
  private static final Pattern ITEM_PATTERN =
      Pattern.compile(
          "<option value=([\\d]+) descid=([\\d]+)>(.*?)( \\(([\\d,]+)\\))?( \\(-[\\d,]*\\))?</option>");

  private final ClanStashRequestType moveType;

  public enum ClanStashRequestType {
    REFRESH_ONLY,
    ITEMS_TO_STASH,
    MEAT_TO_STASH,
    STASH_TO_ITEMS
  }

  public ClanStashRequest() {
    super("clan_stash.php");
    this.moveType = ClanStashRequestType.REFRESH_ONLY;
    this.destination = new ArrayList<>();
  }

  /**
   * Constructs a new <code>ClanStashRequest</code>.
   *
   * @param amount The amount of meat involved in this transaction
   */
  public ClanStashRequest(final long amount) {
    super("clan_stash.php", new AdventureLongCountResult(AdventureResult.MEAT, amount));
    this.addFormField("action", "contribute");

    this.moveType = ClanStashRequestType.MEAT_TO_STASH;
    this.destination = new ArrayList<>();
  }

  public ClanStashRequest(AdventureResult attachment, final ClanStashRequestType moveType) {
    this(new AdventureResult[] {attachment}, moveType);
  }

  /**
   * Constructs a new <code>ClanStashRequest</code>.
   *
   * @param attachments The list of attachments involved in the request
   */
  public ClanStashRequest(
      final AdventureResult[] attachments, final ClanStashRequestType moveType) {
    super("clan_stash.php", attachments);
    this.moveType = moveType;

    if (moveType == ClanStashRequestType.ITEMS_TO_STASH) {
      this.addFormField("action", "addgoodies");
      this.source = KoLConstants.inventory;
      this.destination = ClanManager.getStash();
    } else {
      this.addFormField("action", "takegoodies");
      this.source = ClanManager.getStash();
      this.destination = KoLConstants.inventory;
    }
    this.addFormField("ajax", "1");
  }

  @Override
  protected boolean retryOnTimeout() {
    return this.moveType == ClanStashRequestType.REFRESH_ONLY;
  }

  @Override
  public boolean sendEmpty() {
    return this.moveType == ClanStashRequestType.REFRESH_ONLY;
  }

  @Override
  public String getItemField() {
    return this.moveType == ClanStashRequestType.ITEMS_TO_STASH ? "item" : "whichitem";
  }

  @Override
  public String getQuantityField() {
    return this.moveType == ClanStashRequestType.ITEMS_TO_STASH ? "qty" : "quantity";
  }

  @Override
  public String getMeatField() {
    return "howmuch";
  }

  public ClanStashRequestType getMoveType() {
    return this.moveType;
  }

  public List<AdventureResult> getItems() {
    List<AdventureResult> itemList = new ArrayList<>();

    if (this.attachments == null) {
      return itemList;
    }

    Collections.addAll(itemList, this.attachments);

    return itemList;
  }

  @Override
  public int getCapacity() {
    return this.moveType == ClanStashRequestType.STASH_TO_ITEMS ? 1 : 11;
  }

  @Override
  public TransferItemRequest getSubInstance(final AdventureResult[] attachments) {
    return new ClanStashRequest(attachments, this.moveType);
  }

  @Override
  public void processResults() {
    super.processResults();

    switch (this.moveType) {
      case REFRESH_ONLY:
        KoLmafia.updateDisplay("Stash list retrieved.");
        return;

      case MEAT_TO_STASH:
        KoLmafia.updateDisplay("Clan donation attempt complete.");
        break;

      case STASH_TO_ITEMS:
      case ITEMS_TO_STASH:
        if (!KoLmafia.permitsContinue()) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "Movement of items failed.");
        }

        break;
    }
  }

  @Override
  public boolean parseTransfer() {
    return ClanStashRequest.parseTransfer(this.getURLString(), this.responseText);
  }

  public static final Pattern ITEM_PATTERN1 = Pattern.compile("You add (.*?) to the Goodies Hoard");
  public static final Pattern ITEM_PATTERN2 =
      Pattern.compile("(\\d+) (.+?)(?:, (?=\\d)|, and| and (?=\\d)|$)");

  public static boolean parseTransfer(final String urlString, final String responseText) {
    if (urlString.contains("takegoodies")) {
      // If you ask for too many of an item:
      //     There aren't that many of that item in the stash.
      //
      // If you ask for (and are allowed to take) items:
      //     You acquire 5 xxx
      if (!responseText.contains("You acquire")) {
        return false;
      }

      // Since "you acquire" items, they have already been
      // added to inventory
      TransferItemRequest.transferItems(
          urlString,
          TransferItemRequest.ITEMID_PATTERN,
          TransferItemRequest.QUANTITY_PATTERN,
          ClanManager.getStash(),
          null,
          0);
    } else if (urlString.contains("addgoodies")) {
      // If you didn't have the items you wanted to drop into
      // the stash:
      //     You didn't actually have any of the items you
      //     selected. Tsk, tsk.
      // Otherwise:
      //     You add 5 xxx to the Goodies Hoard.
      if (!responseText.contains("to the Goodies Hoard")) {
        return false;
      }

      // Parse the actual number of items moved from the
      // responseText, rather than believing the URL
      List<AdventureResult> items =
          TransferItemRequest.getItemList(responseText, ITEM_PATTERN1, null, ITEM_PATTERN2);
      TransferItemRequest.transferItems(items, KoLConstants.inventory, ClanManager.getStash());
    } else if (urlString.contains("action=contribute")) {
      long meat = TransferItemRequest.transferredMeat(urlString, "howmuch");
      ResultProcessor.processMeat(-meat);
      KoLCharacter.updateStatus();
    }

    if (urlString.contains("ajax=1")) {
      return true;
    }

    ClanStashRequest.parseStash(responseText);
    return true;
  }

  private static void parseStash(final String responseText) {
    // In the event that the request was broken up into pieces,
    // there's nothing to look at. Return from the function call.

    if (responseText == null || responseText.length() == 0) {
      return;
    }

    ClanManager.setStashRetrieved();

    // Start with current stash contents
    LockableListModel<AdventureResult> stashContents = ClanManager.getStash();

    // Clear it
    stashContents.clear();

    // If there's nothing inside the goodies hoard, clear stash and return
    Matcher stashMatcher = ClanStashRequest.LIST_PATTERN.matcher(responseText);
    if (!stashMatcher.find()) {
      return;
    }

    ArrayList<AdventureResult> items = new ArrayList<>();

    Matcher matcher = ClanStashRequest.ITEM_PATTERN.matcher(stashMatcher.group());
    int lastFindIndex = 0;

    while (matcher.find(lastFindIndex)) {
      lastFindIndex = matcher.end();
      int itemId = StringUtilities.parseInt(matcher.group(1));
      String descId = matcher.group(2);
      String itemName = matcher.group(3).trim();

      String quantityString = matcher.group(5);
      int quantity = 1;

      if (quantityString != null) {
        quantity = StringUtilities.parseInt(quantityString);
      }

      // If this is a previously unknown item, register it.
      if (ItemDatabase.getItemName(itemId) == null) {
        ItemDatabase.registerItem(itemId, itemName, descId);
      }

      items.add(ItemPool.get(itemId, quantity));
    }

    // Add everything en masse to the stash
    stashContents.addAll(items);
  }

  @Override
  public boolean allowMementoTransfer() {
    return true;
  }

  @Override
  public boolean allowUntradeableTransfer() {
    return true;
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("clan_stash.php")) {
      return false;
    }

    if (urlString.contains("takegoodies")) {
      return TransferItemRequest.registerRequest(
          "remove from stash",
          urlString,
          TransferItemRequest.ITEMID_PATTERN,
          TransferItemRequest.QUANTITY_PATTERN,
          ClanManager.getStash(),
          0);
    }

    if (urlString.contains("addgoodies")) {
      return TransferItemRequest.registerRequest(
          "add to stash",
          urlString,
          TransferItemRequest.ITEMID_PATTERN,
          TransferItemRequest.QTY_PATTERN,
          KoLConstants.inventory,
          0);
    }

    if (urlString.contains("action=contribute")) {
      long meat = TransferItemRequest.transferredMeat(urlString, "howmuch");
      String message = "add to stash: " + meat + " Meat";
      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog(message);
    }

    return true;
  }

  @Override
  public String getStatusMessage() {
    return switch (this.moveType) {
      case ITEMS_TO_STASH -> "Dropping items into stash";
      case STASH_TO_ITEMS -> "Pulling items from stash";
      case MEAT_TO_STASH -> "Donating meat to stash";
      default -> "Refreshing stash contents";
    };
  }
}
