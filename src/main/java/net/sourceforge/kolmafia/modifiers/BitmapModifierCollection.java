package net.sourceforge.kolmafia.modifiers;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The bitmaps of one Modifiers, and the map they live in. Synchronized for the reasons set out on
 * {@link DoubleModifierCollection}, and silent when it goes wrong for the reasons set out on {@link
 * StringModifierCollection}.
 */
public class BitmapModifierCollection {
  private final Map<BitmapModifier, Integer> bitmaps = new EnumMap<>(BitmapModifier.class);

  public synchronized void reset() {
    this.bitmaps.clear();
  }

  public void set(BitmapModifierCollection source) {
    // Two monitors, never held together; see StringModifierCollection.set.
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

  /** Outside the lock, over a copy: the action writes to another collection. */
  public void forEach(BiConsumer<? super BitmapModifier, ? super Integer> action) {
    this.copyOfBitmaps().forEach(action);
  }
}
