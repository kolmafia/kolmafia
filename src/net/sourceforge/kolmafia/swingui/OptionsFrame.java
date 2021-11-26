package net.sourceforge.kolmafia.swingui;

import static javax.swing.UIManager.getLookAndFeel;

import com.informit.guides.JDnDList;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.DefaultCaret;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLGUIConstants;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.swingui.MaximizerFrame.SmartButtonGroup;
import net.sourceforge.kolmafia.swingui.button.ThreadedButton;
import net.sourceforge.kolmafia.swingui.panel.AddCustomDeedsPanel;
import net.sourceforge.kolmafia.swingui.panel.CardLayoutSelectorPanel;
import net.sourceforge.kolmafia.swingui.panel.DailyDeedsPanel;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.panel.OptionsPanel;
import net.sourceforge.kolmafia.swingui.panel.ScrollablePanel;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.CollapsibleTextArea;
import net.sourceforge.kolmafia.swingui.widget.ColorChooser;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.RelayServer;
import org.jdesktop.swingx.JXPanel;
import tab.CloseTabPaneEnhancedUI;

public class OptionsFrame extends GenericFrame {
  public OptionsFrame() {
    super("Preferences");

    CardLayoutSelectorPanel selectorPanel = new CardLayoutSelectorPanel(null, "mmmmmmmmmmmm");

    selectorPanel.addPanel("General", new GeneralOptionsPanel(), true);
    selectorPanel.addPanel(" - Item Acquisition", new ItemOptionsPanel(), true);
    selectorPanel.addPanel(" - Maximizer", new MaximizerOptionsPanel(), true);
    selectorPanel.addPanel(" - IotM Tracking", new IotMTrackingPanel(), true);
    selectorPanel.addPanel(" - Session Logs", new SessionLogOptionsPanel(), true);
    selectorPanel.addPanel(" - Extra Debugging", new DebugOptionsPanel(), true);

    JPanel programsPanel = new JPanel();
    programsPanel.setLayout(new BoxLayout(programsPanel, BoxLayout.Y_AXIS));
    programsPanel.add(new EditorPanel());
    programsPanel.add(Box.createVerticalGlue());
    selectorPanel.addPanel(" - External Programs", programsPanel, true);

    selectorPanel.addPanel("Look & Feel", new UserInterfacePanel(), true);
    selectorPanel.addPanel(" - Main Interface", new StartupFramesPanel(), true);
    selectorPanel.addPanel(" - Chat Options", new ChatOptionsPanel(), true);
    selectorPanel.addPanel(" - Relay Browser", new RelayOptionsPanel(), true);
    selectorPanel.addPanel(" - Text Colors", new ColorOptionsPanel(), true);

    selectorPanel.addPanel("Automation", new ScriptPanel(), true);
    selectorPanel.addPanel(" - In Ronin", new BreakfastPanel("Hardcore"), true);
    selectorPanel.addPanel(" - After Ronin", new BreakfastPanel("Softcore"), true);
    selectorPanel.addPanel(" - Always", new BreakfastAlwaysPanel(), true);

    JPanel customDeedPanel = new JPanel();
    customDeedPanel.setLayout(new BoxLayout(customDeedPanel, BoxLayout.Y_AXIS));
    customDeedPanel.add(new CustomizeDailyDeedsPanel("Message"));
    customDeedPanel.add(new CustomizeDailyDeedsPanel());
    selectorPanel.addPanel(" - Daily Deeds", customDeedPanel, true);

    selectorPanel.addPanel("Script Buttons", new ScriptButtonPanel(), true);
    selectorPanel.addPanel("Bookmarks", new BookmarkManagePanel(), true);
    selectorPanel.addPanel("SVN", new SVNPanel(), true);
    selectorPanel.addPanel("Maximizer Strings", new MaximizerStringsPanel());

    this.setCenterComponent(selectorPanel);

    if (!Preferences.getBoolean("customizedTabs")) {
      selectorPanel.setSelectedIndex(5);
    } else if (RelayServer.isRunning()) {
      selectorPanel.setSelectedIndex(8);
    } else {
      selectorPanel.setSelectedIndex(0);
    }
  }

  private class SessionLogOptionsPanel extends OptionsPanel {
    /** Constructs a new <code>SessionLogOptionsPanel</code> */
    public SessionLogOptionsPanel() {
      super(new Dimension(20, 20), new Dimension(370, 20));

      String[][] options = {
        {"logStatusOnLogin", "Session log records your player's state on login"},
        {"logReverseOrder", "Log adventures left instead of adventures used"},
        {},
        {"logBattleAction", "Session log records attacks for each round"},
        {"logFamiliarActions", "Session log records actions made by familiars"},
        {"logMonsterHealth", "Session log records monster health changes"},
        {},
        {"logGainMessages", "Session log records HP/MP/meat changes"},
        {"logStatGains", "Session log records stat gains"},
        {"logAcquiredItems", "Session log records items acquired"},
        {"logStatusEffects", "Session log records status effects gained"},
        {"logPreferenceChange", "Log preference changes"}
      };

      this.setOptions(options);
    }
  }

  private class RelayOptionsPanel extends OptionsPanel {
    private JLabel colorChanger;

    /** Constructs a new <code>RelayOptionsPanel</code> */
    public RelayOptionsPanel() {
      super(new Dimension(16, 16), new Dimension(370, 16));

      String[][] options = {
        {"relayShowSpoilers", "Show blatant spoilers for choices and puzzles"},
        {
          "relayShowWarnings",
          "Warn if about to adventure somewhere which contains common adventures you are unprepared for or can unlock but haven't"
        },
        {},
        {
          "relayAllowRemoteAccess",
          "Allow network devices to access relay browser (requires restart)"
        },
        {"relayOverridesImages", "Override certain KoL images"},
        {"relayAddSounds", "Add sounds to certain events"},
        {},
        {"relayAddsWikiLinks", "Check wiki for item descriptions (fails for unknowns)"},
        {"relayAddsQuickScripts", "Add quick script links to menu bar (see Links tab)"},
        {},
        {"relayAddsRestoreLinks", "Add HP/MP restore links to left side pane"},
        {"relayAddsUpArrowLinks", "Add buff maintenance links to left side pane"},
        {"relayTextualizesEffects", "Textualize effect links in left side pane"},
        {"relayAddsDiscoHelper", "Add Disco Bandit helper to fights"},
        {"macroLens", "Show Combat Macro helper during fights"},
        {},
        {"relayRunsAfterAdventureScript", "Run afterAdventureScript after manual adventures"},
        {"relayRunsBeforeBattleScript", "Run betweenBattleScript before manual adventures"},
        {"relayMaintainsEffects", "Run moods before manual adventures"},
        {"relayMaintainsHealth", "Maintain health before manual adventures"},
        {"relayMaintainsMana", "Maintain mana before manual adventures"},
        {"relayWarnOnRecoverFailure", "Show a warning if any of the above maintenances fails"},
        {"relayRunsBeforePVPScript", "Run beforePVPScript before manual PVP attacks"},
        {},
        {"relayUsesIntegratedChat", "Integrate chat and relay browser gCLI interfaces"},
        {"relayFormatsChatText", "Reformat incoming chat HTML to conform to web standards"},
        {"relayAddsGraphicalCLI", "Add command-line interface to right side pane"},
        {},
        {"relayAddsUseLinks", "Add [use] links when receiving items"},
        {"relayUsesInlineLinks", "Force results to reload inline for [use] links"},
        {"relayHidesJunkMallItems", "Hide junk and overpriced items in PC stores"},
        {"relayTrimsZapList", "Trim zap list to show only known zappable items"},
        {},
        {"relayAddsCustomCombat", "Add custom buttons to the top of fight pages"},
        {
          "relayScriptButtonFirst",
          "If using custom buttons, put script button first rather than attack"
        },
        {"arcadeGameHints", "Provide hints for Arcade games"},
        {"spelunkyHints", "Provide hints for Spelunky"},
      };

      this.setOptions(options);
    }

    @Override
    public void setContent(VerifiableElement[] elements) {
      VerifiableElement[] newElements = new VerifiableElement[elements.length + 1];

      System.arraycopy(elements, 0, newElements, 0, elements.length);

      this.colorChanger = new ColorChooser("defaultBorderColor");

      newElements[elements.length] =
          new VerifiableElement(
              "Change the color for tables in the browser interface",
              SwingConstants.LEFT,
              this.colorChanger);

      super.setContent(newElements);
    }

    @Override
    public void actionConfirmed() {
      boolean old = Preferences.getBoolean("relayOverridesImages");

      super.actionConfirmed();

      if (old != Preferences.getBoolean("relayOverridesImages")) {
        RelayRequest.loadOverrideImages(!old);
      }
    }

    @Override
    public void actionCancelled() {
      String color = Preferences.getString("defaultBorderColor");

      if (color.equals("blue")) {
        this.colorChanger.setBackground(Color.blue);
      } else {
        this.colorChanger.setBackground(DataUtilities.toColor(color));
      }

      super.actionCancelled();
    }
  }

  private class GeneralOptionsPanel extends OptionsPanel {
    /** Constructs a new <code>GeneralOptionsPanel</code> */
    public GeneralOptionsPanel() {
      super(new Dimension(20, 16), new Dimension(370, 16));

      String[][] options = {
        {"showAllRequests", "Show all requests in a mini-browser window"},
        {
          "showExceptionalRequests",
          "Automatically load 'click here to load in relay browser' in mini-browser"
        },
        {},
        {"useZoneComboBox", "Use zone selection instead of adventure name filter"},
        {"cacheMallSearches", "Cache mall search terms in mall search interface"},
        {"saveSettingsOnSet", "Save options to disk whenever they change"},
        {},
        {"removeMalignantEffects", "Auto-remove malignant status effects"},
        {"switchEquipmentForBuffs", "Allow equipment changing when casting buffs"},
        {"allowNonMoodBurning", "Cast buffs not defined in moods during buff balancing"},
        {"allowSummonBurning", "Cast summoning skills during buff balancing"},
        {"restUsingChateau", "Rest in the Chateau Mantegna rather than at your dwelling"},
        {"odeBuffbotCheck", "Give ode warning if you can get buffbot Ode buff"},
        {},
        {"requireSewerTestItems", "Require appropriate test items to adventure in clan sewers "},
        {},
        {"sharePriceData", "Share recent Mall price data with other users"},
        {"showIgnoringStorePrices", "Show prices in stores ignoring you"},
        {},
        {"dontStopForCounters", "Don't stop automation when counters expire"},
        {
          "stopForFixedWanderer",
          "Stop automation, or show relay warning, when Wandering monsters with fixed turn are due"
        },
      };

      this.setOptions(options);
    }
  }

  private class ItemOptionsPanel extends OptionsPanel {
    /** Constructs a new <code>ItemOptionsPanel</code> */
    public ItemOptionsPanel() {
      super(new Dimension(20, 16), new Dimension(370, 16));

      String helpText =
          "The following settings control \"automated\" item acquisition: the \"acquire\" command and other commands, such as \"eat\", \"equip\", or \"use\", that use \"acquire\" to get items into inventory for further manipulation.";

      String[][] options = {
        {"allowNegativeTally", "Allow item counts in session results to go negative"},
        {},
        {"cloverProtectActive", "Protect against accidental ten-leaf clover usage"},
        {"mementoListActive", "Prevent accidental destruction of 'memento' items"},

        // The following cannot be right, but it will
        // require work in ActionVerifyPanel to fix it.
        {},
        {helpText},
        {},
        {},
        {},
        {"autoSatisfyWithNPCs", "Buy items from NPC stores whenever needed", "yes"},
        {
          "autoSatisfyWithStorage",
          "If you are out of Ronin, pull items from storage whenever needed",
          "yes"
        },
        {
          "autoSatisfyWithCoinmasters",
          "Buy items with tokens at coin masters whenever needed",
          "yes"
        },
        {"autoSatisfyWithMall", "Buy items from the mall whenever needed"},
        {"autoSatisfyWithCloset", "Take items from the closet whenever needed", "yes"},
        {"autoSatisfyWithStash", "Take items from the clan stash whenever needed"},
        {},
        {"autoGarish", "Use Potion of the Field Gar when appropriate (& include in adv gain)"},
        {"autoTuxedo", "Wear Tuxedo when when appropriate (& include in adv gain)"},
        {"autoPinkyRing", "Wear Mafia Pinky Ring when when appropriate (& include in adv gain)"},
        {"autoFillMayoMinder", "Fill Mayo Minder&trade; automatically when appropriate"},
      };

      this.setOptions(options);
    }
  }

