package net.sourceforge.kolmafia.request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AdventureResult.AdventureLongCountResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.json.JSONException;
import org.json.JSONObject;

public class ClosetRequest extends TransferItemRequest {
  private int moveType;

  public static final int REFRESH = 0;
  public static final int INVENTORY_TO_CLOSET = 1;
  public static final int CLOSET_TO_INVENTORY = 2;
  public static final int MEAT_TO_CLOSET = 3;
  public static final int MEAT_TO_INVENTORY = 4;
  public static final int EMPTY_CLOSET = 5;

  public static void refresh() {
    // To refresh closet, we get Meat from any page
    // and items from api.php

    RequestThread.postRequest(new ClosetRequest(REFRESH));
    ApiRequest.updateCloset();
  }

  public static final void parseCloset(final JSONObject JSON) {
    if (JSON == null) {
      return;
    }

    ArrayList<AdventureResult> items = new ArrayList<>();

    try {
      // {"1":"1","2":"1" ... }
      Iterator<String> keys = JSON.keys();

      while (keys.hasNext()) {
        String key = keys.next();
        int itemId = StringUtilities.parseInt(key);
        int count = JSON.getInt(key);
        String name = ItemDatabase.getItemDataName(itemId);
        if (name == null) {
          // Fetch descid from api.php?what=item
          // and register new item.
          ItemDatabase.registerItem(itemId);
        }

        items.add(ItemPool.get(itemId, count));
      }
    } catch (JSONException e) {
      ApiRequest.reportParseError("closet", JSON.toString(), e);
      return;
    }

    KoLConstants.closet.clear();
    KoLConstants.closet.addAll(items);
    if (InventoryManager.canUseCloset()) {
      ConcoctionDatabase.refreshConcoctions();
    }
  }

  public ClosetRequest() {
    super("closet.php");
    this.moveType = REFRESH;
  }

  public ClosetRequest(final int moveType) {
    this(moveType, new AdventureResult[0]);
    this.moveType = moveType;
  }

  public ClosetRequest(final int moveType, final long amount) {
    this(moveType, new AdventureLongCountResult(AdventureResult.MEAT, amount));
  }

  public ClosetRequest(final int moveType, final AdventureResult attachment) {
    this(moveType, new AdventureResult[] {attachment});
  }

  public ClosetRequest(final int moveType, final AdventureResult[] attachments) {
    super(ClosetRequest.pickURL(moveType), attachments);
    this.moveType = moveType;

    // Figure out the actual URL information based on the
    // different request types.

    switch (moveType) {
      case REFRESH ->
      // It doesn't matter which page we visit to get Meat
      this.addFormField("which", "1");
      case MEAT_TO_CLOSET -> {
        // closet.php?action=addtakeclosetmeat&addtake=add&pwd&quantity=x
        this.addFormField("action", "addtakeclosetmeat");
        this.addFormField("addtake", "add");
      }
      case MEAT_TO_INVENTORY -> {
        // closet.php?action=addtakeclosetmeat&addtake=take&pwd&quantity=x
        this.addFormField("action", "addtakeclosetmeat");
        this.addFormField("addtake", "take");
      }
      case INVENTORY_TO_CLOSET -> {
        // fillcloset.php?action=closetpush&whichitem=4511&qty=xxx&pwd&ajax=1
        // fillcloset.php?action=closetpush&whichitem=4511&qty=all&pwd&ajax=1
        this.addFormField("action", "closetpush");
        this.addFormField("ajax", "1");
        this.source = KoLConstants.inventory;
        this.destination = KoLConstants.closet;
      }
      case CLOSET_TO_INVENTORY -> {
        // closet.php?action=closetpull&whichitem=4511&qty=xxx&pwd&ajax=1
        // closet.php?action=closetpull&whichitem=4511&qty=all&pwd&ajax=1
        this.addFormField("action", "closetpull");
        this.addFormField("ajax", "1");
        this.source = KoLConstants.closet;
        this.destination = KoLConstants.inventory;
      }
      case EMPTY_CLOSET -> {
        // closet.php?action=pullallcloset&pwd
        this.addFormField("action", "pullallcloset");
        this.source = KoLConstants.closet;
        this.destination = KoLConstants.inventory;
      }
    }
  }

  private static String pickURL(final int moveType) {
    return switch (moveType) {
      case INVENTORY_TO_CLOSET, CLOSET_TO_INVENTORY -> "inventory.php";
      default -> "closet.php";
    };
  }

  @Override
  protected boolean retryOnTimeout() {
    return true;
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
    return "qty";
  }

  @Override
  public String getMeatField() {
    return "quantity";
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
    return 1;
  }

  @Override
  public boolean forceGETMethod() {
    return this.moveType == INVENTORY_TO_CLOSET || this.moveType == CLOSET_TO_INVENTORY;
  }

  @Override
  public TransferItemRequest getSubInstance(final AdventureResult[] attachments) {
    return new ClosetRequest(this.moveType, attachments);
  }

