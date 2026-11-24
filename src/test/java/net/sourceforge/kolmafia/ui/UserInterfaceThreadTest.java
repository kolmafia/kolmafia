package net.sourceforge.kolmafia.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.chat.ChatBuffer;
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

  @Test
  void keepsChatMessagesFromManyThreads() throws Exception {
    var buffer = new ChatBuffer("test");

    List<Thread> writers = new ArrayList<>();
    for (int w = 0; w < 4; w++) {
      int writer = w;
      writers.add(
          new Thread(
              () -> {
                for (int i = 0; i < 100; i++) {
                  buffer.append("w" + writer + "m" + i + "<br>");
                }
              }));
    }
    writers.forEach(Thread::start);
    for (Thread writer : writers) {
      writer.join();
    }
    drain();

    assertThat(buffer.getContent().split("<br>").length, is(400));
    buffer.dispose();
  }
}
