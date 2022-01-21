package net.sourceforge.kolmafia;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class VYKEACompanionData implements Comparable<VYKEACompanionData> {
  public static final String[] VYKEA = {
    "level 1 bookshelf",
    "level 1 frenzy bookshelf",
    "level 1 blood bookshelf",
    "level 1 lightning bookshelf",
    "level 2 bookshelf",
    "level 2 frenzy bookshelf",
    "level 2 blood bookshelf",
    "level 2 lightning bookshelf",
    "level 3 bookshelf",
    "level 3 frenzy bookshelf",
    "level 3 blood bookshelf",
    "level 3 lightning bookshelf",
    "level 4 bookshelf",
    "level 4 frenzy bookshelf",
    "level 4 blood bookshelf",
    "level 4 lightning bookshelf",
    "level 5 bookshelf",
    "level 5 frenzy bookshelf",
    "level 5 blood bookshelf",
    "level 5 lightning bookshelf",
    "level 1 ceiling fan",
    "level 1 frenzy ceiling fan",
    "level 1 blood ceiling fan",
    "level 1 lightning ceiling fan",
    "level 2 ceiling fan",
    "level 2 frenzy ceiling fan",
    "level 2 blood ceiling fan",
    "level 2 lightning ceiling fan",
    "level 3 ceiling fan",
    "level 3 frenzy ceiling fan",
    "level 3 blood ceiling fan",
    "level 3 lightning ceiling fan",
    "level 4 ceiling fan",
    "level 4 frenzy ceiling fan",
    "level 4 blood ceiling fan",
    "level 4 lightning ceiling fan",
    "level 5 ceiling fan",
    "level 5 frenzy ceiling fan",
    "level 5 blood ceiling fan",
    "level 5 lightning ceiling fan",
    "level 1 couch",
    "level 1 frenzy couch",
    "level 1 blood couch",
    "level 1 lightning couch",
    "level 2 couch",
    "level 2 frenzy couch",
    "level 2 blood couch",
    "level 2 lightning couch",
    "level 3 couch",
    "level 3 frenzy couch",
    "level 3 blood couch",
    "level 3 lightning couch",
    "level 4 couch",
    "level 4 frenzy couch",
    "level 4 blood couch",
    "level 4 lightning couch",
    "level 5 couch",
    "level 5 frenzy couch",
    "level 5 blood couch",
    "level 5 lightning couch",
    "level 1 dishrack",
    "level 1 frenzy dishrack",
    "level 1 blood dishrack",
    "level 1 lightning dishrack",
    "level 2 dishrack",
    "level 2 frenzy dishrack",
    "level 2 blood dishrack",
    "level 2 lightning dishrack",
    "level 3 dishrack",
    "level 3 frenzy dishrack",
    "level 3 blood dishrack",
    "level 3 lightning dishrack",
    "level 4 dishrack",
    "level 4 frenzy dishrack",
    "level 4 blood dishrack",
    "level 4 lightning dishrack",
    "level 5 dishrack",
    "level 5 frenzy dishrack",
    "level 5 blood dishrack",
    "level 5 lightning dishrack",
    "level 1 dresser",
    "level 1 frenzy dresser",
    "level 1 blood dresser",
    "level 1 lightning dresser",
    "level 2 dresser",
    "level 2 frenzy dresser",
    "level 2 blood dresser",
    "level 2 lightning dresser",
    "level 3 dresser",
    "level 3 frenzy dresser",
    "level 3 blood dresser",
    "level 3 lightning dresser",
    "level 4 dresser",
    "level 4 frenzy dresser",
    "level 4 blood dresser",
    "level 4 lightning dresser",
    "level 5 dresser",
    "level 5 frenzy dresser",
    "level 5 blood dresser",
    "level 5 lightning dresser",
    "level 1 lamp",
    "level 1 frenzy lamp",
    "level 1 blood lamp",
    "level 1 lightning lamp",
    "level 2 lamp",
    "level 2 frenzy lamp",
    "level 2 blood lamp",
    "level 2 lightning lamp",
    "level 3 lamp",
    "level 3 frenzy lamp",
    "level 3 blood lamp",
    "level 3 lightning lamp",
    "level 4 lamp",
    "level 4 frenzy lamp",
    "level 4 blood lamp",
    "level 4 lightning lamp",
    "level 5 lamp",
    "level 5 frenzy lamp",
    "level 5 blood lamp",
    "level 5 lightning lamp",
  };

  public static final int NONE = 0;

  public static final int BOOKSHELF = 1;
  public static final int DRESSER = 2;
  public static final int CEILING_FAN = 3;
  public static final int COUCH = 4;
  public static final int LAMP = 5;
  public static final int DISHRACK = 6;

  public static final AdventureResult NO_RUNE = ItemPool.get("(none)", 1);
  public static final AdventureResult FRENZY_RUNE = ItemPool.get(ItemPool.VYKEA_FRENZY_RUNE, 1);
  public static final AdventureResult BLOOD_RUNE = ItemPool.get(ItemPool.VYKEA_BLOOD_RUNE, 1);
  public static final AdventureResult LIGHTNING_RUNE =
      ItemPool.get(ItemPool.VYKEA_LIGHTNING_RUNE, 1);

  private final int type;
  private final int level;
  private final AdventureResult rune;
  private final String name;

  // Derived fields
  private final String image;
  private final Element attackElement;
  private final String modifiers;
  private String stringForm;

  public static final VYKEACompanionData NO_COMPANION = new VYKEACompanionData();
  public static VYKEACompanionData currentCompanion = VYKEACompanionData.NO_COMPANION;

  public static void initialize(final boolean loadSettings) {
    VYKEACompanionData.currentCompanion = VYKEACompanionData.NO_COMPANION;
    if (loadSettings) {
      VYKEACompanionData.settingsToVYKEACompanion();
    }
  }

  private static void setVYKEACompanion(
      final VYKEACompanionData companion, final boolean setSettings) {
    VYKEACompanionData.currentCompanion = companion;

    if (setSettings) {

      Preferences.setString("_VYKEACompanionName", companion.name);
      Preferences.setInteger("_VYKEACompanionLevel", companion.level);
      Preferences.setString("_VYKEACompanionType", companion.typeToString());
      Preferences.setString("_VYKEACompanionRune", companion.runeToString());
    }
  }

  public static final VYKEACompanionData currentCompanion() {
    return VYKEACompanionData.currentCompanion;
  }

  public VYKEACompanionData() {
    this(NONE, 0, NO_RUNE, "");
  }

  public VYKEACompanionData(
      final int type, final int level, final AdventureResult rune, final String name) {
    this.type = type;
    this.level = level;
    this.rune = rune;
    this.name = name == null ? "" : name;

    // Derived fields
    this.image = (type < 1 || type > 6) ? "" : ("vykfurn" + type + ".gif");
    switch (this.type) {
      case BOOKSHELF:
        this.attackElement = Element.SPOOKY;
        this.modifiers = "";
        break;
      case DRESSER:
        this.attackElement = Element.SLEAZE;
        this.modifiers = "";
        break;
      case CEILING_FAN:
        this.attackElement = Element.COLD;
        this.modifiers = "";
        break;
      case COUCH:
        this.attackElement = Element.NONE;
        this.modifiers = "Meat Drop: +" + this.level * 10;
        break;
      case LAMP:
        this.attackElement = Element.HOT;
        this.modifiers = "Item Drop: +" + this.level * 10;
        break;
      case DISHRACK:
        this.attackElement = Element.STENCH;
        this.modifiers = "";
        break;
      default:
        this.attackElement = Element.NONE;
        this.modifiers = "";
        break;
    }

    // Build this on demand
    this.stringForm = null;
  }

  public int getType() {
    return this.type;
  }

  public int getLevel() {
    return this.level;
  }

  public AdventureResult getRune() {
    return this.rune;
  }

  public String getName() {
    return this.name;
  }

  public String getImage() {
    return this.image;
  }

  public String getModifiers() {
    return this.modifiers;
  }

  public Element getAttackElement() {
    return this.attackElement;
  }

  public static String typeToString(final int type) {
    switch (type) {
      case BOOKSHELF:
        return "bookshelf";
      case CEILING_FAN:
        return "ceiling fan";
      case COUCH:
        return "couch";
      case DISHRACK:
        return "dishrack";
      case DRESSER:
        return "dresser";
      case LAMP:
        return "lamp";
    }
    return "unknown";
  }

  public String typeToString() {
    return VYKEACompanionData.typeToString(this.type);
  }

  public static int stringToType(final String type) {
    return type == null
        ? NONE
        : type.equals("bookshelf")
            ? BOOKSHELF
            : type.equals("ceiling fan")
                ? CEILING_FAN
                : type.equals("couch")
                    ? COUCH
                    : type.equals("dishrack")
                        ? DISHRACK
                        : type.equals("dresser") ? DRESSER : type.equals("lamp") ? LAMP : NONE;
  }

  public static String runeToString(final AdventureResult rune) {
    switch (rune.getItemId()) {
      case ItemPool.VYKEA_FRENZY_RUNE:
        return "frenzy";
      case ItemPool.VYKEA_BLOOD_RUNE:
        return "blood";
      case ItemPool.VYKEA_LIGHTNING_RUNE:
        return "lightning";
    }
    return "";
  }

  public String runeToString() {
    return VYKEACompanionData.runeToString(this.rune);
  }

  public static AdventureResult stringToRune(final String rune) {
    return rune == null
        ? NO_RUNE
        : rune.equals("frenzy")
            ? FRENZY_RUNE
            : rune.equals("blood")
                ? BLOOD_RUNE
                : rune.equals("lightning") ? LIGHTNING_RUNE : NO_RUNE;
  }

  // CHEBLI the level 5 lamp
  private static final Pattern COMPANION_CHARPANE_PATTERN =
      Pattern.compile(
          "<b>(.*?)</b> the level (\\d).*(bookshelf|ceiling fan|couch|dishrack|dresser|lamp)");

  public static void parseCharpaneCompanion(final String string) {
    // Once you have created a companion today, you can't change it.
    // Don't waste time parsing it.
    if (VYKEACompanionData.currentCompanion != VYKEACompanionData.NO_COMPANION) {
      return;
    }

    Matcher matcher = COMPANION_CHARPANE_PATTERN.matcher(string);
    if (matcher.find()) {
      String name = matcher.group(1);
      int level = StringUtilities.parseInt(matcher.group(2));
      String typeString = matcher.group(3);
      int type = VYKEACompanionData.stringToType(typeString);
      // Use last saved rune
      AdventureResult rune =
          VYKEACompanionData.stringToRune(Preferences.getString("_VYKEACompanionRune"));

      VYKEACompanionData companion = new VYKEACompanionData(type, level, rune, name);
      VYKEACompanionData.setVYKEACompanion(companion, true);
    }
  }

  public static void settingsToVYKEACompanion() {
    String name = Preferences.getString("_VYKEACompanionName");
    int level = Preferences.getInteger("_VYKEACompanionLevel");
    int type = VYKEACompanionData.stringToType(Preferences.getString("_VYKEACompanionType"));
    AdventureResult rune =
        VYKEACompanionData.stringToRune(Preferences.getString("_VYKEACompanionRune"));

    VYKEACompanionData companion =
        type == NONE ? NO_COMPANION : new VYKEACompanionData(type, level, rune, name);
    VYKEACompanionData.setVYKEACompanion(companion, false);
  }

  @Override
  public String toString() {
    if (this.stringForm == null) {
      StringBuilder buffer = new StringBuilder();
      if (this.name != null && !this.name.equals("")) {
        buffer.append(this.name);
        buffer.append(", the ");
      }
      buffer.append("level ");
      buffer.append(this.level);
      if (this.rune != VYKEACompanionData.NO_RUNE) {
        buffer.append(" ");
        buffer.append(this.runeToString());
      }
      buffer.append(" ");
      buffer.append(this.typeToString());
      this.stringForm = buffer.toString();
    }
    return this.stringForm;
  }

  // CHEBLI, the level 5 blood lamp
  private static final Pattern COMPANION_PATTERN =
      Pattern.compile(
          " *(?:(.*?), the )?level ([12345])(?: (blood|frenzy|lightning))? (bookshelf|ceiling fan|couch|dishrack|dresser|lamp) *");

  public static VYKEACompanionData fromString(final String string) {
    Matcher matcher = COMPANION_PATTERN.matcher(string.toLowerCase());
    if (matcher.find()) {
      String name = matcher.group(1);
      int level = StringUtilities.parseInt(matcher.group(2));
      String runeString = matcher.group(3);
      AdventureResult rune = VYKEACompanionData.stringToRune(runeString);
      String typeString = matcher.group(4);
      int type = VYKEACompanionData.stringToType(typeString);

      return new VYKEACompanionData(type, level, rune, name);
    }
    return null;
  }

  // <span class='guts'>You bolt 5 more rails onto the piece of furniture and take a step back to
  // admire your new... lamp.  It's a lamp!<p>You decide to name it...
  // <b>&Aring;VOB&Eacute;</b></span>
  private static final Pattern CREATION_PATTERN =
      Pattern.compile(
          "<span class='guts'>.*?It's a (bookshelf|ceiling fan|couch|dishrack|dresser|lamp).*?<b>(.*?)</b></span>",
          Pattern.DOTALL);

  public static void assembleCompanion(final int choice, final int decision, final String text) {
    // choice 1120 - Some Assembly Required.
    // 1 - Start with 5 planks -> choice 1121 (if you have a rune) or choice 1122 (if you don't)
    // 2 - Start with 5 rails -> 1121 (if you have a rune) or choice 1122 (if you don't)
    // 6 - don't build anything
    if (choice == 1120) {
      switch (decision) {
        case 1:
          // Start with 5 planks -> bookshelf, ceiling fan, dresser
          ResultProcessor.processItem(ItemPool.VYKEA_PLANK, -5);
          break;
        case 2:
          // Start with 5 rails -> couch, dishrack, lamp
          ResultProcessor.processItem(ItemPool.VYKEA_RAIL, -5);
          break;
        case 6:
          // Do nothing
          return;
        default:
          // Invalid decision, presumably from URL manipulation.
          return;
      }

      // You've started construction and cannot abort from
      // here on. Remove the instructions from inventory.
      ResultProcessor.processItem(ItemPool.VYKEA_INSTRUCTIONS, -1);

      // Initialize preferences
      Preferences.setString("_VYKEACompanionName", "");
      Preferences.setInteger("_VYKEACompanionLevel", 0);
      Preferences.setString("_VYKEACompanionType", "");
      Preferences.setString("_VYKEACompanionRune", "");

      return;
    }

    // choice 1121 - Some Assembly Required
    // 1 - Add a frenzy rune -> choice 1122 (if you have at least 1 dowel) or 1123 (if you don't)
    // 2 - Add a blood rune -> choice 1122 (if you have at least 1 dowel) or 1123 (if you don't)
    // 3 - Add a lightning rune -> choice 1122 (if you have at least 1 dowel) or 1123 (if you don't)
    // 6 - Don't add any runes -> choice 1122 (if you have at least 1 dowel) or 1123 (if you don't)

    if (choice == 1121) {
      AdventureResult rune = NO_RUNE;

      switch (decision) {
        case 1:
          rune = FRENZY_RUNE;
          break;
        case 2:
          rune = BLOOD_RUNE;
          break;
        case 3:
          rune = LIGHTNING_RUNE;
          break;
        case 6:
          // Don't add any runes
          break;
        default:
          // Invalid decision, presumably from URL manipulation.
          return;
      }

      // Save the rune in the preference
      Preferences.setString("_VYKEACompanionRune", VYKEACompanionData.runeToString(rune));

      // Remove the rune from inventory
      if (rune != NO_RUNE) {
        ResultProcessor.processItem(rune.getItemId(), -1);
      }

      return;
    }

    // choice 1122 - Some Assembly Required
    // 1 - Add 1 dowel -> choice 1123
    // 2 - Add 11 dowels -> choice 1123
    // 3 - Add 23 dowels -> choice 1123
    // 4 - Add 37 dowels -> choice 1123
    // 6 - Don't add any dowels -> choice 1123

    if (choice == 1122) {
      int level = 1;
      int dowels = 0;

      switch (decision) {
        case 1:
          level = 2;
          dowels = 1;
          break;
        case 2:
          level = 3;
          dowels = 11;
          break;
        case 3:
          level = 4;
          dowels = 23;
          break;
        case 4:
          level = 5;
          dowels = 37;
          break;
        case 6:
          // Do not add any dowels
          break;
        default:
          // Invalid decision, presumably from URL manipulation.
          return;
      }

      // Save the level in the preference
      Preferences.setInteger("_VYKEACompanionLevel", level);

      // Remove the dowels from inventory
      if (dowels > 0) {
        ResultProcessor.processItem(ItemPool.VYKEA_DOWEL, -dowels);
      }
      return;
    }

    // choice 1123 - Some Assembly Required
    // 1 - Add 5 planks
    // 2 - Add 5 rails
    // 3 - Add 5 brackets
    if (choice == 1123) {
      switch (decision) {
        case 1:
          // Add 5 planks -> bookshelf, couch
          ResultProcessor.processItem(ItemPool.VYKEA_PLANK, -5);
          break;
        case 2:
          // Add 5 rails -> dresser, lamp
          ResultProcessor.processItem(ItemPool.VYKEA_RAIL, -5);
          break;
        case 3:
          // Add 5 brackets -> ceiling fan, dishrack
          ResultProcessor.processItem(ItemPool.VYKEA_BRACKET, -5);
          break;
        default:
          // Invalid decision, presumably from URL manipulation.
          return;
      }

      // Parse companion name and type from the result text
      Matcher matcher = CREATION_PATTERN.matcher(text);
      if (!matcher.find()) {
        // Unexpected. We'll pick it up from the charpane.
        return;
      }

      String name = matcher.group(2);
      String typeString = matcher.group(1);
      int type = VYKEACompanionData.stringToType(typeString);

      // Set them into preferences
      Preferences.setString("_VYKEACompanionName", name);
      Preferences.setString("_VYKEACompanionType", typeString);

      int level = Preferences.getInteger("_VYKEACompanionLevel");
      AdventureResult rune =
          VYKEACompanionData.stringToRune(Preferences.getString("_VYKEACompanionRune"));

      // Create the companion
      VYKEACompanionData companion = new VYKEACompanionData(type, level, rune, name);
      VYKEACompanionData.setVYKEACompanion(companion, false);

      // Adjust modifiers
      KoLCharacter.recalculateAdjustments();
      KoLCharacter.updateStatus();
    }
  }

  @Override
  public int compareTo(final VYKEACompanionData o) {
    if (o == null) {
      throw new NullPointerException();
    }

    if (o == this) {
      return 0;
    }

    if (this.type != o.type) {
      return this.type - o.type;
    }

    if (this.rune != o.rune) {
      return this.rune.getItemId() - o.rune.getItemId();
    }

    if (this.level != o.level) {
      return this.level - o.level;
    }

    return 0;
  }
}
