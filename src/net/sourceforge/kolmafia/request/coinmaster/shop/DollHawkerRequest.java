package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.request.SpaaaceRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;

public class DollHawkerRequest extends CoinMasterRequest {
  public static final String master = "Dollhawker's Emporium";

  public static final CoinmasterData DOLLHAWKER =
      new CoinmasterData(master, "dollhawker", DollHawkerRequest.class)
          .withToken("isotope")
          .withTokenTest("You have 0 lunar isotopes")
          .withTokenPattern(SpaaaceRequest.TOKEN_PATTERN)
          .withItem(SpaaaceRequest.ISOTOPE)
          .withShopRowFields(master, "elvishp2")
          .withAccessible(SpaaaceRequest::accessible);

  public DollHawkerRequest() {
    super(DOLLHAWKER);
  }

  public DollHawkerRequest(final boolean buying, final AdventureResult[] attachments) {
    super(DOLLHAWKER, buying, attachments);
  }

  public DollHawkerRequest(final boolean buying, final AdventureResult attachment) {
    super(DOLLHAWKER, buying, attachment);
  }

  public DollHawkerRequest(final boolean buying, final int itemId, final int quantity) {
    super(DOLLHAWKER, buying, itemId, quantity);
  }

  @Override
  public void equip() {
    SpaaaceRequest.equip();
  }
}
