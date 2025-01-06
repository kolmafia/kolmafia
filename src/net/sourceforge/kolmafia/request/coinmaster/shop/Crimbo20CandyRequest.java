package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;

public class Crimbo20CandyRequest extends CoinMasterRequest {
  public static final String master = "Elf Candy Drive";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("([\\d,]+) (boxes of )?donated candy");
  public static final AdventureResult TOKEN = ItemPool.get(ItemPool.DONATED_CANDY, 1);

  public static final CoinmasterData CRIMBO20CANDY =
      new CoinmasterData(master, "crimbo20candy", Crimbo20CandyRequest.class)
          .withToken("donated candy")
          .withTokenTest("no boxes of donated candy")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(TOKEN)
          .withShopRowFields(master, "crimbo20candy")
          .withNeedsPasswordHash(true)
          .withCanBuyItem(Crimbo20CandyRequest::canBuyItem);

  private static Boolean canBuyItem(final Integer itemId) {
    AdventureResult item = ItemPool.get(itemId);
    return switch (itemId) {
      case ItemPool.CANDY_DRIVE_BUTTON, ItemPool.CANDY_MAILING_LIST -> item.getCount(
                  KoLConstants.closet)
              + item.getCount(KoLConstants.inventory)
          == 0;
      default -> item.getCount(CRIMBO20CANDY.getBuyItems()) > 0;
    };
  }

  public Crimbo20CandyRequest() {
    super(CRIMBO20CANDY);
  }

  public Crimbo20CandyRequest(final boolean buying, final AdventureResult[] attachments) {
    super(CRIMBO20CANDY, buying, attachments);
  }

  public Crimbo20CandyRequest(final boolean buying, final AdventureResult attachment) {
    super(CRIMBO20CANDY, buying, attachment);
  }

  public Crimbo20CandyRequest(final boolean buying, final int itemId, final int quantity) {
    super(CRIMBO20CANDY, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=crimbo20candy")) {
      return;
    }

    CoinmasterData data = CRIMBO20CANDY;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    return "Crimbo is gone";
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=crimbo20candy")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(CRIMBO20CANDY, urlString, true);
  }
}
