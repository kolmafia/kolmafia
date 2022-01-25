package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.ListCellRendererFactory;

public class InventoryPanel<E> extends ItemTableManagePanel<E> {
  protected boolean isEquipmentOnly;
  private List<FilterRadioButton> equipmentFilters;

  public InventoryPanel(final LockableListModel<E> elementModel, final boolean isEquipmentOnly) {
    this(elementModel, new boolean[] {isEquipmentOnly, false});
  }

  public InventoryPanel(final LockableListModel<E> elementModel) {
    super(elementModel);

    this.getElementList().setCellRenderer(ListCellRendererFactory.getDefaultRenderer());
    this.addFilters();
  }

  public InventoryPanel(final LockableListModel<E> elementModel, final boolean[] flags) {
    super(elementModel, flags);
    this.isEquipmentOnly = flags[0];

    boolean isCloset = elementModel == KoLConstants.closet;
    ActionListener useListener =
        isEquipmentOnly ? new EquipListener(isCloset) : new ConsumeListener(isCloset);

    ArrayList<ActionListener> listeners = new ArrayList<ActionListener>();
    listeners.add(useListener);
    listeners.add(new AutoSellListener(isCloset, true));
    listeners.add(new AutoSellListener(isCloset, false));
    listeners.add(new PulverizeListener(isCloset));
    listeners.add(new PutInClosetListener(isCloset));
    listeners.add(new PutOnDisplayListener(isCloset));
    listeners.add(new GiveToClanListener(isCloset));
    if (isEquipmentOnly) listeners.add(new FamiliarFeedListener());
    this.setButtons(true, listeners.toArray(new ActionListener[listeners.size()]));

    if (this.isEquipmentOnly) {
      this.getElementList().setCellRenderer(ListCellRendererFactory.getEquipmentPowerRenderer());
    } else {
      this.getElementList().setCellRenderer(ListCellRendererFactory.getDefaultRenderer());
    }

    if (this.movers != null) {
      this.movers[2].setSelected(true);
    }
  }

  public InventoryPanel(
      final String confirmText,
      final String cancelText,
      final LockableListModel<E> model,
      final boolean isEquipmentOnly) {
    super(confirmText, cancelText, model);
    this.isEquipmentOnly = isEquipmentOnly;

    this.addFilters();

    if (this.isEquipmentOnly) {
      this.getElementList().setCellRenderer(ListCellRendererFactory.getEquipmentPowerRenderer());
    } else {
      this.getElementList().setCellRenderer(ListCellRendererFactory.getDefaultRenderer());
    }
  }

  @Override
  public void addFilters() {
    if (!this.isEquipmentOnly) {
      super.addFilters();
      return;
    }

    this.equipmentFilters =
        Arrays.asList(
            new FilterRadioButton("weapons", true),
            new FilterRadioButton("offhand"),
            new FilterRadioButton("hats"),
            new FilterRadioButton("back"),
            new FilterRadioButton("shirts"),
            new FilterRadioButton("pants"),
            new FilterRadioButton("accessories"),
            new FilterRadioButton("familiar"));

    ButtonGroup filterGroup = new ButtonGroup();
    JPanel filterPanel = new JPanel();

    for (FilterRadioButton button : this.equipmentFilters) {
      filterGroup.add(button);
      filterPanel.add(button);
    }

    this.northPanel.add(filterPanel, BorderLayout.NORTH);
    this.filterItems();
  }

  @Override
  public AutoFilterTextField<E> getWordFilter() {
    return new EquipmentFilterField();
  }

  private class FilterRadioButton extends JRadioButton {
    public FilterRadioButton(final String label) {
      this(label, false);
    }

    public FilterRadioButton(final String label, final boolean isSelected) {
      super(label, isSelected);
      InventoryPanel.this.listenToRadioButton(this);
    }
  }

  private class EquipmentFilterField extends FilterItemField {
    @Override
    public boolean isVisible(final Object element) {
      if (InventoryPanel.this.equipmentFilters == null) {
        return super.isVisible(element);
      }

      if (element instanceof AdventureResult && !((AdventureResult) element).isItem()) {
        return false;
      }

      boolean isVisibleWithFilter = true;

      if (element == null) {
        return false;
      }

      int itemId =
          element instanceof AdventureResult
              ? ((AdventureResult) element).getItemId()
              : element instanceof CreateItemRequest
                  ? ((CreateItemRequest) element).getItemId()
                  : null;

      if (itemId == -1) {
        return true;
      }

      switch (ItemDatabase.getConsumptionType(itemId)) {
        case KoLConstants.EQUIP_WEAPON:
          isVisibleWithFilter = InventoryPanel.this.equipmentFilters.get(0).isSelected();
          break;

        case KoLConstants.EQUIP_OFFHAND:
          isVisibleWithFilter = InventoryPanel.this.equipmentFilters.get(1).isSelected();
          break;

        case KoLConstants.EQUIP_HAT:
          isVisibleWithFilter = InventoryPanel.this.equipmentFilters.get(2).isSelected();
          break;

        case KoLConstants.EQUIP_CONTAINER:
          isVisibleWithFilter = InventoryPanel.this.equipmentFilters.get(3).isSelected();
          break;

        case KoLConstants.EQUIP_SHIRT:
          isVisibleWithFilter = InventoryPanel.this.equipmentFilters.get(4).isSelected();
          break;

        case KoLConstants.EQUIP_PANTS:
          isVisibleWithFilter = InventoryPanel.this.equipmentFilters.get(5).isSelected();
          break;

        case KoLConstants.EQUIP_ACCESSORY:
          isVisibleWithFilter = InventoryPanel.this.equipmentFilters.get(6).isSelected();
          break;

        case KoLConstants.EQUIP_FAMILIAR:
          isVisibleWithFilter = InventoryPanel.this.equipmentFilters.get(7).isSelected();
          break;

        default:
          return false;
      }

      return isVisibleWithFilter && super.isVisible(element);
    }
  }

  private class FamiliarFeedListener extends ThreadedListener {
    public FamiliarFeedListener() {}

    @Override
    protected void execute() {
      AdventureResult[] items = InventoryPanel.this.getDesiredItems("Feed");

      if (items == null) {
        return;
      }

      for (int i = 0; i < items.length; ++i) {
        AdventureResult item = items[i];

        RequestThread.postRequest(UseItemRequest.getInstance(KoLConstants.CONSUME_SLIME, item));
      }
    }

    @Override
    public String toString() {
      return "feed slimeling";
    }
  }
}
