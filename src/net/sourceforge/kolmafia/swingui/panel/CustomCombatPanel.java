package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLCharacter.TurtleBlessing;
import net.sourceforge.kolmafia.KoLGUIConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.combat.CombatActionManager;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.swingui.button.RelayBrowserButton;
import net.sourceforge.kolmafia.swingui.button.ThreadedButton;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterComboBox;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.webui.RelayLoader;

public class CustomCombatPanel extends JPanel {
  private JComboBox<String> actionSelect;
  protected JTree combatTree;
  protected JTextArea combatEditor;
  protected DefaultTreeModel combatModel;

  protected JPanel combatCardPanel;
  protected CardLayout combatCards;
  public JComboBox<String> availableScripts;

  private static ImageIcon stealImg, stunImg;
  private static ImageIcon potionImg, olfactImg, puttyImg;
  private static ImageIcon antidoteImg, restoreImg, safeImg;

  static {
    CustomCombatPanel.stealImg = CustomCombatPanel.getImage("knobsack.gif");
    CustomCombatPanel.stunImg = CustomCombatPanel.getImage("entnoodles.gif");
    CustomCombatPanel.potionImg = CustomCombatPanel.getImage("exclam.gif");
    CustomCombatPanel.olfactImg = CustomCombatPanel.getImage("footprints.gif");
    CustomCombatPanel.puttyImg = CustomCombatPanel.getImage("sputtycopy.gif");
    CustomCombatPanel.antidoteImg = CustomCombatPanel.getImage("poisoncup.gif");
    CustomCombatPanel.restoreImg = CustomCombatPanel.getImage("mp.gif");
    CustomCombatPanel.safeImg = CustomCombatPanel.getImage("cast.gif");
  }

  public CustomCombatPanel() {
    this.combatTree = new JTree();
    this.combatModel = (DefaultTreeModel) this.combatTree.getModel();

    this.combatCards = new CardLayout();
    this.combatCardPanel = new JPanel(this.combatCards);

    this.availableScripts = new CombatComboBox();

    this.combatCardPanel.add("tree", new CustomCombatTreePanel());
    this.combatCardPanel.add("editor", new CustomCombatEditorPanel());

    this.setLayout(new BorderLayout(5, 5));

    this.add(new SpecialActionsPanel(), BorderLayout.NORTH);
    this.add(this.combatCardPanel, BorderLayout.CENTER);

    this.updateFromPreferences();
  }

  public void updateFromPreferences() {
    if (this.actionSelect != null) {
      String battleAction = Preferences.getString("battleAction");
      int battleIndex = KoLCharacter.getBattleSkillNames().indexOf(battleAction);
      KoLCharacter.getBattleSkillNames().setSelectedIndex(battleIndex == -1 ? 0 : battleIndex);
    }

    CombatActionManager.updateFromPreferences();
    this.refreshCombatEditor();
  }

  public void refreshCombatEditor() {
    try {
      String script = (String) this.availableScripts.getSelectedItem();
      try (BufferedReader reader =
          FileUtilities.getReader(CombatActionManager.getStrategyLookupFile(script))) {

        if (reader == null) {
          return;
        }

        StringBuffer buffer = new StringBuffer();
        String line;

        while ((line = reader.readLine()) != null) {
          buffer.append(line);
          buffer.append('\n');
        }

        this.combatEditor.setText(buffer.toString());
      }
    } catch (Exception e) {
      // This should not happen.  Therefore, print
      // a stack trace for debug purposes.

      StaticEntity.printStackTrace(e);
    }

    this.refreshCombatTree();
  }

  /** Internal class used to handle everything related to displaying custom combat. */
  public void refreshCombatTree() {
    this.combatModel.setRoot(CombatActionManager.getStrategyLookup());
    this.combatTree.setRootVisible(false);

    for (int i = 0; i < this.combatTree.getRowCount(); ++i) {
      this.combatTree.expandRow(i);
    }
  }

  private static ImageIcon getImage(final String filename) {
    String path = "itemimages/" + filename;
    FileUtilities.downloadImage(KoLmafia.imageServerPath() + path);
    return JComponentUtilities.getImage(path);
  }

  private class SpecialActionsPanel extends GenericPanel implements Listener {
    private final JPanel special;
    private final JPopupMenu specialPopup;

    private final JLabel stealLabel, stunLabel;
    private final JLabel potionLabel, olfactLabel, puttyLabel;
    private final JLabel antidoteLabel, restoreLabel, safeLabel;
    private final JCheckBoxMenuItem stealItem, stunItem;
    private final JCheckBoxMenuItem potionItem, olfactItem, puttyItem;
    private final JCheckBoxMenuItem restoreItem, safePickpocket;
    private final JMenu poisonItem;
    private boolean updating = true;

