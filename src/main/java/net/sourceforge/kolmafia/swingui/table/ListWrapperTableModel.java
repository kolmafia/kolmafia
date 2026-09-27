package net.sourceforge.kolmafia.swingui.table;

import java.util.Vector;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.table.DefaultTableModel;
import net.java.dev.spellcast.utilities.LockableListModel;

public abstract class ListWrapperTableModel<E> extends DefaultTableModel
    implements ListDataListener {
  private final String[] headers;
  private final Class<?>[] types;
  private final boolean[] editable;

  protected LockableListModel<E> listModel;

  public ListWrapperTableModel(
      final String[] headers,
      final Class<?>[] types,
      final boolean[] editable,
      final LockableListModel<E> listModel) {
    super(0, headers.length);

    this.listModel = listModel;
    this.headers = headers;
    this.types = types;
    this.editable = editable;

    SwingUtilities.invokeLater(
        new Runnable() {
          @Override
          public void run() {
            for (int i = 0; i < listModel.size(); ++i) {
              ListWrapperTableModel.this.insertRow(
                  i, ListWrapperTableModel.this.constructVector(listModel.get(i)));
            }
          }
        });

    listModel.addListDataListener(this);
  }

  @Override
  public String getColumnName(final int index) {
    return index < 0 || index >= this.headers.length ? "" : this.headers[index];
  }

  @Override
  public Class<?> getColumnClass(final int column) {
    return column < 0 || column >= this.types.length ? Object.class : this.types[column];
  }

  public abstract Vector<?> constructVector(E o);

  @Override
  public boolean isCellEditable(final int row, final int column) {
    return column < 0 || column >= this.editable.length ? false : this.editable[column];
  }

  /**
   * Called whenever contents have been added to the original list; a function required by every
   * <code>ListDataListener</code>.
   *
   * @param e the <code>ListDataEvent</code> that triggered this function call
   */
  @Override
  public void intervalAdded(final ListDataEvent e) {
    SwingUtilities.invokeLater(
        () -> {
          if (e.getSource() != ListWrapperTableModel.this.listModel) {
            return;
          }

          LockableListModel<E> source = ListWrapperTableModel.this.listModel;
          int index0 = e.getIndex0();
          int index1 = e.getIndex1();

          for (int i = index0; i <= index1; ++i) {
            ListWrapperTableModel.this.insertRow(
                i, ListWrapperTableModel.this.constructVector(source.get(i)));
          }
        });
  }

  /**
   * Called whenever contents have been removed from the original list; a function required by every
   * <code>ListDataListener</code>.
   *
   * @param e the <code>ListDataEvent</code> that triggered this function call
   */
  @Override
  public void intervalRemoved(final ListDataEvent e) {
    SwingUtilities.invokeLater(
        () -> {
          if (e.getSource() != ListWrapperTableModel.this.listModel) {
            return;
          }

          int index0 = e.getIndex0();
          int index1 = e.getIndex1();

          for (int i = index1; i >= index0; --i) {
            ListWrapperTableModel.this.removeRow(i);
          }
        });
  }

  /**
   * Called whenever contents in the original list have changed; a function required by every <code>
   * ListDataListener</code>.
   *
   * @param e the <code>ListDataEvent</code> that triggered this function call
   */
  @Override
  public void contentsChanged(final ListDataEvent e) {
    SwingUtilities.invokeLater(
        () -> {
          if (e.getSource() != ListWrapperTableModel.this.listModel) {
            return;
          }

          LockableListModel<E> source = ListWrapperTableModel.this.listModel;
          int index0 = e.getIndex0();
          int index1 = e.getIndex1();

          if (index0 < 0 || index1 < 0) {
            return;
          }

          int rowCount = ListWrapperTableModel.this.getRowCount();

          for (int i = index1; i >= index0; --i) {
            if (source.size() < i) {
              ListWrapperTableModel.this.removeRow(i);
            } else if (i > rowCount) {
              ListWrapperTableModel.this.insertRow(
                  rowCount, ListWrapperTableModel.this.constructVector(source.get(i)));
            } else {
              ListWrapperTableModel.this.removeRow(i);
              ListWrapperTableModel.this.insertRow(
                  i, ListWrapperTableModel.this.constructVector(source.get(i)));
            }
          }
        });
  }
}
