package net.sourceforge.kolmafia.launcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LauncherTest {
  @ParameterizedTest
  @CsvSource({"1.8, 8", "11, 11", "21, 21", "25, 25"})
  void parsesSpecificationVersion(String specificationVersion, int expected) {
    assertThat(Launcher.javaVersion(specificationVersion), is(expected));
  }
}