  private class DebugOptionsPanel extends OptionsPanel {
    /** Constructs a new <code>DebugOptionsPanel</code> */
    public DebugOptionsPanel() {
      super(new Dimension(20, 16), new Dimension(370, 16));

      String[][] options = {
        {"useLastUserAgent", "Use last browser's userAgent"},
        {"logBrowserInteractions", "Verbosely log communication between KoLmafia and browser"},
        {"logCleanedHTML", "Log cleaned HTML tree of fight pages"},
        {"logReadableHTML", "Include line breaks in logged HTML"},
        {"logDecoratedResponses", "Log decorated responses in debug log"},
        {"logChatRequests", "Include chat-related requests in debug log"},
      };

      this.setOptions(options);
    }
  }

  private class IotMTrackingPanel extends OptionsPanel {
    /** Constructs a new <code>IotMTrackingPanel</code> */
    public IotMTrackingPanel() {
      super(new Dimension(20, 16), new Dimension(370, 16));

      String helpText =
          "Some Items Of The Month have daily passes, and so KoLMafia cannot tell if you have them from seeing the zone. You can mark them here instead.";

      String[][] options = {
        {helpText},
        {},
        {"sleazeAirportAlways", "Have Spring Break Beach"},
        {"spookyAirportAlways", "Have Conspiracy Island"},
        {"stenchAirportAlways", "Have Dinseylandfill"},
        {"hotAirportAlways", "Have That 70s Volcano"},
        {"coldAirportAlways", "Have The Glaciest"},
        {"gingerbreadCityAvailable", "Have Gingerbread City"},
        {"spacegateAlways", "Have Spacegate"},
        {"frAlways", "<html>Have FantasyRealm&trade;</html>"},
        {"prAlways", "<html>Have PirateRealm&trade;</tml>"},
        {"neverendingPartyAlways", "Have Neverending Party"},
        {"voteAlways", "Have Voter Registration"},
        {"daycareOpen", "Have Boxing Daycare"},
      };

      this.setOptions(options);
    }
  }

  private abstract class ShiftableOrderPanel extends ScrollablePanel<JList<String>>
      implements ListDataListener {
    public final LockableListModel<String> list;
    public final JList<String> elementList;

    public ShiftableOrderPanel(final String title, final LockableListModel<String> list) {
      super(title, "move up", "move down", new JList<>(list));

      this.elementList = this.scrollComponent;
      this.elementList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

      this.list = list;
      list.addListDataListener(this);
    }

    @Override
    public void dispose() {
      this.list.removeListDataListener(this);
      super.dispose();
    }

    @Override
    public final void actionConfirmed() {
      int index = this.elementList.getSelectedIndex();
      if (index == -1 || index == 0) {
        return;
      }

      String value = this.list.remove(index);
      this.list.add(index - 1, value);
      this.elementList.setSelectedIndex(index - 1);
    }

    @Override
    public final void actionCancelled() {
      int index = this.elementList.getSelectedIndex();
      if (index == -1 || index == this.list.size() - 1) {
        return;
      }

      String value = this.list.remove(index);
      this.list.add(index + 1, value);
      this.elementList.setSelectedIndex(index + 1);
    }

    public void intervalAdded(final ListDataEvent e) {
      this.saveSettings();
    }

    public void intervalRemoved(final ListDataEvent e) {
      this.saveSettings();
    }

    public void contentsChanged(final ListDataEvent e) {
      this.saveSettings();
    }

    public abstract void saveSettings();
  }

  private class ScriptButtonPanel extends ShiftableOrderPanel {
    public ScriptButtonPanel() {
      super("gCLI Toolbar Buttons", new LockableListModel<>());
      String[] scriptList = Preferences.getString("scriptList").split(" +\\| +");

      this.list.addAll(Arrays.asList(scriptList));

      JPanel extraButtons = new JPanel(new BorderLayout(2, 2));

      JButton addScriptButton = new JButton("script file");
      addScriptButton.addActionListener(new AddScriptListener());
      extraButtons.add(addScriptButton, BorderLayout.NORTH);

      JButton addCommandButton = new JButton("cli command");
      addCommandButton.addActionListener(new AddCommandListener());
      extraButtons.add(addCommandButton, BorderLayout.CENTER);

      JButton deleteListingButton = new JButton("delete");
      deleteListingButton.addActionListener(new DeleteListingListener());
      extraButtons.add(deleteListingButton, BorderLayout.SOUTH);

      this.buttonPanel.add(extraButtons, BorderLayout.SOUTH);
    }

    private class AddScriptListener implements ActionListener {
      public void actionPerformed(final ActionEvent e) {
        File input = InputFieldUtilities.chooseInputFile(KoLConstants.SCRIPT_LOCATION, null);
        if (input == null) {
          return;
        }

        try {
          String rootPath = KoLConstants.SCRIPT_LOCATION.getCanonicalPath();
          String scriptPath = input.getCanonicalPath();
          if (scriptPath.startsWith(rootPath)) {
            scriptPath = scriptPath.substring(rootPath.length() + 1);
          }

          ScriptButtonPanel.this.list.add("call " + scriptPath);
          ScriptButtonPanel.this.saveSettings();
        } catch (IOException ioe) {
        }
      }
    }

    private class AddCommandListener implements ActionListener {
      public void actionPerformed(final ActionEvent e) {
        String currentValue = InputFieldUtilities.input("Enter the desired CLI Command");
        if (currentValue == null || currentValue.length() == 0) {
          return;
        }

        ScriptButtonPanel.this.list.add(currentValue);
        ScriptButtonPanel.this.saveSettings();
      }
    }

    private class DeleteListingListener implements ActionListener {
      public void actionPerformed(final ActionEvent e) {
        int index = ScriptButtonPanel.this.elementList.getSelectedIndex();
        if (index == -1) {
          return;
        }

        ScriptButtonPanel.this.list.remove(index);
        ScriptButtonPanel.this.saveSettings();
      }
    }

    @Override
    public void saveSettings() {
      StringBuilder settingString = new StringBuilder();
      if (this.list.size() != 0) {
        settingString.append(this.list.getElementAt(0));
      }

      for (int i = 1; i < this.list.getSize(); ++i) {
        settingString.append(" | ");
        settingString.append(this.list.getElementAt(i));
      }

      Preferences.setString("scriptList", settingString.toString());
    }
  }

  private class MaximizerStringsPanel extends ShiftableOrderPanel {
    public MaximizerStringsPanel() {
      super("Modifier Maximizer Strings", new LockableListModel<>());
      String[] scriptList = Preferences.getString("maximizerList").split(" +\\| +");

      this.list.addAll(Arrays.asList(scriptList));

      JPanel extraButtons = new JPanel(new BorderLayout(2, 2));
      extraButtons.add(new ThreadedButton("add", new AddMaximizerRunnable()), BorderLayout.CENTER);
      extraButtons.add(
          new ThreadedButton("delete", new DeleteListingRunnable()), BorderLayout.SOUTH);
      this.buttonPanel.add(extraButtons, BorderLayout.SOUTH);
    }

    private class AddMaximizerRunnable implements Runnable {
      public void run() {
        String currentValue = InputFieldUtilities.input("Enter the desired maximizer string");
        if (currentValue != null && currentValue.length() != 0) {
          MaximizerStringsPanel.this.list.add(currentValue);
        }

        MaximizerStringsPanel.this.saveSettings();
      }
    }

    private class DeleteListingRunnable implements Runnable {
      public void run() {
        int index = MaximizerStringsPanel.this.elementList.getSelectedIndex();
        if (index == -1) {
          return;
        }

        MaximizerStringsPanel.this.list.remove(index);
        MaximizerStringsPanel.this.saveSettings();
      }
    }

    @Override
    public void saveSettings() {
      StringBuilder settingString = new StringBuilder();
      if (this.list.size() != 0) {
        settingString.append(this.list.getElementAt(0));
      }

      for (int i = 1; i < this.list.getSize(); ++i) {
        settingString.append(" | ");
        settingString.append(this.list.getElementAt(i));
      }

      Preferences.setString("maximizerList", settingString.toString());
    }
  }

  /** Panel used for handling maximizer related options */
  private class MaximizerOptionsPanel extends GenericPanel implements FocusListener {
    private final JTextField combinationsField;
    private final JTextField mruField;
    private final JTextField priceField;
    private final JCheckBox currentMallBox;
    private final JCheckBox noAdvBox;
    private final JCheckBox alwaysCurrentBox;
    private final JCheckBox foldBox;
    private final JCheckBox verboseBox;
    private final JCheckBox incAllBox;
    private final JCheckBox createBox;
    private final JCheckBox singleFilterBox;
    private final SmartButtonGroup equipmentSelect;
    private final SmartButtonGroup priceSelect;

    public MaximizerOptionsPanel() {
      super(new Dimension(30, 16), new Dimension(370, 16));

      combinationsField = new JTextField(8);
      this.combinationsField.addFocusListener(this);
      this.mruField = new JTextField(4);
      this.mruField.addFocusListener(this);
      this.priceField = new JTextField(8);
      this.priceField.addFocusListener(this);
      this.currentMallBox = new JCheckBox();
      this.currentMallBox.addFocusListener(this);
      this.noAdvBox = new JCheckBox();
      this.noAdvBox.addFocusListener(this);
      this.alwaysCurrentBox = new JCheckBox();
      this.alwaysCurrentBox.addFocusListener(this);
      this.foldBox = new JCheckBox();
      this.foldBox.addFocusListener(this);
      this.verboseBox = new JCheckBox();
      this.verboseBox.addFocusListener(this);
      this.incAllBox = new JCheckBox();
      this.incAllBox.addFocusListener(this);
      this.createBox = new JCheckBox();
      this.createBox.addFocusListener(this);
      this.singleFilterBox = new JCheckBox();
      this.singleFilterBox.addFocusListener(this);

      // Feels kludgy, but makes sure that column width for text fields are respected
      JPanel combinationsPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
      combinationsPanel.add(combinationsField);
      JPanel mruPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
      mruPanel.add(mruField);
      JPanel maxPricePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
      maxPricePanel.add(priceField);

      JPanel equipPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
      this.equipmentSelect = new SmartButtonGroup(equipPanel);
      this.equipmentSelect.add(new JRadioButton("on hand"));
      this.equipmentSelect.add(new JRadioButton("creatable"));
      this.equipmentSelect.add(new JRadioButton("pullable/buyable"));

      JPanel pricePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
      this.priceSelect = new SmartButtonGroup(pricePanel);
      this.priceSelect.add(new JRadioButton("don't check"));
      this.priceSelect.add(new JRadioButton("buyable only"));
      this.priceSelect.add(new JRadioButton("all consumables"));

      VerifiableElement[] elements = new VerifiableElement[13];

      elements[0] = new VerifiableElement("Consider items by default: ", equipPanel, false);
      elements[1] =
          new VerifiableElement(
              "Consider foldable items in Maximizer by default", SwingConstants.LEFT, this.foldBox);
      elements[2] =
          new VerifiableElement(
              "Always consider non-equipment creations as on hand",
              SwingConstants.LEFT,
              this.createBox);
      elements[3] =
          new VerifiableElement(
              "Always consider current equipment outside Hardcore / Ronin",
              SwingConstants.LEFT,
              this.alwaysCurrentBox);
      elements[4] =
          new VerifiableElement(
              "Do not show effects that cost adventures", SwingConstants.LEFT, this.noAdvBox);
      elements[5] =
          new VerifiableElement(
              "Maximum number of combinations to consider (0 for no max)",
              SwingConstants.LEFT,
              combinationsPanel);
      elements[6] = new VerifiableElement("Price check by default:", pricePanel, false);
      elements[7] =
          new VerifiableElement(
              "Max purchase price by default", SwingConstants.LEFT, maxPricePanel);
      elements[8] =
          new VerifiableElement(
              "Check Mall prices every session (not using historical prices)",
              SwingConstants.LEFT,
              this.currentMallBox);
      elements[9] =
          new VerifiableElement(
              "Show cost, turns of effect and number of casts/items remaining",
              SwingConstants.LEFT,
              this.verboseBox);
      elements[10] =
          new VerifiableElement(
              "Show all, effects with no direct source, skills you don't have, etc.",
              SwingConstants.LEFT,
              this.incAllBox);
      elements[11] =
          new VerifiableElement("Recent maximizer string buffer", SwingConstants.LEFT, mruPanel);
      elements[12] =
          new VerifiableElement(
              "Treat filter checkboxes as an exclusive group (will close Maximizer)",
              SwingConstants.LEFT,
              this.singleFilterBox);

      this.actionCancelled();
      this.setContent(elements);
    }

