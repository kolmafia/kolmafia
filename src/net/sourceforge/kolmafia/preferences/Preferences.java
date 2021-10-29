package net.sourceforge.kolmafia.preferences;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.combat.CombatActionManager;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.session.MonorailManager;
import net.sourceforge.kolmafia.swingui.AdventureFrame;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.CharPaneDecorator;

public class Preferences {
  // If false, blocks saving of all preferences. Do not modify outside of tests.
  public static boolean saveSettingsToFile = true;

  private static final Object lock = new Object(); // used to synch io

  private static final byte[] LINE_BREAK_AS_BYTES = KoLConstants.LINE_BREAK.getBytes();

  private static final String[] characterMap = new String[65536];

  private static final HashMap<String, String> globalNames = new HashMap<>();
  private static final SortedMap<String, Object> globalValues =
      Collections.synchronizedSortedMap(new TreeMap<>());
  private static File globalPropertiesFile = null;

  private static final HashMap<String, String> userNames = new HashMap<>();
  private static final SortedMap<String, Object> userValues =
      Collections.synchronizedSortedMap(new TreeMap<>());
  private static File userPropertiesFile = null;

  private static final Set<String> defaultsSet = new HashSet<>();
  private static final Set<String> perUserGlobalSet = new HashSet<>();
  private static final Set<String> legacyDailies =
      new TreeSet<>(
          List.of(
              "barrelLayout",
              "bootsCharged",
              "breakfastCompleted",
              "burrowgrubHiveUsed",
              "burrowgrubSummonsRemaining",
              "cocktailSummons",
              "concertVisited",
              "currentMojoFilters",
              "currentPvpVictories",
              "dailyDungeonDone",
              "demonSummoned",
              "expressCardUsed",
              "extraRolloverAdventures",
              "friarsBlessingReceived",
              "grimoire1Summons",
              "grimoire2Summons",
              "grimoire3Summons",
              "lastBarrelSmashed",
              "libramSummons",
              "libraryCardUsed",
              "noodleSummons",
              "nunsVisits",
              "oscusSodaUsed",
              "outrageousSombreroUsed",
              "prismaticSummons",
              "rageGlandVented",
              "reagentSummons",
              "romanticTarget",
              "seaodesFound",
              "spiceMelangeUsed",
              "spookyPuttyCopiesMade",
              "styxPixieVisited",
              "telescopeLookedHigh",
              "tempuraSummons",
              "timesRested",
              "tomeSummons"));

