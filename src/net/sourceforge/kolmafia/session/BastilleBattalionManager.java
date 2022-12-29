package net.sourceforge.kolmafia.session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;
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
  // persist until rollover, in other scripts wish to analyze them.
  //
  // If the user opts in (via property), the results of all battles and all
  // cheese acquisitions are recorded in files in the "data" directory for
  // later analysis by ASH programs.

  // *** Ongoing research:
  //
  // Stats:
  //
  // - We know stat bonuses offered by style sets as displayed by
  //   "needles". Is it possible to determine actual stat values?
  //   Maybe: 12 cheese encounters scale (positively or negatively) according
  //   to one of the 6 stats. We collect data based on stat bonuses - and the
  //   yield is linear, with randomizing fuzz. Look at the x-intercepts?
  // - Is there a randomizing factor in player stats per game? One hopes not.
  // - How do the potions affect your stats during battles? They do not
  //   register on the "needles" as stat bonuses, and they do not seem to
  //   affect cheese yields that scale by stat. But you can definitely win
  //   against tougher castles if you have them in effect than if you do not.
  // - Do multiple turns of a potion effect stack?
  //
  // Castles:
  //
  // - What are initial stats for the six castles?
  // - Do they have one superior stat, or does each have a superior offense AND
  //   a superior defense?
  // - Is there a randomizing factor per game or battle?
  // - How do they scale as fight # increases?
  //
  // Battles:
  //
  // - It "feels" like you sometimes just can't win against what are normally
  //   easy opponents. Can we use statistics to confirm or deny the "feeling"?
  //   If there is a randomizing factor, which has fuzz? Player stats or castle
  //   stats? KoL "monsters" have such for attack/defense - not the player.
  //
  // Cheese:
  //
  // - What are the linear formulae for the 12 stat-scaling cheese encounters?

  // *** Solved research:
  //
  // Castles:
  //
  // - Each castle type has at least one "superior" stat.
  //   barracks - MA
  //   berserker - PA
  //   bigcastle - CA
  //   frenchcastle - PD
  //   masterofnone - MD
  //   shieldmaster - CD
  // - In battle results, the player or castle stat is always "higher" or
  //   "lower" than the other. I have much data where the highest stat that
  //   results in a "loss" also sometimes results in a "win". Conclusion: Equal
  //   stats have a 50% chance of resulting in a player win or a castle win.
  //
  // Cheese:
  //
  // - The yields of the 3 non-scaling encounters (20, 50, 100)
  // - The yields from defeated castles (45 * castle level)
  // - The wishing well succeeds 1/3 of the time with a yield of 300.
  // - Potions that affect stats do not affect stat-scaling yields
  //
  // Stances:
  //
  // - offensive is 80% aggressor, 20% defender
  // - waiting is 50% aggressor, 50% defender
  // - defensive is 20% aggressor, 80% defender

  private BastilleBattalionManager() {}

  // *** Stats

  // We don't know what your (internal to KoL) stats start at,
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
  // can be used to determine the current level of boostage.
  //
  // (Ezandora's relay script displays that pixel value as the value of your
  // stats. That's pretty funny; they do show how your stats compare to each
  // other, but I am sure the stats are not internally measured in pixels.)

  private static Map<String, Stat> enumNameToStat = new HashMap<>();

  public enum Stat {
    NONE("None", -1),
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
      return stat == Stat.NONE ? 0 : stats[stat.getIndex()];
    }

    public void set(Stat stat, int value) {
      if (stat != Stat.NONE) {
        stats[stat.getIndex()] = value;
      }
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

  // *** Boosts

  // Three potions give you 1 turn of increased (by some amount) attack and
  // defense for one of military, castle, or psychological.

  private static AdventureResult SHARK_TOOTH_GRIN = EffectPool.get(EffectPool.SHARK_TOOTH_GRIN);
  private static AdventureResult BOILING_DETERMINATION =
      EffectPool.get(EffectPool.BOILING_DETERMINATION);
  private static AdventureResult ENHANCED_INTERROGATION =
      EffectPool.get(EffectPool.ENHANCED_INTERROGATION);

  public static class Boosts {
    private final String boosts;
    private final AdventureResult military;
    private final AdventureResult castle;
    private final AdventureResult psychological;

    public Boosts() {
      this.military = Boosts.getBoost(SHARK_TOOTH_GRIN);
      this.castle = Boosts.getBoost(BOILING_DETERMINATION);
      this.psychological = Boosts.getBoost(ENHANCED_INTERROGATION);
      this.boosts = this.makeBoostString();
    }

    private static AdventureResult getBoost(AdventureResult effect) {
      int index = KoLConstants.activeEffects.indexOf(effect);
      return index >= 0 ? KoLConstants.activeEffects.get(index) : null;
    }

    private String makeBoostString() {
      StringBuilder buf = new StringBuilder();
      if (this.military != null) {
        int count = this.military.getCount();
        if (count > 1) {
          buf.append(String.valueOf(count));
        }
        buf.append('M');
      }
      if (this.castle != null) {
        int count = this.castle.getCount();
        if (count > 1) {
          buf.append(String.valueOf(count));
        }
        buf.append('C');
      }
      if (this.psychological != null) {
        int count = this.psychological.getCount();
        if (count > 1) {
          buf.append(String.valueOf(count));
        }
        buf.append('P');
      }
      return buf.toString();
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

    public boolean boosted(Stat stat) {
      return switch (stat) {
        case MA, MD -> this.military != null;
        case CA, CD -> this.castle != null;
        case PA, PD -> this.psychological != null;
        default -> false;
      };
    }

    public int boostedBy(Stat stat) {
      return switch (stat) {
        case MA, MD -> this.military == null ? 0 : this.military.getCount();
        case CA, CD -> this.castle == null ? 0 : this.castle.getCount();
        case PA, PD -> this.psychological == null ? 0 : this.psychological.getCount();
        default -> 0;
      };
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
  // Analysis of more than 4,000 battles shows that each castle type is
  // superior in one the six stats:
  //
  //   Military Fortress (barracks) - higher MA
  //   Imposing Citadel (berserker) - higher PA
  //   Sprawling Chateau (bigcastle) - higher CA
  //   Avant-Garde (frenchcastle) - higher PD
  //   Generic (masterofnone) - higher MD
  //   Fortified Stronghold (shieldmaster) - higher CD
  //
  // This stat starts out higher and improves faster in harder castles than the
  // other stats do.

  private static Map<String, Castle> imageToCastle = new HashMap<>();
  private static Map<String, Castle> descriptionToCastle = new HashMap<>();

  public static enum Castle {
    ART("frenchcastle", "an avant-garde art castle", Stat.PD),
    BORING("masterofnone", "a boring, run-of-the-mill castle", Stat.MD),
    CHATEAU("bigcastle", "a sprawling chateau", Stat.CA),
    CITADEL("berserker", "a dark and menacing citadel", Stat.PA),
    FORTIFIED("shieldmaster", "a fortress that puts the 'fort' in 'fortified'", Stat.CD),
    MILITARY("barracks", "an imposing military fortress", Stat.MA);

    String prefix;
    String description;
    Stat stat;

    private Castle(String prefix, String description, Stat stat) {
      this.prefix = prefix;
      this.description = description;
      this.stat = stat;
      descriptionToCastle.put(description, this);
      imageToCastle.put(prefix + "_1.png", this);
      imageToCastle.put(prefix + "_2.png", this);
      imageToCastle.put(prefix + "_3.png", this);
    }

    public String getPrefix() {
      return this.prefix;
    }

    public Stat getStat() {
      return this.stat;
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
    final int digitIndex;
    final int scale;

    private Upgrade(int option, String name, String prefix) {
      this.name = name;
      this.prefix = prefix;
      this.option = option;
      optionToUpgrade.put(this.option, this);
      this.digitIndex = 4 - option;
      this.scale = (int) Math.pow(3, this.digitIndex);
    }

    public String getPrefix() {
      return this.prefix;
    }

    public int getDigitIndex() {
      return this.digitIndex;
    }

    public int getScale() {
      return this.scale;
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
    BARBECUE("Barbarian Barbecue", 1, Upgrade.BARBICAN),
    BABAR("Babar", 2, Upgrade.BARBICAN),
    BARBERSHOP("Barbershop", 3, Upgrade.BARBICAN),

    BRUTALIST("Brutalist", 1, Upgrade.DRAWBRIDGE),
    DRAFTSMAN("Draftsman", 2, Upgrade.DRAWBRIDGE),
    NOUVEAU("Art Nouveau", 3, Upgrade.DRAWBRIDGE),

    CANNON("Cannon", 1, Upgrade.MURDER_HOLES),
    CATAPULT("Catapult", 2, Upgrade.MURDER_HOLES),
    GESTURE("Gesture", 3, Upgrade.MURDER_HOLES),

    SHARKS("Sharks", 1, Upgrade.MOAT),
    LAVA("Lava", 2, Upgrade.MOAT),
    TRUTH("Truth Serum", 3, Upgrade.MOAT);

    private final Upgrade upgrade;
    private final String image;
    private final String name;
    private final int scaledDigit;

    private Style(String name, int index, Upgrade upgrade) {
      this.upgrade = upgrade;
      this.image = upgrade.getPrefix() + index + ".png";
      this.name = upgrade + " " + name;
      imageToStyle.put(this.image, this);
      this.scaledDigit = (index - 1) * upgrade.getScale();
    }

    public Upgrade getUpgrade() {
      return this.upgrade;
    }

    public int getScaledDigit() {
      return this.scaledDigit;
    }

    @Override
    public String toString() {
      return this.name;
    }

    public void apply() {
      currentStyles.put(this.upgrade, this);
    }
  }

  // *** Style Sets

  // I experimented a lot transitioning between one upgrade and another and observing
  // how my stat bonuses changed.
  //
  // It turns out that those values are not independent; the same upgrade swap
  // might grant +1 or +2 Castle Attack, say, depending on which other upgrades
  // are in place.
  //
  // There are three Styles for each of four Upgrades, so there are a total of 81 = (3 ^ 4)
  // configurations.
  //
  // We'll number them from 0 - 80 (0000 - 2222, in base 3)

  public static Style[] styleSetToArray(Collection<Style> styleSet) {
    return styleSet.toArray(new Style[4]);
  }

  public static int styleSetToKey(Collection<Style> styleSet) {
    return stylesToKey(styleSetToArray(styleSet));
  }

  public static int stylesToKey(Style... styles) {
    assert styles.length == 4;
    return Arrays.stream(styles).mapToInt(Style::getScaledDigit).sum();
  }

  public static Collection<Style> keyToStyleSet(int key) {
    // Base 3 digits: [barbican][drawbridge][murder holes][moat]
    int[] digits = {key % 3, (key / 3) % 3 * 3, (key / 9) % 3 * 9, (key / 27) % 3 * 27};

    Set<Style> styleSet = EnumSet.noneOf(Style.class);
    for (Style style : Style.values()) {
      int digit = style.getUpgrade().getDigitIndex();
      if (digits[digit] == style.getScaledDigit()) {
        styleSet.add(style);
      }
    }

    return styleSet;
  }

  // ***  Data File: Style Set -> Stats

  private static final Map<Integer, Stats> styleSetToStats = new TreeMap<>();

  private static final String BASTILLE_FILE_NAME = "bastille.txt";
  private static final int BASTILLE_FILE_VERSION = 1;

  // Load data file

  private static void readStyleSets() {
    styleSetToStats.clear();

    try (BufferedReader reader =
        FileUtilities.getVersionedReader(BASTILLE_FILE_NAME, BASTILLE_FILE_VERSION)) {
      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length != 11) {
          continue;
        }

        int key = StringUtilities.parseInt(data[0]) - 1;

        if (styleSetToStats.containsKey(key)) {
          // Should be impossible.
          continue;
        }

        // Ignore the style names; they are for humans to read
        // String styleName1 = data[1];
        // String styleName2 = data[2];
        // String styleName3 = data[3];
        // String styleName4 = data[4];

        Collection<Style> styleSet = keyToStyleSet(key);

        int MA = StringUtilities.parseInt(data[5]);
        int MD = StringUtilities.parseInt(data[6]);
        int CA = StringUtilities.parseInt(data[7]);
        int CD = StringUtilities.parseInt(data[8]);
        int PA = StringUtilities.parseInt(data[9]);
        int PD = StringUtilities.parseInt(data[10]);

        Stats stats = new Stats(MA, MD, CA, CD, PA, PD);

        styleSetToStats.put(key, stats);
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  // Write data file: for testing or for generating the first time.

  private static String generateStyleSetFields(int key) {
    Collection<Style> styleSet = keyToStyleSet(key);
    Style[] styles = styleSetToArray(styleSet);
    assert styles.length == 4;
    Stats stats = styleSetToStats.get(key);
    return joinFields(
        "\t",
        String.valueOf(key + 1),
        styles[0].name(),
        styles[1].name(),
        styles[2].name(),
        styles[3].name(),
        String.valueOf(stats.get(Stat.MA)),
        String.valueOf(stats.get(Stat.MD)),
        String.valueOf(stats.get(Stat.CA)),
        String.valueOf(stats.get(Stat.CD)),
        String.valueOf(stats.get(Stat.PA)),
        String.valueOf(stats.get(Stat.PD)));
  }

  public static void saveStyleSets() {
    String path = KoLConstants.DATA_DIRECTORY + BASTILLE_FILE_NAME;
    try (PrintStream stream = LogStream.openStream(path, true)) {
      stream.println(String.valueOf(BASTILLE_FILE_VERSION));
      for (int key = 0; key < 81; key++) {
        String line = generateStyleSetFields(key);
        stream.println(line);
      }
    }
  }

  static {
    readStyleSets();
    assert styleSetToStats.size() == 81;
  }

  // *** Cached state. Resets when you visit the Bastille Battalion control rig

  private static final Map<Upgrade, Style> currentStyles = new TreeMap<>();
  private static Stats currentStats = new Stats();
  private static Castle currentCastle = null;
  private static Battle currentBattle = null;

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

  public static final Map<String, CheeseEncounter> cheeseEncounters = new HashMap<>();

  public static class CheeseEncounter {
    public final String name;
    public final Stat stat;
    public final boolean positive;

    public CheeseEncounter(String name, Stat stat, boolean positive) {
      this.name = name;
      this.stat = stat;
      this.positive = positive;
      cheeseEncounters.put(name, this);
    }

    public CheeseEncounter(String name) {
      this(name, Stat.NONE, true);
    }

    public CheeseEncounter(Castle castle) {
      this(castle.getPrefix(), Stat.NONE, true);
    }
  }

  static {
    // Cheese Seeking Behavior
    new CheeseEncounter("Raid the cave", Stat.MA, true);
    new CheeseEncounter("Enter the Weakest Army competition", Stat.MA, false);
    new CheeseEncounter("Convert the barracks", Stat.MD, true);
    new CheeseEncounter("Let the cheese horse in", Stat.MD, false);
    new CheeseEncounter("Shoot the glacier", Stat.CA, true);
    new CheeseEncounter("Submit embarrassing catapult photos", Stat.CA, false);
    new CheeseEncounter("Try the wall thing", Stat.CD, true);
    new CheeseEncounter("Stand in the waterfall", Stat.CD, false);
    new CheeseEncounter("Rob the suburb", Stat.PA, true);
    new CheeseEncounter("Enter the childrens' art contest", Stat.PA, false);
    new CheeseEncounter("Have the cheese contest", Stat.PD, true);
    new CheeseEncounter("Put on the bad art show", Stat.PD, false);
    new CheeseEncounter("Grab the boulder");
    new CheeseEncounter("Scrape out the mine");
    new CheeseEncounter("Raid the cart");
    new CheeseEncounter("Use the wishing well");
    // A Hello to Arms
    new CheeseEncounter("Levy the tax");
    new CheeseEncounter("Let the citizens hurl cheese at you");
    new CheeseEncounter("Trade soldiers for cheese");
  }

  private static final CheeseEncounter UNKNOWN_ENCOUNTER = new CheeseEncounter("UNKNOWN");

  public static class Cheese {
    public final int turn;
    public final int cheese;
    public final CheeseEncounter encounter;

    // Derived
    public final Stat stat;
    public final int statBonus;
    public final int potion;

    // This is constructed when we have collected cheese.
    public Cheese(int turn, String encounterName, int cheese) {
      this.turn = turn;
      this.cheese = cheese;
      this.encounter = cheeseEncounters.getOrDefault(encounterName, UNKNOWN_ENCOUNTER);
      this.stat = this.encounter.stat;
      this.statBonus = getCurrentStat(this.stat);
      this.potion = (stat == Stat.NONE) ? 0 : new Boosts().boostedBy(this.stat);
    }

    // This is constructed when we have defeated another castle
    public Cheese(int turn, Castle castle, int cheese) {
      this.turn = turn;
      this.cheese = cheese;
      this.encounter = new CheeseEncounter(castle);
      this.stat = Stat.NONE;
      this.statBonus = turn / 3;
      this.potion = 0;
    }
  }

  // *** Battle

  // One of the reasons I started this project was to collect data that could
  // be analyzed to understand how to do well at this game. The improved
  // logging makes the game play much more enjoyable, but the collected data
  // makes automated data analysis possible without manual data entry.
  //
  // Observations so far:
  //
  // Each kind of castle has particular strengths and weaknesses. There are six
  // kinds of castle. Each appears to be stronger in one of the six stats.
  //
  // A "game" has 5 rounds. Your foes increase in power depending on which
  // round you encounter them.  For example, if I attack castle type A on round
  // one, my attack vs. his defense may be 3:0, but on rounds 2 - 5, attack
  // vs. defense may decrease to 2:1, 1:2, and eventually 0:3. Your cheese
  // reward for beating a foe go up correspondingly to the difficulty.
  //
  // Depending on your stat configuration, you will have to depend on offense
  // or defense (i.e., which stance you select) to even have a chance against
  // higher difficulty castles. For a given stat configuration, some castles
  // will be unbeatable at higher levels - and to get the highest scores, you
  // have to be lucky enough to get one of the castles you CAN beat.

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
  // Observations collected from 4308 battles as of 2022/06/20:
  //
  // "offensive" stance (901): 80% aggressor/20% defender
  // "waiting" stance (1586): 50% aggressor/50% defender
  // "defensive" stance (1821): 20% aggressor/80% defender
  //
  // With an "offensive" stance (80% of the time):
  // You charge toward your enemy.
  //
  // With a "defensive" stance (20% of the time):
  // You squat and wait for the attack, but it never comes. You sigh, uproot yourself, and attack
  // them.

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

    public Cheese toCheese() {
      return new Cheese(this.number * 3, this.enemy, this.cheese);
    }
  }

  static {
    // This forces the enums to be initialized, which will populate
    // the various sets and maps initialized in the constructors.
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

  public static String generateStatSetting(Stats stats) {
    String value =
        Arrays.stream(Stat.values())
            .filter(stat -> stat != Stat.NONE)
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
    Preferences.setString("_bastilleLastEncounter", "");

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

  public static final Stats PIXELS = new Stats(124, 240, 124, 240, 124, 240);

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
      }
    }
    saveStyles(currentStyles);
    saveStats(currentStats);
  }

  public static void parseNeedles(String text) {
    currentStats.clear();
    Matcher matcher = IMAGE_PATTERN.matcher(text);
    while (matcher.find()) {
      String image = matcher.group(4);
      if (!image.startsWith("needle")) {
        continue;
      }
      parseNeedle(matcher.group(2), matcher.group(3));
    }
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

  private static final Pattern TOTAL_CHEESE_PATTERN =
      Pattern.compile("You survived for (\\d+) turns and collected ([\\d,]+) cheese");

  private static void endBattle(String text) {
    Results results = logBattle(text);
    boolean won = results.won();
    Preferences.setBoolean("_bastilleLastBattleWon", won);
    Preferences.setString("_bastilleLastBattleResults", results.getValue());

    // Win or lose, this might be the final battle of the game.
    if (text.contains("GAME OVER")) {
      Matcher matcher = TOTAL_CHEESE_PATTERN.matcher(text);
      int cheese = 0;
      if (matcher.find()) {
        String message = matcher.group(0);
        RequestLogger.printLine(message);
        RequestLogger.updateSessionLog(message);
        int calculated = Preferences.getInteger("_bastilleCheese");
        int total = StringUtilities.parseInt(matcher.group(2));
        // Sanity check
        if (calculated != total) {
          System.out.println("Calculated = " + calculated + " total = " + total);
          Preferences.setInteger("_bastilleCheese", total);
        }
      }
    }

    if (currentBattle != null) {
      currentBattle.setResults(results);
      if (won) {
        currentBattle.setCheese(Preferences.getInteger("_bastilleLastCheese"));
        saveCheese(currentBattle.toCheese());
      }
      saveBattle(currentBattle);
    }
  }

  private static void collectCheese(String text) {
    String encounterName = Preferences.getString("_bastilleLastEncounter");
    int curds = Preferences.getInteger("_bastilleLastCheese");
    if (curds == 0) {
      // KoL won't let you use the wishing well if you don't have at least 10
      // cheese. If you have that much, it says "You toss 10 cheese into the
      // wishing well" - but it doesn't really deduct any cheese.
      //
      // Attempting to use the wishing well when you don't have cheese is a
      // user error, not a result that we want to include in the statistics.
      if (!encounterName.equals("Use the wishing well")
          || text.contains("You can't afford to make a wish, so you move on.")) {
        return;
      }
    }
    int turn = Preferences.getInteger("_bastilleGameTurn");
    Cheese cheese = new Cheese(turn, encounterName, curds);
    saveCheese(cheese);
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

  public static boolean checkPredictions() {
    return checkPredictions(getPredictedStats());
  }

  public static Stats getPredictedStats() {
    return styleSetToStats.get(styleSetToKey(currentStyles.values()));
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
          checkPredictions();
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
        switch (ChoiceUtilities.extractChoice(text)) {
          case 1314:
            // We won and it wasn't the last battle.
            parseCastle(text);
            logStrength();
            break;
          case 1316:
            // We lost or it was the last battle
            endGame(text);
            break;
        }
        return;

      case 1316: // GAME OVER
        return;

      case 1317: // A Hello to Arms (Battalion)
      case 1318: // Defensive Posturing
      case 1319: // Cheese Seeking Behavior
        collectCheese(text);
        if (!parseTurn(text)) {
          nextTurn();
        }
        parseNeedles(text);
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

  // *** Game Logging for Analysis

  // Since I am not (yet) working on a script to automate this, I want battle
  // data to be automatically saved to a file in a format which can be read by
  // an analysis script via file_to_map().
  //
  // This is the format.
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
  //     string enemy;       // {frenchcastle,masterofnone,bigcastle,
  //                         // berserker,shieldmaster,barracks}
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
    return String.join(separator, fields);
  }

  private static String generateKey(int game, int number) {
    return joinFields(
        ".",
        KoLConstants.DAILY_FORMAT.format(new Date()),
        KoLCharacter.getPlayerId(),
        String.valueOf(game),
        String.valueOf(number));
  }

  private static String generateFields(String key, Battle battle) {
    return joinFields(
        "\t",
        key,
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
    String line = generateFields(generateKey(game, number), battle);

    String path = KoLConstants.DATA_DIRECTORY + BATTLE_FILE_NAME;
    try (PrintStream stream = LogStream.openStream(path, false)) {
      stream.println(line);
    }
  }

  // I also want to save cheese data to a file. The amount you collect is often
  // affected by a stat: each of the six stats has one cheese encounter which is
  // better the higher the stat and one which is better the lower the stat.
  //
  // There are three encounters which are fixed (modulo randomness) regardless
  // of stats.
  //
  // There is the Wishing Well which sometimes gives nothing and sometimes
  // gives a lot, regardless of stats.
  //
  // It would be nice to know the formulas for the stat-dependent contests.
  // Ezandora assumes a linear equation:
  //
  //   cheese = A * STAT + B
  //
  // Finally, it would nice to learn the probability of the Wishing Well
  // paying off.
  //
  // This is the proposed format.
  //
  // The file is a tab delimited file in "data" named Bastille.cheese.txt
  //
  // Note that this is not user specific; you and all of your multis will contribute to the results
  //
  // The "key" is DATE.PLAYERID.GAME.ROUND
  //
  // The fields are designed to be easily loaded into an ASH record.
  //
  // 20220409.ID.1.11		11	Raid the cave	MA	6	false	133
  //
  // record CheeseData
  // {
  //     int round;             // I don't think this affects yield, but...
  //     string name;           // Name of the cheese-granting encounter
  //     string stat_name;      // Name of relevant stat
  //     int stat_value;        // Value of relevant stat
  //     int potion;		// Turns of appropriate potions in effect
  //     int cheese;            // How much cheese you looted
  // };
  //
  // CheeseData [string] cheeses;
  // file_to_map( FILENAME, cheeses );

  private static final String CHEESE_FILE_NAME = "Bastille.cheese.txt";

  private static String generateFields(String key, Cheese yield) {
    return joinFields(
        "\t",
        key,
        String.valueOf(yield.turn),
        yield.encounter.name,
        String.valueOf(yield.stat.name()),
        String.valueOf(yield.statBonus),
        String.valueOf(yield.potion),
        String.valueOf(yield.cheese));
  }

  private static void saveCheese(Cheese yield) {
    if (!Preferences.getBoolean("logBastilleBattalionBattles")) {
      return;
    }

    int game = Preferences.getInteger("_bastilleGames") + 1;
    int turn = yield.turn;
    String line = generateFields(generateKey(game, turn), yield);

    String path = KoLConstants.DATA_DIRECTORY + CHEESE_FILE_NAME;
    try (PrintStream stream = LogStream.openStream(path, false)) {
      stream.println(line);
    }
  }
}
