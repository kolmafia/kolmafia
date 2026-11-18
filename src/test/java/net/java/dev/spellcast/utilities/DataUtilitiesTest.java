package net.java.dev.spellcast.utilities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

class DataUtilitiesTest {
  @Test
  void findsABundledImage() {
    var url = DataUtilities.getResource("0star.gif");

    assertThat(url.toString(), endsWith("images/0star.gif"));
  }

  @Test
  void findsNothingForAMissingImage() {
    assertThat(DataUtilities.getResource("no-such-image.gif"), nullValue());
  }
}
