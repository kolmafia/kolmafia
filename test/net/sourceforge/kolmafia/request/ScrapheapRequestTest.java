package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ScrapheapRequestTest extends RequestTestBase {

  @BeforeAll
  private static void injectPreferences() {
    KoLCharacter.reset("fakeUserName");
    Preferences.setBoolean("saveSettingsOnSet", false);
  }

  @Test
  public void parseChronolith1() throws IOException {
    byte[] fileData = Files.readAllBytes(Paths.get("request/test_scrapheap_chronolith_1.html"));
    String html = new String(fileData, StandardCharsets.UTF_8);

    var req = new ScrapheapRequest("sh_chrono");
    req.responseText = html;
    req.processResults();

    assertEquals(7, Preferences.getInteger("_chronolithActivations"));
  }

  @Test
  public void parseChronolith37() throws IOException {
    byte[] fileData = Files.readAllBytes(Paths.get("request/test_scrapheap_chronolith_37.html"));
    String html = new String(fileData, StandardCharsets.UTF_8);

    var req = new ScrapheapRequest("sh_chrono");
    req.responseText = html;
    req.processResults();

    assertEquals(60, Preferences.getInteger("_chronolithActivations"));
  }

  @Test
  public void parseChronolith69() throws IOException {
    byte[] fileData = Files.readAllBytes(Paths.get("request/test_scrapheap_chronolith_69.html"));
    String html = new String(fileData, StandardCharsets.UTF_8);

    var req = new ScrapheapRequest("sh_chrono");
    req.responseText = html;
    req.processResults();

    assertEquals(80, Preferences.getInteger("_chronolithActivations"));
  }

  @Test
  public void parseCPUUpgrades() throws IOException {
    byte[] fileData = Files.readAllBytes(Paths.get("request/test_scrapheap_cpu_upgrades.html"));
    String html = new String(fileData, StandardCharsets.UTF_8);

    ChoiceManager.handlingChoice = true;
    var req = new GenericRequest("choice.php?whichchoice=1445&show=cpus");
    req.responseText = html;
    req.processResponse();

    var expected =
        Arrays.asList(
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
            "robot_hp2");
    var actual = Arrays.asList(Preferences.getString("youRobotCPUUpgrades").split(","));

    assertEquals(expected.size(), actual.size());
    assertTrue(actual.containsAll(expected));
    assertTrue(expected.containsAll(actual));
  }
}
