package net.sourceforge.kolmafia.shop;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;

public class ShopDatabase {

  public static final Map<String, String> shopIdToShopName = new TreeMap<>();
  public static final Map<String, String> shopNameToShopId = new HashMap<>();

  // *** Do we need this?
  public enum SHOP {
    NONE, // Unsupported
    CONC, // Supported as a mixing method
    COIN, // Supported as a cooinmaster
    NPC // Supported as an NPC store
  }

  public static final Set<String> coinShops = new TreeSet<>();
  public static final Set<String> concShops = new TreeSet<>();
  public static final Set<String> npcShops = new TreeSet<>();
  public static final Set<String> unknownShops = new TreeSet<>();

  private static Map<String, String> shopIdToType = new HashMap<>();

  private static SHOP shopIdToType(String shopId) {
    return coinShops.contains(shopId)
        ? SHOP.COIN
        : concShops.contains(shopId) ? SHOP.CONC : npcShops.contains(shopId) ? SHOP.NPC : SHOP.NONE;
  }

  private static Set<String> shopTypeToSet(SHOP shopType) {
    return switch (shopType) {
      case NONE -> unknownShops;
      case COIN -> coinShops;
      case CONC -> concShops;
      case NPC -> npcShops;
    };
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
      return false;
    }

    shopIdToShopName.put(shopId, shopName);
    shopNameToShopId.put(shopName, shopId);

    // *** Do we need this?
    Set<String> typeSet = shopTypeToSet(shopType);
    typeSet.add(shopId);

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
        SHOP shopType = shopIdToType(shopId);
        writer.println(toData(shopId, shopName, shopType));
      }
    } finally {
      writer.close();
    }
  }
}
