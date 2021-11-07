package net.sourceforge.kolmafia.utilities;

import java.util.Iterator;
import java.util.Map;
import net.sourceforge.kolmafia.RequestLogger;
import org.htmlcleaner.BaseToken;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.CommentNode;
import org.htmlcleaner.ContentNode;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

public class HTMLParserUtils {
  public static final HtmlCleaner configureDefaultParser() {
    HtmlCleaner cleaner = new HtmlCleaner();

    CleanerProperties props = cleaner.getProperties();
    props.setTranslateSpecialEntities(false);
    props.setRecognizeUnicodeChars(false);
    props.setOmitXmlDeclaration(true);

    return cleaner;
  }

  // Log cleaned HTML

  public static final void logHTML(final TagNode node) {
    if (node != null) {
      StringBuffer buffer = new StringBuffer();
      HTMLParserUtils.logHTML(node, buffer, 0);
    }
  }

  private static void logHTML(final TagNode node, final StringBuffer buffer, int level) {
    String name = node.getName();

    // Skip scripts
    if (name.equals("script")) {
      return;
    }

    HTMLParserUtils.indent(buffer, level);
    HTMLParserUtils.printTag(buffer, node);
    RequestLogger.updateDebugLog(buffer.toString());

    Iterator<? extends BaseToken> it = node.getAllChildren().iterator();
    while (it.hasNext()) {
      BaseToken child = it.next();

      if (child instanceof CommentNode) {
        CommentNode object = (CommentNode) child;
        String content = object.getContent();
        HTMLParserUtils.indent(buffer, level + 1);
        buffer.append("<!--");
        buffer.append(content);
        buffer.append("-->");
        RequestLogger.updateDebugLog(buffer.toString());
        continue;
      }

      if (child instanceof ContentNode) {
        ContentNode object = (ContentNode) child;
        String content = object.getContent().trim();
        if (content.equals("")) {
          continue;
        }

        HTMLParserUtils.indent(buffer, level + 1);
        buffer.append(content);
        RequestLogger.updateDebugLog(buffer.toString());
        continue;
      }

      if (child instanceof TagNode) {
        TagNode object = (TagNode) child;
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

  private static void printTag(final StringBuffer buffer, TagNode node) {
    String name = node.getName();
    Map<String, String> attributes = node.getAttributes();

    buffer.append("<");
    buffer.append(name);

    if (!attributes.isEmpty()) {
      Iterator<String> it = attributes.keySet().iterator();
      while (it.hasNext()) {
        String key = it.next();
        buffer.append(" ");
        buffer.append(key);
        buffer.append("=\"");
        buffer.append(attributes.get(key));
        buffer.append("\"");
      }
    }
    buffer.append(">");
  }
}