  private static final String[] resetOnAscension =
      new String[] {
        "affirmationCookiesEaten",
        "aminoAcidsUsed",
        "awolDeferredPointsBeanslinger",
        "awolDeferredPointsCowpuncher",
        "awolDeferredPointsSnakeoiler",
        "awolMedicine",
        "awolVenom",
        "backupCameraMode",
        "backupCameraReverserEnabled",
        "bagOTricksCharges",
        "banishingShoutMonsters",
        "bigBrotherRescued",
        "blankOutUsed",
        "bondAdv",
        "bondBeach",
        "bondBeat",
        "bondBooze",
        "bondBridge",
        "bondDesert",
        "bondDR",
        "bondDrunk1",
        "bondDrunk2",
        "bondHoney",
        "bondHP",
        "bondInit",
        "bondItem1",
        "bondItem2",
        "bondItem3",
        "bondJetpack",
        "bondMartiniDelivery",
        "bondMartiniPlus",
        "bondMartiniTurn",
        "bondMeat",
        "bondMox1",
        "bondMox2",
        "bondMPregen",
        "bondMus1",
        "bondMus2",
        "bondMys1",
        "bondMys2",
        "bondSpleen",
        "bondStat",
        "bondStat2",
        "bondStealth",
        "bondStealth2",
        "bondSymbols",
        "bondWar",
        "bondWeapon2",
        "bondWpn",
        "boomBoxSong",
        "camelSpit",
        "cameraMonster",
        "campAwayDecoration",
        "carboLoading",
        "cargoPocketScraps",
        "cargoPocketsEmptied",
        "catBurglarBankHeists",
        "chaosButterflyThrown",
        "charitableDonations",
        "cinderellaMinutesToMidnight",
        "cinderellaScore",
        "commerceGhostItem",
        "copperheadClubHazard",
        "cornucopiasOpened",
        "cozyCounter6332",
        "cozyCounter6333",
        "cozyCounter6334",
        "crappyCameraMonster",
        "crimbotArm",
        "crimbotChassis",
        "crimbotPropulsion",
        "crimboTreeDays",
        "crudeMonster",
        "csServicesPerformed",
        "cubelingProgress",
        "currentEasyBountyItem",
        "currentHardBountyItem",
        "currentHedgeMazeRoom",
        "currentHippyStore",
        "currentSpecialBountyItem",
        "cyrusAdjectives",
        "dampOldBootPurchased",
        "daycareEquipment",
        "daycareInstructors",
        "daycareToddlers",
        "demonName12",
        "demonName13",
        "dnaSyringe",
        "dolphinItem",
        "dreadScroll1",
        "dreadScroll2",
        "dreadScroll3",
        "dreadScroll4",
        "dreadScroll5",
        "dreadScroll6",
        "dreadScroll7",
        "dreadScroll8",
        "dripAdventuresSinceAscension",
        "drippingHallAdventuresSinceAscension",
        "drippingTreesAdventuresSinceAscension",
        "drippyJuice",
        "edPiece",
        "eldritchTentaclesFought",
        "encountersUntilDMTChoice",
        "encountersUntilNEPChoice",
        "ensorcelee",
        "ensorceleeLevel",
        "envyfishMonster",
        "falloutShelterChronoUsed",
        "falloutShelterCoolingTankUsed",
        "fireExtinguisherBatHoleUsed",
        "fireExtinguisherChasmUsed",
        "fireExtinguisherCyrptUsed",
        "fireExtinguisherDesertUsed",
        "fireExtinguisherHaremUsed",
        "fistSkillsKnown",
        "fistTeachingsBarroomBrawl",
        "fistTeachingsBatHole",
        "fistTeachingsConservatory",
        "fistTeachingsFratHouse",
        "fistTeachingsFunHouse",
        "fistTeachingsHaikuDungeon",
        "fistTeachingsMenagerie",
        "fistTeachingsNinjaSnowmen",
        "fistTeachingsPokerRoom",
        "fistTeachingsRoad",
        "fistTeachingsSlums",
        "frenchGuardTurtlesFreed",
        "garbageChampagneCharge",
        "garbageFireProgress",
        "garbageShirtCharge",
        "garbageTreeCharge",
        "gingerBlackmailAccomplished",
        "gingerDigCount",
        "gingerLawChoice",
        "gingerMuscleChoice",
        "gingerNegativesDropped",
        "gingerSubwayLineUnlocked",
        "gladiatorBallMovesKnown",
        "gladiatorBladeMovesKnown",
        "gladiatorNetMovesKnown",
        "gnasirProgress",
        "grimstoneCharge",
        "grimstoneMaskPath",
        "guardTurtlesFreed",
        "guyMadeOfBeesCount",
        "guyMadeOfBeesDefeated",
        "guzzlrDeliveryProgress",
        "hasBartender",
        "hasChef",
        "hasCocktailKit",
        "hasOven",
        "hasRange",
        "hasShaker",
        "hasSushiMat",
        "hermitHax0red",
        "highTopPumped",
        "iceSculptureMonster",
        "itemBoughtPerAscension10790",
        "itemBoughtPerAscension10794",
        "itemBoughtPerAscension10795",
        "itemBoughtPerAscension637",
        "itemBoughtPerAscension8266",
        "jungCharge",
        "lassoTraining",
        "lastAnticheeseDay",
        "lastColosseumRoundWon",
        "lastCopyableMonster",
        "lastCouncilVisit",
        "lastZapperWandExplosionDay",
        "latteModifier",
        "latteUnlocks",
        "leafletCompleted",
        "lockPicked",
        "louvreLayout",
        "mappingMonsters",
        "mapToAnemoneMinePurchased",
        "mapToMadnessReefPurchased",
        "mapToTheDiveBarPurchased",
        "mapToTheMarinaraTrenchPurchased",
        "mapToTheSkateParkPurchased",
        "mayflyExperience",
        "mayoInMouth",
        "mayoLevel",
        "mayoMinderSetting",
        "meansuckerPrice",
        "merkinLockkeyMonster",
        "merkinQuestPath",
        "merkinVocabularyMastery",
        "milkOfMagnesiumActive",
        "miniAdvClass",
        "moonTuned",
        "mushroomGardenCropLevel",
        "nextParanormalActivity",
        "nextQuantumFamiliar",
        "nextQuantumFamiliarTurn",
        "nextSpookyravenElizabethRoom",
        "nextSpookyravenStephenRoom",
        "nosyNoseMonster",
        "optimisticCandleProgress",
        "parasolUsed",
        "pastaThrall1",
        "pastaThrall2",
        "pastaThrall3",
        "pastaThrall4",
        "pastaThrall5",
        "pastaThrall6",
        "pastaThrall7",
        "pastaThrall8",
        "pendingMapReflections",
        "photocopyMonster",
        "plantingDate",
        "plantingDay",
        "plumberBadgeCost",
        "plumberCostumeCost",
        "plumberCostumeWorn",
        "pokefamBoosts",
        "popularTartUnlocked",
        "prayedForGlamour",
        "prayedForProtection",
        "prayedForVigor",
        "procrastinatorLanguageFluency",
        "pyramidBombUsed",
        "pyramidPosition",
        "rainDohMonster",
        "redSnapperProgress",
        "retroCapeSuperhero",
        "retroCapeWashingInstructions",
        "rockinRobinProgress",
        "rumpelstiltskinKidsRescued",
        "rumpelstiltskinTurnsUsed",
        "sausageGrinderUnits",
        "scrapbookCharges",
        "screencappedMonster",
        "seahorseName",
        "shenInitiationDay",
        "shockingLickCharges",
        "singleFamiliarRun",
        "slimelingFullness",
        "slimelingStacksDropped",
        "slimelingStacksDue",
        "smoresEaten",
        "smutOrcNoncombatProgress",
        "snojoMoxieWins",
        "snojoMuscleWins",
        "snojoMysticalityWins",
        "snojoSetting",
        "snowsuit",
        "sourceAgentsDefeated",
        "sourceEnlightenment",
        "sourceInterval",
        "sourceOracleTarget",
        "sourceTerminalEducate1",
        "sourceTerminalEducate2",
        "sourceTerminalEnquiry",
        "spaceBabyLanguageFluency",
        "spaceInvaderDefeated",
        "spacePirateLanguageFluency",
        "spookyPuttyMonster",
        "statbotUses",
        "sugarCounter4178",
        "sugarCounter4179",
        "sugarCounter4180",
        "sugarCounter4181",
        "sugarCounter4182",
        "sugarCounter4183",
        "sugarCounter4191",
        "superficiallyInterestedMonster",
        "telescope1",
        "telescope2",
        "telescope3",
        "telescope4",
        "telescope5",
        "telescope6",
        "telescope7",
        "testudinalTeachings",
        "trapperOre",
        "turtleBlessingTurns",
        "twinPeakProgress",
        "unicornHornInflation",
        "violetFogLayout",
        "waxMonster",
        "wildfireBarrelCaulked",
        "wildfireDusted",
        "wildfireFracked",
        "wildfirePumpGreased",
        "wildfireSprinkled",
        "workteaClue",
        "xoSkeleltonOProgress",
        "xoSkeleltonXProgress",
        "yearbookCameraPending",
        "yearbookCameraTarget",
        "youRobotBody",
        "youRobotBottom",
        "youRobotCPUUpgrades",
        "youRobotLeft",
        "youRobotRight",
        "youRobotScavenged",
        "youRobotTop",
      };

