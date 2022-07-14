package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.persistence.DailyLimitDatabase.DailyLimitType;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.panel.RestorativeItemPanel;
import net.sourceforge.kolmafia.swingui.panel.StatusEffectPanel;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterComboBox;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.EditableAutoFilterComboBox;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class SkillBuffFrame extends GenericFrame {
  private LockableListModel<String> contacts;

  private SkillTypeComboBox typeSelect;
  private SkillSelectComboBox skillSelect;
  private AutoHighlightTextField amountField;
  private EditableAutoFilterComboBox targetSelect;
  private final ShowDescriptionList<AdventureResult> effectList;

  public SkillBuffFrame() {
    this("");
  }

  public SkillBuffFrame(final String recipient) {
    super("Skill Casting");

    JPanel skillWrapper = new JPanel(new BorderLayout());
    skillWrapper.add(new SkillBuffPanel(), BorderLayout.NORTH);

    this.effectList =
        new ShowDescriptionList<>(
            (LockableListModel<AdventureResult>) KoLConstants.activeEffects, 12);
    this.effectList.addListSelectionListener(new SkillReselector());

    this.tabs.addTab("Active Effects", new StatusEffectPanel(this.effectList));
    this.tabs.addTab("Recovery Items", new RestorativeItemPanel());

    skillWrapper.add(this.tabs, BorderLayout.CENTER);

    this.setCenterComponent(skillWrapper);

    this.setRecipient(recipient);
  }

  public static void update() {
    for (Frame frame : Frame.getFrames()) {
      if (frame.getClass() == SkillBuffFrame.class) {
        SkillBuffFrame sbf = (SkillBuffFrame) frame;
        if (sbf.skillSelect != null) {
          sbf.skillSelect.update();
        }
      }
    }
  }

  public void setRecipient(String recipient) {
    if (!this.contacts.contains(recipient)) {
      recipient = ContactManager.getPlayerName(recipient);
      this.contacts.add(0, recipient);
    }

    this.targetSelect.getEditor().setItem(recipient);
    this.targetSelect.setSelectedItem(recipient);
  }

  public void dumpDisabledSkills() {
    if (this.skillSelect != null) {
      this.skillSelect.dumpDisabledItems();
    }
  }

  private class SkillReselector implements ListSelectionListener {
    @Override
    public void valueChanged(final ListSelectionEvent e) {
      AdventureResult effect = SkillBuffFrame.this.effectList.getSelectedValue();
      if (effect == null) {
        return;
      }

      SkillBuffFrame.this.skillSelect.setSelectedItem(
          UseSkillRequest.getUnmodifiedInstance(UneffectRequest.effectToSkill(effect.getName())));
    }
  }

  private class SkillSelectComboBox extends AutoFilterComboBox<UseSkillRequest>
      implements Listener {
    public SkillSelectComboBox(final LockableListModel<UseSkillRequest> model) {
      super(model);
      this.update();
      this.setSkillListeners();
    }

    private void setSkillListeners() {
      PreferenceListenerRegistry.registerPreferenceListener("tomeSummons", this);
      for (var limit : DailyLimitType.CAST.getDailyLimits().values()) {
        PreferenceListenerRegistry.registerPreferenceListener(limit.getPref(), this);
      }
    }

    @Override
    public void update() {
      super.update();
      this.clearDisabledItems();

      int selected = this.getSelectedIndex();

      for (int j = 0; j < this.getItemCount(); j++) {
        var disable = this.getItemAt(j).getMaximumCast() <= 0;
        this.setDisabledIndex(j, disable);
        if (disable && j == selected) {
          this.setSelectedIndex(-1);
        }
      }
    }
  }

  private class SkillBuffPanel extends GenericPanel {
    public SkillBuffPanel() {
      super("cast", "maxcast", new Dimension(80, 20), new Dimension(240, 20));

      SkillBuffFrame.this.typeSelect = new SkillTypeComboBox();
      SkillBuffFrame.this.skillSelect =
          new SkillSelectComboBox((LockableListModel<UseSkillRequest>) KoLConstants.usableSkills);
      SkillBuffFrame.this.amountField = new AutoHighlightTextField();

      SkillBuffFrame.this.contacts = ContactManager.getMailContacts().getMirrorImage();
      SkillBuffFrame.this.targetSelect =
          new EditableAutoFilterComboBox(SkillBuffFrame.this.contacts);

      VerifiableElement[] elements = new VerifiableElement[4];
      elements[0] = new VerifiableElement("Skill Type: ", SkillBuffFrame.this.typeSelect);
      elements[1] = new VerifiableElement("Skill Name: ", SkillBuffFrame.this.skillSelect);
      elements[2] = new VerifiableElement("# of Casts: ", SkillBuffFrame.this.amountField);
      elements[3] = new VerifiableElement("The Victim: ", SkillBuffFrame.this.targetSelect);

      this.setContent(elements);
    }

    @Override
    public void setEnabled(final boolean isEnabled) {
      if (SkillBuffFrame.this.skillSelect == null || SkillBuffFrame.this.targetSelect == null) {
        return;
      }

      super.setEnabled(isEnabled);

      SkillBuffFrame.this.skillSelect.setEnabled(isEnabled);
      SkillBuffFrame.this.targetSelect.setEnabled(isEnabled);
    }

    @Override
    public void actionConfirmed() {
      this.buff(false);
    }

    @Override
    public void actionCancelled() {
      this.buff(true);
    }

    private void buff(boolean maxBuff) {
      UseSkillRequest request = (UseSkillRequest) SkillBuffFrame.this.skillSelect.getSelectedItem();
      if (request == null) {
        return;
      }

      String buffName = request.getSkillName();
      if (buffName == null) {
        return;
      }

      if (buffName.equals("Summon Clip Art")) {
        InputFieldUtilities.alert(
            "You cannot specify which item to create here. Please go to the \"create\" tab of the Item Manager and make what you want there.");
        return;
      }

      String[] targets =
          ContactManager.extractTargets(
              (String) SkillBuffFrame.this.targetSelect.getSelectedItem());

      int buffCount =
          !maxBuff
              ? InputFieldUtilities.getValue(SkillBuffFrame.this.amountField, 1)
              : Integer.MAX_VALUE;
      if (buffCount == 0) {
        return;
      }

      if (targets.length == 0) {
        RequestThread.checkpointedPostRequest(
            UseSkillRequest.getInstance(buffName, KoLCharacter.getUserName(), buffCount));
        return;
      }

      try (Checkpoint checkpoint = new Checkpoint()) {
        for (int i = 0; i < targets.length && KoLmafia.permitsContinue(); ++i) {
          if (targets[i] != null) {
            RequestThread.postRequest(UseSkillRequest.getInstance(buffName, targets[i], buffCount));
          }
        }
      }
    }
  }

  private class SkillTypeComboBox extends JComboBox<String> {
    public SkillTypeComboBox() {
      super();
      addItem("All Castable Skills");
      addItem("Summoning Skills");
      addItem("Remedies");
      addItem("Self-Only");
      addItem("Buffs");
      addItem("Songs");
      addItem("Expressions");
      addItem("Walks");
      addActionListener(new SkillTypeListener());
    }

    private class SkillTypeListener implements ActionListener {
      @Override
      public void actionPerformed(final ActionEvent e) {
        ComboBoxModel<UseSkillRequest> oldModel = SkillBuffFrame.this.skillSelect.getModel();
        ComboBoxModel<UseSkillRequest> newModel = oldModel;
        switch (SkillTypeComboBox.this.getSelectedIndex()) {
          case 0:
            // All skills
            newModel = (LockableListModel<UseSkillRequest>) KoLConstants.usableSkills;
            break;
          case 1:
            // Summoning skills
            newModel = (LockableListModel<UseSkillRequest>) KoLConstants.summoningSkills;
            break;
          case 2:
            // Remedy skills
            newModel = (LockableListModel<UseSkillRequest>) KoLConstants.remedySkills;
            break;
          case 3:
            // Self-only skills
            newModel = (LockableListModel<UseSkillRequest>) KoLConstants.selfOnlySkills;
            break;
          case 4:
            // Buff skills
            newModel = (LockableListModel<UseSkillRequest>) KoLConstants.buffSkills;
            break;
          case 5:
            // Song skills
            newModel = (LockableListModel<UseSkillRequest>) KoLConstants.songSkills;
            break;
          case 6:
            // Expression skills
            newModel = (LockableListModel<UseSkillRequest>) KoLConstants.expressionSkills;
            break;
          case 7:
            // Walk skills
            newModel = (LockableListModel<UseSkillRequest>) KoLConstants.walkSkills;
            break;
        }
        if (newModel != oldModel) {
          int index = SkillTypeComboBox.this.getSelectedIndex();
          SkillBuffFrame.this.skillSelect.setModel(newModel);
        }
      }
    }
  }
}
