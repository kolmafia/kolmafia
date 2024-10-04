package net.sourceforge.kolmafia.session;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LockableListFactory;

public abstract class EncounterManager {
  // Types of special encounters

  public enum EncounterType {
    NONE,
    STOP(true),
    LUCKY,
    GLYPH(true),
    TURTLE,
    SEAL,
    FIST,
    BORIS(true),
    BADMOON(true),
    BUGBEAR,
    WANDERER,
    SUPERLIKELY,
    ULTRARARE,
    FREE_COMBAT,
    NOWANDER; // Don't start wandering monster counters

    private final boolean autostop;

    EncounterType(final boolean autostop) {
      this.autostop = autostop;
    }

    EncounterType() {
      this(false);
    }

    public final boolean isAutostop() {
      return this.autostop;
    }
  }

  public static class Encounter {
    String location;
    EncounterType encounterType;
    String encounter;

    public Encounter(String[] row) {
      location = row[0];
      encounterType = EncounterType.valueOf(row[1]);
      encounter = row[2];
    }

    public String getLocation() {
      return this.location;
    }

    public EncounterType getEncounterType() {
      return this.encounterType;
    }

    public String getEncounter() {
      return this.encounter;
    }

    @Override
    public String toString() {
      return this.encounter;
    }
  }

  private static Encounter[] specialEncounters;

  static {
    resetEncounters();
  }

  private static void resetEncounters() {
    ArrayList<Encounter> encounters = new ArrayList<>();

    try (BufferedReader reader =
        FileUtilities.getVersionedReader("encounters.txt", KoLConstants.ENCOUNTERS_VERSION)) {
      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        if (!AdventureDatabase.validateAdventureArea(data[0])) {
          RequestLogger.printLine("Invalid adventure area: \"" + data[0] + "\"");
          continue;
        }

        encounters.add(new Encounter(data));
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }

    specialEncounters = encounters.toArray(new Encounter[encounters.size()]);
  }

  /** Utility method used to register a given adventure in the running adventure summary. */
  public static void registerAdventure(final KoLAdventure adventureLocation) {
    if (adventureLocation == null) {
      return;
    }
    EncounterManager.registerAdventure(adventureLocation.getAdventureName());
  }

  public static void registerAdventure(final String adventureName) {
    if (adventureName == null) {
      return;
    }

    RegisteredEncounter previousAdventure =
        LockableListFactory.lastElement(KoLConstants.adventureList);

    if (previousAdventure != null && previousAdventure.name.equals(adventureName)) {
      previousAdventure.increment();
      // Even though the RegisteredEncounter is mutated above, this runs
      // [LockableListModel.fireContentsChanged]
      // which will result in the pane being updated in a Swing context.
      KoLConstants.adventureList.set(KoLConstants.adventureList.size() - 1, previousAdventure);
    } else {
      KoLConstants.adventureList.add(new RegisteredEncounter(null, adventureName));
    }
  }

  public static final Encounter findEncounter(final String encounterName) {
    return findEncounter(KoLAdventure.lastVisitedLocation(), encounterName);
  }

  public static final Encounter findEncounter(
      final KoLAdventure adventureLocation, final String encounterName) {
    String locationName = adventureLocation == null ? null : adventureLocation.getAdventureName();
    return findEncounter(locationName, encounterName);
  }

  public static final Encounter findEncounter(
      final String locationName, final String encounterName) {
    return Arrays.stream(specialEncounters)
        .filter(e -> locationName == null || e.getLocation().equalsIgnoreCase(locationName))
        .filter(e -> e.getEncounter().equalsIgnoreCase(encounterName))
        .findAny()
        .orElse(null);
  }

  private static EncounterType encounterType(final String encounterName) {
    Encounter encounter = EncounterManager.findEncounter(encounterName);
    return EncounterManager.encounterType(encounter, encounterName);
  }

  private static EncounterType encounterType(
      final Encounter encounter, final String encounterName) {
    return encounter != null
        ? encounter.encounterType
        : BadMoonManager.specialAdventure(encounterName)
            ? EncounterType.BADMOON
            : EncounterType.NONE;
  }

  public static final Encounter findEncounterForLocation(
      final String locationName, final EncounterType type) {
    return Arrays.stream(specialEncounters)
        .filter(e -> e.getLocation().equalsIgnoreCase(locationName))
        .filter(e -> e.getEncounterType() == type)
        .findAny()
        .orElse(null);
  }

  public static final boolean isAutoStop(final String encounterName) {
    if (encounterName.equals("Under the Knife")
        && Preferences.getString("choiceAdventure21").equals("2")) {
      return false;
    }

    return encounterType(encounterName).isAutostop();
  }

