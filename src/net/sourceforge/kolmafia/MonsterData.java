package net.sourceforge.kolmafia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.BountyDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EncounterManager.EncounterType;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.GoalManager;
import net.sourceforge.kolmafia.session.MonsterManuelManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MonsterData extends AdventureResult {

  public static Map<String, Attribute> optionToAttribute = new HashMap<>();

  public enum Attribute {
    // Non-scaling monsters
    ATTACK("Atk:"),
    DEFENSE("Def:"),
    HP("HP:"),
    // Scaling monsters
    SCALE("Scale:"),
    CAP("Cap:"),
    FLOOR("Floor:"),
    // Universal attributes
    ARTICLE("Article:"),
    INITIATIVE("Init:"),
    GROUP("Group:"),
    PHYLUM("P:"),
    MEAT("Meat:"),
    SPRINKLE_MIN("SprinkleMin:"),
    SPRINKLE_MAX("SprinkleMax:"),
    // Monster features
    BOSS("BOSS"),
    NOBANISH("NOBANISH"),
    NOCOPY("NOCOPY"),
    NOMANUEL("NOMANUEL"),
    EA("EA:"),
    ED("ED:"),
    PHYS("Phys:"),
    ELEM("Elem:"),
    // Encounter Types
    WANDERER("WANDERER"),
    ULTRARARE("ULTRARARE"),
    LUCKY("LUCKY"),
    SUPERLIKELY("SUPERLIKELY"),
    FREE("FREE"),
    NOWANDER("NOWANDER"),
    // Subtypes
    GHOST("GHOST"),
    SNAKE("SNAKE"),
    DRIPPY("DRIPPY"),
    // Specialized
    MLMULT("MLMult:"),
    EXPERIENCE("Exp:"),
    POISON("Poison:"),
    ITEM("Item:"),
    SKILL("Skill:"),
    SPELLS("Spell:"),
    MANUEL_NAME("Manuel:"),
    WIKI_NAME("Wiki:");

    private final String option;

    Attribute(String option) {
      this.option = option;
      optionToAttribute.put(option, this);
    }

    public String getOption() {
      return this.option;
    }
  }

  static {
    Attribute[] attributes = Attribute.values();
  }

  // ***********************************************************
  // Parsing an attribute string - from monsters.txt or manually
  // crafted by MonsterManuelManager - into a map from Attribute
  // to value.
  // ***********************************************************

  public static final int DEFAULT_SCALE = 0;
  public static final int DEFAULT_CAP = 10000;
  public static final int DEFAULT_FLOOR = 10;

  public static Map<Attribute, Object> attributeStringToMap(
      final String name, final String attributes) {

    Map<Attribute, Object> attributeMap = new TreeMap<>();

    StringTokenizer tokens = new StringTokenizer(attributes, " ");
    while (tokens.hasMoreTokens()) {
      String option = tokens.nextToken();
      Object value;

      Attribute attribute = MonsterData.optionToAttribute.get(option);

      if (attribute == null) {
        // Special cases:

        if (option.equals("E:")) {
          if (tokens.hasMoreTokens()) {
            String next = tokens.nextToken();
            Element element = parseElement(next);
            if (element != Element.NONE) {
              attributeMap.put(Attribute.EA, element);
              attributeMap.put(Attribute.ED, element);
            }
          }
          continue;
        }

        RequestLogger.printLine("Monster: \"" + name + "\": unknown option: " + option);
        continue;
      }

      // Check if this is a duplicate attribute
      if (attributeMap.containsKey(attribute)) {
        // This is a bug in the data. When we parse it, the second instance
        // will replace the first one. Log it.
        RequestLogger.printLine(
            "Monster: \""
                + name
                + "\" has multiple values for the \""
                + attribute.getOption()
                + "\" attribute.");
      }

      try {
        switch (attribute) {
          case ATTACK:
          case DEFENSE:
          case HP:
          case INITIATIVE:
          case EXPERIENCE:
          case MLMULT:
          case ITEM:
          case SKILL:
          case SPELLS:
          case SPRINKLE_MIN:
          case SPRINKLE_MAX:
          case ELEM:
          case PHYS:
            value = parseNumeric(tokens);
            attributeMap.put(attribute, value);
            continue;
          case SCALE:
            value = parseDefaultedNumeric(tokens);
            attributeMap.put(attribute, value);
            continue;
          case CAP:
            value = parseDefaultedNumeric(tokens);
            attributeMap.put(attribute, value);
            continue;
          case FLOOR:
            value = parseDefaultedNumeric(tokens);
            attributeMap.put(attribute, value);
            continue;
          case EA:
          case ED:
            if (tokens.hasMoreTokens()) {
              String next = tokens.nextToken();
              Element element = parseElement(next);
              if (element != Element.NONE) {
                attributeMap.put(attribute, element);
              }
            }
            continue;
          case GROUP:
            if (tokens.hasMoreTokens()) {
              value = StringUtilities.parseInt(tokens.nextToken());
              attributeMap.put(attribute, value);
            }
            continue;
          case PHYLUM:
            if (tokens.hasMoreTokens()) {
              String next = tokens.nextToken();
              Phylum num = parsePhylum(next);
              if (num != Phylum.NONE) {
                attributeMap.put(attribute, num);
              }
            }
            continue;
          case MEAT:
            if (tokens.hasMoreTokens()) {
              String next = tokens.nextToken();
              if (next.indexOf("-") >= 0) {
                // RequestLogger.printLine("Monster: \"" + name + "\" has meat range " + next);
                // Let user calculate average
                value = next;
              } else {
                value = StringUtilities.parseInt(next);
              }
              attributeMap.put(attribute, value);
            }
            continue;

          case ARTICLE:
            if (tokens.hasMoreTokens()) {
              value = tokens.nextToken();
              attributeMap.put(attribute, value);
            }

          case MANUEL_NAME:
          case WIKI_NAME:
            if (tokens.hasMoreTokens()) {
              value = parseString(tokens.nextToken(), tokens);
              attributeMap.put(attribute, value);
            }
            continue;

          case POISON:
            if (tokens.hasMoreTokens()) {
              String poisonType = parseString(tokens.nextToken(), tokens);
              int poison = EffectDatabase.getPoisonLevel(poisonType);
              if (poison == Integer.MAX_VALUE) {
                RequestLogger.printLine(
                    "Monster: \"" + name + "\": unknown poison type: " + poisonType);
              }
              attributeMap.put(attribute, poison);
            }
            continue;

          case BOSS:
          case NOBANISH:
          case NOCOPY:
          case NOMANUEL:
            attributeMap.put(attribute, true);
            continue;

          case WANDERER:
          case ULTRARARE:
          case LUCKY:
          case SUPERLIKELY:
          case FREE:
          case NOWANDER:
            attributeMap.put(attribute, true);
            continue;

          case GHOST:
          case SNAKE:
          case DRIPPY:
            attributeMap.put(attribute, true);
            continue;
        }
      } catch (Exception e) {
        // This should not happen.  Therefore, print
        // a stack trace for debug purposes.

        StaticEntity.printStackTrace(e, attributes);
      }
    }

    return attributeMap;
  }

  private static Object parseNumeric(StringTokenizer tokens) {
    if (!tokens.hasMoreTokens()) {
      return null;
    }
    return parseNumeric(tokens, tokens.nextToken());
  }

  private static Object parseDefaultedNumeric(StringTokenizer tokens) {
    if (!tokens.hasMoreTokens()) {
      return null;
    }
    String value = tokens.nextToken();
    if (value.equals("?")) {
      return value;
    }
    return parseNumeric(tokens, value);
  }

  private static Object parseNumeric(StringTokenizer tokens, String value) {
    if (!value.startsWith("[")) {
      return StringUtilities.parseInt(value);
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

  // ***********************************************************
  // Converting a map from Attribute to value back into a String of
  // attributes as seen in monsters.txt. This puts the attributes into
  // a "normalized" order.
  // ***********************************************************

  public static String attributeMapToString(final Map<Attribute, Object> attributeMap) {
    // Normalize the order of attributes
    StringBuilder buf = new StringBuilder();

    // Monster flags
    saveKeywordAttribute(Attribute.BOSS, attributeMap, buf);
    saveKeywordAttribute(Attribute.NOBANISH, attributeMap, buf);
    saveKeywordAttribute(Attribute.NOCOPY, attributeMap, buf);
    saveKeywordAttribute(Attribute.NOMANUEL, attributeMap, buf);

    // Encounter Types
    saveKeywordAttribute(Attribute.WANDERER, attributeMap, buf);
    saveKeywordAttribute(Attribute.ULTRARARE, attributeMap, buf);
    saveKeywordAttribute(Attribute.LUCKY, attributeMap, buf);
    saveKeywordAttribute(Attribute.SUPERLIKELY, attributeMap, buf);
    saveKeywordAttribute(Attribute.FREE, attributeMap, buf);
    saveKeywordAttribute(Attribute.NOWANDER, attributeMap, buf);

    saveNumericAttribute(Attribute.ATTACK, attributeMap, buf);
    saveNumericAttribute(Attribute.DEFENSE, attributeMap, buf);
    saveNumericAttribute(Attribute.HP, attributeMap, buf);
    // Scale, Cap, and Floor can be a number or "?"
    saveDefaultedNumericAttribute(Attribute.SCALE, attributeMap, buf);
    saveDefaultedNumericAttribute(Attribute.CAP, attributeMap, buf);
    saveDefaultedNumericAttribute(Attribute.FLOOR, attributeMap, buf);
    saveNumericAttribute(Attribute.MLMULT, attributeMap, buf);
    saveNumericAttribute(Attribute.EXPERIENCE, attributeMap, buf);
    saveNumericAttribute(Attribute.INITIATIVE, attributeMap, buf);
    // Meat can be a number or a range
    saveValueAttribute(Attribute.MEAT, attributeMap, buf);
    saveNumericAttribute(Attribute.SPRINKLE_MIN, attributeMap, buf);
    saveNumericAttribute(Attribute.SPRINKLE_MAX, attributeMap, buf);

    saveValueAttribute(Attribute.GROUP, attributeMap, buf);
    saveValueAttribute(Attribute.PHYLUM, attributeMap, buf);

    // Subtypes
    saveKeywordAttribute(Attribute.GHOST, attributeMap, buf);
    saveKeywordAttribute(Attribute.SNAKE, attributeMap, buf);
    saveKeywordAttribute(Attribute.DRIPPY, attributeMap, buf);

    // Resistances
    saveNumericAttribute(Attribute.PHYS, attributeMap, buf);
    saveNumericAttribute(Attribute.ELEM, attributeMap, buf);

    Object EA = attributeMap.get(Attribute.EA);
    Object ED = attributeMap.get(Attribute.ED);
    if (EA != null && ED != null && EA == ED) {
      buf.append("E: ");
      buf.append(ED);
      buf.append(" ");
    } else {
      saveValueAttribute(Attribute.ED, attributeMap, buf);
      saveValueAttribute(Attribute.EA, attributeMap, buf);
    }

    // Special blocking percentages
    saveNumericAttribute(Attribute.ITEM, attributeMap, buf);
    saveNumericAttribute(Attribute.SKILL, attributeMap, buf);
    saveNumericAttribute(Attribute.SPELLS, attributeMap, buf);

    if (attributeMap.containsKey(Attribute.ARTICLE)) {
      String article = (String) attributeMap.get(Attribute.ARTICLE);
      if (!"".equals(article)) {
        buf.append(Attribute.ARTICLE.getOption());
        buf.append(" ");
        buf.append(article);
        buf.append(" ");
      }
    }

    if (attributeMap.containsKey(Attribute.POISON)) {
      int poison = (int) attributeMap.get(Attribute.POISON);
      String poisonName = EffectDatabase.getPoisonName(poison);
      buf.append(Attribute.POISON.getOption());
      buf.append(" \"");
      buf.append(poisonName);
      buf.append("\" ");
    }

    saveStringAttribute(Attribute.MANUEL_NAME, attributeMap, buf);
    saveStringAttribute(Attribute.WIKI_NAME, attributeMap, buf);

    return buf.toString().trim();
  }

  private static void saveKeywordAttribute(
      Attribute attribute, Map<Attribute, Object> attributeMap, StringBuilder buf) {
    if (attributeMap.containsKey(attribute)) {
      buf.append(attribute.getOption());
      buf.append(" ");
    }
  }

  private static void saveDefaultedNumericAttribute(
      Attribute attribute, Map<Attribute, Object> attributeMap, StringBuilder buf) {
    if (attributeMap.containsKey(attribute)) {
      buf.append(attribute.getOption());
      buf.append(" ");
      Object value = attributeMap.get(attribute);
      if (value instanceof String) {
        String string = (String) value;
        if (string.equals("?")) {
          buf.append(value);
        } else {
          buf.append("[");
          buf.append(value);
          buf.append("]");
        }
      } else {
        buf.append(value);
      }
      buf.append(" ");
    }
  }

  private static void saveNumericAttribute(
      Attribute attribute, Map<Attribute, Object> attributeMap, StringBuilder buf) {
    if (attributeMap.containsKey(attribute)) {
      buf.append(attribute.getOption());
      buf.append(" ");
      Object value = attributeMap.get(attribute);
      if (value instanceof String) {
        buf.append("[");
        buf.append(value);
        buf.append("]");
      } else {
        buf.append(value);
      }
      buf.append(" ");
    }
  }

  private static void saveValueAttribute(
      Attribute attribute, Map<Attribute, Object> attributeMap, StringBuilder buf) {
    if (attributeMap.containsKey(attribute)) {
      buf.append(attribute.getOption());
      buf.append(" ");
      buf.append(attributeMap.get(attribute));
      buf.append(" ");
    }
  }

  private static void saveStringAttribute(
      Attribute attribute, Map<Attribute, Object> attributeMap, StringBuilder buf) {
    if (attributeMap.containsKey(attribute)) {
      buf.append(attribute.getOption());
      buf.append(" \"");
      buf.append(attributeMap.get(attribute));
      buf.append("\" ");
    }
  }

  // ***********************************************************
  // Transforming certain groups of attributes into a collection
  // ***********************************************************

  private EnumSet<EncounterType> attributeMapToEncounterTypes(
      final Map<Attribute, Object> attributeMap) {
    EnumSet<EncounterType> type = EnumSet.noneOf(EncounterType.class);
    if (attributeMap.containsKey(Attribute.WANDERER)) {
      type.add(EncounterType.WANDERER);
    }
    if (attributeMap.containsKey(Attribute.ULTRARARE)) {
      type.add(EncounterType.ULTRARARE);
    }
    if (attributeMap.containsKey(Attribute.LUCKY)) {
      type.add(EncounterType.LUCKY);
    }
    if (attributeMap.containsKey(Attribute.SUPERLIKELY)) {
      type.add(EncounterType.SUPERLIKELY);
    }
    if (attributeMap.containsKey(Attribute.FREE)) {
      type.add(EncounterType.FREE_COMBAT);
    }
    if (attributeMap.containsKey(Attribute.NOWANDER)) {
      type.add(EncounterType.NOWANDER);
    }
    return type;
  }

  private Set<String> attributeMapToSubtypes(final Map<Attribute, Object> attributeMap) {
    Set<String> subTypes = new HashSet<>();
    if (attributeMap.containsKey(Attribute.GHOST)) {
      subTypes.add("ghost");
    }
    if (attributeMap.containsKey(Attribute.SNAKE)) {
      subTypes.add("snake");
    }
    if (attributeMap.containsKey(Attribute.DRIPPY)) {
      subTypes.add("drippy");
    }
    return subTypes;
  }

  // ***********************************************************
  // The attributes of a monster.
  //
  // Those which are "Objects" are allowed to have expressions -
  // surrounded by [] - in their attribute strings.
  //
  // Integer - no expression; the specific value
  // String - expression, brackets removed,  compiled into ...
  // MonsterExpression - compiled expression, evaluated to an int
  // ***********************************************************

  private Object health;
  private Object attack;
  private Object defense;
  private Object initiative;
  private final Object experience;
  private final Object scale;
  private final Object cap;
  private final Object floor;
  private final Object mlMult;
  private Element attackElement;
  private Element defenseElement;
  private Object physicalResistance;
  private Object elementalResistance;
  private int meat;
  private Object minSprinkles;
  private Object maxSprinkles;
  private final int group;
  private final Phylum phylum;
  private final int poison;
  private final boolean boss;
  private final boolean noBanish;
  private final boolean noCopy;
  private final boolean noManuel;
  private boolean transformed; // from CLEESH and such
  private final EnumSet<EncounterType> type;
  private final String image;
  private final String[] images;
  private String manuelName = null;
  private String wikiName = null;
  private String article = null;
  private final Set<String> subTypes;
  private final String attributes;
  private final int beeCount;

  private final ArrayList<AdventureResult> items;
  private final List<Double> pocketRates;

  // The following apply to a specific (cloned) instance of a monster
  private String[] randomModifiers;

  public MonsterData(String name, int id, String[] images, String attributeString) {
    super(AdventureResult.MONSTER_PRIORITY, name);

    Map<Attribute, Object> attributes = attributeStringToMap(name, attributeString);
    EnumSet<EncounterType> type = attributeMapToEncounterTypes(attributes);
    Set<String> subTypes = attributeMapToSubtypes(attributes);

    this.id = id;
    this.image = images.length > 0 ? images[0] : "";
    this.images = images;

    this.attack = attributes.get(Attribute.ATTACK);
    this.defense = attributes.get(Attribute.DEFENSE);
    this.health = attributes.get(Attribute.HP);
    this.initiative = attributes.get(Attribute.INITIATIVE);
    this.scale = getDefaultedNumber(attributes.get(Attribute.SCALE), DEFAULT_SCALE);
    this.cap = getDefaultedNumber(attributes.get(Attribute.CAP), DEFAULT_CAP);
    this.floor = getDefaultedNumber(attributes.get(Attribute.FLOOR), DEFAULT_FLOOR);
    this.experience = attributes.get(Attribute.EXPERIENCE);
    this.mlMult = attributes.get(Attribute.MLMULT);
    this.attackElement = (Element) attributes.getOrDefault(Attribute.EA, Element.NONE);
    this.defenseElement = (Element) attributes.getOrDefault(Attribute.ED, Element.NONE);
    this.physicalResistance = attributes.get(Attribute.PHYS);
    this.elementalResistance = attributes.get(Attribute.ELEM);
    this.meat = getAverageNumber(attributes.get(Attribute.MEAT));
    this.minSprinkles = attributes.get(Attribute.SPRINKLE_MIN);
    this.maxSprinkles = attributes.get(Attribute.SPRINKLE_MAX);
    this.group = (int) attributes.getOrDefault(Attribute.GROUP, 1);
    this.phylum = (Phylum) attributes.getOrDefault(Attribute.PHYLUM, Phylum.NONE);
    this.poison = (int) attributes.getOrDefault(Attribute.POISON, 0);
    this.boss = attributes.containsKey(Attribute.BOSS);
    this.noBanish = attributes.containsKey(Attribute.NOBANISH);
    this.noCopy = attributes.containsKey(Attribute.NOCOPY);
    this.noManuel = attributes.containsKey(Attribute.NOMANUEL);
    this.type = type;
    this.subTypes = subTypes;
    this.manuelName = (String) attributes.get(Attribute.MANUEL_NAME);
    this.wikiName = (String) attributes.getOrDefault(Attribute.WIKI_NAME, name);
    this.article = (String) attributes.get(Attribute.ARTICLE);

    // We could normalize the string.
    //   this.attributes = attributeMapToString(attributes);
    this.attributes = attributeString;

    this.transformed = false;

    int beeCount = 0;
    // Wandering bees don't have a bee count
    if (id < 1075 || id > 1083) {
      for (int i = 0; i < name.length(); ++i) {
        char c = name.charAt(i);
        if (c == 'b' || c == 'B') {
          beeCount++;
        }
      }
    }
    this.beeCount = beeCount;

    this.items = new ArrayList<>();
    this.pocketRates = new ArrayList<>();

    // No random modifiers
    this.randomModifiers = new String[0];
  }

  private Object getDefaultedNumber(Object value, int def) {
    if (value instanceof Integer) {
      return (Integer) value;
    }
    if (value instanceof String) {
      String string = (String) value;
      if (string.equals("?")) {
        return def;
      }
    }
    return value;
  }

  private int getAverageNumber(Object value) {
    if (value instanceof Integer) {
      return (Integer) value;
    }
    if (value instanceof String) {
      String range = (String) value;
      int dash = range.indexOf("-");
      int minMeat = StringUtilities.parseInt(range.substring(0, dash));
      int maxMeat = StringUtilities.parseInt(range.substring(dash + 1));
      return (minMeat + maxMeat) / 2;
    }
    return 0;
  }

  public MonsterData(final MonsterData monster) {
    super(AdventureResult.MONSTER_PRIORITY, monster.getName());

    this.id = monster.id;
    this.health = monster.health;
    this.attack = monster.attack;
    this.defense = monster.defense;
    this.initiative = monster.initiative;
    this.experience = monster.experience;
    this.scale = monster.scale;
    this.cap = monster.cap;
    this.floor = monster.floor;
    this.mlMult = monster.mlMult;
    this.attackElement = monster.attackElement;
    this.defenseElement = monster.defenseElement;
    this.physicalResistance = monster.physicalResistance;
    this.elementalResistance = monster.elementalResistance;
    this.meat = monster.meat;
    this.minSprinkles = monster.minSprinkles;
    this.maxSprinkles = monster.maxSprinkles;
    this.group = monster.group;
    this.phylum = monster.phylum;
    this.poison = monster.poison;
    this.boss = monster.boss;
    this.noBanish = monster.noBanish;
    this.noCopy = monster.noCopy;
    this.transformed = monster.transformed;
    this.type = monster.type;
    this.image = monster.image;
    this.images = monster.images;
    this.manuelName = monster.manuelName;
    this.wikiName = monster.wikiName;
    this.subTypes = monster.subTypes;
    this.attributes = monster.attributes;
    this.noManuel = monster.noManuel;
    this.beeCount = monster.beeCount;
    this.items = monster.items;
    this.pocketRates = monster.pocketRates;
    this.randomModifiers = monster.randomModifiers;
  }

  // Interface for MonsterDatabase to add item drops while parsing monsters.txt.

  public void clearItems() {
    this.items.clear();
  }

  public void addItem(final AdventureResult item) {
    this.items.add(item);
  }

  public void doneWithItems() {
    this.items.trimToSize();

    // Calculate the probability that an item will be yoinked
    // based on the integral provided by Buttons on the HCO forums.
    // http://forums.hardcoreoxygenation.com/viewtopic.php?t=3396

    double probability = 0.0;
    double[] coefficients = new double[this.items.size()];

    for (int i = 0; i < this.items.size(); ++i) {
      coefficients[0] = 1.0;
      for (int j = 1; j < coefficients.length; ++j) {
        coefficients[j] = 0.0;
      }

      for (int j = 0; j < this.items.size(); ++j) {
        AdventureResult item = this.items.get(j);
        probability = (item.getCount() >> 16) / 100.0;
        switch ((char) item.getCount() & 0xFFFF) {
          case 'p':
            if (probability == 0.0) { // assume some probability of a pickpocket-only item
              probability = 0.05;
            }
            break;
          case 'n':
          case 'c':
          case 'f':
          case 'b':
            probability = 0.0;
            break;
        }

        if (i == j) {
          for (int k = 0; k < coefficients.length; ++k) {
            coefficients[k] = coefficients[k] * probability;
          }
        } else {
          for (int k = coefficients.length - 1; k >= 1; --k) {
            coefficients[k] = coefficients[k] - probability * coefficients[k - 1];
          }
        }
      }

      probability = 0.0;

      for (int j = 0; j < coefficients.length; ++j) {
        probability += coefficients[j] / (j + 1);
      }

      this.pocketRates.add(probability);
    }
  }

  // ***********************************************************
  // Last mask worn by current monster - Disguises Delimit
  // ***********************************************************

  public static String lastMask = null;

  // ***********************************************************
  // Random modifers - One Crazy Random Summer or dice gear
  // ***********************************************************

  public static final List<String> lastRandomModifiers = new ArrayList<>();

  private static final String[][] crazyModifierMapping = {
    {"annoying", "annoying"},
    {"artisanal", "artisanal"},
    {"askew", "askew"},
    {"blinking", "phase-shifting"},
    {"blue", "ice-cold"},
    {"blurry", "blurry"},
    {"bouncing", "bouncing"},
    {"broke", "broke"},
    {"clingy", "clingy"},
    {"cloned", "cloned"},
    {"cloud", "cloud-based"},
    {"clowny", "clowning"},
    {"crimbo", "yuletide"},
    {"curse", "cursed"},
    {"disguised", "disguised"},
    {"drunk", "drunk"},
    {"electric", "electrified"},
    {"flies", "filthy"},
    {"flip", "Australian"},
    {"floating", "floating"},
    {"fragile", "fragile"},
    {"fratty", "fratty"},
    {"frozen", "frozen"},
    {"generous", "generous"},
    {"ghostly", "ghostly"},
    {"gray", "spooky"},
    {"green", "stinky"},
    {"haunted", "haunted"},
    {"hilarious", "hilarious"},
    {"hopping", "hopping-mad"},
    {"hot", "hot"},
    {"huge", "huge"},
    {"invisible", "invisible"},
    {"jitter", "jittery"},
    {"lazy", "lazy"},
    {"leet", "1337"},
    {"mirror", "left-handed"},
    {"narcissistic", "narcissistic"},
    {"obscene", "obscene"},
    {"optimal", "optimal"},
    {"patriotic", "American"},
    {"pixellated", "pixellated"},
    {"pulse", "throbbing"},
    {"purple", "sleazy"},
    {"quacking", "quacking"},
    {"rainbow", "tie-dyed"},
    {"red", "red-hot"},
    {"rotate", "twirling"},
    {"shakes", "shaky"},
    {"short", "short"},
    {"shy", "shy"},
    {"skinny", "skinny"},
    {"sparkling", "solid gold"},
    {"spinning", "cartwheeling"},
    {"stingy", "stingy"},
    {"swearing", "foul-mouthed"},
    {"ticking", "ticking"},
    {"tiny", "tiny"},
    {"turgid", "turgid"},
    {"unlucky", "unlucky"},
    {"unstoppable", "unstoppable"},
    {"untouchable", "untouchable"},
    {"wet", "wet"},
    {"wobble", "dancin'"},
    {"xray", "negaverse"},
    {"yellow", "cowardly"},
    {"zoom", "restless"},
  };

  public static final Map<String, String> crazySummerModifiers = new HashMap<>();

  static {
    for (String[] mapping : MonsterData.crazyModifierMapping) {
      MonsterData.crazySummerModifiers.put(mapping[0], mapping[1]);
    }
  }

  private static final String[][] cosmeticModifiers = {
    {"askew", null},
    {"blinking", null},
    {"blue", null},
    {"blurry", null},
    {"bouncing", null},
    {"cgreen", null},
    {"crimbo", null},
    {"deeppurple", null},
    {"drunk", "ol_drunk.gif:0:0"},
    {"electric", "ol_electric.gif:0:-0"},
    {"flies", "ol_flies.gif:0:-0"},
    {"flip", null},
    {"floating", null},
    {"frozen", "ice_overlay.png:0:0"},
    {"ghostly", null},
    {"gold", null},
    {"gray", null},
    {"green", null},
    {"haunted", "ol_haunted.gif:0:-0"},
    {"hopping", null},
    {"huge", null},
    {"jitter", null},
    {"lazy", null},
    {"mirror", null},
    {"obscene", "ol_censored.png:0:0"},
    {"patriotic", null},
    {"pixellated", null},
    {"pulse", null},
    {"purple", null},
    {"quacking", "ol_quack.gif:0:0"},
    {"rainbow", null},
    {"red", null},
    {"rotate", null},
    {"shakes", null},
    {"short", null},
    {"shy", null},
    {"skinny", null},
    {"somersault", null},
    {"sparkling", "ol_sparkles.gif:0:0"},
    {"spinning", null},
    {"swearing", "ol_qbert.gif:0:0"},
    {"wet", "water5.png:0:0"},
    {"wobble", null},
    {"xray", null},
    {"yellow", null},
    {"zoom", null},
  };

  public static final Map<String, String> cosmeticModifierImages = new HashMap<>();

  static {
    for (String[] mapping : MonsterData.cosmeticModifiers) {
      MonsterData.cosmeticModifierImages.put(mapping[0], mapping[1]);
    }
    MonsterData.cosmeticModifierImages.put("powerPixel", null);
    MonsterData.cosmeticModifierImages.put("drippy", null);
  }

  private static final String[] extraModifierNames = {"powerPixel"};

  public static final Set<String> extraModifiers = new HashSet<>();

  static {
    MonsterData.extraModifiers.addAll(Arrays.asList(MonsterData.extraModifierNames));
  }

  // ***********************************************************
  // Special handling requiring cloning the monster to set some
  // attributes for a specific instance of it.
  // ***********************************************************

  public MonsterData handleMonsterLevel() {
    // If we tracked nostagger, stunresist, and start-of-combat
    int physRes = Math.min((int) Math.floor(ML() / 2.5), 50);
    if (this.scale != null && physRes < 0) {
      physRes = 0;
    }

    if (physRes <= this.getPhysicalResistance()) {
      return this;
    }

    // elemental damage, this would be the place to put it.
    try {
      MonsterData monster = (MonsterData) this.clone();
      int monsterPhysRes = monster.getPhysicalResistance();
      if (monsterPhysRes == 0) {
        monster.physicalResistance = physRes;
      } else {
        monster.physicalResistance = Math.max(physRes, monsterPhysRes);
      }
      return monster;
    } catch (CloneNotSupportedException e) {
      // This should not happen. Hope for the best.
      return this;
    }
  }

  public MonsterData handleRandomModifiers() {
    if (MonsterData.lastRandomModifiers.isEmpty()) {
      return this;
    }

    String[] modifiers = new String[MonsterData.lastRandomModifiers.size()];
    MonsterData.lastRandomModifiers.toArray(modifiers);

    if (modifiers == null || modifiers.length == 0) {
      return this;
    }

    // Clone the monster so we don't munge the template
    MonsterData monster;
    try {
      monster = (MonsterData) this.clone();
    } catch (CloneNotSupportedException e) {
      // This should not happen. Hope for the best.
      return this;
    }

    // Save the modifiers for use by scripts
    monster.randomModifiers = modifiers;

    // Iterate over them and modify the base values
    for (String modifier : modifiers) {
      if (modifier.equals("askew")) {
        monster.attack = monster.getRawAttack() * 11 / 10;
      } else if (modifier.equals("bouncing")) {
        monster.attack = monster.getRawAttack() * 3 / 2;
      } else if (modifier.equals("broke")) {
        monster.meat = 5;
      } else if (modifier.equals("cloned")) {
        monster.health = monster.getRawHP() * 2;
        monster.attack = monster.getRawAttack() * 2;
        monster.defense = monster.getRawDefense() * 2;
      } else if (modifier.equals("dancin'")) {
        monster.defense = monster.getRawDefense() * 3 / 2;
      } else if (modifier.equals("filthy")) {
        // Stench Aura
      } else if (modifier.equals("floating")) {
        monster.defense = monster.getRawDefense() * 3 / 2;
      } else if (modifier.equals("foul-mouthed")) {
        // Sleaze Aura
      } else if (modifier.equals("fragile")) {
        monster.health = 1;
      } else if (modifier.equals("frozen")) {
        monster.attackElement = Element.COLD;
        monster.defenseElement = Element.COLD;
      } else if (modifier.equals("ghostly")) {
        if (monster.getPhysicalResistance() == 0) {
          monster.physicalResistance = 90;
        }
      } else if (modifier.equals("haunted")) {
        // Spooky Aura
      } else if (modifier.equals("hot")) {
        // Hot Aura
      } else if (modifier.equals("huge")) {
        monster.health = monster.getRawHP() * 2;
        monster.attack = monster.getRawAttack() * 2;
        monster.defense = monster.getRawDefense() * 2;
      } else if (modifier.equals("ice-cold")) {
        monster.attackElement = Element.COLD;
        monster.defenseElement = Element.COLD;
      } else if (modifier.equals("left-handed")) {
        Object originalAttack = monster.attack;
        Object originalDefense = monster.defense;
        monster.attack = originalDefense;
        monster.defense = originalAttack;
      } else if (modifier.equals("red-hot")) {
        monster.attackElement = Element.HOT;
        monster.defenseElement = Element.HOT;
      } else if (modifier.equals("short")) {
        monster.health = monster.getRawHP() / 2;
        monster.defense = monster.getRawDefense() * 2;
      } else if (modifier.equals("skinny")) {
        monster.health = monster.getRawHP() / 2;
        monster.defense = monster.getRawDefense() / 2;
      } else if (modifier.equals("sleazy")) {
        monster.attackElement = Element.SLEAZE;
        monster.defenseElement = Element.SLEAZE;
      } else if (modifier.equals("solid gold")) {
        monster.meat = 1000;
      } else if (modifier.equals("spooky")) {
        monster.attackElement = Element.SPOOKY;
        monster.defenseElement = Element.SPOOKY;
      } else if (modifier.equals("stinky")) {
        monster.attackElement = Element.STENCH;
        monster.defenseElement = Element.STENCH;
      } else if (modifier.equals("throbbing")) {
        monster.health = monster.getRawHP() * 2;
      } else if (modifier.equals("tiny")) {
        monster.health = monster.getRawHP() / 10;
        monster.attack = monster.getRawAttack() / 10;
        monster.defense = monster.getRawDefense() / 10;
      } else if (modifier.equals("turgid")) {
        monster.health = monster.getRawHP() * 5;
      } else if (modifier.equals("unlucky")) {
        monster.health = 13;
        monster.attack = 13;
        monster.defense = 13;
      } else if (modifier.equals("wet")) {
        // Cold Aura
      }

      // Non-OCRS modifiers
      else if (modifier.equals("mutant")) {
        monster.health = monster.getRawHP() * 6 / 5;
        monster.attack = monster.getRawAttack() * 6 / 5;
        monster.defense = monster.getRawDefense() * 6 / 5;
      } else if (this.scale == null
          && (modifier.equals("Mr. mask") || modifier.equals("Bonerdagon mask"))) {
        monster.health = monster.getRawHP() * 2;
        monster.attack = monster.getRawAttack() * 2;
        monster.defense = monster.getRawDefense() * 2;
      } else if (modifier.equals("ninja mask")) {
        monster.initiative = 10000;
      } else if (this.scale == null && modifier.equals("opera mask")) {
        monster.attack = monster.getRawAttack() * 2;
      } else if (this.scale == null && modifier.equals("bandit mask")) {
        monster.defense = monster.getRawDefense() * 4;
      } else if (modifier.equals("fencing mask")) {
        if (monster.getPhysicalResistance() == 0) {
          monster.physicalResistance = 90;
        }
        if (monster.getElementalResistance() == 0) {
          monster.elementalResistance = 90;
        }
      } else if (this.scale == null && modifier.equals("Naughty Sorceress mask")) {
        monster.health = monster.getRawHP() * 3;
        monster.attack = monster.getRawAttack() * 3;
        monster.defense = monster.getRawDefense() * 3;
      }
    }

    return monster;
  }

  // ***********************************************************
  // Accessors for the various attributes of a monster,
  // ***********************************************************

  private MonsterExpression compile(Object expr) {
    return MonsterExpression.getInstance((String) expr, this.getName());
  }

  private int evaluate(Object obj, int value) {
    if (obj != null) {
      if (obj instanceof Integer) {
        return (Integer) obj;
      }
      if (obj instanceof String) {
        obj = compile(obj);
      }
      if (obj instanceof MonsterExpression) {
        return (int) (((MonsterExpression) obj).eval());
      }
    }
    return value;
  }

  public int ML() {
    if (this.transformed) {
      return 0;
    }
    /* For brevity, and to handle the possible future need for
    asking for speculative monster stats */
    return KoLCharacter.getMonsterLevelAdjustment() * evaluate(this.mlMult, 1);
  }

  public boolean scales() {
    return this.scale != null;
  }

  public int getScale() {
    return evaluate(this.scale, 0);
  }

  public int getCap() {
    return evaluate(this.cap, 0);
  }

  public int getFloor() {
    return evaluate(this.floor, 0);
  }

  private double getBeeosity() {
    return 1.0 + (KoLCharacter.inBeecore() ? (this.beeCount / 5.0) : 0.0);
  }

  public int getId() {
    return this.id;
  }

  public void setId(int newId) {
    this.id = newId;
  }

  public String getManuelName() {
    return this.manuelName == null ? this.name : this.manuelName;
  }

  public void setManuelName(final String manuelName) {
    this.manuelName = manuelName;
  }

  public String getArticle() {
    return this.article == null ? "" : this.article;
  }

  public int getHP() {
    if (this.scale != null && this.health == null) {
      int scale = evaluate(this.scale, MonsterData.DEFAULT_SCALE);
      int hp = KoLCharacter.getAdjustedMuscle() + scale;
      int cap = evaluate(this.cap, MonsterData.DEFAULT_CAP);
      int floor = (int) (evaluate(this.floor, MonsterData.DEFAULT_FLOOR) * 0.75);
      int ml = ML();

      hp = Math.min(hp, cap);
      ml = Math.max(ml, 0);
      hp = (int) Math.floor((hp + ml) * 0.75 * getBeeosity());
      hp = Math.max(hp, floor);
      return Math.max(1, hp);
    }
    if (this.health == null) {
      return 0;
    }
    if (this.health instanceof Integer) {
      int hp = (Integer) this.health;

      if (hp == 0
          && (this.attack == null
              || (this.attack instanceof Integer && (Integer) this.attack == 0))) {
        // The monster is unknown, so do not apply modifiers
        return 0;
      }
      if (KoLCharacter.inBigcore()) {
        hp += 150;
      }
      return (int) Math.floor(Math.max(1, hp + ML()) * getBeeosity());
    }
    if (this.health instanceof String) {
      this.health = compile(this.health);
    }
    return Math.max(1, (int) (((MonsterExpression) this.health).eval() * getBeeosity()));
  }

  public int getRawHP() {
    if (this.scale != null && this.health == null) {
      int scale = evaluate(this.scale, MonsterData.DEFAULT_SCALE);
      int hp = KoLCharacter.getAdjustedMuscle() + scale;
      int cap = evaluate(this.cap, MonsterData.DEFAULT_CAP);
      int floor = evaluate(this.floor, MonsterData.DEFAULT_FLOOR);

      hp = hp > cap ? cap : Math.max(hp, floor);
      return (int) Math.floor(Math.max(1, (hp) * 0.75));
    }
    if (this.health == null) {
      return -1;
    }
    return Math.max(1, evaluate(this.health, 1));
  }

  public int getBaseHP() {
    return this.scale != null
        ? -1
        : this.health == null ? 0 : this.health instanceof Integer ? (Integer) (this.health) : -1;
  }

  public int getAttack() {
    if (this.scale != null && this.attack == null) {
      int scale = evaluate(this.scale, MonsterData.DEFAULT_SCALE);
      int attack = KoLCharacter.getAdjustedMoxie() + scale;
      int cap = evaluate(this.cap, MonsterData.DEFAULT_CAP);
      int floor = evaluate(this.floor, MonsterData.DEFAULT_FLOOR);
      attack = Math.min(attack, cap);
      int ml = ML();
      ml = Math.max(ml, 0);

      attack = (int) Math.floor((attack + ml) * getBeeosity());
      attack = Math.max(attack, floor);
      return Math.max(1, attack);
    }
    if (this.attack == null) {
      return 0;
    }
    if (this.attack instanceof Integer) {
      int attack = (Integer) this.attack;
      if (attack == 0 && (Integer) this.health == 0) {
        // The monster is unknown, so do not apply modifiers
        return 0;
      }
      if (KoLCharacter.inBigcore()) {
        // The bonus attack from BIG cannot raise a monster's attack above 300
        attack = Math.min(attack + 150, Math.max(300, attack));
      }
      return (int) Math.floor(Math.max(1, attack + ML()) * getBeeosity());
    }
    if (this.attack instanceof String) {
      this.attack = compile(this.attack);
    }
    return Math.max(1, (int) (((MonsterExpression) this.attack).eval() * getBeeosity()));
  }

  public int getRawAttack() {
    if (this.scale != null && this.attack == null) {
      int scale = evaluate(this.scale, MonsterData.DEFAULT_SCALE);
      int attack = KoLCharacter.getAdjustedMoxie() + scale;
      int cap = evaluate(this.cap, MonsterData.DEFAULT_CAP);
      int floor = evaluate(this.floor, MonsterData.DEFAULT_FLOOR);
      attack = attack > cap ? cap : Math.max(attack, floor);
      return Math.max(1, attack);
    }

    if (this.attack == null) {
      return -1;
    }
    int attack = evaluate(this.attack, 0);
    return Math.max(1, attack);
  }

  public int getBaseAttack() {
    return this.scale != null
        ? -1
        : this.attack == null ? 0 : this.attack instanceof Integer ? (Integer) (this.attack) : -1;
  }

  public int getDefense() {
    double reduceMonsterDefense =
        KoLCharacter.currentNumericModifier(Modifiers.REDUCE_ENEMY_DEFENSE) / 100;
    if (this.scale != null && this.defense == null) {
      int scale = evaluate(this.scale, MonsterData.DEFAULT_SCALE);
      int defense = KoLCharacter.getAdjustedMuscle() + scale;
      int cap = evaluate(this.cap, MonsterData.DEFAULT_CAP);
      int floor = evaluate(this.floor, MonsterData.DEFAULT_FLOOR);

      defense = Math.min(defense, cap);
      int ml = ML();
      ml = Math.max(ml, 0);
      defense = (int) Math.floor((defense + ml) * getBeeosity());
      defense = Math.max(defense, floor);
      return (int) Math.floor(Math.max(1, defense * (1 - reduceMonsterDefense)));
    }
    if (this.defense == null) {
      return 0;
    }
    if (this.defense instanceof Integer) {
      int defense = (Integer) this.defense;
      if (defense == 0 && (Integer) this.health == 0) {
        // The monster is unknown, so do not apply modifiers
        return 0;
      }
      if (KoLCharacter.inBigcore()) {
        // The bonus defense from BIG cannot raise a monster's defense above 300
        defense = Math.min(defense + 150, Math.max(300, defense));
      }
      return (int)
          Math.floor(Math.max(1, defense + ML()) * getBeeosity() * (1 - reduceMonsterDefense));
    }
    if (this.defense instanceof String) {
      this.defense = compile(this.defense);
    }
    return Math.max(
        1,
        (int)
            (((MonsterExpression) this.defense).eval()
                * getBeeosity()
                * (1 - reduceMonsterDefense)));
  }

  public int getRawDefense() {
    if (this.scale != null && this.defense == null) {
      int scale = evaluate(this.scale, MonsterData.DEFAULT_SCALE);
      int defense = KoLCharacter.getAdjustedMuscle() + scale;
      int cap = evaluate(this.cap, MonsterData.DEFAULT_CAP);
      int floor = evaluate(this.floor, MonsterData.DEFAULT_FLOOR);
      defense = defense > cap ? cap : Math.max(defense, floor);
      return (int) Math.floor(Math.max(1, defense));
    }
    if (this.defense == null) {
      return -1;
    }
    return Math.max(1, evaluate(this.defense, 0));
  }

  public int getBaseDefense() {
    return this.scale != null
        ? -1
        : this.defense == null
            ? 0
            : this.defense instanceof Integer ? (Integer) (this.defense) : -1;
  }

  public int getRawInitiative() {
    return evaluate(this.initiative, 0);
  }

  public int getBaseInitiative() {
    return this.initiative == null
        ? 0
        : this.initiative instanceof Integer ? (Integer) (this.initiative) : -1;
  }

  public int getInitiative() {
    return this.getInitiative(KoLCharacter.getMonsterLevelAdjustment());
  }

  public int getInitiative(final int monsterLevel) {
    int baseInit = this.getRawInitiative();
    if (baseInit == -1 || baseInit == 10000 || baseInit == -10000) {
      return baseInit;
    } else {
      return baseInit + initPenalty(monsterLevel);
    }
  }

  public int getJumpChance() {
    if (this.initiative == null) {
      return -1;
    }
    return this.getJumpChance(
        (int) KoLCharacter.getInitiativeAdjustment(), KoLCharacter.getMonsterLevelAdjustment());
  }

  public int getJumpChance(final int initBonus) {
    return this.getJumpChance(initBonus, KoLCharacter.getMonsterLevelAdjustment());
  }

  public int getJumpChance(final int initBonus, final int monsterLevel) {
    int monsterInit = this.getInitiative(monsterLevel);
    if (monsterInit == 10000) {
      return 0;
    } else if (monsterInit == -10000) {
      return 100;
    }
    int charInit = initBonus;
    // Overclocked helps against Source Monsters
    if (this.name.contains("Source Agent") && KoLCharacter.hasSkill("Overclocked")) {
      charInit += 200;
    }
    int jumpChance =
        100
            - monsterInit
            + charInit
            + Math.max(0, KoLCharacter.getBaseMainstat() - this.getAttack());
    return jumpChance > 100 ? 100 : Math.max(jumpChance, 0);
  }

  public static final int initPenalty(final int monsterLevel) {
    return monsterLevel <= 20
        ? 0
        : monsterLevel <= 40
            ? (monsterLevel - 20)
            : monsterLevel <= 60
                ? (20 + 2 * (monsterLevel - 40))
                : monsterLevel <= 80
                    ? (60 + 3 * (monsterLevel - 60))
                    : monsterLevel <= 100
                        ? (120 + 4 * (monsterLevel - 80))
                        : (200 + 5 * (monsterLevel - 100));
  }

  public Element getAttackElement() {
    return this.attackElement;
  }

  public Element getDefenseElement() {
    return this.defenseElement;
  }

  public int getPhysicalResistance() {
    return evaluate(this.physicalResistance, 0);
  }

  public int getElementalResistance() {
    return evaluate(this.elementalResistance, 0);
  }

  public int getMinMeat() {
    int variation = (int) Math.max(1, Math.floor(this.meat * 0.2));
    return this.meat > 0 ? this.meat - variation : 0;
  }

  public int getBaseMeat() {
    return this.meat;
  }

  public int getMaxMeat() {
    int variation = (int) Math.max(1, Math.floor(this.meat * 0.2));
    return this.meat > 0 ? this.meat + variation : 0;
  }

  public int getMinSprinkles() {
    return evaluate(this.minSprinkles, 0);
  }

  public int getMaxSprinkles() {
    return evaluate(this.maxSprinkles, 0);
  }

  public int getGroup() {
    return this.group;
  }

  public Phylum getPhylum() {
    return this.phylum;
  }

  public int getPoison() {
    return this.poison;
  }

  public boolean isBoss() {
    return this.boss;
  }

  public boolean isNoBanish() {
    return this.noBanish;
  }

  public boolean isNoCopy() {
    return this.noCopy;
  }

  public boolean isDrippy() {
    return this.subTypes.contains("drippy");
  }

  public EnumSet<EncounterType> getType() {
    // Only the first 10 Snowmen are free
    if (this.name.equals("X-32-F Combat Training Snowman")
        && Preferences.getInteger("_snojoFreeFights") < 10) {
      EnumSet<EncounterType> temp = this.type.clone();
      temp.add(EncounterType.FREE_COMBAT);
      return temp;
    }
    return this.type;
  }

  public String getImage() {
    return this.image == null ? "" : this.image;
  }

  public String[] getImages() {
    return this.images == null ? new String[0] : this.images;
  }

  public boolean hasImage(final String test) {
    for (String image : this.getImages()) {
      if (image.equals(test)) {
        return true;
      }
    }
    return false;
  }

  public Set<String> getSubTypes() {
    return this.subTypes;
  }

  public String getAttributes() {
    return this.attributes == null ? "" : this.attributes;
  }

  public String getWikiName() {
    return this.wikiName;
  }

  public String[] getRandomModifiers() {
    return this.randomModifiers == null ? new String[0] : this.randomModifiers;
  }

  public List<AdventureResult> getItems() {
    return this.items;
  }

  public List<Double> getPocketRates() {
    return this.pocketRates;
  }

  public boolean shouldSteal() {
    // If the player has an acceptable dodge rate or
    // then steal anything.

    if (this.willUsuallyDodge(0)) {
      return this.shouldSteal(this.items);
    }

    // Otherwise, only steal from monsters that drop
    // something on your conditions list.

    return this.shouldSteal(GoalManager.getGoals());
  }

  private boolean shouldSteal(final List<AdventureResult> checklist) {
    double dropModifier = AreaCombatData.getDropRateModifier();

    for (AdventureResult item : checklist) {
      if (this.shouldStealItem(item, dropModifier)) {
        return true;
      }
    }

    return false;
  }

  private boolean shouldStealItem(AdventureResult item, final double dropModifier) {
    if (!item.isItem()) {
      return false;
    }

    int itemIndex = this.items.indexOf(item);

    // If the monster drops this item, then return true
    // when the drop rate is less than 100%.

    if (itemIndex != -1) {
      item = this.items.get(itemIndex);
      switch ((char) item.getCount() & 0xFFFF) {
        case 'p':
          return true;
        case 'n':
        case 'c':
        case 'f':
        case 'b':
          return false;
        default:
          return (item.getCount() >> 16) * dropModifier < 100.0;
      }
    }

    // If the item does not drop, check to see if maybe
    // the monster drops one of its ingredients.

    AdventureResult[] subitems = ConcoctionDatabase.getStandardIngredients(item.getItemId());
    if (subitems.length < 2) {
      return false;
    }

    for (AdventureResult subitem : subitems) {
      if (this.shouldStealItem(subitem, dropModifier)) {
        return true;
      }
    }

    // The monster doesn't drop the item or any of its
    // ingredients.

    return false;
  }

  public double getExperience() {
    int xpMultiplier = 1;
    if (KoLCharacter.hasEquipped(
            ItemPool.get(ItemPool.MAKESHIFT_GARBAGE_SHIRT, 1), EquipmentManager.SHIRT)
        && Preferences.getInteger("garbageShirtCharge") > 0) {
      xpMultiplier = 2;
    }
    if (this.scale != null && this.experience == null) {
      int scale = evaluate(this.scale, MonsterData.DEFAULT_SCALE);
      int experience = KoLCharacter.getAdjustedMainstat() + scale;
      int cap = evaluate(this.cap, MonsterData.DEFAULT_CAP);
      int floor = evaluate(this.floor, MonsterData.DEFAULT_FLOOR);
      experience = experience > cap ? cap : Math.max(experience, floor);
      int ml = ML();
      ml = Math.max(ml, 0);
      return Math.max(1, (experience / 8.0 + ml / 6.0) * xpMultiplier);
    }
    if (this.experience == null) {
      return Math.max((this.getAttack() / this.getBeeosity() - ML()) / 8.0 + ML() / 6.0, 0)
          * xpMultiplier;
    }
    return evaluate(this.experience, 0) * xpMultiplier / 2.0;
  }

  public boolean willUsuallyMiss() {
    return this.willUsuallyMiss(0);
  }

  public boolean willUsuallyDodge(final int offenseModifier) {
    int dodgeRate = KoLCharacter.getAdjustedMoxie() - (this.getAttack() + offenseModifier) - 6;
    return dodgeRate > 0;
  }

  public boolean willUsuallyMiss(final int defenseModifier) {
    int hitStat = EquipmentManager.getAdjustedHitStat();

    return AreaCombatData.hitPercent(hitStat - defenseModifier, this.getDefense()) <= 50.0;
  }

  public void appendItemDrops(StringBuilder buffer) {
    boolean first = true;

    for (AdventureResult item : this.items) {
      buffer.append(first ? "<br />Item Drops: " : ", ");
      first = false;

      int rate = item.getCount() >> 16;
      buffer.append(item.getName());
      switch ((char) item.getCount() & 0xFFFF) {
        case 'p':
          buffer.append(" (");
          buffer.append(rate);
          buffer.append(" pp only)");
          break;
        case 'n':
          buffer.append(" (");
          buffer.append(rate);
          buffer.append(" no pp)");
          break;
        case 'c':
          buffer.append(" (");
          buffer.append(rate);
          buffer.append(" cond)");
          break;
        case 'f':
          buffer.append(" (");
          buffer.append(rate);
          buffer.append(" no mod)");
          break;
        case 'a':
          buffer.append(" (stealable accordion)");
          break;
        default:
          buffer.append(" (");
          buffer.append(rate);
          buffer.append(")");
      }
    }

    String bounty = BountyDatabase.getNameByMonster(this.getName());
    if (bounty != null) {
      buffer.append(first ? "<br />Item Drops: " : ", ");
      first = false;
      buffer.append(bounty);
      buffer.append(" (bounty)");
    }
  }

  public MonsterData transform() {
    // Clone the monster so we don't munge the template
    MonsterData monster;
    try {
      monster = (MonsterData) this.clone();
    } catch (CloneNotSupportedException e) {
      // This should not happen. Hope for the best.
      return this;
    }
    // For monsters that have been CLEESHed or similar
    monster.transformed = true;
    return monster;
  }

  // </a></center><p><a name='mon554'></a><table width=95%><tr><td colspan=6 height=1
  // bgcolor=black></td></tr><tr><td rowspan=4 valign=top width=100><img
  // src=https://s3.amazonaws.com/images.kingdomofloathing.com/adventureimages/gremlinamc.gif
  // width=100></td><td width=30><img
  // src=https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/nicesword.gif width=30
  // height=30 alt="Attack Power (approximate)" title="Attack Power (approximate)"></td><td width=50
  // valign=center align=left><b><font size=+2>170</font></b></td><td width=30><img
  // src=https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/statue.gif alt="This
  // monster is a Humanoid" title="This monster is a Humanoid" width=30 height=30></td><td rowspan=4
  // width=10></td><td rowspan=4 valign=top class=small><b><font size=+2>A.M.C.
  // gremlin</font></b><ul><li>Some researchers believe the AMC Gremlin to be evidence that gremlins
  // are a degenerate offshoot of the Crimbo Elf, due to their mechanical skills. However, their
  // research equipment usually falls apart before they get a chance to publish.<li>Be careful never
  // to feed gremlins after midnight. And by 'feed', I mean "allow them to chew on your
  // face".<li>People make snarky jokes about time zones and so on, but the gremlin prohibition on
  // feeding is obviously based on local time, because of their sensitivity to sunlight -- don't
  // feed them between midnight and sunrise. How hard was that, Mr. Sarcasm?</ul></td></tr><tr><td
  // width=30><img
  // src=https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/whiteshield.gif width=30
  // height=30 alt="Defense (approximate)" title="Defense (approximate)"></td><td width=50
  // valign=center align=left><b><font size=+2>153</font></b></td><td width=30><img
  // src=https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/circle.gif width=30
  // height=30 alt="This monster has no particular elemental alignment." title="This monster has no
  // particular elemental alignment."></td></tr><tr><td width=30><img
  // src=https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/hp.gif width=30 height=30
  // alt="Hit Points (approximate)" title="Hit Points (approximate)"></td><td width=50 valign=center
  // align=left><b><font size=+2>170</font></b></td><td width=30><img
  // src=https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/watch.gif alt="Initiative
  // +60%" title="Initiative +60%" width=30 height=30></td></tr><tr><td></td><td></td></tr></table>

  public String craftDescription(final int variant, MonsterData stats) {
    if (stats == null) {
      stats = this;
    }

    StringBuilder buffer = new StringBuilder();

    String imageServerPath = KoLmafia.imageServerPath();

    buffer.append("<html><head></head><body>");

    // Craft the table
    buffer.append("<table>");

    // *** Row 1 ***
    buffer.append("<tr>");

    // The image is first, spanning 4 rows
    {
      buffer.append("<td rowspan=4 valign=top style=\"max-width:350;\">");
      buffer.append("<img src=");
      buffer.append(imageServerPath);

      // Allow variants of image
      int variants = stats.images.length;
      String image =
          variants < 2 ? stats.image : variant < variants ? stats.images[variant] : stats.image;

      if (!image.contains("/")) {
        buffer.append("adventureimages/");
      }
      buffer.append(image);
      buffer.append(" style=\"max-width:350;\">");
      buffer.append("</td>");
    }

    // Attack Power
    {
      buffer.append("<td width=30>");
      buffer.append("<img src=");
      buffer.append(imageServerPath);
      buffer.append("itemimages/nicesword.gif width=30 height=30 alt=\"");

      int attack = stats.getRawAttack();
      String description;

      if (stats.scale != null) {
        int scale = evaluate(stats.scale, MonsterData.DEFAULT_SCALE);
        int cap = evaluate(stats.cap, MonsterData.DEFAULT_CAP);
        int floor = evaluate(stats.floor, MonsterData.DEFAULT_FLOOR);

        String attb =
            "Attack Power scale: (Moxie +" + scale + ", floor " + floor + ", cap " + cap + ")";
        description = attb;
      } else if (stats.attack instanceof Integer) {
        description = "Attack Power (approximate)";
      } else {
        description = "Attack Power (variable)";
      }

      buffer.append(description);
      buffer.append("\" title=\"");
      buffer.append(description);
      buffer.append("\"></td>");

      buffer.append("<td width=50 align=left><b><font size=+2>");
      buffer.append(attack);
      buffer.append("</font></b></td>");
    }

    // Phylum
    {
      String image = stats.phylum.getImage();
      String description = stats.phylum.getDescription();

      buffer.append("<td width=30>");
      buffer.append("<img src=");
      buffer.append(imageServerPath);
      buffer.append("itemimages/");
      buffer.append(image);
      buffer.append(" alt=\"This monster is ");
      buffer.append(description);
      buffer.append("\" title=\"This monster is ");
      buffer.append(description);
      buffer.append("\" width=30 height=30></td>");
    }

    // Monster Name & 3 factoids are last, spanning 3 rows
    {
      Element element = stats.defenseElement;

      buffer.append("<td rowspan=3 valign=top><b><font size=+2");
      if (element != Element.NONE) {
        buffer.append(" color=");
        buffer.append(element.getColor());
      }
      buffer.append(">");
      buffer.append(stats.getName());
      buffer.append("</font></b>");

      if (!this.noManuel) {
        List<String> factoids = MonsterManuelManager.getFactoids(this.id);
        int count = factoids.size();

        buffer.append("<ul>");

        buffer.append("<li>");
        buffer.append(count >= 1 ? factoids.get(0) : "");

        buffer.append("<li>");
        buffer.append(count >= 2 ? factoids.get(1) : "");

        buffer.append("<li>");
        buffer.append(count >= 3 ? factoids.get(2) : "");

        buffer.append("</ul>");
        buffer.append("</td>");
      }
    }

    buffer.append("</tr>");

    // *** Row 2 ***
    buffer.append("<tr>");

    // Defense
    {
      buffer.append("<td width=30>");
      buffer.append("<img src=");
      buffer.append(imageServerPath);
      buffer.append("itemimages/whiteshield.gif width=30 height=30 alt=\"");

      int defense = stats.getRawDefense();
      String description;

      if (stats.scale != null) {
        int scale = evaluate(stats.scale, MonsterData.DEFAULT_SCALE);
        int cap = evaluate(stats.cap, MonsterData.DEFAULT_CAP);
        int floor = evaluate(stats.floor, MonsterData.DEFAULT_FLOOR);

        String defb =
            "Defense scale: (Muscle +" + scale + ", floor " + floor + ", cap " + cap + ")";
        description = defb;
      } else if (stats.defense instanceof Integer) {
        description = "Defense (approximate)";
      } else {
        description = "Defense (variable)";
      }

      buffer.append(description);
      buffer.append("\" title=\"");
      buffer.append(description);
      buffer.append("\"></td>");

      buffer.append("<td width=50 align=left><b><font size=+2>");
      buffer.append(defense);
      buffer.append("</font></b></td>");
    }

    // Element
    {
      String image = stats.defenseElement.getImage();
      String description = "This monster " + stats.defenseElement.getDescription();

      buffer.append("<td width=30>");
      buffer.append("<img src=");
      buffer.append(imageServerPath);
      buffer.append("itemimages/");
      buffer.append(image);
      buffer.append(" alt=\"");
      buffer.append(description);
      buffer.append("\" title=\"");
      buffer.append(description);
      buffer.append("\" width=30 height=30></td>");
    }

    buffer.append("</tr>");

    // *** Row 3 ***
    buffer.append("<tr>");

    // HP
    {
      buffer.append("<td width=30>");
      buffer.append("<img src=");
      buffer.append(imageServerPath);
      buffer.append("itemimages/hp.gif width=30 height=30 alt=\"");

      int HP = stats.getRawHP();
      String description;

      if (stats.scale != null) {
        int scale = evaluate(stats.scale, MonsterData.DEFAULT_SCALE);
        int cap = evaluate(stats.cap, MonsterData.DEFAULT_CAP);
        int floor = evaluate(stats.floor, MonsterData.DEFAULT_FLOOR);

        String hpb =
            "Hit Points scale: 0.75 * (Muscle +"
                + scale
                + ", floor "
                + floor
                + ", cap "
                + cap
                + ")";
        description = hpb;
      } else if (stats.health instanceof Integer) {
        description = "Hit Points (approximate)";
      } else {
        description = "Hit Points (variable)";
      }

      buffer.append(description);
      buffer.append("\" title=\"");
      buffer.append(description);
      buffer.append("\"></td>");

      buffer.append("<td width=50 align=left><b><font size=+2>");
      buffer.append(HP);
      buffer.append("</font></b></td>");
    }

    // Initiative
    {
      buffer.append("<td width=30>");
      buffer.append("<img src=");
      buffer.append(imageServerPath);
      buffer.append("itemimages/");

      int initiative = stats.getRawInitiative();
      String description;
      if (initiative == -10000) {
        buffer.append("snail.gif");
        description = "Never wins initiative";
      } else if (initiative == 10000) {
        buffer.append("lightningbolt.gif");
        description = "Always wins initiative";
      } else {
        buffer.append("watch.gif");
        description = "Initiative +" + initiative + "%";
      }
      buffer.append(" alt=\"");
      buffer.append(description);
      buffer.append("\" title=\"");
      buffer.append(description);
      buffer.append("\" width=30 height=30></td>");
    }

    buffer.append("</tr>");

    // *** Row 4 ***
    buffer.append("<tr>");

    buffer.append("<td width=500 valign=top colspan=4 style=\"border-top:medium solid black;\">");
    if (this.attributes.contains("WANDERER")) {
      buffer.append("This is a wandering monster.");
    } else {
      List<String> zones = AdventureDatabase.getAreasWithMonster(this);
      if (zones.size() > 0) {
        buffer.append("This monster can be found in: ");
        boolean first = true;
        for (String zone : zones) {
          if (!first) {
            buffer.append(", ");
          }
          buffer.append(zone);
          first = false;
        }
      } else {
        buffer.append("This monster does not appear in any known zone.");
      }
    }

    if (this.boss) {
      buffer.append("<br />This monster is a Boss. ");
    }

    if (this.attributes.contains("ULTRARARE")) {
      buffer.append("<br />This monster is Ultrarare. ");
    }

    if (this.attributes.contains("LUCKY")) {
      buffer.append("<br />This monster is Lucky. ");
    }

    if (this.attributes.contains("GHOST")) {
      buffer.append("<br />This monster is a ghost. ");
    }

    if (this.attributes.contains("SNAKE")) {
      buffer.append("<br />This monster is a snake. ");
    }

    if (this.attributes.contains("DRIPPY")) {
      buffer.append("<br />This monster is of The Drip. ");
    }

    int minMeat = this.getMinMeat();
    int maxMeat = this.getMaxMeat();
    if (maxMeat > 0) {
      buffer.append("<br />Meat: ");
      buffer.append(minMeat);
      buffer.append(" - ");
      buffer.append(maxMeat);
    }

    int minSprinkles = this.getMinSprinkles();
    int maxSprinkles = this.getMaxSprinkles();
    if (maxSprinkles > 0) {
      buffer.append("<br />Sprinkles: ");
      buffer.append(minSprinkles);
      if (maxSprinkles != minSprinkles) {
        buffer.append(" - ");
        buffer.append(maxSprinkles);
      }
    }

    stats.appendItemDrops(buffer);

    buffer.append("</td>");

    buffer.append("</tr>");

    buffer.append("</table>");

    buffer.append("</body></html>");
    return buffer.toString();
  }
}
