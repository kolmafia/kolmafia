package net.sourceforge.kolmafia.moods;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    // Check this just for coverage since it is not relevant in testing functionality.
    assertFalse(MoodManager.isExecuting(), "Mood should not be executing");
    List<Mood> loaded = MoodManager.getAvailableMoods();
    assertEquals(2, loaded.size(), "Expected only 2 moods");
    assertThat(always, Matchers.containsInAnyOrder(loaded.toArray()));
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
}
