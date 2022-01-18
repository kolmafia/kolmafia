package net.sourceforge.kolmafia.swingui.widget;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.text.JTextComponent;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AutoFilterComboBox<E> extends DisabledItemsComboBox<E> implements ListElementFilter {
  private int currentIndex = -1;
  private boolean isRecentFocus = false;

  protected String currentName;
  protected String matchString;
  public Object currentMatch;
  protected LockableListModel<E> model;

  private boolean active, strict;
  protected final JTextComponent editor;

  public AutoFilterComboBox(final LockableListModel<E> model) {
    this.setModel(model);

    this.setEditable(true);

    NameInputListener listener = new NameInputListener();

    this.addItemListener(listener);
    this.editor = (JTextComponent) this.getEditor().getEditorComponent();

    this.editor.addFocusListener(listener);
    this.editor.addKeyListener(listener);
  }

  public void setModel(final LockableListModel<E> model) {
    super.setModel(model);
    this.model = model;
    this.model.setFilter(this);
  }

  public void forceAddition() {
    if (this.currentName == null || this.currentName.length() == 0) {
      return;
    }

    this.setSelectedItem(this.currentName);
  }

  protected void update() {
    if (this.currentName == null) {
      return;
    }

    this.isRecentFocus = false;
    this.currentIndex = -1;
    this.model.setSelectedItem(null);

    this.active = true;
    this.matchString = this.currentName.toLowerCase();

    this.strict = true;
    this.model.updateFilter(false);

    if (this.model.getSize() > 0) {
      return;
    }

    this.strict = false;
    this.model.updateFilter(false);
  }

  public synchronized void findMatch(final int keyCode) {
    this.currentName = this.getEditor().getItem().toString();

    if (this.model.contains(this.currentName)) {
      this.setSelectedItem(this.currentName);
      return;
    }

    this.currentMatch = null;
    this.update();

    this.editor.setText(this.currentName);
    if (!this.isPopupVisible()) {
      this.showPopup();
    }
  }

  protected boolean isActive() {
    return this.active;
  }

  @Override
  public boolean isVisible(final Object element) {
    if (!this.active) {
      return true;
    }

    // If it's not a result, then check to see if you need to
    // filter based on its string form.

    if (this.matchString == null || this.matchString.length() == 0) {
      return true;
    }

    String elementName = element.toString().toLowerCase();
    return this.strict
        ? elementName.indexOf(this.matchString) != -1
        : StringUtilities.fuzzyMatches(elementName, this.matchString);
  }

  private class NameInputListener extends KeyAdapter implements FocusListener, ItemListener {
    @Override
    public void keyReleased(final KeyEvent e) {
      if (e.getKeyCode() == KeyEvent.VK_DOWN) {
        if (!AutoFilterComboBox.this.isRecentFocus
            && AutoFilterComboBox.this.currentIndex + 1 < AutoFilterComboBox.this.model.getSize()) {
          AutoFilterComboBox.this.currentMatch =
              AutoFilterComboBox.this.model.getElementAt(++AutoFilterComboBox.this.currentIndex);
        }

        AutoFilterComboBox.this.isRecentFocus = false;
      } else if (e.getKeyCode() == KeyEvent.VK_UP) {
        if (!AutoFilterComboBox.this.isRecentFocus
            && AutoFilterComboBox.this.model.getSize() > 0
            && AutoFilterComboBox.this.currentIndex > 0) {
          AutoFilterComboBox.this.currentMatch =
              AutoFilterComboBox.this.model.getElementAt(--AutoFilterComboBox.this.currentIndex);
        }

        AutoFilterComboBox.this.isRecentFocus = false;
      } else if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB) {
        this.focusLost(null);
      } else if (e.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
        AutoFilterComboBox.this.findMatch(e.getKeyCode());
      }
    }

    @Override
    public final void itemStateChanged(final ItemEvent e) {
      AutoFilterComboBox.this.currentMatch = AutoFilterComboBox.this.getSelectedItem();

      if (AutoFilterComboBox.this.currentMatch == null) {
        return;
      }

      AutoFilterComboBox.this.currentName = AutoFilterComboBox.this.currentMatch.toString();

      if (!AutoFilterComboBox.this.isPopupVisible()) {
        AutoFilterComboBox.this.active = false;
        AutoFilterComboBox.this.model.updateFilter(false);
      }
    }

    @Override
    public final void focusGained(final FocusEvent e) {
      AutoFilterComboBox.this.getEditor().selectAll();

      AutoFilterComboBox.this.isRecentFocus = true;
      AutoFilterComboBox.this.currentIndex = AutoFilterComboBox.this.model.getSelectedIndex();
    }

    @Override
    public final void focusLost(final FocusEvent e) {
      if (AutoFilterComboBox.this.currentMatch != null) {
        AutoFilterComboBox.this.setSelectedItem(AutoFilterComboBox.this.currentMatch);
      } else if (AutoFilterComboBox.this.currentName != null
          && AutoFilterComboBox.this.currentName.trim().length() != 0) {
        AutoFilterComboBox.this.forceAddition();
      }
    }
  }
}
