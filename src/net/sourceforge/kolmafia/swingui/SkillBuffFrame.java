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
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.panel.RestorativeItemPanel;
import net.sourceforge.kolmafia.swingui.panel.StatusEffectPanel;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterComboBox;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class SkillBuffFrame extends GenericFrame {
  private LockableListModel<String> contacts;

  private SkillTypeComboBox typeSelect;
  private SkillSelectComboBox skillSelect;
  private AutoHighlightTextField amountField;
  private AutoFilterComboBox targetSelect;
  private final ShowDescriptionList effectList;

  public SkillBuffFrame() {
    this("");
  }

  public SkillBuffFrame(final String recipient) {
    super("Skill Casting");

    JPanel skillWrapper = new JPanel(new BorderLayout());
    skillWrapper.add(new SkillBuffPanel(), BorderLayout.NORTH);

    this.effectList =
        new ShowDescriptionList(
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
    public void valueChanged(final ListSelectionEvent e) {
      AdventureResult effect = (AdventureResult) SkillBuffFrame.this.effectList.getSelectedValue();
      if (effect == null) {
        return;
      }

      SkillBuffFrame.this.skillSelect.setSelectedItem(
          UseSkillRequest.getUnmodifiedInstance(UneffectRequest.effectToSkill(effect.getName())));
    }
  }

  private class SkillSelectComboBox extends AutoFilterComboBox implements Listener {
    public SkillSelectComboBox(final LockableListModel<UseSkillRequest> model) {
      super(model, false);
      this.update();
      this.setSkillListeners();
    }

    private final String[][] DAILY_LIMITED_SKILLS = {
      {
        "The Smile of Mr. A.", "_smilesOfMrA", "integer", "5",
      },
      {
        "Rainbow Gravitation", "prismaticSummons", "integer", "3",
      },
      {
        "Vent Rage Gland", "rageGlandVented", "boolean", "true",
      },
      {
        "Summon Crimbo Candy", "_candySummons", "integer", "1",
      },
      {
        "Lunch Break", "_lunchBreak", "boolean", "true",
      },
      {
        "Summon &quot;Boner Battalion&quot;", "_bonersSummoned", "boolean", "true",
      },
      {
        "Request Sandwich", "_requestSandwichSucceeded", "boolean", "true",
      },
      {
        "Grab a Cold One", "_coldOne", "boolean", "true",
      },
      {
        "Spaghetti Breakfast", "_spaghettiBreakfast", "boolean", "true",
      },
      {
        "Pastamastery", "noodleSummons", "integer", "1",
      },
      {
        "Canticle of Carboloading", "_carboLoaded", "boolean", "true",
      },
      {
        "Advanced Saucecrafting", "reagentSummons", "integer", "1",
      },
      {
        "That's Not a Knife", "_discoKnife", "boolean", "true",
      },
      {
        "Summon \"Boner Battalion\"", "_bonersSummoned", "boolean", "true",
      },
      {
        "Acquire Rhinestones", "_rhinestonesAcquired", "boolean", "true",
      },
      {
        "Psychokinetic Hug", "_psychokineticHugUsed", "boolean", "true",
      },
      {
        "Managerial Manipulation", "_managerialManipulationUsed", "boolean", "true",
      },
      {
        "Throw Party", "_petePartyThrown", "boolean", "true",
      },
      {
        "Throw Party", "_petePartyThrown", "boolean", "true",
      },
      {
        "Perfect Freeze", "_perfectFreezeUsed", "boolean", "true",
      },
      {
        "Communism!", "_communismUsed", "boolean", "true",
      },
      {
        "Bow-Legged Swagger", "_bowleggedSwaggerUsed", "boolean", "true",
      },
      {
        "Bend Hell", "_bendHellUsed", "boolean", "true",
      },
      {
        "Steely-Eyed Squint", "_steelyEyedSquintUsed", "boolean", "true",
      },
      {
        "Ceci N'Est Pas Un Chapeau", "_ceciHatUsed", "boolean", "true",
      },
      {
        "Evoke Eldritch Horror", "_eldritchHorrorEvoked", "boolean", "true",
      },
      {
        "Incredible Self Esteem", "_incredibleSelfEsteemCast", "boolean", "true",
      },
      {
        "Prevent Scurvy and Sobriety", "_preventScurvy", "boolean", "true",
      },
      {
        "Advanced Cocktailcrafting", "cocktailSummons", "integer", "1",
      },
      {
        "The Ballad of Richie Thingfinder", "_thingfinderCasts", "integer", "10",
      },
      {
        "Benetton's Medley of Diversity", "_benettonsCasts", "integer", "10",
      },
      {
        "Elron's Explosive Etude", "_elronsCasts", "integer", "10",
      },
      {
        "Chorale of Companionship", "_companionshipCasts", "integer", "10",
      },
      {
        "Prelude of Precision", "_precisionCasts", "integer", "10",
      },
      {
        "Donho's Bubbly Ballad", "_donhosCasts", "integer", "50",
      },
      {
        "Inigo's Incantation of Inspiration", "_inigosCasts", "integer", "5",
      },
      {
        "Summon Snowcones", "_snowconeSummons", "variable", "tomesummons",
      },
      {
        "Summon Stickers", "_stickerSummons", "variable", "tomesummons",
      },
      {
        "Summon Sugar Sheets", "_sugarSummons", "variable", "tomesummons",
      },
      {
        "Summon Clip Art", "_clipartSummons", "variable", "tomesummons",
      },
      {
        "Summon Rad Libs", "_radlibSummons", "variable", "tomesummons",
      },
      {
        "Summon Smithsness", "_smithsnessSummons", "variable", "tomesummons",
      },
      {
        "Dummy", "tomeSummons", "variable", "tomesummons",
      },
      {
        "Summon Hilarious Objects", "grimoire1Summons", "integer", "1",
      },
      {
        "Summon Tasteful Items", "grimoire2Summons", "integer", "1",
      },
      {
        "Summon Alice's Army Cards", "grimoire3Summons", "integer", "1",
      },
      {
        "Summon Geeky Gifts", "_grimoireGeekySummons", "integer", "1",
      },
      {
        "Summon Confiscated Things", "_grimoireConfiscatorSummons", "integer", "1",
      },
      {
        "Demand Sandwich", "_demandSandwich", "integer", "3",
      },
      {
        "Conjure Eggs", "_jarlsEggsSummoned", "boolean", "true",
      },
      {
        "Conjure Dough", "_jarlsDoughSummoned", "boolean", "true",
      },
      {
        "Conjure Vegetables", "_jarlsVeggiesSummoned", "boolean", "true",
      },
      {
        "Conjure Cheese", "_jarlsCheeseSummoned", "boolean", "true",
      },
      {
        "Conjure Meat Product", "_jarlsMeatSummoned", "boolean", "true",
      },
      {
        "Conjure Potato", "_jarlsPotatoSummoned", "boolean", "true",
      },
      {
        "Conjure Cream", "_jarlsCreamSummoned", "boolean", "true",
      },
      {
        "Conjure Fruit", "_jarlsFruitSummoned", "boolean", "true",
      },
      {
        "Pirate Bellow", "_pirateBellowUsed", "boolean", "true",
      },
      {
        "Summon Holiday Fun!", "_holidayFunUsed", "boolean", "true",
      },
      {
        "Summon Carrot", "_summonCarrotUsed", "boolean", "true",
      },
      {
        "Summon Kokomo Resort Pass", "_summonResortPassUsed", "boolean", "true",
      },
      {
        "Bowl Full of Jelly", "_bowlFullOfJellyUsed", "boolean", "true",
      },
      {
        "Eye and a Twist", "_eyeAndATwistUsed", "boolean", "true",
      },
      {
        "Chubby and Plump", "_chubbyAndPlumpUsed", "boolean", "true",
      }
    };

    private void setSkillListeners() {
      PreferenceListenerRegistry.registerPreferenceListener("tomeSummons", this);
      for (int i = 0; i < DAILY_LIMITED_SKILLS.length; ++i) {
        String setting = DAILY_LIMITED_SKILLS[i][1];
        PreferenceListenerRegistry.registerPreferenceListener(setting, this);
      }
    }

    public void update() {
      this.clearDisabledItems();

      for (int i = 0; i < DAILY_LIMITED_SKILLS.length; ++i) {
        String skill = DAILY_LIMITED_SKILLS[i][0];
        String setting = DAILY_LIMITED_SKILLS[i][1];
        String type = DAILY_LIMITED_SKILLS[i][2];
        String value = DAILY_LIMITED_SKILLS[i][3];

        boolean skillDisable = false;

        if (type.equals("boolean")) {
          skillDisable = Preferences.getBoolean(setting) == Boolean.valueOf(value);
        } else if (type.equals("integer")) {
          skillDisable = Preferences.getInteger(setting) >= Integer.valueOf(value);
        } else if (type.equals("variable")) {
          if (value.equals("tomesummons")) {
            int used = Preferences.getInteger(KoLCharacter.canInteract() ? setting : "tomeSummons");
            int maxCast = Math.max(3 - used, 0);
            skillDisable = maxCast == 0;
          }
        }

        int selected = this.getSelectedIndex();
        for (int j = 0; j < this.getItemCount(); j++) {
          Object obj = this.getItemAt(j);
          if (obj.toString().contains(skill)) {
            this.setDisabledIndex(j, skillDisable);
            if (skillDisable && j == selected) {
              this.setSelectedIndex(-1);
            }
          }
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
      SkillBuffFrame.this.targetSelect = new AutoFilterComboBox(SkillBuffFrame.this.contacts, true);

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

  private class SkillTypeComboBox extends JComboBox {
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
      public void actionPerformed(final ActionEvent e) {
        ComboBoxModel oldModel = SkillBuffFrame.this.skillSelect.getModel();
        ComboBoxModel newModel = oldModel;
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
