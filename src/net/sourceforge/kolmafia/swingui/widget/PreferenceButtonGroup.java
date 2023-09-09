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
  private final boolean settingUsesButtons;
  private final String[] buttons;

  public PreferenceButtonGroup(String pref, String message, String... buttons) {
    this(pref, message, false, buttons);
  }

  public PreferenceButtonGroup(
      String pref, String message, boolean settingUsesButtons, String... buttons) {
    this.pref = pref;
    this.group = new SmartButtonGroup(this);
    this.settingUsesButtons = settingUsesButtons;
    this.buttons = buttons;

    configure();
    makeLayout(message, buttons);
  }

  private void configure() {
    this.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
    PreferenceListenerRegistry.registerPreferenceListener(pref, this);
    this.group.setActionListener(
        e -> {
          int index = this.group.getSelectedIndex();
          if (this.settingUsesButtons) {
            Preferences.setString(pref, buttons[index]);
          } else {
            Preferences.setInteger(pref, index);
          }
        });
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
    int index = 0;
    if (this.settingUsesButtons) {
      String button = Preferences.getString(pref);
      for (int i = 0; i < buttons.length; ++i) {
        if (button.equals(buttons[i])) {
          index = i;
          break;
        }
      }
    } else {
      index = Preferences.getInteger(pref);
    }
    this.group.setSelectedIndex(index);
  }

  @Override
  public Dimension getMaximumSize() {
    return this.getPreferredSize();
  }
}
