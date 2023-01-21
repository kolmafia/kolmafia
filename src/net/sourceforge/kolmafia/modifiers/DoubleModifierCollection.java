package net.sourceforge.kolmafia.modifiers;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class DoubleModifierCollection {
  private final Map<DoubleModifier, Double> doubles = new EnumMap<>(DoubleModifier.class);

  public void reset() {
    this.doubles.clear();
  }

  public double get(final DoubleModifier mod) {
    return this.doubles.getOrDefault(mod, 0.0);
  }

  public boolean set(final DoubleModifier mod, final double value) {
    Double oldValue = value == 0.0 ? this.doubles.remove(mod) : this.doubles.put(mod, value);

    // TODO: does anything use this return value, or can we save ourselves a check?
    return oldValue == null || oldValue != value;
  }

  public double add(final DoubleModifier mod, final double value) {
    return this.doubles.merge(mod, value, Double::sum);
  }

  public void forEach(BiConsumer<? super DoubleModifier, ? super Double> action) {
    this.doubles.forEach(action);
  }
}
