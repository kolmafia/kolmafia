package net.sourceforge.kolmafia.swingui.menu;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.ListCellRenderer;
import net.sourceforge.kolmafia.utilities.PartialMRUList;
import net.sourceforge.kolmafia.utilities.ScriptMRUList;

/** Shows a most-recently-used list in a combo box. */
public class MRUListComboBoxes {
  private static final Map<PartialMRUList, ComboSeparatorsRenderer> renderers =
      new IdentityHashMap<>();

  private MRUListComboBoxes() {}

  public static void updateJComboData(final ScriptMRUList list, final JComboBox<String> jcb) {
    List<String> items = list.getItems();
    if (items.isEmpty()) {
      return;
    }

    jcb.removeAllItems();
    Iterator<String> i8r = items.iterator();
    int i = 0;
    while (i8r.hasNext()) {
      String val = i8r.next();
      jcb.insertItemAt(val, i);
      i++;
    }
    jcb.setSelectedIndex(0);
  }

  /**
   * Shows a "hybrid" MRU-plus-default list, with a separator after the recent entries. Note to
   * implementers: calling this method alters the combobox's default renderer.
   */
  public static void updateJComboData(final PartialMRUList list, final JComboBox<String> jcb) {
    List<String> items = list.getComboItems();

    ComboSeparatorsRenderer renderer =
        MRUListComboBoxes.renderers.computeIfAbsent(
            list,
            l ->
                new ComboSeparatorsRenderer(new DefaultListCellRenderer()) {
                  @Override
                  protected boolean addSeparatorAfter(JList<?> jlist, Object value, int index) {
                    int mruEntries = l.getRecentCount();
                    return index == mruEntries - 1;
                  }
                });
    if (jcb.getRenderer() != renderer) {
      jcb.setRenderer(renderer);
    }
    jcb.removeAllItems();

    for (String str : items) {
      jcb.addItem(str);
    }
    jcb.setSelectedIndex(0);
  }

  /**
   * Adapted from <http://www.jroller.com/santhosh/entry/jcombobox_items_with_separators>
   *
   * <p>Last Access: 5/11/13
   *
   * <p>Distributed under GNU Lesser GPL.
   *
   * <p>Copyright (C) 2005 Santhosh Kumar T
   *
   * <p>This program is free software: you can redistribute it and/or modify it under the terms of
   * the GNU General Public License as published by the Free Software Foundation, either version 2.1
   * of the License, or (at your option) any later version. This program is distributed in the hope
   * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
   * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
   * more details. You should have received a copy of the GNU General Public License along with this
   * program. If not, see <http://www.gnu.org/licenses/>.
   *
   * @author Santhosh Kumar T <santhosh.tekuri@gmail.com>
   */
  @SuppressWarnings("JavadocLinkAsPlainText")
  abstract static class ComboSeparatorsRenderer implements ListCellRenderer<Object> {
    private final ListCellRenderer<Object> delegate;
    private final JPanel separatorPanel = new JPanel(new BorderLayout());
    private final JSeparator separator = new JSeparator();

    public ComboSeparatorsRenderer(ListCellRenderer<Object> delegate) {
      this.delegate = delegate;
    }

    @Override
    public Component getListCellRendererComponent(
        final JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      Component comp =
          delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (index != -1
          && addSeparatorAfter(
              list, value, index)) { // index==1 if renderer is used to paint current value in combo
        separatorPanel.removeAll();
        separatorPanel.add(comp, BorderLayout.CENTER);
        separatorPanel.add(separator, BorderLayout.SOUTH);
        return separatorPanel;
      } else return comp;
    }

    protected abstract boolean addSeparatorAfter(JList<?> list, Object value, int index);
  }
}
