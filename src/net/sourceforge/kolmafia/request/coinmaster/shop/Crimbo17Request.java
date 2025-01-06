package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;

public class Crimbo17Request extends CoinMasterRequest {
  public static final String master = "Cheer-o-Vend 3000";

  private static final Pattern CHEER_PATTERN = Pattern.compile("([\\d,]+) crystalline cheer");
  public static final AdventureResult CHEER = ItemPool.get(ItemPool.CRYSTALLINE_CHEER, 1);

  public static final CoinmasterData CRIMBO17 =
      new CoinmasterData(master, "crimbo17", Crimbo17Request.class)
          .withToken("crystalline cheer")
          .withTokenTest("no crystalline cheer")
          .withTokenPattern(CHEER_PATTERN)
          .withItem(CHEER)
          .withShopRowFields(master, "crimbo17")
          .withNeedsPasswordHash(true)
          .withCanBuyItem(Crimbo17Request::canBuyItem);

  private static Boolean canBuyItem(final Integer itemId) {
    return switch (itemId) {
      case ItemPool.MIME_SCIENCE_VOL_1 -> KoLCharacter.isSealClubber();
      case ItemPool.MIME_SCIENCE_VOL_2 -> KoLCharacter.isTurtleTamer();
      case ItemPool.MIME_SCIENCE_VOL_3 -> KoLCharacter.isPastamancer();
      case ItemPool.MIME_SCIENCE_VOL_4 -> KoLCharacter.isSauceror();
      case ItemPool.MIME_SCIENCE_VOL_5 -> KoLCharacter.isDiscoBandit();
      case ItemPool.MIME_SCIENCE_VOL_6 -> KoLCharacter.isAccordionThief();
      default -> ItemPool.get(itemId).getCount(CRIMBO17.getBuyItems()) > 0;
    };
  }

  public Crimbo17Request() {
    super(CRIMBO17);
  }

  public Crimbo17Request(final boolean buying, final AdventureResult[] attachments) {
    super(CRIMBO17, buying, attachments);
  }

  public Crimbo17Request(final boolean buying, final AdventureResult attachment) {
    super(CRIMBO17, buying, attachment);
  }

  public Crimbo17Request(final boolean buying, final int itemId, final int quantity) {
    super(CRIMBO17, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=crimbo17")) {
      return;
    }

    CoinmasterData data = CRIMBO17;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    int cheer = CHEER.getCount(KoLConstants.inventory);
    if (cheer == 0) {
      return "You need some crystalline cheer.";
    }
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=crimbo17")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(CRIMBO17, urlString, true);
  }
}