  static {
    // Initialize perUserGlobalSet and read defaults.txt into
    // defaultsSet, globalNames, and userNames
    Preferences.initializeMaps();

    // Read GLOBAL_prefs.txt into globalNames and globalValues
    Preferences.loadGlobalPreferences();
  }

  private static void initializeMaps() {
    // There are three specific per-user settings that appear in
    // GLOBAL_prefs.txt because the LoginFrame needs them

    Preferences.perUserGlobalSet.add("saveState");
    Preferences.perUserGlobalSet.add("displayName");
    Preferences.perUserGlobalSet.add("getBreakfast");

    BufferedReader istream =
        FileUtilities.getVersionedReader("defaults.txt", KoLConstants.DEFAULTS_VERSION);

    String[] current;
    while ((current = FileUtilities.readData(istream)) != null) {
      if (current.length >= 2) {
        String map = current[0];
        String name = current[1];
        String value = current.length == 2 ? "" : current[2];

        HashMap<String, String> desiredMap =
            map.equals("global") ? Preferences.globalNames : Preferences.userNames;
        if (desiredMap.containsKey(name)) {
          System.out.println(map + " setting " + name + " multiply defined");
        }

        HashMap<String, String> otherMap =
            map.equals("global") ? Preferences.userNames : Preferences.globalNames;
        if (otherMap.containsKey(name)) {
          String other = map.equals("global") ? "user" : "global";
          System.out.println(
              map + " setting " + name + " already defined as a " + other + " setting");
          continue;
        }

        desiredMap.put(name, value);

        // Maintain a set of prefs that exist in defaults.txt
        defaultsSet.add(name);
      }
    }

    // Update Mac-specific properties values to ensure
    // that the displays are usable (by default).

    boolean isUsingMac = System.getProperty("os.name").startsWith("Mac");

    Preferences.globalNames.put("useDecoratedTabs", String.valueOf(!isUsingMac));
    Preferences.globalNames.put("chatFontSize", isUsingMac ? "medium" : "small");

    try {
      assert istream != null;
      istream.close();
    } catch (Exception e) {
      // The stream is already closed, go ahead
      // and ignore this error.
    }
  }

