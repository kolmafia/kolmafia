package net.sourceforge.kolmafia.chat;

import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;

import net.sourceforge.kolmafia.utilities.HexColor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class StyledChatBufferTest {
  @AfterEach
  void afterEach() {
    // Resets the highlights to the (empty) highlightList preference
    StyledChatBuffer.initializeHighlights();
  }

  @Test
  void storesHighlights() {
    assertThat(
        StyledChatBuffer.addHighlight("Easyfax", new HexColor("#008000")), is("Easyfax\n#008000"));
    int last = StyledChatBuffer.searchStrings.size() - 1;
    assertThat(StyledChatBuffer.searchStrings.get(last), is("easyfax"));
    assertThat(StyledChatBuffer.colorStrings.get(last), is("#008000"));
  }

  @Test
  void readsHighlightsFromThePreference() {
    try (var cleanups = withProperty("highlightList", "Easyfax\n#ABCDEF")) {
      StyledChatBuffer.initializeHighlights();

      assertThat(StyledChatBuffer.searchStrings, contains("easyfax"));
      assertThat(StyledChatBuffer.colorStrings, contains("#abcdef"));
    }
  }

  @RepeatedTest(20)
  void picksRandomHighlightColorsThatAreNotTooLight() {
    String color = ChatFormatter.getRandomColor().value();

    assertThat(color, matchesPattern("#[0-9a-f]{6}"));
    int red = Integer.parseInt(color.substring(1, 3), 16);
    int green = Integer.parseInt(color.substring(3, 5), 16);
    int blue = Integer.parseInt(color.substring(5, 7), 16);
    assertThat(red <= 128 || green <= 128 || blue <= 128, is(true));
  }
}
