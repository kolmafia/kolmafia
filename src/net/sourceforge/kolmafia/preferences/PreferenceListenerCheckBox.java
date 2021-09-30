package net.sourceforge.kolmafia.preferences;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;

public class PreferenceListenerCheckBox extends JCheckBox implements ActionListener, Listener {
  private final String property;

  public PreferenceListenerCheckBox(String property) {
    this("", property);
  }

  public PreferenceListenerCheckBox(String label, String property) {
    super(label);

    this.property = property;
    PreferenceListenerRegistry.registerPreferenceListener(property, this);

    this.update();
    this.addActionListener(this);
  }

  public void update() {
    boolean isTrue = Preferences.getBoolean(this.property);

    this.setSelected(isTrue);
  }

  public void actionPerformed(final ActionEvent e) {
    if (Preferences.getBoolean(this.property) == this.isSelected()) {
      return;
    }

    Preferences.setBoolean(this.property, this.isSelected());
    this.handleClick();
  }

  protected void handleClick() {}
}
