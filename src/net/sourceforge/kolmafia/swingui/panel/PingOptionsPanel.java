package net.sourceforge.kolmafia.swingui.panel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.PingManager.PingAbortTrigger;
import net.sourceforge.kolmafia.session.PingManager.PingTest;
import net.sourceforge.kolmafia.swingui.widget.PreferenceButtonGroup;
import net.sourceforge.kolmafia.swingui.widget.PreferenceCheckBox;
import net.sourceforge.kolmafia.swingui.widget.PreferenceFloatTextField;
import net.sourceforge.kolmafia.swingui.widget.PreferenceIntegerTextField;
import net.sourceforge.kolmafia.swingui.widget.PreferenceTextArea;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

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
        new PreferenceButtonGroup(
            "pingLoginFail",
            "Action after ping check failure: ",
            true,
            "login",
            "logout",
            "confirm"));
    this.queue(new PingAbortTriggerPanel());

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
      message.append(KoLConstants.FLOAT_FORMAT.format(shortest.getAverage()));
      message.append("-");
      message.append(KoLConstants.FLOAT_FORMAT.format(longest.getAverage()));
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

  private static class PingAbortTriggerPanel extends JPanel implements Listener {
    int rowCount = 0;

    public PingAbortTriggerPanel() {
      Border blackLine = BorderFactory.createLineBorder(Color.black);
      this.setBorder(blackLine);
      this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
      this.loadConfiguration();

      PreferenceListenerRegistry.registerPreferenceListener("pingLoginAbort", this);
    }

    @Override
    public Dimension getMaximumSize() {
      return this.getPreferredSize();
    }

    private void loadConfiguration() {
      Set<PingAbortTrigger> triggers = PingAbortTrigger.load();

      if (triggers.size() == 0) {
        triggers.add(new PingAbortTrigger(0, 0));
      }

      this.removeAll();
      for (var trigger : triggers) {
        this.addRow(trigger);
      }
    }

    private void saveConfiguration() {
      Set<PingAbortTrigger> triggers =
          Arrays.stream(this.getComponents())
              .filter(item -> item instanceof PingAbortTriggerRow)
              .map(item -> ((PingAbortTriggerRow) item).trigger)
              .filter(trigger -> trigger.getCount() != 0 && trigger.getFactor() != 0)
              .collect(Collectors.toCollection(TreeSet::new));

      PingAbortTrigger.save(triggers);
    }

    private void addRow(PingAbortTrigger trigger) {
      this.add(new PingAbortTriggerRow(trigger));
      this.rowCount++;
    }

    private int rowIndex(PingAbortTriggerRow row) {
      var components = this.getComponents();
      for (int i = 0; i < components.length; ++i) {
        if (components[i] == row) {
          return i;
        }
      }
      return -1;
    }

    public void addRowAfter(PingAbortTriggerRow row) {
      int index = rowIndex(row);
      if (index != -1) {
        this.addRow(new PingAbortTrigger(0, 0));
        this.revalidate();
        this.repaint();
      }
    }

    public void removeRow(PingAbortTriggerRow row) {
      // Zero out the trigger for this row
      row.setCount(0);
      row.setFactor(0);

      // Saving the configuration ignores zeroed triggers
      this.saveConfiguration();

      // Do not remove the last row.
      if (rowCount == 1) {
        return;
      }

      // Find the row in the JPanel
      int index = rowIndex(row);
      if (index == -1) {
        // This should not happen
        return;
      }

      // Remove the row and repaint
      this.remove(index);
      this.rowCount--;
      this.revalidate();
      this.repaint();
    }

    public void triggerChanged(PingAbortTrigger trigger) {
      if (trigger.getCount() > 0 && trigger.getFactor() > 0) {
        this.saveConfiguration();
      }
    }

    @Override
    public void update() {
      this.loadConfiguration();
      this.revalidate();
      this.repaint();
    }

    private class PingAbortTriggerRow extends JPanel implements FocusListener {
      static final ImageIcon plusIcon = JComponentUtilities.getImage("icon_plus.gif");
      static final ImageIcon minusIcon = JComponentUtilities.getImage("icon_minus.gif");

      public final PingAbortTrigger trigger;
      private final JTextField countField;
      private final JLabel countLabel;
      private final JTextField factorField;
      private final JButton plusButton;
      private final JButton minusButton;

      public PingAbortTriggerRow(PingAbortTrigger trigger) {
        this.trigger = trigger;
        this.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));

        this.add(new JLabel("Retry if "));
        this.countField = new JTextField("0", 2);
        this.countField.addFocusListener(this);
        this.add(this.countField);
        this.countLabel = new JLabel("");
        this.add(countLabel);
        this.setCount(trigger.getCount());
        this.factorField = new JTextField(String.valueOf(trigger.getFactor()), 2);
        this.factorField.addFocusListener(this);
        this.add(this.factorField);
        this.add(new JLabel(" times average historical ping"));
        this.plusButton = new JButton(plusIcon);
        this.plusButton.addActionListener(
            e -> {
              PingAbortTriggerPanel.this.addRowAfter(this);
            });
        this.add(plusButton);
        this.minusButton = new JButton(minusIcon);
        this.minusButton.addActionListener(
            e -> {
              PingAbortTriggerPanel.this.removeRow(this);
            });
        this.add(minusButton);
      }

      public void setCount(final int count) {
        this.trigger.setCount(count);
        this.countField.setText(String.valueOf(count));
        this.countLabel.setText(count == 1 ? " ping exceeds " : " pings exceed ");
      }

      public void setFactor(final int factor) {
        this.trigger.setFactor(factor);
        this.factorField.setText(String.valueOf(factor));
      }

      @Override
      public void focusLost(final FocusEvent e) {
        Component component = e.getComponent();
        if (component == this.countField) {
          int newValue = InputFieldUtilities.getValue(this.countField, 0);
          if (newValue != this.trigger.getCount()) {
            this.trigger.setCount(newValue);
            PingAbortTriggerPanel.this.triggerChanged(this.trigger);
          }
        } else if (component == this.factorField) {
          int newValue = InputFieldUtilities.getValue(this.factorField, 0);
          if (newValue != this.trigger.getFactor()) {
            this.trigger.setFactor(newValue);
            PingAbortTriggerPanel.this.triggerChanged(this.trigger);
          }
        }
      }

      @Override
      public void focusGained(final FocusEvent e) {}
    }
  }
}
