package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.request.SpaaaceRequest;

public abstract class LunarLunchRequest extends CoinMasterShopRequest {
  public static final String master = "Lunar Lunch-o-Mat";
  public static final String SHOPID = "elvishp3";

  public static final CoinmasterData LUNAR_LUNCH =
      new CoinmasterData(master, "lunarlunch", LunarLunchRequest.class)
          .withToken("isotope")
          .withTokenTest("You have 0 lunar isotopes")
          .withTokenPattern(SpaaaceRequest.TOKEN_PATTERN)
          .withItem(SpaaaceRequest.ISOTOPE)
          .withShopRowFields(master, SHOPID)
          .withVisitShop(SpaaaceRequest::visitShop)
          .withEquip(SpaaaceRequest::equip)
          .withAccessible(SpaaaceRequest::accessible);
}
