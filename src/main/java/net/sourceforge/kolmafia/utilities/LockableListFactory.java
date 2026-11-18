package net.sourceforge.kolmafia.utilities;

import java.util.List;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

public class LockableListFactory {
  private LockableListFactory() {}

  public static <E> List<E> getInstance(Class<E> E) {
    return new LockableListModel<>();
  }

  public static <E extends Comparable<E>> List<E> getSortedInstance(Class<E> E) {
    return new SortedListModel<>();
  }

  public static int getsize(List<?> l) {
    return ((LockableListModel<?>) l).getSize();
  }

  public static void setSelectedItem(List<?> l, Object selection) {
    ((LockableListModel<?>) l).setSelectedItem(selection);
  }

  public static <T> List<T> getMirror(List<T> l) {
    return ((LockableListModel<T>) l).getMirrorImage();
  }

  public static void fireContentsChanged(List<?> l, int index0, int index1) {
    ((LockableListModel<?>) l).fireContentsChanged(l, index0, index1);
  }

  public static <E> E lastElement(List<E> l) {
    return l.isEmpty() ? null : l.get(l.size() - 1);
  }

  public static void sort(List<?> l) {
    ((LockableListModel<?>) l).sort();
  }

  public static <E> E getElementAt(List<E> l, int index) {
    return ((LockableListModel<E>) l).getElementAt(index);
  }
}
