package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.LimitMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CharPaneRequestTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("CharPaneRequestTest");
    Preferences.reset("CharPaneRequestTest");
    KoLCharacter.setCurrentRun(0);
    CharPaneRequest.reset();
  }

  @Test
  void canFindAvatarWithCrossorigin() {
    KoLCharacter.setAvatar("");
    CharPaneRequest.processResults(html("request/test_charpane_sauce.html"));
    var sauceCharacterAvatar = KoLCharacter.getAvatar();
    assertTrue(
        sauceCharacterAvatar.contains("otherimages/classav4a.gif"), "fails with crossorigin");
  }

  @Test
  void canFindAvatarWithouyCrossorigin() {
    KoLCharacter.setAvatar("");
    CharPaneRequest.processResults(html("request/test_charpane_snowsuit.html"));
    var snowCharacterAvatar = KoLCharacter.getAvatar();
    assertTrue(snowCharacterAvatar.contains("itemimages/snowface5.gif"), "fails on no crossorigin");
  }

  @Test
  void canParseSnowsuit() {
    var cleanups = new Cleanups(withProperty("snowsuit", ""));

    try (cleanups) {
      CharPaneRequest.processResults(html("request/test_charpane_snowsuit.html"));
      assertThat("snowsuit", isSetTo("hat"));
    }
  }

  @Nested
  class ApiLimitMode {
    @Test
    void parseApiParsesLimitModeNone() {
      var json = ApiRequest.getJSON(html("request/test_api_status_aftercore.json"), "testing");
      assertThat(json, notNullValue());

      CharPaneRequest.parseStatus(json);

      assertThat(KoLCharacter.getLimitMode(), is(LimitMode.NONE));
    }

    @Test
    void parseApiParsesLimitModeUnknownString() {
      var json =
          ApiRequest.getJSON(html("request/test_api_status_limit_mode_unknown.json"), "testing");
      assertThat(json, notNullValue());

      CharPaneRequest.parseStatus(json);

      assertThat(KoLCharacter.getLimitMode(), is(LimitMode.UNKNOWN));
    }

    @Test
    void parseApiParsesLimitModeUnknownObject() {
      var json =
          ApiRequest.getJSON(
              html("request/test_api_status_limit_mode_unknown_int.json"), "testing");
      assertThat(json, notNullValue());

      CharPaneRequest.parseStatus(json);

      assertThat(KoLCharacter.getLimitMode(), is(LimitMode.UNKNOWN));
    }
  }

  @Nested
  class NonCombatForcers {
    @Test
    void anyNoncombatForcerSetsFlagInApi() {
      var cleanups = new Cleanups(withProperty("noncombatForcerActive", false));
      try (cleanups) {
        var json =
            ApiRequest.getJSON(html("request/test_api_status_noncomforcers.json"), "testing");
        assertThat(json, notNullValue());

        CharPaneRequest.parseStatus(json);

        assertThat("noncombatForcerActive", isSetTo(true));
      }
    }

    @Test
    void absenceOfNoncombatForcerUnsetsFlagInApi() {
      var cleanups = new Cleanups(withProperty("noncombatForcerActive", true));
      try (cleanups) {
        var json =
            ApiRequest.getJSON(
                html("request/test_adventure_crystal_ball_handles_noncombat_api_preadventure.json"),
                "testing");
        assertThat(json, notNullValue());

        CharPaneRequest.parseStatus(json);

        assertThat("noncombatForcerActive", isSetTo(false));
      }
    }

    @Test
    void canParseNoncombatModifiersInCharpane() {
      CharPaneRequest.processResults(
          html("request/test_parse_charpane_for_noncombat_forcers.html"));
      assertThat("noncombatForcerActive", isSetTo(true));
    }

    @Test
    void canParseAbsenceOfNoncombatModifiersInCharpane() {
      var cleanups = new Cleanups(withProperty("noncombatForcerActive", true));

      try (cleanups) {
        // This one doesn't hav eany noncombat modifiers
        CharPaneRequest.processResults(html("request/test_charpane_comma_as_homemade_robot.html"));
        assertThat("noncombatForcerActive", isSetTo(false));
      }
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
          new Cleanups(withEquipped(Slot.PANTS, "designer sweatpants"), withProperty("sweat", 0));

      try (cleanups) {
        var result = CharPaneRequest.processResults(html(responseHtml));
        assertThat(result, equalTo(true));
        assertThat("sweat", isSetTo(expectedValue));
      }
    }

    @Test
    void recogniseNoSweatinessDisplayedMeansZeroIfPantsEquipped() {
      var cleanups =
          new Cleanups(withEquipped(Slot.PANTS, "designer sweatpants"), withProperty("sweat", 11));

      try (cleanups) {
        var result = CharPaneRequest.processResults(html("request/test_charpane_basic.html"));
        assertThat(result, equalTo(true));
        assertThat("sweat", isSetTo(0));
      }
    }
  }

  @Nested
  class Score {
    @ParameterizedTest
    @CsvSource({"black, 0", "blue, 2000", "green, 4000", "red, 6000"})
    void parseScore(String color, int expectedScore) {
      var cleanups =
          new Cleanups(
              withEquipped(ItemPool.TRANSFUNCTIONER),
              withProperty("8BitScore", 0),
              withProperty("8BitColor", ""),
              withProperty("8BitBonusTurns", 0));

      try (cleanups) {
        var responseText = html("request/test_charpane_8bit_" + color + "_score.html");
        var result = CharPaneRequest.processResults(responseText);
        assertThat(result, equalTo(true));
        assertThat("8BitScore", isSetTo(expectedScore));
        assertThat("8BitColor", isSetTo(color));
        assertThat("8BitBonusTurns", isSetTo(0));
      }
    }
  }

  @Nested
  class Comma {
    @Test
    void commaGrantsGreyGooseSkills() {
      var cleanups =
          new Cleanups(withFamiliar(FamiliarPool.CHAMELEON, 200), withProperty("commaFamiliar"));

      try (cleanups) {
        CharPaneRequest.processResults(html("request/test_charpane_comma_as_goose.html"));
        assertThat("commaFamiliar", isSetTo("Grey Goose"));
        assertThat(KoLCharacter.hasCombatSkill(SkillPool.CONVERT_MATTER_TO_PROTEIN), is(true));

        // Removed after change
        CharPaneRequest.processResults(html("request/test_charpane_comma_as_homemade_robot.html"));
        assertThat("commaFamiliar", isSetTo("Homemade Robot"));
        assertThat(KoLCharacter.hasCombatSkill(SkillPool.CONVERT_MATTER_TO_PROTEIN), is(false));
      }
    }
  }
}
