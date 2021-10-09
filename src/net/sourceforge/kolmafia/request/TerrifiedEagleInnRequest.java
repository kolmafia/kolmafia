package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TerrifiedEagleInnRequest extends CoinMasterRequest {
  public static final String master = "The Terrified Eagle Inn";
  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(TerrifiedEagleInnRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(TerrifiedEagleInnRequest.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(TerrifiedEagleInnRequest.master);

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("<td>(\\w+) Freddy Kruegerand(?:s?)</td>");
  public static final AdventureResult KRUEGERAND = ItemPool.get(ItemPool.KRUEGERAND, 1);
  public static final CoinmasterData TERRIFIED_EAGLE_INN =
      new CoinmasterData(
          TerrifiedEagleInnRequest.master,
          "dreadsylvania",
          TerrifiedEagleInnRequest.class,
          "Freddy Kruegerand",
          null,
          false,
          TerrifiedEagleInnRequest.TOKEN_PATTERN,
          TerrifiedEagleInnRequest.KRUEGERAND,
          null,
          TerrifiedEagleInnRequest.itemRows,
          "shop.php?whichshop=dv",
          "buyitem",
          TerrifiedEagleInnRequest.buyItems,
          TerrifiedEagleInnRequest.buyPrices,
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
            case ItemPool.TALES_OF_DREAD:
              return !Preferences.getBoolean("itemBoughtPerCharacter6423");
            case ItemPool.BRASS_DREAD_FLASK:
              return !Preferences.getBoolean("itemBoughtPerCharacter6428");
            case ItemPool.SILVER_DREAD_FLASK:
              return !Preferences.getBoolean("itemBoughtPerCharacter6429");
            case ItemPool.FOLDER_21:
              return KoLCharacter.hasEquipped(EquipmentManager.FOLDER_HOLDER);
          }
          return super.canBuyItem(itemId);
        }
      };

  public TerrifiedEagleInnRequest() {
    super(TerrifiedEagleInnRequest.TERRIFIED_EAGLE_INN);
  }

  public TerrifiedEagleInnRequest(final boolean buying, final AdventureResult[] attachments) {
    super(TerrifiedEagleInnRequest.TERRIFIED_EAGLE_INN, buying, attachments);
  }

  public TerrifiedEagleInnRequest(final boolean buying, final AdventureResult attachment) {
    super(TerrifiedEagleInnRequest.TERRIFIED_EAGLE_INN, buying, attachment);
  }

  public TerrifiedEagleInnRequest(final boolean buying, final int itemId, final int quantity) {
    super(TerrifiedEagleInnRequest.TERRIFIED_EAGLE_INN, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    TerrifiedEagleInnRequest.parseResponse(this.getURLString(), this.responseText);
  }

  private static final Pattern ITEM_PATTERN =
      Pattern.compile(
          "name=whichrow value=(\\d*).*?<a onClick='javascript:descitem\\((\\d+)\\)'><b>(.*?)</b>.*?</a>.*?<b>([,\\d]*)</b>");

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=dv")) {
      return;
    }

    CoinmasterData data = TerrifiedEagleInnRequest.TERRIFIED_EAGLE_INN;

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, urlString, responseText);
      return;
    }

    // Debug: look for new items every time we visit the shop
    Matcher matcher = ITEM_PATTERN.matcher(responseText);
    while (matcher.find()) {
      String row = matcher.group(1);
      String descId = matcher.group(2);
      String itemName = matcher.group(3);
      int price = StringUtilities.parseInt(matcher.group(4));

      String match = ItemDatabase.getItemName(descId);
      if (match == null) {
        // Unfortunately, there is no itemId in the table.
        // ItemDatabase.registerItem( itemId, itemName, descId );

        // Print what goes in coinmasters.txt
        String message =
            KoLConstants.LINE_BREAK
                + TerrifiedEagleInnRequest.master
                + "\tbuy\t"
                + price
                + "\t"
                + itemName
                + "\tROW"
                + row;
        RequestLogger.printLine(message);
        RequestLogger.updateSessionLog(message);
      }
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    return null;
  }

  public static boolean registerRequest(final String urlString) {
    // shop.php?pwd&whichshop=dv
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=dv")) {
      return false;
    }

    CoinmasterData data = TerrifiedEagleInnRequest.TERRIFIED_EAGLE_INN;
    return CoinMasterRequest.registerRequest(data, urlString, true);
  }
}
