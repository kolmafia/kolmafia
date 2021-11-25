package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.InputStream;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Enumeration;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.maximizer.Boost;
import net.sourceforge.kolmafia.maximizer.Maximizer;
import net.sourceforge.kolmafia.maximizer.MaximizerSpeculation;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.panel.ScrollableFilteredPanel;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;
import net.sourceforge.kolmafia.utilities.ByteBufferUtilities;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MaximizerFrame extends GenericFrame implements ListSelectionListener {
  public static final JComboBox<Object> expressionSelect = new JComboBox<>();

  static { // This has to be done before the constructor runs, since the
    // CLI "maximize" command can set the selected item prior to the
    // frame being instantiated.
    expressionSelect.setEditable(true);
    KoLConstants.maximizerMList.updateJComboData(expressionSelect);
  }

  private SmartButtonGroup equipmentSelect, mallSelect;
  private AutoHighlightTextField maxPriceField;
  private final ShowDescriptionList<Boost> boostList;
  private final EnumMap<KoLConstants.filterType, Boolean> activeFilters;
  private EnumMap<KoLConstants.filterType, JCheckBox> filterButtons;
  private JLabel listTitle = null;

  private static String HELP_STRING;

  static {
    InputStream stream =
        DataUtilities.getInputStream(KoLConstants.DATA_DIRECTORY, "maximizer-help.html", false);
    byte[] bytes = ByteBufferUtilities.read(stream);
    MaximizerFrame.HELP_STRING = StringUtilities.getEncodedString(bytes, "UTF-8");
  }

  public MaximizerFrame() {
    super("Modifier Maximizer");

    JPanel wrapperPanel = new JPanel(new BorderLayout());
    wrapperPanel.add(new MaximizerPanel(), BorderLayout.NORTH);

    this.boostList = new ShowDescriptionList<>(Maximizer.boosts, 12);
    this.boostList.addListSelectionListener(this);
    this.activeFilters = new EnumMap<>(KoLConstants.filterType.class);

    wrapperPanel.add(new BoostsPanel(this.boostList), BorderLayout.CENTER);

    this.setCenterComponent(wrapperPanel);

    if (Maximizer.eval != null) {
      this.valueChanged(null);
    } else {
      if (Preferences.getInteger("maximizerMRUSize") > 0) {
        KoLConstants.maximizerMList.updateJComboData(expressionSelect);
      }
    }
    for (KoLConstants.filterType f : KoLConstants.filterType.values()) {
      activeFilters.put(f, true);
    }
  }

  @Override
  public JTabbedPane getTabbedPane() {
    return null;
  }

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
      KoLConstants.maximizerMList.updateJComboData(expressionSelect);
    }
  }

  public void maximize() {
    Maximizer.maximize(
        this.equipmentSelect.getSelectedIndex(),
        InputFieldUtilities.getValue(this.maxPriceField),
        this.mallSelect.getSelectedIndex(),
        Preferences.getBoolean("maximizerIncludeAll"),
        this.activeFilters);

    this.valueChanged(null);
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

      KoLConstants.filterType[] TopRowFilters =
          new KoLConstants.filterType[] {
            KoLConstants.filterType.EQUIP,
            KoLConstants.filterType.CAST,
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
        switch (fType) {
          case BOOZE:
            usageUnderLimit =
                (KoLCharacter.canDrink()
                    && KoLCharacter.getInebriety() < KoLCharacter.getInebrietyLimit());
            break;
          case FOOD:
            usageUnderLimit =
                (KoLCharacter.canEat()
                    && KoLCharacter.getFullness() < KoLCharacter.getFullnessLimit());
            break;
          case SPLEEN:
            usageUnderLimit = (KoLCharacter.getSpleenUse() < KoLCharacter.getSpleenLimit());
            break;
          default:
            usageUnderLimit = true;
        }
        boolean isLastUsedFilter =
            Preferences.getString("maximizerLastSingleFilter").equalsIgnoreCase(fType.name());
        if (Preferences.getBoolean("maximizerSingleFilter")) {
          usageUnderLimit = isLastUsedFilter;
        }
        filterButtons.put(fType, new JCheckBox(fType.toString().toLowerCase(), usageUnderLimit));
        filterButtons.get(fType).addItemListener(this);

        if (Arrays.asList(TopRowFilters).contains(fType)) {
          filterCheckboxTopPanel.add(filterButtons.get(fType));
        } else {
          filterCheckboxBottomPanel.add(filterButtons.get(fType));
        }
        updateFilter(fType, usageUnderLimit);
      }
      filterPanelTopRow.add(filterCheckboxTopPanel, BorderLayout.CENTER);
      filterPanelBottomRow.add(filterCheckboxBottomPanel, BorderLayout.CENTER);

      VerifiableElement[] elements = new VerifiableElement[5];
      elements[0] = new VerifiableElement("Maximize: ", MaximizerFrame.expressionSelect);
      elements[1] = new VerifiableElement("Equipment: ", equipPanel);
      elements[2] = new VerifiableElement("Max price: ", mallPanel);
      elements[3] = new VerifiableElement("Filters: ", filterPanelTopRow);
      elements[4] = new VerifiableElement(filterPanelBottomRow);
      elements[3]
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

          if (activeFilters.containsKey(thisFilter)) {
            activeFilters.replace(fType, singleSelect);
          } else {
            activeFilters.put(fType, singleSelect);
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

  protected void updateFilter(KoLConstants.filterType f, Boolean value) {
    try {
      if (activeFilters.containsKey(f)) {
        activeFilters.replace(f, value);
      } else {
        activeFilters.put(f, value);
      }
    } catch (Exception Ex) {
      // This should probably log the error...
    }
  }

  private static class PullableRadioButton extends JRadioButton implements Listener {
    final String text;

    public PullableRadioButton(String text) {
      super(text);
      this.text = text;
      NamedListenerRegistry.registerNamedListener("(pullsremaining)", this);
      this.update();
    }

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

  private class BoostsPanel extends ScrollableFilteredPanel<Boost> {
    private final ShowDescriptionList<Boost> elementList;

    public BoostsPanel(final ShowDescriptionList<Boost> list) {
      super("Current score: --- \u25CA Predicted: ---", "equip all", "exec selected", list);
      this.elementList = this.scrollComponent;
      MaximizerFrame.this.listTitle = this.titleComponent;
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

  public static class SmartButtonGroup
      extends ButtonGroup { // A version of ButtonGroup that actually does useful things:
    // * Constructor takes a parent container, adding buttons to
    // the group adds them to the container as well.  This generally
    // removes any need for a temp variable to hold the individual
    // buttons as they're being created.
    // * getSelectedIndex() to determine which button (0-based) is
    // selected.  How could that have been missing???

    private final Container parent;

    public SmartButtonGroup(Container parent) {
      this.parent = parent;
    }

    @Override
    public void add(AbstractButton b) {
      super.add(b);
      parent.add(b);
    }

    public void setSelectedIndex(int index) {
      int i = 0;
      Enumeration<AbstractButton> e = this.getElements();
      while (e.hasMoreElements()) {
        e.nextElement().setSelected(i == index);
        ++i;
      }
    }

    public int getSelectedIndex() {
      int i = 0;
      Enumeration<AbstractButton> e = this.getElements();
      while (e.hasMoreElements()) {
        if (e.nextElement().isSelected()) {
          return i;
        }
        ++i;
      }
      return -1;
    }
  }
}
