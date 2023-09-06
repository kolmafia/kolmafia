package net.sourceforge.kolmafia.textui.javascript;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

class StorageTest {
    @Test
    void canFetchKeyByIndex() {
        var storage = new Storage();

        storage.setItem("a", "1");
        storage.setItem("b", "2");
        storage.setItem("c", "3");

        assertThat(storage.getLength(), is(3));
        assertThat(storage.key(1), is("2"));
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

}