    public SpecialActionsPanel() {
      super(new Dimension(70, -1), new Dimension(200, -1));

      CustomCombatPanel.this.actionSelect =
          new AutoFilterComboBox<>(KoLCharacter.getBattleSkillNames());
      CustomCombatPanel.this.actionSelect.addActionListener(new BattleActionListener());

      JPanel special = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
      this.special = special;
      special.setBackground(Color.WHITE);
      special.setBorder(BorderFactory.createLoweredBevelBorder());

      MouseListener listener = new SpecialPopListener();
      special.addMouseListener(listener);

      AscensionClass ascensionClass = KoLCharacter.getAscensionClass();
      String stunSkill = ascensionClass == null ? "none" : ascensionClass.getStun();
      if (stunSkill.equals("Shell Up")) {
        if (KoLCharacter.getBlessingType() != TurtleBlessing.STORM) {
          stunSkill = Preferences.getBoolean("considerShadowNoodles") ? "Shadow Noodles" : "none";
        }
      }
      int stunId = SkillDatabase.getSkillId(stunSkill);
      if (stunId > 0) {
        CustomCombatPanel.stunImg = CustomCombatPanel.getImage(SkillDatabase.getSkillImage(stunId));
      }

      this.stealLabel =
          this.label(
              special,
              listener,
              CustomCombatPanel.stealImg,
              "Pickpocketing will be tried (if appropriate) with non-CCS actions.");

      if (stunSkill.equals("none")) {
        this.stunLabel =
            this.label(
                special,
                listener,
                CustomCombatPanel.stunImg,
                "No stun available. Set considerShadowNoodles = true to use Shadow Noodles.");
      } else {
        this.stunLabel =
            this.label(
                special,
                listener,
                CustomCombatPanel.stunImg,
                stunSkill + " will be cast before non-CCS actions.");
      }

      this.olfactLabel = this.label(special, listener, CustomCombatPanel.olfactImg, null);
      this.puttyLabel = this.label(special, listener, CustomCombatPanel.puttyImg, null);
      this.potionLabel =
          this.label(
              special,
              listener,
              CustomCombatPanel.potionImg,
              "<html>Dungeons of Doom potions will be identified by using them in combat.<br>Requires 'special' action if a CCS is used.</html>");
      this.antidoteLabel = this.label(special, listener, CustomCombatPanel.antidoteImg, null);
      this.restoreLabel =
          this.label(
              special,
              listener,
              CustomCombatPanel.restoreImg,
              "MP restores will be used in combat if needed.");
      this.safeLabel =
          this.label(
              special,
              listener,
              CustomCombatPanel.safeImg,
              "Pickpocketing will be skipped when there are no useful results or it is too dangerous.");

      this.specialPopup = new JPopupMenu("Special Actions");
      this.stealItem =
          this.checkbox(this.specialPopup, listener, "Pickpocket before simple actions");
      if (stunSkill.equals("none")) {
        this.stunItem =
            this.checkbox(
                this.specialPopup,
                listener,
                "No stun available. Set considerShadowNoodles = true to use Shadow Noodles.");
      } else {
        this.stunItem =
            this.checkbox(
                this.specialPopup, listener, "Cast " + stunSkill + " before simple actions");
      }
      this.specialPopup.addSeparator();

      this.olfactItem =
          this.checkbox(this.specialPopup, listener, "One-time automatic Olfaction...");
      this.puttyItem =
          this.checkbox(
              this.specialPopup,
              listener,
              "One-time automatic Spooky Putty/Rain-Doh box/4-d camera...");
      this.potionItem = this.checkbox(this.specialPopup, listener, "Identify bang potions");
      this.specialPopup.addSeparator();

      this.poisonItem = new JMenu("Minimum poison level for antidote use");
      ButtonGroup group = new ButtonGroup();
      this.poison(this.poisonItem, group, listener, "No automatic use");
      this.poison(this.poisonItem, group, listener, "Toad In The Hole (-\u00BDHP/round)");
      this.poison(this.poisonItem, group, listener, "Majorly Poisoned (-90%, -11)");
      this.poison(this.poisonItem, group, listener, "Really Quite Poisoned (-70%, -9)");
      this.poison(this.poisonItem, group, listener, "Somewhat Poisoned (-50%, -7)");
      this.poison(this.poisonItem, group, listener, "A Little Bit Poisoned (-30%, -5)");
      this.poison(this.poisonItem, group, listener, "Hardly Poisoned at All (-10%, -3)");
      this.specialPopup.add(this.poisonItem);
      this.restoreItem = this.checkbox(this.specialPopup, listener, "Restore MP in combat");
      this.safePickpocket =
          this.checkbox(
              this.specialPopup,
              listener,
              "Skip pickpocketing when no useful results or too dangerous");

      VerifiableElement[] elements = new VerifiableElement[2];
      elements[0] = new VerifiableElement("Action:  ", CustomCombatPanel.this.actionSelect);
      elements[1] = new VerifiableElement("Special:  ", special);

      this.setContent(elements);
      ((BorderLayout) this.container.getLayout()).setHgap(0);
      ((BorderLayout) this.container.getLayout()).setVgap(0);

      PreferenceListenerRegistry.registerPreferenceListener("autoSteal", this);
      PreferenceListenerRegistry.registerPreferenceListener("autoEntangle", this);
      PreferenceListenerRegistry.registerPreferenceListener("autoOlfact", this);
      PreferenceListenerRegistry.registerPreferenceListener("autoPutty", this);
      PreferenceListenerRegistry.registerPreferenceListener("autoPotionID", this);
      PreferenceListenerRegistry.registerPreferenceListener("autoAntidote", this);
      PreferenceListenerRegistry.registerPreferenceListener("autoManaRestore", this);
      PreferenceListenerRegistry.registerPreferenceListener("safePickpocket", this);
      PreferenceListenerRegistry.registerPreferenceListener("(skill)", this);

      this.update();
    }

