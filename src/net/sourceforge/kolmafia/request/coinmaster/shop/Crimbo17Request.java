package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class Crimbo17Request extends CoinMasterRequest {
  public static final String master = "Cheer-o-Vend 3000";
  public static final String SHOPID = "crimbo17";

  private static final Pattern CHEER_PATTERN = Pattern.compile("([\\d,]+) crystalline cheer");
  public static final AdventureResult CHEER = ItemPool.get(ItemPool.CRYSTALLINE_CHEER, 1);

  public static final CoinmasterData CRIMBO17 =
      new CoinmasterData(master, "crimbo17", Crimbo17Request.class)
          .inZone("Crimbo17")
          .withToken("crystalline cheer")
          .withTokenTest("no crystalline cheer")
          .withTokenPattern(CHEER_PATTERN)
          .withItem(CHEER)
          .withShopRowFields(master, SHOPID)
          .withCanBuyItem(Crimbo17Request::canBuyItem)
          .withAccessible(Crimbo17Request::accessible);

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

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static String accessible() {
    int cheer = CHEER.getCount(KoLConstants.inventory);
    if (cheer == 0) {
      return "You need some crystalline cheer.";
    }
    return null;
  }
}
