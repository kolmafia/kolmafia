package net.sourceforge.kolmafia.session;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BastilleBattalionManager {

  // Bastille Battalion is a simulation of a video game in which your castle
  // engages in a combat with another castle in order to accumulate cheese.
  //
  // A Game has up to five Battles. You play until you are defeated or until
  // your fifth battle. Your score is the total cheese you gained.
  //
  // Each Battle has two turns of preparation, where you attempt to improve
  // stats and/or gather cheese, and one round of combat.
  //
  // Your stats are Military Attack/Defense, Castle Attack/Defense, and
  // Psychological Attack/Defense. These are supervised by your generals,
  // engineers, and artisans, respectively.
  //
  // You can play up to 5 games per day.
  //
  // This module is intended to track the state over the course of a game:
  // initial stats, changes as you train them, cheese accumulated, and so
  // on. These will be made available in properties so that scripts can use
  // them without having to parse the response text for themselves. These
  // properties will be reset at the beginning of each game and will only be
  // valid while a game is underway.
  //
  // Additionally, it is intended to record the results of games in other
  // properties, which will persist until rollover, in other scripts wish to
  // analyze them.

  // *** Stats

  // We don't actually know what your stats start at,
  //
  // Each of the four castle Upgrades will provide bonuses to one or more
  // stats.
  //
  // There are three potions which are rewards you can get from your first game
  // (won or lost) of the day which affect your stats.
  //
  // sharkfin gumbo grants 1 turn of Shark Tooth Grin
  //    Boosts military attack and defense in Bastille Battalion.
  // boiling broth grants 1 turn of Boiling Determination
  //    Boosts castle attack and defense in Bastille Battalion.
  // interrogative elixir grants 1 turn of Enhanced Interrogation
  //    Boosts psychological attack and defense in Bastille Battalion.
  //
  // The image of the console has six indicators ("needles") at the bottom
  // which show your upgrade-granted boosts to your six stats. The potions are
  // not accounted for in those.
  //
  // The "needles" each have a horizontal location (measured in pixels) which
  // can be used to determine the current level of boostage.
  //
  // (Ezandora's relay script displays that pixel value as the value of your
  // stats. That's pretty funny; they do show how your stats compare to each
  // other, but I am sure the stats are not internally measured in pixels.

  private static Map<String, Stat> enumNameToStat = new HashMap<>();

  public enum Stat {
    MA("Military Attack", 0),
    MD("Military Defense", 1),
    CA("Castle Attack", 2),
    CD("Castle Defense", 3),
    PA("Psychological Attack", 4),
    PD("Psychological Defense", 5);

    private final String name;
    private final int index;

    private Stat(String name, int index) {
      this.name = name;
      this.index = index;
      enumNameToStat.put(this.name(), this);
    }

    public String getShortName() {
      return this.name();
    }

    public String getName() {
      return this.name;
    }

    public int getIndex() {
      return this.index;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  public static class Stats {
    private final int[] stats;

    public Stats() {
      this.stats = new int[6];
    }

    public Stats(int... stats) {
      assert stats.length == 6;
      this.stats = stats;
    }

    public int get(Stat stat) {
      return stats[stat.getIndex()];
    }

    public void set(Stat stat, int value) {
      stats[stat.getIndex()] = value;
    }

    public void clear() {
      Arrays.fill(this.stats, 0);
    }

    public Stats copy() {
      return copy(this);
    }

    public Stats copy(Stats stats) {
      return new Stats(stats.stats);
    }

    public Stats add(Stats stats) {
      for (int i = 0; i < 6; ++i) {
        this.stats[i] += stats.stats[i];
      }
      return this;
    }
  }

  // *** Castles

  // There are six kinds of castle that can appear as an opponent.
  //
  // Each castle has its own set of stats. We don't know what they are,
  // just as we don't know what your initial stats are.
  //
  // Erosionseeker posted on G_D about this:
  //
  //     Avant-Garde - higher psychological defense?
  //     Imposing Citadel - higher psychological attack
  //     Generic - higher military defense??
  //     Military Fortress - higher military attack
  //     Fortified Stronghold - higher castle defense?
  //     Sprawling Chateau - higher castle attack?

  private static Map<String, Castle> imageToCastle = new HashMap<>();

  public static enum Castle {
    ART("frenchcastle", "an avant-garde art castle"),
    BORING("masterofnone", "a boring, run-of-the-mill castle"),
    CHATEAU("bigcastle", "a sprawling chateau"),
    CITADEL("berserker", "a dark and menacing citadel"),
    FORTIFIED("shieldmaster", "a fortress that puts the 'fort' in 'fortified'"),
    MILITARY("barracks", "an imposing military fortress");

    String prefix;
    String description;

    private Castle(String prefix, String description) {
      this.prefix = prefix;
      this.description = description;
      imageToCastle.put(prefix + "_1.png", this);
      imageToCastle.put(prefix + "_2.png", this);
      imageToCastle.put(prefix + "_3.png", this);
    }

    public String getPrefix() {
      return this.prefix;
    }

    @Override
    public String toString() {
      return this.description;
    }
  }

  // *** Stances

  // When you enter battle with a castle, you have three choices:
  //
  // Try to get the jump on them
  // Bide your time
  // Ready your defenses and wait for them.
  //
  // A fortress never initiates battle, so that last one is useless against
  // such.
  //
  // In a battle, either (all of) your Attack stats are compared to your foe's
  // Defense stats, or vice versa. From what I have seen, even if you choose
  // the Offensive stance, you are not guaranteed to be the Attacker

  public static enum Stance {
    OFFENSE("offense"),
    BIDE("bide"),
    DEFENSE("defense");

    String name;

    private Stance(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  // *** Upgrades

  // You can upgrade four areas of your castle
  //
  // Each upgrade provides a reward at the end of your first game, depending on
  // the style you selected for the upgrade.
  //
  // Each upgrade/style also provides a boost to specific attack/defense game stats
  //
  // The Barbican is the fortified gateway.
  //    The reward is {Muscle, Mysticality, Moxie} substats
  // The Drawbridge crosses the Moat
  //    The reward is a (Disappears at Rollover) accessory
  // The Moat surrounds your castle and is filled with something hazardous
  //    The reward is a potion which can be used to enhance future games.
  // Murder Holes are slits in walls or ceilings for launching projectiles
  //    The reward is 100 turns of a useful status effect

  private static final Map<String, Style> imageToStyle = new HashMap<>();
  private static final Map<Integer, Upgrade> optionToUpgrade = new HashMap<>();

  public static enum Upgrade {
    BARBICAN(1, "Barbican", "barb"),
    DRAWBRIDGE(2, "Drawbridge", "bridge"),
    MURDER_HOLES(3, "Murder Holes", "holes"),
    MOAT(4, "Moat", "moat");

    String name;
    String prefix;
    int option;

    private Upgrade(int option, String name, String prefix) {
      this.option = option;
      this.name = name;
      this.prefix = prefix;
      optionToUpgrade.put(this.option, this);
    }

    public String getPrefix() {
      return this.prefix;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  // *** Styles

  // There are three possible styles for each upgrade. In addition to providing
  // attack/defense boosts for the game, they provide a {Muscle, Mysticality,
  // Moxie} inspired reward at the end of your first game of the day.

  public static enum Style {
    BARBEQUE("Barbarian Barbecue", 1, Upgrade.BARBICAN, 3, 2, 0, 0, 0, 0),
    BABAR("Babar", 2, Upgrade.BARBICAN, 0, 0, 2, 3, 0, 0),
    BARBERSHOP("Barbershop", 3, Upgrade.BARBICAN, 0, 0, 0, 0, 2, 3),

    SHARKS("Sharks", 1, Upgrade.MOAT, 0, 1, 0, 0, 2, 0),
    LAVA("Lava", 2, Upgrade.MOAT, 2, 0, 0, 1, 0, 0),
    TRUTH_SERUM("Truth Serum", 3, Upgrade.MOAT, 0, 0, 2, 0, 0, 1),

    BRUTALIST("Brutalist", 1, Upgrade.DRAWBRIDGE, 2, 0, 1, 0, 2, 0),
    DRAFTSMAN("Draftsman", 2, Upgrade.DRAWBRIDGE, 0, 3, 0, 3, 0, 3),
    ART_NOUVEAU("Art Nouveau", 3, Upgrade.DRAWBRIDGE, 0, 0, 0, 0, 2, 2),

    CANNON("Cannon", 1, Upgrade.MURDER_HOLES, 2, 1, 0, 0, 0, 1),
    CATAPULT("Catapult", 2, Upgrade.MURDER_HOLES, 0, 1, 1, 1, 0, 0),
    GESTURE("Gesture", 3, Upgrade.MURDER_HOLES, 0, 0, 0, 1, 1, 1);

    private Upgrade upgrade;
    private String image;
    private Stats stats;
    private String name;

    private Style(String name, int index, Upgrade upgrade, int... stats) {
      this.upgrade = upgrade;
      this.image = upgrade.getPrefix() + index + ".png";
      assert (stats.length == 6);
      this.stats = new Stats(stats);
      this.name = upgrade + " " + name;
      imageToStyle.put(this.image, this);
    }

    @Override
    public String toString() {
      return this.name;
    }

    public void apply() {
      currentStyles.put(this.upgrade, this);
    }

    public void apply(Stats stats) {
      stats.add(this.stats);
    }
  }

  static {
    // This forces the Style enum to be initialized, which will populate
    // all the various sets and maps from the constructors.
    Style[] styles = Style.values();
    Castle[] castles = Castle.values();
  }

  // *** Cached state. This resets when you visit the Bastille Battalion
  // *** control console

  private static final Map<Upgrade, Style> currentStyles = new HashMap<>();
  private static final Map<Stat, Integer> currentStatMap = new TreeMap<>();
  private static Stats currentStats = new Stats();

  private static final Pattern STAT_PATTERN = Pattern.compile("([A-Z][A-Z])=(\\d+)");

  private static void loadStats() {
    currentStatMap.clear();
    currentStats.clear();
    String setting = Preferences.getString("_bastilleStats");
    Matcher matcher = STAT_PATTERN.matcher(setting);
    while (matcher.find()) {
      Stat stat = enumNameToStat.get(matcher.group(1));
      if (stat != null) {
        int value = Integer.valueOf(matcher.group(2));
        currentStatMap.put(stat, value);
        currentStats.set(stat, value);
      }
    }
  }

  private static void saveStats() {
    String value =
        currentStatMap.entrySet().stream()
            .map(e -> e.getKey().getShortName() + "=" + e.getValue())
            .collect(Collectors.joining(","));
    Preferences.setString("_bastilleStats", value);
  }

  // For testing
  public static Map<Upgrade, Style> getCurrentStyles() {
    return currentStyles;
  }

  public static void reset() {
    // Configuration
    currentStyles.clear();

    // Set by initial setup, which is locked in place as soon as you start your
    // first game, since you get the prizes at the end of that
    // game. Thereafter, offensive/defensive training modify them.
    Preferences.setString("_bastilleStats", "");

    // Game progress settings.

    // Two turns of offense/defense/cheese following by a castle battle The
    // game ends when you lose or beat your fifth castle
    Preferences.setInteger("_bastilleGameTurn", 0);
    Preferences.setInteger("_bastilleCheeseCollected", 0);

    // Presumably, the type of castle might influence your training choices.
    Preferences.setString("_bastilleEnemyCastle", "");
    Preferences.setString("_bastilleEnemyName", "");

    // Once you have selected offense/defense/cheese, these are your choice
    // options for that turn.
    Preferences.setString("_bastilleChoice1", "");
    Preferences.setString("_bastilleChoice2", "");
    Preferences.setString("_bastilleChoice3", "");

    Preferences.setInteger("_bastilleGames", 0);
  }

  // <img style='position: absolute; top: 233; left: 124;'
  // src=https://d2uyhvukfffg5a.cloudfront.net/otherimages/bbatt/needle.png>
  private static final Pattern IMAGE_PATTERN =
      Pattern.compile(
          "<img style='(.*?top: (\\d+).*?; left: (\\d+).*?;.*?)'[^>]*otherimages/bbatt/([^>]*)>");

  public static final Stats PIXELS = new Stats(124, 240, 124, 240, 125, 240);

  private static void parseNeedle(String topString, String leftString) {
    int top = StringUtilities.parseInt(topString);
    int left = StringUtilities.parseInt(leftString);
    Stat stat;
    switch (top) {
      case 233:
        stat = left < 200 ? Stat.MA : Stat.MD;
        break;
      case 252:
        stat = left < 200 ? Stat.CA : Stat.CD;
        break;
      case 270:
        stat = left < 200 ? Stat.PA : Stat.PD;
        break;
      default:
        return;
    }

    int value = left - PIXELS.get(stat);
    currentStatMap.put(stat, value);
    currentStats.set(stat, value);
  }

  public static void parseStyles(String text) {
    currentStatMap.clear();
    currentStats.clear();
    Matcher matcher = IMAGE_PATTERN.matcher(text);
    while (matcher.find()) {
      String image = matcher.group(4);
      if (image.startsWith("needle")) {
        parseNeedle(matcher.group(2), matcher.group(3));
        continue;
      }
      Style style = imageToStyle.get(image);
      if (style != null) {
        style.apply();
        continue;
      }
    }
    saveStats();
  }

  public static void parseNeedles(String text) {
    currentStatMap.clear();
    currentStats.clear();
    Matcher matcher = IMAGE_PATTERN.matcher(text);
    while (matcher.find()) {
      String image = matcher.group(4);
      if (!image.startsWith("needle")) {
        continue;
      }
      parseNeedle(matcher.group(2), matcher.group(3));
    }
    saveStats();
  }

  public static void parseCastleImage(String text) {
    Matcher matcher = IMAGE_PATTERN.matcher(text);
    while (matcher.find()) {
      String image = matcher.group(4);
      Castle castle = imageToCastle.get(image);
      if (castle != null) {
        Preferences.setString("_bastilleEnemyCastle", castle.getPrefix());
        return;
      }
    }
  }

  // (turn #1)
  private static final Pattern TURN_PATTERN = Pattern.compile("\\(turn #(\\d+)\\)");

  public static void parseTurn(String text) {
    Matcher matcher = TURN_PATTERN.matcher(text);
    if (matcher.find()) {
      Preferences.setInteger("_bastilleGameTurn", StringUtilities.parseInt(matcher.group(1)));
    }
  }

  // *** Game control flow

  private static void startGame() {
    Preferences.setInteger("_bastilleCheeseCollected", 0);
  }

  private static void nextTurn() {
    Preferences.increment("_bastilleGameTurn", 1);
    Preferences.setString("_bastilleChoice1", "");
    Preferences.setString("_bastilleChoice2", "");
    Preferences.setString("_bastilleChoice3", "");
  }

  private static void nextCastle() {
    Preferences.setString("_bastilleEnemyCastle", "");
    Preferences.setString("_bastilleEnemyName", "");
  }

  private static void getChoices(String responseText) {
    Map<Integer, String> choices = ChoiceUtilities.parseChoices(responseText);
    if (choices.size() == 3) {
      Preferences.setString("_bastilleChoice1", choices.get(1));
      Preferences.setString("_bastilleChoice2", choices.get(2));
      Preferences.setString("_bastilleChoice3", choices.get(3));
    }
  }

  private static void endGame() {
    Preferences.setInteger("_bastilleGameTurn", 0);
  }

  // *** Logging

  private static void logLine(String message) {
    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);
  }

  private static void logLine() {
    logLine("");
  }

  private static void logStrength() {
    StringBuilder buf = new StringBuilder();
    buf.append("Military ");
    buf.append(currentStats.get(Stat.MA));
    buf.append("/");
    buf.append(currentStats.get(Stat.MD));
    buf.append(" ");
    buf.append("Castle ");
    buf.append(currentStats.get(Stat.CA));
    buf.append("/");
    buf.append(currentStats.get(Stat.CD));
    buf.append(" ");
    buf.append("Psychological ");
    buf.append(currentStats.get(Stat.PA));
    buf.append("/");
    buf.append(currentStats.get(Stat.PD));
    String message = buf.toString();
    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);
  }

  // *** Interface for testing

  private static AdventureResult SHARK_TOOTH_GRIN = EffectPool.get(EffectPool.SHARK_TOOTH_GRIN);
  private static AdventureResult BOILING_DETERMINATION =
      EffectPool.get(EffectPool.BOILING_DETERMINATION);
  private static AdventureResult ENHANCED_INTERROGATION =
      EffectPool.get(EffectPool.ENHANCED_INTERROGATION);

  public static Stats getCurrentStats() {
    Stats stats = new Stats();
    for (Style style : currentStyles.values()) {
      style.apply(stats);
    }
    if (KoLConstants.activeEffects.contains(SHARK_TOOTH_GRIN)) {
      // Boosts military attack and defense in Bastille Battalion.
      // Or so it claims; it doesn't show up on the needles
    }
    if (KoLConstants.activeEffects.contains(BOILING_DETERMINATION)) {
      // Boosts castle attack and defense in Bastille Battalion
      // Or so it claims; it doesn't show up on the needles
    }
    if (KoLConstants.activeEffects.contains(ENHANCED_INTERROGATION)) {
      // Boosts psychological attack and defense in Bastille Battalion.
      // Or so it claims; it doesn't show up on the needles
    }
    return stats;
  }

  private static boolean checkStat(Stats stats, Stat stat) {
    int calculated = stats.get(stat);
    int expected = currentStats.get(stat);
    if (calculated != expected) {
      String message =
          stat + " was calculated to be " + calculated + " but is actually " + expected;
      System.out.println(message);
      logLine(message);
      return false;
    }
    return true;
  }

  public static boolean checkPredictions() {
    return checkPredictions(getCurrentStats());
  }

  public static boolean checkPredictions(Stats stats) {
    boolean retval = true;
    retval &= checkStat(stats, Stat.MA);
    retval &= checkStat(stats, Stat.MD);
    retval &= checkStat(stats, Stat.CA);
    retval &= checkStat(stats, Stat.CD);
    retval &= checkStat(stats, Stat.PA);
    retval &= checkStat(stats, Stat.PD);
    return retval;
  }

  // *** Interface for AdventureRequest.parseChoiceEncounter

  private static final Pattern CHEESE_PATTERN = Pattern.compile("You gain (\\d+) cheese!");

  public static void gainCheese(final String text) {
    Matcher matcher = CHEESE_PATTERN.matcher(text);
    if (matcher.find()) {
      String message = matcher.group(0);
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
      int cheese = StringUtilities.parseInt(matcher.group(1));
      Preferences.increment("_bastilleCheeseCollected", cheese);
    }
  }

  public static String parseChoiceEncounter(final int choice, final String responseText) {
    switch (choice) {
      case 1314: // Bastille Battalion (Master of None)
      case 1315: // Castle vs. Castle
      case 1317: // A Hello to Arms (Battalion)
      case 1318: // Defensive Posturing
      case 1319: // Cheese Seeking Behavior
        // Print cheese gain from previous encounter before logging this one.
        BastilleBattalionManager.gainCheese(responseText);
    }
    return null;
  }

  // *** Interface for ChoiceManager

  private static final Pattern GAMES_PATTERN = Pattern.compile("You can play <b>(\\d+)</b>");

  // According to your scanners, the nearest enemy castle is Humongous Craine, a sprawling chateau.
  private static final Pattern CASTLE_PATTERN =
      Pattern.compile("the nearest enemy castle is (.*?), an? (.*?)\\.");

  public static void visitChoice(final GenericRequest request) {
    if (request.getURLString().equals("choice.php?forceoption=0")) {
      logLine("Entering your Bastille Battalion control console.");
    }

    int choice = ChoiceManager.lastChoice;
    String text = request.responseText;

    switch (choice) {
      case 1313: // Bastille Battalion
        if (!text.contains("option=5")) {
          Preferences.setInteger("_bastilleGames", 5);
        }
        parseStyles(text);
        return;

      case 1314: // Bastille Battalion (Master of None)
        if (Preferences.getInteger("_bastilleGameTurn") == 0) {
          startGame();
        }
        nextTurn();
        Matcher castleMatcher = CASTLE_PATTERN.matcher(text);
        if (castleMatcher.find()) {
          Preferences.setString("_bastilleEnemyName", castleMatcher.group(1));
          parseCastleImage(text);
        }

        parseTurn(text);
        parseNeedles(text);
        return;

      case 1315: // Castle vs. Castle
        nextTurn();
        return;

      case 1316: // GAME OVER
        Matcher matcher = GAMES_PATTERN.matcher(text);
        if (matcher.find()) {
          Preferences.setInteger("_bastilleGames", 5 - StringUtilities.parseInt(matcher.group(1)));
        }
        endGame();
        return;

      case 1317: // A Hello to Arms (Battalion)
      case 1318: // Defensive Posturing
      case 1319: // Cheese Seeking Behavior
        getChoices(text);
        return;
    }
  }

  public static void postChoice1(final String urlString, final GenericRequest request) {
    int choice = ChoiceManager.lastChoice;
    int decision = ChoiceManager.lastDecision;
    String text = request.responseText;

    switch (choice) {
      case 1313: // Bastille Battalion
        if (decision >= 1 && decision <= 4) {
          parseStyles(text);
          logLine(currentStyles.get(optionToUpgrade.get(decision)).toString());
          logStrength();
        } else if (decision == 5) {
          logStrength();
        }
        return;

      case 1314: // Bastille Battalion (Master of None)
        return;

      case 1315: // Castle vs. Castle
        nextCastle();
        return;

      case 1316: // GAME OVER
        return;

      case 1317: // A Hello to Arms (Battalion)
      case 1318: // Defensive Posturing
      case 1319: // Cheese Seeking Behavior
        parseNeedles(text);
        logStrength();
        return;
    }
  }

  public static final boolean registerRequest(final String urlString) {
    int choice = ChoiceManager.extractChoiceFromURL(urlString);
    int decision = ChoiceManager.extractOptionFromURL(urlString);

    StringBuilder buf = new StringBuilder();
    switch (choice) {
      case 1313: // Bastille Battalion
        switch (decision) {
          case 1:
            buf.append("Decorating the Barbican");
            break;
          case 2:
            buf.append("Changing the Drawbridge");
            break;
          case 3:
            buf.append("Sizing the Murder Holes");
            break;
          case 4:
            buf.append("Filling the Moat");
            break;
          case 5:
            buf.append("Starting game #");
            buf.append(Preferences.getInteger("_bastilleGames") + 1);
            break;
          case 6:
            // Hi Scores
            return true;
          case 8:
            logLine("Walking away from the game");
            logLine();
            return true;
        }
        break;
      case 1314: // Bastille Battalion (Master of None)
        switch (decision) {
          case 1:
            buf.append("Improving offense.");
            break;
          case 2:
            buf.append("Focusing on defense.");
            break;
          case 3:
            buf.append("Looking for cheese.");
            break;
        }
        break;
      case 1315: // Castle vs. Castle
        switch (decision) {
          case 1:
            buf.append("Charge!");
            break;
          case 2:
            buf.append("Watch warily.");
            break;
          case 3:
            buf.append("Wait to be attacked.");
            break;
        }
        break;
      case 1316: // GAME OVER
        break;
      case 1317: // A Hello to Arms (Battalion)
      case 1318: // Defensive Posturing
      case 1319: // Cheese Seeking Behavior
        switch (decision) {
          case 1:
            buf.append(Preferences.getString("_bastilleChoice1"));
            break;
          case 2:
            buf.append(Preferences.getString("_bastilleChoice2"));
            break;
          case 3:
            buf.append(Preferences.getString("_bastilleChoice3"));
            break;
        }
        break;
    }

    if (buf.length() > 0) {
      logLine(buf.toString());
    }

    return true;
  }
}
