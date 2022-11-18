package net.sourceforge.kolmafia.swingui.panel;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.swingui.listener.PopupListener;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class PreferenceWatcherTable extends JTable {
  public PreferenceWatcherTable() {
    super(new PreferenceTableModel());
    this.getColumnModel().getColumn(1).setCellRenderer(new PreferenceValueCellRenderer());
    this.createPopupMenu();
    this.setFillsViewportHeight(true);
    this.setCellSelectionEnabled(false);
    this.setRowSelectionAllowed(true);
  }

  public PreferenceTableModel getModel() {
    return (PreferenceTableModel) super.getModel();
  }

  private void removePreference() {
    var row = this.getSelectedRow();
    if (row < 0) return;
    this.getModel().removePreference(row);
  }

  private void createPopupMenu() {
    var popup = new JPopupMenu();

    JMenuItem menuItem;

    menuItem = new JMenuItem("Add new watcher");
    menuItem.addActionListener(
        (e) -> {
          var value = InputFieldUtilities.input("Preference to watch");
          if (value != null) {
            this.getModel().addPreference(value);
          }
        });
    popup.add(menuItem);

    menuItem = new JMenuItem("Remove selected watcher");
    menuItem.addActionListener((e) -> this.removePreference());
    popup.add(menuItem);

    var listener = new PopupListener(popup);
    this.addMouseListener(listener);
  }

  private static class PreferenceValueCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
      var cell =
          (JLabel)
              super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

      if (value == null) {
        cell.setEnabled(false);
        cell.setText("(none)");
      } else {
        cell.setEnabled(true);
      }

      return cell;
    }
  }

  public static class PreferenceTableModel extends AbstractTableModel implements Listener {
    private final List<WatchedPreference> preferences = new ArrayList<>();
    private final Map<String, Integer> prefToIndex = new HashMap<>();

    PreferenceTableModel() {
      this.update();
      PreferenceListenerRegistry.registerPreferenceListener("watchedPreferences", this);
    }

    @Override
    public String getColumnName(final int columnIndex) {
      return switch (columnIndex) {
        case 0 -> "Preference";
        case 1 -> "Value";
        default -> null;
      };
    }

    @Override
    public int getRowCount() {
      return this.preferences.size();
    }

    @Override
    public int getColumnCount() {
      return 2;
    }

    @Override
    public String getValueAt(int rowIndex, int columnIndex) {
      if (rowIndex >= preferences.size()) return null;
      var pref = preferences.get(rowIndex);
      return switch (columnIndex) {
        case 0 -> pref.getPreference();
        case 1 -> pref.getValue();
        default -> null;
      };
    }

    private String[] getWatchedPreferences() {
      String preferencesString = Preferences.getString("watchedPreferences");
      if (preferencesString.isEmpty()) return new String[] {};
      return preferencesString.split(",");
    }

    @Override
    public void update() {
      this.preferences.clear();
      this.prefToIndex.clear();

      for (var pref : getWatchedPreferences()) {
        var watchedPref = new WatchedPreference(this, pref);
        this.preferences.add(watchedPref);
        this.prefToIndex.put(pref, this.preferences.size());

        // Perform initial update
        watchedPref.update();
      }

      this.fireTableDataChanged();
    }

    public void addPreference(final String pref) {
      var current = Preferences.getString("watchedPreferences");
      Preferences.setString("watchedPreferences", (current.isBlank() ? "" : current + ",") + pref);
    }

    public void removePreference(final int rowIndex) {
      var pref = preferences.get(rowIndex);

      Preferences.setString(
          "watchedPreferences",
          Arrays.stream(getWatchedPreferences())
              .filter(p -> !p.equals(pref.getPreference()))
              .collect(Collectors.joining(",")));
    }

    public void firePreferenceChanged(final String preference) {
      var index = prefToIndex.get(preference);
      if (index != null) {
        this.fireTableCellUpdated(index, 1);
      }
    }
  }

  private static class WatchedPreference implements Listener {
    private final PreferenceTableModel model;
    private final String preference;
    private String value = null;

    public WatchedPreference(final PreferenceTableModel model, final String preference) {
      this.model = model;
      this.preference = preference;
      PreferenceListenerRegistry.registerPreferenceListener(preference, this);
    }

    public String getPreference() {
      return preference;
    }

    public String getValue() {
      return value;
    }

    @Override
    public void update() {
      this.value =
          Preferences.propertyExists(preference) ? Preferences.getString(preference) : null;
      this.model.firePreferenceChanged(preference);
    }
  }
}
