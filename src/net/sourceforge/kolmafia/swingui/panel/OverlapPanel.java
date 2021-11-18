package net.sourceforge.kolmafia.swingui.panel;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.ListCellRendererFactory;

public class OverlapPanel extends ItemListManagePanel<AdventureResult> {
  private final boolean isOverlap;
  private final LockableListModel<AdventureResult> overlapModel;

  public OverlapPanel(
      final String confirmText,
      final String cancelText,
      final LockableListModel<AdventureResult> overlapModel,
      final boolean isOverlap) {
    super(
        confirmText,
        cancelText,
        isOverlap ? overlapModel : (SortedListModel<AdventureResult>) KoLConstants.inventory,
        true,
        false);
    this.overlapModel = overlapModel;
    this.isOverlap = isOverlap;

    if (this.isOverlap) {
      this.getElementList().setCellRenderer(ListCellRendererFactory.getNameOnlyRenderer());
    }

    this.getElementList().addKeyListener(new OverlapAdapter());
    this.addFilters();
  }

  @Override
  public AutoFilterTextField getWordFilter() {
    return new OverlapFilterField();
  }

  private class OverlapFilterField extends FilterItemField {
    @Override
    public boolean isVisible(final Object element) {
      return super.isVisible(element)
          && (OverlapPanel.this.isOverlap
              ? KoLConstants.inventory.contains(element)
              : !OverlapPanel.this.overlapModel.contains(element));
    }
  }

  private class OverlapAdapter extends KeyAdapter {
    @Override
    public void keyReleased(final KeyEvent e) {
      if (e.isConsumed()) {
        return;
      }

      if (e.getKeyCode() != KeyEvent.VK_DELETE && e.getKeyCode() != KeyEvent.VK_BACK_SPACE) {
        return;
      }

      Object[] items = OverlapPanel.this.getSelectedValues();
      OverlapPanel.this.getElementList().clearSelection();

      for (int i = 0; i < items.length; ++i) {
        OverlapPanel.this.overlapModel.remove(items[i]);
        if (OverlapPanel.this.overlapModel == KoLConstants.singletonList) {
          KoLConstants.junkList.remove(items[i]);
        }
      }

      OverlapPanel.this.filterItems();
      e.consume();
    }
  }
}
