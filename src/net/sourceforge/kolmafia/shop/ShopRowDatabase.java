package net.sourceforge.kolmafia.shop;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AdventureResult.MeatResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.shop.ShopDatabase.SHOP;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ShopRowDatabase {

  enum Mode {
    NONE, // Do nothing
    READ, // Read data file into data structure
    BUILD // Populate data structure from other data
  }

  // If we eventually need this internally, set to Mode.READ
  // If you want to regenerate the data file, set to Mode.BUILD
  // recompile, log in, and "test write-shoprows"
  public static Mode mode = Mode.READ;

  public record ShopRowData(
      int row, String shopId, SHOP shopType, AdventureResult item, AdventureResult[] costs) {
    public String dataString() {
      StringBuilder buf = new StringBuilder();
      buf.append(String.valueOf(row));
      buf.append("\t");
      buf.append(shopId);
      buf.append("\t");
      buf.append(shopType);
      buf.append("\t");
      buf.append(item.toString());
      for (AdventureResult cost : costs) {
        buf.append("\t");
        buf.append(cost.toString());
      }
      return buf.toString();
    }
  }

  public static final Map<Integer, ShopRowData> shopRowData = new HashMap<>();

  public static ShopRowData getShopRowData(final int row) {
    return shopRowData.get(row);
  }

  public static ShopRow getShopRow(final int row) {
    return getShopRow(getShopRowData(row));
  }

  public static ShopRow getShopRow(ShopRowData data) {
    return data != null ? new ShopRow(data.row(), data.item(), data.costs()) : null;
  }

  private ShopRowDatabase() {}

  public static void registerShopRow(ShopRow shopRow, CraftingType craftingType) {
    String shopName = ShopDatabase.getShopName(craftingType);
    String shopId = ShopDatabase.getShopId(shopName);
    registerShopRow(shopRow, shopId, "conc");
  }

  public static void registerShopRow(ShopRow shopRow, String shopId, String type) {
    if (mode == Mode.BUILD) {
      int row = shopRow.getRow();
      if (shopRowData.containsKey(row)) {
        // *** log it
        return;
      }
      SHOP shopType = ShopDatabase.getShopType(shopId);
      AdventureResult item = shopRow.getItem();
      AdventureResult[] costs = shopRow.getCosts();

      ShopRowData data = new ShopRowData(row, shopId, shopType, item, costs);
      shopRowData.put(row, data);
    }
  }

  private static final Pattern MEAT_PATTERN = Pattern.compile("([\\d,]+) Meat");

  public static AdventureResult parseItemOrMeat(final String s) {
    Matcher meatMatcher = MEAT_PATTERN.matcher(s);
    if (meatMatcher.find()) {
      return new MeatResult(StringUtilities.parseInt(meatMatcher.group(1)));
    }
    return AdventureResult.parseItem(s, true);
  }

  public static ShopRowData parseShopRowData(String[] data) {
    // row	shopId	shopType	item	costs...

    if (data.length < 5) {
      return null;
    }

    int row = StringUtilities.parseInt(data[0]);
    String shopId = data[1];
    SHOP shopType = ShopDatabase.parseShopType(data[2]);
    AdventureResult item = parseItemOrMeat(data[3]);
    ArrayList<AdventureResult> costList = new ArrayList<>();
    for (int index = 4; index < data.length; index++) {
      AdventureResult cost = parseItemOrMeat(data[index]);
      costList.add(cost);
    }
    AdventureResult[] costs = costList.toArray(new AdventureResult[0]);
    return new ShopRowData(row, shopId, shopType, item, costs);
  }

  static {
    switch (mode) {
      case READ -> {
        // Read existing data file
        readShopRowDataFile();
      }
      case BUILD -> {
        // Create empty data file
        writeShopRowDataFile();
      }
      case NONE -> {}
    }
  }

  public static void readShopRowDataFile() {
    try (BufferedReader reader =
        FileUtilities.getVersionedReader("shoprows.txt", KoLConstants.SHOPROWS_VERSION)) {
      shopRowData.clear();

      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        ShopRowData rowData = parseShopRowData(data);
        if (rowData == null) {
          continue;
        }

        int row = rowData.row();
        if (shopRowData.containsKey(row)) {
          RequestLogger.printLine("Duplicate shop row:");
          RequestLogger.printLine(shopRowData.get(row).dataString());
          RequestLogger.printLine(rowData.dataString());
          continue;
        }

        shopRowData.put(row, rowData);
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  public static void writeShopRowDataFile() {
    File output = new File(KoLConstants.DATA_LOCATION, "shoprows.txt");
    RequestLogger.printLine("Writing data override: " + output);
    PrintStream writer = LogStream.openStream(output, true);
    try {
      writer.println(KoLConstants.SHOPROWS_VERSION);

      Iterator<Entry<Integer, ShopRowData>> it = shopRowData.entrySet().iterator();
      int lastRow = 1;

      while (it.hasNext()) {
        Entry<Integer, ShopRowData> entry = it.next();
        ShopRowData value = entry.getValue();

        int row = value.row();
        while (lastRow < row) {
          writer.println(lastRow++);
        }
        lastRow = row + 1;

        writer.println(value.dataString());
      }
    } finally {
      writer.close();
    }
  }
}
