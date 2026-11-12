package net.sourceforge.kolmafia.session;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.preferences.Preferences;

public class CommandHistory {
  private static final String PREFERENCE = "commandBufferGCLI";
  private static final String TABBED_CHAT_PREFERENCE = "commandBufferTabbedChat";
  private static final String LIMIT = "commandBufferLimit";
  private static final String DELIMITER = "#";

  static {
    mergeTabbedChatHistory();
  }

  private CommandHistory() {}

  /**
   * Returns the command history that the gCLI and the relay browser CLI share.
   *
   * @return the commands as an immutable list
   */
  public static List<String> get() {
    return split(Preferences.getString(PREFERENCE));
  }

  /**
   * Adds a trimmed command to the history, unless it is blank. Removes earlier copies and drops the
   * oldest commands past {@code commandBufferLimit}.
   *
   * @param command the command to add
   */
  public static synchronized void add(final String command) {
    var trimmed = command.trim();
    if (trimmed.isEmpty()) {
      return;
    }

    save(Stream.concat(Stream.of(trimmed), get().stream()));
  }

  private static void mergeTabbedChatHistory() {
    if (!Preferences.propertyExists(TABBED_CHAT_PREFERENCE, true)) {
      return;
    }

    var tabbedChat = split(Preferences.getString(TABBED_CHAT_PREFERENCE, true));
    save(Stream.concat(get().stream(), tabbedChat.stream()));
    Preferences.removeProperty(TABBED_CHAT_PREFERENCE, true);
  }

  private static void save(final Stream<String> commands) {
    var history =
        commands
            .distinct()
            .limit(Math.max(1, Preferences.getInteger(LIMIT)))
            .map(CommandHistory::encode)
            .toList();
    Preferences.setString(PREFERENCE, String.join(DELIMITER, history));
  }

  private static List<String> split(final String value) {
    return Arrays.stream(value.split(DELIMITER))
        .filter(Predicate.not(String::isBlank))
        .map(CommandHistory::decode)
        .toList();
  }

  private static String encode(final String command) {
    return command.replace("%", "%25").replace(DELIMITER, "%23");
  }

  private static String decode(final String entry) {
    return entry.replace("%23", DELIMITER).replace("%25", "%");
  }
}
