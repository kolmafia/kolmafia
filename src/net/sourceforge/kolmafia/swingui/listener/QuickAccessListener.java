package net.sourceforge.kolmafia.swingui.listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRootPane;
import javax.swing.MenuElement;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class QuickAccessListener implements ActionListener {

  public void actionPerformed(ActionEvent e) {
    JRootPane rootPane = (JRootPane) e.getSource();

    LockableListModel<Object> quickAccessItems = new LockableListModel<>();

    addMenuElement(quickAccessItems, rootPane.getJMenuBar());

    Collections.sort(quickAccessItems, Comparator.comparing(Object::toString));

    Object selectedItem =
        InputFieldUtilities.input("Where would you like to go?", quickAccessItems);

    if (selectedItem == null) {
      return;
    }

    if (selectedItem instanceof JMenuItem) {
      JMenuItem menuItem = (JMenuItem) selectedItem;

      for (ActionListener actionListener : menuItem.getActionListeners()) {

        actionListener.actionPerformed(new ActionEvent(menuItem, e.getID(), e.getActionCommand()));
      }
    }
  }

  protected void addMenuElement(
      LockableListModel<Object> quickAccessItems, MenuElement menuElement) {

    if ((menuElement instanceof JMenuItem) && !(menuElement instanceof JMenu)) {
      JMenuItem menuItem = (JMenuItem) menuElement;
      if (menuItem.isEnabled()) {
        quickAccessItems.add(menuItem);
      }
    }

    for (MenuElement subMenuElement : menuElement.getSubElements()) {
      addMenuElement(quickAccessItems, subMenuElement);
    }
  }
}
