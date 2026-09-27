package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CrimboTrainCommandTest extends AbstractCommandTestBase {

  public CrimboTrainCommandTest() {
    this.command = "crimbotrain";
  }

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("CrimboTrainCommandTest");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("CrimboTrainCommandTest");
  }

  @Test
  public void noTargetIsError() {
    HttpClientWrapper.setupFakeClient();
    var cleanups =
        new Cleanups(
            withProperty("_crimboTraining", false),
            withItem(ItemPool.CRIMBO_TRAINING_MANUAL),
            withContinuationState());

    try (cleanups) {
      String output = execute("");
      assertThat(output, containsString("Train whom?"));
      assertErrorState();

      var requests = getRequests();
      assertThat(requests.size(), equalTo(0));
    }
  }

  @Test
  public void noCrimboTrainingManualIsError() {
    HttpClientWrapper.setupFakeClient();
    var cleanups = new Cleanups(withProperty("_crimboTraining", false));

    try (cleanups) {
      String output = execute("Hairy");
      assertThat(output, containsString("You need 1 more Crimbo training manual to continue."));
      assertErrorState();

      var requests = getRequests();
      assertThat(requests.size(), equalTo(0));
    }
  }

  @Test
  public void alreadyTrainedTodayIsError() {
    HttpClientWrapper.setupFakeClient();
    var cleanups =
        new Cleanups(
            withProperty("_crimboTraining", true), withItem(ItemPool.CRIMBO_TRAINING_MANUAL));

    try (cleanups) {
      String output = execute("Hairy");
      assertThat(output, containsString("You've already trained somebody today."));
      assertErrorState();

      var requests = getRequests();
      assertThat(requests.size(), equalTo(0));
    }
  }

  @Test
  public void canDetectTargetAlreadyKnowsSkill() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withProperty("_crimboTraining", false),
            withItem(ItemPool.CRIMBO_TRAINING_MANUAL));
    try (cleanups) {
      client.addResponse(200, html("request/test_use_crimbo_training_3c.html"));
      String output = execute("121572");
      assertThat(output, containsString("They already know that skill."));
      assertErrorState();

      var requests = client.getRequests();
      assertThat(requests.size(), equalTo(1));
      assertPostRequest(
          requests.get(0), "/curse.php", "action=use&whichitem=11046&targetplayer=121572");
    }
  }
}
