package net.sourceforge.kolmafia.session;

import static internal.helpers.HttpClientWrapper.setupFakeClient;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withAscensions;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withGender;
import static internal.helpers.Player.withHandlingChoice;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withLastLocation;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withPasswordHash;
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
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.PlaceRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class QuestManagerTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("QuestManager");
    Preferences.reset("QuestManager");
    KoLConstants.inventory.clear();
  }

  /*
   * Council
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

  /*
   * Kingdom-Wide Unlocks
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
   * Azazel Quest
   */
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

  /*
   * Citadel Quest
   */
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

  /*
   * Clancy Quest
   */
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

  /*
   * Garbage Quest
   */
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

  /*
   * Palindome Quest
   */
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

  /*
   * Pirate Quest
   */
  @Test
  void canDetectPirateFinishedInPoopDeck() {
    var request = new GenericRequest("adventure.php?snarfblat=159");
    request.responseText = html("request/test_adventure_poop_deck_its_always_swordfish.html");
    QuestManager.handleQuestChange(request);
    assertThat(Quest.PIRATE, isFinished());
  }

  /*
   * Pyramid Quest
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
        QuestManager.updateQuestData(responseText, "giant giant giant centipede");
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
              withEquipped(EquipmentManager.OFFHAND, "UV-resistant compass"));
      try (cleanups) {
        KoLAdventure.setLastAdventure("The Arid, Extra-Dry Desert");
        assertEquals(KoLAdventure.lastAdventureId(), AdventurePool.ARID_DESERT);
        QuestManager.updateQuestData(responseText, "plaque of locusts");
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
              withEquipped(EquipmentManager.WEAPON, "survival knife"),
              withEffect("Ultrahydrated"));
      try (cleanups) {
        KoLAdventure.setLastAdventure("The Arid, Extra-Dry Desert");
        assertEquals(KoLAdventure.lastAdventureId(), AdventurePool.ARID_DESERT);
        QuestManager.updateQuestData(responseText, "rock scorpion");
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
              withEquipped(EquipmentManager.WEAPON, "survival knife"),
              withEquipped(EquipmentManager.OFFHAND, "UV-resistant compass"),
              withEffect("Ultrahydrated"));
      try (cleanups) {
        KoLAdventure.setLastAdventure("The Arid, Extra-Dry Desert");
        assertEquals(KoLAdventure.lastAdventureId(), AdventurePool.ARID_DESERT);
        QuestManager.updateQuestData(responseText, "giant giant giant centipede");
        assertTrue(responseText.contains("Desert exploration <b>+4%</b>"));
        assertEquals(Preferences.getInteger("desertExploration"), 24);
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
        QuestManager.updateQuestData(responseText, "giant giant giant centipede");
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
              withEquipped(EquipmentManager.OFFHAND, "UV-resistant compass"));
      try (cleanups) {
        KoLAdventure.setLastAdventure("The Arid, Extra-Dry Desert");
        assertEquals(KoLAdventure.lastAdventureId(), AdventurePool.ARID_DESERT);
        QuestManager.updateQuestData(responseText, "rock scorpion");
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
              withEquipped(EquipmentManager.WEAPON, "survival knife"),
              withEffect("Ultrahydrated"));
      try (cleanups) {
        KoLAdventure.setLastAdventure("The Arid, Extra-Dry Desert");
        assertEquals(KoLAdventure.lastAdventureId(), AdventurePool.ARID_DESERT);
        QuestManager.updateQuestData(responseText, "giant giant giant centipede");
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
              withEquipped(EquipmentManager.OFFHAND, "UV-resistant compass"),
              withEquipped(EquipmentManager.WEAPON, "survival knife"),
              withEffect("Ultrahydrated"));
      try (cleanups) {
        KoLAdventure.setLastAdventure("The Arid, Extra-Dry Desert");
        assertEquals(KoLAdventure.lastAdventureId(), AdventurePool.ARID_DESERT);
        QuestManager.updateQuestData(responseText, "rock scorpion");
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
              withEquipped(EquipmentManager.WEAPON, "survival knife"));
      try (cleanups) {
        KoLAdventure.setLastAdventure("The Arid, Extra-Dry Desert");
        assertEquals(KoLAdventure.lastAdventureId(), AdventurePool.ARID_DESERT);
        QuestManager.updateQuestData(responseText, "cactuary");
        assertTrue(responseText.contains("Desert exploration <b>+2%</b>"));
        assertEquals(Preferences.getInteger("desertExploration"), 22);
      }
    }
  }

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

    private static final Map<String, Integer> PYRAMID_POSITIONS =
        Map.ofEntries(
            Map.entry("basket", 4),
            Map.entry("first_visit", 1),
            Map.entry("rats_and_basket", 2),
            Map.entry("rubble_and_vending_machine", 3),
            Map.entry("vending_machine_and_rats", 5));

    @ParameterizedTest
    @ValueSource(
        strings = {
          "basket",
          "first_visit",
          "rats_and_basket",
          "rubble_and_vending_machine",
          "vending_machine_and_rats"
        })
    void canDetectPyramidPositionFromPyramid(String pyramidState) {
      var request = new GenericRequest("place.php?whichplace=pyramid");
      request.responseText = html("request/test_place_pyramid_" + pyramidState + ".html");
      QuestManager.handleQuestChange(request);

      assertThat("pyramidPosition", isSetTo(PYRAMID_POSITIONS.get(pyramidState)));
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
   * Ron Quest
   */
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

  /*
   * Spookyraven Dance Quest
   */
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
    request.responseText = html("request/test_place_spookyraven_second_floor_attic_unlocked.html");
    QuestManager.handleQuestChange(request);
    assertThat(Quest.SPOOKYRAVEN_DANCE, isFinished());
  }

  /*
   * Spookyraven Necklace Quest
   */
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
    QuestManager.updateQuestData("anything", "writing desk");
    assertThat("writingDesksDefeated", isSetTo(1));
  }

  @Test
  public void doesNotTrackWritingDesksFoughtBeforeQuest() {
    QuestDatabase.setQuest(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.UNSTARTED);
    QuestManager.updateQuestData("anything", "writing desk");
    assertThat("writingDesksDefeated", isSetTo(0));
  }

  @Test
  public void doesNotTrackWritingDesksFoughtAfterQuest() {
    QuestDatabase.setQuest(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.FINISHED);
    QuestManager.updateQuestData("anything", "writing desk");
    assertThat("writingDesksDefeated", isSetTo(0));
  }

  @Test
  public void doesNotTrackWritingDesksFoughtAfterNecklace() {
    var cleanups = withItem(ItemPool.SPOOKYRAVEN_NECKLACE);

    try (cleanups) {
      QuestManager.updateQuestData("anything", "writing desk");
      assertThat("writingDesksDefeated", isSetTo(0));
    }
  }

  @Test
  public void canDetectSpookyravenNecklaceFinishedTalkingToLadyS() {
    var request = new GenericRequest("place.php?whichplace=manor1&action=manor1_ladys");
    request.responseText = html("request/test_place_spookyraven_first_floor_receive_necklace.html");
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

  /*
   * Swamp Quest
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
   * Topping Quest
   */
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
  public void canTrackOilPeakProgressFightingSlick() {
    String responseText = html("request/test_fight_oil_slick.html");
    QuestManager.updateQuestData(responseText, "oil slick");
    assertThat("oilPeakProgress", isSetTo(304.32f));
  }

  @Test
  public void canTrackOilPeakProgressFightingTycoon() {
    String responseText = html("request/test_fight_oil_tycoon.html");
    QuestManager.updateQuestData(responseText, "oil tycoon");
    assertThat("oilPeakProgress", isSetTo(291.64f));
  }

  @Test
  public void canTrackOilPeakProgressFightingBaron() {
    String responseText = html("request/test_fight_oil_baron.html");
    QuestManager.updateQuestData(responseText, "oil baron");
    assertThat("oilPeakProgress", isSetTo(278.96f));
  }

  @Test
  public void canTrackOilPeakProgressFightingCartel() {
    String responseText = html("request/test_fight_oil_cartel.html");
    QuestManager.updateQuestData(responseText, "oil cartel");
    assertThat("oilPeakProgress", isSetTo(247.26f));
  }

  @Test
  public void canTrackOilPeakProgressWearingDressPants() {
    withEquipped(EquipmentManager.PANTS, "dress pants");
    String responseText = html("request/test_fight_oil_tycoon.html");
    QuestManager.updateQuestData(responseText, "oil tycoon");
    assertThat("oilPeakProgress", isSetTo(285.3f));
  }

  @Test
  public void canTrackOilPeakProgressWithLoveOilBeetle() {
    String responseText = html("request/test_fight_oil_slick_love_oil_beetle_proc.html");
    QuestManager.updateQuestData(responseText, "oil slick");
    assertThat("oilPeakProgress", isSetTo(297.98f));
  }

  @Test
  public void canDetectOilPeakFinishedInOilPeak() {
    var request = new GenericRequest("adventure.php?snarfblat=298");
    request.responseText = html("request/test_adventure_oil_peak_unimpressed_with_pressure.html");
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
    QuestManager.updateQuestData("anything", monsterName);
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

  /*
   * Trapper Quest
   */
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
    QuestManager.updateQuestData("anything", "panicking Knott Yeti");
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

  /*
   * Shen Quest
   */

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

  /*
   * Non-Quest Related
   */

  /*
   * Arrrbor Day
   */
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

  @Test
  void canDetectUnlockingArmoryInSpookyBunker() {
    var cleanups = withItem("armory keycard");
    try (cleanups) {
      assertThat("armoryUnlocked", isSetTo(false));
      assertThat("canteenUnlocked", isSetTo(false));
      assertThat("SHAWARMAInitiativeUnlocked", isSetTo(false));

      var request =
          new GenericRequest("place.php?whichplace=airport_spooky_bunker&action=si_shop3locked");
      request.responseText = html("request/test_place_airport_spooky_bunker_unlocking_armory.html");
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

  /*
   * Spacegate
   */
  @ParameterizedTest
  @ValueSource(strings = {"place.php?whichplace=spacegate", "adventure.php?snarfblat=494"})
  void justBeingInSpacegateWithoutPermanentAccessMeansDaypass() {
    assertThat("spacegateAlways", isSetTo(false));
    assertThat("_spacegateToday", isSetTo(false));

    var request = new GenericRequest("place.php?whichplace=spacegate");
    request.responseText = "anything";
    QuestManager.handleQuestChange(request);

    assertThat("_spacegateToday", isSetTo(true));
  }

  @ParameterizedTest
  @ValueSource(strings = {"place.php?whichplace=spacegate", "adventure.php?snarfblat=494"})
  void justBeingInSpacegateWithPermanentAccessDoesNotMeanDaypass() {
    Preferences.setBoolean("spacegateAlways", true);
    assertThat("_spacegateToday", isSetTo(false));

    var request = new GenericRequest("place.php?whichplace=spacegate");
    request.responseText = "anything";
    QuestManager.handleQuestChange(request);

    assertThat("_spacegateToday", isSetTo(false));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"choice.php?forceoption=0", "place.php?whichplace=spacegate&action=sg_Terminal"})
  void canParseSpacegateTerminal(String url) {
    var request = new GenericRequest(url);
    request.responseText = html("request/test_spacegate_terminal_earddyk.html");
    QuestManager.handleQuestChange(request);

    assertThat("_spacegatePlanetName", isSetTo("Diverticulus Isaacson IX"));
    assertThat("_spacegateCoordinates", isSetTo("EARDDYK"));
    assertThat("_spacegatePlanetIndex", isSetTo(4));
    assertThat("_spacegateHazards", isSetTo("high winds"));
    assertThat("_spacegatePlantLife", isSetTo("primitive"));
    assertThat("_spacegateAnimalLife", isSetTo("primitive (hostile)"));
    assertThat("_spacegateIntelligentLife", isSetTo("none detected"));
    assertThat("_spacegateSpant", isSetTo(false));
    assertThat("_spacegateMurderbot", isSetTo(false));
    assertThat("_spacegateRuins", isSetTo(false));
    assertThat("_spacegateTurnsLeft", isSetTo(20));
  }

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
        request.constructURLString("choice.php?forceoption=0");
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
        assertPostRequest(requests.get(0), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(1), "/choice.php", "pwd=&whichchoice=147&option=3");
      }
    }

    @Test
    public void canDetectDuckAreaCleared() {
      var cleanups =
          new Cleanups(
              withProperty("duckAreasCleared", ""),
              withLastLocation(AdventureDatabase.getAdventureByName("McMillicancuddy's Granary")));
      try (cleanups) {
        var request = new GenericRequest("adventure.php?snarfblat=" + AdventurePool.THE_GRANARY);
        request.responseText = html("request/test_no_more_ducks.html");
        QuestManager.handleQuestChange(request);
        assertEquals(
            Preferences.getString("duckAreasCleared"), String.valueOf(AdventurePool.THE_GRANARY));
      }
    }
  }

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
        var request = new GenericRequest("place.php?whichplace=" + place);
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
        assertPostRequest(requests.get(0), "/place.php", "whichplace=" + place);
      }
    }

    @ParameterizedTest
    @CsvSource({
      "daycareOpen, _daycareToday",
      "neverendingPartyAlways, _neverendingPartyToday",
      "loveTunnelAvailable, _loveTunnelToday"
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
      "gingerbreadCityAvailable, _gingerbreadCityToday"
    })
    public void checkDayPassesInMountains(String always, String today) {
      var html = html("request/test_visit_mountains.html");
      // If we have always access, we don't have today access
      checkDayPasses("mountains", html, true, always, today);
      // If we don't have always access, we have today access
      checkDayPasses("mountains", html, false, always, today);
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
      var builder = new FakeHttpClientBuilder();

      var cleanup =
          new Cleanups(
              withItem(ItemPool.FORGED_ID_DOCUMENTS),
              withItem(ItemPool.MACGUFFIN_DIARY),
              withGender(KoLCharacter.FEMALE),
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.BLACK, "step2"),
              withProperty("autoQuest", true),
              withLastLocation("The Shore, Inc. Travel Agency"),
              withPasswordHash("TEST"));

      try (cleanup) {
        // class net.sourceforge.kolmafia.request.RelayRequest
        // adventure.php?snarfblat=355
        // 302 location = [choice.php?forceoption=0]

        builder.client.addResponse(200, html("request/test_vacation_with_forged_id.html"));
        var request = new RelayRequest(false);
        request.constructURLString("choice.php?forceoption=0");
        request.run();

        builder.client.addResponse(200, html("request/test_vacation_get_diary.html"));
        request = new RelayRequest(false);
        request.constructURLString("choice.php?pwd&whichchoice=793&option=2");
        request.run();

        assertThat(Quest.BLACK, isStep(3));

        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(3));

        assertPostRequest(requests.get(0), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(1), "/choice.php", "pwd=&whichchoice=793&option=2");
        assertPostRequest(requests.get(2), "/diary.php", "textversion=1&pwd=TEST");
      }
    }

    @Test
    public void willUseVolcanoMapWhenAcquired() {
      var builder = new FakeHttpClientBuilder();

      var cleanup =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("autoQuest", true),
              withLastLocation("Madness Bakery"),
              withPasswordHash("TEST"));

      try (cleanup) {
        builder.client.addResponse(200, html("request/test_fight_volcano_map.html"));
        var request = new RelayRequest(false);
        request.constructURLString("fight.php?action=skill&whichskill=4012");
        request.run();

        // Redirected: inventory.php?which=3&action=message
        // responseText = ...

        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/fight.php", "action=skill&whichskill=4012");
        assertPostRequest(
            requests.get(1),
            "/inv_use.php",
            "which=3&whichitem=" + ItemPool.VOLCANO_MAP + "&pwd=TEST");
      }
    }
  }
}
