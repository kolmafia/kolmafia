package net.sourceforge.kolmafia.utilities;

import net.sourceforge.kolmafia.RequestLogger;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

public class HTMLParserUtils {
  private HTMLParserUtils() {}

  public static final HtmlCleaner configureDefaultParser() {
    HtmlCleaner cleaner = new HtmlCleaner();

    CleanerProperties props = cleaner.getProperties();
    props.setTranslateSpecialEntities(false);
    props.setRecognizeUnicodeChars(false);
    props.setOmitXmlDeclaration(true);

    return cleaner;
  }

  // Log cleaned HTML

  public static final void logHTML(final Element node) {
    if (node != null) {
      StringBuffer buffer = new StringBuffer();
      HTMLParserUtils.logHTML(node, buffer, 0);
    }
  }

  private static void logHTML(final Element node, final StringBuffer buffer, int level) {
    String name = node.tagName();

    // Skip scripts
    if (name.equals("script")) {
      return;
    }

    HTMLParserUtils.indent(buffer, level);
    HTMLParserUtils.printTag(buffer, node);
    RequestLogger.updateDebugLog(buffer.toString());

    for (Node child : node.childNodes()) {
      if (child instanceof Comment object) {
        String content = object.getData();
        HTMLParserUtils.indent(buffer, level + 1);
        buffer.append("<!--");
        buffer.append(content);
        buffer.append("-->");
        RequestLogger.updateDebugLog(buffer.toString());
        continue;
      }

      if (child instanceof TextNode object) {
        String content = object.getWholeText().trim();
        if (content.equals("")) {
          continue;
        }

        HTMLParserUtils.indent(buffer, level + 1);
        buffer.append(content);
        RequestLogger.updateDebugLog(buffer.toString());
        continue;
      }

      if (child instanceof Element object) {
        HTMLParserUtils.logHTML(object, buffer, level + 1);
      }
    }
  }

  private static void indent(final StringBuffer buffer, int level) {
    buffer.setLength(0);
    for (int i = 0; i < level; ++i) {
      buffer.append(" ");
      buffer.append(" ");
    }
  }

  private static void printTag(final StringBuffer buffer, Element node) {
    String name = node.tagName();
    Attributes attributes = node.attributes();

    buffer.append("<");
    buffer.append(name);

    for (var attr : attributes) {
      buffer.append(" ");
      buffer.append(attr.getKey());
      buffer.append("=\"");
      buffer.append(attr.getValue());
      buffer.append("\"");
    }
    buffer.append(">");
  }
}
