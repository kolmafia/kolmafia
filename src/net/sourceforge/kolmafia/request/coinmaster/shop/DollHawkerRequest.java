package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.request.SpaaaceRequest;

public abstract class DollHawkerRequest extends CoinMasterShopRequest {
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
          .withEquip(SpaaaceRequest::equip)
          .withAccessible(SpaaaceRequest::accessible);
}
