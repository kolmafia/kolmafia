package net.sourceforge.kolmafia.swingui.widget;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.JTextField;
import net.sourceforge.kolmafia.preferences.Preferences;

public class AutoHighlightTextField extends JTextField implements FocusListener {
  public AutoHighlightTextField() {
    super();
    this.addFocusListener(this);
  }

  public AutoHighlightTextField(final String text) {
    super(text);
    this.addFocusListener(this);
  }

  @Override
  public void setText(final String text) {
    super.setText(text);
    this.selectAll();
  }

  @Override
  public void focusGained(final FocusEvent e) {
    this.selectAll();
  }

  @Override
  public void focusLost(final FocusEvent e) {}

  @Override
  public void selectAll() {
    if (Preferences.getBoolean("autoHighlightOnFocus")) {
      super.selectAll();
    }
  }
}
