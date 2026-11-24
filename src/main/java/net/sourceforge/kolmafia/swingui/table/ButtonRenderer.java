package net.sourceforge.kolmafia.swingui.table;

import java.awt.Component;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import net.sourceforge.kolmafia.swingui.JComponentUtilities;

public class ButtonRenderer implements TableCellRenderer {
  @Override
  public Component getTableCellRendererComponent(
      final JTable table,
      final Object value,
      final boolean isSelected,
      final boolean hasFocus,
      final int row,
      final int column) {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    panel.setOpaque(false);

    JComponentUtilities.setComponentSize((JButton) value, 20, 20);
    panel.add((JButton) value);

    return panel;
  }
}
