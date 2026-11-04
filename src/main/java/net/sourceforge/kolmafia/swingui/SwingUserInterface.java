package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.InterruptableDialog;
import net.sourceforge.kolmafia.swingui.widget.ListCellRendererFactory;
import net.sourceforge.kolmafia.swingui.widget.SwingInputFieldUtilities;
import net.sourceforge.kolmafia.ui.UserInterface;
import net.sourceforge.kolmafia.utilities.StringUtilities;

/** The desktop user interface: asks with Swing dialogs and updates the Swing frames. */
public class SwingUserInterface implements UserInterface {
  @Override
  public void alert(final String message) {
    JOptionPane.showMessageDialog(
        SwingInputFieldUtilities.getActiveWindow(), StringUtilities.basicTextWrap(message));
  }

  @Override
  public boolean confirm(final String message) {
    return JOptionPane.YES_OPTION
        == JOptionPane.showConfirmDialog(
            SwingInputFieldUtilities.getActiveWindow(),
            StringUtilities.basicTextWrap(message),
            "",
            JOptionPane.YES_NO_OPTION);
  }

  @Override
  public String input(final String message) {
    return JOptionPane.showInputDialog(
        SwingInputFieldUtilities.getActiveWindow(), StringUtilities.basicTextWrap(message));
  }

  @Override
  public String input(final String message, final String initial) {
    return JOptionPane.showInputDialog(
        SwingInputFieldUtilities.getActiveWindow(),
        StringUtilities.basicTextWrap(message),
        initial);
  }

  @Override
  public <T> T input(
      final String message,
      final LockableListModel<T> inputs,
      BiFunction<Object, Boolean, String> toHTMLFunction,
      final T initial) {
    JList<T> selector = new JList<>(inputs);

    if (toHTMLFunction != null) {
      selector.setCellRenderer(ListCellRendererFactory.getDefaultRenderer(toHTMLFunction));
    }

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(new AutoFilterTextField<>(selector, initial, true), BorderLayout.NORTH);
    panel.add(new GenericScrollPane(selector), BorderLayout.CENTER);

    int option =
        JOptionPane.showConfirmDialog(
            SwingInputFieldUtilities.getActiveWindow(),
            panel,
            StringUtilities.basicTextWrap(message),
            JOptionPane.OK_CANCEL_OPTION);

    return option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION
        ? null
        : selector.getSelectedValue();
  }

  @Override
  public <T> T input(final String message, final T[] inputs, final T initial) {
    return (T)
        JOptionPane.showInputDialog(
            SwingInputFieldUtilities.getActiveWindow(),
            StringUtilities.basicTextWrap(message),
            "",
            JOptionPane.INFORMATION_MESSAGE,
            null,
            inputs,
            initial);
  }

  @Override
  public <T> List<T> multiple(
      final String message, final LockableListModel<T> inputs, final ListElementFilter filter) {
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

    int option =
        JOptionPane.showConfirmDialog(
            SwingInputFieldUtilities.getActiveWindow(),
            panel,
            StringUtilities.basicTextWrap(message),
            JOptionPane.OK_CANCEL_OPTION);

    return option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION
        ? new ArrayList<>()
        : selector.getSelectedValuesList();
  }

  @Override
  public boolean isDarkTheme() {
    return KoLmafiaGUI.isDarkTheme();
  }

  @Override
  public void showBalloon(final String message, final boolean onlyShowWhenHidden) {
    SystemTrayFrame.showBalloon(message, onlyShowWhenHidden);
  }

  @Override
  public boolean confirm(final String message, final long timeOut, final boolean defaultValue) {
    return InterruptableDialog.confirm(message, timeOut, defaultValue);
  }

  @Override
  public String input(final String message, final long timeOut, final String defaultValue) {
    return InterruptableDialog.input(message, timeOut, defaultValue);
  }

  @Override
  public boolean isUiThread() {
    return SwingUtilities.isEventDispatchThread();
  }
}
