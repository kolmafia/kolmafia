package net.sourceforge.kolmafia.swingui.panel;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.PingManager.PingTest;
import net.sourceforge.kolmafia.swingui.widget.PreferenceButtonGroup;
import net.sourceforge.kolmafia.swingui.widget.PreferenceCheckBox;
import net.sourceforge.kolmafia.swingui.widget.PreferenceFloatTextField;
import net.sourceforge.kolmafia.swingui.widget.PreferenceIntegerTextField;
import net.sourceforge.kolmafia.swingui.widget.PreferenceTextArea;

public class PingOptionsPanel extends ConfigQueueingPanel {

  public PingOptionsPanel() {
    super();

    this.queue(new PingHistory());
    this.queue(new PingResetButton());
    this.queue(this.newSeparator());
    this.queue(
        new PreferenceCheckBox("pingLogin", "Run ping test at login to measure connection lag"));
    this.queue(
        new PreferenceButtonGroup(
            "pingDefaultTestPage",
            "KoL page to ping: ",
            true,
            "api",
            "(events)",
            "(status)",
            "council",
            "main"));
    this.queue(
        new PreferenceIntegerTextField(
            "pingDefaultTestPings", 4, "How many times to ping that page."));
    this.queue(
        new PreferenceButtonGroup(
            "pingLoginCheck", "Login ping check type: ", true, "none", "goal", "threshold"));
    this.queue(new PreferenceIntegerTextField("pingLoginGoal", 4, "Maximum average measured lag"));
    this.queue(
        new PreferenceFloatTextField(
            "pingLoginThreshold", 4, "Allowed threshold above minimum historical lag"));
    this.queue(
        new PreferenceIntegerTextField(
            "pingLoginCount", 4, "Attempted ping checks before giving up"));
    this.queue(
        new PreferenceCheckBox(
            "pingStealthyTimein", "When timing in to reconnect, use stealth mode (/q)"));
    this.queue(
        new PreferenceButtonGroup(
            "pingLoginFail",
            "Action after ping check failure: ",
            true,
            "login",
            "logout",
            "confirm"));

    this.makeLayout();
  }

  private static class PingHistory extends PreferenceTextArea {
    public PingHistory() {
      super("pingShortest", "pingLongest");
      this.update();
    }

    @Override
    public void update() {
      PingTest shortest = PingTest.parseProperty("pingShortest");
      PingTest longest = PingTest.parseProperty("pingLongest");
      StringBuilder message = new StringBuilder();
      message.append("Observed average ping times to ");
      message.append(shortest.getPage());
      message.append(" range from ");
      message.append(String.valueOf(Math.round(shortest.getAverage())));
      message.append("-");
      message.append(String.valueOf(Math.round(longest.getAverage())));
      message.append(" msec.");
      this.setText(message.toString());
    }
  }

  private static class PingResetButton extends JPanel {
    private final JButton button = new JButton("Clear");

    public PingResetButton() {
      configure();
      makeLayout();
    }

    private void configure() {
      this.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
      this.addActionListener(
          e -> {
            Preferences.setString("pingLongest", "");
            Preferences.setString("pingShortest", "");
          });
    }

    public void addActionListener(ActionListener a) {
      this.button.addActionListener(a);
    }

    private void makeLayout() {
      JLabel label = new JLabel("Reset historical ping times:");
      this.add(label);
      this.add(this.button);
    }

    @Override
    public Dimension getMaximumSize() {
      return this.getPreferredSize();
    }
  }
}
