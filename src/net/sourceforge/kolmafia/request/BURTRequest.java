package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BURTRequest extends CoinMasterRequest {
  public static final String master = "Bugbear Token";
  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(BURTRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(BURTRequest.master);
  private static final Map<Integer, Integer> itemByPrice =
      CoinmastersDatabase.invert(BURTRequest.buyPrices);

  private static final Pattern TOKEN_PATTERN = Pattern.compile("You have ([\\d,]+) BURT");
  public static final AdventureResult BURT_TOKEN = ItemPool.get(ItemPool.BURT, 1);
  private static final Pattern TOBUY_PATTERN = Pattern.compile("itemquantity=(\\d+)");
  public static final CoinmasterData BURT =
      new CoinmasterData(
          BURTRequest.master,
          "BURT",
          BURTRequest.class,
          "BURT",
          null,
          false,
          BURTRequest.TOKEN_PATTERN,
          BURTRequest.BURT_TOKEN,
          null,
          null,
          "inv_use.php?whichitem=5683&ajax=1",
          null,
          BURTRequest.buyItems,
          BURTRequest.buyPrices,
          null,
          null,
          null,
          null,
          "itemquantity",
          BURTRequest.TOBUY_PATTERN,
          null,
          null,
          null,
          null,
          true);

  static {
    BURT.plural = "BURTs";
  }

  private static int priceToItemId(final int price) {
    int itemId = BURTRequest.itemByPrice.get(IntegerPool.get(price));
    return itemId;
  }

  private static int itemIdToPrice(final int itemId) {
    CoinmasterData data = BURTRequest.BURT;
    return data.getBuyPrice(itemId);
  }

  private static String lastURL = null;

  public BURTRequest() {
    super(BURTRequest.BURT);
  }

  public BURTRequest(final boolean buying, final AdventureResult[] attachments) {
    super(BURTRequest.BURT, buying, attachments);
  }

  public BURTRequest(final boolean buying, final AdventureResult attachment) {
    super(BURTRequest.BURT, buying, attachment);
  }

  public BURTRequest(final boolean buying, final int itemId, final int quantity) {
    super(BURTRequest.BURT, buying, itemId, quantity);
  }

  @Override
  public void setItem(final AdventureResult item) {
    // The item field is the buy price; the number of BURTS spent
    String itemField = this.data.getItemField();
    this.addFormField(itemField, String.valueOf(BURTRequest.itemIdToPrice(item.getItemId())));
  }

  @Override
  public void processResults() {
    BURTRequest.parseResponse(this.responseText);
  }

  public static void parseResponse(final String responseText) {
    if (BURTRequest.lastURL == null) {
      return;
    }

    String location = BURTRequest.lastURL;
    BURTRequest.lastURL = null;

    CoinmasterData data = BURTRequest.BURT;

    // If you don't have enough BURTs, you are redirected to inventory.php
    if (responseText.indexOf("You don't have enough BURTs") == -1) {
      // inv_use.php?whichitem=5683&pwd&itemquantity=xxx
      Matcher itemMatcher = data.getItemMatcher(location);
      if (itemMatcher.find()) {
        int price = StringUtilities.parseInt(itemMatcher.group(1));
        int itemId = BURTRequest.priceToItemId(price);
        CoinMasterRequest.completePurchase(data, itemId, 1, false);
      }
      return;
    }

    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static final boolean registerRequest(final String urlString) {
    // inv_use.php?whichitem=5683&pwd&itemquantity=xxx
    if (!urlString.startsWith("inv_use.php") || urlString.indexOf("whichitem=5683") == -1) {
      return false;
    }

    // Save URL. If request fails, we are redirected to inventory.php
    BURTRequest.lastURL = urlString;

    CoinmasterData data = BURTRequest.BURT;
    Matcher itemMatcher = data.getItemMatcher(urlString);
    if (!itemMatcher.find()) {
      return true;
    }

    int price = StringUtilities.parseInt(itemMatcher.group(1));
    int itemId = BURTRequest.priceToItemId(price);
    if (itemId != -1) {
      CoinMasterRequest.buyStuff(data, itemId, 1, false);
    }
    return true;
  }

  public static String accessible() {
    int BURTs = BURTRequest.BURT_TOKEN.getCount(KoLConstants.inventory);
    if (BURTs == 0) {
      return "You don't have any BURTs";
    }
    return null;
  }
}
