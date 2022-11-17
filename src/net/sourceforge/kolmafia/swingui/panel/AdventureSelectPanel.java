package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AreaCombatData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.listener.CharacterListener;
import net.sourceforge.kolmafia.listener.CharacterListenerRegistry;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.session.EncounterManager.RegisteredEncounter;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.GoalManager;
import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;
import net.sourceforge.kolmafia.swingui.button.InvocationButton;
import net.sourceforge.kolmafia.swingui.button.ThreadedButton;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightSpinner;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.EditableAutoFilterComboBox;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.RequestPane;
import net.sourceforge.kolmafia.textui.command.ConditionsCommand;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.PauseObject;

public class AdventureSelectPanel extends JPanel {
  private ThreadedButton begin;

  private final TreeMap<String, String> zoneMap;
  private AdventureCountSpinner countField;
  private final LockableListModel<KoLAdventure> matchingAdventures;

  private final JList<KoLAdventure> locationSelect;
  private final JComponent zoneSelect;

  // zoneSelect will be either of these
  private final AutoFilterTextField<KoLAdventure> autoFilterZoneSelect;
  private final JComboBox<String> comboBoxZoneSelect;

  private final LockableListModel<String> locationConditions = new LockableListModel<>();
  private final RedoFreeAdventuresCheckbox redoFreeAdventures;
  private final JCheckBox conditionsFieldActive;
  private final ConditionsComboBox conditionField = new ConditionsComboBox();
  private final SafetyField safetyField = null;

  public AdventureSelectPanel(final boolean enableAdventures) {
    super(new BorderLayout(10, 10));

    this.matchingAdventures = AdventureDatabase.getAsLockableListModel().getMirrorImage();

    // West pane is a scroll pane which lists all of the available
    // locations -- to be included is a map on a separate tab.

    this.locationSelect = new JList<>(this.matchingAdventures);
    this.locationSelect.setVisibleRowCount(4);

    JPanel zonePanel = new JPanel(new BorderLayout(5, 5));

    boolean useZoneComboBox = Preferences.getBoolean("useZoneComboBox");
    if (useZoneComboBox) {
      this.zoneSelect = this.comboBoxZoneSelect = new FilterAdventureComboBox();
      this.matchingAdventures.setFilter((FilterAdventureComboBox) this.zoneSelect);
      this.autoFilterZoneSelect = null;
    } else {
      this.zoneSelect = this.autoFilterZoneSelect = new AutoFilterTextField<>(this.locationSelect);
      this.comboBoxZoneSelect = null;
    }

    this.zoneMap = new TreeMap<>();
    String[] zones = AdventureDatabase.PARENT_LIST.toArray(new String[0]);

    String currentZone;

    for (String zone : zones) {
      currentZone = AdventureDatabase.ZONE_DESCRIPTIONS.get(zone);
      if (currentZone == null) {
        // This indicates an error in zonelist.txt
        System.out.println("null zone for " + zone);
        continue;
      }
      this.zoneMap.put(currentZone, zone);

      if (useZoneComboBox) {
        this.comboBoxZoneSelect.addItem(currentZone);
      }
    }

    JComponentUtilities.setComponentSize(this.zoneSelect, 180, -1);
    zonePanel.add(this.zoneSelect, BorderLayout.CENTER);

    this.redoFreeAdventures = new RedoFreeAdventuresCheckbox();
    if (enableAdventures) {
      JPanel panel = new JPanel();

      panel.add(this.redoFreeAdventures);

      this.countField = new AdventureCountSpinner();
      this.countField.setHorizontalAlignment(AutoHighlightTextField.RIGHT);
      this.countField.getEditor().setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

      JComponentUtilities.setComponentSize(this.countField, 75, -1);
      panel.add(this.countField);

      zonePanel.add(panel, BorderLayout.EAST);
    }

    JPanel contentHolder = new JPanel(new BorderLayout(5, 5));

    contentHolder.add(zonePanel, BorderLayout.NORTH);
    contentHolder.add(new GenericScrollPane(this.locationSelect), BorderLayout.CENTER);

    this.locationSelect.addListSelectionListener(new AdventureSelectListener());

    JPanel conditionPanel = new JPanel(new BorderLayout(5, 5));

    conditionPanel.add(this.conditionField, BorderLayout.CENTER);

    this.conditionsFieldActive = new JCheckBox();
    conditionPanel.add(this.conditionsFieldActive, BorderLayout.EAST);

    boolean enableConditionsField = Preferences.getBoolean("autoSetConditions");
    this.conditionsFieldActive.setSelected(enableConditionsField);
    this.conditionsFieldActive.setToolTipText(
        "enable conditions field and stop if the conditions are met");
    this.conditionsFieldActive.addActionListener(new EnableObjectivesListener());
    this.conditionField.setEnabled(enableConditionsField);

    contentHolder.add(conditionPanel, BorderLayout.SOUTH);

    JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
    contentPanel.add(contentHolder, BorderLayout.CENTER);

    this.setLayout(new CardLayout(5, 5));
    this.add("", contentPanel);

    if (enableAdventures) {
      JPanel buttonHolder = new JPanel(new GridLayout(3, 1, 5, 5));

      this.begin = new ThreadedButton("begin", new ExecuteRunnable());
      this.begin.setToolTipText("Start Adventuring");

      JComponentUtilities.addHotKey(this, KeyEvent.VK_ENTER, this.begin);

      buttonHolder.add(this.begin);
      buttonHolder.add(new InvocationButton("stop now", RequestThread.class, "declareWorldPeace"));
      buttonHolder.add(new StopButton());

      JPanel buttonPanel = new JPanel(new BorderLayout());
      buttonPanel.add(buttonHolder, BorderLayout.NORTH);

      contentPanel.add(buttonPanel, BorderLayout.EAST);

      this.zoneSelect.addKeyListener(this.begin);
      this.countField.addKeyListener(this.begin);
    }
  }

