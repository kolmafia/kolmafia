package net.sourceforge.kolmafia.webui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MemoriesDecorator {
  private static final Pattern ELEMENT_PATTERN =
      Pattern.compile("<select name=\"slot[12345]\">.*?</select>", Pattern.DOTALL);

  public static final void decorateElements(final int choice, final StringBuffer buffer) {
    // Handle only Elements choice adventure
    if (choice != 392) {
      return;
    }

    // Prefill the element dropdowns correctly
    Matcher matcher = ELEMENT_PATTERN.matcher(buffer);
    MemoriesDecorator.selectElement(matcher, buffer, "sleaze");
    MemoriesDecorator.selectElement(matcher, buffer, "spooky");
    MemoriesDecorator.selectElement(matcher, buffer, "stench");
    MemoriesDecorator.selectElement(matcher, buffer, "cold");
    MemoriesDecorator.selectElement(matcher, buffer, "hot");
  }

  private static void selectElement(
      final Matcher matcher, final StringBuffer buffer, final String element) {
    if (!matcher.find()) {
      return;
    }

    String oldSelect = matcher.group(0);
    String newSelect =
        StringUtilities.globalStringReplace(oldSelect, ">" + element, " selected>" + element);
    int index = buffer.indexOf(oldSelect);
    buffer.replace(index, index + oldSelect.length(), newSelect);
  }

  // "your ancestral memories are total, absolute jerks. </p></td>"
  private static final String JERKS = "absolute jerks. </p>";
  private static final String SECRET =
      "<center><table class=\"item\" style=\"float: none\" rel=\"id=4114&s=0&q=0&d=0&g=0&t=0&n=1\"><tr><td><img src=/images/itemimages/futurebox.gif\" alt=\"secret from the future\" title=\"secret from the future\" class=hand onClick='descitem(502821529)'></td><td valign=center class=effect>You acquire an item: <b>secret from the future</b></td></tr></table></center>";

  public static final void decorateElementsResponse(final StringBuffer buffer) {
    int index = buffer.indexOf(MemoriesDecorator.JERKS);
    if (index != -1) {
      buffer.insert(index + MemoriesDecorator.JERKS.length(), MemoriesDecorator.SECRET);
      return;
    }
  }

  private static final String[][] ELEMENTS = {
    {"strikes a match", "red"},
    {"lit match", "red"},
    {"vile-smelling, milky-white replicant blood", "green"},
    {"vile-smelling, milky-white blood", "green"},
    {"spinning, whirring, vibrating, tubular \"appendage.\"", "blueviolet"},
    {"spinning, whirring, vibrating, tubular appendage", "blueviolet"},
    {"liquid nitrogen", "blue"},
    {"freaky alien thing", "gray"},
  };

  public static final void decorateMegalopolisFight(final StringBuffer buffer) {
    if (!KoLCharacter.hasEquipped(ItemPool.get(ItemPool.RUBY_ROD, 1))) {
      return;
    }

    for (int i = 0; i < MemoriesDecorator.ELEMENTS.length; ++i) {
      String message = MemoriesDecorator.ELEMENTS[i][0];
      String color = MemoriesDecorator.ELEMENTS[i][1];
      if (buffer.indexOf(message) != -1) {
        StringUtilities.singleStringReplace(
            buffer, message, "<font color=" + color + ">" + message + "</font>");
        return;
      }
    }
  }
}
