package net.sourceforge.kolmafia.swingui.panel;

import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.RestoresDatabase;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.webui.RelayLoader;

public class RestorativeItemPanel extends ItemTableManagePanel {
  public RestorativeItemPanel() {
    super(
        "use item",
        "check wiki",
        (SortedListModel<AdventureResult>) KoLConstants.inventory,
        new boolean[] {false, true});
    this.filterItems();
  }

  @Override
  public AutoFilterTextField getWordFilter() {
    return new RestorativeItemFilterField();
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

  private class RestorativeItemFilterField extends FilterItemField {
    @Override
    public boolean isVisible(final Object element) {
      AdventureResult item = (AdventureResult) element;
      int itemId = item.getItemId();

      if (RestoresDatabase.isRestore(itemId)) {
        if (KoLCharacter.inBeecore() && ItemDatabase.unusableInBeecore(itemId)) {
          return false;
        }
        if (KoLCharacter.inGLover() && ItemDatabase.unusableInGLover(itemId)) {
          return false;
        }

        String itemName = item.getName();
        if (RestoresDatabase.getUsesLeft(itemName) == 0) {
          return false;
        }
        if (RestoresDatabase.getHPRange(itemName).length() == 0
            && RestoresDatabase.getMPRange(itemName).length() == 0) {
          return false;
        }
        return super.isVisible(element);
      }
      return false;
    }
  }
}
