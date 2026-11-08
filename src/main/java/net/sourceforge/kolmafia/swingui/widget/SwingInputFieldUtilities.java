package net.sourceforge.kolmafia.swingui.widget;

import java.awt.Component;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import net.sourceforge.kolmafia.swingui.GenericFrame;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

/** Helpers for reading Swing input fields and choosing files. */
public class SwingInputFieldUtilities {
  private static GenericFrame activeWindow = null;

  private SwingInputFieldUtilities() {}

  public static void setActiveWindow(GenericFrame activeWindow) {
    SwingInputFieldUtilities.activeWindow = activeWindow;
  }

  public static GenericFrame getActiveWindow() {
    return SwingInputFieldUtilities.activeWindow;
  }

  /**
   * Utility method which retrieves an integer value from the given field. In the event that the
   * field does not contain an integer value, the number "0" is returned instead.
   */
  public static final int getValue(final JTextField field) {
    return SwingInputFieldUtilities.getValue(field, 0);
  }

  public static final int getValue(final JSpinner field) {
    return SwingInputFieldUtilities.getValue(field, 0);
  }

  /**
   * Utility method which retrieves an integer value from the given field. In the event that the
   * field does not contain an integer value, the default value provided will be returned instead.
   */
  public static final int getValue(final JTextField field, final int defaultValue) {
    String currentValue = field.getText();

    if (currentValue == null || currentValue.length() == 0 || currentValue.equals("*")) {
      return defaultValue;
    }

    int result = StringUtilities.parseIntInternal2(currentValue);
    return result == 0 ? defaultValue : result;
  }

  public static final int getValue(final JSpinner field, final int defaultValue) {
    if (field.getValue() instanceof Integer) {
      return (Integer) field.getValue();
    }

    return defaultValue;
  }

  public static boolean finalizeTable(final JTable table) {
    if (table.isEditing()) {
      int row = table.getEditingRow();
      int col = table.getEditingColumn();
      table.getCellEditor(row, col).stopCellEditing();

      if (table.isEditing()) {
        InputFieldUtilities.alert(
            "One or more fields contain invalid values. (Note: they are currently outlined in red)");
        return false;
      }
    }

    return true;
  }

  public static void cancelTableEditing(final JTable table) {
    if (table.isEditing()) {
      int row = table.getEditingRow();
      int col = table.getEditingColumn();
      table.getCellEditor(row, col).cancelCellEditing();
    }
  }

  public static File chooseInputFile(final File path, Component parent) {
    JFileChooser chooser = new JFileChooser(path);
    if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
      return null;
    }

    return chooser.getSelectedFile();
  }

  public static File chooseInputFile(final String path, Component parent) {
    JFileChooser chooser = new JFileChooser(path);
    if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
      return null;
    }

    return chooser.getSelectedFile();
  }

  public static File chooseOutputFile(final File path, Component parent) {
    JFileChooser chooser = new JFileChooser(path);
    if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
      return null;
    }

    return chooser.getSelectedFile();
  }

  public static File chooseOutputFile(final String path, Component parent) {
    JFileChooser chooser = new JFileChooser(path);
    if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
      return null;
    }

    return chooser.getSelectedFile();
  }
}
