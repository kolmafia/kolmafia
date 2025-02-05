package net.sourceforge.kolmafia.shop;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;

public class ShopDatabase {

  public static final Map<String, String> shopIdToShopName = new TreeMap<>();
  public static final Map<String, String> shopNameToShopId = new HashMap<>();

  public static String getShopName(String shopId) {
    return shopIdToShopName.get(shopId);
  }

  public static String getShopId(String shopName) {
    return shopNameToShopId.get(shopName);
  }

  public enum SHOP {
    NONE, // Unsupported
    CONC, // Supported as a mixing method
    NPC, // Supported as an NPC store
    COIN, // Supported as a coinmaster
    NPCCOIN // Supported as both an NPC Store and a coinmaster
  }

  public static final Map<String, SHOP> shopIdToShopType = new TreeMap<>();

  public static SHOP getShopType(String shopId) {
    return shopIdToShopType.getOrDefault(shopId, SHOP.NONE);
  }

  public static SHOP parseShopType(String type) {
    try {
      SHOP shopType = Enum.valueOf(SHOP.class, type.toUpperCase());
      return shopType;
    } catch (IllegalArgumentException e) {
      return SHOP.NONE;
    }
  }

  // Concoctions

  public static final Map<String, CraftingType> shopIdToCraftingType = new TreeMap<>();
  public static final Map<CraftingType, String> craftingTypeToShopId = new HashMap<>();

  public static String getShopName(CraftingType craftingType) {
    String shopId = craftingTypeToShopId.get(craftingType);
    return (shopId != null) ? shopIdToShopName.get(shopId) : craftingType.toString();
  }

  // Coinmasters
  public static final Map<String, CoinmasterData> coinmasterData = new HashMap<>();

  public static void setCoinmasterData(final String shopId, final CoinmasterData data) {
    coinmasterData.put(shopId, data);
  }

  public static CoinmasterData getCoinmasterData(final String shopId) {
    return coinmasterData.get(shopId);
  }

  // Shops that want to log simple visits
  public static final Set<String> logVisitShops = new HashSet<>();

  public static void setLogVisits(final String shopId) {
    logVisitShops.add(shopId);
  }

  public static boolean logVisits(final String shopId) {
    return logVisitShops.contains(shopId);
  }

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
        String shopName = data[1];
        String shopTypeName = data[2];
        SHOP shopType = parseShopType(shopTypeName);

        if (shopType == SHOP.CONC) {
          if (data.length > 3) {
            String craftingTypeName = data[3];
            try {
              CraftingType craftingType = Enum.valueOf(CraftingType.class, craftingTypeName);
              registerShop(shopId, shopName, craftingType);
            } catch (IllegalArgumentException e) {
              RequestLogger.printLine(
                  "shopId "
                      + shopId
                      + " is a CONC, but "
                      + craftingTypeName
                      + " is an unknown CraftingType.");
            }
          } else {
            RequestLogger.printLine(
                "shopId " + shopId + " is a CONC, but CraftingType is not specified.");
          }
        } else {
          registerShop(shopId, shopName, shopType);
        }
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  public static void registerShop(
      final String shopId, final String shopName, final CraftingType type) {
    registerShop(shopId, shopName, SHOP.CONC);
    shopIdToCraftingType.put(shopId, type);
    craftingTypeToShopId.put(type, shopId);
  }

  public static boolean registerShop(final String shopId, final String shopName, SHOP shopType) {
    // Return true if this shop is not previously known
    if (shopIdToShopName.containsKey(shopId)) {
      // The shopId is already registered. The shopName can differ;
      // KoL has duplicates, and KoLmafia will internally rename it.
      //
      // Visiting a shop will register it as NONE. That's OK.
      // Some shops are implemented as both NPC and COIN.
      // Other combos are no good.
      SHOP existingShopType = shopIdToShopType.get(shopId);
      boolean changed = false;
      if (shopType != SHOP.NONE && shopType != existingShopType) {
        // CONC shops cannot have multiple types
        boolean ok = existingShopType != SHOP.CONC;
        switch (shopType) {
          case NPC -> {
            if (existingShopType == SHOP.COIN) {
              // COIN -> NPCCOIN
              shopType = SHOP.NPCCOIN;
              changed = true;
            }
          }
          case COIN -> {
            // NPC -> NPCCOIN
            if (existingShopType == SHOP.NPC) {
              shopType = SHOP.NPCCOIN;
              changed = true;
            }
          }
        }
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
      if (!changed) {
        return false;
      }
    }

    shopIdToShopName.put(shopId, shopName);
    // KoL has duplicate names for shops, but we should not.
    String existingShopId = shopNameToShopId.get(shopName);
    if (existingShopId == null) {
      shopNameToShopId.put(shopName, shopId);
    } else if (shopType != SHOP.NPCCOIN) {
      String printMe =
          "Shop name '"
              + shopName
              + "' for shop id '"
              + shopId
              + "' already used for '"
              + existingShopId
              + "'";
      RequestLogger.printLine(printMe);
      RequestLogger.updateSessionLog(printMe);
    }

    shopIdToShopType.put(shopId, shopType);

    return true;
  }

  public static void writeShopFile() {
    File output = new File(KoLConstants.DATA_LOCATION, "shops.txt");
    PrintStream writer = LogStream.openStream(output, true);
    try (writer) {
      writer.println(KoLConstants.SHOPS_VERSION);

      for (Entry<String, String> entry : shopIdToShopName.entrySet()) {
        String shopId = entry.getKey();
        String shopName = entry.getValue();
        SHOP shopType = shopIdToShopType.get(shopId);
        String line = shopId + "\t" + shopName + "\t" + shopType;
        CraftingType craftingType = shopIdToCraftingType.get(shopId);
        if (craftingType != null) {
          line += "\t" + craftingType.name();
        }
        writer.println(line);
      }
    }
  }
}
