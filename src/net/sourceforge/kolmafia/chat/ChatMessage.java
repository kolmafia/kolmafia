package net.sourceforge.kolmafia.chat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import net.sourceforge.kolmafia.session.ContactManager;
import org.json.JSONException;
import org.json.JSONObject;

public class ChatMessage {
  private String sender;
  private String recipient;
  private String content;
  private boolean isAction;
  private final Date date;
  private final String timestamp;

  private static final SimpleDateFormat MESSAGE_TIMESTAMP =
      new SimpleDateFormat("[HH:mm]", Locale.US);

  public ChatMessage() {
    this.date = new Date();
    this.timestamp = ChatMessage.MESSAGE_TIMESTAMP.format(date);
  }

  public ChatMessage(String sender, String recipient, String content, boolean isAction) {
    this.sender = sender;
    this.recipient = recipient;
    this.content = content.trim();
    this.isAction = isAction;

    this.date = new Date();
    this.timestamp = ChatMessage.MESSAGE_TIMESTAMP.format(date);
  }

  public String getSender() {
    return this.sender;
  }

  public void setSender(String sender) {
    this.sender = sender;
  }

  public String getRecipient() {
    return this.recipient;
  }

  public void setRecipient(String recipient) {
    this.recipient = recipient;
  }

  public String getContent() {
    return this.content;
  }

  public void setContent(String content) {
    this.content = content.trim();
  }

  public boolean isAction() {
    return this.isAction;
  }

  public Date getDate() {
    return this.date;
  }

  public String getTimestamp() {
    return this.timestamp;
  }

  public final JSONObject toJSON() {
    // {"type":"private","who":{"id":"1764512","name":"Tebryn","color":"black"},"for":{"id":"121572","name":"Veracity","color":"black"},"msg":"test","time":1411334015,"format":0}
    //
    // {"type":"public","mid":"1386587663","who":{"name":"Tebryn","id":"1764512","color":"black"},"channel":"talkie","channelcolor":"green","msg":"test 1 2 3","time":"1411335349","format":"0"}

    boolean pmsg =
        (this.sender != null && !this.sender.equals(""))
            && (this.recipient != null
                && !this.recipient.equals("")
                && !this.recipient.startsWith("/"));

    if (pmsg) {
      try {
        JSONObject object = new JSONObject();
        JSONObject whoTag = new JSONObject();
        JSONObject forTag = new JSONObject();

        object.put("type", "private");
        whoTag.put("id", ContactManager.getPlayerId(this.sender));
        whoTag.put("name", this.sender);
        whoTag.put("color", "black");
        object.put("who", whoTag);
        forTag.put("id", ContactManager.getPlayerId(this.recipient));
        forTag.put("name", this.recipient);
        forTag.put("color", "black");
        object.put("for", forTag);
        object.put("msg", this.content);
        object.put("time", this.date.getTime());
        return object;
      } catch (JSONException e) {
        return null;
      }
    }

    return null;
  }
}