    @Override
    public boolean shouldAddStatusLabel() {
      return false;
    }

    @Override
    public void setEnabled(final boolean isEnabled) {}

    @Override
    public void actionConfirmed() {
      Preferences.setInteger(
          "maximizerCombinationLimit", InputFieldUtilities.getValue(this.combinationsField, 0));
      Preferences.setInteger("maximizerMRUSize", InputFieldUtilities.getValue(this.mruField, 0));
      Preferences.setBoolean("maximizerCurrentMallPrices", this.currentMallBox.isSelected());
      Preferences.setBoolean("maximizerNoAdventures", this.noAdvBox.isSelected());
      Preferences.setBoolean("maximizerAlwaysCurrent", this.alwaysCurrentBox.isSelected());
      Preferences.setBoolean("maximizerFoldables", this.foldBox.isSelected());
      Preferences.setBoolean("verboseMaximizer", this.verboseBox.isSelected());
      Preferences.setBoolean("maximizerIncludeAll", this.incAllBox.isSelected());
      Preferences.setBoolean("maximizerCreateOnHand", this.createBox.isSelected());
      Preferences.setInteger("maximizerEquipmentScope", this.equipmentSelect.getSelectedIndex());
      Preferences.setInteger("maximizerMaxPrice", InputFieldUtilities.getValue(this.priceField, 0));
      Preferences.setInteger("maximizerPriceLevel", this.priceSelect.getSelectedIndex());

      if (this.singleFilterBox.isSelected() != Preferences.getBoolean("maximizerSingleFilter")) {
        // redraw Maximizer
        Frame[] frames = Frame.getFrames();
        for (Frame f : frames) {
          if (f.getTitle().contains("Modifier Maximizer")) {
            f.dispose();
          }
        }
      }
      Preferences.setBoolean("maximizerSingleFilter", this.singleFilterBox.isSelected());
    }

    @Override
    public void actionCancelled() {
      this.combinationsField.setText(Preferences.getString("maximizerCombinationLimit"));
      this.mruField.setText(Preferences.getString("maximizerMRUSize"));
      this.currentMallBox.setSelected(Preferences.getBoolean("maximizerCurrentMallPrices"));
      this.noAdvBox.setSelected(Preferences.getBoolean("maximizerNoAdventures"));
      this.alwaysCurrentBox.setSelected(Preferences.getBoolean("maximizerAlwaysCurrent"));
      this.foldBox.setSelected(Preferences.getBoolean("maximizerFoldables"));
      this.verboseBox.setSelected(Preferences.getBoolean("verboseMaximizer"));
      this.incAllBox.setSelected(Preferences.getBoolean("maximizerIncludeAll"));
      this.createBox.setSelected(Preferences.getBoolean("maximizerCreateOnHand"));
      this.equipmentSelect.setSelectedIndex(Preferences.getInteger("maximizerEquipmentScope"));
      this.priceField.setText(Preferences.getString("maximizerMaxPrice"));
      this.priceSelect.setSelectedIndex(Preferences.getInteger("maximizerPriceLevel"));
      this.singleFilterBox.setSelected(Preferences.getBoolean("maximizerSingleFilter"));
    }

    public void focusLost(final FocusEvent e) {
      MaximizerOptionsPanel.this.actionConfirmed();
    }

    public void focusGained(final FocusEvent e) {}
  }

  /**
   * Panel used for handling chat-related options and preferences, including font size, window
   * management and maybe, eventually, coloring options for contacts.
   */
  private class ChatOptionsPanel extends OptionsPanel {
    private ButtonGroup fontSizeGroup;
    private JRadioButton[] fontSizes;
    private JLabel innerGradient, outerGradient;

    public ChatOptionsPanel() {
      super(new Dimension(30, 17), new Dimension(470, 17));

      String[][] options = {
        {"useTabbedChatFrame", "Use tabbed, rather than multi-window, chat"},
        {"useShinyTabbedChat", "Use shiny closeable tabs when using tabbed chat"},
        {"addChatCommandLine", "Add a graphical CLI to tabbed chat"},
        {},
        {"useContactsFrame", "Use a popup window for /friends and /who"},
        {"chatLinksUseRelay", "Use the relay browser when clicking on chat links"},
        {"useChatToolbar", "Add a toolbar to chat windows for special commands"},
        {},
        {"mergeHobopolisChat", "Merge clan dungeon channel displays into /clan"},
        {"useHugglerChannel", "Put Huggler Radio announcements into /huggler"},
        {"greenScreenProtection", "Ignore event messages in KoLmafia chat"},
        {"broadcastEvents", "Send event messages to all open tabs in KoLmafia chat"},
        {"logChatMessages", "Log chats when using KoLmafia (requires restart)"},
        {"chatServesUpdates", "Send recent chat messages when a clan member requests an update"},
      };

      this.setOptions(options);
    }

    @Override
    public void setContent(VerifiableElement[] elements) {
      this.fontSizeGroup = new ButtonGroup();
      this.fontSizes = new JRadioButton[3];
      for (int i = 0; i < 3; ++i) {
        this.fontSizes[i] = new JRadioButton();
        this.fontSizeGroup.add(this.fontSizes[i]);
      }

      VerifiableElement[] newElements = new VerifiableElement[elements.length + 7];

      newElements[0] =
          new VerifiableElement(
              "Use small fonts in hypertext displays", SwingConstants.LEFT, this.fontSizes[0]);
      newElements[1] =
          new VerifiableElement(
              "Use medium fonts in hypertext displays", SwingConstants.LEFT, this.fontSizes[1]);
      newElements[2] =
          new VerifiableElement(
              "Use large fonts in hypertext displays", SwingConstants.LEFT, this.fontSizes[2]);

      newElements[3] = new VerifiableElement();

      System.arraycopy(elements, 0, newElements, 4, elements.length);

      int tabCount = elements.length + 4;

      newElements[tabCount++] = new VerifiableElement();

      this.outerGradient = new TabColorChanger("outerChatColor");
      newElements[tabCount++] =
          new VerifiableElement(
              "Change the outer portion of highlighted tab gradient",
              SwingConstants.LEFT,
              this.outerGradient);

      this.innerGradient = new TabColorChanger("innerChatColor");
      newElements[tabCount++] =
          new VerifiableElement(
              "Change the inner portion of highlighted tab gradient",
              SwingConstants.LEFT,
              this.innerGradient);

      super.setContent(newElements);
    }

    @Override
    public void actionConfirmed() {
      super.actionConfirmed();

      if (this.fontSizes[0].isSelected()) {
        Preferences.setString("chatFontSize", "small");
      } else if (this.fontSizes[1].isSelected()) {
        Preferences.setString("chatFontSize", "medium");
      } else if (this.fontSizes[2].isSelected()) {
        Preferences.setString("chatFontSize", "large");
      }

      KoLConstants.commandBuffer.append(null);
    }

    @Override
    public void actionCancelled() {
      super.actionCancelled();

      this.innerGradient.setBackground(tab.CloseTabPaneEnhancedUI.notifiedA);
      this.outerGradient.setBackground(tab.CloseTabPaneEnhancedUI.notifiedB);

      String fontSize = Preferences.getString("chatFontSize");
      this.fontSizes[fontSize.equals("large") ? 2 : fontSize.equals("medium") ? 1 : 0].setSelected(
          true);
    }

    private class TabColorChanger extends ColorChooser {
      public TabColorChanger(final String property) {
        super(property);
      }

      @Override
      public void applyChanges() {
        if (this.property.equals("innerChatColor")) {
          CloseTabPaneEnhancedUI.notifiedA = ChatOptionsPanel.this.innerGradient.getBackground();
        } else {
          CloseTabPaneEnhancedUI.notifiedB = ChatOptionsPanel.this.outerGradient.getBackground();
        }
      }
    }
  }

  /** A special panel which generates a list of bookmarks which can subsequently be managed. */
  private class BookmarkManagePanel extends ShiftableOrderPanel {
    public BookmarkManagePanel() {
      super("Configure Bookmarks", (LockableListModel<String>) KoLConstants.bookmarks);

      JPanel extraButtons = new JPanel(new BorderLayout(2, 2));
      extraButtons.add(new ThreadedButton("add", new AddBookmarkRunnable()), BorderLayout.NORTH);
      extraButtons.add(
          new ThreadedButton("rename", new RenameBookmarkRunnable()), BorderLayout.CENTER);
      extraButtons.add(
          new ThreadedButton("delete", new DeleteBookmarkRunnable()), BorderLayout.SOUTH);
      this.buttonPanel.add(extraButtons, BorderLayout.SOUTH);
    }

    @Override
    public void saveSettings() {
      GenericFrame.saveBookmarks();
    }

    private class AddBookmarkRunnable implements Runnable {
      public void run() {
        String newName = InputFieldUtilities.input("Add a bookmark!", "http://www.google.com/");

        if (newName == null) {
          return;
        }

        KoLConstants.bookmarks.add(
            "New bookmark "
                + (KoLConstants.bookmarks.size() + 1)
                + "|"
                + newName
                + "|"
                + newName.contains("pwd"));
      }
    }

    private class RenameBookmarkRunnable implements Runnable {
      public void run() {
        int index = BookmarkManagePanel.this.elementList.getSelectedIndex();
        if (index == -1) {
          return;
        }

        String currentItem = BookmarkManagePanel.this.elementList.getSelectedValue();
        if (currentItem == null) {
          return;
        }

        String[] bookmarkData = currentItem.split("\\|");

        String name = bookmarkData[0];
        String location = bookmarkData[1];
        String pwdhash = bookmarkData[2];

        String newName = InputFieldUtilities.input("Rename your bookmark?", name);

        if (newName == null) {
          return;
        }

        KoLConstants.bookmarks.remove(index);
        KoLConstants.bookmarks.add(newName + "|" + location + "|" + pwdhash);
      }
    }

    private class DeleteBookmarkRunnable implements Runnable {
      public void run() {
        int index = BookmarkManagePanel.this.elementList.getSelectedIndex();
        if (index == -1) {
          return;
        }

        KoLConstants.bookmarks.remove(index);
      }
    }
  }

  protected class StartupFramesPanel extends GenericPanel implements ListDataListener {
    private boolean isRefreshing = false;

    private final LockableListModel<String> completeList = new LockableListModel<>();
    private final LockableListModel<String> startupList = new LockableListModel<>();
    private final LockableListModel<String> desktopList = new LockableListModel<>();

    public StartupFramesPanel() {
      super();
      this.setContent(null);

      for (String[] frame : KoLConstants.FRAME_NAMES) {
        this.completeList.add(frame[0]);
      }

      JPanel optionPanel = new JPanel(new GridLayout(1, 3));
      optionPanel.add(new ScrollablePanel<>("Complete List", new JDnDList(this.completeList)));
      optionPanel.add(new ScrollablePanel<>("Startup as Window", new JDnDList(this.startupList)));
      optionPanel.add(new ScrollablePanel<>("Startup in Tabs", new JDnDList(this.desktopList)));

      JTextArea message =
          new JTextArea(
              "These are the global settings for what shows up when KoLmafia successfully logs into the Kingdom of Loathing.  You can drag and drop options in the lists below to customize what will show up.\n\n"
                  + "When you place the Local Relay Server into the 'startup in tabs' section, KoLmafia will start up the server but not open your browser.  When you place the Contact List into the 'startup in tabs' section, KoLmafia will force a refresh of your contact list on login.\n");

      // message.setColumns( 32 );
      message.setLineWrap(true);
      message.setWrapStyleWord(true);
      message.setEditable(false);
      message.setOpaque(false);
      message.setFont(KoLGUIConstants.DEFAULT_FONT);

      this.container.add(message, BorderLayout.NORTH);
      this.container.add(optionPanel, BorderLayout.SOUTH);
      this.actionCancelled();

      this.completeList.addListDataListener(this);
      this.startupList.addListDataListener(this);
      this.desktopList.addListDataListener(this);
    }

