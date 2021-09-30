package net.sourceforge.kolmafia.swingui.panel;

import javax.swing.ListSelectionModel;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;

public class ItemListManagePanel extends ItemManagePanel {
  public ItemListManagePanel(
      final String confirmedText,
      final String cancelledText,
      final LockableListModel elementModel,
      final boolean addFilterField,
      final boolean addRefreshButton) {
    super(
        confirmedText,
        cancelledText,
        elementModel,
        new ShowDescriptionList(elementModel),
        addFilterField,
        addRefreshButton);

    ShowDescriptionList elementList = (ShowDescriptionList) this.elementList;
    elementList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    elementList.setVisibleRowCount(8);
    if (addFilterField) {
      this.filterfield.setList(elementList);
    }
  }

  public ItemListManagePanel(
      final LockableListModel elementModel,
      final boolean addFilterField,
      final boolean addRefreshButton) {
    this(null, null, elementModel, addFilterField, addRefreshButton);
  }

  public ItemListManagePanel(
      final String confirmedText,
      final String cancelledText,
      final LockableListModel elementModel) {
    this(
        confirmedText,
        cancelledText,
        elementModel,
        true,
        ItemManagePanel.shouldAddRefreshButton(elementModel));
  }

  public ItemListManagePanel(final LockableListModel elementModel) {
    this(null, null, elementModel);
  }

  public ShowDescriptionList getElementList() {
    return (ShowDescriptionList) this.scrollComponent;
  }

  @Override
  public Object[] getSelectedValues() {
    return this.getElementList().getSelectedValuesList().toArray();
  }
}
