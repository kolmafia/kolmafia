package net.sourceforge.kolmafia.moods;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MoodManagerTest {

  @BeforeEach
  public void initializeCharPrefs() {
    KoLCharacter.reset("moody");
    KoLCharacter.reset(true);
    Preferences.saveSettingsToFile = false;
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
  public void itShouldLoadMoods() {
    resetCharAndPrefs();
    // These moods should always exist even when there is no character.
    Mood mooda = new Mood("apathetic");
    Mood moodd = new Mood("default");
    List<Mood> always = new ArrayList<>();
    always.add(mooda);
    always.add(moodd);
    MoodManager.loadSettings();
    List<Mood> loaded = MoodManager.getAvailableMoods();
    assertEquals(2, loaded.size(), "Expected only 2 moods");
    assertThat(always, Matchers.containsInAnyOrder(loaded.toArray()));
    // Check this just for coverage since it is not relevant in testing functionality.
    assertFalse(MoodManager.isExecuting(), "Mood should not be executing");
  }

  @Test
  public void itShouldSetMoods() {
    MoodManager.loadSettings();
    MoodManager.setMood(null);
    assertEquals("default", Preferences.getString("currentMood"), "Expecting default mood");
    // These will increase coverage but not change mood.
    MoodManager.setMood("exec");
    assertEquals("default", Preferences.getString("currentMood"), "Expecting default mood");
    MoodManager.setMood("clear");
    assertEquals("default", Preferences.getString("currentMood"), "Expecting default mood");
    MoodManager.setMood("autofill");
    assertEquals("default", Preferences.getString("currentMood"), "Expecting default mood");
    MoodManager.setMood("repeat");
    assertEquals("default", Preferences.getString("currentMood"), "Expecting default mood");
    // This should change the mood
    MoodManager.setMood("xyzzy");
    assertEquals("xyzzy", Preferences.getString("currentMood"), "Expecting xyzzy as mood");
  }

  @Test
  public void itShouldHaveNoTriggersWhenThereAreNotAnyDefined() {
    resetCharAndPrefs();
    MoodManager.loadSettings();
    assertTrue(MoodManager.getTriggers(null).isEmpty(), "Triggers unexpectedly defined");
    assertTrue(MoodManager.getTriggers().isEmpty(), "Triggers unexpectedly defined");
    MoodManager.setMood("xyzzy");
    assertTrue(MoodManager.getTriggers("xyzzy").isEmpty(), "Triggers unexpectedly defined");
    assertTrue(MoodManager.getTriggers().isEmpty(), "Triggers unexpectedly defined");
    assertEquals(0L, MoodManager.getMaintenanceCost(), "Unexpected cost");
  }

  @Test
  public void itShouldRecognizeExtends() {
    MoodManager.loadSettings();
    MoodManager.setMood("xyzzy extends nothing");
    List<Mood> moods = MoodManager.getAvailableMoods();
    for (Mood moo : moods) {
      if (moo.getName().contains("xyzzy")) {
        assertTrue(moo.getParentNames().contains("nothing"), "Extend did not set parent");
      }
    }
    MoodManager.deleteCurrentMood();
    // Do this twice because the second time is a different path because mood already there
    for (int i = 0; i < 2; i++) {
      MoodManager.setMood("xyzzy extends default, nothing");
      moods = MoodManager.getAvailableMoods();
      for (Mood moo : moods) {
        if (moo.getName().contains("xyzzy")) {
          assertTrue(moo.getParentNames().contains("default"), "Extend did not set parent");
          assertTrue(moo.getParentNames().contains("nothing"), "Extend did not set parent");
        }
      }
    }
  }

  @Test
  public void itShouldLoadFromFile() throws IOException {
    // Load settings and set mood preference
    Preferences.setString("currentMood", "meatdrop");
    MoodManager.loadSettings(mockedReader());
    // Give Character skills
    KoLCharacter.addAvailableSkill(SkillDatabase.getSkillId("empathy of the newt"));
    KoLCharacter.addAvailableSkill(SkillDatabase.getSkillId("fat leon's phat loot lyric"));
    KoLCharacter.addAvailableSkill(SkillDatabase.getSkillId("leash of linguini"));
    KoLCharacter.addAvailableSkill(SkillDatabase.getSkillId("the polka of plenty"));
    KoLCharacter.addAvailableSkill(SkillDatabase.getSkillId("singer's faithful ocelot"));
    // Get the cost of maintaining the current mood
    assertEquals(60L, MoodManager.getMaintenanceCost(), "Unexpected cost");
  }

  @Test
  public void itShouldAddAndRemoveTriggers() throws IOException {
    initializeCharPrefs();
    MoodManager.loadSettings(mockedReader());
    MoodManager.setMood("meatdrop");
    Preferences.setString("currentMood", "meatdrop");
    List<MoodTrigger> before = MoodManager.getTriggers("meatdrop");
    assertEquals(8, before.size(), "Unexpected triggers");
    // Make and add a new trigger
    MoodTrigger newTrigger = MoodManager.addTrigger("gain_effect", "beaten up", "abort");
    assertFalse(before.contains(newTrigger), "Unexpected duplication of triggers");
    List<MoodTrigger> after = MoodManager.getTriggers("meatdrop");
    assertEquals(9, after.size(), "Trigger not added");
    assertTrue(after.contains(newTrigger), "Unexpected duplication of triggers");
    Collection<MoodTrigger> collection = new ArrayList<>();
    collection.add(newTrigger);
    MoodManager.removeTriggers(collection);
    after = MoodManager.getTriggers("meatdrop");
    assertEquals(8, after.size(), "Unexpected triggers");
    assertFalse(after.contains(newTrigger), "Unexpected duplication of triggers");
  }

  @Test
  public void itShouldExerciseMinimalSet() {
    MoodManager.deleteCurrentMood();
    // minimal set is nothing for apathetic
    MoodManager.setMood("apathetic");
    assertEquals(0, MoodManager.getTriggers("apathetic").size(), "Triggers already present");
    MoodManager.minimalSet();
    assertEquals(0, MoodManager.getTriggers("apathetic").size(), "Triggers already present");
    // minimal set includes triggers for current effects
    MoodManager.setMood("default");
    assertEquals(0, MoodManager.getTriggers("default").size(), "Triggers already present");
    KoLConstants.activeEffects.add(EffectPool.get(EffectPool.LEASH_OF_LINGUINI));
    MoodManager.minimalSet();
    assertEquals(1, MoodManager.getTriggers("default").size(), "Triggers already present");
  }

  @Test
  public void itShouldExerciseMaximalSet() {
    MoodManager.deleteCurrentMood();
    // maximal set is nothing for apathetic
    MoodManager.setMood("apathetic");
    MoodManager.maximalSet();
    assertEquals(0, MoodManager.getTriggers("apathetic").size(), "Triggers already present");
    MoodManager.setMood("default");
    // No skills so maximal set is empty
    MoodManager.maximalSet();
    assertEquals(0, MoodManager.getTriggers("default").size(), "Triggers already present");
    // Acquire some skills
    KoLCharacter.addAvailableSkill(SkillDatabase.getSkillId("empathy of the newt"));
    KoLCharacter.addAvailableSkill(SkillDatabase.getSkillId("fat leon's phat loot lyric"));
    KoLCharacter.addAvailableSkill(SkillDatabase.getSkillId("leash of linguini"));
    KoLCharacter.addAvailableSkill(SkillDatabase.getSkillId("the polka of plenty"));
    MoodManager.maximalSet();
    assertEquals(4, MoodManager.getTriggers("default").size(), "Triggers already present");
  }
}
