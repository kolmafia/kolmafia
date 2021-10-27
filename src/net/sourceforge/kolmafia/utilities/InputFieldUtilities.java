package net.sourceforge.kolmafia.utilities;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.swingui.GenericFrame;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;

public class InputFieldUtilities {
  private static GenericFrame activeWindow = null;

  public static void setActiveWindow(GenericFrame activeWindow) {
    InputFieldUtilities.activeWindow = activeWindow;
  }

  public static final void alert(final String message) {
    if (StaticEntity.isHeadless()) {
      RequestLogger.printLine(message);
      KoLmafiaCLI.DEFAULT_SHELL.getNextLine("Press enter to continue...");

      return;
    }

    JOptionPane.showMessageDialog(
        InputFieldUtilities.activeWindow, StringUtilities.basicTextWrap(message));
  }

  public static final boolean confirm(final String message) {
    if (StaticEntity.isHeadless()) {
      RequestLogger.printLine(message);
      RequestLogger.printLine("(Y/N, leave blank to choose N)");

      String reply = KoLmafiaCLI.DEFAULT_SHELL.getNextLine(" > ");

      return reply.equalsIgnoreCase("y");
    }

    return JOptionPane.YES_OPTION
        == JOptionPane.showConfirmDialog(
            InputFieldUtilities.activeWindow,
            StringUtilities.basicTextWrap(message),
            "",
            JOptionPane.YES_NO_OPTION);
  }

  public static final String input(final String message) {
    if (StaticEntity.isHeadless()) {
      RequestLogger.printLine(message);

      String reply = KoLmafiaCLI.DEFAULT_SHELL.getNextLine(" > ");

      return reply;
    }

    return JOptionPane.showInputDialog(
        InputFieldUtilities.activeWindow, StringUtilities.basicTextWrap(message));
  }

  public static final String input(final String message, final String initial) {
    if (StaticEntity.isHeadless()) {
      RequestLogger.printLine(message);
      RequestLogger.printLine("(leave blank to use " + initial + ")");

      String reply =
          KoLmafiaCLI.DEFAULT_SHELL.getNextLine(
              message + "\n(Leave blank to use " + initial + ")\n > ");

      if (reply.equals("")) {
        reply = initial;
      }

      return reply;
    }

    return JOptionPane.showInputDialog(
        InputFieldUtilities.activeWindow, StringUtilities.basicTextWrap(message), initial);
  }

  public static final <T> T input(
      final String message, final LockableListModel<T> inputs, final T initial) {
    if (StaticEntity.isHeadless()) {
      int initialIndex = 0;
      RequestLogger.printLine(message);

      for (int i = 0; i < inputs.size(); ++i) {
        T o = inputs.get(i);
        RequestLogger.printLine("  " + (i + 1) + ": " + o);

        if (initial != null && initial.equals(o)) {
          initialIndex = i;
        }
      }

      RequestLogger.printLine(
          "(Enter a number from 1 to "
              + inputs.size()
              + ", leave blank to use "
              + (initialIndex + 1)
              + ")");

      String reply = KoLmafiaCLI.DEFAULT_SHELL.getNextLine(" > ");

      if (reply.equals("")) {
        return initial;
      }

      int selectedIndex = StringUtilities.parseInt(reply) - 1;

      if (selectedIndex > -1 && selectedIndex < inputs.size()) {
        return inputs.get(selectedIndex);
      }

      return null;
    }

    JList<T> selector = new JList<>(inputs);

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(new AutoFilterTextField(selector, initial), BorderLayout.NORTH);
    panel.add(new GenericScrollPane(selector), BorderLayout.CENTER);

    int option =
        JOptionPane.showConfirmDialog(
            InputFieldUtilities.activeWindow,
            panel,
            StringUtilities.basicTextWrap(message),
            JOptionPane.OK_CANCEL_OPTION);

    return option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION
        ? null
        : selector.getSelectedValue();
  }

  public static final <T> T input(final String message, final LockableListModel<T> inputs) {
    return InputFieldUtilities.input(message, inputs, null);
  }

  public static final <T> T input(final String message, final T[] inputs) {
    if (inputs == null || inputs.length == 0) {
      return null;
    }

    return InputFieldUtilities.input(message, inputs, null);
  }

  public static final <T> T input(final String message, final T[] inputs, final T initial) {
    if (inputs == null || inputs.length == 0) {
      return null;
    }

    // Keep simple input dialog (no AutoFilterTextField) if there
    // are only a few input choices: booleans, stats, classes, ...
    if (inputs.length <= 12) {
      return (T)
          JOptionPane.showInputDialog(
              InputFieldUtilities.activeWindow,
              StringUtilities.basicTextWrap(message),
              "",
              JOptionPane.INFORMATION_MESSAGE,
              null,
              inputs,
              initial);
    }

    return InputFieldUtilities.input(
        message, new LockableListModel<>(Arrays.asList(inputs)), initial);
  }

  public static final <T> List<T> multiple(
      final String message, final LockableListModel<T> inputs) {
    return InputFieldUtilities.multiple(message, inputs, null);
  }

  public static final <T> List<T> multiple(
      final String message, final LockableListModel<T> inputs, final ListElementFilter filter) {
    if (StaticEntity.isHeadless()) {
      RequestLogger.printLine(message);
      List<T> visibleInputs = new ArrayList<>();

      for (int i = 0; i < inputs.size(); ++i) {
        T o = inputs.get(i);

        if (filter.isVisible(o)) {
          visibleInputs.add(o);
          RequestLogger.printLine("  " + (visibleInputs.size()) + ": " + o);
        }
      }

      RequestLogger.printLine(
          "(Enter a comma-delimited list of values from 1 to " + visibleInputs.size() + ")");

      String reply = KoLmafiaCLI.DEFAULT_SHELL.getNextLine(" > ");

      String[] replyList = reply.split("\\s*,\\s*");

      Set<T> selectedValues = new HashSet<>();

      for (int i = 0; i < replyList.length; ++i) {
        int selectedIndex = StringUtilities.parseInt(replyList[i]) - 1;

        if (selectedIndex > -1 && selectedIndex < visibleInputs.size()) {
          selectedValues.add(visibleInputs.get(i));
        }
      }

      return new ArrayList<>(selectedValues);
    }

    JList<T> selector = new JList<>(inputs);
    selector.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(
        filter == null
            ? new AutoFilterTextField(selector)
            : new AutoFilterTextField(selector) {
              @Override
              public boolean isVisible(Object o) {
                return filter.isVisible(o) && super.isVisible(o);
              }
            },
        BorderLayout.NORTH);
    inputs.updateFilter(false);
    panel.add(new GenericScrollPane(selector), BorderLayout.CENTER);

    int option =
        JOptionPane.showConfirmDialog(
            InputFieldUtilities.activeWindow,
            panel,
            StringUtilities.basicTextWrap(message),
            JOptionPane.OK_CANCEL_OPTION);

    return option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION
        ? new ArrayList<T>()
        : selector.getSelectedValuesList();
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
      return ((Integer) field.getValue()).intValue();
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
      return IntegerPool.get(1);
    }

    String currentValue =
        InputFieldUtilities.input(message, KoLConstants.COMMA_FORMAT.format(defaultValue));

    if (currentValue == null) {
      return null;
    }

    if (currentValue.equals("*")) {
      return IntegerPool.get(maximumValue);
    }

    int desiredValue = StringUtilities.parseIntInternal2(currentValue);
    if (desiredValue < 0) {
      return IntegerPool.get(maximumValue - desiredValue);
    }

    return IntegerPool.get(Math.min(desiredValue, maximumValue));
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
