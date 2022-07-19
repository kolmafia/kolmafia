package net.sourceforge.kolmafia.swingui.widget;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import net.sourceforge.kolmafia.KoLGUIConstants;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;

public class PreferenceCheckBox extends JPanel implements Listener {
  private final String pref;
  private final String tooltip;

  private final JCheckBox box = new JCheckBox();

  public PreferenceCheckBox(String pref, String message) {
    this(pref, message, null);
  }

  public PreferenceCheckBox(String pref, String message, String tip) {
    this.pref = pref;
    this.tooltip = tip;

    configure();
    makeLayout(message);
  }

  private void configure() {
    this.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
    PreferenceListenerRegistry.registerPreferenceListener(pref, this);
    this.addActionListener(e -> Preferences.setBoolean(pref, box.isSelected()));
  }

  public void addActionListener(ActionListener a) {
    this.box.addActionListener(a);
  }

  private void makeLayout(String message) {
    this.add(this.box);
    JLabel label = new JLabel(message);
    this.add(label);

    if (tooltip != null) {
      this.addToolTip(tooltip);
    }

    update();
  }

  public JCheckBox getCheckBox() {
    return this.box;
  }

  @Override
  public void update() {
    this.box.setSelected(Preferences.getBoolean(this.pref));
  }

  @Override
  public Dimension getMaximumSize() {
    return this.getPreferredSize();
  }

  private void addToolTip(String tooltip) {
    this.add(Box.createHorizontalStrut(3));
    JLabel label = new JLabel("[");
    label.setFont(KoLGUIConstants.DEFAULT_FONT);
    this.add(label);

    label = new JLabel("<html><u>?</u></html>");
    this.add(label);
    label.setForeground(Color.blue.darker());
    label.setFont(KoLGUIConstants.DEFAULT_FONT);
    label.setCursor(new Cursor(Cursor.HAND_CURSOR));
    label.setToolTipText(tooltip);

    // show the tooltip with no delay, don't dismiss while hovered
    ToolTipManager.sharedInstance().registerComponent(label);
    ToolTipManager.sharedInstance().setInitialDelay(0);
    ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

    label = new JLabel("]");
    label.setFont(KoLGUIConstants.DEFAULT_FONT);
    this.add(label);
  }
}
