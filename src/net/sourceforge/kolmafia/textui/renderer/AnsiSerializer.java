package net.sourceforge.kolmafia.textui.renderer;

import java.util.Deque;
import java.util.LinkedList;
import net.sourceforge.kolmafia.utilities.ColorParser;
import org.fusesource.jansi.Ansi;
import org.htmlcleaner.BaseToken;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.ContentNode;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.HtmlNode;
import org.htmlcleaner.TagNode;

public class AnsiSerializer {
  private static final int TAG_COLOR = ColorParser.rgb(50, 50, 50);

  private final Ansi ansi;

  private final Deque<Integer> colorStack = new LinkedList<>();

  private AnsiSerializer(int sizeHint) {
    ansi = Ansi.ansi(sizeHint);
  }

  private void serializeFontNode(TagNode tagNode) {
    String color = tagNode.getAttributeByName("color");
    int rgb = ColorParser.parseColor(color);
    pushColor(rgb);
    serializeChildren(tagNode);
    popColor();
  }

  private void serializeSimpleAttributeNode(TagNode tagNode) {
    Ansi.Attribute attributeOn;
    Ansi.Attribute attributeOff;
    switch (tagNode.getName()) {
      case "b":
        attributeOn = Ansi.Attribute.INTENSITY_BOLD;
        attributeOff = Ansi.Attribute.INTENSITY_BOLD_OFF;
        break;
      case "i":
        attributeOn = Ansi.Attribute.ITALIC;
        attributeOff = Ansi.Attribute.ITALIC_OFF;
        break;
      case "u":
        attributeOn = Ansi.Attribute.UNDERLINE;
        attributeOff = Ansi.Attribute.UNDERLINE_OFF;
        break;
      case "s":
        attributeOn = Ansi.Attribute.STRIKETHROUGH_ON;
        attributeOff = Ansi.Attribute.STRIKETHROUGH_OFF;
        break;
      default:
        throw new IllegalArgumentException("Not supported as simple attribute: " + tagNode);
    }
    ansi.a(attributeOn);
    serializeChildren(tagNode);
    ansi.a(attributeOff);
  }

  private void serializeUnknownTag(TagNode tagNode) {
    pushColor(TAG_COLOR);
    ansi.a('<').a(tagNode.getName());
    tagNode
        .getAttributes()
        .forEach(
            (String k, String v) -> {
              ansi.a(' ').a(k).a("=\"").a(v).a('"');
            });
    ansi.a('>');
    popColor();
    serializeChildren(tagNode);
    pushColor(TAG_COLOR);
    ansi.a("</").a(tagNode.getName()).a('>');
    popColor();
  }

  protected void serialize(TagNode tagNode) {
    switch (tagNode.getName()) {
      case "font":
        serializeFontNode(tagNode);
        break;
      case "i":
      case "b":
      case "u":
      case "s":
        serializeSimpleAttributeNode(tagNode);
        break;
      case "html":
      case "head":
      case "body":
        serializeChildren(tagNode);
        break;
      default:
        serializeUnknownTag(tagNode);
    }
  }

  protected void serialize(HtmlNode htmlNode) {
    if (htmlNode instanceof TagNode) {
      TagNode tagNode = (TagNode) htmlNode;
      serialize(tagNode);
    } else if (htmlNode instanceof ContentNode) {
      ContentNode textNode = (ContentNode) htmlNode;
      ansi.a(textNode.getContent());
    }
  }

  protected final void serializeChildren(TagNode tagNode) {
    for (BaseToken child : tagNode.getAllChildren()) {
      if (child instanceof HtmlNode) {
        HtmlNode htmlNode = (HtmlNode) child;
        serialize(htmlNode);
      }
    }
  }

  private void pushColor(int rgb) {
    colorStack.push(rgb);
    applyColor(rgb);
  }

  private void popColor() {
    Integer color = colorStack.pop();
    if (color == null) {
      return;
    }
    Integer previousColor = colorStack.peek();
    int rgb = previousColor != null ? previousColor : -1;
    applyColor(rgb);
  }

  private void applyColor(int rgb) {
    if (rgb == -1) {
      ansi.fg(Ansi.Color.DEFAULT);
    } else {
      ansi.fgRgb(rgb);
    }
  }

  public static String serializeHtml(String html) {
    AnsiSerializer serializer = new AnsiSerializer(html.length());

    CleanerProperties props = new CleanerProperties();
    props.setDeserializeEntities(true);
    HtmlCleaner cleaner = new HtmlCleaner(props);
    TagNode node = cleaner.clean(html);

    serializer.serialize(node);
    return serializer.ansi.reset().toString();
  }
}
