package net.sourceforge.kolmafia.session;

import java.util.Map;
import java.util.TreeMap;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.KoLMailMessage;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.MailboxRequest;

public abstract class MailManager {
  public static final Map<String, SortedListModel<KoLMailMessage>> mailboxes = new TreeMap<>();

  static {
    MailManager.mailboxes.put("Inbox", new SortedListModel<>());
    MailManager.mailboxes.put("PvP", new SortedListModel<>());
    MailManager.mailboxes.put("Pen Pal", new SortedListModel<>());
    MailManager.mailboxes.put("Outbox", new SortedListModel<>());
    MailManager.mailboxes.put("Saved", new SortedListModel<>());
  }

  public static final void clearMailboxes() {
    MailManager.getMessages("Inbox").clear();
    MailManager.getMessages("PvP").clear();
    MailManager.getMessages("Pen Pal").clear();
    MailManager.getMessages("Outbox").clear();
    MailManager.getMessages("Saved").clear();
  }

  public static final boolean hasNewMessages() {
    String oldMessageId = Preferences.getString("lastMessageId");

    SortedListModel<KoLMailMessage> inbox = MailManager.getMessages("Inbox");
    if (inbox.isEmpty()) {
      return false;
    }

    KoLMailMessage latest = inbox.get(0);
    String newMessageId = latest.getMessageId();

    Preferences.setString("lastMessageId", newMessageId);
    return !oldMessageId.equals(newMessageId);
  }

  /** Returns a list containing the messages within the specified mailbox. */
  public static final SortedListModel<KoLMailMessage> getMessages(final String mailbox) {
    return MailManager.mailboxes.get(mailbox);
  }

  /**
   * Adds the given message to the given mailbox. This should be called whenever a new message is
   * found, and should only be called again if the message indicates that the message was
   * successfully added (it was a new message).
   *
   * @param boxname The name of the mailbox being updated
   * @param message The message to add to the given mailbox
   */
  public static KoLMailMessage addMessage(final String boxname, final String message) {
    SortedListModel<KoLMailMessage> mailbox = MailManager.mailboxes.get(boxname);

    KoLMailMessage toadd = new KoLMailMessage(message);

    if (mailbox.contains(toadd)) {
      return null;
    }

    mailbox.add(toadd);
    return toadd;
  }

  public static final void deleteMessage(final String boxname, final KoLMailMessage message) {
    RequestThread.postRequest(new MailboxRequest(boxname, message, "delete"));

    SortedListModel<KoLMailMessage> mailbox = MailManager.mailboxes.get(boxname);
    int messageIndex = mailbox.indexOf(message);
    if (messageIndex != -1) {
      mailbox.remove(messageIndex);
    }

    Preferences.setInteger("lastMessageCount", MailManager.getMessages("Inbox").size());
  }

  public static final void deleteMessages(final String boxname, final Object[] messages) {
    if (messages.length == 0) {
      return;
    }

    RequestThread.postRequest(new MailboxRequest(boxname, messages, "delete"));

    int messageIndex;
    SortedListModel<KoLMailMessage> mailbox = MailManager.mailboxes.get(boxname);
    for (int i = 0; i < messages.length; ++i) {
      messageIndex = mailbox.indexOf(messages[i]);
      if (messageIndex != -1) {
        mailbox.remove(messageIndex);
      }
    }

    Preferences.setInteger("lastMessageCount", MailManager.getMessages("Inbox").size());
  }

  public static final void saveMessage(final String boxname, final KoLMailMessage message) {
    RequestThread.postRequest(new MailboxRequest(boxname, message, "save"));

    SortedListModel<KoLMailMessage> mailbox = MailManager.mailboxes.get(boxname);
    int messageIndex = mailbox.indexOf(message);
    if (messageIndex != -1) {
      mailbox.remove(messageIndex);
    }

    Preferences.setInteger("lastMessageCount", MailManager.getMessages("Inbox").size());
  }

  public static final void saveMessages(final String boxname, final Object[] messages) {
    if (messages.length == 0) {
      return;
    }

    RequestThread.postRequest(new MailboxRequest(boxname, messages, "save"));

    int messageIndex;
    SortedListModel<KoLMailMessage> mailbox = MailManager.mailboxes.get(boxname);
    for (int i = 0; i < messages.length; ++i) {
      messageIndex = mailbox.indexOf(messages[i]);
      if (messageIndex != -1) {
        mailbox.remove(messageIndex);
      }
    }

    Preferences.setInteger("lastMessageCount", MailManager.getMessages("Inbox").size());
  }
}
