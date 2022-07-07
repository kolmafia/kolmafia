package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.canUse;
import static internal.helpers.Player.equip;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RetroCapeCommandTest extends AbstractCommandTestBase {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("RetroCapeCommandTest");
    HttpClientWrapper.setupFakeClient();
    ChoiceManager.handlingChoice = false;
  }

  public RetroCapeCommandTest() {
    this.command = "retrocape";
  }

  @Test
  void mustHaveItem() {
    String output = execute("vampire hold");

    assertErrorState();
    assertThat(output, containsString("You need a knock-off"));
  }

  @Test
  void equipsItemIfNecessary() {
    var cleanups = new Cleanups(canUse("unwrapped knock-off retro superhero cape"));

    try (cleanups) {
      execute("robot kiss");

      var requests = getRequests();

      assertThat(requests, hasSize(greaterThanOrEqualTo(1)));
      assertPostRequest(
          requests.get(0), "/inv_equip.php", "which=2&ajax=1&action=equip&whichitem=10647");
    }
  }

  @ParameterizedTest
  @CsvSource({
    "robot, 3",
    "mus, 1",
  })
  void configuresJustSuperhero(String superhero, int decision) {
    var cleanups =
        new Cleanups(equip(EquipmentManager.CONTAINER, "unwrapped knock-off retro superhero cape"));

    try (cleanups) {
      String output = execute(superhero);

      var requests = getRequests();

      assertThat(requests, hasSize(5));
      assertGetRequest(requests.get(0), "/inventory.php", "action=hmtmkmkm");
      assertPostRequest(requests.get(2), "/choice.php", "whichchoice=1438&option=" + decision);
    }
  }

  @ParameterizedTest
  @CsvSource({
    "thrill, 3",
    "kill, 5",
  })
  void configuresJustWashingInstruction(String instruction, int decision) {
    var cleanups =
        new Cleanups(equip(EquipmentManager.CONTAINER, "unwrapped knock-off retro superhero cape"));

    try (cleanups) {
      String output = execute(instruction);

      var requests = getRequests();

      assertThat(requests, hasSize(3));
      assertGetRequest(requests.get(0), "/inventory.php", "action=hmtmkmkm");
      assertPostRequest(requests.get(1), "/choice.php", "whichchoice=1437&option=" + decision);
    }
  }

  @ParameterizedTest
  @CsvSource({
    "vampire hold, 1, 2",
    "mys kiss, 2, 4",
  })
  void configuresBothModes(String mode, int decision1, int decision2) {
    var cleanups =
        new Cleanups(equip(EquipmentManager.CONTAINER, "unwrapped knock-off retro superhero cape"));

    try (cleanups) {
      String output = execute(mode);

      var requests = getRequests();

      assertThat(requests, hasSize(6));
      assertGetRequest(requests.get(0), "/inventory.php", "action=hmtmkmkm");
      assertPostRequest(requests.get(1), "/choice.php", "whichchoice=1437&option=" + decision2);
      assertPostRequest(requests.get(3), "/choice.php", "whichchoice=1438&option=" + decision1);
    }
  }
}