  public KoLAdventure getSelectedAdventure() {
    return this.locationSelect.getSelectedValue();
  }

  public void updateFromPreferences() {
    KoLAdventure location = AdventureDatabase.getAdventure(Preferences.getString("lastAdventure"));
    GoalManager.clearGoals();
    this.updateSelectedAdventure(location);
  }

  public void updateSafetyDetails() {
    if (this.safetyField != null) {
      this.safetyField.run();
    }
  }

  public void updateSelectedAdventure(final KoLAdventure location) {
    if (location == null) {
      return;
    }

    if (GoalManager.hasGoals()) {
      return;
    }

    if (this.zoneSelect == this.autoFilterZoneSelect) {
      this.locationSelect.clearSelection();
      this.autoFilterZoneSelect.setText(location.getZone());
    } else {
      this.comboBoxZoneSelect.setSelectedItem(location.getParentZoneDescription());
    }

    this.locationSelect.setSelectedValue(location, true);
  }

  public synchronized void addSelectedLocationListener(final ListSelectionListener listener) {
    this.locationSelect.addListSelectionListener(listener);
  }

  private class FilterAdventureComboBox extends JComboBox<String> implements ListElementFilter {
    private Object selectedZone;

    @Override
    public void setSelectedItem(final Object element) {
      super.setSelectedItem(element);
      this.selectedZone = element;
      AdventureSelectPanel.this.matchingAdventures.updateFilter(false);
    }

    @Override
    public boolean isVisible(final Object element) {
      return ((KoLAdventure) element).getParentZoneDescription().equals(this.selectedZone);
    }
  }

  private class ObjectivesPanel extends GenericPanel {
    public ObjectivesPanel() {
      super(new Dimension(70, -1), new Dimension(200, -1));
    }

    @Override
    public void actionConfirmed() {}

    @Override
    public void actionCancelled() {}

    @Override
    public void addStatusLabel() {}

