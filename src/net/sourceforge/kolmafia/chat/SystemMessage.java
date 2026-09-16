package net.sourceforge.kolmafia.chat;

import java.util.Date;

public class SystemMessage extends ModeratorMessage {
  public SystemMessage(String content) {
    this(content, new Date());
  }

  public SystemMessage(String content, Date date) {
    super("", "System Message", "-1", content, date);
  }
}
