package net.sourceforge.kolmafia.textui.parsetree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TypeList implements Iterable<Type> {
  public static TypeList of(Type... types) {
    var typeList = new TypeList();
    for (var t : types) {
      typeList.add(t);
    }
    return typeList;
  }

  protected final List<Type> list = new ArrayList<>();

  public boolean add(final Type n) {
    if (this.find(n.getName()) != null) {
      return false;
    }

    list.add(n);
    return true;
  }

  public boolean addAll(final TypeList t) {
    return list.addAll(t.list);
  }

  public Type find(final String name) {
    return this.list.stream()
        .filter(t -> t.getName().equalsIgnoreCase(name))
        .findFirst()
        .orElse(null);
  }

  @Override
  public Iterator<Type> iterator() {
    return list.iterator();
  }

  public boolean contains(final Type type) {
    return list.contains(type);
  }
}
