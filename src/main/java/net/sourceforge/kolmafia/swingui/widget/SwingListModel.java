package net.sourceforge.kolmafia.swingui.widget;

import javax.swing.AbstractListModel;
import javax.swing.MutableComboBoxModel;
import javax.swing.event.ListDataListener;
import net.java.dev.spellcast.utilities.LockableListModel;

/** Shows a {@link LockableListModel} in Swing lists and combo boxes. */
public class SwingListModel<E> extends AbstractListModel<E>
    implements MutableComboBoxModel<E>, LockableListModel.ListListener {
  private final LockableListModel<E> list;

  private SwingListModel(final LockableListModel<E> list) {
    this.list = list;
  }

  public static <E> SwingListModel<E> of(final LockableListModel<E> list) {
    return list == null ? null : new SwingListModel<>(list);
  }

  /** The list shown by a Swing model, or null if the model does not show one. */
  @SuppressWarnings("unchecked")
  public static <E> LockableListModel<E> listOf(final Object model) {
    return model instanceof SwingListModel<?> swingModel
        ? (LockableListModel<E>) swingModel.list
        : null;
  }

  public LockableListModel<E> getList() {
    return this.list;
  }

  @Override
  public void addListDataListener(final ListDataListener l) {
    synchronized (this) {
      if (this.getListDataListeners().length == 0) {
        this.list.addListListener(this);
      }
      super.addListDataListener(l);
    }
  }

  @Override
  public void removeListDataListener(final ListDataListener l) {
    synchronized (this) {
      super.removeListDataListener(l);
      if (this.getListDataListeners().length == 0) {
        this.list.removeListListener(this);
      }
    }
  }

  @Override
  public void contentsChanged(final Object source, final int index0, final int index1) {
    this.fireContentsChanged(source, index0, index1);
  }

  @Override
  public void intervalAdded(final Object source, final int index0, final int index1) {
    this.fireIntervalAdded(source, index0, index1);
  }

  @Override
  public void intervalRemoved(final Object source, final int index0, final int index1) {
    this.fireIntervalRemoved(source, index0, index1);
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
