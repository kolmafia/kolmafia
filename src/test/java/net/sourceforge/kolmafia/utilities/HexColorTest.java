package net.sourceforge.kolmafia.utilities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class HexColorTest {
  @ParameterizedTest
  @CsvSource({"#ABCDEF, #abcdef", "#008000, #008000", "#ff00FF, #ff00ff"})
  void keepsColorsInLowerCase(final String color, final String expected) {
    assertThat(new HexColor(color).value(), is(expected));
  }

  @ParameterizedTest
  @ValueSource(strings = {"#zzzzzz", "008000", "#00800", "#0080000", "red"})
  void rejectsOtherText(final String color) {
    assertThrows(IllegalArgumentException.class, () -> new HexColor(color));
  }

  @Test
  void buildsAColorFromItsComponents() {
    assertThat(new HexColor(0, 128, 255).value(), is("#0080ff"));
  }

  @Test
  void rejectsComponentsOutOfRange() {
    assertThrows(IllegalArgumentException.class, () -> new HexColor(256, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> new HexColor(0, -1, 0));
  }

  @Test
  void printsAsHtmlDoes() {
    assertThat("<font color=" + new HexColor(128, 0, 0) + ">", is("<font color=#800000>"));
  }
}
