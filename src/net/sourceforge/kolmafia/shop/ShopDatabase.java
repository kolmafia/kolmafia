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
  public static final Set<String> coinShops = new TreeSet<>();
  public static final Set<String> concShops = new TreeSet<>();
  public static final Set<String> npcShops = new TreeSet<>();

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

        shopIdToShopName.put(shopId, shopName);
        shopNameToShopId.put(shopName, shopId);
        switch (shopType) {
          case "coin" -> coinShops.add(shopId);
          case "conc" -> concShops.add(shopId);
          case "npc" -> npcShops.add(shopId);
        }
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  public static void registerCoinShop(String shopId, String shopName) {
    if (!coinShops.contains(shopId)) {
      shopIdToShopName.put(shopId, shopName);
      shopNameToShopId.put(shopName, shopId);
      coinShops.add(shopId);
    }
  }

  public static void registerConcShop(String shopId, String shopName) {
    if (!concShops.contains(shopId)) {
      shopIdToShopName.put(shopId, shopName);
      shopNameToShopId.put(shopName, shopId);
      concShops.add(shopId);
    }
  }

  public static void registerNPCShop(String shopId, String shopName) {
    if (!npcShops.contains(shopId)) {
      shopIdToShopName.put(shopId, shopName);
      shopNameToShopId.put(shopName, shopId);
      npcShops.add(shopId);
    }
  }

  public static void writeShopFile() {
    File output = new File(KoLConstants.DATA_LOCATION, "shops.txt");
    PrintStream writer = LogStream.openStream(output, true);
    try {
      writer.println(KoLConstants.SHOPS_VERSION);

      for (Entry<String, String> entry : shopIdToShopName.entrySet()) {
        String shopId = entry.getKey();
        String shopName = entry.getValue();
        String shopType =
            coinShops.contains(shopId)
                ? "coin"
                : concShops.contains(shopId)
                    ? "conc"
                    : npcShops.contains(shopId) ? "npc" : "unknown";
        writer.println(shopId + "\t" + shopType + "\t" + shopName);
      }
    } finally {
      writer.close();
    }
  }
}
