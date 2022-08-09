package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withAnapest;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withNextMonster;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import internal.helpers.RequestLoggerOutput;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.GreyYouManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.LocketManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Coverage driven collection of tests for FightRequest. */
public class FightRequestTest {
  private final FightRequest fr = FightRequest.INSTANCE;

  @BeforeEach
  public void beforeEach() {
    GenericRequest.passwordHash = "";
    Preferences.saveSettingsToFile = false;
    KoLCharacter.reset("FightRequestTest");
    Preferences.reset("FightRequestTest");
    FightRequest.clearInstanceData();
    KoLConstants.availableCombatSkillsList.clear();
    KoLConstants.availableCombatSkillsSet.clear();
    KoLCharacter.setFamiliar(FamiliarData.NO_FAMILIAR);
  }

  private void parseCombatData(String path, String location, String encounter) {
    String html = html(path);
    FightRequest.updateCombatData(location, encounter, html);
  }

  private void parseCombatData(String path, String location) {
    parseCombatData(path, location, null);
  }

  private void parseCombatData(String path) {
    parseCombatData(path, null);
  }

  @Test
  public void theProvidedInstanceShouldExistAndReturnSomeThings() {
    assertNotNull(fr);
    // This does nothing but add to coverage
    FightRequest.initialize();
    assertTrue(fr.retryOnTimeout());
    assertTrue(fr.shouldFollowRedirect());
    assertFalse(FightRequest.canStillSteal());
  }

  @Test
  public void itShouldHaveAName() {
    String name = fr.toString();
    assertEquals(name, "fight.php");
  }

  @Test
  public void aSpecialMonsterShouldHaveExpectedCategory() {
    FightRequest.SpecialMonster beeOne = FightRequest.specialMonsterCategory("Queen Bee");
    MonsterData mdb = MonsterDatabase.findMonster("Queen Bee");
    FightRequest.SpecialMonster beeTwo = FightRequest.specialMonsterCategory(mdb);
    assertEquals(beeOne, beeTwo);
  }

  @Test
  public void itShouldReportDreadKisses() {
    FightRequest.resetKisses();
    assertEquals(FightRequest.dreadKisses("Woods"), 1);
    assertEquals(FightRequest.dreadKisses("Village"), 1);
    assertEquals(FightRequest.dreadKisses("Castle"), 1);
    assertEquals(FightRequest.dreadKisses("None of the above"), 0);
  }

  @Test
  public void fakeResponseTextShouldTriggerCodePaths() {
    // Not a fight
    FightRequest.updateCombatData(null, AdventureRequest.NOT_IN_A_FIGHT, null);
    assertFalse(FightRequest.inMultiFight);
    // Twiddle
    FightRequest.updateCombatData(null, null, "You twiddle your thumbs.");
  }

  // Commerce Ghost Tests
  @Test
  public void commerceGhostStartsAtProperValue() {
    FamiliarData fam = new FamiliarData(FamiliarPool.GHOST_COMMERCE);
    KoLCharacter.setFamiliar(fam);
    assertEquals(0, Preferences.getInteger("commerceGhostCombats"));
  }

  @Test
  public void commerceGhostIncrementsByOneOnFight() {
    FamiliarData fam = new FamiliarData(FamiliarPool.GHOST_COMMERCE);
    KoLCharacter.setFamiliar(fam);
    assertEquals(0, Preferences.getInteger("commerceGhostCombats"));
    FightRequest.currentRound = 0;
    parseCombatData("request/test_fight_gnome_adv.html");
    assertEquals(1, Preferences.getInteger("commerceGhostCombats"));
  }

  // If mafia has miscounted we should move our count
  @Test
  @Disabled("Response text does not trigger the code that detects action by ghost.")
  public void commerceGhostResetsTo10() {
    FamiliarData fam = new FamiliarData(FamiliarPool.GHOST_COMMERCE);
    KoLCharacter.setFamiliar(fam);
    Preferences.setInteger("commerceGhostCombats", 5);
    FightRequest.updateCombatData(
        null,
        null,
        "<td style=\"color: white;\" align=center bgcolor=blue><b>Combat!</b></td></tr><tr><tdstyle=\"padding: 5px; border: 1px solid blue;\"><center><table><tr><td> Don't forget to buy a foo!");
    assertEquals(10, Preferences.getInteger("commerceGhostCombats"));
  }

  // When we turn in the quest we should reset
  @Test
  @Disabled("Response text does not trigger the code that detects action by ghost.")
  public void commerceGhostResetsTo0() {
    FamiliarData fam = new FamiliarData(FamiliarPool.GHOST_COMMERCE);
    KoLCharacter.setFamiliar(fam);
    Preferences.setInteger("commerceGhostCombats", 10);
    FightRequest.updateCombatData(null, null, "Nice, you bought a foo!");
    assertEquals(0, Preferences.getInteger("commerceGhostCombats"));
  }

  @Test
  public void gnomeAdv() {
    var familiar = FamiliarData.registerFamiliar(FamiliarPool.REAGNIMATED_GNOME, 1);
    KoLCharacter.setFamiliar(familiar);
    EquipmentManager.setEquipment(EquipmentManager.FAMILIAR, ItemPool.get(ItemPool.GNOMISH_KNEE));

    assertEquals(0, Preferences.getInteger("_gnomeAdv"));
    parseCombatData("request/test_fight_gnome_adv.html");
    assertEquals(1, Preferences.getInteger("_gnomeAdv"));
  }

