package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EncounterManager.Encounter;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BugbearManager {
  private BugbearManager() {}

  public static void resetStatus() {
    Preferences.setInteger("statusEngineering", 0);
    Preferences.setInteger("statusGalley", 0);
    Preferences.setInteger("statusMedbay", 0);
    Preferences.setInteger("statusMorgue", 0);
    Preferences.setInteger("statusNavigation", 0);
    Preferences.setInteger("statusScienceLab", 0);
    Preferences.setInteger("statusSonar", 0);
    Preferences.setInteger("statusSpecialOps", 0);
    Preferences.setInteger("statusWasteProcessing", 0);
    Preferences.setInteger("mothershipProgress", 0);
  }

  public record Bugbear(
      String shipZone, int id, String bugbear, String[] zones, int level, String status) {
    Bugbear(String shipZone, int id, String bugbear, String zone, int level, String status) {
      this(shipZone, id, bugbear, new String[] {zone}, level, status);
    }
  }

  public static final Bugbear[] BUGBEAR_DATA = {
    new Bugbear("Medbay", 1, "hypodermic bugbear", "The Spooky Forest", 1, "statusMedbay"),
    new Bugbear(
        "Waste Processing",
        2,
        "scavenger bugbear",
        "The Sleazy Back Alley",
        1,
        "statusWasteProcessing"),
    new Bugbear("Sonar", 3, "batbugbear", "Guano Junction", 1, "statusSonar"),
    new Bugbear(
        "Science Lab", 4, "bugbear scientist", "Cobb's Knob Laboratory", 2, "statusScienceLab"),
    new Bugbear(
        "Morgue",
        5,
        "bugaboo",
        new String[] {
          "The Defiled Nook", "Post-Cyrpt Cemetary",
        },
        2,
        "statusMorgue"),
    new Bugbear(
        "Special Ops", 6, "Black Ops Bugbear", "Lair of the Ninja Snowmen", 2, "statusSpecialOps"),
    new Bugbear(
        "Engineering",
        7,
        "Battlesuit Bugbear Type",
        "The Penultimate Fantasy Airship",
        3,
        "statusEngineering"),
    new Bugbear(
        "Navigation",
        8,
        "ancient unspeakable bugbear",
        "The Haunted Gallery",
        3,
        "statusNavigation"),
    new Bugbear(
        "Galley",
        9,
        "trendy bugbear chef",
        new String[] {
          "The Battlefield (Frat Uniform)", "The Battlefield (Hippy Uniform)",
        },
        3,
        "statusGalley")
  };

  public static String dataToShipZone(Bugbear data) {
    return data == null ? "" : data.shipZone;
  }

  public static int dataToId(Bugbear data) {
    return data == null ? 0 : data.id;
  }

  public static String dataToBugbear(Bugbear data) {
    return data == null ? "" : data.bugbear;
  }

  public static String dataToBugbearZone1(Bugbear data) {
    if (data == null) {
      return null;
    }

    var zones = data.zones;
    return zones.length > 0 ? zones[0] : "";
  }

  public static String dataToBugbearZone2(Bugbear data) {
    if (data == null) {
      return null;
    }

    var zones = data.zones;
    return zones.length > 1 ? zones[1] : "";
  }

  public static int dataToLevel(Bugbear data) {
    return data == null ? 0 : data.level;
  }

  public static String dataToStatusSetting(Bugbear data) {
    return data == null ? "" : data.status;
  }

  public static Bugbear idToData(final int id) {
    for (Bugbear data : BugbearManager.BUGBEAR_DATA) {
      if (BugbearManager.dataToId(data) == id) {
        return data;
      }
    }
    return null;
  }

  public static Bugbear bugbearToData(final String bugbear) {
    for (Bugbear data : BugbearManager.BUGBEAR_DATA) {
      if (bugbear.equals(BugbearManager.dataToBugbear(data))) {
        return data;
      }
    }
    return null;
  }

  public static Bugbear shipZoneToData(final String zone) {
    for (Bugbear data : BugbearManager.BUGBEAR_DATA) {
      if (zone.equals(BugbearManager.dataToShipZone(data))) {
        return data;
      }
    }
    return null;
  }

  public static void setBiodata(final Bugbear data, final String countString) {
    BugbearManager.setBiodata(data, StringUtilities.parseInt(countString));
  }

  public static void setBiodata(final Bugbear data, final int count) {
    if (data == null) {
      return;
    }

    String statusSetting = BugbearManager.dataToStatusSetting(data);
    int level = BugbearManager.dataToLevel(data);
    if (count < level * 3) {
      Preferences.setInteger(statusSetting, count);
      return;
    }

    String currentStatus = Preferences.getString(statusSetting);
    if (!StringUtilities.isNumeric(currentStatus)) {
      return;
    }

    int currentProgress = Preferences.getInteger("mothershipProgress");
    String newStatus = (level == currentProgress + 1) ? "open" : "unlocked";
    Preferences.setString(statusSetting, newStatus);
  }

  public static void clearShipZone(final String zone) {
    Bugbear data = BugbearManager.shipZoneToData(zone);
    if (data == null) {
      return;
    }

    String statusSetting = BugbearManager.dataToStatusSetting(data);
    if (Preferences.getString(statusSetting).equals("cleared")) {
      return;
    }

    // Mark this ship zone cleared
    Preferences.setString(statusSetting, "cleared");

    // Calculate which level of the ship this zone is on
    int level = BugbearManager.dataToLevel(data);

    // See if we have cleared all the zones on this level
    for (Bugbear zoneData : BugbearManager.BUGBEAR_DATA) {
      if (BugbearManager.dataToLevel(zoneData) != level) {
        continue;
      }
      String zoneSetting = BugbearManager.dataToStatusSetting(zoneData);
      String status = Preferences.getString(zoneSetting);
      if (!status.equals("cleared")) {
        return;
      }
    }

    // Yes. We have cleared this level
    Preferences.setInteger("mothershipProgress", level);
    if (level == 3) {
      return;
    }

    // All "unlocked" zones on the next level are now "open"
    int nextLevel = level + 1;
    for (Bugbear zoneData : BugbearManager.BUGBEAR_DATA) {
      if (BugbearManager.dataToLevel(zoneData) != nextLevel) {
        continue;
      }
      String zoneSetting = BugbearManager.dataToStatusSetting(zoneData);
      String status = Preferences.getString(zoneSetting);
      if (status.equals("unlocked")) {
        Preferences.setString(zoneSetting, "open");
      }
    }
  }

  public static void registerEncounter(final Encounter encounter, final String responseText) {
    // All BUGBEAR encounters indicate that a mothership zone has been cleared

    String zone = encounter.getLocation();
    String encounterName = encounter.getEncounter();

    // We could look at the responseText here to confirm, if we wanted.

    BugbearManager.clearShipZone(zone);
  }
}
