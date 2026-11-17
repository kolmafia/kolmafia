package net.java.dev.spellcast.utilities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LockableListModelTest {
  /** Records the events a list sends. */
  private static class RecordingListener implements LockableListModel.ListListener {
    final List<String> events = new ArrayList<>();
    final List<Object> sources = new ArrayList<>();

    @Override
    public void contentsChanged(final Object source, final int index0, final int index1) {
      this.events.add("changed " + index0 + "-" + index1);
      this.sources.add(source);
    }

    @Override
    public void intervalAdded(final Object source, final int index0, final int index1) {
      this.events.add("added " + index0 + "-" + index1);
      this.sources.add(source);
    }

    @Override
    public void intervalRemoved(final Object source, final int index0, final int index1) {
      this.events.add("removed " + index0 + "-" + index1);
      this.sources.add(source);
    }
  }

  @Nested
  class Listeners {
    @Test
    void tellsListenersWhatChanged() {
      var list = new LockableListModel<String>();
      var listener = new RecordingListener();
      list.addListListener(listener);

      list.add("a");
      list.add("b");
      list.remove("a");
      list.setSelectedItem("b");

      assertThat(list.getSelectedItem(), is("b"));
      assertThat(
          listener.events, contains("added 0-0", "added 1-1", "removed 0-0", "changed -1--1"));
      assertThat(listener.sources, contains(list, list, list, list));
    }

    @Test
    void stopsTellingRemovedListeners() {
      var list = new LockableListModel<String>();
      var listener = new RecordingListener();
      list.addListListener(listener);
      list.removeListListener(listener);

      list.add("a");

      assertThat(listener.events, empty());
    }

    @Test
    void mirrorsNameTheChangedListAsSource() {
      var list = new LockableListModel<String>();
      list.add("b");
      list.add("a");
      var mirror = list.getMirrorImage();
      var listener = new RecordingListener();
      mirror.addListListener(listener);

      list.sort();

      assertThat(mirror, contains("a", "b"));
      assertThat(listener.sources, contains(list));
    }

    @Test
    void doesNotReenterListeners() {
      var list = new LockableListModel<String>();
      var listener =
          new RecordingListener() {
            @Override
            public void intervalAdded(final Object source, final int index0, final int index1) {
              super.intervalAdded(source, index0, index1);
              list.touch();
            }
          };
      list.addListListener(listener);

      list.add("a");

      assertThat(listener.events, contains("added 0-0"));
    }

    @Test
    void clonesHaveNoListeners() {
      var list = new LockableListModel<String>();
      var listener = new RecordingListener();
      list.addListListener(listener);

      @SuppressWarnings("unchecked")
      var copy = (LockableListModel<String>) list.clone();
      copy.add("a");

      assertThat(listener.events, empty());
    }
  }
}
