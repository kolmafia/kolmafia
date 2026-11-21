package net.sourceforge.kolmafia.ui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.sourceforge.kolmafia.StaticEntity;

/**
 * Runs the default {@link UserInterface#invokeLater} work one task at a time and in order, as
 * Swing's event thread does: chat buffers rely on that.
 */
final class UserInterfaceThread {
  private static final ExecutorService EXECUTOR =
      Executors.newSingleThreadExecutor(
          r -> {
            Thread t = new Thread(r, "UserInterfaceThread");
            t.setDaemon(true);
            return t;
          });

  private UserInterfaceThread() {}

  static void execute(final Runnable runnable) {
    UserInterfaceThread.EXECUTOR.execute(
        () -> {
          // Like Swing's event thread, keep running after a task fails
          try {
            runnable.run();
          } catch (Throwable e) {
            StaticEntity.printStackTrace(e);
          }
        });
  }
}
