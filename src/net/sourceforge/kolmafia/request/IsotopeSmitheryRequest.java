package net.sourceforge.kolmafia.request;

import java.util.Map;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class IsotopeSmitheryRequest extends CoinMasterRequest {
  public static final String master = "Isotope Smithery";
  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(IsotopeSmitheryRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(IsotopeSmitheryRequest.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(IsotopeSmitheryRequest.master);

  public static final CoinmasterData ISOTOPE_SMITHERY =
      new CoinmasterData(
          IsotopeSmitheryRequest.master,
          "isotopesmithery",
          IsotopeSmitheryRequest.class,
          "isotope",
          "You have 0 lunar isotopes",
          false,
          SpaaaceRequest.TOKEN_PATTERN,
          SpaaaceRequest.ISOTOPE,
          null,
          IsotopeSmitheryRequest.itemRows,
          "shop.php?whichshop=elvishp1",
          "buyitem",
          IsotopeSmitheryRequest.buyItems,
          IsotopeSmitheryRequest.buyPrices,
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

  public IsotopeSmitheryRequest() {
    super(IsotopeSmitheryRequest.ISOTOPE_SMITHERY);
  }

  public IsotopeSmitheryRequest(final boolean buying, final AdventureResult[] attachments) {
    super(IsotopeSmitheryRequest.ISOTOPE_SMITHERY, buying, attachments);
  }

  public IsotopeSmitheryRequest(final boolean buying, final AdventureResult attachment) {
    super(IsotopeSmitheryRequest.ISOTOPE_SMITHERY, buying, attachment);
  }

  public IsotopeSmitheryRequest(final boolean buying, final int itemId, final int quantity) {
    super(IsotopeSmitheryRequest.ISOTOPE_SMITHERY, buying, itemId, quantity);
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || urlString.indexOf("whichshop=elvishp1") == -1) {
      return false;
    }

    CoinmasterData data = IsotopeSmitheryRequest.ISOTOPE_SMITHERY;
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
