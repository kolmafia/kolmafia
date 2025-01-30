package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.session.BatManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.LimitMode;

public class ChemiCorpRequest extends CoinMasterRequest {
  public static final String master = "ChemiCorp";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) dangerous chemicals");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.DANGEROUS_CHEMICALS, 1);

  public static final CoinmasterData CHEMICORP =
      new CoinmasterData(master, "ChemiCorp", ChemiCorpRequest.class)
          .withToken("dangerous chemicals")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, "batman_chemicorp")
          .withItemBuyPrice(ChemiCorpRequest::itemBuyPrice);

  private static AdventureResult itemBuyPrice(final Integer itemId) {
    int price = CHEMICORP.getBuyPrices().get(itemId);
    if (price == 1) {
      return COIN;
    }
    // price increased by 3 each time you buy one
    int count = InventoryManager.getCount(itemId);
    if (count > 0) {
      price = 3 * (count + 1);
    }
    return COIN.getInstance(price);
  }

  public ChemiCorpRequest() {
    super(CHEMICORP);
  }

  public ChemiCorpRequest(final boolean buying, final AdventureResult[] attachments) {
    super(CHEMICORP, buying, attachments);
  }

  public ChemiCorpRequest(final boolean buying, final AdventureResult attachment) {
    super(CHEMICORP, buying, attachment);
  }

  public ChemiCorpRequest(final boolean buying, final int itemId, final int quantity) {
    super(CHEMICORP, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=batman_chemicorp")) {
      return;
    }

    CoinmasterData data = CHEMICORP;

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=batman_chemicorp")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(CHEMICORP, urlString, true);
  }

  public static String accessible() {
    if (KoLCharacter.getLimitMode() != LimitMode.BATMAN) {
      return "Only Batfellow can go to ChemiCorp.";
    }
    if (BatManager.currentBatZone() != BatManager.DOWNTOWN) {
      return "Batfellow can only visit ChemiCorp while Downtown.";
    }
    return null;
  }
}
