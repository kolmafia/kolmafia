package net.sourceforge.kolmafia.modifiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MultiDoubleModifierCollection {
  public static final int SPARSE_DOUBLES_MAX_SIZE = 32;

  // If only a few values are set in doubles, we instead store all modifiers in a sparse TreeMap.
  // When that map gets bigger than SPARSE_DOUBLES_MAX_SIZE, we copy it over to the dense EnumMap.
  private Map<MultiDoubleModifier, List<Double>> doubles = new TreeMap<>();

  public void reset() {
    this.doubles = new TreeMap<>();
  }

  public void densify() {
    if (this.doubles instanceof EnumMap) return;
    Map<MultiDoubleModifier, List<Double>> newDoubles = new EnumMap<>(MultiDoubleModifier.class);
    newDoubles.putAll(this.doubles);
    this.doubles = newDoubles;
  }

  public List<Double> get(final MultiDoubleModifier mod) {
    return this.doubles.getOrDefault(mod, List.of());
  }

  /**
   * Returns last modifier in the list, to reproduce the state before MultiDoubleModifier that
   * prgressive strings would overwrite the last
   *
   * @param mod Modifier
   * @return Value
   */
  public Double getOne(final MultiDoubleModifier mod) {
    var list = get(mod);
    return list.isEmpty() ? 0.0 : list.get(list.size() - 1);
  }

  public boolean set(final MultiDoubleModifier mod, final List<Double> value) {
    List<Double> oldValue =
        value.isEmpty() ? this.doubles.remove(mod) : this.doubles.put(mod, value);

    if (this.doubles.size() >= MultiDoubleModifierCollection.SPARSE_DOUBLES_MAX_SIZE) {
      this.densify();
    }

    // TODO: does anything use this return value, or can we save ourselves a check?
    return oldValue == null || !oldValue.equals(value);
  }

  public boolean add(MultiDoubleModifier modifier, Double value) {
    var list = new ArrayList<>(get(modifier));
    var success = list.add(value);
    this.set(modifier, Collections.unmodifiableList(list));
    return success;
  }

  /**
   * Adds delta to last modifier in the list, to reproduce the state before MultiDoubleModifier that
   * prgressive doubles would overwrite the last
   *
   * @param mod Modifier
   * @param delta increment
   */
  public void addLast(final MultiDoubleModifier mod, final double delta) {
    var list = get(mod);
    if (list.isEmpty()) {
      this.add(mod, delta);
      return;
    }
    var last = list.get(list.size() - 1);
    var newValue = last + delta;
    var newList = new ArrayList<>(list);
    newList.set(list.size() - 1, newValue);
    this.set(mod, Collections.unmodifiableList(newList));
  }
}
