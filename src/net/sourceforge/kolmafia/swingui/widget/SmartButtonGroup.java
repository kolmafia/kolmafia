package net.sourceforge.kolmafia.swingui.widget;

import java.awt.Container;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;

public class SmartButtonGroup extends ButtonGroup {
  // A version of ButtonGroup that actually does useful things:
  // * Constructor takes a parent container, adding buttons to
  // the group adds them to the container as well.  This generally
  // removes any need for a temp variable to hold the individual
  // buttons as they're being created.
  // * getSelectedIndex() to determine which button (0-based) is
  // selected.  How could that have been missing???

  private final Container parent;
  private ActionListener actionListener = null;

  public SmartButtonGroup(Container parent) {
    this.parent = parent;
  }

  public ActionListener getActionListener() {
    return this.actionListener;
  }

  public void setActionListener(ActionListener listener) {
    this.actionListener = listener;
  }

  @Override
  public void add(AbstractButton b) {
    super.add(b);
    parent.add(b);
    if (actionListener != null) {
      b.addActionListener(actionListener);
    }
  }

  public void setSelectedIndex(int index) {
    int i = 0;
    Enumeration<AbstractButton> e = this.getElements();
    while (e.hasMoreElements()) {
      e.nextElement().setSelected(i == index);
      ++i;
    }
  }

  public int getSelectedIndex() {
    int i = 0;
    Enumeration<AbstractButton> e = this.getElements();
    while (e.hasMoreElements()) {
      if (e.nextElement().isSelected()) {
        return i;
      }
      ++i;
    }
    return -1;
  }
}
