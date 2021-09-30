package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ChezSnooteeRequest extends CafeRequest {
  private static final Pattern SPECIAL_PATTERN =
      Pattern.compile(
          "Today's Special:.*?name=whichitem value=(\\d+).*?onclick='descitem\\(\"(\\d+)\".*?<td>(.*?) \\(([\\d,]+) Meat\\)</td>",
          Pattern.DOTALL);

  public static final AdventureResult getDailySpecial() {
    if (KoLConstants.restaurantItems.isEmpty()) {
      ChezSnooteeRequest.getMenu();
    }

    String itemName = Preferences.getString("_dailySpecial");
    return AdventureResult.tallyItem(itemName);
  }

  public ChezSnooteeRequest() {
    super("Chez Snoot&eacute;e", "1");
  }

  public ChezSnooteeRequest(final String name) {
    super("Chez Snoot&eacute;e", "1");
    this.isPurchase = true;

    int itemId = 0;
    int price = 0;

    switch (KoLConstants.restaurantItems.indexOf(name)) {
      case 0:
        itemId = -1;
        price = 50;
        break;

      case 1:
        itemId = -2;
        price = 75;
        break;

      case 2:
        itemId = -3;
        price = 100;
        break;

      case 3:
        itemId = ItemDatabase.getItemId(name);
        String dsItemName = Preferences.getString("_dailySpecial");

        if (dsItemName.equals(name)) {
          price = Preferences.getInteger("_dailySpecialPrice");
        }

        if (price == 0) {
          price = Math.max(1, Math.abs(ItemDatabase.getPriceById(itemId))) * 3;
        }
        break;
    }

    this.setItem(name, itemId, price);
  }

  @Override
  public void run() {
    if (!KoLCharacter.canadiaAvailable()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You can't find " + this.name);
      return;
    }

    super.run();
  }

  @Override
  public void processResults() {
    if (this.isPurchase) {
      super.processResults();
      return;
    }

    // If we are just visiting, parse the response to find the daily special
    ChezSnooteeRequest.parseResponse(this.getURLString(), this.responseText);
  }

  @Override
  protected void parseResponse() {
    ChezSnooteeRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    // cafe.php?cafeid=1&pwd&action=CONSUME%21&whichitem=806
    if (!urlString.startsWith("cafe.php") || !urlString.contains("cafeid=1")) {
      return;
    }

    // If we were not attempting to consume an item, look for daily special
    if (!urlString.contains("action=CONSUME")) {
      Matcher matcher = ChezSnooteeRequest.SPECIAL_PATTERN.matcher(responseText);
      if (matcher.find()) {
        int itemId = StringUtilities.parseInt(matcher.group(1));
        String descId = matcher.group(2);
        String itemName = matcher.group(3);
        int price = StringUtilities.parseInt(matcher.group(4));
        String match = ItemDatabase.getItemDataName(itemId);
        boolean checkItemName = !KoLCharacter.isCrazyRandomTwo();
        if (match == null || (checkItemName && !match.equals(itemName))) {
          ItemDatabase.registerItem(itemId, itemName, descId);
        }
        Preferences.setString("_dailySpecial", match);
        Preferences.setInteger("_dailySpecialPrice", price);
      }
      return;
    }

    // If consumption failed, nothing to do
    if (!responseText.contains("You gain")) {
      return;
    }

    AdventureResult item = ItemPool.get(GenericRequest.getWhichItem(urlString), 1);

    // Handle food helpers and adjust fullness, if necessary
    EatItemRequest.handleFoodHelper(item.getName(), 1, responseText);
  }

  public static final boolean onMenu(final String name) {
    return KoLConstants.restaurantItems.contains(name);
  }

  public static final void getMenu() {
    if (!KoLCharacter.canadiaAvailable()
        || KoLCharacter.inZombiecore()
        || KoLCharacter.isJarlsberg()) {
      return;
    }

    KoLConstants.restaurantItems.clear();

    CafeRequest.addMenuItem(KoLConstants.restaurantItems, "Peche a la Frog", 50);
    CafeRequest.addMenuItem(KoLConstants.restaurantItems, "As Jus Gezund Heit", 75);
    CafeRequest.addMenuItem(KoLConstants.restaurantItems, "Bouillabaise Coucher Avec Moi", 100);

    RequestThread.postRequest(new ChezSnooteeRequest());

    String itemName = Preferences.getString("_dailySpecial");
    if (itemName != "") {
      int itemId = ItemDatabase.getItemId(itemName);

      int price = Preferences.getInteger("_dailySpecialPrice");

      if (price == 0) {
        price = Math.max(1, Math.abs(ItemDatabase.getPriceById(itemId))) * 3;
      }

      CafeRequest.addMenuItem(KoLConstants.restaurantItems, itemName, price);
    }

    ConcoctionDatabase.getUsables().sort();
    KoLmafia.updateDisplay("Menu retrieved.");
  }

  public static final void reset() {
    CafeRequest.reset(KoLConstants.restaurantItems);
  }

  public static final boolean registerRequest(final String urlString) {
    Matcher matcher = CafeRequest.CAFE_PATTERN.matcher(urlString);
    if (!matcher.find() || !matcher.group(1).equals("1")) {
      return false;
    }

    matcher = CafeRequest.ITEM_PATTERN.matcher(urlString);
    if (!matcher.find()) {
      return true;
    }

    int itemId = StringUtilities.parseInt(matcher.group(1));
    String itemName;
    int price = 0;

    switch (itemId) {
      case -1:
        itemName = "Peche a la Frog";
        price = 50;
        break;
      case -2:
        itemName = "As Jus Gezund Heit";
        price = 75;
        break;
      case -3:
        itemName = "Bouillabaise Coucher Avec Moi";
        price = 100;
        break;
      default:
        itemName = ItemDatabase.getItemName(itemId);

        String dsItemName = Preferences.getString("_dailySpecial");

        if (dsItemName.equals(itemName)) {
          price = Preferences.getInteger("_dailySpecialPrice");
        }

        if (price == 0) {
          price = Math.max(1, Math.abs(ItemDatabase.getPriceById(itemId))) * 3;
        }

        break;
    }

    CafeRequest.registerItemUsage(itemName, price);
    return true;
  }
}
