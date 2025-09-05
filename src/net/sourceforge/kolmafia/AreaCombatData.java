package net.sourceforge.kolmafia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter.Gender;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureDatabase.Environment;
import net.sourceforge.kolmafia.persistence.AdventureQueueDatabase;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.persistence.BountyDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;
import net.sourceforge.kolmafia.persistence.MonsterDrop;
import net.sourceforge.kolmafia.persistence.MonsterDrop.DropFlag;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.BanishManager;
import net.sourceforge.kolmafia.session.CryptManager;
import net.sourceforge.kolmafia.session.CrystalBallManager;
import net.sourceforge.kolmafia.session.EncounterManager;
import net.sourceforge.kolmafia.session.EncounterManager.EncounterType;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.LeprecondoManager;
import net.sourceforge.kolmafia.session.TrackManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AreaCombatData {
  private static double lastDropModifier = 0.0;
  private static double lastDropMultiplier = 0.0;

  private int minHit;
  private int maxHit;
  private int minEvade;
  private int maxEvade;
  private int poison;
  private int jumpChance;

  private final int combats;
  private double weights;

  // Parallel lists: monsters and encounter weighting
  private final List<MonsterData> monsters;
  private final List<MonsterData> superlikelyMonsters;
  private final Map<MonsterData, Integer> baseWeightings;
  private final Map<MonsterData, Integer> currentWeightings;
  private final Map<MonsterData, Integer> rejection;

  private final String zone;

  // Flags in low-order bits of weightings
  private static final int ASCENSION_ODD = 0x01;
  private static final int ASCENSION_EVEN = 0x02;
  private static final int WEIGHT_SHIFT = 2;

  // Combat-data-relevant effects
  private static final AdventureResult EW_THE_HUMANITY = EffectPool.get(EffectPool.EW_THE_HUMANITY);
  private static final AdventureResult A_BEASTLY_ODOR = EffectPool.get(EffectPool.A_BEASTLY_ODOR);

  public AreaCombatData(String zone, final int combats) {
    this.zone = zone;
    this.monsters = new ArrayList<>();
    this.superlikelyMonsters = new ArrayList<>();
    this.baseWeightings = new HashMap<>();
    this.currentWeightings = new HashMap<>();
    this.rejection = new HashMap<>();
    this.combats = combats;
    this.weights = 0.0;
    this.minHit = Integer.MAX_VALUE;
    this.maxHit = 0;
    this.minEvade = Integer.MAX_VALUE;
    this.maxEvade = 0;
    this.poison = Integer.MAX_VALUE;
    this.jumpChance = Integer.MAX_VALUE;
  }

  public void recalculate() {
    this.minHit = Integer.MAX_VALUE;
    this.maxHit = 0;
    this.minEvade = Integer.MAX_VALUE;
    this.maxEvade = 0;
    this.jumpChance = 100;

    double weights = 0.0;
    Map<MonsterData, Integer> currentWeightings = new HashMap<>();
    boolean rwbRelevant =
        Preferences.getString("rwbLocation").equals(this.zone)
            && Preferences.getInteger("rwbMonsterCount") > 0;

    for (MonsterData monster : monsters) {
      // Weighting has two low bits which represent odd or even ascension restriction
      // Strip them out now and restore them at the end
      int baseWeighting = this.baseWeightings.get(monster);
      int flags = baseWeighting & 3;
      baseWeighting = baseWeighting >> WEIGHT_SHIFT;

      String monsterName = monster.getName();

      baseWeighting = AreaCombatData.adjustConditionalWeighting(zone, monsterName, baseWeighting);
      int currentWeighting = baseWeighting;

      if (BanishManager.isBanished(monsterName)
          || (rwbRelevant && !Preferences.getString("rwbMonster").equals(monsterName))) {
        // Banishing reduces copies to 0
        currentWeighting = -3;
      } else {
        var copies = (int) TrackManager.countCopies(monsterName);
        if (Preferences.getInteger("holdHandsMonsterCount") > 0
            && Preferences.getString("holdHandsLocation").equals(this.zone)
            && Preferences.getString("holdHandsMonster").equals(monsterName)) {
          // hold hands
          copies += 1;
        }
        currentWeighting += copies * baseWeighting;
      }

      // Not available in current
      if ((flags == ASCENSION_ODD && KoLCharacter.getAscensions() % 2 == 1)
          || (flags == ASCENSION_EVEN && KoLCharacter.getAscensions() % 2 == 0)) {
        currentWeighting = -2; // impossible this ascension
      }

      // Temporarily set to 0% chance
      if (baseWeighting == -4) {
        currentWeighting = -4;
      }

      currentWeightings.put(monster, (currentWeighting << WEIGHT_SHIFT) | flags);

      // Omit currently 0% chance, banished (-3), impossible (-2) and ultra-rare (-1) monsters
      if (currentWeighting < 0) {
        continue;
      }

      weights += currentWeighting * (1 - (double) this.getRejection(monster) / 100);
      this.addMonsterStats(monster);
    }
    this.weights = weights;
    this.currentWeightings.putAll(currentWeightings);

    // Take into account superlikely monsters if they have a non zero chance to appear
    for (MonsterData monster : superlikelyMonsters) {
      if (AreaCombatData.superlikelyChance(monster) > 0) {
        this.addMonsterStats(monster);
      }
    }
  }

  private void addMonsterStats(MonsterData monster) {
    // These include current monster level and Beeosity

    int attack = monster.getAttack();
    if (attack < this.minEvade) {
      this.minEvade = attack;
    }
    if (attack > this.maxEvade) {
      this.maxEvade = attack;
    }

    int defense = monster.getDefense();
    if (defense < this.minHit) {
      this.minHit = defense;
    }
    if (defense > this.maxHit) {
      this.maxHit = defense;
    }

    int jumpChance = monster.getJumpChance();
    if (jumpChance < this.jumpChance) {
      this.jumpChance = jumpChance;
    }
  }

  public boolean addMonster(String name) {
    int weighting = 1;
    int flags = ASCENSION_EVEN | ASCENSION_ODD;
    int rejection = 0;

    int colon = name.indexOf(":");
    if (colon > 0) {
      String weight = name.substring(colon + 1).trim();

      name = name.substring(0, colon);
      String flag = null;

      if (weight.isEmpty()) {
        KoLmafia.updateDisplay("Missing entry after colon for " + name + " in combats.txt.");
        return false;
      }

      for (int i = 0; i < weight.length(); i++) {
        char ch = weight.charAt(i);
        if (i == 0 && ch == '-') {
          continue;
        }

        if (!Character.isDigit(ch)) {
          flag = weight.substring(i);
          weight = weight.substring(0, i);
          break;
        }
      }

      if (!StringUtilities.isNumeric(weight)) {
        KoLmafia.updateDisplay(
            "First entry after colon for " + name + " in combats.txt is not numeric.");
        return false;
      }

      weighting = Integer.parseInt(weight);

      // Only one flag per monster is supported
      if (flag != null) {
        switch (flag.charAt(0)) {
          case 'e' -> flags = ASCENSION_EVEN;
          case 'o' -> flags = ASCENSION_ODD;
          case 'r' -> {
            if (flag.length() > 1) {
              if (!StringUtilities.isNumeric(flag.substring(1))) {
                KoLmafia.updateDisplay(
                    "Rejection percentage specified for "
                        + name
                        + " in combats.txt is not numeric.");
                return false;
              }
              rejection = StringUtilities.parseInt(flag.substring(1));
            } else {
              KoLmafia.updateDisplay(
                  "No rejection percentage specified for " + name + " in combats.txt.");
              return false;
            }
          }
          default -> {
            KoLmafia.updateDisplay(
                "Unknown flag " + flag.charAt(0) + " specified for " + name + " in combats.txt.");
            return false;
          }
        }
      }
    }

    MonsterData monster = MonsterDatabase.findMonster(name);
    if (monster == null) {
      KoLmafia.updateDisplay(
          "Monster name '" + name + "' in combats.txt does not exactly match a known monster,");
      return false;
    }

    if (EncounterManager.isSuperlikelyMonster(monster.getName())) {
      this.superlikelyMonsters.add(monster);
    } else {
      this.monsters.add(monster);
      this.baseWeightings.put(monster, (weighting << WEIGHT_SHIFT) | flags);
      this.currentWeightings.put(monster, (weighting << WEIGHT_SHIFT) | flags);
      this.rejection.put(monster, rejection);
    }

    this.poison = Math.min(this.poison, monster.getPoison());

    // Don't let ultra-rare monsters skew hit and evade numbers -
    // or anything else.
    if (weighting < 0) {
      return true;
    }

    // Don't let special monsters skew combat percentage numbers
    // or things derived from them, like area-wide item and meat
    // drops. Do include them in hit and evade ("safety") numbers.
    // Assume that the number and total weights of even- and
    // odd-ascension-only monsters are equal.
    if (weighting > 0 && flags != ASCENSION_ODD) {
      this.weights += weighting * (1 - (double) rejection / 100);
    }

    this.addMonsterStats(monster);

    return true;
  }

  public List<MonsterData> getMonsters() {
    return this.monsters;
  }

  public int getMonsterCount() {
    return this.monsters.size();
  }

  public List<MonsterData> getSuperlikelyMonsters() {
    return this.superlikelyMonsters;
  }

  public int getSuperlikelyMonsterCount() {
    return this.superlikelyMonsters.size();
  }

  public int getAvailableMonsterCount() {
    return (int)
        Stream.concat(
                monsters.stream().map(m -> getWeighting(m) > 0),
                superlikelyMonsters.stream().map(m -> superlikelyChance(m) > 0))
            .filter(p -> p)
            .count();
  }

  public MonsterData getMonster(final int i) {
    return this.monsters.get(i);
  }

  public MonsterData getSuperlikelyMonster(final int i) {
    return this.superlikelyMonsters.get(i);
  }

  public boolean hasMonster(final MonsterData m) {
    if (m == null) {
      return false;
    }
    return this.monsters.contains(m) || this.superlikelyMonsters.contains(m);
  }

  public int getWeighting(final MonsterData monster) {
    int raw = this.currentWeightings.getOrDefault(monster, 0);
    if (((raw >> (KoLCharacter.getAscensions() & 1)) & 1) == 0) {
      return -2; // impossible this ascension
    }
    return raw >> WEIGHT_SHIFT;
  }

  private int getMoonlightRejection(final IntSupplier fn) {
    // 0, 1 -> 8/8
    // 2, 3, 4 -> 7/8, 6/8, 5/8 respectively
    return (int) Math.round((Math.min(8, 9 - fn.getAsInt()) / 8.0) * 100);
  }

  public int getRejection(final MonsterData monster) {
    return switch (monster.getName()) {
      case "alielf", "cat-alien", "dog-alien" -> getMoonlightRejection(
          HolidayDatabase::getGrimaceMoonlight);
      case "dogcat", "ferrelf", "hamsterpus" -> getMoonlightRejection(
          HolidayDatabase::getRonaldMoonlight);
      default -> this.rejection.get(monster);
    };
  }

  public double totalWeighting() {
    return this.weights;
  }

  public double dividedByTotalWeighting(final double weight) {
    if (totalWeighting() == 0) return 0;
    return weight / totalWeighting();
  }

  public int combats() {
    return this.combats;
  }

  public int minHit() {
    return this.minHit == Integer.MAX_VALUE ? 0 : this.minHit;
  }

  public int maxHit() {
    return this.maxHit;
  }

  public int minEvade() {
    return this.minEvade == Integer.MAX_VALUE ? 0 : this.minEvade;
  }

  public int maxEvade() {
    return this.maxEvade;
  }

  public int poison() {
    return this.poison;
  }

  public boolean willHitSomething() {
    int hitStat = EquipmentManager.getAdjustedHitStat();
    return AreaCombatData.hitPercent(hitStat, this.minHit()) > 0.0;
  }

  public int getJumpChance() {
    return getJumpChance(MonsterData::getJumpChance);
  }

  public int getJumpChance(int initiative, int ml) {
    return getJumpChance(m -> m.getJumpChance(initiative, ml));
  }

  public int getJumpChance(int initiative) {
    return getJumpChance(m -> m.getJumpChance(initiative));
  }

  public int getJumpChance(Function<MonsterData, Integer> fn) {
    return getMonsterData(true).entrySet().stream()
        .filter(e -> e.getValue() > 0)
        .mapToInt(e -> fn.apply(e.getKey()))
        .min()
        .orElse(0);
  }

  public double getAverageML() {
    double averageML =
        monsters.stream()
            .filter(m -> getWeighting(m) > 0)
            .map(
                m ->
                    dividedByTotalWeighting(
                        (double) m.getAttack()
                            * (double) getWeighting(m)
                            * (1 - (double) this.getRejection(m) / 100)))
            .reduce(0.0, Double::sum);

    double averageSuperlikelyML = 0.0;
    double superlikelyChance = 0.0;

    for (MonsterData monster : this.superlikelyMonsters) {
      double chance = AreaCombatData.superlikelyChance(monster);
      if (chance > 0) {
        averageSuperlikelyML += chance * monster.getAttack();
        superlikelyChance += chance;
      }
    }

    return averageML * (1 - superlikelyChance / 100) + averageSuperlikelyML;
  }

  @Override
  public String toString() {
    return this.toString(false);
  }

  public String toString(final boolean fullString) {
    return this.toString(fullString, false);
  }

  public String toString(final boolean fullString, boolean mapped) {
    StringBuffer buffer = new StringBuffer();

    buffer.append("<html><head>");
    buffer.append("<style>");

    buffer.append("body { font-family: sans-serif; font-size: ");
    buffer.append(Preferences.getString("chatFontSize"));
    buffer.append("; }");

    buffer.append("</style>");

    buffer.append("</head><body>");

    this.getSummary(buffer, fullString, mapped);
    this.getEncounterData(buffer);
    this.appendMonsterData(buffer, fullString, mapped);

    buffer.append("</body></html>");
    return buffer.toString();
  }

  private MonsterData mapMonster(MonsterData mon) {
    Path path = KoLCharacter.getPath();
    if (path != null) {
      Map<MonsterData, MonsterData> pathMap = MonsterDatabase.getMonsterPathMap(path.getName());
      if (pathMap != null) {
        MonsterData mapped = pathMap.get(mon);
        if (mapped != null) {
          return mapped;
        }
      }
    }

    // Your Ascension Class is null in Valhalla
    AscensionClass clazz = KoLCharacter.getAscensionClass();
    if (clazz != null) {
      Map<MonsterData, MonsterData> classMap = MonsterDatabase.getMonsterClassMap(clazz);
      if (classMap != null) {
        MonsterData mapped = classMap.get(mon);
        if (mapped != null) {
          return mapped;
        }
      }
    }

    return mon;
  }

  public void getSummary(
      final StringBuffer buffer, final boolean fullString, final boolean mapped) {
    // Get up-to-date monster stats in area summary
    this.recalculate();

    int moxie = KoLCharacter.getAdjustedMoxie();

    String statName = EquipmentManager.getHitStatType() == Stat.MOXIE ? "Mox" : "Mus";
    int hitstat = EquipmentManager.getAdjustedHitStat();

    double minHitPercent = AreaCombatData.hitPercent(hitstat, this.minHit());
    double maxHitPercent = AreaCombatData.hitPercent(hitstat, this.maxHit);
    int minPerfectHit = AreaCombatData.perfectHit(hitstat, this.minHit());
    int maxPerfectHit = AreaCombatData.perfectHit(hitstat, this.maxHit);
    double minEvadePercent = AreaCombatData.hitPercent(moxie, this.minEvade());
    double maxEvadePercent = AreaCombatData.hitPercent(moxie, this.maxEvade);
    int minPerfectEvade = AreaCombatData.perfectHit(moxie, this.minEvade());
    int maxPerfectEvade = AreaCombatData.perfectHit(moxie, this.maxEvade);
    int jumpChance = this.jumpChance;

    // statGain constants
    double experienceAdjustment = KoLCharacter.getExperienceAdjustment();

    // Area Combat percentage
    double combatFactor = this.areaCombatPercent() / 100.0;

    // Iterate once through monsters to calculate average statGain
    double averageExperience = 0.0;

    for (MonsterData monster : monsters) {
      int weighting = this.getWeighting(monster);
      int rejection = this.getRejection(monster);
      if (mapped) {
        monster = mapMonster(monster);
      }
      if (monster == MonsterData.NO_MONSTER) {
        continue;
      }

      // Omit impossible (-2), ultra-rare (-1) and special/banished (0) monsters
      if (weighting < 1) {
        continue;
      }

      double weight = dividedByTotalWeighting((double) weighting * (1 - (double) rejection / 100));
      int ml = monster.ML();
      averageExperience +=
          weight * (monster.getExperience() + experienceAdjustment - ml / (ml > 0 ? 6.0 : 8.0));
    }

    double averageSuperlikelyExperience = 0.0;
    double superlikelyChance = 0.0;
    for (MonsterData monster : this.superlikelyMonsters) {
      if (mapped) {
        monster = mapMonster(monster);
      }
      if (monster == MonsterData.NO_MONSTER) {
        continue;
      }
      String monsterName = monster.getName();
      double chance = AreaCombatData.superlikelyChance(monsterName);
      if (chance > 0) {
        averageSuperlikelyExperience +=
            chance / 100 * (monster.getExperience() + experienceAdjustment);
        superlikelyChance += chance;
      }
    }

    buffer.append("<b>Hit</b>: ");
    buffer.append(
        this.getRateString(
            minHitPercent, minPerfectHit, maxHitPercent, maxPerfectHit, statName, fullString));

    buffer.append("<br><b>Evade</b>: ");
    buffer.append(
        this.getRateString(
            minEvadePercent, minPerfectEvade, maxEvadePercent, maxPerfectEvade, "Mox", fullString));

    buffer.append("<br><b>Jump Chance</b>: ");
    buffer.append(jumpChance).append("%");

    buffer.append("<br><b>Combat Rate</b>: ");

    double combatXP =
        averageSuperlikelyExperience
            + averageExperience * (1 - superlikelyChance / 100) * combatFactor;
    if (this.combats > 0) {
      buffer
          .append(
              this.format(superlikelyChance + (1 - superlikelyChance / 100) * combatFactor * 100.0))
          .append("%");
      buffer.append("<br><b>Combat XP</b>: ").append(KoLConstants.FLOAT_FORMAT.format(combatXP));
    } else if (this.combats == 0) {
      buffer.append("0%");
    } else {
      buffer.append("No data");
    }
  }

  public Map<MonsterData, Double> getMonsterData() {
    return getMonsterData(false);
  }

  public Map<MonsterData, Double> getMonsterData(boolean stateful) {
    return getMonsterData(stateful, false);
  }

  public Map<MonsterData, Double> getMonsterData(boolean stateful, boolean mapped) {
    Map<MonsterData, Double> monsterData = new TreeMap<>();

    if (stateful) {
      recalculate();
    }

    double totalSuperlikelyChance = 0.0;

    for (MonsterData monster : superlikelyMonsters) {
      if (mapped) {
        monster = mapMonster(monster);
      }
      if (monster == MonsterData.NO_MONSTER) {
        continue;
      }
      double chance = superlikelyChance(monster);
      monsterData.put(monster, chance);
      totalSuperlikelyChance += chance;
    }

    double combatFactor = this.areaCombatPercent(stateful) / 100.0;

    for (MonsterData monster : monsters) {
      int weighting = getWeighting(monster);
      if (mapped) {
        monster = mapMonster(monster);
      }
      if (monster == MonsterData.NO_MONSTER) {
        continue;
      }

      if (weighting == -2) {
        continue;
      }

      if (weighting < 0) {
        monsterData.put(monster, (double) weighting);
        continue;
      }

      // Negative weights (which have special meaning) are already handled above.
      double chance =
          100.0
              * combatFactor
              * (1 - totalSuperlikelyChance / 100)
              * weighting
              * (1 - (double) getRejection(monster) / 100);

      if (stateful) {
        chance = AdventureQueueDatabase.applyQueueEffects(chance, monster, this);
      } else {
        chance = dividedByTotalWeighting(chance);
      }

      monsterData.put(monster, chance);
    }

    return monsterData;
  }

  public void appendMonsterData(
      final StringBuffer buffer, final boolean fullString, final boolean mapped) {
    int moxie = KoLCharacter.getAdjustedMoxie();
    int hitstat = EquipmentManager.getAdjustedHitStat();

    for (Map.Entry<MonsterData, Double> entry : getMonsterData(true).entrySet()) {
      MonsterData monster = entry.getKey();
      int weighting = getWeighting(monster);
      if (mapped) {
        monster = mapMonster(monster);
      }
      if (monster == MonsterData.NO_MONSTER) {
        continue;
      }
      double chance = entry.getValue();
      buffer
          .append("<br><br>")
          .append(this.getMonsterString(monster, moxie, hitstat, weighting, chance, fullString));
    }
  }

  public void getEncounterData(final StringBuffer buffer) {
    Environment environment = AdventureDatabase.getEnvironment(this.zone);
    buffer.append("<br><b>Environment:</b> ");
    if (environment == Environment.UNKNOWN) {
      buffer.append("unknown");
    } else {
      buffer.append(environment);
    }

    int recommendedStat = AdventureDatabase.getRecommendedStat(this.zone);
    buffer.append("<br><b>Recommended Mainstat:</b> ");
    if (recommendedStat == -1) {
      buffer.append("unknown");
    } else {
      buffer.append(recommendedStat);
    }

    if (KoLCharacter.inRaincore()) {
      int waterLevel = KoLCharacter.getWaterLevel();
      boolean fixed = AdventureDatabase.getWaterLevel(this.zone) != -1;
      buffer.append("<br><b>Water Level:</b> ");
      if (environment == null) {
        buffer.append("unknown");
      } else if (recommendedStat == -1 && !fixed) {
        buffer.append(waterLevel);
        buffer.append(" (at least)");
      } else {
        buffer.append(waterLevel);
      }
    }

    var encounter = EncounterManager.findEncounterForLocation(this.zone, EncounterType.LUCKY);

    if (encounter != null) {
      buffer.append("<br>");
      buffer.append("<b>Lucky:</b> ");
      buffer.append(encounter);
    }

    encounter = EncounterManager.findEncounterForLocation(this.zone, EncounterType.GLYPH);

    if (encounter != null) {
      buffer.append("<br>");
      buffer.append("<b>Hobo Glyph:</b> ");
      buffer.append(encounter);
    }

    if (KoLCharacter.inAxecore()) {
      encounter = EncounterManager.findEncounterForLocation(this.zone, EncounterType.BORIS);

      if (encounter != null) {
        buffer.append("<br>");
        buffer.append("<b>Clancy:</b> ");
        buffer.append(encounter);
      }
    }

    if (KoLCharacter.inBadMoon()) {
      encounter = EncounterManager.findEncounterForLocation(this.zone, EncounterType.BADMOON);

      if (encounter != null) {
        buffer.append("<br>");
        buffer.append("<b>Badmoon:</b> ");
        buffer.append(encounter);
      }
    }

    if (InventoryManager.hasItem(ItemPool.LEPRECONDO)) {
      var furniture = LeprecondoManager.getUndiscoveredFurnitureForLocation(this.zone);
      if (furniture.isBlank()) {
        buffer.append("<br>");
        buffer.append("<b>Leprecondo:</b> ");
        buffer.append(furniture);
      }
    }
  }

  private String format(final double percentage) {
    return String.valueOf((int) percentage);
  }

  public double areaCombatPercent() {
    return areaCombatPercent(true);
  }

  public double areaCombatPercent(boolean stateful) {
    if (stateful) {
      // Some situations can force combats
      if (EncounterManager.isSaberForceZone(this.getZone())) {
        return 100;
      }

      // Some areas have fixed non-combats, if we're tracking this, handle them here.
      switch (zone) {
        case "The Defiled Alcove",
            "The Defiled Cranny",
            "The Defiled Niche",
            "The Defiled Nook" -> {
          String property = CryptManager.evilZoneProperty(zone);
          if (Preferences.getInteger(property) <= 13) {
            return 100;
          }
        }
        case "The Smut Orc Logging Camp" -> {
          // Blech House does not appear if the bridge is finished
          return Preferences.getInteger("smutOrcNoncombatProgress") < 15
                  || Preferences.getInteger("chasmBridgeProgress") >= 30
              ? 100
              : 0;
        }
        case "Barf Mountain" -> {
          return Preferences.getBoolean("dinseyRollercoasterNext") ? 0 : 100;
        }
        case "Investigating a Plaintive Telegram" -> {
          return Preferences.getInteger("lttQuestStageCount") == 9
                  || QuestDatabase.isQuestStep(Quest.TELEGRAM, QuestDatabase.STARTED)
              ? 0
              : 100;
        }
        case "The Dripping Trees" -> {
          // Non-Combat on turn 16, 31, 46, ...
          int advs = Preferences.getInteger("drippingTreesAdventuresSinceAscension");
          return (advs > 0 && (advs % 15) == 0) ? 0 : 100;
        }
        case "The SMOOCH Army HQ" -> {
          var combats = Preferences.getInteger("_smoochArmyHQCombats");
          return (combats == 50) ? 0 : 100;
        }
      }
    }

    // If we don't have the data, pretend it's all combat
    if (this.combats < 0) {
      return 100.0;
    }

    // Some areas are inherently all combat or no combat
    if (this.combats == 0 || this.combats == 100) {
      return this.combats;
    }

    double pct = this.combats + KoLCharacter.getCombatRateAdjustment();

    return Math.max(0.0, Math.min(100.0, pct));
  }

  private String getRateString(
      final double minPercent,
      final int minMargin,
      final double maxPercent,
      final int maxMargin,
      final String statName,
      boolean fullString) {
    StringBuilder buffer = new StringBuilder();

    buffer.append(this.format(minPercent));
    buffer.append("%/");

    buffer.append(this.format(maxPercent));
    buffer.append("%");

    if (!fullString) {
      return buffer.toString();
    }

    buffer.append(" (");

    buffer.append(statName);

    if (minMargin < Integer.MAX_VALUE / 2) {
      if (minMargin >= 0) {
        buffer.append("+");
      }
      buffer.append(minMargin);

      buffer.append("/");
    } else {
      buffer.append(" always hit");
    }

    if (minMargin < Integer.MAX_VALUE / 2) {
      if (maxMargin >= 0) {
        buffer.append("+");
      }
      buffer.append(maxMargin);
    }

    buffer.append(")");
    return buffer.toString();
  }

  private String getMonsterString(
      final MonsterData monster,
      final int moxie,
      final int hitstat,
      final int weighting,
      final double chance,
      final boolean fullString) {
    // moxie and hitstat NOT adjusted for monster level, since monster stats now are

    int defense = monster.getDefense();
    double hitPercent = AreaCombatData.hitPercent(hitstat, defense);

    int attack = monster.getAttack();
    double evadePercent = AreaCombatData.hitPercent(moxie, attack);

    int health = monster.getHP();
    double statGain = monster.getExperience();

    StringBuffer buffer = new StringBuffer();

    Element ed = monster.getDefenseElement();
    Element ea = monster.getAttackElement();
    Element element = ed == Element.NONE ? ea : ed;

    Phylum phylum = monster.getPhylum();
    int init = monster.getInitiative();
    int jumpChance = monster.getJumpChance();

    // Color the monster name according to its element
    buffer.append(" <font color=").append(AreaCombatData.elementColor(element)).append("><b>");
    if (monster.getPoison() < Integer.MAX_VALUE) {
      buffer.append("☠ ");
    }
    String name = monster.getName();
    buffer.append(name);
    buffer.append("</b></font> (");

    if (EncounterManager.isSaberForceMonster(name, this.getZone())) {
      buffer.append("forced by the saber");
    } else if (CrystalBallManager.isCrystalBallMonster(name, this.getZone())) {
      buffer.append("predicted by crystal ball");
    } else if (weighting == -1) {
      buffer.append("ultra-rare");
    } else if (weighting == -3) {
      buffer.append("banished");
    } else if (weighting == -4) {
      buffer.append("0%");
    } else if (weighting == 0) {
      buffer.append("special");
    } else {
      buffer.append(this.format(chance)).append("%");
    }

    buffer.append(")<br>Hit: <font color=").append(AreaCombatData.elementColor(ed)).append(">");
    buffer.append(this.format(hitPercent));
    buffer
        .append("%</font>, Evade: <font color=")
        .append(AreaCombatData.elementColor(ea))
        .append(">");
    buffer.append(this.format(evadePercent));
    buffer
        .append("%</font>, Jump Chance: <font color=")
        .append(AreaCombatData.elementColor(ea))
        .append(">");
    buffer.append(this.format(jumpChance));
    buffer.append("%</font><br>Atk: ").append(attack).append(", Def: ").append(defense);
    buffer
        .append(", HP: ")
        .append(health)
        .append(", XP: ")
        .append(KoLConstants.FLOAT_FORMAT.format(statGain));
    buffer.append("<br>Phylum: ").append(phylum);
    if (init == -10000) {
      buffer.append(", Never wins initiative");
    } else if (init == 10000) {
      buffer.append(", Always wins initiative");
    } else {
      buffer.append(", Init: ").append(init);
    }

    if (fullString) {
      this.appendMeatDrop(buffer, monster);
      this.appendSprinkleDrop(buffer, monster);
    }

    this.appendItemList(buffer, monster.getItems(), monster.getPocketRates(), fullString);

    this.appendFact(buffer, monster, fullString);

    String bounty = BountyDatabase.getNameByMonster(monster.getName());
    if (bounty != null) {
      buffer.append("<br>").append(bounty).append(" (bounty)");
    }

    return buffer.toString();
  }

  private void appendMeatDrop(final StringBuffer buffer, final MonsterData monster) {
    int minMeat = monster.getMinMeat();
    int maxMeat = monster.getMaxMeat();

    if (maxMeat == 0) {
      return;
    }

    int avgMeat = monster.getBaseMeat();

    double modifier = Math.max(0.0, (KoLCharacter.getMeatDropPercentAdjustment() + 100.0) / 100.0);
    buffer.append("<br>Meat: ");
    buffer.append(this.format((int) Math.floor(minMeat * modifier)));
    buffer.append("-");
    buffer.append(this.format((int) Math.floor(maxMeat * modifier)));
    buffer.append(" (");
    buffer.append(this.format((int) Math.floor(avgMeat * modifier)));
    buffer.append(" average)");
  }

  private void appendSprinkleDrop(final StringBuffer buffer, final MonsterData monster) {
    int minSprinkles = monster.getMinSprinkles();
    int maxSprinkles = monster.getMaxSprinkles();

    if (maxSprinkles == 0) {
      return;
    }

    double modifier =
        Math.max(0.0, (KoLCharacter.getSprinkleDropPercentAdjustment() + 100.0) / 100.0);
    buffer.append("<br>Sprinkles: ");
    buffer.append(this.format((int) Math.floor(minSprinkles * modifier)));
    if (maxSprinkles != minSprinkles) {
      buffer.append("-");
      buffer.append(this.format((int) Math.ceil(maxSprinkles * modifier)));
    }
  }

  private void appendItemList(
      final StringBuffer buffer,
      final List<MonsterDrop> items,
      final List<Double> pocketRates,
      boolean fullString) {
    if (items.isEmpty()) {
      return;
    }

    double itemModifier = AreaCombatData.getDropRateModifier();
    boolean stealing = KoLCharacter.canPickpocket();
    double pocketModifier =
        (100.0 + KoLCharacter.currentNumericModifier(DoubleModifier.PICKPOCKET_CHANCE)) / 100.0;

    for (int i = 0; i < items.size(); ++i) {
      MonsterDrop drop = items.get(i);

      if (!fullString) {
        if (i == 0) {
          buffer.append("<br>");
        } else {
          buffer.append(", ");
        }

        buffer.append(drop.item().getName());
        continue;
      }

      buffer.append("<br>");

      // Certain items can be increased by other bonuses than just item drop
      int itemId = drop.item().getItemId();
      double itemBonus = 0.0;

      if (ItemDatabase.isFood(itemId) || ItemDatabase.isCookable(itemId)) {
        itemBonus += KoLCharacter.currentNumericModifier(DoubleModifier.FOODDROP) / 100.0;
      } else if (ItemDatabase.isBooze(itemId) || ItemDatabase.isMixable(itemId)) {
        itemBonus += KoLCharacter.currentNumericModifier(DoubleModifier.BOOZEDROP) / 100.0;
      } else if (ItemDatabase.isCandyItem(itemId)) {
        itemBonus += KoLCharacter.currentNumericModifier(DoubleModifier.CANDYDROP) / 100.0;
      } else if (ItemDatabase.isEquipment(itemId)) {
        itemBonus += KoLCharacter.currentNumericModifier(DoubleModifier.GEARDROP) / 100.0;
        if (ItemDatabase.isHat(itemId)) {
          itemBonus += KoLCharacter.currentNumericModifier(DoubleModifier.HATDROP) / 100.0;
        } else if (ItemDatabase.isWeapon(itemId)) {
          itemBonus += KoLCharacter.currentNumericModifier(DoubleModifier.WEAPONDROP) / 100.0;
        } else if (ItemDatabase.isOffHand(itemId)) {
          itemBonus += KoLCharacter.currentNumericModifier(DoubleModifier.OFFHANDDROP) / 100.0;
        } else if (ItemDatabase.isShirt(itemId)) {
          itemBonus += KoLCharacter.currentNumericModifier(DoubleModifier.SHIRTDROP) / 100.0;
        } else if (ItemDatabase.isPants(itemId)) {
          itemBonus += KoLCharacter.currentNumericModifier(DoubleModifier.PANTSDROP) / 100.0;
        } else if (ItemDatabase.isAccessory(itemId)) {
          itemBonus += KoLCharacter.currentNumericModifier(DoubleModifier.ACCESSORYDROP) / 100.0;
        }
      }

      double stealRate = Math.min(pocketRates.get(i) * pocketModifier, 1.0);
      double rawDropRate = drop.chance();
      double dropRate = Math.min(rawDropRate * (itemModifier + itemBonus), 100.0);
      double effectiveDropRate = stealRate * 100.0 + (1.0 - stealRate) * dropRate;

      String rateRaw = this.format(rawDropRate);
      String rate1 = this.format(dropRate);
      String rate2 = this.format(effectiveDropRate);

      if (drop.flag() == DropFlag.MULTI_DROP) {
        buffer.append(drop.itemCount() + " ");
      }
      buffer.append(drop.item().getName());
      switch (drop.flag()) {
        case UNKNOWN_RATE -> buffer.append(" (unknown drop rate)");
        case NO_PICKPOCKET -> {
          if (rawDropRate > 0) {
            buffer.append(" ");
            buffer.append(rate1);
            buffer.append("% (no pickpocket)");
          } else {
            buffer.append(" (no pickpocket, unknown drop rate)");
          }
        }
        case CONDITIONAL -> {
          if (rawDropRate > 0) {
            buffer.append(" ");
            buffer.append(rate1);
            buffer.append("% (conditional)");
          } else {
            buffer.append(" (conditional, unknown drop rate)");
          }
        }
        case FIXED -> {
          buffer.append(" ");
          buffer.append(rateRaw);
          buffer.append("% (no modifiers)");
        }
        case PICKPOCKET_ONLY -> {
          if (rawDropRate == 0) {
            buffer.append(" (pickpocket only, unknown rate)");
          } else if (stealing) {
            buffer.append(" ");
            buffer.append(Math.min(rawDropRate * pocketModifier, 100.0));
            buffer.append("% (pickpocket only)");
          } else {
            buffer.append(" (pickpocket only, cannot steal)");
          }
        }
        case STEAL_ACCORDION -> buffer.append(" (stealable accordion)");
        case MULTI_DROP -> buffer.append(" (multidrop)");
        default -> {
          if (stealing) {
            buffer.append(" ");
            buffer.append(rate2);
            buffer.append("% (");
            buffer.append(this.format(stealRate * 100.0));
            buffer.append("% steal, ");
            buffer.append(rate1);
            buffer.append("% drop)");
          } else {
            buffer.append(" ");
            buffer.append(rate1);
            buffer.append("%");
          }
        }
      }
    }
  }

  // Append facts from the book of facts if we have it.
  private void appendFact(
      final StringBuffer buffer, final MonsterData monster, boolean fullString) {
    String fact = monster.getFact();
    if (fact != null) {
      buffer.append(fact);
    }
    return;
  }

  public static double getDropRateModifier() {
    if (AreaCombatData.lastDropMultiplier != 0.0
        && KoLCharacter.getItemDropPercentAdjustment() == AreaCombatData.lastDropModifier) {
      return AreaCombatData.lastDropMultiplier;
    }

    AreaCombatData.lastDropModifier = KoLCharacter.getItemDropPercentAdjustment();
    AreaCombatData.lastDropMultiplier =
        Math.max(0.0, (100.0 + AreaCombatData.lastDropModifier) / 100.0);

    return AreaCombatData.lastDropMultiplier;
  }

  public static String elementColor(final Element element) {
    return switch (element) {
      case HOT -> (KoLmafiaGUI.isDarkTheme()) ? "#ff8a93" : "#ff0000";
      case COLD -> (KoLmafiaGUI.isDarkTheme()) ? "#00d4ff" : "#0000ff";
      case STENCH -> (KoLmafiaGUI.isDarkTheme()) ? "#39f0d0" : "#008000";
      case SPOOKY -> (KoLmafiaGUI.isDarkTheme()) ? "#bebebe" : "#808080";
      case SLEAZE -> (KoLmafiaGUI.isDarkTheme()) ? "#b980ee" : "#8a2be2";
      case SLIME -> (KoLmafiaGUI.isDarkTheme()) ? "#1adde9" : "#006400";
      default -> (KoLmafiaGUI.isDarkTheme()) ? "#FFFFFF" : "#000000";
    };
  }

  public static double hitPercent(final int attack, final int defense) {
    // ( (Attack - Defense) / 18 ) * 100 + 50 = Hit%
    double percent = 100.0 * (attack - defense) / 18 + 50.0;
    if (percent < 0.0) {
      return 0.0;
    }
    return Math.min(percent, 100.0);
  }

  public static int perfectHit(final int attack, final int defense) {
    return attack - defense - 9;
  }

  public String getZone() {
    return zone;
  }

  private static int adjustConditionalWeighting(String zone, String monster, int weighting) {
    // Bossbat can appear on 4th fight, and will always appear on the 8th fight
    switch (zone) {
      case "The Boss Bat's Lair" -> {
        int bossTurns = AdventureSpentDatabase.getTurns(zone);
        if (monster.equals("Boss Bat")) {
          return bossTurns > 3 && !QuestDatabase.isQuestLaterThan(Quest.BAT, "step3") ? 1 : 0;
        } else {
          return bossTurns > 7 || QuestDatabase.isQuestLaterThan(Quest.BAT, "step3") ? -4 : 1;
        }
      }
      case "The Hidden Park" -> {
        if (monster.equals("pygmy janitor")
            && Preferences.getInteger("relocatePygmyJanitor") != KoLCharacter.getAscensions()) {
          return -4;
        }
        if (monster.equals("pygmy witch lawyer")
            && Preferences.getInteger("relocatePygmyLawyer") != KoLCharacter.getAscensions()) {
          return -4;
        }
      }
      case "The Hidden Apartment Building",
          "The Hidden Hospital",
          "The Hidden Office Building",
          "The Hidden Bowling Alley" -> {
        if (monster.equals("pygmy janitor")
            && Preferences.getInteger("relocatePygmyJanitor") == KoLCharacter.getAscensions()) {
          return -4;
        }
        if (monster.equals("pygmy witch lawyer")
            && Preferences.getInteger("relocatePygmyLawyer") == KoLCharacter.getAscensions()) {
          return -4;
        }
        if (monster.equals("drunk pygmy") && Preferences.getInteger("_drunkPygmyBanishes") >= 11) {
          return -4;
        }
      }
      case "The Fungal Nethers" -> {
        switch (monster) {
          case "muscular mushroom guy":
            return KoLCharacter.isSealClubber() ? 1 : 0;
          case "armored mushroom guy":
            return KoLCharacter.isTurtleTamer() ? 1 : 0;
          case "wizardly mushroom guy":
            return KoLCharacter.isPastamancer() ? 1 : 0;
          case "fiery mushroom guy":
            return KoLCharacter.isSauceror() ? 1 : 0;
          case "dancing mushroom guy":
            return KoLCharacter.isDiscoBandit() ? 1 : 0;
          case "wailing mushroom guy":
            return KoLCharacter.isAccordionThief() ? 1 : 0;
        }
      }
      case "Pirates of the Garbage Barges" -> {
        if (monster.equals("flashy pirate") && !Preferences.getBoolean("dinseyGarbagePirate")) {
          return 0;
        }
      }
      case "Uncle Gator's Country Fun-Time Liquid Waste Sluice" -> {
        if (monster.equals("nasty bear") && QuestDatabase.isQuestStep(Quest.NASTY_BEARS, "step1")) {
          return 1;
        }
      }
      case "Throne Room" -> {
        if (monster.equals("Knob Goblin King") && QuestDatabase.isQuestFinished(Quest.GOBLIN)) {
          return 0;
        }
      }
      case "The Defiled Alcove" -> {
        int evilness = Preferences.getInteger("cyrptAlcoveEvilness");
        if (monster.equals("conjoined zmombie")) {
          return evilness > 0 && evilness <= 13 ? 1 : 0;
        } else if (!monster.equals("modern zmobie")) {
          return evilness > 13 ? 1 : 0;
        }
      }
      case "The Defiled Cranny" -> {
        int evilness = Preferences.getInteger("cyrptCrannyEvilness");
        if (monster.equals("huge ghuol")) {
          return evilness > 0 && evilness <= 13 ? 1 : 0;
        } else if (monster.equals("gaunt ghuol") || monster.equals("gluttonous ghuol")) {
          return evilness > 13 ? 1 : 0;
        }
      }
      case "The Defiled Niche" -> {
        int evilness = Preferences.getInteger("cyrptNicheEvilness");
        if (monster.equals("gargantulihc")) {
          return evilness > 0 && evilness <= 13 ? 1 : 0;
        } else {
          return evilness > 13 ? 1 : 0;
        }
      }
      case "The Defiled Nook" -> {
        int evilness = Preferences.getInteger("cyrptNookEvilness");
        if (monster.equals("giant skeelton")) {
          return evilness > 0 && evilness <= 13 ? 1 : 0;
        } else {
          return evilness > 13 ? 1 : 0;
        }
      }
      case "Haert of the Cyrpt" -> {
        if (monster.equals("Bonerdagon")
            && QuestDatabase.isQuestLaterThan(Quest.CYRPT, QuestDatabase.STARTED)) {
          return 0;
        }
      }
      case "The F'c'le" -> {
        if (monster.equals("clingy pirate (female)")) {
          return KoLCharacter.getGender() == Gender.MALE ? 1 : 0;
        } else if (monster.equals("clingy pirate (male)")) {
          return KoLCharacter.getGender() == Gender.FEMALE ? 1 : 0;
        }
      }
      case "Summoning Chamber" -> {
        if (monster.equals("Lord Spookyraven") && QuestDatabase.isQuestFinished(Quest.MANOR)) {
          return 0;
        }
      }
      case "An Overgrown Shrine (Northwest)" -> {
        // Assume lianas are dealt with once Apartment opened. Player may leave without doing so,
        // but that's a bit niche for me to care!
        if (monster.equals("dense liana")
            && Preferences.getInteger("hiddenApartmentProgress") > 0) {
          return 0;
        }
      }
      case "An Overgrown Shrine (Northeast)" -> {
        // Assume lianas are dealt with once Office opened. Player may leave without doing so, but
        // that's a bit niche for me to care!
        if (monster.equals("dense liana") && Preferences.getInteger("hiddenOfficeProgress") > 0) {
          return 0;
        }
      }
      case "An Overgrown Shrine (Southwest)" -> {
        // Assume lianas are dealt with once Hospital opened. Player may leave without doing so, but
        // that's a bit niche for me to care!
        if (monster.equals("dense liana") && Preferences.getInteger("hiddenHospitalProgress") > 0) {
          return 0;
        }
      }
      case "An Overgrown Shrine (Southeast)" -> {
        // Assume lianas are dealt with once Bowling Alley opened. Player may leave without doing
        // so, but that's a bit niche for me to care!
        if (monster.equals("dense liana")
            && Preferences.getInteger("hiddenBowlingAlleyProgress") > 0) {
          return 0;
        }
      }
      case "A Massive Ziggurat" -> {
        // Assume lianas dealt with after 3 turns, won't always be right, but this is a bit niche
        // for special tracking
        int zoneTurns = AdventureSpentDatabase.getTurns(zone);
        if (monster.equals("dense liana")
            && (zoneTurns >= 3 || QuestDatabase.isQuestFinished(Quest.WORSHIP))) {
          return 0;
        } else if (monster.equals("Protector Spectre")
            && QuestDatabase.isQuestStep(Quest.WORSHIP, "step4")) {
          return 1;
        }
      }
      case "Oil Peak" -> {
        int monsterLevel = (int) KoLCharacter.currentNumericModifier(DoubleModifier.MONSTER_LEVEL);
        return switch (monster) {
          case "oil slick" -> monsterLevel < 20 ? 1 : 0;
          case "oil tycoon" -> monsterLevel >= 20 && monsterLevel < 50 ? 1 : 0;
          case "oil baron" -> monsterLevel >= 50 && monsterLevel < 100 ? 1 : 0;
          case "oil cartel" -> monsterLevel >= 100 ? 1 : 0;
          default -> weighting;
        };
      }
      case "The Battlefield (Frat Uniform)" -> {
        int hippiesDefeated = Preferences.getInteger("hippiesDefeated");

        // If the battlefield is cleared, only the boss can appear
        if (hippiesDefeated == 1000) {
          return monster.equals("The Big Wisniewski") ? 1 : 0;
        }
        return switch (monster) {
            // Junkyard quest completed as hippy
          case "Bailey's Beetle" -> Preferences.getString("sidequestJunkyardCompleted")
                  .equals("hippy")
              ? 1
              : 0;
            // After specific number of hippies defeated
          case "Green Ops Soldier" -> hippiesDefeated >= 401 ? 1 : 0;
          case "Mobile Armored Sweat Lodge" -> hippiesDefeated >= 151 ? 1 : 0;
          case "War Hippy Airborne Commander" -> hippiesDefeated >= 351 ? 1 : 0;
          case "War Hippy Baker" -> hippiesDefeated <= 600 ? 2 : 0;
          case "War Hippy Dread Squad" -> hippiesDefeated <= 850 ? 1 : 0;
          case "War Hippy Elder Shaman" -> hippiesDefeated >= 251 ? 1 : 0;
          case "War Hippy Elite Fire Spinner" -> hippiesDefeated >= 501 ? 1 : 0;
          case "War Hippy Elite Rigger" -> hippiesDefeated >= 301 ? 2 : 0;
          case "War Hippy F.R.O.G." -> hippiesDefeated >= 51 && hippiesDefeated <= 500 ? 2 : 0;
          case "War Hippy Fire Spinner" -> hippiesDefeated >= 301 && hippiesDefeated <= 650 ? 1 : 0;
          case "War Hippy Green Gourmet" -> hippiesDefeated >= 201 && hippiesDefeated <= 750
              ? 2
              : 0;
          case "War Hippy Homeopath" -> hippiesDefeated <= 900 ? 1 : 0;
          case "War Hippy Infantryman" -> hippiesDefeated <= 400 ? 2 : 0;
          case "War Hippy Naturopathic Homeopath" -> hippiesDefeated >= 451 ? 1 : 0;
          case "War Hippy Rigger" -> hippiesDefeated <= 800 ? 2 : 0;
          case "War Hippy Shaman" -> hippiesDefeated >= 26 && hippiesDefeated <= 700 ? 1 : 0;
          case "War Hippy Sky Captain" -> hippiesDefeated >= 76 && hippiesDefeated <= 550 ? 1 : 0;
          case "War Hippy Windtalker" -> hippiesDefeated > 0 ? 1 : 0;
            // Hippy Heroes only appear in specific range. Very low encounter chance
          case "Slow Talkin' Elliot" -> hippiesDefeated >= 501 && hippiesDefeated <= 600 ? -1 : 0;
          case "Neil" -> hippiesDefeated >= 601 && hippiesDefeated <= 700 ? -1 : 0;
          case "Zim Merman" -> hippiesDefeated >= 701 && hippiesDefeated <= 800 ? -1 : 0;
          case "C.A.R.N.I.V.O.R.E. Operative" -> hippiesDefeated >= 801 && hippiesDefeated <= 900
              ? -1
              : 0;
          case "Glass of Orange Juice" -> hippiesDefeated >= 901 && hippiesDefeated <= 999 ? -1 : 0;
          default -> weighting;
        };
      }
      case "The Battlefield (Hippy Uniform)" -> {
        int fratboysDefeated = Preferences.getInteger("fratboysDefeated");

        // If the battlefield is cleared, only the boss can appear
        if (fratboysDefeated == 1000) {
          return monster.equals("The Man") ? 1 : 0;
        }

        return switch (monster) {
            // Junkyard quest completed as fratboy
          case "War Frat Mobile Grill Unit" -> Preferences.getString("sidequestJunkyardCompleted")
                  .equals("fratboy")
              ? 1
              : 0;
            // After specific number of fratboys defeated (todo: has not been spaded)
          case "Sorority Operator" -> fratboysDefeated >= 151 ? 1 : 0;
          case "Panty Raider Frat Boy" -> fratboysDefeated >= 401 ? 1 : 0;
            // Fratboy Heroes only appear in specific range. Very low encounter chance
          case "Next-generation Frat Boy" -> fratboysDefeated >= 501 && fratboysDefeated <= 600
              ? -1
              : 0;
          case "Monty Basingstoke-Pratt, IV" -> fratboysDefeated >= 601 && fratboysDefeated <= 700
              ? -1
              : 0;
          case "Brutus, the toga-clad lout" -> fratboysDefeated >= 701 && fratboysDefeated <= 800
              ? -1
              : 0;
          case "Danglin' Chad" -> fratboysDefeated >= 801 && fratboysDefeated <= 900 ? -1 : 0;
          case "War Frat Streaker" -> fratboysDefeated >= 901 && fratboysDefeated <= 999 ? -1 : 0;
          default -> weighting;
        };
      }
      case "Fastest Adventurer Contest" -> {
        int opponentsLeft = Preferences.getInteger("nsContestants1");
        if (monster.equals("Tasmanian Dervish")) {
          return opponentsLeft == 1 ? 1 : 0;
        } else {
          return opponentsLeft > 1 ? 1 : 0;
        }
      }
      case "Strongest Adventurer Contest" -> {
        int opponentsLeft =
            Preferences.getString("nsChallenge1").equals("Muscle")
                ? Preferences.getInteger("nsContestants2")
                : 0;
        if (monster.equals("Mr. Loathing")) {
          return opponentsLeft == 1 ? 1 : 0;
        } else {
          return opponentsLeft > 1 ? 1 : 0;
        }
      }
      case "Smartest Adventurer Contest" -> {
        int opponentsLeft =
            Preferences.getString("nsChallenge1").equals("Mysticality")
                ? Preferences.getInteger("nsContestants2")
                : 0;
        if (monster.equals("The Mastermind")) {
          return opponentsLeft == 1 ? 1 : 0;
        } else {
          return opponentsLeft > 1 ? 1 : 0;
        }
      }
      case "Smoothest Adventurer Contest" -> {
        int opponentsLeft =
            Preferences.getString("nsChallenge1").equals("Muscle")
                ? Preferences.getInteger("nsContestants2")
                : 0;
        if (monster.equals("Seannery the Conman")) {
          return opponentsLeft == 1 ? 1 : 0;
        } else {
          return opponentsLeft > 1 ? 1 : 0;
        }
      }
      case "Coldest Adventurer Contest" -> {
        int opponentsLeft =
            Preferences.getString("nsChallenge2").equals("cold")
                ? Preferences.getInteger("nsContestants3")
                : 0;
        if (monster.equals("Mrs. Freeze")) {
          return opponentsLeft == 1 ? 1 : 0;
        } else {
          return opponentsLeft > 1 ? 1 : 0;
        }
      }
      case "Hottest Adventurer Contest" -> {
        int opponentsLeft =
            Preferences.getString("nsChallenge2").equals("hot")
                ? Preferences.getInteger("nsContestants3")
                : 0;
        if (monster.equals("Mrs. Freeze")) {
          return opponentsLeft == 1 ? 1 : 0;
        } else {
          return opponentsLeft > 1 ? 1 : 0;
        }
      }
      case "Sleaziest Adventurer Contest" -> {
        int opponentsLeft =
            Preferences.getString("nsChallenge2").equals("sleaze")
                ? Preferences.getInteger("nsContestants3")
                : 0;
        if (monster.equals("Leonard")) {
          return opponentsLeft == 1 ? 1 : 0;
        } else {
          return opponentsLeft > 1 ? 1 : 0;
        }
      }
      case "Spookiest Adventurer Contest" -> {
        int opponentsLeft =
            Preferences.getString("nsChallenge2").equals("spooky")
                ? Preferences.getInteger("nsContestants3")
                : 0;
        if (monster.equals("Arthur Frankenstein")) {
          return opponentsLeft == 1 ? 1 : 0;
        } else {
          return opponentsLeft > 1 ? 1 : 0;
        }
      }
      case "Stinkiest Adventurer Contest" -> {
        int opponentsLeft =
            Preferences.getString("nsChallenge2").equals("stinky")
                ? Preferences.getInteger("nsContestants3")
                : 0;
        if (monster.equals("Odorous Humongous")) {
          return opponentsLeft == 1 ? 1 : 0;
        } else {
          return opponentsLeft > 1 ? 1 : 0;
        }
      }
      case "The Nemesis' Lair" -> {
        int lairTurns = AdventureSpentDatabase.getTurns(zone);
        return switch (monster) {
          case "hellseal guardian" -> KoLCharacter.isSealClubber() ? 1 : 0;
          case "Gorgolok, the Infernal Seal (Inner Sanctum)" -> KoLCharacter.isSealClubber()
                  && lairTurns >= 4
              ? 1
              : 0;
          case "warehouse worker" -> KoLCharacter.isTurtleTamer() ? 1 : 0;
          case "Stella, the Turtle Poacher (Inner Sanctum)" -> KoLCharacter.isTurtleTamer()
                  && lairTurns >= 4
              ? 1
              : 0;
          case "evil spaghetti cult zealot" -> KoLCharacter.isPastamancer() ? 1 : 0;
          case "Spaghetti Elemental (Inner Sanctum)" -> KoLCharacter.isPastamancer()
                  && lairTurns >= 4
              ? 1
              : 0;
          case "security slime" -> KoLCharacter.isSauceror() ? 1 : 0;
          case "Lumpy, the Sinister Sauceblob (Inner Sanctum)" -> KoLCharacter.isSauceror()
                  && lairTurns >= 4
              ? 1
              : 0;
          case "daft punk" -> KoLCharacter.isDiscoBandit() ? 1 : 0;
          case "Spirit of New Wave (Inner Sanctum)" -> KoLCharacter.isDiscoBandit()
                  && lairTurns >= 4
              ? 1
              : 0;
          case "mariachi bruiser" -> KoLCharacter.isAccordionThief() ? 1 : 0;
          case "Somerset Lopez, Dread Mariachi (Inner Sanctum)" -> KoLCharacter.isAccordionThief()
                  && lairTurns >= 4
              ? 1
              : 0;
          default -> weighting;
        };
      }
      case "The Slime Tube" -> {
        int monsterLevel = (int) KoLCharacter.currentNumericModifier(DoubleModifier.MONSTER_LEVEL);
        return switch (monster) {
          case "Slime" -> monsterLevel <= 100 ? 1 : 0;
          case "Slime Hand" -> monsterLevel > 100 && monsterLevel <= 300 ? 1 : 0;
          case "Slime Mouth" -> monsterLevel > 300 && monsterLevel <= 600 ? 1 : 0;
          case "Slime Construct" -> monsterLevel > 600 && monsterLevel <= 1000 ? 1 : 0;
          case "Slime Colossus" -> monsterLevel > 1000 ? 1 : 0;
          default -> weighting;
        };
      }
      case "The Post-Mall" -> {
        int mallTurns = AdventureSpentDatabase.getTurns(zone);
        if (monster.equals("sentient ATM")) {
          return mallTurns == 11 ? 1 : 0;
        } else {
          return mallTurns == 11 ? -4 : 1;
        }
      }
      case "Investigating a Plaintive Telegram" -> {
        String quest = Preferences.getString("lttQuestName");
        String questStep = Preferences.getString("questLTTQuestByWire");
        return switch (monster) {
          case "drunk cowpoke" -> (quest.equals("Missing: Fancy Man") && questStep.equals("step1"))
                  || (quest.equals("Help!  Desperados|") && questStep.equals("step1"))
                  || (quest.equals("Big Gambling Tournament Announced")
                      && questStep.equals("step1"))
                  || (quest.equals("Sheriff Wanted") && questStep.equals("step1"))
                  || (quest.equals("Madness at the Mine") && questStep.equals("step1"))
              ? 1
              : 0;
          case "surly gambler" -> (quest.equals("Missing: Fancy Man") && questStep.equals("step1"))
                  || (quest.equals("Big Gambling Tournament Announced")
                      && questStep.equals("step3"))
                  || (quest.equals("Sheriff Wanted") && questStep.equals("step1"))
              ? 1
              : 0;
          case "wannabe gunslinger" -> (quest.equals("Help!  Desperados|")
                      && questStep.equals("step1"))
                  || (quest.equals("Big Gambling Tournament Announced")
                      && questStep.equals("step1"))
                  || (quest.equals("Sheriff Wanted") && questStep.equals("step1"))
                  || (quest.equals("Wagon Train Escort Wanted") && questStep.equals("step3"))
              ? 1
              : 0;
          case "cow cultist" -> (quest.equals("Missing: Pioneer Daughter")
                      && questStep.equals("step2"))
                  || (quest.equals("Haunted Boneyard") && questStep.equals("step3"))
                  || (quest.equals("Sheriff Wanted") && questStep.equals("step2"))
                  || (quest.equals("Missing: Many Children") && questStep.equals("step1"))
              ? 1
              : 0;
          case "hired gun" -> (quest.equals("Missing: Fancy Man") && questStep.equals("step1"))
                  || (quest.equals("Help!  Desperados|") && questStep.equals("step1"))
                  || (quest.equals("Missing: Pioneer Daughter") && questStep.equals("step2"))
                  || (quest.equals("Big Gambling Tournament Announced")
                      && questStep.equals("step3"))
                  || (quest.equals("Sheriff Wanted") && questStep.equals("step3"))
                  || (quest.equals("Missing: Many Children") && questStep.equals("step1"))
                  || (quest.equals("Wagon Train Escort Wanted") && questStep.equals("step3"))
              ? 1
              : 0;
          case "camp cook" -> (quest.equals("Missing: Fancy Man") && questStep.equals("step2"))
                  || (quest.equals("Sheriff Wanted") && questStep.equals("step3"))
                  || (quest.equals("Madness at the Mine") && questStep.equals("step1"))
                  || (quest.equals("Wagon Train Escort Wanted") && questStep.equals("step3"))
              ? 1
              : 0;
          case "skeletal gunslinger" -> (quest.equals("Help!  Desperados|")
                      && questStep.equals("step3"))
                  || (quest.equals("Haunted Boneyard") && questStep.equals("step1"))
                  || (quest.equals("Madness at the Mine") && questStep.equals("step3"))
                  || (quest.equals("Wagon Train Escort Wanted") && questStep.equals("step2"))
              ? 1
              : 0;
          case "restless ghost" -> (quest.equals("Missing: Fancy Man") && questStep.equals("step3"))
                  || (quest.equals("Missing: Pioneer Daughter") && questStep.equals("step1"))
                  || (quest.equals("Haunted Boneyard") && questStep.equals("step2"))
                  || (quest.equals("Madness at the Mine") && questStep.equals("step3"))
                  || (quest.equals("Missing: Many Children") && questStep.equals("step2"))
                  || (quest.equals("Wagon Train Escort Wanted") && questStep.equals("step2"))
              ? 1
              : 0;
          case "buzzard" -> (quest.equals("Missing: Fancy Man") && questStep.equals("step2"))
                  || (quest.equals("Help! Desperados|") && questStep.equals("step2"))
                  || (quest.equals("Missing: Pioneer Daughter") && questStep.equals("step1"))
                  || (quest.equals("Haunted Boneyard") && questStep.equals("step1"))
              ? 1
              : 0;
          case "mountain lion" -> (quest.equals("Missing: Fancy Man") && questStep.equals("step2"))
                  || (quest.equals("Help!  Desperados|") && questStep.equals("step2"))
                  || (quest.equals("Sheriff Wanted") && questStep.equals("step2"))
                  || (quest.equals("Madness at the Mine") && questStep.equals("step2"))
                  || (quest.equals("Wagon Train Escort Wanted") && questStep.equals("step1"))
              ? 1
              : 0;
          case "grizzled bear" -> (quest.equals("Help!  Desperados|") && questStep.equals("step3"))
                  || (quest.equals("Madness at the Mine") && questStep.equals("step3"))
                  || (quest.equals("Wagon Train Escort Wanted") && questStep.equals("step1"))
              ? 1
              : 0;
          case "diamondback rattler" -> (quest.equals("Help!  Desperados|")
                      && questStep.equals("step2"))
                  || (quest.equals("Big Gambling Tournament Announced")
                      && questStep.equals("step2"))
                  || (quest.equals("Madness at the Mine") && questStep.equals("step2"))
                  || (quest.equals("Wagon Train Escort Wanted") && questStep.equals("step1"))
              ? 1
              : 0;
          case "coal snake" -> (quest.equals("Missing: Fancy Man") && questStep.equals("step3"))
                  || (quest.equals("Big Gambling Tournament Announced")
                      && questStep.equals("step2"))
                  || (quest.equals("Madness at the Mine") && questStep.equals("step1"))
              ? 1
              : 0;
          case "frontwinder" -> (quest.equals("Big Gambling Tournament Announced")
                      && questStep.equals("step2"))
                  || (quest.equals("Sheriff Wanted") && questStep.equals("step2"))
              ? 1
              : 0;
          case "caugr" -> (quest.equals("Missing: Pioneer Daughter") && questStep.equals("step3"))
                  || (quest.equals("Missing: Many Children") && questStep.equals("step3"))
              ? 1
              : 0;
          case "pyrobove" -> (quest.equals("Missing: Pioneer Daughter")
                      && questStep.equals("step3"))
                  || (quest.equals("Missing: Many Children") && questStep.equals("step3"))
                  || (quest.equals("Wagon Train Escort Wanted") && questStep.equals("step2"))
              ? 1
              : 0;
          case "spidercow" -> (quest.equals("Missing: Pioneer Daughter")
                      && questStep.equals("step3"))
                  || (quest.equals("Haunted Boneyard") && questStep.equals("step3"))
                  || (quest.equals("Missing: Many Children") && questStep.equals("step1"))
              ? 1
              : 0;
          case "moomy" -> (quest.equals("Haunted Boneyard") && questStep.equals("step3"))
                  || (quest.equals("Madness at the Mine") && questStep.equals("step2"))
                  || (quest.equals("Missing: Many Children") && questStep.equals("step3"))
              ? 1
              : 0;
          case "Jeff the Fancy Skeleton" -> (quest.equals("Missing: Fancy Man")
                  && questStep.equals("step4"))
              ? 1
              : 0;
          case "Daisy the Unclean" -> (quest.equals("Missing: Pioneer Daughter")
                  && questStep.equals("step4"))
              ? 1
              : 0;
          case "Pecos Dave" -> (quest.equals("Help!  Desperados|") && questStep.equals("step4"))
              ? 1
              : 0;
          case "Pharaoh Amoon-Ra Cowtep" -> (quest.equals("Haunted Boneyard")
                  && questStep.equals("step4"))
              ? 1
              : 0;
          case "Snake-Eyes Glenn" -> (quest.equals("Big Gambling Tournament Announced")
                  && questStep.equals("step4"))
              ? 1
              : 0;
          case "Former Sheriff Dan Driscoll" -> (quest.equals("Sheriff Wanted")
                  && questStep.equals("step4"))
              ? 1
              : 0;
          case "unusual construct" -> (quest.equals("Madness at the Mine")
                  && questStep.equals("step4"))
              ? 1
              : 0;
          case "Clara" -> (quest.equals("Missing: Many Children") && questStep.equals("step4"))
              ? 1
              : 0;
          case "Granny Hackleton" -> (quest.equals("Wagon Train Escort Wanted")
                  && questStep.equals("step4"))
              ? 1
              : 0;
          default -> weighting;
        };
      }
      case "Gingerbread Civic Center",
          "Gingerbread Train Station",
          "Gingerbread Industrial Zone",
          "Gingerbread Upscale Retail District" -> {
        if (monster.equals("gingerbread pigeon") || monster.equals("gingerbread rat")) {
          return Preferences.getBoolean("gingerSewersUnlocked") ? 0 : 1;
        }
      }
      case "The Canadian Wildlife Preserve" -> {
        if (monster.equals("wild reindeer")) {
          return KoLCharacter.getFamiliar().getId() != FamiliarPool.YULE_HOUND ? 0 : 1;
        }
      }
      case "The Clumsiness Grove" -> {
        if (monster.equals("The Bat in the Spats") || monster.equals("The Thorax")) {
          return (monster.equals(Preferences.getString("clumsinessGroveBoss"))) ? 1 : 0;
        }
      }
      case "The Maelstrom of Lovers" -> {
        if (monster.equals("The Terrible Pinch") || monster.equals("Thug 1 and Thug 2")) {
          return (monster.equals(Preferences.getString("maelstromOfLoversBoss"))) ? 1 : 0;
        }
      }
      case "The Glacier of Jerks" -> {
        if (monster.equals("Mammon the Elephant") || monster.equals("The Large-Bellied Snitch")) {
          return (monster.equals(Preferences.getString("glacierOfJerksBoss"))) ? 1 : 0;
        }
      }
      case "The Jungles of Ancient Loathing" -> {
        if (monster.equals("evil cultist")) {
          return QuestDatabase.isQuestFinished(Quest.PRIMORDIAL) ? 1 : 0;
        }
      }
      case "Seaside Megalopolis" -> {
        return switch (monster) {
          case "cyborg policeman" -> (InventoryManager.hasItem(ItemPool.MULTI_PASS)
                  && !QuestDatabase.isQuestFinished(Quest.FUTURE))
              ? 1
              : 0;
          case "obese tourist", "terrifying robot" -> QuestDatabase.isQuestLaterThan(
                  Quest.FUTURE, "step1")
              ? 1
              : 0;
          default -> weighting;
        };
      }

      case "The SMOOCH Army HQ" -> {
        var combats = Preferences.getInteger("_smoochArmyHQCombats");
        var minimum =
            switch (monster) {
              case "SMOOCH sergeant" -> 20;
              case "SMOOCH general" -> 40;
              default -> 0;
            };
        return combats >= minimum ? weighting : 0;
      }

      case "Shadow Rift" -> {
        var ingress = Preferences.getString("shadowRiftIngress");
        return switch (monster) {
              case "shadow bat" -> ingress.equals("manor3")
                  || ingress.equals("pyramid")
                  || ingress.equals("plains")
                  || ingress.equals("giantcastle");
              case "shadow cow" -> ingress.equals("mclargehuge")
                  || ingress.equals("plains")
                  || ingress.equals("town_right");
              case "shadow devil" -> ingress.equals("desertbeach")
                  || ingress.equals("manor3")
                  || ingress.equals("woods");
              case "shadow guy" -> ingress.equals("forestvillage")
                  || ingress.equals("town_right")
                  || ingress.equals("giantcastle")
                  || ingress.equals("cemetery");
              case "shadow hexagon" -> ingress.equals("mclargehuge")
                  || ingress.equals("8bit")
                  || ingress.equals("forestvillage");
              case "shadow orb" -> ingress.equals("desertbeach")
                  || ingress.equals("8bit")
                  || ingress.equals("beanstalk")
                  || ingress.equals("giantcastle");
              case "shadow prism" -> ingress.equals("8bit")
                  || ingress.equals("town_right")
                  || ingress.equals("beanstalk");
              case "shadow slab" -> ingress.equals("pyramid")
                  || ingress.equals("hiddencity")
                  || ingress.equals("cemetery");
              case "shadow spider" -> ingress.equals("manor3")
                  || ingress.equals("forestvillage")
                  || ingress.equals("plains");
              case "shadow snake" -> ingress.equals("desertbeach")
                  || ingress.equals("pyramid")
                  || ingress.equals("hiddencity");
              case "shadow stalk" -> ingress.equals("hiddencity")
                  || ingress.equals("beanstalk")
                  || ingress.equals("woods");
              case "shadow tree" -> ingress.equals("mclargehuge")
                  || ingress.equals("woods")
                  || ingress.equals("cemetery");
                // If you somehow get another monster here, assume it's not affected by ingress
                // point
              default -> true;
            }
            ? weighting
            : -4;
      }
      case "Abuela's Cottage (Contested)",
          "The Embattled Factory",
          "The Bar At War",
          "A Cafe Divided",
          "The Armory Up In Arms" -> {
        final AdventureResult hat = EquipmentManager.getEquipment(Slot.HAT);
        final AdventureResult pants = EquipmentManager.getEquipment(Slot.PANTS);
        final boolean elfOutfit =
            hat.getItemId() == ItemPool.ELF_GUARD_PATROL_CAP
                && pants.getItemId() == ItemPool.ELF_GUARD_HOTPANTS;
        final boolean pirateOutfit =
            hat.getItemId() == ItemPool.CRIMBUCCANEER_TRICORN
                && pants.getItemId() == ItemPool.CRIMBUCCANEER_BREECHES;
        final boolean elfMonster = monster.startsWith("Elf Guard");
        final boolean pirateMonster = monster.startsWith("Crimbuccaneer");
        return switch (monster) {
          case "Crimbuccaneer military school dropout",
              "Crimbuccaneer new recruit",
              "Crimbuccaneer privateer",
              "Elf Guard conscript",
              "Elf Guard convict",
              "Elf Guard private" -> !elfOutfit && !pirateOutfit ? 1 : 0;
          default -> (elfOutfit && pirateMonster) || (pirateOutfit && elfMonster) ? 1 : 0;
        };
      }
      case "The Brinier Deepers" -> {
        if (monster.equals("trophyfish") && !Preferences.getBoolean("grandpaUnlockedTrophyFish"))
          return 0;
      }
      case "The Wreck of the Edgar Fitzsimmons" -> {
        var hatchTurn = Preferences.getInteger("_lastFitzsimmonsHatch");
        var hatchOpen =
            hatchTurn >= 0
                && KoLCharacter.getTurnsPlayed() - Preferences.getInteger("_lastFitzsimmonsHatch")
                    < 20;
        var present =
            switch (monster) {
              case "cargo crab", "drowned sailor" -> !hatchOpen;
              case "mine crab", "unholy diver" -> hatchOpen;
              default -> true;
            };
        return present ? weighting : 0;
      }
    }
    return weighting;
  }

  public static double superlikelyChance(MonsterData monster) {
    return superlikelyChance(monster.getName());
  }

  public static double superlikelyChance(String monster) {
    return switch (monster) {
      case "screambat" -> {
        int turns =
            AdventureSpentDatabase.getTurns("Guano Junction")
                + AdventureSpentDatabase.getTurns("The Batrat and Ratbat Burrow")
                + AdventureSpentDatabase.getTurns("The Beanbat Chamber");
        // Appears every 8 turns in relevant zones
        yield turns > 0 && (turns % 8) == 0 ? 100.0 : 0.0;
      }
      case "modern zmobie" -> {
        if (Preferences.getInteger("cyrptAlcoveEvilness") > 13) {
          // Chance based on initiative
          double chance = 15 + KoLCharacter.getInitiativeAdjustment() / 10;
          yield chance < 0 ? 0.0 : chance > 100 ? 100.0 : chance;
        } else {
          yield 0;
        }
      }
      case "ninja snowman assassin" -> {
        // Do not appear without positive combat rate
        double combatRate = KoLCharacter.getCombatRateAdjustment();
        if (combatRate <= 0) {
          yield 0;
        }
        // Guaranteed on turns 11, 21, and 31
        int snowmanTurns = AdventureSpentDatabase.getTurns("Lair of the Ninja Snowmen");
        if (snowmanTurns == 10 || snowmanTurns == 20 || snowmanTurns == 30) {
          yield 100.0;
        }
        double chance = combatRate / 2 + (double) snowmanTurns * 1.5;
        yield chance < 0 ? 0.0 : chance > 100 ? 100.0 : chance;
      }
      case "mother hellseal" -> {
        double chance = Preferences.getInteger("_sealScreeches") * 10;
        yield chance < 0 ? 0.0 : chance > 100 ? 100.0 : chance;
      }
      case "Brick Mulligan, the Bartender" -> {
        int kokomoTurns = AdventureSpentDatabase.getTurns("Kokomo Resort");
        // Appears every 25 turns
        yield kokomoTurns > 0 && (kokomoTurns % 25) == 0 ? 100.0 : 0.0;
      }
      default -> 0;
    };
  }
}