    @Override
    public void dispose() {
      this.completeList.removeListDataListener(this);
      this.startupList.removeListDataListener(this);
      this.desktopList.removeListDataListener(this);

      super.dispose();
    }

    @Override
    public void actionConfirmed() {
      this.actionCancelled();
    }

    @Override
    public void actionCancelled() {
      this.isRefreshing = true;

      String username =
          (String) ((SortedListModel<String>) KoLConstants.saveStateNames).getSelectedItem();
      if (username == null) {
        username = "";
      }

      this.startupList.clear();
      this.desktopList.clear();

      KoLmafiaGUI.checkFrameSettings();

      String frameString = Preferences.getString("initialFrames");
      String desktopString = Preferences.getString("initialDesktop");

      String[] pieces;

      pieces = frameString.split(",");
      for (String piece : pieces) {
        for (String[] frame : KoLConstants.FRAME_NAMES) {
          if (!this.startupList.contains(frame[0]) && frame[1].equals(piece)) {
            this.startupList.add(frame[0]);
          }
        }
      }

      pieces = desktopString.split(",");
      for (String piece : pieces) {
        for (String[] frame : KoLConstants.FRAME_NAMES) {
          if (!this.desktopList.contains(frame[0]) && frame[1].equals(piece)) {
            this.desktopList.add(frame[0]);
          }
        }
      }

      this.isRefreshing = false;
      this.saveLayoutSettings();
    }

    public boolean shouldAddStatusLabel(final VerifiableElement[] elements) {
      return false;
    }

    @Override
    public void setEnabled(final boolean isEnabled) {}

    public void intervalAdded(final ListDataEvent e) {
      Object src = e.getSource();
      if (src == this.startupList) {
        this.desktopList.removeAll(this.startupList);
      } else if (src == this.desktopList) {
        this.startupList.removeAll(this.desktopList);
      } else if (src == this.completeList) {
        String item = this.completeList.get(e.getIndex0());
        this.desktopList.remove(item);
        this.startupList.remove(item);
      }

      this.saveLayoutSettings();
    }

    public void intervalRemoved(final ListDataEvent e) {
      this.saveLayoutSettings();
    }

    public void contentsChanged(final ListDataEvent e) {}

    public void saveLayoutSettings() {
      if (this.isRefreshing) {
        return;
      }

      StringBuilder frameString = new StringBuilder();
      StringBuilder desktopString = new StringBuilder();

      for (int i = 0; i < this.startupList.getSize(); ++i) {
        for (String[] frame : KoLConstants.FRAME_NAMES) {
          if (this.startupList.getElementAt(i).equals(frame[0])) {
            if (frameString.length() != 0) {
              frameString.append(",");
            }
            frameString.append(frame[1]);
          }
        }
      }

      for (int i = 0; i < this.desktopList.getSize(); ++i) {
        for (String[] frame : KoLConstants.FRAME_NAMES) {
          if (this.desktopList.getElementAt(i).equals(frame[0])) {
            if (desktopString.length() != 0) {
              desktopString.append(",");
            }
            desktopString.append(frame[1]);
          }
        }
      }

      Preferences.setString("initialFrames", frameString.toString());
      Preferences.setString("initialDesktop", desktopString.toString());
    }
  }

  private class DeedsButtonPanel extends ScrollablePanel<JDnDList> implements ListDataListener {
    public DeedsButtonPanel(final String title, final LockableListModel<String> builtIns) {
      super(title, "add custom", "reset deeds", new JDnDList(builtIns));

      this.buttonPanel.add(new ThreadedButton("help", new HelpRunnable()), BorderLayout.CENTER);
    }

    @Override
    public final void actionConfirmed() {
      if (KoLCharacter.baseUserName().equals("GLOBAL")) {
        InputFieldUtilities.alert("You must be logged in to use the custom deed builder.");
      } else {
        new AddCustomDeedsPanel();
      }
    }

    @Override
    public final void actionCancelled() {
      boolean reset =
          InputFieldUtilities.confirm(
              "This will reset your deeds to the default settings.\nAre you sure?");
      if (reset) {
        Preferences.resetToDefault("dailyDeedsOptions");
      }
    }

    public void intervalAdded(final ListDataEvent e) {
      this.saveSettings();
    }

    public void intervalRemoved(final ListDataEvent e) {
      this.saveSettings();
    }

    public void contentsChanged(final ListDataEvent e) {
      this.saveSettings();
    }

    private class HelpRunnable implements Runnable {
      JOptionPane pane;

      public HelpRunnable() {
        String message =
            "<html><table width=750><tr><td>"
                + "<b>NOTE:</b> If you want to use the Custom Deed Builder, click the \"Add Custom\" button; the following instructions are used for manually editing the dailyDeedsOptions user preference.<br>"
                + "------------------------<br>"
                + "All deeds are specified by one comma-delimited preference \"dailyDeedsOptions\".  Order matters.  Built-in deeds are simply called by referring to their built-in name; these are viewable by pulling up the Daily Deeds tab and looking in the \"Built-in Deeds\" list."
                + "<h3><b>Custom Deeds</b></h3>"
                + "Custom deeds provide the user with a way of adding buttons or text to their daily deeds panel that is not natively provided for.  All deeds start with the keyword <b>$CUSTOM</b> followed by a pipe (|) symbol.  As you are constructing a custom deed, you separate the different arguments with pipes.<br>"
                + "<br>"
                + "All deed types except for Text require a preference to track.  If you want to add a button that is always enabled, you will have to create a dummy preference that is always false.<br>"
                + "<br>"
                + "There are currently 6 different types of custom deeds.  Remember that all of these \"acceptable forms\" are prefaced by $CUSTOM|.<br>"
                + "<br>"
                + "<b>Simple</b> - Designed for users who do not want to bother with preferences.  Will be disabled after the first click.<br>"
                + "acceptable forms:<br>"
                + "Simple|displayText<br>"
                + "Simple|displayText|command<br>"
                + "Simple|displayText|command|maxUses<br>"
                + "<br>"
                + "displayText - the text that will be displayed on the button<br>"
                + "command - the command to execute. If not specified, will default to displayText.<br>"
                + "maxUses - an arbitrary integer. Specifies a threshold to disable the button at. A counter in the form of <clicks>/<maxUses> will be displayed to the right of the button. <br>"
                + " After clicking a simple deed, the button will be immediately disabled unless maxUses>1 is specified.<br>"
                + "<br>"
                + "<b>Command</b> - execute a command with a button press<br>"
                + "acceptable forms:"
                + "<br>Command|displayText|preference<br>"
                + "Command|displayText|preference|command<br>"
                + "Command|displayText|preference|command|maxUses<br>"
                + "<br>"
                + "displayText - the text that will be displayed on the button<br>"
                + "preference - the preference to track.  The button will be enabled when the preference is less than maxUses (default 1).<br>"
                + "command - the command to execute.  If not specified, will default to displayText.<br>"
                + "maxUses - an arbitrary integer.  Specifies a threshold to disable the button at.  A counter in the form of &lt;preference&gt;/&lt;maxUses&gt; will be displayed to the right of the button.<br>"
                + "<br>"
                + "<b>Item</b> - this button will use fuzzy matching to find the name of the item specified.  Will execute \"use &lt;itemName&gt;\" when clicked.  Will only be visible when you possess one or more of the item.<br>"
                + "acceptable forms:<br>"
                + "Item|displayText|preference<br>"
                + "Item|displayText|preference|itemName<br>"
                + "Item|displayText|preference|itemName|maxUses<br>"
                + "<br>"
                + "itemName - the name of the item that will be used.  If not specified, will default to displayText.<br>"
                + "<br>"
                + "<b>Skill</b> - cast a skill that is tracked by a boolean or int preference.  Will execute \"cast &lt;skillName&gt;\" when clicked.  Will not be visible if you don't know the skill.<br>"
                + "acceptable forms:<br>"
                + "Skill|displayText|preference<br>"
                + "Skill|displayText|preference|skillName<br>"
                + "Skill|displayText|preference|skillName|maxCasts<br>"
                + "<br>"
                + "skillName- the name of the skill that will be cast.  If not specified, will default to displayText.  Must be specified if maxCasts are specified.<br>"
                + "maxCasts - an arbitrary integer.  Specifies a threshold to disable the button at.  A counter in the form of &lt;preference&gt;/&lt;maxCasts&gt; will be displayed to the right of the button.<br>"
                + "<br>"
                + "<b>Text</b><br>"
                + "acceptable forms:<br>"
                + "Text|pretty much anything.<br>"
                + "<br>"
                + "You can supply as many arguments as you want to a Text deed.  Any argument that uniquely matches a preference will be replaced by that preference's value.  If you want to use a comma in your text, immediately follow the comma with a pipe character so it will not be parsed as the end of the Text deed.<br>"
                + "<br>"
                + "<b>Combo</b> - A cleaner way to collapse multiple related command deeds into one combobox element. Note that there is no GUI to help you construct this, you must manually add it to your dailyDeedsOptions preference.<br>"
                + "acceptable forms:<br>"
                + "$CUSTOM|Combo|displayText|preference1|itemBlock<br>"
                + "$CUSTOM|Combo|displayText|preference1|maxUses|itemBlock<br>"
                + "where the itemBlock consists of an arbitrary number of:<br>"
                + "$ITEM|displayText|preferenceN|command|<br>"
                + "<br>"
                + "Preference1 - The preference to track for enabling/disabling the entire combobox. Default threshold is 1 if maxUses is not specified.<br>"
                + "Preference2 - The individual preference to disable each individual item in the combobox. Note that there is no way to supply maxUses for each individual element; 1 (or true) is always the max. "
                + "</td></tr></table></html>";

        JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        DefaultCaret c = new DefaultCaret();
        c.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        textPane.setCaret(c);
        textPane.setText(message);
        textPane.setOpaque(false);
        textPane.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setPreferredSize(new Dimension(800, 550));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        this.pane = new JOptionPane(scrollPane, JOptionPane.PLAIN_MESSAGE);
      }

      public void run() {
        JDialog dialog = this.pane.createDialog(null, "Daily Deeds Help");
        dialog.setModal(false);
        dialog.setVisible(true);
      }
    }

    public void saveSettings() {}
  }

  private class SVNPanel extends JPanel {
    private List<Component> componentQueue = new ArrayList<>();

    public SVNPanel() {
      // 5 px inset
      this.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));
      // box layoutmanager
      this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
      JTextArea message =
          new JTextArea(
              "Configure the behavior of Mafia's built-in SVN client here.\n\n"
                  + "With SVN you can seamlessly install community-created scripts and have them automatically update.") {
            // don't let boxlayout expand the JTextArea ridiculously
            @Override
            public Dimension getMaximumSize() {
              return this.getPreferredSize();
            }
          };

      message.setColumns(40);
      message.setLineWrap(true);
      message.setWrapStyleWord(true);
      message.setEditable(false);
      message.setOpaque(false);
      message.setFont(KoLGUIConstants.DEFAULT_FONT);
      this.queue(message);

      JSeparator sep = new JSeparator();
      // again, JSeparators have unbounded max size, which messes with boxlayout.  Fix it.
      Dimension size = new Dimension(sep.getMaximumSize().width, sep.getPreferredSize().height);
      sep.setMaximumSize(size);
      this.queue(sep);
      this.queue(Box.createVerticalStrut(5));

      /*
       * Basic Options
       */

      this.queue(
          new PreferenceCheckBox("svnUpdateOnLogin", "Update installed SVN projects on login"));
      String tip =
          "<html>Turning this option on will show the associated message that the author<br>"
              + "provided to describe changes in the new version.  This includes things<br>"
              + "like bug fixes, new features, etc.</html>";
      this.queue(
          new PreferenceCheckBox(
              "svnShowCommitMessages", "Show commit messages after update", tip));

