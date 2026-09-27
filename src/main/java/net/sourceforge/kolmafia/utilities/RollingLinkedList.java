package net.sourceforge.kolmafia.utilities;

import java.util.Collection;
import java.util.LinkedList;

public class RollingLinkedList<E> extends LinkedList<E> {
  private static final long serialVersionUID = 102075105118L;
  private final int limit;

  public RollingLinkedList(int limit) {
    this.limit = limit;
  }

  @Override
  public boolean add(E o) {
    if (size() == this.limit) {
      this.removeFirst();
    }
    super.addLast(o);
    return true;
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    for (E val : c) this.add(val);
    // The list will always be changed.
    return true;
  }

  // This is essentially an add function with the addition that it will return an item that was
  // deleted to
  // make room.
  public E update(E o) {
    E returnValue = null;
    this.remove(o);
    if (size() == this.limit) {
      returnValue = this.removeFirst();
    }
    super.addLast(o);
    return returnValue;
  }
}
