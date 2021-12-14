package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.combat.CombatActionManager;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.EncounterManager.EncounterType;
import net.sourceforge.kolmafia.utilities.FileUtilities;
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

  // For handling duplicate monster and substring match of monster names
  private static final Map<String, MonsterData[]> MONSTER_ID_SET = new HashMap<>();
  private static String[] canonicalNames = new String[0];

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
        "is Supercold");

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
      if (text != null) {
        for (Element elem : Element.values()) {
          if (text.equals(elem.name)) {
            return elem;
          }
        }
      }
      return Element.NONE;
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
        if (name.replace("-", "").equals(phylum.toString().replace("-", ""))
            || (checkPlurals && name.equals(phylum.getPlural()))) {
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
      if (name.equals(elem.toString())) {
        return elem;
      }
    }
    return Element.NONE;
  }

  public static final boolean elementalVulnerability(
      final Element element1, final Element element2) {
    switch (element1) {
      case COLD:
        return element2 == Element.HOT || element2 == Element.SPOOKY;
      case HOT:
        return element2 == Element.SLEAZE || element2 == Element.STENCH;
      case SLEAZE:
        return element2 == Element.COLD || element2 == Element.SPOOKY;
      case SPOOKY:
        return element2 == Element.HOT || element2 == Element.STENCH;
      case STENCH:
        return element2 == Element.SLEAZE || element2 == Element.COLD;
      default:
        return false;
    }
  }

  private static void addMapping(Map<MonsterData, MonsterData> map, String name1, String name2) {
    MonsterData mon1 = MONSTER_DATA.get(name1);
    MonsterData mon2 = MONSTER_DATA.get(name2);
    MonsterDatabase.addMapping(map, mon1, mon2);
  }

  private static void addMapping(Map<MonsterData, MonsterData> map, String name1, int id2) {
    MonsterData mon1 = MONSTER_DATA.get(name1);
    MonsterData mon2 = MONSTER_IDS.get(id2);
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
    MonsterDatabase.addMapping(youRobotMap, "The Big Wisniewski", "Artificial Wisniewski");
    MonsterDatabase.addMapping(youRobotMap, "The Man", "The Android");
    MonsterDatabase.addMapping(youRobotMap, "Naughty Sorceress", "Nautomatic Sorceress");
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
    MonsterDatabase.MONSTER_PATH_MAP.put(Path.HEAVY_RAINS.getName(), heavyRainsMap);

    Map<MonsterData, MonsterData> actuallyEdMap = new TreeMap<>();
    MonsterDatabase.addMapping(actuallyEdMap, "Boss Bat", "Boss Bat?");
    MonsterDatabase.addMapping(actuallyEdMap, "Knob Goblin King", "new Knob Goblin King");
    MonsterDatabase.addMapping(actuallyEdMap, "Bonerdagon", "Donerbagon");
    MonsterDatabase.addMapping(actuallyEdMap, "Groar", "Your winged yeti");
    MonsterDatabase.addMapping(actuallyEdMap, "Naughty Sorceress", "You the Adventurer");
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
    MonsterDatabase.MONSTER_PATH_MAP.put(Path.WILDFIRE.getName(), wildfireMap);
  }

  public static Map<MonsterData, MonsterData> getMonsterPathMap(final String path) {
    return MonsterDatabase.MONSTER_PATH_MAP.get(path);
  }

  public static final void refreshMonsterTable() {
    MonsterDatabase.ALL_MONSTER_DATA.clear();
    MonsterDatabase.MONSTER_DATA.clear();
    MonsterDatabase.OLD_MONSTER_DATA.clear();
    MonsterDatabase.MONSTER_IMAGES.clear();

    BufferedReader reader =
        FileUtilities.getVersionedReader("monsters.txt", KoLConstants.MONSTERS_VERSION);
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
        continue;
      }

      boolean bogus = false;

      for (int i = 4; i < data.length; ++i) {
        String itemString = data[i];
        AdventureResult item = MonsterDatabase.parseItem(itemString);
        if (item == null || item.getItemId() == -1 || item.getName() == null) {
          RequestLogger.printLine("Bad item for monster \"" + name + "\": " + itemString);
          bogus = true;
          continue;
        }

        monster.addItem(item);
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

    try {
      reader.close();
    } catch (Exception e) {
      // This should not happen.  Therefore, print
      // a stack trace for debug purposes.

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
          public String getName() {
            return alias;
          }
        };

    MonsterDatabase.saveMonster(alias, cloned);
    MonsterDatabase.addMonsterToName(cloned);
    MONSTER_ALIASES.add(alias);
  }

  private static void saveMonster(final String name, final MonsterData monster) {
    String keyName = CombatActionManager.encounterKey(name, false);
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
    String keyName = CombatActionManager.encounterKey(name, false);
    MonsterDatabase.MONSTER_DATA.remove(keyName);
    MonsterDatabase.OLD_MONSTER_DATA.remove(keyName.toLowerCase());
    if (keyName.toLowerCase().startsWith("the ")) {
      MonsterDatabase.MONSTER_DATA.remove(keyName.substring(4));
      MonsterDatabase.OLD_MONSTER_DATA.remove(keyName.substring(4).toLowerCase());
    }
  }

  private static AdventureResult parseItem(final String data) {
    String name = data;
    int count = 0;
    String countString;
    char prefix = '0';

    // Remove quantity and flag
    if (name.endsWith(")")) {
      int left = name.lastIndexOf(" (");

      if (left == -1) {
        return null;
      }

      countString = name.substring(left + 2, name.length() - 1);

      if (!Character.isDigit(countString.charAt(0))) {
        countString = countString.substring(1);
      }

      count = StringUtilities.parseInt(countString);
      prefix = name.charAt(left + 2);
      name = name.substring(0, left);
    }

    int itemId = ItemDatabase.getItemId(name);
    if (itemId == -1) {
      return ItemPool.get(data, '0');
    }

    return ItemPool.get(itemId, (count << 16) | prefix);
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
    String keyName = CombatActionManager.encounterKey(name, false);
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
    MonsterDatabase.MONSTER_DATA.put(name, monster);
    MonsterDatabase.OLD_MONSTER_DATA.put(name.toLowerCase(), monster);
    MonsterDatabase.LEET_MONSTER_DATA.put(StringUtilities.leetify(name), monster);
    MonsterDatabase.registerMonsterId(id, name, monster);
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
      final String name, final int id, final String[] images, final String attributes) {
    MonsterData monster = MonsterDatabase.findMonster(name);
    if (monster != null && monster.getId() == id) {
      return monster;
    }

    // parse parameters and make a new monster
    Object health = null;
    Object attack = null;
    Object defense = null;
    Object initiative = null;
    Object experience = null;
    Object scale = null;
    Object cap = null;
    Object floor = null;
    Object mlMult = null;
    int meat = 0;
    Object minSprinkles = null;
    Object maxSprinkles = null;
    Element attackElement = Element.NONE;
    Element defenseElement = Element.NONE;
    Phylum phylum = Phylum.NONE;
    int poison = Integer.MAX_VALUE;
    boolean boss = false;
    boolean noBanish = false;
    boolean noCopy = false;
    EnumSet<EncounterType> type = EnumSet.noneOf(EncounterType.class);
    int physical = 0;
    String manuelName = null;
    String wikiName = null;
    ArrayList<String> subTypes = new ArrayList<String>();

    StringTokenizer tokens = new StringTokenizer(attributes, " ");
    while (tokens.hasMoreTokens()) {
      String option = tokens.nextToken();
      String value;
      try {
        if (option.equals("HP:")) {
          health = parseNumeric(tokens);
          continue;
        } else if (option.equals("Atk:")) {
          attack = parseNumeric(tokens);
          continue;
        } else if (option.equals("Def:")) {
          defense = parseNumeric(tokens);
          continue;
        } else if (option.equals("Init:")) {
          initiative = parseNumeric(tokens);
          continue;
        } else if (option.equals("Exp:")) {
          experience = parseNumeric(tokens);
          continue;
        } else if (option.equals("Scale:")) {
          scale = parseDefaultedNumeric(tokens, MonsterData.DEFAULT_SCALE);
          continue;
        } else if (option.equals("Cap:")) {
          cap = parseDefaultedNumeric(tokens, MonsterData.DEFAULT_CAP);
          continue;
        } else if (option.equals("Floor:")) {
          floor = parseDefaultedNumeric(tokens, MonsterData.DEFAULT_FLOOR);
          continue;
        } else if (option.equals("MLMult:")) {
          mlMult = parseNumeric(tokens);
          continue;
        } else if (option.equals("Phys:")) {
          if (tokens.hasMoreTokens()) {
            physical = StringUtilities.parseInt(tokens.nextToken());
          }
          continue;
        } else if (option.equals("Item:")) {
          /* itemBlock = */ parseNumeric(tokens);
          continue;
        } else if (option.equals("Skill:")) {
          /* skillBlock = */ parseNumeric(tokens);
          continue;
        } else if (option.equals("Spell:")) {
          /* spellBlock = */ parseNumeric(tokens);
          continue;
        } else if (option.equals("E:")) {
          if (tokens.hasMoreTokens()) {
            value = tokens.nextToken();
            Element element = MonsterDatabase.parseElement(value);
            if (element != Element.NONE) {
              attackElement = element;
              defenseElement = element;
            }
          }
          continue;
        } else if (option.equals("ED:")) {
          if (tokens.hasMoreTokens()) {
            value = tokens.nextToken();
            Element element = MonsterDatabase.parseElement(value);
            if (element != Element.NONE) {
              defenseElement = element;
            }
          }
          continue;
        } else if (option.equals("EA:")) {
          if (tokens.hasMoreTokens()) {
            value = tokens.nextToken();
            Element element = MonsterDatabase.parseElement(value);
            if (element != Element.NONE) {
              attackElement = element;
            }
          }
          continue;
        } else if (option.equals("Meat:")) {
          if (tokens.hasMoreTokens()) {
            value = tokens.nextToken();
            int dash = value.indexOf("-");
            if (dash >= 0) {
              int minMeat = StringUtilities.parseInt(value.substring(0, dash));
              int maxMeat = StringUtilities.parseInt(value.substring(dash + 1));
              meat = (minMeat + maxMeat) / 2;
            } else {
              meat = StringUtilities.parseInt(value);
            }
          }
          continue;
        } else if (option.equals("SprinkleMin:")) {
          minSprinkles = parseNumeric(tokens);
          continue;
        } else if (option.equals("SprinkleMax:")) {
          maxSprinkles = parseNumeric(tokens);
          continue;
        } else if (option.equals("P:")) {
          if (tokens.hasMoreTokens()) {
            value = tokens.nextToken();
            Phylum num = MonsterDatabase.parsePhylum(value);
            if (num != Phylum.NONE) {
              phylum = num;
            }
          }
          continue;
        } else if (option.equals("Manuel:")) {
          if (tokens.hasMoreTokens()) {
            manuelName = parseString(tokens.nextToken(), tokens);
          }
          continue;
        } else if (option.equals("Wiki:")) {
          if (tokens.hasMoreTokens()) {
            wikiName = parseString(tokens.nextToken(), tokens);
          }
          continue;
        } else if (option.startsWith("\"")) {
          String string = parseString(option, tokens);
          poison = EffectDatabase.getPoisonLevel(string);
          if (poison == Integer.MAX_VALUE) {
            RequestLogger.printLine("Monster: \"" + name + "\": unknown poison type: " + string);
          }
          continue;
        } else if (option.equals("BOSS")) {
          boss = true;
          continue;
        } else if (option.equals("NOBANISH")) {
          noBanish = true;
          continue;
        } else if (option.equals("NOCOPY")) {
          noCopy = true;
          continue;
        } else if (option.equals("WANDERER")) {
          type.add(EncounterType.WANDERER);
          continue;
        } else if (option.equals("ULTRARARE")) {
          type.add(EncounterType.ULTRARARE);
          continue;
        } else if (option.equals("SEMIRARE")) {
          type.add(EncounterType.SEMIRARE);
          continue;
        } else if (option.equals("SUPERLIKELY")) {
          type.add(EncounterType.SUPERLIKELY);
          continue;
        } else if (option.equals("FREE")) {
          type.add(EncounterType.FREE_COMBAT);
          continue;
        } else if (option.equals("NOWANDER")) {
          type.add(EncounterType.NOWANDER);
          continue;
        } else if (option.equals("NOMANUEL")) {
          continue;
        } else if (option.equals("GHOST")) {
          subTypes.add(option.toLowerCase());
          continue;
        } else if (option.equals("SNAKE")) {
          subTypes.add(option.toLowerCase());
          continue;
        } else if (option.equals("DRIPPY")) {
          subTypes.add(option.toLowerCase());
          continue;
        }

        RequestLogger.printLine("Monster: \"" + name + "\": unknown option: " + option);
      } catch (Exception e) {
        // This should not happen.  Therefore, print
        // a stack trace for debug purposes.

        StaticEntity.printStackTrace(e, attributes);
      }

      return null;
    }

    monster =
        new MonsterData(
            name,
            id,
            health,
            attack,
            defense,
            initiative,
            experience,
            scale,
            cap,
            floor,
            mlMult,
            attackElement,
            defenseElement,
            physical,
            meat,
            minSprinkles,
            maxSprinkles,
            phylum,
            poison,
            boss,
            noBanish,
            noCopy,
            type,
            images,
            manuelName,
            wikiName,
            subTypes,
            attributes);

    return monster;
  }

  private static Object parseNumeric(StringTokenizer tokens) {
    if (!tokens.hasMoreTokens()) {
      return null;
    }
    return parseNumeric(tokens, tokens.nextToken());
  }

  private static Object parseDefaultedNumeric(StringTokenizer tokens, int def) {
    if (!tokens.hasMoreTokens()) {
      return null;
    }
    String value = tokens.nextToken();
    if (value.equals("?")) {
      return IntegerPool.get(def);
    }
    return parseNumeric(tokens, value);
  }

  private static Object parseNumeric(StringTokenizer tokens, String value) {
    if (!value.startsWith("[")) {
      return IntegerPool.get(StringUtilities.parseInt(value));
    }
    // Must paste the entire expression back together, since we're
    // splitting the tokens on spaces.
    StringBuilder temp = new StringBuilder(value);
    while (!value.endsWith("]") && tokens.hasMoreTokens()) {
      value = tokens.nextToken();
      temp.append(' ');
      temp.append(value);
    }
    return temp.substring(1, temp.length() - 1);
  }

  private static String parseString(String token, StringTokenizer tokens) {
    if (!token.startsWith("\"")) {
      return "";
    }

    StringBuilder temp = new StringBuilder(token);
    while (!token.endsWith("\"") && tokens.hasMoreTokens()) {
      token = tokens.nextToken();
      temp.append(' ');
      temp.append(token);
    }

    // Remove initial and final quote
    temp.deleteCharAt(0);
    temp.deleteCharAt(temp.length() - 1);

    return temp.toString();
  }

  public static final Element parseElement(final String s) {
    for (Element elem : Element.values()) {
      if (elem.toString().equals(s)) {
        return elem;
      }
    }
    return Element.NONE;
  }

  public static final Phylum parsePhylum(final String s) {
    for (Phylum phylum : Phylum.values()) {
      if (phylum.toString().equals(s)) {
        return phylum;
      }
    }
    return Phylum.NONE;
  }

  public static final boolean contains(final String name) {
    return MonsterDatabase.findMonster(name) != null;
  }
}
