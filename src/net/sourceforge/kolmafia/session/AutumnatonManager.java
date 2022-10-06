package net.sourceforge.kolmafia.session;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;

public class AutumnatonManager {
  private static final Pattern UPGRADE = Pattern.compile("autumnaton/(.*?)\\.png");

  private static final Map<String, String> UPGRADE_DESCRIPTIONS =
      Map.ofEntries(
          Map.entry("leftarm1", "enhanced left arm"),
          Map.entry("leftleg1", "upgraded left leg"),
          Map.entry("rightarm1", "high performance right arm"),
          Map.entry("rightleg1", "high speed right leg"),
          Map.entry("base_blackhat", "energy-absorptive hat"),
          Map.entry("cowcatcher", "collection prow"),
          Map.entry("periscope", "vision extender"),
          Map.entry("radardish", "radar dish"),
          Map.entry("dualexhaust", "dual exhaust"));

  public static void visitChoice(final String responseText) {
    var upgrades =
        UPGRADE
            .matcher(responseText)
            .results()
            .map(m -> m.group(1))
            .filter(u -> !u.equals("base") && !u.endsWith("0"))
            .sorted()
            .collect(Collectors.joining(","));
    Preferences.setString("autumnatonUpgrades", upgrades);
  }

  private static void parseUpgrade(final String responseText) {
    if (!responseText.contains("You attach")) return;

    var upgrades =
        Stream.concat(
                UPGRADE_DESCRIPTIONS.entrySet().stream()
                    .filter(e -> responseText.contains(e.getValue()))
                    .map(Map.Entry::getKey),
                Arrays.stream(Preferences.getString("autumnatonUpgrades").split(",")))
            .distinct()
            .sorted()
            .collect(Collectors.joining(","));

    Preferences.setString("autumnatonUpgrades", upgrades);
  }

  private static int calculateQuestTurns(final int questNumber) {
    var effectiveQuest = questNumber;
    var upgrades = Preferences.getString("autumnatonUpgrades");
    if (upgrades.contains("leftleg1")) effectiveQuest--;
    if (upgrades.contains("rightleg1")) effectiveQuest--;

    return Math.max(1, effectiveQuest) * 11;
  }

  private static void parseQuest(final String responseText, final int snarfblat) {
    if (!responseText.contains("Good luck, little buddy")) return;
    var questNumber = Preferences.increment("_autumnatonQuests");
    Preferences.setInteger("autumnatonQuestLocation", snarfblat);
    Preferences.setInteger(
        "autumnatonQuestTurn", KoLCharacter.getTurnsPlayed() + calculateQuestTurns(questNumber));
  }

  public static void postChoice(
      final int decision, final String responseText, final int snarfblat) {
    switch (decision) {
      case 1 -> parseUpgrade(responseText);
      case 2 -> parseQuest(responseText, snarfblat);
    }
  }
}
