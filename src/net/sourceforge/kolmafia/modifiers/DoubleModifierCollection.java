package net.sourceforge.kolmafia.modifiers;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;

public class DoubleModifierCollection {
  public static final int SPARSE_DOUBLES_MAX_SIZE = 32;

  // If only a few values are set in doubles, we instead store all modifiers in a sparse TreeMap.
  // When that map gets bigger than SPARSE_DOUBLES_MAX_SIZE, we copy it over to the dense EnumMap.
  private Map<DoubleModifier, DoubleOrList> doubles = new TreeMap<>();

  public void reset() {
    this.doubles = new TreeMap<>();
  }

  public void densify() {
    if (this.doubles instanceof EnumMap) return;
    Map<DoubleModifier, DoubleOrList> newDoubles = new EnumMap<>(DoubleModifier.class);
    newDoubles.putAll(this.doubles);
    this.doubles = newDoubles;
  }

  private DoubleOrList get(final DoubleModifier mod) {
    return this.doubles.getOrDefault(mod, new DoubleOrList(0.0));
  }

  public double getDouble(final DoubleModifier mod) {
    var entry = this.doubles.get(mod);
    if (entry == null) return 0.0;
    return entry.getDoubleValue();
  }

  public List<Double> getList(final DoubleModifier mod) {
    var entry = this.doubles.get(mod);
    if (entry == null) return new ArrayList<>(List.of());
    return entry.getListValue();
  }

  public boolean set(final DoubleModifier mod, final double value) {
    return set(mod, new DoubleOrList(value));
  }

  public boolean set(final DoubleModifier mod, final List<Double> value) {
    return set(mod, new DoubleOrList(value));
  }

  private boolean set(final DoubleModifier mod, final DoubleOrList value) {
    var isMultiple = mod.isMultiple();
    var oldValue = get(mod);
    if (isMultiple) {
      // if multi-modifier, then:
      // * if new element is list, replace or remove if default
      // * if new element is double, append to existing list (creating if absent)
      if (value.isList()) {
        if (value.isDefault()) {
          this.doubles.remove(mod);
        } else {
          this.doubles.put(mod, value);
        }
      } else {
        var lst = oldValue.append(value);
        this.doubles.put(mod, lst);
      }
    } else {
      // if not multi-modifier, new value should be a double, so just replace or remove
      if (value.isDefault()) {
        this.doubles.remove(mod);
      } else {
        this.doubles.put(mod, value);
      }
    }

    if (this.doubles.size() >= DoubleModifierCollection.SPARSE_DOUBLES_MAX_SIZE) {
      this.densify();
    }

    // TODO: does anything use this return value, or can we save ourselves a check?
    return !oldValue.equals(value);
  }

  public double increment(final DoubleModifier mod, final double value) {
    // Anything being accumulated onto should be dense.
    this.densify();
    var asDouble = new DoubleOrList(value);
    return this.doubles.merge(mod, asDouble, DoubleOrList::sum).getDoubleValue();
  }

  public void forEach(BiConsumer<? super DoubleModifier, ? super DoubleOrList> action) {
    this.doubles.forEach(action);
  }
}
