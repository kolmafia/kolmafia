package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.BatManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.LimitMode;

public class GotporkOrphanageRequest extends CoinMasterRequest {
  public static final String master = "Gotpork Orphanage";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) kidnapped orphan");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.KIDNAPPED_ORPHAN, 1);

  public static final CoinmasterData GOTPORK_ORPHANAGE =
      new CoinmasterData(master, "Gotpork Orphanage", GotporkOrphanageRequest.class)
          .withToken("kidnapped orphan")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, "batman_orphanage")
          .withItemBuyPrice(GotporkOrphanageRequest::itemBuyPrice);

  private static AdventureResult itemBuyPrice(final Integer itemId) {
    int price = GOTPORK_ORPHANAGE.getBuyPrices().get(itemId);
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

  public GotporkOrphanageRequest() {
    super(GOTPORK_ORPHANAGE);
  }

  public GotporkOrphanageRequest(final boolean buying, final AdventureResult[] attachments) {
    super(GOTPORK_ORPHANAGE, buying, attachments);
  }

  public GotporkOrphanageRequest(final boolean buying, final AdventureResult attachment) {
    super(GOTPORK_ORPHANAGE, buying, attachment);
  }

  public GotporkOrphanageRequest(final boolean buying, final int itemId, final int quantity) {
    super(GOTPORK_ORPHANAGE, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=batman_orphanage")) {
      return;
    }

    CoinmasterData data = GOTPORK_ORPHANAGE;

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=batman_orphanage")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(GOTPORK_ORPHANAGE, urlString, true);
  }

  public static String accessible() {
    if (KoLCharacter.getLimitMode() != LimitMode.BATMAN) {
      return "Only Batfellow can go to the Gotpork Orphanage.";
    }
    if (BatManager.currentBatZone() != BatManager.DOWNTOWN) {
      return "Batfellow can only visit the Gotpork Orphanage while Downtown.";
    }
    return null;
  }
}
