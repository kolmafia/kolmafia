package net.sourceforge.kolmafia.utilities;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ChoiceUtilitiesTest {
  @Nested
  class ValidateChoiceFields {
    @Test
    void cannotSupplyRandomExtraValues() {
      var page = html("request/test_choice_peridot_zone.html");
      var errors = ChoiceUtilities.validateChoiceFields("1", "author=gausie&bandersnatch=1", page);
      assertThat(errors, equalTo("Choice option 1557/1 does not require 'author'.\n"));
    }

    @Test
    void cannotMissRequiredExtraValues() {
      var page = html("request/test_choice_peridot_zone.html");
      var errors = ChoiceUtilities.validateChoiceFields("1", "", page);
      assertThat(
          errors, equalTo("Choice option 1557/1 requires 'bandersnatch' but not supplied.\n"));
    }

    @Test
    void canMixMultipleErrorMessages() {
      var page = html("request/test_choice_peridot_zone.html");
      var errors = ChoiceUtilities.validateChoiceFields("1", "author=gausie", page);
      assertThat(
          errors,
          equalTo(
              "Choice option 1557/1 requires 'bandersnatch' but not supplied.\nChoice option 1557/1 does not require 'author'.\n"));
    }
  }

  @Test
  void extractChoiceFromLyleOnDevServer() {
    var cleanups = withProperty("useDevServer", true);

    try (cleanups) {
      var page = html("request/test_choice_lyle_dev.html");
      var choice = ChoiceUtilities.extractChoice(page);
      assertThat(choice, is(1309));
    }
  }

  @Nested
  class PeridotOfPeril {
    @Test
    void parseChoicesFromPeridot() {
      var page = html("request/test_choice_peridot_zone.html");
      var choices = ChoiceUtilities.parseChoices(page);
      assertThat(choices, aMapWithSize(2));
      // The first value for option 1
      assertThat(choices, hasEntry(1, "a sleeping Knob Goblin Guard"));
      assertThat(choices, hasEntry(2, "I choose peace"));
    }

    @Test
    void canSupplyBandersnatchToPeridot() {
      var page = html("request/test_choice_peridot_zone.html");
      var errors = ChoiceUtilities.validateChoiceFields("1", "bandersnatch=555", page);
      assertThat(errors, is(nullValue()));
    }
  }
}
