package net.sourceforge.kolmafia.swingui.panel;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.stream.IntStream;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.moods.HPRestoreItemList;
import net.sourceforge.kolmafia.moods.MPRestoreItemList;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;

public class RestoreOptionsPanel extends JPanel implements Listener {
  private JComboBox<String> hpAutoRecoverSelect;
  private JComboBox<String> hpAutoRecoverTargetSelect;
  private JComboBox<String> hpHaltCombatSelect;
  private JCheckBox[] hpRestoreCheckbox;
  private JComboBox<String> mpAutoRecoverSelect;
  private JComboBox<String> mpAutoRecoverTargetSelect;
  private JComboBox<String> mpBalanceTriggerSelect;
  private JComboBox<String> mpBalanceSelect;
  private JCheckBox[] mpRestoreCheckbox;
  private HealthOptionsPanel healthOptionsPanel;
  private ManaOptionsPanel manaOptionsPanel;

  private boolean restoring = false;

  public RestoreOptionsPanel() {
    super(new GridLayout(1, 2, 10, 10));

    JPanel healthPanel = new JPanel();
    healthPanel.add(new HealthOptionsPanel());

    JPanel manaPanel = new JPanel();
    manaPanel.add(new ManaOptionsPanel());

    this.add(healthPanel);
    this.add(manaPanel);

    CheckboxListener listener = new CheckboxListener();
    for (JCheckBox restoreCheckbox : this.hpRestoreCheckbox) {
      restoreCheckbox.addActionListener(listener);
    }
    for (JCheckBox restoreCheckbox : this.mpRestoreCheckbox) {
      restoreCheckbox.addActionListener(listener);
    }
    PreferenceListenerRegistry.registerPreferenceListener("autoAbortThreshold", this);
    PreferenceListenerRegistry.registerPreferenceListener("hpAutoRecovery", this);
    PreferenceListenerRegistry.registerPreferenceListener("hpAutoRecoveryTarget", this);
    PreferenceListenerRegistry.registerPreferenceListener("manaBurningThreshold", this);
    PreferenceListenerRegistry.registerPreferenceListener("manaBurningTrigger", this);
    PreferenceListenerRegistry.registerPreferenceListener("mpAutoRecovery", this);
    PreferenceListenerRegistry.registerPreferenceListener("mpAutoRecoveryTarget", this);
  }

  public void updateFromPreferences() {
    this.restoreRestoreSettings();
  }

