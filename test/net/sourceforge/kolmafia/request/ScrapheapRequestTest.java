package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisabledOnOs(value = OS.MAC, disabledReason = "Testing preference tracking does not work on Mac")
public class ScrapheapRequestTest extends RequestTestBase {

  @BeforeEach
  private void initEach() {
    KoLCharacter.reset("fakeUserName");
    Preferences.setBoolean("saveSettingsOnSet", false);
  }

  @AfterAll
  private static void tidyUp() {
    KoLCharacter.reset("");
  }

  @Test
  public void parseChronolith1() throws IOException {
    String html = Files.readString(Paths.get("request/test_scrapheap_chronolith_1.html"));
    var req = new ScrapheapRequest("sh_chrono");
    req.responseText = html;
    req.processResults();

    assertEquals(7, Preferences.getInteger("_chronolithActivations"));
  }

  @Test
  public void parseChronolith37() throws IOException {
    String html = Files.readString(Paths.get("request/test_scrapheap_chronolith_37.html"));

    var req = new ScrapheapRequest("sh_chrono");
    req.responseText = html;
    req.processResults();

    assertEquals(60, Preferences.getInteger("_chronolithActivations"));
  }

  @Test
  public void parseChronolith69() throws IOException {
    String html = Files.readString(Paths.get("request/test_scrapheap_chronolith_69.html"));

    var req = new ScrapheapRequest("sh_chrono");
    req.responseText = html;
    req.processResults();

    assertEquals(80, Preferences.getInteger("_chronolithActivations"));
  }

  @Test
  public void parseCPUUpgrades() throws IOException {
    String html = Files.readString(Paths.get("request/test_scrapheap_cpu_upgrades.html"));

    ChoiceManager.handlingChoice = true;
    var req = new GenericRequest("choice.php?whichchoice=1445&show=cpus");
    req.responseText = html;
    req.processResponse();

    var expected =
        new String[] {
          "robot_muscle",
          "robot_mysticality",
          "robot_moxie",
          "robot_meat",
          "robot_hp1",
          "robot_regen",
          "robot_resist",
          "robot_items",
          "robot_shirt",
          "robot_energy",
          "robot_potions",
          "robot_hp2"
        };
    var actual = Preferences.getString("youRobotCPUUpgrades").split(",");

    assertArrayEquals(expected, actual);
  }
}
