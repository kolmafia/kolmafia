package net.sourceforge.kolmafia.utilities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PHPRandomTest {
  // Just some data sourced from PHP 5.3 code to confirm we're right

  @ParameterizedTest
  @CsvSource({
    "1, 1804289383, 846930886, 1681692777, 1714636915",
    "2147483647, 1065668062, 2142264300, 1066566375, 1064012770",
    "-8008135, 791676115, 1781863512, 1105079286, 549142576"
  })
  void nextInt(
      final int seed, final int first, final int second, final int third, final int fourth) {
    var rng = new PHPRandom(seed);
    assertThat(rng.nextInt(), equalTo(first));
    assertThat(rng.nextInt(), equalTo(second));
    assertThat(rng.nextInt(), equalTo(third));
    assertThat(rng.nextInt(), equalTo(fourth));
  }

  @Test
  void nextIntRange() {
    var rng = new PHPRandom(69420);
    assertThat(rng.nextInt(5), is(1));
    assertThat(rng.nextInt(10, 20), is(12));
    assertThat(rng.nextInt(-5, 100), is(79));
  }

  @ParameterizedTest
  @CsvSource({
    "6969, 0, 2, 3, 1, 2, 4",
    "2147483647, 0, 2, 3, 1, 3, 4",
    "-8008135, 0, 2, 3, 0, 1, 3"
  })
  void array(int seed, int n1, int n2, int n3, int n4, int n5, int n6) {
    var rng = new PHPRandom(seed);
    var pick = rng.array(5, 3);
    assertThat(pick, equalTo(new int[] {n1, n2, n3}));

    pick = rng.array(5, 3);
    assertThat(pick, equalTo(new int[] {n4, n5, n6}));
  }
}
