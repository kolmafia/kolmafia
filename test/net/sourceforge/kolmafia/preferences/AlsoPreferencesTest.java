package net.sourceforge.kolmafia.preferences;

import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSavePreferencesToFile;
import static internal.helpers.Utilities.verboseDelete;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Cleanups;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.KoLCharacter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlsoPreferencesTest {
  private final String USER_NAME = "PreferencesTestAlsoFakeUser".toLowerCase();

  @BeforeEach
  public void initializeCharPreferences() {
    KoLCharacter.reset(USER_NAME);
  }

  @AfterEach
  public void resetCharAndPreferences() {
    File userFile = new File("settings/" + USER_NAME + "_prefs.txt");
    verboseDelete(userFile);
    File backupFile = new File("settings/" + USER_NAME + "_prefs.bak");
    verboseDelete(backupFile);
  }

  @Test
  public void savesSettingsIfOn() throws IOException {
    var cleanups =
        new Cleanups(withSavePreferencesToFile(), withProperty("saveSettingsOnSet", true));
    try (cleanups) {
      File userFile = new File("settings/" + USER_NAME + "_prefs.txt");
      InputStream inputStream = DataUtilities.getInputStream(userFile);
      String contents = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      inputStream.close();
      assertThat(contents, not(containsString("\nxyz=abc\n")));
      Preferences.setString("xyz", "abc");
      inputStream = DataUtilities.getInputStream(userFile);
      contents = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      assertThat(contents, containsString("\nxyz=abc\n"));
      inputStream.close();
    }
  }

  @Test
  public void canToggle() throws IOException {
    File userFile = new File("settings/" + USER_NAME + "_prefs.txt");
    InputStream inputStream = DataUtilities.getInputStream(userFile);
    String contents = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    inputStream.close();
    assertThat(contents, not(containsString("\nxyz=abc\n")));

    var cleanups =
        new Cleanups(
            withSavePreferencesToFile(),
            withProperty("saveSettingsOnSet", false),
            withProperty("xyz", "abc"));
    try (cleanups) {
      inputStream = DataUtilities.getInputStream(userFile);
      contents = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      inputStream.close();
      assertThat(contents, not(containsString("\nxyz=abc\n")));

      var cleanups2 =
          new Cleanups(withProperty("saveSettingsOnSet", true), withProperty("wxy", "def"));
      try (cleanups2) {
        inputStream = DataUtilities.getInputStream(userFile);
        contents = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        inputStream.close();
        assertThat(contents, containsString("\nxyz=abc\n"));
        assertThat(contents, containsString("\nwxy=def\n"));
      }
    }
  }
}
