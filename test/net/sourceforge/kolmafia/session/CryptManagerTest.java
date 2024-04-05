package net.sourceforge.kolmafia;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.ResponseTextParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class CryptManagerTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("RequestEditorKitTest");
    Preferences.reset("RequestEditorKitTest");
  }

  @Nested
  class VisitCyrpt {
    @Test
    void parsesFourCornerCyrpt() {
      var cleanups =
          new Cleanups(
              withProperty("cyrptNookEvilness", 50),
              withProperty("cyrptNicheEvilness", 50),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptTotalEvilness", 1000));
      try (cleanups) {
        var request = new GenericRequest("crypt.php", true);
        request.responseText = html("request/test_cyrpt_four_corners.html");
        ResponseTextParser.externalUpdate(request);

        assertThat("cyrptNookEvilness", isSetTo(50));
        assertThat("cyrptNicheEvilness", isSetTo(50));
        assertThat("cyrptCrannyEvilness", isSetTo(50));
        assertThat("cyrptAlcoveEvilness", isSetTo(50));
        assertThat("cyrptTotalEvilness", isSetTo(200));
      }
    }

    @Test
    void parsesThreeCornerCyrpt() {
      var cleanups =
          new Cleanups(
              withProperty("cyrptNookEvilness", 50),
              withProperty("cyrptNicheEvilness", 50),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptTotalEvilness", 1000));
      try (cleanups) {
        var request = new GenericRequest("crypt.php", true);
        request.responseText = html("request/test_cyrpt_three_corners.html");
        ResponseTextParser.externalUpdate(request);

        assertThat("cyrptNookEvilness", isSetTo(0));
        assertThat("cyrptNicheEvilness", isSetTo(50));
        assertThat("cyrptCrannyEvilness", isSetTo(50));
        assertThat("cyrptAlcoveEvilness", isSetTo(50));
        assertThat("cyrptTotalEvilness", isSetTo(150));
      }
    }

    @Test
    void parsesTwoCornerCyrpt() {
      var cleanups =
          new Cleanups(
              withProperty("cyrptNookEvilness", 50),
              withProperty("cyrptNicheEvilness", 50),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptTotalEvilness", 1000));
      try (cleanups) {
        var request = new GenericRequest("crypt.php", true);
        request.responseText = html("request/test_cyrpt_two_corners.html");
        ResponseTextParser.externalUpdate(request);

        assertThat("cyrptNookEvilness", isSetTo(0));
        assertThat("cyrptNicheEvilness", isSetTo(0));
        assertThat("cyrptCrannyEvilness", isSetTo(50));
        assertThat("cyrptAlcoveEvilness", isSetTo(50));
        assertThat("cyrptTotalEvilness", isSetTo(100));
      }
    }

    @Test
    void parsesHaertCyrpt() {
      var cleanups =
          new Cleanups(
              withProperty("cyrptNookEvilness", 50),
              withProperty("cyrptNicheEvilness", 50),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptTotalEvilness", 1000));
      try (cleanups) {
        var request = new GenericRequest("crypt.php", true);
        request.responseText = html("request/test_cyrpt_haert.html");
        ResponseTextParser.externalUpdate(request);

        assertThat("cyrptNookEvilness", isSetTo(0));
        assertThat("cyrptNicheEvilness", isSetTo(0));
        assertThat("cyrptCrannyEvilness", isSetTo(0));
        assertThat("cyrptAlcoveEvilness", isSetTo(0));
        assertThat("cyrptTotalEvilness", isSetTo(999));
      }
    }

    @Test
    void parsesEmptyCyrpt() {
      var cleanups =
          new Cleanups(
              withProperty("cyrptNookEvilness", 50),
              withProperty("cyrptNicheEvilness", 50),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptTotalEvilness", 1000));
      try (cleanups) {
        var request = new GenericRequest("crypt.php", true);
        request.responseText = html("request/test_cyrpt_empty.html");
        ResponseTextParser.externalUpdate(request);

        assertThat("cyrptNookEvilness", isSetTo(0));
        assertThat("cyrptNicheEvilness", isSetTo(0));
        assertThat("cyrptCrannyEvilness", isSetTo(0));
        assertThat("cyrptAlcoveEvilness", isSetTo(0));
        assertThat("cyrptTotalEvilness", isSetTo(0));
      }
    }
  }
}
