package net.sourceforge.kolmafia.modifiers;

import static net.sourceforge.kolmafia.persistence.ModifierDatabase.EXPR;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.KoLCharacter;

public enum DoubleModifier implements Modifier {
  FAMILIAR_WEIGHT(
      "Familiar Weight",
      Pattern.compile("([+-]\\d+) (to )?Familiar Weight"),
      Pattern.compile("Familiar Weight: " + EXPR)),
  MONSTER_LEVEL(
      "Monster Level",
      new Pattern[] {
        Pattern.compile("([+-]\\d+) to Monster Level"), Pattern.compile("Monster Level ([+-]\\d+)"),
      },
      Pattern.compile("Monster Level: " + EXPR)),
  COMBAT_RATE("Combat Rate", Pattern.compile("Combat Rate: " + EXPR)),
  INITIATIVE(
      "Initiative",
      new Pattern[] {
        Pattern.compile("Combat Initiative ([+-]\\d+)%"),
        Pattern.compile("([+-]\\d+)% Combat Initiative"),
      },
      Pattern.compile("Initiative: " + EXPR)),
  EXPERIENCE(
      "Experience",
      Pattern.compile("([+-]\\d+) Stat.*Per Fight"),
      Pattern.compile("Experience: " + EXPR)),
  ITEMDROP(
      "Item Drop",
      Pattern.compile("([+-]\\d+)% Item Drops? [Ff]rom Monsters$"),
      Pattern.compile("Item Drop: " + EXPR)),
  MEATDROP(
      "Meat Drop",
      Pattern.compile("([+-]\\d+)% Meat from Monsters"),
      Pattern.compile("Meat Drop: " + EXPR)),
  DAMAGE_ABSORPTION(
      "Damage Absorption",
      Pattern.compile("Damage Absorption ([+-]\\d+)"),
      Pattern.compile("Damage Absorption: " + EXPR)),
  DAMAGE_REDUCTION(
      "Damage Reduction",
      Pattern.compile("Damage Reduction: ([+-]?\\d+)"),
      Pattern.compile("Damage Reduction: " + EXPR)),
  COLD_RESISTANCE("Cold Resistance", Pattern.compile("Cold Resistance: " + EXPR)),
  HOT_RESISTANCE("Hot Resistance", Pattern.compile("Hot Resistance: " + EXPR)),
  SLEAZE_RESISTANCE("Sleaze Resistance", Pattern.compile("Sleaze Resistance: " + EXPR)),
  SPOOKY_RESISTANCE("Spooky Resistance", Pattern.compile("Spooky Resistance: " + EXPR)),
  STENCH_RESISTANCE("Stench Resistance", Pattern.compile("Stench Resistance: " + EXPR)),
  MANA_COST(
      "Mana Cost",
      Pattern.compile("([+-]\\d+) MP to use Skills$"),
      Pattern.compile("Mana Cost: " + EXPR)),
  MOX(
      "Moxie",
      new Pattern[] {
        Pattern.compile("Moxie ([+-]\\d+)$"), Pattern.compile("([+-]\\d+) Moxie$"),
      },
      Pattern.compile("Moxie: " + EXPR)),
  MOX_PCT(
      "Moxie Percent",
      new Pattern[] {
        Pattern.compile("Moxie ([+-]\\d+)%"), Pattern.compile("([+-]\\d+)% Moxie"),
      },
      Pattern.compile("Moxie Percent: " + EXPR)),
  MUS(
      "Muscle",
      new Pattern[] {
        Pattern.compile("Muscle ([+-]\\d+)$"), Pattern.compile("([+-]\\d+) Muscle$"),
      },
      Pattern.compile("Muscle: " + EXPR)),
  MUS_PCT(
      "Muscle Percent",
      new Pattern[] {
        Pattern.compile("Muscle ([+-]\\d+)%"), Pattern.compile("([+-]\\d+)% Muscle"),
      },
      Pattern.compile("Muscle Percent: " + EXPR)),
  MYS(
      "Mysticality",
      new Pattern[] {
        Pattern.compile("Mysticality ([+-]\\d+)$"), Pattern.compile("([+-]\\d+) Mysticality$"),
      },
      Pattern.compile("Mysticality: " + EXPR)),
  MYS_PCT(
      "Mysticality Percent",
      new Pattern[] {
        Pattern.compile("Mysticality ([+-]\\d+)%"), Pattern.compile("([+-]\\d+)% Mysticality"),
      },
      Pattern.compile("Mysticality Percent: " + EXPR)),
  HP(
      "Maximum HP",
      Pattern.compile("Maximum HP ([+-]\\d+)$"),
      Pattern.compile("Maximum HP: " + EXPR)),
  HP_PCT(
      "Maximum HP Percent",
      Pattern.compile("Maximum HP ([+-]\\d+)%"),
      Pattern.compile("Maximum HP Percent: " + EXPR)),
  MP(
      "Maximum MP",
      Pattern.compile("Maximum MP ([+-]\\d+)$"),
      Pattern.compile("Maximum MP: " + EXPR)),
  MP_PCT(
      "Maximum MP Percent",
      Pattern.compile("Maximum MP ([+-]\\d+)%"),
      Pattern.compile("Maximum MP Percent: " + EXPR)),
  WEAPON_DAMAGE(
      "Weapon Damage",
      new Pattern[] {
        Pattern.compile("Weapon Damage ([+-]\\d+)$"), Pattern.compile("([+-]\\d+) Weapon Damage"),
      },
      Pattern.compile("Weapon Damage: " + EXPR)),
  RANGED_DAMAGE(
      "Ranged Damage",
      new Pattern[] {
        Pattern.compile("Ranged Damage ([+-]\\d+)$"), Pattern.compile("([+-]\\d+) Ranged Damage"),
      },
      Pattern.compile("Ranged Damage: " + EXPR)),
  SPELL_DAMAGE(
      "Spell Damage",
      new Pattern[] {
        Pattern.compile("Spell Damage ([+-]\\d+)$"), Pattern.compile("([+-]\\d+) Spell Damage"),
      },
      Pattern.compile("Spell Damage: " + EXPR)),
  SPELL_DAMAGE_PCT(
      "Spell Damage Percent",
      new Pattern[] {
        Pattern.compile("Spell Damage ([+-][\\d.]+)%"),
        Pattern.compile("([+-][\\d.]+)% Spell Damage"),
      },
      Pattern.compile("Spell Damage Percent: " + EXPR)),
  COLD_DAMAGE(
      "Cold Damage",
      Pattern.compile("^([+-]\\d+) <font color=blue>Cold Damage<"),
      Pattern.compile("Cold Damage: " + EXPR)),
  HOT_DAMAGE(
      "Hot Damage",
      Pattern.compile("^([+-]\\d+) <font color=red>Hot Damage<"),
      Pattern.compile("Hot Damage: " + EXPR)),
  SLEAZE_DAMAGE(
      "Sleaze Damage",
      Pattern.compile("^([+-]\\d+) <font color=blueviolet>Sleaze Damage<"),
      Pattern.compile("Sleaze Damage: " + EXPR)),
  SPOOKY_DAMAGE(
      "Spooky Damage",
      Pattern.compile("^([+-]\\d+) <font color=gray>Spooky Damage<"),
      Pattern.compile("Spooky Damage: " + EXPR)),
  STENCH_DAMAGE(
      "Stench Damage",
      Pattern.compile("^([+-]\\d+) <font color=green>Stench Damage<"),
      Pattern.compile("Stench Damage: " + EXPR)),
  COLD_SPELL_DAMAGE(
      "Cold Spell Damage",
      Pattern.compile("^([+-]\\d+) (Damage )?to <font color=blue>Cold Spells</font>"),
      Pattern.compile("Cold Spell Damage: " + EXPR)),
  HOT_SPELL_DAMAGE(
      "Hot Spell Damage",
      Pattern.compile("^([+-]\\d+) (Damage )?to (<font color=red>)?Hot Spells(</font>)?"),
      Pattern.compile("Hot Spell Damage: " + EXPR)),
  SLEAZE_SPELL_DAMAGE(
      "Sleaze Spell Damage",
      Pattern.compile("^([+-]\\d+) (Damage )?to <font color=blueviolet>Sleaze Spells</font>"),
      Pattern.compile("Sleaze Spell Damage: " + EXPR)),
  SPOOKY_SPELL_DAMAGE(
      "Spooky Spell Damage",
      Pattern.compile("^([+-]\\d+) (Damage )?to <font color=gray>Spooky Spells</font>"),
      Pattern.compile("Spooky Spell Damage: " + EXPR)),
  STENCH_SPELL_DAMAGE(
      "Stench Spell Damage",
      Pattern.compile("^([+-]\\d+) (Damage )?to <font color=green>Stench Spells</font>"),
      Pattern.compile("Stench Spell Damage: " + EXPR)),
  UNDERWATER_COMBAT_RATE(
      "Underwater Combat Rate", Pattern.compile("Combat Rate \\(Underwater\\): " + EXPR)),
  FUMBLE("Fumble", Pattern.compile("(\\d+)x chance of Fumble"), Pattern.compile("Fumble: " + EXPR)),
  HP_REGEN_MIN("HP Regen Min", Pattern.compile("HP Regen Min: " + EXPR)),
  HP_REGEN_MAX("HP Regen Max", Pattern.compile("HP Regen Max: " + EXPR)),
  MP_REGEN_MIN("MP Regen Min", Pattern.compile("MP Regen Min: " + EXPR)),
  MP_REGEN_MAX("MP Regen Max", Pattern.compile("MP Regen Max: " + EXPR)),
  ADVENTURES(
      "Adventures",
      Pattern.compile("([+-]\\d+) Adventure\\(s\\) per day( when equipped)?"),
      Pattern.compile("Adventures: " + EXPR)),
  FAMILIAR_WEIGHT_PCT(
      "Familiar Weight Percent",
      Pattern.compile("([+-]\\d+)% Familiar Weight"),
      Pattern.compile("Familiar Weight Percent: " + EXPR)),
  WEAPON_DAMAGE_PCT(
      "Weapon Damage Percent",
      Pattern.compile("Weapon Damage ([+-]\\d+)%"),
      Pattern.compile("Weapon Damage Percent: " + EXPR)),
  RANGED_DAMAGE_PCT(
      "Ranged Damage Percent",
      Pattern.compile("Ranged Damage ([+-]\\d+)%"),
      Pattern.compile("Ranged Damage Percent: " + EXPR)),
  STACKABLE_MANA_COST(
      "Stackable Mana Cost",
      Pattern.compile("([+-]\\d+) MP to use Skills$"),
      Pattern.compile("Mana Cost \\(stackable\\): " + EXPR)),
  HOBO_POWER(
      "Hobo Power",
      Pattern.compile("([+-]\\d+) Hobo Power"),
      Pattern.compile("Hobo Power: " + EXPR)),
  BASE_RESTING_HP("Base Resting HP", Pattern.compile("Base Resting HP: " + EXPR)),
  RESTING_HP_PCT("Resting HP Percent", Pattern.compile("Resting HP Percent: " + EXPR)),
  BONUS_RESTING_HP("Bonus Resting HP", Pattern.compile("Bonus Resting HP: " + EXPR)),
  BASE_RESTING_MP("Base Resting MP", Pattern.compile("Base Resting MP: " + EXPR)),
  RESTING_MP_PCT("Resting MP Percent", Pattern.compile("Resting MP Percent: " + EXPR)),
  BONUS_RESTING_MP("Bonus Resting MP", Pattern.compile("Bonus Resting MP: " + EXPR)),
  CRITICAL_PCT(
      "Critical Hit Percent",
      Pattern.compile("([+-]\\d+)% [Cc]hance of Critical Hit"),
      Pattern.compile("Critical Hit Percent: " + EXPR)),
  PVP_FIGHTS(
      "PvP Fights",
      Pattern.compile("([+-]\\d+) PvP [Ff]ight\\(s\\) per day( when equipped)?"),
      Pattern.compile("PvP Fights: " + EXPR)),
  VOLLEYBALL_WEIGHT("Volleyball", Pattern.compile("Volley(?:ball)?: " + EXPR)),
  SOMBRERO_WEIGHT("Sombrero", Pattern.compile("Somb(?:rero)?: " + EXPR)),
  LEPRECHAUN_WEIGHT("Leprechaun", Pattern.compile("Lep(?:rechaun)?: " + EXPR)),
  FAIRY_WEIGHT("Fairy", Pattern.compile("Fairy: " + EXPR)),
  MEATDROP_PENALTY("Meat Drop Penalty", Pattern.compile("Meat Drop Penalty: " + EXPR)),
  HIDDEN_FAMILIAR_WEIGHT(
      "Hidden Familiar Weight", Pattern.compile("Familiar Weight \\(hidden\\): " + EXPR)),
  ITEMDROP_PENALTY("Item Drop Penalty", Pattern.compile("Item Drop Penalty: " + EXPR)),
  INITIATIVE_PENALTY("Initiative Penalty", Pattern.compile("Initiative Penalty: " + EXPR)),
  FOODDROP(
      "Food Drop",
      Pattern.compile("([+-]\\d+)% Food Drops? [Ff]rom Monsters$"),
      Pattern.compile("Food Drop: " + EXPR)),
  BOOZEDROP(
      "Booze Drop",
      Pattern.compile("([+-]\\d+)% Booze Drops? [Ff]rom Monsters$"),
      Pattern.compile("Booze Drop: " + EXPR)),
  HATDROP(
      "Hat Drop",
      Pattern.compile("([+-]\\d+)% Hat(?:/Pants)? Drops? [Ff]rom Monsters$"),
      Pattern.compile("Hat Drop: " + EXPR)),
  WEAPONDROP(
      "Weapon Drop",
      Pattern.compile("([+-]\\d+)% Weapon Drops? [Ff]rom Monsters$"),
      Pattern.compile("Weapon Drop: " + EXPR)),
  OFFHANDDROP(
      "Offhand Drop",
      Pattern.compile("([+-]\\d+)% Off-[Hh]and Drops? [Ff]rom Monsters$"),
      Pattern.compile("Offhand Drop: " + EXPR)),
  SHIRTDROP(
      "Shirt Drop",
      Pattern.compile("([+-]\\d+)% Shirt Drops? [Ff]rom Monsters$"),
      Pattern.compile("Shirt Drop: " + EXPR)),
  PANTSDROP(
      "Pants Drop",
      Pattern.compile("([+-]\\d+)% (?:Hat/)?Pants Drops? [Ff]rom Monsters$"),
      Pattern.compile("Pants Drop: " + EXPR)),
  ACCESSORYDROP(
      "Accessory Drop",
      Pattern.compile("([+-]\\d+)% Accessory Drops? [Ff]rom Monsters$"),
      Pattern.compile("Accessory Drop: " + EXPR)),
  VOLLEYBALL_EFFECTIVENESS(
      "Volleyball Effectiveness", Pattern.compile("Volleyball Effectiveness: " + EXPR)),
  SOMBRERO_EFFECTIVENESS(
      "Sombrero Effectiveness", Pattern.compile("Sombrero Effectiveness: " + EXPR)),
  LEPRECHAUN_EFFECTIVENESS(
      "Leprechaun Effectiveness", Pattern.compile("Leprechaun Effectiveness: " + EXPR)),
  FAIRY_EFFECTIVENESS("Fairy Effectiveness", Pattern.compile("Fairy Effectiveness: " + EXPR)),
  FAMILIAR_WEIGHT_CAP("Familiar Weight Cap", Pattern.compile("Familiar Weight Cap: " + EXPR)),
  SLIME_RESISTANCE("Slime Resistance", Pattern.compile("Slime Resistance: " + EXPR)),
  SLIME_HATES_IT(
      "Slime Hates It",
      Pattern.compile("Slime( Really)? Hates (It|You)"),
      Pattern.compile("Slime Hates It: " + EXPR)),
  SPELL_CRITICAL_PCT(
      "Spell Critical Percent",
      Pattern.compile("([+-]\\d+)% [cC]hance of Spell Critical Hit"),
      Pattern.compile("Spell Critical Percent: " + EXPR)),
  MUS_EXPERIENCE(
      "Muscle Experience",
      Pattern.compile("([+-]\\d+) Muscle Stat.*Per Fight"),
      Pattern.compile("Experience \\(Muscle\\): " + EXPR),
      "Experience (Muscle)"),
  MYS_EXPERIENCE(
      "Mysticality Experience",
      Pattern.compile("([+-]\\d+) Mysticality Stat.*Per Fight"),
      Pattern.compile("Experience \\(Mysticality\\): " + EXPR),
      "Experience (Mysticality)"),
  MOX_EXPERIENCE(
      "Moxie Experience",
      Pattern.compile("([+-]\\d+) Moxie Stat.*Per Fight"),
      Pattern.compile("Experience \\(Moxie\\): " + EXPR),
      "Experience (Moxie)"),
  EFFECT_DURATION("Effect Duration", Pattern.compile("Effect Duration: " + EXPR)),
  CANDYDROP(
      "Candy Drop",
      Pattern.compile("([+-]\\d+)% Candy Drops? [Ff]rom Monsters$"),
      Pattern.compile("Candy Drop: " + EXPR)),
  DB_COMBAT_DAMAGE(
      "DB Combat Damage",
      new Pattern[] {
        Pattern.compile("([+-]\\d+) damage to Disco Bandit Combat Skills"),
        Pattern.compile("([+-]\\d+) Disco Bandit Skill Damage"),
      },
      Pattern.compile("DB Combat Damage: " + EXPR)),
  SOMBRERO_BONUS(
      "Sombrero Bonus",
      Pattern.compile("([+-]\\d+) lbs?\\. of Sombrero"),
      Pattern.compile("Sombrero Bonus: " + EXPR)),
  FAMILIAR_EXP(
      "Familiar Experience",
      Pattern.compile("([+-]\\d+) Familiar Experience"),
      Pattern.compile("Experience \\(familiar\\): " + EXPR),
      "Experience (familiar)"),
  SPORADIC_MEATDROP(
      "Sporadic Meat Drop",
      Pattern.compile("Meat Drop \\(sporadic\\): " + EXPR),
      "Meat Drop (sporadic)"),
  SPORADIC_ITEMDROP(
      "Sporadic Item Drop",
      Pattern.compile("Item Drop \\(sporadic\\): " + EXPR),
      "Item Drop (sporadic)"),
  MEAT_BONUS("Meat Bonus", Pattern.compile("Meat Bonus: " + EXPR)),
  PICKPOCKET_CHANCE(
      "Pickpocket Chance",
      Pattern.compile("([+-]\\d+)% Pickpocket Chance"),
      Pattern.compile("Pickpocket Chance: " + EXPR)),
  COMBAT_MANA_COST(
      "Combat Mana Cost",
      Pattern.compile("([+-]\\d+) MP to use Skills \\(in-combat only\\)"),
      Pattern.compile("Mana Cost \\(combat\\): " + EXPR),
      "Mana Cost (combat)"),
  MUS_EXPERIENCE_PCT(
      "Muscle Experience Percent",
      Pattern.compile("([+-]\\d+)% to all Muscle Gains"),
      Pattern.compile("Experience Percent \\(Muscle\\): " + EXPR),
      "Experience Percent (Muscle)"),
  MYS_EXPERIENCE_PCT(
      "Mysticality Experience Percent",
      Pattern.compile("([+-]\\d+)% to all Mysticality Gains"),
      Pattern.compile("Experience Percent \\(Mysticality\\): " + EXPR),
      "Experience Percent (Mysticality)"),
  MOX_EXPERIENCE_PCT(
      "Moxie Experience Percent",
      Pattern.compile("([+-]\\d+)% to all Moxie Gains"),
      Pattern.compile("Experience Percent \\(Moxie\\): " + EXPR),
      "Experience Percent (Moxie)"),
  MINSTREL_LEVEL(
      "Minstrel Level",
      new Pattern[] {
        Pattern.compile("([+-]\\d+) to Minstrel Level"),
        Pattern.compile("Minstrel Level ([+-]\\d+)"),
      },
      Pattern.compile("Minstrel Level: " + EXPR)),
  MUS_LIMIT(
      "Muscle Limit",
      Pattern.compile("Base Muscle Limited to (\\d+)"),
      Pattern.compile("Muscle Limit: " + EXPR)),
  MYS_LIMIT(
      "Mysticality Limit",
      Pattern.compile("Base Mysticality Limited to (\\d+)"),
      Pattern.compile("Mysticality Limit: " + EXPR)),
  MOX_LIMIT(
      "Moxie Limit",
      Pattern.compile("Base Moxie Limited to (\\d+)"),
      Pattern.compile("Moxie Limit: " + EXPR)),
  SONG_DURATION(
      "Song Duration",
      Pattern.compile("Song Duration: ([+-]\\d+) Adventures"),
      Pattern.compile("Song Duration: " + EXPR)),
  PRISMATIC_DAMAGE("Prismatic Damage", null),
  SMITHSNESS(
      "Smithsness",
      Pattern.compile("([+-]\\d+) Smithsness"),
      Pattern.compile("Smithsness: " + EXPR)),
  SUPERCOLD_RESISTANCE("Supercold Resistance", Pattern.compile("Supercold Resistance: " + EXPR)),
  REDUCE_ENEMY_DEFENSE(
      "Reduce Enemy Defense",
      Pattern.compile("Reduce enemy defense by (\\d+)%"),
      Pattern.compile("Reduce Enemy Defense: " + EXPR)),
  POOL_SKILL(
      "Pool Skill",
      Pattern.compile("([+-]\\d+) Pool Skill"),
      Pattern.compile("Pool Skill: " + EXPR)),
  FAMILIAR_DAMAGE(
      "Familiar Damage",
      new Pattern[] {
        Pattern.compile("([+-]\\d+) to Familiar Damage"),
        Pattern.compile("Familiar Damage ([+-]\\d+)"),
      },
      Pattern.compile("Familiar Damage: " + EXPR)),
  GEARDROP(
      "Gear Drop",
      Pattern.compile("([+-]\\d+)% Gear Drops? [Ff]rom Monsters$"),
      Pattern.compile("Gear Drop: " + EXPR)),
  MAXIMUM_HOOCH(
      "Maximum Hooch",
      Pattern.compile("([+-]\\d+) Maximum Hooch"),
      Pattern.compile("Maximum Hooch: " + EXPR)),
  WATER_LEVEL("Water Level", Pattern.compile("Water Level: " + EXPR)),
  CRIMBOT_POWER(
      "Crimbot Outfit Power",
      Pattern.compile("([+-]\\d+) Crimbot Outfit Power"),
      Pattern.compile("Crimbot Outfit Power: " + EXPR)),
  FAMILIAR_TUNING_MUSCLE(
      "Familiar Tuning Muscle",
      Pattern.compile("Familiar Tuning \\(Muscle\\): " + EXPR),
      "Familiar Tuning (Muscle)"),
  FAMILIAR_TUNING_MYSTICALITY(
      "Familiar Tuning Mysticality",
      Pattern.compile("Familiar Tuning \\(Mysticality\\): " + EXPR),
      "Familiar Tuning (Mysticality)"),
  FAMILIAR_TUNING_MOXIE(
      "Familiar Tuning Moxie",
      Pattern.compile("Familiar Tuning \\(Moxie\\): " + EXPR),
      "Familiar Tuning (Moxie)"),
  RANDOM_MONSTER_MODIFIERS(
      "Random Monster Modifiers",
      Pattern.compile("([+-]\\d+) Random Monster Modifier"),
      Pattern.compile("Random Monster Modifiers: " + EXPR)),
  LUCK("Luck", Pattern.compile("([+-]\\d+) Luck"), Pattern.compile("Luck: " + EXPR)),
  OTHELLO_SKILL(
      "Othello Skill",
      Pattern.compile("([+-]\\d+) Othello Skill"),
      Pattern.compile("Othello Skill: " + EXPR)),
  DISCO_STYLE(
      "Disco Style",
      Pattern.compile("([+-]\\d+) Disco Style"),
      Pattern.compile("Disco Style: " + EXPR)),
  ROLLOVER_EFFECT_DURATION(
      "Rollover Effect Duration",
      Pattern.compile("Grants (\\d+) Adventures of <b>.*?</b> at Rollover"),
      Pattern.compile("Rollover Effect Duration: " + EXPR)),
  SIXGUN_DAMAGE("Sixgun Damage", Pattern.compile("Sixgun Damage: " + EXPR)),
  FISHING_SKILL(
      "Fishing Skill",
      Pattern.compile("([+-]\\d+) Fishing Skill"),
      Pattern.compile("Fishing Skill: " + EXPR)),
  ADDITIONAL_SONG(
      "Additional Song",
      Pattern.compile("Keep (\\d+) additional song in your head"),
      Pattern.compile("Additional Song: " + EXPR)),
  SPRINKLES(
      "Sprinkle Drop",
      Pattern.compile("([+-]\\d+)% Sprinkles from Monsters"),
      Pattern.compile("Sprinkle Drop: " + EXPR)),
  ABSORB_ADV(
      "Absorb Adventures",
      Pattern.compile("([+-]\\d+) Adventures when you absorb an item"),
      Pattern.compile("Absorb Adventures: " + EXPR)),
  ABSORB_STAT(
      "Absorb Stats",
      Pattern.compile("([+-]\\d+) Stats when you absorb an item"),
      Pattern.compile("Absorb Stats: " + EXPR)),
  RUBEE_DROP(
      "Rubee Drop",
      Pattern.compile("FantasyRealm enemies will drop (\\d+) extra Rubee"),
      Pattern.compile("Rubee Drop: " + EXPR)),
  KRUEGERAND_DROP(
      "Kruegerand Drop",
      Pattern.compile("Lets you find (\\d+)% more Kruegerands"),
      Pattern.compile("Kruegerand Drop: " + EXPR)),
  WARBEAR_ARMOR_PENETRATION(
      "WarBear Armor Penetration",
      Pattern.compile("([+-]\\d+) WarBear Armor Penetration"),
      Pattern.compile("WarBear Armor Penetration: " + EXPR)),
  PP(
      "Maximum PP",
      Pattern.compile("([+-]\\d+) Max(imum)? Power Point"),
      Pattern.compile("Maximum PP: " + EXPR)),
  PLUMBER_POWER("Plumber Power", Pattern.compile("Plumber Power: " + EXPR)),
  DRIPPY_DAMAGE(
      "Drippy Damage",
      new Pattern[] {
        Pattern.compile("([+-]\\d+) Damage vs. creatures of The Drip"),
        Pattern.compile("([+-]\\d+) Damage against Drip creatures"),
      },
      Pattern.compile("Drippy Damage: " + EXPR)),
  DRIPPY_RESISTANCE("Drippy Resistance", Pattern.compile("Drippy Resistance: " + EXPR)),
  ENERGY("Energy", Pattern.compile("Energy: " + EXPR)),
  SCRAP("Scrap", Pattern.compile("Scrap: " + EXPR)),
  FAMILIAR_ACTION_BONUS("Familiar Action Bonus", Pattern.compile("Familiar Action Bonus: " + EXPR)),
  WATER(
      "Water",
      Pattern.compile("Collect (\\d+) water per adventure"),
      Pattern.compile("Water: " + EXPR)),
  SPLEEN_DROP(
      "Spleen Drop",
      Pattern.compile("([+-]\\d+)% Spleen Item Drops? [Ff]rom Monsters$"),
      Pattern.compile("Spleen Drop: " + EXPR)),
  POTION_DROP(
      "Potion Drop",
      Pattern.compile("([+-]\\d+)% Potion Drops? [Ff]rom Monsters$"),
      Pattern.compile("Potion Drop: " + EXPR)),
  SAUCE_SPELL_DAMAGE(
      "Sauce Spell Damage",
      new Pattern[] {
        Pattern.compile("Sauce Spell Damage ([+-]\\d+)$"),
        Pattern.compile("([+-]\\d+) Sauce Spell Damage"),
      },
      Pattern.compile("Sauce Spell Damage: " + EXPR)),
  MONSTER_LEVEL_PERCENT(
      "Monster Level Percent",
      Pattern.compile("([+-]\\d+)% Monster Level"),
      Pattern.compile("Monster Level Percent: " + EXPR)),
  FOOD_FAIRY_WEIGHT("Food Fairy", Pattern.compile("Food Fairy: " + EXPR)),
  BOOZE_FAIRY_WEIGHT("Booze Fairy", Pattern.compile("Booze Fairy: " + EXPR)),
  CANDY_FAIRY_WEIGHT("Candy Fairy", Pattern.compile("Candy Fairy: " + EXPR)),
  FOOD_FAIRY_EFFECTIVENESS(
      "Food Fairy Effectiveness", Pattern.compile("Food Fairy Effectiveness: " + EXPR)),
  BOOZE_FAIRY_EFFECTIVENESS(
      "Booze Fairy Effectiveness", Pattern.compile("Booze Fairy Effectiveness: " + EXPR)),
  CANDY_FAIRY_EFFECTIVENESS(
      "Candy Fairy Effectiveness", Pattern.compile("Candy Fairy Effectiveness: " + EXPR)),
  DAMAGE_AURA(
      "Damage Aura",
      Pattern.compile("Deals (.*) (each|every) round"),
      Pattern.compile("Damage Aura: " + EXPR)),
  SPORADIC_DAMAGE_AURA("Sporadic Damage Aura", Pattern.compile("Sporadic Damage Aura: " + EXPR)),
  THORNS(
      "Thorns",
      new Pattern[] {
        Pattern.compile("Damages Attacking Opponents?"),
        Pattern.compile("Damages enemies who hit you"),
        Pattern.compile("Deals (.*) to attackers")
      },
      Pattern.compile("Thorns: " + EXPR)),
  SPORADIC_THORNS("Sporadic Thorns", Pattern.compile("Sporadic Thorns: " + EXPR)),
  STOMACH_CAPACITY(
      "Stomach Capacity",
      Pattern.compile("(.*) Stomach Capacity"),
      Pattern.compile("Stomach Capacity: " + EXPR)),
  LIVER_CAPACITY(
      "Liver Capacity",
      Pattern.compile("(.*) Liver Capacity"),
      Pattern.compile("Liver Capacity: " + EXPR)),
  SPLEEN_CAPACITY(
      "Spleen Capacity",
      Pattern.compile("(.*) Spleen Capacity"),
      Pattern.compile("Spleen Capacity: " + EXPR));

