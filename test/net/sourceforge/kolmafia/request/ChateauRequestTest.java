package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ChateauRequestTest {
  private static Cleanups propertyCleanups() {
    return new Cleanups(
        withProperty("getawayCampsiteUnlocked", true),
        withProperty("chateauAvailable", true),
        withProperty("timesRested", 0));
  }

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset(true);
    KoLCharacter.reset("ChateauRequestTest");
    Preferences.reset("ChateauRequestTest");
    // All the test cases have ceiling fan installed, so Mafia should recognize we
    // have lots of free rests.
  }

  @ParameterizedTest
  @CsvSource({
    "chateau_restlabelfree, request/test_request_chateau_restlabelfree_next_free.html",
    "chateau_restbox, request/test_request_chateau_restbox_next_free.html"
  })
  public void tracksAndDoesNotSetRestsToMaxIfNextFree(String action, String filename) {
    var cleanups = propertyCleanups();
    try (cleanups) {
      ChateauRequest request = new ChateauRequest(action);
      request.responseText = html(filename);
      request.setHasResult(true);
      request.processResponse();
      assertThat("timesRested", isSetTo(1));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "chateau_restlabelfree, request/test_request_chateau_restlabelfree_next_nonfree.html",
    "chateau_restbox, request/test_request_chateau_restbox_next_nonfree.html",
    "chateau_restlabel, request/test_request_chateau_restlabel.html"
  })
  public void setsRestsToMaxIfNextNonFree(String action, String filename) {
    var cleanups = propertyCleanups();
    try (cleanups) {
      ChateauRequest request = new ChateauRequest(action);
      request.responseText = html(filename);
      request.setHasResult(true);
      request.processResponse();
      assertThat("timesRested", isSetTo(KoLCharacter.freeRestsAvailable()));
    }
  }
}
