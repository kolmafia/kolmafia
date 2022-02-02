package net.sourceforge.kolmafia.session;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.preferences.Preferences;

public class LocketManager {
  private static final Set<Integer> knownMonsters = new TreeSet<>();
  private static final Pattern REMINISCABLE_MONSTER = Pattern.compile("<option value=\"(\\d+)\"");

  private static void addFoughtMonster(int monsterId) {
    String newPref =
        Stream.concat(
                Arrays.stream(Preferences.getString("_locketMonsters").split(","))
                    .filter(i -> !i.isEmpty())
                    .map(Integer::parseInt),
                Stream.of(monsterId))
            .distinct()
            .map(Object::toString)
            .collect(Collectors.joining(","));
    Preferences.setString("_locketMonsters", newPref);
  }

  private LocketManager() {}

  public static void rememberMonster(int monsterId) {
    knownMonsters.add(monsterId);
  }

  public static Set<Integer> getRememberedMonsters() {
    return Collections.unmodifiableSet(knownMonsters);
  }

  public static boolean remembersMonster(int monsterId) {
    return knownMonsters.contains(monsterId);
  }

  public static void parseMonsters(final String text) {
    // Visiting the reminisce page is a source of truth
    knownMonsters.clear();

    var m = REMINISCABLE_MONSTER.matcher(text);

    while (m.find()) {
      rememberMonster(Integer.parseInt(m.group(1)));
    }
  }

  public static void parseFight(final MonsterData monster, final String text) {
    if (!text.contains("loverslocketframe.png")) {
      return;
    }

    // This will not double an existing id so is safe to run at any round
    addFoughtMonster(monster.getId());

    Preferences.setString("locketPhylum", monster.getPhylum().toString());
  }
}
