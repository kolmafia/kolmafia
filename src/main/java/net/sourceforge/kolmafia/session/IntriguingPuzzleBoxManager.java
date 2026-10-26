package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;

public class IntriguingPuzzleBoxManager {
  private static final Map<String, String> FACE_MAP =
      Map.of("left", "L", "right", "R", "top", "T", "bottom", "B", "front", "F", "rear", "K");

  private static final Pattern OPTION =
      Pattern.compile(
          "(turn|push) the (left|right|top|bottom|front|rear) face(?: (clockwise|counterclockwise))?");

  static final List<String> ACTIONS =
      List.of(
          "LCW", "LCCW", "RCW", "RCCW", "TCW", "TCCW", "BCW", "BCCW", "FCW", "FCCW", "KCW", "KCCW",
          "PL", "PR", "PT", "PB", "PF", "PK");

  static String parseOption(final String option) {
    var m = OPTION.matcher(option);
    if (!m.matches()) return null;
    var face = FACE_MAP.get(m.group(2));
    var direction = m.group(3);
    if (m.group(1).equals("push")) return direction == null ? "P" + face : null;
    if (direction == null) return null;
    return face + (direction.equals("clockwise") ? "CW" : "CCW");
  }

  public static List<String> parseHistory() {
    var pref = Preferences.getString("intriguingPuzzleBoxHistory");
    return pref.isBlank() ? List.of() : List.of(pref.split("\\|"));
  }

  public static String getPageOptions(final String page) {
    return page.split("#")[0];
  }

  public static void visitChoice(final String responseText, final int decision) {
    var choices = ChoiceUtilities.parseChoices(responseText);
    var codes =
        choices.values().stream()
            .map(text -> parseOption(text.replaceAll("\\.$", "").toLowerCase()))
            .filter(Objects::nonNull)
            .toList();

    // If something didn't parse right, do nothing.
    if (codes.size() != 6) return;

    var joined = String.join(",", codes);

    var pages = new ArrayList<>(parseHistory());
    var lastOptions = pages.isEmpty() ? "" : getPageOptions(pages.getLast());

    // If this is just a page reload, do nothing.
    if (decision == 0 && lastOptions.equals(joined)) return;

    // If we have a decision but we don't have the information from the last page, do nothing.
    if (decision != 0 && pages.isEmpty()) return;

    if (decision != 0) {
      // If we have a decision, attach it to the last page.
      pages.set(pages.size() - 1, lastOptions + "#" + decision);
    } else {
      // If we have no decision, it means we have restarted, so clear the previous pages.
      pages.clear();
    }

    pages.add(joined);
    Preferences.setString("intriguingPuzzleBoxHistory", String.join("|", pages));
  }
}
