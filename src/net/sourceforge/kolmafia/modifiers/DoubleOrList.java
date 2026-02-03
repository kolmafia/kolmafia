package net.sourceforge.kolmafia.modifiers;

import java.util.List;

/* For use in DoubleModifierCollection by MultiDoubleModifiers
 */
public class DoubleOrList extends ListOrT<Double> {
  public DoubleOrList(double doubleValue) {
    super(doubleValue);
  }

  public DoubleOrList(List<Double> listValue) {
    super(listValue);
  }

  public boolean isDouble() {
    return isT();
  }

  @Override
  public Double defaultT() {
    return 0.0;
  }

  public double getDoubleValue() {
    return getTValue();
  }

  public DoubleOrList append(DoubleOrList newValue) {
    return super.append(newValue, DoubleOrList::new);
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
}
