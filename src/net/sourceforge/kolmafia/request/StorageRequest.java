package net.sourceforge.kolmafia.request;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AdventureResult.AdventureLongCountResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.json.JSONException;
import org.json.JSONObject;

public class StorageRequest extends TransferItemRequest {
  private int moveType;
  private boolean bulkTransfer;

  public static final Set<Integer> roninStoragePulls = new HashSet<>();

  // Public interface
  public static void resetRoninStoragePulls() {
    roninStoragePulls.clear();
  }

  public static void loadRoninStoragePulls() {
    if (KoLCharacter.inRonin()) {
      Set<Integer> set = roninStoragePulls;
      String value = Preferences.getString("_roninStoragePulls");
      pullsStringToSet(value, set);
    }
  }

  public static void saveRoninStoragePulls() {
    if (KoLCharacter.inRonin()) {
      Set<Integer> set = roninStoragePulls;
      String value = pullsSetToString(set);
      Preferences.setString("_roninStoragePulls", value);
    }
  }

  public static void addPulledItem(AdventureResult item) {
    addPulledItem(item.getItemId());
  }

  public static void addPulledItem(int itemId) {
    if (KoLCharacter.inRonin()) {
      Set<Integer> set = roninStoragePulls;
      addPulledItem(set, itemId);
      saveRoninStoragePulls();
    }
  }

  public static boolean itemPulledInRonin(AdventureResult item) {
    return itemPulledInRonin(item.getItemId());
  }

  public static boolean itemPulledInRonin(int itemId) {
    return KoLCharacter.inRonin() && itemPulledInRonin(roninStoragePulls, itemId);
  }

  // Testable underbelly

  public static void pullsStringToSet(String value, Set<Integer> set) {
    set.clear();
    for (String val : value.split("\\s*,\\s*")) {
      if (StringUtilities.isNumeric(val)) {
        set.add(StringUtilities.parseInt(val));
      }
    }
  }

  public static String pullsSetToString(Set<Integer> set) {
    return set.stream().map(String::valueOf).collect(Collectors.joining(","));
  }

  public static void addPulledItem(Set<Integer> set, int itemId) {
    set.add(itemId);
  }

  public static boolean itemPulledInRonin(Set<Integer> set, int itemId) {
    return set.contains(itemId);
  }

  public static final int REFRESH = 0;
  public static final int EMPTY_STORAGE = 1;
  public static final int STORAGE_TO_INVENTORY = 2;
  public static final int PULL_MEAT_FROM_STORAGE = 3;

  public static void refresh() {
    // To refresh storage, we get Meat and pulls from the main page
    // and items from api.php

    RequestThread.postRequest(new StorageRequest(REFRESH));
    ApiRequest.updateStorage();
    StorageRequest.updateSettings();
  }

  public static void emptyStorage() {
    RequestThread.postRequest(new StorageRequest(EMPTY_STORAGE));
  }

  public static final void parseStorage(final JSONObject JSON) {
    if (JSON == null) {
      return;
    }

    ArrayList<AdventureResult> items = new ArrayList<>();
    ArrayList<AdventureResult> freepulls = new ArrayList<>();
    ArrayList<AdventureResult> nopulls = new ArrayList<>();

    try {
      // {"1":"1","2":"1" ... }
      Iterator<String> keys = JSON.keys();

      while (keys.hasNext()) {
        String key = keys.next();
        int itemId = StringUtilities.parseInt(key);
        int count = JSON.getInt(key);
        String name = ItemDatabase.getItemDataName(itemId);
        if (name == null) {
          // api.php?what=item does not work for
          // items in storage:
          // "You don't own that item."
          // ItemDatabase.registerItem( itemId );
          continue;
        }

        AdventureResult item = ItemPool.get(itemId, count);
        List<AdventureResult> list =
            KoLCharacter.canInteract()
                ? items
                : StorageRequest.isFreePull(item)
                    ? freepulls
                    : StorageRequest.isNoPull(item) ? nopulls : items;
        list.add(item);
      }
    } catch (JSONException e) {
      ApiRequest.reportParseError("storage", JSON.toString(), e);
      return;
    }

    KoLConstants.storage.clear();
    KoLConstants.storage.addAll(items);

    KoLConstants.freepulls.clear();
    KoLConstants.freepulls.addAll(freepulls);

    KoLConstants.nopulls.clear();
    KoLConstants.nopulls.addAll(nopulls);

    if (InventoryManager.canUseStorage()) {
      ConcoctionDatabase.refreshConcoctions();
    }
  }

