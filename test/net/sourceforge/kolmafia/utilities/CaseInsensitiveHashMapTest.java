package net.sourceforge.kolmafia.utilities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

public class CaseInsensitiveHashMapTest {
  @Test
  public void construct() {
    var map = new CaseInsensitiveHashMap<>(Map.of("A", 0, "b", 0));

    assertThat(map.size(), equalTo(2));
    assertThat(map.keySet(), contains("a", "b"));
  }

  @Test
  public void containsKey() {
    var map = new CaseInsensitiveHashMap<>(Map.of("A", 0, "b", 0));

    assertThat(map.size(), equalTo(2));
    assertThat(map.containsKey("A"), is(true));
    assertThat(map.containsKey("a"), is(true));
    assertThat(map.containsKey("B"), is(true));
    assertThat(map.containsKey("b"), is(true));
    assertThat(map.containsKey("c"), is(false));
  }

  @Test
  public void get() {
    var map = new CaseInsensitiveHashMap<>(Map.of("A", 0, "b", 1));

    assertThat(map.size(), equalTo(2));
    assertThat(map.get("A"), equalTo(0));
    assertThat(map.get("a"), equalTo(0));
    assertThat(map.get("B"), equalTo(1));
    assertThat(map.get("b"), equalTo(1));
    assertThat(map.get("c"), nullValue());
  }

  @Test
  public void put() {
    var map = new CaseInsensitiveHashMap<>(Map.of("A", 0));
    map.put("a", 1);
    map.put("B", 2);

    assertThat(map.size(), equalTo(2));
    assertThat(map.get("A"), equalTo(1));
    assertThat(map.get("a"), equalTo(1));
    assertThat(map.get("B"), equalTo(2));
    assertThat(map.get("b"), equalTo(2));
    assertThat(map.get("c"), nullValue());
  }

  @Test
  public void putAll() {
    var map = new CaseInsensitiveHashMap<>(Map.of("A", 0));
    map.putAll(Map.of("a", 1, "B", 2));

    assertThat(map.size(), equalTo(2));
    assertThat(map.get("A"), equalTo(1));
    assertThat(map.get("a"), equalTo(1));
    assertThat(map.get("B"), equalTo(2));
    assertThat(map.get("b"), equalTo(2));
    assertThat(map.get("c"), nullValue());
  }

  @Test
  public void remove() {
    var map = new CaseInsensitiveHashMap<>(Map.of("A", 0, "b", 0));
    map.remove("B");

    assertThat(map.size(), equalTo(1));
    assertThat(map.get("A"), equalTo(0));
    assertThat(map.get("a"), equalTo(0));
    assertThat(map.get("b"), nullValue());
  }

  @Test
  public void merge() {
    var map = new CaseInsensitiveHashMap<>(Map.of("A", 1));
    map.merge("a", 1, (existingValue, newValue) -> existingValue + newValue);
    map.merge("b", 1, (existingValue, newValue) -> existingValue + newValue);

    assertThat(map.size(), equalTo(2));
    assertThat(map.get("A"), equalTo(2));
    assertThat(map.get("a"), equalTo(2));
    assertThat(map.get("B"), equalTo(1));
    assertThat(map.get("b"), equalTo(1));
    assertThat(map.get("c"), nullValue());
  }

  @Test
  public void compute() {
    var map = new CaseInsensitiveHashMap<>(Map.of("A", 1));
    map.compute("a", (key, existing) -> existing == null ? 0 : 1 + existing);
    map.compute("b", (key, existing) -> existing == null ? 0 : 1 + existing);

    assertThat(map.size(), equalTo(2));
    assertThat(map.get("A"), equalTo(2));
    assertThat(map.get("a"), equalTo(2));
    assertThat(map.get("B"), equalTo(0));
    assertThat(map.get("b"), equalTo(0));
    assertThat(map.get("c"), nullValue());
  }

  @Test
  public void computeIfAbsent() {
    var map = new CaseInsensitiveHashMap<>(Map.of("A", 1));
    map.computeIfAbsent("a", (key) -> 2);
    map.computeIfAbsent("b", (key) -> 2);

    assertThat(map.size(), equalTo(2));
    assertThat(map.get("A"), equalTo(1));
    assertThat(map.get("a"), equalTo(1));
    assertThat(map.get("B"), equalTo(2));
    assertThat(map.get("b"), equalTo(2));
    assertThat(map.get("c"), nullValue());
  }

  @Test
  public void computeIfPresent() {
    var map = new CaseInsensitiveHashMap<>(Map.of("A", 1));
    map.computeIfPresent("a", (key, existing) -> 1 + existing);
    map.computeIfPresent("b", (key, existing) -> 1 + existing);

    assertThat(map.size(), equalTo(1));
    assertThat(map.get("A"), equalTo(2));
    assertThat(map.get("a"), equalTo(2));
    assertThat(map.get("b"), nullValue());
  }
}
