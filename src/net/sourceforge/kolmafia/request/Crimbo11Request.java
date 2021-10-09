package net.sourceforge.kolmafia.request;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class Crimbo11Request extends CoinMasterRequest {
  public static final String master = "Crimbo 2011";
  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(Crimbo11Request.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(Crimbo11Request.master);
  private static final LockableListModel<AdventureResult> sellItems =
      CoinmastersDatabase.getSellItems(Crimbo11Request.master);
  private static final Map<Integer, Integer> sellPrices =
      CoinmastersDatabase.getSellPrices(Crimbo11Request.master);
  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("You currently have.*?<b>([\\d,]+)</b> Candy Credit", Pattern.DOTALL);
  public static final CoinmasterData CRIMBO11 =
      new CoinmasterData(
          Crimbo11Request.master,
          "crimbo11",
          Crimbo11Request.class,
          "Candy Credit",
          null,
          false,
          Crimbo11Request.TOKEN_PATTERN,
          null,
          "availableCandyCredits",
          null,
          "crimbo11.php",
          "reallybuygifts",
          Crimbo11Request.buyItems,
          Crimbo11Request.buyPrices,
          "crimbo11.php",
          "tradecandy",
          Crimbo11Request.sellItems,
          Crimbo11Request.sellPrices,
          "whichitem",
          GenericRequest.WHICHITEM_PATTERN,
          "howmany",
          GenericRequest.HOWMANY_PATTERN,
          null,
          null,
          false);

  public Crimbo11Request() {
    super(Crimbo11Request.CRIMBO11);
  }

  public Crimbo11Request(final boolean buying, final AdventureResult[] attachments) {
    super(Crimbo11Request.CRIMBO11, buying, attachments);
  }

  public Crimbo11Request(final boolean buying, final AdventureResult attachment) {
    super(Crimbo11Request.CRIMBO11, buying, attachment);
  }

  public Crimbo11Request(final boolean buying, final int itemId, final int quantity) {
    super(Crimbo11Request.CRIMBO11, buying, itemId, quantity);
  }

  private static String placeString(final String urlString) {
    String place = GenericRequest.getPlace(urlString);
    if (place == null) {
      return null;
    } else if (place.equals("tradeincandy")) {
      return "Uncle Crimbo";
    } else if (place.equals("yourpresents")) {
      return "Your Presents";
    } else if (place.equals("buygifts")) {
      return "Crimbo Town Toy Factory";
    }
    return null;
  }

  @Override
  public void run() {
    this.addFormField("place", "tradeincandy");
    super.run();
  }

  @Override
  public void processResults() {
    Crimbo11Request.parseResponse(this.getURLString(), this.responseText);
  }

  // <b>Results:</b></td></tr><tr><td style="padding: 5px; border: 1px solid
  // blue;"><center><table><tr><td>Invalid gift selected.  Bah Humbug!</td></tr></table>
  private static final Pattern FAILURE_PATTERN =
      Pattern.compile("<b>Results:</b>.*?<table><tr><td>(.*?)</td></tr></table>", Pattern.DOTALL);

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("crimbo11.php")) {
      return;
    }

    Crimbo11Request.parseCrimbo11Visit(urlString, responseText);
  }

  public static void parseCrimbo11Visit(final String location, final String responseText) {
    CoinmasterData data = Crimbo11Request.CRIMBO11;

    String action = GenericRequest.getAction(location);
    if (action == null) {
      String place = GenericRequest.getPlace(location);
      if (place != null && (place.equals("tradeincandy") || place.equals("buygifts"))) {
        // Parse current Candy Credits
        CoinMasterRequest.parseBalance(data, responseText);
      }
      return;
    }

    if (action.equals("reallybuygifts")) {
      // Good choice, quotid, good choice. My elves will make
      // sure, listen. My elves will make sure that present
      // goes where it's supposed to, okay? Now go trade some
      // more candy. We're dyin' over here.
      //
      // Don't worry, quotid, my elves will, listen. My elves
      // will stuff that stocking just right, okay? Now go
      // get some more, listen. Go get some more candy and
      // trade it in, okay?  else
      if (responseText.indexOf("My elves will make sure that present goes where it's supposed to")
              != -1
          || responseText.indexOf("My elves will stuff that stocking just right") != -1) {
        CoinMasterRequest.completePurchase(data, location);
        NamedListenerRegistry.fireChange("(coinmaster)");
      }
      // Your fingers are writing checks that your Crimbo
      // Credit Balance can't cash.
      else if (responseText.indexOf("Your fingers are writing checks") != -1) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You can't afford that");
      }
      // You can't send yourself a present.
      else if (responseText.indexOf("You can't send yourself a present") != -1) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You can't send yourself a present");
      }
      // The factory workers inform you that your intended
      // recipient already has one of those.
      else if (responseText.indexOf("already has one of those") != -1) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "That person already has that gift");
      }
      // Invalid gift selected.  Bah Humbug!
      else if (responseText.indexOf("Invalid gift selected") != -1) {
        Matcher itemMatcher = data.getItemMatcher(location);
        String itemId = itemMatcher.find() ? itemMatcher.group(1) : "unknown";
        KoLmafia.updateDisplay(MafiaState.ERROR, "Item #" + itemId + " is not a valid gift");
      } else {
        Matcher failureMatcher = Crimbo11Request.FAILURE_PATTERN.matcher(responseText);
        String message =
            failureMatcher.find() ? failureMatcher.group(1) : "Unknown gifting failure";
        KoLmafia.updateDisplay(MafiaState.ERROR, message);
      }
    } else if (action.equals("tradecandy")) {
      // You don't have that much candy!
      if (responseText.indexOf("You don't have that much candy") == -1) {
        CoinMasterRequest.completeSale(data, location);
        NamedListenerRegistry.fireChange("(coinmaster)");
      }
    } else {
      // Some other action not associated with the cashier
      return;
    }

    // Parse current Candy Credits
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static final Pattern TOWHO_PATTERN = Pattern.compile("towho=([^&]*)");

  private static boolean registerDonation(final String urlString) {
    CoinmasterData data = Crimbo11Request.CRIMBO11;

    Matcher itemMatcher = data.getItemMatcher(urlString);
    if (!itemMatcher.find()) {
      return true;
    }
    String itemIdString = itemMatcher.group(1);
    int itemId = StringUtilities.parseInt(itemIdString);

    Matcher countMatcher = data.getCountMatcher(urlString);
    int count = countMatcher.find() ? StringUtilities.parseInt(countMatcher.group(1)) : 1;

    List<AdventureResult> items = data.getBuyItems();
    AdventureResult item = AdventureResult.findItem(itemId, items);
    String name = item != null ? item.getName() : ("item #" + itemIdString);
    Map<Integer, Integer> prices = data.getBuyPrices();
    int price = CoinmastersDatabase.getPrice(itemId, prices);
    int cost = count * price;

    String tokenName = (cost != 1) ? data.getPluralToken() : data.getToken();
    String itemName = (count != 1) ? ItemDatabase.getPluralName(itemId) : name;

    Matcher victimMatcher = Crimbo11Request.TOWHO_PATTERN.matcher(urlString);
    String victim =
        victimMatcher.find() ? GenericRequest.decodeField(victimMatcher.group(1).trim()) : "0";
    if (victim.equals("") || victim.equals("0")) {
      victim = "the Needy";
    }

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(
        "trading " + cost + " " + tokenName + " for " + count + " " + itemName + " for " + victim);
    return true;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("crimbo11.php")) {
      return false;
    }

    String place = Crimbo11Request.placeString(urlString);
    String action = GenericRequest.getAction(urlString);
    if (place != null && action == null) {
      String message = "Visiting " + place;
      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog(message);
      return true;
    } else if (action == null) {
      return true;
    } else if (action.equals("buygifts")) {
      // Transitional form leading to reallybuygifts
      return true;
    }

    CoinmasterData data = Crimbo11Request.CRIMBO11;
    if (action.equals(data.getBuyAction())) {
      return Crimbo11Request.registerDonation(urlString);
    }

    return CoinMasterRequest.registerRequest(data, urlString);
  }

  public static String accessible() {
    return "Candy Credits are no longer exchangeable";
  }
}
