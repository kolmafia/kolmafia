package net.sourceforge.kolmafia.swingui.panel;

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
    this.queue(
        new PreferenceCheckBox("pingLogin", "Run ping test at login to measure connection lag"));
    this.queue(
        new PreferenceButtonGroup(
            "pingLoginCheck", "Login ping check type: ", true, "none", "goal", "threshold"));
    this.queue(new PreferenceIntegerTextField("pingLoginGoal", 0, "Maximum average measured lag"));
    this.queue(
        new PreferenceFloatTextField(
            "pingLoginThreshold", 0, "Allowed threshold above minimum historical lag"));

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
      message.append("Observed average ping times range from ");
      message.append(String.valueOf(shortest.getAverage()));
      message.append("-");
      message.append(String.valueOf(longest.getAverage()));
      message.append(" msec.");
      this.setText(message.toString());
    }
  }
}
