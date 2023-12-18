package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.request.ArmoryAndLeggeryRequest.CoinmasterItem;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class StandardRewardDatabase {

  // id	year	type	class	row	name
  public static record StandardEquipment(
      int itemId, int year, boolean type, AscensionClass cl, String row, String itemName) {}

  // id	year	type	name
  public static record StandardPulverized(int itemId, int year, boolean type, String itemName) {}

  public static Map<Integer, StandardEquipment> rewardByItemid = new HashMap<>();

  public static Map<Integer, StandardPulverized> pulverizedByItemid = new HashMap<>();

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

        rewardByItemid.put(itemId, new StandardEquipment(itemId, year, type, cl, row, name));
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }

    // System.out.println("There are " + rewardByItemid.size() + " kinds of standard reward
    // equipment");
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

        pulverizedByItemid.put(itemId, new StandardPulverized(itemId, year, type, name));
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }

    // System.out.println("There are " + pulverizedByItemid.size() + " kinds of pulverized standard
    // reward equipment");
  }

  public static String coinmasterString(CoinmasterItem reward) {
    if (reward == null) {
      return null;
    }

    int currency = ItemDatabase.getItemId(reward.currency());
    if (currency == -1) {
      RequestLogger.printLine("currency '" + reward.currency() + "' is unknown.");
      return null;
    }

    StandardPulverized pulverized = pulverizedByItemid.get(currency);

    int itemId = reward.itemId();
    String itemName = reward.itemName();
    int year = pulverized.year() - 1;
    boolean type = pulverized.type();
    StandardEquipment current = rewardByItemid.get(itemId);
    AscensionClass cl = current == null ? null : current.cl();
    String row = "ROW" + reward.row();

    StringBuilder buf = new StringBuilder();
    buf.append(itemId);
    buf.append("\t");
    buf.append(year);
    buf.append("\t");
    buf.append(type ? "hard" : "norm");
    buf.append("\t");
    if (cl == null) {
      buf.append("NONE");
    } else {
      buf.append(
          switch (cl) {
            case SEAL_CLUBBER -> "SC";
            case TURTLE_TAMER -> "TT";
            case PASTAMANCER -> "PA";
            case SAUCEROR -> "SA";
            case DISCO_BANDIT -> "DB";
            case ACCORDION_THIEF -> "AT";
            default -> "";
          });
    }
    buf.append("\t");
    buf.append(row);
    buf.append("\t");
    buf.append(itemName);

    return buf.toString();
  }
}
