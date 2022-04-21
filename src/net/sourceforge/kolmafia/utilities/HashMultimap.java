package net.sourceforge.kolmafia.utilities;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is a basic implementation of a non-shrinking, order-preserving multimap. put() takes an int
 * key, and an arbitrary Object. get() returns an ArrayList of all Objects with the specified key,
 * in their original insertion order, or null if there were none.
 */
public class HashMultimap<V> extends HashMap<Integer, ArrayList<V>> {
  public final void put(int key, V value) {
    ArrayList<V> curr = super.computeIfAbsent(key, k -> new ArrayList<>());

    curr.add(value);
    curr.trimToSize(); // minimize wasted space
  }

  public final ArrayList<V> get(int key) {
    return super.get(key);
  }
}
