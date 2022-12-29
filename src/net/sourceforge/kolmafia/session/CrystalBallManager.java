package net.sourceforge.kolmafia.session;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureQueueDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;

public final class CrystalBallManager {
  private static final Pattern[] CRYSTAL_BALL_PATTERNS = {
    Pattern.compile("your next fight will be against <b>an? (.*?)</b>"),
    Pattern.compile("next monster in this (?:zone is going to|area will) be <b>an? (.*?)</b>"),
    Pattern.compile("Look out, there's <b>an? (.*?)</b> right around the next corner"),
    Pattern.compile("There's a little you fighting a little <b>(.*?)</b>"),
    Pattern.compile("How do you feel about fighting <b>an? (.*?)</b>\\? Coz that's"),
    Pattern.compile("the next monster in this area will be <b>an? (.*?)</b>"),
    Pattern.compile("and see a tiny you fighting a tiny <b>(.*?)</b> in a tiny"),
    Pattern.compile("it looks like there's <b>an? (.*?)</b> prowling around"),
    Pattern.compile("and see yourself running into <b>an? (.*?)</b> soon"),
    Pattern.compile("showing you an image of yourself fighting <b>an? (.*?)</b>"),
    Pattern.compile("if you stick around here you're going to run into <b>an? (.*?)</b>")
  };

  private static final AdventureResult ORB = ItemPool.get(ItemPool.MINIATURE_CRYSTAL_BALL);

  private CrystalBallManager() {}

  public static class Prediction implements Comparable<Prediction> {
    public final int turnCount;
    public final String location;
    public final String monster;

    private Prediction(final int turnCount, final String location, final String monster) {
      this.turnCount = turnCount;
      this.location = location;
      this.monster = monster;
    }

    @Override
    public int compareTo(final Prediction o) {
      if (this.turnCount != o.turnCount) {
        return Integer.compare(this.turnCount, o.turnCount);
      }

      return this.location.compareTo(o.location);
    }

    @Override
    public String toString() {
      return this.turnCount + ":" + this.location + ":" + this.monster;
    }
  }

  public static final Map<String, Prediction> predictions = new HashMap<>();

  public static void clear() {
    CrystalBallManager.predictions.clear();
    updatePreference();
  }

  public static void reset() {
    CrystalBallManager.predictions.clear();

    String[] predictions = Preferences.getString("crystalBallPredictions").split("\\|");

    for (final String prediction : predictions) {
      String[] parts = prediction.split(":", 3);

      if (parts.length < 3) {
        continue;
      }

      try {
        CrystalBallManager.predictions.put(
            parts[1], new Prediction(Integer.parseInt(parts[0]), parts[1], parts[2]));
      } catch (NumberFormatException e) {
      }
    }
  }

  private static void updatePreference() {
    List<String> predictions =
        CrystalBallManager.predictions.values().stream()
            .sorted()
            .map(Prediction::toString)
            .collect(Collectors.toList());

    Preferences.setString("crystalBallPredictions", String.join("|", predictions));
  }

  /** Parses an in-combat miniature crystal ball prediction. */
  public static void parseCrystalBall(final String responseText) {
    String predictedMonster = parseCrystalBallMonster(responseText);

    if (predictedMonster == null) {
      return;
    }

    MonsterData predictedMonsterData = findMonsterName(predictedMonster);

    if (predictedMonsterData == null) {
      return;
    }

    // Don't use last vanilla adventure as it's loaded via an api request after the combat starts
    KoLAdventure location = KoLAdventure.lastVisitedLocation();

    if (location == null) {
      return;
    }

    Prediction existingPrediction = CrystalBallManager.predictions.get(location.getAdventureName());

    // If this monster has been predicted already at this location, yet was not encountered. Do not
    // do anything.
    // A new prediction was not made, the turn it expires must remain unchanged.
    if (existingPrediction != null
        && existingPrediction.monster.equals(predictedMonsterData.getName())) {
      return;
    }

    addAndEnqueuePrediction(location, predictedMonsterData.getName());
    updatePreference();
  }

  private static void addPredictionToMap(
      final int turnPredicted, final KoLAdventure location, final String predictedMonster) {
    CrystalBallManager.predictions.put(
        location.getAdventureName(),
        new Prediction(turnPredicted, location.getAdventureName(), predictedMonster));
  }

  private static void addAndEnqueuePrediction(
      final KoLAdventure location, final String predictedMonster) {
    addPredictionToMap(KoLCharacter.getCurrentRun(), location, predictedMonster);

    AdventureQueueDatabase.enqueue(location, predictedMonster);
  }

  private static String parseCrystalBallMonster(final String responseText) {
    for (Pattern p : CRYSTAL_BALL_PATTERNS) {
      Matcher matcher = p.matcher(responseText);
      if (matcher.find()) {
        return matcher.group(1);
      }
    }

    return null;
  }

