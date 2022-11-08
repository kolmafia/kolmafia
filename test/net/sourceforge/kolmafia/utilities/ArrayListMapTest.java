package net.sourceforge.kolmafia.utilities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ArrayListMapTest {
  @Test
  void size() {
    Map<Integer, Integer> map = new ArrayListMap<>();
    map.put(0, 0);
    map.put(5, 0);
    assertThat(map.size(), is(2));
  }

  @Test
  void isEmpty() {
    Map<Integer, Integer> map = new ArrayListMap<>();
    assertThat(map.isEmpty(), is(true));
    map.put(0, 0);
    assertThat(map.isEmpty(), is(false));
  }

  @Test
  void containsKey() {
    Map<Integer, Integer> map = new ArrayListMap<>();
    map.put(0, 0);
    map.put(5, 0);

    assertThat(map.containsKey(new Object()), is(false));
    assertThat(map.containsKey(0), is(true));
    assertThat(map.containsKey(5), is(true));
    assertThat(map.containsKey(1), is(false));
    assertThat(map.containsKey(6), is(false));
    assertThat(map.containsKey(-1), is(false));
  }

  @Test
  void get() {
    Map<Integer, Integer> map = new ArrayListMap<>();
    map.put(0, 1);
    map.put(5, 2);

    assertThat(map.get(new Object()), nullValue());
    assertThat(map.get(0), is(1));
    assertThat(map.get(5), is(2));
    assertThat(map.get(1), nullValue());
    assertThat(map.get(6), nullValue());
    assertThat(map.get(-1), nullValue());
  }

  @Test
  void put() {
    Map<Integer, Integer> map = new ArrayListMap<>();
    assertThat(map.put(-1, 0), nullValue()); // should be dropped
    assertThat(map.put(0, 1), nullValue());
    assertThat(map.put(5, 2), nullValue());
    assertThat(map.put(3, 3), nullValue());
    assertThat(map.put(2, 4), nullValue());
    assertThat(map.put(3, null), is(3));
    assertThat(map.put(5, null), is(2));
    assertThat(map.put(5, 5), nullValue());

    assertThat(map.size(), is(3));
    assertThat(map.get(0), is(1));
    assertThat(map.get(1), nullValue());
    assertThat(map.get(2), is(4));
    assertThat(map.get(3), nullValue());
    assertThat(map.get(4), nullValue());
    assertThat(map.get(5), is(5));
  }

  @Test
  void remove() {
    Map<Integer, Integer> map = new ArrayListMap<>();
    map.put(0, 1);
    map.put(5, 2);
    map.put(3, 3);
    map.put(2, 4);
    assertThat(map.remove(3), is(3));
    assertThat(map.remove(5), is(2));
    map.put(5, 5);
    assertThat(map.remove(1), nullValue());
    assertThat(map.remove(new Object()), nullValue());

    assertThat(map.size(), is(3));
    assertThat(map.get(0), is(1));
    assertThat(map.get(1), nullValue());
    assertThat(map.get(2), is(4));
    assertThat(map.get(3), nullValue());
    assertThat(map.get(4), nullValue());
    assertThat(map.get(5), is(5));
  }

  @Test
  void entrySet() {
    Map<Integer, Integer> map = new ArrayListMap<>();
    map.put(0, 1);
    map.put(5, 2);

    var entrySet = map.entrySet();
    for (var entry : map.entrySet()) {
      assertThat(
          (entry.getKey() == 0 && entry.getValue() == 1)
              || (entry.getKey() == 5 && entry.getValue() == 2),
          is(true));
    }
  }
}