  /** Resets all settings so that the given user is represented whenever settings are modified. */
  public static final synchronized void reset(final String username) {
    Preferences.saveToFile(Preferences.globalPropertiesFile, Preferences.globalValues);

    // Prevent anybody from manipulating the user map until we are
    // done bulk-loading it.
    synchronized (Preferences.userValues) {
      if (username == null || username.equals("")) {
        if (Preferences.userPropertiesFile != null) {
          Preferences.saveToFile(Preferences.userPropertiesFile, Preferences.userValues);
          Preferences.userPropertiesFile = null;
          Preferences.userValues.clear();
        }

        return;
      }

      Preferences.loadUserPreferences(username);
    }

    AdventureFrame.updateFromPreferences();
    CharPaneDecorator.updateFromPreferences();
    CombatActionManager.updateFromPreferences();
    MoodManager.updateFromPreferences();
    PreferenceListenerRegistry.fireAllPreferencesChanged();
  }

  public static final String baseUserName(final String name) {
    return name == null || name.equals("")
        ? "GLOBAL"
        : StringUtilities.globalStringReplace(name.trim(), " ", "_").toLowerCase();
  }

  private static void loadGlobalPreferences() {
    File file =
        new File(KoLConstants.SETTINGS_LOCATION, Preferences.baseUserName("") + "_prefs.txt");
    Preferences.globalPropertiesFile = file;

    Properties p = Preferences.loadPreferences(file);
    Preferences.globalValues.clear();

    // GLOBAL_prefs.txt can contain obsolete settings which
    // migrated from global to user. Leave them, since the
    // migration will pull the value from the global map
    for (Entry<Object, Object> entry : p.entrySet()) {
      String key = (String) entry.getKey();
      if (!Preferences.globalNames.containsKey(key)) {
        Preferences.isPerUserGlobalProperty(key);
      } // System.out.println( "obsolete global setting detected: " + key );
      // continue;

      String value = (String) entry.getValue();
      Preferences.globalValues.put(key, value);
    }

    // For all global properties in defaults.txt which were not in
    // GLOBAL_prefs.txt, add to global map with default value.
    for (Entry<String, String> entry : Preferences.globalNames.entrySet()) {
      String key = entry.getKey();
      if (!Preferences.globalValues.containsKey(key)) {
        // System.out.println( "Adding new built-in global setting: " + key );
        String value = entry.getValue();
        Preferences.globalValues.put(key, value);
      }
    }
  }

