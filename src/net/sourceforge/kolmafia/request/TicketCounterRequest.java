package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TicketCounterRequest extends CoinMasterRequest {
  public static final String master = "Arcade Ticket Counter";
  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(TicketCounterRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(TicketCounterRequest.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(TicketCounterRequest.master);

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("You currently have ([\\d,]+) Game Grid redemption ticket");
  public static final AdventureResult TICKET = ItemPool.get(ItemPool.GG_TICKET, 1);
  public static final CoinmasterData TICKET_COUNTER =
      new CoinmasterData(
          TicketCounterRequest.master,
          "arcade",
          TicketCounterRequest.class,
          "ticket",
          "You currently have no Game Grid redemption tickets",
          false,
          TicketCounterRequest.TOKEN_PATTERN,
          TicketCounterRequest.TICKET,
          null,
          TicketCounterRequest.itemRows,
          "shop.php?whichshop=arcade",
          "buyitem",
          TicketCounterRequest.buyItems,
          TicketCounterRequest.buyPrices,
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
          true) {
        @Override
        public final boolean canBuyItem(final int itemId) {
          switch (itemId) {
            case ItemPool.FOLDER_14:
              return KoLCharacter.hasEquipped(EquipmentManager.FOLDER_HOLDER);
            case ItemPool.SINISTER_DEMON_MASK:
            case ItemPool.CHAMPION_BELT:
            case ItemPool.SPACE_TRIP_HEADPHONES:
            case ItemPool.DUNGEON_FIST_GAUNTLET:
            case ItemPool.METEOID_ICE_BEAM:
              return !Preferences.getBoolean("lockedItem" + itemId);
          }
          return super.canBuyItem(itemId);
        }
      };

  public TicketCounterRequest() {
    super(TicketCounterRequest.TICKET_COUNTER);
  }

  public TicketCounterRequest(final boolean buying, final AdventureResult[] attachments) {
    super(TicketCounterRequest.TICKET_COUNTER, buying, attachments);
  }

  public TicketCounterRequest(final boolean buying, final AdventureResult attachment) {
    super(TicketCounterRequest.TICKET_COUNTER, buying, attachment);
  }

  public TicketCounterRequest(final boolean buying, final int itemId, final int quantity) {
    super(TicketCounterRequest.TICKET_COUNTER, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    TicketCounterRequest.parseResponse(this.getURLString(), this.responseText);
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
      for (int i = 0; i < TicketCounterRequest.unlockables.length; i++) {
        if (id == TicketCounterRequest.unlockables[i]) {
          Preferences.setBoolean("lockedItem" + id, false);
          break;
        }
      }
      String desc = matcher.group(2);
      String name = matcher.group(3);
      String data = ItemDatabase.getItemDataName(id);
      // String price = matcher.group(4);
      if (data == null || !data.equals(name)) {
        ItemDatabase.registerItem(id, name, desc);
      }
    }

    CoinMasterRequest.parseResponse(TicketCounterRequest.TICKET_COUNTER, urlString, responseText);

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

    CoinmasterData data = TicketCounterRequest.TICKET_COUNTER;
    return CoinMasterRequest.registerRequest(data, urlString);
  }
}
