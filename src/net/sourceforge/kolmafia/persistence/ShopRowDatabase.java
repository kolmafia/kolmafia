package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AdventureResult.MeatResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.ShopRow;
import net.sourceforge.kolmafia.StaticEntity;
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
  public static Mode mode = Mode.NONE;

  public static final Map<Integer, ShopRow> allShopRows = new HashMap<>();
  public static final Map<Integer, String> allShopRowShops = new HashMap<>();

  private ShopRowDatabase() {}

  record ShopRowData(int row, String type, AdventureResult item, String shopName) {
    public String dataString() {
      StringBuilder buf = new StringBuilder();
      buf.append(String.valueOf(row));
      buf.append("\t");
      buf.append(type);
      buf.append("\t");
      buf.append(item.toString());
      buf.append("\t");
      buf.append(shopName);
      return buf.toString();
    }
  }

  public static Map<Integer, ShopRowData> shopRowData = new TreeMap<>();

  public static void registerShopRow(ShopRow shopRow, String type, String shopName) {
    if (mode == Mode.BUILD) {
      int row = shopRow.getRow();
      if (allShopRows.containsKey(row)) {
        // *** log it
        return;
      }
      allShopRows.put(row, shopRow);
      allShopRowShops.put(row, shopName);
      AdventureResult item = shopRow.getItem();
      if (type.equals("sell")) {
        item = shopRow.getCosts()[0];
      }
      ShopRowData data = new ShopRowData(row, type, item, shopName);
      shopRowData.put(row, data);
    }
  }

  private static final Pattern MEAT_PATTERN = Pattern.compile("([\\d,]+) Meat");

  public static AdventureResult parseItemOrMeat(final String s) {
    Matcher meatMatcher = MEAT_PATTERN.matcher(s);
    if (meatMatcher.find()) {
      return new MeatResult(StringUtilities.parseInt(meatMatcher.group(1)));
    }
    return AdventureResult.parseItem(s, false);
  }

  public static ShopRowData parseShopRowData(String[] data) {
    if (data.length < 4) {
      return null;
    }

    int row = StringUtilities.parseInt(data[0]);
    String type = data[1];
    AdventureResult item = parseItemOrMeat(data[2]);
    String shopName = data[3];

    return new ShopRowData(row, type, item, shopName);
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
        writeShopRowFile();
      }
      case NONE -> {}
    }
  }

  public static void readShopRowDataFile() {
    try (BufferedReader reader =
        FileUtilities.getVersionedReader("shoprows.txt", KoLConstants.SHOPROWS_VERSION)) {
      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        ShopRowData rowData = parseShopRowData(data);
        if (shopRowData == null) {
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

  public static void writeShopRowFile() {
    File output = new File(KoLConstants.DATA_LOCATION, "allshoprows.txt");
    PrintStream writer = LogStream.openStream(output, true);
    try {
      writer.println(KoLConstants.SHOPROWS_VERSION);

      Iterator<Entry<Integer, ShopRow>> it = allShopRows.entrySet().iterator();
      while (it.hasNext()) {
        Entry<Integer, ShopRow> entry = it.next();

        ShopRow value = entry.getValue();
        int row = value.getRow();
        String shopName = allShopRowShops.get(row);

        writer.println(value.toData(shopName));
      }
    } finally {
      writer.close();
    }
  }
}
