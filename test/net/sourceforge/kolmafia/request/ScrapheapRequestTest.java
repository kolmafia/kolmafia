package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ScrapheapRequestTest {

  @BeforeEach
  protected void initEach() {
    KoLCharacter.reset("fakeUserName");
  }

  @AfterAll
  public static void afterAll() {
    ChoiceManager.handlingChoice = false;
  }

  private int parseActivations(String path) {
    String html = html(path);
    var req = new ScrapheapRequest("sh_chrono");
    req.responseText = html;
    req.processResults();
    return Preferences.getInteger("_chronolithActivations");
  }

  @Test
  public void parseChronolith1() {
    var cleanups = new Cleanups(withPath(Path.YOU_ROBOT));
    try (cleanups) {
      KoLCharacter.setYouRobotEnergy(1000);
      int cost = 16;
      Preferences.setInteger("_chronolithNextCost", cost);
      assertEquals(7, parseActivations("request/test_scrapheap_chronolith_1.html"));
      assertEquals(1000 - cost, KoLCharacter.getYouRobotEnergy());
      assertEquals(cost + 1, Preferences.getInteger("_chronolithNextCost"));
    }
  }

  @Test
  public void parseChronolith37() {
    var cleanups = new Cleanups(withPath(Path.YOU_ROBOT));
    try (cleanups) {
      KoLCharacter.setYouRobotEnergy(1000);
      int cost = 138;
      Preferences.setInteger("_chronolithNextCost", cost);
      assertEquals(60, parseActivations("request/test_scrapheap_chronolith_37.html"));
      assertEquals(1000 - cost, KoLCharacter.getYouRobotEnergy());
      assertEquals(cost + 2, Preferences.getInteger("_chronolithNextCost"));
    }
  }

  @Test
  public void parseChronolith69() {
    var cleanups = new Cleanups(withPath(Path.YOU_ROBOT));
    try (cleanups) {
      KoLCharacter.setYouRobotEnergy(1000);
      int cost = 890;
      Preferences.setInteger("_chronolithNextCost", cost);
      assertEquals(80, parseActivations("request/test_scrapheap_chronolith_69.html"));
      assertEquals(1000 - cost, KoLCharacter.getYouRobotEnergy());
      assertEquals(cost + 10, Preferences.getInteger("_chronolithNextCost"));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "request/test_scrapheap_cpu_upgrades.html, robot_muscle:robot_mysticality:robot_moxie:robot_meat:robot_hp1:robot_regen:robot_resist:robot_items:robot_shirt:robot_energy:robot_potions:robot_hp2"
  })
  public void parseCPUUpgrades(String path, String upgrades) {
    var cleanups = new Cleanups(withPath(Path.YOU_ROBOT));
    try (cleanups) {
      String html = html(path);
      var request = new GenericRequest("choice.php?whichchoice=1445&show=cpus");
      request.setHasResult(true);
      request.responseText = html;
      ChoiceManager.preChoice(request);
      request.processResponse();

      var expected = upgrades.split(":");
      var actual = Preferences.getString("youRobotCPUUpgrades").split(",");

      assertThat(actual, arrayContainingInAnyOrder(expected));
    }
  }
}
