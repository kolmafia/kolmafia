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

  public static final Object[][] BUGBEAR_DATA = {
    {
      "Medbay", 1, "hypodermic bugbear", "The Spooky Forest", 1, "statusMedbay",
    },
    {
      "Waste Processing",
      2,
      "scavenger bugbear",
      "The Sleazy Back Alley",
      1,
      "statusWasteProcessing",
    },
    {
      "Sonar", 3, "batbugbear", "Guano Junction", 1, "statusSonar",
    },
    {"Science Lab", 4, "bugbear scientist", "Cobb's Knob Laboratory", 2, "statusScienceLab"},
    {
      "Morgue",
      5,
      "bugaboo",
      new String[] {
        "The Defiled Nook", "Post-Cyrpt Cemetary",
      },
      2,
      "statusMorgue",
    },
    {
      "Special Ops", 6, "black ops bugbear", "Lair of the Ninja Snowmen", 2, "statusSpecialOps",
    },
    {
      "Engineering",
      7,
      "battlesuit bugbear type",
      "The Penultimate Fantasy Airship",
      3,
      "statusEngineering",
    },
    {
      "Navigation", 8, "ancient unspeakable bugbear", "The Haunted Gallery", 3, "statusNavigation",
    },
    {
      "Galley",
      9,
      "trendy bugbear chef",
      new String[] {
        "The Battlefield (Frat Uniform)", "The Battlefield (Hippy Uniform)",
      },
      3,
      "statusGalley",
    }
  };

  public static String dataToShipZone(Object[] data) {
    return data == null ? "" : (String) data[0];
  }

  public static int dataToId(Object[] data) {
    return data == null ? 0 : ((Integer) data[1]).intValue();
  }

  public static String dataToBugbear(Object[] data) {
    return data == null ? "" : (String) data[2];
  }

  public static String dataToBugbearZone1(Object[] data) {
    if (data == null) {
      return null;
    }

    Object zones = data[3];
    return zones instanceof String
        ? (String) zones
        : zones instanceof String[] ? ((String[]) zones)[0] : "";
  }

  public static String dataToBugbearZone2(Object[] data) {
    if (data == null) {
      return null;
    }

    Object zones = data[3];
    return zones instanceof String ? "" : zones instanceof String[] ? ((String[]) zones)[1] : "";
  }

  public static int dataToLevel(Object[] data) {
    return data == null ? 0 : ((Integer) data[4]).intValue();
  }

  public static String dataToStatusSetting(Object[] data) {
    return data == null ? "" : (String) data[5];
  }

  public static Object[] idToData(final int id) {
    for (int i = 0; i < BugbearManager.BUGBEAR_DATA.length; ++i) {
      Object[] data = BugbearManager.BUGBEAR_DATA[i];
      if (BugbearManager.dataToId(data) == id) {
        return data;
      }
    }
    return null;
  }

  public static Object[] bugbearToData(final String bugbear) {
    for (int i = 0; i < BugbearManager.BUGBEAR_DATA.length; ++i) {
      Object[] data = BugbearManager.BUGBEAR_DATA[i];
      if (bugbear.equals(BugbearManager.dataToBugbear(data))) {
        return data;
      }
    }
    return null;
  }

  public static Object[] shipZoneToData(final String zone) {
    for (int i = 0; i < BugbearManager.BUGBEAR_DATA.length; ++i) {
      Object[] data = BugbearManager.BUGBEAR_DATA[i];
      if (zone.equals(BugbearManager.dataToShipZone(data))) {
        return data;
      }
    }
    return null;
  }

  public static void setBiodata(final Object[] data, final String countString) {
    BugbearManager.setBiodata(data, StringUtilities.parseInt(countString));
  }

  public static void setBiodata(final Object[] data, final int count) {
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
    Object[] data = BugbearManager.shipZoneToData(zone);
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
    for (int i = 0; i < BugbearManager.BUGBEAR_DATA.length; ++i) {
      Object[] zoneData = BugbearManager.BUGBEAR_DATA[i];
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
    for (int i = 0; i < BugbearManager.BUGBEAR_DATA.length; ++i) {
      Object[] zoneData = BugbearManager.BUGBEAR_DATA[i];
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
