package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.ImageIcon;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.PokefamData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FamiliarDatabase {
  private static final Map<Integer, String> familiarById = new TreeMap<>();
  private static final Map<String, Integer> familiarByName = new TreeMap<>();
  private static final Map<String, String> canonicalNameMap = new HashMap<>();

  private static final Map<Integer, String> familiarItemById = new HashMap<>();
  private static final Map<String, Integer> familiarByItem = new HashMap<>();

  private static final Map<Integer, Integer> familiarLarvaById = new HashMap<>();
  private static final Map<Integer, Integer> familiarByLarva = new HashMap<>();

  private static final Map<Integer, String> familiarImageById = new HashMap<>();
  private static final Map<String, Integer> familiarByImage = new HashMap<>();

  private static final Set<Integer> volleyById = new HashSet<>();
  private static final Set<Integer> sombreroById = new HashSet<>();
  private static final Set<Integer> meatDropById = new HashSet<>();
  private static final Set<Integer> fairyById = new HashSet<>();

  private static final Set<Integer> combat0ById = new HashSet<>();
  private static final Set<Integer> combat1ById = new HashSet<>();
  private static final Set<Integer> blockById = new HashSet<>();
  private static final Set<Integer> delevelById = new HashSet<>();
  private static final Set<Integer> meat1ById = new HashSet<>();
  private static final Set<Integer> stat2ById = new HashSet<>();
  private static final Set<Integer> hp0ById = new HashSet<>();
  private static final Set<Integer> mp0ById = new HashSet<>();
  private static final Set<Integer> other0ById = new HashSet<>();

  private static final Set<Integer> hp1ById = new HashSet<>();
  private static final Set<Integer> mp1ById = new HashSet<>();
  private static final Set<Integer> stat3ById = new HashSet<>();
  private static final Set<Integer> other1ById = new HashSet<>();

  private static final Set<Integer> passiveById = new HashSet<>();
  private static final Set<Integer> dropById = new HashSet<>();
  private static final Set<Integer> underwaterById = new HashSet<>();

  private static final Set<Integer> noneById = new HashSet<>();
  private static final Set<Integer> variableById = new HashSet<>();

  private static final Map<String, Integer>[] eventSkillByName = new HashMap[4];
  private static final Map<Integer, List<String>> attributesById = new HashMap<>();

  public static boolean newFamiliars = false;
  public static int maxFamiliarId = 0;
  private static String[] canonicalNames = new String[0];

  private static final Map<Integer, PokefamData> pokefamById = new TreeMap<>();
  private static final Map<String, PokefamData> pokefamByName = new TreeMap<>();

  static {
    FamiliarDatabase.reset();
  }

  public static void reset() {
    FamiliarDatabase.newFamiliars = false;

    for (int i = 0; i < 4; ++i) {
      FamiliarDatabase.eventSkillByName[i] = new HashMap<String, Integer>();
    }
    FamiliarDatabase.readFamiliars();
    FamiliarDatabase.saveCanonicalNames();
  }

  private static void readFamiliars() {
    BufferedReader reader =
        FileUtilities.getVersionedReader("familiars.txt", KoLConstants.FAMILIARS_VERSION);

    String[] data;

    while ((data = FileUtilities.readData(reader)) != null) {
      if (data.length != 10 && data.length != 11) {
        continue;
      }

      int familiarId = StringUtilities.parseInt(data[0]);
      Integer id = IntegerPool.get(familiarId);
      if (familiarId > FamiliarDatabase.maxFamiliarId) {
        FamiliarDatabase.maxFamiliarId = id;
      }

      String name = data[1];
      String canonical = StringUtilities.getCanonicalName(name);
      String display = StringUtilities.getDisplayName(name);

      FamiliarDatabase.familiarById.put(id, display);
      FamiliarDatabase.familiarByName.put(name, id);
      FamiliarDatabase.canonicalNameMap.put(canonical, name);

      String image = data[2];
      FamiliarDatabase.familiarImageById.put(id, image);
      FamiliarDatabase.familiarByImage.put(image, id);
      // Kludge: Happy Medium has 4 different images
      if (id == FamiliarPool.HAPPY_MEDIUM) {
        FamiliarDatabase.familiarByImage.put("medium_1.gif", id);
        FamiliarDatabase.familiarByImage.put("medium_2.gif", id);
        FamiliarDatabase.familiarByImage.put("medium_3.gif", id);
      }

      String type = data[3];
      FamiliarDatabase.updateType(type, id);

      String larvaName = data[4];
      Integer larva = Integer.valueOf(ItemDatabase.getItemId(larvaName));
      FamiliarDatabase.familiarLarvaById.put(id, larva);
      FamiliarDatabase.familiarByLarva.put(larva, id);

      String itemName = data[5];
      FamiliarDatabase.familiarItemById.put(id, itemName);
      FamiliarDatabase.familiarByItem.put(itemName, id);

      for (int i = 0; i < 4; ++i) {
        FamiliarDatabase.eventSkillByName[i].put(name, Integer.valueOf(data[i + 6]));
      }

      if (data.length == 11) {
        String[] list = data[10].split("\\s*,\\s*");
        List<String> attrs = Arrays.asList(list);
        FamiliarDatabase.attributesById.put(id, attrs);
      }
    }

    try {
      reader.close();
    } catch (Exception e) {
      StaticEntity.printStackTrace(e);
    }
  }

  private static void saveCanonicalNames() {
    String[] newArray = new String[FamiliarDatabase.canonicalNameMap.size()];
    FamiliarDatabase.canonicalNameMap.keySet().toArray(newArray);
    Arrays.sort(newArray);
    FamiliarDatabase.canonicalNames = newArray;
  }

  private static void updateType(final String type, final Integer id) {
    FamiliarDatabase.updateType(type, "stat0", id, volleyById);
    FamiliarDatabase.updateType(type, "stat1", id, sombreroById);
    FamiliarDatabase.updateType(type, "item0", id, fairyById);
    FamiliarDatabase.updateType(type, "meat0", id, meatDropById);

    // The following are "combat" abilities
    FamiliarDatabase.updateType(type, "combat0", id, combat0ById);
    FamiliarDatabase.updateType(type, "combat1", id, combat1ById);
    FamiliarDatabase.updateType(type, "block", id, blockById);
    FamiliarDatabase.updateType(type, "delevel", id, delevelById);
    FamiliarDatabase.updateType(type, "hp0", id, hp0ById);
    FamiliarDatabase.updateType(type, "mp0", id, mp0ById);
    FamiliarDatabase.updateType(type, "meat1", id, meat1ById);
    FamiliarDatabase.updateType(type, "stat2", id, stat2ById);
    FamiliarDatabase.updateType(type, "other0", id, other0ById);

    // The following are "after combat" abilities
    FamiliarDatabase.updateType(type, "hp1", id, hp1ById);
    FamiliarDatabase.updateType(type, "mp1", id, mp1ById);
    FamiliarDatabase.updateType(type, "stat3", id, stat3ById);
    FamiliarDatabase.updateType(type, "other1", id, other1ById);

    // The following are other abilities that deserve their own category
    FamiliarDatabase.updateType(type, "passive", id, passiveById);
    FamiliarDatabase.updateType(type, "drop", id, dropById);
    FamiliarDatabase.updateType(type, "underwater", id, underwaterById);

    FamiliarDatabase.updateType(type, "none", id, noneById);
    FamiliarDatabase.updateType(type, "variable", id, variableById);
  }

  private static void updateType(
      final String type, final String key, final Integer id, final Set<Integer> set) {
    if (type.contains(key)) {
      set.add(id);
    }
  }

  static {
    // Do the same thing for PokefamData from fambattle.txt

    BufferedReader reader =
        FileUtilities.getVersionedReader("fambattle.txt", KoLConstants.FAMBATTLE_VERSION);

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

    try {
      reader.close();
    } catch (Exception e) {
      // This should not happen.  Therefore, print
      // a stack trace for debug purposes.

      StaticEntity.printStackTrace(e);
    }
  }

  /**
   * Temporarily adds a familiar to the familiar database. This is used whenever KoLmafia encounters
   * an unknown familiar on login
   */
  private static final Integer ZERO = IntegerPool.get(0);

  public static final void registerFamiliar(
      final int familiarId, final String familiarName, final String image) {
    FamiliarDatabase.registerFamiliar(familiarId, familiarName, image, FamiliarDatabase.ZERO);
  }

  // Hatches into:</b><br><table cellpadding=5 style='border: 1px solid black;'><tr><td
  // align=center><a class=nounder href=desc_familiar.php?which=154><img border=0
  // src=http://images.kingdomofloathing.com/itemimages/groose.gif width=30
  // height=30><br><b>Bloovian Groose</b></a></td></tr></table>

  private static final Pattern FAMILIAR_PATTERN =
      Pattern.compile("Hatches into:.*?<table.*?which=(\\d*).*?itemimages/(.*?) .*?<b>(.*?)</b>");

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

    RequestLogger.printLine(
        "New familiar: \"" + familiarName + "\" (" + familiarId + ") @ " + image);

    if (familiarId > FamiliarDatabase.maxFamiliarId) {
      FamiliarDatabase.maxFamiliarId = familiarId;
    }

    Integer id = IntegerPool.get(familiarId);
    String canonical = StringUtilities.getCanonicalName(familiarName);

    FamiliarDatabase.familiarById.put(id, familiarName);
    FamiliarDatabase.familiarByName.put(familiarName, id);
    FamiliarDatabase.canonicalNameMap.put(canonical, familiarName);
    FamiliarDatabase.familiarImageById.put(id, image);
    FamiliarDatabase.familiarByImage.put(image, id);
    FamiliarDatabase.familiarLarvaById.put(id, larvaId);
    FamiliarDatabase.familiarByLarva.put(larvaId, id);
    FamiliarDatabase.familiarItemById.put(id, "");
    for (int i = 0; i < 4; ++i) {
      FamiliarDatabase.eventSkillByName[i].put(familiarName, FamiliarDatabase.ZERO);
    }
    FamiliarDatabase.newFamiliars = true;
    FamiliarDatabase.saveCanonicalNames();
  }

  /**
   * Returns the name for an familiar, given its Id.
   *
   * @param familiarId The Id of the familiar to lookup
   * @return The name of the corresponding familiar
   */
  public static final String getFamiliarName(final int familiarId) {
    return FamiliarDatabase.getFamiliarName(IntegerPool.get(familiarId));
  }

  public static final String getFamiliarName(final Integer familiarId) {
    return FamiliarDatabase.familiarById.get(familiarId);
  }

  /**
   * Returns the Id number for an familiar, given its larval stage.
   *
   * @param larvaId The larva stage of the familiar to lookup
   * @return The Id number of the corresponding familiar
   */
  public static final FamiliarData growFamiliarLarva(final int larvaId) {
    Integer familiarId = FamiliarDatabase.familiarByLarva.get(IntegerPool.get(larvaId));
    return familiarId == null ? null : new FamiliarData(familiarId.intValue());
  }

  /**
   * Returns the Id number for an familiar, given its name.
   *
   * @param substring The name of the familiar to lookup
   * @return The Id number of the corresponding familiar
   */
  public static final int getFamiliarId(final String name) {
    return getFamiliarId(name, true);
  }

  public static final int getFamiliarId(final String name, final boolean substringMatch) {
    // Look for an exact match first
    Integer familiarId = FamiliarDatabase.familiarByName.get(name);
    if (familiarId != null) {
      return familiarId.intValue();
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

  public static final boolean isVolleyType(final Integer familiarId) {
    return FamiliarDatabase.volleyById.contains(familiarId);
  }

  public static final boolean isSombreroType(final Integer familiarId) {
    return FamiliarDatabase.sombreroById.contains(familiarId);
  }

  public static final boolean isFairyType(final Integer familiarId) {
    return FamiliarDatabase.fairyById.contains(familiarId);
  }

  public static final boolean isMeatDropType(final Integer familiarId) {
    return FamiliarDatabase.meatDropById.contains(familiarId);
  }

  public static final boolean isCombatType(final Integer familiarId) {
    return FamiliarDatabase.combat0ById.contains(familiarId)
        || FamiliarDatabase.combat1ById.contains(familiarId)
        || FamiliarDatabase.blockById.contains(familiarId)
        || FamiliarDatabase.delevelById.contains(familiarId)
        || FamiliarDatabase.hp0ById.contains(familiarId)
        || FamiliarDatabase.mp0ById.contains(familiarId)
        || FamiliarDatabase.other0ById.contains(familiarId);
  }

  public static final boolean isCombat0Type(final Integer familiarId) {
    return FamiliarDatabase.combat0ById.contains(familiarId);
  }

  public static final boolean isCombat1Type(final Integer familiarId) {
    return FamiliarDatabase.combat1ById.contains(familiarId);
  }

  public static final boolean isDropType(final Integer familiarId) {
    return FamiliarDatabase.dropById.contains(familiarId);
  }

  public static final boolean isBlockType(final Integer familiarId) {
    return FamiliarDatabase.blockById.contains(familiarId);
  }

  public static final boolean isDelevelType(final Integer familiarId) {
    return FamiliarDatabase.delevelById.contains(familiarId);
  }

  public static final boolean isHp0Type(final Integer familiarId) {
    return FamiliarDatabase.hp0ById.contains(familiarId);
  }

  public static final boolean isMp0Type(final Integer familiarId) {
    return FamiliarDatabase.mp0ById.contains(familiarId);
  }

  public static final boolean isMeat1Type(final Integer familiarId) {
    return FamiliarDatabase.meat1ById.contains(familiarId);
  }

  public static final boolean isStat2Type(final Integer familiarId) {
    return FamiliarDatabase.stat2ById.contains(familiarId);
  }

  public static final boolean isOther0Type(final Integer familiarId) {
    return FamiliarDatabase.other0ById.contains(familiarId);
  }

  public static final boolean isHp1Type(final Integer familiarId) {
    return FamiliarDatabase.hp1ById.contains(familiarId);
  }

  public static final boolean isMp1Type(final Integer familiarId) {
    return FamiliarDatabase.mp1ById.contains(familiarId);
  }

  public static final boolean isStat3Type(final Integer familiarId) {
    return FamiliarDatabase.stat3ById.contains(familiarId);
  }

  public static final boolean isNoneType(final Integer familiarId) {
    return FamiliarDatabase.noneById.contains(familiarId);
  }

  public static final boolean isOther1Type(final Integer familiarId) {
    return FamiliarDatabase.other1ById.contains(familiarId);
  }

  public static final boolean isPassiveType(final Integer familiarId) {
    return FamiliarDatabase.passiveById.contains(familiarId);
  }

  public static final boolean isUnderwaterType(final Integer familiarId) {
    if (familiarId == FamiliarPool.ROBORTENDER) {
      String drinks = Preferences.getString("_roboDrinks");
      return drinks.contains("low tide martini") || drinks.contains("Bloody Nora");
    }

    return FamiliarDatabase.underwaterById.contains(familiarId);
  }

  public static final boolean isVariableType(final Integer familiarId) {
    return FamiliarDatabase.variableById.contains(familiarId);
  }

  public static final String getFamiliarItem(final int familiarId) {
    return FamiliarDatabase.getFamiliarItem(IntegerPool.get(familiarId));
  }

  public static final String getFamiliarItem(final Integer familiarId) {
    return FamiliarDatabase.familiarItemById.get(familiarId);
  }

  public static final int getFamiliarItemId(final int familiarId) {
    return FamiliarDatabase.getFamiliarItemId(IntegerPool.get(familiarId));
  }

  public static final int getFamiliarItemId(final Integer familiarId) {
    String name = FamiliarDatabase.getFamiliarItem(familiarId);
    return name == null ? -1 : ItemDatabase.getItemId(name);
  }

  public static final int getFamiliarByItem(final String item) {
    Integer familiarId = FamiliarDatabase.familiarByItem.get(item);
    return familiarId == null ? -1 : familiarId.intValue();
  }

  public static final int getFamiliarLarva(final int familiarId) {
    return FamiliarDatabase.getFamiliarLarva(IntegerPool.get(familiarId));
  }

  public static final int getFamiliarLarva(final Integer familiarId) {
    Integer id = FamiliarDatabase.familiarLarvaById.get(familiarId);
    return id == null ? 0 : id.intValue();
  }

  public static final String getFamiliarType(final int familiarId) {
    StringBuilder buffer = new StringBuilder();
    String sep = "";

    // Base types: Leprechaun, Fairy, Volleyball, Sombrero
    if (FamiliarDatabase.meatDropById.contains(familiarId)) {
      buffer.append(sep);
      sep = ",";
      buffer.append("meat0");
    }
    if (FamiliarDatabase.fairyById.contains(familiarId)) {
      buffer.append(sep);
      sep = ",";
      buffer.append("item0");
    }
    if (FamiliarDatabase.volleyById.contains(familiarId)) {
      buffer.append(sep);
      sep = ",";
      buffer.append("stat0");
    }
    if (FamiliarDatabase.sombreroById.contains(familiarId)) {
      buffer.append(sep);
      sep = ",";
      buffer.append("stat1");
    }

    // Combat abilities
    if (FamiliarDatabase.combat0ById.contains(familiarId)) {
      buffer.append(sep);
      sep = ",";
      buffer.append("combat0");
    }
    if (FamiliarDatabase.combat1ById.contains(familiarId)) {
      buffer.append(sep);
      sep = ",";
      buffer.append("combat1");
    }
    if (FamiliarDatabase.blockById.contains(familiarId)) {
      buffer.append(sep);
      sep = ",";
      buffer.append("block");
    }
    if (FamiliarDatabase.delevelById.contains(familiarId)) {
      buffer.append(sep);
      sep = ",";
      buffer.append("delevel");
    }
    if (FamiliarDatabase.hp0ById.contains(familiarId)) {
      buffer.append(sep);
      sep = ",";
      buffer.append("hp0");
    }
    if (FamiliarDatabase.mp0ById.contains(familiarId)) {
      buffer.append(sep);
      sep = ",";
      buffer.append("mp0");
    }
    if (FamiliarDatabase.other0ById.contains(familiarId)) {
      buffer.append(sep);
      sep = ",";
      buffer.append("other0");
    }
    if (FamiliarDatabase.meat1ById.contains(familiarId)) {
      buffer.append(sep);
      sep = ",";
      buffer.append("meat1");
    }
    if (FamiliarDatabase.stat2ById.contains(familiarId)) {
      buffer.append(sep);
      sep = ",";
      buffer.append("stat2");
    }

    // After Combat abilities
    if (FamiliarDatabase.hp1ById.contains(familiarId)) {
      buffer.append(sep);
      sep = ",";
      buffer.append("hp1");
    }
    if (FamiliarDatabase.mp1ById.contains(familiarId)) {
      buffer.append(sep);
      sep = ",";
      buffer.append("mp1");
    }
    if (FamiliarDatabase.other1ById.contains(familiarId)) {
      buffer.append(sep);
      sep = ",";
      buffer.append("other1");
    }

    if (FamiliarDatabase.passiveById.contains(familiarId)) {
      buffer.append(sep);
      sep = ",";
      buffer.append("passive");
    }
    if (FamiliarDatabase.underwaterById.contains(familiarId)) {
      buffer.append(sep);
      sep = ",";
      buffer.append("underwater");
    }

    if (FamiliarDatabase.variableById.contains(familiarId)) {
      buffer.append(sep);
      sep = ",";
      buffer.append("variable");
    }

    // Special items
    if (FamiliarDatabase.dropById.contains(familiarId)) {
      buffer.append(sep);
      sep = ",";
      buffer.append("drop");
    }
    if (sep.equals("")) {
      buffer.append("none");
    }
    return buffer.toString();
  }

  public static final void setFamiliarImageLocation(final int familiarId, final String location) {
    FamiliarDatabase.familiarImageById.put(IntegerPool.get(familiarId), location);
  }

  public static final String getFamiliarImageLocation(final int familiarId) {
    String location = FamiliarDatabase.familiarImageById.get(IntegerPool.get(familiarId));
    return (location != null) ? location : "debug.gif";
  }

  public static final int getFamiliarByImageLocation(final String image) {
    Integer familiarId = FamiliarDatabase.familiarByImage.get(image);
    return familiarId == null ? -1 : familiarId.intValue();
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

  public static final Integer getFamiliarSkill(final String name, final int event) {
    return FamiliarDatabase.eventSkillByName[event - 1].get(name);
  }

  public static final int[] getFamiliarSkills(final int id) {
    return FamiliarDatabase.getFamiliarSkills(IntegerPool.get(id));
  }

  public static final int[] getFamiliarSkills(final Integer id) {
    String name = FamiliarDatabase.getFamiliarName(id);
    int[] skills = new int[4];
    for (int i = 0; i < 4; ++i) {
      skills[i] = FamiliarDatabase.eventSkillByName[i].get(name).intValue();
    }
    return skills;
  }

  public static final void setFamiliarSkills(final String name, final int[] skills) {
    for (int i = 0; i < 4; ++i) {
      FamiliarDatabase.eventSkillByName[i].put(name, IntegerPool.get(skills[i]));
    }
    FamiliarDatabase.newFamiliars = true;
    FamiliarDatabase.saveDataOverride();
  }

  public static final List<String> getFamiliarAttributes(final int familiarId) {
    return FamiliarDatabase.attributesById.get(familiarId);
  }

  public static final boolean hasAttribute(final String name, final String attribute) {
    int familiarId = FamiliarDatabase.getFamiliarId(name);
    if (familiarId == -1) {
      return false;
    }
    return FamiliarDatabase.hasAttribute(familiarId, attribute);
  }

  public static final boolean hasAttribute(final int familiarId, final String attribute) {
    List attrs = FamiliarDatabase.getFamiliarAttributes(familiarId);
    if (attrs == null) {
      return false;
    }
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
  public static final Set<Entry<Integer, String>> entrySet() {
    return FamiliarDatabase.familiarById.entrySet();
  }

  public static final void saveDataOverride() {
    FamiliarDatabase.writeFamiliars(new File(KoLConstants.DATA_LOCATION, "familiars.txt"));
    FamiliarDatabase.newFamiliars = false;
  }

  public static void writeFamiliars(final File output) {
    RequestLogger.printLine("Writing data override: " + output);
    PrintStream writer = LogStream.openStream(output, true);
    writer.println(KoLConstants.FAMILIARS_VERSION);

    writer.println("# Original familiar arena stats from Vladjimir's arena data");
    writer.println("# http://www.therye.org/familiars/");
    writer.println();
    writer.println("# no.	name	image	type	larva	item	CM	SH	OC	H&S");
    writer.println();

    Integer[] familiarIds = new Integer[FamiliarDatabase.familiarById.size()];
    FamiliarDatabase.familiarById.keySet().toArray(familiarIds);

    int lastInteger = 1;
    for (int i = 0; i < familiarIds.length; ++i) {
      Integer nextInteger = familiarIds[i];
      int familiarId = nextInteger.intValue();

      for (int j = lastInteger; j < familiarId; ++j) {
        writer.println(j);
      }

      lastInteger = familiarId + 1;

      String name = FamiliarDatabase.getFamiliarName(nextInteger);
      String image = FamiliarDatabase.getFamiliarImageLocation(familiarId);
      String type = FamiliarDatabase.getFamiliarType(familiarId);
      int larvaId = FamiliarDatabase.getFamiliarLarva(nextInteger);
      int itemId = FamiliarDatabase.getFamiliarItemId(nextInteger);
      int[] skills = FamiliarDatabase.getFamiliarSkills(nextInteger);

      FamiliarDatabase.writeFamiliar(
          writer, familiarId, name, image, type, larvaId, itemId, skills);
    }
  }

  public static void writeFamiliar(
      final PrintStream writer,
      final int familiarId,
      final String name,
      final String image,
      final String type,
      final int larvaId,
      final int itemId,
      final int[] skills) {
    writer.println(
        FamiliarDatabase.familiarString(familiarId, name, image, type, larvaId, itemId, skills));
  }

  public static String familiarString(
      final int familiarId,
      final String name,
      final String image,
      final String type,
      final int larvaId,
      final int itemId,
      final int[] skills) {
    String larva = larvaId == -1 ? "" : ItemDatabase.getItemDataName(larvaId);
    String item = itemId == -1 ? "" : ItemDatabase.getItemDataName(itemId);
    return familiarId
        + "\t"
        + name
        + "\t"
        + image
        + "\t"
        + type
        + "\t"
        + larva
        + "\t"
        + item
        + "\t"
        + skills[0]
        + "\t"
        + skills[1]
        + "\t"
        + skills[2]
        + "\t"
        + skills[3];
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
}
