package net.sourceforge.kolmafia.swingui.widget;

import java.awt.event.KeyEvent;
import net.java.dev.spellcast.utilities.LockableListModel;

public class EditableAutoFilterComboBox extends AutoFilterComboBox<String> {
  public EditableAutoFilterComboBox(final LockableListModel<String> model) {
    super(model);
  }

  @Override
  public void forceAddition() {
    if (this.currentName == null || this.currentName.length() == 0) {
      return;
    }

    if (this.currentMatch == null && !this.model.contains(this.currentName)) {
      this.model.add(this.currentName);
    }

    super.forceAddition();
  }

  @Override
  public synchronized void findMatch(final int keyCode) {
    this.currentName = this.getEditor().getItem().toString();
    int caretPosition = this.editor.getCaretPosition();

    this.currentMatch = null;
    this.update();

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
  }

  @Override
  public boolean isVisible(final Object element) {
    if (!this.isActive()) {
      return true;
    }

    // If it's not a result, then check to see if you need to
    // filter based on its string form.

    if (this.matchString == null || this.matchString.length() == 0) {
      return true;
    }

    String elementName = element.toString().toLowerCase();
    return elementName.startsWith(this.matchString);
  }
}
