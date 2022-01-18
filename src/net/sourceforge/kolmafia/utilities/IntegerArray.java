package net.sourceforge.kolmafia.utilities;

import java.util.ArrayList;
import java.util.Iterator;
import net.sourceforge.kolmafia.objectpool.IntegerPool;

/**
 * Internal class which functions exactly an array of integers, except it uses "sets" and "gets"
 * like a list.
 *
 * <p>This could be done with generics (Java 1.5) but is done like this so that we get backwards
 * compatibility.
 *
 * <p>News flash! Since we have moved to Java 1.5, we can use generics
 */
public class IntegerArray implements Iterable<Integer> {
  private final ArrayList<Integer> internalList = new ArrayList<>();

  @Override
  public Iterator<Integer> iterator() {
    return this.internalList.iterator();
  }

  public void add(final int value) {
    this.set(this.internalList.size(), value);
  }

  public int get(final int index) {
    return index < 0 || index >= this.internalList.size() ? 0 : this.internalList.get(index);
  }

  public void set(final int index, final int value) {
    while (index >= this.internalList.size()) {
      this.internalList.add(IntegerPool.get(0));
    }

    this.internalList.set(index, IntegerPool.get(value));
  }

  public int size() {
    return this.internalList.size();
  }

  public boolean contains(final int value) {
    return this.internalList.contains(IntegerPool.get(value));
  }

  public int[] toArray() {
    int[] array = new int[this.internalList.size()];
    Iterator<Integer> iterator = this.internalList.iterator();

    for (int i = 0; i < array.length; ++i) {
      array[i] = iterator.next();
    }

    return array;
  }
}
