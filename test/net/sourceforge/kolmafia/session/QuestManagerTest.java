package net.sourceforge.kolmafia.session;

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

  @Test
  public void adventuringInPalindomeAdvancesQuest() {
    assertTrue(QuestDatabase.isQuestStep(Quest.PALINDOME, "unstarted"));
    var request = new GenericRequest("adventure.php?snarfblat=386");
    request.redirectLocation = "fight.php";
    QuestManager.handleQuestChange(request);
    assertTrue(QuestDatabase.isQuestStep(Quest.PALINDOME, "started"));
  }

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

  @Test
  public void visitingPandamoniumMakesSureAzazelQuestIsStarted() {
    assertTrue(QuestDatabase.isQuestStep(Quest.AZAZEL, "unstarted"));
    var request = new GenericRequest("pandamonium.php");
    QuestManager.handleQuestChange(request);
    assertTrue(QuestDatabase.isQuestStep(Quest.AZAZEL, "started"));
  }

  @Test
  public void visitingPandamoniumDoesNotRevertQuest() {
    QuestDatabase.setQuest(Quest.AZAZEL, "step1");
    var request = new GenericRequest("pandamonium.php");
    QuestManager.handleQuestChange(request);
    assertTrue(QuestDatabase.isQuestStep(Quest.AZAZEL, "step1"));
  }

  @Test
  public void canStartMartyQuest() throws IOException {
    assertTrue(QuestDatabase.isQuestStep(Quest.SWAMP, "unstarted"));
    var request = new GenericRequest("place.php?whichplace=canadia&action=lc_marty");
    request.responseText = Files.readString(Path.of("request/test_canadia_start_quest.html"));
    QuestManager.handleQuestChange(request);
    assertTrue(QuestDatabase.isQuestStep(Quest.SWAMP, "started"));
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
