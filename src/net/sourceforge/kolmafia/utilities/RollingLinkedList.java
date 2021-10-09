package net.sourceforge.kolmafia.utilities;

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

  public E update(E o) {
    this.remove(o);

    E rv = null;

    if (size() == this.limit) {
      rv = this.removeFirst();
    }

    super.addLast(o);
    return rv;
  }
}