  public static boolean isRomanticEncounter(final String responseText, final boolean checkMonster) {
    // You stop for a moment to catch your breath, and possibly a
    // cold, and hear a wolf whistle from behind you. You spin
    // around and see <monster> that looks suspiciously like the
    // ones you shot with a love arrow earlier.
    if (responseText.contains("hear a wolf whistle")
        || responseText.contains("you feel the hairs")) {
      return true;
    }

    // This is called from some places before the next monster is set.
    // Don't bother checking the monster in those cases.
    if (checkMonster
        && TurnCounter.isCounting("Romantic Monster window end", 0, 10)
        && Preferences.getString("nextAdventure").equals("The Deep Machine Tunnels")) {
      String name = MonsterStatusTracker.getLastMonsterName();
      return name.equalsIgnoreCase(Preferences.getString("romanticTarget"));
    }

    return false;
  }

  public static final boolean isEnamorangEncounter(
      final String responseText, final boolean checkMonster) {
    if (responseText.contains("tangled heartstrings")) {
      return true;
    }

    if (checkMonster
        && TurnCounter.isCounting("Enamorang Monster", 0)
        && Preferences.getString("nextAdventure").equals("The Deep Machine Tunnels")) {
      String name = MonsterStatusTracker.getLastMonsterName();
      return name.equalsIgnoreCase(Preferences.getString("enamorangMonster"));
    }

    return false;
  }

  public static boolean isSpookyVHSTapeMonster(
      final String responseText, final boolean checkMonster) {
    if (responseText.contains("suddenly roll back and they fall... dead")) {
      return true;
    }

    if (checkMonster
        && TurnCounter.isCounting("Spooky VHS Tape Monster", 0)
        && Preferences.getString("nextAdventure").equals("The Deep Machine Tunnels")) {
      String name = MonsterStatusTracker.getLastMonsterName();
      return name.equalsIgnoreCase(Preferences.getString("spookyVHSTapeMonster"));
    }

    return false;
  }

  public static final boolean isDigitizedEncounter(
      final String responseText, final boolean checkMonster) {
    if (responseText.contains("must have hit CTRL+V")) {
      return true;
    }

    // This is called from some places before the next monster is set.
    // Don't bother checking the monster in those cases.
    if (checkMonster
        && TurnCounter.isCounting("Digitize Monster", 0)
        && Preferences.getString("nextAdventure").equals("The Deep Machine Tunnels")) {
      String name = MonsterStatusTracker.getLastMonsterName();
      return name.equalsIgnoreCase(Preferences.getString("_sourceTerminalDigitizeMonster"));
    }

    return false;
  }

  public static final boolean isSaberForceMonster(String monsterName) {
    // There's no message to check for, and the monster generally shows up
    // immediately, so assume the correct monster is from this skill
    if (Preferences.getInteger("_saberForceMonsterCount") < 1) {
      return false;
    }

    return monsterName.equalsIgnoreCase(Preferences.getString("_saberForceMonster"));
  }

  public static final boolean isRedWhiteBlueMonster(final String responseText) {
    return responseText.contains("still hanging around to watch the fireworks");
  }

  public static final boolean isSaberForceZone(String monsterName, String zone) {
    MonsterData monster = MonsterDatabase.findMonster(monsterName);
    return AdventureDatabase.getAreaCombatData(zone).hasMonster(monster);
  }

  public static final boolean isSaberForceZone(String zone) {
    return isSaberForceZone(Preferences.getString("_saberForceMonster"), zone);
  }

  public static final boolean isSaberForceMonster(MonsterData monster, String zone) {
    return isSaberForceMonster(monster.getName(), zone);
  }

  public static final boolean isSaberForceMonster(String monsterName, String zone) {
    if (!isSaberForceZone(monsterName, zone)) {
      return false;
    }

    return isSaberForceMonster(monsterName);
  }

  public static final boolean isSaberForceMonster() {
    String name = MonsterStatusTracker.getLastMonsterName();
    return isSaberForceMonster(name);
  }

  public static final boolean isRelativityMonster() {
    // There's no message to check for, and the monster always
    // shows up immediately following the fight as a chained
    // encounter.
    if (Preferences.getBoolean("_relativityMonster")) {
      Preferences.setBoolean("_relativityMonster", false);
      return true;
    }
    return false;
  }

  public static boolean isMimeographEncounter(final String responseText) {
    return RequestEditorKit.parseCosmeticModifiers(responseText).contains("mimeo");
  }

  public static boolean isRainManEncounter(final String responseText) {
    // Use of Rain Man skill fires two encounters, first is a non-combat, second a fight, which
    // could be a semi-rare
    return KoLCharacter.inRaincore() && responseText.contains("simulacrum of a previous foe");
  }

