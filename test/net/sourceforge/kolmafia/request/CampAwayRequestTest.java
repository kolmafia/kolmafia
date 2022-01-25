package net.sourceforge.kolmafia.request;

import static internal.helpers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CampAwayRequestTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("CampAwayRequest");
    Preferences.reset("CampAwayRequest");
  }

  @Test
  void canDetectUnlockedByVisiting() throws IOException {
    var request = new CampAwayRequest();
    request.responseText = Files.readString(Path.of("request/test_place_campaway_visit.html"));
    request.processResults();

    assertThat("getawayCampsiteUnlocked", isSetTo(true));
  }

  @Test
  void doesNotUnlockIfVisitingIsRejected() throws IOException {
    var request = new CampAwayRequest();
    request.responseText =
        Files.readString(Path.of("request/test_place_campaway_dont_have_access.html"));
    request.processResults();

    assertThat("getawayCampsiteUnlocked", isSetTo(false));
  }
}
