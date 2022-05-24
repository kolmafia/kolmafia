package net.sourceforge.kolmafia.session;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AdventureResult.AdventureLongCountResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.EdServantData;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.Modifiers.Modifier;
import net.sourceforge.kolmafia.Modifiers.ModifierList;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.VYKEACompanionData;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.moods.HPRestoreItemList;
import net.sourceforge.kolmafia.moods.MPRestoreItemList;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.ArcadeRequest;
import net.sourceforge.kolmafia.request.BeachCombRequest;
import net.sourceforge.kolmafia.request.BeerPongRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest.Mushroom;
import net.sourceforge.kolmafia.request.CargoCultistShortsRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest.Companion;
import net.sourceforge.kolmafia.request.DeckOfEveryCardRequest;
import net.sourceforge.kolmafia.request.DecorateTentRequest;
import net.sourceforge.kolmafia.request.EatItemRequest;
import net.sourceforge.kolmafia.request.EdBaseRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FloristRequest;
import net.sourceforge.kolmafia.request.FloristRequest.Florist;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.GenieRequest;
import net.sourceforge.kolmafia.request.LatteRequest;
import net.sourceforge.kolmafia.request.MummeryRequest;
import net.sourceforge.kolmafia.request.PantogramRequest;
import net.sourceforge.kolmafia.request.PyramidRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.SaberRequest;
import net.sourceforge.kolmafia.request.SpaaaceRequest;
import net.sourceforge.kolmafia.request.SpelunkyRequest;
import net.sourceforge.kolmafia.request.SweetSynthesisRequest;
import net.sourceforge.kolmafia.request.TavernRequest;
import net.sourceforge.kolmafia.request.UmbrellaRequest;
import net.sourceforge.kolmafia.request.WildfireCampRequest;
import net.sourceforge.kolmafia.textui.command.EdPieceCommand;
import net.sourceforge.kolmafia.textui.command.SnowsuitCommand;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.ClanFortuneDecorator;
import net.sourceforge.kolmafia.webui.MemoriesDecorator;

public abstract class ChoiceAdventures {

  private static int abooPeakLevel = 0;

  public static final Pattern URL_IID_PATTERN = Pattern.compile("iid=(\\d+)");

  public static int extractIidFromURL(final String urlString) {
    Matcher matcher = URL_IID_PATTERN.matcher(urlString);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : -1;
  }

  public static final Pattern URL_QTY_PATTERN = Pattern.compile("qty=(\\d+)");

  public static int extractQtyFromURL(final String urlString) {
    Matcher matcher = URL_QTY_PATTERN.matcher(urlString);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : -1;
  }

  private static final String[] NO_ITEM_NAMES = new String[0];

  public static class Option {
    private final String name;
    private final int option;
    private final AdventureResult items[];

    public Option(final String name) {
      this(name, 0, NO_ITEM_NAMES);
    }

    public Option(final String name, final String... itemNames) {
      this(name, 0, itemNames);
    }

    public Option(final String name, final int option) {
      this(name, option, NO_ITEM_NAMES);
    }

    public Option(final String name, final int option, final String... itemNames) {
      this.name = name;
      this.option = option;

      int count = itemNames.length;
      this.items = new AdventureResult[count];

      for (int index = 0; index < count; ++index) {
        this.items[index] = ItemPool.get(ItemDatabase.getItemId(itemNames[index]));
      }
    }

    public String getName() {
      return this.name;
    }

    public int getOption() {
      return this.option;
    }

    public int getDecision(final int def) {
      return this.option == 0 ? def : this.option;
    }

    public AdventureResult[] getItems() {
      return this.items;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  public static class Spoilers {
    private final int choice;
    private final String name;
    private final Option[] options;

    public Spoilers(final int choice, final String name, final Option[] options) {
      assert options != null;
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

    public Option[] getOptions() {
      return this.options;
    }
  }

  // Here are the various maps from choice# -> choice object

  public static final Map<Integer, ChoiceAdventure> choiceToChoiceAdventure = new HashMap<>();
  public static final Map<Integer, ChoiceSpoiler> choiceToChoiceSpoiler = new HashMap<>();
  public static final Map<Integer, ChoiceCost> choiceToChoiceCost = new HashMap<>();

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
    private final Option[] options;

    // Derived fields
    private final String property;
    private final Spoilers spoilers;

    public ChoiceAdventure(
        final int choice, String zone, final String name, final Option... options) {
      this(choice, zone, name, 0, options);
    }

    public ChoiceAdventure(
        final int choice,
        final String zone,
        final String name,
        final int ordering,
        final Option... options) {
      super(choice, ordering);
      this.zone = zone;
      this.name = name;
      assert options != null;
      this.options = options;

      this.property = "whichchoice" + String.valueOf(choice);
      this.spoilers = new Spoilers(choice, name, options);
    }

    public String getZone() {
      return this.zone;
    }

    public String getName() {
      return this.name;
    }

    public Option[] getOptions() {
      return (this.options.length == 0)
          ? ChoiceAdventures.dynamicChoiceOptions(this.choice)
          : this.options;
    }

    public String getSetting() {
      return this.property;
    }

    public Spoilers getSpoilers() {
      return this.spoilers;
    }

    @Override
    protected void addToMap() {
      choiceToChoiceAdventure.put(this.choice, this);
    }
  }

  // A ChoiceSpoiler is a ChoiceAdventure that isn't user-configurable.
  // The zone is optional, since it doesn't appear in the choiceadv GUI.
  public static class ChoiceSpoiler extends ChoiceAdventure {
    public ChoiceSpoiler(final int choice, final String name, final Option... options) {
      super(choice, "Unsorted", name, options);
    }

    public ChoiceSpoiler(
        final int choice, final String zone, final String name, final Option... options) {
      super(choice, zone, name, options);
    }

    @Override
    protected void addToMap() {
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
      choiceToChoiceCost.put(this.choice, this);
    }
  }

  public static final Option findOption(final Option[] options, final int decision) {
    for (int i = 0; i < options.length; ++i) {
      Option opt = options[i];
      if (opt.getDecision(i + 1) == decision) {
        return opt;
      }
    }
    return null;
  }

  private static final Option SKIP_ADVENTURE = new Option("skip adventure");

  static {
    // Choice 1 is unknown

    // Denim Axes Examined
    new ChoiceSpoiler(
        2,
        "Palindome",
        // Option...
        new Option("denim axe", "denim axe"),
        new Option("skip adventure", "rubber axe"));
    // Denim Axes Examined
    new ChoiceCost(2, new Cost(1, ItemPool.get(ItemPool.RUBBER_AXE, -1)));

    // The Oracle Will See You Now
    new ChoiceSpoiler(
        3,
        "Teleportitis",
        // Option...
        SKIP_ADVENTURE,
        new Option("randomly sink 100 meat"),
        new Option("make plus sign usable"));

    // Finger-Lickin'... Death.
    new ChoiceAdventure(
        4,
        "Beach",
        "South of the Border",
        // Option...
        new Option("small meat boost"),
        new Option("try for poultrygeist", "poultrygeist"),
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
        new Option("fight the fairy queen"),
        SKIP_ADVENTURE);

    // Darker Than Dark
    new ChoiceSpoiler(
        6,
        "Gravy Barrow",
        // Option...
        new Option("get Beaten Up"),
        SKIP_ADVENTURE);

    // Choice 7 is How Depressing

    // On the Verge of a Dirge -> Self Explanatory
    new ChoiceSpoiler(
        8,
        "Gravy Barrow",
        // Option...
        new Option("enter the chamber"),
        new Option("enter the chamber"),
        new Option("enter the chamber"));

    // Wheel In the Sky Keep on Turning: Muscle Position
    new ChoiceSpoiler(
        9,
        "Castle Wheel",
        // Option...
        new Option("Turn to mysticality"),
        new Option("Turn to moxie"),
        new Option("Leave at muscle"));

    // Wheel In the Sky Keep on Turning: Mysticality Position
    new ChoiceSpoiler(
        10,
        "Castle Wheel",
        // Option...
        new Option("Turn to Map Quest"),
        new Option("Turn to muscle"),
        new Option("Leave at mysticality"));

    // Wheel In the Sky Keep on Turning: Map Quest Position
    new ChoiceSpoiler(
        11,
        "Castle Wheel",
        // Option...
        new Option("Turn to moxie"),
        new Option("Turn to mysticality"),
        new Option("Leave at map quest"));

    // Wheel In the Sky Keep on Turning: Moxie Position
    new ChoiceSpoiler(
        12,
        "Castle Wheel",
        // Option...
        new Option("Turn to muscle"),
        new Option("Turn to map quest"),
        new Option("Leave at moxie"));

    // Choice 13 is unknown

    // A Bard Day's Night
    new ChoiceAdventure(
        14,
        "Knob",
        "Cobb's Knob Harem",
        // Option...
        new Option("Knob goblin harem veil", "Knob goblin harem veil"),
        new Option("Knob goblin harem pants", "Knob goblin harem pants"),
        new Option("small meat boost"),
        new Option("complete the outfit"));

    // Yeti Nother Hippy
    new ChoiceAdventure(
        15,
        "McLarge",
        "eXtreme Slope",
        // Option...
        new Option("eXtreme mittens", "eXtreme mittens"),
        new Option("eXtreme scarf", "eXtreme scarf"),
        new Option("small meat boost"),
        new Option("complete the outfit"));

    // Saint Beernard
    new ChoiceAdventure(
        16,
        "McLarge",
        "eXtreme Slope",
        // Option...
        new Option("snowboarder pants", "snowboarder pants"),
        new Option("eXtreme scarf", "eXtreme scarf"),
        new Option("small meat boost"),
        new Option("complete the outfit"));

    // Generic Teen Comedy
    new ChoiceAdventure(
        17,
        "McLarge",
        "eXtreme Slope",
        // Option...
        new Option("eXtreme mittens", "eXtreme mittens"),
        new Option("snowboarder pants", "snowboarder pants"),
        new Option("small meat boost"),
        new Option("complete the outfit"));

    // A Flat Miner
    new ChoiceAdventure(
        18,
        "McLarge",
        "Itznotyerzitz Mine",
        // Option...
        new Option("miner's pants", "miner's pants"),
        new Option("7-Foot Dwarven mattock", "7-Foot Dwarven mattock"),
        new Option("small meat boost"),
        new Option("complete the outfit"));

    // 100% Legal
    new ChoiceAdventure(
        19,
        "McLarge",
        "Itznotyerzitz Mine",
        // Option...
        new Option("miner's helmet", "miner's helmet"),
        new Option("miner's pants", "miner's pants"),
        new Option("small meat boost"),
        new Option("complete the outfit"));

    // See You Next Fall
    new ChoiceAdventure(
        20,
        "McLarge",
        "Itznotyerzitz Mine",
        // Option...
        new Option("miner's helmet", "miner's helmet"),
        new Option("7-Foot Dwarven mattock", "7-Foot Dwarven mattock"),
        new Option("small meat boost"),
        new Option("complete the outfit"));

    // Under the Knife
    new ChoiceAdventure(
        21,
        "Town",
        "Sleazy Back Alley",
        // Option...
        new Option("switch genders"),
        SKIP_ADVENTURE);
    // Under the Knife
    new ChoiceCost(21, new Cost(1, new AdventureResult(AdventureResult.MEAT, -500)));

    // The Arrrbitrator
    new ChoiceAdventure(
        22,
        "Island",
        "Pirate's Cove",
        // Option...
        new Option("eyepatch", "eyepatch"),
        new Option("swashbuckling pants", "swashbuckling pants"),
        new Option("small meat boost"),
        new Option("complete the outfit"));

    // Barrie Me at Sea
    new ChoiceAdventure(
        23,
        "Island",
        "Pirate's Cove",
        // Option...
        new Option("stuffed shoulder parrot", "stuffed shoulder parrot"),
        new Option("swashbuckling pants", "swashbuckling pants"),
        new Option("small meat boost"),
        new Option("complete the outfit"));

    // Amatearrr Night
    new ChoiceAdventure(
        24,
        "Island",
        "Pirate's Cove",
        // Option...
        new Option("stuffed shoulder parrot", "stuffed shoulder parrot"),
        new Option("small meat boost"),
        new Option("eyepatch", "eyepatch"),
        new Option("complete the outfit"));

    // Ouch! You bump into a door!
    new ChoiceAdventure(
        25,
        "Dungeon",
        "Dungeon of Doom",
        // Option...
        new Option("magic lamp", "magic lamp"),
        new Option("dead mimic", "dead mimic"),
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
        new Option("muscle classes"),
        new Option("mysticality classes"),
        new Option("moxie classes"));

    // Footprints
    new ChoiceSpoiler(
        27,
        "Woods",
        "Spooky Forest",
        // Option...
        new Option(AscensionClass.SEAL_CLUBBER.toString()),
        new Option(AscensionClass.TURTLE_TAMER.toString()));

    // A Pair of Craters
    new ChoiceSpoiler(
        28,
        "Woods",
        "Spooky Forest",
        // Option...
        new Option(AscensionClass.PASTAMANCER.toString()),
        new Option(AscensionClass.SAUCEROR.toString()));

    // The Road Less Visible
    new ChoiceSpoiler(
        29,
        "Woods",
        "Spooky Forest",
        // Option...
        new Option(AscensionClass.DISCO_BANDIT.toString()),
        new Option(AscensionClass.ACCORDION_THIEF.toString()));

    // Choices 30 - 39 are unknown

    // The Effervescent Fray
    new ChoiceAdventure(
        40,
        "Rift",
        "Cola Wars",
        // Option...
        new Option("Cloaca-Cola fatigues", "Cloaca-Cola fatigues"),
        new Option("Dyspepsi-Cola shield", "Dyspepsi-Cola shield"),
        new Option("mysticality substats"));

    // Smells Like Team Spirit
    new ChoiceAdventure(
        41,
        "Rift",
        "Cola Wars",
        // Option...
        new Option("Dyspepsi-Cola fatigues", "Dyspepsi-Cola fatigues"),
        new Option("Cloaca-Cola helmet", "Cloaca-Cola helmet"),
        new Option("muscle substats"));

    // What is it Good For?
    new ChoiceAdventure(
        42,
        "Rift",
        "Cola Wars",
        // Option...
        new Option("Dyspepsi-Cola helmet", "Dyspepsi-Cola helmet"),
        new Option("Cloaca-Cola shield", "Cloaca-Cola shield"),
        new Option("moxie substats"));

    // Choices 43 - 44 are unknown

    // Maps and Legends
    new ChoiceSpoiler(
        45,
        "Woods",
        "Spooky Forest",
        // Option...
        new Option("Spooky Temple map", "Spooky Temple map"),
        SKIP_ADVENTURE,
        SKIP_ADVENTURE);

    // An Interesting Choice
    new ChoiceAdventure(
        46,
        "Woods",
        "Spooky Forest Vampire",
        // Option...
        new Option("moxie substats"),
        new Option("muscle substats"),
        new Option("vampire heart", "vampire heart"));

    // Have a Heart
    new ChoiceAdventure(
        47,
        "Woods",
        "Spooky Forest Vampire Hunter",
        // Option...
        new Option("bottle of used blood", "bottle of used blood"),
        new Option("skip adventure and keep vampire hearts", "vampire heart"));
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
        new Option("around the world", "around the world"),
        new Option("skip adventure", "Spanish fly"));
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
        new Option("muscle substats"),
        new Option("white picket fence", "white picket fence"),
        new Option("wedding cake, white rice 3x (+2x w/ rice bowl)", "piece of wedding cake"));

    // The Only Thing About Him is the Way That He Walks
    new ChoiceAdventure(
        74,
        "Woods",
        "Whitey's Grove",
        // Option...
        new Option("moxie substats"),
        new Option("boxed wine", "boxed wine"),
        new Option("mullet wig", "mullet wig"));

    // Rapido!
    new ChoiceAdventure(
        75,
        "Woods",
        "Whitey's Grove",
        // Option...
        new Option("mysticality substats"),
        new Option("white lightning", "white lightning"),
        new Option("white collar", "white collar"));

    // Junction in the Trunction
    new ChoiceAdventure(
        76,
        "Knob",
        "Knob Shaft",
        // Option...
        new Option("cardboard ore", "cardboard ore"),
        new Option("styrofoam ore", "styrofoam ore"),
        new Option("bubblewrap ore", "bubblewrap ore"));

    // History is Fun!
    new ChoiceSpoiler(
        86,
        "Haunted Library",
        // Option...
        new Option("Spookyraven Chapter 1"),
        new Option("Spookyraven Chapter 2"),
        new Option("Spookyraven Chapter 3"));

    // History is Fun!
    new ChoiceSpoiler(
        87,
        "Haunted Library",
        // Option...
        new Option("Spookyraven Chapter 4"),
        new Option("Spookyraven Chapter 5"),
        new Option("Spookyraven Chapter 6"));

    // Naughty, Naughty
    new ChoiceSpoiler(
        88,
        "Haunted Library",
        // Option...
        new Option("mysticality substats"),
        new Option("moxie substats"),
        new Option("Fettucini / Scarysauce"));
    new ChoiceSpoiler(
        89,
        "Haunted Gallery",
        // Option...
        new Option("Wolf Knight"),
        new Option("Snake Knight"),
        new Option("Dreams and Lights"),
        SKIP_ADVENTURE);

    // Curtains
    new ChoiceAdventure(
        90,
        "Manor2",
        "Haunted Ballroom",
        // Option...
        new Option("enter combat"),
        new Option("moxie substats"),
        SKIP_ADVENTURE);

    // Having a Medicine Ball
    new ChoiceAdventure(
        105,
        "Manor2",
        "Haunted Bathroom",
        // Option...
        new Option("mysticality substats"),
        new Option("other options"),
        new Option("guy made of bees"));

    // Strung-Up Quartet
    new ChoiceAdventure(
        106,
        "Manor2",
        "Haunted Ballroom",
        // Option...
        new Option("increase monster level"),
        new Option("decrease combat frequency"),
        new Option("increase item drops"),
        new Option("disable song"));

    // Bad Medicine is What You Need
    new ChoiceAdventure(
        107,
        "Manor2",
        "Haunted Bathroom",
        // Option...
        new Option("antique bottle of cough syrup", "antique bottle of cough syrup"),
        new Option("tube of hair oil", "tube of hair oil"),
        new Option("bottle of ultravitamins", "bottle of ultravitamins"),
        SKIP_ADVENTURE);

    // Aww, Craps
    new ChoiceAdventure(
        108,
        "Town",
        "Sleazy Back Alley",
        // Option...
        new Option("moxie substats"),
        new Option("meat and moxie"),
        new Option("random effect"),
        SKIP_ADVENTURE);

    // Dumpster Diving
    new ChoiceAdventure(
        109,
        "Town",
        "Sleazy Back Alley",
        // Option...
        new Option("enter combat"),
        new Option("meat and moxie"),
        new Option("Mad Train wine", "Mad Train wine"));

    // The Entertainer
    new ChoiceAdventure(
        110,
        "Town",
        "Sleazy Back Alley",
        // Option...
        new Option("moxie substats"),
        new Option("moxie and muscle"),
        new Option("small meat boost"),
        SKIP_ADVENTURE);

    // Malice in Chains
    new ChoiceAdventure(
        111,
        "Knob",
        "Outskirts of The Knob",
        // Option...
        new Option("muscle substats"),
        new Option("muscle substats"),
        new Option("enter combat"));

    // Please, Hammer
    new ChoiceAdventure(
        112,
        "Town",
        "Sleazy Back Alley",
        // Option...
        new Option("accept hammer quest"),
        new Option("reject quest"),
        new Option("muscle substats"));

    // Knob Goblin BBQ
    new ChoiceAdventure(
        113,
        "Knob",
        "Outskirts of The Knob",
        // Option...
        new Option("complete cake quest"),
        new Option("enter combat"),
        new Option("get a random item"));

    // The Baker's Dilemma
    new ChoiceAdventure(
        114,
        "Manor1",
        "Haunted Pantry",
        // Option...
        new Option("accept cake quest"),
        new Option("reject quest"),
        new Option("moxie and meat"));

    // Oh No, Hobo
    new ChoiceAdventure(
        115,
        "Manor1",
        "Haunted Pantry",
        // Option...
        new Option("enter combat"),
        new Option("Good Karma"),
        new Option("mysticality, moxie, and meat"));

    // The Singing Tree
    new ChoiceAdventure(
        116,
        "Manor1",
        "Haunted Pantry",
        // Option...
        new Option("mysticality substats"),
        new Option("moxie substats"),
        new Option("random effect"),
        SKIP_ADVENTURE);

    // Tresspasser
    new ChoiceAdventure(
        117,
        "Manor1",
        "Haunted Pantry",
        // Option...
        new Option("enter combat"),
        new Option("mysticality substats"),
        new Option("get a random item"));

    // When Rocks Attack
    new ChoiceAdventure(
        118,
        "Knob",
        "Outskirts of The Knob",
        // Option...
        new Option("accept unguent quest"),
        SKIP_ADVENTURE);

    // Choice 119 is Check It Out Now

    // Ennui is Wasted on the Young
    new ChoiceAdventure(
        120,
        "Knob",
        "Outskirts of The Knob",
        // Option...
        new Option("muscle and Pumped Up"),
        new Option("ice cold Sir Schlitz", "ice cold Sir Schlitz"),
        new Option("moxie and lemon", "lemon"),
        SKIP_ADVENTURE);

    // Choice 121 is Next Sunday, A.D.
    // Choice 122 is unknown

    // At Least It's Not Full Of Trash
    new ChoiceSpoiler(
        123,
        "Hidden Temple",
        // Option...
        new Option("lose HP"),
        new Option("Unlock Quest Puzzle"),
        new Option("lose HP"));

    // Choice 124 is unknown

    // No Visible Means of Support
    new ChoiceSpoiler(
        125,
        "Hidden Temple",
        // Option...
        new Option("lose HP"),
        new Option("lose HP"),
        new Option("Unlock Hidden City"));

    // Sun at Noon, Tan Us
    new ChoiceAdventure(
        126,
        "Plains",
        "Palindome",
        // Option...
        new Option("moxie"),
        new Option("chance of more moxie"),
        new Option("sunburned"));

    // No sir, away!  A papaya war is on!
    new ChoiceSpoiler(
        127,
        "Plains",
        "Palindome",
        // Option...
        new Option("3 papayas", "papaya"),
        new Option("trade 3 papayas for stats"),
        new Option("stats"));
    // No sir, away!  A papaya war is on!
    new ChoiceCost(127, new Cost(2, ItemPool.get(ItemPool.PAPAYA, -3)));

    // Choice 128 is unknown

    // Do Geese See God?
    new ChoiceSpoiler(
        129,
        "Plains",
        "Palindome",
        // Option...
        new Option("photograph of God", "photograph of God"),
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
        new Option("filthy corduroys", "filthy corduroys"),
        new Option("filthy knitted dread sack", "filthy knitted dread sack"),
        new Option("small meat boost"),
        new Option("complete the outfit"));

    // An Inconvenient Truth
    new ChoiceAdventure(
        137,
        "Island",
        "Hippy Camp",
        // Option...
        new Option("filthy knitted dread sack", "filthy knitted dread sack"),
        new Option("filthy corduroys", "filthy corduroys"),
        new Option("small meat boost"),
        new Option("complete the outfit"));

    // Purple Hazers
    new ChoiceAdventure(
        138,
        "Island",
        "Frat House",
        // Option...
        new Option("Orcish cargo shorts", "Orcish cargo shorts"),
        new Option("Orcish baseball cap", "Orcish baseball cap"),
        new Option("Orcish frat-paddle", "Orcish frat-paddle"),
        new Option("complete the outfit"));

    // Bait and Switch
    new ChoiceAdventure(
        139,
        "IsleWar",
        "War Hippies",
        // Option...
        new Option("muscle substats"),
        new Option("ferret bait", "ferret bait"),
        new Option("enter combat"));

    // The Thin Tie-Dyed Line
    new ChoiceAdventure(
        140,
        "IsleWar",
        "War Hippies",
        // Option...
        new Option("water pipe bombs", "water pipe bomb"),
        new Option("moxie substats"),
        new Option("enter combat"));

    // Blockin' Out the Scenery
    new ChoiceAdventure(
        141,
        "IsleWar",
        "War Hippies",
        // Option...
        new Option("mysticality substats"),
        new Option("get some hippy food"),
        new Option("waste a turn"));

    // Blockin' Out the Scenery
    new ChoiceAdventure(
        142,
        "IsleWar",
        "War Hippies",
        // Option...
        new Option("mysticality substats"),
        new Option("get some hippy food"),
        new Option("start the war"));

    // Catching Some Zetas
    new ChoiceAdventure(
        143,
        "IsleWar",
        "War Fraternity",
        // Option...
        new Option("muscle substats"),
        new Option("sake bombs", "sake bomb"),
        new Option("enter combat"));

    // One Less Room Than In That Movie
    new ChoiceAdventure(
        144,
        "IsleWar",
        "War Fraternity",
        // Option...
        new Option("moxie substats"),
        new Option("beer bombs", "beer bomb"),
        new Option("enter combat"));

    // Fratacombs
    new ChoiceAdventure(
        145,
        "IsleWar",
        "War Fraternity",
        // Option...
        new Option("muscle substats"),
        new Option("get some frat food"),
        new Option("waste a turn"));

    // Fratacombs
    new ChoiceAdventure(
        146,
        "IsleWar",
        "War Fraternity",
        // Option...
        new Option("muscle substats"),
        new Option("get some frat food"),
        new Option("start the war"));

    // Cornered!
    new ChoiceAdventure(
        147,
        "Farm",
        "McMillicancuddy's Barn",
        // Option...
        new Option("Open The Granary (meat)"),
        new Option("Open The Bog (stench)"),
        new Option("Open The Pond (cold)"));

    // Cornered Again!
    new ChoiceAdventure(
        148,
        "Farm",
        "McMillicancuddy's Barn",
        // Option...
        new Option("Open The Back 40 (hot)"),
        new Option("Open The Family Plot (spooky)"));

    // How Many Corners Does this Stupid Barn Have!?
    new ChoiceAdventure(
        149,
        "Farm",
        "McMillicancuddy's Barn",
        // Option...
        new Option("Open The Shady Thicket (booze)"),
        new Option("Open The Other Back 40 (sleaze)"));

    // Choice 150 is Another Adventure About BorderTown

    // Adventurer, $1.99
    new ChoiceAdventure(
        151,
        "Plains",
        "Fun House",
        // Option...
        new Option("fight the clownlord"),
        SKIP_ADVENTURE);

    // Lurking at the Threshold
    new ChoiceSpoiler(
        152,
        "Plains",
        "Fun House",
        // Option...
        new Option("fight the clownlord"),
        SKIP_ADVENTURE);

    // Turn Your Head and Coffin
    new ChoiceAdventure(
        153,
        "Cyrpt",
        "Defiled Alcove",
        // Option...
        new Option("muscle substats"),
        new Option("small meat boost"),
        new Option("half-rotten brain", "half-rotten brain"),
        SKIP_ADVENTURE);

    // Choice 154 used to be Doublewide

    // Skull, Skull, Skull
    new ChoiceAdventure(
        155,
        "Cyrpt",
        "Defiled Nook",
        // Option...
        new Option("moxie substats"),
        new Option("small meat boost"),
        new Option("rusty bonesaw", "rusty bonesaw"),
        new Option("debonair deboner", "debonair deboner"),
        SKIP_ADVENTURE);

    // Choice 156 used to be Pileup

    // Urning Your Keep
    new ChoiceAdventure(
        157,
        "Cyrpt",
        "Defiled Niche",
        // Option...
        new Option("mysticality substats"),
        new Option("plus-sized phylactery", "plus-sized phylactery"),
        new Option("small meat boost"),
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
        new Option("Open Goatlet"),
        SKIP_ADVENTURE);

    // Melvil Dewey Would Be Ashamed
    new ChoiceAdventure(
        163,
        "Manor1",
        "Haunted Library",
        // Option...
        new Option("Necrotelicomnicon", "Necrotelicomnicon"),
        new Option("Cookbook of the Damned", "Cookbook of the Damned"),
        new Option("Sinful Desires", "Sinful Desires"),
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
        new Option("muscle substats"),
        new Option("MP & Spirit of Alph"),
        new Option("enter combat"));

    // Beyond Any Measure
    new ChoiceAdventure(
        165,
        "Wormwood",
        "Pleasure Dome",
        // Option...
        new Option("Rat-Faced -> Night Vision"),
        new Option("Bats in the Belfry -> Good with the Ladies"),
        new Option("mysticality substats"),
        SKIP_ADVENTURE);

    // Death is a Boat
    new ChoiceAdventure(
        166,
        "Wormwood",
        "Pleasure Dome",
        // Option...
        new Option("No Vertigo -> S.T.L.T.", "S.T.L.T."),
        new Option("moxie substats"),
        new Option("Unusual Fashion Sense -> albatross necklace", "albatross necklace"));

    // It's a Fixer-Upper
    new ChoiceAdventure(
        167,
        "Wormwood",
        "Moulder Mansion",
        // Option...
        new Option("enter combat"),
        new Option("mysticality substats"),
        new Option("HP & MP & Bats in the Belfry"));

    // Midst the Pallor of the Parlor
    new ChoiceAdventure(
        168,
        "Wormwood",
        "Moulder Mansion",
        // Option...
        new Option("moxie substats"),
        new Option("Spirit of Alph -> Feelin' Philosophical"),
        new Option("Rat-Faced -> Unusual Fashion Sense"));

    // A Few Chintz Curtains, Some Throw Pillows, It
    new ChoiceAdventure(
        169,
        "Wormwood",
        "Moulder Mansion",
        // Option...
        new Option("Night Vision -> flask of Amontillado", "flask of Amontillado"),
        new Option("muscle substats"),
        new Option("Dancing Prowess -> fancy ball mask", "fancy ball mask"));

    // La Vie Boheme
    new ChoiceAdventure(
        170,
        "Wormwood",
        "Rogue Windmill",
        // Option...
        new Option("HP & Rat-Faced"),
        new Option("enter combat"),
        new Option("moxie substats"));

    // Backstage at the Rogue Windmill
    new ChoiceAdventure(
        171,
        "Wormwood",
        "Rogue Windmill",
        // Option...
        new Option("Bats in the Belfry -> No Vertigo"),
        new Option("muscle substats"),
        new Option("Spirit of Alph -> Dancing Prowess"));

    // Up in the Hippo Room
    new ChoiceAdventure(
        172,
        "Wormwood",
        "Rogue Windmill",
        // Option...
        new Option("Good with the Ladies -> Can-Can skirt", "Can-Can skirt"),
        new Option("Feelin' Philosophical -> not-a-pipe", "not-a-pipe"),
        new Option("mysticality substats"));

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
        new Option("bronze breastplate", "bronze breastplate"),
        SKIP_ADVENTURE);

    // Choice 179 is unknown

    // A Pre-War Dresser Drawer, Pa!
    new ChoiceAdventure(
        180,
        "Plains",
        "Palindome Shirt",
        // Option...
        new Option("Ye Olde Navy Fleece", "Ye Olde Navy Fleece"),
        SKIP_ADVENTURE);

    // Chieftain of the Flies
    new ChoiceAdventure(
        181,
        "Island",
        "Frat House (Stone Age)",
        // Option...
        new Option("around the world", "around the world"),
        new Option("skip adventure", "Spanish fly"));
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
        new Option("enter combat"),
        new Option("Penultimate Fantasy chest", "Penultimate Fantasy chest"),
        new Option("stats"),
        new Option("model airship and combat", "model airship"),
        new Option("model airship and chest", "model airship"),
        new Option("model airship and stats", "model airship"));

    // That Explains All The Eyepatches
    // Dynamically calculate options based on mainstat
    new ChoiceAdventure(184, "Island", "Barrrney's Barrr");

    // Yes, You're a Rock Starrr
    new ChoiceAdventure(185, "Island", "Barrrney's Barrr");

    // A Test of Testarrrsterone
    new ChoiceAdventure(
        186,
        "Island",
        "Barrrney's Barrr",
        // Option...
        new Option("stats"),
        new Option("drunkenness and stats"),
        new Option("moxie"));

    // Choice 187 is Arrr You Man Enough?

    // The Infiltrationist
    new ChoiceAdventure(
        188,
        "Item-Driven",
        "Frathouse Blueprints",
        // Option...
        new Option("frat boy ensemble"),
        new Option("mullet wig and briefcase"),
        new Option("frilly skirt and hot wings"));

    //  O Cap'm, My Cap'm
    new ChoiceCost(189, new Cost(1, new AdventureResult(AdventureResult.MEAT, -977)));

    // Choice 190 is unknown

    // Chatterboxing
    new ChoiceAdventure(
        191,
        "Island",
        "F'c'le",
        // Option...
        new Option("moxie substats"),
        new Option("use valuable trinket to banish, or lose hp"),
        new Option("muscle substats"),
        new Option("mysticality substats"),
        new Option("use valuable trinket to banish, or moxie"),
        new Option("use valuable trinket to banish, or muscle"),
        new Option("use valuable trinket to banish, or mysticality"),
        new Option("use valuable trinket to banish, or mainstat"));
    new ChoiceCost(191, new Cost(2, ItemPool.get(ItemPool.VALUABLE_TRINKET, -1)));

    // Choice 192 is unknown
    // Choice 193 is Modular, Dude

    // Somewhat Higher and Mostly Dry
    new ChoiceAdventure(
        197,
        "Hobopolis",
        "A Maze of Sewer Tunnels",
        // Option...
        new Option("take the tunnel"),
        new Option("sewer gator"),
        new Option("turn the valve"));

    // Disgustin' Junction
    new ChoiceAdventure(
        198,
        "Hobopolis",
        "A Maze of Sewer Tunnels",
        // Option...
        new Option("take the tunnel"),
        new Option("giant zombie goldfish"),
        new Option("open the grate"));

    // The Former or the Ladder
    new ChoiceAdventure(
        199,
        "Hobopolis",
        "A Maze of Sewer Tunnels",
        // Option...
        new Option("take the tunnel"),
        new Option("C. H. U. M."),
        new Option("head down the ladder"));

    // Enter The Hoboverlord
    new ChoiceAdventure(
        200,
        "Hobopolis",
        "Hobopolis Town Square",
        // Option...
        new Option("enter combat with Hodgman"),
        SKIP_ADVENTURE);

    // Home, Home in the Range
    new ChoiceAdventure(
        201,
        "Hobopolis",
        "Burnbarrel Blvd.",
        // Option...
        new Option("enter combat with Ol' Scratch"),
        SKIP_ADVENTURE);

    // Bumpity Bump Bump
    new ChoiceAdventure(
        202,
        "Hobopolis",
        "Exposure Esplanade",
        // Option...
        new Option("enter combat with Frosty"),
        SKIP_ADVENTURE);

    // Deep Enough to Dive
    new ChoiceAdventure(
        203,
        "Hobopolis",
        "The Heap",
        // Option...
        new Option("enter combat with Oscus"),
        SKIP_ADVENTURE);

    // Welcome To You!
    new ChoiceAdventure(
        204,
        "Hobopolis",
        "The Ancient Hobo Burial Ground",
        // Option...
        new Option("enter combat with Zombo"),
        SKIP_ADVENTURE);

    // Van, Damn
    new ChoiceAdventure(
        205,
        "Hobopolis",
        "The Purple Light District",
        // Option...
        new Option("enter combat with Chester"),
        SKIP_ADVENTURE);

    // Getting Tired
    new ChoiceAdventure(
        206,
        "Hobopolis",
        "Burnbarrel Blvd.",
        // Option...
        new Option("start tirevalanche"),
        new Option("add tire to stack"),
        SKIP_ADVENTURE);

    // Hot Dog! I Mean... Door!
    new ChoiceAdventure(
        207,
        "Hobopolis",
        "Burnbarrel Blvd.",
        // Option...
        new Option("increase hot hobos & get clan meat"),
        SKIP_ADVENTURE);

    // Ah, So That's Where They've All Gone
    new ChoiceAdventure(
        208,
        "Hobopolis",
        "The Ancient Hobo Burial Ground",
        // Option...
        new Option("increase spooky hobos & decrease stench"),
        SKIP_ADVENTURE);

    // Choice 209 is Timbarrrr!
    // Choice 210 is Stumped

    // Despite All Your Rage
    new ChoiceAdventure(
        211,
        "Hobopolis",
        "A Maze of Sewer Tunnels",
        // Option...
        new Option("gnaw through the bars"));

    // Choice 212 is also Despite All Your Rage, apparently after you've already
    // tried to wait for rescue?
    new ChoiceAdventure(
        212,
        "Hobopolis",
        "A Maze of Sewer Tunnels",
        // Option...
        new Option("gnaw through the bars"));

    // Piping Hot
    new ChoiceAdventure(
        213,
        "Hobopolis",
        "Burnbarrel Blvd.",
        // Option...
        new Option("increase sleaze hobos & decrease heat"),
        SKIP_ADVENTURE);

    // You vs. The Volcano
    new ChoiceAdventure(
        214,
        "Hobopolis",
        "The Heap",
        // Option...
        new Option("decrease stench hobos & increase stench"),
        SKIP_ADVENTURE);

    // Piping Cold
    new ChoiceAdventure(
        215,
        "Hobopolis",
        "Exposure Esplanade",
        // Option...
        new Option("decrease heat"),
        new Option("decrease sleaze hobos"),
        new Option("increase number of icicles"));

    // The Compostal Service
    new ChoiceAdventure(
        216,
        "Hobopolis",
        "The Heap",
        // Option...
        new Option("decrease stench & spooky"),
        SKIP_ADVENTURE);

    // There Goes Fritz!
    new ChoiceAdventure(
        217,
        "Hobopolis",
        "Exposure Esplanade",
        // Option...
        new Option("yodel a little"),
        new Option("yodel a lot"),
        new Option("yodel your heart out"));

    // I Refuse!
    new ChoiceAdventure(
        218,
        "Hobopolis",
        "The Heap",
        // Option...
        new Option("explore the junkpile"),
        SKIP_ADVENTURE);

    // The Furtivity of My City
    new ChoiceAdventure(
        219,
        "Hobopolis",
        "The Purple Light District",
        // Option...
        new Option("fight sleaze hobo"),
        new Option("increase stench"),
        new Option("increase sleaze hobos & get clan meat"));

    // Returning to the Tomb
    new ChoiceAdventure(
        220,
        "Hobopolis",
        "The Ancient Hobo Burial Ground",
        // Option...
        new Option("increase spooky hobos & get clan meat"),
        SKIP_ADVENTURE);

    // A Chiller Night
    new ChoiceAdventure(
        221,
        "Hobopolis",
        "The Ancient Hobo Burial Ground",
        // Option...
        new Option("study the dance moves"),
        new Option("dance with hobo zombies"),
        SKIP_ADVENTURE);

    // A Chiller Night (2)
    new ChoiceAdventure(
        222,
        "Hobopolis",
        "The Ancient Hobo Burial Ground",
        // Option...
        new Option("dance with hobo zombies"),
        SKIP_ADVENTURE);

    // Getting Clubbed
    new ChoiceAdventure(
        223,
        "Hobopolis",
        "The Purple Light District",
        // Option...
        new Option("try to get inside"),
        new Option("try to bamboozle the crowd"),
        new Option("try to flimflam the crowd"));

    // Exclusive!
    new ChoiceAdventure(
        224,
        "Hobopolis",
        "The Purple Light District",
        // Option...
        new Option("fight sleaze hobo"),
        new Option("start barfight"),
        new Option("gain stats"));

    // Attention -- A Tent!
    new ChoiceAdventure(
        225,
        "Hobopolis",
        "Hobopolis Town Square",
        // Option...
        new Option("perform on stage"),
        new Option("join the crowd"),
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
        new Option("hobo code binder", "hobo code binder"),
        SKIP_ADVENTURE);
    // Mind Yer Binder
    new ChoiceCost(230, new Cost(1, ItemPool.get(ItemPool.HOBO_NICKEL, -30)));

    // Choices 231-271 are subchoices of Choice 272

    // Food, Glorious Food
    new ChoiceSpoiler(
        235,
        "Hobopolis Marketplace",
        // Option...
        new Option("muscle food"),
        new Option("mysticality food"),
        new Option("moxie food"));

    // Booze, Glorious Booze
    new ChoiceSpoiler(
        240,
        "Hobopolis Marketplace",
        // Option...
        new Option("muscle booze"),
        new Option("mysticality booze"),
        new Option("moxie booze"));

    // The Guy Who Carves Driftwood Animals
    new ChoiceCost(247, new Cost(1, ItemPool.get(ItemPool.HOBO_NICKEL, -10)));

    // A Hattery
    new ChoiceSpoiler(
        250,
        "Hobopolis Marketplace",
        // Option...
        new Option("crumpled felt fedora", "crumpled felt fedora"),
        new Option("battered old top-hat", "battered old top-hat"),
        new Option("shapeless wide-brimmed hat", "shapeless wide-brimmed hat"));
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
        new Option("mostly rat-hide leggings", "mostly rat-hide leggings"),
        new Option("hobo dungarees", "hobo dungarees"),
        new Option("old patched suit-pants", "old patched suit-pants"));
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
        new Option("old soft shoes", "old soft shoes"),
        new Option("hobo stogie", "hobo stogie"),
        new Option("rope with some soap on it", "rope with some soap on it"));
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
        new Option("sharpened hubcap", "sharpened hubcap"),
        new Option("very large caltrop", "very large caltrop"),
        new Option("The Six-Pack of Pain", "The Six-Pack of Pain"));
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
        new Option("hobo monkey"),
        new Option("stats"),
        new Option("enter combat"));

    // Everybody's Got Something To Hide
    new ChoiceCost(261, new Cost(1, ItemPool.get(ItemPool.HOBO_NICKEL, -1000)));

    // Tanning Salon
    new ChoiceSpoiler(
        264,
        "Hobopolis Marketplace",
        // Option...
        new Option("20 adv of +50% moxie"),
        new Option("20 adv of +50% mysticality"));
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
        new Option("20 adv of +5 spooky resistance"),
        new Option("20 adv of +5 sleaze resistance"));
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
        new Option("20 adv of +5 stench resistance"),
        new Option("20 adv of +50% muscle"));
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
        new Option("enter marketplace"),
        SKIP_ADVENTURE);

    // Piping Cold
    new ChoiceAdventure(
        273,
        "Hobopolis",
        "Exposure Esplanade",
        // Option...
        new Option("frozen banquet"),
        new Option("increase cold hobos & get clan meat"),
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
        new Option("3 adventures"),
        new Option("12 adventures"),
        new Option("15 adventures"));

    // Welcome Back!
    new ChoiceSpoiler(
        277,
        "Gong",
        // Option...
        new Option("finish journey"),
        new Option("also finish journey"));

    // Enter the Roach
    new ChoiceSpoiler(
        278,
        "Gong",
        // Option...
        new Option("muscle substats"),
        new Option("mysticality substats"),
        new Option("moxie substats"));

    // It's Nukyuhlur - the 'S' is Silent.
    new ChoiceSpoiler(
        279,
        "Gong",
        // Option...
        new Option("moxie substats"),
        new Option("muscle substats"),
        new Option("gain MP"));

    // Eek! Eek!
    new ChoiceSpoiler(
        280,
        "Gong",
        // Option...
        new Option("mysticality substats"),
        new Option("muscle substats"),
        new Option("gain MP"));

    // A Meta-Metamorphosis
    new ChoiceSpoiler(
        281,
        "Gong",
        // Option...
        new Option("moxie substats"),
        new Option("mysticality substats"),
        new Option("gain MP"));

    // You've Got Wings, But No Wingman
    new ChoiceSpoiler(
        282,
        "Gong",
        // Option...
        new Option("+30% muscle"),
        new Option("+10% all stats"),
        new Option("+30 ML"));

    // Time Enough at Last!
    new ChoiceSpoiler(
        283,
        "Gong",
        // Option...
        new Option("+30% muscle"),
        new Option("+10% all stats"),
        new Option("+50% item drops"));

    // Scavenger Is Your Middle Name
    new ChoiceSpoiler(
        284,
        "Gong",
        // Option...
        new Option("+30% muscle"),
        new Option("+50% item drops"),
        new Option("+30 ML"));

    // Bugging Out
    new ChoiceSpoiler(
        285,
        "Gong",
        // Option...
        new Option("+30% mysticality"),
        new Option("+30 ML"),
        new Option("+10% all stats"));

    // A Sweeping Generalization
    new ChoiceSpoiler(
        286,
        "Gong",
        // Option...
        new Option("+50% item drops"),
        new Option("+10% all stats"),
        new Option("+30% mysticality"));

    // In the Frigid Aire
    new ChoiceSpoiler(
        287,
        "Gong",
        // Option...
        new Option("+30 ML"),
        new Option("+30% mysticality"),
        new Option("+50% item drops"));

    // Our House
    new ChoiceSpoiler(
        288,
        "Gong",
        // Option...
        new Option("+30 ML"),
        new Option("+30% moxie"),
        new Option("+10% all stats"));

    // Workin' For The Man
    new ChoiceSpoiler(
        289,
        "Gong",
        // Option...
        new Option("+30 ML"),
        new Option("+30% moxie"),
        new Option("+50% item drops"));

    // The World's Not Fair
    new ChoiceSpoiler(
        290,
        "Gong",
        // Option...
        new Option("+30% moxie"),
        new Option("+10% all stats"),
        new Option("+50% item drops"));

    // A Tight Squeeze
    new ChoiceAdventure(
        291,
        "Hobopolis",
        "Burnbarrel Blvd.",
        // Option...
        new Option("jar of squeeze", "jar of squeeze"),
        SKIP_ADVENTURE);
    // A Tight Squeeze - jar of squeeze
    new ChoiceCost(291, new Cost(1, ItemPool.get(ItemPool.HOBO_NICKEL, -5)));

    // Cold Comfort
    new ChoiceAdventure(
        292,
        "Hobopolis",
        "Exposure Esplanade",
        // Option...
        new Option("bowl of fishysoisse", "bowl of fishysoisse"),
        SKIP_ADVENTURE);
    // Cold Comfort - bowl of fishysoisse
    new ChoiceCost(292, new Cost(1, ItemPool.get(ItemPool.HOBO_NICKEL, -5)));

    // Flowers for You
    new ChoiceAdventure(
        293,
        "Hobopolis",
        "The Ancient Hobo Burial Ground",
        // Option...
        new Option("deadly lampshade", "deadly lampshade"),
        SKIP_ADVENTURE);
    // Flowers for You - deadly lampshade
    new ChoiceCost(293, new Cost(1, ItemPool.get(ItemPool.HOBO_NICKEL, -5)));

    // Maybe It's a Sexy Snake!
    new ChoiceAdventure(
        294,
        "Hobopolis",
        "The Purple Light District",
        // Option...
        new Option("lewd playing card", "lewd playing card"),
        SKIP_ADVENTURE);
    // Maybe It's a Sexy Snake! - lewd playing card
    new ChoiceCost(294, new Cost(1, ItemPool.get(ItemPool.HOBO_NICKEL, -5)));

    // Juicy!
    new ChoiceAdventure(
        295,
        "Hobopolis",
        "The Heap",
        // Option...
        new Option("concentrated garbage juice", "concentrated garbage juice"),
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
        new Option("mushrooms"),
        new Option("fairy gravy boat", "fairy gravy boat"),
        SKIP_ADVENTURE);

    // In the Shade
    new ChoiceAdventure(
        298,
        "The Sea",
        "An Octopus's Garden",
        // Option...
        new Option("plant seeds"),
        SKIP_ADVENTURE);

    // Down at the Hatch
    new ChoiceAdventure(
        299,
        "The Sea",
        "The Wreck of the Edgar Fitzsimmons",
        // Option...
        new Option("release creatures"),
        SKIP_ADVENTURE,
        new Option("unlock tarnished luggage key adventure"));

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
        new Option("bubbling tempura batter", "bubbling tempura batter"),
        SKIP_ADVENTURE);
    // A Vent Horizon
    new ChoiceCost(304, new Cost(1, new AdventureLongCountResult(AdventureResult.MP, -200)));

    // There is Sauce at the Bottom of the Ocean
    new ChoiceAdventure(
        305,
        "The Sea",
        "The Marinara Trench",
        // Option...
        new Option("globe of Deep Sauce", "globe of Deep Sauce"),
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
        new Option("seaode", "seaode"),
        SKIP_ADVENTURE);

    // The Economist of Scales
    new ChoiceAdventure(
        310,
        "The Sea",
        "Madness Reef",
        // Option...
        new Option("get 1 rough fish scale", 1, "rough fish scale"),
        new Option("get 1 pristine fish scale", 2, "pristine fish scale"),
        new Option("get multiple rough fish scales", 4, "rough fish scale"),
        new Option("get multiple pristine fish scales", 5, "pristine fish scale"),
        new Option("skip adventure", 6));
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
        new Option("The Economist of Scales"),
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
        new Option("enter combat with Mother Slime"),
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
        new Option("stats and pool skill"),
        new Option("cube of billiard chalk", "cube of billiard chalk"));

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
        new Option("+1 rusty -> slime-covered item conversion"),
        new Option("raise area ML"),
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
        new Option("swim upwards"),
        new Option("swim in circles"),
        new Option("swim downwards"));

    // Soupercharged
    new ChoiceAdventure(
        350,
        "Memories",
        "The Primordial Soup",
        // Option...
        new Option("Fight Cyrus"),
        SKIP_ADVENTURE);

    // Choice 351 is Beginner's Luck

    // Savior Faire
    new ChoiceAdventure(
        352,
        "Memories",
        "Seaside Megalopolis",
        // Option...
        new Option("Moxie -> Bad Reception Down Here"),
        new Option("Muscle -> A Diseased Procurer"),
        new Option("Mysticality -> Give it a Shot"));

    // Bad Reception Down Here
    new ChoiceAdventure(
        353,
        "Memories",
        "Seaside Megalopolis",
        // Option...
        new Option("Indigo Party Invitation", "Indigo Party Invitation"),
        new Option("Violet Hunt Invitation", "Violet Hunt Invitation"));

    // You Can Never Be Too Rich or Too in the Future
    new ChoiceAdventure(
        354,
        "Memories",
        "Seaside Megalopolis",
        // Option...
        new Option("Moxie"),
        new Option("Serenity"));

    // I'm on the Hunt, I'm After You
    new ChoiceAdventure(
        355,
        "Memories",
        "Seaside Megalopolis",
        // Option...
        new Option("Stats"),
        new Option("Phairly Pheromonal"));

    // A Diseased Procurer
    new ChoiceAdventure(
        356,
        "Memories",
        "Seaside Megalopolis",
        // Option...
        new Option("Blue Milk Club Card", "Blue Milk Club Card"),
        new Option("Mecha Mayhem Club Card", "Mecha Mayhem Club Card"));

    // Painful, Circuitous Logic
    new ChoiceAdventure(
        357,
        "Memories",
        "Seaside Megalopolis",
        // Option...
        new Option("Muscle"),
        new Option("Nano-juiced"));

    // Brings All the Boys to the Blue Yard
    new ChoiceAdventure(
        358,
        "Memories",
        "Seaside Megalopolis",
        // Option...
        new Option("Stats"),
        new Option("Dance Interpreter"));

    // Choice 359 is unknown

    // Cavern Entrance
    new ChoiceAdventure(
        360,
        "Memories",
        "Jungles: Wumpus Cave",
        // Option...
        new Option("skip adventure", 2));

    // Give it a Shot
    new ChoiceAdventure(
        361,
        "Memories",
        "Seaside Megalopolis",
        // Option...
        new Option("'Smuggler Shot First' Button", "'Smuggler Shot First' Button"),
        new Option("Spacefleet Communicator Badge", "Spacefleet Communicator Badge"));

    // A Bridge Too Far
    new ChoiceAdventure(
        362,
        "Memories",
        "Seaside Megalopolis",
        // Option...
        new Option("Stats"),
        new Option("Meatwise"));

    // Does This Bug You? Does This Bug You?
    new ChoiceAdventure(
        363,
        "Memories",
        "Seaside Megalopolis",
        // Option...
        new Option("Mysticality"),
        new Option("In the Saucestream"));

    // 451 Degrees! Burning Down the House!
    new ChoiceAdventure(
        364,
        "Memories",
        "Seaside Megalopolis",
        // Option...
        new Option("Moxie"),
        new Option("Supreme Being Glossary", "Supreme Being Glossary"),
        new Option("Muscle"));

    // None Shall Pass
    new ChoiceAdventure(
        365,
        "Memories",
        "Seaside Megalopolis",
        // Option...
        new Option("Muscle"),
        new Option("multi-pass", "multi-pass"));

    // Entrance to the Forgotten City
    new ChoiceAdventure(
        366,
        "Memories",
        "Jungles: Forgotten City",
        // Option...
        new Option("skip adventure", 2));

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
        new Option("Enter the Temple"),
        new Option("leave"));

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
        new Option("1 of each marble -> 32768 Meat"),
        SKIP_ADVENTURE);

    // Choice 394 is Hellevator Music
    // Choice 395 is Rumble On

    // Woolly Scaly Bully
    new ChoiceAdventure(
        396,
        "The Sea",
        "Mer-kin Elementary School",
        // Option...
        new Option("lose HP"),
        new Option("lose HP"),
        new Option("unlock janitor's closet"));

    // Bored of Education
    new ChoiceAdventure(
        397,
        "The Sea",
        "Mer-kin Elementary School",
        // Option...
        new Option("lose HP"),
        new Option("unlock the bathrooms"),
        new Option("lose HP"));

    // A Mer-kin Graffiti
    new ChoiceAdventure(
        398,
        "The Sea",
        "Mer-kin Elementary School",
        // Option...
        new Option("unlock teacher's lounge"),
        new Option("lose HP"),
        new Option("lose HP"));

    // The Case of the Closet
    new ChoiceAdventure(
        399,
        "The Sea",
        "Mer-kin Elementary School",
        // Option...
        new Option("fight a Mer-kin monitor"),
        new Option("Mer-kin sawdust", "Mer-kin sawdust"));

    // No Rest for the Room
    new ChoiceAdventure(
        400,
        "The Sea",
        "Mer-kin Elementary School",
        // Option...
        new Option("fight a Mer-kin teacher"),
        new Option("Mer-kin cancerstick", "Mer-kin cancerstick"));

    // Raising Cane
    new ChoiceAdventure(
        401,
        "The Sea",
        "Mer-kin Elementary School",
        // Option...
        new Option("fight a Mer-kin punisher"),
        new Option("Mer-kin wordquiz", "Mer-kin wordquiz"));

    // Don't Hold a Grudge
    new ChoiceAdventure(
        402,
        "Manor2",
        "Haunted Bathroom",
        // Option...
        new Option("muscle substats"),
        new Option("mysticality substats"),
        new Option("moxie substats"));

    // Picking Sides
    new ChoiceAdventure(
        403,
        "The Sea",
        "Skate Park",
        // Option...
        new Option("skate blade", "skate blade"),
        new Option("brand new key", "brand new key"));

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
        "RabbitHole",
        "A Moment of Reflection",
        // Option...
        new Option("Seal Clubber/Pastamancer/custard"),
        new Option("Accordion Thief/Sauceror/comfit"),
        new Option("Turtle Tamer/Disco Bandit/croqueteer"),
        new Option("Ittah bittah hookah"),
        new Option("Chessboard"),
        new Option("nothing"));

    // Choice 443 is Chess Puzzle

    // Choice 444 is The Field of Strawberries (Seal Clubber)
    new ChoiceAdventure(
        444,
        "RabbitHole",
        "Reflection of Map (Seal Clubber)",
        // Option...
        new Option("walrus ice cream", "walrus ice cream"),
        new Option("yellow matter custard", "yellow matter custard"));

    // Choice 445 is The Field of Strawberries (Pastamancer)
    new ChoiceAdventure(
        445,
        "RabbitHole",
        "Reflection of Map (Pastamancer)",
        // Option...
        new Option("eggman noodles", "eggman noodles"),
        new Option("yellow matter custard", "yellow matter custard"));

    // Choice 446 is The Caucus Racetrack (Accordion Thief)
    new ChoiceAdventure(
        446,
        "RabbitHole",
        "Reflection of Map (Accordion Thief)",
        // Option...
        new Option("missing wine", "missing wine"),
        new Option("delicious comfit?", "delicious comfit?"));

    // Choice 447 is The Caucus Racetrack (Sauceror)
    new ChoiceAdventure(
        447,
        "RabbitHole",
        "Reflection of Map (Sauceror)",
        // Option...
        new Option("Vial of <i>jus de larmes</i>", "Vial of <i>jus de larmes</i>"),
        new Option("delicious comfit?", "delicious comfit?"));

    // Choice 448 is The Croquet Grounds (Turtle Tamer)
    new ChoiceAdventure(
        448,
        "RabbitHole",
        "Reflection of Map (Turtle Tamer)",
        // Option...
        new Option("beautiful soup", "beautiful soup"),
        new Option("fight croqueteer"));

    // Choice 449 is The Croquet Grounds (Disco Bandit)
    new ChoiceAdventure(
        449,
        "RabbitHole",
        "Reflection of Map (Disco Bandit)",
        // Option...
        new Option("Lobster <i>qua</i> Grill", "Lobster <i>qua</i> Grill"),
        new Option("fight croqueteer"));

    // Choice 450 is The Duchess' Cottage

    // Typographical Clutter
    new ChoiceAdventure(
        451,
        "Dungeon",
        "Greater-Than Sign",
        // Option...
        new Option("left parenthesis", "left parenthesis"),
        new Option("moxie, alternately lose then gain meat"),
        new Option("plus sign, then muscle", "plus sign"),
        new Option("mysticality substats"),
        new Option("get teleportitis"));

    // Leave a Message and I'll Call You Back
    new ChoiceAdventure(
        452,
        "Jacking",
        "Small-O-Fier",
        // Option...
        new Option("combat"),
        new Option("tiny fly glasses", "tiny fly glasses"),
        new Option("fruit"));

    // Getting a Leg Up
    new ChoiceAdventure(
        453,
        "Jacking",
        "Small-O-Fier",
        // Option...
        new Option("combat"),
        new Option("stats"),
        new Option("hair of the calf", "hair of the calf"));

    // Just Like the Ocean Under the Moon
    new ChoiceAdventure(
        454,
        "Jacking",
        "Small-O-Fier",
        // Option...
        new Option("combat"),
        new Option("HP and MP"));

    // Double Trouble in the Stubble
    new ChoiceAdventure(
        455,
        "Jacking",
        "Small-O-Fier",
        // Option...
        new Option("stats"),
        new Option("quest item"));

    // Made it, Ma! Top of the World!
    new ChoiceAdventure(
        456,
        "Jacking",
        "Huge-A-Ma-tron",
        // Option...
        new Option("combat"),
        new Option("Hurricane Force"),
        new Option("a dance upon the palate", "a dance upon the palate"),
        new Option("stats"));

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

    // Choice 496 is Crate Expectations
    // -> can skip if have +20 hot damage

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
        new Option("gain some meat"),
        new Option("gain stakes or trade vampire hearts", "wooden stakes"),
        new Option("gain spooky sapling or trade bar skins", "spooky sapling"));

    // Tree's Last Stand
    new ChoiceSpoiler(
        504,
        "Spooky Forest",
        // Option...
        new Option("bar skin", "bar skin"),
        new Option("bar skins", "bar skin"),
        new Option("buy spooky sapling", "spooky sapling"),
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
        new Option("gain mosquito larva then 3 spooky mushrooms", "mosquito larva"),
        new Option("gain 300 meat & tree-holed coin then nothing"),
        new Option("fight a spooky vampire"));

    // Through Thicket and Thinnet
    new ChoiceSpoiler(
        506,
        "Spooky Forest",
        // Option...
        new Option("gain a starter item"),
        new Option("gain Spooky-Gro fertilizer", "Spooky-Gro fertilizer"),
        new Option("gain spooky temple map", "spooky temple map"),
        new Option("gain fake blood", "fake blood"));

    // O Lith, Mon
    new ChoiceSpoiler(
        507,
        "Spooky Forest",
        // Option...
        new Option("gain Spooky Temple map"),
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
        new Option("Baron von Ratsworth"),
        SKIP_ADVENTURE);

    // Hot and Cold Running Rats
    new ChoiceAdventure(
        512,
        "Woods",
        "Typical Tavern",
        // Option...
        new Option("fight"),
        SKIP_ADVENTURE);

    // Choice 513 is Staring Down the Barrel
    // -> can skip if have +20 cold damage
    // Choice 514 is 1984 Had Nothing on This Cellar
    // -> can skip if have +20 stench damage
    // Choice 515 is A Rat's Home...
    // -> can skip if have +20 spooky damage

    // Choice 516 is unknown
    // Choice 517 is Mr. Alarm, I Presarm

    // Clear and Present Danger
    new ChoiceAdventure(
        518,
        "Crimbo10",
        "Elf Alley",
        // Option...
        new Option("enter combat with Uncle Hobo"),
        SKIP_ADVENTURE);

    // What a Tosser
    new ChoiceAdventure(
        519,
        "Crimbo10",
        "Elf Alley",
        // Option...
        new Option("gift-a-pult", "gift-a-pult"),
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
        new Option("outfit piece or donut"),
        SKIP_ADVENTURE);

    // Death Rattlin'
    new ChoiceAdventure(
        523,
        "Cyrpt",
        "Defiled Cranny",
        // Option...
        new Option("small meat boost"),
        new Option("stats & HP & MP"),
        new Option("can of Ghuol-B-Gone&trade;", "can of Ghuol-B-Gone&trade;"),
        new Option("fight swarm of ghuol whelps"),
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
        new Option("fight the Bonerdagon"),
        SKIP_ADVENTURE);

    // Choice 528 is It Was Then That a Hideous Monster Carried You

    // A Swarm of Yeti-Mounted Skeletons
    new ChoiceAdventure(
        529,
        "Events",
        "Skeleton Swarm",
        // Option...
        new Option("Weapon Damage"),
        new Option("Spell Damage"),
        new Option("Ranged Damage"));

    // It Was Then That... Aaaaaaaah!
    new ChoiceAdventure(
        530,
        "Events",
        "Icy Peak",
        // Option...
        new Option("hideous egg", "hideous egg"),
        new Option("skip the adventure"));

    // The Bonewall Is In
    new ChoiceAdventure(
        531,
        "Events",
        "Bonewall",
        // Option...
        new Option("Item Drop"),
        new Option("HP Bonus"));

    // You'll Sink His Battleship
    new ChoiceAdventure(
        532,
        "Events",
        "Battleship",
        // Option...
        new Option("Class Skills"),
        new Option("Accordion Thief Songs"));

    // Train, Train, Choo-Choo Train
    new ChoiceAdventure(
        533,
        "Events",
        "Supply Train",
        // Option...
        new Option("Meat Drop"),
        new Option("Pressure Penalty Modifiers"));

    // That's No Bone Moon...
    new ChoiceAdventure(
        534,
        "Events",
        "Bone Star",
        // Option...
        new Option("Torpedos", "photoprotoneutron torpedo"),
        new Option("Initiative"),
        new Option("Monster Level"));

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
        new Option("enter combat with The Necbromancer"),
        SKIP_ADVENTURE);

    // Dark in the Attic
    new ChoiceSpoiler(
        549,
        "Events",
        "Dark in the Attic",
        // Option...
        new Option("staff guides", "Haunted Sorority House staff guide"),
        new Option("ghost trap", "ghost trap"),
        new Option("raise area ML"),
        new Option("lower area ML"),
        new Option("mass kill werewolves with silver shotgun shell", "silver shotgun shell"));

    // The Unliving Room
    new ChoiceSpoiler(
        550,
        "Events",
        "The Unliving Room",
        // Option...
        new Option("raise area ML"),
        new Option("lower area ML"),
        new Option("mass kill zombies with chainsaw chain", "chainsaw chain"),
        new Option("mass kill skeletons with funhouse mirror", "funhouse mirror"),
        new Option("get costume item"));

    // Debasement
    new ChoiceSpoiler(
        551,
        "Events",
        "Debasement",
        // Option...
        new Option("Prop Deportment"),
        new Option("mass kill vampires with plastic vampire fangs"),
        new Option("raise area ML"),
        new Option("lower area ML"));

    // Prop Deportment
    new ChoiceSpoiler(
        552,
        "Events",
        "Prop Deportment",
        // Option...
        new Option("chainsaw chain", "chainsaw chain"),
        new Option("create a silver shotgun shell", "silver shotgun shell"),
        new Option("funhouse mirror", "funhouse mirror"));

    // Relocked and Reloaded
    new ChoiceSpoiler(
        553,
        "Events",
        "Relocked and Reloaded",
        // Option...
        new Option("", "Maxwell's Silver hammer"),
        new Option("", "silver tongue charrrm bracelet"),
        new Option("", "silver cheese-slicer"),
        new Option("", "silver shrimp fork"),
        new Option("", "silver pat&eacute; knife"),
        new Option("exit adventure"));

    // Behind the Spooky Curtain
    new ChoiceSpoiler(
        554,
        "Events",
        "Behind the Spooky Curtain",
        // Option...
        new Option("staff guides, ghost trap, kill werewolves"),
        new Option("kill zombies, kill skeletons, costume item"),
        new Option("chainsaw chain, silver item, funhouse mirror, kill vampires"));

    // More Locker Than Morlock
    new ChoiceAdventure(
        556,
        "McLarge",
        "Itznotyerzitz Mine",
        // Option...
        new Option("get an outfit piece"),
        SKIP_ADVENTURE);

    // Gingerbread Homestead
    new ChoiceAdventure(
        557,
        "The Candy Diorama",
        "Gingerbread Homestead",
        // Option...
        new Option("get candies"),
        new Option("licorice root", "licorice root"),
        new Option("skip adventure or make a lollipop stick item", "lollipop stick"));

    // Tool Time
    new ChoiceAdventure(
        558,
        "The Candy Diorama",
        "Tool Time",
        // Option...
        new Option("sucker bucket", "sucker bucket"),
        new Option("sucker kabuto", "sucker kabuto"),
        new Option("sucker hakama", "sucker hakama"),
        new Option("sucker tachi", "sucker tachi"),
        new Option("sucker scaffold", "sucker scaffold"),
        SKIP_ADVENTURE);

    // Fudge Mountain Breakdown
    new ChoiceAdventure(
        559,
        "The Candy Diorama",
        "Fudge Mountain Breakdown",
        // Option...
        new Option("fudge lily", "fudge lily"),
        new Option("fight a swarm of fudgewasps or skip adventure"),
        new Option("frigid fudgepuck or skip adventure", "frigid fudgepuck"),
        new Option("superheated fudge or skip adventure", "superheated fudge"));

    // Foreshadowing Demon!
    new ChoiceAdventure(
        560,
        "Suburbs",
        "The Clumsiness Grove",
        // Option...
        new Option("head towards boss"),
        SKIP_ADVENTURE);

    // You Must Choose Your Destruction!
    new ChoiceAdventure(
        561,
        "Suburbs",
        "The Clumsiness Grove",
        // Option...
        new Option("The Thorax"),
        new Option("The Bat in the Spats"));

    // Choice 562 is You're the Fudge Wizard Now, Dog

    // A Test of your Mettle
    new ChoiceAdventure(
        563,
        "Suburbs",
        "The Clumsiness Grove",
        // Option...
        new Option("Fight Boss"),
        SKIP_ADVENTURE);

    // A Maelstrom of Trouble
    new ChoiceAdventure(
        564,
        "Suburbs",
        "The Maelstrom of Lovers",
        // Option...
        new Option("head towards boss"),
        SKIP_ADVENTURE);

    // To Get Groped or Get Mugged?
    new ChoiceAdventure(
        565,
        "Suburbs",
        "The Maelstrom of Lovers",
        // Option...
        new Option("The Terrible Pinch"),
        new Option("Thug 1 and Thug 2"));

    // A Choice to be Made
    new ChoiceAdventure(
        566,
        "Suburbs",
        "The Maelstrom of Lovers",
        // Option...
        new Option("Fight Boss"),
        SKIP_ADVENTURE);

    // You May Be on Thin Ice
    new ChoiceAdventure(
        567,
        "Suburbs",
        "The Glacier of Jerks",
        // Option...
        new Option("Fight Boss"),
        SKIP_ADVENTURE);

    // Some Sounds Most Unnerving
    new ChoiceAdventure(
        568,
        "Suburbs",
        "The Glacier of Jerks",
        // Option...
        new Option("Mammon the Elephant"),
        new Option("The Large-Bellied Snitch"));

    // One More Demon to Slay
    new ChoiceAdventure(
        569,
        "Suburbs",
        "The Glacier of Jerks",
        // Option...
        new Option("head towards boss"),
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
        new Option("get an outfit piece"),
        new Option("jar of frostigkraut", "jar of frostigkraut"),
        SKIP_ADVENTURE,
        new Option("lucky pill", "lucky pill"));

    // Choice 576 is Your Minstrel Camps
    // Choice 577 is Your Minstrel Scamp
    // Choice 578 is End of the Boris Road

    // Such Great Heights
    new ChoiceAdventure(
        579,
        "Woods",
        "Hidden Temple Heights",
        // Option...
        new Option("mysticality substats"),
        new Option("Nostril of the Serpent then skip adventure", "Nostril of the Serpent"),
        new Option("gain 3 adv then skip adventure"));

    // Choice 580 is The Hidden Heart of the Hidden Temple (4 variations)

    // Such Great Depths
    new ChoiceAdventure(
        581,
        "Woods",
        "Hidden Temple Depths",
        // Option...
        new Option("glowing fungus", "glowing fungus"),
        new Option("+15 mus/mys/mox then skip adventure"),
        new Option("fight clan of cave bars"));

    // Fitting In
    new ChoiceAdventure(
        582,
        "Woods",
        "Hidden Temple",
        // Option...
        new Option("Such Great Heights"),
        new Option("heart of the Hidden Temple"),
        new Option("Such Great Depths"));

    // Confusing Buttons
    new ChoiceSpoiler(
        583,
        "Woods",
        "Hidden Temple",
        // Option...
        new Option("Press a random button"));

    // Unconfusing Buttons
    new ChoiceAdventure(
        584,
        "Woods",
        "Hidden Temple",
        // Option...
        new Option("Hidden Temple (Stone) - muscle substats"),
        new Option("Hidden Temple (Sun) - gain ancient calendar fragment"),
        new Option("Hidden Temple (Gargoyle) - MP"),
        new Option("Hidden Temple (Pikachutlotal) - Hidden City unlock"));

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
        new Option("lost glasses", "lost glasses"),
        new Option("lost comb", "lost comb"),
        new Option("lost pill bottle", "lost pill bottle"));

    // Fire! I... have made... fire!
    new ChoiceAdventure(
        595,
        "Item-Driven",
        "CSA fire-starting kit",
        // Option...
        new Option("pvp fights"),
        new Option("hp/mp regen"));

    // Choice 596 is Dawn of the D'oh

    // Cake Shaped Arena
    new ChoiceAdventure(
        597,
        "Item-Driven",
        "Reagnimated Gnome",
        // Option...
        new Option("gnomish swimmer's ears (underwater)", "gnomish swimmer's ears"),
        new Option("gnomish coal miner's lung (block)", "gnomish coal miner's lung"),
        new Option("gnomish tennis elbow (damage)", "gnomish tennis elbow"),
        new Option("gnomish housemaid's kgnee (gain advs)", "gnomish housemaid's kgnee"),
        new Option("gnomish athlete's foot (delevel)", "gnomish athlete's foot"));

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
        new Option("warrior (dmg, delevel)"),
        new Option("cleric (hot dmg, hp)"),
        new Option("wizard (cold dmg, mp)"),
        new Option("rogue (dmg, meat)"),
        new Option("buddy (delevel, exp)"),
        new Option("ignore this adventure"));

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
        new Option("Familiar does physical damage"),
        new Option("Familiar does cold damage"),
        new Option("+10% item drops, can drop carrot nose"),
        new Option("Heals 1-20 HP after combat"),
        new Option("Restores 1-10 MP after combat"));

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
        new Option("Fight Boss"),
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
        new Option("Open Ground Floor with titanium umbrella, otherwise Neckbeard Choice"),
        new Option("200 Moxie substats"),
        new Option("???"),
        new Option("skip adventure and guarantee this adventure will reoccur"));

    // You Don't Mess Around with Gym
    new ChoiceAdventure(
        670,
        "Beanstalk",
        "Basement Fitness",
        // Option...
        new Option("massive dumbbell, then skip adventure", "massive dumbbell"),
        new Option("Muscle stats"),
        new Option("Items"),
        new Option("Open Ground Floor with amulet, otherwise skip"),
        new Option("skip adventure and guarantee this adventure will reoccur"));

    // Out in the Open Source
    new ChoiceAdventure(
        671,
        "Beanstalk",
        "Basement Neckbeard",
        // Option...
        new Option(
            "With massive dumbbell, open Ground Floor, otherwise skip adventure",
            "massive dumbbell"),
        new Option("200 Mysticality substats"),
        new Option("O'RLY manual, open sauce"),
        new Option("Fitness Choice"));

    // There's No Ability Like Possibility
    new ChoiceAdventure(
        672,
        "Beanstalk",
        "Ground Possibility",
        // Option...
        new Option("3 random items"),
        new Option("Nothing Is Impossible"),
        SKIP_ADVENTURE);

    // Putting Off Is Off-Putting
    new ChoiceAdventure(
        673,
        "Beanstalk",
        "Ground Procrastination",
        // Option...
        new Option("very overdue library book, then skip adventure", "very overdue library book"),
        new Option("Trash-Wrapped"),
        SKIP_ADVENTURE);

    // Huzzah!
    new ChoiceAdventure(
        674,
        "Beanstalk",
        "Ground Renaissance",
        // Option...
        new Option("pewter claymore, then skip adventure", "pewter claymore"),
        new Option("Pretending to Pretend"),
        SKIP_ADVENTURE);

    // Melon Collie and the Infinite Lameness
    new ChoiceAdventure(
        675,
        "Beanstalk",
        "Top Goth",
        // Option...
        new Option("Fight a Goth Giant"),
        new Option("complete quest", "drum 'n' bass 'n' drum 'n' bass record"),
        new Option("3 thin black candles", "thin black candle"),
        new Option("Steampunk Choice"));

    // Flavor of a Raver
    new ChoiceAdventure(
        676,
        "Beanstalk",
        "Top Raver",
        // Option...
        new Option("Fight a Raver Giant"),
        new Option("Restore 1000 hp & mp"),
        new Option(
            "drum 'n' bass 'n' drum 'n' bass record, then skip adventure",
            "drum 'n' bass 'n' drum 'n' bass record"),
        new Option("Punk Rock Choice"));

    // Copper Feel
    new ChoiceAdventure(
        677,
        "Beanstalk",
        "Top Steampunk",
        // Option...
        new Option(
            "With model airship, complete quest, otherwise fight Steampunk Giant", "model airship"),
        new Option(
            "steam-powered model rocketship, then skip adventure",
            "steam-powered model rocketship"),
        new Option("brass gear", "brass gear"),
        new Option("Goth Choice"));

    // Yeah, You're for Me, Punk Rock Giant
    new ChoiceAdventure(
        678,
        "Beanstalk",
        "Top Punk Rock",
        // Option...
        new Option("Wearing mohawk wig, turn wheel, otherwise fight Punk Rock Giant"),
        new Option("500 meat"),
        new Option("Steampunk Choice"),
        new Option("Raver Choice"));

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
        new Option("Get fat loot token"));

    // The First Chest Isn't the Deepest.
    new ChoiceAdventure(
        690,
        "Dungeon",
        "Daily Dungeon: Chest 1",
        // Option...
        new Option("Get item"),
        new Option("Skip to 8th chamber, no turn spent"),
        new Option("Skip to 6th chamber, no turn spent"));

    // Second Chest
    new ChoiceAdventure(
        691,
        "Dungeon",
        "Daily Dungeon: Chest 2",
        // Option...
        new Option("Get item"),
        new Option("Skip to 13th chamber, no turn spent"),
        new Option("Skip to 11th chamber, no turn spent"));

    // Choice 692 is I Wanna Be a Door

    // It's Almost Certainly a Trap
    new ChoiceAdventure(
        693,
        "Dungeon",
        "Daily Dungeon: Traps",
        // Option...
        new Option("Suffer elemental damage, get stats"),
        new Option("Avoid trap with eleven-foot pole, no turn spent"),
        new Option("Leave, no turn spent"));

    // Choice 695 is A Drawer of Chests

    // Choice 696 is Stick a Fork In It
    new ChoiceAdventure(
        696,
        "Le Marais D&egrave;gueulasse",
        "Edge of the Swamp",
        // Option...
        new Option("unlock The Dark and Spooky Swamp"),
        new Option("unlock The Wildlife Sanctuarrrrrgh"));

    // Choice 697 is Sophie's Choice
    new ChoiceAdventure(
        607,
        "Le Marais D&egrave;gueulasse",
        "Dark and Spooky Swamp",
        // Option...
        new Option("unlock The Corpse Bog"),
        new Option("unlock The Ruined Wizard Tower"));

    // Choice 698 is From Bad to Worst
    new ChoiceAdventure(
        698,
        "Le Marais D&egrave;gueulasse",
        "Wildlife Sanctuarrrrrgh",
        // Option...
        new Option("unlock Swamp Beaver Territory"),
        new Option("unlock The Weird Swamp Village"));

    // Choice 701 is Ators Gonna Ate
    new ChoiceAdventure(
        701,
        "The Sea",
        "Mer-kin Gymnasium",
        // Option...
        new Option("get an item"),
        SKIP_ADVENTURE);

    // Choice 703 is Mer-kin dreadscroll
    // Choice 704 is Playing the Catalog Card

    // Choice 705 is Halls Passing in the Night
    new ChoiceAdventure(
        705,
        "The Sea",
        "Mer-kin Elementary School",
        // Option...
        new Option("fight a Mer-kin spectre"),
        new Option("Mer-kin sawdust", "Mer-kin sawdust"),
        new Option("Mer-kin cancerstick", "Mer-kin cancerstick"),
        new Option("Mer-kin wordquiz", "Mer-kin wordquiz"));

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
        new Option("learn shortcut", 5),
        new Option("skip adventure", 6));

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
        new Option("learn shortcut", 5),
        new Option("skip adventure", 6));

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
        new Option("learn shortcut", 5),
        new Option("skip adventure", 6));

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
        new Option("learn shortcut", 5),
        new Option("skip adventure", 6));

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
        new Option("learn shortcut", 5),
        new Option("skip adventure", 6));

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
        new Option("learn shortcut", 5),
        new Option("skip adventure", 6));

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
        new Option("learn shortcut", 5),
        new Option("skip adventure", 6));

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
        new Option("learn shortcut", 5),
        new Option("skip adventure", 6));

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
        new Option("learn shortcut", 5),
        new Option("skip adventure", 6));

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
        new Option("gain 400-500 meat", 1),
        new Option("gain 50-60 muscle stats", 2),
        new Option("gain 50-60 mysticality stats", 3),
        new Option("gain 50-60 moxie stats", 4),
        new Option("don't use it", 6));

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
        new Option("Muscle Vacation"),
        new Option("Mysticality Vacation"),
        new Option("Moxie Vacation"));

    // Choice 794 is Once More Unto the Junk
    new ChoiceAdventure(
        794,
        "Woods",
        "The Old Landfill",
        // Option...
        new Option("The Bathroom of Ten Men"),
        new Option("The Den of Iquity"),
        new Option("Let's Workshop This a Little"));

    // Choice 795 is The Bathroom of Ten Men
    new ChoiceAdventure(
        795,
        "Woods",
        "The Bathroom of Ten Men",
        // Option...
        new Option("old claw-foot bathtub", "old claw-foot bathtub"),
        new Option("fight junksprite"),
        new Option("make lots of noise"));

    // Choice 796 is The Den of Iquity
    new ChoiceAdventure(
        796,
        "Woods",
        "The Den of Iquity",
        // Option...
        new Option("make lots of noise"),
        new Option("old clothesline pole", "old clothesline pole"),
        new Option("tangle of copper wire", "tangle of copper wire"));

    // Choice 797 is Let's Workshop This a Little
    new ChoiceAdventure(
        797,
        "Woods",
        "Let's Workshop This a Little",
        // Option...
        new Option("Junk-Bond", "Junk-Bond"),
        new Option("make lots of noise"),
        new Option("antique cigar sign", "antique cigar sign"));

    // Choice 801 is A Reanimated Conversation

    // Choice 803 is Behind the Music.  Literally.
    new ChoiceAdventure(
        803,
        "Events",
        "The Space Odyssey Discotheque",
        // Option...
        new Option("gain 2-3 horoscopes", 1),
        new Option("find interesting room", 3),
        new Option("investigate interesting room", 4),
        new Option("investigate trap door", 5),
        new Option("investigate elevator", 6));

    // Choice 804 is Trick or Treat!

    // Choice 805 is A Sietch in Time
    new ChoiceAdventure(
        805,
        "Beach",
        "Arid, Extra-Dry Desert",
        // Option...
        new Option("talk to Gnasir"));

    // Choice 808 is Silence at Last.
    new ChoiceAdventure(
        808,
        "Events",
        "The Spirit World",
        // Option...
        new Option("gain spirit bed piece"),
        new Option("fight spirit alarm clock"));

    // Choice 809 is Uncle Crimbo's Trailer
    // Choice 810 is K.R.A.M.P.U.S. facility

    // Choice 813 is What Warbears Are Good For
    new ChoiceAdventure(
        813,
        "Crimbo13",
        "Warbear Fortress (First Level)",
        // Option...
        new Option("Open K.R.A.M.P.U.S. facility"));

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
        new Option("+Wolf Offence or +Wolf Defence"),
        new Option("+Wolf Elemental Attacks or +Rabbit"),
        new Option("Improved Howling! or +Wolf Lung Capacity"),
        new Option("Leave", 6));

    // Choice 832 is Shower Power
    new ChoiceAdventure(
        832,
        "Skid Row",
        "Shower Power",
        // Option...
        new Option("+Wolf Offence"),
        new Option("+Wolf Defence"));

    // Choice 833 is Vendie, Vidi, Vici
    new ChoiceAdventure(
        833,
        "Skid Row",
        "Vendie, Vidi, Vici",
        // Option...
        new Option("+Wolf Elemental Attacks"),
        new Option("+Rabbit"));

    // Choice 834 is Back Room Dealings
    new ChoiceAdventure(
        834,
        "Skid Row",
        "Back Room Dealings",
        // Option...
        new Option("Improved Howling!", 2),
        new Option("+Wolf Lung Capacity", 3));

    // Choice 835 is Barely Tales
    new ChoiceAdventure(
        835,
        "Item-Driven",
        "Grim Brother",
        // Option...
        new Option("30 turns of +20 initiative"),
        new Option("30 turns of +20 max HP, +10 max MP"),
        new Option("30 turns of +10 Weapon Damage, +20 Spell Damage"));

    // Choice 836 is Adventures Who Live in Ice Houses...

    // Choice 837 is On Purple Pond
    new ChoiceAdventure(
        837,
        "The Candy Witch and the Relentless Child Thieves",
        "On Purple Pond",
        // Option...
        new Option("find out the two children not invading"),
        new Option("+1 Moat"),
        new Option("gain Candy"));

    // Choice 838 is General Mill
    new ChoiceAdventure(
        838,
        "The Candy Witch and the Relentless Child Thieves",
        "General Mill",
        // Option...
        new Option("+1 Moat"),
        new Option("gain Candy"));

    // Choice 839 is On The Sounds of the Undergrounds
    new ChoiceAdventure(
        839,
        "The Candy Witch and the Relentless Child Thieves",
        "The Sounds of the Undergrounds",
        // Option...
        new Option("learn what the first two waves will be"),
        new Option("+1 Minefield Strength"),
        new Option("gain Candy"));

    // Choice 840 is Hop on Rock Pops
    new ChoiceAdventure(
        840,
        "The Candy Witch and the Relentless Child Thieves",
        "Hop on Rock Pops",
        // Option...
        new Option("+1 Minefield Strength"),
        new Option("gain Candy"));

    // Choice 841 is Building, Structure, Edifice
    new ChoiceAdventure(
        841,
        "The Candy Witch and the Relentless Child Thieves",
        "Building, Structure, Edifice",
        // Option...
        new Option("increase candy in another location"),
        new Option("+2 Random Defense"),
        new Option("gain Candy"));

    // Choice 842 is The Gingerbread Warehouse
    new ChoiceAdventure(
        842,
        "The Candy Witch and the Relentless Child Thieves",
        "The Gingerbread Warehouse",
        // Option...
        new Option("+1 Wall Strength"),
        new Option("+1 Poison Jar"),
        new Option("+1 Anti-Aircraft Turret"),
        new Option("gain Candy"));

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
        new Option("don't take initial damage in fights"),
        new Option("can get priceless diamond"),
        new Option("can make Flamin' Whatshisname"),
        new Option("get 4-5 random items"));

    // Choice 856 is This Looks Like a Good Bush for an Ambush
    new ChoiceAdventure(
        856,
        "The Red Zeppelin's Mooring",
        "This Looks Like a Good Bush for an Ambush",
        // Option...
        new Option("scare protestors (more with lynyrd gear)"),
        SKIP_ADVENTURE);

    // Choice 857 is Bench Warrant
    new ChoiceAdventure(
        857,
        "The Red Zeppelin's Mooring",
        "Bench Warrant",
        // Option...
        new Option("creep protestors (more with sleaze damage/sleaze spell damage)"),
        SKIP_ADVENTURE);

    // Choice 858 is Fire Up Above
    new ChoiceAdventure(
        858,
        "The Red Zeppelin's Mooring",
        "Fire Up Above",
        // Option...
        new Option("set fire to protestors (more with Flamin' Whatshisname)"),
        SKIP_ADVENTURE);

    // Choice 866 is Methinks the Protesters Doth Protest Too Little
    new ChoiceAdventure(
        866,
        "The Red Zeppelin's Mooring",
        "Methinks the Protesters Doth Protest Too Little",
        // Option...
        new Option("scare protestors (more with lynyrd gear)"),
        new Option("creep protestors (more with sleaze damage/sleaze spell damage)"),
        new Option("set fire to protestors (more with Flamin' Whatshisname)"));

    // Rod Nevada, Vendor
    new ChoiceSpoiler(
        873,
        "Plains",
        "The Palindome",
        // Option...
        new Option("photograph of a red nugget", "photograph of a red nugget"),
        SKIP_ADVENTURE);
    // Rod Nevada, Vendor
    new ChoiceCost(873, new Cost(1, new AdventureResult(AdventureResult.MEAT, -500)));

    // Welcome To Our ool Table
    new ChoiceAdventure(
        875,
        "Manor1",
        "Pool Table",
        // Option...
        new Option("try to beat ghost"),
        new Option("improve pool skill"),
        new Option("skip"));

    // One Simple Nightstand
    new ChoiceAdventure(
        876,
        "Manor2",
        "One Simple Nightstand",
        // Option...
        new Option("old leather wallet", 1),
        new Option("muscle substats", 2),
        new Option("muscle substats (with ghost key)", 3),
        new Option("skip", 6));

    // One Mahogany Nightstand
    new ChoiceAdventure(
        877,
        "Manor2",
        "One Mahogany Nightstand",
        // Option...
        new Option("old coin purse or half a memo", 1),
        new Option("take damage", 2),
        new Option("quest item", 3),
        new Option("gain more meat (with ghost key)", 4),
        new Option("skip", 6));

    // One Ornate Nightstand
    new ChoiceAdventure(
        878,
        "Manor2",
        "One Ornate Nightstand",
        // Option...
        new Option("small meat boost", 1),
        new Option("mysticality substats", 2),
        new Option("Lord Spookyraven's spectacles", 3, "Lord Spookyraven's spectacles"),
        new Option("disposable instant camera", 4, "disposable instant camera"),
        new Option("mysticality substats (with ghost key)", 5),
        new Option("skip", 6));

    // One Rustic Nightstand
    new ChoiceAdventure(
        879,
        "Manor2",
        "One Rustic Nightstand",
        // Option...
        new Option("moxie", 1),
        new Option("grouchy restless spirit or empty drawer", 2, "grouchy restless spirit"),
        new Option("enter combat with mistress (1)", 3),
        new Option("Engorged Sausages and You or moxie", 4),
        new Option("moxie substats (with ghost key)", 5),
        new Option("skip", 6));

    // One Elegant Nightstand
    new ChoiceAdventure(
        880,
        "Manor2",
        "One Elegant Nightstand",
        // Option...
        new Option(
            "Lady Spookyraven's finest gown (once only)", 1, "Lady Spookyraven's finest gown"),
        new Option("elegant nightstick", 2, "elegant nightstick"),
        new Option("stats (with ghost key)", 3),
        new Option("skip", 6));

    // Off the Rack
    new ChoiceAdventure(
        882,
        "Manor2",
        "Bathroom Towel",
        // Option...
        new Option("get towel"),
        new Option("skip"));

    // Take a Look, it's in a Book!
    new ChoiceSpoiler(
        888,
        "Haunted Library",
        // Option...
        new Option("background history"),
        new Option("cooking recipe"),
        new Option("other options"),
        SKIP_ADVENTURE);

    // Take a Look, it's in a Book!
    new ChoiceSpoiler(
        889,
        "Haunted Library",
        // Option...
        new Option("background history", 1),
        new Option("cocktailcrafting recipe", 2),
        new Option("muscle substats", 3),
        new Option("dictionary", 4, "dictionary"),
        new Option("skip", 5));

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
        new Option("Enter the Drawing"),
        SKIP_ADVENTURE);

    // Choice 918 is Yachtzee!
    new ChoiceAdventure(
        918,
        "Spring Break Beach",
        "Yachtzee!",
        // Option...
        new Option("get cocktail ingredients (sometimes Ultimate Mind Destroyer)"),
        new Option("get 5k meat and random item"),
        new Option("get Beach Bucks"));

    // Choice 919 is Break Time!
    new ChoiceAdventure(
        919,
        "Spring Break Beach",
        "Break Time!",
        // Option...
        new Option("get Beach Bucks"),
        new Option("+15ML on Sundaes"),
        new Option("+15ML on Burgers"),
        new Option("+15ML on Cocktails"),
        new Option("reset ML on monsters"),
        new Option("leave without using a turn"));

    // Choice 920 is Eraser
    new ChoiceAdventure(
        920,
        "Item-Driven",
        "Eraser",
        // Option...
        new Option("reset Buff Jimmy quests"),
        new Option("reset Taco Dan quests"),
        new Option("reset Broden quests"),
        new Option("don't use it"));

    // Choice 921 is We'll All Be Flat

    // Choice 923 is All Over the Map
    new ChoiceAdventure(
        923,
        "Woods",
        "Black Forest",
        // Option...
        new Option("fight blackberry bush, visit cobbler, or raid beehive"),
        new Option("visit blacksmith"),
        new Option("visit black gold mine"),
        new Option("visit black church"));

    // Choice 924 is You Found Your Thrill
    new ChoiceAdventure(
        924,
        "Woods",
        "Blackberry",
        // Option...
        new Option("fight blackberry bush"),
        new Option("visit cobbler"),
        new Option("head towards beehive (1)"));

    // Choice 925 is The Blackest Smith
    new ChoiceAdventure(
        925,
        "Woods",
        "Blacksmith",
        // Option...
        new Option("get black sword", 1, "black sword"),
        new Option("get black shield", 2, "black shield"),
        new Option("get black helmet", 3, "black helmet"),
        new Option("get black greaves", 4, "black greaves"),
        new Option("return to main choice", 6));

    // Choice 926 is Be Mine
    new ChoiceAdventure(
        926,
        "Woods",
        "Black Gold Mine",
        // Option...
        new Option("get black gold", 1, "black gold"),
        new Option("get Texas tea", 2, "Texas tea"),
        new Option("get Black Lung effect", 3),
        new Option("return to main choice", 6));

    // Choice 927 is Sunday Black Sunday
    new ChoiceAdventure(
        927,
        "Woods",
        "Black Church",
        // Option...
        new Option("get 13 turns of Salsa Satanica or beaten up", 1),
        new Option("get black kettle drum", 2, "black kettle drum"),
        new Option("return to main choice", 6));

    // Choice 928 is The Blackberry Cobbler
    new ChoiceAdventure(
        928,
        "Woods",
        "Blackberry Cobbler",
        // Option...
        new Option("get blackberry slippers", 1, "blackberry slippers"),
        new Option("get blackberry moccasins", 2, "blackberry moccasins"),
        new Option("get blackberry combat boots", 3, "blackberry combat boots"),
        new Option("get blackberry galoshes", 4, "blackberry galoshes"),
        new Option("return to main choice", 6));

    // Choice 929 is Control Freak
    new ChoiceAdventure(
        929,
        "Pyramid",
        "Control Room",
        // Option...
        new Option("turn lower chamber, lose wheel", 1),
        new Option("turn lower chamber, lose ratchet", 2),
        new Option("enter lower chamber", 5),
        new Option("leave", 6));

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
        new Option("fight whitesnake"),
        new Option("fight white lion"),
        new Option("fight white chocolate golem"),
        new Option("fight white knight"),
        new Option("fight white elephant"),
        new Option("skip"));

    // Choice 950 is Time-Twitching Tower Voting / Phone Booth

    // Choice 955 is Time Cave.  Period.
    new ChoiceAdventure(
        955,
        "Twitch",
        "Time Cave",
        // Option...
        new Option("fight Adventurer echo"),
        new Option("twitching time capsule", "twitching time capsule"),
        new Option("talk to caveman"));

    // Choice 973 is Shoe Repair Store
    new ChoiceAdventure(
        973,
        "Twitch",
        "Shoe Repair Store",
        // Option...
        new Option("visit shop", 1),
        new Option("exchange hooch for Chroners", 2),
        new Option("leave", 6));

    // Choice 974 is Around The World
    new ChoiceAdventure(
        974,
        "Twitch",
        "Bohemian Party",
        // Option...
        new Option("get up to 5 hooch"),
        new Option("leave"));

    // Choice 975 is Crazy Still After All These Years
    new ChoiceAdventure(
        975,
        "Twitch",
        "Moonshriner's Woods",
        // Option...
        new Option("swap 5 cocktail onions for 10 hooch"),
        new Option("leave"));

    // Choice 979 is The Agora
    new ChoiceAdventure(
        979,
        "Twitch",
        "The Agora",
        // Option...
        new Option("get blessing", 1),
        new Option("visit store", 2),
        new Option("play dice", 6));

    // Choice 980 is Welcome to Blessings Hut
    new ChoiceAdventure(
        980,
        "Twitch",
        "Blessings Hut",
        // Option...
        new Option("Bruno's blessing of Mars", "Bruno's blessing of Mars"),
        new Option("Dennis's blessing of Minerva", "Dennis's blessing of Minerva"),
        new Option("Burt's blessing of Bacchus", "Burt's blessing of Bacchus"),
        new Option("Freddie's blessing of Mercury", "Freddie's blessing of Mercury"),
        new Option("return to Agora", 6));

    // Choice 982 is The 99-Centurion Store
    new ChoiceAdventure(
        982,
        "Twitch",
        "The 99-Centurion Store",
        // Option...
        new Option("centurion helmet", "centurion helmet"),
        new Option("pteruges", "pteruges"),
        new Option("return to Agora", 6));

    // Choice 983 is Playing Dice With Romans
    new ChoiceAdventure(
        983,
        "Twitch",
        "Playing Dice With Romans",
        // Option...
        new Option("make a bet and throw dice", 1),
        new Option("return to Agora", 6));

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
        new Option("Gain 7 Chroner"),
        new Option("Gain 9 Chroner"),
        new Option("Gain 13 Chroner (80% chance)"),
        new Option("Gain 17 Chroner (60% chance)"),
        new Option("Gain 21 Chroner, lose pocket ace"));

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
        new Option("topiary nugglet and advance to Room 2", "topiary nugglet"),
        new Option("Test #1 and advance to Room 4"));

    // One Small Step For Adventurer
    new ChoiceAdventure(
        1006,
        "Sorceress",
        "Hedge Maze 2",
        // Option...
        new Option("topiary nugglet and advance to Room 3", "topiary nugglet"),
        new Option("Fight topiary gopher and advance to Room 4"));

    // Twisty Little Passages, All Hedge
    new ChoiceAdventure(
        1007,
        "Sorceress",
        "Hedge Maze 3",
        // Option...
        new Option("topiary nugglet and advance to Room 4", "topiary nugglet"),
        new Option("Fight topiary chihuahua herd and advance to Room 5"));

    // Pooling Your Resources
    new ChoiceAdventure(
        1008,
        "Sorceress",
        "Hedge Maze 4",
        // Option...
        new Option("topiary nugglet and advance to Room 5", "topiary nugglet"),
        new Option("Test #2 and advance to Room 7"));

    // Good Ol' 44% Duck
    new ChoiceAdventure(
        1009,
        "Sorceress",
        "Hedge Maze 5",
        // Option...
        new Option("topiary nugglet and advance to Room 6", "topiary nugglet"),
        new Option("Fight topiary duck and advance to Room 7"));

    // Another Day, Another Fork
    new ChoiceAdventure(
        1010,
        "Sorceress",
        "Hedge Maze 6",
        // Option...
        new Option("topiary nugglet and advance to Room 7", "topiary nugglet"),
        new Option("Fight topiary kiwi and advance to Room 8"));

    // Of Mouseholes and Manholes
    new ChoiceAdventure(
        1011,
        "Sorceress",
        "Hedge Maze 7",
        // Option...
        new Option("topiary nugglet and advance to Room 8", "topiary nugglet"),
        new Option("Test #3 and advance to Room 9"));

    // The Last Temptation
    new ChoiceAdventure(
        1012,
        "Sorceress",
        "Hedge Maze 8",
        // Option...
        new Option("topiary nugglet and advance to Room 9", "topiary nugglet"),
        new Option("Lose HP for no benefit and advance to Room 9"));

    // Choice 1013 is Mazel Tov!

    // The Mirror in the Tower has the View that is True
    new ChoiceAdventure(
        1015,
        "Sorceress",
        "Tower Mirror",
        // Option...
        new Option("Gain Confidence! intrinsic until leave tower (1)"),
        new Option("Make Sorceress tougher (0 turns)"));

    // Choice 1016 is Frank Gets Earnest
    // Choice 1017 is Bear Verb Orgy

    // Bee Persistent
    new ChoiceAdventure(
        1018,
        "Woods",
        "Bees 1",
        // Option...
        new Option("head towards beehive (1)"),
        new Option("give up"));

    // Bee Rewarded
    new ChoiceAdventure(
        1019,
        "Woods",
        "Bees 2",
        // Option...
        new Option("beehive (1)", "beehive"),
        new Option("give up"));

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
        new Option("4 pieces of candy"),
        new Option("electric boning knife, then skip adventure", "electric boning knife"),
        SKIP_ADVENTURE);

    // Choice 1027 is The End of the Tale of Spelunking

    // Choice 1028 is A Shop
    new ChoiceAdventure(
        1028,
        "Spelunky Area",
        "A Shop",
        // Option...
        new Option("chance to fight shopkeeper", 5),
        new Option("leave", 6));

    // Choice 1029 is An Old Clay Pot
    new ChoiceAdventure(
        1029,
        "Spelunky Area",
        "An Old Clay Pot",
        // Option...
        new Option("gain 18-20 gold", 1),
        new Option("gain pot", 5, "pot"));

    // Choice 1030 is It's a Trap!  A Dart Trap.
    new ChoiceAdventure(
        1030,
        "Spelunky Area",
        "It's a Trap!  A Dart Trap.",
        // Option...
        new Option("escape with whip", 1),
        new Option("unlock The Snake Pit using bomb", 2),
        new Option("unlock The Spider Hole using rope", 3),
        new Option("escape using offhand item", 4),
        new Option("take damage", 6));

    // Choice 1031 is A Tombstone
    new ChoiceAdventure(
        1031,
        "Spelunky Area",
        "A Tombstone",
        // Option...
        new Option("gain 20-25 gold or buddy", 1),
        new Option("gain shotgun with pickaxe", 2, "shotgun"),
        new Option("gain Clown Crown with x-ray specs", 3, "The Clown Crown"));

    // Choice 1032 is It's a Trap!  A Tiki Trap.
    new ChoiceAdventure(
        1032,
        "Spelunky Area",
        "It's a Trap!  A Tiki Trap.",
        // Option...
        new Option("escape with spring boots", 1),
        new Option("unlock The Beehive using bomb, take damage without sticky bomb", 2),
        new Option("unlock The Ancient Burial Ground using rope, take damage without back item", 3),
        new Option("lose 30 hp", 6));

    // Choice 1033 is A Big Block of Ice
    new ChoiceAdventure(
        1033,
        "Spelunky Area",
        "A Big Block of Ice",
        // Option...
        new Option("gain 50-60 gold and restore health (with cursed coffee cup)", 1),
        new Option("gain buddy (or 60-70 gold) with torch", 2));

    // Choice 1034 is A Landmine
    new ChoiceAdventure(
        1034,
        "Spelunky Area",
        "A Landmine",
        // Option...
        new Option("unlock An Ancient Altar and lose 10 HP", 2),
        new Option("unlock The Crashed UFO using 3 ropes", 3),
        new Option("lose 30 hp", 6));

    // Choice 1035 is A Crate

    // Choice 1036 is Idolatry
    new ChoiceAdventure(
        1036,
        "Spelunky Area",
        "Idolatry",
        // Option...
        new Option("gain 250 gold with Resourceful Kid", 1),
        new Option("gain 250 gold with spring boots and yellow cloak", 2),
        new Option("gain 250 gold with jetpack", 3),
        new Option("gain 250 gold and lose 50 hp", 4),
        new Option("leave", 6));

    // Choice 1037 is It's a Trap!  A Smashy Trap.
    new ChoiceAdventure(
        1037,
        "Spelunky Area",
        "It's a Trap!  A Smashy Trap.",
        // Option...
        new Option("unlock The City of Goooold with key, or take damage", 2),
        new Option("lose 40 hp", 6));

    // Choice 1038 is A Wicked Web
    new ChoiceAdventure(
        1038,
        "Spelunky Area",
        "A Wicked Web",
        // Option...
        new Option("gain 15-20 gold", 1),
        new Option("gain buddy (or 20-30 gold) with machete", 2),
        new Option("gain 30-50 gold with torch", 3));

    // Choice 1039 is A Golden Chest
    new ChoiceAdventure(
        1039,
        "Spelunky Area",
        "A Golden Chest",
        // Option...
        new Option("gain 150 gold with key", 1),
        new Option("gain 80-100 gold with bomb", 2),
        new Option("gain 50-60 gold and lose 20 hp", 3));

    // Choice 1040 is It's Lump. It's Lump.
    new ChoiceAdventure(
        1040,
        "Spelunky Area",
        "It's Lump. It's Lump",
        // Option...
        new Option("gain heavy pickaxe with bomb", 1, "heavy pickaxe"),
        new Option("leave", 6));

    // choice 1041 is Spelunkrifice
    new ChoiceAdventure(
        1041,
        "Spelunky Area",
        "Spelunkrifice",
        // Option...
        new Option("sacrifice buddy", 1),
        new Option("leave", 6));

    // choice 1042 is Pick a Perk!
    // choice 1044 is The Gates of Hell

    new ChoiceAdventure(
        1045,
        "Spelunky Area",
        "Hostile Work Environment",
        // Option...
        new Option("fight shopkeeper", 1),
        new Option("take damage", 6));

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
        new Option("gain office key, then ~35 meat", 1, "Skeleton Store office key"),
        new Option(
            "gain ring of telling skeletons what to do, then 300 meat, with skeleton key",
            2,
            "ring of telling skeletons what to do"),
        new Option("gain muscle stats", 3),
        new Option("fight former owner of the Skeleton Store, with office key", 4));

    // Choice 1061 is Heart of Madness
    new ChoiceAdventure(
        1061,
        "Town",
        "Madness Bakery",
        // Option...
        new Option("try to enter office", 1),
        new Option("bagel machine", 2),
        new Option("popular machine", 3),
        new Option("learn recipe", 4),
        new Option("gain mysticality stats", 5));

    // Choice 1062 is Lots of Options
    new ChoiceAdventure(
        1062,
        "Town",
        "Overgrown Lot",
        // Option...
        new Option("acquire flowers", 1),
        new Option("acquire food", 2),
        new Option("acquire drinks", 3),
        new Option("gain moxie stats", 4),
        new Option("acquire more booze with map", 5));

    // Choice 1063 is Adjust your 'Edpiece
    new ChoiceSpoiler(
        1063,
        "Crown of Ed the Undying",
        // Option...
        new Option("Muscle +20, +2 Muscle Stats Per Fight"),
        new Option("Mysticality +20, +2 Mysticality Stats Per Fight"),
        new Option("Moxie +20, +2 Moxie Stats Per Fight"),
        new Option("+20 to Monster Level"),
        new Option("+10% Item Drops from Monsters, +20% Meat from Monsters"),
        new Option(
            "The first attack against you will always miss, Regenerate 10-20 HP per Adventure"),
        new Option("Lets you breathe underwater"));

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
        new Option("gain stats and meat", 1),
        new Option("skip adventure and guarantees this adventure will reoccur", 6));

    // Choice 1076 is Mayo Minder&trade;

    // Choice 1080 is Bagelmat-5000
    new ChoiceAdventure(
        1080,
        "Town",
        "Bagelmat-5000",
        // Option...
        new Option("make 3 plain bagels using wad of dough", 1),
        new Option("return to Madness Bakery", 2));

    // Choice 1081 is Assault and Baguettery
    new ChoiceAdventure(
        1081,
        "Item-Driven",
        "magical baguette",
        // Option...
        new Option("breadwand", 1, "breadwand"),
        new Option("loafers", 2, "loafers"),
        new Option("bread basket", 3, "bread basket"),
        new Option("make nothing", 4));

    // Choice 1084 is The Popular Machine
    new ChoiceAdventure(
        1084,
        "Town",
        "Popular Machine",
        // Option...
        new Option("make popular tart", 1),
        new Option("return to Madness Bakery", 2));

    // Choice 1090 is The Towering Inferno Discotheque

    // Choice 1091 is The Floor Is Yours
    new ChoiceAdventure(
        1091,
        "That 70s Volcano",
        "LavaCo Lamp Factory",
        // Option...
        new Option("1,970 carat gold -> thin gold wire", 1, "thin gold wire"),
        new Option("New Age healing crystal -> empty lava bottle", 2, "empty lava bottle"),
        new Option("empty lava bottle -> full lava bottle", 3, "full lava bottle"),
        new Option("make colored lava globs", 4),
        new Option(
            "glowing New Age crystal -> crystalline light bulb", 5, "crystalline light bulb"),
        new Option(
            "crystalline light bulb + insulated wire + heat-resistant sheet metal -> LavaCo&trade; Lamp housing",
            6,
            "LavaCo&trade; Lamp housing"),
        new Option("fused fuse", 7, "fused fuse"),
        new Option("leave", 9));

    // Choice 1092 is Dyer Maker
    // Choice 1093 is The WLF Bunker

    // Choice 1094 is Back Room SMOOCHing
    new ChoiceAdventure(
        1094,
        "That 70s Volcano",
        "The SMOOCH Army HQ",
        // Option...
        new Option("fight Geve Smimmons", 1),
        new Option("fight Raul Stamley", 2),
        new Option("fight Pener Crisp", 3),
        new Option("fight Deuce Freshly", 4),
        new Option("acquire SMOOCH coffee cup", 5, "SMOOCH coffee cup"));

    // Choice 1095 is Tin Roof -- Melted
    new ChoiceAdventure(
        1095,
        "That 70s Volcano",
        "The Velvet / Gold Mine",
        // Option...
        new Option("fight Mr. Choch", 1),
        new Option("acquire half-melted hula girl", 2, "half-melted hula girl"));

    // Choice 1096 is Re-Factory Period
    new ChoiceAdventure(
        1096,
        "That 70s Volcano",
        "LavaCo Lamp Factory",
        // Option...
        new Option("fight Mr. Cheeng", 1),
        new Option("acquire glass ceiling fragments", 2, "glass ceiling fragments"));

    // Choice 1097 is Who You Gonna Caldera?
    new ChoiceAdventure(
        1097,
        "That 70s Volcano",
        "The Bubblin' Caldera",
        // Option...
        new Option("acquire The One Mood Ring", 1, "The One Mood Ring"),
        new Option("fight Lavalos", 2));

    // Choice 1102 is The Biggest Barrel

    // Choice 1106 is Wooof! Wooooooof!
    new ChoiceAdventure(
        1106,
        "Item-Driven",
        "Haunted Doghouse 1",
        // Option...
        new Option("gain stats", 1),
        new Option("+50% all stats for 30 turns", 2),
        new Option("acquire familiar food", 3, "Ghost Dog Chow"));

    // Choice 1107 is Playing Fetch*
    new ChoiceAdventure(
        1107,
        "Item-Driven",
        "Haunted Doghouse 2",
        // Option...
        new Option("acquire tennis ball", 1, "tennis ball"),
        new Option("+50% init for 30 turns", 2),
        new Option("acquire ~500 meat", 3));

    // Choice 1108 is Your Dog Found Something Again
    new ChoiceAdventure(
        1108,
        "Item-Driven",
        "Haunted Doghouse 3",
        // Option...
        new Option("acquire food", 1),
        new Option("acquire booze", 2),
        new Option("acquire cursed thing", 3));

    // Choice 1110 is Spoopy
    // Choice 1114 is Walford Rusley, Bucket Collector

    // Choice 1115 is VYKEA!
    new ChoiceAdventure(
        1115,
        "The Glaciest",
        "VYKEA!",
        // Option...
        new Option("acquire VYKEA meatballs and mead (1/day)", 1),
        new Option("acquire VYKEA hex key", 2, "VYKEA hex key"),
        new Option("fill bucket by 10-15%", 3),
        new Option("acquire 3 Wal-Mart gift certificates (1/day)", 4, "Wal-Mart gift certificate"),
        new Option("acquire VYKEA rune", 5),
        new Option("leave", 6));

    // Choice 1116 is All They Got Inside is Vacancy (and Ice)
    new ChoiceAdventure(
        1116,
        "The Glaciest",
        "All They Got Inside is Vacancy (and Ice)",
        // Option...
        new Option("fill bucket by 10-15%", 3),
        new Option("acquire cocktail ingredients", 4),
        new Option("acquire 3 Wal-Mart gift certificates (1/day)", 5, "Wal-Mart gift certificate"),
        new Option("leave", 6));

    // Choice 1118 is X-32-F Combat Training Snowman Control Console
    new ChoiceAdventure(
        1118,
        "The Snojo",
        "Control Console",
        // Option...
        new Option("muscle training", 1),
        new Option("mysticality training", 2),
        new Option("moxie training", 3),
        new Option("tournament", 4),
        new Option("leave", 6));

    // Choice 1119 is Shining Mauve Backwards In Time
    new ChoiceAdventure(
        1119,
        "Town",
        "Deep Machine Tunnels",
        // Option...
        new Option("acquire some abstractions", 1),
        new Option("acquire abstraction: comprehension", 2, "abstraction: comprehension"),
        new Option("acquire modern picture frame", 3, "modern picture frame"),
        new Option("duplicate one food, booze, spleen or potion", 4),
        new Option("leave", 6));

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
        new Option("fancy marzipan briefcase", 1, "fancy marzipan briefcase"),
        new Option("acquire 50 sprinkles and unlock judge fudge", 2, "sprinkles"),
        new Option("enter Civic Planning Office (costs 1000 sprinkles)", 3),
        new Option("acquire briefcase full of sprinkles (with gingerbread blackmail photos)", 4));

    // Choice 1203 is Midnight in Civic Center
    new ChoiceAdventure(
        1203,
        "Gingerbread City",
        "Midnight in the Civic Center",
        // Option...
        new Option("gain 500 mysticality", 1),
        new Option("acquire counterfeit city (costs 300 sprinkles)", 2, "counterfeit city"),
        new Option(
            "acquire gingerbread moneybag (with creme brulee torch)", 3, "gingerbread moneybag"),
        new Option(
            "acquire 5 gingerbread cigarettes (costs 5 sprinkles)", 4, "gingerbread cigarette"),
        new Option("acquire chocolate puppy (with gingerbread dog treat)", 5, "chocolate puppy"));

    // Choice 1204 is Noon at the Train Station
    new ChoiceAdventure(
        1204,
        "Gingerbread City",
        "Noon at the Train Station",
        // Option...
        new Option("gain 8-11 candies", 1),
        new Option("increase size of sewer gators (with sewer unlocked)", 2),
        new Option("gain 250 mysticality", 3));

    // Choice 1205 is Midnight at the Train Station
    new ChoiceAdventure(
        1205,
        "Gingerbread City",
        "Midnight at the Train Station",
        // Option...
        new Option("gain 500 muscle and add track", 1),
        new Option(
            "acquire broken chocolate pocketwatch (with pumpkin spice candle)",
            2,
            "broken chocolate pocketwatch"),
        new Option("enter The Currency Exchange (with candy crowbar)", 3),
        new Option(
            "acquire fruit-leather negatives (with track added)", 4, "fruit-leather negatives"),
        new Option("acquire various items (with teethpick)", 5));

    // Choice 1206 is Noon in the Industrial Zone
    new ChoiceAdventure(
        1206,
        "Gingerbread City",
        "Noon in the Industrial Zone",
        // Option...
        new Option("acquire creme brulee torch (costs 25 sprinkles)", 1, "creme brulee torch"),
        new Option("acquire candy crowbar (costs 50 sprinkles)", 2, "candy crowbar"),
        new Option("acquire candy screwdriver (costs 100 sprinkles)", 3, "candy screwdriver"),
        new Option("acquire teethpick (costs 1000 sprinkles after studying law)", 4, "teethpick"),
        new Option("acquire 400-600 sprinkles (with gingerbread mask, pistol and moneybag)", 5));

    // Choice 1207 is Midnight in the Industrial Zone
    new ChoiceAdventure(
        1207,
        "Gingerbread City",
        "Midnight in the Industrial Zone",
        // Option...
        new Option("enter Seedy Seedy Seedy", 1),
        new Option("enter The Factory Factor", 2),
        new Option("acquire tattoo (costs 100000 sprinkles)", 3));

    // Choice 1208 is Upscale Noon
    new ChoiceAdventure(
        1208,
        "Gingerbread City",
        "Upscale Noon",
        // Option...
        new Option(
            "acquire gingerbread dog treat (costs 200 sprinkles)", 1, "gingerbread dog treat"),
        new Option("acquire pumpkin spice candle (costs 150 sprinkles)", 2, "pumpkin spice candle"),
        new Option(
            "acquire gingerbread spice latte (costs 50 sprinkles)", 3, "gingerbread spice latte"),
        new Option("acquire gingerbread trousers (costs 500 sprinkles)", 4, "gingerbread trousers"),
        new Option(
            "acquire gingerbread waistcoat (costs 500 sprinkles)", 5, "gingerbread waistcoat"),
        new Option("acquire gingerbread tophat (costs 500 sprinkles)", 6, "gingerbread tophat"),
        new Option("acquire 400-600 sprinkles (with gingerbread mask, pistol and moneybag)", 7),
        new Option(
            "acquire gingerbread blackmail photos (drop off fruit-leather negatives and pick up next visit)",
            8,
            "gingerbread blackmail photos"),
        new Option("leave", 9));

    // Choice 1209 is Upscale Midnight
    new ChoiceAdventure(
        1209,
        "Gingerbread City",
        "Upscale Midnight",
        // Option...
        new Option("acquire fake cocktail", 1, "fake cocktail"),
        new Option("enter The Gingerbread Gallery (wearing Gingerbread Best", 2));

    // Choice 1210 is Civic Planning Office
    new ChoiceAdventure(
        1210,
        "Gingerbread City",
        "Civic Planning Office",
        // Option...
        new Option("unlock Gingerbread Upscale Retail District", 1),
        new Option("unlock Gingerbread Sewers", 2),
        new Option("unlock 10 extra City adventures", 3),
        new Option("unlock City Clock", 4));

    // Choice 1211 is The Currency Exchange
    new ChoiceAdventure(
        1211,
        "Gingerbread City",
        "The Currency Exchange",
        // Option...
        new Option("acquire 5000 meat", 1),
        new Option("acquire fat loot token", 2, "fat loot token"),
        new Option("acquire 250 sprinkles", 3, "sprinkles"),
        new Option("acquire priceless diamond", 4, "priceless diamond"),
        new Option("acquire 5 pristine fish scales)", 5, "pristine fish scales"));

    // Choice 1212 is Seedy Seedy Seedy
    new ChoiceAdventure(
        1212,
        "Gingerbread City",
        "Seedy Seedy Seedy",
        // Option...
        new Option("acquire gingerbread pistol (costs 300 sprinkles)", 1, "gingerbread pistol"),
        new Option("gain 500 moxie", 2),
        new Option("ginger beer (with gingerbread mug)", 3, "ginger beer"));

    // Choice 1213 is The Factory Factor
    new ChoiceAdventure(
        1213,
        "Gingerbread City",
        "The Factory Factor",
        // Option...
        new Option("acquire spare chocolate parts", 1, "spare chocolate parts"),
        new Option("fight GNG-3-R (with gingerservo", 2));

    // Choice 1214 is The Gingerbread Gallery
    new ChoiceAdventure(
        1214,
        "Gingerbread City",
        "The Gingerbread Gallery",
        // Option...
        new Option("acquire high-end ginger wine", 1, "high-end ginger wine"),
        new Option(
            "acquire fancy chocolate sculpture (costs 300 sprinkles)",
            2,
            "fancy chocolate sculpture"),
        new Option("acquire Pop Art: a Guide (costs 1000 sprinkles)", 3, "Pop Art: a Guide"),
        new Option("acquire No Hats as Art (costs 1000 sprinkles)", 4, "No Hats as Art"));

    // Choice 1215 is Setting the Clock
    new ChoiceAdventure(
        1215,
        "Gingerbread City",
        "Setting the Clock",
        // Option...
        new Option("move clock forward", 1),
        new Option("leave", 2));

    // Choice 1217 is Sweet Synthesis
    // Choice 1218 is Wax On

    // Choice 1222 is The Tunnel of L.O.V.E.

    // Choice 1223 is L.O.V. Entrance
    new ChoiceAdventure(
        1223,
        "Town",
        "L.O.V.E Fight 1",
        // Option...
        new Option("(free) fight LOV Enforcer", 1),
        new Option("avoid fight", 2));

    // Choice 1224 is L.O.V. Equipment Room
    new ChoiceAdventure(
        1224,
        "Town",
        "L.O.V.E Choice 1",
        // Option...
        new Option("acquire LOV Eardigan", 1, "LOV Eardigan"),
        new Option("acquire LOV Epaulettes", 2, "LOV Epaulettes"),
        new Option("acquire LOV Earrings", 3, "LOV Earrings"),
        new Option("take nothing", 4));

    // Choice 1225 is L.O.V. Engine Room
    new ChoiceAdventure(
        1225,
        "Town",
        "L.O.V.E Fight 2",
        // Option...
        new Option("(free) fight LOV Engineer", 1),
        new Option("avoid fight", 2));

    // Choice 1226 is L.O.V. Emergency Room
    new ChoiceAdventure(
        1226,
        "Town",
        "L.O.V.E Choice 2",
        // Option...
        new Option("50 adv of Lovebotamy (+10 stats/fight)", 1),
        new Option("50 adv of Open Heart Surgery (+10 fam weight)", 2),
        new Option("50 adv of Wandering Eye Surgery (+50 item drop)", 3),
        new Option("get no buff", 4));

    // Choice 1227 is L.O.V. Elbow Room
    new ChoiceAdventure(
        1227,
        "Town",
        "L.O.V.E Fight 3",
        // Option...
        new Option("(free) fight LOV Equivocator", 1),
        new Option("avoid fight", 2));

    // Choice 1228 is L.O.V. Emporium
    new ChoiceAdventure(
        1228,
        "Town",
        "L.O.V.E Choice 3",
        // Option...
        new Option("acquire LOV Enamorang", 1, "LOV Enamorang"),
        new Option("acquire LOV Emotionizer", 2, "LOV Emotionizer"),
        new Option("acquire LOV Extraterrestrial Chocolate", 3, "LOV Extraterrestrial Chocolate"),
        new Option("acquire LOV Echinacea Bouquet", 4, "LOV Echinacea Bouquet"),
        new Option("acquire LOV Elephant", 5, "LOV Elephant"),
        new Option("acquire 2 pieces of toast (if have Space Jellyfish)", 6, "toast"),
        new Option("take nothing", 7));

    // Choice 1229 is L.O.V. Exit

    // Choice 1236 is Space Cave
    new ChoiceAdventure(
        1236,
        "The Spacegate",
        "Space Cave",
        // Option...
        new Option("acquire some alien rock samples", 1, "alien rock sample"),
        new Option(
            "acquire some more alien rock samples (with geology kit)", 2, "alien rock sample"),
        new Option("skip adventure", 6));

    // Choice 1237 is A Simple Plant
    new ChoiceAdventure(
        1237,
        "The Spacegate",
        "A Simple Plant",
        // Option...
        new Option("acquire edible alien plant bit", 1, "edible alien plant bit"),
        new Option("acquire alien plant fibers", 2, "alien plant fibers"),
        new Option("acquire alien plant sample (with botany kit)", 3, "alien plant sample"),
        new Option("skip adventure", 6));

    // Choice 1238 is A Complicated Plant
    new ChoiceAdventure(
        1238,
        "The Spacegate",
        "A Complicated Plant",
        // Option...
        new Option("acquire some edible alien plant bit", 1, "edible alien plant bit"),
        new Option("acquire some alien plant fibers", 2, "alien plant fibers"),
        new Option(
            "acquire complex alien plant sample (with botany kit)",
            3,
            "complex alien plant sample"),
        new Option("skip adventure", 6));

    // Choice 1239 is What a Plant!
    new ChoiceAdventure(
        1239,
        "The Spacegate",
        "What a Plant!",
        // Option...
        new Option("acquire some edible alien plant bit", 1, "edible alien plant bit"),
        new Option("acquire some alien plant fibers", 2, "alien plant fibers"),
        new Option(
            "acquire fascinating alien plant sample (with botany kit)",
            3,
            "fascinating alien plant sample"),
        new Option("skip adventure", 6));

    // Choice 1240 is The Animals, The Animals
    new ChoiceAdventure(
        1240,
        "The Spacegate",
        "The Animals, The Animals",
        // Option...
        new Option("acquire alien meat", 1, "alien meat"),
        new Option("acquire alien toenails", 2, "alien toenails"),
        new Option(
            "acquire alien zoological sample (with zoology kit)", 3, "alien zoological sample"),
        new Option("skip adventure", 6));

    // Choice 1241 is Buffalo-Like Animal, Won't You Come Out Tonight
    new ChoiceAdventure(
        1241,
        "The Spacegate",
        "Buffalo-Like Animal, Won't You Come Out Tonight",
        // Option...
        new Option("acquire some alien meat", 1, "alien meat"),
        new Option("acquire some alien toenails", 2, "alien toenails"),
        new Option(
            "acquire complex alien zoological sample (with zoology kit)",
            3,
            "complex alien zoological sample"),
        new Option("skip adventure", 6));

    // Choice 1242 is House-Sized Animal
    new ChoiceAdventure(
        1242,
        "The Spacegate",
        "House-Sized Animal",
        // Option...
        new Option("acquire some alien meat", 1, "alien meat"),
        new Option("acquire some alien toenails", 2, "alien toenails"),
        new Option(
            "acquire fascinating alien zoological sample (with zoology kit)",
            3,
            "fascinating alien zoological sample"),
        new Option("skip adventure", 6));

    // Choice 1243 is Interstellar Trade
    new ChoiceAdventure(
        1243,
        "The Spacegate",
        "Interstellar Trade",
        // Option...
        new Option("purchase item", 1),
        new Option("leave", 6));

    // Choice 1244 is Here There Be No Spants
    new ChoiceAdventure(
        1244,
        "The Spacegate",
        "Here There Be No Spants",
        // Option...
        new Option("acquire spant egg casing", 1, "spant egg casing"));

    // Choice 1245 is Recovering the Satellites
    new ChoiceAdventure(
        1245,
        "The Spacegate",
        "Recovering the Satellite",
        // Option...
        new Option("acquire murderbot data core", 1, "murderbot data core"));

    // Choice 1246 is Land Ho
    new ChoiceAdventure(
        1246,
        "The Spacegate",
        "Land Ho",
        // Option...
        new Option("gain 10% Space Pirate language", 1),
        new Option("leave", 6));

    // Choice 1247 is Half The Ship it Used to Be
    new ChoiceAdventure(
        1247,
        "The Spacegate",
        "Half The Ship it Used to Be",
        // Option...
        new Option(
            "acquire space pirate treasure map (with enough Space Pirate language)",
            1,
            "space pirate treasure map"),
        new Option("leave", 6));

    // Choice 1248 is Paradise Under a Strange Sun
    new ChoiceAdventure(
        1248,
        "The Spacegate",
        "Paradise Under a Strange Sun",
        // Option...
        new Option(
            "acquire Space Pirate Astrogation Handbook (with space pirate treasure map)",
            1,
            "Space Pirate Astrogation Handbook"),
        new Option("gain 1000 moxie stats", 2),
        new Option("leave", 6));

    // Choice 1249 is That's No Moonlith, it's a Monolith!
    new ChoiceAdventure(
        1249,
        "The Spacegate",
        "That's No Moonlith, it's a Monolith!",
        // Option...
        new Option("gain 20% procrastinator language (with murderbot data core)", 1),
        new Option("leave", 6));

    // Choice 1250 is I'm Afraid It's Terminal
    new ChoiceAdventure(
        1250,
        "The Spacegate",
        "I'm Afraid It's Terminal",
        // Option...
        new Option(
            "acquire procrastinator locker key (with enough procrastinator language)",
            1,
            "Procrastinator locker key"),
        new Option("leave", 6));

    // Choice 1251 is Curses, a Hex
    new ChoiceAdventure(
        1251,
        "The Spacegate",
        "Curses, a Hex",
        // Option...
        new Option(
            "acquire Non-Euclidean Finance (with procrastinator locker key)",
            1,
            "Non-Euclidean Finance"),
        new Option("leave", 6));

    // Choice 1252 is Time Enough at Last
    new ChoiceAdventure(
        1252,
        "The Spacegate",
        "Time Enough at Last",
        // Option...
        new Option("acquire Space Baby childrens' book", 1, "Space Baby childrens' book"),
        new Option("leave", 6));

    // Choice 1253 is Mother May I
    new ChoiceAdventure(
        1253,
        "The Spacegate",
        "Mother May I",
        // Option...
        new Option(
            "acquire Space Baby bawbaw (with enough Space Baby language)", 1, "Space Baby bawbaw"),
        new Option("leave", 6));

    // Choice 1254 is Please Baby Baby Please
    new ChoiceAdventure(
        1254,
        "The Spacegate",
        "Please Baby Baby Please",
        // Option...
        new Option("acquire Peek-a-Boo! (with Space Baby bawbaw)", 1, "Peek-a-Boo!"),
        new Option("leave", 6));

    // Choice 1255 is Cool Space Rocks
    new ChoiceAdventure(
        1255,
        "The Spacegate",
        "Cool Space Rocks",
        // Option...
        new Option("acquire some alien rock samples", 1, "alien rock sample"),
        new Option(
            "acquire some more alien rock samples (with geology kit)", 2, "alien rock sample"));

    // Choice 1256 is Wide Open Spaces
    new ChoiceAdventure(
        1256,
        "The Spacegate",
        "Wide Open Spaces",
        // Option...
        new Option("acquire some alien rock samples", 1, "alien rock sample"),
        new Option(
            "acquire some more alien rock samples (with geology kit)", 2, "alien rock sample"));

    // Choice 1280 is Welcome to FantasyRealm
    new ChoiceAdventure(
        1280,
        "FantasyRealm",
        "Welcome to FantasyRealm",
        // Option...
        new Option("acquire FantasyRealm Warrior's Helm", 1, "FantasyRealm Warrior's Helm"),
        new Option("acquire FantasyRealm Mage's Hat", 2, "FantasyRealm Mage's Hat"),
        new Option("acquire FantasyRealm Rogue's Mask", 3, "FantasyRealm Rogue's Mask"),
        new Option("leave", 6));

    // Choice 1281 is You'll See You at the Crossroads
    new ChoiceAdventure(
        1281,
        "FantasyRealm",
        "You'll See You at the Crossroads",
        // Option...
        new Option("unlock The Towering Mountains", 1),
        new Option("unlock The Mystic Wood", 2),
        new Option("unlock The Putrid Swamp", 3),
        new Option("unlock Cursed Village", 4),
        new Option("unlock The Sprawling Cemetery", 5),
        new Option("leave", 8));

    // Choice 1282 is Out of Range
    new ChoiceAdventure(
        1282,
        "FantasyRealm",
        "Out of Range",
        // Option...
        new Option("unlock The Old Rubee Mine (using FantasyRealm key)", 1),
        new Option("unlock The Foreboding Cave", 2),
        new Option("unlock The Master Thief's Chalet (with FantasyRealm Rogue's Mask)", 3),
        new Option("charge druidic orb (need orb)", 4, "charged druidic orb"),
        new Option("unlock The Ogre Chieftain's Keep (with FantasyRealm Warrior's Helm)", 5),
        new Option("1/5 to fight Skeleton Lord (with FantasyRealm outfit)", 10),
        new Option("leave", 11));

    // Choice 1283 is Where Wood You Like to Go
    new ChoiceAdventure(
        1283,
        "FantasyRealm",
        "Where Wood You Like to Go",
        // Option...
        new Option("unlock The Faerie Cyrkle", 1),
        new Option("unlock The Druidic Campsite (with LyleCo premium rope)", 2),
        new Option("unlock The Ley Nexus (with Cheswick Copperbottom's compass)", 3),
        new Option("acquire plump purple mushroom", 5, "plump purple mushroom"),
        new Option("1/5 to fight Skeleton Lord (with FantasyRealm outfit)", 10),
        new Option("leave", 11));

    // Choice 1284 is Swamped with Leisure
    new ChoiceAdventure(
        1284,
        "FantasyRealm",
        "Swamped with Leisure",
        // Option...
        new Option("unlock Near the Witch's House", 1),
        new Option("unlock The Troll Fortress (using FantasyRealm key)", 2),
        new Option("unlock The Dragon's Moor (with FantasyRealm Warrior's Helm)", 3),
        new Option("acquire tainted marshmallow", 5, "tainted marshmallow"),
        new Option("1/5 to fight Skeleton Lord (with FantasyRealm outfit)", 10),
        new Option("leave", 11));

    // Choice 1285 is It Takes a Cursed Village
    new ChoiceAdventure(
        1285,
        "FantasyRealm",
        "It Takes a Cursed Village",
        // Option...
        new Option("unlock The Evil Cathedral", 1),
        new Option("unlock The Cursed Village Thieves' Guild (using FantasyRealm Rogue's Mask)", 2),
        new Option("unlock The Archwizard's Tower (with FantasyRealm Mage's Hat)", 3),
        new Option("get 20 adv of +2-3 Rubee&trade; drop", 4),
        new Option("acquire 40-60 Rubees&trade; (with LyleCo premium rope)", 5, "Rubee&trade;"),
        new Option(
            "acquire dragon slaying sword (with dragon aluminum ore)", 6, "dragon slaying sword"),
        new Option(
            "acquire notarized arrest warrant (with arrest warrant)",
            7,
            "notarized arrest warrant"),
        new Option("1/5 to fight Skeleton Lord (with FantasyRealm outfit)", 10),
        new Option("leave", 11));

    // Choice 1286 is Resting in Peace
    new ChoiceAdventure(
        1286,
        "FantasyRealm",
        "Resting in Peace",
        // Option...
        new Option("unlock The Labyrinthine Crypt", 1),
        new Option("unlock The Barrow Mounds", 2),
        new Option("unlock Duke Vampire's Chateau (with FantasyRealm Rogue's Mask)", 3),
        new Option("acquire 40-60 Rubees&trade; (need LyleCo premium pickaxe)", 4, "Rubee&trade;"),
        new Option(
            "acquire Chewsick Copperbottom's notes (with FantasyRealm Mage's Hat)",
            5,
            "Chewsick Copperbottom's notes"),
        new Option("1/5 to fight Skeleton Lord (with FantasyRealm outfit)", 10),
        new Option("leave", 11));

    // Choice 1288 is What's Yours is Yours
    new ChoiceAdventure(
        1288,
        "FantasyRealm",
        "What's Yours is Yours",
        // Option...
        new Option("acquire 20-30 Rubees&trade;", 1, "Rubee&trade;"),
        new Option(
            "acquire dragon aluminum ore (need LyleCo premium pickaxe)", 2, "dragon aluminum ore"),
        new Option("acquire grolblin rum", 3, "grolblin rum"),
        new Option("leave", 6));

    // Choice 1289 is A Warm Place
    new ChoiceAdventure(
        1289,
        "FantasyRealm",
        "A Warm Place",
        // Option...
        new Option("acquire 90-110 Rubees&trade; (with FantasyRealm key)", 1, "Rubee&trade;"),
        new Option("acquire sachet of strange powder", 2, "sachet of strange powder"),
        new Option("unlock The Lair of the Phoenix (with FantasyRealm Mage's Hat)", 3),
        new Option("leave", 6));

    // Choice 1290 is The Cyrkle Is Compleat
    new ChoiceAdventure(
        1290,
        "FantasyRealm",
        "The Cyrkle Is Compleat",
        // Option...
        new Option("get 100 adv of Fantasy Faerie Blessing", 1),
        new Option("acquire faerie dust", 2, "faerie dust"),
        new Option("unlock The Spider Queen's Lair (with FantasyRealm Rogue's Mask)", 3),
        new Option("leave", 6));

    // Choice 1291 is Dudes, Where's My Druids?
    new ChoiceAdventure(
        1291,
        "FantasyRealm",
        "Dudes, Where's My Druids?",
        // Option...
        new Option("acquire druidic s'more", 1, "druidic s'more"),
        new Option(
            "acquire poisoned druidic s'more (with tainted marshmallow)",
            2,
            "poisoned druidic s'more"),
        new Option("acquire druidic orb (with FantasyRealm Mage's Hat)", 3, "druidic orb"),
        new Option("leave", 6));

    // Choice 1292 is Witch One You Want?
    new ChoiceAdventure(
        1292,
        "FantasyRealm",
        "Witch One You Want?",
        // Option...
        new Option("get 50 adv of +200% init", 1),
        new Option("get 10 adv of Poison for Blood (with plump purple mushroom)", 2),
        new Option("acquire to-go brew", 3, "to-go brew"),
        new Option("acquire 40-60 Rubees&trade;", 4, "Rubee&trade;"),
        new Option("leave", 6));

    // Choice 1293 is Altared States
    new ChoiceAdventure(
        1293,
        "FantasyRealm",
        "Altared States",
        // Option...
        new Option("acquire 20-30 Rubees&trade;", 1, "Rubee&trade;"),
        new Option("get 100 adv of +200% HP", 2),
        new Option("acquire sanctified cola", 3, "sanctified cola"),
        new Option(
            "acquire flask of holy water (with FantasyRealm Mage's Hat)", 4, "flask of holy water"),
        new Option("leave", 6));

    // Choice 1294 is Neither a Barrower Nor a Lender Be
    new ChoiceAdventure(
        1294,
        "FantasyRealm",
        "Neither a Barrower Nor a Lender Be",
        // Option...
        new Option("acquire 20-30 Rubees&trade;", 1, "Rubee&trade;"),
        new Option("acquire mourning wine", 2, "mourning wine"),
        new Option("unlock The Ghoul King's Catacomb (with FantasyRealm Warrior's Helm)", 3),
        new Option("leave", 6));

    // Choice 1295 is Honor Among You
    new ChoiceAdventure(
        1295,
        "FantasyRealm",
        "Honor Among You",
        // Option...
        new Option("acquire 40-60 Rubees&trade;", 1, "Rubee&trade;"),
        new Option("acquire universal antivenin", 2, "universal antivenin"),
        new Option("leave", 6));

    // Choice 1296 is For Whom the Bell Trolls
    new ChoiceAdventure(
        1296,
        "FantasyRealm",
        "For Whom the Bell Trolls",
        // Option...
        new Option("nothing happens", 1),
        new Option("acquire nasty haunch", 2, "nasty haunch"),
        new Option(
            "acquire Cheswick Copperbottom's compass (with Chewsick Copperbottom's notes)",
            3,
            "Cheswick Copperbottom's compass"),
        new Option("acquire 40-60 Rubees&trade; (with LyleCo premium pickaxe)", 4, "Rubee&trade;"),
        new Option("leave", 6));

    // Choice 1297 is Stick to the Crypt
    new ChoiceAdventure(
        1297,
        "FantasyRealm",
        "Stick to the Crypt",
        // Option...
        new Option("acquire hero's skull", 1, "hero's skull"),
        new Option("acquire 40-60 Rubees&trade;", 2, "Rubee&trade;"),
        new Option("acquire arrest warrant (with FantasyRealm Rogue's Mask)", 3, "arrest warrant"),
        new Option("leave", 6));

    // Choice 1298 is The "Phoenix"
    new ChoiceAdventure(
        1298,
        "FantasyRealm",
        "The \"Phoenix\"",
        // Option...
        new Option("fight \"Phoenix\" (with 5+ hot res and flask of holy water)", 1),
        new Option("get beaten up", 2),
        new Option("leave", 6));

    // Choice 1299 is Stop Dragon Your Feet
    new ChoiceAdventure(
        1299,
        "FantasyRealm",
        "Stop Dragon Your Feet",
        // Option...
        new Option(
            "fight Sewage Treatment Dragon (with 5+ stench res and dragon slaying sword)", 1),
        new Option("get beaten up", 2),
        new Option("leave", 6));

    // Choice 1300 is Just Vamping
    new ChoiceAdventure(
        1300,
        "FantasyRealm",
        "Just Vamping",
        // Option...
        new Option("fight Duke Vampire (with 250%+ init and Poison for Blood)", 1),
        new Option("get beaten up", 2),
        new Option("leave", 6));

    // Choice 1301 is Now You've Spied Her
    new ChoiceAdventure(
        1301,
        "FantasyRealm",
        "Now You've Spied Her",
        // Option...
        new Option("fight Spider Queen (with 500+ mox and Fantastic Immunity)", 1),
        new Option("get beaten up", 2),
        new Option("leave", 6));

    // Choice 1302 is Don't Be Arch
    new ChoiceAdventure(
        1302,
        "FantasyRealm",
        "Don't Be Arch",
        // Option...
        new Option("fight Archwizard (with 5+ cold res and charged druidic orb)", 1),
        new Option("get beaten up", 2),
        new Option("leave", 6));

    // Choice 1303 is Ley Lady Ley
    new ChoiceAdventure(
        1303,
        "FantasyRealm",
        "Ley Lady Ley",
        // Option...
        new Option("fight Ley Incursion (with 500+ mys and Cheswick Copperbottom's compass)", 1),
        new Option("get beaten up", 2),
        new Option("leave", 6));

    // Choice 1304 is He Is the Ghoul King, He Can Do Anything
    new ChoiceAdventure(
        1304,
        "FantasyRealm",
        "He Is the Ghoul King, He Can Do Anything",
        // Option...
        new Option("fight Ghoul King (with 5+ spooky res and Fantasy Faerie Blessing)", 1),
        new Option("get beaten up", 2),
        new Option("leave", 6));

    // Choice 1305 is The Brogre's Progress
    new ChoiceAdventure(
        1305,
        "FantasyRealm",
        "The Brogre's Progress",
        // Option...
        new Option("fight Ogre Chieftain (with 500+ mus and poisoned druidic s'more)", 1),
        new Option("get beaten up", 2),
        new Option("leave", 6));

    // Choice 1307 is It Takes a Thief
    new ChoiceAdventure(
        1307,
        "FantasyRealm",
        "It Takes a Thief",
        // Option...
        new Option(
            "fight Ted Schwartz, Master Thief (with 5+ sleaze res and notarized arrest warrant)",
            1),
        new Option("get beaten up", 2),
        new Option("leave", 6));

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
        "Town",
        "Neverending Party Intro",
        // Option...
        new Option("accept quest", 1),
        new Option("reject quest", 2),
        new Option("leave", 6));

    // Choice 1323 is All Done!

    // Choice 1324 is It Hasn't Ended, It's Just Paused
    new ChoiceAdventure(
        1324,
        "Town",
        "Neverending Party Pause",
        // Option...
        new Option(
            "Full HP/MP heal, +Mys Exp (20adv), clear partiers (quest), DJ meat (quest), megawoots (quest)",
            1),
        new Option("Mys stats, +Mus Exp (20 adv), snacks quest, burn trash (quest)", 2),
        new Option("Mox stats, +30 ML (50 adv), clear partiers (quest), booze quest", 3),
        new Option("Mus stats, +Mox Exp (20 adv), chainsaw, megawoots (quest)", 4),
        new Option("fight random partier", 5));

    // Choice 1325 is A Room With a View...  Of a Bed
    new ChoiceAdventure(
        1325,
        "Town",
        "Neverending Party Bedroom",
        // Option...
        new Option("full HP/MP heal", 1),
        new Option("get 20 adv of +20% mys exp", 2),
        new Option("remove partiers (with jam band bootleg)", 3),
        new Option("get meat for dj (with 300 Moxie)", 4),
        new Option("increase megawoots", 5));

    // Choice 1326 is Gone Kitchin'
    new ChoiceAdventure(
        1326,
        "Town",
        "Neverending Party Kitchen",
        // Option...
        new Option("gain mys stats", 1),
        new Option("get 20 adv of +20% Mus exp", 2),
        new Option("find out food to collect", 3),
        new Option("give collected food", 4),
        new Option("reduce trash", 5));

    // Choice 1327 is Forward to the Back
    new ChoiceAdventure(
        1327,
        "Town",
        "Neverending Party Back Yard",
        // Option...
        new Option("gain mox stats", 1),
        new Option("get 50 adv of +30 ML", 2),
        new Option("find out booze to collect", 3),
        new Option("give collected booze", 4),
        new Option("remove partiers (with Purple Beast energy drink)", 5));

    // Choice 1328 is Basement Urges
    new ChoiceAdventure(
        1328,
        "Town",
        "Neverending Party Basement",
        // Option...
        new Option("gain mus stats", 1),
        new Option("get 20 adv of +20% Mox exp", 2),
        new Option("acquire intimidating chainsaw", 3, "intimidating chainsaw"),
        new Option("increase megawoots", 4));

    // Choice 1331 is Daily Loathing Ballot
    // Choice 1332 is government requisition form

    // Choice 1333 is Canadian Cabin
    new ChoiceAdventure(
        1333,
        "Crimbo18",
        "Canadian Cabin",
        // Option...
        new Option("gain 50 adv of +100% weapon and spell damage", 1),
        new Option("acquire grilled mooseflank (with mooseflank)", 2, "grilled mooseflank"),
        new Option(
            "acquire antique Canadian lantern (with 10 thick walrus blubber)",
            3,
            "antique Canadian lantern"),
        new Option("acquire muskox-skin cap (with 10 tiny bombs)", 4, "muskox-skin cap"),
        new Option("acquire antique beer (with Yeast-Hungry)", 5, "antique beer"),
        new Option("skip adventure", 10));

    // Choice 1334 is Boxing Daycare (Lobby)
    // Choice 1335 is Boxing Day Spa
    new ChoiceAdventure(
        1335,
        "Town",
        "Boxing Day Spa",
        // Option...
        new Option("gain 100 adv of +200% muscle and +15 ML"),
        new Option("gain 100 adv of +200% moxie and +50% init"),
        new Option("gain 100 adv of +200% myst and +25% item drop"),
        new Option(
            "gain 100 adv of +100 max hp, +50 max mp, +25 dr, 5-10 mp regen, 10-20 hp regen"),
        new Option("skip"));

    // Choice 1336 is Boxing Daycare
    // Choice 1339 is A Little Pump and Grind

    // Choice 1340 is Is There A Doctor In The House?
    new ChoiceAdventure(
        1340,
        "Item-Driven",
        "Lil' Doctor&trade; bag Quest",
        // Option...
        new Option("get quest", 1),
        new Option("refuse quest", 2),
        new Option("stop offering quest", 3));

    // Choice 1341 is A Pound of Cure
    new ChoiceAdventure(
        1341,
        "Item-Driven",
        "Lil' Doctor&trade; bag Cure",
        // Option...
        new Option("cure patient", 1));

    // Choice 1342 is Torpor

    // Choice 1345 is Blech House
    new ChoiceAdventure(
        1345,
        "Mountain",
        "Blech House",
        // Option...
        new Option("use muscle/weapon damage", 1),
        new Option("use myst/spell damage", 2),
        new Option("use mox/sleaze res", 3));

    // Choice 1392 is Decorate your Tent
    new ChoiceSpoiler(
        1392,
        "Decorate your Tent",
        // Option...
        new Option("gain 20 adv of +3 mus xp"),
        new Option("gain 20 adv of +3 mys xp"),
        new Option("gain 20 adv of +3 mox xp"));

    // Choice 1397 is Kringle workshop
    new ChoiceAdventure(
        1397,
        "Tammy's Offshore Platform",
        "Kringle workshop",
        // Option...
        new Option("craft stuff", 1),
        new Option("get waterlogged items", 2),
        new Option("fail at life", 3));

    // Choice 1411 is The Hall in the Hall
    new ChoiceAdventure(
        1411,
        "The Drip",
        "The Hall in the Hall",
        // Option...
        new Option("drippy pool table", 1),
        new Option("drippy vending machine", 2),
        new Option("drippy humanoid", 3),
        new Option("drippy keg", 4),
        new Option("Driplets", 5));

    // Choice 1415 is Revolting Vending
    new ChoiceAdventure(
        1415,
        "The Drip",
        "Revolting Vending",
        // Option...
        new Option("drippy candy bar", 1, "drippy candy bar"),
        new Option("Driplets", 2));
    new ChoiceCost(1415, new Cost(1, new AdventureResult(AdventureResult.MEAT, -10000)));

    // Choice 1427 is The Hidden Junction
    new ChoiceAdventure(
        1427,
        "BatHole",
        "The Hidden Junction",
        // Option...
        new Option("fight screambat", 1),
        new Option("gain ~360 meat", 2));

    // Choice 1428 is Your Neck of the Woods
    new ChoiceAdventure(
        1428,
        "Friars",
        "Your Neck of the Woods",
        // Option...
        new Option("advance quest 1 step and gain 1000 meat", 1),
        new Option("advance quest 2 steps", 2));

    // Choice 1429 is No Nook Unknown
    new ChoiceAdventure(
        1429,
        "Cyrpt",
        "No Nook Unknown",
        // Option...
        new Option("acquire 2 evil eyes", 1),
        new Option("fight party skeleton", 2));

    // Choice 1430 is Ghostly Memories
    new ChoiceAdventure(
        1430,
        "Highlands",
        "Ghostly Memories",
        // Option...
        new Option("the Horror, spooky/cold res recommended", 1),
        new Option("fight oil baron", 2),
        new Option("lost overlook lodge", 3));

    // Choice 1431 is Here There Be Giants
    new ChoiceAdventure(
        1431,
        "Beanstalk",
        "Here There Be Giants",
        // Option...
        new Option("complete trash quest, unlock HiTS", 1),
        new Option("fight goth giant, acquire black candles", 2),
        new Option("fight raver, restore hp/mp", 3),
        new Option("complete quest w/ mohawk wig, gain ~500 meat", 4));

    // Choice 1432 is Mob Maptality
    new ChoiceAdventure(
        1432,
        "The Red Zeppelin's Mooring",
        "Mob Maptality",
        // Option...
        new Option("creep protestors (more with sleaze damage/sleaze spell damage)", 1),
        new Option("scare protestors (more with lynyrd gear)", 2),
        new Option("set fire to protestors (more with Flamin' Whatshisname)", 3));

    // Choice 1433 is Hippy camp verge of war Sneaky Sneaky
    new ChoiceAdventure(
        1433,
        "Island",
        "Sneaky Sneaky",
        // Option...
        new Option("fight a war hippy drill sergeant", 1),
        new Option("fight a war hippy space cadet", 2),
        new Option("start the war", 3));

    // Choice 1434 is frat camp verge of war Sneaky Sneaky
    new ChoiceAdventure(
        1434,
        "Island",
        "Sneaky Sneaky",
        // Option...
        new Option("fight a war pledge/acquire sake bombs", 1),
        new Option("start the war", 2),
        new Option("fight a frat warrior drill sergeant/acquire beer bombs", 3));

    // Choice 1436 is Billiards Room Options
    new ChoiceAdventure(
        1436,
        "Manor1",
        "Billiards Room Options",
        // Option...
        new Option("aquire pool cue", 1),
        new Option("play pool with the ghost", 2),
        new Option("fight a chalkdust wraith", 3));

    // Gift Fabrication Lab
    new ChoiceAdventure(
        1460,
        "Crimbo21",
        "Site Alpha Toy Lab",
        // Option...
        new Option("fleshy putty", "fleshy putty", "third ear", "festive egg sac"),
        new Option(
            "poisonsettia", "poisonsettia", "peppermint-scented socks", "the Crymbich Manuscript"),
        new Option(
            "projectile chemistry set",
            "projectile chemistry set",
            "depleted Crimbonium football helmet",
            "synthetic rock"),
        new Option(
            "&quot;caramel&quot; orange",
            "&quot;caramel&quot; orange",
            "self-repairing earmuffs",
            "carnivorous potted plant"),
        new Option("universal biscuit", "universal biscuit", "yule hatchet", "potato alarm clock"),
        new Option("lab-grown meat", "lab-grown meat", "golden fleece", "boxed gumball machine"),
        new Option("cloning kit", "cloning kit", "electric pants", "can of mixed everything"),
        new Option("return to Site Alpha"));

    // Hello Knob My Old Friend
    new ChoiceAdventure(
        1461,
        "Crimbo21",
        "Site Alpha Primary Lab",
        // Option...
        new Option("Increase goo intensity", 1),
        new Option("Decrease goo intensity", 2),
        new Option("Trade grey goo ring for gooified matter", 3),
        new Option("Do nothing", 4),
        new Option("Grab the cheer core. Just do it!", 5));
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

  public static final void decorateChoice(final int choice, final StringBuffer buffer) {
    if (choice >= 48 && choice <= 70) {
      // Add "Go To Goal" button for the Violet Fog
      VioletFogManager.addGoalButton(buffer);
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
      if (photo.equals("photo1")) {
        if (find.contains("2259")) {
          replace =
              StringUtilities.singleStringReplace(
                  find, "<option value=\"2259\">", "<option value=\"2259\" selected>");
        }
      } else if (photo.equals("photo2")) {
        if (find.contains("7264")) {
          replace =
              StringUtilities.singleStringReplace(
                  find, "<option value=\"7264\">", "<option value=\"7264\" selected>");
        }
      } else if (photo.equals("photo3")) {
        if (find.contains("7263")) {
          replace =
              StringUtilities.singleStringReplace(
                  find, "<option value=\"7263\">", "<option value=\"7263\" selected>");
        }
      } else if (photo.equals("photo4")) {
        if (find.contains("7265")) {
          replace =
              StringUtilities.singleStringReplace(
                  find, "<option value=\"7265\">", "<option value=\"7265\" selected>");
        }
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

  public static final String parseVoteSpeech(final String party, final String speech) {
    if (party.contains("Pork Elf Historical Preservation Party")) {
      if (speech.contains("strict curtailing of unnatural modern technologies")) {
        return "government bureaucrat";
      } else if (speech.contains("reintroduce Pork Elf DNA")) {
        return "terrible mutant";
      } else if (speech.contains("kingdom-wide seance")) {
        return "angry ghost";
      } else if (speech.contains("very interested in snakes")) {
        return "annoyed snake";
      } else if (speech.contains("lots of magical lard")) {
        return "slime blob";
      }
    } else if (party.contains("Clan Ventrilo")) {
      if (speech.contains("bringing this blessing to the entire population")) {
        return "slime blob";
      } else if (speech.contains("see your deceased loved ones again")) {
        return "angry ghost";
      } else if (speech.contains("stronger and more vigorous")) {
        return "terrible mutant";
      } else if (speech.contains("implement healthcare reforms")) {
        return "government bureaucrat";
      } else if (speech.contains("flavored drink in a tube")) {
        return "annoyed snake";
      }
    } else if (party.contains("Bureau of Efficient Government")) {
      if (speech.contains("graveyards are a terribly inefficient use of space")) {
        return "angry ghost";
      } else if (speech.contains("strictly enforced efficiency laws")) {
        return "government bureaucrat";
      } else if (speech.contains("distribute all the medications for all known diseases ")) {
        return "terrible mutant";
      } else if (speech.contains("introduce an influx of snakes")) {
        return "annoyed snake";
      } else if (speech.contains("releasing ambulatory garbage-eating slimes")) {
        return "slime blob";
      }
    } else if (party.contains("Scions of Ich'Xuul'kor")) {
      if (speech.contains("increase awareness of our really great god")) {
        return "terrible mutant";
      } else if (speech.contains("hunt these evil people down")) {
        return "government bureaucrat";
      } else if (speech.contains("sound of a great hissing")) {
        return "annoyed snake";
      } else if (speech.contains("make things a little bit more like he's used to")) {
        return "slime blob";
      } else if (speech.contains("kindness energy")) {
        return "angry ghost";
      }
    } else if (party.contains("Extra-Terrific Party")) {
      if (speech.contains("wondrous chemical")) {
        return "terrible mutant";
      } else if (speech.contains("comprehensive DNA harvesting program")) {
        return "government bureaucrat";
      } else if (speech.contains("mining and refining processes begin")) {
        return "slime blob";
      } else if (speech.contains("warp engines will not destabilize")) {
        return "angry ghost";
      } else if (speech.contains("breeding pair of these delightful creatures")) {
        return "annoyed snake";
      }
    }

    return null;
  }

  public static final void decorateVote(final StringBuffer buffer) {
    Matcher matcher = VOTE_SPEECH_PATTERN.matcher(buffer.toString());

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

  private static final Option FLICKERING_PIXEL = new Option("flickering pixel");

  public static final Option[] dynamicChoiceOptions(final int choice) {
    Option[] result;
    switch (choice) {
      case 5:
        // Heart of Very, Very Dark Darkness
        result = new Option[2];

        boolean rock = InventoryManager.getCount(ItemPool.INEXPLICABLY_GLOWING_ROCK) >= 1;

        result[0] =
            new Option("You " + (rock ? "" : "DON'T ") + " have an inexplicably glowing rock");
        result[1] = SKIP_ADVENTURE;

        return result;

      case 7:
        // How Depressing
        result = new Option[2];

        boolean glove = KoLCharacter.hasEquipped(ItemPool.get(ItemPool.SPOOKY_GLOVE, 1));

        result[0] = new Option("spooky glove " + (glove ? "" : "NOT ") + "equipped");
        result[1] = SKIP_ADVENTURE;

        return result;

      case 184:
        // That Explains All The Eyepatches
        result = new Option[6];

        // The choices are based on character class.
        // Mus: combat, shot of rotgut (2948), drunkenness
        // Mys: drunkenness, shot of rotgut (2948), shot of rotgut (2948)
        // Mox: combat, drunkenness, shot of rotgut (2948)

        result[0] =
            new Option(
                KoLCharacter.isMysticalityClass()
                    ? "3 drunk and stats (varies by class)"
                    : "enter combat (varies by class)");
        result[1] =
            new Option(
                KoLCharacter.isMoxieClass()
                    ? "3 drunk and stats (varies by class)"
                    : "shot of rotgut (varies by class)",
                "shot of rotgut");
        result[2] =
            new Option(
                KoLCharacter.isMuscleClass()
                    ? "3 drunk and stats (varies by class)"
                    : "shot of rotgut (varies by class)",
                "shot of rotgut");
        result[3] = new Option("always 3 drunk & stats");
        result[4] = new Option("always shot of rotgut");
        result[5] = new Option("combat (or rotgut if Myst class)");
        return result;

      case 185:
        // Yes, You're a Rock Starrr
        result = new Option[3];

        int drunk = KoLCharacter.getInebriety();

        // 0 drunk: base booze, mixed booze, fight
        // More than 0 drunk: base booze, mixed booze, stats

        result[0] = new Option("base booze");
        result[1] = new Option("mixed booze");
        result[2] = new Option(drunk == 0 ? "combat" : "stats");
        return result;

      case 187:
        // Arrr You Man Enough?

        result = new Option[2];
        float odds = BeerPongRequest.pirateInsultOdds() * 100.0f;

        result[0] = new Option(KoLConstants.FLOAT_FORMAT.format(odds) + "% chance of winning");
        result[1] = new Option(odds == 100.0f ? "Oh come on. Do it!" : "Try later");
        return result;

      case 188:
        // The Infiltrationist
        result = new Option[3];

        // Attempt a frontal assault
        boolean ok1 = EquipmentManager.isWearingOutfit(OutfitPool.FRAT_OUTFIT);
        result[0] = new Option("Frat Boy Ensemble (" + (ok1 ? "" : "NOT ") + "equipped)");

        // Go in through the side door
        boolean ok2a = KoLCharacter.hasEquipped(ItemPool.get(ItemPool.MULLET_WIG, 1));
        boolean ok2b = InventoryManager.getCount(ItemPool.BRIEFCASE) >= 1;
        result[1] =
            new Option(
                "mullet wig ("
                    + (ok2a ? "" : "NOT ")
                    + "equipped) + briefcase ("
                    + (ok2b ? "OK)" : "0 in inventory)"));

        // Catburgle
        boolean ok3a = KoLCharacter.hasEquipped(ItemPool.get(ItemPool.FRILLY_SKIRT, 1));
        int wings = InventoryManager.getCount(ItemPool.HOT_WING);
        result[2] =
            new Option(
                "frilly skirt ("
                    + (ok3a ? "" : "NOT ")
                    + "equipped) + 3 hot wings ("
                    + wings
                    + " in inventory)");

        return result;

      case 191:
        // Chatterboxing
        result = new Option[4];

        int trinks = InventoryManager.getCount(ItemPool.VALUABLE_TRINKET);
        result[0] = new Option("moxie substats");
        result[1] =
            new Option(
                trinks == 0
                    ? "lose hp (no valuable trinkets)"
                    : "use valuable trinket to banish (" + trinks + " in inventory)");
        result[2] = new Option("muscle substats");
        result[3] = new Option("mysticality substats");

        return result;

      case 272:
        // Marketplace Entrance
        result = new Option[2];

        int nickels = InventoryManager.getCount(ItemPool.HOBO_NICKEL);
        boolean binder = KoLCharacter.hasEquipped(ItemPool.get(ItemPool.HOBO_CODE_BINDER, 1));

        result[0] =
            new Option(
                nickels + " nickels, " + (binder ? "" : "NO ") + " hobo code binder equipped");
        result[1] = SKIP_ADVENTURE;

        return result;

      case 298:
        // In the Shade
        result = new Option[2];

        int seeds = InventoryManager.getCount(ItemPool.SEED_PACKET);
        int slime = InventoryManager.getCount(ItemPool.GREEN_SLIME);

        result[0] = new Option(seeds + " seed packets, " + slime + " globs of green slime");
        result[1] = SKIP_ADVENTURE;

        return result;

      case 304:
        // A Vent Horizon
        result = new Option[2];

        int summons = 3 - Preferences.getInteger("tempuraSummons");

        result[0] = new Option(summons + " summons left today");
        result[1] = SKIP_ADVENTURE;

        return result;

      case 305:
        // There is Sauce at the Bottom of the Ocean
        result = new Option[2];

        int globes = InventoryManager.getCount(ItemPool.MERKIN_PRESSUREGLOBE);

        result[0] = new Option(globes + " Mer-kin pressureglobes");
        result[1] = SKIP_ADVENTURE;

        return result;

      case 309:
        // Barback
        result = new Option[2];

        int seaodes = 3 - Preferences.getInteger("seaodesFound");

        result[0] = new Option(seaodes + " more seodes available today");
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
        result = new Option[6];
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
        result[0] = new Option("Seal Clubber/Pastamancer item, or yellow matter custard");
        result[1] = new Option("Sauceror/Accordion Thief item, or delicious comfit?");
        result[2] = new Option("Disco Bandit/Turtle Tamer item, or fight croqueteer");
        result[3] =
            new Option("you have " + count + "/5 of the items needed for an ittah bittah hookah");
        result[4] = new Option("get a chess cookie");
        result[5] = SKIP_ADVENTURE;
        return result;

      case 502:
        // Arboreal Respite
        result = new Option[3];

        // meet the vampire hunter, trade bar skins or gain a spooky sapling
        int stakes = InventoryManager.getCount(ItemPool.WOODEN_STAKES);
        int hearts = InventoryManager.getCount(ItemPool.VAMPIRE_HEART);
        String hunterAction =
            (stakes > 0 ? "and get wooden stakes" : "and trade " + hearts + " hearts");

        int barskins = InventoryManager.getCount(ItemPool.BAR_SKIN);
        int saplings = InventoryManager.getCount(ItemPool.SPOOKY_SAPLING);

        result[0] =
            new Option(
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
            new Option(
                "gain mosquito larva or spooky mushrooms, "
                    + coinAction
                    + ", get stats or fight a vampire");

        // gain a starter item, gain Spooky-Gro fertilizer, gain spooky temple map or gain fake bood
        int fertilizer = InventoryManager.getCount(ItemPool.SPOOKY_FERTILIZER);
        String mapAction = (haveCoin ? ", gain spooky temple map" : "");

        result[2] =
            new Option(
                "gain a starter item, gain Spooky-Gro fertilizer ("
                    + fertilizer
                    + ")"
                    + mapAction
                    + ", gain fake blood");

        return result;

      case 522:
        // Welcome to the Footlocker
        result = new Option[2];

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
                ? new Option("knob goblin elite polearm", "knob goblin elite polearm")
                : !havePants
                    ? new Option("knob goblin elite pants", "knob goblin elite pants")
                    : !haveHelm
                        ? new Option("knob goblin elite helm", "knob goblin elite helm")
                        : new Option("knob jelly donut", "knob jelly donut");
        result[1] = SKIP_ADVENTURE;
        return result;

      case 579:
        // Such Great Heights
        result = new Option[3];

        boolean haveNostril = (InventoryManager.getCount(ItemPool.NOSTRIL_OF_THE_SERPENT) > 0);
        boolean gainNostril =
            (!haveNostril
                && Preferences.getInteger("lastTempleButtonsUnlock")
                    != KoLCharacter.getAscensions());
        boolean templeAdvs =
            (Preferences.getInteger("lastTempleAdventures") == KoLCharacter.getAscensions());

        result[0] = new Option("mysticality substats");
        result[1] = (gainNostril ? new Option("gain the Nostril of the Serpent") : SKIP_ADVENTURE);
        result[2] = (templeAdvs ? SKIP_ADVENTURE : new Option("gain 3 adventures"));
        return result;

      case 580:
        // The Hidden Heart of the Hidden Temple
        result = new Option[3];

        haveNostril = (InventoryManager.getCount(ItemPool.NOSTRIL_OF_THE_SERPENT) > 0);
        boolean buttonsUnconfused =
            (Preferences.getInteger("lastTempleButtonsUnlock") == KoLCharacter.getAscensions());

        if (ChoiceManager.lastResponseText.contains("door_stone.gif")) {
          result[0] = new Option("muscle substats");
          result[1] =
              new Option(
                  buttonsUnconfused || haveNostril
                      ? "choose Hidden Heart adventure"
                      : "randomise Hidden Heart adventure");
          result[2] = new Option("moxie substats and 5 turns of Somewhat poisoned");
        } else if (ChoiceManager.lastResponseText.contains("door_sun.gif")) {
          result[0] = new Option("gain ancient calendar fragment");
          result[1] =
              new Option(
                  buttonsUnconfused || haveNostril
                      ? "choose Hidden Heart adventure"
                      : "randomise Hidden Heart adventure");
          result[2] = new Option("moxie substats and 5 turns of Somewhat poisoned");
        } else if (ChoiceManager.lastResponseText.contains("door_gargoyle.gif")) {
          result[0] = new Option("gain mana");
          result[1] =
              new Option(
                  buttonsUnconfused || haveNostril
                      ? "choose Hidden Heart adventure"
                      : "randomise Hidden Heart adventure");
          result[2] = new Option("moxie substats and 5 turns of Somewhat poisoned");
        } else if (ChoiceManager.lastResponseText.contains("door_pikachu.gif")) {
          result[0] = new Option("unlock Hidden City");
          result[1] =
              new Option(
                  buttonsUnconfused || haveNostril
                      ? "choose Hidden Heart adventure"
                      : "randomise Hidden Heart adventure");
          result[2] = new Option("moxie substats and 5 turns of Somewhat poisoned");
        }

        return result;

      case 581:
        // Such Great Depths
        result = new Option[3];

        int fungus = InventoryManager.getCount(ItemPool.GLOWING_FUNGUS);

        result[0] = new Option("gain a glowing fungus (" + fungus + ")");
        result[1] =
            Preferences.getBoolean("_templeHiddenPower")
                ? SKIP_ADVENTURE
                : new Option("5 advs of +15 mus/mys/mox");
        result[2] = new Option("fight clan of cave bars");
        return result;

      case 582:
        // Fitting In
        result = new Option[3];

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

        result[0] = new Option("mysticality substats, " + nostrilAction + " or " + advAction);

        // Hidden Heart of the Hidden Temple
        result[1] = new Option("Hidden Heart of the Hidden Temple");

        // gain glowing fungus, gain Hidden Power or fight a clan of cave bars
        String powerAction =
            (Preferences.getBoolean("_templeHiddenPower") ? "skip adventure" : "Hidden Power");

        result[2] =
            new Option("gain a glowing fungus, " + powerAction + " or fight a clan of cave bars");

        return result;

      case 606:
        // Lost in the Great Overlook Lodge
        result = new Option[6];

        result[0] =
            new Option(
                "need +4 stench resist, have "
                    + KoLCharacter.getElementalResistanceLevels(Element.STENCH));

        // annoyingly, the item drop check does not take into account fairy (or other sidekick)
        // bonus.
        // This is just a one-off implementation, but should be standardized somewhere in Modifiers
        // if kol adds more things like this.
        double bonus = 0;
        // Check for familiars
        if (!KoLCharacter.getFamiliar().equals(FamiliarData.NO_FAMILIAR)) {
          bonus = Modifiers.getNumericModifier(KoLCharacter.getFamiliar(), "Item Drop");
        }
        // Check for Clancy
        else if (KoLCharacter.getCurrentInstrument() != null
            && KoLCharacter.getCurrentInstrument().equals(CharPaneRequest.LUTE)) {
          int weight = 5 * KoLCharacter.getMinstrelLevel();
          bonus = Math.sqrt(55 * weight) + weight - 3;
        }
        // Check for Eggman
        else if (KoLCharacter.getCompanion() == Companion.EGGMAN) {
          bonus = KoLCharacter.hasSkill("Working Lunch") ? 75 : 50;
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
          bonus += Modifiers.getNumericModifier("Throne", throned.getRace(), "Item Drop");
        }
        // Check for Bjorn
        FamiliarData bjorned = KoLCharacter.getBjorned();
        if (!bjorned.equals(FamiliarData.NO_FAMILIAR)) {
          bonus += Modifiers.getNumericModifier("Throne", bjorned.getRace(), "Item Drop");
        }
        // Check for Florist
        if (FloristRequest.haveFlorist()) {
          List<Florist> plants = FloristRequest.getPlants("Twin Peak");
          if (plants != null) {
            for (Florist plant : plants) {
              bonus += Modifiers.getNumericModifier("Florist", plant.toString(), "Item Drop");
            }
          }
        }
        result[1] =
            new Option(
                "need +50% item drop, have "
                    + Math.round(
                        KoLCharacter.getItemDropPercentAdjustment()
                            + KoLCharacter.currentNumericModifier(Modifiers.FOODDROP)
                            - bonus)
                    + "%");
        result[2] = new Option("need jar of oil", "jar of oil");
        result[3] =
            new Option("need +40% init, have " + KoLCharacter.getInitiativeAdjustment() + "%");
        result[4] = null; // why is there a missing button 5?
        result[5] = new Option("flee");

        return result;

      case 611:
        // The Horror... (A-Boo Peak)
        result = new Option[2];
        result[0] = booPeakDamage();
        result[1] = new Option("Flee");
        return result;

      case 636:
      case 637:
      case 638:
      case 639:
        // Old Man psychosis choice adventures are randomized and may not include all elements.
        return oldManPsychosisSpoilers();

      case 641:
        // Stupid Pipes. (Mystic's psychoses)
        result = new Option[3];
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
          result[0] = new Option(buffer.toString());
        }
        result[1] = FLICKERING_PIXEL;
        result[2] = SKIP_ADVENTURE;
        return result;

      case 642:
        // You're Freaking Kidding Me (Mystic's psychoses)
        result = new Option[3];
        {
          String buffer =
              "50 buffed Muscle/Mysticality/Moxie required, have "
                  + KoLCharacter.getAdjustedMuscle()
                  + "/"
                  + KoLCharacter.getAdjustedMysticality()
                  + "/"
                  + KoLCharacter.getAdjustedMoxie();
          result[0] = new Option(buffer);
        }
        result[1] = FLICKERING_PIXEL;
        result[2] = SKIP_ADVENTURE;
        return result;

      case 644:
        // Snakes. (Mystic's psychoses)
        result = new Option[3];
        {
          String buffer = "50 buffed Moxie required, have " + KoLCharacter.getAdjustedMoxie();
          result[0] = new Option(buffer);
        }
        result[1] = FLICKERING_PIXEL;
        result[2] = SKIP_ADVENTURE;
        return result;

      case 645:
        // So... Many... Skulls... (Mystic's psychoses)
        result = new Option[3];
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
          result[0] = new Option(buffer.toString());
        }
        result[1] = FLICKERING_PIXEL;
        result[2] = SKIP_ADVENTURE;
        return result;

      case 647:
        // A Stupid Dummy. Also, a Straw Man. (Mystic's psychoses)
        result = new Option[3];
        {
          StringBuilder buffer = new StringBuilder();
          String current = String.valueOf(KoLCharacter.currentBonusDamage());
          buffer.append("100 weapon damage required");
          result[0] = new Option(buffer.toString());
        }
        result[1] = FLICKERING_PIXEL;
        result[2] = SKIP_ADVENTURE;
        return result;

      case 648:
        // Slings and Arrows (Mystic's psychoses)
        result = new Option[3];
        {
          String buffer = "101 HP required, have " + KoLCharacter.getCurrentHP();
          result[0] = new Option(buffer);
        }
        result[1] = FLICKERING_PIXEL;
        result[2] = SKIP_ADVENTURE;
        return result;

      case 650:
        // This Is Your Life. Your Horrible, Horrible Life. (Mystic's psychoses)
        result = new Option[3];
        {
          String buffer = "101 MP required, have " + KoLCharacter.getCurrentMP();
          result[0] = new Option(buffer);
        }
        result[1] = FLICKERING_PIXEL;
        result[2] = SKIP_ADVENTURE;
        return result;

      case 651:
        // The Wall of Wailing (Mystic's psychoses)
        result = new Option[3];
        {
          String buffer =
              "10 prismatic damage required, have " + KoLCharacter.currentPrismaticDamage();
          result[0] = new Option(buffer);
        }
        result[1] = FLICKERING_PIXEL;
        result[2] = SKIP_ADVENTURE;
        return result;

      case 669:
        // The Fast and the Furry-ous
        result = new Option[4];
        result[0] =
            new Option(
                KoLCharacter.hasEquipped(ItemPool.get(ItemPool.TITANIUM_UMBRELLA))
                    ? "open Ground Floor (titanium umbrella equipped)"
                    : KoLCharacter.hasEquipped(ItemPool.get(ItemPool.UNBREAKABLE_UMBRELLA))
                        ? "open Ground Floor (unbreakable umbrella equipped)"
                        : "Neckbeard Choice (titanium/unbreakable umbrella not equipped)");
        result[1] = new Option("200 Moxie substats");
        result[2] = new Option("");
        result[3] = new Option("skip adventure and guarantees this adventure will reoccur");
        return result;

      case 670:
        // You Don't Mess Around with Gym
        result = new Option[5];
        result[0] = new Option("massive dumbbell, then skip adventure");
        result[1] = new Option("200 Muscle substats");
        result[2] = new Option("pec oil, giant jar of protein powder, Squat-Thrust Magazine");
        result[3] =
            new Option(
                KoLCharacter.hasEquipped(ItemPool.get(ItemPool.EXTREME_AMULET, 1))
                    ? "open Ground Floor (amulet equipped)"
                    : "skip adventure (amulet not equipped)");
        result[4] = new Option("skip adventure and guarantees this adventure will reoccur");
        return result;

      case 678:
        // Yeah, You're for Me, Punk Rock Giant
        result = new Option[4];
        result[0] =
            new Option(
                KoLCharacter.hasEquipped(ItemPool.get(ItemPool.MOHAWK_WIG, 1))
                    ? "Finish quest (mohawk wig equipped)"
                    : "Fight Punk Rock Giant (mohawk wig not equipped)");
        result[1] = new Option("500 meat");
        result[2] = new Option("Steampunk Choice");
        result[3] = new Option("Raver Choice");
        return result;

      case 692:
        // I Wanna Be a Door
        result = new Option[9];
        result[0] = new Option("suffer trap effects");
        result[1] = new Option("unlock door with key, no turn spent");
        result[2] = new Option("pick lock with lockpicks, no turn spent");
        result[3] =
            new Option(
                KoLCharacter.getAdjustedMuscle() >= 30
                    ? "bypass trap with muscle"
                    : "suffer trap effects");
        result[4] =
            new Option(
                KoLCharacter.getAdjustedMysticality() >= 30
                    ? "bypass trap with mysticality"
                    : "suffer trap effects");
        result[5] =
            new Option(
                KoLCharacter.getAdjustedMoxie() >= 30
                    ? "bypass trap with moxie"
                    : "suffer trap effects");
        result[6] = new Option("open door with card, no turn spent");
        result[7] = new Option("leave, no turn spent");
        return result;

      case 696:
        // Stick a Fork In It
        result = new Option[2];
        result[0] =
            new Option(
                Preferences.getBoolean("maraisDarkUnlock")
                    ? "Dark and Spooky Swamp already unlocked"
                    : "unlock Dark and Spooky Swamp");
        result[1] =
            new Option(
                Preferences.getBoolean("maraisWildlifeUnlock")
                    ? "The Wildlife Sanctuarrrrrgh already unlocked"
                    : "unlock The Wildlife Sanctuarrrrrgh");
        return result;

      case 697:
        // Sophie's Choice
        result = new Option[2];
        result[0] =
            new Option(
                Preferences.getBoolean("maraisCorpseUnlock")
                    ? "The Corpse Bog already unlocked"
                    : "unlock The Corpse Bog");
        result[1] =
            new Option(
                Preferences.getBoolean("maraisWizardUnlock")
                    ? "The Ruined Wizard Tower already unlocked"
                    : "unlock The Ruined Wizard Tower");
        return result;

      case 698:
        // From Bad to Worst
        result = new Option[2];
        result[0] =
            new Option(
                Preferences.getBoolean("maraisBeaverUnlock")
                    ? "Swamp Beaver Territory already unlocked"
                    : "unlock Swamp Beaver Territory");
        result[1] =
            new Option(
                Preferences.getBoolean("maraisVillageUnlock")
                    ? "The Weird Swamp Village already unlocked"
                    : "unlock The Weird Swamp Village");
        return result;

      case 700:
        // Delirium in the Cafeteria
        result = new Option[9];
        result[0] =
            new Option(KoLConstants.activeEffects.contains(JOCK_EFFECT) ? "Gain stats" : "Lose HP");
        result[1] =
            new Option(KoLConstants.activeEffects.contains(NERD_EFFECT) ? "Gain stats" : "Lose HP");
        result[2] =
            new Option(
                KoLConstants.activeEffects.contains(GREASER_EFFECT) ? "Gain stats" : "Lose HP");
        return result;

      case 721:
        {
          // The Cabin in the Dreadsylvanian Woods

          result = new Option[6];

          StringBuilder buffer = new StringBuilder();
          buffer.append("dread tarragon");
          if (KoLCharacter.isMuscleClass()) {
            buffer.append(", old dry bone (");
            buffer.append(InventoryManager.getCount(ItemPool.OLD_DRY_BONE));
            buffer.append(") -> bone flour");
          }
          buffer.append(", -stench");
          result[0] = new Option(buffer.toString()); // The Kitchen

          buffer.setLength(0);
          buffer.append("Freddies");
          buffer.append(", Bored Stiff (+100 spooky damage)");
          buffer.append(", replica key (");
          buffer.append(InventoryManager.getCount(ItemPool.REPLICA_KEY));
          buffer.append(") -> Dreadsylvanian auditor's badge");
          buffer.append(", wax banana (");
          buffer.append(InventoryManager.getCount(ItemPool.WAX_BANANA));
          buffer.append(") -> complicated lock impression");
          result[1] = new Option(buffer.toString()); // The Cellar

          buffer.setLength(0);
          ChoiceAdventures.lockSpoiler(buffer);
          buffer.append("-spooky");
          if (KoLCharacter.isAccordionThief()) {
            buffer.append(" + intricate music box parts");
          }
          buffer.append(", fewer werewolves");
          buffer.append(", fewer vampires");
          buffer.append(", +Moxie");
          result[2] = new Option(buffer.toString()); // The Attic (locked)

          result[4] = ChoiceAdventures.shortcutSpoiler("ghostPencil1");
          result[5] = new Option("Leave this noncombat");
          return result;
        }

      case 722:
        // The Kitchen in the Woods
        result = new Option[6];
        result[0] = new Option("dread tarragon");
        result[1] =
            new Option(
                "old dry bone ("
                    + InventoryManager.getCount(ItemPool.OLD_DRY_BONE)
                    + ") -> bone flour");
        result[2] = new Option("-stench");
        result[5] = new Option("Return to The Cabin");
        return result;

      case 723:
        // What Lies Beneath (the Cabin)
        result = new Option[6];
        result[0] = new Option("Freddies");
        result[1] = new Option("Bored Stiff (+100 spooky damage)");
        result[2] =
            new Option(
                "replica key ("
                    + InventoryManager.getCount(ItemPool.REPLICA_KEY)
                    + ") -> Dreadsylvanian auditor's badge");
        result[3] =
            new Option(
                "wax banana ("
                    + InventoryManager.getCount(ItemPool.WAX_BANANA)
                    + ") -> complicated lock impression");
        result[5] = new Option("Return to The Cabin");
        return result;

      case 724:
        // Where it's Attic
        result = new Option[6];
        result[0] =
            new Option(
                "-spooky"
                    + (KoLCharacter.isAccordionThief() ? " + intricate music box parts" : ""));
        result[1] = new Option("fewer werewolves");
        result[2] = new Option("fewer vampires");
        result[3] = new Option("+Moxie");
        result[5] = new Option("Return to The Cabin");
        return result;

      case 725:
        {
          // Tallest Tree in the Forest

          result = new Option[6];

          StringBuilder buffer = new StringBuilder();
          if (KoLCharacter.isMuscleClass()) {
            buffer.append("drop blood kiwi");
            buffer.append(", -sleaze");
            buffer.append(", moon-amber");
          } else {
            buffer.append("unavailable (Muscle class only)");
          }
          result[0] = new Option(buffer.toString()); // Climb tree (muscle only)

          buffer.setLength(0);
          ChoiceAdventures.lockSpoiler(buffer);
          buffer.append("fewer ghosts");
          buffer.append(", Freddies");
          buffer.append(", +Muscle");
          result[1] = new Option(buffer.toString()); // Fire Tower (locked)

          buffer.setLength(0);
          buffer.append("blood kiwi (from above)");
          buffer.append(", Dreadsylvanian seed pod");
          if (KoLCharacter.hasEquipped(ItemPool.get(ItemPool.FOLDER_HOLDER, 1))) {
            buffer.append(", folder (owl)");
          }

          result[2] = new Option(buffer.toString()); // Base of tree

          result[4] = ChoiceAdventures.shortcutSpoiler("ghostPencil2");
          result[5] = new Option("Leave this noncombat");
          return result;
        }

      case 726:
        // Top of the Tree, Ma!
        result = new Option[6];
        result[0] = new Option("drop blood kiwi");
        result[1] = new Option("-sleaze");
        result[2] = new Option("moon-amber");
        result[5] = new Option("Return to The Tallest Tree");
        return result;

      case 727:
        // All Along the Watchtower
        result = new Option[6];
        result[0] = new Option("fewer ghosts");
        result[1] = new Option("Freddies");
        result[2] = new Option("+Muscle");
        result[5] = new Option("Return to The Tallest Tree");
        return result;

      case 728:
        // Treebasing
        result = new Option[6];
        result[0] = new Option("blood kiwi (from above)");
        result[1] = new Option("Dreadsylvanian seed pod");
        result[2] = new Option("folder (owl)");
        result[5] = new Option("Return to The Tallest Tree");
        return result;

      case 729:
        {
          // Below the Roots

          result = new Option[6];

          StringBuilder buffer = new StringBuilder();
          buffer.append("-hot");
          buffer.append(", Dragged Through the Coals (+100 hot damage)");
          buffer.append(", old ball and chain (");
          buffer.append(InventoryManager.getCount(ItemPool.OLD_BALL_AND_CHAIN));
          buffer.append(") -> cool iron ingot");
          result[0] = new Option(buffer.toString()); // Hot

          buffer.setLength(0);
          buffer.append("-cold");
          buffer.append(", +Mysticality");
          buffer.append(", Nature's Bounty (+300 max HP)");
          result[1] = new Option(buffer.toString()); // Cold

          buffer.setLength(0);
          buffer.append("fewer bugbears");
          buffer.append(", Freddies");
          result[2] = new Option(buffer.toString()); // Smelly

          result[4] = ChoiceAdventures.shortcutSpoiler("ghostPencil3");
          result[5] = new Option("Leave this noncombat");
          return result;
        }

      case 730:
        // Hot Coals
        result = new Option[6];
        result[0] = new Option("-hot");
        result[1] = new Option("Dragged Through the Coals (+100 hot damage)");
        result[2] =
            new Option(
                "old ball and chain ("
                    + InventoryManager.getCount(ItemPool.OLD_BALL_AND_CHAIN)
                    + ") -> cool iron ingot");
        result[5] = new Option("Return to The Burrows");
        return result;

      case 731:
        // The Heart of the Matter
        result = new Option[6];
        result[0] = new Option("-cold");
        result[1] = new Option("+Mysticality");
        result[2] = new Option("Nature's Bounty (+300 max HP)");
        result[5] = new Option("Return to The Burrows");
        return result;

      case 732:
        // Once Midden, Twice Shy
        result = new Option[6];
        result[0] = new Option("fewer bugbears");
        result[1] = new Option("Freddies");
        result[5] = new Option("Return to The Burrows");
        return result;

      case 733:
        {
          // Dreadsylvanian Village Square

          result = new Option[6];

          StringBuilder buffer = new StringBuilder();
          ChoiceAdventures.lockSpoiler(buffer);
          buffer.append("fewer ghosts");
          buffer.append(", ghost pencil");
          buffer.append(", +Mysticality");
          result[0] = new Option(buffer.toString()); // Schoolhouse (locked)

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
          result[1] = new Option(buffer.toString()); // Blacksmith

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
          result[2] = new Option(buffer.toString()); // Gallows

          result[4] = ChoiceAdventures.shortcutSpoiler("ghostPencil4");
          result[5] = new Option("Leave this noncombat");
          return result;
        }

      case 734:
        // Fright School
        result = new Option[6];
        result[0] = new Option("fewer ghosts");
        result[1] = new Option("ghost pencil");
        result[2] = new Option("+Mysticality");
        result[5] = new Option("Return to The Village Square");
        return result;

      case 735:
        // Smith, Black as Night
        result = new Option[6];
        result[0] = new Option("-cold");
        result[1] = new Option("Freddies");
        result[2] =
            new Option(
                "cool iron ingot ("
                    + InventoryManager.getCount(ItemPool.COOL_IRON_INGOT)
                    + ") + warm fur ("
                    + InventoryManager.getCount(ItemPool.WARM_FUR)
                    + ") -> cooling iron equipment");
        result[5] = new Option("Return to The Village Square");
        return result;

      case 736:
        // Gallows
        result = new Option[6];
        result[0] = new Option("-spooky");
        result[1] =
            new Option(
                "gain "
                    + (KoLCharacter.isMuscleClass()
                        ? "hangman's hood"
                        : KoLCharacter.isMysticalityClass()
                            ? "cursed ring finger ring"
                            : KoLCharacter.isMoxieClass()
                                ? "Dreadsylvanian clockwork key"
                                : "nothing")
                    + " with help of clannie");
        result[3] = new Option("help clannie gain an item");
        result[5] = new Option("Return to The Village Square");
        return result;

      case 737:
        {
          // The Even More Dreadful Part of Town

          result = new Option[6];

          StringBuilder buffer = new StringBuilder();
          buffer.append("-stench");
          buffer.append(", Sewer-Drenched (+100 stench damage)");
          result[0] = new Option(buffer.toString()); // Sewers

          buffer.setLength(0);
          buffer.append("fewer skeletons");
          buffer.append(", -sleaze");
          buffer.append(", +Muscle");
          result[1] = new Option(buffer.toString()); // Tenement

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
          result[2] = new Option(buffer.toString()); // Ticking Shack (moxie only)

          result[4] = ChoiceAdventures.shortcutSpoiler("ghostPencil5");
          result[5] = new Option("Leave this noncombat");
          return result;
        }

      case 738:
        // A Dreadful Smell
        result = new Option[6];
        result[0] = new Option("-stench");
        result[1] = new Option("Sewer-Drenched (+100 stench damage)");
        result[5] = new Option("Return to Skid Row");
        return result;

      case 739:
        // The Tinker's. Damn.
        result = new Option[6];
        result[0] = new Option("Freddies");
        result[1] =
            new Option(
                "lock impression ("
                    + InventoryManager.getCount(ItemPool.WAX_LOCK_IMPRESSION)
                    + ") + music box parts ("
                    + InventoryManager.getCount(ItemPool.INTRICATE_MUSIC_BOX_PARTS)
                    + ") -> replica key");
        result[2] =
            new Option(
                "moon-amber ("
                    + InventoryManager.getCount(ItemPool.MOON_AMBER)
                    + ") -> polished moon-amber");
        result[3] =
            new Option(
                "3 music box parts ("
                    + InventoryManager.getCount(ItemPool.INTRICATE_MUSIC_BOX_PARTS)
                    + ") + clockwork key ("
                    + InventoryManager.getCount(ItemPool.DREADSYLVANIAN_CLOCKWORK_KEY)
                    + ") -> mechanical songbird");
        result[4] = new Option("3 lengths of old fuse");
        result[5] = new Option("Return to Skid Row");
        return result;

      case 740:
        // Eight, Nine, Tenement
        result = new Option[6];
        result[0] = new Option("fewer skeletons");
        result[1] = new Option("-sleaze");
        result[2] = new Option("+Muscle");
        result[5] = new Option("Return to Skid Row");
        return result;

      case 741:
        {
          // The Old Duke's Estate

          result = new Option[6];

          StringBuilder buffer = new StringBuilder();
          buffer.append("fewer zombies");
          buffer.append(", Freddies");
          buffer.append(", Fifty Ways to Bereave Your Lover (+100 sleaze damage)");
          result[0] = new Option(buffer.toString()); // Cemetery

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
          result[1] = new Option(buffer.toString()); // Servants' Quarters

          buffer.setLength(0);
          ChoiceAdventures.lockSpoiler(buffer);
          buffer.append("fewer werewolves");
          buffer.append(", eau de mort");
          buffer.append(", 10 ghost thread (");
          buffer.append(InventoryManager.getCount(ItemPool.GHOST_THREAD));
          buffer.append(") -> ghost shawl");
          result[2] = new Option(buffer.toString()); // Master Suite (locked)

          result[4] = ChoiceAdventures.shortcutSpoiler("ghostPencil6");
          result[5] = new Option("Leave this noncombat");
          return result;
        }

      case 742:
        // The Plot Thickens
        result = new Option[6];
        result[0] = new Option("fewer zombies");
        result[1] = new Option("Freddies");
        result[2] = new Option("Fifty Ways to Bereave Your Lover (+100 sleaze damage)");
        result[5] = new Option("Return to The Old Duke's Estate");
        return result;

      case 743:
        // No Quarter
        result = new Option[6];
        result[0] = new Option("-hot");
        result[1] =
            new Option(
                "dread tarragon ("
                    + InventoryManager.getCount(ItemPool.DREAD_TARRAGON)
                    + ") + dreadful roast ("
                    + InventoryManager.getCount(ItemPool.DREADFUL_ROAST)
                    + ") + bone flour ("
                    + InventoryManager.getCount(ItemPool.BONE_FLOUR)
                    + ") + stinking agaricus ("
                    + InventoryManager.getCount(ItemPool.STINKING_AGARICUS)
                    + ") -> Dreadsylvanian shepherd's pie");
        result[2] = new Option("+Moxie");
        result[5] = new Option("Return to The Old Duke's Estate");
        return result;

      case 744:
        // The Master Suite -- Sweet!
        result = new Option[6];
        result[0] = new Option("fewer werewolves");
        result[1] = new Option("eau de mort");
        result[2] =
            new Option(
                "10 ghost thread ("
                    + InventoryManager.getCount(ItemPool.GHOST_THREAD)
                    + ") -> ghost shawl");
        result[5] = new Option("Return to The Old Duke's Estate");
        return result;

      case 745:
        {
          // This Hall is Really Great

          result = new Option[6];

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
          result[0] = new Option(buffer.toString()); // Ballroom (locked)

          buffer.setLength(0);
          buffer.append("-cold");
          buffer.append(", Staying Frosty (+100 cold damage)");
          result[1] = new Option(buffer.toString()); // Kitchen

          buffer.setLength(0);
          buffer.append("dreadful roast");
          buffer.append(", -stench");
          if (KoLCharacter.isMysticalityClass()) {
            buffer.append(", wax banana");
          }
          result[2] = new Option(buffer.toString()); // Dining Room

          result[4] = ChoiceAdventures.shortcutSpoiler("ghostPencil7");
          result[5] = new Option("Leave this noncombat");
          return result;
        }

      case 746:
        // The Belle of the Ballroom
        result = new Option[6];
        result[0] = new Option("fewer vampires");
        result[1] =
            new Option(
                (KoLCharacter.hasEquipped(ItemPool.get(ItemPool.MUDDY_SKIRT, 1))
                        ? "equipped muddy skirt -> weedy skirt and "
                        : InventoryManager.getCount(ItemPool.MUDDY_SKIRT) > 0
                            ? "(muddy skirt in inventory but not equipped) "
                            : "")
                    + "+Moxie");
        result[5] = new Option("Return to The Great Hall");
        return result;

      case 747:
        // Cold Storage
        result = new Option[6];
        result[0] = new Option("-cold");
        result[1] = new Option("Staying Frosty (+100 cold damage)");
        result[5] = new Option("Return to The Great Hall");
        return result;

      case 748:
        // Dining In (the Castle)
        result = new Option[6];
        result[0] = new Option("dreadful roast");
        result[1] = new Option("-stench");
        result[2] = new Option("wax banana");
        result[5] = new Option("Return to The Great Hall");
        return result;

      case 749:
        {
          // Tower Most Tall

          result = new Option[6];

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
          result[0] = new Option(buffer.toString()); // Laboratory (locked)

          buffer.setLength(0);
          if (KoLCharacter.isMysticalityClass()) {
            buffer.append("fewer skeletons");
            buffer.append(", +Mysticality");
            buffer.append(", learn recipe for moon-amber necklace");
          } else {
            buffer.append("unavailable (Mysticality class only)");
          }
          result[1] = new Option(buffer.toString()); // Books (mysticality only)

          buffer.setLength(0);
          buffer.append("-sleaze");
          buffer.append(", Freddies");
          buffer.append(", Magically Fingered (+150 max MP, 40-50 MP regen)");
          result[2] = new Option(buffer.toString()); // Bedroom

          result[4] = ChoiceAdventures.shortcutSpoiler("ghostPencil8");
          result[5] = new Option("Leave this noncombat");
          return result;
        }

      case 750:
        // Working in the Lab, Late One Night
        result = new Option[6];
        result[0] = new Option("fewer bugbears");
        result[1] = new Option("fewer zombies");
        result[2] = new Option("visit The Machine");
        result[3] =
            new Option(
                "blood kiwi ("
                    + InventoryManager.getCount(ItemPool.BLOOD_KIWI)
                    + ") + eau de mort ("
                    + InventoryManager.getCount(ItemPool.EAU_DE_MORT)
                    + ") -> bloody kiwitini");
        result[5] = new Option("Return to The Tower");
        return result;

      case 751:
        // Among the Quaint and Curious Tomes.
        result = new Option[6];
        result[0] = new Option("fewer skeletons");
        result[1] = new Option("+Mysticality");
        result[2] = new Option("learn recipe for moon-amber necklace");
        result[5] = new Option("Return to The Tower");
        return result;

      case 752:
        // In The Boudoir
        result = new Option[6];
        result[0] = new Option("-sleaze");
        result[1] = new Option("Freddies");
        result[2] = new Option("Magically Fingered (+150 max MP, 40-50 MP regen)");
        result[5] = new Option("Return to The Tower");
        return result;

      case 753:
        {
          // The Dreadsylvanian Dungeon

          result = new Option[6];

          StringBuilder buffer = new StringBuilder();
          buffer.append("-spooky");
          buffer.append(", +Muscle");
          buffer.append(", +MP");
          result[0] = new Option(buffer.toString()); // Prison

          buffer.setLength(0);
          buffer.append("-hot");
          buffer.append(", Freddies");
          buffer.append(", +Muscle/Mysticality/Moxie");
          result[1] = new Option(buffer.toString()); // Boiler Room

          buffer.setLength(0);
          buffer.append("stinking agaricus");
          buffer.append(", Spore-wreathed (reduce enemy defense by 20%)");
          result[2] = new Option(buffer.toString()); // Guard room

          result[4] = ChoiceAdventures.shortcutSpoiler("ghostPencil9");
          result[5] = new Option("Leave this noncombat");
          return result;
        }

      case 754:
        // Live from Dungeon Prison
        result = new Option[6];
        result[0] = new Option("-spooky");
        result[1] = new Option("+Muscle");
        result[2] = new Option("+MP");
        result[5] = new Option("Return to The Dungeons");
        return result;

      case 755:
        // The Hot Bowels
        result = new Option[6];
        result[0] = new Option("-hot");
        result[1] = new Option("Freddies");
        result[2] = new Option("+Muscle/Mysticality/Moxie");
        result[5] = new Option("Return to The Dungeons");
        return result;

      case 756:
        // Among the Fungus
        result = new Option[6];
        result[0] = new Option("stinking agaricus");
        result[1] = new Option("Spore-wreathed (reduce enemy defense by 20%)");
        result[5] = new Option("Return to The Dungeons");
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

          result = new Option[2];
          result[0] = new Option(buffer.toString());
          result[1] = new Option("Run away");
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

          result = new Option[2];
          result[0] = new Option(buffer.toString());
          result[1] = new Option("Run away");
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

          result = new Option[2];
          result[0] = new Option(buffer.toString());
          result[1] = new Option("Run away");
          return result;
        }

      case 772:
        {
          // Saved by the Bell

          // If you reach this encounter and Mafia things you've not spend 40 adventures in KOL High
          // school, correct this
          Preferences.setInteger("_kolhsAdventures", 40);

          result = new Option[10];
          String buffer =
              "Get "
                  + (Preferences.getInteger("kolhsTotalSchoolSpirited") + 1) * 10
                  + " turns of School Spirited (+100% Meat drop, +50% Item drop)";
          result[0] =
              new Option(
                  Preferences.getBoolean("_kolhsSchoolSpirited")
                      ? "Already got School Spirited today"
                      : buffer);
          result[1] =
              new Option(
                  Preferences.getBoolean("_kolhsPoeticallyLicenced")
                      ? "Already got Poetically Licenced today"
                      : "50 turns of Poetically Licenced (+20% Myst, -20% Muscle, +2 Myst stats/fight, +10% Spell damage)");
          result[2] =
              new Option(
                  InventoryManager.getCount(ItemPool.YEARBOOK_CAMERA) > 0
                          || KoLCharacter.hasEquipped(ItemPool.get(ItemPool.YEARBOOK_CAMERA, 1))
                      ? "Turn in yesterday's photo (if you have it)"
                      : "Get Yearbook Camera");
          result[3] =
              new Option(
                  Preferences.getBoolean("_kolhsCutButNotDried")
                      ? "Already got Cut But Not Dried today"
                      : "50 turns of Cut But Not Dried (+20% Muscle, -20% Moxie, +2 Muscle stats/fight, +10% Weapon damage)");
          result[4] =
              new Option(
                  Preferences.getBoolean("_kolhsIsskayLikeAnAshtray")
                      ? "Already got Isskay Like An Ashtray today"
                      : "50 turns of Isskay Like An Ashtray (+20% Moxie, -20% Myst, +2 Moxie stats/fight, +10% Pickpocket chance)");
          result[5] = new Option("Make items");
          result[6] = new Option("Make items");
          result[7] = new Option("Make items");
          result[9] = new Option("Leave");
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

          result = new Option[6];
          result[0] =
              new Option(
                  (hiddenApartmentProgress >= 7
                      ? "penthouse empty"
                      : hasThriceCursed
                          ? "Fight ancient protector spirit"
                          : "Need Thrice-Cursed to fight ancient protector spirit"));
          result[1] =
              new Option(
                  (hasThriceCursed
                      ? "Increase Thrice-Cursed"
                      : hasTwiceCursed
                          ? "Get Thrice-Cursed"
                          : hasOnceCursed ? "Get Twice-Cursed" : "Get Once-Cursed"));
          result[2] =
              new Option(
                  (pygmyLawyersRelocated
                      ? "Waste adventure"
                      : "Relocate pygmy witch lawyers to Hidden Park"));
          result[5] = SKIP_ADVENTURE;
          return result;
        }

      case 781:
        // Earthbound and Down
        result = new Option[6];
        result[0] = new Option("Unlock Hidden Apartment Building");
        result[1] = new Option("Get stone triangle");
        result[2] = new Option("Get Blessing of Bulbazinalli");
        result[5] = SKIP_ADVENTURE;
        return result;

      case 783:
        // Water You Dune
        result = new Option[6];
        result[0] = new Option("Unlock Hidden Hospital");
        result[1] = new Option("Get stone triangle");
        result[2] = new Option("Get Blessing of Squirtlcthulli");
        result[5] = SKIP_ADVENTURE;
        return result;

      case 784:
        // You, M. D.
        result = new Option[6];
        result[0] = new Option("Fight ancient protector spirit");
        result[5] = SKIP_ADVENTURE;
        return result;

      case 785:
        // Air Apparent
        result = new Option[6];
        result[0] = new Option("Unlock Hidden Office Building");
        result[1] = new Option("Get stone triangle");
        result[2] = new Option("Get Blessing of Pikachutlotal");
        result[5] = SKIP_ADVENTURE;
        return result;

      case 786:
        {
          // Working Holiday

          int hiddenOfficeProgress = Preferences.getInteger("hiddenOfficeProgress");
          boolean hasBossUnlock = hiddenOfficeProgress >= 6;
          boolean hasMcCluskyFile = InventoryManager.getCount(MCCLUSKY_FILE) > 0;
          boolean hasBinderClip = InventoryManager.getCount(BINDER_CLIP) > 0;

          result = new Option[6];
          result[0] =
              new Option(
                  (hiddenOfficeProgress >= 7
                      ? "office empty"
                      : hasMcCluskyFile || hasBossUnlock
                          ? "Fight ancient protector spirit"
                          : "Need McClusky File (complete) to fight ancient protector spirit"));
          result[1] =
              new Option(
                  (hasBinderClip || hasMcCluskyFile || hasBossUnlock)
                      ? "Get random item"
                      : "Get boring binder clip");
          result[2] = new Option("Fight pygmy witch accountant");
          result[5] = SKIP_ADVENTURE;
          return result;
        }

      case 787:
        // Fire when Ready
        result = new Option[6];
        result[0] = new Option("Unlock Hidden Bowling Alley");
        result[1] = new Option("Get stone triangle");
        result[2] = new Option("Get Blessing of Charcoatl");
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

          result = new Option[6];
          result[0] =
              new Option(
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
          result = new Option[6];
          result[0] = new Option("Get random items");
          result[1] =
              new Option(
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

          result = new Option[6];
          String buffer =
              "Need 4 stone triangles to fight Protector Spectre (" + stoneTriangles + ")";
          result[0] = new Option(stoneTriangles == 4 ? "fight Protector Spectre" : buffer);
          result[5] = SKIP_ADVENTURE;
          return result;
        }

      case 801:

        // A Reanimated Conversation
        result = new Option[7];
        result[0] = new Option("skulls increase meat drops");
        result[1] = new Option("arms deal extra damage");
        result[2] = new Option("legs increase item drops");
        result[3] = new Option("wings sometimes delevel at start of combat");
        result[4] = new Option("weird parts sometimes block enemy attacks");
        result[5] = new Option("get rid of all collected parts");
        result[6] = new Option("no changes");
        return result;

      case 918:

        // Yachtzee
        result = new Option[3];
        // Is it 7 or more days since the last time you got the Ultimate Mind Destroyer?
        Calendar date = HolidayDatabase.getCalendar();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String lastUMDDateString = Preferences.getString("umdLastObtained");
        if (lastUMDDateString != null && lastUMDDateString != "") {
          try {
            Date lastUMDDate = sdf.parse(lastUMDDateString);
            Calendar compareDate = HolidayDatabase.getCalendar();
            compareDate.setTime(lastUMDDate);
            compareDate.add(Calendar.DAY_OF_MONTH, 7);
            if (date.compareTo(compareDate) >= 0) {
              result[0] = new Option("get Ultimate Mind Destroyer");
            } else {
              result[0] = new Option("get cocktail ingredients");
            }
          } catch (ParseException ex) {
            result[0] = new Option("get cocktail ingredients (sometimes Ultimate Mind Destroyer)");
            KoLmafia.updateDisplay("Unable to parse " + lastUMDDateString);
          }
        } else {
          // Change to "get Ultimate Mind Destroyer" after 12th August 2014
          result[0] = new Option("get cocktail ingredients (sometimes Ultimate Mind Destroyer)");
        }
        result[1] = new Option("get 5k meat and random item");
        result[2] = new Option("get Beach Bucks");
        return result;

      case 988:

        // The Containment Unit
        result = new Option[2];
        String containment = Preferences.getString("EVEDirections");
        if (containment.length() != 6) {
          return result;
        }
        int progress = StringUtilities.parseInt(containment.substring(5, 6));
        if (progress < 0 && progress > 5) {
          return result;
        }
        if (containment.charAt(progress) == 'L') {
          result[0] = new Option("right way");
          result[1] = null;
        } else if (containment.charAt(progress) == 'R') {
          result[0] = null;
          result[1] = new Option("right way");
        } else {
          result[0] = new Option("unknown");
          result[1] = new Option("unknown");
        }
        return result;

      case 1049:
        {
          // Tomb of the Unknown Your Class Here

          String responseText = ChoiceManager.lastResponseText;
          Map<Integer, String> choices = ChoiceUtilities.parseChoices(responseText);
          int options = choices.size();
          if (options == 1) {
            return new Option[0];
          }

          int decision = ChoiceManager.getDecision(choice, responseText);
          if (decision == 0) {
            return new Option[0];
          }

          result = new Option[options];
          for (int i = 0; i < options; ++i) {
            result[i] = new Option((i == decision - 1) ? "right answer" : "wrong answer");
          }

          return result;
        }

      case 1411:
        {
          // The Hall in the Hall
          result = new Option[5];
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
            result[0] = new Option(buf);
          }
          result[1] = new Option("Buy a drippy candy bar for 10,000 Meat or get Driplets");
          {
            String item =
                KoLCharacter.hasSkill("Drippy Eye-Sprout")
                    ? "a drippy seed"
                    : KoLCharacter.hasSkill("Drippy Eye-Stone")
                        ? "a drippy bezoar"
                        : KoLCharacter.hasSkill("Drippy Eye-Beetle") ? "a drippy grub" : "nothing";
            result[2] = new Option("Get " + item);
          }
          {
            int steins = InventoryManager.getCount(ItemPool.DRIPPY_STEIN);
            result[3] =
                new Option(
                    (steins > 0) ? "Trade a drippy stein for a drippy pilsner" : "Get nothing");
          }
          result[4] = new Option("Get some Driplets");
          return result;
        }
    }
    return null;
  }

  private static Option shortcutSpoiler(final String setting) {
    return new Option(Preferences.getBoolean(setting) ? "shortcut KNOWN" : "learn shortcut");
  }

  private static void lockSpoiler(StringBuilder buffer) {
    buffer.append("possibly locked,");
    if (InventoryManager.getCount(ItemPool.DREADSYLVANIAN_SKELETON_KEY) == 0) {
      buffer.append(" no");
    }
    buffer.append(" key in inventory: ");
  }

  public static final Option choiceSpoiler(
      final int choice, final int decision, final Option[] spoilers) {
    switch (choice) {
      case 105:
        // Having a Medicine Ball
        if (decision == 3) {
          KoLCharacter.ensureUpdatedGuyMadeOfBees();
          boolean defeated = Preferences.getBoolean("guyMadeOfBeesDefeated");
          if (defeated) {
            return new Option("guy made of bees: defeated");
          }
          return new Option(
              "guy made of bees: called " + Preferences.getString("guyMadeOfBeesCount") + " times");
        }
        break;
      case 182:
        if (decision == 4) {
          return new Option("model airship");
        }
        break;
      case 793:
        if (decision == 4) {
          return new Option("gift shop");
        }
        break;
    }

    if (spoilers == null) {
      return null;
    }

    // Iterate through the spoilers and find the one corresponding to the decision
    for (int i = 0; i < spoilers.length; ++i) {
      Option spoiler = spoilers[i];

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

  private static final Pattern TELEGRAM_PATTERN = Pattern.compile("value=\"RE: (.*?)\"");

  public static final void preChoice(final GenericRequest request) {
    switch (ChoiceManager.lastChoice) {
        // Wheel In the Sky Keep on Turning: Muscle Position
      case 9:
        Preferences.setString(
            "currentWheelPosition",
            ChoiceManager.lastDecision == 1
                ? "mysticality"
                : ChoiceManager.lastDecision == 2 ? "moxie" : "muscle");
        break;

        // Wheel In the Sky Keep on Turning: Mysticality Position
      case 10:
        Preferences.setString(
            "currentWheelPosition",
            ChoiceManager.lastDecision == 1
                ? "map quest"
                : ChoiceManager.lastDecision == 2 ? "muscle" : "mysticality");
        break;

        // Wheel In the Sky Keep on Turning: Map Quest Position
      case 11:
        Preferences.setString(
            "currentWheelPosition",
            ChoiceManager.lastDecision == 1
                ? "moxie"
                : ChoiceManager.lastDecision == 2 ? "mysticality" : "map quest");
        break;

        // Wheel In the Sky Keep on Turning: Moxie Position
      case 12:
        Preferences.setString(
            "currentWheelPosition",
            ChoiceManager.lastDecision == 1
                ? "muscle"
                : ChoiceManager.lastDecision == 2 ? "map quest" : "moxie");
        break;

        // Maidens: disambiguate the Knights
      case 89:
        AdventureRequest.setNameOverride(
            "Knight", ChoiceManager.lastDecision == 1 ? "Knight (Wolf)" : "Knight (Snake)");
        break;

        // Strung-Up Quartet
      case 106:
        Preferences.setInteger("lastQuartetAscension", KoLCharacter.getAscensions());
        Preferences.setInteger("lastQuartetRequest", ChoiceManager.lastDecision);

        if (KoLCharacter.recalculateAdjustments()) {
          KoLCharacter.updateStatus();
        }

        break;

      case 123: // At Least It's Not Full Of Trash
        if (ChoiceManager.lastDecision == 2) {
          // Raise your hands up towards the heavens
          // This takes take a turn and advances to the tiles
          ResultProcessor.processAdventuresUsed(1);
        }
        break;

        // Start the Island War Quest
      case 142:
      case 146:
        if (ChoiceManager.lastDecision == 3) {
          QuestDatabase.setQuestProgress(Quest.ISLAND_WAR, "step1");
          Preferences.setString("warProgress", "started");
          if (KoLCharacter.inPokefam()) {
            // The following is a guess. Since all
            // sidequests are open, it is at least
            // 458, and surely both sides are equal
            Preferences.setInteger("hippiesDefeated", 500);
            Preferences.setInteger("fratboysDefeated", 500);
          }
        }
        break;

        // The Gong Has Been Bung
      case 276:
        ResultProcessor.processItem(ItemPool.GONG, -1);
        Preferences.setInteger("moleTunnelLevel", 0);
        Preferences.setInteger("birdformCold", 0);
        Preferences.setInteger("birdformHot", 0);
        Preferences.setInteger("birdformRoc", 0);
        Preferences.setInteger("birdformSleaze", 0);
        Preferences.setInteger("birdformSpooky", 0);
        Preferences.setInteger("birdformStench", 0);
        break;

        // The Horror...
      case 611:
        // To find which step we're on, look at the responseText from the _previous_ request.  This
        // should still be in lastResponseText.
        abooPeakLevel =
            findBooPeakLevel(
                ChoiceUtilities.findChoiceDecisionText(1, ChoiceManager.lastResponseText));
        // Handle changing the progress level in postChoice1 where we know the result.
        break;

        // Behind the world there is a door...
      case 612:
        TurnCounter.stopCounting("Silent Invasion window begin");
        TurnCounter.stopCounting("Silent Invasion window end");
        TurnCounter.startCounting(35, "Silent Invasion window begin loc=*", "lparen.gif");
        TurnCounter.startCounting(40, "Silent Invasion window end loc=*", "rparen.gif");
        break;

      case 794:
        ResultProcessor.removeItem(ItemPool.FUNKY_JUNK_KEY);
        break;

      case 931:
        // Life Ain't Nothin But Witches and Mummies
        QuestDatabase.setQuestIfBetter(Quest.CITADEL, "step6");
        break;

      case 932:
        // No Whammies
        QuestDatabase.setQuestIfBetter(Quest.CITADEL, "step8");
        break;

      case 1005: // 'Allo
      case 1006: // One Small Step For Adventurer
      case 1007: // Twisty Little Passages, All Hedge
      case 1008: // Pooling Your Resources
      case 1009: // Good Ol' 44% Duck
      case 1010: // Another Day, Another Fork
      case 1011: // Of Mouseholes and Manholes
      case 1012: // The Last Temptation
      case 1013: // Mazel Tov!
        // Taking any of these takes a turn. We'll eventually
        // be informed of that in a charpane/api refresh, but
        // that's too late for logging.
        ResultProcessor.processAdventuresUsed(1);
        break;

      case 1023: // Like a Bat into Hell
        if (ChoiceManager.lastDecision == 2) {
          int edDefeats = Preferences.getInteger("_edDefeats");
          int kaCost = edDefeats > 2 ? (int) (Math.pow(2, Math.min(edDefeats - 3, 5))) : 0;
          AdventureResult cost = ItemPool.get(ItemPool.KA_COIN, -kaCost);
          ResultProcessor.processResult(cost);
          KoLCharacter.setLimitmode(null);
        }
        break;

      case 1024: // Like a Bat out of Hell
        switch (ChoiceManager.lastDecision) {
          case 2:
            Preferences.setInteger("_edDefeats", 0);
            Preferences.setBoolean("edUsedLash", false);
            MonsterStatusTracker.reset();
            KoLCharacter.setLimitmode(null);
            break;
          case 1:
            int edDefeats = Preferences.getInteger("_edDefeats");
            int kaCost = edDefeats > 2 ? (int) (Math.pow(2, Math.min(edDefeats - 3, 5))) : 0;
            AdventureResult cost = ItemPool.get(ItemPool.KA_COIN, -kaCost);
            ResultProcessor.processResult(cost);
            KoLCharacter.setLimitmode(null);
            break;
        }
        break;

      case 1028:
        // A Shop
        SpelunkyRequest.logShop(ChoiceManager.lastResponseText, ChoiceManager.lastDecision);
        break;

      case 1085: // Deck of Every Card
        if (ChoiceManager.lastDecision == 1) {
          Preferences.increment("_deckCardsDrawn", 1, 15, false);
        }
        break;

      case 1086: // Pick a Card
        if (ChoiceManager.lastDecision == 1) {
          // The extra 1 will be covered in choice 1085
          Preferences.increment("_deckCardsDrawn", 4, 15, false);
        }
        break;

      case 1171: // LT&T Office
        if (ChoiceManager.lastDecision < 4) {
          QuestDatabase.setQuestProgress(Quest.TELEGRAM, QuestDatabase.STARTED);
          Preferences.setInteger("lttQuestDifficulty", ChoiceManager.lastDecision);
          Preferences.setInteger("lttQuestStageCount", 0);
          Matcher matcher = TELEGRAM_PATTERN.matcher(ChoiceManager.lastResponseText);
          for (int i = 0; i < ChoiceManager.lastDecision; i++) {
            if (!matcher.find()) {
              break;
            }
          }
          Preferences.setString("lttQuestName", matcher.group(1));
        } else if (ChoiceManager.lastDecision == 5) {
          QuestDatabase.setQuestProgress(Quest.TELEGRAM, QuestDatabase.UNSTARTED);
          Preferences.setInteger("lttQuestDifficulty", 0);
          Preferences.setInteger("lttQuestStageCount", 0);
          Preferences.setString("lttQuestName", "");
        }
        break;

      case 1197:
        // Travel back to a Delicious Meal
        if (ChoiceManager.lastDecision == 1 && !request.getURLString().contains("foodid=0")) {
          EatItemRequest.timeSpinnerUsed = true;
        }
        break;

      case 1261:
        // Which Door?
        if (ChoiceManager.lastResponseText.contains("Boris")) {
          Preferences.setString("_villainLairKey", "boris");
        } else if (ChoiceManager.lastResponseText.contains("Jarlsberg")) {
          Preferences.setString("_villainLairKey", "jarlsberg");
        } else if (ChoiceManager.lastResponseText.contains("Sneaky Pete")) {
          Preferences.setString("_villainLairKey", "pete");
        }
        break;

      case 1345:
        // Blech House
        Preferences.setInteger("smutOrcNoncombatProgress", 0);
        break;
    }
  }

  public static void postChoice0(int choice, final String urlString, final GenericRequest request) {
    String text = request.responseText;
    switch (choice) {
      case 125: // No Visible Means of Support
        if (ChoiceManager.lastChoice == 0) {
          // If we are visiting for the first time,
          // finish the tiles
          DvorakManager.lastTile(text);
        }
        break;

      case 360: // Wumpus Cave
        WumpusManager.preWumpus(ChoiceManager.lastDecision);
        break;

      case 1019: // Bee Rewarded
        if (ChoiceManager.lastDecision == 1) {
          // This does not contain an "Encounter", so was
          // not logged.
          RequestLogger.registerLocation("The Black Forest");
        }
        break;

      case 1308:
        {
          // place.php?whichplace=monorail&action=monorail_downtown
          //
          // On a Downtown Train
          //
          // Everything you do at this location uses this choice
          // adventure #. As you select options, you stay in the
          // same choice, but the available options change.
          //
          // We must deduce what is happening by looking at the
          // response text

          if (text.contains("muffin is not yet ready")) {
            Preferences.setBoolean("_muffinOrderedToday", true);

            // We submitted a choice. If we are buying a muffin,
            // remove the muffin tin here, since the response is
            // indistinguishable from simply visiting the Breakfast
            // Counter with a muffin on order.
            //
            // Therefore, look at the previous response. If it is
            // selling muffins and we jut bought a muffin, remove
            // the muffin tin.

            if (ChoiceManager.lastResponseText != null
                && ChoiceManager.lastResponseText.contains("Order a blueberry muffin")) {
              ResultProcessor.processResult(ItemPool.get(ItemPool.EARTHENWARE_MUFFIN_TIN, -1));
            }
          }
          break;
        }

      case 1313: // Bastille Battalion
      case 1314: // Bastille Battalion (Master of None)
      case 1315: // Castle vs. Castle
      case 1316: // GAME OVER
      case 1317: // A Hello to Arms (Battalion)
      case 1318: // Defensive Posturing
      case 1319: // Cheese Seeking Behavior
        BastilleBattalionManager.preChoice(urlString, request);
        break;

      case 1451:
        // Fire Captain Hagnk
        WildfireCampRequest.parseCaptain(text);
        break;
      case 1452:
        // Sprinkler Joe
        if (text.contains("Thanks again for your help!")) {
          Preferences.setBoolean("wildfireSprinkled", true);
        }
        break;
      case 1453:
        // Fracker Dan
        if (text.contains("Thanks for the help!")) {
          Preferences.setBoolean("wildfireFracked", true);
        }
        break;
      case 1454:
        // Cropduster Dusty
        if (text.contains("Thanks for helping out.")) {
          Preferences.setBoolean("wildfireDusted", true);
        }
        break;
    }
  }

  private static final Pattern BROKEN_CHAMPAGNE_PATTERN =
      Pattern.compile("Looks like it has (\\d+) ounce");
  private static final Pattern DAYCARE_RECRUIT_PATTERN =
      Pattern.compile("attract (.*?) new children");
  private static final Pattern DAYCARE_EQUIPMENT_PATTERN =
      Pattern.compile("manage to find (.*?) used");
  private static final Pattern DAYCARE_ITEM_PATTERN =
      Pattern.compile("<td valign=center>You lose an item: </td>.*?<b>(.*?)</b> \\((.*?)\\)</td>");
  private static final Pattern DECEASED_TREE_PATTERN =
      Pattern.compile("Looks like it has (.*?) needle");
  private static final Pattern DESCID_PATTERN = Pattern.compile("descitem\\((.*?)\\)");
  // You toss the space wine off the edge of the floating battlefield and 21 frat boys jump off
  // after it.
  private static final Pattern FRAT_RATIONING_PATTERN =
      Pattern.compile(
          "You toss the (.*?) off the edge of the floating battlefield and (\\d+) frat boys? jumps? off after it.");
  private static final Pattern GARBAGE_SHIRT_PATTERN =
      Pattern.compile("Looks like you can read roughly (\\d+) scrap");
  private static final Pattern GERALD_PATTERN =
      Pattern.compile("Gerald wants (\\d+)<table>.*?descitem\\((\\d+)\\)");
  private static final Pattern GERALDINE_PATTERN =
      Pattern.compile("Geraldine wants (\\d+)<table>.*?descitem\\((\\d+)\\)");
  private static final Pattern HELLEVATOR_PATTERN =
      Pattern.compile(
          "the (lobby|first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth|eleventh) (button|floor)");
  // You toss the mana curds into the crowd.  10 hippies dive onto it, greedily consume it, and pass
  // out.
  private static final Pattern HIPPY_RATIONING_PATTERN =
      Pattern.compile(
          "You toss the (.*?) into the crowd.  (\\d+) hippies dive onto it, greedily consume it, and pass out.");
  // You select a Gold Tier client, Norma "Smelly" Jackson, a hobo from The Oasis.
  private static final Pattern GUZZLR_LOCATION_PATTERN =
      Pattern.compile(
          "You select a (Bronze|Gold|Platinum) Tier client, (.*?), +an? (.*?) from (.*?)\\.<p>");
  private static final Pattern ITEMID_PATTERN = Pattern.compile("itemid(\\d+)=(\\d+)");
  private static final Pattern ORACLE_QUEST_PATTERN =
      Pattern.compile("don't remember leaving any spoons in (.*?)&quot;");
  private static final Pattern QTY_PATTERN = Pattern.compile("qty(\\d+)=(\\d+)");
  private static final Pattern SAFE_PATTERN = Pattern.compile("find ([\\d,]+) Meat in the safe");
  private static final Pattern SKELETON_PATTERN =
      Pattern.compile("You defeated <b>(\\d+)</b> skeletons");
  private static final Pattern TATTOO_PATTERN =
      Pattern.compile("otherimages/sigils/hobotat(\\d+).gif");
  private static final Pattern TOSSID_PATTERN = Pattern.compile("tossid=(\\d+)");
  private static final Pattern TRASH_PATTERN =
      Pattern.compile("must have been (\\d+) pieces of trash");
  private static final Pattern UNPERM_PATTERN =
      Pattern.compile("Turning (.+)(?: \\(HP\\)) into (\\d+) karma.");
  private static final Pattern URL_SKILLID_PATTERN = Pattern.compile("skillid=(\\d+)");
  private static final Pattern URL_VOTE_PATTERN = Pattern.compile("local\\[\\]=(\\d)");
  private static final Pattern YEARBOOK_TARGET_PATTERN =
      Pattern.compile("<b>Results:</b>.*?<b>(.*?)</b>");

  private static final String[] FLOORS =
      new String[] {
        "lobby",
        "first",
        "second",
        "third",
        "fourth",
        "fifth",
        "sixth",
        "seventh",
        "eighth",
        "ninth",
        "tenth",
        "eleventh",
      };

  public static void postChoice1(final String urlString, final GenericRequest request) {
    String text = request.responseText;

    switch (ChoiceManager.lastChoice) {
      case 188:
        // The Infiltrationist

        // Once you're inside the frat house, it's a simple
        // matter of making your way down to the basement and
        // retrieving Caronch's dentures from the frat boys'
        // ridiculous trophy case.

        if (ChoiceManager.lastDecision == 3 && text.contains("ridiculous trophy case")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.HOT_WING, -3));
        }
        break;

      case 189:
        // O Cap'm, My Cap'm
        if (ChoiceManager.lastDecision == 3) {
          QuestDatabase.setQuestIfBetter(Quest.NEMESIS, "step26");
        }
        break;

      case 191:
        // Chatterboxing
        if (ChoiceManager.lastDecision == 2
            && text.contains("find a valuable trinket that looks promising")) {
          BanishManager.banishMonster("chatty pirate", BanishManager.Banisher.CHATTERBOXING);
        }
        break;

      case 237:
        // Big Merv's Protein Shakes
      case 238:
        // Suddenly Salad!
      case 239:
        // Sizzling Weasel On a Stick
        if (ChoiceManager.lastDecision == 1 && text.contains("You gain")) {
          // You spend 20 hobo nickels
          AdventureResult cost = ItemPool.get(ItemPool.HOBO_NICKEL, -20);
          ResultProcessor.processResult(cost);

          // You gain 5 fullness
          KoLCharacter.setFullness(KoLCharacter.getFullness() + 5);
        }
        break;

      case 242:
        // Arthur Finn's World-Record Homebrew Stout
      case 243:
        // Mad Jack's Corn Squeezery
      case 244:
        // Bathtub Jimmy's Gin Mill
        if (ChoiceManager.lastDecision == 1 && text.contains("You gain")) {
          // You spend 20 hobo nickels
          AdventureResult cost = ItemPool.get(ItemPool.HOBO_NICKEL, -20);
          ResultProcessor.processResult(cost);

          // You gain 5 drunkenness.  This will be set
          // when we refresh the charpane.
        }
        break;

      case 271:
        // Tattoo Shop
      case 274:
        // Tattoo Redux
        if (ChoiceManager.lastDecision == 1) {
          Matcher matcher = TATTOO_PATTERN.matcher(request.responseText);
          if (matcher.find()) {
            int tattoo = StringUtilities.parseInt(matcher.group(1));
            AdventureResult cost = ItemPool.get(ItemPool.HOBO_NICKEL, -20 * tattoo);
            ResultProcessor.processResult(cost);
          }
        }
        break;

      case 298:
        // In the Shade

        // You carefully plant the packet of seeds, sprinkle it
        // with gooey green algae, wait a few days, and then
        // you reap what you sow. Sowed. Sew?

        if (ChoiceManager.lastDecision == 1 && text.contains("you reap what you sow")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SEED_PACKET, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.GREEN_SLIME, -1));
        }
        break;

      case 346:
        // Soup For You
      case 347:
        // Yes, Soup For You...
      case 348:
        // Souped Up
      case 351:
        // Beginner's Luck
        QuestDatabase.setQuestIfBetter(Quest.PRIMORDIAL, QuestDatabase.STARTED);
        break;

      case 349:
        // The Primordial Directive
        // You swam upward, into a brighter and warmer part of the soup
        if (ChoiceManager.lastDecision == 1
            && text.contains("a brighter and warmer part of the soup")) {
          QuestDatabase.setQuestIfBetter(Quest.PRIMORDIAL, "step1");
        }
        break;

      case 350:
        // Soupercharged
        if (ChoiceManager.lastDecision == 1 && text.contains("You've fixed me all up")) {
          Preferences.setInteger("aminoAcidsUsed", 0);
          QuestDatabase.setQuestProgress(Quest.PRIMORDIAL, QuestDatabase.FINISHED);
        }
        break;

      case 354:
        // You Can Never Be Too Rich or Too in the Future
        ResultProcessor.processResult(ItemPool.get(ItemPool.INDIGO_PARTY_INVITATION, -1));
        break;

      case 355:
        // I'm on the Hunt, I'm After You
        ResultProcessor.processResult(ItemPool.get(ItemPool.VIOLET_HUNT_INVITATION, -1));
        break;

      case 357:
        // Painful, Circuitous Logic
        ResultProcessor.processResult(ItemPool.get(ItemPool.MECHA_MAYHEM_CLUB_CARD, -1));
        break;

      case 358:
        // Brings All the Boys to the Blue Yard
        ResultProcessor.processResult(ItemPool.get(ItemPool.BLUE_MILK_CLUB_CARD, -1));
        break;

      case 362:
        // A Bridge Too Far
        ResultProcessor.processResult(ItemPool.get(ItemPool.SPACEFLEET_COMMUNICATOR_BADGE, -1));
        break;

      case 363:
        // Does This Bug You? Does This Bug You?
        ResultProcessor.processResult(ItemPool.get(ItemPool.SMUGGLER_SHOT_FIRST_BUTTON, -1));
        break;

      case 373:
        // Choice 373 is Northern Gate

        // Krakrox plugged the small stone block he found in
        // the basement of the abandoned building into the hole
        // on the left side of the gate

        if (text.contains("Krakrox plugged the small stone block")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SMALL_STONE_BLOCK, -1));
        }

        // Krakrox plugged the little stone block he had found
        // in the belly of the giant snake into the hole on the
        // right side of the gate

        else if (text.contains("Krakrox plugged the little stone block")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.LITTLE_STONE_BLOCK, -1));
        }
        break;

      case 376:
        // Choice 376 is Ancient Temple

        // You fit the two halves of the stone circle together,
        // and slot them into the depression on the door. After
        // a moment's pause, a low rumbling becomes audible,
        // and then the stone slab lowers into the ground. The
        // temple is now open to you, if you are ready to face
        // whatever lies inside.

        if (text.contains("two halves of the stone circle")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.HALF_STONE_CIRCLE, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.STONE_HALF_CIRCLE, -1));
        }
        break;

      case 389:
        // Choice 389 is The Unbearable Supremeness of Being

        // "Of course I understand," Jill says, in fluent
        // English. "I learned your language in the past five
        // minutes. I know where the element is, but we'll have
        // to go offworld to get it. Meet me at the Desert
        // Beach Spaceport." And with that, she gives you a
        // kiss and scampers off. Homina-homina.

        if (text.contains("Homina-homina")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SUPREME_BEING_GLOSSARY, -1));
        }
        break;

      case 392:
        // Choice 392 is The Elements of Surprise . . .

        // And as the two of you walk toward the bed, you sense
        // your ancestral memories pulling you elsewhere, ever
        // elsewhere, because your ancestral memories are
        // total, absolute jerks.

        if (text.contains("total, absolute jerks")) {
          EquipmentManager.discardEquipment(ItemPool.RUBY_ROD);
          ResultProcessor.processResult(ItemPool.get(ItemPool.ESSENCE_OF_HEAT, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.ESSENCE_OF_KINK, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.ESSENCE_OF_COLD, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.ESSENCE_OF_STENCH, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.ESSENCE_OF_FRIGHT, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.ESSENCE_OF_CUTE, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.SECRET_FROM_THE_FUTURE, 1));
        }
        break;

      case 393:
        // The Collector
        if (ChoiceManager.lastDecision == 1) {
          for (int i = ItemPool.GREEN_PEAWEE_MARBLE; i <= ItemPool.BIG_BUMBOOZER_MARBLE; ++i) {
            ResultProcessor.processResult(ItemPool.get(i, -1));
          }
        }
        break;

      case 394:
        {
          // Hellevator Music
          // Parse response
          Matcher matcher = HELLEVATOR_PATTERN.matcher(text);
          if (!matcher.find()) {
            break;
          }
          String floor = matcher.group(1);
          for (int mcd = 0; mcd < FLOORS.length; ++mcd) {
            if (floor.equals(FLOORS[mcd])) {
              String message = "Setting monster level to " + mcd;
              RequestLogger.printLine(message);
              RequestLogger.updateSessionLog(message);
              break;
            }
          }
          break;
        }

      case 413:
      case 414:
      case 415:
      case 416:
      case 417:
      case 418:
        HaciendaManager.parseRoom(ChoiceManager.lastChoice, ChoiceManager.lastDecision, text);
        break;

      case 440:
        // Puttin' on the Wax
        if (ChoiceManager.lastDecision == 1) {
          HaciendaManager.parseRecording(urlString, text);
        }
        break;

      case 443:
        // Chess Puzzle
        if (ChoiceManager.lastDecision == 1) {
          // Option 1 is "Play"
          RabbitHoleManager.parseChessMove(urlString, text);
        }
        break;

      case 450:
        // The Duchess' Cottage
        if (ChoiceManager.lastDecision == 1
            && text.contains("Delectable and pulchritudinous!")) { // Option 1 is Feed the Duchess
          ResultProcessor.processItem(ItemPool.BEAUTIFUL_SOUP, -1);
          ResultProcessor.processItem(ItemPool.LOBSTER_QUA_GRILL, -1);
          ResultProcessor.processItem(ItemPool.MISSING_WINE, -1);
          ResultProcessor.processItem(ItemPool.WALRUS_ICE_CREAM, -1);
          ResultProcessor.processItem(ItemPool.HUMPTY_DUMPLINGS, -1);
        }
        break;

      case 457:
        // Oh, No! Five-Oh!
        int count = InventoryManager.getCount(ItemPool.ORQUETTES_PHONE_NUMBER);
        if (ChoiceManager.lastDecision == 1 && count > 0) {
          ResultProcessor.processItem(ItemPool.ORQUETTES_PHONE_NUMBER, -count);
          ResultProcessor.processItem(ItemPool.KEGGER_MAP, -1);
        }

      case 460:
      case 461:
      case 462:
      case 463:
      case 464:
      case 465:
      case 467:
      case 468:
      case 469:
      case 470:
      case 472:
      case 473:
      case 474:
      case 475:
      case 476:
      case 477:
      case 478:
      case 479:
      case 480:
      case 481:
      case 482:
      case 483:
      case 484:
        // Space Trip
        ArcadeRequest.postChoiceSpaceTrip(
            request, ChoiceManager.lastChoice, ChoiceManager.lastDecision);
        break;

      case 471:
        // DemonStar
        ArcadeRequest.postChoiceDemonStar(request, ChoiceManager.lastDecision);
        break;

      case 485:
        // Fighters Of Fighting
        ArcadeRequest.postChoiceFightersOfFighting(request, ChoiceManager.lastDecision);
        break;

      case 486:
        // Dungeon Fist!
        ArcadeRequest.postChoiceDungeonFist(request, ChoiceManager.lastDecision);
        break;

      case 488:
      case 489:
      case 490:
      case 491:
        // Meteoid
        ArcadeRequest.postChoiceMeteoid(
            request, ChoiceManager.lastChoice, ChoiceManager.lastDecision);
        break;

      case 529:
      case 531:
      case 532:
      case 533:
      case 534:
        Matcher skeletonMatcher = SKELETON_PATTERN.matcher(text);
        if (skeletonMatcher.find()) {
          String message = "You defeated " + skeletonMatcher.group(1) + " skeletons";
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
        }
        break;

      case 539:
        // Choice 539 is An E.M.U. for Y.O.U.
        EquipmentManager.discardEquipment(ItemPool.SPOOKY_LITTLE_GIRL);
        break;

      case 540:
        // Choice 540 is Big-Time Generator - game board
        //
        // Win:
        //
        // The generator starts to hum and the well above you
        // begins to spin, slowly at first, then faster and
        // faster. The humming becomes a deep, sternum-rattling
        // thrum, a sub-audio *WHOOMP WHOOMP WHOOMPWHOOMPWHOOMP.*
        // Brilliant blue light begins to fill the well, and
        // you feel like your bones are turning to either
        // powder, jelly, or jelly with powder in it.<p>Then
        // you fall through one of those glowy-circle
        // transporter things and end up back on Grimace, and
        // boy, are they glad to see you! You're not sure where
        // one gets ticker-tape after an alien invasion, but
        // they seem to have found some.
        //
        // Lose 3 times:
        //
        // Your E.M.U.'s getting pretty beaten up from all the
        // pinballing between obstacles, and you don't like
        // your chances of getting back to the surface if you
        // try again. You manage to blast out of the generator
        // well and land safely on the surface. After that,
        // though, the E.M.U. gives an all-over shudder, a sad
        // little servo whine, and falls apart.

        if (text.contains("WHOOMP") || text.contains("a sad little servo whine")) {
          EquipmentManager.discardEquipment(ItemPool.EMU_UNIT);
          QuestDatabase.setQuestIfBetter(Quest.GENERATOR, QuestDatabase.FINISHED);
        }
        break;

      case 542:
        // The Now's Your Pants!  I Mean... Your Chance!

        // Then you make your way back out of the Alley,
        // clutching your pants triumphantly and trying really
        // hard not to think about how oddly chilly it has
        // suddenly become.

        // When you steal your pants, they are unequipped, you
        // gain a "you acquire" message", and they appear in
        // inventory.
        //
        // Treat this is simply discarding the pants you are
        // wearing
        if (text.contains("oddly chilly")) {
          EquipmentManager.discardEquipment(EquipmentManager.getEquipment(EquipmentManager.PANTS));
          QuestDatabase.setQuestProgress(Quest.MOXIE, "step1");
        }
        break;

      case 546:
        // Interview With You
        VampOutManager.postChoiceVampOut(text);
        break;

      case 559:
        // Fudge Mountain Breakdown
        if (ChoiceManager.lastDecision == 2) {
          if (text.contains("but nothing comes out")) {
            Preferences.setInteger("_fudgeWaspFights", 3);
          } else if (text.contains("trouble has taken a holiday")) {
            // The Advent Calendar hasn't been punched out enough to find fudgewasps yet
          } else {
            Preferences.increment("_fudgeWaspFights", 1);
          }
        }
        break;

      case 571:
        // Choice 571 is Your Minstrel Vamps
        QuestDatabase.setQuestProgress(Quest.CLANCY, QuestDatabase.STARTED);
        break;

      case 572:
        // Choice 572 is Your Minstrel Clamps
        QuestDatabase.setQuestProgress(Quest.CLANCY, "step2");
        break;

      case 573:
        // Choice 573 is Your Minstrel Stamps
        QuestDatabase.setQuestProgress(Quest.CLANCY, "step4");
        break;

      case 576:
        // Choice 576 is Your Minstrel Camps
        QuestDatabase.setQuestProgress(Quest.CLANCY, "step6");
        break;

      case 577:
        // Choice 577 is Your Minstrel Scamp
        QuestDatabase.setQuestProgress(Quest.CLANCY, "step8");
        break;

      case 588:
        // Machines!
        if (text.contains("The batbugbears around you start acting weird")) {
          BugbearManager.clearShipZone("Sonar");
        }
        break;

      case 589:
        // Autopsy Auturvy
        // The tweezers you used dissolve in the caustic fluid. Rats.
        if (text.contains("dissolve in the caustic fluid")) {
          ResultProcessor.processItem(ItemPool.AUTOPSY_TWEEZERS, -1);
        }
        return;

      case 594:
        // A Lost Room
        if (text.contains("You acquire")) {
          ResultProcessor.processItem(ItemPool.LOST_KEY, -1);
        }
        break;

      case 595:
        // Fire! I... have made... fire!
        if (text.contains("rubbing the two stupid sticks together")
            || text.contains("pile the sticks up on top of the briefcase")) {
          ResultProcessor.processItem(ItemPool.CSA_FIRE_STARTING_KIT, -1);
        }
        return;

      case 599:
        // A Zombie Master's Bait
        if (request.getFormField("quantity") == null) {
          return;
        }

        AdventureResult brain;
        if (ChoiceManager.lastDecision == 1) {
          brain = ItemPool.get(ItemPool.CRAPPY_BRAIN, 1);
        } else if (ChoiceManager.lastDecision == 2) {
          brain = ItemPool.get(ItemPool.DECENT_BRAIN, 1);
        } else if (ChoiceManager.lastDecision == 3) {
          brain = ItemPool.get(ItemPool.GOOD_BRAIN, 1);
        } else if (ChoiceManager.lastDecision == 4) {
          brain = ItemPool.get(ItemPool.BOSS_BRAIN, 1);
        } else {
          return;
        }

        int quantity = StringUtilities.parseInt(request.getFormField("quantity"));
        int inventoryCount = brain.getCount(KoLConstants.inventory);
        brain = brain.getInstance(-1 * Math.min(quantity, inventoryCount));

        ResultProcessor.processResult(brain);

        return;

      case 603:
        // Skeletons and The Closet
        if (ChoiceManager.lastDecision != 6) {
          ResultProcessor.removeItem(ItemPool.SKELETON);
        }
        return;

      case 607:
        // Room 237
        // Twin Peak first choice
        if (text.contains("You take a moment to steel your nerves.")) {
          int prefval = Preferences.getInteger("twinPeakProgress");
          prefval |= 1;
          Preferences.setInteger("twinPeakProgress", prefval);
        }
        return;

      case 608:
        // Go Check It Out!
        // Twin Peak second choice
        if (text.contains("All work and no play")) {
          int prefval = Preferences.getInteger("twinPeakProgress");
          prefval |= 2;
          Preferences.setInteger("twinPeakProgress", prefval);
        }
        return;

      case 611:
        // The Horror...
        // We need to detect if the choiceadv step was completed OR we got beaten up.
        // If we Flee, nothing changes
        if (ChoiceManager.lastDecision == 1) {
          if (text.contains("That's all the horror you can take")) { // AKA beaten up
            Preferences.decrement("booPeakProgress", 2, 0);
          } else {
            Preferences.decrement("booPeakProgress", 2 * abooPeakLevel, 0);
          }
          if (Preferences.getInteger("booPeakProgress") < 0) {
            Preferences.setInteger("booPeakProgress", 0);
          }
        }
        return;

      case 614:
        // Near the fog there is an... anvil?
        if (!text.contains("You acquire")) {
          return;
        }
        int souls =
            ChoiceManager.lastDecision == 1
                ? 3
                : ChoiceManager.lastDecision == 2
                    ? 11
                    : ChoiceManager.lastDecision == 3
                        ? 23
                        : ChoiceManager.lastDecision == 4 ? 37 : 0;
        if (souls > 0) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.MIME_SOUL_FRAGMENT, 0 - souls));
        }
        return;

      case 616:
        // He Is the Arm, and He Sounds Like This
        // Twin Peak third choice
        if (text.contains("You attempt to mingle")) {
          int prefval = Preferences.getInteger("twinPeakProgress");
          prefval |= 4;
          Preferences.setInteger("twinPeakProgress", prefval);
          ResultProcessor.processResult(ItemPool.get(ItemPool.JAR_OF_OIL, -1));
        }
        return;

      case 617:
        // Now It's Dark
        // Twin Peak fourth choice
        if (text.contains("When the lights come back")) {
          // the other three must be completed at this point.
          Preferences.setInteger("twinPeakProgress", 15);
        }
        return;

      case 618:
        // Cabin Fever
        if (text.contains("mercifully, the hotel explodes")) {
          // the other three must be completed at this point.
          Preferences.setInteger("twinPeakProgress", 15);
        }
        return;

      case 669:
      case 670:
      case 671:
        // All New Area Unlocked messages unlock the Ground Floor but check for it specifically in
        // case future changes unlock areas with message.
        if (text.contains("New Area Unlocked") && text.contains("The Ground Floor")) {
          Preferences.setInteger("lastCastleGroundUnlock", KoLCharacter.getAscensions());
          QuestDatabase.setQuestProgress(Quest.GARBAGE, "step8");
        }
        break;

      case 675:
        // Melon Collie and the Infinite Lameness
        if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.removeItem(ItemPool.DRUM_N_BASS_RECORD);
        }
        break;

      case 679:
        // Keep On Turnin' the Wheel in the Sky
        QuestDatabase.setQuestProgress(Quest.GARBAGE, "step10");
        break;

      case 689:
        // The Final Reward
        if (text.contains("claim your rightful reward")) {
          // Daily Dungeon Complete
          Preferences.setBoolean("dailyDungeonDone", true);
          Preferences.setInteger("_lastDailyDungeonRoom", 15);
        }
        return;

      case 690:
      case 691:
        // The First Chest Isn't the Deepest and Second Chest
        if (ChoiceManager.lastDecision == 2) {
          Preferences.increment("_lastDailyDungeonRoom", 3);
        } else {
          Preferences.increment("_lastDailyDungeonRoom", 1);
        }
        return;

      case 692:
        // I Wanna Be a Door
        if (text.contains("key breaks off in the lock")) {
          // Unfortunately, the key breaks off in the lock.
          ResultProcessor.processItem(ItemPool.SKELETON_KEY, -1);
        }
        if (ChoiceManager.lastDecision != 8) {
          Preferences.increment("_lastDailyDungeonRoom", 1);
        }
        return;

      case 693:
        // It's Almost Certainly a Trap
        if (ChoiceManager.lastDecision != 3) {
          Preferences.increment("_lastDailyDungeonRoom", 1);
        }
        return;

      case 699:
        // Lumber-related Pun
        if (text.contains("hand him the branch")) {
          // Marty's eyes widen when you hand him the
          // branch from the Great Tree.
          ResultProcessor.processItem(ItemPool.GREAT_TREE_BRANCH, -1);
        } else if (text.contains("hand him the rust")) {
          // At first Marty looks disappointed when you
          // hand him the rust-spotted, rotten-handled
          // axe, but after a closer inspection he gives
          // an impressed whistle.
          ResultProcessor.processItem(ItemPool.PHIL_BUNION_AXE, -1);
        } else if (text.contains("hand him the bouquet")) {
          // Marty looks delighted when you hand him the
          // bouquet of swamp roses.
          ResultProcessor.processItem(ItemPool.SWAMP_ROSE_BOUQUET, -1);
        }
        return;

      case 700:
        // Delirium in the Cafeterium
        Preferences.increment("_kolhsAdventures", 1);
        return;

      case 703:
        // Mer-kin dreadscroll
        if (text.contains("I guess you're the Mer-kin High Priest now")) {
          Preferences.setString("merkinQuestPath", "scholar");
          ResultProcessor.processItem(ItemPool.DREADSCROLL, -1);
          return;
        }
        return;

      case 709:
        // You Beat Shub to a Stub, Bub
      case 713:
        // You Brought Her To Her Kn-kn-kn-kn-knees, Knees.
      case 717:
        // Over. Over Now.
        Preferences.setString("merkinQuestPath", "done");
        return;

      case 720:
        // The Florist Friar's Cottage
        FloristRequest.parseResponse(urlString, text);
        return;

      case 721:
        // The Cabin in the Dreadsylvanian Woods
        if (ChoiceManager.lastDecision == 3) {
          // Try the Attic

          // You use your skeleton key to unlock the padlock on the attic trap door.
          // Then you use your legs to climb the ladder into the attic.
          // Then you use your stupidity to lose the skeleton key.  Crap.

          if (text.contains("lose the skeleton key")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREADSYLVANIAN_SKELETON_KEY, -1));
          }
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }
        return;

      case 722:
        // The Kitchen in the Woods
        if (ChoiceManager.lastDecision == 2) {
          // Screw around with the flour mill
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.OLD_DRY_BONE, -1));
          }
        }
        return;

      case 723:
        // What Lies Beneath (the Cabin)
        if (ChoiceManager.lastDecision == 3) {
          // Check out the lockbox
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.REPLICA_KEY, -1));
          }
        } else if (ChoiceManager.lastDecision == 4) {
          // Stick a wax banana in the lock
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.WAX_BANANA, -1));
          }
        }
        return;

      case 725:
        // Tallest Tree in the Forest
        if (ChoiceManager.lastDecision == 2) {
          // Check out the fire tower

          // You climb the rope ladder and use your skeleton key to
          // unlock the padlock on the door leading into the little room
          // at the top of the watchtower. Then you accidentally drop
          // your skeleton key and lose it in a pile of leaves. Rats.

          if (text.contains("you accidentally drop your skeleton key")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREADSYLVANIAN_SKELETON_KEY, -1));
          }
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }

        return;

      case 729:
        // Below the Roots
        if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }
        return;

      case 730:
        // Hot Coals
        if (ChoiceManager.lastDecision == 3) {
          // Melt down an old ball and chain
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.OLD_BALL_AND_CHAIN, -1));
          }
        }
        return;

      case 733:
        // Dreadsylvanian Village Square
        if (ChoiceManager.lastDecision == 1) {
          // The schoolhouse

          // You try the door of the schoolhouse, but it's locked. You
          // try your skeleton key in the lock, but it works. I mean and
          // it works. But it breaks. That was the but.

          if (text.contains("But it breaks")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREADSYLVANIAN_SKELETON_KEY, -1));
          }
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }
        return;

      case 737:
        // The Even More Dreadful Part of Town
        if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }
        return;

      case 739:
        // The Tinker's. Damn.
        if (ChoiceManager.lastDecision == 2) {
          // Make a key using the wax lock impression
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.WAX_LOCK_IMPRESSION, -1));
            ResultProcessor.processResult(ItemPool.get(ItemPool.INTRICATE_MUSIC_BOX_PARTS, -1));
          }
        } else if (ChoiceManager.lastDecision == 3) {
          // Polish the moon-amber
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.MOON_AMBER, -1));
          }
        } else if (ChoiceManager.lastDecision == 4) {
          // Assemble a clockwork bird
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREADSYLVANIAN_CLOCKWORK_KEY, -1));
            ResultProcessor.processResult(ItemPool.get(ItemPool.INTRICATE_MUSIC_BOX_PARTS, -3));
          }
        }
        return;

      case 741:
        // The Old Duke's Estate
        if (ChoiceManager.lastDecision == 3) {
          // Make your way to the master suite

          // You find the door to the old Duke's master bedroom and
          // unlock it with your skeleton key.

          if (text.contains("unlock it with your skeleton key")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREADSYLVANIAN_SKELETON_KEY, -1));
          }
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }
        return;

      case 743:
        // No Quarter
        if (ChoiceManager.lastDecision == 2) {
          // Make a shepherd's pie
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREAD_TARRAGON, -1));
            ResultProcessor.processResult(ItemPool.get(ItemPool.BONE_FLOUR, -1));
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREADFUL_ROAST, -1));
            ResultProcessor.processResult(ItemPool.get(ItemPool.STINKING_AGARICUS, -1));
          }
        }
        return;

      case 744:
        // The Master Suite -- Sweet!
        if (ChoiceManager.lastDecision == 3) {
          // Mess with the loom
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_THREAD, -10));
          }
        }
        return;

      case 745:
        // This Hall is Really Great
        if (ChoiceManager.lastDecision == 1) {
          // Head to the ballroom

          // You unlock the door to the ballroom with your skeleton
          // key. You open the door, and are so impressed by the site of
          // the elegant ballroom that you drop the key down a nearby
          // laundry chute.

          if (text.contains("you drop the key")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREADSYLVANIAN_SKELETON_KEY, -1));
          }
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }
        return;

      case 746:
        // The Belle of the Ballroom
        if (ChoiceManager.lastDecision == 2) {
          // Trip the light fantastic

          // You twirl around on the dance floor to music only you can
          // hear, your muddy skirt whirling around you filthily. You get
          // so caught up in the twirling that you drop your seed pod. It
          // breaks open, spreading weed seeds all over your skirt, which
          // immediately take root and grow.

          if (text.contains("spreading weed seeds all over your skirt")) {
            EquipmentManager.discardEquipment(ItemPool.MUDDY_SKIRT);
            EquipmentManager.setEquipment(
                EquipmentManager.PANTS, ItemPool.get(ItemPool.WEEDY_SKIRT, 1));
          }
        }
        return;

      case 749:
        // Tower Most Tall
        if (ChoiceManager.lastDecision == 1) {
          // Go to the laboratory

          // You use your skeleton key to unlock the door to the
          // laboratory. Unfortunately, the lock is electrified, and it
          // incinerates the key shortly afterwards.

          if (text.contains("it incinerates the key")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREADSYLVANIAN_SKELETON_KEY, -1));
          }
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }
        return;

      case 750:
        // Working in the Lab, Late One Night
        if (ChoiceManager.lastDecision == 4) {
          // Use the still
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.BLOOD_KIWI, -1));
            ResultProcessor.processResult(ItemPool.get(ItemPool.EAU_DE_MORT, -1));
          }
        }
        return;

      case 753:
        // The Dreadsylvanian Dungeon
        if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }
        return;

      case 762:
        // Try New Extra-Strength Anvil
        // All of the options that craft something use the same ingredients
        if (text.contains("You acquire")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.COOL_IRON_INGOT, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.WARM_FUR, -1));
        }
        return;

      case 772:
        // Saved by the Bell
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("_kolhsSchoolSpirited", true);
          Preferences.increment("kolhsTotalSchoolSpirited", 1);
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setBoolean("_kolhsPoeticallyLicenced", true);
        } else if (ChoiceManager.lastDecision == 3) {
          // You walk into the Yearbook Club and collar the kid with all
          // the camera equipment from yesterday. "Let me check your
          // memory card," he says, plugging the camera into a computer.
          // "Yup! You got it! Nice work. Here's your reward -- a nice
          // new accessory for that camera! If you're interested, now we
          // need a picture of a <b>monster</b>. You up for it?"
          //
          // You walk back into the Yearbook Club, a little tentatively.
          // "All right! Let's see what you've got!" the camera kid
          // says, and plugs your camera into a computer. "Aw, man, you
          // didn't get it? Well, I'll give you another chance.  If you
          // can still get us a picture of a <b>monster</b> and bring it
          // in tomorrow, you're still in the Club."
          //
          // You poke your head into the Yearbook Club room, but the
          // camera kid's packing up all the equipment and putting it
          // away. "Sorry, gotta go," he says, "but remember, you've
          // gotta get a picture of a <b>monster</b> for tomorrow, all
          // right? We're counting on you."

          if (text.contains("You got it!")) {
            Preferences.setString("yearbookCameraTarget", "");
            Preferences.setBoolean("yearbookCameraPending", false);
            Preferences.increment("yearbookCameraUpgrades", 1, 20, false);
            if (KoLCharacter.getAscensions()
                != Preferences.getInteger("lastYearbookCameraAscension")) {
              Preferences.setInteger("lastYearbookCameraAscension", KoLCharacter.getAscensions());
              Preferences.increment("yearbookCameraAscensions", 1, 20, false);
            }
          }

          Matcher matcher = YEARBOOK_TARGET_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("yearbookCameraTarget", matcher.group(1));
          }
        } else if (ChoiceManager.lastDecision == 4) {
          Preferences.setBoolean("_kolhsCutButNotDried", true);
        } else if (ChoiceManager.lastDecision == 5) {
          Preferences.setBoolean("_kolhsIsskayLikeAnAshtray", true);
        }
        if (ChoiceManager.lastDecision != 8) {
          Preferences.increment("_kolhsSavedByTheBell", 1);
        }
        return;

      case 778:
        // If You Could Only See
        if (ChoiceManager.lastDecision != 6) {
          Preferences.setBoolean("_tonicDjinn", true);
          if (!text.contains("already had a wish today")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.TONIC_DJINN, -1));
          }
        }
        return;

      case 780:
        // Action Elevator
        if (ChoiceManager.lastDecision == 1 && text.contains("penthouse is empty now")) {
          if (Preferences.getInteger("hiddenApartmentProgress") < 7) {
            Preferences.setInteger("hiddenApartmentProgress", 7);
          }
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.setInteger("relocatePygmyLawyer", KoLCharacter.getAscensions());
        }
        return;

      case 781:
        // Earthbound and Down
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setInteger("hiddenApartmentProgress", 1);
          QuestDatabase.setQuestProgress(Quest.CURSES, QuestDatabase.STARTED);
        } else if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.MOSS_COVERED_STONE_SPHERE, -1));
          Preferences.setInteger("hiddenApartmentProgress", 8);
        } else if (ChoiceManager.lastDecision == 3) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SIX_BALL, -1));
        }
        return;

      case 783:
        // Water You Dune
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setInteger("hiddenHospitalProgress", 1);
          QuestDatabase.setQuestProgress(Quest.DOCTOR, QuestDatabase.STARTED);
        } else if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.DRIPPING_STONE_SPHERE, -1));
          Preferences.setInteger("hiddenHospitalProgress", 8);
        } else if (ChoiceManager.lastDecision == 3) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.TWO_BALL, -1));
        }
        return;

      case 785:
        // Air Apparent
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setInteger("hiddenOfficeProgress", 1);
          QuestDatabase.setQuestProgress(Quest.BUSINESS, QuestDatabase.STARTED);
        } else if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.CRACKLING_STONE_SPHERE, -1));
          Preferences.setInteger("hiddenOfficeProgress", 8);
        } else if (ChoiceManager.lastDecision == 3) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.ONE_BALL, -1));
        }
        return;

      case 786:
        // Working Holiday
        if (ChoiceManager.lastDecision == 1 && text.contains("boss's office is empty")) {
          if (Preferences.getInteger("hiddenOfficeProgress") < 7) {
            Preferences.setInteger("hiddenOfficeProgress", 7);
          }
        }
        // if you don't get the expected binder clip, don't have one, and don't have a mcclusky
        // file, you must have unlocked the boss at least
        else if (ChoiceManager.lastDecision == 2
            && !text.contains("boring binder clip")
            && InventoryManager.getCount(MCCLUSKY_FILE) == 0
            && InventoryManager.getCount(BINDER_CLIP) == 0
            && Preferences.getInteger("hiddenOfficeProgress") < 6) {
          Preferences.setInteger("hiddenOfficeProgress", 6);
        }
        return;

      case 787:
        // Fire when Ready
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setInteger("hiddenBowlingAlleyProgress", 1);
          QuestDatabase.setQuestProgress(Quest.SPARE, QuestDatabase.STARTED);
        } else if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SCORCHED_STONE_SPHERE, -1));
          Preferences.setInteger("hiddenBowlingAlleyProgress", 8);
        } else if (ChoiceManager.lastDecision == 3) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.FIVE_BALL, -1));
        }
        return;

      case 788:
        // Life is Like a Cherry of Bowls
        if (ChoiceManager.lastDecision == 1
            && text.contains("without a frustrated ghost to torment")) {
          if (Preferences.getInteger("hiddenBowlingAlleyProgress") < 7) {
            Preferences.setInteger("hiddenBowlingAlleyProgress", 7);
          }
        }
        if (ChoiceManager.lastDecision == 1) {
          ResultProcessor.removeItem(ItemPool.BOWLING_BALL);
          int bowlCount = Preferences.getInteger("hiddenBowlingAlleyProgress");
          if (bowlCount < 6) {
            Preferences.setInteger(
                "hiddenBowlingAlleyProgress", (bowlCount < 2 ? 2 : bowlCount + 1));
          }
        }
        return;

      case 789:
        // Where Does The Lone Ranger Take His Garbagester?
        if (ChoiceManager.lastDecision == 2) {
          Preferences.setInteger("relocatePygmyJanitor", KoLCharacter.getAscensions());
        }
        return;

      case 801:
        // A Reanimated Conversation
        if (ChoiceManager.lastDecision == 6) {
          Preferences.setInteger("reanimatorArms", 0);
          Preferences.setInteger("reanimatorLegs", 0);
          Preferences.setInteger("reanimatorSkulls", 0);
          Preferences.setInteger("reanimatorWeirdParts", 0);
          Preferences.setInteger("reanimatorWings", 0);
        }
        return;

      case 805:
        // A Sietch in Time
        int gnasirProgress = Preferences.getInteger("gnasirProgress");

        // Annoyingly, the option numbers change as you turn
        // things in. Therefore, we must look at response text

        if (text.contains("give the stone rose to Gnasir")) {
          ResultProcessor.removeItem(ItemPool.STONE_ROSE);
          gnasirProgress |= 1;
          Preferences.setInteger("gnasirProgress", gnasirProgress);
        } else if (text.contains("hold up the bucket of black paint")) {
          ResultProcessor.removeItem(ItemPool.BLACK_PAINT);
          gnasirProgress |= 2;
          Preferences.setInteger("gnasirProgress", gnasirProgress);
        } else if (text.contains("hand Gnasir the glass jar")) {
          ResultProcessor.removeItem(ItemPool.KILLING_JAR);
          gnasirProgress |= 4;
          Preferences.setInteger("gnasirProgress", gnasirProgress);
        }
        // You hand him the pages, and he shuffles them into their correct order and inspects them
        // carefully.
        else if (text.contains("hand him the pages")) {
          ResultProcessor.processItem(ItemPool.WORM_RIDING_MANUAL_PAGE, -15);
          gnasirProgress |= 8;
          Preferences.setInteger("gnasirProgress", gnasirProgress);
        }
        return;

      case 812:
        if (ChoiceManager.lastDecision == 1) {
          Matcher matcher = UNPERM_PATTERN.matcher(text);
          if (matcher.find()) {
            KoLCharacter.removeAvailableSkill(matcher.group(1));
            Preferences.increment("bankedKarma", Integer.parseInt(matcher.group(2)));
          }
        }
        return;
      case 821:
        // LP-ROM burner
        if (ChoiceManager.lastDecision == 1) {
          HaciendaManager.parseRecording(urlString, text);
        }
        break;

      case 835:
        // Barely Tales
        if (ChoiceManager.lastDecision != 0) {
          Preferences.setBoolean("_grimBuff", true);
        }
        break;

      case 836:
        // Adventures Who Live in Ice Houses...
        if (ChoiceManager.lastDecision == 1) {
          BanishManager.removeBanishByBanisher(BanishManager.Banisher.ICE_HOUSE);
        }
        break;

      case 851:
        // Shen Copperhead, Nightclub Owner
        QuestDatabase.setQuestProgress(Quest.SHEN, "step1");
        Preferences.setInteger("shenInitiationDay", KoLCharacter.getCurrentDays());
        if (Preferences.getString("shenQuestItem").isEmpty()) {
          // We didn't recognise quest text before accepting quest, so get it from quest log
          RequestThread.postRequest(new QuestLogRequest());
        }

        break;

      case 852:
        // Shen Copperhead, Jerk
        // Deliberate fallthrough
      case 853:
        { // Shen Copperhead, Huge Jerk
          Matcher matcher = SHEN_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("shenQuestItem", matcher.group(1));
          }
        }
        // Deliberate fallthrough
      case 854:
        // Shen Copperhead, World's Biggest Jerk
        QuestDatabase.advanceQuest(Quest.SHEN);
        if (ChoiceManager.lastChoice == 854) {
          Preferences.setString("shenQuestItem", "");
        }

        // You will have exactly one of these items to ger rid of
        ResultProcessor.removeItem(ItemPool.FIRST_PIZZA);
        ResultProcessor.removeItem(ItemPool.LACROSSE_STICK);
        ResultProcessor.removeItem(ItemPool.EYE_OF_THE_STARS);
        ResultProcessor.removeItem(ItemPool.STANKARA_STONE);
        ResultProcessor.removeItem(ItemPool.MURPHYS_FLAG);
        ResultProcessor.removeItem(ItemPool.SHIELD_OF_BROOK);
        break;

      case 872:
        // Drawn Onward - Handle quest in Ed
        if (text.contains("Rot in a jar of dog paws!")) {
          QuestDatabase.setQuestProgress(Quest.PALINDOME, QuestDatabase.FINISHED);
          ResultProcessor.removeItem(ItemPool.ED_FATS_STAFF);
          if (InventoryManager.getCount(ItemPool.ED_EYE) == 0
              && InventoryManager.getCount(ItemPool.ED_AMULET) == 0) {
            QuestDatabase.setQuestProgress(Quest.MACGUFFIN, QuestDatabase.FINISHED);
          }
        }
        break;

      case 890:
        // Lights Out in the Storage Room
        if (text.contains("BUT AIN'T NO ONE CAN GET A STAIN OUT LIKE OLD AGNES!")
            && !Preferences.getString("nextSpookyravenElizabethRoom").equals("none")) {
          Preferences.setString("nextSpookyravenElizabethRoom", "The Haunted Laundry Room");
        }
        break;

      case 891:
        // Lights Out in the Laundry Room
        if (text.contains("DO YOU SEE THE STAIN UPON MY TOWEL?")
            && !Preferences.getString("nextSpookyravenElizabethRoom").equals("none")) {
          Preferences.setString("nextSpookyravenElizabethRoom", "The Haunted Bathroom");
        }
        break;

      case 892:
        // Lights Out in the Bathroom
        if (text.contains("THE STAIN HAS BEEN LIFTED")
            && !Preferences.getString("nextSpookyravenElizabethRoom").equals("none")) {
          Preferences.setString("nextSpookyravenElizabethRoom", "The Haunted Kitchen");
        }
        break;

      case 893:
        // Lights Out in the Kitchen
        if (text.contains("If You Give a Demon a Brownie")
            && !Preferences.getString("nextSpookyravenElizabethRoom").equals("none")) {
          Preferences.setString("nextSpookyravenElizabethRoom", "The Haunted Library");
        }
        break;

      case 894:
        // Lights Out in the Library
        if (text.contains("If You Give a Demon a Brownie")
            && !Preferences.getString("nextSpookyravenElizabethRoom").equals("none")) {
          Preferences.setString("nextSpookyravenElizabethRoom", "The Haunted Ballroom");
        }
        break;

      case 895:
        // Lights Out in the Ballroom
        if (text.contains("The Flowerbed of Unearthly Delights")
            && !Preferences.getString("nextSpookyravenElizabethRoom").equals("none")) {
          Preferences.setString("nextSpookyravenElizabethRoom", "The Haunted Gallery");
        }
        break;

      case 896:
        // Lights Out in the Gallery

        // The correct option leads to a combat with Elizabeth.
        // If you win, we will set "nextSpookyravenElizabethRoom" to "none"
        break;

      case 897:
        // Lights Out in the Bedroom
        if (text.contains("restock his medical kit in the nursery")
            && !Preferences.getString("nextSpookyravenStephenRoom").equals("none")) {
          Preferences.setString("nextSpookyravenStephenRoom", "The Haunted Nursery");
        }
        break;

      case 898:
        // Lights Out in the Nursery
        if (text.contains("This afternoon we're burying Crumbles")
            && !Preferences.getString("nextSpookyravenStephenRoom").equals("none")) {
          Preferences.setString("nextSpookyravenStephenRoom", "The Haunted Conservatory");
        }
        break;

      case 899:
        // Lights Out in the Conservatory
        if (text.contains("Crumbles isn't buried very deep")
            && !Preferences.getString("nextSpookyravenStephenRoom").equals("none")) {
          Preferences.setString("nextSpookyravenStephenRoom", "The Haunted Billiards Room");
        }
        break;

      case 900:
        // Lights Out in the Billiards Room
        if (text.contains("The wolf head has a particularly nasty expression on its face")
            && !Preferences.getString("nextSpookyravenStephenRoom").equals("none")) {
          Preferences.setString("nextSpookyravenStephenRoom", "The Haunted Wine Cellar");
        }
        break;

      case 901:
        // Lights Out in the Wine Cellar
        if (text.contains("Crumbles II (Wolf)")
            && !Preferences.getString("nextSpookyravenStephenRoom").equals("none")) {
          Preferences.setString("nextSpookyravenStephenRoom", "The Haunted Boiler Room");
        }
        break;

      case 902:
        // Lights Out in the Boiler Room
        if (text.contains("CRUMBLES II")
            && !Preferences.getString("nextSpookyravenStephenRoom").equals("none")) {
          Preferences.setString("nextSpookyravenStephenRoom", "The Haunted Laboratory");
        }
        break;

      case 903:
        // Lights Out in the Laboratory

        // The correct option leads to a combat with Stephen.
        // If you win, we will set "nextSpookyravenStephenRoom" to "none"
        break;

      case 915:
        // Choice 915 is Et Tu, Buff Jimmy?
        if (text.contains("skinny mushroom girls")) {
          QuestDatabase.setQuestProgress(Quest.JIMMY_MUSHROOM, QuestDatabase.STARTED);
        } else if (text.contains(
            "But here's a few Beach Bucks as a token of my changes in gratitude")) {
          QuestDatabase.setQuestProgress(Quest.JIMMY_MUSHROOM, QuestDatabase.FINISHED);
          ResultProcessor.processItem(ItemPool.PENCIL_THIN_MUSHROOM, -10);
        } else if (text.contains("not really into moving out of this hammock")) {
          QuestDatabase.setQuestProgress(Quest.JIMMY_CHEESEBURGER, QuestDatabase.STARTED);
          Preferences.setInteger("buffJimmyIngredients", 0);
        } else if (text.contains("So I'll just give you some Beach Bucks instead")) {
          QuestDatabase.setQuestProgress(Quest.JIMMY_CHEESEBURGER, QuestDatabase.FINISHED);
          Preferences.setInteger("buffJimmyIngredients", 0);
          ResultProcessor.processItem(ItemPool.CHEESEBURGER_RECIPE, -1);
        } else if (text.contains("sons of sons of sailors are")) {
          QuestDatabase.setQuestProgress(Quest.JIMMY_SALT, QuestDatabase.STARTED);
        } else if (text.contains("So here's some Beach Bucks instead")) {
          QuestDatabase.setQuestProgress(Quest.JIMMY_SALT, QuestDatabase.FINISHED);
          ResultProcessor.processItem(ItemPool.SAILOR_SALT, -50);
        }
        break;
      case 916:
        // Choice 916 is Taco Dan's Taco Stand's Taco Dan
        if (text.contains("find those receipts")) {
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_AUDIT, QuestDatabase.STARTED);
        } else if (text.contains("Here's a little Taco Dan's Taco Stand gratitude for ya")) {
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_AUDIT, QuestDatabase.FINISHED);
          ResultProcessor.processItem(ItemPool.TACO_DAN_RECEIPT, -10);
        } else if (text.contains("fill it up with as many cocktail drippings")) {
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_COCKTAIL, QuestDatabase.STARTED);
          Preferences.setInteger("tacoDanCocktailSauce", 0);
        } else if (text.contains("sample of Taco Dan's Taco Stand's Tacoriffic Cocktail Sauce")) {
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_COCKTAIL, QuestDatabase.FINISHED);
          Preferences.setInteger("tacoDanCocktailSauce", 0);
          ResultProcessor.processItem(ItemPool.TACO_DAN_SAUCE_BOTTLE, -1);
        } else if (text.contains("get enough taco fish")) {
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_FISH, QuestDatabase.STARTED);
          Preferences.setInteger("tacoDanFishMeat", 0);
        } else if (text.contains("batch of those Taco Dan's Taco Stand's Taco Fish Tacos")) {
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_FISH, QuestDatabase.FINISHED);
          Preferences.setInteger("tacoDanFishMeat", 0);
        }
        break;
      case 917:
        // Choice 917 is Do You Even Brogurt
        if (text.contains("need about ten shots of it")) {
          QuestDatabase.setQuestProgress(Quest.BRODEN_BACTERIA, QuestDatabase.STARTED);
          Preferences.setInteger("brodenBacteria", 0);
        } else if (text.contains("YOLO cup to spit the bacteria into")) {
          QuestDatabase.setQuestProgress(Quest.BRODEN_BACTERIA, QuestDatabase.FINISHED);
          Preferences.setInteger("brodenBacteria", 0);
        } else if (text.contains("loan you my sprinkle shaker to fill up")) {
          QuestDatabase.setQuestProgress(Quest.BRODEN_SPRINKLES, QuestDatabase.STARTED);
          Preferences.setInteger("brodenSprinkles", 0);
        } else if (text.contains("can sell some <i>deluxe</i> brogurts")) {
          QuestDatabase.setQuestProgress(Quest.BRODEN_SPRINKLES, QuestDatabase.FINISHED);
          Preferences.setInteger("brodenSprinkles", 0);
          ResultProcessor.processItem(ItemPool.SPRINKLE_SHAKER, -1);
        } else if (text.contains("There were like fifteen of these guys")) {
          QuestDatabase.setQuestProgress(Quest.BRODEN_DEBT, QuestDatabase.STARTED);
        } else if (text.contains("And they all had broupons, huh")) {
          QuestDatabase.setQuestProgress(Quest.BRODEN_DEBT, QuestDatabase.FINISHED);
          ResultProcessor.processItem(ItemPool.BROUPON, -15);
        }
        break;
      case 918:
        // Yachtzee!
        if (ChoiceManager.lastDecision == 3 && text.contains("You open the captain's door")) {
          int beads = Math.min(InventoryManager.getCount(ItemPool.MOIST_BEADS), 100);
          ResultProcessor.processResult(ItemPool.get(ItemPool.MOIST_BEADS, -beads));
        }
        break;
      case 919:
        // Choice 919 is Break Time!
        if (ChoiceManager.lastDecision == 1) {
          if (text.contains("You've already thoroughly")) {
            Preferences.setInteger("_sloppyDinerBeachBucks", 4);
          } else {
            Preferences.increment("_sloppyDinerBeachBucks", 1);
          }
        }
        break;
      case 920:
        // Choice 920 is Eraser
        if (ChoiceManager.lastDecision == 1) {
          QuestDatabase.setQuestProgress(Quest.JIMMY_MUSHROOM, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.JIMMY_CHEESEBURGER, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.JIMMY_SALT, QuestDatabase.UNSTARTED);
        } else if (ChoiceManager.lastDecision == 2) {
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_AUDIT, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_COCKTAIL, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_FISH, QuestDatabase.UNSTARTED);
        } else if (ChoiceManager.lastDecision == 3) {
          QuestDatabase.setQuestProgress(Quest.BRODEN_BACTERIA, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.BRODEN_SPRINKLES, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.BRODEN_DEBT, QuestDatabase.UNSTARTED);
        }
        if (ChoiceManager.lastDecision != 4) {
          ResultProcessor.processItem(ItemPool.MIND_DESTROYER, -1);
        }
        break;

      case 930:
        // Another Errand I Mean Quest
        if (ChoiceManager.lastDecision == 1) {
          QuestDatabase.setQuestIfBetter(Quest.CITADEL, QuestDatabase.STARTED);
        }
        break;

      case 932:
        // No Whammies
        if (text.contains("steel your nerves for what lies ahead")) {
          QuestDatabase.setQuestProgress(Quest.CITADEL, "step9");
        }
        break;

      case 967:
        // The Thunder Rolls...
        if (ChoiceManager.lastDecision != 8) {
          ResultProcessor.removeItem(ItemPool.THUNDER_THIGH);
        }
        break;

      case 968:
        // The Rain Falls Down With Your Help...
        if (ChoiceManager.lastDecision != 8) {
          ResultProcessor.removeItem(ItemPool.AQUA_BRAIN);
        }
        break;

      case 969:
        // And The Lightning Strikes...
        if (ChoiceManager.lastDecision != 8) {
          ResultProcessor.removeItem(ItemPool.LIGHTNING_MILK);
        }
        break;

      case 970:
        // Rainy Fax Dreams on your Wedding Day
        if (ChoiceManager.lastDecision == 1) {
          EncounterManager.ignoreSpecialMonsters();
          KoLAdventure.lastVisitedLocation = null;
          KoLAdventure.lastLocationName = null;
          KoLAdventure.lastLocationURL = urlString;
          KoLAdventure.setLastAdventure("None");
          KoLAdventure.setNextAdventure("None");
          GenericRequest.itemMonster = "Rain Man";
        }
        break;

      case 984:
        {
          // A Radio on a Beach
          // Clear quests when accepting a new one as you can only have one
          if (text.contains("your best paramilitary-sounding radio lingo")) {
            QuestDatabase.setQuestProgress(Quest.JUNGLE_PUN, QuestDatabase.UNSTARTED);
            QuestDatabase.setQuestProgress(Quest.GORE, QuestDatabase.UNSTARTED);
            QuestDatabase.setQuestProgress(Quest.CLIPPER, QuestDatabase.UNSTARTED);
          }
          // Also clear repeatable quests if there is no quest active at the radio
          if (text.contains("Maybe try again tomorrow")) {
            QuestDatabase.setQuestProgress(Quest.JUNGLE_PUN, QuestDatabase.UNSTARTED);
            QuestDatabase.setQuestProgress(Quest.GORE, QuestDatabase.UNSTARTED);
            QuestDatabase.setQuestProgress(Quest.CLIPPER, QuestDatabase.UNSTARTED);
          }
          // EVE quest started
          else if (text.contains("navigation protocol")) {
            QuestDatabase.setQuestProgress(Quest.EVE, QuestDatabase.STARTED);
            Preferences.setString("EVEDirections", "LLRLR0");
          }
          // EVE quest finished
          else if (text.contains("a tiny parachute")) {
            QuestDatabase.setQuestProgress(Quest.EVE, QuestDatabase.FINISHED);
            Preferences.resetToDefault("EVEDirections");
          }
          // Jungle Pun quest finished (start handled in ResultProcessor)
          else if (text.contains(
              "tape recorder self-destructs with a shower of sparks and a puff of smoke")) {
            EquipmentManager.discardEquipment(ItemPool.MINI_CASSETTE_RECORDER);
            ResultProcessor.removeItem(ItemPool.MINI_CASSETTE_RECORDER);
            QuestDatabase.setQuestProgress(Quest.JUNGLE_PUN, QuestDatabase.UNSTARTED);
            Preferences.resetToDefault("junglePuns");
          }
          // Gore quest finished (start handled in ResultProcessor)
          else if (text.contains("bucket came from")) {
            EquipmentManager.discardEquipment(ItemPool.GORE_BUCKET);
            ResultProcessor.removeItem(ItemPool.GORE_BUCKET);
            QuestDatabase.setQuestProgress(Quest.GORE, QuestDatabase.UNSTARTED);
            Preferences.resetToDefault("goreCollected");
          }
          // Clipper quest finished (start handled in ResultProcessor)
          else if (text.contains("return the fingernails and the clippers")) {
            ResultProcessor.removeItem(ItemPool.FINGERNAIL_CLIPPERS);
            QuestDatabase.setQuestProgress(Quest.CLIPPER, QuestDatabase.UNSTARTED);
            Preferences.resetToDefault("fingernailsClipped");
          }
          // Fake Medium quest started
          else if (text.contains("maximal discretion")) {
            QuestDatabase.setQuestProgress(Quest.FAKE_MEDIUM, QuestDatabase.STARTED);
          }
          // Fake Medium quest finished
          else if (text.contains("toss the device into the ocean")) {
            ResultProcessor.removeItem(ItemPool.ESP_COLLAR);
            QuestDatabase.setQuestProgress(Quest.FAKE_MEDIUM, QuestDatabase.FINISHED);
          }
          // Serum quest started
          else if (text.contains("wonder how many vials they want")) {
            if (InventoryManager.getCount(ItemPool.EXPERIMENTAL_SERUM_P00) >= 5) {
              QuestDatabase.setQuestProgress(Quest.SERUM, "step1");
            } else {
              QuestDatabase.setQuestProgress(Quest.SERUM, QuestDatabase.STARTED);
            }
          }
          // Serum quest finished
          else if (text.contains("drop the vials into it")) {
            QuestDatabase.setQuestProgress(Quest.SERUM, QuestDatabase.FINISHED);
          }
          // Smokes quest started
          else if (text.contains("acquire cigarettes")) {
            QuestDatabase.setQuestProgress(Quest.SMOKES, QuestDatabase.STARTED);
          }
          // Smokes quest finished
          else if (text.contains("cigarettes with a grappling gun")) {
            QuestDatabase.setQuestProgress(Quest.SMOKES, QuestDatabase.FINISHED);
          }
          // Out of Order quest finished
          else if (text.contains("takes your nifty new watch")) {
            EquipmentManager.discardEquipment(ItemPool.GPS_WATCH);
            ResultProcessor.removeItem(ItemPool.GPS_WATCH);
            ResultProcessor.removeItem(ItemPool.PROJECT_TLB);
            QuestDatabase.setQuestProgress(Quest.OUT_OF_ORDER, QuestDatabase.FINISHED);
          }
          // Can't parse quest due to static so visit quest log
          else {
            RequestThread.postRequest(new QuestLogRequest());
          }
          break;
        }

      case 986:
        // Control Panel
        if (ChoiceManager.lastDecision >= 1 && ChoiceManager.lastDecision <= 9) {
          Preferences.setBoolean("_controlPanelUsed", true);
          if (!text.contains("minimum of 24 hours")) {
            Preferences.increment("controlPanelOmega", 11, 100, false);
          }
        }
        break;
      case 987:
        // The Post-Apocalyptic Survivor Encampment
        if (!text.contains("accept your donation")) {
          break;
        }
        int qty = -1;
        if (urlString.contains("giveten")) {
          qty = -10;
        }
        // Declare the pattern here instead of globally because this is available infrequently
        Pattern encampmentPattern = Pattern.compile("whichfood=(\\d+)");
        Matcher encampmentMatcher = encampmentPattern.matcher(urlString);
        if (encampmentMatcher.find()) {
          int encampmentId = StringUtilities.parseInt(encampmentMatcher.group(1));
          ResultProcessor.processItem(encampmentId, qty);
        }
        break;

      case 988:
        // The Containment Unit
        String containment = Preferences.getString("EVEDirections");
        if (containment.length() != 6) {
          break;
        }
        if (text.contains("another pair of doors")) {
          int progress = StringUtilities.parseInt(containment.substring(5, 6));
          if (progress < 0 && progress > 4) {
            break;
          }
          progress++;
          Preferences.setString("EVEDirections", containment.substring(0, 5) + progress);
        } else {
          Preferences.setString("EVEDirections", containment.substring(0, 5) + "0");
        }
        break;

      case 991:
        // Build a Crimbot!
        if (ChoiceManager.lastDecision == 1) {
          ResultProcessor.removeItem(ItemPool.CRIMBONIUM_FUEL_ROD);
        }
        break;

      case 993:
        // Tales of Spelunking
        if (ChoiceManager.lastDecision == 1) {
          KoLCharacter.enterLimitmode(Limitmode.SPELUNKY);
        }
        break;

      case 994:
        // Hide a gift!
        if (text.contains("You hide")) {
          HashMap<Integer, Integer> idMap = new HashMap<Integer, Integer>(3);
          HashMap<Integer, Integer> qtyMap = new HashMap<Integer, Integer>(3);
          int index;
          int id;
          int giftQty;

          Matcher idMatcher = ITEMID_PATTERN.matcher(urlString);
          while (idMatcher.find()) {
            index = StringUtilities.parseInt(idMatcher.group(1));
            if (index < 1) continue;
            id = StringUtilities.parseInt(idMatcher.group(2));
            if (id < 1) continue;
            idMap.put(index, id);
          }

          Matcher qtyMatcher = QTY_PATTERN.matcher(urlString);
          while (qtyMatcher.find()) {
            index = StringUtilities.parseInt(qtyMatcher.group(1));
            if (index < 1) continue;
            giftQty = StringUtilities.parseInt(qtyMatcher.group(2));
            if (giftQty < 1) continue;
            qtyMap.put(index, giftQty);
          }

          for (int i = 1; i <= 3; i++) {
            Integer itemId = idMap.get(i);
            Integer giftQuantity = qtyMap.get(i);
            if (itemId == null || giftQuantity == null) continue;
            ResultProcessor.processResult(ItemPool.get(itemId, -giftQuantity));
          }
          ResultProcessor.removeItem(ItemPool.SNEAKY_WRAPPING_PAPER);
        }
        break;

      case 998:
        if (text.contains("confiscate your deuces")) {
          int removeDeuces = ChoiceManager.lastDecision - 1;
          ResultProcessor.processResult(ItemPool.get(ItemPool.SLEEVE_DEUCE, removeDeuces));
          break;
        }
        if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.removeItem(ItemPool.POCKET_ACE);
        }
        break;

      case 999:
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("_shrubDecorated", true);
          Pattern topperPattern = Pattern.compile("topper=(\\d)");
          Pattern lightsPattern = Pattern.compile("lights=(\\d)");
          Pattern garlandPattern = Pattern.compile("garland=(\\d)");
          Pattern giftPattern = Pattern.compile("gift=(\\d)");
          int decoration;

          Matcher matcher = topperPattern.matcher(urlString);
          if (matcher.find()) {
            decoration = StringUtilities.parseInt(matcher.group(1));
            switch (decoration) {
              case 1:
                Preferences.setString("shrubTopper", "Muscle");
                break;
              case 2:
                Preferences.setString("shrubTopper", "Mysticality");
                break;
              case 3:
                Preferences.setString("shrubTopper", "Moxie");
                break;
            }
          }

          matcher = lightsPattern.matcher(urlString);
          if (matcher.find()) {
            decoration = StringUtilities.parseInt(matcher.group(1));
            switch (decoration) {
              case 1:
                Preferences.setString("shrubLights", "prismatic");
                break;
              case 2:
                Preferences.setString("shrubLights", "Hot");
                break;
              case 3:
                Preferences.setString("shrubLights", "Cold");
                break;
              case 4:
                Preferences.setString("shrubLights", "Stench");
                break;
              case 5:
                Preferences.setString("shrubLights", "Spooky");
                break;
              case 6:
                Preferences.setString("shrubLights", "Sleaze");
                break;
            }
          }

          matcher = garlandPattern.matcher(urlString);
          if (matcher.find()) {
            decoration = StringUtilities.parseInt(matcher.group(1));
            switch (decoration) {
              case 1:
                Preferences.setString("shrubGarland", "HP");
                break;
              case 2:
                Preferences.setString("shrubGarland", "PvP");
                break;
              case 3:
                Preferences.setString("shrubGarland", "blocking");
                break;
            }
          }

          matcher = giftPattern.matcher(urlString);
          if (matcher.find()) {
            decoration = StringUtilities.parseInt(matcher.group(1));
            switch (decoration) {
              case 1:
                Preferences.setString("shrubGifts", "yellow");
                break;
              case 2:
                Preferences.setString("shrubGifts", "meat");
                break;
              case 3:
                Preferences.setString("shrubGifts", "gifts");
                break;
            }
          }
        }
        break;

      case 1002:
        // Temple of the Legend in the Hidden City - Handle quest in Ed
        if (text.contains("The spectre nods emphatically")) {
          QuestDatabase.setQuestProgress(Quest.WORSHIP, QuestDatabase.FINISHED);
          ResultProcessor.removeItem(ItemPool.ED_AMULET);
          if (InventoryManager.getCount(ItemPool.ED_EYE) == 0
              && InventoryManager.getCount(ItemPool.ED_FATS_STAFF) == 0) {
            QuestDatabase.setQuestProgress(Quest.MACGUFFIN, QuestDatabase.FINISHED);
          }
        }
        break;

      case 1003:
        // Test Your Might And Also Test Other Things
        SorceressLairManager.parseContestBooth(ChoiceManager.lastDecision, text);
        break;

      case 1005: // 'Allo
      case 1008: // Pooling Your Resources
      case 1011: // Of Mouseholes and Manholes
        SorceressLairManager.parseMazeTrap(ChoiceManager.lastChoice, text);
        break;

      case 1013: // Mazel Tov!
        // Then you both giggle and head through the exit at the same time.
        QuestDatabase.setQuestProgress(Quest.FINAL, "step5");
        break;

      case 1015: // The Mirror in the Tower has the View that is True
        QuestDatabase.setQuestProgress(Quest.FINAL, "step10");
        break;

      case 1022: // Meet Frank
        // Frank bobs his head toward the hedge maze in front of you.
        QuestDatabase.setQuestProgress(Quest.FINAL, "step4");
        break;

      case 1023: // Like a Bat Into Hell
        if (ChoiceManager.lastDecision == 1) {
          KoLCharacter.setLimitmode(Limitmode.ED);
        }
        break;

      case 1027: // The End of the Tale of Spelunking
        if (ChoiceManager.lastDecision == 1) {
          // Remove all virtual items from inventory/tally
          SpelunkyRequest.resetItems();
        }
        break;

      case 1028: // A Shop
      case 1029: // An Old Clay Pot
      case 1030: // It's a Trap!  A Dart Trap.
      case 1031: // A Tombstone
      case 1032: // It's a Trap!  A Tiki Trap.
      case 1033: // A Big Block of Ice
      case 1034: // A Landmine
      case 1035: // A Crate
      case 1036: // Idolatry
      case 1037: // It's a Trap!  A Smashy Trap.
      case 1038: // A Wicked Web
      case 1039: // A Golden Chest
      case 1040: // It's Lump. It's Lump.
      case 1041: // Spelunkrifice
      case 1045: // Hostile Work Environment
        SpelunkyRequest.parseChoice(ChoiceManager.lastChoice, text, ChoiceManager.lastDecision);
        break;

      case 1042: // Pick a Perk
        SpelunkyRequest.upgrade(ChoiceManager.lastDecision);
        break;

      case 1044:
        // The Gates of Hell
        if (text.contains("unlock the padlock")) {
          SpelunkyRequest.unlock("Hell", "Hell");
        }
        break;

      case 1049:
        // Tomb of the Unknown Your Class Here
        if (text.contains("Also in this room is a ghost")) {
          QuestDatabase.setQuestProgress(Quest.NEMESIS, "step1");
        } else if (text.contains("You acquire")) {
          AscensionClass ascensionClass = KoLCharacter.getAscensionClass();
          int starterWeaponId = ascensionClass == null ? -1 : ascensionClass.getStarterWeapon();
          ResultProcessor.processItem(starterWeaponId, -1);
          QuestDatabase.setQuestProgress(Quest.NEMESIS, "step4");
        }
        break;

      case 1052:
        // Underworld Body Shop
        Matcher skillidMatcher = URL_SKILLID_PATTERN.matcher(urlString);
        if (skillidMatcher.find()) {
          int cost = 0;
          switch (StringUtilities.parseInt(skillidMatcher.group(1))) {
            case 30:
              cost = 5;
              break;
            case 31:
            case 36:
            case 39:
            case 40:
            case 43:
            case 44:
              cost = 10;
              break;
            case 32:
              cost = 15;
              break;
            case 33:
            case 37:
            case 38:
            case 41:
            case 42:
            case 45:
            case 48:
              cost = 20;
              break;
            case 34:
              cost = 25;
              break;
            case 28:
            case 29:
            case 35:
            case 46:
              cost = 30;
              break;
          }
          ResultProcessor.processResult(ItemPool.get(ItemPool.KA_COIN, -cost));
        }
        break;

      case 1053: // The Servants' Quarters
        EdServantData.manipulateServants(request, text);
        break;

      case 1056:
        // Now It's Dark
        // Twin Peak fourth choice
        if (text.contains("When the lights come back")) {
          // the other three must be completed at this point.
          Preferences.setInteger("twinPeakProgress", 15);
        }
        break;

      case 1057:
        // A Stone Shrine
        if (text.contains("shatter the")) {
          KoLCharacter.setHippyStoneBroken(true);
        }
        break;

      case 1059:
        // Helping Make Ends Meat
        if (text.contains("excitedly takes the check")) {
          QuestDatabase.setQuestProgress(Quest.MEATSMITH, QuestDatabase.FINISHED);
          ResultProcessor.removeItem(ItemPool.MEATSMITH_CHECK);
        } else if (text.contains("skeleton store is right next door")
            || text.contains("I'll be here")) {
          QuestDatabase.setQuestProgress(Quest.MEATSMITH, QuestDatabase.STARTED);
        }
        break;

      case 1060:
        // Temporarily Out of Skeletons
        if (text.contains("it snaps off")) {
          ResultProcessor.removeItem(ItemPool.SKELETON_KEY);
        }
        break;

      case 1061:
        // Heart of Madness
        if (ChoiceManager.lastDecision == 1) {
          QuestDatabase.setQuestIfBetter(Quest.ARMORER, "step1");
        }
        if (text.contains("place the popular part")) {
          ResultProcessor.removeItem(ItemPool.POPULAR_PART);
          Preferences.setBoolean("popularTartUnlocked", true);
        }
        break;

      case 1064:
        // The Doctor is Out.  Of Herbs.
        if (ChoiceManager.lastDecision == 1) {
          QuestDatabase.setQuestProgress(Quest.DOC, QuestDatabase.STARTED);
        } else if (ChoiceManager.lastDecision == 2) {
          QuestDatabase.setQuestProgress(Quest.DOC, QuestDatabase.FINISHED);
          ResultProcessor.processResult(ItemPool.get(ItemPool.FRAUDWORT, -3));
          ResultProcessor.processResult(ItemPool.get(ItemPool.SHYSTERWEED, -3));
          ResultProcessor.processResult(ItemPool.get(ItemPool.SWINDLEBLOSSOM, -3));
          HPRestoreItemList.updateHealthRestored();
          MPRestoreItemList.updateManaRestored();
        }
        break;

      case 1065:
        // Lending a Hand (and a Foot)
        if (ChoiceManager.lastDecision == 1) {
          QuestDatabase.setQuestProgress(Quest.ARMORER, QuestDatabase.STARTED);
        } else if (ChoiceManager.lastDecision == 3) {
          QuestDatabase.setQuestIfBetter(Quest.ARMORER, QuestDatabase.STARTED);
        }
        break;

      case 1066:
        // Employee Assignment Kiosk
        if (text.contains("Performance Review:  Sufficient")) {
          EquipmentManager.discardEquipment(ItemPool.TRASH_NET);
          ResultProcessor.removeItem(ItemPool.TRASH_NET);
          Preferences.setInteger("dinseyFilthLevel", 0);
          QuestDatabase.setQuestProgress(Quest.FISH_TRASH, QuestDatabase.UNSTARTED);
        } else if (text.contains("Performance Review:  Unobjectionable")) {
          ResultProcessor.processItem(ItemPool.TOXIC_GLOBULE, -20);
          QuestDatabase.setQuestProgress(Quest.GIVE_ME_FUEL, QuestDatabase.UNSTARTED);
        } else if (text.contains("Performance Review:  Bearable")) {
          Preferences.setInteger("dinseyNastyBearsDefeated", 0);
          QuestDatabase.setQuestProgress(Quest.NASTY_BEARS, QuestDatabase.UNSTARTED);
        } else if (text.contains("Performance Review:  Acceptable")) {
          Preferences.setInteger("dinseySocialJusticeIProgress", 0);
          QuestDatabase.setQuestProgress(Quest.SOCIAL_JUSTICE_I, QuestDatabase.UNSTARTED);
        } else if (text.contains("Performance Review:  Fair")) {
          Preferences.setInteger("dinseySocialJusticeIIProgress", 0);
          QuestDatabase.setQuestProgress(Quest.SOCIAL_JUSTICE_II, QuestDatabase.UNSTARTED);
        } else if (text.contains("Performance Review:  Average")) {
          EquipmentManager.discardEquipment(ItemPool.LUBE_SHOES);
          ResultProcessor.removeItem(ItemPool.LUBE_SHOES);
          QuestDatabase.setQuestProgress(Quest.SUPER_LUBER, QuestDatabase.UNSTARTED);
        } else if (text.contains("Performance Review:  Adequate")) {
          Preferences.setInteger("dinseyTouristsFed", 0);
          ResultProcessor.removeItem(ItemPool.DINSEY_REFRESHMENTS);
          QuestDatabase.setQuestProgress(Quest.WORK_WITH_FOOD, QuestDatabase.UNSTARTED);
        } else if (text.contains("Performance Review:  Tolerable")) {
          EquipmentManager.discardEquipment(ItemPool.MASCOT_MASK);
          ResultProcessor.removeItem(ItemPool.MASCOT_MASK);
          Preferences.setInteger("dinseyFunProgress", 0);
          QuestDatabase.setQuestProgress(Quest.ZIPPITY_DOO_DAH, QuestDatabase.UNSTARTED);
        } else if (text.contains("weren't kidding about the power")) {
          if (InventoryManager.getCount(ItemPool.TOXIC_GLOBULE) >= 20) {
            QuestDatabase.setQuestProgress(Quest.GIVE_ME_FUEL, "step1");
          } else {
            QuestDatabase.setQuestProgress(Quest.GIVE_ME_FUEL, QuestDatabase.STARTED);
          }
        } else if (text.contains("anatomical diagram of a nasty bear")) {
          QuestDatabase.setQuestProgress(Quest.NASTY_BEARS, QuestDatabase.STARTED);
        } else if (text.contains("lists all of the sexist aspects of the ride")) {
          QuestDatabase.setQuestProgress(Quest.SOCIAL_JUSTICE_I, QuestDatabase.STARTED);
        } else if (text.contains("ideas are all themselves so racist")) {
          QuestDatabase.setQuestProgress(Quest.SOCIAL_JUSTICE_II, QuestDatabase.STARTED);
        } else if (text.contains("box of snacks issues forth")) {
          Preferences.setInteger("dinseyTouristsFed", 0);
          QuestDatabase.setQuestProgress(Quest.WORK_WITH_FOOD, QuestDatabase.STARTED);
        }
        break;

      case 1067:
        // Maint Misbehavin'
        if (text.contains("throw a bag of garbage into it")) {
          ResultProcessor.processItem(ItemPool.GARBAGE_BAG, -1);
          Preferences.setBoolean("_dinseyGarbageDisposed", true);
        }
        break;

      case 1076:
        // Mayo Minder&trade;
        switch (ChoiceManager.lastDecision) {
          case 1:
            Preferences.setString("mayoMinderSetting", "Mayonex");
            break;
          case 2:
            Preferences.setString("mayoMinderSetting", "Mayodiol");
            break;
          case 3:
            Preferences.setString("mayoMinderSetting", "Mayostat");
            break;
          case 4:
            Preferences.setString("mayoMinderSetting", "Mayozapine");
            break;
          case 5:
            Preferences.setString("mayoMinderSetting", "Mayoflex");
            break;
          case 6:
            Preferences.setString("mayoMinderSetting", "");
            break;
        }
        break;

      case 1080:
        // Bagelmat-5000
        if (text.contains("shove a wad of dough into the slot")) {
          ResultProcessor.removeItem(ItemPool.DOUGH);
        }
        break;

      case 1081:
        // Assault and Baguettery
        if (ChoiceManager.lastDecision == 1
            || ChoiceManager.lastDecision == 2
            || ChoiceManager.lastDecision == 3) {
          ResultProcessor.removeItem(ItemPool.MAGICAL_BAGUETTE);
        }
        break;

      case 1084:
        // The Popular Machine
        if (text.contains("popular tart springs")) {
          ResultProcessor.removeItem(ItemPool.DOUGH);
          ResultProcessor.removeItem(ItemPool.STRAWBERRY);
          ResultProcessor.removeItem(ItemPool.ENCHANTED_ICING);
        }
        break;

      case 1085:
      case 1086:
        // The Deck of Every Card
        DeckOfEveryCardRequest.postChoice1(text);
        return;

      case 1087:
        // The Dark and Dank and Sinister Cave Entrance
        if (text.contains("stumpy-legged mushroom creatures")) {
          QuestDatabase.setQuestProgress(Quest.NEMESIS, "step12");
        }
        break;

      case 1088:
        // Rubble, Rubble, Toil and Trouble
        if (text.contains("BOOOOOOM!")) {
          ResultProcessor.processItem(ItemPool.FIZZING_SPORE_POD, -6);
          QuestDatabase.setQuestProgress(Quest.NEMESIS, "step15");
        }
        break;

      case 1118:
        // X-32-F Combat Training Snowman Control Console
        switch (ChoiceManager.lastDecision) {
          case 1:
            Preferences.setString("snojoSetting", "MUSCLE");
            break;
          case 2:
            Preferences.setString("snojoSetting", "MYSTICALITY");
            break;
          case 3:
            Preferences.setString("snojoSetting", "MOXIE");
            break;
          case 4:
            Preferences.setString("snojoSetting", "TOURNAMENT");
            break;
        }
        break;

      case 1120:
      case 1121:
      case 1122:
      case 1123:
        // Some Assembly Required
        VYKEACompanionData.assembleCompanion(
            ChoiceManager.lastChoice, ChoiceManager.lastDecision, text);
        break;

      case 1133:
        // Batfellow Begins
        if (ChoiceManager.lastDecision == 1) {
          KoLCharacter.enterLimitmode(Limitmode.BATMAN);
        }
        break;

      case 1137:
        // Bat-Suit Upgrades
        BatManager.batSuitUpgrade(ChoiceManager.lastDecision, text);
        break;

      case 1138:
        // Bat-Sedan Upgrades
        BatManager.batSedanUpgrade(ChoiceManager.lastDecision, text);
        break;

      case 1139:
        // Bat-Cavern Upgrades
        BatManager.batCavernUpgrade(ChoiceManager.lastDecision, text);
        break;

      case 1140:
        // Casing the Conservatory
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.GLOB_OF_BAT_GLUE);
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.removeItem(ItemPool.FINGERPRINT_DUSTING_KIT);
          ResultProcessor.removeItem(ItemPool.FINGERPRINT_DUSTING_KIT);
          ResultProcessor.removeItem(ItemPool.FINGERPRINT_DUSTING_KIT);
        }
        break;

      case 1141:
        // Researching the Reservoir
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.BAT_AID_BANDAGE);
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.removeItem(ItemPool.ULTRACOAGULATOR);
          ResultProcessor.removeItem(ItemPool.ULTRACOAGULATOR);
          ResultProcessor.removeItem(ItemPool.ULTRACOAGULATOR);
        }
        break;

      case 1142:
        // Combing the Cemetery
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.BAT_BEARING);
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.removeItem(ItemPool.EXPLODING_KICKBALL);
          ResultProcessor.removeItem(ItemPool.EXPLODING_KICKBALL);
          ResultProcessor.removeItem(ItemPool.EXPLODING_KICKBALL);
        }
        break;

      case 1143:
        // Searching the Sewers
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.BAT_OOMERANG);
        }
        break;

      case 1144:
        // Assessing the Asylum
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.BAT_O_MITE);
        }
        break;

      case 1145:
        // Looking over the Library
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.BAT_JUTE);
        }
        break;

      case 1146:
        // Considering the Clock Factory
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.EXPLODING_KICKBALL);
        }
        break;

      case 1147:
        // Frisking the Foundry
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.ULTRACOAGULATOR);
        }
        break;

      case 1148:
        // Taking Stock of the Trivia Company
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.FINGERPRINT_DUSTING_KIT);
        }
        break;

      case 1176:
        // Go West, Young Adventurer!
        if (ChoiceManager.lastDecision == 3) {
          // Snake Oilers start with extra
          Preferences.setInteger("awolMedicine", 3);
          Preferences.setInteger("awolVenom", 3);
        }
        break;

      case 1182:
        // Play against the Witchess Pieces
        if (ChoiceManager.lastDecision == 1) {
          KoLAdventure.lastVisitedLocation = null;
          KoLAdventure.lastLocationName = null;
          KoLAdventure.lastLocationURL = urlString;
          KoLAdventure.setLastAdventure("None");
          KoLAdventure.setNextAdventure("None");
          GenericRequest.itemMonster = "Your Witchess Set";
        }
        break;

      case 1190:
        // The Oracle
        if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.removeItem(ItemPool.NO_SPOON);
          QuestDatabase.setQuestProgress(Quest.ORACLE, QuestDatabase.UNSTARTED);
          Preferences.increment("sourceEnlightenment");
          Preferences.setString("sourceOracleTarget", "");
        } else if (ChoiceManager.lastDecision <= 3) {
          QuestDatabase.setQuestProgress(Quest.ORACLE, QuestDatabase.STARTED);
          Matcher matcher = ORACLE_QUEST_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("sourceOracleTarget", matcher.group(1));
          }
        }
        break;

      case 1191:
        // Source Terminal
        request.setHasResult(false);
        if (ChoiceManager.lastDecision != 1) {
          break;
        }
        String input = request.getFormField("input");
        if (input == null) {
          break;
        }
        handleSourceTerminal(input, text);
        break;

      case 1195:
        // Spinning Your Time-Spinner
        if (ChoiceManager.lastDecision == 3) {
          Preferences.increment("_timeSpinnerMinutesUsed");
        } else if (ChoiceManager.lastDecision == 4) {
          Preferences.increment("_timeSpinnerMinutesUsed", 2);
        }
        break;

      case 1196:
        // Travel to a Recent Fight
        if (ChoiceManager.lastDecision == 1 && !urlString.contains("monid=0")) {
          Preferences.increment("_timeSpinnerMinutesUsed", 3);
          EncounterManager.ignoreSpecialMonsters();
        }
        break;

      case 1215:
        // Setting the Clock
        Preferences.setBoolean("_gingerbreadClockVisited", true);

        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("_gingerbreadClockAdvanced", true);
          Preferences.increment("_gingerbreadCityTurns");
        }
        break;

      case 1217:
        // Sweet Synthesis
        SweetSynthesisRequest.postChoice1(urlString, text);
        return;

      case 1222:
        // The Tunnel of L.O.V.E.
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("_loveTunnelUsed", true);
        }
        return;

      case 1229:
        {
          // L.O.V. Exit

          Matcher matcher = LOV_LOGENTRY_PATTERN.matcher(text);
          if (matcher.find()) {
            String message = "Your log entry: " + matcher.group(1);
            RequestLogger.printLine(message);
            RequestLogger.updateSessionLog(message);
          }
          return;
        }

      case 1231:
        // Gummi-Memories, In the Corner of Your Mind
        if (ChoiceManager.lastDecision == 1) {
          ResultProcessor.removeItem(ItemPool.GUMMY_MEMORY);
          Preferences.increment("noobDeferredPoints", 5);
        }
        return;

      case 1234:
        // Spacegate Vaccinator
        //
        // option 1 = Rainbow Vaccine
        // option 2 = Broad-Spectrum Vaccine
        // option 3 = Emotional Vaccine
        //
        // You can unlock it (by turning in enough research) or
        // Select it (if previously unlocked).
        //
        if (text.contains("New vaccine unlocked!")) {
          Preferences.setBoolean("spacegateVaccine" + ChoiceManager.lastDecision, true);
        } else if (text.contains("You acquire an effect")) {
          Preferences.setBoolean("_spacegateVaccine", true);
        }
        break;

      case 1235:
        // Spacegate Terminal
        QuestManager.parseSpacegateTerminal(text, true);
        return;

      case 1246:
        // Land Ho
        if (ChoiceManager.lastDecision == 1) {
          parseLanguageFluency(text, "spacePirateLanguageFluency");
        }
        break;

      case 1247:
        // Half the Ship it used to Be
        if (ChoiceManager.lastDecision == 1) {
          if (text.contains("You acquire an item")) {
            Preferences.setInteger("spacePirateLanguageFluency", 0);
          } else {
            parseLanguageFluency(text, "spacePirateLanguageFluency");
          }
        }
        break;

      case 1248:
        // Paradise Under a Strange Sun
        if (text.contains("You acquire an item")) {
          ResultProcessor.removeItem(ItemPool.SPACE_PIRATE_TREASURE_MAP);
        }
        break;

      case 1249:
        // That's No Monolith, it's a Monolith
        if (ChoiceManager.lastDecision == 1) {
          parseLanguageFluency(text, "procrastinatorLanguageFluency");
          ResultProcessor.removeItem(ItemPool.MURDERBOT_DATA_CORE);
        }
        break;

      case 1250:
        // I'm Afraid It's Terminal
        if (ChoiceManager.lastDecision == 1) {
          if (text.contains("You acquire an item")) {
            Preferences.setInteger("procrastinatorLanguageFluency", 0);
          }
        }
        break;

      case 1251:
        // Curses, A Hex
        if (text.contains("You acquire an item")) {
          ResultProcessor.removeItem(ItemPool.PROCRASTINATOR_LOCKER_KEY);
        }
        break;

      case 1252:
        // Time Enough at Last
        if (ChoiceManager.lastDecision == 1) {
          // You get a Space Baby children's book which
          // will grants spaceBabyLanguageFluency +10
          // when read
        }
        break;

      case 1253:
        // Mother May I
        if (ChoiceManager.lastDecision == 1) {
          if (text.contains("You acquire an item")) {
            Preferences.setInteger("spaceBabyLanguageFluency", 0);
          }
        }
        break;

      case 1254:
        // Please Baby Baby Please
        if (text.contains("You acquire an item")) {
          ResultProcessor.removeItem(ItemPool.SPACE_BABY_BAWBAW);
        }
        break;

      case 1260:
        // A Strange Panel
        if (ChoiceManager.lastDecision < 1 || ChoiceManager.lastDecision > 3) {
          break;
        }
        if (text.contains("10 casualties")
            || text.contains("10 crew")
            || text.contains("10 minions")
            || text.contains("10 ski")
            || text.contains("10 members")
            || text.contains("ten techs")
            || text.contains("10 soldiers")) {
          Preferences.increment("_villainLairProgress", 10);
        } else if (text.contains("5 casualties")
            || text.contains("5 souls")
            || text.contains("5 minions")
            || text.contains("five minions")
            || text.contains("group of ski")
            || text.contains("5 members")
            || text.contains("five people")
            || text.contains("five of us")) {
          Preferences.increment("_villainLairProgress", 5);
        } else {
          Preferences.decrement("_villainLairProgress", 7);
        }
        Preferences.setBoolean("_villainLairColorChoiceUsed", true);
        break;

      case 1261:
        // Which Door?
        if (ChoiceManager.lastDecision == 1) {
          if (text.contains("drop 1000")) {
            Preferences.increment("_villainLairProgress", 5);
            Preferences.setBoolean("_villainLairDoorChoiceUsed", true);
          }
        } else if (ChoiceManager.lastDecision == 2) {
          if (text.contains("insert the key")) {
            int itemId = -1;
            String key = Preferences.getString("_villainLairKey");
            if (key.equals("boris")) {
              itemId = ItemPool.BORIS_KEY;
            } else if (key.equals("jarlsberg")) {
              itemId = ItemPool.JARLSBERG_KEY;
            }
            if (key.equals("pete")) {
              itemId = ItemPool.SNEAKY_PETE_KEY;
            }
            ResultProcessor.removeItem(itemId);
            Preferences.increment("_villainLairProgress", 15);
            Preferences.setBoolean("_villainLairDoorChoiceUsed", true);
          }
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.decrement("_villainLairProgress", 13);
          Preferences.setBoolean("_villainLairDoorChoiceUsed", true);
        }
        break;

      case 1262:
        // What Setting?
        if (ChoiceManager.lastDecision < 1 || ChoiceManager.lastDecision > 3) {
          break;
        }
        if (text.contains("20 of the")
            || text.contains("20 minions")
            || text.contains("20 or so")
            || text.contains("20 soldiers")) {
          Preferences.increment("_villainLairProgress", 20);
        } else if (text.contains("10 or so")
            || text.contains("10 injured")
            || text.contains("10 patrol-sicles")
            || text.contains("10 soldiers")) {
          Preferences.increment("_villainLairProgress", 10);
        } else if (text.contains("15 aquanats")
            || text.contains("15 reserve")
            || text.contains("15 previously")
            || text.contains("15 Soldiers")) {
          Preferences.decrement("_villainLairProgress", 15);
        }
        Preferences.setBoolean("_villainLairSymbologyChoiceUsed", true);
        break;

      case 1266:
        // The Hostler
        if (ChoiceManager.lastDecision < 5) {
          Pattern pattern = Pattern.compile("You rent(|ed) the (.*?)!");
          Matcher matcher = pattern.matcher(text);
          if (matcher.find()) {
            String horse = matcher.group(2);
            Preferences.setString("_horsery", horse);
            String message = "Chose the " + horse;
            RequestLogger.printLine(message);
            RequestLogger.updateSessionLog(message);
            String setting =
                horse.equals("crazy horse")
                    ? "_horseryCrazyName"
                    : horse.equals("dark horse")
                        ? "_horseryDarkName"
                        : horse.equals("normal horse")
                            ? "_horseryNormalName"
                            : horse.equals("pale horse") ? "_horseryPaleName" : null;
            if (setting != null) {
              String name = Preferences.getString(setting);
              Preferences.setString("_horseryCurrentName", name);
            }
          }
        } else if (ChoiceManager.lastDecision == 5) {
          Preferences.setString("_horsery", "");
          Preferences.setString("_horseryCurrentName", "");
          String message = "Returned your horse";
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
        }
        break;

      case 1270:
        // Pantagramming
        // The item that we get has a procedurally-generated name
        request.setHasResult(false);
        PantogramRequest.parseResponse(urlString, text);
        break;

      case 1271:
        // Mummery
        MummeryRequest.parseResponse(ChoiceManager.lastDecision, text);
        break;

      case 1275:
        {
          // Rummaging through the Garbage
          if (ChoiceManager.lastDecision >= 1 && ChoiceManager.lastDecision <= 5) {
            // Remove all of the items before parsing the newly-received one
            EquipmentManager.removeEquipment(ItemPool.DECEASED_TREE);
            ResultProcessor.removeItem(ItemPool.DECEASED_TREE);
            EquipmentManager.removeEquipment(ItemPool.BROKEN_CHAMPAGNE);
            ResultProcessor.removeItem(ItemPool.BROKEN_CHAMPAGNE);
            EquipmentManager.removeEquipment(ItemPool.TINSEL_TIGHTS);
            ResultProcessor.removeItem(ItemPool.TINSEL_TIGHTS);
            EquipmentManager.removeEquipment(ItemPool.WAD_OF_TAPE);
            ResultProcessor.removeItem(ItemPool.WAD_OF_TAPE);
            EquipmentManager.removeEquipment(ItemPool.MAKESHIFT_GARBAGE_SHIRT);
            ResultProcessor.removeItem(ItemPool.MAKESHIFT_GARBAGE_SHIRT);
            if (!Preferences.getBoolean("_garbageItemChanged")) {
              Preferences.setInteger("garbageTreeCharge", 1000);
              Preferences.setInteger("garbageChampagneCharge", 11);
              Preferences.setInteger("garbageShirtCharge", 37);
            }
            Preferences.setBoolean("_garbageItemChanged", true);
          }
          // Do some parsing of needles/wine/scraps here
          Matcher matcher = DECEASED_TREE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger("garbageTreeCharge", StringUtilities.parseInt(matcher.group(1)));
          }
          matcher = BROKEN_CHAMPAGNE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger(
                "garbageChampagneCharge", StringUtilities.parseInt(matcher.group(1)));
          }
          matcher = GARBAGE_SHIRT_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger(
                "garbageShirtCharge", StringUtilities.parseInt(matcher.group(1)));
          }
          break;
        }

      case 1277:
        // Extra, Extra
        if (ChoiceManager.lastDecision >= 1
            && ChoiceManager.lastDecision <= 5
            && text.contains("You acquire")) {
          ResultProcessor.removeItem(ItemPool.BURNING_NEWSPAPER);
        }
        break;

      case 1280:
        // Welcome to FantasyRealm
        if (ChoiceManager.lastDecision != 6) {
          Preferences.setInteger("_frHoursLeft", 5);
          StringBuffer unlocks = new StringBuffer();
          unlocks.append("The Bandit Crossroads,");
          if (Preferences.getBoolean("frMountainsUnlocked")) {
            unlocks.append("The Towering Mountains,");
          }
          if (Preferences.getBoolean("frWoodUnlocked")) {
            unlocks.append("The Mystic Wood,");
          }
          if (Preferences.getBoolean("frSwampUnlocked")) {
            unlocks.append("The Putrid Swamp,");
          }
          if (Preferences.getBoolean("frVillageUnlocked")) {
            unlocks.append("The Cursed Village,");
          }
          if (Preferences.getBoolean("frCemetaryUnlocked")) {
            unlocks.append("The Sprawling Cemetery,");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
        }
        break;

      case 1281:
        {
          // You'll See You at the Crossroads
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Bandit Crossroads,", "");
          }
          if (ChoiceManager.lastDecision == 1) {
            unlocks.append("The Towering Mountains,");
          }
          if (ChoiceManager.lastDecision == 2) {
            unlocks.append("The Mystic Wood,");
          }
          if (ChoiceManager.lastDecision == 3) {
            unlocks.append("The Putrid Swamp,");
          }
          if (ChoiceManager.lastDecision == 4) {
            unlocks.append("The Cursed Village,");
          }
          if (ChoiceManager.lastDecision == 5) {
            unlocks.append("The Sprawling Cemetery,");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1282:
        {
          // Out of Range
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 11) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Towering Mountains,", "");
          }
          if (ChoiceManager.lastDecision == 1) {
            unlocks.append("The Old Rubee Mine,");
            ResultProcessor.removeItem(ItemPool.FR_KEY);
          }
          if (ChoiceManager.lastDecision == 2) {
            unlocks.append("The Foreboding Cave,");
          }
          if (ChoiceManager.lastDecision == 3) {
            unlocks.append("The Master Thief's Chalet,");
          }
          if (ChoiceManager.lastDecision == 4) {
            ResultProcessor.removeItem(ItemPool.FR_DRUIDIC_ORB);
          }
          if (ChoiceManager.lastDecision == 5) {
            unlocks.append("The Ogre Chieftain's Keep,");
          }
          if (ChoiceManager.lastDecision == 10) {
            Preferences.increment("_frButtonsPressed");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1283:
        {
          // Where Wood You Like to Go
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 11) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Mystic Wood,", "");
          }
          if (ChoiceManager.lastDecision == 1) {
            unlocks.append("The Faerie Cyrkle,");
          }
          if (ChoiceManager.lastDecision == 2) {
            unlocks.append("The Druidic Campsite,");
          }
          if (ChoiceManager.lastDecision == 3) {
            unlocks.append("The Ley Nexus,");
          }
          if (ChoiceManager.lastDecision == 10) {
            Preferences.increment("_frButtonsPressed");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1284:
        {
          // Swamped with Leisure
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 11) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Putrid Swamp,", "");
          }
          if (ChoiceManager.lastDecision == 1) {
            unlocks.append("Near the Witch's House,");
          }
          if (ChoiceManager.lastDecision == 2) {
            unlocks.append("The Troll Fortress,");
            ResultProcessor.removeItem(ItemPool.FR_KEY);
          }
          if (ChoiceManager.lastDecision == 3) {
            unlocks.append("The Dragon's Moor,");
          }
          if (ChoiceManager.lastDecision == 10) {
            Preferences.increment("_frButtonsPressed");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1285:
        {
          // It Takes a Cursed Village
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 11) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Cursed Village,", "");
          }
          if (ChoiceManager.lastDecision == 1) {
            unlocks.append("The Evil Cathedral,");
          }
          if (ChoiceManager.lastDecision == 2) {
            unlocks.append("The Cursed Village Thieves' Guild,");
          }
          if (ChoiceManager.lastDecision == 3) {
            unlocks.append("The Archwizard's Tower,");
          }
          if (ChoiceManager.lastDecision == 6) {
            ResultProcessor.removeItem(ItemPool.FR_DRAGON_ORE);
          }
          if (ChoiceManager.lastDecision == 7) {
            ResultProcessor.removeItem(ItemPool.FR_ARREST_WARRANT);
          }
          if (ChoiceManager.lastDecision == 10) {
            Preferences.increment("_frButtonsPressed");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1286:
        {
          // Resting in Peace
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 11) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Sprawling Cemetery,", "");
          }
          if (ChoiceManager.lastDecision == 1) {
            unlocks.append("The Labyrinthine Crypt,");
          }
          if (ChoiceManager.lastDecision == 2) {
            unlocks.append("The Barrow Mounds,");
          }
          if (ChoiceManager.lastDecision == 3) {
            unlocks.append("Duke Vampire's Chateau,");
          }
          if (ChoiceManager.lastDecision == 10) {
            Preferences.increment("_frButtonsPressed");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1288:
        {
          // What's Yours is Yours
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Old Rubee Mine,", "");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1289:
        {
          // A Warm Place
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Foreboding Cave,", "");
          }
          if (ChoiceManager.lastDecision == 3) {
            unlocks.append("The Lair of the Phoenix,");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1290:
        {
          // The Cyrkle Is Compleat
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Faerie Cyrkle,", "");
          }
          if (ChoiceManager.lastDecision == 3) {
            unlocks.append("The Spider Queen's Lair,");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1291:
        {
          // Dudes, Where's My Druids?
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Druidic Campsite,", "");
          }
          if (ChoiceManager.lastDecision == 2) {
            ResultProcessor.removeItem(ItemPool.FR_TAINTED_MARSHMALLOW);
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1292:
        {
          // Witch One You Want?
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "Near the Witch's House,", "");
          }
          if (ChoiceManager.lastDecision == 2) {
            ResultProcessor.removeItem(ItemPool.FR_PURPLE_MUSHROOM);
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1293:
        {
          // Altared States
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Evil Cathedral,", "");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1294:
        {
          // Neither a Barrower Nor a Lender Be
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Barrow Mounds,", "");
          }
          if (ChoiceManager.lastDecision == 3) {
            unlocks.append("The Ghoul King's Catacomb,");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1295:
        {
          // Honor Among You
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Cursed Village Thieves' Guild,", "");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1296:
        {
          // For Whom the Bell Trolls
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Troll Fortress,", "");
          }
          if (ChoiceManager.lastDecision == 3) {
            ResultProcessor.removeItem(ItemPool.FR_CHESWICKS_NOTES);
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1297:
        {
          // Stick to the Crypt
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Labyrinthine Crypt,", "");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1298:
        {
          // The "Phoenix"
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            StringUtilities.singleStringReplace(unlocks, "The Lair of the Phoenix,", "");
          }
          if (ChoiceManager.lastDecision == 1) {
            ResultProcessor.removeItem(ItemPool.FR_HOLY_WATER);
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1299:
        {
          // Stop Dragon Your Feet
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            StringUtilities.singleStringReplace(unlocks, "The Dragon's Moor,", "");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1300:
        {
          // Just Vamping
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            StringUtilities.singleStringReplace(unlocks, "Duke Vampire's Chateau,", "");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1301:
        {
          // Now You've Spied Her
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            StringUtilities.singleStringReplace(unlocks, "The Spider Queen's Lair,", "");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1302:
        {
          // Don't Be Arch
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            StringUtilities.singleStringReplace(unlocks, "The Archwizard's Tower,", "");
          }
          if (ChoiceManager.lastDecision == 1) {
            ResultProcessor.removeItem(ItemPool.FR_CHARGED_ORB);
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1303:
        {
          // Ley Lady Ley
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            StringUtilities.singleStringReplace(unlocks, "The Ley Nexus,", "");
          }
          if (ChoiceManager.lastDecision == 1) {
            ResultProcessor.removeItem(ItemPool.FR_CHESWICKS_COMPASS);
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1304:
        {
          // He Is the Ghoul King, He Can Do Anything
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            StringUtilities.singleStringReplace(unlocks, "The Ghoul King's Catacomb,", "");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1305:
        {
          // The Brogre's Progress
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            StringUtilities.singleStringReplace(unlocks, "The Ogre Chieftain's Keep,", "");
          }
          if (ChoiceManager.lastDecision == 1) {
            ResultProcessor.removeItem(ItemPool.FR_POISONED_SMORE);
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1307:
        {
          // It Takes a Thief
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            StringUtilities.singleStringReplace(unlocks, "The Master Thief's Chalet,", "");
          }
          if (ChoiceManager.lastDecision == 1) {
            ResultProcessor.removeItem(ItemPool.FR_NOTARIZED_WARRANT);
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1312:
        {
          // Choose a Soundtrack
          if (!text.contains("decide not to change the station")) {
            String songChosen = "";
            switch (ChoiceManager.lastDecision) {
              case 1:
                songChosen = "Eye of the Giger";
                break;
              case 2:
                songChosen = "Food Vibrations";
                break;
              case 3:
                songChosen = "Remainin' Alive";
                break;
              case 4:
                songChosen = "These Fists Were Made for Punchin'";
                break;
              case 5:
                songChosen = "Total Eclipse of Your Meat";
                break;
            }
            if (!songChosen.equals("")) {
              if (!KoLCharacter.hasSkill("Sing Along")) {
                KoLCharacter.addAvailableSkill("Sing Along");
              }
              if (!Preferences.getString("boomBoxSong").equals(songChosen)) {
                Preferences.setString("boomBoxSong", songChosen);
                Preferences.decrement("_boomBoxSongsLeft");
                String message = "Setting soundtrack to " + songChosen;
                RequestLogger.printLine(message);
                RequestLogger.updateSessionLog(message);
              }
            } else {
              if (KoLCharacter.hasSkill("Sing Along")) {
                KoLCharacter.removeAvailableSkill("Sing Along");
              }
              if (!Preferences.getString("boomBoxSong").equals("")) {
                Preferences.setString("boomBoxSong", "");
                String message = "Switching soundtrack off";
                RequestLogger.printLine(message);
                RequestLogger.updateSessionLog(message);
              }
            }
          }
          break;
        }

      case 1313: // Bastille Battalion
      case 1314: // Bastille Battalion (Master of None)
      case 1315: // Castle vs. Castle
      case 1316: // GAME OVER
      case 1317: // A Hello to Arms (Battalion)
      case 1318: // Defensive Posturing
      case 1319: // Cheese Seeking Behavior
        BastilleBattalionManager.postChoice1(urlString, request);
        break;

      case 1322:
        {
          // The Beginning of the Neverend
          boolean hard = KoLCharacter.hasEquipped(ItemPool.PARTY_HARD_T_SHIRT);
          Preferences.setBoolean("_partyHard", hard);
          if (ChoiceManager.lastDecision == 1) {
            // Decided to quest
            Preferences.setInteger("encountersUntilNEPChoice", 7);

            String quest = Preferences.getString("_questPartyFairQuest");
            if (quest.equals("booze") || quest.equals("food")) {
              QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, QuestDatabase.STARTED);
              Preferences.setString("_questPartyFairProgress", "");
            } else {
              QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, "step1");
              if (quest.equals("woots")) {
                Preferences.setInteger("_questPartyFairProgress", 10);
              } else if (quest.equals("partiers")) {
                if (hard) {
                  Preferences.setInteger("_questPartyFairProgress", 100);
                } else {
                  Preferences.setInteger("_questPartyFairProgress", 50);
                }
              } else if (quest.equals("dj")) {
                if (hard) {
                  Preferences.setInteger("_questPartyFairProgress", 10000);
                } else {
                  Preferences.setInteger("_questPartyFairProgress", 5000);
                }
              } else if (quest.equals("trash")) {
                // The amount isn't known, so check quest log
                (new GenericRequest("questlog.php?which=1")).run();
              }
            }
          } else if (ChoiceManager.lastDecision == 2) {
            // Decided to party
            Preferences.setString("_questPartyFair", "");
            Preferences.setString("_questPartyFairQuest", "");
            Preferences.setString("_questPartyFairProgress", "");
          }
          break;
        }

      case 1323:
        // All Done!
        QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, QuestDatabase.FINISHED);
        Preferences.setString("_questPartyFairQuest", "");
        Preferences.setString("_questPartyFairProgress", "");
        break;

      case 1324:
        // It Hasn't Ended, It's Just Paused
        // Decision 5 is followed by a fight, which will decrement the free turns available itself
        if (ChoiceManager.lastDecision != 5) {
          Preferences.decrement("encountersUntilNEPChoice", 1, 0);
          Preferences.increment("_neverendingPartyFreeTurns", 1, 10, false);
        }
        break;

      case 1325:
        // A Room With a View...  Of a Bed
        if (ChoiceManager.lastDecision == 3) {
          // Removes 30% of current partiers
          int current = Preferences.getInteger("_questPartyFairProgress");
          Preferences.setInteger(
              "_questPartyFairProgress", current - (int) Math.floor(current * 0.3));
          ResultProcessor.removeItem(ItemPool.JAM_BAND_BOOTLEG);
        } else if (ChoiceManager.lastDecision == 4) {
          // On dj quest (choice number guessed)
          Matcher matcher = SAFE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.decrement(
                "_questPartyFairProgress", StringUtilities.parseInt(matcher.group(1)), 0);
            if (Preferences.getInteger("_questPartyFairProgress") < 1) {
              QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, "step2");
            }
          }
        } else if (ChoiceManager.lastDecision == 5) {
          // On woots quest
          Preferences.increment("_questPartyFairProgress", 20, 100, false);
          if (Preferences.getInteger("_questPartyFairProgress") == 100) {
            QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, "step2");
          }
          ResultProcessor.removeItem(ItemPool.VERY_SMALL_RED_DRESS);
        }
        break;

      case 1326:
        // Gone Kitchin'
        if (ChoiceManager.lastDecision == 3) {
          Matcher matcher = GERALDINE_PATTERN.matcher(text);
          if (matcher.find()) {
            int itemCount = StringUtilities.parseInt(matcher.group(1));
            int itemId = ItemDatabase.getItemIdFromDescription(matcher.group(2));
            Preferences.setString("_questPartyFairProgress", itemCount + " " + itemId);
            if (InventoryManager.getCount(itemId) >= itemCount) {
              QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, "step2");
            }
          }
          QuestDatabase.setQuestIfBetter(Quest.PARTY_FAIR, "step1");
        } else if (ChoiceManager.lastDecision == 4) {
          String pref = Preferences.getString("_questPartyFairProgress");
          String itemIdString = null;
          int position = pref.indexOf(" ");
          if (position > 0) {
            itemIdString = pref.substring(position);
            if (itemIdString != null) {
              ResultProcessor.processItem(StringUtilities.parseInt(itemIdString), -10);
            }
          }
          QuestDatabase.setQuestIfBetter(Quest.PARTY_FAIR, QuestDatabase.FINISHED);
          Preferences.setString("_questPartyFairQuest", "");
          Preferences.setString("_questPartyFairProgress", "");
        } else if (ChoiceManager.lastDecision == 5) {
          Matcher matcher = TRASH_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.decrement(
                "_questPartyFairProgress", StringUtilities.parseInt(matcher.group(1)), 0);
          }
        }
        break;

      case 1327:
        // Forward to the Back
        if (ChoiceManager.lastDecision == 3) {
          Matcher matcher = GERALD_PATTERN.matcher(text);
          if (matcher.find()) {
            int itemCount = StringUtilities.parseInt(matcher.group(1));
            int itemId = ItemDatabase.getItemIdFromDescription(matcher.group(2));
            Preferences.setString("_questPartyFairProgress", itemCount + " " + itemId);
            if (InventoryManager.getCount(itemId) >= itemCount) {
              QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, "step2");
            }
          }
          QuestDatabase.setQuestIfBetter(Quest.PARTY_FAIR, "step1");
        } else if (ChoiceManager.lastDecision == 4) {
          String pref = Preferences.getString("_questPartyFairProgress");
          String itemIdString = null;
          int position = pref.indexOf(" ");
          if (position > 0) {
            itemIdString = pref.substring(position);
            if (itemIdString != null) {
              int itemCount = Preferences.getBoolean("_partyHard") ? 20 : 10;
              ResultProcessor.processItem(StringUtilities.parseInt(itemIdString), -itemCount);
            }
          }
          QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, QuestDatabase.FINISHED);
          Preferences.setString("_questPartyFairQuest", "");
          Preferences.setString("_questPartyFairProgress", "");
        } else if (ChoiceManager.lastDecision == 5) {
          // Removes 20% of current partiers
          int current = Preferences.getInteger("_questPartyFairProgress");
          Preferences.setInteger(
              "_questPartyFairProgress", current - (int) Math.floor(current * 0.2));
          ResultProcessor.removeItem(ItemPool.PURPLE_BEAST_ENERGY_DRINK);
        }
        break;

      case 1328:
        // Basement Urges
        if (ChoiceManager.lastDecision == 4) {
          // On woots quest
          Preferences.increment("_questPartyFairProgress", 20, 100, false);
          if (Preferences.getInteger("_questPartyFairProgress") == 100) {
            QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, "step2");
          }
          ResultProcessor.removeItem(ItemPool.ELECTRONICS_KIT);
        }
        break;

      case 1329:
        // Latte Shop
        // The item that we get has a procedurally-generated name
        request.setHasResult(false);
        LatteRequest.parseResponse(urlString, text);
        break;

      case 1331:
        // Daily Loathing Ballot
        if (ChoiceManager.lastDecision == 1 && !text.contains("must vote for a candidate")) {
          ModifierList modList = new ModifierList();
          Matcher matcher = URL_VOTE_PATTERN.matcher(urlString);
          while (matcher.find()) {
            int vote = StringUtilities.parseInt(matcher.group(1)) + 1;
            String pref = Preferences.getString("_voteLocal" + vote);
            ModifierList addModList = Modifiers.splitModifiers(pref);
            for (Modifier modifier : addModList) {
              modList.addToModifier(modifier);
            }
          }
          Preferences.setString("_voteModifier", modList.toString());
          String message = "You have cast your votes";
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
        }
        break;

      case 1332:
        // government requisition form
        ResultProcessor.removeItem(ItemPool.GOVERNMENT_REQUISITION_FORM);
        break;

      case 1333:
        // Canadian cabin
        if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.removeItem(ItemPool.MOOSEFLANK);
        } else if (ChoiceManager.lastDecision == 3) {
          ResultProcessor.processItem(ItemPool.WALRUS_BLUBBER, -10);
        } else if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.processItem(ItemPool.TINY_BOMB, -10);
        }
        break;

      case 1334:
        // Boxing Daycare (Lobby)
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("_daycareNap", true);
        } else if (ChoiceManager.lastDecision == 2
            && text.contains("only allowed one spa treatment")) {
          Preferences.setBoolean("_daycareSpa", true);
        }
        break;

      case 1335:
        // Boxing Day Spa
        if (ChoiceManager.lastDecision != 5) {
          Preferences.setBoolean("_daycareSpa", true);
        }
        break;

      case 1336:
        {
          // Boxing Daycare
          String message1 = null;
          String message2 = null;
          if (ChoiceManager.lastDecision == 1) {
            Matcher matcher = DAYCARE_RECRUIT_PATTERN.matcher(text);
            if (matcher.find()) {
              message1 = "Activity: Recruit toddlers";
              message2 = "You have recruited " + matcher.group(1) + " toddlers";
              Preferences.increment("_daycareRecruits");
            }
          } else if (ChoiceManager.lastDecision == 2) {
            Matcher matcher = DAYCARE_EQUIPMENT_PATTERN.matcher(text);
            if (matcher.find()) {
              AdventureResult effect = EffectPool.get(EffectPool.BOXING_DAY_BREAKFAST, 0);
              boolean haveBreakfast = effect.getCount(KoLConstants.activeEffects) > 0;
              String countString = matcher.group(1);
              int equipment = StringUtilities.parseInt(countString.replaceAll(",", ""));
              Preferences.setInteger("daycareLastScavenge", equipment / (haveBreakfast ? 2 : 1));

              message1 = "Activity: Scavenge for gym equipment";
              message2 = "You have found " + countString + " pieces of gym equipment";
              Preferences.increment("_daycareGymScavenges");
            }
          } else if (ChoiceManager.lastDecision == 3) {
            if (text.contains("new teacher joins the staff")) {
              message1 = "Activity: Hire an instructor";
              message2 = "You have hired a new instructor";
            }
            Matcher matcher = DAYCARE_ITEM_PATTERN.matcher(text);
            if (matcher.find()) {
              String itemName = matcher.group(1);
              int itemCount = StringUtilities.parseInt(matcher.group(2).replaceAll(",", ""));
              ResultProcessor.processItem(ItemDatabase.getItemId(itemName), -itemCount);
            }
          } else if (ChoiceManager.lastDecision == 4) {
            if (text.contains("step into the ring")) {
              message1 = "Activity: Spar";
              Preferences.setBoolean("_daycareFights", true);
            }
          }
          if (message1 != null) {
            RequestLogger.printLine(message1);
            RequestLogger.updateSessionLog(message1);
          }
          if (message2 != null) {
            RequestLogger.printLine(message2);
            RequestLogger.updateSessionLog(message2);
          }
          break;
        }

      case 1340:
        // Is There A Doctor In The House?
        if (ChoiceManager.lastDecision == 1) {
          if (Preferences.getString("doctorBagQuestItem").isEmpty()) {
            // We didn't recognise quest text, so get it from quest log
            RequestThread.postRequest(new QuestLogRequest());
          }
          int itemId = ItemDatabase.getItemId(Preferences.getString("doctorBagQuestItem"));

          if (itemId > 0) {
            if (InventoryManager.getCount(itemId) > 0) {
              QuestDatabase.setQuestProgress(Quest.DOCTOR_BAG, "step1");
            } else {
              QuestDatabase.setQuestProgress(Quest.DOCTOR_BAG, QuestDatabase.STARTED);
            }
          } else {
            QuestDatabase.setQuestProgress(Quest.DOCTOR_BAG, QuestDatabase.STARTED);
          }
        } else {
          QuestDatabase.setQuestProgress(Quest.DOCTOR_BAG, QuestDatabase.UNSTARTED);
          Preferences.setString("doctorBagQuestItem", "");
          Preferences.setString("doctorBagQuestLocation", "");
        }
        break;

      case 1341:
        // A Pound of Cure
        if (ChoiceManager.lastDecision == 1) {
          String itemName = Preferences.getString("doctorBagQuestItem");
          if (!itemName.equals("")) {
            ResultProcessor.processItem(ItemDatabase.getItemId(itemName), -1);
          } else {
            // Don't know item to remove so refresh inventory instead
            ApiRequest.updateInventory();
          }
          QuestDatabase.setQuestProgress(Quest.DOCTOR_BAG, QuestDatabase.UNSTARTED);
          Preferences.setString("doctorBagQuestItem", "");
          Preferences.setString("doctorBagQuestLocation", "");
          if (text.contains("One of the five green lights")) {
            Preferences.setInteger("doctorBagQuestLights", 1);
          } else if (text.contains("second of the five green lights")) {
            Preferences.setInteger("doctorBagQuestLights", 2);
          } else if (text.contains("third of the five green lights")) {
            Preferences.setInteger("doctorBagQuestLights", 3);
          } else if (text.contains("fourth of the five green lights")) {
            Preferences.setInteger("doctorBagQuestLights", 4);
          } else if (text.contains("lights go dark again")) {
            Preferences.setInteger("doctorBagQuestLights", 0);
          }
          if (text.contains("bag has been permanently upgraded")) {
            Preferences.increment("doctorBagUpgrades");
          }
        }
        break;

      case 1342:
        // Torpor
        if (ChoiceManager.lastDecision == 2) {
          // You can learn or forget Vampyre skills
          for (int i = 10; i < 39; i++) {
            if (urlString.contains("sk[]=" + i) && !KoLCharacter.hasSkill(24000 + i)) {
              String skillName = SkillDatabase.getSkillName(24000 + i);
              KoLCharacter.addAvailableSkill(skillName);
              String message = "You have learned " + skillName;
              RequestLogger.printLine(message);
              RequestLogger.updateSessionLog(message);
            }
            if (!urlString.contains("sk[]=" + i) && KoLCharacter.hasSkill(24000 + i)) {
              String skillName = SkillDatabase.getSkillName(24000 + i);
              KoLCharacter.removeAvailableSkill(skillName);
              String message = "You have forgotten " + skillName;
              RequestLogger.printLine(message);
              RequestLogger.updateSessionLog(message);
            }
          }
        }
        break;

      case 1360:
        // Like Shops in the Night
        if (ChoiceManager.lastDecision == 5 && text.contains("You gain 500 gold")) {
          // Sell them the cursed compass
          // Remove from equipment (including checkpoints)
          if (EquipmentManager.discardEquipment(ItemPool.CURSED_COMPASS) == -1) {
            // Remove from inventory
            ResultProcessor.removeItem(ItemPool.CURSED_COMPASS);
          }
        }
        break;

      case 1386:
        SaberRequest.parseUpgrade(urlString, text);
        break;

      case 1387:
        SaberRequest.parseForce(urlString, text);
        break;

      case 1388:
        BeachManager.parseBeachHeadCombing(text);
        BeachManager.parseCombUsage(urlString, text);
        break;

      case 1391:
        {
          // Rationing out Destruction
          //    choice.php?whichchoice=1391&option=1&pwd&tossid=10321

          Matcher tossMatcher = TOSSID_PATTERN.matcher(urlString);
          if (!tossMatcher.find()) {
            break;
          }
          int itemId = StringUtilities.parseInt(tossMatcher.group(1));
          ResultProcessor.processItem(itemId, -1);

          String army = null;
          String property = null;
          String consumable = null;
          int casualties = 0;

          if (casualties == 0) {
            Matcher fratMatcher = FRAT_RATIONING_PATTERN.matcher(text);
            if (fratMatcher.find()) {
              army = "frat boys";
              property = "fratboysDefeated";
              consumable = fratMatcher.group(1);
              casualties = StringUtilities.parseInt(fratMatcher.group(2));
            }
          }
          if (casualties == 0) {
            Matcher hippyMatcher = HIPPY_RATIONING_PATTERN.matcher(text);
            if (hippyMatcher.find()) {
              army = "hippies";
              property = "hippiesDefeated";
              consumable = hippyMatcher.group(1);
              casualties = StringUtilities.parseInt(hippyMatcher.group(2));
            }
          }

          Preferences.increment(property, casualties);

          String message = "You defeated " + casualties + " " + army + " with some " + consumable;
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
          break;
        }

      case 1392:
        DecorateTentRequest.parseDecoration(urlString, text);
        break;

      case 1394:
        // Send up a Smoke Signal
        if (ChoiceManager.lastDecision == 1
            && text.contains("You send a smoky message to the sky.")) {
          // Remove from inventory
          ResultProcessor.removeItem(ItemPool.CAMPFIRE_SMOKE);
        }
        break;

      case 1396:
        // Adjusting Your Fish
        // choice.php?pwd&whichchoice=1396&option=1&cat=fish
        if (urlString.contains("option=1") && urlString.contains("cat=")) {
          Pattern pattern = Pattern.compile("cat=([^&]*)");
          Matcher matcher = pattern.matcher(urlString);
          if (matcher.find()) {
            String phylum = matcher.group(1);
            String fixed = phylum.equals("merkin") ? "mer-kin" : phylum;
            Preferences.setString("redSnapperPhylum", fixed);
            Preferences.setInteger("redSnapperProgress", 0);
          }
        }
        break;

      case 1399:
        // New Favorite Bird?
        // Auto correct, in case we cast this outside of KoLmafia
        Preferences.setInteger("_birdsSoughtToday", 6);
        if (ChoiceManager.lastDecision == 1) {
          String bird = Preferences.getString("_birdOfTheDay");
          Preferences.setString("yourFavoriteBird", bird);
          ResponseTextParser.learnSkill("Visit your Favorite Bird");
          ResultProcessor.updateBirdModifiers(
              EffectPool.BLESSING_OF_YOUR_FAVORITE_BIRD, "yourFavoriteBird");
        }
        break;

      case 1406:
        {
          // Drippy House on the Prairie
          // 1 = Explore the House
          // Even though the house doesn't have a door, you check under the mat for a key anyway.
          // You don't find one, but you <i>do</i> find a little puddle of those Driplet things
          // Jeremy told you about.
          // 1 = Keep Exploring
          // Just inside the door of the house, you discover a colony of nasty bat-looking things
          // nesting in the rafters.
          // In one of the side rooms of the house, you find a giant spiral shell stuck to the wall.
          //  You pry it loose -- Jeremy will probably want to see this.
          // In the house's kitchen, you find a bucket underneath a dripping sink pipe. The
          // oddly-solid drops make a -thunk-... -thunk-... -thunk-... sound as they fall.
          // In the backyard of the house, you notice a strange fruit tree you hadn't seen before.
          // It's maybe... plums? Kinda hard to tell, what with everything being made out of the
          // same gross stuff.
          // In one of the back rooms, you find a workbench covered in drippy woodworking supplies.
          // Underneath a shattered bed frame in one of the back rooms, you find a trap door in the
          // floor. You press your ear to it and hear some... Thing... slithering around underneath
          // it.
          // 2 = Dislodge some bats
          // You flush some of the vile bat-things out of the rafters and into the nearby forest.
          // No way that'll come back to bite you in the ass!
          // 3 = Check the bucket under the sink
          // You look in the bucket and find a few Driplets.  You collect them and put the bucket
          // back under the sink.
          // 4 - Pick a nasty fruit
          // You pluck the least nasty-looking plum(?) from the tree and pocket it.
          // 5 - Check out the woodworking bench
          // You use the tools to carve your truncheon into a sharp stake.
          // 6 - Go down to the basement
          // 9 = Leave
          if (text.contains("vile bat-things")) {
            Preferences.increment("drippyBatsUnlocked", 7);
          }
          if (text.contains("sharp stake")) {
            EquipmentManager.discardEquipment(ItemPool.DRIPPY_TRUNCHEON);
          }

          // Since this choice appears on a schedule - the 16th
          // adventure in The Dripping Trees and then every 15
          // turns thereafter - "fix" the adventure count as needed.
          int advs = Preferences.getInteger("drippingTreesAdventuresSinceAscension");
          if (advs < 16) {
            Preferences.setInteger("drippingTreesAdventuresSinceAscension", 16);
            advs = 16;
          }
          int mod = (advs - 1) % 15;
          if (mod != 0) {
            Preferences.increment("drippingTreesAdventuresSinceAscension", 15 - mod);
          }

          break;
        }

      case 1407:
        {
          // Mushroom District Costume Shop

          String costume = "none";
          if (text.contains("You slip into something a little more carpentable")) {
            costume = "muscle";
          } else if (text.contains("You let down your guard and put on the gardener costume")) {
            costume = "mysticality";
          } else if (text.contains("Todge holds out a tutu and you jump into it")) {
            costume = "moxie";
          } else {
            break;
          }

          int cost = Preferences.getInteger("plumberCostumeCost");
          ResultProcessor.processItem(ItemPool.COIN, -cost);
          Preferences.increment("plumberCostumeCost", 50);
          Preferences.setString("plumberCostumeWorn", costume);
          break;
        }

      case 1408:
        {
          // Mushroom District Badge Shop
          // If successful, deduct coins.

          if (text.contains("You acquire a skill")) {
            int cost = Preferences.getInteger("plumberBadgeCost");
            ResultProcessor.processItem(ItemPool.COIN, -cost);
            Preferences.increment("plumberBadgeCost", 25);
          }
          break;
        }

      case 1410:
        {
          // The Mushy Center
          int mushroomLevel = 1; // Tomorrow's mushroom
          switch (ChoiceManager.lastDecision) {
            case 1:
              // Fertilize the mushroom
              mushroomLevel = Preferences.increment("mushroomGardenCropLevel", 1, 11, false);
              break;
            case 2:
              // Pick the mushroom
              Preferences.setInteger("mushroomGardenCropLevel", 1);
              break;
          }
          CampgroundRequest.clearCrop();
          CampgroundRequest.setCampgroundItem(new Mushroom(mushroomLevel));
          Preferences.setBoolean("_mushroomGardenVisited", true);
          break;
        }

      case 1411:
        {
          // The Hall in the Hall
          switch (ChoiceManager.lastDecision) {
            case 1:
              Preferences.setBoolean("_drippingHallDoor1", true);
              // If you acquire a drippy org, count it
              if (text.contains("drippy orb")) {
                Preferences.increment("drippyOrbsClaimed");
              } else {
                int known = Preferences.getInteger("drippyOrbsClaimed");
                int min = KoLCharacter.estimatedPoolSkill() / 20;
                Preferences.setInteger("drippyOrbsClaimed", Math.max(known, min));
              }
              break;
            case 2:
              Preferences.setBoolean("_drippingHallDoor2", true);
              break;
            case 3:
              Preferences.setBoolean("_drippingHallDoor3", true);
              break;
            case 4:
              Preferences.setBoolean("_drippingHallDoor4", true);

              // If you acquire drippy pilsner, uses a drippy stein
              if (text.contains("drippy pilsner")) {
                ResultProcessor.processItem(ItemPool.DRIPPY_STEIN, -1);
              }
              break;
          }

          // This only advances upon defeating a dripping reveler
          // or encountering this choice
          int advs = Preferences.increment("drippingHallAdventuresSinceAscension");

          // Since this choice appears on a schedule - the 12th
          // adventure in The Dripping Hall and then every 12
          // turns thereafter - "fix" the adventure count as needed.
          if (advs < 12) {
            Preferences.setInteger("drippingHallAdventuresSinceAscension", 12);
            advs = 12;
          }
          int mod = advs % 12;
          if (mod != 0) {
            Preferences.increment("drippingHallAdventuresSinceAscension", 12 - mod);
          }

          break;
        }

      case 1412:
        {
          // Guzzlr Client Selection
          switch (ChoiceManager.lastDecision) {
            case 1:
              // Abandon
              Preferences.setBoolean("_guzzlrQuestAbandoned", true);

              Preferences.setString("guzzlrQuestBooze", "");
              Preferences.setString("guzzlrQuestClient", "");
              Preferences.setString("guzzlrQuestLocation", "");
              Preferences.setString("guzzlrQuestTier", "");
              QuestDatabase.setQuestProgress(Quest.GUZZLR, QuestDatabase.UNSTARTED);
              break;
            case 2:
            case 3:
            case 4:
              {
                Matcher locationMatcher = GUZZLR_LOCATION_PATTERN.matcher(text);
                Matcher boozeMatcher = DESCID_PATTERN.matcher(text);

                String tier = "";

                if (locationMatcher.find()) {
                  tier = locationMatcher.group(1).toLowerCase();
                  Preferences.setString("guzzlrQuestClient", locationMatcher.group(2));
                  Preferences.setString("guzzlrQuestLocation", locationMatcher.group(4));
                }

                // If we didn't capture from the ouput we can determine from the choice
                if (tier.equals("")) {
                  tier =
                      ChoiceManager.lastDecision == 2
                          ? "bronze"
                          : ChoiceManager.lastDecision == 3 ? "gold" : "platinum";
                }

                // Remember the tier of the current quest
                Preferences.setString("guzzlrQuestTier", tier);

                // Increment the number of gold or platinum deliveres STARTED today
                if (!tier.equals("bronze")) {
                  Preferences.increment(
                      "_guzzlr" + StringUtilities.toTitleCase(tier) + "Deliveries", 1);
                }

                if (boozeMatcher.find()) {
                  int itemId = ItemDatabase.getItemIdFromDescription(boozeMatcher.group(1));
                  Preferences.setString("guzzlrQuestBooze", ItemDatabase.getItemName(itemId));
                }

                if (Preferences.getString("guzzlrQuestBooze").isEmpty()
                    || Preferences.getString("guzzlrQuestLocation").isEmpty()) {
                  RequestThread.postRequest(new QuestLogRequest());
                }

                int itemId = ItemDatabase.getItemId(Preferences.getString("guzzlrQuestBooze"));

                if (itemId > 0 && itemId != ItemPool.GUZZLR_COCKTAIL_SET) {
                  if (InventoryManager.getCount(itemId) > 0) {
                    QuestDatabase.setQuestProgress(Quest.GUZZLR, "step1");
                  } else {
                    QuestDatabase.setQuestProgress(Quest.GUZZLR, QuestDatabase.STARTED);
                  }
                } else {
                  QuestDatabase.setQuestProgress(Quest.GUZZLR, QuestDatabase.STARTED);
                }
                break;
              }
          }

          break;
        }

      case 1414:
        Preferences.setBoolean("lockPicked", true);
        break;

      case 1418:
        // So Cold
        if (ChoiceManager.lastDecision == 1) {
          KoLCharacter.findFamiliar(FamiliarPool.MELODRAMEDARY).loseExperience();
          Preferences.setBoolean("_entauntaunedToday", true);
        }
        break;

      case 1420:
        // Cargo Cultist Shorts
        if (ChoiceManager.lastDecision == 1) {
          CargoCultistShortsRequest.parsePocketPick(urlString, text);
        }
        break;

      case 1435:
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("mappingMonsters", false);
        }
        break;
      case 1437:
        {
          // Configuring the retro cape washing instructions
          String instructions =
              ChoiceManager.lastDecision == 2
                  ? "hold"
                  : ChoiceManager.lastDecision == 3
                      ? "thrill"
                      : ChoiceManager.lastDecision == 4
                          ? "kiss"
                          : ChoiceManager.lastDecision == 5 ? "kill" : null;

          if (instructions != null) {
            Preferences.setString("retroCapeWashingInstructions", instructions);
            ItemDatabase.setCapeSkills();
          }
          break;
        }
      case 1438:
        {
          // Configuring the retro cape superhero
          String hero =
              ChoiceManager.lastDecision == 1
                  ? "vampire"
                  : ChoiceManager.lastDecision == 2
                      ? "heck"
                      : ChoiceManager.lastDecision == 3 ? "robot" : null;

          if (hero != null) {
            Preferences.setString("retroCapeSuperhero", hero);
            ItemDatabase.setCapeSkills();
          }
          break;
        }
      case 1442:
        {
          if (text.contains("You fill out all the appropriate forms")) {
            ResultProcessor.removeItem(ItemPool.GOVERNMENT_FOOD_SHIPMENT);
          }
          break;
        }
      case 1443:
        {
          if (text.contains("You fill out all the appropriate forms")) {
            ResultProcessor.removeItem(ItemPool.GOVERNMENT_BOOZE_SHIPMENT);
          }
          break;
        }
      case 1444:
        {
          if (text.contains("You fill out all the appropriate forms")) {
            ResultProcessor.removeItem(ItemPool.GOVERNMENT_CANDY_SHIPMENT);
          }
          break;
        }

      case 1445: // Reassembly Station
      case 1447: // Statbot 5000
        YouRobotManager.postChoice1(urlString, request);
        break;

      case 1448:
        {
          if (text.contains("You acquire an item:")) {
            Matcher stalkMatcher = Pattern.compile("pp=(\\d+)").matcher(urlString);
            if (stalkMatcher.find()) {
              int pp = Integer.parseInt(stalkMatcher.group(1)) - 1;
              String[] stalkStatus = Preferences.getString("_pottedPowerPlant").split(",");
              stalkStatus[pp] = "0";
              Preferences.setString("_pottedPowerPlant", String.join(",", stalkStatus));
            }
          }
          break;
        }
      case 1449:
        {
          switch (ChoiceManager.lastDecision) {
            case 1:
              Preferences.setString("backupCameraMode", "ml");
              break;
            case 2:
              Preferences.setString("backupCameraMode", "meat");
              break;
            case 3:
              Preferences.setString("backupCameraMode", "init");
              break;
            case 4:
              Preferences.setBoolean("backupCameraReverserEnabled", true);
              break;
            case 5:
              Preferences.setBoolean("backupCameraReverserEnabled", false);
              break;
          }

          break;
        }
      case 1451:
        // Fire Captain Hagnk
        if (ChoiceManager.lastDecision == 1) {
          Matcher locationIdMatcher = Pattern.compile("zid=([^&]*)").matcher(urlString);
          if (locationIdMatcher.find()) {
            int zid = StringUtilities.parseInt(locationIdMatcher.group(1));
            KoLAdventure location =
                AdventureDatabase.getAdventureByURL("adventure.php?snarfblat=" + zid);
            WildfireCampRequest.reduceFireLevel(location);
          }
        } else if (ChoiceManager.lastDecision == 3) {
          if (text.contains("Hagnk takes your fire extinguisher")) {
            Preferences.setInteger("_fireExtinguisherCharge", 100);
            Preferences.setBoolean("_fireExtinguisherRefilled", true);
          }
        }
        WildfireCampRequest.parseCaptain(text);
        break;
      case 1452:
        // Sprinkler Joe
        if (ChoiceManager.lastDecision == 1 && text.contains("raindrop.gif")) {
          Preferences.setBoolean("wildfireSprinkled", true);
        }
        break;
      case 1453:
        // Fracker Dan
        if (ChoiceManager.lastDecision == 1 && text.contains("raindrop.gif")) {
          Preferences.setBoolean("wildfireFracked", true);
        }
        break;
      case 1454:
        // Cropduster Dusty
        if (ChoiceManager.lastDecision == 1 && text.contains("raindrop.gif")) {
          Preferences.setBoolean("wildfireDusted", true);
        }
        break;
      case 1455:
        // Cold Medicine Cabinet
        if (ChoiceManager.lastDecision != 6) {
          Preferences.increment("_coldMedicineConsults", 1, 5, false);
          Preferences.setInteger("_nextColdMedicineConsult", KoLCharacter.getTurnsPlayed() + 20);
        }
        break;
    }
  }

  // <td align="center" valign="middle"><a href="choice.php?whichchoice=810&option=1&slot=7&pwd=xxx"
  // style="text-decoration:none"><img alt='Toybot (Level 3)' title='Toybot (Level 3)' border=0
  // src='http://images.kingdomofloathing.com/otherimages/crimbotown/krampus_toybot.gif' /></a></td>
  private static final Pattern URL_SLOT_PATTERN = Pattern.compile("slot=(\\d+)");
  private static final Pattern BOT_PATTERN = Pattern.compile("<td.*?<img alt='([^']*)'.*?</td>");

  private static String findKRAMPUSBot(final String urlString, final String responseText) {
    Matcher slotMatcher = ChoiceAdventures.URL_SLOT_PATTERN.matcher(urlString);
    if (!slotMatcher.find()) {
      return null;
    }
    String slotString = slotMatcher.group(0);
    Matcher botMatcher = ChoiceAdventures.BOT_PATTERN.matcher(responseText);
    while (botMatcher.find()) {
      if (botMatcher.group(0).contains(slotString)) {
        return botMatcher.group(1);
      }
    }
    return null;
  }

  private static final Pattern BENCH_WARRANT_PATTERN =
      Pattern.compile("creep <font color=blueviolet><b>(\\d+)</b></font> of them");
  private static final Pattern CINDERELLA_SCORE_PATTERN =
      Pattern.compile("score (?:is now|was) <b>(\\d+)</b>");
  private static final Pattern FOG_PATTERN = Pattern.compile("<font.*?><b>(.*?)</b></font>");
  private static final Pattern LOV_EXIT_PATTERN =
      Pattern.compile("a sign above it that says <b>(.*?)</b>");
  private static final Pattern LOV_LOGENTRY_PATTERN = Pattern.compile("you scrawl <b>(.*?)</b>");
  private static final Pattern LYNYRD_PATTERN =
      Pattern.compile(
          "(?:scare|group of|All) <b>(\\d+)</b> (?:of the protesters|protesters|of them)");
  private static final Pattern POOL_SKILL_PATTERN = Pattern.compile("(\\d+) Pool Skill</b>");
  private static final Pattern PINK_WORD_PATTERN =
      Pattern.compile(
          "scrawled in lipstick on a cocktail napkin:  <b><font color=pink>(.*?)</font></b>");
  private static final Pattern STILL_PATTERN =
      Pattern.compile("toss (.*?) cocktail onions into the still");
  private static final Pattern TIME_SPINNER_PATTERN = Pattern.compile("have (\\d+) minute");
  private static final Pattern TIME_SPINNER_MEDALS_PATTERN =
      Pattern.compile("memory of earning <b>(\\d+) medal");
  private static final Pattern VACCINE_PATTERN =
      Pattern.compile("option value=(\\d+).*?class=button type=submit value=\"([^\"]*)");
  private static final Pattern WALFORD_PATTERN =
      Pattern.compile("\\(Walford's bucket filled by (\\d+)%\\)");

  public static void postChoice2(final String urlString, final GenericRequest request) {
    String text = request.responseText;

    switch (ChoiceManager.lastChoice) {
      case 3:
        // The Oracle Will See You Now
        if (text.contains("actually a book")) {
          Preferences.setInteger("lastPlusSignUnlock", KoLCharacter.getAscensions());
        }
        break;
      case 7:
        // How Depressing

        if (ChoiceManager.lastDecision == 1) {
          EquipmentManager.discardEquipment(ItemPool.SPOOKY_GLOVE);
        }
        break;

      case 21:
        // Under the Knife
        if (ChoiceManager.lastDecision == 1 && text.contains("anaesthetizes you")) {
          Preferences.increment("sexChanges", 1);
          Preferences.setBoolean("_sexChanged", true);
          KoLCharacter.setGender(
              text.contains("in more ways than one") ? KoLCharacter.FEMALE : KoLCharacter.MALE);
          ConcoctionDatabase.setRefreshNeeded(false);
        }
        break;

      case 48:
      case 49:
      case 50:
      case 51:
      case 52:
      case 53:
      case 54:
      case 55:
      case 56:
      case 57:
      case 58:
      case 59:
      case 60:
      case 61:
      case 62:
      case 63:
      case 64:
      case 65:
      case 66:
      case 67:
      case 68:
      case 69:
      case 70:
        // Choices in the Violet Fog
        VioletFogManager.mapChoice(ChoiceManager.lastChoice, ChoiceManager.lastDecision, text);
        break;

      case 73:
        // Don't Fence Me In
        if (ChoiceManager.lastDecision == 3) {
          if (text.contains("you pick") || text.contains("you manage")) {
            Preferences.increment("_whiteRiceDrops", 1);
          }
        }
        break;

      case 89:
        if (ChoiceManager.lastDecision == 4) {
          TurnCounter.startCounting(10, "Garden Banished loc=*", "wolfshield.gif");
        }
        break;

      case 105:
        if (ChoiceManager.lastDecision == 3) {
          checkGuyMadeOfBees(request);
        }
        break;

      case 112:
        // Please, Hammer
        if (ChoiceManager.lastDecision == 1 && KoLmafia.isAdventuring()) {
          InventoryManager.retrieveItem(ItemPool.get(ItemPool.HAROLDS_HAMMER, 1));
        }
        break;

      case 125:
        // No visible means of support
        if (ChoiceManager.lastDecision == 3) {
          QuestDatabase.setQuestProgress(Quest.WORSHIP, "step3");
        }
        break;

      case 132:
        // Let's Make a Deal!
        if (ChoiceManager.lastDecision == 2) {
          QuestDatabase.setQuestProgress(Quest.PYRAMID, "step1");
        }
        break;

      case 162:
        // Between a Rock and Some Other Rocks
        if (KoLmafia.isAdventuring()
            && !EquipmentManager.isWearingOutfit(OutfitPool.MINING_OUTFIT)
            && !KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.EARTHEN_FIST))) {
          QuestManager.unlockGoatlet();
        }
        break;

      case 197:
        // Somewhat Higher and Mostly Dry
      case 198:
        // Disgustin' Junction
      case 199:
        // The Former or the Ladder
        if (ChoiceManager.lastDecision == 1) {
          checkDungeonSewers(request);
        }
        break;

      case 200:
        // Enter The Hoboverlord
      case 201:
        // Home, Home in the Range
      case 202:
        // Bumpity Bump Bump
      case 203:
        // Deep Enough to Dive
      case 204:
        // Welcome To You!
      case 205:
        // Van, Damn

        // Stop for Hobopolis bosses
        if (ChoiceManager.lastDecision == 2 && KoLmafia.isAdventuring()) {
          KoLmafia.updateDisplay(
              MafiaState.PENDING, hobopolisBossName(ChoiceManager.lastChoice) + " waits for you.");
        }
        break;

      case 299:
        // Down at the Hatch
        if (ChoiceManager.lastDecision == 2) {
          // The first time you take option 2, you
          // release Big Brother. Subsequent times, you
          // release other creatures.
          Preferences.setBoolean("bigBrotherRescued", true);
          QuestDatabase.setQuestProgress(Quest.SEA_MONKEES, "step2");
          ConcoctionDatabase.setRefreshNeeded(false);
        }
        break;

      case 304:
        // A Vent Horizon

        // "You conjure some delicious batter from the core of
        // the thermal vent. It pops and sizzles as you stick
        // it in your sack."

        if (text.contains("pops and sizzles")) {
          Preferences.increment("tempuraSummons", 1);
        }
        break;

      case 309:
        // Barback

        // "You head down the tunnel into the cave, and manage
        // to find another seaode. Sweet! I mean... salty!"

        if (text.contains("salty!")) {
          Preferences.increment("seaodesFound", 1);
        }
        break;

      case 326:
        // Showdown

        if (ChoiceManager.lastDecision == 2 && KoLmafia.isAdventuring()) {
          KoLmafia.updateDisplay(MafiaState.ABORT, "Mother Slime waits for you.");
        }
        break;

      case 330:
        // A Shark's Chum
        if (ChoiceManager.lastDecision == 1) {
          Preferences.increment("poolSharkCount", 1);
        }
        break;

      case 360:
        WumpusManager.takeChoice(ChoiceManager.lastDecision, text);
        break;

      case 441:
        // The Mad Tea Party

        // I'm sorry, but there's a very strict dress code for
        // this party

        if (ChoiceManager.lastDecision == 1 && !text.contains("very strict dress code")) {
          Preferences.setBoolean("_madTeaParty", true);
        }
        break;

      case 442:
        // A Moment of Reflection
        if (ChoiceManager.lastDecision == 5) {
          // Option 5 is Chess Puzzle
          RabbitHoleManager.parseChessPuzzle(text);
        }

        if (ChoiceManager.lastDecision != 6) {
          // Option 6 does not consume the map. Others do.
          ResultProcessor.processItem(ItemPool.REFLECTION_OF_MAP, -1);
        }
        break;

      case 508:
        // Pants-Gazing
        if (text.contains("You acquire an effect")) {
          Preferences.increment("_gapBuffs", 1);
        }
        break;

      case 517:
        // Mr. Alarm, I presarm
        QuestDatabase.setQuestIfBetter(Quest.PALINDOME, "step3");
        break;

      case 518:
        // Clear and Present Danger

        // Stop for Hobopolis bosses
        if (ChoiceManager.lastDecision == 2 && KoLmafia.isAdventuring()) {
          KoLmafia.updateDisplay(
              MafiaState.PENDING, hobopolisBossName(ChoiceManager.lastChoice) + " waits for you.");
        }
        break;

      case 524:
        // The Adventures of Lars the Cyberian
        if (text.contains("Skullhead's Screw")) {
          // You lose the book if you receive the reward.
          // I don't know if that's always the result of
          // the same choice option
          ResultProcessor.processItem(ItemPool.LARS_THE_CYBERIAN, -1);
        }
        break;

      case 548:
        // Behind Closed Doors
        if (ChoiceManager.lastDecision == 2 && KoLmafia.isAdventuring()) {
          KoLmafia.updateDisplay(MafiaState.ABORT, "The Necbromancer waits for you.");
        }
        break;

      case 549:
        // Dark in the Attic
        if (text.contains("The silver pellets tear through the sorority werewolves")) {
          ResultProcessor.processItem(ItemPool.SILVER_SHOTGUN_SHELL, -1);
          RequestLogger.printLine("You took care of a bunch of werewolves.");
        } else if (text.contains("quietly sneak away")) {
          RequestLogger.printLine("You need a silver shotgun shell to kill werewolves.");
        } else if (text.contains("a loose shutter")) {
          RequestLogger.printLine("All the werewolves have been defeated.");
        } else if (text.contains("crank up the volume on the boombox")) {
          RequestLogger.printLine("You crank up the volume on the boombox.");
        } else if (text.contains("a firm counterclockwise twist")) {
          RequestLogger.printLine("You crank down the volume on the boombox.");
        }
        break;

      case 550:
        // The Unliving Room
        if (text.contains("you pull out the chainsaw blades")) {
          ResultProcessor.processItem(ItemPool.CHAINSAW_CHAIN, -1);
          RequestLogger.printLine("You took out a bunch of zombies.");
        } else if (text.contains("a wet tearing noise")) {
          RequestLogger.printLine("You need a chainsaw chain to kill zombies.");
        } else if (text.contains("a bloody tangle")) {
          RequestLogger.printLine("All the zombies have been defeated.");
        } else if (text.contains("the skeletons collapse into piles of loose bones")) {
          ResultProcessor.processItem(ItemPool.FUNHOUSE_MIRROR, -1);
          RequestLogger.printLine("You made short work of some skeletons.");
        } else if (text.contains("couch in front of the door")) {
          RequestLogger.printLine("You need a funhouse mirror to kill skeletons.");
        } else if (text.contains("just coats")) {
          RequestLogger.printLine("All the skeletons have been defeated.");
        } else if (text.contains("close the windows")) {
          RequestLogger.printLine("You close the windows.");
        } else if (text.contains("open the windows")) {
          RequestLogger.printLine("You open the windows.");
        }
        break;

      case 551:
        // Debasement
        if (text.contains("the vampire girls shriek")) {
          RequestLogger.printLine("You slew some vampires.");
        } else if (text.contains("gets back in her coffin")) {
          RequestLogger.printLine("You need to equip plastic vampire fangs to kill vampires.");
        } else if (text.contains("they recognize you")) {
          RequestLogger.printLine("You have already killed some vampires.");
        } else if (text.contains("crank up the fog machine")) {
          RequestLogger.printLine("You crank up the fog machine.");
        } else if (text.contains("turn the fog machine way down")) {
          RequestLogger.printLine("You crank down the fog machine.");
        }
        break;

      case 553:
        // Relocked and Reloaded
        if (text.contains("You melt")) {
          int item = 0;
          switch (ChoiceManager.lastDecision) {
            case 1:
              item = ItemPool.MAXWELL_HAMMER;
              break;
            case 2:
              item = ItemPool.TONGUE_BRACELET;
              break;
            case 3:
              item = ItemPool.SILVER_CHEESE_SLICER;
              break;
            case 4:
              item = ItemPool.SILVER_SHRIMP_FORK;
              break;
            case 5:
              item = ItemPool.SILVER_PATE_KNIFE;
              break;
          }
          if (item > 0) {
            ResultProcessor.processItem(item, -1);
          }
        }
        break;

      case 558:
        // Tool Time
        if (text.contains("You acquire an item")) {
          int amount = 3 + ChoiceManager.lastDecision;
          ResultProcessor.processItem(ItemPool.LOLLIPOP_STICK, -amount);
        }
        break;

      case 578:
        // End of the Boris Road
        handleAfterAvatar(ChoiceManager.lastDecision);
        break;

      case 579:
        // Such Great Heights
        if (ChoiceManager.lastDecision == 3
            && Preferences.getInteger("lastTempleAdventures") != KoLCharacter.getAscensions()) {
          Preferences.setInteger("lastTempleAdventures", KoLCharacter.getAscensions());
        }
        break;

      case 581:
        // Such Great Depths
        if (ChoiceManager.lastDecision == 2) {
          Preferences.setBoolean("_templeHiddenPower", true);
        }
        break;

      case 584:
        // Unconfusing Buttons
        if (Preferences.getInteger("lastTempleButtonsUnlock") != KoLCharacter.getAscensions()) {
          Preferences.setInteger("lastTempleButtonsUnlock", KoLCharacter.getAscensions());
        }
        if (InventoryManager.getCount(ItemPool.NOSTRIL_OF_THE_SERPENT) > 0) {
          ResultProcessor.processItem(ItemPool.NOSTRIL_OF_THE_SERPENT, -1);
        }
        if (ChoiceManager.lastDecision == 4) {
          QuestDatabase.setQuestIfBetter(Quest.WORSHIP, "step2");
        }
        break;

      case 595:
        // Fire! I... have made... fire!
        Preferences.setBoolean("_fireStartingKitUsed", true);
        break;

      case 602:
        // Behind the Gash
        // This is a multi-part choice adventure, and we only want to handle the last choice
        if (text.contains("you shout into the blackness")) {
          handleAfterAvatar(ChoiceManager.lastDecision);
        }
        break;

      case 613:
        // Behind the door there is a fog
        if (ChoiceManager.lastDecision == 1) {
          Matcher fogMatcher = FOG_PATTERN.matcher(text);
          if (fogMatcher.find()) {
            String message = "Message: \"" + fogMatcher.group(1) + "\"";
            RequestLogger.printLine(message);
            RequestLogger.updateSessionLog(message);
          }
        }
        break;

      case 633:
        // ChibiBuddy&trade;
        if (ChoiceManager.lastDecision == 1) {
          ResultProcessor.processItem(ItemPool.CHIBIBUDDY_OFF, -1);
          ResultProcessor.processItem(ItemPool.CHIBIBUDDY_ON, 1);
        }
        break;

      case 640:
        // Tailor the Snow Suit
        Preferences.setString(
            "snowsuit", SnowsuitCommand.DECORATION[ChoiceManager.lastDecision - 1][0]);
        break;

      case 641:
        // Stupid Pipes.
        if (ChoiceManager.lastDecision == 2 && text.contains("flickering pixel")) {
          Preferences.setBoolean("flickeringPixel1", true);
        }
        break;

      case 642:
        // You're Freaking Kidding Me
        if (ChoiceManager.lastDecision == 2 && text.contains("flickering pixel")) {
          Preferences.setBoolean("flickeringPixel2", true);
        }
        break;

      case 644:
        // Snakes.
        if (ChoiceManager.lastDecision == 2 && text.contains("flickering pixel")) {
          Preferences.setBoolean("flickeringPixel3", true);
        }
        break;

      case 645:
        // So... Many... Skulls...
        if (ChoiceManager.lastDecision == 2 && text.contains("flickering pixel")) {
          Preferences.setBoolean("flickeringPixel4", true);
        }
        break;

      case 647:
        // A Stupid Dummy. Also, a Straw Man.
        if (ChoiceManager.lastDecision == 2 && text.contains("flickering pixel")) {
          Preferences.setBoolean("flickeringPixel5", true);
        }
        break;

      case 648:
        // Slings and Arrows
        if (ChoiceManager.lastDecision == 2 && text.contains("flickering pixel")) {
          Preferences.setBoolean("flickeringPixel6", true);
        }
        break;

      case 650:
        // This Is Your Life. Your Horrible, Horrible Life.
        if (ChoiceManager.lastDecision == 2 && text.contains("flickering pixel")) {
          Preferences.setBoolean("flickeringPixel7", true);
        }
        break;

      case 651:
        // The Wall of Wailing
        if (ChoiceManager.lastDecision == 2 && text.contains("flickering pixel")) {
          Preferences.setBoolean("flickeringPixel8", true);
        }
        break;

      case 677:
        // Copper Feel
        if (ChoiceManager.lastDecision == 1) {
          ResultProcessor.removeItem(ItemPool.MODEL_AIRSHIP);
        }
        break;

      case 682:
        // Now Leaving Jarlsberg, Population You
        handleAfterAvatar(ChoiceManager.lastDecision);
        break;

      case 696:
        // Stick a Fork In It
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("maraisDarkUnlock", true);
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setBoolean("maraisWildlifeUnlock", true);
        }
        break;

      case 697:
        // Sophie's Choice
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("maraisCorpseUnlock", true);
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setBoolean("maraisWizardUnlock", true);
        }
        break;

      case 698:
        // From Bad to Worst
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("maraisBeaverUnlock", true);
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setBoolean("maraisVillageUnlock", true);
        }
        break;

      case 704:
        // Playing the Catalog Card
        DreadScrollManager.handleLibrary(text);
        break;

      case 774:
        // Opening up the Folder Holder

        // Choice 1 is adding a folder.
        if (ChoiceManager.lastDecision == 1
            && text.contains("You carefully place your new folder in the holder")) {
          // Figure out which one it was from the URL
          String id = request.getFormField("folder");
          AdventureResult folder = EquipmentRequest.idToFolder(id);
          ResultProcessor.removeItem(folder.getItemId());
        }

        // Choice 2 is removing a folder. Since the folder is
        // destroyed, it does not go back to inventory.

        // Set all folder slots from the response text
        EquipmentRequest.parseFolders(text);
        break;

      case 786:
        if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.autoCreate(ItemPool.MCCLUSKY_FILE);
        }
        break;

      case 798:
        // Hippy Talkin'
        if (text.contains("Point me at the landfill")) {
          QuestDatabase.setQuestProgress(Quest.HIPPY, QuestDatabase.STARTED);
        }
        break;

      case 810:
        if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.processItem(ItemPool.WARBEAR_WHOSIT, -100);
        } else if (ChoiceManager.lastDecision == 4 && text.contains("You upgrade the robot!")) {
          String bot = findKRAMPUSBot(request.getURLString(), text);
          int cost =
              (bot == null) ? 0 : bot.contains("Level 2") ? 250 : bot.contains("Level 3") ? 500 : 0;
          if (cost != 0) {
            ResultProcessor.processItem(ItemPool.WARBEAR_WHOSIT, -cost);
          }
        }
        break;

      case 822:
      case 823:
      case 824:
      case 825:
      case 826:
      case 827:
        {
          // The Prince's Ball
          if (parseCinderellaTime() == false) {
            Preferences.decrement("cinderellaMinutesToMidnight");
          }
          Matcher matcher = CINDERELLA_SCORE_PATTERN.matcher(text);
          if (matcher.find()) {
            int score = StringUtilities.parseInt(matcher.group(1));
            if (score != -1) {
              Preferences.setInteger("cinderellaScore", score);
            }
          }
          if (text.contains("Your final score was")) {
            Preferences.setInteger("cinderellaMinutesToMidnight", 0);
            Preferences.setString("grimstoneMaskPath", "");
          }
          break;
        }

      case 829:
        // We all wear masks
        if (ChoiceManager.lastDecision != 6) {
          ResultProcessor.processItem(ItemPool.GRIMSTONE_MASK, -1);
          Preferences.setInteger("cinderellaMinutesToMidnight", 0);
        }
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setInteger("cinderellaMinutesToMidnight", 30);
          Preferences.setInteger("cinderellaScore", 0);
          Preferences.setString("grimstoneMaskPath", "stepmother");
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setString("grimstoneMaskPath", "wolf");
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.setString("grimstoneMaskPath", "witch");
        } else if (ChoiceManager.lastDecision == 4) {
          Preferences.setString("grimstoneMaskPath", "gnome");
        } else if (ChoiceManager.lastDecision == 5) {
          Preferences.setString("grimstoneMaskPath", "hare");
        }
        RumpleManager.reset(ChoiceManager.lastDecision);
        break;

      case 844:
        // The Portal to Horrible Parents
        if (ChoiceManager.lastDecision == 1) {
          RumpleManager.spyOnParents(text);
        }
        break;

      case 846:
        // Bartering for the Future of Innocent Children
        RumpleManager.pickParent(ChoiceManager.lastDecision);
        break;

      case 847:
        // Pick Your Poison
        RumpleManager.pickSin(ChoiceManager.lastDecision);
        break;

      case 848:
        // Where the Magic Happens
        if (ChoiceManager.lastDecision != 4) {
          RumpleManager.recordTrade(text);
        }
        break;

      case 854:
        // Shen Copperhead, World's Biggest Jerk
        if (InventoryManager.hasItem(ItemPool.COPPERHEAD_CHARM)
            && InventoryManager.hasItem(ItemPool.COPPERHEAD_CHARM_RAMPANT)) {
          ResultProcessor.autoCreate(ItemPool.TALISMAN);
        }
        break;

      case 855:
        {
          // Behind the 'Stache
          String hazard = Preferences.getString("copperheadClubHazard");
          switch (ChoiceManager.lastDecision) {
            case 1:
              hazard = "gong";
              break;
            case 2:
              hazard = "ice";
              break;
            case 3:
              hazard = "lantern";
              break;
          }
          Preferences.setString("copperheadClubHazard", hazard);
          break;
        }

      case 856:
        {
          // This Looks Like a Good Bush for an Ambush
          Matcher lynyrdMatcher = LYNYRD_PATTERN.matcher(text);
          if (lynyrdMatcher.find()) {
            int protestersScared = StringUtilities.parseInt(lynyrdMatcher.group(1));
            Preferences.increment("zeppelinProtestors", protestersScared);
            RequestLogger.printLine("Scared off " + protestersScared + " protesters");
          }
          break;
        }

      case 857:
        {
          // Bench Warrant
          Matcher benchWarrantMatcher = BENCH_WARRANT_PATTERN.matcher(text);
          if (benchWarrantMatcher.find()) {
            int protestersCreeped = StringUtilities.parseInt(benchWarrantMatcher.group(1));
            Preferences.increment("zeppelinProtestors", protestersCreeped);
            RequestLogger.printLine("Creeped out " + protestersCreeped + " protesters");
          }
          break;
        }

      case 858:
        // Fire Up Above
        if (text.contains("three nearest protesters")) {
          Preferences.increment("zeppelinProtestors", 3);
          RequestLogger.printLine("Soaked 3 protesters");
        } else if (text.contains("Flamin' Whatshisname")) {
          Preferences.increment("zeppelinProtestors", 10);
          ResultProcessor.processItem(ItemPool.FLAMIN_WHATSHISNAME, -1);
          RequestLogger.printLine("Set fire to 10 protesters");
        }
        break;

      case 860:
        // Another Tired Retread
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setString("peteMotorbikeTires", "Racing Slicks");
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setString("peteMotorbikeTires", "Spiky Tires");
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.setString("peteMotorbikeTires", "Snow Tires");
        }
        break;
      case 861:
        // Station of the Gas
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setString("peteMotorbikeGasTank", "Large Capacity Tank");
          KoLCharacter.setDesertBeachAvailable();
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setString("peteMotorbikeGasTank", "Extra-Buoyant Tank");
          Preferences.setInteger("lastIslandUnlock", KoLCharacter.getAscensions());
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.setString("peteMotorbikeGasTank", "Nitro-Burnin' Funny Tank");
        }
        break;
      case 862:
        // Me and Cinderella Put It All Together
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setString("peteMotorbikeHeadlight", "Ultrabright Yellow Bulb");
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setString("peteMotorbikeHeadlight", "Party Bulb");
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.setString("peteMotorbikeHeadlight", "Blacklight Bulb");
        }
        break;
      case 863:
        // Endowing the Cowling
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setString("peteMotorbikeCowling", "Ghost Vacuum");
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setString("peteMotorbikeCowling", "Rocket Launcher");
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.setString("peteMotorbikeCowling", "Sweepy Red Light");
        }
        break;
      case 864:
        // Diving into the Mufflers
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setString("peteMotorbikeMuffler", "Extra-Loud Muffler");
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setString("peteMotorbikeMuffler", "Extra-Quiet Muffler");
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.setString("peteMotorbikeMuffler", "Extra-Smelly Muffler");
        }
        break;
      case 865:
        // Ayy, Sit on It
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setString("peteMotorbikeSeat", "Massage Seat");
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setString("peteMotorbikeSeat", "Deep Seat Cushions");
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.setString("peteMotorbikeSeat", "Sissy Bar");
        }
        break;

      case 869:
        // End of Pete Road
        handleAfterAvatar(ChoiceManager.lastDecision);
        break;

      case 875:
        {
          // Welcome To Our ool Table
          Matcher poolSkillMatcher = POOL_SKILL_PATTERN.matcher(text);
          if (poolSkillMatcher.find()) {
            Preferences.increment("poolSkill", StringUtilities.parseInt(poolSkillMatcher.group(1)));
          }
          break;
        }

      case 918:
        // Yachtzee!
        if (text.contains("Ultimate Mind Destroyer")) {
          Calendar date = HolidayDatabase.getCalendar();
          SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
          String today = sdf.format(date.getTime());
          Preferences.setString("umdLastObtained", today);
        }
        break;

      case 921:
        // We'll All Be Flat
        QuestDatabase.setQuestProgress(Quest.MANOR, "step1");
        break;

      case 928:
        // The Blackberry Cobbler
        if (ChoiceManager.lastDecision != 6) {
          ResultProcessor.processItem(ItemPool.BLACKBERRY, -3);
        }
        break;

      case 929:
        // Control Freak
        if (ChoiceManager.lastDecision == 1 && text.contains("wooden wheel disintegrating")) {
          ResultProcessor.processItem(ItemPool.CRUMBLING_WHEEL, -1);
          PyramidRequest.advancePyramidPosition();
        } else if (ChoiceManager.lastDecision == 2
            && text.contains("snap the ratchet onto the peg")) {
          ResultProcessor.processItem(ItemPool.TOMB_RATCHET, -1);
          PyramidRequest.advancePyramidPosition();
        }
        break;

      case 940:
        // Let Your Fists Do The Walking
        if (ChoiceManager.lastDecision == 6) {
          ResultProcessor.processItem(ItemPool.WHITE_PAGE, 1);
        }
        break;

      case 974:
        {
          // Around The World
          Matcher pinkWordMatcher = PINK_WORD_PATTERN.matcher(text);
          if (pinkWordMatcher.find()) {
            String pinkWord = pinkWordMatcher.group(1);
            String message =
                "Bohemian Party Pink Word found: "
                    + pinkWord
                    + " in clan "
                    + ClanManager.getClanName(false)
                    + ".";
            RequestLogger.printLine("<font color=\"blue\">" + message + "</font>");
            RequestLogger.updateSessionLog(message);
          }
          break;
        }

      case 975:
        {
          // Crazy Still After All These Years
          Matcher stillMatcher = STILL_PATTERN.matcher(text);
          if (stillMatcher.find()) {
            ResultProcessor.processItem(
                ItemPool.COCKTAIL_ONION, -StringUtilities.parseInt(stillMatcher.group(1)));
          }
          break;
        }

      case 977:
        // [Chariot Betting]
        if (ChoiceManager.lastDecision < 12) {
          ResultProcessor.processItem(ItemPool.CHRONER, -10);
        }
        break;

      case 981:
        // [back room]
        if (ChoiceManager.lastDecision < 12) {
          ResultProcessor.processItem(ItemPool.MERCURY_BLESSING, -1);
        }
        break;

      case 982:
      case 983:
        // Playing Dice With Romans & The 99-Centurion Store
        if (ChoiceManager.lastDecision < 6) {
          ResultProcessor.processItem(ItemPool.CHRONER, -1);
        }
        break;

      case 1042: // Pick a Perk
        KoLmafia.resetAfterLimitmode();
        break;

      case 1054:
        // Returning the MacGuffin
        QuestDatabase.setQuestProgress(Quest.WAREHOUSE, QuestDatabase.FINISHED);
        if (ChoiceManager.lastDecision == 1) {
          ResultProcessor.processItem(ItemPool.ED_HOLY_MACGUFFIN, -1);
        }
        break;

      case 1055:
        // Returning the MacGuffin
        KoLCharacter.liberateKing();
        handleAfterAvatar(ChoiceManager.lastDecision);
        break;

      case 1062:
        // Lots of Options
        if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processItem(ItemPool.BOOZE_MAP, -1);
        }
        break;

      case 1073:
        // This Ride Is Like... A Rollercoaster Baby Baby
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("dinseyRollercoasterNext", false);
        }
        if (text.contains("lubricating every inch of the tracks")) {
          QuestDatabase.setQuestProgress(Quest.SUPER_LUBER, "step2");
        }
        break;

      case 1089: // Community Service
        if (text.contains("You acquire")) {
          String quest = null;
          switch (ChoiceManager.lastDecision) {
            case 1:
              quest = "Donate Blood";
              break;
            case 2:
              quest = "Feed The Children";
              break;
            case 3:
              quest = "Build Playground Mazes";
              break;
            case 4:
              quest = "Feed Conspirators";
              break;
            case 5:
              quest = "Breed More Collies";
              break;
            case 6:
              quest = "Reduce Gazelle Population";
              break;
            case 7:
              quest = "Make Sausage";
              break;
            case 8:
              quest = "Be a Living Statue";
              break;
            case 9:
              quest = "Make Margaritas";
              break;
            case 10:
              quest = "Clean Steam Tunnels";
              break;
            case 11:
              quest = "Coil Wire";
              break;
          }
          if (quest != null) {
            String current = Preferences.getString("csServicesPerformed");
            if (current.equals("")) {
              Preferences.setString("csServicesPerformed", quest);
            } else {
              Preferences.setString("csServicesPerformed", current + "," + quest);
            }
          }
        }
        break;

      case 1090:
        // The Towering Inferno Discotheque
        if (ChoiceManager.lastDecision > 1) {
          Preferences.setBoolean("_infernoDiscoVisited", true);
        }
        break;

      case 1091:
        // Choice 1091 is The Floor Is Yours
        if (text.contains("You acquire")) {
          switch (ChoiceManager.lastDecision) {
            case 1:
              ResultProcessor.processResult(ItemPool.get(ItemPool.GOLD_1970, -1));
              break;
            case 2:
              ResultProcessor.processResult(ItemPool.get(ItemPool.NEW_AGE_HEALING_CRYSTAL, -1));
              break;
            case 3:
              ResultProcessor.processResult(ItemPool.get(ItemPool.EMPTY_LAVA_BOTTLE, -1));
              break;
            case 5:
              ResultProcessor.processResult(ItemPool.get(ItemPool.GLOWING_NEW_AGE_CRYSTAL, -1));
              break;
            case 6:
              ResultProcessor.processResult(ItemPool.get(ItemPool.CRYSTALLINE_LIGHT_BULB, -1));
              ResultProcessor.processResult(ItemPool.get(ItemPool.INSULATED_GOLD_WIRE, -1));
              ResultProcessor.processResult(ItemPool.get(ItemPool.HEAT_RESISTANT_SHEET_METAL, -1));
              break;
          }
        }
        break;

      case 1092:
        // Choice 1092 is Dyer Maker
        if (text.contains("You acquire")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.VISCOUS_LAVA_GLOBS, -1));
        }
        break;

      case 1093:
        // The WLF Bunker

        // A woman in an orange denim jumpsuit emerges from a
        // hidden trap door, smiles, and hands you a coin in
        // exchange for your efforts.

        if (text.contains("hands you a coin")) {
          String index = String.valueOf(ChoiceManager.lastDecision);
          int itemId = Preferences.getInteger("_volcanoItem" + index);
          int count = Preferences.getInteger("_volcanoItemCount" + index);
          if (itemId > 0 && count > 0) {
            ResultProcessor.processResult(ItemPool.get(itemId, -count));
          }
          Preferences.setBoolean("_volcanoItemRedeemed", true);
          Preferences.setInteger("_volcanoItem1", 0);
          Preferences.setInteger("_volcanoItem2", 0);
          Preferences.setInteger("_volcanoItem3", 0);
          Preferences.setInteger("_volcanoItemCount1", 0);
          Preferences.setInteger("_volcanoItemCount2", 0);
          Preferences.setInteger("_volcanoItemCount3", 0);
        }

        break;

      case 1100:
        // Pray to the Barrel God
        if (ChoiceManager.lastDecision <= 4) {
          Preferences.setBoolean("_barrelPrayer", true);
          ConcoctionDatabase.refreshConcoctions();
        }
        break;

      case 1101:
        {
          // It's a Barrel Smashing Party!
          if (ChoiceManager.lastDecision == 2) {
            // We're smashing 100 barrels
            // The results don't say which barrels are being smashed, but it seems to happen in item
            // order
            int count = 100;
            int itemId = ItemPool.LITTLE_FIRKIN;
            while (count > 0 && itemId <= ItemPool.BARNACLED_BARREL) {
              int smashNumber = Math.min(count, InventoryManager.getCount(itemId));
              if (smashNumber > 0) {
                ResultProcessor.processResult(ItemPool.get(itemId, -smashNumber));
                count -= smashNumber;
              }
              itemId++;
            }
            break;
          }
          int itemId = extractIidFromURL(request.getURLString());
          String name = ItemDatabase.getItemName(itemId);
          if (name != null) {
            ResultProcessor.removeItem(itemId);
          }
          break;
        }

      case 1103:
        // Doing the Maths
        if (!text.contains("Try again")) {
          Preferences.increment("_universeCalculated");
        }
        break;

      case 1104:
        // Tree Tea
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("_pottedTeaTreeUsed", true);
        }
        break;

      case 1105:
        // Specifici Tea
        if (request.getURLString().contains("itemid")) {
          Preferences.setBoolean("_pottedTeaTreeUsed", true);
        }
        break;

      case 1110:
        // Spoopy
        if (ChoiceManager.lastDecision == 5) {
          if (text.contains("You board up the doghouse")) {
            Preferences.setBoolean("doghouseBoarded", true);
          } else if (text.contains("You unboard-up the doghouse")) {
            Preferences.setBoolean("doghouseBoarded", false);
          }
        }
        break;

      case 1114:
        // Walford Rusley, Bucket Collector
        if (ChoiceManager.lastDecision == 1) {
          QuestDatabase.setQuestProgress(Quest.BUCKET, QuestDatabase.UNSTARTED);
          Preferences.setInteger("walfordBucketProgress", 0);
          Preferences.setString("walfordBucketItem", "");
        } else if (ChoiceManager.lastDecision < 5) {
          QuestDatabase.setQuestProgress(Quest.BUCKET, QuestDatabase.STARTED);
          Preferences.setInteger("walfordBucketProgress", 0);
          Preferences.setBoolean("_walfordQuestStartedToday", true);
          if (text.contains("Bucket of balls")) {
            Preferences.setString("walfordBucketItem", "balls");
          } else if (text.contains("bucket with blood")) {
            Preferences.setString("walfordBucketItem", "blood");
          } else if (text.contains("Bolts, mainly")) {
            Preferences.setString("walfordBucketItem", "bolts");
          } else if (text.contains("bucket of chicken")) {
            Preferences.setString("walfordBucketItem", "chicken");
          } else if (text.contains("Here y'go -- chum")) {
            Preferences.setString("walfordBucketItem", "chum");
          } else if (text.contains("fill that with ice")) {
            Preferences.setString("walfordBucketItem", "ice");
          } else if (text.contains("fill it up with milk")) {
            Preferences.setString("walfordBucketItem", "milk");
          } else if (text.contains("bucket of moonbeams")) {
            Preferences.setString("walfordBucketItem", "moonbeams");
          } else if (text.contains("bucket with rain")) {
            Preferences.setString("walfordBucketItem", "rain");
          }
        }
        break;

      case 1115:
        // VYKEA!
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("_VYKEACafeteriaRaided", true);
        } else if (ChoiceManager.lastDecision == 3) {
          Matcher WalfordMatcher = WALFORD_PATTERN.matcher(text);
          if (WalfordMatcher.find()) {
            Preferences.increment(
                "walfordBucketProgress", StringUtilities.parseInt(WalfordMatcher.group(1)));
            if (Preferences.getInteger("walfordBucketProgress") >= 100) {
              QuestDatabase.setQuestProgress(Quest.BUCKET, "step2");
            }
          }
        } else if (ChoiceManager.lastDecision == 4) {
          Preferences.setBoolean("_VYKEALoungeRaided", true);
        }
        break;

      case 1116:
        // All They Got Inside is Vacancy (and Ice)
        if (ChoiceManager.lastDecision == 3) {
          Matcher WalfordMatcher = WALFORD_PATTERN.matcher(text);
          if (WalfordMatcher.find()) {
            Preferences.increment(
                "walfordBucketProgress", StringUtilities.parseInt(WalfordMatcher.group(1)));
            if (Preferences.getInteger("walfordBucketProgress") >= 100) {
              QuestDatabase.setQuestProgress(Quest.BUCKET, "step2");
            }
          }
        } else if (ChoiceManager.lastDecision == 5) {
          Preferences.setBoolean("_iceHotelRoomsRaided", true);
        }
        break;

      case 1119:
        // Shining Mauve Backwards In Time
        Preferences.setInteger("encountersUntilDMTChoice", 49);
        if (ChoiceManager.lastDecision == 4
            && Preferences.getInteger("lastDMTDuplication") != KoLCharacter.getAscensions()) {
          Preferences.setInteger("lastDMTDuplication", KoLCharacter.getAscensions());
        }
        break;

      case 1134:
        // Batfellow Ends
        // (choosing to exit)
        if (ChoiceManager.lastDecision == 1) {
          KoLCharacter.setLimitmode(null);
        }
        break;

      case 1168:
        // Batfellow Ends
        // (from running out of time)
        KoLCharacter.setLimitmode(null);
        break;

      case 1171: // LT&T Office
        if (ChoiceManager.lastDecision < 4) {
          QuestDatabase.setQuestProgress(Quest.TELEGRAM, QuestDatabase.STARTED);
        }
        break;

      case 1172: // The Investigation Begins
        QuestDatabase.setQuestProgress(Quest.TELEGRAM, "step1");
        Preferences.setInteger("lttQuestStageCount", 0);
        break;

      case 1173: // The Investigation Continues
        QuestDatabase.setQuestProgress(Quest.TELEGRAM, "step2");
        Preferences.setInteger("lttQuestStageCount", 0);
        break;

      case 1174: // The Investigation Continues
        QuestDatabase.setQuestProgress(Quest.TELEGRAM, "step3");
        Preferences.setInteger("lttQuestStageCount", 0);
        break;

      case 1175: // The Investigation Thrillingly Concludes!
        QuestDatabase.setQuestProgress(Quest.TELEGRAM, "step4");
        Preferences.setInteger("lttQuestStageCount", 0);
        break;

      case 1188: // The Call is Coming from Outside the Simulation
        if (ChoiceManager.lastDecision == 1) {
          // Skill learned
          Preferences.decrement("sourceEnlightenment", 1, 0);
        }
        break;

      case 1191:
        // Source Terminal
        request.setHasResult(true);
        break;

      case 1198:
        // Play a Time Prank
        if (ChoiceManager.lastDecision == 1 && text.contains("paradoxical time copy")) {
          Preferences.increment("_timeSpinnerMinutesUsed");
        }
        break;

      case 1199:
        {
          // The Far Future
          if (text.contains("item appears in the replicator")
              || text.contains("convoluted nature of time-travel")) {
            Preferences.setBoolean("_timeSpinnerReplicatorUsed", true);
            break;
          }
          Matcher medalMatcher = TIME_SPINNER_MEDALS_PATTERN.matcher(text);
          if (medalMatcher.find()) {
            Preferences.setInteger(
                "timeSpinnerMedals", StringUtilities.parseInt(medalMatcher.group(1)));
          }
          break;
        }

      case 1202:
        // Noon in the Civic Center

        // You knock the column over and collect the sprinkles from its shattered remains.
        // A booming voice from behind you startles you. "YOU ARE IN CONTEMPT OF COURT!"
        if (text.contains("CONTEMPT OF COURT!")) {
          Preferences.setBoolean("_gingerbreadColumnDestroyed", true);
          break;
        }

        // You bribe the clerk and he lets you into the office with a sneer. And a key. The key was
        // probably the important part.
        if (text.contains("bribe the clerk")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -1000));
          break;
        }

        // He squints at you and pushes a briefcase across the table.
        if (text.contains("briefcase full of sprinkles")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GINGERBREAD_BLACKMAIL_PHOTOS, -1));
          Preferences.setBoolean("gingerBlackmailAccomplished", true);
          break;
        }
        break;

      case 1203:
        // Midnight in Civic Center
        // You step into the library and spend a few hours studying law. It's surprisingly
        // difficult! You gain a new respect for lawyers.
        // Haha no you don't.
        if (text.contains("few hours studying law")) {
          Preferences.increment("gingerLawChoice");
          break;
        }

        // You pay the counterfeiter to make you a fake version of Gingerbread City to pawn off on
        // some rube as the real thing
        if (text.contains("fake version of Gingerbread City")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -300));
          break;
        }

        // You quickly melt the lock on the cell and the criminal inside thanks you as he runs off
        // into the night.
        // "Hey," you shout after him, "you forgot your..." but he's already gone.
        // Oh well. He almost certainly stole this thing, anyway.
        if (text.contains("melt the lock on the cell")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.CREME_BRULEE_TORCH, -1));
          break;
        }

        // You insert your sprinkles and buy your cigarettes.
        if (text.contains("buy your cigarettes")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -5));
          break;
        }

        // You feed the treat to the puppy and he immediately becomes a loyal friend to you. Dogs
        // are so easy!
        if (text.contains("Dogs are so easy")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GINGERBREAD_DOG_TREAT, -1));
          break;
        }
        break;

      case 1204:
        // Noon at the Train Station
        // You pull the lever and hear a rumbling from underneath you as the sewer gets much larger.
        // Now there's more room for the alligators!
        if (text.contains("more room for the alligators")) {
          Preferences.setBoolean("_gingerBiggerAlligators", true);
          break;
        }

        // You can't make heads or tails of it
        if (text.contains("can't make heads or tails of it")) {
          Preferences.increment("gingerTrainScheduleStudies");
        }
        // You're starting to get a feel for it, but it's still really confusing
        else if (text.contains("starting to get a feel for it")) {
          Preferences.increment("gingerTrainScheduleStudies");
          if (Preferences.getInteger("gingerTrainScheduleStudies") < 4) {
            Preferences.setInteger("gingerTrainScheduleStudies", 4);
          }
        }
        // You're starting to get a handle on how it all works.
        else if (text.contains("starting to get a handle")) {
          Preferences.increment("gingerTrainScheduleStudies");
          if (Preferences.getInteger("gingerTrainScheduleStudies") < 7) {
            Preferences.setInteger("gingerTrainScheduleStudies", 7);
          }
        }
        // You think you've got a pretty good understanding of it at this point.
        else if (text.contains("pretty good understanding")) {
          Preferences.increment("gingerTrainScheduleStudies");
          if (Preferences.getInteger("gingerTrainScheduleStudies") < 10) {
            Preferences.setInteger("gingerTrainScheduleStudies", 10);
          }
        }
        // What next?
        break;

      case 1205:
        // Midnight at the Train Station
        if (text.contains("provide a great workout")) {
          Preferences.increment("gingerMuscleChoice");
        }

        if (text.contains("new line to the subway system")) {
          Preferences.setBoolean("gingerSubwayLineUnlocked", true);
        }

        if (text.contains("what looks like a sweet roll")) {
          Preferences.increment("gingerDigCount");
        } else if (text.contains("piece of rock candy")) {
          Preferences.increment("gingerDigCount");
          if (Preferences.getInteger("gingerDigCount") < 4) {
            Preferences.setInteger("gingerDigCount", 4);
          }
        } else if (text.contains("sugar raygun")) {
          Preferences.setInteger("gingerDigCount", 7);
          ResultProcessor.processResult(ItemPool.get(ItemPool.TEETHPICK, -1));
        }

        break;

      case 1206:
        // Noon in the Industrial Zone
        // You buy the tool.
        if (!text.contains("buy the tool")) {
          break;
        }

        switch (ChoiceManager.lastDecision) {
          case 1:
            // creme brulee torch
            ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -25));
            break;
          case 2:
            // candy crowbar
            ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -50));
            break;
          case 3:
            // candy screwdriver
            ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -100));
            break;
          case 4:
            // teethpick
            ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -1000));
            break;
        }
        break;

      case 1207:
        // Midnight in the Industrial Zone
        // You can't afford a tattoo.
        if (ChoiceManager.lastDecision == 3 && !text.contains("can't afford a tattoo")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -100000));
        }
        break;

      case 1208:
        // Upscale Noon
        if (text.contains("drop off the negatives")) {
          Preferences.setBoolean("gingerNegativesDropped", true);
          ResultProcessor.processResult(ItemPool.get(ItemPool.FRUIT_LEATHER_NEGATIVE, -1));
          break;
        }

        if (!text.contains("You acquire an item")) {
          break;
        }

        switch (ChoiceManager.lastDecision) {
          case 1:
            // gingerbread dog treat
            ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -200));
            break;
          case 2:
            // pumpkin spice candle
            ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -150));
            break;
          case 3:
            // gingerbread spice latte
            ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -50));
            break;
          case 4:
            // gingerbread trousers
            ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -500));
            break;
          case 5:
            // gingerbread waistcoat
            ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -500));
            break;
          case 6:
            // gingerbread tophat
            ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -500));
            break;
          case 8:
            // gingerbread blackmail photos
            Preferences.setBoolean("gingerNegativesDropped", false);
            break;
        }
        break;

      case 1210:
        // Civic Planning Office
        // You move the policy to the front of the drawer, and by the time you leave the office
        // they've already enacted it!
        if (!text.contains("they've already enacted it")) {
          break;
        }

        switch (ChoiceManager.lastDecision) {
          case 1:
            Preferences.setBoolean("gingerRetailUnlocked", true);
            break;
          case 2:
            Preferences.setBoolean("gingerSewersUnlocked", true);
            break;
          case 3:
            Preferences.setBoolean("gingerExtraAdventures", true);
            break;
          case 4:
            Preferences.setBoolean("gingerAdvanceClockUnlocked", true);
            break;
        }
        break;

      case 1212:
        // Seedy Seedy Seedy
        if (!text.contains("You acquire an item")) {
          break;
        }

        if (ChoiceManager.lastDecision == 1) {
          // gingerbread pistol
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -300));
        } else if (ChoiceManager.lastDecision == 3) {
          // gingerbread beer
          ResultProcessor.processResult(ItemPool.get(ItemPool.GINGERBREAD_MUG, -1));
        }
        break;

      case 1214:
        // The Gingerbread Gallery
        if (!text.contains("You acquire an item")) {
          break;
        }

        if (ChoiceManager.lastDecision == 1) {
          // high-end ginger wine
        } else if (ChoiceManager.lastDecision == 2) {
          // chocolate sculpture
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -300));
        } else if (ChoiceManager.lastDecision == 3) {
          // Pop Art: a Guide
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -1000));
        } else if (ChoiceManager.lastDecision == 4) {
          // No Hats as Art
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -1000));
        }
        break;

      case 1218:
        // Wax On
        if (text.contains("You acquire an item")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.WAX_GLOB, -1));
        }
        break;

      case 1219:
        // Approach the Jellyfish
        // You acquire an item: sea jelly
        // You think it'd be best to leave them alone for the rest of the day.
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("_seaJellyHarvested", true);
        }
        break;

      case 1232: // Finally Human
        handleAfterAvatar(ChoiceManager.lastDecision);
        break;

      case 1264: // Meteor Metal Machinations
        if (ChoiceManager.lastDecision >= 1 && ChoiceManager.lastDecision <= 6) {
          // Exchanging for equipment
          ResultProcessor.removeItem(ItemPool.METAL_METEOROID);
        }
        break;

      case 1267: // Rubbed it the Right Way
        String wish = request.getFormField("wish");
        GenieRequest.postChoice(text, wish);
        break;

      case 1272:
        // R&D
        int letterId = extractIidFromURL(urlString);
        String letterName = ItemDatabase.getItemName(letterId);
        if (letterName != null) {
          // Turning in letter
          String message = "handed over " + letterName;
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
          ResultProcessor.removeItem(letterId);
        }
        break;

      case 1273:
        // R&D
        if (text.contains("slide the weird key into the weird lock")) {
          ResultProcessor.removeItem(ItemPool.WAREHOUSE_KEY);
        }
        break;

      case 1320:
        // A Heist!
        if (ChoiceManager.lastDecision == 1) {
          Preferences.increment("_catBurglarHeistsComplete");
        }
        break;

      case 1339:
        {
          // A Little Pump and Grind
          if (ChoiceManager.lastDecision == 1 && text.contains("filling counter increments")) {
            int itemId = extractIidFromURL(request.getURLString());
            int qty = extractQtyFromURL(request.getURLString());
            ResultProcessor.processResult(ItemPool.get(itemId, -qty));
          } else if (ChoiceManager.lastDecision == 2 && text.contains("You acquire an item")) {
            ResultProcessor.removeItem(ItemPool.MAGICAL_SAUSAGE_CASING);
          }
          break;
        }

      case 1344: // Thank You, Come Again
        handleAfterAvatar(ChoiceManager.lastDecision);
        break;

      case 1395: // Take your Pills
        {
          if (!text.contains("day's worth of pills")) {
            // Something failed.  Not enough spleen space left, already have
            // Everything Looks Yellow active, or maybe some other failure condition
            break;
          }
          if (ChoiceManager.lastDecision >= 1 && ChoiceManager.lastDecision <= 8) {
            if (!Preferences.getBoolean("_freePillKeeperUsed")) {
              Preferences.setBoolean("_freePillKeeperUsed", true);
            } else {
              KoLCharacter.setSpleenUse(KoLCharacter.getSpleenUse() + 3);
            }
          }
          if (ChoiceManager.lastDecision == 7) {
            // *** No longer forces a semirare
          }
        }
        break;

      case 1409:
        // Your Quest is Over
        handleAfterAvatar(ChoiceManager.lastDecision);
        break;

      case 1449:
        // If you change the mode with the item equipped, you need to un-equip and re-equip it to
        // get the modifiers
        if (ChoiceManager.lastDecision >= 1 && ChoiceManager.lastDecision <= 3) {
          for (int i = EquipmentManager.ACCESSORY1; i <= EquipmentManager.ACCESSORY3; ++i) {
            AdventureResult item = EquipmentManager.getEquipment(i);
            if (item != null && item.getItemId() == ItemPool.BACKUP_CAMERA) {
              RequestThread.postRequest(new EquipmentRequest(EquipmentRequest.UNEQUIP, i));
              RequestThread.postRequest(new EquipmentRequest(item, i));
            }
          }
        }
        break;
      case 1452: // Sprinkler Joe
      case 1453: // Fracker Dan
      case 1454: // Cropduster Dusty
        WildfireCampRequest.refresh();
        break;

      case 1457: // Food Lab
        if (ChoiceManager.lastDecision == 1 && text.contains("You acquire an item")) {
          ResultProcessor.processItem(ItemPool.GOOIFIED_ANIMAL_MATTER, -5);
        }
        break;

      case 1458: // Nog Lab
        if (ChoiceManager.lastDecision == 1 && text.contains("You acquire an item")) {
          ResultProcessor.processItem(ItemPool.GOOIFIED_VEGETABLE_MATTER, -5);
        }
        break;

      case 1459: // Chem Lab
        if (ChoiceManager.lastDecision == 1 && text.contains("You acquire an item")) {
          ResultProcessor.processItem(ItemPool.GOOIFIED_MINERAL_MATTER, -5);
        }
        break;

      case 1460: // Gift Fabrication Lab
        if (text.contains("You acquire an item")) {
          switch (ChoiceManager.lastDecision) {
            case 1:
              ResultProcessor.processItem(ItemPool.GOOIFIED_ANIMAL_MATTER, -30);
              break;
            case 2:
              ResultProcessor.processItem(ItemPool.GOOIFIED_VEGETABLE_MATTER, -30);
              break;
            case 3:
              ResultProcessor.processItem(ItemPool.GOOIFIED_MINERAL_MATTER, -30);
              break;
            case 4:
              ResultProcessor.processItem(ItemPool.GOOIFIED_ANIMAL_MATTER, -15);
              ResultProcessor.processItem(ItemPool.GOOIFIED_VEGETABLE_MATTER, -15);
              break;
            case 5:
              ResultProcessor.processItem(ItemPool.GOOIFIED_VEGETABLE_MATTER, -15);
              ResultProcessor.processItem(ItemPool.GOOIFIED_MINERAL_MATTER, -15);
              break;
            case 6:
              ResultProcessor.processItem(ItemPool.GOOIFIED_MINERAL_MATTER, -15);
              ResultProcessor.processItem(ItemPool.GOOIFIED_ANIMAL_MATTER, -15);
              break;
            case 7:
              ResultProcessor.processItem(ItemPool.GOOIFIED_ANIMAL_MATTER, -10);
              ResultProcessor.processItem(ItemPool.GOOIFIED_VEGETABLE_MATTER, -10);
              ResultProcessor.processItem(ItemPool.GOOIFIED_MINERAL_MATTER, -10);
              break;
            default:
              break;
          }
        }
        break;

      case 1461: // Site Alpha Primary Lab
        switch (ChoiceManager.lastDecision) {
          case 1:
            Preferences.increment("primaryLabGooIntensity", 1);
            break;
          case 2:
            Preferences.decrement("primaryLabGooIntensity", 1);
            break;
          case 3:
            ResultProcessor.processItem(ItemPool.GREY_GOO_RING, -1);
            break;
          case 4:
            // Do nothing
            break;
          case 5:
            // Grab the Cheer Core
            Preferences.setBoolean("primaryLabCheerCoreGrabbed", true);
            break;
        }
        break;

      case 1465:
        // No More Grey You
        handleAfterAvatar(ChoiceManager.lastDecision);
        break;

      case 1466: // Configure Your Unbreakable Umbrella
        UmbrellaRequest.parseUmbrella(urlString, text);
        break;
    }
  }

  private static void handleAfterAvatar(final int decision) {
    String newClass = "Unknown";
    switch (ChoiceManager.lastDecision) {
      case 1:
        newClass = "Seal Clubber";
        break;
      case 2:
        newClass = "Turtle Tamer";
        break;
      case 3:
        newClass = "Pastamancer";
        break;
      case 4:
        newClass = "Sauceror";
        break;
      case 5:
        newClass = "Disco Bandit";
        break;
      case 6:
        newClass = "Accordion Thief";
        break;
    }

    String message = "Now walking on the " + newClass + " road.";

    KoLmafia.updateDisplay(message);
    RequestLogger.updateSessionLog(message);

    KoLmafia.resetAfterAvatar();
  }

  private static final Pattern BOOMBOX_PATTERN = Pattern.compile("you can do <b>(\\d+)</b> more");
  private static final Pattern BOOMBOX_SONG_PATTERN =
      Pattern.compile("&quot;(.*?)&quot;( \\(Keep playing\\)|)");
  private static final Pattern CASE_PATTERN = Pattern.compile("\\((\\d+) more case");
  private static final Pattern CHAMBER_PATTERN = Pattern.compile("Chamber <b>#(\\d+)</b>");
  private static final Pattern CRIMBOT_CHASSIS_PATTERN =
      Pattern.compile("base chassis is the (.*?),");
  private static final Pattern CRIMBOT_ARM_PATTERN =
      Pattern.compile("(?:My arm is the|</i> equipped with a) (.*?),");
  private static final Pattern CRIMBOT_PROPULSION_PATTERN =
      Pattern.compile(
          "(?:provided by a|am mobilized by an|equipped with a pair of|move via) (.*?),");
  private static final Pattern DAYCARE_PATTERN =
      Pattern.compile(
          "(?:Looks like|Probably around) (.*?) pieces in all. (.*?) toddlers are training with (.*?) instructor");
  private static final Pattern DAYCARE_RECRUITS_PATTERN =
      Pattern.compile("<font color=blue><b>[(.*?) Meat]</b></font>");
  private static final Pattern DINSEY_ROLLERCOASTER_PATTERN =
      Pattern.compile("rollercoaster is currently set to (.*?) Mode");
  private static final Pattern DINSEY_PIRATE_PATTERN =
      Pattern.compile("'Updated Pirate' is (lit|dark)");
  private static final Pattern DINSEY_TEACUP_PATTERN =
      Pattern.compile("'Current Teacup Spin Rate' points to (\\d+),000 RPM");
  private static final Pattern DINSEY_SLUICE_PATTERN =
      Pattern.compile("'Sluice Swishers' is currently in the (.*?) position");
  private static final Pattern DOCTOR_BAG_PATTERN =
      Pattern.compile("We've received a report of a patient (.*?), in (.*?)\\.");
  private static final Pattern EARLY_DAYCARE_PATTERN =
      Pattern.compile("mostly empty. (.*?) toddlers are training with (.*?) instructor");
  private static final Pattern ED_RETURN_PATTERN =
      Pattern.compile("Return to the fight! \\((\\d+) Ka\\)");
  private static final Pattern EDPIECE_PATTERN =
      Pattern.compile("<p>The crown is currently adorned with a golden (.*?).<center>");
  private static final Pattern ENLIGHTENMENT_PATTERN =
      Pattern.compile("achieved <b>(\\d+)</b> enlightenment");
  private static final Pattern GUZZLR_TIER_PATTERN =
      Pattern.compile("You have completed ([0-9,]+) (Bronze|Gold|Platinum) Tier deliveries");
  private static final Pattern GUZZLR_QUEST_PATTERN =
      Pattern.compile("<p>You are currently tasked with taking a (.*?) to (.*?) in (.*?)\\.<p>");
  private static final Pattern ICEHOUSE_PATTERN =
      Pattern.compile("perfectly-preserved (.*?), right");
  private static final Pattern MAYO_MINDER_PATTERN =
      Pattern.compile("currently loaded up with packets of (.*?)<p>");
  private static final Pattern MOTORBIKE_TIRES_PATTERN = Pattern.compile("<b>Tires:</b> (.*?)?\\(");
  private static final Pattern MOTORBIKE_GASTANK_PATTERN =
      Pattern.compile("<b>Gas Tank:</b> (.*?)?\\(");
  private static final Pattern MOTORBIKE_HEADLIGHT_PATTERN =
      Pattern.compile("<b>Headlight:</b> (.*?)?\\(");
  private static final Pattern MOTORBIKE_COWLING_PATTERN =
      Pattern.compile("<b>Cowling:</b> (.*?)?\\(");
  private static final Pattern MOTORBIKE_MUFFLER_PATTERN =
      Pattern.compile("<b>Muffler:</b> (.*?)?\\(");
  private static final Pattern MOTORBIKE_SEAT_PATTERN = Pattern.compile("<b>Seat:</b> (.*?)?\\(");
  // Looks like your order for a <muffin type> muffin is not yet ready.
  public static final Pattern MUFFIN_TYPE_PATTERN =
      Pattern.compile("Looks like your order for a (.*? muffin) is not yet ready");
  private static final Pattern MUSHROOM_COSTUME_PATTERN =
      Pattern.compile(
          "<form.*?name=option value=(\\d).*?type=submit value=\"(.*?) Costume\".*?>(\\d+) coins<.*?</form>");
  private static final Pattern MUSHROOM_BADGE_PATTERN =
      Pattern.compile("Current cost: (\\d+) coins.");
  private static final Pattern OMEGA_PATTERN =
      Pattern.compile("<br>Current power level: (\\d+)%</td>");
  private static final Pattern RADIO_STATIC_PATTERN =
      Pattern.compile("<p>(?!(?:<form|</center>))(.+?)(?=<[^i</])");
  private static final Pattern REANIMATOR_ARM_PATTERN = Pattern.compile("(\\d+) arms??<br>");
  private static final Pattern REANIMATOR_LEG_PATTERN = Pattern.compile("(\\d+) legs??<br>");
  private static final Pattern REANIMATOR_SKULL_PATTERN = Pattern.compile("(\\d+) skulls??<br>");
  private static final Pattern REANIMATOR_WEIRDPART_PATTERN =
      Pattern.compile("(\\d+) weird random parts??<br>");
  private static final Pattern REANIMATOR_WING_PATTERN = Pattern.compile("(\\d+) wings??<br>");
  private static final Pattern RED_SNAPPER_PATTERN =
      Pattern.compile("guiding you towards: <b>(.*?)</b>.  You've found <b>(\\d+)</b> of them");
  private static final Pattern RUMPLE_MATERIAL_PATTERN =
      Pattern.compile("alt=\"(.*?)\"></td><td valign=center>(\\d+)<");
  private static final Pattern SAUSAGE_PATTERN =
      Pattern.compile(
          "grinder needs (.*?) of the (.*?) required units of filling to make a sausage.  Your grinder reads \\\"(\\d+)\\\" units.");
  private static final Pattern SHEN_PATTERN =
      Pattern.compile(
          "(?:Bring me|artifact known only as) <b>(.*?)</b>, hidden away for centuries");
  private static final Pattern SNOJO_CONSOLE_PATTERN = Pattern.compile("<b>(.*?) MODE</b>");
  private static final Pattern VOTE_PATTERN =
      Pattern.compile(
          "<label><input .*? value=\\\"(\\d)\\\" class=\\\"locals\\\" /> (.*?)<br /><span .*? color: blue\\\">(.*?)</span><br /></label>");
  private static final Pattern VOTE_SPEECH_PATTERN =
      Pattern.compile(
          "<p><input type='radio' name='g' value='(\\d+)' /> <b>(.*?)</b>(.*?)<br><blockquote>(.*?)</blockquote>");
  private static final Pattern WLF_PATTERN =
      Pattern.compile(
          "<form action=choice.php>.*?<b>(.*?)</b>.*?descitem\\((.*?)\\).*?>(.*?)<.*?name=option value=([\\d]*).*?</form>",
          Pattern.DOTALL);
  private static final Pattern WLF_COUNT_PATTERN = Pattern.compile(".*? \\(([\\d]+)\\)$");

  public static final Map<Quest, String> conspiracyQuestMessages = new HashMap<>();

  static {
    conspiracyQuestMessages.put(
        Quest.CLIPPER,
        "&quot;Attention any available operative. Attention any available operative. A reward has been posted for DNA evidence gathered from Lt. Weirdeaux's subjects inside Site 15. The DNA is to be gathered via keratin extraction. Message repeats.&quot;");
    conspiracyQuestMessages.put(
        Quest.EVE,
        "&quot;Attention Operative 01-A-A. General Sitterson reports a... situation involving experiment E-V-E-6. Military intervention has been requested. Message repeats.&quot;");
    conspiracyQuestMessages.put(
        Quest.FAKE_MEDIUM,
        "&quot;Attention Operative EC-T-1. An outside client has expressed interest in the acquisition of an ESP suppression collar from the laboratory. Operationally significant sums of money are involved. Message repeats.&quot;");
    conspiracyQuestMessages.put(
        Quest.GORE,
        "&quot;Attention any available operative. Attention any available operative. Laboratory overseer General Sitterson reports unacceptable levels of environmental gore. Several elevator shafts are already fully clogged, limiting staff mobility, and several surveillance camera lenses have been rendered opaque, placing the validity of experimental data at risk. Immediate janitorial assistance is requested. Message repeats.&quot;");
    conspiracyQuestMessages.put(
        Quest.JUNGLE_PUN,
        "&quot;Attention any available operative. Attention any available operative. The director of Project Buena Vista has posted a significant bounty for the collection of jungle-related puns. Repeat: Jungle-related puns. Non-jungle puns or jungle non-puns will not be accepted. Non-jungle non-puns, by order of the director, are to be rewarded with summary execution. Message repeats.&quot;");
    conspiracyQuestMessages.put(
        Quest.OUT_OF_ORDER,
        "&quot;Attention Operative QZ-N-0. Colonel Kurzweil at Jungle Interior Camp 4 reports the theft of Project T. L. B. materials. Requests immediate assistance. Is confident that it has not yet been removed from the jungle. Message repeats.&quot;");
    conspiracyQuestMessages.put(
        Quest.SERUM,
        "&quot;Attention Operative 21-B-M. Emergency deployment orders have been executed due to a shortage of experimental serum P-00. Repeat: P Zero Zero. Lt. Weirdeaux is known to have P-00 manufacturing facilities inside the Site 15 mansion. Message repeats.&quot;");
    conspiracyQuestMessages.put(
        Quest.SMOKES,
        "&quot;Attention Operative 00-A-6. Colonel Kurzweil at Jungle Interior Camp 4 reports that they have run out of smokes. Repeat: They have run out of smokes. Requests immediate assistance. Message repeats.&quot;");
  }

  public static void visitChoice(final GenericRequest request) {
    String text = request.responseText;
    switch (ChoiceManager.lastChoice) {
      case 360:
        // Wumpus Hunt
        WumpusManager.visitChoice(text);
        break;

      case 440:
        // Puttin' on the Wax
        HaciendaManager.preRecording(text);
        break;

      case 460:
        // Space Trip
        ArcadeRequest.visitSpaceTripChoice(text);
        break;

      case 471:
        // DemonStar
        ArcadeRequest.visitDemonStarChoice(text);
        break;

      case 485:
        // Fighters Of Fighting
        ArcadeRequest.visitFightersOfFightingChoice(text);
        break;

      case 486:
        // DungeonFist!
        ArcadeRequest.visitDungeonFistChoice(text);
        break;

      case 488:
        // Meteoid
        ArcadeRequest.visitMeteoidChoice(text);
        break;

      case 496:
        // Crate Expectations
      case 509:
        // Of Course!
      case 510:
        // Those Who Came Before You
      case 511:
        // If it's Tiny, is it Still a Mansion?
      case 512:
        // Hot and Cold Running Rats
      case 513:
        // Staring Down the Barrel
      case 514:
        // 1984 Had Nothing on This Cellar
      case 515:
        // A Rat's Home...
      case 1000:
        // Everything in Moderation
      case 1001:
        // Hot and Cold Dripping Rats
        TavernRequest.postTavernVisit(request);
        break;

      case 537:
        // Play Porko!
        SpaaaceRequest.visitPorkoChoice(text);
        break;

      case 540:
        // Big-Time Generator
        SpaaaceRequest.visitGeneratorChoice(text);
        break;

      case 570:
        GameproManager.parseGameproMagazine(text);
        break;

      case 641:
        // Stupid Pipes.
        if (!text.contains("Dive Down")
            && KoLCharacter.getElementalResistanceLevels(Element.HOT) >= 25) {
          Preferences.setBoolean("flickeringPixel1", true);
        }
        break;

      case 642:
        // You're Freaking Kidding Me
        if (!text.contains("Wait a minute...")
            && KoLCharacter.getAdjustedMuscle() >= 500
            && KoLCharacter.getAdjustedMysticality() >= 500
            && KoLCharacter.getAdjustedMoxie() >= 500) {
          Preferences.setBoolean("flickeringPixel2", true);
        }
        break;

      case 644:
        // Snakes.
        if (!text.contains("Tie the snakes in a knot.") && KoLCharacter.getAdjustedMoxie() >= 300) {
          Preferences.setBoolean("flickeringPixel3", true);
        }
        break;

      case 645:
        // So... Many... Skulls...
        if (!text.contains("You fear no evil")
            && KoLCharacter.getElementalResistanceLevels(Element.SPOOKY) >= 25) {
          Preferences.setBoolean("flickeringPixel4", true);
        }
        break;

      case 647:
        // A Stupid Dummy. Also, a Straw Man.

        // *** unspaded
        if (!text.contains("Graaaaaaaaargh!") && KoLCharacter.currentBonusDamage() >= 1000) {
          Preferences.setBoolean("flickeringPixel5", true);
        }
        break;

      case 648:
        // Slings and Arrows
        // *** Yes, there supposed to be two spaces there.
        if (!text.contains("Arrows?  Ha.") && KoLCharacter.getCurrentHP() >= 1000) {
          Preferences.setBoolean("flickeringPixel6", true);
        }
        break;

      case 650:
        // This Is Your Life. Your Horrible, Horrible Life.
        if (!text.contains("Then watch it again with the commentary on!")
            && KoLCharacter.getCurrentMP() >= 1000) {
          Preferences.setBoolean("flickeringPixel7", true);
        }
        break;

      case 651:
        // The Wall of Wailing
        if (!text.contains("Make the tide resist you")
            && KoLCharacter.currentPrismaticDamage() >= 60) {
          Preferences.setBoolean("flickeringPixel8", true);
        }

        break;

      case 658:
        // Debasement
        ResultProcessor.processItem(ItemPool.GOLD_PIECE, -30);
        break;

      case 689:
        // The Final Reward
        Preferences.setInteger("_lastDailyDungeonRoom", 14);
        break;

      case 690:
        // The First Chest Isn't the Deepest
        Preferences.setInteger("_lastDailyDungeonRoom", 4);
        break;

      case 691:
        // Second Chest
        Preferences.setInteger("_lastDailyDungeonRoom", 9);
        break;

      case 692:
      case 693:
        // I Wanna Be a Door and It's Almost Certainly a Trap
        Matcher chamberMatcher = CHAMBER_PATTERN.matcher(text);
        if (chamberMatcher.find()) {
          int round = StringUtilities.parseInt(chamberMatcher.group(1));
          Preferences.setInteger("_lastDailyDungeonRoom", round - 1);
        }
        break;

      case 705:
        // Halls Passing in the Night
        ResultProcessor.processItem(ItemPool.MERKIN_HALLPASS, -1);
        break;

      case 764:
        // The Machine

        // You approach The Machine, and notice that the
        // capacitor you're carrying fits perfectly into an
        // obviously empty socket on the base of it. You plug
        // it in, and The Machine whirs ominously to life.......

        if (text.contains("You plug it in")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SKULL_CAPACITOR, -1));
        }
        break;

      case 774:
        // Opening up the Folder Holder

        String option = request.getFormField("forceoption");
        if (option != null) {
          ChoiceManager.lastDecision = StringUtilities.parseInt(option);
        }
        break;

      case 781:
        // Earthbound and Down
        if (!text.contains("option value=1")) {
          if (Preferences.getInteger("hiddenApartmentProgress") == 0) {
            Preferences.setInteger("hiddenApartmentProgress", 1);
          }
        }
        break;

      case 783:
        // Water You Dune
        if (!text.contains("option value=1")) {
          if (Preferences.getInteger("hiddenHospitalProgress") == 0) {
            Preferences.setInteger("hiddenHospitalProgress", 1);
          }
        }
        break;

      case 785:
        // Air Apparent
        if (!text.contains("option value=1")) {
          if (Preferences.getInteger("hiddenOfficeProgress") == 0) {
            Preferences.setInteger("hiddenOfficeProgress", 1);
          }
        }
        break;

      case 787:
        // Fire When Ready
        if (!text.contains("option value=1")) {
          if (Preferences.getInteger("hiddenBowlingAlleyProgress") == 0) {
            Preferences.setInteger("hiddenBowlingAlleyProgress", 1);
          }
        }
        break;

      case 791:
        // Legend of the Temple in the Hidden City
        Preferences.setInteger("zigguratLianas", 1);
        break;

      case 798:
        // Hippy Talkin'
        if (text.contains("You should totally keep it!")) {
          QuestDatabase.setQuestProgress(Quest.HIPPY, QuestDatabase.FINISHED);
          Preferences.setInteger("lastIslandUnlock", KoLCharacter.getAscensions());
        }
        break;

      case 801:
        {
          // A Reanimated Conversation
          Matcher matcher = REANIMATOR_ARM_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger("reanimatorArms", StringUtilities.parseInt(matcher.group(1)));
          } else {
            Preferences.setInteger("reanimatorArms", 0);
          }
          matcher = REANIMATOR_LEG_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger("reanimatorLegs", StringUtilities.parseInt(matcher.group(1)));
          } else {
            Preferences.setInteger("reanimatorLegs", 0);
          }
          matcher = REANIMATOR_SKULL_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger("reanimatorSkulls", StringUtilities.parseInt(matcher.group(1)));
          } else {
            Preferences.setInteger("reanimatorSkulls", 0);
          }
          matcher = REANIMATOR_WEIRDPART_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger(
                "reanimatorWeirdParts", StringUtilities.parseInt(matcher.group(1)));
          } else {
            Preferences.setInteger("reanimatorWeirdParts", 0);
          }
          matcher = REANIMATOR_WING_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger("reanimatorWings", StringUtilities.parseInt(matcher.group(1)));
          } else {
            Preferences.setInteger("reanimatorWings", 0);
          }
          break;
        }

      case 836:
        {
          // Adventures Who Live in Ice Houses...
          Matcher matcher = ICEHOUSE_PATTERN.matcher(text);
          if (matcher.find()) {
            String icehouseMonster = matcher.group(1);
            BanishManager.banishMonster(icehouseMonster, BanishManager.Banisher.ICE_HOUSE);
          }
          break;
        }

      case 822:
      case 823:
      case 824:
      case 825:
      case 826:
      case 827:
        // The Prince's Ball
        parseCinderellaTime();
        Preferences.setString("grimstoneMaskPath", "stepmother");
        break;

      case 848:
      case 849:
      case 850:
        {
          // Where the Magic Happens & The Practice & World of Bartercraft
          Preferences.setString("grimstoneMaskPath", "gnome");
          // Update remaining materials
          Matcher matcher = RUMPLE_MATERIAL_PATTERN.matcher(text);
          while (matcher.find()) {
            String material = matcher.group(1);
            int number = StringUtilities.parseInt(matcher.group(2));
            if (material.equals("straw")) {
              int straw = InventoryManager.getCount(ItemPool.STRAW);
              if (straw != number) {
                ResultProcessor.processItem(ItemPool.STRAW, number - straw);
              }
            } else if (material.equals("leather")) {
              int leather = InventoryManager.getCount(ItemPool.LEATHER);
              if (leather != number) {
                ResultProcessor.processItem(ItemPool.LEATHER, number - leather);
              }
            } else if (material.equals("clay")) {
              int clay = InventoryManager.getCount(ItemPool.CLAY);
              if (clay != number) {
                ResultProcessor.processItem(ItemPool.CLAY, number - clay);
              }
            } else if (material.equals("filling")) {
              int filling = InventoryManager.getCount(ItemPool.FILLING);
              if (filling != number) {
                ResultProcessor.processItem(ItemPool.FILLING, number - filling);
              }
            } else if (material.equals("parchment")) {
              int parchment = InventoryManager.getCount(ItemPool.PARCHMENT);
              if (parchment != number) {
                ResultProcessor.processItem(ItemPool.PARCHMENT, number - parchment);
              }
            } else if (material.equals("glass")) {
              int glass = InventoryManager.getCount(ItemPool.GLASS);
              if (glass != number) {
                ResultProcessor.processItem(ItemPool.GLASS, number - glass);
              }
            }
          }
          break;
        }

      case 851:
        {
          // Shen Copperhead, Nightclub Owner
          Matcher matcher = SHEN_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("shenQuestItem", matcher.group(1));
          }
          break;
        }

      case 871:
        {
          // inspecting Motorbike
          Matcher matcher = MOTORBIKE_TIRES_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("peteMotorbikeTires", matcher.group(1).trim());
          }
          matcher = MOTORBIKE_GASTANK_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("peteMotorbikeGasTank", matcher.group(1).trim());
            if (Preferences.getString("peteMotorbikeGasTank").equals("Large Capacity Tank")) {
              KoLCharacter.setDesertBeachAvailable();
            } else if (Preferences.getString("peteMotorbikeGasTank").equals("Extra-Buoyant Tank")) {
              Preferences.setInteger("lastIslandUnlock", KoLCharacter.getAscensions());
            }
          }
          matcher = MOTORBIKE_HEADLIGHT_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("peteMotorbikeHeadlight", matcher.group(1).trim());
          }
          matcher = MOTORBIKE_COWLING_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("peteMotorbikeCowling", matcher.group(1).trim());
          }
          matcher = MOTORBIKE_MUFFLER_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("peteMotorbikeMuffler", matcher.group(1).trim());
          }
          matcher = MOTORBIKE_SEAT_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("peteMotorbikeSeat", matcher.group(1).trim());
          }
          break;
        }

      case 890: // Lights Out in the Storage Room
      case 891: // Lights Out in the Laundry Room
      case 892: // Lights Out in the Bathroom
      case 893: // Lights Out in the Kitchen
      case 894: // Lights Out in the Library
      case 895: // Lights Out in the Ballroom
      case 896: // Lights Out in the Gallery
      case 897: // Lights Out in the Bedroom
      case 898: // Lights Out in the Nursery
      case 899: // Lights Out in the Conservatory
      case 900: // Lights Out in the Billiards Room
      case 901: // Lights Out in the Wine Cellar
      case 902: // Lights Out in the Boiler Room
      case 903: // Lights Out in the Laboratory
        // Remove the counter if it exists so a new one can be made
        // as soon as the next adventure is started
        TurnCounter.stopCounting("Spookyraven Lights Out");
        Preferences.setInteger("lastLightsOutTurn", KoLCharacter.getTurnsPlayed());
        break;

      case 984:
        if (text.contains("Awaiting mission")) {
          break;
        }

        Matcher staticMatcher = RADIO_STATIC_PATTERN.matcher(text);

        String snippet = ".*";

        while (staticMatcher.find()) {
          String section = staticMatcher.group(1);

          if (section.contains("You turn the biggest knob on the radio")) {
            continue;
          }

          for (String part : section.split("&lt;.*?&gt;")) {
            if (part.startsWith("&lt;") || part.length() < 3) {
              continue;
            }

            part = part.replaceAll("\\.* *$", "");
            part = part.replace("  ", " ");
            snippet += Pattern.quote(part) + ".*";
          }
        }

        Iterator<Entry<Quest, String>> iterator = conspiracyQuestMessages.entrySet().iterator();

        Quest todaysQuest = null;

        while (iterator.hasNext()) {
          Map.Entry<Quest, String> entry = iterator.next();

          if (Pattern.matches(snippet, entry.getValue())) {
            if (todaysQuest == null) {
              todaysQuest = entry.getKey();
            } else {
              // Multiple matches
              todaysQuest = null;
              break;
            }
          }
        }

        if (todaysQuest != null) {
          Preferences.setString("_questESp", todaysQuest.getPref());
        }

        break;

      case 986:
        // Control Panel
        Preferences.setBoolean(
            "controlPanel1", !text.contains("All-Ranchero FM station: VOLUNTARY"));
        Preferences.setBoolean(
            "controlPanel2", !text.contains("&pi; sleep-hypnosis generators: OFF"));
        Preferences.setBoolean(
            "controlPanel3", !text.contains("Simian Ludovico Wednesdays: CANCELLED"));
        Preferences.setBoolean(
            "controlPanel4", !text.contains("Monkey food safety protocols: OBEYED"));
        Preferences.setBoolean("controlPanel5", !text.contains("Shampoo Dispensers: CHILD-SAFE"));
        Preferences.setBoolean("controlPanel6", !text.contains("Assemble-a-Bear kiosks: CLOSED"));
        Preferences.setBoolean("controlPanel7", !text.contains("Training algorithm: ROUND ROBIN"));
        Preferences.setBoolean(
            "controlPanel8", !text.contains("Re-enactment supply closet: LOCKED"));
        Preferences.setBoolean("controlPanel9", !text.contains("Thermostat setting: 76 DEGREES"));
        Matcher omegaMatcher = OMEGA_PATTERN.matcher(text);
        if (omegaMatcher.find()) {
          Preferences.setInteger(
              "controlPanelOmega", StringUtilities.parseInt(omegaMatcher.group(1)));
        }
        if (text.contains("Omega device activated")) {
          Preferences.setInteger("controlPanelOmega", 0);
          QuestDatabase.setQuestProgress(Quest.EVE, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.FAKE_MEDIUM, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.SERUM, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.SMOKES, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.OUT_OF_ORDER, QuestDatabase.UNSTARTED);
        }
        break;

      case 1002:
        // Legend of the Temple in the Hidden City
        Preferences.setInteger("zigguratLianas", 1);
        break;

      case 1003:
        // Test Your Might And Also Test Other Things
        SorceressLairManager.parseContestBooth(0, text);
        break;

      case 1005: // 'Allo
      case 1006: // One Small Step For Adventurer
      case 1007: // Twisty Little Passages, All Hedge
      case 1008: // Pooling Your Resources
      case 1009: // Good Ol' 44% Duck
      case 1010: // Another Day, Another Fork
      case 1011: // Of Mouseholes and Manholes
      case 1012: // The Last Temptation
      case 1013: // Mazel Tov!
        SorceressLairManager.visitChoice(ChoiceManager.lastChoice, text);
        break;

      case 1023:
      case 1024:
        {
          // Like a Bat into Hell
          // Like a Bat out of Hell
          Matcher matcher = ED_RETURN_PATTERN.matcher(text);
          if (matcher.find()) {
            int cost = StringUtilities.parseInt(matcher.group(1));
            int defeats = 3 + (int) (Math.log(cost) / Math.log(2));
            Preferences.setInteger("_edDefeats", defeats);
          }
          break;
        }

      case 1025:
        {
          // Reconfigure your Mini-Crimbot
          Matcher matcher = CRIMBOT_CHASSIS_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("crimbotChassis", matcher.group(1));
          }
          matcher = CRIMBOT_ARM_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("crimbotArm", matcher.group(1));
          }
          matcher = CRIMBOT_PROPULSION_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("crimbotPropulsion", matcher.group(1));
          }
          break;
        }

      case 1049: // Tomb of the Unknown Your Class Here
        if (text.contains("The Epic Weapon's yours")) {
          QuestDatabase.setQuestProgress(Quest.NEMESIS, "step3");
        }
        break;

      case 1051: // The Book of the Undying
        EdBaseRequest.inspectBook(text);
        break;

      case 1053: // The Servants' Quarters
        EdBaseRequest.inspectServants(text);
        EdServantData.inspectServants(text);
        break;

      case 1063:
        {
          // Adjust your 'Edpiece
          Matcher matcher = EDPIECE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("edPiece", matcher.group(1).trim());
          }
          break;
        }

      case 1068:
        {
          // Barf Mountain Breakdown
          Matcher matcher = DINSEY_ROLLERCOASTER_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("dinseyRollercoasterStats", matcher.group(1).trim());
          }
          Preferences.setBoolean(
              "dinseyRapidPassEnabled", text.contains("Disable Rapid-Pass System"));
          break;
        }

      case 1069:
        {
          // The Pirate Bay
          Matcher matcher = DINSEY_PIRATE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setBoolean("dinseyGarbagePirate", (matcher.group(1).equals("lit")));
          }
          break;
        }

      case 1070:
        {
          // In Your Cups
          Matcher matcher = DINSEY_TEACUP_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("dinseyToxicMultiplier", matcher.group(1).trim());
          }
          Preferences.setBoolean(
              "dinseySafetyProtocolsLoose", text.contains("protocols seem pretty loose"));
          break;
        }

      case 1071:
        {
          // Gator Gamer
          Matcher matcher = DINSEY_SLUICE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("dinseyGatorStenchDamage", matcher.group(1).trim());
          }
          Preferences.setBoolean("dinseyAudienceEngagement", text.contains("High Engagement Mode"));
          break;
        }

      case 1075:
        {
          // Mmmmmmayonnaise
          TurnCounter.stopCounting("Mmmmmmayonnaise window begin");
          TurnCounter.stopCounting("Mmmmmmayonnaise window end");
          break;
        }

      case 1076:
        {
          // Mayo Minder&trade;
          Matcher matcher = MAYO_MINDER_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("mayoMinderSetting", matcher.group(1).trim());
          } else {
            Preferences.setString("mayoMinderSetting", "");
          }
          break;
        }

      case 1087:
        // The Dark and Dank and Sinister Cave Entrance
        QuestDatabase.setQuestIfBetter(Quest.NEMESIS, "step11");
        break;

      case 1088:
        // Rubble, Rubble, Toil and Trouble
        QuestDatabase.setQuestIfBetter(Quest.NEMESIS, "step13");
        break;

      case 1093:
        {
          // The WLF Bunker

          // There is no choice if this happens, but we recognise title in visitChoice()
          // You enter the bunker, but the speaker is silent. You've already done your day's work,
          // soldier!
          if (text.contains("the speaker is silent")) {
            Preferences.setBoolean("_volcanoItemRedeemed", true);
            Preferences.setInteger("_volcanoItem1", 0);
            Preferences.setInteger("_volcanoItem2", 0);
            Preferences.setInteger("_volcanoItem3", 0);
            Preferences.setInteger("_volcanoItemCount1", 0);
            Preferences.setInteger("_volcanoItemCount2", 0);
            Preferences.setInteger("_volcanoItemCount3", 0);
            break;
          }

          // On the other hand, if there IS a choice on the page,
          // it will be whichchoice=1093 asking to redeem items

          Preferences.setBoolean("_volcanoItemRedeemed", false);

          Matcher matcher = WLF_PATTERN.matcher(text);
          while (matcher.find()) {
            // String challenge = matcher.group( 1 );
            String descid = matcher.group(2);
            int itemId = ItemDatabase.getItemIdFromDescription(descid);
            if (itemId != -1) {
              String itemName = matcher.group(3).trim();
              Matcher countMatcher = WLF_COUNT_PATTERN.matcher(itemName);
              int count = countMatcher.find() ? StringUtilities.parseInt(countMatcher.group(1)) : 1;
              String index = matcher.group(4);
              Preferences.setInteger("_volcanoItem" + index, itemId);
              Preferences.setInteger("_volcanoItemCount" + index, count);
            }
          }

          break;
        }

      case 1100:
        // Pray to the Barrel God
        if (text.contains("You already prayed to the Barrel god today")) {
          Preferences.setBoolean("_barrelPrayer", true);
          break;
        }
        if (!text.contains("barrel lid shield")) {
          Preferences.setBoolean("prayedForProtection", true);
        }
        if (!text.contains("barrel hoop earring")) {
          Preferences.setBoolean("prayedForGlamour", true);
        }
        if (!text.contains("bankruptcy barrel")) {
          Preferences.setBoolean("prayedForVigor", true);
        }
        break;

      case 1110:
        // Spoopy
        Preferences.setBoolean("doghouseBoarded", !text.contains("Board up the doghouse"));
        break;

      case 1118:
        {
          // X-32-F Combat Training Snowman Control Console
          Matcher matcher = SNOJO_CONSOLE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("snojoSetting", matcher.group(1).trim());
          } else {
            Preferences.setString("snojoSetting", "");
          }
          break;
        }

      case 1181:
        // Witchess Set
        if (!text.contains("Examine the shrink ray")) {
          Preferences.setInteger("_witchessFights", 5);
        }
        break;

      case 1188:
        {
          // The Call is Coming from Outside the Simulation
          Matcher matcher = ENLIGHTENMENT_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger(
                "sourceEnlightenment", StringUtilities.parseInt(matcher.group(1)));
          }
          break;
        }

      case 1191:
        // Source Terminal
        request.setHasResult(false);
        break;

      case 1193:
        {
          // The Precinct
          Matcher matcher = CASE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger(
                "_detectiveCasesCompleted", 3 - StringUtilities.parseInt(matcher.group(1)));
          }
          break;
        }

      case 1195:
        {
          // Spinning Your Time-Spinner
          Matcher matcher = TIME_SPINNER_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger(
                "_timeSpinnerMinutesUsed", 10 - StringUtilities.parseInt(matcher.group(1)));
          }
          break;
        }

      case 1202: // Noon in the Civic Center
      case 1203: // Midnight in Civic Center
      case 1204: // Noon at the Train Station
      case 1205: // Midnight at the Train Station
      case 1206: // Noon in the Industrial Zone
      case 1207: // Midnight in the Industrial Zone
      case 1208: // Upscale Noon
      case 1209: // Upscale Midnight
        {
          Preferences.increment("_gingerbreadCityTurns");
          break;
        }

      case 1229:
        {
          // L.O.V. Exit

          // As you are about to leave the station, you notice a
          // data entry pad and a sign above it that says <WORD>.
          // Huh, that's odd.

          Matcher matcher = LOV_EXIT_PATTERN.matcher(text);
          if (matcher.find()) {
            String message = "L.O.V. Exit word: " + matcher.group(1);
            RequestLogger.printLine(message);
            RequestLogger.updateSessionLog(message);
          }

          break;
        }

      case 1234:
        {
          // Spacegate Vaccinator

          Matcher matcher = VACCINE_PATTERN.matcher(text);

          while (matcher.find()) {
            String setting = "spacegateVaccine" + matcher.group(1);
            String button = matcher.group(2);
            if (button.startsWith("Select Vaccine")) {
              Preferences.setBoolean(setting, true);
            } else if (button.startsWith("Unlock Vaccine")) {
              Preferences.setBoolean(setting, false);
            }
          }
          break;
        }

      case 1259: // LI-11 HQ
        if (text.contains("LI-11 HQ")) {
          Pattern capitalPattern = Pattern.compile("<td><b>(.*?)</b>(.*?)</td></tr>");
          boolean bondAdv = false,
              bondWpn = false,
              bondInit = false,
              bondDR = false,
              bondHP = false,
              bondItem2 = false,
              bondStat = false,
              bondDrunk1 = false,
              bondBooze = false,
              bondSymbols = false,
              bondDrunk2 = false,
              bondJetpack = false,
              bondMartiniTurn = false,
              bondMeat = false,
              bondItem1 = false,
              bondMus1 = false,
              bondMys1 = false,
              bondMox1 = false,
              bondBeach = false,
              bondBeat = false,
              bondMartiniDelivery = false,
              bondMus2 = false,
              bondMys2 = false,
              bondMox2 = false,
              bondStealth = false,
              bondMartiniPlus = false,
              bondBridge = false,
              bondWar = false,
              bondMPregen = false,
              bondWeapon2 = false,
              bondItem3 = false,
              bondStealth2 = false,
              bondSpleen = false,
              bondStat2 = false,
              bondDesert = false,
              bondHoney = false;
          Matcher matcher = capitalPattern.matcher(text);
          while (matcher.find()) {
            if (matcher.group(2).contains("Active") || matcher.group(2).contains("Connected")) {
              if (matcher.group(1).equals("Super-Accurate Spy Watch")) {
                bondAdv = true;
              } else if (matcher.group(1).equals("Razor-Sharp Tie")) {
                bondWpn = true;
              } else if (matcher.group(1).equals("Jet-Powered Skis")) {
                bondInit = true;
              } else if (matcher.group(1).equals("Kevlar-Lined Pants")) {
                bondDR = true;
              } else if (matcher.group(1).equals("Injected Nanobots")) {
                bondHP = true;
              } else if (matcher.group(1).equals("Sticky Climbing Gloves")) {
                bondItem2 = true;
              } else if (matcher.group(1).equals("Retinal Knowledge HUD")) {
                bondStat = true;
              } else if (matcher.group(1).equals("Belt-Implanted Still")) {
                bondDrunk1 = true;
              } else if (matcher.group(1).equals("Alcohol Absorbent Underwear")) {
                bondBooze = true;
              } else if (matcher.group(1).equals("Universal Symbology Guide")) {
                bondSymbols = true;
              } else if (matcher.group(1).equals("Soberness Injection Pen")) {
                bondDrunk2 = true;
              } else if (matcher.group(1).equals("Short-Range Jetpack")) {
                bondJetpack = true;
              } else if (matcher.group(1).equals("Invisible Meat Car, the Vanish")) {
                bondStealth = true;
              } else if (matcher.group(1).equals("Portable Pocket Bridge")) {
                bondBridge = true;
              } else if (matcher.group(1).equals("Static-Inducing, Bug-Shorting Underpants")) {
                bondMPregen = true;
              } else if (matcher.group(1).equals("Exotic Bartender, Barry L. Eagle")) {
                bondMartiniTurn = true;
              } else if (matcher.group(1).equals("Renowned Meat Thief, Ivanna Cuddle")) {
                bondMeat = true;
              } else if (matcher.group(1).equals("Master Art Thief, Sly Richard")) {
                bondItem1 = true;
              } else if (matcher.group(1).equals("Personal Trainer, Debbie Dallas")) {
                bondMus1 = true;
              } else if (matcher.group(1).equals("Rocket Scientist, Crimbo Jones")) {
                bondMys1 = true;
              } else if (matcher.group(1).equals("Licensed Masseur, Oliver Closehoff")) {
                bondMox1 = true;
              } else if (matcher.group(1).equals("Professional Cabbie, Rock Hardy")) {
                bondBeach = true;
              } else if (matcher.group(1).equals("Fellow Spy, Daisy Duke")) {
                bondBeat = true;
              } else if (matcher.group(1).equals("Fellow Spy, Prince O'Toole")) {
                bondMartiniDelivery = true;
              } else if (matcher.group(1).equals("Personal Kinesiologist, Doctor Kittie")) {
                bondMus2 = true;
              } else if (matcher.group(1).equals("Computer Hacker, Mitt Jobs")) {
                bondMys2 = true;
              } else if (matcher.group(1).equals("Spa Owner, Fatima Jiggles")) {
                bondMox2 = true;
              } else if (matcher.group(1).equals("Exotic Olive Procurer, Ben Dover")) {
                bondMartiniPlus = true;
              } else if (matcher.group(1).equals("Trained Sniper, Felicity Snuggles")) {
                bondWar = true;
              } else if (matcher.group(1).equals("Martial Arts Trainer, Jaques Trappe")) {
                bondWeapon2 = true;
              } else if (matcher.group(1).equals("Electromagnetic Ring")) {
                bondItem3 = true;
              } else if (matcher.group(1).equals("Robo-Spleen")) {
                bondSpleen = true;
              } else if (matcher.group(1).equals("Universal GPS")) {
                bondDesert = true;
              } else if (matcher.group(1).equals("Mission Controller, Maeby Moneypenny")) {
                bondStealth2 = true;
              } else if (matcher.group(1).equals("Sage Advisor, London McBrittishman")) {
                bondStat2 = true;
              } else if (matcher.group(1).equals("True Love, Honey Potts")) {
                bondHoney = true;
              }
            }
          }
          Preferences.setBoolean("bondAdv", bondAdv);
          Preferences.setBoolean("bondWpn", bondWpn);
          Preferences.setBoolean("bondInit", bondInit);
          Preferences.setBoolean("bondDA", bondDR);
          Preferences.setBoolean("bondHP", bondHP);
          Preferences.setBoolean("bondItem2", bondItem2);
          Preferences.setBoolean("bondStat", bondStat);
          Preferences.setBoolean("bondDrunk1", bondDrunk1);
          Preferences.setBoolean("bondBooze", bondBooze);
          Preferences.setBoolean("bondSymbols", bondSymbols);
          Preferences.setBoolean("bondDrunk2", bondDrunk2);
          Preferences.setBoolean("bondJetpack", bondJetpack);
          Preferences.setBoolean("bondStealth", bondStealth);
          Preferences.setBoolean("bondMartiniTurn", bondMartiniTurn);
          Preferences.setBoolean("bondMeat", bondMeat);
          Preferences.setBoolean("bondItem1", bondItem1);
          Preferences.setBoolean("bondMus1", bondMus1);
          Preferences.setBoolean("bondMys1", bondMys1);
          Preferences.setBoolean("bondMox1", bondMox1);
          Preferences.setBoolean("bondBeach", bondBeach);
          Preferences.setBoolean("bondBeat", bondBeat);
          Preferences.setBoolean("bondMartiniDelivery", bondMartiniDelivery);
          Preferences.setBoolean("bondMus2", bondMus2);
          Preferences.setBoolean("bondMys2", bondMys2);
          Preferences.setBoolean("bondMox2", bondMox2);
          Preferences.setBoolean("bondMartiniPlus", bondMartiniPlus);
          Preferences.setBoolean("bondBridge", bondBridge); // need to handle when this is selected
          Preferences.setBoolean("bondWar", bondWar);
          Preferences.setBoolean("bondMPregen", bondMPregen);
          Preferences.setBoolean("bondWeapon2", bondWeapon2);
          Preferences.setBoolean("bondItem3", bondItem3);
          Preferences.setBoolean("bondStealth2", bondStealth2);
          Preferences.setBoolean("bondSpleen", bondSpleen);
          Preferences.setBoolean("bondStat2", bondStat2);
          Preferences.setBoolean("bondDesert", bondDesert); // Do something with this
          Preferences.setBoolean("bondHoney", bondHoney);
          if (bondBeach) {
            KoLCharacter.setDesertBeachAvailable();
          }
          KoLCharacter.recalculateAdjustments();
          KoLCharacter.updateStatus();
        }
        break;

      case 1266:
        {
          // The Hostler
          Preferences.setBoolean("horseryAvailable", true);

          // <td valign=top class=small><b>Drab Teddy</b> the Normal Horse<P>
          // <td valign=top class=small><b>Surreptitious Mantilla</b> the Dark Horse<P>
          // <td valign=top class=small><b>Wacky Biggles</b> the Crazy Horse<P>
          // <td valign=top class=small><b>Frightful Twiggy</b> the Pale Horse<P>

          // Save the horse names so we can recognize them in combat
          Pattern pattern =
              Pattern.compile("<td valign=top class=small><b>([^<]+)</b> the ([^ ]+) Horse<P>");
          Matcher matcher = pattern.matcher(text);
          while (matcher.find()) {
            String name = matcher.group(1);
            String type = matcher.group(2);
            String setting =
                type.equals("Crazy")
                    ? "_horseryCrazyName"
                    : type.equals("Dark")
                        ? "_horseryDarkName"
                        : type.equals("Normal")
                            ? "_horseryNormalName"
                            : type.equals("Pale") ? "_horseryPaleName" : null;
            if (setting != null) {
              Preferences.setString(setting, name);
            }
          }

          pattern =
              Pattern.compile(
                  "Gives you\\s+([+-]\\d+)% Muscle, ([+-]\\d+)% Mysticality, and ([+-]\\d+)%");
          matcher = pattern.matcher(text);
          if (matcher.find()) {
            Preferences.setString("_horseryCrazyMus", matcher.group(1));
            Preferences.setString("_horseryCrazyMys", matcher.group(2));
            Preferences.setString("_horseryCrazyMox", matcher.group(3));
          }

          if (!text.contains("name=option value=1")) {
            Preferences.setString("_horsery", "normal horse");
          } else if (!text.contains("name=option value=2")) {
            Preferences.setString("_horsery", "dark horse");
          } else if (!text.contains("name=option value=3")) {
            Preferences.setString("_horsery", "crazy horse");
          } else if (!text.contains("name=option value=4")) {
            Preferences.setString("_horsery", "pale horse");
          } else {
            Preferences.setString("_horsery", "");
          }
          break;
        }

      case 1267:
        // Rubbed it the Right Way
        GenieRequest.visitChoice(text);
        break;

      case 1308:
        {
          // place.php?whichplace=monorail&action=monorail_downtown
          //
          // On a Downtown Train
          //
          // Everything you do at this location uses this choice
          // adventure #. As you select options, you stay in the
          // same choice, but the available options change.
          //
          // We must deduce what is happening by looking at the
          // response text

          // Looks like your order for a <muffin type> muffin is not yet ready.
          Matcher muffinMatcher = MUFFIN_TYPE_PATTERN.matcher(text);
          if (muffinMatcher.find()) {
            Preferences.setString("muffinOnOrder", muffinMatcher.group(1));
          }
          // "Sorry, you placed your order a lifetime ago, so we had to throw out the actual baked
          // good. Here's your earthenware cookware.
          else if (text.contains("you placed your order a lifetime ago")) {
            Preferences.setString("muffinOnOrder", "none");
          }
          // You spot your order from the other day, neatly labelled on the pickup shelf.
          else if (text.contains("You spot your order from the other day")) {
            Preferences.setString("muffinOnOrder", "none");
          }
          // (If we have an earthenware muffin tin and the "order" buttons are present)
          else if (text.contains("Order a blueberry muffin")) {
            Preferences.setString("muffinOnOrder", "none");
          }

          // "Excellent! Here's your muffin tin! Stop by any time to order a muffin!"
          if (text.contains("Here's your muffin tin!")) {
            ResultProcessor.processItem(ItemPool.SHOVELFUL_OF_EARTH, -10);
            ResultProcessor.processItem(ItemPool.HUNK_OF_GRANITE, -10);
          }
          break;
        }

      case 1309:
        // We will either now, or in the past, have had Favored By Lyle
        Preferences.setBoolean("_lyleFavored", true);
        break;

      case 1312:
        {
          // Choose a Soundtrack
          Matcher matcher = BOOMBOX_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("_boomBoxSongsLeft", matcher.group(1));
          }
          Preferences.setString("boomBoxSong", "");
          matcher = BOOMBOX_SONG_PATTERN.matcher(text);
          while (matcher.find()) {
            if (matcher.group(2) != null && matcher.group(2).contains("Keep playing")) {
              Preferences.setString("boomBoxSong", matcher.group(1));
            }
          }
          break;
        }

      case 1313: // Bastille Battalion
      case 1314: // Bastille Battalion (Master of None)
      case 1315: // Castle vs. Castle
      case 1316: // GAME OVER
      case 1317: // A Hello to Arms (Battalion)
      case 1318: // Defensive Posturing
      case 1319: // Cheese Seeking Behavior
        BastilleBattalionManager.visitChoice(request);
        break;

      case 1322:
        // The Beginning of the Neverend
        if (text.contains("talk to him and help him get more booze")) {
          Preferences.setString("_questPartyFairQuest", "booze");
        } else if (text.contains("Think you can help me clean the place up?")) {
          Preferences.setString("_questPartyFairQuest", "trash");
        } else if (text.contains(
            "helping her with whatever problem she's having with the snacks")) {
          Preferences.setString("_questPartyFairQuest", "food");
        } else if (text.contains("megawoots right now")) {
          Preferences.setString("_questPartyFairQuest", "woots");
        } else if (text.contains("taking up a collection from the guests")) {
          Preferences.setString("_questPartyFairQuest", "dj");
        } else if (text.contains("all of the people to leave")) {
          Preferences.setString("_questPartyFairQuest", "partiers");
        }
        break;

      case 1324:
        // It Hasn't Ended, It's Just Paused
        Preferences.setInteger("encountersUntilNEPChoice", 7);
        break;

      case 1329:
        // Latte Shop
        LatteRequest.parseVisitChoice(text);
        break;

      case 1331:
        {
          // Daily Loathing Ballot
          Matcher localMatcher = VOTE_PATTERN.matcher(text);
          while (localMatcher.find()) {
            int voteValue = StringUtilities.parseInt(localMatcher.group(1)) + 1;
            String voteMod = Modifiers.parseModifier(localMatcher.group(3));
            if (voteMod != null) {
              Preferences.setString("_voteLocal" + voteValue, voteMod);
            }
          }

          Matcher platformMatcher = VOTE_SPEECH_PATTERN.matcher(text);

          int count = 1;

          while (platformMatcher.find()) {
            String party = platformMatcher.group(3);
            String speech = platformMatcher.group(4);

            String monsterName = parseVoteSpeech(party, speech);

            if (monsterName != null) {
              Preferences.setString("_voteMonster" + count, monsterName);
            }

            count++;
          }
          break;
        }

      case 1336:
        {
          // Boxing Daycare
          Matcher matcher = DAYCARE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("daycareEquipment", matcher.group(1).replaceAll(",", ""));
            Preferences.setString("daycareToddlers", matcher.group(2).replaceAll(",", ""));
            String instructors = matcher.group(3);
            if (instructors.equals("an")) {
              instructors = "1";
            }
            Preferences.setString("daycareInstructors", instructors);
          } else {
            matcher = EARLY_DAYCARE_PATTERN.matcher(text);
            if (matcher.find()) {
              Preferences.setString("daycareToddlers", matcher.group(1).replaceAll(",", ""));
              String instructors = matcher.group(2);
              if (instructors.equals("an")) {
                instructors = "1";
              }
              Preferences.setString("daycareInstructors", instructors);
            }
          }
          Matcher recruitsToday = DAYCARE_RECRUITS_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger(
                "_daycareRecruits", (recruitsToday.group(1).replaceAll(",", "")).length() - 3);
          }
          // *** Update _daycareScavenges
          // *** Update daycareInstructorCost (new)
          break;
        }

      case 1339:
        {
          // A Little Pump and Grind
          Matcher matcher = SAUSAGE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger(
                "_sausagesMade",
                StringUtilities.parseInt(matcher.group(2).replaceAll(",", "")) / 111 - 1);
            Preferences.setString("sausageGrinderUnits", matcher.group(3));
          }
          break;
        }

      case 1340:
        {
          // Is There A Doctor In The House?
          Matcher matcher = DOCTOR_BAG_PATTERN.matcher(text);
          if (matcher.find()) {
            String malady = matcher.group(1);
            String item = "";
            if (malady.contains("tropical heatstroke")) {
              item = "palm-frond fan";
            } else if (malady.contains("archaic cough")) {
              item = "antique bottle of cough syrup";
            } else if (malady.contains("broken limb")) {
              item = "cast";
            } else if (malady.contains("low vim and vigor")) {
              item = "Doc Galaktik's Vitality Serum";
            } else if (malady.contains("bad clams")) {
              item = "anti-anti-antidote";
            } else if (malady.contains("criss-cross laceration")) {
              item = "plaid bandage";
            } else if (malady.contains("knocked out by a random encounter")) {
              item = "phonics down";
            } else if (malady.contains("Thin Blood Syndrome")) {
              item = "red blood cells";
            } else if (malady.contains("a blood shortage")) {
              item = "bag of pygmy blood";
            }
            Preferences.setString("doctorBagQuestItem", item);
            Preferences.setString("doctorBagQuestLocation", matcher.group(2));
          }
          break;
        }

      case 1388:
        BeachManager.parseCombUsage(text);
        BeachManager.parseBeachMap(text);
        break;

      case 1396:
        {
          // Adjusting Your Fish
          Matcher matcher = RED_SNAPPER_PATTERN.matcher(text);
          if (matcher.find()) {
            Phylum phylum = Phylum.find(matcher.group(1));
            int progress = StringUtilities.parseInt(matcher.group(2));
            Preferences.setString("redSnapperPhylum", phylum.toString());
            Preferences.setInteger("redSnapperProgress", progress);
          }
          break;
        }

      case 1407:
        {
          // Mushroom District Costume Shop
          Matcher matcher = MUSHROOM_COSTUME_PATTERN.matcher(text);
          int cost = 0;
          boolean carpenter = false;
          boolean gardener = false;
          boolean ballerina = false;
          while (matcher.find()) {
            String costume = matcher.group(2);
            cost = StringUtilities.parseInt(matcher.group(3));
            if (costume.equals("Carpenter")) {
              carpenter = true;
            } else if (costume.equals("Gardener")) {
              gardener = true;
            } else if (costume.equals("Ballerina")) {
              ballerina = true;
            }
          }
          String wearing =
              !carpenter ? "muscle" : !gardener ? "mysticality" : !ballerina ? "moxie" : "none";
          Preferences.setInteger("plumberCostumeCost", cost);
          Preferences.setString("plumberCostumeWorn", wearing);
          break;
        }

      case 1408:
        {
          // Mushroom District Badge Shop
          Matcher matcher = MUSHROOM_BADGE_PATTERN.matcher(text);
          int cost = 0;
          if (matcher.find()) {
            cost = StringUtilities.parseInt(matcher.group(1));
          }
          Preferences.setInteger("plumberBadgeCost", cost);
          break;
        }

      case 1410:
        {
          // The Mushy Center

          // The mushroom in your garden is now large enough to walk around inside.  Also there's a
          // door on it, which is convenient for that purpose.

          int mushroomMessageLevel =
              text.contains("walk around inside")
                  ? 11
                  : text.contains("immense mushroom")
                      ? 5
                      : text.contains("giant mushroom")
                          ? 4
                          : text.contains("bulky mushroom")
                              ? 3
                              : text.contains("plump mushroom")
                                  ? 2
                                  : text.contains("decent-sized mushroom") ? 1 : 0;

          // mushgrow5.gif is used for both the immense and colossal mushroom

          int mushroomImageLevel =
              text.contains("mushgrow5.gif")
                  ? 5
                  : text.contains("mushgrow4.gif")
                      ? 4
                      : text.contains("mushgrow3.gif")
                          ? 3
                          : text.contains("mushgrow2.gif")
                              ? 2
                              : text.contains("mushgrow1.gif") ? 1 : 0;

          int mushroomLevel = Math.max(mushroomMessageLevel, mushroomImageLevel);

          // After level 5, you can continue fertilizing for an unknown number
          // of days before you get the ultimate mushroom.

          // If we have fertilized the garden through KoLmafia,
          // this is the number of days we have fertilized.
          int currentLevel = Preferences.getInteger("mushroomGardenCropLevel");

          // If we have not fertilized consistently through KoLmafia, correct it here
          int newLevel = Math.max(mushroomLevel, currentLevel);

          Preferences.setInteger("mushroomGardenCropLevel", newLevel);
          CampgroundRequest.clearCrop();
          CampgroundRequest.setCampgroundItem(new Mushroom(newLevel));
          break;
        }
      case 1412:
        Matcher tierMatcher = GUZZLR_TIER_PATTERN.matcher(text);
        while (tierMatcher.find()) {
          Preferences.setInteger(
              "guzzlr" + tierMatcher.group(2) + "Deliveries",
              StringUtilities.parseInt(tierMatcher.group(1)));
        }

        if (text.contains("You are currently tasked with")) {
          Matcher alreadyMatcher = GUZZLR_QUEST_PATTERN.matcher(text);
          if (alreadyMatcher.find()) {
            String booze = alreadyMatcher.group(1);
            Preferences.setString(
                "guzzlrQuestBooze",
                booze.equals("special personalized cocktail") ? "Guzzlr cocktail set" : booze);
            Preferences.setString("guzzlrQuestClient", alreadyMatcher.group(2));
            Preferences.setString("guzzlrQuestLocation", alreadyMatcher.group(3));
          }

          int itemId = ItemDatabase.getItemId(Preferences.getString("guzzlrQuestBooze"));

          if (itemId > 0) {
            if (InventoryManager.getCount(itemId) > 0) {
              QuestDatabase.setQuestProgress(Quest.GUZZLR, "step1");
            } else {
              QuestDatabase.setQuestProgress(Quest.GUZZLR, QuestDatabase.STARTED);
            }
          } else {
            QuestDatabase.setQuestProgress(Quest.GUZZLR, QuestDatabase.STARTED);
          }

          Preferences.setBoolean(
              "_guzzlrQuestAbandoned",
              (ChoiceUtilities.findChoiceDecisionIndex("Abandon Client", text).equals("0")));

          break;
        }

        // If we have unlocked Gold Tier but cannot accept one, we must have already accepted three.
        boolean unlockedGoldTier = Preferences.getInteger("guzzlrBronzeDeliveries") >= 5;
        if (unlockedGoldTier
            && ChoiceUtilities.findChoiceDecisionIndex("Gold Tier", text).equals("0")) {
          Preferences.setInteger("_guzzlrGoldDeliveries", 3);
        }

        // If we have unlocked Platinum Tier but cannot accept one, we must have already accepted
        // one.
        boolean unlockedPlatinumTier = Preferences.getInteger("guzzlrGoldDeliveries") >= 5;
        if (unlockedPlatinumTier
            && ChoiceUtilities.findChoiceDecisionIndex("Platinum Tier", text).equals("0")) {
          Preferences.setInteger("_guzzlrPlatinumDeliveries", 1);
        }

        break;

      case 1420:
        // Cargo Cultist Shorts
        CargoCultistShortsRequest.parseAvailablePockets(text);
        break;

      case 1445: // Reassembly Station
      case 1447: // Statbot 5000
        YouRobotManager.visitChoice(request);
        break;

      case 1448:
        {
          String[] stalkStatus = new String[7];
          Matcher stalks =
              Pattern.compile(
                      "<button.*?name=\"pp\" value=\"(\\d+)\".*?>\\s+<img.*?src=\".*?/otherimages/powerplant/(\\d+)\\.png\"")
                  .matcher(text);
          while (stalks.find()) {
            int pp = Integer.parseInt(stalks.group(1)) - 1;
            stalkStatus[pp] = stalks.group(2);
          }
          Preferences.setString("_pottedPowerPlant", String.join(",", stalkStatus));
          break;
        }
      case 1449:
        {
          String setting =
              (!text.contains("Warning Beep"))
                  ? "ml"
                  : (!text.contains("Infrared Spectrum"))
                      ? "meat"
                      : (!text.contains("Maximum Framerate")) ? "init" : "";

          Preferences.setString("backupCameraMode", setting);
          Preferences.setBoolean("backupCameraReverserEnabled", text.contains("Disable Reverser"));
          break;
        }
      case 1455:
        CampgroundRequest.setCurrentWorkshedItem(ItemPool.COLD_MEDICINE_CABINET);
        Matcher consultations = Pattern.compile("You have <b>(\\d)</b> consul").matcher(text);
        if (consultations.find()) {
          int remaining = Integer.parseInt(consultations.group(1));
          Preferences.setInteger("_coldMedicineConsults", 5 - remaining);
        }
        break;
      case 1462:
        CrystalBallManager.parsePonder(text);
        break;
      case 1463:
        LocketManager.parseMonsters(text);
        break;
    }
  }

  private static Option booPeakDamage() {
    int booPeakLevel =
        ChoiceAdventures.findBooPeakLevel(
            ChoiceUtilities.findChoiceDecisionText(1, ChoiceManager.lastResponseText));
    if (booPeakLevel < 1) return new Option("");

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
    return new Option(
        ((int) Math.ceil(spookyDamage))
            + " spooky damage, "
            + ((int) Math.ceil(coldDamage))
            + " cold damage");
  }

  private static int findBooPeakLevel(String decisionText) {
    if (decisionText == null) {
      return 0;
    }
    if (decisionText.equals("Ask the Question")
        || decisionText.equals("Talk to the Ghosts")
        || decisionText.equals("I Wanna Know What Love Is")
        || decisionText.equals("Tap Him on the Back")
        || decisionText.equals("Avert Your Eyes")
        || decisionText.equals("Approach a Raider")
        || decisionText.equals("Approach the Argument")
        || decisionText.equals("Approach the Ghost")
        || decisionText.equals("Approach the Accountant Ghost")
        || decisionText.equals("Ask if He's Lost")) {
      return 1;
    } else if (decisionText.equals("Enter the Crypt")
        || decisionText.equals("Try to Talk Some Sense into Them")
        || decisionText.equals("Put Your Two Cents In")
        || decisionText.equals("Talk to the Ghost")
        || decisionText.equals("Tell Them What Werewolves Are")
        || decisionText.equals("Scream in Terror")
        || decisionText.equals("Check out the Duel")
        || decisionText.equals("Watch the Fight")
        || decisionText.equals("Approach and Reproach")
        || decisionText.equals("Talk Back to the Robot")) {
      return 2;
    } else if (decisionText.equals("Go down the Steps")
        || decisionText.equals("Make a Suggestion")
        || decisionText.equals("Tell Them About True Love")
        || decisionText.equals("Scold the Ghost")
        || decisionText.equals("Examine the Pipe")
        || decisionText.equals("Say What?")
        || decisionText.equals("Listen to the Lesson")
        || decisionText.equals("Listen in on the Discussion")
        || decisionText.equals("Point out the Malefactors")
        || decisionText.equals("Ask for Information")) {
      return 3;
    } else if (decisionText.equals("Hurl Some Spells of Your Own")
        || decisionText.equals("Take Command")
        || decisionText.equals("Lose Your Patience")
        || decisionText.equals("Fail to Stifle a Sneeze")
        || decisionText.equals("Ask for Help")
        || decisionText.equals(
            "Ask How Duskwalker Basketball Is Played, Against Your Better Judgment")
        || decisionText.equals("Knights in White Armor, Never Reaching an End")
        || decisionText.equals("Own up to It")
        || decisionText.equals("Approach the Poor Waifs")
        || decisionText.equals("Look Behind You")) {
      return 4;
    } else if (decisionText.equals("Read the Book")
        || decisionText.equals("Join the Conversation")
        || decisionText.equals("Speak of the Pompatus of Love")
        || decisionText.equals("Ask What's Going On")
        || decisionText.equals("Interrupt the Rally")
        || decisionText.equals("Ask What She's Doing Up There")
        || decisionText.equals("Point Out an Unfortunate Fact")
        || decisionText.equals("Try to Talk Sense")
        || decisionText.equals("Ask for Directional Guidance")
        || decisionText.equals("What?")) {
      return 5;
    }

    return 0;
  }

  private static void checkGuyMadeOfBees(final GenericRequest request) {
    KoLCharacter.ensureUpdatedGuyMadeOfBees();
    Preferences.increment("guyMadeOfBeesCount", 1, 5, true);

    String text = request.responseText;
    String urlString = request.getPath();

    if (urlString.startsWith("fight.php")) {
      if (text.contains("guy made of bee pollen")) {
        // Record that we beat the guy made of bees.
        Preferences.setBoolean("guyMadeOfBeesDefeated", true);
      }
    } else if (urlString.startsWith("choice.php") && text.contains("that ship is sailed")) {
      // For some reason, we didn't notice when we
      // beat the guy made of bees. Record it now.
      Preferences.setBoolean("guyMadeOfBeesDefeated", true);
    }
  }

  private static void handleSourceTerminal(final String input, String text) {
    if (input.startsWith("educate")) {
      int successIndex = text.lastIndexOf("active skills");
      int failIndex = text.lastIndexOf("missing educate");
      int listIndex = text.lastIndexOf("usage: educate [target file]");

      if (listIndex > successIndex && listIndex > failIndex) {
        int startIndex = text.lastIndexOf("available targets:");
        int endIndex = text.lastIndexOf("&gt;");
        if (startIndex == -1 || endIndex == -1) {
          // this shouldn't happen...
          return;
        }
        text = text.substring(startIndex, endIndex);
        Pattern EDUCATE_PATTERN = Pattern.compile("rel=\"educate (.*?).edu\"");
        StringBuilder knownString = new StringBuilder();

        Matcher matcher = EDUCATE_PATTERN.matcher(text);
        while (matcher.find()) {
          if (knownString.length() > 0) {
            knownString.append(",");
          }
          knownString.append(matcher.group(1)).append(".edu");
        }
        Preferences.setString("sourceTerminalEducateKnown", knownString.toString());
        return;
      }

      if (failIndex > successIndex) return;

      String skill = input.substring(7).trim();

      if (Preferences.getString("sourceTerminalChips").contains("DRAM")) {
        Preferences.setString(
            "sourceTerminalEducate1", Preferences.getString("sourceTerminalEducate2"));
        Preferences.setString("sourceTerminalEducate2", skill);
      } else {
        Preferences.setString("sourceTerminalEducate1", skill);
      }
    } else if (input.startsWith("enhance")) {
      int successIndex = text.lastIndexOf("You acquire an effect");
      int badInputIndex = text.lastIndexOf("missing enhance");
      int limitIndex = text.lastIndexOf("enhance limit exceeded");
      int listIndex = text.lastIndexOf("usage: enhance [target file]");

      if (listIndex > limitIndex && listIndex > badInputIndex && listIndex > successIndex) {
        int startIndex = text.lastIndexOf("available targets:");
        int endIndex = text.lastIndexOf("&gt;");
        if (startIndex == -1 || endIndex == -1) {
          // this shouldn't happen...
          return;
        }
        text = text.substring(startIndex, endIndex);
        Pattern ENHANCE_PATTERN = Pattern.compile("rel=\"enhance (.*?).enh\"");
        StringBuilder knownString = new StringBuilder();

        Matcher matcher = ENHANCE_PATTERN.matcher(text);
        while (matcher.find()) {
          if (knownString.length() > 0) {
            knownString.append(",");
          }
          knownString.append(matcher.group(1) + ".enh");
        }
        Preferences.setString("sourceTerminalEnhanceKnown", knownString.toString());
        return;
      }

      if (limitIndex > badInputIndex && limitIndex > successIndex) {
        String chips = Preferences.getString("sourceTerminalChips");
        int limit = 1 + (chips.contains("CRAM") ? 1 : 0) + (chips.contains("SCRAM") ? 1 : 0);
        Preferences.setInteger("_sourceTerminalEnhanceUses", limit);
        return;
      }

      if (badInputIndex > successIndex) return;

      int startIndex = text.lastIndexOf("You acquire");
      int endIndex = text.lastIndexOf("&gt;");
      if (startIndex == -1 || endIndex == -1) {
        // this shouldn't happen...
        return;
      }
      text = text.substring(startIndex, endIndex);
      Pattern ENHANCE_PATTERN =
          Pattern.compile("acquire an effect: (.*?) \\(duration: (\\d+) Adventures\\)</div>");

      Matcher matcher = ENHANCE_PATTERN.matcher(text);
      if (matcher.find()) {
        String message =
            "You acquire an effect: " + matcher.group(1) + " (" + matcher.group(2) + ")";
        RequestLogger.printLine(message);
        RequestLogger.updateSessionLog(message);
        // Refresh status manually since KoL doesn't trigger it
        ApiRequest.updateStatus(true);
      }

      Preferences.increment("_sourceTerminalEnhanceUses");
    } else if (input.startsWith("enquiry")) {
      int successIndex = text.lastIndexOf("enquiry mode set:");
      int failIndex = text.lastIndexOf("missing enquiry target");
      int listIndex = text.lastIndexOf("usage: enquiry [target file]");

      if (listIndex > failIndex && listIndex > successIndex) {
        int startIndex = text.lastIndexOf("available targets:");
        int endIndex = text.lastIndexOf("&gt;");
        if (startIndex == -1 || endIndex == -1) {
          // this shouldn't happen...
          return;
        }
        text = text.substring(startIndex, endIndex);
        Pattern ENQUIRY_PATTERN = Pattern.compile("rel=\"enquiry (.*?).enq\"");
        StringBuilder knownString = new StringBuilder();

        Matcher matcher = ENQUIRY_PATTERN.matcher(text);
        while (matcher.find()) {
          if (knownString.length() > 0) {
            knownString.append(",");
          }
          knownString.append(matcher.group(1) + ".enq");
        }
        Preferences.setString("sourceTerminalEnquiryKnown", knownString.toString());
        return;
      }

      if (failIndex > successIndex) return;

      int beginIndex = successIndex + 18;
      int endIndex = text.indexOf("</div>", beginIndex);
      Preferences.setString("sourceTerminalEnquiry", text.substring(beginIndex, endIndex));
    } else if (input.startsWith("extrude")) {
      int acquire = text.lastIndexOf("You acquire");
      int invalid = text.lastIndexOf("Invalid");
      int insufficient = text.lastIndexOf("Insufficient");
      int confirm = text.lastIndexOf("to confirm");
      int exceeded = text.lastIndexOf("limits exceeded");
      int listIndex = text.lastIndexOf("usage: extrude [target file]");

      if (listIndex > acquire
          && listIndex > invalid
          && listIndex > insufficient
          && listIndex > confirm
          && listIndex > exceeded) {
        int startIndex = text.lastIndexOf("available targets:");
        int endIndex = text.lastIndexOf("&gt;");
        if (startIndex == -1 || endIndex == -1) {
          // this shouldn't happen...
          return;
        }
        text = text.substring(startIndex, endIndex);
        Pattern EXTRUDE_PATTERN = Pattern.compile("rel=\"extrude (.*?).ext\"");
        StringBuilder knownString = new StringBuilder();

        Matcher matcher = EXTRUDE_PATTERN.matcher(text);
        while (matcher.find()) {
          if (knownString.length() > 0) {
            knownString.append(",");
          }
          knownString.append(matcher.group(1) + ".ext");
        }
        Preferences.setString("sourceTerminalExtrudeKnown", knownString.toString());
        return;
      }

      if (exceeded > acquire
          && exceeded > invalid
          && exceeded > insufficient
          && exceeded > confirm) {
        Preferences.setInteger("_sourceTerminalExtrudes", 3);
        return;
      }

      if (invalid > acquire || insufficient > acquire || confirm > acquire) return;

      // Creation must have succeeded
      String message = "";
      if (input.contains("food")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.BROWSER_COOKIE));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -10));
        message = "You acquire an item: browser cookie";
      } else if (input.contains("booze")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.HACKED_GIBSON));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -10));
        message = "You acquire an item: hacked gibson";
      } else if (input.contains("goggles")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_SHADES));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -100));
        message = "You acquire an item: Source shades";
      } else if (input.contains("gram")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_TERMINAL_GRAM_CHIP));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -100));
        message = "You acquire an item: Source terminal GRAM chip";
      } else if (input.contains("pram")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_TERMINAL_PRAM_CHIP));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -100));
        message = "You acquire an item: Source terminal PRAM chip";
      } else if (input.contains("spam")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_TERMINAL_SPAM_CHIP));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -100));
        message = "You acquire an item: Source terminal SPAM chip";
      } else if (input.contains("cram")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_TERMINAL_CRAM_CHIP));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -1000));
        message = "You acquire an item: Source terminal CRAM chip";
      } else if (input.contains("dram")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_TERMINAL_DRAM_CHIP));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -1000));
        message = "You acquire an item: Source terminal DRAM chip";
      } else if (input.contains("tram")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_TERMINAL_TRAM_CHIP));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -1000));
        message = "You acquire an item: Source terminal TRAM chip";
      } else if (input.contains("familiar")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOFTWARE_BUG));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -10000));
        message = "You acquire an item: software bug";
      }
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
      Preferences.increment("_sourceTerminalExtrudes");
    } else if (input.startsWith("status")) {
      int startIndex = text.lastIndexOf("Installed Hardware");
      int endIndex = text.lastIndexOf("&gt;");
      if (startIndex == -1 || endIndex == -1) {
        // this shouldn't happen...
        return;
      }
      text = text.substring(startIndex, endIndex);
      Pattern CHIPS_PATTERN =
          Pattern.compile(
              "PRAM chips installed: (\\d+).*?GRAM chips installed: (\\d+).*?SPAM chips installed: (\\d+)");
      Pattern CHIP_PATTERN = Pattern.compile("<div>(.*?) chip installed");
      StringBuilder chipString = new StringBuilder();

      Matcher matcher = CHIPS_PATTERN.matcher(text);
      if (matcher.find()) {
        Preferences.setInteger("sourceTerminalPram", StringUtilities.parseInt(matcher.group(1)));
        Preferences.setInteger("sourceTerminalGram", StringUtilities.parseInt(matcher.group(2)));
        Preferences.setInteger("sourceTerminalSpam", StringUtilities.parseInt(matcher.group(3)));
      }

      text = text.substring(text.indexOf("reduced"));
      matcher = CHIP_PATTERN.matcher(text);
      while (matcher.find()) {
        if (chipString.length() > 0) {
          chipString.append(",");
        }
        chipString.append(matcher.group(1));
      }
      Preferences.setString("sourceTerminalChips", chipString.toString());
    }
  }

  private static String hobopolisBossName(final int choice) {
    switch (choice) {
      case 200:
        // Enter The Hoboverlord
        return "Hodgman";
      case 201:
        // Home, Home in the Range
        return "Ol' Scratch";
      case 202:
        // Bumpity Bump Bump
        return "Frosty";
      case 203:
        // Deep Enough to Dive
        return "Oscus";
      case 204:
        // Welcome To You!
        return "Zombo";
      case 205:
        // Van, Damn
        return "Chester";
      case 518:
        // Clear and Present Danger
        return "Uncle Hobo";
    }

    return "nobody";
  }

  private static void checkDungeonSewers(final GenericRequest request) {
    // Somewhat Higher and Mostly Dry
    // Disgustin' Junction
    // The Former or the Ladder

    String text = request.responseText;
    int explorations = 0;

    int dumplings = InventoryManager.getAccessibleCount(ItemPool.DUMPLINGS);
    int wads = InventoryManager.getAccessibleCount(ItemPool.SEWER_WAD);
    int oozeo = InventoryManager.getAccessibleCount(ItemPool.OOZE_O);
    int oil = InventoryManager.getAccessibleCount(ItemPool.OIL_OF_OILINESS);
    int umbrella = InventoryManager.getAccessibleCount(ItemPool.GATORSKIN_UMBRELLA);

    // You steel your nerves and descend into the darkened tunnel.
    if (!text.contains("You steel your nerves and descend into the darkened tunnel.")) {
      return;
    }

    // *** CODE TESTS ***

    // You flip through your code binder, and figure out that one
    // of the glyphs is code for 'shortcut', while the others are
    // the glyphs for 'longcut' and 'crewcut', respectively. You
    // head down the 'shortcut' tunnel.

    if (text.contains("'crewcut'")) {
      explorations += 1;
    }

    // You flip through your code binder, and gain a basic
    // understanding of the sign: "This ladder just goes in a big
    // circle. If you climb it you'll end up back where you
    // started." You continue down the tunnel, instead.

    if (text.contains("in a big circle")) {
      explorations += 3;
    }

    // You consult your binder and translate the glyphs -- one of
    // them says "This way to the Great Egress" and the other two
    // are just advertisements for Amalgamated Ladderage, Inc. You
    // head toward the Egress.

    if (text.contains("Amalgamated Ladderage")) {
      explorations += 5;
    }

    // *** ITEM TESTS ***

    // "How about these?" you ask, offering the fish some of your
    // unfortunate dumplings.
    if (text.contains("some of your unfortunate dumplings")) {
      // Remove unfortunate dumplings from inventory
      ResultProcessor.processItem(ItemPool.DUMPLINGS, -1);
      ++explorations;
      dumplings = InventoryManager.getAccessibleCount(ItemPool.DUMPLINGS);
      if (dumplings <= 0) {
        RequestLogger.printLine("That was your last unfortunate dumplings.");
      }
    }

    // Before you can ask him what kind of tribute he wants, you
    // see his eyes light up at the sight of your sewer wad.
    if (text.contains("the sight of your sewer wad")) {
      // Remove sewer wad from inventory
      ResultProcessor.processItem(ItemPool.SEWER_WAD, -1);
      ++explorations;
      wads = InventoryManager.getAccessibleCount(ItemPool.SEWER_WAD);
      if (wads <= 0) {
        RequestLogger.printLine("That was your last sewer wad.");
      }
    }

    // He finds a bottle of Ooze-O, and begins giggling madly. He
    // uncorks the bottle, takes a drink, and passes out in a heap.
    if (text.contains("He finds a bottle of Ooze-O")) {
      // Remove bottle of Ooze-O from inventory
      ResultProcessor.processItem(ItemPool.OOZE_O, -1);
      ++explorations;
      oozeo = InventoryManager.getAccessibleCount(ItemPool.OOZE_O);
      if (oozeo <= 0) {
        RequestLogger.printLine("That was your last bottle of Ooze-O.");
      }
    }

    // You grunt and strain, but you can't manage to get between
    // the bars. In a flash of insight, you douse yourself with oil
    // of oiliness (it takes three whole bottles to cover your
    // entire body) and squeak through like a champagne cork. Only
    // without the bang, and you're not made out of cork, and
    // champagne doesn't usually smell like sewage. Anyway. You
    // continue down the tunnel.
    if (text.contains("it takes three whole bottles")) {
      // Remove 3 bottles of oil of oiliness from inventory
      ResultProcessor.processItem(ItemPool.OIL_OF_OILINESS, -3);
      ++explorations;
      oil = InventoryManager.getAccessibleCount(ItemPool.OIL_OF_OILINESS);
      if (oil < 3) {
        RequestLogger.printLine("You have less than 3 bottles of oil of oiliness left.");
      }
    }

    // Fortunately, your gatorskin umbrella allows you to pass
    // beneath the sewagefall without incident. There's not much
    // left of the umbrella, though, and you discard it before
    // moving deeper into the tunnel.
    if (text.contains("your gatorskin umbrella allows you to pass")) {
      // Unequip gatorskin umbrella and discard it.

      ++explorations;
      AdventureResult item = ItemPool.get(ItemPool.GATORSKIN_UMBRELLA, 1);
      int slot = EquipmentManager.WEAPON;
      if (KoLCharacter.hasEquipped(item, EquipmentManager.WEAPON)) {
        slot = EquipmentManager.WEAPON;
      } else if (KoLCharacter.hasEquipped(item, EquipmentManager.OFFHAND)) {
        slot = EquipmentManager.OFFHAND;
      }

      EquipmentManager.setEquipment(slot, EquipmentRequest.UNEQUIP);

      AdventureResult.addResultToList(KoLConstants.inventory, item);
      ResultProcessor.processItem(ItemPool.GATORSKIN_UMBRELLA, -1);
      umbrella = InventoryManager.getAccessibleCount(item);
      if (umbrella > 0) {
        RequestThread.postRequest(new EquipmentRequest(item, slot));
      } else {
        RequestLogger.printLine("That was your last gatorskin umbrella.");
      }
    }

    // *** GRATE ***

    // Further into the sewer, you encounter a halfway-open grate
    // with a crank on the opposite side. What luck -- looks like
    // somebody else opened this grate from the other side!

    if (text.contains("somebody else opened this grate")) {
      explorations += 5;
    }

    // Now figure out how to say what happened. If the player wants
    // to stop if runs out of test items, generate an ERROR and
    // list the missing items in the status message. Otherwise,
    // simply tell how many explorations were accomplished.

    AdventureResult result =
        AdventureResult.tallyItem("sewer tunnel explorations", explorations, false);
    AdventureResult.addResultToList(KoLConstants.tally, result);

    MafiaState state = MafiaState.CONTINUE;
    String message = "+" + explorations + " Explorations";

    if (Preferences.getBoolean("requireSewerTestItems")) {
      String missing = "";
      String comma = "";

      if (dumplings < 1) {
        missing = missing + comma + "unfortunate dumplings";
        comma = ", ";
      }
      if (wads < 1) {
        missing = missing + comma + "sewer wad";
        comma = ", ";
      }
      if (oozeo < 1) {
        missing = missing + comma + "bottle of Ooze-O";
        comma = ", ";
      }
      if (oil < 1) {
        missing = missing + comma + "oil of oiliness";
        comma = ", ";
      }
      if (umbrella < 1) {
        missing = missing + comma + "gatorskin umbrella";
        comma = ", ";
      }
      if (!missing.equals("")) {
        state = MafiaState.ERROR;
        message += ", NEED: " + missing;
      }
    }

    KoLmafia.updateDisplay(state, message);
  }

  public static String choiceDescription(final int choice, final int decision) {
    // If we have spoilers for this choice, use that
    Spoilers spoilers = ChoiceAdventures.choiceSpoilers(choice, null);
    if (spoilers != null) {
      Option spoiler = ChoiceAdventures.choiceSpoiler(choice, decision, spoilers.getOptions());
      if (spoiler != null) {
        return spoiler.toString();
      }
    }

    // If we didn't find a spoiler, use KoL's label for the option
    Map<Integer, String> choices = ChoiceUtilities.parseChoices(ChoiceManager.lastResponseText);
    String desc = choices.get(decision);

    // If we still can't find it, throw up our hands
    return (desc == null) ? "unknown" : desc;
  }

  public static final boolean registerRequest(final String urlString) {
    int choice = ChoiceUtilities.extractChoiceFromURL(urlString);
    int decision = ChoiceUtilities.extractOptionFromURL(urlString);
    if (choice != 0) {
      switch (choice) {
        case 443:
          // Chess Puzzle
          return RabbitHoleManager.registerChessboardRequest(urlString);

        case 460:
        case 461:
        case 462:
        case 463:
        case 464:
        case 465:
        case 467:
        case 468:
        case 469:
        case 470:
        case 472:
        case 473:
        case 474:
        case 475:
        case 476:
        case 477:
        case 478:
        case 479:
        case 480:
        case 481:
        case 482:
        case 483:
        case 484:
          // Space Trip
        case 471:
          // DemonStar
        case 485:
          // Fighters Of Fighting
        case 486:
          // Dungeon Fist!
        case 488:
        case 489:
        case 490:
        case 491:
          // Meteoid
          return true;

        case 535: // Deep Inside Ronald
        case 536: // Deep Inside Grimace
          return true;

        case 546: // Interview With You
          return true;

        case 1003: // Test Your Might And Also Test Other Things
        case 1015: // The Mirror in the Tower has the View that is True
        case 1020: // Closing Ceremony
        case 1021: // Meet Frank
        case 1022: // Meet Frank
        case 1005: // 'Allo
        case 1006: // One Small Step For Adventurer
        case 1007: // Twisty Little Passages, All Hedge
        case 1008: // Pooling Your Resources
        case 1009: // Good Ol' 44% Duck
        case 1010: // Another Day, Another Fork
        case 1011: // Of Mouseholes and Manholes
        case 1012: // The Last Temptation
        case 1013: // Mazel Tov!
          return SorceressLairManager.registerChoice(choice, urlString);

        case 1053: // The Servants' Quarters
          return EdServantData.registerRequest(urlString);

        case 1063: // Adjust your 'Edpiece
          {
            int index = decision - 1;
            if (index < 0 || index > EdPieceCommand.ANIMAL.length) {
              // Doing nothing
              return true;
            }
            String decoration = EdPieceCommand.ANIMAL[index][0];
            RequestLogger.updateSessionLog();
            RequestLogger.updateSessionLog("edpiece " + decoration);
            return true;
          }

        case 1101: // It's a Barrel Smashing Party!
          {
            if (decision == 2) {
              // We're smashing 100 barrels
              // The results don't say which barrels are being smashed, but it seems to happen in
              // item order
              int count = 100;
              int itemId = ItemPool.LITTLE_FIRKIN;
              RequestLogger.updateSessionLog("smashing 100 barrels");
              while (count > 0 && itemId <= ItemPool.BARNACLED_BARREL) {
                int smashNumber = Math.min(count, InventoryManager.getCount(itemId));
                String name = ItemDatabase.getItemName(itemId);
                if (smashNumber > 0 && name != null) {
                  RequestLogger.updateSessionLog("smash " + smashNumber + " " + name);
                  count -= smashNumber;
                }
                itemId++;
              }
              return true;
            }
            int itemId = extractIidFromURL(urlString);
            String name = ItemDatabase.getItemName(itemId);
            if (name != null) {
              RequestLogger.updateSessionLog("smash " + name);
              return true;
            }
            break;
          }

        case 1181: // Your Witchess Set
          {
            String desc = ChoiceManager.choiceDescription(choice, decision);
            RequestLogger.updateSessionLog("Took choice " + choice + "/" + decision + ": " + desc);
            return true;
          }

        case 1182: // Play against the Witchess Pieces
          // These will redirect to a fight. The encounter will suffice.
          if (decision >= 1 && decision <= 7) {
            String desc = "Play against the Witchess pieces";
            RequestLogger.updateSessionLog("Took choice " + choice + "/" + decision + ": " + desc);
          }
          return true;

        case 1313: // Bastille Battalion
        case 1314: // Bastille Battalion (Master of None)
        case 1315: // Castle vs. Castle
        case 1316: // GAME OVER
        case 1317: // A Hello to Arms (Battalion)
        case 1318: // Defensive Posturing
        case 1319: // Cheese Seeking Behavior
          return BastilleBattalionManager.registerRequest(urlString);

        case 1334: // Boxing Daycare (Lobby)
        case 1335: // Boxing Daycare Spa
        case 1336: // Boxing Daycare
          // Special logging done elsewhere
          return true;

        case 1339: // A Little Pump and Grind
          if (decision == 1) {
            int itemId = extractIidFromURL(urlString);
            int qty = extractQtyFromURL(urlString);
            String name = ItemDatabase.getItemName(itemId);
            if (name != null) {
              RequestLogger.updateSessionLog("grinding " + qty + " " + name);
              return true;
            }
          }
          return true;

        case 1352: //  Island #1, Who Are You?
        case 1353: //  What's Behind Island #2?
        case 1354: //  Third Island's the Charm
          {
            String desc = ChoiceManager.choiceDescription(choice, decision);
            RequestLogger.updateSessionLog("Took choice " + choice + "/" + decision + ": " + desc);
            if (desc != null && !desc.equals("Decide Later")) {
              Preferences.setString("_LastPirateRealmIsland", desc);
            }
            return true;
          }

        case 1388: // Comb the Beach
          {
            return BeachCombRequest.registerRequest(urlString);
          }

        case 1445: // Reassembly Station
        case 1447: // Statbot 5000
          return YouRobotManager.registerRequest(urlString);
      }

      if (decision != 0) {
        // Figure out which decision we took
        String desc = ChoiceManager.choiceDescription(choice, decision);
        RequestLogger.updateSessionLog("Took choice " + choice + "/" + decision + ": " + desc);
      }
    } else if (decision == 0) {
      // forceoption=0 will redirect to the real choice.
      // Don't bother logging it.
      return true;
    }

    // By default, we log the url of any choice we take
    RequestLogger.updateSessionLog(urlString);

    switch (choice) {
      case 1195:
        if (decision == 3) {
          KoLAdventure.lastVisitedLocation = null;
          KoLAdventure.lastLocationName = null;
          KoLAdventure.lastLocationURL = urlString;
          KoLAdventure.setLastAdventure("None");
          KoLAdventure.setNextAdventure("None");
          GenericRequest.itemMonster = "Time-Spinner";

          String message = "[" + KoLAdventure.getAdventureCount() + "] Way Back in Time";
          RequestLogger.printLine();
          RequestLogger.printLine(message);

          RequestLogger.updateSessionLog();
          RequestLogger.updateSessionLog(message);
        }
        return true;

      case 1196:
        if (ChoiceManager.lastDecision == 1 && !urlString.contains("monid=0")) {
          KoLAdventure.lastVisitedLocation = null;
          KoLAdventure.lastLocationName = null;
          KoLAdventure.lastLocationURL = urlString;
          KoLAdventure.setLastAdventure("None");
          KoLAdventure.setNextAdventure("None");
          GenericRequest.itemMonster = "Time-Spinner";

          String message = "[" + KoLAdventure.getAdventureCount() + "] A Recent Fight";
          RequestLogger.printLine();
          RequestLogger.printLine(message);

          RequestLogger.updateSessionLog();
          RequestLogger.updateSessionLog(message);
        }
        return true;
    }

    // Special cases
    if (choice == 879 && decision == 3) {
      // One Rustic Nightstand
      //
      // Option 3 redirects to a fight with the remains of a
      // jilted mistress. Unlike other such redirections,
      // this takes a turn.
      RequestLogger.registerLocation("The Haunted Bedroom");
    }

    return true;
  }

  public static final void registerDeferredChoice(final int choice, final String encounter) {
    switch (choice) {
      case 123: // At Least It's Not Full Of Trash
        RequestLogger.registerLocation("The Hidden Temple");
        break;

      case 125: // No Visible Means of Support
        // The tiles took a turn to get here
        ResultProcessor.processAdventuresUsed(1);
        RequestLogger.registerLocation("The Hidden Temple");
        break;

      case 437: // Flying In Circles
        ResultProcessor.processAdventuresUsed(1);
        RequestLogger.registerLocation("The Nemesis' Lair");
        break;

      case 620: // A Blow Is Struck!
      case 621: // Hold the Line!
      case 622: // The Moment of Truth
      case 634: // Goodbye Fnord
        // These all arise out of a multifight, rather than by
        // visiting a location.
        RequestLogger.registerLastLocation();
        break;

      case 1005: // 'Allo
      case 1006: // One Small Step For Adventurer
      case 1007: // Twisty Little Passages, All Hedge
      case 1008: // Pooling Your Resources
      case 1009: // Good Ol' 44% Duck
      case 1010: // Another Day, Another Fork
      case 1011: // Of Mouseholes and Manholes
      case 1012: // The Last Temptation
      case 1013: // Mazel Tov!
        // This is chain of choices that either immediately
        // follow a fight or the previous choice, either of
        // which takes a turn (unlike normal choice chains)
        String location = "The Hedge Maze (Room " + (choice - 1004) + ")";
        RequestLogger.registerLocation(location);
        break;

      case 1018: // Bee Persistent
      case 1019: // Bee Rewarded
        // Getting here took a turn
        ResultProcessor.processAdventuresUsed(1);
        RequestLogger.registerLocation("The Black Forest");
        break;

      case 1223: // L.O.V. Entrance
      case 1224: // L.O.V. Equipment Room
      case 1225: // L.O.V. Engine Room
      case 1226: // L.O.V. Emergency Room
      case 1227: // L.O.V. Elbow Room
      case 1228: // L.O.V. Emporium
        // This is chain of choices that either immediately
        // follow a fight or the previous choice.
        RequestLogger.registerLastLocation();
        break;

      case 1310: // Granted a Boon
        {
          // Boon after fight, location is currently null, so don't log under that name
          String message = "[" + KoLAdventure.getAdventureCount() + "] God Lobster";
          RequestLogger.printLine();
          RequestLogger.printLine(message);

          RequestLogger.updateSessionLog();
          RequestLogger.updateSessionLog(message);
          break;
        }

      case 1334: // Boxing Daycare (Lobby)
      case 1335: // Boxing Daycare Spa
      case 1336: // Boxing Daycare
        RequestLogger.registerLocation("Boxing Daycare");
        break;
    }
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

  private static Option[] oldManPsychosisSpoilers() {
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
    Option[] spoilers = new Option[4];

    for (int j = 0; j < spoilers.length; j++) {
      for (String[] s : OLD_MAN_PSYCHOSIS_SPOILERS) {
        if (s[0].equals(buttons[j][1])) {
          spoilers[Integer.parseInt(buttons[j][0]) - 1] =
              new Option(s[1]); // button 1 text should be in index 0, 2 -> 1, etc.
          break;
        }
      }
    }

    return spoilers;
  }

  private static final Pattern CINDERELLA_TIME_PATTERN =
      Pattern.compile("<i>It is (\\d+) minute(?:s) to midnight.</i>");

  private static boolean parseCinderellaTime() {
    Matcher matcher = CINDERELLA_TIME_PATTERN.matcher(ChoiceManager.lastResponseText);
    while (matcher.find()) {
      int time = StringUtilities.parseInt(matcher.group(1));
      if (time != -1) {
        Preferences.setInteger("cinderellaMinutesToMidnight", time);
        return true;
      }
    }
    return false;
  }

  private static final Pattern FLUENCY_PATTERN = Pattern.compile("Fluency is now (\\d+)%");

  public static void parseLanguageFluency(final String text, final String setting) {
    Matcher m = FLUENCY_PATTERN.matcher(text);
    if (m.find()) {
      Preferences.setInteger(setting, StringUtilities.parseInt(m.group(1)));
    }
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

  public static boolean canWalkFromChoice(int choice) {
    switch (choice) {
      case 570: // GameInformPowerDailyPro Walkthru
      case 603: // Skeletons and The Closet
      case 627: // ChibiBuddy&trade; (on)
      case 632: // Add an E-Mail Address
      case 633: // ChibiBuddy&trade; (off)
      case 664: // The Crackpot Mystic's Shed
      case 720: // The Florist Friar's Cottage
      case 767: // Tales of Dread
      case 769: // The Super-Secret Canadian Mind-Control Device
      case 770: // The Institute for Canadian Studies
      case 774: // Opening up the Folder Holder
      case 792: // The Degrassi Knoll Gym
      case 793: // Welcome to The Shore, Inc.
      case 801: // A Reanimated Conversation
      case 804: // Trick or Treat!
      case 810: // K.R.A.M.P.U.S. facility
      case 812: // The Unpermery
      case 821: // LP-ROM burner
      case 835: // Barely Tales
      case 836: // Adventures Who Live in Ice Houses...
      case 844: // The Portal to Horrible Parents
      case 845: // Rumpelstiltskin's Workshop
      case 859: // Upping your grade
      case 867: // Sneaky Peterskills
      case 870: // Hair Today
      case 871: // inspecting Motorbike
      case 872: // Drawn Onward
      case 922: // Summoning Chamber
      case 929: // Control Freak
      case 984: // A Radio on a Beach
      case 985: // The Odd Jobs Board
      case 986: // Control Panel
      case 987: // The Post-Apocalyptic Survivor Encampment
      case 991: // Build a Crimbot!
      case 994: // Hide a Gift!
      case 999: // Shrubberatin'
      case 1003: // Contest Booth
      case 1025: // Reconfigure your Mini-Crimbot
      case 1051: // The Book of the Undying
      case 1053: // The Servants' Quarters
      case 1059: // Helping Make Ends Meat
      case 1063: // Adjust your 'Edpiece
      case 1064: // The Doctor is Out.  Of Herbs.
      case 1065: // Lending a Hand (and a Foot)
      case 1066: // Employee Assignment Kiosk
      case 1067: // Maint Misbehavin'
      case 1076: // Mayo Minder&trade;
      case 1089: // Community Service
      case 1090: // The Towering Inferno Discotheque
      case 1093: // The WLF Bunker
      case 1099: // The Barrel Full of Barrels
      case 1100: // Pray to the Barrel God
      case 1101: // It's a Barrel Smashing Party!
      case 1103: // Doing the Maths
      case 1104: // Tree Tea
      case 1105: // Specifici Tea
      case 1110: // Spoopy
      case 1114: // Walford Rusley, Bucket Collector
      case 1118: // X-32-F Combat Training Snowman Control Console
      case 1171: // LT&T Office
      case 1177: // Book of the West: Cow Punching
      case 1178: // Book of the West: Beanslinging
      case 1179: // Book of the West: Snake Oiling
      case 1181: // Witchess Set
      case 1188: // The Call is Coming from Outside the Simulation
      case 1190: // The Oracle
      case 1191: // Source Terminal
      case 1193: // The Precinct
      case 1195: // Spinning Your Time-Spinner
      case 1197: // Travel back to a Delicious Meal
      case 1217: // Sweet Synthesis
      case 1218: // Wax On
      case 1233: // Equipment Requisition
      case 1234: // Spacegate Vaccination Machine
      case 1235: // Spacegate Terminal
      case 1259: // LI-11 HQ
      case 1264: // Meteor Metal Machinations
      case 1266: // The Hostler
      case 1267: // Rubbed it the Right Way
      case 1270: // Pantagramming
      case 1271: // Mummery
      case 1272: // R&D
      case 1273: // The Cursed Warehouse
      case 1275: // Rummaging through the Garbage
      case 1277: // Extra, Extra
      case 1278: // Madame Zatara’s Relationship Fortune Teller
      case 1320: // A Heist!
      case 1322: // The Beginning of the Neverend
      case 1329: // Latte Shop
      case 1334: // Boxing Daycare (Lobby)
      case 1335: // Boxing Day Spa
      case 1336: // Boxing Daycare
      case 1339: // A Little Pump and Grind
      case 1389: // The Council of Exploathing
      case 1395: // Take your Pills
      case 1396: // Adjusting Your Fish
      case 1407: // Mushroom District Costume Shop
      case 1408: // Mushroom District Badge Shop
      case 1420: // What has it got in its pocketses?
      case 1437: // Setup Your knock-off retro superhero cape
      case 1438: // Setup your knock-off retro superhero cape
      case 1439: // Spread Crimbo Spirit
      case 1445: // Reassembly Station
      case 1447: // Statbot 5000
      case 1448: // Potted Power Plant
      case 1449: // Set Backup Camera Mode
      case 1451: // Fire Captain Hagnk
      case 1452: // Sprinkler Joe
      case 1453: // Fracker Dan
      case 1454: // Cropduster Dusty
      case 1455: // Cold Medicine Cabinet
      case 1457: // Food Lab
      case 1458: // Booze Lab
      case 1459: // Chem Lab
      case 1460: // Toy Lab
      case 1463: // Reminiscing About Those Monsters You Fought
        return true;

      default:
        return false;
    }
  }
}
