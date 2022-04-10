package net.sourceforge.kolmafia.session;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class BastilleBattalionManager {

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

  private BastilleBattalionManager() {}

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
  // The image of the rig has six indicators ("needles") at the bottom
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

    public Stats add(Stats stats) {
      for (int i = 0; i < 6; ++i) {
        this.stats[i] += stats.stats[i];
      }
      return this;
    }

    public Stats subtract(Stats stats) {
      for (int i = 0; i < 6; ++i) {
        this.stats[i] -= stats.stats[i];
      }
      return this;
    }

    public Stats copy() {
      return new Stats().add(this);
    }

    public Stats diff(Stats stats) {
      return stats.copy().subtract(this);
    }

    public String toStrengthString() {
      StringBuilder buf = new StringBuilder();
      buf.append("Military ");
      buf.append(this.get(Stat.MA));
      buf.append("/");
      buf.append(this.get(Stat.MD));
      buf.append(" ");
      buf.append("Castle ");
      buf.append(this.get(Stat.CA));
      buf.append("/");
      buf.append(this.get(Stat.CD));
      buf.append(" ");
      buf.append("Psychological ");
      buf.append(this.get(Stat.PA));
      buf.append("/");
      buf.append(this.get(Stat.PD));
      return buf.toString();
    }
  }

  // *** Boosts

  // Three potions give you 1 turn of increased (by some amount) attack and
  // defense for one of military, castle, or psychological

  private static AdventureResult SHARK_TOOTH_GRIN = EffectPool.get(EffectPool.SHARK_TOOTH_GRIN);
  private static AdventureResult BOILING_DETERMINATION =
      EffectPool.get(EffectPool.BOILING_DETERMINATION);
  private static AdventureResult ENHANCED_INTERROGATION =
      EffectPool.get(EffectPool.ENHANCED_INTERROGATION);

  public static class Boosts {
    private final String boosts;

    public Boosts() {
      StringBuilder buf = new StringBuilder();
      if (KoLConstants.activeEffects.contains(SHARK_TOOTH_GRIN)) {
        buf.append('M');
      }
      if (KoLConstants.activeEffects.contains(BOILING_DETERMINATION)) {
        buf.append('C');
      }
      if (KoLConstants.activeEffects.contains(ENHANCED_INTERROGATION)) {
        buf.append('P');
      }
      this.boosts = buf.toString();
    }

    public void log() {
      if (boosts.contains("M")) {
        logLine("(Military attack and defense boosted from Shark Tooth Grin)");
      }
      if (boosts.contains("C")) {
        logLine("(Castle attack and defense boosted from Boiling Determination)");
      }
      if (boosts.contains("P")) {
        logLine("(Psychological attack and defense boosted from Enhanced Interrogation)");
      }
    }

    @Override
    public String toString() {
      return this.boosts;
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
  //
  // Observations:
  //
  // "shieldmaster" - the Fortified Stronghold - will never attack, even if you
  // selected a defensive Stance

  private static Map<String, Castle> imageToCastle = new HashMap<>();
  private static Map<String, Castle> descriptionToCastle = new HashMap<>();

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
      descriptionToCastle.put(description, this);
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
    BARBECUE("Barbarian Barbecue", 1, Upgrade.BARBICAN, 3, 2, 0, 0, 0, 0),
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

  // *** Cached state. This resets when you visit the Bastille Battalion
  // *** control rig

  private static final Map<Upgrade, Style> currentStyles = new TreeMap<>();
  private static Stats currentStats = new Stats();
  private static Castle currentCastle = null;
  private static Battle currentBattle = null;

  // *** Battle

  // One of the reasons I started this project was to collect data that could be analyzed to
  // understand how to do well at this game. The already released improved logging has made the game
  // play much more enjoyable, but I have been manually making observations and taking notes that
  // could much more usefully be recorded automatically.
  //
  // Some observations so far:
  //
  // Each kind of castle has particular strengths and weaknesses. There are six kinds of castle. I
  // believe that each is stronger in one of the six stats.
  //
  // A "game" has 5 rounds. Your foes increase in power depending on which round you encounter them.
  // For example, if I attack castle type A on round one, my attack vs. his defense may be 3:0, but
  // on rounds 2 - 5, attack vs. defense may decrease to 2:1, 1:2, and eventually 0:3. Your rewards
  // for beating a foe go up correspondingly to the difficulty.
  //
  // The role of "stance" is unclear: none of offense, bide, and defense guarantees that you will be
  // the aggressor or the defender.

  // *** Stances

  // When you enter battle with a castle, you have three choices:
  //
  // Try to get the jump on them
  // Bide your time
  // Ready your defenses and wait for them.
  //
  // In a battle, either (all of) your Attack stats are compared to your foe's
  // Defense stats, or vice versa.

  private static final Map<Integer, Stance> optionToStance = new HashMap<>();

  public static enum Stance {
    OFFENSE(1, "offensive"),
    BIDE(2, "waiting"),
    DEFENSE(3, "defensive");

    String name;

    private Stance(int option, String name) {
      this.name = name;
      optionToStance.put(option, this);
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  // *** Results

  // Your Stance may indicate your desire to attack vs. defend, but it's not entirely up to you.
  // Even if you charge in, your foe may attack first. Even if you try to defend, you may end up
  // attacking first.
  //
  // The aggressor's attacks are compared against the defender's defense.
  // You can win from 0 to 3 of these comparisons.
  // If you win 2 or 3, you win the battle and loot some cheese.
  // If you win 0 or 1, the game is over.

  public static class Results {
    // If you are the aggressor, it is your attack vs. their defense
    // If they are the aggressor, it is their attack vs. your defense
    public final boolean aggressor;

    // True if your (stat) beat their (stat)
    public final boolean military;
    public final boolean castle;
    public final boolean psychological;

    private final String value;

    public Results(boolean aggressor, boolean military, boolean castle, boolean psychological) {
      this.aggressor = aggressor;
      this.military = military;
      this.castle = castle;
      this.psychological = psychological;
      this.value = setValue();
    }

    private String setValue() {
      StringBuilder buf = new StringBuilder();
      buf.append('M');
      buf.append(this.aggressor ? 'A' : 'D');
      buf.append(this.military ? '>' : '<');
      buf.append('M');
      buf.append(this.aggressor ? 'D' : 'A');
      buf.append(",");
      buf.append('C');
      buf.append(this.aggressor ? 'A' : 'D');
      buf.append(this.castle ? '>' : '<');
      buf.append('C');
      buf.append(this.aggressor ? 'D' : 'A');
      buf.append(",");
      buf.append('P');
      buf.append(this.aggressor ? 'A' : 'D');
      buf.append(this.psychological ? '>' : '<');
      buf.append('P');
      buf.append(this.aggressor ? 'D' : 'A');
      return buf.toString();
    }

    public String getValue() {
      return this.value;
    }

    public boolean won() {
      return winCount() >= 2;
    }

    public int winCount() {
      int wins = 0;
      wins += this.military ? 1 : 0;
      wins += this.castle ? 1 : 0;
      wins += this.psychological ? 1 : 0;
      return wins;
    }
  }

  public static class Battle {
    public int number;
    public Stats stats;
    public Boosts boosts;
    public Castle enemy;
    public Stance stance;
    public Results results;
    public int cheese;

    // This is constructed when we are about to enter battle.
    // Everything except the results and cheese won is known.

    public Battle(int turn, int option) {
      this.number = (turn + 2) / 3;
      this.stats = currentStats.copy();
      this.boosts = new Boosts();
      this.enemy = currentCastle;
      this.stance = optionToStance.get(option);
      this.results = null;
      this.cheese = 0;
    }

    public void setResults(Results results) {
      this.results = results;
    }

    public void setCheese(int cheese) {
      this.cheese = cheese;
    }
  }

  static {
    // This forces the Style enum to be initialized, which will populate
    // all the various sets and maps from the constructors.
    Style[] styles = Style.values();
    Castle[] castles = Castle.values();
    Stance[] stances = Stance.values();
  }

  private static final Pattern STAT_PATTERN = Pattern.compile("([MCP][AD])=(\\d+)");

  public static void loadStats() {
    loadStats(Preferences.getString("_bastilleStats"));
  }

  public static void loadStats(String setting) {
    currentStats.clear();
    Matcher matcher = STAT_PATTERN.matcher(setting);
    while (matcher.find()) {
      Stat stat = enumNameToStat.get(matcher.group(1));
      if (stat != null) {
        int value = Integer.valueOf(matcher.group(2));
        currentStats.set(stat, value);
      }
    }
  }

  public static boolean debugStats = false;

  private static Stats logStatsDiff(Stats old, Stats updated) {
    Stats diff = old.diff(updated);
    if (debugStats) {
      System.out.println("old: " + generateSetting(old));
      System.out.println("new: " + generateSetting(updated));
      System.out.println("diff: " + generateSetting(diff));
    }
    return diff;
  }

  public static String generateSetting(Stats stats) {
    String value =
        Arrays.stream(Stat.values())
            .map(stat -> stat.name() + "=" + stats.get(stat))
            .collect(Collectors.joining(","));
    return value;
  }

  private static void saveStats(Stats stats) {
    String value = generateSetting(stats);
    Preferences.setString("_bastilleStats", value);
  }

  private static void saveStyles(Map<Upgrade, Style> styleMap) {
    String value = styleMap.values().stream().map(Style::name).collect(Collectors.joining(","));
    Preferences.setString("_bastilleCurrentStyles", value);
  }

  public static void reset() {
    // Cached configuration
    currentStyles.clear();
    currentStats.clear();
    currentCastle = null;
    currentBattle = null;

    // You can play up to five games a day
    Preferences.setInteger("_bastilleGames", 0);

    // Three (reward) potions grant one turn of an effect which will boost your
    // initial stats. If you are smart, you play all five games in a row...
    Preferences.setString("_bastilleBoosts", "");

    // When you initially visit the control rig, you can select the "style" of
    // the four available upgrades: barbican, drawbridge, murder holes, moat.
    // You can fiddle with them to your heart's content until you start your
    // first game of the day. At that point they are locked in and you will
    // receive the appropriate prizes at the end of that game, win or lose.
    Preferences.setString("_bastilleCurrentStyles", "");

    // Each configured style grants a specific bonus to the set of stats.  That
    // is locked in once you start your first game. As you progress through the
    // game, you perform actions to add or subtract to specific (or all) attack
    // or defense stats. Stats revert to your style-provided bonuses at the
    // start of each game.
    //
    // Since we don't know your actual stats; this is user visible bonuses.
    Preferences.setString("_bastilleStats", "");

    // Game progress settings.

    // Two turns of offense/defense/cheese following by a castle battle.
    // The game ends when you lose or beat your fifth castle
    Preferences.setInteger("_bastilleGameTurn", 0);
    Preferences.setInteger("_bastilleCheese", 0);

    // Presumably, the type of castle might influence your training choices.
    Preferences.setString("_bastilleEnemyCastle", "");
    Preferences.setString("_bastilleEnemyName", "");

    // Once you have selected offense/defense/cheese, these are your choice
    // options for that turn.
    Preferences.setString("_bastilleChoice1", "");
    Preferences.setString("_bastilleChoice2", "");
    Preferences.setString("_bastilleChoice3", "");

    // The attributes of the last battle are interesting, but should not carry
    // over across tests.
    Preferences.setString("_bastilleLastBattleResults", "");
    Preferences.setBoolean("_bastilleLastBattleWon", false);
    Preferences.setInteger("_bastilleLastCheese", 0);
  }

  // <img style='position: absolute; top: 233; left: 124;'
  // src=https://d2uyhvukfffg5a.cloudfront.net/otherimages/bbatt/needle.png>
  private static final Pattern IMAGE_PATTERN =
      Pattern.compile(
          "<img style='(.*?top: (\\d+).*?; left: (\\d+).*?;.*?)'[^>]*otherimages/bbatt/([^>]*)>");

  // Yes. the x-offset for Psychological is 1 pixel higher.
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
    currentStats.set(stat, value);
  }

  public static void parseStyles(String text) {
    Stats old = currentStats.copy();
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
    logStatsDiff(old, currentStats);
    saveStyles(currentStyles);
    saveStats(currentStats);
  }

  public static void parseNeedles(String text) {
    Stats old = currentStats.copy();
    currentStats.clear();
    Matcher matcher = IMAGE_PATTERN.matcher(text);
    while (matcher.find()) {
      String image = matcher.group(4);
      if (!image.startsWith("needle")) {
        continue;
      }
      parseNeedle(matcher.group(2), matcher.group(3));
    }
    logStatsDiff(old, currentStats);
    saveStats(currentStats);
  }

  // According to your scanners, the nearest enemy castle is Humongous Craine, a sprawling chateau.
  private static final Pattern CASTLE_PATTERN =
      Pattern.compile("the nearest enemy castle is ((.*?), (an? .*?)\\.)");

  public static void parseCastle(String text) {
    Matcher matcher = CASTLE_PATTERN.matcher(text);
    if (!matcher.find()) {
      return;
    }
    Castle castle = descriptionToCastle.get(matcher.group(3));
    if (castle == null) {
      return;
    }
    currentCastle = castle;
    Preferences.setString("_bastilleEnemyName", matcher.group(2));
    Preferences.setString("_bastilleEnemyCastle", castle.getPrefix());
    logLine("Your next foe is " + matcher.group(1));
  }

  // <img style='position: absolute; top: 79; left: 116;'
  // src=https://d2uyhvukfffg5a.cloudfront.net/otherimages/bbatt/bigcastle_3.png></div></center>
  // The time has come for battle.  Lew the Vast is nearby, and conflict is inevitable.
  private static final Pattern LOOMING_CASTLE_PATTERN =
      Pattern.compile("otherimages/bbatt/([a-z]+_3.png)");

  public static void parseLoomingCastle(String text) {
    Matcher matcher = LOOMING_CASTLE_PATTERN.matcher(text);
    if (!matcher.find()) {
      return;
    }
    Castle castle = imageToCastle.get(matcher.group(1));
    if (castle == null) {
      return;
    }
    currentCastle = castle;
  }

  // (turn #1)
  private static final Pattern TURN_PATTERN = Pattern.compile("\\(turn #(\\d+)\\)");

  public static boolean parseTurn(String text) {
    Matcher matcher = TURN_PATTERN.matcher(text);
    if (matcher.find()) {
      Preferences.setInteger("_bastilleGameTurn", StringUtilities.parseInt(matcher.group(1)));
      return true;
    }
    return false;
  }

  // Military results:  Your attack strength is higher than their defense.<br />
  // Castle results:  Your attack strength is lower than their defense .<br />
  // Psychological results:  Your attack strength is higher than their defense.<br /><p>
  // You have razed your foe!

  // Military results:  Your attack strength is higher than their defense.<br />
  // Castle results:  Your attack strength is lower than their defense .<br />
  // Psychological results:  Your attack strength is lower than their defense .<br /><p>
  // Unfortunately, you have been razed.

  // Military results:  Your defense is lower than their attack strength.<br />
  // Castle results:  Your defense is higher than their attack strength.<br />
  // Psychological results:  Your defense is higher than their attack strength.<br /><p>
  // You have razed your foe!

  private static final Pattern BATTLE_PATTERN =
      Pattern.compile(
          "(Military|Castle|Psychological) results:.*?(Your|Their) (attack strength|defense) is (higher|lower) than (your|their) (defense|attack strength)");

  public static Results logBattle(String text) {
    boolean aggressor = false;
    boolean military = false;
    boolean castle = false;
    boolean psychological = false;

    Matcher matcher = BATTLE_PATTERN.matcher(text);
    while (matcher.find()) {
      logLine(matcher.group(0) + ".");
      aggressor = matcher.group(3).equals("attack strength");
      boolean won = matcher.group(4).equals("higher");
      switch (matcher.group(1)) {
        case "Military":
          military = won;
          break;
        case "Castle":
          castle = won;
          break;
        case "Psychological":
          psychological = won;
          break;
      }
    }

    Results results = new Results(aggressor, military, castle, psychological);
    logLine(results.won() ? "You won!" : "You lost.");
    return results;
  }

  // *** Game control flow

  private static void startGame() {
    Preferences.setInteger("_bastilleCheese", 0);
    clearChoices();
  }

  private static void nextTurn() {
    Preferences.increment("_bastilleGameTurn", 1);
  }

  private static void clearChoices() {
    Preferences.setString("_bastilleChoice1", "");
    Preferences.setString("_bastilleChoice2", "");
    Preferences.setString("_bastilleChoice3", "");
  }

  private static void getChoices(String responseText) {
    Map<Integer, String> choices = ChoiceUtilities.parseChoices(responseText);
    if (choices.size() == 3) {
      Preferences.setString("_bastilleChoice1", choices.get(1));
      Preferences.setString("_bastilleChoice2", choices.get(2));
      Preferences.setString("_bastilleChoice3", choices.get(3));
    }
  }

  private static void startBattle(int option) {
    int turn = Preferences.getInteger("_bastilleGameTurn");
    currentBattle = new Battle(turn, option);
  }

  private static void endBattle(String text) {
    Results results = logBattle(text);
    Preferences.setBoolean("_bastilleLastBattleWon", results.won());
    Preferences.setString("_bastilleLastBattleResults", results.getValue());

    if (currentBattle != null) {
      currentBattle.setResults(results);
      currentBattle.setCheese(Preferences.getInteger("_bastilleLastCheese"));
      saveBattle(currentBattle);
    }
  }

  private static void endGame(String text) {
    Preferences.increment("_bastilleGames");
    Preferences.setInteger("_bastilleGameTurn", 0);
  }

  // *** Logging

  private static void logLine(String message) {
    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);
  }

  public static void logBoosts() {
    Boosts boosts = new Boosts();
    boosts.log();
    Preferences.setString("_bastilleBoosts", boosts.toString());
  }

  private static void logStrength() {
    String message = currentStats.toStrengthString();
    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);
  }

  private static StringBuilder logAction(StringBuilder buf, String action) {
    buf.append("Turn #");
    buf.append(Preferences.getInteger("_bastilleGameTurn"));
    buf.append(": ");
    buf.append(action);
    return buf;
  }

  // *** Interface for testing

  public static int getCurrentStat(Stat stat) {
    return currentStats.get(stat);
  }

  public static Stats getCurrentStats() {
    Stats stats = new Stats();
    for (Style style : currentStyles.values()) {
      style.apply(stats);
    }
    return stats;
  }

  private static boolean checkStat(Stats stats, Stat stat) {
    int calculated = stats.get(stat);
    int expected = currentStats.get(stat);
    if (calculated == expected) {
      return true;
    }
    String message = stat + " was calculated to be " + calculated + " but is actually " + expected;
    logLine(message);
    return false;
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

  public static int gainCheese(final String text) {
    Matcher matcher = CHEESE_PATTERN.matcher(text);
    int cheese = 0;
    if (matcher.find()) {
      String message = matcher.group(0);
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
      cheese = StringUtilities.parseInt(matcher.group(1));
      Preferences.increment("_bastilleCheese", cheese);
    }
    Preferences.setInteger("_bastilleLastCheese", cheese);
    return cheese;
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

  public static void visitChoice(final GenericRequest request) {
    String text = request.responseText;

    if (request.getURLString().equals("choice.php?forceoption=0")) {
      logLine("Entering your Bastille Battalion control rig.");
      parseStyles(text);
      logStrength();
    }

    switch (ChoiceManager.lastChoice) {
      case 1313: // Bastille Battalion
        return;

      case 1314: // Bastille Battalion (Master of None)
        parseTurn(text);
        clearChoices();
        return;

      case 1315: // Castle vs. Castle
        parseLoomingCastle(text);
        clearChoices();
        return;

      case 1316: // GAME OVER
        return;

      case 1317: // A Hello to Arms (Battalion)
      case 1318: // Defensive Posturing
      case 1319: // Cheese Seeking Behavior
        getChoices(text);
        return;
    }
  }

  public static void preChoice(final String urlString, final GenericRequest request) {
    int choice = ChoiceManager.lastChoice;
    int decision = ChoiceManager.lastDecision;

    switch (choice) {
      case 1315: // Castle vs. Castle
        // If we are about to take  a stance and battle...
        if (decision != 0) {
          startBattle(decision);
        }
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
          // Your stats reset to those provided by your styles at the start of
          // each game.
          startGame();
          parseCastle(text);
          parseTurn(text);
          parseStyles(text);
          logBoosts();
          logStrength();
        }
        return;

      case 1314: // Bastille Battalion (Master of None)
        return;

      case 1315: // Castle vs. Castle
        endBattle(text);
        switch (ChoiceManager.extractChoice(text)) {
          case 1314:
            // We won and it wasn't the last battle.
            parseCastle(text);
            logStrength();
            break;
          case 1316:
            // We lost or it was the last choice
            endGame(text);
            break;
        }
        return;

      case 1316: // GAME OVER
        return;

      case 1317: // A Hello to Arms (Battalion)
      case 1318: // Defensive Posturing
      case 1319: // Cheese Seeking Behavior
        if (!parseTurn(text)) {
          nextTurn();
        }
        parseNeedles(text);
        logStrength();
        return;
    }
  }

  public static final boolean registerRequest(final String urlString) {
    int choice = ChoiceManager.extractChoiceFromURL(urlString);
    int decision = ChoiceManager.extractOptionFromURL(urlString);
    int turn = Preferences.getInteger("_bastilleGameTurn");

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
            logLine("");
            return true;
        }
        break;
      case 1314: // Bastille Battalion (Master of None)
        switch (decision) {
          case 1:
            logAction(buf, "Improving offense.");
            break;
          case 2:
            logAction(buf, "Focusing on defense.");
            break;
          case 3:
            logAction(buf, "Looking for cheese.");
            break;
        }
        break;
      case 1315: // Castle vs. Castle
        switch (decision) {
          case 1:
            logAction(buf, "Charge!");
            break;
          case 2:
            logAction(buf, "Watch warily.");
            break;
          case 3:
            logAction(buf, "Wait to be attacked.");
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

  // *** Game Logging for Analysis

  //
  // Since I am not (yet) working on a script to automate this, I want the data to be automatically
  // saved to a file in a format which can be read by an analysis script via file_to_map().
  //
  // This is the proposed format.
  //
  // The file is a tab delimited file in "data" named Bastille.battles.txt
  //
  // Note that this is not user specific; you and all of your multis will contribute to the results
  //
  // The "key" is DATE.PLAYERID.GAME.ROUND
  //
  // The fields are designed to be easily loaded into an ASH record.
  //
  // 20220409.ID.1.1	1	1	4	-1	6	0	5	MCP	bigcastle	offense	true	true	false	true	142
  //
  // record battle {
  //     int number;         // Affects strength of enemy
  //     int [6] stats;      // MA/MD/CA/CD/PA/PD
  //     string boosts;      // MCP
  //     string enemy;       // frenchcastle,masterofnone,bigcastle,
  //                         // berserker,shieldmaster,barracks
  //     string stance;      // {offensive,waiting,defensive}
  //     boolean aggressor;  // as opposed to defender
  //     boolean military;   // true if won
  //     boolean castle;     // true if won
  //     boolean psych;      // true if won
  //     int cheese;         // if won
  // };
  //
  // battle [string] battles;
  // file_to_map( FILENAME, battles );

  private static final String BATTLE_FILE_NAME = "Bastille.battles.txt";

  private static String joinFields(String separator, String... fields) {
    return Arrays.stream(fields).collect(Collectors.joining(separator));
  }

  private static String generateKey(int game, int number) {
    return joinFields(
        ".",
        KoLConstants.DAILY_FORMAT.format(new Date()),
        KoLCharacter.getPlayerId(),
        String.valueOf(game),
        String.valueOf(number));
  }

  private static String generateFields(Battle battle) {
    return joinFields(
        "\t",
        String.valueOf(battle.number),
        String.valueOf(battle.stats.get(Stat.MA)),
        String.valueOf(battle.stats.get(Stat.MD)),
        String.valueOf(battle.stats.get(Stat.CA)),
        String.valueOf(battle.stats.get(Stat.CD)),
        String.valueOf(battle.stats.get(Stat.PA)),
        String.valueOf(battle.stats.get(Stat.PD)),
        battle.boosts.toString(),
        battle.enemy.getPrefix(),
        battle.stance.toString(),
        String.valueOf(battle.results.aggressor),
        String.valueOf(battle.results.military),
        String.valueOf(battle.results.castle),
        String.valueOf(battle.results.psychological),
        String.valueOf(battle.cheese));
  }

  private static void saveBattle(Battle battle) {
    if (!Preferences.getBoolean("logBastilleBattalionBattles")) {
      return;
    }

    int game = Preferences.getInteger("_bastilleGames") + 1;
    int number = battle.number;
    String key = generateKey(game, number);
    String fields = generateFields(battle);

    StringBuilder buf = new StringBuilder();
    buf.append(key);
    buf.append("\t");
    buf.append(fields);
    String line = buf.toString();

    String path = KoLConstants.DATA_DIRECTORY + BATTLE_FILE_NAME;
    try (PrintStream stream = LogStream.openStream(path, false)) {
      stream.println(line);
    }
  }
}
