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
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.PingManager.PingTest;
import net.sourceforge.kolmafia.session.PingManager.PingTestAbort;
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
    this.queue(new PingLoginAbortPanel());

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

  private static class PingLoginAbortPanel extends JPanel implements Listener {
    int rowCount = 0;

    public PingLoginAbortPanel() {
      Border blackLine = BorderFactory.createLineBorder(Color.black);
      ;
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
      Set<PingTestAbort> aborts = PingTestAbort.load();

      if (aborts.size() == 0) {
        aborts.add(new PingTestAbort(0, 0));
      }

      this.removeAll();
      for (var abort : aborts) {
        this.addRow(abort);
      }
    }

    private void saveConfiguration() {
      Set<PingTestAbort> aborts =
          Arrays.stream(this.getComponents())
              .filter(item -> item instanceof PingTestAbortRow)
              .map(item -> ((PingTestAbortRow) item).abort)
              .filter(abort -> abort.getCount() != 0 && abort.getFactor() != 0)
              .collect(Collectors.toCollection(TreeSet::new));

      PingTestAbort.save(aborts);
    }

    private void addRow(PingTestAbort abort) {
      this.add(new PingTestAbortRow(abort));
      this.rowCount++;
    }

    private int rowIndex(PingTestAbortRow row) {
      var components = this.getComponents();
      for (int i = 0; i < components.length; ++i) {
        if (components[i] == row) {
          return i;
        }
      }
      return -1;
    }

    public void addRowAfter(PingTestAbortRow row) {
      int index = rowIndex(row);
      if (index != -1) {
        this.addRow(new PingTestAbort(0, 0));
        this.revalidate();
        this.repaint();
      }
    }

    public void removeRow(PingTestAbortRow row) {
      // Zero out the abort for this row
      row.setCount(0);
      row.setFactor(0);

      // Saving the configuration ignores zeroed aborts
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

    public void abortChanged(PingTestAbort abort) {
      if (abort.getCount() > 0 && abort.getFactor() > 0) {
        this.saveConfiguration();
      }
    }

    @Override
    public void update() {
      this.loadConfiguration();
      this.revalidate();
      this.repaint();
    }

    private class PingTestAbortRow extends JPanel implements FocusListener {
      static final ImageIcon plusIcon = JComponentUtilities.getImage("icon_plus.gif");
      static final ImageIcon minusIcon = JComponentUtilities.getImage("icon_minus.gif");

      public final PingTestAbort abort;
      private final JTextField countField;
      private final JTextField factorField;
      private final JButton plusButton;
      private final JButton minusButton;

      public PingTestAbortRow(PingTestAbort abort) {
        this.abort = abort;
        this.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));

        this.add(new JLabel("Retry if "));
        this.countField = new JTextField(String.valueOf(abort.getCount()), 2);
        this.countField.addFocusListener(this);
        this.add(this.countField);
        this.add(new JLabel(" pings exceed "));
        this.factorField = new JTextField(String.valueOf(abort.getFactor()), 2);
        this.factorField.addFocusListener(this);
        this.add(this.factorField);
        this.add(new JLabel(" times average historical ping"));
        this.plusButton = new JButton(plusIcon);
        this.plusButton.addActionListener(
            e -> {
              PingLoginAbortPanel.this.addRowAfter(this);
            });
        this.add(plusButton);
        this.minusButton = new JButton(minusIcon);
        this.minusButton.addActionListener(
            e -> {
              PingLoginAbortPanel.this.removeRow(this);
            });
        this.add(minusButton);
      }

      public void setCount(final int count) {
        this.abort.setCount(count);
        this.countField.setText(String.valueOf(count));
      }

      public void setFactor(final int factor) {
        this.abort.setFactor(factor);
        this.factorField.setText(String.valueOf(factor));
      }

      @Override
      public void focusLost(final FocusEvent e) {
        Component component = e.getComponent();
        if (component == this.countField) {
          int newValue = InputFieldUtilities.getValue(this.countField, 0);
          if (newValue != this.abort.getCount()) {
            this.abort.setCount(newValue);
            PingLoginAbortPanel.this.abortChanged(this.abort);
          }
        } else if (component == this.factorField) {
          int newValue = InputFieldUtilities.getValue(this.factorField, 0);
          if (newValue != this.abort.getFactor()) {
            this.abort.setFactor(newValue);
            PingLoginAbortPanel.this.abortChanged(this.abort);
          }
        }
      }

      @Override
      public void focusGained(final FocusEvent e) {}
    }
  }
}