    @Override
    public void update() {
      this.updating = true;

      CustomCombatPanel.this.actionSelect.setSelectedItem(Preferences.getString("battleAction"));

      AscensionClass ascensionClass = KoLCharacter.getAscensionClass();
      String stunSkill = ascensionClass == null ? "none" : ascensionClass.getStun();

      if (KoLCharacter.hasSkill(stunSkill)) {
        this.stunItem.setEnabled(true);
      } else {
        this.stunItem.setEnabled(false);
        Preferences.setBoolean("autoEntangle", false);
      }

      String text;
      boolean pref;
      pref = Preferences.getBoolean("autoSteal");
      this.stealLabel.setVisible(pref);
      this.stealItem.setSelected(pref);
      pref = Preferences.getBoolean("autoEntangle");
      this.stunLabel.setVisible(pref);
      this.stunItem.setSelected(pref);
      text = Preferences.getString("autoOlfact");
      pref = text.length() > 0;
      this.olfactLabel.setVisible(pref);
      this.olfactItem.setSelected(pref);
      this.olfactLabel.setToolTipText(
          "<html>Automatic Olfaction or odor extractor use: "
              + text
              + "<br>Requires 'special' action if a CCS is used.</html>");
      text = Preferences.getString("autoPutty");
      pref = text.length() > 0;
      this.puttyLabel.setVisible(pref);
      this.puttyItem.setSelected(pref);
      this.puttyLabel.setToolTipText(
          "<html>Automatic Spooky Putty sheet, Rain-Doh black box, 4-d camera, crappy camera or portable photocopier use: "
              + text
              + "<br>Requires 'special' action if a CCS is used.</html>");
      pref = Preferences.getBoolean("autoPotionID");
      this.potionLabel.setVisible(pref);
      this.potionItem.setSelected(pref);
      int antidote = Preferences.getInteger("autoAntidote");
      this.antidoteLabel.setVisible(antidote > 0);
      if (antidote >= 0 && antidote < this.poisonItem.getMenuComponentCount()) {
        JRadioButtonMenuItem option =
            (JRadioButtonMenuItem) this.poisonItem.getMenuComponent(antidote);
        option.setSelected(true);
        this.antidoteLabel.setToolTipText(
            "Anti-anti-antidote will be used in combat if you get "
                + option.getText()
                + " or worse.");
      }
      pref = Preferences.getBoolean("autoManaRestore");
      this.restoreLabel.setVisible(pref);
      this.restoreItem.setSelected(pref);
      pref = Preferences.getBoolean("safePickpocket");
      this.safeLabel.setVisible(pref);
      this.safePickpocket.setSelected(pref);

      this.updating = false;
    }

    private JLabel label(
        final JPanel special,
        final MouseListener listener,
        final ImageIcon img,
        final String toolTip) {
      JLabel rv = new JLabel(img);
      rv.setToolTipText(toolTip);
      rv.addMouseListener(listener);
      special.add(rv);
      return rv;
    }

