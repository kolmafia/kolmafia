package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withNextResponse;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import internal.network.FakeHttpClientBuilder;
import internal.network.FakeHttpResponse;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.utilities.HttpUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BackupCameraCommandTest extends AbstractCommandTestBase {
  public BackupCameraCommandTest() {
    this.command = "backupcamera";
  }

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("BackupCameraCommandTest");
  }

  @BeforeEach
  public void initializeState() {
    HttpClientWrapper.setupFakeClient();
    StaticEntity.setContinuationState(KoLConstants.MafiaState.CONTINUE);
    ChoiceManager.handlingChoice = false;
  }

  @Test
  public void getModes() {
    var command = new BackupCameraCommand();
    assertThat(command.getModes(), hasSize(3));
  }

  @Test
  public void validate() {
    var command = new BackupCameraCommand();
    assertThat(command.validate("backupcamera", "ml"), equalTo(true));
    assertThat(command.validate("backupcamera", "reverser enable"), equalTo(true));
    assertThat(command.validate("backupcamera", "reverser disable"), equalTo(true));
    assertThat(command.validate("backupcamera", "reverse"), equalTo(true));
    assertThat(command.validate("backupcamera", "win_game"), equalTo(false));
  }

  @Test
  public void warnIfNoCamera() {
    String output = execute("ml");
    assertThat(output, containsString("You need a backup camera first."));
    assertThat(getRequests(), empty());
  }

  @Test
  public void warnAgainstUnknownInput() {
    var cleanups = new Cleanups(withItem("backup camera"));

    try (cleanups) {
      String output = execute("rverser");

      assertThat(output, containsString("not recognised"));

      var requests = getRequests();
      assertThat(requests, empty());
    }
  }

  @Test
  public void canChangeMode() {
    var cleanups = new Cleanups(withItem("backup camera"));

    try (cleanups) {
      execute("init");

      var requests = getRequests();

      assertThat(requests, hasSize(greaterThanOrEqualTo(2)));
      assertPostRequest(requests.get(0), "/inventory.php", "action=bcmode");
      assertPostRequest(requests.get(1), "/choice.php", "whichchoice=1449&option=3");
    }
  }

  @Test
  public void canHandleAlreadySelectedOption() {
    // The same page is returned when an invalid option is selected
    var cleanups =
        new Cleanups(
            withItem("backup camera"),
            withNextResponse(
                new FakeHttpResponse<>(
                    302, Map.of("location", List.of("choice.php?forceoption=0")), ""),
                new FakeHttpResponse<>(
                    200, html("request/test_command_backupcamera_handles_unavailable_option.html")),
                new FakeHttpResponse<>(
                    200, html("request/test_command_backupcamera_handles_unavailable_option.html")),
                new FakeHttpResponse<>(
                    200, html("request/test_command_backupcamera_leave_choice.html"))));

    try (cleanups) {
      execute("ml");

      var requests =
          ((FakeHttpClientBuilder) HttpUtilities.getClientBuilder()).client.getRequests();

      assertThat(requests, hasSize(4));
      assertPostRequest(requests.get(0), "/inventory.php", "action=bcmode");
      assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
      assertPostRequest(requests.get(2), "/choice.php", "whichchoice=1449&option=1");
      assertPostRequest(requests.get(3), "/choice.php", "whichchoice=1449&option=6");
      assertThat(ChoiceManager.handlingChoice, is(false));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"off", "disable"})
  public void canDisableReverser(String disable) {
    // But... why?
    var cleanups = new Cleanups(withItem("backup camera"));

    try (cleanups) {
      execute("reverser " + disable);

      var requests = getRequests();

      assertThat(requests, hasSize(greaterThanOrEqualTo(2)));
      assertPostRequest(requests.get(0), "/inventory.php", "action=bcmode");
      assertPostRequest(requests.get(1), "/choice.php", "whichchoice=1449&option=5");
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "enable"})
  public void canEnableReverser(String enable) {
    var cleanups = new Cleanups(withItem("backup camera"));

    try (cleanups) {
      String output = execute("reverser " + enable);

      var requests = getRequests();

      assertThat(requests, hasSize(greaterThanOrEqualTo(2)));
      assertPostRequest(requests.get(0), "/inventory.php", "action=bcmode");
      assertPostRequest(requests.get(1), "/choice.php", "whichchoice=1449&option=4");
    }
  }
}
