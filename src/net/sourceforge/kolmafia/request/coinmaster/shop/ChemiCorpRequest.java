package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.session.BatManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.LimitMode;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class ChemiCorpRequest extends CoinMasterRequest {
  public static final String master = "ChemiCorp";
  public static final String SHOPID = "batman_chemicorp";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) dangerous chemicals");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.DANGEROUS_CHEMICALS, 1);

  public static final CoinmasterData CHEMICORP =
      new CoinmasterData(master, "ChemiCorp", ChemiCorpRequest.class)
          .withToken("dangerous chemicals")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, SHOPID)
          .withItemBuyPrice(ChemiCorpRequest::itemBuyPrice)
          .withAccessible(ChemiCorpRequest::accessible);

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

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), this.responseText);
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