  private GenericScrollPane constructScroller(final JCheckBox[] restoreCheckbox) {
    JPanel checkboxPanel = new JPanel(new GridLayout(restoreCheckbox.length, 1));
    for (JCheckBox checkbox : restoreCheckbox) {
      checkboxPanel.add(checkbox);
    }

    return new GenericScrollPane(
        checkboxPanel,
        GenericScrollPane.VERTICAL_SCROLLBAR_NEVER,
        GenericScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
  }

  private JPanel constructLabelPair(final String label, final JComponent element1) {
    return this.constructLabelPair(label, element1, null);
  }

  private JPanel constructLabelPair(
      final String label, final JComponent element1, final JComponent element2) {
    JPanel container = new JPanel();
    container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

    if (element1 instanceof JComboBox) {
      JComponentUtilities.setComponentSize(element1, 240, 20);
    }

    if (element2 instanceof JComboBox) {
      JComponentUtilities.setComponentSize(element2, 240, 20);
    }

    JPanel labelPanel = new JPanel(new GridLayout(1, 1));
    labelPanel.add(new JLabel("<html><b>" + label + "</b></html>", JLabel.LEFT));

    container.add(labelPanel);

    if (element1 != null) {
      container.add(Box.createVerticalStrut(5));
      container.add(element1);
    }

    if (element2 != null) {
      container.add(Box.createVerticalStrut(5));
      container.add(element2);
    }

    return container;
  }

  private void saveRestoreSettings() {
    if (this.restoring) {
      return;
    }

    Preferences.setFloat("autoAbortThreshold", this.getPercentage(this.hpHaltCombatSelect));
    Preferences.setFloat("hpAutoRecovery", this.getPercentage(this.hpAutoRecoverSelect));
    Preferences.setFloat(
        "hpAutoRecoveryTarget", this.getPercentage(this.hpAutoRecoverTargetSelect));
    Preferences.setString("hpAutoRecoveryItems", this.getSettingString(this.hpRestoreCheckbox));

    Preferences.setFloat("manaBurningTrigger", this.getPercentage(this.mpBalanceTriggerSelect));
    Preferences.setFloat("manaBurningThreshold", this.getPercentage(this.mpBalanceSelect));
    Preferences.setFloat("mpAutoRecovery", this.getPercentage(this.mpAutoRecoverSelect));
    Preferences.setFloat(
        "mpAutoRecoveryTarget", this.getPercentage(this.mpAutoRecoverTargetSelect));
    Preferences.setString("mpAutoRecoveryItems", this.getSettingString(this.mpRestoreCheckbox));
  }

  private String getSettingString(final JCheckBox[] restoreCheckbox) {
    StringBuilder restoreSetting = new StringBuilder();

    for (JCheckBox checkbox : restoreCheckbox) {
      if (checkbox.isSelected()) {
        if (restoreSetting.length() != 0) {
          restoreSetting.append(';');
        }

        restoreSetting.append(checkbox.getText().toLowerCase());
      }
    }

    return restoreSetting.toString();
  }

  private float getPercentage(final JComboBox<String> option) {
    return (option.getSelectedIndex() - 1) / 20.0f;
  }

  private void restoreRestoreSettings() {
    this.restoring = true;
    this.setSelectedIndex(this.hpHaltCombatSelect, "autoAbortThreshold");
    this.setSelectedIndex(this.hpAutoRecoverSelect, "hpAutoRecovery");
    this.setSelectedIndex(this.hpAutoRecoverTargetSelect, "hpAutoRecoveryTarget");
    HPRestoreItemList.updateCheckboxes(this.hpRestoreCheckbox);
    this.setSelectedIndex(this.mpBalanceTriggerSelect, "manaBurningTrigger");
    this.setSelectedIndex(this.mpBalanceSelect, "manaBurningThreshold");
    this.setSelectedIndex(this.mpAutoRecoverSelect, "mpAutoRecovery");
    this.setSelectedIndex(this.mpAutoRecoverTargetSelect, "mpAutoRecoveryTarget");
    MPRestoreItemList.updateCheckboxes(this.mpRestoreCheckbox);
    this.restoring = false;
  }

  private void setSelectedIndex(final JComboBox<String> option, final String property) {
    int desiredIndex = (int) (Preferences.getFloat(property) * 20.0f + 1);
    option.setSelectedIndex(Math.min(Math.max(desiredIndex, 0), option.getItemCount() - 1));
  }

  private class CheckboxListener implements ActionListener {
    @Override
    public void actionPerformed(final ActionEvent e) {
      RestoreOptionsPanel.this.saveRestoreSettings();
    }
  }

  private class HealthOptionsPanel extends JPanel implements ActionListener {

    public HealthOptionsPanel() {
      RestoreOptionsPanel.this.hpHaltCombatSelect = new JComboBox<>();
      RestoreOptionsPanel.this.hpHaltCombatSelect.addItem("Stop if auto-recovery fails");
      for (int i = 0; i <= 19; ++i) {
        RestoreOptionsPanel.this.hpHaltCombatSelect.addItem("Stop if health at " + i * 5 + "%");
      }

      RestoreOptionsPanel.this.hpAutoRecoverSelect = new JComboBox<>();
      RestoreOptionsPanel.this.hpAutoRecoverSelect.addItem("Do not auto-recover health");
      for (int i = 0; i <= 19; ++i) {
        RestoreOptionsPanel.this.hpAutoRecoverSelect.addItem(
            "Auto-recover health at " + i * 5 + "%");
      }

      RestoreOptionsPanel.this.hpAutoRecoverTargetSelect = new JComboBox<>();
      RestoreOptionsPanel.this.hpAutoRecoverTargetSelect.addItem("Do not recover health");
      for (int i = 0; i <= 20; ++i) {
        RestoreOptionsPanel.this.hpAutoRecoverTargetSelect.addItem(
            "Try to recover up to " + i * 5 + "% health");
      }

      // Add the elements to the panel

      this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      this.add(
          RestoreOptionsPanel.this.constructLabelPair(
              "Stop automation: ", RestoreOptionsPanel.this.hpHaltCombatSelect));
      this.add(Box.createVerticalStrut(25 + 15));

      this.add(
          RestoreOptionsPanel.this.constructLabelPair(
              "Restore your health: ",
              RestoreOptionsPanel.this.hpAutoRecoverSelect,
              RestoreOptionsPanel.this.hpAutoRecoverTargetSelect));
      this.add(Box.createVerticalStrut(15));

      RestoreOptionsPanel.this.hpRestoreCheckbox = HPRestoreItemList.getCheckboxes();
      final JCheckBox[] hpRestoreCheckbox = RestoreOptionsPanel.this.hpRestoreCheckbox;
      // Assuming that each checkbox at index i is associated with HPRestoreItemList.CONFIGURES[i],
      // build an array of checkboxes for each restorer type

      final JCheckBox[] actionCheckboxes =
          IntStream.range(0, hpRestoreCheckbox.length)
              .filter(
                  i ->
                      HPRestoreItemList.CONFIGURES[i]
                          instanceof HPRestoreItemList.HPRestoreItemAction)
              .mapToObj(i -> hpRestoreCheckbox[i])
              .toArray(JCheckBox[]::new);
      final JCheckBox[] itemCheckboxes =
          IntStream.range(0, hpRestoreCheckbox.length)
              .filter(
                  i ->
                      HPRestoreItemList.CONFIGURES[i]
                          instanceof HPRestoreItemList.HPRestoreItemItem)
              .mapToObj(i -> hpRestoreCheckbox[i])
              .toArray(JCheckBox[]::new);
      final JCheckBox[] skillCheckboxes =
          IntStream.range(0, hpRestoreCheckbox.length)
              .filter(
                  i ->
                      HPRestoreItemList.CONFIGURES[i]
                          instanceof HPRestoreItemList.HPRestoreItemSkill)
              .mapToObj(i -> hpRestoreCheckbox[i])
              .toArray(JCheckBox[]::new);

      if (actionCheckboxes.length > 0) {
        this.add(
            RestoreOptionsPanel.this.constructLabelPair(
                "Use these restores (actions): ",
                RestoreOptionsPanel.this.constructScroller(actionCheckboxes)));
        this.add(Box.createVerticalStrut(15));
      }
      if (skillCheckboxes.length > 0) {
        this.add(
            RestoreOptionsPanel.this.constructLabelPair(
                "Use these restores (skills): ",
                RestoreOptionsPanel.this.constructScroller(skillCheckboxes)));
        this.add(Box.createVerticalStrut(15));
      }
      if (itemCheckboxes.length > 0) {
        this.add(
            RestoreOptionsPanel.this.constructLabelPair(
                "Use these restores (items): ",
                RestoreOptionsPanel.this.constructScroller(itemCheckboxes)));
        this.add(Box.createVerticalStrut(15));
      }

      RestoreOptionsPanel.this.setSelectedIndex(
          RestoreOptionsPanel.this.hpHaltCombatSelect, "autoAbortThreshold");
      RestoreOptionsPanel.this.setSelectedIndex(
          RestoreOptionsPanel.this.hpAutoRecoverSelect, "hpAutoRecovery");
      RestoreOptionsPanel.this.setSelectedIndex(
          RestoreOptionsPanel.this.hpAutoRecoverTargetSelect, "hpAutoRecoveryTarget");

      RestoreOptionsPanel.this.hpHaltCombatSelect.addActionListener(this);
      RestoreOptionsPanel.this.hpAutoRecoverSelect.addActionListener(this);
      RestoreOptionsPanel.this.hpAutoRecoverTargetSelect.addActionListener(this);

      for (JCheckBox restoreCheckbox : RestoreOptionsPanel.this.hpRestoreCheckbox) {
        restoreCheckbox.addActionListener(this);
      }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      RestoreOptionsPanel.this.saveRestoreSettings();
    }
  }

  private class ManaOptionsPanel extends JPanel implements ActionListener {
    public ManaOptionsPanel() {
      RestoreOptionsPanel.this.mpBalanceTriggerSelect = new JComboBox<>();
      RestoreOptionsPanel.this.mpBalanceTriggerSelect.addItem("Start recasting immediately");
      for (int i = 0; i <= 20; ++i) {
        RestoreOptionsPanel.this.mpBalanceTriggerSelect.addItem(
            "Start recasting at " + i * 5 + "%");
      }

      RestoreOptionsPanel.this.mpBalanceSelect = new JComboBox<>();
      RestoreOptionsPanel.this.mpBalanceSelect.addItem("Do not rebalance buffs");
      for (int i = 0; i <= 19; ++i) {
        RestoreOptionsPanel.this.mpBalanceSelect.addItem("Recast buffs down to " + i * 5 + "%");
      }

      RestoreOptionsPanel.this.mpAutoRecoverSelect = new JComboBox<>();
      RestoreOptionsPanel.this.mpAutoRecoverSelect.addItem("Do not auto-recover mana");
      for (int i = 0; i <= 19; ++i) {
        RestoreOptionsPanel.this.mpAutoRecoverSelect.addItem("Auto-recover mana at " + i * 5 + "%");
      }

      RestoreOptionsPanel.this.mpAutoRecoverTargetSelect = new JComboBox<>();
      RestoreOptionsPanel.this.mpAutoRecoverTargetSelect.addItem("Do not auto-recover mana");
      for (int i = 0; i <= 20; ++i) {
        RestoreOptionsPanel.this.mpAutoRecoverTargetSelect.addItem(
            "Try to recover up to " + i * 5 + "% mana");
      }

      // Add the elements to the panel

      this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

      this.add(
          RestoreOptionsPanel.this.constructLabelPair(
              "Mana burning: ",
              RestoreOptionsPanel.this.mpBalanceTriggerSelect,
              RestoreOptionsPanel.this.mpBalanceSelect));
      this.add(Box.createVerticalStrut(15));

      this.add(
          RestoreOptionsPanel.this.constructLabelPair(
              "Restore your mana: ",
              RestoreOptionsPanel.this.mpAutoRecoverSelect,
              RestoreOptionsPanel.this.mpAutoRecoverTargetSelect));
      this.add(Box.createVerticalStrut(15));

      RestoreOptionsPanel.this.mpRestoreCheckbox = MPRestoreItemList.getCheckboxes();
      final JCheckBox[] mpRestoreCheckbox = RestoreOptionsPanel.this.mpRestoreCheckbox;
      // Assuming that each checkbox at index i is associated with MPRestoreItemList.CONFIGURES[i],
      // build an array of checkboxes for each restorer type

      final JCheckBox[] actionCheckboxes =
          IntStream.range(0, mpRestoreCheckbox.length)
              .filter(
                  i ->
                      MPRestoreItemList.CONFIGURES[i]
                          instanceof MPRestoreItemList.MPRestoreItemAction)
              .mapToObj(i -> mpRestoreCheckbox[i])
              .toArray(JCheckBox[]::new);
      final JCheckBox[] itemCheckboxes =
          IntStream.range(0, mpRestoreCheckbox.length)
              .filter(
                  i ->
                      MPRestoreItemList.CONFIGURES[i]
                          instanceof MPRestoreItemList.MPRestoreItemItem)
              .mapToObj(i -> mpRestoreCheckbox[i])
              .toArray(JCheckBox[]::new);
      final JCheckBox[] skillCheckboxes =
          IntStream.range(0, mpRestoreCheckbox.length)
              .filter(
                  i ->
                      MPRestoreItemList.CONFIGURES[i]
                          instanceof MPRestoreItemList.MPRestoreItemSkill)
              .mapToObj(i -> mpRestoreCheckbox[i])
              .toArray(JCheckBox[]::new);

      if (actionCheckboxes.length > 0) {
        this.add(
            RestoreOptionsPanel.this.constructLabelPair(
                "Use these restores (actions): ",
                RestoreOptionsPanel.this.constructScroller(actionCheckboxes)));
        this.add(Box.createVerticalStrut(15));
      }
      if (skillCheckboxes.length > 0) {
        this.add(
            RestoreOptionsPanel.this.constructLabelPair(
                "Use these restores (skills): ",
                RestoreOptionsPanel.this.constructScroller(skillCheckboxes)));
        this.add(Box.createVerticalStrut(15));
      }
      if (itemCheckboxes.length > 0) {
        this.add(
            RestoreOptionsPanel.this.constructLabelPair(
                "Use these restores (items): ",
                RestoreOptionsPanel.this.constructScroller(itemCheckboxes)));
        this.add(Box.createVerticalStrut(15));
      }

      RestoreOptionsPanel.this.setSelectedIndex(
          RestoreOptionsPanel.this.mpBalanceTriggerSelect, "manaBurningTrigger");
      RestoreOptionsPanel.this.setSelectedIndex(
          RestoreOptionsPanel.this.mpBalanceSelect, "manaBurningThreshold");
      RestoreOptionsPanel.this.setSelectedIndex(
          RestoreOptionsPanel.this.mpAutoRecoverSelect, "mpAutoRecovery");
      RestoreOptionsPanel.this.setSelectedIndex(
          RestoreOptionsPanel.this.mpAutoRecoverTargetSelect, "mpAutoRecoveryTarget");

      RestoreOptionsPanel.this.mpBalanceTriggerSelect.addActionListener(this);
      RestoreOptionsPanel.this.mpBalanceSelect.addActionListener(this);
      RestoreOptionsPanel.this.mpAutoRecoverSelect.addActionListener(this);
      RestoreOptionsPanel.this.mpAutoRecoverTargetSelect.addActionListener(this);

      for (JCheckBox restoreCheckbox : RestoreOptionsPanel.this.mpRestoreCheckbox) {
        restoreCheckbox.addActionListener(this);
      }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      RestoreOptionsPanel.this.saveRestoreSettings();
    }
  }

  @Override
  public void update() {
    updateFromPreferences();
    this.revalidate();
    this.repaint();
  }
}
