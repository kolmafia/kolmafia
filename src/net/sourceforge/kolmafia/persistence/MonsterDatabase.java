package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.MonsterData.Attribute;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.combat.CombatActionManager;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.MonsterDrop.DropFlag;
import net.sourceforge.kolmafia.persistence.MonsterDrop.MultiDrop;
import net.sourceforge.kolmafia.persistence.MonsterDrop.SimpleMonsterDrop;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MonsterDatabase {
  private static final Set<MonsterData> ALL_MONSTER_DATA = new TreeSet<>();
  private static final Map<String, MonsterData> MONSTER_DATA = new TreeMap<>();
  private static final Map<Integer, MonsterData> MONSTER_IDS = new TreeMap<>();
  private static final Map<String, MonsterData> OLD_MONSTER_DATA = new TreeMap<>();
  private static final Map<String, MonsterData> LEET_MONSTER_DATA = new TreeMap<>();
  private static final Set<String> MONSTER_ALIASES = new HashSet<>();
  private static String[] MONSTER_STRINGS = null;
  private static final Map<String, MonsterData> MONSTER_IMAGES = new TreeMap<>();
  private static final Map<String, Map<MonsterData, MonsterData>> MONSTER_PATH_MAP =
      new TreeMap<>();
  private static final Map<AscensionClass, Map<MonsterData, MonsterData>> MONSTER_CLASS_MAP =
      new EnumMap<>(AscensionClass.class);

  // For handling duplicate monster and substring match of monster names
  private static final Map<String, MonsterData[]> MONSTER_ID_SET = new HashMap<>();
  private static String[] canonicalNames = new String[0];

  private MonsterDatabase() {}

  public enum Element {
    NONE("none", "circle.gif", "black", "has no particular elemental alignment"),
    COLD("cold", "snowflake.gif", "blue", "is Cold. Cold is weak against Hot and Spooky."),
    HOT("hot", "fire.gif", "red", "is Hot. Hot is weak against Sleaze and Stench."),
    SLEAZE(
        "sleaze",
        "wink.gif",
        "#8A2BE2", // blueviolet
        "is Sleazy. Sleaze is weak against Cold and Spooky."),
    SPOOKY("spooky", "skull.gif", "gray", "is Spooky. Spooky is weak against Hot and Stench."),
    STENCH("stench", "stench.gif", "green", "is Stinky. Stench is weak against Cold and Sleaze."),
    SLIME(
        "slime",
        "sebashield.gif",
        "#006400", // darkgreen
        "is Slimy."),
    SUPERCOLD(
        "supercold",
        "ice.gif",
        "#ADD8E6", // lightblue
        "is Supercold."),
    BADSPELLING(
        "bad spelling",
        "cookbook.gif",
        "#800080", // purple
        "is Bad Spelling. Bad Spelling is weak against dictionaries."),
    SHADOW(
        "shadow",
        "ice.gif",
        "#808080", // gray
        "is Shadow.");

    private final String name;
    private final String image;
    private final String color;
    private final String description;

    Element(String name, String image, String color, String description) {
      this.name = name;
      this.image = image;
      this.color = color;
      this.description = description;
    }

    @Override
    public String toString() {
      return this.name;
    }

    public String toTitle() {
      return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public String getImage() {
      return this.image;
    }

    public String getColor() {
      return this.color;
    }

    public String getDescription() {
      return this.description;
    }

    public static Element fromString(String text) {
      if (text == null || text.isEmpty()) return Element.NONE;
      return Arrays.stream(values())
          .filter(e -> e.name.equalsIgnoreCase(text))
          .findAny()
          .orElse(Element.NONE);
    }
  }

  public enum Phylum {
    NONE("none", "circle.gif", "Unknown", ""),
    BEAST("beast", "beastflavor.gif", "a Beast", "beasts"),
    BUG("bug", "stinkbug.gif", "a Bug", "bugs"),
    CONSTELLATION("constellation", "star.gif", "a Constellation", "constellations"),
    CONSTRUCT("construct", "sprocket.gif", "a Construct", "constructs"),
    DEMON("demon", "demonflavor.gif", "a Demon", "demons"),
    DUDE("dude", "happy.gif", "a Dude", "dudes"),
    ELEMENTAL("elemental", "rrainbow.gif", "an Elemental", "elementals"),
    ELF("elf", "elfflavor.gif", "an Elf", "elves"),
    FISH("fish", "fish.gif", "a Fish", "fishies"),
    GOBLIN("goblin", "goblinflavor.gif", "a Goblin", "goblins"),
    HIPPY("hippy", "hippyflavor.gif", "a Hippy", "hippys"),
    HOBO("hobo", "hoboflavor.gif", "a Hobo", "hobos"),
    HORROR("horror", "skull.gif", "a Horror", "horrors"),
    HUMANOID("humanoid", "statue.gif", "a Humanoid", "humanoids"),
    MER_KIN("mer-kin", "merkinflavor.gif", "a Mer-kin", "merkins"),
    ORC("orc", "frattyflavor.gif", "an Orc", "orcs"),
    PENGUIN("penguin", "bowtie.gif", "a Penguin", "penguins"),
    PIRATE("pirate", "pirateflavor.gif", "a Pirate", "pirates"),
    PLANT("plant", "leafflavor.gif", "a Plant", "plants"),
    SLIME("slime", "sebashield.gif", "a Slime", "slimes"),
    UNDEAD("undead", "spookyflavor.gif", "an Undead", "the undead"),
    WEIRD("weird", "weirdflavor.gif", "Weird", "weirds"),
    ;

    private final String name;
    private final String image;
    private final String description;
    private final String plural;

    Phylum(String name, String image, String description, String plural) {
      this.name = name;
      this.image = image;
      this.description = description;
      this.plural = plural;
    }

    @Override
    public String toString() {
      return this.name;
    }

    public String toToken() {
      return this.name.replace("-", "");
    }

    public String getImage() {
      return this.image;
    }

    public String getDescription() {
      return this.description;
    }

    public String getPlural() {
      return this.plural;
    }

    public static final Phylum find(final String name, final boolean checkPlurals) {
      for (Phylum phylum : Phylum.values()) {
        if (name.replace("-", "").equalsIgnoreCase(phylum.toString().replace("-", ""))
            || (checkPlurals && name.equalsIgnoreCase(phylum.getPlural()))) {
          return phylum;
        }
      }
      return Phylum.NONE;
    }

    public static final Phylum find(final String name) {
      return find(name, true);
    }
  }

  public static final String[] ELEMENT_ARRAY = new String[Element.values().length];
  public static final String[] PHYLUM_ARRAY = new String[Phylum.values().length];

  static {
    for (int i = 0; i < Element.values().length; i++) {
      ELEMENT_ARRAY[i] = Element.values()[i].toString();
    }

    for (int i = 0; i < Phylum.values().length; i++) {
      PHYLUM_ARRAY[i] = Phylum.values()[i].toString();
    }
  }

  public static final Element stringToElement(final String name) {
    for (Element elem : Element.values()) {
      if (name.equalsIgnoreCase(elem.toString())) {
        return elem;
      }
    }
    return Element.NONE;
  }

  public static final boolean elementalVulnerability(
      final Element element1, final Element element2) {
    return switch (element1) {
      case COLD -> element2 == Element.HOT || element2 == Element.SPOOKY;
      case HOT -> element2 == Element.SLEAZE || element2 == Element.STENCH;
      case SLEAZE -> element2 == Element.COLD || element2 == Element.SPOOKY;
      case SPOOKY -> element2 == Element.HOT || element2 == Element.STENCH;
      case STENCH -> element2 == Element.SLEAZE || element2 == Element.COLD;
      default -> false;
    };
  }

  private static void addMapping(Map<MonsterData, MonsterData> map, String name1, String name2) {
    MonsterData mon1 = MONSTER_DATA.get(monsterKey(name1));
    MonsterData mon2 = name2 != null ? MONSTER_DATA.get(monsterKey(name2)) : MonsterData.NO_MONSTER;
    MonsterDatabase.addMapping(map, mon1, mon2);
  }

  private static void addMapping(Map<MonsterData, MonsterData> map, String name1, int id2) {
    MonsterData mon1 = MONSTER_DATA.get(name1);
    MonsterData mon2 = id2 != 0 ? MONSTER_IDS.get(id2) : MonsterData.NO_MONSTER;
    MonsterDatabase.addMapping(map, mon1, mon2);
  }

  private static void addMapping(
      Map<MonsterData, MonsterData> map, MonsterData mon1, MonsterData mon2) {
    if (mon1 != null && mon2 != null) {
      map.put(mon1, mon2);
    }
  }

  static {
    MonsterDatabase.refreshMonsterTable();

    Map<MonsterData, MonsterData> youRobotMap = new TreeMap<>();
    MonsterDatabase.addMapping(youRobotMap, "Boss Bat", "Boss Bot");
    MonsterDatabase.addMapping(youRobotMap, "Knob Goblin King", "Gobot King");
    MonsterDatabase.addMapping(youRobotMap, "Bonerdagon", "Robonerdagon");
    MonsterDatabase.addMapping(youRobotMap, "Groar", "Groarbot");
    MonsterDatabase.addMapping(youRobotMap, "Dr. Awkward", "Tobias J. Saibot");
    MonsterDatabase.addMapping(youRobotMap, "Lord Spookyraven", "Lord Cyberraven");
    MonsterDatabase.addMapping(youRobotMap, "Protector Spectre", "Protector S. P. E. C. T. R. E.");
    MonsterDatabase.addMapping(youRobotMap, "The Big Wisniewski", "The Artificial Wisniewski");
    MonsterDatabase.addMapping(youRobotMap, "The Man", "The Android");
    MonsterDatabase.addMapping(youRobotMap, "Naughty Sorceress", "Nautomatic Sorceress");
    MonsterDatabase.addMapping(youRobotMap, "Naughty Sorceress (2)", null);
    MonsterDatabase.addMapping(youRobotMap, "Naughty Sorceress (3)", null);
    MonsterDatabase.MONSTER_PATH_MAP.put(Path.YOU_ROBOT.getName(), youRobotMap);

    Map<MonsterData, MonsterData> plumberMap = new TreeMap<>();
    MonsterDatabase.addMapping(plumberMap, "Boss Bat", "Koopa Paratroopa");
    MonsterDatabase.addMapping(plumberMap, "Knob Goblin King", "Hammer Brother");
    MonsterDatabase.addMapping(plumberMap, "Bonerdagon", "Very Dry Bones");
    MonsterDatabase.addMapping(plumberMap, "Groar", "Angry Sun");
    MonsterDatabase.addMapping(plumberMap, "Dr. Awkward", "Birdo");
    MonsterDatabase.addMapping(plumberMap, "Lord Spookyraven", "King Boo");
    MonsterDatabase.addMapping(plumberMap, "Protector Spectre", "Kamek");
    MonsterDatabase.addMapping(plumberMap, "The Big Wisniewski", 2172);
    MonsterDatabase.addMapping(plumberMap, "The Man", 2173);
    MonsterDatabase.addMapping(plumberMap, "Naughty Sorceress", "Wa%playername/lowercase%");
    MonsterDatabase.addMapping(plumberMap, "Naughty Sorceress (2)", null);
    MonsterDatabase.addMapping(plumberMap, "Naughty Sorceress (3)", null);
    MonsterDatabase.MONSTER_PATH_MAP.put(Path.PATH_OF_THE_PLUMBER.getName(), plumberMap);

    Map<MonsterData, MonsterData> darkGyffteMap = new TreeMap<>();
    MonsterDatabase.addMapping(darkGyffteMap, "Boss Bat", "Steve Belmont");
    MonsterDatabase.addMapping(darkGyffteMap, "Knob Goblin King", "Ricardo Belmont");
    MonsterDatabase.addMapping(darkGyffteMap, "Bonerdagon", "Jayden Belmont");
    MonsterDatabase.addMapping(darkGyffteMap, "Groar", "Sharona");
    MonsterDatabase.addMapping(darkGyffteMap, "Dr. Awkward", "Travis Belmont");
    MonsterDatabase.addMapping(darkGyffteMap, "Lord Spookyraven", "Greg Dagreasy");
    MonsterDatabase.addMapping(darkGyffteMap, "Protector Spectre", "Sylvia Belgrande");
    MonsterDatabase.addMapping(darkGyffteMap, "The Big Wisniewski", "Jake Norris");
    MonsterDatabase.addMapping(darkGyffteMap, "The Man", "Chad Alacarte");
    MonsterDatabase.addMapping(darkGyffteMap, "Your Shadow", "Your Lack of Reflection");
    MonsterDatabase.addMapping(darkGyffteMap, "Naughty Sorceress", "%alucard%");
    MonsterDatabase.addMapping(darkGyffteMap, "Naughty Sorceress (2)", null);
    MonsterDatabase.addMapping(darkGyffteMap, "Naughty Sorceress (3)", null);
    MonsterDatabase.MONSTER_PATH_MAP.put(Path.DARK_GYFFTE.getName(), darkGyffteMap);

    Map<MonsterData, MonsterData> pocketFamiliarsMap = new TreeMap<>();
    // These are all "Jerry Bradford"
    MonsterDatabase.addMapping(pocketFamiliarsMap, "Boss Bat", 2050);
    MonsterDatabase.addMapping(pocketFamiliarsMap, "Knob Goblin King", 2051);
    MonsterDatabase.addMapping(pocketFamiliarsMap, "Bonerdagon", 2052);
    MonsterDatabase.addMapping(pocketFamiliarsMap, "Groar", 2053);
    MonsterDatabase.addMapping(pocketFamiliarsMap, "Dr. Awkward", 2054);
    MonsterDatabase.addMapping(pocketFamiliarsMap, "Lord Spookyraven", 2055);
    MonsterDatabase.addMapping(pocketFamiliarsMap, "Protector Spectre", 2056);
    MonsterDatabase.addMapping(pocketFamiliarsMap, "The Big Wisniewski", 2057);
    MonsterDatabase.addMapping(pocketFamiliarsMap, "The Man", 2058);
    MonsterDatabase.addMapping(pocketFamiliarsMap, "Naughty Sorceress", 2059);
    MonsterDatabase.addMapping(pocketFamiliarsMap, "Naughty Sorceress (2)", null);
    MonsterDatabase.addMapping(pocketFamiliarsMap, "Naughty Sorceress (3)", null);
    MonsterDatabase.MONSTER_PATH_MAP.put(Path.POKEFAM.getName(), pocketFamiliarsMap);

    Map<MonsterData, MonsterData> heavyRainsMap = new TreeMap<>();
    MonsterDatabase.addMapping(heavyRainsMap, "Boss Bat", "Aquabat");
    MonsterDatabase.addMapping(heavyRainsMap, "Knob Goblin King", "Aquagoblin");
    MonsterDatabase.addMapping(heavyRainsMap, "Bonerdagon", "Auqadargon");
    MonsterDatabase.addMapping(heavyRainsMap, "Groar", "Gurgle");
    MonsterDatabase.addMapping(heavyRainsMap, "Dr. Awkward", "Dr. Aquard");
    MonsterDatabase.addMapping(heavyRainsMap, "Lord Spookyraven", "Lord Soggyraven");
    MonsterDatabase.addMapping(heavyRainsMap, "Protector Spectre", "Protector Spurt");
    MonsterDatabase.addMapping(heavyRainsMap, "The Big Wisniewski", "Big Wisnaqua");
    MonsterDatabase.addMapping(heavyRainsMap, "The Man", "The Aquaman");
    MonsterDatabase.addMapping(heavyRainsMap, "Naughty Sorceress", "The Rain King");
    MonsterDatabase.addMapping(heavyRainsMap, "Naughty Sorceress (2)", null);
    MonsterDatabase.addMapping(heavyRainsMap, "Naughty Sorceress (3)", null);
    MonsterDatabase.MONSTER_PATH_MAP.put(Path.HEAVY_RAINS.getName(), heavyRainsMap);

    Map<MonsterData, MonsterData> actuallyEdMap = new TreeMap<>();
    MonsterDatabase.addMapping(actuallyEdMap, "Boss Bat", "Boss Bat?");
    MonsterDatabase.addMapping(actuallyEdMap, "Knob Goblin King", "new Knob Goblin King");
    MonsterDatabase.addMapping(actuallyEdMap, "Bonerdagon", "Donerbagon");
    MonsterDatabase.addMapping(actuallyEdMap, "Groar", "Your winged yeti");
    MonsterDatabase.addMapping(actuallyEdMap, "Naughty Sorceress", "You the Adventurer");
    MonsterDatabase.addMapping(actuallyEdMap, "Naughty Sorceress (2)", null);
    MonsterDatabase.addMapping(actuallyEdMap, "Naughty Sorceress (3)", null);
    MonsterDatabase.MONSTER_PATH_MAP.put(Path.ACTUALLY_ED_THE_UNDYING.getName(), actuallyEdMap);

    Map<MonsterData, MonsterData> wildfireMap = new TreeMap<>();
    MonsterDatabase.addMapping(wildfireMap, "Boss Bat", "Blaze Bat");
    MonsterDatabase.addMapping(wildfireMap, "Knob Goblin King", "fired-up Knob Goblin King");
    MonsterDatabase.addMapping(wildfireMap, "Bonerdagon", "Burnerdagon");
    MonsterDatabase.addMapping(wildfireMap, "Groar", "Groar, Except Hot");
    MonsterDatabase.addMapping(wildfireMap, "Dr. Awkward", "Dr. Awkward, who is on fire");
    MonsterDatabase.addMapping(wildfireMap, "Lord Spookyraven", "Lord Sootyraven");
    MonsterDatabase.addMapping(wildfireMap, "Protector Spectre", "Protector Spectre (Wildfire)");
    MonsterDatabase.addMapping(wildfireMap, "The Big Wisniewski", "The Big Ignatowicz");
    MonsterDatabase.addMapping(wildfireMap, "The Man", "The Man on Fire");
    MonsterDatabase.addMapping(wildfireMap, "Naughty Sorceress", "The Naughty Scorcheress");
    MonsterDatabase.addMapping(wildfireMap, "Naughty Sorceress (2)", null);
    MonsterDatabase.addMapping(wildfireMap, "Naughty Sorceress (3)", null);
    MonsterDatabase.MONSTER_PATH_MAP.put(Path.WILDFIRE.getName(), wildfireMap);

    Map<MonsterData, MonsterData> dinoMap = new TreeMap<>();
    MonsterDatabase.addMapping(
        dinoMap, "Boss Bat", "two-headed pteranodon with a two-headed bat inside it");
    MonsterDatabase.addMapping(dinoMap, "Knob Goblin King", "goblodocus");
    MonsterDatabase.addMapping(dinoMap, "Bonerdagon", "T-Rex who ate the Bonerdagon");
    MonsterDatabase.addMapping(dinoMap, "Groar", "refrigeradon");
    MonsterDatabase.addMapping(dinoMap, "Dr. Awkward", "suruasaurus");
    MonsterDatabase.addMapping(dinoMap, "Lord Spookyraven", "herd of well-fed microraptors");
    MonsterDatabase.addMapping(dinoMap, "Protector Spectre", "protoceratops spectre");
    MonsterDatabase.addMapping(dinoMap, "The Big Wisniewski", "Slackiosaurus");
    MonsterDatabase.addMapping(dinoMap, "The Man", "Oligarcheopteryx");
    MonsterDatabase.addMapping(dinoMap, "Naughty Sorceress", "Naughty Saursaurus");
    MonsterDatabase.addMapping(dinoMap, "Naughty Sorceress (2)", null);
    MonsterDatabase.addMapping(dinoMap, "Naughty Sorceress (3)", null);
    MonsterDatabase.MONSTER_PATH_MAP.put(Path.DINOSAURS.getName(), dinoMap);

    Map<MonsterData, MonsterData> aosolMap = new TreeMap<>();
    MonsterDatabase.addMapping(aosolMap, "Boss Bat", "two-headed shadow bat");
    MonsterDatabase.addMapping(aosolMap, "Knob Goblin King", "goblin king's shadow");
    MonsterDatabase.addMapping(aosolMap, "Bonerdagon", "shadowboner shadowdagon");
    MonsterDatabase.addMapping(aosolMap, "Groar", "shadow of groar");
    MonsterDatabase.addMapping(aosolMap, "Dr. Awkward", "W. Odah's Shadow");
    MonsterDatabase.addMapping(aosolMap, "Lord Spookyraven", "shadow Lord Spookyraven");
    MonsterDatabase.addMapping(aosolMap, "Protector Spectre", "corruptor shadow");
    MonsterDatabase.addMapping(aosolMap, "The Big Wisniewski", "shadow of the 1960s");
    MonsterDatabase.addMapping(aosolMap, "The Man", "shadow of the 1980s");
    MonsterDatabase.MONSTER_PATH_MAP.put(Path.SHADOWS_OVER_LOATHING.getName(), aosolMap);

    Map<MonsterData, MonsterData> pigSkinnerMap = new TreeMap<>();
    MonsterDatabase.addMapping(pigSkinnerMap, "Naughty Sorceress", "General Bruise");
    MonsterDatabase.addMapping(
        pigSkinnerMap, "Naughty Sorceress (2)", "General Bruise (true form)");
    MonsterDatabase.addMapping(pigSkinnerMap, "Naughty Sorceress (3)", null);
    MonsterDatabase.MONSTER_CLASS_MAP.put(AscensionClass.PIG_SKINNER, pigSkinnerMap);

    Map<MonsterData, MonsterData> cheeseWizardMap = new TreeMap<>();
    MonsterDatabase.addMapping(cheeseWizardMap, "Naughty Sorceress", "Dark Noël");
    MonsterDatabase.addMapping(cheeseWizardMap, "Naughty Sorceress (2)", "Dark Noël (true form)");
    MonsterDatabase.addMapping(cheeseWizardMap, "Naughty Sorceress (3)", null);
    MonsterDatabase.MONSTER_CLASS_MAP.put(AscensionClass.CHEESE_WIZARD, cheeseWizardMap);

    Map<MonsterData, MonsterData> jazzAgentMap = new TreeMap<>();
    MonsterDatabase.addMapping(jazzAgentMap, "Naughty Sorceress", "Terrence Poindexter");
    MonsterDatabase.addMapping(
        jazzAgentMap, "Naughty Sorceress (2)", "Terrence Poindexter (true form)");
    MonsterDatabase.addMapping(jazzAgentMap, "Naughty Sorceress (3)", null);
    MonsterDatabase.MONSTER_CLASS_MAP.put(AscensionClass.JAZZ_AGENT, jazzAgentMap);

    Map<MonsterData, MonsterData> lolMap = new TreeMap<>();
    MonsterDatabase.addMapping(lolMap, "Boss Bat", "Classic Boss Bat");
    MonsterDatabase.addMapping(lolMap, "Knob Goblin King", "Weirdly Scrawny Knob Goblin King");
    MonsterDatabase.addMapping(lolMap, "Bonerdagon", "Orignial Bonerdagon");
    MonsterDatabase.addMapping(lolMap, "Groar", "Flock of Groars?");
    MonsterDatabase.addMapping(lolMap, "Dr. Awkward", "Jr. Awkwarj");
    MonsterDatabase.addMapping(lolMap, "Lord Spookyraven", "Little Lord Spookyraven");
    MonsterDatabase.addMapping(lolMap, "Protector Spectre", "Protector Spectre Candidate");
    MonsterDatabase.addMapping(lolMap, "The Big Wisniewski", "The Little Wisniewski");
    MonsterDatabase.addMapping(lolMap, "The Man", "The Boy");
    MonsterDatabase.MONSTER_PATH_MAP.put(Path.LEGACY_OF_LOATHING.getName(), lolMap);

    Map<MonsterData, MonsterData> wereProfMap = new TreeMap<>();
    MonsterDatabase.addMapping(wereProfMap, "Boss Bat", "Boss Beast");
    MonsterDatabase.addMapping(wereProfMap, "Knob Goblin King", "Knob Goblin Beast");
    MonsterDatabase.addMapping(wereProfMap, "Bonerdagon", "Curséd Bonerdagon");
    MonsterDatabase.addMapping(wereProfMap, "Groar", "Just Groar");
    MonsterDatabase.addMapping(wereProfMap, "Dr. Awkward", "Were-Dr. Awkwarder, ew");
    MonsterDatabase.addMapping(wereProfMap, "Lord Spookyraven", "Lord Beastlyraven");
    MonsterDatabase.addMapping(wereProfMap, "Protector Spectre", "Protector Beast");
    MonsterDatabase.addMapping(wereProfMap, "The Big Wisniewski", "The Beast Wisniewski");
    // MonsterDatabase.addMapping(wereProfMap, "The Man", "The Beast Man");
    MonsterDatabase.addMapping(wereProfMap, "Naughty Sorceress", "The Naughty Wolferess");
    MonsterDatabase.addMapping(wereProfMap, "Naughty Sorceress (2)", null);
    MonsterDatabase.addMapping(wereProfMap, "Naughty Sorceress (3)", null);
    MonsterDatabase.MONSTER_PATH_MAP.put(Path.WEREPROFESSOR.getName(), wereProfMap);
  }

  public static Map<MonsterData, MonsterData> getMonsterPathMap(final String path) {
    return MonsterDatabase.MONSTER_PATH_MAP.get(path);
  }

  public static Map<MonsterData, MonsterData> getMonsterClassMap(final AscensionClass clazz) {
    return MonsterDatabase.MONSTER_CLASS_MAP.get(clazz);
  }

  public static final void refreshMonsterTable() {
    MonsterDatabase.ALL_MONSTER_DATA.clear();
    MonsterDatabase.MONSTER_DATA.clear();
    MonsterDatabase.OLD_MONSTER_DATA.clear();
    MonsterDatabase.MONSTER_IMAGES.clear();

    try (BufferedReader reader =
        FileUtilities.getVersionedReader("monsters.txt", KoLConstants.MONSTERS_VERSION)) {
      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length < 1) {
          continue;
        }

        String name = data[0];
        String idString = data.length > 1 ? data[1] : "";
        String imageString = data.length > 2 ? data[2] : "";
        String attributes = data.length > 3 ? data[3] : "";

        int id = StringUtilities.isNumeric(idString) ? StringUtilities.parseInt(idString) : 0;
        String[] images = imageString.split("\\s*,\\s*");

        MonsterData monster = MonsterDatabase.newMonster(name, id, images, attributes);
        if (monster == null) {
          // *** Surely this is an error?
          continue;
        }

        boolean bogus = false;

        for (int i = 4; i < data.length; ++i) {
          String itemString = data[i];
          MonsterDrop drop = MonsterDatabase.parseItem(itemString);
          var item = drop.item();
          if (item == null || item.getItemId() == -1 || item.getName() == null) {
            RequestLogger.printLine("Bad item for monster \"" + name + "\": " + itemString);
            bogus = true;
            continue;
          }

          monster.addItem(drop);
        }

        if (!bogus) {
          monster.doneWithItems();

          MonsterDatabase.saveMonster(name, monster);
          for (String image : monster.getImages()) {
            MonsterDatabase.MONSTER_IMAGES.put(image, monster);
          }
          MonsterDatabase.registerMonsterId(id, name, monster);

          MonsterDatabase.LEET_MONSTER_DATA.put(StringUtilities.leetify(name), monster);
        }
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }

    // Save canonical names for substring lookup
    MonsterDatabase.saveCanonicalNames();
  }

  private static void addMonsterToName(MonsterData monster) {
    String canonicalName = StringUtilities.getCanonicalName(monster.getName());
    MonsterData[] monsterSet = MonsterDatabase.MONSTER_ID_SET.get(canonicalName);
    MonsterData[] newSet;

    if (monsterSet == null) {
      newSet = new MonsterData[1];
    }
    // *** This assumes the array is sorted
    else if (Arrays.binarySearch(monsterSet, monster) >= 0) {
      return;
    } else {
      newSet = Arrays.copyOf(monsterSet, monsterSet.length + 1);
    }

    newSet[newSet.length - 1] = monster;
    // *** Make it so
    Arrays.sort(newSet);
    MonsterDatabase.MONSTER_ID_SET.put(canonicalName, newSet);
  }

  private static void saveCanonicalNames() {
    String[] newArray = new String[MonsterDatabase.MONSTER_ID_SET.size()];
    MonsterDatabase.MONSTER_ID_SET.keySet().toArray(newArray);
    Arrays.sort(newArray);
    MonsterDatabase.canonicalNames = newArray;
  }

  public static final void saveAliases() {
    // Remove previously saved aliases, since you can log in as a
    // new player without exiting KoLmafia
    for (String alias : MONSTER_ALIASES) {
      MonsterDatabase.removeAlias(alias);
    }
    MONSTER_ALIASES.clear();

    String playerName = KoLCharacter.getUserName();
    if (playerName.equals("")) {
      return;
    }

    String waname = "Wa" + playerName.toLowerCase();
    String alucard = new StringBuilder(playerName).reverse().toString();

    MonsterDatabase.saveAlias("Wa%playername/lowercase%", waname);
    MonsterDatabase.saveAlias("%alucard%", alucard);
  }

  private static void saveAlias(final String name, final String alias) {
    MonsterData monster = MonsterDatabase.findMonster(name);
    if (monster == null) {
      // This is a bug and should not happen
      System.out.println("Could not find monster named '" + name + "'");
      return;
    }

    MonsterData cloned =
        new MonsterData(monster) {
          @Override
          public String getName() {
            return alias;
          }
        };

    MonsterDatabase.saveMonster(alias, cloned);
    MonsterDatabase.addMonsterToName(cloned);
    MONSTER_ALIASES.add(alias);
  }

  private static String monsterKey(String name) {
    return CombatActionManager.encounterKey(name, false);
  }

  private static void saveMonster(final String name, final MonsterData monster) {
    String keyName = monsterKey(name);
    StringUtilities.registerPrepositions(keyName);
    MonsterDatabase.ALL_MONSTER_DATA.add(monster);
    MonsterDatabase.MONSTER_DATA.put(keyName, monster);
    MonsterDatabase.OLD_MONSTER_DATA.put(keyName.toLowerCase(), monster);
    if (keyName.toLowerCase().startsWith("the ")) {
      // Some effects seem to sometimes remove The from the start of the monster name even if
      // normally part of name
      // eg. ELDRITCH HORROR Master Of Thieves
      // So allow finding monster without the 'The' also
      MonsterDatabase.MONSTER_DATA.put(keyName.substring(4), monster);
      MonsterDatabase.OLD_MONSTER_DATA.put(keyName.substring(4).toLowerCase(), monster);
    }
  }

  private static void removeAlias(final String name) {
    String keyName = monsterKey(name);
    MonsterDatabase.MONSTER_DATA.remove(keyName);
    MonsterDatabase.OLD_MONSTER_DATA.remove(keyName.toLowerCase());
    if (keyName.toLowerCase().startsWith("the ")) {
      MonsterDatabase.MONSTER_DATA.remove(keyName.substring(4));
      MonsterDatabase.OLD_MONSTER_DATA.remove(keyName.substring(4).toLowerCase());
    }
  }

  private static MonsterDrop parseItem(final String data) {
    Matcher dropMatcher = MonsterDrop.DROP.matcher(data);
    if (!dropMatcher.matches()) {
      throw new IllegalStateException(data + " did not match expected layout");
    }

    var itemName = dropMatcher.group(1);
    var flag = DropFlag.fromFlag(dropMatcher.group(2));
    var chance = StringUtilities.parseDouble(dropMatcher.group(3));

    if (flag == DropFlag.MULTI_DROP) {
      Matcher itemMatcher = MultiDrop.ITEM.matcher(itemName);
      if (!itemMatcher.matches()) {
        throw new IllegalStateException(data + " did not match expected layout");
      }

      AdventureResult item = ItemPool.get(itemMatcher.group(2), 1);
      return new MultiDrop(itemMatcher.group(1), item, chance, flag);
    } else {
      AdventureResult item = ItemPool.get(itemName, 1);

      if (flag == DropFlag.NONE && chance == 0) {
        flag = DropFlag.UNKNOWN_RATE;
      }

      return new SimpleMonsterDrop(item, chance, flag);
    }
  }

  private static synchronized void initializeMonsterStrings() {
    if (MonsterDatabase.MONSTER_STRINGS == null) {
      String[] monsterData = new String[MonsterDatabase.MONSTER_DATA.size()];
      MonsterDatabase.MONSTER_DATA.keySet().toArray(monsterData);
      MonsterDatabase.MONSTER_STRINGS = monsterData;
    }
  }

  // Monster lookup using encounter keys. Used for finding monsters that KoL gives us.

  public static final MonsterData findMonster(final String name) {
    // Exact match monster name lookup - use this when KoL itself gives us a monster name
    return findMonster(name, false, true);
  }

  public static final MonsterData findMonster(
      final String name, boolean trySubstrings, boolean matchCase) {
    // Look for case-sensitive exact match
    String keyName = monsterKey(name);
    MonsterData match = MonsterDatabase.MONSTER_DATA.get(keyName);

    if (match != null) {
      return match;
    }

    // If we are looking for an exact match, try again with "the"
    // or "The" removed from the front; there are plenty of monsters
    // with either of those - and CombatActionManager.encounterKey
    // therefore does not remove them - but KoL sometimes includes such.
    if (!trySubstrings && matchCase) {
      if (keyName.startsWith("the ") || keyName.startsWith("The ")) {
        return MonsterDatabase.MONSTER_DATA.get(keyName.substring(4));
      }
      return null;
    }

    // Look for case-sensitive fuzzy match
    if (trySubstrings) {
      MonsterDatabase.initializeMonsterStrings();
      List<String> matchingNames =
          StringUtilities.getMatchingNames(MonsterDatabase.MONSTER_STRINGS, keyName);
      if (matchingNames.size() == 1) {
        return MonsterDatabase.MONSTER_DATA.get(matchingNames.get(0));
      }
    }

    // Look for case-insensitive exact match
    if (!matchCase) {
      keyName = CombatActionManager.encounterKey(name);
      return MonsterDatabase.OLD_MONSTER_DATA.get(keyName);
    }

    return null;
  }

  // Monster lookup using monster name from ASH scripts.

  public static final MonsterData findMonster(final String name, boolean trySubstrings) {
    // Get all the monsters which match this name
    MonsterData[] monsters = MonsterDatabase.findMonsters(name, trySubstrings);

    // If no (or ambiguous) name match found, no monster to return;
    if (monsters == null) {
      return null;
    }

    // If there is exactly one monster that matches, we got it.
    if (monsters.length == 1) {
      return monsters[0];
    }

    // Exact match for the name, but multiple monsters
    return null;
  }

  private static MonsterData getBracketedMonster(final String monsterName) {
    if (monsterName.startsWith("[")) {
      int index = monsterName.indexOf("]");
      if (index > 0) {
        String idString = monsterName.substring(1, index);
        if (StringUtilities.isNumeric(idString)) {
          int monsterId = StringUtilities.parseInt(idString);
          MonsterData monster = MonsterDatabase.findMonsterById(monsterId);
          if (monster != null) {
            return monster;
          }
        }
      }
    }
    return null;
  }

  private static final MonsterData[] NO_MONSTERS = new MonsterData[0];

  public static final MonsterData[] findMonsters(
      final String monsterName, final boolean substringMatch) {
    if (monsterName == null) {
      return NO_MONSTERS;
    }

    // If name starts with [nnnn] then that is explicitly the monster id
    if (monsterName.startsWith("[")) {
      MonsterData monster = MonsterDatabase.getBracketedMonster(monsterName);
      if (monster != null) {
        MonsterData[] monsters = new MonsterData[1];
        monsters[0] = monster;
        return monsters;
      }
    }

    // We want an exact (case insensitive) match
    if (!substringMatch) {
      String canonicalName = StringUtilities.getCanonicalName(monsterName);
      MonsterData[] monsters = MonsterDatabase.MONSTER_ID_SET.get(canonicalName);
      if (monsters != null && monsters.length > 0) {
        return monsters;
      }
      return NO_MONSTERS;
    }

    // We want a substring match. Do a canonical name search
    String canonical = StringUtilities.getCanonicalName(monsterName);
    List<String> possibilities =
        StringUtilities.getMatchingNames(MonsterDatabase.canonicalNames, canonical);

    // If one name matches, return the monster set for that name
    if (possibilities.size() == 1) {
      String first = possibilities.get(0);
      return MonsterDatabase.MONSTER_ID_SET.get(first);
    }

    // Otherwise the name is ambiguous or not found
    return NO_MONSTERS;
  }

  private static final int[] NO_MONSTER_IDS = new int[0];

  public static final int[] getMonsterIds(final String monsterName, final boolean substringMatch) {
    MonsterData[] monsters = MonsterDatabase.findMonsters(monsterName, substringMatch);

    if (monsters == null) {
      return NO_MONSTER_IDS;
    }

    int length = monsters.length;
    int[] monsterIds = new int[length];
    for (int i = 0; i < length; ++i) {
      monsterIds[i] = monsters[i].getId();
    }

    return monsterIds;
  }

  public static final MonsterData findMonsterByImage(String image) {
    int slashIndex = image.indexOf("/");
    while (slashIndex != -1) {
      image = image.substring(slashIndex + 1);
      MonsterData monster = MonsterDatabase.MONSTER_IMAGES.get(image);
      if (monster != null) {
        return monster;
      }
      slashIndex = image.indexOf("/");
    }
    return MonsterDatabase.MONSTER_IMAGES.get(image);
  }

  public static final MonsterData findMonsterById(final int id) {
    return MonsterDatabase.MONSTER_IDS.get(id);
  }

  public static final String getMonsterName(final int id) {
    MonsterData monster = MonsterDatabase.MONSTER_IDS.get(id);
    return monster == null ? "" : monster.getName();
  }

  public static final String translateLeetMonsterName(final String leetName) {
    MonsterData monster = MonsterDatabase.LEET_MONSTER_DATA.get(leetName);
    return monster == null ? leetName : monster.getName();
  }

  // Register an unknown monster
  public static final void registerMonster(final MonsterData monster) {
    int id = monster.getId();
    String name = monster.getName();
    String keyName = monsterKey(name);
    MonsterDatabase.MONSTER_DATA.put(keyName, monster);
    MonsterDatabase.OLD_MONSTER_DATA.put(keyName.toLowerCase(), monster);
    MonsterDatabase.LEET_MONSTER_DATA.put(StringUtilities.leetify(name), monster);
    MonsterDatabase.registerMonsterId(id, name, monster);
    MonsterDatabase.saveCanonicalNames();
  }

  // *** For testing only!
  public static final void unregisterMonster(final MonsterData monster) {
    int id = monster.getId();
    String name = monster.getName();
    String keyName = monsterKey(name);
    MonsterDatabase.MONSTER_DATA.remove(keyName);
    MonsterDatabase.OLD_MONSTER_DATA.remove(keyName.toLowerCase());
    MonsterDatabase.LEET_MONSTER_DATA.remove(StringUtilities.leetify(name));
    MonsterDatabase.MONSTER_IDS.remove(id);
    MonsterDatabase.saveCanonicalNames();
  }

  public static final MonsterData registerMonster(final String name) {
    MonsterData monster = MonsterDatabase.newMonster(name, 0, new String[0], "");
    MonsterDatabase.registerMonster(monster);
    return monster;
  }

  // Register an unknown monster
  public static final MonsterData registerMonster(
      final String name, final int id, final String image) {
    String[] images = {image};
    MonsterData monster = MonsterDatabase.newMonster(name, id, images, "");
    MonsterDatabase.registerMonster(monster);
    return monster;
  }

  // Register an unknown monster from Manuel
  public static final MonsterData registerMonster(
      final String name, final int id, final String image, final String attributes) {
    String[] images = {image};
    MonsterData monster = MonsterDatabase.newMonster(name, id, images, attributes);
    MonsterDatabase.registerMonster(monster);
    return monster;
  }

  public static final void setMonsterId(MonsterData monster, int newMonsterId) {
    int oldMonsterId = monster.getId();
    MonsterDatabase.MONSTER_IDS.remove(oldMonsterId);
    MonsterDatabase.MONSTER_IDS.put(newMonsterId, monster);
    monster.setId(newMonsterId);
  }

  private static void registerMonsterId(
      final int id, final String name, final MonsterData monster) {
    if (id != 0) {
      MonsterData old = MonsterDatabase.MONSTER_IDS.get(id);
      if (old == null) {
        MonsterDatabase.MONSTER_IDS.put(id, monster);
      } else {
        RequestLogger.printLine(
            "Duplicate monster ID " + id + " : (" + old.getName() + "," + name + ")");
      }
    }
    MonsterDatabase.addMonsterToName(monster);
  }

  public static final Set<Entry<String, MonsterData>> entrySet() {
    return MonsterDatabase.MONSTER_DATA.entrySet();
  }

  public static final Set<MonsterData> valueSet() {
    return MonsterDatabase.ALL_MONSTER_DATA;
  }

  public static final Set<Integer> idKeySet() {
    return MonsterDatabase.MONSTER_IDS.keySet();
  }

  public static final Set<Entry<Integer, MonsterData>> idEntrySet() {
    return MonsterDatabase.MONSTER_IDS.entrySet();
  }

  public static final MonsterData newMonster(
      final String name, int id, final String[] images, final String attributes) {
    MonsterData monster = MonsterDatabase.findMonster(name);
    if (monster != null && monster.getId() == id) {
      // *** Is this an error?
      return monster;
    }

    // Ed the Undying has ID = 473. We have 7 different pseudo-monsters for his
    // different stages, named Ed the Undying (1), and so on.  on.  We've given
    // wach of those id = 0, so that looking up monster by id will find the
    // base.  However, when we are in a fight, we want to match MONSTERID with
    // the disambiguated version. Therefore, give each of them id = 473 here.
    if (id == 0) {
      if (name.startsWith("Ed the Undying")) {
        id = 473;
      }
    }

    return new MonsterData(name, id, images, attributes);
  }

  public static final boolean contains(final String name) {
    return MonsterDatabase.findMonster(name, false) != null;
  }

  public static int fixMonsterAttributes(Map<Integer, Map<Attribute, Object>> updates) {
    int count = 0;

    String filename = "monsters.txt";
    // Read from built-in built-in file
    // Write to file in "data" directory.
    //
    // If you restart, KoLmafia will use the version in "data" as an override
    // and you can try things out. But until you copy it into src/data and
    // rebuild, this script will not modify the file in-place.
    try (BufferedReader reader = FileUtilities.getReader(filename, false);
        PrintStream writer =
            LogStream.openStream(new File(KoLConstants.DATA_LOCATION, filename), true)) {

      // Read the version number and write it to output.
      String line = reader.readLine();
      writer.println(line);

      while ((line = reader.readLine()) != null) {
        if (line.length() == 0 || line.startsWith("#")) {
          writer.println(line);
          continue;
        }

        String[] data = line.split("\t", -1);
        if (data.length < 4) {
          writer.println(line);
          continue;
        }

        String name = data[0];
        int monsterId = StringUtilities.parseInt(data[1]);
        String attributes = data[3];

        Map<Attribute, Object> attributeMap = MonsterData.attributeStringToMap(name, attributes);
        if (updates != null) {
          Map<Attribute, Object> update = updates.get(monsterId);
          if (update != null) {
            attributeMap.putAll(update);
          }
        }
        String attributeString = MonsterData.attributeMapToString(attributeMap);

        if (attributes.equals(attributeString)) {
          writer.println(line);
          continue;
        }

        data[3] = attributeString;
        count++;

        writer.println(String.join("\t", data));
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }

    return count;
  }
}
