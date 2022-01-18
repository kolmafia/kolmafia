package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.addItem;
import static internal.helpers.Player.countItem;
import static internal.helpers.Player.equip;
import static internal.helpers.Quest.isFinished;
import static internal.helpers.Quest.isStarted;
import static internal.helpers.Quest.isStep;
import static internal.helpers.Quest.isUnstarted;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class QuestManagerTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("QuestManager");
    Preferences.reset("QuestManager");
    KoLConstants.inventory.clear();
  }

  /*
   * Kingdom-Wide Unlocks
   */
  @Test
  public void seeingTheIslandMarksItAsUnlocked() throws IOException {
    var ascension = 50;
    KoLCharacter.setAscensions(ascension);
    assertThat(Preferences.getInteger("lastIslandUnlock"), lessThan(ascension));
    var request = new GenericRequest("main.php");
    request.responseText = Files.readString(Path.of("request/test_main_island.html"));
    QuestManager.handleQuestChange(request);
    assertEquals(ascension, Preferences.getInteger("lastIslandUnlock"));
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
    assertThat(Preferences.getInteger("lastCastleGroundUnlock"), lessThan(ascension));
    var request = new GenericRequest("adventure.php?snarfblat=322");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_castle_basement_unlock_ground.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.GARBAGE, isStep(8));
    assertEquals(ascension, Preferences.getInteger("lastCastleGroundUnlock"));
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
    assertThat(Preferences.getInteger("lastCastleTopUnlock"), lessThan(ascension));
    var request = new GenericRequest("adventure.php?snarfblat=323");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_castle_first_top_of_the_castle_ma.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.GARBAGE, isStep(9));
    assertEquals(ascension, Preferences.getInteger("lastCastleTopUnlock"));
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
    assertEquals(1, Preferences.getInteger("writingDesksDefeated"));
  }

  @Test
  public void doesNotTrackWritingDesksFoughtBeforeQuest() {
    QuestDatabase.setQuest(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.UNSTARTED);
    QuestManager.updateQuestData("anything", "writing desk");
    assertEquals(0, Preferences.getInteger("writingDesksDefeated"));
  }

  @Test
  public void doesNotTrackWritingDesksFoughtAfterQuest() {
    QuestDatabase.setQuest(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.FINISHED);
    QuestManager.updateQuestData("anything", "writing desk");
    assertEquals(0, Preferences.getInteger("writingDesksDefeated"));
  }

  @Test
  public void doesNotTrackWritingDesksFoughtAfterNecklace() {
    addItem(ItemPool.SPOOKYRAVEN_NECKLACE);
    QuestManager.updateQuestData("anything", "writing desk");
    assertEquals(0, Preferences.getInteger("writingDesksDefeated"));
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
    assertThat(Preferences.getInteger("lastSecondFloorUnlock"), lessThan(ascension));
    var request = new GenericRequest("place.php?whichplace=manor1");
    request.responseText =
        Files.readString(Path.of("request/test_place_spookyraven_second_floor_first_visit.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.SPOOKYRAVEN_NECKLACE, isFinished());
    assertEquals(ascension, Preferences.getInteger("lastSecondFloorUnlock"));
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
    assertEquals(304.32f, Preferences.getFloat("oilPeakProgress"));
  }

  @Test
  public void canTrackOilPeakProgressFightingTycoon() throws IOException {
    String responseText = Files.readString(Path.of("request/test_fight_oil_tycoon.html"));
    QuestManager.updateQuestData(responseText, "oil tycoon");
    assertEquals(291.64f, Preferences.getFloat("oilPeakProgress"));
  }

  @Test
  public void canTrackOilPeakProgressFightingBaron() throws IOException {
    String responseText = Files.readString(Path.of("request/test_fight_oil_baron.html"));
    QuestManager.updateQuestData(responseText, "oil baron");
    assertEquals(278.96f, Preferences.getFloat("oilPeakProgress"));
  }

  @Test
  public void canTrackOilPeakProgressFightingCartel() throws IOException {
    String responseText = Files.readString(Path.of("request/test_fight_oil_cartel.html"));
    QuestManager.updateQuestData(responseText, "oil cartel");
    assertEquals(247.26f, Preferences.getFloat("oilPeakProgress"));
  }

  @Test
  public void canTrackOilPeakProgressWearingDressPants() throws IOException {
    equip(EquipmentManager.PANTS, "dress pants");
    String responseText = Files.readString(Path.of("request/test_fight_oil_tycoon.html"));
    QuestManager.updateQuestData(responseText, "oil tycoon");
    assertEquals(285.3f, Preferences.getFloat("oilPeakProgress"));
  }

  @Test
  public void canTrackOilPeakProgressWithLoveOilBeetle() throws IOException {
    String responseText =
        Files.readString(Path.of("request/test_fight_oil_slick_love_oil_beetle_proc.html"));
    QuestManager.updateQuestData(responseText, "oil slick");
    assertEquals(297.98f, Preferences.getFloat("oilPeakProgress"));
  }

  @Test
  public void canDetectOilPeakFinishedInOilPeak() throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=298");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_oil_peak_unimpressed_with_pressure.html"));
    QuestManager.handleQuestChange(request);
    assertEquals(0f, Preferences.getFloat("oilPeakProgress"));
    assertTrue(Preferences.getBoolean("oilPeakLit"));
  }

  @Test
  public void canDetectOilPeakFinishedInHighlands() throws IOException {
    var request = new GenericRequest("place.php?whichplace=highlands");
    request.responseText =
        Files.readString(Path.of("request/test_place_highlands_oil_peak_lit.html"));
    QuestManager.handleQuestChange(request);
    assertEquals(0f, Preferences.getFloat("oilPeakProgress"));
    assertTrue(Preferences.getBoolean("oilPeakLit"));
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
  public void canTrackBooPeakProgress(String monsterName) throws IOException {
    QuestManager.updateQuestData("anything", monsterName);
    assertEquals(98, Preferences.getInteger("booPeakProgress"));
  }

  @Test
  public void canDetectBooPeakFinishedInBooPeak() throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=296");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_boo_peak_come_on_ghostly.html"));
    QuestManager.handleQuestChange(request);
    assertEquals(0, Preferences.getInteger("booPeakProgress"));
    assertTrue(Preferences.getBoolean("booPeakLit"));
  }

  @Test
  public void canDetectBooPeakFinishedInHighlands() throws IOException {
    var request = new GenericRequest("place.php?whichplace=highlands");
    request.responseText =
        Files.readString(Path.of("request/test_place_highlands_boo_peak_lit.html"));
    QuestManager.handleQuestChange(request);
    assertEquals(0, Preferences.getInteger("booPeakProgress"));
    assertTrue(Preferences.getBoolean("booPeakLit"));
  }

  @Test
  public void canDetectToppingStep3InHighlands() throws IOException {
    var request = new GenericRequest("place.php?whichplace=highlands");
    request.responseText =
        Files.readString(Path.of("request/test_place_highlands_all_fires_lit.html"));
    QuestManager.handleQuestChange(request);
    assertEquals(15, Preferences.getInteger("twinPeakProgress"));
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
    assertEquals(1, Preferences.getInteger("currentExtremity"));
  }

  @Test
  public void canDetectTrapperStep3InMcLargeHuge() throws IOException {
    Preferences.setInteger("currentExtremity", 3);
    var request = new GenericRequest("place.php?whichplace=mclargehuge&action=cloudypeak");
    request.responseText =
        Files.readString(Path.of("request/test_place_mclargehuge_extreme_peak.html"));
    QuestManager.handleQuestChange(request);
    assertThat(Quest.TRAPPER, isStep(3));
    assertEquals(0, Preferences.getInteger("currentExtremity"));
  }

  @Test
  public void canDetectTrapperStep4InIcyPeak() {
    QuestManager.updateQuestData("anything", "panicking Knott Yeti");
    assertThat(Quest.TRAPPER, isStep(4));
  }

  /*
   * Non-Quest Related
   */
  @Test
  void tracksArrrborDaySaplingsPlanted() {
    var request = new GenericRequest("adventure.php?snarfblat=174");
    request.responseText = "anything";
    QuestManager.handleQuestChange(request);
    assertEquals(1, Preferences.getInteger("_saplingsPlanted"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"timbarrrr", "plant_a_tree"})
  void doesNotTrackArrrborDayNonCombats(String nonCombat) throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=174");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_arrrboretum_" + nonCombat + ".html"));
    QuestManager.handleQuestChange(request);
    assertEquals(0, Preferences.getInteger("_saplingsPlanted"));
  }

  @Test
  void canParseSpacegate() throws IOException {
    var request = new GenericRequest("choice.php?forceoption=0");
    request.responseText =
        Files.readString(Path.of("request/test_spacegate_terminal_earddyk.html"));
    QuestManager.handleQuestChange(request);

    assertEquals("Diverticulus Isaacson IX", Preferences.getString("_spacegatePlanetName"));
    assertEquals("EARDDYK", Preferences.getString("_spacegateCoordinates"));
    assertEquals(4, Preferences.getInteger("_spacegatePlanetIndex"));
    assertEquals("high winds", Preferences.getString("_spacegateHazards"));
    assertEquals("primitive", Preferences.getString("_spacegatePlantLife"));
    assertEquals("primitive (hostile)", Preferences.getString("_spacegateAnimalLife"));
    assertEquals("none detected", Preferences.getString("_spacegateIntelligentLife"));
    assertEquals(false, Preferences.getBoolean("_spacegateSpant"));
    assertEquals(false, Preferences.getBoolean("_spacegateMurderbot"));
    assertEquals(false, Preferences.getBoolean("_spacegateRuins"));
    assertEquals(20, Preferences.getInteger("_spacegateTurnsLeft"));
  }
}
