package net.sourceforge.kolmafia.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.request.ChatRequest;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ChatSender {
  private static final Pattern PRIVATE_MESSAGE_PATTERN =
      Pattern.compile("^/(?:msg|whisper|w|tell)\\s+(\\S+)\\s+", Pattern.CASE_INSENSITIVE);

  private static boolean scriptedMessagesEnabled = true;

  private static final ArrayList<String> CHANNEL_COMMANDS = new ArrayList<>();

  static {
    ChatSender.CHANNEL_COMMANDS.add("/em");
    ChatSender.CHANNEL_COMMANDS.add("/me");
    ChatSender.CHANNEL_COMMANDS.add("/ann");
  }

  private ChatSender() {}

  public static final void executeMacro(String macro) {
    if (!ChatSender.scriptedMessagesEnabled || !ChatManager.chatLiterate()) {
      return;
    }

    ChatRequest request = new ChatRequest(macro, false);

    List<ChatMessage> accumulatedMessages =
        new LinkedList<>(ChatSender.sendRequest(request, false));

    ChatPoller.addSentEntry(request.responseText, false);

    for (ChatMessage message : accumulatedMessages) {
      String recipient = message.getRecipient();

      ChatSender.scriptedMessagesEnabled =
          recipient == null
              || recipient.equals("")
              || recipient.equals("/clan")
              || recipient.equals("/hobopolis")
              || recipient.equals("/slimetube")
              || recipient.equals("/dread")
              || recipient.equals("/hauntedhouse");
    }
  }

  public static final void sendMessage(String contact, String message, boolean channelRestricted) {
    if (!ChatManager.chatLiterate()) {
      return;
    }

    List<String> grafs = getGrafs(contact, message);

    if (grafs == null) {
      return;
    }

    List<ChatMessage> accumulatedMessages = new LinkedList<>();

    for (String graf : grafs) {
      String responseText =
          ChatSender.sendMessage(accumulatedMessages, graf, false, channelRestricted, false);

      ChatPoller.addSentEntry(responseText, false);
    }
  }

  public static final String sendMessage(
      List<ChatMessage> accumulatedMessages,
      String graf,
      boolean isRelayRequest,
      boolean channelRestricted,
      boolean tabbedChat) {
    if (graf == null) {
      return "";
    }

    if (!ChatManager.chatLiterate()) {
      return "";
    }

    if (channelRestricted && !ChatSender.scriptedMessagesEnabled) {
      return "";
    }

    if (ChatSender.executeCommand(graf)) {
      return "";
    }

    if (graf.startsWith("/examine")) {
      String item = graf.substring(graf.indexOf(" ")).trim();

      AdventureResult result = ItemFinder.getFirstMatchingItem(item, false, Match.ANY);

      if (result != null) {
        ShowDescriptionList.showGameDescription(result);
      } else {
        EventMessage message =
            new EventMessage("Unable to find a unique match for " + item, "green");
        ChatManager.broadcastEvent(message);
      }

      return "";
    }

    ChatPoller.sentMessage(tabbedChat);
    ChatRequest request = new ChatRequest(graf, tabbedChat);
    List<ChatMessage> messages = ChatSender.sendRequest(request, tabbedChat);
    accumulatedMessages.addAll(messages);

    if (channelRestricted) {
      Iterator<ChatMessage> messageIterator = accumulatedMessages.iterator();

      while (messageIterator.hasNext() && ChatSender.scriptedMessagesEnabled) {
        ChatMessage message = messageIterator.next();

        String recipient = message.getRecipient();

        ChatSender.scriptedMessagesEnabled =
            recipient == null
                || recipient.equals("/clan")
                || recipient.equals("/hobopolis")
                || recipient.equals("/slimetube")
                || recipient.equals("/dread")
                || recipient.equals("/hauntedhouse");
      }
    }

    return request.responseText == null ? "" : request.responseText;
  }

  public static final List<ChatMessage> sendRequest(ChatRequest request) {
    return ChatSender.sendRequest(request, false);
  }

  public static final List<ChatMessage> sendRequest(ChatRequest request, boolean tabbedChat) {
    if (!ChatManager.chatLiterate()) {
      return Collections.emptyList();
    }

    RequestThread.postRequest(request);

    if (request.responseText == null) {
      return Collections.emptyList();
    }

    List<ChatMessage> newMessages = new LinkedList<>();

    if (!tabbedChat) {
      String graf = request.getGraf();
      ChatSender.processResponse(newMessages, request.responseText, graf);
      ChatManager.processMessages(newMessages);
    }

    return newMessages;
  }

  public static final void processResponse(
      List<ChatMessage> newMessages, String responseText, String graf) {
    // Protect against server lagging out
    if (responseText == null || responseText.equals("")) {
      return;
    }

    if (graf.equals("/listen")) {
      ChatParser.parseChannelList(newMessages, responseText);
    } else if (graf.startsWith("/l ") || graf.startsWith("/listen ")) {
      ChatParser.parseListen(newMessages, responseText);
    } else if (graf.startsWith("/c ") || graf.startsWith("/channel ")) {
      ChatParser.parseChannel(newMessages, responseText);
    } else if (graf.startsWith("/s ") || graf.startsWith("/switch ")) {
      ChatParser.parseSwitch(newMessages, responseText);
    } else if (graf.startsWith("/who ")
        || graf.equals("/f")
        || graf.equals("/friends")
        || graf.equals("/romans")
        || graf.equals("/clannies")
        || graf.equals("/countrymen")) {
      ChatParser.parseContacts(newMessages, responseText, graf.equals("/clannies"));
    } else {
      ChatParser.parseLines(newMessages, responseText);
    }
  }

  private static List<String> getGrafs(String contact, String message) {
    List<String> grafs = new LinkedList<>();

    if (message.startsWith("/do ") || message.startsWith("/run ") || message.startsWith("/cli ")) {
      grafs.add(message);

      return grafs;
    }

    Matcher privateMessageMatcher = ChatSender.PRIVATE_MESSAGE_PATTERN.matcher(message);

    if (privateMessageMatcher.find()) {
      contact = privateMessageMatcher.group(1).trim();
      message = message.substring(privateMessageMatcher.end()).trim();
    }

    // contact is null only for very short internally generated messages.
    if (message.length() <= 256 || contact == null) {
      String graf = ChatSender.getGraf(contact, message);

      if (graf != null) {
        grafs.add(graf);
      }

      return grafs;
    }

    // If the message is too long for one message, then
    // divide it into its component pieces.

    if (message.startsWith("/")) {
      // This is one or more chained game commands. We need
      // to split on && boundaries.

      String[] commands = message.split(" +&& +");
      StringBuilder buffer = new StringBuilder();

      for (String command : commands) {
        int current = buffer.length();
        int needed = command.length();

        // If you have a single command that is longer
        // than 256 characters, that's almost certainly
        // not going to work, but it gets its own graf.
        if (current > 0 && (needed > 256 || (current + needed) > (256 - 4))) {
          String graf = ChatSender.getGraf(contact, buffer.toString());
          if (graf != null) {
            grafs.add(graf);
          }
          buffer.setLength(0);
          current = 0;
        }

        if (current > 0) {
          buffer.append(" && ");
        }

        buffer.append(command);
      }

      if (buffer.length() > 0) {
        String graf = ChatSender.getGraf(contact, buffer.toString());
        if (graf != null) {
          grafs.add(graf);
        }
      }

      return grafs;
    }

    String command = "";
    String splitter = " ";
    String prefix = "... ";
    String suffix = " ...";

    if (message.startsWith("/")) {
      int spaceIndex = message.indexOf(" ");

      if (spaceIndex != -1) {
        command = message.substring(0, spaceIndex).trim();
        message = message.substring(spaceIndex).trim();
      } else {
        command = message.trim();
        message = "";
      }
    }

    String graf;

    int maxPiece = 255 - command.length() - suffix.length();

    while (message.length() > maxPiece) {
      int splitPos = message.lastIndexOf(splitter, maxPiece);
      if (splitPos <= prefix.length()) {
        splitPos = maxPiece;
      }

      graf = ChatSender.getGraf(contact, command + " " + message.substring(0, splitPos) + suffix);

      if (graf != null) {
        grafs.add(graf);
      }

      message = prefix + message.substring(splitPos + splitter.length());
    }

    graf = ChatSender.getGraf(contact, command + " " + message);

    if (graf != null) {
      grafs.add(graf);
    }

    return grafs;
  }

  private static String getGraf(String contact, String message) {
    String contactId = "[none]";

    if (contact != null) {
      contactId = ContactManager.getPlayerId(contact);
      contactId = StringUtilities.globalStringReplace(contactId, " ", "_").trim();
    }

    String graf = message == null ? "" : message.trim();

    if (graf.startsWith("/exit")) {
      // Exiting chat should dispose.  KoLmafia should send the
      // message to be server-friendly.

      if (ChatManager.isRunning()) {
        ChatManager.dispose();
      }

      return null;
    }

    if (contactId.startsWith("[")) {
      // This is a message coming from an aggregated window, so
      // leave it as is.
    } else if (!contact.startsWith("/") && !graf.startsWith("/")) {
      // Implied requests for a private message should be wrapped
      // in a /msg block.

      graf = "/msg " + contactId + " " + graf;
    } else if (!graf.startsWith("/")) {
      // All non-command messages are directed to a channel.  Append the
      // name of the channel to the beginning of the message so you
      // ensure the message gets there.

      graf = contact + " " + graf;
    } else {
      int spaceIndex = graf.indexOf(" ");
      String baseCommand =
          spaceIndex == -1 ? graf.toLowerCase() : graf.substring(0, spaceIndex).toLowerCase();

      if (graf.equals("/c") || graf.equals("/channel")) {
        graf = "/channel " + contact.substring(1);
      } else if (graf.equals("/l") || graf.equals("/listen")) {
        graf = "/listen " + contact.substring(1);
      } else if (graf.equals("/s") || graf.equals("/switch")) {
        graf = "/switch " + contact.substring(1);
      } else if (graf.equals("/w") || graf.equals("/who")) {
        // Attempts to view the /who list use the name of the channel
        // when the user doesn't specify the channel.

        graf = "/who " + contact.substring(1);
      } else if (graf.equals("/whois") || graf.equals("/friend") || graf.equals("/baleet")) {
        graf = graf + " " + contact;
      } else if (ChatSender.CHANNEL_COMMANDS.contains(baseCommand)) {
        if (contact.startsWith("/")) {
          // Direct the message to a channel by appending the name
          // of the channel to the beginning of the message.
          graf = contact + " " + graf;
        } else {
          // And if it's a private message
          graf = "/msg " + contact + " " + graf;
        }
      }
    }

    if (graf.startsWith("/l ") || graf.startsWith("/listen ")) {
      String currentChannel = ChatManager.getCurrentChannel();

      if (currentChannel != null && graf.endsWith(currentChannel)) {
        return null;
      }
    }

    return graf;
  }

  private static boolean executeCommand(String graf) {
    if (graf == null) {
      return false;
    }

    graf = graf.trim();

    int spaceIndex = graf.indexOf(" ");

    // Strip off channel name
    if (spaceIndex != -1 && ChatManager.activeChannels.contains(graf.substring(0, spaceIndex))) {
      graf = graf.substring(spaceIndex).trim();
      spaceIndex = graf.indexOf(" ");
    }

    if (graf.equalsIgnoreCase("/trivia")) {
      ChatManager.startTriviaGame();
      return true;
    }

    if (graf.equalsIgnoreCase("/endtrivia") || graf.equalsIgnoreCase("/stoptrivia")) {
      ChatManager.stopTriviaGame();
      return true;
    }

    if (spaceIndex == -1) {
      return false;
    }

    if (!graf.startsWith("/do ") && !graf.startsWith("/run ") && !graf.startsWith("/cli ")) {
      return false;
    }

    String command = graf.substring(spaceIndex).trim();
    CommandDisplayFrame.executeCommand(command);
    return true;
  }
}
