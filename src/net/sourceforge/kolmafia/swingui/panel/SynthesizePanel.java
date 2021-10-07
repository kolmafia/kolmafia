package net.sourceforge.kolmafia.swingui.panel;

import com.jgoodies.binding.adapter.AbstractTableAdapter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.persistence.CandyDatabase;
import net.sourceforge.kolmafia.persistence.CandyDatabase.Candy;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.request.SweetSynthesisRequest;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class SynthesizePanel extends JPanel implements ActionListener, Listener {
  // The panel with the effect buttons
  private final EffectPanel effectPanel;

  // The filter checkboxes
  private JCheckBox[] filters;
  private boolean availableChecked = false;
  private boolean chocolateChecked = false;
  private boolean blacklistChecked = false;

  // The panel with Candy A and Candy B columns
  private final CandyPanel candyPanel;
  private CandyPanel.CandyList candyList1;
  private CandyPanel.CandyList candyList2;

  // The Buttons
  private final JButton synthesizeButton;
  private final JButton automaticButton;
  private final JButton priceCheckButton;

  // The panel with data about Candy A and Candy B and total cost/turn
  private final CandyDataPanel candyData;

  // How old is "too old" for a price. Expressed in fractional days.
  public static final float AGE_LIMIT = (60.0f * 60.0f) / 86400.0f; // One hour

  public SynthesizePanel() {
    super();

    JPanel centerPanel = new JPanel(new BorderLayout(0, 0));

    JPanel northPanel = new JPanel();
    northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));

    this.effectPanel = new EffectPanel();

    northPanel.add(this.effectPanel);
    northPanel.add(this.addFilters());

    centerPanel.add(northPanel, BorderLayout.NORTH);

    candyPanel = new CandyPanel();
    centerPanel.add(this.candyPanel, BorderLayout.CENTER);

    JPanel eastPanel = new JPanel(new BorderLayout());

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

    this.synthesizeButton = new JButton("Synthesize!");
    this.synthesizeButton.addActionListener(new SynthesizeListener());
    buttonPanel.add(this.synthesizeButton);

    this.automaticButton = new JButton("Automatic");
    this.automaticButton.addActionListener(new AutomaticListener());
    buttonPanel.add(this.automaticButton);

    this.priceCheckButton = new JButton("Check Prices");
    this.priceCheckButton.addActionListener(new PriceCheckListener());
    buttonPanel.add(this.priceCheckButton);

    eastPanel.add(buttonPanel, BorderLayout.NORTH);

    this.candyData = new CandyDataPanel();
    eastPanel.add(this.candyData, BorderLayout.SOUTH);

    this.setLayout(new BorderLayout(10, 10));
    this.add(centerPanel, BorderLayout.CENTER);
    this.add(eastPanel, BorderLayout.EAST);

    NamedListenerRegistry.registerNamedListener("(candy)", this);
    PreferenceListenerRegistry.registerPreferenceListener("sweetSynthesisBlacklist", this);

    this.setEnabled(true);
  }

  private int effectId() {
    return this.effectPanel == null ? -1 : this.effectPanel.currentEffectId();
  }

  private Candy candy1() {
    return this.candyList1 == null ? null : this.candyList1.currentCandy();
  }

  private Candy candy2() {
    return this.candyList2 == null ? null : this.candyList2.currentCandy();
  }

  private static boolean haveSpleenAvailable() {
    boolean loggedIn = KoLCharacter.getUserId() > 0;
    return !loggedIn || KoLCharacter.getSpleenLimit() > KoLCharacter.getSpleenUse();
  }

  @Override
  public void setEnabled(final boolean isEnabled) {
    this.synthesizeButton.setEnabled(
        isEnabled
            && this.effectId() != -1
            && this.candy1() != null
            && this.candy2() != null
            && SynthesizePanel.haveSpleenAvailable());
    this.automaticButton.setEnabled(isEnabled && this.effectId() != -1);
    this.priceCheckButton.setEnabled(isEnabled && !this.availableChecked);
    this.effectPanel.setEnabled(isEnabled);
  }

  private JPanel addFilters() {
    JPanel filterPanel = new JPanel();

    boolean loggedIn = KoLCharacter.getUserId() > 0;
    this.availableChecked = loggedIn && !KoLCharacter.canInteract();
    this.chocolateChecked = false;
    this.blacklistChecked = true;

    // Load the current blacklist
    CandyDatabase.loadBlacklist();

    this.filters = new JCheckBox[3];
    this.filters[0] = new JCheckBox("available", this.availableChecked);
    this.filters[0].setToolTipText(
        "Show only items that 'acquire' will find. Inventory, at least.");
    this.filters[1] = new JCheckBox("chocolates", this.chocolateChecked);
    this.filters[1].setToolTipText("Allow adventure-producing chocolates as ingredients.");
    this.filters[2] = new JCheckBox("blacklist", this.blacklistChecked);
    this.filters[2].setToolTipText(
        "Do not consider expensive candies from \"sweetSynthesisBlacklist\" property.");

    for (JCheckBox checkbox : this.filters) {
      filterPanel.add(checkbox);
      checkbox.addActionListener(this);
    }

    return filterPanel;
  }

  // Called when checkbox changes
  public void actionPerformed(final ActionEvent e) {
    this.availableChecked = this.filters[0].isSelected();
    this.chocolateChecked = this.filters[1].isSelected();
    this.blacklistChecked = this.filters[2].isSelected();

    // Filter candy lists
    this.filterItems();

    // Only enable Price Check if you (might be) buying in mall
    this.priceCheckButton.setEnabled(!this.availableChecked);
  }

  // Invoke this in the Swing event thread
  private void filterItems() {
    // If we are using available candies only, availability of
    // effects might have changed
    SynthesizePanel.this.effectPanel.update();

    Candy candy1 = this.candy1();
    Candy candy2 = this.candy2();
    this.candyList1.filterItems(candy1);
    this.candyList2.filterItems(candy2);
  }

  // called when (candy) fires
  // called when "sweetSynthesisBlacklist" fires
  public void update() {
    CandyDatabase.loadBlacklist();

    for (Candy candy : this.candyList1.getCandyList()) {
      candy.update();
    }

    for (Candy candy : this.candyList2.getCandyList()) {
      candy.update();
    }

    // Having updated the data, update the GUI
    try {
      SwingUtilities.invokeAndWait(
          new Runnable() {
            public void run() {
              SynthesizePanel.this.filterItems();

              // Since quantities might have changed, sort
              Candy candy1 = SynthesizePanel.this.candy1();
              Candy candy2 = SynthesizePanel.this.candy2();
              SynthesizePanel.this.candyList1.sortCandy(candy1);
              SynthesizePanel.this.candyList2.sortCandy(candy2);

              SynthesizePanel.this.candyData.update();
            }
          });
    } catch (Exception ie) {
    }
  }

  private class EffectPanel extends JPanel {
    public EffectButton selected = null;

    public EffectPanel() {
      super(new GridLayout(3, 5));

      // Tier 1 effects
      this.add(new EffectButton("Hot Res +9", EffectPool.SYNTHESIS_HOT));
      this.add(new EffectButton("Cold Res +9", EffectPool.SYNTHESIS_COLD));
      this.add(new EffectButton("Stench Res +9", EffectPool.SYNTHESIS_PUNGENT));
      this.add(new EffectButton("Spooky Res +9", EffectPool.SYNTHESIS_SCARY));
      this.add(new EffectButton("Sleaze Res +9", EffectPool.SYNTHESIS_GREASY));

      // Tier 2 effects
      this.add(new EffectButton("Mus +300%", EffectPool.SYNTHESIS_STRONG));
      this.add(new EffectButton("Mys +300%", EffectPool.SYNTHESIS_SMART));
      this.add(new EffectButton("Mox +300%", EffectPool.SYNTHESIS_COOL));
      this.add(new EffectButton("Max HP +300%", EffectPool.SYNTHESIS_HARDY));
      this.add(new EffectButton("Max MP +300%", EffectPool.SYNTHESIS_ENERGY));

      // Tier 3 effects
      this.add(new EffectButton("Meat +300%", EffectPool.SYNTHESIS_GREED));
      this.add(new EffectButton("Item +150%", EffectPool.SYNTHESIS_COLLECTION));
      this.add(new EffectButton("Mus Exp +50%", EffectPool.SYNTHESIS_MOVEMENT));
      this.add(new EffectButton("Mys Exp +50%", EffectPool.SYNTHESIS_LEARNING));
      this.add(new EffectButton("Mox Exp +50%", EffectPool.SYNTHESIS_STYLE));

      // Wrap the buttons in an attractive titled border
      this.setBorder(
          BorderFactory.createTitledBorder(null, "Effects", TitledBorder.CENTER, TitledBorder.TOP));
    }

    public int currentEffectId() {
      return this.selected == null ? -1 : this.selected.effectId;
    }

    public void setEnabled(final boolean isEnabled) {
      this.update();
    }

    public void update() {
      // Enable or disable buttons depending on whether the
      // effect can be made using available candies

      boolean available = SynthesizePanel.this.availableChecked;
      boolean chocolate = SynthesizePanel.this.chocolateChecked;
      boolean noblacklist = !SynthesizePanel.this.blacklistChecked;
      int flags = CandyDatabase.makeFlags(available, chocolate, noblacklist);

      for (Component component : this.getComponents()) {
        if (component instanceof EffectButton) {
          EffectButton button = (EffectButton) component;
          boolean enabled = !available;

          if (!enabled) {
            int effectId = button.effectId;
            int tier = CandyDatabase.getEffectTier(effectId);
            for (int itemId : CandyDatabase.candyForTier(tier, flags)) {
              if (CandyDatabase.sweetSynthesisPairing(effectId, itemId, flags).size() > 0) {
                enabled = true;
                break;
              }
            }

            if (!enabled && button == this.selected) {
              button.doClick();
            }
          }

          button.setEnabled(enabled);
        }
      }
    }

    private class EffectButton extends JButton implements ActionListener {
      public final int effectId;
      final Color foreground;
      final Color background;

      public EffectButton(final String name, final int effectId) {
        super(name);

        // The following makes the button a solid
        // rectangle on OS X.
        this.setContentAreaFilled(false);
        this.setBorderPainted(false);
        this.setOpaque(true);

        this.effectId = effectId;
        this.foreground = this.getForeground();
        this.background = this.getBackground();

        this.addActionListener(this);

        String effectName = EffectDatabase.getEffectName(effectId);
        this.setToolTipText(effectName);
      }

      private void originalColors() {
        this.setBackground(background);
        this.setForeground(foreground);
      }

      private void reverseColors() {
        this.setBackground(Color.BLACK);
        this.setForeground(Color.WHITE);
      }

      public void actionPerformed(final ActionEvent e) {
        EffectButton current = EffectPanel.this.selected;
        if (current != null) {
          current.originalColors();
          SynthesizePanel.this.candyList2.getCandyList().clear();
        }
        if (current == this) {
          EffectPanel.this.selected = null;
          SynthesizePanel.this.automaticButton.setEnabled(false);
          SynthesizePanel.this.candyList1.getCandyList().clear();
        } else {
          EffectPanel.this.selected = this;
          SynthesizePanel.this.automaticButton.setEnabled(true);
          this.reverseColors();
          Set<Integer> candy =
              CandyDatabase.candyForTier(
                  CandyDatabase.getEffectTier(this.effectId), CandyDatabase.ALL_CANDY);
          SynthesizePanel.this.candyList1.loadCandy(candy);
        }
      }
    }
  }

  // Why can't this be inside CandyTableModel?
  private static final String[] columnNames = {"candy", "have", "cost"};

  public class CandyTableModel extends AbstractTableAdapter {
    private static final int NAME = 0;
    private static final int COUNT = 1;
    private static final int COST = 2;

    private final LockableListModel<Candy> model;

    public CandyTableModel(LockableListModel<Candy> listModel) {
      super(listModel, columnNames);
      this.model = listModel;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      return this.model.getSize() == 0 ? Object.class : this.getValueAt(0, columnIndex).getClass();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      Candy candy = this.model.getElementAt(rowIndex);

      switch (columnIndex) {
        case NAME:
          return candy.getName();
        case COUNT:
          return IntegerPool.get(candy.getCount());
        case COST:
          return IntegerPool.get(candy.getCost());
        default:
          throw new IllegalArgumentException("Invalid column index");
      }
    }
  }

  private class CandyPanel extends JPanel {
    public CandyPanel() {
      super(new GridLayout(1, 2));

      SynthesizePanel.this.candyList1 = new CandyListA();
      this.add(SynthesizePanel.this.candyList1);

      SynthesizePanel.this.candyList2 = new CandyListB();
      this.add(SynthesizePanel.this.candyList2);
    }

    public abstract class CandyList extends JPanel
        implements ListElementFilter, ListSelectionListener {
      private final CandyTable table;
      private final TableRowSorter<CandyTableModel> rowSorter;
      private final ListSelectionModel selectionModel;

      protected final LockableListModel<Candy> model = new LockableListModel<Candy>();
      protected Candy candy = null;

      // Don't do anything with ListSelection events while we are sorting the candy list
      protected boolean sorting = false;

      public CandyList(final String title) {
        super(new BorderLayout());

        this.setPreferredSize(new Dimension(200, 400));

        this.table = new CandyTable(this.model);
        this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.selectionModel = this.table.getSelectionModel();
        this.selectionModel.addListSelectionListener(this);

        this.table.setAutoCreateRowSorter(true);
        this.rowSorter = (TableRowSorter<CandyTableModel>) this.table.getRowSorter();
        this.rowSorter.setSortable(0, false);

        this.model.setFilter(this);

        JScrollPane scrollComponent = new JScrollPane(this.table);
        scrollComponent.setBorder(
            BorderFactory.createTitledBorder(null, title, TitledBorder.CENTER, TitledBorder.TOP));

        this.add(scrollComponent, BorderLayout.CENTER);
      }

      public List<Candy> getCandyList() {
        return this.model;
      }

      public Candy currentCandy() {
        return this.candy;
      }

      public Candy getSelectedValue() {
        return this.table.getSelectedValue();
      }

      public abstract void valueChanged(ListSelectionEvent e);

      public void loadCandy(Set<Integer> itemIds) {
        List<Candy> list = CandyDatabase.itemIdSetToCandyList(itemIds);

        this.model.clear();
        this.model.addAll(list);

        this.filterItems(null);

        this.sortCandy(null);
      }

      public void sortCandy(final Candy selected) {
        this.sorting = true;
        TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(this.table.getModel());
        this.table.setRowSorter(sorter);

        List<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();

        if (KoLCharacter.canInteract()) {
          // Prefer cheapest candy, then most numerous
          sortKeys.add(new RowSorter.SortKey(CandyTableModel.COST, SortOrder.ASCENDING));
          sortKeys.add(new RowSorter.SortKey(CandyTableModel.COUNT, SortOrder.DESCENDING));
        } else {
          // Prefer most numerous candy, then cheapest
          sortKeys.add(new RowSorter.SortKey(CandyTableModel.COUNT, SortOrder.DESCENDING));
          sortKeys.add(new RowSorter.SortKey(CandyTableModel.COST, SortOrder.ASCENDING));
        }
        sortKeys.add(new RowSorter.SortKey(CandyTableModel.NAME, SortOrder.ASCENDING));

        sorter.setSortKeys(sortKeys);
        sorter.sort();

        // Don't let user use this TableRowSorter to sort on candy name
        sorter.setSortable(0, false);

        // Selected item might have moved. Make sure it is visible.
        this.selectAndScroll(candy);

        this.sorting = false;
      }

      public void filterItems(final Candy selected) {
        this.model.updateFilter(false);
        int size = this.model.getSize();

        // Update displayed rows
        this.model.fireContentsChanged(this.model, 0, size - 1);

        if (size == 0) {
          this.table.clearSelection();
        } else {
          this.selectAndScroll(selected);
        }
      }

      public void selectAndScroll(final Candy selected) {
        if (selected == null) {
          return;
        }

        int index = this.model.getIndexOf(selected);
        if (index == -1) {
          this.table.clearSelection();
          return;
        }

        // We must locate the new index in the sorted view
        int size = this.model.getSize();
        for (int i = 0; i < size; ++i) {
          int modelIndex = this.table.convertRowIndexToModel(i);
          if (this.model.getElementAt(modelIndex).equals(selected)) {
            this.table.setSelectedIndex(modelIndex);
            this.table.ensureIndexIsVisible(modelIndex);
            return;
          }
        }
      }

      public boolean isVisible(final Object o) {
        if (o instanceof Candy) {
          if (SynthesizePanel.this.availableChecked && ((Candy) o).getCount() == 0) {
            return false;
          }
          if (!SynthesizePanel.this.chocolateChecked && ((Candy) o).isChocolate()) {
            return false;
          }
          if (SynthesizePanel.this.blacklistChecked && ((Candy) o).isBlacklisted()) {
            return false;
          }
        }
        return true;
      }

      public class CandyTable extends JTable {
        private final LockableListModel<Candy> model;

        public CandyTable(LockableListModel<Candy> model) {
          super(new CandyTableModel(model));
          this.model = model;

          // Magic number! Make the name column wide.
          this.getColumnModel().getColumn(0).setPreferredWidth(220);
        }

        public void ensureIndexIsVisible(int index) {
          int i = convertRowIndexToView(index);
          this.scrollRectToVisible(this.getCellRect(i, 0, true));
        }

        public int getSelectedIndex() {
          int selectedRow = this.getSelectedRow();
          return selectedRow == -1 ? -1 : this.convertRowIndexToModel(selectedRow);
        }

        public Candy getSelectedValue() {
          int selectedRow = this.getSelectedRow();
          return selectedRow == -1
              ? null
              : this.model.getElementAt(this.convertRowIndexToModel(selectedRow));
        }

        public void setSelectedIndex(int index) {
          int i = this.convertRowIndexToView(index);
          this.setRowSelectionInterval(i, i);
        }
      }
    }

    public class CandyListA extends CandyList {
      public CandyListA() {
        super("Candy A");
      }

      public void valueChanged(ListSelectionEvent e) {
        // The selection is cleared at the beginning of a sort.
        // We will restore it when we are done.
        if (this.sorting) {
          return;
        }

        if (e.getValueIsAdjusting()) {
          // Mouse down, for example.
          // Wait until final event comes in
          return;
        }

        Candy item = SynthesizePanel.this.candyList1.getSelectedValue();
        Candy current = SynthesizePanel.this.candy1();
        Candy replace = item;
        if (current != replace) {
          this.candy = replace;
          SynthesizePanel.this.candyData.update();
          if (replace == null) {
            SynthesizePanel.this.candyList2.getCandyList().clear();
          } else {
            Set<Integer> candy =
                CandyDatabase.sweetSynthesisPairing(
                    SynthesizePanel.this.effectId(), replace.getItemId(), CandyDatabase.ALL_CANDY);
            SynthesizePanel.this.candyList2.loadCandy(candy);
          }
          SynthesizePanel.this.synthesizeButton.setEnabled(false);
        }
        return;
      }

      @Override
      public boolean isVisible(final Object o) {
        if (o instanceof Candy) {
          Candy candy = (Candy) o;

          boolean available = SynthesizePanel.this.availableChecked;
          boolean chocolate = SynthesizePanel.this.chocolateChecked;
          boolean noblacklist = !SynthesizePanel.this.blacklistChecked;

          if (!chocolate && candy.isChocolate()) {
            return false;
          }
          if (!noblacklist && candy.isBlacklisted()) {
            return false;
          }

          if (available) {
            // Filter out candy we have none of
            int count = candy.getCount();
            if (count == 0) {
              return false;
            }

            // Filter out candy which has no available pairing
            int effectId = SynthesizePanel.this.effectId();
            int itemId = candy.getItemId();
            int flags = CandyDatabase.makeFlags(available, chocolate, noblacklist);
            if (CandyDatabase.sweetSynthesisPairing(effectId, itemId, flags).size() == 0) {
              return false;
            }
          }
        }
        return true;
      }
    }

    public class CandyListB extends CandyList {
      public CandyListB() {
        super("Candy B");
      }

      public void valueChanged(ListSelectionEvent e) {
        // The selection is cleared at the beginning of a sort.
        // We will restore it when we are done.
        if (this.sorting) {
          return;
        }

        if (e.getValueIsAdjusting()) {
          // Mouse down, for example.
          // Wait until final event comes in
          return;
        }

        Candy item = SynthesizePanel.this.candyList2.getSelectedValue();
        Candy current = SynthesizePanel.this.candy2();
        Candy replace = item;
        if (current != replace) {
          this.candy = replace;
          SynthesizePanel.this.candyData.update();
          SynthesizePanel.this.synthesizeButton.setEnabled(
              replace != null && SynthesizePanel.haveSpleenAvailable());
        }
      }

      public boolean isVisible(final Object o) {
        if (o instanceof Candy) {
          Candy candy = (Candy) o;

          boolean available = SynthesizePanel.this.availableChecked;
          boolean chocolate = SynthesizePanel.this.chocolateChecked;
          boolean noblacklist = !SynthesizePanel.this.blacklistChecked;

          if (!chocolate && candy.isChocolate()) {
            return false;
          }

          if (!noblacklist && candy.isBlacklisted()) {
            return false;
          }

          if (available) {
            // Filter out candy we have none of.
            // You can synthesize two of the same
            // candy. If only have one, can't reuse it.
            int count = candy.getCount();
            if ((count == 0) || (count == 1 && candy.equals(SynthesizePanel.this.candy1()))) {
              return false;
            }
          }
        }
        return true;
      }
    }
  }

  private class CandyDataPanel extends JPanel {
    private final CandyData candyData1;
    private final CandyData candyData2;
    private final CandyTotal candyTotal;

    public CandyDataPanel() {
      super();
      this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      this.candyData1 = new CandyData("Candy A");
      this.add(this.candyData1);
      this.candyData2 = new CandyData("Candy B");
      this.add(this.candyData2);
      this.candyTotal = new CandyTotal();
      this.add(this.candyTotal);
    }

    public void update() {
      Candy candy1 = SynthesizePanel.this.candy1();
      Candy candy2 = SynthesizePanel.this.candy2();
      this.candyData1.updateCandy(candy1);
      this.candyData2.updateCandy(candy2);
      this.candyTotal.update(candy1, candy2);
    }

    private class CandyData extends JPanel {
      private final JLabel haveValue;
      private final JLabel costValue;

      public CandyData(final String title) {
        super(new BorderLayout());
        this.setBorder(
            BorderFactory.createTitledBorder(null, title, TitledBorder.CENTER, TitledBorder.TOP));

        JPanel labelPanel = new JPanel(new GridLayout(2, 1));
        labelPanel.add(new JLabel("Have: "));
        labelPanel.add(new JLabel("Cost:"));

        JPanel valuePanel = new JPanel(new GridLayout(2, 1));
        this.haveValue = new JLabel("");
        valuePanel.add(this.haveValue);
        this.costValue = new JLabel("");
        valuePanel.add(this.costValue);

        this.add(labelPanel, BorderLayout.WEST);
        this.add(valuePanel, BorderLayout.CENTER);
      }

      public void updateCandy(Candy candy) {
        if (candy == null) {
          this.haveValue.setText("");
          this.costValue.setText("");
        } else {
          this.haveValue.setText(String.valueOf(candy.getCount()));
          this.costValue.setText(String.valueOf(candy.getCost()));
        }
      }
    }

    private class CandyTotal extends JPanel {
      private final JLabel totalValue;
      private final JLabel perTurnValue;

      public CandyTotal() {
        super(new BorderLayout());
        this.setBorder(
            BorderFactory.createTitledBorder(null, "Total", TitledBorder.CENTER, TitledBorder.TOP));

        JPanel labelPanel = new JPanel(new GridLayout(2, 1));
        labelPanel.add(new JLabel("Cost:"));
        labelPanel.add(new JLabel("/Adv: "));

        JPanel valuePanel = new JPanel(new GridLayout(2, 1));
        this.totalValue = new JLabel("");
        valuePanel.add(this.totalValue);
        this.perTurnValue = new JLabel("");
        valuePanel.add(this.perTurnValue);

        this.add(labelPanel, BorderLayout.WEST);
        this.add(valuePanel, BorderLayout.CENTER);
      }

      public void update(Candy candy1, Candy candy2) {
        if (candy1 == null || candy2 == null) {
          this.totalValue.setText("");
          this.perTurnValue.setText("");
        } else {
          int total = candy1.getCost() + candy2.getCost();
          int perTurn = Math.round(total / 30.0f);
          this.totalValue.setText(String.valueOf(total));
          this.perTurnValue.setText(String.valueOf(perTurn));
        }
      }
    }
  }

  private class SynthesizeListener extends ThreadedListener {
    @Override
    protected void execute() {
      if (SynthesizePanel.this.candy1() == null
          || SynthesizePanel.this.candy2() == null
          || KoLCharacter.getUserId() == 0) {
        return;
      }

      int availableSpleen = KoLCharacter.getSpleenLimit() - KoLCharacter.getSpleenUse();
      if (availableSpleen == 0) {
        KoLmafia.updateDisplay("Your spleen is too sore to synthesize any more today");
        return;
      }

      Candy candy1 = SynthesizePanel.this.candy1();
      int itemId1 = candy1.getItemId();
      Candy candy2 = SynthesizePanel.this.candy2();
      int itemId2 = candy2.getItemId();

      boolean available = SynthesizePanel.this.availableChecked;
      int have1 = available ? candy1.getCount() : Integer.MAX_VALUE;
      int have2 = available ? candy2.getCount() : Integer.MAX_VALUE;
      int have =
          !available
              ? Integer.MAX_VALUE
              : (itemId1 == itemId2) ? (have1 / 2) : Math.min(have1, have2);

      int casts = Math.min(availableSpleen, have);

      if (casts > 1) {
        String message = "You can synthesize up to " + casts + " times. How many do you want?";
        Integer value = InputFieldUtilities.getQuantity(message, casts, 1);
        if (value == null) {
          return;
        }
        casts = value.intValue();
      }

      KoLmafia.updateDisplay(
          "Synthesizing " + casts + " " + candy1 + " with " + casts + " " + candy2 + "...");

      SweetSynthesisRequest request = new SweetSynthesisRequest(casts, itemId1, itemId2);
      RequestThread.postRequest(request);

      if (KoLmafia.permitsContinue()) {
        KoLmafia.updateDisplay("Done!");
      }
    }

    @Override
    public String toString() {
      return "synthesize";
    }
  }

  private class AutomaticListener implements ActionListener {
    public void actionPerformed(final ActionEvent e) {
      int effectId = SynthesizePanel.this.effectId();
      if (effectId == -1) {
        return;
      }

      // Flags required by current character state are not
      // optional, but the user can use the checkboxes to be
      // more restrictive

      boolean available = SynthesizePanel.this.availableChecked;
      boolean chocolate = SynthesizePanel.this.chocolateChecked;
      boolean noblacklist = !SynthesizePanel.this.blacklistChecked;
      int flags =
          CandyDatabase.defaultFlags() | CandyDatabase.makeFlags(available, chocolate, noblacklist);

      Candy[] pair = CandyDatabase.synthesisPair(effectId, flags);
      if (pair.length == 0) {
        KoLmafia.updateDisplay("Can't find a pair of candies for that effect");
        return;
      }

      candyList1.selectAndScroll(pair[0]);
      candyList2.selectAndScroll(pair[1]);
    }

    @Override
    public String toString() {
      return "automatic";
    }
  }

  private class PriceCheckListener extends ThreadedListener {
    @Override
    protected void execute() {
      // As of 2019-07-21, there are 140 "potions", 23 "foods", and 55 "other" candies
      //
      // There are 105 pages of "potions", which is fewer than 140 potions updated individually
      // There are 70 pages of "foods", which is more than 23 foods updated individually
      // There is  no category that contains non-potion, non-food candies.
      //
      // Therefore, by bulk updating all potions, it will take
      //    105 + 23 + 55 = 183 pages hits
      // to update the mall prices for all
      //    140 + 23 + 55 = 218 candies.
      //
      // If there were a "candies" category, all candies could be done in
      // 22 server hits. Instead, it takes 183 server hits.  I submitted a
      // Feature Request to KoL for that, but no joy so far
      //
      // On the other hand, if we update all candies individually, it will
      // take more server hits, but cached prices will not require a hit.

      CandyDatabase.updatePrices();
      SynthesizePanel.this.update();

      KoLmafia.updateDisplay("All prices are less than one hour old.");
    }

    @Override
    public String toString() {
      return "check prices";
    }
  }
}
