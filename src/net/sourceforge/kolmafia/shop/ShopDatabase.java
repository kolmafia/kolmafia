package net.sourceforge.kolmafia.shop;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;

public class ShopDatabase {

  public static final Map<String, String> shopIdToShopName = new TreeMap<>();
  public static final Map<String, String> shopNameToShopId = new HashMap<>();

  public enum SHOP {
    NONE, // Unsupported
    CONC, // Supported as a mixing method
    COIN, // Supported as a cooinmaster
    NPC // Supported as an NPC store
  }

  public static final Map<String, SHOP> shopIdToShopType = new TreeMap<>();

  private ShopDatabase() {}

  static {
    try (BufferedReader reader =
        FileUtilities.getVersionedReader("shops.txt", KoLConstants.SHOPS_VERSION)) {
      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length < 3) {
          continue;
        }

        String shopId = data[0];
        String shopType = data[1];
        String shopName = data[2];

        registerShop(shopId, shopName, Enum.valueOf(SHOP.class, shopType));
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  public static boolean registerShop(
      final String shopId, final String shopName, final SHOP shopType) {
    // Return true if this shop is not previously known
    if (shopIdToShopName.containsKey(shopId)) {
      // The shopId is already registered. The shopName can differ;
      // KoL has duplicates, and KoLmafia will internally rename it.
      //
      // Visiting a shop will register it as NONE. That's OK.
      // Some shops are implemented as both NPC and COIN.
      // Other combos are no good.
      SHOP existingShopType = shopIdToShopType.get(shopId);
      if (shopType != existingShopType) {
        boolean ok =
            switch (shopType) {
              case NONE -> true;
              case NPC -> existingShopType == SHOP.COIN;
              case COIN -> existingShopType == SHOP.NPC;
              case CONC -> false;
            };
        if (!ok) {
          String printMe =
              "Shop id '"
                  + shopId
                  + "' of type "
                  + shopType
                  + " already registered as "
                  + existingShopType;
          RequestLogger.printLine(printMe);
          RequestLogger.updateSessionLog(printMe);
        }
      }
      return false;
    }

    shopIdToShopName.put(shopId, shopName);
    // KoL has duplicate names for shops, but we should not.
    String existingShopId = shopNameToShopId.get(shopName);
    if (existingShopId == null) {
      shopNameToShopId.put(shopName, shopId);
    } else {
      String printMe =
          "Shop name '"
              + shopName
              + "' for shop id ' "
              + shopId
              + " already used for "
              + existingShopId;
      RequestLogger.printLine(printMe);
      RequestLogger.updateSessionLog(printMe);
    }

    shopIdToShopType.put(shopId, shopType);

    return true;
  }

  public static String toData(final String shopId, final String shopName, SHOP shopType) {
    StringBuilder buf = new StringBuilder();
    buf.append(shopId);
    buf.append("\t");
    buf.append(shopType);
    buf.append("\t");
    buf.append(shopName);
    return buf.toString();
  }

  public static void writeShopFile() {
    File output = new File(KoLConstants.DATA_LOCATION, "shops.txt");
    PrintStream writer = LogStream.openStream(output, true);
    try {
      writer.println(KoLConstants.SHOPS_VERSION);

      for (Entry<String, String> entry : shopIdToShopName.entrySet()) {
        String shopId = entry.getKey();
        String shopName = entry.getValue();
        SHOP shopType = shopIdToShopType.get(shopId);
        writer.println(toData(shopId, shopName, shopType));
      }
    } finally {
      writer.close();
    }
  }
}
