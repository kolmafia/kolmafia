package net.sourceforge.kolmafia.utilities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.ui.UserInterface;
import net.sourceforge.kolmafia.ui.UserInterfaceManager;
import org.junit.jupiter.api.Test;

class PauseObjectTest {
  private static Cleanups withUiThread(final boolean isUiThread) {
    UserInterfaceManager.setUserInterface(
        new UserInterface() {
          @Override
          public boolean isUiThread() {
            return isUiThread;
          }
        });
    return new Cleanups(() -> UserInterfaceManager.setUserInterface(null));
  }

  @Test
  void pausesOffTheUserInterfaceThread() {
    try (var cleanups = withUiThread(false)) {
      long start = System.nanoTime();
      new PauseObject().pause(50);

      assertThat((System.nanoTime() - start) / 1_000_000, greaterThanOrEqualTo(45L));
    }
  }

  @Test
  void neverPausesTheUserInterfaceThread() {
    try (var cleanups = withUiThread(true)) {
      long start = System.nanoTime();
      new PauseObject().pause(5000);

      assertThat((System.nanoTime() - start) / 1_000_000, lessThan(1000L));
    }
  }
}
