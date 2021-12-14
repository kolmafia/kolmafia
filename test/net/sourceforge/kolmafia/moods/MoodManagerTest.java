package net.sourceforge.kolmafia.moods;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
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

  public boolean copyMoodFile() {
    String name = "moody_moods.txt";
    File sourceF = new File(KoLConstants.DATA_LOCATION, name);
    if (!sourceF.exists()) return false;
    Path source = sourceF.toPath();
    Path destination = new File(KoLConstants.SETTINGS_LOCATION, name).toPath();
    try {
      Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
      assertTrue(destination.toFile().exists(), "Copy did not create file");
      assertEquals(
          Files.readAllLines(source), Files.readAllLines(destination), "Files not the same");
      return true;
    } catch (Exception e) {
      return false;
    }
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
    MoodManager.setMood("xyzzy extends default, nothing");
    moods = MoodManager.getAvailableMoods();
    for (Mood moo : moods) {
      if (moo.getName().contains("xyzzy")) {
        assertTrue(moo.getParentNames().contains("default"), "Extend did not set parent");
        assertTrue(moo.getParentNames().contains("nothing"), "Extend did not set parent");
      }
    }
  }

  @Test
  public void itShouldLoadFromFile() {
    // Copy file from data to settings
    assertTrue(copyMoodFile(), "Could not copy file");
    // Load settings and set mood preference
    MoodManager.loadSettings();
    Preferences.setString("currentMood", "meatdrop");
  }
}
