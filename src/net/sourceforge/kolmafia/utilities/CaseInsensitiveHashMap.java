package net.sourceforge.kolmafia.utilities;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CaseInsensitiveHashMap<V> extends HashMap<String, V> {
  public CaseInsensitiveHashMap() {
    super();
  }

  CaseInsensitiveHashMap(int initialCapacity) {
    super(initialCapacity);
  }

  CaseInsensitiveHashMap(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  CaseInsensitiveHashMap(Map<? extends String, ? extends V> m) {
    super();
    putAll(m);
  }

  @Override
  public boolean containsKey(Object key) {
    return super.containsKey(key instanceof String ? ((String) key).toLowerCase() : key);
  }

  @Override
  public V get(Object key) {
    return super.get(key instanceof String ? ((String) key).toLowerCase() : key);
  }

  @Override
  public V put(String key, V value) {
    return super.put(key.toLowerCase(), value);
  }

  @Override
  public void putAll(Map<? extends String, ? extends V> m) {
    super.putAll(
        m.entrySet().stream()
            .collect(Collectors.toMap(entry -> entry.getKey().toLowerCase(), Map.Entry::getValue)));
  }

  @Override
  public V remove(Object key) {
    return super.remove(key instanceof String ? ((String) key).toLowerCase() : key);
  }

  @Override
  public V merge(
      String key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
    return super.merge(key.toLowerCase(), value, remappingFunction);
  }

  @Override
  public V compute(
      String key, BiFunction<? super String, ? super V, ? extends V> remappingFunction) {
    return super.compute(key.toLowerCase(), (k, v) -> remappingFunction.apply(k.toLowerCase(), v));
  }

  @Override
  public V computeIfAbsent(String key, Function<? super String, ? extends V> mappingFunction) {
    return super.computeIfAbsent(key.toLowerCase(), (k) -> mappingFunction.apply(k.toLowerCase()));
  }

  @Override
  public V computeIfPresent(
      String key, BiFunction<? super String, ? super V, ? extends V> remappingFunction) {
    return super.computeIfPresent(
        key.toLowerCase(), (k, v) -> remappingFunction.apply(k.toLowerCase(), v));
  }
}