  private static void loadUserPreferences(final String username) {
    // Apparently the CodeQL security scan requires this as a fix...
    if ((username != null) && (username.contains(".."))) {
      return;
    }
    File file =
        new File(KoLConstants.SETTINGS_LOCATION, Preferences.baseUserName(username) + "_prefs.txt");
    Preferences.userPropertiesFile = file;

    Properties p = Preferences.loadPreferences(file);
    Preferences.userValues.clear();

    for (Entry<Object, Object> currentEntry : p.entrySet()) {
      String key = (String) currentEntry.getKey();
      String value = (String) currentEntry.getValue();

      Preferences.userValues.put(key, value);
    }

    for (Entry<String, String> entry : Preferences.userNames.entrySet()) {
      String key = entry.getKey();
      if (Preferences.userValues.containsKey(key)) {
        continue;
      }

      // If a user property in defaults.txt was not in
      // NAME_prefs.txt, add to user map with default value
      // (this is how we add a new user property)
      //
      // If it had a value in the GLOBAL map, use that (this
      // is how we migrate a preference from GLOBAL to user)
      String value =
          Preferences.globalValues.containsKey(key)
              ? (String) Preferences.globalValues.get(key)
              : entry.getValue();

      // System.out.println( "Adding new built-in user setting: " + key );
      Preferences.userValues.put(key, value);
    }
  }

  private static Properties loadPreferences(File file) {
    InputStream istream = DataUtilities.getInputStream(file);

    Properties p = new Properties();
    try {
      p.load(istream);
    } catch (IOException e) {
      System.out.println(e.getMessage() + " trying to load preferences from file.");
    }

    try {
      istream.close();
    } catch (IOException e) {
      System.out.println(e.getMessage() + " trying to close preferences file.");
    }

    return p;
  }

  private static String encodeProperty(String name, String value) {
    StringBuffer buffer = new StringBuffer();

    Preferences.encodeString(buffer, name);

    if (value != null && value.length() > 0) {
      buffer.append("=");
      Preferences.encodeString(buffer, value);
    }

    return buffer.toString();
  }

  private static void encodeString(StringBuffer buffer, String string) {
    int length = string.length();

    for (int i = 0; i < length; ++i) {
      char ch = string.charAt(i);
      encodeCharacter(ch);
      buffer.append(characterMap[ch]);
    }
  }

  private static void encodeCharacter(char ch) {
    if (characterMap[ch] != null) {
      return;
    }

    switch (ch) {
      case '\t':
        characterMap[ch] = "\\t";
        return;
      case '\n':
        characterMap[ch] = "\\n";
        return;
      case '\f':
        characterMap[ch] = "\\f";
        return;
      case '\r':
        characterMap[ch] = "\\r";
        return;
      case '\\':
      case '=':
      case ':':
      case '#':
      case '!':
        characterMap[ch] = "\\" + ch;
        return;
    }

    characterMap[ch] =
        (ch > 0x0019 && ch < 0x007f)
            ? String.valueOf(ch)
            : (ch < 0x0010)
                ? "\\u000" + Integer.toHexString(ch)
                : (ch < 0x0100)
                    ? "\\u00" + Integer.toHexString(ch)
                    : (ch < 0x1000)
                        ? "\\u0" + Integer.toHexString(ch)
                        : "\\u" + Integer.toHexString(ch);
  }

  public static final boolean propertyExists(final String name, final boolean global) {
    return global
        ? Preferences.globalValues.containsKey(name)
        : Preferences.userValues.containsKey(name);
  }

  public static final String getString(final String name, final boolean global) {
    Object value = null;

    if (global) {
      if (Preferences.globalValues.containsKey(name)) {
        value = Preferences.globalValues.get(name);
      }
    } else {
      if (Preferences.userValues.containsKey(name)) {
        value = Preferences.userValues.get(name);
      }
    }

    return value == null ? "" : value.toString();
  }

  public static final String getDefault(final String name) {
    if (Preferences.globalNames.containsKey(name)) {
      return Preferences.globalNames.get(name);
    }

    if (Preferences.userNames.containsKey(name)) {
      return Preferences.userNames.get(name);
    }

    return "";
  }

  public static final void removeProperty(final String name, final boolean global) {
    // Remove only properties which do not have defaults
    if (global) {
      if (!Preferences.globalNames.containsKey(name)) {
        // We are changing the structure of the map.
        // globalValues is a synchronized map.

        Preferences.globalValues.remove(name);
        if (Preferences.getBoolean("saveSettingsOnSet")) {
          Preferences.saveToFile(Preferences.globalPropertiesFile, Preferences.globalValues);
        }
      }
    } else {
      if (!Preferences.userNames.containsKey(name)) {
        // We are changing the structure of the map.
        // userValues is a synchronized map.

        Preferences.userValues.remove(name);
        if (Preferences.getBoolean("saveSettingsOnSet")) {
          Preferences.saveToFile(Preferences.userPropertiesFile, Preferences.userValues);
        }
      }
    }
  }

