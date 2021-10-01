package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.PastaThrallData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest.BuffTool;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LockableListFactory;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SkillDatabase {
  private static String[] canonicalNames = new String[0];
  private static final Map<Integer, String> nameById = new TreeMap<>();
  private static final Map<String, int[]> skillIdSetByName = new TreeMap<>();

  private static final Map<Integer, String> imageById = new TreeMap<>();
  private static final Map<Integer, Long> mpConsumptionById = new HashMap<>();
  private static final Map<Integer, Integer> skillTypeById = new TreeMap<>();
  private static final Map<Integer, Integer> durationById = new HashMap<>();
  private static final Map<Integer, Integer> levelById = new HashMap<>();

  private static final Map<String, List<String>> skillsByCategory = new HashMap<>();
  private static final Map<Integer, String> skillCategoryById = new HashMap<>();

  // Per-user data. Needs to be reset when log in as a new user.
  private static final Map<Integer, Integer> castsById = new HashMap<>();

  public static final int ALL = -2;
  public static final int CASTABLE = -1;
  public static final int PASSIVE = 0;
  public static final int SUMMON = 1;
  public static final int REMEDY = 2;
  public static final int SELF_ONLY = 3;
  public static final int BUFF = 4;
  public static final int COMBAT = 5;
  public static final int SONG = 6;
  public static final int COMBAT_NONCOMBAT_REMEDY = 7;
  public static final int COMBAT_PASSIVE = 8;
  public static final int EXPRESSION = 9;
  public static final int WALK = 10;

  public static final String skillTypeToTypeName(final int type) {
    return type == PASSIVE
        ? "passive"
        : type == SUMMON
            ? "summon"
            : type == REMEDY
                ? "remedy"
                : type == SELF_ONLY
                    ? "self-only"
                    : type == BUFF
                        ? "buff"
                        : type == COMBAT
                            ? "combat"
                            : type == SONG
                                ? "song"
                                : type == COMBAT_NONCOMBAT_REMEDY
                                    ? "combat/noncombat remedy"
                                    : type == COMBAT_PASSIVE
                                        ? "combat/passive"
                                        : type == EXPRESSION
                                            ? "expression"
                                            : type == WALK ? "walk" : "unknown";
  }

  public static final int skillTypeNameToType(final String typeName) {
    return typeName.equals("passive")
        ? SkillDatabase.PASSIVE
        : typeName.equals("summon")
            ? SkillDatabase.SUMMON
            : typeName.equals("remedy")
                ? SkillDatabase.REMEDY
                : typeName.equals("self-only")
                    ? SkillDatabase.SELF_ONLY
                    : typeName.equals("buff")
                        ? SkillDatabase.BUFF
                        : typeName.equals("combat")
                            ? SkillDatabase.COMBAT
                            : typeName.equals("song")
                                ? SkillDatabase.SONG
                                : typeName.equals("combat/noncombat remedy")
                                    ? SkillDatabase.COMBAT_NONCOMBAT_REMEDY
                                    : typeName.equals("combat/passive")
                                        ? SkillDatabase.COMBAT_PASSIVE
                                        : typeName.equals("expression")
                                            ? SkillDatabase.EXPRESSION
                                            : typeName.equals("walk") ? SkillDatabase.WALK : -1;
  }

  private static final String UNCATEGORIZED = "uncategorized";
  private static final String CONDITIONAL = "conditional";
  private static final String MR_SKILLS = "mr. skills";
  private static final String GNOME_SKILLS = "gnome trainer";
  private static final String BAD_MOON = "bad moon";
  private static final String AVATAR_OF_BORIS = "avatar of Boris";
  private static final String ZOMBIE_MASTER = "zombie master";
  private static final String AVATAR_OF_JARLSBERG = "Avatar of Jarlsberg";
  private static final String AVATAR_OF_SNEAKY_PETE = "Avatar of Sneaky Pete";
  private static final String HEAVY_RAINS = "Heavy Rains";
  private static final String ED = "Ed";
  private static final String COWPUNCHER = "Cow Puncher";
  private static final String BEANSLINGER = "Beanslinger";
  private static final String SNAKE_OILER = "Snake Oiler";
  private static final String SOURCE = "The Source";
  private static final String NUCLEAR_AUTUMN = "Nuclear Autumn";
  private static final String GELATINOUS_NOOB = "Gelatinous Noob";
  private static final String VAMPYRE = "Vampyre";
  private static final String PLUMBER = "Plumber";

  private static final String[] CATEGORIES =
      new String[] {
        SkillDatabase.UNCATEGORIZED,
        "seal clubber", // 1xxx
        "turtle tamer", // 2xxx
        "pastamancer", // 3xxx
        "sauceror", // 4xxx
        "disco bandit", // 5xxx
        "accordion thief", // 6xxx
        SkillDatabase.CONDITIONAL, // 7xxx
        SkillDatabase.MR_SKILLS, // 8xxx
        "9XXX", // 9xxx
        "10XXX", // 10xxx
        SkillDatabase.AVATAR_OF_BORIS, // 11xxx
        SkillDatabase.ZOMBIE_MASTER, // 12xxx
        "13XXX", // 13xxx
        SkillDatabase.AVATAR_OF_JARLSBERG, // 14xxx
        SkillDatabase.AVATAR_OF_SNEAKY_PETE, // 15xxx
        SkillDatabase.HEAVY_RAINS, // 16xxx
        SkillDatabase.ED, // 17xxx
        SkillDatabase.COWPUNCHER, // 18xxx
        SkillDatabase.BEANSLINGER, // 19xxx
        SkillDatabase.SNAKE_OILER, // 20xxx
        SkillDatabase.SOURCE, // 21xxx
        SkillDatabase.NUCLEAR_AUTUMN, // 22xxx
        SkillDatabase.GELATINOUS_NOOB, // 23xxx
        SkillDatabase.VAMPYRE, // 24xxx
        SkillDatabase.PLUMBER, // 25xxx
        // The following are convenience categories, not implied by skill id
        SkillDatabase.GNOME_SKILLS,
        SkillDatabase.BAD_MOON
      };

  static {
    SkillDatabase.reset();
  }

  public static void reset() {
    for (int i = 0; i < SkillDatabase.CATEGORIES.length; ++i) {
      String category = SkillDatabase.CATEGORIES[i];
      SkillDatabase.skillsByCategory.put(category, new ArrayList<>());
    }

    BufferedReader reader =
        FileUtilities.getVersionedReader("classskills.txt", KoLConstants.CLASSSKILLS_VERSION);
    String[] data;

    while ((data = FileUtilities.readData(reader)) != null) {
      if (data.length < 6) {
        continue;
      }

      Integer skillId = Integer.valueOf(data[0]);
      String name = data[1];
      String image = data[2];
      Integer type = Integer.valueOf(data[3]);
      Long mp = Long.valueOf(data[4]);
      Integer duration = Integer.valueOf(data[5]);
      Integer level = (data.length > 6) ? Integer.valueOf(data[6]) : null;
      SkillDatabase.addSkill(skillId, name, image, type, mp, duration, level);
    }

    try {
      reader.close();
    } catch (Exception e) {
      // This should not happen.  Therefore, print
      // a stack trace for debug purposes.

      StaticEntity.printStackTrace(e);
    }

    SkillDatabase.canonicalNames = new String[SkillDatabase.skillIdSetByName.size()];
    SkillDatabase.skillIdSetByName.keySet().toArray(SkillDatabase.canonicalNames);
  }

  public static void resetCasts() {
    SkillDatabase.castsById.clear();
  }

  private static void addIdToName(String canonicalName, int skillId) {
    int[] idSet = SkillDatabase.skillIdSetByName.get(canonicalName);
    int[] newSet;

    if (idSet == null) {
      newSet = new int[1];
    }
    // *** This assumes the array is sorted
    else if (Arrays.binarySearch(idSet, skillId) >= 0) {
      return;
    } else {
      newSet = Arrays.copyOf(idSet, idSet.length + 1);
    }

    newSet[newSet.length - 1] = skillId;
    // *** Make it so
    Arrays.sort(newSet);
    SkillDatabase.skillIdSetByName.put(canonicalName, newSet);
  }

  private static void addSkill(
      final Integer skillId,
      final String name,
      final String image,
      final Integer skillType,
      final Long mpConsumption,
      final Integer duration,
      final Integer level) {
    String canonicalName = StringUtilities.getCanonicalName(name);
    SkillDatabase.nameById.put(skillId, name);
    SkillDatabase.addIdToName(canonicalName, skillId);

    if (image != null) {
      SkillDatabase.imageById.put(skillId, image);
    }
    SkillDatabase.skillTypeById.put(skillId, skillType);

    SkillDatabase.mpConsumptionById.put(skillId, mpConsumption);
    SkillDatabase.durationById.put(skillId, duration);
    if (level != null) {
      SkillDatabase.levelById.put(skillId, level);
    }

    String category;
    int categoryId = skillId.intValue() / 1000;

    switch (skillId.intValue()) {
      case SkillPool.SMILE_OF_MR_A:
      case SkillPool.SNOWCONE:
      case SkillPool.STICKER:
      case SkillPool.SUGAR:
      case SkillPool.CLIP_ART:
      case SkillPool.RAD_LIB:
      case SkillPool.SMITHSNESS:
      case SkillPool.CANDY_HEART:
      case SkillPool.PARTY_FAVOR:
      case SkillPool.LOVE_SONG:
      case SkillPool.BRICKOS:
      case SkillPool.DICE:
      case SkillPool.RESOLUTIONS:
      case SkillPool.TAFFY:
      case SkillPool.HILARIOUS:
      case SkillPool.TASTEFUL:
      case SkillPool.CARDS:
      case SkillPool.GEEKY:
      case SkillPool.CONFISCATOR:
        category = SkillDatabase.MR_SKILLS;
        break;

      case SkillPool.OBSERVATIOGN:
      case SkillPool.GNEFARIOUS_PICKPOCKETING:
      case SkillPool.TORSO:
      case SkillPool.GNOMISH_HARDINESS:
      case SkillPool.COSMIC_UNDERSTANDING:
        category = SkillDatabase.GNOME_SKILLS;
        break;

      case SkillPool.LUST: // Lust
      case SkillPool.GLUTTONY: // Gluttony
      case SkillPool.GREED: // Greed
      case SkillPool.SLOTH: // Sloth
      case SkillPool.WRATH: // Wrath
      case SkillPool.ENVY: // Envy
      case SkillPool.PRIDE: // Pride
        category = SkillDatabase.BAD_MOON;
        break;

      case SkillPool.MUG_FOR_THE_AUDIENCE: // Pride
        category = SkillDatabase.AVATAR_OF_SNEAKY_PETE;
        break;

      default:

        // Moxious maneuver has a 7000 id, but
        // it's not gained by equipment.

        category = SkillDatabase.CATEGORIES[categoryId];
    }

    SkillDatabase.skillCategoryById.put(skillId, category);
    SkillDatabase.skillsByCategory.get(category).add(name);

    SkillDatabase.castsById.put(skillId, IntegerPool.get(0));
  }

  public static final List getSkillsByCategory(String category) {
    if (category == null) {
      return new ArrayList();
    }

    List categoryMatches = StringUtilities.getMatchingNames(SkillDatabase.CATEGORIES, category);

    if (categoryMatches.size() != 1) {
      return new ArrayList();
    }

    category = (String) categoryMatches.get(0);

    List skills = SkillDatabase.skillsByCategory.get(category);

    if (skills == null) {
      return new ArrayList();
    }

    return skills;
  }

  /**
   * Returns the name for an skill, given its Id.
   *
   * @param skillId The Id of the skill to lookup
   * @return The name of the corresponding skill
   */
  public static final String getSkillName(final int skillId) {
    return SkillDatabase.nameById.get(IntegerPool.get(skillId));
  }

  public static final String getSkillDataName(final String skillName) {
    if (skillName.startsWith("[")) {
      int ind = skillName.indexOf("]");
      if (ind > 0) {
        int skillId = StringUtilities.parseInt(skillName.substring(1, ind));
        return getSkillName(skillId);
      }
    }
    return skillName;
  }

  static final Set<Integer> idKeySet() {
    return SkillDatabase.nameById.keySet();
  }

  /**
   * Returns the Id number for an skill, given its name.
   *
   * @param skillName The name of the skill to lookup
   * @return The Id number of the corresponding skill
   */
  public static final int getSkillId(final String skillName) {
    return SkillDatabase.getSkillId(skillName, false);
  }

  public static final int getSkillId(final String skillName, final boolean exact) {
    if (skillName == null) {
      return -1;
    }

    // If name starts with [nnnn] then that is explicitly the skill id
    if (skillName.startsWith("[")) {
      int index = skillName.indexOf("]");
      if (index > 0) {
        String idString = skillName.substring(1, index);
        int skillId = -1;
        try {
          skillId = StringUtilities.parseInt(idString);
        } catch (NumberFormatException e) {
        }
        return skillId;
      }
    }

    int[] ids = SkillDatabase.skillIdSetByName.get(StringUtilities.getCanonicalName(skillName));

    if (ids != null) {
      if (exact && ids.length > 1) {
        return -1;
      }
      return ids[ids.length - 1];
    }

    if (exact) {
      return -1;
    }

    List<String> names = SkillDatabase.getMatchingNames(skillName);
    if (names.size() == 1) {
      return SkillDatabase.getSkillId(names.get(0), true);
    }

    return -1;
  }

  public static final int getSkillId(final String skillName, final int type) {
    if (skillName == null) {
      return -1;
    }

    int[] ids = SkillDatabase.skillIdSetByName.get(StringUtilities.getCanonicalName(skillName));

    if (ids == null) {
      return -1;
    }

    for (int skillId : ids) {
      if (SkillDatabase.isType(skillId, type)) {
        return skillId;
      }
    }

    return -1;
  }

  private static final int[] NO_SKILL_IDS = new int[0];

  public static final int[] getSkillIds(final String skillName, final boolean exact) {
    if (skillName == null) {
      return NO_SKILL_IDS;
    }

    // If name starts with [nnnn] then that is explicitly the effect id
    if (skillName.startsWith("[")) {
      int index = skillName.indexOf("]");
      if (index > 0) {
        String idString = skillName.substring(1, index);
        int skillId = -1;
        try {
          skillId = StringUtilities.parseInt(idString);
        } catch (NumberFormatException e) {
        }
        int[] ids = new int[1];
        ids[0] = skillId;
        return ids;
      }
    }

    int[] ids = SkillDatabase.skillIdSetByName.get(StringUtilities.getCanonicalName(skillName));

    if (ids != null) {
      if (exact && ids.length > 1) {
        return NO_SKILL_IDS;
      }
      return ids;
    }

    if (exact) {
      return NO_SKILL_IDS;
    }

    List<String> names = SkillDatabase.getMatchingNames(skillName);
    if (names.size() != 1) {
      return NO_SKILL_IDS;
    }

    ids = skillIdSetByName.get(StringUtilities.getCanonicalName(names.get(0)));

    return ids != null ? ids : NO_SKILL_IDS;
  }

  /** Returns a list of all skills which contain the given substring. */
  public static final List<String> getMatchingNames(final String substring) {
    // If name starts with [nnnn] then that is explicitly the skill id
    if (substring.startsWith("[")) {
      int index = substring.indexOf("]");
      if (index > 0) {
        String idString = substring.substring(1, index);
        try {
          int skillId = StringUtilities.parseInt(idString);
          // It parsed to a number so is valid
          List<String> list = new ArrayList<>();
          list.add(substring);
          return list;
        } catch (NumberFormatException e) {
        }
      }
    }
    return StringUtilities.getMatchingNames(SkillDatabase.canonicalNames, substring);
  }

  /**
   * Returns the level for an skill, given its Id.
   *
   * @param skillId The Id of the skill to lookup
   * @return The level of the corresponding skill
   */
  public static final int getSkillLevel(final int skillId) {
    Integer level = SkillDatabase.levelById.get(IntegerPool.get(skillId));
    return level == null ? -1 : level;
  }

  public static final int getSkillPurchaseCost(final int skillId) {
    if (!(1000 <= skillId && skillId < 7000)) {
      return 0;
    }

    switch (SkillDatabase.getSkillLevel(skillId)) {
      default:
        return 0;
      case 1:
        return 125;
      case 2:
        return 250;
      case 3:
        return 500;
      case 4:
        return 750;
      case 5:
        return 1250;
      case 6:
        return 1750;
      case 7:
        return 2500;
      case 8:
        return 3250;
      case 9:
        return 4000;
      case 10:
        return 5000;
      case 11:
        return 6250;
      case 12:
        return 7500;
      case 13:
        return 10000;
      case 14:
        return 12500;
      case 15:
        return 15000;
    }
  }

  public static final int classSkillsBase() {
    String name = KoLCharacter.getClassType();

    if (name.equals(KoLCharacter.SEAL_CLUBBER)) {
      return 1000;
    }
    if (name.equals(KoLCharacter.TURTLE_TAMER)) {
      return 2000;
    }
    if (name.equals(KoLCharacter.PASTAMANCER)) {
      return 3000;
    }
    if (name.equals(KoLCharacter.SAUCEROR)) {
      return 4000;
    }
    if (name.equals(KoLCharacter.DISCO_BANDIT)) {
      return 5000;
    }
    if (name.equals(KoLCharacter.ACCORDION_THIEF)) {
      return 6000;
    }
    if (name.equals(KoLCharacter.AVATAR_OF_BORIS)) {
      return 11000;
    }
    if (name.equals(KoLCharacter.ZOMBIE_MASTER)) {
      return 12000;
    }
    if (name.equals(KoLCharacter.AVATAR_OF_JARLSBERG)) {
      return 14000;
    }
    if (name.equals(KoLCharacter.AVATAR_OF_SNEAKY_PETE)) {
      return 15000;
    }
    if (name.equals(KoLCharacter.ED)) {
      return 17000;
    }
    if (name.equals(KoLCharacter.COWPUNCHER)) {
      return 18000;
    }
    if (name.equals(KoLCharacter.BEANSLINGER)) {
      return 19000;
    }
    if (name.equals(KoLCharacter.SNAKE_OILER)) {
      return 20000;
    }
    if (name.equals(KoLCharacter.GELATINOUS_NOOB)) {
      return 23000;
    }
    if (name.equals(KoLCharacter.VAMPYRE)) {
      return 24000;
    }
    if (name.equals(KoLCharacter.PLUMBER)) {
      return 25000;
    }

    return 0;
  }

  /**
   * Returns the type for an skill, given its Id.
   *
   * @param skillId The Id of the skill to lookup
   * @return The type of the corresponding skill
   */
  public static final int getSkillType(final int skillId) {
    Integer skillType = SkillDatabase.skillTypeById.get(IntegerPool.get(skillId));
    return skillType == null ? -1 : skillType.intValue();
  }

  public static final String getSkillTypeName(final int skillId) {
    Integer skillType = SkillDatabase.skillTypeById.get(IntegerPool.get(skillId));
    if (skillType == null) {
      return "unknown";
    }
    String typeName = SkillDatabase.skillTypeToTypeName(skillType.intValue());
    return typeName;
  }

  public static final String getSkillCategory(final int skillId) {
    String cat = SkillDatabase.skillCategoryById.get(IntegerPool.get(skillId));
    return cat == null ? "" : cat;
  }

  /**
   * Returns the image for an skill, given its Id.
   *
   * @param skillId The Id of the skill to lookup
   * @return The type of the corresponding skill
   */
  public static final String getSkillImage(final int skillId) {
    return SkillDatabase.imageById.get(IntegerPool.get(skillId));
  }

  private static final AdventureResult SUPER_SKILL = EffectPool.get(EffectPool.SUPER_SKILL);

  /**
   * Returns how much MP is consumed by using the skill with the given Id.
   *
   * @param skillId The id of the skill to lookup
   * @return The MP consumed by the skill, or 0 if unknown
   */
  public static final long getMPConsumptionById(final int skillId) {
    if (isLibramSkill(skillId)) {
      return libramSkillMPConsumption();
    }

    String classType = null;
    boolean thrallReduced = false;
    boolean isCombat =
        (SkillDatabase.isCombat(skillId) && !SkillDatabase.isNonCombat(skillId))
            || (SkillDatabase.isCombat(skillId) && FightRequest.getCurrentRound() > 0);
    boolean terminal = false;

    switch (skillId) {
      case SkillPool.CLOBBER:
        classType = KoLCharacter.SEAL_CLUBBER;
        break;
      case SkillPool.TOSS:
        classType = KoLCharacter.TURTLE_TAMER;
        break;
      case SkillPool.SPAGHETTI_SPEAR:
        classType = KoLCharacter.PASTAMANCER;
        break;
      case SkillPool.SALSABALL:
        classType = KoLCharacter.SAUCEROR;
        break;
      case SkillPool.SUCKERPUNCH:
        classType = KoLCharacter.DISCO_BANDIT;
        break;
      case SkillPool.SING:
        classType = KoLCharacter.ACCORDION_THIEF;
        break;
      case SkillPool.MILD_CURSE:
        classType = KoLCharacter.ED;
        break;

      case SkillPool.MAGIC_MISSILE:
        return Math.max(
            Math.min((KoLCharacter.getLevel() + 3) / 2, 6) + KoLCharacter.getManaCostAdjustment(),
            1);

      case SkillPool.STRINGOZZI:
      case SkillPool.RAVIOLI_SHURIKENS:
      case SkillPool.CANNELLONI_CANNON:
      case SkillPool.STUFFED_MORTAR_SHELL:
      case SkillPool.WEAPON_PASTALORD:
        if (KoLCharacter.currentPastaThrall() != PastaThrallData.NO_THRALL
            && KoLCharacter.hasSkill("Thrall Unit Tactics")) {
          thrallReduced = true;
        }
        break;

      case SkillPool.EXTRACT:
      case SkillPool.DIGITIZE:
      case SkillPool.COMPRESS:
      case SkillPool.DUPLICATE:
      case SkillPool.PORTSCAN:
      case SkillPool.TURBO:
        terminal = true;
        break;

      case SkillPool.STACK_LUMPS:
        return SkillDatabase.stackLumpsCost();

      case SkillPool.SEEK_OUT_A_BIRD:
        int birds = Preferences.getInteger("_birdsSoughtToday");
        return SkillDatabase.birdSkillMPConsumption(birds);
    }

    if (classType != null) {
      return KoLCharacter.getClassType().equals(classType)
          ? 0
          : Math.max(1 + KoLCharacter.getManaCostAdjustment(), 1);
    }

    if (SkillDatabase.getSkillType(skillId) == SkillDatabase.PASSIVE) {
      return 0;
    }

    if (isCombat && KoLConstants.activeEffects.contains(SkillDatabase.SUPER_SKILL)) {
      return 0;
    }

    Long mpConsumption = SkillDatabase.mpConsumptionById.get(IntegerPool.get(skillId));

    if (mpConsumption == null) {
      return 0;
    }

    int cost = mpConsumption.intValue();
    if (cost == 0) {
      return 0;
    }

    if (thrallReduced) {
      cost = cost / 2;
    }

    if (terminal) {
      cost -= Preferences.getInteger("sourceTerminalSpam");
      if (Preferences.getString("sourceTerminalChips").contains("ASHRAM")) {
        cost -= 5;
      }
    }

    int adjustment = KoLCharacter.getManaCostAdjustment(isCombat);
    return Math.max(cost + adjustment, 1);
  }

  public static final boolean hasVariableMpCost(final int skillId) {
    return SkillDatabase.isLibramSkill(skillId) || skillId == SkillPool.SEEK_OUT_A_BIRD;
  }

  /**
   * Determines if a skill comes from a Libram
   *
   * @param skillId The Id of the skill to lookup
   * @return true if it comes from a Libram
   */
  public static final boolean isLibramSkill(final int skillId) {
    return skillId == SkillPool.CANDY_HEART
        || skillId == SkillPool.PARTY_FAVOR
        || skillId == SkillPool.LOVE_SONG
        || skillId == SkillPool.BRICKOS
        || skillId == SkillPool.DICE
        || skillId == SkillPool.RESOLUTIONS
        || skillId == SkillPool.TAFFY;
  }

  /**
   * Determines the cost for next casting of a libram skill
   *
   * @return the MP cost to cast it
   */
  public static final long libramSkillMPConsumption() {
    int cast = Preferences.getInteger("libramSummons");
    return libramSkillMPConsumption(cast + 1);
  }

  public static final void setLibramSkillCasts(long cost) {
    // With sufficient mana cost reduction, the first, second, and
    // third libram summons all cost 1 MP. Therefore, we can't
    // necessarily tell how many times librams have been used today
    // by looking at the summoning cost.

    // Heuristic: if the mana cost shown by the bookcase agrees
    // with our current calculated mana cost, assume we have it
    // right. Otherwise, assume that summons have been made outside
    // of KoLmafia and back-calculate from the bookshelf's cost.

    // Get KoLmafia's idea of number of casts
    int casts = Preferences.getInteger("libramSummons");

    // If the next cast costs what the bookshelf says it costs,
    // assume we're correct.
    if (libramSkillMPConsumption(casts + 1) == cost) {
      return;
    }

    // Otherwise, derive number of casts from unadjusted mana cost
    // Make sure we have updated modifiers - otherwise, the initial
    // cost setting done at login may ignore our MP cost adjustments.
    KoLCharacter.recalculateAdjustments();
    cost -= KoLCharacter.getManaCostAdjustment();

    // cost = 1 + (n * (n-1) / 2)
    //
    // n^2 - n + (2 - 2cost) = 0
    //
    // Use the quadratic formula
    //
    //    a = 1, b = -1, c = 2-2*cost
    //
    // x = ( 1 + sqrt(8*cost - 7))/2

    int count = (1 + (int) Math.sqrt(8 * cost - 7)) / 2;

    Preferences.setInteger("libramSummons", count - 1);
    LockableListFactory.sort(KoLConstants.summoningSkills);
    LockableListFactory.sort(KoLConstants.usableSkills);
  }

  /**
   * Determines the cost for a specific casting of a libram skill
   *
   * @param cast which casting
   * @return the MP cost to cast it
   */
  public static final long libramSkillMPConsumption(final int cast) {
    // Old formula: n * (n+1) / 2
    // return Math.max( (cast * ( cast + 1 ) / 2 + KoLCharacter.getManaCostAdjustment(), 1 );

    // New formula: 1 + (n * (n-1) / 2)
    return Math.max(1 + cast * (cast - 1) / 2 + KoLCharacter.getManaCostAdjustment(), 1);
  }

  /**
   * Determines the cost for casting a libram skill multiple times
   *
   * @param cast which casting
   * @param count how many casts
   * @return the MP cost to cast it
   */
  public static final long libramSkillMPConsumption(int cast, int count) {
    long total = 0;
    while (count-- > 0) {
      total += libramSkillMPConsumption(cast++);
    }
    return total;
  }

  /**
   * Determines how many times you can cast libram skills with the specified amount of MP
   *
   * @param availableMP how much MP is available
   * @return the number of casts
   */
  public static final long libramSkillCasts(long availableMP) {
    int cast = Preferences.getInteger("libramSummons");
    return Math.min(200, libramSkillCasts(cast + 1, availableMP));
  }

  /**
   * Determines how many times you can cast libram skills with the specified amount of MP starting
   * with specified casting
   *
   * @param cast which casting
   * @param availableMP how much MP is available
   * @return the number of casts
   */
  public static final int libramSkillCasts(int cast, long availableMP) {
    long mpCost = SkillDatabase.libramSkillMPConsumption(cast);
    int count = 0;

    while (mpCost <= availableMP) {
      count++;
      availableMP -= mpCost;
      mpCost = SkillDatabase.libramSkillMPConsumption(++cast);
    }

    return count;
  }

  public static final long birdSkillMPConsumption(final int cast) {
    // Casting cost: 5, 10, 20, 40, 80, 160, 320, ...
    long mp = 5 * (long) Math.pow(2.0, cast);
    return Math.max(mp + KoLCharacter.getManaCostAdjustment(), 1);
  }

  public static final int birdSkillCasts(int cast, long availableMP) {
    long mpCost = SkillDatabase.birdSkillMPConsumption(cast);
    int count = 0;

    while (mpCost <= availableMP) {
      count++;
      availableMP -= mpCost;
      mpCost = SkillDatabase.birdSkillMPConsumption(cast);
    }

    return count;
  }

  public static final long birdSkillCasts(long availableMP) {
    int birds = Preferences.getInteger("_birdsSoughtToday");
    return SkillDatabase.birdSkillCasts(birds, availableMP);
  }

  public static final long stackLumpsCost() {
    long mpCost = 1;
    int casts = Preferences.getInteger("_stackLumpsUses");
    if (casts < 0) return mpCost;
    for (int i = 0; i <= casts; i++) {
      mpCost += 10 * Math.pow(10, i);
    }

    return mpCost;
  }

  /**
   * Returns how many rounds of buff are gained by using the skill with the given Id.
   *
   * @param skillId The id of the skill to lookup
   * @return The duration of effect the cast gives
   */
  public static final int getEffectDuration(final int skillId) {
    Integer duration = SkillDatabase.durationById.get(IntegerPool.get(skillId));
    if (duration == null) {
      return 0;
    }

    int actualDuration = duration.intValue();
    if (actualDuration == 0) {
      return 0;
    }

    int type = SkillDatabase.getSkillType(skillId);

    if (type == SkillDatabase.SONG) {
      int multiplier = KoLCharacter.hasSkill(SkillPool.GOOD_SINGING_VOICE) ? 2 : 1;
      return actualDuration * multiplier;
    }

    if (type != SkillDatabase.BUFF) {
      switch (skillId) {
        case SkillPool.SPIRIT_BOON:
          return KoLCharacter.getBlessingLevel() * 5;

        case SkillPool.WAR_BLESSING:
        case SkillPool.SHE_WHO_WAS_BLESSING:
        case SkillPool.STORM_BLESSING:
          if (!KoLCharacter.getClassType().equals(KoLCharacter.TURTLE_TAMER)) {
            return 10;
          }
          break;

        case SkillPool.BIND_VAMPIEROGHI:
        case SkillPool.BIND_VERMINCELLI:
        case SkillPool.BIND_ANGEL_HAIR_WISP:
        case SkillPool.BIND_UNDEAD_ELBOW_MACARONI:
        case SkillPool.BIND_PENNE_DREADFUL:
        case SkillPool.BIND_LASAGMBIE:
        case SkillPool.BIND_SPICE_GHOST:
          if (!KoLCharacter.getClassType().equals(KoLCharacter.PASTAMANCER)) {
            return 10;
          }
          break;

        case SkillPool.REV_ENGINE:
          return Math.max(Math.abs(KoLCharacter.getAudience()), 5);

        case SkillPool.BIKER_SWAGGER:
          return Math.max(Math.abs(KoLCharacter.getAudience()), 10);
      }

      return actualDuration;
    }

    if (KoLConstants.inventory.contains(UseSkillRequest.WIZARD_HAT)
        || KoLCharacter.hasEquipped(UseSkillRequest.WIZARD_HAT, EquipmentManager.HAT)) {
      actualDuration += 5;
    }

    BuffTool[] tools =
        (SkillDatabase.isTurtleTamerBuff(skillId))
            ? UseSkillRequest.TAMER_TOOLS
            : (SkillDatabase.isSaucerorBuff(skillId))
                ? UseSkillRequest.SAUCE_TOOLS
                : (SkillDatabase.isAccordionThiefSong(skillId))
                    ? UseSkillRequest.THIEF_TOOLS
                    : null;

    if (tools == null) {
      return actualDuration;
    }

    int inventoryDuration = 0;

    for (BuffTool tool : tools) {
      int current = actualDuration + tool.getBonusTurns();

      if (current <= inventoryDuration) {
        continue;
      }

      if ((tool.hasEquipped() || KoLConstants.inventory.contains(tool.getItem()))
          && (!tool.isClassLimited() || KoLCharacter.getClassType().equals(tool.getClassType()))) {
        inventoryDuration = current;
      }
    }

    return inventoryDuration;
  }

  /**
   * Returns whether or not this is a normal skill that can only be used on the player.
   *
   * @return <code>true</code> if the skill is a normal skill
   */
  public static final boolean isNormal(final int skillId) {
    Integer skillType = SkillDatabase.skillTypeById.get(IntegerPool.get(skillId));
    if (skillType == null) return false;
    int type = skillType.intValue();
    return type == SUMMON
        || type == REMEDY
        || type == SELF_ONLY
        || type == SONG
        || type == COMBAT_NONCOMBAT_REMEDY
        || type == EXPRESSION;
  }

  /**
   * Returns whether or not the skill is a passive.
   *
   * @return <code>true</code> if the skill is passive
   */
  public static final boolean isPassive(final int skillId) {
    // Shake it off is a passive as well as a non-combat heal
    // Vampyre skills all have a passive (-hp) effect
    return SkillDatabase.isType(skillId, SkillDatabase.PASSIVE)
        || SkillDatabase.isType(skillId, SkillDatabase.COMBAT_PASSIVE)
        || SkillDatabase.isVampyreSkill(skillId);
  }

  /**
   * Returns whether or not the skill is a buff (ie: can be used on others).
   *
   * @return <code>true</code> if the skill can target other players
   */
  public static final boolean isBuff(final int skillId) {
    return SkillDatabase.isType(skillId, SkillDatabase.BUFF);
  }

  public static final boolean isTurtleTamerBuff(final int skillId) {
    return (skillId > 2000 && skillId < 3000 && SkillDatabase.isBuff(skillId));
  }

  public static final boolean isSaucerorBuff(final int skillId) {
    return (skillId > 4000 && skillId < 5000 && SkillDatabase.isBuff(skillId));
  }

  public static final boolean isAccordionThiefSong(final int skillId) {
    return (skillId > 6000 && skillId < 7000 && SkillDatabase.isBuff(skillId));
  }

  /**
   * Returns whether or not the skill is a combat skill (ie: can be used while fighting).
   *
   * @return <code>true</code> if the skill can be used in combat
   */
  public static final boolean isCombat(final int skillId) {
    return SkillDatabase.isType(skillId, SkillDatabase.COMBAT)
        || SkillDatabase.isType(skillId, SkillDatabase.COMBAT_NONCOMBAT_REMEDY)
        || SkillDatabase.isType(skillId, SkillDatabase.COMBAT_PASSIVE);
  }

  /**
   * Returns whether or not the skill is a non combat skill (ie: can be used while not fighting).
   *
   * @return <code>true</code> if the skill can be used out of combat
   */
  public static final boolean isNonCombat(final int skillId) {
    return !SkillDatabase.isType(skillId, SkillDatabase.COMBAT)
        && !SkillDatabase.isType(skillId, SkillDatabase.COMBAT_PASSIVE);
  }

  /**
   * Returns whether or not the skill is a song
   *
   * @return <code>true</code> if the skill is a song
   */
  public static final boolean isSong(final int skillId) {
    return SkillDatabase.isType(skillId, SkillDatabase.SONG);
  }

  /**
   * Returns whether or not the skill is an expression
   *
   * @return <code>true</code> if the skill is an expression
   */
  public static final boolean isExpression(final int skillId) {
    return SkillDatabase.isType(skillId, SkillDatabase.EXPRESSION);
  }

  /**
   * Returns whether or not the skill is a walk
   *
   * @return <code>true</code> if the skill is a walk
   */
  public static final boolean isWalk(final int skillId) {
    return SkillDatabase.isType(skillId, SkillDatabase.WALK);
  }

  /**
   * Returns whether or not the skill is a summon
   *
   * @return <code>true</code> if the skill is a summon
   */
  public static final boolean isSummon(final int skillId) {
    return SkillDatabase.isType(skillId, SkillDatabase.SUMMON);
  }

  /** Utility method used to determine if the given skill is of the appropriate type. */
  private static boolean isType(final int skillId, final int type) {
    Integer skillType = SkillDatabase.skillTypeById.get(IntegerPool.get(skillId));
    return skillType != null && skillType.intValue() == type;
  }

  public static final boolean isSoulsauceSkill(final int skillId) {
    return SkillDatabase.getSoulsauceCost(skillId) > 0;
  }

  public static final int getSoulsauceCost(final int skillId) {
    switch (skillId) {
      case SkillPool.SOUL_BUBBLE:
      case SkillPool.SOUL_FOOD:
        return 5;
      case SkillPool.SOUL_FINGER:
        return 40;
      case SkillPool.SOUL_BLAZE:
        return 100;
      case SkillPool.SOUL_ROTATION:
        return 25;
      case SkillPool.SOUL_FUNK:
        return 50;
      default:
        return 0;
    }
  }

  public static final boolean isThunderSkill(final int skillId) {
    return SkillDatabase.getThunderCost(skillId) > 0;
  }

  public static final int getThunderCost(final int skillId) {
    switch (skillId) {
      case SkillPool.THUNDER_CLAP:
        return 40;
      case SkillPool.THUNDERCLOUD:
      case SkillPool.THUNDERHEART:
        return 20;
      case SkillPool.THUNDER_BIRD:
        return 1;
      case SkillPool.THUNDERSTRIKE:
        return 5;
      case SkillPool.THUNDER_DOWN_UNDERWEAR:
        return 60;
      default:
        return 0;
    }
  }

  public static final boolean isRainSkill(final int skillId) {
    return SkillDatabase.getRainCost(skillId) > 0;
  }

  public static final int getRainCost(final int skillId) {
    switch (skillId) {
      case SkillPool.RAIN_MAN:
        return 50;
      case SkillPool.RAINY_DAY:
        return 20;
      case SkillPool.MAKE_IT_RAIN:
      case SkillPool.RAIN_DANCE:
        return 10;
      case SkillPool.RAINBOW:
        return 3;
      case SkillPool.RAINCOAT:
        return 40;
      default:
        return 0;
    }
  }

  public static final boolean isLightningSkill(final int skillId) {
    return SkillDatabase.getLightningCost(skillId) > 0;
  }

  public static final int getLightningCost(final int skillId) {
    switch (skillId) {
      case SkillPool.LIGHTNING_STRIKE:
      case SkillPool.LIGHTNING_ROD:
        return 20;
      case SkillPool.CLEAN_HAIR_LIGHTNING:
      case SkillPool.SHEET_LIGHTNING:
        return 10;
      case SkillPool.BALL_LIGHTNING:
        return 5;
      case SkillPool.LIGHTNING_BOLT_RAIN:
        return 1;
      default:
        return 0;
    }
  }

  public static final boolean isAsdonMartinSkill(final int skillId) {
    return SkillDatabase.getFuelCost(skillId) > 0;
  }

  public static final int getFuelCost(final int skillId) {
    switch (skillId) {
      case SkillPool.AM_MISSILE_LAUNCHER:
        return 100;
      case SkillPool.AM_BEAN_BAG_CANNON:
        return 10;
      case SkillPool.AM_FRONT_BUMPER:
        return 50;
      default:
        return 0;
    }
  }

  public static final boolean isVampyreSkill(final int skillId) {
    return SkillDatabase.getSkillCategory(skillId).equals(SkillDatabase.VAMPYRE);
  }

  public static final int getHPCost(final int skillId) {
    switch (skillId) {
      case SkillPool.BLOOD_SPIKE:
      case SkillPool.PIERCING_GAZE:
      case SkillPool.SAVAGE_BITE:
        return 3;
      case SkillPool.BLOOD_CHAINS:
        return 5;
      case SkillPool.CHILL_OF_THE_TOMB:
        return 7;
      case SkillPool.BLOOD_CLOAK:
      case SkillPool.CEASELESS_SNARL:
      case SkillPool.CRUSH:
      case SkillPool.FLOCK_OF_BATS_FORM:
      case SkillPool.MIST_FORM:
      case SkillPool.SPECTRAL_AWARENESS:
      case SkillPool.WOLF_FORM:
      case SkillPool.BLOOD_BUCATINI:
        return 10;
      case SkillPool.PERCEIVE_SOUL:
        return 15;
      case SkillPool.BALEFUL_HOWL:
      case SkillPool.ENSORCEL:
        return 30;

        // Vampyre Book Skills
      case SkillPool.BLOOD_FRENZY:
      case SkillPool.BLOOD_BOND:
      case SkillPool.BLOOD_BUBBLE:
        return 30;
      case SkillPool.BLOOD_bLADE:
      case SkillPool.BRAMS_BLOODY_BAGATELLE:
        return 50;

      default:
        return 0;
    }
  }

  public static final int getPPCost(final int skillId) {
    switch (skillId) {
      case SkillPool.HAMMER_THROW_COMBAT:
      case SkillPool.JUGGLE_FIREBALLS_COMBAT:
      case SkillPool.SPIN_JUMP_COMBAT:
        return 1;

      case SkillPool.ULTRA_SMASH_COMBAT:
      case SkillPool.FIREBALL_BARRAGE_COMBAT:
      case SkillPool.MULTI_BOUNCE_COMBAT:
        return 2;

      default:
        return 0;
    }
  }

  public static final AdventureResult getManaItemCost(final int skillId) {
    switch (skillId) {
      case SkillPool.DARK_RITUAL:
        return ItemPool.get(ItemPool.BLACK_MANA, 1);
      case SkillPool.ANCESTRAL_RECALL:
        return ItemPool.get(ItemPool.BLUE_MANA, 1);
      case SkillPool.GIANT_GROWTH:
        return ItemPool.get(ItemPool.GREEN_MANA, 1);
      case SkillPool.LIGHTNING_BOLT_CARD:
        return ItemPool.get(ItemPool.RED_MANA, 1);
      case SkillPool.HEALING_SALVE:
        return ItemPool.get(ItemPool.WHITE_MANA, 1);
      default:
        return null;
    }
  }

  public static final int getBlackManaCost(final int skillId) {
    return skillId == SkillPool.DARK_RITUAL ? 1 : 0;
  }

  public static final int getBlueManaCost(final int skillId) {
    return skillId == SkillPool.ANCESTRAL_RECALL ? 1 : 0;
  }

  public static final int getGreenManaCost(final int skillId) {
    return skillId == SkillPool.GIANT_GROWTH ? 1 : 0;
  }

  public static final int getRedManaCost(final int skillId) {
    return skillId == SkillPool.LIGHTNING_BOLT_CARD ? 1 : 0;
  }

  public static final int getWhiteManaCost(final int skillId) {
    return skillId == SkillPool.HEALING_SALVE ? 1 : 0;
  }

  public static final int getAdventureCost(final int skillId) {
    switch (skillId) {
      case SkillPool.HIBERNATE:
      case SkillPool.SPIRIT_VACATION:
      case SkillPool.TRANSCENDENTAL_DENTE:
      case SkillPool.SIMMER:
      case SkillPool.RECRUIT_ZOMBIE:
      case SkillPool.CHECK_MIRROR:
      case SkillPool.RAIN_MAN:
      case SkillPool.EVOKE_ELDRITCH_HORROR:
        return 1;
      default:
        return 0;
    }
  }

  /** Utility method used to determine if the given skill can be made permanent */
  public static final boolean isPermable(final int skillId) {
    switch (skillId) {
      case SkillPool.OLD_OLD_SMILE:
      case SkillPool.SMILE_OF_MR_A:
      case SkillPool.ARSE_SHOOT:
        // Item granted skills
        return false;

      case SkillPool.STEEL_LIVER:
      case SkillPool.STEEL_STOMACH:
      case SkillPool.STEEL_SPLEEN:
        // Steel Organs
        return false;

      case SkillPool.LUST: // Lust
      case SkillPool.GLUTTONY: // Gluttony
      case SkillPool.GREED: // Greed
      case SkillPool.SLOTH: // Sloth
      case SkillPool.WRATH: // Wrath
      case SkillPool.ENVY: // Envy
      case SkillPool.PRIDE: // Pride
        // Bad Moon skills
        return false;

      case SkillPool.DOG_TIRED:
      case SkillPool.HOLLOW_LEG:
        // VIP lounge skills
        return false;

      case SkillPool.GOTHY_HANDWAVE:
      case SkillPool.BREAK_IT_ON_DOWN:
      case SkillPool.POP_AND_LOCK:
      case SkillPool.RUN_LIKE_THE_WIND:
      case SkillPool.CARBOLOADING:
        // Nemesis skills
        return false;

      case SkillPool.MIYAGI_MASSAGE:
      case SkillPool.SALAMANDER_KATA:
      case SkillPool.FLYING_FIRE_FIST:
      case SkillPool.STINKPALM:
      case SkillPool.SEVEN_FINGER_STRIKE:
      case SkillPool.KNUCKLE_SANDWICH:
      case SkillPool.CHILLED_MONKEY_BRAIN:
      case SkillPool.DRUNKEN_BABY_STYLE:
      case SkillPool.WORLDPUNCH:
      case SkillPool.ZENDO_KOBUSHI_KANCHO:
        // Way of the Surprising Fist skills
        return false;

      case SkillPool.OLFACTION:
      case SkillPool.THICK_SKINNED:
      case SkillPool.CHIP_ON_YOUR_SHOULDER:
      case SkillPool.REQUEST_SANDWICH:
      case SkillPool.PIRATE_BELLOW:
      case SkillPool.INCREDIBLE_SELF_ESTEEM:
      case SkillPool.GET_BIG:
      case SkillPool.MATING_CALL:
      case SkillPool.INSCRUTABLE_GAZE:
      case SkillPool.LOVE_MIXOLOGY:
      case SkillPool.ACQUIRE_RHINESTONES:
      case SkillPool.POP_SONG:
      case SkillPool.BUDGET_CONSCIOUS:
      case SkillPool.DRINKING_TO_DRINK:
      case SkillPool.CAROL_OF_THE_BULLS:
      case SkillPool.CAROL_OF_THE_HELLS:
      case SkillPool.CAROL_OF_THE_THRILLS:
        // Auto-HP-Permed
        return false;

      case SkillPool.SPIRIT_CAYENNE:
      case SkillPool.SPIRIT_PEPPERMINT:
      case SkillPool.SPIRIT_GARLIC:
      case SkillPool.SPIRIT_WORMWOOD:
      case SkillPool.SPIRIT_BACON:
      case SkillPool.SPIRIT_NOTHING:
        // Derived skills
        return false;

      case SkillPool.GEMELLIS_MARCH_OF_TESTERY:
        // Skills players can't get
        return false;

      case SkillPool.MILD_CURSE:
        // Other skills from this class are not permable
        return true;

      case SkillPool.SHOOT:
        // Avatar of West of Loathing skills
        return false;
    }

    switch (skillId / 1000) {
      case 7: // Skills granted by items
      case 8: // Mystical Bookshelf Skills
      case 11: // Avatar of Boris skills
      case 12: // Zombie Slayer skills
      case 14: // Avatar of Jarlsberg skills
      case 15: // Avatar of Sneaky Pete skills
      case 16: // Heavy Rains skills
      case 17: // Ed skills
      case 18: // Cow Puncher skills
      case 19: // Bean Slinger skills
      case 20: // Snake Oiler skills
      case 21: // The Source skills
      case 22: // Nuclear Autumn skills
      case 23: // Gelatinous Noob skills
      case 24: // Vampyre skills
      case 25: // Plumber skills
        return false;
    }

    return true;
  }

  public static final boolean isBookshelfSkill(final int skillId) {
    return skillId >= SkillPool.SNOWCONE && skillId <= SkillPool.CONFISCATOR;
  }

  public static final boolean isBookshelfSkill(final String skillName) {
    return isBookshelfSkill(SkillDatabase.getSkillId(skillName));
  }

  public static final int skillToBook(final String skillName) {
    switch (SkillDatabase.getSkillId(skillName)) {
      case SkillPool.SNOWCONE:
        return ItemPool.SNOWCONE_BOOK;
      case SkillPool.STICKER:
        return ItemPool.STICKER_BOOK;
      case SkillPool.SUGAR:
        return ItemPool.SUGAR_BOOK;
      case SkillPool.CLIP_ART:
        return ItemPool.CLIP_ART_BOOK;
      case SkillPool.RAD_LIB:
        return ItemPool.RAD_LIB_BOOK;
      case SkillPool.SMITHSNESS:
        return ItemPool.SMITH_BOOK;
      case SkillPool.CANDY_HEART:
        return ItemPool.CANDY_BOOK;
      case SkillPool.PARTY_FAVOR:
        return ItemPool.DIVINE_BOOK;
      case SkillPool.LOVE_SONG:
        return ItemPool.LOVE_BOOK;
      case SkillPool.BRICKOS:
        return ItemPool.BRICKO_BOOK;
      case SkillPool.DICE:
        return ItemPool.DICE_BOOK;
      case SkillPool.RESOLUTIONS:
        return ItemPool.RESOLUTION_BOOK;
      case SkillPool.TAFFY:
        return ItemPool.TAFFY_BOOK;
      case SkillPool.HILARIOUS:
        return ItemPool.HILARIOUS_BOOK;
      case SkillPool.TASTEFUL:
        return ItemPool.TASTEFUL_BOOK;
      case SkillPool.CARDS:
        return ItemPool.CARD_GAME_BOOK;
      case SkillPool.GEEKY:
        return ItemPool.GEEKY_BOOK;
      case SkillPool.CONFISCATOR:
        return ItemPool.CONFISCATOR_BOOK;
    }

    return -1;
  }

  /** Returns all skills in the database of the given type. */
  public static final List<UseSkillRequest> getSkillsByType(final int type) {
    return SkillDatabase.getSkillsByType(type, false);
  }

  public static final List<UseSkillRequest> getSkillsByType(
      final int type, final boolean onlyKnown) {
    Integer[] keys = new Integer[SkillDatabase.skillTypeById.size()];
    SkillDatabase.skillTypeById.keySet().toArray(keys);

    ArrayList<UseSkillRequest> list = new ArrayList<>();

    for (Integer skillId : keys) {
      Integer value = SkillDatabase.skillTypeById.get(skillId);
      if (value == null) continue;

      int skillType = value.intValue();

      boolean shouldAdd;
      if (type == SkillDatabase.ALL) {
        shouldAdd = true;
      } else if (type == SkillDatabase.CASTABLE) {
        shouldAdd =
            skillType == SUMMON
                || skillType == REMEDY
                || skillType == SELF_ONLY
                || skillType == BUFF
                || skillType == SONG
                || skillType == COMBAT_NONCOMBAT_REMEDY
                || skillType == EXPRESSION
                || skillType == WALK;
      } else if (type == SkillDatabase.COMBAT) {
        shouldAdd =
            skillType == COMBAT
                || skillType == COMBAT_NONCOMBAT_REMEDY
                || skillType == COMBAT_PASSIVE;
      } else if (type == SkillDatabase.REMEDY) {
        shouldAdd = skillType == REMEDY || skillType == COMBAT_NONCOMBAT_REMEDY;
      } else if (type == SkillDatabase.PASSIVE) {
        shouldAdd = skillType == PASSIVE || skillType == COMBAT_PASSIVE;
      } else {
        shouldAdd = skillType == type;
      }

      if (!shouldAdd || onlyKnown && !KoLCharacter.hasSkill(skillId)) {
        continue;
      }

      list.add(UseSkillRequest.getUnmodifiedInstance(skillId));
    }

    return list;
  }

  /**
   * Returns whether or not an item with a given name exists in the database; this is useful in the
   * event that an item is encountered which is not tradeable (and hence, should not be displayed).
   *
   * @return <code>true</code> if the item is in the database
   */
  public static final boolean contains(final String skillName) {
    if (skillName == null) {
      return false;
    }

    return Arrays.binarySearch(
            SkillDatabase.canonicalNames, StringUtilities.getCanonicalName(skillName))
        >= 0;
  }

  /**
   * Returns the set of skills keyed by name
   *
   * @return The set of skills keyed by name
   */
  public static final Set<Entry<Integer, String>> entrySet() {
    return SkillDatabase.nameById.entrySet();
  }

  private static final ArrayList<String> skillNames = new ArrayList<>();

  public static final void generateSkillList(final StringBuffer buffer, final boolean appendHTML) {
    ArrayList<String>[] categories = new ArrayList[SkillDatabase.CATEGORIES.length];

    if (SkillDatabase.skillNames.isEmpty()) {
      SkillDatabase.skillNames.addAll(SkillDatabase.skillIdSetByName.keySet());
    }

    for (int i = 0; i < categories.length; ++i) {
      categories[i] = new ArrayList<>();
      categories[i].addAll(SkillDatabase.skillsByCategory.get(SkillDatabase.CATEGORIES[i]));

      for (int j = 0; j < categories[i].size(); ++j) {
        if (!KoLConstants.availableSkills.contains(
            UseSkillRequest.getUnmodifiedInstance(categories[i].get(j)))) {
          categories[i].remove(j--);
        }
      }
    }

    boolean printedList = false;

    for (int i = 0; i < categories.length; ++i) {
      if (categories[i].isEmpty()) {
        continue;
      }

      if (printedList) {
        if (appendHTML) {
          buffer.append("<br>");
        } else {
          buffer.append(KoLConstants.LINE_BREAK);
        }
      }

      SkillDatabase.appendSkillList(
          buffer,
          appendHTML,
          StringUtilities.toTitleCase(SkillDatabase.CATEGORIES[i]),
          categories[i]);
      printedList = true;
    }
  }

  private static void appendSkillList(
      final StringBuffer buffer,
      final boolean appendHTML,
      final String listName,
      final ArrayList<String> list) {
    if (list.isEmpty()) {
      return;
    }

    Collections.sort(list);

    if (appendHTML) {
      buffer.append("<u><b>");
    }

    buffer.append(StringUtilities.toTitleCase(listName));

    if (appendHTML) {
      buffer.append("</b></u><br>");
    } else {
      buffer.append(KoLConstants.LINE_BREAK);
    }

    String currentSkill;

    for (String s : list) {
      currentSkill = s;

      if (appendHTML) {
        buffer.append("<a onClick=\"javascript:skill(");
        buffer.append(SkillDatabase.getSkillId(currentSkill));
        buffer.append(");\">");
      } else {
        buffer.append(" - ");
      }

      buffer.append(currentSkill);

      if (appendHTML) {
        buffer.append("</a><br>");
      } else {
        buffer.append(KoLConstants.LINE_BREAK);
      }
    }
  }

  /**
   * Utility method used to retrieve the full name of a skill, given a substring representing it.
   */
  public static final String getSkillName(
      final String substring, final List<UseSkillRequest> list) {
    UseSkillRequest match = getSkill(substring, list);
    return match == null ? null : match.getSkillName();
  }

  /** Utility method used to retrieve a UseSkillRequest, given a substring of its name */
  public static final UseSkillRequest getSkill(
      final String substring, final List<UseSkillRequest> skills) {
    String canonical = StringUtilities.getCanonicalName(substring);

    // Search for exact match
    for (UseSkillRequest skill : skills) {
      if (skill.getCanonical().equals(canonical)) {
        return skill;
      }
    }

    // Search for case insensitive substring match
    UseSkillRequest match = null;
    boolean ambiguous = false;
    for (UseSkillRequest skill : skills) {
      if (skill.getCanonical().contains(canonical)) {
        String skillName = skill.getSkillName();
        if (ambiguous) {
          RequestLogger.printLine(skillName);
        } else if (match != null) {
          RequestLogger.printLine("Possible matches:");
          RequestLogger.printLine(match.getSkillName());
          RequestLogger.printLine(skillName);
          ambiguous = true;
        } else {
          match = skill;
        }
      }
    }

    return (ambiguous || match == null) ? null : match;
  }

  /**
   * Utility method used to retrieve the full name of a skill, given a substring representing it.
   */
  public static final String getSkillName(final String substring) {
    return getSkillName(substring, getSkillsByType(ALL));
  }

  /**
   * Utility method used to retrieve the full name of a castable skill, given a substring
   * representing it.
   */
  public static final String getUsableSkillName(final String substring) {
    return getSkillName(substring, getSkillsByType(CASTABLE));
  }

  /**
   * Utility method used to retrieve the full name of a known castable skill, given a substring
   * representing it.
   */
  public static final String getUsableKnownSkillName(final String substring) {
    return getSkillName(substring, getSkillsByType(CASTABLE, true));
  }

  /**
   * Utility method used to retrieve the full name of a combat skill, given a substring representing
   * it.
   */
  public static final UseSkillRequest getCombatSkill(final String substring) {
    return getSkill(substring, getSkillsByType(COMBAT));
  }

  /** Utility method used to retrieve the maximum daily casts of a skill. Returns -1 if no limit. */
  public static long getMaxCasts(int skillId) {
    UseSkillRequest skill = UseSkillRequest.getUnmodifiedInstance(skillId);
    if (skill == null) {
      return -1;
    }
    long max = skill.getMaximumCast();
    return (max == Long.MAX_VALUE ? -1 : max);
  }

  /** Method that is called when we need to update the number of casts for a given skill. */
  public static void registerCasts(int skillId, int count) {
    Integer oldCasts = SkillDatabase.castsById.get(IntegerPool.get(skillId));
    if (oldCasts == null) {
      oldCasts = IntegerPool.get(0);
    }
    int newCasts = oldCasts.intValue() + count;
    SkillDatabase.castsById.put(IntegerPool.get(skillId), IntegerPool.get(newCasts));
  }

  public static String skillString(
      final int skillId,
      final String skillName,
      final String image,
      final int type,
      final long mp,
      final int duration,
      final int level) {
    StringBuilder buffer = new StringBuilder();

    buffer.append(skillId);
    buffer.append("\t");
    buffer.append(skillName);
    buffer.append("\t");
    buffer.append(image);
    buffer.append("\t");
    buffer.append(type);
    buffer.append("\t");
    buffer.append(mp);
    buffer.append("\t");
    buffer.append(duration);
    if (level != 0) {
      buffer.append("\t");
      buffer.append(level);
    }
    return buffer.toString();
  }

  public static final void registerSkill(final int skillId) {
    // Load the description text for this skill
    String text = DebugDatabase.readSkillDescriptionText(skillId);
    if (text == null) {
      return;
    }
    SkillDatabase.registerSkill(text, skillId, null);
  }

  public static final void registerSkill(final int skillId, String skillName) {
    // Load the description text for this skill
    String text = DebugDatabase.readSkillDescriptionText(skillId);
    if (text == null) {
      return;
    }
    SkillDatabase.registerSkill(text, skillId, skillName);
  }

  public static final void registerSkill(String text, final int skillId, String skillName) {
    if (skillName == null) {
      skillName = DebugDatabase.parseName(text);
    }

    String image = DebugDatabase.parseImage(text);

    // Detach name and image from being substrings
    skillName = skillName;
    image = image;

    String typeString = DebugDatabase.parseSkillType(text);
    int type =
        typeString.equals("Passive")
            ? 0
            : typeString.equals("Noncombat") ? 3 : typeString.equals("Combat") ? 5 : -1;
    long mp = DebugDatabase.parseSkillMPCost(text);
    int duration = DebugDatabase.parseSkillEffectDuration(text);
    int level = 0;

    SkillDatabase.addSkill(skillId, skillName, image, type, mp, duration, level);

    String printMe;

    // Print what goes in classkills.txt
    printMe = "--------------------";
    RequestLogger.printLine(printMe);
    RequestLogger.updateSessionLog(printMe);

    printMe = SkillDatabase.skillString(skillId, skillName, image, type, mp, duration, level);
    RequestLogger.printLine(printMe);
    RequestLogger.updateSessionLog(printMe);

    // Passive skills have modifiers
    if (type == 0) {
      // Let modifiers database do what it wishes with this skill
      Modifiers.registerSkill(skillName, text);
    }

    printMe = "--------------------";
    RequestLogger.printLine(printMe);
    RequestLogger.updateSessionLog(printMe);

    String effectName = DebugDatabase.parseSkillEffectName(text);
    if (!effectName.equals("") && EffectDatabase.getEffectId(effectName, true) == -1) {
      String effectDescid = DebugDatabase.parseSkillEffectId(text);
      EffectDatabase.registerEffect(effectName, effectDescid, "cast 1 " + skillName);
    }

    // Update Canonical names list
    SkillDatabase.canonicalNames = new String[SkillDatabase.skillIdSetByName.size()];
    SkillDatabase.skillIdSetByName.keySet().toArray(SkillDatabase.canonicalNames);
  }

  /**
   * Utility method used to get the number of times a skill has been cast in the current session.
   */
  public static int getCasts(int skillId) {
    Integer casts = SkillDatabase.castsById.get(IntegerPool.get(skillId));

    if (casts == null) {
      return 0;
    }
    return casts.intValue();
  }

  public static boolean sourceAgentSkill(int skillId) {
    // Return true if this skill is usable against a source agent
    // All class 21 skills can be used
    if ((skillId / 1000) == 21) {
      return true;
    }

    // Some Source Terminal skills are usable. Turbo for sure.
    // List all until we learn which ones are not usable
    switch (skillId) {
      case SkillPool.EXTRACT:
      case SkillPool.DIGITIZE:
      case SkillPool.COMPRESS:
      case SkillPool.DUPLICATE:
      case SkillPool.PORTSCAN:
      case SkillPool.TURBO:
        return true;
    }
    return false;
  }

  public static boolean summonsMonster(int skillId) {
    switch (skillId) {
      case SkillPool.RAIN_MAN:
      case SkillPool.EVOKE_ELDRITCH_HORROR:
        return true;
    }
    return false;
  }
}
