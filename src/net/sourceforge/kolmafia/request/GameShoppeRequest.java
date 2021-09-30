package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class GameShoppeRequest extends CoinMasterRequest {
  public static final String master = "Game Shoppe";
  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(GameShoppeRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(GameShoppeRequest.master);
  private static final LockableListModel<AdventureResult> sellItems =
      CoinmastersDatabase.getSellItems(GameShoppeRequest.master);
  private static final Map<Integer, Integer> sellPrices =
      CoinmastersDatabase.getSellPrices(GameShoppeRequest.master);

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("You currently have ([\\d,]+) store credit");
  public static final CoinmasterData GAMESHOPPE =
      new CoinmasterData(
          GameShoppeRequest.master,
          "gameshoppe",
          GameShoppeRequest.class,
          "store credit",
          "You currently have no store credit",
          false,
          GameShoppeRequest.TOKEN_PATTERN,
          null,
          "availableStoreCredits",
          null,
          "gamestore.php",
          "redeem",
          GameShoppeRequest.buyItems,
          GameShoppeRequest.buyPrices,
          "gamestore.php",
          "tradein",
          GameShoppeRequest.sellItems,
          GameShoppeRequest.sellPrices,
          "whichitem",
          GenericRequest.WHICHITEM_PATTERN,
          "quantity",
          GenericRequest.QUANTITY_PATTERN,
          null,
          null,
          true);

  static {
    ConcoctionPool.set(new Concoction("store credit", "availableStoreCredits"));
  }

  public GameShoppeRequest() {
    super(GameShoppeRequest.GAMESHOPPE);
  }

  public GameShoppeRequest(final boolean buying, final AdventureResult[] attachments) {
    super(GameShoppeRequest.GAMESHOPPE, buying, attachments);
  }

  public GameShoppeRequest(final boolean buying, final AdventureResult attachment) {
    super(GameShoppeRequest.GAMESHOPPE, buying, attachment);
  }

  public GameShoppeRequest(final boolean buying, final int itemId, final int quantity) {
    super(GameShoppeRequest.GAMESHOPPE, buying, itemId, quantity);
  }

  public static String canBuy() {
    if (KoLCharacter.isHardcore()) {
      return "You are in Hardcore and the credit reader is broken.";
    }

    if (KoLCharacter.inRonin()) {
      return "You are in Ronin and the credit reader is broken.";
    }

    return null;
  }

  @Override
  public void processResults() {
    GameShoppeRequest.parseResponse(this.getURLString(), this.responseText);
  }

  private static final Pattern ITEM_PATTERN =
      Pattern.compile(
          "name=whichitem value=([\\d]+)>.*?descitem.([\\d]+).*?<b>([^<&]*)(?:&nbsp;)*</td>.*?<b>([\\d,]+) credit</b>",
          Pattern.DOTALL);

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("gamestore.php")) {
      return;
    }

    if (urlString.indexOf("place=cashier") != -1) {
      // Learn new trade items by simply visiting GameShoppe
      Matcher matcher = ITEM_PATTERN.matcher(responseText);
      while (matcher.find()) {
        int id = StringUtilities.parseInt(matcher.group(1));
        String desc = matcher.group(2);
        String name = matcher.group(3);
        String data = ItemDatabase.getItemDataName(id);
        // String price = matcher.group(4);
        if (data == null || !data.equals(name)) {
          ItemDatabase.registerItem(id, name, desc);
        }
      }
    }

    GameShoppeRequest.parseGameShoppeVisit(urlString, responseText);
  }

  public static void parseGameShoppeVisit(final String location, final String responseText) {
    String action = GenericRequest.getAction(location);
    if (action == null) {
      if (location.indexOf("place=cashier") == -1) {
        return;
      }
    } else if (action.equals("redeem")) {
      CoinmasterData data = GameShoppeRequest.GAMESHOPPE;
      if (responseText.indexOf("You don't have enough") == -1) {
        CoinMasterRequest.completePurchase(data, location);
      }
    } else if (action.equals("tradein")) {
      CoinmasterData data = GameShoppeRequest.GAMESHOPPE;
      // The teenager scowls. "You can't trade in cards you don't have."
      if (responseText.indexOf("You can't trade in cards you don't have") == -1) {
        CoinMasterRequest.completeSale(data, location);
      }
    } else if (action.equals("buysnack")) {
      FreeSnackRequest.parseFreeSnackVisit(location, responseText);
    } else {
      // Some other action not associated with the cashier
      return;
    }

    // Parse current store credit and free snack balance
    CoinmasterData data = GameShoppeRequest.GAMESHOPPE;
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    if (KoLCharacter.isHardcore() || KoLCharacter.inRonin()) {
      return "Characters in Hardcore or Ronin cannot redeem Game Shoppe credit.";
    }
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("gamestore.php")) {
      return false;
    }

    String message = null;

    if (urlString.indexOf("action") == -1 && urlString.indexOf("place=cashier") != -1) {
      message = "Visiting Game Shoppe Cashier";
      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog(message);
    }

    CoinmasterData data = GameShoppeRequest.GAMESHOPPE;
    return CoinMasterRequest.registerRequest(data, urlString);
  }
}
