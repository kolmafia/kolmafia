package net.sourceforge.kolmafia.session;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.chat.EventMessage;
import net.sourceforge.kolmafia.request.LoginRequest;
import net.sourceforge.kolmafia.swingui.SystemTrayFrame;

public class EventManager {
  private static final LockableListModel<String> eventTexts = new LockableListModel<>();
  private static final LockableListModel<String> eventHyperTexts = new LockableListModel<>();

  public static final List<Pattern> EVENT_PATTERNS =
      List.of(
          Pattern.compile(
              "<table[^>]*><tr><td[^>]*bgcolor=orange><b>New Events:</b></td></tr><tr><td style=\"padding: 5px; border: 1px solid orange;\"><center><table><tr><td>(.*?)</td></tr></table></center></td></tr><tr><td height=4></td></tr></table>"),
          Pattern.compile(
              "<table[^>]*><tr><td[^>]*bgcolor=orange><b>New Events:</b></td></tr><tr><td style=\"padding: 5px; border: 1px solid orange;\" align=center>(.*?)</td></tr><tr><td height=4></td></tr></table>"));

  private static final SimpleDateFormat EVENT_TIMESTAMP =
      new SimpleDateFormat("MM/dd/yy hh:mm a", Locale.US);

  private EventManager() {}

  public static List<String> parseEvents(final String responseText) {
    return EVENT_PATTERNS.stream()
        .flatMap(p -> p.matcher(responseText).results())
        .map(r -> r.group(1))
        .toList();
  }

  public static boolean hasEvents() {
    return !EventManager.eventTexts.isEmpty();
  }

  public static void clearEventHistory() {
    EventManager.eventTexts.clear();
    EventManager.eventHyperTexts.clear();
  }

  public static LockableListModel<String> getEventTexts() {
    return EventManager.eventTexts;
  }

  public static LockableListModel<String> getEventHyperTexts() {
    return EventManager.eventHyperTexts;
  }

  public static void addChatEvent(final String eventHtml) {
    EventManager.addNormalEvent(eventHtml, true);
  }

  public static boolean addNormalEvent(String eventHtml) {
    return EventManager.addNormalEvent(eventHtml, false);
  }

  private static String prependTimestamp(String eventHtml) {
    return EventManager.EVENT_TIMESTAMP.format(new Date()) + " - " + eventHtml;
  }

  public static boolean addNormalEvent(String eventHtml, boolean addTimestamp) {
    if (eventHtml == null) {
      return false;
    }

    if (eventHtml.contains("logged") || eventHtml.contains("has left the building")) {
      return false;
    }

    // Add to the event hyper texts list, but only include the timestamp only to Relay Browser
    // rendering of the event
    EventManager.eventHyperTexts.add(addTimestamp ? prependTimestamp(eventHtml) : eventHtml);

    var autopull = eventHtml.contains("<table class=\"item\"");

    // The event may be marked up with color and links to
    // user profiles. For example:

    // 04/25/06 12:53:54 PM - New message received from <a target=mainpane
    // href='showplayer.php?who=115875'><font color=green>Brianna</font></a>.
    // 04/25/06 01:06:43 PM - <a class=nounder target=mainpane
    // href='showplayer.php?who=115875'><b><font color=green>Brianna</font></b></a> has played a
    // song (The Polka of Plenty) for you.

    var eventText =
        eventHtml
            // Add a space before item acquisition
            .replace("<center><table class=\"item\"", " <table")
            // Remove tags that are not hyperlinks
            .replaceAll("</?[^aA/][^>]*>", "")
            // Replace links to profiles with a player descriptor
            .replaceAll("<a[^>]*showplayer\\.php\\?who=(\\d+)[^>]*>(.*?)</a>", "$2 (#$1)")
            // Remove the rest of the tags
            .replaceAll("<[^>]*>", "");

    EventManager.eventTexts.add(addTimestamp ? prependTimestamp(eventText) : eventText);

    if (!LoginRequest.isInstanceRunning()) {
      // Print everything to the default shell; this way, the
      // graphical CLI is also notified of events.
      if (!autopull) RequestLogger.printLine(eventHtml);

      // Balloon messages for whenever the person does not have
      // focus on KoLmafia.
      if (StaticEntity.usesSystemTray()) {
        SystemTrayFrame.showBalloon(eventText);
      }
    }

    return true;
  }

  public static void checkForNewEvents(String responseText) {
    if (responseText == null) {
      return;
    }

    // Capture the entire new events table in order to display the
    // appropriate message.
    EventManager.parseEvents(responseText).stream()
        .flatMap(fullBlock -> Arrays.stream(fullBlock.split("<p>")))
        .flatMap(block -> Arrays.stream(block.split("<br(?: /)?>|\n")))
        .forEach(
            e -> {
              EventManager.addNormalEvent(e);
              if (ChatManager.isRunning()) {
                ChatManager.broadcastEvent(new EventMessage(e, "green"));
              }
            });
  }

  public static Matcher findEventsBlock(final StringBuffer responseText) {
    return EVENT_PATTERNS.stream()
        .map(p -> p.matcher(responseText))
        .filter(Matcher::find)
        .findFirst()
        .orElse(null);
  }
}
