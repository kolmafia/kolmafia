package net.sourceforge.kolmafia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.BountyDatabase;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.session.ElVibratoManager;
import net.sourceforge.kolmafia.session.ElVibratoManager.Punchcard;
import net.sourceforge.kolmafia.session.GoalManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AdventureResult implements Comparable<AdventureResult>, Cloneable {
  public static final String[] STAT_NAMES = {"muscle", "mysticality", "moxie"};

  protected Priority priority;
  protected int id;
  protected String name;
  protected int count;

  public enum Priority {
    MONSTER,

    NONE,
    ADV,
    MEAT,
    SUBSTAT,
    FULLSTAT,
    ITEM,
    BOUNTY_ITEM,
    EFFECT,

    PSEUDO_ITEM
  }

  public static final String ADV = "Adv";
  public static final String ARENA_ML = "Arena flyer ML";
  public static final String AUTOSTOP = "Autostop";
  public static final String CHASM_BRIDGE = "Chasm Bridge Progress";
  public static final String CHOICE = "Choice";
  public static final String DRUNK = "Drunk";
  public static final String EXTRUDE = "Source Terminal Extrude";
  public static final String FACTOID = "Factoid";
  public static final String FLOUNDRY = "Floundry Fish";
  public static final String FREE_CRAFT = "Free Craft";
  public static final String FULL = "Fullness";
  public static final String PIRATE_INSULT = "pirate insult";
  public static final String PULL = "Pull";
  public static final String PVP = "PvP";
  public static final String STILL = "Still";
  public static final String TOME = "Tome Summon";

  // Meat, HP, and MP need their "count" field to be a long, not an int
  // and should be delegated to AdventureLongCountResult
  public static final String MEAT = "Meat";
  public static final String MEAT_SPENT = "Meat Spent";
  public static final String HP = "HP";
  public static final String MP = "MP";

  // You, Robot has Scrap and Energy
  public static final String ENERGY = "Energy";
  public static final String SCRAP = "Scrap";

  // Sub/full stats have multiple values and should be delegated
  // to AdventureMultiResult.
  public static final String SUBSTATS = "Substats";
  public static final String FULLSTATS = "Fullstats";

  public static final List<String> MUS_SUBSTAT = new ArrayList<>();
  public static final List<String> MYS_SUBSTAT = new ArrayList<>();
  public static final List<String> MOX_SUBSTAT = new ArrayList<>();

  static {
    AdventureResult.MUS_SUBSTAT.add("Beefiness");
    AdventureResult.MUS_SUBSTAT.add("Fortitude");
    AdventureResult.MUS_SUBSTAT.add("Muscleboundness");
    AdventureResult.MUS_SUBSTAT.add("Strengthliness");
    AdventureResult.MUS_SUBSTAT.add("Strongness");
    AdventureResult.MUS_SUBSTAT.add("Muscle");
    // The following only under Can Has Cyborger
    AdventureResult.MUS_SUBSTAT.add("muskewlairtees");

    AdventureResult.MYS_SUBSTAT.add("Enchantedness");
    AdventureResult.MYS_SUBSTAT.add("Magicalness");
    AdventureResult.MYS_SUBSTAT.add("Mysteriousness");
    AdventureResult.MYS_SUBSTAT.add("Wizardliness");
    AdventureResult.MUS_SUBSTAT.add("Mysticality");
    // The following only under Can Has Cyborger
    AdventureResult.MYS_SUBSTAT.add("mistikkaltees");

    AdventureResult.MOX_SUBSTAT.add("Cheek");
    AdventureResult.MOX_SUBSTAT.add("Chutzpah");
    AdventureResult.MOX_SUBSTAT.add("Roguishness");
    AdventureResult.MOX_SUBSTAT.add("Sarcasm");
    AdventureResult.MOX_SUBSTAT.add("Smarm");
    AdventureResult.MUS_SUBSTAT.add("Moxie");
    // The following only under Can Has Cyborger
    AdventureResult.MOX_SUBSTAT.add("mawksees");
  }

  public static final int[] SESSION_SUBSTATS = new int[3];
  public static final AdventureResult SESSION_SUBSTATS_RESULT =
      new AdventureMultiResult(AdventureResult.SUBSTATS, AdventureResult.SESSION_SUBSTATS);

  public static final int[] SESSION_FULLSTATS = new int[3];
  public static final AdventureResult SESSION_FULLSTATS_RESULT =
      new AdventureMultiResult(AdventureResult.FULLSTATS, AdventureResult.SESSION_FULLSTATS);

  /**
   * Constructs a new <code>AdventureResult</code> with the given name. The amount of gain will
   * default to zero.
   *
   * @param name The name of the result
   */
  public AdventureResult(final String name) {
    this(AdventureResult.choosePriority(name), name, 0);
  }

  public AdventureResult(final String name, final int count) {
    this(AdventureResult.choosePriority(name), name, count);
  }

  public AdventureResult(final String name, final int count, final boolean isStatusEffect) {
    this(isStatusEffect ? Priority.EFFECT : Priority.ITEM, name, count);
  }

  public AdventureResult(final Priority subType, final String name) {
    this(subType, name, 1);
  }

  public AdventureResult(final Priority subType, final String name, final int count) {
    this.name = name;
    this.count = count;
    this.priority = subType;
    this.id = -1;

    if (this.priority == Priority.EFFECT) {
      // This will also set this.id as appropriate
      this.normalizeEffectName();
    } else if (this.priority == Priority.ITEM) {
      // This will also set this.id as appropriate
      this.normalizeItemName();
    } else if (this.priority == Priority.PSEUDO_ITEM) {
      // Detach substring from larger text
      this.name = name;
      this.priority = Priority.ITEM;
    } else {
      // Detach substring from larger text
      this.name = name;
    }
  }

  protected static Priority choosePriority(final String name) {
    if (name.equals(AdventureResult.ADV)
        || name.equals(AdventureResult.CHOICE)
        || name.equals(AdventureResult.AUTOSTOP)
        || name.equals(AdventureResult.FACTOID)
        || name.equals(AdventureResult.PULL)
        || name.equals(AdventureResult.STILL)
        || name.equals(AdventureResult.TOME)
        || name.equals(AdventureResult.EXTRUDE)
        || name.equals(AdventureResult.FREE_CRAFT)) {
      return Priority.ADV;
    }
    if (name.equals(AdventureResult.MEAT) || name.equals(AdventureResult.MEAT_SPENT)) {
      return Priority.MEAT;
    }
    if (name.equals(AdventureResult.HP)
        || name.equals(AdventureResult.MP)
        || name.equals(AdventureResult.DRUNK)
        || name.equals(AdventureResult.FULL)
        || name.equals(AdventureResult.PVP)
        || name.equals(AdventureResult.ENERGY)
        || name.equals(AdventureResult.SCRAP)) {
      return Priority.NONE;
    }
    if (name.equals(AdventureResult.SUBSTATS)) {
      return Priority.SUBSTAT;
    }
    if (name.equals(AdventureResult.FULLSTATS)) {
      return Priority.FULLSTAT;
    }
    if (name.equals(AdventureResult.FLOUNDRY)) {
      return Priority.PSEUDO_ITEM;
    }
    if (BountyDatabase.getType(name) != null) {
      return Priority.BOUNTY_ITEM;
    }
    if (EffectDatabase.contains(name)) {
      return Priority.EFFECT;
    }
    return Priority.ITEM;
  }

  public AdventureResult(final int id, final int count, final boolean isStatusEffect) {
    if (isStatusEffect) {
      String name = EffectDatabase.getEffectName(id);
      this.name = name != null ? name : "(unknown effect " + id + ")";
      this.priority = Priority.EFFECT;
    } else {
      String name = ItemDatabase.getItemDataName(id);
      this.name = name != null ? name : "(unknown item " + id + ")";
      this.priority = Priority.ITEM;
    }
    this.id = id;
    this.count = count;
  }

  public AdventureResult(
      final String name, final int id, final int count, final boolean isStatusEffect) {
    this.name = name;
    this.id = id;
    this.count = count;
    this.priority = isStatusEffect ? Priority.EFFECT : Priority.ITEM;
  }

  // Need this to retain instance-specific methods
  @Override
  protected AdventureResult clone() throws CloneNotSupportedException {
    return (AdventureResult) super.clone();
  }

  public void normalizeEffectName() {
    this.priority = Priority.EFFECT;

    if (this.name == null) {
      this.name = "(unknown effect)";
      return;
    }

    this.id = EffectDatabase.getEffectId(this.name);
    if (this.id != -1) {
      String name = EffectDatabase.getEffectName(this.id);
      if (name != null) {
        this.name = name;
      } else {
        RequestLogger.printLine(
            "Effect database error: id = " + this.id + " name = \"" + this.name + "\"");
      }
    }
  }

  public void normalizeItemName() {
    this.priority = Priority.ITEM;

    if (this.name == null) {
      this.name = "(unknown item " + this.id + ")";
      return;
    }

    if (this.name.equals("(none)") || this.name.equals("-select an item-")) {
      return;
    }

    this.id = ItemDatabase.getItemId(this.name, this.getCount());

    if (this.id > 0) {
      String name = ItemDatabase.getItemDataName(this.id);
      if (name != null) {
        this.name = name;
      } else {
        RequestLogger.printLine(
            "Item database error: id = " + this.id + " name = \"" + this.name + "\"");
      }
    } else {
      RequestLogger.printLine("Unknown item found: " + this.name);
    }
  }

  public static AdventureResult pseudoItem(final String name) {
    AdventureResult item = ItemFinder.getFirstMatchingItem(name, false);
    if (item != null) {
      return item;
    }

    // Make a pseudo-item with the required name
    return new AdventureResult(name, -1, 1, false);
  }

  public static AdventureResult tallyItem(final String name) {
    return AdventureResult.tallyItem(name, true);
  }

  public static AdventureResult tallyItem(final String name, final boolean setItemId) {
    AdventureResult item = new AdventureResult(Priority.NONE, name);
    item.priority = Priority.ITEM;
    item.id = setItemId ? ItemDatabase.getItemId(name, 1, false) : -1;
    return item;
  }

  public static AdventureResult tallyItem(final String name, final int itemId) {
    return new AdventureResult(name, itemId, 1, false);
  }

  public static AdventureResult tallyItem(
      final String name, final int count, final boolean setItemId) {
    AdventureResult item = AdventureResult.tallyItem(name, setItemId);
    item.count = count;
    return item;
  }

  /**
   * Accessor method to determine if this result is a status effect.
   *
   * @return <code>true</code> if this result represents a status effect
   */
  public boolean isStatusEffect() {
    return this.priority == Priority.EFFECT;
  }

  /**
   * Accessor method to determine if this result is a muscle gain.
   *
   * @return <code>true</code> if this result represents muscle subpoint gain
   */
  public boolean isMuscleGain() {
    return false; // overriden in subclass
  }

  /**
   * Accessor method to determine if this result is a mysticality gain.
   *
   * @return <code>true</code> if this result represents mysticality subpoint gain
   */
  public boolean isMysticalityGain() {
    return false; // overriden in subclass
  }

  /**
   * Accessor method to determine if this result is a muscle gain.
   *
   * @return <code>true</code> if this result represents muscle subpoint gain
   */
  public boolean isMoxieGain() {
    return false; // overriden in subclass
  }

  /**
   * Accessor method to determine if this result is an item, as opposed to meat, drunkenness,
   * adventure or substat gains.
   *
   * @return <code>true</code> if this result represents an item
   */
  public boolean isItem() {
    return this.priority == Priority.ITEM;
  }

  @SuppressWarnings("unused")
  public boolean isBountyItem() {
    return this.priority == Priority.BOUNTY_ITEM;
  }

  public boolean isMeat() {
    return this.priority == Priority.MEAT;
  }

  public boolean isHP() {
    return this.name.equals(AdventureResult.HP);
  }

  public boolean isMP() {
    return this.name.equals(AdventureResult.MP);
  }

  public boolean isEnergy() {
    return this.name.equals(AdventureResult.ENERGY);
  }

  public boolean isScrap() {
    return this.name.equals(AdventureResult.SCRAP);
  }

  public boolean isMonster() {
    return this.priority == Priority.MONSTER;
  }

  /**
   * Accessor method to retrieve the name associated with the result.
   *
   * @return The name of the result
   */
  public String getName() {
    if (!this.isItem()) {
      return this.name;
    }

    return switch (this.id) {
      case ItemPool.DUSTY_BOTTLE_OF_MERLOT,
          ItemPool.DUSTY_BOTTLE_OF_PORT,
          ItemPool.DUSTY_BOTTLE_OF_PINOT_NOIR,
          ItemPool.DUSTY_BOTTLE_OF_ZINFANDEL,
          ItemPool.DUSTY_BOTTLE_OF_MARSALA,
          ItemPool.DUSTY_BOTTLE_OF_MUSCAT -> ConsumablesDatabase.dustyBottleName(this.id);
      case ItemPool.MILKY_POTION,
          ItemPool.SWIRLY_POTION,
          ItemPool.BUBBLY_POTION,
          ItemPool.SMOKY_POTION,
          ItemPool.CLOUDY_POTION,
          ItemPool.EFFERVESCENT_POTION,
          ItemPool.FIZZY_POTION,
          ItemPool.DARK_POTION,
          ItemPool.MURKY_POTION -> AdventureResult.bangPotionName(this.id);
      case ItemPool.VIAL_OF_RED_SLIME,
          ItemPool.VIAL_OF_YELLOW_SLIME,
          ItemPool.VIAL_OF_BLUE_SLIME,
          ItemPool.VIAL_OF_ORANGE_SLIME,
          ItemPool.VIAL_OF_GREEN_SLIME,
          ItemPool.VIAL_OF_VIOLET_SLIME,
          ItemPool.VIAL_OF_VERMILION_SLIME,
          ItemPool.VIAL_OF_AMBER_SLIME,
          ItemPool.VIAL_OF_CHARTREUSE_SLIME,
          ItemPool.VIAL_OF_TEAL_SLIME,
          ItemPool.VIAL_OF_INDIGO_SLIME,
          ItemPool.VIAL_OF_PURPLE_SLIME -> AdventureResult.slimeVialName(this.id);
      case ItemPool.PUNCHCARD_ATTACK,
          ItemPool.PUNCHCARD_REPAIR,
          ItemPool.PUNCHCARD_BUFF,
          ItemPool.PUNCHCARD_MODIFY,
          ItemPool.PUNCHCARD_BUILD,
          ItemPool.PUNCHCARD_TARGET,
          ItemPool.PUNCHCARD_SELF,
          ItemPool.PUNCHCARD_FLOOR,
          ItemPool.PUNCHCARD_DRONE,
          ItemPool.PUNCHCARD_WALL,
          ItemPool.PUNCHCARD_SPHERE -> AdventureResult.punchCardName(this.id);
      case ItemPool.UNBREAKABLE_UMBRELLA -> this.name
          + " ("
          + Preferences.getString("umbrellaState")
          + ")";
      case ItemPool.JURASSIC_PARKA, ItemPool.REPLICA_JURASSIC_PARKA -> {
        var mode = Preferences.getString("parkaMode");
        yield mode.equals("") ? this.name : this.name + " (" + mode + " mode)";
      }
      case ItemPool.BACKUP_CAMERA -> this.name
          + " ("
          + Preferences.getString("backupCameraMode")
          + ")";
      default -> this.name;
    };
  }

  public String getDisambiguatedName() {
    if ((this.priority == Priority.ITEM && ItemDatabase.getItemIds(this.name, 1, false).length > 1)
        || (this.priority == Priority.EFFECT
            && EffectDatabase.getEffectIds(this.name, false).length > 1)) {
      return "[" + this.id + "]" + this.name;
    }

    return this.name;
  }

  public String getDataName() {
    return this.name;
  }

  public int getPluralCount() {
    return this.getCount();
  }

  public String getPluralName() {
    return this.getPluralName(this.getPluralCount());
  }

  public String getPluralName(final int count) {
    return count == 1
        ? this.getName()
        : this.priority == Priority.BOUNTY_ITEM
            ? BountyDatabase.getPlural(this.getName())
            : this.id == -1 ? this.getName() + "s" : ItemDatabase.getPluralName(this.id);
  }

  public String getArticle() {
    return "a" + ((StringUtilities.isVowel(this.getName().charAt(0))) ? "n" : "");
  }

  /**
   * Accessor method to retrieve the item Id associated with the result, if this is an item and the
   * item Id is known.
   *
   * @return The item Id associated with this item
   */
  public int getItemId() {
    if (this.priority == Priority.ITEM) {
      return this.id;
    }
    return -1;
  }

  public int getEffectId() {
    if (this.priority == Priority.EFFECT) {
      return this.id;
    }
    return -1;
  }

  /**
   * Accessor method to retrieve the total value associated with the result. In the event of substat
   * points, this returns the total subpoints within the <code>AdventureResult</code>; in the event
   * of an item or meat gains, this will return the total number of meat/items in this result.
   *
   * @return The amount associated with this result
   */
  public int getCount() {
    return count;
  }

  public long getLongCount() {
    return count;
  }

  public int[] getCounts() { // This should be called on multi-valued subclasses only!
    return null;
  }

  /**
   * Accessor method to retrieve the total value associated with the result stored at the given
   * index of the count array.
   *
   * @return The total value at the given index of the count array
   */
  public int getCount(final int index) {
    return index != 0 ? 0 : this.count;
  }

  public long getLongCount(final int index) {
    return index != 0 ? 0 : (long) this.count;
  }

  /**
   * A static final method which parses the given string for any content which might be applicable
   * to an <code>AdventureResult</code>, and returns the resulting <code>AdventureResult</code>.
   *
   * @param s The string suspected of being an <code>AdventureResult</code>
   * @return An <code>AdventureResult</code> with the appropriate data
   * @throws NumberFormatException The string was not a recognized <code>AdventureResult</code>
   */
  public static AdventureResult parseResult(final String s) {
    // If this result has been screwed up with Rad Libs, can't do anything with it.
    if (s.startsWith("You &nbsp;")) {
      return null;
    }

    if (s.startsWith("You gain") || s.startsWith("You lose") || s.startsWith("You spent")) {
      // A stat has been modified - now you figure out which
      // one it was, how much it's been modified by, and
      // return the appropriate value

      StringTokenizer parsedGain = new StringTokenizer(s, " .");
      if (parsedGain.countTokens() < 4) {
        return null;
      }
      parsedGain.nextToken(); // Skip "You"

      // Decide if the quantity increases or decreases
      int sign = parsedGain.nextToken().startsWith("gain") ? 1 : -1;
      // Make sure we are looking at a number
      String val = parsedGain.nextToken();
      if (val.equals("no")) {
        val = "0";
      }
      if (!StringUtilities.isNumeric(val)) {
        return null;
      }

      // Yes. It is safe to parse it as an integer
      long modifier = sign * StringUtilities.parseLong(val);

      // Stats actually fall into one of four categories -
      // simply pick the correct one and return the result.

      String statname = parsedGain.nextToken();

      if (statname.startsWith("Adv")) {
        return new AdventureResult(AdventureResult.ADV, (int) modifier);
      }

      if (statname.startsWith("Dru")) {
        return new AdventureResult(AdventureResult.DRUNK, (int) modifier);
      }

      if (statname.startsWith("Full")) {
        return new AdventureResult(AdventureResult.FULL, (int) modifier);
      }

      if (statname.startsWith("Me")) {
        // "Meat" or "Meets", if Can Has Cyborger
        return new AdventureLongCountResult(AdventureResult.MEAT, modifier);
      }

      if (statname.startsWith("addit")) {
        statname = parsedGain.nextToken();
      }

      if (statname.toLowerCase().startsWith("energy")) {
        return new AdventureResult(AdventureResult.ENERGY, (int) modifier);
      }

      if (statname.startsWith("scrap")) {
        return new AdventureResult(AdventureResult.SCRAP, (int) modifier);
      }

      if (statname.startsWith("PvP")) {
        return new AdventureResult(AdventureResult.PVP, (int) modifier);
      }

      if (parsedGain.hasMoreTokens()) {
        char identifier = statname.charAt(0);
        String name =
            identifier == 'H' || identifier == 'h' ? AdventureResult.HP : AdventureResult.MP;
        return new AdventureLongCountResult(name, modifier);
      }

      // In the current implementations, all stats gains are
      // located inside of a generic AdventureResult which
      // indicates how much of each substat is gained.

      int[] gained = {
        AdventureResult.MUS_SUBSTAT.contains(statname) ? (int) modifier : 0,
        AdventureResult.MYS_SUBSTAT.contains(statname) ? (int) modifier : 0,
        AdventureResult.MOX_SUBSTAT.contains(statname) ? (int) modifier : 0
      };

      return new AdventureMultiResult(AdventureResult.SUBSTATS, gained);
    }

    return AdventureResult.parseItem(s, false);
  }

  public static AdventureResult parseItem(final String s, final boolean pseudoAllowed) {
    // Certain items contain parentheses. Appending (COUNT) to such is problematic.
    // For now, if we have an exact match for an item name, use it with a count of 1.
    int itemId = ItemDatabase.getItemId(s.trim(), 1, false);
    // Ignore any bracketed items as they include item ID in the name
    if (itemId != -1 && !s.trim().matches("^\\[\\d+].*")) {
      AdventureResult item = new AdventureResult(Priority.NONE, s.trim());
      item.priority = Priority.ITEM;
      item.id = itemId;
      item.count = 1;
      return item;
    }

    StringTokenizer parsedItem = new StringTokenizer(s, "()");

    if (parsedItem.countTokens() == 0) {
      return null;
    }

    StringBuilder nameBuilder = new StringBuilder(parsedItem.nextToken().trim());
    int count = 1;
    while (parsedItem.hasMoreTokens()) {
      String next = parsedItem.nextToken().trim();
      if (!parsedItem.hasMoreTokens() && StringUtilities.isNumeric(next)) {
        count = StringUtilities.parseInt(next);
      } else if (!next.isEmpty()) {
        nameBuilder.append(" (").append(next).append(")");
      }
    }

    String name = nameBuilder.toString();

    if (!pseudoAllowed) {
      return new AdventureResult(name, count);
    }

    // Hand craft an item Adventure Result, regardless of the name
    AdventureResult item = new AdventureResult(Priority.NONE, name);
    item.priority = Priority.ITEM;
    item.id = ItemDatabase.getItemId(name, 1, false);
    item.count = count;
    if (item.id > 0) { // normalize name
      item.name = ItemDatabase.getItemDataName(item.id);
    }

    return item;
  }

  /**
   * Converts the <code>AdventureResult</code> to a <code>String</code>. This is especially useful
   * in debug, or if the <code>AdventureResult</code> is to be displayed in a <code>ListModel</code>
   * .
   *
   * @return The string version of this <code>AdventureResult</code>
   */
  @Override
  public String toString() {
    if (this.name == null) {
      return "(Unrecognized result)";
    }

    if (this.priority == Priority.MONSTER) {
      return this.name;
    }

    if (this.name.equals(AdventureResult.ADV)) {
      return " Advs Used: " + KoLConstants.COMMA_FORMAT.format(this.count);
    }

    if (this.name.equals(AdventureResult.FREE_CRAFT)) {
      return " Free Crafts: " + KoLConstants.COMMA_FORMAT.format(this.count);
    }

    if (this.name.equals(AdventureResult.MEAT)) {
      return " Meat Gained: " + KoLConstants.COMMA_FORMAT.format(this.getLongCount());
    }

    if (this.name.equals(AdventureResult.MEAT_SPENT)) {
      return " Meat Spent: " + KoLConstants.COMMA_FORMAT.format(this.count);
    }

    if (this.name.equals(AdventureResult.CHOICE)) {
      return " Choices Left: " + KoLConstants.COMMA_FORMAT.format(this.count);
    }

    if (this.name.equals(AdventureResult.AUTOSTOP)) {
      return " Autostops Left: " + KoLConstants.COMMA_FORMAT.format(this.count);
    }

    if (this.name.equals(AdventureResult.PULL)) {
      return " Budgeted Pulls: " + KoLConstants.COMMA_FORMAT.format(this.count);
    }

    if (this.name.equals(AdventureResult.STILL)) {
      return " Still Usages: " + KoLConstants.COMMA_FORMAT.format(this.count);
    }

    if (this.name.equals(AdventureResult.TOME)) {
      return " Tome Summons: " + KoLConstants.COMMA_FORMAT.format(this.count);
    }

    if (this.name.equals(AdventureResult.EXTRUDE)) {
      return " Source Terminal Extrudes: " + KoLConstants.COMMA_FORMAT.format(this.count);
    }

    if (this.name.equals(AdventureResult.HP) || this.name.equals(AdventureResult.MP)) {
      return " " + this.name + ": " + KoLConstants.COMMA_FORMAT.format(this.getLongCount());
    }

    if (this.name.equals(AdventureResult.DRUNK) || this.name.equals(AdventureResult.FULL)) {
      return " " + this.name + ": " + KoLConstants.COMMA_FORMAT.format(this.count);
    }

    String name = this.getName();

    if (this.priority == Priority.EFFECT) {
      if (name.equals("On the Trail")) {
        String monster = Preferences.getString("olfactedMonster");
        if (!monster.isEmpty()) {
          name = name + " [" + monster + "]";
        }
      } else {
        if (EffectDatabase.isSong(name)) {
          name = "♫ " + name;
        }

        String skillName = UneffectRequest.effectToSkill(name);
        if (SkillDatabase.contains(skillName)) {
          int skillId = SkillDatabase.getSkillId(skillName);
          if (SkillDatabase.isExpression(skillId)) {
            name = "☺ " + name;
          }
        }
      }
    }

    int count = this.count;

    return count == 1
        ? name
        : count > Integer.MAX_VALUE / 2
            ? name + " (∞)"
            : count == PurchaseRequest.MAX_QUANTITY
                ? name + " (unlimited)"
                : name + " (" + KoLConstants.COMMA_FORMAT.format(count) + ")";
  }

  public String getConditionType() {
    if (this.name == null) {
      return "";
    }

    if (this.priority == Priority.PSEUDO_ITEM) {
      return this.name.toLowerCase();
    }

    if (this.priority == Priority.BOUNTY_ITEM || this.priority == Priority.EFFECT) {
      return this.name.toLowerCase();
    }

    if (this.priority == Priority.SUBSTAT || this.priority == Priority.FULLSTAT) {
      return "substats";
    }

    if (this.name.equals(AdventureResult.ADV) || this.name.equals(AdventureResult.CHOICE)) {
      return "choiceadv";
    }

    if (this.name.equals(AdventureResult.AUTOSTOP)) {
      return "autostop";
    }

    if (this.name.equals(AdventureResult.MEAT)) {
      return "meat";
    }

    if (this.name.equals(AdventureResult.HP)) {
      return "health";
    }

    if (this.name.equals(AdventureResult.MP)) {
      return "mana";
    }

    if (this.name.equals(AdventureResult.FACTOID)) {
      return "factoid";
    }

    if (this.name.equals(AdventureResult.FLOUNDRY)) {
      return "floundry fish";
    }

    if (this.name.equals(AdventureResult.PIRATE_INSULT)) {
      return "pirate insult";
    }

    if (this.name.equals(AdventureResult.ARENA_ML)) {
      return "Arena flyer ML";
    }

    if (this.name.equals(AdventureResult.CHASM_BRIDGE)) {
      return "Chasm Bridge Progress";
    }

    return "item";
  }

  public String toConditionString() {
    if (this.name == null) {
      return "";
    }

    String conditionType = this.getConditionType();

    if (conditionType.equals("pirate insult")) {
      return "+" + this.getLongCount() + " " + this.name;
    }

    if (conditionType.equals("item")) {
      return "+" + this.getLongCount() + " " + this.getDisambiguatedName().replaceAll("[,\"]", "");
    }

    return this.getLongCount() + " " + conditionType;
  }

  /**
   * Compares the <code>AdventureResult</code> with the given object for equality.
   *
   * @param o The <code>Object</code> to be compared with this <code>AdventureResult</code>
   * @return <code>true</code> if the <code>Object</code> is an <code>AdventureResult</code> and has
   *     the same name as this one
   */
  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof AdventureResult ar)) {
      return false;
    }

    if (o instanceof WildcardResult) {
      return o.equals(this);
    }

    return this.priority == ar.priority && this.id == ar.id && Objects.equals(this.name, ar.name);
  }

  @Override
  public int hashCode() {
    if (this.name == null) {
      return 0;
    }

    return this.name.hashCode();
  }

  /**
   * Compares the <code>AdventureResult</code> with the given object for name equality and priority
   * differences. Return values are consistent with the rules laid out in {@link
   * java.lang.Comparable#compareTo(Object)}.
   */
  @Override
  public int compareTo(final AdventureResult o) {
    if (o == null) {
      throw new NullPointerException();
    }

    if (o == this) {
      return 0;
    }

    if (this.priority != o.priority) {
      return this.priority.compareTo(o.priority);
    }

    if (this.name == null) {
      return o.name == null ? 0 : 1;
    }

    if (o.name == null) {
      return -1;
    }

    if (this.priority == Priority.EFFECT) {
      // Status effects have IDs and durations.  Sort by
      // duration, or by name, if durations are equal.
      int countComparison = this.getCount() - o.getCount();
      return countComparison != 0
          ? countComparison
          : this.id != o.id ? this.name.compareToIgnoreCase(o.name) : 0;
    }

    int nameComparison = this.name.compareToIgnoreCase(o.name);
    return nameComparison != 0 ? nameComparison : this.id - o.id;
  }

  /**
   * Utility method used for adding a given <code>AdventureResult</code> to a tally of <code>
   * AdventureResult</code>s.
   *
   * @param sourceList The tally accumulating <code>AdventureResult</code>s
   * @param result The result to add to the tally
   */
  public static void addResultToList(
      final List<AdventureResult> sourceList, final AdventureResult result) {
    int index = sourceList.indexOf(result);

    // First, filter out things where it's a simple addition of an
    // item, or something which may not result in a change in the
    // state of the sourceList list.

    if (index == -1) {
      if (!result.isItem()) {
        sourceList.add(result);
        return;
      }
      int count = result.getCount();
      if (count == 0) return;
      if (count < 0
          && (sourceList != KoLConstants.tally || !Preferences.getBoolean("allowNegativeTally"))) {
        return;
      }
      sourceList.add(result);
      if (sourceList == KoLConstants.inventory) {
        InventoryManager.fireInventoryChanged(result.getItemId());
      }
      return;
    }

    // These don't involve any addition -- ignore this entirely
    // for now.

    if (result == GoalManager.GOAL_SUBSTATS) {
      return;
    }

    // Compute the sum of the existing adventure result and the
    // current adventure result, and construct the sum.

    AdventureResult current = sourceList.get(index);

    // Modify substats and fullstats in place
    if (current instanceof AdventureMultiResult c && result instanceof AdventureMultiResult r) {
      c.addResultInPlace(r);
      return;
    }

    AdventureResult sumResult = current.getInstance(current.getLongCount() + result.getLongCount());

    // Check to make sure that the result didn't transform the value
    // to zero - if it did, then remove the item from the list if
    // it's an item (non-items are exempt).

    if (sumResult.isItem()) {
      if (sumResult.getCount() == 0) {
        sourceList.remove(index);
        if (sourceList == KoLConstants.inventory) {
          InventoryManager.fireInventoryChanged(result.getItemId());
        }
        return;
      } else if (sumResult.getCount() < 0
          && (sourceList != KoLConstants.tally || !Preferences.getBoolean("allowNegativeTally"))) {
        sourceList.remove(index);
        if (sourceList == KoLConstants.inventory) {
          InventoryManager.fireInventoryChanged(result.getItemId());
        }
        return;
      }
      sourceList.set(index, sumResult);
      if (sourceList == KoLConstants.inventory) {
        InventoryManager.fireInventoryChanged(result.getItemId());
      }
      return;
    } else if (sumResult.getCount() == 0
        && (sumResult.isStatusEffect()
            || sumResult.getName().equals(AdventureResult.CHOICE)
            || sumResult.getName().equals(AdventureResult.AUTOSTOP))) {
      sourceList.remove(index);
      return;
    } else if (sumResult.getCount() < 0 && sumResult.isStatusEffect()) {
      sourceList.remove(index);
      return;
    }

    sourceList.set(index, sumResult);
  }

  public static void addOrRemoveResultToList(
      final List<AdventureResult> sourceList, final AdventureResult result) {
    int index = sourceList.indexOf(result);

    if (index == -1) {
      sourceList.add(result);
      return;
    }

    AdventureResult current = sourceList.get(index);
    AdventureResult sumResult = current.getInstance(current.count + result.count);

    if (sumResult.getCount() <= 0) {
      sourceList.remove(index);
      return;
    }

    sourceList.set(index, sumResult);
  }

  public static void removeResultFromList(
      final List<AdventureResult> sourceList, final AdventureResult result) {
    int index = sourceList.indexOf(result);
    if (index != -1) {
      sourceList.remove(index);
    }
  }

  public AdventureResult getNegation() {
    if (this.isItem() && this.id != -1) {
      return this.count == 0 ? this : new AdventureResult(this.id, -this.count, false);
    } else if (this.isStatusEffect() && this.id != -1) {
      return this.count == 0 ? this : new AdventureResult(this.id, -this.count, true);
    }

    return this.getInstance(-this.count);
  }

  public AdventureResult getInstance(final long quantity) {
    if (this.isMeat() || this.isHP() || this.isMP()) {
      if (this.count == quantity) {
        return this;
      }

      return new AdventureLongCountResult(this.name, quantity);
    }

    if (this.isItem()) {
      if (this.count == quantity) {
        return this;
      }

      // Handle pseudo and tally items that override methods of AdventureResult

      AdventureResult item;
      try {
        item = this.clone();
      } catch (CloneNotSupportedException e) {
        // This should not happen. Hope for the best.
        item = new AdventureResult(Priority.NONE, this.name);
        item.priority = Priority.ITEM;
        item.id = this.id;
      }

      item.count = (int) quantity;
      return item;
    }

    if (this.isStatusEffect()) {
      AdventureResult effect;
      try {
        effect = this.clone();
      } catch (CloneNotSupportedException e) {
        // This should not happen. Hope for the best.
        effect = new AdventureResult(Priority.NONE, this.name);
        effect.priority = Priority.EFFECT;
        effect.id = this.id;
      }

      effect.count = (int) quantity;
      return effect;
    }

    return new AdventureResult(this.name, (int) quantity);
  }

  public AdventureResult getInstance(final int[] quantity) {
    return this.getInstance(quantity[0]);
  }

  /**
   * Special method which simplifies the constant use of indexOf and count retrieval. This makes
   * intent more transparent.
   */
  public int getCount(final List<AdventureResult> list) {
    int index = list.indexOf(this);
    if (index == -1) return 0;
    AdventureResult item = list.get(index);
    if (item == null) return 0;
    return item.getCount();
  }

  public int getCount(final Map<Integer, AdventureResult> map) {
    AdventureResult item = map.get(this.getItemId());
    if (item == null) return 0;
    return item.getCount();
  }

  public static AdventureResult findItem(final int itemId, final List<AdventureResult> list) {
    for (AdventureResult item : list) {
      if (item.getItemId() == itemId) {
        return item;
      }
    }
    return null;
  }

  public static String bangPotionName(final int itemId) {
    String itemName = ItemDatabase.getItemDataName(itemId);

    String effect = Preferences.getString("lastBangPotion" + itemId);
    if (effect.isEmpty()) {
      return itemName;
    }

    return itemName + " of " + effect;
  }

  public static String slimeVialName(final int itemId) {
    String itemName = ItemDatabase.getItemDataName(itemId);

    String effect = Preferences.getString("lastSlimeVial" + itemId);
    if (effect.isEmpty()) {
      return itemName;
    }

    return itemName + ": " + effect;
  }

  public final String bangPotionAlias() {
    if (this.isItem()) {
      if (this.id >= ItemPool.FIRST_BANG_POTION && this.id <= ItemPool.LAST_BANG_POTION) {
        String effect = Preferences.getString("lastBangPotion" + this.id);
        if (effect.isEmpty()) {
          return this.name;
        }

        return "potion of " + effect;
      }
      if (this.id >= ItemPool.FIRST_SLIME_VIAL && this.id < ItemPool.LAST_SLIME_VIAL) {
        String effect = Preferences.getString("lastSlimeVial" + this.id);
        if (effect.isEmpty()) {
          return this.name;
        }

        return "vial of slime: " + effect;
      }
    }

    return this.name;
  }

  public final AdventureResult resolveBangPotion() {
    String name = this.name;

    if (name.startsWith("potion of ")) {
      for (int itemId = ItemPool.FIRST_BANG_POTION; itemId <= ItemPool.LAST_BANG_POTION; ++itemId) {
        String potion = Preferences.getString("lastBangPotion" + itemId);
        if (!potion.isEmpty() && name.endsWith(potion)) {
          return ItemPool.get(itemId, this.getCount());
        }
      }
      return this;
    }

    if (name.startsWith("vial of slime: ")) {
      for (int itemId = ItemPool.FIRST_SLIME_VIAL; itemId < ItemPool.LAST_SLIME_VIAL; ++itemId) {
        String vial = Preferences.getString("lastSlimeVial" + itemId);
        if (!vial.isEmpty() && name.endsWith(vial)) {
          return ItemPool.get(itemId, this.getCount());
        }
      }
      return this;
    }

    return this;
  }

  public static String punchCardName(final int itemId) {
    for (Punchcard punchcard : ElVibratoManager.PUNCHCARDS) {
      if (punchcard.id() == itemId) {
        return punchcard.alias();
      }
    }

    return ItemDatabase.getItemDataName(itemId);
  }

  // AdventureMultiResult handles the specific stat-related result types
  // that must store multiple values, rather than having a 1-element count
  // array inside every AdventureResult.
  public static class AdventureMultiResult extends AdventureResult {
    private final int[] counts;

    public AdventureMultiResult(final String name, final int[] counts) {
      this(AdventureResult.choosePriority(name), name, counts);
    }

    protected AdventureMultiResult(final Priority subType, final String name, final int[] counts) {
      super(subType, name, counts[0]);
      this.counts = counts;
    }

    /**
     * Accessor method to determine if this result is a muscle gain.
     *
     * @return <code>true</code> if this result represents muscle subpoint gain
     */
    @Override
    public boolean isMuscleGain() {
      return this.priority == Priority.SUBSTAT && this.counts[0] != 0;
    }

    /**
     * Accessor method to determine if this result is a mysticality gain.
     *
     * @return <code>true</code> if this result represents mysticality subpoint gain
     */
    @Override
    public boolean isMysticalityGain() {
      return this.priority == Priority.SUBSTAT && this.counts[1] != 0;
    }

    /**
     * Accessor method to determine if this result is a muscle gain.
     *
     * @return <code>true</code> if this result represents muscle subpoint gain
     */
    @Override
    public boolean isMoxieGain() {
      return this.priority == Priority.SUBSTAT && this.counts[2] != 0;
    }

    /**
     * Accessor method to retrieve the total value associated with the result. In the event of
     * substat points, this returns the total subpoints within the <code>AdventureResult</code>; in
     * the event of an item or meat gains, this will return the total number of meat/items in this
     * result.
     *
     * @return The amount associated with this result
     */
    @Override
    public int getCount() {
      return Arrays.stream(this.counts).sum();
    }

    @Override
    public int[] getCounts() {
      return this.counts;
    }

    /**
     * Accessor method to retrieve the total value associated with the result stored at the given
     * index of the count array.
     *
     * @return The total value at the given index of the count array
     */
    @Override
    public int getCount(final int index) {
      return index < 0 || index >= this.counts.length ? 0 : this.counts[index];
    }

    /**
     * Converts the <code>AdventureResult</code> to a <code>String</code>. This is especially useful
     * in debug, or if the <code>AdventureResult</code> is to be displayed in a <code>ListModel
     * </code>.
     *
     * @return The string version of this <code>AdventureResult</code>
     */
    @Override
    public String toString() {
      if (this.name.equals(AdventureResult.SUBSTATS)
          || this.name.equals(AdventureResult.FULLSTATS)) {
        return " "
            + this.name
            + ": "
            + KoLConstants.COMMA_FORMAT.format(this.counts[0])
            + " / "
            + KoLConstants.COMMA_FORMAT.format(this.counts[1])
            + " / "
            + KoLConstants.COMMA_FORMAT.format(this.counts[2]);
      }

      return "(Unrecognized multi-result)";
    }

    @Override
    public String toConditionString() {
      if (this.name.equals(AdventureResult.SUBSTATS)) {
        StringBuilder stats = new StringBuilder();

        if (this.counts[0] > 0) {
          stats
              .append(
                  KoLCharacter.calculateBasePoints(KoLCharacter.getTotalMuscle() + this.counts[0]))
              .append(" muscle");
        }

        if (this.counts[1] > 0) {
          if (this.counts[0] > 0) {
            stats.append(", ");
          }

          stats
              .append(
                  KoLCharacter.calculateBasePoints(
                      KoLCharacter.getTotalMysticality() + this.counts[1]))
              .append(" mysticality");
        }

        if (this.counts[2] > 0) {
          if (this.counts[0] > 0 || this.counts[1] > 0) {
            stats.append(", ");
          }

          stats
              .append(
                  KoLCharacter.calculateBasePoints(KoLCharacter.getTotalMoxie() + this.counts[2]))
              .append(" moxie");
        }

        return stats.toString();
      }

      return super.toConditionString();
    }

    protected void addResultInPlace(AdventureMultiResult result) {
      for (int i = 0; i < this.counts.length; ++i) {
        this.counts[i] += result.counts[i];
      }
    }

    @Override
    public AdventureResult getNegation() {
      int[] newcounts = new int[this.counts.length];
      for (int i = 0; i < this.counts.length; ++i) {
        newcounts[i] = -this.counts[i];
      }

      return this.getInstance(newcounts);
    }

    @Override
    public AdventureResult getInstance(final int[] quantity) {
      if (this.priority == Priority.SUBSTAT) {
        return new AdventureMultiResult(AdventureResult.SUBSTATS, quantity);
      }

      return new AdventureMultiResult(AdventureResult.FULLSTATS, quantity);
    }
  }

  public static class AdventureLongCountResult extends AdventureResult {
    private long longCount;

    public AdventureLongCountResult(final String name, final long count) {
      super(name, AdventureLongCountResult.intCount(count));
      this.longCount = count;
    }

    public AdventureLongCountResult(final String name) {
      this(name, 0);
    }

    private static int intCount(long count) {
      return count > Integer.MAX_VALUE
          ? Integer.MAX_VALUE
          : count < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) count;
    }

    @Override
    public long getLongCount() {
      return longCount;
    }

    @Override
    public long getLongCount(final int index) {
      return index != 0 ? 0 : longCount;
    }

    @Override
    public AdventureResult getNegation() {
      return this.getInstance(-this.longCount);
    }

    @Override
    public AdventureResult getInstance(final long quantity) {
      if (this.longCount == quantity) {
        return this;
      }

      // Try to use clone() to preserve instance-specific method overrides
      try {
        AdventureLongCountResult item = (AdventureLongCountResult) this.clone();
        item.count = AdventureLongCountResult.intCount(quantity);
        item.longCount = quantity;
        return item;
      } catch (CloneNotSupportedException e) {
        // This should not happen. Hope for the best.
        return new AdventureLongCountResult(this.name, quantity);
      }
    }
  }

  public static class WildcardResult extends AdventureResult {
    // Note that these objects must not be placed in a sorted list, since they
    // are not meaningfully comparable other than via equals().
    private final String match;
    private final String[] matches;
    private final boolean negated;

    public WildcardResult(String name, int count, String match, boolean negated) {
      super(Priority.ITEM, name, count);

      this.match = match;
      this.matches = match.split("\\s*[|/]\\s*");

      for (int i = 0; i < matches.length; ++i) {
        this.matches[i] = this.matches[i].toLowerCase();
      }

      this.negated = negated;
    }

    @Override
    public AdventureResult getInstance(long count) {
      return new WildcardResult(this.getName(), (int) count, this.match, this.negated);
    }

    @Override
    public boolean equals(final Object o) {
      if (!(o instanceof AdventureResult ar)) {
        return false;
      }

      boolean hasMatch = false;
      String arName = ar.getName().toLowerCase();

      for (int i = 0; i < this.matches.length && !hasMatch; ++i) {
        hasMatch = arName.contains(this.matches[i]);
      }

      return hasMatch ^ this.negated;
    }

    @Override
    public int getCount(final List<AdventureResult> list) {
      int count = 0;
      for (AdventureResult ar : list) {
        if (this.equals(ar)) {
          count += ar.getCount();
        }
      }
      return count;
    }

    @Override
    public void normalizeItemName() { // Overridden to avoid "unknown item found" messages.
    }

    public static WildcardResult getInstance(String text) {
      if (!text.contains("any")) {
        return null;
      }

      String[] pieces = text.split(" ", 2);
      int count = StringUtilities.isNumeric(pieces[0]) ? StringUtilities.parseInt(pieces[0]) : 0;
      if (pieces.length > 1 && count != 0) {
        text = pieces[1];
      } else {
        count = 1;
      }

      if (text.startsWith("any ")) {
        return new WildcardResult(text, count, text.substring(4), false);
      }
      if (text.startsWith("anything but ")) {
        return new WildcardResult(text, count, text.substring(13), true);
      }
      return null;
    }
  }

  public static AdventureResult parseEffectString(String effectString) {
    String name = effectString;
    int duration = 0;
    int lparen = effectString.lastIndexOf("(");
    int rparen = effectString.lastIndexOf(")");
    if (lparen >= 0 && rparen >= 0) {
      String durationString = effectString.substring(lparen + 1, rparen);
      if (StringUtilities.isNumeric(durationString)) {
        name = effectString.substring(0, lparen).trim();
        duration = StringUtilities.parseInt(durationString);
      }
    }
    int effectId = EffectDatabase.getEffectId(name, true);
    if (effectId < 0) {
      return null;
    }
    return EffectPool.get(effectId, duration);
  }

  public static AdventureResult parseItemString(String itemString) {
    String name = itemString;
    int count = 1;
    int lparen = itemString.lastIndexOf("(");
    int rparen = itemString.lastIndexOf(")");
    if (lparen >= 0 && rparen >= 0) {
      String countString = itemString.substring(lparen + 1, rparen);
      if (StringUtilities.isNumeric(countString)) {
        name = itemString.substring(0, lparen).trim();
        count = StringUtilities.parseInt(countString);
      }
    }
    int itemId = ItemDatabase.getItemId(name, 1, false);
    if (itemId < 0) {
      return null;
    }
    return ItemPool.get(itemId, count);
  }
}
