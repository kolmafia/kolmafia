package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.session.UserPrompt;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.ListCellRendererFactory;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class UserPromptDialog {
  private static GenericFrame activeWindow = null;

  private record Dialog<R>(JOptionPane pane, String title, Supplier<R> answer) {}

  private UserPromptDialog() {}

  public static void setActiveWindow(final GenericFrame activeWindow) {
    UserPromptDialog.activeWindow = activeWindow;
  }

  @SuppressWarnings("unchecked")
  public static <R> void show(final UserPrompt<R> prompt) {
    var dialog =
        (Dialog<R>)
            switch (prompt.request) {
              case UserPrompt.Alert alert -> alert(alert);
              case UserPrompt.Confirm confirm -> confirm(confirm);
              case UserPrompt.Text text -> text(text);
              case UserPrompt.Select<?> select -> select(select);
              case UserPrompt.Multiple<?> multiple -> multiple(multiple);
            };

    var window = dialog.pane().createDialog(UserPromptDialog.activeWindow, dialog.title());
    window.addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowOpened(final WindowEvent e) {
            if (prompt.isDone()) {
              window.dispose();
            }
          }
        });
    prompt.onAnswer(() -> SwingUtilities.invokeLater(window::dispose));
    if (!prompt.isDone()) {
      window.setVisible(true);
    }
    prompt.complete(dialog.answer().get());
  }

  private static Dialog<Void> alert(final UserPrompt.Alert request) {
    var pane = new JOptionPane(wrap(request.message()), JOptionPane.INFORMATION_MESSAGE);
    return new Dialog<>(pane, "", () -> null);
  }

  private static Dialog<Boolean> confirm(final UserPrompt.Confirm request) {
    var pane =
        new JOptionPane(
            wrap(request.message()), JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION);
    return new Dialog<>(
        pane, "", () -> Integer.valueOf(JOptionPane.YES_OPTION).equals(pane.getValue()));
  }

  private static Dialog<String> text(final UserPrompt.Text request) {
    var pane =
        new JOptionPane(
            wrap(request.message()), JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
    pane.setWantsInput(true);
    pane.setInitialSelectionValue(request.initial());
    return new Dialog<>(pane, "", () -> inputValue(pane));
  }

  private static <T> Dialog<T> select(final UserPrompt.Select<T> request) {
    var choices = request.choices();

    // Keep simple input dialog (no AutoFilterTextField) if there
    // are only a few input choices: booleans, stats, classes, ...
    if (choices.size() <= 12 && !(choices instanceof LockableListModel)) {
      var pane =
          new JOptionPane(
              wrap(request.message()),
              JOptionPane.INFORMATION_MESSAGE,
              JOptionPane.OK_CANCEL_OPTION);
      pane.setWantsInput(true);
      pane.setSelectionValues(choices.toArray());
      pane.setInitialSelectionValue(request.initialChoice());
      return new Dialog<>(pane, "", () -> inputValue(pane));
    }

    var model = listModel(choices);
    var selector = new JList<>(model);
    if (request.renderer() != null) {
      selector.setCellRenderer(ListCellRendererFactory.getDefaultRenderer(request.renderer()));
    }

    var panel = new JPanel(new BorderLayout());
    panel.add(
        new AutoFilterTextField<>(selector, request.initialChoice(), true), BorderLayout.NORTH);
    panel.add(new GenericScrollPane(selector), BorderLayout.CENTER);

    var pane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
    return new Dialog<>(
        pane, wrap(request.message()), () -> ok(pane) ? selector.getSelectedValue() : null);
  }

  private static <T> Dialog<List<T>> multiple(final UserPrompt.Multiple<T> request) {
    var model = listModel(request.choices());
    var filter = request.filter();
    var selector = new JList<>(model);
    selector.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    var panel = new JPanel(new BorderLayout());
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
    model.updateFilter(false);
    panel.add(new GenericScrollPane(selector), BorderLayout.CENTER);

    var pane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
    return new Dialog<>(
        pane,
        wrap(request.message()),
        () -> ok(pane) ? selector.getSelectedValuesList() : new ArrayList<>());
  }

  private static <T> LockableListModel<T> listModel(final List<T> choices) {
    return choices instanceof LockableListModel<T> model ? model : new LockableListModel<>(choices);
  }

  private static boolean ok(final JOptionPane pane) {
    return Integer.valueOf(JOptionPane.OK_OPTION).equals(pane.getValue());
  }

  @SuppressWarnings("unchecked")
  private static <T> T inputValue(final JOptionPane pane) {
    var value = pane.getInputValue();
    return ok(pane) && value != JOptionPane.UNINITIALIZED_VALUE ? (T) value : null;
  }

  private static String wrap(final String message) {
    return StringUtilities.basicTextWrap(message);
  }
}
