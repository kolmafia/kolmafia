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
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.chat.ChatSender;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase.FaxBot;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase.Monster;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest.Action;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.swingui.panel.CardLayoutSelectorPanel;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.panel.ScrollablePanel;
import net.sourceforge.kolmafia.swingui.panel.StatusPanel;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.PauseObject;

public class FaxRequestFrame extends GenericFrame {
  private static final int ROWS = 15;
  private static final int LIMIT = 60;
  private static final int DELAY = 200;

  private CardLayoutSelectorPanel selectorPanel = null;
  private static String statusMessage;

  static {
    FaxBotDatabase.configure();
  }

  public FaxRequestFrame() {
    super("Request a Fax");
    this.selectorPanel = new CardLayoutSelectorPanel("faxbots", "MMMMMMMMMMMM");
    for (FaxBot bot : FaxBotDatabase.faxbots) {
      JPanel panel = new JPanel(new BorderLayout());
      FaxRequestPanel botPanel = new FaxRequestPanel(bot);
      panel.add(botPanel);
      this.selectorPanel.addPanel(bot.getName(), panel);
    }
    this.selectorPanel.setSelectedIndex(0);
    this.setCenterComponent(this.selectorPanel);
    this.add(new StatusPanel(), BorderLayout.SOUTH);
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
      if (FaxRequestFrame.isBotOnline(botName)) {
        FaxRequestFrame.statusMessage = botName + " is online.";
      }
      KoLmafia.updateDisplay(FaxRequestFrame.statusMessage);
    }
  }

  public static boolean requestFax(final String botName, final Monster monster) {
    return FaxRequestFrame.requestFax(botName, monster, true);
  }

  public static boolean requestFax(
      final String botName, final Monster monster, final boolean checkOnline) {
    // Validate ability to receive a fax
    if (!FaxRequestFrame.canReceiveFax()) {
      KoLmafia.updateDisplay(FaxRequestFrame.statusMessage);
      return false;
    }

    // Make sure FaxBot is online
    if (checkOnline && !FaxRequestFrame.isBotOnline(botName)) {
      KoLmafia.updateDisplay(FaxRequestFrame.statusMessage);
      return false;
    }

    // Make sure we can receive chat messages, either via KoLmafia chat or in the Relay Browser.
    if (!(ChatManager.isRunning() || true)) {
      FaxRequestFrame.statusMessage =
          "You must be in chat so we can receive messages from " + botName;
      KoLmafia.updateDisplay(FaxRequestFrame.statusMessage);
      return false;
    }

    // Do you already have a photocopied monster?
    if (InventoryManager.hasItem(ItemPool.PHOTOCOPIED_MONSTER)) {
      String current = Preferences.getString("photocopyMonster");
      if (current.equals("")) {
        current = "monster";
      }

      // Yes. Offer a chance to discard it right now
      if (!InputFieldUtilities.confirm(
          "You have a photocopied " + current + " in your inventory. Dump it?")) {
        FaxRequestFrame.statusMessage =
            "You need to dispose of your photocopied " + current + " before you can receive a fax.";
        KoLmafia.updateDisplay(FaxRequestFrame.statusMessage);
        return false;
      }

      ClanLoungeRequest request =
          new ClanLoungeRequest(Action.FAX_MACHINE, ClanLoungeRequest.SEND_FAX);
      RequestThread.postRequest(request);
    }

    String name = monster.getName();
    String command = monster.getCommand();

    // We can try several times...
    PauseObject pauser = new PauseObject();

    String message = "Asking " + botName + " to send a fax" + " of " + name + ": " + command;

    try {
      ChatManager.setFaxBot(botName);

      while (true) {
        KoLmafia.updateDisplay(message);

        // Clear last message, just in case.
        ChatManager.getLastFaxBotMessage();

        ChatSender.sendMessage(botName, command, false);

        String response = null;
        // Response is sent blue message. Can it fail?

        int polls = LIMIT * 1000 / DELAY;
        for (int i = 0; i < polls; ++i) {
          response = ChatManager.getLastFaxBotMessage();
          if (response != null) {
            break;
          }
          pauser.pause(DELAY);
        }

        if (response == null) {
          FaxRequestFrame.statusMessage =
              "No response from " + botName + " after " + LIMIT + " seconds.";
          KoLmafia.updateDisplay(FaxRequestFrame.statusMessage);
          return false;
        }

        // FaxBot just delivered a fax to your clan, please try again in 1 minute.
        if (response.contains("just delivered a fax")) {
          FaxRequestFrame.statusMessage =
              botName + " recently delivered another fax. Retrying in one minute.";
          KoLmafia.updateDisplay(FaxRequestFrame.statusMessage);
          KoLmafia.forceContinue();
          StaticEntity.executeCountdown("Countdown: ", 60);
          continue;
        }

        // parse FaxBot's response
        if (!FaxRequestFrame.faxAvailable(botName, response)) {
          KoLmafia.updateDisplay(FaxRequestFrame.statusMessage);
          return false;
        }

        // Success! No need to retry
        break;
      }
    } finally {
      ChatManager.setFaxBot(null);
    }

    // The monster is there! retrieve it.
    ClanLoungeRequest request =
        new ClanLoungeRequest(Action.FAX_MACHINE, ClanLoungeRequest.RECEIVE_FAX);
    RequestThread.postRequest(request);
    KoLmafia.enableDisplay();
    return true;
  }

  private static boolean canReceiveFax() {
    // Are you allowed to use chat?
    if (!ChatManager.chatLiterate()) {
      FaxRequestFrame.statusMessage = "You are not allowed to use chat.";
      return false;
    }

    // Do you have a VIP key?
    if (!InventoryManager.hasItem(ClanLoungeRequest.VIP_KEY)) {
      FaxRequestFrame.statusMessage = "You don't have a VIP key.";
      return false;
    }

    // Are you Trendy?
    if (KoLCharacter.isTrendy() || KoLCharacter.getRestricted()) {
      FaxRequestFrame.statusMessage = "Fax machines are out of style.";
      return false;
    }

    // Are you an Avatar of Boris?
    if (KoLCharacter.inAxecore()) {
      FaxRequestFrame.statusMessage = "Boris sneered at technology.";
      return false;
    }

    // Are you an Avatar of Jarlsberg?
    if (KoLCharacter.isJarlsberg()) {
      FaxRequestFrame.statusMessage = "Jarlsberg was more into magic than technology.";
      return false;
    }

    // Are you an Avatar of Sneaky Pete?
    if (KoLCharacter.isSneakyPete()) {
      FaxRequestFrame.statusMessage =
          "Have you ever seen a cool person use a fax machine? I didn't think so.";
      return false;
    }

    // Try to visit the fax machine
    ClanLoungeRequest request = new ClanLoungeRequest(Action.FAX_MACHINE);
    RequestThread.postRequest(request);

    // Are you in a clan?
    String redirect = request.redirectLocation;
    if (redirect != null && redirect.equals("clan_signup.php")) {
      FaxRequestFrame.statusMessage = "You are not in a clan.";
      return false;
    }

    // Does your clan have a fax machine?
    if (!request.responseText.contains("You approach the fax machine")) {
      FaxRequestFrame.statusMessage = "Your clan does not have a fax machine.";
      return false;
    }
    return true;
  }

  private static boolean faxAvailable(final String botName, final String response) {
    // FaxBot has copied a Rockfish into your clan's Fax Machine.
    // Your monster has been delivered to your clan Fax Machine. Thank you for using FaustBot.
    // Your fax is ready.
    if (response.contains("into your clan's Fax Machine")
        || response.contains("delivered to your clan Fax Machine")
        || response.contains("Your fax is ready")) {
      return true;
    }

    // Sorry, it appears you requested an invalid monster.
    // I couldn't find that monster. Try sending "list" for a list of monster names.
    // I do not recognize that monster
    if (response.contains("I do not understand your request")
        || response.contains("you requested an invalid monster")
        || response.contains("I couldn't find that monster")
        || response.contains("I do not recognize that monster")) {
      FaxRequestFrame.statusMessage = "Configuration error: unknown command sent to " + botName;
      return false;
    }

    // I am unable to whitelist to clan 'xxx', please verify faustbot (#2504770) is whitelisted.
    // Thank you.
    // I couldn't get into your clan
    // I am not whitelisted
    if (response.contains("could not whitelist")
        || response.contains("unable to whitelist")
        || response.contains("I couldn't get into your clan")
        || response.contains("I am not whitelisted")) {
      FaxRequestFrame.statusMessage = botName + " is not on your clan's whitelist";
      return false;
    }

    // You are only allowed 20 fax requests per day. Please
    // try again tomorrow.

    FaxRequestFrame.statusMessage = response;
    return false;
  }

  public static boolean isBotOnline(final String botName) {
    // Return false and set FaxRequestFrame.statusMessage to an appropriate
    // message if the bot is NOT online.

    if (botName == null) {
      FaxRequestFrame.statusMessage = "No faxbots configured.";
      return false;
    }

    if (!KoLmafia.isPlayerOnline(botName)) {
      FaxRequestFrame.statusMessage = botName + " is probably not online.";
      return false;
    }

    // Do not bother allocating a message if bot is online
    return true;
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
