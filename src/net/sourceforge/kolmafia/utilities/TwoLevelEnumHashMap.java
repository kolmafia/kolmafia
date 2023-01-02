package net.sourceforge.kolmafia.utilities;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class TwoLevelEnumHashMap<K1 extends Enum<K1>, K2, V> {
  private final Map<K1, Map<K2, V>> level1;

  public TwoLevelEnumHashMap(Class<K1> k1Class) {
    this.level1 = new EnumMap<>(k1Class);
  }

  public int size() {
    return this.level1.values().stream().mapToInt(Map::size).sum();
  }

  public Map<K2, V> getAll(K1 k1) {
    return this.level1.get(k1);
  }

  public V get(K1 k1, K2 k2) {
    Map<K2, V> level2 = this.level1.get(k1);
    if (level2 == null) return null;
    return level2.get(k2);
  }

  public boolean containsKey(K1 k1, K2 k2) {
    return this.get(k1, k2) != null;
  }

  public V put(K1 k1, K2 k2, V value) {
    Map<K2, V> level2 = this.level1.computeIfAbsent(k1, (x) -> new HashMap<>());

    return level2.put(k2, value);
  }

  public V putIfAbsent(K1 k1, K2 k2, V value) {
    Map<K2, V> level2 = this.level1.computeIfAbsent(k1, (x) -> new HashMap<>());

    return level2.putIfAbsent(k2, value);
  }

  public V remove(K1 k1, K2 k2) {
    Map<K2, V> level2 = this.level1.get(k1);
    if (level2 == null) return null;
    return level2.remove(k2);
  }

  public Collection<Map.Entry<K1, Map<K2, V>>> entrySet() {
    return this.level1.entrySet();
  }

  public void clear() {
    this.level1.clear();
  }
}
