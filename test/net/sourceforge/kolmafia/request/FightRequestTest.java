package net.sourceforge.kolmafia.request;

import static internal.helpers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
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
import net.sourceforge.kolmafia.session.LocketManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
  }

  private void parseCombatData(String path, String location, String encounter) throws IOException {
    String html = Files.readString(Paths.get(path)).trim();
    FightRequest.updateCombatData(location, encounter, html);
  }

  private void parseCombatData(String path, String location) throws IOException {
    parseCombatData(path, location, null);
  }

  private void parseCombatData(String path) throws IOException {
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
  public void commerceGhostIncrementsByOneOnFight() throws IOException {
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
  public void gnomeAdv() throws IOException {
    var familiar = FamiliarData.registerFamiliar(FamiliarPool.REAGNIMATED_GNOME, 1);
    KoLCharacter.setFamiliar(familiar);
    EquipmentManager.setEquipment(EquipmentManager.FAMILIAR, ItemPool.get(ItemPool.GNOMISH_KNEE));

    assertEquals(0, Preferences.getInteger("_gnomeAdv"));
    parseCombatData("request/test_fight_gnome_adv.html");
    assertEquals(1, Preferences.getInteger("_gnomeAdv"));
  }

  @Test
  public void mafiaThumbRingAdvs() throws IOException {
    assertEquals(0, Preferences.getInteger("_mafiaThumbRingAdvs"));
    parseCombatData("request/test_fight_mafia_thumb_ring.html");
    assertEquals(1, Preferences.getInteger("_mafiaThumbRingAdvs"));

    // Regression test for thumb ring adventures being picked up as Reagnimated Gnome adventures
    assertEquals(0, Preferences.getInteger("_gnomeAdv"));
  }

  @Test
  public void crystalBallPredictions() throws IOException {
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
  public void voidMonsterIncrementationTest() throws IOException {
    MonsterStatusTracker.setNextMonster(MonsterDatabase.findMonster("void slab"));
    parseCombatData("request/test_fight_void_monster.html");
    assertEquals(5, Preferences.getInteger("_voidFreeFights"));
  }

  @Test
  public void cursedMagnifyingGlassTest() throws IOException {
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
  public void daylightShavingTest() throws IOException {
    EquipmentManager.setEquipment(
        EquipmentManager.HAT, ItemPool.get(ItemPool.DAYLIGHT_SHAVINGS_HELMET));
    parseCombatData("request/test_fight_daylight_shavings_buff.html");
    assertEquals(2671, Preferences.getInteger("lastBeardBuff"));
  }

  @Test
  public void luckyGoldRingVolcoinoDropRecorded() throws IOException {
    assertFalse(Preferences.getBoolean("_luckyGoldRingVolcoino"));
    parseCombatData("request/test_fight_lucky_gold_ring_volcoino.html");
    assertTrue(Preferences.getBoolean("_luckyGoldRingVolcoino"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"alielf", "Black Crayon Crimbo Elf"})
  public void registersLocketFight(String monsterName) throws IOException {
    var monster = MonsterDatabase.findMonster(monsterName);
    MonsterStatusTracker.setNextMonster(monster);
    parseCombatData("request/test_fight_start_locket_fight_with_" + monster.getPhylum() + ".html");
    assertThat("locketPhylum", isSetTo(monster.getPhylum().toString()));
    assertThat("_locketMonstersFought", isSetTo(monster.getId()));
  }

  @Test
  public void rememberNewMonsterForLocket() throws IOException {
    assertFalse(LocketManager.remembersMonster(1568));

    MonsterStatusTracker.setNextMonster(MonsterDatabase.findMonster("Sloppy Seconds Sundae"));
    parseCombatData("request/test_fight_monster_added_to_locket.html");

    assertTrue(LocketManager.remembersMonster(1568));
  }

  @Test
  public void updatesListIfMonsterWasAlreadyInLocket() throws IOException {
    assertFalse(LocketManager.remembersMonster(155));

    MonsterStatusTracker.setNextMonster(MonsterDatabase.findMonster("Knob Goblin Barbecue Team"));
    parseCombatData("request/test_fight_monster_already_in_locket.html");

    assertTrue(LocketManager.remembersMonster(155));
  }

  @Test
  public void dontIncrementWitchessIfFromLocket() throws IOException {
    assertEquals(Preferences.getInteger("_witchessFights"), 0);

    MonsterStatusTracker.setNextMonster(MonsterDatabase.findMonster("Witchess Knight"));
    parseCombatData("request/test_fight_witchess_with_locket.html");

    assertEquals(Preferences.getInteger("_witchessFights"), 0);
  }

  static String loadHTMLResponse(String path) throws IOException {
    // Load the responseText from saved HTML file
    return Files.readString(Paths.get(path)).trim();
  }

  // Grey Goose Tests
  @Test
  public void shouldWorkAroundGreyGooseKoLBug() throws IOException {
    String html = loadHTMLResponse("request/test_fight_grey_goose_combat_skills.html");
    FightRequest.currentRound = 1;
    FamiliarData fam = new FamiliarData(FamiliarPool.GREY_GOOSE);
    KoLCharacter.setFamiliar(fam);
    fam.setWeight(6);
    // If we have a 6-lb. Grey Goose, the Grey Goose combat skills on the fight
    // page are valid.
    FightRequest.parseAvailableCombatSkills(html);
    assertTrue(KoLCharacter.hasCombatSkill(SkillPool.EMIT_MATTER_DUPLICATING_DRONES));
    // If it is less than 6-lbs., a KoL bug still shows them on the combat page,
    // but if you try to use them, "You don't know that skill."
    fam.setWeight(1);
    FightRequest.parseAvailableCombatSkills(html);
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.EMIT_MATTER_DUPLICATING_DRONES));
  }

  @Test
  public void canTrackMeatifyMatterCast() throws IOException {
    String html = loadHTMLResponse("request/test_fight_meatify_matter.html");
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
  public void canTrackGooseDrones() throws IOException {
    FamiliarData fam = new FamiliarData(FamiliarPool.GREY_GOOSE);
    KoLCharacter.setFamiliar(fam);
    KoLCharacter.getFamiliar().setWeight(6);

    assertEquals(0, Preferences.getInteger("gooseDronesRemaining"));

    String html = loadHTMLResponse("request/test_fight_goose_drones_1.html");
    MonsterStatusTracker.setNextMonster(MonsterDatabase.findMonster("angry tourist"));
    FightRequest.registerRequest(true, "fight.php?action=skill&whichskill=7410");
    FightRequest.currentRound = 1;
    FightRequest.updateCombatData(null, null, html);
    assertEquals(6, Preferences.getInteger("gooseDronesRemaining"));

    html = loadHTMLResponse("request/test_fight_goose_drones_2.html");
    FightRequest.registerRequest(true, "fight.php?action=attack");
    FightRequest.currentRound = 2;
    FightRequest.updateCombatData(null, null, html);
    assertEquals(5, Preferences.getInteger("gooseDronesRemaining"));

    html = loadHTMLResponse("request/test_fight_goose_drones_3.html");
    FightRequest.registerRequest(true, "fight.php?action=skill&whichskill=7410");
    FightRequest.currentRound = 1;
    FightRequest.updateCombatData(null, null, html);
    assertEquals(9, Preferences.getInteger("gooseDronesRemaining"));

    html = loadHTMLResponse("request/test_fight_goose_drones_4.html");
    FightRequest.registerRequest(true, "fight.php?action=attack");
    FightRequest.currentRound = 2;
    FightRequest.updateCombatData(null, null, html);
    assertEquals(7, Preferences.getInteger("gooseDronesRemaining"));

    html = loadHTMLResponse("request/test_fight_goose_drones_5.html");
    FightRequest.registerRequest(true, "fight.php?action=attack");
    FightRequest.currentRound = 1;
    FightRequest.updateCombatData(null, null, html);
    assertEquals(6, Preferences.getInteger("gooseDronesRemaining"));

    html = loadHTMLResponse("request/test_fight_goose_drones_6.html");
    FightRequest.registerRequest(true, "fight.php?action=attack");
    FightRequest.currentRound = 1;
    FightRequest.updateCombatData(null, null, html);
    assertEquals(4, Preferences.getInteger("gooseDronesRemaining"));

    html = loadHTMLResponse("request/test_fight_goose_drones_7.html");
    FightRequest.registerRequest(true, "fight.php?action=attack");
    FightRequest.currentRound = 1;
    FightRequest.updateCombatData(null, null, html);
    assertEquals(3, Preferences.getInteger("gooseDronesRemaining"));

    html = loadHTMLResponse("request/test_fight_goose_drones_8.html");
    FightRequest.registerRequest(true, "fight.php?action=attack");
    FightRequest.currentRound = 1;
    FightRequest.updateCombatData(null, null, html);
    assertEquals(2, Preferences.getInteger("gooseDronesRemaining"));

    html = loadHTMLResponse("request/test_fight_goose_drones_9.html");
    FightRequest.registerRequest(true, "fight.php?action=attack");
    FightRequest.currentRound = 1;
    FightRequest.updateCombatData(null, null, html);
    assertEquals(1, Preferences.getInteger("gooseDronesRemaining"));

    html = loadHTMLResponse("request/test_fight_goose_drones_10.html");
    FightRequest.registerRequest(true, "fight.php?action=attack");
    FightRequest.currentRound = 1;
    FightRequest.updateCombatData(null, null, html);
    assertEquals(0, Preferences.getInteger("gooseDronesRemaining"));
  }

  @Test
  public void canTrackDronesWithDramadery() throws IOException {
    FamiliarData fam = new FamiliarData(FamiliarPool.MELODRAMEDARY);
    KoLCharacter.setFamiliar(fam);

    String html = loadHTMLResponse("request/test_fight_drama_drones_1.html");
    FightRequest.registerRequest(true, "fight.php?action=attack");
    FightRequest.currentRound = 1;
    Preferences.setInteger("gooseDronesRemaining", 2);
    FightRequest.updateCombatData(null, null, html);
    assertEquals(1, Preferences.getInteger("gooseDronesRemaining"));
  }

  @Test
  public void canTrackGreyYouAbsorptions() throws IOException {
    FamiliarData fam = new FamiliarData(FamiliarPool.GREY_GOOSE);
    KoLCharacter.setFamiliar(fam);
    KoLCharacter.getFamiliar().setExperience(36);

    KoLCharacter.setPath(Path.GREY_YOU);
    GreyYouManager.resetAbsorptions();

    // Initial absorption of an albino bat
    String urlString = "fight.php?action=skill&whichskill=27000";
    String html = loadHTMLResponse("request/test_fight_goo_absorption_1.html");
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
    html = loadHTMLResponse("request/test_fight_goo_absorption_2.html");
    FightRequest.currentRound = 2;
    FightRequest.registerRequest(true, urlString);
    FightRequest.updateCombatData(null, null, html);
    assertTrue(GreyYouManager.absorbedMonsters.contains(monsterId));
    assertEquals(1, KoLCharacter.getFamiliar().getWeight());

    GreyYouManager.resetAbsorptions();

    // Absorbing a Passive Skill
    urlString = "fight.php?action=skill&whichskill=27000";
    html = loadHTMLResponse("request/test_fight_goo_absorption_3.html");
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
    html = loadHTMLResponse("request/test_fight_goo_absorption_4.html");
    monster = MonsterDatabase.findMonster("regular old bat");
    monsterId = monster.getId();
    MonsterStatusTracker.setNextMonster(monster);
    FightRequest.currentRound = 2;
    FightRequest.registerRequest(true, urlString);
    FightRequest.updateCombatData(null, null, html);
    assertFalse(GreyYouManager.absorbedMonsters.contains(monsterId));
  }

  // Cosmic Bowling Ball Tests
  @Test
  public void canTrackCosmicBowlingBall() throws IOException {
    FightRequest.currentRound = 1;

    // Off in the distance, you hear your cosmic bowling ball rattling around in the ball return
    // system.
    String html = loadHTMLResponse("request/test_fight_bowling_ball_1.html");
    FightRequest.parseAvailableCombatSkills(html);
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.BOWL_STRAIGHT_UP));

    // You hear your cosmic bowling ball rattling around in the ball return system.
    html = loadHTMLResponse("request/test_fight_bowling_ball_2.html");
    FightRequest.parseAvailableCombatSkills(html);
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.BOWL_STRAIGHT_UP));

    // You hear your cosmic bowling ball rattling around in the ball return system nearby.
    html = loadHTMLResponse("request/test_fight_bowling_ball_3.html");
    FightRequest.parseAvailableCombatSkills(html);
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.BOWL_STRAIGHT_UP));

    // You hear your cosmic bowling ball approaching.
    html = loadHTMLResponse("request/test_fight_bowling_ball_4.html");
    FightRequest.parseAvailableCombatSkills(html);
    assertFalse(KoLCharacter.hasCombatSkill(SkillPool.BOWL_STRAIGHT_UP));

    // Your cosmic bowling ball clatters into the closest ball return and you grab it.
    html = loadHTMLResponse("request/test_fight_bowling_ball_5.html");
    FightRequest.parseAvailableCombatSkills(html);
    assertTrue(KoLCharacter.hasCombatSkill(SkillPool.BOWL_STRAIGHT_UP));
  }
}
