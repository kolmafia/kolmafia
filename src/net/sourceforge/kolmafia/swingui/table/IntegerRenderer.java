package net.sourceforge.kolmafia.swingui.table;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class IntegerRenderer extends DefaultTableCellRenderer {
  @Override
  public Component getTableCellRendererComponent(
      final JTable table,
      final Object value,
      final boolean isSelected,
      final boolean hasFocus,
      final int row,
      final int column) {
    Component component =
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

    if (!(component instanceof JLabel)) {
      return component;
    }

    JLabel label = (JLabel) component;

    int intValue =
        value instanceof Integer
            ? ((Integer) value).intValue()
            : StringUtilities.parseInt(value.toString());

    String stringValue = KoLConstants.COMMA_FORMAT.format(intValue);
    label.setText(stringValue);

    return label;
  }
}
