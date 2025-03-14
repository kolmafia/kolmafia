package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class StandardRewardDatabase {

  // id	year	type	class	row	name
  public static record StandardReward(
      int itemId, int year, boolean type, AscensionClass cl, String row, String itemName) {}

  public static Map<Integer, StandardReward> rewardByItemid = new TreeMap<>();

  public static Map<Integer, StandardReward> allStandardRewards() {
    return rewardByItemid;
  }

  public static StandardReward findStandardReward(final int itemId) {
    return rewardByItemid.get(itemId);
  }

  public static void registerStandardReward(int itemId, StandardReward standardReward) {
    rewardByItemid.put(itemId, standardReward);
  }

  // id	year	type	name
  public static record StandardPulverized(int itemId, int year, boolean type, String itemName) {}

  public static Map<Integer, StandardPulverized> pulverizedByItemid = new HashMap<>();

  public static StandardPulverized findStandardPulverized(int itemId) {
    return pulverizedByItemid.get(itemId);
  }

  // Map from year -> map from type -> StandardPulverized
  public static Map<Integer, Map<Boolean, StandardPulverized>> pulverizedByYearAndType =
      new HashMap<>();

  public static void registerStandardPulverized(int itemId, StandardPulverized pulverized) {
    pulverizedByItemid.put(itemId, pulverized);
    int year = pulverized.year;
    boolean type = pulverized.type;
    Map<Boolean, StandardPulverized> map = pulverizedByYearAndType.get(year);
    if (map == null) {
      map = new HashMap<>();
      pulverizedByYearAndType.put(year, map);
    }
    map.put(type, pulverized);
  }

  static {
    StandardRewardDatabase.reset();
  }

  public static void reset() {
    StandardRewardDatabase.readEquipment();
    StandardRewardDatabase.readPulverized();
  }

  private static void readEquipment() {
    try (BufferedReader reader =
        FileUtilities.getVersionedReader(
            "standard-rewards.txt", KoLConstants.STANDARD_REWARDS_VERSION)) {
      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length != 6) {
          continue;
        }

        boolean bogus = false;

        int itemId = StringUtilities.parseInt(data[0]);
        int year = StringUtilities.parseInt(data[1]);

        boolean type;
        switch (data[2]) {
          case "norm" -> {
            type = false;
          }
          case "hard" -> {
            type = true;
          }
          default -> {
            RequestLogger.printLine(
                "Path type for itemId " + itemId + " must be 'norm' or 'hard'.");
            bogus = true;
            continue;
          }
        }

        AscensionClass cl;
        switch (data[3]) {
          case "SC" -> {
            cl = AscensionClass.SEAL_CLUBBER;
          }
          case "TT" -> {
            cl = AscensionClass.TURTLE_TAMER;
          }
          case "PA" -> {
            cl = AscensionClass.PASTAMANCER;
          }
          case "SA" -> {
            cl = AscensionClass.SAUCEROR;
          }
          case "DB" -> {
            cl = AscensionClass.DISCO_BANDIT;
          }
          case "AT" -> {
            cl = AscensionClass.ACCORDION_THIEF;
          }
          default -> {
            RequestLogger.printLine(
                "Class type for itemId " + itemId + " must be SC, TT, PA, SA, DB, or AT.");
            bogus = true;
            continue;
          }
        }

        String row = data[4];

        // Defined by itemId, so, for human use only
        String name = data[5];

        StandardReward toRegister = new StandardReward(itemId, year, type, cl, row, name);
        registerStandardReward(itemId, toRegister);
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  public static String toData(StandardReward reward) {
    int itemId = reward.itemId();
    String itemName = reward.itemName();
    int year = reward.year();
    boolean type = reward.type();
    AscensionClass cl = reward.cl();
    String row = reward.row();

    StringBuilder buf = new StringBuilder();
    buf.append(itemId);
    buf.append("\t");
    buf.append(year);
    buf.append("\t");
    buf.append(type ? "hard" : "norm");
    buf.append("\t");
    buf.append(
        switch (cl) {
          case SEAL_CLUBBER -> "SC";
          case TURTLE_TAMER -> "TT";
          case PASTAMANCER -> "PA";
          case SAUCEROR -> "SA";
          case DISCO_BANDIT -> "DB";
          case ACCORDION_THIEF -> "AT";
          default -> "NONE";
        });
    buf.append("\t");
    buf.append(row);
    buf.append("\t");
    buf.append(itemName);

    return buf.toString();
  }

  private static void readPulverized() {
    try (BufferedReader reader =
        FileUtilities.getVersionedReader(
            "standard-pulverized.txt", KoLConstants.STANDARD_PULVERIZED_VERSION)) {
      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length != 4) {
          continue;
        }

        boolean bogus = false;

        int itemId = StringUtilities.parseInt(data[0]);
        int year = StringUtilities.parseInt(data[1]);

        boolean type;
        switch (data[2]) {
          case "norm" -> {
            type = false;
          }
          case "hard" -> {
            type = true;
          }
          default -> {
            RequestLogger.printLine(
                "Pulverized type for itemId " + itemId + " must be 'norm' or 'hard'.");
            bogus = true;
            continue;
          }
        }

        // Defined by itemId, so, for human use only
        String name = data[3];

        StandardPulverized toRegister = new StandardPulverized(itemId, year, type, name);
        registerStandardPulverized(itemId, toRegister);
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  public static String toData(StandardPulverized pulverized) {
    int itemId = pulverized.itemId();
    String itemName = pulverized.itemName();
    int year = pulverized.year();
    boolean type = pulverized.type();

    StringBuilder buf = new StringBuilder();
    buf.append(itemId);
    buf.append("\t");
    buf.append(year);
    buf.append("\t");
    buf.append(type ? "hard" : "norm");
    buf.append("\t");
    buf.append(itemName);
    return buf.toString();
  }

  public static int findPulverization(StandardReward item) {
    return findPulverization(item.year, item.type);
  }

  public static int findPulverization(int year, boolean type) {
    var yearMap = pulverizedByYearAndType.get(year);
    if (yearMap == null) {
      return -1;
    }
    var pulverized = yearMap.get(type);
    if (pulverized == null) {
      return -1;
    }
    return pulverized.itemId;
  }

  // For ResultProcessor
  public static boolean isPulverizedStandardReward(int itemId) {
    return pulverizedByItemid.get(itemId) != null;
  }

  // For EquipmentDatabase
  public static void derivePulverization() {
    for (var reward : rewardByItemid.entrySet()) {
      int itemId = reward.getKey();
      StandardReward item = reward.getValue();
      int result = findPulverization(item);
      EquipmentDatabase.addPulverization(itemId, result);
    }
  }
}
