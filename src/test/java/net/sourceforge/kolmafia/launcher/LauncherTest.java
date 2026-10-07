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

  @ParameterizedTest
  @CsvSource({
    "21, 25, , false",
    "25, 25, , true",
    "27, 25, , true",
    "24, 25, 26, false",
    "26, 25, 26, true",
    "27, 25, 26, false"
  })
  void checksVersionAgainstRange(int version, int minimum, Integer maximum, boolean expected) {
    assertThat(Launcher.isSupported(version, minimum, maximum), is(expected));
  }

  @ParameterizedTest
  @CsvSource({"25, , Java 25 or newer", "25, 25, Java 25", "25, 26, Java 25 to 26"})
  void describesSupportedVersions(int minimum, Integer maximum, String expected) {
    assertThat(Launcher.supportedVersions(minimum, maximum), is(expected));
  }
}
