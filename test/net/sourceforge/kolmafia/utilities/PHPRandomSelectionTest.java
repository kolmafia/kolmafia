package net.sourceforge.kolmafia.utilities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PHPRandomSelectionTest {

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  void emptyListPicksNothing(final int count) {
    // A single pick rerolls until it misses the overflow value, which with no entries to pick from
    // is every roll. Guarding the empty case is what stops that spinning forever, so bound the test
    // rather than let a regression hang the build.
    assertTimeoutPreemptively(
        Duration.ofSeconds(5),
        () -> assertThat(new PHPRandomSelection(123).pick(0, count).length, equalTo(0)));
  }

  @Test
  void picksNothingForANonPositiveCount() {
    assertThat(new PHPRandomSelection(123).pick(10, 0).length, equalTo(0));
    assertThat(new PHPRandomSelection(123).pick(10, -1).length, equalTo(0));
  }

  @Test
  void singlePickStaysInRange() {
    for (var seed = 0; seed < 200; seed++) {
      var picked = new PHPRandomSelection(seed).pick(15, 1);
      assertThat(picked.length, equalTo(1));
      assertThat(picked[0], greaterThanOrEqualTo(0));
      assertThat(picked[0], lessThan(15));
    }
  }
}
