package net.sourceforge.kolmafia.chat;

public class EnableMessage extends ChatMessage {
  private final boolean isTalkChannel;

  public EnableMessage(String channel, boolean isTalkChannel) {
    this.setSender(channel);

    this.isTalkChannel = isTalkChannel;
  }

  public boolean isTalkChannel() {
    return this.isTalkChannel;
  }
}
