package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;

public class DollHawkerRequest extends CoinMasterRequest {
  public static final String master = "Dollhawker's Emporium";

  public static final CoinmasterData DOLLHAWKER =
      new CoinmasterData(master, "dollhawker", DollHawkerRequest.class)
          .withToken("isotope")
          .withTokenTest("You have 0 lunar isotopes")
          .withTokenPattern(SpaaaceRequest.TOKEN_PATTERN)
          .withItem(SpaaaceRequest.ISOTOPE)
          .withShopRowFields(master, "elvishp2");

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

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || urlString.indexOf("whichshop=elvishp2") == -1) {
      return false;
    }

    return CoinMasterRequest.registerRequest(DOLLHAWKER, urlString, true);
  }

  public static String accessible() {
    return SpaaaceRequest.accessible();
  }

  @Override
  public void equip() {
    SpaaaceRequest.equip();
  }
}
