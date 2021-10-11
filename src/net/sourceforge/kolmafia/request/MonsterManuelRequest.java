package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.session.MonsterManuelManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MonsterManuelRequest extends GenericRequest {
  public MonsterManuelRequest(final String page) {
    // questlog.php?which=6&vl=a
    super("questlog.php");
    this.addFormField("which", "6");
    if (page != null) {
      this.addFormField("vl", page);
    }
  }

  public MonsterManuelRequest(final int id) {
    this(MonsterManuelRequest.getManuelPage(id));
  }

  public static String getManuelPage(final int id) {
    MonsterData monster = MonsterDatabase.findMonsterById(id);
    if (monster == null) {
      return null;
    }

    String name = monster.getManuelName();
    if (name == null || name.length() < 1) {
      return null;
    }

    char first = name.charAt(0);
    return Character.isLetter(first) ? String.valueOf(Character.toLowerCase(first)) : "-";
  }

  private static final Pattern MONSTER_ENTRY_PATTERN =
      Pattern.compile("<a name='mon(\\d+)'>.*?</table>", Pattern.DOTALL);

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("questlog.php") || !urlString.contains("which=6")) {
      return;
    }

    // Parse the page and register each Manuel entry
    Matcher matcher = MonsterManuelRequest.MONSTER_ENTRY_PATTERN.matcher(responseText);
    while (matcher.find()) {
      int id = StringUtilities.parseInt(matcher.group(1));
      MonsterManuelManager.registerMonster(id, matcher.group(0));
    }
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("questlog.php") || !urlString.contains("which=6")) {
      return false;
    }

    // Claim but don't log Monster Manuel visits
    return true;
  }
}
