package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;

public class Crimbo20BoozeRequest extends CoinMasterRequest {
  public static final String master = "Elf Booze Drive";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("([\\d,]+) (boxes of )?donated booze");
  public static final AdventureResult TOKEN = ItemPool.get(ItemPool.DONATED_BOOZE, 1);

  public static final CoinmasterData CRIMBO20BOOZE =
      new CoinmasterData(master, "crimbo20booze", Crimbo20BoozeRequest.class)
          .withToken("donated booze")
          .withTokenTest("no boxes of donated booze")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(TOKEN)
          .withShopRowFields(master, "crimbo20booze")
          .withNeedsPasswordHash(true)
          .withCanBuyItem(Crimbo20BoozeRequest::canBuyItem);

  private static Boolean canBuyItem(final Integer itemId) {
    AdventureResult item = ItemPool.get(itemId);
    return switch (itemId) {
      case ItemPool.BOOZE_DRIVE_BUTTON, ItemPool.BOOZE_MAILING_LIST -> item.getCount(
                  KoLConstants.closet)
              + item.getCount(KoLConstants.inventory)
          == 0;
      default -> item.getCount(CRIMBO20BOOZE.getBuyItems()) > 0;
    };
  }

  public Crimbo20BoozeRequest() {
    super(CRIMBO20BOOZE);
  }

  public Crimbo20BoozeRequest(final boolean buying, final AdventureResult[] attachments) {
    super(CRIMBO20BOOZE, buying, attachments);
  }

  public Crimbo20BoozeRequest(final boolean buying, final AdventureResult attachment) {
    super(CRIMBO20BOOZE, buying, attachment);
  }

  public Crimbo20BoozeRequest(final boolean buying, final int itemId, final int quantity) {
    super(CRIMBO20BOOZE, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=crimbo20booze")) {
      return;
    }

    CoinmasterData data = CRIMBO20BOOZE;

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
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=crimbo20booze")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(CRIMBO20BOOZE, urlString, true);
  }
}
