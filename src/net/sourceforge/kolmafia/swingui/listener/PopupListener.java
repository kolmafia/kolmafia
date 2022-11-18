package net.sourceforge.kolmafia.swingui.listener;

import java.awt.Point;
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
        select(list, point);
      } else if (source instanceof JTable table) {
        select(table, point);
      } else {
        return;
      }

      popup.show(e.getComponent(), e.getX(), e.getY());
    }
  }

  private void select(JList<?> list, Point point) {
    int index = list.locationToIndex(point);

    if (index < 0) {
      var size = list.getModel().getSize();
      if (size == 0) return;
      index = size - 1;
    }

    if (!list.isSelectedIndex(index)) {
      list.clearSelection();
      list.addSelectionInterval(index, index);
    }
  }

  private void select(JTable table, Point point) {
    int row = table.rowAtPoint(point);

    if (row < 0) {
      var size = table.getModel().getRowCount();
      if (size == 0) return;
      row = size - 1;
    }

    if (!table.isRowSelected(row)) {
      table.clearSelection();
      table.addRowSelectionInterval(row, row);
    }
  }
}
