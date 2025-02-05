package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.request.SpaaaceRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class DollHawkerRequest extends CoinMasterRequest {
  public static final String master = "Dollhawker's Emporium";
  public static final String SHOPID = "elvishp2";

  public static final CoinmasterData DOLLHAWKER =
      new CoinmasterData(master, "dollhawker", DollHawkerRequest.class)
          .withToken("isotope")
          .withTokenTest("You have 0 lunar isotopes")
          .withTokenPattern(SpaaaceRequest.TOKEN_PATTERN)
          .withItem(SpaaaceRequest.ISOTOPE)
          .withShopRowFields(master, SHOPID)
          .withVisitShop(SpaaaceRequest::visitShop)
          .withAccessible(SpaaaceRequest::accessible);

  public DollHawkerRequest() {
    super(DOLLHAWKER);
  }

  public DollHawkerRequest(final boolean buying, final AdventureResult[] attachments) {
    super(DOLLHAWKER, buying, attachments);
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
