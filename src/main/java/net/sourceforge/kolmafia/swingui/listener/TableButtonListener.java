package net.sourceforge.kolmafia.swingui.listener;

import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumnModel;

/** Utility class used to forward events to JButtons enclosed inside of a JTable object. */
public class TableButtonListener extends ThreadedListener {
  private final JTable table;

  public TableButtonListener(final JTable table) {
    this.table = table;
  }

  @Override
  protected void execute() {
    TableColumnModel columnModel = this.table.getColumnModel();

    int row = getMousePositionY() / this.table.getRowHeight();
    int column = columnModel.getColumnIndexAtX(getMousePositionX());

    if (row >= 0
        && row < this.table.getRowCount()
        && column >= 0
        && column < this.table.getColumnCount()) {
      Object value = this.table.getValueAt(row, column);

      if (value instanceof JButton) {
        MouseEvent event =
            SwingUtilities.convertMouseEvent(this.table, getMouseEvent(), (JButton) value);
        ((JButton) value).dispatchEvent(event);
        this.table.repaint();
      }
    }
  }
}
