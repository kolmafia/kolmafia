package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class NuggletCraftingRequest extends CoinMasterRequest {
  public static final String master = "Topiary Nuggletcrafting";
  public static final String SHOPID = "topiary";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) topiary nugglet");
  public static final AdventureResult TOPIARY_NUGGLET = ItemPool.get(ItemPool.TOPIARY_NUGGLET, 1);

  public static final CoinmasterData NUGGLETCRAFTING =
      new CoinmasterData(master, "NuggletCrafting", NuggletCraftingRequest.class)
          .withToken("topiary nugglet")
          .withTokenTest("no topiary nugglets")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(TOPIARY_NUGGLET)
          .withShopRowFields(master, SHOPID)
          .withAccessible(NuggletCraftingRequest::accessible);

  public NuggletCraftingRequest() {
    super(NUGGLETCRAFTING);
  }

  public NuggletCraftingRequest(final boolean buying, final AdventureResult[] attachments) {
    super(NUGGLETCRAFTING, buying, attachments);
  }

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static String accessible() {
    if (TOPIARY_NUGGLET.getCount(KoLConstants.inventory) == 0) {
      return "You do not have a topiary nugglet in inventory";
    }
    return null;
  }
}