  public StorageRequest() {
    super("storage.php");
    this.moveType = StorageRequest.REFRESH;
  }

  public StorageRequest(final int moveType) {
    this(moveType, new AdventureResult[0]);
    this.moveType = moveType;
  }

  public StorageRequest(final int moveType, final long amount) {
    this(moveType, new AdventureLongCountResult(AdventureResult.MEAT, amount));
  }

  public StorageRequest(final int moveType, final AdventureResult attachment) {
    this(moveType, new AdventureResult[] {attachment});
  }

  public StorageRequest(final int moveType, final AdventureResult[] attachments) {
    this(moveType, attachments, false);
  }

  public StorageRequest(
      final int moveType, final AdventureResult[] attachments, final boolean bulkTransfer) {
    super("storage.php", attachments);
    this.moveType = moveType;
    this.bulkTransfer = bulkTransfer;

    // Figure out the actual URL information based on the
    // different request types.

    switch (moveType) {
      case REFRESH -> this.addFormField("which", "5");
      case EMPTY_STORAGE -> {
        this.addFormField("action", "pullall");
        this.source = KoLConstants.storage;
        this.destination = KoLConstants.inventory;
      }
      case STORAGE_TO_INVENTORY -> {
        // storage.php?action=pull&whichitem1=1649&howmany1=1&pwd
        this.addFormField("action", "pull");
        this.addFormField("ajax", "1");
        this.source = KoLConstants.storage;
        this.destination = KoLConstants.inventory;
      }
      case PULL_MEAT_FROM_STORAGE -> this.addFormField("action", "takemeat");
    }
  }

  @Override
  protected boolean retryOnTimeout() {
    return this.moveType == StorageRequest.REFRESH;
  }

  public int getMoveType() {
    return this.moveType;
  }

  @Override
  public String getItemField() {
    return "whichitem";
  }

  @Override
  public String getQuantityField() {
    return "howmany";
  }

  @Override
  public String getMeatField() {
    return "amt";
  }

  public List<AdventureResult> getItems() {
    List<AdventureResult> itemList = new ArrayList<>();

    if (this.attachments == null) {
      return itemList;
    }

    for (AdventureResult item : this.attachments) {
      itemList.add(item);
    }

    return itemList;
  }

  @Override
  public int getCapacity() {
    return 11;
  }

  @Override
  public boolean forceGETMethod() {
    return this.moveType == STORAGE_TO_INVENTORY;
  }

  @Override
  public ArrayList<TransferItemRequest> generateSubInstances() {
    ArrayList<TransferItemRequest> subinstances = new ArrayList<>();

    if (KoLmafia.refusesContinue()) {
      return subinstances;
    }

    int capacity = this.getCapacity();
    long meatAttachment = 0;
    boolean inRonin = KoLCharacter.inRonin();

    // If we are in Ronin:
    //
    // - We can pull as many free-pull items as we want, but one at a time
    // - We can only pull one per day of non-free-pull items.

    List<AdventureResult> nextAttachments = new ArrayList<>();
    for (AdventureResult item : this.attachments) {
      // Add up all requested Meat
      if (item.isMeat()) {
        meatAttachment += item.getLongCount();
        continue;
      }

      // See how many of the requested item are available
      int availableCount =
          item.getCount(KoLConstants.storage) + item.getCount(KoLConstants.freepulls);
      if (availableCount <= 0) {
        continue;
      }

      // If we are in Ronin, skip items that we have already pulled today.
      if (inRonin && itemPulledInRonin(item)) {
        KoLmafia.updateDisplay(
            "You've already pulled one '" + item.getName() + "' today. Skipping...");
        continue;
      }

      int desiredCount = Math.min(availableCount, item.getCount());
      if (inRonin) {
        // Free Pulls have to go into requests by themself
        if (StorageRequest.isFreePull(item)) {
          // You are allowed to pull as many of this item as you want, but
          // only one per request and not mixed with other items
          if (!nextAttachments.isEmpty()) {
            subinstances.add(this.getSubInstance(nextAttachments));
            nextAttachments.clear();
          }

          while (desiredCount-- > 0) {
            nextAttachments.add(item.getInstance(1));
            subinstances.add(this.getSubInstance(nextAttachments));
            nextAttachments.clear();
          }
          continue;
        }

        if (desiredCount > 1) {
          KoLmafia.updateDisplay("You can only pull one '" + item.getName() + "' today...");
          desiredCount = 1;
        }
        // Fall through to add the single item.
      }

      nextAttachments.add(item.getInstance(desiredCount));

      if (nextAttachments.size() == capacity) {
        subinstances.add(this.getSubInstance(nextAttachments));
        nextAttachments.clear();
      }
    }

    if (!nextAttachments.isEmpty()) {
      subinstances.add(this.getSubInstance(nextAttachments));
    }

    if (subinstances.size() == 0) {
      // This can only happen if we are sending no items
      this.isSubInstance = true;
      subinstances.add(this);
    }

    if (meatAttachment > 0) {
      // Attach all the Meat to the first request
      TransferItemRequest first = subinstances.get(0);
      first.addFormField(this.getMeatField(), String.valueOf(meatAttachment));
    }

    return subinstances;
  }