  @Override
  public boolean parseTransfer() {
    return ClosetRequest.parseTransfer(this.getURLString(), this.responseText);
  }

  @Override
  public void run() {
    // If it's a transfer, let TransferItemRequest handle it
    super.run();
  }

  @Override
  public void processResults() {
    switch (this.moveType) {
      case ClosetRequest.REFRESH -> {
        ClosetRequest.parseCloset(this.getURLString(), this.responseText);
        return;
      }
      default -> super.processResults();
    }
  }

  // Your closet contains <b>170,000,000</b> meat.
  private static final Pattern CLOSETMEAT_PATTERN =
      Pattern.compile("Your closet contains <b>([\\d,]+)</b> meat\\.");

  public static void parseCloset(final String urlString, final String responseText) {
    if (!urlString.startsWith("closet.php")) {
      return;
    }

    Matcher meatInClosetMatcher = ClosetRequest.CLOSETMEAT_PATTERN.matcher(responseText);

    if (meatInClosetMatcher.find()) {
      String meatInCloset = meatInClosetMatcher.group(1);
      KoLCharacter.setClosetMeat(StringUtilities.parseInt(meatInCloset));
    }
  }

  public static final boolean parseTransfer(final String urlString, final String responseText) {
    if (!urlString.contains("action")) {
      ClosetRequest.parseCloset(urlString, responseText);
      return true;
    }

    boolean success = false;

    if (urlString.contains("action=addtakeclosetmeat")) {
      // Determine how much meat is left in your closet by locating
      // "Your closet contains x meat" and update the display with
      // that information.

      Matcher matcher = ClosetRequest.CLOSETMEAT_PATTERN.matcher(responseText);
      long before = KoLCharacter.getClosetMeat();
      long after = matcher.find() ? StringUtilities.parseLong(matcher.group(1)) : 0;

      KoLCharacter.setClosetMeat(after);
      success = before != after;
    } else if (urlString.contains("action=closetpull")) {
      if (!responseText.contains("You acquire")) {
        return false;
      }

      // Since "you acquire" items, they have already been
      // added to inventory

      TransferItemRequest.transferItems(
          urlString,
          TransferItemRequest.ITEMID_PATTERN,
          TransferItemRequest.QTY_PATTERN,
          KoLConstants.closet,
          null,
          0);
      success = true;
    } else if (urlString.contains("action=closetpush")) {
      if (responseText.contains("in your closet")) {
        TransferItemRequest.transferItems(
            urlString,
            TransferItemRequest.ITEMID_PATTERN,
            TransferItemRequest.QTY_PATTERN,
            KoLConstants.inventory,
            KoLConstants.closet,
            0);
        success = true;
      }
    } else if (urlString.contains("action=pullallcloset")) {
      if (!responseText.contains("taken from your closet")) {
        return false;
      }

      TransferItemRequest.transferItems(
          new ArrayList<>(KoLConstants.closet), KoLConstants.closet, KoLConstants.inventory);
      success = true;
    }

    if (success) {
      // If we can't use the closet as to get ingredients,
      // moving items in or out of inventory can affect what
      // we can make.
      if (!InventoryManager.canUseCloset()) {
        ConcoctionDatabase.refreshConcoctions(true);
      }

      KoLCharacter.updateStatus();
    }

    return success;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("closet.php")
        && !urlString.startsWith("fillcloset.php")
        && !urlString.startsWith("inventory.php")) {
      return false;
    }

    if (urlString.contains("action=closetpull")) {
      return TransferItemRequest.registerRequest(
          "take from closet",
          urlString,
          TransferItemRequest.ITEMID_PATTERN,
          TransferItemRequest.QTY_PATTERN,
          KoLConstants.closet,
          0);
    }

    if (urlString.contains("action=closetpush")) {
      return TransferItemRequest.registerRequest(
          "add to closet",
          urlString,
          TransferItemRequest.ITEMID_PATTERN,
          TransferItemRequest.QTY_PATTERN,
          KoLConstants.inventory,
          0);
    }

    long meat = TransferItemRequest.transferredMeat(urlString, "quantity");
    String message = null;

    if (urlString.contains("action=addtakeclosetmeat")) {
      if (urlString.contains("addtake=add")) {
        message = "add to closet: " + meat + " Meat";
      } else if (urlString.contains("addtake=take")) {
        message = "take from closet " + meat + " Meat";
      }
    }

    if (meat > 0 && message != null) {
      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog(message);
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
      case REFRESH -> "Examining Meat in closet";
      case INVENTORY_TO_CLOSET -> "Placing items into closet";
      case CLOSET_TO_INVENTORY -> "Removing items from closet";
      case MEAT_TO_CLOSET -> "Placing meat into closet";
      case MEAT_TO_INVENTORY -> "Removing meat from closet";
      case EMPTY_CLOSET -> "Emptying closet";
      default -> "Unknown request type";
    };
  }
}
