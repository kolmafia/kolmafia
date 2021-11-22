package net.sourceforge.kolmafia.swingui.menu;

import darrylbu.util.MenuScroller;
import java.util.ArrayList;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JSeparator;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import net.java.dev.spellcast.utilities.LockableListModel;

public abstract class MenuItemList<E> extends JMenu implements ListDataListener {
  private final int headerCount;
  private ArrayList<E> dataValues;
  private LockableListModel<E> model;

  public MenuItemList(final String title, final LockableListModel<E> model) {
    super(title);

    this.model = model;
    this.dataValues = new ArrayList<>();

    // Add the headers to the list of items which
    // need to be added.

    JComponent[] headers = this.getHeaders();

    for (int i = 0; i < headers.length; ++i) {
      this.add(headers[i]);
    }

    // Add a separator between the headers and the
    // elements displayed in the list.  Also go
    // ahead and initialize the header count.

    if (headers.length == 0) {
      this.headerCount = 0;
    } else {
      this.add(new JSeparator());
      this.headerCount = headers.length + 1;
    }

    MenuScroller.setScrollerFor(this, 25, 150, headerCount, 0);

    // Now, add everything that's contained inside of
    // the current list.

    for (int i = 0; i < model.size(); ++i) {
      this.dataValues.add(model.get(i));
      this.add(this.constructMenuItem(model.get(i)));
    }

    // Add this as a listener to the list so that the menu gets
    // updated whenever the list updates.

    model.addListDataListener(this);
  }

  public void dispose() {
    if (this.dataValues != null) {
      this.dataValues.clear();
      this.dataValues = null;
    }

    if (this.model != null) {
      this.model.removeListDataListener(this);
      this.model = null;
    }
  }

  public abstract JComponent[] getHeaders();

  public abstract JComponent constructMenuItem(Object o);

  /**
   * Called whenever contents have been added to the original list; a function required by every
   * <code>ListDataListener</code>.
   *
   * @param e the <code>ListDataEvent</code> that triggered this function call
   */
  public void intervalAdded(final ListDataEvent e) {
    LockableListModel<E> source = (LockableListModel<E>) e.getSource();
    int index0 = e.getIndex0();
    int index1 = e.getIndex1();

    for (int i = index0; i <= index1; ++i) {
      E item = source.get(i);

      this.dataValues.add(i, item);
      this.add(this.constructMenuItem(item), i + this.headerCount);
    }

    this.validate();
  }

  /**
   * Called whenever contents have been removed from the original list; a function required by every
   * <code>ListDataListener</code>.
   *
   * @param e the <code>ListDataEvent</code> that triggered this function call
   */
  public void intervalRemoved(final ListDataEvent e) {
    int index0 = e.getIndex0();
    int index1 = e.getIndex1();

    for (int i = index1; i >= index0; --i) {
      this.dataValues.remove(i);
      this.remove(i + this.headerCount);
    }

    this.validate();
  }

  /**
   * Called whenever contents in the original list have changed; a function required by every <code>
   * ListDataListener</code>.
   *
   * @param e the <code>ListDataEvent</code> that triggered this function call
   */
  public void contentsChanged(final ListDataEvent e) {
    for (int i = 0; i < this.dataValues.size(); ++i) {
      this.remove(this.headerCount);
    }

    this.dataValues.clear();
    LockableListModel<E> source = (LockableListModel<E>) e.getSource();

    for (int i = 0; i < source.size(); ++i) {
      this.dataValues.add(i, source.get(i));
      this.add(this.constructMenuItem(source.get(i)), i + this.headerCount);
    }
  }
}