  @Test
  public void mafiaThumbRingAdvs() {
    assertEquals(0, Preferences.getInteger("_mafiaThumbRingAdvs"));
    parseCombatData("request/test_fight_mafia_thumb_ring.html");
    assertEquals(1, Preferences.getInteger("_mafiaThumbRingAdvs"));

    // Regression test for thumb ring adventures being picked up as Reagnimated Gnome adventures
    assertEquals(0, Preferences.getInteger("_gnomeAdv"));
  }

  @Test
  public void crystalBallPredictions() {
    assertEquals("", Preferences.getString("crystalBallPredictions"));

    KoLAdventure.setLastAdventure(AdventureDatabase.getAdventure("The Neverending Party"));
    parseCombatData("request/test_fight_crystal_ball_neverending_party.html");
    FightRequest.clearInstanceData();
    // Parsing isn't initiated without the crystal ball
    assertEquals("", Preferences.getString("crystalBallPredictions"));

    // NOW equip the crystal ball
    FamiliarData familiar = FamiliarData.registerFamiliar(FamiliarPool.MOSQUITO, 1);
    KoLCharacter.setFamiliar(familiar);
    EquipmentManager.setEquipment(
        EquipmentManager.FAMILIAR, ItemPool.get(ItemPool.MINIATURE_CRYSTAL_BALL));

    parseCombatData("request/test_fight_crystal_ball_neverending_party.html");
    FightRequest.clearInstanceData();
    assertEquals(
        "0:The Neverending Party:party girl", Preferences.getString("crystalBallPredictions"));

    KoLAdventure.setLastAdventure(AdventureDatabase.getAdventure("The Red Zeppelin"));
    parseCombatData("request/test_fight_crystal_ball_zeppelin.html");
    FightRequest.clearInstanceData();
    assertEquals(
        "0:The Neverending Party:party girl|0:The Red Zeppelin:Red Snapper",
        Preferences.getString("crystalBallPredictions"));
  }

  @Test
  public void voidMonsterIncrementationTest() {
    MonsterStatusTracker.setNextMonster(MonsterDatabase.findMonster("void slab"));
    parseCombatData("request/test_fight_void_monster.html");
    assertEquals(5, Preferences.getInteger("_voidFreeFights"));
  }

  @Test
  public void cursedMagnifyingGlassTest() {
    EquipmentManager.setEquipment(
        EquipmentManager.OFFHAND, ItemPool.get(ItemPool.CURSED_MAGNIFYING_GLASS));
    Preferences.setInteger("cursedMagnifyingGlassCount", 13);
    MonsterStatusTracker.setNextMonster(MonsterDatabase.findMonster("void slab"));
    parseCombatData("request/test_fight_void_monster.html");
    assertEquals(0, Preferences.getInteger("cursedMagnifyingGlassCount"));

    MonsterStatusTracker.setNextMonster(MonsterDatabase.findMonster("lavatory"));
    parseCombatData("request/test_fight_cursed_magnifying_glass_update.html");
    assertEquals(3, Preferences.getInteger("cursedMagnifyingGlassCount"));
  }

  @Test
  public void daylightShavingTest() {
    EquipmentManager.setEquipment(
        EquipmentManager.HAT, ItemPool.get(ItemPool.DAYLIGHT_SHAVINGS_HELMET));
    parseCombatData("request/test_fight_daylight_shavings_buff.html");
    assertEquals(2671, Preferences.getInteger("lastBeardBuff"));
  }

