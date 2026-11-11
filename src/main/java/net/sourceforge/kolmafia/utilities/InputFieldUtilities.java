package net.sourceforge.kolmafia.utilities;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.session.UserPrompt;
import net.sourceforge.kolmafia.swingui.GenericFrame;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.ListCellRendererFactory;

public class InputFieldUtilities {
  private static GenericFrame activeWindow = null;

  private InputFieldUtilities() {}

  public static void setActiveWindow(GenericFrame activeWindow) {
    InputFieldUtilities.activeWindow = activeWindow;
  }

  public static final void alert(final String message) {
    if (StaticEntity.isHeadless()) {
      RequestLogger.printLine(message);
      KoLmafiaCLI.DEFAULT_SHELL.getNextLine("Press enter to continue...");

      return;
    }

    var pane =
        new JOptionPane(StringUtilities.basicTextWrap(message), JOptionPane.INFORMATION_MESSAGE);
    showPrompt(pane, "", UserPrompt.open(UserPrompt.Kind.ALERT, message, List.of()), () -> "");
  }

  public static final boolean confirm(final String message) {
    return InputFieldUtilities.confirm(message, 0, false);
  }

  public static final boolean confirm(
      final String message, final long timeoutMillis, final boolean defaultValue) {
    if (StaticEntity.isHeadless()) {
      RequestLogger.printLine(message);
      RequestLogger.printLine("(Y/N, leave blank to choose N)");

      String reply = KoLmafiaCLI.DEFAULT_SHELL.getNextLine(" > ");

      return reply.equalsIgnoreCase("y");
    }

    var pane =
        new JOptionPane(
            StringUtilities.basicTextWrap(message),
            JOptionPane.QUESTION_MESSAGE,
            JOptionPane.YES_NO_OPTION);
    var prompt = UserPrompt.open(UserPrompt.Kind.CONFIRM, message, List.of());
    if (timeoutMillis > 0) {
      prompt.expireAfter(timeoutMillis, String.valueOf(defaultValue));
    }
    var answer =
        showPrompt(
            pane,
            "",
            prompt,
            () -> String.valueOf(Integer.valueOf(JOptionPane.YES_OPTION).equals(pane.getValue())));
    return "true".equals(answer);
  }

  public static final String input(final String message) {
    return InputFieldUtilities.input(message, 0, null);
  }

  public static final String input(
      final String message, final long timeoutMillis, final String defaultValue) {
    if (StaticEntity.isHeadless()) {
      RequestLogger.printLine(message);

      String reply = KoLmafiaCLI.DEFAULT_SHELL.getNextLine(" > ");

      return reply;
    }

    var pane =
        new JOptionPane(
            StringUtilities.basicTextWrap(message),
            JOptionPane.QUESTION_MESSAGE,
            JOptionPane.OK_CANCEL_OPTION);
    pane.setWantsInput(true);
    var prompt = UserPrompt.open(UserPrompt.Kind.TEXT, message, List.of());
    if (timeoutMillis > 0) {
      prompt.expireAfter(timeoutMillis, defaultValue);
    }
    return showPrompt(pane, "", prompt, () -> inputValue(pane));
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

    var pane =
        new JOptionPane(
            StringUtilities.basicTextWrap(message),
            JOptionPane.QUESTION_MESSAGE,
            JOptionPane.OK_CANCEL_OPTION);
    pane.setWantsInput(true);
    pane.setInitialSelectionValue(initial);
    var prompt = UserPrompt.open(UserPrompt.Kind.TEXT, message, List.of(), initial);
    return showPrompt(pane, "", prompt, () -> inputValue(pane));
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

    if (toHTMLFunction != null) {
      selector.setCellRenderer(ListCellRendererFactory.getDefaultRenderer(toHTMLFunction));
    }

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(new AutoFilterTextField<>(selector, initial, true), BorderLayout.NORTH);
    panel.add(new GenericScrollPane(selector), BorderLayout.CENTER);

    var pane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
    return select(
        pane,
        StringUtilities.basicTextWrap(message),
        message,
        inputs,
        () ->
            Integer.valueOf(JOptionPane.OK_OPTION).equals(pane.getValue())
                ? String.valueOf(selector.getSelectedValue())
                : null);
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
      var pane =
          new JOptionPane(
              StringUtilities.basicTextWrap(message),
              JOptionPane.INFORMATION_MESSAGE,
              JOptionPane.OK_CANCEL_OPTION);
      pane.setWantsInput(true);
      pane.setSelectionValues(inputs);
      pane.setInitialSelectionValue(initial);
      return select(pane, "", message, Arrays.asList(inputs), () -> inputValue(pane));
    }

