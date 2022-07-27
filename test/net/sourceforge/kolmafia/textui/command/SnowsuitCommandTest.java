package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withUnequipped;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SnowsuitCommandTest extends AbstractCommandTestBase {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("SnowsuitCommandTest");
    HttpClientWrapper.setupFakeClient();
    ChoiceManager.handlingChoice = false;
  }

  public SnowsuitCommandTest() {
    this.command = "snowsuit";
  }

  @Test
  void showsCurrentValue() {
    var cleanups = new Cleanups(withProperty("snowsuit", "eyebrows"));

    try (cleanups) {
      String output = execute("");

      assertContinueState();
      assertThat(output, containsString("is eyebrows"));
    }
  }

  @Test
  void rejectsInvalidValue() {
    String output = execute("codpiece");

    assertThat(output, containsString("codpiece not recognised"));
  }

  @Test
  void equipsSnowsuitIfNotEquipped() {
    var cleanups =
        new Cleanups(
            withEquippableItem("Snow Suit"),
            withFamiliar(FamiliarPool.CRAB),
            withUnequipped(EquipmentManager.FAMILIAR),
            withProperty("snowsuit", "smirk"));

    try (cleanups) {
      String output = execute("smirk");
      var requests = getRequests();

      assertThat(output, containsString("Putting on Snow Suit"));
      assertThat(requests, hasSize(greaterThanOrEqualTo(1)));
      assertPostRequest(
          requests.get(0), "/inv_equip.php", "which=2&ajax=1&action=equip&whichitem=6150");
    }
  }

  @Test
  void doesNothingIfAlreadySet() {
    var cleanups =
        new Cleanups(
            withEquippableItem("Snow Suit"),
            withFamiliar(FamiliarPool.CRAB),
            withEquipped(EquipmentManager.FAMILIAR, "Snow Suit"),
            withProperty("snowsuit", "goatee"));

    try (cleanups) {
      String output = execute("goatee");
      var requests = getRequests();

      assertThat(output, containsString("goatee already equipped"));
      assertThat(requests, hasSize(0));
    }
  }

  private static Stream<Arguments> provideModes() {
    return SnowsuitCommand.MODES.entrySet().stream()
        .map(e -> Arguments.of(e.getKey(), e.getValue()));
  }

  @ParameterizedTest
  @MethodSource("provideModes")
  void successFullyhangesDecoration(int decision, String decoration) {
    var cleanups =
        new Cleanups(
            withEquippableItem("Snow Suit"),
            withFamiliar(FamiliarPool.CRAB),
            withEquipped(EquipmentManager.FAMILIAR, "Snow Suit"),
            withProperty("snowsuit", ""));

    try (cleanups) {
      String output = execute(decoration);
      var requests = getRequests();

      assertThat(requests, hasSize(2));
      assertPostRequest(requests.get(0), "/inventory.php", "action=decorate");
      assertPostRequest(requests.get(1), "/choice.php", "whichchoice=640&option=" + decision);
      assertThat(output, containsString("decorated with " + decoration));
    }
  }
}
