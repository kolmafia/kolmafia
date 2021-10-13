package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisabledOnOs(value = OS.MAC, disabledReason = "Testing preference tracking does not work on Mac")
public class AscensionHistoryRequestTest extends RequestTestBase {
  @BeforeEach
  private void initEach() {
    KoLCharacter.reset("the Tristero");
    KoLCharacter.setUserId(177122);
    Preferences.setBoolean("saveSettingsOnSet", false);
  }

  @AfterEach
  private void tidyUp() {
    KoLCharacter.reset("");
    KoLCharacter.setUserId(0);
  }

  @Test
  public void parseAscensionHistory() throws IOException {
    String html = Files.readString(Paths.get("request/test_ascensionhistory.html"));

    AscensionHistoryRequest.parseResponse("ascensionhistory.php?who=177122", html);

    assertEquals(43, Preferences.getInteger("borisPoints"));
    assertEquals(31, Preferences.getInteger("zombiePoints"));
    assertEquals(15, Preferences.getInteger("jarlsbergPoints"));
    assertEquals(15, Preferences.getInteger("sneakyPetePoints"));
    assertEquals(28, Preferences.getInteger("edPoints"));
    assertEquals(5, Preferences.getInteger("awolPointsCowpuncher"));
    assertEquals(5, Preferences.getInteger("awolPointsBeanslinger"));
    assertEquals(8, Preferences.getInteger("awolPointsSnakeoiler"));
    assertEquals(13, Preferences.getInteger("sourcePoints"));
    assertEquals(9, Preferences.getInteger("noobPoints"));
    assertEquals(24, Preferences.getInteger("bondPoints"));
    assertEquals(10, Preferences.getInteger("garlandUpgrades"));
    assertEquals(3, Preferences.getInteger("masksUnlocked"));
    assertEquals(23, Preferences.getInteger("darkGyfftePoints"));
    assertEquals(22, Preferences.getInteger("plumberPoints"));
    assertEquals(3, Preferences.getInteger("youRobotPoints"));
    assertEquals(0, Preferences.getInteger("quantumPoints"));
  }
}