  public static final boolean isGregariousEncounter(final String responseText) {
    return responseText.contains("Looks like it's that friend you gregariously made");
  }

  public static final boolean isHabitatFactEncounter(final String responseText) {
    return responseText.contains("Fun fact that you just remembered");
  }

  public static final boolean isBodyguardEncounter(final String responseText) {
    return KoLCharacter.inAvantGuard() && responseText.contains("acting as the bodyguard to");
  }

  public static final boolean isWanderingMonster(String encounter) {
    MonsterData monster = MonsterDatabase.findMonster(encounter);
    return monster != null && monster.getType().contains(EncounterType.WANDERER);
  }

  public static boolean isLuckyMonster(String encounter) {
    MonsterData monster = MonsterDatabase.findMonster(encounter);
    return monster != null && monster.getType().contains(EncounterType.LUCKY);
  }

  public static boolean isSuperlikelyMonster(String encounter) {
    MonsterData monster = MonsterDatabase.findMonster(encounter);
    return monster != null && monster.getType().contains(EncounterType.SUPERLIKELY);
  }

  public static boolean isFreeCombatMonster(String encounter) {
    MonsterData monster = MonsterDatabase.findMonster(encounter);
    return monster != null && monster.getType().contains(EncounterType.FREE_COMBAT);
  }

  public static boolean isUltrarareMonster(String encounter) {
    MonsterData monster = MonsterDatabase.findMonster(encounter);
    return monster != null && monster.getType().contains(EncounterType.ULTRARARE);
  }

  public static boolean isNoWanderMonster(String encounter) {
    MonsterData monster = MonsterDatabase.findMonster(encounter);
    return monster != null && monster.getType().contains(EncounterType.NOWANDER);
  }

  // Used to ignore special monsters re-encountered via copying
  public static boolean ignoreSpecialMonsters = false;

  public static void ignoreSpecialMonsters() {
    ignoreSpecialMonsters = true;
  }

  private static final AdventureResult TELEPORTITIS = EffectPool.get(EffectPool.TELEPORTITIS);
  private static final AdventureResult FEELING_LOST = EffectPool.get(EffectPool.FEELING_LOST);

  private static void recognizeEncounter(final String encounterName, final String responseText) {
    Encounter encounter = EncounterManager.findEncounter(encounterName);
    EncounterType encounterType = EncounterManager.encounterType(encounter, encounterName);

    if (encounterType == EncounterType.BUGBEAR) {
      BugbearManager.registerEncounter(encounter, responseText);
      return;
    }

    if (encounterType == EncounterType.NONE) {
      return;
    }

    if (encounterType == EncounterType.BADMOON) {
      BadMoonManager.registerAdventure(encounterName);
    }

    if (encounterType.isAutostop()) {
      // Don't autostop if you have teleportisis
      if (KoLCharacter.hasEquipped(ItemPool.RING_OF_TELEPORTATION, Slot.ACCESSORY1)
          || KoLCharacter.hasEquipped(ItemPool.RING_OF_TELEPORTATION, Slot.ACCESSORY2)
          || KoLCharacter.hasEquipped(ItemPool.RING_OF_TELEPORTATION, Slot.ACCESSORY3)
          || KoLConstants.activeEffects.contains(TELEPORTITIS)
          || KoLConstants.activeEffects.contains(FEELING_LOST)) {
        return;
      }

      GoalManager.checkAutoStop(encounterName);
    }
  }

  /** Utility. The method used to register a given encounter in the running adventure summary. */
  public static void registerEncounter(
      final String encounterName, final String encounterType, final String responseText) {
    final String name = encounterName.trim();

    handleSpecialEncounter(name, responseText);
    recognizeEncounter(name, responseText);

    IntStream.range(0, KoLConstants.encounterList.size())
        .mapToObj(i -> Map.entry(i, KoLConstants.encounterList.get(i)))
        .filter(e -> e.getValue().name.equalsIgnoreCase(name))
        .findFirst()
        .ifPresentOrElse(
            e -> {
              var encounter = e.getValue();
              encounter.increment();
              // Even though RegisteredEncounter is mutated above, this runs
              // [LockableListModel.fireContentsChanged]
              // which will result in the pane being updated in a Swing context
              KoLConstants.encounterList.set(e.getKey(), encounter);
            },
            () ->
                KoLConstants.encounterList.add(
                    new RegisteredEncounter(encounterType, encounterName)));
  }

