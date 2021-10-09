package net.sourceforge.kolmafia;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class KoLMailMessage implements Comparable<KoLMailMessage> {
  private static final SimpleDateFormat TIMESTAMP_FORMAT =
      new SimpleDateFormat("EEEE, MMMM dd, yyyy, hh:mmaa", Locale.US);

  private final String messageId;
  private final String senderId;
  private final String senderName;
  private String messageDate;
  private Date timestamp;
  private final String messageHTML;

  private String completeHTML;

  public KoLMailMessage(final String message) {
    // Blank lines are not displayed correctly
    this.completeHTML = StringUtilities.globalStringReplace(message, "<br><br>", "<br>&nbsp;<br>");

    // Extract message ID
    this.messageId = message.substring(message.indexOf("name=") + 6, message.indexOf("\">"));

    // Tokenize message
    StringTokenizer messageParser = new StringTokenizer(message, "<>");
    String lastToken = messageParser.nextToken();

    // Trim off message ID
    this.completeHTML = this.completeHTML.substring(this.completeHTML.indexOf(">") + 1);

    // Messages from pseudo-characters do not have a [reply] link
    int replyLink = this.completeHTML.indexOf("reply</a>]");
    if (replyLink > 0) {
      // Real sender. Trim message, parse and register sender
      while (!lastToken.startsWith("a ")) {
        lastToken = messageParser.nextToken();
      }

      this.senderId = lastToken.substring(lastToken.indexOf("who=") + 4, lastToken.length() - 1);
      this.senderName = messageParser.nextToken();

      ContactManager.registerPlayerId(this.senderName, this.senderId);
    } else {
      // Pseudo player.
      this.senderId = "";

      while (!lastToken.startsWith("/b")) {
        lastToken = messageParser.nextToken();
      }

      String name = messageParser.nextToken();
      int sp = name.indexOf("&nbsp;");
      if (sp > 0) {
        name = name.substring(0, sp);
      }
      this.senderName = name.trim();
    }

    while (!messageParser.nextToken().startsWith("Date")) {}
    messageParser.nextToken();

    this.messageDate = messageParser.nextToken().trim();
    this.messageHTML =
        message.substring(message.indexOf(this.messageDate) + this.messageDate.length() + 4);

    try {
      // This attempts to parse the date from
      // the given string; note it may throw
      // an exception (but probably not)

      this.timestamp = KoLMailMessage.TIMESTAMP_FORMAT.parse(this.messageDate);
    } catch (Exception e) {
      // This should not happen.  Therefore, print
      // a stack trace for debug purposes.

      StaticEntity.printStackTrace(e, "Could not parse date \"" + this.messageDate + "\"");

      // Initialize the date to the current time,
      // since that's about as close as it gets

      this.timestamp = new Date();
      this.messageDate = KoLMailMessage.TIMESTAMP_FORMAT.format(this.timestamp);
    }
  }

  @Override
  public String toString() {
    return this.senderName + " @ " + this.messageDate;
  }

  public int compareTo(final KoLMailMessage o) {
    return !(o instanceof KoLMailMessage) ? -1 : this.messageId.compareTo(o.messageId);
  }

  @Override
  public boolean equals(final Object o) {
    return !(o instanceof KoLMailMessage)
        ? false
        : this.messageId.equals(((KoLMailMessage) o).messageId);
  }

  @Override
  public int hashCode() {
    return this.messageId != null ? this.messageId.hashCode() : 0;
  }

  public String getMessageId() {
    return this.messageId;
  }

  public Date getTimestamp() {
    return this.timestamp;
  }

  public String getCompleteHTML() {
    return this.completeHTML;
  }

  public String getMessageHTML() {
    return this.messageHTML;
  }

  public String getSenderName() {
    return this.senderName;
  }

  public String getSenderId() {
    return this.senderId;
  }
}
