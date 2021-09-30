package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.session.InventoryManager;

public class VendingMachineRequest extends CoinMasterRequest {
  public static final String master = "Vending Machine";

  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(VendingMachineRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(VendingMachineRequest.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(VendingMachineRequest.master);
  private static final Pattern TOKEN_PATTERN = Pattern.compile("(\\d+) fat loot token");
  public static final AdventureResult FAT_LOOT_TOKEN = ItemPool.get(ItemPool.FAT_LOOT_TOKEN, 1);

  public static final CoinmasterData VENDING_MACHINE =
      new CoinmasterData(
          VendingMachineRequest.master,
          "vendingmachine",
          VendingMachineRequest.class,
          "fat loot token",
          "no fat loot tokens",
          false,
          VendingMachineRequest.TOKEN_PATTERN,
          VendingMachineRequest.FAT_LOOT_TOKEN,
          null,
          VendingMachineRequest.itemRows,
          "shop.php?whichshop=damachine",
          "buyitem",
          VendingMachineRequest.buyItems,
          VendingMachineRequest.buyPrices,
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
        public final boolean canBuyItem(final int itemId) {
          switch (itemId) {
            case ItemPool.SEWING_KIT:
              return InventoryManager.getCount(itemId) == 0;
          }
          return super.canBuyItem(itemId);
        }
      };

  public VendingMachineRequest() {
    super(VendingMachineRequest.VENDING_MACHINE);
  }

  public VendingMachineRequest(final boolean buying, final AdventureResult[] attachments) {
    super(VendingMachineRequest.VENDING_MACHINE, buying, attachments);
  }

  public VendingMachineRequest(final boolean buying, final AdventureResult attachment) {
    super(VendingMachineRequest.VENDING_MACHINE, buying, attachment);
  }

  public VendingMachineRequest(final boolean buying, final int itemId, final int quantity) {
    super(VendingMachineRequest.VENDING_MACHINE, buying, itemId, quantity);
  }

  @Override
  public void run() {
    if (this.action != null) {
      this.addFormField("pwd");
    }

    super.run();
  }

  @Override
  public void processResults() {
    VendingMachineRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    CoinmasterData data = VendingMachineRequest.VENDING_MACHINE;
    String action = GenericRequest.getAction(location);
    if (action == null) {
      if (location.contains("whichshop=damachine")) {
        // Parse current coin balances
        CoinMasterRequest.parseBalance(data, responseText);
      }

      return;
    }

    CoinMasterRequest.parseResponse(data, location, responseText);
  }

  public static String accessible() {
    if (KoLCharacter.isKingdomOfExploathing()) {
      return "The vending machine exploded";
    }
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=damachine")) {
      return false;
    }

    CoinmasterData data = VendingMachineRequest.VENDING_MACHINE;
    return CoinMasterRequest.registerRequest(data, urlString, true);
  }
}
