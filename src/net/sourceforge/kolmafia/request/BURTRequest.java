package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BURTRequest extends CoinMasterRequest {
  public static final String master = "Bugbear Token";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("You have ([\\d,]+) BURT");
  public static final AdventureResult BURT_TOKEN = ItemPool.get(ItemPool.BURT, 1);
  private static final Pattern TOBUY_PATTERN = Pattern.compile("itemquantity=(\\d+)");

  public static final CoinmasterData BURT =
      new CoinmasterData(master, "BURT", BURTRequest.class)
          .withToken("BURT")
          .withPluralToken("BURTs")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(BURT_TOKEN)
          .withBuyURL("inv_use.php?whichitem=5683&ajax=1")
          .withBuyItems(master)
          .withBuyPrices(master)
          .withItemField("itemquantity")
          .withItemPattern(TOBUY_PATTERN);

  private static final Map<Integer, Integer> itemByPrice =
      CoinmastersDatabase.invert(BURT.getBuyPrices());

  private static int priceToItemId(final int price) {
    return itemByPrice.get(price);
  }

  private static int itemIdToPrice(final int itemId) {
    return BURT.getBuyPrice(itemId);
  }

  private static String lastURL = null;

  public BURTRequest() {
    super(BURT);
  }

  public BURTRequest(final boolean buying, final AdventureResult[] attachments) {
    super(BURT, buying, attachments);
  }

  public BURTRequest(final boolean buying, final AdventureResult attachment) {
    super(BURT, buying, attachment);
  }

  public BURTRequest(final boolean buying, final int itemId, final int quantity) {
    super(BURT, buying, itemId, quantity);
  }

  @Override
  public void setItem(final AdventureResult item) {
    // The item field is the buy price; the number of BURTS spent
    String itemField = BURT.getItemField();
    this.addFormField(itemField, String.valueOf(itemIdToPrice(item.getItemId())));
  }

  @Override
  public void processResults() {
    parseResponse(this.responseText);
  }

  public static void parseResponse(final String responseText) {
    if (lastURL == null) {
      return;
    }

    String location = lastURL;
    lastURL = null;

    CoinmasterData data = BURT;

    // If you don't have enough BURTs, you are redirected to inventory.php
    if (responseText.indexOf("You don't have enough BURTs") == -1) {
      // inv_use.php?whichitem=5683&pwd&itemquantity=xxx
      Matcher itemMatcher = data.getItemMatcher(location);
      if (itemMatcher.find()) {
        int price = StringUtilities.parseInt(itemMatcher.group(1));
        int itemId = priceToItemId(price);
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
    lastURL = urlString;

    CoinmasterData data = BURT;
    Matcher itemMatcher = data.getItemMatcher(urlString);
    if (!itemMatcher.find()) {
      return true;
    }

    int price = StringUtilities.parseInt(itemMatcher.group(1));
    int itemId = priceToItemId(price);
    if (itemId != -1) {
      CoinMasterRequest.buyStuff(data, itemId, 1, false);
    }
    return true;
  }

  public static String accessible() {
    int BURTs = BURT_TOKEN.getCount(KoLConstants.inventory);
    if (BURTs == 0) {
      return "You don't have any BURTs";
    }
    return null;
  }
}
