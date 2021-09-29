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

public class AutoFilterComboBox extends DisabledItemsComboBox implements ListElementFilter {
  private int currentIndex = -1;
  private boolean isRecentFocus = false;

  private String currentName;
  private String matchString;
  public Object currentMatch;
  private LockableListModel model;
  private final boolean allowAdditions;

  private boolean active, strict;
  private final JTextComponent editor;

  public AutoFilterComboBox(final LockableListModel model, final boolean allowAdditions) {
    this.setModel(model);

    this.setEditable(true);

    this.allowAdditions = allowAdditions;
    NameInputListener listener = new NameInputListener();

    this.addItemListener(listener);
    this.editor = (JTextComponent) this.getEditor().getEditorComponent();

    this.editor.addFocusListener(listener);
    this.editor.addKeyListener(listener);
  }

  public String getText() {
    return (String) (this.getSelectedItem() != null ? this.getSelectedItem() : this.currentMatch);
  }

  public void setText(final String text) {
    if (this.model.contains(text)) {
      this.setSelectedItem(text);
    } else {
      this.setSelectedItem(null);
      this.currentMatch = text;
      this.editor.setText(text);
    }
  }

  public void setModel(final LockableListModel model) {
    super.setModel(model);
    this.model = model;
    this.model.setFilter(this);
  }

  public void forceAddition() {
    if (this.currentName == null || this.currentName.length() == 0) {
      return;
    }

    if (this.currentMatch == null
        && this.allowAdditions
        && !this.model.contains(this.currentName)) {
      this.model.add(this.currentName);
    }

    this.setSelectedItem(this.currentName);
  }

  private void update() {
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
    int caretPosition = this.editor.getCaretPosition();

    if (!this.allowAdditions && this.model.contains(this.currentName)) {
      this.setSelectedItem(this.currentName);
      return;
    }

    this.currentMatch = null;
    this.update();

    if (this.allowAdditions) {
      if (this.model.getSize() != 1
          || keyCode == KeyEvent.VK_BACK_SPACE
          || keyCode == KeyEvent.VK_DELETE) {
        this.editor.setText(this.currentName);
        this.editor.setCaretPosition(caretPosition);
        return;
      }

      this.currentMatch = this.model.getElementAt(0);
      this.matchString = this.currentMatch.toString().toLowerCase();

      this.editor.setText(this.currentMatch.toString());
      this.editor.moveCaretPosition(caretPosition);
      return;
    }

    this.editor.setText(this.currentName);
    if (!this.isPopupVisible()) {
      this.showPopup();
    }
  }

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
    return this.allowAdditions
        ? elementName.startsWith(this.matchString)
        : this.strict
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

    public final void focusGained(final FocusEvent e) {
      AutoFilterComboBox.this.getEditor().selectAll();

      AutoFilterComboBox.this.isRecentFocus = true;
      AutoFilterComboBox.this.currentIndex = AutoFilterComboBox.this.model.getSelectedIndex();
    }

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
