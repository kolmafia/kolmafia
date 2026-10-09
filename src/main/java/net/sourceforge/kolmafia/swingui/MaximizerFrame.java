package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.filterType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.maximizer.Boost;
import net.sourceforge.kolmafia.maximizer.EquipScope;
import net.sourceforge.kolmafia.maximizer.Maximizer;
import net.sourceforge.kolmafia.maximizer.MaximizerSpeculation;
import net.sourceforge.kolmafia.maximizer.PriceLevel;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.swingui.menu.MRUListComboBoxes;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.panel.ScrollableFilteredPanel;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;
import net.sourceforge.kolmafia.swingui.widget.SmartButtonGroup;
import net.sourceforge.kolmafia.utilities.ByteBufferUtilities;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MaximizerFrame extends GenericFrame implements ListSelectionListener {
  public static final JComboBox<String> expressionSelect = new JComboBox<>();

  static { // This has to be done before the constructor runs, since the
    // CLI "maximize" command can set the selected item prior to the
    // frame being instantiated.
    expressionSelect.setEditable(true);
    MRUListComboBoxes.updateJComboData(KoLConstants.maximizerMList, expressionSelect);
  }

  private SmartButtonGroup equipmentSelect, mallSelect, budgetSelect, scoreDifferenceSelect;
  private AutoHighlightTextField maxPriceField,
      searchTimeField,
      combinationLimitField,
      scoreDifferencePercentField,
      scoreDifferencePointsField;
  private JCheckBox certificateModeCheckBox;
  private final ShowDescriptionList<Boost> boostList;
  private final EnumSet<filterType> activeFilters;
  private EnumMap<KoLConstants.filterType, JCheckBox> filterButtons;
  private JLabel listTitle = null;

  private static String HELP_STRING;

  static {
    InputStream stream =
        DataUtilities.getInputStream(KoLConstants.DATA_DIRECTORY, "maximizer-help.html", false);
    byte[] bytes = ByteBufferUtilities.read(stream);
    MaximizerFrame.HELP_STRING = new String(bytes, StandardCharsets.UTF_8);
  }

  public MaximizerFrame() {
    super("Modifier Maximizer");

    JPanel wrapperPanel = new JPanel(new BorderLayout());
    wrapperPanel.add(new MaximizerPanel(), BorderLayout.NORTH);

    this.boostList = new ShowDescriptionList<>(Maximizer.boosts, 12);
    this.boostList.addListSelectionListener(this);
    this.activeFilters = EnumSet.noneOf(KoLConstants.filterType.class);

    wrapperPanel.add(new BoostsPanel(this.boostList), BorderLayout.CENTER);

    this.setCenterComponent(wrapperPanel);

    if (Maximizer.eval != null) {
      this.valueChanged(null);
    } else {
      if (Preferences.getInteger("maximizerMRUSize") > 0) {
        MRUListComboBoxes.updateJComboData(KoLConstants.maximizerMList, expressionSelect);
      }
    }
    for (KoLConstants.filterType f : KoLConstants.filterType.values()) {
      if (filterButtons.get(f).isSelected()) {
        activeFilters.add(f);
      }
    }
  }

  @Override
  public JTabbedPane getTabbedPane() {
    return null;
  }

  @Override
  public void valueChanged(final ListSelectionEvent e) {
    double current = Maximizer.eval.getScore(KoLCharacter.getCurrentModifiers());
    boolean failed = Maximizer.eval.failed;
    Object[] items = this.boostList.getSelectedValuesList().toArray();

    StringBuilder buff = new StringBuilder("Current score: ");
    buff.append(KoLConstants.FLOAT_FORMAT.format(current));
    if (failed) {
      buff.append(" (FAILED)");
    }
    buff.append(" \u25CA Predicted: ");
    if (items.length == 0) {
      buff.append("---");
    } else {
      MaximizerSpeculation spec = new MaximizerSpeculation();
      for (Object item : items) {
        if (item instanceof Boost) {
          ((Boost) item).addTo(spec);
        }
      }
      double score = spec.getScore();
      buff.append(KoLConstants.FLOAT_FORMAT.format(score));
      buff.append(" (");
      buff.append(KoLConstants.MODIFIER_FORMAT.format(score - current));
      if (spec.failed) {
        buff.append(", FAILED)");
      } else {
        buff.append(")");
      }
    }
    if (this.listTitle != null) {
      this.listTitle.setText(buff.toString());
    }
    if (Preferences.getInteger("maximizerMRUSize") > 0) {
      MRUListComboBoxes.updateJComboData(KoLConstants.maximizerMList, expressionSelect);
    }
  }

  public void maximize() {
    if (!this.saveSearchSettings()) return;

    Maximizer.maximize(
        EquipScope.byIndex(this.equipmentSelect.getSelectedIndex()),
        InputFieldUtilities.getValue(this.maxPriceField),
        PriceLevel.byIndex(this.mallSelect.getSelectedIndex()),
        Preferences.getBoolean("maximizerIncludeAll"),
        this.activeFilters);

    this.valueChanged(null);
  }

  private boolean saveSearchSettings() {
    long seconds;
    long combinations;
    try {
      seconds = Long.parseLong(this.searchTimeField.getText().trim());
      combinations = Long.parseLong(this.combinationLimitField.getText().trim());
      if (seconds < 0 || combinations < 0) throw new NumberFormatException();
    } catch (NumberFormatException e) {
      InputFieldUtilities.alert("Search budgets must be nonnegative whole numbers.");
      return false;
    }

    boolean certificateMode = this.certificateModeCheckBox.isSelected();
    double percent = 0.0;
    double points = 0.0;
    if (certificateMode) {
      try {
        percent = Double.parseDouble(this.scoreDifferencePercentField.getText().trim());
        points = Double.parseDouble(this.scoreDifferencePointsField.getText().trim());
        if (!Double.isFinite(percent)
            || percent < 0.0
            || !Double.isFinite(points)
            || points < 0.0) {
          throw new NumberFormatException();
        }
      } catch (NumberFormatException e) {
        InputFieldUtilities.alert("Proof gaps must be finite, nonnegative numbers.");
        return false;
      }
    }

    Preferences.setLong("maximizerTimeLimitSeconds", seconds);
    Preferences.setLong("maximizerCombinationLimit", combinations);
    Preferences.setBoolean("maximizerBudgetUsesOr", this.budgetSelect.getSelectedIndex() == 1);
    Preferences.setBoolean("maximizerCertificateMode", certificateMode);
    if (certificateMode) {
      Preferences.setDouble("maximizerMaximumScoreDifferencePercent", percent);
      Preferences.setDouble("maximizerMaximumScoreDifferencePoints", points);
      Preferences.setBoolean(
          "maximizerScoreDifferenceOr", this.scoreDifferenceSelect.getSelectedIndex() == 1);
    }
    return true;
  }

  private class MaximizerPanel extends GenericPanel implements ItemListener, ActionListener {
    public MaximizerPanel() {
      super("update", "help", new Dimension(80, 22), new Dimension(450, 22));
      filterButtons = new EnumMap<>(KoLConstants.filterType.class);

      MaximizerFrame.this.maxPriceField = new AutoHighlightTextField();
      JComponentUtilities.setComponentSize(MaximizerFrame.this.maxPriceField, 80, -1);
      if (Preferences.getInteger("maximizerMaxPrice") > 0) {
        MaximizerFrame.this.maxPriceField.setText(Preferences.getString("maximizerMaxPrice"));
      }

      JPanel equipPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
      MaximizerFrame.this.equipmentSelect = new SmartButtonGroup(equipPanel);
      MaximizerFrame.this.equipmentSelect.add(new JRadioButton("on hand"));
      MaximizerFrame.this.equipmentSelect.add(new JRadioButton("creatable"));
      MaximizerFrame.this.equipmentSelect.add(new PullableRadioButton("pullable/buyable"));

      if (!Preferences.getBoolean("maximizerUseScope")) {
        int maximizerEquipmentLevel = Preferences.getInteger("maximizerEquipmentLevel");
        if (maximizerEquipmentLevel == 0) {
          // no longer supported...
          maximizerEquipmentLevel = 1;
        }
        Preferences.setInteger("maximizerEquipmentScope", maximizerEquipmentLevel - 1);
        Preferences.setBoolean("maximizerUseScope", true);
      }

      MaximizerFrame.this.equipmentSelect.setSelectedIndex(
          Preferences.getInteger("maximizerEquipmentScope"));

      JPanel mallPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
      mallPanel.add(MaximizerFrame.this.maxPriceField);
      MaximizerFrame.this.mallSelect = new SmartButtonGroup(mallPanel);
      MaximizerFrame.this.mallSelect.add(new JRadioButton("don't check"));
      MaximizerFrame.this.mallSelect.add(new JRadioButton("buyable only"));
      MaximizerFrame.this.mallSelect.add(new JRadioButton("all consumables"));
      MaximizerFrame.this.mallSelect.setSelectedIndex(
          Preferences.getInteger("maximizerPriceLevel"));

      JPanel searchModePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
      MaximizerFrame.this.certificateModeCheckBox =
          new JCheckBox(
              "Prove the requested gap or exact optimum",
              Preferences.getBoolean("maximizerCertificateMode"));
      searchModePanel.add(MaximizerFrame.this.certificateModeCheckBox);

      JPanel budgetPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
      budgetPanel.add(new JLabel("Limit: "));
      MaximizerFrame.this.searchTimeField =
          new AutoHighlightTextField(Preferences.getString("maximizerTimeLimitSeconds"));
      JComponentUtilities.setComponentSize(MaximizerFrame.this.searchTimeField, 55, -1);
      budgetPanel.add(MaximizerFrame.this.searchTimeField);
      budgetPanel.add(new JLabel(" seconds "));
      MaximizerFrame.this.budgetSelect = new SmartButtonGroup(budgetPanel);
      MaximizerFrame.this.budgetSelect.add(new JRadioButton("and"));
      MaximizerFrame.this.budgetSelect.add(new JRadioButton("or"));
      MaximizerFrame.this.budgetSelect.setSelectedIndex(
          Preferences.getBoolean("maximizerBudgetUsesOr") ? 1 : 0);
      MaximizerFrame.this.combinationLimitField =
          new AutoHighlightTextField(Preferences.getString("maximizerCombinationLimit"));
      JComponentUtilities.setComponentSize(MaximizerFrame.this.combinationLimitField, 80, -1);
      budgetPanel.add(MaximizerFrame.this.combinationLimitField);
      budgetPanel.add(new JLabel(" combinations"));
      budgetPanel.add(new JLabel(" (0 disables a budget)."));

      JPanel scoreDifferencePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
      scoreDifferencePanel.add(new JLabel("Proof target: optimum within "));
      MaximizerFrame.this.scoreDifferencePercentField =
          new AutoHighlightTextField(
              Preferences.getString("maximizerMaximumScoreDifferencePercent"));
      JComponentUtilities.setComponentSize(MaximizerFrame.this.scoreDifferencePercentField, 50, -1);
      scoreDifferencePanel.add(MaximizerFrame.this.scoreDifferencePercentField);
      scoreDifferencePanel.add(new JLabel("% "));
      MaximizerFrame.this.scoreDifferenceSelect = new SmartButtonGroup(scoreDifferencePanel);
      MaximizerFrame.this.scoreDifferenceSelect.add(new JRadioButton("and"));
      MaximizerFrame.this.scoreDifferenceSelect.add(new JRadioButton("or"));
      MaximizerFrame.this.scoreDifferenceSelect.setSelectedIndex(
          Preferences.getBoolean("maximizerScoreDifferenceOr") ? 1 : 0);
      MaximizerFrame.this.scoreDifferencePointsField =
          new AutoHighlightTextField(
              Preferences.getString("maximizerMaximumScoreDifferencePoints"));
      JComponentUtilities.setComponentSize(MaximizerFrame.this.scoreDifferencePointsField, 50, -1);
      scoreDifferencePanel.add(MaximizerFrame.this.scoreDifferencePointsField);
      scoreDifferencePanel.add(new JLabel(" points"));

      JPanel searchSettingsPanel = new JPanel(new CardLayout());
      searchSettingsPanel.add(budgetPanel, "limits");
      searchSettingsPanel.add(scoreDifferencePanel, "targets");
      CardLayout searchSettingsLayout = (CardLayout) searchSettingsPanel.getLayout();
      MaximizerFrame.this.certificateModeCheckBox.addItemListener(
          e ->
              searchSettingsLayout.show(
                  searchSettingsPanel,
                  e.getStateChange() == ItemEvent.SELECTED ? "targets" : "limits"));
      searchSettingsLayout.show(
          searchSettingsPanel,
          MaximizerFrame.this.certificateModeCheckBox.isSelected() ? "targets" : "limits");

      KoLConstants.filterType[] TopRowFilters =
          new KoLConstants.filterType[] {
            KoLConstants.filterType.EQUIP,
            KoLConstants.filterType.CAST,
            KoLConstants.filterType.WISH,
            KoLConstants.filterType.OTHER,
          };
      // JPanel filterPanel= new JPanel( new FlowLayout( FlowLayout.LEADING, 0, 0 ) );
      JPanel filterCheckboxTopPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
      JPanel filterCheckboxBottomPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));

      JPanel filterPanelTopRow = new JPanel(new BorderLayout());
      JPanel filterPanelBottomRow = new JPanel(new BorderLayout());

      JButton filterAllButton = new JButton("Set All Filters");
      JButton filterNoneButton = new JButton("Clear Filters");
      filterAllButton.addActionListener(this);
      filterNoneButton.addActionListener(this);
      filterAllButton.setPreferredSize(new Dimension(110, 20));
      filterNoneButton.setPreferredSize(new Dimension(110, 20));

      if (!Preferences.getBoolean("maximizerSingleFilter")) {
        filterPanelTopRow.add(filterAllButton, BorderLayout.LINE_END);
        filterPanelBottomRow.add(filterNoneButton, BorderLayout.LINE_END);
      }
      boolean usageUnderLimit;
      for (KoLConstants.filterType fType : KoLConstants.filterType.values()) {
        String filterString = fType.toString().toLowerCase();

        boolean isLastUsedFilter = true;
        if (Preferences.getBoolean("maximizerSingleFilter")) {
          isLastUsedFilter =
              Preferences.getString("maximizerLastSingleFilter").equals(filterString);
        }

        String lastFilters = Preferences.getString("maximizerLastFilters");
        boolean isPersistedFilter =
            Set.of(lastFilters.split(",")).contains(filterString) || lastFilters.equals("");

        usageUnderLimit =
            switch (fType) {
              case BOOZE ->
                  (KoLCharacter.canDrink()
                      && KoLCharacter.getInebriety() < KoLCharacter.getLiverCapacity());
              case FOOD ->
                  (KoLCharacter.canEat()
                      && KoLCharacter.getFullness() < KoLCharacter.getStomachCapacity());
              case SPLEEN -> (KoLCharacter.getSpleenUse() < KoLCharacter.getSpleenLimit());
              default -> true;
            };
        boolean isEnabled = isLastUsedFilter && isPersistedFilter && usageUnderLimit;

        filterButtons.put(fType, new JCheckBox(filterString, isEnabled));
        filterButtons.get(fType).addItemListener(this);

        if (Arrays.asList(TopRowFilters).contains(fType)) {
          filterCheckboxTopPanel.add(filterButtons.get(fType));
        } else {
          filterCheckboxBottomPanel.add(filterButtons.get(fType));
        }
        updateFilter(fType, isEnabled);
      }
      filterPanelTopRow.add(filterCheckboxTopPanel, BorderLayout.CENTER);
      filterPanelBottomRow.add(filterCheckboxBottomPanel, BorderLayout.CENTER);

      VerifiableElement[] elements = new VerifiableElement[7];
      elements[0] = new VerifiableElement("Maximize: ", MaximizerFrame.expressionSelect);
      elements[1] = new VerifiableElement("Equipment: ", equipPanel);
      elements[2] = new VerifiableElement("Max price: ", mallPanel);
      elements[3] = new VerifiableElement(searchModePanel);
      elements[4] = new VerifiableElement(searchSettingsPanel);
      elements[5] = new VerifiableElement("Filters: ", filterPanelTopRow);
      elements[6] = new VerifiableElement(filterPanelBottomRow);
      elements[5]
          .getLabel()
          .setToolTipText(
              "Other options available under menu General -> Preferences, tab General - Maximizer.");

      this.setContent(elements);
      expressionSelect.requestFocus();
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
      JCheckBox changedBox = (JCheckBox) e.getSource();
      KoLConstants.filterType thisFilter = null;
      for (KoLConstants.filterType fType : KoLConstants.filterType.values()) {
        if (fType.name().equalsIgnoreCase(changedBox.getText())) {
          thisFilter = fType;
          break;
        }
      }

      if (Preferences.getBoolean("maximizerSingleFilter")) {
        if (!changedBox.isSelected()) {
          return;
        }

        for (KoLConstants.filterType fType : KoLConstants.filterType.values()) {
          // selecting a filter turns off all others, as if it was a radio button
          boolean singleSelect = (fType == thisFilter);

          filterButtons.get(fType).setSelected(singleSelect);

          if (singleSelect) {
            activeFilters.add(fType);
          } else {
            activeFilters.remove(fType);
          }
        }
        Preferences.setString("maximizerLastSingleFilter", changedBox.getText());
      } else if (thisFilter != null) {
        boolean updatedValue = (e.getStateChange() == ItemEvent.SELECTED);
        updateFilter(thisFilter, updatedValue);
      }
    }

    @Override
    public void actionConfirmed() {
      MaximizerFrame.this.maximize();
    }

    @Override
    public void actionCancelled() {
      // InputFieldUtilities.alert( MaximizerFrame.HELP_STRING );
      JLabel help = new JLabel(MaximizerFrame.HELP_STRING);
      // JComponentUtilities.setComponentSize( help, 750, -1 );
      GenericScrollPane content = new GenericScrollPane(help);
      JComponentUtilities.setComponentSize(content, -1, 500);
      JOptionPane.showMessageDialog(
          this, content, "Modifier Maximizer help", JOptionPane.PLAIN_MESSAGE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      JButton SourceButton = (JButton) e.getSource();
      Boolean filterOn = (SourceButton.getText().equalsIgnoreCase("Set All Filters"));
      updateCheckboxes(filterOn);
    }
  }

  private void updateCheckboxes(Boolean filterAll) {
    for (KoLConstants.filterType fType : KoLConstants.filterType.values()) {
      filterButtons.get(fType).setSelected(filterAll);
    }
  }

  protected void updateFilter(KoLConstants.filterType f, boolean value) {
    try {
      String lastFilters = Preferences.getString("maximizerLastFilters");

      Set<String> newFilters;
      if (lastFilters.equals("")) {
        newFilters = new HashSet();
        for (KoLConstants.filterType filter : KoLConstants.filterType.values()) {
          newFilters.add(filter.toString().toLowerCase());
        }
      } else {
        newFilters = new HashSet(Arrays.asList(lastFilters.split(",")));
      }

      if (value) {
        activeFilters.add(f);
        newFilters.add(f.toString().toLowerCase());
      } else {
        activeFilters.remove(f);
        newFilters.remove(f.toString().toLowerCase());
      }
      Preferences.setString("maximizerLastFilters", String.join(",", newFilters));
    } catch (Exception Ex) {
      // This should probably log the error...
    }
  }

  protected EnumSet<filterType> getActiveFilters() {
    return this.activeFilters;
  }

  private static class PullableRadioButton extends JRadioButton implements Listener {
    final String text;

    public PullableRadioButton(String text) {
      super(text);
      this.text = text;
      NamedListenerRegistry.registerNamedListener("(pullsremaining)", this);
      this.update();
    }

    @Override
    public void update() {
      int pulls = ConcoctionDatabase.getPullsRemaining();
      StringBuilder buf = new StringBuilder(this.text);
      if (pulls == -1) {
        buf.append(" (unlimited)");
      } else {
        buf.append(" (");
        buf.append(pulls);
        buf.append(" pull");
        if (pulls != 1) {
          buf.append("s");
        }
        buf.append(" left)");
      }
      this.setText(buf.toString());
    }
  }

  static class FilterBoosts extends AutoFilterTextField<Boost> {
    private List<Filter> parsedText;

    public FilterBoosts(JList<Boost> list) {
      super(list);
    }

    LockableListModel<Boost> getModel() {
      return this.model;
    }

    private record Filter(String txt, boolean strict, boolean not) {}

    private LinkedList<Filter> parseFilter(String text) {
      var lst = new LinkedList<Filter>();
      var split = text.split("\\s+");

      for (var spl : split) {
        if (spl.length() > 2 && spl.startsWith("!'")) {
          lst.add(new Filter(spl.substring(2), true, true));
          // backwards compat: accept '-' for not
        } else if (spl.length() > 1 && (spl.startsWith("!") || spl.startsWith("-"))) {
          lst.add(new Filter(spl.substring(1), false, true));
        } else if (spl.length() > 1 && spl.startsWith("'")) {
          lst.add(new Filter(spl.substring(1), true, false));
        } else {
          lst.add(new Filter(spl, false, false));
        }
      }

      return lst;
    }

    @Override
    public void update() {
      try {
        var text = this.getText().toLowerCase();
        this.text = text;
        this.parsedText = parseFilter(text);

        if (parsedText.size() == 1 && !parsedText.get(0).strict) {
          // use the old strict / non-strict behaviour: strict first, else non-strict if no matches
          var filter = parsedText.get(0);
          parsedText = List.of(new Filter(filter.txt, true, filter.not));
          this.model.updateFilter(false);
          if (this.model.getSize() == 0) {
            parsedText = List.of(filter);
            this.model.updateFilter(false);
          }
        } else {
          this.model.updateFilter(false);
        }
        updateList();
      } finally {
        fireContentsChanged();
      }
    }

    @Override
    public boolean isVisible(final Object element) {
      if (this.text == null || this.text.isEmpty()) {
        return true;
      }

      var cmd = element.toString().toLowerCase();

      for (var elt : parsedText) {
        var matches = matchedFilter(cmd, elt);
        if (!matches) {
          return false;
        }
      }

      return true;
    }

    private boolean matchedFilter(String cmd, Filter filter) {
      var matches =
          filter.strict ? cmd.contains(filter.txt) : StringUtilities.fuzzyMatches(cmd, filter.txt);
      if (filter.not) {
        matches = !matches;
      }
      return matches;
    }
  }

  private class BoostsPanel extends ScrollableFilteredPanel<Boost> {
    public BoostsPanel(final ShowDescriptionList<Boost> list) {
      super("Current score: --- \u25CA Predicted: ---", "equip all", "exec selected", list);
      MaximizerFrame.this.listTitle = this.titleComponent;
    }

    @Override
    protected AutoFilterTextField<Boost> createFilterField() {
      return new FilterBoosts(this.elementList);
    }

    @Override
    public void actionConfirmed() {
      KoLmafia.forceContinue();
      boolean any = false;
      for (Boost boost : Maximizer.boosts) {
        if (boost != null) {
          boolean did = boost.execute(true);
          if (!KoLmafia.permitsContinue()) return;
          any |= did;
        }
      }
      if (any) {
        MaximizerFrame.this.maximize();
      }
    }

    @Override
    public void actionCancelled() {
      KoLmafia.forceContinue();
      boolean any = false;
      Object[] boosts = this.elementList.getSelectedValuesList().toArray();
      for (Object boost : boosts) {
        if (boost instanceof Boost) {
          boolean did = ((Boost) boost).execute(false);
          if (!KoLmafia.permitsContinue()) return;
          any |= did;
        }
      }
      if (any) {
        MaximizerFrame.this.maximize();
      }
    }
  }
}
