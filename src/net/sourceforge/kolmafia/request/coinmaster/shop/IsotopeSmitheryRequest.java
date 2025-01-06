package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.request.SpaaaceRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;

public class IsotopeSmitheryRequest extends CoinMasterRequest {
  public static final String master = "Isotope Smithery";

  public static final CoinmasterData ISOTOPE_SMITHERY =
      new CoinmasterData(master, "isotopesmithery", IsotopeSmitheryRequest.class)
          .withToken("isotope")
          .withTokenTest("You have 0 lunar isotopes")
          .withTokenPattern(SpaaaceRequest.TOKEN_PATTERN)
          .withItem(SpaaaceRequest.ISOTOPE)
          .withShopRowFields(master, "elvishp1");

  public IsotopeSmitheryRequest() {
    super(ISOTOPE_SMITHERY);
  }

  public IsotopeSmitheryRequest(final boolean buying, final AdventureResult[] attachments) {
    super(ISOTOPE_SMITHERY, buying, attachments);
  }

  public IsotopeSmitheryRequest(final boolean buying, final AdventureResult attachment) {
    super(ISOTOPE_SMITHERY, buying, attachment);
  }

  public IsotopeSmitheryRequest(final boolean buying, final int itemId, final int quantity) {
    super(ISOTOPE_SMITHERY, buying, itemId, quantity);
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || urlString.indexOf("whichshop=elvishp1") == -1) {
      return false;
    }

    return CoinMasterRequest.registerRequest(ISOTOPE_SMITHERY, urlString, true);
  }

  public static String accessible() {
    return SpaaaceRequest.accessible();
  }

  @Override
  public void equip() {
    SpaaaceRequest.equip();
  }
}
