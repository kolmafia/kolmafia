package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ScrapheapRequestTest extends RequestTestBase {

  @BeforeEach
  protected void initEach() {
    Preferences.saveSettingsToFile = false;
    KoLCharacter.reset("fakeUserName");
  }

  @AfterEach
  protected void tidyUp() {
    KoLCharacter.reset("");
    Preferences.saveSettingsToFile = true;
  }

  private int parseActivations(String path) throws IOException {

    String html = Files.readString(Paths.get(path));
    var req = new ScrapheapRequest("sh_chrono");
    req.responseText = html;
    req.processResults();
    return Preferences.getInteger("_chronolithActivations");
  }

  @Test
  public void parseChronolith1() throws IOException {
    assertEquals(7, parseActivations("request/test_scrapheap_chronolith_1.html"));
  }

  @Test
  public void parseChronolith37() throws IOException {
    assertEquals(60, parseActivations("request/test_scrapheap_chronolith_37.html"));
  }

  @Test
  public void parseChronolith69() throws IOException {
    assertEquals(80, parseActivations("request/test_scrapheap_chronolith_69.html"));
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
