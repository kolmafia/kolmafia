package net.sourceforge.kolmafia.session;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LockableListFactory;

public abstract class EncounterManager {
  // Types of special encounters
  public enum EncounterType {
    NONE,
    STOP,
    SEMIRARE,
    CLOVER,
    GLYPH,
    TURTLE,
    SEAL,
    FIST,
    BORIS,
    BADMOON,
    BUGBEAR,
    WANDERER,
    SUPERLIKELY,
    ULTRARARE,
    FREE_COMBAT,
    NOWANDER, // Don't start wandering monster counters
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
  }

  private static Encounter[] specialEncounters;

  static {
    resetEncounters();
  }

  private static void resetEncounters() {
    BufferedReader reader =
        FileUtilities.getVersionedReader("encounters.txt", KoLConstants.ENCOUNTERS_VERSION);

    ArrayList<Encounter> encounters = new ArrayList<Encounter>();
    String[] data;

    while ((data = FileUtilities.readData(reader)) != null) {
      if (!AdventureDatabase.validateAdventureArea(data[0])) {
        RequestLogger.printLine("Invalid adventure area: \"" + data[0] + "\"");
        continue;
      }

      encounters.add(new Encounter(data));
    }

    specialEncounters = encounters.toArray(new Encounter[encounters.size()]);
  }

