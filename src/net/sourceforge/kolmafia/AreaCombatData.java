package net.sourceforge.kolmafia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureQueueDatabase;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.persistence.BountyDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.session.BanishManager;
import net.sourceforge.kolmafia.session.EncounterManager;
import net.sourceforge.kolmafia.session.EncounterManager.EncounterType;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.TurnCounter;
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

    for (MonsterData monster : monsters) {
      // Weighting has two low bits which represent odd or even ascension restriction
      // Strip them out now and restore them at the end
      int baseWeighting = this.baseWeightings.get(monster);
      int flags = baseWeighting & 3;
      baseWeighting = baseWeighting >> WEIGHT_SHIFT;

      String monsterName = monster.getName();
      Phylum monsterPhylum = monster.getPhylum();

      baseWeighting = AreaCombatData.adjustConditionalWeighting(zone, monsterName, baseWeighting);
      int currentWeighting = baseWeighting;

      // If olfacted, add three to encounter pool
      if (Preferences.getString("olfactedMonster").equals(monsterName)
          && KoLConstants.activeEffects.contains(FightRequest.ONTHETRAIL)) {
        currentWeighting += 3 * baseWeighting;
      }
      // If Nosy Nose sniffed, and is current familiar, add one to encounter pool
      if (Preferences.getString("nosyNoseMonster").equals(monsterName)
          && KoLCharacter.getFamiliar().getId() == FamiliarPool.NOSY_NOSE) {
        currentWeighting += baseWeighting;
      }
      // If Red Snapper tracks its phylum, and is current familiar, add two to encounter pool
      if (Preferences.getString("redSnapperPhylum").equals(monsterPhylum.toString())
          && KoLCharacter.getFamiliar().getId() == FamiliarPool.RED_SNAPPER) {
        currentWeighting += 2 * baseWeighting;
      }
      // If any relevant Daily Candle familiar-tracking potions are active, add two(?) to the
      // encounter pool
      if ((monsterPhylum.equals(Phylum.HUMANOID)
              && KoLConstants.activeEffects.contains(EW_THE_HUMANITY))
          || (monsterPhylum.equals(Phylum.BEAST)
              && KoLConstants.activeEffects.contains(A_BEASTLY_ODOR))) {
        currentWeighting += 2 * baseWeighting;
      }
      // If Gallapagosian Mating Call used, add one to encounter pool
      if (Preferences.getString("_gallapagosMonster").equals(monsterName)) {
        currentWeighting += baseWeighting;
      }
      // If Offer Latte to Opponent used, add two to encounter pool
      if (Preferences.getString("_latteMonster").equals(monsterName)
          && TurnCounter.isCounting("Latte Monster")) {
        currentWeighting += 2 * baseWeighting;
      }
      // If Superficially Interested used, add three to encounter pool
      if (Preferences.getString("superficiallyInterestedMonster").equals(monsterName)
          && TurnCounter.isCounting("Superficially Interested Monster")) {
        currentWeighting += 3 * baseWeighting;
      }
      // If Staff of the Cream of the Cream jiggle, add two to encounter pool
      if (Preferences.getString("_jiggleCreamedMonster").equals(monsterName)) {
        currentWeighting += 2 * baseWeighting;
      }
      // If Make Friends used, add three to encounter pool
      if (Preferences.getString("makeFriendsMonster").equals(monsterName)) {
        currentWeighting += 3 * baseWeighting;
      }
      // If Curse of Stench used, add three to encounter pool
      if (Preferences.getString("stenchCursedMonster").equals(monsterName)) {
        currentWeighting += 3 * baseWeighting;
      }
      // If Long Con used, add three to encounter pool
      if (Preferences.getString("longConMonster").equals(monsterName)) {
        currentWeighting += 3 * baseWeighting;
      }

      if (BanishManager.isBanished(monsterName)) {
        // Banishing reduces number of copies
        currentWeighting -= baseWeighting;
        // If this takes it to zero chance, it's properly banished
        if (currentWeighting == 0) {
          currentWeighting = -3;
        }
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

      if (weight.length() == 0) {
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

      // Only one flag per monster is is supported
      if (flag != null) {
        switch (flag.charAt(0)) {
          case 'e':
            flags = ASCENSION_EVEN;
            break;
          case 'o':
            flags = ASCENSION_ODD;
            break;
          case 'r':
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
            break;
          default:
            KoLmafia.updateDisplay(
                "Unknown flag " + flag.charAt(0) + " specified for " + name + " in combats.txt.");
            return false;
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
      this.baseWeightings.put(monster, IntegerPool.get((weighting << WEIGHT_SHIFT) | flags));
      this.currentWeightings.put(monster, IntegerPool.get((weighting << WEIGHT_SHIFT) | flags));
      this.rejection.put(monster, IntegerPool.get(rejection));
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

  /**
   * Counts the number of monsters in this area that drop the item with the given ID.
   *
   * @param itemId the itemID of the the item to count
   * @return the number of monsters in this area dropping the item
   */
  public int countMonstersDroppingItem(final int itemId) {
    int total = 0;

    for (MonsterData monster : this.monsters) {
      for (AdventureResult item : monster.getItems()) {
        if (item.getItemId() == itemId) {
          total++;
          break;
        }
      }
    }

    for (MonsterData monster : this.superlikelyMonsters) {
      for (AdventureResult item : monster.getItems()) {
        if (item.getItemId() == itemId) {
          total++;
          break;
        }
      }
    }

    return total;
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

  public int getMonsterIndex(MonsterData monster) {
    return this.monsters.indexOf(monster);
  }

  public int getSuperlikelyMonsterIndex(MonsterData monster) {
    return this.superlikelyMonsters.indexOf(monster);
  }

  public int getWeighting(final MonsterData monster) {
    int raw = (this.currentWeightings.getOrDefault(monster, 0)).intValue();
    if (((raw >> (KoLCharacter.getAscensions() & 1)) & 1) == 0) {
      return -2; // impossible this ascension
    }
    return raw >> WEIGHT_SHIFT;
  }

  public int getRejection(final MonsterData monster) {
    return this.rejection.get(monster);
  }

  public double totalWeighting() {
    return this.weights;
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
    return getJumpChance(m -> m.getJumpChance());
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
                    (double) getWeighting(m)
                        * (1 - (double) this.getRejection(m) / 100)
                        / this.totalWeighting())
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
    StringBuffer buffer = new StringBuffer();

    buffer.append("<html><head>");
    buffer.append("<style>");

    buffer.append("body { font-family: sans-serif; font-size: ");
    buffer.append(Preferences.getString("chatFontSize"));
    buffer.append("; }");

    buffer.append("</style>");

    buffer.append("</head><body>");

    this.getSummary(buffer, fullString);
    this.getEncounterData(buffer);
    this.appendMonsterData(buffer, fullString);

    buffer.append("</body></html>");
    return buffer.toString();
  }

  public void getSummary(final StringBuffer buffer, final boolean fullString) {
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

      // Omit impossible (-2), ultra-rare (-1) and special/banished (0) monsters
      if (weighting < 1) {
        continue;
      }

      double weight =
          (double) weighting
              * (1 - (double) this.getRejection(monster) / 100)
              / this.totalWeighting();
      int ml = monster.ML();
      averageExperience +=
          weight * (monster.getExperience() + experienceAdjustment - ml / (ml > 0 ? 6.0 : 8.0));
    }

    double averageSuperlikelyExperience = 0.0;
    double superlikelyChance = 0.0;
    for (MonsterData monster : this.superlikelyMonsters) {
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
    buffer.append(jumpChance + "%");

    buffer.append("<br><b>Combat Rate</b>: ");

    double combatXP =
        averageSuperlikelyExperience
            + averageExperience * (1 - superlikelyChance / 100) * combatFactor;
    if (this.combats > 0) {
      buffer.append(
          this.format(superlikelyChance + (1 - superlikelyChance / 100) * combatFactor * 100.0)
              + "%");
      buffer.append("<br><b>Combat XP</b>: " + KoLConstants.FLOAT_FORMAT.format(combatXP));
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
    Map<MonsterData, Double> monsterData = new HashMap<>();

    if (stateful) {
      recalculate();
    }

    double totalSuperlikelyChance = 0.0;

    for (MonsterData monster : superlikelyMonsters) {
      double chance = superlikelyChance(monster);
      monsterData.put(monster, chance);
      totalSuperlikelyChance += chance;
    }

    double combatFactor = this.areaCombatPercent(stateful) / 100.0;

    for (MonsterData monster : monsters) {
      int weighting = getWeighting(monster);

      if (weighting == -2) {
        continue;
      }

      // Negative weight will set this to zero (which is good!)
      double chance =
          Math.max(
              0,
              100.0
                  * combatFactor
                  * (1 - totalSuperlikelyChance / 100)
                  * weighting
                  * (1 - (double) getRejection(monster) / 100));

      if (stateful) {
        chance = AdventureQueueDatabase.applyQueueEffects(chance, monster, this);
      } else {
        chance = chance / totalWeighting();
      }

      monsterData.put(monster, chance);
    }

    return monsterData;
  }

  public void appendMonsterData(final StringBuffer buffer, final boolean fullString) {
    int moxie = KoLCharacter.getAdjustedMoxie();
    int hitstat = EquipmentManager.getAdjustedHitStat();

    for (Map.Entry<MonsterData, Double> entry : getMonsterData(true).entrySet()) {
      MonsterData monster = entry.getKey();
      double chance = entry.getValue();
      buffer
          .append("<br><br>")
          .append(
              this.getMonsterString(
                  monster, moxie, hitstat, getWeighting(monster), chance, fullString));
    }
  }

  public void getEncounterData(final StringBuffer buffer) {
    String environment = AdventureDatabase.getEnvironment(this.zone);
    if (environment == null) {
      buffer.append("<br>");
      buffer.append("<b>Environment:</b> unknown");
    } else {
      buffer.append("<br>");
      buffer.append("<b>Environment:</b> ");
      buffer.append(environment);
    }

    int recommendedStat = AdventureDatabase.getRecommendedStat(this.zone);
    if (recommendedStat == -1) {
      buffer.append("<br>");
      buffer.append("<b>Recommended Mainstat:</b> unknown");
    } else {
      buffer.append("<br>");
      buffer.append("<b>Recommended Mainstat:</b> ");
      buffer.append(recommendedStat);
    }

    if (KoLCharacter.inRaincore()) {
      int waterLevel = KoLCharacter.getWaterLevel();
      Boolean fixed = AdventureDatabase.getWaterLevel(this.zone) != -1;
      if (environment == null) {
        buffer.append("<br>");
        buffer.append("<b>Water Level:</b> unknown");
      } else if (recommendedStat == -1 && !fixed) {
        buffer.append("<br>");
        buffer.append("<b>Water Level:</b> ");
        buffer.append(waterLevel);
        buffer.append(" (at least)");
      } else {
        buffer.append("<br>");
        buffer.append("<b>Water Level:</b> ");
        buffer.append(waterLevel);
      }
    }

    String encounter = EncounterManager.findEncounterForLocation(this.zone, EncounterType.SEMIRARE);

    if (null != encounter) {
      buffer.append("<br>");
      buffer.append("<b>Semi-Rare:</b> ");
      buffer.append(encounter);
    }

    encounter = EncounterManager.findEncounterForLocation(this.zone, EncounterType.CLOVER);

    if (null != encounter) {
      buffer.append("<br>");
      buffer.append("<b>Clover:</b> ");
      buffer.append(encounter);
    }

    encounter = EncounterManager.findEncounterForLocation(this.zone, EncounterType.GLYPH);

    if (null != encounter) {
      buffer.append("<br>");
      buffer.append("<b>Hobo Glyph:</b> ");
      buffer.append(encounter);
    }

    if (KoLCharacter.inAxecore()) {
      encounter = EncounterManager.findEncounterForLocation(this.zone, EncounterType.BORIS);

      if (null != encounter) {
        buffer.append("<br>");
        buffer.append("<b>Clancy:</b> ");
        buffer.append(encounter);
      }
    }

    if (KoLCharacter.inBadMoon()) {
      encounter = EncounterManager.findEncounterForLocation(this.zone, EncounterType.BADMOON);

      if (null != encounter) {
        buffer.append("<br>");
        buffer.append("<b>Badmoon:</b> ");
        buffer.append(encounter);
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
        case "The Defiled Alcove":
          if (Preferences.getInteger("cyrptAlcoveEvilness") <= 25) {
            return 100;
          }
          break;
        case "The Defiled Cranny":
          if (Preferences.getInteger("cyrptCrannyEvilness") <= 25) {
            return 100;
          }
          break;
        case "The Defiled Niche":
          if (Preferences.getInteger("cyrptNicheEvilness") <= 25) {
            return 100;
          }
          break;
        case "The Defiled Nook":
          if (Preferences.getInteger("cyrptNookEvilness") <= 25) {
            return 100;
          }
          break;
        case "The Smut Orc Logging Camp":
          return Preferences.getInteger("smutOrcNoncombatProgress") < 15 ? 100 : 0;
        case "Barf Mountain":
          return Preferences.getBoolean("dinseyRollercoasterNext") ? 0 : 100;
        case "Investigating a Plaintive Telegram":
          return Preferences.getInteger("lttQuestStageCount") == 9
                  || QuestDatabase.isQuestStep(Quest.TELEGRAM, QuestDatabase.STARTED)
              ? 0
              : 100;
        case "The Dripping Trees":
          // Non-Combat on turn 16, 31, 46, ...
          int advs = Preferences.getInteger("drippingTreesAdventuresSinceAscension");
          return (advs > 0 && (advs % 15) == 0) ? 0 : 100;
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

    double pct = this.combats;

    if (stateful) {
      pct += KoLCharacter.getCombatRateAdjustment();
    }

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
    buffer.append(" <font color=" + AreaCombatData.elementColor(element) + "><b>");
    if (monster.getPoison() < Integer.MAX_VALUE) {
      buffer.append("\u2620 ");
    }
    String name = monster.getName();
    buffer.append(name);
    buffer.append("</b></font> (");

    if (EncounterManager.isSaberForceMonster(name, this.getZone())) {
      buffer.append("forced by the saber");
    } else if (EncounterManager.isCrystalBallMonster(name, this.getZone())) {
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
      buffer.append(this.format(chance) + "%");
    }

    buffer.append(")<br>Hit: <font color=" + AreaCombatData.elementColor(ed) + ">");
    buffer.append(this.format(hitPercent));
    buffer.append("%</font>, Evade: <font color=" + AreaCombatData.elementColor(ea) + ">");
    buffer.append(this.format(evadePercent));
    buffer.append("%</font>, Jump Chance: <font color=" + AreaCombatData.elementColor(ea) + ">");
    buffer.append(this.format(jumpChance));
    buffer.append("%</font><br>Atk: " + attack + ", Def: " + defense);
    buffer.append(", HP: " + health + ", XP: " + KoLConstants.FLOAT_FORMAT.format(statGain));
    buffer.append("<br>Phylum: " + phylum);
    if (init == -10000) {
      buffer.append(", Never wins initiative");
    } else if (init == 10000) {
      buffer.append(", Always wins initiative");
    } else {
      buffer.append(", Init: " + init);
    }

    if (fullString) {
      this.appendMeatDrop(buffer, monster);
      this.appendSprinkleDrop(buffer, monster);
    }

    this.appendItemList(buffer, monster.getItems(), monster.getPocketRates(), fullString);

    String bounty = BountyDatabase.getNameByMonster(monster.getName());
    if (bounty != null) {
      buffer.append("<br>" + bounty + " (bounty)");
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
      final List<AdventureResult> items,
      final List<Double> pocketRates,
      boolean fullString) {
    if (items.size() == 0) {
      return;
    }

    double itemModifier = AreaCombatData.getDropRateModifier();
    boolean stealing = KoLCharacter.canPickpocket();
    double pocketModifier =
        (100.0 + KoLCharacter.currentNumericModifier(Modifiers.PICKPOCKET_CHANCE)) / 100.0;

    for (int i = 0; i < items.size(); ++i) {
      AdventureResult item = items.get(i);

      if (!fullString) {
        if (i == 0) {
          buffer.append("<br>");
        } else {
          buffer.append(", ");
        }

        buffer.append(item.getName());
        continue;
      }

      buffer.append("<br>");

      // Certain items can be increased by other bonuses than just item drop
      int itemId = item.getItemId();
      double itemBonus = 0.0;

      if (ItemDatabase.isFood(itemId)) {
        itemBonus += KoLCharacter.currentNumericModifier(Modifiers.FOODDROP) / 100.0;
      } else if (ItemDatabase.isBooze(itemId)) {
        itemBonus += KoLCharacter.currentNumericModifier(Modifiers.BOOZEDROP) / 100.0;
      } else if (ItemDatabase.isCandyItem(itemId)) {
        itemBonus += KoLCharacter.currentNumericModifier(Modifiers.CANDYDROP) / 100.0;
      } else if (ItemDatabase.isEquipment(itemId)) {
        itemBonus += KoLCharacter.currentNumericModifier(Modifiers.GEARDROP) / 100.0;
        if (ItemDatabase.isHat(itemId)) {
          itemBonus += KoLCharacter.currentNumericModifier(Modifiers.HATDROP) / 100.0;
        } else if (ItemDatabase.isWeapon(itemId)) {
          itemBonus += KoLCharacter.currentNumericModifier(Modifiers.WEAPONDROP) / 100.0;
        } else if (ItemDatabase.isOffHand(itemId)) {
          itemBonus += KoLCharacter.currentNumericModifier(Modifiers.OFFHANDDROP) / 100.0;
        } else if (ItemDatabase.isShirt(itemId)) {
          itemBonus += KoLCharacter.currentNumericModifier(Modifiers.SHIRTDROP) / 100.0;
        } else if (ItemDatabase.isPants(itemId)) {
          itemBonus += KoLCharacter.currentNumericModifier(Modifiers.PANTSDROP) / 100.0;
        } else if (ItemDatabase.isAccessory(itemId)) {
          itemBonus += KoLCharacter.currentNumericModifier(Modifiers.ACCESSORYDROP) / 100.0;
        }
      }

      double stealRate = Math.min(pocketRates.get(i).doubleValue() * pocketModifier, 1.0);
      int rawDropRate = item.getCount() >> 16;
      double dropRate = Math.min(rawDropRate * (itemModifier + itemBonus), 100.0);
      double effectiveDropRate = stealRate * 100.0 + (1.0 - stealRate) * dropRate;

      String rateRaw = this.format(rawDropRate);
      String rate1 = this.format(dropRate);
      String rate2 = this.format(effectiveDropRate);

      buffer.append(item.getName());
      switch ((char) item.getCount() & 0xFFFF) {
        case '0':
          buffer.append(" (unknown drop rate)");
          break;

        case 'n':
          if (rawDropRate > 0) {
            buffer.append(" ");
            buffer.append(rate1);
            buffer.append("% (no pickpocket)");
          } else {
            buffer.append(" (no pickpocket, unknown drop rate)");
          }
          break;

        case 'c':
          if (rawDropRate > 0) {
            buffer.append(" ");
            buffer.append(rate1);
            buffer.append("% (conditional)");
          } else {
            buffer.append(" (conditional, unknown drop rate)");
          }
          break;

        case 'f':
          buffer.append(" ");
          buffer.append(rateRaw);
          buffer.append("% (no modifiers)");
          break;

        case 'p':
          if (stealing && rawDropRate > 0) {
            buffer.append(" ");
            buffer.append(Math.min(rawDropRate * pocketModifier, 100.0));
            buffer.append("% (pickpocket only)");
          } else {
            buffer.append(" (pickpocket only, unknown rate)");
          }
          break;

        case 'a':
          buffer.append(" (stealable accordion)");
          break;

        default:
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

  public static final double getDropRateModifier() {
    if (AreaCombatData.lastDropMultiplier != 0.0
        && KoLCharacter.getItemDropPercentAdjustment() == AreaCombatData.lastDropModifier) {
      return AreaCombatData.lastDropMultiplier;
    }

    AreaCombatData.lastDropModifier = KoLCharacter.getItemDropPercentAdjustment();
    AreaCombatData.lastDropMultiplier =
        Math.max(0.0, (100.0 + AreaCombatData.lastDropModifier) / 100.0);

    return AreaCombatData.lastDropMultiplier;
  }

  public static final String elementColor(final Element element) {
    switch (element) {
      case HOT:
        return (KoLmafiaGUI.isDarkTheme()) ? "#ff8a93" : "#ff0000";
      case COLD:
        return (KoLmafiaGUI.isDarkTheme()) ? "#00d4ff" : "#0000ff";
      case STENCH:
        return (KoLmafiaGUI.isDarkTheme()) ? "#39f0d0" : "#008000";
      case SPOOKY:
        return (KoLmafiaGUI.isDarkTheme()) ? "#bebebe" : "#808080";
      case SLEAZE:
        return (KoLmafiaGUI.isDarkTheme()) ? "#b980ee" : "#8a2be2";
      case SLIME:
        return (KoLmafiaGUI.isDarkTheme()) ? "#1adde9" : "#006400";
      default:
        return (KoLmafiaGUI.isDarkTheme()) ? "#FFFFFF" : "#000000";
    }
  }

  public static final double hitPercent(final int attack, final int defense) {
    // ( (Attack - Defense) / 18 ) * 100 + 50 = Hit%
    double percent = 100.0 * (attack - defense) / 18 + 50.0;
    if (percent < 0.0) {
      return 0.0;
    }
    return Math.min(percent, 100.0);
  }

  public static final int perfectHit(final int attack, final int defense) {
    return attack - defense - 9;
  }

  public String getZone() {
    return zone;
  }

  private static int adjustConditionalWeighting(String zone, String monster, int weighting) {
    // Bossbat can appear on 4th fight, and will always appear on the 8th fight
    if (zone.equals("The Boss Bat's Lair")) {
      int bossTurns = AdventureSpentDatabase.getTurns(zone);
      if (monster.equals("Boss Bat")) {
        return bossTurns > 3 && !QuestDatabase.isQuestLaterThan(Quest.BAT, "step3") ? 1 : 0;
      } else {
        return bossTurns > 7 || QuestDatabase.isQuestLaterThan(Quest.BAT, "step3") ? -4 : 1;
      }
    } else if (zone.equals("The Hidden Park")) {
      if (monster.equals("pygmy janitor")
          && Preferences.getInteger("relocatePygmyJanitor") != KoLCharacter.getAscensions()) {
        return -4;
      }
      if (monster.equals("pygmy witch lawyer")
          && Preferences.getInteger("relocatePygmyLawyer") != KoLCharacter.getAscensions()) {
        return -4;
      }
    } else if (zone.equals("The Hidden Apartment Building")
        || zone.equals("The Hidden Hospital")
        || zone.equals("The Hidden Office Building")
        || zone.equals("The Hidden Bowling Alley")) {
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
    } else if (zone.equals("The Fungal Nethers")) {
      if (monster.equals("muscular mushroom guy")) {
        return KoLCharacter.isSealClubber() ? 1 : 0;
      }
      if (monster.equals("armored mushroom guy")) {
        return KoLCharacter.isTurtleTamer() ? 1 : 0;
      }
      if (monster.equals("wizardly mushroom guy")) {
        return KoLCharacter.isPastamancer() ? 1 : 0;
      }
      if (monster.equals("fiery mushroom guy")) {
        return KoLCharacter.isSauceror() ? 1 : 0;
      }
      if (monster.equals("dancing mushroom guy")) {
        return KoLCharacter.isDiscoBandit() ? 1 : 0;
      }
      if (monster.equals("wailing mushroom guy")) {
        return KoLCharacter.isAccordionThief() ? 1 : 0;
      }
    } else if (zone.equals("Pirates of the Garbage Barges")) {
      if (monster.equals("flashy pirate") && !Preferences.getBoolean("dinseyGarbagePirate")) {
        return 0;
      }
    } else if (zone.equals("Uncle Gator's Country Fun-Time Liquid Waste Sluice")) {
      if (monster.equals("nasty bear") && QuestDatabase.isQuestStep(Quest.NASTY_BEARS, "step1")) {
        return 1;
      }
    } else if (zone.equals("Throne Room")) {
      if (monster.equals("Knob Goblin King") && QuestDatabase.isQuestFinished(Quest.GOBLIN)) {
        return 0;
      }
    } else if (zone.equals("The Defiled Alcove")) {
      int evilness = Preferences.getInteger("cyrptAlcoveEvilness");
      if (monster.equals("conjoined zmombie")) {
        return evilness > 0 && evilness <= 25 ? 1 : 0;
      } else if (!monster.equals("modern zmobie")) {
        return evilness > 25 ? 1 : 0;
      }
    } else if (zone.equals("The Defiled Cranny")) {
      int evilness = Preferences.getInteger("cyrptCrannyEvilness");
      if (monster.equals("huge ghuol")) {
        return evilness > 0 && evilness <= 25 ? 1 : 0;
      } else if (monster.equals("gaunt ghuol") || monster.equals("gluttonous ghuol")) {
        return evilness > 25 ? 1 : 0;
      }
    } else if (zone.equals("The Defiled Niche")) {
      int evilness = Preferences.getInteger("cyrptNicheEvilness");
      if (monster.equals("gargantulihc")) {
        return evilness > 0 && evilness <= 25 ? 1 : 0;
      } else {
        return evilness > 25 ? 1 : 0;
      }
    } else if (zone.equals("The Defiled Nook")) {
      int evilness = Preferences.getInteger("cyrptNookEvilness");
      if (monster.equals("giant skeelton")) {
        return evilness > 0 && evilness <= 25 ? 1 : 0;
      } else {
        return evilness > 25 ? 1 : 0;
      }
    } else if (zone.equals("Haert of the Cyrpt")) {
      if (monster.equals("Bonerdagon")
          && QuestDatabase.isQuestLaterThan(Quest.CYRPT, QuestDatabase.STARTED)) {
        return 0;
      }
    } else if (zone.equals("The F'c'le")) {
      if (monster.equals("clingy pirate (female)")) {
        return KoLCharacter.getGender() == KoLCharacter.MALE ? 1 : 0;
      } else if (monster.equals("clingy pirate (male)")) {
        return KoLCharacter.getGender() == KoLCharacter.FEMALE ? 1 : 0;
      }
    } else if (zone.equals("Summoning Chamber")) {
      if (monster.equals("Lord Spookyraven") && QuestDatabase.isQuestFinished(Quest.MANOR)) {
        return 0;
      }
    } else if (zone.equals("An Overgrown Shrine (Northwest)")) {
      // Assume lianas are dealt with once Apartment opened. Player may leave without doing so, but
      // that's
      // abit niche for me to care!
      if (monster.equals("dense liana") && Preferences.getInteger("hiddenApartmentProgress") > 0) {
        return 0;
      }
    } else if (zone.equals("An Overgrown Shrine (Northeast)")) {
      // Assume lianas are dealt with once Office opened. Player may leave without doing so, but
      // that's
      // abit niche for me to care!
      if (monster.equals("dense liana") && Preferences.getInteger("hiddenOfficeProgress") > 0) {
        return 0;
      }
    } else if (zone.equals("An Overgrown Shrine (Southwest)")) {
      // Assume lianas are dealt with once Hospital opened. Player may leave without doing so, but
      // that's
      // abit niche for me to care!
      if (monster.equals("dense liana") && Preferences.getInteger("hiddenHospitalProgress") > 0) {
        return 0;
      }
    } else if (zone.equals("An Overgrown Shrine (Southeast)")) {
      // Assume lianas are dealt with once Bowling Alley opened. Player may leave without doing so,
      // but that's
      // abit niche for me to care!
      if (monster.equals("dense liana")
          && Preferences.getInteger("hiddenBowlingAlleyProgress") > 0) {
        return 0;
      }
    } else if (zone.equals("A Massive Ziggurat")) {
      // Assume lianas dealt with after 3 turns, won't always be right, but this is a bit niche for
      // special tracking
      int zoneTurns = AdventureSpentDatabase.getTurns(zone);
      if (monster.equals("dense liana")
          && (zoneTurns >= 3 || QuestDatabase.isQuestFinished(Quest.WORSHIP))) {
        return 0;
      } else if (monster.equals("Protector Spectre")
          && QuestDatabase.isQuestStep(Quest.WORSHIP, "step4")) {
        return 1;
      }
    } else if (zone.equals("Oil Peak")) {
      int monsterLevel = (int) KoLCharacter.currentNumericModifier(Modifiers.MONSTER_LEVEL);
      if (monster.equals("oil slick")) {
        return monsterLevel < 20 ? 1 : 0;
      } else if (monster.equals("oil tycoon")) {
        return monsterLevel >= 20 && monsterLevel < 50 ? 1 : 0;
      } else if (monster.equals("oil baron")) {
        return monsterLevel >= 50 && monsterLevel < 100 ? 1 : 0;
      } else if (monster.equals("oil cartel")) {
        return monsterLevel >= 100 ? 1 : 0;
      }
    } else if (zone.equals("Fastest Adventurer Contest")) {
      int opponentsLeft = Preferences.getInteger("nsContestants1");
      if (monster.equals("Tasmanian Dervish")) {
        return opponentsLeft == 1 ? 1 : 0;
      } else {
        return opponentsLeft > 1 ? 1 : 0;
      }
    } else if (zone.equals("Strongest Adventurer Contest")) {
      int opponentsLeft =
          Preferences.getString("nsChallenge1").equals("Muscle")
              ? Preferences.getInteger("nsContestants2")
              : 0;
      if (monster.equals("Mr. Loathing")) {
        return opponentsLeft == 1 ? 1 : 0;
      } else {
        return opponentsLeft > 1 ? 1 : 0;
      }
    } else if (zone.equals("Smartest Adventurer Contest")) {
      int opponentsLeft =
          Preferences.getString("nsChallenge1").equals("Mysticality")
              ? Preferences.getInteger("nsContestants2")
              : 0;
      if (monster.equals("The Mastermind")) {
        return opponentsLeft == 1 ? 1 : 0;
      } else {
        return opponentsLeft > 1 ? 1 : 0;
      }
    } else if (zone.equals("Smoothest Adventurer Contest")) {
      int opponentsLeft =
          Preferences.getString("nsChallenge1").equals("Muscle")
              ? Preferences.getInteger("nsContestants2")
              : 0;
      if (monster.equals("Seannery the Conman")) {
        return opponentsLeft == 1 ? 1 : 0;
      } else {
        return opponentsLeft > 1 ? 1 : 0;
      }
    } else if (zone.equals("Coldest Adventurer Contest")) {
      int opponentsLeft =
          Preferences.getString("nsChallenge2").equals("cold")
              ? Preferences.getInteger("nsContestants3")
              : 0;
      if (monster.equals("Mrs. Freeze")) {
        return opponentsLeft == 1 ? 1 : 0;
      } else {
        return opponentsLeft > 1 ? 1 : 0;
      }
    } else if (zone.equals("Hottest Adventurer Contest")) {
      int opponentsLeft =
          Preferences.getString("nsChallenge2").equals("hot")
              ? Preferences.getInteger("nsContestants3")
              : 0;
      if (monster.equals("Mrs. Freeze")) {
        return opponentsLeft == 1 ? 1 : 0;
      } else {
        return opponentsLeft > 1 ? 1 : 0;
      }
    } else if (zone.equals("Sleaziest Adventurer Contest")) {
      int opponentsLeft =
          Preferences.getString("nsChallenge2").equals("sleaze")
              ? Preferences.getInteger("nsContestants3")
              : 0;
      if (monster.equals("Leonard")) {
        return opponentsLeft == 1 ? 1 : 0;
      } else {
        return opponentsLeft > 1 ? 1 : 0;
      }
    } else if (zone.equals("Spookiest Adventurer Contest")) {
      int opponentsLeft =
          Preferences.getString("nsChallenge2").equals("spooky")
              ? Preferences.getInteger("nsContestants3")
              : 0;
      if (monster.equals("Arthur Frankenstein")) {
        return opponentsLeft == 1 ? 1 : 0;
      } else {
        return opponentsLeft > 1 ? 1 : 0;
      }
    } else if (zone.equals("Stinkiest Adventurer Contest")) {
      int opponentsLeft =
          Preferences.getString("nsChallenge2").equals("stinky")
              ? Preferences.getInteger("nsContestants3")
              : 0;
      if (monster.equals("Odorous Humongous")) {
        return opponentsLeft == 1 ? 1 : 0;
      } else {
        return opponentsLeft > 1 ? 1 : 0;
      }
    } else if (zone.equals("The Nemesis' Lair")) {
      int lairTurns = AdventureSpentDatabase.getTurns(zone);
      if (monster.equals("hellseal guardian")) {
        return KoLCharacter.isSealClubber() ? 1 : 0;
      } else if (monster.equals("Gorgolok, the Infernal Seal (Inner Sanctum)")) {
        return KoLCharacter.isSealClubber() && lairTurns >= 4 ? 1 : 0;
      } else if (monster.equals("warehouse worker")) {
        return KoLCharacter.isTurtleTamer() ? 1 : 0;
      } else if (monster.equals("Stella, the Turtle Poacher (Inner Sanctum)")) {
        return KoLCharacter.isTurtleTamer() && lairTurns >= 4 ? 1 : 0;
      } else if (monster.equals("evil spaghetti cult zealot")) {
        return KoLCharacter.isPastamancer() ? 1 : 0;
      } else if (monster.equals("Spaghetti Elemental (Inner Sanctum)")) {
        return KoLCharacter.isPastamancer() && lairTurns >= 4 ? 1 : 0;
      } else if (monster.equals("security slime")) {
        return KoLCharacter.isSauceror() ? 1 : 0;
      } else if (monster.equals("Lumpy, the Sinister Sauceblob (Inner Sanctum)")) {
        return KoLCharacter.isSauceror() && lairTurns >= 4 ? 1 : 0;
      } else if (monster.equals("daft punk")) {
        return KoLCharacter.isDiscoBandit() ? 1 : 0;
      } else if (monster.equals("Spirit of New Wave (Inner Sanctum)")) {
        return KoLCharacter.isDiscoBandit() && lairTurns >= 4 ? 1 : 0;
      } else if (monster.equals("mariachi bruiser")) {
        return KoLCharacter.isAccordionThief() ? 1 : 0;
      } else if (monster.equals("Somerset Lopez, Dread Mariachi (Inner Sanctum)")) {
        return KoLCharacter.isAccordionThief() && lairTurns >= 4 ? 1 : 0;
      }
    } else if (zone.equals("The Slime Tube")) {
      int monsterLevel = (int) KoLCharacter.currentNumericModifier(Modifiers.MONSTER_LEVEL);
      if (monster.equals("Slime")) {
        return monsterLevel <= 100 ? 1 : 0;
      } else if (monster.equals("Slime Hand")) {
        return monsterLevel > 100 && monsterLevel <= 300 ? 1 : 0;
      } else if (monster.equals("Slime Mouth")) {
        return monsterLevel > 300 && monsterLevel <= 600 ? 1 : 0;
      } else if (monster.equals("Slime Construct")) {
        return monsterLevel > 600 && monsterLevel <= 1000 ? 1 : 0;
      } else if (monster.equals("Slime Colossus")) {
        return monsterLevel > 1000 ? 1 : 0;
      }
    } else if (zone.equals("The Post-Mall")) {
      int mallTurns = AdventureSpentDatabase.getTurns(zone);
      if (monster.equals("sentient ATM")) {
        return mallTurns == 11 ? 1 : 0;
      } else {
        return mallTurns == 11 ? -4 : 1;
      }
    } else if (zone.equals("Investigating a Plaintive Telegram")) {
      String quest = Preferences.getString("lttQuestName");
      String questStep = Preferences.getString("questLTTQuestByWire");
      if (monster.equals("drunk cowpoke")) {
        return (quest.equals("Missing: Fancy Man") && questStep.equals("step1"))
                || (quest.equals("Help!  Desperados|") && questStep.equals("step1"))
                || (quest.equals("Big Gambling Tournament Announced") && questStep.equals("step1"))
                || (quest.equals("Sheriff Wanted") && questStep.equals("step1"))
                || (quest.equals("Madness at the Mine") && questStep.equals("step1"))
            ? 1
            : 0;
      } else if (monster.equals("surly gambler")) {
        return (quest.equals("Missing: Fancy Man") && questStep.equals("step1"))
                || (quest.equals("Big Gambling Tournament Announced") && questStep.equals("step3"))
                || (quest.equals("Sheriff Wanted") && questStep.equals("step1"))
            ? 1
            : 0;
      } else if (monster.equals("wannabe gunslinger")) {
        return (quest.equals("Help!  Desperados|") && questStep.equals("step1"))
                || (quest.equals("Big Gambling Tournament Announced") && questStep.equals("step1"))
                || (quest.equals("Sheriff Wanted") && questStep.equals("step1"))
                || (quest.equals("Wagon Train Escort Wanted") && questStep.equals("step3"))
            ? 1
            : 0;
      } else if (monster.equals("cow cultist")) {
        return (quest.equals("Missing: Pioneer Daughter") && questStep.equals("step2"))
                || (quest.equals("Haunted Boneyard") && questStep.equals("step3"))
                || (quest.equals("Sheriff Wanted") && questStep.equals("step2"))
                || (quest.equals("Missing: Many Children") && questStep.equals("step1"))
            ? 1
            : 0;
      } else if (monster.equals("hired gun")) {
        return (quest.equals("Missing: Fancy Man") && questStep.equals("step1"))
                || (quest.equals("Help!  Desperados|") && questStep.equals("step1"))
                || (quest.equals("Missing: Pioneer Daughter") && questStep.equals("step2"))
                || (quest.equals("Big Gambling Tournament Announced") && questStep.equals("step3"))
                || (quest.equals("Sheriff Wanted") && questStep.equals("step3"))
                || (quest.equals("Missing: Many Children") && questStep.equals("step1"))
                || (quest.equals("Wagon Train Escort Wanted") && questStep.equals("step3"))
            ? 1
            : 0;
      } else if (monster.equals("camp cook")) {
        return (quest.equals("Missing: Fancy Man") && questStep.equals("step2"))
                || (quest.equals("Sheriff Wanted") && questStep.equals("step3"))
                || (quest.equals("Madness at the Mine") && questStep.equals("step1"))
                || (quest.equals("Wagon Train Escort Wanted") && questStep.equals("step3"))
            ? 1
            : 0;
      } else if (monster.equals("skeletal gunslinger")) {
        return (quest.equals("Help!  Desperados|") && questStep.equals("step3"))
                || (quest.equals("Haunted Boneyard") && questStep.equals("step1"))
                || (quest.equals("Madness at the Mine") && questStep.equals("step3"))
                || (quest.equals("Wagon Train Escort Wanted") && questStep.equals("step2"))
            ? 1
            : 0;
      } else if (monster.equals("restless ghost")) {
        return (quest.equals("Missing: Fancy Man") && questStep.equals("step3"))
                || (quest.equals("Missing: Pioneer Daughter") && questStep.equals("step1"))
                || (quest.equals("Haunted Boneyard") && questStep.equals("step2"))
                || (quest.equals("Madness at the Mine") && questStep.equals("step3"))
                || (quest.equals("Missing: Many Children") && questStep.equals("step2"))
                || (quest.equals("Wagon Train Escort Wanted") && questStep.equals("step2"))
            ? 1
            : 0;
      } else if (monster.equals("buzzard")) {
        return (quest.equals("Missing: Fancy Man") && questStep.equals("step2"))
                || (quest.equals("Help! Desperados|") && questStep.equals("step2"))
                || (quest.equals("Missing: Pioneer Daughter") && questStep.equals("step1"))
                || (quest.equals("Haunted Boneyard") && questStep.equals("step1"))
            ? 1
            : 0;
      } else if (monster.equals("mountain lion")) {
        return (quest.equals("Missing: Fancy Man") && questStep.equals("step2"))
                || (quest.equals("Help!  Desperados|") && questStep.equals("step2"))
                || (quest.equals("Sheriff Wanted") && questStep.equals("step2"))
                || (quest.equals("Madness at the Mine") && questStep.equals("step2"))
                || (quest.equals("Wagon Train Escort Wanted") && questStep.equals("step1"))
            ? 1
            : 0;
      } else if (monster.equals("grizzled bear")) {
        return (quest.equals("Help!  Desperados|") && questStep.equals("step3"))
                || (quest.equals("Madness at the Mine") && questStep.equals("step3"))
                || (quest.equals("Wagon Train Escort Wanted") && questStep.equals("step1"))
            ? 1
            : 0;
      } else if (monster.equals("diamondback rattler")) {
        return (quest.equals("Help!  Desperados|") && questStep.equals("step2"))
                || (quest.equals("Big Gambling Tournament Announced") && questStep.equals("step2"))
                || (quest.equals("Madness at the Mine") && questStep.equals("step2"))
                || (quest.equals("Wagon Train Escort Wanted") && questStep.equals("step1"))
            ? 1
            : 0;
      } else if (monster.equals("coal snake")) {
        return (quest.equals("Missing: Fancy Man") && questStep.equals("step3"))
                || (quest.equals("Big Gambling Tournament Announced") && questStep.equals("step2"))
                || (quest.equals("Madness at the Mine") && questStep.equals("step1"))
            ? 1
            : 0;
      } else if (monster.equals("frontwinder")) {
        return (quest.equals("Big Gambling Tournament Announced") && questStep.equals("step2"))
                || (quest.equals("Sheriff Wanted") && questStep.equals("step2"))
            ? 1
            : 0;
      } else if (monster.equals("caugr")) {
        return (quest.equals("Missing: Pioneer Daughter") && questStep.equals("step3"))
                || (quest.equals("Missing: Many Children") && questStep.equals("step3"))
            ? 1
            : 0;
      } else if (monster.equals("pyrobove")) {
        return (quest.equals("Missing: Pioneer Daughter") && questStep.equals("step3"))
                || (quest.equals("Missing: Many Children") && questStep.equals("step3"))
                || (quest.equals("Wagon Train Escort Wanted") && questStep.equals("step2"))
            ? 1
            : 0;
      } else if (monster.equals("spidercow")) {
        return (quest.equals("Missing: Pioneer Daughter") && questStep.equals("step3"))
                || (quest.equals("Haunted Boneyard") && questStep.equals("step3"))
                || (quest.equals("Missing: Many Children") && questStep.equals("step1"))
            ? 1
            : 0;
      } else if (monster.equals("moomy")) {
        return (quest.equals("Haunted Boneyard") && questStep.equals("step3"))
                || (quest.equals("Madness at the Mine") && questStep.equals("step2"))
                || (quest.equals("Missing: Many Children") && questStep.equals("step3"))
            ? 1
            : 0;
      } else if (monster.equals("Jeff the Fancy Skeleton")) {
        return (quest.equals("Missing: Fancy Man") && questStep.equals("step4")) ? 1 : 0;
      } else if (monster.equals("Daisy the Unclean")) {
        return (quest.equals("Missing: Pioneer Daughter") && questStep.equals("step4")) ? 1 : 0;
      } else if (monster.equals("Pecos Dave")) {
        return (quest.equals("Help!  Desperados|") && questStep.equals("step4")) ? 1 : 0;
      } else if (monster.equals("Pharaoh Amoon-Ra Cowtep")) {
        return (quest.equals("Haunted Boneyard") && questStep.equals("step4")) ? 1 : 0;
      } else if (monster.equals("Snake-Eyes Glenn")) {
        return (quest.equals("Big Gambling Tournament Announced") && questStep.equals("step4"))
            ? 1
            : 0;
      } else if (monster.equals("Former Sheriff Dan Driscoll")) {
        return (quest.equals("Sheriff Wanted") && questStep.equals("step4")) ? 1 : 0;
      } else if (monster.equals("unusual construct")) {
        return (quest.equals("Madness at the Mine") && questStep.equals("step4")) ? 1 : 0;
      } else if (monster.equals("Clara")) {
        return (quest.equals("Missing: Many Children") && questStep.equals("step4")) ? 1 : 0;
      } else if (monster.equals("Granny Hackleton")) {
        return (quest.equals("Wagon Train Escort Wanted") && questStep.equals("step4")) ? 1 : 0;
      }
    } else if (zone.equals("Gingerbread Civic Center")
        || zone.equals("Gingerbread Train Station")
        || zone.equals("Gingerbread Industrial Zone")
        || zone.equals("Gingerbread Upscale Retail District")) {
      if (monster.equals("gingerbread pigeon") || monster.equals("gingerbread rat")) {
        return Preferences.getBoolean("gingerSewersUnlocked") ? 0 : 1;
      }
    } else if (zone.equals("The Canadian Wildlife Preserve")) {
      if (monster.equals("wild reindeer")) {
        return KoLCharacter.getFamiliar().getId() != FamiliarPool.YULE_HOUND ? 0 : 1;
      }
    }
    return weighting;
  }

  public static final double superlikelyChance(MonsterData monster) {
    return superlikelyChance(monster.getName());
  }

  public static final double superlikelyChance(String monster) {
    if (monster.equals("screambat")) {
      int turns =
          AdventureSpentDatabase.getTurns("Guano Junction")
              + AdventureSpentDatabase.getTurns("The Batrat and Ratbat Burrow")
              + AdventureSpentDatabase.getTurns("The Beanbat Chamber");
      // Appears every 8 turns in relevant zones
      return turns > 0 && (turns % 8) == 0 ? 100.0 : 0.0;
    }
    if (monster.equals("modern zmobie") && Preferences.getInteger("cyrptAlcoveEvilness") > 25) {
      // Chance based on initiative
      double chance = 15 + KoLCharacter.getInitiativeAdjustment() / 10;
      return chance < 0 ? 0.0 : chance > 100 ? 100.0 : chance;
    }
    if (monster.equals("ninja snowman assassin")) {
      // Do not appear without positive combat rate
      double combatRate = KoLCharacter.getCombatRateAdjustment();
      if (combatRate <= 0) {
        return 0;
      }
      // Guaranteed on turns 11, 21, and 31
      int snowmanTurns = AdventureSpentDatabase.getTurns("Lair of the Ninja Snowmen");
      if (snowmanTurns == 10 || snowmanTurns == 20 || snowmanTurns == 30) {
        return 100.0;
      }
      double chance = combatRate / 2 + (double) snowmanTurns * 1.5;
      return chance < 0 ? 0.0 : chance > 100 ? 100.0 : chance;
    }
    if (monster.equals("mother hellseal")) {
      double chance = Preferences.getInteger("_sealScreeches") * 10;
      return chance < 0 ? 0.0 : chance > 100 ? 100.0 : chance;
    }
    if (monster.equals("Brick Mulligan, the Bartender")) {
      int kokomoTurns = AdventureSpentDatabase.getTurns("Kokomo Resort");
      // Appears every 25 turns
      return kokomoTurns > 0 && (kokomoTurns % 25) == 0 ? 100.0 : 0.0;
    }
    return 0;
  }
}
