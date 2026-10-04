package net.sourceforge.kolmafia.utilities;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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

  @Test
  void parsesChoiceFromBarrelFullOfBarrels() {
    var page = html("request/test_choice_barrel_full_of_barrels.html");
    var choices = ChoiceUtilities.parseChoices(page);
    assertThat(choices, aMapWithSize(3));
    assertThat(choices, hasEntry(1, "A barrel"));
    assertThat(choices, hasEntry(2, "Turn Crank (1)"));
    assertThat(choices, hasEntry(3, "Exit"));
  }

  @Nested
  class ChoicesWithExtras {
    @Test
    void canParseChoicesWithExtras() {
      String page = html("request/test_choice_peridot.html");
      var choices = ChoiceUtilities.parseFormChoices(page);
      assertThat(choices.size(), is(6));

      var formChoice = choices.get(0);
      assertThat(formChoice.decision(), is(1));
      assertThat(formChoice.hidden(), is(Map.of("bandersnatch", "100")));
      assertThat(formChoice.label(), is("a Ninja Snowman"));

      formChoice = choices.get(3);
      assertThat(formChoice.decision(), is(1));
      assertThat(formChoice.hidden(), is(Map.of("bandersnatch", "339")));
      assertThat(formChoice.label(), is("a Ninja Snowman Janitor"));

      formChoice = choices.get(5);
      assertThat(formChoice.decision(), is(2));
      assertThat(formChoice.hidden(), is(Collections.emptyMap()));
      assertThat(formChoice.label(), is("I choose peace"));
    }

    @Test
    void canDetectDisabledChoices() {
      String page = html("request/test_choice_rosegarden_disabled.html");
      var choices = ChoiceUtilities.parseFormChoices(page);

      List<String> choiceLabels = choices.stream().map(ChoiceUtilities.FormChoice::label).toList();
      assertThat(choiceLabels, hasItem("Fight rose garden gnome at position 20,16"));
      assertThat(choiceLabels, not(hasItem("Open the chest at position 23,25")));
      assertThat(choiceLabels, hasItem("Leave the garden"));
      assertThat(choices.size(), is(8));
    }
  }
}
