package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.Collection;
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
        continue;
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

    MonsterData predictedMonsterData = MonsterDatabase.findMonster(predictedMonster);

    // Some monsters cannot be uniquely identified by name as several monsters share the name.
    // Ponder also has this problem.
    // Eg, Ninja Snowman (Chopsticks)
    if (predictedMonsterData == null) {
      return;
    }

    KoLAdventure location = AdventureDatabase.getAdventure(KoLAdventure.lastVanillaLocationName);

    if (location == null) {
      return;
    }

    addPrediction(location, predictedMonsterData.getName());
    updatePreference();
  }

  private static void addPrediction(
      final int turnPredicted, final KoLAdventure location, final String predictedMonster) {
    CrystalBallManager.predictions.put(
        location.getAdventureName(),
        new Prediction(turnPredicted, location.getAdventureName(), predictedMonster));

    AdventureQueueDatabase.enqueue(location, predictedMonster);
  }

  private static void addPrediction(final KoLAdventure location, final String predictedMonster) {
    addPrediction(KoLCharacter.getCurrentRun(), location, predictedMonster);
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
    String lastAdventureName = KoLAdventure.lastVanillaLocationName;

    if (lastAdventureName == null) {
      return;
    }

    if (CrystalBallManager.predictions.isEmpty()) {
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
        .anyMatch(p -> p.monster.equalsIgnoreCase(monster) && p.location.equalsIgnoreCase(zone));
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
      Pattern.compile("<li> +(?:an?|the|some)? ?([^<]*) in ([^<]*)</li>");

  public static void parsePonder(final String responseText) {
    Collection<Prediction> oldPredictions = new ArrayList<>(predictions.values());
    predictions.clear();

    Matcher m = POSSIBLE_PREDICTION.matcher(responseText);

    while (m.find()) {
      MonsterData monster = MonsterDatabase.findMonster(m.group(1));
      KoLAdventure location = AdventureDatabase.getAdventure(m.group(2));

      // Some monsters cannot be uniquely identified by name as several monsters share the name.
      // Combat predictions also has this problem.
      // Eg, Ninja Snowman (Chopsticks)
      if (location == null || monster == null) {
        continue;
      }

      Prediction oldPrediction =
          oldPredictions.stream()
              .filter(
                  prediction ->
                      prediction.location.equals(location.getAdventureName())
                          && prediction.monster.equals(monster.getName()))
              .findAny()
              .orElse(null);

      if (oldPrediction != null) {
        addPrediction(oldPrediction.turnCount, location, monster.getName());
      } else {
        addPrediction(location, monster.getName());
      }
    }

    updatePreference();
  }
}
