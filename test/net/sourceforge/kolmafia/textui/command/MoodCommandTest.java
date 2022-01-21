package net.sourceforge.kolmafia.textui.command;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.moods.RecoveryManager;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MoodCommandTest extends AbstractCommandTestBase {

  private static final String LS = System.lineSeparator();

  public MoodCommandTest() {
    this.command = "mood";
  }

  @BeforeEach
  public void initializeState() throws IOException {
    KoLCharacter.reset("moody");
    KoLCharacter.reset(true);
    Preferences.saveSettingsToFile = false;

    Preferences.setString("currentMood", "default");
    MoodManager.loadSettings(mockedReader());

    // Stop requests from actually running
    GenericRequest.sessionId = null;
  }

  @AfterEach
  public void resetCharAndPrefs() {
    KoLCharacter.reset("");
    KoLCharacter.reset(true);
    KoLCharacter.setUserId(0);
    Preferences.saveSettingsToFile = false;
  }

  public BufferedReader mockedReader() throws IOException {
    BufferedReader bufferedReader = Mockito.mock(BufferedReader.class);
    Mockito.when(bufferedReader.readLine())
        .thenReturn(
            "[ apathetic ]",
            "",
            "[ default ]",
            "gain_effect beaten up => uneffect Beaten Up",
            "lose_effect empathy => cast 1 empathy of the newt",
            "lose_effect leash of linguini => cast 1 leash of linguini",
            "lose_effect singer's faithful ocelot => cast 1 singer's faithful ocelot",
            "",
            "[ meatdrop ]",
            "unconditional => MercenaryMood.ash",
            "gain_effect just the best anapests => uneffect Just the Best Anapests",
            "lose_effect disco leer => cast 1 disco leer",
            "lose_effect empathy => cast 1 empathy of the newt",
            "lose_effect fat leon's phat loot lyric => cast 1 fat leon's phat loot lyric",
            "lose_effect leash of linguini => cast 1 leash of linguini",
            "lose_effect polka of plenty => cast 1 the polka of plenty",
            "lose_effect singer's faithful ocelot => cast 1 singer's faithful ocelot",
            "",
            "[ nuts ]",
            "unconditional => MercenaryMood.ash",
            "gain_effect beaten up => abort",
            "gain_effect just the best anapests => uneffect Just the Best Anapests",
            "lose_effect mixed nutrients => use 1 tiny handful of mixed nuts",
            "",
            "[ omniquest ]",
            "unconditional => MercenaryMood.ash",
            "gain_effect beaten up => uneffect Beaten Up",
            "gain_effect just the best anapests => uneffect Just the Best Anapests",
            "lose_effect disco fever => cast 1 disco fever",
            "",
            "[ oq_hp ]",
            "unconditional => MercenaryMood.ash",
            "lose_effect big => cast 1 get big",
            "lose_effect mariachi mood => cast 1 moxie of the mariachi",
            "lose_effect patience of the tortoise => cast 1 patience of the tortoise",
            "lose_effect phorcefullness => use either 1 philter of phorce, 1 evil philter of phorce",
            "lose_effect power ballad of the arrowsmith => cast 1 the power ballad of the arrowsmith",
            "lose_effect rage of the reindeer => cast 1 rage of the reindeer",
            "lose_effect reptilian fortitude => cast 1 reptilian fortitude",
            "lose_effect saucemastery => cast 1 sauce contemplation",
            "lose_effect seal clubbing frenzy => cast 1 seal clubbing frenzy",
            "lose_effect stevedave's shanty of superiority => cast 1 stevedave's shanty of superiority",
            "",
            "[ stannius_spaaace ]",
            "gain_effect beaten up => abort",
            "gain_effect just the best anapests => uneffect Just the Best Anapests",
            "lose_effect empathy => cast 1 empathy of the newt",
            "lose_effect fresh scent => use 1 chunk of rock salt",
            "lose_effect leash of linguini => cast 1 leash of linguini",
            "lose_effect smooth movements => cast 1 smooth movement",
            "lose_effect springy fusilli => cast 1 springy fusilli",
            "lose_effect the sonata of sneakiness => cast 1 the sonata of sneakiness",
            "lose_effect transpondent => use 1 transporter transponder",
            "",
            null);
    return bufferedReader;
  }

  @Test
  public void itShouldListTriggersForCurrentMood() {
    String output = execute("list");
    String expected =
        "When I get Beaten Up, uneffect Beaten Up"
            + LS
            + "When I run low on Empathy, cast 1 Empathy of the Newt"
            + LS
            + "When I run low on Leash of Linguini, cast 1 Leash of Linguini"
            + LS
            + "When I run low on Singer's Faithful Ocelot, cast 1 Singer's Faithful Ocelot"
            + LS;
    assertEquals(expected, output, "Unexpected output");
  }

  @Test
  public void itShouldExecuteCurrentMood() {
    String output = execute("execute");
    String expected = "Created an empty checkpoint." + LS + "Mood swing complete." + LS;
    assertEquals(expected, output, "Unexpected output");
    // RecoveryManager active should prevent execution
    RecoveryManager.setRecoveryActive(true);
    output = execute("execute");
    expected = "";
    assertEquals(expected, output, "Unexpected output");
    RecoveryManager.setRecoveryActive(false);
  }

  @Test
  public void itShouldAutofill() {
    Preferences.setString("currentMood", "default");
    MoodManager.deleteCurrentMood();
    KoLCharacter.addAvailableSkill(SkillDatabase.getSkillId("empathy of the newt"));
    KoLCharacter.addAvailableSkill(SkillDatabase.getSkillId("fat leon's phat loot lyric"));
    KoLCharacter.addAvailableSkill(SkillDatabase.getSkillId("leash of linguini"));
    KoLCharacter.addAvailableSkill(SkillDatabase.getSkillId("the polka of plenty"));
    String output = execute("autofill");
    String expected =
        "When I run low on Empathy, cast 1 Empathy of the Newt"
            + LS
            + "When I run low on Fat Leon's Phat Loot Lyric, cast 1 Fat Leon's Phat Loot Lyric"
            + LS
            + "When I run low on Leash of Linguini, cast 1 Leash of Linguini"
            + LS
            + "When I run low on Polka of Plenty, cast 1 The Polka of Plenty"
            + LS;
    assertEquals(expected, output, "Unexpected output");
  }

  @Test
  public void itShouldListAllMoods() {
    String output = execute("listall");
    String expected =
        "apathetic"
            + LS
            + "default"
            + LS
            + "meatdrop"
            + LS
            + "nuts"
            + LS
            + "omniquest"
            + LS
            + "oq_hp"
            + LS
            + "stannius_spaaace"
            + LS;
    assertEquals(expected, output, "Unexpected output");
  }

  @Test
  public void itShouldClearAllTriggersForCurrentMood() {
    assertEquals(4, MoodManager.getTriggers().size(), "Moods not initialized as expected");
    String output = execute("clear");
    assertEquals(0, MoodManager.getTriggers().size(), "Moods not cleared");
    assertEquals("Cleared mood." + LS, output, "Unexpected cli output");
  }
}
