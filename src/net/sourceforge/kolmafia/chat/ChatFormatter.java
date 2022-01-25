package net.sourceforge.kolmafia.chat;

import java.awt.Color;
import java.util.HashMap;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ChatFormatter {
  // KoL itself inserts "chat effect" images. One of these results from the Pirate Bellow skill.
  // If we want to excise images but keep that one, the following pattern will do.

  private static final Pattern IMAGE_PATTERN =
      Pattern.compile("<img.*?/(.*?)(?<!12x12skull)\\.gif.*?>");

  private static final Pattern LASTSEEN_PATTERN = Pattern.compile("<!--lastseen:.*?-->");
  private static final Pattern EXPAND_PATTERN = Pattern.compile("</?p>");
  private static final Pattern COLOR_PATTERN = Pattern.compile("</?font.*?>");
  private static final Pattern LINEBREAK_PATTERN =
      Pattern.compile("</?br>", Pattern.CASE_INSENSITIVE);
  private static final Pattern TABLE_PATTERN = Pattern.compile("<table>.*?</table>");
  private static final Pattern EMPTY_SPAN_PATTERN = Pattern.compile("<span[^>]*></span>");

  private static final Pattern GREEN_PATTERN =
      Pattern.compile("<font color=green><b>(.*?)</font></a></b> (.*?)</font>");
  private static final Pattern LINE_COLOR_PATTERN =
      Pattern.compile("^<font color=(?:red|green)>(.*?)</font>$");
  private static final Pattern NESTED_LINKS_PATTERN =
      Pattern.compile(
          "<a target=mainpane href=\"([^<]*?)\"><font color=green>(.*?) <a[^>]+><font color=green>([^<]*?)</font></a>.</font></a>");
  private static final Pattern CHAT_LINKS_PATTERN =
      Pattern.compile("<a target=_blank href=\"([^<]*?)\">(.*?)</a>");
  private static final Pattern WHOIS_PATTERN =
      Pattern.compile("(<a [^>]*?>)<b><font color=green>([^>]*? \\(#\\d+\\))</b></a>([^<]*?)<br>");

  private static final Pattern MULTILINE_PATTERN = Pattern.compile("\n+");

  private static final String DEFAULT_TIMESTAMP_COLOR = "#7695B4";

  private static final HashMap<String, String> chatColors = new HashMap<>();

  private ChatFormatter() {}

  public static final String formatInternalMessage(final String originalContent) {
    // This is called once for all of the lines that arrive in a single response

    String normalizedContent = ChatFormatter.getNormalizedMessage(originalContent.trim());

    // KoL inserts HTML comments for special "chat effects". Keep them.
    // But, we don't want the lastseen comment
    normalizedContent = ChatFormatter.LASTSEEN_PATTERN.matcher(normalizedContent).replaceAll("");

    // noTableContent
    normalizedContent = ChatFormatter.TABLE_PATTERN.matcher(normalizedContent).replaceAll("");

    return normalizedContent;
  }

  public static final String removeMessageColors(final String originalContent) {
    // This is called for each message from a response.

    String normalizedContent = originalContent;

    // noColorContent
    normalizedContent = ChatFormatter.COLOR_PATTERN.matcher(normalizedContent).replaceAll("");

    return normalizedContent;
  }

  public static final String removeLineColor(final String originalContent) {
    // This is called for each message from a response.

    String normalizedContent = originalContent;

    // noLineColorContent
    normalizedContent =
        ChatFormatter.LINE_COLOR_PATTERN.matcher(normalizedContent).replaceAll("$1");

    return normalizedContent;
  }

  public static final String formatExternalMessage(final String originalContent) {
    String normalizedContent = ChatFormatter.getNormalizedMessage(originalContent.trim());

    // normalPrivateContent
    normalizedContent =
        StringUtilities.globalStringReplace(
            normalizedContent,
            "<font color=blue>private to ",
            "<font color=blue>private to</font></b> <b>");
    normalizedContent =
        StringUtilities.globalStringReplace(
            normalizedContent, "(private)</a></b>", "(private)</b></font></a><font color=blue>");

    return normalizedContent;
  }

  private static String getNormalizedMessage(final String originalContent) {
    if (originalContent == null || originalContent.length() == 0) {
      return "";
    }

    String normalizedContent = originalContent;

    // KoL inserts images for special "chat effects". Let the user see them.
    // noImageContent
    // normalizedContent = ChatFormatter.IMAGE_PATTERN.matcher( normalizedContent ).replaceAll( ""
    // );

    // normalBreaksContent
    normalizedContent =
        ChatFormatter.LINEBREAK_PATTERN.matcher(normalizedContent).replaceAll("<br>");

    // condensedContent
    normalizedContent = ChatFormatter.EXPAND_PATTERN.matcher(normalizedContent).replaceAll("<br>");

    // normalBoldsContent
    normalizedContent =
        StringUtilities.globalStringReplace(normalizedContent, "<br></b>", "</b><br>");

    // normalFontsContent
    normalizedContent =
        StringUtilities.globalStringReplace(normalizedContent, "<br></font>", "</font><br>");

    // normalSpanContent
    normalizedContent =
        StringUtilities.globalStringReplace(normalizedContent, "<br></span>", "</span><br>");
    normalizedContent = ChatFormatter.EMPTY_SPAN_PATTERN.matcher(normalizedContent).replaceAll("");

    // colonOrderedContent
    normalizedContent =
        StringUtilities.globalStringReplace(normalizedContent, ":</b></a>", "</a></b>:");
    normalizedContent =
        StringUtilities.globalStringReplace(normalizedContent, "</a>:</b>", "</a></b>:");
    normalizedContent =
        StringUtilities.globalStringReplace(normalizedContent, "</b></a>:", "</a></b>:");

    // italicOrderedContent
    normalizedContent = StringUtilities.globalStringReplace(normalizedContent, "<b><i>", "<i><b>");
    normalizedContent =
        StringUtilities.globalStringReplace(
            normalizedContent, "</b></font></a>", "</font></a></b>");

    // fixedGreenContent
    normalizedContent =
        ChatFormatter.GREEN_PATTERN
            .matcher(normalizedContent)
            .replaceAll("<font color=green><b>$1</b></font></a> $2</font>");
    normalizedContent =
        ChatFormatter.NESTED_LINKS_PATTERN
            .matcher(normalizedContent)
            .replaceAll("<a target=mainpane href=\"$1\"><font color=green>$2 $3</font></a>");
    normalizedContent =
        ChatFormatter.WHOIS_PATTERN
            .matcher(normalizedContent)
            .replaceAll("$1<b><font color=green>$2</font></b></a><font color=green>$3</font><br>");

    // leftAlignContent
    normalizedContent = StringUtilities.globalStringDelete(normalizedContent, "<center>");
    normalizedContent = StringUtilities.globalStringReplace(normalizedContent, "</center>", "<br>");

    return normalizedContent;
  }

  public static final String formatChatMessage(final ChatMessage message) {
    return ChatFormatter.formatChatMessage(message, true);
  }

  public static final String formatChatMessage(
      final ChatMessage message, boolean includeTimestamp) {
    StringBuilder displayHTML = new StringBuilder();

    if (includeTimestamp) {
      displayHTML.append("<font color=\"");
      displayHTML.append(ChatFormatter.DEFAULT_TIMESTAMP_COLOR);
      displayHTML.append("\">");
      displayHTML.append(message.getTimestamp());
      displayHTML.append("</font>");

      displayHTML.append(" ");
    }

    String sender = message.getSender();

    if (message instanceof SystemMessage) {
      displayHTML.append("<b><font color=\"red\">");
      displayHTML.append("<a href=\"showplayer.php?who=-1\"><b>System Message</b></a>");

      displayHTML.append(": ");
      displayHTML.append(message.getContent());
      displayHTML.append("</font></b>");
    } else if (sender == null) {
      String messageColor = null;

      if (message instanceof EventMessage) {
        messageColor = ((EventMessage) message).getColor();
      }

      if (messageColor != null) {
        displayHTML.append("<font color=");
        displayHTML.append(messageColor);
        displayHTML.append(">");
      }

      displayHTML.append(message.getContent());

      if (messageColor != null) {
        displayHTML.append("</font>");
      }
    } else if (message instanceof ModeratorMessage) {
      ModeratorMessage modMessage = (ModeratorMessage) message;

      String open, close;
      if (sender.equals("Mod Announcement")) {
        open = "<font color=green>";
        close = "</font>";
      } else {
        open = "<b><font color=red>";
        close = "</font></b>";
      }

      displayHTML.append(open);
      displayHTML.append("<a href=\"showplayer.php?who=");
      displayHTML.append(modMessage.getModeratorId());
      displayHTML.append("\">");
      displayHTML.append("<b>");
      displayHTML.append(sender);
      displayHTML.append("</b>");
      displayHTML.append("</a>");

      displayHTML.append(": ");
      displayHTML.append(message.getContent());
      displayHTML.append(close);
    } else {
      if (message.isAction()) {
        displayHTML.append("<i>");
      }

      displayHTML.append("<a href=\"showplayer.php?who=");
      displayHTML.append(ContactManager.getPlayerId(sender));
      displayHTML.append("\"><b>");

      displayHTML.append("<font color=\"");
      displayHTML.append(ChatFormatter.getChatColor(sender));
      displayHTML.append("\">");
      displayHTML.append(sender);
      displayHTML.append("</font>");
      displayHTML.append("</b></a>");

      if (!message.isAction()) {
        displayHTML.append(":");
      }

      displayHTML.append(" ");
      String chatLinkContent =
          ChatFormatter.CHAT_LINKS_PATTERN
              .matcher(message.getContent())
              .replaceAll("<a target=_blank href=\"$1\"><font color=\"blue\">$2</font></a>");
      displayHTML.append(chatLinkContent);

      if (message.isAction()) {
        displayHTML.append("</i>");
      }
    }

    displayHTML.append("<br>");

    return displayHTML.toString();
  }

  public static final String getChatColor(final String sender) {
    if (ChatFormatter.chatColors.containsKey(sender)) {
      return ChatFormatter.chatColors.get(sender);
    }

    if (sender.startsWith("/")) {
      return "green";
    }

    if (sender.equalsIgnoreCase(KoLCharacter.getUserName())
        && ChatFormatter.chatColors.containsKey("chatcolorself")) {
      return ChatFormatter.chatColors.get("chatcolorself");
    }

    if (ContactManager.isMailContact(sender.toLowerCase())
        && ChatFormatter.chatColors.containsKey("chatcolorcontacts")) {
      return ChatFormatter.chatColors.get("chatcolorcontacts");
    }

    if (ChatFormatter.chatColors.containsKey("chatcolorothers")) {
      return ChatFormatter.chatColors.get("chatcolorothers");
    }

    return "black";
  }

  public static void setChatColor(final String sender, final String color) {
    ChatFormatter.chatColors.put(sender, color);
  }

  public static final Color getRandomColor() {
    int[] colors = new int[3];

    do {
      for (int i = 0; i < 3; ++i) {
        colors[i] = 40 + KoLConstants.RNG.nextInt(160);
      }
    } while (colors[0] > 128 && colors[1] > 128 && colors[2] > 128);

    return new Color(colors[0], colors[1], colors[2]);
  }

  /**
   * Utility method to add a highlight word to the list of words currently being handled by the
   * highlighter. This method will prompt the user for the word or phrase which is to be
   * highlighted, followed by a prompt for the color which they would like to use. Cancellation
   * during any point of this process results in no chat highlighting being added.
   */
  public static final void addHighlighting() {
    String highlight =
        InputFieldUtilities.input(
            "What word/phrase would you like to highlight?", KoLCharacter.getUserName());

    if (highlight == null || highlight.length() == 0) {
      return;
    }

    Color color = ChatFormatter.getRandomColor();

    String newSetting =
        Preferences.getString("highlightList")
            + "\n"
            + StyledChatBuffer.addHighlight(highlight, color);
    Preferences.setString("highlightList", newSetting.trim());
    ChatManager.applyHighlights();
  }

  /**
   * Utility method to remove a word or phrase from being highlighted. The user will be prompted
   * with the highlights which are currently active, and the user can select which one they would
   * like to remove. Note that only one highlight at a time can be removed with this method.
   */
  public static final void removeHighlighting() {
    String[] patterns = StyledChatBuffer.searchStrings.toArray(new String[0]);

    if (patterns.length == 0) {
      InputFieldUtilities.alert("No active highlights.");
      return;
    }

    String selectedValue =
        InputFieldUtilities.input("Currently highlighting the following terms:", patterns);

    if (selectedValue == null) {
      return;
    }

    for (int i = 0; i < patterns.length; ++i) {
      if (patterns[i].equals(selectedValue)) {
        String settingString = StyledChatBuffer.removeHighlight(i);

        String oldSetting = Preferences.getString("highlightList");
        int startIndex = oldSetting.indexOf(settingString);
        int endIndex = startIndex + settingString.length();

        StringBuffer newSetting = new StringBuffer();

        if (startIndex != -1) {
          newSetting.append(oldSetting, 0, startIndex);

          if (endIndex < oldSetting.length()) {
            newSetting.append(oldSetting.substring(endIndex));
          }
        }

        String cleanString = newSetting.toString();
        cleanString = ChatFormatter.MULTILINE_PATTERN.matcher(cleanString).replaceAll("\n");
        cleanString = cleanString.trim();

        Preferences.setString("highlightList", cleanString);
      }
    }
  }
}
