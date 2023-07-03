package net.sourceforge.kolmafia.swingui.widget;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import net.sourceforge.kolmafia.KoLGUIConstants;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class PreferenceFloatTextField extends JPanel implements Listener, FocusListener {
  private final String pref;
  private final JTextField field;
  private final String tooltip;

  private final JCheckBox box = new JCheckBox();

  public PreferenceFloatTextField(String pref, int size, String message) {
    this(pref, size, message, null);
  }

  public PreferenceFloatTextField(String pref, int size, String message, String tip) {
    this.pref = pref;
    this.tooltip = tip;

    this.field = new JTextField(size);
    this.field.addFocusListener(this);

    configure();
    makeLayout(message);
  }

  private void configure() {
    this.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
    PreferenceListenerRegistry.registerPreferenceListener(pref, this);
  }

  private void makeLayout(String message) {
    this.add(this.field);
    JLabel label = new JLabel(message, SwingConstants.LEFT);
    label.setLabelFor(this.field);
    label.setVerticalAlignment(SwingConstants.TOP);
    this.add(label);

    if (tooltip != null) {
      this.addToolTip(tooltip);
    }

    update();
  }

  public JTextField getTextField() {
    return this.field;
  }

  @Override
  public void update() {
    this.actionCancelled();
  }

  @Override
  public Dimension getMaximumSize() {
    return this.getPreferredSize();
  }

  public void actionConfirmed() {
    Preferences.setDouble(this.pref, InputFieldUtilities.getValue(this.field, 0.0));
  }

  public void actionCancelled() {
    this.field.setText(String.valueOf(Preferences.getDouble(this.pref)));
  }

  @Override
  public void focusLost(final FocusEvent e) {
    this.actionConfirmed();
  }

  @Override
  public void focusGained(final FocusEvent e) {}

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
