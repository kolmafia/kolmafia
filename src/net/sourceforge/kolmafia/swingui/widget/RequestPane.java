package net.sourceforge.kolmafia.swingui.widget;

import java.awt.*;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.InlineView;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class RequestPane extends JEditorPane {
  static class WrappedHtmlEditorKit extends HTMLEditorKit {
    private final ViewFactory viewFactory;

    public WrappedHtmlEditorKit() {
      super();
      this.viewFactory = new WrappedHtmlFactory();
    }

    @Override
    public ViewFactory getViewFactory() {
      return this.viewFactory;
    }

    private static class WrappedHtmlFactory extends HTMLEditorKit.HTMLFactory {
      @Override
      public View create(Element elem) {
        View view = super.create(elem);

        if (view instanceof LabelView) {
          Object attribute = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);

          if ((attribute instanceof HTML.Tag) && (attribute == HTML.Tag.BR)) {
            return view;
          }

          return new WrapInlineView(elem);
        }

        return view;
      }

      private static class WrapInlineView extends InlineView {
        private String title;

        public WrapInlineView(Element elem) {
          super(elem);

          Enumeration<?> iterator = elem.getAttributes().getAttributeNames();

          while (iterator.hasMoreElements()) {
            Object attribute = iterator.nextElement();
            Object value = elem.getAttributes().getAttribute(attribute);

            if (value instanceof SimpleAttributeSet) {
              SimpleAttributeSet attributeSet = (SimpleAttributeSet) value;

              String text = (String) attributeSet.getAttribute(HTML.Attribute.TITLE);

              if (text == null) {
                continue;
              }

              this.title = text;
              break;
            }
          }
        }

        @Override
        public float getMinimumSpan(int axis) {
          switch (axis) {
            case View.X_AXIS:
              {
                if (!Preferences.getBoolean("wrapLongLines")) {
                  return super.getMinimumSpan(axis);
                }
                return 0;
              }
            case View.Y_AXIS:
              {
                return super.getMinimumSpan(axis);
              }
            default:
              {
                throw new IllegalArgumentException("Invalid axis: " + axis);
              }
          }
        }

        @Override
        public String getToolTipText(float x, float y, Shape allocation) {
          return this.title;
        }
      }
    }
  }

  private static final Pattern WHITESPACE = Pattern.compile("\n\\s*");
  private static final Pattern LINE_BREAK = Pattern.compile("<br/?>", Pattern.CASE_INSENSITIVE);

  public RequestPane() {
    this.setEditorKit(new WrappedHtmlEditorKit());
    this.setContentType("text/html");
    this.setEditable(false);

    // No need to unregister the component as this only registers variables on this component, not
    // on the shared instance.
    ToolTipManager.sharedInstance().registerComponent(this);

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
