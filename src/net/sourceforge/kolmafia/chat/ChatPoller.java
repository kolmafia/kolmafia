package net.sourceforge.kolmafia.chat;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.request.ChatRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.RollingLinkedList;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ChatPoller extends Thread {
  // The most recent HistoryEntries we processed
  private static final LinkedList<HistoryEntry> chatHistoryEntries = new RollingLinkedList<>(25);

  // The sequence number of the last HistoryEntry we have added
  public static long localLastSeen = 0;

  // The sequence number of the last poll from a chat client, either our
  // chat GUI or the browser
  public static long serverLastSeen = 0;

  // ***
  public static long localLastSent = 0;

  // Milliseconds between polls. Extracted from the Javascript source on
  // Oct 21, 2022
  private static final int LCHAT_DELAY_NORMAL = 3000;
  private static final int LCHAT_DELAY_PAUSED = 10000;
  private static final int MCHAT_DELAY_NORMAL = 3000;
  private static final int MCHAT_DELAY_PAUSED = 10000;

  // lchat and mchat like to go into "away" mode after 15 minutes.  If
  // you are running GUI chat and browser chat at the same time, let the
  // browser chat go first.
  private static final int AWAY_MODE_THRESHOLD = (16 * 60 * 1000);

  private static final String AWAY_MESSAGE =
      "You are now in away mode, chat will update more slowly until you say something.";
  private static final String BACK_MESSAGE = "Welcome back!  Away mode disabled.";

  // timestamps
  public static Date lastServerPoll = new Date(0);
  public static Date lastSentMessage = new Date(0);
  public static long lastLocalSent = 0;

  private static String rightClickMenu = "";

  public static final void reset() {
    ChatPoller.chatHistoryEntries.clear();

    ChatPoller.serverLastSeen = 0;
    ChatPoller.localLastSeen = 0;
    ChatPoller.localLastSent = 0;
  }

  // The instance of the chat poller currently serving the chat GUI
  private static ChatPoller INSTANCE = null;

  // The GUI chat poller is running
  private boolean running = false;

  // The GUI chat poller is in "away" mode or not
  private boolean paused = false;

  // The delay between polls
  private int delay = ChatPoller.LCHAT_DELAY_NORMAL;

  public static ChatPoller getInstance() {
    return ChatPoller.INSTANCE;
  }

  public static void startInstance() {
    if (ChatPoller.INSTANCE == null) {
      ChatPoller.INSTANCE = new ChatPoller();
      ChatPoller.INSTANCE.start();
    }
  }

  public static void stopInstance() {
    if (ChatPoller.INSTANCE != null) {
      ChatPoller.INSTANCE.running = false;
      ChatPoller.INSTANCE = null;
    }
  }

  // Things that KoL tells us

  public static void setServerLast(final long last) {
    // The "last" field of a JSON mchat response
    ChatPoller.serverLastSeen = last;
  }

  public static void setServerDelay(int delay) {
    // The "delay" field of a JSON mchat response
    if (ChatPoller.INSTANCE != null) {
      // mchat does not choose delay; KoL specifies it after each poll
      // However, mchat rejects wildly inappropriate values. So do we.

      if (delay < 1 || delay > 60000) {
        delay = ChatPoller.MCHAT_DELAY_NORMAL;
      }

      ChatPoller.INSTANCE.delay = delay;
    }
  }

  public static void pauseChat(final boolean paused, final boolean mchat) {
    if (ChatPoller.INSTANCE != null) {
      if (paused) {
        ChatPoller.INSTANCE.pause(mchat);
      } else {
        ChatPoller.INSTANCE.unpause(mchat);
      }
    }
  }

  private void pause(final boolean mchat) {
    if (!this.paused) {
      this.paused = true;
      this.delay = mchat ? ChatPoller.MCHAT_DELAY_PAUSED : ChatPoller.LCHAT_DELAY_PAUSED;

      // mchat gives us the AWAY message as an event
      if (!mchat) {
        // If this is not from mchat, craft our own event
        EventMessage message = new EventMessage(ChatPoller.AWAY_MESSAGE, "green");
        ChatManager.broadcastEvent(message);
      }

      NamedListenerRegistry.fireChange("[chatAway]");
    }
  }

  private void unpause(final boolean mchat) {
    if (this.paused) {
      this.paused = false;
      this.delay = mchat ? ChatPoller.MCHAT_DELAY_NORMAL : ChatPoller.LCHAT_DELAY_NORMAL;

      // mchat gives us the BACK message as an event
      if (!mchat) {
        // If this is not from mchat, craft our own event
        EventMessage message = new EventMessage(ChatPoller.BACK_MESSAGE, "green");
        ChatManager.broadcastEvent(message);
      }

      NamedListenerRegistry.fireChange("[chatAway]");
    }
  }

  public static boolean isPaused() {
    return ChatPoller.INSTANCE != null && ChatPoller.INSTANCE.paused;
  }

  public static void serverPolled() {
    ChatPoller.lastServerPoll = new Date();
  }

  public static void sentMessage(final boolean mchat) {
    ChatPoller.lastSentMessage = new Date();
    if (ChatPoller.INSTANCE != null) {
      ChatPoller.INSTANCE.unpause(mchat);
    }
  }

  // The executable method which polls using the "lchat" protocol

  @Override
  public void run() {
    PauseObject pauser = new PauseObject();

    this.running = true;
    this.paused = false;

    // Since we just entered chat, pretend that we just sent a message
    ChatPoller.lastSentMessage = new Date();

    while (this.running) {
      Date now = new Date();
      try {
        // Only poll if the browser has not polled recently enough
        long serverLast;
        synchronized (ChatPoller.lastServerPoll) {
          serverLast = ChatPoller.lastServerPoll.getTime();
        }
        if (serverLast == 0 || (now.getTime() - serverLast) >= this.delay) {
          List<HistoryEntry> entries =
              ChatPoller.getEntries(ChatPoller.localLastSeen, false, this.paused);
        }
      } catch (Exception e) {
        StaticEntity.printStackTrace(e);
      }

      if (!this.paused
          && (now.getTime() - ChatPoller.lastSentMessage.getTime())
              > ChatPoller.AWAY_MODE_THRESHOLD) {
        this.pause(false);
      }

      pauser.pause(this.delay);
    }
  }

  public static synchronized void addEntry(ChatMessage message) {
    HistoryEntry entry = new HistoryEntry(message, ++ChatPoller.localLastSent);

    synchronized (ChatPoller.chatHistoryEntries) {
      ChatPoller.chatHistoryEntries.add(entry);
    }

    ChatManager.processMessages(entry.getChatMessages());
  }

  public static synchronized void addSentEntry(
      final String responseText, final boolean isRelayRequest) {
    SentMessageEntry entry =
        new SentMessageEntry(responseText, ++ChatPoller.localLastSent, isRelayRequest);

    entry.executeAjaxCommand();

    synchronized (ChatPoller.chatHistoryEntries) {
      ChatPoller.chatHistoryEntries.add(entry);
    }
  }

  private static void addValidEntry(
      final List<HistoryEntry> newEntries, final HistoryEntry entry, final boolean isRelayRequest) {
    if (!(entry instanceof SentMessageEntry sentEntry)) {
      newEntries.add(entry);
      return;
    }

    if (!isRelayRequest) {
      return;
    }

    if (sentEntry.isRelayRequest()) {
      return;
    }

    newEntries.add(entry);
    return;
  }

  public static synchronized List<HistoryEntry> getOldEntries(final boolean isRelayRequest) {
    List<HistoryEntry> newEntries = new ArrayList<>();
    final long lastSeen = ChatPoller.localLastSeen;

    synchronized (ChatPoller.chatHistoryEntries) {
      Iterator<HistoryEntry> entryIterator = ChatPoller.chatHistoryEntries.iterator();
      while (entryIterator.hasNext()) {
        HistoryEntry entry = entryIterator.next();

        if (entry.getLocalLastSeen() > lastSeen) {
          ChatPoller.addValidEntry(newEntries, entry, isRelayRequest);

          while (entryIterator.hasNext()) {
            ChatPoller.addValidEntry(newEntries, entryIterator.next(), isRelayRequest);
          }
        }
      }
    }

    ChatPoller.localLastSeen = ChatPoller.localLastSent;

    return newEntries;
  }

  public static synchronized List<HistoryEntry> getEntries(
      final long lastSeen, final boolean isRelayRequest, final boolean paused) {
    List<HistoryEntry> newEntries = ChatPoller.getOldEntries(isRelayRequest);

    if (ChatManager.getCurrentChannel() == null) {
      ChatSender.sendMessage(null, "/listen", true);
    }

    ChatRequest request = new ChatRequest(ChatPoller.serverLastSeen, false, paused);
    request.run();

    HistoryEntry entry = new HistoryEntry(request.responseText, ++ChatPoller.localLastSent);
    ChatPoller.localLastSeen = ChatPoller.localLastSent;
    ChatPoller.setServerLast(entry.getServerLastSeen());
    newEntries.add(entry);

    synchronized (ChatPoller.chatHistoryEntries) {
      ChatPoller.chatHistoryEntries.add(entry);
    }
    ChatManager.processMessages(entry.getChatMessages());

    return newEntries;
  }

  public static final String getRightClickMenu() {
    if (ChatPoller.rightClickMenu.equals("")) {
      GenericRequest request = new GenericRequest("lchat.php");
      RequestThread.postRequest(request);

      int actionIndex = request.responseText.indexOf("actions = {");
      if (actionIndex != -1) {
        ChatPoller.rightClickMenu =
            request.responseText.substring(
                actionIndex, request.responseText.indexOf(";", actionIndex) + 1);
      }
    }

    return ChatPoller.rightClickMenu;
  }

  private static boolean messageAlreadySeen(
      final String recipient, final String content, final long localLastSeen) {
    synchronized (ChatPoller.chatHistoryEntries) {
      for (HistoryEntry entry : ChatPoller.chatHistoryEntries) {
        if (entry instanceof SentMessageEntry && entry.getLocalLastSeen() > localLastSeen) {
          for (ChatMessage message : entry.getChatMessages()) {
            if (recipient.equals(message.getRecipient()) && content.equals(message.getContent())) {
              return true;
            }
          }
        }
      }
    }

    return false;
  }

  public static List<ChatMessage> parseNewChat(final String responseData) {
    List<ChatMessage> messages = new LinkedList<>();
    try {
      ChatPoller.parseNewChat(
          messages, JSON.parseObject(responseData), "", ChatPoller.localLastSeen, true);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return messages;
  }

  public static List<ChatMessage> parseNewChat(
      final List<ChatMessage> messages,
      JSONObject obj,
      final String sent,
      final long localLastSeen,
      final boolean debug)
      throws JSONException {
    JSONArray msgs = obj.getJSONArray("msgs");
    for (int i = 0; i < msgs.size(); i++) {
      JSONObject msg = msgs.getJSONObject(i);

      // If we have already seen this message in the chat GUI, skip it.
      long mid = msg.getLongValue("mid");
      if (!debug && mid != 0 && mid <= ChatPoller.serverLastSeen) {
        continue;
      }

      String type = msg.getString("type");
      boolean pub = type.equals("public");

      String formatString = msg.getString("format");
      int format = formatString == null ? 0 : StringUtilities.parseInt(formatString);

      // From mchat.js:
      //
      // type = "public" format = "0" -> normal public chat message
      // type = "public" format = "1" -> /em public chat message
      // type = "public" format = "2" -> System Message
      // type = "public" format = "3" -> Mod Warning
      // type = "public" format = "4" -> Mod Announcement
      // type = "public" format = "98" -> event
      // type = "public" format = "99" -> Welcome message
      // type = "private" -> private chat message
      // type = "event" -> event
      // type = "system" -> System Message
      //
      // I have never seen the "public" versions of
      // System Message or Event. Those are probably
      // obsolete, now that they have their own types

      JSONObject whoObj = msg.getJSONObject("who");
      String sender = whoObj != null ? whoObj.getString("name") : null;
      String senderId = whoObj != null ? whoObj.getString("id") : null;
      boolean mine = KoLCharacter.getPlayerId().equals(senderId);

      JSONObject forObj = msg.getJSONObject("for");
      String recipient = forObj != null ? forObj.getString("name") : null;

      String content = msg.getString("msg");

      if (type.equals("event")) {
        // {"type":"event","msg":"You are now in away mode, chat will update more slowly until you
        // say something.","notnew":1,"time":1411683685}
        // {"type":"event","msg":"Welcome back!  Away mode disabled.","time":1411683893}
        if (!debug && content != null) {
          if (content.startsWith("You are now in away mode")) {
            ChatPoller.pauseChat(true, true);
          } else if (content.contains("Away mode disabled")) {
            ChatPoller.pauseChat(false, true);
          }

          // TODO: handle other events
        }

        messages.add(new EventMessage(content, "green"));
        continue;
      }

      if (type.equals("system")) {
        messages.add(new SystemMessage(content));
        continue;
      }

      if (sender == null) {
        if (!debug) {
          ChatSender.processResponse(messages, content, sent);
          continue;
        }
      } else if (sender.equals("HMC Radio")) {
        messages.add(new HugglerMessage(content));
        continue;
      }

      if (recipient == null) {
        if (pub) {
          String channel = "/" + msg.getString("channel");
          if (sender.equals("Mod Announcement") || sender.equals("Mod Warning")) {
            messages.add(new ModeratorMessage(channel, sender, senderId, content));
            continue;
          }
          recipient = channel;
        } else {
          recipient = KoLCharacter.getUserName().replaceAll(" ", "_");
        }
      }

      // Apparently isAction corresponds to /em commands.
      boolean isAction = format == 1;

      if (isAction) {
        // username ends with "</b></font></a> "; remove trailing </i>
        content = content.substring(content.indexOf("</a>") + 5, content.length() - 4);
      }

      if (pub && mine && ChatPoller.messageAlreadySeen(recipient, content, localLastSeen)) {
        continue;
      }

      messages.add(new ChatMessage(sender, recipient, content, isAction));
    }

    return messages;
  }

  public static void handleNewChat(
      final String responseData, final String sent, final long localLastSeen) {
    try {
      List<ChatMessage> messages = new LinkedList<>();
      JSONObject obj = JSON.parseObject(responseData);

      // note: output is where /who, /listen, + various game commands
      // (/use etc) output goes. May exist.
      String output = obj.getString("output");
      if (output != null) {
        // TODO: strip channelname so /cli works again.
        ChatSender.processResponse(messages, output, sent);
      }

      ChatPoller.parseNewChat(messages, obj, sent, localLastSeen, false);

      if (obj.containsKey("last")) {
        // Remember the last timestamp the server gave us
        ChatPoller.setServerLast(obj.getLong("last"));
      }

      if (obj.containsKey("delay")) {
        // Set the chat GUI's delay to whatever KoL says it should be.
        ChatPoller.setServerDelay(obj.getIntValue("delay"));
      }

      ChatManager.processMessages(messages);
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }
}
