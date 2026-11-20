package net.sourceforge.kolmafia.chat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StyledChatBufferTest {
  @AfterEach
  void afterEach() {
    // Resets the highlights to the (empty) highlightList preference
    StyledChatBuffer.initializeHighlights();
  }

  @ParameterizedTest
  @CsvSource({"#ABCDEF, #abcdef", "#008000, #008000", "#ff00FF, #ff00ff"})
  void storesHighlightColorsInLowerCase(final String color, final String stored) {
    assertThat(StyledChatBuffer.addHighlight("Easyfax", color), is("Easyfax\n" + stored));
    int last = StyledChatBuffer.searchStrings.size() - 1;
    assertThat(StyledChatBuffer.searchStrings.get(last), is("easyfax"));
    assertThat(StyledChatBuffer.colorStrings.get(last), is(stored));
  }

  @Test
  void rejectsColorsThatAreNotHexadecimal() {
    assertThrows(
        IllegalArgumentException.class, () -> StyledChatBuffer.addHighlight("Easyfax", "#zzzzzz"));
  }

  @RepeatedTest(20)
  void picksRandomHighlightColorsThatAreNotTooLight() {
    String color = ChatFormatter.getRandomColor();

    assertThat(color, matchesPattern("#[0-9a-f]{6}"));
    int red = Integer.parseInt(color.substring(1, 3), 16);
    int green = Integer.parseInt(color.substring(3, 5), 16);
    int blue = Integer.parseInt(color.substring(5, 7), 16);
    assertThat(red <= 128 || green <= 128 || blue <= 128, is(true));
  }
}
