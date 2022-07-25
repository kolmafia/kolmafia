package net.sourceforge.kolmafia.textui.command;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public class ColdMedicineCabinetCommand extends AbstractCommand {
  public ColdMedicineCabinetCommand() {
    this.usage = " - show information about the cold medicine cabinet";
  }

  private static final Map<Character, AdventureResult> PILLS =
      Map.ofEntries(
          Map.entry('i', ItemPool.get(ItemPool.EXTROVERMECTIN)),
          Map.entry('o', ItemPool.get(ItemPool.HOMEBODYL)),
          Map.entry('u', ItemPool.get(ItemPool.BREATHITIN)),
          Map.entry('x', ItemPool.get(ItemPool.FLESHAZOLE)));

  /**
   * Count all the last combat environments
   *
   * @return The lastCombatEnvironments pref transformed into a map of environment characters to
   *     counts
   */
  private static Map<Character, Integer> getCounts() {
    return Preferences.getString("lastCombatEnvironments")
        .chars()
        .mapToObj(i -> (char) i)
        .collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(i -> 1)));
  }

  public static AdventureResult nextPill() {
    return nextPill(getCounts());
  }

  public static AdventureResult nextPill(Map<Character, Integer> counts) {
    int unknown = counts.getOrDefault('?', 0);

    if (unknown > 10) return null;

    for (var e : counts.entrySet()) {
      var environment = e.getKey();
      if (environment == '?') continue;
      var count = e.getValue();
      // If we have an overall majority return it.
      if (count > 10) return PILLS.get(environment);
      // If there is a potential majority when considering unknowns, return none.
      if ((count + unknown) > 10) return null;
    }

    // Otherwise return the pill you get from having a majority not inside, outside or underground.
    return PILLS.get('x');
  }

  @Override
  public void run(final String cmd, String parameter) {
    var output = new StringBuilder();

    var counts = getCounts();
    var pill = nextPill(counts);

    var pillName = (pill != null) ? pill.toString() : "unknown";

    output.append("Your next pill is ").append(pillName);

    RequestLogger.printLine(output.toString());
  }
}
