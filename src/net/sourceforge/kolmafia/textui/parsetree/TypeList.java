package net.sourceforge.kolmafia.textui.parsetree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TypeList implements Iterable<Type> {
  private final List<Type> list = new ArrayList<>();

  public boolean add(final Type n) {
    if (this.find(n.getName()) != null) {
      return false;
    }

    list.add(n);
    return true;
  }

  public Type find(final String name) {
    for (Type currentType : this.list) {
      if (currentType.getName().equalsIgnoreCase(name)) {
        return currentType;
      }
    }

    return null;
  }

  @Override
  public Iterator<Type> iterator() {
    return list.iterator();
  }

  public boolean contains(final Type type) {
    return list.contains(type);
  }
}
