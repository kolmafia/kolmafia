package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.canUse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLCharacter;
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
  void mustSpecifyState() {
    var cleanups = new Cleanups(canUse("unbreakable umbrella"));
    try (cleanups) {
      String output = execute("");

      assertErrorState();
      assertThat(output, containsString("What state do you want to fold your umbrella to?"));
    }
  }

  @Test
  void mustSpecifyValidState() {
    var cleanups = new Cleanups(canUse("unbreakable umbrella"));
    try (cleanups) {
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
    var cleanups = new Cleanups(canUse("unbreakable umbrella"));
    try (cleanups) {
      assertChoseState(mode.getShorthand(), mode);
    }
  }

  @ParameterizedTest
  @EnumSource(UmbrellaMode.class)
  void canChooseStateByName(UmbrellaMode mode) {
    var cleanups = new Cleanups(canUse("unbreakable umbrella"));
    try (cleanups) {
      assertChoseState(mode.getName(), mode);
    }
  }

  @Test
  void canChooseStateWithTwirling() {
    var cleanups = new Cleanups(canUse("unbreakable umbrella"));
    try (cleanups) {
      assertChoseState("twirling", UmbrellaMode.TWIRL);
    }
  }
}
