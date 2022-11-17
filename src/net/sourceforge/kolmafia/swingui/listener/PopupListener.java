package net.sourceforge.kolmafia.swingui.listener;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JTable;

public class PopupListener extends MouseAdapter {
  JPopupMenu popup;

  public PopupListener(JPopupMenu popupMenu) {
    popup = popupMenu;
  }

  public void mousePressed(MouseEvent e) {
    maybeShowPopup(e);
  }

  public void mouseReleased(MouseEvent e) {
    maybeShowPopup(e);
  }

  private void maybeShowPopup(MouseEvent e) {
    if (e.isPopupTrigger()) {
      var source = (JComponent) e.getSource();

      var point = e.getPoint();

      if (source instanceof JList<?> list) {
        int index = list.locationToIndex(point);
        if (!list.isSelectedIndex(index)) {
          list.clearSelection();
          list.addSelectionInterval(index, index);
        }
      } else if (source instanceof JTable table) {
        int row = table.rowAtPoint(point);
        if (!table.isRowSelected(row)) {
          table.clearSelection();
          table.addRowSelectionInterval(row, row);
        }
      } else {
        return;
      }

      popup.show(e.getComponent(), e.getX(), e.getY());
    }
  }
}
