package net.sourceforge.kolmafia.utilities;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorParser {

  private static final Pattern rgbPattern = Pattern.compile("#([0-9a-h]{6})");

  private ColorParser() {}

  /**
   * @param color a 6-digit #hex code or a HTML color name
   * @return a RGB color code (r << 16 & g << 8 & b) or -1 if no color can be parsed
   */
  public static int parseColor(String color) {
    color = color.toLowerCase(Locale.ROOT);
    Integer byName = colorsByName.get(color);
    if (byName != null) {
      return byName;
    }
    Matcher matcher = rgbPattern.matcher(color);
    if (matcher.matches()) {
      return Integer.parseInt(matcher.group(1), 16);
    }
    return -1;
  }

  public static int rgb(int r, int g, int b) {
    return ((r & 255) << 16) + ((g & 255) << 8) + (b & 255);
  }

  private static final Map<String, Integer> colorsByName;

  static {
    HashMap<String, Integer> colors = new HashMap<>();
    colors.put("aliceblue", rgb(240, 248, 255));
    colors.put("antiquewhite", rgb(250, 235, 215));
    colors.put("aqua", rgb(0, 255, 255));
    colors.put("aquamarine", rgb(127, 255, 212));
    colors.put("azure", rgb(240, 255, 255));
    colors.put("beige", rgb(245, 245, 220));
    colors.put("bisque", rgb(255, 228, 196));
    colors.put("black", rgb(0, 0, 0));
    colors.put("blanchedalmond", rgb(255, 235, 205));
    colors.put("blue", rgb(0, 0, 255));
    colors.put("blueviolet", rgb(138, 43, 226));
    colors.put("brown", rgb(165, 42, 42));
    colors.put("burlywood", rgb(222, 184, 135));
    colors.put("cadetblue", rgb(95, 158, 160));
    colors.put("chartreuse", rgb(127, 255, 0));
    colors.put("chocolate", rgb(210, 105, 30));
    colors.put("coral", rgb(255, 127, 80));
    colors.put("cornflowerblue", rgb(100, 149, 237));
    colors.put("cornsilk", rgb(255, 248, 220));
    colors.put("crimson", rgb(220, 20, 60));
    colors.put("cyan", rgb(0, 255, 255));
    colors.put("darkblue", rgb(0, 0, 139));
    colors.put("darkcyan", rgb(0, 139, 139));
    colors.put("darkgoldenrod", rgb(184, 134, 11));
    colors.put("darkgray", rgb(169, 169, 169));
    colors.put("darkgreen", rgb(0, 100, 0));
    colors.put("darkgrey", rgb(169, 169, 169));
    colors.put("darkkhaki", rgb(189, 183, 107));
    colors.put("darkmagenta", rgb(139, 0, 139));
    colors.put("darkolivegreen", rgb(85, 107, 47));
    colors.put("darkorange", rgb(255, 140, 0));
    colors.put("darkorchid", rgb(153, 50, 204));
    colors.put("darkred", rgb(139, 0, 0));
    colors.put("darksalmon", rgb(233, 150, 122));
    colors.put("darkseagreen", rgb(143, 188, 143));
    colors.put("darkslateblue", rgb(72, 61, 139));
    colors.put("darkslategray", rgb(47, 79, 79));
    colors.put("darkslategrey", rgb(47, 79, 79));
    colors.put("darkturquoise", rgb(0, 206, 209));
    colors.put("darkviolet", rgb(148, 0, 211));
    colors.put("deeppink", rgb(255, 20, 147));
    colors.put("deepskyblue", rgb(0, 191, 255));
    colors.put("dimgray", rgb(105, 105, 105));
    colors.put("dimgrey", rgb(105, 105, 105));
    colors.put("dodgerblue", rgb(30, 144, 255));
    colors.put("firebrick", rgb(178, 34, 34));
    colors.put("floralwhite", rgb(255, 250, 240));
    colors.put("forestgreen", rgb(34, 139, 34));
    colors.put("fuchsia", rgb(255, 0, 255));
    colors.put("gainsboro", rgb(220, 220, 220));
    colors.put("ghostwhite", rgb(248, 248, 255));
    colors.put("gold", rgb(255, 215, 0));
    colors.put("goldenrod", rgb(218, 165, 32));
    colors.put("gray", rgb(128, 128, 128));
    colors.put("grey", rgb(128, 128, 128));
    colors.put("green", rgb(0, 128, 0));
    colors.put("greenyellow", rgb(173, 255, 47));
    colors.put("honeydew", rgb(240, 255, 240));
    colors.put("hotpink", rgb(255, 105, 180));
    colors.put("indianred", rgb(205, 92, 92));
    colors.put("indigo", rgb(75, 0, 130));
    colors.put("ivory", rgb(255, 255, 240));
    colors.put("khaki", rgb(240, 230, 140));
    colors.put("lavender", rgb(230, 230, 250));
    colors.put("lavenderblush", rgb(255, 240, 245));
    colors.put("lawngreen", rgb(124, 252, 0));
    colors.put("lemonchiffon", rgb(255, 250, 205));
    colors.put("lightblue", rgb(173, 216, 230));
    colors.put("lightcoral", rgb(240, 128, 128));
    colors.put("lightcyan", rgb(224, 255, 255));
    colors.put("lightgoldenrodyellow", rgb(250, 250, 210));
    colors.put("lightgray", rgb(211, 211, 211));
    colors.put("lightgreen", rgb(144, 238, 144));
    colors.put("lightgrey", rgb(211, 211, 211));
    colors.put("lightpink", rgb(255, 182, 193));
    colors.put("lightsalmon", rgb(255, 160, 122));
    colors.put("lightseagreen", rgb(32, 178, 170));
    colors.put("lightskyblue", rgb(135, 206, 250));
    colors.put("lightslategray", rgb(119, 136, 153));
    colors.put("lightslategrey", rgb(119, 136, 153));
    colors.put("lightsteelblue", rgb(176, 196, 222));
    colors.put("lightyellow", rgb(255, 255, 224));
    colors.put("lime", rgb(0, 255, 0));
    colors.put("limegreen", rgb(50, 205, 50));
    colors.put("linen", rgb(250, 240, 230));
    colors.put("magenta", rgb(255, 0, 255));
    colors.put("maroon", rgb(128, 0, 0));
    colors.put("mediumaquamarine", rgb(102, 205, 170));
    colors.put("mediumblue", rgb(0, 0, 205));
    colors.put("mediumorchid", rgb(186, 85, 211));
    colors.put("mediumpurple", rgb(147, 112, 219));
    colors.put("mediumseagreen", rgb(60, 179, 113));
    colors.put("mediumslateblue", rgb(123, 104, 238));
    colors.put("mediumspringgreen", rgb(0, 250, 154));
    colors.put("mediumturquoise", rgb(72, 209, 204));
    colors.put("mediumvioletred", rgb(199, 21, 133));
    colors.put("midnightblue", rgb(25, 25, 112));
    colors.put("mintcream", rgb(245, 255, 250));
    colors.put("mistyrose", rgb(255, 228, 225));
    colors.put("moccasin", rgb(255, 228, 181));
    colors.put("navajowhite", rgb(255, 222, 173));
    colors.put("navy", rgb(0, 0, 128));
    colors.put("oldlace", rgb(253, 245, 230));
    colors.put("olive", rgb(128, 128, 0));
    colors.put("olivedrab", rgb(107, 142, 35));
    colors.put("orange", rgb(255, 165, 0));
    colors.put("orangered", rgb(255, 69, 0));
    colors.put("orchid", rgb(218, 112, 214));
    colors.put("palegoldenrod", rgb(238, 232, 170));
    colors.put("palegreen", rgb(152, 251, 152));
    colors.put("paleturquoise", rgb(175, 238, 238));
    colors.put("palevioletred", rgb(219, 112, 147));
    colors.put("papayawhip", rgb(255, 239, 213));
    colors.put("peachpuff", rgb(255, 218, 185));
    colors.put("peru", rgb(205, 133, 63));
    colors.put("pink", rgb(255, 192, 203));
    colors.put("plum", rgb(221, 160, 221));
    colors.put("powderblue", rgb(176, 224, 230));
    colors.put("purple", rgb(128, 0, 128));
    colors.put("red", rgb(255, 0, 0));
    colors.put("rosybrown", rgb(188, 143, 143));
    colors.put("royalblue", rgb(65, 105, 225));
    colors.put("saddlebrown", rgb(139, 69, 19));
    colors.put("salmon", rgb(250, 128, 114));
    colors.put("sandybrown", rgb(244, 164, 96));
    colors.put("seagreen", rgb(46, 139, 87));
    colors.put("seashell", rgb(255, 245, 238));
    colors.put("sienna", rgb(160, 82, 45));
    colors.put("silver", rgb(192, 192, 192));
    colors.put("skyblue", rgb(135, 206, 235));
    colors.put("slateblue", rgb(106, 90, 205));
    colors.put("slategray", rgb(112, 128, 144));
    colors.put("slategrey", rgb(112, 128, 144));
    colors.put("snow", rgb(255, 250, 250));
    colors.put("springgreen", rgb(0, 255, 127));
    colors.put("steelblue", rgb(70, 130, 180));
    colors.put("tan", rgb(210, 180, 140));
    colors.put("teal", rgb(0, 128, 128));
    colors.put("thistle", rgb(216, 191, 216));
    colors.put("tomato", rgb(255, 99, 71));
    colors.put("turquoise", rgb(64, 224, 208));
    colors.put("violet", rgb(238, 130, 238));
    colors.put("wheat", rgb(245, 222, 179));
    colors.put("white", rgb(255, 255, 255));
    colors.put("whitesmoke", rgb(245, 245, 245));
    colors.put("yellow", rgb(255, 255, 0));
    colors.put("yellowgreen", rgb(154, 205, 50));
    colorsByName = colors;
  }
}
