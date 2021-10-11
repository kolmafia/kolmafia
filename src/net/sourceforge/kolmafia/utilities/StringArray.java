package net.sourceforge.kolmafia.utilities;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Internal class which functions exactly an array of strings, except it uses "sets" and "gets" like
 * a list.
 *
 * <p>This could be done with generics (Java 1.5) but is done like this so that we get backwards
 * compatibility.
 *
 * <p>News flash! Since we have moved to Java 1.5, we can use generics
 */
public class StringArray implements Iterable<String> {
  private final ArrayList<String> internalList = new ArrayList<String>();

  public Iterator<String> iterator() {
    return this.internalList.iterator();
  }

  public String get(final int index) {
    return index < 0 || index >= this.internalList.size() ? "" : this.internalList.get(index);
  }

  public void set(final int index, final String value) {
    while (index >= this.internalList.size()) {
      this.internalList.add("");
    }

    this.internalList.set(index, value);
  }

  public void add(final String s) {
    this.internalList.add(s);
  }

  public void clear() {
    this.internalList.clear();
  }

  public String[] toArray() {
    String[] array = new String[this.internalList.size()];
    this.internalList.toArray(array);
    return array;
  }

  public int size() {
    return this.internalList.size();
  }
}