  /*
   * Quote from a /devster
   *
   * orb predictions are valid if:
   * * your turnsplayed is <= the turnsplayed when you got the prediction + 1
   * OR
   * * your [lastadv] flag is the zone the prediction is in.
   */
  public static void updateCrystalBallPredictions() {
    if (CrystalBallManager.predictions.isEmpty()) {
      return;
    }

    // We use the vanilla last adventure here as sometimes mafia does not recognize it
    // And we still want to invalidate the predictions, even if mafia doesn't recognize it
    String lastAdventureName = KoLAdventure.lastZoneName;

    if (lastAdventureName == null) {
      return;
    }

    CrystalBallManager.predictions
        .values()
        .removeIf(
            prediction ->
                !prediction.location.equals(lastAdventureName)
                    && prediction.turnCount + 2 <= KoLCharacter.getCurrentRun());

    updatePreference();
  }

  // EncounterManager methods
  private static boolean isEquipped() {
    return KoLCharacter.hasEquipped(ORB, EquipmentManager.FAMILIAR);
  }

  public static boolean isCrystalBallZone(final String zone) {
    if (!isEquipped()) return false;
    return predictions.values().stream().anyMatch(p -> p.location.equalsIgnoreCase(zone));
  }

  public static boolean isCrystalBallMonster() {
    return CrystalBallManager.isCrystalBallMonster(
        MonsterStatusTracker.getLastMonsterName(), Preferences.getString("nextAdventure"));
  }

  public static boolean isCrystalBallMonster(final MonsterData monster, final String zone) {
    return CrystalBallManager.isCrystalBallMonster(monster.getName(), zone);
  }

  public static boolean isCrystalBallMonster(final String monster, final String zone) {
    // There's no message to check for so assume the correct monster in the correct zone is from the
    // crystal ball (if it is equipped)
    if (!isEquipped()) return false;
    return predictions.values().stream()
        .anyMatch(p -> isMonster(p.monster, monster) && p.location.equalsIgnoreCase(zone));
  }

  public static boolean own() {
    AdventureResult ORB = ItemPool.get(ItemPool.MINIATURE_CRYSTAL_BALL, 1);
    return (KoLCharacter.hasEquipped(ORB, EquipmentManager.FAMILIAR)
        || KoLConstants.inventory.contains(ORB));
  }

  public static void ponder() {
    if (!own()) return;
    RequestThread.postRequest(new GenericRequest("inventory.php?ponder=1", false));
  }

  private static final Pattern POSSIBLE_PREDICTION =
      Pattern.compile("<li> +(?:an?|the|some)? ?(.*? in .*?)</li>");

  public static void parsePonder(final String responseText) {
    Prediction[] oldPredictions = predictions.values().toArray(new Prediction[0]);
    predictions.clear();

    Matcher m = POSSIBLE_PREDICTION.matcher(responseText);

    while (m.find()) {
      MonsterData monster = null;
      KoLAdventure location = null;

      String group = m.group(1);
      int index = group.indexOf(" in ");

      // As locations and monsters can both have ' in ' in their name, we try to match the location,
      // then the monster
      // Only stopping when we've found both viable matches.
      while (index >= 0) {
        String monsterName = group.substring(0, index);
        String locationName = group.substring(index + 4);
        index = group.indexOf(" in ", index + 4);

        location = AdventureDatabase.getAdventure(locationName);

        if (location == null) {
          continue;
        }

        monster = findMonsterName(monsterName);

        if (monster == null) {
          continue;
        }

        break;
      }

      if (location == null || monster == null) {
        continue;
      }

      Prediction oldPrediction = null;

      for (Prediction prediction : oldPredictions) {
        if (!prediction.location.equals(location.getAdventureName())) {
          continue;
        }

        if (!isMonster(prediction.monster, monster)) {
          continue;
        }

        oldPrediction = prediction;
        break;
      }

      if (oldPrediction != null) {
        addPredictionToMap(oldPrediction.turnCount, location, monster.getName());
      } else {
        addAndEnqueuePrediction(location, monster.getName());
      }
    }

    updatePreference();
  }

  private static boolean isMonster(String predictionMonster, String monsterName) {
    return isMonster(predictionMonster, MonsterDatabase.findMonster(monsterName));
  }

  private static boolean isMonster(String predictionMonster, MonsterData monsterData) {
    // If we're asked about a monster we don't know, return false
    if (monsterData == null) {
      return false;
    }

    // If prediction monster has the same name as the monster, return true
    if (monsterData.getName().equals(predictionMonster)) {
      return true;
    }

    MonsterData predictionMonsterData = MonsterDatabase.findMonster(predictionMonster);

    // If we cannot find a predicted monster by this name, return false
    if (predictionMonsterData == null) {
      return false;
    }

    // If the manual names of both monsters match, even if they're known by different names...
    // The predicted monster might have the same name in its manual, but we may still have predicted
    // the wrong one
    return monsterData.getManuelName().equals(predictionMonsterData.getManuelName());
  }

  private static MonsterData findMonsterName(String monsterName) {
    MonsterData monster = MonsterDatabase.findMonster(monsterName);

    if (monster != null) {
      return monster;
    }

    // Some monsters cannot be uniquely identified by name as several monsters share the name.
    // Both fight predictions and pondering has this problem.
    // Eg, Ninja Snowman (Chopsticks)
    for (Map.Entry<String, MonsterData> entry : MonsterDatabase.entrySet()) {
      if (!entry.getValue().getManuelName().equals(monsterName)) {
        continue;
      }

      return entry.getValue();
    }

    return null;
  }
}
