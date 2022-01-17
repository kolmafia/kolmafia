package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.addItem;
import static internal.helpers.Player.countItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class QuestManagerTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("QuestManager");
    Preferences.reset("QuestManager");
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
    assertTrue(QuestDatabase.isQuestStep(Quest.AZAZEL, QuestDatabase.UNSTARTED));
    var request = new GenericRequest("pandamonium.php");
    request.responseText = "anything";
    QuestManager.handleQuestChange(request);
    assertTrue(QuestDatabase.isQuestStep(Quest.AZAZEL, QuestDatabase.STARTED));
  }

  @Test
  public void visitingPandamoniumDoesNotRevertQuest() {
    QuestDatabase.setQuest(Quest.AZAZEL, "step1");
    var request = new GenericRequest("pandamonium.php");
    request.responseText = "anything";
    QuestManager.handleQuestChange(request);
    assertTrue(QuestDatabase.isQuestStep(Quest.AZAZEL, "step1"));
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
    assertTrue(QuestDatabase.isQuestStep(Quest.CITADEL, "step1"));
  }

  @Test
  void canDetectCitadelStep2InWhiteCitadel() throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=413");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_white_citadel_they_arent_blind.html"));
    QuestManager.handleQuestChange(request);
    assertTrue(QuestDatabase.isQuestStep(Quest.CITADEL, "step2"));
  }

  @Test
  void canDetectCitadelStep3InWhiteCitadel() throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=413");
    request.responseText =
        Files.readString(
            Path.of("request/test_adventure_white_citadel_existential_blues_brothers.html"));
    QuestManager.handleQuestChange(request);
    assertTrue(QuestDatabase.isQuestStep(Quest.CITADEL, "step3"));
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
    assertTrue(QuestDatabase.isQuestStep(Quest.CLANCY, "step1"));
  }

  @Test
  void canDetectClancyStep3InKnobShaft() throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=101");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_knob_shaft_a_miner_variation.html"));
    QuestManager.handleQuestChange(request);
    assertTrue(QuestDatabase.isQuestStep(Quest.CLANCY, "step3"));
  }

  @Test
  void canDetectClancyStep7InIcyPeak() throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=110");
    request.responseText =
        Files.readString(Path.of("request/test_adventure_icy_peak_mercury_rising.html"));
    QuestManager.handleQuestChange(request);
    assertTrue(QuestDatabase.isQuestStep(Quest.CLANCY, "step7"));
  }

  @Test
  void canDetectClancyFinishInMiddleChamber() throws IOException {
    var request = new GenericRequest("adventure.php?snarfblat=407");
    request.responseText =
        Files.readString(
            Path.of("request/test_adventure_middle_chamber_dont_you_know_who_i_am.html"));
    QuestManager.handleQuestChange(request);
    assertTrue(QuestDatabase.isQuestStep(Quest.CLANCY, QuestDatabase.FINISHED));
  }

  /*
   * Garbage Quest
   */
  @Test
  void canDetectGarbageStep1InPlains() throws IOException {
    var request = new GenericRequest("place.php?whichplace=plains");
    request.responseText = Files.readString(Path.of("request/test_place_plains_beanstalk.html"));
    QuestManager.handleQuestChange(request);
    assertTrue(QuestDatabase.isQuestStep(Quest.GARBAGE, "step1"));
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

  /*
   * Palindome Quest
   */
  @Test
  public void canDetectPalindomeStartInPalindome() {
    assertTrue(QuestDatabase.isQuestStep(Quest.PALINDOME, QuestDatabase.UNSTARTED));
    var request = new GenericRequest("adventure.php?snarfblat=386");
    request.redirectLocation = "fight.php";
    QuestManager.handleQuestChange(request);
    assertTrue(QuestDatabase.isQuestStep(Quest.PALINDOME, QuestDatabase.STARTED));
  }

  /*
   * Swamp Quest
   */
  @Test
  public void canStartMartyQuest() throws IOException {
    assertTrue(QuestDatabase.isQuestStep(Quest.SWAMP, QuestDatabase.UNSTARTED));
    var request = new GenericRequest("place.php?whichplace=canadia&action=lc_marty");
    request.responseText = Files.readString(Path.of("request/test_canadia_start_quest.html"));
    QuestManager.handleQuestChange(request);
    assertTrue(QuestDatabase.isQuestStep(Quest.SWAMP, QuestDatabase.STARTED));
  }

  /*
   * Non-Quest Related
   */
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
