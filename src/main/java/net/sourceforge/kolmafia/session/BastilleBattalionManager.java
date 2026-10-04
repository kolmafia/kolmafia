package net.sourceforge.kolmafia.session;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class BastilleBattalionManager {

  // Bastille Battalion is a simulation of a video game in which your castle
  // engages in a combat with another castle in order to accumulate cheese.
  //
  // A Game has up to five Battles. You play until you are defeated or until
  // you win the fifth battle. Your score is the total cheese you gained.
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
  // This module tracks the state over the course of a game:
  // stats, changes as you train them, cheese accumulated, and so on.
  //
  // These are made available in properties so that scripts can use them
  // without having to parse the response text for themselves. The properties
  // reset at the beginning of a game and are valid while a game is underway.
  //
  // This module records the results of games in other properties, which will
  // persist until rollover, in case other scripts wish to analyze them.

  // *** Unknowns:
  //
  // Cheese:
  //
  // - The exact spread of the fuzz applied to cheese yields. It looks like
  //   about 10% either way.

  // *** Mechanics:
  //
  // Stats:
  //
  // - Every game starts at a baseline of MA 100, MD 90, CA 110, CD 100,
  //   PA 110, PD 110, plus a fixed delta for each of the four styles. The
  //   style deltas are independent of each other.
  // - Each preparation option adds a fixed delta, so stats can be tracked
  //   exactly through a game. There is no per-game randomness.
  // - The "needles" move one pixel per 7.5 stat points. An attack needle sits
  //   at 124 + floor((stat - 95) / 7.5) and a defense needle at
  //   240 + floor((stat - 87.5) / 7.5).
  // - Each turn of a potion effect adds 10% to the matching attack and
  //   defense in battle, up to 3 turns (30%). Potions do not move the needles
  //   or affect stat-scaling cheese yields.
  //
  // Castles:
  //
  // - Base stats for each castle type:
  //                 MA   MD   CA   CD   PA   PD
  //   frenchcastle   80   80  100  100  120  120
  //   masterofnone  110  100  110  100  110  100
  //   bigcastle     100  100  120  120   80   80
  //   berserker     130   70  120   80  110   90
  //   shieldmaster   90  110   70  130   80  120
  //   barracks      120  120   80   80  100  100
  // - Each game draws a bracket of 31 castles. After each battle, every stat
  //   of every other castle independently becomes
  //   floor(stat * U(105..125) / 100), then half of them survive at random
  //   (31 -> 15 -> 7 -> 3 -> 1). Your next foe is one of the survivors.
  // - The defender wins ties. When you attack, a comparison is won only if
  //   your attack is strictly higher than their defense. When they attack, it
  //   is won if your defense is at least their attack.
  //
  // Cheese:
  //
  // - The 3 non-scaling encounters yield 20, 50 and 100.
  // - The 12 stat-scaling encounters yield the stat itself, or 200 - stat
  //   for the reversed ones.
  // - The wishing well needs 10 cheese, then yields 300 one time in 3.
  // - Every yield above is fuzzed, with a minimum of 10.
  // - Defeating a castle yields a 10-20 roll for every turn elapsed, summed
  //   (45 per battle round on average).
  // - Potions that affect stats do not affect stat-scaling yields.
  //
  // Stances:
  //
  // - offensive is 80% aggressor, 20% defender
  // - waiting is 50% aggressor, 50% defender, and adds 10 to your stats in
  //   that battle before any potion multiplier
  // - defensive is 20% aggressor, 80% defender

  private BastilleBattalionManager() {}

  // *** Stats

  // Your stats start each game at BASELINE plus the deltas of your four styles.
  //
  // Each of the four castle Upgrades provides bonuses to one or more stats.
  //
  // There are three potions which are rewards you can get from your first game
  // (won or lost) of the day which affect your stats (for combats only).
  //
  // sharkfin gumbo grants 1 turn of Shark Tooth Grin
  //    Boosts military attack and defense in Bastille Battalion.
  // boiling broth grants 1 turn of Boiling Determination
  //    Boosts castle attack and defense in Bastille Battalion.
  // interrogative elixir grants 1 turn of Enhanced Interrogation
  //    Boosts psychological attack and defense in Bastille Battalion.
  //
  // The image of the rig has six indicators ("needles") at the bottom which
  // show your upgrade-granted boosts to your six stats. The potions do not
  // affect that display.
  //
  // The "needles" each have a horizontal location (measured in pixels) which
  // moves one pixel per 7.5 stat points. We track exact stats and use the
  // needles as a cross-check.

  public enum Stat {
    MA("Military Attack"),
    MD("Military Defense"),
    CA("Castle Attack"),
    CD("Castle Defense"),
    PA("Psychological Attack"),
    PD("Psychological Defense");

    private final String name;

    private Stat(String name) {
      this.name = name;
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
      return stats[stat.ordinal()];
    }

    public void set(Stat stat, int value) {
      stats[stat.ordinal()] = value;
    }

    public Stats add(Stats stats) {
      for (int i = 0; i < 6; ++i) {
        this.stats[i] += stats.stats[i];
      }
      return this;
    }

    public Stats copy() {
      return new Stats().add(this);
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

  // *** Castles

  // There are six kinds of castle that can appear as an opponent.
  //
  // Each castle type has fixed base stats (see "Mechanics" above) which
  // grow randomly each round of a game.

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
  // Each upgrade provides a reward at the end of your first game of the day,
  // depending on the style you selected for the upgrade.
  //
  // Each upgrade/style also provides a boost to specific attack/defense stats
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

    final String name;
    final String prefix;
    final int option;

    private Upgrade(int option, String name, String prefix) {
      this.name = name;
      this.prefix = prefix;
      this.option = option;
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
  //
  // Each style adds a fixed delta (MA, MD, CA, CD, PA, PD) to BASELINE,
  // independently of the other styles.

  public static enum Style {
    BARBECUE("Barbarian Barbecue", 1, Upgrade.BARBICAN, 20, 20, 0, 0, 0, 0),
    BABAR("Babar", 2, Upgrade.BARBICAN, 0, 0, 20, 20, 0, 0),
    BARBERSHOP("Barbershop", 3, Upgrade.BARBICAN, 0, 0, 0, 0, 20, 20),

    BRUTALIST("Brutalist", 1, Upgrade.DRAWBRIDGE, 10, 0, 10, 0, 10, 0),
    DRAFTSMAN("Draftsman", 2, Upgrade.DRAWBRIDGE, 0, 20, 0, 20, 0, 20),
    NOUVEAU("Art Nouveau", 3, Upgrade.DRAWBRIDGE, 0, 0, 0, 0, 15, 15),

    CANNON("Cannon", 1, Upgrade.MURDER_HOLES, 10, 10, 0, -10, -10, 0),
    CATAPULT("Catapult", 2, Upgrade.MURDER_HOLES, 0, 10, 10, 0, -10, -10),
    GESTURE("Gesture", 3, Upgrade.MURDER_HOLES, 0, 0, 0, 0, 0, 0),

    SHARKS("Sharks", 1, Upgrade.MOAT, 0, 10, -10, 0, 10, -10),
    LAVA("Lava", 2, Upgrade.MOAT, 10, 0, -10, 10, 0, -10),
    TRUTH("Truth Serum", 3, Upgrade.MOAT, 0, 0, 0, 0, 0, 0);

    private final Upgrade upgrade;
    private final String image;
    private final String name;
    private final Stats delta;

    private Style(String name, int index, Upgrade upgrade, int... delta) {
      this.upgrade = upgrade;
      this.image = upgrade.getPrefix() + index + ".png";
      this.name = upgrade + " " + name;
      imageToStyle.put(this.image, this);
      this.delta = new Stats(delta);
    }

    public Stats getDelta() {
      return this.delta;
    }

    @Override
    public String toString() {
      return this.name;
    }

    public void apply() {
      currentStyles.put(this.upgrade, this);
    }
  }

  // *** Stat tracking

  // Everyone's stats before style deltas are applied.
  private static final Stats BASELINE = new Stats(100, 90, 110, 100, 110, 110);

  // Each preparation option (choices 1317 and 1318, plus the three cheesy
  // options in 1317) adds a fixed delta to your stats. Cheese Seeking options
  // do not change stats.
  private static final Map<String, Stats> prepDeltas = new HashMap<>();

  static {
    // A Hello to Arms
    prepDeltas.put("Conscript the soldiers", new Stats(5, 0, 0, 0, 0, 0));
    prepDeltas.put("Pick up the boulders", new Stats(0, 0, 5, 0, 0, 0));
    prepDeltas.put("Commission some art", new Stats(0, 0, 0, 0, 5, 0));
    prepDeltas.put("Draft those artists", new Stats(10, 0, 0, 0, 0, -5));
    prepDeltas.put("Widen the arrow slits", new Stats(0, -5, 10, 0, 0, 0));
    prepDeltas.put("Add more windows", new Stats(0, 0, 0, -5, 10, 0));
    prepDeltas.put("Strengthen the walls", new Stats(10, 0, 10, 0, -10, 0));
    prepDeltas.put("Build the memorial", new Stats(10, 0, -10, 0, 10, 0));
    prepDeltas.put("Improve the keep", new Stats(-10, 0, 10, 0, 10, 0));
    prepDeltas.put("Approve the retrofit", new Stats(5, 0, 5, 0, 5, 0));
    prepDeltas.put("Get sloppy", new Stats(10, -5, 10, -5, 10, -5));
    prepDeltas.put("Adopt the radical combat style", new Stats(15, -15, 15, -15, 15, -15));
    prepDeltas.put("Levy the tax", new Stats(10, 0, 0, -10, 0, 0));
    prepDeltas.put("Let the citizens hurl cheese at you", new Stats(0, 0, 10, 0, 0, -10));
    prepDeltas.put("Trade soldiers for cheese", new Stats(0, -10, 0, 0, 10, 0));
    // Defensive Posturing
    prepDeltas.put("Train the soldiers", new Stats(0, 5, 0, 0, 0, 0));
    prepDeltas.put("Thicken the walls", new Stats(0, 0, 0, 5, 0, 0));
    prepDeltas.put("Add more murals", new Stats(0, 0, 0, 0, 0, 5));
    prepDeltas.put("Convert the galleries", new Stats(0, 10, 0, 0, -5, 0));
    prepDeltas.put("Make the soldiers masons", new Stats(-5, 0, 0, 10, 0, 0));
    prepDeltas.put("Build the weird statue", new Stats(0, 0, -5, 0, 0, 10));
    prepDeltas.put("Repurpose the statues", new Stats(0, 10, 0, 10, 0, -10));
    prepDeltas.put("Lower the walls", new Stats(0, 10, 0, -10, 0, 10));
    prepDeltas.put("Cut military spending", new Stats(0, -10, 0, 10, 0, 10));
    prepDeltas.put("Throw the party", new Stats(0, 5, 0, 5, 0, 5));
    prepDeltas.put("Blunt everything", new Stats(-5, 10, -5, 10, -5, 10));
    prepDeltas.put("Do the plowshares thing", new Stats(-15, 15, -15, 15, -15, 15));
  }

  private static Stats stylesToStats(Collection<Style> styles) {
    Stats stats = BASELINE.copy();
    for (Style style : styles) {
      stats.add(style.getDelta());
    }
    return stats;
  }

  // Each needle moves one pixel per 7.5 points of its stat.
  private static int statToNeedle(Stat stat, int value) {
    return switch (stat) {
      case MA, CA, PA -> 124 + (int) Math.floor((value - 95) / 7.5);
      case MD, CD, PD -> 240 + (int) Math.floor((value - 87.5) / 7.5);
    };
  }

  // *** Cached state. Resets when you visit the Bastille Battalion control rig

  private static final Map<Upgrade, Style> currentStyles = new TreeMap<>();

  // *** Cheese

  // When you are in choice 1314 - Bastille Battalion (Master of None) - you
  // can focus on offense or defense, or choose to seek cheese.
  //
  // If you select Cheese Seeking Behavior (choice 1319), you will be presented
  // with 3 different options out of a pool of 16 possibilities. You can take
  // each option only once per game. Since you have 2 rounds of preparation and
  // up to 5 castles per game, if you do nothing except look for cheese, your
  // first prep round will offer 3 out of 16, the second, 3 out of 15, until
  // the 10th, which will offer 3 out of 7 options.
  //
  // The Wishing Well is useless if it occurs on the very first turn, since you
  // will not have the 10 cheese required to activate it. If you skip it, like
  // all untaken options, it may be offered again later in the same game.
  //
  // The 16 possible Cheese Seeking encounters include these:
  //
  // 2 that scale based on (higher or lower) Military Attack
  // 2 that scale based on (higher or lower) Military Defense
  // 2 that scale based on (higher or lower) Castle Attack
  // 2 that scale based on (higher or lower) Castle Defense
  // 2 that scale based on (higher or lower) Psychological Attack
  // 2 that scale based on (higher or lower) Psychological Defense
  //
  // 3 that are not affected by a stat
  //
  // The Wishing Well is not affected by a stat, but either gives you no cheese
  // (2/3 chance) or about 300 cheese (1/3 chance).
  //
  // Other sources of cheese:
  //
  // Three encounters when you are focusing on offense
  // Winning a battle. The harder the battle, the more cheese.

  // *** Stances

  // When you enter battle with a castle, you have three choices:
  //
  // Try to get the jump on them
  // Bide your time
  // Ready your defenses and wait for them.
  //
  // In a battle, either (all of) your Attack stats are compared to your foe's
  // Defense stats, or vice versa.
  //
  // With an "offensive" stance (80% of the time):
  // You charge toward your enemy.
  //
  // With a "defensive" stance (20% of the time):
  // You squat and wait for the attack, but it never comes. You sigh, uproot yourself, and attack
  // them.

  // *** Results

  // Your Stance indicates your desire to attack vs. defend, but it's not
  // entirely up to you.  Even if you charge in, your foe may attack
  // first. Even if you try to defend, you may end up attacking first.
  //
  // The aggressor's attacks are compared against the defender's defense.
  // You can win from 0 to 3 of these comparisons.
  // If you win 2 or 3, you win the battle and loot some cheese.
  // If you win 0 or 1, the game is over.

  public static class Results {
    // If you are the aggressor, it is your attack vs. their defense
    // If they are the aggressor, it is their attack vs. your defense
    private final boolean aggressor;

    // True if your (stat) beat their (stat)
    private final boolean military;
    private final boolean castle;
    private final boolean psychological;

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
      int wins = 0;
      wins += this.military ? 1 : 0;
      wins += this.castle ? 1 : 0;
      wins += this.psychological ? 1 : 0;
      return wins >= 2;
    }
  }

  static {
    // This forces the enums to be initialized, which will populate
    // the various sets and maps initialized in the constructors.
    Style[] styles = Style.values();
    Castle[] castles = Castle.values();
  }

  private static final Pattern STAT_PATTERN = Pattern.compile("([MCP][AD])=(-?\\d+)");

  private static Stats loadStats() {
    Stats stats = new Stats();
    Matcher matcher = STAT_PATTERN.matcher(Preferences.getString("_bastilleStats"));
    while (matcher.find()) {
      stats.set(Stat.valueOf(matcher.group(1)), StringUtilities.parseInt(matcher.group(2)));
    }
    return stats;
  }

  public static String generateStatSetting(Stats stats) {
    String value =
        Arrays.stream(Stat.values())
            .map(stat -> stat.name() + "=" + stats.get(stat))
            .collect(Collectors.joining(","));
    return value;
  }

  private static void saveStats(Stats stats) {
    String value = generateStatSetting(stats);
    Preferences.setString("_bastilleStats", value);
  }

  private static void saveStyles(Map<Upgrade, Style> styleMap) {
    String value = styleMap.values().stream().map(Style::name).collect(Collectors.joining(","));
    Preferences.setString("_bastilleCurrentStyles", value);
  }

  public static void reset() {
    // Cached configuration
    currentStyles.clear();

    // You can play up to five games a day
    Preferences.setInteger("_bastilleGames", 0);

    // When you initially visit the control rig, you can select the "style" of
    // the four available upgrades: barbican, drawbridge, murder holes, moat.
    // You can fiddle with them to your heart's content until you start your
    // first game of the day. At that point they are locked in and you will
    // receive the appropriate prizes at the end of that game, win or lose.
    Preferences.setString("_bastilleCurrentStyles", "");

    // Each configured style grants a specific bonus to the set of stats.  That
    // is locked in once you start your first game. As you progress through the
    // game, you perform actions to add or subtract to specific (or all) attack
    // or defense stats. Stats revert to BASELINE plus your style deltas at the
    // start of each game.
    Preferences.setString("_bastilleStats", "");

    // Game progress settings.

    // Two turns of offense/defense/cheese following by a castle battle.
    // The game ends when you lose or beat your fifth castle
    Preferences.setInteger("_bastilleGameTurn", 0);
    Preferences.setInteger("_bastilleCheese", 0);

    // The type of castle might influence your training choices.
    Preferences.setString("_bastilleEnemyCastle", "");
    Preferences.setString("_bastilleEnemyName", "");

    // Once you have selected offense/defense/cheese, these are your choice
    // options for that turn.
    Preferences.setString("_bastilleChoice1", "");
    Preferences.setString("_bastilleChoice2", "");
    Preferences.setString("_bastilleChoice3", "");
    Preferences.setString("_bastilleLastEncounter", "");
    Preferences.setString("_bastilleOptionsTaken", "");

    // The rewards for your first game of the day arrive with whichever
    // choice you make on the GAME OVER screen.
    Preferences.setBoolean("_bastilleRewardsCollected", false);

    // Your locked-in score for the day.
    Preferences.setInteger("_bastilleLockedInScore", 0);

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

  public static boolean checkNeedles(String text) {
    boolean retval = true;
    Stats stats = loadStats();
    Matcher matcher = IMAGE_PATTERN.matcher(text);
    while (matcher.find()) {
      if (!matcher.group(4).startsWith("needle")) {
        continue;
      }
      int left = StringUtilities.parseInt(matcher.group(3));
      Stat stat =
          switch (StringUtilities.parseInt(matcher.group(2))) {
            case 233 -> left < 200 ? Stat.MA : Stat.MD;
            case 252 -> left < 200 ? Stat.CA : Stat.CD;
            case 270 -> left < 200 ? Stat.PA : Stat.PD;
            default -> null;
          };
      if (stat == null) {
        continue;
      }
      int value = stats.get(stat);
      int expected = statToNeedle(stat, value);
      if (expected != left) {
        logLine(
            stat
                + " is tracked as "
                + value
                + " (needle at "
                + expected
                + ") but the needle is at "
                + left);
        retval = false;
      }
    }
    return retval;
  }

  public static void parseStyles(String text) {
    Matcher matcher = IMAGE_PATTERN.matcher(text);
    while (matcher.find()) {
      Style style = imageToStyle.get(matcher.group(4));
      if (style != null) {
        style.apply();
      }
    }
    saveStyles(currentStyles);
    saveStats(stylesToStats(currentStyles.values()));
    checkNeedles(text);
  }

  private static void applyPrepOption(String option, String text) {
    Stats delta = prepDeltas.get(option);
    if (delta != null) {
      saveStats(loadStats().add(delta));
    }
    checkNeedles(text);
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
    Preferences.setString("_bastilleEnemyCastle", castle.getPrefix());
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
        case "Military" -> military = won;
        case "Castle" -> castle = won;
        case "Psychological" -> psychological = won;
      }
    }

    Results results = new Results(aggressor, military, castle, psychological);
    logLine(results.won() ? "You won!" : "You lost.");
    return results;
  }

  // *** Game control flow

  private static void startGame() {
    Preferences.setInteger("_bastilleCheese", 0);
    Preferences.setString("_bastilleOptionsTaken", "");
    clearChoices();
  }

  private static void nextTurn() {
    Preferences.increment("_bastilleGameTurn", 1, 15);
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

  private static final Pattern TOTAL_CHEESE_PATTERN =
      Pattern.compile("You survived for (\\d+) turns and collected ([\\d,]+) cheese");

  private static void endBattle(String text) {
    Results results = logBattle(text);
    boolean won = results.won();
    Preferences.setBoolean("_bastilleLastBattleWon", won);
    Preferences.setString("_bastilleLastBattleResults", results.getValue());

    Preferences.setString("_bastilleEnemyCastle", "");
    Preferences.setString("_bastilleEnemyName", "");

    // Win or lose, this might be the final battle of the game.
    if (text.contains("GAME OVER")) {
      Matcher matcher = TOTAL_CHEESE_PATTERN.matcher(text);
      if (matcher.find()) {
        logLine(matcher.group(0));
        int calculated = Preferences.getInteger("_bastilleCheese");
        int total = StringUtilities.parseInt(matcher.group(2));
        // Sanity check
        if (calculated != total) {
          System.out.println("Calculated = " + calculated + " total = " + total);
          Preferences.setInteger("_bastilleCheese", total);
        }
      }
    }
  }

  // You can play <b>4</b> more times today.
  private static final Pattern GAMES_LEFT_PATTERN =
      Pattern.compile("You can play <b>(\\d+)</b> more time");

  private static void parseGamesLeft(String text) {
    Matcher matcher = GAMES_LEFT_PATTERN.matcher(text);
    if (matcher.find()) {
      Preferences.setInteger("_bastilleGames", 5 - StringUtilities.parseInt(matcher.group(1)));
    }
  }

  private static void endGame() {
    Preferences.increment("_bastilleGames", 1, 5);
    Preferences.setInteger("_bastilleGameTurn", 0);
  }

  private static void addOptionTaken(String option) {
    if (option.isEmpty()) {
      return;
    }
    Preferences.setString(
        "_bastilleOptionsTaken", taken -> taken.isEmpty() ? option : taken + "," + option);
  }

  // *** Logging

  private static void logLine(String message) {
    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);
  }

  private static void logBoosts() {
    logBoost(EffectPool.SHARK_TOOTH_GRIN, "Military");
    logBoost(EffectPool.BOILING_DETERMINATION, "Castle");
    logBoost(EffectPool.ENHANCED_INTERROGATION, "Psychological");
  }

  private static void logBoost(int effectId, String stats) {
    var effect = EffectPool.get(effectId);
    var turns = effect.getCount(KoLConstants.activeEffects);
    if (turns == 0) {
      return;
    }
    logLine(
        "("
            + stats
            + " attack and defense boosted by "
            + Math.min(turns, 3) * 10
            + "% from "
            + effect.getName()
            + ")");
  }

  private static void logStrength() {
    logLine(loadStats().toStrengthString());
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
    return loadStats().get(stat);
  }

  // *** Interface for AdventureRequest.parseChoiceEncounter

  private static final Pattern CHEESE_PATTERN = Pattern.compile("You gain (\\d+) cheese!");

  public static int gainCheese(final String text) {
    Matcher matcher = CHEESE_PATTERN.matcher(text);
    int cheese = 0;
    if (matcher.find()) {
      logLine(matcher.group(0));
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
      case 1316: // GAME OVER
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
      if (ChoiceManager.lastChoice == 1313) {
        parseStyles(text);
      } else {
        checkNeedles(text);
      }
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
        parseGamesLeft(text);
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
          if (ChoiceUtilities.extractChoice(text) != 1314) {
            return;
          }
          logLine("Starting game #" + (Preferences.getInteger("_bastilleGames") + 1));
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
        switch (ChoiceUtilities.extractChoice(text)) {
          case 1314:
            // We won and it wasn't the last battle.
            parseCastle(text);
            logStrength();
            break;
          case 1316:
            // We lost or it was the last battle
            endGame();
            break;
        }
        return;

      case 1316: // GAME OVER
        if (text.contains("you Lock it in!")) {
          Preferences.setInteger(
              "_bastilleLockedInScore", Math.max(1, Preferences.getInteger("_bastilleCheese")));
        }
        if (text.contains("You grab your rewards, for your first play of the day!")) {
          Preferences.setBoolean("_bastilleRewardsCollected", true);
        }
        return;

      case 1317: // A Hello to Arms (Battalion)
      case 1318: // Defensive Posturing
      case 1319: // Cheese Seeking Behavior
        if (!parseTurn(text)) {
          nextTurn();
        }
        String option = Preferences.getString("_bastilleLastEncounter");
        addOptionTaken(option);
        applyPrepOption(option, text);
        logStrength();
        return;
    }
  }

  public static final boolean registerRequest(final String urlString) {
    int choice = ChoiceUtilities.extractChoiceFromURL(urlString);
    int decision = ChoiceUtilities.extractOptionFromURL(urlString);
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
            return true;
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
          case 1 -> logAction(buf, "Improving offense.");
          case 2 -> logAction(buf, "Focusing on defense.");
          case 3 -> logAction(buf, "Looking for cheese.");
        }
        break;
      case 1315: // Castle vs. Castle
        switch (decision) {
          case 1 -> logAction(buf, "Charge!");
          case 2 -> logAction(buf, "Watch warily.");
          case 3 -> logAction(buf, "Wait to be attacked.");
        }
        break;
      case 1316: // GAME OVER
        break;
      case 1317: // A Hello to Arms (Battalion)
      case 1318: // Defensive Posturing
      case 1319: // Cheese Seeking Behavior
        String encounter = Preferences.getString("_bastilleChoice" + decision);
        Preferences.setString("_bastilleLastEncounter", encounter);
        buf.append(encounter);
        break;
    }

    if (buf.length() > 0) {
      logLine(buf.toString());
    }

    return true;
  }
}
