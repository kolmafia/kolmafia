package net.sourceforge.kolmafia.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.sourceforge.kolmafia.KoLCharacter;import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;import net.sourceforge.kolmafia.request.GenericRequest;

import org.junit.jupiter.api.BeforeEach;import org.junit.jupiter.api.Test;

public class QuestManagerTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("QuestManager");
    Preferences.reset("QuestManager");
  }

  @Test
  public void adventuringInPalindomeAdvancesQuest() {
    assertTrue(QuestDatabase.isQuestStep(Quest.PALINDOME, "unstarted"));
    var palindomeRequest = new GenericRequest("adventure.php?snarfblat=386");
    palindomeRequest.redirectLocation = "fight.php";
    QuestManager.handleQuestChange(palindomeRequest);
    assertTrue(QuestDatabase.isQuestStep(Quest.PALINDOME, "started"));
  }

  @Test
  public void seeingTheIslandMarksItAsUnlocked() {
    var ascension = KoLCharacter.getAscensions();
    assertThat(Preferences.getInteger("lastIslandUnlock"), lessThan(ascension));
    var mainRequest = new GenericRequest("main.php");
    mainRequest.responseText = "island.php";
    QuestManager.handleQuestChange(mainRequest);
    assertEquals(ascension, Preferences.getInteger("lastIslandUnlock"));
  }

  @Test
  public void visitingPandamoniumMakesSureAzazelQuestIsStarted() {
    assertTrue(QuestDatabase.isQuestStep(Quest.AZAZEL, "unstarted"));
    var pandamoniumRequest = new GenericRequest("pandamonium.php");
    QuestManager.handleQuestChange(pandamoniumRequest);
    assertTrue(QuestDatabase.isQuestStep(Quest.AZAZEL, "started"));
  }

  @Test
  public void visitingPandamoniumDoesNotRevertQuest() {
    QuestDatabase.setQuest(Quest.AZAZEL, "step1");
    var pandamoniumRequest = new GenericRequest("pandamonium.php");
    QuestManager.handleQuestChange(pandamoniumRequest);
    assertFalse(QuestDatabase.isQuestStep(Quest.AZAZEL, "started"));
  }
}
