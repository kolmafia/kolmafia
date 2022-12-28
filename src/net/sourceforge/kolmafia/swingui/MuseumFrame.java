package net.sourceforge.kolmafia.swingui;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.PanelList;
import net.java.dev.spellcast.utilities.PanelListCell;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.DisplayCaseRequest;
import net.sourceforge.kolmafia.session.DisplayCaseManager;
import net.sourceforge.kolmafia.swingui.button.InvocationButton;
import net.sourceforge.kolmafia.swingui.panel.ItemListManagePanel;
import net.sourceforge.kolmafia.swingui.panel.OverlapPanel;
import net.sourceforge.kolmafia.swingui.panel.ScrollableFilteredPanel;
import net.sourceforge.kolmafia.swingui.panel.ScrollablePanel;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class MuseumFrame extends GenericFrame {
  private final JComponent general, restock, shelves, ordering;

  /**
   * Constructs a new <code>MuseumFrame</code> and inserts all of the necessary panels into a
   * tabular layout for accessibility.
   */
  public MuseumFrame() {
    super("Museum Display");

    this.general = new AddRemovePanel();
    this.restock = new DisplayCaseMatchPanel();
    this.shelves = new MuseumShelfList();
    this.ordering = new OrderingPanel();

    this.addTab("General", this.general);
    this.addTab("End of Run", this.restock);
    this.addTab("Shelves", this.shelves);
    this.tabs.addTab("Ordering", this.ordering);

    this.setCenterComponent(this.tabs);
  }

  private static class DisplayCaseMatchPanel extends OverlapPanel {
    public DisplayCaseMatchPanel() {
      super("display", "help", (SortedListModel<AdventureResult>) KoLConstants.collection, true);
    }

    @Override
    public void actionConfirmed() {
      KoLmafia.updateDisplay("Gathering data...");

      AdventureResult[] display = new AdventureResult[KoLConstants.collection.size()];
      KoLConstants.collection.toArray(display);

      List<AdventureResult> items = new ArrayList<>();

      for (int i = 0; i < display.length; ++i) {
        AdventureResult item = display[i];
        int itemCount = item.getCount(KoLConstants.inventory);
        if (itemCount > 0 && item.getCount() > 1) {
          items.add(item.getInstance(itemCount));
        }
      }

      if (items.isEmpty()) {
        return;
      }

      RequestThread.postRequest(
          new DisplayCaseRequest(items.toArray(new AdventureResult[0]), true));
    }

    @Override
    public void actionCancelled() {
      InputFieldUtilities.alert(
          "This feature scans your inventory and if it finds any items which are in your display case, it puts those items on display.");
    }
  }

  /**
   * Internal class used to handle everything related to placing items into the display and taking
   * items from the display.
   */
  private static class AddRemovePanel extends JPanel {
    private final ScrollablePanel<ShowDescriptionList<AdventureResult>> inventoryPanel,
        displayPanel;

    public AddRemovePanel() {
      this.setLayout(new GridLayout(2, 1, 10, 10));

      this.inventoryPanel = new OutsideDisplayPanel();
      this.displayPanel = new InsideDisplayPanel();

      this.add(this.inventoryPanel);
      this.add(this.displayPanel);
    }

    @Override
    public void setEnabled(final boolean isEnabled) {
      if (this.inventoryPanel == null || this.displayPanel == null) {
        return;
      }

      super.setEnabled(isEnabled);
      this.inventoryPanel.setEnabled(isEnabled);
      this.displayPanel.setEnabled(isEnabled);
    }

    private AdventureResult[] getSelectedValuesList(
        final AdventureResult[] selection, boolean moveAll) {
      if (!moveAll) {
        for (int i = 0; i < selection.length; ++i) {
          AdventureResult item = selection[i];
          Integer value =
              InputFieldUtilities.getQuantity(
                  "Moving " + item.getName() + "...", item.getCount(), 1);
          int count = (value == null) ? 0 : value.intValue();
          selection[i] = item.getInstance(count);
        }
      }

      return selection;
    }

    private class OutsideDisplayPanel extends ScrollableFilteredPanel<AdventureResult> {
      private final ShowDescriptionList<AdventureResult> elementList;

      public OutsideDisplayPanel() {
        super(
            "Inventory",
            "add all",
            "add some",
            new ShowDescriptionList<>((SortedListModel<AdventureResult>) KoLConstants.inventory));
        this.elementList = this.scrollComponent;
      }

      private void move(final boolean moveAll) {
        AdventureResult[] selection = this.elementList.getSelectedItems();
        AdventureResult[] items = AddRemovePanel.this.getSelectedValuesList(selection, moveAll);
        RequestThread.postRequest(new DisplayCaseRequest(items, true));
        RequestThread.postRequest(new DisplayCaseRequest());
      }

      @Override
      public void actionConfirmed() {
        this.move(true);
      }

      @Override
      public void actionCancelled() {
        this.move(false);
      }
    }

    private class InsideDisplayPanel extends ScrollableFilteredPanel<AdventureResult> {
      private final ShowDescriptionList<AdventureResult> elementList;

      public InsideDisplayPanel() {
        super(
            "Display Case",
            "take all",
            "take some",
            new ShowDescriptionList<>((SortedListModel<AdventureResult>) KoLConstants.collection));
        this.elementList = this.scrollComponent;
      }

      private void move(final boolean moveAll) {
        AdventureResult[] selection = this.elementList.getSelectedItems();
        AdventureResult[] items = AddRemovePanel.this.getSelectedValuesList(selection, moveAll);
        RequestThread.postRequest(new DisplayCaseRequest(items, false));
        RequestThread.postRequest(new DisplayCaseRequest());
      }

      @Override
      public void actionConfirmed() {
        this.move(true);
      }

      @Override
      public void actionCancelled() {
        this.move(false);
      }
    }
  }

  public static class MuseumShelfList extends PanelList {
    public MuseumShelfList() {
      super(1, 480, 200, DisplayCaseManager.getShelves(), true);
    }

    @Override
    public PanelListCell constructPanelListCell(final Object value, final int index) {
      MuseumShelfPanel toConstruct =
          new MuseumShelfPanel(index, (SortedListModel<AdventureResult>) value);
      return toConstruct;
    }

    @Override
    public boolean isResizeableList() {
      return true;
    }
  }

  public static class MuseumShelfPanel extends ScrollablePanel<ShowDescriptionList<AdventureResult>>
      implements PanelListCell {
    private final int index;
    private final ShowDescriptionList<AdventureResult> elementList;

    public MuseumShelfPanel(final int index, final SortedListModel<AdventureResult> value) {
      super(
          DisplayCaseManager.getHeader(index),
          "move",
          "remove",
          new ShowDescriptionList<>(value),
          false);

      this.index = index;
      this.elementList = this.scrollComponent;
    }

    @Override
    public void actionConfirmed() {
      String[] headerArray = DisplayCaseManager.getHeaders().toArray(new String[0]);

      String selectedValue = InputFieldUtilities.input("Moving to this shelf...", headerArray);

      if (selectedValue == null) {
        return;
      }

      for (int i = 0; i < headerArray.length; ++i) {
        if (selectedValue.equals(headerArray[i])) {
          DisplayCaseManager.move(this.elementList.getSelectedValuesList(), this.index, i);
          break;
        }
      }
    }

    @Override
    public void actionCancelled() {
      AdventureResult[] items = this.elementList.getSelectedItems();
      RequestThread.postRequest(new DisplayCaseRequest(items, false));
      RequestThread.postRequest(new DisplayCaseRequest());
    }

    @Override
    public void updateDisplay(final PanelList list, final Object value, final int index) {}
  }

  public static class OrderingPanel extends ItemListManagePanel<String> {
    @SuppressWarnings("unchecked")
    // LockableListModel (the type of getHeaders())'s clone() method still claims to return an
    // Object, but we know its internal list has the same elements, so their types definitely match
    public OrderingPanel() {
      super((LockableListModel<String>) DisplayCaseManager.getHeaders().clone());

      this.setButtons(
          false,
          new ActionListener[] {
            new MoveUpListener(),
            new MoveDownListener(),
            new InvocationButton("apply", this, "apply")
          });
    }

    private class MoveUpListener implements ActionListener {
      @Override
      public void actionPerformed(final ActionEvent e) {
        int selectedIndex = OrderingPanel.this.getElementList().getSelectedIndex();
        if (selectedIndex < 1) {
          return;
        }

        String removed = OrderingPanel.this.elementModel.remove(selectedIndex);
        OrderingPanel.this.elementModel.add(selectedIndex - 1, removed);
        OrderingPanel.this.getElementList().setSelectedIndex(selectedIndex - 1);
      }

      @Override
      public String toString() {
        return "move up";
      }
    }

    private class MoveDownListener implements ActionListener {
      @Override
      public void actionPerformed(final ActionEvent e) {
        int selectedIndex = OrderingPanel.this.getElementList().getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex == OrderingPanel.this.elementModel.size() - 1) {
          return;
        }

        String removed = OrderingPanel.this.elementModel.remove(selectedIndex);
        OrderingPanel.this.elementModel.add(selectedIndex + 1, removed);
        OrderingPanel.this.getElementList().setSelectedIndex(selectedIndex + 1);
      }

      @Override
      public String toString() {
        return "move down";
      }
    }

    public void apply() {
      String[] headerArray = new String[this.elementModel.size()];
      this.elementModel.toArray(headerArray);
      DisplayCaseManager.reorder(headerArray);
    }
  }
}
