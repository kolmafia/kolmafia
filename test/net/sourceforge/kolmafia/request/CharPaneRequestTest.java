package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import internal.helpers.Cleanups;
import internal.helpers.Player;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CharPaneRequestTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("CharPaneRequestTest");
    KoLCharacter.setCurrentRun(0);
    CharPaneRequest.reset();
  }

  @Test
  void canParseSnowsuit() {
    var cleanups = new Cleanups(Player.withProperty("snowsuit", ""));

    try (cleanups) {
      CharPaneRequest.processResults(html("request/test_charpane_snowsuit.html"));
      assertThat("snowsuit", isSetTo("hat"));
    }
  }

  @Nested
  class Sweaty {
    @ParameterizedTest
    @CsvSource({
      "request/test_charpane_sweatiness_100.html, 100",
      "request/test_charpane_sweatiness_compact.html, 69",
    })
    void parseSweatiness(String responseHtml, int expectedValue) {
      var cleanups =
          new Cleanups(
              Player.withEquipped(EquipmentManager.PANTS, "designer sweatpants"),
              Player.withProperty("sweat", 0));

      try (cleanups) {
        var result = CharPaneRequest.processResults(html(responseHtml));
        assertThat(result, equalTo(true));
        assertThat("sweat", isSetTo(expectedValue));
      }
    }

    @Test
    void recogniseNoSweatinessDisplayedMeansZeroIfPantsEquipped() {
      var cleanups =
          new Cleanups(
              Player.withEquipped(EquipmentManager.PANTS, "designer sweatpants"),
              Player.withProperty("sweat", 11));

      try (cleanups) {
        var result = CharPaneRequest.processResults(html("request/test_charpane_basic.html"));
        assertThat(result, equalTo(true));
        assertThat("sweat", isSetTo(0));
      }
    }
  }
}
