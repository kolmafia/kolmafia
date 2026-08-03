package net.sourceforge.kolmafia.modifiers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class ListOrT<T> {
  protected T tValue;
  protected List<T> listValue;

  public ListOrT(T tValue) {
    this.tValue = tValue;
  }

  public ListOrT(List<T> listValue) {
    this.listValue = listValue;
  }

  public boolean isT() {
    return listValue == null;
  }

  public boolean isList() {
    return listValue != null;
  }

  public boolean isDefault() {
    if (isList()) {
      return listValue.isEmpty();
    }
    return tValue.equals(defaultT());
  }

  public abstract T defaultT();

  public T getTValue() {
    if (isT()) {
      return tValue;
    }
    // we're a list
    if (listValue.isEmpty()) {
      return defaultT();
    }
    // want the last value for backwards compat
    return listValue.getLast();
  }

  public List<T> getListValue() {
    if (isList()) {
      return new ArrayList<>(listValue);
    }
    // we're a T
    if (isDefault()) {
      // translate the default value
      return new ArrayList<>(List.of());
    }
    return new ArrayList<>(List.of(tValue));
  }

  public <R extends ListOrT<T>> R append(ListOrT<T> newValue, Function<List<T>, R> factory) {
    var curVal = getListValue();
    if (newValue.isT()) {
      curVal.add(newValue.getTValue());
    } else {
      // don't think this ever happens in practice but support anyway
      curVal.addAll(newValue.getListValue());
    }
    return factory.apply(curVal);
  }

  @Override
  public int hashCode() {
    return isT() ? this.tValue.hashCode() : this.listValue.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ListOrT<?> other)) {
      return false;
    }
    if (isT()) {
      if (!other.isT()) return false;
      return this.tValue.equals(other.tValue);
    }
    return this.listValue.equals(other.listValue);
  }

  @Override
  public String toString() {
    return isT() ? this.tValue.toString() : this.listValue.toString();
  }
}
