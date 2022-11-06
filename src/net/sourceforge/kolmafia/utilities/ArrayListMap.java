package net.sourceforge.kolmafia.utilities;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Map from Integer to value backed by an ArrayList. Useful when keys are nonnegative integers and are dense, with very
 * fast reads.
 * @param <V> Type of map value.
 */
public class ArrayListMap<V> extends AbstractMap<Integer, V> {
  private int size = 0;
  private List<V> contents;

  public ArrayListMap() {
    this.contents = new ArrayList<>();
  }

  public ArrayListMap(int initialCapacity) {
    this.contents = new ArrayList<>(initialCapacity);
  }

  @Override
  public int size() {
    return this.size;
  }

  @Override
  public boolean isEmpty() {
    return this.size == 0;
  }

  @Override
  public boolean containsKey(Object key) {
    return this.get(key) != null;
  }

  @Override
  public V get(Object key) {
    if (!(key instanceof Integer)) {
      return null;
    }

    Integer keyInt = (Integer) key;
    if ( keyInt < 0 || keyInt >= this.contents.size()) {
      return null;
    }

    return this.contents.get(keyInt);
  }

  @Override
  public V put(Integer key, V value) {
    if (this.contents.size() <= key) {
      int needed = key - this.contents.size() + 1;
      this.contents.addAll(Collections.nCopies(needed, null));
    }

    if (this.contents.get(key) == null && value != null) {
      this.size++;
    } else if (this.contents.get(key) != null && value == null) {
      this.size--;
    }
    this.contents.set(key, value);
    return value;
  }

  @Override
  public V remove(Object key) {
    if (!(key instanceof Integer)) {
      return null;
    }

    V current = this.get(key);
    this.put((Integer) key, null);
    return current;
  }

  @Override
  public void clear() {
    this.size = 0;
    this.contents.clear();
  }

  @Override
  public Set<Entry<Integer, V>> entrySet() {
    return IntStream.range(0, this.contents.size()).mapToObj(i -> new SimpleImmutableEntry<>(i, this.contents.get(i))).filter(entry -> entry.getValue() != null).collect(Collectors.toUnmodifiableSet());
  }
}
