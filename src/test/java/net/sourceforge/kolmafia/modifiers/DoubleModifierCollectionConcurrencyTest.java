package net.sourceforge.kolmafia.modifiers;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * A Modifiers in ModifierDatabase's cache is shared, and more than one thread reads it: the relay
 * browser rendering a page, a script in the CLI, the main thread after a request. Reading one while
 * another thread is still filling it in used to throw from inside TreeMap - and not where the
 * reading was, but wherever the aborted script happened to be.
 *
 * <p>These run the two things against each other on purpose. Without synchronization they fail in
 * well under a second, from inside TreeMap's own iterator: a ConcurrentModificationException when
 * the write is seen, or a NoSuchElementException when it isn't and the map simply runs out before
 * the count putAll() was promised. The second is what reached a user, as "Script execution aborted
 * (java.util.NoSuchElementException)" in a script that had nothing to do with modifiers - and what
 * left a half-copied map in ModifierDatabase's cache for every later read to trip over. Which of
 * the two comes out is a matter of where the writer happens to be, so neither is asserted on: what
 * is asserted is that reading a collection while it is written does not throw.
 */
class DoubleModifierCollectionConcurrencyTest {
  private static final long RUN_FOR_MS = 2000;

  /** Enough to churn, few enough that the collection stays a sparse TreeMap. */
  private static List<DoubleModifier> churnable() {
    List<DoubleModifier> mods = new ArrayList<>();
    for (DoubleModifier mod : Arrays.asList(DoubleModifier.values())) {
      if (mod.isMultiple()) continue;
      mods.add(mod);
      if (mods.size() == 20) break;
    }
    return mods;
  }

  /**
   * One thread writing, one copying. The copy is what Modifiers' own constructor does, and where
   * putAll() asks a map how big it is and then reads that many entries out of it.
   */
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
              // Fill it, then empty it. A value of 0 is the default, which removes the entry, so
              // the size swings the whole way and back on every pass - and the size is exactly
              // what putAll() reads once and then trusts. An earlier version of this set the even
              // entries and removed the odd ones, which leaves the size where it was after the
              // first pass and races against nothing at all.
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

  /** The same, for the other way a collection is read whole. */
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
