package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.withProperty;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SetPreferencesCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("SetPreferencesCommandTest");
    Preferences.reset("SetPreferencesCommandTest");
  }

  @Nested
  class Set {
    @BeforeEach
    void setupSet() {
      SetPreferencesCommandTest.this.command = "set";
    }

    @Nested
    class Deprecation {
      @Test
      void warnsWithDefaultNotice() {
        var pref = "deprecatedPref";
        var cleanups = withProperty(pref, "value");

        try (cleanups) {
          Preferences.deprecationNotices.put(pref, "");

          assertThat(
              execute(pref + " = value2").trim(),
              containsString(
                  "Warning: Preference '"
                      + pref
                      + "' is deprecated. This preference is deprecated."));

          Preferences.deprecationNotices.remove(pref);
        }
      }

      @Test
      void warnsWithCustomNotice() {
        var pref = "customDeprecatedPref";
        var customNotice = "Do not use this pref!";

        var cleanups = withProperty(pref, "value");

        try (cleanups) {
          Preferences.deprecationNotices.put(pref, customNotice);

          assertThat(
              execute(pref + " = value2").trim(),
              containsString("Warning: Preference '" + pref + "' is deprecated. " + customNotice));

          Preferences.deprecationNotices.remove(pref);
        }
      }
    }
  }

  @Nested
  class Get {
    @BeforeEach
    void setupGet() {
      SetPreferencesCommandTest.this.command = "get";
    }

    @Nested
    class Deprecation {
      @Test
      void warnsWithDefaultNotice() {
        var pref = "deprecatedPref";
        var cleanups = withProperty(pref, "value");

        try (cleanups) {
          Preferences.deprecationNotices.put(pref, "");

          assertThat(
              execute(pref).trim(),
              containsString(
                  "Warning: Preference '"
                      + pref
                      + "' is deprecated. This preference is deprecated."));

          Preferences.deprecationNotices.remove(pref);
        }
      }

      @Test
      void warnsWithCustomNotice() {
        var pref = "customDeprecatedPref";
        var customNotice = "Do not use this pref!";

        var cleanups = withProperty(pref, "value");

        try (cleanups) {
          Preferences.deprecationNotices.put(pref, customNotice);

          assertThat(
              execute(pref).trim(),
              containsString("Warning: Preference '" + pref + "' is deprecated. " + customNotice));

          Preferences.deprecationNotices.remove(pref);
        }
      }
    }
  }
}
