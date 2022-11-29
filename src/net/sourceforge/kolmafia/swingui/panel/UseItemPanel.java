package net.sourceforge.kolmafia.swingui.panel;

import java.util.EnumSet;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase.Attribute;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.webui.RelayLoader;

public class UseItemPanel extends InventoryPanel<AdventureResult> {
  public UseItemPanel() {
    super((SortedListModel<AdventureResult>) KoLConstants.inventory, false);
  }

  @Override
  public AutoFilterTextField<AdventureResult> getWordFilter() {
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

    for (final AdventureResult value : this.getSelectedValues()) {
      name = value.getName();
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
        case EAT:
          filter = UsableItemFilterField.this.food;
          break;

        case DRINK:
          filter = UsableItemFilterField.this.booze;
          break;

        case USE:
        case SPLEEN:
        case USE_MESSAGE_DISPLAY:
        case USE_INFINITE:
        case USE_MULTIPLE:
        case AVATAR_POTION:
        case FAMILIAR_HATCHLING:
        case ZAP:
          filter = UsableItemFilterField.this.other;
          break;

        case FAMILIAR_EQUIPMENT:
        case ACCESSORY:
        case HAT:
        case PANTS:
        case CONTAINER:
        case SHIRT:
        case WEAPON:
        case OFFHAND:
          filter = UsableItemFilterField.this.equip;
          break;

        default:
          filter =
              UsableItemFilterField.this.other
                  && ItemDatabase.getAttribute(
                      itemId,
                      EnumSet.of(
                          Attribute.USABLE,
                          Attribute.MULTIPLE,
                          Attribute.REUSABLE,
                          Attribute.CURSE));
      }

      return filter && super.isVisible(element);
    }
  }
}
