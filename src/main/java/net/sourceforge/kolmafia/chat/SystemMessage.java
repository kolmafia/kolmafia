package net.sourceforge.kolmafia.chat;

public class SystemMessage extends ModeratorMessage {
  public SystemMessage(String content) {
    super("", "System Message", "-1", content);
  }
}
