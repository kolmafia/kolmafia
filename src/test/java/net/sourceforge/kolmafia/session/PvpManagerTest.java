package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withAttacksLeft;
import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withHippyStoneBroken;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junitpioneer.jupiter.cartesian.CartesianTest.Values;

import internal.helpers.Cleanups;
import internal.helpers.RequestLoggerOutput;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ProfileRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junitpioneer.jupiter.cartesian.CartesianTest;

public class PvpManagerTest {

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("PvpManagerTest");
  }

  @BeforeEach
  protected void beforeEach() {
    Preferences.reset("PvpManagerTest");
  }

  @Nested
  class DirectedAttack {
    // A directed PVP attack cannot attack for fame.
    // If you are in Hardcore or Ronin, you can only attack for flowers.
    // If you are out of Hardcore or Ronin, you can attack for loot or flowers.
    // If your target is in Hardcore or Ronin, you can only attack for flowers.

    static ProfileRequest profileCanInteract =
        ProfileRequest.getInstance(
            "player1", "123456", "18", html("request/test_profile_can_interact.html"), null);
    static ProfileRequest profileInRonin =
        ProfileRequest.getInstance(
            "player2", "234567", "13", html("request/test_profile_in_ronin.html"), null);

    @CartesianTest
    public void validatesMission(
        @Values(booleans = {false, true}) final boolean inRonin,
        @Values(booleans = {false, true}) final boolean targetInRonin,
        @Values(strings = {"flowers", "fame", "lootwhatever"}) final String mission) {
      RequestLoggerOutput.startStream();
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withHippyStoneBroken(),
              withAttacksLeft(1),
              withInteractivity(!inRonin),
              withContinuationState(),
              withProperty("defaultFlowerWinMessage", "lucky!"),
              withProperty("defaultFlowerLossMessage", "oops."));

      try (cleanups) {
        client.addResponse(200, "");

        var target = targetInRonin ? profileInRonin : profileCanInteract;
        String targetName = target.getPlayerName();

        var targets = new ProfileRequest[1];
        targets[0] = target;

        RequestLoggerOutput.startStream();
        PvpManager.executePvpRequest(targets, mission, 1);

        var output = RequestLoggerOutput.stopStream();
        var requests = client.getRequests();

        boolean expectedFailure = inRonin && mission.equals("lootwhatever");
        var state = StaticEntity.getContinuationState();
        if (expectedFailure) {
          assertThat(state, is(MafiaState.ERROR));
          assertThat(output, containsString("Cannot attack for loot if you can't interact"));
          assertThat(requests, hasSize(0));
        } else {
          assertEquals(MafiaState.CONTINUE, state);
          assertThat(output, containsString("Attacking " + targetName + "..."));
          assertThat(requests, hasSize(1));

          // If you can interact and the target cannot interact, your
          // only mission is flowers.
          String expectedMission = (!inRonin && targetInRonin) ? "flowers" : mission;

          // Construct the request we submitted
          StringBuilder buf = new StringBuilder();
          buf.append("action=fight&place=fight&attacktype=");
          buf.append(expectedMission);
          buf.append("&winmessage=lucky!&losemessage=oops.&stance=1&who=");
          buf.append(targetName);
          buf.append("&ranked=0");

          assertPostRequest(requests.get(0), "/peevpee.php", buf.toString());
        }
      }
    }
  }
}
