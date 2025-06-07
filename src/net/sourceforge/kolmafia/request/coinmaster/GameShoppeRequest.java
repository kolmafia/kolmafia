package net.sourceforge.kolmafia.request.coinmaster;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class GameShoppeRequest extends CoinMasterRequest {
  public static final String master = "Game Shoppe";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("You currently have ([\\d,]+) store credit");

  public static final CoinmasterData GAMESHOPPE =
      new CoinmasterData(master, "gameshoppe", GameShoppeRequest.class)
          .withToken("store credit")
          .withTokenTest("You currently have no store credit")
          .withTokenPattern(TOKEN_PATTERN)
          .withProperty("availableStoreCredits")
          .withBuyURL("gamestore.php")
          .withBuyAction("redeem")
          .withBuyItems(master)
          .withBuyPrices(master)
          .withSellURL("gamestore.php")
          .withSellAction("tradein")
          .withSellItems(master)
          .withSellPrices(master)
          .withItemField("whichitem")
          .withItemPattern(GenericRequest.WHICHITEM_PATTERN)
          .withCountField("quantity")
          .withCountPattern(GenericRequest.QUANTITY_PATTERN)
          .withCanBuy(GameShoppeRequest::canBuy)
          .withAccessible(GameShoppeRequest::accessible);

  public GameShoppeRequest() {
    super(GAMESHOPPE);
  }

  public GameShoppeRequest(final boolean buying, final AdventureResult[] attachments) {
    super(GAMESHOPPE, buying, attachments);
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
    parseResponse(this.getURLString(), this.responseText);
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
        String name = matcher.group(3).trim();
        String data = ItemDatabase.getItemDataName(id);
        // String price = matcher.group(4);
        if (data == null || !data.equals(name)) {
          ItemDatabase.registerItem(id, name, desc);
        }
      }
    }

    parseGameShoppeVisit(urlString, responseText);
  }

  public static void parseGameShoppeVisit(final String location, final String responseText) {
    String action = GenericRequest.getAction(location);
    if (action == null) {
      if (location.indexOf("place=cashier") == -1) {
        return;
      }
    } else if (action.equals("redeem")) {
      CoinmasterData data = GAMESHOPPE;
      if (responseText.indexOf("You don't have enough") == -1) {
        CoinMasterRequest.completePurchase(data, location);
      }
    } else if (action.equals("tradein")) {
      CoinmasterData data = GAMESHOPPE;
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
    CoinmasterData data = GAMESHOPPE;
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

    return CoinMasterRequest.registerRequest(GAMESHOPPE, urlString);
  }
}
