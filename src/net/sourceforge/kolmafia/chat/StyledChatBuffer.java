package net.sourceforge.kolmafia.chat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.java.dev.spellcast.utilities.ChatBuffer;
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

  public static final boolean initializeHighlights() {
    StyledChatBuffer.highlightCount = 0;
    StyledChatBuffer.searchStrings.clear();
    StyledChatBuffer.colorStrings.clear();

    String highlights = Preferences.getString("highlightList").trim();

    if (highlights.length() == 0) {
      return false;
    }

    String[] highlightList = highlights.split("\n+");

    for (int i = 0; i < highlightList.length; ++i) {
      StyledChatBuffer.addHighlight(highlightList[i], DataUtilities.toColor(highlightList[++i]));
    }

    return true;
  }

  public static final String removeHighlight(final int index) {
    --StyledChatBuffer.highlightCount;

    String searchString = StyledChatBuffer.searchStrings.remove(index);
    String colorString = StyledChatBuffer.colorStrings.remove(index);

    return searchString + "\n" + colorString;
  }

  public static final String addHighlight(final String searchString, final Color color) {
    ++StyledChatBuffer.highlightCount;

    String colorString = DataUtilities.toHexString(color);

    StyledChatBuffer.searchStrings.add(searchString.toLowerCase());
    StyledChatBuffer.colorStrings.add(colorString);

    return searchString + "\n" + colorString;
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
    String[] lines = this.getContent().split("<br>");

    this.clear();

    for (int i = 0; i < lines.length; ++i) {
      this.append(lines[i] + "<br>");
    }
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

    if (highlightMessage.length() == 0) {
      return message;
    }

    highlightMessage.append(remaining);
    return highlightMessage.toString();
  }
}
