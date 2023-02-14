package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withSign;
import static internal.matchers.Preference.isSetTo;
import static internal.matchers.Quest.isStep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

class EncounterManagerTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("EncounterManagerTest");
    Preferences.reset("EncounterManagerTest");
    KoLmafia.resetSession();
    EncounterManager.ignoreSpecialMonsters = false;
    KoLAdventure.setLastAdventure("");
  }

  @AfterAll
  public static void cleanup() {
    TurnCounter.clearCounters();
  }

  @Test
  void canRegisterNewAdventureByString() {
    EncounterManager.registerAdventure("The Sleazy Back Alley");

    assertThat(KoLConstants.adventureList, hasSize(1));
    assertThat(KoLConstants.adventureList.get(0).toString(), equalTo("The Sleazy Back Alley (1)"));
  }

  @Test
  void canRegisterNewAdventureByKoLAdventure() {
    var adv = AdventureDatabase.getAdventure("The Sleazy Back Alley");
    EncounterManager.registerAdventure(adv);

    assertThat(KoLConstants.adventureList, hasSize(1));
    assertThat(KoLConstants.adventureList.get(0).toString(), equalTo("The Sleazy Back Alley (1)"));
  }

  @Test
  void canRegisterRepeatAdventure() {
    EncounterManager.registerAdventure("The Sleazy Back Alley");
    EncounterManager.registerAdventure("The Sleazy Back Alley");

    assertThat(KoLConstants.adventureList, hasSize(1));
    assertThat(
        KoLConstants.adventureList, contains(hasToString(equalTo("The Sleazy Back Alley (2)"))));
  }

  @Test
  void canAddDifferentAdventureToList() {
    EncounterManager.registerAdventure("The Sleazy Back Alley");
    EncounterManager.registerAdventure("The Skeleton Store");

    assertThat(KoLConstants.adventureList, hasSize(2));
    assertThat(
        KoLConstants.adventureList,
        contains(
            hasToString(equalTo("The Sleazy Back Alley (1)")),
            hasToString(equalTo("The Skeleton Store (1)"))));
  }

  @Test
  void findEncounterByName() {
    var enc = EncounterManager.findEncounter("With a Clatter");

    assertThat(enc, notNullValue());
    assertThat(enc.getEncounterType(), equalTo(EncounterManager.EncounterType.LUCKY));
    assertThat(enc.getLocation(), equalTo("The Haunted Gallery"));
    assertThat(enc.getEncounter(), equalTo("With a Clatter"));
  }

  @Test
  void findEncounterByNameAndNullLocation() {
    var enc = EncounterManager.findEncounter((KoLAdventure) null, "With a Clatter");

    assertThat(enc, notNullValue());
    assertThat(enc.getEncounterType(), equalTo(EncounterManager.EncounterType.LUCKY));
    assertThat(enc.getLocation(), equalTo("The Haunted Gallery"));
  }

  @Test
  void doNotFindNonExistantEncounter() {
    var enc = EncounterManager.findEncounter("Timmy Tinkles Goes To College!");

    assertThat(enc, nullValue());
  }

  @Test
  void findEncounterByNameAndLocation() {
    var enc = EncounterManager.findEncounter("The Limerick Dungeon", "Thud");

    assertThat(enc, notNullValue());
    assertThat(enc.getEncounterType(), equalTo(EncounterManager.EncounterType.GLYPH));
  }

  @Test
  void findEncounterByNameAndKoLAdventure() {
    var adv = AdventureDatabase.getAdventure("Waste Processing");

    var enc = EncounterManager.findEncounter(adv, "Smell Bad!");

    assertThat(enc, notNullValue());
    assertThat(enc.getEncounterType(), equalTo(EncounterManager.EncounterType.BUGBEAR));
  }

  @Test
  void doNotFindExistingEncounterAtWrongLocation() {
    var enc = EncounterManager.findEncounter("The Sleazy Back Alley", "Thud");

    assertThat(enc, nullValue());
  }

  @Test
  void findEncounterByForLocationType() {
    var enc =
        EncounterManager.findEncounterForLocation(
            "Whitey's Grove", EncounterManager.EncounterType.LUCKY);

    assertThat(enc, hasToString(equalTo("Monty of County Crisco")));
  }

  @Test
  void doNotFindEncounterByForLocationTypeIfDoesntExist() {
    var enc =
        EncounterManager.findEncounterForLocation(
            "Whitey's Grove", EncounterManager.EncounterType.GLYPH);

    assertThat(enc, nullValue());
  }

  @ParameterizedTest
  @CsvSource({
    "History is Fun!, true", // STOP
    "Methinks the Protesters Doth Protest Too Little, false", // LUCKY
    "Jackin' the Jukebox, true", // BORIS
    "My Little Stowaway, true", // GLYPH
    "The Ghost in You, false", // BUGBEAR
  })
  void canTellAutostop(String encounterName, String expected) {
    boolean actual = EncounterManager.isAutoStop(encounterName);

    assertThat(actual, equalTo(Boolean.parseBoolean(expected)));
  }

  @Test
  void canRecogniseBadmoonAutostopInBadmoon() {
    var cleanups = withSign(ZodiacSign.BAD_MOON);
    try (cleanups) {
      boolean actual = EncounterManager.isAutoStop("Getting Hammered");

      assertThat(actual, equalTo(true));
    }
  }

  @Test
  void cannotRecogniseBadmoonAutostopNotInBadmoon() {
    boolean actual = EncounterManager.isAutoStop("Getting Hammered");

    assertThat(actual, equalTo(false));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void underTheKnifeIsConditionalAutostop(boolean skipping) {
    Preferences.setInteger("choiceAdventure21", skipping ? 2 : 1);
    boolean actual = EncounterManager.isAutoStop("Under the Knife");

    assertThat(actual, equalTo(!skipping));
  }

  @Test
  void isRomanticEncounterBasedOnResponseText() {
    String html = html("request/test_fight_romantic_monster.html");

    boolean actual = EncounterManager.isRomanticEncounter(html, false);

    assertThat(actual, equalTo(true));
  }

  @Test
  void isRomanticEncounterBasedOnMonster() {
    TurnCounter.startCounting(10, "Romantic Monster window end loc=* type=wander", "rparen.gif");
    KoLAdventure.setNextAdventure("The Deep Machine Tunnels");
    Preferences.setString("romanticTarget", "Witchess Knight");
    MonsterStatusTracker.setNextMonster(MonsterDatabase.findMonster("Witchess Knight"));
    String html = html("request/test_fight_witchess_knight_in_dmt.html");

    boolean actual = EncounterManager.isRomanticEncounter(html, true);

    assertThat(actual, equalTo(true));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void noRomanticEncounterFalsePositive(boolean checkMonster) {
    String html = html("request/test_fight_oil_baron.html");

    boolean actual = EncounterManager.isRomanticEncounter(html, checkMonster);

    assertThat(actual, equalTo(false));
  }

  @Disabled("Need some HTML")
  @Test
  void isEnamorangEncounterBasedOnResponseText() {
    String html = html("request/test_fight_enamorang_monster.html");

    boolean actual = EncounterManager.isEnamorangEncounter(html, false);

    assertThat(actual, equalTo(true));
  }

  @Test
  void isEnamorangEncounterBasedOnMonster() {
    KoLCharacter.setCurrentRun(0);
    TurnCounter.startCounting(1, "Enamorang Monster loc=* type=wander", "watch.gif");
    KoLCharacter.setCurrentRun(1);
    KoLAdventure.setNextAdventure("The Deep Machine Tunnels");
    Preferences.setString("enamorangMonster", "Witchess Knight");
    MonsterStatusTracker.setNextMonster(MonsterDatabase.findMonster("Witchess Knight"));
    String html = html("request/test_fight_witchess_knight_in_dmt.html");

    boolean actual = EncounterManager.isEnamorangEncounter(html, true);

    assertThat(actual, equalTo(true));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void noEnamorangEncounterFalsePositive(boolean checkMonster) {
    String html = html("request/test_fight_oil_baron.html");

    boolean actual = EncounterManager.isEnamorangEncounter(html, checkMonster);

    assertThat(actual, equalTo(false));
  }

  @Test
  void isDigitizedEncounterBasedOnResponseText() {
    String html = html("request/test_fight_digitized_monster.html");

    boolean actual = EncounterManager.isDigitizedEncounter(html, false);

    assertThat(actual, equalTo(true));
  }

  @Test
  void isDigitizedEncounterBasedOnMonster() {
    KoLCharacter.setCurrentRun(0);
    TurnCounter.startCounting(1, "Digitize Monster loc=* type=wander", "watch.gif");
    KoLCharacter.setCurrentRun(1);
    KoLAdventure.setNextAdventure("The Deep Machine Tunnels");
    Preferences.setString("_sourceTerminalDigitizeMonster", "Witchess Knight");
    MonsterStatusTracker.setNextMonster(MonsterDatabase.findMonster("Witchess Knight"));
    String html = html("request/test_fight_witchess_knight_in_dmt.html");

    boolean actual = EncounterManager.isDigitizedEncounter(html, true);

    assertThat(actual, equalTo(true));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void noDigitizedEncounterFalsePositive(boolean checkMonster) {
    String html = html("request/test_fight_oil_baron.html");

    boolean actual = EncounterManager.isDigitizedEncounter(html, checkMonster);

    assertThat(actual, equalTo(false));
  }

  @Test
  void isSaberForceMonsterBasedOnMonster() {
    Preferences.setInteger("_saberForceMonsterCount", 3);
    Preferences.setString("_saberForceMonster", "oil slick");

    boolean actual = EncounterManager.isSaberForceMonster("oil slick");

    assertThat(actual, equalTo(true));
  }

  @Test
  void isNotSaberForceMonsterIfNoMonstersLeft() {
    Preferences.setInteger("_saberForceMonsterCount", 0);
    Preferences.setString("_saberForceMonster", "oil slick");

    boolean actual = EncounterManager.isSaberForceMonster("oil slick");

    assertThat(actual, equalTo(false));
  }

  @Test
  void isSaberForceMonsterBasedOnMonsterAndZone() {
    Preferences.setInteger("_saberForceMonsterCount", 3);
    Preferences.setString("_saberForceMonster", "oil slick");

    boolean actual = EncounterManager.isSaberForceMonster("oil slick", "Oil Peak");

    assertThat(actual, equalTo(true));
  }

  @Test
  void isNotSaberForceMonsterBasedOnMonsterAndWrongZone() {
    Preferences.setInteger("_saberForceMonsterCount", 3);
    Preferences.setString("_saberForceMonster", "oil slick");

    boolean actual = EncounterManager.isSaberForceMonster("oil slick", "A-Boo Peak");

    assertThat(actual, equalTo(false));
  }

  @ParameterizedTest
  @CsvSource({"Demoninja, true", "generic duck, false"})
  void isSaberForceZone(String monsterName, String expected) {
    boolean actual = EncounterManager.isSaberForceZone(monsterName, "Pandamonium Slums");

    assertThat(actual, equalTo(Boolean.parseBoolean(expected)));
  }

  @ParameterizedTest
  @CsvSource({"Demoninja, true", "generic duck, false"})
  void isSaberForceZoneFromProperty(String monsterName, String expected) {
    Preferences.setString("_saberForceMonster", monsterName);

    boolean actual = EncounterManager.isSaberForceZone("Pandamonium Slums");

    assertThat(actual, equalTo(Boolean.parseBoolean(expected)));
  }

  @ParameterizedTest
  @CsvSource({"Demoninja, true", "generic duck, false"})
  void isSaberForceMonsterFromName(String monsterName, String expected) {
    Preferences.setInteger("_saberForceMonsterCount", 3);
    Preferences.setString("_saberForceMonster", monsterName);

    boolean actual = EncounterManager.isSaberForceMonster(monsterName, "Pandamonium Slums");

    assertThat(actual, equalTo(Boolean.parseBoolean(expected)));
  }

  @ParameterizedTest
  @CsvSource({"Demoninja, true", "generic duck, false"})
  void isSaberForceMonsterFromData(String monsterName, String expected) {
    Preferences.setInteger("_saberForceMonsterCount", 3);
    Preferences.setString("_saberForceMonster", monsterName);

    var monster = MonsterDatabase.findMonster(monsterName);
    boolean actual = EncounterManager.isSaberForceMonster(monster, "Pandamonium Slums");

    assertThat(actual, equalTo(Boolean.parseBoolean(expected)));
  }

  @Test
  void isSaberForceMonsterFromNextMonster() {
    Preferences.setInteger("_saberForceMonsterCount", 3);
    Preferences.setString("_saberForceMonster", "Demoninja");
    var monster = MonsterDatabase.findMonster("Demoninja");
    MonsterStatusTracker.setNextMonster(monster);

    boolean actual = EncounterManager.isSaberForceMonster();

    assertThat(actual, equalTo(true));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void isRelativityMonster(boolean relativityMonster) {
    Preferences.setBoolean("_relativityMonster", relativityMonster);

    boolean actual = EncounterManager.isRelativityMonster();

    assertThat(actual, equalTo(relativityMonster));
    assertThat("_relativityMonster", isSetTo(false));
  }

  @ParameterizedTest
  @CsvSource({"gregarious_monster, true", "oil_slick, false"})
  void isGregariousEncounter(String file, String expected) {
    String html = html("request/test_fight_" + file + ".html");

    boolean actual = EncounterManager.isGregariousEncounter(html);

    assertThat(actual, equalTo(Boolean.parseBoolean(expected)));
  }

  @ParameterizedTest
  @CsvSource({"The Mariachi With No Name, true", "tiny barrel mimic, false"})
  void isWanderingMonster(String monsterName, String expected) {
    boolean actual = EncounterManager.isWanderingMonster(monsterName);

    assertThat(actual, equalTo(Boolean.parseBoolean(expected)));
  }

  @ParameterizedTest
  @CsvSource({"C. H. U. M. chieftain, true", "C. H. U. M., false"})
  void isLuckyMonster(String monsterName, String expected) {
    boolean actual = EncounterManager.isLuckyMonster(monsterName);

    assertThat(actual, equalTo(Boolean.parseBoolean(expected)));
  }

  @ParameterizedTest
  @CsvSource({"modern zmobie, true", "zmobie, false"})
  void isSuperlikelyMonster(String monsterName, String expected) {
    boolean actual = EncounterManager.isSuperlikelyMonster(monsterName);

    assertThat(actual, equalTo(Boolean.parseBoolean(expected)));
  }

  @ParameterizedTest
  @CsvSource({"giant rubber spider, true", "giant squid., false"})
  void isFreeCombatMonster(String monsterName, String expected) {
    boolean actual = EncounterManager.isFreeCombatMonster(monsterName);

    assertThat(actual, equalTo(Boolean.parseBoolean(expected)));
  }

  @ParameterizedTest
  @CsvSource({"Knott Slanding, true", "Knott Yeti, false"})
  void isUltrarareMonster(String monsterName, String expected) {
    boolean actual = EncounterManager.isUltrarareMonster(monsterName);

    assertThat(actual, equalTo(Boolean.parseBoolean(expected)));
  }

  @ParameterizedTest
  @CsvSource({"'Emily Koops, a spooky lime', true", "sabre-toothed lime, false"})
  void isNoWanderMonster(String monsterName, String expected) {
    boolean actual = EncounterManager.isNoWanderMonster(monsterName);

    assertThat(actual, equalTo(Boolean.parseBoolean(expected)));
  }

  @Test
  void ignoreSpecialMonsters() {
    assertThat(EncounterManager.ignoreSpecialMonsters, equalTo(false));

    EncounterManager.ignoreSpecialMonsters();

    assertThat(EncounterManager.ignoreSpecialMonsters, equalTo(true));
  }

  @Test
  void canRegisterNewEncounter() {
    String html = html("request/test_fight_oil_slick.html");
    EncounterManager.registerEncounter("oil slick", "Combat", html);

    assertThat(KoLConstants.encounterList, hasSize(1));
    assertThat(KoLConstants.encounterList.get(0).toString(), equalTo("Combat: oil slick (1)"));
  }

  @Test
  void canRegisterRepeatEncounter() {
    String html = html("request/test_fight_oil_slick.html");
    EncounterManager.registerEncounter("oil slick", "Combat", html);
    EncounterManager.registerEncounter("oil slick", "Combat", html);

    assertThat(KoLConstants.encounterList, hasSize(1));
    assertThat(KoLConstants.encounterList, contains(hasToString(equalTo("Combat: oil slick (2)"))));
  }

  @Test
  void canAddDifferentEncounterToList() {
    String oilSlickHtml = html("request/test_fight_oil_slick.html");
    EncounterManager.registerEncounter("oil slick", "Combat", oilSlickHtml);
    String oilCartelHtml = html("request/test_fight_oil_slick.html");
    EncounterManager.registerEncounter("oil cartel", "Combat", oilCartelHtml);

    assertThat(KoLConstants.encounterList, hasSize(2));
    assertThat(
        KoLConstants.encounterList,
        contains(
            hasToString(equalTo("Combat: oil cartel (1)")),
            hasToString(equalTo("Combat: oil slick (1)"))));
  }

  @Test
  void canAddEarlierEncounterToList() {
    String oilSlickHtml = html("request/test_fight_oil_slick.html");
    String oilCartelHtml = html("request/test_fight_oil_slick.html");

    EncounterManager.registerEncounter("oil slick", "Combat", oilSlickHtml);
    EncounterManager.registerEncounter("oil cartel", "Combat", oilCartelHtml);
    EncounterManager.registerEncounter("oil slick", "Combat", oilSlickHtml);

    assertThat(KoLConstants.encounterList, hasSize(2));
    assertThat(
        KoLConstants.encounterList,
        contains(
            hasToString(equalTo("Combat: oil cartel (1)")),
            hasToString(equalTo("Combat: oil slick (2)"))));
  }

  @Test
  void canRecognizeSpecialEncounter() {
    var mocked = mockStatic(GoalManager.class, Mockito.CALLS_REAL_METHODS);
    try (mocked) {
      EncounterManager.registerEncounter("Drawn Onward", "Combat", "");

      assertThat(EncounterManager.ignoreSpecialMonsters, equalTo(false));
      mocked.verify(() -> GoalManager.checkAutoStop(anyString()));
    }
  }

  @Test
  void rainManMonsterIgnoresSpecialMonsters() {
    String html = html("request/test_fight_rainman_monster.html");

    var cleanups = withPath(AscensionPath.Path.HEAVY_RAINS);

    try (cleanups) {
      EncounterManager.registerEncounter("Knob Goblin Embezzler", "Combat", html);

      assertThat(EncounterManager.ignoreSpecialMonsters, equalTo(true));
    }
  }

  @Test
  void relativityMonsterIgnoresSpecialMonsters() {
    Preferences.setBoolean("_relativityMonster", true);

    EncounterManager.registerEncounter("oil slick", "Combat", "");

    assertThat(EncounterManager.ignoreSpecialMonsters, equalTo(true));
  }

  @Test
  void nonEncounterTypeDoesNotTrackGoal() {
    var mocked = mockStatic(GoalManager.class, Mockito.CALLS_REAL_METHODS);
    try (mocked) {
      EncounterManager.registerEncounter("oil slick", "Combat", "");
      mocked.verifyNoInteractions();
    }
  }

  @Test
  void bugbearTypeRecognized() {
    var goalManager = mockStatic(GoalManager.class, Mockito.CALLS_REAL_METHODS);
    var bugbearManager = mockStatic(BugbearManager.class, Mockito.CALLS_REAL_METHODS);
    try (goalManager;
        bugbearManager) {
      EncounterManager.registerEncounter("Out of N-Space", "Noncombat", "");
      bugbearManager.verify(() -> BugbearManager.registerEncounter(any(), eq("")));
      goalManager.verifyNoInteractions();
    }
  }

  @Test
  void badMoonTypeRecognized() {
    var goalManager = mockStatic(GoalManager.class, Mockito.CALLS_REAL_METHODS);
    var badMoonManager = mockStatic(BadMoonManager.class, Mockito.CALLS_REAL_METHODS);
    var cleanups =
        new Cleanups(withPath(AscensionPath.Path.BAD_MOON), withSign(ZodiacSign.BAD_MOON));
    try (goalManager;
        badMoonManager;
        cleanups) {
      EncounterManager.registerEncounter("How Far Down Do You Want To Go?", "Noncombat", "");
      badMoonManager.verify(
          () -> BadMoonManager.registerAdventure("How Far Down Do You Want To Go?"));
      goalManager.verify(() -> GoalManager.checkAutoStop(anyString()));
    }
  }

  @Test
  void teleportisisTypeDoesNotTrackGoal() {
    var mocked = mockStatic(GoalManager.class, Mockito.CALLS_REAL_METHODS);
    var cleanups = withEffect("Teleportitis");
    try (mocked;
        cleanups) {
      EncounterManager.registerEncounter("Drawn Onward", "Combat", "");
      mocked.verifyNoInteractions();
    }
  }

  @ParameterizedTest
  @EnumSource(
      value = Slot.class,
      names = {"ACCESSORY1", "ACCESSORY2", "ACCESSORY3"})
  void ringOfTeleportationTypeDoesNotTrackGoal(Slot slot) {
    var mocked = mockStatic(GoalManager.class, Mockito.CALLS_REAL_METHODS);
    var cleanups = withEquipped(slot, "ring of teleportation");
    try (mocked;
        cleanups) {
      EncounterManager.registerEncounter("Drawn Onward", "Combat", "");
      mocked.verifyNoInteractions();
    }
  }

  @Test
  void handlesCapmCaronchEncounter() {
    var cleanups =
        new Cleanups(withItem(ItemPool.CARONCH_DENTURES), withItem(ItemPool.FRATHOUSE_BLUEPRINTS));

    try (cleanups) {
      EncounterManager.registerEncounter(
          "Step Up to the Table, Put the Ball in Play", "Noncombat", "");

      assertThat(InventoryManager.getCount(ItemPool.CARONCH_DENTURES), equalTo(0));
      assertThat(InventoryManager.getCount(ItemPool.FRATHOUSE_BLUEPRINTS), equalTo(0));
      assertThat(QuestDatabase.Quest.PIRATE, isStep(4));
    }
  }

  @Test
  void handlesGrandmaSeaMonkeyUnlockEncounter() {
    var cleanups = withItem(ItemPool.GRANDMAS_MAP);

    try (cleanups) {
      EncounterManager.registerEncounter("Granny, Does Your Dogfish Bite?", "Noncombat", "");

      assertThat(InventoryManager.getCount(ItemPool.GRANDMAS_MAP), equalTo(0));
    }
  }

  @Test
  void handlesTreasuryEliteMeatEncounter() {
    assertThat("_treasuryEliteMeatCollected", isSetTo(false));

    EncounterManager.registerEncounter("Meat For Nothing and the Harem for Free", "Noncombat", "");

    assertThat("_treasuryEliteMeatCollected", isSetTo(true));
  }

  @Test
  void handlesHaremEliteMeatEncounter() {
    assertThat("_treasuryHaremMeatCollected", isSetTo(false));

    EncounterManager.registerEncounter("Finally, the Payoff", "Noncombat", "");

    assertThat("_treasuryHaremMeatCollected", isSetTo(true));
  }

  @Test
  void handlesABooPeakInitialEncounter() {
    assertThat("booPeakProgress", isSetTo(100));

    EncounterManager.registerEncounter("Faction Traction = Inaction", "Noncombat", "");

    assertThat("booPeakProgress", isSetTo(98));
  }

  @Test
  void handlesDailyDungeonCompletedEncounter() {
    assertThat("dailyDungeonDone", isSetTo(false));
    assertThat("_lastDailyDungeonRoom", isSetTo(0));

    EncounterManager.registerEncounter("Daily Done, John.", "Noncombat", "");

    assertThat("dailyDungeonDone", isSetTo(true));
    assertThat("_lastDailyDungeonRoom", isSetTo(15));
  }
}