    @Override
    public void setEnabled(final boolean isEnabled) {
      if (AdventureSelectPanel.this.begin != null) {
        AdventureSelectPanel.this.begin.setEnabled(isEnabled);
      }
    }
  }

  private class RedoFreeAdventuresCheckbox extends JCheckBox implements ActionListener, Listener {
    public RedoFreeAdventuresCheckbox() {
      super();
      this.setToolTipText("Don't count free adventures towards turn count maximum");
      this.setSelected(KoLmafia.redoSkippedAdventures);
      this.addActionListener(this);
      NamedListenerRegistry.registerNamedListener("(adventuring)", this);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      KoLmafia.redoSkippedAdventures = this.isSelected();
    }

    // called when (adventuring) fires
    @Override
    public void update() {
      this.setEnabled(!KoLmafia.isAdventuring());
    }
  }

  private class EnableObjectivesListener extends ThreadedListener {
    @Override
    protected void execute() {
      boolean enabled = AdventureSelectPanel.this.conditionsFieldActive.isSelected();
      Preferences.setBoolean("autoSetConditions", enabled);
      AdventureSelectPanel.this.conditionField.setEnabled(enabled && !KoLmafia.isAdventuring());
    }
  }

  private class AdventureSelectListener implements ListSelectionListener, ListDataListener {
    public AdventureSelectListener() {
      GoalManager.getGoals().addListDataListener(this);
      AdventureSelectPanel.this.fillDefaultConditions();
    }

    @Override
    public void valueChanged(final ListSelectionEvent e) {
      if (KoLmafia.isAdventuring()) {
        return;
      }

      KoLAdventure location = AdventureSelectPanel.this.getSelectedAdventure();
      if (location != null) {
        KoLAdventure.setNextAdventure(location);
      }

      AdventureSelectPanel.this.fillDefaultConditions();
    }

    @Override
    public void intervalAdded(final ListDataEvent e) {
      AdventureSelectPanel.this.fillCurrentConditions();
    }

    @Override
    public void intervalRemoved(final ListDataEvent e) {
      AdventureSelectPanel.this.fillCurrentConditions();
    }

    @Override
    public void contentsChanged(final ListDataEvent e) {
      AdventureSelectPanel.this.fillCurrentConditions();
    }
  }

  private class StopButton extends JButton implements ActionListener {
    public StopButton() {
      super("stop after");
      this.addActionListener(this);
      this.setToolTipText("Stop after current adventure");
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      KoLmafia.abortAfter("Manual stop requested.");
    }
  }

  private class ExecuteRunnable implements Runnable {
    private final PauseObject pauser = new PauseObject();

    @Override
    public void run() {
      KoLmafia.updateDisplay("Validating adventure sequence...");

      KoLAdventure request = AdventureSelectPanel.this.getSelectedAdventure();
      if (request == null) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "No location selected.");
        return;
      }

      // If GCLI has commands, complete them before automation
      while (!KoLmafia.refusesContinue() && CommandDisplayFrame.hasQueuedCommands()) {
        this.pauser.pause(500);
      }

      // If there are conditions in the condition field, be
      // sure to process them.

      boolean conditionsActive = AdventureSelectPanel.this.conditionsFieldActive.isSelected();
      String text = AdventureSelectPanel.this.conditionField.getText();
      String conditionList = text == null ? "" : text.trim().toLowerCase();

      List<AdventureResult> previousGoals = new ArrayList<>(GoalManager.getGoals());
      GoalManager.clearGoals();

      // Retain any stat goal
      for (AdventureResult previousGoal : previousGoals) {
        if (previousGoal.getName().equals(AdventureResult.SUBSTATS)) {
          GoalManager.addGoal(previousGoal);
          break;
        }
      }

      boolean shouldAdventure = true;

      if (conditionsActive && conditionList.length() > 0 && !conditionList.equals("none")) {
        shouldAdventure = this.handleConditions(conditionList, request);
      }