      /*
       * End Basic Options
       */

      this.queue(Box.createVerticalStrut(10));
      JLabel label = new JLabel("Advanced options:");
      this.queue(label);
      JSeparator sep2 = new JSeparator();
      size =
          new Dimension(
              label.getFontMetrics(label.getFont()).stringWidth(label.getText()),
              sep.getPreferredSize().height);
      sep2.setMaximumSize(size);
      this.queue(sep2);

      /*
       * Advanced Options
       */

      tip =
          "<html>A script may declare dependencies - i.e. other scripts that should be installed<br>"
              + "along with it.  Those dependencies can declare their own dependencies, and<br>so forth.<br>"
              + "<br>"
              + "Users who want complete control over what they are installing can turn this off,<br>"
              + "but should make sure to manually install dependencies or scripts may<br>malfunction.";
      this.queue(
          new PreferenceCheckBox(
              "svnInstallDependencies",
              "Automatically install dependencies for SVN projects",
              tip));

      tip =
          "<html>If you manually modify your working copies, syncing will ensure that any<br>"
              + "changes you made in the WC (along with merged updates from the repo) are<br>"
              + "transferred to your local copy whenever you perform an svn update.  This is<br>"
              + "equivalent to executing the CLI command <i>svn sync</i>.<br><br>"
              + "If you have not modified your working copies, you do not need this feature.</html>";
      this.queue(new PreferenceCheckBox("syncAfterSvnUpdate", "Sync after svn update", tip));

      /*
       * End Advanced Options
       */

