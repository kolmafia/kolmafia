package net.sourceforge.kolmafia.utilities;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class ChoiceUtilitiesTest {

  @Test
  void extractChoiceFromLyleOnDevServer() {
    var cleanups = withProperty("useDevServer", true);

    try (cleanups) {
      var page = html("request/test_choice_lyle_dev.html");
      var choice = ChoiceUtilities.extractChoice(page);
      assertThat(choice, is(1309));
    }
  }

  @Test
  void parseChoicesFromPeridot() {
    var page = html("request/test_choice_peridot_zone.html");
    var choices = ChoiceUtilities.parseChoices(page);
    assertThat(choices, aMapWithSize(2));
    // The first value for option 1
    assertThat(choices, hasEntry(1, "a sleeping Knob Goblin Guard"));
    assertThat(choices, hasEntry(2, "I choose peace"));
  }
}
