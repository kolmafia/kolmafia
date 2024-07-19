package net.sourceforge.kolmafia.utilities;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
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
}
