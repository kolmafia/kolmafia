package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class TicketCounterRequest extends CoinMasterShopRequest {
  public static final String master = "Arcade Ticket Counter";
  public static final String SHOPID = "arcade";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("You currently have ([\\d,]+) Game Grid redemption ticket");
  public static final AdventureResult TICKET = ItemPool.get(ItemPool.GG_TICKET, 1);

  public static final CoinmasterData TICKET_COUNTER =
      new CoinmasterData(master, "arcade", TicketCounterRequest.class)
          .withToken("ticket")
          .withTokenTest("You currently have no Game Grid redemption tickets")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(TICKET)
          .withShopRowFields(master, SHOPID)
          .withCanBuyItem(TicketCounterRequest::canBuyItem)
          .withVisitShop(TicketCounterRequest::visitShop)
          .withAccessible(TicketCounterRequest::accessible);

  private static Boolean canBuyItem(final Integer itemId) {
    return switch (itemId) {
      case ItemPool.FOLDER_14 -> KoLCharacter.hasEquipped(EquipmentManager.FOLDER_HOLDER)
          || KoLCharacter.hasEquipped(EquipmentManager.REPLICA_FOLDER_HOLDER);
      case ItemPool.SINISTER_DEMON_MASK,
          ItemPool.CHAMPION_BELT,
          ItemPool.SPACE_TRIP_HEADPHONES,
          ItemPool.DUNGEON_FIST_GAUNTLET,
          ItemPool.METEOID_ICE_BEAM -> !Preferences.getBoolean("lockedItem" + itemId);
      default -> ItemPool.get(itemId).getCount(TICKET_COUNTER.getBuyItems()) > 0;
    };
  }

  private static final Pattern ITEM_PATTERN =
      Pattern.compile("<tr rel=\"(\\d+)\".*?descitem\\((\\d+).*?<b>(.*?)</b>");

  private static final int[] unlockables = {
    ItemPool.SINISTER_DEMON_MASK,
    ItemPool.CHAMPION_BELT,
    ItemPool.SPACE_TRIP_HEADPHONES,
    ItemPool.METEOID_ICE_BEAM,
    ItemPool.DUNGEON_FIST_GAUNTLET,
  };

  public static void visitShop(String responseText) {
    // Learn new trade items by simply visiting Arcade
    Matcher matcher = ITEM_PATTERN.matcher(responseText);
    while (matcher.find()) {
      int id = StringUtilities.parseInt(matcher.group(1));
      for (int i = 0; i < unlockables.length; i++) {
        if (id == unlockables[i]) {
          Preferences.setBoolean("lockedItem" + id, false);
        }
      }
    }
  }

  public static String accessible() {
    // *** Finish this.
    return null;
  }
}
