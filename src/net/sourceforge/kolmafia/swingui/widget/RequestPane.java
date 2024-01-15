package net.sourceforge.kolmafia.swingui.widget;

import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.StringWriter;
import java.util.Enumeration;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLWriter;
import javax.swing.text.html.InlineView;
import net.sourceforge.kolmafia.ImageCachingEditorKit;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class RequestPane extends JEditorPane {
  static class WrappedHtmlEditorKit extends ImageCachingEditorKit {
    private final ViewFactory viewFactory;

    /*
     Code originally from https://stackoverflow.com/questions/17533451/jeditorpane-linewrap-in-java7/26583365#26583365
     by Ludovic Pecquot (https://stackoverflow.com/users/4185005/ludovic-pecquot)
     available under CC BY-SA 3.0 (http://creativecommons.org/licenses/by-sa/3.0/)
     Some code changes made from original, changes at https://github.com/kolmafia/kolmafia/commits
    */
    public WrappedHtmlEditorKit() {
      super();
      this.viewFactory = new WrappedHtmlFactory();
    }

    @Override
    public ViewFactory getViewFactory() {
      return this.viewFactory;
    }

    private static class WrappedHtmlFactory extends ImageCachingViewFactory {
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

            if (value instanceof SimpleAttributeSet attributeSet) {
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
            case View.X_AXIS -> {
              if (!Preferences.getBoolean("wrapLongLines")) {
                return super.getMinimumSpan(axis);
              }
              return 0;
            }
            case View.Y_AXIS -> {
              return super.getMinimumSpan(axis);
            }
            default -> {
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

  public RequestPane() {
    this.setEditorKit(new WrappedHtmlEditorKit());
    this.setContentType("text/html");
    this.setEditable(false);
    this.addFocusListener(
        new FocusListener() {
          @Override
          public void focusGained(FocusEvent e) {
            // In java 21, a change was made to always render the caret's position despite text
            // being uneditable
            // This isn't as useful for us as we automatically scroll to bottom
            // https://bugs.openjdk.org/browse/JDK-4512626
            getCaret().setVisible(false);
          }

          @Override
          public void focusLost(FocusEvent e) {}
        });

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
      // Provides a workaround for the writer normally trying to indent the text after 80
      // characters.
      // This results in weird text like "Example    goes here"
      if (this.getDocument() instanceof HTMLDocument) {
        HTMLWriter w =
            new HTMLWriter(
                sw,
                (HTMLDocument) this.getDocument(),
                this.getSelectionStart(),
                this.getSelectionEnd() - this.getSelectionStart()) {
              {
                setLineLength(999_999);
              }
            };
        w.write();
      } else {
        this.getEditorKit()
            .write(
                sw,
                this.getDocument(),
                this.getSelectionStart(),
                this.getSelectionEnd() - this.getSelectionStart());
      }
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

    return StringUtilities.stripHtml(selectedText);
  }
}
