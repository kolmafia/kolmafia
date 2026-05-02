package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BountyDatabase {
  public record BountyData(
      String plural, String type, String image, int number, String monster, String location) {
    public String getKoLInternalType() {
      return type.equals("easy")
          ? "low"
          : type.equals("hard") ? "high" : type.equals("special") ? "special" : null;
    }

    String toDataLine(final String name) {
      String outputLocation = location != null ? location : "unknown";
      return name
          + "\t"
          + plural
          + "\t"
          + type
          + "\t"
          + image
          + "\t"
          + number
          + "\t"
          + monster
          + "\t"
          + outputLocation;
    }
  }

  private static final Map<String, BountyData> bountyByName = new LinkedHashMap<>();
  private static final Map<String, String> bountyByPlural = new HashMap<>();
  private static final Map<String, String> nameByMonster = new HashMap<>();

  public static String[] canonicalNames;
  private static final Map<String, String> canonicalToName = new HashMap<>();

  static {
    BountyDatabase.reset();
  }

  private BountyDatabase() {}

  public static void reset() {
    BountyDatabase.bountyByName.clear();
    BountyDatabase.bountyByPlural.clear();
    BountyDatabase.nameByMonster.clear();

    BountyDatabase.readData();
    BountyDatabase.buildCanonicalNames();
  }

  private static void readData() {
    try (BufferedReader reader =
        FileUtilities.getVersionedReader("bounty.txt", KoLConstants.BOUNTY_VERSION)) {

      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length < 7) {
          continue;
        }

        String name = data[0];
        BountyDatabase.bountyByName.put(
            name,
            new BountyData(
                data[1], data[2], data[3], StringUtilities.parseInt(data[4]), data[5], data[6]));
        BountyDatabase.bountyByPlural.put(data[1], name);
        BountyDatabase.nameByMonster.put(data[5], name);
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  public static final BountyData getBountyData(final String name) {
    return BountyDatabase.bountyByName.get(name);
  }

  private static void buildCanonicalNames() {
    BountyDatabase.canonicalNames = new String[BountyDatabase.bountyByName.size()];
    int i = 0;
    for (String name : BountyDatabase.bountyByName.keySet()) {
      String canonical = StringUtilities.getCanonicalName(name);
      BountyDatabase.canonicalNames[i] = canonical;
      BountyDatabase.canonicalToName.put(canonical, name);
      i++;
    }
    Arrays.sort(BountyDatabase.canonicalNames);
  }

  public static final List<String> getMatchingNames(final String substring) {
    return StringUtilities.getMatchingNames(BountyDatabase.canonicalNames, substring);
  }

  public static final String canonicalToName(final String canonical) {
    String name = BountyDatabase.canonicalToName.get(canonical);
    return name == null ? "" : name;
  }

  public static final void setValue(
      String name,
      String plural,
      String type,
      String image,
      int number,
      String monster,
      String location) {
    BountyData existing = BountyDatabase.bountyByName.get(name);
    String newLocation =
        location != null ? location : existing != null ? existing.location() : null;
    BountyDatabase.bountyByName.put(
        name, new BountyData(plural, type, image, number, monster, newLocation));
    BountyDatabase.bountyByPlural.put(plural, name);
    BountyDatabase.nameByMonster.put(monster, name);
    BountyDatabase.buildCanonicalNames();

    String printMe = "Unknown bounty:";
    RequestLogger.printLine(printMe);
    RequestLogger.updateSessionLog(printMe);
    printMe = "--------------------";
    RequestLogger.printLine(printMe);
    RequestLogger.updateSessionLog(printMe);
    printMe = BountyDatabase.bountyByName.get(name).toDataLine(name);
    RequestLogger.printLine(printMe);
    RequestLogger.updateSessionLog(printMe);
    printMe = "--------------------";
    RequestLogger.printLine(printMe);
    RequestLogger.updateSessionLog(printMe);
  }

  public static final String[] entrySet() {
    return BountyDatabase.bountyByName.keySet().toArray(new String[0]);
  }

  public static final String getName(String plural) {
    if (plural == null || plural.equals("")) {
      return null;
    }

    return BountyDatabase.bountyByPlural.get(plural);
  }

  public static final String getNameByMonster(String monster) {
    if (monster == null || monster.equals("")) {
      return null;
    }

    return BountyDatabase.nameByMonster.get(monster);
  }

  public static final String getPlural(String name) {
    if (name == null || name.equals("")) {
      return null;
    }

    BountyData data = BountyDatabase.bountyByName.get(name);
    return data == null ? null : data.plural();
  }

  public static final String getType(String name) {
    if (name == null || name.equals("")) {
      return null;
    }

    BountyData data = BountyDatabase.bountyByName.get(name);
    return data == null ? null : data.type();
  }

  public static final String getImage(String name) {
    if (name == null || name.equals("")) {
      return null;
    }

    BountyData data = BountyDatabase.bountyByName.get(name);
    return data == null ? null : data.image();
  }

  public static final int getNumber(String name) {
    if (name == null || name.equals("")) {
      return 0;
    }

    BountyData data = BountyDatabase.bountyByName.get(name);
    return data == null ? 0 : data.number();
  }

  public static final String getMonster(String name) {
    if (name == null || name.equals("")) {
      return null;
    }

    BountyData data = BountyDatabase.bountyByName.get(name);
    return data == null ? null : data.monster();
  }

  public static final String getLocation(String name) {
    if (name == null || name.equals("")) {
      return null;
    }

    BountyData data = BountyDatabase.bountyByName.get(name);
    return data == null ? null : data.location();
  }

  public static final boolean checkBounty(String pref) {
    String currentBounty = Preferences.getString(pref);
    int bountySeparator = currentBounty.indexOf(":");
    if (bountySeparator != -1) {
      String bountyName = currentBounty.substring(0, bountySeparator);
      if ("null".equals(bountyName)) {
        Preferences.setString(pref, "");
        return false;
      }
      if (bountyName != null && !bountyName.equals("")) {
        int currentBountyCount =
            StringUtilities.parseInt(currentBounty.substring(bountySeparator + 1));
        if (currentBountyCount == BountyDatabase.getNumber(bountyName)) {
          return true;
        }
      }
    }
    return false;
  }
}
