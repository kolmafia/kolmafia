package net.sourceforge.kolmafia;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.maximizer.Maximizer;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.DebugDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest.Companion;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FloristRequest;
import net.sourceforge.kolmafia.request.FloristRequest.Florist;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class Modifiers {
  private static final Map<String, Object> modifiersByName = new HashMap<>();
  private static final Map<String, String> familiarEffectByName = new HashMap<>();
  private static final Map<String, Integer> modifierIndicesByName = new HashMap<>();
  private static final List<UseSkillRequest> passiveSkills = new ArrayList<>();
  private static final Map<String, Integer> synergies = new HashMap<>();
  private static final List<String> mutexes = new ArrayList<>();
  private static final Map<String, Set<String>> uniques = new HashMap<>();
  public static String currentLocation = "";
  public static String currentZone = "";
  public static String currentEnvironment = "";
  public static double currentML = 4.0;
  public static String currentFamiliar = "";
  public static String mainhandClass = "";
  public static double hoboPower = 0.0;
  public static double smithsness = 0.0;
  public static double currentWeight = 0.0;
  public static boolean unarmed = false;

  private static final Pattern FAMILIAR_EFFECT_PATTERN =
      Pattern.compile("Familiar Effect: \"(.*?)\"");
  private static final Pattern FAMILIAR_EFFECT_TRANSLATE_PATTERN =
      Pattern.compile("([\\d.]+)\\s*x\\s*(Volley|Somb|Lep|Fairy)");
  private static final String FAMILIAR_EFFECT_TRANSLATE_REPLACEMENT = "$2: $1 ";
  private static final Pattern FAMILIAR_EFFECT_TRANSLATE_PATTERN2 =
      Pattern.compile("cap ([\\d.]+)");
  private static final String FAMILIAR_EFFECT_TRANSLATE_REPLACEMENT2 = "Familiar Weight Cap: $1 ";

  public static final int FAMILIAR_WEIGHT = 0;
  public static final int MONSTER_LEVEL = 1;
  public static final int COMBAT_RATE = 2;
  public static final int INITIATIVE = 3;
  public static final int EXPERIENCE = 4;
  public static final int ITEMDROP = 5;
  public static final int MEATDROP = 6;
  public static final int DAMAGE_ABSORPTION = 7;
  public static final int DAMAGE_REDUCTION = 8;
  public static final int COLD_RESISTANCE = 9;
  public static final int HOT_RESISTANCE = 10;
  public static final int SLEAZE_RESISTANCE = 11;
  public static final int SPOOKY_RESISTANCE = 12;
  public static final int STENCH_RESISTANCE = 13;
  public static final int MANA_COST = 14;
  public static final int MOX = 15;
  public static final int MOX_PCT = 16;
  public static final int MUS = 17;
  public static final int MUS_PCT = 18;
  public static final int MYS = 19;
  public static final int MYS_PCT = 20;
  public static final int HP = 21;
  public static final int HP_PCT = 22;
  public static final int MP = 23;
  public static final int MP_PCT = 24;
  public static final int WEAPON_DAMAGE = 25;
  public static final int RANGED_DAMAGE = 26;
  public static final int SPELL_DAMAGE = 27;
  public static final int SPELL_DAMAGE_PCT = 28;
  public static final int COLD_DAMAGE = 29;
  public static final int HOT_DAMAGE = 30;
  public static final int SLEAZE_DAMAGE = 31;
  public static final int SPOOKY_DAMAGE = 32;
  public static final int STENCH_DAMAGE = 33;
  public static final int COLD_SPELL_DAMAGE = 34;
  public static final int HOT_SPELL_DAMAGE = 35;
  public static final int SLEAZE_SPELL_DAMAGE = 36;
  public static final int SPOOKY_SPELL_DAMAGE = 37;
  public static final int STENCH_SPELL_DAMAGE = 38;
  public static final int UNDERWATER_COMBAT_RATE = 39;
  public static final int FUMBLE = 40;
  public static final int HP_REGEN_MIN = 41;
  public static final int HP_REGEN_MAX = 42;
  public static final int MP_REGEN_MIN = 43;
  public static final int MP_REGEN_MAX = 44;
  public static final int ADVENTURES = 45;
  public static final int FAMILIAR_WEIGHT_PCT = 46;
  public static final int WEAPON_DAMAGE_PCT = 47;
  public static final int RANGED_DAMAGE_PCT = 48;
  public static final int STACKABLE_MANA_COST = 49;
  public static final int HOBO_POWER = 50;
  public static final int BASE_RESTING_HP = 51;
  public static final int RESTING_HP_PCT = 52;
  public static final int BONUS_RESTING_HP = 53;
  public static final int BASE_RESTING_MP = 54;
  public static final int RESTING_MP_PCT = 55;
  public static final int BONUS_RESTING_MP = 56;
  public static final int CRITICAL_PCT = 57;
  public static final int PVP_FIGHTS = 58;
  public static final int VOLLEYBALL_WEIGHT = 59;
  public static final int SOMBRERO_WEIGHT = 60;
  public static final int LEPRECHAUN_WEIGHT = 61;
  public static final int FAIRY_WEIGHT = 62;
  public static final int MEATDROP_PENALTY = 63;
  public static final int HIDDEN_FAMILIAR_WEIGHT = 64;
  public static final int ITEMDROP_PENALTY = 65;
  public static final int INITIATIVE_PENALTY = 66;
  public static final int FOODDROP = 67;
  public static final int BOOZEDROP = 68;
  public static final int HATDROP = 69;
  public static final int WEAPONDROP = 70;
  public static final int OFFHANDDROP = 71;
  public static final int SHIRTDROP = 72;
  public static final int PANTSDROP = 73;
  public static final int ACCESSORYDROP = 74;
  public static final int VOLLEYBALL_EFFECTIVENESS = 75;
  public static final int SOMBRERO_EFFECTIVENESS = 76;
  public static final int LEPRECHAUN_EFFECTIVENESS = 77;
  public static final int FAIRY_EFFECTIVENESS = 78;
  public static final int FAMILIAR_WEIGHT_CAP = 79;
  public static final int SLIME_RESISTANCE = 80;
  public static final int SLIME_HATES_IT = 81;
  public static final int SPELL_CRITICAL_PCT = 82;
  public static final int MUS_EXPERIENCE = 83;
  public static final int MYS_EXPERIENCE = 84;
  public static final int MOX_EXPERIENCE = 85;
  public static final int EFFECT_DURATION = 86;
  public static final int CANDYDROP = 87;
  public static final int DB_COMBAT_DAMAGE = 88;
  public static final int SOMBRERO_BONUS = 89;
  public static final int FAMILIAR_EXP = 90;
  public static final int SPORADIC_MEATDROP = 91;
  public static final int SPORADIC_ITEMDROP = 92;
  public static final int MEAT_BONUS = 93;
  public static final int PICKPOCKET_CHANCE = 94;
  public static final int COMBAT_MANA_COST = 95;
  public static final int MUS_EXPERIENCE_PCT = 96;
  public static final int MYS_EXPERIENCE_PCT = 97;
  public static final int MOX_EXPERIENCE_PCT = 98;
  public static final int MINSTREL_LEVEL = 99;
  public static final int MUS_LIMIT = 100;
  public static final int MYS_LIMIT = 101;
  public static final int MOX_LIMIT = 102;
  public static final int SONG_DURATION = 103;
  public static final int PRISMATIC_DAMAGE = 104;
  public static final int SMITHSNESS = 105;
  public static final int SUPERCOLD_RESISTANCE = 106;
  public static final int REDUCE_ENEMY_DEFENSE = 107;
  public static final int POOL_SKILL = 108;
  public static final int SURGEONOSITY = 109;
  public static final int FAMILIAR_DAMAGE = 110;
  public static final int GEARDROP = 111;
  public static final int MAXIMUM_HOOCH = 112;
  public static final int WATER_LEVEL = 113;
  public static final int CRIMBOT_POWER = 114;
  public static final int FAMILIAR_TUNING_MUSCLE = 115;
  public static final int FAMILIAR_TUNING_MYSTICALITY = 116;
  public static final int FAMILIAR_TUNING_MOXIE = 117;
  public static final int RANDOM_MONSTER_MODIFIERS = 118;
  public static final int LUCK = 119;
  public static final int OTHELLO_SKILL = 120;
  public static final int DISCO_STYLE = 121;
  public static final int ROLLOVER_EFFECT_DURATION = 122;
  public static final int SIXGUN_DAMAGE = 123;
  public static final int FISHING_SKILL = 124;
  public static final int ADDITIONAL_SONG = 125;
  public static final int SPRINKLES = 126;
  public static final int ABSORB_ADV = 127;
  public static final int ABSORB_STAT = 128;
  public static final int RUBEE_DROP = 129;
  public static final int KRUEGERAND_DROP = 130;
  public static final int WARBEAR_ARMOR_PENETRATION = 131;
  public static final int CLOWNINESS = 132;
  public static final int PP = 133;
  public static final int PLUMBER_POWER = 134;
  public static final int DRIPPY_DAMAGE = 135;
  public static final int DRIPPY_RESISTANCE = 136;
  public static final int ENERGY = 137;
  public static final int SCRAP = 138;
  public static final int FAMILIAR_ACTION_BONUS = 139;
  public static final int WATER = 140;
  public static final int SPLEEN_DROP = 141;
  public static final int POTION_DROP = 142;
  public static final int SAUCE_SPELL_DAMAGE = 143;
  public static final String EXPR = "(?:([-+]?[\\d.]+)|\\[([^]]+)\\])";

  private static final Object[][] doubleModifiers = {
    {
      "Familiar Weight",
      Pattern.compile("([+-]\\d+) (to )?Familiar Weight"),
      Pattern.compile("Familiar Weight: " + EXPR)
    },
    {
      "Monster Level",
      new Object[] {
        Pattern.compile("([+-]\\d+) to Monster Level"), Pattern.compile("Monster Level ([+-]\\d+)"),
      },
      Pattern.compile("Monster Level: " + EXPR)
    },
    {"Combat Rate", null, Pattern.compile("Combat Rate: " + EXPR)},
    {
      "Initiative",
      new Object[] {
        Pattern.compile("Combat Initiative ([+-]\\d+)%"),
        Pattern.compile("([+-]\\d+)% Combat Initiative"),
      },
      Pattern.compile("Initiative: " + EXPR)
    },
    {
      "Experience",
      Pattern.compile("([+-]\\d+) Stat.*Per Fight"),
      Pattern.compile("Experience: " + EXPR)
    },
    {
      "Item Drop",
      Pattern.compile("([+-]\\d+)% Item Drops? [Ff]rom Monsters$"),
      Pattern.compile("Item Drop: " + EXPR)
    },
    {
      "Meat Drop",
      Pattern.compile("([+-]\\d+)% Meat from Monsters"),
      Pattern.compile("Meat Drop: " + EXPR)
    },
    {
      "Damage Absorption",
      Pattern.compile("Damage Absorption ([+-]\\d+)"),
      Pattern.compile("Damage Absorption: " + EXPR)
    },
    {
      "Damage Reduction",
      Pattern.compile("Damage Reduction: ([+-]?\\d+)"),
      Pattern.compile("Damage Reduction: " + EXPR)
    },
    {"Cold Resistance", null, Pattern.compile("Cold Resistance: " + EXPR)},
    {"Hot Resistance", null, Pattern.compile("Hot Resistance: " + EXPR)},
    {"Sleaze Resistance", null, Pattern.compile("Sleaze Resistance: " + EXPR)},
    {"Spooky Resistance", null, Pattern.compile("Spooky Resistance: " + EXPR)},
    {"Stench Resistance", null, Pattern.compile("Stench Resistance: " + EXPR)},
    {
      "Mana Cost",
      Pattern.compile("([+-]\\d+) MP to use Skills$"),
      Pattern.compile("Mana Cost: " + EXPR)
    },
    {
      "Moxie",
      new Object[] {
        Pattern.compile("Moxie ([+-]\\d+)$"), Pattern.compile("([+-]\\d+) Moxie$"),
      },
      Pattern.compile("Moxie: " + EXPR)
    },
    {
      "Moxie Percent",
      new Object[] {
        Pattern.compile("Moxie ([+-]\\d+)%"), Pattern.compile("([+-]\\d+)% Moxie"),
      },
      Pattern.compile("Moxie Percent: " + EXPR)
    },
    {
      "Muscle",
      new Object[] {
        Pattern.compile("Muscle ([+-]\\d+)$"), Pattern.compile("([+-]\\d+) Muscle$"),
      },
      Pattern.compile("Muscle: " + EXPR)
    },
    {
      "Muscle Percent",
      new Object[] {
        Pattern.compile("Muscle ([+-]\\d+)%"), Pattern.compile("([+-]\\d+)% Muscle"),
      },
      Pattern.compile("Muscle Percent: " + EXPR)
    },
    {
      "Mysticality",
      new Object[] {
        Pattern.compile("Mysticality ([+-]\\d+)$"), Pattern.compile("([+-]\\d+) Mysticality$"),
      },
      Pattern.compile("Mysticality: " + EXPR)
    },
    {
      "Mysticality Percent",
      new Object[] {
        Pattern.compile("Mysticality ([+-]\\d+)%"), Pattern.compile("([+-]\\d+)% Mysticality"),
      },
      Pattern.compile("Mysticality Percent: " + EXPR)
    },
    {
      "Maximum HP",
      Pattern.compile("Maximum HP ([+-]\\d+)$"),
      Pattern.compile("Maximum HP: " + EXPR)
    },
    {
      "Maximum HP Percent",
      Pattern.compile("Maximum HP ([+-]\\d+)%"),
      Pattern.compile("Maximum HP Percent: " + EXPR)
    },
    {
      "Maximum MP",
      Pattern.compile("Maximum MP ([+-]\\d+)$"),
      Pattern.compile("Maximum MP: " + EXPR)
    },
    {
      "Maximum MP Percent",
      Pattern.compile("Maximum MP ([+-]\\d+)%"),
      Pattern.compile("Maximum MP Percent: " + EXPR)
    },
    {
      "Weapon Damage",
      new Object[] {
        Pattern.compile("Weapon Damage ([+-]\\d+)$"), Pattern.compile("([+-]\\d+) Weapon Damage"),
      },
      Pattern.compile("Weapon Damage: " + EXPR)
    },
    {
      "Ranged Damage",
      new Object[] {
        Pattern.compile("Ranged Damage ([+-]\\d+)$"), Pattern.compile("([+-]\\d+) Ranged Damage"),
      },
      Pattern.compile("Ranged Damage: " + EXPR)
    },
    {
      "Spell Damage",
      new Object[] {
        Pattern.compile("Spell Damage ([+-]\\d+)$"), Pattern.compile("([+-]\\d+) Spell Damage"),
      },
      Pattern.compile("(?:^|, )Spell Damage: " + EXPR)
    },
    {
      "Spell Damage Percent",
      new Object[] {
        Pattern.compile("Spell Damage ([+-][\\d.]+)%"),
        Pattern.compile("([+-][\\d.]+)% Spell Damage"),
      },
      Pattern.compile("Spell Damage Percent: " + EXPR)
    },
    {
      "Cold Damage",
      Pattern.compile("^([+-]\\d+) <font color=blue>Cold Damage<"),
      Pattern.compile("Cold Damage: " + EXPR)
    },
    {
      "Hot Damage",
      Pattern.compile("^([+-]\\d+) <font color=red>Hot Damage<"),
      Pattern.compile("Hot Damage: " + EXPR)
    },
    {
      "Sleaze Damage",
      Pattern.compile("^([+-]\\d+) <font color=blueviolet>Sleaze Damage<"),
      Pattern.compile("Sleaze Damage: " + EXPR)
    },
    {
      "Spooky Damage",
      Pattern.compile("^([+-]\\d+) <font color=gray>Spooky Damage<"),
      Pattern.compile("Spooky Damage: " + EXPR)
    },
    {
      "Stench Damage",
      Pattern.compile("^([+-]\\d+) <font color=green>Stench Damage<"),
      Pattern.compile("Stench Damage: " + EXPR)
    },
    {
      "Cold Spell Damage",
      Pattern.compile("^([+-]\\d+) (Damage )?to <font color=blue>Cold Spells</font>"),
      Pattern.compile("Cold Spell Damage: " + EXPR)
    },
    {
      "Hot Spell Damage",
      Pattern.compile("^([+-]\\d+) (Damage )?to (<font color=red>)?Hot Spells(</font>)?"),
      Pattern.compile("Hot Spell Damage: " + EXPR)
    },
    {
      "Sleaze Spell Damage",
      Pattern.compile("^([+-]\\d+) (Damage )?to <font color=blueviolet>Sleaze Spells</font>"),
      Pattern.compile("Sleaze Spell Damage: " + EXPR)
    },
    {
      "Spooky Spell Damage",
      Pattern.compile("^([+-]\\d+) (Damage )?to <font color=gray>Spooky Spells</font>"),
      Pattern.compile("Spooky Spell Damage: " + EXPR)
    },
    {
      "Stench Spell Damage",
      Pattern.compile("^([+-]\\d+) (Damage )?to <font color=green>Stench Spells</font>"),
      Pattern.compile("Stench Spell Damage: " + EXPR)
    },
    {"Underwater Combat Rate", null, Pattern.compile("Combat Rate \\(Underwater\\): " + EXPR)},
    {"Fumble", Pattern.compile("(\\d+)x chance of Fumble"), Pattern.compile("Fumble: " + EXPR)},
    {"HP Regen Min", null, Pattern.compile("HP Regen Min: " + EXPR)},
    {"HP Regen Max", null, Pattern.compile("HP Regen Max: " + EXPR)},
    {"MP Regen Min", null, Pattern.compile("MP Regen Min: " + EXPR)},
    {"MP Regen Max", null, Pattern.compile("MP Regen Max: " + EXPR)},
    {
      "Adventures",
      Pattern.compile("([+-]\\d+) Adventure\\(s\\) per day( when equipped)?"),
      Pattern.compile("Adventures: " + EXPR)
    },
    {
      "Familiar Weight Percent",
      Pattern.compile("([+-]\\d+)% Familiar Weight"),
      Pattern.compile("Familiar Weight Percent: " + EXPR)
    },
    {
      "Weapon Damage Percent",
      Pattern.compile("Weapon Damage ([+-]\\d+)%"),
      Pattern.compile("Weapon Damage Percent: " + EXPR)
    },
    {
      "Ranged Damage Percent",
      Pattern.compile("Ranged Damage ([+-]\\d+)%"),
      Pattern.compile("Ranged Damage Percent: " + EXPR)
    },
    {
      "Stackable Mana Cost",
      Pattern.compile("([+-]\\d+) MP to use Skills$"),
      Pattern.compile("Mana Cost \\(stackable\\): " + EXPR)
    },
    {
      "Hobo Power", Pattern.compile("([+-]\\d+) Hobo Power"), Pattern.compile("Hobo Power: " + EXPR)
    },
    {"Base Resting HP", null, Pattern.compile("Base Resting HP: " + EXPR)},
    {"Resting HP Percent", null, Pattern.compile("Resting HP Percent: " + EXPR)},
    {"Bonus Resting HP", null, Pattern.compile("Bonus Resting HP: " + EXPR)},
    {"Base Resting MP", null, Pattern.compile("Base Resting MP: " + EXPR)},
    {"Resting MP Percent", null, Pattern.compile("Resting MP Percent: " + EXPR)},
    {"Bonus Resting MP", null, Pattern.compile("Bonus Resting MP: " + EXPR)},
    {
      "Critical Hit Percent",
      Pattern.compile("([+-]\\d+)% [Cc]hance of Critical Hit"),
      Pattern.compile("Critical Hit Percent: " + EXPR)
    },
    {
      "PvP Fights",
      Pattern.compile("([+-]\\d+) PvP [Ff]ight\\(s\\) per day( when equipped)?"),
      Pattern.compile("PvP Fights: " + EXPR)
    },
    {"Volleyball", null, Pattern.compile("Volley(?:ball)?: " + EXPR)},
    {"Sombrero", null, Pattern.compile("Somb(?:rero)?: " + EXPR)},
    {"Leprechaun", null, Pattern.compile("Lep(?:rechaun)?: " + EXPR)},
    {"Fairy", null, Pattern.compile("Fairy: " + EXPR)},
    {"Meat Drop Penalty", null, Pattern.compile("Meat Drop Penalty: " + EXPR)},
    {"Hidden Familiar Weight", null, Pattern.compile("Familiar Weight \\(hidden\\): " + EXPR)},
    {"Item Drop Penalty", null, Pattern.compile("Item Drop Penalty: " + EXPR)},
    {"Initiative Penalty", null, Pattern.compile("Initiative Penalty: " + EXPR)},
    {
      "Food Drop",
      Pattern.compile("([+-]\\d+)% Food Drops? [Ff]rom Monsters$"),
      Pattern.compile("Food Drop: " + EXPR)
    },
    {
      "Booze Drop",
      Pattern.compile("([+-]\\d+)% Booze Drops? [Ff]rom Monsters$"),
      Pattern.compile("Booze Drop: " + EXPR)
    },
    {
      "Hat Drop",
      Pattern.compile("([+-]\\d+)% Hat(?:/Pants)? Drops? [Ff]rom Monsters$"),
      Pattern.compile("Hat Drop: " + EXPR)
    },
    {
      "Weapon Drop",
      Pattern.compile("([+-]\\d+)% Weapon Drops? [Ff]rom Monsters$"),
      Pattern.compile("Weapon Drop: " + EXPR)
    },
    {
      "Offhand Drop",
      Pattern.compile("([+-]\\d+)% Off-[Hh]and Drops? [Ff]rom Monsters$"),
      Pattern.compile("Offhand Drop: " + EXPR)
    },
    {
      "Shirt Drop",
      Pattern.compile("([+-]\\d+)% Shirt Drops? [Ff]rom Monsters$"),
      Pattern.compile("Shirt Drop: " + EXPR)
    },
    {
      "Pants Drop",
      Pattern.compile("([+-]\\d+)% (?:Hat/)?Pants Drops? [Ff]rom Monsters$"),
      Pattern.compile("Pants Drop: " + EXPR)
    },
    {
      "Accessory Drop",
      Pattern.compile("([+-]\\d+)% Accessory Drops? [Ff]rom Monsters$"),
      Pattern.compile("Accessory Drop: " + EXPR)
    },
    {"Volleyball Effectiveness", null, Pattern.compile("Volleyball Effectiveness: " + EXPR)},
    {"Sombrero Effectiveness", null, Pattern.compile("Sombrero Effectiveness: " + EXPR)},
    {"Leprechaun Effectiveness", null, Pattern.compile("Leprechaun Effectiveness: " + EXPR)},
    {"Fairy Effectiveness", null, Pattern.compile("Fairy Effectiveness: " + EXPR)},
    {"Familiar Weight Cap", null, Pattern.compile("Familiar Weight Cap: " + EXPR)},
    {"Slime Resistance", null, Pattern.compile("Slime Resistance: " + EXPR)},
    {
      "Slime Hates It",
      Pattern.compile("Slime( Really)? Hates (It|You)"),
      Pattern.compile("Slime Hates It: " + EXPR)
    },
    {
      "Spell Critical Percent",
      Pattern.compile("([+-]\\d+)% [cC]hance of Spell Critical Hit"),
      Pattern.compile("Spell Critical Percent: " + EXPR)
    },
    {
      "Muscle Experience",
      Pattern.compile("([+-]\\d+) Muscle Stat.*Per Fight"),
      Pattern.compile("Experience \\(Muscle\\): " + EXPR),
      "Experience (Muscle)"
    },
    {
      "Mysticality Experience",
      Pattern.compile("([+-]\\d+) Mysticality Stat.*Per Fight"),
      Pattern.compile("Experience \\(Mysticality\\): " + EXPR),
      "Experience (Mysticality)"
    },
    {
      "Moxie Experience",
      Pattern.compile("([+-]\\d+) Moxie Stat.*Per Fight"),
      Pattern.compile("Experience \\(Moxie\\): " + EXPR),
      "Experience (Moxie)"
    },
    {"Effect Duration", null, Pattern.compile("(?:^|, )Effect Duration: " + EXPR)},
    {
      "Candy Drop",
      Pattern.compile("([+-]\\d+)% Candy Drops? [Ff]rom Monsters$"),
      Pattern.compile("Candy Drop: " + EXPR)
    },
    {
      "DB Combat Damage",
      new Object[] {
        Pattern.compile("([+-]\\d+) damage to Disco Bandit Combat Skills"),
        Pattern.compile("([+-]\\d+) Disco Bandit Skill Damage"),
      },
      Pattern.compile("DB Combat Damage: " + EXPR)
    },
    {
      "Sombrero Bonus",
      Pattern.compile("([+-]\\d+) lbs?\\. of Sombrero"),
      Pattern.compile("Sombrero Bonus: " + EXPR)
    },
    {
      "Familiar Experience",
      Pattern.compile("([+-]\\d+) Familiar Experience"),
      Pattern.compile("Experience \\(familiar\\): " + EXPR),
      "Experience (familiar)"
    },
    {
      "Sporadic Meat Drop",
      null,
      Pattern.compile("Meat Drop \\(sporadic\\): " + EXPR),
      "Meat Drop (sporadic)"
    },
    {
      "Sporadic Item Drop",
      null,
      Pattern.compile("Item Drop \\(sporadic\\): " + EXPR),
      "Item Drop (sporadic)"
    },
    {"Meat Bonus", null, Pattern.compile("Meat Bonus: " + EXPR)},
    {
      "Pickpocket Chance",
      Pattern.compile("([+-]\\d+)% Pickpocket Chance"),
      Pattern.compile("Pickpocket Chance: " + EXPR)
    },
    {
      "Combat Mana Cost",
      Pattern.compile("([+-]\\d+) MP to use Skills \\(in-combat only\\)"),
      Pattern.compile("Mana Cost \\(combat\\): " + EXPR),
      "Mana Cost (combat)"
    },
    {
      "Muscle Experience Percent",
      Pattern.compile("([+-]\\d+)% to all Muscle Gains"),
      Pattern.compile("Experience Percent \\(Muscle\\): " + EXPR),
      "Experience Percent (Muscle)"
    },
    {
      "Mysticality Experience Percent",
      Pattern.compile("([+-]\\d+)% to all Mysticality Gains"),
      Pattern.compile("Experience Percent \\(Mysticality\\): " + EXPR),
      "Experience Percent (Mysticality)"
    },
    {
      "Moxie Experience Percent",
      Pattern.compile("([+-]\\d+)% to all Moxie Gains"),
      Pattern.compile("Experience Percent \\(Moxie\\): " + EXPR),
      "Experience Percent (Moxie)"
    },
    {
      "Minstrel Level",
      new Object[] {
        Pattern.compile("([+-]\\d+) to Minstrel Level"),
        Pattern.compile("Minstrel Level ([+-]\\d+)"),
      },
      Pattern.compile("Minstrel Level: " + EXPR)
    },
    {
      "Muscle Limit",
      Pattern.compile("Base Muscle Limited to (\\d+)"),
      Pattern.compile("Muscle Limit: " + EXPR)
    },
    {
      "Mysticality Limit",
      Pattern.compile("Base Mysticality Limited to (\\d+)"),
      Pattern.compile("Mysticality Limit: " + EXPR)
    },
    {
      "Moxie Limit",
      Pattern.compile("Base Moxie Limited to (\\d+)"),
      Pattern.compile("Moxie Limit: " + EXPR)
    },
    {
      "Song Duration",
      Pattern.compile("Song Duration: ([+-]\\d+) Adventures"),
      Pattern.compile("Song Duration: " + EXPR)
    },
    {
      "Prismatic Damage", null, null,
    },
    {
      "Smithsness", Pattern.compile("([+-]\\d+) Smithsness"), Pattern.compile("Smithsness: " + EXPR)
    },
    {"Supercold Resistance", null, Pattern.compile("Supercold Resistance: " + EXPR)},
    {
      "Reduce Enemy Defense",
      Pattern.compile("Reduce enemy defense by (\\d+)%"),
      Pattern.compile("Reduce Enemy Defense: " + EXPR)
    },
    {
      "Pool Skill", Pattern.compile("([+-]\\d+) Pool Skill"), Pattern.compile("Pool Skill: " + EXPR)
    },
    {
      "Surgeonosity",
      new Object[] {
        Pattern.compile("Makes you look like a doctor"),
        Pattern.compile("Makes you look like a gross doctor"),
      },
      Pattern.compile("Surgeonosity: (\\+?\\d+)")
    },
    {
      "Familiar Damage",
      new Object[] {
        Pattern.compile("([+-]\\d+) to Familiar Damage"),
        Pattern.compile("Familiar Damage ([+-]\\d+)"),
      },
      Pattern.compile("Familiar Damage: " + EXPR)
    },
    {
      "Gear Drop",
      Pattern.compile("([+-]\\d+)% Gear Drops? [Ff]rom Monsters$"),
      Pattern.compile("Gear Drop: " + EXPR)
    },
    {
      "Maximum Hooch",
      Pattern.compile("([+-]\\d+) Maximum Hooch"),
      Pattern.compile("Maximum Hooch: " + EXPR)
    },
    {"Water Level", null, Pattern.compile("Water Level: " + EXPR)},
    {
      "Crimbot Outfit Power",
      Pattern.compile("([+-]\\d+) Crimbot Outfit Power"),
      Pattern.compile("Crimbot Outfit Power: " + EXPR)
    },
    {
      "Familiar Tuning Muscle",
      null,
      Pattern.compile("Familiar Tuning \\(Muscle\\): " + EXPR),
      "Familiar Tuning (Muscle)"
    },
    {
      "Familiar Tuning Mysticality",
      null,
      Pattern.compile("Familiar Tuning \\(Mysticality\\): " + EXPR),
      "Familiar Tuning (Mysticality)"
    },
    {
      "Familiar Tuning Moxie",
      null,
      Pattern.compile("Familiar Tuning \\(Moxie\\): " + EXPR),
      "Familiar Tuning (Moxie)"
    },
    {
      "Random Monster Modifiers",
      Pattern.compile("([+-]\\d+) Random Monster Modifier"),
      Pattern.compile("Random Monster Modifiers: " + EXPR)
    },
    {"Luck", Pattern.compile("([+-]\\d+) Luck"), Pattern.compile("Luck: " + EXPR)},
    {
      "Othello Skill",
      Pattern.compile("([+-]\\d+) Othello Skill"),
      Pattern.compile("Othello Skill: " + EXPR)
    },
    {
      "Disco Style",
      Pattern.compile("([+-]\\d+) Disco Style"),
      Pattern.compile("Disco Style: " + EXPR)
    },
    {
      "Rollover Effect Duration",
      Pattern.compile("Grants (\\d+) Adventures of <b>.*?</b> at Rollover"),
      Pattern.compile("Rollover Effect Duration: " + EXPR)
    },
    {"Sixgun Damage", null, Pattern.compile("Sixgun Damage: " + EXPR)},
    {
      "Fishing Skill",
      Pattern.compile("([+-]\\d+) Fishing Skill"),
      Pattern.compile("Fishing Skill: " + EXPR)
    },
    {
      "Additional Song",
      Pattern.compile("Keep (\\d+) additional song in your head"),
      Pattern.compile("Additional Song: " + EXPR)
    },
    {
      "Sprinkle Drop",
      Pattern.compile("([+-]\\d+)% Sprinkles from Monsters"),
      Pattern.compile("Sprinkle Drop: " + EXPR)
    },
    {
      "Absorb Adventures",
      Pattern.compile("([+-]\\d+) Adventures when you absorb an item"),
      Pattern.compile("Absorb Adventures: " + EXPR)
    },
    {
      "Absorb Stats",
      Pattern.compile("([+-]\\d+) Stats when you absorb an item"),
      Pattern.compile("Absorb Stats: " + EXPR)
    },
    {
      "Rubee Drop",
      Pattern.compile("FantasyRealm enemies will drop (\\d+) extra Rubee"),
      Pattern.compile("Rubee Drop: " + EXPR)
    },
    {
      "Kruegerand Drop",
      Pattern.compile("Lets you find (\\d+)% more Kruegerands"),
      Pattern.compile("Kruegerand Drop: " + EXPR)
    },
    {
      "WarBear Armor Penetration",
      Pattern.compile("([+-]\\d+) WarBear Armor Penetration"),
      Pattern.compile("WarBear Armor Penetration: " + EXPR)
    },
    {
      "Clowniness",
      Pattern.compile("Makes you look (\\d+)% clowny"),
      Pattern.compile("Clowniness: " + EXPR)
    },
    {
      "Maximum PP",
      Pattern.compile("([+-]\\d+) Max(imum)? Power Point"),
      Pattern.compile("Maximum PP: " + EXPR)
    },
    {"Plumber Power", null, Pattern.compile("Plumber Power: " + EXPR)},
    {
      "Drippy Damage",
      new Object[] {
        Pattern.compile("([+-]\\d+) Damage vs. creatures of The Drip"),
        Pattern.compile("([+-]\\d+) Damage against Drip creatures"),
      },
      Pattern.compile("Drippy Damage: " + EXPR)
    },
    {"Drippy Resistance", null, Pattern.compile("Drippy Resistance: " + EXPR)},
    {"Energy", null, Pattern.compile("Energy: " + EXPR)},
    {"Scrap", null, Pattern.compile("Scrap: " + EXPR)},
    {"Familiar Action Bonus", null, Pattern.compile("Familiar Action Bonus: " + EXPR)},
    {
      "Water",
      Pattern.compile("Collect (\\d+) water per adventure"),
      Pattern.compile("Water: " + EXPR)
    },
    {
      "Spleen Drop",
      Pattern.compile("([+-]\\d+)% Spleen Item Drops? [Ff]rom Monsters$"),
      Pattern.compile("Spleen Drop: " + EXPR)
    },
    {
      "Potion Drop",
      Pattern.compile("([+-]\\d+)% Potion Drops? [Ff]rom Monsters$"),
      Pattern.compile("Potion Drop: " + EXPR)
    },
    {
      "Sauce Spell Damage",
      new Object[] {
        Pattern.compile("Sauce Spell Damage ([+-]\\d+)$"),
        Pattern.compile("([+-]\\d+) Sauce Spell Damage"),
      },
      Pattern.compile("(?:^|, )Sauce Spell Damage: " + EXPR)
    },
  };

  public static final int DOUBLE_MODIFIERS = Modifiers.doubleModifiers.length;

  private static final HashSet<String> numericModifiers = new HashSet<String>();

  static {
    for (int i = 0; i < DOUBLE_MODIFIERS; ++i) {
      Object[] modifier = Modifiers.doubleModifiers[i];
      modifierIndicesByName.put((String) modifier[0], i);
      String tag = modifier.length > 3 ? (String) modifier[3] : (String) modifier[0];
      Modifiers.numericModifiers.add(tag);
    }
  }

  public static boolean isNumericModifier(final String key) {
    return Modifiers.numericModifiers.contains(key);
  }

  public static final int BOOLEANS = 0;
  public static final int BRIMSTONE = 1;
  public static final int CLOATHING = 2;
  public static final int SYNERGETIC = 3;
  public static final int RAVEOSITY = 4;
  public static final int MUTEX = 5;
  public static final int MUTEX_VIOLATIONS = 6;

  private static final Object[][] bitmapModifiers = {
    {"(booleans)", null, null},
    {"Brimstone", null, Pattern.compile("Brimstone")},
    {"Cloathing", null, Pattern.compile("Cloathing")},
    {"Synergetic", null, Pattern.compile("Synergetic")},
    {"Raveosity", null, Pattern.compile("Raveosity: (\\+?\\d+)")},
    {"Mutually Exclusive", null, null},
    {"Mutex Violations", null, null},
  };

  public static final int BITMAP_MODIFIERS = Modifiers.bitmapModifiers.length;
  private static final int[] bitmapMasks = new int[BITMAP_MODIFIERS];

  static {
    Arrays.fill(bitmapMasks, 1);

    for (int i = 0; i < BITMAP_MODIFIERS; ++i) {
      Object[] modifier = Modifiers.bitmapModifiers[i];
      modifierIndicesByName.put((String) modifier[0], DOUBLE_MODIFIERS + i);
    }
  }

  public static final int SOFTCORE = 0;
  public static final int SINGLE = 1;
  public static final int NEVER_FUMBLE = 2;
  public static final int WEAKENS = 3;
  public static final int FREE_PULL = 4;
  public static final int VARIABLE = 5;
  public static final int NONSTACKABLE_WATCH = 6;
  public static final int COLD_IMMUNITY = 7;
  public static final int HOT_IMMUNITY = 8;
  public static final int SLEAZE_IMMUNITY = 9;
  public static final int SPOOKY_IMMUNITY = 10;
  public static final int STENCH_IMMUNITY = 11;
  public static final int COLD_VULNERABILITY = 12;
  public static final int HOT_VULNERABILITY = 13;
  public static final int SLEAZE_VULNERABILITY = 14;
  public static final int SPOOKY_VULNERABILITY = 15;
  public static final int STENCH_VULNERABILITY = 16;
  public static final int MOXIE_CONTROLS_MP = 17;
  public static final int MOXIE_MAY_CONTROL_MP = 18;
  public static final int FOUR_SONGS = 19;
  public static final int ADVENTURE_UNDERWATER = 20;
  public static final int UNDERWATER_FAMILIAR = 21;
  public static final int GENERIC = 22;
  public static final int UNARMED = 23;
  public static final int NOPULL = 24;
  public static final int LASTS_ONE_DAY = 25;
  public static final int ATTACKS_CANT_MISS = 26;
  public static final int LOOK_LIKE_A_PIRATE = 27;
  public static final int BREAKABLE = 28;
  public static final int DROPS_ITEMS = 29;
  public static final int DROPS_MEAT = 30;

  private static final Object[][] booleanModifiers = {
    {
      "Softcore Only",
      Pattern.compile("This item cannot be equipped while in Hardcore"),
      Pattern.compile("Softcore Only")
    },
    {"Single Equip", null, Pattern.compile("Single Equip")},
    {"Never Fumble", Pattern.compile("Never Fumble"), Pattern.compile("Never Fumble")},
    {
      "Weakens Monster",
      Pattern.compile("Successful hit weakens opponent"),
      Pattern.compile("Weakens Monster")
    },
    {"Free Pull", null, Pattern.compile("Free Pull")},
    {"Variable", null, Pattern.compile("Variable")},
    {"Nonstackable Watch", null, Pattern.compile("Nonstackable Watch")},
    {"Cold Immunity", null, Pattern.compile("Cold Immunity")},
    {"Hot Immunity", null, Pattern.compile("Hot Immunity")},
    {"Sleaze Immunity", null, Pattern.compile("Sleaze Immunity")},
    {"Spooky Immunity", null, Pattern.compile("Spooky Immunity")},
    {"Stench Immunity", null, Pattern.compile("Stench Immunity")},
    {"Cold Vulnerability", null, Pattern.compile("Cold Vulnerability")},
    {"Hot Vulnerability", null, Pattern.compile("Hot Vulnerability")},
    {"Sleaze Vulnerability", null, Pattern.compile("Sleaze Vulnerability")},
    {"Spooky Vulnerability", null, Pattern.compile("Spooky Vulnerability")},
    {"Stench Vulnerability", null, Pattern.compile("Stench Vulnerability")},
    {"Moxie Controls MP", null, Pattern.compile("Moxie Controls MP")},
    {"Moxie May Control MP", null, Pattern.compile("Moxie May Control MP")},
    {
      "Four Songs",
      Pattern.compile("Allows you to keep 4 songs in your head instead of 3"),
      Pattern.compile("Four Songs")
    },
    {
      "Adventure Underwater",
      Pattern.compile("Lets you [bB]reathe [uU]nderwater"),
      Pattern.compile("Adventure Underwater")
    },
    {
      "Underwater Familiar",
      Pattern.compile("Lets your Familiar Breathe Underwater"),
      Pattern.compile("Underwater Familiar")
    },
    {"Generic", null, Pattern.compile("Generic")},
    {
      "Unarmed",
      Pattern.compile("Bonus&nbsp;for&nbsp;Unarmed&nbsp;Characters&nbsp;only"),
      Pattern.compile("Unarmed")
    },
    {"No Pull", null, Pattern.compile("No Pull")},
    {
      "Lasts Until Rollover",
      Pattern.compile("This item will disappear at the end of the day"),
      Pattern.compile("Lasts Until Rollover")
    },
    {
      "Attacks Can't Miss",
      Pattern.compile("Regular Attacks Can't Miss"),
      Pattern.compile("Attacks Can't Miss")
    },
    {"Pirate", null, Pattern.compile("Look like a Pirate")},
    {"Breakable", null, Pattern.compile("Breakable")},
    {"Drops Items", null, Pattern.compile("Drops Items")},
    {"Drops Meat", null, Pattern.compile("Drops Meat")},
  };

  public static final int BOOLEAN_MODIFIERS = Modifiers.booleanModifiers.length;

  static {
    if (BOOLEAN_MODIFIERS > 32) {
      KoLmafia.updateDisplay(
          "Too many boolean modifiers to fit into bitmaps[0].  Will have to store bitmaps as longs, or use two bitmaps to hold the booleans.");
    }
    for (int i = 0; i < BOOLEAN_MODIFIERS; ++i) {
      Object[] modifier = Modifiers.booleanModifiers[i];
      modifierIndicesByName.put((String) modifier[0], DOUBLE_MODIFIERS + BITMAP_MODIFIERS + i);
    }
  }

  public static final int CLASS = 0;
  public static final int INTRINSIC_EFFECT = 1;
  public static final int EQUALIZE = 2;
  public static final int WIKI_NAME = 3;
  public static final int MODIFIERS = 4;
  public static final int OUTFIT = 5;
  public static final int STAT_TUNING = 6;
  public static final int EFFECT = 7;
  public static final int EQUIPS_ON = 8;
  public static final int FAMILIAR_EFFECT = 9;
  public static final int JIGGLE = 10;
  public static final int EQUALIZE_MUSCLE = 11;
  public static final int EQUALIZE_MYST = 12;
  public static final int EQUALIZE_MOXIE = 13;
  public static final int AVATAR = 14;
  public static final int ROLLOVER_EFFECT = 15;
  public static final int SKILL = 16;
  public static final int FLOOR_BUFFED_MUSCLE = 17;
  public static final int FLOOR_BUFFED_MYST = 18;
  public static final int FLOOR_BUFFED_MOXIE = 19;
  public static final int PLUMBER_STAT = 20;

  private static final Object[][] stringModifiers = {
    {
      "Class",
      new Object[] {
        Pattern.compile("Only (.*?) may use this item"),
        Pattern.compile("Bonus for (.*?) only"),
        Pattern.compile("Bonus&nbsp;for&nbsp;(.*?)&nbsp;only"),
      },
      Pattern.compile("Class: \"(.*?)\"")
    },
    {
      "Intrinsic Effect",
      Pattern.compile("Intrinsic Effect: <a.*?><font color=blue>(.*)</font></a>"),
      Pattern.compile("Intrinsic Effect: \"(.*?)\"")
    },
    {"Equalize", null, Pattern.compile("Equalize: \"(.*?)\"")},
    {"Wiki Name", null, Pattern.compile("Wiki Name: \"(.*?)\"")},
    {"Modifiers", null, Pattern.compile("^(none)$")},
    {"Outfit", null, null},
    {"Stat Tuning", null, Pattern.compile("Stat Tuning: \"(.*?)\"")},
    {"Effect", null, Pattern.compile("(?:^|, )Effect: \"(.*?)\"")},
    {"Equips On", null, Pattern.compile("Equips On: \"(.*?)\"")},
    {"Familiar Effect", null, Pattern.compile("Familiar Effect: \"(.*?)\"")},
    {"Jiggle", Pattern.compile("Jiggle: *(.*?)$"), Pattern.compile("Jiggle: \"(.*?)\"")},
    {"Equalize Muscle", null, Pattern.compile("Equalize Muscle: \"(.*?)\"")},
    {"Equalize Mysticality", null, Pattern.compile("Equalize Mysticality: \"(.*?)\"")},
    {"Equalize Moxie", null, Pattern.compile("Equalize Moxie: \"(.*?)\"")},
    {
      "Avatar",
      new Object[] {
        Pattern.compile("Makes you look like (?:a |an |the )?(.++)(?<!doctor|gross doctor)"),
        Pattern.compile("Te hace ver como un (.++)"),
      },
      Pattern.compile("Avatar: \"(.*?)\"")
    },
    {
      "Rollover Effect",
      Pattern.compile("Adventures of <b><a.*?>(.*)</a></b> at Rollover"),
      Pattern.compile("Rollover Effect: \"(.*?)\"")
    },
    {"Skill", Pattern.compile("Grants Skill:.*?<b>(.*?)</b>"), Pattern.compile("Skill: \"(.*?)\"")},
    {"Floor Buffed Muscle", null, Pattern.compile("Floor Buffed Muscle: \"(.*?)\"")},
    {"Floor Buffed Mysticality", null, Pattern.compile("Floor Buffed Mysticality: \"(.*?)\"")},
    {"Floor Buffed Moxie", null, Pattern.compile("Floor Buffed Moxie: \"(.*?)\"")},
    {"Plumber Stat", null, Pattern.compile("Plumber Stat: \"(.*?)\"")},
  };

  public static final int STRING_MODIFIERS = Modifiers.stringModifiers.length;

  static {
    for (int i = 0; i < STRING_MODIFIERS; ++i) {
      Object[] modifier = Modifiers.stringModifiers[i];
      modifierIndicesByName.put(
          (String) modifier[0], DOUBLE_MODIFIERS + BITMAP_MODIFIERS + BOOLEAN_MODIFIERS + i);
    }
  }

  // Indexes for array returned by predict():
  public static final int BUFFED_MUS = 0;
  public static final int BUFFED_MYS = 1;
  public static final int BUFFED_MOX = 2;
  public static final int BUFFED_HP = 3;
  public static final int BUFFED_MP = 4;

  private static final Object[][] derivedModifiers = {
    {"Buffed Muscle"},
    {"Buffed Mysticality"},
    {"Buffed Moxie"},
    {"Buffed HP Maximum"},
    {"Buffed MP Maximum"},
  };

  public static final int DERIVED_MODIFIERS = Modifiers.derivedModifiers.length;

  public int[] predict() {
    int[] rv = new int[Modifiers.DERIVED_MODIFIERS];

    int mus = KoLCharacter.getBaseMuscle();
    int mys = KoLCharacter.getBaseMysticality();
    int mox = KoLCharacter.getBaseMoxie();

    String equalize = this.getString(Modifiers.EQUALIZE);
    if (equalize.startsWith("Mus")) {
      mys = mox = mus;
    } else if (equalize.startsWith("Mys")) {
      mus = mox = mys;
    } else if (equalize.startsWith("Mox")) {
      mus = mys = mox;
    } else if (equalize.startsWith("High")) {
      int high = Math.max(Math.max(mus, mys), mox);
      mus = mys = mox = high;
    }

    String mus_equalize = this.getString(Modifiers.EQUALIZE_MUSCLE);
    if (mus_equalize.startsWith("Mys")) {
      mus = mys;
    } else if (mus_equalize.startsWith("Mox")) {
      mus = mox;
    }
    String mys_equalize = this.getString(Modifiers.EQUALIZE_MYST);
    if (mys_equalize.startsWith("Mus")) {
      mys = mus;
    } else if (mys_equalize.startsWith("Mox")) {
      mys = mox;
    }
    String mox_equalize = this.getString(Modifiers.EQUALIZE_MOXIE);
    if (mox_equalize.startsWith("Mus")) {
      mox = mus;
    } else if (mox_equalize.startsWith("Mys")) {
      mox = mys;
    }

    int mus_limit = (int) this.get(Modifiers.MUS_LIMIT);
    if (mus_limit > 0 && mus > mus_limit) {
      mus = mus_limit;
    }
    int mys_limit = (int) this.get(Modifiers.MYS_LIMIT);
    if (mys_limit > 0 && mys > mys_limit) {
      mys = mys_limit;
    }
    int mox_limit = (int) this.get(Modifiers.MOX_LIMIT);
    if (mox_limit > 0 && mox > mox_limit) {
      mox = mox_limit;
    }

    rv[Modifiers.BUFFED_MUS] =
        mus
            + (int) this.get(Modifiers.MUS)
            + (int) Math.ceil(this.get(Modifiers.MUS_PCT) * mus / 100.0);
    rv[Modifiers.BUFFED_MYS] =
        mys
            + (int) this.get(Modifiers.MYS)
            + (int) Math.ceil(this.get(Modifiers.MYS_PCT) * mys / 100.0);
    rv[Modifiers.BUFFED_MOX] =
        mox
            + (int) this.get(Modifiers.MOX)
            + (int) Math.ceil(this.get(Modifiers.MOX_PCT) * mox / 100.0);

    String mus_buffed_floor = this.getString(Modifiers.FLOOR_BUFFED_MUSCLE);
    if (mus_buffed_floor.startsWith("Mys")) {
      if (rv[Modifiers.BUFFED_MYS] > rv[Modifiers.BUFFED_MUS]) {
        rv[Modifiers.BUFFED_MUS] = rv[Modifiers.BUFFED_MYS];
      }
    } else if (mus_buffed_floor.startsWith("Mox")) {
      if (rv[Modifiers.BUFFED_MOX] > rv[Modifiers.BUFFED_MUS]) {
        rv[Modifiers.BUFFED_MUS] = rv[Modifiers.BUFFED_MOX];
      }
    }
    String mys_buffed_floor = this.getString(Modifiers.FLOOR_BUFFED_MYST);
    if (mys_buffed_floor.startsWith("Mus")) {
      if (rv[Modifiers.BUFFED_MUS] > rv[Modifiers.BUFFED_MYS]) {
        rv[Modifiers.BUFFED_MYS] = rv[Modifiers.BUFFED_MUS];
      }
    } else if (mys_buffed_floor.startsWith("Mox")) {
      if (rv[Modifiers.BUFFED_MOX] > rv[Modifiers.BUFFED_MYS]) {
        rv[Modifiers.BUFFED_MYS] = rv[Modifiers.BUFFED_MOX];
      }
    }
    String mox_buffed_floor = this.getString(Modifiers.FLOOR_BUFFED_MOXIE);
    if (mox_buffed_floor.startsWith("Mus")) {
      if (rv[Modifiers.BUFFED_MUS] > rv[Modifiers.BUFFED_MOX]) {
        rv[Modifiers.BUFFED_MOX] = rv[Modifiers.BUFFED_MUS];
      }
    } else if (mox_buffed_floor.startsWith("Mys")) {
      if (rv[Modifiers.BUFFED_MYS] > rv[Modifiers.BUFFED_MOX]) {
        rv[Modifiers.BUFFED_MOX] = rv[Modifiers.BUFFED_MYS];
      }
    }

    int hpbase =
        KoLCharacter.isVampyre() ? KoLCharacter.getBaseMuscle() : rv[Modifiers.BUFFED_MUS] + 3;
    double C = KoLCharacter.isMuscleClass() ? 1.5 : 1.0;
    int hp =
        (int) Math.ceil(hpbase * (C + this.get(Modifiers.HP_PCT) / 100.0))
            + (int) this.get(Modifiers.HP);
    if (KoLCharacter.isVampyre()) {
      // This block could be merged into the previous calculation, but that
      // would result in a significant reduction in readability
      hp = hpbase + (int) this.get(Modifiers.HP);
    }
    rv[Modifiers.BUFFED_HP] = Math.max(hp, mus);

    int mpbase = rv[Modifiers.BUFFED_MYS];
    if (this.getBoolean(Modifiers.MOXIE_CONTROLS_MP)
        || (this.getBoolean(Modifiers.MOXIE_MAY_CONTROL_MP) && rv[Modifiers.BUFFED_MOX] > mpbase)) {
      mpbase = rv[Modifiers.BUFFED_MOX];
    }
    C = KoLCharacter.isMysticalityClass() ? 1.5 : 1.0;
    int mp =
        (int) Math.ceil(mpbase * (C + this.get(Modifiers.MP_PCT) / 100.0))
            + (int) this.get(Modifiers.MP);
    rv[Modifiers.BUFFED_MP] = Math.max(mp, mys);

    return rv;
  }

  public static final Iterator<String> getAllModifiers() {
    return Modifiers.modifiersByName.keySet().iterator();
  }

  public static final void overrideEffectModifiers(final int effectId) {
    String name = EffectDatabase.getEffectName(effectId);
    String descId = EffectDatabase.getDescriptionId(effectId);
    String text = DebugDatabase.readEffectDescriptionText(descId);

    String mod = DebugDatabase.parseEffectEnchantments(text);
    String lookup = Modifiers.getLookupName("Effect", name);
    Modifiers.overrideModifier(lookup, mod);
  }

  public static final void overrideModifier(String lookup, Object value) {
    if (value != null) {
      Modifiers.modifiersByName.put(lookup, value);
    } else {
      Modifiers.modifiersByName.remove(lookup);
    }
  }

  public static final String getModifierName(final int index) {
    return Modifiers.modifierName(Modifiers.doubleModifiers, index);
  }

  public static final String getBitmapModifierName(final int index) {
    return Modifiers.modifierName(Modifiers.bitmapModifiers, index);
  }

  public static final String getBooleanModifierName(final int index) {
    return Modifiers.modifierName(Modifiers.booleanModifiers, index);
  }

  public static final String getStringModifierName(final int index) {
    return Modifiers.modifierName(Modifiers.stringModifiers, index);
  }

  public static final String getDerivedModifierName(final int index) {
    return Modifiers.modifierName(Modifiers.derivedModifiers, index);
  }

  private static String modifierName(final Object[][] table, final int index) {
    if (index < 0 || index >= table.length) {
      return null;
    }
    return (String) table[index][0];
  }

  private static Object modifierDescPattern(final Object[][] table, final int index) {
    if (index < 0 || index >= table.length) {
      return null;
    }
    return table[index][1];
  }

  private static Pattern modifierTagPattern(final Object[][] table, final int index) {
    if (index < 0 || index >= table.length) {
      return null;
    }
    return (Pattern) table[index][2];
  }

  private static String modifierTag(final Object[][] table, final int index) {
    if (index < 0 || index >= table.length) {
      return null;
    }
    return table[index].length > 3 ? (String) table[index][3] : (String) table[index][0];
  }

  private static final String COLD =
      Modifiers.modifierTag(Modifiers.doubleModifiers, Modifiers.COLD_RESISTANCE) + ": ";
  private static final String HOT =
      Modifiers.modifierTag(Modifiers.doubleModifiers, Modifiers.HOT_RESISTANCE) + ": ";
  private static final String SLEAZE =
      Modifiers.modifierTag(Modifiers.doubleModifiers, Modifiers.SLEAZE_RESISTANCE) + ": ";
  private static final String SPOOKY =
      Modifiers.modifierTag(Modifiers.doubleModifiers, Modifiers.SPOOKY_RESISTANCE) + ": ";
  private static final String STENCH =
      Modifiers.modifierTag(Modifiers.doubleModifiers, Modifiers.STENCH_RESISTANCE) + ": ";
  private static final String SLIME =
      Modifiers.modifierTag(Modifiers.doubleModifiers, Modifiers.SLIME_RESISTANCE) + ": ";
  private static final String SUPERCOLD =
      Modifiers.modifierTag(Modifiers.doubleModifiers, Modifiers.SUPERCOLD_RESISTANCE) + ": ";

  private static final String MOXIE =
      Modifiers.modifierTag(Modifiers.doubleModifiers, Modifiers.MOX) + ": ";
  private static final String MUSCLE =
      Modifiers.modifierTag(Modifiers.doubleModifiers, Modifiers.MUS) + ": ";
  private static final String MYSTICALITY =
      Modifiers.modifierTag(Modifiers.doubleModifiers, Modifiers.MYS) + ": ";

  private static final String MOXIE_PCT =
      Modifiers.modifierTag(Modifiers.doubleModifiers, Modifiers.MOX_PCT) + ": ";
  private static final String MUSCLE_PCT =
      Modifiers.modifierTag(Modifiers.doubleModifiers, Modifiers.MUS_PCT) + ": ";
  private static final String MYSTICALITY_PCT =
      Modifiers.modifierTag(Modifiers.doubleModifiers, Modifiers.MYS_PCT) + ": ";

  private static final String HP_TAG =
      Modifiers.modifierTag(Modifiers.doubleModifiers, Modifiers.HP) + ": ";
  private static final String MP_TAG =
      Modifiers.modifierTag(Modifiers.doubleModifiers, Modifiers.MP) + ": ";

  private static final String HP_REGEN_MIN_TAG =
      Modifiers.modifierTag(Modifiers.doubleModifiers, Modifiers.HP_REGEN_MIN) + ": ";
  private static final String HP_REGEN_MAX_TAG =
      Modifiers.modifierTag(Modifiers.doubleModifiers, Modifiers.HP_REGEN_MAX) + ": ";
  private static final String MP_REGEN_MIN_TAG =
      Modifiers.modifierTag(Modifiers.doubleModifiers, Modifiers.MP_REGEN_MIN) + ": ";
  private static final String MP_REGEN_MAX_TAG =
      Modifiers.modifierTag(Modifiers.doubleModifiers, Modifiers.MP_REGEN_MAX) + ": ";

  public static int elementalResistance(final Element element) {
    switch (element) {
      case COLD:
        return Modifiers.COLD_RESISTANCE;
      case HOT:
        return Modifiers.HOT_RESISTANCE;
      case SLEAZE:
        return Modifiers.SLEAZE_RESISTANCE;
      case SPOOKY:
        return Modifiers.SPOOKY_RESISTANCE;
      case STENCH:
        return Modifiers.STENCH_RESISTANCE;
      default:
        return -1;
    }
  }

  public static List<AdventureResult> getPotentialChanges(final int index) {
    ArrayList<AdventureResult> available = new ArrayList<AdventureResult>();

    for (String check : Modifiers.modifiersByName.keySet()) {
      String effectName = check.replace("Effect:", "");
      int effectId = EffectDatabase.getEffectId(effectName);

      if (effectId == -1) {
        continue;
      }

      Modifiers currentTest = Modifiers.getEffectModifiers(effectId);
      double value = currentTest.get(index);

      if (value == 0.0) {
        continue;
      }

      AdventureResult currentEffect = EffectPool.get(effectId);
      boolean hasEffect = KoLConstants.activeEffects.contains(currentEffect);

      if (value > 0.0 && !hasEffect) {
        available.add(currentEffect);
      } else if (value < 0.0 && hasEffect) {
        available.add(currentEffect);
      }
    }

    return available;
  }

  private static int findName(final Object[][] table, final String name) {
    for (int i = 0; i < table.length; ++i) {
      if (name.equalsIgnoreCase((String) table[i][0])) {
        return i;
      }
    }
    return -1;
  }

  public static final int findName(String name) {
    return Modifiers.findName(Modifiers.doubleModifiers, name);
  }

  public static final int findBooleanName(String name) {
    return Modifiers.findName(Modifiers.booleanModifiers, name);
  }

  private String name;
  public boolean variable;
  private final double[] doubles;
  private final int[] bitmaps;
  private final String[] strings;
  private ModifierExpression[] expressions;
  // These are used for Steely-Eyed Squint and so on
  private final double[] extras;

  public Modifiers() {
    this.variable = false;
    this.doubles = new double[Modifiers.DOUBLE_MODIFIERS];
    this.bitmaps = new int[Modifiers.BITMAP_MODIFIERS];
    this.strings = new String[Modifiers.STRING_MODIFIERS];
    this.extras = new double[Modifiers.DOUBLE_MODIFIERS];
    this.reset();
  }

  public Modifiers(Modifiers copy) {
    this();
    this.set(copy);
  }

  public Modifiers(String name, ModifierList mods) {
    this.name = name;
    this.variable = false;
    this.doubles = new double[Modifiers.DOUBLE_MODIFIERS];
    this.bitmaps = new int[Modifiers.BITMAP_MODIFIERS];
    this.strings = new String[Modifiers.STRING_MODIFIERS];
    this.extras = new double[Modifiers.DOUBLE_MODIFIERS];
    this.reset();

    for (Modifier m : mods) {
      this.add(m);
    }
  }

  public String getName() {
    return this.name;
  }

  public final void reset() {
    Arrays.fill(this.doubles, 0.0);
    Arrays.fill(this.bitmaps, 0);
    Arrays.fill(this.strings, "");
    this.expressions = null;
  }

  private double derivePrismaticDamage() {
    double damage = this.doubles[Modifiers.COLD_DAMAGE];
    damage = Math.min(damage, this.doubles[Modifiers.HOT_DAMAGE]);
    damage = Math.min(damage, this.doubles[Modifiers.SLEAZE_DAMAGE]);
    damage = Math.min(damage, this.doubles[Modifiers.SPOOKY_DAMAGE]);
    damage = Math.min(damage, this.doubles[Modifiers.STENCH_DAMAGE]);
    this.doubles[Modifiers.PRISMATIC_DAMAGE] = damage;
    return damage;
  }

  public double get(final int index) {
    if (index == Modifiers.PRISMATIC_DAMAGE) {
      return this.derivePrismaticDamage();
    }

    if (index < 0 || index >= this.doubles.length) {
      return 0.0;
    }

    return this.doubles[index];
  }

  public double get(final String name) {
    if (name.equals("Prismatic Damage")) {
      return this.derivePrismaticDamage();
    }

    int index = Modifiers.findName(Modifiers.doubleModifiers, name);
    if (index < 0 || index >= this.doubles.length) {
      index = Modifiers.findName(Modifiers.derivedModifiers, name);
      if (index < 0 || index >= Modifiers.DERIVED_MODIFIERS) {
        return this.getBitmap(name);
      }
      return this.predict()[index];
    }

    return this.doubles[index];
  }

  public int getRawBitmap(final int index) {
    if (index < 0 || index >= this.bitmaps.length) {
      return 0;
    }

    return this.bitmaps[index];
  }

  public int getRawBitmap(final String name) {
    int index = Modifiers.findName(Modifiers.bitmapModifiers, name);
    if (index < 0 || index >= this.bitmaps.length) {
      return 0;
    }

    return this.bitmaps[index];
  }

  public int getBitmap(final int index) {
    if (index < 0 || index >= this.bitmaps.length) {
      return 0;
    }

    int n = this.bitmaps[index];
    // Count the bits:
    if (n == 0) return 0;
    n = ((n & 0xAAAAAAAA) >>> 1) + (n & 0x55555555);
    n = ((n & 0xCCCCCCCC) >>> 2) + (n & 0x33333333);
    n = ((n & 0xF0F0F0F0) >>> 4) + (n & 0x0F0F0F0F);
    n = ((n & 0xFF00FF00) >>> 8) + (n & 0x00FF00FF);
    n = ((n & 0xFFFF0000) >>> 16) + (n & 0x0000FFFF);
    return n;
  }

  public int getBitmap(final String name) {
    return this.getBitmap(Modifiers.findName(Modifiers.bitmapModifiers, name));
  }

  public boolean getBoolean(final int index) {
    if (index < 0 || index >= Modifiers.BOOLEAN_MODIFIERS) {
      return false;
    }

    return ((this.bitmaps[0] >>> index) & 1) != 0;
  }

  public boolean getBoolean(final String name) {
    int index = Modifiers.findName(Modifiers.booleanModifiers, name);
    if (index < 0 || index >= Modifiers.BOOLEAN_MODIFIERS) {
      return false;
    }

    return ((this.bitmaps[0] >>> index) & 1) != 0;
  }

  public String getString(final int index) {
    if (index < 0 || index >= this.strings.length) {
      return "";
    }

    return this.strings[index];
  }

  public String getString(final String name) {
    // Can't cache this as expressions can be dependent on things
    // that can change within a session, like character level.
    if (name.equals("Evaluated Modifiers")) {
      return Modifiers.evaluateModifiers(this.name, this.strings[Modifiers.MODIFIERS]).toString();
    }

    int index = Modifiers.findName(Modifiers.stringModifiers, name);
    if (index < 0 || index >= this.strings.length) {
      return "";
    }

    return this.strings[index];
  }

  public double getExtra(final int index) {
    if (index < 0 || index >= this.extras.length) {
      return -9999.0;
    }
    return this.extras[index];
  }

  public double getExtra(final String name) {
    // extras uses the same indexes as doubles, so the same lookup will work
    int index = Modifiers.findName(Modifiers.doubleModifiers, name);
    if (index < 0 || index >= this.extras.length) {
      // For now, make it obvious that something went wrong
      return -9999.0;
    }

    return this.extras[index];
  }

  public boolean set(final int index, final double mod) {
    if (index < 0 || index >= this.doubles.length) {
      return false;
    }

    if (this.doubles[index] != mod) {
      this.doubles[index] = mod;
      return true;
    }
    return false;
  }

  public boolean set(final int index, final int mod) {
    if (index < 0 || index >= this.bitmaps.length) {
      return false;
    }

    if (this.bitmaps[index] != mod) {
      this.bitmaps[index] = mod;
      return true;
    }
    return false;
  }

  public boolean set(final int index, final boolean mod) {
    if (index < 0 || index >= Modifiers.BOOLEAN_MODIFIERS) {
      return false;
    }

    int mask = 1 << index;
    int val = mod ? mask : 0;
    if ((this.bitmaps[0] & mask) != val) {
      this.bitmaps[0] ^= mask;
      return true;
    }
    return false;
  }

  public boolean set(final int index, String mod) {
    if (index < 0 || index >= this.strings.length) {
      return false;
    }

    if (mod == null) {
      mod = "";
    }

    if (!mod.equals(this.strings[index])) {
      this.strings[index] = mod;
      return true;
    }
    return false;
  }

  public boolean set(final Modifiers mods) {
    if (mods == null) {
      return false;
    }

    boolean changed = false;
    this.name = mods.name;

    double[] copyDoubles = mods.doubles;
    for (int index = 0; index < this.doubles.length; ++index) {
      if (this.doubles[index] != copyDoubles[index]) {
        this.doubles[index] = copyDoubles[index];
        changed = true;
      }
    }

    int[] copyBitmaps = mods.bitmaps;
    for (int index = 0; index < this.bitmaps.length; ++index) {
      if (this.bitmaps[index] != copyBitmaps[index]) {
        this.bitmaps[index] = copyBitmaps[index];
        changed = true;
      }
    }

    String[] copyStrings = mods.strings;
    for (int index = 0; index < this.strings.length; ++index) {
      if (!this.strings[index].equals(copyStrings[index])) {
        this.strings[index] = copyStrings[index];
        changed = true;
      }
    }

    return changed;
  }

  public void add(final int index, final double mod, final String desc) {
    switch (index) {
      case COMBAT_RATE:
        // Combat Rate has diminishing returns beyond + or - 25%

        // Assume that all the sources of Combat Rate modifiers are of + or - 5%,
        // and start by obtaining the current value without the diminishing returns taken into
        // account
        double rate = this.doubles[index];
        double extra = Math.abs(rate) - 25.0;
        if (extra > 0.0) {
          rate = (25.0 + Math.ceil(extra) * 5.0) * (rate < 0.0 ? -1.0 : 1.0);
        }

        // Add mod and calculate the new value with the diminishing returns taken into account
        rate += mod;
        extra = Math.abs(rate) - 25.0;
        if (extra > 0.0) {
          rate = (25.0 + Math.floor(extra / 5.0)) * (rate < 0.0 ? -1.0 : 1.0);
        }
        this.doubles[index] = rate;
        break;
      case MANA_COST:
        // Total Mana Cost reduction cannot exceed 3
        this.doubles[index] += mod;
        if (this.doubles[index] < -3) {
          this.doubles[index] = -3;
        }
        break;
      case FAMILIAR_WEIGHT_PCT:
        // The three current sources of -wt% do not stack
        if (this.doubles[index] > mod) {
          this.doubles[index] = mod;
        }
        break;
      case MUS_LIMIT:
      case MYS_LIMIT:
      case MOX_LIMIT:
        // Only the lowest limiter applies
        if ((this.doubles[index] == 0.0 || this.doubles[index] > mod) && mod > 0.0) {
          this.doubles[index] = mod;
        }
        break;
      case ITEMDROP:
        String type = Modifiers.getTypeFromLookup(desc);
        if (type.equals("Ballroom")
            || type.equals("Bjorn")
            || type.equals("Effect")
            || type.equals("Item")
            || type.equals("Local Vote")
            || type.equals("Outfit")
            || type.equals("Path")
            || type.equals("Sign")
            || type.equals("Skill")
            || type.equals("Synergy")
            || type.equals("Throne")) {
          String name = Modifiers.getNameFromLookup(desc);
          if (!name.equals("Steely-Eyed Squint") && !name.equals("broken champagne bottle")) {
            this.extras[index] += mod;
          }
        }
        this.doubles[index] += mod;
        break;
      case INITIATIVE:
      case HOT_DAMAGE:
      case COLD_DAMAGE:
      case STENCH_DAMAGE:
      case SPOOKY_DAMAGE:
      case SLEAZE_DAMAGE:
      case HOT_SPELL_DAMAGE:
      case COLD_SPELL_DAMAGE:
      case STENCH_SPELL_DAMAGE:
      case SPOOKY_SPELL_DAMAGE:
      case SLEAZE_SPELL_DAMAGE:
        String name = Modifiers.getNameFromLookup(desc);
        if (!name.equals("Bendin' Hell") && !name.equals("Bow-Legged Swagger")) {
          this.extras[index] += mod;
        }
        this.doubles[index] += mod;
        break;
      case EXPERIENCE:
      case MUS_EXPERIENCE:
      case MYS_EXPERIENCE:
      case MOX_EXPERIENCE:
        name = Modifiers.getNameFromLookup(desc);
        if (!name.equals("makeshift garbage shirt")) {
          this.extras[index] += mod;
        }
        this.doubles[index] += mod;
        break;
      case FAMILIAR_ACTION_BONUS:
        this.doubles[index] = Math.min(100, this.doubles[index] + mod);
        break;
      default:
        this.doubles[index] += mod;
        break;
    }
  }

  public void add(final Modifiers mods) {
    if (mods == null) {
      return;
    }

    // Make sure the modifiers apply to current class
    AscensionClass ascensionClass = AscensionClass.nameToClass(mods.strings[Modifiers.CLASS]);
    if (ascensionClass != null && ascensionClass != KoLCharacter.getAscensionClass()) {
      return;
    }

    // Unarmed modifiers apply only if the character has no weapon or offhand
    boolean unarmed = mods.getBoolean(Modifiers.UNARMED);
    if (unarmed && !Modifiers.unarmed) {
      return;
    }

    String name = mods.name;

    // Add in the double modifiers

    double[] addition = mods.doubles;

    for (int i = 0; i < this.doubles.length; ++i) {
      if (addition[i] != 0.0) {
        if (i == Modifiers.ADVENTURES
            && (mods.bitmaps[0] & this.bitmaps[0] & (1 << Modifiers.NONSTACKABLE_WATCH)) != 0) {
          continue;
        }
        this.add(i, addition[i], name);
      }
    }

    // Add in string modifiers as appropriate.

    String val;
    val = mods.strings[Modifiers.EQUALIZE];
    if (!val.equals("") && this.strings[Modifiers.EQUALIZE].equals("")) {
      this.strings[Modifiers.EQUALIZE] = val;
    }
    val = mods.strings[Modifiers.INTRINSIC_EFFECT];
    if (!val.equals("")) {
      String prev = this.strings[INTRINSIC_EFFECT];
      if (prev.equals("")) {
        this.strings[Modifiers.INTRINSIC_EFFECT] = val;
      } else {
        this.strings[Modifiers.INTRINSIC_EFFECT] = prev + "\t" + val;
      }
    }
    val = mods.strings[Modifiers.STAT_TUNING];
    if (!val.equals("")) {
      this.strings[Modifiers.STAT_TUNING] = val;
    }
    val = mods.strings[Modifiers.EQUALIZE_MUSCLE];
    if (!val.equals("")) {
      this.strings[Modifiers.EQUALIZE_MUSCLE] = val;
    }
    val = mods.strings[Modifiers.EQUALIZE_MYST];
    if (!val.equals("")) {
      this.strings[Modifiers.EQUALIZE_MYST] = val;
    }
    val = mods.strings[Modifiers.EQUALIZE_MOXIE];
    if (!val.equals("")) {
      this.strings[Modifiers.EQUALIZE_MOXIE] = val;
    }

    // OR in the bitmap modifiers (including all the boolean modifiers)
    this.bitmaps[Modifiers.MUTEX_VIOLATIONS] |=
        this.bitmaps[Modifiers.MUTEX] & mods.bitmaps[Modifiers.MUTEX];
    for (int i = 0; i < this.bitmaps.length; ++i) {
      this.bitmaps[i] |= mods.bitmaps[i];
    }
  }

  public boolean add(final Modifier mod) {
    if (mod == null) {
      return false;
    }

    Integer index = modifierIndicesByName.get(mod.getName());
    if (index == null) {
      return false;
    }
    if (index < DOUBLE_MODIFIERS) {
      return this.set(index, Double.parseDouble(mod.getValue()));
    }

    index -= DOUBLE_MODIFIERS;
    if (index < BITMAP_MODIFIERS) {
      return this.set(index, Integer.parseInt(mod.getValue()));
    }

    index -= BITMAP_MODIFIERS;
    if (index < BOOLEAN_MODIFIERS) {
      return this.set(index, mod.getValue().equals("true"));
    }

    index -= BOOLEAN_MODIFIERS;
    return this.set(index, mod.getValue());
  }

  public static final Modifiers getItemModifiers(final int id) {
    if (id <= 0) {
      return null;
    }
    String name = "[" + id + "]";
    return Modifiers.getModifiers("Item", name);
  }

  /**
   * Get item modifiers if the item is to be equipped on Disembodied Hand or Left-Hand Man
   *
   * @param id Item id
   * @return Returns modifiers for item excluding some that just do not apply
   */
  public static final Modifiers getItemModifiersInFamiliarSlot(final int id) {
    Modifiers mods = new Modifiers(getItemModifiers(id));

    if (mods != null) {
      mods.set(Modifiers.SLIME_HATES_IT, 0.0f);
      mods.set(Modifiers.BRIMSTONE, 0);
      mods.set(Modifiers.CLOATHING, 0);
      mods.set(Modifiers.SYNERGETIC, 0);
      mods.set(Modifiers.MOXIE_MAY_CONTROL_MP, false);
      mods.set(Modifiers.MOXIE_CONTROLS_MP, false);
    }

    return mods;
  }

  public static final Modifiers getEffectModifiers(final int id) {
    if (id <= 0) {
      return null;
    }
    if (KoLCharacter.inGLover()) {
      String effectName = EffectDatabase.getEffectName(id);
      if (!KoLCharacter.hasGs(effectName)) {
        return null;
      }
    }
    String name = "[" + id + "]";
    return Modifiers.getModifiers("Effect", name);
  }

  public static final Modifiers getModifiers(String type, final String name) {
    String changeType = null;
    if (name == null || name.equals("")) {
      return null;
    }

    if (type.equals("Bjorn")) {
      changeType = type;
      type = "Throne";
    }

    String lookup = Modifiers.getLookupName(type, name);
    Object modifier = Modifiers.modifiersByName.get(lookup);

    if (modifier == null) {
      return null;
    }

    if (modifier instanceof Modifiers) {
      Modifiers mods = (Modifiers) modifier;
      if (mods.variable) {
        mods.override(lookup);
        if (changeType != null) {
          mods.name = changeType + ":" + name;
        }
      }
      return mods;
    }

    if (!(modifier instanceof String)) {
      return null;
    }

    Modifiers newMods = Modifiers.parseModifiers(lookup, (String) modifier);

    if (changeType != null) {
      newMods.name = changeType + ":" + name;
    }

    newMods.variable = newMods.override(lookup) || type.equals("Loc") || type.equals("Zone");

    Modifiers.modifiersByName.put(lookup, newMods);

    return newMods;
  }

  public static final Modifiers parseModifiers(final String lookup, final String string) {
    Modifiers newMods = new Modifiers();
    double[] newDoubles = newMods.doubles;
    int[] newBitmaps = newMods.bitmaps;
    String[] newStrings = newMods.strings;

    newMods.name = lookup;
    String name = Modifiers.getNameFromLookup(lookup);

    for (int i = 0; i < newDoubles.length; ++i) {
      Pattern pattern = Modifiers.modifierTagPattern(Modifiers.doubleModifiers, i);
      if (pattern == null) {
        continue;
      }

      Matcher matcher = pattern.matcher(string);
      if (!matcher.find()) {
        continue;
      }

      if (matcher.group(1) != null) {
        newDoubles[i] = Double.parseDouble(matcher.group(1));
      } else {
        if (newMods.expressions == null) {
          newMods.expressions = new ModifierExpression[Modifiers.DOUBLE_MODIFIERS];
        }
        newMods.expressions[i] = ModifierExpression.getInstance(matcher.group(2), lookup);
      }
    }

    for (int i = 0; i < newBitmaps.length; ++i) {
      Pattern pattern = Modifiers.modifierTagPattern(Modifiers.bitmapModifiers, i);
      if (pattern == null) {
        continue;
      }

      Matcher matcher = pattern.matcher(string);
      if (!matcher.find()) {
        continue;
      }
      int bitcount = 1;
      if (matcher.groupCount() > 0) {
        bitcount = StringUtilities.parseInt(matcher.group(1));
      }
      int mask = Modifiers.bitmapMasks[i];
      switch (bitcount) {
        case 1:
          Modifiers.bitmapMasks[i] <<= 1;
          break;
        case 2:
          mask |= mask << 1;
          Modifiers.bitmapMasks[i] <<= 2;
          break;
        default:
          KoLmafia.updateDisplay("ERROR: invalid count for bitmap modifier in " + lookup);
          continue;
      }
      if (Modifiers.bitmapMasks[i] == 0) {
        KoLmafia.updateDisplay(
            "ERROR: too many sources for bitmap modifier "
                + Modifiers.modifierName(Modifiers.bitmapModifiers, i)
                + ", consider using longs.");
      }

      newBitmaps[i] |= mask;
    }

    for (int i = 0; i < Modifiers.BOOLEAN_MODIFIERS; ++i) {
      Pattern pattern = Modifiers.modifierTagPattern(Modifiers.booleanModifiers, i);
      if (pattern == null) {
        continue;
      }

      Matcher matcher = pattern.matcher(string);
      if (!matcher.find()) {
        continue;
      }

      newBitmaps[0] |= 1 << i;
    }

    for (int i = 0; i < newStrings.length; ++i) {
      Pattern pattern = Modifiers.modifierTagPattern(Modifiers.stringModifiers, i);
      if (pattern == null) {
        continue;
      }

      Matcher matcher = pattern.matcher(string);
      if (!matcher.find()) {
        continue;
      }

      String key = Modifiers.modifierName(Modifiers.stringModifiers, i);
      String value = matcher.group(1);

      if (key.equals("Class")) {
        value = Modifiers.depluralizeClassName(value);
      }

      newStrings[i] = value;
    }

    newStrings[Modifiers.MODIFIERS] = string;

    return newMods;
  }

  private static final String[][] classStrings = {
    {
      AscensionClass.SEAL_CLUBBER.getName(), "Seal Clubbers", "Seal&nbsp;Clubbers",
    },
    {
      AscensionClass.TURTLE_TAMER.getName(), "Turtle Tamers", "Turtle&nbsp;Tamers",
    },
    {
      AscensionClass.PASTAMANCER.getName(), "Pastamancers",
    },
    {
      AscensionClass.SAUCEROR.getName(), "Saucerors",
    },
    {
      AscensionClass.DISCO_BANDIT.getName(), "Disco Bandits", "Disco&nbsp;Bandits",
    },
    {
      AscensionClass.ACCORDION_THIEF.getName(), "Accordion Thieves", "Accordion&nbsp;Thieves",
    },
  };

  private static String depluralizeClassName(final String string) {
    for (String[] results : Modifiers.classStrings) {
      String result = results[0];
      for (String candidate : results) {
        if (candidate.equals(string)) {
          return result;
        }
      }
    }
    return string;
  }

  public static class Modifier {
    private final String name;
    private String value;

    public Modifier(final String name, final String value) {
      this.name = name;
      this.value = value;
    }

    public String getName() {
      return this.name;
    }

    public String getValue() {
      return this.value;
    }

    public void setValue(final String value) {
      this.value = value;
    }

    public void eval(final String lookup) {
      if (this.value == null) {
        return;
      }

      int lb = this.value.indexOf("[");
      if (lb == -1) {
        return;
      }

      int rb = this.value.indexOf("]");
      if (rb == -1) {
        return;
      }

      ModifierExpression expr = new ModifierExpression(this.value.substring(lb + 1, rb), lookup);
      if (expr.hasErrors()) {
        return;
      }

      int val = (int) expr.eval();
      this.value = (val > 0 ? "+" : "") + val;
    }

    public void toString(final StringBuilder buffer) {
      buffer.append(name);
      if (value != null) {
        buffer.append(": ");
        buffer.append(value);
      }
    }

    @Override
    public String toString() {
      StringBuilder buffer = new StringBuilder();
      this.toString(buffer);
      return buffer.toString();
    }
  }

  public static class ModifierList implements Iterable<Modifier> {
    private final LinkedList<Modifier> list;

    public ModifierList() {
      this.list = new LinkedList<Modifier>();
    }

    @Override
    public Iterator<Modifier> iterator() {
      return this.list.iterator();
    }

    public void clear() {
      this.list.clear();
    }

    public int size() {
      return this.list.size();
    }

    public void addAll(final ModifierList list) {
      this.list.addAll(list.list);
    }

    public void addModifier(final Modifier modifier) {
      this.list.add(modifier);
    }

    public void addModifier(final String name, final String value) {
      this.list.add(new Modifier(name, value));
    }

    public void addToModifier(final Modifier modifier) {
      String name = modifier.getName();
      String current = this.getModifierValue(name);
      if (current == null) {
        this.list.add(modifier);
      } else {
        // We can only add to numeric values
        String value = modifier.getValue();
        if (StringUtilities.isNumeric(current) && StringUtilities.isNumeric(value)) {
          int newValue = Integer.parseInt(current) + Integer.parseInt(value);
          this.removeModifier(name);
          this.list.add(new Modifier(name, String.valueOf(newValue)));
        }
      }
    }

    public void addToModifier(final String name, final String value) {
      String current = this.getModifierValue(name);
      if (current == null) {
        this.list.add(new Modifier(name, value));
      } else {
        // We can only add to numeric values
        if (StringUtilities.isNumeric(current) && StringUtilities.isNumeric(value)) {
          int newValue = Integer.parseInt(current) + Integer.parseInt(value);
          this.removeModifier(name);
          this.list.add(new Modifier(name, String.valueOf(newValue)));
        }
      }
    }

    public boolean containsModifier(final String name) {
      for (Modifier modifier : this.list) {
        if (name.equals(modifier.name)) {
          return true;
        }
      }
      return false;
    }

    public String getModifierValue(final String name) {
      for (Modifier modifier : this.list) {
        if (name.equals(modifier.name)) {
          return modifier.value;
        }
      }
      return null;
    }

    public Modifier removeModifier(final String name) {
      Iterator<Modifier> iterator = this.iterator();
      while (iterator.hasNext()) {
        Modifier modifier = iterator.next();
        if (name.equals(modifier.name)) {
          iterator.remove();
          return modifier;
        }
      }
      return null;
    }

    @Override
    public String toString() {
      StringBuilder buffer = new StringBuilder();
      for (Modifier modifier : this.list) {
        if (buffer.length() > 0) {
          buffer.append(", ");
        }

        modifier.toString(buffer);
      }
      return buffer.toString();
    }
  }

  public static final ModifierList getModifierList(final String type, final int id) {
    String name = "[" + id + "]";
    return Modifiers.getModifierList(type, name);
  }

  public static final ModifierList getModifierList(final String type, final String name) {
    Modifiers mods = Modifiers.getModifiers(type, name);
    if (mods == null) {
      return new ModifierList();
    }

    return Modifiers.splitModifiers(mods.getString("Modifiers"));
  }

  public static final ModifierList splitModifiers(String modifiers) {
    ModifierList list = new ModifierList();

    // Iterate over string, pulling off modifiers
    while (modifiers != null) {
      // Moxie: +5, Muscle: +5, Mysticality: +5, Familiar Effect: "1xPotato, 3xGhuol, cap 18"
      int comma = modifiers.indexOf(",");
      if (comma != -1) {
        int bracket1 = modifiers.indexOf("[");
        if (bracket1 != -1 && bracket1 < comma) {
          int bracket2 = modifiers.indexOf("]", bracket1 + 1);
          if (bracket2 != -1) {
            comma = modifiers.indexOf(",", bracket2 + 1);
          } else {
            // bogus: no close bracket
            comma = -1;
          }
        } else {
          int quote1 = modifiers.indexOf("\"");
          if (quote1 != -1 && quote1 < comma) {
            int quote2 = modifiers.indexOf("\"", quote1 + 1);
            if (quote2 != -1) {
              comma = modifiers.indexOf(",", quote2 + 1);
            } else {
              // bogus: no close quote
              comma = -1;
            }
          }
        }
      }

      String string;
      if (comma == -1) {
        string = modifiers;
        modifiers = null;
      } else {
        string = modifiers.substring(0, comma).trim();
        modifiers = modifiers.substring(comma + 1).trim();
      }

      String key, value;

      // Every pattern for a modifier with a value separates
      // the key with a colon and a single space. Therefore,
      // split on precisely that string and trim neither the
      // key nor the value.
      int colon = string.indexOf(": ");
      if (colon == -1) {
        key = string;
        value = null;
      } else {
        key = string.substring(0, colon);
        value = string.substring(colon + 2);
      }

      list.addModifier(key, value);
    }

    return list;
  }

  public static final ModifierList evaluateModifiers(final String lookup, final String modifiers) {
    // Nothing to do if no expressions
    if (!modifiers.contains("[")) {
      return Modifiers.splitModifiers(modifiers);
    }

    // Otherwise, break apart the string and rebuild it with all
    // expressions evaluated.
    ModifierList list = Modifiers.splitModifiers(modifiers);
    for (Modifier modifier : list) {
      // Evaluate the modifier expression
      modifier.eval(lookup);
    }

    return list;
  }

  public static final String trimModifiers(final String modifiers, final String remove) {
    ModifierList list = Modifiers.splitModifiers(modifiers);
    list.removeModifier(remove);
    return list.toString();
  }

  private boolean override(final String lookup) {
    if (this.expressions != null) {
      for (int i = 0; i < this.expressions.length; ++i) {
        ModifierExpression expr = this.expressions[i];
        if (expr != null) {
          this.doubles[i] = expr.eval();
        }
      }
    }

    // If the object does not require hard-coding, we're done
    if (!this.getBoolean(Modifiers.VARIABLE)) {
      return this.expressions != null;
    }

    String name = Modifiers.getNameFromLookup(lookup);
    String type = Modifiers.getTypeFromLookup(lookup);

    if (type.equals("Item")) {
      int itemId = ItemDatabase.getItemId(name);

      switch (itemId) {
        case ItemPool.TUESDAYS_RUBY:
          {
            // Reset to defaults

            this.set(Modifiers.MEATDROP, 0.0);
            this.set(Modifiers.ITEMDROP, 0.0);
            this.set(Modifiers.MOX_PCT, 0.0);
            this.set(Modifiers.MUS_PCT, 0.0);
            this.set(Modifiers.MYS_PCT, 0.0);
            this.set(Modifiers.HP_REGEN_MIN, 0.0);
            this.set(Modifiers.HP_REGEN_MAX, 0.0);
            this.set(Modifiers.MP_REGEN_MIN, 0.0);
            this.set(Modifiers.MP_REGEN_MAX, 0.0);

            // Set modifiers depending on what KoL day of the week it is

            Calendar date = Calendar.getInstance(TimeZone.getTimeZone("GMT-0700"));
            switch (date.get(Calendar.DAY_OF_WEEK)) {
              case Calendar.SUNDAY:
                // +5% Meat from Monsters
                this.set(Modifiers.MEATDROP, 5.0);
                break;
              case Calendar.MONDAY:
                // Muscle +5%
                this.set(Modifiers.MUS_PCT, 5.0);
                break;
              case Calendar.TUESDAY:
                // Regenerate 3-7 MP per adventure
                this.set(Modifiers.MP_REGEN_MIN, 3.0);
                this.set(Modifiers.MP_REGEN_MAX, 7.0);
                break;
              case Calendar.WEDNESDAY:
                // +5% Mysticality
                this.set(Modifiers.MYS_PCT, 5.0);
                break;
              case Calendar.THURSDAY:
                // +5% Item Drops from Monsters
                this.set(Modifiers.ITEMDROP, 5.0);
                break;
              case Calendar.FRIDAY:
                // +5% Moxie
                this.set(Modifiers.MOX_PCT, 5.0);
                break;
              case Calendar.SATURDAY:
                // Regenerate 3-7 HP per adventure
                this.set(Modifiers.HP_REGEN_MIN, 3.0);
                this.set(Modifiers.HP_REGEN_MAX, 7.0);
                break;
            }
            return true;
          }

        case ItemPool.UNCLE_HOBO_BEARD:
        case ItemPool.GINGERBEARD:
          {
            Calendar date = Calendar.getInstance(TimeZone.getTimeZone("GMT-0700"));
            double adventures = date.get(Calendar.MONTH) == Calendar.DECEMBER ? 9.0 : 6.0;
            this.set(Modifiers.ADVENTURES, adventures);
            return true;
          }

        case ItemPool.CRIMBO_CANDLE:
          {
            Calendar date = Calendar.getInstance(TimeZone.getTimeZone("GMT-0700"));
            double adventures = date.get(Calendar.MONTH) == Calendar.DECEMBER ? 3.0 : 0.0;
            this.set(Modifiers.ADVENTURES, adventures);
            return true;
          }

        case ItemPool.TIME_TWITCHING_TOOLBELT:
          {
            this.set(Modifiers.FREE_PULL, Preferences.getBoolean("timeTowerAvailable"));
            return true;
          }

        case ItemPool.PANTSGIVING:
          {
            this.set(Modifiers.DROPS_ITEMS, Preferences.getInteger("_pantsgivingCrumbs") < 10);
            return true;
          }

        case ItemPool.PATRIOT_SHIELD:
          {
            // Muscle classes
            this.set(Modifiers.HP_REGEN_MIN, 0.0);
            this.set(Modifiers.HP_REGEN_MAX, 0.0);
            // Seal clubber
            this.set(Modifiers.WEAPON_DAMAGE, 0.0);
            this.set(Modifiers.DAMAGE_REDUCTION, 0.0);
            // Turtle Tamer
            this.set(Modifiers.FAMILIAR_WEIGHT, 0.0);
            // Disco Bandit
            this.set(Modifiers.RANGED_DAMAGE, 0.0);
            // Accordion Thief
            this.set(Modifiers.FOUR_SONGS, false);
            // Mysticality classes
            this.set(Modifiers.MP_REGEN_MIN, 0.0);
            this.set(Modifiers.MP_REGEN_MAX, 0.0);
            // Pastamancer
            this.set(Modifiers.COMBAT_MANA_COST, 0.0);
            // Sauceror
            this.set(Modifiers.SPELL_DAMAGE, 0.0);

            // Set modifiers depending on Character class
            AscensionClass ascensionClass = KoLCharacter.getAscensionClass();
            if (ascensionClass != null) {
              switch (ascensionClass) {
                case SEAL_CLUBBER:
                case ZOMBIE_MASTER:
                case ED:
                case COWPUNCHER:
                case BEANSLINGER:
                case SNAKE_OILER:
                  this.set(Modifiers.HP_REGEN_MIN, 10.0);
                  this.set(Modifiers.HP_REGEN_MAX, 12.0);
                  this.set(Modifiers.WEAPON_DAMAGE, 15.0);
                  this.set(Modifiers.DAMAGE_REDUCTION, 1.0);
                  break;
                case TURTLE_TAMER:
                  this.set(Modifiers.HP_REGEN_MIN, 10.0);
                  this.set(Modifiers.HP_REGEN_MAX, 12.0);
                  this.set(Modifiers.FAMILIAR_WEIGHT, 5.0);
                  break;
                case DISCO_BANDIT:
                case AVATAR_OF_SNEAKY_PETE:
                  this.set(Modifiers.RANGED_DAMAGE, 20.0);
                  break;
                case ACCORDION_THIEF:
                  this.set(Modifiers.FOUR_SONGS, true);
                  break;
                case PASTAMANCER:
                  this.set(Modifiers.MP_REGEN_MIN, 5.0);
                  this.set(Modifiers.MP_REGEN_MAX, 6.0);
                  this.set(Modifiers.COMBAT_MANA_COST, -3.0);
                  break;
                case SAUCEROR:
                case AVATAR_OF_JARLSBERG:
                  this.set(Modifiers.MP_REGEN_MIN, 5.0);
                  this.set(Modifiers.MP_REGEN_MAX, 6.0);
                  this.set(Modifiers.SPELL_DAMAGE, 20.0);
                  break;
              }
            }
            return true;
          }
      }
    } else if (type.equals("Skill")) {
      if (name.equals("Ferocity")) {
        if (KoLCharacter.isVampyre()) {
          this.set(Modifiers.HP, -10.0);
        } else if (KoLCharacter.isAvatarOfBoris()) {
          this.set(Modifiers.CRITICAL_PCT, 25.0);
        }
        return true;
      }
    } else if (type.equals("Throne")) {
      if (name.equals("Adventurous Spelunker")) {
        this.set(Modifiers.DROPS_ITEMS, Preferences.getInteger("_oreDropsCrown") < 6);
        return true;
      }
      if (name.equals("Garbage Fire")) {
        this.set(Modifiers.DROPS_ITEMS, Preferences.getInteger("_garbageFireDropsCrown") < 3);
        return true;
      }
      if (name.equals("Grimstone Golem")) {
        this.set(Modifiers.DROPS_ITEMS, Preferences.getInteger("_grimstoneMaskDropsCrown") < 1);
        return true;
      }
      if (name.equals("Grim Brother")) {
        this.set(Modifiers.DROPS_ITEMS, Preferences.getInteger("_grimFairyTaleDropsCrown") < 2);
        return true;
      }
      if (name.equals("Machine Elf")) {
        this.set(Modifiers.DROPS_ITEMS, Preferences.getInteger("_abstractionDropsCrown") < 25);
        return true;
      }
      if (name.equals("Optimistic Candle")) {
        this.set(Modifiers.DROPS_ITEMS, Preferences.getInteger("_optimisticCandleDropsCrown") < 3);
        return true;
      }
      if (name.equals("Trick-or-Treating Tot")) {
        this.set(Modifiers.DROPS_ITEMS, Preferences.getInteger("_hoardedCandyDropsCrown") < 3);
        return true;
      }
      if (name.equals("Twitching Space Critter")) {
        this.set(Modifiers.DROPS_ITEMS, Preferences.getInteger("_spaceFurDropsCrown") < 1);
        return true;
      }
    }
    return false;
  }

  public static final double getNumericModifier(final String type, final int id, final String mod) {
    String name = "[" + id + "]";
    return Modifiers.getNumericModifier(type, name, mod);
  }

  public static final double getNumericModifier(
      final String type, final String name, final String mod) {
    Modifiers mods = Modifiers.getModifiers(type, name);
    if (mods == null) {
      return 0.0;
    }
    return mods.get(mod);
  }

  public static final double getNumericModifier(final FamiliarData fam, final String mod) {
    return Modifiers.getNumericModifier(fam, mod, fam.getModifiedWeight(false), fam.getItem());
  }

  public static final double getNumericModifier(
      final FamiliarData fam,
      final String mod,
      final int passedWeight,
      final AdventureResult item) {
    int familiarId = fam != null ? fam.getId() : -1;
    if (familiarId == -1) {
      return 0.0;
    }

    Modifiers.setFamiliar(fam);

    int weight = passedWeight;

    Modifiers tempMods = new Modifiers();

    // Mad Hatrack ... hats do not give their normal modifiers
    // Fancypants Scarecrow ... pants do not give their normal modifiers
    int itemId = item.getItemId();
    int type = ItemDatabase.getConsumptionType(itemId);
    if ((familiarId != FamiliarPool.HATRACK || type != KoLConstants.EQUIP_HAT)
        && (familiarId != FamiliarPool.SCARECROW || type != KoLConstants.EQUIP_PANTS)) {
      // Add in all the modifiers bestowed by this item
      tempMods.add(Modifiers.getItemModifiers(itemId));

      // Apply weight modifiers right now
      weight += (int) tempMods.get(Modifiers.FAMILIAR_WEIGHT);
      weight += (int) tempMods.get(Modifiers.HIDDEN_FAMILIAR_WEIGHT);
      weight += (fam.getFeasted() ? 10 : 0);
      double percent = tempMods.get(Modifiers.FAMILIAR_WEIGHT_PCT) / 100.0;
      if (percent != 0.0) {
        weight = (int) Math.floor(weight + weight * percent);
      }
    }

    tempMods.lookupFamiliarModifiers(fam, weight, item);

    return tempMods.get(mod);
  }

  public static final boolean getBooleanModifier(
      final String type, final int id, final String mod) {
    String name = "[" + id + "]";
    return Modifiers.getBooleanModifier(type, name, mod);
  }

  public static final boolean getBooleanModifier(
      final String type, final String name, final String mod) {
    Modifiers mods = Modifiers.getModifiers(type, name);
    if (mods == null) {
      return false;
    }
    return mods.getBoolean(mod);
  }

  public static final String getStringModifier(final String type, final int id, final String mod) {
    String name = "[" + id + "]";
    return Modifiers.getStringModifier(type, name, mod);
  }

  public static final String getStringModifier(
      final String type, final String name, final String mod) {
    Modifiers mods = Modifiers.getModifiers(type, name);
    if (mods == null) {
      return "";
    }
    return mods.getString(mod);
  }

  public void applyPassiveModifiers() {
    // You'd think this could be done at class initialization time,
    // but no: the SkillDatabase depends on the Mana Cost
    // modifier being set.

    if (Modifiers.passiveSkills.isEmpty()) {
      for (String lookup : Modifiers.modifiersByName.keySet()) {
        if (!Modifiers.getTypeFromLookup(lookup).equals("Skill")) {
          continue;
        }
        String skill = Modifiers.getNameFromLookup(lookup);
        if (!SkillDatabase.contains(skill)) {
          continue;
        }

        if (SkillDatabase.isPassive(SkillDatabase.getSkillId(skill))) {
          Modifiers.passiveSkills.add(UseSkillRequest.getUnmodifiedInstance(skill));
        }
      }
    }

    for (int i = Modifiers.passiveSkills.size() - 1; i >= 0; --i) {
      UseSkillRequest skill = Modifiers.passiveSkills.get(i);
      if (KoLCharacter.hasSkill(skill)) {
        String name = skill.getSkillName();

        // G-Lover shows passives on the char sheet,
        // even though they are ineffective.
        if (KoLCharacter.inGLover() && !KoLCharacter.hasGs(name)) {
          continue;
        }

        this.add(Modifiers.getModifiers("Skill", name));
      }
    }

    if (KoLCharacter.getFamiliar().getId() == FamiliarPool.DODECAPEDE
        && KoLCharacter.hasAmphibianSympathy()) {
      this.add(Modifiers.FAMILIAR_WEIGHT, -10, "Familiar:dodecapede sympathy");
    }
  }

  public final void applyFloristModifiers() {
    if (!FloristRequest.haveFlorist()) {
      return;
    }

    if (Modifiers.currentLocation == null) {
      return;
    }

    List<Florist> plants = FloristRequest.getPlants(Modifiers.currentLocation);
    if (plants == null) {
      return;
    }

    for (Florist plant : plants) {
      this.add(Modifiers.getModifiers("Florist", plant.toString()));
    }
  }

  public void applySynergies() {
    int synergetic = this.getRawBitmap(Modifiers.SYNERGETIC);
    if (synergetic == 0) return; // nothing possible
    for (Entry<String, Integer> entry : Modifiers.synergies.entrySet()) {
      String name = entry.getKey();
      int mask = entry.getValue().intValue();
      if ((synergetic & mask) == mask) {
        this.add(Modifiers.getModifiers("Synergy", name));
      }
    }
  }

  // Returned set yields bitmaps keyed by names
  public static Set<Entry<String, Integer>> getSynergies() {
    return Collections.unmodifiableSet(Modifiers.synergies.entrySet());
  }

  private static final AdventureResult somePigs = EffectPool.get(EffectPool.SOME_PIGS);

  public void applyFamiliarModifiers(final FamiliarData familiar, AdventureResult famItem) {
    if (KoLConstants.activeEffects.contains(Modifiers.somePigs)) {
      // Under the effect of SOME PIGS, familiar gives no modifiers
      return;
    }

    int weight = familiar.getUncappedWeight();

    weight += (int) this.get(Modifiers.FAMILIAR_WEIGHT);
    weight += (int) this.get(Modifiers.HIDDEN_FAMILIAR_WEIGHT);
    weight += (familiar.getFeasted() ? 10 : 0);

    double percent = this.get(Modifiers.FAMILIAR_WEIGHT_PCT) / 100.0;
    if (percent != 0.0) {
      weight = (int) Math.floor(weight + weight * percent);
    }

    weight = Math.max(1, weight);
    this.lookupFamiliarModifiers(familiar, weight, famItem);
  }

  public void lookupFamiliarModifiers(
      final FamiliarData familiar, int weight, final AdventureResult famItem) {
    int familiarId = familiar.getId();
    weight = Math.max(1, weight);
    Modifiers.currentWeight = weight;

    String race = familiar.getRace();

    // Comma Chameleon acts as if it was something else
    if (familiarId == FamiliarPool.CHAMELEON) {
      String newRace = Preferences.getString("commaFamiliar");
      if (newRace != null && !newRace.equals("")) {
        race = newRace;
        familiarId = FamiliarDatabase.getFamiliarId(race);
      }
    }
    this.add(Modifiers.getModifiers("Familiar", race));
    if (famItem != null) {
      // "fameq" modifiers are generated when "Familiar Effect" is parsed
      // from modifiers.txt
      this.add(Modifiers.getModifiers("FamEq", famItem.getName()));
    }

    int cap = (int) this.get(Modifiers.FAMILIAR_WEIGHT_CAP);
    int cappedWeight = (cap == 0) ? weight : Math.min(weight, cap);

    double effective = cappedWeight * this.get(Modifiers.VOLLEYBALL_WEIGHT);
    if (effective == 0.0 && FamiliarDatabase.isVolleyType(familiarId)) {
      effective = weight;
    }
    if (effective != 0.0) {
      double factor = this.get(Modifiers.VOLLEYBALL_EFFECTIVENESS);
      // The 0->1 factor for generic familiars conflicts with the JitB
      if (factor == 0.0 && familiarId != FamiliarPool.JACK_IN_THE_BOX) factor = 1.0;
      factor = factor * (2 + effective / 5);
      double tuning;
      if ((tuning = this.get(Modifiers.FAMILIAR_TUNING_MUSCLE)) > 0) {
        double mainstatFactor = tuning / 100;
        double offstatFactor = (1 - mainstatFactor) / 2;
        this.add(Modifiers.MUS_EXPERIENCE, factor * mainstatFactor, "Tuned Volleyball:" + race);
        this.add(Modifiers.MYS_EXPERIENCE, factor * offstatFactor, "Tuned Volleyball:" + race);
        this.add(Modifiers.MOX_EXPERIENCE, factor * offstatFactor, "Tuned Volleyball:" + race);
      } else if ((tuning = this.get(Modifiers.FAMILIAR_TUNING_MYSTICALITY)) > 0) {
        double mainstatFactor = tuning / 100;
        double offstatFactor = (1 - mainstatFactor) / 2;
        this.add(Modifiers.MUS_EXPERIENCE, factor * offstatFactor, "Tuned Volleyball:" + race);
        this.add(Modifiers.MYS_EXPERIENCE, factor * mainstatFactor, "Tuned Volleyball:" + race);
        this.add(Modifiers.MOX_EXPERIENCE, factor * offstatFactor, "Tuned Volleyball:" + race);
      } else if ((tuning = this.get(Modifiers.FAMILIAR_TUNING_MOXIE)) > 0) {
        double mainstatFactor = tuning / 100;
        double offstatFactor = (1 - mainstatFactor) / 2;
        this.add(Modifiers.MUS_EXPERIENCE, factor * offstatFactor, "Tuned Volleyball:" + race);
        this.add(Modifiers.MYS_EXPERIENCE, factor * offstatFactor, "Tuned Volleyball:" + race);
        this.add(Modifiers.MOX_EXPERIENCE, factor * mainstatFactor, "Tuned Volleyball:" + race);
      } else {
        this.add(Modifiers.EXPERIENCE, factor, "Volleyball:" + race);
      }
    }

    effective = cappedWeight * this.get(Modifiers.SOMBRERO_WEIGHT);
    if (effective == 0.0 && FamiliarDatabase.isSombreroType(familiarId)) {
      effective = weight;
    }
    effective += this.get(Modifiers.SOMBRERO_BONUS);
    if (effective != 0.0) {
      double factor = this.get(Modifiers.SOMBRERO_EFFECTIVENESS);
      if (factor == 0.0) factor = 1.0;
      // currentML is always >= 4, so we don't need to check for negatives
      int maxStats = 230;
      this.add(
          Modifiers.EXPERIENCE,
          Math.min(
              Math.max(factor * (Modifiers.currentML / 4) * (0.1 + 0.005 * effective), 1),
              maxStats),
          "Familiar:" + race);
    }

    effective = cappedWeight * this.get(Modifiers.LEPRECHAUN_WEIGHT);
    if (effective == 0.0 && FamiliarDatabase.isMeatDropType(familiarId)) {
      effective = weight;
    }
    if (effective != 0.0) {
      double factor = this.get(Modifiers.LEPRECHAUN_EFFECTIVENESS);
      if (factor == 0.0) factor = 1.0;
      this.add(
          Modifiers.MEATDROP,
          factor * (Math.sqrt(220 * effective) + 2 * effective - 6),
          "Familiar:" + race);
    }

    effective = cappedWeight * this.get(Modifiers.FAIRY_WEIGHT);
    if (effective == 0.0 && FamiliarDatabase.isFairyType(familiarId)) {
      effective = weight;
    }
    if (effective != 0.0) {
      double factor = this.get(Modifiers.FAIRY_EFFECTIVENESS);
      // The 0->1 factor for generic familiars conflicts with the JitB
      if (factor == 0.0 && familiarId != FamiliarPool.JACK_IN_THE_BOX) factor = 1.0;
      this.add(
          Modifiers.ITEMDROP,
          factor * (Math.sqrt(55 * effective) + effective - 3),
          "Familiar:" + race);
    }

    if (FamiliarDatabase.isUnderwaterType(familiarId)) {
      this.set(Modifiers.UNDERWATER_FAMILIAR, true);
    }

    switch (familiarId) {
      case FamiliarPool.HATRACK:
        if (famItem == EquipmentRequest.UNEQUIP) {
          this.add(Modifiers.HATDROP, 50.0, "Familiar:naked hatrack");
        }
        break;
      case FamiliarPool.SCARECROW:
        if (famItem == EquipmentRequest.UNEQUIP) {
          this.add(Modifiers.PANTSDROP, 50.0, "Familiar:naked scarecrow");
        }
        break;
    }
  }

  public static final String getFamiliarEffect(final String itemName) {
    return Modifiers.familiarEffectByName.get(itemName);
  }

  public void applyMinstrelModifiers(final int level, AdventureResult instrument) {
    String name = instrument.getName();
    Modifiers imods = Modifiers.getModifiers("Clancy", name);

    double effective = imods.get(Modifiers.VOLLEYBALL_WEIGHT);
    if (effective != 0.0) {
      double factor = 2 + effective / 5;
      this.add(Modifiers.EXPERIENCE, factor, name);
    }

    effective = imods.get(Modifiers.FAIRY_WEIGHT);
    if (effective != 0.0) {
      double factor = Math.sqrt(55 * effective) + effective - 3;
      this.add(Modifiers.ITEMDROP, factor, name);
    }

    this.add(Modifiers.HP_REGEN_MIN, imods.get(Modifiers.HP_REGEN_MIN), name);
    this.add(Modifiers.HP_REGEN_MAX, imods.get(Modifiers.HP_REGEN_MAX), name);
    this.add(Modifiers.MP_REGEN_MIN, imods.get(Modifiers.MP_REGEN_MIN), name);
    this.add(Modifiers.MP_REGEN_MAX, imods.get(Modifiers.MP_REGEN_MAX), name);
  }

  public void applyCompanionModifiers(Companion companion) {
    double multiplier = 1.0;
    if (KoLCharacter.hasSkill("Working Lunch")) {
      multiplier = 1.5;
    }

    switch (companion) {
      case EGGMAN:
        this.add(Modifiers.ITEMDROP, 50 * multiplier, "Eggman");
        break;
      case RADISH:
        this.add(Modifiers.INITIATIVE, 50 * multiplier, "Radish Horse");
        break;
      case HIPPO:
        this.add(Modifiers.EXPERIENCE, 3 * multiplier, "Hippotatomous");
        break;
      case CREAM:
        this.add(Modifiers.MONSTER_LEVEL, 20 * multiplier, "Cream Puff");
        break;
    }
  }

  public void applyServantModifiers(EdServantData servant) {
    int id = servant.getId();
    int level = servant.getLevel();
    switch (id) {
      case 1: // Cat
        if (servant.getLevel() >= 7) {
          this.add(Modifiers.ITEMDROP, Math.sqrt(55 * level) + level - 3, "Servant: Cat");
        }
        break;

      case 3: // Maid
        this.add(Modifiers.MEATDROP, Math.sqrt(220 * level) + 2 * level - 6, "Servant: Maid");
        break;

      case 5: // Scribe
        this.add(Modifiers.EXPERIENCE, 2 + level / 5, "Servant: Scribe");
        break;
    }
  }

  public void applyCompanionModifiers(VYKEACompanionData companion) {
    int type = companion.getType();
    int level = companion.getLevel();
    switch (type) {
      case VYKEACompanionData.LAMP:
        this.add(Modifiers.ITEMDROP, level * 10, "VYKEA Companion: Lamp");
        break;

      case VYKEACompanionData.COUCH:
        this.add(Modifiers.MEATDROP, level * 10, "VYKEA Companion: Couch");
        break;
    }
  }

  public void applyVampyricCloakeModifiers() {
    MonsterData ensorcelee = MonsterDatabase.findMonster(Preferences.getString("ensorcelee"));

    if (ensorcelee != null) {
      Modifiers ensorcelMods =
          Modifiers.getModifiers("Ensorcel", ensorcelee.getPhylum().toString());
      if (ensorcelMods != null) {
        String desc = "Item: vampyric cloake";

        this.add(Modifiers.MEATDROP, ensorcelMods.get(Modifiers.MEATDROP) * 0.25, desc);
        this.add(Modifiers.ITEMDROP, ensorcelMods.get(Modifiers.ITEMDROP) * 0.25, desc);
        this.add(Modifiers.CANDYDROP, ensorcelMods.get(Modifiers.CANDYDROP) * 0.25, desc);
      }
    }
  }

  // Parsing item enchantments into KoLmafia modifiers

  private static final Pattern SKILL_PATTERN = Pattern.compile("Grants Skill:.*?<b>(.*?)</b>");

  public static final String parseSkill(final String text) {
    Matcher matcher = Modifiers.SKILL_PATTERN.matcher(text);
    if (matcher.find()) {
      return Modifiers.modifierTag(Modifiers.stringModifiers, Modifiers.SKILL)
          + ": \""
          + matcher.group(1)
          + "\"";
    }

    return null;
  }

  private static final Pattern DR_PATTERN =
      Pattern.compile("Damage Reduction: (<b>)?([+-]?\\d+)(</b>)?");

  public static final String parseDamageReduction(final String text) {
    if (!text.contains("Damage Reduction:")) {
      return null;
    }

    Matcher matcher = Modifiers.DR_PATTERN.matcher(text);
    int dr = 0;

    while (matcher.find()) {
      dr += StringUtilities.parseInt(matcher.group(2));
    }

    return Modifiers.modifierTag(Modifiers.doubleModifiers, Modifiers.DAMAGE_REDUCTION) + ": " + dr;
  }

  private static final Pattern SINGLE_PATTERN =
      Pattern.compile("You may not equip more than one of these at a time");

  public static final String parseSingleEquip(final String text) {
    Matcher matcher = Modifiers.SINGLE_PATTERN.matcher(text);
    if (matcher.find()) {
      return Modifiers.modifierTag(Modifiers.booleanModifiers, Modifiers.SINGLE);
    }

    return null;
  }

  private static final Pattern SOFTCORE_PATTERN =
      Pattern.compile("This item cannot be equipped while in Hardcore");

  public static final String parseSoftcoreOnly(final String text) {
    Matcher matcher = Modifiers.SOFTCORE_PATTERN.matcher(text);
    if (matcher.find()) {
      return Modifiers.modifierTag(Modifiers.booleanModifiers, Modifiers.SOFTCORE);
    }

    return null;
  }

  private static final Pattern ITEM_DROPPER_PATTERN = Pattern.compile("Occasional Hilarity");

  public static final String parseDropsItems(final String text) {
    Matcher matcher = Modifiers.ITEM_DROPPER_PATTERN.matcher(text);
    if (matcher.find()) {
      return Modifiers.modifierTag(Modifiers.booleanModifiers, Modifiers.DROPS_ITEMS);
    }

    return null;
  }

  private static final Pattern LASTS_ONE_DAY_PATTERN =
      Pattern.compile("This item will disappear at the end of the day");

  public static final String parseLastsOneDay(final String text) {
    Matcher matcher = Modifiers.LASTS_ONE_DAY_PATTERN.matcher(text);
    if (matcher.find()) {
      return Modifiers.modifierTag(Modifiers.booleanModifiers, Modifiers.LASTS_ONE_DAY);
    }

    return null;
  }

  private static final Pattern FREE_PULL_PATTERN = Pattern.compile("Free pull from Hagnk's");

  public static final String parseFreePull(final String text) {
    Matcher matcher = Modifiers.FREE_PULL_PATTERN.matcher(text);
    if (matcher.find()) {
      return Modifiers.modifierTag(Modifiers.booleanModifiers, Modifiers.FREE_PULL);
    }

    return null;
  }

  private static final Pattern EFFECT_PATTERN =
      Pattern.compile("Effect: <b><a([^>]*)>([^<]*)</a></b>");

  public static final String parseEffect(final String text) {
    Matcher matcher = Modifiers.EFFECT_PATTERN.matcher(text);
    if (matcher.find()) {
      // matcher.group( 1 ) contains the the link to the description
      // matcher.group( 2 ) contains the name.
      // Look up the effect by descid. If it is unknown, we'll just use the name.
      // Otherwise, we may need to disambiguate the name by effectId.
      String name = matcher.group(2).trim();
      int[] effectIds = EffectDatabase.getEffectIds(name, false);
      if (effectIds.length > 1) {
        String descid = DebugDatabase.parseEffectDescid(matcher.group(1));
        int effectId = EffectDatabase.getEffectIdFromDescription(descid);
        if (effectId != -1) {
          name = "[" + effectId + "]" + name;
        }
      }
      return Modifiers.modifierTag(Modifiers.stringModifiers, Modifiers.EFFECT)
          + ": \""
          + name
          + "\"";
    }

    return null;
  }

  private static final Pattern EFFECT_DURATION_PATTERN =
      Pattern.compile("</a></b> \\(([\\d]*) Adventures?\\)");

  public static final String parseEffectDuration(final String text) {
    Matcher matcher = Modifiers.EFFECT_DURATION_PATTERN.matcher(text);
    if (matcher.find()) {
      return Modifiers.modifierTag(Modifiers.doubleModifiers, Modifiers.EFFECT_DURATION)
          + ": "
          + matcher.group(1);
    }

    return null;
  }

  private static final Pattern SONG_DURATION_PATTERN =
      Pattern.compile("Song Duration: <b>([\\d]*) Adventures</b>");

  public static final String parseSongDuration(final String text) {
    Matcher matcher = Modifiers.SONG_DURATION_PATTERN.matcher(text);
    if (matcher.find()) {
      return Modifiers.modifierTag(Modifiers.doubleModifiers, Modifiers.SONG_DURATION)
          + ": "
          + matcher.group(1);
    }

    return null;
  }

  private static final Pattern ALL_ATTR_PATTERN = Pattern.compile("^All Attributes ([+-]\\d+)$");
  private static final Pattern ALL_ATTR_PCT_PATTERN =
      Pattern.compile("^All Attributes ([+-]\\d+)%$");
  private static final Pattern CLASS_PATTERN =
      Pattern.compile("Bonus&nbsp;for&nbsp;(.*)&nbsp;only");
  private static final Pattern COMBAT_PATTERN =
      Pattern.compile("Monsters (?:are|will be) (.*) attracted to you");
  private static final Pattern HP_MP_PATTERN = Pattern.compile("^Maximum HP/MP ([+-]\\d+)$");

  public static final String parseModifier(final String enchantment) {
    String result;

    // Search the double modifiers first

    result = Modifiers.parseModifier(Modifiers.doubleModifiers, enchantment, false);
    if (result != null) {
      return result;
    }

    // Then the boolean modifiers

    result = Modifiers.parseModifier(Modifiers.booleanModifiers, enchantment, false);
    if (result != null) {
      return result;
    }

    // Then the string modifiers

    result = Modifiers.parseModifier(Modifiers.stringModifiers, enchantment, true);
    if (result != null) {
      return result;
    }

    // Special handling needed

    Matcher matcher;

    matcher = Modifiers.ALL_ATTR_PATTERN.matcher(enchantment);
    if (matcher.find()) {
      String mod = matcher.group(1);
      return Modifiers.MUSCLE
          + mod
          + ", "
          + Modifiers.MYSTICALITY
          + mod
          + ", "
          + Modifiers.MOXIE
          + mod;
    }

    matcher = Modifiers.ALL_ATTR_PCT_PATTERN.matcher(enchantment);
    if (matcher.find()) {
      String mod = matcher.group(1);
      return Modifiers.MUSCLE_PCT
          + mod
          + ", "
          + Modifiers.MYSTICALITY_PCT
          + mod
          + ", "
          + Modifiers.MOXIE_PCT
          + mod;
    }

    matcher = Modifiers.CLASS_PATTERN.matcher(enchantment);
    if (matcher.find()) {
      String plural = matcher.group(1);
      AscensionClass cls = null;
      if (plural.equals("Accordion&nbsp;Thieves")) {
        cls = AscensionClass.ACCORDION_THIEF;
      } else if (plural.equals("Disco&nbsp;Bandits")) {
        cls = AscensionClass.DISCO_BANDIT;
      } else if (plural.equals("Pastamancers")) {
        cls = AscensionClass.PASTAMANCER;
      } else if (plural.equals("Saucerors")) {
        cls = AscensionClass.SAUCEROR;
      } else if (plural.equals("Seal&nbsp;Clubbers")) {
        cls = AscensionClass.SEAL_CLUBBER;
      } else if (plural.equals("Turtle&nbsp;Tamers")) {
        cls = AscensionClass.TURTLE_TAMER;
      } else {
        return null;
      }
      return Modifiers.modifierTag(Modifiers.stringModifiers, Modifiers.CLASS)
          + ": \""
          + cls.getName()
          + "\"";
    }

    matcher = Modifiers.COMBAT_PATTERN.matcher(enchantment);
    if (matcher.find()) {
      String tag =
          !enchantment.contains("Underwater only")
              ? Modifiers.modifierTag(Modifiers.doubleModifiers, Modifiers.COMBAT_RATE)
              : "Combat Rate (Underwater)";
      String level = matcher.group(1);
      String rate =
          level.equals("<i>way</i> more")
              ? "+20"
              : level.equals("significantly more")
                  ? "+15"
                  : level.equals("much more")
                      ? "+10"
                      : level.equals("more")
                          ? "+5"
                          : level.equals("slightly less")
                              ? "-3"
                              : level.equals("less")
                                  ? "-5"
                                  : level.equals("more than a little less")
                                      ? "-7"
                                      : level.equals("quite a bit less")
                                          ? "-9"
                                          : level.equals("much less")
                                              ? "-10"
                                              : level.equals("very much less")
                                                  ? "-11"
                                                  : level.equals("significantly less")
                                                      ? "-15"
                                                      : level.equals("very very very much less")
                                                          ? "-20"
                                                          : level.equals("<i>way</i> less")
                                                              ? "-20"
                                                              : "+0";

      return tag + ": " + rate;
    }

    matcher = Modifiers.HP_MP_PATTERN.matcher(enchantment);
    if (matcher.find()) {
      String mod = matcher.group(1);
      return Modifiers.HP_TAG + mod + ", " + Modifiers.MP_TAG + mod;
    }

    if (enchantment.contains("Regenerate")) {
      return Modifiers.parseRegeneration(enchantment);
    }

    if (enchantment.contains("Resistance")) {
      return Modifiers.parseResistance(enchantment);
    }

    if (enchantment.contains("Your familiar will always act in combat")) {
      return Modifiers.modifierTag(Modifiers.doubleModifiers, Modifiers.FAMILIAR_ACTION_BONUS)
          + ": +100";
    }

    return null;
  }

  public static final String parseStringModifier(final String enchantment) {
    return Modifiers.parseModifier(Modifiers.stringModifiers, enchantment, true);
  }

  public static final String parseDoubleModifier(final String enchantment) {
    return Modifiers.parseModifier(Modifiers.doubleModifiers, enchantment, false);
  }

  private static String parseModifier(
      final Object[][] table, final String enchantment, final boolean quoted) {
    String quote = quoted ? "\"" : "";
    for (int i = 0; i < table.length; ++i) {
      Object object = Modifiers.modifierDescPattern(table, i);
      if (object == null) {
        continue;
      }

      Object[] patterns;

      if (object instanceof Pattern) {
        patterns = new Pattern[1];
        patterns[0] = object;
      } else {
        patterns = (Object[]) object;
      }

      for (int j = 0; j < patterns.length; ++j) {
        Pattern pattern = (Pattern) patterns[j];
        Matcher matcher = pattern.matcher(enchantment);
        if (!matcher.find()) {
          continue;
        }

        if (matcher.groupCount() == 0) {
          String tag = Modifiers.modifierTag(table, i);
          // Kludge for Sureonosity, which always gives +1
          if (tag.equals("Surgeonosity")) {
            return (tag + ": +1");
          }
          return tag;
        }

        String tag = Modifiers.modifierTag(table, i);

        // Kludge for Slime (Really) Hates it
        if (tag.equals("Slime Hates It")) {
          return matcher.group(1) == null ? "Slime Hates It: +1" : "Slime Hates It: +2";
        }

        String value = matcher.group(1);

        if (tag.equals("Class")) {
          value = Modifiers.depluralizeClassName(value);
        }

        return tag + ": " + quote + value.trim() + quote;
      }
    }

    return null;
  }

  private static final Pattern REGEN_PATTERN =
      Pattern.compile("Regenerate (\\d*)-?(\\d*)? ([HM]P)( and .*)? per [aA]dventure$");

  private static String parseRegeneration(final String enchantment) {
    Matcher matcher = Modifiers.REGEN_PATTERN.matcher(enchantment);
    if (!matcher.find()) {
      return null;
    }

    String min = matcher.group(1);
    String max = matcher.group(2) == null ? min : matcher.group(2);
    boolean hp = matcher.group(3).equals("HP");
    boolean both = matcher.group(4) != null;

    if (max.equals("")) {
      max = min;
    }

    if (both) {
      return Modifiers.HP_REGEN_MIN_TAG
          + min
          + ", "
          + Modifiers.HP_REGEN_MAX_TAG
          + max
          + ", "
          + Modifiers.MP_REGEN_MIN_TAG
          + min
          + ", "
          + Modifiers.MP_REGEN_MAX_TAG
          + max;
    }

    if (hp) {
      return Modifiers.HP_REGEN_MIN_TAG + min + ", " + Modifiers.HP_REGEN_MAX_TAG + max;
    }

    return Modifiers.MP_REGEN_MIN_TAG + min + ", " + Modifiers.MP_REGEN_MAX_TAG + max;
  }

  private static final Pattern RESISTANCE_PATTERN = Pattern.compile("Resistance \\(([+-]\\d+)\\)");

  private static String parseResistance(final String enchantment) {
    String level = "";

    Matcher matcher = RESISTANCE_PATTERN.matcher(enchantment);
    if (matcher.find()) {
      level = matcher.group(1);
    } else if (enchantment.contains("Slight")) {
      level = "+1";
    } else if (enchantment.contains("So-So")) {
      level = "+2";
    } else if (enchantment.contains("Serious")) {
      level = "+3";
    } else if (enchantment.contains("Stupendous")) {
      level = "+4";
    } else if (enchantment.contains("Superhuman")) {
      level = "+5";
    } else if (enchantment.contains("Stunning")) {
      level = "+7";
    } else if (enchantment.contains("Sublime")) {
      level = "+9";
    }

    if (enchantment.contains("All Elements")) {
      return Modifiers.SPOOKY
          + level
          + ", "
          + Modifiers.STENCH
          + level
          + ", "
          + Modifiers.HOT
          + level
          + ", "
          + Modifiers.COLD
          + level
          + ", "
          + Modifiers.SLEAZE
          + level;
    }

    if (enchantment.contains("Spooky")) {
      return Modifiers.SPOOKY + level;
    }

    if (enchantment.contains("Stench")) {
      return Modifiers.STENCH + level;
    }

    if (enchantment.contains("Hot")) {
      return Modifiers.HOT + level;
    }

    if (enchantment.contains("Cold")) {
      return Modifiers.COLD + level;
    }

    if (enchantment.contains("Sleaze")) {
      return Modifiers.SLEAZE + level;
    }

    if (enchantment.contains("Slime")) {
      return Modifiers.SLIME + level;
    }

    if (enchantment.contains("Supercold")) {
      return Modifiers.SUPERCOLD + level;
    }

    return null;
  }

  private static boolean findModifier(final Object[][] table, final String tag) {
    for (int i = 0; i < table.length; ++i) {
      Pattern pattern = Modifiers.modifierTagPattern(table, i);
      if (pattern == null) {
        continue;
      }

      Matcher matcher = pattern.matcher(tag);
      if (matcher.find()) {
        return true;
      }
    }
    return false;
  }

  public static final void checkModifiers() {
    for (String lookup : Modifiers.modifiersByName.keySet()) {
      Object modifiers = Modifiers.modifiersByName.get(lookup);

      if (modifiers == null) {
        RequestLogger.printLine("Key \"" + lookup + "\" has no modifiers");
        continue;
      }

      String modifierString =
          (modifiers instanceof Modifiers)
              ? ((Modifiers) modifiers).getString(Modifiers.MODIFIERS)
              : (modifiers instanceof String) ? (String) modifiers : null;

      if (modifierString == null) {
        RequestLogger.printLine(
            "Key \""
                + lookup
                + "\" has bogus modifiers of class "
                + modifiers.getClass().toString());
        continue;
      }

      ModifierList list = Modifiers.splitModifiers(modifierString);

      for (Modifier modifier : list) {
        String mod = modifier.toString();

        if (Modifiers.findModifier(Modifiers.doubleModifiers, mod)) {
          continue;
        }
        if (Modifiers.findModifier(Modifiers.bitmapModifiers, mod)) {
          continue;
        }
        if (Modifiers.findModifier(Modifiers.booleanModifiers, mod)) {
          continue;
        }
        if (Modifiers.findModifier(Modifiers.stringModifiers, mod)) {
          continue;
        }
        if (lookup.startsWith("FamEq:")) {
          continue; // these may contain freeform text
        }
        RequestLogger.printLine("Key \"" + lookup + "\" has unknown modifier: \"" + mod + "\"");
      }
    }
  }

  public static void setLocation(KoLAdventure location) {
    if (location == null) {
      Modifiers.currentLocation = "";
      Modifiers.currentZone = "";
      Modifiers.currentML = 4.0;
      return;
    }

    Modifiers.currentLocation = location.getAdventureName();
    Modifiers.currentZone = location.getZone();
    Modifiers.currentEnvironment = location.getEnvironment();
    AreaCombatData data = location.getAreaSummary();
    Modifiers.currentML = Math.max(4.0, data == null ? 0.0 : data.getAverageML());
  }

  public static double getCurrentML() {
    return Modifiers.currentML;
  }

  public static void setFamiliar(FamiliarData fam) {
    Modifiers.currentFamiliar = fam == null ? "" : fam.getRace();
  }

  public static String getLookupName(final String type, final String name) {
    if (type.equals("Item")) {
      int itemId = ItemDatabase.getItemId(name);
      if (itemId >= 0) {
        return "Item:[" + itemId + "]";
      }
    }
    if (type.equals("Effect")) {
      int effectId = EffectDatabase.getEffectId(name);
      if (effectId >= 0) {
        return "Effect:[" + effectId + "]";
      }
    }
    return type + ":" + name;
  }

  public static String getTypeFromLookup(final String lookup) {
    int index = lookup.indexOf(":");
    if (index != -1) {
      return lookup.substring(0, index);
    }
    return "";
  }

  public static String getNameFromLookup(final String lookup) {
    int index = lookup.indexOf(":");
    if (index != -1) {
      return lookup.substring(index + 1);
    }
    return lookup;
  }

  public static void resetModifiers() {
    Modifiers.modifiersByName.clear();
    Modifiers.familiarEffectByName.clear();
    Modifiers.passiveSkills.clear();
    Modifiers.synergies.clear();
    Modifiers.mutexes.clear();
    Modifiers.uniques.clear();
    Arrays.fill(Modifiers.bitmapMasks, 1);

    BufferedReader reader =
        FileUtilities.getVersionedReader("modifiers.txt", KoLConstants.MODIFIERS_VERSION);
    String[] data;

    loop:
    while ((data = FileUtilities.readData(reader)) != null) {
      if (data.length != 3) {
        continue;
      }

      String type = data[0];
      String name = data[1];
      String lookup = Modifiers.getLookupName(type, name);
      if (Modifiers.modifiersByName.containsKey(lookup)) {
        KoLmafia.updateDisplay("Duplicate modifiers for: " + lookup);
      }

      String modifiers = data[2];
      Modifiers.modifiersByName.put(lookup, modifiers);

      Matcher matcher = FAMILIAR_EFFECT_PATTERN.matcher(modifiers);
      if (matcher.find()) {
        String effect = matcher.group(1);
        Modifiers.familiarEffectByName.put(name, effect);
        matcher = FAMILIAR_EFFECT_TRANSLATE_PATTERN.matcher(effect);
        if (matcher.find()) {
          effect = matcher.replaceAll(FAMILIAR_EFFECT_TRANSLATE_REPLACEMENT);
        }
        matcher = FAMILIAR_EFFECT_TRANSLATE_PATTERN2.matcher(effect);
        if (matcher.find()) {
          effect = matcher.replaceAll(FAMILIAR_EFFECT_TRANSLATE_REPLACEMENT2);
        }
        Modifiers.modifiersByName.put("FamEq:" + name, effect);
      }

      if (type.equals("Synergy")) {
        String[] pieces = name.split("/");
        if (pieces.length < 2) {
          KoLmafia.updateDisplay(name + " contain less than 2 elements.");
          continue;
        }
        int mask = 0;
        for (int i = 0; i < pieces.length; ++i) {
          Modifiers mods = Modifiers.getModifiers("Item", pieces[i]);
          if (mods == null) {
            KoLmafia.updateDisplay(name + " contains element " + pieces[i] + " with no modifiers.");
            continue loop;
          }
          int emask = mods.bitmaps[Modifiers.SYNERGETIC];
          if (emask == 0) {
            KoLmafia.updateDisplay(
                name + " contains element " + pieces[i] + " that isn't Synergetic.");
            continue loop;
          }
          mask |= emask;
        }
        Modifiers.synergies.put(name, IntegerPool.get(mask));
      } else if (type.startsWith("Mutex")) {
        String[] pieces = name.split("/");
        if (pieces.length < 2) {
          KoLmafia.updateDisplay(name + " contain less than 2 elements.");
          continue;
        }
        int bit = 1 << Modifiers.mutexes.size();
        for (int i = 0; i < pieces.length; ++i) {
          Modifiers mods = null;
          if (type.equals("MutexI")) {
            mods = Modifiers.getModifiers("Item", pieces[i]);
          } else if (type.equals("MutexE")) {
            mods = Modifiers.getModifiers("Effect", pieces[i]);
          }
          if (mods == null) {
            KoLmafia.updateDisplay(name + " contains element " + pieces[i] + " with no modifiers.");
            continue loop;
          }
          mods.bitmaps[Modifiers.MUTEX] |= bit;
        }
        Modifiers.mutexes.add(name);
      } else if (type.equals("Unique")) {
        if (Modifiers.uniques.containsKey(name)) {
          KoLmafia.updateDisplay("Unique items for " + name + " already declared.");
          continue;
        }
        Modifiers.uniques.put(name, new HashSet<String>(Arrays.asList(modifiers.split("/"))));
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

  static {
    Modifiers.resetModifiers();
  }

  public static Set<String> getUniques(String name) {
    return Modifiers.uniques.get(name);
  }

  public static void writeModifiers(final File output) {
    RequestLogger.printLine("Writing data override: " + output);

    // One map per equipment category
    Set<String> hats = new TreeSet<>();
    Set<String> weapons = new TreeSet<>();
    Set<String> offhands = new TreeSet<>();
    Set<String> shirts = new TreeSet<>();
    Set<String> pants = new TreeSet<>();
    Set<String> accessories = new TreeSet<>();
    Set<String> containers = new TreeSet<>();
    Set<String> famitems = new TreeSet<>();
    Set<String> sixguns = new TreeSet<>();
    Set<String> bedazzlements = new TreeSet<>();
    Set<String> cards = new TreeSet<>();
    Set<String> folders = new TreeSet<>();
    Set<String> freepulls = new TreeSet<>();
    Set<String> potions = new TreeSet<>();
    Set<String> wikiname = new TreeSet<>();

    // Iterate over all items and assign item id to category
    for (Entry<Integer, String> entry : ItemDatabase.dataNameEntrySet()) {
      Integer key = entry.getKey();
      String name = entry.getValue();
      int type = ItemDatabase.getConsumptionType(key.intValue());

      switch (type) {
        case KoLConstants.EQUIP_HAT:
          hats.add(name);
          break;
        case KoLConstants.EQUIP_PANTS:
          pants.add(name);
          break;
        case KoLConstants.EQUIP_SHIRT:
          shirts.add(name);
          break;
        case KoLConstants.EQUIP_WEAPON:
          weapons.add(name);
          break;
        case KoLConstants.EQUIP_OFFHAND:
          offhands.add(name);
          break;
        case KoLConstants.EQUIP_ACCESSORY:
          accessories.add(name);
          break;
        case KoLConstants.EQUIP_CONTAINER:
          containers.add(name);
          break;
        case KoLConstants.EQUIP_FAMILIAR:
          famitems.add(name);
          break;
        case KoLConstants.CONSUME_SIXGUN:
          sixguns.add(name);
          break;
        case KoLConstants.CONSUME_STICKER:
          bedazzlements.add(name);
          break;
        case KoLConstants.CONSUME_CARD:
          cards.add(name);
          break;
        case KoLConstants.CONSUME_FOLDER:
          folders.add(name);
          break;
        default:
          Modifiers mods = Modifiers.getModifiers("Item", name);
          if (mods == null) {
            break;
          }
          if (!mods.getString(Modifiers.EFFECT).equals("")) {
            potions.add(name);
          } else if (mods.getBoolean(Modifiers.FREE_PULL)) {
            freepulls.add(name);
          } else if (!mods.getString(Modifiers.WIKI_NAME).equals("")) {
            wikiname.add(name);
          }
          break;
      }
    }

    // Make a map of familiars
    Set<String> familiars = new TreeSet<>();
    familiars.add("Familiar:(none)");

    for (Entry<Integer, String> entry : FamiliarDatabase.entrySet()) {
      String name = entry.getValue();
      if (Modifiers.getModifiers("Familiar", name) != null) {
        familiars.add(name);
      }
    }

    // Make a map of campground items
    Set<String> campground = new TreeSet<>();

    for (int i = 0; i < CampgroundRequest.campgroundItems.length; ++i) {
      int itemId = CampgroundRequest.campgroundItems[i];
      String name = ItemDatabase.getItemDataName(itemId);
      // Sanity check: if the user has an old override file
      // which we didn't delete for some reason, we may have
      // an unknown item on the list of campground items.
      if (name == null) {
        KoLmafia.updateDisplay(
            "Campground item #"
                + itemId
                + " not found in data file. Do 'update clear' to remove stale override!");
      }
      // Skip toilet paper, since we want that in the free
      // pull section
      else if (itemId != ItemPool.TOILET_PAPER) {
        campground.add(name);
      }
    }

    // Make a map of status effects
    Set<String> effects = new TreeSet<>();

    for (Entry<Integer, String> entry : EffectDatabase.entrySet()) {
      Integer key = entry.getKey();
      String name = entry.getValue();
      // Skip effect which is also an item
      effects.add(name);
    }

    // Make a map of passive skills
    Set<String> passives = new TreeSet<>();

    for (Entry<Integer, String> entry : SkillDatabase.entrySet()) {
      Integer key = entry.getKey();
      String name = entry.getValue();
      if (SkillDatabase.isPassive(key.intValue())) {
        passives.add(name);
      }
    }

    // Make a map of outfits
    Set<String> outfits = new TreeSet<>();
    int outfitCount = EquipmentDatabase.getOutfitCount();

    for (int i = 1; i <= outfitCount; ++i) {
      SpecialOutfit outfit = EquipmentDatabase.getOutfit(i);
      if (outfit != null) {
        outfits.add(outfit.getName());
      }
    }

    // Make a map of zodiac signs
    Set<String> zodiacs = new TreeSet<>();

    for (ZodiacSign sign : ZodiacSign.standardZodiacSigns) {
      zodiacs.add(sign.getName());
    }

    // Make a map of stat days
    Set<String> statdays = new TreeSet<>();
    statdays.add("Muscle Day");
    statdays.add("Mysticality Day");
    statdays.add("Moxie Day");

    // Make a map of zones
    Set<String> zones = new TreeSet<>();

    for (String name : AdventureDatabase.ZONE_DESCRIPTIONS.keySet()) {
      if (Modifiers.getModifiers("Zone", name) != null) {
        zones.add(name);
      }
    }

    // Make a map of locations
    Set<String> locations = new TreeSet<>();

    for (KoLAdventure key : AdventureDatabase.getAsLockableListModel()) {
      String name = key.getAdventureName();
      if (Modifiers.getModifiers("Loc", name) != null) {
        locations.add(name);
      }
    }

    // Make a map of synergies
    Set<String> synergies = new TreeSet<>();

    for (Entry<String, Integer> entry : Modifiers.synergies.entrySet()) {
      String name = entry.getKey();
      int mask = entry.getValue().intValue();
      synergies.add(name);
    }

    // Make a map of mutexes
    Set<String> mutexes = new TreeSet<>();

    for (String name : Modifiers.mutexes) {
      mutexes.add(name);
    }

    // Make a map of maximization categories
    Set<String> maximization = new TreeSet<>();
    int maximizationCount = Maximizer.maximizationCategories.length;

    for (int i = 0; i < maximizationCount; ++i) {
      maximization.add(Maximizer.maximizationCategories[i]);
    }

    // Open the output file
    PrintStream writer = LogStream.openStream(output, true);
    writer.println(KoLConstants.EQUIPMENT_VERSION);

    // For each equipment category, write the map entries
    Modifiers.writeModifierCategory(writer, hats, "Item", "Hats");
    writer.println();
    Modifiers.writeModifierCategory(writer, pants, "Item", "Pants");
    writer.println();
    Modifiers.writeModifierCategory(writer, shirts, "Item", "Shirts");
    writer.println();
    Modifiers.writeModifierCategory(writer, weapons, "Item", "Weapons");
    writer.println();
    Modifiers.writeModifierCategory(writer, offhands, "Item", "Off-hand");
    writer.println();
    Modifiers.writeModifierCategory(writer, accessories, "Item", "Accessories");
    writer.println();
    Modifiers.writeModifierCategory(writer, containers, "Item", "Containers");
    writer.println();
    Modifiers.writeModifierCategory(writer, famitems, "Item", "Familiar Items");
    writer.println();
    Modifiers.writeModifierCategory(writer, sixguns, "Item", "Sixguns");
    writer.println();
    Modifiers.writeModifierCategory(writer, familiars, "Familiar", "Familiars");
    writer.println();
    Modifiers.writeModifierCategory(writer, bedazzlements, "Item", "Bedazzlements");
    writer.println();
    Modifiers.writeModifierCategory(writer, cards, "Item", "Alice's Army");
    writer.println();
    Modifiers.writeModifierCategory(writer, folders, "Item", "Folder");
    writer.println();
    Modifiers.writeModifierCategory(writer, campground, "Campground", "Campground equipment");
    writer.println();
    Modifiers.writeModifierCategory(writer, effects, "Effect", "Status Effects");
    writer.println();
    Modifiers.writeModifierCategory(writer, passives, "Skill", "Passive Skills");
    writer.println();
    Modifiers.writeModifierCategory(writer, outfits, "Outfit", "Outfits");
    writer.println();
    Modifiers.writeModifierCategory(writer, zodiacs, "Sign", "Zodiac Sign");
    writer.println();
    Modifiers.writeModifierCategory(writer, statdays, "StatDay", "Stat Day");
    writer.println();
    Modifiers.writeModifierCategory(writer, zones, "Zone", "Zone-specific");
    writer.println();
    Modifiers.writeModifierCategory(writer, locations, "Loc", "Location-specific");
    writer.println();
    Modifiers.writeModifierCategory(writer, synergies, "Synergy", "Synergies");
    writer.println();
    Modifiers.writeModifierCategory(writer, mutexes, "Mutex", "Mutual exclusions");
    writer.println();
    Modifiers.writeModifierCategory(writer, maximization, "MaxCat", "Maximization categories");
    writer.println();
    Modifiers.writeModifierCategory(writer, potions, "Item", "Everything Else");
    Modifiers.writeModifierCategory(writer, freepulls, "Item");
    Modifiers.writeModifierCategory(writer, wikiname, "Item");

    writer.close();
  }

  private static void writeModifierCategory(
      final PrintStream writer, final Set<String> set, final String type, final String tag) {
    writer.println("# " + tag + " section of modifiers.txt");
    Modifiers.writeModifierCategory(writer, set, type);
  }

  private static void writeModifierCategory(
      final PrintStream writer, final Set<String> set, final String type) {
    writer.println();

    for (String name : set) {
      String lookup = Modifiers.getLookupName(type, name);
      Object modifiers = Modifiers.modifiersByName.get(lookup);
      Modifiers.writeModifierItem(writer, type, name, modifiers);
    }
  }

  public static void writeModifierItem(
      final PrintStream writer, final String type, final String name, Object modifiers) {
    if (modifiers == null) {
      Modifiers.writeModifierComment(writer, type, name);
      return;
    }

    if (modifiers instanceof Modifiers) {
      modifiers = ((Modifiers) modifiers).getString(Modifiers.MODIFIERS);
    }

    Modifiers.writeModifierString(writer, type, name, (String) modifiers);
  }

  public static void writeModifierString(
      final PrintStream writer, final String type, final String name, final String modifiers) {
    writer.println(Modifiers.modifierString(type, name, modifiers));
  }

  public static String modifierString(
      final String type, final String name, final String modifiers) {
    return type + "\t" + name + "\t" + modifiers;
  }

  public static String modifierCommentString(
      final String type, final String name, final String value) {
    return "# " + (type == null ? "" : type + " ") + name + ": " + value;
  }

  public static void writeModifierComment(
      final PrintStream writer, final String type, final String name, final String value) {
    writer.println(Modifiers.modifierCommentString(type, name, value));
  }

  public static String modifierCommentString(final String type, final String name) {
    return "# " + (type == null ? "" : type + " ") + name;
  }

  public static void writeModifierComment(
      final PrintStream writer, final String type, final String name) {
    writer.println(Modifiers.modifierCommentString(type, name));
  }

  public static final void registerItem(final String name, final String text, final int type) {
    // Examine the item description and decide what it is.
    ArrayList<String> unknown = new ArrayList<String>();
    String known = DebugDatabase.parseItemEnchantments(text, unknown, type);
    DebugDatabase.parseRestores(name, text);
    Modifiers.registerObject("Item", name, unknown, known);
  }

  public static final void registerEffect(final String name, final String text) {
    // Examine the effect description and decide what it is.
    ArrayList<String> unknown = new ArrayList<String>();
    String known = DebugDatabase.parseEffectEnchantments(text, unknown);
    Modifiers.registerObject("Effect", name, unknown, known);
  }

  public static final void registerSkill(final String name, final String text) {
    // Examine the effect description and decide what it is.
    ArrayList<String> unknown = new ArrayList<String>();
    String known = DebugDatabase.parseSkillEnchantments(text, unknown);
    Modifiers.registerObject("Skill", name, unknown, known);
  }

  public static final void registerOutfit(final String name, final String text) {
    // Examine the outfit description and decide what it is.
    ArrayList<String> unknown = new ArrayList<String>();
    String known = DebugDatabase.parseOutfitEnchantments(text, unknown);
    Modifiers.registerObject("Outfit", name, unknown, known);
  }

  public static final void updateItem(final String name, final String known) {
    String lookup = Modifiers.getLookupName("Item", name);
    Modifiers.modifiersByName.put(lookup, known);
  }

  private static void registerObject(
      final String type, final String name, final ArrayList<String> unknown, final String known) {
    for (String value : unknown) {
      String printMe = Modifiers.modifierCommentString(type, name, value);
      RequestLogger.printLine(printMe);
      RequestLogger.updateSessionLog(printMe);
    }

    if (known.equals("")) {
      if (unknown.size() == 0) {
        String printMe = Modifiers.modifierCommentString(type, name);
        RequestLogger.printLine(printMe);
        RequestLogger.updateSessionLog(printMe);
      }
    } else {
      String printMe = Modifiers.modifierString(type, name, known);
      RequestLogger.printLine(printMe);
      RequestLogger.updateSessionLog(printMe);

      String lookup = Modifiers.getLookupName(type, name);
      if (!Modifiers.modifiersByName.containsKey(lookup)) {
        Modifiers.modifiersByName.put(lookup, known);
      }
    }
  }
}
