package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.request.SpaaaceRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class LunarLunchRequest extends CoinMasterRequest {
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
          .withAccessible(SpaaaceRequest::accessible);

  public LunarLunchRequest() {
    super(LUNAR_LUNCH);
  }

  public LunarLunchRequest(final boolean buying, final AdventureResult[] attachments) {
    super(LUNAR_LUNCH, buying, attachments);
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
