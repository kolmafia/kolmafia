package net.sourceforge.kolmafia.modifiers;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

// EnumMap and EnumSet are not fail-fast, so the bitmap and boolean collections do not throw
// on a read during a write - they answer, and set() empties before refilling, so the answer
// can be that modifiers which are set are not. These assert that a read of the whole
// collection sees one state or the other, never half of each.
class ModifierCollectionVisibilityTest {
  private static final long RUN_FOR_MS = 2000;
  private static final int HOW_MANY = 12;

  @Test
  void aBitmapCollectionIsNeverIteratedHalfCopied() throws InterruptedException {
    List<BitmapModifier> mods = new ArrayList<>();
    for (BitmapModifier mod : BitmapModifier.values()) {
      mods.add(mod);
      if (mods.size() == HOW_MANY) break;
    }

    var one = new BitmapModifierCollection();
    var other = new BitmapModifierCollection();
    for (BitmapModifier mod : mods) {
      one.set(mod, 1);
      other.set(mod, 2);
    }

    // However many there turn out to be: fewer BitmapModifiers than HOW_MANY.
    final int expected = mods.size();

    var shared = new BitmapModifierCollection();
    shared.set(one);

    race(
        flip -> shared.set(flip ? one : other),
        failure -> {
          var seen = new ArrayList<Integer>();
          shared.forEach((mod, value) -> seen.add(value));
          // Both states set every modifier, so anything missing is half of each copy.
          Integer first = seen.isEmpty() ? null : seen.get(0);
          if (seen.size() != expected || seen.stream().anyMatch(v -> !v.equals(first))) {
            failure.compareAndSet(null, new AssertionError("half-copied: saw " + seen));
          }
        });
  }

  @Test
  void aBooleanCollectionIsNeverReadHalfCopied() throws InterruptedException {
    List<BooleanModifier> mods = new ArrayList<>();
    for (BooleanModifier mod : BooleanModifier.values()) {
      mods.add(mod);
      if (mods.size() == HOW_MANY) break;
    }

    // Both states non-empty and of different sizes: with "none set" as one of them, an
    // empty read would be a state the writer really does copy in.
    var all = new BooleanModifierCollection();
    for (BooleanModifier mod : mods) all.set(mod, true);
    final int whole = mods.size();
    final int part = whole / 2;
    var half = new BooleanModifierCollection();
    for (BooleanModifier mod : mods.subList(0, part)) half.set(mod, true);

    var shared = new BooleanModifierCollection();
    shared.set(all);

    race(
        flip -> shared.set(flip ? all : half),
        failure -> {
          EnumSet<BooleanModifier> seen = shared.raw();
          if (seen.size() != whole && seen.size() != part) {
            failure.compareAndSet(null, new AssertionError("half-copied: " + seen.size() + " set"));
          }
        });
  }

  private interface Writer {
    void write(boolean flip);
  }

  private interface Reader {
    void read(AtomicReference<Throwable> failure);
  }

  // A writer and a reader, against each other, until one of them objects.
  private static void race(Writer writer, Reader reader) throws InterruptedException {
    var failure = new AtomicReference<Throwable>();
    var stop = new AtomicBoolean(false);
    var ready = new CountDownLatch(2);

    Thread w =
        spawn(
            ready,
            stop,
            failure,
            new Runnable() {
              private boolean flip;

              @Override
              public void run() {
                flip = !flip;
                writer.write(flip);
              }
            });
    Thread r = spawn(ready, stop, failure, () -> reader.read(failure));

    ready.await();
    Thread.sleep(RUN_FOR_MS);
    stop.set(true);
    w.join();
    r.join();

    assertNull(failure.get(), () -> String.valueOf(failure.get().getMessage()));
  }

  private static Thread spawn(
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
