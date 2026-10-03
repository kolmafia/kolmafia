package net.sourceforge.kolmafia.modifiers;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class BitmapModifierCollection {
  private final Map<BitmapModifier, Integer> bitmaps = new EnumMap<>(BitmapModifier.class);

  public synchronized void reset() {
    this.bitmaps.clear();
  }

  public void set(BitmapModifierCollection source) {
    // Copy under the source's monitor and assign under ours, never both at once:
    // a.set(b) and b.set(a) on two threads would deadlock.
    Map<BitmapModifier, Integer> copy = source.copyOfBitmaps();
    synchronized (this) {
      this.bitmaps.clear();
      this.bitmaps.putAll(copy);
    }
  }

  private synchronized Map<BitmapModifier, Integer> copyOfBitmaps() {
    return new EnumMap<>(this.bitmaps);
  }

  public synchronized Integer get(final BitmapModifier mod) {
    return this.bitmaps.getOrDefault(mod, 0);
  }

  public synchronized boolean set(BitmapModifier modifier, Integer value) {
    Integer oldValue =
        value == 0 ? this.bitmaps.remove(modifier) : this.bitmaps.put(modifier, value);

    // TODO: does anything use this return value, or can we save ourselves a check?
    return oldValue == null || !oldValue.equals(value);
  }

  public synchronized double add(final BitmapModifier mod, final Integer value) {
    return this.bitmaps.merge(mod, value, (v1, v2) -> v1 | v2);
  }

  // Run the action outside the lock, over a copy: it writes to a different collection.
  public void forEach(BiConsumer<? super BitmapModifier, ? super Integer> action) {
    this.copyOfBitmaps().forEach(action);
  }
}
