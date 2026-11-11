package net.sourceforge.kolmafia.utilities;

import java.awt.Component;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import javax.swing.JFileChooser;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.session.UserPrompt;
import net.sourceforge.kolmafia.swingui.GenericFrame;
import net.sourceforge.kolmafia.swingui.UserPromptDialog;

public class InputFieldUtilities {
  private InputFieldUtilities() {}

  public static void setActiveWindow(GenericFrame activeWindow) {
    UserPromptDialog.setActiveWindow(activeWindow);
  }

  public static final void alert(final String message) {
    UserPrompt.ask(new UserPrompt.Alert(message));
  }

  public static final boolean confirm(final String message) {
    return UserPrompt.ask(new UserPrompt.Confirm(message));
  }

  public static final boolean confirm(
      final String message, final long timeoutMillis, final boolean defaultValue) {
    return UserPrompt.ask(new UserPrompt.Confirm(message), timeoutMillis, defaultValue);
  }

  public static final String input(final String message) {
    return UserPrompt.ask(new UserPrompt.Text(message, null));
  }

  public static final String input(
      final String message, final long timeoutMillis, final String defaultValue) {
    return UserPrompt.ask(new UserPrompt.Text(message, null), timeoutMillis, defaultValue);
  }

  public static final String input(final String message, final String initial) {
    return UserPrompt.ask(new UserPrompt.Text(message, initial));
  }

  public static final <T> T input(
      final String message, final LockableListModel<T> inputs, final T initial) {
    return input(message, inputs, null, initial);
  }

  public static final <T> T input(
      final String message,
      final LockableListModel<T> inputs,
      BiFunction<Object, Boolean, String> toHTMLFunction,
      final T initial) {
    return UserPrompt.ask(new UserPrompt.Select<>(message, inputs, initial, toHTMLFunction));
  }

  public static final <T> T input(final String message, final LockableListModel<T> inputs) {
    return InputFieldUtilities.input(message, inputs, null);
  }

  public static final <T> T input(final String message, final T[] inputs) {
    return InputFieldUtilities.input(message, inputs, null);
  }

  public static final <T> T input(final String message, final T[] inputs, final T initial) {
    if (inputs == null || inputs.length == 0) {
      return null;
    }

    return UserPrompt.ask(new UserPrompt.Select<>(message, Arrays.asList(inputs), initial, null));
  }

  public static final <T> List<T> multiple(
      final String message, final LockableListModel<T> inputs) {
    return InputFieldUtilities.multiple(message, inputs, null);
  }

  public static final <T> List<T> multiple(
      final String message, final LockableListModel<T> inputs, final ListElementFilter filter) {
    return UserPrompt.ask(new UserPrompt.Multiple<>(message, inputs, filter));
  }

  /**
   * Utility method which retrieves an integer value from the given field. In the event that the
   * field does not contain an integer value, the number "0" is returned instead.
   */
  public static final int getValue(final JTextField field) {
    return InputFieldUtilities.getValue(field, 0);
  }

  public static final int getValue(final JSpinner field) {
    return InputFieldUtilities.getValue(field, 0);
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

  public static final Integer getQuantity(final String title, final int maximumValue) {
    return InputFieldUtilities.getQuantity(title, maximumValue, maximumValue);
  }

  public static final Integer getQuantity(
      final String message, final int maximumValue, int defaultValue) {
    // Check parameters; avoid programmer error.
    if (defaultValue > maximumValue) {
      defaultValue = maximumValue;
    }

    if (maximumValue == 1 && maximumValue == defaultValue) {
      return 1;
    }

    String currentValue =
        InputFieldUtilities.input(message, KoLConstants.COMMA_FORMAT.format(defaultValue));

    if (currentValue == null) {
      return null;
    }

    if (currentValue.equals("*")) {
      return maximumValue;
    }

    int desiredValue = StringUtilities.parseIntInternal2(currentValue);
    if (desiredValue < 0) {
      return maximumValue - desiredValue;
    }

    return Math.min(desiredValue, maximumValue);
  }

  public static boolean finalizeTable(final JTable table) {
    if (table.isEditing()) {
      int row = table.getEditingRow();
      int col = table.getEditingColumn();
      table.getCellEditor(row, col).stopCellEditing();

      if (table.isEditing()) {
        alert(
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
