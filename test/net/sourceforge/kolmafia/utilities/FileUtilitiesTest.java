package net.sourceforge.kolmafia.utilities;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.File;
import net.sourceforge.kolmafia.KoLConstants;
import org.junit.jupiter.api.Test;

class FileUtilitiesTest {

  void deleteLoaded(String directory, String filename) {
    directory = directory.replaceAll(File.separator.replaceAll("\\\\", "\\\\\\\\"), "/");
    filename = filename.replaceAll(File.separator.replaceAll("\\\\", "\\\\\\\\"), "/");
    if (directory.length() > 0 && !directory.endsWith("/")) {
      directory += "/";
    }
    String fullname = directory + filename;
    File loadedFile = new File(fullname);
    if (loadedFile.exists()) {
      loadedFile.deleteOnExit();
    }
  }

  @Test
  public void exerciseSomeMethodsForCoverage() {

    FileUtilities.loadLibrary(KoLConstants.IMAGE_LOCATION, "images/", "TrayIcon12.gif");
    deleteLoaded("images/", "TrayIcon12.gif");
    FileUtilities.loadLibrary(KoLConstants.RELAY_LOCATION, "images/", "TrayIcon12.gif");
    deleteLoaded("images/", "TrayIcon12.gif");
    BufferedReader br = FileUtilities.getReader("xyzzy");
    String line = FileUtilities.readLine(br);
    assertNull(line, "Read something from non-existent file");
    assertFalse(FileUtilities.internalRelayScriptExists("xyzzy"));
    assertTrue(FileUtilities.internalRelayScriptExists("barrel_sounds.js"));
    File image = FileUtilities.imageFile("yzzyx");
    assertFalse(image.exists());
    String location = "itemimages/mushsprout.gif";
    File source = new File(KoLConstants.IMAGE_LOCATION, location);
    File destination = new File(KoLConstants.PLOTS_LOCATION + "/" + location);
    FileUtilities.copyFile(source, destination);
    assertTrue(destination.exists());
    destination.delete();
  }
}
