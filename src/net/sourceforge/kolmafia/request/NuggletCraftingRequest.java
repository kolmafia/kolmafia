package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public class NuggletCraftingRequest extends CoinMasterRequest {
  public static final String master = "Topiary Nuggletcrafting";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) topiary nugglet");
  public static final AdventureResult TOPIARY_NUGGLET = ItemPool.get(ItemPool.TOPIARY_NUGGLET, 1);

  public static final CoinmasterData NUGGLETCRAFTING =
      new CoinmasterData(master, "NuggletCrafting", NuggletCraftingRequest.class)
          .withToken("topiary nugglet")
          .withTokenTest("no topiary nugglets")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(TOPIARY_NUGGLET)
          .withShopRowFields(master, "topiary");

  public NuggletCraftingRequest() {
    super(NUGGLETCRAFTING);
  }

  public NuggletCraftingRequest(final boolean buying, final AdventureResult[] attachments) {
    super(NUGGLETCRAFTING, buying, attachments);
  }

  public NuggletCraftingRequest(final boolean buying, final AdventureResult attachment) {
    super(NUGGLETCRAFTING, buying, attachment);
  }

  public NuggletCraftingRequest(final boolean buying, final int itemId, final int quantity) {
    super(NUGGLETCRAFTING, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.startsWith("shop.php") || !location.contains("whichshop=topiary")) {
      return;
    }

    CoinmasterData data = NUGGLETCRAFTING;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    if (TOPIARY_NUGGLET.getCount(KoLConstants.inventory) == 0) {
      return "You do not have a topiary nugglet in inventory";
    }
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    // shop.php?pwd&whichshop=topiary
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=topiary")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(NUGGLETCRAFTING, urlString, true);
  }
}
