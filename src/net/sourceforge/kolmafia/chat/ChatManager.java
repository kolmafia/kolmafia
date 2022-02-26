package net.sourceforge.kolmafia.chat;

import java.awt.Toolkit;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.BuffBotHome;
import net.sourceforge.kolmafia.CreateFrameRunnable;
import net.sourceforge.kolmafia.EdServantData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.moods.RecoveryManager;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.AltarOfLiteracyRequest;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.ChannelColorsRequest;
import net.sourceforge.kolmafia.request.LoginRequest;
import net.sourceforge.kolmafia.request.SendMailRequest;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.EventManager;
import net.sourceforge.kolmafia.swingui.ChatFrame;
import net.sourceforge.kolmafia.swingui.ContactListFrame;
import net.sourceforge.kolmafia.swingui.GenericFrame;
import net.sourceforge.kolmafia.swingui.TabbedChatFrame;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.utilities.RollingLinkedList;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class ChatManager {
  private static final LinkedList<ChatMessage> clanMessages = new RollingLinkedList<>(20);
  private static final Set<String> validChatReplyRecipients = new HashSet<String>();

  private static final TreeMap<String, StyledChatBuffer> instantMessageBuffers =
      new TreeMap<String, StyledChatBuffer>();
  private static List<Entry<String, StyledChatBuffer>> bufferEntries = new ArrayList<>(0);

  private static boolean isRunning = false;
  private static boolean checkedLiteracy = false;
  private static boolean chatLiterate = false;

  private static String currentChannel = null;

  private static final List<String> activeWindows = new ArrayList<String>();
  public static List<String> activeChannels = new ArrayList<String>();

  private static TabbedChatFrame tabbedFrame = null;

  private static boolean triviaGameActive = false;
  private static int triviaGameIndex = 0;
  private static String triviaGameId = "[trivia0]";
  private static final LockableListModel<String> triviaGameContacts =
      new LockableListModel<String>();
  private static ContactListFrame triviaGameContactListFrame = null;

  private static String faxbot = null;
  private static ChatMessage faxbotMessage = null;

  public static final void reset() {
    ChatManager.dispose();
    ChatPoller.reset();

    ChatManager.clanMessages.clear();
    ChatManager.instantMessageBuffers.clear();
    ChatManager.bufferEntries = new ArrayList<>(0);
    ChatManager.activeChannels.clear();
    ChatManager.currentChannel = null;

    ChatManager.triviaGameActive = false;
    ChatManager.triviaGameIndex = 0;
    ChatManager.triviaGameId = "[trivia0]";
    ChatManager.triviaGameContacts.clear();

    ChatManager.faxbot = null;
    ChatManager.faxbotMessage = null;
  }

  public static final void resetChatLiteracy() {
    ChatManager.checkedLiteracy = false;
  }

  public static final void resetClanMessages() {
    ChatManager.clanMessages.clear();
  }

  public static final boolean checkedChatLiteracy() {
    return ChatManager.checkedLiteracy;
  }

  public static final boolean getChatLiteracy() {
    return ChatManager.chatLiterate;
  }

  public static final void setChatLiteracy(final boolean on) {
    ChatManager.checkedLiteracy = true;
    ChatManager.chatLiterate = on;
  }

  public static final boolean chatLiterate() {
    // If login is incomplete because we are stuck in a fight or
    // choice, don't bother checking the Altar of Literacy

    if (KoLmafia.isRefreshing()) {
      return true;
    }

    if (!ChatManager.checkedLiteracy) {
      ChatManager.chatLiterate = Preferences.getBoolean("chatLiterate");

      if (!ChatManager.chatLiterate) {
        AltarOfLiteracyRequest request = new AltarOfLiteracyRequest();
        RequestThread.postRequest(request);
      }

      Preferences.setBoolean("chatLiterate", ChatManager.chatLiterate);
    }

    return ChatManager.chatLiterate;
  }

  /**
   * Initializes the chat buffer with the provided chat pane. Note that the chat refresher will also
   * be initialized by calling this method; to stop the chat refresher, call the <code>dispose()
   * </code> method.
   */
  public static final void initialize() {
    if (ChatManager.isRunning || !LoginRequest.completedLogin()) {
      return;
    }

    if (!ChatManager.chatLiterate()) {
      KoLmafia.updateDisplay("You cannot access chat until you complete the Altar of Literacy");
      return;
    }

    ChatManager.isRunning = true;

    StyledChatBuffer.initializeHighlights();

    synchronized (ChatManager.activeChannels) {
      for (String channel : ChatManager.activeChannels) {
        ChatManager.openWindow(channel, false);
      }
    }

    ChatPoller.startInstance();

    RequestThread.postRequest(new ChannelColorsRequest());
  }

  public static final void setFaxBot(final String faxbot) {
    ChatManager.faxbot = faxbot;
  }

  public static final synchronized String getLastFaxBotMessage() {
    if (ChatManager.faxbotMessage != null) {
      String message = ChatManager.faxbotMessage.getContent();
      ChatManager.faxbotMessage = null;
      return message;
    }
    return null;
  }

  public static final boolean isValidChatReplyRecipient(String playerName) {
    synchronized (ChatManager.validChatReplyRecipients) {
      return validChatReplyRecipients.contains(playerName);
    }
  }

  /** Disposes the messenger's frames. */
  public static final void dispose() {
    if (ChatManager.isRunning) {
      ChatManager.isRunning = false;
      ChatSender.sendMessage(null, "/exit", false);
    }

    ChatPoller.stopInstance();

    ChatManager.activeWindows.clear();
    ChatManager.activeChannels.clear();
    ChatManager.currentChannel = null;

    ChatManager.tabbedFrame = null;
  }

  public static final boolean isRunning() {
    return ChatManager.isRunning;
  }

  public static final String getCurrentChannel() {
    return ChatManager.currentChannel;
  }

  public static final StyledChatBuffer getBuffer(final String bufferKey) {
    StyledChatBuffer buffer = ChatManager.instantMessageBuffers.get(bufferKey);

    if (buffer != null) {
      return buffer;
    }

    buffer = new StyledChatBuffer(bufferKey, "black", !bufferKey.equals("[high]"));

    if (Preferences.getBoolean("logChatMessages")) {
      String fileSuffix = bufferKey.replaceAll(" ", "_");

      if (fileSuffix.startsWith("/")) {
        fileSuffix = "[" + fileSuffix.substring(1) + "]";
      }

      String fileName =
          KoLConstants.DAILY_FORMAT.format(new Date())
              + "_"
              + KoLCharacter.baseUserName()
              + "_"
              + fileSuffix
              + ".html";
      buffer.setLogFile(new File(KoLConstants.CHATLOG_LOCATION, fileName));
    }

    synchronized (ChatManager.bufferEntries) {
      ChatManager.instantMessageBuffers.put(bufferKey, buffer);
      ChatManager.bufferEntries = new ArrayList<>(ChatManager.instantMessageBuffers.size());
      ChatManager.bufferEntries.addAll(ChatManager.instantMessageBuffers.entrySet());
    }

    return buffer;
  }

  public static final void startTriviaGame() {
    ChatManager.triviaGameContacts.clear();
    ChatManager.triviaGameId = "[trivia" + (++ChatManager.triviaGameIndex) + "]";
    ChatManager.triviaGameActive = true;

    if (ChatManager.triviaGameContactListFrame == null) {
      ChatManager.triviaGameContactListFrame = new ContactListFrame(ChatManager.triviaGameContacts);
    }

    ChatManager.triviaGameContactListFrame.setTitle("Contestants for " + triviaGameId);
    ChatManager.triviaGameContactListFrame.setVisible(true);
  }

  public static final void stopTriviaGame() {
    ChatManager.triviaGameActive = false;
  }

  public static void processMessages(final List<ChatMessage> messages) {
    for (ChatMessage message : messages) {
      ChatManager.processMessage(message);
    }
  }

  private static void displayMessage(
      final String bufferKey, final boolean highlight, final String message) {
    ChatManager.openWindow(bufferKey, highlight);
    StyledChatBuffer buffer = ChatManager.getBuffer(bufferKey);
    buffer.append(message);
  }

  public static void processMessage(final ChatMessage message) {
    if (message instanceof EventMessage) {
      ChatManager.processEvent((EventMessage) message);
      return;
    }

    if (message instanceof SystemMessage) {
      ChatManager.processSystemMessage((SystemMessage) message);
      return;
    }

    if (message instanceof EnableMessage) {
      ChatManager.processChannelEnable((EnableMessage) message);
      return;
    }

    if (message instanceof DisableMessage) {
      ChatManager.processChannelDisable((DisableMessage) message);
      return;
    }

    String sender = message.getSender();
    String recipient = message.getRecipient();

    if (message instanceof ModeratorMessage) {
      // Format the message
      String displayHTML = ChatFormatter.formatChatMessage(message);

      // Display the message in the current channel
      ChatManager.displayMessage(ChatManager.getBufferKey(recipient), true, displayHTML);
      return;
    }

    if (ChatManager.faxbot != null && sender.equalsIgnoreCase(ChatManager.faxbot)) {
      ChatManager.faxbotMessage = message;
    }

    String content = message.getContent();

    if (recipient == null) {
      ChatManager.processCommand(sender, content, recipient);
    } else if (recipient.equals("/clan")
        || recipient.equals("/hobopolis")
        || recipient.equals("/slimetube")
        || recipient.equals("/dread")
        || recipient.equals("/hauntedhouse")) {
      ChatManager.clanMessages.add(message);
      ChatManager.processCommand(sender, content, recipient);
    } else if (recipient.equals("/talkie")) {
      // Allow chatbot scripts to process talkie messages
      ChatManager.processCommand(sender, content, recipient);
    } else if (Preferences.getBoolean("chatBeep")
        && (StringUtilities.globalStringReplace(KoLCharacter.getUserName(), " ", "_")
            .equalsIgnoreCase(recipient))) {
      Toolkit.getDefaultToolkit().beep();
    }

    String destination = recipient;

    if (KoLCharacter.getUserName().equals(recipient)) {
      if (ChatManager.triviaGameActive) {
        if (!ChatManager.triviaGameContacts.contains(message.getSender())) {
          ChatManager.triviaGameContacts.add(message.getSender());
        }
      }

      ChatManager.processCommand(sender, content, "");
      destination = sender;
    }

    // Display the message in the appropriate tab
    ChatManager.displayMessage(
        ChatManager.getBufferKey(destination), true, ChatFormatter.formatChatMessage(message));
  }

  public static final String getBufferKey(String destination) {
    String bufferKey = destination.toLowerCase();

    if (Preferences.getBoolean("mergeHobopolisChat")) {
      if (destination.equals("/hobopolis")
          || destination.equals("/slimetube")
          || destination.equals("/dread")
          || destination.equals("/hauntedhouse")) {
        bufferKey = "/clan";
      }
    }

    if (!bufferKey.startsWith("/") && ChatManager.triviaGameActive) {
      bufferKey = triviaGameId;
    }

    return bufferKey;
  }

  public static final void processEvent(final EventMessage message) {
    String content = message.getContent();

    // Parse buffs bestowed upon us, etc.
    ChatManager.parseEvent(content);

    // If we are suppressing event display, bail now
    if (Preferences.getBoolean("greenScreenProtection")
        || BuffBotHome.isBuffBotActive()
        || message.isHidden()) {
      return;
    }

    // Otherwise, munge it, save it, and display it
    EventManager.addChatEvent(ChatFormatter.formatChatMessage(message, false));
    String cleanContent = KoLConstants.ANYTAG_PATTERN.matcher(content).replaceAll("");
    ChatManager.processCommand("", cleanContent, "Events");
    ChatManager.broadcastEvent(message);
  }

  // <a class=nounder target=mainpane href=showplayer.php?who=121572><b>Veracity</b></a> has played
  // Fat Leon's Phat Loot Lyric  for you. (10 Adventures)

  // An Elemental Saucesphere has been conjured around you by <a class=nounder target=mainpane
  // href=showplayer.php?who=121572><b>Veracity</b></a> (5 Adventures)
  // An Jalape&ntilde;o Saucesphere has been conjured around you by <a class=nounder target=mainpane
  // href=showplayer.php?who=121572><b>Veracity</b></a> (5 Adventures)
  // An Antibiotic Saucesphere has been conjured around you by <a class=nounder target=mainpane
  // href=showplayer.php?who=121572><b>Veracity</b></a> (5 Adventures)
  // A layer of Scarysauce has been conjured around you by <a class=nounder target=mainpane
  // href=showplayer.php?who=121572><b>Veracity</b></a> (5 Adventures)

  // <a href='showplayer.php?who=121572' style='color: green' target='mainpane'>Veracity</a> has
  // given you a psychokinetic hug!

  // <a class=nounder target=mainpane href=showplayer.php?who=121572><b>Veracity</b></a> has
  // fortified you with the Empathy of the Newt. (10 Adventures)
  // <a class=nounder target=mainpane href=showplayer.php?who=121572><b>Veracity</b></a> has imbued
  // you with Reptilian Fortitude. (10 Adventures)
  // <a class=nounder target=mainpane href=showplayer.php?who=121572><b>Veracity</b></a> has given
  // you the Tenacity of the Snapper. (10 Adventures)
  // An Astral Shell has been conjured around you by <a class=nounder target=mainpane
  // href=showplayer.php?who=121572><b>Veracity</b></a>. (10 Adventures)
  // A Ghostly Shell has been conjured around you by <a class=nounder target=mainpane
  // href=showplayer.php?who=121572><b>Veracity</b></a>. (10 Adventures)

  private static final Pattern BUFF_PATTERN =
      Pattern.compile(
          "(?:A layer of |A |An |has played |has fortified you with |has imbued you with |has given you the )(.*?) *(?:for you|has been conjured around you|\\.).*?\\(([\\d]*) Adventures\\)");

  // &quot;Hey, Lazy Servant, get to work&quot; you bark at your
  // Priest.<!--js(parent.charpane.location.href="charpane.php";)-->

  private static final Pattern BARK_PATTERN = Pattern.compile("you bark at your (.*?)\\.");

  public static final void parseEvent(final String content) {
    if (content.contains("Lazy Servant")) {
      Matcher barkMatcher = BARK_PATTERN.matcher(content);
      if (barkMatcher.find()) {
        String servantType = barkMatcher.group(1);
        EdServantData.setEdServant(servantType);
      }
      return;
    }

    if (content.contains("New message received from") || content.contains("has sent you")) {
      // May have been sent items, and API request is low impact.
      ApiRequest.updateInventory(true);
    }

    if (content.contains("just attacked you!")) {
      // May have lost items, and API request is low impact.
      ApiRequest.updateInventory(true);
    }

    if (content.contains(" has ")) {
      // Refresh effects via api.php
      ApiRequest.updateStatus(true);
    }
  }

  public static final void processSystemMessage(final SystemMessage message) {
    // Format the message
    String displayHTML = ChatFormatter.formatChatMessage(message);

    // If the user doesn't want events to appear in all tabs,
    // put the System Message only in the Events tab
    if (!Preferences.getBoolean("broadcastEvents")) {
      // Put the message in the Events tab and highlight it
      ChatManager.displayMessage("[events]", true, displayHTML);
      return;
    }

    // Otherwise, broadcast it
    synchronized (ChatManager.bufferEntries) {
      for (Entry<String, StyledChatBuffer> entry : ChatManager.bufferEntries) {
        String key = entry.getKey();
        StyledChatBuffer buffer = entry.getValue();

        buffer.append(displayHTML);

        // Open the Events tab
        if (key.equals("[events]")) {
          ChatManager.openWindow(key, false);
        }
      }
    }
  }

  public static final void processChannelEnable(final EnableMessage message) {
    String sender = message.getSender();

    if (!ChatManager.activeChannels.contains(sender)) {
      String bufferKey = ChatManager.getBufferKey(sender);
      ChatManager.activeChannels.add(sender);
      ChatManager.openWindow(bufferKey, false);
    }

    if (message.isTalkChannel()) {
      ChatManager.currentChannel = sender;
    }
  }

  public static final void processChannelDisable(final DisableMessage message) {
    String sender = message.getSender();

    if (ChatManager.activeChannels.contains(sender)) {
      String bufferKey = ChatManager.getBufferKey(sender);
      ChatManager.activeChannels.remove(sender);
      ChatManager.closeWindow(bufferKey);
    }
  }

  public static final void processCommand(
      final String sender, final String content, final String channel) {
    if (sender == null || content == null) {
      return;
    }

    // If a buffbot is running, certain commands become active, such
    // as help, restores, and logoff.

    if ("".equals(channel) && BuffBotHome.isBuffBotActive()) {
      if (content.equalsIgnoreCase("help")) {
        ChatSender.sendMessage(sender, "Please check my profile.", false);
        return;
      }

      if (content.equalsIgnoreCase("restores")) {
        ChatSender.sendMessage(
            sender,
            "I currently have "
                + RecoveryManager.getRestoreCount()
                + " mana restores at my disposal.",
            false);

        return;
      }

      if (content.equalsIgnoreCase("logoff")) {
        BuffBotHome.update(BuffBotHome.ERRORCOLOR, "Logoff requested by " + sender);

        if (ClanManager.isCurrentMember(sender)) {
          KoLmafia.quit();
        }

        BuffBotHome.update(BuffBotHome.ERRORCOLOR, sender + " added to ignore list");
        ChatSender.sendMessage(sender, "/baleet", false);
        return;
      }
    }

    // Otherwise, sometimes clannies want to take advantage of KoLmafia's
    // automatic chat logging.  In that case...

    if ("".equals(channel) && content.equalsIgnoreCase("update")) {
      if (!Preferences.getBoolean("chatServesUpdates")) {
        KoLmafia.updateDisplay(
            sender
                + " tried to retrieve recent clan chat messages from you, but you have that feature disabled.");
        return;
      }
      if (!ClanManager.isCurrentMember(sender)) {
        KoLmafia.updateDisplay(
            sender
                + " tried to retrieve recent clan chat messages from you, but they are not in your clan.");
        return;
      }

      StringBuilder mailContent = new StringBuilder();

      Iterator<ChatMessage> clanMessageIterator = ChatManager.clanMessages.iterator();

      while (clanMessageIterator.hasNext()) {
        ChatMessage message = clanMessageIterator.next();
        String cleanMessage =
            KoLConstants.ANYTAG_PATTERN
                .matcher(ChatFormatter.formatChatMessage(message))
                .replaceAll("");

        mailContent.append(cleanMessage);
        mailContent.append("\n");
      }

      RequestThread.postRequest(new SendMailRequest(sender, mailContent.toString()));
      KoLmafia.updateDisplay("Recent clan chat messages were sent to " + sender);
      return;
    }

    ChatManager.invokeChatScript(sender, content, channel);
  }

  public static final void invokeChatScript(
      final String sender, final String content, final String channel) {
    String scriptName = Preferences.getString("chatbotScript");
    if (scriptName.equals("")) {
      return;
    }

    List<File> scriptFiles = KoLmafiaCLI.findScriptFile(scriptName);
    ScriptRuntime interpreter = KoLmafiaASH.getInterpreter(scriptFiles);
    if (interpreter == null) {
      return;
    }

    String name = scriptFiles.get(0).getName();
    int parameterCount = 3;
    if (interpreter instanceof AshRuntime) {
      parameterCount =
          ((AshRuntime) interpreter).getParser().getMainMethod().getVariableReferences().size();
    }

    String[] scriptParameters;
    if (parameterCount == 3) {
      scriptParameters = new String[] {sender, content, channel};
    } else if (channel != null && !channel.equals("")) {
      scriptParameters = new String[] {sender, content};
    } else {
      return;
    }

    synchronized (ChatManager.validChatReplyRecipients) {
      ChatManager.validChatReplyRecipients.add(sender);
    }

    synchronized (interpreter) {
      KoLmafiaASH.logScriptExecution("Starting chat script: ", name, interpreter);
      interpreter.execute("main", scriptParameters);
      KoLmafiaASH.logScriptExecution("Finished chat script: ", name, interpreter);
    }

    synchronized (ChatManager.validChatReplyRecipients) {
      ChatManager.validChatReplyRecipients.remove(sender);
    }
  }

  public static final void broadcastEvent(final EventMessage message) {
    if (!ChatManager.isRunning()) {
      return;
    }

    String displayHTML = ChatFormatter.formatChatMessage(message);

    StyledChatBuffer buffer = ChatManager.getBuffer("[events]");
    buffer.append(displayHTML);

    if (message instanceof InternalMessage || !Preferences.getBoolean("broadcastEvents")) {
      ChatManager.openWindow("[events]", true);
    } else {
      ChatManager.openWindow("[events]", false);

      synchronized (ChatManager.bufferEntries) {
        for (Entry<String, StyledChatBuffer> entry : ChatManager.bufferEntries) {
          String key = entry.getKey();

          if (key.equals("[events]")) {
            continue;
          }

          buffer = entry.getValue();

          buffer.append(displayHTML);
        }
      }
    }
  }

  public static final void openWindow(final String bufferKey, boolean highlightOnOpen) {
    if (StaticEntity.isHeadless() || !ChatManager.isRunning || bufferKey == null) {
      return;
    }

    if (ChatManager.activeWindows.contains(bufferKey)) {
      if (ChatManager.tabbedFrame != null) {
        ChatManager.tabbedFrame.highlightTab(bufferKey);
      }

      return;
    }

    ChatManager.activeWindows.add(bufferKey);

    if (Preferences.getBoolean("useTabbedChatFrame")) {
      if (ChatManager.tabbedFrame == null) {
        CreateFrameRunnable creator = new CreateFrameRunnable(TabbedChatFrame.class);
        boolean appearsInTab = GenericFrame.appearsInTab("ChatManager");
        ChatManager.tabbedFrame = (TabbedChatFrame) creator.createFrame(appearsInTab);
      }

      ChatManager.tabbedFrame.addTab(bufferKey);

      if (highlightOnOpen) {
        ChatManager.tabbedFrame.highlightTab(bufferKey);
      }

      return;
    }

    ChatFrame frame = new ChatFrame(bufferKey);
    CreateFrameRunnable.decorate(frame);
    frame.setVisible(true);
  }

  public static final void closeWindow(String closedWindow) {
    if (closedWindow == null) {
      ChatManager.dispose();
      return;
    }

    ChatManager.activeWindows.remove(closedWindow);

    if (!ChatManager.isRunning() || !closedWindow.startsWith("/")) {
      return;
    }

    if (!ChatManager.activeChannels.contains(closedWindow)) {
      return;
    }

    if (!closedWindow.equals(ChatManager.getCurrentChannel())) {
      ChatSender.sendMessage(closedWindow, "/listen", false);
      return;
    }

    String selectedWindow = null;

    for (String channel : ChatManager.activeChannels) {
      if (channel.startsWith("/") && !channel.equals(closedWindow)) {
        selectedWindow = channel;
        break;
      }
    }

    if (selectedWindow != null) {
      ChatSender.sendMessage(selectedWindow, "/channel", false);
      return;
    }
  }

  public static final void checkFriends() {
    ChatSender.sendMessage(null, "/friends", false);
  }

  public static void applyHighlights() {
    for (Entry<String, StyledChatBuffer> entry : ChatManager.bufferEntries) {
      StyledChatBuffer buffer = entry.getValue();
      buffer.applyHighlights();
    }
  }
}