  @Test
  public void luckyGoldRingVolcoinoDropRecorded() {
    assertFalse(Preferences.getBoolean("_luckyGoldRingVolcoino"));
    parseCombatData("request/test_fight_lucky_gold_ring_volcoino.html");
    assertTrue(Preferences.getBoolean("_luckyGoldRingVolcoino"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"alielf", "Black Crayon Crimbo Elf"})
  public void registersLocketFight(String monsterName) {
    var monster = MonsterDatabase.findMonster(monsterName);
    MonsterStatusTracker.setNextMonster(monster);
    parseCombatData("request/test_fight_start_locket_fight_with_" + monster.getPhylum() + ".html");
    assertThat("locketPhylum", isSetTo(monster.getPhylum().toString()));
    assertThat("_locketMonstersFought", isSetTo(monster.getId()));
  }

  @Test
  public void rememberNewMonsterForLocket() {
    assertFalse(LocketManager.remembersMonster(1568));

    MonsterStatusTracker.setNextMonster(MonsterDatabase.findMonster("Sloppy Seconds Sundae"));
    parseCombatData("request/test_fight_monster_added_to_locket.html");

    assertTrue(LocketManager.remembersMonster(1568));
  }

  @Test
  public void updatesListIfMonsterWasAlreadyInLocket() {
    assertFalse(LocketManager.remembersMonster(155));

    MonsterStatusTracker.setNextMonster(MonsterDatabase.findMonster("Knob Goblin Barbecue Team"));
    parseCombatData("request/test_fight_monster_already_in_locket.html");

    assertTrue(LocketManager.remembersMonster(155));
  }

  @Test
  public void dontIncrementWitchessIfFromLocket() {
    assertEquals(Preferences.getInteger("_witchessFights"), 0);

    MonsterStatusTracker.setNextMonster(MonsterDatabase.findMonster("Witchess Knight"));
    parseCombatData("request/test_fight_witchess_with_locket.html");

    assertEquals(Preferences.getInteger("_witchessFights"), 0);
  }

  // Grey Goose Tests
  @Test
  public void shouldWorkAroundGreyGooseKoLBug() {
    String html = html("request/test_fight_grey_goose_combat_skills.html");
    FightRequest.currentRound = 1;
    FamiliarData fam = new FamiliarData(FamiliarPool.GREY_GOOSE);
    KoLCharacter.setFamiliar(fam);
    KoLCharacter.getFamiliar().setWeight(6);
    // If we have a 6-lb. Grey Goose, the Grey Goose combat skills on the fight
    // page are valid.
    FightRequest.parseAvailableCombatSkills(html);
    assertTrue(KoLCharacter.hasCombatSkill(SkillPool.EMIT_MATTER_DUPLICATING_DRONES));
    // If it is less than 6-lbs., a KoL bug still shows them on the combat page,
    // but if you try to use them, "You don't know that skill."
    KoLCharacter.getFamiliar().setWeight(1);
    FightRequest.parseAvailableCombatSkills(html);
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.EMIT_MATTER_DUPLICATING_DRONES));
  }

  @Test
  public void canTrackMeatifyMatterCast() {
    String html = html("request/test_fight_meatify_matter.html");
    FamiliarData fam = new FamiliarData(FamiliarPool.GREY_GOOSE);
    KoLCharacter.setFamiliar(fam);
    KoLCharacter.getFamiliar().setWeight(6);
    FightRequest.currentRound = 1;
    MonsterStatusTracker.setNextMonster(MonsterDatabase.findMonster("angry tourist"));
    FightRequest.registerRequest(true, "fight.php?action=skill&whichskill=7409");
    FightRequest.updateCombatData(null, null, html);
    assertEquals(5, KoLCharacter.getFamiliar().getWeight());
    assertTrue(Preferences.getBoolean("_meatifyMatterUsed"));
    assertEquals(0, SkillDatabase.getMaxCasts(SkillPool.MEATIFY_MATTER));
  }

  // X bits of goo emerge from <name> and begin hovering about, moving probingly around various
  // objects.
  // One of the matter duplicating drones seems to coalesce around the <item> and then transforms
  // into an exact replica. <X-1> more drones are still circling around.
  // One of the matter duplicating drones seems to coalesce around the <item> and then transforms
  // into an exact replica. That was the last drone.

  @Test
  public void canTrackGooseDrones() {
    FamiliarData fam = new FamiliarData(FamiliarPool.GREY_GOOSE);
    KoLCharacter.setFamiliar(fam);
    KoLCharacter.getFamiliar().setWeight(6);

    assertEquals(0, Preferences.getInteger("gooseDronesRemaining"));

    String html = html("request/test_fight_goose_drones_1.html");
    MonsterStatusTracker.setNextMonster(MonsterDatabase.findMonster("angry tourist"));
    FightRequest.registerRequest(true, "fight.php?action=skill&whichskill=7410");
    FightRequest.currentRound = 1;
    FightRequest.updateCombatData(null, null, html);
    assertEquals(6, Preferences.getInteger("gooseDronesRemaining"));

    html = html("request/test_fight_goose_drones_2.html");
    FightRequest.registerRequest(true, "fight.php?action=attack");
    FightRequest.currentRound = 2;
    FightRequest.updateCombatData(null, null, html);
    assertEquals(5, Preferences.getInteger("gooseDronesRemaining"));

    html = html("request/test_fight_goose_drones_3.html");
    FightRequest.registerRequest(true, "fight.php?action=skill&whichskill=7410");
    FightRequest.currentRound = 1;
    FightRequest.updateCombatData(null, null, html);
    assertEquals(9, Preferences.getInteger("gooseDronesRemaining"));

    html = html("request/test_fight_goose_drones_4.html");
    FightRequest.registerRequest(true, "fight.php?action=attack");
    FightRequest.currentRound = 2;
    FightRequest.updateCombatData(null, null, html);
    assertEquals(7, Preferences.getInteger("gooseDronesRemaining"));

    html = html("request/test_fight_goose_drones_5.html");
    FightRequest.registerRequest(true, "fight.php?action=attack");
    FightRequest.currentRound = 1;
    FightRequest.updateCombatData(null, null, html);
    assertEquals(6, Preferences.getInteger("gooseDronesRemaining"));

    html = html("request/test_fight_goose_drones_6.html");
    FightRequest.registerRequest(true, "fight.php?action=attack");
    FightRequest.currentRound = 1;
    FightRequest.updateCombatData(null, null, html);
    assertEquals(4, Preferences.getInteger("gooseDronesRemaining"));

    html = html("request/test_fight_goose_drones_7.html");
    FightRequest.registerRequest(true, "fight.php?action=attack");
    FightRequest.currentRound = 1;
    FightRequest.updateCombatData(null, null, html);
    assertEquals(3, Preferences.getInteger("gooseDronesRemaining"));

    html = html("request/test_fight_goose_drones_8.html");
    FightRequest.registerRequest(true, "fight.php?action=attack");
    FightRequest.currentRound = 1;
    FightRequest.updateCombatData(null, null, html);
    assertEquals(2, Preferences.getInteger("gooseDronesRemaining"));

    html = html("request/test_fight_goose_drones_9.html");
    FightRequest.registerRequest(true, "fight.php?action=attack");
    FightRequest.currentRound = 1;
    FightRequest.updateCombatData(null, null, html);
    assertEquals(1, Preferences.getInteger("gooseDronesRemaining"));

    html = html("request/test_fight_goose_drones_10.html");
    FightRequest.registerRequest(true, "fight.php?action=attack");
    FightRequest.currentRound = 1;
    FightRequest.updateCombatData(null, null, html);
    assertEquals(0, Preferences.getInteger("gooseDronesRemaining"));
  }

  @Test
  public void canTrackCastAndUseGooseDrones() {
    FamiliarData fam = new FamiliarData(FamiliarPool.GREY_GOOSE);
    KoLCharacter.setFamiliar(fam);
    KoLCharacter.getFamiliar().setWeight(6);

    String html = html("request/test_fight_cast_and_use_drones.html");
    MonsterStatusTracker.setNextMonster(MonsterDatabase.findMonster("Witchess Knight"));
    // Multi-round response text. Matter_Duplicating drones emitted one round and used-up next round
    assertEquals(0, Preferences.getInteger("gooseDronesRemaining"));
    FightRequest.registerRequest(
        true,
        "fight.php?action=macro&macrotext=if+hasskill+curse+of+weaksauce%3Bskill+curse+of+weaksauce%3Bendif%3Bif+hascombatitem+porquoise-handled+sixgun+%26%26+hascombatitem+mayor+ghost%3Buse+porquoise-handled+sixgun%2Cmayor+ghost%3Bendif%3Bif+hasskill+bowl+straight+up%3Bskill+bowl+straight+up%3Bendif%3Bif+hascombatitem+spooky+putty+sheet%3Buse+spooky+putty+sheet%3Bendif%3Bif+hasskill+emit+matter+duplicating+drones%3Bskill+emit+matter+duplicating+drones%3Bendif%3Battack%3Brepeat%3Babort%3B");
    FightRequest.currentRound = 1;
    FightRequest.updateCombatData(null, null, html);
    assertEquals(0, Preferences.getInteger("gooseDronesRemaining"));
  }

  @Test
  public void canFindItemsAfterSlayTheDead() {
    FamiliarData fam = new FamiliarData(FamiliarPool.GREY_GOOSE);
    KoLCharacter.setFamiliar(fam);

    KoLConstants.inventory.clear();

    String html = html("request/test_fight_slay_the_dead.html");
    String url =
        "fight.php?action=macro&macrotext=abort+hppercentbelow+20%3B+abort+pastround+25%3B+skill+Slay+the+Dead%3B+use+beehive%3B+skill+Double+Nanovision%3B+repeat%3B+mark+eof%3B+";
    MonsterStatusTracker.setNextMonster(MonsterDatabase.findMonster("toothy sklelton"));
    FightRequest.registerRequest(true, url);
    FightRequest.currentRound = 1;
    FightRequest.updateCombatData(null, null, html);
    assertEquals(1, InventoryManager.getCount(ItemPool.LOOSE_TEETH));
    assertEquals(1, InventoryManager.getCount(ItemPool.SKELETON_BONE));
    assertEquals(1, InventoryManager.getCount(ItemPool.BONE_FLUTE));
    assertEquals(1, InventoryManager.getCount(ItemPool.EVIL_EYE));
  }

  @Test
  public void canFindItemsWithGravyBoat() {
    KoLConstants.inventory.clear();

    String html = html("request/test_fight_gravy_boat_1.html");
    String url = "fight.php?action=skill&whichskill=27043";
    MonsterStatusTracker.setNextMonster(MonsterDatabase.findMonster("spiny skelelton"));
    FightRequest.registerRequest(true, url);
    FightRequest.currentRound = 2;
    // This html has hewn moon-rune spoon munging. processResults will un-munge
    // it before calling updateCombatData.
    FightRequest.processResults(null, null, html);
    assertEquals(1, InventoryManager.getCount(ItemPool.SKELETON_BONE));
    assertEquals(1, InventoryManager.getCount(ItemPool.SMART_SKULL));
    assertEquals(1, InventoryManager.getCount(ItemPool.EVIL_EYE));
    assertEquals(1, InventoryManager.getCount(ItemPool.BOTTLE_OF_GIN));
  }

  @Test
  public void canFindItemsWithGravyBoatAndSlayTheDead() {
    KoLConstants.inventory.clear();

    String html = html("request/test_fight_gravy_boat_2.html");
    String url = "fight.php?action=skill&whichskill=7348";
    MonsterStatusTracker.setNextMonster(MonsterDatabase.findMonster("toothy sklelton"));
    FightRequest.registerRequest(true, url);
    FightRequest.currentRound = 2;
    // This html has hewn moon-rune spoon munging. processResults will un-munge
    // it before calling updateCombatData.
    FightRequest.processResults(null, null, html);
    assertEquals(1, InventoryManager.getCount(ItemPool.LOOSE_TEETH));
    assertEquals(1, InventoryManager.getCount(ItemPool.EVIL_EYE));
  }

  @Test
  public void canTrackDramederyActions() {
    FamiliarData fam =
        new FamiliarData(FamiliarPool.MELODRAMEDARY, "Gogarth", 1, EquipmentRequest.UNEQUIP);
    KoLCharacter.setFamiliar(fam);

    String html = html("request/test_fight_drama_drones_1.html");
    FightRequest.registerRequest(true, "fight.php?action=attack");
    FightRequest.currentRound = 1;
    Preferences.setInteger("camelSpit", 60);
    FightRequest.updateCombatData(null, null, html);
    assertEquals(63, Preferences.getInteger("camelSpit"));

    html = html("request/test_fight_drama_spit_1.html");
    FightRequest.registerRequest(true, "fight.php?action=skill&whichskill=7340");
    FightRequest.currentRound = 1;
    Preferences.setInteger("camelSpit", 1000);
    FightRequest.updateCombatData(null, null, html);
    assertEquals(0, Preferences.getInteger("camelSpit"));
  }

  @Test
  public void canTrackDronesWithDramedery() {
    FamiliarData fam = new FamiliarData(FamiliarPool.MELODRAMEDARY);
    KoLCharacter.setFamiliar(fam);

    String html = html("request/test_fight_drama_drones_1.html");
    FightRequest.registerRequest(true, "fight.php?action=attack");
    FightRequest.currentRound = 1;
    Preferences.setInteger("gooseDronesRemaining", 2);
    FightRequest.updateCombatData(null, null, html);
    assertEquals(1, Preferences.getInteger("gooseDronesRemaining"));
  }

  @Test
  public void canTrackGreyYouAbsorptions() {
    FamiliarData fam = new FamiliarData(FamiliarPool.GREY_GOOSE);
    KoLCharacter.setFamiliar(fam);
    KoLCharacter.getFamiliar().setExperience(36);

    KoLCharacter.setPath(Path.GREY_YOU);
    GreyYouManager.resetAbsorptions();

    // Initial absorption of an albino bat
    String urlString = "fight.php?action=skill&whichskill=27000";
    String html = html("request/test_fight_goo_absorption_1.html");
    MonsterData monster = MonsterDatabase.findMonster("albino bat");
    int monsterId = monster.getId();
    MonsterStatusTracker.setNextMonster(monster);

    FightRequest.currentRound = 2;
    FightRequest.registerRequest(true, urlString);
    FightRequest.updateCombatData(null, null, html);
    assertTrue(GreyYouManager.absorbedMonsters.contains(monsterId));
    assertEquals(6, KoLCharacter.getFamiliar().getWeight());

    GreyYouManager.resetAbsorptions();

    // Subsequent absorption of an albino bat via Re-Process Matter
    urlString = "fight.php?action=skill&whichskill=7408";
    html = html("request/test_fight_goo_absorption_2.html");
    FightRequest.currentRound = 2;
    FightRequest.registerRequest(true, urlString);
    assertEquals("", Preferences.getString("gooseReprocessed"));
    FightRequest.updateCombatData(null, null, html);
    assertTrue(GreyYouManager.absorbedMonsters.contains(monsterId));
    assertEquals(String.valueOf(monsterId), Preferences.getString("gooseReprocessed"));
    assertEquals(1, KoLCharacter.getFamiliar().getWeight());

    GreyYouManager.resetAbsorptions();

    // Absorbing a Passive Skill
    urlString = "fight.php?action=skill&whichskill=27000";
    html = html("request/test_fight_goo_absorption_3.html");
    monster = MonsterDatabase.findMonster("rushing bum");
    monsterId = monster.getId();
    MonsterStatusTracker.setNextMonster(monster);
    FightRequest.currentRound = 2;
    assertFalse(KoLCharacter.hasSkill(SkillPool.HARRIED));
    FightRequest.registerRequest(true, urlString);
    FightRequest.updateCombatData(null, null, html);
    assertTrue(GreyYouManager.absorbedMonsters.contains(monsterId));
    assertTrue(KoLCharacter.hasSkill(SkillPool.HARRIED));

    GreyYouManager.resetAbsorptions();

    // Absorbing a non-special monster
    urlString = "fight.php?action=skill&whichskill=27000";
    html = html("request/test_fight_goo_absorption_4.html");
    monster = MonsterDatabase.findMonster("regular old bat");
    monsterId = monster.getId();
    MonsterStatusTracker.setNextMonster(monster);
    FightRequest.currentRound = 2;
    FightRequest.registerRequest(true, urlString);
    FightRequest.updateCombatData(null, null, html);
    assertFalse(GreyYouManager.absorbedMonsters.contains(monsterId));
  }

  @Test
  public void canReabsorbMultipleMonsters() {
    FamiliarData fam = new FamiliarData(FamiliarPool.GREY_GOOSE);
    KoLCharacter.setFamiliar(fam);
    KoLCharacter.getFamiliar().setExperience(36);

    KoLCharacter.setPath(Path.GREY_YOU);
    GreyYouManager.resetAbsorptions();

    // Absorption of an albino bat via Re-Process Matter
    String urlString = "fight.php?action=skill&whichskill=7408";
    String html = html("request/test_fight_goo_absorption_2.html");
    FightRequest.currentRound = 2;
    FightRequest.registerRequest(true, urlString);
    assertEquals("", Preferences.getString("gooseReprocessed"));
    FightRequest.updateCombatData(null, null, html);
    assertEquals("41", Preferences.getString("gooseReprocessed"));

    // Second absorption of a model skeleton via Re-Process Matter
    KoLCharacter.getFamiliar().setExperience(36);
    urlString = "fight.php?action=skill&whichskill=7408";
    html = html("request/test_fight_goo_absorption_5.html");
    FightRequest.currentRound = 2;
    FightRequest.registerRequest(true, urlString);
    assertEquals("41", Preferences.getString("gooseReprocessed"));
    FightRequest.updateCombatData(null, null, html);
    assertEquals("41,1547", Preferences.getString("gooseReprocessed"));

    // Third absorption of a model skeleton via Re-Process Matter
    KoLCharacter.getFamiliar().setExperience(36);
    urlString = "fight.php?action=skill&whichskill=7408";
    html = html("request/test_fight_goo_absorption_5.html");
    FightRequest.currentRound = 2;
    FightRequest.registerRequest(true, urlString);
    assertEquals("41,1547", Preferences.getString("gooseReprocessed"));
    FightRequest.updateCombatData(null, null, html);
    assertEquals("41,1547", Preferences.getString("gooseReprocessed"));
  }

  @Nested
  class CosmicBowlingBall {
    @ParameterizedTest
    @ValueSource(
        ints = {
          // Off in the distance, you hear your cosmic bowling ball rattling around in the ball
          // return system.
          1,
          // You hear your cosmic bowling ball rattling around in the ball return system.
          2,
          // You hear your cosmic bowling ball rattling around in the ball return system nearby.
          3,
          // You hear your cosmic bowling ball approaching.
          4,
          // Your cosmic bowling ball clatters into the closest ball return and you grab it.
          5
        })
    public void canTrackCosmicBowlingBall(int step) {
      FightRequest.currentRound = 1;

      String html = html("request/test_fight_bowling_ball_" + step + ".html");
      FightRequest.parseAvailableCombatSkills(html);
      assertThat(KoLCharacter.hasCombatSkill(SkillPool.BOWL_STRAIGHT_UP), equalTo(step == 5));
    }

    @Test
    public void canTrackCosmicBowlingBallBanishInAnapests() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.COSMIC_BOWLING_BALL),
              withAnapest(),
              withNextMonster("Marcus Macurgeon"));

      try (cleanups) {
        String urlString = "fight.php?action=skill&whichskill=7405";
        String html = html("request/test_fight_anapest_runaway.html");
        FightRequest.currentRound = 2;
        FightRequest.registerRequest(true, urlString);
        FightRequest.updateCombatData(null, null, html);

        assertEquals(0, InventoryManager.getCount(ItemPool.COSMIC_BOWLING_BALL));
      }
    }
  }

  // Robort drop tracking preference tests
  @Test
  public void canTrackRoboDrops() {
    FamiliarData fam = new FamiliarData(FamiliarPool.ROBORTENDER);
    KoLCharacter.setFamiliar(fam);

    assertEquals(0, Preferences.getInteger("_roboDrops"));
    parseCombatData("request/test_fight_robort_drops_1.html");
    assertEquals(1, Preferences.getInteger("_roboDrops"));

    Preferences.setInteger("_roboDrops", 0);

    assertEquals(0, Preferences.getInteger("_roboDrops"));
    parseCombatData("request/test_fight_robort_drops_2.html");
    assertEquals(0, Preferences.getInteger("_roboDrops"));
  }

  @Test
  public void canDetectMaydaySupplyPackage() {
    assertFalse(Preferences.getBoolean("_maydayDropped"));
    parseCombatData("request/test_fight_mayday_contract.html");
    assertTrue(Preferences.getBoolean("_maydayDropped"));
  }

  @Test
  public void canTrackJuneCleaverPrefs() {
    EquipmentManager.setEquipment(EquipmentManager.WEAPON, ItemPool.get(ItemPool.JUNE_CLEAVER));
    parseCombatData("request/test_fight_june_cleaver.html");
    assertEquals(Preferences.getInteger("_juneCleaverSleaze"), 2);
    assertEquals(Preferences.getInteger("_juneCleaverFightsLeft"), 0);
  }

  @Test
  public void canTrackBellydancerPickpocket() {
    assertEquals(0, Preferences.getInteger("_bellydancerPickpockets"));

    parseCombatData("request/test_fight_bellydancing_pickpocket_1.html");
    assertEquals(1, Preferences.getInteger("_bellydancerPickpockets"));

    parseCombatData("request/test_fight_bellydancing_pickpocket_2.html");
    assertEquals(2, Preferences.getInteger("_bellydancerPickpockets"));

    parseCombatData("request/test_fight_bellydancing_pickpocket_3.html");
    assertEquals(3, Preferences.getInteger("_bellydancerPickpockets"));
  }

  @ParameterizedTest
  @CsvSource({
    "request/test_fight_designer_sweatpants_gain_2_sweat.html, 2",
    "request/test_fight_designer_sweatpants_lose_3_sweat.html, -3"
  })
  public void canTrackDesignerSweatpants(String responseHtml, int sweatChange) {
    var cleanups =
        new Cleanups(
            withEquipped(EquipmentManager.PANTS, "designer sweatpants"), withProperty("sweat", 10));

    try (cleanups) {
      parseCombatData(responseHtml);
      assertEquals(10 + sweatChange, Preferences.getInteger("sweat"));
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 4, 75})
  public void canUpdateSnowSuitUsage(int count) {
    var cleanups =
        new Cleanups(
            withFamiliar(FamiliarPool.CORNBEEFADON),
            withEquipped(EquipmentManager.FAMILIAR, "Snow Suit"),
            withProperty("_snowSuitCount", count));

    try (cleanups) {
      // Calculate initial Familiar Weight Modifier
      KoLCharacter.recalculateAdjustments();
      int property = count;
      int expected = 20 - (property / 5);
      int adjustment = KoLCharacter.getFamiliarWeightAdjustment();
      assertEquals(expected, adjustment);
      FightRequest.updateFinalRoundData("", true);
      // We expect the property to increment, but cap at 75
      property = Math.min(75, property + 1);
      assertEquals(property, Preferences.getInteger("_snowSuitCount"));
      expected = 20 - (property / 5);
      adjustment = KoLCharacter.getFamiliarWeightAdjustment();
      assertEquals(expected, adjustment);
    }
  }

  @Test
  public void canDetectPottedPlantWins() {
    RequestLoggerOutput.startStream();
    var cleanups = new Cleanups(withEquipped(EquipmentManager.OFFHAND, "carnivorous potted plant"));
    try (cleanups) {
      parseCombatData("request/test_fight_potted_plant.html");
      var text = RequestLoggerOutput.stopStream();
      assertThat(text, containsString("Your potted plant swallows your opponent{s} whole."));
    }
  }

  @Test
  public void canDetectCanOfMixedEverythingDrops() {
    RequestLoggerOutput.startStream();
    var cleanups = new Cleanups(withEquipped(EquipmentManager.OFFHAND, "can of mixed everything"));
    try (cleanups) {
      parseCombatData("request/test_fight_can_of_mixed_everything.html");
      var text = RequestLoggerOutput.stopStream();
      assertThat(
          text,
          containsString(
              "Something falls out of your can of mixed everything.\nYou acquire an item: ice-cold Willer"));
    }
  }

  @Nested
  class XOSkeleton {
    @Test
    public void canTrackXandOCounter() {
      var cleanups =
          new Cleanups(
              withProperty("xoSkeleltonXProgress", 8),
              withProperty("xoSkeleltonOProgress", 3),
              withFamiliar(FamiliarPool.XO_SKELETON));

      try (cleanups) {
        String html = html("request/test_fight_xo_end_of_fight.html");
        FightRequest.currentRound = 2;
        FightRequest.updateCombatData(null, null, html);
        assertThat("xoSkeleltonXProgress", isSetTo(0));
        assertThat("xoSkeleltonOProgress", isSetTo(4));
      }
    }

    @Test
    public void canTrackHugsAndKissesSuccess() {
      var cleanups = new Cleanups(withProperty("_xoHugsUsed", 0));

      try (cleanups) {
        String urlString = "fight.php?action=macro&macrotext=skill+7293&whichmacro=0";
        String html = html("request/test_fight_hugs_and_kisses_success.html");
        FightRequest.currentRound = 2;
        FightRequest.registerRequest(true, urlString);
        FightRequest.updateCombatData(null, null, html);
        assertThat("_xoHugsUsed", isSetTo(1));
      }
    }

    @Test
    public void canTrackHugsAndKissesFailure() {
      var cleanups = new Cleanups(withProperty("_xoHugsUsed", 3));

      try (cleanups) {
        String urlString = "fight.php?action=macro&macrotext=skill+7293&whichmacro=0";
        String html = html("request/test_fight_hugs_and_kisses_failure.html");
        FightRequest.currentRound = 2;
        FightRequest.registerRequest(true, urlString);
        FightRequest.updateCombatData(null, null, html);
        assertThat("_xoHugsUsed", isSetTo(3));
      }
    }
  }

  @Nested
  class Vintner {
    @Test
    public void notesIncreaseVintnerCharge() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.VAMPIRE_VINTNER), withProperty("vintnerCharge", 0));
      try (cleanups) {
        parseCombatData("request/test_fight_vintner_makes_notes.html");
        assertThat("vintnerCharge", isSetTo(1));
        assertThat(KoLCharacter.getFamiliar().getCharges(), equalTo(1));
      }
    }

    @Test
    public void wineDropCorrectsVintnerCharge() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.VAMPIRE_VINTNER), withProperty("vintnerCharge", 11));
      try (cleanups) {
        parseCombatData("request/test_fight_vintner_drops_wine.html");
        assertThat("vintnerCharge", isSetTo(13));
        assertThat(KoLCharacter.getFamiliar().getCharges(), equalTo(13));
      }
    }

    @ParameterizedTest
    @ValueSource(strings = {"clears_throat", "gestures", "taps"})
    public void waitingCorrectsVintnerCharge(String dialog) {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.VAMPIRE_VINTNER), withProperty("vintnerCharge", 9));
      try (cleanups) {
        parseCombatData("request/test_fight_vintner_" + dialog + ".html");
        assertThat("vintnerCharge", isSetTo(13));
        assertThat(KoLCharacter.getFamiliar().getCharges(), equalTo(13));
      }
    }
  }

  @Nested
  class SummonHoboUnderling {
    @Test
    public void canTrackSummoningHoboUnderling() {
      var cleanups = new Cleanups(withProperty("_hoboUnderlingSummons", 0));

      try (cleanups) {
        String urlString = "fight.php?action=skill&whichskill=7052";
        String html = html("request/test_fight_summon_hobo_underling.html");
        FightRequest.currentRound = 2;
        FightRequest.registerRequest(true, urlString);
        FightRequest.updateCombatData(null, null, html);
        assertThat("_hoboUnderlingSummons", isSetTo(1));
      }
    }

    @Test
    public void askHoboToDoADance() {
      String urlString = "fight.php?action=skill&whichskill=7051";
      String html = html("request/test_fight_ask_hobo_to_dance.html");
      FightRequest.currentRound = 3;
      FightRequest.registerRequest(true, urlString);
      FightRequest.updateCombatData(null, null, html);
      var fightMods = Modifiers.getModifiers("Generated", "fightMods");
      assertThat(fightMods.get(Modifiers.ITEMDROP), equalTo(100.0));
    }

    @Test
    public void askHoboToTellAJoke() {
      String urlString = "fight.php?action=skill&whichskill=7050";
      String html = html("request/test_fight_ask_hobo_to_joke.html");
      FightRequest.currentRound = 3;
      FightRequest.registerRequest(true, urlString);
      FightRequest.updateCombatData(null, null, html);
      var fightMods = Modifiers.getModifiers("Generated", "fightMods");
      assertThat(fightMods.get(Modifiers.MEATDROP), equalTo(100.0));
    }
  }

  @Nested
  class LoveBugsPreferenceButtonGroupTest {
    @BeforeAll
    private static void beforeAll() {
      Preferences.saveSettingsToFile = false;
    }

    @AfterAll
    private static void afterAll() {
      Preferences.saveSettingsToFile = true;
    }

    @BeforeEach
    private void beforeEach() {
      KoLCharacter.reset("lovebugs");
    }

    @ParameterizedTest
    @CsvSource({
      // Meat or Item Drop
      "request/test_fight_lovebug_grub.html, lovebugsMeatDrop, 1, false",
      "request/test_fight_lovebug_cricket.html, lovebugsItemDrop, 1, false",
      // Stat gain
      "request/test_fight_lovebug_muscle.html, lovebugsMuscle, 5, false",
      "request/test_fight_lovebug_mysticality.html, lovebugsMysticality, 4, false",
      "request/test_fight_lovebug_moxie.html, lovebugsMoxie, 6, false",
      // Meat
      "request/test_fight_lovebug_meat1.html, lovebugsMeat, 81, false",
      "request/test_fight_lovebug_meat2.html, lovebugsMeat, 30, false",
      "request/test_fight_lovebug_meat3.html, lovebugsMeat, 26, false",
      "request/test_fight_lovebug_meat4.html, lovebugsMeat, 29, false",
      // Items
      "request/test_fight_lovebug_booze.html, lovebugsBooze, 1, false",
      "request/test_fight_lovebug_powder.html, lovebugsPowder, 1, false",
      // Quests
      "request/test_fight_lovebug_ant.html, lovebugsOrcChasm, 1, false",
      "request/test_fight_lovebug_desert.html, lovebugsAridDesert, 1, false",
      "request/test_fight_lovebug_oil.html, lovebugsOilPeak, 1, false",
      // Currency
      "request/test_fight_lovebug_beach_buck.html, lovebugsBeachBuck, 1, true",
      "request/test_fight_lovebug_coinspiracy.html, lovebugsCoinspiracy, 1, true",
      "request/test_fight_lovebug_freddy.html, lovebugsFreddy, 1, true",
      "request/test_fight_lovebug_funfunds.html, lovebugsFunFunds, 1, true",
      "request/test_fight_lovebug_hobo_nickel.html, lovebugsHoboNickel, 1, true",
      "request/test_fight_lovebug_walmart.html, lovebugsWalmart, 1, true"
    })
    public void canTrackLoveBugDrops(
        String responseHtml, String property, int delta, boolean daily) {
      var cleanups =
          new Cleanups(withProperty("lovebugsUnlocked", true), withProperty(property, 0));

      try (cleanups) {
        parseCombatData(responseHtml);
        assertEquals(delta, Preferences.getInteger(property));
        if (daily) {
          assertEquals(delta, Preferences.getInteger("_" + property));
        }
      }
    }
  }

  @Nested
  class CombatEnvironment {
    @ParameterizedTest
    @CsvSource({
      "Oil Peak, o",
      "The Haunted Pantry, i",
      "The Middle Chamber, u",
      "The Briny Deeps, x",
      // If they add Gausie's Grotto I promise to come and make up a new location
      "Gausie's Grotto, ?"
    })
    public void canDetectEnvironment(String adventureName, String environmentSymbol) {
      var cleanups = withProperty("lastCombatEnvironments", "xxxxxxxxxxxxxxxxxxxx");
      try (cleanups) {
        KoLAdventure.lastVisitedLocation = AdventureDatabase.getAdventure(adventureName);
        // Any old non-free fight from our fixtures
        parseCombatData("request/test_fight_oil_slick.html");
        assertThat("lastCombatEnvironments", isSetTo("xxxxxxxxxxxxxxxxxxx" + environmentSymbol));
      }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "xxxxx", "xxxxxxxxxxxxxxxxxxx"})
    public void canRecoverUndersizedProp(String pref) {
      var cleanups = withProperty("lastCombatEnvironments", pref);
      try (cleanups) {
        KoLAdventure.lastVisitedLocation = AdventureDatabase.getAdventure("The Oasis");
        // Any old non-free fight from our fixtures
        parseCombatData("request/test_fight_oil_slick.html");
        assertThat("lastCombatEnvironments", isSetTo("xxxxxxxxxxxxxxxxxxxo"));
      }
    }

    @Test
    public void doesNotCountFreeFights() {
      var cleanups = withProperty("lastCombatEnvironments", "ioioioioioioioioioio");
      try (cleanups) {
        KoLAdventure.lastVisitedLocation = AdventureDatabase.getAdventure("Hobopolis Town Square");
        // Any old free fight from our fixtures
        parseCombatData("request/test_fight_potted_plant.html");
        assertThat("lastCombatEnvironments", isSetTo("ioioioioioioioioioio"));
      }
    }
  }

  @Nested
  class StillSuit {
    @Test
    public void canTrackFamiliarSweatOnCurrentFamiliar() {
      var cleanups =
          new Cleanups(
              withProperty("familiarSweat", 5),
              withFamiliar(FamiliarPool.WOIM),
              withEquipped(EquipmentManager.FAMILIAR, "tiny stillsuit"));
      try (cleanups) {
        parseCombatData("request/test_fight_stillsuit_on_familiar.html");
        assertThat("familiarSweat", isSetTo(8));
      }
    }

    @Test
    public void canTrackFamiliarSweatOnTerrariumFamiliar() {
      var cleanups =
          new Cleanups(
              withProperty("familiarSweat", 5),
              withFamiliar(FamiliarPool.WOIM),
              withEquipped(EquipmentManager.FAMILIAR, "woimbook"),
              withFamiliarInTerrarium(FamiliarPool.PET_ROCK));
      try (cleanups) {
        var rock = KoLCharacter.findFamiliar(FamiliarPool.PET_ROCK);
        rock.setItem(ItemPool.get(ItemPool.STILLSUIT));
        parseCombatData("request/test_fight_stillsuit_in_terrarium.html");
        assertThat("familiarSweat", isSetTo(6));
      }
    }
  }
}
