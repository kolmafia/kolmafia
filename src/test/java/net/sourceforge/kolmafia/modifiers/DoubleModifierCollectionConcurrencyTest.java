package net.sourceforge.kolmafia.modifiers;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

// Reading a cached Modifiers while another thread fills it in used to throw from inside
// TreeMap. These run a writer and a reader against each other; without synchronization they
// fail in well under a second.
class DoubleModifierCollectionConcurrencyTest {
  private static final long RUN_FOR_MS = 2000;

  // Enough to churn, few enough that the collection stays a sparse TreeMap.
  private static List<DoubleModifier> churnable() {
    List<DoubleModifier> mods = new ArrayList<>();
    for (DoubleModifier mod : Arrays.asList(DoubleModifier.values())) {
      if (mod.isMultiple()) continue;
      mods.add(mod);
      if (mods.size() == 20) break;
    }
    return mods;
  }

  // One thread writing, one copying, which is what Modifiers' own constructor does.
  @Test
  void copyingSurvivesWritesToTheCollectionBeingCopied() throws InterruptedException {
    var source = new DoubleModifierCollection();
    var mods = churnable();
    var failure = new AtomicReference<Throwable>();
    var stop = new AtomicBoolean(false);
    var ready = new CountDownLatch(2);

    Thread writer =
        run(
            ready,
            stop,
            failure,
            () -> {
              // Fill it, then empty it: 0 is the default, so the entry is removed and the
              // size swings the whole way and back, which is what putAll() reads once and
              // then trusts.
              for (DoubleModifier mod : mods) source.set(mod, 1.0);
              for (DoubleModifier mod : mods) source.set(mod, 0.0);
            });

    Thread copier =
        run(
            ready,
            stop,
            failure,
            () -> {
              var copy = new DoubleModifierCollection();
              copy.set(source);
            });

    ready.await();
    Thread.sleep(RUN_FOR_MS);
    stop.set(true);
    writer.join();
    copier.join();

    assertNull(failure.get(), () -> "copying threw: " + failure.get());
  }

  // The same, for the other way a collection is read whole.
  @Test
  void iteratingSurvivesWritesToTheCollectionBeingIterated() throws InterruptedException {
    var source = new DoubleModifierCollection();
    var mods = churnable();
    var failure = new AtomicReference<Throwable>();
    var stop = new AtomicBoolean(false);
    var ready = new CountDownLatch(2);

    Thread writer =
        run(
            ready,
            stop,
            failure,
            () -> {
              for (DoubleModifier mod : mods) source.set(mod, 1.0);
              for (DoubleModifier mod : mods) source.set(mod, 0.0);
            });

    var sink = new DoubleModifierCollection();
    Thread reader =
        run(
            ready,
            stop,
            failure,
            () ->
                source.forEach(
                    (mod, value) -> {
                      if (value.isDouble()) sink.set(mod, value.getDoubleValue());
                    }));

    ready.await();
    Thread.sleep(RUN_FOR_MS);
    stop.set(true);
    writer.join();
    reader.join();

    assertNull(failure.get(), () -> "iterating threw: " + failure.get());
  }

  private static Thread run(
      CountDownLatch ready, AtomicBoolean stop, AtomicReference<Throwable> failure, Runnable body) {
    Thread thread =
        new Thread(
            () -> {
              ready.countDown();
              while (!stop.get() && failure.get() == null) {
                try {
                  body.run();
                } catch (Throwable t) {
                  failure.compareAndSet(null, t);
                  return;
                }
              }
            });
    thread.setDaemon(true);
    thread.start();
    return thread;
  }
}
