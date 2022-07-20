package net.sourceforge.kolmafia.swingui.widget;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;

public class PreferenceButtonGroup extends JPanel implements Listener {
  private final String pref;
  private final SmartButtonGroup group;

  public PreferenceButtonGroup(String pref, String message, String... buttons) {
    this.pref = pref;
    this.group = new SmartButtonGroup(this);

    configure();
    makeLayout(message, buttons);
  }

  private void configure() {
    this.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
    PreferenceListenerRegistry.registerPreferenceListener(pref, this);
    this.group.setActionListener(e -> Preferences.setInteger(pref, this.group.getSelectedIndex()));
  }

  private void makeLayout(String message, String... buttons) {
    JLabel label = new JLabel(message);
    this.add(label);
    for (String button : buttons) {
      this.group.add(new JRadioButton(button));
    }

    update();
  }

  public SmartButtonGroup getButtonGroup() {
    return this.group;
  }

  @Override
  public void update() {
    this.group.setSelectedIndex(Preferences.getInteger(pref));
  }

  @Override
  public Dimension getMaximumSize() {
    return this.getPreferredSize();
  }
}
