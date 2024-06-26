package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import org.junit.jupiter.api.Test;

public class RelayRequestTest {

  /**
   * The global clean up code will delete root/relay since relay is not under Git control. This is
   * the only test that breaks when that happens. Duplicate the code that loads the relay directory
   * here so that the directory will be present when the test is run.
   */
  public void initializeRelayFileDirectory() {
    for (int i = 0; i < KoLConstants.RELAY_FILES.length; ++i) {
      FileUtilities.loadLibrary(
          KoLConstants.RELAY_LOCATION, KoLConstants.RELAY_DIRECTORY, KoLConstants.RELAY_FILES[i]);
    }
  }

  @Test
  public void findVariousRelayFilesOrNot() {
    initializeRelayFileDirectory();
    File f;
    f = RelayRequest.findRelayFile("thisIsNotAPipe");
    assertNotNull(f, "Allowed to find a new file.");
    assertFalse(f.exists(), "Found file is not supposed to exist.");
    f = RelayRequest.findRelayFile("thisIs..NotAPipe");
    assertNull(f, "Not supposed to find file with dots in name.");
    f = RelayRequest.findRelayFile("barrel_sounds.js");
    assertTrue(f.exists(), "Supposed to find file that exists.");
  }

  @Test
  public void exerciseSomeStaticThingsForCoverage() {
    RelayRequest.loadOverrideImages(false);
    RelayRequest.loadOverrideImages(true);
    Preferences.setBoolean("relayOverridesImages", true);
    RelayRequest.loadOverrideImages(false);
    RelayRequest.loadOverrideImages(true);
    RelayRequest.clearImageCache();
    assertFalse(RelayRequest.builtinRelayFile("notafile"));
    assertTrue(RelayRequest.builtinRelayFile("afterlife.1.ash"));
  }

  @Test
  public void exerciseSomeObjectMethodsForCoverage() {
    RelayRequest rr = new RelayRequest(false);
    assertNull(rr.getHashField());
    assertFalse(rr.retryOnTimeout());
    rr.constructURLString("diary.php?textversion=1");
  }
}
