package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.session.DisplayCaseManager;
import net.sourceforge.kolmafia.utilities.AdventureResultArray;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class DisplayCaseRequest extends TransferItemRequest {
  private boolean isDeposit;
  private boolean isWithdrawal;

  public DisplayCaseRequest() {
    super("managecollectionshelves.php");

    this.isDeposit = false;
    this.isWithdrawal = false;
  }

  public DisplayCaseRequest(final AdventureResult[] items, final int[] shelves) {
    super("managecollectionshelves.php");
    this.addFormField("action", "arrange");

    for (int i = 0; i < items.length; ++i) {
      this.addFormField("whichshelf" + items[i].getItemId(), String.valueOf(shelves[i]));
    }

    this.isDeposit = false;
    this.isWithdrawal = false;
  }

  public DisplayCaseRequest(final AdventureResult[] attachments, boolean isDeposit) {
    super("managecollection.php", attachments);
    if (isDeposit) {
      this.addFormField("action", "put");
      this.isDeposit = true;
      this.isWithdrawal = false;
      this.source = KoLConstants.inventory;
      this.destination = KoLConstants.collection;
    } else {
      this.addFormField("action", "take");
      this.isDeposit = false;
      this.isWithdrawal = true;
      this.source = KoLConstants.collection;
      this.destination = KoLConstants.inventory;
    }
    this.addFormField("ajax", "1");
  }

  public DisplayCaseRequest(String who) {
    super("displaycollection.php");

    if (!StringUtilities.isNumeric(who)) {
      who = ContactManager.getPlayerId(who, true);
    }

    this.addFormField("who", who);
  }

  public DisplayCaseRequest(final AdventureResult[] items, final int shelf) {
    this();
    this.addFormField("action", "arrange");

    String shelfString = String.valueOf(shelf);
    for (int i = 0; i < items.length; ++i) {
      this.addFormField("whichshelf" + items[i].getItemId(), shelfString);
    }

    this.isDeposit = false;
    this.isWithdrawal = false;
  }

  @Override
  protected boolean retryOnTimeout() {
    return !this.isDeposit && !this.isWithdrawal;
  }

  @Override
  public int getCapacity() {
    return 11;
  }

  @Override
  public TransferItemRequest getSubInstance(final AdventureResult[] attachments) {
    return new DisplayCaseRequest(attachments, this.isDeposit);
  }

  public String getSuccessMessage() {
    return "";
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
    return "";
  }

  @Override
  public boolean parseTransfer() {
    return DisplayCaseRequest.parseTransfer(this.getURLString(), this.responseText);
  }

  public static final boolean parseTransfer(final String urlString, final String responseText) {
    if (urlString.startsWith("managecollection.php")) {
      return DisplayCaseRequest.parseDisplayTransfer(urlString, responseText);
    }

    if (urlString.startsWith("managecollectionshelves.php")) {
      return DisplayCaseRequest.parseDisplayArrangement(urlString, responseText);
    }

    return false;
  }

  public static final Pattern ITEM_PATTERN1 =
      Pattern.compile("<b>(([^<]*((?!</b>)<))*[^<]*)</b> moved from inventory to case");
  public static final Pattern ITEM_PATTERN2 =
      Pattern.compile("<b>(([^<]*((?!</b>)<))*[^<]*)</b> moved from case to inventory");

  public static final boolean parseDisplayTransfer(
      final String urlString, final String responseText) {
    if (urlString.indexOf("put") != -1) {
      // You haven't got any of that item in your inventory.
      // <b>club necklace (5)</b> moved from inventory to case.
      if (responseText.indexOf("moved from inventory to case") == -1) {
        return false;
      }

      TransferItemRequest.transferItems(
          responseText,
          DisplayCaseRequest.ITEM_PATTERN1,
          KoLConstants.inventory,
          KoLConstants.collection);

      return true;
    }

    if (urlString.indexOf("take") != -1) {
      // You haven't got any of that item in your case.
      // <b>club necklace (5)</b> moved from case to inventory.
      if (responseText.indexOf("moved from case to inventory") == -1) {
        return false;
      }

      AdventureResultArray itemList =
          TransferItemRequest.getItemList(
              responseText, ITEM_PATTERN2, TransferItemRequest.ITEM_PATTERN1, (Pattern) null);

      if (itemList.isEmpty()) {
        return false;
      }

      TransferItemRequest.transferItems(itemList, KoLConstants.collection, KoLConstants.inventory);

      for (int i = 0; i < itemList.size(); ++i) {
        AdventureResult item = itemList.get(i);
        KoLmafia.updateDisplay("You acquire " + item);
      }

      return true;
    }

    return false;
  }

  public static final boolean parseDisplayArrangement(
      final String urlString, final String responseText) {
    if (urlString.indexOf("action=arrange") == -1) {
      DisplayCaseManager.update(responseText);
    }

    return true;
  }

  // <table><tr><td valign=center><img
  // src="http://images.kingdomofloathing.com/otherimages/museum/displaycase.gif" width=100
  // height=100></td><td valign=center>...txt...</td></tr></table>
  public static final Pattern ANNOUNCEMENT_PATTERN =
      Pattern.compile(
          "<table><tr><td valign=center><img src=[^>]*?(?:cloudfront.net|images.kingdomofloathing.com|/images)/otherimages/museum/displaycase.gif\" width=100 height=100></td><td[^.]*>(.*?)</td></table>");

  public static final boolean parseDisplayCase(final String urlString, String responseText) {
    RequestThread.runInParallel(new DisplayCaseParser(responseText), false);
    return true;
  }

  private static class DisplayCaseParser implements Runnable {
    private final String responseText;

    public DisplayCaseParser(final String responseText) {
      Matcher matcher = DisplayCaseRequest.ANNOUNCEMENT_PATTERN.matcher(responseText);
      String announcement = matcher.find() ? matcher.group(1).trim() : "";
      this.responseText =
          announcement.equals("")
              ? responseText
              : StringUtilities.singleStringReplace(responseText, announcement, "");
    }

    public void run() {
      ItemDatabase.parseNewItems(responseText);
    }
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
    return this.isDeposit
        ? "Placing items in display case"
        : this.isWithdrawal ? "Removing items from display case" : "Updating display case";
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("managecollection.php")) {
      return false;
    }

    if (urlString.indexOf("action=take") != -1) {
      return TransferItemRequest.registerRequest(
          "remove from display case",
          urlString,
          TransferItemRequest.ITEMID_PATTERN,
          TransferItemRequest.HOWMANY_PATTERN,
          KoLConstants.collection,
          0);
    }

    if (urlString.indexOf("action=put") != -1) {
      return TransferItemRequest.registerRequest(
          "put in display case",
          urlString,
          TransferItemRequest.ITEMID_PATTERN,
          TransferItemRequest.HOWMANY_PATTERN,
          KoLConstants.inventory,
          0);
    }

    return true;
  }
}
