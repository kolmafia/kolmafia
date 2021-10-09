package net.sourceforge.kolmafia.request;

import java.util.Map;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class DollHawkerRequest extends CoinMasterRequest {
  public static final String master = "Dollhawker's Emporium";
  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(DollHawkerRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(DollHawkerRequest.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(DollHawkerRequest.master);

  public static final CoinmasterData DOLLHAWKER =
      new CoinmasterData(
          DollHawkerRequest.master,
          "dollhawker",
          DollHawkerRequest.class,
          "isotope",
          "You have 0 lunar isotopes",
          false,
          SpaaaceRequest.TOKEN_PATTERN,
          SpaaaceRequest.ISOTOPE,
          null,
          DollHawkerRequest.itemRows,
          "shop.php?whichshop=elvishp2",
          "buyitem",
          DollHawkerRequest.buyItems,
          DollHawkerRequest.buyPrices,
          null,
          null,
          null,
          null,
          "whichrow",
          GenericRequest.WHICHROW_PATTERN,
          "quantity",
          GenericRequest.QUANTITY_PATTERN,
          null,
          null,
          true);

  public DollHawkerRequest() {
    super(DollHawkerRequest.DOLLHAWKER);
  }

  public DollHawkerRequest(final boolean buying, final AdventureResult[] attachments) {
    super(DollHawkerRequest.DOLLHAWKER, buying, attachments);
  }

  public DollHawkerRequest(final boolean buying, final AdventureResult attachment) {
    super(DollHawkerRequest.DOLLHAWKER, buying, attachment);
  }

  public DollHawkerRequest(final boolean buying, final int itemId, final int quantity) {
    super(DollHawkerRequest.DOLLHAWKER, buying, itemId, quantity);
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || urlString.indexOf("whichshop=elvishp2") == -1) {
      return false;
    }

    CoinmasterData data = DollHawkerRequest.DOLLHAWKER;
    return CoinMasterRequest.registerRequest(data, urlString, true);
  }

  public static String accessible() {
    return SpaaaceRequest.accessible();
  }

  @Override
  public void equip() {
    SpaaaceRequest.equip();
  }
}
