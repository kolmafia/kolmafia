package net.sourceforge.kolmafia.textui.javascript;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class Storage {
  private static class IndexedMap<K, V> extends LinkedHashMap<K, V> {
    ArrayList<K> indices = new ArrayList<>();

    @Override
    public V put(K key, V value) {
      super.put(key, value);
      indices.add(key);
      return value;
    }

    @Override
    public V remove(Object key) {
      var value = super.remove(key);
      indices.removeIf(k -> k == key);
      return value;
    }

    public K key(Integer index) {
      return (index < indices.size()) ? indices.get(index) : null;
    }

    @Override
    public void clear() {
      super.clear();
      indices.clear();
    }
  }

  private final IndexedMap<String, String> values = new IndexedMap<>();

  Storage() {}

  public String key(final Integer index) {
    return this.values.key(index);
  }

  public String getItem(final String key) {
    return this.values.get(key);
  }

  public String setItem(final String key, final String value) {
    return this.values.put(key, value);
  }

  public void removeItem(final String key) {
    this.values.remove(key);
  }

  public void clear() {
    this.values.clear();
  }

  public Integer getLength() {
    return this.values.size();
  }
}
