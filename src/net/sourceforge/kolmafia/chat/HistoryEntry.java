package net.sourceforge.kolmafia.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class HistoryEntry {
  private static final Pattern LASTSEEN_PATTERN = Pattern.compile("<!--lastseen:(\\d+)-->");

  private final List<ChatMessage> chatMessages;

  private final long localLastSeen;
  private final long serverLastSeen;

  private String content;

  public HistoryEntry(final ChatMessage message, final long localLastSeen) {
    this.chatMessages = new ArrayList<ChatMessage>();
    this.chatMessages.add(message);

    this.localLastSeen = localLastSeen;
    this.serverLastSeen = 0;

    this.content = ChatFormatter.formatChatMessage(message);
  }

  public HistoryEntry(final String responseText, final long localLastSeen) {
    this.chatMessages = new ArrayList<ChatMessage>();

    this.localLastSeen = localLastSeen;
    Matcher matcher =
        responseText != null ? HistoryEntry.LASTSEEN_PATTERN.matcher(responseText) : null;

    if (matcher != null && matcher.find()) {
      this.serverLastSeen = StringUtilities.parseLong(matcher.group(1));
      this.content = matcher.replaceFirst("");
    } else {
      this.serverLastSeen = 0;
      this.content = responseText;
    }

    ChatParser.parseLines(this.chatMessages, this.content);
  }

  public String getContent() {
    return this.content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public long getLocalLastSeen() {
    return this.localLastSeen;
  }

  public long getServerLastSeen() {
    return this.serverLastSeen;
  }

  public List<ChatMessage> getChatMessages() {
    return this.chatMessages;
  }
}
