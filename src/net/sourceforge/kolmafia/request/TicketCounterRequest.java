package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TicketCounterRequest extends CoinMasterRequest {
  public static final String master = "Arcade Ticket Counter";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("You currently have ([\\d,]+) Game Grid redemption ticket");
  public static final AdventureResult TICKET = ItemPool.get(ItemPool.GG_TICKET, 1);

  public static final CoinmasterData TICKET_COUNTER =
      new CoinmasterData(master, "arcade", TicketCounterRequest.class)
          .withToken("ticket")
          .withTokenTest("You currently have no Game Grid redemption tickets")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(TICKET)
          .withShopRowFields(master, "arcade")
          .withCanBuyItem(TicketCounterRequest::canBuyItem);

  private static Boolean canBuyItem(final Integer itemId) {
    return switch (itemId) {
      case ItemPool.FOLDER_14 -> KoLCharacter.hasEquipped(EquipmentManager.FOLDER_HOLDER);
      case ItemPool.SINISTER_DEMON_MASK,
          ItemPool.CHAMPION_BELT,
          ItemPool.SPACE_TRIP_HEADPHONES,
          ItemPool.DUNGEON_FIST_GAUNTLET,
          ItemPool.METEOID_ICE_BEAM -> !Preferences.getBoolean("lockedItem" + itemId);
      default -> ItemPool.get(itemId).getCount(TICKET_COUNTER.getBuyItems()) > 0;
    };
  }

  public TicketCounterRequest() {
    super(TICKET_COUNTER);
  }

  public TicketCounterRequest(final boolean buying, final AdventureResult[] attachments) {
    super(TICKET_COUNTER, buying, attachments);
  }

  public TicketCounterRequest(final boolean buying, final AdventureResult attachment) {
    super(TICKET_COUNTER, buying, attachment);
  }

  public TicketCounterRequest(final boolean buying, final int itemId, final int quantity) {
    super(TICKET_COUNTER, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
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

  public static boolean parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=arcade")) {
      return false;
    }
    // Learn new trade items by simply visiting Arcade
    Matcher matcher = ITEM_PATTERN.matcher(responseText);
    while (matcher.find()) {
      int id = StringUtilities.parseInt(matcher.group(1));
      for (int i = 0; i < unlockables.length; i++) {
        if (id == unlockables[i]) {
          Preferences.setBoolean("lockedItem" + id, false);
          break;
        }
      }
      String desc = matcher.group(2);
      String name = matcher.group(3).trim();
      String data = ItemDatabase.getItemDataName(id);
      // String price = matcher.group(4);
      if (data == null || !data.equals(name)) {
        ItemDatabase.registerItem(id, name, desc);
      }
    }

    CoinMasterRequest.parseResponse(TICKET_COUNTER, urlString, responseText);

    return true;
  }

  public static String accessible() {
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    // We only claim arcade.php?action=redeem
    if (!urlString.contains("whichshop=arcade")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(TICKET_COUNTER, urlString);
  }
}
