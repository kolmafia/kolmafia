package net.sourceforge.kolmafia.swingui.widget;

import javax.swing.ListModel;
import javax.swing.MutableComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import net.java.dev.spellcast.utilities.LockableListModel;

/** Shows a {@link LockableListModel} in Swing lists and combo boxes. */
public class SwingListModel<E> implements MutableComboBoxModel<E> {
  private final LockableListModel<E> list;

  private SwingListModel(final LockableListModel<E> list) {
    this.list = list;
  }

  public static <E> SwingListModel<E> of(final LockableListModel<E> list) {
    return list == null ? null : new SwingListModel<>(list);
  }

  /** The list shown by a Swing model, or null if the model does not show one. */
  @SuppressWarnings("unchecked")
  public static <E> LockableListModel<E> listOf(final ListModel<?> model) {
    return model instanceof SwingListModel<?> swingModel
        ? (LockableListModel<E>) swingModel.list
        : null;
  }

  public LockableListModel<E> getList() {
    return this.list;
  }

  // Listeners are kept on the list, so one added through any SwingListModel of a list can be
  // removed through any other.

  @Override
  public void addListDataListener(final ListDataListener l) {
    this.list.addListListener(new Listener(l));
  }

  @Override
  public void removeListDataListener(final ListDataListener l) {
    this.list.removeListListener(new Listener(l));
  }

  private record Listener(ListDataListener listener) implements LockableListModel.ListListener {
    @Override
    public void contentsChanged(final Object source, final int index0, final int index1) {
      this.listener.contentsChanged(
          new ListDataEvent(source, ListDataEvent.CONTENTS_CHANGED, index0, index1));
    }

    @Override
    public void intervalAdded(final Object source, final int index0, final int index1) {
      this.listener.intervalAdded(
          new ListDataEvent(source, ListDataEvent.INTERVAL_ADDED, index0, index1));
    }

    @Override
    public void intervalRemoved(final Object source, final int index0, final int index1) {
      this.listener.intervalRemoved(
          new ListDataEvent(source, ListDataEvent.INTERVAL_REMOVED, index0, index1));
    }
  }

  @Override
  public int getSize() {
    return this.list.getSize();
  }

  @Override
  public E getElementAt(final int index) {
    return this.list.getElementAt(index);
  }

  @Override
  public Object getSelectedItem() {
    return this.list.getSelectedItem();
  }

  @Override
  public void setSelectedItem(final Object anItem) {
    this.list.setSelectedItem(anItem);
  }

  @Override
  public void addElement(final E item) {
    this.list.addElement(item);
  }

  @Override
  public void removeElement(final Object obj) {
    this.list.removeElement(obj);
  }

  @Override
  public void insertElementAt(final E item, final int index) {
    this.list.insertElementAt(item, index);
  }

  @Override
  public void removeElementAt(final int index) {
    this.list.removeElementAt(index);
  }
}