      if (!shouldAdventure) {
        return;
      }

      int requestCount =
          Math.min(
              InputFieldUtilities.getValue(AdventureSelectPanel.this.countField, 1),
              KoLCharacter.getAdventuresLeft());

      AdventureSelectPanel.this.countField.setValue(requestCount);
      boolean resetCount = requestCount == KoLCharacter.getAdventuresLeft();

      KoLmafia.makeRequest(request, requestCount);

      if (resetCount) {
        AdventureSelectPanel.this.countField.setValue(KoLCharacter.getAdventuresLeft());
      }
    }

    private boolean handleConditions(final String conditionList, final KoLAdventure request) {
      if (KoLmafia.isAdventuring()) {
        return false;
      }

      GoalManager.clearGoals();

      String[] splitConditions = conditionList.split("\\s*,\\s*");

      // First, figure out whether or not you need to do a
      // disjunction on the conditions, which changes how
      // KoLmafia handles them.

      for (String splitCondition : splitConditions) {
        if (splitCondition == null) {
          continue;
        }

        if (splitCondition.equals("check")) {
          // Postpone verification of conditions
          // until all other conditions added.
        } else if (splitCondition.equals("outfit")) {
          // Determine where you're adventuring
          // and use that to determine which
          // components make up the outfit pulled
          // from that area.

          if (!EquipmentManager.addOutfitConditions(request)) {
            return true;
          }
        } else if (splitCondition.startsWith("+")) {
          if (!ConditionsCommand.update("add", splitCondition.substring(1))) {
            return false;
          }
        } else if (!ConditionsCommand.update("set", splitCondition)) {
          return false;
        }
      }

      if (!GoalManager.hasGoals()) {
        KoLmafia.updateDisplay("All conditions already satisfied.");
        return false;
      }

      if (InputFieldUtilities.getValue(AdventureSelectPanel.this.countField) == 0) {
        AdventureSelectPanel.this.countField.setValue(KoLCharacter.getAdventuresLeft());
      }

      return true;
    }
  }

  private String getDefaultConditions() {
    KoLAdventure location = this.getSelectedAdventure();
    AdventureDatabase.getDefaultConditionsList(location, this.locationConditions);
    return this.locationConditions.get(0);
  }

  public void fillCurrentConditions() {
    String text = GoalManager.getGoalString();

    if (text.length() == 0) {
      text = this.getDefaultConditions();
    }

    this.conditionField.setText(text);
  }

  public void fillDefaultConditions() {
    this.conditionField.setText(this.getDefaultConditions());
  }

  public static JPanel getAdventureSummary(final String property) {
    int selectedIndex = Preferences.getInteger(property);

    CardLayout resultCards = new CardLayout();
    JPanel resultPanel = new JPanel(resultCards);
    JComboBox<String> resultSelect = new JComboBox<>();

    int cardCount = 0;

    resultSelect.addItem("Session Results");
    resultPanel.add(
        new GenericScrollPane((LockableListModel<AdventureResult>) KoLConstants.tally, 4),
        String.valueOf(cardCount++));

    resultSelect.addItem("Location Details");
    resultPanel.add(new SafetyField(), String.valueOf(cardCount++));

    resultSelect.addItem("Conditions Left");
    resultPanel.add(new GenericScrollPane(GoalManager.getGoals(), 4), String.valueOf(cardCount++));

    resultSelect.addItem("Available Skills");
    resultPanel.add(
        new GenericScrollPane((LockableListModel<UseSkillRequest>) KoLConstants.availableSkills, 4),
        String.valueOf(cardCount++));

    resultSelect.addItem("Active Effects");
    resultPanel.add(
        new GenericScrollPane((LockableListModel<AdventureResult>) KoLConstants.activeEffects, 4),
        String.valueOf(cardCount++));

    resultSelect.addItem("Encounter Listing");
    resultPanel.add(
        new GenericScrollPane(
            (LockableListModel<RegisteredEncounter>) KoLConstants.encounterList, 4),
        String.valueOf(cardCount++));

    resultSelect.addItem("Visited Locations");
    resultPanel.add(
        new GenericScrollPane(
            (LockableListModel<RegisteredEncounter>) KoLConstants.adventureList, 4),
        String.valueOf(cardCount++));

    resultSelect.addItem("Daily Deeds");
    resultPanel.add(new GenericScrollPane(new DailyDeedsPanel()), String.valueOf(cardCount++));

    resultSelect.addItem("Watched Preferences");
    resultPanel.add(
        new GenericScrollPane(new PreferenceWatcherTable()), String.valueOf(cardCount++));

    resultSelect.addActionListener(
        new ResultSelectListener(resultCards, resultPanel, resultSelect, property));

    if (selectedIndex >= cardCount) {
      selectedIndex = cardCount - 1;
    }

    resultSelect.setSelectedIndex(selectedIndex);

    JPanel containerPanel = new JPanel(new BorderLayout());
    containerPanel.add(resultSelect, BorderLayout.NORTH);
    containerPanel.add(resultPanel, BorderLayout.CENTER);

    return containerPanel;
  }

  private static class ResultSelectListener implements ActionListener {
    private final String property;
    private final CardLayout resultCards;
    private final JPanel resultPanel;
    private final JComboBox<String> resultSelect;

    public ResultSelectListener(
        final CardLayout resultCards,
        final JPanel resultPanel,
        final JComboBox<String> resultSelect,
        final String property) {
      this.resultCards = resultCards;
      this.resultPanel = resultPanel;
      this.resultSelect = resultSelect;
      this.property = property;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      String index = String.valueOf(this.resultSelect.getSelectedIndex());
      this.resultCards.show(this.resultPanel, index);
      Preferences.setString(this.property, index);
    }
  }

  private static class SafetyField extends JPanel implements Runnable {
    private String savedText = " ";
    private final RequestPane safetyDisplay;

    public SafetyField() {
      super(new BorderLayout());

      this.safetyDisplay = new RequestPane();

      JScrollPane safetyScroller =
          new JScrollPane(
              this.safetyDisplay,
              ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

      JComponentUtilities.setComponentSize(this.safetyDisplay, 100, 100);
      this.add(safetyScroller, BorderLayout.CENTER);

      CharacterListenerRegistry.addCharacterListener(new CharacterListener(this));

      this.setSafetyString();
    }

    @Override
    public void run() {
      this.setSafetyString();
    }

    private synchronized void setSafetyString() {
      KoLAdventure request = KoLCharacter.getSelectedLocation();

      if (request == null) {
        return;
      }

      AreaCombatData combat = request.getAreaSummary();
      String text = combat == null ? " " : combat.toString(true);

      // Avoid rendering and screen flicker if no change.
      // Compare with our own copy of what we set, since
      // getText() returns a modified version.

      if (text.equals(this.savedText)) {
        return;
      }

      this.savedText = text;
      this.safetyDisplay.setText(text);
    }
  }

  private class ConditionsComboBox extends EditableAutoFilterComboBox {
    public ConditionsComboBox() {
      super(AdventureSelectPanel.this.locationConditions);
    }
  }

  private class AdventureCountSpinner extends AutoHighlightSpinner implements ChangeListener {
    public AdventureCountSpinner() {
      super();
      this.addChangeListener(this);
      this.setToolTipText("Number of turns to adventure in the selected zone");
    }

    @Override
    public void stateChanged(final ChangeEvent e) {
      int maximum = KoLCharacter.getAdventuresLeft();
      if (maximum == 0) {
        this.setValue(0);
        return;
      }

      int desired = InputFieldUtilities.getValue(this, maximum);
      if (desired == maximum + 1) {
        this.setValue(1);
      } else if (desired <= 0 || desired > maximum) {
        this.setValue(maximum);
      }
    }
  }
}
