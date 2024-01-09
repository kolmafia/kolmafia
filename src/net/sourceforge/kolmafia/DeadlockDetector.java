package net.sourceforge.kolmafia;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

class DeadlockDetector {
  static ThreadMXBean bean = ManagementFactory.getThreadMXBean();
  static ScheduledExecutorService executor;
  static ScheduledFuture<?> task;

  static final void findDeadlocks() {
    long[] threads = bean.findDeadlockedThreads();
    if (threads == null) {
      return;
    }
    ThreadInfo[] infos =
        bean.getThreadInfo(threads, /*lockedMonitors=*/ true, /*lockedSynchronizers=*/ true);
    RequestLogger.openDebugLog();
    for (ThreadInfo info : infos) {
      RequestLogger.updateDebugLog(info);
      var stack = info.getStackTrace();
      if (stack.length <= 8) {
        continue;
      }
      // Print the rest of the stack frames.
      for (int i = 8; i < stack.length; ++i) {
        RequestLogger.updateDebugLog("        at " + stack[i]);
      }
      RequestLogger.updateDebugLog();
    }
    RequestLogger.closeDebugLog();
    // We don't need a second debug log...
    task.cancel(false);
  }

  public static void registerDeadlockDetector() {
    if (!bean.isSynchronizerUsageSupported()) {
      return;
    }
    executor = Executors.newSingleThreadScheduledExecutor();
    task =
        executor.scheduleWithFixedDelay(
            () -> {
              DeadlockDetector.findDeadlocks();
            },
            10,
            10,
            TimeUnit.SECONDS);
  }
}
