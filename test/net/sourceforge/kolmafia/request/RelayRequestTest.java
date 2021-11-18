package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.Test;

public class RelayRequestTest {

  @Test
  public void findVariousRelayFilesOrNot() {
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
