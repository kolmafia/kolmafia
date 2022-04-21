package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CrimboCafeRequest extends CafeRequest {
  public static final boolean AVAILABLE = false;
  public static final String CAFEID = "10";
  public static final Object[][] MENU_DATA = {
    // Item, itemID, price
    {
      "Peppermint Nutrition Block", -104, 50,
    },
    {
      "Gingerbread Nutrition Block", -105, 75,
    },
    {
      "Cinnamon Nutrition Block", -106, 100,
    },
    {
      "Fortified Eggnog Slurry", -107, 50,
    },
    {
      "Hot Buttered Rum", -108, 75,
    },
    {
      "Spicy Hot Chocolate", -109, 100,
    },
  };

  private static Object[] dataByName(final String name) {
    for (int i = 0; i < CrimboCafeRequest.MENU_DATA.length; ++i) {
      Object[] data = CrimboCafeRequest.MENU_DATA[i];
      if (name.equalsIgnoreCase((String) data[0])) {
        return data;
      }
    }
    return null;
  }

  private static Object[] dataByItemID(final int itemId) {
    for (int i = 0; i < CrimboCafeRequest.MENU_DATA.length; ++i) {
      Object[] data = CrimboCafeRequest.MENU_DATA[i];
      if (itemId == ((Integer) data[1]).intValue()) {
        return data;
      }
    }
    return null;
  }

  public static final String dataName(Object[] data) {
    return (String) data[0];
  }

  public static final int dataItemID(Object[] data) {
    return ((Integer) data[1]).intValue();
  }

  public static final int dataPrice(Object[] data) {
    return ((Integer) data[2]).intValue();
  }

  public CrimboCafeRequest(final String name) {
    super("Crimbo Cafe", CrimboCafeRequest.CAFEID);

    int itemId = 0;
    int price = 0;

    Object[] data = dataByName(name);
    if (data != null) {
      itemId = dataItemID(data);
      price = dataPrice(data);
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
      Object[] data = CrimboCafeRequest.MENU_DATA[i];
      String name = CrimboCafeRequest.dataName(data);
      int price = CrimboCafeRequest.dataPrice(data);
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

    Object[] data = CrimboCafeRequest.dataByItemID(itemId);
    if (data == null) {
      return false;
    }

    String itemName = CrimboCafeRequest.dataName(data);
    int price = CrimboCafeRequest.dataPrice(data);

    CafeRequest.registerItemUsage(itemName, price);
    return true;
  }
}
