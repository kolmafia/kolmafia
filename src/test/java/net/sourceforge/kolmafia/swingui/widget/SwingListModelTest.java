package net.sourceforge.kolmafia.swingui.widget;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import net.java.dev.spellcast.utilities.LockableListModel;
import org.junit.jupiter.api.Test;

class SwingListModelTest {
  private static class RecordingListener implements ListDataListener {
    final List<String> events = new ArrayList<>();
    final List<Object> sources = new ArrayList<>();

    private void record(final String kind, final ListDataEvent e) {
      this.events.add(kind + " " + e.getIndex0() + "-" + e.getIndex1());
      this.sources.add(e.getSource());
    }

    @Override
    public void intervalAdded(final ListDataEvent e) {
      this.record("added", e);
    }

    @Override
    public void intervalRemoved(final ListDataEvent e) {
      this.record("removed", e);
    }

    @Override
    public void contentsChanged(final ListDataEvent e) {
      this.record("changed", e);
    }
  }

  @Test
  void showsItsList() {
    var list = new LockableListModel<String>();

    assertThat(SwingListModel.listOf(SwingListModel.of(list)), sameInstance(list));
    assertThat(SwingListModel.listOf(new DefaultListModel<String>()), nullValue());
    assertThat(SwingListModel.of(null), nullValue());
  }

  @Test
  void showsTheListAndSharesItsSelection() {
    var list = new LockableListModel<String>();
    list.add("a");
    list.add("b");
    var model = SwingListModel.of(list);

    model.setSelectedItem("b");

    assertThat(model.getSize(), is(2));
    assertThat(model.getElementAt(1), is("b"));
    assertThat(list.getSelectedItem(), is("b"));
    assertThat(model.getSelectedItem(), is("b"));
    assertThat(SwingListModel.of(list).getSelectedItem(), is("b"));
  }

  @Test
  void passesChangesOnWithTheirSource() {
    var list = new LockableListModel<String>();
    var listener = new RecordingListener();
    SwingListModel.of(list).addListDataListener(listener);

    list.add("a");
    list.remove("a");

    assertThat(listener.events, contains("added 0-0", "removed 0-0"));
    assertThat(listener.sources, contains(list, list));
  }

  @Test
  void listensToTheListOnlyWhileSwingListens() {
    var list = new LockableListModel<String>();
    var listener = new RecordingListener();
    var model = SwingListModel.of(list);

    model.addListDataListener(listener);
    model.removeListDataListener(listener);
    list.add("a");

    assertThat(listener.events, empty());
    assertThat(model.getListDataListeners().length, is(0));
  }

  @Test
  void editsTheListLikeAComboBoxModel() {
    var list = new LockableListModel<String>();
    var model = SwingListModel.of(list);

    model.addElement("a");
    model.insertElementAt("b", 0);
    model.removeElement("a");

    assertThat(list, contains("b"));
  }
}
