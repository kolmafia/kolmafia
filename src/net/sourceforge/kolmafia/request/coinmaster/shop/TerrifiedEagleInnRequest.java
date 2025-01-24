package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TerrifiedEagleInnRequest extends CoinMasterRequest {
  public static final String master = "The Terrified Eagle Inn";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("<td>(\\w+) Freddy Kruegerand(?:s?)</td>");
  public static final AdventureResult KRUEGERAND = ItemPool.get(ItemPool.KRUEGERAND, 1);

  public static final CoinmasterData TERRIFIED_EAGLE_INN =
      new CoinmasterData(master, "dreadsylvania", TerrifiedEagleInnRequest.class)
          .withToken("Freddy Kruegerand")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(KRUEGERAND)
          .withShopRowFields(master, "dv")
          .withCanBuyItem(TerrifiedEagleInnRequest::canBuyItem);

  private static Boolean canBuyItem(final Integer itemId) {
    return switch (itemId) {
      case ItemPool.TALES_OF_DREAD -> !Preferences.getBoolean("itemBoughtPerCharacter6423");
      case ItemPool.BRASS_DREAD_FLASK -> !Preferences.getBoolean("itemBoughtPerCharacter6428");
      case ItemPool.SILVER_DREAD_FLASK -> !Preferences.getBoolean("itemBoughtPerCharacter6429");
      case ItemPool.FOLDER_21 -> KoLCharacter.hasEquipped(EquipmentManager.FOLDER_HOLDER)
          || KoLCharacter.hasEquipped(EquipmentManager.REPLICA_FOLDER_HOLDER);
      default -> ItemPool.get(itemId).getCount(TERRIFIED_EAGLE_INN.getBuyItems()) > 0;
    };
  }

  public TerrifiedEagleInnRequest() {
    super(TERRIFIED_EAGLE_INN);
  }

  public TerrifiedEagleInnRequest(final boolean buying, final AdventureResult[] attachments) {
    super(TERRIFIED_EAGLE_INN, buying, attachments);
  }

  public TerrifiedEagleInnRequest(final boolean buying, final AdventureResult attachment) {
    super(TERRIFIED_EAGLE_INN, buying, attachment);
  }

  public TerrifiedEagleInnRequest(final boolean buying, final int itemId, final int quantity) {
    super(TERRIFIED_EAGLE_INN, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  private static final Pattern ITEM_PATTERN =
      Pattern.compile(
          "name=whichrow value=(\\d*).*?<a onClick='javascript:descitem\\((\\d+)\\)'><b>(.*?)</b>.*?</a>.*?<b>([,\\d]*)</b>");

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=dv")) {
      return;
    }

    CoinmasterData data = TERRIFIED_EAGLE_INN;

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
      String itemName = matcher.group(3).trim();
      int price = StringUtilities.parseInt(matcher.group(4));

      String match = ItemDatabase.getItemName(descId);
      if (match == null) {
        // Unfortunately, there is no itemId in the table.
        // ItemDatabase.registerItem( itemId, itemName, descId );

        // Print what goes in coinmasters.txt
        String message =
            KoLConstants.LINE_BREAK + master + "\tbuy\t" + price + "\t" + itemName + "\tROW" + row;
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

    return CoinMasterRequest.registerRequest(TERRIFIED_EAGLE_INN, urlString, true);
  }
}