  public static final boolean isGlobalProperty(final String name) {
    return Preferences.globalNames.containsKey(name);
  }

  public static boolean isPerUserGlobalProperty(final String property) {
    if (property.contains(".")) {
      for (String prefix : Preferences.perUserGlobalSet) {
        if (property.startsWith(prefix)) {
          return true;
        }
      }
    }
    return false;
  }

  public static final boolean isUserEditable(final String property) {
    return !property.startsWith("saveState") && !property.equals("externalEditor");
  }

  public static final void setString(final String name, final String value) {
    setString(null, name, value);
  }

  public static final String getString(final String name) {
    return getString(null, name);
  }

  public static final void setBoolean(final String name, final boolean value) {
    setBoolean(null, name, value);
  }

  public static final boolean getBoolean(final String name) {
    return getBoolean(null, name);
  }

  public static final void setInteger(final String name, final int value) {
    setInteger(null, name, value);
  }

  public static final int getInteger(final String name) {
    return getInteger(null, name);
  }

  public static final void setFloat(final String name, final float value) {
    setFloat(null, name, value);
  }

  public static final float getFloat(final String name) {
    return getFloat(null, name);
  }

  public static final void setLong(final String name, final long value) {
    setLong(null, name, value);
  }

  public static final long getLong(final String name) {
    return getLong(null, name);
  }

  public static final void setDouble(final String name, final double value) {
    setDouble(null, name, value);
  }

  public static final double getDouble(final String name) {
    return getDouble(null, name);
  }

  public static final int increment(final String name) {
    return Preferences.increment(name, 1);
  }

  public static final int increment(final String name, final int delta) {
    return Preferences.increment(name, delta, 0, false);
  }

  public static final int increment(
      final String name, final int delta, final int max, final boolean mod) {
    int current = Preferences.getInteger(name);
    if (delta != 0) {
      current += delta;

      if (max > 0 && current >= max) {
        if (mod) {
          current %= max;
        } else {
          current = max;
        }
      }

      Preferences.setInteger(name, current);
    }
    return current;
  }

  public static final int decrement(final String name) {
    return Preferences.decrement(name, 1);
  }

  public static final int decrement(final String name, final int delta) {
    return Preferences.decrement(name, delta, 0);
  }

  public static final int decrement(final String name, final int delta, final int min) {
    int current = Preferences.getInteger(name);
    if (delta != 0) {
      current -= delta;

      if (current < min) {
        current = min;
      }

      Preferences.setInteger(name, current);
    }
    return current;
  }

  // Per-user global properties are stored in the global settings with
  // key "<name>.<user>"

  public static final String getString(final String user, final String name) {
    Object value = Preferences.getObject(user, name);

    if (value == null) {
      return "";
    }

    return value.toString();
  }

  public static final boolean getBoolean(final String user, final String name) {
    Map<String, Object> map = Preferences.getMap(name);
    Object value = Preferences.getObject(map, user, name);

    if (value == null) {
      return false;
    }

    if (!(value instanceof Boolean)) {
      value = Boolean.valueOf(value.toString());
      map.put(name, value);
    }

    return (Boolean) value;
  }

  public static final int getInteger(final String user, final String name) {
    Map<String, Object> map = Preferences.getMap(name);
    Object value = Preferences.getObject(map, user, name);

    if (value == null) {
      return 0;
    }

    if (!(value instanceof Integer)) {
      value = IntegerPool.get(StringUtilities.parseInt(value.toString()));
      map.put(name, value);
    }

    return (Integer) value;
  }

  public static final long getLong(final String user, final String name) {
    Map<String, Object> map = Preferences.getMap(name);
    Object value = Preferences.getObject(map, user, name);

    if (value == null) {
      return 0;
    }

    if (!(value instanceof Long)) {
      value = StringUtilities.parseLong(value.toString());
      map.put(name, value);
    }

    return (Long) value;
  }

