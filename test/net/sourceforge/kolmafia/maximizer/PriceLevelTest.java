package net.sourceforge.kolmafia.maximizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class PriceLevelTest {

  @Nested
  class ByIndex {
    @Test
    public void index0ReturnsDontCheck() {
      assertEquals(PriceLevel.DONT_CHECK, PriceLevel.byIndex(0));
    }

    @Test
    public void index1ReturnsBuyableOnly() {
      assertEquals(PriceLevel.BUYABLE_ONLY, PriceLevel.byIndex(1));
    }

    @Test
    public void index2ReturnsAll() {
      assertEquals(PriceLevel.ALL, PriceLevel.byIndex(2));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 3, 100, Integer.MIN_VALUE, Integer.MAX_VALUE})
    public void invalidIndexReturnsDontCheck(int index) {
      // Default/backwards compat returns DONT_CHECK
      assertEquals(PriceLevel.DONT_CHECK, PriceLevel.byIndex(index));
    }
  }

  @Nested
  class EnumValues {
    @Test
    public void allValuesExist() {
      assertEquals(3, PriceLevel.values().length);
    }

    @ParameterizedTest
    @CsvSource({"DONT_CHECK, 0", "BUYABLE_ONLY, 1", "ALL, 2"})
    public void ordinalValuesAreCorrect(String name, int expectedOrdinal) {
      PriceLevel level = PriceLevel.valueOf(name);
      assertEquals(expectedOrdinal, level.ordinal());
    }
  }
}
