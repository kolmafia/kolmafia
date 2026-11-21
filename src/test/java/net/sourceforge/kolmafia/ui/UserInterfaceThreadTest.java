package net.sourceforge.kolmafia.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import java.io.File;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import org.junit.jupiter.api.Test;

class UserInterfaceThreadTest {
  private static final UserInterface DEFAULTS = new UserInterface() {};

  private static void drain() throws InterruptedException {
    var done = new CountDownLatch(1);
    DEFAULTS.invokeLater(done::countDown);
    assertThat(done.await(10, TimeUnit.SECONDS), is(true));
  }

  private static Cleanups withDebugLogRemoved() {
    var debugLog =
        new File(
            KoLConstants.ROOT_LOCATION,
            "DEBUG_" + KoLConstants.DAILY_FORMAT.format(new Date()) + ".txt");
    boolean existed = debugLog.exists();
    return new Cleanups(
        () -> {
          RequestLogger.closeDebugLog();
          if (!existed) {
            debugLog.delete();
          }
        });
  }

  @Test
  void runsWorkOnItsOwnThread() throws Exception {
    var threadName = new AtomicReference<String>();

    DEFAULTS.invokeLater(() -> threadName.set(Thread.currentThread().getName()));
    drain();

    assertThat(threadName.get(), is("UserInterfaceThread"));
  }

  @Test
  void keepsRunningAfterATaskFails() throws Exception {
    var ran = new AtomicBoolean(false);

    try (var cleanups = withDebugLogRemoved()) {
      DEFAULTS.invokeLater(
          () -> {
            throw new IllegalStateException("expected by the test");
          });
      DEFAULTS.invokeLater(() -> ran.set(true));
      drain();

      assertThat(ran.get(), is(true));
    }
  }
}