  public static final float getFloat(final String user, final String name) {
    Map<String, Object> map = Preferences.getMap(name);
    Object value = Preferences.getObject(map, user, name);

    if (value == null) {
      return 0.0f;
    }

    if (!(value instanceof Float)) {
      value = StringUtilities.parseFloat(value.toString());
      map.put(name, value);
    }

    return (Float) value;
  }

  public static final double getDouble(final String user, final String name) {
    Map<String, Object> map = Preferences.getMap(name);
    Object value = Preferences.getObject(map, user, name);

    if (value == null) {
      return 0.0;
    }

    if (!(value instanceof Double)) {
      value = StringUtilities.parseDouble(value.toString());
      map.put(name, value);
    }

    return (Double) value;
  }

  private static Map<String, Object> getMap(final String name) {
    return Preferences.isGlobalProperty(name) ? Preferences.globalValues : Preferences.userValues;
  }

  private static Object getObject(final String user, final String name) {
    return Preferences.getObject(Preferences.getMap(name), user, name);
  }

  private static Object getObject(
      final Map<String, Object> map, final String user, final String name) {
    String key = Preferences.propertyName(user, name);
    return map.get(key);
  }

  public static final TreeMap<String, String> getMap(boolean defaults, boolean user) {
    if (defaults) {
      return new TreeMap<>(user ? userNames : globalNames);
    } else {
      TreeMap<String, String> map = new TreeMap<>();
      Map<String, Object> srcmap = user ? userValues : globalValues;
      for (String pref : srcmap.keySet()) {
        map.put(pref, getString(pref));
      }
      return map;
    }
  }

  public static final void setString(final String user, final String name, final String value) {
    String old = Preferences.getString(user, name);
    if (!old.equals(value)) {
      Preferences.setObject(user, name, value, value);
    }
  }

  public static final void setBoolean(final String user, final String name, final boolean value) {
    boolean old = Preferences.getBoolean(user, name);
    if (old != value) {
      Preferences.setObject(user, name, value ? "true" : "false", value);
    }
  }

  public static final void setInteger(final String user, final String name, final int value) {
    int old = Preferences.getInteger(user, name);
    if (old != value) {
      Preferences.setObject(user, name, String.valueOf(value), IntegerPool.get(value));
    }
  }

  public static final void setLong(final String user, final String name, final long value) {
    long old = Preferences.getLong(user, name);
    if (old != value) {
      Preferences.setObject(user, name, String.valueOf(value), value);
    }
  }

  public static final void setFloat(final String user, final String name, final float value) {
    float old = Preferences.getFloat(user, name);
    if (old != value) {
      Preferences.setObject(user, name, String.valueOf(value), value);
    }
  }

  public static final void setDouble(final String user, final String name, final double value) {
    double old = Preferences.getDouble(user, name);
    if (old != value) {
      Preferences.setObject(user, name, String.valueOf(value), value);
    }
  }

  private static void setObject(
      final String user, final String name, final String value, final Object object) {
    if (Preferences.getBoolean("logPreferenceChange")) {
      Set<String> preferenceFilter = new HashSet<>();
      Collections.addAll(
          preferenceFilter, Preferences.getString("logPreferenceChangeFilter").split(","));
      if (!preferenceFilter.contains(name)) {
        String message =
            "Preference " + name + " changed from " + Preferences.getString(name) + " to " + value;
        RequestLogger.printLine(message);
        RequestLogger.updateSessionLog(message);
      }
    }

    if (Preferences.isGlobalProperty(name)) {
      String actualName = Preferences.propertyName(user, name);

      // We might be changing the structure of the map.
      // globalValues is a synchronized map.

      Preferences.globalValues.put(actualName, object);
      if (Preferences.getBoolean("saveSettingsOnSet")) {
        Preferences.saveToFile(Preferences.globalPropertiesFile, Preferences.globalValues);
      }
    } else if (Preferences.userPropertiesFile != null) {
      // We might be changing the structure of the map.
      // userValues is a synchronized map.

      Preferences.userValues.put(name, object);
      if (Preferences.getBoolean("saveSettingsOnSet")) {
        Preferences.saveToFile(Preferences.userPropertiesFile, Preferences.userValues);
      }
    }

    PreferenceListenerRegistry.firePreferenceChanged(name);

    if (name.startsWith("choiceAdventure")) {
      PreferenceListenerRegistry.firePreferenceChanged("choiceAdventure*");
    }
  }

