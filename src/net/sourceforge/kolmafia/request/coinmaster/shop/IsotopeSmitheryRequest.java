package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.request.SpaaaceRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class IsotopeSmitheryRequest extends CoinMasterRequest {
  public static final String master = "Isotope Smithery";
  public static final String SHOPID = "elvishp1";

  public static final CoinmasterData ISOTOPE_SMITHERY =
      new CoinmasterData(master, "isotopesmithery", IsotopeSmitheryRequest.class)
          .withToken("isotope")
          .withTokenTest("You have 0 lunar isotopes")
          .withTokenPattern(SpaaaceRequest.TOKEN_PATTERN)
          .withItem(SpaaaceRequest.ISOTOPE)
          .withShopRowFields(master, SHOPID)
          .withVisitShop(SpaaaceRequest::visitShop)
          .withAccessible(SpaaaceRequest::accessible);

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

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), this.responseText);
  }

  @Override
  public void equip() {
    SpaaaceRequest.equip();
  }
}