  private TransferItemRequest getSubInstance(List<AdventureResult> attachments) {
    return this.getSubInstance(attachments.toArray(new AdventureResult[0]));
  }

  @Override
  public TransferItemRequest getSubInstance(final AdventureResult[] attachments) {
    TransferItemRequest subInstance =
        new StorageRequest(this.moveType, attachments, this.bulkTransfer);
    subInstance.isSubInstance = true;
    return subInstance;
  }

  @Override
  public void run() {
    if (KoLCharacter.isHardcore()) {
      switch (this.moveType) {
        case EMPTY_STORAGE:
          KoLmafia.updateDisplay(MafiaState.ERROR, "You cannot empty storage while in Hardcore.");
          return;
        case PULL_MEAT_FROM_STORAGE:
          KoLmafia.updateDisplay(
              MafiaState.ERROR, "You cannot remove meat from storage while in Hardcore.");
          return;
        case STORAGE_TO_INVENTORY:
          for (AdventureResult attachment : this.attachments) {
            if (attachment == null) {
              continue;
            }

            if (!StorageRequest.isFreePull(attachment)) {
              KoLmafia.updateDisplay(
                  MafiaState.ERROR, "You cannot pull a " + attachment.getName() + " in Hardcore.");
              return;
            }
          }
          break;
      }
    }

    if (KoLCharacter.inRonin()) {
      switch (this.moveType) {
        case EMPTY_STORAGE:
          KoLmafia.updateDisplay(MafiaState.ERROR, "You cannot empty storage while in Ronin.");
          return;
        case STORAGE_TO_INVENTORY:
          // Filter out items that you've already pulled today
          for (int index = 0; index < this.attachments.length; ++index) {
            AdventureResult item = this.attachments[index];
            if (item != null && item.isItem() && itemPulledInRonin(item)) {
              KoLmafia.updateDisplay(
                  "You've already pulled one '" + item.getName() + "' today. Skipping...");
              this.attachments[index] = null;
            }
          }
          break;
      }
    }

    switch (this.moveType) {
      case PULL_MEAT_FROM_STORAGE:
        if (KoLCharacter.inFistcore()) {
          KoLmafia.updateDisplay(
              MafiaState.ERROR, "You cannot remove meat from storage while in Fistcore.");
          return;
        }
        break;
      case STORAGE_TO_INVENTORY:
        boolean nonNullItems = false;
        for (AdventureResult attachment : this.attachments) {
          if (attachment != null) {
            nonNullItems = true;
            break;
          }
        }
        if (!nonNullItems) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "No items could be removed from storage.");
          return;
        }
        break;
    }

    // Let TransferItemRequest handle it
    super.run();
  }

  @Override
  public void processResults() {
    switch (this.moveType) {
      case StorageRequest.REFRESH -> {
        StorageRequest.parseStorage(this.getURLString(), this.responseText);
        return;
      }
      default -> super.processResults();
    }
  }

  // <b>You have 178,634,761 meat in long-term storage.</b>
  private static final Pattern STORAGEMEAT_PATTERN =
      Pattern.compile("<b>You have ([\\d,]+) meat in long-term storage.</b>");
  private static final Pattern STORAGEMEAT_FIST_PATTERN =
      Pattern.compile("thinking about the ([\\d,]+) you currently have");

  private static final Pattern PULLS_PATTERN =
      Pattern.compile("<span class=\"pullsleft\">(\\d+)</span>");

  private static void parseStorage(final String urlString, final String responseText) {
    if (!urlString.startsWith("storage.php")) {
      return;
    }

    // On the main page - which=5 - Hagnk tells you how much meat
    // you have in storage and how many pulls you have remaining.
    //
    // These data do not appear on the three item pages, and items
    // do not appear on page 5.

    if (!urlString.contains("which=5")) {
      return;
    }

    Matcher meatInStorageMatcher =
        KoLCharacter.inFistcore()
            ? StorageRequest.STORAGEMEAT_FIST_PATTERN.matcher(responseText)
            : StorageRequest.STORAGEMEAT_PATTERN.matcher(responseText);
    if (meatInStorageMatcher.find()) {
      long meat = StringUtilities.parseLong(meatInStorageMatcher.group(1));
      KoLCharacter.setStorageMeat(meat);
    } else if (responseText.contains("Hagnk doesn't have any of your meat")) {
      KoLCharacter.setStorageMeat(0);
    }

    Matcher pullsMatcher = StorageRequest.PULLS_PATTERN.matcher(responseText);
    if (pullsMatcher.find()) {
      ConcoctionDatabase.setPullsRemaining(StringUtilities.parseInt(pullsMatcher.group(1)));
    } else if (KoLCharacter.isHardcore() || !KoLCharacter.canInteract()) {
      ConcoctionDatabase.setPullsRemaining(0);
    } else {
      ConcoctionDatabase.setPullsRemaining(-1);
    }

    return;
  }

  public static boolean isFreePull(final AdventureResult item) {
    // For now, special handling for the few items which are a free
    // pull only for a specific path. If more path-specific free
    // pulls are introduced, we'll define a "Free Pull Path"
    // modifier or something.

    int itemId = item.getItemId();

    if ((itemId == ItemPool.BORIS_HELM || itemId == ItemPool.BORIS_HELM_ASKEW)
        && !KoLCharacter.inAxecore()) {
      return false;
    }

    if ((itemId == ItemPool.JARLS_COSMIC_PAN || itemId == ItemPool.JARLS_PAN)
        && !KoLCharacter.isJarlsberg()) {
      return false;
    }

    if ((itemId == ItemPool.PETE_JACKET || itemId == ItemPool.PETE_JACKET_COLLAR)
        && !KoLCharacter.isSneakyPete()) {
      return false;
    }

    return Modifiers.getBooleanModifier(ModifierType.ITEM, itemId, "Free Pull");
  }

  public static boolean isNoPull(final AdventureResult item) {
    return Modifiers.getBooleanModifier(ModifierType.ITEM, item.getItemId(), "No Pull");
  }

  @Override
  public boolean parseTransfer() {
    return StorageRequest.parseTransfer(this.getURLString(), this.responseText, this.bulkTransfer);
  }

  public static final boolean parseTransfer(final String urlString, final String responseText) {
    return StorageRequest.parseTransfer(urlString, responseText, false);
  }

  private static final Pattern ICHOR_PATTERN = Pattern.compile("iqty=([\\d,]+)");

  public static final int ichorQuantity(final String urlString) {
    Matcher matcher = ICHOR_PATTERN.matcher(urlString);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
  }

  private static boolean parseTransfer(
      final String urlString, final String responseText, final boolean bulkTransfer) {
    String action = GenericRequest.getAction(urlString);
    if (action == null) {
      StorageRequest.parseStorage(urlString, responseText);
      return true;
    }

    if (action.equals("tossichor")) {
      // So generous, contributing 0 ichor to save the Kingdom.
      // You don't have that much ichor. Good Try!
      // You toss the ichor into the fissure and hear a distant voice, "Thanks a lot! We can save
      // the Kingdom!"
      if (responseText.contains("You toss the ichor into the fissure")) {
        int ichor = StorageRequest.ichorQuantity(urlString);
        ResultProcessor.processResult(ItemPool.get(ItemPool.ELDRITCH_ICHOR, -ichor));
      }
      return true;
    }

    if (action.equals("pullall")) {
      // Hagnk leans back and yells something
      // ugnigntelligible to a group of Knob Goblin teegnage
      // delignquegnts, who go and grab all of your stuff
      // from storage and bring it to you.

      if (responseText.contains("go and grab all of your stuff")) {
        StorageRequest.emptyStorage(urlString);
        KoLCharacter.updateStatus();
      }

      return true;
    }

    boolean transfer = false;

    if (action.equals("takemeat")) {
      if (responseText.contains("Meat out of storage")) {
        StorageRequest.transferMeat(urlString);
        transfer = true;
      }
    } else if (action.equals("pull")) {
      if (responseText.contains("moved from storage to inventory")) {
        // Pull items from storage and/or freepulls
        StorageRequest.transferItems(urlString, responseText, bulkTransfer);
        transfer = true;
      }
    }

    if (!urlString.contains("ajax=1")) {
      StorageRequest.parseStorage(urlString, responseText);
    }

    StorageRequest.updateSettings();

    if (transfer) {
      KoLCharacter.updateStatus();
    }

    return true;
  }

  public static final void emptyStorage(final String urlString) {
    KoLConstants.storage.clear();
    KoLConstants.freepulls.clear();
    KoLCharacter.setAvailableMeat(KoLCharacter.getStorageMeat() + KoLCharacter.getAvailableMeat());
    KoLCharacter.setStorageMeat(0);

    // Doing a "pull all" in Hagnk's does not tell
    // you what went into inventory and what went
    // into the closet.

    InventoryManager.refresh();
    ClosetRequest.refresh();
    NamedListenerRegistry.fireChange("(coinmaster)");

    // If we are still in a Trendy run or are pulling only
    // "favorite things", we may have left items in storage.

    if (KoLCharacter.isTrendy()
        || KoLCharacter.getRestricted()
        || urlString.contains("favonly=1")) {
      StorageRequest.refresh();
    }

    // Update settings
    StorageRequest.updateSettings();
  }

  private static void updateSettings() {
    if (KoLConstants.storage.isEmpty()
        && KoLConstants.freepulls.isEmpty()
        && KoLCharacter.getStorageMeat() == 0) {
      Preferences.setInteger("lastEmptiedStorage", KoLCharacter.getAscensions());
    } else if (Preferences.getInteger("lastEmptiedStorage") == KoLCharacter.getAscensions()) {
      // Storage is not empty, but we erroneously thought it was
      Preferences.setInteger("lastEmptiedStorage", -1);
    }
  }

  // <b>star hat (1)</b> moved from storage to inventory.
  private static final Pattern URL_ITEM_PATTERN = Pattern.compile("whichitem\\d+=(\\d+)");
  private static final Pattern PULL_ITEM_PATTERN =
      Pattern.compile(
          "(You already pulled one of those today|<b>([^<]*) \\((\\d+)\\)</b> moved from storage to inventory)");

  public static void transferItems(
      final String urlString, final String responseText, final boolean bulkTransfer) {

    // In Ronin, if you attempt to pull an item you've already pulled today,
    // KoL simply reports (without identifying the item):
    //
    // You already pulled one of those today.

    // Items that are "free pulls" are not limited in Ronin, although
    // you must pull them one at a time.

    // We will walk the URL and the responseText in parallel.
    // The itemId comes from the URL string
    // The count comes from the responseText

    Matcher matcher1 = StorageRequest.URL_ITEM_PATTERN.matcher(urlString);
    Matcher matcher2 = StorageRequest.PULL_ITEM_PATTERN.matcher(responseText);

    // Transfer items from storage and/or freepulls
    ArrayList<AdventureResult> list = bulkTransfer ? new ArrayList<>() : null;
    int pulls = 0;

    while (matcher1.find() && matcher2.find()) {
      int itemId = StringUtilities.parseInt(matcher1.group(1));
      boolean failed = matcher2.group(0).equals("You already pulled one of those today");
      int count = failed ? 0 : StringUtilities.parseInt(matcher2.group(3));

      AdventureResult item = ItemPool.get(itemId, count);

      if (KoLCharacter.inRonin() && !StorageRequest.isFreePull(item)) {
        addPulledItem(item);
      }

      if (failed) {
        continue;
      }

      List<AdventureResult> source;

      if (KoLConstants.freepulls.contains(item)) {
        source = KoLConstants.freepulls;
      } else {
        source = KoLConstants.storage;
        pulls += count;
      }

      // Remove from storage or freepulls list
      AdventureResult.addResultToList(source, item.getNegation());

      if (bulkTransfer) {
        list.add(item);
      } else {
        // Add to inventory
        ResultProcessor.processResult(item);
      }
    }

    if (pulls == 0) {
      return;
    }

    if (bulkTransfer) {
      StorageRequest.processBulkItems(list);
    }

    // If remaining is -1, pulls are unlimited.
    int remaining = ConcoctionDatabase.getPullsRemaining();
    if (remaining >= pulls) {
      ConcoctionDatabase.setPullsRemaining(remaining - pulls);
    }
  }

  private static void transferMeat(final String urlString) {
    long meat = TransferItemRequest.transferredMeat(urlString, "amt");
    KoLCharacter.setStorageMeat(KoLCharacter.getStorageMeat() - meat);
    ResultProcessor.processMeat(meat);

    // If remaining is -1, pulls are unlimited.
    int remaining = ConcoctionDatabase.getPullsRemaining();
    long pulls = (meat + 999) / 1000;
    if (pulls > 0 && remaining >= pulls) {
      ConcoctionDatabase.setPullsRemaining((int) (remaining - pulls));
    }
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("storage.php")) {
      return false;
    }

    if (urlString.contains("action=pullall")) {
      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog("Emptying storage");
      return true;
    }

    if (urlString.contains("action=tossichor")) {
      int ichor = StorageRequest.ichorQuantity(urlString);
      if (ichor > 0) {
        RequestLogger.updateSessionLog();
        RequestLogger.updateSessionLog("Toss " + ichor + " eldritch ichor into the fissure");
      }
      return true;
    }

    if (urlString.contains("action=takemeat")) {
      long meat = TransferItemRequest.transferredMeat(urlString, "amt");
      String message = "pull: " + meat + " Meat";

      if (meat > 0) {
        RequestLogger.updateSessionLog();
        RequestLogger.updateSessionLog(message);
      }

      return true;
    }

    if (urlString.contains("pull")) {
      return TransferItemRequest.registerRequest("pull", urlString, KoLConstants.storage, 0);
    }

    return true;
  }

  @Override
  public boolean allowMementoTransfer() {
    return true;
  }

  @Override
  public boolean allowUntradeableTransfer() {
    return true;
  }

  @Override
  public boolean allowUngiftableTransfer() {
    return true;
  }

  @Override
  public String getStatusMessage() {
    return switch (this.moveType) {
      case REFRESH -> "Examining Meat and pulls in storage";
      case EMPTY_STORAGE -> "Emptying storage";
      case STORAGE_TO_INVENTORY -> "Pulling items from storage";
      case PULL_MEAT_FROM_STORAGE -> "Pulling meat from storage";
      default -> "Unknown request type";
    };
  }

  /**
   * Handle lots of items being received at once, deferring updates to the end as much as possible.
   */
  private static void processBulkItems(List<AdventureResult> items) {
    if (items.size() == 0) {
      return;
    }

    if (RequestLogger.isDebugging()) {
      RequestLogger.updateDebugLog("Processing bulk items");
    }

    KoLmafia.updateDisplay("Processing bulk items...");

    for (AdventureResult result : items) {
      AdventureResult.addResultToList(KoLConstants.inventory, result);
      EquipmentManager.processResult(result);
    }

    // Assume that at least one item in the list requires this update

    NamedListenerRegistry.fireChange("(coinmaster)");

    KoLmafia.updateDisplay("Processing complete.");
  }
}