    private JCheckBoxMenuItem checkbox(
        final JPopupMenu menu, final Object listener, final String text) {
      JCheckBoxMenuItem rv = new JCheckBoxMenuItem(text);
      menu.add(rv);
      rv.addItemListener((ItemListener) listener);
      return rv;
    }

    private void poison(
        final JMenu menu, final ButtonGroup group, final Object listener, final String text) {
      JRadioButtonMenuItem rb = new JRadioButtonMenuItem(text);
      menu.add(rb);
      group.add(rb);
      rb.addItemListener((ItemListener) listener);
    }

    @Override
    public void actionConfirmed() {}

    @Override
    public void actionCancelled() {}

    @Override
    public void addStatusLabel() {}

    private class BattleActionListener implements ActionListener {
      @Override
      public void actionPerformed(ActionEvent e) {
        // Don't set preferences from widgets when we
        // are in the middle of loading widgets from
        // preferences.
        if (SpecialActionsPanel.this.updating) {
          return;
        }

        String value = (String) CustomCombatPanel.this.actionSelect.getSelectedItem();

        if (value != null) {
          Preferences.setString("battleAction", value);
        }
      }
    }

    private class SpecialPopListener extends MouseAdapter implements ItemListener {
      @Override
      public void mousePressed(final MouseEvent e) {
        SpecialActionsPanel.this.specialPopup.show(SpecialActionsPanel.this.special, 0, 32);
      }

      @Override
      public void itemStateChanged(final ItemEvent e) {
        // Don't set preferences from widgets when we
        // are in the middle of loading widgets from
        // preferences.
        if (SpecialActionsPanel.this.updating) {
          return;
        }

        boolean state = e.getStateChange() == ItemEvent.SELECTED;
        JMenuItem source = (JMenuItem) e.getItemSelectable();
        if (source == SpecialActionsPanel.this.stealItem) {
          Preferences.setBoolean("autoSteal", state);
        } else if (source == SpecialActionsPanel.this.stunItem) {
          Preferences.setBoolean("autoEntangle", state);
        } else if (source == SpecialActionsPanel.this.olfactItem) {
          if (state
              == !Preferences.getString("autoOlfact")
                  .equals("")) { // pref already set externally, don't prompt
            return;
          }
          String option =
              !state
                  ? null
                  : InputFieldUtilities.input(
                      "Use Transcendent Olfaction or odor extractor when? (item, \"goals\", or \"monster\" plus name; add \"abort\" to stop adventuring)",
                      "goals");

          KoLmafiaCLI.DEFAULT_SHELL.executeCommand("olfact", option == null ? "none" : option);
        } else if (source == SpecialActionsPanel.this.puttyItem) {
          if (state
              == !Preferences.getString("autoPutty")
                  .equals("")) { // pref already set externally, don't prompt
            return;
          }
          String option =
              !state
                  ? null
                  : InputFieldUtilities.input(
                      "Use Spooky Putty sheet, Rain-Doh black box, 4-d camera, crappy camera or portable photocopier when? (item, \"goals\", or \"monster\" plus name; add \"abort\" to stop adventuring)",
                      "goals abort");

          KoLmafiaCLI.DEFAULT_SHELL.executeCommand("putty", option == null ? "none" : option);
        } else if (source == SpecialActionsPanel.this.potionItem) {
          Preferences.setBoolean("autoPotionID", state);
        } else if (source == SpecialActionsPanel.this.restoreItem) {
          Preferences.setBoolean("autoManaRestore", state);
        } else if (source == SpecialActionsPanel.this.safePickpocket) {
          Preferences.setBoolean("safePickpocket", state);
        } else if (source instanceof JRadioButtonMenuItem) {
          Preferences.setInteger(
              "autoAntidote",
              Arrays.asList(SpecialActionsPanel.this.poisonItem.getMenuComponents())
                  .indexOf(source));
        }
      }
    }
  }

  public class CombatComboBox extends JComboBox<String> implements Listener {
    public CombatComboBox() {
      super(CombatActionManager.getAvailableLookups());
      this.addActionListener(this);
      PreferenceListenerRegistry.registerPreferenceListener("customCombatScript", this);
    }

    @Override
    public void update() {
      CustomCombatPanel.this.combatCards.show(CustomCombatPanel.this.combatCardPanel, "tree");
      this.setSelectedItem(Preferences.getString("customCombatScript"));
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      String script = (String) this.getSelectedItem();
      if (script != null) {
        CombatActionManager.loadStrategyLookup(script);
        CustomCombatPanel.this.refreshCombatTree();
      }
    }
  }

