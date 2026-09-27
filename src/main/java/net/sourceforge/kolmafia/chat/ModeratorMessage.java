package net.sourceforge.kolmafia.chat;

public class ModeratorMessage extends ChatMessage {
  private final String playerId;

  public ModeratorMessage(String channel, String messageType, String playerId, String content) {
    this.setRecipient(channel);
    this.setSender(messageType);
    this.playerId = playerId;
    this.setContent(content);
  }

  public String getModeratorId() {
    return playerId;
  }
}
