package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.moods.Mood;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.moods.MoodTrigger;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.swingui.button.ThreadedButton;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterComboBox;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class MoodOptionsPanel extends JPanel {
  protected JList<MoodTrigger> moodList;

  public MoodOptionsPanel() {
    super(new BorderLayout());

    this.add(new MoodTriggerListPanel(), BorderLayout.CENTER);

    AddTriggerPanel triggers = new AddTriggerPanel();
    this.moodList.addListSelectionListener(triggers);
    this.add(triggers, BorderLayout.NORTH);
  }

  private class MoodTriggerListPanel extends ScrollablePanel<JList<MoodTrigger>> {
    public JComboBox<Mood> availableMoods;

    public MoodTriggerListPanel() {
      super("", new ShowDescriptionList<>(MoodManager.getTriggers()));

      this.availableMoods = new MoodComboBox();

      this.centerPanel.add(this.availableMoods, BorderLayout.NORTH);
      MoodOptionsPanel.this.moodList = this.scrollComponent;

      JPanel extraButtons = new JPanel(new GridLayout(4, 1, 5, 5));

      extraButtons.add(new ThreadedButton("new list", new NewMoodRunnable()));
      extraButtons.add(new ThreadedButton("delete list", new DeleteMoodRunnable()));
      extraButtons.add(new ThreadedButton("copy list", new CopyMoodRunnable()));
      extraButtons.add(new ThreadedButton("execute", new ExecuteRunnable()));

      JPanel buttonHolder = new JPanel(new BorderLayout());
      buttonHolder.add(extraButtons, BorderLayout.NORTH);

      this.actualPanel.add(buttonHolder, BorderLayout.EAST);
    }

    @Override
    public void actionConfirmed() {}

    @Override
    public void actionCancelled() {}

    @Override
    public void setEnabled(final boolean isEnabled) {}

    private class MoodComboBox extends JComboBox<Mood> {
      public MoodComboBox() {
        super(MoodManager.getAvailableMoods());

        MoodManager.updateFromPreferences();

        this.addActionListener(new MoodComboBoxListener());
      }

      public class MoodComboBoxListener implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent e) {
          Mood mood = (Mood) MoodComboBox.this.getSelectedItem();
          if (mood != null) {
            MoodManager.setMood(mood.toString());
          }
        }
      }
    }

    private class NewMoodRunnable implements Runnable {
      @Override
      public void run() {
        String name = InputFieldUtilities.input("Give your list a name!");
        if (name == null) {
          return;
        }

        MoodManager.setMood(name);
        MoodManager.saveSettings();
      }
    }

    private class DeleteMoodRunnable implements Runnable {
      @Override
      public void run() {
        MoodManager.deleteCurrentMood();
        MoodManager.saveSettings();
      }
    }

    private class CopyMoodRunnable implements Runnable {
      @Override
      public void run() {
        String moodName = InputFieldUtilities.input("Make a copy of current mood list called:");
        if (moodName == null) {
          return;
        }

        if (moodName.equals("default")) {
          return;
        }

        MoodManager.copyTriggers(moodName);
        MoodManager.setMood(moodName);
        MoodManager.saveSettings();
      }
    }

    private class ExecuteRunnable implements Runnable {
      @Override
      public void run() {
        KoLmafiaCLI.DEFAULT_SHELL.executeLine("mood execute");
      }
    }
  }

  public class AddTriggerPanel extends GenericPanel implements ListSelectionListener {
    public final LockableListModel<String> EMPTY_MODEL = new LockableListModel<>();
    public final LockableListModel<String> EFFECT_MODEL = new LockableListModel<>();

    public TypeComboBox typeSelect;
    public ValueComboBox valueSelect;
    public JTextField commandField;

    public AddTriggerPanel() {
      super("add entry", "auto-fill");

      this.typeSelect = new TypeComboBox();

      Object[] names = EffectDatabase.values().toArray();

      for (int i = 0; i < names.length; ++i) {
        this.EFFECT_MODEL.add(names[i].toString());
      }

      this.EFFECT_MODEL.sort();

      this.valueSelect = new ValueComboBox();
      this.commandField = new JTextField();

      VerifiableElement[] elements = new VerifiableElement[3];
      elements[0] = new VerifiableElement("Trigger On: ", this.typeSelect);
      elements[1] = new VerifiableElement("Check For: ", this.valueSelect);
      elements[2] = new VerifiableElement("Command: ", this.commandField);

      this.setContent(elements);
    }

    @Override
    public void valueChanged(final ListSelectionEvent e) {
      MoodTrigger selected = MoodOptionsPanel.this.moodList.getSelectedValue();
      if (selected == null) {
        return;
      }

      MoodTrigger node = selected;
      String type = node.getType();

      // Update the selected type

      switch (type) {
        case "lose_effect" -> this.typeSelect.setSelectedIndex(0);
        case "gain_effect" -> this.typeSelect.setSelectedIndex(1);
        case "unconditional" -> this.typeSelect.setSelectedIndex(2);
      }

      // Update the selected effect

      this.valueSelect.setSelectedItem(node.getName());
      this.commandField.setText(node.getAction());
    }

    @Override
    public void actionConfirmed() {
      String currentMood = Preferences.getString("currentMood");
      if (currentMood.equals("apathetic")) {
        InputFieldUtilities.alert("You cannot add triggers to an apathetic mood.");
        return;
      }

      MoodManager.addTrigger(
          this.typeSelect.getSelectedType(),
          (String) this.valueSelect.getSelectedItem(),
          this.commandField.getText());
      MoodManager.saveSettings();
    }

    @Override
    public void actionCancelled() {
      String[] autoFillTypes =
          new String[] {"minimal set (current active buffs)", "maximal set (all castable buffs)"};
      String desiredType =
          InputFieldUtilities.input("Which kind of buff set would you like to use?", autoFillTypes);

      if (desiredType == null) {
        return;
      }

      if (desiredType.equals(autoFillTypes[0])) {
        MoodManager.minimalSet();
      } else {
        MoodManager.maximalSet();
      }

      MoodManager.saveSettings();
    }

    @Override
    public void setEnabled(final boolean isEnabled) {}

    @Override
    public void addStatusLabel() {}

    private class ValueComboBox extends AutoFilterComboBox<String> {
      public ValueComboBox() {
        super(AddTriggerPanel.this.EFFECT_MODEL);
      }

      @Override
      public void setSelectedItem(final Object anObject) {
        AddTriggerPanel.this.commandField.setText(
            MoodManager.getDefaultAction(
                AddTriggerPanel.this.typeSelect.getSelectedType(), (String) anObject));
        super.setSelectedItem(anObject);
      }
    }

    private class TypeComboBox extends JComboBox<String> {
      public TypeComboBox() {
        this.addItem("When an effect is lost");
        this.addItem("When an effect is gained");
        this.addItem("Unconditional trigger");

        this.addActionListener(new TypeComboBoxListener());
      }

      public String getSelectedType() {
        return switch (this.getSelectedIndex()) {
          case 0 -> "lose_effect";
          case 1 -> "gain_effect";
          case 2 -> "unconditional";
          default -> null;
        };
      }

      private class TypeComboBoxListener implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent e) {
          AddTriggerPanel.this.valueSelect.setModel(
              TypeComboBox.this.getSelectedIndex() == 2
                  ? AddTriggerPanel.this.EMPTY_MODEL
                  : AddTriggerPanel.this.EFFECT_MODEL);
        }
      }
    }
  }
}
