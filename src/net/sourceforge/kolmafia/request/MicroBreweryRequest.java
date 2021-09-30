package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MicroBreweryRequest extends CafeRequest {
  private static final Pattern SPECIAL_PATTERN =
      Pattern.compile(
          "Today's Special:.*?name=whichitem value=(\\d+).*?onclick='descitem\\(\"(\\d+)\".*?<td>(.*?) \\(([\\d,]+) Meat\\)</td>",
          Pattern.DOTALL);

  public static final AdventureResult getDailySpecial() {
    if (KoLConstants.microbreweryItems.isEmpty()) {
      MicroBreweryRequest.getMenu();
    }

    String itemName = Preferences.getString("_dailySpecial");
    return AdventureResult.tallyItem(itemName);
  }

  public MicroBreweryRequest() {
    super("The Gnomish Micromicrobrewery", "2");
  }

  public MicroBreweryRequest(final String name) {
    super("The Gnomish Micromicrobrewery", "2");
    this.isPurchase = true;

    int itemId = 0;
    int price = 0;

    switch (KoLConstants.microbreweryItems.indexOf(name)) {
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
    if (!KoLCharacter.gnomadsAvailable()) {
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
    MicroBreweryRequest.parseResponse(this.getURLString(), this.responseText);
  }

  @Override
  protected void parseResponse() {
    MicroBreweryRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    // cafe.php?cafeid=2&pwd&action=CONSUME%21&whichitem=806
    if (!urlString.startsWith("cafe.php") || !urlString.contains("cafeid=2")) {
      return;
    }

    // If we were not attempting to consume an item, look for daily special
    if (!urlString.contains("action=CONSUME")) {
      Matcher matcher = MicroBreweryRequest.SPECIAL_PATTERN.matcher(responseText);
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

    // We attempted to drink something. It might have failed - and
    // if it was attempted via a CafeRequest, this will have been
    // detected there - but if it succeeded, via internal or
    // external request, update things here.
    if (responseText.contains("You pour your drink into your mime army shotglass")) {
      Preferences.setBoolean("_mimeArmyShotglassUsed", true);
    }
  }

  public static final boolean onMenu(final String name) {
    return KoLConstants.microbreweryItems.contains(name);
  }

  public static final void getMenu() {
    if (!KoLCharacter.gnomadsAvailable()
        || KoLCharacter.inZombiecore()
        || KoLCharacter.isJarlsberg()) {
      return;
    }

    KoLConstants.microbreweryItems.clear();

    CafeRequest.addMenuItem(KoLConstants.microbreweryItems, "Petite Porter", 50);
    CafeRequest.addMenuItem(KoLConstants.microbreweryItems, "Scrawny Stout", 75);
    CafeRequest.addMenuItem(KoLConstants.microbreweryItems, "Infinitesimal IPA", 100);

    RequestThread.postRequest(new MicroBreweryRequest());

    String itemName = Preferences.getString("_dailySpecial");
    if (itemName != "") {
      int itemId = ItemDatabase.getItemId(itemName);

      int price = Preferences.getInteger("_dailySpecialPrice");

      if (price == 0) {
        price = Math.max(1, Math.abs(ItemDatabase.getPriceById(itemId))) * 3;
      }

      CafeRequest.addMenuItem(KoLConstants.microbreweryItems, itemName, price);
    }

    ConcoctionDatabase.getUsables().sort();
    KoLmafia.updateDisplay("Menu retrieved.");
  }

  public static final void reset() {
    CafeRequest.reset(KoLConstants.microbreweryItems);
  }

  public static final boolean registerRequest(final String urlString) {
    Matcher matcher = CafeRequest.CAFE_PATTERN.matcher(urlString);
    if (!matcher.find() || !matcher.group(1).equals("2")) {
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
        itemName = "Petite Porter";
        price = 50;
        break;
      case -2:
        itemName = "Scrawny Stout";
        price = 75;
        break;
      case -3:
        itemName = "Infinitesimal IPA";
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
