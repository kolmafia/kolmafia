package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.UmbrellaRequest;
import net.sourceforge.kolmafia.request.UmbrellaRequest.UmbrellaMode;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class UmbrellaCommandTest extends AbstractCommandTestBase {
  @BeforeEach
  public void initEach() {
    KoLCharacter.reset("UmbrellaCommandTest");
    HttpClientWrapper.setupFakeClient();
    ChoiceManager.handlingChoice = false;
  }

  public UmbrellaCommandTest() {
    this.command = "umbrella";
  }

  @Test
  void mustHaveUmbrella() {
    String output = execute("ml");

    assertErrorState();
    assertThat(output, containsString("You need an Unbreakable Umbrella first."));
  }

  @Test
  void mustUnequipLefty() {
    try (var _ =
        new Cleanups(
            withFamiliarInTerrarium(FamiliarPool.LEFT_HAND),
            withFamiliar(FamiliarPool.BLOOD_FACED_VOLLEYBALL))) {
      KoLCharacter.usableFamiliar(FamiliarPool.LEFT_HAND)
          .setItem(ItemPool.get(ItemPool.UNBREAKABLE_UMBRELLA));

      String output = execute("ml");

      var requests = getRequests();

      assertPostRequest(
          requests.get(0),
          "/familiar.php",
          "famid=" + FamiliarPool.LEFT_HAND + "&action=unequip&ajax=1");
    }
  }

  @Test
  void mustSpecifyState() {
    try (var _ = new Cleanups(withEquippableItem("unbreakable umbrella"))) {
      String output = execute("");

      assertErrorState();
      assertThat(output, containsString("What state do you want to fold your umbrella to?"));
    }
  }

  @Test
  void mustSpecifyValidState() {
    try (var _ = new Cleanups(withEquippableItem("unbreakable umbrella"))) {
      String output = execute("the bourgeoisie");

      assertErrorState();
      assertThat(output, containsString("I don't understand what Umbrella form"));
    }
  }

  private void assertChoseState(final String command, final UmbrellaRequest.UmbrellaMode mode) {
    String output = execute(command);

    assertContinueState();
    assertThat(output, containsString("Folding umbrella"));

    var requests = getRequests();

    assertThat(requests, hasSize(2));
    assertGetRequest(requests.get(0), "/inventory.php", "action=useumbrella");
    assertPostRequest(requests.get(1), "/choice.php", "whichchoice=1466&option=" + mode.getId());
  }

  @ParameterizedTest
  @EnumSource(UmbrellaMode.class)
  void canChooseStateByShorthand(UmbrellaMode mode) {
    try (var _ = new Cleanups(withEquippableItem("unbreakable umbrella"))) {
      assertChoseState(mode.getShorthand(), mode);
    }
  }

  @ParameterizedTest
  @EnumSource(UmbrellaMode.class)
  void canChooseStateByName(UmbrellaMode mode) {
    try (var _ = new Cleanups(withEquippableItem("unbreakable umbrella"))) {
      assertChoseState(mode.getName(), mode);
    }
  }

  @Test
  void canChooseStateWithTwirling() {
    try (var _ = new Cleanups(withEquippableItem("unbreakable umbrella"))) {
      assertChoseState("twirling", UmbrellaMode.TWIRL);
    }
  }

  @Test
  void canChooseStateWithCapitalizedTwirling() {
    try (var _ = new Cleanups(withEquippableItem("unbreakable umbrella"))) {
      assertChoseState("Twirling", UmbrellaMode.TWIRL);
    }
  }
}
