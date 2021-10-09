package net.sourceforge.kolmafia.session;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.chat.ChatSender;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.ContactListRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.swingui.ContactListFrame;
import net.sourceforge.kolmafia.utilities.HTMLListEntry;

public class ContactManager {
  private static final HashMap<String, String> seenPlayerIds = new HashMap<>();
  private static final HashMap<String, String> seenPlayerNames = new HashMap<>();

  private static final SortedListModel<String> mailContacts = new SortedListModel<>();
  private static final SortedListModel<HTMLListEntry> chatContacts = new SortedListModel<>();

  private static ContactListFrame contactsFrame = null;

  public static final void updateMailContacts() {
    if (ContactManager.mailContacts.isEmpty()
        && !CharPaneRequest.inValhalla()
        && !FightRequest.initializingAfterFight()
        && !ChoiceManager.initializingAfterChoice()) {
      RequestThread.postRequest(new ContactListRequest());
    }
  }

  public static final boolean isMailContact(final String playerName) {
    return ContactManager.mailContacts.contains(playerName);
  }

  public static final LockableListModel<String> getMailContacts() {
    return ContactManager.mailContacts;
  }

  public static final void clearMailContacts() {
    ContactManager.mailContacts.clear();
  }

  /**
   * Replaces the current contact list with the given contact list. This is used after every call to
   * /friends or /who.
   */
  public static final void updateContactList(
      final String title, final Map<String, Boolean> contacts) {
    if (!ChatManager.isRunning()) {
      return;
    }

    ContactManager.chatContacts.clear();

    for (Entry<String, Boolean> entry : contacts.entrySet()) {
      String playerName = entry.getKey().toLowerCase();
      String color = entry.getValue() == Boolean.TRUE ? "black" : "gray";

      ContactManager.chatContacts.add(new HTMLListEntry(playerName, color));
    }

    if (Preferences.getBoolean("useContactsFrame")) {
      if (ContactManager.contactsFrame == null) {
        ContactManager.contactsFrame = new ContactListFrame(ContactManager.chatContacts);
      }

      ContactManager.contactsFrame.setTitle(title);
      ContactManager.contactsFrame.setVisible(true);
    }
  }

  public static final void addMailContact(String playerName, final String playerId) {
    ContactManager.registerPlayerId(playerName, playerId);

    playerName = playerName.toLowerCase().replaceAll("[^0-9A-Za-z_ ]", "");

    if (!ContactManager.mailContacts.contains(playerName)) {
      ContactManager.mailContacts.add(playerName.toLowerCase());
    }
  }

  /**
   * Registers the given player name and player Id with KoLmafia's player name tracker.
   *
   * @param playerName The name of the player
   * @param playerId The player Id associated with this player
   */
  public static final void registerPlayerId(String playerName, final String playerId) {
    if (playerId.startsWith("-")) {
      return;
    }

    String lowercase = playerName.toLowerCase().trim();

    if (ContactManager.seenPlayerIds.containsKey(lowercase)) {
      return;
    }

    ContactManager.seenPlayerIds.put(lowercase, playerId);
    ContactManager.seenPlayerNames.put(playerId, playerName);
  }

  /**
   * Returns the string form of the player Id associated with the given player name.
   *
   * @param playerName The name of the player
   * @return The player's Id if the player has been seen, or the player's name with spaces replaced
   *     with underscores and other elements encoded if the player's Id has not been seen.
   */
  public static final String getPlayerId(final String playerName) {
    return ContactManager.getPlayerId(playerName, false);
  }

  public static final String getPlayerId(final String playerName, boolean retrieveId) {
    if (playerName == null) {
      return null;
    }

    String playerId = ContactManager.seenPlayerIds.get(playerName.toLowerCase());

    if (playerId != null) {
      return playerId;
    }

    if (!retrieveId) {
      return playerName;
    }

    ChatSender.executeMacro("/whois " + playerName);

    return ContactManager.getPlayerId(playerName, false);
  }

  /**
   * Returns the string form of the player Id associated with the given player name.
   *
   * @param playerId The Id of the player
   * @return The player's name if it has been seen, or null if it has not yet appeared in the chat
   *     (not likely, but possible).
   */
  public static final String getPlayerName(final String playerId) {
    return ContactManager.getPlayerName(playerId, false);
  }

  public static final String getPlayerName(final String playerId, boolean retrieveName) {
    if (playerId == null) {
      return null;
    }

    String playerName = ContactManager.seenPlayerNames.get(playerId);

    if (playerName != null) {
      return playerName;
    }

    if (!retrieveName) {
      return playerId;
    }

    ChatSender.executeMacro("/whois " + playerId);

    return ContactManager.getPlayerName(playerId, false);
  }

  public static final String[] extractTargets(final String targetList) {
    // If there are no targets in the list, then
    // return absolutely nothing.

    if (targetList == null || targetList.trim().length() == 0) {
      return new String[0];
    }

    // Otherwise, split the list of targets, and
    // determine who all the unique targets are.

    String[] targets = targetList.trim().split("\\s*,\\s*");
    for (int i = 0; i < targets.length; ++i) {
      targets[i] = getPlayerId(targets[i]) == null ? targets[i] : getPlayerId(targets[i]);
    }

    // Sort the list in order to increase the
    // speed of duplicate detection.

    Arrays.sort(targets);

    // Determine who all the duplicates are.

    int uniqueListSize = targets.length;
    for (int i = 1; i < targets.length; ++i) {
      if (targets[i].equals(targets[i - 1])) {
        targets[i - 1] = null;
        --uniqueListSize;
      }
    }

    // Now, create the list of unique targets;
    // if the list has the same size as the original,
    // you can skip this step.

    if (uniqueListSize != targets.length) {
      int addedCount = 0;
      String[] uniqueList = new String[uniqueListSize];
      for (int i = 0; i < targets.length; ++i) {
        if (targets[i] != null) {
          uniqueList[addedCount++] = targets[i];
        }
      }

      targets = uniqueList;
    }

    // Convert all the user Ids back to the
    // original player names so that the results
    // are easy to understand for the user.

    for (int i = 0; i < targets.length; ++i) {
      targets[i] = getPlayerName(targets[i]) == null ? targets[i] : getPlayerName(targets[i]);
    }

    // Sort the list one more time, this time
    // by player name.

    Arrays.sort(targets);

    // Parsing complete. Return the list of
    // unique targets.

    return targets;
  }
}
