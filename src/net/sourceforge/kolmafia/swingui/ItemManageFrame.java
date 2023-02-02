package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.objectpool.ConcoctionType;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.ClosetRequest.ClosetRequestType;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.StorageRequest.StorageRequestType;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.swingui.button.InvocationButton;
import net.sourceforge.kolmafia.swingui.panel.CardLayoutSelectorPanel;
import net.sourceforge.kolmafia.swingui.panel.CreateItemPanel;
import net.sourceforge.kolmafia.swingui.panel.CreateSpecialPanel;
import net.sourceforge.kolmafia.swingui.panel.InventoryPanel;
import net.sourceforge.kolmafia.swingui.panel.ItemManagePanel;
import net.sourceforge.kolmafia.swingui.panel.LabeledPanel;
import net.sourceforge.kolmafia.swingui.panel.OverlapPanel;
import net.sourceforge.kolmafia.swingui.panel.PulverizePanel;
import net.sourceforge.kolmafia.swingui.panel.RestorativeItemPanel;
import net.sourceforge.kolmafia.swingui.panel.UseItemDequeuePanel;
import net.sourceforge.kolmafia.swingui.panel.UseItemEnqueuePanel;
import net.sourceforge.kolmafia.swingui.panel.UseItemPanel;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightSpinner;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.textui.command.AutoMallCommand;
import net.sourceforge.kolmafia.textui.command.CleanupJunkRequest;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ItemManageFrame extends GenericFrame {
  private static final JLabel pullsRemainingLabel1 = new JLabel(" ");
  private static final JLabel pullsRemainingLabel2 = new JLabel(" ");
  private static final PullBudgetSpinner pullBudgetSpinner1 = new PullBudgetSpinner();
  private static final PullBudgetSpinner pullBudgetSpinner2 = new PullBudgetSpinner();
  private static CardLayoutSelectorPanel selectorPanel;

  /**
   * Constructs a new <code>ItemManageFrame</code> and inserts all of the necessary panels into a
   * tabular layout for accessibility.
   */
  public ItemManageFrame() {
    this(true);
  }

  public ItemManageFrame(final boolean useTabs) {
    super("Item Manager");

    ItemManageFrame.selectorPanel = new CardLayoutSelectorPanel("itemManagerIndex");

    boolean creationQueue = Preferences.getBoolean("addCreationQueue");

    selectorPanel.addPanel("Usable", new UseItemPanel());
    selectorPanel.addPanel(" - Food", makeConsumablePanel(ConcoctionType.FOOD, creationQueue));
    selectorPanel.addPanel(" - Booze", makeConsumablePanel(ConcoctionType.BOOZE, creationQueue));
    selectorPanel.addPanel(" - Spleen", makeConsumablePanel(ConcoctionType.SPLEEN, creationQueue));
    selectorPanel.addPanel(" - Potions", makeConsumablePanel(ConcoctionType.POTION, creationQueue));
    selectorPanel.addPanel(" - Restores", new RestorativeItemPanel());

    selectorPanel.addSeparator();

    selectorPanel.addPanel(
        "General",
        new InventoryPanel<>((SortedListModel<AdventureResult>) KoLConstants.inventory, false));
    selectorPanel.addPanel(
        " - Recent",
        new InventoryPanel<>((SortedListModel<AdventureResult>) KoLConstants.tally, false));
    selectorPanel.addPanel(
        " - Closet",
        new InventoryPanel<>((SortedListModel<AdventureResult>) KoLConstants.closet, false));
    selectorPanel.addPanel(" - Storage", new HagnkStoragePanel(false));
    selectorPanel.addPanel(
        " - Unlimited",
        new ViewOnlyPanel((SortedListModel<AdventureResult>) KoLConstants.unlimited));
    selectorPanel.addPanel(" - Free Pulls", new FreePullsPanel());
    selectorPanel.addPanel(
        " - No Pull", new ViewOnlyPanel((SortedListModel<AdventureResult>) KoLConstants.nopulls));

    selectorPanel.addSeparator();

    selectorPanel.addPanel("Creatable", new CreateItemPanel(true, true, true, true));

    selectorPanel.addPanel(" - Cookable", new CreateItemPanel(true, false, false, false));
    selectorPanel.addPanel(" - Mixable", new CreateItemPanel(false, true, false, false));
    selectorPanel.addPanel(" - Fine Tuning", new CreateSpecialPanel());

    selectorPanel.addSeparator();

    selectorPanel.addPanel(
        "Equipment",
        new InventoryPanel<>((SortedListModel<AdventureResult>) KoLConstants.inventory, true));
    selectorPanel.addPanel(
        " - Storage ",
        new HagnkStoragePanel(
            true)); // the extra end space is used to distinguish it for serializing purposes
    selectorPanel.addPanel(" - Create", new CreateItemPanel(false, false, true, false));
    selectorPanel.addPanel(" - Pulverize", new PulverizePanel());

    // Now a special panel which does nothing more than list
    // some common actions and some descriptions.

    selectorPanel.addSeparator();

    selectorPanel.addPanel("Item Filters", new ItemFilterPanel());
    selectorPanel.addPanel(" - Mementos", new MementoItemsPanel());
    selectorPanel.addPanel(" - Cleanup", new JunkItemsPanel());
    selectorPanel.addPanel(" - Keep One", new SingletonItemsPanel());
    selectorPanel.addPanel(" - Restock", new RestockPanel());

    selectorPanel.setSelectedIndex(Preferences.getInteger("itemManagerIndex"));

    this.setCenterComponent(selectorPanel);

    ItemManageFrame.setHeaderStates();
  }

  public JPanel makeConsumablePanel(ConcoctionType type, boolean creationQueue) {
    JPanel panel = new JPanel(new BorderLayout());
    JTabbedPane queueTabs = null;

    if (creationQueue) {
      UseItemDequeuePanel dequeuePanel = new UseItemDequeuePanel(type);
      panel.add(dequeuePanel, BorderLayout.NORTH);
      queueTabs = dequeuePanel.getQueueTabs();
    }

    panel.add(new UseItemEnqueuePanel(type, queueTabs), BorderLayout.CENTER);
    return panel;
  }

  public static void saveHeaderStates() {
    if (ItemManageFrame.selectorPanel == null) {
      return;
    }
    ArrayList<JComponent> panels = ItemManageFrame.selectorPanel.panels;
    StringBuilder builder = new StringBuilder();

    for (int i = 0; i < panels.size(); ++i) {
      JComponent comp = panels.get(i);
      if (comp instanceof InventoryPanel) {
        String s = ItemManageFrame.selectorPanel.panelNames.get(i).toString();
        if (s.startsWith(" - ")) {
          s = s.substring(3);
        }
        builder.append(s).append("|");
        builder.append(((InventoryPanel<?>) comp).scrollComponent.collectHeaderStates());
      } else if (comp instanceof RestorativeItemPanel) {
        String s = ItemManageFrame.selectorPanel.panelNames.get(i).toString();
        if (s.startsWith(" - ")) {
          s = s.substring(3);
        }
        builder.append(s).append("|");
        builder.append(((RestorativeItemPanel) comp).scrollComponent.collectHeaderStates());
      }
    }

    Preferences.setString("headerStates", builder.toString());
  }

  public static void setHeaderStates() {
    ArrayList<JComponent> panels = ItemManageFrame.selectorPanel.panels;

    // first, parse the raw string

    String rawPref = Preferences.getString("headerStates");

    if (rawPref.length() < 1) {
      return;
    }

    for (String it : rawPref.split(";")) {
      if (it.length() < 1) {
        continue;
      }

      String panelName = it.split("\\|")[0];

      if (panelName.length() < 3) {
        // sanitizing; no valid panel names with length less than 5 or so. Don't want a
        // whitespace string to match a panel name.
        continue;
      }
      // find a panel that's named the same thing as the pref value
      for (int i = 0; i < panels.size(); ++i) {
        JComponent comp = panels.get(i);
        if (comp instanceof InventoryPanel) {
          String s = ItemManageFrame.selectorPanel.panelNames.get(i).toString();

          if (s.contains(panelName)) {
            // set the header states.
            ((InventoryPanel<?>) comp).scrollComponent.setHeaderStates(it);
            break;
          }
        } else if (comp instanceof RestorativeItemPanel) {
          String s = ItemManageFrame.selectorPanel.panelNames.get(i).toString();
          if (s.contains(panelName)) {
            // set the header states.
            ((RestorativeItemPanel) comp).scrollComponent.setHeaderStates(it);
            break;
          }
        }
      }
    }
  }

  public static void updatePullsRemaining(final int pullsRemaining) {
    if (KoLCharacter.isHardcore()) {
      ItemManageFrame.pullsRemainingLabel1.setText("In Hardcore");
      ItemManageFrame.pullsRemainingLabel2.setText("In Hardcore");
      return;
    }

    switch (pullsRemaining) {
      case 0 -> {
        ItemManageFrame.pullsRemainingLabel1.setText("No Pulls Left");
        ItemManageFrame.pullsRemainingLabel2.setText("No Pulls Left");
      }
      case 1 -> {
        ItemManageFrame.pullsRemainingLabel1.setText("1 Pull Left");
        ItemManageFrame.pullsRemainingLabel2.setText("1 Pull Left");
      }
      default -> {
        ItemManageFrame.pullsRemainingLabel1.setText(pullsRemaining + " Pulls Left");
        ItemManageFrame.pullsRemainingLabel2.setText(pullsRemaining + " Pulls Left");
      }
    }
  }

  public static void updatePullsBudgeted(final int pullsBudgeted) {
    Integer value = pullsBudgeted;
    ItemManageFrame.pullBudgetSpinner1.setValue(value);
    ItemManageFrame.pullBudgetSpinner2.setValue(value);
  }

  private static class JunkItemsPanel extends OverlapPanel {
    public JunkItemsPanel() {
      super("cleanup", "help", (LockableListModel<AdventureResult>) KoLConstants.junkList, true);
    }

    @Override
    public void actionConfirmed() {
      CleanupJunkRequest.cleanup();
    }

    @Override
    public void actionCancelled() {
      InputFieldUtilities.alert(
          "These items have been flagged as \"junk\" because at some point in the past, you've opted to autosell all of the item.  If you use the \"cleanup\" command, KoLmafia will dispose of these items either by pulverizing them (equipment) or autoselling them (non-equipment).");
    }
  }

  private static class SingletonItemsPanel extends OverlapPanel {
    public SingletonItemsPanel() {
      super(
          "closet", "help", (LockableListModel<AdventureResult>) KoLConstants.singletonList, true);
    }

    @Override
    public void actionConfirmed() {
      AdventureResult[] items = new AdventureResult[KoLConstants.singletonList.size()];
      for (int i = 0; i < KoLConstants.singletonList.size(); ++i) {
        AdventureResult current = KoLConstants.singletonList.get(i);
        int icount = current.getCount(KoLConstants.inventory);
        int ccount = current.getCount(KoLConstants.closet);
        items[i] = current.getInstance(Math.min(icount, Math.max(0, 1 - ccount)));
      }

      RequestThread.postRequest(new ClosetRequest(ClosetRequestType.INVENTORY_TO_CLOSET, items));
    }

    @Override
    public void actionCancelled() {
      InputFieldUtilities.alert(
          "These items are flagged as \"singletons\".  Using the \"closet\" button, KoLmafia will try to ensure that at least one of the item exists in your closet.\n\nIF THE PLAYER IS STILL IN HARDCORE OR RONIN, these items are treated as a special class of junk items where during the \"cleanup\" routine mentioned in the junk tab, KoLmafia will attempt to leave one of the item in the players inventory.\n\nPlease take note that once the player breaks Ronin, KoLmafia will treat these items as normal junk and ignore the general preservation rule.");
    }
  }

  private static class MementoItemsPanel extends OverlapPanel {
    public MementoItemsPanel() {
      super("closet", "help", (LockableListModel<AdventureResult>) KoLConstants.mementoList, true);
    }

    @Override
    public void actionConfirmed() {
      AdventureResult current;
      AdventureResult[] items = new AdventureResult[KoLConstants.mementoList.size()];
      for (int i = 0; i < KoLConstants.mementoList.size(); ++i) {
        current = KoLConstants.mementoList.get(i);
        items[i] = current.getInstance(current.getCount(KoLConstants.inventory));
      }

      RequestThread.postRequest(new ClosetRequest(ClosetRequestType.INVENTORY_TO_CLOSET, items));
    }

    @Override
    public void actionCancelled() {
      InputFieldUtilities.alert(
          "These items are flagged as \"mementos\".  IF YOU SET A PREFERENCE, KoLmafia will never sell or pulverize these items.");
    }
  }

  private class RestockPanel extends OverlapPanel {
    public RestockPanel() {
      super(
          "automall",
          "host sale",
          (LockableListModel<AdventureResult>) KoLConstants.profitableList,
          true);

      this.filters[4].setSelected(false);
      this.filters[4].setEnabled(false);
      this.filterItems();
    }

    @Override
    public void actionConfirmed() {
      if (!InputFieldUtilities.confirm(
          "ALL OF THE ITEMS IN THIS LIST, not just the ones you've selected, will be placed into your store.  Are you sure you wish to continue?")) {
        return;
      }

      AutoMallCommand.automall();
    }

    @Override
    public void actionCancelled() {
      int selected =
          JOptionPane.showConfirmDialog(
              ItemManageFrame.this,
              StringUtilities.basicTextWrap(
                  "KoLmafia will place all tradeable, autosellable items into your store at 999,999,999 meat. "
                      + StoreManageFrame.UNDERCUT_MESSAGE),
              "",
              JOptionPane.YES_NO_CANCEL_OPTION);

      if (selected != JOptionPane.YES_OPTION && selected != JOptionPane.NO_OPTION) {
        return;
      }

      KoLmafia.updateDisplay("Gathering data...");
      StoreManager.endOfRunSale(selected == JOptionPane.YES_OPTION);
    }
  }

  private static class HagnkStoragePanel extends InventoryPanel<AdventureResult> {
    private boolean isPullingForUse = false;
    private final EmptyStorageButton emptyButton;

    public HagnkStoragePanel(final boolean isEquipmentOnly) {
      super(
          "pull item",
          isEquipmentOnly ? "pull & equip" : "put in closet",
          (SortedListModel<AdventureResult>) KoLConstants.storage,
          isEquipmentOnly);

      this.setButtons(new ActionListener[] {});

      ActionListener mallListener = new StorageToMallListener();
      JButton mallButton = new JButton(mallListener.toString());
      mallButton.addActionListener(mallListener);

      // Disable if you are in Hardcore or Ronin, enable once you leave Ronin or free the king
      emptyButton = new EmptyStorageButton();

      this.addButtons(
          new JButton[] {
            this.confirmedButton, this.cancelledButton, mallButton, emptyButton,
          });

      this.addFilters();
      this.addMovers();

      Box box = Box.createVerticalBox();
      JLabel budget = new JLabel("Budget:");
      budget.setToolTipText(
          "Sets the number of pulls KoLmafia is allowed to use\n"
              + "to fulfill item consumption and other usage requests");
      box.add(budget);
      box.add(Box.createVerticalStrut(5));
      if (isEquipmentOnly) {
        ItemManageFrame.pullBudgetSpinner1.setHorizontalAlignment(AutoHighlightTextField.RIGHT);
        ItemManageFrame.pullBudgetSpinner1
            .getEditor()
            .setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        JComponentUtilities.setComponentSize(ItemManageFrame.pullBudgetSpinner1, 60, 27);
        box.add(ItemManageFrame.pullBudgetSpinner1);
        box.add(Box.createVerticalStrut(5));
        box.add(ItemManageFrame.pullsRemainingLabel1);
      } else {
        ItemManageFrame.pullBudgetSpinner2.setHorizontalAlignment(AutoHighlightTextField.RIGHT);
        ItemManageFrame.pullBudgetSpinner2
            .getEditor()
            .setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        JComponentUtilities.setComponentSize(ItemManageFrame.pullBudgetSpinner2, 60, 27);
        box.add(ItemManageFrame.pullBudgetSpinner2);
        box.add(Box.createVerticalStrut(5));
        box.add(ItemManageFrame.pullsRemainingLabel2);
      }

      JPanel southeastPanel = new JPanel(new BorderLayout(0, 5));
      southeastPanel.add(box, BorderLayout.CENTER);
      southeastPanel.add(this.refreshButton, BorderLayout.SOUTH);

      this.eastPanel.add(southeastPanel, BorderLayout.SOUTH);
    }

    @Override
    public void setEnabled(final boolean isEnabled) {
      if (isEnabled) {
        this.emptyButton.update();
      }
    }

    private static class EmptyStorageButton extends InvocationButton implements Listener {
      public EmptyStorageButton() {
        super("empty", StorageRequest.class, "emptyStorage");
        NamedListenerRegistry.registerNamedListener("(hardcore)", this);
        NamedListenerRegistry.registerNamedListener("(ronin)", this);
        this.update();
      }

      @Override
      public void update() {
        boolean enabled =
            !KoLCharacter.isHardcore()
                && !KoLCharacter.inRonin()
                && !KoLConstants.storage.isEmpty();
        this.setEnabled(enabled);
      }
    }

    @Override
    public void addMovers() {
      if (!this.isEquipmentOnly) {
        super.addMovers();
        if (KoLCharacter.inRonin()) {
          this.movers[3].setSelected(true);
        }
      }
    }

    @Override
    protected int getDesiredItemAmount(
        final Object item,
        final String itemName,
        final int itemCount,
        final String message,
        final int quantityType) {
      if (!this.isPullingForUse || quantityType != ItemManagePanel.TAKE_MULTIPLE) {
        return super.getDesiredItemAmount(item, itemName, itemCount, message, quantityType);
      }

      ConsumptionType consumptionType =
          ItemDatabase.getConsumptionType(((AdventureResult) item).getItemId());
      return switch (consumptionType) {
        case HAT, PANTS, SHIRT, CONTAINER, WEAPON, OFFHAND -> 1;
        default -> super.getDesiredItemAmount(item, itemName, itemCount, message, quantityType);
      };
    }

    private AdventureResult[] pullItems(final boolean isPullingForUse) {
      this.isPullingForUse = isPullingForUse;
      AdventureResult[] items = this.getDesiredItems("Pulling");

      if (items == null) {
        return null;
      }

      // Unallowed items can't be pulled
      if (KoLCharacter.isTrendy() || KoLCharacter.getRestricted()) {
        for (int i = 0; i < items.length; ++i) {
          AdventureResult item = items[i];
          String itemName = item.getName();
          if (!StandardRequest.isAllowed(RestrictedItemType.ITEMS, itemName)) {
            items[i] = null;
          }
        }
      }

      if (items.length == KoLConstants.storage.size()) {
        RequestThread.postRequest(new StorageRequest(StorageRequestType.EMPTY_STORAGE));
      } else {
        RequestThread.postRequest(
            new StorageRequest(StorageRequestType.STORAGE_TO_INVENTORY, items));
      }

      return items;
    }

    @Override
    public void actionConfirmed() {
      this.pullItems(false);
    }

    @Override
    public void actionCancelled() {
      AdventureResult[] items = this.pullItems(this.isEquipmentOnly);
      if (items == null) {
        return;
      }

      if (this.isEquipmentOnly) {
        for (AdventureResult item : items) {
          if (item != null) {
            RequestThread.postRequest(new EquipmentRequest(item));
          }
        }
      } else {
        RequestThread.postRequest(new ClosetRequest(ClosetRequestType.INVENTORY_TO_CLOSET, items));
      }
    }
  }

  private static class FreePullsPanel extends InventoryPanel<AdventureResult> {
    public FreePullsPanel() {
      super(
          "pull item",
          "put in closet",
          (SortedListModel<AdventureResult>) KoLConstants.freepulls,
          false);

      this.addFilters();
      this.addMovers();
    }

    @Override
    public void addMovers() {
      super.addMovers();
    }

    private AdventureResult[] pullItems() {
      AdventureResult[] items = this.getDesiredItems("Pulling");

      if (items == null) {
        return null;
      }

      RequestThread.postRequest(new StorageRequest(StorageRequestType.STORAGE_TO_INVENTORY, items));
      return items;
    }

    @Override
    public void actionConfirmed() {
      this.pullItems();
    }

    @Override
    public void actionCancelled() {
      AdventureResult[] items = this.pullItems();
      if (items == null) {
        return;
      }

      RequestThread.postRequest(new ClosetRequest(ClosetRequestType.INVENTORY_TO_CLOSET, items));
    }
  }

  private static class ViewOnlyPanel extends InventoryPanel<AdventureResult> {
    public ViewOnlyPanel(final LockableListModel<AdventureResult> elementModel) {
      super(elementModel);
    }
  }

  private static class PullBudgetSpinner extends AutoHighlightSpinner implements ChangeListener {
    private boolean changing = true;

    public PullBudgetSpinner() {
      super();
      this.setAlignmentX(0.0f);
      this.addChangeListener(this);
      this.changing = false;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
      if (this.changing) {
        return;
      }

      int desired = InputFieldUtilities.getValue(this, 0);
      if (desired != ConcoctionDatabase.getPullsBudgeted()) {
        this.changing = true;
        ConcoctionDatabase.setPullsBudgeted(desired);
        this.changing = false;
        RequestThread.runInParallel(new RefreshConcoctionsRunnable(desired));
      }
    }
  }

  private static class RefreshConcoctionsRunnable implements Runnable {
    private final int desired;

    public RefreshConcoctionsRunnable(final int desired) {
      this.desired = desired;
    }

    @Override
    public void run() {
      ConcoctionDatabase.refreshConcoctions();
    }
  }

  public static class PrefPopup extends JComboBox<String> implements Listener {
    private final String pref;

    public PrefPopup(String pref) {
      this(pref, "1|2|3|4|5");
    }

    public PrefPopup(String pref, String items) {
      super(items.split("\\|"));
      this.pref = pref;
      this.addActionListener(this);
      PreferenceListenerRegistry.registerPreferenceListener(pref, this);
      this.update();
    }

    @Override
    public void update() {
      this.setSelectedItem(Preferences.getString(this.pref));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      Preferences.setString(this.pref, (String) this.getSelectedItem());
    }
  }

  private static class ItemFilterPanel extends LabeledPanel {
    public ItemFilterPanel() {
      super(
          "Number of items retained by \"all but usable\" option",
          "reset to defaults",
          new Dimension(100, 20),
          new Dimension(100, 20));

      VerifiableElement[] elements = new VerifiableElement[10];
      elements[0] = new VerifiableElement("Hats: ", new PrefPopup("usableHats"));
      elements[1] = new VerifiableElement("1H Weapons: ", new PrefPopup("usable1HWeapons"));
      elements[2] = new VerifiableElement("2H Weapons: ", new PrefPopup("usable2HWeapons"));
      elements[3] = new VerifiableElement("3H Weapons: ", new PrefPopup("usable3HWeapons"));
      elements[4] = new VerifiableElement("Off-Hands: ", new PrefPopup("usableOffhands"));
      elements[5] = new VerifiableElement("Shirts: ", new PrefPopup("usableShirts"));
      elements[6] = new VerifiableElement("Pants: ", new PrefPopup("usablePants"));
      elements[7] = new VerifiableElement("1x-equip Accs.: ", new PrefPopup("usable1xAccs"));
      elements[8] = new VerifiableElement("Accessories: ", new PrefPopup("usableAccessories"));
      elements[9] = new VerifiableElement("Other Items: ", new PrefPopup("usableOther"));

      this.setContent(elements);
    }

    @Override
    public void actionConfirmed() {
      Preferences.resetToDefault("usable1HWeapons");
      Preferences.resetToDefault("usable1xAccs");
      Preferences.resetToDefault("usable2HWeapons");
      Preferences.resetToDefault("usable3HWeapons");
      Preferences.resetToDefault("usableAccessories");
      Preferences.resetToDefault("usableHats");
      Preferences.resetToDefault("usableOffhands");
      Preferences.resetToDefault("usableOther");
      Preferences.resetToDefault("usablePants");
      Preferences.resetToDefault("usableShirts");
    }

    @Override
    public void actionCancelled() {}
  }

  @Override
  public void dispose() {
    ItemManageFrame.saveHeaderStates();
    super.dispose();
  }
}
