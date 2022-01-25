package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.addItem;
import static internal.helpers.Player.countItem;
import static internal.helpers.Player.equip;
import static internal.helpers.Preference.hasIntegerValue;
import static internal.helpers.Preference.isSetTo;
import static internal.helpers.Quest.isFinished;
import static internal.helpers.Quest.isStarted;
import static internal.helpers.Quest.isStep;
import static internal.helpers.Quest.isUnstarted;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
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
  public void canParseABigBusyCouncilPage() throws IOException {
    var request = new GenericRequest("council.php");
    request.responseText =
        Files.readString(Path.of("request/test_council_first_visit_level_13.html"));
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
  public void canParseLarvaReturn() throws IOException {
    addItem("mosquito larva");

    var request = new GenericRequest("council.php");
    request.responseText = Files.readString(Path.of("request/test_council_hand_in_larva.html"));
    QuestManager.handleQuestChange(request);

    assertThat(Quest.LARVA, isFinished());
    assertThat(countItem("enchanted bean"), is(0));
  }

  /*
   * Kingdom-Wide Unlocks
   */
  @Test
  public void seeingTheIslandMarksItAsUnlocked() throws IOException {
    var ascension = 50;
    KoLCharacter.setAscensions(ascension);
    assertThat("lastIslandUnlock", hasIntegerValue(lessThan(ascension)));
    var request = new GenericRequest("main.php");
    request.responseText = Files.readString(Path.of("request/test_main_island.html"));
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
  void canDetectCitadelStep1InGrove() throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=100");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_whiteys_grove_its_a_sign.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.CITADEL, isStep(1));
  }

  @Test
  void canDetectCitadelStep2InWhiteCitadel() throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=413");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_white_citadel_they_arent_blind.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.CITADEL, isStep(2));
  }

  @Test
  void canDetectCitadelStep3InWhiteCitadel() throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=413");
    request.responseText =
        Files.readString(
            Path.of("request/test_adventure_white_citadel_existential_blues_brothers.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.CITADEL, isStep(3));
  }

  /*
   * Clancy Quest
   */
  @Test
  void canDetectClancyStep1InBarroom() throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=233");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_barroom_brawl_jackin_the_jukebox.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.CLANCY, isStep(1));
  }

  @Test
  void canDetectClancyStep3InKnobShaft() throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=101");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_knob_shaft_a_miner_variation.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.CLANCY, isStep(3));
  }

  @Test
  void canDetectClancyStep7InIcyPeak() throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=110");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_icy_peak_mercury_rising.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.CLANCY, isStep(7));
  }

  @Test
  void canDetectClancyFinishInMiddleChamber() throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=407");
    request.responseText =
        Files.readString(
            Path.of("request/test_adventure_middle_chamber_dont_you_know_who_i_am.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.CLANCY, isFinished());
  }

  /*
   * Garbage Quest
   */
  @Test
  void canDetectGarbageStep1InPlains() throws IOException {
    var request = new GenericRequest("place.php?whichplace=plains");
    request.responseText = Files.readString(Path.of("request/test_place_plains_beanstalk.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.GARBAGE, isStep(1));
  }

  @Test
  public void deductsEnchantedBeanWhenPlanting() throws IOException {
    addItem("enchanted bean");
    assertEquals(1, countItem("enchanted bean"));
    var request = new GenericRequest("place.php?whichplace=plains");
    request.responseText = Files.readString(Path.of("request/test_place_plains_beanstalk.html"));
    QuestManager.handleQuestChange(request);
    assertEquals(0, countItem("enchanted bean"));
  }

  @Test
  public void justBeingInAirshipIsGarbageStep1() {
    var request = new GenericRequest("adventure.php?snarfblat=81");
    request.responseText = "anything";
    QuestManager.handleQuestChange(request);
    assertThat(Quest.GARBAGE, isStep(1));
  }

  @Test
  public void canDetectGarbageStep2InAirship() throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=81");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_airship_beginning_of_the_end.html"));
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
  public void canDetectGarbageStep8InCastleBasement() throws IOException {
    var ascension = 50;
    KoLCharacter.setAscensions(ascension);
    assertThat("lastCastleGroundUnlock", hasIntegerValue(lessThan(ascension)));
    var request = new GenericRequest("adventure.php?snarfblat=322");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_castle_basement_unlock_ground.html"));
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
  public void canDetectGarbageStep9InCastleFirstFloor() throws IOException {
    var ascension = 50;
    KoLCharacter.setAscensions(ascension);
    assertThat("lastCastleTopUnlock", hasIntegerValue(lessThan(ascension)));
    var request = new GenericRequest("adventure.php?snarfblat=323");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_castle_first_top_of_the_castle_ma.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.GARBAGE, isStep(9));
    assertThat("lastCastleTopUnlock", isSetTo(ascension));
  }

  @Test
  public void failingToAdventureInCastleTopFloorDoesNotAdvanceStep9() throws IOException {
    QuestDatabase.setQuestIfBetter(Quest.GARBAGE, "step8");
    var request = new GenericRequest("adventure.php?snarfblat=323");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_castle_top_floor_walk_before_fly.html"));
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
  void canDetectPalindomeStartedInPlains() throws IOException {
    var request = new GenericRequest("place.php?whichplace=plains");
    request.responseText = Files.readString(Path.of("request/test_place_plains_palindome.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.PALINDOME, isStarted());
  }

  @Test
  void canDetectPalindomeStep3InPalindome() throws IOException {
    var request = new GenericRequest("place.php?whichplace=palindome&action=pal_mr");
    request.responseText =
        Files.readString(Path.of("request/test_place_palindome_meet_mr_alarm.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.PALINDOME, isStep(3));
  }

  /*
   * Pirate Quest
   */
  @Test
  void canDetectPirateFinishedInPoopDeck() throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=159");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_poop_deck_its_always_swordfish.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.PIRATE, isFinished());
  }

  /*
   * Pyramid Quest
   */
  @Test
  void canDetectPyramidStartedFromBeach() throws IOException {
    var request = new GenericRequest("place.php?whichplace=desertbeach&action=db_pyramid1");
    request.responseText =
        Files.readString(Path.of("request/test_place_desert_beach_uncover_pyramid.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.PYRAMID, isStarted());
  }

  @Test
  void justBeingInPyramidIsPyramidStarted() throws IOException {
    var request = new GenericRequest("place.php?whichplace=pyramid");
    request.responseText = Files.readString(Path.of("request/test_place_pyramid_first_visit.html"));
    QuestManager.handleQuestChange(request);

    assertThat(Quest.PYRAMID, isStarted());
  }

  @Test
  void canDetectPyramidStep1FromUpperChamber() throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=406");
    request.responseText =
        Files.readString(
            Path.of("request/test_adventure_upper_chamber_down_dooby_doo_down_down.html"));
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
  void canDetectPyramidStep1FromPyramid() throws IOException {
    var request = new GenericRequest("place.php?whichplace=pyramid");
    request.responseText =
        Files.readString(Path.of("request/test_place_pyramid_unlocked_middle_chamber.html"));
    QuestManager.handleQuestChange(request);

    assertThat(Quest.PYRAMID, isStep(1));
    assertThat("middleChamberUnlock", isSetTo(true));
    assertThat("lowerChamberUnlock", isSetTo(false));
    assertThat("controlRoomUnlock", isSetTo(false));
  }

  @Test
  void canDetectPyramidStep2FromMiddleChamber() throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=407");
    request.responseText =
        Files.readString(
            Path.of("request/test_adventure_middle_chamber_further_down_dooby_doo_down_down.html"));
    QuestManager.handleQuestChange(request);

    assertThat(Quest.PYRAMID, isStep(2));
    assertThat("lowerChamberUnlock", isSetTo(true));
  }

  @Test
  void canDetectPyramidStep2FromPyramid() throws IOException {
    var request = new GenericRequest("place.php?whichplace=pyramid");
    request.responseText =
        Files.readString(Path.of("request/test_place_pyramid_unlocked_lower_chamber.html"));
    QuestManager.handleQuestChange(request);

    assertThat(Quest.PYRAMID, isStep(2));
    assertThat("middleChamberUnlock", isSetTo(true));
    assertThat("lowerChamberUnlock", isSetTo(true));
    assertThat("controlRoomUnlock", isSetTo(false));
  }

  @Test
  void canDetectPyramidStep3FromMiddleChamber() throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=407");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_middle_chamber_under_control.html"));
    QuestManager.handleQuestChange(request);

    assertThat(Quest.PYRAMID, isStep(3));
    assertThat("controlRoomUnlock", isSetTo(true));
    assertThat("pyramidPosition", isSetTo(1));
  }

  @Test
  void canDetectPyramidStep3FromPyramid() throws IOException {
    var request = new GenericRequest("place.php?whichplace=pyramid");
    request.responseText =
        Files.readString(Path.of("request/test_place_pyramid_unlocked_control_room.html"));
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
  void canDetectPyramidPositionFromPyramid(String pyramidState) throws IOException {
    var request = new GenericRequest("place.php?whichplace=pyramid");
    request.responseText =
        Files.readString(Path.of("request/test_place_pyramid_" + pyramidState + ".html"));
    QuestManager.handleQuestChange(request);

    assertThat("pyramidPosition", isSetTo(PYRAMID_POSITIONS.get(pyramidState)));
  }

  @Test
  void canDetectPyramidBombUsedFromPyramidAction() throws IOException {
    var request = new GenericRequest("place.php?whichplace=pyramid&action=pyramid_state1");
    request.responseText = Files.readString(Path.of("request/test_place_pyramid_bomb_rubble.html"));
    QuestManager.handleQuestChange(request);

    assertThat("pyramidBombUsed", isSetTo(true));
  }

  @Test
  void canDetectPyramidBombUsedFromPyramid() throws IOException {
    var request = new GenericRequest("place.php?whichplace=pyramid");
    request.responseText =
        Files.readString(Path.of("request/test_place_pyramid_unlocked_tomb.html"));
    QuestManager.handleQuestChange(request);

    assertThat("pyramidBombUsed", isSetTo(true));
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
  public void canDetectRonStep2InProtestors() throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=384");
    request.responseText =
        Files.readString(
            Path.of("request/test_adventure_protestors_not_so_much_with_the_humanity.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.RON, isStep(2));
  }

  @Test
  public void seeingClearedProtestorsSetsRonStep2() throws IOException {
    var request = new GenericRequest("place.php?whichplace=zeppelin");
    request.responseText =
        Files.readString(Path.of("request/test_place_zeppelin_cleared_protestors.html"));
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
  public void canDetectRonStep3InZeppelin() throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=385");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_zeppelin_zeppelintro.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.RON, isStep(3));
  }

  /*
   * Spookyraven Dance Quest
   */
  @Test
  public void canDetectSpookyravenDanceStep1TalkingToLadyS() throws IOException {
    var request = new GenericRequest("place.php?whichplace=manor2&action=manor2_ladys");
    request.responseText =
        Files.readString(Path.of("request/test_place_spookyraven_second_floor_talk_to_ladys.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.SPOOKYRAVEN_DANCE, isStep(1));
  }

  @Test
  public void canDetectSpookyravenDanceStep3InSpookyravenSecondFloor() throws IOException {
    var request = new GenericRequest("place.php?whichplace=manor2");
    request.responseText =
        Files.readString(
            Path.of("request/test_place_spookyraven_second_floor_ballroom_unlocked.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.SPOOKYRAVEN_DANCE, isStep(3));
  }

  @Test
  public void canDetectSpookyravenDanceStepFinishedInBallroom() throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=395");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_spookyraven_ballroom_having_a_ball.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.SPOOKYRAVEN_DANCE, isFinished());
  }

  @Test
  public void canDetectSpookyravenDanceStepFinishedFromStairsToAttic() throws IOException {
    var request = new GenericRequest("place.php?whichplace=manor2");
    request.responseText =
        Files.readString(
            Path.of("request/test_place_spookyraven_second_floor_attic_unlocked.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.SPOOKYRAVEN_DANCE, isFinished());
  }

  /*
   * Spookyraven Necklace Quest
   */
  @Test
  public void canDetectSpookyravenNecklaceStartedInSpookyravenFirstFloor() throws IOException {
    var request = new GenericRequest("place.php?whichplace=manor1");
    request.responseText =
        Files.readString(Path.of("request/test_place_spookyraven_first_floor_quest_started.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.SPOOKYRAVEN_NECKLACE, isStarted());
  }

  @Test
  public void canDetectSpookyravenNecklaceStep2InBilliardsRoom() throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=391");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_billiards_room_thats_your_cue.html"));
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
    addItem(ItemPool.SPOOKYRAVEN_NECKLACE);
    QuestManager.updateQuestData("anything", "writing desk");
    assertThat("writingDesksDefeated", isSetTo(0));
  }

  @Test
  public void canDetectSpookyravenNecklaceFinishedTalkingToLadyS() throws IOException {
    var request = new GenericRequest("place.php?whichplace=manor1&action=manor1_ladys");
    request.responseText =
        Files.readString(
            Path.of("request/test_place_spookyraven_first_floor_receive_necklace.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.SPOOKYRAVEN_NECKLACE, isFinished());
  }

  @Test
  public void canDetectSpookyravenNecklaceFinishedInSpookyravenFirstFloor() throws IOException {
    var request = new GenericRequest("place.php?whichplace=manor1");
    request.responseText =
        Files.readString(
            Path.of("request/test_place_spookyraven_first_floor_second_floor_unlocked.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.SPOOKYRAVEN_NECKLACE, isFinished());
  }

  @Test
  public void justBeingInSpookyravenSecondFloorIsSpookyravenNecklaceFinished() throws IOException {
    var ascension = 50;
    KoLCharacter.setAscensions(ascension);
    assertThat("lastSecondFloorUnlock", hasIntegerValue(lessThan(ascension)));
    var request = new GenericRequest("place.php?whichplace=manor1");
    request.responseText =
        Files.readString(Path.of("request/test_place_spookyraven_second_floor_first_visit.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.SPOOKYRAVEN_NECKLACE, isFinished());
    assertThat("lastSecondFloorUnlock", isSetTo(ascension));
  }

  /*
   * Swamp Quest
   */
  @Test
  public void canDetectSwapStartedInCanadia() throws IOException {
    assertThat(Quest.SWAMP, isUnstarted());
    var request = new GenericRequest("place.php?whichplace=canadia&action=lc_marty");
    request.responseText = Files.readString(Path.of("request/test_canadia_start_quest.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.SWAMP, isStarted());
  }

  /*
   * Topping Quest
   */
  @Test
  public void canDetectToppingStep1InChasm() throws IOException {
    var request = new GenericRequest("place.php?whichplace=orc_chasm");
    request.responseText =
        Files.readString(Path.of("request/test_place_orc_chasm_bridge_built.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.TOPPING, isStep(1));
  }

  @Test
  public void canDetectToppingStep2InHighlands() throws IOException {
    var request = new GenericRequest("place.php?whichplace=highlands&action=highlands_dude");
    request.responseText =
        Files.readString(Path.of("request/test_place_highlands_meet_highland_lord.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.TOPPING, isStep(2));
  }

  @Test
  public void canTrackOilPeakProgressFightingSlick() throws IOException {
    String responseText = Files.readString(Path.of("request/test_fight_oil_slick.html"));
    QuestManager.updateQuestData(responseText, "oil slick");
    assertThat("oilPeakProgress", isSetTo(304.32f));
  }

  @Test
  public void canTrackOilPeakProgressFightingTycoon() throws IOException {
    String responseText = Files.readString(Path.of("request/test_fight_oil_tycoon.html"));
    QuestManager.updateQuestData(responseText, "oil tycoon");
    assertThat("oilPeakProgress", isSetTo(291.64f));
  }

  @Test
  public void canTrackOilPeakProgressFightingBaron() throws IOException {
    String responseText = Files.readString(Path.of("request/test_fight_oil_baron.html"));
    QuestManager.updateQuestData(responseText, "oil baron");
    assertThat("oilPeakProgress", isSetTo(278.96f));
  }

  @Test
  public void canTrackOilPeakProgressFightingCartel() throws IOException {
    String responseText = Files.readString(Path.of("request/test_fight_oil_cartel.html"));
    QuestManager.updateQuestData(responseText, "oil cartel");
    assertThat("oilPeakProgress", isSetTo(247.26f));
  }

  @Test
  public void canTrackOilPeakProgressWearingDressPants() throws IOException {
    equip(EquipmentManager.PANTS, "dress pants");
    String responseText = Files.readString(Path.of("request/test_fight_oil_tycoon.html"));
    QuestManager.updateQuestData(responseText, "oil tycoon");
    assertThat("oilPeakProgress", isSetTo(285.3f));
  }

  @Test
  public void canTrackOilPeakProgressWithLoveOilBeetle() throws IOException {
    String responseText =
        Files.readString(Path.of("request/test_fight_oil_slick_love_oil_beetle_proc.html"));
    QuestManager.updateQuestData(responseText, "oil slick");
    assertThat("oilPeakProgress", isSetTo(297.98f));
  }

  @Test
  public void canDetectOilPeakFinishedInOilPeak() throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=298");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_oil_peak_unimpressed_with_pressure.html"));
    Files.readString(Path.of("request/test_adventure_oil_peak_unimpressed_with_pressure.html"));
    QuestManager.handleQuestChange(request);
    assertThat("oilPeakProgress", isSetTo(0f));
    assertThat("oilPeakLit", isSetTo(true));
  }

  @Test
  public void canDetectOilPeakFinishedInHighlands() throws IOException {
    var request = new GenericRequest("place.php?whichplace=highlands");
    request.responseText =
        Files.readString(Path.of("request/test_place_highlands_oil_peak_lit.html"));
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
  public void canDetectBooPeakFinishedInBooPeak() throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=296");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_boo_peak_come_on_ghostly.html"));
    QuestManager.handleQuestChange(request);
    assertThat("booPeakProgress", isSetTo(0));
    assertThat("booPeakLit", isSetTo(true));
  }

  @Test
  public void canDetectBooPeakFinishedInHighlands() throws IOException {
    var request = new GenericRequest("place.php?whichplace=highlands");
    request.responseText =
        Files.readString(Path.of("request/test_place_highlands_boo_peak_lit.html"));
    QuestManager.handleQuestChange(request);
    assertThat("booPeakProgress", isSetTo(0));
    assertThat("booPeakLit", isSetTo(true));
  }

  @Test
  public void canDetectToppingStep3InHighlands() throws IOException {
    var request = new GenericRequest("place.php?whichplace=highlands");
    request.responseText =
        Files.readString(Path.of("request/test_place_highlands_all_fires_lit.html"));
    QuestManager.handleQuestChange(request);
    assertThat("twinPeakProgress", isSetTo(15));
    assertThat(Quest.TOPPING, isStep(3));
  }

  @Test
  public void canDetectToppingFinishedInHighlands() throws IOException {
    var request = new GenericRequest("place.php?whichplace=highlands&action=highlands_dude");
    request.responseText =
        Files.readString(Path.of("request/test_place_highlands_revisit_highland_lord.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.TOPPING, isFinished());
  }

  /*
   * Trapper Quest
   */
  @Test
  public void canDetectTrapperStep1InMcLargeHuge() throws IOException {
    var request = new GenericRequest("place.php?whichplace=mclargehuge&action=trappercabin");
    request.responseText =
        Files.readString(Path.of("request/test_place_mclargehuge_trapper_give_quest.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.TRAPPER, isStep(1));
  }

  @Test
  public void canDetectTrapperStep2InMcLargeHuge() throws IOException {
    var request = new GenericRequest("place.php?whichplace=mclargehuge&action=trappercabin");
    request.responseText =
        Files.readString(Path.of("request/test_place_mclargehuge_trapper_get_cheese_and_ore.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.TRAPPER, isStep(2));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"discovering_your_extremity", "2_exxtreme_4_u", "3_exxxtreme_4ever_6pack"})
  public void canDetectExtremityInExtremeSlope(String nonCombat) throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=273");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_extreme_slope_" + nonCombat + ".html"));
    QuestManager.handleQuestChange(request);
    assertThat("currentExtremity", isSetTo(1));
  }

  @Test
  public void canDetectTrapperStep3ViaExtremeInMcLargeHuge() throws IOException {
    Preferences.setInteger("currentExtremity", 3);

    var request = new GenericRequest("place.php?whichplace=mclargehuge&action=cloudypeak");
    request.responseText =
        Files.readString(Path.of("request/test_place_mclargehuge_extreme_peak.html"));
    QuestManager.handleQuestChange(request);

    assertThat(Quest.TRAPPER, isStep(3));
    assertThat("currentExtremity", isSetTo(0));
  }

  @Test
  public void canDetectTrapperStep3ViaNinjaInMcLargeHuge() throws IOException {
    Preferences.setInteger("currentExtremity", 2);
    var request = new GenericRequest("place.php?whichplace=mclargehuge&action=cloudypeak");
    request.responseText =
        Files.readString(Path.of("request/test_place_mclargehuge_ninja_peak.html"));
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
  public void canDetectTrapperFinishedInMcLargeHuge() throws IOException {
    var ascension = 50;
    KoLCharacter.setAscensions(ascension);
    addItem("groar's fur");
    assertThat("lastTr4pz0rQuest", hasIntegerValue(lessThan(ascension)));

    var request = new GenericRequest("place.php?whichplace=mclargehuge&action=trappercabin");
    request.responseText =
        Files.readString(Path.of("request/test_place_mclargehuge_trapper_give_fur.html"));
    QuestManager.handleQuestChange(request);

    assertThat("lastTr4pz0rQuest", isSetTo(ascension));
    assertThat(Quest.TRAPPER, isFinished());
    assertEquals(0, countItem("groar's fur"));
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
  void doesNotTrackArrrborDayNonCombats(String nonCombat) throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=174");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_arrrboretum_" + nonCombat + ".html"));
    QuestManager.handleQuestChange(request);
    assertThat("_saplingsPlanted", isSetTo(0));
  }

  /*
   * Airport
   */
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
              AdventurePool.SLOPPY_SECONDS_DINER, AdventurePool.FUN_GUY_MANSION, AdventurePool.YACHT
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
  void canParseAFullAirport(String element, int[] zones) throws IOException {
    assertThat(element + "AirportAlways", isSetTo(false));
    assertThat("_" + element + "AirportToday", isSetTo(false));

    var request = new GenericRequest("place.php?whichplace=airport");
    request.responseText =
        Files.readString(Path.of("request/test_place_airport_all_charters.html"));
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
  void beingDeniedAccessToAirportLocationDoesNotMeanDaypass(String element, int[] snarfblats)
      throws IOException {
    assertThat(element + "AirportAlways", isSetTo(false));
    assertThat("_" + element + "AirportToday", isSetTo(false));

    var request = new GenericRequest("adventure.php?snarfblat=" + snarfblats[0]);
    request.responseText =
        Files.readString(Path.of("request/test_adventure_that_isnt_a_place.html"));
    QuestManager.handleQuestChange(request);

    assertThat(element, "_" + element + "AirportToday", isSetTo(false));
  }

  @Test
  void canDetectUnlockingArmoryInSpookyBunker() throws IOException {
    addItem("armory keycard");
    assertThat("armoryUnlocked", isSetTo(false));
    assertThat("canteenUnlocked", isSetTo(false));
    assertThat("SHAWARMAInitiativeUnlocked", isSetTo(false));

    var request =
        new GenericRequest("place.php?whichplace=airport_spooky_bunker&action=si_shop3locked");
    request.responseText =
        Files.readString(Path.of("request/test_place_airport_spooky_bunker_unlocking_armory.html"));
    QuestManager.handleQuestChange(request);

    assertEquals(0, countItem("armory keycard"));
    assertThat("armoryUnlocked", isSetTo(true));
    assertThat("canteenUnlocked", isSetTo(false));
    assertThat("SHAWARMAInitiativeUnlocked", isSetTo(false));
  }

  @Test
  void canDetectUnlockingCanteenInSpookyBunker() throws IOException {
    addItem("bottle-opener keycard");
    assertThat("armoryUnlocked", isSetTo(false));
    assertThat("canteenUnlocked", isSetTo(false));
    assertThat("SHAWARMAInitiativeUnlocked", isSetTo(false));

    var request =
        new GenericRequest("place.php?whichplace=airport_spooky_bunker&action=si_shop2locked");
    request.responseText =
        Files.readString(
            Path.of("request/test_place_airport_spooky_bunker_unlocking_canteen.html"));
    QuestManager.handleQuestChange(request);

    assertEquals(0, countItem("bottle-opener keycard"));
    assertThat("armoryUnlocked", isSetTo(false));
    assertThat("canteenUnlocked", isSetTo(true));
    assertThat("SHAWARMAInitiativeUnlocked", isSetTo(false));
  }

  @Test
  void canDetectUnlockingShawarmaInSpookyBunker() throws IOException {
    addItem("SHAWARMA Initiative Keycard");
    assertThat("armoryUnlocked", isSetTo(false));
    assertThat("canteenUnlocked", isSetTo(false));
    assertThat("SHAWARMAInitiativeUnlocked", isSetTo(false));

    var request =
        new GenericRequest("place.php?whichplace=airport_spooky_bunker&action=si_shop1locked");
    request.responseText =
        Files.readString(
            Path.of("request/test_place_airport_spooky_bunker_unlocking_shawarma.html"));
    QuestManager.handleQuestChange(request);

    assertEquals(0, countItem("SHAWARMA Initiative keycard"));
    assertThat("armoryUnlocked", isSetTo(false));
    assertThat("canteenUnlocked", isSetTo(false));
    assertThat("SHAWARMAInitiativeUnlocked", isSetTo(true));
  }

  @Test
  void canDetectEverythingUnlockedInSpookyBunker() throws IOException {
    assertThat("armoryUnlocked", isSetTo(false));
    assertThat("canteenUnlocked", isSetTo(false));
    assertThat("SHAWARMAInitiativeUnlocked", isSetTo(false));

    var request = new GenericRequest("place.php?whichplace=airport_spooky_bunker");
    request.responseText =
        Files.readString(
            Path.of("request/test_place_airport_spooky_bunker_everything_unlocked.html"));
    QuestManager.handleQuestChange(request);

    assertThat("armoryUnlocked", isSetTo(true));
    assertThat("canteenUnlocked", isSetTo(true));
    assertThat("SHAWARMAInitiativeUnlocked", isSetTo(true));
  }

  @Test
  void canDetectNothingUnlockedInSpookyBunker() throws IOException {
    Preferences.setBoolean("armoryUnlocked", true);
    Preferences.setBoolean("canteenUnlocked", true);
    Preferences.setBoolean("SHAWARMAInitiativeUnlocked", true);

    var request = new GenericRequest("place.php?whichplace=airport_spooky_bunker");
    request.responseText =
        Files.readString(Path.of("request/test_place_airport_spooky_bunker_nothing_unlocked.html"));
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
  void canParseSpacegateTerminal(String url) throws IOException {
    var request = new GenericRequest(url);
    request.responseText =
        Files.readString(Path.of("request/test_spacegate_terminal_earddyk.html"));
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
}
