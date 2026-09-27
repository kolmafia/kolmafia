package net.sourceforge.kolmafia.utilities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FileUtilitiesTest {

  @BeforeEach
  public void deletePriors() {
    // Delete the tray icon in images
    deleteLoaded("images/", "TrayIcon12.gif");
    // Delete the tray icon in relay
    deleteLoaded("relay/", "TrayIcon12.gif");
    // Set the Preference
    Preferences.setString("lastRelayUpdate", StaticEntity.getVersion());
  }

  void deleteLoaded(String directory, String filename) {
    directory = directory.replaceAll(File.separator.replaceAll("\\\\", "\\\\\\\\"), "/");
    filename = filename.replaceAll(File.separator.replaceAll("\\\\", "\\\\\\\\"), "/");
    if (directory.length() > 0 && !directory.endsWith("/")) {
      directory += "/";
    }
    String fullname = directory + filename;
    File loadedFile = new File(fullname);
    if (loadedFile.exists()) {
      loadedFile.delete();
    }
  }

  @Test
  public void itShouldLoadTrayIconFromImages() {

    File trayIcon;
    // Confirm absence of tray icon, load it from images, verify
    trayIcon = new File(KoLConstants.IMAGE_LOCATION, "TrayIcon12.gif");
    assertFalse(trayIcon.exists(), "Tray Icon image exists.");
    FileUtilities.loadLibrary(KoLConstants.IMAGE_LOCATION, "images/", "TrayIcon12.gif");
    trayIcon = new File(KoLConstants.IMAGE_LOCATION, "TrayIcon12.gif");
    assertTrue(trayIcon.exists(), "Tray Icon image doesn't exist.");
  }

  @Test
  public void itShouldLoadTrayIconFromRelay() {
    File trayIcon;
    trayIcon = new File(KoLConstants.RELAY_LOCATION, "TrayIcon12.gif");
    assertFalse(trayIcon.exists(), "Tray Icon image exists.");
    FileUtilities.loadLibrary(KoLConstants.RELAY_LOCATION, "images/", "TrayIcon12.gif");
    trayIcon = new File(KoLConstants.RELAY_LOCATION, "TrayIcon12.gif");
    assertTrue(trayIcon.exists(), "Tray Icon image doesn't exist.");
  }

  @Test
  public void itShouldLoadTrayIconFromRelayWithPreference() {
    File trayIcon;
    Preferences.setString("lastRelayUpdate", "xyzzy");
    trayIcon = new File(KoLConstants.RELAY_LOCATION, "TrayIcon12.gif");
    assertFalse(trayIcon.exists(), "Tray Icon image exists.");
    FileUtilities.loadLibrary(KoLConstants.RELAY_LOCATION, "images/", "TrayIcon12.gif");
    trayIcon = new File(KoLConstants.RELAY_LOCATION, "TrayIcon12.gif");
    assertTrue(trayIcon.exists(), "Tray Icon image doesn't exist.");
  }

  @Test
  public void itShouldNotReadFromAFileThatDoesNotExist() {
    BufferedReader br = FileUtilities.getReader("xyzzy");
    String line = FileUtilities.readLine(br);
    assertNull(line, "Read something from non-existent file");
  }

  @Test
  public void itShouldNotLoadAnImageThatDoesNotExist() {
    File image = FileUtilities.imageFile("yzzyx");
    assertFalse(image.exists());
  }

  @Test
  public void itShouldNotLoadARelayFileThatDoesNotExist() {
    assertFalse(FileUtilities.internalRelayScriptExists("xyzzy"));
  }

  @Test
  public void itShouldLoadARelayFileThatExists() {
    assertTrue(FileUtilities.internalRelayScriptExists("barrel_sounds.js"));
  }

  @Test
  public void itShouldNotLoadALibraryWithBadData() {
    // This test only has side effects observable in coverage reports
    FileUtilities.loadLibrary(KoLConstants.IMAGE_LOCATION, "images/", null);
    FileUtilities.loadLibrary(KoLConstants.IMAGE_LOCATION, "images/", "TrayIc..on12.gif");
    assertTrue(true);
  }

  @Test
  public void itShouldNotDoAnythingWithANullInput() {
    assertNull(FileUtilities.readData(null));
    assertNull(FileUtilities.readLine(null));
  }

  @Nested
  class EmptyDirectory {
    @BeforeAll
    public static void setupEmpty() throws IOException {
      Files.createDirectory(KoLConstants.ROOT_LOCATION.toPath().resolve("empty"));
    }

    @AfterAll
    public static void removeEmpty() throws IOException {
      Files.deleteIfExists(KoLConstants.ROOT_LOCATION.toPath().resolve("empty"));
    }

    @ParameterizedTest
    @CsvSource({
      "ccs, false",
      "README, false",
      "empty, true",
    })
    public void shouldDetectEmptyDirectory(String directory, boolean isEmptyDirectory)
        throws IOException {
      var path = KoLConstants.ROOT_LOCATION.toPath().resolve(directory);
      assertThat(FileUtilities.isEmptyDirectory(path), equalTo(isEmptyDirectory));
    }
  }
}
