package net.sourceforge.kolmafia.swingui.panel;

import javax.swing.ListSelectionModel;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;

public class ItemListManagePanel<E> extends ItemManagePanel<E, ShowDescriptionList<E>> {
  public ItemListManagePanel(
      final String confirmedText,
      final String cancelledText,
      final LockableListModel<E> elementModel,
      final boolean addFilterField,
      final boolean addRefreshButton) {
    super(
        confirmedText,
        cancelledText,
        elementModel,
        new ShowDescriptionList<>(elementModel),
        addFilterField,
        addRefreshButton);

    ShowDescriptionList<E> elementList = this.scrollComponent;
    elementList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    elementList.setVisibleRowCount(8);
    if (addFilterField) {
      this.filterField.setList(elementList);
    }
  }

  public ItemListManagePanel(
      final LockableListModel<E> elementModel,
      final boolean addFilterField,
      final boolean addRefreshButton) {
    this(null, null, elementModel, addFilterField, addRefreshButton);
  }

  public ItemListManagePanel(
      final String confirmedText,
      final String cancelledText,
      final LockableListModel<E> elementModel) {
    this(
        confirmedText,
        cancelledText,
        elementModel,
        true,
        ItemManagePanel.shouldAddRefreshButton(elementModel));
  }

  public ItemListManagePanel(final LockableListModel<E> elementModel) {
    this(null, null, elementModel);
  }

  public ShowDescriptionList<E> getElementList() {
    return this.scrollComponent;
  }

  @Override
  public Object[] getSelectedValues() {
    return this.getElementList().getSelectedValuesList().toArray();
  }
}
