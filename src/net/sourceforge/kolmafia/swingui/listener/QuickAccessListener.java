package net.sourceforge.kolmafia.swingui.listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRootPane;
import javax.swing.MenuElement;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.swingui.menu.WindowMenu;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class QuickAccessListener implements ActionListener {

  public void actionPerformed(ActionEvent e) {
    JRootPane rootPane = (JRootPane) e.getSource();

    Map<String, Object> quickAccessItems = new TreeMap<String, Object>();

    addMenuElement(quickAccessItems, rootPane.getJMenuBar());

    LockableListModel<Object> quickAccessList = new LockableListModel<>(quickAccessItems.values());

    Object selectedItem = InputFieldUtilities.input("Where would you like to go?", quickAccessList);

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

  protected void addMenuElement(Map<String, Object> quickAccessItems, MenuElement menuElement) {
    if (menuElement instanceof WindowMenu) {
      return;
    }

    if ((menuElement instanceof JMenuItem) && !(menuElement instanceof JMenu)) {
      JMenuItem menuItem = (JMenuItem) menuElement;
      if (menuItem.isEnabled()) {
        quickAccessItems.put(menuItem.getText(), menuItem);
      }
    }

    for (MenuElement subMenuElement : menuElement.getSubElements()) {
      addMenuElement(quickAccessItems, subMenuElement);
    }
  }
}
