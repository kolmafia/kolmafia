package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CrimboCafeRequest extends CafeRequest {
  public static final boolean AVAILABLE = false;
  public static final String CAFEID = "10";

  private record Item(String name, int itemId, int price) {}

  public static final Item[] MENU_DATA = {
    // Item, itemID, price
    new Item("Peppermint Nutrition Block", -104, 50),
    new Item("Gingerbread Nutrition Block", -105, 75),
    new Item("Cinnamon Nutrition Block", -106, 100),
    new Item("Fortified Eggnog Slurry", -107, 50),
    new Item("Hot Buttered Rum", -108, 75),
    new Item("Spicy Hot Chocolate", -109, 100),
  };

  private static Item dataByName(final String name) {
    for (int i = 0; i < CrimboCafeRequest.MENU_DATA.length; ++i) {
      Item data = CrimboCafeRequest.MENU_DATA[i];
      if (name.equalsIgnoreCase(data.name)) {
        return data;
      }
    }
    return null;
  }

  private static Item dataByItemID(final int itemId) {
    for (int i = 0; i < CrimboCafeRequest.MENU_DATA.length; ++i) {
      Item data = CrimboCafeRequest.MENU_DATA[i];
      if (itemId == data.itemId) {
        return data;
      }
    }
    return null;
  }

  public CrimboCafeRequest(final String name) {
    super("Crimbo Cafe", CrimboCafeRequest.CAFEID);

    int itemId = 0;
    int price = 0;

    Item data = dataByName(name);
    if (data != null) {
      itemId = data.itemId;
      price = data.price;
    }

    this.setItem(name, itemId, price);
  }

  public static final boolean onMenu(final String name) {
    return KoLConstants.cafeItems.contains(name);
  }

  public static final void getMenu() {
    if (!CrimboCafeRequest.AVAILABLE) {
      return;
    }
    KoLmafia.updateDisplay("Visiting Crimbo Cafe...");
    KoLConstants.cafeItems.clear();
    for (int i = 0; i < CrimboCafeRequest.MENU_DATA.length; ++i) {
      Item data = CrimboCafeRequest.MENU_DATA[i];
      String name = data.name;
      int price = data.price;
      CafeRequest.addMenuItem(KoLConstants.cafeItems, name, price);
    }
    ConcoctionDatabase.getUsables().sort();
    KoLmafia.updateDisplay("Menu retrieved.");
  }

  public static final void reset() {
    CafeRequest.reset(KoLConstants.cafeItems);
  }

  public static final boolean registerRequest(final String urlString) {
    Matcher matcher = CafeRequest.CAFE_PATTERN.matcher(urlString);
    if (!matcher.find() || !matcher.group(1).equals(CrimboCafeRequest.CAFEID)) {
      return false;
    }

    matcher = CafeRequest.ITEM_PATTERN.matcher(urlString);
    if (!matcher.find()) {
      return true;
    }

    int itemId = StringUtilities.parseInt(matcher.group(1));

    Item data = CrimboCafeRequest.dataByItemID(itemId);
    if (data == null) {
      return false;
    }

    String itemName = data.name;
    int price = data.price;

    CafeRequest.registerItemUsage(itemName, price);
    return true;
  }
}
