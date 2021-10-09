package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.session.BatManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.Limitmode;

public class GotporkOrphanageRequest extends CoinMasterRequest {
  public static final String master = "Gotpork Orphanage";
  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(GotporkOrphanageRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(GotporkOrphanageRequest.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(GotporkOrphanageRequest.master);

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) kidnapped orphan");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.KIDNAPPED_ORPHAN, 1);
  public static final CoinmasterData GOTPORK_ORPHANAGE =
      new CoinmasterData(
          GotporkOrphanageRequest.master,
          "Gotpork Orphanage",
          GotporkOrphanageRequest.class,
          "kidnapped orphan",
          null,
          false,
          GotporkOrphanageRequest.TOKEN_PATTERN,
          GotporkOrphanageRequest.COIN,
          null,
          GotporkOrphanageRequest.itemRows,
          "shop.php?whichshop=batman_orphanage",
          "buyitem",
          GotporkOrphanageRequest.buyItems,
          GotporkOrphanageRequest.buyPrices,
          null,
          null,
          null,
          null,
          "whichrow",
          GenericRequest.WHICHROW_PATTERN,
          "quantity",
          GenericRequest.QUANTITY_PATTERN,
          null,
          null,
          true) {
        @Override
        public AdventureResult itemBuyPrice(final int itemId) {
          int price = GotporkOrphanageRequest.buyPrices.get(IntegerPool.get(itemId));
          if (price == 1) {
            return GotporkOrphanageRequest.COIN;
          }
          // price increased by 3 each time you buy one
          int count = InventoryManager.getCount(itemId);
          if (count > 0) {
            price = 3 * (count + 1);
          }
          return GotporkOrphanageRequest.COIN.getInstance(price);
        }
      };

  public GotporkOrphanageRequest() {
    super(GotporkOrphanageRequest.GOTPORK_ORPHANAGE);
  }

  public GotporkOrphanageRequest(final boolean buying, final AdventureResult[] attachments) {
    super(GotporkOrphanageRequest.GOTPORK_ORPHANAGE, buying, attachments);
  }

  public GotporkOrphanageRequest(final boolean buying, final AdventureResult attachment) {
    super(GotporkOrphanageRequest.GOTPORK_ORPHANAGE, buying, attachment);
  }

  public GotporkOrphanageRequest(final boolean buying, final int itemId, final int quantity) {
    super(GotporkOrphanageRequest.GOTPORK_ORPHANAGE, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    GotporkOrphanageRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=batman_orphanage")) {
      return;
    }

    CoinmasterData data = GotporkOrphanageRequest.GOTPORK_ORPHANAGE;

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

    CoinmasterData data = GotporkOrphanageRequest.GOTPORK_ORPHANAGE;
    return CoinMasterRequest.registerRequest(data, urlString, true);
  }

  public static String accessible() {
    if (KoLCharacter.getLimitmode() != Limitmode.BATMAN) {
      return "Only Batfellow can go to the Gotpork Orphanage.";
    }
    if (BatManager.currentBatZone() != BatManager.DOWNTOWN) {
      return "Batfellow can only visit the Gotpork Orphanage while Downtown.";
    }
    return null;
  }
}
