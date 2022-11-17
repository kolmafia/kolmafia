package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.InventoryManager;

public class VendingMachineRequest extends CoinMasterRequest {
  public static final String master = "Vending Machine";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("(\\d+) fat loot token");
  public static final AdventureResult FAT_LOOT_TOKEN = ItemPool.get(ItemPool.FAT_LOOT_TOKEN, 1);

  public static final CoinmasterData VENDING_MACHINE =
      new CoinmasterData(master, "vendingmachine", VendingMachineRequest.class)
          .withToken("fat loot token")
          .withTokenTest("no fat loot tokens")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(FAT_LOOT_TOKEN)
          .withShopRowFields(master, "damachine")
          .withNeedsPasswordHash(true)
          .withCanBuyItem(VendingMachineRequest::canBuyItem);

  private static Boolean canBuyItem(final Integer itemId) {
    AdventureResult item = ItemPool.get(itemId);
    return switch (itemId) {
      case ItemPool.SEWING_KIT -> InventoryManager.getCount(item) == 0;
      default -> item.getCount(VENDING_MACHINE.getBuyItems()) > 0;
    };
  }

  public VendingMachineRequest() {
    super(VENDING_MACHINE);
  }

  public VendingMachineRequest(final boolean buying, final AdventureResult[] attachments) {
    super(VENDING_MACHINE, buying, attachments);
  }

  public VendingMachineRequest(final boolean buying, final AdventureResult attachment) {
    super(VENDING_MACHINE, buying, attachment);
  }

  public VendingMachineRequest(final boolean buying, final int itemId, final int quantity) {
    super(VENDING_MACHINE, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    CoinmasterData data = VENDING_MACHINE;
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

    return CoinMasterRequest.registerRequest(VENDING_MACHINE, urlString, true);
  }
}