    return InputFieldUtilities.input(
        message, new LockableListModel<>(Arrays.asList(inputs)), initial);
  }

  private static String inputValue(final JOptionPane pane) {
    var value = pane.getInputValue();
    return Integer.valueOf(JOptionPane.OK_OPTION).equals(pane.getValue())
            && value != JOptionPane.UNINITIALIZED_VALUE
        ? String.valueOf(value)
        : null;
  }

  private static <T> T select(
      final JOptionPane pane,
      final String title,
      final String message,
      final List<T> inputs,
      final Supplier<String> swingAnswer) {
    var options = inputs.stream().map(String::valueOf).toList();
    var prompt = UserPrompt.open(UserPrompt.Kind.SELECT, message, options);
    var index = options.indexOf(showPrompt(pane, title, prompt, swingAnswer));
    return index < 0 ? null : inputs.get(index);
  }

  private static String showPrompt(
      final JOptionPane pane,
      final String title,
      final UserPrompt prompt,
      final Supplier<String> swingAnswer) {
    var dialog = pane.createDialog(InputFieldUtilities.activeWindow, title);
    dialog.addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowOpened(final WindowEvent e) {
            if (prompt.isDone()) {
              dialog.dispose();
            }
          }
        });
    prompt.onAnswer(() -> SwingUtilities.invokeLater(dialog::dispose));
    if (!prompt.isDone()) {
      dialog.setVisible(true);
    }
    prompt.complete(swingAnswer.get());
    return prompt.join();
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

      for (T o : inputs) {
        if (filter.isVisible(o)) {
          visibleInputs.add(o);
          RequestLogger.printLine("  " + (visibleInputs.size()) + ": " + o);
        }
      }

      RequestLogger.printLine(
          "(Enter a comma-delimited list of values from 1 to " + visibleInputs.size() + ")");

      String reply = KoLmafiaCLI.DEFAULT_SHELL.getNextLine(" > ");

      String[] replyList = StringUtilities.splitByComma(reply);

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
            ? new AutoFilterTextField<>(selector)
            : new AutoFilterTextField<>(selector) {
              @Override
              public boolean isVisible(Object o) {
                return filter.isVisible(o) && super.isVisible(o);
              }
            },
        BorderLayout.NORTH);
    inputs.updateFilter(false);
    panel.add(new GenericScrollPane(selector), BorderLayout.CENTER);

    var pane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
    var options = inputs.stream().map(String::valueOf).toList();
    var prompt = UserPrompt.open(UserPrompt.Kind.MULTIPLE, message, options);
    var answer =
        showPrompt(
            pane,
            StringUtilities.basicTextWrap(message),
            prompt,
            () ->
                Integer.valueOf(JOptionPane.OK_OPTION).equals(pane.getValue())
                    ? selector.getSelectedValuesList().stream()
                        .map(value -> String.valueOf(options.indexOf(String.valueOf(value))))
                        .collect(Collectors.joining(","))
                    : null);

    var selected = new ArrayList<T>();
    for (var index : StringUtilities.splitByComma(answer == null ? "" : answer)) {
      var i = StringUtilities.parseInt(index);
      if (!index.isEmpty() && i >= 0 && i < inputs.size()) {
        selected.add(inputs.get(i));
      }
    }
    return selected;
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