  private static String propertyName(final String user, final String name) {
    return user == null ? name : name + "." + Preferences.baseUserName(user);
  }

  private static void saveToFile(File file, Map<String, Object> data) {
    if (!Preferences.saveSettingsToFile) {
      return;
    }

    // See Collections.synchronizedSortedMap
    //
    // We are essentially iterating over the map. Not exactly - we
    // are iterating over the entrySet - but let's keep the map and
    // the file in synch atomically

    synchronized (lock) {
      // Determine the contents of the file by
      // actually printing them.

      ByteArrayOutputStream ostream = new ByteArrayOutputStream();

      try {
        for (Entry<String, Object> current : data.entrySet()) {
          ostream.write(
              Preferences.encodeProperty(current.getKey(), current.getValue().toString())
                  .getBytes());
          ostream.write(LINE_BREAK_AS_BYTES);
        }
      } catch (IOException e) {
        System.out.println(e.getMessage() + " trying to write preferences as byte array.");
      }

      OutputStream fstream = DataUtilities.getOutputStream(file);

      try {
        ostream.writeTo(fstream);
      } catch (IOException e) {
        System.out.println(e.getMessage() + " trying to write preferences as stream.");
      }

      try {
        fstream.close();
      } catch (IOException e) {
        System.out.println(e.getMessage() + " trying to close preferences stream.");
      }
    }
  }

  public static void resetToDefault(String name) {
    if (Preferences.userNames.containsKey(name)) {
      Preferences.setString(name, Preferences.userNames.get(name));
    } else if (Preferences.globalNames.containsKey(name)) {
      Preferences.setString(name, Preferences.globalNames.get(name));
    }
  }

  public static boolean isDaily(String name) {
    return name.startsWith("_") || legacyDailies.contains(name);
  }

  public static void resetPerAscension() {
    // Most prefs that get reset on ascension just return to their default value
    for (String pref : resetOnAscension) {
      resetToDefault(pref);
    }

    // Some need special treatment
    MonorailManager.resetMuffinOrder();

    // Deferred ascension rewards
    Preferences.setInteger(
        "yearbookCameraUpgrades", Preferences.getInteger("yearbookCameraAscensions"));
    Preferences.increment(
        "awolPointsBeanslinger", Preferences.getInteger("awolDeferredPointsBeanslinger"));
    Preferences.increment(
        "awolPointsCowpuncher", Preferences.getInteger("awolDeferredPointsCowpuncher"));
    Preferences.increment(
        "awolPointsSnakeoiler", Preferences.getInteger("awolDeferredPointsSnakeoiler"));
    Preferences.increment("noobPoints", Preferences.getInteger("noobDeferredPoints"));
  }

  public static void resetDailies() {
    // See Collections.synchronizedSortedMap
    //
    // userValues is a synchronized map, but we are doing a mass
    // change to it.

    synchronized (Preferences.userValues) {
      Iterator<String> it = Preferences.userValues.keySet().iterator();
      while (it.hasNext()) {
        String name = it.next();
        if (isDaily(name)) {
          if (!Preferences.containsDefault(name)) {
            // fully delete preferences that start with _ and aren't in defaults.txt
            it.remove();
            continue;
          }
          String val = Preferences.userNames.get(name);
          if (val == null) val = "";
          Preferences.setString(name, val);
        }
      }

      if (Preferences.getBoolean("saveSettingsOnSet")) {
        Preferences.saveToFile(Preferences.userPropertiesFile, Preferences.userValues);
      }
    }
  }

  public static void resetGlobalDailies() {
    // See Collections.synchronizedSortedMap
    //
    // globalValues is a synchronized map, but we are doing a mass
    // change to it.

    synchronized (Preferences.globalValues) {
      for (String name : Preferences.globalValues.keySet()) {
        if (isDaily(name)) {
          String val = Preferences.globalNames.get(name);
          if (val == null) val = "";
          Preferences.setString(name, val);
        }
      }

      Preferences.setLong("lastGlobalCounterDay", KoLCharacter.getRollover());

      if (Preferences.getBoolean("saveSettingsOnSet")) {
        Preferences.saveToFile(Preferences.globalPropertiesFile, Preferences.globalValues);
      }
    }
  }

  public static boolean containsDefault(String key) {
    return defaultsSet.contains(key);
  }
}
