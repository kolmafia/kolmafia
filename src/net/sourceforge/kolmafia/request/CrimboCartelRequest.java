package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public class CrimboCartelRequest extends CoinMasterRequest {
  public static final String master = "Crimbo Cartel";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("You currently have <b>([\\d,]+)</b> Crimbux");
  public static final AdventureResult CRIMBUCK = ItemPool.get(ItemPool.CRIMBUCK, 1);

  public static final CoinmasterData CRIMBO_CARTEL =
      new CoinmasterData(master, "cartel", CrimboCartelRequest.class) {}.withToken("Crimbuck")
          .withTokenTest("You do not currently have any Crimbux")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(CRIMBUCK)
          .withBuyURL("crimbo09.php")
          .withBuyAction("buygift")
          .withBuyItems(master)
          .withBuyPrices(master)
          .withItemField("whichitem")
          .withItemPattern(GenericRequest.WHICHITEM_PATTERN)
          .withCountField("howmany")
          .withCountPattern(GenericRequest.HOWMANY_PATTERN)
          .withAvailableItem(CrimboCartelRequest::availableItem);

  private static Boolean availableItem(final Integer itemId) {
    return switch (itemId) {
      case ItemPool.CRIMBO_CAROL_V1 -> KoLCharacter.isSealClubber();
      case ItemPool.CRIMBO_CAROL_V2 -> KoLCharacter.isTurtleTamer();
      case ItemPool.CRIMBO_CAROL_V3 -> KoLCharacter.isPastamancer();
      case ItemPool.CRIMBO_CAROL_V4 -> KoLCharacter.isSauceror();
      case ItemPool.CRIMBO_CAROL_V5 -> KoLCharacter.isDiscoBandit();
      case ItemPool.CRIMBO_CAROL_V6 -> KoLCharacter.isAccordionThief();
      default -> CRIMBO_CARTEL.getBuyItems().contains(ItemPool.get(itemId));
    };
  }

  public CrimboCartelRequest() {
    super(CRIMBO_CARTEL);
  }

  public CrimboCartelRequest(final boolean buying, final AdventureResult[] attachments) {
    super(CRIMBO_CARTEL, buying, attachments);
  }

  public CrimboCartelRequest(final boolean buying, final AdventureResult attachment) {
    super(CRIMBO_CARTEL, buying, attachment);
  }

  public CrimboCartelRequest(final boolean buying, final int itemId, final int quantity) {
    super(CRIMBO_CARTEL, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    CoinmasterData data = CRIMBO_CARTEL;
    String action = GenericRequest.getAction(location);
    if (action == null) {
      if (location.indexOf("place=store") != -1) {
        // Parse current coin balances
        CoinMasterRequest.parseBalance(data, responseText);
      }

      return;
    }

    CoinMasterRequest.parseResponse(data, location, responseText);
  }

  public static final boolean registerRequest(final String urlString) {
    // We only claim crimbo09.php?action=buygift
    if (!urlString.startsWith("crimbo09.php")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(CRIMBO_CARTEL, urlString);
  }

  public static String accessible() {
    return "The Crimbo Cartel is not available";
  }
}
