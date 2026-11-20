package net.sourceforge.kolmafia.chat;

import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.preferences.Preferences;

public class StyledChatBuffer extends ChatBuffer {
  private static int highlightCount = 0;

  public static final List<String> searchStrings = new ArrayList<>();
  public static final List<String> colorStrings = new ArrayList<>();

  private final String linkColor;

  public StyledChatBuffer(
      final String title, final String linkColor, final boolean affectsHighlightBuffer) {
    super(title);

    this.linkColor = linkColor;
  }

  public static boolean initializeHighlights() {
    StyledChatBuffer.highlightCount = 0;
    StyledChatBuffer.searchStrings.clear();
    StyledChatBuffer.colorStrings.clear();

    String highlights = Preferences.getString("highlightList").trim();

    if (highlights.isEmpty()) {
      return false;
    }

    String[] highlightList = highlights.split("\n+");

    for (int i = 0; i < highlightList.length; ++i) {
      StyledChatBuffer.addHighlight(highlightList[i], highlightList[++i]);
    }

    return true;
  }

  public static String removeHighlight(final int index) {
    --StyledChatBuffer.highlightCount;

    String searchString = StyledChatBuffer.searchStrings.remove(index);
    String colorString = StyledChatBuffer.colorStrings.remove(index);

    return searchString + "\n" + colorString;
  }

  public static String addHighlight(final String searchString, final String color) {
    ++StyledChatBuffer.highlightCount;

    String colorString = StyledChatBuffer.toHexString(color);

    StyledChatBuffer.searchStrings.add(searchString.toLowerCase());
    StyledChatBuffer.colorStrings.add(colorString);

    return searchString + "\n" + colorString;
  }

  private static String toHexString(final String color) {
    int red = Character.digit(color.charAt(1), 16) * 16 + Character.digit(color.charAt(2), 16);
    int green = Character.digit(color.charAt(3), 16) * 16 + Character.digit(color.charAt(4), 16);
    int blue = Character.digit(color.charAt(5), 16) * 16 + Character.digit(color.charAt(6), 16);

    if (red < 0 || red > 255 || green < 0 || green > 255 || blue < 0 || blue > 255) {
      throw new IllegalArgumentException("Color parameter outside of expected range: " + color);
    }

    return "#" + DataUtilities.toHexString((red << 16) | (green << 8) | blue, 6);
  }

  /** Appends the given message to the chat buffer. */
  @Override
  public void append(final String message) {
    if (message == null) {
      super.append(null);
      return;
    }

    // Download all the images outside of the Swing thread
    // by downloading them here.

    String highlightMessage = message;

    for (int i = 0; i < StyledChatBuffer.highlightCount; ++i) {
      String searchString = StyledChatBuffer.searchStrings.get(i);
      String colorString = StyledChatBuffer.colorStrings.get(i);

      highlightMessage = this.applyHighlight(highlightMessage, searchString, colorString);
    }

    super.append(highlightMessage);
  }

  @Override
  public String getStyle() {
    return "body { font-family: sans-serif; font-size: "
        + Preferences.getString("chatFontSize")
        + "; } a { color: "
        + linkColor
        + "; text-decoration: none; } a.error { color: red; text-decoration: underline }";
  }

  public void applyHighlights() {
    SwingUtilities.invokeLater(
        () -> {
          String[] lines = this.getContent().split("<br>");

          this.clear();

          for (String line : lines) {
            this.append(line + "<br>");
          }
        });
  }

  private String applyHighlight(
      final String message, final String searchString, final String colorString) {
    if (message.contains("<html>")) {
      return message;
    }

    StringBuilder highlightMessage = new StringBuilder();
    String remaining = message;

    while (true) {
      int searchIndex = remaining.toLowerCase().indexOf(searchString);
      if (searchIndex == -1) {
        break;
      }

      // Do not highlight HTML tags
      int openIndex = remaining.indexOf("<");
      if (openIndex < searchIndex) {
        int closeIndex = remaining.indexOf(">", openIndex) + 1;
        if (closeIndex > 0) {
          highlightMessage.append(remaining, 0, closeIndex);
          remaining = remaining.substring(closeIndex);
          continue;
        }
      }

      int stopIndex = searchIndex + searchString.length();

      highlightMessage.append(remaining, 0, searchIndex);

      highlightMessage.append("<font color=\"");
      highlightMessage.append(colorString);
      highlightMessage.append("\">");
      highlightMessage.append(remaining, searchIndex, stopIndex);
      highlightMessage.append("</font>");

      remaining = remaining.substring(stopIndex);
    }

    if (highlightMessage.isEmpty()) {
      return message;
    }

    highlightMessage.append(remaining);
    return highlightMessage.toString();
  }
}
