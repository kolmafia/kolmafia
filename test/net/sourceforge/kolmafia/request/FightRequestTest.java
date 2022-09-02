package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withAnapest;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withFight;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withLastLocation;
import static internal.helpers.Player.withNextMonster;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSkill;
import static internal.helpers.Player.withoutSkill;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import internal.helpers.RequestLoggerOutput;
import java.util.Set;
import net.sourceforge.kolmafia.AscensionPath.Path;
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
import net.sourceforge.kolmafia.session.CrystalBallManager;
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
    KoLCharacter.reset("");
    KoLCharacter.reset("FightRequestTest");
    Preferences.saveSettingsToFile = false;
    KoLConstants.availableCombatSkillsList.clear();
    KoLConstants.availableCombatSkillsSet.clear();
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
    var cleanups = new Cleanups(withFamiliar(FamiliarPool.GHOST_COMMERCE));
    try (cleanups) {
      assertEquals(0, Preferences.getInteger("commerceGhostCombats"));
    }
  }

  @Nested
  class CommerceGhost {
    @Test
    public void commerceGhostIncrementsByOneOnFight() {
      var cleanups = new Cleanups(withFamiliar(FamiliarPool.GHOST_COMMERCE), withFight(0));
      try (cleanups) {
        assertEquals(0, Preferences.getInteger("commerceGhostCombats"));
        parseCombatData("request/test_fight_gnome_adv.html");
        assertEquals(1, Preferences.getInteger("commerceGhostCombats"));
      }
    }

    // If mafia has miscounted we should move our count
    @Test
    @Disabled("Response text does not trigger the code that detects action by ghost.")
    public void commerceGhostResetsTo10() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.GHOST_COMMERCE),
              withProperty("commerceGhostCombats", 5),
              withFight());
      try (cleanups) {
        FightRequest.updateCombatData(
            null,
            null,
            "<td style=\"color: white;\" align=center bgcolor=blue><b>Combat!</b></td></tr><tr><tdstyle=\"padding: 5px; border: 1px solid blue;\"><center><table><tr><td> Don't forget to buy a foo!");
        assertEquals(10, Preferences.getInteger("commerceGhostCombats"));
      }
    }

    // When we turn in the quest we should reset
    @Test
    @Disabled("Response text does not trigger the code that detects action by ghost.")
    public void commerceGhostResetsTo0() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.GHOST_COMMERCE),
              withProperty("commerceGhostCombats", 10),
              withFight());
      try (cleanups) {
        FightRequest.updateCombatData(null, null, "Nice, you bought a foo!");
        assertEquals(0, Preferences.getInteger("commerceGhostCombats"));
      }
    }
  }

  @Test
  public void gnomeAdv() {
    var cleanups =
        new Cleanups(
            withFamiliar(FamiliarPool.REAGNIMATED_GNOME),
            withEquipped(EquipmentManager.FAMILIAR, ItemPool.GNOMISH_KNEE),
            withFight());
    try (cleanups) {
      assertEquals(0, Preferences.getInteger("_gnomeAdv"));
      parseCombatData("request/test_fight_gnome_adv.html");
      assertEquals(1, Preferences.getInteger("_gnomeAdv"));
    }
  }

  @Test
  public void mafiaThumbRingAdvs() {
    assertEquals(0, Preferences.getInteger("_mafiaThumbRingAdvs"));
    parseCombatData("request/test_fight_mafia_thumb_ring.html");
    assertEquals(1, Preferences.getInteger("_mafiaThumbRingAdvs"));

    // Regression test for thumb ring adventures being picked up as Reagnimated Gnome adventures
    assertEquals(0, Preferences.getInteger("_gnomeAdv"));
  }

  @Nested
  class MiniatureCrystalBall {
    @BeforeEach
    void beforeEach() {
      FightRequest.clearInstanceData();
    }

    @Test
    public void noParsingWithoutBall() {
      var cleanups =
          new Cleanups(
              withProperty("crystalBallPredictions"),
              withLastLocation("The Neverending Party"),
              withFight());

      try (cleanups) {
        parseCombatData("request/test_fight_crystal_ball_neverending_party.html");
        assertThat("crystalBallPredictions", isSetTo(""));
      }
    }

    @Test
    public void parsesPredictionWithCrystalBall() {
      var cleanups =
          new Cleanups(
              withFight(0),
              withProperty("crystalBallPredictions"),
              withLastLocation("The Neverending Party"),
              withFamiliar(FamiliarPool.MOSQUITO),
              withEquipped(EquipmentManager.FAMILIAR, ItemPool.MINIATURE_CRYSTAL_BALL));

      try (cleanups) {
        CrystalBallManager.reset();
        parseCombatData("request/test_fight_crystal_ball_neverending_party.html");
        assertThat("crystalBallPredictions", isSetTo("0:The Neverending Party:party girl"));
      }
    }

    @Test
    void parsesASecondPrediction() {
      var cleanups =
          new Cleanups(
              withProperty("crystalBallPredictions", "0:The Neverending Party:party girl"),
              withLastLocation("The Red Zeppelin"),
              withFamiliar(FamiliarPool.MOSQUITO),
              withEquipped(EquipmentManager.FAMILIAR, ItemPool.MINIATURE_CRYSTAL_BALL),
              withFight(0));

      try (cleanups) {
        CrystalBallManager.reset();
        parseCombatData("request/test_fight_crystal_ball_zeppelin.html");
        assertThat(
            "crystalBallPredictions",
            isSetTo("0:The Neverending Party:party girl|0:The Red Zeppelin:Red Snapper"));
      }
    }
  }

  @Test
  public void voidMonsterIncrementationTest() {
    var cleanups =
        new Cleanups(withFight(0), withNextMonster("void slab"), withProperty("_voidFreeFights"));

    try (cleanups) {
      parseCombatData("request/test_fight_void_monster.html");
      assertEquals(5, Preferences.getInteger("_voidFreeFights"));
    }
  }

  @Nested
  class CursedMagnifyingGlass {
    @Test
    public void cursedMagnifyingGlassResetsOnVoidMonster() {
      var cleanups =
          new Cleanups(
              withEquipped(EquipmentManager.OFFHAND, ItemPool.CURSED_MAGNIFYING_GLASS),
              withProperty("cursedMagnifyingGlassCount", 13),
              withNextMonster("void slab"),
              withFight(0));

      try (cleanups) {
        parseCombatData("request/test_fight_void_monster.html");
        assertThat("cursedMagnifyingGlassCount", isSetTo(0));
      }
    }

    @Test
    public void cursedMagnifyingGlassCanUpdateAnIncorrectPref() {
      var cleanups =
          new Cleanups(
              withEquipped(EquipmentManager.OFFHAND, ItemPool.CURSED_MAGNIFYING_GLASS),
              withProperty("cursedMagnifyingGlassCount", 0),
              withNextMonster("lavatory"));

      try (cleanups) {
        parseCombatData("request/test_fight_cursed_magnifying_glass_update.html");
        assertThat("cursedMagnifyingGlassCount", isSetTo(3));
      }
    }
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

  @Nested
  class CombatLoversLocket {
    @ParameterizedTest
    @ValueSource(strings = {"alielf", "Black Crayon Crimbo Elf"})
    public void registersLocketFight(String monsterName) {
      var monster = MonsterDatabase.findMonster(monsterName);
      var cleanups = new Cleanups(withNextMonster(monster), withFight(0));

      try (cleanups) {
        parseCombatData(
            "request/test_fight_start_locket_fight_with_" + monster.getPhylum() + ".html");
        assertThat("locketPhylum", isSetTo(monster.getPhylum().toString()));
        assertThat("_locketMonstersFought", isSetTo(monster.getId()));
      }
    }

    @Test
    public void rememberNewMonsterForLocket() {
      var SLOPPY_SECONDS_SUNDAE = 1568;
      var cleanups = new Cleanups(withNextMonster("Sloppy Seconds Sundae"), withFight(0));
      try (cleanups) {
        assertFalse(LocketManager.remembersMonster(SLOPPY_SECONDS_SUNDAE));

        parseCombatData("request/test_fight_monster_added_to_locket.html");

        assertTrue(LocketManager.remembersMonster(SLOPPY_SECONDS_SUNDAE));
      }
    }

    @Test
    public void updatesListIfMonsterWasAlreadyInLocket() {
      var KNOB_GOBLIN_BBQ_TEAM = 155;
      var cleanups = new Cleanups(withNextMonster("Knob Goblin Barbecue Team"), withFight(0));
      try (cleanups) {
        assertFalse(LocketManager.remembersMonster(KNOB_GOBLIN_BBQ_TEAM));

        parseCombatData("request/test_fight_monster_already_in_locket.html");

        assertTrue(LocketManager.remembersMonster(KNOB_GOBLIN_BBQ_TEAM));
      }
    }

    @Test
    public void dontIncrementWitchessIfFromLocket() {
      var cleanups =
          new Cleanups(
              withNextMonster("Witches Knight"), withFight(), withProperty("_witchessFights", 0));

      try (cleanups) {
        parseCombatData("request/test_fight_witchess_with_locket.html");

        assertEquals(Preferences.getInteger("_witchessFights"), 0);
      }
    }
  }

  @Nested
  class GreyGoose {
    @ParameterizedTest
    @CsvSource({
      // If we have a 6-lb. Grey Goose, the Grey Goose combat skills on the fight
      // page are valid.
      "6, true",
      // If it is less than 6-lbs., a KoL bug still shows them on the combat page,
      // but if you try to use them, "You don't know that skill."
      "1, false",
    })
    public void shouldWorkAroundGreyGooseKoLBug(int weight, boolean hasSkill) {
      var cleanups =
          new Cleanups(withFight(1), withFamiliar(FamiliarPool.GREY_GOOSE, weight * weight));

      try (cleanups) {
        String html = html("request/test_fight_grey_goose_combat_skills.html");

        FightRequest.parseAvailableCombatSkills(html);
        assertThat(
            KoLCharacter.hasCombatSkill(SkillPool.EMIT_MATTER_DUPLICATING_DRONES), is(hasSkill));
      }
    }

    @Test
    public void canTrackMeatifyMatterCast() {
      var cleanups =
          new Cleanups(
              withFight(1),
              withFamiliar(FamiliarPool.GREY_GOOSE, 6 * 6),
              withNextMonster("angry tourist"));

      try (cleanups) {
        String html = html("request/test_fight_meatify_matter.html");
        FightRequest.registerRequest(true, "fight.php?action=skill&whichskill=7409");
        FightRequest.updateCombatData(null, null, html);

        assertThat(KoLCharacter.getFamiliar().getWeight(), is(5));
        assertThat("_meatifyMatterUsed", isSetTo(true));
        assertThat(SkillDatabase.getMaxCasts(SkillPool.MEATIFY_MATTER), is(0L));
      }
    }

    // X bits of goo emerge from <name> and begin hovering about, moving probingly around various
    // objects.
    // One of the matter duplicating drones seems to coalesce around the <item> and then transforms
    // into an exact replica. <X-1> more drones are still circling around.
    // One of the matter duplicating drones seems to coalesce around the <item> and then transforms
    // into an exact replica. That was the last drone.

    @ParameterizedTest
    @CsvSource({
      "1, skill&whichskill=7410, 1, 0, 6",
      "2, attack, 2, 6, 5",
      "3, skill&whichskill=7410, 1, 5, 9",
      "4, attack, 2, 9, 7",
      "5, attack, 1, 7, 6",
      "6, attack, 1, 6, 4",
      "7, attack, 1, 4, 3",
      "8, attack, 1, 3, 2",
      "9, attack, 1, 2, 1",
      "10, attack, 1, 1, 0",
    })
    public void canTrackGooseDrones(
        int file, String action, int round, int dronesBefore, int dronesAfter) {
      var cleanups =
          new Cleanups(
              withFight(round),
              withFamiliar(FamiliarPool.GREY_GOOSE, 6 * 6),
              withNextMonster("angry tourist"),
              withProperty("gooseDronesRemaining", dronesBefore));

      try (cleanups) {
        String html = html("request/test_fight_goose_drones_" + file + ".html");
        FightRequest.registerRequest(true, "fight.php?action=" + action);
        FightRequest.updateCombatData(null, null, html);
        assertThat("gooseDronesRemaining", isSetTo(dronesAfter));
      }
    }

    @Test
    public void canTrackCastAndUseGooseDrones() {
      var cleanups =
          new Cleanups(
              withFight(1),
              withFamiliar(FamiliarPool.GREY_GOOSE, 6 * 6),
              withNextMonster("Witchess Knight"),
              withProperty("gooseDronesRemaining", 0));

      try (cleanups) {
        String html = html("request/test_fight_cast_and_use_drones.html");
        // Multi-round response text. Matter_Duplicating drones emitted one round and used-up next
        // round
        FightRequest.registerRequest(
            true,
            "fight.php?action=macro&macrotext=if+hasskill+curse+of+weaksauce%3Bskill+curse+of+weaksauce%3Bendif%3Bif+hascombatitem+porquoise-handled+sixgun+%26%26+hascombatitem+mayor+ghost%3Buse+porquoise-handled+sixgun%2Cmayor+ghost%3Bendif%3Bif+hasskill+bowl+straight+up%3Bskill+bowl+straight+up%3Bendif%3Bif+hascombatitem+spooky+putty+sheet%3Buse+spooky+putty+sheet%3Bendif%3Bif+hasskill+emit+matter+duplicating+drones%3Bskill+emit+matter+duplicating+drones%3Bendif%3Battack%3Brepeat%3Babort%3B");
        FightRequest.updateCombatData(null, null, html);
        assertEquals(0, Preferences.getInteger("gooseDronesRemaining"));
      }
    }
  }

  @Test
  public void canFindItemsAfterSlayTheDead() {
    var cleanups =
        new Cleanups(
            withFight(1),
            withFamiliar(FamiliarPool.GREY_GOOSE, 6 * 6),
            withNextMonster("toothy sklelton"));

    try (cleanups) {
      String html = html("request/test_fight_slay_the_dead.html");
      String url =
          "fight.php?action=macro&macrotext=abort+hppercentbelow+20%3B+abort+pastround+25%3B+skill+Slay+the+Dead%3B+use+beehive%3B+skill+Double+Nanovision%3B+repeat%3B+mark+eof%3B+";
      FightRequest.registerRequest(true, url);
      FightRequest.updateCombatData(null, null, html);
      assertEquals(1, InventoryManager.getCount(ItemPool.LOOSE_TEETH));
      assertEquals(1, InventoryManager.getCount(ItemPool.SKELETON_BONE));
      assertEquals(1, InventoryManager.getCount(ItemPool.BONE_FLUTE));
      assertEquals(1, InventoryManager.getCount(ItemPool.EVIL_EYE));
    }

    KoLConstants.inventory.clear();
  }

  @Test
  public void canFindItemsWithGravyBoat() {
    var cleanups = new Cleanups(withFight(2), withNextMonster("spiny skelelton"));

    try (cleanups) {
      String html = html("request/test_fight_gravy_boat_1.html");
      String url = "fight.php?action=skill&whichskill=27043";
      FightRequest.registerRequest(true, url);
      // This html has hewn moon-rune spoon munging. processResults will un-munge
      // it before calling updateCombatData.
      FightRequest.processResults(null, null, html);
      assertEquals(1, InventoryManager.getCount(ItemPool.SKELETON_BONE));
      assertEquals(1, InventoryManager.getCount(ItemPool.SMART_SKULL));
      assertEquals(1, InventoryManager.getCount(ItemPool.EVIL_EYE));
      assertEquals(1, InventoryManager.getCount(ItemPool.BOTTLE_OF_GIN));
      KoLConstants.inventory.clear();
    }
  }

  @Test
  public void canFindItemsWithGravyBoatAndSlayTheDead() {
    var cleanups = new Cleanups(withFight(2), withNextMonster("toothy sklelton"));

    try (cleanups) {
      String html = html("request/test_fight_gravy_boat_2.html");
      String url = "fight.php?action=skill&whichskill=7348";
      FightRequest.registerRequest(true, url);
      // This html has hewn moon-rune spoon munging. processResults will un-munge
      // it before calling updateCombatData.
      FightRequest.processResults(null, null, html);
      assertEquals(1, InventoryManager.getCount(ItemPool.LOOSE_TEETH));
      assertEquals(1, InventoryManager.getCount(ItemPool.EVIL_EYE));

      KoLConstants.inventory.clear();
    }
  }

  @ParameterizedTest
  @CsvSource({"drones_1, 60, 63", "spit_1, 1000, 0"})
  public void canTrackDramederyActions(String source, int spitBefore, int spitAfter) {
    var cleanups =
        new Cleanups(
            withFamiliar(FamiliarPool.MELODRAMEDARY, "Gogarth"),
            withFight(1),
            withProperty("camelSpit", spitBefore));

    try (cleanups) {
      String html = html("request/test_fight_drama_" + source + ".html");
      FightRequest.registerRequest(true, "fight.php?action=attack");
      FightRequest.updateCombatData(null, null, html);
      assertThat("camelSpit", isSetTo(spitAfter));
    }
  }

  @Test
  public void canTrackDronesWithDramedery() {
    var cleanups =
        new Cleanups(
            withFamiliar(FamiliarPool.MELODRAMEDARY, "Gogarth"),
            withFight(1),
            withProperty("gooseDronesRemaining", 2));

    try (cleanups) {
      String html = html("request/test_fight_drama_drones_1.html");
      FightRequest.registerRequest(true, "fight.php?action=attack");
      FightRequest.updateCombatData(null, null, html);
      assertThat("gooseDronesRemaining", isSetTo(1));
    }
  }

  @Nested
  class GreyYou {
    @Test
    public void canAbsorbAlbinoBat() {
      var ALBINO_BAT = 41;
      var cleanups =
          new Cleanups(
              withFight(2),
              withFamiliar(FamiliarPool.GREY_GOOSE, 36),
              withPath(Path.GREY_YOU),
              withNextMonster("albino bat"));

      try (cleanups) {
        String urlString = "fight.php?action=skill&whichskill=27000";
        String html = html("request/test_fight_goo_absorption_1.html");

        FightRequest.registerRequest(true, urlString);
        FightRequest.updateCombatData(null, null, html);

        assertThat(GreyYouManager.absorbedMonsters, hasItem(ALBINO_BAT));
        assertThat(KoLCharacter.getFamiliar().getWeight(), equalTo(6));
      }

      GreyYouManager.resetAbsorptions();
    }

    @Test
    public void canReAbsorbAlbinoBat() {
      var ALBINO_BAT = 41;
      var cleanups =
          new Cleanups(
              withFight(2),
              withFamiliar(FamiliarPool.GREY_GOOSE, 36),
              withPath(Path.GREY_YOU),
              withNextMonster("albino bat"),
              withProperty("gooseReprocessed", ""));

      try (cleanups) {
        var urlString = "fight.php?action=skill&whichskill=7408";
        var html = html("request/test_fight_goo_absorption_2.html");
        FightRequest.registerRequest(true, urlString);
        FightRequest.updateCombatData(null, null, html);

        assertThat(GreyYouManager.absorbedMonsters, hasItem(ALBINO_BAT));
        assertThat("gooseReprocessed", isSetTo(ALBINO_BAT));
        assertThat(KoLCharacter.getFamiliar().getWeight(), equalTo(1));
      }

      GreyYouManager.resetAbsorptions();
    }

    @Test
    public void canAbsorbPassiveSkill() {
      var RUSHING_BUM = 159;
      var cleanups =
          new Cleanups(
              withFight(2),
              withFamiliar(FamiliarPool.GREY_GOOSE, 36),
              withPath(Path.GREY_YOU),
              withNextMonster("rushing bum"),
              withProperty("gooseReprocessed", ""),
              withoutSkill(SkillPool.HARRIED));

      try (cleanups) {
        var urlString = "fight.php?action=skill&whichskill=27000";
        var html = html("request/test_fight_goo_absorption_3.html");

        FightRequest.registerRequest(true, urlString);
        FightRequest.updateCombatData(null, null, html);

        assertThat(GreyYouManager.absorbedMonsters, hasItem(RUSHING_BUM));
        assertTrue(KoLCharacter.hasSkill(SkillPool.HARRIED));
      }

      GreyYouManager.resetAbsorptions();
    }

    @Test
    public void canAbsorbNonSpecialMonster() {
      var REGULAR_OLD_BAT = 44;
      var cleanups =
          new Cleanups(
              withFight(2),
              withFamiliar(FamiliarPool.GREY_GOOSE, 36),
              withPath(Path.GREY_YOU),
              withNextMonster("regular old bat"),
              withProperty("gooseReprocessed", ""));

      try (cleanups) {
        var urlString = "fight.php?action=skill&whichskill=27000";
        var html = html("request/test_fight_goo_absorption_4.html");
        FightRequest.registerRequest(true, urlString);
        FightRequest.updateCombatData(null, null, html);

        assertThat(GreyYouManager.absorbedMonsters, not(hasItem(REGULAR_OLD_BAT)));
      }
    }

    @Test
    public void canReabsorbSecondMonster() {
      var cleanups =
          new Cleanups(
              withFight(2),
              withFamiliar(FamiliarPool.GREY_GOOSE, 36),
              withPath(Path.GREY_YOU),
              withNextMonster("regular old bat"),
              withProperty("gooseReprocessed", "41"));

      try (cleanups) {
        // Second absorption of a model skeleton via Re-Process Matter
        var urlString = "fight.php?action=skill&whichskill=7408";
        var html = html("request/test_fight_goo_absorption_5.html");
        FightRequest.registerRequest(true, urlString);
        FightRequest.updateCombatData(null, null, html);
        assertThat("gooseReprocessed", isSetTo("41,1547"));
      }
    }

    @Test
    public void canReabsorbThirdMonster() {
      var cleanups =
          new Cleanups(
              withFight(2),
              withFamiliar(FamiliarPool.GREY_GOOSE, 36),
              withPath(Path.GREY_YOU),
              withNextMonster("regular old bat"),
              withProperty("gooseReprocessed", "41,1547"));

      try (cleanups) {
        // Second absorption of a model skeleton via Re-Process Matter
        var urlString = "fight.php?action=skill&whichskill=7408";
        var html = html("request/test_fight_goo_absorption_5.html");
        FightRequest.registerRequest(true, urlString);
        FightRequest.updateCombatData(null, null, html);
        assertThat("gooseReprocessed", isSetTo("41,1547"));
      }
    }
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
      var cleanups = new Cleanups(withFight());
      try (cleanups) {
        String html = html("request/test_fight_bowling_ball_" + step + ".html");
        FightRequest.parseAvailableCombatSkills(html);
        assertThat(KoLCharacter.hasCombatSkill(SkillPool.BOWL_STRAIGHT_UP), equalTo(step == 5));
      }
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
  @ParameterizedTest
  @CsvSource({
    "1, 1", "2, 0",
  })
  public void canTrackRoboDrops(int source, int drops) {
    var cleanups =
        new Cleanups(withFamiliar(FamiliarPool.ROBORTENDER), withProperty("_roboDrops", 0));
    try (cleanups) {
      parseCombatData("request/test_fight_robort_drops_" + source + ".html");
      assertEquals(drops, Preferences.getInteger("_roboDrops"));
    }
  }

  @Test
  public void canDetectMaydaySupplyPackage() {
    var cleanups = new Cleanups(withProperty("_maydayDropped", false));
    try (cleanups) {
      parseCombatData("request/test_fight_mayday_contract.html");
      assertTrue(Preferences.getBoolean("_maydayDropped"));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "fight_june_cleaver, Sleaze",
    "fight_lovebug_booze, Cold",
    "desert_exploration_no_bonuses, Spooky",
    "fight_lovebug_cricket, Stench",
    "fight_potted_plant, Hot",
  })
  public void canTrackJuneCleaverPrefs(String file, String element) {
    var cleanups =
        new Cleanups(
            withEquipped(EquipmentManager.WEAPON, ItemPool.JUNE_CLEAVER),
            withProperty("_juneCleaver" + element),
            withProperty("_juneCleaverFightsLeft"));

    try (cleanups) {
      parseCombatData("request/test_" + file + ".html");
      assertEquals(Preferences.getInteger("_juneCleaver" + element), 2);
      assertEquals(Preferences.getInteger("_juneCleaverFightsLeft"), 0);
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  public void canTrackBellydancerPickpocket(final int pickpockets) {
    var cleanups =
        new Cleanups(withFight(0), withProperty("_bellydancerPickpockets", pickpockets - 1));

    try (cleanups) {
      parseCombatData("request/test_fight_bellydancing_pickpocket_" + pickpockets + ".html");
      assertThat("_bellydancerPickpockets", isSetTo(pickpockets));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "request/test_fight_designer_sweatpants_gain_2_sweat.html, 2",
    "request/test_fight_designer_sweatpants_lose_3_sweat.html, -3"
  })
  public void canTrackDesignerSweatpants(String responseHtml, int sweatChange) {
    var cleanups =
        new Cleanups(
            withEquipped(EquipmentManager.PANTS, "designer sweatpants"),
            withProperty("sweat", 10),
            withFight());

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

  @Test
  public void canDetectPowerfulGloveCharge() {
    var cleanups =
        new Cleanups(
            withFight(),
            withProperty("_powerfulGloveBatteryPowerUsed", 0),
            withEquipped(EquipmentManager.ACCESSORY1, "Powerful Glove"));

    try (cleanups) {
      String html = html("request/test_fight_skill_name_uses_remaining.html");
      FightRequest.parseAvailableCombatSkills(html);

      assertTrue(KoLCharacter.hasCombatSkill(SkillPool.REPLACE_ENEMY));
      assertTrue(KoLCharacter.hasCombatSkill(SkillPool.SHRINK_ENEMY));
      assertThat("_powerfulGloveBatteryPowerUsed", isSetTo(30));
    }
  }

  @Test
  public void canDetectCosplaySaberUses() {
    var cleanups =
        new Cleanups(
            withFight(),
            withProperty("_saberForceUses", 0),
            withEquipped(EquipmentManager.WEAPON, "Fourth of May Cosplay Saber"));

    try (cleanups) {
      String html = html("request/test_fight_skill_name_uses_remaining.html");
      FightRequest.parseAvailableCombatSkills(html);

      assertTrue(KoLCharacter.hasCombatSkill(SkillPool.USE_THE_FORCE));
      assertThat("_saberForceUses", isSetTo(3));
    }
  }

  @Test
  public void canDetectLilDoctorBagUses() {
    var cleanups =
        new Cleanups(
            withFight(),
            withProperty("_otoscopeUsed", 10),
            withProperty("_reflexHammerUsed", 10),
            withProperty("_chestXRayUsed", 10),
            withEquipped(EquipmentManager.ACCESSORY1, "Lil' Doctor™ bag"));

    try (cleanups) {
      String html = html("request/test_fight_skill_name_uses_remaining.html");
      FightRequest.parseAvailableCombatSkills(html);

      assertTrue(KoLCharacter.hasCombatSkill(SkillPool.OTOSCOPE));
      assertTrue(KoLCharacter.hasCombatSkill(SkillPool.REFLEX_HAMMER));
      assertTrue(KoLCharacter.hasCombatSkill(SkillPool.CHEST_X_RAY));
      assertThat("_otoscopeUsed", isSetTo(0));
      assertThat("_reflexHammerUsed", isSetTo(0));
      assertThat("_chestXRayUsed", isSetTo(0));
    }
  }

  @Test
  public void canDetectExtinguisherCharge() {
    var cleanups =
        new Cleanups(
            withFight(),
            withProperty("_fireExtinguisherCharge", 0),
            withEquipped(EquipmentManager.WEAPON, "Industrial Fire Extinguisher"));

    try (cleanups) {
      String html = html("request/test_fight_skill_name_uses_remaining.html");
      FightRequest.parseAvailableCombatSkills(html);

      assertTrue(KoLCharacter.hasCombatSkill(SkillPool.FIRE_EXTINGUISHER__FOAM_EM_UP));
      assertThat("_fireExtinguisherCharge", isSetTo(35));
    }
  }

  @Test
  public void canDetectVampyreCloakeFormUses() {
    var cleanups =
        new Cleanups(
            withFight(),
            withProperty("_vampyreCloakeFormUses", 5),
            withEquipped(EquipmentManager.CONTAINER, "Vampyric cloake"));

    try (cleanups) {
      String html = html("request/test_fight_skill_name_uses_remaining.html");
      FightRequest.parseAvailableCombatSkills(html);

      assertTrue(KoLCharacter.hasCombatSkill(SkillPool.BECOME_BAT));
      assertTrue(KoLCharacter.hasCombatSkill(SkillPool.BECOME_MIST));
      assertTrue(KoLCharacter.hasCombatSkill(SkillPool.BECOME_WOLF));
      assertThat("_vampyreCloakeFormUses", isSetTo(0));
    }
  }

  @Test
  public void canDetectMeteorLoreUses() {
    var cleanups =
        new Cleanups(
            withFight(),
            withProperty("_meteorShowerUses", 1),
            withProperty("_macrometeoriteUses", 1));

    try (cleanups) {
      String html = html("request/test_fight_skill_name_uses_remaining.html");
      FightRequest.parseAvailableCombatSkills(html);

      assertTrue(KoLCharacter.hasCombatSkill(SkillPool.METEOR_SHOWER));
      assertTrue(KoLCharacter.hasCombatSkill(SkillPool.MACROMETEOR));
      assertThat("_meteorShowerUses", isSetTo(0));
      assertThat("_macrometeoriteUses", isSetTo(0));
    }
  }

  @Test
  public void canDetectPantsgivingBanishUses() {
    var cleanups =
        new Cleanups(
            withFight(),
            withProperty("_pantsgivingBanish", 3),
            withEquipped(EquipmentManager.PANTS, "Pantsgiving"));

    try (cleanups) {
      String html = html("request/test_fight_skill_name_uses_remaining.html");
      FightRequest.parseAvailableCombatSkills(html);

      assertTrue(KoLCharacter.hasCombatSkill(SkillPool.TALK_ABOUT_POLITICS));
      assertTrue(KoLCharacter.hasCombatSkill(SkillPool.POCKET_CRUMBS));
      assertThat("_pantsgivingBanish", isSetTo(0));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "5, true",
    "0, false",
  })
  public void canDetectBackupCameraUses(int backupsUsed, boolean youRobotPath) {
    // Back-Up to your Last Enemy (11 uses today)
    var cleanups =
        new Cleanups(
            withPath(youRobotPath ? Path.YOU_ROBOT : Path.NONE),
            withFight(),
            withProperty("_backUpUses", 3),
            withEquipped(EquipmentManager.ACCESSORY1, "Backup Camera"));

    try (cleanups) {
      String html = html("request/test_fight_skill_name_uses_remaining.html");
      FightRequest.parseAvailableCombatSkills(html);

      assertTrue(KoLCharacter.hasCombatSkill(SkillPool.BACK_UP));
      assertThat("_backUpUses", isSetTo(backupsUsed));
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
      var cleanups = new Cleanups(withProperty("_xoHugsUsed", 0), withFight(2));

      try (cleanups) {
        String urlString = "fight.php?action=macro&macrotext=skill+7293&whichmacro=0";
        String html = html("request/test_fight_hugs_and_kisses_success.html");
        FightRequest.registerRequest(true, urlString);
        FightRequest.updateCombatData(null, null, html);
        assertThat("_xoHugsUsed", isSetTo(1));
      }
    }

    @Test
    public void canTrackHugsAndKissesFailure() {
      var cleanups = new Cleanups(withProperty("_xoHugsUsed", 3), withFight(2));

      try (cleanups) {
        String urlString = "fight.php?action=macro&macrotext=skill+7293&whichmacro=0";
        String html = html("request/test_fight_hugs_and_kisses_failure.html");
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
      var cleanups = new Cleanups(withProperty("_hoboUnderlingSummons", 0), withFight(2));

      try (cleanups) {
        String urlString = "fight.php?action=skill&whichskill=7052";
        String html = html("request/test_fight_summon_hobo_underling.html");
        FightRequest.registerRequest(true, urlString);
        FightRequest.updateCombatData(null, null, html);
        assertThat("_hoboUnderlingSummons", isSetTo(1));
      }
    }

    @Test
    public void askHoboToDoADance() {
      var cleanups = new Cleanups(withFight(3), withFight(0));

      try (cleanups) {
        String urlString = "fight.php?action=skill&whichskill=7051";
        String html = html("request/test_fight_ask_hobo_to_dance.html");
        FightRequest.registerRequest(true, urlString);
        FightRequest.updateCombatData(null, null, html);
        var fightMods = Modifiers.getModifiers("Generated", "fightMods");
        assertThat(fightMods.get(Modifiers.ITEMDROP), equalTo(100.0));
      }
    }

    @Test
    public void askHoboToTellAJoke() {
      var cleanups = new Cleanups(withFight(3));

      try (cleanups) {
        String urlString = "fight.php?action=skill&whichskill=7050";
        String html = html("request/test_fight_ask_hobo_to_joke.html");
        FightRequest.registerRequest(true, urlString);
        FightRequest.updateCombatData(null, null, html);
        var fightMods = Modifiers.getModifiers("Generated", "fightMods");
        assertThat(fightMods.get(Modifiers.MEATDROP), equalTo(100.0));
      }
    }
  }

  @Nested
  class LoveBugsPreferenceButtonGroupTest {
    @BeforeAll
    public static void beforeAll() {
      Preferences.saveSettingsToFile = false;
    }

    @AfterAll
    public static void afterAll() {
      Preferences.saveSettingsToFile = true;
    }

    @BeforeEach
    public void beforeEach() {
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

  @Nested
  class Dinosaurs {
    @ParameterizedTest
    @CsvSource({
      "request/test_dino_archelon.html, none, archelon, animated ornate nightstand",
      "request/test_dino_dilophosaur.html, carrion-eating, dilophosaur, cosmetics wraith",
      "request/test_dino_flatusaurus.html, steamy, flatusaurus, Hellion",
      "request/test_dino_ghostasaurus.html, none, ghostasaurus, cubist bull",
      "request/test_dino_kachungasaur.html, none, kachungasaur, malevolent hair clog",
      "request/test_dino_primitive_chicken.html, none, chicken, amateur ninja",
      "request/test_dino_pterodactyl.html, none, pterodactyl, W imp",
      "request/test_dino_spikolodon.html, none, spikolodon, empty suit of armor",
      "request/test_dino_velociraptor.html, none, velociraptor, cubist bull",
    })
    public void canExtractDinosaurFromFight(
        String filename, String modifier, String dinosaur, String monsterName) {

      // This is obviously not where these dinosaurs were encountered.
      // However, in order to register an encounter (which is where we parse
      // the dinosaur attributes), we had to register the request first, and
      // this location will do as well as any.
      var cleanups = new Cleanups(withLastLocation("The Haunted Pantry"), withPath(Path.DINOSAURS));

      try (cleanups) {
        GenericRequest request = new GenericRequest("fight.php");
        request.responseText = html(filename);
        String encounter = AdventureRequest.registerEncounter(request);
        MonsterData monster = MonsterStatusTracker.getLastMonster();
        assertEquals(monster.getName(), monsterName);
        var modifiers = Set.of(monster.getRandomModifiers());
        if (!modifier.equals("none")) {
          assertTrue(modifiers.contains(modifier));
        }
        assertTrue(modifiers.contains(dinosaur));
      }
    }
  }

  @Nested
  class PocketProfessor {
    @Test
    public void incrementsLecturesIfFamWeightIncreasesMidCombat() {
      var cleanups =
          new Cleanups(
              withSkill(SkillPool.METEOR_SHOWER),
              withFamiliar(FamiliarPool.POCKET_PROFESSOR, 100),
              withProperty("_pocketProfessorLectures", 4));

      try (cleanups) {
        parseCombatData("request/test_fight_meteor_shower_lecture.html");
        assertThat("_pocketProfessorLectures", isSetTo(5));
      }
    }
  }
}
