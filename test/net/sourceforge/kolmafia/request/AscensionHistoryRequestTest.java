package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AscensionHistoryRequestTest extends RequestTestBase {

  @BeforeAll
  private static void init() {
    Preferences.saveSettingsToFile = false;
    KoLCharacter.reset("the Tristero");
    KoLCharacter.setUserId(177122);
  }

  @AfterAll
  private static void tidyUp() {
    KoLCharacter.reset("");
    KoLCharacter.setUserId(0);
    Preferences.saveSettingsToFile = true;
  }

  @Test
  public void parseAscensionHistory() throws IOException {
    KoLCharacter.reset("the Tristero");

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
    assertEquals(10, Preferences.getInteger("gloverPoints"), "Glover Points Mismatch");
    assertEquals(3, Preferences.getInteger("masksUnlocked"));
    assertEquals(23, Preferences.getInteger("darkGyfftePoints"));
    assertEquals(22, Preferences.getInteger("plumberPoints"));
    assertEquals(3, Preferences.getInteger("youRobotPoints"));
    assertEquals(0, Preferences.getInteger("quantumPoints"));
  }
}
