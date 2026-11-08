package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase.FaxBot;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase.Monster;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.FaxRequestManager;
import net.sourceforge.kolmafia.swingui.panel.CardLayoutSelectorPanel;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.panel.ScrollablePanel;
import net.sourceforge.kolmafia.swingui.panel.StatusPanel;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;

public class FaxRequestFrame extends GenericFrame implements ChangeListener {
  private static final int ROWS = 15;

  private CardLayoutSelectorPanel selectorPanel = null;

  static {
    FaxBotDatabase.configure();
  }

  public FaxRequestFrame() {
    super("Request a Fax");
    this.selectorPanel = new CardLayoutSelectorPanel(null, "MMMMMMMMMMMM");
    for (FaxBot bot : FaxBotDatabase.faxbots) {
      JPanel panel = new JPanel(new BorderLayout());
      FaxRequestPanel botPanel = new FaxRequestPanel(bot);
      panel.add(botPanel);
      this.selectorPanel.addPanel(bot.getName(), panel);
    }
    // Gets the index of the last faxbot selected in gui
    int lastFaxbot =
        this.selectorPanel.panelNames.indexOf(Preferences.getString("lastSelectedFaxbot"));
    // If not found, defaults to -1. Set selected index to 0 or higher
    this.selectorPanel.setSelectedIndex(Math.max(0, lastFaxbot));
    this.selectorPanel.addChangeListener(this);
    this.setCenterComponent(this.selectorPanel);
    this.add(new StatusPanel(), BorderLayout.SOUTH);
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    Object panelName = this.selectorPanel.panelNames.get(this.selectorPanel.getSelectedIndex());

    if (!(panelName instanceof String)) return;

    Preferences.setString("lastSelectedFaxbot", (String) panelName);
  }

  private class FaxRequestPanel extends GenericPanel {
    private final FaxBot bot;

    public List<ShowDescriptionList<Monster>> monsterLists;
    public int monsterIndex;
    private final MonsterCategoryComboBox categorySelect;
    private final MonsterSelectPanel monsterSelect;

    public FaxRequestPanel(FaxBot bot) {
      super("request", "online?", new Dimension(75, 24), new Dimension(200, 24));

      this.bot = bot;

      List<LockableListModel<Monster>> monstersByCategory = bot.getMonstersByCategory();
      int categories = monstersByCategory.size();
      this.monsterLists = new ArrayList<>(categories);
      for (int i = 0; i < categories; ++i) {
        this.monsterLists.add(new ShowDescriptionList<>(monstersByCategory.get(i), ROWS));
      }

      this.categorySelect = new MonsterCategoryComboBox(this, bot);
      this.monsterSelect = new MonsterSelectPanel(this.monsterLists.get(0));
      this.monsterIndex = 0;

      VerifiableElement[] elements = new VerifiableElement[1];
      elements[0] = new VerifiableElement("Category: ", this.categorySelect);

      this.setContent(elements);
      this.add(this.monsterSelect, BorderLayout.CENTER);
    }

    @Override
    public boolean shouldAddStatusLabel() {
      return false;
    }

    @Override
    public void setEnabled(final boolean isEnabled) {
      super.setEnabled(isEnabled);
      if (this.categorySelect != null) {
        this.categorySelect.setEnabled(isEnabled);
      }
      if (this.monsterSelect != null) {
        this.monsterSelect.setEnabled(isEnabled);
      }
    }

    @Override
    public void actionConfirmed() {
      int list = this.monsterIndex;
      Monster monster = monsterLists.get(list).getSelectedValue();
      if (monster == null) {
        return;
      }

      String botName = this.bot.getName();
      FaxRequestFrame.requestFax(botName, monster);
    }

    @Override
    public void actionCancelled() {
      String botName = this.bot.getName();
      String message =
          FaxRequestFrame.isBotOnline(botName)
              ? botName + " is online."
              : FaxRequestManager.getStatusMessage();
      KoLmafia.updateDisplay(message);
    }
  }

  public static boolean requestFax(final String botName, final Monster monster) {
    return FaxRequestManager.requestFax(botName, monster);
  }

  public static boolean requestFax(
      final String botName, final Monster monster, final boolean checkOnline) {
    return FaxRequestManager.requestFax(botName, monster, checkOnline);
  }

  public static boolean isBotOnline(final String botName) {
    return FaxRequestManager.isBotOnline(botName);
  }

  private class MonsterCategoryComboBox extends JComboBox<String> {
    FaxRequestPanel panel;

    public MonsterCategoryComboBox(FaxRequestPanel panel, FaxBot bot) {
      super();
      this.panel = panel;
      for (String category : bot.getCategories()) {
        addItem(category);
      }
      addActionListener(new MonsterCategoryListener());
    }

    private class MonsterCategoryListener implements ActionListener {
      @Override
      public void actionPerformed(final ActionEvent e) {
        int index = MonsterCategoryComboBox.this.getSelectedIndex();
        MonsterCategoryComboBox.this.panel.monsterIndex = index;
        MonsterCategoryComboBox.this.panel.monsterSelect.setElementList(
            MonsterCategoryComboBox.this.panel.monsterLists.get(index));
      }
    }
  }

  private static class MonsterSelectPanel extends ScrollablePanel<ShowDescriptionList<Monster>> {
    private ShowDescriptionList<Monster> elementList;
    private final AutoFilterTextField<Monster> filterField;

    public MonsterSelectPanel(final ShowDescriptionList<Monster> list) {
      super("", null, null, list, false);

      this.elementList = list;
      this.elementList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      this.elementList.setVisibleRowCount(8);

      this.filterField = new AutoFilterTextField<>(this.elementList);
      this.centerPanel.add(this.filterField, BorderLayout.NORTH);
    }

    public void setElementList(final ShowDescriptionList<Monster> list) {
      this.elementList = list;
      this.scrollPane.getViewport().setView(list);
      this.filterField.setList(list);
    }
  }
}
