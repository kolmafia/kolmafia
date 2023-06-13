package net.sourceforge.kolmafia.modifiers;

import java.util.EnumMap;
import java.util.Map;

public class BitmapModifierCollection {
  private final Map<BitmapModifier, Integer> bitmaps = new EnumMap<>(BitmapModifier.class);

  public void reset() {
    this.bitmaps.clear();
  }

  public Integer get(final BitmapModifier mod) {
    return this.bitmaps.getOrDefault(mod, 0);
  }

  public boolean set(BitmapModifier modifier, Integer value) {
    Integer oldValue =
        value == 0 ? this.bitmaps.remove(modifier) : this.bitmaps.put(modifier, value);

    // TODO: does anything use this return value, or can we save ourselves a check?
    return oldValue == null || !oldValue.equals(value);
  }

  public double add(final BitmapModifier mod, final Integer value) {
    return this.bitmaps.merge(mod, value, (v1, v2) -> v1 | v2);
  }
}
