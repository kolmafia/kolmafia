package net.sourceforge.kolmafia.chat;

import java.util.Date;

public class ModeratorMessage extends ChatMessage {
  private final String playerId;

  public ModeratorMessage(String channel, String messageType, String playerId, String content) {
    this(channel, messageType, playerId, content, new Date());
  }

  public ModeratorMessage(
      String channel, String messageType, String playerId, String content, Date date) {
    super(messageType, channel, content, false, date, null);

    this.playerId = playerId;
  }

  public String getModeratorId() {
    return playerId;
  }
}