  private class CustomCombatEditorPanel extends ScrollablePanel<JTextArea> {
    public CustomCombatEditorPanel() {
      super("Editor", "save", "cancel", new JTextArea());
      CustomCombatPanel.this.combatEditor = this.scrollComponent;
      CustomCombatPanel.this.combatEditor.setFont(KoLGUIConstants.DEFAULT_FONT);
      CustomCombatPanel.this.refreshCombatTree();

      this.eastPanel.add(
          new RelayBrowserButton("help", "https://wiki.kolmafia.us/index.php/Custom_Combat_Script"),
          BorderLayout.SOUTH);
    }

    @Override
    public void actionConfirmed() {
      String script = (String) CustomCombatPanel.this.availableScripts.getSelectedItem();
      String saveText = CustomCombatPanel.this.combatEditor.getText();

      File location = CombatActionManager.getStrategyLookupFile(script);
      PrintStream writer = LogStream.openStream(location, true);

      writer.print(saveText);
      writer.close();
      writer = null;

      KoLCharacter.battleSkillNames.setSelectedItem("custom combat script");
      Preferences.setString("battleAction", "custom combat script");

      // After storing all the data on disk, go ahead
      // and reload the data inside of the tree.

      CombatActionManager.loadStrategyLookup(script);
      CombatActionManager.saveStrategyLookup(script);

      CustomCombatPanel.this.refreshCombatTree();
      CustomCombatPanel.this.combatCards.show(CustomCombatPanel.this.combatCardPanel, "tree");
    }

    @Override
    public void actionCancelled() {
      CustomCombatPanel.this.refreshCombatEditor();
      CustomCombatPanel.this.combatCards.show(CustomCombatPanel.this.combatCardPanel, "tree");
    }

    @Override
    public void setEnabled(final boolean isEnabled) {}
  }

  public class CustomCombatTreePanel extends ScrollablePanel<JTree> {
    public CustomCombatTreePanel() {
      super("", "edit", "help", CustomCombatPanel.this.combatTree);
      CustomCombatPanel.this.combatTree.setVisibleRowCount(8);

      this.centerPanel.add(CustomCombatPanel.this.availableScripts, BorderLayout.NORTH);

      JPanel extraButtons = new JPanel(new GridLayout(3, 1, 5, 5));

      extraButtons.add(new ThreadedButton("new", new NewScriptRunnable()));
      extraButtons.add(new ThreadedButton("copy", new CopyScriptRunnable()));
      extraButtons.add(new ThreadedButton("delete", new DeleteScriptRunnable()));

      JPanel buttonHolder = new JPanel(new BorderLayout());
      buttonHolder.add(extraButtons, BorderLayout.NORTH);

      this.eastPanel.add(buttonHolder, BorderLayout.SOUTH);
    }

    @Override
    public void actionConfirmed() {
      CustomCombatPanel.this.refreshCombatEditor();
      CustomCombatPanel.this.combatCards.show(CustomCombatPanel.this.combatCardPanel, "editor");
    }

    @Override
    public void actionCancelled() {
      RelayLoader.openSystemBrowser("https://wiki.kolmafia.us/index.php/Custom_Combat_Script");
    }

    @Override
    public void setEnabled(final boolean isEnabled) {}

    public class NewScriptRunnable implements Runnable {
      @Override
      public void run() {
        String name = InputFieldUtilities.input("Give your combat script a name!");
        if (name == null || name.equals("") || name.equals("default")) {
          return;
        }

        CombatActionManager.loadStrategyLookup(name);
        CustomCombatPanel.this.refreshCombatTree();
      }
    }

    public class CopyScriptRunnable implements Runnable {
      @Override
      public void run() {
        String name = InputFieldUtilities.input("Make a copy of current script called:");
        if (name == null || name.equals("") || name.equals("default")) {
          return;
        }

        CombatActionManager.copyStrategyLookup(name);
        CombatActionManager.loadStrategyLookup(name);
        CustomCombatPanel.this.refreshCombatTree();
      }
    }

    public class DeleteScriptRunnable implements Runnable {
      @Override
      public void run() {
        String strategy = CombatActionManager.getStrategyLookupName();

        if (!InputFieldUtilities.confirm("Delete " + strategy + "?")) {
          return;
        }

        CombatActionManager.deleteCurrentStrategyLookup();
        CombatActionManager.loadStrategyLookup("default");
        CustomCombatPanel.this.refreshCombatTree();
      }
    }
  }
}
