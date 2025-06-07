package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.withDay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.Month;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LoginManagerTest {
  @ParameterizedTest
  @CsvSource({
    "2023, 9, 14, 'Generic Summer Holiday today, 2 days until Mysticism.'",
    "2010, 1, 1, 'Festival of Jarlsberg today, Moxie bonus tomorrow (not today).'",
    "2020, 5, 5, '6 days until Labór Day, Moxie bonus today and tomorrow.'",
  })
  void getCurrentHoliday(final int year, final int month, final int day, final String text) {
    try (var cleanups = withDay(year, Month.of(month), day)) {
      assertThat(LoginManager.getCurrentHoliday(), is(text));
    }
  }
}
