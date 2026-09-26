package net.sourceforge.kolmafia.chat;

public class EventMessage extends ChatMessage {
  private final String color;
  private boolean hidden;

  public EventMessage(String content, String color) {
    this.setContent(content);

    this.color = color;
    this.hidden = false;
  }

  public String getColor() {
    return this.color;
  }

  public boolean isHidden() {
    return this.hidden;
  }

  public void setHidden(boolean hidden) {
    this.hidden = hidden;
  }
}
