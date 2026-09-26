package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;

public class CardLayoutSelectorPanel extends JPanel {
  private final String indexPreference;

  public final LockableListModel<Object> panelNames = new LockableListModel<>();
  private final ArrayList<Object> originalPanelNames = new ArrayList<>();
  private final JList<Object> panelList = new JList<>(this.panelNames);
  public final ArrayList<JComponent> panels = new ArrayList<>();
  private final CardLayout panelCards = new CardLayout();
  private final JPanel mainPanel = new JPanel(this.panelCards);
  protected ChangeListener changeListener = null;
  private final PanelFilterField filterField;

  public CardLayoutSelectorPanel() {
    this(null);
  }

  public CardLayoutSelectorPanel(final String indexPreference) {
    this(indexPreference, "ABCDEFGHIJKLM");
  }

  public CardLayoutSelectorPanel(final String indexPreference, final String prototype) {
    this(indexPreference, prototype, false);
  }

  public CardLayoutSelectorPanel(
      final String indexPreference, final String prototype, final boolean showFilter) {
    super(new BorderLayout());

    this.indexPreference = indexPreference;

    this.panelList.addListSelectionListener(new CardSwitchListener());
    this.panelList.setPrototypeCellValue(prototype);
    this.panelList.setCellRenderer(new OptionRenderer());

    JPanel listHolder = new JPanel(new BorderLayout());
    listHolder.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 0));

    if (showFilter) {
      this.filterField = new PanelFilterField();
      this.panelNames.setFilter(this.filterField);

      listHolder.add(this.filterField, BorderLayout.NORTH);
    } else {
      this.filterField = null;
    }

    listHolder.add(new GenericScrollPane(this.panelList), BorderLayout.CENTER);

    this.add(listHolder, BorderLayout.WEST);
    this.add(this.mainPanel, BorderLayout.CENTER);
  }

  public void addChangeListener(final ChangeListener changeListener) {
    this.changeListener = changeListener;
  }

  public void setSelectedIndex(final int selectedIndex) {
    this.panelList.setSelectedIndex(selectedIndex);
  }

  public int getSelectedIndex() {
    return this.panelList.getSelectedIndex();
  }

  public void addPanel(final String name, final JComponent panel) {
    this.addPanel(name, panel, false);
  }

  public void addPanel(final String name, JComponent panel, boolean addScrollPane) {
    this.panelNames.add(name);
    this.originalPanelNames.add(name);

    if (addScrollPane) {
      panel =
          new GenericScrollPane(
              panel,
              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      JComponentUtilities.setComponentSize(panel, 500, 400);
    }

    this.panels.add(panel);
    this.mainPanel.add(panel, String.valueOf(this.panelNames.size()));
  }

  public void addSeparator() {
    JPanel separator = new JPanel();
    separator.setOpaque(false);
    separator.setLayout(new BoxLayout(separator, BoxLayout.Y_AXIS));

    separator.add(Box.createVerticalGlue());
    separator.add(new JSeparator());
    this.panelNames.add(separator);
    this.originalPanelNames.add(separator);
    this.panels.add(separator);
  }

  public void addCategory(final String name) {
    JPanel category = new JPanel();
    category.setOpaque(false);
    category.setLayout(new BoxLayout(category, BoxLayout.Y_AXIS));
    category.add(new JLabel(name));
    this.panelNames.add(category);
    this.originalPanelNames.add(category);
    this.panels.add(category);
  }

  public JComponent currentPanel() {
    Object selectedValue = this.panelList.getSelectedValue();
    if (selectedValue == null) {
      return null;
    }
    int originalIndex = this.originalPanelNames.indexOf(selectedValue);
    return originalIndex == -1 ? null : this.panels.get(originalIndex);
  }

  private class CardSwitchListener implements ListSelectionListener {
    @Override
    public void valueChanged(final ListSelectionEvent e) {
      Object selectedValue = CardLayoutSelectorPanel.this.panelList.getSelectedValue();

      if (selectedValue == null || selectedValue instanceof JComponent) {
        return;
      }

      int originalIndex = CardLayoutSelectorPanel.this.originalPanelNames.indexOf(selectedValue);
      if (originalIndex == -1) {
        return;
      }

      if (CardLayoutSelectorPanel.this.indexPreference != null) {
        Preferences.setInteger(CardLayoutSelectorPanel.this.indexPreference, originalIndex);
      }

      CardLayoutSelectorPanel.this.panelCards.show(
          CardLayoutSelectorPanel.this.mainPanel, String.valueOf(originalIndex + 1));

      if (CardLayoutSelectorPanel.this.changeListener != null) {
        CardLayoutSelectorPanel.this.changeListener.stateChanged(
            new ChangeEvent(CardLayoutSelectorPanel.this));
      }
    }
  }

  private static class OptionRenderer extends DefaultListCellRenderer {
    public OptionRenderer() {
      this.setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(
        final JList<?> list,
        final Object value,
        final int index,
        final boolean isSelected,
        final boolean cellHasFocus) {
      return value instanceof JComponent
          ? (Component) value
          : super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    }
  }

  private class PanelFilterField extends AutoHighlightTextField implements ListElementFilter {
    private String filterText = "";

    public PanelFilterField() {
      this.putClientProperty("JTextField.variant", "search");
      this.addKeyListener(
          new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent e) {
              PanelFilterField.this.update();
            }
          });
    }

    private void update() {
      this.filterText = this.getText().toLowerCase().trim();
      CardLayoutSelectorPanel.this.panelNames.updateFilter(false);
    }

    @Override
    public boolean isVisible(final Object element) {
      if (this.filterText.isEmpty()) {
        return true;
      }

      // Categories and separators are JComponents, if this is not just match it normally.
      if (!(element instanceof JComponent component)) {
        return this.matchesFilter(element);
      }

      int index = CardLayoutSelectorPanel.this.originalPanelNames.indexOf(component);
      var names = CardLayoutSelectorPanel.this.originalPanelNames;

      // Separators (have two children) should show if there was a match before it and after the
      // last component
      // and there are any matches after at all
      if (component.getComponentCount() == 2) {
        var matchBefore =
            names.subList(0, index).reversed().stream()
                .takeWhile(prev -> !(prev instanceof JComponent))
                .anyMatch(this::matchesFilter);
        var matchAfter = names.stream().skip(index + 1).anyMatch(this::matchesFilter);
        return matchAfter && matchBefore;
      }

      // Categories (have one child) should show if there are matches after before the next
      // component
      if (component.getComponentCount() == 1) {
        return names.stream()
            .skip(index + 1)
            .takeWhile(next -> !(next instanceof JComponent))
            .anyMatch(this::matchesFilter);
      }

      // Any other count is unknown
      return false;
    }

    private boolean matchesFilter(final Object element) {
      String name = element.toString().toLowerCase();
      // Strip HTML tags for matching
      name = name.replaceAll("<[^>]*>", "");
      return name.contains(this.filterText);
    }
  }
}
