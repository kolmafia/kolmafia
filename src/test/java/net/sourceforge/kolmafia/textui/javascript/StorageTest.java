package net.sourceforge.kolmafia.textui.javascript;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

class StorageTest {
  @Test
  void canFetchKeyByIndex() {
    var storage = new Storage();

    storage.setItem("a", "1");
    storage.setItem("b", "2");
    storage.setItem("c", "3");

    assertThat(storage.getLength(), is(3));
    assertThat(storage.key(1), is("b"));
  }

  @Test
  void clearedStorageHasLengthZero() {
    var storage = new Storage();

    storage.setItem("a", "1");
    storage.setItem("b", "2");
    storage.setItem("c", "3");
    storage.clear();

    assertThat(storage.getLength(), is(0));
  }

  @Test
  void keyIsNullIfOutOfBounds() {
    var storage = new Storage();

    storage.setItem("a", "1");
    storage.setItem("b", "2");
    storage.setItem("c", "3");

    assertThat(storage.key(3), nullValue());
  }
}
