package net.sourceforge.kolmafia.modifiers;

import java.util.ArrayList;
import java.util.List;

/* For use in DoubleModifierCollection by MultiDoubleModifiers
 */
public class DoubleOrList {
  private double doubleValue = 0.0;
  private List<Double> listValue = null;

  public DoubleOrList(double doubleValue) {
    this.doubleValue = doubleValue;
  }

  public DoubleOrList(List<Double> listValue) {
    this.listValue = listValue;
  }

  public boolean isDouble() {
    return listValue == null;
  }

  public boolean isList() {
    return listValue != null;
  }

  public boolean isDefault() {
    if (isList()) {
      return listValue.isEmpty();
    }
    return doubleValue == 0.0;
  }

  public double getDoubleValue() {
    if (isDouble()) {
      return doubleValue;
    }
    // we're a list
    if (listValue.isEmpty()) {
      return 0.0;
    }
    // want the last value for backwards compat
    return listValue.getLast();
  }

  public List<Double> getListValue() {
    if (isList()) {
      return new ArrayList<>(listValue);
    }
    // we're a double
    if (isDefault()) {
      // translate the default value. we definitely want to do this in some places (e.g. append)
      return new ArrayList<>(List.of());
    }
    return new ArrayList<>(List.of(doubleValue));
  }

  public DoubleOrList append(DoubleOrList newValue) {
    var curVal = getListValue();
    if (newValue.isDouble()) {
      curVal.add(newValue.getDoubleValue());
    } else {
      // don't think this ever happens in practice but support anyway
      curVal.addAll(newValue.getListValue());
    }
    return new DoubleOrList(curVal);
  }

  /**
   * Add newValue, which should be a double, to last entry in mapValue.
   *
   * <p>Reproduce the state before MultiDoubleModifier that progressive doubles would overwrite the
   * last which is potentially not what we want but potentially is
   */
  public static DoubleOrList sum(DoubleOrList mapValue, DoubleOrList newValue) {
    if (!newValue.isDouble()) {
      // shouldn't happen
      return mapValue;
    }
    var newDouble = newValue.getDoubleValue();
    if (mapValue.isDouble()) {
      return new DoubleOrList(mapValue.getDoubleValue() + newDouble);
    }
    var lst = mapValue.getListValue();
    if (lst.isEmpty()) {
      return new DoubleOrList(List.of(newDouble));
    }
    var last = lst.getLast();
    var newLast = last + newDouble;
    lst.set(lst.size() - 1, newLast);
    return new DoubleOrList(lst);
  }

  @Override
  public int hashCode() {
    return isDouble() ? Double.valueOf(this.doubleValue).hashCode() : this.listValue.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof DoubleOrList other)) {
      return false;
    }
    if (isDouble()) {
      if (!other.isDouble()) return false;
      return this.doubleValue == other.doubleValue;
    }
    return this.listValue.equals(other.listValue);
  }

  @Override
  public String toString() {
    return isDouble() ? Double.toString(this.doubleValue) : this.listValue.toString();
  }
}
