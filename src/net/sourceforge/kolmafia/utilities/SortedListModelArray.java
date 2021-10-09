package net.sourceforge.kolmafia.utilities;

import java.util.ArrayList;
import net.java.dev.spellcast.utilities.SortedListModel;

/**
 * Internal class which functions exactly an array of sorted lists, except it uses "sets" and "gets"
 * like a list. This could be done with generics (Java 1.5) but is done like this so that we get
 * backwards compatibility.
 */
public class SortedListModelArray<E> {
  private final ArrayList<SortedListModel<E>> internalList = new ArrayList<SortedListModel<E>>();

  public SortedListModel<E> get(final int index) {
    if (index < 0) {
      return null;
    }

    while (index >= this.internalList.size()) {
      this.internalList.add(null);
    }

    return this.internalList.get(index);
  }

  public void set(final int index, final SortedListModel<E> value) {
    while (index >= this.internalList.size()) {
      this.internalList.add(null);
    }

    this.internalList.set(index, value);
  }
}
