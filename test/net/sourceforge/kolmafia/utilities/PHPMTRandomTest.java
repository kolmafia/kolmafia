package net.sourceforge.kolmafia.utilities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
}
