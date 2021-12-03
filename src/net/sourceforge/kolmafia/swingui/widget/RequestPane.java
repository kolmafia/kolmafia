package net.sourceforge.kolmafia.swingui.widget;

import java.io.StringWriter;
import java.util.regex.Pattern;
import javax.swing.JEditorPane;
import javax.swing.text.html.HTMLDocument;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class RequestPane extends JEditorPane {
  private static final Pattern WHITESPACE = Pattern.compile("\n\\s*");
  private static final Pattern LINE_BREAK = Pattern.compile("<br/?>", Pattern.CASE_INSENSITIVE);

  public RequestPane() {
    this.setContentType("text/html");
    this.setEditable(false);

    HTMLDocument currentHTML = (HTMLDocument) getDocument();
    currentHTML.putProperty("multiByte", Boolean.FALSE);
  }

  @Override
  public String getSelectedText() {
    // Retrieve the HTML version of the current selection
    // so that you can override the <BR> handling.

    StringWriter sw = new StringWriter();

    try {
      this.getEditorKit()
          .write(
              sw,
              this.getDocument(),
              this.getSelectionStart(),
              this.getSelectionEnd() - this.getSelectionStart());
    } catch (Exception e) {
      // In the event that an exception happens, return
      // an empty string.

      return "";
    }

    // The HTML returned by Java is wrapped in body tags,
    // so remove those to find out the remaining HTML.

    String selectedText = sw.toString();
    int beginIndex = selectedText.indexOf("<body>");
    int endIndex = selectedText.lastIndexOf("</body>");

    if (beginIndex == -1 || endIndex == -1) {
      return "";
    }

    // skip over body tag
    beginIndex = beginIndex + 6;

    selectedText = selectedText.substring(beginIndex, endIndex).trim();
    if (Preferences.getBoolean("copyAsHTML")) {
      return selectedText;
    }

    // Now we begin trimming out some of the whitespace,
    // because that causes some strange rendering problems.

    selectedText = RequestPane.WHITESPACE.matcher(selectedText).replaceAll("\n");

    selectedText = StringUtilities.globalStringDelete(selectedText, "\r");
    selectedText = StringUtilities.globalStringDelete(selectedText, "\n");
    selectedText = StringUtilities.globalStringDelete(selectedText, "\t");

    // Finally, we start replacing the various HTML tags
    // with emptiness, except for the <br> tag which is
    // rendered as a new line.

    selectedText = RequestPane.LINE_BREAK.matcher(selectedText).replaceAll("\n").trim();
    selectedText = KoLConstants.ANYTAG_PATTERN.matcher(selectedText).replaceAll("");

    return StringUtilities.getEntityDecode(selectedText, false);
  }
}
