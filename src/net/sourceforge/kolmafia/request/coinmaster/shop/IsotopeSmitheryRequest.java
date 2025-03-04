package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.request.SpaaaceRequest;

public abstract class IsotopeSmitheryRequest extends CoinMasterShopRequest {
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
          .withEquip(SpaaaceRequest::equip)
          .withAccessible(SpaaaceRequest::accessible);
}
