package net.sourceforge.kolmafia.session;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.request.CharSheetRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class GreyYouManager {

  // Every monster you absorb is listed on the Charsheet

  public static final Set<MonsterData> absorbedMonsters = new HashSet<>();

  public static void refreshAbsorptions() {
    CharSheetRequest request = new CharSheetRequest();
    request.run();
  }

  // Absorbed 5 adventures from a warwelf.<!-- 199 -->
  private static final Pattern ABSORPTION_PATTERN =
      Pattern.compile("Absorbed (.*?) from an? (.*?)\\.<!-- ([\\d]+) -->", Pattern.DOTALL);

  public static void parseAbsorptions(String responseText) {
    absorbedMonsters.clear();
    if (!responseText.contains("Absorptions:")) {
      return;
    }
    Matcher matcher = ABSORPTION_PATTERN.matcher(responseText);
    while (matcher.find()) {
      int monsterId = StringUtilities.parseInt(matcher.group(3));
      MonsterData monster = MonsterDatabase.findMonsterById(monsterId);
      absorbedMonsters.add(monster);
    }
  }

  // When Grey You absorbs a monster, you gain stats.  Each zone has monsters
  // that grant an additional boon the first time you absorb them. They can
  // grant you immediate adventures or extra stats or a new skill.

  public enum AbsorptionType {
    SKILL("Skill"),
    ADVENTURES("Adventures"),
    MUSCLE("Muscle"),
    MYSTICALITY("Mysticality"),
    MOXIE("Moxie"),
    MAX_HP("Max HP"),
    MAX_MP("Max MP");

    String name;

    AbsorptionType(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  // Each monster gives at most one absorption.
  // Here is a map from monster => Absorption
  public static final Map<MonsterData, Absorption> allAbsorptions = new HashMap<>();

  // This is the class which associates a monster with a particular type of absorption

  public abstract static class Absorption {
    protected final AbsorptionType type;
    protected final MonsterData monster;
    protected final String zone;

    public Absorption(final AbsorptionType type, final String monsterName) {
      this.type = type;
      this.monster = MonsterDatabase.findMonster(monsterName);
      List<String> zones = AdventureDatabase.getAreasWithMonster(this.monster);
      this.zone = (zones.size() > 0) ? zones.get(0) : "";
      allAbsorptions.put(this.monster, this);
    }

    public AbsorptionType getType() {
      return this.type;
    }

    public String getMonsterName() {
      return (this.monster == null) ? "" : this.monster.getName();
    }

    public String getMonsterZone() {
      return (this.zone == null) ? "" : this.zone;
    }

    public abstract boolean haveAbsorbed();

    @Override
    public abstract String toString();
  }

  // This is the class which associates a monster with a non-skill absorption

  public static class GooAbsorption extends Absorption {
    private final int value;
    private final String stringValue;

    public GooAbsorption(final AbsorptionType type, final String monsterName, int value) {
      super(type, monsterName);
      this.value = value;

      StringBuilder string = new StringBuilder();
      switch (type) {
        case ADVENTURES:
          string.append("+");
          string.append(value);
          string.append(" Adventures");
          break;
        case MUSCLE:
        case MYSTICALITY:
        case MOXIE:
        case MAX_HP:
        case MAX_MP:
          string.append(type);
          string.append(" +");
          string.append(value);
          break;
      }
      this.stringValue = string.toString();
    }

    public int getValue() {
      return this.value;
    }

    @Override
    public boolean haveAbsorbed() {
      return absorbedMonsters.contains(this.monster);
    }

    @Override
    public String toString() {
      return this.stringValue;
    }
  }

  public static final GooAbsorption[] GOO_ABSORPTIONS = {
    new GooAbsorption(AbsorptionType.ADVENTURES, "albino bat", 5),
    new GooAbsorption(AbsorptionType.ADVENTURES, "batrat", 5),
    new GooAbsorption(AbsorptionType.ADVENTURES, "dire pigeon", 5),
    new GooAbsorption(AbsorptionType.ADVENTURES, "G imp", 5),
    new GooAbsorption(AbsorptionType.ADVENTURES, "gingerbread murderer", 5),
    new GooAbsorption(AbsorptionType.ADVENTURES, "grave rober", 5),
    new GooAbsorption(AbsorptionType.ADVENTURES, "irate mariachi", 5),
    new GooAbsorption(AbsorptionType.ADVENTURES, "Knob Goblin Bean Counter", 5),
    new GooAbsorption(AbsorptionType.ADVENTURES, "Knob Goblin Madam", 5),
    new GooAbsorption(AbsorptionType.ADVENTURES, "Knob Goblin Master Chef", 5),
    new GooAbsorption(AbsorptionType.ADVENTURES, "L imp", 5),
    new GooAbsorption(AbsorptionType.ADVENTURES, "magical fruit bat", 5),
    new GooAbsorption(AbsorptionType.ADVENTURES, "P imp", 5),
    new GooAbsorption(AbsorptionType.ADVENTURES, "plastered frat orc", 5),
    new GooAbsorption(AbsorptionType.ADVENTURES, "swarm of Knob lice", 5),
    new GooAbsorption(AbsorptionType.ADVENTURES, "swarm of skulls", 5),
    new GooAbsorption(AbsorptionType.ADVENTURES, "W imp", 5),
    new GooAbsorption(AbsorptionType.ADVENTURES, "warwelf", 5),
    new GooAbsorption(AbsorptionType.ADVENTURES, "animated rustic nightstand", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "basic lihc", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "Battlie Knight Ghost", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "Booze Giant", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "Bubblemint Twins", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "CH Imp", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "chalkdust wraith", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "cloud of disembodied whiskers", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "eXtreme Orcish snowboarder", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "gluttonous ghuol", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "Grass Elemental", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "grave rober smobie", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "guy with a pitchfork, and his wife", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "junksprite sharpener", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "Knob Goblin Very Mad Scientist", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "model skeleton", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "Ninja Snowman Janitor", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "oil baron", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "party skelteon", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "possessed silverware drawer", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "possessed toy chest", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "revolving bugbear", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "sabre-toothed goat", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "serialbus", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "sheet ghost", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "skeletal hamster", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "smut orc pipelayer", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "swarm of killer bees", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "tapdancing skeleton", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "toilet papergeist", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "upgraded ram", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "vicious gnauga", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "whitesnake", 7),
    new GooAbsorption(AbsorptionType.ADVENTURES, "1335 HaXx0r", 10),
    new GooAbsorption(AbsorptionType.ADVENTURES, "Alphabet Giant", 10),
    new GooAbsorption(AbsorptionType.ADVENTURES, "black magic woman", 10),
    new GooAbsorption(AbsorptionType.ADVENTURES, "blur", 10),
    new GooAbsorption(AbsorptionType.ADVENTURES, "Bob Racecar", 10),
    new GooAbsorption(AbsorptionType.ADVENTURES, "coaltergeist", 10),
    new GooAbsorption(AbsorptionType.ADVENTURES, "fleet woodman", 10),
    new GooAbsorption(AbsorptionType.ADVENTURES, "Iiti Kitty", 10),
    new GooAbsorption(AbsorptionType.ADVENTURES, "Irritating Series of Random Encounters", 10),
    new GooAbsorption(AbsorptionType.ADVENTURES, "Little Man in a Canoe", 10),
    new GooAbsorption(AbsorptionType.ADVENTURES, "mad wino", 10),
    new GooAbsorption(AbsorptionType.ADVENTURES, "Mob Penguin Capo", 10),
    new GooAbsorption(AbsorptionType.ADVENTURES, "One-Eyed Willie", 10),
    new GooAbsorption(AbsorptionType.ADVENTURES, "pygmy blowgunner", 10),
    new GooAbsorption(AbsorptionType.ADVENTURES, "pygmy headhunter", 10),
    new GooAbsorption(AbsorptionType.ADVENTURES, "pygmy orderlies", 10),
    new GooAbsorption(AbsorptionType.ADVENTURES, "pygmy shaman", 10),
    new GooAbsorption(AbsorptionType.ADVENTURES, "Racecar Bob", 10),
    new GooAbsorption(AbsorptionType.ADVENTURES, "Raver giant", 10),
    new GooAbsorption(AbsorptionType.ADVENTURES, "Renaissance Giant", 10),
    new GooAbsorption(AbsorptionType.ADVENTURES, "swarm of fire ants", 10),
    new GooAbsorption(AbsorptionType.ADVENTURES, "tomb asp", 10),
    new GooAbsorption(AbsorptionType.MUSCLE, "stone temple pirate", 3),
    new GooAbsorption(AbsorptionType.MUSCLE, "Burly Sidekick", 5),
    new GooAbsorption(AbsorptionType.MUSCLE, "Knob Goblin Mutant", 5),
    new GooAbsorption(AbsorptionType.MUSCLE, "sleeping Knob Goblin Guard", 5),
    new GooAbsorption(AbsorptionType.MUSCLE, "angry bugbear", 10),
    new GooAbsorption(AbsorptionType.MUSCLE, "Fallen Archfiend", 10),
    new GooAbsorption(AbsorptionType.MUSCLE, "Fitness Giant", 10),
    new GooAbsorption(AbsorptionType.MUSCLE, "toothy sklelton", 10),
    new GooAbsorption(AbsorptionType.MYSTICALITY, "baa-relief sheep", 3),
    new GooAbsorption(AbsorptionType.MYSTICALITY, "fiendish can of asparagus", 5),
    new GooAbsorption(AbsorptionType.MYSTICALITY, "Quiet Healer", 5),
    new GooAbsorption(AbsorptionType.MYSTICALITY, "Blue Oyster Cultist", 10),
    new GooAbsorption(AbsorptionType.MYSTICALITY, "bookbat", 10),
    new GooAbsorption(AbsorptionType.MYSTICALITY, "forest spirit", 10),
    new GooAbsorption(AbsorptionType.MYSTICALITY, "Hellion", 10),
    new GooAbsorption(AbsorptionType.MYSTICALITY, "Possibility Giant", 10),
    new GooAbsorption(AbsorptionType.MYSTICALITY, "senile lihc", 10),
    new GooAbsorption(AbsorptionType.MYSTICALITY, "tomb servant", 10),
    new GooAbsorption(AbsorptionType.MOXIE, "craven carven raven", 3),
    new GooAbsorption(AbsorptionType.MOXIE, "drunken half-orc hobo", 5),
    new GooAbsorption(AbsorptionType.MOXIE, "hung-over half-orc hobo", 5),
    new GooAbsorption(AbsorptionType.MOXIE, "sassy pirate", 5),
    new GooAbsorption(AbsorptionType.MOXIE, "Spunky Princess", 5),
    new GooAbsorption(AbsorptionType.MOXIE, "demoninja", 10),
    new GooAbsorption(AbsorptionType.MOXIE, "gaunt ghuol", 10),
    new GooAbsorption(AbsorptionType.MOXIE, "Gnefarious gnome", 10),
    new GooAbsorption(AbsorptionType.MOXIE, "Punk Rock Giant", 10),
    new GooAbsorption(AbsorptionType.MOXIE, "swarm of scarab beatles", 10),
    new GooAbsorption(AbsorptionType.MAX_HP, "fluffy bunny", 5),
    new GooAbsorption(AbsorptionType.MAX_HP, "bodyguard bat", 10),
    new GooAbsorption(AbsorptionType.MAX_HP, "vampire bat", 10),
    new GooAbsorption(AbsorptionType.MAX_HP, "corpulent zobmie", 20),
    new GooAbsorption(AbsorptionType.MAX_MP, "Zol", 5),
    new GooAbsorption(AbsorptionType.MAX_MP, "7-Foot Dwarf", 10),
    new GooAbsorption(AbsorptionType.MAX_MP, "plaque of locusts", 10),
  };

  // This is the class which associates a monster with a skill absorption

  public static class GooSkill extends Absorption {
    private final int skillId;

    private final String name;
    private final int skillType;
    private final String skillTypeName;
    private final long mpCost;

    private final String enchantments;
    private final String modsLookup;

    public GooSkill(final int skillId, final String monsterName) {
      // This is for passive skills; we will look up the enchantments
      this(skillId, monsterName, "");
    }

    public GooSkill(final int skillId, final String monsterName, final String effects) {
      super(AbsorptionType.SKILL, monsterName);

      this.skillId = skillId;
      this.name = SkillDatabase.getSkillName(skillId);
      this.skillType = SkillDatabase.getSkillType(skillId);
      this.skillTypeName = SkillDatabase.getSkillTypeName(skillId);

      Modifiers mods = null;
      if (this.skillType == SkillDatabase.PASSIVE) {
        mods = Modifiers.getModifiers("Skill", this.name);
      }

      if (mods != null) {
        this.enchantments = mods.getString("Modifiers");
        this.modsLookup = mods.getName();
      } else {
        this.enchantments = effects;
        this.modsLookup = "";
      }

      this.mpCost =
          (this.skillType != SkillDatabase.PASSIVE)
              ? SkillDatabase.getMPConsumptionById(skillId)
              : 0;
    }

    @Override
    public boolean haveAbsorbed() {
      UseSkillRequest skill = UseSkillRequest.getUnmodifiedInstance(this.skillId);
      return KoLCharacter.hasSkill(skill, KoLConstants.availableSkills)
          || KoLCharacter.hasSkill(skill, KoLConstants.availableCombatSkills);
    }

    public int getSkillId() {
      return this.skillId;
    }

    public String getName() {
      return this.name;
    }

    public int getSkillType() {
      return this.skillType;
    }

    public String getSkillTypeName() {
      return this.skillTypeName;
    }

    public long getMPCost() {
      return this.mpCost;
    }

    public String getEnchantments() {
      return this.enchantments;
    }

    public String getEvaluatedEnchantments() {
      if (this.modsLookup.equals("")) {
        return this.enchantments;
      }
      return Modifiers.evaluateModifiers(this.modsLookup, this.enchantments).toString();
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  // *** Here are all the skills you can absorb from monsters

  public static final GooSkill[] GOO_SKILLS = {
    new GooSkill(SkillPool.PSEUDOPOD_SLAP, "", "Deals 10 damage"),
    new GooSkill(SkillPool.HARDSLAB, "remaindered skeleton", "Deals Mus in physical damage"),
    new GooSkill(SkillPool.TELEKINETIC_MURDER, "cr&ecirc;ep", "Deals Mys in physical damage"),
    new GooSkill(
        SkillPool.SNAKESMACK,
        "sewer snake with a sewer snake in it",
        "Deals Mox in physical damage"),
    new GooSkill(SkillPool.IRE_PROOF, "raging bull"),
    new GooSkill(SkillPool.NANOFUR, "ratbat"),
    new GooSkill(SkillPool.AUTOVAMPIRISM_ROUTINES, "spooky vampire"),
    new GooSkill(SkillPool.CONIFER_POLYMERS, "pine bat"),
    new GooSkill(SkillPool.ANTI_SLEAZE_RECURSION, "werecougar"),
    new GooSkill(SkillPool.MICROBURNER, "Cobb's Knob oven"),
    new GooSkill(SkillPool.CRYOCURRENCY, "Knob Goblin MBA"),
    new GooSkill(SkillPool.CURSES_LIBRARY, "lihc"),
    new GooSkill(SkillPool.EXHAUST_TUBULES, "beanbat"),
    new GooSkill(SkillPool.CAMP_SUBROUTINES, "Knob Goblin Harem Girl"),
    new GooSkill(SkillPool.GREY_NOISE, "Boss Bat", "Deals 5 damage + bonus elemental damage"),
    new GooSkill(SkillPool.ADVANCED_EXO_ALLOY, "Knob Goblin Elite Guard"),
    new GooSkill(SkillPool.LOCALIZED_VACUUM, "cubist bull"),
    new GooSkill(SkillPool.MICROWEAVE, "eXtreme cross-country hippy"),
    new GooSkill(SkillPool.ECTOGENESIS, "Claybender Sorcerer Ghost"),
    new GooSkill(SkillPool.CLAMMY_MICROCILIA, "malevolent hair clog"),
    new GooSkill(SkillPool.LUBRICANT_LAYER, "oil slick"),
    new GooSkill(SkillPool.INFERNAL_AUTOMATA, "demonic icebox"),
    new GooSkill(SkillPool.COOLING_TUBULES, "Ninja Snowman Weaponmaster"),
    new GooSkill(SkillPool.OMINOUS_SUBSTRATE, "animated ornate nightstand"),
    new GooSkill(SkillPool.SECONDARY_FERMENTATION, "drunk goat"),
    new GooSkill(SkillPool.PROCGEN_RIBALDRY, "smut orc screwer"),
    new GooSkill(SkillPool.SOLID_FUEL, "Knob Goblin Alchemist"),
    new GooSkill(SkillPool.AUTOCHRONY, "zombie waltzers"),
    new GooSkill(SkillPool.TEMPORAL_HYPEREXTENSION, "Pr Imp"),
    new GooSkill(SkillPool.PROPAGATION_DRIVE, "junksprite bender"),
    new GooSkill(SkillPool.FINANCIAL_SPREADSHEETS, "me4t begZ0r"),
    new GooSkill(
        SkillPool.PHASE_SHIFT, "Spectral Jellyfish", "10 turns of Shifted Phase (Combat Rate -10)"),
    new GooSkill(
        SkillPool.PIEZOELECTRIC_HONK, "white lion", "10 turns of Hooooooooonk! (Combat Rate +10)"),
    new GooSkill(SkillPool.OVERCLOCKING, "Big Wheelin' Twins"),
    new GooSkill(SkillPool.SUBATOMIC_HARDENING, "pooltergeist"),
    new GooSkill(SkillPool.GRAVITATIONAL_COMPRESSION, "suckubus"),
    new GooSkill(SkillPool.HIVEMINDEDNESS, "mind flayer"),
    new GooSkill(SkillPool.PONZI_APPARATUS, "anglerbush"),
    new GooSkill(SkillPool.FLUID_DYNAMICS_SIMULATION, "Carnivorous Moxie Weed"),
    new GooSkill(SkillPool.NANTLERS, "stuffed moose head", "Deals Mus in damage + bonus damage"),
    new GooSkill(SkillPool.NANOSHOCK, "Jacob's adder", "Deals Mys in damage + bonus damage"),
    new GooSkill(SkillPool.AUDIOCLASM, "spooky music box", "Deals Mox in damage + bonus damage"),
    new GooSkill(
        SkillPool.SYSTEM_SWEEP, "pygmy janitor", "Deals Mus in physical damage & banish on win"),
    new GooSkill(
        SkillPool.DOUBLE_NANOVISION,
        "drunk pygmy",
        "Deals Mys in physical damage & +100% Item Drop on win"),
    new GooSkill(
        SkillPool.INFINITE_LOOP,
        "pygmy witch lawyer",
        "Deals Mus in physical damage & +3 exp on win"),
    new GooSkill(
        SkillPool.PHOTONIC_SHROUD,
        "black panther",
        "10 turns of Darkened Photons (Combat Rate -10)"),
    // new GooSkill(SkillPool.UNUSED_COMBAT_SELFBUFF, ""),
    new GooSkill(SkillPool.STEAM_MYCELIA, "steam elemental"),
    new GooSkill(SkillPool.SNOW_COOLING_SYSTEM, "Snow Queen"),
    new GooSkill(SkillPool.LEGACY_CODE, "possessed wine rack"),
    new GooSkill(SkillPool.AUTOEXEC_BAT, "Flock of Stab-bats"),
    new GooSkill(SkillPool.INNUENDO_CIRCUITRY, "Astronomer"),
    new GooSkill(SkillPool.SUBATOMIC_TANGO, "fan dancer"),
    new GooSkill(SkillPool.EXTRA_INNINGS, "baseball bat"),
    new GooSkill(SkillPool.RELOADING, "Bullet Bill"),
    new GooSkill(SkillPool.HARRIED, "rushing bum"),
    new GooSkill(SkillPool.TEMPORAL_BENT, "undead elbow macaroni"),
    new GooSkill(SkillPool.PROVABLY_EFFICIENT, "Sub-Assistant Knob Mad Scientist"),
    new GooSkill(SkillPool.BASIC_IMPROVEMENTS, "BASIC Elemental"),
    new GooSkill(SkillPool.SHIFTED_ABOUT, "shifty pirate"),
    new GooSkill(SkillPool.SPOOKY_VEINS, "ghost miner"),
    new GooSkill(SkillPool.SEVEN_FOOT_FEELINGS, "dopey 7-Foot Dwarf"),
    new GooSkill(SkillPool.SELF_ACTUALIZED, "banshee librarian"),
  };

  // *** GooSkills can be sorted in various ways.

  private static final Comparator<GooSkill> idComparator = new IdComparator();
  private static final Comparator<GooSkill> nameComparator = new NameComparator();
  private static final Comparator<GooSkill> monsterComparator = new MonsterComparator();
  private static final Comparator<GooSkill> zoneComparator = new ZoneComparator();

  public static GooSkill[] sortGooSkills(String order) {
    Comparator<GooSkill> comparator =
        order.equals("id")
            ? idComparator
            : order.equals("name")
                ? nameComparator
                : order.equals("monster")
                    ? monsterComparator
                    : order.equals("zone") ? zoneComparator : null;

    if (comparator == null) {
      return GOO_SKILLS;
    }

    GooSkill[] skills = Arrays.copyOf(GOO_SKILLS, GOO_SKILLS.length);
    Arrays.sort(skills, comparator);

    return skills;
  }

  private static class IdComparator implements Comparator<GooSkill> {
    @Override
    public int compare(GooSkill o1, GooSkill o2) {
      if (o1 == null || o2 == null) {
        throw new NullPointerException();
      }

      return o1.skillId - o2.skillId;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof IdComparator;
    }
  }

  private static class NameComparator implements Comparator<GooSkill> {
    @Override
    public int compare(GooSkill o1, GooSkill o2) {
      if (o1 == null || o2 == null) {
        throw new NullPointerException();
      }

      return o1.name.compareToIgnoreCase(o2.name);
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof NameComparator;
    }
  }

  private static class MonsterComparator implements Comparator<GooSkill> {
    @Override
    public int compare(GooSkill o1, GooSkill o2) {
      if (o1 == null || o2 == null) {
        throw new NullPointerException();
      }

      return o1.getMonsterName().compareTo(o2.getMonsterName());
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof MonsterComparator;
    }
  }

  private static class ZoneComparator implements Comparator<GooSkill> {
    @Override
    public int compare(GooSkill o1, GooSkill o2) {
      if (o1 == null || o2 == null) {
        throw new NullPointerException();
      }

      return o1.getMonsterZone().compareTo(o2.getMonsterZone());
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof ZoneComparator;
    }
  }

  // This is a map from zone to set of absorptions
  public static Map<String, Set<Absorption>> zoneAbsorptions = new TreeMap<>();

  static {
    for (Absorption absorption : allAbsorptions.values()) {
      String zone = absorption.getMonsterZone();
      if (zone.equals("")) {
        continue;
      }
      Set<Absorption> set = zoneAbsorptions.get(zone);
      if (set == null) {
        set = new HashSet<Absorption>();
        zoneAbsorptions.put(zone, set);
      }
      set.add(absorption);
    }
  }
}
