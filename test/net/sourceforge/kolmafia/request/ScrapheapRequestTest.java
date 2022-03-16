package net.sourceforge.kolmafia.request;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ScrapheapRequestTest {

  @BeforeAll
  private static void beforeAll() {
    Preferences.saveSettingsToFile = false;
  }

  @BeforeEach
  protected void initEach() {
    KoLCharacter.reset("fakeUserName");
  }

  @AfterAll
  private static void afterAll() {
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
    KoLCharacter.setYouRobotEnergy(1000);
    int cost = 16;
    Preferences.setInteger("_chronolithNextCost", cost);
    assertEquals(7, parseActivations("request/test_scrapheap_chronolith_1.html"));
    assertEquals(1000 - cost, KoLCharacter.getYouRobotEnergy());
    assertEquals(cost + 1, Preferences.getInteger("_chronolithNextCost"));
  }

  @Test
  public void parseChronolith37() throws IOException {
    KoLCharacter.setYouRobotEnergy(1000);
    int cost = 138;
    Preferences.setInteger("_chronolithNextCost", cost);
    assertEquals(60, parseActivations("request/test_scrapheap_chronolith_37.html"));
    assertEquals(1000 - cost, KoLCharacter.getYouRobotEnergy());
    assertEquals(cost + 2, Preferences.getInteger("_chronolithNextCost"));
  }

  @Test
  public void parseChronolith69() throws IOException {
    KoLCharacter.setYouRobotEnergy(1000);
    int cost = 890;
    Preferences.setInteger("_chronolithNextCost", cost);
    assertEquals(80, parseActivations("request/test_scrapheap_chronolith_69.html"));
    assertEquals(1000 - cost, KoLCharacter.getYouRobotEnergy());
    assertEquals(cost + 10, Preferences.getInteger("_chronolithNextCost"));
  }

  @Test
  public void parseCPUUpgrades() throws IOException {
    String html = Files.readString(Paths.get("request/test_scrapheap_cpu_upgrades.html"));

    var req = new GenericRequest("choice.php?whichchoice=1445&show=cpus");
    req.responseText = html;
    ChoiceManager.visitChoice(req);

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

    assertThat(actual, arrayContainingInAnyOrder(expected));
  }
}