  private final String name;
  private final Pattern[] descPatterns;
  private final Pattern tagPattern;
  private final String tag;

  DoubleModifier(String name, Pattern tagPattern) {
    this(name, (Pattern[]) null, tagPattern, name);
  }

  DoubleModifier(String name, Pattern tagPattern, String tag) {
    this(name, (Pattern[]) null, tagPattern, tag);
  }

  DoubleModifier(String name, Pattern descPattern, Pattern tagPattern) {
    this(name, new Pattern[] {descPattern}, tagPattern, name);
  }

  DoubleModifier(String name, Pattern descPattern, Pattern tagPattern, String tag) {
    this(name, new Pattern[] {descPattern}, tagPattern, tag);
  }

  DoubleModifier(String name, Pattern[] descPatterns, Pattern tagPattern) {
    this(name, descPatterns, tagPattern, name);
  }

  DoubleModifier(String name, Pattern[] descPatterns, Pattern tagPattern, String tag) {
    this.name = name;
    this.descPatterns = descPatterns;
    this.tagPattern = tagPattern;
    this.tag = tag;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Pattern[] getDescPatterns() {
    return descPatterns;
  }

  @Override
  public Pattern getTagPattern() {
    return tagPattern;
  }

  @Override
  public String getTag() {
    return tag;
  }

  @Override
  public ModifierValueType getType() {
    return ModifierValueType.NUMERIC;
  }

  @Override
  public String toString() {
    return name;
  }

  public static final Set<DoubleModifier> DOUBLE_MODIFIERS =
      Collections.unmodifiableSet(EnumSet.allOf(DoubleModifier.class));

  private static final Map<String, DoubleModifier> caselessNameToModifier =
      DOUBLE_MODIFIERS.stream()
          .collect(Collectors.toMap(type -> type.name.toLowerCase(), Function.identity()));

  // equivalent to `Modifiers.findName`
  public static DoubleModifier byCaselessName(String name) {
    return caselessNameToModifier.get(name.toLowerCase());
  }

  // equivalent to `Modifiers.findModifier`
  public static DoubleModifier byTagPattern(final String tag) {
    for (var modifier : DOUBLE_MODIFIERS) {
      Pattern pattern = modifier.getTagPattern();
      if (pattern == null) {
        continue;
      }

      Matcher matcher = pattern.matcher(tag);
      if (matcher.matches()) {
        return modifier;
      }
    }
    return null;
  }

  // equivalent to `Modifiers.parseModifier`
  public static String parseModifier(final String enchantment) {
    for (var mod : DOUBLE_MODIFIERS) {
      Pattern[] patterns = mod.getDescPatterns();

      if (patterns == null) {
        continue;
      }

      for (Pattern pattern : patterns) {
        Matcher matcher = pattern.matcher(enchantment);
        if (!matcher.find()) {
          continue;
        }

        String tag = mod.getTag();

        if (matcher.groupCount() == 0) {
          return tag;
        }

        // Kludge for Slime (Really) Hates it
        if (mod == DoubleModifier.SLIME_HATES_IT) {
          return matcher.group(1) == null ? "Slime Hates It: +1" : "Slime Hates It: +2";
        }

        String value = matcher.group(1);

        return tag + ": " + value.trim();
      }
    }

    return null;
  }

  public static DoubleModifier primeStat() {
    return switch (KoLCharacter.getPrimeIndex()) {
      case 0 -> DoubleModifier.MUS;
      case 1 -> DoubleModifier.MYS;
      case 2 -> DoubleModifier.MOX;
      default -> null;
    };
  }

  public static DoubleModifier primeStatExp() {
    return switch (KoLCharacter.getPrimeIndex()) {
      case 0 -> DoubleModifier.MUS_EXPERIENCE;
      case 1 -> DoubleModifier.MYS_EXPERIENCE;
      case 2 -> DoubleModifier.MOX_EXPERIENCE;
      default -> null;
    };
  }

  public static DoubleModifier primeStatExpPercent() {
    return switch (KoLCharacter.getPrimeIndex()) {
      case 0 -> DoubleModifier.MUS_EXPERIENCE_PCT;
      case 1 -> DoubleModifier.MYS_EXPERIENCE_PCT;
      case 2 -> DoubleModifier.MOX_EXPERIENCE_PCT;
      default -> null;
    };
  }
}
