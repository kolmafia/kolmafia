package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ConcertCommandTest extends AbstractCommandTestBase {

  public ConcertCommandTest() {
    this.command = "concert";
  }

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("ConcertCommandTest");
    Preferences.reset("ConcertCommandTest");
  }

  @Test
  public void warUnstartedIsError() {
    String output = execute("o");
    assertErrorState();
    assertThat(output, containsString("You have not started the island war yet."));
  }

  @Test
  public void arenaNotCompletedIsError() {
    var cleanups =
        new Cleanups(
            withProperty("warProgress", "started"),
            withProperty("sidequestArenaCompleted", "none"));

    try (cleanups) {
      String output = execute("o");
      assertErrorState();
      assertThat(output, containsString("The arena is not open."));
    }
  }

  @ParameterizedTest
  @CsvSource({"hippy,hippies,o", "fratboy,fratboys,wi", "fratboy,both,wi"})
  public void arenaCompletedWithDefeatedSideIsError(String arena, String defeated, String effect) {
    var cleanups =
        new Cleanups(
            withProperty("warProgress", "started"),
            withProperty("sidequestArenaCompleted", arena),
            withProperty("sideDefeated", defeated));

    try (cleanups) {
      String output = execute(effect);
      assertErrorState();
      assertThat(output, containsString("The arena's fans were defeated in the war."));
    }
  }

  @Test
  public void numberOutOfRangeIsError() {
    var cleanups =
        new Cleanups(
            withProperty("warProgress", "started"),
            withProperty("sidequestArenaCompleted", "hippy"));

    try (cleanups) {
      String output = execute("5");
      assertErrorState();
      assertThat(output, containsString("Invalid concert number."));
    }
  }

  @ParameterizedTest
  @CsvSource({"hippy,hippies,winklered", "fratboy,fratboys,optimist"})
  public void wrongSideEffectIsError(String arena, String plural, String effect) {
    var cleanups =
        new Cleanups(
            withProperty("warProgress", "started"), withProperty("sidequestArenaCompleted", arena));

    try (cleanups) {
      String output = execute(effect);
      assertErrorState();
      assertThat(
          output, containsString("The \"" + effect + "\" effect is not available to " + plural));
    }
  }

  @Test
  public void noArgIsError() {
    var cleanups =
        new Cleanups(
            withProperty("warProgress", "started"),
            withProperty("sidequestArenaCompleted", "fratboy"));

    try (cleanups) {
      String output = execute("");
      assertErrorState();
      assertThat(output, containsString("The \"\" effect is not available to fratboys."));
    }
  }

  @ParameterizedTest
  @CsvSource({"hippy,optimist,3", "fratboy,winklered,2"})
  public void successSendsRequest(String arena, String effect, String option) {
    HttpClientWrapper.setupFakeClient();
    var cleanups =
        new Cleanups(
            withProperty("warProgress", "started"), withProperty("sidequestArenaCompleted", arena));

    try (cleanups) {
      execute(effect);
      assertContinueState();

      var requests = getRequests();
      assertThat(requests, not(empty()));
      assertThat(requests.size(), equalTo(1));
      assertPostRequest(requests.get(0), "/bigisland.php", "action=concert&option=" + option);
    }
  }
}
