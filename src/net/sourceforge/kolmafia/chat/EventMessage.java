package net.sourceforge.kolmafia.chat;

import java.util.Date;

public class EventMessage extends ChatMessage {
  private final String color;
  private boolean hidden;

  public EventMessage(String content, String color) {
    this(content, color, new Date());
  }

  public EventMessage(String content, String color, Date date) {
    super(null, null, content, false, date, null);

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
