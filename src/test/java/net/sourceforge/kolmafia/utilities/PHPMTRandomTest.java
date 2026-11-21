package net.sourceforge.kolmafia.utilities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PHPMTRandomTest {
  // Just some data sourced from PHP 5.3 code to confirm we're right

  @ParameterizedTest
  @CsvSource({
    "1, 1244335972, 15217923, 1546885062, 2002651684",
    "2147483647, 844801015, 1915574197, 726043576, 780612688"
  })
  void nextInt(
      final int seed, final int first, final int second, final int third, final int fourth) {
    var rng = new PHPMTRandom(seed);
    assertThat(rng.nextInt(), equalTo(first));
    assertThat(rng.nextInt(), equalTo(second));
    assertThat(rng.nextInt(), equalTo(third));
    assertThat(rng.nextInt(), equalTo(fourth));
  }

  @Test
  void negativeSeed() {
    var rng = new PHPMTRandom(-8008135);
    assertThat(rng.nextInt(), equalTo(595078597));
    assertThat(rng.nextInt(), equalTo(690674654));
    assertThat(rng.nextInt(), equalTo(1259522838));
    assertThat(rng.nextInt(), equalTo(277454392));
  }

  @Test
  void nextIntRange() {
    var rng = new PHPMTRandom(69420);
    assertThat(rng.nextInt(5), is(2));
    assertThat(rng.nextInt(10, 20), is(11));
    assertThat(rng.nextInt(-5, 100), is(32));
  }

  @Test
  void nextIntAfter700Calls() {
    var rng = new PHPMTRandom(768);
    for (int i = 0; i < 700; i++) {
      rng.nextInt();
    }
    assertThat(rng.nextInt(), equalTo(49898254));
  }

  @ParameterizedTest
  @ValueSource(
      longs = {
        0,
        1,
        768,
        -8008135,
        1_000_000,
        9_999_999,
        Integer.MAX_VALUE,
        0x1_0000_0000L,
        Long.MIN_VALUE,
        Long.MAX_VALUE
      })
  void matchesEagerSequenceAcrossMultipleBlocks(final long seed) {
    var expected = eagerSequence(seed, 2500);
    var rng = new PHPMTRandom(seed);
    for (int i = 0; i < expected.length; i++) {
      assertThat("draw " + i, rng.nextInt(), is(expected[i]));
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 226, 227, 623, 624, 625, 1248})
  void reseedsAfterPartialInitializationAndOutputBlocks(final int draws) {
    var rng = new PHPMTRandom(768);
    for (int i = 0; i < draws; i++) {
      rng.nextInt();
    }
    rng.setSeed(-8008135);

    var expected = eagerSequence(-8008135, 1300);
    for (int i = 0; i < expected.length; i++) {
      assertThat("draw " + i, rng.nextInt(), is(expected[i]));
    }
  }

  // Keep the eager algorithm as a reference for the demand-driven state updates.
  private static int[] eagerSequence(long seed, final int count) {
    if (seed < 0) {
      seed += 0x100000000L;
    }
    var state = new long[624];
    state[0] = seed & Long.MAX_VALUE;
    for (int i = 1; i < state.length; i++) {
      long prev = state[i - 1];
      state[i] = ((1_812_433_253L * (prev ^ (prev >> 30))) + i) & 4_294_967_295L;
    }

    var values = new int[count];
    for (int offset = 0; offset < count; offset += state.length) {
      for (int i = 0; i < state.length; i++) {
        state[i] =
            PHPMTRandom.twist(
                state[(i + 397) % state.length], state[i], state[(i + 1) % state.length]);
      }
      for (int i = 0; i < state.length && offset + i < count; i++) {
        values[offset + i] = (int) (PHPMTRandom.temper(state[i]) >> 1);
      }
    }
    return values;
  }
}
