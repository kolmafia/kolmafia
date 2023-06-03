package net.sourceforge.kolmafia.session;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AdventureResult.AdventureLongCountResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.EdServantData;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.DateTimeManager;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ArcadeRequest;
import net.sourceforge.kolmafia.request.BeerPongRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest.Companion;
import net.sourceforge.kolmafia.request.FloristRequest;
import net.sourceforge.kolmafia.request.FloristRequest.Florist;
import net.sourceforge.kolmafia.request.SpaaaceRequest;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.ClanFortuneDecorator;
import net.sourceforge.kolmafia.webui.MemoriesDecorator;

public abstract class ChoiceAdventures {

  public static class Spoilers {
    private final int choice;
    private final String name;
    private final ChoiceOption[] options;

    public Spoilers(final int choice, final String name, final ChoiceOption[] options) {
      this.choice = choice;
      this.name = name;
      this.options = options;
    }

    public int getChoice() {
      return this.choice;
    }

    public String getName() {
      return this.name;
    }

    public ChoiceOption[] getOptions() {
      return this.options;
    }
  }

  // Here are the various maps from choice# -> choice object

  public static final Map<Integer, ChoiceAdventure> choiceToChoiceAdventure = new HashMap<>();
  public static final Map<Integer, ChoiceSpoiler> choiceToChoiceSpoiler = new HashMap<>();
  public static final Map<Integer, ChoiceCost> choiceToChoiceCost = new HashMap<>();

  // Here are various sets used to register error detected during initialization
  private static final Set<Integer> duplicateChoiceAdventures = new TreeSet<>();
  private static final Set<Integer> missingChoiceAdventureOptions = new TreeSet<>();
  private static final Set<Integer> missingChoiceAdventureDefaultProperties = new TreeSet<>();

  private static final Set<Integer> duplicateChoiceSpoilers = new TreeSet<>();
  private static final Set<Integer> duplicateChoiceCosts = new TreeSet<>();

  // These are not errors, per se, but may be worth looking at
  public static final Set<Integer> choiceSpoilersWithDefaults = new TreeSet<>();
  public static final Set<Integer> choiceSpoilersWithoutDefaults = new TreeSet<>();

  public static class Choice implements Comparable<Choice> {
    protected final int choice;
    protected final int ordering;

    public Choice(final int choice, final int ordering) {
      this.choice = choice;
      this.ordering = ordering;
      this.addToMap();
    }

    public Choice(final int choice) {
      this(choice, 0);
    }

    public int getChoice() {
      return this.choice;
    }

    protected void addToMap() {}

    @Override
    public int compareTo(final Choice o) {
      // Choices can have a specified relative ordering
      // within zone regardless of name or choice number
      if (this.ordering != o.ordering) {
        return this.ordering - o.ordering;
      }
      return this.choice - o.choice;
    }
  }

  public static class ChoiceAdventure extends Choice {
    private final String zone;
    private final String name;
    private final ChoiceOption[] options;

    // Derived fields
    protected final String property;
    private final Spoilers spoilers;

    public ChoiceAdventure(
        final int choice, String zone, final String name, final ChoiceOption... options) {
      this(choice, zone, name, 0, options);
    }

    public ChoiceAdventure(
        final int choice,
        final String zone,
        final String name,
        final int ordering,
        ChoiceOption... options) {
      super(choice, ordering);
      this.zone = zone;
      if (!AdventureDatabase.ZONE_DESCRIPTIONS.containsKey(zone)) {
        System.out.println("ChoiceAdventure # " + choice + " has an invalid zone " + zone);
      }

      this.name = name;
      if (options == null) {
        System.out.println("ChoiceAdventure # " + choice + " has no Options configured");
        missingChoiceAdventureOptions.add(choice);
        options = new ChoiceOption[0];
      }
      this.options = options;
      this.property = "choiceAdventure" + String.valueOf(choice);
      this.spoilers = new Spoilers(choice, name, options);
      this.checkProperty();
    }

    public String getZone() {
      return this.zone;
    }

    public String getName() {
      return this.name;
    }

    public ChoiceOption[] getOptions() {
      return (this.options.length == 0)
          ? ChoiceAdventures.dynamicChoiceOptions(this.choice)
          : this.options;
    }

    public String getSetting() {
      return this.property;
    }

    public void checkProperty() {
      // A ChoiceAdventure is configurable in the GUI and its property is used
      // to automate it. There MUST be a property in defaults.txt for it.
      if (!Preferences.containsDefault(this.property)) {
        System.out.println(
            "ChoiceAdventure #"
                + this.choice
                + " does not have a default value for "
                + this.property);
        missingChoiceAdventureDefaultProperties.add(this.choice);
      }
    }

    public Spoilers getSpoilers() {
      return this.spoilers;
    }

    @Override
    protected void addToMap() {
      if (choiceToChoiceAdventure.containsKey(this.choice)) {
        System.out.println("ChoiceAdventure #" + this.choice + " is configured multiple times");
        duplicateChoiceAdventures.add(this.choice);
      }
      choiceToChoiceAdventure.put(this.choice, this);
    }
  }

  // A ChoiceSpoiler is a ChoiceAdventure that isn't user-configurable.
  // The zone is optional, since it doesn't appear in the choiceadv GUI.
  public static class ChoiceSpoiler extends ChoiceAdventure {
    public ChoiceSpoiler(final int choice, final String name, final ChoiceOption... options) {
      super(choice, "Unsorted", name, options);
    }

    public ChoiceSpoiler(
        final int choice, final String zone, final String name, final ChoiceOption... options) {
      super(choice, zone, name, options);
    }

    @Override
    public void checkProperty() {
      // A ChoiceSpoiler isn't configured in the GUI but still has a property
      // associated with it. If it has a default, it can be automated, and that
      // option will be pointed out in the Relay Browser.
      //
      // If it doesn't have a default, automation will use "0", which
      // means "show in browser".

      // In case it is of interest, count ChoiceSpoilers with and without
      // defaults.
      if (Preferences.containsDefault(this.property)) {
        choiceSpoilersWithDefaults.add(this.choice);
      } else {
        choiceSpoilersWithoutDefaults.add(this.choice);
      }
    }

    @Override
    protected void addToMap() {
      if (choiceToChoiceSpoiler.containsKey(this.choice)) {
        System.out.println("ChoiceSpoiler #" + this.choice + " is configured multiple times");
        duplicateChoiceSpoilers.add(this.choice);
      }
      choiceToChoiceSpoiler.put(this.choice, this);
    }
  }

  // A Cost is the (item, meat, MP, ...) cost of taking a particular decision
  public static class Cost {
    private final int decision;
    private final AdventureResult cost;

    public Cost(final int decision, final AdventureResult cost) {
      this.decision = decision;
      this.cost = cost;
    }

    public int getDecision() {
      return this.decision;
    }

    public AdventureResult getCost() {
      return this.cost;
    }
  }

  // A ChoiceCost is a set of costs associated with the various decisions of a choiceAdventure
  public static class ChoiceCost extends Choice {
    private final Cost[] costs;

    public ChoiceCost(final int choice, final Cost... costs) {
      super(choice);
      this.costs = costs;
    }

    public Cost[] getCosts() {
      return this.costs;
    }

    public AdventureResult getCost(final int decision) {
      for (Cost cost : this.costs) {
        if (cost.getDecision() == decision) {
          return cost.getCost();
        }
      }
      return null;
    }

    @Override
    protected void addToMap() {
      if (choiceToChoiceCost.containsKey(this.choice)) {
        System.out.println("ChoiceCost #" + this.choice + " is configured multiple times");
        duplicateChoiceCosts.add(this.choice);
      }
      choiceToChoiceCost.put(this.choice, this);
    }
  }

  public static final ChoiceOption findOption(final ChoiceOption[] options, final int decision) {
    for (int i = 0; i < options.length; ++i) {
      ChoiceOption opt = options[i];
      if (opt != null && opt.getDecision(i + 1) == decision) {
        return opt;
      }
    }
    return null;
  }

  private static final ChoiceOption SKIP_ADVENTURE = new ChoiceOption("skip adventure");

  static {
    // Choice 1 is unknown

    // Denim Axes Examined
    new ChoiceSpoiler(
        2,
        "Palindome",
        // Option...
        new ChoiceOption("denim axe", "denim axe"),
        new ChoiceOption("skip adventure", "rubber axe"));
    // Denim Axes Examined
    new ChoiceCost(2, new Cost(1, ItemPool.get(ItemPool.RUBBER_AXE, -1)));

    // The Oracle Will See You Now
    new ChoiceSpoiler(
        3,
        "Teleportitis",
        // Option...
        SKIP_ADVENTURE,
        new ChoiceOption("randomly sink 100 meat"),
        new ChoiceOption("make plus sign usable"));

    // Finger-Lickin'... Death.
    new ChoiceAdventure(
        4,
        "Beach",
        "South of the Border",
        // Option...
        new ChoiceOption("small meat boost"),
        new ChoiceOption("try for poultrygeist", "poultrygeist"),
        SKIP_ADVENTURE);
    // Finger-Lickin'... Death.
    new ChoiceCost(
        4,
        new Cost(1, new AdventureResult(AdventureResult.MEAT, -500)),
        new Cost(2, new AdventureResult(AdventureResult.MEAT, -500)));

    // Heart of Very, Very Dark Darkness
    new ChoiceAdventure(
        5,
        "MusSign",
        "Gravy Barrow",
        // Option...
        new ChoiceOption("fight the fairy queen"),
        SKIP_ADVENTURE);

    // Darker Than Dark
    new ChoiceSpoiler(
        6,
        "Gravy Barrow",
        // Option...
        new ChoiceOption("get Beaten Up"),
        SKIP_ADVENTURE);

    // Choice 7 is How Depressing

    // On the Verge of a Dirge -> Self Explanatory
    new ChoiceSpoiler(
        8,
        "Gravy Barrow",
        // Option...
        new ChoiceOption("enter the chamber"),
        new ChoiceOption("enter the chamber"),
        new ChoiceOption("enter the chamber"));

    // Wheel In the Sky Keep on Turning: Muscle Position
    new ChoiceSpoiler(
        9,
        "Castle Wheel",
        // Option...
        new ChoiceOption("Turn to mysticality"),
        new ChoiceOption("Turn to moxie"),
        new ChoiceOption("Leave at muscle"));

    // Wheel In the Sky Keep on Turning: Mysticality Position
    new ChoiceSpoiler(
        10,
        "Castle Wheel",
        // Option...
        new ChoiceOption("Turn to Map Quest"),
        new ChoiceOption("Turn to muscle"),
        new ChoiceOption("Leave at mysticality"));

    // Wheel In the Sky Keep on Turning: Map Quest Position
    new ChoiceSpoiler(
        11,
        "Castle Wheel",
        // Option...
        new ChoiceOption("Turn to moxie"),
        new ChoiceOption("Turn to mysticality"),
        new ChoiceOption("Leave at map quest"));

    // Wheel In the Sky Keep on Turning: Moxie Position
    new ChoiceSpoiler(
        12,
        "Castle Wheel",
        // Option...
        new ChoiceOption("Turn to muscle"),
        new ChoiceOption("Turn to map quest"),
        new ChoiceOption("Leave at moxie"));

    // Choice 13 is unknown

    // A Bard Day's Night
    new ChoiceAdventure(
        14,
        "Knob",
        "Cobb's Knob Harem",
        // Option...
        new ChoiceOption("Knob goblin harem veil", "Knob goblin harem veil"),
        new ChoiceOption("Knob goblin harem pants", "Knob goblin harem pants"),
        new ChoiceOption("small meat boost"),
        new ChoiceOption("complete the outfit"));

    // Yeti Nother Hippy
    new ChoiceAdventure(
        15,
        "McLarge",
        "eXtreme Slope",
        // Option...
        new ChoiceOption("eXtreme mittens", "eXtreme mittens"),
        new ChoiceOption("eXtreme scarf", "eXtreme scarf"),
        new ChoiceOption("small meat boost"),
        new ChoiceOption("complete the outfit"));

    // Saint Beernard
    new ChoiceAdventure(
        16,
        "McLarge",
        "eXtreme Slope",
        // Option...
        new ChoiceOption("snowboarder pants", "snowboarder pants"),
        new ChoiceOption("eXtreme scarf", "eXtreme scarf"),
        new ChoiceOption("small meat boost"),
        new ChoiceOption("complete the outfit"));

    // Generic Teen Comedy
    new ChoiceAdventure(
        17,
        "McLarge",
        "eXtreme Slope",
        // Option...
        new ChoiceOption("eXtreme mittens", "eXtreme mittens"),
        new ChoiceOption("snowboarder pants", "snowboarder pants"),
        new ChoiceOption("small meat boost"),
        new ChoiceOption("complete the outfit"));

    // A Flat Miner
    new ChoiceAdventure(
        18,
        "McLarge",
        "Itznotyerzitz Mine",
        // Option...
        new ChoiceOption("miner's pants", "miner's pants"),
        new ChoiceOption("7-Foot Dwarven mattock", "7-Foot Dwarven mattock"),
        new ChoiceOption("small meat boost"),
        new ChoiceOption("complete the outfit"));

    // 100% Legal
    new ChoiceAdventure(
        19,
        "McLarge",
        "Itznotyerzitz Mine",
        // Option...
        new ChoiceOption("miner's helmet", "miner's helmet"),
        new ChoiceOption("miner's pants", "miner's pants"),
        new ChoiceOption("small meat boost"),
        new ChoiceOption("complete the outfit"));

    // See You Next Fall
    new ChoiceAdventure(
        20,
        "McLarge",
        "Itznotyerzitz Mine",
        // Option...
        new ChoiceOption("miner's helmet", "miner's helmet"),
        new ChoiceOption("7-Foot Dwarven mattock", "7-Foot Dwarven mattock"),
        new ChoiceOption("small meat boost"),
        new ChoiceOption("complete the outfit"));

    // Under the Knife
    new ChoiceAdventure(
        21,
        "Town",
        "Sleazy Back Alley",
        // Option...
        new ChoiceOption("switch genders"),
        SKIP_ADVENTURE);
    // Under the Knife
    new ChoiceCost(21, new Cost(1, new AdventureResult(AdventureResult.MEAT, -500)));

    // The Arrrbitrator
    new ChoiceAdventure(
        22,
        "Island",
        "Pirate's Cove",
        // Option...
        new ChoiceOption("eyepatch", "eyepatch"),
        new ChoiceOption("swashbuckling pants", "swashbuckling pants"),
        new ChoiceOption("small meat boost"),
        new ChoiceOption("complete the outfit"));

    // Barrie Me at Sea
    new ChoiceAdventure(
        23,
        "Island",
        "Pirate's Cove",
        // Option...
        new ChoiceOption("stuffed shoulder parrot", "stuffed shoulder parrot"),
        new ChoiceOption("swashbuckling pants", "swashbuckling pants"),
        new ChoiceOption("small meat boost"),
        new ChoiceOption("complete the outfit"));

    // Amatearrr Night
    new ChoiceAdventure(
        24,
        "Island",
        "Pirate's Cove",
        // Option...
        new ChoiceOption("stuffed shoulder parrot", "stuffed shoulder parrot"),
        new ChoiceOption("small meat boost"),
        new ChoiceOption("eyepatch", "eyepatch"),
        new ChoiceOption("complete the outfit"));

    // Ouch! You bump into a door!
    new ChoiceAdventure(
        25,
        "Dungeon",
        "Dungeon of Doom",
        // Option...
        new ChoiceOption("magic lamp", "magic lamp"),
        new ChoiceOption("dead mimic", "dead mimic"),
        SKIP_ADVENTURE);
    // Ouch! You bump into a door!
    new ChoiceCost(
        25,
        new Cost(1, new AdventureResult(AdventureResult.MEAT, -50)),
        new Cost(2, new AdventureResult(AdventureResult.MEAT, -5000)));

    // A Three-Tined Fork
    new ChoiceSpoiler(
        26,
        "Woods",
        "Spooky Forest",
        // Option...
        new ChoiceOption("muscle classes"),
        new ChoiceOption("mysticality classes"),
        new ChoiceOption("moxie classes"));

    // Footprints
    new ChoiceSpoiler(
        27,
        "Woods",
        "Spooky Forest",
        // Option...
        new ChoiceOption(AscensionClass.SEAL_CLUBBER.toString()),
        new ChoiceOption(AscensionClass.TURTLE_TAMER.toString()));

    // A Pair of Craters
    new ChoiceSpoiler(
        28,
        "Woods",
        "Spooky Forest",
        // Option...
        new ChoiceOption(AscensionClass.PASTAMANCER.toString()),
        new ChoiceOption(AscensionClass.SAUCEROR.toString()));

    // The Road Less Visible
    new ChoiceSpoiler(
        29,
        "Woods",
        "Spooky Forest",
        // Option...
        new ChoiceOption(AscensionClass.DISCO_BANDIT.toString()),
        new ChoiceOption(AscensionClass.ACCORDION_THIEF.toString()));

    // Choices 30 - 39 are unknown

    // The Effervescent Fray
    new ChoiceAdventure(
        40,
        "Rift",
        "Cola Wars",
        // Option...
        new ChoiceOption("Cloaca-Cola fatigues", "Cloaca-Cola fatigues"),
        new ChoiceOption("Dyspepsi-Cola shield", "Dyspepsi-Cola shield"),
        new ChoiceOption("mysticality substats"));

    // Smells Like Team Spirit
    new ChoiceAdventure(
        41,
        "Rift",
        "Cola Wars",
        // Option...
        new ChoiceOption("Dyspepsi-Cola fatigues", "Dyspepsi-Cola fatigues"),
        new ChoiceOption("Cloaca-Cola helmet", "Cloaca-Cola helmet"),
        new ChoiceOption("muscle substats"));

    // What is it Good For?
    new ChoiceAdventure(
        42,
        "Rift",
        "Cola Wars",
        // Option...
        new ChoiceOption("Dyspepsi-Cola helmet", "Dyspepsi-Cola helmet"),
        new ChoiceOption("Cloaca-Cola shield", "Cloaca-Cola shield"),
        new ChoiceOption("moxie substats"));

    // Choices 43 - 44 are unknown

    // Maps and Legends
    new ChoiceSpoiler(
        45,
        "Woods",
        "Spooky Forest",
        // Option...
        new ChoiceOption("Spooky Temple map", "Spooky Temple map"),
        SKIP_ADVENTURE,
        SKIP_ADVENTURE);

    // An Interesting Choice
    new ChoiceAdventure(
        46,
        "Woods",
        "Spooky Forest Vampire",
        // Option...
        new ChoiceOption("moxie substats"),
        new ChoiceOption("muscle substats"),
        new ChoiceOption("vampire heart", "vampire heart"));

    // Have a Heart
    new ChoiceAdventure(
        47,
        "Woods",
        "Spooky Forest Vampire Hunter",
        // Option...
        new ChoiceOption("bottle of used blood", "bottle of used blood"),
        new ChoiceOption("skip adventure and keep vampire hearts", "vampire heart"));
    // Have a Heart
    // This trades all vampire hearts for an equal number of
    // bottles of used blood.
    new ChoiceCost(47, new Cost(1, ItemPool.get(ItemPool.VAMPIRE_HEART, 1)));

    // Choices 48 - 70 are violet fog adventures
    // Choice 71 is A Journey to the Center of Your Mind

    // Lording Over The Flies
    new ChoiceAdventure(
        72,
        "Island",
        "Frat House",
        // Option...
        new ChoiceOption("around the world", "around the world"),
        new ChoiceOption("skip adventure", "Spanish fly"));
    // Lording Over The Flies
    // This trades all Spanish flies for around the worlds,
    // in multiples of 5.  Excess flies are left in inventory.
    new ChoiceCost(72, new Cost(1, ItemPool.get(ItemPool.SPANISH_FLY, 5)));

    // Don't Fence Me In
    new ChoiceAdventure(
        73,
        "Woods",
        "Whitey's Grove",
        // Option...
        new ChoiceOption("muscle substats"),
        new ChoiceOption("white picket fence", "white picket fence"),
        new ChoiceOption(
            "wedding cake, white rice 3x (+2x w/ rice bowl)", "piece of wedding cake"));

    // The Only Thing About Him is the Way That He Walks
    new ChoiceAdventure(
        74,
        "Woods",
        "Whitey's Grove",
        // Option...
        new ChoiceOption("moxie substats"),
        new ChoiceOption("boxed wine", "boxed wine"),
        new ChoiceOption("mullet wig", "mullet wig"));

    // Rapido!
    new ChoiceAdventure(
        75,
        "Woods",
        "Whitey's Grove",
        // Option...
        new ChoiceOption("mysticality substats"),
        new ChoiceOption("white lightning", "white lightning"),
        new ChoiceOption("white collar", "white collar"));

    // Junction in the Trunction
    new ChoiceAdventure(
        76,
        "Knob",
        "Knob Shaft",
        // Option...
        new ChoiceOption("cardboard ore", "cardboard ore"),
        new ChoiceOption("styrofoam ore", "styrofoam ore"),
        new ChoiceOption("bubblewrap ore", "bubblewrap ore"));

    // History is Fun!
    new ChoiceSpoiler(
        86,
        "Haunted Library",
        // Option...
        new ChoiceOption("Spookyraven Chapter 1"),
        new ChoiceOption("Spookyraven Chapter 2"),
        new ChoiceOption("Spookyraven Chapter 3"));

    // History is Fun!
    new ChoiceSpoiler(
        87,
        "Haunted Library",
        // Option...
        new ChoiceOption("Spookyraven Chapter 4"),
        new ChoiceOption("Spookyraven Chapter 5"),
        new ChoiceOption("Spookyraven Chapter 6"));

    // Naughty, Naughty
    new ChoiceSpoiler(
        88,
        "Haunted Library",
        // Option...
        new ChoiceOption("mysticality substats"),
        new ChoiceOption("moxie substats"),
        new ChoiceOption("Fettucini / Scarysauce"));
    new ChoiceSpoiler(
        89,
        "Haunted Gallery",
        // Option...
        new ChoiceOption("Wolf Knight"),
        new ChoiceOption("Snake Knight"),
        new ChoiceOption("Dreams and Lights"),
        SKIP_ADVENTURE);

    // Curtains
    new ChoiceAdventure(
        90,
        "Manor2",
        "Haunted Ballroom",
        // Option...
        new ChoiceOption("enter combat"),
        new ChoiceOption("moxie substats"),
        SKIP_ADVENTURE);

    // Having a Medicine Ball
    new ChoiceAdventure(
        105,
        "Manor2",
        "Haunted Bathroom",
        // Option...
        new ChoiceOption("mysticality substats"),
        new ChoiceOption("other options"),
        new ChoiceOption("guy made of bees"));

    // Strung-Up Quartet
    new ChoiceAdventure(
        106,
        "Manor2",
        "Haunted Ballroom",
        // Option...
        new ChoiceOption("increase monster level"),
        new ChoiceOption("decrease combat frequency"),
        new ChoiceOption("increase item drops"),
        new ChoiceOption("disable song"));

    // Bad Medicine is What You Need
    new ChoiceAdventure(
        107,
        "Manor2",
        "Haunted Bathroom",
        // Option...
        new ChoiceOption("antique bottle of cough syrup", "antique bottle of cough syrup"),
        new ChoiceOption("tube of hair oil", "tube of hair oil"),
        new ChoiceOption("bottle of ultravitamins", "bottle of ultravitamins"),
        SKIP_ADVENTURE);

    // Aww, Craps
    new ChoiceAdventure(
        108,
        "Town",
        "Sleazy Back Alley",
        // Option...
        new ChoiceOption("moxie substats"),
        new ChoiceOption("meat and moxie"),
        new ChoiceOption("random effect"),
        SKIP_ADVENTURE);

    // Dumpster Diving
    new ChoiceAdventure(
        109,
        "Town",
        "Sleazy Back Alley",
        // Option...
        new ChoiceOption("enter combat"),
        new ChoiceOption("meat and moxie"),
        new ChoiceOption("Mad Train wine", "Mad Train wine"));

    // The Entertainer
    new ChoiceAdventure(
        110,
        "Town",
        "Sleazy Back Alley",
        // Option...
        new ChoiceOption("moxie substats"),
        new ChoiceOption("moxie and muscle"),
        new ChoiceOption("small meat boost"),
        SKIP_ADVENTURE);

    // Malice in Chains
    new ChoiceAdventure(
        111,
        "Knob",
        "Outskirts of The Knob",
        // Option...
        new ChoiceOption("muscle substats"),
        new ChoiceOption("muscle substats"),
        new ChoiceOption("enter combat"));

    // Please, Hammer
    new ChoiceAdventure(
        112,
        "Town",
        "Sleazy Back Alley",
        // Option...
        new ChoiceOption("accept hammer quest"),
        new ChoiceOption("reject quest"),
        new ChoiceOption("muscle substats"));

    // Knob Goblin BBQ
    new ChoiceAdventure(
        113,
        "Knob",
        "Outskirts of The Knob",
        // Option...
        new ChoiceOption("complete cake quest"),
        new ChoiceOption("enter combat"),
        new ChoiceOption("get a random item"));

    // The Baker's Dilemma
    new ChoiceAdventure(
        114,
        "Manor1",
        "Haunted Pantry",
        // Option...
        new ChoiceOption("accept cake quest"),
        new ChoiceOption("reject quest"),
        new ChoiceOption("moxie and meat"));

    // Oh No, Hobo
    new ChoiceAdventure(
        115,
        "Manor1",
        "Haunted Pantry",
        // Option...
        new ChoiceOption("enter combat"),
        new ChoiceOption("Good Karma"),
        new ChoiceOption("mysticality, moxie, and meat"));

    // The Singing Tree
    new ChoiceAdventure(
        116,
        "Manor1",
        "Haunted Pantry",
        // Option...
        new ChoiceOption("mysticality substats"),
        new ChoiceOption("moxie substats"),
        new ChoiceOption("random effect"),
        SKIP_ADVENTURE);

    // Tresspasser
    new ChoiceAdventure(
        117,
        "Manor1",
        "Haunted Pantry",
        // Option...
        new ChoiceOption("enter combat"),
        new ChoiceOption("mysticality substats"),
        new ChoiceOption("get a random item"));

    // When Rocks Attack
    new ChoiceAdventure(
        118,
        "Knob",
        "Outskirts of The Knob",
        // Option...
        new ChoiceOption("accept unguent quest"),
        SKIP_ADVENTURE);

    // Choice 119 is Check It Out Now

    // Ennui is Wasted on the Young
    new ChoiceAdventure(
        120,
        "Knob",
        "Outskirts of The Knob",
        // Option...
        new ChoiceOption("muscle and Pumped Up"),
        new ChoiceOption("ice cold Sir Schlitz", "ice cold Sir Schlitz"),
        new ChoiceOption("moxie and lemon", "lemon"),
        SKIP_ADVENTURE);

    // Choice 121 is Next Sunday, A.D.
    // Choice 122 is unknown

    // At Least It's Not Full Of Trash
    new ChoiceSpoiler(
        123,
        "Hidden Temple",
        // Option...
        new ChoiceOption("lose HP"),
        new ChoiceOption("Unlock Quest Puzzle"),
        new ChoiceOption("lose HP"));

    // Choice 124 is unknown

    // No Visible Means of Support
    new ChoiceSpoiler(
        125,
        "Hidden Temple",
        // Option...
        new ChoiceOption("lose HP"),
        new ChoiceOption("lose HP"),
        new ChoiceOption("Unlock Hidden City"));

    // Sun at Noon, Tan Us
    new ChoiceAdventure(
        126,
        "Plains",
        "Palindome",
        // Option...
        new ChoiceOption("moxie"),
        new ChoiceOption("chance of more moxie"),
        new ChoiceOption("sunburned"));

    // No sir, away!  A papaya war is on!
    new ChoiceSpoiler(
        127,
        "Plains",
        "Palindome",
        // Option...
        new ChoiceOption("3 papayas", "papaya"),
        new ChoiceOption("trade 3 papayas for stats"),
        new ChoiceOption("stats"));
    // No sir, away!  A papaya war is on!
    new ChoiceCost(127, new Cost(2, ItemPool.get(ItemPool.PAPAYA, -3)));

    // Choice 128 is unknown

    // Do Geese See God?
    new ChoiceSpoiler(
        129,
        "Plains",
        "Palindome",
        // Option...
        new ChoiceOption("photograph of God", "photograph of God"),
        SKIP_ADVENTURE);
    // Do Geese See God?
    new ChoiceCost(129, new Cost(1, new AdventureResult(AdventureResult.MEAT, -500)));

    // Choice 133 is unknown

    // Peace Wants Love
    new ChoiceAdventure(
        136,
        "Island",
        "Hippy Camp",
        // Option...
        new ChoiceOption("filthy corduroys", "filthy corduroys"),
        new ChoiceOption("filthy knitted dread sack", "filthy knitted dread sack"),
        new ChoiceOption("small meat boost"),
        new ChoiceOption("complete the outfit"));

    // An Inconvenient Truth
    new ChoiceAdventure(
        137,
        "Island",
        "Hippy Camp",
        // Option...
        new ChoiceOption("filthy knitted dread sack", "filthy knitted dread sack"),
        new ChoiceOption("filthy corduroys", "filthy corduroys"),
        new ChoiceOption("small meat boost"),
        new ChoiceOption("complete the outfit"));

    // Purple Hazers
    new ChoiceAdventure(
        138,
        "Island",
        "Frat House",
        // Option...
        new ChoiceOption("Orcish cargo shorts", "Orcish cargo shorts"),
        new ChoiceOption("Orcish baseball cap", "Orcish baseball cap"),
        new ChoiceOption("Orcish frat-paddle", "Orcish frat-paddle"),
        new ChoiceOption("complete the outfit"));

    // Bait and Switch
    new ChoiceAdventure(
        139,
        "IsleWar",
        "War Hippies",
        // Option...
        new ChoiceOption("muscle substats"),
        new ChoiceOption("ferret bait", "ferret bait"),
        new ChoiceOption("enter combat"));

    // The Thin Tie-Dyed Line
    new ChoiceAdventure(
        140,
        "IsleWar",
        "War Hippies",
        // Option...
        new ChoiceOption("water pipe bombs", "water pipe bomb"),
        new ChoiceOption("moxie substats"),
        new ChoiceOption("enter combat"));

    // Blockin' Out the Scenery
    new ChoiceAdventure(
        141,
        "IsleWar",
        "War Hippies",
        // Option...
        new ChoiceOption("mysticality substats"),
        new ChoiceOption("get some hippy food"),
        new ChoiceOption("waste a turn"));

    // Blockin' Out the Scenery
    new ChoiceAdventure(
        142,
        "IsleWar",
        "War Hippies",
        // Option...
        new ChoiceOption("mysticality substats"),
        new ChoiceOption("get some hippy food"),
        new ChoiceOption("start the war"));

    // Catching Some Zetas
    new ChoiceAdventure(
        143,
        "IsleWar",
        "War Fraternity",
        // Option...
        new ChoiceOption("muscle substats"),
        new ChoiceOption("sake bombs", "sake bomb"),
        new ChoiceOption("enter combat"));

    // One Less Room Than In That Movie
    new ChoiceAdventure(
        144,
        "IsleWar",
        "War Fraternity",
        // Option...
        new ChoiceOption("moxie substats"),
        new ChoiceOption("beer bombs", "beer bomb"),
        new ChoiceOption("enter combat"));

    // Fratacombs
    new ChoiceAdventure(
        145,
        "IsleWar",
        "War Fraternity",
        // Option...
        new ChoiceOption("muscle substats"),
        new ChoiceOption("get some frat food"),
        new ChoiceOption("waste a turn"));

    // Fratacombs
    new ChoiceAdventure(
        146,
        "IsleWar",
        "War Fraternity",
        // Option...
        new ChoiceOption("muscle substats"),
        new ChoiceOption("get some frat food"),
        new ChoiceOption("start the war"));

    // Cornered!
    new ChoiceAdventure(
        147,
        "Farm",
        "McMillicancuddy's Barn",
        // Option...
        new ChoiceOption("Open The Granary (meat)"),
        new ChoiceOption("Open The Bog (stench)"),
        new ChoiceOption("Open The Pond (cold)"));

    // Cornered Again!
    new ChoiceAdventure(
        148,
        "Farm",
        "McMillicancuddy's Barn",
        // Option...
        new ChoiceOption("Open The Back 40 (hot)"),
        new ChoiceOption("Open The Family Plot (spooky)"));

    // How Many Corners Does this Stupid Barn Have!?
    new ChoiceAdventure(
        149,
        "Farm",
        "McMillicancuddy's Barn",
        // Option...
        new ChoiceOption("Open The Shady Thicket (booze)"),
        new ChoiceOption("Open The Other Back 40 (sleaze)"));

    // Choice 150 is Another Adventure About BorderTown

    // Adventurer, $1.99
    new ChoiceAdventure(
        151,
        "Plains",
        "Fun House",
        // Option...
        new ChoiceOption("fight the clownlord"),
        SKIP_ADVENTURE);

    // Lurking at the Threshold
    new ChoiceSpoiler(
        152,
        "Plains",
        "Fun House",
        // Option...
        new ChoiceOption("fight the clownlord"),
        SKIP_ADVENTURE);

    // Turn Your Head and Coffin
    new ChoiceAdventure(
        153,
        "Cyrpt",
        "Defiled Alcove",
        // Option...
        new ChoiceOption("muscle substats"),
        new ChoiceOption("small meat boost"),
        new ChoiceOption("half-rotten brain", "half-rotten brain"),
        SKIP_ADVENTURE);

    // Choice 154 used to be Doublewide

    // Skull, Skull, Skull
    new ChoiceAdventure(
        155,
        "Cyrpt",
        "Defiled Nook",
        // Option...
        new ChoiceOption("moxie substats"),
        new ChoiceOption("small meat boost"),
        new ChoiceOption("rusty bonesaw", "rusty bonesaw"),
        new ChoiceOption("debonair deboner", "debonair deboner"),
        SKIP_ADVENTURE);

    // Choice 156 used to be Pileup

    // Urning Your Keep
    new ChoiceAdventure(
        157,
        "Cyrpt",
        "Defiled Niche",
        // Option...
        new ChoiceOption("mysticality substats"),
        new ChoiceOption("plus-sized phylactery", "plus-sized phylactery"),
        new ChoiceOption("small meat boost"),
        SKIP_ADVENTURE);

    // Choice 158 used to be Lich in the Niche
    // Choice 159 used to be Go Slow Past the Drawers
    // Choice 160 used to be Lunchtime

    // Choice 161 is Bureaucracy of the Damned

    // Between a Rock and Some Other Rocks
    new ChoiceSpoiler(
        162,
        "Goatlet",
        // Option...
        new ChoiceOption("Open Goatlet"),
        SKIP_ADVENTURE);

    // Melvil Dewey Would Be Ashamed
    new ChoiceAdventure(
        163,
        "Manor1",
        "Haunted Library",
        // Option...
        new ChoiceOption("Necrotelicomnicon", "Necrotelicomnicon"),
        new ChoiceOption("Cookbook of the Damned", "Cookbook of the Damned"),
        new ChoiceOption("Sinful Desires", "Sinful Desires"),
        SKIP_ADVENTURE);

    // The Wormwood choices always come in order

    // 1: 164, 167, 170
    // 2: 165, 168, 171
    // 3: 166, 169, 172

    // Some first-round choices give you an effect for five turns:

    // 164/2 -> Spirit of Alph
    // 167/3 -> Bats in the Belfry
    // 170/1 -> Rat-Faced

    // First-round effects modify some second round options and
    // give you a second effect for five rounds. If you do not have
    // the appropriate first-round effect, these second-round
    // options do not consume an adventure.

    // 165/1 + Rat-Faced -> Night Vision
    // 165/2 + Bats in the Belfry -> Good with the Ladies
    // 168/2 + Spirit of Alph -> Feelin' Philosophical
    // 168/2 + Rat-Faced -> Unusual Fashion Sense
    // 171/1 + Bats in the Belfry -> No Vertigo
    // 171/3 + Spirit of Alph -> Dancing Prowess

    // Second-round effects modify some third round options and
    // give you an item. If you do not have the appropriate
    // second-round effect, most of these third-round options do
    // not consume an adventure.

    // 166/1 + No Vertigo -> S.T.L.T.
    // 166/3 + Unusual Fashion Sense -> albatross necklace
    // 169/1 + Night Vision -> flask of Amontillado
    // 169/3 + Dancing Prowess -> fancy ball mask
    // 172/1 + Good with the Ladies -> Can-Can skirt
    // 172/1 -> combat
    // 172/2 + Feelin' Philosophical -> not-a-pipe

    // Down by the Riverside
    new ChoiceAdventure(
        164,
        "Wormwood",
        "Pleasure Dome",
        // Option...
        new ChoiceOption("muscle substats"),
        new ChoiceOption("MP & Spirit of Alph"),
        new ChoiceOption("enter combat"));

    // Beyond Any Measure
    new ChoiceAdventure(
        165,
        "Wormwood",
        "Pleasure Dome",
        // Option...
        new ChoiceOption("Rat-Faced -> Night Vision"),
        new ChoiceOption("Bats in the Belfry -> Good with the Ladies"),
        new ChoiceOption("mysticality substats"),
        SKIP_ADVENTURE);

    // Death is a Boat
    new ChoiceAdventure(
        166,
        "Wormwood",
        "Pleasure Dome",
        // Option...
        new ChoiceOption("No Vertigo -> S.T.L.T.", "S.T.L.T."),
        new ChoiceOption("moxie substats"),
        new ChoiceOption("Unusual Fashion Sense -> albatross necklace", "albatross necklace"));

    // It's a Fixer-Upper
    new ChoiceAdventure(
        167,
        "Wormwood",
        "Moulder Mansion",
        // Option...
        new ChoiceOption("enter combat"),
        new ChoiceOption("mysticality substats"),
        new ChoiceOption("HP & MP & Bats in the Belfry"));

    // Midst the Pallor of the Parlor
    new ChoiceAdventure(
        168,
        "Wormwood",
        "Moulder Mansion",
        // Option...
        new ChoiceOption("moxie substats"),
        new ChoiceOption("Spirit of Alph -> Feelin' Philosophical"),
        new ChoiceOption("Rat-Faced -> Unusual Fashion Sense"));

    // A Few Chintz Curtains, Some Throw Pillows, It
    new ChoiceAdventure(
        169,
        "Wormwood",
        "Moulder Mansion",
        // Option...
        new ChoiceOption("Night Vision -> flask of Amontillado", "flask of Amontillado"),
        new ChoiceOption("muscle substats"),
        new ChoiceOption("Dancing Prowess -> fancy ball mask", "fancy ball mask"));

    // La Vie Boheme
    new ChoiceAdventure(
        170,
        "Wormwood",
        "Rogue Windmill",
        // Option...
        new ChoiceOption("HP & Rat-Faced"),
        new ChoiceOption("enter combat"),
        new ChoiceOption("moxie substats"));

    // Backstage at the Rogue Windmill
    new ChoiceAdventure(
        171,
        "Wormwood",
        "Rogue Windmill",
        // Option...
        new ChoiceOption("Bats in the Belfry -> No Vertigo"),
        new ChoiceOption("muscle substats"),
        new ChoiceOption("Spirit of Alph -> Dancing Prowess"));

    // Up in the Hippo Room
    new ChoiceAdventure(
        172,
        "Wormwood",
        "Rogue Windmill",
        // Option...
        new ChoiceOption("Good with the Ladies -> Can-Can skirt", "Can-Can skirt"),
        new ChoiceOption("Feelin' Philosophical -> not-a-pipe", "not-a-pipe"),
        new ChoiceOption("mysticality substats"));

    // Choice 173 is The Last Stand, Man
    // Choice 174 is The Last Stand, Bra
    // Choice 175-176 are unknown

    // Choice 177 was The Blackberry Cobbler

    // Hammering the Armory
    new ChoiceAdventure(
        178,
        "Beanstalk",
        "Fantasy Airship Shirt",
        // Option...
        new ChoiceOption("bronze breastplate", "bronze breastplate"),
        SKIP_ADVENTURE);

    // Choice 179 is unknown

    // A Pre-War Dresser Drawer, Pa!
    new ChoiceAdventure(
        180,
        "Plains",
        "Palindome Shirt",
        // Option...
        new ChoiceOption("Ye Olde Navy Fleece", "Ye Olde Navy Fleece"),
        SKIP_ADVENTURE);

    // Chieftain of the Flies
    new ChoiceAdventure(
        181,
        "Island",
        "Frat House (Stone Age)",
        // Option...
        new ChoiceOption("around the world", "around the world"),
        new ChoiceOption("skip adventure", "Spanish fly"));
    // Chieftain of the Flies
    // This trades all Spanish flies for around the worlds,
    // in multiples of 5.  Excess flies are left in inventory.
    new ChoiceCost(181, new Cost(1, ItemPool.get(ItemPool.SPANISH_FLY, 5)));

    // Random Lack of an Encounter
    new ChoiceAdventure(
        182,
        "Beanstalk",
        "Fantasy Airship",
        // Option...
        new ChoiceOption("enter combat"),
        new ChoiceOption("Penultimate Fantasy chest", "Penultimate Fantasy chest"),
        new ChoiceOption("stats"),
        new ChoiceOption("model airship and combat", "model airship"),
        new ChoiceOption("model airship and chest", "model airship"),
        new ChoiceOption("model airship and stats", "model airship"));

    // That Explains All The Eyepatches
    // Dynamically calculate options based on mainstat
    new ChoiceAdventure(184, "Pirate", "Barrrney's Barrr");

    // Yes, You're a Rock Starrr
    new ChoiceAdventure(185, "Pirate", "Barrrney's Barrr");

    // A Test of Testarrrsterone
    new ChoiceAdventure(
        186,
        "Pirate",
        "Barrrney's Barrr",
        // Option...
        new ChoiceOption("stats"),
        new ChoiceOption("drunkenness and stats"),
        new ChoiceOption("moxie"));

    // Choice 187 is Arrr You Man Enough?

    // The Infiltrationist
    new ChoiceAdventure(
        188,
        "Item-Driven",
        "Frathouse Blueprints",
        // Option...
        new ChoiceOption("frat boy ensemble"),
        new ChoiceOption("mullet wig and briefcase"),
        new ChoiceOption("frilly skirt and hot wings"));

    // O Cap'm, My Cap'm
    new ChoiceCost(189, new Cost(1, new AdventureResult(AdventureResult.MEAT, -977)));

    // Choice 190 is unknown

    // Chatterboxing
    new ChoiceAdventure(
        191,
        "Pirate",
        "F'c'le",
        // Option...
        new ChoiceOption("moxie substats"),
        new ChoiceOption("use valuable trinket to banish, or lose hp"),
        new ChoiceOption("muscle substats"),
        new ChoiceOption("mysticality substats"),
        new ChoiceOption("use valuable trinket to banish, or moxie"),
        new ChoiceOption("use valuable trinket to banish, or muscle"),
        new ChoiceOption("use valuable trinket to banish, or mysticality"),
        new ChoiceOption("use valuable trinket to banish, or mainstat"));
    new ChoiceCost(191, new Cost(2, ItemPool.get(ItemPool.VALUABLE_TRINKET, -1)));

    // Choice 192 is unknown
    // Choice 193 is Modular, Dude

    // Somewhat Higher and Mostly Dry
    new ChoiceAdventure(
        197,
        "Hobopolis",
        "A Maze of Sewer Tunnels",
        // Option...
        new ChoiceOption("take the tunnel"),
        new ChoiceOption("sewer gator"),
        new ChoiceOption("turn the valve"));

    // Disgustin' Junction
    new ChoiceAdventure(
        198,
        "Hobopolis",
        "A Maze of Sewer Tunnels",
        // Option...
        new ChoiceOption("take the tunnel"),
        new ChoiceOption("giant zombie goldfish"),
        new ChoiceOption("open the grate"));

    // The Former or the Ladder
    new ChoiceAdventure(
        199,
        "Hobopolis",
        "A Maze of Sewer Tunnels",
        // Option...
        new ChoiceOption("take the tunnel"),
        new ChoiceOption("C. H. U. M."),
        new ChoiceOption("head down the ladder"));

    // Enter The Hoboverlord
    new ChoiceAdventure(
        200,
        "Hobopolis",
        "Hobopolis Town Square",
        // Option...
        new ChoiceOption("enter combat with Hodgman"),
        SKIP_ADVENTURE);

    // Home, Home in the Range
    new ChoiceAdventure(
        201,
        "Hobopolis",
        "Burnbarrel Blvd.",
        // Option...
        new ChoiceOption("enter combat with Ol' Scratch"),
        SKIP_ADVENTURE);

    // Bumpity Bump Bump
    new ChoiceAdventure(
        202,
        "Hobopolis",
        "Exposure Esplanade",
        // Option...
        new ChoiceOption("enter combat with Frosty"),
        SKIP_ADVENTURE);

    // Deep Enough to Dive
    new ChoiceAdventure(
        203,
        "Hobopolis",
        "The Heap",
        // Option...
        new ChoiceOption("enter combat with Oscus"),
        SKIP_ADVENTURE);

    // Welcome To You!
    new ChoiceAdventure(
        204,
        "Hobopolis",
        "The Ancient Hobo Burial Ground",
        // Option...
        new ChoiceOption("enter combat with Zombo"),
        SKIP_ADVENTURE);

    // Van, Damn
    new ChoiceAdventure(
        205,
        "Hobopolis",
        "The Purple Light District",
        // Option...
        new ChoiceOption("enter combat with Chester"),
        SKIP_ADVENTURE);

    // Getting Tired
    new ChoiceAdventure(
        206,
        "Hobopolis",
        "Burnbarrel Blvd.",
        // Option...
        new ChoiceOption("start tirevalanche"),
        new ChoiceOption("add tire to stack"),
        SKIP_ADVENTURE);

    // Hot Dog! I Mean... Door!
    new ChoiceAdventure(
        207,
        "Hobopolis",
        "Burnbarrel Blvd.",
        // Option...
        new ChoiceOption("increase hot hobos & get clan meat"),
        SKIP_ADVENTURE);

    // Ah, So That's Where They've All Gone
    new ChoiceAdventure(
        208,
        "Hobopolis",
        "The Ancient Hobo Burial Ground",
        // Option...
        new ChoiceOption("increase spooky hobos & decrease stench"),
        SKIP_ADVENTURE);

    // Choice 209 is Timbarrrr!
    // Choice 210 is Stumped

    // Despite All Your Rage
    new ChoiceAdventure(
        211,
        "Hobopolis",
        "A Maze of Sewer Tunnels",
        // Option...
        new ChoiceOption("gnaw through the bars"));

    // Choice 212 is also Despite All Your Rage, apparently after you've already
    // tried to wait for rescue?
    new ChoiceAdventure(
        212,
        "Hobopolis",
        "A Maze of Sewer Tunnels",
        // Option...
        new ChoiceOption("gnaw through the bars"));

    // Piping Hot
    new ChoiceAdventure(
        213,
        "Hobopolis",
        "Burnbarrel Blvd.",
        // Option...
        new ChoiceOption("increase sleaze hobos & decrease heat"),
        SKIP_ADVENTURE);

    // You vs. The Volcano
    new ChoiceAdventure(
        214,
        "Hobopolis",
        "The Heap",
        // Option...
        new ChoiceOption("decrease stench hobos & increase stench"),
        SKIP_ADVENTURE);

    // Piping Cold
    new ChoiceAdventure(
        215,
        "Hobopolis",
        "Exposure Esplanade",
        // Option...
        new ChoiceOption("decrease heat"),
        new ChoiceOption("decrease sleaze hobos"),
        new ChoiceOption("increase number of icicles"));

    // The Compostal Service
    new ChoiceAdventure(
        216,
        "Hobopolis",
        "The Heap",
        // Option...
        new ChoiceOption("decrease stench & spooky"),
        SKIP_ADVENTURE);

    // There Goes Fritz!
    new ChoiceAdventure(
        217,
        "Hobopolis",
        "Exposure Esplanade",
        // Option...
        new ChoiceOption("yodel a little"),
        new ChoiceOption("yodel a lot"),
        new ChoiceOption("yodel your heart out"));

    // I Refuse!
    new ChoiceAdventure(
        218,
        "Hobopolis",
        "The Heap",
        // Option...
        new ChoiceOption("explore the junkpile"),
        SKIP_ADVENTURE);

    // The Furtivity of My City
    new ChoiceAdventure(
        219,
        "Hobopolis",
        "The Purple Light District",
        // Option...
        new ChoiceOption("fight sleaze hobo"),
        new ChoiceOption("increase stench"),
        new ChoiceOption("increase sleaze hobos & get clan meat"));

    // Returning to the Tomb
    new ChoiceAdventure(
        220,
        "Hobopolis",
        "The Ancient Hobo Burial Ground",
        // Option...
        new ChoiceOption("increase spooky hobos & get clan meat"),
        SKIP_ADVENTURE);

    // A Chiller Night
    new ChoiceAdventure(
        221,
        "Hobopolis",
        "The Ancient Hobo Burial Ground",
        // Option...
        new ChoiceOption("study the dance moves"),
        new ChoiceOption("dance with hobo zombies"),
        SKIP_ADVENTURE);

    // A Chiller Night (2)
    new ChoiceAdventure(
        222,
        "Hobopolis",
        "The Ancient Hobo Burial Ground",
        // Option...
        new ChoiceOption("dance with hobo zombies"),
        SKIP_ADVENTURE);

    // Getting Clubbed
    new ChoiceAdventure(
        223,
        "Hobopolis",
        "The Purple Light District",
        // Option...
        new ChoiceOption("try to get inside"),
        new ChoiceOption("try to bamboozle the crowd"),
        new ChoiceOption("try to flimflam the crowd"));

    // Exclusive!
    new ChoiceAdventure(
        224,
        "Hobopolis",
        "The Purple Light District",
        // Option...
        new ChoiceOption("fight sleaze hobo"),
        new ChoiceOption("start barfight"),
        new ChoiceOption("gain stats"));

    // Attention -- A Tent!
    new ChoiceAdventure(
        225,
        "Hobopolis",
        "Hobopolis Town Square",
        // Option...
        new ChoiceOption("perform on stage"),
        new ChoiceOption("join the crowd"),
        SKIP_ADVENTURE);

    // Choice 226 is Here You Are, Up On Stage (use the same system as 211 & 212)
    // Choice 227 is Working the Crowd (use the same system as 211 & 212)

    // Choices 228 & 229 are unknown

    // Mind Yer Binder
    new ChoiceAdventure(
        230,
        "Hobopolis",
        "Hobopolis Town Square",
        // Option...
        new ChoiceOption("hobo code binder", "hobo code binder"),
        SKIP_ADVENTURE);
    // Mind Yer Binder
    new ChoiceCost(230, new Cost(1, ItemPool.get(ItemPool.HOBO_NICKEL, -30)));

    // Choices 231-271 are subchoices of Choice 272

    // Food, Glorious Food
    new ChoiceSpoiler(
        235,
        "Hobopolis Marketplace",
        // Option...
        new ChoiceOption("muscle food"),
        new ChoiceOption("mysticality food"),
        new ChoiceOption("moxie food"));

    // Booze, Glorious Booze
    new ChoiceSpoiler(
        240,
        "Hobopolis Marketplace",
        // Option...
        new ChoiceOption("muscle booze"),
        new ChoiceOption("mysticality booze"),
        new ChoiceOption("moxie booze"));

    // The Guy Who Carves Driftwood Animals
    new ChoiceCost(247, new Cost(1, ItemPool.get(ItemPool.HOBO_NICKEL, -10)));

    // A Hattery
    new ChoiceSpoiler(
        250,
        "Hobopolis Marketplace",
        // Option...
        new ChoiceOption("crumpled felt fedora", "crumpled felt fedora"),
        new ChoiceOption("battered old top-hat", "battered old top-hat"),
        new ChoiceOption("shapeless wide-brimmed hat", "shapeless wide-brimmed hat"));
    // A Hattery
    new ChoiceCost(
        250,
        new Cost(1, ItemPool.get(ItemPool.HOBO_NICKEL, -250)),
        new Cost(2, ItemPool.get(ItemPool.HOBO_NICKEL, -150)),
        new Cost(3, ItemPool.get(ItemPool.HOBO_NICKEL, -200)));

    // A Pantry
    new ChoiceSpoiler(
        251,
        "Hobopolis Marketplace",
        // Option...
        new ChoiceOption("mostly rat-hide leggings", "mostly rat-hide leggings"),
        new ChoiceOption("hobo dungarees", "hobo dungarees"),
        new ChoiceOption("old patched suit-pants", "old patched suit-pants"));
    // A Pantry
    new ChoiceCost(
        251,
        new Cost(1, ItemPool.get(ItemPool.HOBO_NICKEL, -200)),
        new Cost(2, ItemPool.get(ItemPool.HOBO_NICKEL, -150)),
        new Cost(3, ItemPool.get(ItemPool.HOBO_NICKEL, -250)));

    // Hobo Blanket Bingo
    new ChoiceSpoiler(
        252,
        "Hobopolis Marketplace",
        // Option...
        new ChoiceOption("old soft shoes", "old soft shoes"),
        new ChoiceOption("hobo stogie", "hobo stogie"),
        new ChoiceOption("rope with some soap on it", "rope with some soap on it"));
    // Hobo Blanket Bingo
    new ChoiceCost(
        252,
        new Cost(1, ItemPool.get(ItemPool.HOBO_NICKEL, -250)),
        new Cost(2, ItemPool.get(ItemPool.HOBO_NICKEL, -200)),
        new Cost(3, ItemPool.get(ItemPool.HOBO_NICKEL, -150)));

    // Black-and-Blue-and-Decker
    new ChoiceSpoiler(
        255,
        "Hobopolis Marketplace",
        // Option...
        new ChoiceOption("sharpened hubcap", "sharpened hubcap"),
        new ChoiceOption("very large caltrop", "very large caltrop"),
        new ChoiceOption("The Six-Pack of Pain", "The Six-Pack of Pain"));
    // Black-and-Blue-and-Decker
    new ChoiceCost(
        255,
        new Cost(1, ItemPool.get(ItemPool.HOBO_NICKEL, -10)),
        new Cost(2, ItemPool.get(ItemPool.HOBO_NICKEL, -10)),
        new Cost(3, ItemPool.get(ItemPool.HOBO_NICKEL, -10)));

    // Instru-mental
    new ChoiceCost(258, new Cost(1, ItemPool.get(ItemPool.HOBO_NICKEL, -99)));

    // We'll Make Great...
    new ChoiceSpoiler(
        259,
        "Hobopolis Marketplace",
        // Option...
        new ChoiceOption("hobo monkey"),
        new ChoiceOption("stats"),
        new ChoiceOption("enter combat"));

    // Everybody's Got Something To Hide
    new ChoiceCost(261, new Cost(1, ItemPool.get(ItemPool.HOBO_NICKEL, -1000)));

    // Tanning Salon
    new ChoiceSpoiler(
        264,
        "Hobopolis Marketplace",
        // Option...
        new ChoiceOption("20 adv of +50% moxie"),
        new ChoiceOption("20 adv of +50% mysticality"));
    // Tanning Salon
    new ChoiceCost(
        264,
        new Cost(1, ItemPool.get(ItemPool.HOBO_NICKEL, -5)),
        new Cost(2, ItemPool.get(ItemPool.HOBO_NICKEL, -5)));

    // Let's All Go To The Movies
    new ChoiceSpoiler(
        267,
        "Hobopolis Marketplace",
        // Option...
        new ChoiceOption("20 adv of +5 spooky resistance"),
        new ChoiceOption("20 adv of +5 sleaze resistance"));
    // Let's All Go To The Movies
    new ChoiceCost(
        267,
        new Cost(1, ItemPool.get(ItemPool.HOBO_NICKEL, -5)),
        new Cost(2, ItemPool.get(ItemPool.HOBO_NICKEL, -5)));

    // It's Fun To Stay There
    new ChoiceSpoiler(
        268,
        "Hobopolis Marketplace",
        // Option...
        new ChoiceOption("20 adv of +5 stench resistance"),
        new ChoiceOption("20 adv of +50% muscle"));
    // It's Fun To Stay There
    new ChoiceCost(
        268,
        new Cost(1, ItemPool.get(ItemPool.HOBO_NICKEL, -5)),
        new Cost(2, ItemPool.get(ItemPool.HOBO_NICKEL, -5)));

    // Marketplace Entrance
    new ChoiceAdventure(
        272,
        "Hobopolis",
        "Hobopolis Town Square",
        // Option...
        new ChoiceOption("enter marketplace"),
        SKIP_ADVENTURE);

    // Piping Cold
    new ChoiceAdventure(
        273,
        "Hobopolis",
        "Exposure Esplanade",
        // Option...
        new ChoiceOption("frozen banquet"),
        new ChoiceOption("increase cold hobos & get clan meat"),
        SKIP_ADVENTURE);

    // Choice 274 is Tattoo Redux, a subchoice of Choice 272 when
    // you've started a tattoo

    // Choice 275 is Triangle, Man, a subchoice of Choice 272 when
    // you've already purchased your class instrument
    // Triangle, Man
    new ChoiceCost(275, new Cost(1, ItemPool.get(ItemPool.HOBO_NICKEL, -10)));

    // Choices 278-290 are llama lama gong related choices

    // The Gong Has Been Bung
    new ChoiceSpoiler(
        276,
        "Gong",
        // Option...
        new ChoiceOption("3 adventures"),
        new ChoiceOption("12 adventures"),
        new ChoiceOption("15 adventures"));

    // Welcome Back!
    new ChoiceSpoiler(
        277,
        "Gong",
        // Option...
        new ChoiceOption("finish journey"),
        new ChoiceOption("also finish journey"));

    // Enter the Roach
    new ChoiceSpoiler(
        278,
        "Gong",
        // Option...
        new ChoiceOption("muscle substats"),
        new ChoiceOption("mysticality substats"),
        new ChoiceOption("moxie substats"));

    // It's Nukyuhlur - the 'S' is Silent.
    new ChoiceSpoiler(
        279,
        "Gong",
        // Option...
        new ChoiceOption("moxie substats"),
        new ChoiceOption("muscle substats"),
        new ChoiceOption("gain MP"));

    // Eek! Eek!
    new ChoiceSpoiler(
        280,
        "Gong",
        // Option...
        new ChoiceOption("mysticality substats"),
        new ChoiceOption("muscle substats"),
        new ChoiceOption("gain MP"));

    // A Meta-Metamorphosis
    new ChoiceSpoiler(
        281,
        "Gong",
        // Option...
        new ChoiceOption("moxie substats"),
        new ChoiceOption("mysticality substats"),
        new ChoiceOption("gain MP"));

    // You've Got Wings, But No Wingman
    new ChoiceSpoiler(
        282,
        "Gong",
        // Option...
        new ChoiceOption("+30% muscle"),
        new ChoiceOption("+10% all stats"),
        new ChoiceOption("+30 ML"));

    // Time Enough at Last!
    new ChoiceSpoiler(
        283,
        "Gong",
        // Option...
        new ChoiceOption("+30% muscle"),
        new ChoiceOption("+10% all stats"),
        new ChoiceOption("+50% item drops"));

    // Scavenger Is Your Middle Name
    new ChoiceSpoiler(
        284,
        "Gong",
        // Option...
        new ChoiceOption("+30% muscle"),
        new ChoiceOption("+50% item drops"),
        new ChoiceOption("+30 ML"));

    // Bugging Out
    new ChoiceSpoiler(
        285,
        "Gong",
        // Option...
        new ChoiceOption("+30% mysticality"),
        new ChoiceOption("+30 ML"),
        new ChoiceOption("+10% all stats"));

    // A Sweeping Generalization
    new ChoiceSpoiler(
        286,
        "Gong",
        // Option...
        new ChoiceOption("+50% item drops"),
        new ChoiceOption("+10% all stats"),
        new ChoiceOption("+30% mysticality"));

    // In the Frigid Aire
    new ChoiceSpoiler(
        287,
        "Gong",
        // Option...
        new ChoiceOption("+30 ML"),
        new ChoiceOption("+30% mysticality"),
        new ChoiceOption("+50% item drops"));

    // Our House
    new ChoiceSpoiler(
        288,
        "Gong",
        // Option...
        new ChoiceOption("+30 ML"),
        new ChoiceOption("+30% moxie"),
        new ChoiceOption("+10% all stats"));

    // Workin' For The Man
    new ChoiceSpoiler(
        289,
        "Gong",
        // Option...
        new ChoiceOption("+30 ML"),
        new ChoiceOption("+30% moxie"),
        new ChoiceOption("+50% item drops"));

    // The World's Not Fair
    new ChoiceSpoiler(
        290,
        "Gong",
        // Option...
        new ChoiceOption("+30% moxie"),
        new ChoiceOption("+10% all stats"),
        new ChoiceOption("+50% item drops"));

    // A Tight Squeeze
    new ChoiceAdventure(
        291,
        "Hobopolis",
        "Burnbarrel Blvd.",
        // Option...
        new ChoiceOption("jar of squeeze", "jar of squeeze"),
        SKIP_ADVENTURE);
    // A Tight Squeeze - jar of squeeze
    new ChoiceCost(291, new Cost(1, ItemPool.get(ItemPool.HOBO_NICKEL, -5)));

    // Cold Comfort
    new ChoiceAdventure(
        292,
        "Hobopolis",
        "Exposure Esplanade",
        // Option...
        new ChoiceOption("bowl of fishysoisse", "bowl of fishysoisse"),
        SKIP_ADVENTURE);
    // Cold Comfort - bowl of fishysoisse
    new ChoiceCost(292, new Cost(1, ItemPool.get(ItemPool.HOBO_NICKEL, -5)));

    // Flowers for You
    new ChoiceAdventure(
        293,
        "Hobopolis",
        "The Ancient Hobo Burial Ground",
        // Option...
        new ChoiceOption("deadly lampshade", "deadly lampshade"),
        SKIP_ADVENTURE);
    // Flowers for You - deadly lampshade
    new ChoiceCost(293, new Cost(1, ItemPool.get(ItemPool.HOBO_NICKEL, -5)));

    // Maybe It's a Sexy Snake!
    new ChoiceAdventure(
        294,
        "Hobopolis",
        "The Purple Light District",
        // Option...
        new ChoiceOption("lewd playing card", "lewd playing card"),
        SKIP_ADVENTURE);
    // Maybe It's a Sexy Snake! - lewd playing card
    new ChoiceCost(294, new Cost(1, ItemPool.get(ItemPool.HOBO_NICKEL, -5)));

    // Juicy!
    new ChoiceAdventure(
        295,
        "Hobopolis",
        "The Heap",
        // Option...
        new ChoiceOption("concentrated garbage juice", "concentrated garbage juice"),
        SKIP_ADVENTURE);
    // Juicy! - concentrated garbage juice
    new ChoiceCost(295, new Cost(1, ItemPool.get(ItemPool.HOBO_NICKEL, -5)));

    // Choice 296 is Pop!

    // Gravy Fairy Ring
    new ChoiceAdventure(
        297,
        "Dungeon",
        "Haiku Dungeon",
        // Option...
        new ChoiceOption("mushrooms"),
        new ChoiceOption("fairy gravy boat", "fairy gravy boat"),
        SKIP_ADVENTURE);

    // In the Shade
    new ChoiceAdventure(
        298,
        "The Sea",
        "An Octopus's Garden",
        // Option...
        new ChoiceOption("plant seeds"),
        SKIP_ADVENTURE);

    // Down at the Hatch
    new ChoiceAdventure(
        299,
        "The Sea",
        "The Wreck of the Edgar Fitzsimmons",
        // Option...
        new ChoiceOption("release creatures"),
        SKIP_ADVENTURE,
        new ChoiceOption("unlock tarnished luggage key adventure"));

    // Choice 300 is Merry Crimbo!
    // Choice 301 is And to All a Good Night
    // Choice 302 is You've Hit Bottom (Sauceror)
    // Choice 303 is You've Hit Bottom (Pastamancer)

    // A Vent Horizon
    new ChoiceAdventure(
        304,
        "The Sea",
        "The Marinara Trench",
        // Option...
        new ChoiceOption("bubbling tempura batter", "bubbling tempura batter"),
        SKIP_ADVENTURE);
    // A Vent Horizon
    new ChoiceCost(304, new Cost(1, new AdventureLongCountResult(AdventureResult.MP, -200)));

    // There is Sauce at the Bottom of the Ocean
    new ChoiceAdventure(
        305,
        "The Sea",
        "The Marinara Trench",
        // Option...
        new ChoiceOption("globe of Deep Sauce", "globe of Deep Sauce"),
        SKIP_ADVENTURE);
    // There is Sauce at the Bottom of the Ocean
    new ChoiceCost(305, new Cost(1, ItemPool.get(ItemPool.MERKIN_PRESSUREGLOBE, -1)));

    // Choice 306 is [Grandpa Mine Choice]
    // Choice 307 is Ode to the Sea
    // Choice 308 is Boxing the Juke

    // Barback
    new ChoiceAdventure(
        309,
        "The Sea",
        "The Dive Bar",
        // Option...
        new ChoiceOption("seaode", "seaode"),
        SKIP_ADVENTURE);

    // The Economist of Scales
    new ChoiceAdventure(
        310,
        "The Sea",
        "Madness Reef",
        // Option...
        new ChoiceOption("get 1 rough fish scale", 1, "rough fish scale"),
        new ChoiceOption("get 1 pristine fish scale", 2, "pristine fish scale"),
        new ChoiceOption("get multiple rough fish scales", 4, "rough fish scale"),
        new ChoiceOption("get multiple pristine fish scales", 5, "pristine fish scale"),
        new ChoiceOption("skip adventure", 6));
    // The Economist of Scales
    // This trades 10 dull fish scales in.
    new ChoiceCost(
        310,
        new Cost(1, ItemPool.get(ItemPool.DULL_FISH_SCALE, -10)),
        new Cost(2, ItemPool.get(ItemPool.ROUGH_FISH_SCALE, -10)),
        new Cost(4, ItemPool.get(ItemPool.DULL_FISH_SCALE, 10)),
        new Cost(5, ItemPool.get(ItemPool.ROUGH_FISH_SCALE, 10)));

    // Heavily Invested in Pun Futures
    new ChoiceAdventure(
        311,
        "The Sea",
        "Madness Reef",
        // Option...
        new ChoiceOption("The Economist of Scales"),
        SKIP_ADVENTURE);

    // Choice 312 is unknown
    // Choice 313 is unknown
    // Choice 314 is unknown
    // Choice 315 is unknown
    // Choice 316 is unknown

    // Choice 317 is No Man, No Hole
    // Choice 318 is C'mere, Little Fella
    // Choice 319 is Turtles of the Universe
    // Choice 320 is A Rolling Turtle Gathers No Moss
    // Choice 321 is Boxed In
    // Choice 322 is Capital!

    // Choice 323 is unknown
    // Choice 324 is unknown
    // Choice 325 is unknown

    // Showdown
    new ChoiceAdventure(
        326,
        "Clan Basement",
        "The Slime Tube",
        // Option...
        new ChoiceOption("enter combat with Mother Slime"),
        SKIP_ADVENTURE);

    // Choice 327 is Puttin' it on Wax
    // Choice 328 is Never Break the Chain
    // Choice 329 is Don't Be Alarmed, Now

    // A Shark's Chum
    new ChoiceAdventure(
        330,
        "Manor1",
        "Haunted Billiards Room",
        // Option...
        new ChoiceOption("stats and pool skill"),
        new ChoiceOption("cube of billiard chalk", "cube of billiard chalk"));

    // Choice 331 is Like That Time in Tortuga
    // Choice 332 is More eXtreme Than Usual
    // Choice 333 is Cleansing your Palette
    // Choice 334 is O Turtle Were Art Thou
    // Choice 335 is Blue Monday
    // Choice 336 is Jewel in the Rough

    // Engulfed!
    new ChoiceAdventure(
        337,
        "Clan Basement",
        "The Slime Tube",
        // Option...
        new ChoiceOption("+1 rusty -> slime-covered item conversion"),
        new ChoiceOption("raise area ML"),
        SKIP_ADVENTURE);

    // Choice 338 is Duel Nature
    // Choice 339 is Kick the Can
    // Choice 340 is Turtle in peril
    // Choice 341 is Nantucket Snapper
    // Choice 342 is The Horror...
    // Choice 343 is Turtles All The Way Around
    // Choice 344 is Silent Strolling
    // Choice 345 is Training Day

    // Choice 346 is Soup For You
    // Choice 347 is Yes, Soup For You
    // Choice 348 is Souped Up

    // The Primordial Directive
    new ChoiceAdventure(
        349,
        "Memories",
        "The Primordial Soup",
        // Option...
        new ChoiceOption("swim upwards"),
        new ChoiceOption("swim in circles"),
        new ChoiceOption("swim downwards"));

    // Soupercharged
    new ChoiceAdventure(
        350,
        "Memories",
        "The Primordial Soup",
        // Option...
        new ChoiceOption("Fight Cyrus"),
        SKIP_ADVENTURE);

    // Choice 351 is Beginner's Luck

    // Savior Faire
    new ChoiceAdventure(
        352,
        "Memories",
        "Seaside Megalopolis",
        // Option...
        new ChoiceOption("Moxie -> Bad Reception Down Here"),
        new ChoiceOption("Muscle -> A Diseased Procurer"),
        new ChoiceOption("Mysticality -> Give it a Shot"));

    // Bad Reception Down Here
    new ChoiceAdventure(
        353,
        "Memories",
        "Seaside Megalopolis",
        // Option...
        new ChoiceOption("Indigo Party Invitation", "Indigo Party Invitation"),
        new ChoiceOption("Violet Hunt Invitation", "Violet Hunt Invitation"));

    // You Can Never Be Too Rich or Too in the Future
    new ChoiceAdventure(
        354,
        "Memories",
        "Seaside Megalopolis",
        // Option...
        new ChoiceOption("Moxie"),
        new ChoiceOption("Serenity"));

    // I'm on the Hunt, I'm After You
    new ChoiceAdventure(
        355,
        "Memories",
        "Seaside Megalopolis",
        // Option...
        new ChoiceOption("Stats"),
        new ChoiceOption("Phairly Pheromonal"));

    // A Diseased Procurer
    new ChoiceAdventure(
        356,
        "Memories",
        "Seaside Megalopolis",
        // Option...
        new ChoiceOption("Blue Milk Club Card", "Blue Milk Club Card"),
        new ChoiceOption("Mecha Mayhem Club Card", "Mecha Mayhem Club Card"));

    // Painful, Circuitous Logic
    new ChoiceAdventure(
        357,
        "Memories",
        "Seaside Megalopolis",
        // Option...
        new ChoiceOption("Muscle"),
        new ChoiceOption("Nano-juiced"));

    // Brings All the Boys to the Blue Yard
    new ChoiceAdventure(
        358,
        "Memories",
        "Seaside Megalopolis",
        // Option...
        new ChoiceOption("Stats"),
        new ChoiceOption("Dance Interpreter"));

    // Choice 359 is unknown

    // Cavern Entrance
    new ChoiceAdventure(
        360,
        "Memories",
        "Jungles: Wumpus Cave",
        // Option...
        new ChoiceOption("skip adventure", 2));

    // Give it a Shot
    new ChoiceAdventure(
        361,
        "Memories",
        "Seaside Megalopolis",
        // Option...
        new ChoiceOption("'Smuggler Shot First' Button", "'Smuggler Shot First' Button"),
        new ChoiceOption("Spacefleet Communicator Badge", "Spacefleet Communicator Badge"));

    // A Bridge Too Far
    new ChoiceAdventure(
        362,
        "Memories",
        "Seaside Megalopolis",
        // Option...
        new ChoiceOption("Stats"),
        new ChoiceOption("Meatwise"));

    // Does This Bug You? Does This Bug You?
    new ChoiceAdventure(
        363,
        "Memories",
        "Seaside Megalopolis",
        // Option...
        new ChoiceOption("Mysticality"),
        new ChoiceOption("In the Saucestream"));

    // 451 Degrees! Burning Down the House!
    new ChoiceAdventure(
        364,
        "Memories",
        "Seaside Megalopolis",
        // Option...
        new ChoiceOption("Moxie"),
        new ChoiceOption("Supreme Being Glossary", "Supreme Being Glossary"),
        new ChoiceOption("Muscle"));

    // None Shall Pass
    new ChoiceAdventure(
        365,
        "Memories",
        "Seaside Megalopolis",
        // Option...
        new ChoiceOption("Muscle"),
        new ChoiceOption("multi-pass", "multi-pass"));

    // Entrance to the Forgotten City
    new ChoiceAdventure(
        366,
        "Memories",
        "Jungles: Forgotten City",
        // Option...
        new ChoiceOption("skip adventure", 2));

    // Choice 367 is Ancient Temple (unlocked)
    // Choice 368 is City Center
    // Choice 369 is North Side of the City
    // Choice 370 is East Side of the City
    // Choice 371 is West Side of the City
    // Choice 372 is An Ancient Well
    // Choice 373 is Northern Gate
    // Choice 374 is An Ancient Tower
    // Choice 375 is Northern Abandoned Building

    // Ancient Temple
    new ChoiceAdventure(
        376,
        "Memories",
        "Jungles: Ancient Temple",
        // Option...
        new ChoiceOption("Enter the Temple"),
        new ChoiceOption("leave"));

    // Choice 377 is Southern Abandoned Building
    // Choice 378 is Storehouse
    // Choice 379 is Northern Building (Basement)
    // Choice 380 is Southern Building (Upstairs)
    // Choice 381 is Southern Building (Basement)
    // Choice 382 is Catacombs Entrance
    // Choice 383 is Catacombs Junction
    // Choice 384 is Catacombs Dead-End
    // Choice 385 is Sore of an Underground Lake
    // Choice 386 is Catacombs Machinery

    // Choice 387 is Time Isn't Holding Up; Time is a Doughnut
    // Choice 388 is Extra Savoir Faire
    // Choice 389 is The Unbearable Supremeness of Being
    // Choice 390 is A Winning Pass
    // Choice 391 is OMG KAWAIII
    // Choice 392 is The Elements of Surprise . . .

    // The Collector
    new ChoiceAdventure(
        393,
        "Item-Driven",
        "big bumboozer marble",
        // Option...
        new ChoiceOption("1 of each marble -> 32768 Meat"),
        SKIP_ADVENTURE);

    // Choice 394 is Hellevator Music
    // Choice 395 is Rumble On

    // Woolly Scaly Bully
    new ChoiceAdventure(
        396,
        "The Sea",
        "Mer-kin Elementary School",
        // Option...
        new ChoiceOption("lose HP"),
        new ChoiceOption("lose HP"),
        new ChoiceOption("unlock janitor's closet"));

    // Bored of Education
    new ChoiceAdventure(
        397,
        "The Sea",
        "Mer-kin Elementary School",
        // Option...
        new ChoiceOption("lose HP"),
        new ChoiceOption("unlock the bathrooms"),
        new ChoiceOption("lose HP"));

    // A Mer-kin Graffiti
    new ChoiceAdventure(
        398,
        "The Sea",
        "Mer-kin Elementary School",
        // Option...
        new ChoiceOption("unlock teacher's lounge"),
        new ChoiceOption("lose HP"),
        new ChoiceOption("lose HP"));

    // The Case of the Closet
    new ChoiceAdventure(
        399,
        "The Sea",
        "Mer-kin Elementary School",
        // Option...
        new ChoiceOption("fight a Mer-kin monitor"),
        new ChoiceOption("Mer-kin sawdust", "Mer-kin sawdust"));

    // No Rest for the Room
    new ChoiceAdventure(
        400,
        "The Sea",
        "Mer-kin Elementary School",
        // Option...
        new ChoiceOption("fight a Mer-kin teacher"),
        new ChoiceOption("Mer-kin cancerstick", "Mer-kin cancerstick"));

    // Raising Cane
    new ChoiceAdventure(
        401,
        "The Sea",
        "Mer-kin Elementary School",
        // Option...
        new ChoiceOption("fight a Mer-kin punisher"),
        new ChoiceOption("Mer-kin wordquiz", "Mer-kin wordquiz"));

    // Don't Hold a Grudge
    new ChoiceAdventure(
        402,
        "Manor2",
        "Haunted Bathroom",
        // Option...
        new ChoiceOption("muscle substats"),
        new ChoiceOption("mysticality substats"),
        new ChoiceOption("moxie substats"));

    // Picking Sides
    new ChoiceAdventure(
        403,
        "The Sea",
        "Skate Park",
        // Option...
        new ChoiceOption("skate blade", "skate blade"),
        new ChoiceOption("brand new key", "brand new key"));

    // Choice 409 is The Island Barracks
    //	1 = only option
    // Choice 410 is A Short Hallway
    //	1 = left, 2 = right, 3 = exit
    // Choice 411 is Hallway Left
    //	1 = kitchen, 2 = dining room, 3 = storeroom, 4 = exit
    // Choice 412 is Hallway Right
    //	1 = bedroom, 2 = library, 3 = parlour, 4 = exit
    // Choice 413 is Kitchen
    //	1 = cupboards, 2 = pantry, 3 = fridges, 4 = exit
    // Choice 414 is Dining Room
    //	1 = tables, 2 = sideboard, 3 = china cabinet, 4 = exit
    // Choice 415 is Store Room
    //	1 = crates, 2 = workbench, 3 = gun cabinet, 4 = exit
    // Choice 416 is Bedroom
    //	1 = beds, 2 = dressers, 3 = bathroom, 4 = exit
    // Choice 417 is Library
    //	1 = bookshelves, 2 = chairs, 3 = chess set, 4 = exit
    // Choice 418 is Parlour
    //	1 = pool table, 2 = bar, 3 = fireplace, 4 = exit

    // Choice 423 is A Wrenching Encounter
    // Choice 424 is Get Your Bolt On, Michael
    // Choice 425 is Taking a Proper Gander
    // Choice 426 is It's Electric, Boogie-oogie-oogie
    // Choice 427 is A Voice Crying in the Crimbo Factory
    // Choice 428 is Disguise the Limit
    // Choice 429 is Diagnosis: Hypnosis
    // Choice 430 is Secret Agent Penguin
    // Choice 431 is Zapatos Con Crete
    // Choice 432 is Don We Now Our Bright Apparel
    // Choice 433 is Everything is Illuminated?
    // Choice 435 is Season's Beatings
    // Choice 436 is unknown
    // Choice 437 is Flying In Circles

    // From Little Acorns...
    new ChoiceCost(438, new Cost(1, ItemPool.get(ItemPool.UNDERWORLD_ACORN, -1)));

    // Choice 439 is unknown
    // Choice 440 is Puttin' on the Wax
    // Choice 441 is The Mad Tea Party

    // Choice 442 is A Moment of Reflection
    new ChoiceAdventure(
        442,
        "Rabbit Hole",
        "A Moment of Reflection",
        // Option...
        new ChoiceOption("Seal Clubber/Pastamancer/custard"),
        new ChoiceOption("Accordion Thief/Sauceror/comfit"),
        new ChoiceOption("Turtle Tamer/Disco Bandit/croqueteer"),
        new ChoiceOption("Ittah bittah hookah"),
        new ChoiceOption("Chessboard"),
        new ChoiceOption("nothing"));

    // Choice 443 is Chess Puzzle

    // Choice 444 is The Field of Strawberries (Seal Clubber)
    new ChoiceAdventure(
        444,
        "Rabbit Hole",
        "Reflection of Map (Seal Clubber)",
        // Option...
        new ChoiceOption("walrus ice cream", "walrus ice cream"),
        new ChoiceOption("yellow matter custard", "yellow matter custard"));

    // Choice 445 is The Field of Strawberries (Pastamancer)
    new ChoiceAdventure(
        445,
        "Rabbit Hole",
        "Reflection of Map (Pastamancer)",
        // Option...
        new ChoiceOption("eggman noodles", "eggman noodles"),
        new ChoiceOption("yellow matter custard", "yellow matter custard"));

    // Choice 446 is The Caucus Racetrack (Accordion Thief)
    new ChoiceAdventure(
        446,
        "Rabbit Hole",
        "Reflection of Map (Accordion Thief)",
        // Option...
        new ChoiceOption("missing wine", "missing wine"),
        new ChoiceOption("delicious comfit?", "delicious comfit?"));

    // Choice 447 is The Caucus Racetrack (Sauceror)
    new ChoiceAdventure(
        447,
        "Rabbit Hole",
        "Reflection of Map (Sauceror)",
        // Option...
        new ChoiceOption("Vial of <i>jus de larmes</i>", "Vial of <i>jus de larmes</i>"),
        new ChoiceOption("delicious comfit?", "delicious comfit?"));

    // Choice 448 is The Croquet Grounds (Turtle Tamer)
    new ChoiceAdventure(
        448,
        "Rabbit Hole",
        "Reflection of Map (Turtle Tamer)",
        // Option...
        new ChoiceOption("beautiful soup", "beautiful soup"),
        new ChoiceOption("fight croqueteer"));

    // Choice 449 is The Croquet Grounds (Disco Bandit)
    new ChoiceAdventure(
        449,
        "Rabbit Hole",
        "Reflection of Map (Disco Bandit)",
        // Option...
        new ChoiceOption("Lobster <i>qua</i> Grill", "Lobster <i>qua</i> Grill"),
        new ChoiceOption("fight croqueteer"));

    // Choice 450 is The Duchess' Cottage

    // Typographical Clutter
    new ChoiceAdventure(
        451,
        "Dungeon",
        "Greater-Than Sign",
        // Option...
        new ChoiceOption("left parenthesis", "left parenthesis"),
        new ChoiceOption("moxie, alternately lose then gain meat"),
        new ChoiceOption("plus sign, then muscle", "plus sign"),
        new ChoiceOption("mysticality substats"),
        new ChoiceOption("get teleportitis"));

    // Leave a Message and I'll Call You Back
    new ChoiceAdventure(
        452,
        "Jacking",
        "Small-O-Fier",
        // Option...
        new ChoiceOption("combat"),
        new ChoiceOption("tiny fly glasses", "tiny fly glasses"),
        new ChoiceOption("fruit"));

    // Getting a Leg Up
    new ChoiceAdventure(
        453,
        "Jacking",
        "Small-O-Fier",
        // Option...
        new ChoiceOption("combat"),
        new ChoiceOption("stats"),
        new ChoiceOption("hair of the calf", "hair of the calf"));

    // Just Like the Ocean Under the Moon
    new ChoiceAdventure(
        454,
        "Jacking",
        "Small-O-Fier",
        // Option...
        new ChoiceOption("combat"),
        new ChoiceOption("HP and MP"));

    // Double Trouble in the Stubble
    new ChoiceAdventure(
        455,
        "Jacking",
        "Small-O-Fier",
        // Option...
        new ChoiceOption("stats"),
        new ChoiceOption("quest item"));

    // Made it, Ma! Top of the World!
    new ChoiceAdventure(
        456,
        "Jacking",
        "Huge-A-Ma-tron",
        // Option...
        new ChoiceOption("combat"),
        new ChoiceOption("Hurricane Force"),
        new ChoiceOption("a dance upon the palate", "a dance upon the palate"),
        new ChoiceOption("stats"));

    // Choice 457 is Oh, No! Five-Oh!
    // Choice 458 is ... Grow Unspeakable Horrors
    // Choice 459 is unknown
    // Choice 460 is Space Trip (Bridge)
    // Choice 461 is Space Trip (Navigation)
    // Choice 462 is Space Trip (Diagnostics)
    // Choice 463 is Space Trip (Alpha Quadrant)
    // Choice 464 is Space Trip (Beta Quadrant)
    // Choice 465 is Space Trip (Planet)
    // Choice 466 is unknown
    // Choice 467 is Space Trip (Combat)
    // Choice 468 is Space Trip (Starbase Hub)
    // Choice 469 is Space Trip (General Store)
    // Choice 470 is Space Trip (Military Surplus Store)
    // Choice 471 is DemonStar
    // Choice 472 is Space Trip (Astrozorian Trade Vessel: Alpha)
    // Choice 473 is Space Trip (Murderbot Miner: first encounter)
    // Choice 474 is Space Trip (Slavers: Alpha)
    // Choice 475 is Space Trip (Astrozorian Trade Vessel: Beta)
    // Choice 476 is Space Trip (Astrozorian Trade Vessel: Gamma)
    // Choice 477 is Space Trip (Gamma Quadrant)
    // Choice 478 is Space Trip (The Source)
    // Choice 479 is Space Trip (Slavers: Beta)
    // Choice 480 is Space Trip (Scadian ship)
    // Choice 481 is Space Trip (Hipsterian ship)
    // Choice 482 is Space Trip (Slavers: Gamma)
    // Choice 483 is Space Trip (Scadian Homeworld)
    // Choice 484 is Space Trip (End)
    // Choice 485 is Fighters of Fighting
    // Choice 486 is Dungeon Fist!
    // Choice 487 is unknown
    // Choice 488 is Meteoid (Bridge)
    // Choice 489 is Meteoid (SpaceMall)
    // Choice 490 is Meteoid (Underground Complex)
    // Choice 491 is Meteoid (End)
    // Choice 492 is unknown
    // Choice 493 is unknown
    // Choice 494 is unknown
    // Choice 495 is unknown

    // Crate Expectations
    new ChoiceAdventure(
        496,
        "Woods",
        "Typical Tavern",
        // Option...
        new ChoiceOption("3 bottles of basic booze"),
        new ChoiceOption("Get rid of crate without spending an adventure"));

    // Choice 497 is SHAFT!
    // Choice 498 is unknown
    // Choice 499 is unknown
    // Choice 500 is unknown
    // Choice 501 is unknown

    // Choice 502 is Arboreal Respite

    // The Road Less Traveled
    new ChoiceSpoiler(
        503,
        "Spooky Forest",
        // Option...
        new ChoiceOption("gain some meat"),
        new ChoiceOption("gain stakes or trade vampire hearts", "wooden stakes"),
        new ChoiceOption("gain spooky sapling or trade bar skins", "spooky sapling"));

    // Tree's Last Stand
    new ChoiceSpoiler(
        504,
        "Spooky Forest",
        // Option...
        new ChoiceOption("bar skin", "bar skin"),
        new ChoiceOption("bar skins", "bar skin"),
        new ChoiceOption("buy spooky sapling", "spooky sapling"),
        SKIP_ADVENTURE);
    // Tree's Last Stand
    new ChoiceCost(
        504,
        new Cost(1, ItemPool.get(ItemPool.BAR_SKIN, -1)),
        new Cost(2, ItemPool.get(ItemPool.BAR_SKIN, 1)),
        new Cost(3, new AdventureResult(AdventureResult.MEAT, -100)));

    // Consciousness of a Stream
    new ChoiceSpoiler(
        505,
        "Spooky Forest",
        // Option...
        new ChoiceOption("gain mosquito larva then 3 spooky mushrooms", "mosquito larva"),
        new ChoiceOption("gain 300 meat & tree-holed coin then nothing"),
        new ChoiceOption("fight a spooky vampire"));

    // Through Thicket and Thinnet
    new ChoiceSpoiler(
        506,
        "Spooky Forest",
        // Option...
        new ChoiceOption("gain a starter item"),
        new ChoiceOption("gain Spooky-Gro fertilizer", "Spooky-Gro fertilizer"),
        new ChoiceOption("gain spooky temple map", "spooky temple map"),
        new ChoiceOption("gain fake blood", "fake blood"));

    // O Lith, Mon
    new ChoiceSpoiler(
        507,
        "Spooky Forest",
        // Option...
        new ChoiceOption("gain Spooky Temple map"),
        SKIP_ADVENTURE,
        SKIP_ADVENTURE);
    // O Lith, Mon
    new ChoiceCost(507, new Cost(1, ItemPool.get(ItemPool.TREE_HOLED_COIN, -1)));

    // Choice 508 is Pants-Gazing
    // Choice 509 is Of Course!
    // Choice 510 is Those Who Came Before You

    // If it's Tiny, is it Still a Mansion?
    new ChoiceAdventure(
        511,
        "Woods",
        "Typical Tavern",
        // Option...
        new ChoiceOption("Baron von Ratsworth"),
        SKIP_ADVENTURE);

    // Hot and Cold Running Rats
    new ChoiceAdventure(
        512,
        "Woods",
        "Typical Tavern",
        // Option...
        new ChoiceOption("fight"),
        SKIP_ADVENTURE);

    // Staring Down the Barrel
    new ChoiceAdventure(
        513,
        "Woods",
        "Typical Tavern",
        // Option...
        new ChoiceOption("3-5 ice-cold Willers"),
        new ChoiceOption("Get rid of crate without spending an adventure"));

    // 1984 Had Nothing on This Cellar
    new ChoiceAdventure(
        514,
        "Woods",
        "Typical Tavern",
        // Option...
        new ChoiceOption("3-5 rat whiskers and smiling rat (sometimes)"),
        new ChoiceOption("Get rid of crate without spending an adventure"));

    // A Rat's Home...
    new ChoiceAdventure(
        515,
        "Woods",
        "Typical Tavern",
        // Option...
        new ChoiceOption("3 bottles of tequila"),
        new ChoiceOption("Get rid of crate without spending an adventure"));

    // Choice 516 is unknown
    // Choice 517 is Mr. Alarm, I Presarm

    // Clear and Present Danger
    new ChoiceAdventure(
        518,
        "Crimbo10",
        "Elf Alley",
        // Option...
        new ChoiceOption("enter combat with Uncle Hobo"),
        SKIP_ADVENTURE);

    // What a Tosser
    new ChoiceAdventure(
        519,
        "Crimbo10",
        "Elf Alley",
        // Option...
        new ChoiceOption("gift-a-pult", "gift-a-pult"),
        SKIP_ADVENTURE);
    // What a Tosser - gift-a-pult
    new ChoiceCost(519, new Cost(1, ItemPool.get(ItemPool.HOBO_NICKEL, -50)));

    // Choice 520 is A Show-ho-ho-down
    // Choice 521 is A Wicked Buzz

    // Welcome to the Footlocker
    new ChoiceAdventure(
        522,
        "Knob",
        "Cobb's Knob Barracks",
        // Option...
        new ChoiceOption("outfit piece or donut"),
        SKIP_ADVENTURE);

    // Death Rattlin'
    new ChoiceAdventure(
        523,
        "Cyrpt",
        "Defiled Cranny",
        // Option...
        new ChoiceOption("small meat boost"),
        new ChoiceOption("stats & HP & MP"),
        new ChoiceOption("can of Ghuol-B-Gone&trade;", "can of Ghuol-B-Gone&trade;"),
        new ChoiceOption("fight swarm of ghuol whelps"),
        SKIP_ADVENTURE);

    // Choice 524 is The Adventures of Lars the Cyberian
    // Choice 525 is Fiddling with a Puzzle
    // Choice 526 is unknown

    // Choice 527 is The Haert of Darkness
    new ChoiceAdventure(
        527,
        "Cyrpt",
        "Haert of the Cyrpt",
        // Option...
        new ChoiceOption("fight the Bonerdagon"),
        SKIP_ADVENTURE);

    // Choice 528 is It Was Then That a Hideous Monster Carried You

    // A Swarm of Yeti-Mounted Skeletons
    new ChoiceAdventure(
        529,
        "Events",
        "Skeleton Swarm",
        // Option...
        new ChoiceOption("Weapon Damage"),
        new ChoiceOption("Spell Damage"),
        new ChoiceOption("Ranged Damage"));

    // It Was Then That... Aaaaaaaah!
    new ChoiceAdventure(
        530,
        "Events",
        "Icy Peak",
        // Option...
        new ChoiceOption("hideous egg", "hideous egg"),
        new ChoiceOption("skip the adventure"));

    // The Bonewall Is In
    new ChoiceAdventure(
        531,
        "Events",
        "Bonewall",
        // Option...
        new ChoiceOption("Item Drop"),
        new ChoiceOption("HP Bonus"));

    // You'll Sink His Battleship
    new ChoiceAdventure(
        532,
        "Events",
        "Battleship",
        // Option...
        new ChoiceOption("Class Skills"),
        new ChoiceOption("Accordion Thief Songs"));

    // Train, Train, Choo-Choo Train
    new ChoiceAdventure(
        533,
        "Events",
        "Supply Train",
        // Option...
        new ChoiceOption("Meat Drop"),
        new ChoiceOption("Pressure Penalty Modifiers"));

    // That's No Bone Moon...
    new ChoiceAdventure(
        534,
        "Events",
        "Bone Star",
        // Option...
        new ChoiceOption("Torpedos", "photoprotoneutron torpedo"),
        new ChoiceOption("Initiative"),
        new ChoiceOption("Monster Level"));

    // Deep Inside Ronald, Baby
    new ChoiceAdventure(535, "Spaaace", "Deep Inside Ronald", SafetyShelterManager.RonaldGoals);

    // Deep Inside Grimace, Bow Chick-a Bow Bow
    new ChoiceAdventure(536, "Spaaace", "Deep Inside Grimace", SafetyShelterManager.GrimaceGoals);

    // Choice 537 is Play Porko!
    // Choice 538 is Big-Time Generator
    // Choice 539 is An E.M.U. for Y.O.U.
    // Choice 540 is Big-Time Generator - game board
    // Choice 541 is unknown
    // Choice 542 is Now's Your Pants!  I Mean... Your Chance!
    // Choice 543 is Up In Their Grill
    // Choice 544 is A Sandwich Appears!
    // Choice 545 is unknown

    // Interview With You
    new ChoiceAdventure(546, "Item-Driven", "Interview With You", VampOutManager.VampOutGoals);

    // Behind Closed Doors
    new ChoiceAdventure(
        548,
        "Events",
        "Sorority House Necbromancer",
        // Option...
        new ChoiceOption("enter combat with The Necbromancer"),
        SKIP_ADVENTURE);

    // Dark in the Attic
    new ChoiceSpoiler(
        549,
        "Events",
        "Dark in the Attic",
        // Option...
        new ChoiceOption("staff guides", "Haunted Sorority House staff guide"),
        new ChoiceOption("ghost trap", "ghost trap"),
        new ChoiceOption("raise area ML"),
        new ChoiceOption("lower area ML"),
        new ChoiceOption("mass kill werewolves with silver shotgun shell", "silver shotgun shell"));

    // The Unliving Room
    new ChoiceSpoiler(
        550,
        "Events",
        "The Unliving Room",
        // Option...
        new ChoiceOption("raise area ML"),
        new ChoiceOption("lower area ML"),
        new ChoiceOption("mass kill zombies with chainsaw chain", "chainsaw chain"),
        new ChoiceOption("mass kill skeletons with funhouse mirror", "funhouse mirror"),
        new ChoiceOption("get costume item"));

    // Debasement
    new ChoiceSpoiler(
        551,
        "Events",
        "Debasement",
        // Option...
        new ChoiceOption("Prop Deportment"),
        new ChoiceOption("mass kill vampires with plastic vampire fangs"),
        new ChoiceOption("raise area ML"),
        new ChoiceOption("lower area ML"));

    // Prop Deportment
    new ChoiceSpoiler(
        552,
        "Events",
        "Prop Deportment",
        // Option...
        new ChoiceOption("chainsaw chain", "chainsaw chain"),
        new ChoiceOption("create a silver shotgun shell", "silver shotgun shell"),
        new ChoiceOption("funhouse mirror", "funhouse mirror"));

    // Relocked and Reloaded
    new ChoiceSpoiler(
        553,
        "Events",
        "Relocked and Reloaded",
        // Option...
        new ChoiceOption("", "Maxwell's Silver hammer"),
        new ChoiceOption("", "silver tongue charrrm bracelet"),
        new ChoiceOption("", "silver cheese-slicer"),
        new ChoiceOption("", "silver shrimp fork"),
        new ChoiceOption("", "silver pat&eacute; knife"),
        new ChoiceOption("exit adventure"));

    // Behind the Spooky Curtain
    new ChoiceSpoiler(
        554,
        "Events",
        "Behind the Spooky Curtain",
        // Option...
        new ChoiceOption("staff guides, ghost trap, kill werewolves"),
        new ChoiceOption("kill zombies, kill skeletons, costume item"),
        new ChoiceOption("chainsaw chain, silver item, funhouse mirror, kill vampires"));

    // More Locker Than Morlock
    new ChoiceAdventure(
        556,
        "McLarge",
        "Itznotyerzitz Mine",
        // Option...
        new ChoiceOption("get an outfit piece"),
        SKIP_ADVENTURE);

    // Gingerbread Homestead
    new ChoiceAdventure(
        557,
        "The Candy Diorama",
        "Gingerbread Homestead",
        // Option...
        new ChoiceOption("get candies"),
        new ChoiceOption("licorice root", "licorice root"),
        new ChoiceOption("skip adventure or make a lollipop stick item", "lollipop stick"));

    // Tool Time
    new ChoiceAdventure(
        558,
        "The Candy Diorama",
        "Tool Time",
        // Option...
        new ChoiceOption("sucker bucket", "sucker bucket"),
        new ChoiceOption("sucker kabuto", "sucker kabuto"),
        new ChoiceOption("sucker hakama", "sucker hakama"),
        new ChoiceOption("sucker tachi", "sucker tachi"),
        new ChoiceOption("sucker scaffold", "sucker scaffold"),
        SKIP_ADVENTURE);

    // Fudge Mountain Breakdown
    new ChoiceAdventure(
        559,
        "The Candy Diorama",
        "Fudge Mountain Breakdown",
        // Option...
        new ChoiceOption("fudge lily", "fudge lily"),
        new ChoiceOption("fight a swarm of fudgewasps or skip adventure"),
        new ChoiceOption("frigid fudgepuck or skip adventure", "frigid fudgepuck"),
        new ChoiceOption("superheated fudge or skip adventure", "superheated fudge"));

    // Foreshadowing Demon!
    new ChoiceAdventure(
        560,
        "Suburbs",
        "The Clumsiness Grove",
        // Option...
        new ChoiceOption("head towards boss"),
        SKIP_ADVENTURE);

    // You Must Choose Your Destruction!
    new ChoiceAdventure(
        561,
        "Suburbs",
        "The Clumsiness Grove",
        // Option...
        new ChoiceOption("The Thorax"),
        new ChoiceOption("The Bat in the Spats"));

    // Choice 562 is You're the Fudge Wizard Now, Dog

    // A Test of your Mettle
    new ChoiceAdventure(
        563,
        "Suburbs",
        "The Clumsiness Grove",
        // Option...
        new ChoiceOption("Fight Boss"),
        SKIP_ADVENTURE);

    // A Maelstrom of Trouble
    new ChoiceAdventure(
        564,
        "Suburbs",
        "The Maelstrom of Lovers",
        // Option...
        new ChoiceOption("head towards boss"),
        SKIP_ADVENTURE);

    // To Get Groped or Get Mugged?
    new ChoiceAdventure(
        565,
        "Suburbs",
        "The Maelstrom of Lovers",
        // Option...
        new ChoiceOption("The Terrible Pinch"),
        new ChoiceOption("Thug 1 and Thug 2"));

    // A Choice to be Made
    new ChoiceAdventure(
        566,
        "Suburbs",
        "The Maelstrom of Lovers",
        // Option...
        new ChoiceOption("Fight Boss"),
        SKIP_ADVENTURE);

    // You May Be on Thin Ice
    new ChoiceAdventure(
        567,
        "Suburbs",
        "The Glacier of Jerks",
        // Option...
        new ChoiceOption("Fight Boss"),
        SKIP_ADVENTURE);

    // Some Sounds Most Unnerving
    new ChoiceAdventure(
        568,
        "Suburbs",
        "The Glacier of Jerks",
        // Option...
        new ChoiceOption("Mammon the Elephant"),
        new ChoiceOption("The Large-Bellied Snitch"));

    // One More Demon to Slay
    new ChoiceAdventure(
        569,
        "Suburbs",
        "The Glacier of Jerks",
        // Option...
        new ChoiceOption("head towards boss"),
        SKIP_ADVENTURE);

    // Choice 571 is Your Minstrel Vamps
    // Choice 572 is Your Minstrel Clamps
    // Choice 573 is Your Minstrel Stamps
    // Choice 574 is The Minstrel Cycle Begins

    // Duffel on the Double
    new ChoiceAdventure(
        575,
        "McLarge",
        "eXtreme Slope",
        // Option...
        new ChoiceOption("get an outfit piece"),
        new ChoiceOption("jar of frostigkraut", "jar of frostigkraut"),
        SKIP_ADVENTURE,
        new ChoiceOption("lucky pill", "lucky pill"));

    // Choice 576 is Your Minstrel Camps
    // Choice 577 is Your Minstrel Scamp
    // Choice 578 is End of the Boris Road

    // Such Great Heights
    new ChoiceAdventure(
        579,
        "Woods",
        "Hidden Temple Heights",
        // Option...
        new ChoiceOption("mysticality substats"),
        new ChoiceOption("Nostril of the Serpent then skip adventure", "Nostril of the Serpent"),
        new ChoiceOption("gain 3 adv then skip adventure"));

    // Choice 580 is The Hidden Heart of the Hidden Temple (4 variations)

    // Such Great Depths
    new ChoiceAdventure(
        581,
        "Woods",
        "Hidden Temple Depths",
        // Option...
        new ChoiceOption("glowing fungus", "glowing fungus"),
        new ChoiceOption("+15 mus/mys/mox then skip adventure"),
        new ChoiceOption("fight clan of cave bars"));

    // Fitting In
    new ChoiceAdventure(
        582,
        "Woods",
        "Hidden Temple",
        // Option...
        new ChoiceOption("Such Great Heights"),
        new ChoiceOption("heart of the Hidden Temple"),
        new ChoiceOption("Such Great Depths"));

    // Confusing Buttons
    new ChoiceSpoiler(
        583,
        "Woods",
        "Hidden Temple",
        // Option...
        new ChoiceOption("Press a random button"));

    // Unconfusing Buttons
    new ChoiceAdventure(
        584,
        "Woods",
        "Hidden Temple",
        // Option...
        new ChoiceOption("Hidden Temple (Stone) - muscle substats"),
        new ChoiceOption("Hidden Temple (Sun) - gain ancient calendar fragment"),
        new ChoiceOption("Hidden Temple (Gargoyle) - MP"),
        new ChoiceOption("Hidden Temple (Pikachutlotal) - Hidden City unlock"));

    // Choice 585 is Screwing Around!
    // Choice 586 is All We Are Is Radio Huggler

    // Choice 588 is Machines!
    // Choice 589 is Autopsy Auturvy
    // Choice 590 is Not Alone In The Dark

    // A Lost Room
    new ChoiceAdventure(
        594,
        "Item-Driven",
        "Lost Key",
        // Option...
        new ChoiceOption("lost glasses", "lost glasses"),
        new ChoiceOption("lost comb", "lost comb"),
        new ChoiceOption("lost pill bottle", "lost pill bottle"));

    // Fire! I... have made... fire!
    new ChoiceAdventure(
        595,
        "Item-Driven",
        "CSA fire-starting kit",
        // Option...
        new ChoiceOption("pvp fights"),
        new ChoiceOption("hp/mp regen"));

    // Choice 596 is Dawn of the D'oh

    // Cake Shaped Arena
    new ChoiceAdventure(
        597,
        "Item-Driven",
        "Reagnimated Gnome",
        // Option...
        new ChoiceOption("gnomish swimmer's ears (underwater)", "gnomish swimmer's ears"),
        new ChoiceOption("gnomish coal miner's lung (block)", "gnomish coal miner's lung"),
        new ChoiceOption("gnomish tennis elbow (damage)", "gnomish tennis elbow"),
        new ChoiceOption("gnomish housemaid's kgnee (gain advs)", "gnomish housemaid's kgnee"),
        new ChoiceOption("gnomish athlete's foot (delevel)", "gnomish athlete's foot"));

    // Choice 598 is Recruitment Jive
    // Choice 599 is A Zombie Master's Bait
    // Choice 600 is Summon Minion
    // Choice 601 is Summon Horde
    // Choice 602 is Behind the Gash

    // Skeletons and The Closet
    new ChoiceAdventure(
        603,
        "Item-Driven",
        "Skeleton",
        // Option...
        new ChoiceOption("warrior (dmg, delevel)"),
        new ChoiceOption("cleric (hot dmg, hp)"),
        new ChoiceOption("wizard (cold dmg, mp)"),
        new ChoiceOption("rogue (dmg, meat)"),
        new ChoiceOption("buddy (delevel, exp)"),
        new ChoiceOption("ignore this adventure"));

    // Choice 604 is unknown
    // Choice 605 is Welcome to the Great Overlook Lodge
    // Choice 606 is Lost in the Great Overlook Lodge
    // Choice 607 is Room 237
    // Choice 608 is Go Check It Out!
    // Choice 609 is There's Always Music In the Air
    // Choice 610 is To Catch a Killer
    // Choice 611 is The Horror... (A-Boo Peak)
    // Choice 612 is Behind the world there is a door...
    // Choice 613 is Behind the door there is a fog
    // Choice 614 is Near the fog there is an... anvil?
    // Choice 615 is unknown

    // Choice 616 is He Is the Arm, and He Sounds Like This
    // Choice 617 is Now It's Dark
    // Choice 618 is Cabin Fever
    // Choice 619 is To Meet a Gourd
    // Choice 620 is A Blow Is Struck!
    // Choice 621 is Hold the Line!
    // Choice 622 is The Moment of Truth
    // Choice 623 is Return To the Fray!
    // Choice 624 is Returning to Action
    // Choice 625 is The Table
    // Choice 626 is Super Crimboman Crimbo Type is Go!
    // Choice 627 is unknown
    // Choice 628 is unknown
    // Choice 629 is unknown
    // Choice 630 is unknown
    // Choice 631 is unknown
    // Choice 632 is unknown
    // Choice 633 is ChibiBuddy&trade;
    // Choice 634 is Goodbye Fnord
    // Choice 635 is unknown
    // Choice 636 is unknown
    // Choice 637 is unknown
    // Choice 638 is unknown
    // Choice 639 is unknown

    // Choice 640 is Tailor the Snow Suit
    new ChoiceAdventure(
        640,
        "Item-Driven",
        "Snow Suit",
        // Option...
        new ChoiceOption("Familiar does physical damage"),
        new ChoiceOption("Familiar does cold damage"),
        new ChoiceOption("+10% item drops, can drop carrot nose"),
        new ChoiceOption("Heals 1-20 HP after combat"),
        new ChoiceOption("Restores 1-10 MP after combat"));

    // Choice 641 is Stupid Pipes.
    // Choice 642 is You're Freaking Kidding Me
    // Choice 643 is Great. A Stupid Door. What Next?
    // Choice 644 is Snakes.
    // Choice 645 is So... Many... Skulls...
    // Choice 646 is Oh No... A Door...
    // Choice 647 is A Stupid Dummy. Also, a Straw Man.
    // Choice 648 is Slings and Arrows
    // Choice 649 is A Door. Figures.
    // Choice 650 is This Is Your Life. Your Horrible, Horrible Life.
    // Choice 651 is The Wall of Wailing
    // Choice 652 is A Door. Too Soon...
    // Choice 653 is unknown
    // Choice 654 is Courier? I don't even...
    // Choice 655 is They Have a Fight, Triangle Loses
    // Choice 656 is Wheels Within Wheel

    // You Grind 16 Rats, and Whaddya Get?
    new ChoiceAdventure(
        657,
        "Psychoses",
        "Chinatown Tenement",
        // Option...
        new ChoiceOption("Fight Boss"),
        SKIP_ADVENTURE);

    // Choice 658 is Debasement
    // Choice 659 is How Does a Floating Platform Even Work?
    // Choice 660 is It's a Place Where Books Are Free
    // Choice 661 is Sphinx For the Memories
    // Choice 662 is Think or Thwim
    // Choice 663 is When You're a Stranger
    // Choice 664 is unknown
    // Choice 665 is A Gracious Maze
    // Choice 666 is unknown
    // Choice 667 is unknown
    // Choice 668 is unknown

    // The Fast and the Furry-ous
    new ChoiceAdventure(
        669,
        "Beanstalk",
        "Basement Furry",
        // Option...
        new ChoiceOption("Open Ground Floor with titanium umbrella, otherwise Neckbeard Choice"),
        new ChoiceOption("200 Moxie substats"),
        new ChoiceOption("???"),
        new ChoiceOption("skip adventure and guarantee this adventure will reoccur"));

    // You Don't Mess Around with Gym
    new ChoiceAdventure(
        670,
        "Beanstalk",
        "Basement Fitness",
        // Option...
        new ChoiceOption("massive dumbbell, then skip adventure", "massive dumbbell"),
        new ChoiceOption("Muscle stats"),
        new ChoiceOption("Items"),
        new ChoiceOption("Open Ground Floor with amulet, otherwise skip"),
        new ChoiceOption("skip adventure and guarantee this adventure will reoccur"));

    // Out in the Open Source
    new ChoiceAdventure(
        671,
        "Beanstalk",
        "Basement Neckbeard",
        // Option...
        new ChoiceOption(
            "With massive dumbbell, open Ground Floor, otherwise skip adventure",
            "massive dumbbell"),
        new ChoiceOption("200 Mysticality substats"),
        new ChoiceOption("O'RLY manual, open sauce"),
        new ChoiceOption("Fitness Choice"));

    // There's No Ability Like Possibility
    new ChoiceAdventure(
        672,
        "Beanstalk",
        "Ground Possibility",
        // Option...
        new ChoiceOption("3 random items"),
        new ChoiceOption("Nothing Is Impossible"),
        SKIP_ADVENTURE);

    // Putting Off Is Off-Putting
    new ChoiceAdventure(
        673,
        "Beanstalk",
        "Ground Procrastination",
        // Option...
        new ChoiceOption(
            "very overdue library book, then skip adventure", "very overdue library book"),
        new ChoiceOption("Trash-Wrapped"),
        SKIP_ADVENTURE);

    // Huzzah!
    new ChoiceAdventure(
        674,
        "Beanstalk",
        "Ground Renaissance",
        // Option...
        new ChoiceOption("pewter claymore, then skip adventure", "pewter claymore"),
        new ChoiceOption("Pretending to Pretend"),
        SKIP_ADVENTURE);

    // Melon Collie and the Infinite Lameness
    new ChoiceAdventure(
        675,
        "Beanstalk",
        "Top Goth",
        // Option...
        new ChoiceOption("Fight a Goth Giant"),
        new ChoiceOption("complete quest", "drum 'n' bass 'n' drum 'n' bass record"),
        new ChoiceOption("3 thin black candles", "thin black candle"),
        new ChoiceOption("Steampunk Choice"));

    // Flavor of a Raver
    new ChoiceAdventure(
        676,
        "Beanstalk",
        "Top Raver",
        // Option...
        new ChoiceOption("Fight a Raver Giant"),
        new ChoiceOption("Restore 1000 hp & mp"),
        new ChoiceOption(
            "drum 'n' bass 'n' drum 'n' bass record, then skip adventure",
            "drum 'n' bass 'n' drum 'n' bass record"),
        new ChoiceOption("Punk Rock Choice"));

    // Copper Feel
    new ChoiceAdventure(
        677,
        "Beanstalk",
        "Top Steampunk",
        // Option...
        new ChoiceOption(
            "With model airship, complete quest, otherwise fight Steampunk Giant", "model airship"),
        new ChoiceOption(
            "steam-powered model rocketship, then skip adventure",
            "steam-powered model rocketship"),
        new ChoiceOption("brass gear", "brass gear"),
        new ChoiceOption("Goth Choice"));

    // Yeah, You're for Me, Punk Rock Giant
    new ChoiceAdventure(
        678,
        "Beanstalk",
        "Top Punk Rock",
        // Option...
        new ChoiceOption("Wearing mohawk wig, turn wheel, otherwise fight Punk Rock Giant"),
        new ChoiceOption("500 meat"),
        new ChoiceOption("Steampunk Choice"),
        new ChoiceOption("Raver Choice"));

    // Choice 679 is Keep On Turnin' the Wheel in the Sky
    // Choice 680 is Are you a Man or a Mouse?
    // Choice 681 is F-F-Fantastic!
    // Choice 682 is Now Leaving Jarlsberg, Population You

    // Choice 686 is Of Might and Magic

    // Choice 689 is The Final Chest
    new ChoiceAdventure(
        689,
        "Dungeon",
        "Daily Dungeon: Chest 3",
        // Option...
        new ChoiceOption("Get fat loot token"));

    // The First Chest Isn't the Deepest.
    new ChoiceAdventure(
        690,
        "Dungeon",
        "Daily Dungeon: Chest 1",
        // Option...
        new ChoiceOption("Get item"),
        new ChoiceOption("Skip to 8th chamber, no turn spent"),
        new ChoiceOption("Skip to 6th chamber, no turn spent"));

    // Second Chest
    new ChoiceAdventure(
        691,
        "Dungeon",
        "Daily Dungeon: Chest 2",
        // Option...
        new ChoiceOption("Get item"),
        new ChoiceOption("Skip to 13th chamber, no turn spent"),
        new ChoiceOption("Skip to 11th chamber, no turn spent"));

    // Choice 692 is I Wanna Be a Door

    // It's Almost Certainly a Trap
    new ChoiceAdventure(
        693,
        "Dungeon",
        "Daily Dungeon: Traps",
        // Option...
        new ChoiceOption("Suffer elemental damage, get stats"),
        new ChoiceOption("Avoid trap with eleven-foot pole, no turn spent"),
        new ChoiceOption("Leave, no turn spent"));

    // Choice 695 is A Drawer of Chests

    // Choice 696 is Stick a Fork In It
    new ChoiceAdventure(
        696,
        "Le Marais D&egrave;gueulasse",
        "Edge of the Swamp",
        // Option...
        new ChoiceOption("unlock The Dark and Spooky Swamp"),
        new ChoiceOption("unlock The Wildlife Sanctuarrrrrgh"));

    // Choice 697 is Sophie's Choice
    new ChoiceAdventure(
        697,
        "Le Marais D&egrave;gueulasse",
        "Dark and Spooky Swamp",
        // Option...
        new ChoiceOption("unlock The Corpse Bog"),
        new ChoiceOption("unlock The Ruined Wizard Tower"));

    // Choice 698 is From Bad to Worst
    new ChoiceAdventure(
        698,
        "Le Marais D&egrave;gueulasse",
        "Wildlife Sanctuarrrrrgh",
        // Option...
        new ChoiceOption("unlock Swamp Beaver Territory"),
        new ChoiceOption("unlock The Weird Swamp Village"));

    // Choice 701 is Ators Gonna Ate
    new ChoiceAdventure(
        701,
        "The Sea",
        "Mer-kin Gymnasium",
        // Option...
        new ChoiceOption("get an item"),
        SKIP_ADVENTURE);

    // Choice 703 is Mer-kin dreadscroll
    // Choice 704 is Playing the Catalog Card

    // Choice 705 is Halls Passing in the Night
    new ChoiceAdventure(
        705,
        "The Sea",
        "Mer-kin Elementary School",
        // Option...
        new ChoiceOption("fight a Mer-kin spectre"),
        new ChoiceOption("Mer-kin sawdust", "Mer-kin sawdust"),
        new ChoiceOption("Mer-kin cancerstick", "Mer-kin cancerstick"),
        new ChoiceOption("Mer-kin wordquiz", "Mer-kin wordquiz"));

    //     Shub-Jigguwatt (Violence) path
    // Choice 706 is In The Temple of Violence, Shine Like Thunder
    // Choice 707 is Flex Your Pecs in the Narthex
    // Choice 708 is Don't Falter at the Altar
    // Choice 709 is You Beat Shub to a Stub, Bub

    //     Yog-Urt (Hatred) path
    // Choice 710 is They've Got Fun and Games
    // Choice 711 is They've Got Everything You Want
    // Choice 712 is Honey, They Know the Names
    // Choice 713 is You Brought Her To Her Kn-kn-kn-kn-knees, Knees.

    //     Dad Sea Monkee (Loathing) path
    // Choice 714 is An Unguarded Door (1)
    // Choice 715 is Life in the Stillness
    // Choice 716 is An Unguarded Door (2)
    // Choice 717 is Over. Over Now.

    // The Cabin in the Dreadsylvanian Woods
    new ChoiceAdventure(
        721,
        "Dreadsylvania",
        "Cabin",
        1,
        // Option...
        new ChoiceOption("learn shortcut", 5),
        new ChoiceOption("skip adventure", 6));

    // Choice 722 is The Kitchen in the Woods
    // Choice 723 is What Lies Beneath (the Cabin)
    // Choice 724 is Where it's Attic

    // Tallest Tree in the Forest
    new ChoiceAdventure(
        725,
        "Dreadsylvania",
        "Tallest Tree",
        2,
        // Option...
        new ChoiceOption("learn shortcut", 5),
        new ChoiceOption("skip adventure", 6));

    // Choice 726 is Top of the Tree, Ma!
    // Choice 727 is All Along the Watchtower
    // Choice 728 is Treebasing

    // Below the Roots
    new ChoiceAdventure(
        729,
        "Dreadsylvania",
        "Burrows",
        3,
        // Option...
        new ChoiceOption("learn shortcut", 5),
        new ChoiceOption("skip adventure", 6));

    // Choice 730 is Hot Coals
    // Choice 731 is The Heart of the Matter
    // Choice 732 is Once Midden, Twice Shy

    // Dreadsylvanian Village Square
    new ChoiceAdventure(
        733,
        "Dreadsylvania",
        "Village Square",
        4,
        // Option...
        new ChoiceOption("learn shortcut", 5),
        new ChoiceOption("skip adventure", 6));

    // Choice 734 is Fright School
    // Choice 735 is Smith, Black as Night
    // Choice 736 is Gallows

    // The Even More Dreadful Part of Town
    new ChoiceAdventure(
        737,
        "Dreadsylvania",
        "Skid Row",
        5,
        // Option...
        new ChoiceOption("learn shortcut", 5),
        new ChoiceOption("skip adventure", 6));

    // Choice 738 is A Dreadful Smell
    // Choice 739 is The Tinker's. Damn.
    // Choice 740 is Eight, Nine, Tenement

    // The Old Duke's Estate
    new ChoiceAdventure(
        741,
        "Dreadsylvania",
        "Old Duke's Estate",
        6,
        // Option...
        new ChoiceOption("learn shortcut", 5),
        new ChoiceOption("skip adventure", 6));

    // Choice 742 is The Plot Thickens
    // Choice 743 is No Quarter
    // Choice 744 is The Master Suite -- Sweet!

    // This Hall is Really Great
    new ChoiceAdventure(
        745,
        "Dreadsylvania",
        "Great Hall",
        8,
        // Option...
        new ChoiceOption("learn shortcut", 5),
        new ChoiceOption("skip adventure", 6));

    // Choice 746 is The Belle of the Ballroom
    // Choice 747 is Cold Storage
    // Choice 748 is Dining In (the Castle)

    // Tower Most Tall
    new ChoiceAdventure(
        749,
        "Dreadsylvania",
        "Tower",
        7,
        // Option...
        new ChoiceOption("learn shortcut", 5),
        new ChoiceOption("skip adventure", 6));

    // Choice 750 is Working in the Lab, Late One Night
    // Choice 751 is Among the Quaint and Curious Tomes.
    // Choice 752 is In The Boudoir

    // The Dreadsylvanian Dungeon
    new ChoiceAdventure(
        753,
        "Dreadsylvania",
        "Dungeons",
        9,
        // Option...
        new ChoiceOption("learn shortcut", 5),
        new ChoiceOption("skip adventure", 6));

    // Choice 754 is Live from Dungeon Prison
    // Choice 755 is The Hot Bowels
    // Choice 756 is Among the Fungus

    // Choice 757 is ???

    // Choice 758 is End of the Path
    // Choice 759 is You're About to Fight City Hall
    // Choice 760 is Holding Court
    // Choice 761 is Staring Upwards...
    // Choice 762 is Try New Extra-Strength Anvil
    // Choice 763 is ???
    // Choice 764 is The Machine
    // Choice 765 is Hello Gallows
    // Choice 766 is ???
    // Choice 767 is Tales of Dread

    // Choice 768 is The Littlest Identity Crisis
    // Choice 771 is It Was All a Horrible, Horrible Dream

    // Choice 772 is Saved by the Bell
    // Choice 774 is Opening up the Folder Holder

    // Choice 778 is If You Could Only See
    new ChoiceAdventure(
        778,
        "Item-Driven",
        "Tonic Djinn",
        // Option...
        new ChoiceOption("gain 400-500 meat", 1),
        new ChoiceOption("gain 50-60 muscle stats", 2),
        new ChoiceOption("gain 50-60 mysticality stats", 3),
        new ChoiceOption("gain 50-60 moxie stats", 4),
        new ChoiceOption("don't use it", 6));

    // Choice 780 is Action Elevator
    // Choice 781 is Earthbound and Down
    // Choice 783 is Water You Dune
    // Choice 784 is You, M. D.
    // Choice 785 is Air Apparent
    // Choice 786 is Working Holiday
    // Choice 787 is Fire when Ready
    // Choice 788 is Life is Like a Cherry of Bowls
    // Choice 789 is Where Does The Lone Ranger Take His Garbagester?
    // Choice 791 is Legend of the Temple in the Hidden City

    // Choice 793 is Welcome to The Shore, Inc.
    new ChoiceAdventure(
        793,
        "Beach",
        "The Shore",
        // Option...
        new ChoiceOption("Muscle Vacation"),
        new ChoiceOption("Mysticality Vacation"),
        new ChoiceOption("Moxie Vacation"));

    // Choice 794 is Once More Unto the Junk
    new ChoiceAdventure(
        794,
        "Woods",
        "The Old Landfill",
        // Option...
        new ChoiceOption("The Bathroom of Ten Men"),
        new ChoiceOption("The Den of Iquity"),
        new ChoiceOption("Let's Workshop This a Little"));

    // Choice 795 is The Bathroom of Ten Men
    new ChoiceAdventure(
        795,
        "Woods",
        "The Bathroom of Ten Men",
        // Option...
        new ChoiceOption("old claw-foot bathtub", "old claw-foot bathtub"),
        new ChoiceOption("fight junksprite"),
        new ChoiceOption("make lots of noise"));

    // Choice 796 is The Den of Iquity
    new ChoiceAdventure(
        796,
        "Woods",
        "The Den of Iquity",
        // Option...
        new ChoiceOption("make lots of noise"),
        new ChoiceOption("old clothesline pole", "old clothesline pole"),
        new ChoiceOption("tangle of copper wire", "tangle of copper wire"));

    // Choice 797 is Let's Workshop This a Little
    new ChoiceAdventure(
        797,
        "Woods",
        "Let's Workshop This a Little",
        // Option...
        new ChoiceOption("Junk-Bond", "Junk-Bond"),
        new ChoiceOption("make lots of noise"),
        new ChoiceOption("antique cigar sign", "antique cigar sign"));

    // Choice 801 is A Reanimated Conversation

    // Choice 803 is Behind the Music.  Literally.
    new ChoiceAdventure(
        803,
        "Events",
        "The Space Odyssey Discotheque",
        // Option...
        new ChoiceOption("gain 2-3 horoscopes", 1),
        new ChoiceOption("find interesting room", 3),
        new ChoiceOption("investigate interesting room", 4),
        new ChoiceOption("investigate trap door", 5),
        new ChoiceOption("investigate elevator", 6));

    // Choice 804 is Trick or Treat!

    // Choice 805 is A Sietch in Time
    new ChoiceAdventure(
        805,
        "Beach",
        "Arid, Extra-Dry Desert",
        // Option...
        new ChoiceOption("talk to Gnasir"));

    // Choice 808 is Silence at Last.
    new ChoiceAdventure(
        808,
        "Events",
        "The Spirit World",
        // Option...
        new ChoiceOption("gain spirit bed piece"),
        new ChoiceOption("fight spirit alarm clock"));

    // Choice 809 is Uncle Crimbo's Trailer
    // Choice 810 is K.R.A.M.P.U.S. facility

    // Choice 813 is What Warbears Are Good For
    new ChoiceAdventure(
        813,
        "Crimbo13",
        "Warbear Fortress (First Level)",
        // Option...
        new ChoiceOption("Open K.R.A.M.P.U.S. facility"));

    // Choice 822 is The Prince's Ball (In the Restroom)
    // Choice 823 is The Prince's Ball (On the Dance Floor)
    // Choice 824 is The Prince's Ball (In the Kitchen)
    // Choice 825 is The Prince's Ball (On the Balcony)
    // Choice 826 is The Prince's Ball (In the Lounge)
    // Choice 827 is The Prince's Ball (At the Canap&eacute;s Table)

    // Choice 829 is We All Wear Masks

    // Choice 830 is Cooldown
    new ChoiceAdventure(
        830,
        "Skid Row",
        "Cooldown",
        // Option...
        new ChoiceOption("+Wolf Offence or +Wolf Defence"),
        new ChoiceOption("+Wolf Elemental Attacks or +Rabbit"),
        new ChoiceOption("Improved Howling! or +Wolf Lung Capacity"),
        new ChoiceOption("Leave", 6));

    // Choice 832 is Shower Power
    new ChoiceAdventure(
        832,
        "Skid Row",
        "Shower Power",
        // Option...
        new ChoiceOption("+Wolf Offence"),
        new ChoiceOption("+Wolf Defence"));

    // Choice 833 is Vendie, Vidi, Vici
    new ChoiceAdventure(
        833,
        "Skid Row",
        "Vendie, Vidi, Vici",
        // Option...
        new ChoiceOption("+Wolf Elemental Attacks"),
        new ChoiceOption("+Rabbit"));

    // Choice 834 is Back Room Dealings
    new ChoiceAdventure(
        834,
        "Skid Row",
        "Back Room Dealings",
        // Option...
        new ChoiceOption("Improved Howling!", 2),
        new ChoiceOption("+Wolf Lung Capacity", 3));

    // Choice 835 is Barely Tales
    new ChoiceAdventure(
        835,
        "Item-Driven",
        "Grim Brother",
        // Option...
        new ChoiceOption("30 turns of +20 initiative"),
        new ChoiceOption("30 turns of +20 max HP, +10 max MP"),
        new ChoiceOption("30 turns of +10 Weapon Damage, +20 Spell Damage"));

    // Choice 836 is Adventures Who Live in Ice Houses...

    // Choice 837 is On Purple Pond
    new ChoiceAdventure(
        837,
        "The Candy Witch and the Relentless Child Thieves",
        "On Purple Pond",
        // Option...
        new ChoiceOption("find out the two children not invading"),
        new ChoiceOption("+1 Moat"),
        new ChoiceOption("gain Candy"));

    // Choice 838 is General Mill
    new ChoiceAdventure(
        838,
        "The Candy Witch and the Relentless Child Thieves",
        "General Mill",
        // Option...
        new ChoiceOption("+1 Moat"),
        new ChoiceOption("gain Candy"));

    // Choice 839 is On The Sounds of the Undergrounds
    new ChoiceAdventure(
        839,
        "The Candy Witch and the Relentless Child Thieves",
        "The Sounds of the Undergrounds",
        // Option...
        new ChoiceOption("learn what the first two waves will be"),
        new ChoiceOption("+1 Minefield Strength"),
        new ChoiceOption("gain Candy"));

    // Choice 840 is Hop on Rock Pops
    new ChoiceAdventure(
        840,
        "The Candy Witch and the Relentless Child Thieves",
        "Hop on Rock Pops",
        // Option...
        new ChoiceOption("+1 Minefield Strength"),
        new ChoiceOption("gain Candy"));

    // Choice 841 is Building, Structure, Edifice
    new ChoiceAdventure(
        841,
        "The Candy Witch and the Relentless Child Thieves",
        "Building, Structure, Edifice",
        // Option...
        new ChoiceOption("increase candy in another location"),
        new ChoiceOption("+2 Random Defense"),
        new ChoiceOption("gain Candy"));

    // Choice 842 is The Gingerbread Warehouse
    new ChoiceAdventure(
        842,
        "The Candy Witch and the Relentless Child Thieves",
        "The Gingerbread Warehouse",
        // Option...
        new ChoiceOption("+1 Wall Strength"),
        new ChoiceOption("+1 Poison Jar"),
        new ChoiceOption("+1 Anti-Aircraft Turret"),
        new ChoiceOption("gain Candy"));

    // Choice 844 is The Portal to Horrible Parents
    // Choice 845 is Rumpelstiltskin's Workshop
    // Choice 846 is Bartering for the Future of Innocent Children
    // Choice 847 is Pick Your Poison
    // Choice 848 is Where the Magic Happens
    // Choice 850 is World of Bartercraft

    // Choice 851 is Shen Copperhead, Nightclub Owner
    // Choice 852 is Shen Copperhead, Jerk
    // Choice 853 is Shen Copperhead, Huge Jerk
    // Choice 854 is Shen Copperhead, World's Biggest Jerk

    // Choice 855 is Behind the 'Stache
    new ChoiceAdventure(
        855,
        "Town",
        "Behind the 'Stache",
        // Option...
        new ChoiceOption("don't take initial damage in fights"),
        new ChoiceOption("can get priceless diamond"),
        new ChoiceOption("can make Flamin' Whatshisname"),
        new ChoiceOption("get 4-5 random items"));

    // Choice 856 is This Looks Like a Good Bush for an Ambush
    new ChoiceAdventure(
        856,
        "The Red Zeppelin's Mooring",
        "This Looks Like a Good Bush for an Ambush",
        // Option...
        new ChoiceOption("scare protestors (more with lynyrd gear)"),
        SKIP_ADVENTURE);

    // Choice 857 is Bench Warrant
    new ChoiceAdventure(
        857,
        "The Red Zeppelin's Mooring",
        "Bench Warrant",
        // Option...
        new ChoiceOption("creep protestors (more with sleaze damage/sleaze spell damage)"),
        SKIP_ADVENTURE);

    // Choice 858 is Fire Up Above
    new ChoiceAdventure(
        858,
        "The Red Zeppelin's Mooring",
        "Fire Up Above",
        // Option...
        new ChoiceOption("set fire to protestors (more with Flamin' Whatshisname)"),
        SKIP_ADVENTURE);

    // Choice 866 is Methinks the Protesters Doth Protest Too Little
    new ChoiceAdventure(
        866,
        "The Red Zeppelin's Mooring",
        "Methinks the Protesters Doth Protest Too Little",
        // Option...
        new ChoiceOption("scare protestors (more with lynyrd gear)"),
        new ChoiceOption("creep protestors (more with sleaze damage/sleaze spell damage)"),
        new ChoiceOption("set fire to protestors (more with Flamin' Whatshisname)"));

    // Rod Nevada, Vendor
    new ChoiceSpoiler(
        873,
        "Plains",
        "The Palindome",
        // Option...
        new ChoiceOption("photograph of a red nugget", "photograph of a red nugget"),
        SKIP_ADVENTURE);
    // Rod Nevada, Vendor
    new ChoiceCost(873, new Cost(1, new AdventureResult(AdventureResult.MEAT, -500)));

    // Welcome To Our ool Table
    new ChoiceAdventure(
        875,
        "Manor1",
        "Pool Table",
        // Option...
        new ChoiceOption("try to beat ghost"),
        new ChoiceOption("improve pool skill"),
        new ChoiceOption("skip"));

    // One Simple Nightstand
    new ChoiceAdventure(
        876,
        "Manor2",
        "One Simple Nightstand",
        // Option...
        new ChoiceOption("old leather wallet", 1),
        new ChoiceOption("muscle substats", 2),
        new ChoiceOption("muscle substats (with ghost key)", 3),
        new ChoiceOption("skip", 6));

    // One Mahogany Nightstand
    new ChoiceAdventure(
        877,
        "Manor2",
        "One Mahogany Nightstand",
        // Option...
        new ChoiceOption("old coin purse or half a memo", 1),
        new ChoiceOption("take damage", 2),
        new ChoiceOption("quest item", 3),
        new ChoiceOption("gain more meat (with ghost key)", 4),
        new ChoiceOption("skip", 6));

    // One Ornate Nightstand
    new ChoiceAdventure(
        878,
        "Manor2",
        "One Ornate Nightstand",
        // Option...
        new ChoiceOption("small meat boost", 1),
        new ChoiceOption("mysticality substats", 2),
        new ChoiceOption("Lord Spookyraven's spectacles", 3, "Lord Spookyraven's spectacles"),
        new ChoiceOption("disposable instant camera", 4, "disposable instant camera"),
        new ChoiceOption("mysticality substats (with ghost key)", 5),
        new ChoiceOption("skip", 6));

    // One Rustic Nightstand
    new ChoiceAdventure(
        879,
        "Manor2",
        "One Rustic Nightstand",
        // Option...
        new ChoiceOption("moxie", 1),
        new ChoiceOption("grouchy restless spirit or empty drawer", 2, "grouchy restless spirit"),
        new ChoiceOption("enter combat with mistress (1)", 3),
        new ChoiceOption("Engorged Sausages and You or moxie", 4),
        new ChoiceOption("moxie substats (with ghost key)", 5),
        new ChoiceOption("skip", 6));

    // One Elegant Nightstand
    new ChoiceAdventure(
        880,
        "Manor2",
        "One Elegant Nightstand",
        // Option...
        new ChoiceOption(
            "Lady Spookyraven's finest gown (once only)", 1, "Lady Spookyraven's finest gown"),
        new ChoiceOption("elegant nightstick", 2, "elegant nightstick"),
        new ChoiceOption("stats (with ghost key)", 3),
        new ChoiceOption("skip", 6));

    // Off the Rack
    new ChoiceAdventure(
        882,
        "Manor2",
        "Bathroom Towel",
        // Option...
        new ChoiceOption("get towel"),
        new ChoiceOption("skip"));

    // Take a Look, it's in a Book!
    new ChoiceSpoiler(
        888,
        "Haunted Library",
        // Option...
        new ChoiceOption("background history"),
        new ChoiceOption("cooking recipe"),
        new ChoiceOption("other options"),
        SKIP_ADVENTURE);

    // Take a Look, it's in a Book!
    new ChoiceSpoiler(
        889,
        "Haunted Library",
        // Option...
        new ChoiceOption("background history", 1),
        new ChoiceOption("cocktailcrafting recipe", 2),
        new ChoiceOption("muscle substats", 3),
        new ChoiceOption("dictionary", 4, "dictionary"),
        new ChoiceOption("skip", 5));

    // Choice 890 is Lights Out in the Storage Room
    // Choice 891 is Lights Out in the Laundry Room
    // Choice 892 is Lights Out in the Bathroom
    // Choice 893 is Lights Out in the Kitchen
    // Choice 894 is Lights Out in the Library
    // Choice 895 is Lights Out in the Ballroom
    // Choice 896 is Lights Out in the Gallery
    // Choice 897 is Lights Out in the Bedroom
    // Choice 898 is Lights Out in the Nursery
    // Choice 899 is Lights Out in the Conservatory
    // Choice 900 is Lights Out in the Billiards Room
    // Choice 901 is Lights Out in the Wine Cellar
    // Choice 902 is Lights Out in the Boiler Room
    // Choice 903 is Lights Out in the Laboratory

    // Choices 904-913 are Escher print adventures

    // Louvre It or Leave It
    new ChoiceSpoiler(
        914,
        "Haunted Gallery",
        // Option...
        new ChoiceOption("Enter the Drawing"),
        SKIP_ADVENTURE);

    // Choice 918 is Yachtzee!
    new ChoiceAdventure(
        918,
        "Spring Break Beach",
        "Yachtzee!",
        // Option...
        new ChoiceOption("get cocktail ingredients (sometimes Ultimate Mind Destroyer)"),
        new ChoiceOption("get 5k meat and random item"),
        new ChoiceOption("get Beach Bucks"));

    // Choice 919 is Break Time!
    new ChoiceAdventure(
        919,
        "Spring Break Beach",
        "Break Time!",
        // Option...
        new ChoiceOption("get Beach Bucks"),
        new ChoiceOption("+15ML on Sundaes"),
        new ChoiceOption("+15ML on Burgers"),
        new ChoiceOption("+15ML on Cocktails"),
        new ChoiceOption("reset ML on monsters"),
        new ChoiceOption("leave without using a turn"));

    // Choice 920 is Eraser
    new ChoiceAdventure(
        920,
        "Item-Driven",
        "Eraser",
        // Option...
        new ChoiceOption("reset Buff Jimmy quests"),
        new ChoiceOption("reset Taco Dan quests"),
        new ChoiceOption("reset Broden quests"),
        new ChoiceOption("don't use it"));

    // Choice 921 is We'll All Be Flat

    // Choice 923 is All Over the Map
    new ChoiceAdventure(
        923,
        "Woods",
        "Black Forest",
        // Option...
        new ChoiceOption("fight blackberry bush, visit cobbler, or raid beehive"),
        new ChoiceOption("visit blacksmith"),
        new ChoiceOption("visit black gold mine"),
        new ChoiceOption("visit black church"));

    // Choice 924 is You Found Your Thrill
    new ChoiceAdventure(
        924,
        "Woods",
        "Blackberry",
        // Option...
        new ChoiceOption("fight blackberry bush"),
        new ChoiceOption("visit cobbler"),
        new ChoiceOption("head towards beehive (1)"));

    // Choice 925 is The Blackest Smith
    new ChoiceAdventure(
        925,
        "Woods",
        "Blacksmith",
        // Option...
        new ChoiceOption("get black sword", 1, "black sword"),
        new ChoiceOption("get black shield", 2, "black shield"),
        new ChoiceOption("get black helmet", 3, "black helmet"),
        new ChoiceOption("get black greaves", 4, "black greaves"),
        new ChoiceOption("return to main choice", 6));

    // Choice 926 is Be Mine
    new ChoiceAdventure(
        926,
        "Woods",
        "Black Gold Mine",
        // Option...
        new ChoiceOption("get black gold", 1, "black gold"),
        new ChoiceOption("get Texas tea", 2, "Texas tea"),
        new ChoiceOption("get Black Lung effect", 3),
        new ChoiceOption("return to main choice", 6));

    // Choice 927 is Sunday Black Sunday
    new ChoiceAdventure(
        927,
        "Woods",
        "Black Church",
        // Option...
        new ChoiceOption("get 13 turns of Salsa Satanica or beaten up", 1),
        new ChoiceOption("get black kettle drum", 2, "black kettle drum"),
        new ChoiceOption("return to main choice", 6));

    // Choice 928 is The Blackberry Cobbler
    new ChoiceAdventure(
        928,
        "Woods",
        "Blackberry Cobbler",
        // Option...
        new ChoiceOption("get blackberry slippers", 1, "blackberry slippers"),
        new ChoiceOption("get blackberry moccasins", 2, "blackberry moccasins"),
        new ChoiceOption("get blackberry combat boots", 3, "blackberry combat boots"),
        new ChoiceOption("get blackberry galoshes", 4, "blackberry galoshes"),
        new ChoiceOption("return to main choice", 6));

    // Choice 929 is Control Freak
    new ChoiceAdventure(
        929,
        "Pyramid",
        "Control Room",
        // Option...
        new ChoiceOption("turn lower chamber, lose wheel", 1),
        new ChoiceOption("turn lower chamber, lose ratchet", 2),
        new ChoiceOption("enter lower chamber", 5),
        new ChoiceOption("leave", 6));

    // Choice 930 is Another Errand I Mean Quest
    // Choice 931 is Life Ain't Nothin But Witches and Mummies
    // Choice 932 is No Whammies
    // Choice 935 is Lost in Space... Ship
    // Choice 936 is The Nerve Center
    // Choice 937 is The Spacement
    // Choice 938 is The Ship's Kitchen

    // Choice 940 is Let Your Fists Do The Walking
    new ChoiceAdventure(
        940,
        "Item-Driven",
        "white page",
        // Option...
        new ChoiceOption("fight whitesnake"),
        new ChoiceOption("fight white lion"),
        new ChoiceOption("fight white chocolate golem"),
        new ChoiceOption("fight white knight"),
        new ChoiceOption("fight white elephant"),
        new ChoiceOption("skip"));

    // Choice 950 is Time-Twitching Tower Voting / Phone Booth

    // Choice 955 is Time Cave.  Period.
    new ChoiceAdventure(
        955,
        "Twitch",
        "Time Cave",
        // Option...
        new ChoiceOption("fight Adventurer echo"),
        new ChoiceOption("twitching time capsule", "twitching time capsule"),
        new ChoiceOption("talk to caveman"));

    // Choice 973 is Shoe Repair Store
    new ChoiceAdventure(
        973,
        "Twitch",
        "Shoe Repair Store",
        // Option...
        new ChoiceOption("visit shop", 1),
        new ChoiceOption("exchange hooch for Chroners", 2),
        new ChoiceOption("leave", 6));

    // Choice 974 is Around The World
    new ChoiceAdventure(
        974,
        "Twitch",
        "Bohemian Party",
        // Option...
        new ChoiceOption("get up to 5 hooch"),
        new ChoiceOption("leave"));

    // Choice 975 is Crazy Still After All These Years
    new ChoiceAdventure(
        975,
        "Twitch",
        "Moonshriner's Woods",
        // Option...
        new ChoiceOption("swap 5 cocktail onions for 10 hooch"),
        new ChoiceOption("leave"));

    // Choice 979 is The Agora
    new ChoiceAdventure(
        979,
        "Twitch",
        "The Agora",
        // Option...
        new ChoiceOption("get blessing", 1),
        new ChoiceOption("visit store", 2),
        new ChoiceOption("play dice", 6));

    // Choice 980 is Welcome to Blessings Hut
    new ChoiceAdventure(
        980,
        "Twitch",
        "Blessings Hut",
        // Option...
        new ChoiceOption("Bruno's blessing of Mars", "Bruno's blessing of Mars"),
        new ChoiceOption("Dennis's blessing of Minerva", "Dennis's blessing of Minerva"),
        new ChoiceOption("Burt's blessing of Bacchus", "Burt's blessing of Bacchus"),
        new ChoiceOption("Freddie's blessing of Mercury", "Freddie's blessing of Mercury"),
        new ChoiceOption("return to Agora", 6));

    // Choice 982 is The 99-Centurion Store
    new ChoiceAdventure(
        982,
        "Twitch",
        "The 99-Centurion Store",
        // Option...
        new ChoiceOption("centurion helmet", "centurion helmet"),
        new ChoiceOption("pteruges", "pteruges"),
        new ChoiceOption("return to Agora", 6));

    // Choice 983 is Playing Dice With Romans
    new ChoiceAdventure(
        983,
        "Twitch",
        "Playing Dice With Romans",
        // Option...
        new ChoiceOption("make a bet and throw dice", 1),
        new ChoiceOption("return to Agora", 6));

    // Choice 984 is A Radio on a Beach
    // Choice 988 is The Containment Unit
    // Choice 989 is Paranormal Test Lab

    // Choice 993 is Tales of Spelunking

    // Choice 996 is (Untitled) Crimbomega

    // Choice 998 is Game of Cards
    new ChoiceAdventure(
        998,
        "Twitch",
        "Game of Cards",
        // Option...
        new ChoiceOption("Gain 7 Chroner"),
        new ChoiceOption("Gain 9 Chroner"),
        new ChoiceOption("Gain 13 Chroner (80% chance)"),
        new ChoiceOption("Gain 17 Chroner (60% chance)"),
        new ChoiceOption("Gain 21 Chroner, lose pocket ace"));

    // Choice 1000 is Everything in Moderation
    // Choice 1001 is Hot and Cold Dripping Rats

    // Choice 1003 is Test Your Might And Also Test Other Things

    // Choice 1004 is This Maze is... Mazelike...

    // 'Allo
    new ChoiceAdventure(
        1005,
        "Sorceress",
        "Hedge Maze 1",
        // Option...
        new ChoiceOption("topiary nugglet and advance to Room 2", "topiary nugglet"),
        new ChoiceOption("Test #1 and advance to Room 4"));

    // One Small Step For Adventurer
    new ChoiceAdventure(
        1006,
        "Sorceress",
        "Hedge Maze 2",
        // Option...
        new ChoiceOption("topiary nugglet and advance to Room 3", "topiary nugglet"),
        new ChoiceOption("Fight topiary gopher and advance to Room 4"));

    // Twisty Little Passages, All Hedge
    new ChoiceAdventure(
        1007,
        "Sorceress",
        "Hedge Maze 3",
        // Option...
        new ChoiceOption("topiary nugglet and advance to Room 4", "topiary nugglet"),
        new ChoiceOption("Fight topiary chihuahua herd and advance to Room 5"));

    // Pooling Your Resources
    new ChoiceAdventure(
        1008,
        "Sorceress",
        "Hedge Maze 4",
        // Option...
        new ChoiceOption("topiary nugglet and advance to Room 5", "topiary nugglet"),
        new ChoiceOption("Test #2 and advance to Room 7"));

    // Good Ol' 44% Duck
    new ChoiceAdventure(
        1009,
        "Sorceress",
        "Hedge Maze 5",
        // Option...
        new ChoiceOption("topiary nugglet and advance to Room 6", "topiary nugglet"),
        new ChoiceOption("Fight topiary duck and advance to Room 7"));

    // Another Day, Another Fork
    new ChoiceAdventure(
        1010,
        "Sorceress",
        "Hedge Maze 6",
        // Option...
        new ChoiceOption("topiary nugglet and advance to Room 7", "topiary nugglet"),
        new ChoiceOption("Fight topiary kiwi and advance to Room 8"));

    // Of Mouseholes and Manholes
    new ChoiceAdventure(
        1011,
        "Sorceress",
        "Hedge Maze 7",
        // Option...
        new ChoiceOption("topiary nugglet and advance to Room 8", "topiary nugglet"),
        new ChoiceOption("Test #3 and advance to Room 9"));

    // The Last Temptation
    new ChoiceAdventure(
        1012,
        "Sorceress",
        "Hedge Maze 8",
        // Option...
        new ChoiceOption("topiary nugglet and advance to Room 9", "topiary nugglet"),
        new ChoiceOption("Lose HP for no benefit and advance to Room 9"));

    // Choice 1013 is Mazel Tov!

    // The Mirror in the Tower has the View that is True
    new ChoiceAdventure(
        1015,
        "Sorceress",
        "Tower Mirror",
        // Option...
        new ChoiceOption("Gain Confidence! intrinsic until leave tower (1)"),
        new ChoiceOption("Make Sorceress tougher (0 turns)"));

    // Choice 1016 is Frank Gets Earnest
    // Choice 1017 is Bear Verb Orgy

    // Bee Persistent
    new ChoiceAdventure(
        1018,
        "Woods",
        "Bees 1",
        // Option...
        new ChoiceOption("head towards beehive (1)"),
        new ChoiceOption("give up"));

    // Bee Rewarded
    new ChoiceAdventure(
        1019,
        "Woods",
        "Bees 2",
        // Option...
        new ChoiceOption("beehive (1)", "beehive"),
        new ChoiceOption("give up"));

    // Choice 1020 is Closing Ceremony
    // Choice 1021 is Meet Frank
    // Choice 1022 is Meet Frank
    // Choice 1023 is Like a Bat Into Hell
    // Choice 1024 is Like a Bat out of Hell

    // Home on the Free Range
    new ChoiceAdventure(
        1026,
        "Beanstalk",
        "Ground Floor Foodie",
        // Option...
        new ChoiceOption("4 pieces of candy"),
        new ChoiceOption("electric boning knife, then skip adventure", "electric boning knife"),
        SKIP_ADVENTURE);

    // Choice 1027 is The End of the Tale of Spelunking

    // Choice 1028 is A Shop
    new ChoiceAdventure(
        1028,
        "Spelunky Area",
        "A Shop",
        // Option...
        new ChoiceOption("chance to fight shopkeeper", 5),
        new ChoiceOption("leave", 6));

    // Choice 1029 is An Old Clay Pot
    new ChoiceAdventure(
        1029,
        "Spelunky Area",
        "An Old Clay Pot",
        // Option...
        new ChoiceOption("gain 18-20 gold", 1),
        new ChoiceOption("gain pot", 5, "pot"));

    // Choice 1030 is It's a Trap!  A Dart Trap.
    new ChoiceAdventure(
        1030,
        "Spelunky Area",
        "It's a Trap!  A Dart Trap.",
        // Option...
        new ChoiceOption("escape with whip", 1),
        new ChoiceOption("unlock The Snake Pit using bomb", 2),
        new ChoiceOption("unlock The Spider Hole using rope", 3),
        new ChoiceOption("escape using offhand item", 4),
        new ChoiceOption("take damage", 6));

    // Choice 1031 is A Tombstone
    new ChoiceAdventure(
        1031,
        "Spelunky Area",
        "A Tombstone",
        // Option...
        new ChoiceOption("gain 20-25 gold or buddy", 1),
        new ChoiceOption("gain shotgun with pickaxe", 2, "shotgun"),
        new ChoiceOption("gain Clown Crown with x-ray specs", 3, "The Clown Crown"));

    // Choice 1032 is It's a Trap!  A Tiki Trap.
    new ChoiceAdventure(
        1032,
        "Spelunky Area",
        "It's a Trap!  A Tiki Trap.",
        // Option...
        new ChoiceOption("escape with spring boots", 1),
        new ChoiceOption("unlock The Beehive using bomb, take damage without sticky bomb", 2),
        new ChoiceOption(
            "unlock The Ancient Burial Ground using rope, take damage without back item", 3),
        new ChoiceOption("lose 30 hp", 6));

    // Choice 1033 is A Big Block of Ice
    new ChoiceAdventure(
        1033,
        "Spelunky Area",
        "A Big Block of Ice",
        // Option...
        new ChoiceOption("gain 50-60 gold and restore health (with cursed coffee cup)", 1),
        new ChoiceOption("gain buddy (or 60-70 gold) with torch", 2));

    // Choice 1034 is A Landmine
    new ChoiceAdventure(
        1034,
        "Spelunky Area",
        "A Landmine",
        // Option...
        new ChoiceOption("unlock An Ancient Altar and lose 10 HP", 2),
        new ChoiceOption("unlock The Crashed UFO using 3 ropes", 3),
        new ChoiceOption("lose 30 hp", 6));

    // Choice 1035 is A Crate

    // Choice 1036 is Idolatry
    new ChoiceAdventure(
        1036,
        "Spelunky Area",
        "Idolatry",
        // Option...
        new ChoiceOption("gain 250 gold with Resourceful Kid", 1),
        new ChoiceOption("gain 250 gold with spring boots and yellow cloak", 2),
        new ChoiceOption("gain 250 gold with jetpack", 3),
        new ChoiceOption("gain 250 gold and lose 50 hp", 4),
        new ChoiceOption("leave", 6));

    // Choice 1037 is It's a Trap!  A Smashy Trap.
    new ChoiceAdventure(
        1037,
        "Spelunky Area",
        "It's a Trap!  A Smashy Trap.",
        // Option...
        new ChoiceOption("unlock The City of Goooold with key, or take damage", 2),
        new ChoiceOption("lose 40 hp", 6));

    // Choice 1038 is A Wicked Web
    new ChoiceAdventure(
        1038,
        "Spelunky Area",
        "A Wicked Web",
        // Option...
        new ChoiceOption("gain 15-20 gold", 1),
        new ChoiceOption("gain buddy (or 20-30 gold) with machete", 2),
        new ChoiceOption("gain 30-50 gold with torch", 3));

    // Choice 1039 is A Golden Chest
    new ChoiceAdventure(
        1039,
        "Spelunky Area",
        "A Golden Chest",
        // Option...
        new ChoiceOption("gain 150 gold with key", 1),
        new ChoiceOption("gain 80-100 gold with bomb", 2),
        new ChoiceOption("gain 50-60 gold and lose 20 hp", 3));

    // Choice 1040 is It's Lump. It's Lump.
    new ChoiceAdventure(
        1040,
        "Spelunky Area",
        "It's Lump. It's Lump",
        // Option...
        new ChoiceOption("gain heavy pickaxe with bomb", 1, "heavy pickaxe"),
        new ChoiceOption("leave", 6));

    // choice 1041 is Spelunkrifice
    new ChoiceAdventure(
        1041,
        "Spelunky Area",
        "Spelunkrifice",
        // Option...
        new ChoiceOption("sacrifice buddy", 1),
        new ChoiceOption("leave", 6));

    // choice 1042 is Pick a Perk!
    // choice 1044 is The Gates of Hell

    new ChoiceAdventure(
        1045,
        "Spelunky Area",
        "Hostile Work Environment",
        // Option...
        new ChoiceOption("fight shopkeeper", 1),
        new ChoiceOption("take damage", 6));

    // Choice 1046 is Actually Ed the Undying
    // Choice 1048 is Twitch Event #8 Time Period
    // Choice 1049 is Tomb of the Unknown Your Class Here
    // Choice 1051 is The Book of the Undying
    // Choice 1052 is Underworld Body Shop
    // Choice 1053 is The Servants' Quarters
    // Choice 1054 is Returning the MacGuffin
    // Choice 1055 is Returning the MacGuffin
    // Choice 1056 is Now It's Dark
    // Choice 1057 is A Stone Shrine
    // Choice 1059 is Helping Make Ends Meat

    // Choice 1060 is Temporarily Out of Skeletons
    new ChoiceAdventure(
        1060,
        "Town",
        "Skeleton Store",
        // Option...
        new ChoiceOption("gain office key, then ~35 meat", 1, "Skeleton Store office key"),
        new ChoiceOption(
            "gain ring of telling skeletons what to do, then 300 meat, with skeleton key",
            2,
            "ring of telling skeletons what to do"),
        new ChoiceOption("gain muscle stats", 3),
        new ChoiceOption("fight former owner of the Skeleton Store, with office key", 4));

    // Choice 1061 is Heart of Madness
    new ChoiceAdventure(
        1061,
        "Town",
        "Madness Bakery",
        // Option...
        new ChoiceOption("try to enter office", 1),
        new ChoiceOption("bagel machine", 2),
        new ChoiceOption("popular machine", 3),
        new ChoiceOption("learn recipe", 4),
        new ChoiceOption("gain mysticality stats", 5));

    // Choice 1062 is Lots of Options
    new ChoiceAdventure(
        1062,
        "Town",
        "Overgrown Lot",
        // Option...
        new ChoiceOption("acquire flowers", 1),
        new ChoiceOption("acquire food", 2),
        new ChoiceOption("acquire drinks", 3),
        new ChoiceOption("gain moxie stats", 4),
        new ChoiceOption("acquire more booze with map", 5));

    // Choice 1063 is Adjust your 'Edpiece
    new ChoiceSpoiler(
        1063,
        "Crown of Ed the Undying",
        // Option...
        new ChoiceOption("Muscle +20, +2 Muscle Stats Per Fight"),
        new ChoiceOption("Mysticality +20, +2 Mysticality Stats Per Fight"),
        new ChoiceOption("Moxie +20, +2 Moxie Stats Per Fight"),
        new ChoiceOption("+20 to Monster Level"),
        new ChoiceOption("+10% Item Drops from Monsters, +20% Meat from Monsters"),
        new ChoiceOption(
            "The first attack against you will always miss, Regenerate 10-20 HP per Adventure"),
        new ChoiceOption("Lets you breathe underwater"));

    // Choice 1065 is Lending a Hand (and a Foot)
    // Choice 1067 is Maint Misbehavin'
    // Choice 1068 is Barf Mountain Breakdown
    // Choice 1069 is The Pirate Bay
    // Choice 1070 is In Your Cups
    // Choice 1071 is Gator Gamer
    // Choice 1073 is This Ride Is Like... A Rollercoaster Baby Baby
    new ChoiceAdventure(
        1073,
        "Dinseylandfill",
        "This Ride Is Like... A Rollercoaster Baby Baby",
        // Option...
        new ChoiceOption("gain stats and meat", 1),
        new ChoiceOption("skip adventure and guarantees this adventure will reoccur", 6));

    // Choice 1076 is Mayo Minder&trade;

    // Choice 1080 is Bagelmat-5000
    new ChoiceAdventure(
        1080,
        "Town",
        "Bagelmat-5000",
        // Option...
        new ChoiceOption("make 3 plain bagels using wad of dough", 1),
        new ChoiceOption("return to Madness Bakery", 2));

    // Choice 1081 is Assault and Baguettery
    new ChoiceAdventure(
        1081,
        "Item-Driven",
        "magical baguette",
        // Option...
        new ChoiceOption("breadwand", 1, "breadwand"),
        new ChoiceOption("loafers", 2, "loafers"),
        new ChoiceOption("bread basket", 3, "bread basket"),
        new ChoiceOption("make nothing", 4));

    // Choice 1084 is The Popular Machine
    new ChoiceAdventure(
        1084,
        "Town",
        "Popular Machine",
        // Option...
        new ChoiceOption("make popular tart", 1),
        new ChoiceOption("return to Madness Bakery", 2));

    // Choice 1090 is The Towering Inferno Discotheque

    // Choice 1091 is The Floor Is Yours
    new ChoiceAdventure(
        1091,
        "That 70s Volcano",
        "LavaCo Lamp Factory",
        // Option...
        new ChoiceOption("1,970 carat gold -> thin gold wire", 1, "thin gold wire"),
        new ChoiceOption("New Age healing crystal -> empty lava bottle", 2, "empty lava bottle"),
        new ChoiceOption("empty lava bottle -> full lava bottle", 3, "full lava bottle"),
        new ChoiceOption("make colored lava globs", 4),
        new ChoiceOption(
            "glowing New Age crystal -> crystalline light bulb", 5, "crystalline light bulb"),
        new ChoiceOption(
            "crystalline light bulb + insulated wire + heat-resistant sheet metal -> LavaCo&trade; Lamp housing",
            6,
            "LavaCo&trade; Lamp housing"),
        new ChoiceOption("fused fuse", 7, "fused fuse"),
        new ChoiceOption("leave", 9));

    // Choice 1092 is Dyer Maker
    // Choice 1093 is The WLF Bunker

    // Choice 1094 is Back Room SMOOCHing
    new ChoiceAdventure(
        1094,
        "That 70s Volcano",
        "The SMOOCH Army HQ",
        // Option...
        new ChoiceOption("fight Geve Smimmons", 1),
        new ChoiceOption("fight Raul Stamley", 2),
        new ChoiceOption("fight Pener Crisp", 3),
        new ChoiceOption("fight Deuce Freshly", 4),
        new ChoiceOption("acquire SMOOCH coffee cup", 5, "SMOOCH coffee cup"));

    // Choice 1095 is Tin Roof -- Melted
    new ChoiceAdventure(
        1095,
        "That 70s Volcano",
        "The Velvet / Gold Mine",
        // Option...
        new ChoiceOption("fight Mr. Choch", 1),
        new ChoiceOption("acquire half-melted hula girl", 2, "half-melted hula girl"));

    // Choice 1096 is Re-Factory Period
    new ChoiceAdventure(
        1096,
        "That 70s Volcano",
        "LavaCo Lamp Factory",
        // Option...
        new ChoiceOption("fight Mr. Cheeng", 1),
        new ChoiceOption("acquire glass ceiling fragments", 2, "glass ceiling fragments"));

    // Choice 1097 is Who You Gonna Caldera?
    new ChoiceAdventure(
        1097,
        "That 70s Volcano",
        "The Bubblin' Caldera",
        // Option...
        new ChoiceOption("acquire The One Mood Ring", 1, "The One Mood Ring"),
        new ChoiceOption("fight Lavalos", 2));

    // Choice 1102 is The Biggest Barrel

    // Choice 1106 is Wooof! Wooooooof!
    new ChoiceAdventure(
        1106,
        "Item-Driven",
        "Haunted Doghouse 1",
        // Option...
        new ChoiceOption("gain stats", 1),
        new ChoiceOption("+50% all stats for 30 turns", 2),
        new ChoiceOption("acquire familiar food", 3, "Ghost Dog Chow"));

    // Choice 1107 is Playing Fetch*
    new ChoiceAdventure(
        1107,
        "Item-Driven",
        "Haunted Doghouse 2",
        // Option...
        new ChoiceOption("acquire tennis ball", 1, "tennis ball"),
        new ChoiceOption("+50% init for 30 turns", 2),
        new ChoiceOption("acquire ~500 meat", 3));

    // Choice 1108 is Your Dog Found Something Again
    new ChoiceAdventure(
        1108,
        "Item-Driven",
        "Haunted Doghouse 3",
        // Option...
        new ChoiceOption("acquire food", 1),
        new ChoiceOption("acquire booze", 2),
        new ChoiceOption("acquire cursed thing", 3));

    // Choice 1110 is Spoopy
    // Choice 1114 is Walford Rusley, Bucket Collector

    // Choice 1115 is VYKEA!
    new ChoiceAdventure(
        1115,
        "The Glaciest",
        "VYKEA!",
        // Option...
        new ChoiceOption("acquire VYKEA meatballs and mead (1/day)", 1),
        new ChoiceOption("acquire VYKEA hex key", 2, "VYKEA hex key"),
        new ChoiceOption("fill bucket by 10-15%", 3),
        new ChoiceOption(
            "acquire 3 Wal-Mart gift certificates (1/day)", 4, "Wal-Mart gift certificate"),
        new ChoiceOption("acquire VYKEA rune", 5),
        new ChoiceOption("leave", 6));

    // Choice 1116 is All They Got Inside is Vacancy (and Ice)
    new ChoiceAdventure(
        1116,
        "The Glaciest",
        "All They Got Inside is Vacancy (and Ice)",
        // Option...
        new ChoiceOption("fill bucket by 10-15%", 3),
        new ChoiceOption("acquire cocktail ingredients", 4),
        new ChoiceOption(
            "acquire 3 Wal-Mart gift certificates (1/day)", 5, "Wal-Mart gift certificate"),
        new ChoiceOption("leave", 6));

    // Choice 1118 is X-32-F Combat Training Snowman Control Console
    new ChoiceAdventure(
        1118,
        "The Snojo",
        "Control Console",
        // Option...
        new ChoiceOption("muscle training", 1),
        new ChoiceOption("mysticality training", 2),
        new ChoiceOption("moxie training", 3),
        new ChoiceOption("tournament", 4),
        new ChoiceOption("leave", 6));

    // Choice 1119 is Shining Mauve Backwards In Time
    new ChoiceAdventure(
        1119,
        "Town",
        "Deep Machine Tunnels",
        // Option...
        new ChoiceOption("acquire some abstractions", 1),
        new ChoiceOption("acquire abstraction: comprehension", 2, "abstraction: comprehension"),
        new ChoiceOption("acquire modern picture frame", 3, "modern picture frame"),
        new ChoiceOption("duplicate one food, booze, spleen or potion", 4),
        new ChoiceOption("leave", 6));

    // Choice 1120 is Some Assembly Required
    // Choice 1121 is Some Assembly Required
    // Choice 1122 is Some Assembly Required
    // Choice 1123 is Some Assembly Required
    // Choice 1127 is The Crimbo Elf Commune
    // Choice 1128 is Reindeer Commune
    // Choice 1129 is The Crimbulmination
    // Choice 1130 is The Crimbulmination
    // Choice 1131 is The Crimbulmination
    // Choice 1132 is The Crimbulmination

    // Choice 1188 is The Call is Coming from Outside the Simulation
    // Choice 1190 is The Oracle

    // Choice 1195 is Spinning Your Time-Spinner
    // Choice 1196 is Travel to a Recent Fight
    // Choice 1197 is Travel back to a Delicious Meal
    // Choice 1198 is Play a Time Prank
    // Choice 1199 is The Far Future

    // Choice 1202 is Noon in the Civic Center
    new ChoiceAdventure(
        1202,
        "Gingerbread City",
        "Noon in the Civic Center",
        // Option...
        new ChoiceOption("fancy marzipan briefcase", 1, "fancy marzipan briefcase"),
        new ChoiceOption("acquire 50 sprinkles and unlock judge fudge", 2, "sprinkles"),
        new ChoiceOption("enter Civic Planning Office (costs 1000 sprinkles)", 3),
        new ChoiceOption(
            "acquire briefcase full of sprinkles (with gingerbread blackmail photos)", 4));

    // Choice 1203 is Midnight in Civic Center
    new ChoiceAdventure(
        1203,
        "Gingerbread City",
        "Midnight in the Civic Center",
        // Option...
        new ChoiceOption("gain 500 mysticality", 1),
        new ChoiceOption("acquire counterfeit city (costs 300 sprinkles)", 2, "counterfeit city"),
        new ChoiceOption(
            "acquire gingerbread moneybag (with creme brulee torch)", 3, "gingerbread moneybag"),
        new ChoiceOption(
            "acquire 5 gingerbread cigarettes (costs 5 sprinkles)", 4, "gingerbread cigarette"),
        new ChoiceOption(
            "acquire chocolate puppy (with gingerbread dog treat)", 5, "chocolate puppy"));

    // Choice 1204 is Noon at the Train Station
    new ChoiceAdventure(
        1204,
        "Gingerbread City",
        "Noon at the Train Station",
        // Option...
        new ChoiceOption("gain 8-11 candies", 1),
        new ChoiceOption("increase size of sewer gators (with sewer unlocked)", 2),
        new ChoiceOption("gain 250 mysticality", 3));

    // Choice 1205 is Midnight at the Train Station
    new ChoiceAdventure(
        1205,
        "Gingerbread City",
        "Midnight at the Train Station",
        // Option...
        new ChoiceOption("gain 500 muscle and add track", 1),
        new ChoiceOption(
            "acquire broken chocolate pocketwatch (with pumpkin spice candle)",
            2,
            "broken chocolate pocketwatch"),
        new ChoiceOption("enter The Currency Exchange (with candy crowbar)", 3),
        new ChoiceOption(
            "acquire fruit-leather negatives (with track added)", 4, "fruit-leather negatives"),
        new ChoiceOption("acquire various items (with teethpick)", 5));

    // Choice 1206 is Noon in the Industrial Zone
    new ChoiceAdventure(
        1206,
        "Gingerbread City",
        "Noon in the Industrial Zone",
        // Option...
        new ChoiceOption(
            "acquire creme brulee torch (costs 25 sprinkles)", 1, "creme brulee torch"),
        new ChoiceOption("acquire candy crowbar (costs 50 sprinkles)", 2, "candy crowbar"),
        new ChoiceOption("acquire candy screwdriver (costs 100 sprinkles)", 3, "candy screwdriver"),
        new ChoiceOption(
            "acquire teethpick (costs 1000 sprinkles after studying law)", 4, "teethpick"),
        new ChoiceOption(
            "acquire 400-600 sprinkles (with gingerbread mask, pistol and moneybag)", 5));

    // Choice 1207 is Midnight in the Industrial Zone
    new ChoiceAdventure(
        1207,
        "Gingerbread City",
        "Midnight in the Industrial Zone",
        // Option...
        new ChoiceOption("enter Seedy Seedy Seedy", 1),
        new ChoiceOption("enter The Factory Factor", 2),
        new ChoiceOption("acquire tattoo (costs 100000 sprinkles)", 3));

    // Choice 1208 is Upscale Noon
    new ChoiceAdventure(
        1208,
        "Gingerbread City",
        "Upscale Noon",
        // Option...
        new ChoiceOption(
            "acquire gingerbread dog treat (costs 200 sprinkles)", 1, "gingerbread dog treat"),
        new ChoiceOption(
            "acquire pumpkin spice candle (costs 150 sprinkles)", 2, "pumpkin spice candle"),
        new ChoiceOption(
            "acquire gingerbread spice latte (costs 50 sprinkles)", 3, "gingerbread spice latte"),
        new ChoiceOption(
            "acquire gingerbread trousers (costs 500 sprinkles)", 4, "gingerbread trousers"),
        new ChoiceOption(
            "acquire gingerbread waistcoat (costs 500 sprinkles)", 5, "gingerbread waistcoat"),
        new ChoiceOption(
            "acquire gingerbread tophat (costs 500 sprinkles)", 6, "gingerbread tophat"),
        new ChoiceOption(
            "acquire 400-600 sprinkles (with gingerbread mask, pistol and moneybag)", 7),
        new ChoiceOption(
            "acquire gingerbread blackmail photos (drop off fruit-leather negatives and pick up next visit)",
            8,
            "gingerbread blackmail photos"),
        new ChoiceOption("leave", 9));

    // Choice 1209 is Upscale Midnight
    new ChoiceAdventure(
        1209,
        "Gingerbread City",
        "Upscale Midnight",
        // Option...
        new ChoiceOption("acquire fake cocktail", 1, "fake cocktail"),
        new ChoiceOption("enter The Gingerbread Gallery (wearing Gingerbread Best", 2));

    // Choice 1210 is Civic Planning Office
    new ChoiceAdventure(
        1210,
        "Gingerbread City",
        "Civic Planning Office",
        // Option...
        new ChoiceOption("unlock Gingerbread Upscale Retail District", 1),
        new ChoiceOption("unlock Gingerbread Sewers", 2),
        new ChoiceOption("unlock 10 extra City adventures", 3),
        new ChoiceOption("unlock City Clock", 4));

    // Choice 1211 is The Currency Exchange
    new ChoiceAdventure(
        1211,
        "Gingerbread City",
        "The Currency Exchange",
        // Option...
        new ChoiceOption("acquire 5000 meat", 1),
        new ChoiceOption("acquire fat loot token", 2, "fat loot token"),
        new ChoiceOption("acquire 250 sprinkles", 3, "sprinkles"),
        new ChoiceOption("acquire priceless diamond", 4, "priceless diamond"),
        new ChoiceOption("acquire 5 pristine fish scales)", 5, "pristine fish scales"));

    // Choice 1212 is Seedy Seedy Seedy
    new ChoiceAdventure(
        1212,
        "Gingerbread City",
        "Seedy Seedy Seedy",
        // Option...
        new ChoiceOption(
            "acquire gingerbread pistol (costs 300 sprinkles)", 1, "gingerbread pistol"),
        new ChoiceOption("gain 500 moxie", 2),
        new ChoiceOption("ginger beer (with gingerbread mug)", 3, "ginger beer"));

    // Choice 1213 is The Factory Factor
    new ChoiceAdventure(
        1213,
        "Gingerbread City",
        "The Factory Factor",
        // Option...
        new ChoiceOption("acquire spare chocolate parts", 1, "spare chocolate parts"),
        new ChoiceOption("fight GNG-3-R (with gingerservo", 2));

    // Choice 1214 is The Gingerbread Gallery
    new ChoiceAdventure(
        1214,
        "Gingerbread City",
        "The Gingerbread Gallery",
        // Option...
        new ChoiceOption("acquire high-end ginger wine", 1, "high-end ginger wine"),
        new ChoiceOption(
            "acquire fancy chocolate sculpture (costs 300 sprinkles)",
            2,
            "fancy chocolate sculpture"),
        new ChoiceOption("acquire Pop Art: a Guide (costs 1000 sprinkles)", 3, "Pop Art: a Guide"),
        new ChoiceOption("acquire No Hats as Art (costs 1000 sprinkles)", 4, "No Hats as Art"));

    // Choice 1215 is Setting the Clock
    new ChoiceAdventure(
        1215,
        "Gingerbread City",
        "Setting the Clock",
        // Option...
        new ChoiceOption("move clock forward", 1),
        new ChoiceOption("leave", 2));

    // Choice 1217 is Sweet Synthesis
    // Choice 1218 is Wax On

    // Choice 1222 is The Tunnel of L.O.V.E.

    // Choice 1223 is L.O.V. Entrance
    new ChoiceAdventure(
        1223,
        "Tunnel of L.O.V.E.",
        "L.O.V.E Fight 1",
        // Option...
        new ChoiceOption("(free) fight LOV Enforcer", 1),
        new ChoiceOption("avoid fight", 2));

    // Choice 1224 is L.O.V. Equipment Room
    new ChoiceAdventure(
        1224,
        "Tunnel of L.O.V.E.",
        "L.O.V.E Choice 1",
        // Option...
        new ChoiceOption("acquire LOV Eardigan", 1, "LOV Eardigan"),
        new ChoiceOption("acquire LOV Epaulettes", 2, "LOV Epaulettes"),
        new ChoiceOption("acquire LOV Earrings", 3, "LOV Earrings"),
        new ChoiceOption("take nothing", 4));

    // Choice 1225 is L.O.V. Engine Room
    new ChoiceAdventure(
        1225,
        "Tunnel of L.O.V.E.",
        "L.O.V.E Fight 2",
        // Option...
        new ChoiceOption("(free) fight LOV Engineer", 1),
        new ChoiceOption("avoid fight", 2));

    // Choice 1226 is L.O.V. Emergency Room
    new ChoiceAdventure(
        1226,
        "Tunnel of L.O.V.E.",
        "L.O.V.E Choice 2",
        // Option...
        new ChoiceOption("50 adv of Lovebotamy (+10 stats/fight)", 1),
        new ChoiceOption("50 adv of Open Heart Surgery (+10 fam weight)", 2),
        new ChoiceOption("50 adv of Wandering Eye Surgery (+50 item drop)", 3),
        new ChoiceOption("get no buff", 4));

    // Choice 1227 is L.O.V. Elbow Room
    new ChoiceAdventure(
        1227,
        "Tunnel of L.O.V.E.",
        "L.O.V.E Fight 3",
        // Option...
        new ChoiceOption("(free) fight LOV Equivocator", 1),
        new ChoiceOption("avoid fight", 2));

    // Choice 1228 is L.O.V. Emporium
    new ChoiceAdventure(
        1228,
        "Tunnel of L.O.V.E.",
        "L.O.V.E Choice 3",
        // Option...
        new ChoiceOption("acquire LOV Enamorang", 1, "LOV Enamorang"),
        new ChoiceOption("acquire LOV Emotionizer", 2, "LOV Emotionizer"),
        new ChoiceOption(
            "acquire LOV Extraterrestrial Chocolate", 3, "LOV Extraterrestrial Chocolate"),
        new ChoiceOption("acquire LOV Echinacea Bouquet", 4, "LOV Echinacea Bouquet"),
        new ChoiceOption("acquire LOV Elephant", 5, "LOV Elephant"),
        new ChoiceOption("acquire 2 pieces of toast (if have Space Jellyfish)", 6, "toast"),
        new ChoiceOption("take nothing", 7));

    // Choice 1229 is L.O.V. Exit

    // Choice 1236 is Space Cave
    new ChoiceAdventure(
        1236,
        "The Spacegate",
        "Space Cave",
        // Option...
        new ChoiceOption("acquire some alien rock samples", 1, "alien rock sample"),
        new ChoiceOption(
            "acquire some more alien rock samples (with geology kit)", 2, "alien rock sample"),
        new ChoiceOption("skip adventure", 6));

    // Choice 1237 is A Simple Plant
    new ChoiceAdventure(
        1237,
        "The Spacegate",
        "A Simple Plant",
        // Option...
        new ChoiceOption("acquire edible alien plant bit", 1, "edible alien plant bit"),
        new ChoiceOption("acquire alien plant fibers", 2, "alien plant fibers"),
        new ChoiceOption("acquire alien plant sample (with botany kit)", 3, "alien plant sample"),
        new ChoiceOption("skip adventure", 6));

    // Choice 1238 is A Complicated Plant
    new ChoiceAdventure(
        1238,
        "The Spacegate",
        "A Complicated Plant",
        // Option...
        new ChoiceOption("acquire some edible alien plant bit", 1, "edible alien plant bit"),
        new ChoiceOption("acquire some alien plant fibers", 2, "alien plant fibers"),
        new ChoiceOption(
            "acquire complex alien plant sample (with botany kit)",
            3,
            "complex alien plant sample"),
        new ChoiceOption("skip adventure", 6));

    // Choice 1239 is What a Plant!
    new ChoiceAdventure(
        1239,
        "The Spacegate",
        "What a Plant!",
        // Option...
        new ChoiceOption("acquire some edible alien plant bit", 1, "edible alien plant bit"),
        new ChoiceOption("acquire some alien plant fibers", 2, "alien plant fibers"),
        new ChoiceOption(
            "acquire fascinating alien plant sample (with botany kit)",
            3,
            "fascinating alien plant sample"),
        new ChoiceOption("skip adventure", 6));

    // Choice 1240 is The Animals, The Animals
    new ChoiceAdventure(
        1240,
        "The Spacegate",
        "The Animals, The Animals",
        // Option...
        new ChoiceOption("acquire alien meat", 1, "alien meat"),
        new ChoiceOption("acquire alien toenails", 2, "alien toenails"),
        new ChoiceOption(
            "acquire alien zoological sample (with zoology kit)", 3, "alien zoological sample"),
        new ChoiceOption("skip adventure", 6));

    // Choice 1241 is Buffalo-Like Animal, Won't You Come Out Tonight
    new ChoiceAdventure(
        1241,
        "The Spacegate",
        "Buffalo-Like Animal, Won't You Come Out Tonight",
        // Option...
        new ChoiceOption("acquire some alien meat", 1, "alien meat"),
        new ChoiceOption("acquire some alien toenails", 2, "alien toenails"),
        new ChoiceOption(
            "acquire complex alien zoological sample (with zoology kit)",
            3,
            "complex alien zoological sample"),
        new ChoiceOption("skip adventure", 6));

    // Choice 1242 is House-Sized Animal
    new ChoiceAdventure(
        1242,
        "The Spacegate",
        "House-Sized Animal",
        // Option...
        new ChoiceOption("acquire some alien meat", 1, "alien meat"),
        new ChoiceOption("acquire some alien toenails", 2, "alien toenails"),
        new ChoiceOption(
            "acquire fascinating alien zoological sample (with zoology kit)",
            3,
            "fascinating alien zoological sample"),
        new ChoiceOption("skip adventure", 6));

    // Choice 1243 is Interstellar Trade
    new ChoiceAdventure(
        1243,
        "The Spacegate",
        "Interstellar Trade",
        // Option...
        new ChoiceOption("purchase item", 1),
        new ChoiceOption("leave", 6));

    // Choice 1244 is Here There Be No Spants
    new ChoiceAdventure(
        1244,
        "The Spacegate",
        "Here There Be No Spants",
        // Option...
        new ChoiceOption("acquire spant egg casing", 1, "spant egg casing"));

    // Choice 1245 is Recovering the Satellites
    new ChoiceAdventure(
        1245,
        "The Spacegate",
        "Recovering the Satellite",
        // Option...
        new ChoiceOption("acquire murderbot data core", 1, "murderbot data core"));

    // Choice 1246 is Land Ho
    new ChoiceAdventure(
        1246,
        "The Spacegate",
        "Land Ho",
        // Option...
        new ChoiceOption("gain 10% Space Pirate language", 1),
        new ChoiceOption("leave", 6));

    // Choice 1247 is Half The Ship it Used to Be
    new ChoiceAdventure(
        1247,
        "The Spacegate",
        "Half The Ship it Used to Be",
        // Option...
        new ChoiceOption(
            "acquire space pirate treasure map (with enough Space Pirate language)",
            1,
            "space pirate treasure map"),
        new ChoiceOption("leave", 6));

    // Choice 1248 is Paradise Under a Strange Sun
    new ChoiceAdventure(
        1248,
        "The Spacegate",
        "Paradise Under a Strange Sun",
        // Option...
        new ChoiceOption(
            "acquire Space Pirate Astrogation Handbook (with space pirate treasure map)",
            1,
            "Space Pirate Astrogation Handbook"),
        new ChoiceOption("gain 1000 moxie stats", 2),
        new ChoiceOption("leave", 6));

    // Choice 1249 is That's No Moonlith, it's a Monolith!
    new ChoiceAdventure(
        1249,
        "The Spacegate",
        "That's No Moonlith, it's a Monolith!",
        // Option...
        new ChoiceOption("gain 20% procrastinator language (with murderbot data core)", 1),
        new ChoiceOption("leave", 6));

    // Choice 1250 is I'm Afraid It's Terminal
    new ChoiceAdventure(
        1250,
        "The Spacegate",
        "I'm Afraid It's Terminal",
        // Option...
        new ChoiceOption(
            "acquire procrastinator locker key (with enough procrastinator language)",
            1,
            "Procrastinator locker key"),
        new ChoiceOption("leave", 6));

    // Choice 1251 is Curses, a Hex
    new ChoiceAdventure(
        1251,
        "The Spacegate",
        "Curses, a Hex",
        // Option...
        new ChoiceOption(
            "acquire Non-Euclidean Finance (with procrastinator locker key)",
            1,
            "Non-Euclidean Finance"),
        new ChoiceOption("leave", 6));

    // Choice 1252 is Time Enough at Last
    new ChoiceAdventure(
        1252,
        "The Spacegate",
        "Time Enough at Last",
        // Option...
        new ChoiceOption("acquire Space Baby childrens' book", 1, "Space Baby childrens' book"),
        new ChoiceOption("leave", 6));

    // Choice 1253 is Mother May I
    new ChoiceAdventure(
        1253,
        "The Spacegate",
        "Mother May I",
        // Option...
        new ChoiceOption(
            "acquire Space Baby bawbaw (with enough Space Baby language)", 1, "Space Baby bawbaw"),
        new ChoiceOption("leave", 6));

    // Choice 1254 is Please Baby Baby Please
    new ChoiceAdventure(
        1254,
        "The Spacegate",
        "Please Baby Baby Please",
        // Option...
        new ChoiceOption("acquire Peek-a-Boo! (with Space Baby bawbaw)", 1, "Peek-a-Boo!"),
        new ChoiceOption("leave", 6));

    // Choice 1255 is Cool Space Rocks
    new ChoiceAdventure(
        1255,
        "The Spacegate",
        "Cool Space Rocks",
        // Option...
        new ChoiceOption("acquire some alien rock samples", 1, "alien rock sample"),
        new ChoiceOption(
            "acquire some more alien rock samples (with geology kit)", 2, "alien rock sample"));

    // Choice 1256 is Wide Open Spaces
    new ChoiceAdventure(
        1256,
        "The Spacegate",
        "Wide Open Spaces",
        // Option...
        new ChoiceOption("acquire some alien rock samples", 1, "alien rock sample"),
        new ChoiceOption(
            "acquire some more alien rock samples (with geology kit)", 2, "alien rock sample"));

    // Choice 1280 is Welcome to FantasyRealm
    new ChoiceAdventure(
        1280,
        "FantasyRealm",
        "Welcome to FantasyRealm",
        // Option...
        new ChoiceOption("acquire FantasyRealm Warrior's Helm", 1, "FantasyRealm Warrior's Helm"),
        new ChoiceOption("acquire FantasyRealm Mage's Hat", 2, "FantasyRealm Mage's Hat"),
        new ChoiceOption("acquire FantasyRealm Rogue's Mask", 3, "FantasyRealm Rogue's Mask"),
        new ChoiceOption("leave", 6));

    // Choice 1281 is You'll See You at the Crossroads
    new ChoiceAdventure(
        1281,
        "FantasyRealm",
        "You'll See You at the Crossroads",
        // Option...
        new ChoiceOption("unlock The Towering Mountains", 1),
        new ChoiceOption("unlock The Mystic Wood", 2),
        new ChoiceOption("unlock The Putrid Swamp", 3),
        new ChoiceOption("unlock Cursed Village", 4),
        new ChoiceOption("unlock The Sprawling Cemetery", 5),
        new ChoiceOption("leave", 8));

    // Choice 1282 is Out of Range
    new ChoiceAdventure(
        1282,
        "FantasyRealm",
        "Out of Range",
        // Option...
        new ChoiceOption("unlock The Old Rubee Mine (using FantasyRealm key)", 1),
        new ChoiceOption("unlock The Foreboding Cave", 2),
        new ChoiceOption("unlock The Master Thief's Chalet (with FantasyRealm Rogue's Mask)", 3),
        new ChoiceOption("charge druidic orb (need orb)", 4, "charged druidic orb"),
        new ChoiceOption("unlock The Ogre Chieftain's Keep (with FantasyRealm Warrior's Helm)", 5),
        new ChoiceOption("1/5 to fight Skeleton Lord (with FantasyRealm outfit)", 10),
        new ChoiceOption("leave", 11));

    // Choice 1283 is Where Wood You Like to Go
    new ChoiceAdventure(
        1283,
        "FantasyRealm",
        "Where Wood You Like to Go",
        // Option...
        new ChoiceOption("unlock The Faerie Cyrkle", 1),
        new ChoiceOption("unlock The Druidic Campsite (with LyleCo premium rope)", 2),
        new ChoiceOption("unlock The Ley Nexus (with Cheswick Copperbottom's compass)", 3),
        new ChoiceOption("acquire plump purple mushroom", 5, "plump purple mushroom"),
        new ChoiceOption("1/5 to fight Skeleton Lord (with FantasyRealm outfit)", 10),
        new ChoiceOption("leave", 11));

    // Choice 1284 is Swamped with Leisure
    new ChoiceAdventure(
        1284,
        "FantasyRealm",
        "Swamped with Leisure",
        // Option...
        new ChoiceOption("unlock Near the Witch's House", 1),
        new ChoiceOption("unlock The Troll Fortress (using FantasyRealm key)", 2),
        new ChoiceOption("unlock The Dragon's Moor (with FantasyRealm Warrior's Helm)", 3),
        new ChoiceOption("acquire tainted marshmallow", 5, "tainted marshmallow"),
        new ChoiceOption("1/5 to fight Skeleton Lord (with FantasyRealm outfit)", 10),
        new ChoiceOption("leave", 11));

    // Choice 1285 is It Takes a Cursed Village
    new ChoiceAdventure(
        1285,
        "FantasyRealm",
        "It Takes a Cursed Village",
        // Option...
        new ChoiceOption("unlock The Evil Cathedral", 1),
        new ChoiceOption(
            "unlock The Cursed Village Thieves' Guild (using FantasyRealm Rogue's Mask)", 2),
        new ChoiceOption("unlock The Archwizard's Tower (with FantasyRealm Mage's Hat)", 3),
        new ChoiceOption("get 20 adv of +2-3 Rubee&trade; drop", 4),
        new ChoiceOption(
            "acquire 40-60 Rubees&trade; (with LyleCo premium rope)", 5, "Rubee&trade;"),
        new ChoiceOption(
            "acquire dragon slaying sword (with dragon aluminum ore)", 6, "dragon slaying sword"),
        new ChoiceOption(
            "acquire notarized arrest warrant (with arrest warrant)",
            7,
            "notarized arrest warrant"),
        new ChoiceOption("1/5 to fight Skeleton Lord (with FantasyRealm outfit)", 10),
        new ChoiceOption("leave", 11));

    // Choice 1286 is Resting in Peace
    new ChoiceAdventure(
        1286,
        "FantasyRealm",
        "Resting in Peace",
        // Option...
        new ChoiceOption("unlock The Labyrinthine Crypt", 1),
        new ChoiceOption("unlock The Barrow Mounds", 2),
        new ChoiceOption("unlock Duke Vampire's Chateau (with FantasyRealm Rogue's Mask)", 3),
        new ChoiceOption(
            "acquire 40-60 Rubees&trade; (need LyleCo premium pickaxe)", 4, "Rubee&trade;"),
        new ChoiceOption(
            "acquire Chewsick Copperbottom's notes (with FantasyRealm Mage's Hat)",
            5,
            "Chewsick Copperbottom's notes"),
        new ChoiceOption("1/5 to fight Skeleton Lord (with FantasyRealm outfit)", 10),
        new ChoiceOption("leave", 11));

    // Choice 1288 is What's Yours is Yours
    new ChoiceAdventure(
        1288,
        "FantasyRealm",
        "What's Yours is Yours",
        // Option...
        new ChoiceOption("acquire 20-30 Rubees&trade;", 1, "Rubee&trade;"),
        new ChoiceOption(
            "acquire dragon aluminum ore (need LyleCo premium pickaxe)", 2, "dragon aluminum ore"),
        new ChoiceOption("acquire grolblin rum", 3, "grolblin rum"),
        new ChoiceOption("leave", 6));

    // Choice 1289 is A Warm Place
    new ChoiceAdventure(
        1289,
        "FantasyRealm",
        "A Warm Place",
        // Option...
        new ChoiceOption("acquire 90-110 Rubees&trade; (with FantasyRealm key)", 1, "Rubee&trade;"),
        new ChoiceOption("acquire sachet of strange powder", 2, "sachet of strange powder"),
        new ChoiceOption("unlock The Lair of the Phoenix (with FantasyRealm Mage's Hat)", 3),
        new ChoiceOption("leave", 6));

    // Choice 1290 is The Cyrkle Is Compleat
    new ChoiceAdventure(
        1290,
        "FantasyRealm",
        "The Cyrkle Is Compleat",
        // Option...
        new ChoiceOption("get 100 adv of Fantasy Faerie Blessing", 1),
        new ChoiceOption("acquire faerie dust", 2, "faerie dust"),
        new ChoiceOption("unlock The Spider Queen's Lair (with FantasyRealm Rogue's Mask)", 3),
        new ChoiceOption("leave", 6));

    // Choice 1291 is Dudes, Where's My Druids?
    new ChoiceAdventure(
        1291,
        "FantasyRealm",
        "Dudes, Where's My Druids?",
        // Option...
        new ChoiceOption("acquire druidic s'more", 1, "druidic s'more"),
        new ChoiceOption(
            "acquire poisoned druidic s'more (with tainted marshmallow)",
            2,
            "poisoned druidic s'more"),
        new ChoiceOption("acquire druidic orb (with FantasyRealm Mage's Hat)", 3, "druidic orb"),
        new ChoiceOption("leave", 6));

    // Choice 1292 is Witch One You Want?
    new ChoiceAdventure(
        1292,
        "FantasyRealm",
        "Witch One You Want?",
        // Option...
        new ChoiceOption("get 50 adv of +200% init", 1),
        new ChoiceOption("get 10 adv of Poison for Blood (with plump purple mushroom)", 2),
        new ChoiceOption("acquire to-go brew", 3, "to-go brew"),
        new ChoiceOption("acquire 40-60 Rubees&trade;", 4, "Rubee&trade;"),
        new ChoiceOption("leave", 6));

    // Choice 1293 is Altared States
    new ChoiceAdventure(
        1293,
        "FantasyRealm",
        "Altared States",
        // Option...
        new ChoiceOption("acquire 20-30 Rubees&trade;", 1, "Rubee&trade;"),
        new ChoiceOption("get 100 adv of +200% HP", 2),
        new ChoiceOption("acquire sanctified cola", 3, "sanctified cola"),
        new ChoiceOption(
            "acquire flask of holy water (with FantasyRealm Mage's Hat)", 4, "flask of holy water"),
        new ChoiceOption("leave", 6));

    // Choice 1294 is Neither a Barrower Nor a Lender Be
    new ChoiceAdventure(
        1294,
        "FantasyRealm",
        "Neither a Barrower Nor a Lender Be",
        // Option...
        new ChoiceOption("acquire 20-30 Rubees&trade;", 1, "Rubee&trade;"),
        new ChoiceOption("acquire mourning wine", 2, "mourning wine"),
        new ChoiceOption("unlock The Ghoul King's Catacomb (with FantasyRealm Warrior's Helm)", 3),
        new ChoiceOption("leave", 6));

    // Choice 1295 is Honor Among You
    new ChoiceAdventure(
        1295,
        "FantasyRealm",
        "Honor Among You",
        // Option...
        new ChoiceOption("acquire 40-60 Rubees&trade;", 1, "Rubee&trade;"),
        new ChoiceOption("acquire universal antivenin", 2, "universal antivenin"),
        new ChoiceOption("leave", 6));

    // Choice 1296 is For Whom the Bell Trolls
    new ChoiceAdventure(
        1296,
        "FantasyRealm",
        "For Whom the Bell Trolls",
        // Option...
        new ChoiceOption("nothing happens", 1),
        new ChoiceOption("acquire nasty haunch", 2, "nasty haunch"),
        new ChoiceOption(
            "acquire Cheswick Copperbottom's compass (with Chewsick Copperbottom's notes)",
            3,
            "Cheswick Copperbottom's compass"),
        new ChoiceOption(
            "acquire 40-60 Rubees&trade; (with LyleCo premium pickaxe)", 4, "Rubee&trade;"),
        new ChoiceOption("leave", 6));

    // Choice 1297 is Stick to the Crypt
    new ChoiceAdventure(
        1297,
        "FantasyRealm",
        "Stick to the Crypt",
        // Option...
        new ChoiceOption("acquire hero's skull", 1, "hero's skull"),
        new ChoiceOption("acquire 40-60 Rubees&trade;", 2, "Rubee&trade;"),
        new ChoiceOption(
            "acquire arrest warrant (with FantasyRealm Rogue's Mask)", 3, "arrest warrant"),
        new ChoiceOption("leave", 6));

    // Choice 1298 is The "Phoenix"
    new ChoiceAdventure(
        1298,
        "FantasyRealm",
        "The \"Phoenix\"",
        // Option...
        new ChoiceOption("fight \"Phoenix\" (with 5+ hot res and flask of holy water)", 1),
        new ChoiceOption("get beaten up", 2),
        new ChoiceOption("leave", 6));

    // Choice 1299 is Stop Dragon Your Feet
    new ChoiceAdventure(
        1299,
        "FantasyRealm",
        "Stop Dragon Your Feet",
        // Option...
        new ChoiceOption(
            "fight Sewage Treatment Dragon (with 5+ stench res and dragon slaying sword)", 1),
        new ChoiceOption("get beaten up", 2),
        new ChoiceOption("leave", 6));

    // Choice 1300 is Just Vamping
    new ChoiceAdventure(
        1300,
        "FantasyRealm",
        "Just Vamping",
        // Option...
        new ChoiceOption("fight Duke Vampire (with 250%+ init and Poison for Blood)", 1),
        new ChoiceOption("get beaten up", 2),
        new ChoiceOption("leave", 6));

    // Choice 1301 is Now You've Spied Her
    new ChoiceAdventure(
        1301,
        "FantasyRealm",
        "Now You've Spied Her",
        // Option...
        new ChoiceOption("fight Spider Queen (with 500+ mox and Fantastic Immunity)", 1),
        new ChoiceOption("get beaten up", 2),
        new ChoiceOption("leave", 6));

    // Choice 1302 is Don't Be Arch
    new ChoiceAdventure(
        1302,
        "FantasyRealm",
        "Don't Be Arch",
        // Option...
        new ChoiceOption("fight Archwizard (with 5+ cold res and charged druidic orb)", 1),
        new ChoiceOption("get beaten up", 2),
        new ChoiceOption("leave", 6));

    // Choice 1303 is Ley Lady Ley
    new ChoiceAdventure(
        1303,
        "FantasyRealm",
        "Ley Lady Ley",
        // Option...
        new ChoiceOption(
            "fight Ley Incursion (with 500+ mys and Cheswick Copperbottom's compass)", 1),
        new ChoiceOption("get beaten up", 2),
        new ChoiceOption("leave", 6));

    // Choice 1304 is He Is the Ghoul King, He Can Do Anything
    new ChoiceAdventure(
        1304,
        "FantasyRealm",
        "He Is the Ghoul King, He Can Do Anything",
        // Option...
        new ChoiceOption("fight Ghoul King (with 5+ spooky res and Fantasy Faerie Blessing)", 1),
        new ChoiceOption("get beaten up", 2),
        new ChoiceOption("leave", 6));

    // Choice 1305 is The Brogre's Progress
    new ChoiceAdventure(
        1305,
        "FantasyRealm",
        "The Brogre's Progress",
        // Option...
        new ChoiceOption("fight Ogre Chieftain (with 500+ mus and poisoned druidic s'more)", 1),
        new ChoiceOption("get beaten up", 2),
        new ChoiceOption("leave", 6));

    // Choice 1307 is It Takes a Thief
    new ChoiceAdventure(
        1307,
        "FantasyRealm",
        "It Takes a Thief",
        // Option...
        new ChoiceOption(
            "fight Ted Schwartz, Master Thief (with 5+ sleaze res and notarized arrest warrant)",
            1),
        new ChoiceOption("get beaten up", 2),
        new ChoiceOption("leave", 6));

    // Choice 1310 is Granted a Boon
    // Choice 1312 is Choose a Soundtrack

    // Choice 1313 is Bastille Battalion
    // Choice 1314 is Bastille Battalion (turn #x)
    // Choice 1315 is Castle vs. Castle
    // Choice 1316 is GAME OVER
    // Choice 1317 is A Hello to Arms
    // Choice 1318 is Defensive Posturing
    // Choice 1319 is Cheese Seeking Behavior

    // Choice 1321 is Disguises Delimit

    // Choice 1322 is The Beginning of the Neverend
    new ChoiceAdventure(
        1322,
        "Neverending Party",
        "Neverending Party Intro",
        // Option...
        new ChoiceOption("accept quest", 1),
        new ChoiceOption("reject quest", 2),
        new ChoiceOption("leave", 6));

    // Choice 1323 is All Done!

    // Choice 1324 is It Hasn't Ended, It's Just Paused
    new ChoiceAdventure(
        1324,
        "Neverending Party",
        "Neverending Party Pause",
        // Option...
        new ChoiceOption(
            "Full HP/MP heal, +Mys Exp (20adv), clear partiers (quest), DJ meat (quest), megawoots (quest)",
            1),
        new ChoiceOption("Mys stats, +Mus Exp (20 adv), snacks quest, burn trash (quest)", 2),
        new ChoiceOption("Mox stats, +30 ML (50 adv), clear partiers (quest), booze quest", 3),
        new ChoiceOption("Mus stats, +Mox Exp (20 adv), chainsaw, megawoots (quest)", 4),
        new ChoiceOption("fight random partier", 5));

    // Choice 1325 is A Room With a View...  Of a Bed
    new ChoiceAdventure(
        1325,
        "Neverending Party",
        "Neverending Party Bedroom",
        // Option...
        new ChoiceOption("full HP/MP heal", 1),
        new ChoiceOption("get 20 adv of +20% mys exp", 2),
        new ChoiceOption("remove partiers (with jam band bootleg)", 3),
        new ChoiceOption("get meat for dj (with 300 Moxie)", 4),
        new ChoiceOption("increase megawoots", 5));

    // Choice 1326 is Gone Kitchin'
    new ChoiceAdventure(
        1326,
        "Neverending Party",
        "Neverending Party Kitchen",
        // Option...
        new ChoiceOption("gain mys stats", 1),
        new ChoiceOption("get 20 adv of +20% Mus exp", 2),
        new ChoiceOption("find out food to collect", 3),
        new ChoiceOption("give collected food", 4),
        new ChoiceOption("reduce trash", 5));

    // Choice 1327 is Forward to the Back
    new ChoiceAdventure(
        1327,
        "Neverending Party",
        "Neverending Party Back Yard",
        // Option...
        new ChoiceOption("gain mox stats", 1),
        new ChoiceOption("get 50 adv of +30 ML", 2),
        new ChoiceOption("find out booze to collect", 3),
        new ChoiceOption("give collected booze", 4),
        new ChoiceOption("remove partiers (with Purple Beast energy drink)", 5));

    // Choice 1328 is Basement Urges
    new ChoiceAdventure(
        1328,
        "Neverending Party",
        "Neverending Party Basement",
        // Option...
        new ChoiceOption("gain mus stats", 1),
        new ChoiceOption("get 20 adv of +20% Mox exp", 2),
        new ChoiceOption("acquire intimidating chainsaw", 3, "intimidating chainsaw"),
        new ChoiceOption("increase megawoots", 4));

    // Choice 1331 is Daily Loathing Ballot
    // Choice 1332 is government requisition form

    // Choice 1333 is Canadian Cabin
    new ChoiceAdventure(
        1333,
        "Crimbo18",
        "Canadian Cabin",
        // Option...
        new ChoiceOption("gain 50 adv of +100% weapon and spell damage", 1),
        new ChoiceOption("acquire grilled mooseflank (with mooseflank)", 2, "grilled mooseflank"),
        new ChoiceOption(
            "acquire antique Canadian lantern (with 10 thick walrus blubber)",
            3,
            "antique Canadian lantern"),
        new ChoiceOption("acquire muskox-skin cap (with 10 tiny bombs)", 4, "muskox-skin cap"),
        new ChoiceOption("acquire antique beer (with Yeast-Hungry)", 5, "antique beer"),
        new ChoiceOption("skip adventure", 10));

    // Choice 1334 is Boxing Daycare (Lobby)
    // Choice 1335 is Boxing Day Spa
    new ChoiceAdventure(
        1335,
        "Town",
        "Boxing Day Spa",
        // Option...
        new ChoiceOption("gain 100 adv of +200% muscle and +15 ML"),
        new ChoiceOption("gain 100 adv of +200% moxie and +50% init"),
        new ChoiceOption("gain 100 adv of +200% myst and +25% item drop"),
        new ChoiceOption(
            "gain 100 adv of +100 max hp, +50 max mp, +25 dr, 5-10 mp regen, 10-20 hp regen"),
        new ChoiceOption("skip"));

    // Choice 1336 is Boxing Daycare
    // Choice 1339 is A Little Pump and Grind

    // Choice 1340 is Is There A Doctor In The House?
    new ChoiceAdventure(
        1340,
        "Item-Driven",
        "Lil' Doctor&trade; bag Quest",
        // Option...
        new ChoiceOption("get quest", 1),
        new ChoiceOption("refuse quest", 2),
        new ChoiceOption("stop offering quest", 3));

    // Choice 1341 is A Pound of Cure
    new ChoiceAdventure(
        1341,
        "Item-Driven",
        "Lil' Doctor&trade; bag Cure",
        // Option...
        new ChoiceOption("cure patient", 1));

    // Choice 1342 is Torpor

    // Choice 1345 is Blech House
    new ChoiceAdventure(
        1345,
        "Mountain",
        "Blech House",
        // Option...
        new ChoiceOption("use muscle/weapon damage", 1),
        new ChoiceOption("use myst/spell damage", 2),
        new ChoiceOption("use mox/sleaze res", 3));

    // Choice 1392 is Decorate your Tent
    new ChoiceSpoiler(
        1392,
        "Decorate your Tent",
        // Option...
        new ChoiceOption("gain 20 adv of +3 mus xp"),
        new ChoiceOption("gain 20 adv of +3 mys xp"),
        new ChoiceOption("gain 20 adv of +3 mox xp"));

    // Choice 1397 is Kringle workshop
    new ChoiceAdventure(
        1397,
        "Tammy's Offshore Platform",
        "Kringle workshop",
        // Option...
        new ChoiceOption("craft stuff", 1),
        new ChoiceOption("get waterlogged items", 2),
        new ChoiceOption("fail at life", 3));

    // Choice 1411 is The Hall in the Hall
    new ChoiceAdventure(
        1411,
        "The Drip",
        "The Hall in the Hall",
        // Option...
        new ChoiceOption("drippy pool table", 1),
        new ChoiceOption("drippy vending machine", 2),
        new ChoiceOption("drippy humanoid", 3),
        new ChoiceOption("drippy keg", 4),
        new ChoiceOption("Driplets", 5));

    // Choice 1415 is Revolting Vending
    new ChoiceAdventure(
        1415,
        "The Drip",
        "Revolting Vending",
        // Option...
        new ChoiceOption("drippy candy bar", 1, "drippy candy bar"),
        new ChoiceOption("Driplets", 2));
    new ChoiceCost(1415, new Cost(1, new AdventureResult(AdventureResult.MEAT, -10000)));

    // Choice 1427 is The Hidden Junction
    new ChoiceAdventure(
        1427,
        "BatHole",
        "The Hidden Junction",
        // Option...
        new ChoiceOption("fight screambat", 1),
        new ChoiceOption("gain 300-400 meat", 2));

    // Choice 1428 is Your Neck of the Woods
    new ChoiceAdventure(
        1428,
        "Friars",
        "Your Neck of the Woods",
        // Option...
        new ChoiceOption("advance quest 1 step and gain 1000 meat", 1),
        new ChoiceOption("advance quest 2 steps", 2));

    // Choice 1429 is No Nook Unknown
    new ChoiceAdventure(
        1429,
        "Cyrpt",
        "No Nook Unknown",
        // Option...
        new ChoiceOption("acquire 2 evil eyes", 1),
        new ChoiceOption("fight party skeleton", 2));

    // Choice 1430 is Ghostly Memories
    new ChoiceAdventure(
        1430,
        "Highlands",
        "Ghostly Memories",
        // Option...
        new ChoiceOption("the Horror, spooky/cold res recommended", 1),
        new ChoiceOption("fight oil baron", 2),
        new ChoiceOption("lost overlook lodge", 3));

    // Choice 1431 is Here There Be Giants
    new ChoiceAdventure(
        1431,
        "Beanstalk",
        "Here There Be Giants",
        // Option...
        new ChoiceOption("complete trash quest, unlock HiTS", 1),
        new ChoiceOption("fight goth giant, acquire black candles", 2),
        new ChoiceOption("fight raver, restore hp/mp", 3),
        new ChoiceOption("complete quest w/ mohawk wig, gain ~500 meat", 4));

    // Choice 1432 is Mob Maptality
    new ChoiceAdventure(
        1432,
        "The Red Zeppelin's Mooring",
        "Mob Maptality",
        // Option...
        new ChoiceOption("creep protestors (more with sleaze damage/sleaze spell damage)", 1),
        new ChoiceOption("scare protestors (more with lynyrd gear)", 2),
        new ChoiceOption("set fire to protestors (more with Flamin' Whatshisname)", 3));

    // Choice 1433 is Hippy camp verge of war Sneaky Sneaky
    new ChoiceAdventure(
        1433,
        "Island",
        "Sneaky Sneaky",
        // Option...
        new ChoiceOption("fight a war hippy drill sergeant", 1),
        new ChoiceOption("fight a war hippy space cadet", 2),
        new ChoiceOption("start the war", 3));

    // Choice 1434 is frat camp verge of war Sneaky Sneaky
    new ChoiceAdventure(
        1434,
        "Island",
        "Sneaky Sneaky",
        // Option...
        new ChoiceOption("fight a war pledge/acquire sake bombs", 1),
        new ChoiceOption("start the war", 2),
        new ChoiceOption("fight a frat warrior drill sergeant/acquire beer bombs", 3));

    // Choice 1436 is Billiards Room Options
    new ChoiceAdventure(
        1436,
        "Manor1",
        "Billiards Room Options",
        // Option...
        new ChoiceOption("aquire pool cue", 1),
        new ChoiceOption("play pool with the ghost", 2),
        new ChoiceOption("fight a chalkdust wraith", 3));

    // Gift Fabrication Lab
    new ChoiceAdventure(
        1460,
        "Crimbo21",
        "Site Alpha Toy Lab",
        // Option...
        new ChoiceOption("fleshy putty", "fleshy putty", "third ear", "festive egg sac"),
        new ChoiceOption(
            "poisonsettia", "poisonsettia", "peppermint-scented socks", "the Crymbich Manuscript"),
        new ChoiceOption(
            "projectile chemistry set",
            "projectile chemistry set",
            "depleted Crimbonium football helmet",
            "synthetic rock"),
        new ChoiceOption(
            "&quot;caramel&quot; orange",
            "&quot;caramel&quot; orange",
            "self-repairing earmuffs",
            "carnivorous potted plant"),
        new ChoiceOption(
            "universal biscuit", "universal biscuit", "yule hatchet", "potato alarm clock"),
        new ChoiceOption(
            "lab-grown meat", "lab-grown meat", "golden fleece", "boxed gumball machine"),
        new ChoiceOption("cloning kit", "cloning kit", "electric pants", "can of mixed everything"),
        new ChoiceOption("return to Site Alpha"));

    // Hello Knob My Old Friend
    new ChoiceAdventure(
        1461,
        "Crimbo21",
        "Site Alpha Primary Lab",
        // Option...
        new ChoiceOption("Increase goo intensity", 1),
        new ChoiceOption("Decrease goo intensity", 2),
        new ChoiceOption("Trade grey goo ring for gooified matter", 3),
        new ChoiceOption("Do nothing", 4),
        new ChoiceOption("Grab the cheer core. Just do it!", 5));

    // Poetic Justice
    new ChoiceAdventure(
        1467,
        "Item-Driven",
        "June cleaver",
        // Option...
        new ChoiceOption("Moxie substats", 1),
        new ChoiceOption("Mysticality substats", 2),
        new ChoiceOption("Gain 5 adventures, get beaten up", 3),
        new ChoiceOption("Do nothing", 4));

    // Aunts not Ants
    new ChoiceAdventure(
        1468,
        "Item-Driven",
        "June cleaver",
        // Option...
        new ChoiceOption("Moxie substats", 1),
        new ChoiceOption("Muscle substats", 2),
        new ChoiceOption("get Ashamed", 3),
        new ChoiceOption("Do nothing", 4));

    // Beware of Alligator
    new ChoiceAdventure(
        1469,
        "Item-Driven",
        "June cleaver",
        // Option...
        new ChoiceOption("get Yapping Pal", 1),
        new ChoiceOption("Dad's brandy", 2, "Dad's brandy"),
        new ChoiceOption("1500 meat", 3),
        new ChoiceOption("Do nothing", 4));

    // Teacher's Pet
    new ChoiceAdventure(
        1470,
        "Item-Driven",
        "June cleaver",
        // Option...
        new ChoiceOption("30 turns of Teacher's Pet", 1),
        new ChoiceOption("teacher's pen", 2, "teacher's pen"),
        new ChoiceOption("Muscle substats", 3),
        new ChoiceOption("Do nothing", 4));

    // Lost and Found
    new ChoiceAdventure(
        1471,
        "Item-Driven",
        "June cleaver",
        // Option...
        new ChoiceOption("savings bond", 1, "savings bond"),
        new ChoiceOption("Muscle substats, 250 meat, get beaten up", 2),
        new ChoiceOption("Mysticality substats", 3),
        new ChoiceOption("Do nothing", 4));

    // Summer Days
    new ChoiceAdventure(
        1472,
        "Item-Driven",
        "June cleaver",
        // Option...
        new ChoiceOption("trampled ticket stub", 1, "trampled ticket stub"),
        new ChoiceOption("fire-roasted lake trout", 2, "fire-roasted lake trout"),
        new ChoiceOption("Moxie substats", 3),
        new ChoiceOption("Do nothing", 4));

    // Bath Time
    new ChoiceAdventure(
        1473,
        "Item-Driven",
        "June cleaver",
        // Option...
        new ChoiceOption("Muscle substats, gob of wet hair", 1, "gob of wet hair"),
        new ChoiceOption("get Wholesomely Resolved", 2),
        new ChoiceOption("get Kinda Damp", 3),
        new ChoiceOption("Do nothing", 4));

    // Delicious Sprouts
    new ChoiceAdventure(
        1474,
        "Item-Driven",
        "June cleaver",
        // Option...
        new ChoiceOption("Mysticality substats", 1),
        new ChoiceOption("guilty sprout", 2, "guilty sprout"),
        new ChoiceOption("Muscle substats", 3),
        new ChoiceOption("Do nothing", 4));

    // Hypnotic Master
    new ChoiceAdventure(
        1475,
        "Item-Driven",
        "June cleaver",
        // Option...
        new ChoiceOption("mother's necklace", 1, "mother's necklace"),
        new ChoiceOption("Muscle substats", 2),
        new ChoiceOption("Two random effects", 3),
        new ChoiceOption("Do nothing", 4));

    // Choose an Action During a Caboose Distraction
    new ChoiceAdventure(
        1486,
        "Crimbo22",
        "Crimbo Train (Caboose)",
        new ChoiceOption("acquire 6 Trainbot potions", 1),
        new ChoiceOption("+3 Elf Gratitude", 2),
        new ChoiceOption("acquire a ping-pong paddle then acquire 3-5 ping-pong balls", 3));

    // A Passenger Among Passengers
    new ChoiceAdventure(
        1487, "Crimbo22", "Crimbo Train (Passenger Car)", new ChoiceOption("+5 Elf Gratitude", 1));

    // Pre-Dinner Activities
    new ChoiceAdventure(
        1488,
        "Crimbo22",
        "Crimbo Train (Dining Car)",
        new ChoiceOption("acquire 3 lost elf trunks", 1),
        new ChoiceOption("decrease Trainbot strength", 2));

    // Slagging Off
    new ChoiceAdventure(
        1489,
        "Crimbo22",
        "Crimbo Train (Coal Car)",
        new ChoiceOption("shard -> crystal Crimbo goblet, or none", 1),
        new ChoiceOption("shard -> crystal Crimbo platter, or none", 2),
        new ChoiceOption("shard -> goblet or platter, or none", 3));

    // Strange Stalagmite(s)
    new ChoiceAdventure(
        1491,
        "Item-Driven",
        "strange stalagmite",
        new ChoiceOption("muscle substats", 1),
        new ChoiceOption("mysticality substats", 2),
        new ChoiceOption("moxie substats", 3));

    // Examine S.I.T. Course Certificate
    new ChoiceAdventure(
        1494,
        "Item-Driven",
        "S.I.T. Course Certificate",
        new ChoiceOption("Psychogeologist", 1),
        new ChoiceOption("Insectologist", 2),
        new ChoiceOption("Cryptobotanist", 3));

    // Sing!
    new ChoiceAdventure(
        1505,
        "Item-Driven",
        "Loathing Idol Microphone",
        new ChoiceOption("30 turns of +100% init, +50% moxie", 1),
        new ChoiceOption("30 turns of +5% combat chance", 2),
        new ChoiceOption("30 turns of +50% item drop", 3),
        new ChoiceOption("30 turns of +3 exp, +4 stench/sleaze res", 4));
  }

  // This array is used by the ChoiceOptionsPanel to provide all the GUI configurable choices.
  // Everywhere else SHOULD use the choiceToChoiceAdventure map
  public static final ChoiceAdventure[] CHOICE_ADVS;

  static {
    CHOICE_ADVS =
        choiceToChoiceAdventure
            .values()
            .toArray(new ChoiceAdventure[choiceToChoiceAdventure.size()]);
    Arrays.sort(CHOICE_ADVS);

    // Log errors detected during class initialization. These are programming
    // errors and the developer who inserted the bug should have seen the
    // report on stdout, but, just in case...
    if (duplicateChoiceAdventures.size() > 0) {
      RequestLogger.printLine(
          "Duplicate ChoiceAdventures: ("
              + duplicateChoiceAdventures.stream()
                  .map(String::valueOf)
                  .collect(Collectors.joining(","))
              + ")");
    }
    if (missingChoiceAdventureOptions.size() > 0) {
      RequestLogger.printLine(
          "Missing ChoiceAdventure Options: ("
              + missingChoiceAdventureOptions.stream()
                  .map(String::valueOf)
                  .collect(Collectors.joining(","))
              + ")");
    }
    if (missingChoiceAdventureDefaultProperties.size() > 0) {
      RequestLogger.printLine(
          "Missing ChoiceAdventure default peoperties: ("
              + missingChoiceAdventureDefaultProperties.stream()
                  .map(String::valueOf)
                  .collect(Collectors.joining(","))
              + ")");
    }
    if (duplicateChoiceSpoilers.size() > 0) {
      RequestLogger.printLine(
          "Duplicate ChoiceSpoilers: ("
              + duplicateChoiceSpoilers.stream()
                  .map(String::valueOf)
                  .collect(Collectors.joining(","))
              + ")");
    }
    if (duplicateChoiceCosts.size() > 0) {
      RequestLogger.printLine(
          "Duplicate ChoiceCosts: ("
              + duplicateChoiceCosts.stream().map(String::valueOf).collect(Collectors.joining(","))
              + ")");
    }
  }

  public static AdventureResult getCost(final int choice, final int decision) {
    ChoiceCost cost = choiceToChoiceCost.get(choice);
    return cost == null ? null : cost.getCost(decision);
  }

  public static void payCost(final int choice, final int decision) {
    AdventureResult cost = getCost(choice, decision);

    // No cost for this choice/decision
    if (cost == null) {
      return;
    }

    long costCount = cost.getLongCount();

    // No cost for this choice/decision
    if (costCount == 0) {
      return;
    }

    if (cost.isItem()) {
      int inventoryCount = cost.getCount(KoLConstants.inventory);
      // Make sure we have enough in inventory
      if (costCount + inventoryCount < 0) {
        return;
      }

      if (costCount > 0) {
        long multiplier = inventoryCount / costCount;
        cost = cost.getInstance(multiplier * costCount * -1);
      }
    } else if (cost.isMeat()) {
      long purseCount = KoLCharacter.getAvailableMeat();
      // Make sure we have enough in inventory
      if (costCount + purseCount < 0) {
        return;
      }
    } else if (cost.isMP()) {
      long current = KoLCharacter.getCurrentMP();
      // Make sure we have enough mana
      if (costCount + current < 0) {
        return;
      }
    } else {
      return;
    }

    ResultProcessor.processResult(cost);
  }

  public static boolean noRelayChoice(int choice) {
    // Some choices are so clear (or non-standard) that we don't want to mark them up
    // but do want a choice in Mafia GUI
    switch (choice) {
      case 1223: // L.O.V. Entrance
      case 1224: // L.O.V. Equipment Room
      case 1225: // L.O.V. Engine Room
      case 1226: // L.O.V. Emergency Room
      case 1227: // L.O.V. Elbow Room
      case 1228: // L.O.V. Emporium
        return true;

      default:
        return false;
    }
  }

  public static final void decorateChoice(
      final int choice, final StringBuffer buffer, final boolean addComplexFeatures) {
    if (choice >= 48 && choice <= 70) {
      // Add "Go To Goal" button for the Violet Fog
      VioletFogManager.addGoalButton(buffer);
      if (addComplexFeatures) {
        VioletFogManager.addGraph(buffer);
      }
      return;
    }

    if (choice >= 904 && choice <= 913) {
      // Add "Go To Goal" button for the Louvre.
      LouvreManager.addGoalButton(buffer);
      return;
    }

    switch (choice) {
      case 360:
        WumpusManager.decorate(buffer);
        break;
      case 392:
        MemoriesDecorator.decorateElements(choice, buffer);
        break;
      case 443:
        // Chess Puzzle
        RabbitHoleManager.decorateChessPuzzle(buffer);
        break;
      case 485:
        // Fighters of Fighting
        ArcadeRequest.decorateFightersOfFighting(buffer);
        break;
      case 486:
        // Dungeon Fist
        ArcadeRequest.decorateDungeonFist(buffer);
        break;

      case 535:
        // Add "Go To Goal" button for a Safety Shelter Map
        SafetyShelterManager.addRonaldGoalButton(buffer);
        break;

      case 536:
        // Add "Go To Goal" button for a Safety Shelter Map
        SafetyShelterManager.addGrimaceGoalButton(buffer);
        break;

      case 537:
        // Play Porko!
      case 540:
        // Big-Time Generator
        SpaaaceRequest.decoratePorko(buffer);
        break;

      case 546:
        // Add "Go To Goal" button for Interview With You
        VampOutManager.addGoalButton(buffer);
        break;

      case 594:
        // Add "Go To Goal" button for a Lost Room
        LostKeyManager.addGoalButton(buffer);
        break;

      case 665:
        // Add "Solve" button for A Gracious Maze
        GameproManager.addGoalButton(buffer);
        break;

      case 703:
        // Load the options of the dreadscroll with the correct responses
        DreadScrollManager.decorate(buffer);
        break;

      case 850:
        RumpleManager.decorateWorkshop(buffer);
        break;

      case 872:
        ChoiceAdventures.decorateDrawnOnward(buffer);
        break;

      case 989:
        // Highlight valid card
        ChoiceAdventures.decorateParanormalTestLab(buffer);
        break;

      case 1023:
        // Like a Bat Into Hell
        StringUtilities.globalStringReplace(buffer, "Go right back to the fight!", "UNDYING!");
        break;

      case 1024:
        // Like a Bat out of Hell
        StringUtilities.globalStringReplace(buffer, "Return to the fight!", "UNDYING!");
        break;

      case 1094:
        // Back Room SMOOCHing
        ChoiceAdventures.decorateBackRoomSMOOCHing(buffer);
        break;

      case 1278:
        // Madame Zatara’s Relationship Fortune Teller
        ClanFortuneDecorator.decorateQuestion(buffer);
        break;

      case 1331:
        // Daily Loathing Ballot
        ChoiceAdventures.decorateVote(buffer);
        break;
      case 1435:
        // Leading Yourself Right to Them
        ChoiceAdventures.decorateMonsterMap(buffer);
        break;
    }
  }

  private static final Pattern PHOTO_PATTERN =
      Pattern.compile("<select name=\"(.*?)\".*?</select>");

  public static final void decorateDrawnOnward(final StringBuffer buffer) {
    Matcher matcher = PHOTO_PATTERN.matcher(buffer.toString());
    while (matcher.find()) {
      String photo = matcher.group(1);
      String find = matcher.group(0);
      String replace = null;
      switch (photo) {
        case "photo1":
          if (find.contains("2259")) {
            replace =
                StringUtilities.singleStringReplace(
                    find, "<option value=\"2259\">", "<option value=\"2259\" selected>");
          }
          break;
        case "photo2":
          if (find.contains("7264")) {
            replace =
                StringUtilities.singleStringReplace(
                    find, "<option value=\"7264\">", "<option value=\"7264\" selected>");
          }
          break;
        case "photo3":
          if (find.contains("7263")) {
            replace =
                StringUtilities.singleStringReplace(
                    find, "<option value=\"7263\">", "<option value=\"7263\" selected>");
          }
          break;
        case "photo4":
          if (find.contains("7265")) {
            replace =
                StringUtilities.singleStringReplace(
                    find, "<option value=\"7265\">", "<option value=\"7265\" selected>");
          }
          break;
      }

      if (replace != null) {
        StringUtilities.singleStringReplace(buffer, find, replace);
      }
    }
  }

  public static final void decorateParanormalTestLab(final StringBuffer buffer) {
    String pageText = buffer.toString();
    int answer = 0;
    if (pageText.contains("ever-changing constellation")) {
      answer = 1;
    } else if (pageText.contains("card in the circle of light")) {
      answer = 2;
    } else if (pageText.contains("waves a fly away")) {
      answer = 3;
    } else if (pageText.contains("back to square one")) {
      answer = 4;
    } else if (pageText.contains("adds to your anxiety")) {
      answer = 5;
    }
    String find = "espcard" + answer + ".gif";
    String replace = "espcard" + answer + ".gif style=\"border: 2px solid blue;\"";
    if (pageText.contains(find)) {
      StringUtilities.singleStringReplace(buffer, find, replace);
    }
  }

  public static final void decorateBackRoomSMOOCHing(final StringBuffer buffer) {
    int choice = Preferences.getInteger("choiceAdventure1094");
    String find = "smoochdoor" + choice + ".gif";
    String replace = "smoochdoor" + choice + ".gif style=\"border: 2px solid blue;\"";
    if (buffer.toString().contains(find)) {
      StringUtilities.singleStringReplace(buffer, find, replace);
    }
    StringUtilities.globalStringReplace(buffer, "Door #1", "Geve Smimmons");
    StringUtilities.globalStringReplace(buffer, "Door #2", "Raul Stamley");
    StringUtilities.globalStringReplace(buffer, "Door #3", "Pener Crisp");
    StringUtilities.globalStringReplace(buffer, "Door #4", "Deuce Freshly");
  }

  public static final void decorateVote(final StringBuffer buffer) {
    Matcher matcher = ChoiceControl.VOTE_SPEECH_PATTERN.matcher(buffer.toString());

    int count = 1;

    while (matcher.find()) {
      String find = matcher.group(0);
      String monsterName = Preferences.getString("_voteMonster" + count);

      if (monsterName != "") {
        String replace =
            StringUtilities.singleStringReplace(
                find,
                "</blockquote>",
                "<br />(vote for " + monsterName + " tomorrow)</blockquote>");
        StringUtilities.singleStringReplace(buffer, find, replace);
      }

      count++;
    }
  }

  public static final Pattern MAPPED_MONSTER_PATTERN =
      Pattern.compile(
          "(<input type=\"hidden\" name=\"heyscriptswhatsupwinkwink\" value=\"(\\d+)\" />\\s+<input type=\"submit\" class=\"button\" value=\").*?(\" />\\s+</form>)");

  public static final void decorateMonsterMap(final StringBuffer buffer) {
    Matcher matcher = MAPPED_MONSTER_PATTERN.matcher(buffer.toString());

    while (matcher.find()) {
      String find = matcher.group(0);
      Integer monsterId = Integer.parseInt(matcher.group(2));
      String monsterName = MonsterDatabase.getMonsterName(monsterId);

      String replace = matcher.group(1) + monsterName + matcher.group(3);
      StringUtilities.singleStringReplace(buffer, find, replace);
    }
  }

  public static final Spoilers choiceSpoilers(final int choice, final StringBuffer buffer) {
    Spoilers spoilers;

    // See if spoilers are dynamically generated
    spoilers = dynamicChoiceSpoilers(choice);
    if (spoilers != null) {
      return spoilers;
    }

    // Nope. See if it's in the Violet Fog
    spoilers = VioletFogManager.choiceSpoilers(choice);
    if (spoilers != null) {
      return spoilers;
    }

    // Nope. See if it's in the Louvre
    spoilers = LouvreManager.choiceSpoilers(choice);
    if (spoilers != null) {
      return spoilers;
    }

    // Nope. See if it's On a Downtown Train
    spoilers = MonorailManager.choiceSpoilers(choice, buffer);
    if (spoilers != null) {
      return spoilers;
    }

    // Nope. See if it's a Safety Shelter Map
    if (choice == 535 || choice == 536) {
      return null;
    }

    // Nope. See if it's Interview with you.
    if (choice == 546) {
      return null;
    }

    // See if it's A Lost Room
    if (choice == 594) {
      return null;
    }

    // See if this choice is controlled by user option
    ChoiceAdventure choiceAdventure = choiceToChoiceAdventure.get(choice);
    if (choiceAdventure != null) {
      return choiceAdventure.getSpoilers();
    }

    // Nope. See if we know this choice
    ChoiceSpoiler choiceSpoiler = choiceToChoiceSpoiler.get(choice);
    if (choiceSpoiler != null) {
      return choiceSpoiler.getSpoilers();
    }

    // Unknown choice
    return null;
  }

  private static Spoilers dynamicChoiceSpoilers(final int choice) {
    switch (choice) {
      case 5:
        // How Depressing
      case 7:
        // Heart of Very, Very Dark Darkness
        return dynamicChoiceSpoilers(choice, "Spooky Gravy Burrow");

      case 184:
        // Yes, You're a Rock Starrr
      case 185:
        // Arrr You Man Enough?
      case 187:
        // That Explains All The Eyepatches
        return dynamicChoiceSpoilers(choice, "Barrrney's Barrr");

      case 188:
        // The Infiltrationist
        return dynamicChoiceSpoilers(choice, "Orcish Frat House Blueprints");

      case 272:
        // Marketplace Entrance
        return dynamicChoiceSpoilers(choice, "Hobo Marketplace");

      case 298:
        // In the Shade
        return dynamicChoiceSpoilers(choice, "An Octopus's Garden");

      case 304:
        // A Vent Horizon
        return dynamicChoiceSpoilers(choice, "The Marinara Trench");

      case 305:
        // There is Sauce at the Bottom of the Ocean
        return dynamicChoiceSpoilers(choice, "The Marinara Trench");

      case 309:
        // Barback
        return dynamicChoiceSpoilers(choice, "The Dive Bar");

      case 360:
        // Wumpus Hunt
        return dynamicChoiceSpoilers(choice, "The Jungles of Ancient Loathing");

      case 410:
      case 411:
      case 412:
      case 413:
      case 414:
      case 415:
      case 416:
      case 417:
      case 418:
        // The Barracks
        return dynamicChoiceSpoilers(choice, "The Barracks");

      case 442:
        // A Moment of Reflection
        return dynamicChoiceSpoilers(choice, "Rabbit Hole");

      case 522:
        // Welcome to the Footlocker
        return dynamicChoiceSpoilers(choice, "Welcome to the Footlocker");

      case 502:
        // Arboreal Respite
        return dynamicChoiceSpoilers(choice, "Arboreal Respite");

      case 579:
        // Such Great Heights
        return dynamicChoiceSpoilers(choice, "Such Great Heights");

      case 580:
        // The Hidden Heart of the Hidden Temple
        return dynamicChoiceSpoilers(choice, "The Hidden Heart of the Hidden Temple");

      case 581:
        // Such Great Depths
        return dynamicChoiceSpoilers(choice, "Such Great Depths");

      case 582:
        // Fitting In
        return dynamicChoiceSpoilers(choice, "Fitting In");

      case 606:
        // Lost in the Great Overlook Lodge
        return dynamicChoiceSpoilers(choice, "Lost in the Great Overlook Lodge");

      case 611:
        // The Horror...(A-Boo Peak)
        return dynamicChoiceSpoilers(choice, "The Horror...");

      case 636:
      case 637:
      case 638:
      case 639:
        // Old Man psychoses
        return dynamicChoiceSpoilers(choice, "First Mate's Log Entry");

      case 641:
        // Stupid Pipes. (Mystic's psychoses)
        return dynamicChoiceSpoilers(choice, "Stupid Pipes.");

      case 642:
        // You're Freaking Kidding Me (Mystic's psychoses)
        return dynamicChoiceSpoilers(choice, "You're Freaking Kidding Me");

      case 644:
        // Snakes. (Mystic's psychoses)
        return dynamicChoiceSpoilers(choice, "Snakes.");

      case 645:
        // So... Many... Skulls... (Mystic's psychoses)
        return dynamicChoiceSpoilers(choice, "So... Many... Skulls...");

      case 647:
        // A Stupid Dummy. Also, a Straw Man. (Mystic's psychoses)
        return dynamicChoiceSpoilers(choice, "A Stupid Dummy. Also, a Straw Man.");

      case 648:
        // Slings and Arrows (Mystic's psychoses)
        return dynamicChoiceSpoilers(choice, "Slings and Arrows");

      case 650:
        // This Is Your Life. Your Horrible, Horrible Life. (Mystic's psychoses)
        return dynamicChoiceSpoilers(choice, "This Is Your Life. Your Horrible, Horrible Life.");

      case 651:
        // The Wall of Wailing (Mystic's psychoses)
        return dynamicChoiceSpoilers(choice, "The Wall of Wailing");

      case 669:
        // The Fast and the Furry-ous
        return dynamicChoiceSpoilers(choice, "The Fast and the Furry-ous");

      case 670:
        // You Don't Mess Around with Gym
        return dynamicChoiceSpoilers(choice, "You Don't Mess Around with Gym");

      case 678:
        // Yeah, You're for Me, Punk Rock Giant
        return dynamicChoiceSpoilers(choice, "Yeah, You're for Me, Punk Rock Giant");

      case 692:
        // I Wanna Be a Door
        return dynamicChoiceSpoilers(choice, "I Wanna Be a Door");

      case 696:
        // Stick a Fork In It
        return dynamicChoiceSpoilers(choice, "Stick a Fork In It");

      case 697:
        // Sophie's Choice
        return dynamicChoiceSpoilers(choice, "Sophie's Choice");

      case 698:
        // From Bad to Worst
        return dynamicChoiceSpoilers(choice, "From Bad to Worst");

      case 700:
        // Delirium in the Cafeterium
        return dynamicChoiceSpoilers(choice, "Delirium in the Cafeterium");

      case 721:
        // The Cabin in the Dreadsylvanian Woods
        return dynamicChoiceSpoilers(choice, "The Cabin in the Dreadsylvanian Woods");

      case 722:
        // The Kitchen in the Woods
        return dynamicChoiceSpoilers(choice, "The Kitchen in the Woods");

      case 723:
        // What Lies Beneath (the Cabin)
        return dynamicChoiceSpoilers(choice, "What Lies Beneath (the Cabin)");

      case 724:
        // Where it's Attic
        return dynamicChoiceSpoilers(choice, "Where it's Attic");

      case 725:
        // Tallest Tree in the Forest
        return dynamicChoiceSpoilers(choice, "Tallest Tree in the Forest");

      case 726:
        // Top of the Tree, Ma!
        return dynamicChoiceSpoilers(choice, "Top of the Tree, Ma!");

      case 727:
        // All Along the Watchtower
        return dynamicChoiceSpoilers(choice, "All Along the Watchtower");

      case 728:
        // Treebasing
        return dynamicChoiceSpoilers(choice, "Treebasing");

      case 729:
        // Below the Roots
        return dynamicChoiceSpoilers(choice, "Below the Roots");

      case 730:
        // Hot Coals
        return dynamicChoiceSpoilers(choice, "Hot Coals");

      case 731:
        // The Heart of the Matter
        return dynamicChoiceSpoilers(choice, "The Heart of the Matter");

      case 732:
        // Once Midden, Twice Shy
        return dynamicChoiceSpoilers(choice, "Once Midden, Twice Shy");

      case 733:
        // Dreadsylvanian Village Square
        return dynamicChoiceSpoilers(choice, "Dreadsylvanian Village Square");

      case 734:
        // Fright School
        return dynamicChoiceSpoilers(choice, "Fright School");

      case 735:
        // Smith, Black as Night
        return dynamicChoiceSpoilers(choice, "Smith, Black as Night");

      case 736:
        // Gallows
        return dynamicChoiceSpoilers(choice, "Gallows");

      case 737:
        // The Even More Dreadful Part of Town
        return dynamicChoiceSpoilers(choice, "The Even More Dreadful Part of Town");

      case 738:
        // A Dreadful Smell
        return dynamicChoiceSpoilers(choice, "A Dreadful Smell");

      case 739:
        // The Tinker's. Damn.
        return dynamicChoiceSpoilers(choice, "The Tinker's. Damn.");

      case 740:
        // Eight, Nine, Tenement
        return dynamicChoiceSpoilers(choice, "Eight, Nine, Tenement");

      case 741:
        // The Old Duke's Estate
        return dynamicChoiceSpoilers(choice, "The Old Duke's Estate");

      case 742:
        // The Plot Thickens
        return dynamicChoiceSpoilers(choice, "The Plot Thickens");

      case 743:
        // No Quarter
        return dynamicChoiceSpoilers(choice, "No Quarter");

      case 744:
        // The Master Suite -- Sweet!
        return dynamicChoiceSpoilers(choice, "The Master Suite -- Sweet!");

      case 745:
        // This Hall is Really Great
        return dynamicChoiceSpoilers(choice, "This Hall is Really Great");

      case 746:
        // The Belle of the Ballroom
        return dynamicChoiceSpoilers(choice, "The Belle of the Ballroom");

      case 747:
        // Cold Storage
        return dynamicChoiceSpoilers(choice, "Cold Storage");

      case 748:
        // Dining In (the Castle)
        return dynamicChoiceSpoilers(choice, "Dining In (the Castle)");

      case 749:
        // Tower Most Tall
        return dynamicChoiceSpoilers(choice, "Tower Most Tall");

      case 750:
        // Working in the Lab, Late One Night
        return dynamicChoiceSpoilers(choice, "Working in the Lab, Late One Night");

      case 751:
        // Among the Quaint and Curious Tomes.
        return dynamicChoiceSpoilers(choice, "Among the Quaint and Curious Tomes.");

      case 752:
        // In The Boudoir
        return dynamicChoiceSpoilers(choice, "In The Boudoir");

      case 753:
        // The Dreadsylvanian Dungeon
        return dynamicChoiceSpoilers(choice, "The Dreadsylvanian Dungeon");

      case 754:
        // Live from Dungeon Prison
        return dynamicChoiceSpoilers(choice, "Live from Dungeon Prison");

      case 755:
        // The Hot Bowels
        return dynamicChoiceSpoilers(choice, "The Hot Bowels");

      case 756:
        // Among the Fungus
        return dynamicChoiceSpoilers(choice, "Among the Fungus");

      case 758:
        // End of the Path
        return dynamicChoiceSpoilers(choice, "End of the Path");

      case 759:
        // You're About to Fight City Hall
        return dynamicChoiceSpoilers(choice, "You're About to Fight City Hall");

      case 760:
        // Holding Court
        return dynamicChoiceSpoilers(choice, "Holding Court");

        // Choice 761 is Staring Upwards...
        // Choice 762 is Try New Extra-Strength Anvil
        // Choice 764 is The Machine
        // Choice 765 is Hello Gallows

      case 772:
        // Saved by the Bell
        return dynamicChoiceSpoilers(choice, "Saved by the Bell");

      case 780:
        // Action Elevator
        return dynamicChoiceSpoilers(choice, "Action Elevator");

      case 781:
        // Earthbound and Down
        return dynamicChoiceSpoilers(choice, "Earthbound and Down");

      case 783:
        // Water You Dune
        return dynamicChoiceSpoilers(choice, "Water You Dune");

      case 784:
        // You, M. D.
        return dynamicChoiceSpoilers(choice, "You, M. D.");

      case 785:
        // Air Apparent
        return dynamicChoiceSpoilers(choice, "Air Apparent");

      case 786:
        // Working Holiday
        return dynamicChoiceSpoilers(choice, "Working Holiday");

      case 787:
        // Fire when Ready
        return dynamicChoiceSpoilers(choice, "Fire when Ready");

      case 788:
        // Life is Like a Cherry of Bowls
        return dynamicChoiceSpoilers(choice, "Life is Like a Cherry of Bowls");

      case 789:
        // Where Does The Lone Ranger Take His Garbagester?
        return dynamicChoiceSpoilers(choice, "Where Does The Lone Ranger Take His Garbagester?");

      case 791:
        // Legend of the Temple in the Hidden City
        return dynamicChoiceSpoilers(choice, "Legend of the Temple in the Hidden City");

      case 801:
        // A Reanimated Conversation
        return dynamicChoiceSpoilers(choice, "A Reanimated Conversation");

      case 918:
        // Yachtzee!
        return dynamicChoiceSpoilers(choice, "Yachtzee!");

      case 988:
        // The Containment Unit
        return dynamicChoiceSpoilers(choice, "The Containment Unit");

      case 1049:
        // Tomb of the Unknown Your Class Here
        return dynamicChoiceSpoilers(choice, "Tomb of the Unknown Your Class Here");

      case 1411:
        // The Hall in the Hall
        return dynamicChoiceSpoilers(choice, "The Hall in the Hall");

      case 1489:
        // Slagging Off
        return dynamicChoiceSpoilers(choice, "Slagging Off");

      case 1499:
        // A Labyrinth of Shadows
        return dynamicChoiceSpoilers(choice, "A Labyrinth of Shadows");
    }

    return null;
  }

  private static Spoilers dynamicChoiceSpoilers(final int choice, final String name) {
    return new Spoilers(choice, name, ChoiceAdventures.dynamicChoiceOptions(choice));
  }

  private static final AdventureResult BINDER_CLIP = ItemPool.get(ItemPool.BINDER_CLIP, 1);
  private static final AdventureResult MCCLUSKY_FILE = ItemPool.get(ItemPool.MCCLUSKY_FILE, 1);
  private static final AdventureResult STONE_TRIANGLE = ItemPool.get(ItemPool.STONE_TRIANGLE, 1);

  private static final AdventureResult CURSE1_EFFECT = EffectPool.get(EffectPool.ONCE_CURSED);
  private static final AdventureResult CURSE2_EFFECT = EffectPool.get(EffectPool.TWICE_CURSED);
  private static final AdventureResult CURSE3_EFFECT = EffectPool.get(EffectPool.THRICE_CURSED);
  private static final AdventureResult GREASER_EFFECT = EffectPool.get(EffectPool.GREASER_LIGHTNIN);
  private static final AdventureResult JOCK_EFFECT =
      EffectPool.get(EffectPool.JAMMING_WITH_THE_JOCKS);
  private static final AdventureResult NERD_EFFECT = EffectPool.get(EffectPool.NERD_IS_THE_WORD);

  // Dreadsylvania items and effects
  private static final AdventureResult AUDITORS_BADGE = ItemPool.get(ItemPool.AUDITORS_BADGE, 1);
  private static final AdventureResult BLOODY_KIWITINI = ItemPool.get(ItemPool.BLOODY_KIWITINI, 1);
  private static final AdventureResult GHOST_SHAWL = ItemPool.get(ItemPool.GHOST_SHAWL, 1);
  private static final AdventureResult HELPS_YOU_SLEEP = ItemPool.get(ItemPool.HELPS_YOU_SLEEP, 1);
  private static final AdventureResult MAKESHIFT_TURBAN =
      ItemPool.get(ItemPool.MAKESHIFT_TURBAN, 1);
  private static final AdventureResult MOON_AMBER_NECKLACE =
      ItemPool.get(ItemPool.MOON_AMBER_NECKLACE, 1);
  private static final AdventureResult SHEPHERDS_PIE = ItemPool.get(ItemPool.SHEPHERDS_PIE, 1);
  private static final AdventureResult SLEEP_MASK = ItemPool.get(ItemPool.SLEEP_MASK, 1);
  private static final AdventureResult WEEDY_SKIRT = ItemPool.get(ItemPool.WEEDY_SKIRT, 1);

  private static final AdventureResult KIWITINI_EFFECT =
      EffectPool.get(EffectPool.FIRST_BLOOD_KIWI);
  private static final AdventureResult PIE_EFFECT = EffectPool.get(EffectPool.SHEPHERDS_BREATH);
  private static final AdventureResult TEMPORARY_BLINDNESS =
      EffectPool.get(EffectPool.TEMPORARY_BLINDNESS);

  private static final ChoiceOption FLICKERING_PIXEL = new ChoiceOption("flickering pixel");

  public static final ChoiceOption[] dynamicChoiceOptions(final int choice) {
    ChoiceOption[] result;
    switch (choice) {
      case 5:
        // Heart of Very, Very Dark Darkness
        result = new ChoiceOption[2];

        boolean rock = InventoryManager.getCount(ItemPool.INEXPLICABLY_GLOWING_ROCK) >= 1;

        result[0] =
            new ChoiceOption(
                "You " + (rock ? "" : "DON'T ") + " have an inexplicably glowing rock");
        result[1] = SKIP_ADVENTURE;

        return result;

      case 7:
        // How Depressing
        result = new ChoiceOption[2];

        boolean glove = KoLCharacter.hasEquipped(ItemPool.get(ItemPool.SPOOKY_GLOVE, 1));

        result[0] = new ChoiceOption("spooky glove " + (glove ? "" : "NOT ") + "equipped");
        result[1] = SKIP_ADVENTURE;

        return result;

      case 184:
        // That Explains All The Eyepatches
        result = new ChoiceOption[6];

        // The choices are based on character class.
        // Mus: combat, shot of rotgut (2948), drunkenness
        // Mys: drunkenness, shot of rotgut (2948), shot of rotgut (2948)
        // Mox: combat, drunkenness, shot of rotgut (2948)

        result[0] =
            new ChoiceOption(
                KoLCharacter.isMysticalityClass()
                    ? "3 drunk and stats (varies by class)"
                    : "enter combat (varies by class)");
        result[1] =
            new ChoiceOption(
                KoLCharacter.isMoxieClass()
                    ? "3 drunk and stats (varies by class)"
                    : "shot of rotgut (varies by class)",
                "shot of rotgut");
        result[2] =
            new ChoiceOption(
                KoLCharacter.isMuscleClass()
                    ? "3 drunk and stats (varies by class)"
                    : "shot of rotgut (varies by class)",
                "shot of rotgut");
        result[3] = new ChoiceOption("always 3 drunk & stats");
        result[4] = new ChoiceOption("always shot of rotgut");
        result[5] = new ChoiceOption("combat (or rotgut if Myst class)");
        return result;

      case 185:
        // Yes, You're a Rock Starrr
        result = new ChoiceOption[3];

        int drunk = KoLCharacter.getInebriety();

        // 0 drunk: base booze, mixed booze, fight
        // More than 0 drunk: base booze, mixed booze, stats

        result[0] = new ChoiceOption("base booze");
        result[1] = new ChoiceOption("mixed booze");
        result[2] = new ChoiceOption(drunk == 0 ? "combat" : "stats");
        return result;

      case 187:
        // Arrr You Man Enough?

        result = new ChoiceOption[2];
        float odds = BeerPongRequest.pirateInsultOdds() * 100.0f;

        result[0] =
            new ChoiceOption(KoLConstants.FLOAT_FORMAT.format(odds) + "% chance of winning");
        result[1] = new ChoiceOption(odds == 100.0f ? "Oh come on. Do it!" : "Try later");
        return result;

      case 188:
        // The Infiltrationist
        result = new ChoiceOption[3];

        // Attempt a frontal assault
        boolean ok1 = EquipmentManager.isWearingOutfit(OutfitPool.FRAT_OUTFIT);
        result[0] = new ChoiceOption("Frat Boy Ensemble (" + (ok1 ? "" : "NOT ") + "equipped)");

        // Go in through the side door
        boolean ok2a = KoLCharacter.hasEquipped(ItemPool.get(ItemPool.MULLET_WIG, 1));
        boolean ok2b = InventoryManager.getCount(ItemPool.BRIEFCASE) >= 1;
        result[1] =
            new ChoiceOption(
                "mullet wig ("
                    + (ok2a ? "" : "NOT ")
                    + "equipped) + briefcase ("
                    + (ok2b ? "OK)" : "0 in inventory)"));

        // Catburgle
        boolean ok3a = KoLCharacter.hasEquipped(ItemPool.get(ItemPool.FRILLY_SKIRT, 1));
        int wings = InventoryManager.getCount(ItemPool.HOT_WING);
        result[2] =
            new ChoiceOption(
                "frilly skirt ("
                    + (ok3a ? "" : "NOT ")
                    + "equipped) + 3 hot wings ("
                    + wings
                    + " in inventory)");

        return result;

      case 191:
        // Chatterboxing
        result = new ChoiceOption[4];

        int trinks = InventoryManager.getCount(ItemPool.VALUABLE_TRINKET);
        result[0] = new ChoiceOption("moxie substats");
        result[1] =
            new ChoiceOption(
                trinks == 0
                    ? "lose hp (no valuable trinkets)"
                    : "use valuable trinket to banish (" + trinks + " in inventory)");
        result[2] = new ChoiceOption("muscle substats");
        result[3] = new ChoiceOption("mysticality substats");

        return result;

      case 272:
        // Marketplace Entrance
        result = new ChoiceOption[2];

        int nickels = InventoryManager.getCount(ItemPool.HOBO_NICKEL);
        boolean binder = KoLCharacter.hasEquipped(ItemPool.get(ItemPool.HOBO_CODE_BINDER, 1));

        result[0] =
            new ChoiceOption(
                nickels + " nickels, " + (binder ? "" : "NO ") + " hobo code binder equipped");
        result[1] = SKIP_ADVENTURE;

        return result;

      case 298:
        // In the Shade
        result = new ChoiceOption[2];

        int seeds = InventoryManager.getCount(ItemPool.SEED_PACKET);
        int slime = InventoryManager.getCount(ItemPool.GREEN_SLIME);

        result[0] = new ChoiceOption(seeds + " seed packets, " + slime + " globs of green slime");
        result[1] = SKIP_ADVENTURE;

        return result;

      case 304:
        // A Vent Horizon
        result = new ChoiceOption[2];

        int summons = 3 - Preferences.getInteger("tempuraSummons");

        result[0] = new ChoiceOption(summons + " summons left today");
        result[1] = SKIP_ADVENTURE;

        return result;

      case 305:
        // There is Sauce at the Bottom of the Ocean
        result = new ChoiceOption[2];

        int globes = InventoryManager.getCount(ItemPool.MERKIN_PRESSUREGLOBE);

        result[0] = new ChoiceOption(globes + " Mer-kin pressureglobes");
        result[1] = SKIP_ADVENTURE;

        return result;

      case 309:
        // Barback
        result = new ChoiceOption[2];

        int seaodes = 3 - Preferences.getInteger("seaodesFound");

        result[0] = new ChoiceOption(seaodes + " more seodes available today");
        result[1] = SKIP_ADVENTURE;

        return result;

      case 360:
        // Wumpus Hunt
        return WumpusManager.dynamicChoiceOptions(ChoiceManager.lastResponseText);

      case 410:
      case 411:
      case 412:
      case 413:
      case 414:
      case 415:
      case 416:
      case 417:
      case 418:
        // The Barracks
        return HaciendaManager.getSpoilers(choice);

      case 442:
        // A Moment of Reflection
        result = new ChoiceOption[6];
        int count = 0;
        if (InventoryManager.getCount(ItemPool.BEAUTIFUL_SOUP) > 0) {
          ++count;
        }
        if (InventoryManager.getCount(ItemPool.LOBSTER_QUA_GRILL) > 0) {
          ++count;
        }
        if (InventoryManager.getCount(ItemPool.MISSING_WINE) > 0) {
          ++count;
        }
        if (InventoryManager.getCount(ItemPool.WALRUS_ICE_CREAM) > 0) {
          ++count;
        }
        if (InventoryManager.getCount(ItemPool.HUMPTY_DUMPLINGS) > 0) {
          ++count;
        }
        result[0] = new ChoiceOption("Seal Clubber/Pastamancer item, or yellow matter custard");
        result[1] = new ChoiceOption("Sauceror/Accordion Thief item, or delicious comfit?");
        result[2] = new ChoiceOption("Disco Bandit/Turtle Tamer item, or fight croqueteer");
        result[3] =
            new ChoiceOption(
                "you have " + count + "/5 of the items needed for an ittah bittah hookah");
        result[4] = new ChoiceOption("get a chess cookie");
        result[5] = SKIP_ADVENTURE;
        return result;

      case 502:
        // Arboreal Respite
        result = new ChoiceOption[3];

        // meet the vampire hunter, trade bar skins or gain a spooky sapling
        int stakes = InventoryManager.getCount(ItemPool.WOODEN_STAKES);
        int hearts = InventoryManager.getCount(ItemPool.VAMPIRE_HEART);
        String hunterAction =
            (stakes > 0 ? "and get wooden stakes" : "and trade " + hearts + " hearts");

        int barskins = InventoryManager.getCount(ItemPool.BAR_SKIN);
        int saplings = InventoryManager.getCount(ItemPool.SPOOKY_SAPLING);

        result[0] =
            new ChoiceOption(
                "gain some meat, meet the vampire hunter "
                    + hunterAction
                    + ", sell bar skins ("
                    + barskins
                    + ") or buy a spooky sapling ("
                    + saplings
                    + ")");

        // gain mosquito larva, gain quest coin or gain a vampire heart
        boolean haveMap = InventoryManager.getCount(ItemPool.SPOOKY_MAP) > 0;
        boolean haveCoin = InventoryManager.getCount(ItemPool.TREE_HOLED_COIN) > 0;
        boolean getCoin = (!haveCoin && !haveMap && !KoLCharacter.getTempleUnlocked());
        String coinAction = (getCoin ? "gain quest coin" : "skip adventure");

        result[1] =
            new ChoiceOption(
                "gain mosquito larva or spooky mushrooms, "
                    + coinAction
                    + ", get stats or fight a vampire");

        // gain a starter item, gain Spooky-Gro fertilizer, gain spooky temple map or gain fake bood
        int fertilizer = InventoryManager.getCount(ItemPool.SPOOKY_FERTILIZER);
        String mapAction = (haveCoin ? ", gain spooky temple map" : "");

        result[2] =
            new ChoiceOption(
                "gain a starter item, gain Spooky-Gro fertilizer ("
                    + fertilizer
                    + ")"
                    + mapAction
                    + ", gain fake blood");

        return result;

      case 522:
        // Welcome to the Footlocker
        result = new ChoiceOption[2];

        boolean havePolearm =
            (InventoryManager.getCount(ItemPool.KNOB_GOBLIN_POLEARM) > 0
                || InventoryManager.getEquippedCount(ItemPool.KNOB_GOBLIN_POLEARM) > 0);
        boolean havePants =
            (InventoryManager.getCount(ItemPool.KNOB_GOBLIN_PANTS) > 0
                || InventoryManager.getEquippedCount(ItemPool.KNOB_GOBLIN_PANTS) > 0);
        boolean haveHelm =
            (InventoryManager.getCount(ItemPool.KNOB_GOBLIN_HELM) > 0
                || InventoryManager.getEquippedCount(ItemPool.KNOB_GOBLIN_HELM) > 0);

        result[0] =
            !havePolearm
                ? new ChoiceOption("knob goblin elite polearm", "knob goblin elite polearm")
                : !havePants
                    ? new ChoiceOption("knob goblin elite pants", "knob goblin elite pants")
                    : !haveHelm
                        ? new ChoiceOption("knob goblin elite helm", "knob goblin elite helm")
                        : new ChoiceOption("knob jelly donut", "knob jelly donut");
        result[1] = SKIP_ADVENTURE;
        return result;

      case 579:
        // Such Great Heights
        result = new ChoiceOption[3];

        boolean haveNostril = (InventoryManager.getCount(ItemPool.NOSTRIL_OF_THE_SERPENT) > 0);
        boolean gainNostril =
            (!haveNostril
                && Preferences.getInteger("lastTempleButtonsUnlock")
                    != KoLCharacter.getAscensions());
        boolean templeAdvs =
            (Preferences.getInteger("lastTempleAdventures") == KoLCharacter.getAscensions());

        result[0] = new ChoiceOption("mysticality substats");
        result[1] =
            (gainNostril ? new ChoiceOption("gain the Nostril of the Serpent") : SKIP_ADVENTURE);
        result[2] = (templeAdvs ? SKIP_ADVENTURE : new ChoiceOption("gain 3 adventures"));
        return result;

      case 580:
        // The Hidden Heart of the Hidden Temple
        result = new ChoiceOption[3];

        haveNostril = (InventoryManager.getCount(ItemPool.NOSTRIL_OF_THE_SERPENT) > 0);
        boolean buttonsUnconfused =
            (Preferences.getInteger("lastTempleButtonsUnlock") == KoLCharacter.getAscensions());

        if (ChoiceManager.lastResponseText.contains("door_stone.gif")) {
          result[0] = new ChoiceOption("muscle substats");
          result[1] =
              new ChoiceOption(
                  buttonsUnconfused || haveNostril
                      ? "choose Hidden Heart adventure"
                      : "randomise Hidden Heart adventure");
          result[2] = new ChoiceOption("moxie substats and 5 turns of Somewhat poisoned");
        } else if (ChoiceManager.lastResponseText.contains("door_sun.gif")) {
          result[0] = new ChoiceOption("gain ancient calendar fragment");
          result[1] =
              new ChoiceOption(
                  buttonsUnconfused || haveNostril
                      ? "choose Hidden Heart adventure"
                      : "randomise Hidden Heart adventure");
          result[2] = new ChoiceOption("moxie substats and 5 turns of Somewhat poisoned");
        } else if (ChoiceManager.lastResponseText.contains("door_gargoyle.gif")) {
          result[0] = new ChoiceOption("gain mana");
          result[1] =
              new ChoiceOption(
                  buttonsUnconfused || haveNostril
                      ? "choose Hidden Heart adventure"
                      : "randomise Hidden Heart adventure");
          result[2] = new ChoiceOption("moxie substats and 5 turns of Somewhat poisoned");
        } else if (ChoiceManager.lastResponseText.contains("door_pikachu.gif")) {
          result[0] = new ChoiceOption("unlock Hidden City");
          result[1] =
              new ChoiceOption(
                  buttonsUnconfused || haveNostril
                      ? "choose Hidden Heart adventure"
                      : "randomise Hidden Heart adventure");
          result[2] = new ChoiceOption("moxie substats and 5 turns of Somewhat poisoned");
        }

        return result;

      case 581:
        // Such Great Depths
        result = new ChoiceOption[3];

        int fungus = InventoryManager.getCount(ItemPool.GLOWING_FUNGUS);

        result[0] = new ChoiceOption("gain a glowing fungus (" + fungus + ")");
        result[1] =
            Preferences.getBoolean("_templeHiddenPower")
                ? SKIP_ADVENTURE
                : new ChoiceOption("5 advs of +15 mus/mys/mox");
        result[2] = new ChoiceOption("fight clan of cave bars");
        return result;

      case 582:
        // Fitting In
        result = new ChoiceOption[3];

        // mysticality substats, gain the Nostril of the Serpent or gain 3 adventures
        haveNostril = (InventoryManager.getCount(ItemPool.NOSTRIL_OF_THE_SERPENT) > 0);
        gainNostril =
            (!haveNostril
                && Preferences.getInteger("lastTempleButtonsUnlock")
                    != KoLCharacter.getAscensions());
        String nostrilAction = (gainNostril ? "gain the Nostril of the Serpent" : "skip adventure");

        templeAdvs =
            (Preferences.getInteger("lastTempleAdventures") == KoLCharacter.getAscensions());
        String advAction = (templeAdvs ? "skip adventure" : "gain 3 adventures");

        result[0] = new ChoiceOption("mysticality substats, " + nostrilAction + " or " + advAction);

        // Hidden Heart of the Hidden Temple
        result[1] = new ChoiceOption("Hidden Heart of the Hidden Temple");

        // gain glowing fungus, gain Hidden Power or fight a clan of cave bars
        String powerAction =
            (Preferences.getBoolean("_templeHiddenPower") ? "skip adventure" : "Hidden Power");

        result[2] =
            new ChoiceOption(
                "gain a glowing fungus, " + powerAction + " or fight a clan of cave bars");

        return result;

      case 606:
        // Lost in the Great Overlook Lodge
        result = new ChoiceOption[6];

        result[0] =
            new ChoiceOption(
                "need +4 stench resist, have "
                    + KoLCharacter.getElementalResistanceLevels(Element.STENCH));

        // annoyingly, the item drop check does not take into account fairy (or other sidekick)
        // bonus.
        // This is just a one-off implementation, but should be standardized somewhere in Modifiers
        // if kol adds more things like this.
        double bonus = 0;
        // Check for familiars
        if (!KoLCharacter.getFamiliar().equals(FamiliarData.NO_FAMILIAR)) {
          bonus =
              ModifierDatabase.getNumericModifier(
                  KoLCharacter.getFamiliar(), DoubleModifier.ITEMDROP);
          bonus +=
              ModifierDatabase.getNumericModifier(
                  KoLCharacter.getFamiliar(), DoubleModifier.FOODDROP);
        }
        // Check for Clancy
        else if (KoLCharacter.getCurrentInstrument() != null
            && KoLCharacter.getCurrentInstrument().equals(CharPaneRequest.LUTE)) {
          int weight = 5 * KoLCharacter.getMinstrelLevel();
          bonus = Math.sqrt(55 * weight) + weight - 3;
        }
        // Check for Eggman
        else if (KoLCharacter.getCompanion() == Companion.EGGMAN) {
          bonus = KoLCharacter.hasSkill(SkillPool.WORKING_LUNCH) ? 75 : 50;
        }
        // Check for Cat Servant
        else if (KoLCharacter.isEd()) {
          EdServantData servant = EdServantData.currentServant();
          if (servant != null && servant.getId() == 1) {
            int level = servant.getLevel();
            if (level >= 7) {
              bonus = Math.sqrt(55 * level) + level - 3;
            }
          }
        }
        // Check for Throne
        FamiliarData throned = KoLCharacter.getEnthroned();
        if (!throned.equals(FamiliarData.NO_FAMILIAR)) {
          bonus +=
              ModifierDatabase.getNumericModifier(
                  ModifierType.THRONE, throned.getRace(), DoubleModifier.ITEMDROP);
        }
        // Check for Bjorn
        FamiliarData bjorned = KoLCharacter.getBjorned();
        if (!bjorned.equals(FamiliarData.NO_FAMILIAR)) {
          bonus +=
              ModifierDatabase.getNumericModifier(
                  ModifierType.THRONE, bjorned.getRace(), DoubleModifier.ITEMDROP);
        }
        // Check for Florist
        if (FloristRequest.haveFlorist()) {
          List<Florist> plants = FloristRequest.getPlants("Twin Peak");
          if (plants != null) {
            for (Florist plant : plants) {
              bonus +=
                  ModifierDatabase.getNumericModifier(
                      ModifierType.FLORIST, plant.toString(), DoubleModifier.ITEMDROP);
            }
          }
        }
        result[1] =
            new ChoiceOption(
                "need +50% item drop, have "
                    + Math.round(
                        KoLCharacter.getItemDropPercentAdjustment()
                            + KoLCharacter.currentNumericModifier(DoubleModifier.FOODDROP)
                            - bonus)
                    + "%");
        result[2] = new ChoiceOption("need jar of oil", "jar of oil");
        result[3] =
            new ChoiceOption(
                "need +40% init, have " + KoLCharacter.getInitiativeAdjustment() + "%");
        result[4] = null; // why is there a missing button 5?
        result[5] = new ChoiceOption("flee");

        return result;

      case 611:
        // The Horror... (A-Boo Peak)
        result = new ChoiceOption[2];
        result[0] = booPeakDamage();
        result[1] = new ChoiceOption("Flee");
        return result;

      case 636:
      case 637:
      case 638:
      case 639:
        // Old Man psychosis choice adventures are randomized and may not include all elements.
        return oldManPsychosisSpoilers();

      case 641:
        // Stupid Pipes. (Mystic's psychoses)
        result = new ChoiceOption[3];
        {
          StringBuilder buffer = new StringBuilder();
          int resistance = KoLCharacter.getElementalResistanceLevels(Element.HOT);
          int damage = (int) (2.50 * (100.0 - KoLCharacter.elementalResistanceByLevel(resistance)));
          long hp = KoLCharacter.getCurrentHP();
          buffer.append("take ");
          buffer.append(damage);
          buffer.append(" hot damage, current HP = ");
          buffer.append(hp);
          buffer.append(", current hot resistance = ");
          buffer.append(resistance);
          result[0] = new ChoiceOption(buffer.toString());
        }
        result[1] = FLICKERING_PIXEL;
        result[2] = SKIP_ADVENTURE;
        return result;

      case 642:
        // You're Freaking Kidding Me (Mystic's psychoses)
        result = new ChoiceOption[3];
        {
          String buffer =
              "50 buffed Muscle/Mysticality/Moxie required, have "
                  + KoLCharacter.getAdjustedMuscle()
                  + "/"
                  + KoLCharacter.getAdjustedMysticality()
                  + "/"
                  + KoLCharacter.getAdjustedMoxie();
          result[0] = new ChoiceOption(buffer);
        }
        result[1] = FLICKERING_PIXEL;
        result[2] = SKIP_ADVENTURE;
        return result;

      case 644:
        // Snakes. (Mystic's psychoses)
        result = new ChoiceOption[3];
        {
          String buffer = "50 buffed Moxie required, have " + KoLCharacter.getAdjustedMoxie();
          result[0] = new ChoiceOption(buffer);
        }
        result[1] = FLICKERING_PIXEL;
        result[2] = SKIP_ADVENTURE;
        return result;

      case 645:
        // So... Many... Skulls... (Mystic's psychoses)
        result = new ChoiceOption[3];
        {
          StringBuilder buffer = new StringBuilder();
          int resistance = KoLCharacter.getElementalResistanceLevels(Element.SPOOKY);
          int damage = (int) (2.50 * (100.0 - KoLCharacter.elementalResistanceByLevel(resistance)));
          long hp = KoLCharacter.getCurrentHP();
          buffer.append("take ");
          buffer.append(damage);
          buffer.append(" spooky damage, current HP = ");
          buffer.append(hp);
          buffer.append(", current spooky resistance = ");
          buffer.append(resistance);
          result[0] = new ChoiceOption(buffer.toString());
        }
        result[1] = FLICKERING_PIXEL;
        result[2] = SKIP_ADVENTURE;
        return result;

      case 647:
        // A Stupid Dummy. Also, a Straw Man. (Mystic's psychoses)
        result = new ChoiceOption[3];
        {
          StringBuilder buffer = new StringBuilder();
          String current = String.valueOf(KoLCharacter.currentBonusDamage());
          buffer.append("100 weapon damage required");
          result[0] = new ChoiceOption(buffer.toString());
        }
        result[1] = FLICKERING_PIXEL;
        result[2] = SKIP_ADVENTURE;
        return result;

      case 648:
        // Slings and Arrows (Mystic's psychoses)
        result = new ChoiceOption[3];
        {
          String buffer = "101 HP required, have " + KoLCharacter.getCurrentHP();
          result[0] = new ChoiceOption(buffer);
        }
        result[1] = FLICKERING_PIXEL;
        result[2] = SKIP_ADVENTURE;
        return result;

      case 650:
        // This Is Your Life. Your Horrible, Horrible Life. (Mystic's psychoses)
        result = new ChoiceOption[3];
        {
          String buffer = "101 MP required, have " + KoLCharacter.getCurrentMP();
          result[0] = new ChoiceOption(buffer);
        }
        result[1] = FLICKERING_PIXEL;
        result[2] = SKIP_ADVENTURE;
        return result;

      case 651:
        // The Wall of Wailing (Mystic's psychoses)
        result = new ChoiceOption[3];
        {
          String buffer =
              "10 prismatic damage required, have " + KoLCharacter.currentPrismaticDamage();
          result[0] = new ChoiceOption(buffer);
        }
        result[1] = FLICKERING_PIXEL;
        result[2] = SKIP_ADVENTURE;
        return result;

      case 669:
        // The Fast and the Furry-ous
        result = new ChoiceOption[4];
        result[0] =
            new ChoiceOption(
                KoLCharacter.hasEquipped(ItemPool.get(ItemPool.TITANIUM_UMBRELLA))
                    ? "open Ground Floor (titanium umbrella equipped)"
                    : KoLCharacter.hasEquipped(ItemPool.get(ItemPool.UNBREAKABLE_UMBRELLA))
                        ? "open Ground Floor (unbreakable umbrella equipped)"
                        : "Neckbeard Choice (titanium/unbreakable umbrella not equipped)");
        result[1] = new ChoiceOption("200 Moxie substats");
        result[2] = new ChoiceOption("");
        result[3] = new ChoiceOption("skip adventure and guarantees this adventure will reoccur");
        return result;

      case 670:
        // You Don't Mess Around with Gym
        result = new ChoiceOption[5];
        result[0] = new ChoiceOption("massive dumbbell, then skip adventure");
        result[1] = new ChoiceOption("200 Muscle substats");
        result[2] = new ChoiceOption("pec oil, giant jar of protein powder, Squat-Thrust Magazine");
        result[3] =
            new ChoiceOption(
                KoLCharacter.hasEquipped(ItemPool.get(ItemPool.EXTREME_AMULET, 1))
                    ? "open Ground Floor (amulet equipped)"
                    : "skip adventure (amulet not equipped)");
        result[4] = new ChoiceOption("skip adventure and guarantees this adventure will reoccur");
        return result;

      case 678:
        // Yeah, You're for Me, Punk Rock Giant
        result = new ChoiceOption[4];
        result[0] =
            new ChoiceOption(
                KoLCharacter.hasEquipped(ItemPool.get(ItemPool.MOHAWK_WIG, 1))
                    ? "Finish quest (mohawk wig equipped)"
                    : "Fight Punk Rock Giant (mohawk wig not equipped)");
        result[1] = new ChoiceOption("500 meat");
        result[2] = new ChoiceOption("Steampunk Choice");
        result[3] = new ChoiceOption("Raver Choice");
        return result;

      case 692:
        // I Wanna Be a Door
        result = new ChoiceOption[9];
        result[0] = new ChoiceOption("suffer trap effects");
        result[1] = new ChoiceOption("unlock door with key, no turn spent");
        result[2] = new ChoiceOption("pick lock with lockpicks, no turn spent");
        result[3] =
            new ChoiceOption(
                KoLCharacter.getAdjustedMuscle() >= 30
                    ? "bypass trap with muscle"
                    : "suffer trap effects");
        result[4] =
            new ChoiceOption(
                KoLCharacter.getAdjustedMysticality() >= 30
                    ? "bypass trap with mysticality"
                    : "suffer trap effects");
        result[5] =
            new ChoiceOption(
                KoLCharacter.getAdjustedMoxie() >= 30
                    ? "bypass trap with moxie"
                    : "suffer trap effects");
        result[6] = new ChoiceOption("open door with card, no turn spent");
        result[7] = new ChoiceOption("leave, no turn spent");
        return result;

      case 696:
        // Stick a Fork In It
        result = new ChoiceOption[2];
        result[0] =
            new ChoiceOption(
                Preferences.getBoolean("maraisDarkUnlock")
                    ? "Dark and Spooky Swamp already unlocked"
                    : "unlock Dark and Spooky Swamp");
        result[1] =
            new ChoiceOption(
                Preferences.getBoolean("maraisWildlifeUnlock")
                    ? "The Wildlife Sanctuarrrrrgh already unlocked"
                    : "unlock The Wildlife Sanctuarrrrrgh");
        return result;

      case 697:
        // Sophie's Choice
        result = new ChoiceOption[2];
        result[0] =
            new ChoiceOption(
                Preferences.getBoolean("maraisCorpseUnlock")
                    ? "The Corpse Bog already unlocked"
                    : "unlock The Corpse Bog");
        result[1] =
            new ChoiceOption(
                Preferences.getBoolean("maraisWizardUnlock")
                    ? "The Ruined Wizard Tower already unlocked"
                    : "unlock The Ruined Wizard Tower");
        return result;

      case 698:
        // From Bad to Worst
        result = new ChoiceOption[2];
        result[0] =
            new ChoiceOption(
                Preferences.getBoolean("maraisBeaverUnlock")
                    ? "Swamp Beaver Territory already unlocked"
                    : "unlock Swamp Beaver Territory");
        result[1] =
            new ChoiceOption(
                Preferences.getBoolean("maraisVillageUnlock")
                    ? "The Weird Swamp Village already unlocked"
                    : "unlock The Weird Swamp Village");
        return result;

      case 700:
        // Delirium in the Cafeteria
        result = new ChoiceOption[9];
        result[0] =
            new ChoiceOption(
                KoLConstants.activeEffects.contains(JOCK_EFFECT) ? "Gain stats" : "Lose HP");
        result[1] =
            new ChoiceOption(
                KoLConstants.activeEffects.contains(NERD_EFFECT) ? "Gain stats" : "Lose HP");
        result[2] =
            new ChoiceOption(
                KoLConstants.activeEffects.contains(GREASER_EFFECT) ? "Gain stats" : "Lose HP");
        return result;

      case 721:
        {
          // The Cabin in the Dreadsylvanian Woods

          result = new ChoiceOption[6];

          StringBuilder buffer = new StringBuilder();
          buffer.append("dread tarragon");
          if (KoLCharacter.isMuscleClass()) {
            buffer.append(", old dry bone (");
            buffer.append(InventoryManager.getCount(ItemPool.OLD_DRY_BONE));
            buffer.append(") -> bone flour");
          }
          buffer.append(", -stench");
          result[0] = new ChoiceOption(buffer.toString()); // The Kitchen

          buffer.setLength(0);
          buffer.append("Freddies");
          buffer.append(", Bored Stiff (+100 spooky damage)");
          buffer.append(", replica key (");
          buffer.append(InventoryManager.getCount(ItemPool.REPLICA_KEY));
          buffer.append(") -> Dreadsylvanian auditor's badge");
          buffer.append(", wax banana (");
          buffer.append(InventoryManager.getCount(ItemPool.WAX_BANANA));
          buffer.append(") -> complicated lock impression");
          result[1] = new ChoiceOption(buffer.toString()); // The Cellar

          buffer.setLength(0);
          ChoiceAdventures.lockSpoiler(buffer);
          buffer.append("-spooky");
          if (KoLCharacter.isAccordionThief()) {
            buffer.append(" + intricate music box parts");
          }
          buffer.append(", fewer werewolves");
          buffer.append(", fewer vampires");
          buffer.append(", +Moxie");
          result[2] = new ChoiceOption(buffer.toString()); // The Attic (locked)

          result[4] = ChoiceAdventures.shortcutSpoiler("ghostPencil1");
          result[5] = new ChoiceOption("Leave this noncombat");
          return result;
        }

      case 722:
        // The Kitchen in the Woods
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("dread tarragon");
        result[1] =
            new ChoiceOption(
                "old dry bone ("
                    + InventoryManager.getCount(ItemPool.OLD_DRY_BONE)
                    + ") -> bone flour");
        result[2] = new ChoiceOption("-stench");
        result[5] = new ChoiceOption("Return to The Cabin");
        return result;

      case 723:
        // What Lies Beneath (the Cabin)
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("Freddies");
        result[1] = new ChoiceOption("Bored Stiff (+100 spooky damage)");
        result[2] =
            new ChoiceOption(
                "replica key ("
                    + InventoryManager.getCount(ItemPool.REPLICA_KEY)
                    + ") -> Dreadsylvanian auditor's badge");
        result[3] =
            new ChoiceOption(
                "wax banana ("
                    + InventoryManager.getCount(ItemPool.WAX_BANANA)
                    + ") -> complicated lock impression");
        result[5] = new ChoiceOption("Return to The Cabin");
        return result;

      case 724:
        // Where it's Attic
        result = new ChoiceOption[6];
        result[0] =
            new ChoiceOption(
                "-spooky"
                    + (KoLCharacter.isAccordionThief() ? " + intricate music box parts" : ""));
        result[1] = new ChoiceOption("fewer werewolves");
        result[2] = new ChoiceOption("fewer vampires");
        result[3] = new ChoiceOption("+Moxie");
        result[5] = new ChoiceOption("Return to The Cabin");
        return result;

      case 725:
        {
          // Tallest Tree in the Forest

          result = new ChoiceOption[6];

          StringBuilder buffer = new StringBuilder();
          if (KoLCharacter.isMuscleClass()) {
            buffer.append("drop blood kiwi");
            buffer.append(", -sleaze");
            buffer.append(", moon-amber");
          } else {
            buffer.append("unavailable (Muscle class only)");
          }
          result[0] = new ChoiceOption(buffer.toString()); // Climb tree (muscle only)

          buffer.setLength(0);
          ChoiceAdventures.lockSpoiler(buffer);
          buffer.append("fewer ghosts");
          buffer.append(", Freddies");
          buffer.append(", +Muscle");
          result[1] = new ChoiceOption(buffer.toString()); // Fire Tower (locked)

          buffer.setLength(0);
          buffer.append("blood kiwi (from above)");
          buffer.append(", Dreadsylvanian seed pod");
          if (KoLCharacter.hasEquipped(ItemPool.FOLDER_HOLDER)
              || KoLCharacter.hasEquipped(ItemPool.REPLICA_FOLDER_HOLDER)) {
            buffer.append(", folder (owl)");
          }

          result[2] = new ChoiceOption(buffer.toString()); // Base of tree

          result[4] = ChoiceAdventures.shortcutSpoiler("ghostPencil2");
          result[5] = new ChoiceOption("Leave this noncombat");
          return result;
        }

      case 726:
        // Top of the Tree, Ma!
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("drop blood kiwi");
        result[1] = new ChoiceOption("-sleaze");
        result[2] = new ChoiceOption("moon-amber");
        result[5] = new ChoiceOption("Return to The Tallest Tree");
        return result;

      case 727:
        // All Along the Watchtower
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("fewer ghosts");
        result[1] = new ChoiceOption("Freddies");
        result[2] = new ChoiceOption("+Muscle");
        result[5] = new ChoiceOption("Return to The Tallest Tree");
        return result;

      case 728:
        // Treebasing
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("blood kiwi (from above)");
        result[1] = new ChoiceOption("Dreadsylvanian seed pod");
        result[2] = new ChoiceOption("folder (owl)");
        result[5] = new ChoiceOption("Return to The Tallest Tree");
        return result;

      case 729:
        {
          // Below the Roots

          result = new ChoiceOption[6];

          StringBuilder buffer = new StringBuilder();
          buffer.append("-hot");
          buffer.append(", Dragged Through the Coals (+100 hot damage)");
          buffer.append(", old ball and chain (");
          buffer.append(InventoryManager.getCount(ItemPool.OLD_BALL_AND_CHAIN));
          buffer.append(") -> cool iron ingot");
          result[0] = new ChoiceOption(buffer.toString()); // Hot

          buffer.setLength(0);
          buffer.append("-cold");
          buffer.append(", +Mysticality");
          buffer.append(", Nature's Bounty (+300 max HP)");
          result[1] = new ChoiceOption(buffer.toString()); // Cold

          buffer.setLength(0);
          buffer.append("fewer bugbears");
          buffer.append(", Freddies");
          result[2] = new ChoiceOption(buffer.toString()); // Smelly

          result[4] = ChoiceAdventures.shortcutSpoiler("ghostPencil3");
          result[5] = new ChoiceOption("Leave this noncombat");
          return result;
        }

      case 730:
        // Hot Coals
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("-hot");
        result[1] = new ChoiceOption("Dragged Through the Coals (+100 hot damage)");
        result[2] =
            new ChoiceOption(
                "old ball and chain ("
                    + InventoryManager.getCount(ItemPool.OLD_BALL_AND_CHAIN)
                    + ") -> cool iron ingot");
        result[5] = new ChoiceOption("Return to The Burrows");
        return result;

      case 731:
        // The Heart of the Matter
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("-cold");
        result[1] = new ChoiceOption("+Mysticality");
        result[2] = new ChoiceOption("Nature's Bounty (+300 max HP)");
        result[5] = new ChoiceOption("Return to The Burrows");
        return result;

      case 732:
        // Once Midden, Twice Shy
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("fewer bugbears");
        result[1] = new ChoiceOption("Freddies");
        result[5] = new ChoiceOption("Return to The Burrows");
        return result;

      case 733:
        {
          // Dreadsylvanian Village Square

          result = new ChoiceOption[6];

          StringBuilder buffer = new StringBuilder();
          ChoiceAdventures.lockSpoiler(buffer);
          buffer.append("fewer ghosts");
          buffer.append(", ghost pencil");
          buffer.append(", +Mysticality");
          result[0] = new ChoiceOption(buffer.toString()); // Schoolhouse (locked)

          buffer.setLength(0);
          buffer.append("-cold");
          buffer.append(", Freddies");
          if (InventoryManager.getCount(ItemPool.HOTHAMMER) > 0) {
            buffer.append(", cool iron ingot (");
            buffer.append(InventoryManager.getCount(ItemPool.COOL_IRON_INGOT));
            buffer.append(") + warm fur (");
            buffer.append(InventoryManager.getCount(ItemPool.WARM_FUR));
            buffer.append(") -> cooling iron equipment");
          }
          result[1] = new ChoiceOption(buffer.toString()); // Blacksmith

          buffer.setLength(0);
          buffer.append("-spooky");
          buffer.append(", gain ");
          String item =
              KoLCharacter.isMuscleClass()
                  ? "hangman's hood"
                  : KoLCharacter.isMysticalityClass()
                      ? "cursed ring finger ring"
                      : KoLCharacter.isMoxieClass() ? "Dreadsylvanian clockwork key" : "nothing";
          buffer.append(item);
          buffer.append(" with help of clannie");
          buffer.append(" or help clannie gain an item");
          result[2] = new ChoiceOption(buffer.toString()); // Gallows

          result[4] = ChoiceAdventures.shortcutSpoiler("ghostPencil4");
          result[5] = new ChoiceOption("Leave this noncombat");
          return result;
        }

      case 734:
        // Fright School
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("fewer ghosts");
        result[1] = new ChoiceOption("ghost pencil");
        result[2] = new ChoiceOption("+Mysticality");
        result[5] = new ChoiceOption("Return to The Village Square");
        return result;

      case 735:
        // Smith, Black as Night
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("-cold");
        result[1] = new ChoiceOption("Freddies");
        result[2] =
            new ChoiceOption(
                "cool iron ingot ("
                    + InventoryManager.getCount(ItemPool.COOL_IRON_INGOT)
                    + ") + warm fur ("
                    + InventoryManager.getCount(ItemPool.WARM_FUR)
                    + ") -> cooling iron equipment");
        result[5] = new ChoiceOption("Return to The Village Square");
        return result;

      case 736:
        // Gallows
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("-spooky");
        result[1] =
            new ChoiceOption(
                "gain "
                    + (KoLCharacter.isMuscleClass()
                        ? "hangman's hood"
                        : KoLCharacter.isMysticalityClass()
                            ? "cursed ring finger ring"
                            : KoLCharacter.isMoxieClass()
                                ? "Dreadsylvanian clockwork key"
                                : "nothing")
                    + " with help of clannie");
        result[3] = new ChoiceOption("help clannie gain an item");
        result[5] = new ChoiceOption("Return to The Village Square");
        return result;

      case 737:
        {
          // The Even More Dreadful Part of Town

          result = new ChoiceOption[6];

          StringBuilder buffer = new StringBuilder();
          buffer.append("-stench");
          buffer.append(", Sewer-Drenched (+100 stench damage)");
          result[0] = new ChoiceOption(buffer.toString()); // Sewers

          buffer.setLength(0);
          buffer.append("fewer skeletons");
          buffer.append(", -sleaze");
          buffer.append(", +Muscle");
          result[1] = new ChoiceOption(buffer.toString()); // Tenement

          buffer.setLength(0);
          if (KoLCharacter.isMoxieClass()) {
            buffer.append("Freddies");
            buffer.append(", lock impression (");
            buffer.append(InventoryManager.getCount(ItemPool.WAX_LOCK_IMPRESSION));
            buffer.append(") + music box parts (");
            buffer.append(InventoryManager.getCount(ItemPool.INTRICATE_MUSIC_BOX_PARTS));
            buffer.append(") -> replica key");
            buffer.append(", moon-amber (");
            buffer.append(InventoryManager.getCount(ItemPool.MOON_AMBER));
            buffer.append(") -> polished moon-amber");
            buffer.append(", 3 music box parts (");
            buffer.append(InventoryManager.getCount(ItemPool.INTRICATE_MUSIC_BOX_PARTS));
            buffer.append(") + clockwork key (");
            buffer.append(InventoryManager.getCount(ItemPool.DREADSYLVANIAN_CLOCKWORK_KEY));
            buffer.append(") -> mechanical songbird");
            buffer.append(", 3 lengths of old fuse");
          } else {
            buffer.append("unavailable (Moxie class only)");
          }
          result[2] = new ChoiceOption(buffer.toString()); // Ticking Shack (moxie only)

          result[4] = ChoiceAdventures.shortcutSpoiler("ghostPencil5");
          result[5] = new ChoiceOption("Leave this noncombat");
          return result;
        }

      case 738:
        // A Dreadful Smell
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("-stench");
        result[1] = new ChoiceOption("Sewer-Drenched (+100 stench damage)");
        result[5] = new ChoiceOption("Return to Skid Row");
        return result;

      case 739:
        // The Tinker's. Damn.
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("Freddies");
        result[1] =
            new ChoiceOption(
                "lock impression ("
                    + InventoryManager.getCount(ItemPool.WAX_LOCK_IMPRESSION)
                    + ") + music box parts ("
                    + InventoryManager.getCount(ItemPool.INTRICATE_MUSIC_BOX_PARTS)
                    + ") -> replica key");
        result[2] =
            new ChoiceOption(
                "moon-amber ("
                    + InventoryManager.getCount(ItemPool.MOON_AMBER)
                    + ") -> polished moon-amber");
        result[3] =
            new ChoiceOption(
                "3 music box parts ("
                    + InventoryManager.getCount(ItemPool.INTRICATE_MUSIC_BOX_PARTS)
                    + ") + clockwork key ("
                    + InventoryManager.getCount(ItemPool.DREADSYLVANIAN_CLOCKWORK_KEY)
                    + ") -> mechanical songbird");
        result[4] = new ChoiceOption("3 lengths of old fuse");
        result[5] = new ChoiceOption("Return to Skid Row");
        return result;

      case 740:
        // Eight, Nine, Tenement
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("fewer skeletons");
        result[1] = new ChoiceOption("-sleaze");
        result[2] = new ChoiceOption("+Muscle");
        result[5] = new ChoiceOption("Return to Skid Row");
        return result;

      case 741:
        {
          // The Old Duke's Estate

          result = new ChoiceOption[6];

          StringBuilder buffer = new StringBuilder();
          buffer.append("fewer zombies");
          buffer.append(", Freddies");
          buffer.append(", Fifty Ways to Bereave Your Lover (+100 sleaze damage)");
          result[0] = new ChoiceOption(buffer.toString()); // Cemetery

          buffer.setLength(0);
          buffer.append("-hot");
          if (KoLCharacter.isMysticalityClass()) {
            buffer.append(", dread tarragon (");
            buffer.append(InventoryManager.getCount(ItemPool.DREAD_TARRAGON));
            buffer.append(") + dreadful roast (");
            buffer.append(InventoryManager.getCount(ItemPool.DREADFUL_ROAST));
            buffer.append(") + bone flour (");
            buffer.append(InventoryManager.getCount(ItemPool.BONE_FLOUR));
            buffer.append(") + stinking agaricus (");
            buffer.append(InventoryManager.getCount(ItemPool.STINKING_AGARICUS));
            buffer.append(") -> Dreadsylvanian shepherd's pie");
          }
          buffer.append(", +Moxie");
          result[1] = new ChoiceOption(buffer.toString()); // Servants' Quarters

          buffer.setLength(0);
          ChoiceAdventures.lockSpoiler(buffer);
          buffer.append("fewer werewolves");
          buffer.append(", eau de mort");
          buffer.append(", 10 ghost thread (");
          buffer.append(InventoryManager.getCount(ItemPool.GHOST_THREAD));
          buffer.append(") -> ghost shawl");
          result[2] = new ChoiceOption(buffer.toString()); // Master Suite (locked)

          result[4] = ChoiceAdventures.shortcutSpoiler("ghostPencil6");
          result[5] = new ChoiceOption("Leave this noncombat");
          return result;
        }

      case 742:
        // The Plot Thickens
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("fewer zombies");
        result[1] = new ChoiceOption("Freddies");
        result[2] = new ChoiceOption("Fifty Ways to Bereave Your Lover (+100 sleaze damage)");
        result[5] = new ChoiceOption("Return to The Old Duke's Estate");
        return result;

      case 743:
        // No Quarter
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("-hot");
        result[1] =
            new ChoiceOption(
                "dread tarragon ("
                    + InventoryManager.getCount(ItemPool.DREAD_TARRAGON)
                    + ") + dreadful roast ("
                    + InventoryManager.getCount(ItemPool.DREADFUL_ROAST)
                    + ") + bone flour ("
                    + InventoryManager.getCount(ItemPool.BONE_FLOUR)
                    + ") + stinking agaricus ("
                    + InventoryManager.getCount(ItemPool.STINKING_AGARICUS)
                    + ") -> Dreadsylvanian shepherd's pie");
        result[2] = new ChoiceOption("+Moxie");
        result[5] = new ChoiceOption("Return to The Old Duke's Estate");
        return result;

      case 744:
        // The Master Suite -- Sweet!
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("fewer werewolves");
        result[1] = new ChoiceOption("eau de mort");
        result[2] =
            new ChoiceOption(
                "10 ghost thread ("
                    + InventoryManager.getCount(ItemPool.GHOST_THREAD)
                    + ") -> ghost shawl");
        result[5] = new ChoiceOption("Return to The Old Duke's Estate");
        return result;

      case 745:
        {
          // This Hall is Really Great

          result = new ChoiceOption[6];

          StringBuilder buffer = new StringBuilder();
          ChoiceAdventures.lockSpoiler(buffer);
          buffer.append("fewer vampires");
          buffer.append(", ");
          if (KoLCharacter.hasEquipped(ItemPool.get(ItemPool.MUDDY_SKIRT, 1))) {
            buffer.append("equipped muddy skirt -> weedy skirt and ");
          } else if (InventoryManager.getCount(ItemPool.MUDDY_SKIRT) > 0) {
            buffer.append("(muddy skirt in inventory but not equipped) ");
          }
          buffer.append("+Moxie");
          result[0] = new ChoiceOption(buffer.toString()); // Ballroom (locked)

          buffer.setLength(0);
          buffer.append("-cold");
          buffer.append(", Staying Frosty (+100 cold damage)");
          result[1] = new ChoiceOption(buffer.toString()); // Kitchen

          buffer.setLength(0);
          buffer.append("dreadful roast");
          buffer.append(", -stench");
          if (KoLCharacter.isMysticalityClass()) {
            buffer.append(", wax banana");
          }
          result[2] = new ChoiceOption(buffer.toString()); // Dining Room

          result[4] = ChoiceAdventures.shortcutSpoiler("ghostPencil7");
          result[5] = new ChoiceOption("Leave this noncombat");
          return result;
        }

      case 746:
        // The Belle of the Ballroom
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("fewer vampires");
        result[1] =
            new ChoiceOption(
                (KoLCharacter.hasEquipped(ItemPool.get(ItemPool.MUDDY_SKIRT, 1))
                        ? "equipped muddy skirt -> weedy skirt and "
                        : InventoryManager.getCount(ItemPool.MUDDY_SKIRT) > 0
                            ? "(muddy skirt in inventory but not equipped) "
                            : "")
                    + "+Moxie");
        result[5] = new ChoiceOption("Return to The Great Hall");
        return result;

      case 747:
        // Cold Storage
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("-cold");
        result[1] = new ChoiceOption("Staying Frosty (+100 cold damage)");
        result[5] = new ChoiceOption("Return to The Great Hall");
        return result;

      case 748:
        // Dining In (the Castle)
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("dreadful roast");
        result[1] = new ChoiceOption("-stench");
        result[2] = new ChoiceOption("wax banana");
        result[5] = new ChoiceOption("Return to The Great Hall");
        return result;

      case 749:
        {
          // Tower Most Tall

          result = new ChoiceOption[6];

          StringBuilder buffer = new StringBuilder();
          ChoiceAdventures.lockSpoiler(buffer);
          buffer.append("fewer bugbears");
          buffer.append(", fewer zombies");
          buffer.append(", visit The Machine");
          if (KoLCharacter.isMoxieClass()) {
            buffer.append(", blood kiwi (");
            buffer.append(InventoryManager.getCount(ItemPool.BLOOD_KIWI));
            buffer.append(") + eau de mort (");
            buffer.append(InventoryManager.getCount(ItemPool.EAU_DE_MORT));
            buffer.append(") -> bloody kiwitini");
          }
          result[0] = new ChoiceOption(buffer.toString()); // Laboratory (locked)

          buffer.setLength(0);
          if (KoLCharacter.isMysticalityClass()) {
            buffer.append("fewer skeletons");
            buffer.append(", +Mysticality");
            buffer.append(", learn recipe for moon-amber necklace");
          } else {
            buffer.append("unavailable (Mysticality class only)");
          }
          result[1] = new ChoiceOption(buffer.toString()); // Books (mysticality only)

          buffer.setLength(0);
          buffer.append("-sleaze");
          buffer.append(", Freddies");
          buffer.append(", Magically Fingered (+150 max MP, 40-50 MP regen)");
          result[2] = new ChoiceOption(buffer.toString()); // Bedroom

          result[4] = ChoiceAdventures.shortcutSpoiler("ghostPencil8");
          result[5] = new ChoiceOption("Leave this noncombat");
          return result;
        }

      case 750:
        // Working in the Lab, Late One Night
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("fewer bugbears");
        result[1] = new ChoiceOption("fewer zombies");
        result[2] = new ChoiceOption("visit The Machine");
        result[3] =
            new ChoiceOption(
                "blood kiwi ("
                    + InventoryManager.getCount(ItemPool.BLOOD_KIWI)
                    + ") + eau de mort ("
                    + InventoryManager.getCount(ItemPool.EAU_DE_MORT)
                    + ") -> bloody kiwitini");
        result[5] = new ChoiceOption("Return to The Tower");
        return result;

      case 751:
        // Among the Quaint and Curious Tomes.
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("fewer skeletons");
        result[1] = new ChoiceOption("+Mysticality");
        result[2] = new ChoiceOption("learn recipe for moon-amber necklace");
        result[5] = new ChoiceOption("Return to The Tower");
        return result;

      case 752:
        // In The Boudoir
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("-sleaze");
        result[1] = new ChoiceOption("Freddies");
        result[2] = new ChoiceOption("Magically Fingered (+150 max MP, 40-50 MP regen)");
        result[5] = new ChoiceOption("Return to The Tower");
        return result;

      case 753:
        {
          // The Dreadsylvanian Dungeon

          result = new ChoiceOption[6];

          StringBuilder buffer = new StringBuilder();
          buffer.append("-spooky");
          buffer.append(", +Muscle");
          buffer.append(", +MP");
          result[0] = new ChoiceOption(buffer.toString()); // Prison

          buffer.setLength(0);
          buffer.append("-hot");
          buffer.append(", Freddies");
          buffer.append(", +Muscle/Mysticality/Moxie");
          result[1] = new ChoiceOption(buffer.toString()); // Boiler Room

          buffer.setLength(0);
          buffer.append("stinking agaricus");
          buffer.append(", Spore-wreathed (reduce enemy defense by 20%)");
          result[2] = new ChoiceOption(buffer.toString()); // Guard room

          result[4] = ChoiceAdventures.shortcutSpoiler("ghostPencil9");
          result[5] = new ChoiceOption("Leave this noncombat");
          return result;
        }

      case 754:
        // Live from Dungeon Prison
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("-spooky");
        result[1] = new ChoiceOption("+Muscle");
        result[2] = new ChoiceOption("+MP");
        result[5] = new ChoiceOption("Return to The Dungeons");
        return result;

      case 755:
        // The Hot Bowels
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("-hot");
        result[1] = new ChoiceOption("Freddies");
        result[2] = new ChoiceOption("+Muscle/Mysticality/Moxie");
        result[5] = new ChoiceOption("Return to The Dungeons");
        return result;

      case 756:
        // Among the Fungus
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("stinking agaricus");
        result[1] = new ChoiceOption("Spore-wreathed (reduce enemy defense by 20%)");
        result[5] = new ChoiceOption("Return to The Dungeons");
        return result;

      case 758:
        {
          // End of the Path

          StringBuilder buffer = new StringBuilder();
          boolean necklaceEquipped = KoLCharacter.hasEquipped(ChoiceAdventures.MOON_AMBER_NECKLACE);
          boolean necklaceAvailable =
              InventoryManager.getCount(ChoiceAdventures.MOON_AMBER_NECKLACE) > 0;
          boolean hasKiwiEffect =
              KoLConstants.activeEffects.contains(ChoiceAdventures.KIWITINI_EFFECT);
          boolean isBlind =
              KoLConstants.activeEffects.contains(ChoiceAdventures.TEMPORARY_BLINDNESS)
                  || KoLCharacter.hasEquipped(ChoiceAdventures.MAKESHIFT_TURBAN)
                  || KoLCharacter.hasEquipped(ChoiceAdventures.HELPS_YOU_SLEEP)
                  || KoLCharacter.hasEquipped(ChoiceAdventures.SLEEP_MASK);
          boolean kiwitiniAvailable =
              InventoryManager.getCount(ChoiceAdventures.BLOODY_KIWITINI) > 0;

          buffer.append(
              necklaceEquipped
                  ? "moon-amber necklace equipped"
                  : necklaceAvailable
                      ? "moon-amber necklace NOT equipped but in inventory"
                      : "moon-amber necklace neither equipped nor available");
          buffer.append(" / ");
          buffer.append(
              hasKiwiEffect
                  ? (isBlind ? "First Blood Kiwi and blind" : "First Blood Kiwi but NOT blind")
                  : kiwitiniAvailable
                      ? "bloody kiwitini in inventory"
                      : "First Blood Kiwi neither active nor available");

          result = new ChoiceOption[2];
          result[0] = new ChoiceOption(buffer.toString());
          result[1] = new ChoiceOption("Run away");
          return result;
        }

      case 759:
        {
          // You're About to Fight City Hall

          StringBuilder buffer = new StringBuilder();
          boolean badgeEquipped = KoLCharacter.hasEquipped(ChoiceAdventures.AUDITORS_BADGE);
          boolean badgeAvailable = InventoryManager.getCount(ChoiceAdventures.AUDITORS_BADGE) > 0;
          boolean skirtEquipped = KoLCharacter.hasEquipped(ChoiceAdventures.WEEDY_SKIRT);
          boolean skirtAvailable = InventoryManager.getCount(ChoiceAdventures.WEEDY_SKIRT) > 0;

          buffer.append(
              badgeEquipped
                  ? "Dreadsylvanian auditor's badge equipped"
                  : badgeAvailable
                      ? "Dreadsylvanian auditor's badge NOT equipped but in inventory"
                      : "Dreadsylvanian auditor's badge neither equipped nor available");
          buffer.append(" / ");
          buffer.append(
              skirtEquipped
                  ? "weedy skirt equipped"
                  : skirtAvailable
                      ? "weedy skirt NOT equipped but in inventory"
                      : "weedy skirt neither equipped nor available");

          result = new ChoiceOption[2];
          result[0] = new ChoiceOption(buffer.toString());
          result[1] = new ChoiceOption("Run away");
          return result;
        }

      case 760:
        {
          // Holding Court

          StringBuilder buffer = new StringBuilder();
          boolean shawlEquipped = KoLCharacter.hasEquipped(ChoiceAdventures.GHOST_SHAWL);
          boolean shawlAvailable = InventoryManager.getCount(ChoiceAdventures.GHOST_SHAWL) > 0;
          boolean hasPieEffect = KoLConstants.activeEffects.contains(ChoiceAdventures.PIE_EFFECT);
          boolean pieAvailable = InventoryManager.getCount(ChoiceAdventures.SHEPHERDS_PIE) > 0;

          buffer.append(
              shawlEquipped
                  ? "ghost shawl equipped"
                  : shawlAvailable
                      ? "ghost shawl NOT equipped but in inventory"
                      : "ghost shawl neither equipped nor available");
          buffer.append(" / ");
          buffer.append(
              hasPieEffect
                  ? "Shepherd's Breath active"
                  : pieAvailable
                      ? "Dreadsylvanian shepherd's pie in inventory"
                      : "Shepherd's Breath neither active nor available");

          result = new ChoiceOption[2];
          result[0] = new ChoiceOption(buffer.toString());
          result[1] = new ChoiceOption("Run away");
          return result;
        }

      case 772:
        {
          // Saved by the Bell

          // If you reach this encounter and Mafia things you've not spend 40 adventures in KOL High
          // school, correct this
          Preferences.setInteger("_kolhsAdventures", 40);

          result = new ChoiceOption[10];
          String buffer =
              "Get "
                  + (Preferences.getInteger("kolhsTotalSchoolSpirited") + 1) * 10
                  + " turns of School Spirited (+100% Meat drop, +50% Item drop)";
          result[0] =
              new ChoiceOption(
                  Preferences.getBoolean("_kolhsSchoolSpirited")
                      ? "Already got School Spirited today"
                      : buffer);
          result[1] =
              new ChoiceOption(
                  Preferences.getBoolean("_kolhsPoeticallyLicenced")
                      ? "Already got Poetically Licenced today"
                      : "50 turns of Poetically Licenced (+20% Myst, -20% Muscle, +2 Myst stats/fight, +10% Spell damage)");
          result[2] =
              new ChoiceOption(
                  InventoryManager.getCount(ItemPool.YEARBOOK_CAMERA) > 0
                          || KoLCharacter.hasEquipped(ItemPool.get(ItemPool.YEARBOOK_CAMERA, 1))
                      ? "Turn in yesterday's photo (if you have it)"
                      : "Get Yearbook Camera");
          result[3] =
              new ChoiceOption(
                  Preferences.getBoolean("_kolhsCutButNotDried")
                      ? "Already got Cut But Not Dried today"
                      : "50 turns of Cut But Not Dried (+20% Muscle, -20% Moxie, +2 Muscle stats/fight, +10% Weapon damage)");
          result[4] =
              new ChoiceOption(
                  Preferences.getBoolean("_kolhsIsskayLikeAnAshtray")
                      ? "Already got Isskay Like An Ashtray today"
                      : "50 turns of Isskay Like An Ashtray (+20% Moxie, -20% Myst, +2 Moxie stats/fight, +10% Pickpocket chance)");
          result[5] = new ChoiceOption("Make items");
          result[6] = new ChoiceOption("Make items");
          result[7] = new ChoiceOption("Make items");
          result[9] = new ChoiceOption("Leave");
          return result;
        }

      case 780:
        {
          // Action Elevator

          int hiddenApartmentProgress = Preferences.getInteger("hiddenApartmentProgress");
          boolean hasOnceCursed = KoLConstants.activeEffects.contains(CURSE1_EFFECT);
          boolean hasTwiceCursed = KoLConstants.activeEffects.contains(CURSE2_EFFECT);
          boolean hasThriceCursed = KoLConstants.activeEffects.contains(CURSE3_EFFECT);
          boolean pygmyLawyersRelocated =
              Preferences.getInteger("relocatePygmyLawyer") == KoLCharacter.getAscensions();

          result = new ChoiceOption[6];
          result[0] =
              new ChoiceOption(
                  (hiddenApartmentProgress >= 7
                      ? "penthouse empty"
                      : hasThriceCursed
                          ? "Fight ancient protector spirit"
                          : "Need Thrice-Cursed to fight ancient protector spirit"));
          result[1] =
              new ChoiceOption(
                  (hasThriceCursed
                      ? "Increase Thrice-Cursed"
                      : hasTwiceCursed
                          ? "Get Thrice-Cursed"
                          : hasOnceCursed ? "Get Twice-Cursed" : "Get Once-Cursed"));
          result[2] =
              new ChoiceOption(
                  (pygmyLawyersRelocated
                      ? "Waste adventure"
                      : "Relocate pygmy witch lawyers to Hidden Park"));
          result[5] = SKIP_ADVENTURE;
          return result;
        }

      case 781:
        // Earthbound and Down
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("Unlock Hidden Apartment Building");
        result[1] = new ChoiceOption("Get stone triangle");
        result[2] = new ChoiceOption("Get Blessing of Bulbazinalli");
        result[5] = SKIP_ADVENTURE;
        return result;

      case 783:
        // Water You Dune
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("Unlock Hidden Hospital");
        result[1] = new ChoiceOption("Get stone triangle");
        result[2] = new ChoiceOption("Get Blessing of Squirtlcthulli");
        result[5] = SKIP_ADVENTURE;
        return result;

      case 784:
        // You, M. D.
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("Fight ancient protector spirit");
        result[5] = SKIP_ADVENTURE;
        return result;

      case 785:
        // Air Apparent
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("Unlock Hidden Office Building");
        result[1] = new ChoiceOption("Get stone triangle");
        result[2] = new ChoiceOption("Get Blessing of Pikachutlotal");
        result[5] = SKIP_ADVENTURE;
        return result;

      case 786:
        {
          // Working Holiday

          int hiddenOfficeProgress = Preferences.getInteger("hiddenOfficeProgress");
          boolean hasBossUnlock = hiddenOfficeProgress >= 6;
          boolean hasMcCluskyFile = InventoryManager.getCount(MCCLUSKY_FILE) > 0;
          boolean hasBinderClip = InventoryManager.getCount(BINDER_CLIP) > 0;

          result = new ChoiceOption[6];
          result[0] =
              new ChoiceOption(
                  (hiddenOfficeProgress >= 7
                      ? "office empty"
                      : hasMcCluskyFile || hasBossUnlock
                          ? "Fight ancient protector spirit"
                          : "Need McClusky File (complete) to fight ancient protector spirit"));
          result[1] =
              new ChoiceOption(
                  (hasBinderClip || hasMcCluskyFile || hasBossUnlock)
                      ? "Get random item"
                      : "Get boring binder clip");
          result[2] = new ChoiceOption("Fight pygmy witch accountant");
          result[5] = SKIP_ADVENTURE;
          return result;
        }

      case 787:
        // Fire when Ready
        result = new ChoiceOption[6];
        result[0] = new ChoiceOption("Unlock Hidden Bowling Alley");
        result[1] = new ChoiceOption("Get stone triangle");
        result[2] = new ChoiceOption("Get Blessing of Charcoatl");
        result[5] = SKIP_ADVENTURE;
        return result;

      case 788:
        {
          // Life is Like a Cherry of Bowls
          int hiddenBowlingAlleyProgress = Preferences.getInteger("hiddenBowlingAlleyProgress");

          StringBuilder buffer = new StringBuilder();
          buffer.append("Get stats, on 5th visit, fight ancient protector spirit (");
          buffer.append((6 - hiddenBowlingAlleyProgress));
          buffer.append(" visit");
          if (hiddenBowlingAlleyProgress < 5) {
            buffer.append("s");
          }
          buffer.append(" left");

          result = new ChoiceOption[6];
          result[0] =
              new ChoiceOption(
                  (hiddenBowlingAlleyProgress > 6
                      ? "Get stats"
                      : hiddenBowlingAlleyProgress == 6
                          ? "fight ancient protector spirit"
                          : buffer.toString()));
          result[5] = SKIP_ADVENTURE;
          return result;
        }

      case 789:
        {
          boolean pygmyJanitorsRelocated =
              Preferences.getInteger("relocatePygmyJanitor") == KoLCharacter.getAscensions();

          // Where Does The Lone Ranger Take His Garbagester?
          result = new ChoiceOption[6];
          result[0] = new ChoiceOption("Get random items");
          result[1] =
              new ChoiceOption(
                  pygmyJanitorsRelocated
                      ? "Waste adventure"
                      : "Relocate pygmy janitors to Hidden Park");
          result[5] = SKIP_ADVENTURE;
          return result;
        }

      case 791:
        {
          // Legend of the Temple in the Hidden City

          int stoneTriangles = InventoryManager.getCount(ChoiceAdventures.STONE_TRIANGLE);

          result = new ChoiceOption[6];
          String buffer =
              "Need 4 stone triangles to fight Protector Spectre (" + stoneTriangles + ")";
          result[0] = new ChoiceOption(stoneTriangles == 4 ? "fight Protector Spectre" : buffer);
          result[5] = SKIP_ADVENTURE;
          return result;
        }

      case 801:

        // A Reanimated Conversation
        result = new ChoiceOption[7];
        result[0] = new ChoiceOption("skulls increase meat drops");
        result[1] = new ChoiceOption("arms deal extra damage");
        result[2] = new ChoiceOption("legs increase item drops");
        result[3] = new ChoiceOption("wings sometimes delevel at start of combat");
        result[4] = new ChoiceOption("weird parts sometimes block enemy attacks");
        result[5] = new ChoiceOption("get rid of all collected parts");
        result[6] = new ChoiceOption("no changes");
        return result;

      case 918:

        // Yachtzee
        result = new ChoiceOption[3];
        // Is it 7 or more days since the last time you got the Ultimate Mind Destroyer?
        var date = DateTimeManager.getArizonaDateTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String lastUMDDateString = Preferences.getString("umdLastObtained");
        if (lastUMDDateString != null && !lastUMDDateString.equals("")) {
          try {
            var compareDate =
                sdf.parse(lastUMDDateString)
                    .toInstant()
                    .atZone(DateTimeManager.ARIZONA)
                    .plusDays(7);
            if (date.compareTo(compareDate) >= 0) {
              result[0] = new ChoiceOption("get Ultimate Mind Destroyer");
            } else {
              result[0] = new ChoiceOption("get cocktail ingredients");
            }
          } catch (ParseException ex) {
            result[0] =
                new ChoiceOption("get cocktail ingredients (sometimes Ultimate Mind Destroyer)");
            KoLmafia.updateDisplay("Unable to parse " + lastUMDDateString);
          }
        } else {
          // Change to "get Ultimate Mind Destroyer" after 12th August 2014
          result[0] =
              new ChoiceOption("get cocktail ingredients (sometimes Ultimate Mind Destroyer)");
        }
        result[1] = new ChoiceOption("get 5k meat and random item");
        result[2] = new ChoiceOption("get Beach Bucks");
        return result;

      case 988:

        // The Containment Unit
        result = new ChoiceOption[2];
        String containment = Preferences.getString("EVEDirections");
        if (containment.length() != 6) {
          return result;
        }
        int progress = StringUtilities.parseInt(containment.substring(5, 6));
        if (progress < 0 && progress > 5) {
          return result;
        }
        if (containment.charAt(progress) == 'L') {
          result[0] = new ChoiceOption("right way");
          result[1] = null;
        } else if (containment.charAt(progress) == 'R') {
          result[0] = null;
          result[1] = new ChoiceOption("right way");
        } else {
          result[0] = new ChoiceOption("unknown");
          result[1] = new ChoiceOption("unknown");
        }
        return result;

      case 1049:
        {
          // Tomb of the Unknown Your Class Here

          String responseText = ChoiceManager.lastResponseText;
          Map<Integer, String> choices = ChoiceUtilities.parseChoices(responseText);
          int options = choices.size();
          if (options == 1) {
            return new ChoiceOption[0];
          }

          int decision = ChoiceManager.getDecision(choice, responseText);
          if (decision == 0) {
            return new ChoiceOption[0];
          }

          result = new ChoiceOption[options];
          for (int i = 0; i < options; ++i) {
            result[i] = new ChoiceOption((i == decision - 1) ? "right answer" : "wrong answer");
          }

          return result;
        }

      case 1411:
        {
          // The Hall in the Hall
          result = new ChoiceOption[5];
          {
            boolean haveStaff = InventoryManager.getCount(ItemPool.DRIPPY_STAFF) > 0;
            int inebriety = KoLCharacter.getInebriety();
            int totalPoolSkill = KoLCharacter.estimatedPoolSkill();
            String buf =
                (haveStaff ? "M" : "A drippy staff and m")
                    + "aybe a drippy orb (Pool Skill at "
                    + Integer.valueOf(inebriety)
                    + " inebriety = "
                    + Integer.valueOf(totalPoolSkill)
                    + ")";
            result[0] = new ChoiceOption(buf);
          }
          result[1] = new ChoiceOption("Buy a drippy candy bar for 10,000 Meat or get Driplets");
          {
            String item =
                KoLCharacter.hasSkill(SkillPool.DRIPPY_EYE_SPROUT)
                    ? "a drippy seed"
                    : KoLCharacter.hasSkill(SkillPool.DRIPPY_EYE_STONE)
                        ? "a drippy bezoar"
                        : KoLCharacter.hasSkill(SkillPool.DRIPPY_EYE_BEETLE)
                            ? "a drippy grub"
                            : "nothing";
            result[2] = new ChoiceOption("Get " + item);
          }
          {
            int steins = InventoryManager.getCount(ItemPool.DRIPPY_STEIN);
            result[3] =
                new ChoiceOption(
                    (steins > 0) ? "Trade a drippy stein for a drippy pilsner" : "Get nothing");
          }
          result[4] = new ChoiceOption("Get some Driplets");
          return result;
        }

      case 1489:
        {
          // Slagging Off
          result = new ChoiceOption[3];
          result[0] = new ChoiceOption("Get a crystal Crimbo goblet", "crystal Crimbo goblet");
          result[1] = new ChoiceOption("Get a crystal Crimbo platter", "crystal Crimbo platter");
          result[2] = new ChoiceOption("Walk away in disappointment");
          return result;
        }

      case 1499:
        {
          // A Labyrinth of Shadows

          String responseText = ChoiceManager.lastResponseText;
          Map<Integer, String> choices = ChoiceUtilities.parseChoices(responseText);
          int options = choices.size();

          result = new ChoiceOption[options];
          result[0] = new ChoiceOption("Randomize themes");
          for (int i = 1; i <= 3; ++i) {
            result[i] = RufusManager.shadowLabyrinthSpoiler(choices.get(i + 1));
          }
          result[4] = new ChoiceOption("Randomize themes");
          result[5] = new ChoiceOption("Leave with nothing");

          return result;
        }
    }
    return null;
  }

  private static ChoiceOption booPeakDamage() {
    int booPeakLevel =
        ChoiceControl.findBooPeakLevel(
            ChoiceUtilities.findChoiceDecisionText(1, ChoiceManager.lastResponseText));
    if (booPeakLevel < 1) return new ChoiceOption("");

    int damageTaken = 0;
    int diff = 0;

    switch (booPeakLevel) {
      case 1:
        // actual base damage is 13
        damageTaken = 30;
        diff = 17;
        break;
      case 2:
        // actual base damage is 25
        damageTaken = 30;
        diff = 5;
        break;
      case 3:
        damageTaken = 50;
        break;
      case 4:
        damageTaken = 125;
        break;
      case 5:
        damageTaken = 250;
        break;
    }

    double spookyDamage =
        KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.SPOOKYFORM))
            ? 1.0
            : Math.max(
                damageTaken
                        * (100.0
                            - KoLCharacter.elementalResistanceByLevel(
                                KoLCharacter.getElementalResistanceLevels(Element.SPOOKY)))
                        / 100.0
                    - diff,
                1);
    if (KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.COLDFORM))
        || KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.SLEAZEFORM))) {
      spookyDamage *= 2;
    }

    double coldDamage =
        KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.COLDFORM))
            ? 1.0
            : Math.max(
                damageTaken
                        * (100.0
                            - KoLCharacter.elementalResistanceByLevel(
                                KoLCharacter.getElementalResistanceLevels(Element.COLD)))
                        / 100.0
                    - diff,
                1);
    if (KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.SLEAZEFORM))
        || KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.STENCHFORM))) {
      coldDamage *= 2;
    }
    return new ChoiceOption(
        ((int) Math.ceil(spookyDamage))
            + " spooky damage, "
            + ((int) Math.ceil(coldDamage))
            + " cold damage");
  }

  private static ChoiceOption shortcutSpoiler(final String setting) {
    return new ChoiceOption(Preferences.getBoolean(setting) ? "shortcut KNOWN" : "learn shortcut");
  }

  private static void lockSpoiler(StringBuilder buffer) {
    buffer.append("possibly locked,");
    if (InventoryManager.getCount(ItemPool.DREADSYLVANIAN_SKELETON_KEY) == 0) {
      buffer.append(" no");
    }
    buffer.append(" key in inventory: ");
  }

  public static final ChoiceOption choiceSpoiler(
      final int choice, final int decision, final ChoiceOption[] spoilers) {
    switch (choice) {
      case 105:
        // Having a Medicine Ball
        if (decision == 3) {
          KoLCharacter.ensureUpdatedGuyMadeOfBees();
          boolean defeated = Preferences.getBoolean("guyMadeOfBeesDefeated");
          if (defeated) {
            return new ChoiceOption("guy made of bees: defeated");
          }
          return new ChoiceOption(
              "guy made of bees: called " + Preferences.getString("guyMadeOfBeesCount") + " times");
        }
        break;
      case 182:
        if (decision == 4) {
          return new ChoiceOption("model airship");
        }
        break;
      case 793:
        if (decision == 4) {
          return new ChoiceOption("gift shop");
        }
        break;
    }

    if (spoilers == null) {
      return null;
    }

    // Iterate through the spoilers and find the one corresponding to the decision
    for (int i = 0; i < spoilers.length; ++i) {
      ChoiceOption spoiler = spoilers[i];
      if (spoiler == null) {
        continue;
      }
      int option = spoiler.getOption();
      if (option == decision) {
        return spoiler;
      }
      if (option != 0) {
        continue;
      }
      // option of 0 means use positional index
      if ((i + 1) == decision) {
        return spoiler;
      }
    }

    // If we get here, we ran out of spoilers.
    return null;
  }

  private static final String[][] OLD_MAN_PSYCHOSIS_SPOILERS = {
    {"Draw a Monster with a Crayon", "-1 Crayon, Add Cray-Kin"},
    {"Build a Bubble Mountain", "+3 crew, -8-10 bubbles"},
    {"Ask Mom for More Bath Toys", "+2 crayons, +8-11 bubbles"},
    {"Draw a Bunch of Coconuts with Crayons", "Block Ferocious roc, -2 crayons"},
    {"Splash in the Water", "Add Bristled Man-O-War"},
    {"Draw a Big Storm Cloud on the Shower Wall", "Block Deadly Hydra, -3 crayons"},
    {"Knock an Action Figure Overboard", "+20-23 bubbles, -1 crew"},
    {"Submerge Some Bubbles", "Block giant man-eating shark, -16 bubbles"},
    {"Turn on the Shower Wand", "Add Deadly Hydra"},
    {"Dump Bubble Bottle and Turn on the Faucet", "+13-19 bubbles"},
    {"Put the Toy Boat on the Side of the Tub", "+4 crayon, -1 crew"},
    {"Cover the Ship in Bubbles", "Block fearsome giant squid, -13-20 bubbles"},
    {"Pull the Drain Plug", "-8 crew, -3 crayons, -17 bubbles, increase NC rate"},
    {"Open a New Bathtub Crayon Box", "+3 crayons"},
    {"Sing a Bathtime Tune", "+3 crayons, +16 bubbles, -2 crew"},
    {"Surround Bubbles with Crayons", "+5 crew, -6-16 bubbles, -2 crayons"},
  };

  private static ChoiceOption[] oldManPsychosisSpoilers() {
    Matcher matcher =
        ChoiceUtilities.DECISION_BUTTON_PATTERN.matcher(ChoiceManager.lastResponseText);

    String[][] buttons = new String[4][2];
    int i = 0;
    while (matcher.find()) {
      buttons[i][0] = matcher.group(1);
      buttons[i][1] = matcher.group(2);
      ++i;
    }

    // we need to return a string array with len=4 - even if there are buttons missing
    // the missing buttons are just "hidden" and thus the later buttons have the appropriate form
    // field
    // i.e. button 2 may be the first button.

    // As it turns out, I think all this cancels out and they could just be implemented as standard
    // choice adventures,
    // since the buttons are not actually randomized, they are consistent within the four choice
    // adventures that make up the 10 log entry non-combats.
    // Ah well.  Leavin' it here.
    ChoiceOption[] spoilers = new ChoiceOption[4];

    for (int j = 0; j < spoilers.length; j++) {
      for (String[] s : OLD_MAN_PSYCHOSIS_SPOILERS) {
        if (s[0].equals(buttons[j][1])) {
          spoilers[Integer.parseInt(buttons[j][0]) - 1] =
              new ChoiceOption(s[1]); // button 1 text should be in index 0, 2 -> 1, etc.
          break;
        }
      }
    }

    return spoilers;
  }
}
