package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.ImageIcon;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.PokefamData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FamiliarDatabase {
  public enum FamiliarType {
    NONE("none"),
    STAT0("stat0"),
    STAT1("stat1"),
    ITEM0("item0"),
    ITEM1("item1"),
    ITEM2("item2"),
    ITEM3("item3"),
    MEAT0("meat0"),
    COMBAT0("combat0"),
    COMBAT1("combat1"),
    BLOCK("block"),
    DELEVEL0("delevel0"),
    DELEVEL1("delevel1"),
    HP0("hp0"),
    MP0("mp0"),
    MEAT1("meat1"),
    STAT2("stat2"),
    OTHER0("other0"),
    HP1("hp1"),
    MP1("mp1"),
    STAT3("stat3"),
    OTHER1("other1"),
    PASSIVE("passive"),
    DROP("drop"),
    UNDERWATER("underwater"),
    POKEFAM("pokefam"),
    VARIABLE("variable");

    private final String name;

    FamiliarType(final String name) {
      this.name = name;
    }
  }

  public static class FamiliarRaceData {
    private final int id;
    private final String name;
    private String image;
    private final EnumSet<FamiliarType> types;
    private final int larvaId;
    private final String item;
    private int[] skills;
    private final List<String> attributes;

    public FamiliarRaceData(
        final int id,
        final String name,
        final String image,
        final EnumSet<FamiliarType> types,
        final int larvaId,
        final String item,
        final int[] skills,
        final List<String> attributes) {
      this.id = id;
      this.name = name;
      this.image = image;
      this.types = types;
      this.larvaId = larvaId;
      this.item = item;
      this.skills = skills;
      this.attributes = attributes;
    }

    public String name() {
      return name;
    }

    String types() {
      String typeString = types.stream().map(x -> x.name).collect(Collectors.joining(","));
      if (typeString.isEmpty()) {
        return "none";
      }
      return typeString;
    }

    public boolean isType(final FamiliarType type) {
      return this.types.contains(type);
    }

    public boolean isAnyType(final EnumSet<FamiliarType> types) {
      for (FamiliarType type : types) {
        if (this.types.contains(type)) {
          return true;
        }
      }
      return false;
    }

    public boolean isVolleyType() {
      return isType(FamiliarType.STAT0);
    }

    public boolean isSombreroType() {
      return isType(FamiliarType.STAT1);
    }

    public boolean isFairyType() {
      return isType(FamiliarType.ITEM0);
    }

    public boolean isFoodFairyType() {
      return isType(FamiliarType.ITEM1);
    }

    public boolean isBoozeFairyType() {
      return isType(FamiliarType.ITEM2);
    }

    public boolean isCandyFairyType() {
      return isType(FamiliarType.ITEM3);
    }

    public boolean isMeatDropType() {
      return isType(FamiliarType.MEAT0);
    }

    public boolean isFairyType(final DoubleModifier fairyModifier) {
      return switch (fairyModifier) {
        case FAIRY_WEIGHT -> isFairyType();
        case BOOZE_FAIRY_WEIGHT -> isBoozeFairyType();
        case CANDY_FAIRY_WEIGHT -> isCandyFairyType();
        case FOOD_FAIRY_WEIGHT -> isFoodFairyType();
        default -> false;
      };
    }

    public boolean isCombatType() {
      return isAnyType(
          EnumSet.of(
              FamiliarType.COMBAT0,
              FamiliarType.COMBAT1,
              FamiliarType.BLOCK,
              FamiliarType.DELEVEL0,
              FamiliarType.DELEVEL1,
              FamiliarType.HP0,
              FamiliarType.MP0,
              FamiliarType.OTHER0));
    }

    public boolean isCombat0Type() {
      return isType(FamiliarType.COMBAT0);
    }

    public boolean isCombat1Type() {
      return isType(FamiliarType.COMBAT1);
    }

    public boolean isDropType() {
      return isType(FamiliarType.DROP);
    }

    public boolean isBlockType() {
      return isType(FamiliarType.BLOCK);
    }

    public boolean isDelevelType() {
      return isAnyType(EnumSet.of(FamiliarType.DELEVEL0, FamiliarType.DELEVEL1));
    }

    public boolean isHp0Type() {
      return isType(FamiliarType.HP0);
    }

    public boolean isMp0Type() {
      return isType(FamiliarType.MP0);
    }

    public boolean isMeat1Type() {
      return isType(FamiliarType.MEAT1);
    }

    public boolean isStat2Type() {
      return isType(FamiliarType.STAT2);
    }

    public boolean isOther0Type() {
      return isType(FamiliarType.OTHER0);
    }

    public boolean isHp1Type() {
      return isType(FamiliarType.HP1);
    }

    public boolean isMp1Type() {
      return isType(FamiliarType.MP1);
    }

    public boolean isStat3Type() {
      return isType(FamiliarType.STAT3);
    }

    public boolean isNoneType() {
      return isType(FamiliarType.NONE);
    }

    public boolean isOther1Type() {
      return isType(FamiliarType.OTHER1);
    }

    public boolean isPassiveType() {
      return isType(FamiliarType.PASSIVE);
    }

    public boolean isUnderwaterType() {
      if (id == FamiliarPool.ROBORTENDER) {
        String drinks = Preferences.getString("_roboDrinks");
        return drinks.contains("low tide martini") || drinks.contains("Bloody Nora");
      }

      return isType(FamiliarType.UNDERWATER);
    }

    public boolean isPokefamType() {
      return isType(FamiliarType.POKEFAM);
    }

    public boolean isVariableType() {
      return isType(FamiliarType.VARIABLE);
    }

    public int larvaId() {
      return larvaId;
    }

    public String toDataLine() {
      String larva = larvaId == -1 ? "" : ItemDatabase.getItemDataName(larvaId);

      var base =
          id + "\t" + name + "\t" + image + "\t" + types() + "\t" + larva + "\t" + item + "\t"
              + skills[0] + "\t" + skills[1] + "\t" + skills[2] + "\t" + skills[3];
      if (!attributes.isEmpty()) {
        base += "\t" + String.join(",", attributes);
      }
      return base;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  private static final Map<String, Integer> familiarByName = new TreeMap<>();
  private static final Map<String, String> canonicalNameMap = new HashMap<>();
  private static final Map<Integer, FamiliarRaceData> familiarDataById = new TreeMap<>();
  private static final Map<Integer, Integer> familiarByLarva = new HashMap<>();
  private static final Map<String, Integer> familiarByItem = new HashMap<>();
  private static final Map<String, Integer> familiarByImage = new HashMap<>();

  public static boolean newFamiliars = false;
  public static int maxFamiliarId = 0;
  private static String[] canonicalNames = new String[0];

  private static final Map<Integer, PokefamData> pokefamById = new TreeMap<>();
  private static final Map<String, PokefamData> pokefamByName = new TreeMap<>();

  static {
    FamiliarDatabase.reset();
  }

  private FamiliarDatabase() {}

  public static void reset() {
    FamiliarDatabase.newFamiliars = false;

    FamiliarDatabase.readFamiliars();
    FamiliarDatabase.saveCanonicalNames();
  }

  private static void readFamiliars() {
    try (BufferedReader reader =
        FileUtilities.getVersionedReader("familiars.txt", KoLConstants.FAMILIARS_VERSION)) {
      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length != 10 && data.length != 11) {
          continue;
        }

        int familiarId = StringUtilities.parseInt(data[0]);
        Integer id = familiarId;
        if (familiarId > FamiliarDatabase.maxFamiliarId) {
          FamiliarDatabase.maxFamiliarId = id;
        }

        String name = data[1];
        String canonical = StringUtilities.getCanonicalName(name);
        String display = StringUtilities.getDisplayName(name);

        FamiliarDatabase.familiarByName.put(name, id);
        FamiliarDatabase.canonicalNameMap.put(canonical, name);

        String image = data[2];
        FamiliarDatabase.familiarByImage.put(image, id);
        // Kludge: Happy Medium has 4 different images
        if (id == FamiliarPool.HAPPY_MEDIUM) {
          FamiliarDatabase.familiarByImage.put("medium_1.gif", id);
          FamiliarDatabase.familiarByImage.put("medium_2.gif", id);
          FamiliarDatabase.familiarByImage.put("medium_3.gif", id);
        }
        // Kludge: Melodramadary has multiple different images
        if (id == FamiliarPool.MELODRAMEDARY) {
          FamiliarDatabase.familiarByImage.put("camelfam_left.gif", id);
          FamiliarDatabase.familiarByImage.put("camelfam_middle.gif", id);
          FamiliarDatabase.familiarByImage.put("camelfam_right.gif", id);
        }

        EnumSet<FamiliarType> types = FamiliarDatabase.parseTypes(data[3]);

        int larvaId = ItemDatabase.getItemId(data[4]);
        FamiliarDatabase.familiarByLarva.put(larvaId, id);

        String itemName = data[5];
        FamiliarDatabase.familiarByItem.put(itemName, id);

        int[] skills = new int[4];
        for (int i = 0; i < 4; ++i) {
          skills[i] = Integer.parseInt(data[i + 6]);
        }
        List<String> attrs = List.of();
        if (data.length == 11 && !data[10].isEmpty()) {
          String[] list = StringUtilities.splitByComma(data[10]);
          attrs = Arrays.asList(list);
        }
        FamiliarDatabase.familiarDataById.put(
            id, new FamiliarRaceData(id, display, image, types, larvaId, itemName, skills, attrs));
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  private static void saveCanonicalNames() {
    String[] newArray = new String[FamiliarDatabase.canonicalNameMap.size()];
    FamiliarDatabase.canonicalNameMap.keySet().toArray(newArray);
    Arrays.sort(newArray);
    FamiliarDatabase.canonicalNames = newArray;
  }

  private static EnumSet<FamiliarType> parseTypes(final String typeString) {
    EnumSet<FamiliarType> types = EnumSet.noneOf(FamiliarType.class);
    for (FamiliarType type : FamiliarType.values()) {
      if (typeString.contains(type.name)) {
        types.add(type);
      }
    }
    if (types.isEmpty()) {
      types.add(FamiliarType.NONE);
    }
    return types;
  }

  static {
    // Do the same thing for PokefamData from fambattle.txt

    try (BufferedReader reader =
        FileUtilities.getVersionedReader("fambattle.txt", KoLConstants.FAMBATTLE_VERSION)) {
      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length != 8) {
          continue;
        }

        try {
          String race = data[0];
          String level2 = data[1];
          String level3 = data[2];
          String level4 = data[3];
          String move1 = data[4];
          String move2 = data[5];
          String move3 = data[6];
          String attribute = data[7];

          Integer id = FamiliarDatabase.familiarByName.get(race);
          if (id == null) {
            RequestLogger.printLine("Unknown familiar in fambattle.txt: " + race);
            continue;
          }

          PokefamData value =
              new PokefamData(race, level2, level3, level4, move1, move2, move3, attribute);

          FamiliarDatabase.pokefamById.put(id, value);
          FamiliarDatabase.pokefamByName.put(race, value);
        } catch (Exception e) {
          // This should not happen.  Therefore, print
          // a stack trace for debug purposes.

          StaticEntity.printStackTrace(e);
        }
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  /**
   * Temporarily adds a familiar to the familiar database. This is used whenever KoLmafia encounters
   * an unknown familiar on login
   */
  private static final Integer ZERO = 0;

  public static final void registerFamiliar(
      final int familiarId, final String familiarName, final String image) {
    FamiliarDatabase.registerFamiliar(familiarId, familiarName, image, FamiliarDatabase.ZERO);
  }

  // Hatches into:</b><br><table cellpadding=5 style='border: 1px solid black;'><tr><td
  // align=center><a class=nounder href=desc_familiar.php?which=154><img border=0
  // src=http://images.kingdomofloathing.com/itemimages/groose.gif width=30
  // height=30><br><b>Bloovian Groose</b></a></td></tr></table>

  private static final Pattern FAMILIAR_PATTERN =
      Pattern.compile(
          "Hatches into:.*?<table.*?which=(\\d*).*?itemimages/([^\"]*?)\"? .*?<b>(.*?)</b>");

  public static final void registerFamiliar(final Integer larvaId, final String text) {
    Matcher matcher = FAMILIAR_PATTERN.matcher(text);
    if (matcher.find()) {
      int familiarId = StringUtilities.parseInt(matcher.group(1));
      String image = matcher.group(2);
      String familiarName = matcher.group(3);
      FamiliarDatabase.registerFamiliar(familiarId, familiarName, image, larvaId);
    }
  }

  public static final void registerFamiliar(
      final int familiarId, final String familiarName, final String image, final Integer larvaId) {
    if (FamiliarDatabase.familiarByName.containsKey(familiarName)) {
      return;
    }

    var larvaName = ItemDatabase.getItemName(larvaId);
    var printMe =
        "New familiar: \""
            + larvaName
            + "\" hatches into \""
            + familiarName
            + "\" ("
            + familiarId
            + ") @ "
            + image;

    RequestLogger.printLine(printMe);
    RequestLogger.updateSessionLog(printMe);

    if (familiarId > FamiliarDatabase.maxFamiliarId) {
      FamiliarDatabase.maxFamiliarId = familiarId;
    }

    Integer id = familiarId;
    String canonical = StringUtilities.getCanonicalName(familiarName);

    FamiliarDatabase.familiarByName.put(familiarName, id);
    FamiliarDatabase.canonicalNameMap.put(canonical, familiarName);
    FamiliarDatabase.familiarByImage.put(image, id);
    FamiliarDatabase.familiarByLarva.put(larvaId, id);
    FamiliarDatabase.familiarDataById.put(
        id,
        new FamiliarRaceData(
            id,
            familiarName,
            image,
            EnumSet.of(FamiliarType.NONE),
            larvaId,
            "",
            new int[] {0, 0, 0, 0},
            List.of()));
    FamiliarDatabase.newFamiliars = true;
    FamiliarDatabase.saveCanonicalNames();
  }

  /**
   * Returns the name for an familiar, given its Id.
   *
   * @param familiarId The Id of the familiar to lookup
   * @return The name of the corresponding familiar
   */
  public static final String getFamiliarName(final Integer familiarId) {
    FamiliarRaceData data = getFamiliarRaceData(familiarId);
    return data == null ? null : data.name;
  }

  public static FamiliarRaceData getFamiliarRaceData(final Integer familiarId) {
    return FamiliarDatabase.familiarDataById.get(familiarId);
  }

  /**
   * Returns the Id number for an familiar, given its larval stage.
   *
   * @param larvaId The larva stage of the familiar to lookup
   * @return The Id number of the corresponding familiar
   */
  public static final FamiliarData growFamiliarLarva(final int larvaId) {
    Integer familiarId = FamiliarDatabase.familiarByLarva.get(larvaId);
    if (familiarId == null) {
      switch (larvaId) {
        case ItemPool.REPLICA_DARK_JILL -> familiarId = FamiliarPool.JILL_O_LANTERN;
        case ItemPool.REPLICA_HAND_TURKEY -> familiarId = FamiliarPool.HAND_TURKEY;
        case ItemPool.REPLICA_CRIMBO_ELF -> familiarId = FamiliarPool.CRIMBO_ELF;
        case ItemPool.REPLICA_BUGBEAR_SHAMAN -> familiarId = FamiliarPool.PYGMY_BUGBEAR_SHAMAN;
        case ItemPool.REPLICA_COTTON_CANDY_COCOON -> familiarId = FamiliarPool.CARNIE;
        case ItemPool.REPLICA_SQUAMOUS_POLYP -> familiarId = FamiliarPool.GIBBERER;
        case ItemPool.REPLICA_ORGAN_GRINDER -> familiarId = FamiliarPool.GRINDER;
        case ItemPool.REPLICA_CUTE_ANGEL -> familiarId = FamiliarPool.OBTUSE_ANGEL;
        case ItemPool.REPLICA_DEACTIVATED_NANOBOTS -> familiarId = FamiliarPool.NANORHINO;
        case ItemPool.REPLICA_BANDERSNATCH -> familiarId = FamiliarPool.BANDER;
        case ItemPool.REPLICA_STILL_GRILL -> familiarId = FamiliarPool.GALLOPING_GRILL;
        case ItemPool.REPLICA_CRIMBO_SAPLING -> familiarId = FamiliarPool.CRIMBO_SHRUB;
        case ItemPool.REPLICA_YELLOW_PUCK -> familiarId = FamiliarPool.PUCK_MAN;
        case ItemPool.REPLICA_INTERGNAT -> familiarId = FamiliarPool.INTERGNAT;
        case ItemPool.REPLICA_SPACE_PLANULA -> familiarId = FamiliarPool.SPACE_JELLYFISH;
        case ItemPool.REPLICA_ROBORTENDER -> familiarId = FamiliarPool.ROBORTENDER;
        case ItemPool.REPLICA_GOD_LOBSTER -> familiarId = FamiliarPool.GOD_LOBSTER;
        case ItemPool.REPLICA_CAMELCALF -> familiarId = FamiliarPool.MELODRAMEDARY;
        case ItemPool.REPLICA_GREY_GOSLING -> familiarId = FamiliarPool.GREY_GOOSE;
        case ItemPool.REPLICA_PATRIOTIC_EAGLE -> familiarId = FamiliarPool.PATRIOTIC_EAGLE;
      }
    }
    return familiarId == null ? null : new FamiliarData(familiarId);
  }

  /**
   * Returns the Id number for an familiar, given its name.
   *
   * @param name The name of the familiar to lookup
   * @return The Id number of the corresponding familiar
   */
  public static final int getFamiliarId(final String name) {
    return getFamiliarId(name, true);
  }

  public static final int getFamiliarId(final String name, final boolean substringMatch) {
    // Look for an exact match first
    Integer familiarId = FamiliarDatabase.familiarByName.get(name);
    if (familiarId != null) {
      return familiarId;
    }

    if (!substringMatch) {
      return -1;
    }

    String canonical = StringUtilities.getCanonicalName(name);
    List<String> possibilities =
        StringUtilities.getMatchingNames(FamiliarDatabase.canonicalNames, canonical);
    int matches = possibilities.size();

    if (matches == 1) {
      String first = possibilities.get(0);
      String realName = FamiliarDatabase.canonicalNameMap.get(first);
      return FamiliarDatabase.familiarByName.get(realName);
    }

    return -1;
  }

  public static final String getFamiliarItem(final Integer familiarId) {
    FamiliarRaceData data = getFamiliarRaceData(familiarId);
    return data == null ? null : data.item;
  }

  public static final int getFamiliarItemId(final Integer familiarId) {
    String name = FamiliarDatabase.getFamiliarItem(familiarId);
    return name == null ? -1 : ItemDatabase.getItemId(name);
  }

  public static final int getFamiliarByItem(final String item) {
    Integer familiarId = FamiliarDatabase.familiarByItem.get(item);
    return familiarId == null ? -1 : familiarId;
  }

  public static int getFamiliarLarva(final Integer familiarId) {
    FamiliarRaceData data = getFamiliarRaceData(familiarId);
    return data == null ? 0 : data.larvaId;
  }

  public static final void setFamiliarImageLocation(final int familiarId, final String location) {
    FamiliarRaceData current = getFamiliarRaceData(familiarId);
    if (current == null) {
      return;
    }
    current.image = location;
    FamiliarDatabase.familiarByImage.put(location, familiarId);
  }

  public static final String getFamiliarImageLocation(final int familiarId) {
    FamiliarRaceData data = getFamiliarRaceData(familiarId);
    String location = data == null ? null : data.image;
    return (location != null) ? location : "debug.gif";
  }

  public static final String getFamiliarFightImageLocation(final int familiarId) {
    // Melodramadery' image is composed of three images
    return (familiarId == FamiliarPool.MELODRAMEDARY)
        ? "camelfam_left.gif"
        : getFamiliarImageLocation(familiarId);
  }

  public static final int getFamiliarByImageLocation(final String image) {
    Integer familiarId = FamiliarDatabase.familiarByImage.get(image);
    return familiarId == null ? -1 : familiarId;
  }

  private static ImageIcon getFamiliarIcon(final String location) {
    if (location == null || location.equals("debug.gif")) {
      return FamiliarDatabase.getNoFamiliarImage();
    }
    String path = (location.contains("/") ? "" : "itemimages/") + location;
    String url = KoLmafia.imageServerPath() + path;
    File file = FileUtilities.downloadImage(url);
    if (file == null) {
      return FamiliarDatabase.getNoFamiliarImage();
    }
    ImageIcon icon = JComponentUtilities.getImage(path);
    return icon != null ? icon : FamiliarDatabase.getNoFamiliarImage();
  }

  public static final ImageIcon getNoFamiliarImage() {
    return JComponentUtilities.getImage("debug.gif");
  }

  public static final ImageIcon getFamiliarImage(final String name) {
    return FamiliarDatabase.getFamiliarImage(FamiliarDatabase.getFamiliarId(name));
  }

  public static final ImageIcon getFamiliarImage(final int familiarId) {
    String location = FamiliarDatabase.getFamiliarImageLocation(familiarId);
    return FamiliarDatabase.getFamiliarIcon(location);
  }

  public static final ImageIcon getCurrentFamiliarImage() {
    String location = KoLCharacter.getFamiliarImage();
    return FamiliarDatabase.getFamiliarIcon(location);
  }

  /**
   * Returns whether or not a familiar with a given name exists in the database.
   *
   * @return <code>true</code> if the familiar is in the database
   */
  public static final boolean contains(final String familiarName) {
    return FamiliarDatabase.familiarByName.containsKey(familiarName);
  }

  public static final Integer getFamiliarSkill(final Integer familiarId, final int event) {
    if (event < 1 || event > 4) {
      return null;
    }
    if (!FamiliarDatabase.familiarDataById.containsKey(familiarId)) {
      return null;
    }
    return FamiliarDatabase.getFamiliarSkills(familiarId)[event - 1];
  }

  public static final int[] getFamiliarSkills(final Integer id) {
    FamiliarRaceData data = getFamiliarRaceData(id);
    return data == null ? new int[] {0, 0, 0, 0} : data.skills.clone();
  }

  public static final void setFamiliarSkills(final Integer familiarId, final int[] skills) {
    FamiliarRaceData current = getFamiliarRaceData(familiarId);
    if (current == null) {
      return;
    }
    current.skills = skills.clone();
    FamiliarDatabase.newFamiliars = true;
    FamiliarDatabase.saveDataOverride();
  }

  public static final List<String> getFamiliarAttributes(final int familiarId) {
    FamiliarRaceData data = getFamiliarRaceData(familiarId);
    return data == null ? List.of() : data.attributes;
  }

  public static final boolean hasAttribute(final String name, final String attribute) {
    int familiarId = FamiliarDatabase.getFamiliarId(name);
    if (familiarId == -1) {
      return false;
    }
    return FamiliarDatabase.hasAttribute(familiarId, attribute);
  }

  public static final boolean hasAttribute(final int familiarId, final String attribute) {
    List<String> attrs = FamiliarDatabase.getFamiliarAttributes(familiarId);
    return attrs.contains(attribute);
  }

  public static final PokefamData getPokeDataByName(final String name) {
    return FamiliarDatabase.pokefamByName.get(name);
  }

  public static final PokefamData getPokeDataById(final int id) {
    return FamiliarDatabase.pokefamById.get(id);
  }

  /**
   * Returns the set of familiars keyed by name
   *
   * @return The set of familiars keyed by name
   */
  public static final Set<Entry<Integer, FamiliarRaceData>> entrySet() {
    return FamiliarDatabase.familiarDataById.entrySet();
  }

  public static final void saveDataOverride() {
    FamiliarDatabase.writeFamiliars(new File(KoLConstants.DATA_LOCATION, "familiars.txt"));
    FamiliarDatabase.newFamiliars = false;
  }

  public static void writeFamiliars(final File output) {
    RequestLogger.printLine("Writing data override: " + output);
    PrintStream writer = LogStream.openStream(output, true);
    try (writer) {
      writeFamiliars(writer);
    }
  }

  static void writeFamiliars(final PrintStream writer) {
    writer.println(KoLConstants.FAMILIARS_VERSION);

    writer.println("# Original familiar arena stats from Vladjimir's arena data");
    writer.println("# http://www.therye.org/familiars/");
    writer.println();
    writer.println("# no.	name	image	type	larva	item	CM	SH	OC	H&S");
    writer.println();

    int lastInteger = 1;
    for (var familiar : FamiliarDatabase.entrySet()) {
      int familiarId = familiar.getKey();

      for (int j = lastInteger; j < familiarId; ++j) {
        writer.println(j);
      }

      lastInteger = familiarId + 1;

      FamiliarRaceData data = getFamiliarRaceData(familiarId);
      writer.println(data.toDataLine());
    }
  }

  // ****** PokefamData support

  private static final String UNKNOWN_LEVEL = "x/x";

  public static void registerPokefam(
      String race,
      int level,
      int power,
      int hp,
      String attribute,
      String move1,
      String move2,
      String move3) {
    PokefamData current = FamiliarDatabase.getPokeDataByName(race);

    // If no data on this familiar, create new entry
    if (current == null) {
      String levelData = power + "/" + hp;
      String level2 = level == 2 ? levelData : UNKNOWN_LEVEL;
      String level3 = level == 3 ? levelData : UNKNOWN_LEVEL;
      String level4 = level >= 4 ? levelData : UNKNOWN_LEVEL;
      String ultimate = move3 != null ? move3 : "Unknown";

      PokefamData data =
          new PokefamData(race, level2, level3, level4, move1, move2, ultimate, attribute);

      int id = FamiliarDatabase.getFamiliarId(race);
      FamiliarDatabase.pokefamById.put(id, data);
      FamiliarDatabase.pokefamByName.put(race, data);
      printNewPokefamData(data);
      return;
    }

    // We have data on this familiar. If anything is different, update existing record
    boolean update = false;

    if (!move1.equals(current.getMove1())) {
      current.setMove1(move1);
      update = true;
    }

    if (!move2.equals(current.getMove2())) {
      current.setMove2(move2);
      update = true;
    }

    if (!attribute.equals(current.getAttribute())) {
      current.setAttribute(attribute);
      update = true;
    }

    switch (level) {
      case 2:
        if (power != current.getPower2()) {
          current.setPower2(power);
          update = true;
        }
        if (hp != current.getHP2()) {
          current.setHP2(hp);
          update = true;
        }
        break;
      case 3:
        if (power != current.getPower3()) {
          current.setPower3(power);
          update = true;
        }
        if (hp != current.getHP3()) {
          current.setHP3(hp);
          update = true;
        }
        break;
      case 5:
        if (!move3.equals(current.getMove3())) {
          current.setMove3(move3);
          update = true;
        }
      // Fall through
      case 4:
        if (power != current.getPower4()) {
          current.setPower4(power);
          update = true;
        }
        if (hp != current.getHP4()) {
          current.setHP4(hp);
          update = true;
        }
        break;
    }

    if (update) {
      printNewPokefamData(current);
    }
  }

  private static void printNewPokefamData(PokefamData data) {
    String printMe;
    // Print what goes in fambattle.txt
    printMe = "--------------------";
    RequestLogger.printLine(printMe);
    RequestLogger.updateSessionLog(printMe);

    printMe = FamiliarDatabase.pokefamString(data);
    RequestLogger.printLine(printMe);
    RequestLogger.updateSessionLog(printMe);

    printMe = "--------------------";
    RequestLogger.printLine(printMe);
    RequestLogger.updateSessionLog(printMe);
  }

  private static String pokefamString(PokefamData data) {

    String buffer =
        data.getRace()
            + "\t"
            + (data.getPower2() == 0 ? "x" : String.valueOf(data.getPower2()))
            + "/"
            + (data.getHP2() == 0 ? "x" : String.valueOf(data.getHP2()))
            + "\t"
            + (data.getPower3() == 0 ? "x" : String.valueOf(data.getPower3()))
            + "/"
            + (data.getHP3() == 0 ? "x" : String.valueOf(data.getHP3()))
            + "\t"
            + (data.getPower4() == 0 ? "x" : String.valueOf(data.getPower4()))
            + "/"
            + (data.getHP4() == 0 ? "x" : String.valueOf(data.getHP4()))
            + "\t"
            + data.getMove1()
            + "\t"
            + data.getMove2()
            + "\t"
            + data.getMove3()
            + "\t"
            + data.getAttribute();
    return buffer;
  }

  public static int zootomistTrackCopies(int id) {
    var intensity = zootomistTrackIntensity(id);
    if (intensity < 0.3) return 2;
    if (intensity < 0.6) return 3;
    if (intensity < 0.9) return 4;
    return 5;
  }

  public static int zootomistBanishDuration(int id) {
    var intensity = zootomistBanishIntensity(id);
    var isFree = intensity == 1;
    var duration = (int) Math.floor(intensity * 90) + 10; // based on two data points
    if (!isFree) {
      duration -= 1;
    }
    return duration;
  }

  private static double zootomistBanishIntensity(int id) {
    return zootomistIntensity(
        id,
        Set.of(
            "animatedart",
            "hard",
            "hasbones",
            "haslegs",
            "haswings",
            "spooky",
            "swims",
            "vegetable"));
  }

  private static double zootomistTrackIntensity(int id) {
    return zootomistIntensity(
        id,
        Set.of("animal", "haseyes", "hot", "humanoid", "mineral", "orb", "sentient", "software"));
  }

  private static double zootomistIntensity(int id, Set<String> skillAttrs) {
    List<String> attrs = FamiliarDatabase.getFamiliarAttributes(id);
    if (attrs.isEmpty()) {
      return 0;
    }
    var relevantAttrs = attrs.stream().filter(skillAttrs::contains).count();
    return (double) relevantAttrs / attrs.size();
  }
}