  public static void handleSpecialEncounter(final String encounterName, final String responseText) {
    switch (encounterName.toLowerCase()) {
      case "step up to the table, put the ball in play" -> {
        if (InventoryManager.hasItem(ItemPool.CARONCH_DENTURES)) {
          ResultProcessor.processItem(ItemPool.CARONCH_DENTURES, -1);
          QuestDatabase.setQuestIfBetter(Quest.PIRATE, "step4");
        }
        if (InventoryManager.hasItem(ItemPool.FRATHOUSE_BLUEPRINTS)) {
          ResultProcessor.processItem(ItemPool.FRATHOUSE_BLUEPRINTS, -1);
        }
        return;
      }
      case "granny, does your dogfish bite?" -> {
        if (InventoryManager.hasItem(ItemPool.GRANDMAS_MAP)) {
          ResultProcessor.processItem(ItemPool.GRANDMAS_MAP, -1);
        }
        return;
      }
      case "meat for nothing and the harem for free" -> {
        Preferences.setBoolean("_treasuryEliteMeatCollected", true);
        return;
      }
      case "finally, the payoff" -> {
        Preferences.setBoolean("_treasuryHaremMeatCollected", true);
        return;
      }
      case "faction traction = inaction" -> {
        Preferences.setInteger("booPeakProgress", 98);
        return;
      }
      case "daily done, john." -> {
        // Daily Dungeon Complete
        Preferences.setBoolean("dailyDungeonDone", true);
        Preferences.setInteger("_lastDailyDungeonRoom", 15);
        return;
      }
      case "a hidden surprise!" -> {
        // Since this content is short-lived, create the patterns here every time
        // the encounter is found instead of globally
        Pattern GIFT_SENDER_PATTERN = Pattern.compile("nounder><b>(.*?)</b></a>");
        Pattern NOTE_PATTERN = Pattern.compile("1px solid black;'>(.*?)</td></tr>", Pattern.DOTALL);
        Matcher senderMatcher = GIFT_SENDER_PATTERN.matcher(responseText);
        if (senderMatcher.find()) {
          String sender = senderMatcher.group(1);
          RequestLogger.printLine("Gift sender: " + sender);
          RequestLogger.updateSessionLog("Gift sender: " + sender);
        }
        Matcher noteMatcher = NOTE_PATTERN.matcher(responseText);
        if (noteMatcher.find()) {
          String note = noteMatcher.group(1);
          RequestLogger.printLine("Gift note: " + note);
          RequestLogger.updateSessionLog("Gift note: " + note);
        }
        return;
      }
      case "labrador conspirator" -> {
        Preferences.increment("hallowienerCoinspiracy");
        return;
      }
      case "lava dogs" -> {
        Preferences.setBoolean("hallowienerVolcoino", true);
        return;
      }
      case "fruuuuuuuit" -> {
        Preferences.setBoolean("hallowienerSkeletonStore", true);
        return;
      }
      case "boooooze hound" -> {
        Preferences.setBoolean("hallowienerOvergrownLot", true);
        return;
      }
      case "baker's dogzen" -> {
        Preferences.setBoolean("hallowienerMadnessBakery", true);
        return;
      }
      case "dog needs food badly" -> {
        Preferences.increment("hallowiener8BitRealm");
        return;
      }
      case "ratchet-catcher" -> {
        Preferences.setBoolean("hallowienerMiddleChamber", true);
        return;
      }
      case "seeing-eyes dog" -> {
        Preferences.setBoolean("hallowienerDefiledNook", true);
        return;
      }
      case "carpenter dog" -> {
        Preferences.setBoolean("hallowienerSmutOrcs", true);
        return;
      }
      case "are they made of real dogs?" -> {
        Preferences.setBoolean("hallowienerGuanoJunction", true);
        return;
      }
      case "gunbowwowder" -> {
        Preferences.setBoolean("hallowienerSonofaBeach", true);
        return;
      }
      case "it isn't a poodle" -> {
        Preferences.setBoolean("hallowienerKnollGym", true);
        return;
      }
    }
  }

  public static class RegisteredEncounter implements Comparable<RegisteredEncounter> {
    private final String type;
    private final String name;
    private final String stringform;
    private int encounterCount;

    public RegisteredEncounter(final String type, final String name) {
      this.type = type;
      // The name is likely a substring of a page load, so storing it
      // as-is would keep the entire page in memory.
      this.name = name;

      this.stringform = type == null ? name : type + ": " + name;
      this.encounterCount = 1;
    }

    @Override
    public String toString() {
      return this.stringform + " (" + KoLConstants.COMMA_FORMAT.format(this.encounterCount) + ")";
    }

    public void increment() {
      this.encounterCount++;
    }

    public String getName() {
      return this.name;
    }

    public int getCount() {
      return this.encounterCount;
    }

    @Override
    public int compareTo(final RegisteredEncounter o) {
      if (o == null) {
        return -1;
      }

      if (this.type == null || o.type == null || this.type.equals(o.type)) {
        return this.name.compareToIgnoreCase(o.name);
      }

      return this.type.equals("Combat") ? 1 : -1;
    }
  }
}
