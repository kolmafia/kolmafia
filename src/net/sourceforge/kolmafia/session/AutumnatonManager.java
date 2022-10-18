package net.sourceforge.kolmafia.session;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AutumnatonManager {
  private static final Pattern UPGRADE = Pattern.compile("autumnaton/(.*?)\\.png");

  private static final Map<String, String> UPGRADE_DESCRIPTIONS =
      Map.ofEntries(
          Map.entry("enhanced left arm", "leftarm1"),
          Map.entry("upgraded left leg", "leftleg1"),
          Map.entry("high performance right arm", "rightarm1"),
          Map.entry("high speed right leg", "rightleg1"),
          Map.entry("energy-absorptive hat", "base_blackhat"),
          Map.entry("collection prow", "cowcatcher"),
          Map.entry("vision extender", "periscope"),
          Map.entry("radar dish", "radardish"),
          Map.entry("dual exhaust", "dualexhaust"));

  public static boolean hasUpgrade(String upgrade) {
    var img = UPGRADE_DESCRIPTIONS.get(upgrade);
    if (img == null) return false;
    return Preferences.getString("autumnatonUpgrades").contains(img);
  }

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
                    .filter(e -> responseText.contains(e.getKey()))
                    .map(Map.Entry::getValue),
                Arrays.stream(Preferences.getString("autumnatonUpgrades").split(",")))
            .filter(Predicate.not(String::isBlank))
            .distinct()
            .sorted()
            .collect(Collectors.joining(","));

    Preferences.setString("autumnatonUpgrades", upgrades);
  }

  private static int calculateQuestTurns(final int questNumber) {
    var effectiveQuest = questNumber - 1;
    var upgrades = Preferences.getString("autumnatonUpgrades");
    if (upgrades.contains("leftleg1")) effectiveQuest--;
    if (upgrades.contains("rightleg1")) effectiveQuest--;

    return Math.max(1, effectiveQuest) * 11;
  }

  private static void parseQuest(final String responseText, final int snarfblat) {
    if (!responseText.contains("Good luck, little buddy")) return;
    ResultProcessor.processItem(ItemPool.AUTUMNATON, -1);
    var questNumber = Preferences.increment("_autumnatonQuests");
    var adventure = AdventureDatabase.getAdventure(snarfblat);
    if (adventure != null) {
      Preferences.setString("autumnatonQuestLocation", adventure.getAdventureName());
    }
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

  public static String getQuestLocation() {
    if (KoLCharacter.getTurnsPlayed() > Preferences.getInteger("autumnatonQuestTurn")) return "";
    return Preferences.getString("autumnatonQuestLocation");
  }

  private static final Pattern LOCATION = Pattern.compile("returns? from (.*?)(?:\\.| after)");
  private static final Pattern TURNS_LEFT = Pattern.compile("<!-- autumnback -->(\\d+)");

  public static void parseFight(final String responseText) {
    if (!responseText.contains("autumnaton.gif")) return;

    if (responseText.contains("<!-- autumnback -->")) {
      // Their quest is ongoing
      var matcher = LOCATION.matcher(responseText);
      if (matcher.find()) {
        var location = matcher.group(1);
        Preferences.setString("autumnatonQuestLocation", location);
      }
      matcher = TURNS_LEFT.matcher(responseText);
      if (matcher.find()) {
        var turnsLeft = StringUtilities.parseInt(matcher.group(1));
        Preferences.setInteger("autumnatonQuestTurn", KoLCharacter.getTurnsPlayed() + turnsLeft);
      }
    } else if (responseText.contains("Having completed its mission")) {
      // Their quest is finished
      Preferences.setString("autumnatonQuestLocation", "");
      Preferences.setInteger("autumnatonQuestTurn", KoLCharacter.getTurnsPlayed());
    }
  }

  public static String useAutumnaton() {
    GenericRequest request =
        new GenericRequest("inv_use.php?which=3&whichitem=" + ItemPool.AUTUMNATON);
    RequestThread.postRequest(request);
    return request.responseText;
  }

  private static final Pattern VISITABLE_LOCATION = Pattern.compile("<option +value=\"(\\d+)\">");

  public static Set<Integer> parseLocations(final String responseText) {
    var locs = new HashSet<Integer>();
    var m = VISITABLE_LOCATION.matcher(responseText);
    while (m.find()) {
      locs.add(Integer.parseInt(m.group(1)));
    }
    return locs;
  }
}
