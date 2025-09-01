package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.*;
import static internal.matchers.Item.isInInventory;
import static internal.matchers.Preference.hasIntegerValue;
import static internal.matchers.Preference.hasStringValue;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.helpers.RequestLoggerOutput;
import internal.helpers.SessionLoggerOutput;
import internal.network.FakeHttpClientBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.CrystalBallManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.GoalManager;
import net.sourceforge.kolmafia.session.GreyYouManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.LocketManager;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.hamcrest.core.Is;
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
    KoLCharacter.setUserId(1);
    KoLConstants.availableCombatSkillsList.clear();
    KoLConstants.availableCombatSkillsSet.clear();
    KoLAdventure.setLastAdventure("None");
  }

  private void parseCombatData(String path, String location, String encounter) {
    String html = html(path);

    if (location != null) {
      FightRequest.registerRequest(true, location);
    }

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
  @Nested
  class CommerceGhost {
    @Test
    public void commerceGhostStartsAtProperValue() {
      var cleanups = new Cleanups(withFamiliar(FamiliarPool.GHOST_COMMERCE));
      try (cleanups) {
        assertEquals(0, Preferences.getInteger("commerceGhostCombats"));
      }
    }

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

  @Nested
  class CookBookBat {
    @ParameterizedTest
    @CsvSource({"test_fight_win.html,2", "test_fight_lose.html,1", "test_fight_run.html,1"})
    public void handlesFight(String file, int expectedCharge) {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.COOKBOOKBAT),
              withProperty("cookbookbatIngredientsCharge", 1));
      try (cleanups) {
        parseCombatData("request/" + file);
        assertThat("cookbookbatIngredientsCharge", isSetTo(expectedCharge));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "test_fight_cookbookbat_ingredients.html,10,0",
      "test_fight_win.html,2,3",
      "test_fight_lose.html,2,2",
      "test_fight_run.html,2,2"
    })
    public void handlesIngredientsCharge(String file, int chargeInitial, int chargeExpected) {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.COOKBOOKBAT),
              withProperty("cookbookbatIngredientsCharge", chargeInitial));
      try (cleanups) {
        parseCombatData("request/" + file);
        assertThat("cookbookbatIngredientsCharge", isSetTo(chargeExpected));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "test_fight_cookbookbat_quest_new_1.html,false,skullery maid,The Haunted Kitchen,Vegetable of Jarlsberg",
      "test_fight_cookbookbat_quest_new_2.html,false,crate,Noob Cave,Yeast of Boris",
      "test_fight_cookbookbat_quest_new_3.html,false,novelty tropical skeleton,The Skeleton Store,St. Sneaky Pete's Whey",
      "test_fight_cookbookbat_quest_reminder_1.html,true,'',The Haunted Kitchen,''",
      "test_fight_cookbookbat_quest_reminder_2.html,true,crate,'',''",
      "test_fight_cookbookbat_quest_reminder_3.html,true,horrible tourist family,Barf Mountain,''",
    })
    public void handlesQuest(
        String file,
        boolean isReminder,
        String monsterName,
        String locationName,
        String ingredientName) {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.COOKBOOKBAT),
              withProperty("_cookbookbatQuestMonster", ""),
              withProperty("_cookbookbatQuestLastLocation", ""),
              withProperty("_cookbookbatQuestIngredient", ""),
              withProperty("_cookbookbatCombatsUntilNewQuest", 2));
      try (cleanups) {
        parseCombatData("request/" + file);
        assertThat("_cookbookbatQuestMonster", isSetTo(monsterName));
        assertThat("_cookbookbatQuestLastLocation", isSetTo(locationName));
        assertThat("_cookbookbatQuestIngredient", isSetTo(ingredientName));
        assertThat("_cookbookbatCombatsUntilNewQuest", isSetTo(isReminder ? 1 : 5));
      }
    }

    @Test
    public void handlesQuestComplete() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.COOKBOOKBAT),
              withProperty("_cookbookbatQuestMonster", "skullery maid"),
              withProperty("_cookbookbatQuestLastLocation", "The Haunted Kitchen"),
              withProperty("_cookbookbatQuestIngredient", "Vegetable of Jarlsberg"),
              withProperty("_cookbookbatCombatsUntilNewQuest", 3));
      try (cleanups) {
        parseCombatData("request/test_fight_cookbookbat_quest_complete.html");
        assertThat("_cookbookbatQuestMonster", isSetTo(""));
        assertThat("_cookbookbatQuestLastLocation", isSetTo("The Haunted Kitchen"));
        assertThat("_cookbookbatQuestIngredient", isSetTo(""));
        assertThat("_cookbookbatCombatsUntilNewQuest", isSetTo(2));
      }
    }

    @ParameterizedTest
    @CsvSource({
      // Counter decrements on win, not on loss
      "test_fight_win.html,crate,Noob Cave,false,3,2",
      "test_fight_lose.html,crate,Noob Cave,false,3,3",
      "test_fight_run.html,crate,Noob Cave,false,3,3",
      // Counter decrements to 0 if not in location
      "test_fight_win.html,crate,Noob Cave,false,1,0",
      // Counter decrements to >0 in location
      "test_fight_win.html,crate,Noob Cave,true,3,2",
      // Counter doesn't decrement to 0 in location
      "test_fight_win.html,crate,Noob Cave,true,1,1",
      // Counter decrements (doesn't reset) on quest reminder
      "test_fight_cookbookbat_quest_reminder_3.html,horrible tourist family,Barf Mountain,true,3,2",
    })
    public void decrementsQuestCounters(
        String file,
        String monsterName,
        String locationName,
        boolean inLocation,
        int newInitial,
        int newExpected) {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.COOKBOOKBAT),
              withProperty("_cookbookbatQuestMonster", monsterName),
              withProperty("_cookbookbatQuestLastLocation", locationName),
              withProperty("_cookbookbatCombatsUntilNewQuest", newInitial));
      if (inLocation) cleanups.add(withLastLocation(locationName));
      try (cleanups) {
        parseCombatData("request/" + file);
        assertThat("_cookbookbatQuestMonster", isSetTo(monsterName));
        assertThat("_cookbookbatQuestLastLocation", isSetTo(locationName));
        assertThat("_cookbookbatCombatsUntilNewQuest", isSetTo(newExpected));
      }
    }

    @Test
    public void handlesNullLocation() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.COOKBOOKBAT),
              withProperty("_cookbookbatQuestMonster", "crate"),
              withProperty("_cookbookbatQuestLastLocation", "Noob Cave"),
              withProperty("_cookbookbatQuestIngredient", "Vegetable of Jarlsberg"),
              withLastLocation((KoLAdventure) null));
      try (cleanups) {
        parseCombatData("request/test_fight_win.html");
        assertThat("_cookbookbatQuestMonster", isSetTo("crate"));
        assertThat("_cookbookbatQuestLastLocation", isSetTo("Noob Cave"));
        assertThat("_cookbookbatQuestIngredient", isSetTo("Vegetable of Jarlsberg"));
      }
    }
  }

  @Test
  public void gnomeAdv() {
    var cleanups =
        new Cleanups(
            withFamiliar(FamiliarPool.REAGNIMATED_GNOME),
            withEquipped(Slot.FAMILIAR, ItemPool.GNOMISH_KNEE),
            withFight());
    try (cleanups) {
      assertEquals(0, Preferences.getInteger("_gnomeAdv"));
      parseCombatData("request/test_fight_gnome_adv.html");
      assertEquals(1, Preferences.getInteger("_gnomeAdv"));
    }
  }

  @Test
  public void hareAdv() {
    var cleanups =
        new Cleanups(
            withFamiliar(FamiliarPool.HARE),
            withProperty("_hareCharge", 11),
            withProperty("_hareAdv", 0),
            withFight());
    try (cleanups) {
      parseCombatData("request/test_hare_rollover_adventure.html");
      assertEquals(1, Preferences.getInteger("_hareAdv"));
      assertEquals(0, Preferences.getInteger("_hareCharge"));
    }
  }

  @Nested
  class Gibberer {
    @Test
    public void gibbererAdv() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.GIBBERER),
              withProperty("_gibbererAdv", 0),
              withProperty("_gibbererCharge", 14),
              withLastLocation("Noob Cave"),
              withFight());
      try (cleanups) {
        parseCombatData("request/test_gibberer_rollover_adventure.html");
        assertEquals(1, Preferences.getInteger("_gibbererAdv"));
        assertEquals(0, Preferences.getInteger("_gibbererCharge"));
      }
    }

    @Test
    public void gibbererCharge() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.GIBBERER),
              withProperty("_gibbererCharge", 12),
              withLastLocation("Noob Cave"),
              withFight());
      try (cleanups) {
        parseCombatData("request/test_fight_feel_superior_pvp.html");
        assertEquals(13, Preferences.getInteger("_gibbererCharge"));
      }
    }

    @Test
    public void gibbererChargeUnderwater() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.GIBBERER),
              withProperty("_gibbererCharge", 12),
              withLastLocation("The Ice Hole"),
              withFight());
      try (cleanups) {
        parseCombatData("request/test_fight_feel_superior_pvp.html");
        assertEquals(14, Preferences.getInteger("_gibbererCharge"));
      }
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
              withEquipped(Slot.FAMILIAR, ItemPool.MINIATURE_CRYSTAL_BALL));

      try (cleanups) {
        CrystalBallManager.reset();
        parseCombatData("request/test_fight_crystal_ball_neverending_party.html");
        assertThat("crystalBallPredictions", isSetTo("0:The Neverending Party:party girl"));
      }
    }

    @Test
    public void doesCrystalBallReplaceExistingPrediction() {
      var cleanups =
          new Cleanups(
              withFight(0),
              withProperty("crystalBallPredictions", "0:The Neverending Party:burnout"),
              withCurrentRun(1),
              withLastLocation("The Neverending Party"),
              withFamiliar(FamiliarPool.MOSQUITO),
              withEquipped(Slot.FAMILIAR, ItemPool.MINIATURE_CRYSTAL_BALL));

      try (cleanups) {
        CrystalBallManager.reset();
        parseCombatData("request/test_fight_crystal_ball_neverending_party.html");
        assertThat("crystalBallPredictions", isSetTo("1:The Neverending Party:party girl"));
      }
    }

    @Test
    public void testCrystalBallDoesntOverwriteExistingIdenticalPrediction() {
      var cleanups =
          new Cleanups(
              withFight(0),
              withProperty("crystalBallPredictions", "0:The Neverending Party:party girl"),
              withCurrentRun(1),
              withLastLocation("The Neverending Party"),
              withFamiliar(FamiliarPool.MOSQUITO),
              withEquipped(Slot.FAMILIAR, ItemPool.MINIATURE_CRYSTAL_BALL));

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
              withEquipped(Slot.FAMILIAR, ItemPool.MINIATURE_CRYSTAL_BALL),
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
              withEquipped(Slot.OFFHAND, ItemPool.CURSED_MAGNIFYING_GLASS),
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
              withEquipped(Slot.OFFHAND, ItemPool.CURSED_MAGNIFYING_GLASS),
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
    EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.DAYLIGHT_SHAVINGS_HELMET));
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

  @Nested
  class EvolvingOrganism {
    @Test
    public void resetsFamiliarExperienceWhenEvolves() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              // I trained this familiar from 100 experience to 501 experience
              // in the Cake Shaped Arena. Apparently, it does not evolve there.
              withFamiliar(FamiliarPool.EVOLVING_ORGANISM, 501));
      try (cleanups) {
        client.addResponse(
            302, Map.of("location", List.of("fight.php?ireallymeanit=1677340903")), "");
        client.addResponse(200, html("request/test_fight_evolving_organism_0.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_fight_evolving_organism_1.html"));
        client.addResponse(200, ""); // api.php

        var request = new GenericRequest("adventure.php?snarfblat=443");
        request.run();
        // Honathan has evolved a rather noxious odor.

        var fight = new GenericRequest("fight.php?action=attack");
        fight.run();
        // Honathan expends all their experience and evolves some sort of exothermic process.
        assertThat(KoLCharacter.getFamiliar().getTotalExperience(), is(0));
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

    @Test
    public void canTrackCappedAdventureReprocess() {
      // When 500+ adventures are absorbed in Grey You, monsters will no longer give adventures.
      // In this scenario if an adventure granting monster is reprocessed, no adventures will be
      // given.
      // However, reprocessing is still tracked and can no longer be done on the same monster.
      // As such we'll track it as reprocessed, but not track it as absorbed
      var TOMB_ASP = 469;
      var cleanups =
          new Cleanups(
              withFight(2),
              withFamiliar(FamiliarPool.GREY_GOOSE, 36),
              withPath(Path.GREY_YOU),
              withNextMonster("tomb asp"),
              withProperty("gooseReprocessed", "41,1547"));

      try (cleanups) {
        String urlString = "fight.php?action=skill&whichskill=7408";
        String html = html("request/test_grey_you_capped_adventure_reabsorb.html");

        FightRequest.registerRequest(true, urlString);
        FightRequest.updateCombatData(null, null, html);

        assertThat("gooseReprocessed", isSetTo("41,469,1547"));
        assertThat(GreyYouManager.absorbedMonsters, not(hasItem(TOMB_ASP)));
        assertThat(KoLCharacter.getFamiliar().getWeight(), equalTo(1));
      }

      GreyYouManager.resetAbsorptions();
    }

    @Test
    public void canAcknowledgeCappedAdventureAbsorb() {
      // A capped adventure absorb will always give +10 stat, even if fought before
      // As such it should not be tracked as absorbed
      var TOMB_ASP = 469;
      var cleanups =
          new Cleanups(
              withFight(2),
              withFamiliar(FamiliarPool.GREY_GOOSE, 36),
              withPath(Path.GREY_YOU),
              withNextMonster("tomb asp"));

      try (cleanups) {
        String urlString = "fight.php?action=skill&whichskill=27044";
        String html = html("request/test_grey_you_capped_adventure_absorb.html");

        FightRequest.registerRequest(true, urlString);
        FightRequest.updateCombatData(null, null, html);

        assertThat(GreyYouManager.absorbedMonsters, not(hasItem(TOMB_ASP)));
        assertThat(KoLCharacter.getFamiliar().getWeight(), equalTo(6));
      }

      GreyYouManager.resetAbsorptions();
    }

    @Test
    public void canCountAbsorbedAdventures() {
      var ALBINO_BAT = 41;
      var cleanups =
          new Cleanups(
              withFight(2),
              withFamiliar(FamiliarPool.GREY_GOOSE, 36),
              withPath(Path.GREY_YOU),
              withNextMonster("albino bat"),
              withProperty("_greyYouAdventures", 5));

      try (cleanups) {
        String urlString = "fight.php?action=skill&whichskill=27000";
        String html = html("request/test_fight_goo_absorption_1.html");

        FightRequest.registerRequest(true, urlString);
        FightRequest.updateCombatData(null, null, html);

        assertThat(GreyYouManager.absorbedMonsters, hasItem(ALBINO_BAT));
        assertThat(KoLCharacter.getFamiliar().getWeight(), equalTo(6));
        assertThat("_greyYouAdventures", isSetTo(10));
      }

      GreyYouManager.resetAbsorptions();
    }

    @Test
    public void canCountReabsorbedAdventures() {
      var ALBINO_BAT = 41;
      var cleanups =
          new Cleanups(
              withFight(2),
              withFamiliar(FamiliarPool.GREY_GOOSE, 36),
              withPath(Path.GREY_YOU),
              withNextMonster("albino bat"),
              withProperty("gooseReprocessed", ""),
              withProperty("_greyYouAdventures", 0));

      try (cleanups) {
        var urlString = "fight.php?action=skill&whichskill=7408";
        var html = html("request/test_fight_goo_absorption_2.html");
        FightRequest.registerRequest(true, urlString);
        FightRequest.updateCombatData(null, null, html);

        assertThat(GreyYouManager.absorbedMonsters, hasItem(ALBINO_BAT));
        assertThat("gooseReprocessed", isSetTo(ALBINO_BAT));
        assertThat(KoLCharacter.getFamiliar().getWeight(), equalTo(1));
        assertThat("_greyYouAdventures", isSetTo(5));
      }

      GreyYouManager.resetAbsorptions();
    }
  }

  @Nested
  class CosmicBowlingBall {
    @ParameterizedTest
    @CsvSource({
      // Off in the distance, you hear your cosmic bowling ball rattling around in the ball
      // return system.
      "1, 7, 8",
      "1, 10, 9",
      // You hear your cosmic bowling ball rattling around in the ball return system.
      "2, 9, 7",
      "2, 2, 4",
      // You hear your cosmic bowling ball rattling around in the ball return system nearby.
      "3, 5, 3",
      "3, 1, 2",
      // You hear your cosmic bowling ball approaching.
      "4, 6, 1",
      // Your cosmic bowling ball clatters into the closest ball return and you grab it.
      "5, 10, -1"
    })
    public void canTrackCosmicBowlingBall(int step, int previous, int expected) {
      var cleanups =
          new Cleanups(withProperty("cosmicBowlingBallReturnCombats", previous), withFight(0));

      try (cleanups) {
        String html = html("request/test_fight_bowling_ball_" + step + ".html");
        FightRequest.updateCombatData(null, null, html);
        assertThat("cosmicBowlingBallReturnCombats", isSetTo(expected));
      }
    }

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
    public void canTrackCosmicBowlingBallSkills(int step) {
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
            withEquipped(Slot.WEAPON, ItemPool.JUNE_CLEAVER),
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
            withEquipped(Slot.PANTS, "designer sweatpants"),
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
            withEquipped(Slot.FAMILIAR, "Snow Suit"),
            withProperty("_snowSuitCount", count));

    try (cleanups) {
      // Calculate initial Familiar Weight Modifier
      KoLCharacter.recalculateAdjustments();
      int property = count;
      int expected = 20 - (property / 5);
      int adjustment = KoLCharacter.getFamiliarWeightAdjustment();
      assertEquals(expected, adjustment);
      FightRequest.updateFinalRoundData("", true, false);
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
    var cleanups =
        new Cleanups(
            withEquipped(Slot.OFFHAND, "carnivorous potted plant"),
            withProperty("_carnivorousPottedPlantWins", 0));
    try (cleanups) {
      parseCombatData("request/test_fight_potted_plant.html");
      var text = RequestLoggerOutput.stopStream();
      assertThat(text, containsString("Your potted plant swallows your opponent{s} whole."));
      assertEquals(1, Preferences.getInteger("_carnivorousPottedPlantWins"));
    }
  }

  @Test
  public void canDetectSpiritOfTheMountainsTriggered() {
    RequestLoggerOutput.startStream();
    var cleanups =
        new Cleanups(
            withEffect(EffectPool.SPIRIT_OF_THE_MOUNTAINS),
            withProperty("_spiritOfTheMountainsAdvs", 0));
    try (cleanups) {
      parseCombatData("request/test_fight_spirit_of_the_mountains.html");
      var text = RequestLoggerOutput.stopStream();
      assertThat(text, containsString("Your soul was restored by the fresh mountain air."));
      assertEquals(1, Preferences.getInteger("_spiritOfTheMountainsAdvs"));
    }
  }

  @Test
  public void canDetectCanOfMixedEverythingDrops() {
    RequestLoggerOutput.startStream();
    var cleanups = new Cleanups(withEquipped(Slot.OFFHAND, "can of mixed everything"));
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
  public void canDetectCartography() {
    RequestLoggerOutput.startStream();
    var cleanups = new Cleanups(withSkill(SkillPool.COMPREHENSIVE_CARTOGRAPHY));
    try (cleanups) {
      parseCombatData("request/test_barrow_wraith_win.html");
      var text = RequestLoggerOutput.stopStream();
      assertThat(text, containsString("\"Aroma of Juniper,\" was the label in this region."));
    }
  }

  @Test
  public void canDetectPowerfulGloveCharge() {
    var cleanups =
        new Cleanups(
            withFight(),
            withProperty("_powerfulGloveBatteryPowerUsed", 0),
            withEquipped(Slot.ACCESSORY1, "Powerful Glove"));

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
            withEquipped(Slot.WEAPON, "Fourth of May Cosplay Saber"));

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
            withEquipped(Slot.ACCESSORY1, "Lil' Doctor™ bag"));

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
            withEquipped(Slot.WEAPON, "Industrial Fire Extinguisher"));

    try (cleanups) {
      String html = html("request/test_fight_skill_name_uses_remaining.html");
      FightRequest.parseAvailableCombatSkills(html);

      assertTrue(KoLCharacter.hasCombatSkill(SkillPool.FIRE_EXTINGUISHER__FOAM_EM_UP));
      assertThat("_fireExtinguisherCharge", isSetTo(35));
    }
  }

  @Test
  public void canDetectCinchRemaining() {
    var cleanups =
        new Cleanups(
            withFight(),
            withProperty("_cinchUsed", 0),
            withEquipped(Slot.ACCESSORY1, "Cincho de Mayo"));

    try (cleanups) {
      String html = html("request/test_fight_cincho_uses_remaining.html");
      FightRequest.parseAvailableCombatSkills(html);

      assertThat("_cinchUsed", isSetTo(30));
    }
  }

  @Test
  public void canDetectVampyreCloakeFormUses() {
    var cleanups =
        new Cleanups(
            withFight(),
            withProperty("_vampyreCloakeFormUses", 5),
            withEquipped(Slot.CONTAINER, "Vampyric cloake"));

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
            withEquipped(Slot.PANTS, "Pantsgiving"));

    try (cleanups) {
      String html = html("request/test_fight_skill_name_uses_remaining.html");
      FightRequest.parseAvailableCombatSkills(html);

      assertTrue(KoLCharacter.hasCombatSkill(SkillPool.TALK_ABOUT_POLITICS));
      assertTrue(KoLCharacter.hasCombatSkill(SkillPool.POCKET_CRUMBS));
      assertThat("_pantsgivingBanish", isSetTo(0));
    }
  }

  @Nested
  class SmashGraaagh {
    @Test
    public void canTrackSmashAndGraaaghPickPocketSuccess() {
      var cleanups = new Cleanups(withProperty("_zombieSmashPocketsUsed", 0), withFight());

      try (cleanups) {
        String urlString = "fight.php?action=skill&whichskill=12023";
        String html = html("request/test_fight_smash_and_graaagh_success.html");
        FightRequest.registerRequest(true, urlString);
        FightRequest.updateCombatData(null, null, html);
        assertThat("_zombieSmashPocketsUsed", isSetTo(1));
      }
    }

    @Test
    public void canTrackSmashAndGraaaghPickPocketFailure() {
      var cleanups = new Cleanups(withProperty("_zombieSmashPocketsUsed", 3), withFight());

      try (cleanups) {
        String urlString = "fight.php?action=skill&whichskill=12023";
        String html = html("request/test_fight_smash_and_graaagh_failure.html");
        FightRequest.registerRequest(true, urlString);
        FightRequest.updateCombatData(null, null, html);
        assertThat("_zombieSmashPocketsUsed", isSetTo(3));
      }
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
            withEquipped(Slot.ACCESSORY1, "Backup Camera"));

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
        var fightMods = ModifierDatabase.getModifiers(ModifierType.GENERATED, "fightMods");
        assertThat(fightMods.getDouble(DoubleModifier.ITEMDROP), equalTo(100.0));
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
        var fightMods = ModifierDatabase.getModifiers(ModifierType.GENERATED, "fightMods");
        assertThat(fightMods.getDouble(DoubleModifier.MEATDROP), equalTo(100.0));
      }
    }
  }

  @Nested
  class LoveBugsPreferenceButtonGroupTest {
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
    @Test
    public void doesNotTrackEnvironmentWithoutCMC() {
      var cleanups =
          new Cleanups(
              withWorkshedItem(null),
              withProperty("_coldMedicineConsults", 1),
              withProperty("lastCombatEnvironments", "xxxxxxxxxxxxxxxxxxxx"),
              withLastLocation("Oil Peak"));
      try (cleanups) {
        parseCombatData("request/test_fight_oil_slick.html");
        assertThat("lastCombatEnvironments", isSetTo("xxxxxxxxxxxxxxxxxxxx"));
      }
    }

    @Test
    public void doesNotTrackEnvironmentWithoutConsults() {
      var cleanups =
          new Cleanups(
              withWorkshedItem(ItemPool.COLD_MEDICINE_CABINET),
              withProperty("_coldMedicineConsults", 5),
              withProperty("lastCombatEnvironments", "xxxxxxxxxxxxxxxxxxxx"),
              withLastLocation("Oil Peak"));
      try (cleanups) {
        parseCombatData("request/test_fight_oil_slick.html");
        assertThat("lastCombatEnvironments", isSetTo("xxxxxxxxxxxxxxxxxxxx"));
      }
    }

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
      var cleanups =
          new Cleanups(
              withWorkshedItem(ItemPool.COLD_MEDICINE_CABINET),
              withProperty("_coldMedicineConsults", 1),
              withProperty("lastCombatEnvironments", "xxxxxxxxxxxxxxxxxxxx"),
              withLastLocation(adventureName));
      try (cleanups) {
        // Any old non-free fight from our fixtures
        parseCombatData("request/test_fight_oil_slick.html");
        assertThat("lastCombatEnvironments", isSetTo("xxxxxxxxxxxxxxxxxxx" + environmentSymbol));
      }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "xxxxx", "xxxxxxxxxxxxxxxxxxx"})
    public void canRecoverUndersizedProp(String pref) {
      var cleanups =
          new Cleanups(
              withWorkshedItem(ItemPool.COLD_MEDICINE_CABINET),
              withProperty("_coldMedicineConsults", 1),
              withProperty("lastCombatEnvironments", pref),
              withLastLocation("The Oasis"));
      try (cleanups) {
        // Any old non-free fight from our fixtures
        parseCombatData("request/test_fight_oil_slick.html");
        assertThat("lastCombatEnvironments", isSetTo("xxxxxxxxxxxxxxxxxxxo"));
      }
    }

    @Test
    public void doesNotCountFreeFights() {
      var cleanups =
          new Cleanups(
              withWorkshedItem(ItemPool.COLD_MEDICINE_CABINET),
              withProperty("_coldMedicineConsults", 1),
              withProperty("lastCombatEnvironments", "ioioioioioioioioioio"),
              withLastLocation("Hobopolis Town Square"));
      try (cleanups) {
        // Any old free fight from our fixtures
        parseCombatData("request/test_fight_potted_plant.html");
        assertThat("lastCombatEnvironments", isSetTo("ioioioioioioioioioio"));
      }
    }

    @Test
    public void doesNotCountNonSnarfblats() {
      var cleanups =
          new Cleanups(
              withWorkshedItem(ItemPool.COLD_MEDICINE_CABINET),
              withProperty("_coldMedicineConsults", 1),
              withProperty("lastCombatEnvironments", "ioioioioioioioioioio"),
              withLastLocation("The Typical Tavern Cellar"));
      try (cleanups) {
        // Any old non-free fight from our fixtures
        parseCombatData("request/test_fight_oil_slick.html");
        assertThat("lastCombatEnvironments", isSetTo("ioioioioioioioioioio"));
      }
    }

    @Test
    public void countsNewZonesAsQuestions() {
      var overrideLocation = new KoLAdventure("Override", "adventure.php", "69", "Nice");
      var cleanups =
          new Cleanups(
              withWorkshedItem(ItemPool.COLD_MEDICINE_CABINET),
              withProperty("_coldMedicineConsults", 1),
              withProperty("lastCombatEnvironments", "ioioioioioioioioioio"),
              withLastLocation(overrideLocation));
      try (cleanups) {
        // Any old non-free fight from our fixtures
        parseCombatData("request/test_fight_oil_slick.html");
        assertThat("lastCombatEnvironments", isSetTo("oioioioioioioioioio?"));
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
              withEquipped(Slot.FAMILIAR, "tiny stillsuit"));
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
              withEquipped(Slot.FAMILIAR, "woimbook"),
              withFamiliarInTerrarium(FamiliarPool.PET_ROCK));
      try (cleanups) {
        var rock = KoLCharacter.usableFamiliar(FamiliarPool.PET_ROCK);
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
              withProperty("_pocketProfessorLectures", 4),
              withProperty("_relativityMonster", false));

      try (cleanups) {
        parseCombatData("request/test_fight_meteor_shower_lecture.html");
        assertThat("_pocketProfessorLectures", isSetTo(5));
        assertThat("_relativityMonster", isSetTo(true));
      }
    }
  }

  @Nested
  class GothKid {
    @Test
    public void advancesFightCounters() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.ARTISTIC_GOTH_KID),
              withHippyStoneBroken(),
              withProperty("_gothKidCharge", 1),
              withProperty("_gothKidFights", 1));

      try (cleanups) {
        parseCombatData("request/test_fight_goth_kid_pvp.html");
        assertThat("_gothKidCharge", isSetTo(0));
        assertThat("_gothKidFights", isSetTo(2));
      }
    }

    @Test
    public void doesNotMatchOtherPvPGains() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.ARTISTIC_GOTH_KID),
              withHippyStoneBroken(),
              withProperty("_gothKidCharge", 1),
              withProperty("_gothKidFights", 1));

      try (cleanups) {
        parseCombatData("request/test_fight_feel_superior_pvp.html");
        assertThat("_gothKidCharge", isSetTo(2));
        assertThat("_gothKidFights", isSetTo(1));
      }
    }
  }

  @Nested
  class JurassicParka {
    @Test
    void spikolodonSpikesRecorded() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.SHIRT, ItemPool.JURASSIC_PARKA),
              withProperty("_spikolodonSpikeUses", 0));

      try (cleanups) {
        parseCombatData("request/test_fight_spikolodon_spikes.html");
        assertThat("_spikolodonSpikeUses", isSetTo(1));
      }
    }

    @Test
    void spikodonSpikesSetNCForcerFlag() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.SHIRT, ItemPool.JURASSIC_PARKA),
              withProperty("_spikolodonSpikeUses", 0),
              withProperty("noncombatForcerActive", false));

      try (cleanups) {
        parseCombatData("request/test_fight_spikolodon_spikes.html");
        assertThat("noncombatForcerActive", isSetTo(true));
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"projectile", "confetti", "party"})
  void cinchoCastRecorded(String fileName) {
    var cleanups = new Cleanups(withProperty("_cinchUsed", 90));

    try (cleanups) {
      parseCombatData("request/test_fight_parse_casting_cinch_" + fileName + ".html");
      assertThat("_cinchUsed", isSetTo(95));
    }
  }

  @Nested
  class BottleOfBlankOut {
    @Test
    void canTrackSuccessfulUse() {
      var cleanups =
          new Cleanups(withProperty("blankOutUsed", 1), withItem(ItemPool.GLOB_OF_BLANK_OUT));

      try (cleanups) {
        parseCombatData(
            "request/test_fight_blank_out.html",
            "fight.php?action=useitem&whichitem=4872&whichitem2=0");
        assertThat(ItemPool.GLOB_OF_BLANK_OUT, isInInventory(1));
        assertThat("blankOutUsed", isSetTo(2));
      }
    }

    @Test
    void canTrackSuccessfulUseInAnapests() {
      var cleanups =
          new Cleanups(
              withEffect(EffectPool.JUST_THE_BEST_ANAPESTS),
              withProperty("blankOutUsed", 1),
              withItem(ItemPool.GLOB_OF_BLANK_OUT));

      try (cleanups) {
        parseCombatData(
            "request/test_fight_blank_out_anapests.html",
            "fight.php?action=useitem&whichitem=4872&whichitem2=0");
        assertThat(ItemPool.GLOB_OF_BLANK_OUT, isInInventory(1));
        assertThat("blankOutUsed", isSetTo(2));
      }
    }

    @Test
    void canTrackSuccessfulFinalUse() {
      var cleanups =
          new Cleanups(withProperty("blankOutUsed", 4), withItem(ItemPool.GLOB_OF_BLANK_OUT));

      try (cleanups) {
        parseCombatData(
            "request/test_fight_blank_out_finished.html",
            "fight.php?action=useitem&whichitem=4872&whichitem2=0");
        assertThat(ItemPool.GLOB_OF_BLANK_OUT, isInInventory(0));
        assertThat("blankOutUsed", isSetTo(0));
      }
    }

    @Test
    void canTrackSuccessfulFinalUseInAnapests() {
      var cleanups =
          new Cleanups(
              withEffect(EffectPool.JUST_THE_BEST_ANAPESTS),
              withProperty("blankOutUsed", 4),
              withItem(ItemPool.GLOB_OF_BLANK_OUT));

      try (cleanups) {
        parseCombatData(
            "request/test_fight_blank_out_anapests.html",
            "fight.php?action=useitem&whichitem=4872&whichitem2=0");
        assertThat(ItemPool.GLOB_OF_BLANK_OUT, isInInventory(0));
        assertThat("blankOutUsed", isSetTo(0));
      }
    }
  }

  @ParameterizedTest
  @CsvSource({
    "win, fight.php?action=skill&whichskill=1005, true, false",
    "lose, fight.php?action=useitem&whichitem=9963&whichitem2=0, false, true",
    "expire, fight.php?action=useitem&whichitem=2, false, true",
    "run, fight.php?action=runaway, false, false"
  })
  void setsLastFightProperty(String html, String action, boolean win, boolean lose) {
    var cleanups = new Cleanups(withProperty("_lastCombatWon"), withProperty("_lastCombatLost"));

    try (cleanups) {
      parseCombatData("request/test_fight_" + html + ".html", action);
      assertThat("_lastCombatWon", isSetTo(win));
      assertThat("_lastCombatLost", isSetTo(lose));
    }
  }

  @Nested
  class Autumnaton {
    @ParameterizedTest
    @CsvSource({
      "away_1, 29, The Fun-Guy Mansion",
      "away_2, 30, The Fun-Guy Mansion",
      "finished, 1, ''",
      "same_location, 2, The Fun-Guy Mansion",
      "period_in_name, 45, St. Patrick's Day Island"
    })
    public void canUpdateQuestParamsFromFightInfo(
        final String fixture, final int questTurn, final String questLocation) {
      var cleanups =
          new Cleanups(
              withTurnsPlayed(1),
              withProperty("autumnatonQuestTurn", 5),
              withProperty("autumnatonQuestLocation", "The Spooky Forest"));

      try (cleanups) {
        parseCombatData(
            "request/test_fight_autumnaton_" + fixture + ".html", "fight.php?action=attack");
        assertThat("autumnatonQuestTurn", isSetTo(questTurn));
        assertThat("autumnatonQuestLocation", isSetTo(questLocation));
      }
    }
  }

  @Nested
  class InvalidAttacks {
    @Test
    public void validAttack() {
      assertFalse(FightRequest.isInvalidAttack("skill Clobber"));
    }

    @Test
    public void shieldbuttIsValidWithShield() {
      var cleanups = withEquipped(Slot.OFFHAND, "vinyl shield");

      try (cleanups) {
        assertFalse(FightRequest.isInvalidAttack("skill Shieldbutt"));
      }
    }

    @Test
    public void shieldbuttIsInvalidWithoutShield() {
      assertTrue(FightRequest.isInvalidAttack("skill Shieldbutt"));
    }

    @Test
    public void summonLeviIsValidUnderWater() {
      var cleanups = withLastLocation("The Ice Hole");

      try (cleanups) {
        assertFalse(FightRequest.isInvalidAttack("skill Summon Leviatuga"));
      }
    }

    @Test
    public void summonLeviIsInvalidAboveWater() {
      var cleanups = withLastLocation("Noob Cave");

      try (cleanups) {
        assertTrue(FightRequest.isInvalidAttack("skill Summon Leviatuga"));
      }
    }
  }

  @Nested
  class Speakeasy {
    @BeforeEach
    public void beforeEach() {
      Preferences.resetToDefault("_speakeasyFreeFights");
    }

    @Test
    public void speakeasyFreeFights() {
      var cleanups = withLastLocation("An Unusually Quiet Barroom Brawl");
      try (cleanups) {
        parseCombatData("request/test_oliver_free.html");
        assertEquals(Preferences.getInteger("_speakeasyFreeFights"), 1);
      }
    }

    @Test
    public void speakeasyHeatingUp() {
      var cleanups = withLastLocation("An Unusually Quiet Barroom Brawl");
      try (cleanups) {
        parseCombatData("request/test_oliver_heating_up.html");
        assertEquals(Preferences.getInteger("_speakeasyFreeFights"), 3);
      }
    }

    @Test
    public void speakeasyNotFree() {
      var cleanups = withLastLocation("An Unusually Quiet Barroom Brawl");
      try (cleanups) {
        parseCombatData("request/test_oliver_not_free.html");
        assertEquals(Preferences.getInteger("_speakeasyFreeFights"), 0);
      }
    }
  }

  @Test
  public void loseInitiativeAndLoseLittleRoundPebble() {
    var cleanups =
        new Cleanups(withEquipped(Slot.OFFHAND, ItemPool.LITTLE_ROUND_PEBBLE), withFight());
    try (cleanups) {
      parseCombatData("request/test_fight_little_round_pebble.html");
      assertFalse(KoLCharacter.hasEquipped(ItemPool.LITTLE_ROUND_PEBBLE));
    }
  }

  @Nested
  class Camel {
    @Test
    public void sloshingSetsSpitToFull() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.MELODRAMEDARY), withProperty("camelSpit", 0), withFight());
      try (cleanups) {
        parseCombatData("request/test_melodramedary_sloshing.html");
        assertThat("camelSpit", isSetTo(100));
      }
    }
  }

  @Nested
  class ShadowRift {
    @Test
    void canTrackShadowRiftCombats() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("_shadowRiftCombats", 0),
              withProperty("shadowRiftIngress", ""),
              withLastLocation("None"));
      try (cleanups) {
        client.addResponse(
            302,
            Map.of("location", List.of("adventure.php?snarfblat=" + AdventurePool.SHADOW_RIFT)),
            "");
        client.addResponse(
            302, Map.of("location", List.of("fight.php?ireallymeanit=1677340903")), "");
        client.addResponse(200, html("request/test_shadow_rift_fight.html"));
        client.addResponse(200, ""); // api.php

        var request = new GenericRequest("place.php?whichplace=hiddencity&action=hc_shadowrift");
        request.run();
        assertThat("shadowRiftIngress", isSetTo("hiddencity"));
        assertThat("lastAdventure", isSetTo("Shadow Rift (The Hidden City)"));
        assertThat("_shadowRiftCombats", isSetTo(1));
      }
    }
  }

  @Nested
  class DreadConsumables {
    @ParameterizedTest
    @CsvSource({
      "getsYouDrunkTurnsLeft, gets_you_drunk",
      "ghostPepperTurnsLeft, ghost_pepper",
    })
    public void survivingDecrementsCounter(String pref, String fixture) {
      var cleanups = new Cleanups(withProperty(pref, 4), withHP(5000, 5000, 5000));
      try (cleanups) {
        parseCombatData("request/test_fight_" + fixture + "_survive.html");
        assertThat(pref, isSetTo(3));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "getsYouDrunkTurnsLeft, gets_you_drunk_elemental_form",
      "ghostPepperTurnsLeft, ghost_pepper_elemental_form",
      "getsYouDrunkTurnsLeft, gets_you_drunk_complete",
      "ghostPepperTurnsLeft, ghost_pepper_complete",
    })
    public void otherSituationsEndCounter(String pref, String fixture) {
      var cleanups = new Cleanups(withProperty(pref, 4));
      try (cleanups) {
        parseCombatData("request/test_fight_" + fixture + ".html");
        assertThat(pref, isSetTo(0));
      }
    }
  }

  @Nested
  class SpookyVHSTape {
    @Test
    public void canRecogniseSpookyVHSTapeMonster() {
      var cleanups =
          new Cleanups(
              withProperty("spookyVHSTapeMonster", "ghost"),
              withProperty("spookyVHSTapeMonsterTurn", "119"),
              withTurnsPlayed(111),
              withFight(0),
              withCounter(8, "Spooky VHS Tape Monster type=wander", "watch.gif"),
              withCounter(
                  0,
                  "Spooky VHS Tape unknown monster window begin loc=* type=wander",
                  "lparen.gif"),
              withCounter(
                  8, "Spooky VHS Tape unknown monster window end loc=* type=wander", "rparen.gif"));

      try (cleanups) {
        String html = html("request/test_fight_spooky_vhs_tape_monster.html");
        FightRequest.updateCombatData(null, null, html);
        assertThat("spookyVHSTapeMonster", isSetTo(""));
        assertThat(TurnCounter.isCounting("Spooky VHS Tape Monster"), is(false));
        assertThat(
            TurnCounter.isCounting("Spooky VHS Tape unknown monster window begin"), is(false));
        assertThat(TurnCounter.isCounting("Spooky VHS Tape unknown monster window end"), is(false));
      }
    }

    @Test
    public void canTrackSpookyVHSTapeSuccess() {
      var cleanups =
          new Cleanups(
              withoutCounters(),
              withProperty("spookyVHSTapeMonster"),
              withProperty("spookyVHSTapeMonsterTurn"),
              withTurnsPlayed(111),
              withItem(ItemPool.SPOOKY_VHS_TAPE),
              withFight(1));

      try (cleanups) {
        String html = html("request/test_fight_spooky_vhs_tape_success.html");
        FightRequest.registerRequest(true, "fight.php?action=useitem&whichitem=11270");
        FightRequest.updateCombatData(null, null, html);
        assertThat("Spooky VHS Tape", not(isInInventory()));
        assertThat("spookyVHSTapeMonster", isSetTo("ghost"));
        assertThat("spookyVHSTapeMonsterTurn", isSetTo(111));
        assertThat(TurnCounter.isCounting("Spooky VHS Tape Monster"), is(true));
      }
    }

    @Test
    public void canTrackSpookyVHSTapeFailure() {
      var cleanups =
          new Cleanups(
              withoutCounters(),
              withProperty("spookyVHSTapeMonster"),
              withProperty("spookyVHSTapeMonsterTurn"),
              withTurnsPlayed(111),
              withItem(ItemPool.SPOOKY_VHS_TAPE),
              withFight(1));

      try (cleanups) {
        String html = html("request/test_fight_spooky_vhs_tape_failure.html");
        FightRequest.registerRequest(true, "fight.php?action=useitem&whichitem=11270");
        FightRequest.updateCombatData(null, null, html);
        assertThat("Spooky VHS Tape", isInInventory());
        assertThat("spookyVHSTapeMonster", isSetTo(""));
        assertThat("spookyVHSTapeMonsterTurn", isSetTo(-1));
        assertThat(
            TurnCounter.isCounting("Spooky VHS Tape unknown monster window begin"), is(true));
        assertThat(TurnCounter.isCounting("Spooky VHS Tape unknown monster window end"), is(true));
      }
    }
  }

  @Test
  public void canDetectFludaUse() {
    var cleanups =
        new Cleanups(
            withFight(),
            withProperty("_douseFoeUses", 2),
            withEquipped(Slot.ACCESSORY1, "Flash Liquidizer Ultra Dousing Accessory"));

    try (cleanups) {
      parseCombatData(
          "request/test_fight_douse_foe.html", "fight.php?action=skill&whichskill=7448");

      assertThat("_douseFoeUses", isSetTo(3));
    }
  }

  @Test
  public void canDetectMcTwist() {
    var cleanups =
        new Cleanups(
            withFight(),
            withProperty("_epicMcTwistUsed", false),
            withEquipped(Slot.ACCESSORY1, "pro skateboard"));

    try (cleanups) {
      parseCombatData(
          "request/test_fight_epic_mctwist.html", "fight.php?action=skill&whichskill=7447");

      assertThat("_epicMcTwistUsed", isSetTo(true));
    }
  }

  @Nested
  class RedWhiteBlueBlast {
    @Test
    public void canDetect() {
      var cleanups =
          new Cleanups(
              withFight(),
              withProperty("rwbMonster"),
              withProperty("rwbMonsterCount"),
              withProperty("rwbLocation"),
              withLastLocation("South of the Border"));

      try (cleanups) {
        parseCombatData(
            "request/test_fight_red_white_blue.html", "fight.php?action=skill&whichskill=7450");

        assertThat("rwbMonster", isSetTo("raging bull"));
        assertThat("rwbMonsterCount", isSetTo(2));
        assertThat("rwbLocation", isSetTo("South of the Border"));
      }
    }

    @Test
    public void canDetectMonsterAfterCast() {
      var cleanups =
          new Cleanups(
              withFight(0),
              withProperty("rwbMonster", "raging bull"),
              withProperty("rwbMonsterCount", 2),
              withProperty("rwbLocation", "South of the Border"),
              withNextMonster("raging bull"));

      try (cleanups) {
        parseCombatData("request/test_fight_red_white_blue_after.html");

        assertThat("rwbMonster", isSetTo("raging bull"));
        assertThat("rwbMonsterCount", isSetTo(1));
      }
    }
  }

  @Test
  public void canDetectEagleScreech() {
    RequestLoggerOutput.startStream();
    var cleanups =
        new Cleanups(
            withFight(),
            withBanishedPhyla(""),
            withProperty("screechCombats"),
            withFamiliar(FamiliarPool.PATRIOTIC_EAGLE));

    try (cleanups) {
      parseCombatData(
          "request/test_fight_eagle_screech.html", "fight.php?action=skill&whichskill=7451");

      assertThat("screechCombats", isSetTo(11));
      assertThat("banishedPhyla", hasStringValue(startsWith("beast:Patriotic Screech:")));

      var text = RequestLoggerOutput.stopStream();
      assertThat(text, not(containsString("fire a Red, White and Blue Blast (0 Mojo Points)")));
    }
  }

  @Nested
  class Eagle {
    @ParameterizedTest
    @ValueSource(strings = {"", "_2"})
    public void screechTimerAdvances(String extension) {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.PATRIOTIC_EAGLE),
              withProperty("screechCombats", 6),
              withFight());
      try (cleanups) {
        parseCombatData("request/test_fight_eagle_screech_after" + extension + ".html");
        assertThat("screechCombats", isSetTo(5));
      }
    }

    @Test
    public void screechTimerEnds() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.PATRIOTIC_EAGLE),
              withProperty("screechCombats", 6),
              withFight());
      try (cleanups) {
        parseCombatData("request/test_fight_eagle_screech_after_done.html");
        assertThat("screechCombats", isSetTo(0));
      }
    }
  }

  @Nested
  class RecallFactsHabitats {
    @Test
    public void canDetectCast() {
      var cleanups =
          new Cleanups(
              withFight(),
              withProperty("_monsterHabitatsRecalled", 1),
              withProperty("_monsterHabitatsFightsLeft", 0),
              withProperty("_monsterHabitatsMonster", ""));

      try (cleanups) {
        parseCombatData(
            "request/test_fight_recall_habitat.html", "fight.php?action=skill&whichskill=7485");

        assertThat("_monsterHabitatsRecalled", isSetTo(2));
        assertThat("_monsterHabitatsFightsLeft", isSetTo(5));
        assertThat("_monsterHabitatsMonster", isSetTo("Knob Goblin Embezzler"));
      }
    }

    @Test
    public void canDetectNewEncounter() {
      var cleanups =
          new Cleanups(
              withFight(0),
              withProperty("_monsterHabitatsFightsLeft", 4),
              withProperty("_monsterHabitatsMonster", "Knob Goblin Embezzler"));

      try (cleanups) {
        String html = html("request/test_fight_recall_habitat_adv.html");
        FightRequest.updateCombatData(null, null, html);
        assertThat("_monsterHabitatsFightsLeft", isSetTo(3));
      }
    }
  }

  @Nested
  class RecallFactsCircadian {
    @Test
    public void canDetectCast() {
      var cleanups = new Cleanups(withFight(), withProperty("_circadianRhythmsRecalled", false));

      try (cleanups) {
        parseCombatData(
            "request/test_fight_recall_circadian.html", "fight.php?action=skill&whichskill=7486");

        assertThat("_circadianRhythmsRecalled", isSetTo(true));
      }
    }

    @Test
    public void canDetectAdventureGain() {
      var cleanups =
          new Cleanups(
              withFight(),
              withProperty("_circadianRhythmsRecalled", true),
              withProperty("_circadianRhythmsAdventures", 3));

      try (cleanups) {
        parseCombatData("request/test_fight_recall_circadian_adv.html", "fight.php?action=attack");

        assertThat("_circadianRhythmsAdventures", isSetTo(4));
      }
    }
  }

  @Nested
  class JustTheFacts {
    @Test
    void canDetectFactsDrops() {
      RequestLoggerOutput.startStream();
      var cleanups = new Cleanups(withSkill(SkillPool.JUST_THE_FACTS));
      try (cleanups) {
        parseCombatData("request/test_fight_recall_circadian_adv.html");
        var text = RequestLoggerOutput.stopStream();
        assertThat(
            text,
            containsString(
                "These monsters have vestigial organ that collects things they can't digest.\nYou acquire an item: foon"));
        assertThat(text, containsString("sleep a bit better tonight"));
      }
    }

    @Test
    void doesNotLogCircadianFailures() {
      RequestLoggerOutput.startStream();
      var cleanups =
          new Cleanups(
              withSkill(SkillPool.JUST_THE_FACTS),
              withEffect(EffectPool.RECALLING_CIRCADIAN_RHYTHMS));
      try (cleanups) {
        parseCombatData("request/test_fight_recall_circadian_wrong_monster.html");
        var text = RequestLoggerOutput.stopStream();
        assertThat(text, not(containsString("rythm")));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "true, 1, 2",
      "false, 1, 11",
    })
    void tracksTatterDrop(final boolean success, final int current, final int next) {
      var cleanups =
          new Cleanups(
              withClass(AscensionClass.PASTAMANCER),
              withPath(Path.NONE),
              withNextMonster("Sorority Nurse"),
              withSkill(SkillPool.JUST_THE_FACTS),
              withProperty("_bookOfFactsTatters", current),
              withFight());
      try (cleanups) {
        parseCombatData(
            "request/test_fight_book_of_facts_tatter_"
                + (success ? "success" : "fallback")
                + ".html");
        assertThat("_bookOfFactsTatters", isSetTo(next));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "true, 1, 2",
      "false, 1, 3",
    })
    void tracksWishDrop(final boolean success, final int current, final int next) {
      var cleanups =
          new Cleanups(
              withClass(AscensionClass.SEAL_CLUBBER),
              withPath(Path.CRAZY_RANDOM_SUMMER),
              withNextMonster("Keese"),
              withSkill(SkillPool.JUST_THE_FACTS),
              withProperty("_bookOfFactsWishes", current),
              withFight());
      try (cleanups) {
        parseCombatData(
            "request/test_fight_book_of_facts_pocket_wish_"
                + (success ? "success" : "fallback")
                + ".html");
        assertThat("_bookOfFactsWishes", isSetTo(next));
      }
    }

    @Test
    void doesNotTrackWishDropAfterMonsterReplacement() {
      var cleanups =
          new Cleanups(
              withClass(AscensionClass.SAUCEROR),
              withPath(Path.NONE),
              withNextMonster("chalkdust wraith"),
              withSkill(SkillPool.JUST_THE_FACTS),
              withProperty("_bookOfFactsWishes", 0),
              withFight(0));
      try (cleanups) {
        parseCombatData("request/test_fight_book_of_facts_backup.html");
        assertThat("_bookOfFactsWishes", isSetTo(0));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "true, 3, 1",
      "false, 1, 2",
      "false, 3, 0",
    })
    void tracksGummiEffect(final boolean success, final int current, final int next) {
      var cleanups =
          new Cleanups(
              withClass(AscensionClass.PASTAMANCER),
              withPath(Path.NONE),
              withNextMonster("fiendish can of asparagus"),
              withSkill(SkillPool.JUST_THE_FACTS),
              withProperty("bookOfFactsGummi", current),
              withFight());
      try (cleanups) {
        parseCombatData(
            "request/test_fight_book_of_facts_gummi_"
                + (success ? "success" : "fallback")
                + ".html");
        assertThat("bookOfFactsGummi", isSetTo(next));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "true, 1, 1",
      "true, 0, 1",
      "false, 1, 0",
    })
    void tracksPinataEffect(final boolean success, final int current, final int next) {
      var cleanups =
          new Cleanups(
              withClass(AscensionClass.PASTAMANCER),
              withPath(Path.NONE),
              withNextMonster("axe handle"),
              withSkill(SkillPool.JUST_THE_FACTS),
              withProperty("bookOfFactsPinata", current),
              withFight());
      try (cleanups) {
        parseCombatData(
            "request/test_fight_book_of_facts_pinata_"
                + (success ? "success" : "fallback")
                + ".html");
        assertThat("bookOfFactsPinata", isSetTo(next));
      }
    }

    @Test
    void circadianRhythmsDoesNotBreakWishTracking() {
      var cleanups =
          new Cleanups(
              withClass(AscensionClass.TURTLE_TAMER),
              withPath(Path.NONE),
              withNextMonster("Furry Giant"),
              withSkill(SkillPool.JUST_THE_FACTS),
              withProperty("_bookOfFactsWishes", 1),
              withFight());
      try (cleanups) {
        parseCombatData("request/test_fight_book_of_facts_rhythms_and_wish.html");
        assertThat("_bookOfFactsWishes", isSetTo(2));
      }
    }
  }

  @Nested
  class Yachtzee {
    @Test
    void canTrackPartyYachtCombats() {
      var cleanups =
          new Cleanups(
              withFight(),
              withLastLocation("The Sunken Party Yacht"),
              withProperty("encountersUntilYachtzeeChoice", 20));

      try (cleanups) {
        parseCombatData("request/test_party_yacht_fight.html");
        assertThat("encountersUntilYachtzeeChoice", isSetTo(19));
      }
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void rainmanMonstersOnlyStartCounterInPath(final boolean inRaincore) {
    var cleanups = new Cleanups(withFight(0), withNextMonster("alley catfish"), withoutCounters());

    if (inRaincore) {
      cleanups.add(withPath(Path.HEAVY_RAINS));
    }

    try (cleanups) {
      parseCombatData("request/test_fight_alley_catfish.html");
      assertThat(TurnCounter.isCounting("Rain Monster window begin"), is(inRaincore));
      assertThat(TurnCounter.isCounting("Rain Monster window end"), is(inRaincore));
    }
  }

  @Test
  public void crimbuccaneerScoreIsNotDamage() {
    var cleanups = new Cleanups(withFight(0));
    try (cleanups) {
      assertEquals(0, InventoryManager.getAccessibleCount(ItemPool.ELF_ARMY_MACHINE_PARTS));
      parseCombatData("request/test_fight_crimbo23.html");
      assertEquals(3, InventoryManager.getAccessibleCount(ItemPool.ELF_ARMY_MACHINE_PARTS));
    }
  }

  @Test
  void canTrackSuccessfulPrankCardUse() {
    var cleanups =
        new Cleanups(withProperty("_prankCardMonster"), withItem(ItemPool.PRANK_CRIMBO_CARD));

    try (cleanups) {
      parseCombatData(
          "request/test_fight_elf_crimbo_card.html",
          "fight.php?action=useitem&whichitem=11487&whichitem2=0");
      assertThat("_prankCardMonster", isSetTo("Elf Guard engineer"));
    }
  }

  @Test
  void canTrackSuccessfulTrickCoinUse() {
    var cleanups = new Cleanups(withProperty("_trickCoinMonster"), withItem(ItemPool.TRICK_COIN));

    try (cleanups) {
      parseCombatData(
          "request/test_fight_pirate_crimbo_coin.html",
          "fight.php?action=useitem&whichitem=11480&whichitem2=0");
      assertThat("_trickCoinMonster", isSetTo("Crimbuccaneer mudlark"));
    }
  }

  @Nested
  class CandyCaneSkills {
    @Test
    public void canTrackSurprisinglySweetSlash() {
      var cleanups = new Cleanups(withProperty("_surprisinglySweetSlashUsed", 0), withFight());

      try (cleanups) {
        String urlString = "fight.php?action=skill&whichskill=7488";
        String html = html("request/test_fight_surprisingly_sweet_slash.html");
        FightRequest.registerRequest(true, urlString);
        FightRequest.updateCombatData(null, null, html);
        assertThat("_surprisinglySweetSlashUsed", isSetTo(1));
      }
    }

    @Test
    public void canTrackSurprisinglySweetStab() {
      var cleanups = new Cleanups(withProperty("_surprisinglySweetStabUsed", 0), withFight());

      try (cleanups) {
        String urlString = "fight.php?action=skill&whichskill=7489";
        String html = html("request/test_fight_surprisingly_sweet_stab.html");
        FightRequest.registerRequest(true, urlString);
        FightRequest.updateCombatData(null, null, html);
        assertThat("_surprisinglySweetStabUsed", isSetTo(1));
      }
    }
  }

  @Test
  public void canTrackMimicEggLay() {
    var cleanups =
        new Cleanups(
            withProperty("mimicEggMonsters", ""),
            withProperty("_mimicEggsObtained", 0),
            withFight());

    try (cleanups) {
      String urlString = "fight.php?action=skill&whichskill=7494";
      String html = html("request/test_fight_lay_mimic_egg_success.html");
      FightRequest.registerRequest(true, urlString);
      FightRequest.updateCombatData(null, null, html);
      assertThat("_mimicEggsObtained", isSetTo(1));
      assertThat("mimicEggMonsters", isSetTo("2409:1"));
    }
  }

  @Test
  public void canDetectSpringBootsBanish() {
    var cleanups = new Cleanups(withFight(), withBanishedMonsters(""));

    try (cleanups) {
      parseCombatData(
          "request/test_fight_spring_boots_banish.html", "fight.php?action=skill&whichskill=7501");

      assertThat("banishedMonsters", hasStringValue(startsWith("fluffy bunny:Spring Kick:")));
    }
  }

  @Nested
  class ResearchPoints {
    @Test
    public void initialResearchIsDetected() {
      var cleanups =
          new Cleanups(
              withPath(Path.WEREPROFESSOR),
              withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR),
              withProperty("wereProfessorResearchPoints", 11),
              withProperty("wereProfessorAdvancedResearch", "1000,10,30,20"),
              withFight(0));
      try (cleanups) {
        String html = html("request/test_fight_research_initial.html");
        String url = "fight.php?ireallymeanit=1709453567";
        FightRequest.registerRequest(true, url);
        FightRequest.processResults(null, null, html);
        assertThat("wereProfessorResearchPoints", isSetTo(12));
        assertThat("wereProfessorAdvancedResearch", isSetTo("1000,10,30,20"));
      }
    }

    @Test
    public void advancedResearchSuccessIsDetected() {
      var cleanups =
          new Cleanups(
              withPath(Path.WEREPROFESSOR),
              withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR),
              withProperty("wereProfessorResearchPoints", 11),
              withProperty("wereProfessorAdvancedResearch", "1000,10,30,20"),
              withFight(1));
      try (cleanups) {
        String html = html("request/test_fight_research_advanced_success.html");
        String url = "fight.php?whichskill=7512&action=skill";
        FightRequest.registerRequest(true, url);
        FightRequest.processResults(null, null, html);
        assertThat("wereProfessorResearchPoints", isSetTo(21));
        assertThat("wereProfessorAdvancedResearch", isSetTo("10,20,30,539,1000"));
      }
    }

    @Test
    public void advancedResearchFailureIsDetected() {
      var cleanups =
          new Cleanups(
              withPath(Path.WEREPROFESSOR),
              withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR),
              withProperty("wereProfessorResearchPoints", 11),
              withProperty("wereProfessorAdvancedResearch", "1000,10,30,20"),
              withFight(1));
      try (cleanups) {
        String html = html("request/test_fight_research_advanced_failed.html");
        String url = "fight.php?whichskill=7512&action=skill";
        FightRequest.registerRequest(true, url);
        FightRequest.processResults(null, null, html);
        assertThat("wereProfessorResearchPoints", isSetTo(11));
        assertThat("wereProfessorAdvancedResearch", isSetTo("10,20,30,539,1000"));
      }
    }
  }

  @Nested
  class DartHolster {
    @ParameterizedTest
    @CsvSource({
      "six_no_duplicates, junksprite hubcap bender, arm;wing;leg;head;butt;torso",
      "six_with_duplicates, fruit golem, watermelon;butt;grapes;grapefruit;banana;watermelon",
      "four_no_duplicates, skullbat, head;wing;butt;torso",
      "four_with_duplicates, spectral jellyfish, tentacle;head;butt;tentacle"
    })
    public void canParseDartboard(String file, String monsterName, String partNames) {
      var cleanups = new Cleanups(withProperty("_currentDartboard", ""), withFight(0));
      try (cleanups) {
        String html = html("request/test_fight_darts_" + file + ".html");

        // Derive expected skills

        // These are the partnames as they are presented in the
        // particular responseText. KoL seems to start counting them
        // with skill 7513, although duplicate part names will be
        // assigned the same (earlier assigned) skill number.

        String[] parts = partNames.split("\\s*;\\s*");

        Map<String, Integer> partMap = new HashMap<>();
        // Assume/require that the property is sorted by skill ID
        Map<Integer, String> skillMap = new TreeMap<>();
        int skillId = 7513;

        for (String part : parts) {
          if (!partMap.containsKey(part)) {
            partMap.put(part, skillId);
            skillMap.put(skillId++, part);
          }
        }

        String expected =
            skillMap.entrySet().stream()
                .map(e -> String.valueOf(e.getKey()) + ":" + e.getValue())
                .collect(Collectors.joining(","));

        String url = "fight.php?ireallymeanit=1710016436";
        FightRequest.registerRequest(true, url);
        FightRequest.processResults(null, null, html);

        assertThat("_currentDartboard", isSetTo(expected));
      }
    }

    @Test
    public void canDetectDartsThrown() {
      var cleanups = new Cleanups(withFight(), withProperty("dartsThrown", 16));

      try (cleanups) {
        parseCombatData("request/test_fight_dart.html", "fight.php?action=skill&whichskill=7516");

        assertThat("dartsThrown", hasIntegerValue(equalTo(17)));
      }
    }
  }

  @Nested
  class TearawayPants {
    @Test
    public void canTrackPlantAdventures() {
      var cleanups = new Cleanups(withProperty("_tearawayPantsAdvs", 0), withFight());

      try (cleanups) {
        String urlString = "fight.php?action=skill&whichskill=7527";
        String html = html("request/test_fight_tearaway_gain_adv.html");
        FightRequest.registerRequest(true, urlString);
        FightRequest.updateCombatData(null, null, html);
        assertThat("_tearawayPantsAdvs", isSetTo(1));
      }
    }

    @Test
    public void canTrackItemDropImprovement() {
      var cleanups = new Cleanups(withFight());

      try (cleanups) {
        String urlString = "fight.php?action=skill&whichskill=7527";
        String html = html("request/test_fight_tearaway_itemdrop.html");
        FightRequest.registerRequest(true, urlString);
        FightRequest.updateCombatData(null, null, html);
        var fightMods = ModifierDatabase.getModifiers(ModifierType.GENERATED, "fightMods");
        assertThat(fightMods.getDouble(DoubleModifier.ITEMDROP), equalTo(15.0));
      }
    }
  }

  @Test
  public void canDetectThrowinEmberBanish() {
    var cleanups = new Cleanups(withFight(), withBanishedMonsters(""));

    try (cleanups) {
      parseCombatData(
          "request/test_fight_throwin_ember.html",
          "fight.php?action=useitem&whichitem=11652&whichitem2=8489");

      assertThat("banishedMonsters", hasStringValue(startsWith("spooky mummy:throwin' ember:")));
    }
  }

  @Nested
  class BurlyBodyguard {
    @ParameterizedTest
    @CsvSource({
      "request/test_fight_win.html",
      "request/test_fight_lose.html",
      "request/test_fight_run.html"
    })
    void tracksCharge(String file) {
      try (var cleanups =
          new Cleanups(
              withPath(Path.AVANT_GUARD),
              withFamiliar(FamiliarPool.BURLY_BODYGUARD),
              withProperty("bodyguardCharge", 0),
              withFight())) {
        parseCombatData(file);
        assertThat("bodyguardCharge", isSetTo(1));
      }
    }

    @Test
    void tracksReady() {
      try (var cleanups =
          new Cleanups(
              withPath(Path.AVANT_GUARD),
              withFamiliar(FamiliarPool.BURLY_BODYGUARD),
              withProperty("bodyguardCharge", 10),
              withFight())) {
        parseCombatData("request/test_fight_bodyguard_ready.html");
        assertThat("bodyguardCharge", isSetTo(50));
      }
    }

    @Test
    void resetsChattedBodyguardOnEncounter() {
      try (var cleanups =
          new Cleanups(
              withPath(Path.AVANT_GUARD),
              withFamiliar(FamiliarPool.BURLY_BODYGUARD),
              withProperty("bodyguardChatMonster", "pygmy witch accountant"),
              withFight(0))) {
        parseCombatData("request/test_fight_bodyguard_pwa.html");
        assertThat("bodyguardChatMonster", isSetTo(""));
      }
    }

    @Test
    void recordsBonusApplied() {
      var cleanups =
          new Cleanups(
              withProperty("burlyBodyguardReceivedBonus", false),
              withFamiliar(FamiliarPool.BURLY_BODYGUARD),
              withFight());
      try (cleanups) {
        parseCombatData("request/test_fight_burly_bodyguard_bonus.html");
        assertThat("burlyBodyguardReceivedBonus", isSetTo(true));
      }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6, 11})
    void addsExperienceFromBonus(final int points) {
      var cleanups =
          new Cleanups(
              withProperty("avantGuardPoints", points),
              withProperty("burlyBodyguardReceivedBonus", false),
              withFamiliar(FamiliarPool.BURLY_BODYGUARD, 0),
              withFight());
      try (cleanups) {
        parseCombatData("request/test_fight_burly_bodyguard_bonus.html");
        var fam = KoLCharacter.getEffectiveFamiliar();
        assertThat(fam.getTotalExperience(), is((int) Math.pow(points, 2) + 1));
      }
    }
  }

  @Nested
  class BatWings {
    @Test
    public void canDetectBatWingsWins() {
      RequestLoggerOutput.startStream();
      var cleanups =
          new Cleanups(
              withEquipped(Slot.CONTAINER, ItemPool.BAT_WINGS),
              withProperty("_batWingsFreeFights", 0));
      try (cleanups) {
        parseCombatData("request/test_fight_bat_wings_free.html");
        var text = RequestLoggerOutput.stopStream();
        assertThat(text, containsString("You flap your bat wings gustily"));
        assertEquals(1, Preferences.getInteger("_batWingsFreeFights"));
      }
    }

    @Test
    void swoopRecorded() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.CONTAINER, ItemPool.BAT_WINGS),
              withProperty("_batWingsSwoopUsed", 0),
              withFight());

      try (cleanups) {
        parseCombatData(
            "request/test_fight_bat_wings_swoop.html", "fight.php?action=skill&whichskill=7530");
        assertThat("_batWingsSwoopUsed", isSetTo(1));
      }
    }

    @Test
    void cauldronRecorded() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.CONTAINER, ItemPool.BAT_WINGS),
              withProperty("_batWingsCauldronUsed", 0),
              withFight());

      try (cleanups) {
        parseCombatData(
            "request/test_fight_bat_wings_cauldron.html", "fight.php?action=skill&whichskill=7531");
        assertThat("_batWingsCauldronUsed", isSetTo(1));
      }
    }
  }

  @Nested
  class Authority {
    @Test
    void canDetectAssertAuthority() {
      var cleanups = new Cleanups(withProperty("_assertYourAuthorityCast", 0), withFight());
      try (cleanups) {
        parseCombatData(
            "request/test_fight_sheriff_authority.html", "fight.php?action=skill&whichskill=7532");
        assertThat("_assertYourAuthorityCast", isSetTo(1));
      }
    }
  }

  @Nested
  class PeaceTurkey {
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7})
    void canTrackProcIndex(final int index) {
      var cleanups =
          new Cleanups(
              withProperty("peaceTurkeyIndex", index > 0 ? 0 : 5),
              withFamiliar(FamiliarPool.PEACE_TURKEY),
              withFight());
      try (cleanups) {
        parseCombatData("request/test_fight_peace_turkey_" + index + ".html");
        assertThat("peaceTurkeyIndex", isSetTo(index));
      }
    }
  }

  @Test
  public void canDetectSplitPeaSoupBanish() {
    var cleanups = new Cleanups(withFight(), withBanishedMonsters(""));

    try (cleanups) {
      parseCombatData(
          "request/test_fight_split_pea_soup.html",
          "fight.php?action=useitem&whichitem=11685&whichitem2=0");

      assertThat(
          "banishedMonsters",
          hasStringValue(startsWith("pair of burnouts:handful of split pea soup:")));
    }
  }

  @Nested
  class PowerPill {
    @ParameterizedTest
    @ValueSource(ints = {FamiliarPool.PUCK_MAN, FamiliarPool.MS_PUCK_MAN})
    public void tracksProgressOnWin(int familiar) {
      var cleanups =
          new Cleanups(
              withFamiliar(familiar),
              withProperty("powerPillProgress", 12),
              withProperty("_powerPillDrops", 1));
      try (cleanups) {
        parseCombatData("request/test_fight_win.html");
        assertThat("powerPillProgress", isSetTo(13));
        assertThat("_powerPillDrops", isSetTo(1));
      }
    }

    @ParameterizedTest
    @CsvSource({
      FamiliarPool.PUCK_MAN + ",test_fight_lose.html",
      FamiliarPool.PUCK_MAN + ",test_fight_run.html",
      FamiliarPool.MS_PUCK_MAN + ",test_fight_lose.html",
      FamiliarPool.MS_PUCK_MAN + ",test_fight_run.html",
    })
    public void doesntTrackProgressOnRunOrLoss(int familiar, String file) {
      var cleanups =
          new Cleanups(
              withFamiliar(familiar),
              withProperty("powerPillProgress", 12),
              withProperty("_powerPillDrops", 1));
      try (cleanups) {
        parseCombatData("request/" + file);
        assertThat("powerPillProgress", isSetTo(12));
        assertThat("_powerPillDrops", isSetTo(1));
      }
    }
  }

  @Test
  public void canDetectAnchorBombBanish() {
    var cleanups = new Cleanups(withFight(), withBanishedMonsters(""));

    try (cleanups) {
      parseCombatData(
          "request/test_fight_anchor_bomb.html",
          "fight.php?action=useitem&whichitem=11706&whichitem2=0");

      assertThat("banishedMonsters", hasStringValue(startsWith("lynyrd:anchor bomb:")));
    }
  }

  @Test
  public void canDetectPirateHookSteal() {
    RequestLoggerOutput.startStream();
    var cleanups = new Cleanups(withFight(), withEquipped(Slot.OFFHAND, "deft pirate hook"));
    try (cleanups) {
      parseCombatData("request/test_fight_deft_pirate_hook_steal.html", "fight.php?action=attack");
      var text = RequestLoggerOutput.stopStream();
      assertThat(
          text,
          containsString(
              "You deftly snag something from your opponent with your deft pirate hook.\nYou acquire an item: Spirit of Easter"));
    }
  }

  @Nested
  class PokeFam {
    @Test
    public void parseInitialPokefam() {
      RequestLoggerOutput.startStream();
      var cleanups = new Cleanups(withFight(0), withPath(Path.POKEFAM));
      try (cleanups) {
        parseCombatData("request/test_fight_pokefam_start.html", "fambattle.php");
        var text = RequestLoggerOutput.stopStream();
        assertThat(text, containsString("Horlotte, Lv. 1 Trick-or-Treating Tot"));
        assertThat(
            text,
            containsString("Pokefam move2 'Hug' -> 'hug': Heal the frontmost ally by [power]."));
        assertThat(
            FamiliarDatabase.getPokeDataById(FamiliarPool.BURLY_BODYGUARD).getMove2(),
            Is.is("Hug"));
        assertThat(text, not(containsString("unspecified macro action")));
      }
    }

    @Test
    public void logPokefamMoves() {
      RequestLoggerOutput.startStream();
      var cleanups = new Cleanups(withFightRequestPokefam(), withFight(1), withPath(Path.POKEFAM));
      try (cleanups) {
        parseCombatData(
            "request/test_fight_pokefam_end.html", "fambattle.php?famaction[splash-110]=Splash");
        var text = RequestLoggerOutput.stopStream();
        assertThat(text, containsString("FightRequestTest's Wereturtle uses Splash!"));
      }
    }
  }

  @Test
  public void canDetectGlitchedMalwareBanish() {
    var cleanups = new Cleanups(withFight(), withBanishedMonsters(""));

    try (cleanups) {
      parseCombatData(
          "request/test_fight_glitched_malware.html", "fight.php?action=skill&whichskill=7548");

      assertThat(
          "banishedMonsters", hasStringValue(startsWith("network worm:Deploy Glitched Malware:")));
    }
  }

  @Nested
  class CyberRealm {
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    public void cyberRealmFightsIncrementTurns(int securityLevel) {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      String adventureName = "Cyberzone " + securityLevel;
      String fileName = "request/test_cyber_zone" + securityLevel + "_fight.html";
      String property = "_cyberZone" + securityLevel + "Turns";
      KoLAdventure adventure = AdventureDatabase.getAdventureByName(adventureName);
      int snarfblat = adventure.getSnarfblat();
      String html = html(fileName);
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withFight(0),
              withProperty("_cyberZone1Turns", 4),
              withProperty("_cyberZone2Turns", 4),
              withProperty("_cyberZone3Turns", 4));
      try (cleanups) {
        client.addResponse(
            302, Map.of("location", List.of("fight.php?ireallymeanit=1667327836")), "");
        client.addResponse(200, html);
        client.addResponse(200, ""); // api.php

        var request = new GenericRequest("adventure.php?snarfblat=" + snarfblat);
        request.run();

        assertThat("_cyberZone1Turns", isSetTo(securityLevel == 1 ? 5 : 4));
        assertThat("_cyberZone2Turns", isSetTo(securityLevel == 2 ? 5 : 4));
        assertThat("_cyberZone3Turns", isSetTo(securityLevel == 3 ? 5 : 4));

        var requests = client.getRequests();

        assertThat(requests, hasSize(3));
        assertPostRequest(requests.get(0), "/adventure.php", "snarfblat=" + snarfblat);
        assertGetRequest(requests.get(1), "/fight.php", "ireallymeanit=1667327836");
        assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    public void cyberRealmFightsIncrementFreeTurns(int securityLevel) {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      String fileName = "request/test_fight_new_overclocked_win.html";
      String html = html(fileName);
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withSkill(SkillPool.OVERCLOCK10),
              withLastLocation("Cyberzone " + securityLevel),
              withFight(4),
              withProperty("_cyberFreeFights", 5));
      try (cleanups) {
        client.addResponse(200, html);
        client.addResponse(200, ""); // api.php

        var request = new GenericRequest("fight.php?action=skill&whichskill=4012");
        request.run();

        assertThat("_cyberFreeFights", isSetTo(6));
      }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    public void cyberRealmFightHasFreeTurnMaximum(int securityLevel) {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      String fileName = "request/test_fight_new_overclocked_win.html";
      String html = html(fileName);
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withSkill(SkillPool.OVERCLOCK10),
              withLastLocation("Cyberzone " + securityLevel),
              withFight(4),
              withProperty("_cyberFreeFights", 10));
      try (cleanups) {
        client.addResponse(200, html);
        client.addResponse(200, ""); // api.php

        var request = new GenericRequest("fight.php?action=skill&whichskill=4012");
        request.run();

        assertThat("_cyberFreeFights", isSetTo(10));
      }
    }

    @Test
    public void nonCyberRealmFightsDoNotIncrementFreeTurns() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      String fileName = "request/test_cyrpt_boss_defeat.html";
      String html = html(fileName);
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withSkill(SkillPool.OVERCLOCK10),
              withLastLocation("The Defiled Cranny"),
              withFight(1),
              withProperty("_cyberFreeFights", 5));
      try (cleanups) {
        client.addResponse(200, html);
        client.addResponse(200, ""); // api.php

        var request = new GenericRequest("fight.php?action=attack");
        request.run();

        assertThat("_cyberFreeFights", isSetTo(5));
      }
    }

    @Nested
    class CombatSkills {
      @Test
      public void canTrackBruteForceHammer() {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withFight(0),
                // RAM +3
                withEquipped(Slot.HAT, ItemPool.CYBERVISOR),
                // RAM +1
                withEquipped(Slot.ACCESSORY1, ItemPool.DATASTICK),
                // RAM +3
                withEffect(EffectPool.CYBER_MEMORY_BOOST),
                // Grants skill: Brute Force Hammer for 3 RAM
                withEquipped(Slot.WEAPON, ItemPool.BRUTE_FORCE_HAMMER));
        try (cleanups) {
          // adventure.php?snarfblat=587
          client.addResponse(
              302, Map.of("location", List.of("fight.php?ireallymeanit=1737296962")), "");
          client.addResponse(200, html("request/test_cyber_brute_force_hammer_0.html"));
          client.addResponse(200, ""); // api.php
          // fight.php?action=skill&whichskill=7543
          client.addResponse(200, html("request/test_cyber_brute_force_hammer_1.html"));
          client.addResponse(200, ""); // api.php
          // fight.php?action=skill&whichskill=7543
          client.addResponse(200, html("request/test_cyber_brute_force_hammer_2.html"));
          client.addResponse(200, ""); // api.php
          // fight.php?action=skill&whichskill=7543
          client.addResponse(200, html("request/test_cyber_brute_force_hammer_3.html"));
          client.addResponse(200, ""); // api.php

          var request = new GenericRequest("adventure.php?snarfblat=587");
          request.run();

          // We are now in a fight.
          assertThat(FightRequest.currentRound, is(1));
          assertThat(FightRequest.getCurrentRAM(), is(10));
          assertThat(KoLCharacter.hasCombatSkill(SkillPool.BRUTE_FORCE_HAMMER), is(true));

          var round1 =
              new GenericRequest(
                  "fight.php?action=skill&whichskill=" + SkillPool.BRUTE_FORCE_HAMMER);
          round1.run();

          assertThat(FightRequest.currentRound, is(2));
          assertThat(FightRequest.getCurrentRAM(), is(7));
          assertThat(KoLCharacter.hasCombatSkill(SkillPool.BRUTE_FORCE_HAMMER), is(true));

          var round2 =
              new GenericRequest(
                  "fight.php?action=skill&whichskill=" + SkillPool.BRUTE_FORCE_HAMMER);
          round2.run();

          assertThat(FightRequest.currentRound, is(3));
          assertThat(FightRequest.getCurrentRAM(), is(4));
          assertThat(KoLCharacter.hasCombatSkill(SkillPool.BRUTE_FORCE_HAMMER), is(true));

          var round3 =
              new GenericRequest(
                  "fight.php?action=skill&whichskill=" + SkillPool.BRUTE_FORCE_HAMMER);
          round3.run();

          assertThat(FightRequest.currentRound, is(4));
          assertThat(FightRequest.getCurrentRAM(), is(1));
          assertThat(KoLCharacter.hasCombatSkill(SkillPool.BRUTE_FORCE_HAMMER), is(false));
        }
      }

      @Test
      public void canTrackInjectMalware() {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withFight(0),
                // RAM +3 (was active during my test)
                withEquipped(Slot.HAT, ItemPool.CYBERVISOR),
                // RAM +3 (was active during my test)
                withEffect(EffectPool.CYBER_MEMORY_BOOST),
                // Grants skill: Inkect Malware for 1 RAM
                withEquipped(Slot.OFFHAND, ItemPool.MALWARE_INJECTOR));
        try (cleanups) {
          // adventure.php?snarfblat=587
          client.addResponse(
              302, Map.of("location", List.of("fight.php?ireallymeanit=1737306988")), "");
          client.addResponse(200, html("request/test_cyber_inject_malware_0.html"));
          client.addResponse(200, ""); // api.php
          // fight.php?action=skill&whichskill=7544
          client.addResponse(200, html("request/test_cyber_inject_malware_1.html"));
          client.addResponse(200, ""); // api.php

          var request = new GenericRequest("adventure.php?snarfblat=587");
          request.run();

          // We are now in a fight.
          assertThat(FightRequest.currentRound, is(1));
          assertThat(FightRequest.getCurrentRAM(), is(9));
          assertThat(KoLCharacter.hasCombatSkill(SkillPool.INJECT_MALWARE), is(true));

          var round1 =
              new GenericRequest("fight.php?action=skill&whichskill=" + SkillPool.INJECT_MALWARE);
          round1.run();

          assertThat(FightRequest.currentRound, is(2));
          assertThat(FightRequest.getCurrentRAM(), is(8));
          // You have RAM enough to (pointlessly) use the skill again.
          assertThat(KoLCharacter.hasCombatSkill(SkillPool.INJECT_MALWARE), is(true));
        }
      }

      @Test
      public void canTrackEncrypteShuriken() {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withFight(0),
                // RAM +3 (was active during my test)
                withEquipped(Slot.HAT, ItemPool.CYBERVISOR),
                // RAM +3 (was active during my test)
                withEffect(EffectPool.CYBER_MEMORY_BOOST),
                // Grants skill: Inkect Malware for 1 RAM
                withEquipped(Slot.WEAPON, ItemPool.ENCRYPTED_SHURIKEN));
        try (cleanups) {
          // adventure.php?snarfblat=586
          client.addResponse(
              302, Map.of("location", List.of("fight.php?ireallymeanit=1737308332")), "");
          client.addResponse(200, html("request/test_cyber_encrypted_shuriken_0.html"));
          client.addResponse(200, ""); // api.php
          // fight.php?action=skill&whichskill=7544
          client.addResponse(200, html("request/test_cyber_encrypted_shuriken_1.html"));
          client.addResponse(200, ""); // api.php

          var request = new GenericRequest("adventure.php?snarfblat=586");
          request.run();

          // We are now in a fight.
          assertThat(FightRequest.currentRound, is(1));
          assertThat(FightRequest.getCurrentRAM(), is(9));
          assertThat(KoLCharacter.hasCombatSkill(SkillPool.ENCRYPTED_SHURIKEN), is(true));

          var round1 =
              new GenericRequest(
                  "fight.php?action=skill&whichskill=" + SkillPool.ENCRYPTED_SHURIKEN);
          round1.run();

          assertThat(FightRequest.currentRound, is(2));
          assertThat(FightRequest.getCurrentRAM(), is(7));
          assertThat(KoLCharacter.hasCombatSkill(SkillPool.ENCRYPTED_SHURIKEN), is(true));
        }
      }

      @Test
      public void canTrackRefreshHP() {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withFight(0),
                // RAM +3 (was active during my test)
                withEquipped(Slot.HAT, ItemPool.CYBERVISOR),
                // RAM +3 (was active during my test)
                withEffect(EffectPool.CYBER_MEMORY_BOOST),
                // Grants skill: Inkect Malware for 1 RAM
                withEquipped(Slot.PANTS, ItemPool.WIRED_UNDERWEAR));
        try (cleanups) {
          // adventure.php?snarfblat=585
          client.addResponse(
              302, Map.of("location", List.of("fight.php?ireallymeanit=1737309108")), "");
          client.addResponse(200, html("request/test_cyber_refresh_hp_0.html"));
          client.addResponse(200, ""); // api.php
          // fight.php?action=skill&whichskill=7544
          client.addResponse(200, html("request/test_cyber_refresh_hp_1.html"));
          client.addResponse(200, ""); // api.php

          var request = new GenericRequest("adventure.php?snarfblat=586");
          request.run();

          // We are now in a fight.
          assertThat(FightRequest.currentRound, is(1));
          assertThat(FightRequest.getCurrentRAM(), is(9));
          assertThat(KoLCharacter.hasCombatSkill(SkillPool.REFRESH_HP), is(true));

          var round1 =
              new GenericRequest("fight.php?action=skill&whichskill=" + SkillPool.REFRESH_HP);
          round1.run();

          assertThat(FightRequest.currentRound, is(2));
          assertThat(FightRequest.getCurrentRAM(), is(8));
          assertThat(KoLCharacter.hasCombatSkill(SkillPool.REFRESH_HP), is(true));
        }
      }

      @Test
      public void canTrackLaunchLogicGrenade() {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withFight(0),
                // Grants skill: Launch Logic Grenade for 0 RAM
                withItem(ItemPool.LOGIC_GRENADE, 25));
        try (cleanups) {
          // adventure.php?snarfblat=587
          client.addResponse(
              302, Map.of("location", List.of("fight.php?ireallymeanit=1737310026")), "");
          client.addResponse(200, html("request/test_cyber_launch_logic_grenade_0.html"));
          client.addResponse(200, ""); // api.php
          // fight.php?action=skill&whichskill=7547
          client.addResponse(200, html("request/test_cyber_launch_logic_grenade_1.html"));
          client.addResponse(200, ""); // api.php

          var request = new GenericRequest("adventure.php?snarfblat=587");
          request.run();

          // We are now in a fight.
          assertThat(FightRequest.currentRound, is(1));
          assertThat(KoLCharacter.hasCombatSkill(SkillPool.LAUNCH_LOGIC_GRENADE), is(true));

          var round1 =
              new GenericRequest(
                  "fight.php?action=skill&whichskill=" + SkillPool.LAUNCH_LOGIC_GRENADE);
          round1.run();

          // Consumes item for an instakill
          assertThat(FightRequest.currentRound, is(0));
          assertEquals(24, InventoryManager.getCount(ItemPool.LOGIC_GRENADE));
        }
      }

      @Test
      public void canTrackDeployGlitchedMalware() {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withFight(0),
                // Grants skill: Deploy Glitched Malware for 0 RAM
                withItem(ItemPool.GLITCHED_MALWARE, 8));
        try (cleanups) {
          // adventure.php?snarfblat=587
          client.addResponse(
              302, Map.of("location", List.of("fight.php?ireallymeanit=1737310597")), "");
          client.addResponse(200, html("request/test_cyber_deploy_glitched_malware_0.html"));
          client.addResponse(200, ""); // api.php
          // fight.php?action=skill&whichskill=7548
          client.addResponse(200, html("request/test_cyber_deploy_glitched_malware_1.html"));
          client.addResponse(200, ""); // api.php

          var request = new GenericRequest("adventure.php?snarfblat=587");
          request.run();

          // We are now in a fight.
          assertThat(FightRequest.currentRound, is(1));
          assertThat(KoLCharacter.hasCombatSkill(SkillPool.DEPLOY_GLITCHED_MALWARE), is(true));

          var round1 =
              new GenericRequest(
                  "fight.php?action=skill&whichskill=" + SkillPool.DEPLOY_GLITCHED_MALWARE);
          round1.run();

          // Consumes item for a banish
          assertThat(FightRequest.currentRound, is(0));
          assertEquals(7, InventoryManager.getCount(ItemPool.GLITCHED_MALWARE));
        }
      }

      @Test
      public void canTrackThrustYourGeofencingRapier() {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withFight(0),
                // RAM +3
                withEquipped(Slot.HAT, ItemPool.CYBERVISOR),
                // RAM +1
                withEquipped(Slot.ACCESSORY1, ItemPool.DATASTICK),
                // RAM +3
                withEffect(EffectPool.CYBER_MEMORY_BOOST),
                // Grants skill: Thrust your geofencing rapier for 7 RAM
                withEquipped(Slot.WEAPON, ItemPool.GEOFENCING_RAPIER));
        try (cleanups) {
          // adventure.php?snarfblat=587
          client.addResponse(
              302, Map.of("location", List.of("fight.php?ireallymeanit=1737300202")), "");
          client.addResponse(200, html("request/test_cyber_thrust_geofencing_rapier_0.html"));
          client.addResponse(200, ""); // api.php
          // fight.php?action=skill&whichskill=7543
          client.addResponse(200, html("request/test_cyber_thrust_geofencing_rapier_1.html"));
          client.addResponse(200, ""); // api.php

          var request = new GenericRequest("adventure.php?snarfblat=587");
          request.run();

          // We are now in a fight.
          assertThat(FightRequest.currentRound, is(1));
          assertThat(FightRequest.getCurrentRAM(), is(10));
          assertThat(
              KoLCharacter.hasCombatSkill(SkillPool.THRUST_YOUR_GEOFENCING_RAPIER), is(true));

          var round1 =
              new GenericRequest(
                  "fight.php?action=skill&whichskill=" + SkillPool.THRUST_YOUR_GEOFENCING_RAPIER);
          round1.run();

          assertThat(FightRequest.currentRound, is(2));
          assertThat(FightRequest.getCurrentRAM(), is(3));
          assertThat(
              KoLCharacter.hasCombatSkill(SkillPool.THRUST_YOUR_GEOFENCING_RAPIER), is(false));
        }
      }
    }
  }

  @Test
  public void canDetectPirateInsult() {
    RequestLoggerOutput.startStream();
    var cleanups = new Cleanups(withFight(), withProperty("lastPirateInsult3"));
    try (cleanups) {
      parseCombatData(
          "request/test_fight_pirate_insult.html",
          "fight.php?action=useitem&whichitem=2947&whichitem2=0");
      assertThat("lastPirateInsult3", isSetTo(true));
      var text = RequestLoggerOutput.stopStream();
      assertThat(
          text,
          containsString(
              """
                  Round 1: FightRequestTest uses the The Big Book of Pirate Insults!
                  Pirate insults known: 1 (0.00%)
                  You acquire an effect: Embarrassed (1)"""));
    }
  }

  @Test
  public void canDetectTimePrankMessage() {
    RequestLoggerOutput.startStream();
    var cleanups = new Cleanups(withLastLocation("Noob Cave"));
    try (cleanups) {
      var page = "request/test_fight_time_prank.html";
      GenericRequest request = new GenericRequest("fight.php");
      request.responseText = html(page);
      AdventureRequest.registerEncounter(request);
      parseCombatData(page, "fight.php?ireallymeanit=1737125012");
      var text = RequestLoggerOutput.stopStream();
      // the original message was !"£$%^&*()<>€ so there is some double escaping going on here
      assertThat(
          text,
          containsString(
              "Round 0: Ryo_Sangnoir says: \"!&quot;&Acirc;&pound;$%^&amp;*()&lt;&gt;&acirc;�&not;\""));
    }
  }

  @Nested
  class CupidBow {
    @Test
    void canDetectCupidBow() {
      RequestLoggerOutput.startStream();
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.MINI_KIWI),
              withEquipped(Slot.FAMILIAR, ItemPool.TOY_CUPID_BOW),
              withFight());
      try (cleanups) {
        parseCombatData("request/test_cupid_bow.html");
        var text = RequestLoggerOutput.stopStream();
        assertThat(text, containsString("looks askance at the toy bow"));
        assertThat("_cupidBowFamiliars", isSetTo("300"));
      }
    }

    @Test
    void canIncrementCupidBowOnFight() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.MINI_KIWI),
              withEquipped(Slot.FAMILIAR, ItemPool.TOY_CUPID_BOW),
              withProperty("cupidBowFights", 1),
              withProperty("cupidBowLastFamiliar", 300),
              withFight());
      try (cleanups) {
        // Need a test that DOESN'T have the askance language
        parseCombatData("request/test_fight_haiku_serendipity.html");
        assertThat("cupidBowLastFamiliar", isSetTo("300"));
        assertThat("cupidBowFights", isSetTo("2"));
      }
    }

    @Test
    void canChangeCupidBowOnFight() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.MINI_KIWI),
              withEquipped(Slot.FAMILIAR, ItemPool.TOY_CUPID_BOW),
              withProperty("cupidBowFights", 0),
              withProperty("cupidBowLastFamiliar", 1),
              withFight());
      try (cleanups) {
        // Need a test that DOESN'T have the askance language
        parseCombatData("request/test_fight_haiku_serendipity.html");
        assertThat("cupidBowLastFamiliar", isSetTo("300"));
        assertThat("cupidBowFights", isSetTo("1"));
      }
    }

    @Test
    void canIncrementCupidBowOnRun() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.MINI_KIWI),
              withEquipped(Slot.FAMILIAR, ItemPool.TOY_CUPID_BOW),
              withProperty("cupidBowFights", 1),
              withProperty("cupidBowLastFamiliar", 300),
              withFight(0));
      try (cleanups) {
        // Need a test that DOESN'T have the askance language
        parseCombatData("request/test_fight_run.html");
        assertThat("cupidBowLastFamiliar", isSetTo("300"));
        assertThat("cupidBowFights", isSetTo("2"));
      }
    }
  }

  @Nested
  class Haiku {
    @Test
    public void canDetectSerendipity() {
      RequestLoggerOutput.startStream();
      var cleanups = new Cleanups(withFight(), withEffect(EffectPool.SERENDIPITY));
      try (cleanups) {
        parseCombatData("request/test_fight_haiku_serendipity.html");
        var text = RequestLoggerOutput.stopStream();
        assertThat(
            text,
            containsString(
                "After Battle: Looks like luck is on your side, you just tripped on this:"));
      }
    }

    @Test
    public void canDetectHaikuMonster() {
      RequestLoggerOutput.startStream();
      var cleanups =
          new Cleanups(
              withLastLocation("The Haiku Dungeon"), withFamiliar(FamiliarPool.CAT_BURGLAR));
      try (cleanups) {
        var page = "request/test_fight_haiku_serendipity.html";
        GenericRequest request = new GenericRequest("fight.php");
        request.responseText = html(page);
        AdventureRequest.registerEncounter(request);
        MonsterData monster = MonsterStatusTracker.getLastMonster();
        assertEquals("amateur ninja", monster.getName());
        parseCombatData(page, "fight.php?ireallymeanit=1737125012");
        var text = RequestLoggerOutput.stopStream();
        assertThat(text, containsString("Encounter: amateur ninja"));
        assertThat(text, containsString("Round 0: FightRequestTest wins initiative!"));
        assertThat(text, containsString("Round 3: amateur ninja takes 1198 damage."));
        assertThat(
            text,
            containsString(
                """
                    You gain 23 Strongness
                    You gain 36 Magicalness
                    You gain a Mysticality point!
                    You gain 49 Roguishness
                    You gain a Moxie point!"""));
      }
    }

    @Test
    public void canDetectKnobGoblinPoseur() {
      RequestLoggerOutput.startStream();
      var cleanups = new Cleanups(withLastLocation("The Haiku Dungeon"));
      try (cleanups) {
        var page = "request/test_fight_haiku_knob_goblin_poseur.html";
        GenericRequest request = new GenericRequest("fight.php");
        request.responseText = html(page);
        AdventureRequest.registerEncounter(request);
        parseCombatData(page, "fight.php?ireallymeanit=1737125012");
        var text = RequestLoggerOutput.stopStream();
        assertThat(text, containsString("Encounter: Knob Goblin poseur"));
      }
    }
  }

  @Nested
  class StillInBattle {
    @Test
    public void doNotThinkFightEndsEarlyWithBothCombatForms() {
      // check that we do not mistakenly set last combat won to "false" on round 0
      var cleanups =
          new Cleanups(
              withProperty("_lastCombatWon", true),
              withProperty("serverAddsCustomCombat", true),
              withProperty("serverAddsBothCombat", true));
      try (cleanups) {
        parseCombatData("request/test_fight_battle_end_both_combat_bars.html");
        assertThat("_lastCombatWon", isSetTo(true));
      }
    }

    @Test
    public void runAwayEndsCombatWithBothCombatForms() {
      var cleanups =
          new Cleanups(
              withProperty("_lastCombatWon", true),
              withProperty("serverAddsCustomCombat", true),
              withProperty("serverAddsBothCombat", true));
      try (cleanups) {
        parseCombatData("request/test_fight_battle_end_both_combat_bars_runaway.html");
        assertThat("_lastCombatWon", isSetTo(false));
      }
    }

    @Test
    public void runAwayEndsCombatWithOldCombatForm() {
      var cleanups =
          new Cleanups(
              withProperty("_lastCombatWon", true), withProperty("serverAddsCustomCombat", false));
      try (cleanups) {
        parseCombatData("request/test_fight_battle_end_old_combat_bar_only_runaway.html");
        assertThat("_lastCombatWon", isSetTo(false));
      }
    }

    @Test
    public void runAwayEndsCombatWithNewCombatForm() {
      var cleanups =
          new Cleanups(
              withProperty("_lastCombatWon", true), withProperty("serverAddsCustomCombat", true));
      try (cleanups) {
        parseCombatData("request/test_fight_battle_end_new_combat_bar_only_runaway.html");
        assertThat("_lastCombatWon", isSetTo(false));
      }
    }
  }

  @Nested
  class Leprecondo {
    @Test
    void parsesFurnitureDiscovery() {
      var goal = GoalManager.GOAL_LEPRECONDO.getInstance(1);
      var cleanups =
          new Cleanups(
              withItem("Leprecondo"),
              withGoal(goal),
              withProperty("leprecondoDiscovered", "1,21"),
              withProperty("leprecondoCurrentNeed"),
              withProperty("leprecondoNeedOrder"),
              withProperty("_leprecondoFurniture", 1),
              withFight(0));
      try (cleanups) {
        assertTrue(GoalManager.hasGoal(goal));
        SessionLoggerOutput.startStream();
        parseCombatData("request/test_fight_leprecondo_furniture_found.html");
        var text = SessionLoggerOutput.stopStream();
        String expected =
            "Round 2: Gog spots a sous vide laboratory inside the garbage disposal and runs out of his condo. He drags it back to the condo and stores it in the attic.";
        assertThat(text, containsString(expected));
        assertThat("leprecondoDiscovered", isSetTo("1,13,21"));
        assertThat("_leprecondoFurniture", isSetTo(2));
        assertFalse(GoalManager.hasGoal(goal));
      }
    }

    @Test
    void parsesManCaveDiscovery() {
      var goal = GoalManager.GOAL_LEPRECONDO.getInstance(1);
      var cleanups =
          new Cleanups(
              withItem("Leprecondo"),
              withGoal(goal),
              withProperty("leprecondoDiscovered", "1,21"),
              withProperty("_leprecondoFurniture", 0),
              withFight(0));
      try (cleanups) {
        String html = html("request/test_fight_leprecondo_mancave_found.html");

        SessionLoggerOutput.startStream();
        FightRequest.updateCombatData(null, null, html);
        var text = SessionLoggerOutput.stopStream();

        String expected =
            "Gwen spots a ManCave&trade; sports bar set in a puddle of spilled beer and runs out of his condo.";

        // KoL returns an HTML entity, but KoLmafia encodes messages when logging them.
        assertThat(html, containsString(expected));
        String decoded = StringUtilities.getEntityDecode(expected, false);
        assertThat(text, containsString(decoded));

        assertThat("leprecondoDiscovered", isSetTo("1,17,21"));
        assertThat("_leprecondoFurniture", isSetTo(1));
        assertFalse(GoalManager.hasGoal(goal));
      }
    }

    @Test
    void parsesNeed() {
      var cleanups =
          new Cleanups(
              withItem("Leprecondo"),
              withProperty("leprecondoDiscovered"),
              withProperty("leprecondoCurrentNeed"),
              withProperty("leprecondoLastNeedChange", 0),
              withProperty("leprecondoNeedOrder"),
              withCurrentRun(45),
              withFight(0));
      try (cleanups) {
        parseCombatData("request/test_fight_leprecondo_furniture_found.html");
        assertThat("leprecondoCurrentNeed", isSetTo("booze"));
        assertThat("leprecondoLastNeedChange", isSetTo(45));
      }
    }

    @Test
    void ignoresKnownNeed() {
      var cleanups =
          new Cleanups(
              withItem("Leprecondo"),
              withProperty("leprecondoDiscovered"),
              withProperty("leprecondoCurrentNeed", "booze"),
              withProperty("leprecondoLastNeedChange", 42),
              withProperty("leprecondoNeedOrder"),
              withCurrentRun(45),
              withFight(0));
      try (cleanups) {
        parseCombatData("request/test_fight_leprecondo_furniture_found.html");
        assertThat("leprecondoCurrentNeed", isSetTo("booze"));
        assertThat("leprecondoLastNeedChange", isSetTo(42));
      }
    }

    @Test
    public void canDetectPunchOutBanish() {
      var cleanups = new Cleanups(withFight(), withBanishedMonsters(""));

      try (cleanups) {
        parseCombatData(
            "request/test_fight_punch_out_banish.html", "fight.php?action=skill&whichskill=7561");

        assertThat(
            "banishedMonsters", hasStringValue(startsWith("dairy goat:Punch Out your Foe:")));
      }
    }

    @Test
    public void detectsAfterimageChainedMonster() {
      var cleanups = new Cleanups(withProperty("_afterimageMonster", false));

      try (cleanups) {
        parseCombatData("request/test_fight_afterimage_chain.html");
        assertThat("_afterimageMonster", isSetTo(true));
      }
    }
  }

  @Nested
  class ZootomistKicks {
    @Test
    public void canDetectKickTrack() {
      var cleanups =
          new Cleanups(
              withFight(),
              withTrackedMonsters(""),
              withProperty("zootGraftedFootLeftFamiliar", FamiliarPool.OBSERVER));

      try (cleanups) {
        parseCombatData(
            "request/test_fight_zoot_kick_track.html", "fight.php?action=skill&whichskill=7559");

        assertThat(
            "trackedMonsters",
            hasStringValue(startsWith("completely different spider:Left %n Kick:")));
      }
    }

    @Test
    public void canDetectKickBanish() {
      var cleanups =
          new Cleanups(
              withFight(),
              withBanishedMonsters(""),
              withProperty("zootGraftedFootRightFamiliar", FamiliarPool.DIRE_CASSAVA));

      try (cleanups) {
        parseCombatData(
            "request/test_fight_zoot_kick_banish.html", "fight.php?action=skill&whichskill=7560");

        assertThat(
            "banishedMonsters", hasStringValue(startsWith("big creepy spider:Right %n Kick:")));
      }
    }
  }

  @Test
  public void canDetectBloodBagFromDoctorBag() {
    RequestLoggerOutput.startStream();
    var cleanups = new Cleanups(withFight(), withProperty("_bloodBagDoctorBag", false));

    try (cleanups) {
      parseCombatData("request/test_fight_lil_doctor_blood_bag.html", "fight.php?action=attack");

      assertThat("_bloodBagDoctorBag", isSetTo(true));

      var text = RequestLoggerOutput.stopStream();
      assertThat(
          text,
          containsString("You notice a button on your doctor bag that you hadn't seen before."));
    }
  }

  @Test
  public void canDetectBloodBagFromCloake() {
    var cleanups = new Cleanups(withFight(), withProperty("_bloodBagCloake", false));

    try (cleanups) {
      parseCombatData(
          "request/test_fight_dark_feast_blood_bag.html",
          "fight.php?action=skill&whichskill=24000");

      assertThat("_bloodBagCloake", isSetTo(true));
    }
  }

  @Test
  public void canDetectYearbookCamera() {
    RequestLoggerOutput.startStream();
    var cleanups = new Cleanups(withFight(5), withPath(Path.KOLHS));
    try (cleanups) {
      var page = "request/test_fight_yearbook_camera.html";
      parseCombatData(page);
      var text = RequestLoggerOutput.stopStream();
      assertThat(text, containsString("photographed for Yearbook Club"));
      assertThat("yearbookCameraPending", isSetTo(true));
    }
  }

  @Test
  public void canDetectYearbookCameraHaiku() {
    RequestLoggerOutput.startStream();
    var cleanups =
        new Cleanups(
            withLastLocation("The Haiku Dungeon"),
            withNextMonster("ancient insane monk"),
            withFight(0),
            withPath(Path.KOLHS));
    try (cleanups) {
      var page = "request/test_fight_yearbook_camera_haiku.html";
      parseCombatData(page);
      var text = RequestLoggerOutput.stopStream();
      assertThat(text, containsString("photographed for Yearbook Club"));
      assertThat("yearbookCameraPending", isSetTo(true));
    }
  }

  @Test
  public void tracksLassoTraining() {
    var cleanups =
        new Cleanups(
            withFight(),
            withProperty("lassoTraining"),
            withProperty("lassoTrainingCount", 1),
            withEquipped(ItemPool.SEA_COWBOY_HAT),
            withEquipped(ItemPool.SEA_CHAPS));

    try (cleanups) {
      var page = "request/test_fight_sea_lasso.html";
      parseCombatData(page, "fight.php?action=useitem&whichitem=4198&whichitem2=0");
      assertThat("lassoTraining", isSetTo("clumsily"));
      assertThat("lassoTrainingCount", isSetTo(4));
    }
  }

  @Test
  public void tracksMomSeaMonkeeProgress() {
    var cleanups =
        new Cleanups(
            withFight(),
            withNextMonster("school of many"),
            withProperty("momSeaMonkeeProgress", 3),
            withEquipped(ItemPool.SHARK_JUMPER),
            withEquipped(ItemPool.SCALE_MAIL_UNDERWEAR));

    try (cleanups) {
      FightRequest.updateFinalRoundData("", true, false);
      assertThat("momSeaMonkeeProgress", isSetTo(6));
    }
  }

  @Nested
  class Seadent {
    @Test
    public void seadentIncrementsConstructKill() {
      var cleanups =
          new Cleanups(withProperty("seadentConstructKills"), withProperty("seadentLevel"));

      try (cleanups) {
        parseCombatData("request/test_fight_seadent_nubbin.html");
        assertThat("seadentConstructKills", isSetTo(1));
        assertThat("seadentLevel", isSetTo(1));
      }
    }

    @Test
    public void seadentIncrementsLevel() {
      var cleanups =
          new Cleanups(withProperty("seadentConstructKills"), withProperty("seadentLevel"));

      try (cleanups) {
        parseCombatData("request/test_fight_seadent_tine.html");
        assertThat("seadentConstructKills", isSetTo(1));
        assertThat("seadentLevel", isSetTo(2));
      }
    }

    @Test
    public void canDetectSeadentLightning() {
      var cleanups = new Cleanups(withFight(), withBanishedMonsters(""));

      try (cleanups) {
        parseCombatData(
            "request/test_fight_seadent_lightning_banish.html",
            "fight.php?action=skill&whichskill=7568");

        assertThat("banishedMonsters", hasStringValue(startsWith("Raver Giant:Sea *dent:")));
      }
    }

    @Test
    public void canDetectSeadentFishReplace() {
      RequestLoggerOutput.startStream();
      var cleanups = new Cleanups(withFight());

      try (cleanups) {
        parseCombatData(
            "request/test_fight_replace_some_fish.html", "fight.php?action=skill&whichskill=7570");

        var stream = RequestLoggerOutput.stopStream();
        assertThat(stream, containsString("your opponent becomes some fish!"));
      }
    }
  }
}
