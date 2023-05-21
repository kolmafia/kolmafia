package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public class ReplicaMrStoreRequest extends CoinMasterRequest {
  public static final String master = "Replica Mr. Store";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("<td>([\\d,]+) Replica Mr. Accessor");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.REPLICA_MR_ACCESSORY, 1);

  public static final CoinmasterData REPLICA_MR_STORE =
      new CoinmasterData(master, "Replica Mr. Store", ReplicaMrStoreRequest.class)
          .withToken("replica Mr. Accessory")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, "mrreplica");

  public ReplicaMrStoreRequest() {
    super(REPLICA_MR_STORE);
  }

  public ReplicaMrStoreRequest(final boolean buying, final AdventureResult[] attachments) {
    super(REPLICA_MR_STORE, buying, attachments);
  }

  public ReplicaMrStoreRequest(final boolean buying, final AdventureResult attachment) {
    super(REPLICA_MR_STORE, buying, attachment);
  }

  public ReplicaMrStoreRequest(final boolean buying, final int itemId, final int quantity) {
    super(REPLICA_MR_STORE, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=mrreplica")) {
      return;
    }

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(REPLICA_MR_STORE, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(REPLICA_MR_STORE, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=mrreplica")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(REPLICA_MR_STORE, urlString, true);
  }

  public static String accessible() {
    if (!KoLCharacter.inLegacyOfLoathing()) {
      return "Only Legacy Loathers can buy replica Mr. Items";
    }
    return null;
  }
}