      this.makeLayout();
    }

    private void queue(Component comp) {
      this.componentQueue.add(comp);
    }

    private void makeLayout() {
      for (Component comp : this.componentQueue) {
        if (comp instanceof JComponent) {
          ((JComponent) comp).setAlignmentX(LEFT_ALIGNMENT);
        }
        this.add(comp);
      }
      this.componentQueue = null;
    }
  }

  private class PreferenceCheckBox extends JPanel implements Listener {
    private final String pref;
    private final String tooltip;

    private final JCheckBox box = new JCheckBox();

    public PreferenceCheckBox(String pref, String message) {
      this(pref, message, null);
    }

    public PreferenceCheckBox(String pref, String message, String tip) {
      this.pref = pref;
      this.tooltip = tip;

      configure();
      makeLayout(message);
    }

    private void configure() {
      this.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
      PreferenceListenerRegistry.registerPreferenceListener(pref, this);
      this.box.addActionListener(e -> Preferences.setBoolean(pref, box.isSelected()));
    }

    private void makeLayout(String message) {
      this.add(this.box);
      JLabel label = new JLabel(message);
      this.add(label);

      if (tooltip != null) {
        this.add(Box.createHorizontalStrut(3));
        label = new JLabel("[");
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

      update();
    }

    public void update() {
      this.box.setSelected(Preferences.getBoolean(this.pref));
    }

    @Override
    public Dimension getMaximumSize() {
      return this.getPreferredSize();
    }
  }

  protected class CustomizeDailyDeedsPanel extends GenericPanel
      implements ListDataListener, Listener {
    private boolean isRefreshing = false;

    private final LockableListModel<String> builtInsList = new LockableListModel<>();
    private final LockableListModel<String> deedsList = new LockableListModel<>();

    public CustomizeDailyDeedsPanel() {
      super(new Dimension(2, 2), new Dimension(2, 2));
      this.setContent(null);

      JPanel botPanel = new JPanel(new GridLayout(1, 0, 10, 0));
      JPanel centerPanel = new JPanel(new GridLayout(1, 2, 0, 0));

      for (String[] builtinDeed : DailyDeedsPanel.BUILTIN_DEEDS) {
        this.builtInsList.add(builtinDeed[1]);
      }

      centerPanel.add(new DeedsButtonPanel("Built-In Deeds", this.builtInsList));
      botPanel.add(new ScrollablePanel<>("Current Deeds", new JDnDList(this.deedsList)));

      this.container.add(centerPanel, BorderLayout.PAGE_START);
      this.container.add(botPanel, BorderLayout.PAGE_END);
      this.actionCancelled();

      this.builtInsList.addListDataListener(this);
      this.deedsList.addListDataListener(this);
      PreferenceListenerRegistry.registerPreferenceListener("dailyDeedsOptions", this);
    }

    public CustomizeDailyDeedsPanel(final String string) {
      super(new Dimension(2, 2), new Dimension(2, 2));
      this.setContent(null);

      JTextArea message =
          new JTextArea(
              "Edit the appearance of your daily deeds panel.\n\n"
                  + "Drag built-in deeds into the 'Current Deeds' box down below to include, "
                  + "and delete them from there to exclude.  Drag and drop to rearrange. "
                  + "Note that some deeds added to the 'Current Deeds' box may still remain hidden "
                  + "once you add them depending on whether you posess certain "
                  + "items, skills, and/or access to zones.");

      message.setColumns(40);
      message.setLineWrap(true);
      message.setWrapStyleWord(true);
      message.setEditable(false);
      message.setOpaque(false);
      message.setFont(KoLGUIConstants.DEFAULT_FONT);

      this.container.add(message, BorderLayout.NORTH);
    }

    @Override
    public void dispose() {
      this.builtInsList.removeListDataListener(this);
      this.deedsList.removeListDataListener(this);

      super.dispose();
    }

    @Override
    public void actionConfirmed() {
      this.actionCancelled();
    }

    @Override
    public void actionCancelled() {
      this.isRefreshing = true;
      String deedsString = Preferences.getString("dailyDeedsOptions");
      String[] pieces;

      pieces = deedsString.split(",(?!\\|)");

      this.deedsList.clear();

      KoLmafiaGUI.checkFrameSettings();

      for (String piece : pieces) {
        for (String[] builtinDeed : DailyDeedsPanel.BUILTIN_DEEDS) {
          String builtinDeedName = builtinDeed[1];

          if (builtinDeedName.equals(piece) && !this.deedsList.contains(builtinDeedName)) {
            this.deedsList.add(builtinDeedName);
            break;
          }
        }
        if (piece.startsWith("$CUSTOM|")) {
          this.deedsList.add(piece);
        }
      }

      this.isRefreshing = false;
      this.saveLayoutSettings();
    }

    public boolean shouldAddStatusLabel(final VerifiableElement[] elements) {
      return false;
    }

    @Override
    public void setEnabled(final boolean isEnabled) {}

    public void intervalAdded(final ListDataEvent e) {
      Object src = e.getSource();

      if (src == this.builtInsList) {
        String item = this.builtInsList.get(e.getIndex0());

        this.deedsList.remove(item);
      }

      this.saveLayoutSettings();
    }

    public void intervalRemoved(final ListDataEvent e) {
      this.saveLayoutSettings();
    }

    public void contentsChanged(final ListDataEvent e) {}

    public void saveLayoutSettings() {
      if (this.isRefreshing) {
        return;
      }

      List<String> frameStrings = new ArrayList<>();

      for (int i = 0; i < this.deedsList.getSize(); ++i) {
        String listedDeed = this.deedsList.getElementAt(i).toString();

        if (listedDeed.startsWith("$CUSTOM|")) {
          frameStrings.add(listedDeed);
          continue;
        }
        for (String[] builtinDeed : DailyDeedsPanel.BUILTIN_DEEDS) {
          String builtinDeedName = builtinDeed[1];

          if (listedDeed.equals(builtinDeedName)) {
            frameStrings.add(builtinDeedName);
            break;
          }
        }
      }

      Preferences.setString("dailyDeedsOptions", String.join(",", frameStrings));
    }

    public void update() {
      this.actionCancelled();
    }
  }

  /** Allows the user to select to select the framing mode to use. */
  protected class UserInterfacePanel extends OptionsPanel implements Listener {
    private JCheckBox[] optionBoxes;

    private final String[][] options =
        System.getProperty("os.name").startsWith("Windows")
            ? new String[][] {
              {"guiUsesOneWindow", "Restrict interface to a single window"},
              {"useSystemTrayIcon", "Minimize main interface to system tray"},
              {"addCreationQueue", "Add creation queueing interface to item manager"},
              {"addStatusBarToFrames", "Add a status line to independent windows"},
              {"autoHighlightOnFocus", "Highlight text fields when selected"},
              {},
              {"useDecoratedTabs", "Use shiny decorated tabs instead of OS default"},
              {"allowCloseableDesktopTabs", "Allow tabs on main window to be closed"},
            }
            : System.getProperty("os.name").startsWith("Mac")
                ? new String[][] {
                  {"guiUsesOneWindow", "Restrict interface to a single window"},
                  {"useDockIconBadge", "Show turns remaining on Dock icon (OSX 10.5+)"},
                  {"addCreationQueue", "Add creation queueing interface to item manager"},
                  {"addStatusBarToFrames", "Add a status line to independent windows"},
                  {"autoHighlightOnFocus", "Highlight text fields when selected"},
                  {},
                  {"useDecoratedTabs", "Use shiny decorated tabs instead of OS default"},
                  {"allowCloseableDesktopTabs", "Allow tabs on main window to be closed"},
                  // { "darkThemeToolIconOverride", "Always use dark toolbar icons with dark Look
                  // and Feel" },
                }
                : new String[][] {
                  {"guiUsesOneWindow", "Restrict interface to a single window"},
                  {"addCreationQueue", "Add creation queueing interface to item manager"},
                  {"addStatusBarToFrames", "Add a status line to independent windows"},
                  {"autoHighlightOnFocus", "Highlight text fields when selected"},
                  {},
                  {"useDecoratedTabs", "Use shiny decorated tabs instead of OS default"},
                  {"allowCloseableDesktopTabs", "Allow tabs on main window to be closed"},
                };

    private final JComboBox<String> looks, toolbars, toolIcons, scripts;

    public UserInterfacePanel() {
      super(new Dimension(80, 22), new Dimension(280, 22));
      PreferenceListenerRegistry.registerPreferenceListener("swingLookAndFeel", this);

      UIManager.LookAndFeelInfo[] installed = UIManager.getInstalledLookAndFeels();
      String[] installedLooks = new String[installed.length + 1];
      String CurrentLook = getLookAndFeel().getClass().getName();

      installedLooks[0] = "Always use OS default look and feel";

      this.looks = new JComboBox<>();
      this.looks.addItem(installedLooks[0]);

      for (int i = 0; i < installed.length; ++i) {
        // TODO: Add filter based on checkboxes (to be added) to show dark or light themes
        installedLooks[i + 1] = installed[i].getClassName();
        this.looks.addItem(installed[i].getName());
        if (installed[i].getClassName().equalsIgnoreCase(CurrentLook)) {
          this.looks.setSelectedIndex(i + 1);
        }
      }

      this.toolbars = new JComboBox<>();
      this.toolbars.addItem("Show global menus only");
      this.toolbars.addItem("Put toolbar along top of panel");
      this.toolbars.addItem("Put toolbar along bottom of panel");
      this.toolbars.addItem("Put toolbar along left of panel");

      this.toolIcons = new JComboBox<>();
      this.toolIcons.addItem("Use classic toolbar icons");
      this.toolIcons.addItem("Use dark toolbar icons");
      // add additional toolbar icon sets here...
      // this.toolIcons.addItem( "Use modern toolbar icons" );
      // this.toolIcons.addItem( "Use light toolbar icons" );

      this.scripts = new JComboBox<>();
      this.scripts.addItem("Do not show script bar on main interface");
      this.scripts.addItem("Show script bar along right of panel");

      VerifiableElement[] elements = new VerifiableElement[3];

      elements[0] = new VerifiableElement("Java L&F: ", this.looks);
      elements[1] = new VerifiableElement("Toolbar: ", this.toolbars);
      // elements[ 2 ] = new VerifiableElement( "Toolbar Icons", this.toolIcons );
      elements[2] = new VerifiableElement("Scripts: ", this.scripts);

      this.actionCancelled();
      this.setContent(elements);
    }

    public boolean shouldAddStatusLabel(final VerifiableElement[] elements) {
      return false;
    }

    @Override
    public void setContent(final VerifiableElement[] elements) {
      super.setContent(elements);
      this.add(new InterfaceCheckboxPanel(), BorderLayout.CENTER);
    }

    @Override
    public void setEnabled(final boolean isEnabled) {}

    @Override
    public void actionConfirmed() {
      String lookAndFeel = "";

      if (this.looks.getSelectedIndex() > 0) {
        UIManager.LookAndFeelInfo[] installed = UIManager.getInstalledLookAndFeels();
        lookAndFeel = installed[this.looks.getSelectedIndex() - 1].getClassName();
      }

      Preferences.setString("swingLookAndFeel", lookAndFeel);

      Preferences.setBoolean("useToolbars", this.toolbars.getSelectedIndex() != 0);
      Preferences.setInteger("scriptButtonPosition", this.scripts.getSelectedIndex());
      Preferences.setInteger("toolbarPosition", this.toolbars.getSelectedIndex());
    }

    @Override
    public void actionCancelled() {
      String lookAndFeel = Preferences.getString("swingLookAndFeel");

      if (lookAndFeel.equals("")) {
        this.looks.setSelectedIndex(0);
      } else {
        this.looks.setSelectedItem(lookAndFeel);
      }

      this.toolbars.setSelectedIndex(Preferences.getInteger("toolbarPosition"));
      this.scripts.setSelectedIndex(Preferences.getInteger("scriptButtonPosition") == 0 ? 0 : 1);
    }

    @Override
    public void update() {
      String lookAndFeel = Preferences.getString("swingLookAndFeel");
      String defaultLookAndFeel =
          (System.getProperty("os.name").startsWith("Mac")
                  || System.getProperty("os.name").startsWith("Win"))
              ? UIManager.getSystemLookAndFeelClassName()
              : UIManager.getCrossPlatformLookAndFeelClassName();
      if (lookAndFeel.equals("")) {
        lookAndFeel = defaultLookAndFeel;
      }
      try {
        UIManager.setLookAndFeel(lookAndFeel);
        // This is where we invalidate the UI and re-draw it because we switched Lafs..."
        Frame[] frames = Frame.getFrames();
        for (Frame f : frames) {
          SwingUtilities.invokeLater(
              () -> {
                SwingUtilities.updateComponentTreeUI(f); // update components
                f.pack(); // adapt container size if required
              });
        }

      } catch (Exception Ex) {
        // This is probably a case of a bad Laf option.  Try not to have one...
        // currently just fails here if it gets UnsupportedLookAndFeelException...
        // System.out.println("Something went wrong\n"+Ex.getMessage());
      }
    }

    private class InterfaceCheckboxPanel extends OptionsPanel {
      private final JLabel innerGradient, outerGradient;

      public InterfaceCheckboxPanel() {
        super(new Dimension(20, 16), new Dimension(370, 16));
        VerifiableElement[] elements =
            new VerifiableElement[UserInterfacePanel.this.options.length + 3];

        UserInterfacePanel.this.optionBoxes = new JCheckBox[UserInterfacePanel.this.options.length];

        for (int i = 0; i < UserInterfacePanel.this.options.length; ++i) {
          String[] option = UserInterfacePanel.this.options[i];
          JCheckBox optionBox = new JCheckBox();
          UserInterfacePanel.this.optionBoxes[i] = optionBox;
          elements[i] =
              option.length == 0
                  ? new VerifiableElement()
                  : new VerifiableElement(option[1], SwingConstants.LEFT, optionBox);
        }

        elements[UserInterfacePanel.this.options.length] = new VerifiableElement();

        this.outerGradient = new TabColorChanger("outerTabColor");
        elements[UserInterfacePanel.this.options.length + 1] =
            new VerifiableElement(
                "Change the outer portion of the tab gradient (shiny tabs)",
                SwingConstants.LEFT,
                this.outerGradient);

        this.innerGradient = new TabColorChanger("innerTabColor");
        elements[UserInterfacePanel.this.options.length + 2] =
            new VerifiableElement(
                "Change the inner portion of the tab gradient (shiny tabs)",
                SwingConstants.LEFT,
                this.innerGradient);

        this.actionCancelled();
        this.setContent(elements);
      }

      @Override
      public void actionConfirmed() {
        for (int i = 0; i < UserInterfacePanel.this.options.length; ++i) {
          String[] option = UserInterfacePanel.this.options[i];
          if (option.length == 0) {
            continue;
          }
          JCheckBox optionBox = UserInterfacePanel.this.optionBoxes[i];
          Preferences.setBoolean(option[0], optionBox.isSelected());
        }
      }

      @Override
      public void actionCancelled() {
        for (int i = 0; i < UserInterfacePanel.this.options.length; ++i) {
          String[] option = UserInterfacePanel.this.options[i];
          if (option.length == 0) {
            continue;
          }
          JCheckBox optionBox = UserInterfacePanel.this.optionBoxes[i];
          optionBox.setSelected(Preferences.getBoolean(option[0]));
        }

        this.innerGradient.setBackground(tab.CloseTabPaneEnhancedUI.selectedA);
        this.outerGradient.setBackground(tab.CloseTabPaneEnhancedUI.selectedB);
      }

      @Override
      public void setEnabled(final boolean isEnabled) {}

      private class TabColorChanger extends ColorChooser {
        public TabColorChanger(final String property) {
          super(property);
        }

        @Override
        public void applyChanges() {
          if (this.property.equals("innerTabColor")) {
            CloseTabPaneEnhancedUI.selectedA =
                InterfaceCheckboxPanel.this.innerGradient.getBackground();
          } else {
            CloseTabPaneEnhancedUI.selectedB =
                InterfaceCheckboxPanel.this.outerGradient.getBackground();
          }
        }
      }
    }
  }

  protected class EditorPanel extends OptionsPanel {
    private final FileSelectPanel preferredEditor;

    public EditorPanel() {
      AutoHighlightTextField textField = new AutoHighlightTextField();
      //			boolean button = true;
      //			String helpText = "";
      //			String path = null;

      boolean button = false;
      String path = "";
      String helpText =
          "The command will be invoked with the full path to the script as its only parameter.";

      this.preferredEditor = new FileSelectPanel(textField, button);
      if (button) {
        this.preferredEditor.setPath(new File(path));
      }

      VerifiableElement[] elements = new VerifiableElement[1];
      elements[0] = new VerifiableElement("Editor command: ", this.preferredEditor);

      this.setContent(elements);

      JTextArea message = new JTextArea(helpText);
      message.setColumns(40);
      message.setLineWrap(true);
      message.setWrapStyleWord(true);
      message.setEditable(false);
      message.setOpaque(false);
      message.setFont(KoLGUIConstants.DEFAULT_FONT);
      message.setPreferredSize(this.getPreferredSize());

      this.container.add(message, BorderLayout.SOUTH);

      this.actionCancelled();
    }

    @Override
    public void actionConfirmed() {
      Preferences.setString("externalEditor", this.preferredEditor.getText());
    }

    @Override
    public void actionCancelled() {
      this.preferredEditor.setText(Preferences.getString("externalEditor"));
    }
  }

  protected class ScriptPanel extends OptionsPanel {
    private ScriptSelectPanel loginScript;
    private ScriptSelectPanel logoutScript;

    private ScriptSelectPanel afterAdventureScript;
    private ScriptSelectPanel betweenBattleScript;
    private ScriptSelectPanel choiceAdventureScript;
    private ScriptSelectPanel counterScript;
    private ScriptSelectPanel familiarScript;
    private ScriptSelectPanel recoveryScript;

    private ScriptSelectPanel kingLiberatedScript;
    private ScriptSelectPanel preAscensionScript;
    private ScriptSelectPanel postAscensionScript;

    private ScriptSelectPanel beforePVPScript;
    private ScriptSelectPanel buyScript;
    private ScriptSelectPanel chatbotScript;
    private ScriptSelectPanel plantingScript;
    private ScriptSelectPanel spadingScript;
    private ScriptSelectPanel chatPlayerScript;

    public ScriptPanel() {
      initialize();

      final List<ScriptSelectPanel> list = new ArrayList<>();

      fillList(list);

      JPanel layoutPanel = makeLayoutPane(list);
      JScrollPane scrollPane = makeScrollPane(layoutPanel);

      this.contentPane.add(scrollPane);
      this.actionCancelled();
    }

    private JScrollPane makeScrollPane(JPanel layoutPanel) {
      // Make the layout manager understand how the viewport works.
      JScrollPane scrollPane = new JScrollPane();
      JViewport vp = new JViewport();

      vp.setPreferredSize(this.contentPane.getPreferredSize());
      vp.setView(layoutPanel);

      scrollPane.setViewport(vp);

      return scrollPane;
    }

    private JPanel makeLayoutPane(final List<ScriptSelectPanel> list) {
      JXPanel layoutPanel = new JXPanel();
      layoutPanel.setLayout(new BoxLayout(layoutPanel, BoxLayout.Y_AXIS));

      // We're going to use BorderLayout for the rows, with the textComponent as the CENTER.
      // Since the textComponent will gobble up all available space, make the labels on the left a
      // uniform size.
      int bigDim = 0;
      for (ScriptSelectPanel panel : list) {
        int size = panel.getLabel().getPreferredSize().width;
        if (size > bigDim) {
          bigDim = size;
        }
      }

      for (ScriptSelectPanel panel : list) {
        JXPanel p = new JXPanel(new BorderLayout());
        JLabel lab = panel.getLabel();
        lab.setPreferredSize(new Dimension(bigDim + 2, lab.getPreferredSize().height));
        p.add(lab, BorderLayout.WEST);
        p.add(panel, BorderLayout.CENTER);

        p.add(Box.createVerticalStrut(5), BorderLayout.SOUTH);

        layoutPanel.add(p);
      }

      // Do resize the horizontal dimension as the viewport resizes.  JXPanel does this by default.
      layoutPanel.setScrollableTracksViewportHeight(
          false); // Don't resize the vertical dimension, let the viewport do that
      layoutPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
      return layoutPanel;
    }

    private void fillList(final List<ScriptSelectPanel> list) {
      list.add(this.loginScript);
      list.add(this.logoutScript);
      list.add(this.recoveryScript);
      list.add(this.betweenBattleScript);
      list.add(this.afterAdventureScript);
      list.add(this.choiceAdventureScript);
      list.add(this.counterScript);
      list.add(this.familiarScript);
      list.add(this.kingLiberatedScript);
      list.add(this.preAscensionScript);
      list.add(this.postAscensionScript);
      list.add(this.beforePVPScript);
      list.add(this.buyScript);
      list.add(this.plantingScript);
      list.add(this.spadingScript);
      list.add(this.chatbotScript);
      list.add(this.chatPlayerScript);
    }

    private void initialize() {
      this.afterAdventureScript = new ScriptSelectPanel(new CollapsibleTextArea("Post-Adventure:"));
      this.beforePVPScript = new ScriptSelectPanel(new CollapsibleTextArea("Before PvP:"));
      this.betweenBattleScript = new ScriptSelectPanel(new CollapsibleTextArea("Pre-Adventure:"));
      this.buyScript = new ScriptSelectPanel(new CollapsibleTextArea("Buy Script:"));
      this.chatbotScript = new ScriptSelectPanel(new CollapsibleTextArea("Chatbot Script:"));
      this.choiceAdventureScript =
          new ScriptSelectPanel(new CollapsibleTextArea("Choice-Adventure:"));
      this.counterScript = new ScriptSelectPanel(new CollapsibleTextArea("Counter Script:"));
      this.familiarScript = new ScriptSelectPanel(new CollapsibleTextArea("Familiar Script: "));
      this.kingLiberatedScript = new ScriptSelectPanel(new CollapsibleTextArea("King Freed:"));
      this.loginScript = new ScriptSelectPanel(new CollapsibleTextArea("On Login:"));
      this.logoutScript = new ScriptSelectPanel(new CollapsibleTextArea("On Logout:"));
      this.plantingScript = new ScriptSelectPanel(new CollapsibleTextArea("Planting:"));
      this.postAscensionScript = new ScriptSelectPanel(new CollapsibleTextArea("Post-Ascension:"));
      this.preAscensionScript = new ScriptSelectPanel(new CollapsibleTextArea("Pre-Ascension:"));
      this.recoveryScript = new ScriptSelectPanel(new CollapsibleTextArea("Recovery:"));
      this.spadingScript = new ScriptSelectPanel(new CollapsibleTextArea("Spading:"));
      this.chatPlayerScript = new ScriptSelectPanel(new CollapsibleTextArea("Chat Player Click:"));
    }

    @Override
    public void actionConfirmed() {
      Preferences.setString("afterAdventureScript", this.afterAdventureScript.getText());
      Preferences.setString("beforePVPScript", this.beforePVPScript.getText());
      Preferences.setString("betweenBattleScript", this.betweenBattleScript.getText());
      Preferences.setString("buyScript", this.buyScript.getText());
      Preferences.setString("chatbotScript", this.chatbotScript.getText());
      Preferences.setString("choiceAdventureScript", this.choiceAdventureScript.getText());
      Preferences.setString("counterScript", this.counterScript.getText());
      Preferences.setString("familiarScript", this.familiarScript.getText());
      Preferences.setString("kingLiberatedScript", this.kingLiberatedScript.getText());
      Preferences.setString("loginScript", this.loginScript.getText());
      Preferences.setString("logoutScript", this.logoutScript.getText());
      Preferences.setString("plantingScript", this.plantingScript.getText());
      Preferences.setString("postAscensionScript", this.postAscensionScript.getText());
      Preferences.setString("preAscensionScript", this.preAscensionScript.getText());
      Preferences.setString("recoveryScript", this.recoveryScript.getText());
      Preferences.setString("spadingScript", this.spadingScript.getText());
      Preferences.setString("chatPlayerScript", this.chatPlayerScript.getText());
    }

    @Override
    public void actionCancelled() {
      this.afterAdventureScript.setText(Preferences.getString("afterAdventureScript"));
      this.beforePVPScript.setText(Preferences.getString("beforePVPScript"));
      this.betweenBattleScript.setText(Preferences.getString("betweenBattleScript"));
      this.buyScript.setText(Preferences.getString("buyScript"));
      this.chatbotScript.setText(Preferences.getString("chatbotScript"));
      this.choiceAdventureScript.setText(Preferences.getString("choiceAdventureScript"));
      this.counterScript.setText(Preferences.getString("counterScript"));
      this.familiarScript.setText(Preferences.getString("familiarScript"));
      this.kingLiberatedScript.setText(Preferences.getString("kingLiberatedScript"));
      this.loginScript.setText(Preferences.getString("loginScript"));
      this.logoutScript.setText(Preferences.getString("logoutScript"));
      this.plantingScript.setText(Preferences.getString("plantingScript"));
      this.postAscensionScript.setText(Preferences.getString("postAscensionScript"));
      this.preAscensionScript.setText(Preferences.getString("preAscensionScript"));
      this.recoveryScript.setText(Preferences.getString("recoveryScript"));
      this.spadingScript.setText(Preferences.getString("spadingScript"));
      this.chatPlayerScript.setText(Preferences.getString("chatPlayerScript"));
    }
  }

  protected class BreakfastAlwaysPanel extends JPanel implements ActionListener {
    private final JCheckBox[] skillOptions;

    public BreakfastAlwaysPanel() {
      super(new CardLayout(10, 10));

      JPanel centerContainer = new JPanel();
      centerContainer.setLayout(new BoxLayout(centerContainer, BoxLayout.Y_AXIS));

      int rows = (UseSkillRequest.BREAKFAST_ALWAYS_SKILLS.length) / 2 + 5;

      JPanel centerPanel = new JPanel(new GridLayout(rows, 2));

      this.skillOptions = new JCheckBox[UseSkillRequest.BREAKFAST_ALWAYS_SKILLS.length];
      for (int i = 0; i < UseSkillRequest.BREAKFAST_ALWAYS_SKILLS.length; ++i) {
        this.skillOptions[i] =
            new JCheckBox(UseSkillRequest.BREAKFAST_ALWAYS_SKILLS[i].toLowerCase());
        this.skillOptions[i].addActionListener(this);
        centerPanel.add(this.skillOptions[i]);
      }

      centerContainer.add(centerPanel);
      centerContainer.add(Box.createVerticalGlue());

      this.add(centerContainer, "");

      this.actionCancelled();
    }

    public void actionPerformed(final ActionEvent e) {
      this.actionConfirmed();
    }

    public void actionConfirmed() {
      StringBuilder skillString = new StringBuilder();

      for (int i = 0; i < UseSkillRequest.BREAKFAST_ALWAYS_SKILLS.length; ++i) {
        if (this.skillOptions[i].isSelected()) {
          if (skillString.length() != 0) {
            skillString.append(",");
          }

          skillString.append(UseSkillRequest.BREAKFAST_ALWAYS_SKILLS[i]);
        }
      }

      Preferences.setString("breakfastAlways", skillString.toString());
    }

    public void actionCancelled() {
      String skillString = Preferences.getString("breakfastAlways");
      for (int i = 0; i < UseSkillRequest.BREAKFAST_ALWAYS_SKILLS.length; ++i) {
        this.skillOptions[i].setSelected(
            skillString.contains(UseSkillRequest.BREAKFAST_ALWAYS_SKILLS[i]));
      }
    }

    @Override
    public void setEnabled(final boolean isEnabled) {}
  }

  protected class BreakfastPanel extends JPanel implements ActionListener {
    private final String breakfastType;
    private final JCheckBox[] skillOptions;

    private final JCheckBox loginRecovery;
    private final JCheckBox pathedSummons;
    private final JCheckBox rumpusRoom;
    private final JCheckBox clanLounge;

    private final JCheckBox mushroomPlot;
    private final JCheckBox grabClovers;
    private final JCheckBox readManual;
    private final JCheckBox useCrimboToys;
    private final JCheckBox checkJackass;
    private final JCheckBox makePocketWishes;
    private final JCheckBox haveBoxingDaydream;
    private final JCheckBox harvestBatteries;

    private final SkillMenu tomeSkills;
    private final SkillMenu libramSkills;
    private final SkillMenu grimoireSkills;

    private final CropMenu cropsMenu;

    public BreakfastPanel(final String breakfastType) {
      super(new CardLayout(10, 10));

      JPanel centerContainer = new JPanel();
      centerContainer.setLayout(new BoxLayout(centerContainer, BoxLayout.Y_AXIS));

      int rows = (UseSkillRequest.BREAKFAST_SKILLS.length + 11) / 2 + 1;

      JPanel centerPanel = new JPanel(new GridLayout(rows, 2));

      this.loginRecovery = new JCheckBox("enable auto-recovery");
      this.loginRecovery.addActionListener(this);
      centerPanel.add(this.loginRecovery);

      this.pathedSummons = new JCheckBox("honor path restrictions");
      this.pathedSummons.addActionListener(this);
      centerPanel.add(this.pathedSummons);

      this.rumpusRoom = new JCheckBox("visit clan rumpus room");
      this.rumpusRoom.addActionListener(this);
      centerPanel.add(this.rumpusRoom);

      this.clanLounge = new JCheckBox("visit clan VIP lounge");
      this.clanLounge.addActionListener(this);
      centerPanel.add(this.clanLounge);

      this.breakfastType = breakfastType;
      this.skillOptions = new JCheckBox[UseSkillRequest.BREAKFAST_SKILLS.length];
      for (int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i) {
        this.skillOptions[i] = new JCheckBox(UseSkillRequest.BREAKFAST_SKILLS[i].toLowerCase());
        this.skillOptions[i].addActionListener(this);
        centerPanel.add(this.skillOptions[i]);
      }

      this.mushroomPlot = new JCheckBox("plant mushrooms");
      this.mushroomPlot.addActionListener(this);
      centerPanel.add(this.mushroomPlot);

      this.grabClovers = new JCheckBox("get hermit clovers");
      this.grabClovers.addActionListener(this);
      centerPanel.add(this.grabClovers);

      this.readManual = new JCheckBox("read guild manual");
      this.readManual.addActionListener(this);
      centerPanel.add(this.readManual);

      this.useCrimboToys = new JCheckBox("use once-a-day items");
      this.useCrimboToys.addActionListener(this);
      centerPanel.add(this.useCrimboToys);

      this.checkJackass = new JCheckBox("check Jackass Plumber");
      this.checkJackass.addActionListener(this);
      centerPanel.add(this.checkJackass);

      this.makePocketWishes = new JCheckBox("make Pocket Wishes");
      this.makePocketWishes.addActionListener(this);
      centerPanel.add(this.makePocketWishes);

      this.haveBoxingDaydream = new JCheckBox("have Boxing Daydream");
      this.haveBoxingDaydream.addActionListener(this);
      centerPanel.add(this.haveBoxingDaydream);

      this.harvestBatteries = new JCheckBox("harvest batteries");
      this.harvestBatteries.addActionListener(this);
      centerPanel.add(this.harvestBatteries);

      centerContainer.add(centerPanel);
      centerContainer.add(Box.createVerticalStrut(10));

      centerPanel = new JPanel(new GridLayout(4, 1));

      this.tomeSkills =
          new SkillMenu(
              "Tome Skills", UseSkillRequest.TOME_SKILLS, "tomeSkills" + this.breakfastType);
      if (this.breakfastType.equals("Hardcore")) {
        // Only show this option in the In Ronin panel
        this.tomeSkills.addActionListener(this);
        centerPanel.add(this.tomeSkills);
      } else {
        // Always select "All Tome Skills" in the hidden After Ronin panel
        this.tomeSkills.setSelectedIndex(1);
        this.tomeSkills.setPreference();
      }

      this.libramSkills =
          new SkillMenu(
              "Libram Skills", UseSkillRequest.LIBRAM_SKILLS, "libramSkills" + this.breakfastType);
      this.libramSkills.addActionListener(this);
      centerPanel.add(this.libramSkills);

      this.grimoireSkills =
          new SkillMenu(
              "Grimoire Skills",
              UseSkillRequest.GRIMOIRE_SKILLS,
              "grimoireSkills" + this.breakfastType);
      this.grimoireSkills.addActionListener(this);
      centerPanel.add(this.grimoireSkills);

      this.cropsMenu = new CropMenu("harvestGarden" + this.breakfastType);
      this.cropsMenu.addActionListener(this);
      centerPanel.add(this.cropsMenu);

      centerContainer.add(centerPanel);
      centerContainer.add(Box.createVerticalGlue());

      this.add(centerContainer, "");

      this.actionCancelled();
    }

    public void actionPerformed(final ActionEvent e) {
      this.actionConfirmed();
    }

    public void actionConfirmed() {
      StringBuilder skillString = new StringBuilder();

      for (int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i) {
        if (this.skillOptions[i].isSelected()) {
          if (skillString.length() != 0) {
            skillString.append(",");
          }

          skillString.append(UseSkillRequest.BREAKFAST_SKILLS[i]);
        }
      }

      Preferences.setString("breakfast" + this.breakfastType, skillString.toString());
      Preferences.setBoolean("loginRecovery" + this.breakfastType, this.loginRecovery.isSelected());
      Preferences.setBoolean("pathedSummons" + this.breakfastType, this.pathedSummons.isSelected());
      Preferences.setBoolean("visitRumpus" + this.breakfastType, this.rumpusRoom.isSelected());
      Preferences.setBoolean("visitLounge" + this.breakfastType, this.clanLounge.isSelected());
      Preferences.setBoolean("autoPlant" + this.breakfastType, this.mushroomPlot.isSelected());
      Preferences.setBoolean("grabClovers" + this.breakfastType, this.grabClovers.isSelected());
      Preferences.setBoolean("readManual" + this.breakfastType, this.readManual.isSelected());
      Preferences.setBoolean("useCrimboToys" + this.breakfastType, this.useCrimboToys.isSelected());
      Preferences.setBoolean("checkJackass" + this.breakfastType, this.checkJackass.isSelected());
      Preferences.setBoolean(
          "makePocketWishes" + this.breakfastType, this.makePocketWishes.isSelected());
      Preferences.setBoolean(
          "haveBoxingDaydream" + this.breakfastType, this.haveBoxingDaydream.isSelected());
      Preferences.setBoolean(
          "harvestBatteries" + this.breakfastType, this.harvestBatteries.isSelected());

      this.tomeSkills.setPreference();
      this.libramSkills.setPreference();
      this.grimoireSkills.setPreference();
      this.cropsMenu.setPreference();
    }

    public void actionCancelled() {
      String skillString = Preferences.getString("breakfast" + this.breakfastType);
      for (int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i) {
        this.skillOptions[i].setSelected(skillString.contains(UseSkillRequest.BREAKFAST_SKILLS[i]));
      }

      this.loginRecovery.setSelected(Preferences.getBoolean("loginRecovery" + this.breakfastType));
      this.pathedSummons.setSelected(Preferences.getBoolean("pathedSummons" + this.breakfastType));
      this.rumpusRoom.setSelected(Preferences.getBoolean("visitRumpus" + this.breakfastType));
      this.clanLounge.setSelected(Preferences.getBoolean("visitLounge" + this.breakfastType));
      this.mushroomPlot.setSelected(Preferences.getBoolean("autoPlant" + this.breakfastType));
      this.grabClovers.setSelected(Preferences.getBoolean("grabClovers" + this.breakfastType));
      this.readManual.setSelected(Preferences.getBoolean("readManual" + this.breakfastType));
      this.useCrimboToys.setSelected(Preferences.getBoolean("useCrimboToys" + this.breakfastType));
      this.checkJackass.setSelected(Preferences.getBoolean("checkJackass" + this.breakfastType));
      this.makePocketWishes.setSelected(
          Preferences.getBoolean("makePocketWishes" + this.breakfastType));
      this.haveBoxingDaydream.setSelected(
          Preferences.getBoolean("haveBoxingDaydream" + this.breakfastType));
      this.harvestBatteries.setSelected(
          Preferences.getBoolean("harvestBatteries" + this.breakfastType));
    }

    @Override
    public void setEnabled(final boolean isEnabled) {}
  }

  private class SkillMenu extends JComboBox<String> {
    final String preference;

    public SkillMenu(final String name, final String[] skills, final String preference) {
      super();
      this.addItem("No " + name);
      this.addItem("All " + name);
      for (String skill : skills) {
        this.addItem(skill);
      }

      this.preference = preference;
      this.getPreference();
    }

    public void getPreference() {
      String skill = Preferences.getString(this.preference);
      if (skill.equals("none")) {
        this.setSelectedIndex(0);
      } else if (skill.equals("all")) {
        this.setSelectedIndex(1);
      } else {
        this.setSelectedItem(skill);
      }

      if (this.getSelectedIndex() < 0) {
        this.setSelectedIndex(0);
      }
    }

    public void setPreference() {
      String skill = null;
      int index = this.getSelectedIndex();
      switch (index) {
        case -1:
        case 0:
          skill = "none";
          break;
        case 1:
          skill = "all";
          break;
        default:
          skill = (String) this.getItemAt(index);
          break;
      }
      Preferences.setString(this.preference, skill);
    }
  }

  private class CropMenu extends JComboBox<String> {
    final String preference;

    public CropMenu(final String preference) {
      super();

      this.addItem("Harvest Nothing");
      this.addItem("Harvest Anything");

      for (AdventureResult crop : CampgroundRequest.CROPS) {
        this.addItem(crop.toString());
      }

      this.preference = preference;
      this.getPreference();
    }

    public void getPreference() {
      String crop = Preferences.getString(this.preference);
      if (crop.equals("none")) {
        this.setSelectedIndex(0);
      } else if (crop.equals("any")) {
        this.setSelectedIndex(1);
      } else {
        this.setSelectedItem(crop);
      }

      if (this.getSelectedIndex() < 0) {
        this.setSelectedIndex(0);
      }
    }

    public void setPreference() {
      String crop = null;
      int index = this.getSelectedIndex();
      switch (index) {
        case -1:
        case 0:
          crop = "none";
          break;
        case 1:
          crop = "any";
          break;
        default:
          crop = (String) this.getItemAt(index);
          break;
      }
      Preferences.setString(this.preference, crop);
    }
  }

  /** Main panel for color options. */
  private class ColorOptionsPanel extends JPanel implements Listener {
    private final ColorOptionsPreferencePanel colorOptionsPreferencePanel;

    ColorOptionsPanel() {
      super();
      this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

      JButton defaultButton = new JButton("Default Color Set");
      defaultButton.addActionListener(
          e -> {
            Preferences.resetToDefault("textColors");
            update();
          });
      JButton darkDefaultButton = new JButton("Dark Color Set");
      darkDefaultButton.addActionListener(
          e -> {
            Preferences.setString(
                "textColors",
                "crappy:#999999|good:#00bf00|awesome:#7f7fff|epic:#cc00ff|junk:gray|memento:olive|notavailable:gray|decent:#cccccc");
            update();
          });

      JPanel resetPanel = new JPanel();
      resetPanel.setLayout(new BoxLayout(resetPanel, BoxLayout.LINE_AXIS));
      resetPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 10));
      // This actually left-aligns the buttons.
      // Using LEFT_ALIGNMENT on the other hand right-aligns the buttons. WTF?
      resetPanel.setAlignmentX(RIGHT_ALIGNMENT);
      resetPanel.add(defaultButton);
      resetPanel.add(darkDefaultButton);

      this.colorOptionsPreferencePanel = new ColorOptionsPreferencePanel();

      super.add(this.colorOptionsPreferencePanel);
      super.add(resetPanel);
    }

    @Override
    public void update() {
      this.colorOptionsPreferencePanel.update();
    }
  }

  /**
   * Used by ColorOptionsPanel to display rows of color preference items.
   *
   * @see ColorOptionsPanel
   */
  private class ColorOptionsPreferencePanel extends OptionsPanel implements Listener {
    JLabel crappy, decent, good, awesome, epic;
    JLabel memento, junk, notavailable;

    public ColorOptionsPreferencePanel() {
      super(new Dimension(16, 17), new Dimension(370, 17));
      PreferenceListenerRegistry.registerPreferenceListener("textColors", this);

      this.setContent();
    }

    private void setContent() {
      this.crappy = new FontColorChooser("crappy");
      this.decent = new FontColorChooser("decent");
      this.good = new FontColorChooser("good");
      this.awesome = new FontColorChooser("awesome");
      this.epic = new FontColorChooser("epic");
      this.junk = new FontColorChooser("junk");
      this.memento = new FontColorChooser("memento");
      this.notavailable = new FontColorChooser("notavailable");

      VerifiableElement[] newElements = {
        new VerifiableElement(
            "   ",
            SwingConstants.RIGHT,
            new JLabel("This panel alters the appearance of some text colors in the Mafia UI.")),
        new VerifiableElement("", SwingConstants.RIGHT, new JSeparator()),
        new VerifiableElement("Item Quality Colors:", SwingConstants.LEFT, new JLabel()),
        new VerifiableElement("Crappy", SwingConstants.LEFT, this.crappy),
        new VerifiableElement("Decent", SwingConstants.LEFT, this.decent),
        new VerifiableElement("Good", SwingConstants.LEFT, this.good),
        new VerifiableElement("Awesome", SwingConstants.LEFT, this.awesome),
        new VerifiableElement("EPIC", SwingConstants.LEFT, this.epic),
        new VerifiableElement(), // Spacer
        new VerifiableElement("Other Font Colors:", SwingConstants.LEFT, new JLabel()),
        new VerifiableElement("Junk Items", SwingConstants.LEFT, this.junk),
        new VerifiableElement("Mementos", SwingConstants.LEFT, this.memento),
        new VerifiableElement(
            "Not Equippable/Creatable/Available", SwingConstants.LEFT, this.notavailable),
      };

      super.setContent(newElements);
      this.readFromPref();
    }

    private void readFromPref() {
      String rawPref = Preferences.getString("textColors");
      String[] splitPref = rawPref.split("\\|");

      for (String s : splitPref) {
        String[] it = s.split(":");
        if (it.length == 2) {
          switch (it[0]) {
            case "crappy":
              decodeColor(it[1], this.crappy);
              break;
            case "decent":
              decodeColor(it[1], this.decent);
              break;
            case "good":
              decodeColor(it[1], this.good);
              break;
            case "awesome":
              decodeColor(it[1], this.awesome);
              break;
            case "epic":
              decodeColor(it[1], this.epic);
              break;
            case "memento":
              decodeColor(it[1], this.memento);
              break;
            case "junk":
              decodeColor(it[1], this.junk);
              break;
            case "notavailable":
              decodeColor(it[1], this.notavailable);
              break;
          }
        }
      }

      fillDefaults();
    }

    private void fillDefaults() {
      if (this.crappy.getClientProperty("set") == null) {
        this.crappy.setBackground(Color.gray);
      }
      if (this.decent.getClientProperty("set") == null) {
        this.decent.setBackground(Color.black);
      }
      if (this.good.getClientProperty("set") == null) {
        this.good.setBackground(Color.green);
      }
      if (this.awesome.getClientProperty("set") == null) {
        this.awesome.setBackground(Color.blue);
      }
      if (this.epic.getClientProperty("set") == null) {
        this.epic.setBackground(DataUtilities.toColor("#8a2be2")); // purple..ish
      }
      if (this.memento.getClientProperty("set") == null) {
        this.memento.setBackground(DataUtilities.toColor("#808000")); // olive
      }
      if (this.junk.getClientProperty("set") == null) {
        this.junk.setBackground(Color.gray);
      }
      if (this.notavailable.getClientProperty("set") == null) {
        this.notavailable.setBackground(Color.gray);
      }
    }

    private void decodeColor(String it, JLabel label) {
      label.putClientProperty("set", Boolean.TRUE);
      try {
        Field field = Color.class.getField(it);
        Color color = (Color) field.get(null);

        label.setBackground(color);
      } catch (Exception e) {
        try {
          // maybe the pref was a hex code
          label.setBackground(DataUtilities.toColor(it));
        } catch (Exception f) {
          // olive color is not an acceptable label, but is recognized by HTML parser
          // just hardcode it, whatever
          if (it.equals("olive")) {
            label.setBackground(DataUtilities.toColor("#808000"));
          }
          // else fall through, invalid color format
        }
      }
    }

    @Override
    public void update() {
      try {
        // This is where we invalidate the UI and re-draw it because we switched Colors..."
        Frame[] frames = Frame.getFrames();
        for (Frame f : frames) {
          SwingUtilities.invokeLater(
              () -> {
                SwingUtilities.updateComponentTreeUI(f); // update components
                f.pack(); // adapt container size if required
              });
        }

        // Reload font color prefs (in case they changed)
        readFromPref();
      } catch (Exception Ex) {
        // This is probably a case of a bad Laf option.  Try not to have one...
        // currently just fails silently if we get an UnsupportedLookAndFeelException...
        // System.out.println("Something went wrong\n"+Ex.getMessage());
      }
    }

    private final class FontColorChooser extends JLabel implements MouseListener {
      protected String property;

      public FontColorChooser(final String property) {
        this.property = property;
        this.setOpaque(true);
        this.addMouseListener(this);
      }

      public void mousePressed(final MouseEvent e) {
        Color c = JColorChooser.showDialog(null, "Choose a color:", this.getBackground());
        if (c == null) {
          return;
        }

        updatePref(this.property, DataUtilities.toHexString(c));
        this.setBackground(c);
      }

      public void updatePref(String property, String hexString) {
        String rawPref = Preferences.getString("textColors");
        String[] splitPref = rawPref.split("\\|");
        String newProperty = property + ":" + hexString;

        for (String s : splitPref) {
          String[] it = s.split(":");
          if (it.length == 2) {
            if (it[0].equals(property)) {
              String newPref = StringUtilities.globalStringReplace(rawPref, s, newProperty);
              Preferences.setString("textColors", newPref);
              return;
            }
          }
        }

        // property does not exist in pref; add it
        String delimiter = "";
        if (rawPref.length() > 0) {
          delimiter = "|";
        }
        String newPref = rawPref + delimiter + newProperty;
        Preferences.setString("textColors", newPref);
      }

      public void mouseReleased(final MouseEvent e) {}

      public void mouseClicked(final MouseEvent e) {}

      public void mouseEntered(final MouseEvent e) {}

      public void mouseExited(final MouseEvent e) {}
    }
  }
}
