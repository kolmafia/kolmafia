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
  protected static void init() {
    Preferences.saveSettingsToFile = false;
    KoLCharacter.reset("the Tristero");
    KoLCharacter.setUserId(177122);
  }

  @AfterAll
  protected static void tidyUp() {
    KoLCharacter.reset("");
    KoLCharacter.setUserId(0);
    Preferences.saveSettingsToFile = true;
  }

  @Test
  public void checkUserName() throws IOException {
    assertEquals("the Tristero", KoLCharacter.getUserName());
  }

  @Test
  public void parseAscensionHistory() throws IOException {
    String html = Files.readString(Paths.get("request/test_ascensionhistory.html"));

    AscensionHistoryRequest.parseResponse("ascensionhistory.php?who=177122", html);

    assertEquals(43, Preferences.getInteger("borisPoints"), "Boris ascensions mismatch");
    assertEquals(31, Preferences.getInteger("zombiePoints"), "Zombie ascensions mismatch");
    assertEquals(15, Preferences.getInteger("jarlsbergPoints"), "Jarlsberg ascensions mismatch");
    assertEquals(15, Preferences.getInteger("sneakyPetePoints"), "Sneaky Pete ascensions mismatch");
    assertEquals(28, Preferences.getInteger("edPoints"), "Ed ascensions mismatch");
    assertEquals(
        5, Preferences.getInteger("awolPointsCowpuncher"), "Cowpuncher ascensions mismatch");
    assertEquals(
        5, Preferences.getInteger("awolPointsBeanslinger"), "Beanslinger ascensions mismatch");
    assertEquals(
        8, Preferences.getInteger("awolPointsSnakeoiler"), "Snake Oiler ascensions mismatch");
    assertEquals(13, Preferences.getInteger("sourcePoints"), "The Source ascensions mismatch");
    assertEquals(9, Preferences.getInteger("noobPoints"), "Gelatinous Noob ascensions mismatch");
    assertEquals(24, Preferences.getInteger("bondPoints"), "Bond ascensions mismatch");
    assertEquals(10, Preferences.getInteger("gloverPoints"), "G-Lover ascensions mismatch");
    assertEquals(3, Preferences.getInteger("masksUnlocked"), "Masks Unlocked mismatch");
    assertEquals(23, Preferences.getInteger("darkGyfftePoints"), "Dark Gyffte ascensions mismatch");
    assertEquals(22, Preferences.getInteger("plumberPoints"), "Plumber ascensions mismatch");
    assertEquals(3, Preferences.getInteger("youRobotPoints"), "You, Robot ascensions mismatch");
    assertEquals(0, Preferences.getInteger("quantumPoints"), "Quantum ascensions mismatch");
  }
}
