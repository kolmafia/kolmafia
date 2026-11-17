package net.sourceforge.kolmafia.utilities;

import static internal.helpers.Player.withUserInterface;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.ui.UserInterface;
import org.junit.jupiter.api.Test;

class PauseObjectTest {
  private static Cleanups withEventDispatchThread(final boolean isEventDispatchThread) {
    return withUserInterface(
        new UserInterface() {
          @Override
          public boolean isEventDispatchThread() {
            return isEventDispatchThread;
          }
        });
  }

  @Test
  void pausesOffTheEventDispatchThread() {
    try (var cleanups = withEventDispatchThread(false)) {
      long start = System.nanoTime();
      new PauseObject().pause(50);

      assertThat((System.nanoTime() - start) / 1_000_000, greaterThanOrEqualTo(45L));
    }
  }

  @Test
  void neverPausesTheEventDispatchThread() {
    try (var cleanups = withEventDispatchThread(true)) {
      long start = System.nanoTime();
      new PauseObject().pause(5000);

      assertThat((System.nanoTime() - start) / 1_000_000, lessThan(1000L));
    }
  }
}
