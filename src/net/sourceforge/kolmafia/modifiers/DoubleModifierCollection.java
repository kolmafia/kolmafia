package net.sourceforge.kolmafia.modifiers;

import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;

public class DoubleModifierCollection {
  public static final int SPARSE_DOUBLES_MAX_SIZE = 32;

  // If only a few values are set in doubles, we instead store all modifiers in a sparse TreeMap.
  // When that map gets bigger than SPARSE_DOUBLES_MAX_SIZE, we copy it over to the dense EnumMap.
  private Map<DoubleModifier, Double> doubles = new TreeMap<>();

  public void reset() {
    this.doubles = new TreeMap<>();
  }

  public void densify() {
    if (this.doubles instanceof EnumMap) return;
    Map<DoubleModifier, Double> newDoubles = new EnumMap<>(DoubleModifier.class);
    newDoubles.putAll(this.doubles);
    this.doubles = newDoubles;
  }

  public double get(final DoubleModifier mod) {
    return this.doubles.getOrDefault(mod, 0.0);
  }

  public boolean set(final DoubleModifier mod, final double value) {
    Double oldValue = value == 0.0 ? this.doubles.remove(mod) : this.doubles.put(mod, value);

    if (this.doubles.size() >= DoubleModifierCollection.SPARSE_DOUBLES_MAX_SIZE) {
      this.densify();
    }

    // TODO: does anything use this return value, or can we save ourselves a check?
    return oldValue == null || oldValue != value;
  }

  public double add(final DoubleModifier mod, final double value) {
    // Anything being accumulated onto should be dense.
    this.densify();
    return this.doubles.merge(mod, value, Double::sum);
  }

  public void forEach(BiConsumer<? super DoubleModifier, ? super Double> action) {
    this.doubles.forEach(action);
  }
}
