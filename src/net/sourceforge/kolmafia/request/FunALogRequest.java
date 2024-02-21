package net.sourceforge.kolmafia.request;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FunALogRequest extends CoinMasterRequest {
  public static final String master = "PirateRealm Fun-a-Log";

  private static String unlockedItems = "";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("<b>You have ([\\d,]+) FunPoints?\\.</b>");

  public static final CoinmasterData FUN_A_LOG =
      new CoinmasterData(master, "Fun-a-Log", FunALogRequest.class)
          .withToken("FunPoint")
          .withTokenTest("You have no FunPoints")
          .withTokenPattern(TOKEN_PATTERN)
          .withProperty("availableFunPoints")
          .withShopRowFields(master, "piraterealm")
          .withAvailableItem(FunALogRequest::availableItem);

  private static Boolean availableItem(final Integer itemId) {
    return unlockedItems.contains(ItemDatabase.getItemName(itemId));
  }

  public FunALogRequest() {
    super(FUN_A_LOG);
  }

  public FunALogRequest(final boolean buying, final AdventureResult[] attachments) {
    super(FUN_A_LOG, buying, attachments);
  }

  public FunALogRequest(final boolean buying, final AdventureResult attachment) {
    super(FUN_A_LOG, buying, attachment);
  }

  public FunALogRequest(final boolean buying, final int itemId, final int quantity) {
    super(FUN_A_LOG, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  // <tr rel="10231"><td valign=center><input type=radio name=whichrow value=1064></td><td><img
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/pr_partyhat.gif"
  // class="hand pop" rel="desc_item.php?whichitem=971293634"
  // onClick='javascript:descitem(971293634)'></td><td valign=center><a
  // onClick='javascript:descitem(971293634)'><b>PirateRealm party
  // hat</b>&nbsp;&nbsp;&nbsp;&nbsp;</a></td><td>F</td><td><b>20</b>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td valign=center><input class="button doit multibuy "  type=button rel='shop.php?whichshop=piraterealm&action=buyitem&quantity=1&whichrow=1064&pwd=5f195b385cbe62956e089308af45f544' value='Buy'></td></tr>

  private static final Pattern ITEM_PATTERN =
      Pattern.compile(
          "<tr rel=\"(\\d+)\">.*?whichrow value=(\\d+)>.*?desc_item.php\\?whichitem=(\\d+).*?<b>(.*?)</b>.*?<td>F</td><td><b>([,\\d]+)</b>");

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=piraterealm")) {
      return;
    }

    // Learn new Fun-a-Log simply visiting the shop
    // Refresh the Coin Master inventory every time we visit.

    CoinmasterData data = FUN_A_LOG;

    Set<Integer> originalItems = data.getBuyPrices().keySet();
    List<AdventureResult> items = data.getBuyItems();
    Map<Integer, Integer> prices = data.getBuyPrices();
    Map<Integer, Integer> rows = data.getRows();

    StringBuilder unlocked = new StringBuilder();

    Matcher matcher = ITEM_PATTERN.matcher(responseText);
    while (matcher.find()) {
      int itemId = StringUtilities.parseInt(matcher.group(1));
      int row = StringUtilities.parseInt(matcher.group(2));
      String descId = matcher.group(3);
      String itemName = matcher.group(4);
      int price = StringUtilities.parseInt(matcher.group(5));

      String match = ItemDatabase.getItemDataName(itemId);
      if (match == null || !match.equals(itemName)) {
        ItemDatabase.registerItem(itemId, itemName, descId);
      }

      // Add it to the unlocked items
      if (unlocked.length() > 0) {
        unlocked.append("|");
      }
      unlocked.append(itemName);

      // If this item was not previously known,
      if (!originalItems.contains(itemId)) {
        // Add it to the Fun-a-Log inventory
        AdventureResult item = ItemPool.get(itemId, PurchaseRequest.MAX_QUANTITY);
        items.add(item);
        prices.put(itemId, price);
        rows.put(itemId, row);

        // Print a coinmasters.txt line for it
        NPCPurchaseRequest.learnCoinmasterItem(
            master,
            new AdventureResult(itemName, 1),
            new AdventureResult("FunPoint", price),
            String.valueOf(row));
      }
    }

    // Remember which items we have unlocked
    unlockedItems = unlocked.toString();

    // Register the purchase requests, now that we know what is available
    data.registerPurchaseRequests();

    CoinMasterRequest.parseResponse(data, urlString, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=piraterealm")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(FUN_A_LOG, urlString, true);
  }

  public static String accessible() {
    // You have to have the Fun-A-Log in your inventory in order to
    // purchase from it.  It is a quest item, so if you have it, it
    // will be there.  You get it the first time you complete a
    // PirateRealm adventure.  Therefore, you needed access to the
    // PirateRealm at least once to get it, but you do not need
    // current access to PirateRealm to use it.

    return InventoryManager.hasItem(ItemPool.PIRATE_REALM_FUN_LOG)
        ? null
        : "Need PirateRealm fun-a-log";
  }
}
