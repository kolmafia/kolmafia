package net.sourceforge.kolmafia.swingui.panel;

import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.webui.RelayLoader;

public class UseItemPanel extends InventoryPanel {
  public UseItemPanel() {
    super((SortedListModel<AdventureResult>) KoLConstants.inventory, false);
  }

  @Override
  public AutoFilterTextField getWordFilter() {
    return new UsableItemFilterField();
  }

  @Override
  public void actionConfirmed() {
    AdventureResult[] items = this.getDesiredItems("Consume");
    if (items == null) {
      return;
    }

    for (int i = 0; i < items.length; ++i) {
      RequestThread.postRequest(UseItemRequest.getInstance(items[i]));
    }
  }

  @Override
  public void actionCancelled() {
    String name;
    Object[] values = this.getSelectedValues();

    for (int i = 0; i < values.length; ++i) {
      name = ((AdventureResult) values[i]).getName();
      if (name != null) {
        RelayLoader.openSystemBrowser(
            "http://kol.coldfront.net/thekolwiki/index.php/Special:Search?search=" + name);
      }
    }
  }

  private class UsableItemFilterField extends FilterItemField {
    @Override
    public boolean isVisible(final Object element) {
      AdventureResult item = (AdventureResult) element;
      int itemId = item.getItemId();

      if (!UsableItemFilterField.this.notrade && !ItemDatabase.isTradeable(itemId)) {
        return false;
      }

      boolean filter = false;

      switch (ItemDatabase.getConsumptionType(itemId)) {
        case KoLConstants.CONSUME_EAT:
          filter = UsableItemFilterField.this.food;
          break;

        case KoLConstants.CONSUME_DRINK:
          filter = UsableItemFilterField.this.booze;
          break;

        case KoLConstants.CONSUME_USE:
        case KoLConstants.CONSUME_SPLEEN:
        case KoLConstants.MESSAGE_DISPLAY:
        case KoLConstants.INFINITE_USES:
        case KoLConstants.CONSUME_MULTIPLE:
        case KoLConstants.CONSUME_AVATAR:
        case KoLConstants.GROW_FAMILIAR:
        case KoLConstants.CONSUME_ZAP:
          filter = UsableItemFilterField.this.other;
          break;

        case KoLConstants.EQUIP_FAMILIAR:
        case KoLConstants.EQUIP_ACCESSORY:
        case KoLConstants.EQUIP_HAT:
        case KoLConstants.EQUIP_PANTS:
        case KoLConstants.EQUIP_CONTAINER:
        case KoLConstants.EQUIP_SHIRT:
        case KoLConstants.EQUIP_WEAPON:
        case KoLConstants.EQUIP_OFFHAND:
          filter = UsableItemFilterField.this.equip;
          break;

        default:
          filter =
              UsableItemFilterField.this.other
                  && ItemDatabase.getAttribute(
                      itemId,
                      ItemDatabase.ATTR_USABLE
                          | ItemDatabase.ATTR_MULTIPLE
                          | ItemDatabase.ATTR_REUSABLE
                          | ItemDatabase.ATTR_CURSE);
      }

      return filter && super.isVisible(element);
    }
  }
}
