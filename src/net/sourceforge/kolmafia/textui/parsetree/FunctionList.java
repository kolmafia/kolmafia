package net.sourceforge.kolmafia.textui.parsetree;

import java.util.Iterator;
import java.util.TreeMap;

public class FunctionList implements Iterable<Function> {
  private final TreeMap<String, Function> list = new TreeMap<String, Function>();

  // Assumes there will not be more than 65535 functions in any scope.
  // Assumes that \0 will never appear in a function name.
  private char sequence = '\0';

  public boolean add(final Function f) {
    this.list.put(f.getName().toLowerCase() + '\0' + this.sequence, f);
    ++this.sequence;
    return true;
  }

  public boolean remove(final Function f) {
    return this.list.values().remove(f);
  }

  public Function[] findFunctions(String name) {
    name = name.toLowerCase();
    return this.list.subMap(name + '\0', name + '\1').values().toArray(new Function[0]);
  }

  public boolean isEmpty() {
    return list.isEmpty();
  }

  @Override
  public Iterator<Function> iterator() {
    return list.values().iterator();
  }
}
