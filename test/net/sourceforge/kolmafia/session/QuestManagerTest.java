package net.sourceforge.kolmafia.session;

import static internal.helpers.HttpClientWrapper.setupFakeClient;
import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withAdventuresSpent;
import static internal.helpers.Player.withAscensions;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFight;
import static internal.helpers.Player.withGender;
import static internal.helpers.Player.withHandlingChoice;
import static internal.helpers.Player.withHardcore;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withLastLocation;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withNoEffects;
import static internal.helpers.Player.withNoItems;
import static internal.helpers.Player.withPasswordHash;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withQuestProgress;
import static internal.matchers.Item.isInInventory;
import static internal.matchers.Preference.hasIntegerValue;
import static internal.matchers.Preference.isSetTo;
import static internal.matchers.Quest.isFinished;
import static internal.matchers.Quest.isStarted;
import static internal.matchers.Quest.isStep;
import static internal.matchers.Quest.isUnstarted;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import internal.network.FakeHttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLCharacter.Gender;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.PlaceRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import org.eclipse.jgit.util.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.cartesian.CartesianTest;

public class QuestManagerTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("QuestManager");
    Preferences.reset("QuestManager");
    KoLConstants.inventory.clear();
    AdventureSpentDatabase.resetTurns(false);
  }

  private static MonsterData monsterData(String monsterName) {
    return MonsterDatabase.findMonster(monsterName, false, true);
  }

  /*
   * *** Council Quests
   */

  @Test
  public void canParseABigBusyCouncilPage() {
    var request = new GenericRequest("council.php");
    request.responseText = html("request/test_council_first_visit_level_13.html");
    QuestManager.handleQuestChange(request);

    Set<Quest> started =
        Set.of(
            Quest.BAT,
            Quest.BLACK,
            Quest.CYRPT,
            Quest.FRIAR,
            Quest.GARBAGE,
            Quest.GOBLIN,
            Quest.ISLAND_WAR,
            Quest.LARVA,
            Quest.MACGUFFIN,
            Quest.TOPPING,
            Quest.TRAPPER);

    for (Quest quest : Quest.councilQuests()) {
      assertThat(
          "Status of " + quest.name() + " quest",
          quest,
          started.contains(quest) ? isStarted() : isUnstarted());
    }
  }

  /*
   * Level 2 - Larva
   */
  @Nested
  class Larva {
    @Test
    public void canParseLarvaReturn() {
      var cleanups = new Cleanups(withItem("mosquito larva"));

      try (cleanups) {
        var request = new GenericRequest("council.php");
        request.responseText = html("request/test_council_hand_in_larva.html");
        QuestManager.handleQuestChange(request);

        assertThat(Quest.LARVA, isFinished());
        assertThat(ItemPool.ENCHANTED_BEAN, not(isInInventory()));
      }
    }
  }

  /*
   * Level 8 - Trapper
   */

  @Nested
  class Trapper {
    @Test
    public void canDetectTrapperStep1InMcLargeHuge() {
      var request = new GenericRequest("place.php?whichplace=mclargehuge&action=trappercabin");
      request.responseText = html("request/test_place_mclargehuge_trapper_give_quest.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.TRAPPER, isStep(1));
    }

    @Test
    public void canDetectTrapperStep2InMcLargeHuge() {
      var request = new GenericRequest("place.php?whichplace=mclargehuge&action=trappercabin");
      request.responseText = html("request/test_place_mclargehuge_trapper_get_cheese_and_ore.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.TRAPPER, isStep(2));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {"discovering_your_extremity", "2_exxtreme_4_u", "3_exxxtreme_4ever_6pack"})
    public void canDetectExtremityInExtremeSlope(String nonCombat) {
      var request = new GenericRequest("adventure.php?snarfblat=273");
      request.responseText = html("request/test_adventure_extreme_slope_" + nonCombat + ".html");
      QuestManager.handleQuestChange(request);
      assertThat("currentExtremity", isSetTo(1));
    }

    @Test
    public void canDetectTrapperStep3ViaExtremeInMcLargeHuge() {
      Preferences.setInteger("currentExtremity", 3);

      var request = new GenericRequest("place.php?whichplace=mclargehuge&action=cloudypeak");
      request.responseText = html("request/test_place_mclargehuge_extreme_peak.html");
      QuestManager.handleQuestChange(request);

      assertThat(Quest.TRAPPER, isStep(3));
      assertThat("currentExtremity", isSetTo(0));
    }

    @Test
    public void canDetectTrapperStep3ViaNinjaInMcLargeHuge() {
      Preferences.setInteger("currentExtremity", 2);
      var request = new GenericRequest("place.php?whichplace=mclargehuge&action=cloudypeak");
      request.responseText = html("request/test_place_mclargehuge_ninja_peak.html");
      QuestManager.handleQuestChange(request);

      assertThat(Quest.TRAPPER, isStep(3));
      // The player may have partially completed both quests, so we still check this.
      assertThat("currentExtremity", isSetTo(0));
    }

    @Test
    public void canDetectTrapperStep4InIcyPeak() {
      QuestManager.updateQuestData("anything", monsterData("panicking Knott Yeti"));
      assertThat(Quest.TRAPPER, isStep(4));
    }

    @Test
    public void canDetectTrapperFinishedInMcLargeHuge() {
      var ascension = 50;

      var cleanups = new Cleanups(withAscensions(ascension), withItem("groar's fur"));

      try (cleanups) {
        assertThat("lastTr4pz0rQuest", hasIntegerValue(lessThan(ascension)));

        var request = new GenericRequest("place.php?whichplace=mclargehuge&action=trappercabin");
        request.responseText = html("request/test_place_mclargehuge_trapper_give_fur.html");
        QuestManager.handleQuestChange(request);

        assertThat("lastTr4pz0rQuest", isSetTo(ascension));
        assertThat(Quest.TRAPPER, isFinished());
        assertThat("groar's fur", not(isInInventory()));
      }
    }
  }

  /*
   * Level 9 - Topping Quest
   */

  @Nested
  class Topping {
    @Test
    public void canDetectToppingStep1InChasm() {
      var request = new GenericRequest("place.php?whichplace=orc_chasm");
      request.responseText = html("request/test_place_orc_chasm_bridge_built.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.TOPPING, isStep(1));
    }

    @Test
    public void canDetectToppingStep2InHighlands() {
      var request = new GenericRequest("place.php?whichplace=highlands&action=highlands_dude");
      request.responseText = html("request/test_place_highlands_meet_highland_lord.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.TOPPING, isStep(2));
    }

    @Test
    public void canDetectToppingStep3InHighlands() {
      var request = new GenericRequest("place.php?whichplace=highlands");
      request.responseText = html("request/test_place_highlands_all_fires_lit.html");
      QuestManager.handleQuestChange(request);
      assertThat("twinPeakProgress", isSetTo(15));
      assertThat(Quest.TOPPING, isStep(3));
    }

    @Test
    public void canDetectToppingFinishedInHighlands() {
      var request = new GenericRequest("place.php?whichplace=highlands&action=highlands_dude");
      request.responseText = html("request/test_place_highlands_revisit_highland_lord.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.TOPPING, isFinished());
    }

    @Nested
    class OilPeak {
      public static Stream<Arguments> oilMonsters() {
        return Stream.of(
            Arguments.of("oil slick", "oil_slick", 304.32f),
            Arguments.of("oil tycoon", "oil_tycoon", 291.64f),
            Arguments.of("oil baron", "oil_baron", 278.96f),
            Arguments.of("oil cartel", "oil_cartel", 247.26f));
      }

      @ParameterizedTest
      @MethodSource("oilMonsters")
      public void canTrackOIlPeakProgress(String monsterName, String html, float progress) {
        String path = "request/test_fight_" + html + ".html";
        String responseText = html(path);
        QuestManager.updateQuestData(responseText, monsterData(monsterName));
        assertThat("oilPeakProgress", isSetTo(progress));
      }

      @Test
      public void canTrackOilPeakProgressWearingDressPants() {
        withEquipped(Slot.PANTS, "dress pants");
        String responseText = html("request/test_fight_oil_tycoon.html");
        QuestManager.updateQuestData(responseText, monsterData("oil tycoon"));
        assertThat("oilPeakProgress", isSetTo(285.3f));
      }

      @Test
      public void canTrackOilPeakProgressWithLoveOilBeetle() {
        String responseText = html("request/test_fight_oil_slick_love_oil_beetle_proc.html");
        QuestManager.updateQuestData(responseText, monsterData("oil slick"));
        assertThat("oilPeakProgress", isSetTo(297.98f));
      }

      @Test
      public void canDetectOilPeakFinishedInOilPeak() {
        var request = new GenericRequest("adventure.php?snarfblat=298");
        request.responseText =
            html("request/test_adventure_oil_peak_unimpressed_with_pressure.html");
        html("request/test_adventure_oil_peak_unimpressed_with_pressure.html");
        QuestManager.handleQuestChange(request);
        assertThat("oilPeakProgress", isSetTo(0f));
        assertThat("oilPeakLit", isSetTo(true));
      }

      @Test
      public void canDetectOilPeakFinishedInHighlands() {
        var request = new GenericRequest("place.php?whichplace=highlands");
        request.responseText = html("request/test_place_highlands_oil_peak_lit.html");
        QuestManager.handleQuestChange(request);
        assertThat("oilPeakProgress", isSetTo(0f));
        assertThat("oilPeakLit", isSetTo(true));
      }
    }

    @Nested
    class ABooPeak {
      @ParameterizedTest
      @ValueSource(
          strings = {
            "Battlie Knight Ghost",
            "Claybender Sorcerer Ghost",
            "Dusken Raider Ghost",
            "Space Tourist Explorer Ghost",
            "Whatsian Commando Ghost"
          })
      public void canTrackBooPeakProgress(String monsterName) {
        QuestManager.updateQuestData("anything", monsterData(monsterName));
        assertThat("booPeakProgress", isSetTo(98));
      }

      @Test
      public void canDetectBooPeakFinishedInBooPeak() {
        var request = new GenericRequest("adventure.php?snarfblat=296");
        request.responseText = html("request/test_adventure_boo_peak_come_on_ghostly.html");
        QuestManager.handleQuestChange(request);
        assertThat("booPeakProgress", isSetTo(0));
        assertThat("booPeakLit", isSetTo(true));
      }

      @Test
      public void canDetectBooPeakFinishedInHighlands() {
        var request = new GenericRequest("place.php?whichplace=highlands");
        request.responseText = html("request/test_place_highlands_boo_peak_lit.html");
        QuestManager.handleQuestChange(request);
        assertThat("booPeakProgress", isSetTo(0));
        assertThat("booPeakLit", isSetTo(true));
      }
    }
  }

  /*
   * Level 10 - Garbage
   */
  @Nested
  class Garbage {
    @Test
    void canDetectGarbageStep1InPlains() {
      var request = new GenericRequest("place.php?whichplace=plains");
      request.responseText = html("request/test_place_plains_beanstalk.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.GARBAGE, isStep(1));
    }

    @Test
    public void deductsEnchantedBeanWhenPlanting() {
      var cleanups = withItem("enchanted bean");

      try (cleanups) {
        assertThat("enchanted bean", isInInventory());
        var request = new GenericRequest("place.php?whichplace=plains");
        request.responseText = html("request/test_place_plains_beanstalk.html");
        QuestManager.handleQuestChange(request);
        assertThat("enchanted bean", not(isInInventory()));
      }
    }

    @Test
    public void justBeingInAirshipIsGarbageStep1() {
      var request = new GenericRequest("adventure.php?snarfblat=81");
      request.responseText = "anything";
      QuestManager.handleQuestChange(request);
      assertThat(Quest.GARBAGE, isStep(1));
    }

    @Test
    public void canDetectGarbageStep2InAirship() {
      var request = new GenericRequest("adventure.php?snarfblat=81");
      request.responseText = html("request/test_adventure_airship_beginning_of_the_end.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.GARBAGE, isStep(2));
    }

    @Test
    public void justBeingInCastleBasementIsGarbageStep7() {
      var request = new GenericRequest("adventure.php?snarfblat=322");
      request.responseText = "anything";
      QuestManager.handleQuestChange(request);
      assertThat(Quest.GARBAGE, isStep(7));
    }

    @Test
    public void canDetectGarbageStep8InCastleBasement() {
      var ascension = 50;
      KoLCharacter.setAscensions(ascension);
      assertThat("lastCastleGroundUnlock", hasIntegerValue(lessThan(ascension)));
      var request = new GenericRequest("adventure.php?snarfblat=322");
      request.responseText = html("request/test_adventure_castle_basement_unlock_ground.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.GARBAGE, isStep(8));
      assertThat("lastCastleGroundUnlock", isSetTo(ascension));
    }

    @Test
    public void justBeingInCastleFirstFloorIsGarbageStep7() {
      var request = new GenericRequest("adventure.php?snarfblat=322");
      request.responseText = "anything";
      QuestManager.handleQuestChange(request);
      assertThat(Quest.GARBAGE, isStep(7));
    }

    @Test
    public void canDetectGarbageStep9InCastleFirstFloor() {
      var ascension = 50;
      KoLCharacter.setAscensions(ascension);
      assertThat("lastCastleTopUnlock", hasIntegerValue(lessThan(ascension)));
      var request = new GenericRequest("adventure.php?snarfblat=323");
      request.responseText = html("request/test_adventure_castle_first_top_of_the_castle_ma.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.GARBAGE, isStep(9));
      assertThat("lastCastleTopUnlock", isSetTo(ascension));
    }

    @Test
    public void failingToAdventureInCastleTopFloorDoesNotAdvanceStep9() {
      QuestDatabase.setQuestIfBetter(Quest.GARBAGE, "step8");
      var request = new GenericRequest("adventure.php?snarfblat=323");
      request.responseText = html("request/test_adventure_castle_top_floor_walk_before_fly.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.GARBAGE, isStep(8));
    }
  }

  /*
   * Level 11 - Shen
   */

  @Nested
  class Shen {
    @Test
    void canHandleDuplicatedShenQuestItem() {
      String html = html("request/test_stankara_drones.html");
      var cleanups =
          new Cleanups(
              withProperty("questL11Shen", "step2"),
              withFamiliar(FamiliarPool.GREY_GOOSE),
              withProperty("gooseDronesRemaining", 1));
      try (cleanups) {
        KoLAdventure.setLastAdventure("The Batrat and Ratbat Burrow");
        assertEquals(KoLAdventure.lastAdventureId(), AdventurePool.BATRAT);
        FightRequest.registerRequest(true, "fight.php?action=attack");
        FightRequest.currentRound = 1;
        FightRequest.updateCombatData(null, null, html);
        assertThat("gooseDronesRemaining", isSetTo(0));
        assertThat("The Stankara Stone", isInInventory(2));
        assertThat(Quest.SHEN, isStep(3));
      }
    }
  }

  /*
   * Level 11 - Ron
   */

  @Nested
  class Ron {
    @Test
    public void justBeingInProtestorsIsRonStep1() {
      var request = new GenericRequest("adventure.php?snarfblat=384");
      request.responseText = "anything";
      QuestManager.handleQuestChange(request);
      assertThat(Quest.RON, isStep(1));
    }

    @Test
    public void canDetectRonStep2InProtestors() {
      var request = new GenericRequest("adventure.php?snarfblat=384");
      request.responseText =
          html("request/test_adventure_protestors_not_so_much_with_the_humanity.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.RON, isStep(2));
    }

    @Test
    public void seeingClearedProtestorsSetsRonStep2() {
      var request = new GenericRequest("place.php?whichplace=zeppelin");
      request.responseText = html("request/test_place_zeppelin_cleared_protestors.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.RON, isStep(2));
    }

    @Test
    public void justBeingInZeppelinIsRonStep2() {
      var request = new GenericRequest("adventure.php?snarfblat=385");
      request.responseText = "anything";
      QuestManager.handleQuestChange(request);
      assertThat(Quest.RON, isStep(2));
    }

    @Test
    public void canDetectRonStep3InZeppelin() {
      var request = new GenericRequest("adventure.php?snarfblat=385");
      request.responseText = html("request/test_adventure_zeppelin_zeppelintro.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.RON, isStep(3));
    }

    @ParameterizedTest
    @CsvSource({
      "test_adventure_zeppelin_progress_1.html,1",
      "test_adventure_zeppelin_progress_2.html,2",
      "test_adventure_zeppelin_progress_3.html,3",
      "test_adventure_zeppelin_progress_4.html,4",
      "test_adventure_zeppelin_progress_5.html,5",
      "test_adventure_zeppelin_progress_6.html,6",
    })
    public void canDetectZeppelinProgress(String htmlFile, int expectedProgress) {
      // Can get progress in any location, so set to Noob Cabe.
      var cleanups = withLastLocation(AdventureDatabase.getAdventure(AdventurePool.NOOB_CAVE));
      try (cleanups) {
        var request = new GenericRequest("fight.php");
        request.responseText = html("request/" + htmlFile);
        String encounter = AdventureRequest.registerEncounter(request);
        QuestManager.updateQuestData(request.responseText, monsterData(encounter));
        assertThat("zeppelinProgress", isSetTo(expectedProgress));
        if (expectedProgress == 6) {
          assertThat(Quest.RON, isStep(4));
        }
      }
    }
  }

  /*
   * Level 11 - Palindome
   */

  @Nested
  class Palindome {
    @Test
    public void canDetectPalindomeStartInPalindome() {
      assertThat(Quest.PALINDOME, isUnstarted());
      var request = new GenericRequest("adventure.php?snarfblat=386");
      request.redirectLocation = "fight.php";
      QuestManager.handleQuestChange(request);
      assertThat(Quest.PALINDOME, isStarted());
    }

    @Test
    void canDetectPalindomeStartedInPlains() {
      var request = new GenericRequest("place.php?whichplace=plains");
      request.responseText = html("request/test_place_plains_palindome.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.PALINDOME, isStarted());
    }

    @Test
    void canDetectPalindomeStep3InPalindome() {
      var request = new GenericRequest("place.php?whichplace=palindome&action=pal_mr");
      request.responseText = html("request/test_place_palindome_meet_mr_alarm.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.PALINDOME, isStep(3));
    }
  }

  /*
   * Level 11 - Arid, Extra-Dry - Desert
   */

  @Nested
  class DesertExploration {
    @Test
    void canDetectDesertProgressWithNoBonuses() {
      String responseText = html("request/test_desert_exploration_no_bonuses.html");
      var cleanups = new Cleanups(withProperty("desertExploration", 20));
      try (cleanups) {
        KoLAdventure.setLastAdventure("The Arid, Extra-Dry Desert");
        assertEquals(KoLAdventure.lastAdventureId(), AdventurePool.ARID_DESERT);
        QuestManager.updateQuestData(responseText, monsterData("giant giant giant centipede"));
        assertTrue(responseText.contains("Desert exploration <b>+1%</b>"));
        assertEquals(Preferences.getInteger("desertExploration"), 21);
      }
    }

    @Test
    void canDetectDesertProgressWithUVResistantCompass() {
      String responseText = html("request/test_desert_exploration_compass.html");
      var cleanups =
          new Cleanups(
              withProperty("desertExploration", 20),
              withEquipped(Slot.OFFHAND, "UV-resistant compass"));
      try (cleanups) {
        KoLAdventure.setLastAdventure("The Arid, Extra-Dry Desert");
        assertEquals(KoLAdventure.lastAdventureId(), AdventurePool.ARID_DESERT);
        QuestManager.updateQuestData(responseText, monsterData("plaque of locusts"));
        assertTrue(responseText.contains("Desert exploration <b>+2%</b>"));
        assertEquals(Preferences.getInteger("desertExploration"), 22);
      }
    }

    @Test
    void canDetectDesertProgressWithSurvivalKnifeUltrahydrated() {
      String responseText = html("request/test_desert_exploration_knife.html");
      var cleanups =
          new Cleanups(
              withProperty("desertExploration", 20),
              withEquipped(Slot.WEAPON, "survival knife"),
              withEffect("Ultrahydrated"));
      try (cleanups) {
        KoLAdventure.setLastAdventure("The Arid, Extra-Dry Desert");
        assertEquals(KoLAdventure.lastAdventureId(), AdventurePool.ARID_DESERT);
        QuestManager.updateQuestData(responseText, monsterData("rock scorpion"));
        assertTrue(responseText.contains("Desert exploration <b>+3%</b>"));
        assertEquals(Preferences.getInteger("desertExploration"), 23);
      }
    }

    @Test
    void canDetectDesertProgressWithCompassAndSurvivalKnifeUltrahydrated() {
      String responseText = html("request/test_desert_exploration_compass_knife.html");
      var cleanups =
          new Cleanups(
              withProperty("desertExploration", 20),
              withEquipped(Slot.WEAPON, "survival knife"),
              withEquipped(Slot.OFFHAND, "UV-resistant compass"),
              withEffect("Ultrahydrated"));
      try (cleanups) {
        KoLAdventure.setLastAdventure("The Arid, Extra-Dry Desert");
        assertEquals(KoLAdventure.lastAdventureId(), AdventurePool.ARID_DESERT);
        QuestManager.updateQuestData(responseText, monsterData("giant giant giant centipede"));
        assertTrue(responseText.contains("Desert exploration <b>+4%</b>"));
        assertEquals(Preferences.getInteger("desertExploration"), 24);
      }
    }

    @Test
    void canDetectNoDesertProgressInFirstDesertAdvWithCompassAndSurvivalKnifeUltrahydrated() {
      String responseText = html("request/test_desert_exploration_first_adv_compass_knife.html");
      var cleanups =
          new Cleanups(
              withProperty("desertExploration", 5),
              withEquipped(Slot.WEAPON, "survival knife"),
              withEquipped(Slot.OFFHAND, "UV-resistant compass"),
              withEffect("Ultrahydrated"));
      try (cleanups) {
        KoLAdventure.setLastAdventure("The Arid, Extra-Dry Desert");
        assertEquals(KoLAdventure.lastAdventureId(), AdventurePool.ARID_DESERT);
        QuestManager.updateQuestData(responseText, monsterData("giant giant giant centipede"));
        assertTrue(responseText.contains("Desert exploration <b>+2%</b>"));
        assertEquals(Preferences.getInteger("desertExploration"), 7);
      }
    }

    @Test
    void canDetectDesertProgressWithMelodramadery() {
      String responseText = html("request/test_desert_exploration_camel.html");
      var cleanups =
          new Cleanups(
              withProperty("desertExploration", 20), withFamiliar(FamiliarPool.MELODRAMEDARY));
      try (cleanups) {
        KoLAdventure.setLastAdventure("The Arid, Extra-Dry Desert");
        assertEquals(KoLAdventure.lastAdventureId(), AdventurePool.ARID_DESERT);
        QuestManager.updateQuestData(responseText, monsterData("giant giant giant centipede"));
        assertTrue(responseText.contains("Desert exploration <b>+2%</b>"));
        assertEquals(Preferences.getInteger("desertExploration"), 22);
      }
    }

    @Test
    void canDetectDesertProgressWithMelodramaderyAndCompass() {
      String responseText = html("request/test_desert_exploration_camel_compass.html");
      var cleanups =
          new Cleanups(
              withProperty("desertExploration", 20),
              withFamiliar(FamiliarPool.MELODRAMEDARY),
              withEquipped(Slot.OFFHAND, "UV-resistant compass"));
      try (cleanups) {
        KoLAdventure.setLastAdventure("The Arid, Extra-Dry Desert");
        assertEquals(KoLAdventure.lastAdventureId(), AdventurePool.ARID_DESERT);
        QuestManager.updateQuestData(responseText, monsterData("rock scorpion"));
        assertTrue(responseText.contains("Desert exploration <b>+3%</b>"));
        assertEquals(Preferences.getInteger("desertExploration"), 23);
      }
    }

    @Test
    void canDetectDesertProgressWithMelodramaderyAndSurvivalKnifeUltrahydrated() {
      String responseText = html("request/test_desert_exploration_camel_knife.html");
      var cleanups =
          new Cleanups(
              withProperty("desertExploration", 20),
              withFamiliar(FamiliarPool.MELODRAMEDARY),
              withEquipped(Slot.WEAPON, "survival knife"),
              withEffect("Ultrahydrated"));
      try (cleanups) {
        KoLAdventure.setLastAdventure("The Arid, Extra-Dry Desert");
        assertEquals(KoLAdventure.lastAdventureId(), AdventurePool.ARID_DESERT);
        QuestManager.updateQuestData(responseText, monsterData("giant giant giant centipede"));
        assertTrue(responseText.contains("Desert exploration <b>+4%</b>"));
        assertEquals(Preferences.getInteger("desertExploration"), 24);
      }
    }

    @Test
    void canDetectDesertProgressWithMelodramaderyAndCompassAndSurvivalKnifeUltrahydrated() {
      String responseText = html("request/test_desert_exploration_camel_compass_knife.html");
      var cleanups =
          new Cleanups(
              withProperty("desertExploration", 20),
              withFamiliar(FamiliarPool.MELODRAMEDARY),
              withEquipped(Slot.OFFHAND, "UV-resistant compass"),
              withEquipped(Slot.WEAPON, "survival knife"),
              withEffect("Ultrahydrated"));
      try (cleanups) {
        KoLAdventure.setLastAdventure("The Arid, Extra-Dry Desert");
        assertEquals(KoLAdventure.lastAdventureId(), AdventurePool.ARID_DESERT);
        QuestManager.updateQuestData(responseText, monsterData("rock scorpion"));
        assertTrue(responseText.contains("Desert exploration <b>+5%</b>"));
        assertEquals(Preferences.getInteger("desertExploration"), 25);
      }
    }

    @Test
    void canDetectDesertProgressWithMelodramedaryAndSurvivalKnifeUnhydrated() {
      String responseText = html("request/test_desert_exploration_camel_knife_unhydrated.html");
      var cleanups =
          new Cleanups(
              withProperty("desertExploration", 20),
              withFamiliar(FamiliarPool.MELODRAMEDARY),
              withEquipped(Slot.WEAPON, "survival knife"));
      try (cleanups) {
        KoLAdventure.setLastAdventure("The Arid, Extra-Dry Desert");
        assertEquals(KoLAdventure.lastAdventureId(), AdventurePool.ARID_DESERT);
        QuestManager.updateQuestData(responseText, monsterData("cactuary"));
        assertTrue(responseText.contains("Desert exploration <b>+2%</b>"));
        assertEquals(Preferences.getInteger("desertExploration"), 22);
      }
    }
  }

  /*
   * Level 11 - The Oasis
   */

  @Nested
  class Oasis {
    @Test
    void canDetectOasisNotOpenWithNoDesert() {
      var cleanups =
          new Cleanups(withProperty("desertExploration", 0), withProperty("oasisAvailable", false));
      try (cleanups) {
        var URL = "place.php?whichplace=desertbeach";
        var responseText = html("request/test_visit_beach_no_desert.html");
        var request = new GenericRequest(URL);
        request.responseText = responseText;
        QuestManager.handleQuestChange(request);

        assertThat("desertExploration", isSetTo(0));
        assertThat("oasisAvailable", isSetTo(false));
      }
    }

    @Test
    void canDetectOasisNotOpenWithNoDesertProgress() {
      var cleanups =
          new Cleanups(withProperty("desertExploration", 0), withProperty("oasisAvailable", false));
      try (cleanups) {
        var URL = "place.php?whichplace=desertbeach";
        var responseText = html("request/test_visit_beach_desert_unexplored.html");
        var request = new GenericRequest(URL);
        request.responseText = responseText;
        QuestManager.handleQuestChange(request);

        assertThat("desertExploration", isSetTo(0));
        assertThat("oasisAvailable", isSetTo(false));
      }
    }

    @Test
    void canDetectOasisNotOpenWithProgress() {
      var cleanups =
          new Cleanups(withProperty("desertExploration", 0), withProperty("oasisAvailable", false));
      try (cleanups) {
        var URL = "place.php?whichplace=desertbeach";
        var responseText = html("request/test_visit_beach_desert_explored.html");
        var request = new GenericRequest(URL);
        request.responseText = responseText;
        QuestManager.handleQuestChange(request);

        assertThat("desertExploration", isSetTo(10));
        assertThat("oasisAvailable", isSetTo(false));
      }
    }

    @Test
    void canDetectOasisNotOpenWithProgressAndGnasir() {
      var cleanups =
          new Cleanups(withProperty("desertExploration", 0), withProperty("oasisAvailable", false));
      try (cleanups) {
        var URL = "place.php?whichplace=desertbeach";
        var responseText = html("request/test_visit_beach_desert_explored_gnasir.html");
        var request = new GenericRequest(URL);
        request.responseText = responseText;
        QuestManager.handleQuestChange(request);

        assertThat("desertExploration", isSetTo(10));
        assertThat("oasisAvailable", isSetTo(false));
      }
    }

    @Test
    void canDetectOasisOpenWithProgress() {
      var cleanups =
          new Cleanups(withProperty("desertExploration", 0), withProperty("oasisAvailable", false));
      try (cleanups) {
        var URL = "place.php?whichplace=desertbeach";
        var responseText = html("request/test_visit_beach_desert_explored_oasis.html");
        var request = new GenericRequest(URL);
        request.responseText = responseText;
        QuestManager.handleQuestChange(request);

        assertThat("desertExploration", isSetTo(2));
        assertThat("oasisAvailable", isSetTo(true));
      }
    }

    @Test
    void canDetectOasisOpenWithProgressAndGnasir() {
      var cleanups =
          new Cleanups(withProperty("desertExploration", 0), withProperty("oasisAvailable", false));
      try (cleanups) {
        var URL = "place.php?whichplace=desertbeach";
        var responseText = html("request/test_visit_beach_desert_explored_oasis_gnasir.html");
        var request = new GenericRequest(URL);
        request.responseText = responseText;
        QuestManager.handleQuestChange(request);

        assertThat("desertExploration", isSetTo(12));
        assertThat("oasisAvailable", isSetTo(true));
      }
    }

    @Test
    void willDowngradeDesertExplorationOnVisit() {
      var cleanups =
          new Cleanups(
              withProperty("desertExploration", 100), withProperty("oasisAvailable", true));
      try (cleanups) {
        var URL = "place.php?whichplace=desertbeach";
        var responseText = html("request/test_visit_beach_desert_98.html");
        var request = new GenericRequest(URL);
        request.responseText = responseText;
        QuestManager.handleQuestChange(request);

        assertThat("desertExploration", isSetTo(98));
        assertThat("oasisAvailable", isSetTo(true));
      }
    }
  }

  /*
   * Level 11 - Pyramid
   */

  @Nested
  class Pyramid {
    @Test
    void canDetectPyramidStartedFromBeach() {
      var request = new GenericRequest("place.php?whichplace=desertbeach&action=db_pyramid1");
      request.responseText = html("request/test_place_desert_beach_uncover_pyramid.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.PYRAMID, isStarted());
    }

    @Test
    void justBeingInPyramidIsPyramidStarted() {
      var request = new GenericRequest("place.php?whichplace=pyramid");
      request.responseText = html("request/test_place_pyramid_first_visit.html");
      QuestManager.handleQuestChange(request);

      assertThat(Quest.PYRAMID, isStarted());
    }

    @Test
    void canDetectPyramidStep1FromUpperChamber() {
      var request = new GenericRequest("adventure.php?snarfblat=406");
      request.responseText =
          html("request/test_adventure_upper_chamber_down_dooby_doo_down_down.html");
      QuestManager.handleQuestChange(request);

      assertThat(Quest.PYRAMID, isStep(1));
      assertThat("middleChamberUnlock", isSetTo(true));
    }

    @Test
    void justBeingInMiddleChamberIsPyramidStep1() {
      var request = new GenericRequest("adventure.php?snarfblat=407");
      request.responseText = "anything";
      QuestManager.handleQuestChange(request);

      assertThat(Quest.PYRAMID, isStep(1));
      assertThat("middleChamberUnlock", isSetTo(true));
    }

    @Test
    void canDetectPyramidStep1FromPyramid() {
      var request = new GenericRequest("place.php?whichplace=pyramid");
      request.responseText = html("request/test_place_pyramid_unlocked_middle_chamber.html");
      QuestManager.handleQuestChange(request);

      assertThat(Quest.PYRAMID, isStep(1));
      assertThat("middleChamberUnlock", isSetTo(true));
      assertThat("lowerChamberUnlock", isSetTo(false));
      assertThat("controlRoomUnlock", isSetTo(false));
    }

    @Test
    void canDetectPyramidStep2FromMiddleChamber() {
      var request = new GenericRequest("adventure.php?snarfblat=407");
      request.responseText =
          html("request/test_adventure_middle_chamber_further_down_dooby_doo_down_down.html");
      QuestManager.handleQuestChange(request);

      assertThat(Quest.PYRAMID, isStep(2));
      assertThat("lowerChamberUnlock", isSetTo(true));
    }

    @Test
    void canDetectPyramidStep2FromPyramid() {
      var request = new GenericRequest("place.php?whichplace=pyramid");
      request.responseText = html("request/test_place_pyramid_unlocked_lower_chamber.html");
      QuestManager.handleQuestChange(request);

      assertThat(Quest.PYRAMID, isStep(2));
      assertThat("middleChamberUnlock", isSetTo(true));
      assertThat("lowerChamberUnlock", isSetTo(true));
      assertThat("controlRoomUnlock", isSetTo(false));
    }

    @Test
    void canDetectPyramidStep3FromMiddleChamber() {
      var request = new GenericRequest("adventure.php?snarfblat=407");
      request.responseText = html("request/test_adventure_middle_chamber_under_control.html");
      QuestManager.handleQuestChange(request);

      assertThat(Quest.PYRAMID, isStep(3));
      assertThat("controlRoomUnlock", isSetTo(true));
      assertThat("pyramidPosition", isSetTo(1));
    }

    @Test
    void canDetectPyramidStep3FromPyramid() {
      var request = new GenericRequest("place.php?whichplace=pyramid");
      request.responseText = html("request/test_place_pyramid_unlocked_control_room.html");
      QuestManager.handleQuestChange(request);

      assertThat(Quest.PYRAMID, isStep(3));
      assertThat("middleChamberUnlock", isSetTo(true));
      assertThat("lowerChamberUnlock", isSetTo(true));
      assertThat("controlRoomUnlock", isSetTo(true));
    }

    public static Stream<Arguments> pyramidPositions() {
      return Stream.of(
          Arguments.of("basket", 4),
          Arguments.of("first_visit", 1),
          Arguments.of("rats_and_basket", 2),
          Arguments.of("rubble_and_vending_machine", 3),
          Arguments.of("vending_machine_and_rats", 5));
    }

    @ParameterizedTest
    @MethodSource("pyramidPositions")
    void canDetectPyramidPositionFromPyramid(String pyramidState, int position) {
      var request = new GenericRequest("place.php?whichplace=pyramid");
      request.responseText = html("request/test_place_pyramid_" + pyramidState + ".html");
      QuestManager.handleQuestChange(request);

      assertThat("pyramidPosition", isSetTo(position));
    }

    @Test
    void canDetectPyramidBombUsedFromPyramidAction() {
      var request = new GenericRequest("place.php?whichplace=pyramid&action=pyramid_state1");
      request.responseText = html("request/test_place_pyramid_bomb_rubble.html");
      QuestManager.handleQuestChange(request);

      assertThat("pyramidBombUsed", isSetTo(true));
    }

    @Test
    void canDetectPyramidBombUsedFromPyramid() {
      var request = new GenericRequest("place.php?whichplace=pyramid");
      request.responseText = html("request/test_place_pyramid_unlocked_tomb.html");
      QuestManager.handleQuestChange(request);

      assertThat("pyramidBombUsed", isSetTo(true));
    }
  }

  /*
   * Level 12 - Farm sidequest
   */

  @Nested
  class Farm {
    @Test
    public void canSelectDuckArea() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("duckAreasSelected", ""),
              withLastLocation("McMillicancuddy's Barn"),
              withPasswordHash("TEST"),
              withHandlingChoice(false));
      try (cleanups) {
        // class net.sourceforge.kolmafia.request.RelayRequest
        // adventure.php?snarfblat=137
        // 302 location = [choice.php?forceoption=0]

        builder.client.addResponse(200, html("request/test_duck_choice_1.html"));
        var request = new RelayRequest(false);
        request.constructURLString("choice.php?forceoption=0", false);
        request.run();
        assertTrue(ChoiceManager.handlingChoice);
        assertEquals(147, ChoiceManager.lastChoice);

        builder.client.addResponse(200, html("request/test_duck_choice_2.html"));
        request = new RelayRequest(false);
        request.constructURLString("choice.php?pwd&whichchoice=147&option=3");
        request.run();

        assertFalse(ChoiceManager.handlingChoice);
        assertEquals(
            Preferences.getString("duckAreasSelected"), String.valueOf(AdventurePool.THE_POND));

        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(2));
        // assertPostRequest(requests.get(0), "/adventure.php", "snarfblat=" +
        // AdventurePool.THE_BARN);
        assertGetRequest(requests.get(0), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(1), "/choice.php", "pwd=&whichchoice=147&option=3");
      }
    }

    @Test
    public void canDetectDuckAreaCleared() {
      var cleanups =
          new Cleanups(
              withProperty("duckAreasCleared", ""), withLastLocation("McMillicancuddy's Granary"));
      try (cleanups) {
        var request = new GenericRequest("adventure.php?snarfblat=" + AdventurePool.THE_GRANARY);
        request.responseText = html("request/test_no_more_ducks.html");
        QuestManager.handleQuestChange(request);
        assertEquals(
            Preferences.getString("duckAreasCleared"), String.valueOf(AdventurePool.THE_GRANARY));
      }
    }
  }

  /*
   * *** Other Quests
   */

  /*
   * Quests from Market Shops
   */

  @Nested
  class Shopkeepers {

    @Test
    public void canDetectOvergrownLotInTownWrong() {
      var cleanups = new Cleanups(withProperty("overgrownLotAvailable", false));

      try (cleanups) {
        var request = new GenericRequest("place.php?whichplace=town_wrong");
        request.responseText = html("request/test_place_town_wrong_overgrown_lot.html");
        QuestManager.handleQuestChange(request);
        assertTrue(Preferences.getBoolean("overgrownLotAvailable"));
      }
    }

    @Test
    public void canDetectMadnessBakeryInTownRight() {
      var cleanups = new Cleanups(withProperty("madnessBakeryAvailable", false));

      try (cleanups) {
        var request = new GenericRequest("place.php?whichplace=town_right");
        request.responseText = html("request/test_place_town_right_madness_bakery.html");
        QuestManager.handleQuestChange(request);
        assertTrue(Preferences.getBoolean("madnessBakeryAvailable"));
      }
    }

    @Test
    public void canDetectSkeletonStoreInTownMarket() {
      var cleanups = new Cleanups(withProperty("skeletonStoreAvailable", false));

      try (cleanups) {
        var request = new GenericRequest("place.php?whichplace=town_market");
        request.responseText = html("request/test_place_town_market_skeleton_store.html");
        QuestManager.handleQuestChange(request);
        assertTrue(Preferences.getBoolean("skeletonStoreAvailable"));
      }
    }
  }

  /*
   * Untinker Quest
   */

  @Nested
  class Untinker {
    @Test
    public void visitingUntinkerStartsQuest() {
      var cleanups = new Cleanups(withQuestProgress(Quest.UNTINKER, QuestDatabase.UNSTARTED));

      try (cleanups) {
        // "fv_untinker_quest" is the untinker before you have accepted his quest
        var urlString = "place.php?whichplace=forestvillage&action=fv_untinker_quest";
        var responseText = html("request/test_visit_untinker_quest.html");
        PlaceRequest.parseResponse(urlString, responseText);
        assertThat(Quest.UNTINKER, isUnstarted());
      }
    }

    @Test
    public void acceptingUntinkerRequestStartsQuest() {
      var cleanups = new Cleanups(withQuestProgress(Quest.UNTINKER, QuestDatabase.UNSTARTED));

      try (cleanups) {
        // "fv_untinker_quest" is the untinker before you have accepted his quest
        var urlString =
            "place.php?whichplace=forestvillage&preaction=screwquest&action=fv_untinker_quest";
        var responseText = html("request/test_visit_untinker_accept_quest.html");
        PlaceRequest.parseResponse(urlString, responseText);
        assertThat(Quest.UNTINKER, isStarted());
      }
    }

    @Test
    public void turningInScrewdriverFinishesQuest() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.UNTINKER, QuestDatabase.UNSTARTED),
              withItem(ItemPool.RUSTY_SCREWDRIVER));

      try (cleanups) {
        // "fv_untinker" is the untinker after you have accepted his quest
        var urlString = "place.php?whichplace=forestvillage&action=fv_untinker";
        var responseText = html("request/test_visit_untinker_finish_quest.html");
        PlaceRequest.parseResponse(urlString, responseText);
        assertThat(Quest.UNTINKER, isFinished());
        assertFalse(InventoryManager.hasItem(ItemPool.RUSTY_SCREWDRIVER));
      }
    }
  }

  /*
   * Spookyraven Necklace Quest
   */

  @Nested
  class LadySpookyravenNecklace {
    @Test
    public void canDetectSpookyravenNecklaceStartedInSpookyravenFirstFloor() {
      var request = new GenericRequest("place.php?whichplace=manor1");
      request.responseText = html("request/test_place_spookyraven_first_floor_quest_started.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.SPOOKYRAVEN_NECKLACE, isStarted());
    }

    @Test
    public void canDetectSpookyravenNecklaceStep2InBilliardsRoom() {
      var request = new GenericRequest("adventure.php?snarfblat=391");
      request.responseText = html("request/test_adventure_billiards_room_thats_your_cue.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.SPOOKYRAVEN_NECKLACE, isStep(2));
    }

    @Test
    public void canTrackWritingDesksFought() {
      QuestDatabase.setQuest(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.STARTED);
      QuestManager.updateQuestData("anything", monsterData("writing desk"));
      assertThat("writingDesksDefeated", isSetTo(1));
    }

    @Test
    public void doesNotTrackWritingDesksFoughtBeforeQuest() {
      QuestDatabase.setQuest(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.UNSTARTED);
      QuestManager.updateQuestData("anything", monsterData("writing desk"));
      assertThat("writingDesksDefeated", isSetTo(0));
    }

    @Test
    public void doesNotTrackWritingDesksFoughtAfterQuest() {
      QuestDatabase.setQuest(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.FINISHED);
      QuestManager.updateQuestData("anything", monsterData("writing desk"));
      assertThat("writingDesksDefeated", isSetTo(0));
    }

    @Test
    public void doesNotTrackWritingDesksFoughtAfterNecklace() {
      var cleanups = withItem(ItemPool.SPOOKYRAVEN_NECKLACE);

      try (cleanups) {
        QuestManager.updateQuestData("anything", monsterData("writing desk"));
        assertThat("writingDesksDefeated", isSetTo(0));
      }
    }

    @Test
    public void canDetectSpookyravenNecklaceFinishedTalkingToLadyS() {
      var request = new GenericRequest("place.php?whichplace=manor1&action=manor1_ladys");
      request.responseText =
          html("request/test_place_spookyraven_first_floor_receive_necklace.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.SPOOKYRAVEN_NECKLACE, isFinished());
    }

    @Test
    public void canDetectSpookyravenNecklaceFinishedInSpookyravenFirstFloor() {
      var request = new GenericRequest("place.php?whichplace=manor1");
      request.responseText =
          html("request/test_place_spookyraven_first_floor_second_floor_unlocked.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.SPOOKYRAVEN_NECKLACE, isFinished());
    }

    @Test
    public void justBeingInSpookyravenSecondFloorIsSpookyravenNecklaceFinished() {
      var ascension = 50;
      KoLCharacter.setAscensions(ascension);
      assertThat("lastSecondFloorUnlock", hasIntegerValue(lessThan(ascension)));
      var request = new GenericRequest("place.php?whichplace=manor1");
      request.responseText = html("request/test_place_spookyraven_second_floor_first_visit.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.SPOOKYRAVEN_NECKLACE, isFinished());
      assertThat("lastSecondFloorUnlock", isSetTo(ascension));
    }
  }

  /*
   * Spookyraven Dance Quest
   */
  @Nested
  class LadySpookyravenDance {
    @Test
    public void canDetectSpookyravenDanceStep1TalkingToLadyS() {
      var request = new GenericRequest("place.php?whichplace=manor2&action=manor2_ladys");
      request.responseText = html("request/test_place_spookyraven_second_floor_talk_to_ladys.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.SPOOKYRAVEN_DANCE, isStep(1));
    }

    @Test
    public void canDetectSpookyravenDanceStep3InSpookyravenSecondFloor() {
      var request = new GenericRequest("place.php?whichplace=manor2");
      request.responseText =
          html("request/test_place_spookyraven_second_floor_ballroom_unlocked.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.SPOOKYRAVEN_DANCE, isStep(3));
    }

    @Test
    public void canDetectSpookyravenDanceStepFinishedInBallroom() {
      var request = new GenericRequest("adventure.php?snarfblat=395");
      request.responseText = html("request/test_adventure_spookyraven_ballroom_having_a_ball.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.SPOOKYRAVEN_DANCE, isFinished());
    }

    @Test
    public void canDetectSpookyravenDanceStepFinishedFromStairsToAttic() {
      var request = new GenericRequest("place.php?whichplace=manor2");
      request.responseText =
          html("request/test_place_spookyraven_second_floor_attic_unlocked.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.SPOOKYRAVEN_DANCE, isFinished());
    }
  }

  /*
   * Pirate Quest
   */
  @Nested
  class Pirate {
    @Test
    void canDetectPirateFinishedInPoopDeck() {
      var request = new GenericRequest("adventure.php?snarfblat=159");
      request.responseText = html("request/test_adventure_poop_deck_its_always_swordfish.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.PIRATE, isFinished());
    }
  }

  /*
   * Azazel Quest
   */
  @Nested
  class Azazel {
    @Test
    public void visitingPandamoniumMakesSureAzazelQuestIsStarted() {
      assertThat(Quest.AZAZEL, isUnstarted());
      var request = new GenericRequest("pandamonium.php");
      request.responseText = "anything";
      QuestManager.handleQuestChange(request);
      assertThat(Quest.AZAZEL, isStarted());
    }

    @Test
    public void visitingPandamoniumDoesNotRevertQuest() {
      QuestDatabase.setQuest(Quest.AZAZEL, "step1");
      var request = new GenericRequest("pandamonium.php");
      request.responseText = "anything";
      QuestManager.handleQuestChange(request);
      assertThat(Quest.AZAZEL, isStep(1));
    }
  }

  /*
   * Melvign's Garment
   */

  @Nested
  class Melvign {
    @Test
    public void equippingShirtWithoutTorsoAwarenessGivesLetter() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.JURASSIC_PARKA),
              withItem(ItemPool.LETTER_FOR_MELVIGN, 0),
              withQuestProgress(Quest.SHIRT, QuestDatabase.UNSTARTED));
      try (cleanups) {
        client.addResponse(200, html("request/test_melvign_get_letter.html"));
        var urlString =
            "inv_equip.php?which=2&action=equip&whichitem=" + ItemPool.JURASSIC_PARKA + "&ajax=1";
        var request = new GenericRequest(urlString);
        request.run();
        assertTrue(InventoryManager.hasItem(ItemPool.LETTER_FOR_MELVIGN));
        assertThat(Quest.SHIRT, isUnstarted());
      }
    }

    @Test
    public void readingLetterStartsQuest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.LETTER_FOR_MELVIGN, 1),
              withQuestProgress(Quest.SHIRT, QuestDatabase.UNSTARTED));
      try (cleanups) {
        client.addResponse(
            302,
            Map.of("location", List.of("place.php?whichplace=mountains&action=mts_melvin")),
            "");
        client.addResponse(200, html("request/test_melvign_read_letter.html"));
        client.addResponse(200, ""); // api.php
        var urlString = "inv_use.php?which=3&whichitem=" + ItemPool.LETTER_FOR_MELVIGN + "&ajax=1";
        var request = new GenericRequest(urlString);
        request.run();
        assertFalse(InventoryManager.hasItem(ItemPool.LETTER_FOR_MELVIGN));
        assertThat(Quest.SHIRT, isStarted());
      }
    }

    @Test
    public void visitShopWithoutGarmentStartsQuest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.SHIRT, QuestDatabase.UNSTARTED));
      try (cleanups) {
        client.addResponse(200, html("request/test_melvign_visit_shop.html"));
        var urlString = "place.php?whichplace=mountains&action=mts_melvin";
        var request = new GenericRequest(urlString);
        request.run();
        assertThat(Quest.SHIRT, isStarted());
      }
    }

    @Test
    public void seeingComicShopStartsQuest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.SHIRT, QuestDatabase.UNSTARTED));
      try (cleanups) {
        client.addResponse(200, html("request/test_melvign_visit_mountains_shop.html"));
        var urlString = "place.php?whichplace=mountains";
        var request = new GenericRequest(urlString);
        request.run();
        assertThat(Quest.SHIRT, isStarted());
      }
    }

    @Test
    public void findingGarmentAdvancesQuest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.PROFESSOR_WHAT_GARMENT, 0),
              withQuestProgress(Quest.SHIRT, QuestDatabase.UNSTARTED));
      try (cleanups) {
        client.addResponse(200, html("request/test_melvign_get_garment.html"));
        client.addResponse(200, ""); // api.php
        var urlString = "adventure.php?snarfblat=387";
        var request = new GenericRequest(urlString);
        request.run();
        assertTrue(InventoryManager.hasItem(ItemPool.PROFESSOR_WHAT_GARMENT));
        assertThat(Quest.SHIRT, isStep("step1"));
      }
    }

    @Test
    public void returningGarmentFinishesQuest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.PROFESSOR_WHAT_GARMENT, 1),
              withQuestProgress(Quest.SHIRT, QuestDatabase.UNSTARTED));
      try (cleanups) {
        client.addResponse(200, html("request/test_melvign_return_shirt.html"));
        client.addResponse(200, ""); // api.php
        var urlString = "place.php?whichplace=mountains&action=mts_melvin";
        var request = new GenericRequest(urlString);
        request.run();
        assertFalse(InventoryManager.hasItem(ItemPool.PROFESSOR_WHAT_GARMENT));
        assertTrue(InventoryManager.hasItem(ItemPool.PROFESSOR_WHAT_TSHIRT));
        assertTrue(KoLCharacter.hasSkill(SkillPool.TORSO));
        assertThat(Quest.SHIRT, isFinished());
      }
    }

    @Test
    public void seeingNoComicShopFinishesQuest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.SHIRT, QuestDatabase.UNSTARTED));
      try (cleanups) {
        client.addResponse(200, html("request/test_melvign_visit_mountains_no_shop.html"));
        var urlString = "place.php?whichplace=mountains";
        var request = new GenericRequest(urlString);
        request.run();
        assertThat(Quest.SHIRT, isFinished());
      }
    }
  }

  /*
   * *** Guild Quests
   */

  /*
   * Citadel Quest
   */
  @Nested
  class Citadel {
    @Test
    void canDetectCitadelStep1InGrove() {
      var request = new GenericRequest("adventure.php?snarfblat=100");
      request.responseText = html("request/test_adventure_whiteys_grove_its_a_sign.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.CITADEL, isStep(1));
    }

    @Test
    void canDetectCitadelStep2InWhiteCitadel() {
      var request = new GenericRequest("adventure.php?snarfblat=413");
      request.responseText = html("request/test_adventure_white_citadel_they_arent_blind.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.CITADEL, isStep(2));
    }

    @Test
    void canDetectCitadelStep3InWhiteCitadel() {
      var request = new GenericRequest("adventure.php?snarfblat=413");
      request.responseText =
          html("request/test_adventure_white_citadel_existential_blues_brothers.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.CITADEL, isStep(3));
    }
  }

  /*
   * *** Path Quests
   */

  /*
   * Avatar of Boris - Clancy
   */
  @Nested
  class Clancy {
    @Test
    void canDetectClancyStep1InBarroom() {
      var request = new GenericRequest("adventure.php?snarfblat=233");
      request.responseText = html("request/test_adventure_barroom_brawl_jackin_the_jukebox.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.CLANCY, isStep(1));
    }

    @Test
    void canDetectClancyStep3InKnobShaft() {
      var request = new GenericRequest("adventure.php?snarfblat=101");
      request.responseText = html("request/test_adventure_knob_shaft_a_miner_variation.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.CLANCY, isStep(3));
    }

    @Test
    void canDetectClancyStep7InIcyPeak() {
      var request = new GenericRequest("adventure.php?snarfblat=110");
      request.responseText = html("request/test_adventure_icy_peak_mercury_rising.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.CLANCY, isStep(7));
    }

    @Test
    void canDetectClancyFinishInMiddleChamber() {
      var request = new GenericRequest("adventure.php?snarfblat=407");
      request.responseText =
          html("request/test_adventure_middle_chamber_dont_you_know_who_i_am.html");
      QuestManager.handleQuestChange(request);
      assertThat(Quest.CLANCY, isFinished());
    }
  }

  /*
   * Kingdom Of Exploathing
   */
  @Nested
  class Exploathing {
    @Test
    public void canParseInitialCouncilPage() {
      var request = new GenericRequest("place.php?whichplace=exploathing&action=expl_council");
      request.responseText = html("request/test_visit_initial_exploathing_council.html");
      QuestManager.handleQuestChange(request);

      Map<Quest, String> quests =
          Map.ofEntries(
              Map.entry(Quest.LARVA, QuestDatabase.STARTED),
              Map.entry(Quest.RAT, QuestDatabase.STARTED),
              Map.entry(Quest.BAT, QuestDatabase.STARTED),
              Map.entry(Quest.GOBLIN, "step1"),
              Map.entry(Quest.FRIAR, QuestDatabase.STARTED),
              Map.entry(Quest.CYRPT, QuestDatabase.STARTED),
              Map.entry(Quest.TRAPPER, QuestDatabase.STARTED),
              Map.entry(Quest.TOPPING, QuestDatabase.STARTED),
              Map.entry(Quest.GARBAGE, "step7"),
              Map.entry(Quest.BLACK, QuestDatabase.STARTED),
              Map.entry(Quest.MACGUFFIN, QuestDatabase.STARTED),
              Map.entry(Quest.HIPPY_FRAT, QuestDatabase.STARTED));

      for (Quest quest : Quest.councilQuests()) {
        assertThat(
            "Status of " + quest.name() + " quest",
            quest,
            quests.containsKey(quest) ? isStep(quests.get(quest)) : isUnstarted());
      }
    }
  }

  /*
   * Z is for Zootomist
   */
  @Nested
  class Zootomist {
    @Test
    public void canParseLevel1CouncilPage() {
      var request = new GenericRequest("council.php");
      request.responseText = html("request/test_visit_level1_zootomist_council.html");
      QuestManager.handleQuestChange(request);

      assertThat("Status of Larva quest", Quest.LARVA, isUnstarted());
    }
  }

  /*
   * 11,037 Leagues Under the Sea
   */
  @Nested
  class UnderTheSea {
    @ParameterizedTest
    @CsvSource({
      // We improved our dolphin whistling experience
      "true, false, true, 9, 11",
      "true, false, false, 9, 10",
      "true, false, true, 10, 11",
      "true, false, false, 10, 11",
      // We acquired a dolphin whistle
      "false, true, true, 0, 2",
      "false, true, false, 0, 1",
      // We neither improved our dolphin whistling experience nor obtained a whistle
      "false, false, true, 0, 11",
      "false, false, false, 0, 11",
    })
    public void defeatingNauticalSeaceressIncrementsSeaPoints(
        boolean message, boolean whistle, boolean isHardcore, int before, int after) {
      var cleanups =
          new Cleanups(
              withPath(Path.UNDER_THE_SEA),
              withFight(12),
              // Defeating a monster can give effects (from Book of Facts)
              withNoEffects(),
              // Ditto for items
              withNoItems(),
              withHardcore(isHardcore),
              withProperty("seaPoints", before));
      try (cleanups) {
        String html =
            message
                ? html("request/test_fight_nautical_seaceress_won.html")
                : whistle
                    ? html("request/test_fight_nautical_seaceress_won_whistle.html")
                    : html("request/test_fight_nautical_seaceress_won_no_whistle.html");
        String location = "Mer-kin Temple (Center Door)";
        String encounter = "The Nautical Seaceress";

        FightRequest.registerRequest(true, location);
        FightRequest.updateCombatData(location, encounter, html);

        assertThat("seaPoints", isSetTo(after));
      }
    }
  }

  /*
   * *** Zodiac Quests
   */

  /*
   * Little Canadia - Swamp Quest
   */
  @Test
  public void canDetectSwapStartedInCanadia() {
    assertThat(Quest.SWAMP, isUnstarted());
    var request = new GenericRequest("place.php?whichplace=canadia&action=lc_marty");
    request.responseText = html("request/test_canadia_start_quest.html");
    QuestManager.handleQuestChange(request);
    assertThat(Quest.SWAMP, isStarted());
  }

  /*
   * *** Non-Quest Related
   */

  /*
   * Arrrbor Day
   */
  @Nested
  class ArrrborDay {
    @Test
    void tracksArrrborDaySaplingsPlanted() {
      var request = new GenericRequest("adventure.php?snarfblat=174");
      request.responseText = "anything";
      QuestManager.handleQuestChange(request);
      assertThat("_saplingsPlanted", isSetTo(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"timbarrrr", "plant_a_tree"})
    void doesNotTrackArrrborDayNonCombats(String nonCombat) {
      var request = new GenericRequest("adventure.php?snarfblat=174");
      request.responseText = html("request/test_adventure_arrrboretum_" + nonCombat + ".html");
      QuestManager.handleQuestChange(request);
      assertThat("_saplingsPlanted", isSetTo(0));
    }
  }

  /*
   * *** Kingdom-Wide Unlocks
   */

  @Test
  public void seeingTheIslandMarksItAsUnlocked() {
    var ascension = 50;
    KoLCharacter.setAscensions(ascension);
    assertThat("lastIslandUnlock", hasIntegerValue(lessThan(ascension)));
    var request = new GenericRequest("main.php");
    request.responseText = html("request/test_main_island.html");
    QuestManager.handleQuestChange(request);
    assertThat("lastIslandUnlock", isSetTo(ascension));
  }

  /*
   * Airport
   */

  @Nested
  class Airport {
    private static Stream<Arguments> provideAirportsAndZones() {
      return Stream.of(
          Arguments.of(
              "cold",
              new int[] {AdventurePool.ICE_HOTEL, AdventurePool.VYKEA, AdventurePool.ICE_HOLE}),
          Arguments.of(
              "hot",
              new int[] {
                AdventurePool.SMOOCH_ARMY_HQ,
                AdventurePool.VELVET_GOLD_MINE,
                AdventurePool.LAVACO_LAMP_FACTORY,
                AdventurePool.BUBBLIN_CALDERA
              }),
          Arguments.of(
              "sleaze",
              new int[] {
                AdventurePool.SLOPPY_SECONDS_DINER,
                AdventurePool.FUN_GUY_MANSION,
                AdventurePool.YACHT
              }),
          Arguments.of(
              "spooky",
              new int[] {
                AdventurePool.DR_WEIRDEAUX,
                AdventurePool.SECRET_GOVERNMENT_LAB,
                AdventurePool.DEEP_DARK_JUNGLE
              }),
          Arguments.of(
              "stench",
              new int[] {
                AdventurePool.BARF_MOUNTAIN,
                AdventurePool.GARBAGE_BARGES,
                AdventurePool.TOXIC_TEACUPS,
                AdventurePool.LIQUID_WASTE_SLUICE
              }));
    }

    @ParameterizedTest
    @MethodSource("provideAirportsAndZones")
    void canParseAFullAirport(String element, int[] zones) {
      assertThat(element + "AirportAlways", isSetTo(false));
      assertThat("_" + element + "AirportToday", isSetTo(false));

      var request = new GenericRequest("place.php?whichplace=airport");
      request.responseText = html("request/test_place_airport_all_charters.html");
      QuestManager.handleQuestChange(request);

      assertThat("_" + element + "AirportToday", isSetTo(true));
    }

    @ParameterizedTest
    @MethodSource("provideAirportsAndZones")
    void justBeingInAirportLocationWithoutPermanentAccessMeansDaypass(
        String element, int[] snarfblats) {
      assertThat(element + "AirportAlways", isSetTo(false));
      assertThat("_" + element + "AirportToday", isSetTo(false));

      var request = new GenericRequest("adventure.php?snarfblat=" + snarfblats[0]);
      request.responseText = "anything";
      QuestManager.handleQuestChange(request);

      assertThat(element, "_" + element + "AirportToday", isSetTo(true));
    }

    @ParameterizedTest
    @MethodSource("provideAirportsAndZones")
    void justBeingInAirportLocationWithPermanentAccessDoesNotMeanDaypass(
        String element, int[] snarfblats) {
      Preferences.setBoolean(element + "AirportAlways", true);
      assertThat(element, "_" + element + "AirportToday", isSetTo(false));

      for (int snarfblat : snarfblats) {
        var request = new GenericRequest("adventure.php?snarfblat=" + snarfblat);
        request.responseText = "anything";
        QuestManager.handleQuestChange(request);
      }

      assertThat("_" + element + "AirportToday", isSetTo(false));
    }

    @ParameterizedTest
    @MethodSource("provideAirportsAndZones")
    void beingDeniedAccessToAirportLocationDoesNotMeanDaypass(String element, int[] snarfblats) {
      assertThat(element + "AirportAlways", isSetTo(false));
      assertThat("_" + element + "AirportToday", isSetTo(false));

      var request = new GenericRequest("adventure.php?snarfblat=" + snarfblats[0]);
      request.responseText = html("request/test_adventure_that_isnt_a_place.html");
      QuestManager.handleQuestChange(request);

      assertThat(element, "_" + element + "AirportToday", isSetTo(false));
    }
  }

  @Nested
  class ConspiracyIsland {
    @Test
    void canDetectUnlockingArmoryInSpookyBunker() {
      var cleanups = withItem("armory keycard");
      try (cleanups) {
        assertThat("armoryUnlocked", isSetTo(false));
        assertThat("canteenUnlocked", isSetTo(false));
        assertThat("SHAWARMAInitiativeUnlocked", isSetTo(false));

        var request =
            new GenericRequest("place.php?whichplace=airport_spooky_bunker&action=si_shop3locked");
        request.responseText =
            html("request/test_place_airport_spooky_bunker_unlocking_armory.html");
        QuestManager.handleQuestChange(request);

        assertThat("armory keycard", not(isInInventory()));
        assertThat("armoryUnlocked", isSetTo(true));
        assertThat("canteenUnlocked", isSetTo(false));
        assertThat("SHAWARMAInitiativeUnlocked", isSetTo(false));
      }
    }

    @Test
    void canDetectUnlockingCanteenInSpookyBunker() {
      var cleanups = withItem("bottle-opener keycard");

      try (cleanups) {
        assertThat("armoryUnlocked", isSetTo(false));
        assertThat("canteenUnlocked", isSetTo(false));
        assertThat("SHAWARMAInitiativeUnlocked", isSetTo(false));

        var request =
            new GenericRequest("place.php?whichplace=airport_spooky_bunker&action=si_shop2locked");
        request.responseText =
            html("request/test_place_airport_spooky_bunker_unlocking_canteen.html");
        QuestManager.handleQuestChange(request);

        assertThat("bottle-opener keycard", not(isInInventory()));
        assertThat("armoryUnlocked", isSetTo(false));
        assertThat("canteenUnlocked", isSetTo(true));
        assertThat("SHAWARMAInitiativeUnlocked", isSetTo(false));
      }
    }

    @Test
    void canDetectUnlockingShawarmaInSpookyBunker() {
      var cleanups = withItem("SHAWARMA Initiative Keycard");

      try (cleanups) {
        assertThat("armoryUnlocked", isSetTo(false));
        assertThat("canteenUnlocked", isSetTo(false));
        assertThat("SHAWARMAInitiativeUnlocked", isSetTo(false));

        var request =
            new GenericRequest("place.php?whichplace=airport_spooky_bunker&action=si_shop1locked");
        request.responseText =
            html("request/test_place_airport_spooky_bunker_unlocking_shawarma.html");
        QuestManager.handleQuestChange(request);

        assertThat("SHAWARMA Initiative keycard", not(isInInventory()));
        assertThat("armoryUnlocked", isSetTo(false));
        assertThat("canteenUnlocked", isSetTo(false));
        assertThat("SHAWARMAInitiativeUnlocked", isSetTo(true));
      }
    }

    @Test
    void canDetectEverythingUnlockedInSpookyBunker() {
      assertThat("armoryUnlocked", isSetTo(false));
      assertThat("canteenUnlocked", isSetTo(false));
      assertThat("SHAWARMAInitiativeUnlocked", isSetTo(false));

      var request = new GenericRequest("place.php?whichplace=airport_spooky_bunker");
      request.responseText =
          html("request/test_place_airport_spooky_bunker_everything_unlocked.html");
      QuestManager.handleQuestChange(request);

      assertThat("armoryUnlocked", isSetTo(true));
      assertThat("canteenUnlocked", isSetTo(true));
      assertThat("SHAWARMAInitiativeUnlocked", isSetTo(true));
    }

    @Test
    void canDetectNothingUnlockedInSpookyBunker() {
      Preferences.setBoolean("armoryUnlocked", true);
      Preferences.setBoolean("canteenUnlocked", true);
      Preferences.setBoolean("SHAWARMAInitiativeUnlocked", true);

      var request = new GenericRequest("place.php?whichplace=airport_spooky_bunker");
      request.responseText = html("request/test_place_airport_spooky_bunker_nothing_unlocked.html");
      QuestManager.handleQuestChange(request);

      assertThat("armoryUnlocked", isSetTo(false));
      assertThat("canteenUnlocked", isSetTo(false));
      assertThat("SHAWARMAInitiativeUnlocked", isSetTo(false));
    }
  }

  /*
   * The Sea
   */

  @Nested
  class TheSea {

    // This test uses a complete map of The Sea Floor.
    // It was captured while wearing "black glass".
    // I.e., every zone has been opened.

    @Test
    public void seeingCompleteSeaFloorOpensAllZones() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED),
              withProperty("mapToAnemoneMinePurchased", false),
              withProperty("mapToMadnessReefPurchased", false),
              withProperty("mapToTheDiveBarPurchased", false),
              withProperty("mapToTheMarinaraTrenchPurchased", false),
              withProperty("mapToTheSkateParkPurchased", false),
              withProperty("intenseCurrents", false),
              withProperty("corralUnlocked", false));
      try (cleanups) {
        var request = new GenericRequest("seafloor.php");
        request.responseText = html("request/test_visit_sea_floor.html");
        QuestManager.handleQuestChange(request);

        assertThat(Quest.SEA_MONKEES, isStep("step12"));
        assertThat("mapToAnemoneMinePurchased", isSetTo(true));
        assertThat("mapToMadnessReefPurchased", isSetTo(true));
        assertThat("mapToTheDiveBarPurchased", isSetTo(true));
        assertThat("mapToTheMarinaraTrenchPurchased", isSetTo(true));
        assertThat("mapToTheSkateParkPurchased", isSetTo(true));
        assertThat("intenseCurrents", isSetTo(true));
        assertThat("corralUnlocked", isSetTo(true));
      }
    }

    // This test uses a the Sea Floor as you first see it.
    // I.e., Only An Octopus's Garden is open.

    @Test
    public void seeingEmptySeaFloorOpensNoZones() {
      var cleanups = new Cleanups(withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED));
      try (cleanups) {
        var request = new GenericRequest("seafloor.php");
        request.responseText = html("request/test_quest_sea_monkee_unstarted.html");
        QuestManager.handleQuestChange(request);
        assertThat(Quest.SEA_MONKEES, isUnstarted());
      }
    }

    @ParameterizedTest
    @CsvSource({"4, PASTAMANCER, false", "4, SEAL_CLUBBER, true", "2, PASTAMANCER, true"})
    public void seeingTrenchOnSeaFloor(
        final int step, AscensionClass ascensionClass, boolean unlocked) {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withClass(ascensionClass),
              withQuestProgress(Quest.SEA_MONKEES, step));
      try (cleanups) {
        builder.client.addResponse(200, html("request/test_quest_sea_monkee_step_4_2.html"));
        builder.client.addResponse(200, ""); // api.php

        var request = new GenericRequest("seafloor.php", false);
        request.run();

        assertThat("mapToTheMarinaraTrenchPurchased", isSetTo(unlocked));
      }
    }

    private static final AdventureResult SAND_DOLLAR = ItemPool.get(ItemPool.SAND_DOLLAR);

    // ***** The Old Guy Quest *****
    //
    // Talk to the Old Man
    // Buy a damp old boot from Big Brother
    // Give the Old Man the boot and pick a reward.

    @Nested
    class OldMan {
      private static final AdventureResult DAMP_OLD_BOOT = ItemPool.get(ItemPool.DAMP_OLD_BOOT);
      private static final AdventureResult FISHY_PIPE = ItemPool.get(ItemPool.FISHY_PIPE);

      @Test
      public void talkingToOldManStartsQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.UNSTARTED));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_visit_old_man_1.html"));
          builder.client.addResponse(200, ""); // api.php

          var URL = "place.php?whichplace=sea_oldman&action=oldman_oldman";
          var request = new GenericRequest(URL, false);
          request.run();

          assertThat(Quest.SEA_OLD_GUY, isStarted());

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(2));
          assertGetRequest(
              requests.get(0), "/place.php", "whichplace=sea_oldman&action=oldman_oldman");
          assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        }
      }

      @Test
      public void talkingToOldManAgainStartsQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.UNSTARTED));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_visit_old_man_1A.html"));

          var URL = "place.php?whichplace=sea_oldman&action=oldman_oldman";
          var request = new GenericRequest(URL, false);
          request.run();

          assertThat(Quest.SEA_OLD_GUY, isStarted());

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(1));
          assertGetRequest(
              requests.get(0), "/place.php", "whichplace=sea_oldman&action=oldman_oldman");
        }
      }

      @Test
      public void buyingDampOldBootAdvancesQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withItem(SAND_DOLLAR.getInstance(3102)),
                withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.UNSTARTED),
                withProperty("dampOldBootPurchased", false),
                withPasswordHash("SEAMONKEES"),
                withGender(Gender.FEMALE));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_buy_damp_old_boot.html"));
          builder.client.addResponse(200, ""); // api.php

          String URL = "monkeycastle.php?action=buyitem&whichitem=3471&quantity=1";
          var request = new GenericRequest(URL);
          request.run();

          assertThat(Quest.SEA_OLD_GUY, isStep(1));
          assertThat("dampOldBootPurchased", isSetTo(true));
          assertTrue(InventoryManager.hasItem(DAMP_OLD_BOOT));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(2));
          assertPostRequest(
              requests.get(0),
              "/monkeycastle.php",
              "action=buyitem&whichitem=3471&quantity=1&pwd=SEAMONKEES");
          assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        }
      }

      @Test
      public void givingBootToOldManFinishesQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withItem(DAMP_OLD_BOOT),
                withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.UNSTARTED));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_visit_old_man_2.html"));
          builder.client.addResponse(200, html("request/test_visit_old_man_3.html"));
          builder.client.addResponse(200, ""); // api.php

          var URL = "place.php?whichplace=sea_oldman&action=oldman_oldman";
          var request = new GenericRequest(URL, false);
          request.run();
          URL =
              "place.php?whichplace=sea_oldman&action=oldman_oldman&preaction=pickreward&whichreward=6314";
          request.constructURLString(URL);
          request.run();

          assertThat(Quest.SEA_OLD_GUY, isFinished());
          assertFalse(InventoryManager.hasItem(DAMP_OLD_BOOT));
          assertTrue(InventoryManager.hasItem(FISHY_PIPE));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(3));
          assertGetRequest(
              requests.get(0), "/place.php", "whichplace=sea_oldman&action=oldman_oldman");
          assertPostRequest(
              requests.get(1),
              "/place.php",
              "whichplace=sea_oldman&action=oldman_oldman&preaction=pickreward&whichreward=6314");
          assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
        }
      }

      @Test
      public void seeingOldManSnoringFinishesQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.UNSTARTED));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_visit_old_man_4.html"));

          var URL = "place.php?whichplace=sea_oldman&action=oldman_oldman";
          var request = new GenericRequest(URL, false);
          request.run();

          assertThat(Quest.SEA_OLD_GUY, isFinished());

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(1));
          assertGetRequest(
              requests.get(0), "/place.php", "whichplace=sea_oldman&action=oldman_oldman");
        }
      }
    }

    // ***** The Sea Monkee Quest *****
    //
    // - Rescue Little Brother
    // - Rescue Big Brother
    // - Rescue Grandpa
    // - Rescue Grandma
    // - Rescue Mom

    @Nested
    class LittleBrother {
      private static final AdventureResult WRIGGLING_FLYTRAP_PELLET =
          ItemPool.get(ItemPool.WRIGGLING_FLYTRAP_PELLET);

      @Test
      public void gettingWrigglingPelletDoesNotStartQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withFight(1),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED));
        try (cleanups) {
          // fight.php?action=attack
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_started_1.html"));
          builder.client.addResponse(200, ""); // api.php

          var request = new GenericRequest("fight.php?action=attack");
          request.run();

          assertEquals(0, FightRequest.currentRound);
          assertTrue(InventoryManager.hasItem(WRIGGLING_FLYTRAP_PELLET));
          assertThat(Quest.SEA_MONKEES, isUnstarted());

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(2));
          assertPostRequest(requests.get(0), "/fight.php", "action=attack");
          assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        }
      }

      @Test
      public void usingWrigglingPelletStartsQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withItem(WRIGGLING_FLYTRAP_PELLET),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED),
                withPasswordHash("SEAMONKEES"),
                withGender(Gender.FEMALE));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_started_2.html"));
          builder.client.addResponse(200, ""); // api.php

          var request = new GenericRequest("inv_use.php?which=3&whichitem=3580&pwd&ajax=1");
          request.run();

          assertThat(Quest.SEA_MONKEES, isStarted());
          assertFalse(InventoryManager.hasItem(WRIGGLING_FLYTRAP_PELLET));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(2));
          assertPostRequest(
              requests.get(0), "/inv_use.php", "which=3&whichitem=3580&ajax=1&pwd=SEAMONKEES");
          assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        }
      }

      @Test
      public void seeingOnlySeaMonkeeCastleStartsQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_started_3.html"));

          var request = new GenericRequest("seafloor.php", false);
          request.run();

          assertThat(Quest.SEA_MONKEES, isStarted());

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(1));
          assertGetRequest(requests.get(0), "/seafloor.php", null);
        }
      }

      @Test
      public void seeingLittleBrotherInCastleStartsQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_started_4.html"));
          builder.client.addResponse(200, ""); // api.php

          var request = new GenericRequest("monkeycastle.php", false);
          request.run();

          assertThat(Quest.SEA_MONKEES, isStarted());

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(2));
          assertGetRequest(requests.get(0), "/monkeycastle.php", null);
          assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        }
      }
    }

    @Nested
    class BigBrother {
      @Test
      public void talkingToLittleBrotherAdvancesQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_step_1_1.html"));

          var request = new GenericRequest("monkeycastle.php?who=1", false);
          request.run();

          assertThat(Quest.SEA_MONKEES, isStep(1));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(1));
          assertGetRequest(requests.get(0), "/monkeycastle.php", "who=1");
        }
      }

      @Test
      public void seeingWreckOnSeaFloorAdvancesQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_step_1_2.html"));

          var request = new GenericRequest("seafloor.php", false);
          request.run();

          assertThat(Quest.SEA_MONKEES, isStep(1));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(1));
          assertGetRequest(requests.get(0), "/seafloor.php", null);
        }
      }

      @Test
      void rescuingBigBrotherAdvancesQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED),
                withProperty("bigBrotherRescued", false),
                withPasswordHash("SEAMONKEES"),
                withGender(Gender.FEMALE));
        try (cleanups) {
          builder.client.addResponse(
              302, Map.of("location", List.of("choice.php?forceoption=0")), "");
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_step_2_1.html"));
          builder.client.addResponse(200, ""); // api.php
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_step_2_2.html"));
          builder.client.addResponse(200, ""); // api.php

          String URL =
              "adventure.php?snarfblat=" + AdventurePool.THE_WRECK_OF_THE_EDGAR_FITZSIMMONS;
          var request = new GenericRequest(URL);
          request.run();

          URL = "choice.php?whichchoice=299&option=1";
          request.constructURLString(URL);
          request.run();

          assertThat(Quest.SEA_MONKEES, isStep(2));
          assertThat("bigBrotherRescued", isSetTo(true));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(5));
          assertPostRequest(
              requests.get(0),
              "/adventure.php",
              "snarfblat=" + AdventurePool.THE_WRECK_OF_THE_EDGAR_FITZSIMMONS + "&pwd=SEAMONKEES");
          assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
          assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
          assertPostRequest(
              requests.get(3), "/choice.php", "whichchoice=299&option=1&pwd=SEAMONKEES");
          assertPostRequest(requests.get(4), "/api.php", "what=status&for=KoLmafia");
        }
      }

      @Test
      void seeingBigBrotherInCastleAdvancesQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withProperty("bigBrotherRescued", false),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_step_2_3.html"));

          var request = new GenericRequest("monkeycastle.php", false);
          request.run();

          assertThat(Quest.SEA_MONKEES, isStep(2));
          assertThat("bigBrotherRescued", isSetTo(true));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(1));
          assertGetRequest(requests.get(0), "/monkeycastle.php", null);
        }
      }

      @Test
      public void talkingToBigBrotherAdvancesQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_step_3.html"));

          var request = new GenericRequest("monkeycastle.php?who=2", false);
          request.run();

          assertThat(Quest.SEA_MONKEES, isStep(3));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(1));
          assertGetRequest(requests.get(0), "/monkeycastle.php", "who=2");
        }
      }
    }

    @Nested
    class Grandpa {
      @Test
      public void talkingToLittleBrotherStartsQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_step_4_1.html"));

          var request = new GenericRequest("monkeycastle.php?who=1", false);
          request.run();

          assertThat(Quest.SEA_MONKEES, isStep(4));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(1));
          assertGetRequest(requests.get(0), "/monkeycastle.php", "who=1");
        }
      }

      @Test
      void findingGrandpaAdvancesQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED),
                withPasswordHash("SEAMONKEES"),
                withGender(Gender.FEMALE));
        try (cleanups) {
          builder.client.addResponse(
              302, Map.of("location", List.of("choice.php?forceoption=0")), "");
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_step_5_1.html"));
          builder.client.addResponse(200, ""); // api.php
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_step_5_2.html"));
          builder.client.addResponse(200, ""); // api.php

          String URL = "adventure.php?snarfblat=" + AdventurePool.MARINARA_TRENCH;
          var request = new GenericRequest(URL);
          request.run();

          URL = "choice.php?whichchoice=303&option=1";
          request.constructURLString(URL);
          request.run();

          assertThat(Quest.SEA_MONKEES, isStep(5));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(5));
          assertPostRequest(
              requests.get(0),
              "/adventure.php",
              "snarfblat=" + AdventurePool.MARINARA_TRENCH + "&pwd=SEAMONKEES");
          assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
          assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
          assertPostRequest(
              requests.get(3), "/choice.php", "whichchoice=303&option=1&pwd=SEAMONKEES");
          assertPostRequest(requests.get(4), "/api.php", "what=status&for=KoLmafia");
        }
      }

      @Test
      void seeingGrandpaInCastleAdvancesQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_step_5_3.html"));

          var request = new GenericRequest("monkeycastle.php", false);
          request.run();

          assertThat(Quest.SEA_MONKEES, isStep(5));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(1));
          assertGetRequest(requests.get(0), "/monkeycastle.php", null);
        }
      }
    }

    @Nested
    class Grandma {
      private static final AdventureResult GRANDMAS_NOTE = ItemPool.get(ItemPool.GRANDMAS_NOTE);
      private static final AdventureResult FUCHSIA_YARN = ItemPool.get(ItemPool.FUCHSIA_YARN);
      private static final AdventureResult CHARTREUSE_YARN = ItemPool.get(ItemPool.CHARTREUSE_YARN);
      private static final AdventureResult GRANDMAS_MAP = ItemPool.get(ItemPool.GRANDMAS_MAP);

      @Test
      public void talkingToGrandpaStartsQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED),
                withItem(GRANDMAS_NOTE));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_step_6_1.html"));
          builder.client.addResponse(200, ""); // api.php

          String URL = "monkeycastle.php?action=grandpastory&topic=grandma";
          var request = new GenericRequest(URL);
          request.run();

          assertThat(Quest.SEA_MONKEES, isStep(6));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(2));
          assertPostRequest(
              requests.get(0), "/monkeycastle.php", "action=grandpastory&topic=grandma");
          assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        }
      }

      @Test
      public void seeingOutPostOnSeaFloorStartsQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_step_6_2.html"));

          var request = new GenericRequest("seafloor.php", false);
          request.run();

          assertThat(Quest.SEA_MONKEES, isStep(6));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(1));
          assertGetRequest(requests.get(0), "/seafloor.php", null);
        }
      }

      @Test
      void gettingGrandmasNoteAdvancesQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_step_7_1.html"));
          builder.client.addResponse(200, ""); // api.php

          String URL = "adventure.php?snarfblat=" + AdventurePool.MERKIN_OUTPOST;
          var request = new GenericRequest(URL);
          request.run();

          assertThat(Quest.SEA_MONKEES, isStep(7));
          assertTrue(InventoryManager.hasItem(GRANDMAS_NOTE));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(2));
          assertPostRequest(
              requests.get(0), "/adventure.php", "snarfblat=" + AdventurePool.MERKIN_OUTPOST);
          assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        }
      }

      @Test
      public void talkingToGrandpaWithNoteConfirmsQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED),
                withItem(GRANDMAS_NOTE));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_step_7_2.html"));
          builder.client.addResponse(200, ""); // api.php

          String URL = "monkeycastle.php?action=grandpastory&topic=note";
          var request = new GenericRequest(URL);
          request.run();

          assertThat(Quest.SEA_MONKEES, isStep(7));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(2));
          assertPostRequest(requests.get(0), "/monkeycastle.php", "action=grandpastory&topic=note");
          assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        }
      }

      @Test
      public void talkingToGrandpaWithNoteAndYarnAdvancesQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED),
                withItem(GRANDMAS_NOTE),
                withItem(FUCHSIA_YARN),
                withItem(CHARTREUSE_YARN));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_step_8.html"));

          String URL = "monkeycastle.php?action=grandpastory&topic=note";
          var request = new GenericRequest(URL);
          request.run();

          assertThat(Quest.SEA_MONKEES, isStep(8));
          assertFalse(InventoryManager.hasItem(GRANDMAS_NOTE));
          assertFalse(InventoryManager.hasItem(FUCHSIA_YARN));
          assertFalse(InventoryManager.hasItem(CHARTREUSE_YARN));
          assertTrue(InventoryManager.hasItem(GRANDMAS_MAP));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(1));
          assertPostRequest(requests.get(0), "/monkeycastle.php", "action=grandpastory&topic=note");
        }
      }

      @Test
      void rescuingGrandmaAdvancesQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_step_9_1.html"));
          builder.client.addResponse(200, ""); // api.php

          String URL = "adventure.php?snarfblat=" + AdventurePool.MERKIN_OUTPOST;
          var request = new GenericRequest(URL);
          request.run();

          assertThat(Quest.SEA_MONKEES, isStep(9));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(2));
          assertPostRequest(
              requests.get(0), "/adventure.php", "snarfblat=" + AdventurePool.MERKIN_OUTPOST);
          assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        }
      }

      @Test
      void seeingGrandmaInCastleAdvancesQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_step_9_2.html"));
          builder.client.addResponse(200, ""); // api.php

          var request = new GenericRequest("monkeycastle.php", false);
          request.run();

          assertThat(Quest.SEA_MONKEES, isStep(9));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(2));
          assertGetRequest(requests.get(0), "/monkeycastle.php", null);
          assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        }
      }
    }

    @Nested
    class Mom {
      private static final AdventureResult BLACK_GLASS = ItemPool.get(ItemPool.BLACK_GLASS);

      @Test
      public void talkingToLittleBrotherStartsQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_step_10.html"));
          builder.client.addResponse(200, ""); // api.php

          var request = new GenericRequest("monkeycastle.php?who=1", false);
          request.run();

          assertThat(Quest.SEA_MONKEES, isStep(10));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(2));
          assertGetRequest(requests.get(0), "/monkeycastle.php", "who=1");
          assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        }
      }

      @Test
      public void talkingToBigBrotherAdvancesQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_step_11.html"));
          builder.client.addResponse(200, ""); // api.php

          var request = new GenericRequest("monkeycastle.php?who=2", false);
          request.run();

          assertThat(Quest.SEA_MONKEES, isStep(11));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(2));
          assertGetRequest(requests.get(0), "/monkeycastle.php", "who=2");
          assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        }
      }

      @Test
      public void buyingBlackGlassAdvancesQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withItem(SAND_DOLLAR.getInstance(2887)),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED),
                withPasswordHash("SEAMONKEES"),
                withGender(Gender.FEMALE));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_step_12.html"));
          builder.client.addResponse(200, ""); // api.php

          String URL = "monkeycastle.php?pwd&action=buyitem&whichitem=6398&quantity=1";
          var request = new GenericRequest(URL);
          request.run();

          assertThat(Quest.SEA_MONKEES, isStep(12));
          assertTrue(InventoryManager.hasItem(BLACK_GLASS));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(2));
          assertPostRequest(
              requests.get(0),
              "/monkeycastle.php",
              "action=buyitem&whichitem=6398&quantity=1&pwd=SEAMONKEES");
          assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        }
      }

      @Test
      void rescuingMomFinishesQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_finished_1.html"));
          builder.client.addResponse(200, ""); // api.php

          String URL = "adventure.php?snarfblat=" + AdventurePool.CALIGINOUS_ABYSS;
          var request = new GenericRequest(URL);
          request.run();

          assertThat(Quest.SEA_MONKEES, isFinished());

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(2));
          assertPostRequest(
              requests.get(0), "/adventure.php", "snarfblat=" + AdventurePool.CALIGINOUS_ABYSS);
          assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        }
      }

      @Test
      void seeingMomInCastleFinishesQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_finished_2.html"));

          var request = new GenericRequest("monkeycastle.php", false);
          request.run();

          assertThat(Quest.SEA_MONKEES, isFinished());

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(1));
          assertGetRequest(requests.get(0), "/monkeycastle.php", null);
        }
      }

      @Test
      void gettingBuffFromMomFinishesQuest() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_sea_monkee_finished_3.html"));
          builder.client.addResponse(200, ""); // api.php

          String URL = "monkeycastle.php?action=mombuff&whichbuff=7";
          var request = new GenericRequest(URL);
          request.run();

          assertThat(Quest.SEA_MONKEES, isFinished());

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(2));
          assertPostRequest(requests.get(0), "/monkeycastle.php", "action=mombuff&whichbuff=7");
          assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        }
      }
    }

    // ***** Mer-Kin Deepcity Quest *****
    //
    // - Expose Intense Currents
    // - Open Corral
    // - Tame a Seahorse
    //
    // Do one of {Mer-kin scholar, Mer-kin gladiator, Dad}

    @Nested
    class Currents {
      private static final AdventureResult MERKIN_TRAILMAP = ItemPool.get(ItemPool.MERKIN_TRAILMAP);

      @Test
      public void usingTrailmapExposesCurrents() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withItem(MERKIN_TRAILMAP),
                withProperty("intenseCurrents", false),
                withPasswordHash("MERKIN"),
                withGender(Gender.FEMALE));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_intense_currents_1.html"));
          builder.client.addResponse(200, ""); // api.php

          var request = new GenericRequest("inv_use.php?which=3&whichitem=3808&pwd&ajax=1");
          request.run();

          assertThat("intenseCurrents", isSetTo(true));
          assertFalse(InventoryManager.hasItem(MERKIN_TRAILMAP));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(2));
          assertPostRequest(
              requests.get(0), "/inv_use.php", "which=3&whichitem=3808&ajax=1&pwd=MERKIN");
          assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        }
      }

      @Test
      public void visitingSeaFloorSeesCurrents() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(withHttpClientBuilder(builder), withProperty("intenseCurrents", false));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_intense_currents_2.html"));
          builder.client.addResponse(200, ""); // api.php

          var request = new GenericRequest("seafloor.php", false);
          request.run();

          assertThat("intenseCurrents", isSetTo(true));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(2));
          assertGetRequest(requests.get(0), "/seafloor.php", null);
          assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        }
      }
    }

    @Nested
    class Corral {
      @Test
      public void talkingToGrandpaOpensCorral() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(withHttpClientBuilder(builder), withProperty("corralUnlocked", false));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_coral_corral_1.html"));
          builder.client.addResponse(200, html("request/test_quest_coral_corral_2.html"));
          builder.client.addResponse(200, ""); // api.php

          var request = new GenericRequest("monkeycastle.php?who=3", false);
          request.run();
          request = new GenericRequest("monkeycastle.php?action=grandpastory&topic=currents");
          request.run();

          assertThat("corralUnlocked", isSetTo(true));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(3));
          assertGetRequest(requests.get(0), "/monkeycastle.php", "who=3");
          assertPostRequest(
              requests.get(1), "/monkeycastle.php", "action=grandpastory&topic=currents");
          assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
        }
      }

      @Test
      public void visitingSeaFloorSeesCorral() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(withHttpClientBuilder(builder), withProperty("corralUnlocked", false));
        try (cleanups) {
          builder.client.addResponse(200, html("request/test_quest_coral_corral_3.html"));

          var request = new GenericRequest("seafloor.php", false);
          request.run();

          assertThat("corralUnlocked", isSetTo(true));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(1));
          assertGetRequest(requests.get(0), "/seafloor.php", null);
        }
      }
    }

    @Nested
    class Seahorse {
      @Test
      public void tamingSeahorseLearnsName() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withItem(ItemPool.get(ItemPool.SEA_COWBELL, 3)),
                withItem(ItemPool.get(ItemPool.SEA_LASSO, 1)),
                withProperty("seahorseName", ""));
        try (cleanups) {
          // adventure.php?snarfblat=199
          builder.client.addResponse(
              302, Map.of("location", List.of("fight.php?ireallymeanit=1669055563")), "");
          // fight.php?ireallymeanit=1669055563
          builder.client.addResponse(200, html("request/test_tame_seahorse_0.html"));
          builder.client.addResponse(200, ""); // api.php
          // fight.php?action=useitem&whichitem=4196
          builder.client.addResponse(200, html("request/test_tame_seahorse_1.html"));
          // fight.php?action=useitem&whichitem=4196
          builder.client.addResponse(200, html("request/test_tame_seahorse_2.html"));
          // fight.php?action=useitem&whichitem=4196
          builder.client.addResponse(200, html("request/test_tame_seahorse_3.html"));
          // fight.php?action=useitem&whichitem=4198
          builder.client.addResponse(200, html("request/test_tame_seahorse_4.html"));
          builder.client.addResponse(200, ""); // api.php

          String URL = "adventure.php?snarfblat=" + AdventurePool.THE_CORAL_CORRAL;
          var request = new GenericRequest(URL);
          request.run();
          request.constructURLString("fight.php?action=useitem&whichitem=4196");
          request.run();
          request.constructURLString("fight.php?action=useitem&whichitem=4196");
          request.run();
          request.constructURLString("fight.php?action=useitem&whichitem=4196");
          request.run();
          request.constructURLString("fight.php?action=useitem&whichitem=4198");
          request.run();

          assertEquals(0, FightRequest.currentRound);
          assertThat(InventoryManager.getCount(ItemPool.SEA_COWBELL), is(0));
          assertThat(InventoryManager.getCount(ItemPool.SEA_LASSO), is(0));
          assertThat("seahorseName", isSetTo("Shimmerswim"));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(8));

          assertPostRequest(
              requests.get(0), "/adventure.php", "snarfblat=" + AdventurePool.THE_CORAL_CORRAL);
          assertGetRequest(requests.get(1), "/fight.php", "ireallymeanit=1669055563");
          assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
          assertPostRequest(requests.get(3), "/fight.php", "action=useitem&whichitem=4196");
          assertPostRequest(requests.get(4), "/fight.php", "action=useitem&whichitem=4196");
          assertPostRequest(requests.get(5), "/fight.php", "action=useitem&whichitem=4196");
          assertPostRequest(requests.get(6), "/fight.php", "action=useitem&whichitem=4198");
          assertPostRequest(requests.get(7), "/api.php", "what=status&for=KoLmafia");
        }
      }

      @Test
      public void followingCurrentsLearnsSeahorseName() {
        var builder = new FakeHttpClientBuilder();
        var cleanups =
            new Cleanups(withHttpClientBuilder(builder), withProperty("seahorseName", ""));
        try (cleanups) {
          // seafloor.php?action=currents
          builder.client.addResponse(
              302, Map.of("location", List.of("sea_merkin.php?seahorse=1")), "");
          // sea_merkin.php?seahorse=1
          builder.client.addResponse(200, html("request/test_visit_mer_kin_deepcity.html"));

          var request = new GenericRequest("seafloor.php?action=currents");
          request.run();

          assertThat("seahorseName", isSetTo("Shimmerswim"));

          var requests = builder.client.getRequests();
          assertThat(requests, hasSize(2));

          assertPostRequest(requests.get(0), "/seafloor.php", "action=currents");
          assertGetRequest(requests.get(1), "/sea_merkin.php", "seahorse=1");
        }
      }
    }
  }

  /*
   * Spacegate
   */

  @Nested
  class Spacegate {
    @Test
    void allHazardsHaveValidItems() {
      for (QuestManager.Hazard hazard : QuestManager.HAZARDS) {
        assertTrue(ItemDatabase.contains(hazard.gear()));
      }
    }

    @ParameterizedTest
    @ValueSource(strings = {"place.php?whichplace=spacegate", "adventure.php?snarfblat=494"})
    void justBeingInSpacegateWithoutPermanentAccessMeansDaypass() {
      var cleanups =
          new Cleanups(
              withProperty("spacegateAlways", false), withProperty("_spacegateToday", false));
      try (cleanups) {
        var request = new GenericRequest("place.php?whichplace=spacegate");
        request.responseText = "anything";
        QuestManager.handleQuestChange(request);
        assertThat("_spacegateToday", isSetTo(true));
      }
    }

    @ParameterizedTest
    @ValueSource(strings = {"place.php?whichplace=spacegate", "adventure.php?snarfblat=494"})
    void justBeingInSpacegateWithPermanentAccessDoesNotMeanDaypass() {
      var cleanups =
          new Cleanups(
              withProperty("spacegateAlways", true), withProperty("_spacegateToday", false));
      try (cleanups) {
        var request = new GenericRequest("place.php?whichplace=spacegate");
        request.responseText = "anything";
        QuestManager.handleQuestChange(request);

        assertThat("_spacegateToday", isSetTo(false));
      }
    }

    @ParameterizedTest
    @ValueSource(
        strings = {"choice.php?forceoption=0", "place.php?whichplace=spacegate&action=sg_Terminal"})
    void canParseSpacegateTerminal(String url) {
      var cleanups =
          new Cleanups(
              withProperty("_spacegateAnimalLife"),
              withProperty("_spacegateCoordinates"),
              withProperty("_spacegateGear"),
              withProperty("_spacegateHazards"),
              withProperty("_spacegateIntelligentLife"),
              withProperty("_spacegateMurderbot"),
              withProperty("_spacegatePlanetIndex"),
              withProperty("_spacegatePlanetName"),
              withProperty("_spacegatePlantLife"),
              withProperty("_spacegateRuins"),
              withProperty("_spacegateSpant"),
              withProperty("_spacegateTurnsLeft"));
      try (cleanups) {
        var request = new GenericRequest(url);
        request.responseText = html("request/test_spacegate_terminal_earddyk.html");
        QuestManager.handleQuestChange(request);
        assertThat("_spacegateCoordinates", isSetTo("EARDDYK"));
        assertThat("_spacegatePlanetIndex", isSetTo(4));
        assertThat("_spacegatePlanetName", isSetTo("Diverticulus Isaacson IX"));
        assertThat("_spacegateHazards", isSetTo("high winds"));
        assertThat("_spacegateGear", isSetTo("high-friction boots"));
        assertThat("_spacegateAnimalLife", isSetTo("primitive (hostile)"));
        assertThat("_spacegatePlantLife", isSetTo("primitive"));
        assertThat("_spacegateIntelligentLife", isSetTo("none detected"));
        assertThat("_spacegateMurderbot", isSetTo(false));
        assertThat("_spacegateRuins", isSetTo(false));
        assertThat("_spacegateSpant", isSetTo(false));
        assertThat("_spacegateTurnsLeft", isSetTo(20));
      }
    }

    @Test
    void canActivateSpacegateAndAcquireGear() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("_spacegateAnimalLife"),
              withProperty("_spacegateCoordinates"),
              withProperty("_spacegateGear"),
              withProperty("_spacegateHazards"),
              withProperty("_spacegateIntelligentLife"),
              withProperty("_spacegateMurderbot"),
              withProperty("_spacegatePlanetIndex"),
              withProperty("_spacegatePlanetName"),
              withProperty("_spacegatePlantLife"),
              withProperty("_spacegateRuins"),
              withProperty("_spacegateSpant"),
              withProperty("_spacegateTurnsLeft"));
      try (cleanups) {
        // Approach the terminal and prepare to activate it
        var request = new GenericRequest("place.php?whichplace=spacegate&action=sg_Terminal");
        builder.client.addResponse(
            302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        builder.client.addResponse(200, html("request/test_spacegate_terminal.html"));
        request.run();

        // Select a random planet
        request = new GenericRequest("choice.php?whichchoice=1235&pwd&option=3");
        builder.client.addResponse(200, html("request/test_spacegate_activate.html"));
        builder.client.addResponse(200, ""); // api.php
        request.run();

        assertThat("_spacegateCoordinates", isSetTo("ECWSIJJ"));
        assertThat("_spacegatePlanetIndex", isSetTo(4));
        assertThat("_spacegatePlanetName", isSetTo("Beta Fritus IX"));
        assertThat("_spacegateHazards", isSetTo("irradiated"));
        assertThat("_spacegateGear", isSetTo("rad cloak"));
        assertThat("_spacegateAnimalLife", isSetTo("primitive"));
        assertThat("_spacegatePlantLife", isSetTo("none detected"));
        assertThat("_spacegateIntelligentLife", isSetTo("none detected"));
        assertThat("_spacegateMurderbot", isSetTo(true));
        assertThat("_spacegateRuins", isSetTo(false));
        assertThat("_spacegateSpant", isSetTo(true));
        assertThat("_spacegateTurnsLeft", isSetTo(20));

        // Attempt to adventure.
        assertFalse(InventoryManager.hasItem(ItemPool.RAD_CLOAK));
        request = new GenericRequest("adventure.php?snarfblat=494");
        builder.client.addResponse(200, html("request/test_spacegate_hazards_1.html"));
        builder.client.addResponse(200, ""); // api.php
        request.run();
        assertTrue(InventoryManager.hasItem(ItemPool.RAD_CLOAK));
      }
    }

    @Test
    void canActivatePortableSpacegateAndAcquireGear() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.PORTABLE_SPACEGATE),
              withProperty("_spacegateGear"),
              withProperty("_spacegateHazards"),
              withProperty("_spacegateTurnsLeft"));
      try (cleanups) {
        // Use your portable spacegate
        var request = new GenericRequest("inv_use.php?pwd&which=3&whichitem=9465&ajax=1");
        builder.client.addResponse(200, html("request/test_portable_spacegate_activate.html"));
        builder.client.addResponse(200, ""); // api.php
        request.run();
        assertFalse(InventoryManager.hasItem(ItemPool.PORTABLE_SPACEGATE));
        assertTrue(InventoryManager.hasItem(ItemPool.OPEN_PORTABLE_SPACEGATE));
        assertThat("_spacegateTurnsLeft", isSetTo(20));

        // It gave us a random planet. Look at the gate.
        request = new GenericRequest("place.php?whichplace=spacegate_portable");
        builder.client.addResponse(200, html("request/test_portable_spacegate.html"));
        request.run();

        // It didn't tell us a thing about it. Dangerous!
        // Attempt to adventure.
        request = new GenericRequest("adventure.php?snarfblat=494");
        builder.client.addResponse(200, html("request/test_spacegate_hazards_2.html"));
        request.run();
        assertThat("_spacegateHazards", isSetTo("high gravity"));
        assertThat("_spacegateGear", isSetTo("exo-servo leg braces"));
        assertTrue(InventoryManager.hasItem(ItemPool.EXO_SERVO_LEG_BRACES));
      }
    }
  }

  @Nested
  class DayPasses {
    public void checkDayPasses(
        String place, String html, boolean perm, String always, String today) {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty(always, perm),
              withProperty(today, false));
      try (cleanups) {
        boolean isPlace = !place.endsWith(".php");
        var url = isPlace ? "place.php?whichplace=" + place : place;
        var request = new GenericRequest(url);
        builder.client.addResponse(200, html);
        request.run();
        if (today.equals("none")) {
          assertTrue(Preferences.getBoolean(always));
        } else if (perm) {
          assertTrue(Preferences.getBoolean(always));
          assertFalse(Preferences.getBoolean(today));
        } else {
          assertFalse(Preferences.getBoolean(always));
          assertTrue(Preferences.getBoolean(today));
        }

        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(1));
        if (isPlace) {
          assertPostRequest(requests.get(0), "/place.php", "whichplace=" + place);
        } else {
          assertGetRequest(requests.get(0), "/" + url, null);
        }
      }
    }

    @ParameterizedTest
    @CsvSource({
      "daycareOpen, _daycareToday",
      "neverendingPartyAlways, _neverendingPartyToday",
      "loveTunnelAvailable, _loveTunnelToday",
      "ownsSpeakeasy, none"
    })
    public void checkDayPassesInTownWrong(String always, String today) {
      var html = html("request/test_visit_town_wrong.html");
      // If we have always access, we don't have today access
      checkDayPasses("town_wrong", html, true, always, today);
      // If we don't have always access, we have today access
      checkDayPasses("town_wrong", html, false, always, today);
    }

    @ParameterizedTest
    @CsvSource({
      "horseryAvailable, none",
      "telegraphOfficeAvailable, _telegraphOfficeToday",
      "voteAlways, _voteToday"
    })
    public void checkDayPassesInTownRight(String always, String today) {
      var html = html("request/test_visit_town_right.html");
      // If we have always access, we don't have today access
      checkDayPasses("town_right", html, true, always, today);
      // If we don't have always access, we have today access
      checkDayPasses("town_right", html, false, always, today);
    }

    @ParameterizedTest
    @CsvSource({"frAlways, _frToday", "prAlways, _prToday", "crAlways, _crToday"})
    public void checkDayPassesInMonorail(String always, String today) {
      var html = html("request/test_visit_monorail.html");
      // If we have always access, we don't have today access
      checkDayPasses("monorail", html, true, always, today);
      // If we don't have always access, we have today access
      checkDayPasses("monorail", html, false, always, today);
    }

    @ParameterizedTest
    @CsvSource({
      "coldAirportAlways, _coldAirportToday",
      "hotAirportAlways, _hotAirportToday",
      "sleazeAirportAlways, _sleazeAirportToday",
      "spookyAirportAlways, _spookyAirportToday",
      "stenchAirportAlways, _stenchAirportToday"
    })
    public void checkDayPassesInAirport(String always, String today) {
      var html = html("request/test_visit_airport.html");
      // If we have always access, we don't have today access
      checkDayPasses("airport", html, true, always, today);
      // If we don't have always access, we have today access
      checkDayPasses("airport", html, false, always, today);
    }

    @ParameterizedTest
    @CsvSource({
      "chateauAvailable, none",
      "snojoAvailable, none",
      "gingerbreadCityAvailable, _gingerbreadCityToday",
      "spacegateAlways, none"
    })
    public void checkDayPassesInMountains(String always, String today) {
      var html = html("request/test_visit_mountains.html");
      // If we have always access, we don't have today access
      checkDayPasses("mountains", html, true, always, today);
      // If we don't have always access, we have today access
      checkDayPasses("mountains", html, false, always, today);
    }

    @ParameterizedTest
    @CsvSource({"barrelShrineUnlocked, none"})
    public void checkDayPassesInDungeons(String always, String today) {
      var html = html("request/test_visit_dungeons.html");
      // If we have always access, we don't have today access
      checkDayPasses("da.php", html, true, always, today);
      // If we don't have always access, we have today access
      checkDayPasses("da.php", html, false, always, today);
    }

    @ParameterizedTest
    @CsvSource({"getawayCampsiteUnlocked, none"})
    public void checkDayPassesInWoods(String always, String today) {
      var html = html("request/test_visit_woods.html");
      // If we have always access, we don't have today access
      checkDayPasses("woods", html, true, always, today);
      // If we don't have always access, we have today access
      checkDayPasses("woods", html, false, always, today);
    }
  }

  @Nested
  class ZoneOpening {
    @Test
    public void willOpenThirdFloorAfterDancingWithLadySpookyraven() {
      var builder = new FakeHttpClientBuilder();
      var cleanup =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.STARTED));

      try (cleanup) {
        var request =
            new GenericRequest("adventure.php?snarfblat=" + AdventurePool.HAUNTED_BALLROOM);
        builder.client.addResponse(200, html("request/test_spookraven_dance.html"));
        request.run();
        assertThat(Quest.SPOOKYRAVEN_DANCE, isFinished());
        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(3));
        assertPostRequest(
            requests.get(0), "/adventure.php", "snarfblat=" + AdventurePool.HAUNTED_BALLROOM);
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(2), "/place.php", "whichplace=manor2");
      }
    }

    @Test
    public void willOpenBeanstalkAfterPlantingBean() {
      var builder = new FakeHttpClientBuilder();
      var cleanup =
          new Cleanups(
              withItem("enchanted bean"),
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.GARBAGE, QuestDatabase.STARTED));

      try (cleanup) {
        var request = new GenericRequest("place.php?whichplace=plains&action=garbage_grounds");
        builder.client.addResponse(200, html("request/test_plant_enchanted_bean.html"));
        request.run();
        assertThat(Quest.GARBAGE, isStep(1));
        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0), "/place.php", "whichplace=plains&action=garbage_grounds");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void willOpenHiddenTempleAfterReadingMap() {
      var builder = new FakeHttpClientBuilder();

      // You plant your Spooky Sapling in the loose soil at the base of the
      // Temple. You spray it with your Spooky-Gro Fertilizer, and it immediately
      // grows to 20 feet in height. You can easily climb the branches to reach
      // the first step of the Temple now...

      var ascension = 50;
      var cleanup =
          new Cleanups(
              withItem(ItemPool.SPOOKY_MAP),
              withItem(ItemPool.SPOOKY_SAPLING),
              withItem(ItemPool.SPOOKY_FERTILIZER),
              withHttpClientBuilder(builder),
              // The Quest SHOULD suffice...
              withQuestProgress(Quest.TEMPLE, QuestDatabase.STARTED),
              // But we have a legacy property which tracks the same thing.
              withAscensions(ascension),
              withProperty("lastTempleUnlock", ascension - 1));

      try (cleanup) {
        assertFalse(KoLCharacter.getTempleUnlocked());
        var request = UseItemRequest.getInstance(ItemPool.SPOOKY_MAP, 1);
        builder.client.addResponse(200, html("request/test_spooky_temple_map.html"));
        request.run();

        assertThat(Quest.TEMPLE, isFinished());
        assertEquals(Preferences.getInteger("lastTempleUnlock"), ascension);
        assertTrue(KoLCharacter.getTempleUnlocked());

        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0), "/inv_use.php", "whichitem=" + ItemPool.SPOOKY_MAP + "&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void willStartHiddenTempleQuestProfessorFanning() {
      setupFakeClient();

      // "Sure, right. So I've got a 'quest' for you. See all the vines growing
      // on the rock? I could climb up with those, but they need
      // strengthening. So I need you to pick up a couple things for me."

      var cleanup =
          new Cleanups(
              withNextResponse(200, html("request/test_dakota_1.html")),
              withQuestProgress(Quest.TEMPLE, QuestDatabase.UNSTARTED));

      try (cleanup) {
        assertFalse(KoLCharacter.getTempleUnlocked());
        var request = new PlaceRequest("woods", "woods_dakota_anim");
        request.run();

        assertThat(Quest.TEMPLE, isStarted());
        assertFalse(KoLCharacter.getTempleUnlocked());
      }
    }

    @Test
    public void willOpenHiddenTempleProfessorFanning() {
      setupFakeClient();

      // "I never told you my name!" you shout back, but he's already
      // gone. Grumbling, you make a note of the temple's location on your
      // map. What a jerk. You hope he gets killed by pygmies or something.

      var ascension = 50;
      var cleanup =
          new Cleanups(
              withNextResponse(200, html("request/test_dakota_2.html")),
              withItem(ItemPool.BENDY_STRAW),
              withItem(ItemPool.PLANT_FOOD),
              withItem(ItemPool.SEWING_KIT),
              // The Quest SHOULD suffice...
              withQuestProgress(Quest.TEMPLE, QuestDatabase.STARTED),
              // But we have a legacy property which tracks the same thing.
              withAscensions(ascension),
              withProperty("lastTempleUnlock", ascension - 1));

      try (cleanup) {
        assertFalse(KoLCharacter.getTempleUnlocked());
        var request = new PlaceRequest("woods", "woods_dakota");
        request.run();

        assertThat(Quest.TEMPLE, isFinished());
        assertEquals(Preferences.getInteger("lastTempleUnlock"), ascension);
        assertTrue(KoLCharacter.getTempleUnlocked());
      }
    }

    @Test
    public void willReadDiaryWhenAcquired() {
      var cleanup =
          new Cleanups(
              withItem(ItemPool.FORGED_ID_DOCUMENTS),
              withItem(ItemPool.MACGUFFIN_DIARY),
              withGender(Gender.FEMALE),
              withQuestProgress(Quest.BLACK, "step2"),
              withProperty("autoQuest", true),
              withQuestProgress(Quest.MACGUFFIN, QuestDatabase.STARTED),
              withQuestProgress(Quest.DESERT, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.MANOR, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.SHEN, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.RON, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.WORSHIP, QuestDatabase.UNSTARTED),
              withNextResponse(
                  new FakeHttpResponse<>(
                      302, Map.of("location", List.of("choice.php?forceoption=0")), ""),
                  new FakeHttpResponse<>(200, html("request/test_vacation_with_forged_id.html")),
                  new FakeHttpResponse<>(200, html("request/test_vacation_get_diary.html")),
                  new FakeHttpResponse<>(200, html("request/test_vacation_diary.html"))));

      try (cleanup) {
        // This does a 302 redirect to choice.php
        var shore = new GenericRequest("adventure.php?snarfblat=" + AdventurePool.THE_SHORE);
        shore.run();
        // This takes a vacation with the forged identification documents
        var vacation = new GenericRequest("choice.php?pwd&whichchoice=793&option=2");
        vacation.run();
        // You acquire your father's MacGuffin diary.
        // with autoQuest = true, we "use" it, which calls diary.php?textversion=1

        assertThat(Quest.MACGUFFIN, isStep(2));
        assertThat(Quest.BLACK, isFinished());
        assertThat(Quest.DESERT, isStarted());
        assertThat(Quest.MANOR, isStarted());
        assertThat(Quest.SHEN, isStarted());
        assertThat(Quest.RON, isStarted());
        assertThat(Quest.WORSHIP, isStarted());
      }
    }

    @Test
    public void willUseVolcanoMapWhenAcquired() {
      var builder = new FakeHttpClientBuilder();

      var cleanup =
          new Cleanups(
              withProperty("autoQuest", true),
              withLastLocation("Madness Bakery"),
              withHttpClientBuilder(builder));

      try (cleanup) {
        builder.client.addResponse(
            new FakeHttpResponse<>(200, html("request/test_fight_volcano_map.html")));
        builder.client.addResponse(
            new FakeHttpResponse<>(
                302, Map.of("location", List.of("inventory.php?which=3&action=message")), ""));
        builder.client.addResponse(
            new FakeHttpResponse<>(200, html("request/test_volcano_message.html")));

        var fight = new GenericRequest("fight.php?action=skill&whichskill=4012");
        // This finishes the fight with the final Nemesis assassin
        fight.run();
        // You acquire the secret tropical island volcano lair map
        // with autoQuest = true, we "use" it, which calls inv_use.php
        //
        // This redirects to inventory.php?which=3&action=message (first time)
        // or volcanoisland.php (subsequent times)

        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(4));
        assertPostRequest(requests.get(0), "/fight.php", "action=skill&whichskill=4012");
        assertPostRequest(
            requests.get(1), "/inv_use.php", "which=3&whichitem=" + ItemPool.VOLCANO_MAP);
        assertGetRequest(requests.get(2), "/inventory.php", "which=3&action=message");
        assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }

  @CartesianTest
  public void canTrackFriarNCs(
      @CartesianTest.Values(ints = {1, 2, 3, 4}) int ncNumber,
      @CartesianTest.Values(strings = {"heart", "neck", "elbow"}) String name) {
    String locationName = "The Dark " + StringUtils.capitalize(name) + " of the Woods";
    String fileName = "test_friar_" + name + "_" + ncNumber + ".html";
    String propertyName = "lastFriars" + StringUtils.capitalize(name) + "NC";

    var request =
        new GenericRequest(
            "adventure.php?snarfblat="
                + AdventureDatabase.getAdventureByName(locationName).getSnarfblat());
    request.responseText = html("request/" + fileName);
    var cleanup =
        new Cleanups(withAdventuresSpent(locationName, 11), withProperty(propertyName, -1));
    try (cleanup) {
      QuestManager.handleQuestChange(request);
      assertEquals(Preferences.getInteger(propertyName), 11);
    }
  }

  @Nested
  class Speakeasy {
    @Test
    public void canParseSpeakeasyName() {
      var cleanup = new Cleanups(withProperty("speakeasyName", "Oliver's Place"));

      try (cleanup) {
        var request = new GenericRequest("place.php?whichplace=town_wrong");
        request.responseText = html("request/test_visit_town_wrong.html");
        QuestManager.handleQuestChange(request);

        assertThat("speakeasyName", isSetTo("Veracity's Place"));
      }
    }

    @Test
    public void canParseSpeakeasyBeingNotFree() {
      var request = new GenericRequest("place.php?whichplace=speakeasy");
      request.responseText = html("request/test_speakeasy_brawl_(1).html");
      QuestManager.handleQuestChange(request);
      assertEquals(Preferences.getInteger("_speakeasyFreeFights"), 3);
    }

    @Test
    public void canParseSpeakeasyBeingFree() {
      var request = new GenericRequest("place.php?whichplace=speakeasy");
      request.responseText = html("request/test_speakeasy_brawl_(0).html");
      QuestManager.handleQuestChange(request);
      assertEquals(Preferences.getInteger("_speakeasyFreeFights"), 0);
    }
  }

  @Nested
  class ElfGratitude {
    @Test
    public void canDetectElfGratitudeFromQuestLog() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups = new Cleanups(withHttpClientBuilder(builder), withProperty("elfGratitude", 0));
      try (cleanups) {
        client.addResponse(200, html("request/test_questlog_elf_gratitude.html"));
        client.addResponse(200, ""); // api.php

        var request = new GenericRequest("questlog.php?which=3");
        request.run();

        assertThat("elfGratitude", isSetTo(219));

        var requests = client.getRequests();
        assertThat(requests, hasSize(1));

        assertPostRequest(requests.get(0), "/questlog.php", "which=3");
      }
    }
  }

  @Test
  public void canDefeatSuperconductor() {
    var builder = new FakeHttpClientBuilder();
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withProperty("superconductorDefeated", false),
            withLastLocation("Crimbo Train (Locomotive)"));
    try (cleanups) {
      builder.client.addResponse(
          302, Map.of("location", List.of("fight.php?ireallymeanit=1671907453")), "");
      builder.client.addResponse(200, html("request/test_fight_superconductor_1.html"));
      builder.client.addResponse(200, ""); // api.php
      builder.client.addResponse(200, html("request/test_fight_superconductor_2.html"));
      builder.client.addResponse(200, ""); // api.php

      var request = new GenericRequest("place.php?whichplace=crimbo22&action=crimbo22_engine");
      request.run();
      request = new GenericRequest("fight.php?action=skill&whichskill=4012");
      request.run();

      assertThat("superconductorDefeated", isSetTo(true));

      var requests = builder.client.getRequests();
      assertThat(requests, hasSize(5));

      assertPostRequest(
          requests.get(0), "/place.php", "whichplace=crimbo22&action=crimbo22_engine");
      assertGetRequest(requests.get(1), "/fight.php", "ireallymeanit=1671907453");
      assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
      assertPostRequest(requests.get(3), "/fight.php", "action=skill&whichskill=4012");
      assertPostRequest(requests.get(4), "/api.php", "what=status&for=KoLmafia");
    }
  }

  @Nested
  class UhOhs {
    @Test
    public void doNotProgressQuestManorOnUhOh() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.MANOR, QuestDatabase.UNSTARTED),
              withProperty("lastSecondFloorUnlock", -1));
      try (cleanups) {
        builder.client.addResponse(200, html("request/test_quest_manor11_uhoh.html"));

        var request = new GenericRequest("place.php?whichplace=manor4", false);
        request.run();

        assertThat(Quest.MANOR, isUnstarted());
        assertThat("lastSecondFloorUnlock", isSetTo(-1));
      }
    }

    @Test
    public void doNotProgressQuestPyramidOnUhOh() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.PYRAMID, QuestDatabase.UNSTARTED));
      try (cleanups) {
        builder.client.addResponse(200, html("request/test_quest_pyramid_uhoh.html"));

        var request = new GenericRequest("place.php?whichplace=pyramid", false);
        request.run();

        assertThat(Quest.PYRAMID, isUnstarted());
      }
    }
  }

  @Nested
  class WizardOfEgo {
    @ParameterizedTest
    @CsvSource({"staring_into_nothing, 4", "into_the_maw_of_deepness, 5", "take_a_dusty_look, 6"})
    public void canParseTowerRuinsChoices(String html, int expectedResult) {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.EGO, QuestDatabase.UNSTARTED));
      try (cleanups) {
        client.addResponse(200, html("request/test_adventure_tower_ruins_" + html + ".html"));
        var request = new GenericRequest("adventure.php?snarfblat=22");
        request.run();
        assertThat(Quest.EGO, isStep(expectedResult));
      }
    }

    @Test
    public void doNotUnfinishQuest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder), withQuestProgress(Quest.EGO, QuestDatabase.FINISHED));
      try (cleanups) {
        client.addResponse(
            200, html("request/test_adventure_tower_ruins_staring_into_nothing.html"));
        var request = new GenericRequest("adventure.php?snarfblat=22");
        request.run();
        assertThat(Quest.EGO, isStep(QuestDatabase.FINISHED));
      }
    }

    @Test
    public void canSetQuestProgressMinimum() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.EGO, QuestDatabase.UNSTARTED));
      try (cleanups) {
        client.addResponse(200, html("request/test_fight_bread_golem.html"));
        var request = new GenericRequest("adventure.php?snarfblat=22");
        request.run();
        assertThat(Quest.EGO, isStep(3));
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"started", "step1", "step2", "step3", "step4"})
  public void canHandleBatholeChange(String step) {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder), withQuestProgress(Quest.BAT, QuestDatabase.UNSTARTED));
    try (cleanups) {
      client.addResponse(200, html("request/test_bathole_" + step + ".html"));
      var request = new GenericRequest("place.php?whichplace=bathole");
      request.run();
      assertThat(Quest.BAT, isStep(step));
    }
  }

  @Nested
  class PixelRealm {
    public static Stream<Arguments> pixelMonsterIncrements() {
      return Stream.of(
          Arguments.of("fleaman", "fleaman", "Vanya's Castle", 4, 5),
          Arguments.of("Blader", "blader", "Megalo-City", 3, 4),
          Arguments.of("Buzzy Beetle", "buzzy_beetle", "The Fungus Plains", 2, 3),
          Arguments.of("Zol", "zol", "Hero's Field", 1, 2));
    }

    public static Stream<Arguments> pixelMonsterNoIncrements() {
      return Stream.of(
          Arguments.of(
              "Tektite",
              "request/test_loss_tektite.html",
              "Hero's Field",
              "skill&whichskill=17047",
              1),
          Arguments.of("Keese", "request/test_freerun_keese.html", "Hero's Field", "runaway", 2));
    }

    @ParameterizedTest
    @MethodSource("pixelMonsterIncrements")
    public void canTrack8BitBonusTurns(
        String monsterName,
        String response,
        String location,
        int startingValue,
        int expectedResult) {
      var cleanups =
          new Cleanups(withLastLocation(location), withProperty("8BitBonusTurns", startingValue));
      try (cleanups) {
        String URL = "fight.php?action=attack";
        String html = html("request/test_fight_" + response + ".html");
        FightRequest.registerRequest(true, URL);
        FightRequest.updateCombatData(URL, monsterName, html);
        assertThat("8BitBonusTurns", isSetTo(expectedResult));
      }
    }

    @ParameterizedTest
    @MethodSource("pixelMonsterNoIncrements")
    public void doNotTrack8BitBonusTurns(
        String monsterName, String response, String location, String action, int startingValue) {
      var cleanups =
          new Cleanups(withLastLocation(location), withProperty("8BitBonusTurns", startingValue));
      try (cleanups) {
        String URL = "fight.php?action=" + action;
        String html = html(response);
        FightRequest.registerRequest(true, URL);
        FightRequest.updateCombatData(URL, monsterName, html);
        assertThat("8BitBonusTurns", isSetTo(startingValue));
      }
    }
  }

  @Nested
  class FantasyRealm {
    @Test
    public void canTrackBarrowWraith() {
      var cleanups =
          new Cleanups(
              withLastLocation("The Barrow Mounds"), withProperty("_frMonstersKilled", ""));
      try (cleanups) {
        String responseText = html("request/test_barrow_wraith_win.html");
        QuestManager.updateQuestData(responseText, monsterData("barrow wraith?"));
        assertEquals(Preferences.getString("_frMonstersKilled"), "barrow wraith?:1,");
      }
    }
  }

  @Nested
  class CyberRealm {
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    public void canDetectWhenZoneFinished(int level) {
      var builder = new FakeHttpClientBuilder();
      int snarfblat =
          switch (level) {
            case 1 -> AdventurePool.CYBER_ZONE_1;
            case 2 -> AdventurePool.CYBER_ZONE_2;
            case 3 -> AdventurePool.CYBER_ZONE_3;
            default -> 0;
          };
      String property = "_cyberZone" + level + "Turns";
      String html = html("request/test_adventure_hacked_cyberrealm_zone1.html");
      var cleanups = new Cleanups(withHttpClientBuilder(builder), withProperty(property, 10));
      try (cleanups) {
        builder.client.addResponse(200, html);
        var request = new GenericRequest("adventure.php?snarfblat=" + snarfblat, true);
        request.run();
        assertThat(property, isSetTo(20));
      }
    }
  }

  @Nested
  class ServerRoom {
    @Test
    public void canParseFileDrawer() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("_cyberZone1Owner", ""),
              withProperty("_cyberZone1Defense", ""),
              withProperty("_cyberZone1Hacker", ""),
              withProperty("_cyberZone2Owner", ""),
              withProperty("_cyberZone2Defense", ""),
              withProperty("_cyberZone2Hacker", ""),
              withProperty("_cyberZone3Owner", ""),
              withProperty("_cyberZone3Defense", ""),
              withProperty("_cyberZone3Hacker", ""));
      try (cleanups) {
        builder.client.addResponse(200, html("request/test_place_serverroom_filedrawer.html"));
        var request = new PlaceRequest("serverroom", "serverroom_filedrawer");
        request.run();
        assertThat("_cyberZone1Owner", isSetTo("Century-Price Quasi-Marketing Companies"));
        assertThat("_cyberZone1Defense", isSetTo("null container"));
        assertThat("_cyberZone1Hacker", isSetTo("greyhat hacker"));
        assertThat("_cyberZone2Owner", isSetTo("Taking Compu-Equipment"));
        assertThat("_cyberZone2Defense", isSetTo("parental controls"));
        assertThat("_cyberZone2Hacker", isSetTo("greyhat hacker"));
        assertThat("_cyberZone3Owner", isSetTo("United Kingdom Compu-Industry"));
        assertThat("_cyberZone3Defense", isSetTo("ICE barrier"));
        assertThat("_cyberZone3Hacker", isSetTo("redhat hacker"));
      }
    }
  }
}
