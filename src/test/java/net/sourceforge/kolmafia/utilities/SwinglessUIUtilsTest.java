package net.sourceforge.kolmafia.utilities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class SwinglessUIUtilsTest {
  @Test
  void findsSwingOnTheDesktop() {
    // The test JVM includes the java.desktop module
    assertThat(SwinglessUIUtils.isSwingAvailable(), is(true));
  }
}