  /** Utility method used to register a given adventure in the running adventure summary. */
  public void registerAdventure(final KoLAdventure adventureLocation) {
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
      ++previousAdventure.encounterCount;
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
    for (int i = 0; i < specialEncounters.length; ++i) {
      Encounter encounter = specialEncounters[i];
      if (locationName != null && !locationName.equalsIgnoreCase(encounter.location)) {
        continue;
      }
      if (!encounterName.equalsIgnoreCase(encounter.encounter)) {
        continue;
      }
      return encounter;
    }

    return null;
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

  public static final String findEncounterForLocation(
      final String locationName, final EncounterType type) {
    for (int i = 0; i < specialEncounters.length; ++i) {
      Encounter encounter = specialEncounters[i];
      if (!locationName.equalsIgnoreCase(encounter.location)) {
        continue;
      }
      if (!type.equals(encounter.encounterType)) {
        continue;
      }
      return encounter.encounter;
    }

    return null;
  }

  public static final boolean isAutoStop(final String encounterName) {
    if (encounterName.equals("Under the Knife")
        && Preferences.getString("choiceAdventure21").equals("2")) {
      return false;
    }

    EncounterType encounterType = encounterType(encounterName);
    return encounterType == EncounterType.STOP
        || encounterType == EncounterType.BORIS
        || encounterType == EncounterType.GLYPH
        || encounterType == EncounterType.BADMOON;
  }

  public static boolean isRomanticEncounter(final String responseText, final boolean checkMonster) {
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

  public static final boolean isSaberForceZone(String monsterName, String zone) {
    MonsterData monster = MonsterDatabase.findMonster(monsterName);
    return AdventureDatabase.getAreasWithMonster(monster).contains(zone);
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

  public static final boolean isGregariousEncounter(
      final String responseText, final boolean checkMonster) {
    if (responseText.contains("Looks like it's that friend you gregariously made")) {
      return true;
    }

    if (Preferences.getInteger("beGregariousFightsLeft") < 1) {
      return false;
    }

    String monsterName = MonsterStatusTracker.getLastMonsterName();
    return monsterName.equalsIgnoreCase(Preferences.getString("beGregariousMonster"));
  }

  public static final boolean isWanderingMonster(String encounter) {
    MonsterData monster = MonsterDatabase.findMonster(encounter);
    return monster != null && monster.getType().contains(EncounterType.WANDERER);
  }

  public static boolean isSemiRareMonster(String encounter) {
    MonsterData monster = MonsterDatabase.findMonster(encounter);
    return monster != null && monster.getType().contains(EncounterType.SEMIRARE);
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

  private static void recognizeEncounter(final String encounterName, final String responseText) {
    Encounter encounter = EncounterManager.findEncounter(encounterName);
    EncounterType encounterType = EncounterManager.encounterType(encounter, encounterName);

    if (encounterType == EncounterType.BUGBEAR) {
      BugbearManager.registerEncounter(encounter, responseText);
      return;
    }

    // Use of Rain Man skill fires two encounters, first is a non-combat, second a fight, which
    // could be a semi-rare
    if (KoLCharacter.inRaincore() && responseText.contains("simulacrum of a previous foe")) {
      EncounterManager.ignoreSpecialMonsters();
    }

    // Using the Lecture About Relativity skill fires an immediate encounter, a fight, which could
    // be a semi-rare.
    if (EncounterManager.isRelativityMonster()) {
      EncounterManager.ignoreSpecialMonsters();
    }

    // You stop for a moment to catch your breath, and possibly a
    // cold, and hear a wolf whistle from behind you. You spin
    // around and see <monster> that looks suspiciously like the
    // ones you shot with a love arrow earlier.

    // Some semirares can also be clover adventures, if a clover disappears it isn't a semi-rare

    if (encounterType == EncounterType.SEMIRARE
        && !ignoreSpecialMonsters
        && !EncounterManager.isRomanticEncounter(responseText, false)
        && !EncounterManager.isDigitizedEncounter(responseText, false)
        && !EncounterManager.isEnamorangEncounter(responseText, false)
        && !responseText.contains("clover disappears")
        && !FightRequest.edFightInProgress()) {
      KoLCharacter.registerSemirare();
      return;
    }

    if (encounterType == EncounterType.NONE) {
      return;
    }

    if (encounterType == EncounterType.BADMOON) {
      BadMoonManager.registerAdventure(encounterName);
    }

    if (encounterType == EncounterType.STOP
        || encounterType == EncounterType.BORIS
        || encounterType == EncounterType.GLYPH
        || encounterType == EncounterType.BADMOON) {
      // Don't autostop if you have teleportisis
      if (KoLCharacter.hasEquipped(ItemPool.RING_OF_TELEPORTATION, EquipmentManager.ACCESSORY1)
          || KoLCharacter.hasEquipped(ItemPool.RING_OF_TELEPORTATION, EquipmentManager.ACCESSORY2)
          || KoLCharacter.hasEquipped(ItemPool.RING_OF_TELEPORTATION, EquipmentManager.ACCESSORY3)
          || KoLConstants.activeEffects.contains(EncounterManager.TELEPORTITIS)) {
        return;
      }

      GoalManager.checkAutoStop(encounterName);
    }
  }

  /** Utility. The method used to register a given encounter in the running adventure summary. */
  public static void registerEncounter(
      String encounterName, final String encounterType, final String responseText) {
    encounterName = encounterName.trim();

    handleSpecialEncounter(encounterName, responseText);
    recognizeEncounter(encounterName, responseText);

    RegisteredEncounter[] encounters = new RegisteredEncounter[KoLConstants.encounterList.size()];
    KoLConstants.encounterList.toArray(encounters);

    for (int i = 0; i < encounters.length; ++i) {
      if (encounters[i].name.equals(encounterName)) {
        ++encounters[i].encounterCount;

        // Manually set to force repainting in GUI
        KoLConstants.encounterList.set(i, encounters[i]);
        return;
      }
    }

    KoLConstants.encounterList.add(new RegisteredEncounter(encounterType, encounterName));
  }

  public static void handleSpecialEncounter(final String encounterName, final String responseText) {
    if (encounterName.equalsIgnoreCase("Step Up to the Table, Put the Ball in Play")) {
      if (InventoryManager.hasItem(ItemPool.CARONCH_DENTURES)) {
        ResultProcessor.processItem(ItemPool.CARONCH_DENTURES, -1);
        QuestDatabase.setQuestIfBetter(Quest.PIRATE, "step4");
      }

      if (InventoryManager.hasItem(ItemPool.FRATHOUSE_BLUEPRINTS)) {
        ResultProcessor.processItem(ItemPool.FRATHOUSE_BLUEPRINTS, -1);
      }
      return;
    }

    if (encounterName.equalsIgnoreCase("Granny, Does Your Dogfish Bite?")) {
      if (InventoryManager.hasItem(ItemPool.GRANDMAS_MAP)) {
        ResultProcessor.processItem(ItemPool.GRANDMAS_MAP, -1);
      }
      return;
    }

    if (encounterName.equalsIgnoreCase("Meat For Nothing and the Harem for Free")) {
      Preferences.setBoolean("_treasuryEliteMeatCollected", true);
      return;
    }

    if (encounterName.equalsIgnoreCase("Finally, the Payoff")) {
      Preferences.setBoolean("_treasuryHaremMeatCollected", true);
      return;
    }

    if (encounterName.equals("Faction Traction = Inaction")) {
      Preferences.setInteger("booPeakProgress", 98);
      return;
    }

    if (encounterName.equals("Daily Done, John.")) {
      // Daily Dungeon Complete
      Preferences.setBoolean("dailyDungeonDone", true);
      Preferences.setInteger("_lastDailyDungeonRoom", 15);
      return;
    }

    if (encounterName.equals("A hidden surprise!")) {
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
      return this.stringform + " (" + this.encounterCount + ")";
    }

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
