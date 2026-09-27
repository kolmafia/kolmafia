package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withoutItem;
import static net.sourceforge.kolmafia.textui.command.AbstractCommandTestBase.assertContinueState;
import static net.sourceforge.kolmafia.textui.command.AbstractCommandTestBase.assertErrorState;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ForeseeRequestTest {
  @BeforeAll
  public static void beforeAll() {
    Preferences.reset("ForeseeRequestTest");
  }

  @Test
  void failsWithoutPeridot() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withoutItem(ItemPool.PERIDOT_OF_PERIL),
            withContinuationState());

    try (cleanups) {
      var outputStream = new ByteArrayOutputStream();
      RequestLogger.openCustom(new PrintStream(outputStream));

      var request = new ForeseeRequest();
      request.run();

      RequestLogger.closeCustom();
      assertErrorState();
      assertThat(outputStream.toString(), containsString("You do not own a Peridot of Peril."));
      assertThat(client.getRequests(), hasSize(0));
    }
  }

  @Test
  void failsWhenPeridotIsUsedUp() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withItem(ItemPool.PERIDOT_OF_PERIL),
            withProperty("_perilsForeseen", "3"),
            withContinuationState());

    try (cleanups) {
      var outputStream = new ByteArrayOutputStream();
      RequestLogger.openCustom(new PrintStream(outputStream));

      var request = new ForeseeRequest();
      request.run();

      RequestLogger.closeCustom();
      assertErrorState();
      assertThat(
          outputStream.toString(), containsString("You can only foresee peril thrice daily."));
      assertThat(client.getRequests(), hasSize(0));
    }
  }

  @Test
  void isPermittedWhenRequirementsAreMet() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withItem(ItemPool.PERIDOT_OF_PERIL),
            withProperty("_perilsForeseen", "1"),
            withContinuationState());

    try (cleanups) {
      var request = new ForeseeRequest();
      request.run();
      assertContinueState();

      var requests = client.getRequests();
      assertThat(requests, hasSize(1));
      assertGetRequest(requests.getFirst(), "/inventory.php", "action=foresee&pwd=");
    }
  }
}
