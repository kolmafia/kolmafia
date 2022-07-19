package net.sourceforge.kolmafia.swingui.menu;

import javax.swing.JMenuItem;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

public class ThreadedMenuItem extends JMenuItem {
  public ThreadedMenuItem(final String label, ThreadedListener... actions) {
    super(label);

    for (ThreadedListener action : actions) {
      this.addActionListener(action);
    }
  }

  @Override
  public String toString() {
    return this.getText();
  }
}
