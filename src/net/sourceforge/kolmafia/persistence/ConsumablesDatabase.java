package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.CaseInsensitiveHashMap;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ConsumablesDatabase {
  public static final AdventureResult ODE = EffectPool.get(EffectPool.ODE);
  public static final AdventureResult GLORIOUS_LUNCH = EffectPool.get(EffectPool.GLORIOUS_LUNCH);
  public static final AdventureResult BARREL_OF_LAUGHS =
      EffectPool.get(EffectPool.BARREL_OF_LAUGHS);
  public static final AdventureResult BEER_BARREL_POLKA =
      EffectPool.get(EffectPool.BEER_BARREL_POLKA);
  public static final AdventureResult RECORD_HUNGER = EffectPool.get(EffectPool.RECORD_HUNGER);
  public static final AdventureResult DRUNK_AVUNCULAR = EffectPool.get(EffectPool.DRUNK_AVUNCULAR);
  public static final AdventureResult REFINED_PALATE = EffectPool.get(EffectPool.REFINED_PALATE);

  private static final Map<Integer, Consumable> consumableByItemId = new HashMap<>();
  // Certain consumables are stored with inconsistent case in different places. Until that's fixed,
  // this needs to be case-insensitive.
  private static final Map<String, Consumable> consumableByName = new CaseInsensitiveHashMap<>();
  public static final Set<Consumable> allConsumables = new HashSet<>();

  // This is a cache which stores adventure information under different boosting-effect scenarios.
  // The index into the ArrayList is a bit vector of flags that specify how adventures are boosted
  // (e.g. Ode, munchies pill, etc). See calculateAverageAdventures.
  private static final int AVERAGE_ADVENTURE_CACHE_SIZE = 1 << 5;
  private static final ArrayList<Map<String, Double>> currentAverageAdventures =
      new ArrayList<>(AVERAGE_ADVENTURE_CACHE_SIZE);

  public enum ConsumableQuality {
    NONE(""),
    CRAPPY("crappy", "#999999"),
    DECENT("decent"),
    GOOD("good", "green"),
    AWESOME("awesome", "blue"),
    EPIC("EPIC", "#8a2be2"),
    QUEST("quest"),
    CHANGING("???"),
    DRIPPY("drippy", "#964B00"),
    SUSHI("sushi");

    static final Map<String, ConsumableQuality> nameToQuality = new HashMap<>();

    static {
      Arrays.stream(values()).forEach(q -> nameToQuality.put(q.getName(), q));
    }

    static ConsumableQuality find(final String name) {
      return nameToQuality.getOrDefault(name, ConsumableQuality.NONE);
    }

    private final String name;
    private final String color;

    ConsumableQuality(final String name, final String color) {
      this.name = name;
      this.color = color;
    }

    ConsumableQuality(final String name) {
      this(name, null);
    }

    @Override
    public String toString() {
      return name;
    }

    public String getName() {
      return name;
    }

    public String getColor() {
      return color;
    }
  }

  public record DustyBottle(int id, String name, String alias) {}

  public static DustyBottle[] DUSTY_BOTTLES = {
    new DustyBottle(
        ItemPool.DUSTY_BOTTLE_OF_MERLOT,
        "dusty bottle of Merlot",
        "dusty bottle of average Merlot"),
    new DustyBottle(
        ItemPool.DUSTY_BOTTLE_OF_PORT, "dusty bottle of Port", "dusty bottle of vinegar Port"),
    new DustyBottle(
        ItemPool.DUSTY_BOTTLE_OF_PINOT_NOIR,
        "dusty bottle of Pinot Noir",
        "dusty bottle of spooky Pinot Noir"),
    new DustyBottle(
        ItemPool.DUSTY_BOTTLE_OF_ZINFANDEL,
        "dusty bottle of Zinfandel",
        "dusty bottle of great Zinfandel"),
    new DustyBottle(
        ItemPool.DUSTY_BOTTLE_OF_MARSALA,
        "dusty bottle of Marsala",
        "dusty bottle of glassy Marsala"),
    new DustyBottle(
        ItemPool.DUSTY_BOTTLE_OF_MUSCAT, "dusty bottle of Muscat", "dusty bottle of bad Muscat")
  };

  private ConsumablesDatabase() {}

  public static void reset() {
    ConsumablesDatabase.readConsumptionData(
        "fullness.txt", KoLConstants.FULLNESS_VERSION, ConsumptionType.EAT);
    ConsumablesDatabase.readConsumptionData(
        "inebriety.txt", KoLConstants.INEBRIETY_VERSION, ConsumptionType.DRINK);
    ConsumablesDatabase.readConsumptionData(
        "spleenhit.txt", KoLConstants.SPLEENHIT_VERSION, ConsumptionType.SPLEEN);
    ConsumablesDatabase.readNonfillingData();

    // Once we have all this data, we can init the ConcoctionDatabase.
    ConcoctionDatabase.resetUsableList();
  }

  static {
    populateAndBuild();
  }

  private static void populateAndBuild() {
    for (int i = 0; i < AVERAGE_ADVENTURE_CACHE_SIZE; ++i) {
      ConsumablesDatabase.currentAverageAdventures.add(new HashMap<>());
    }
    ConsumablesDatabase.reset();
  }

  // Used for testing to replicate static initialization
  public static void clearAndRebuild() {
    consumableByItemId.clear();
    consumableByName.clear();
    allConsumables.clear();
    currentAverageAdventures.clear();
    populateAndBuild();
  }

  public static void writeConsumable(final PrintStream writer, final Consumable consumable) {
    writer.println(consumable.toString());
  }

  private static void readConsumptionData(String filename, int version, ConsumptionType usage) {
    try (BufferedReader reader = FileUtilities.getVersionedReader(filename, version)) {
      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        ConsumablesDatabase.saveConsumptionValues(data, usage);
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  private static Consumable setConsumptionData(
      final String name,
      final Integer fullness,
      final Integer inebriety,
      final Integer spleenHit,
      final int level,
      final ConsumableQuality quality,
      final String adventures,
      final String muscle,
      final String mysticality,
      final String moxie,
      final String note) {
    Consumable existing = ConsumablesDatabase.consumableByName.get(name);
    Set<String> aliases = existing != null ? existing.aliases : new TreeSet<>(List.of(name));
    Consumable consumable =
        new Consumable(
            name,
            fullness != null ? fullness : existing != null ? existing.getRawFullness() : null,
            inebriety != null ? inebriety : existing != null ? existing.getRawInebriety() : null,
            spleenHit != null ? spleenHit : existing != null ? existing.getRawSpleenHit() : null,
            level,
            quality,
            adventures,
            new String[] {muscle, mysticality, moxie},
            note,
            aliases);

    if (consumable.itemId > 0) {
      ConsumablesDatabase.consumableByItemId.put(consumable.itemId, consumable);
    }
    for (String alias : aliases) {
      ConsumablesDatabase.consumableByName.put(alias, consumable);
    }
    if (existing != null) {
      ConsumablesDatabase.allConsumables.remove(existing);
    }
    ConsumablesDatabase.allConsumables.add(consumable);

    ConsumablesDatabase.calculateAverageAdventures(consumable);

    Concoction c = consumable.getConcoction();
    if (c != null) {
      c.setConsumptionData(consumable);
    }
    return consumable;
  }

  public static final void cloneConsumptionData(final String name, final String alias) {
    Consumable consumable = ConsumablesDatabase.consumableByName.get(name);
    if (consumable != null) {
      consumable.aliases.add(alias);
      ConsumablesDatabase.consumableByName.put(alias, consumable);
    }
  }

  private static void readNonfillingData() {
    try (BufferedReader reader =
        FileUtilities.getVersionedReader("nonfilling.txt", KoLConstants.NONFILLING_VERSION)) {
      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length < 2) continue;

        String name = data[0];
        setConsumptionData(
            name,
            null,
            null,
            null,
            Integer.parseInt(data[1]),
            ConsumableQuality.NONE,
            "0",
            "0",
            "0",
            "0",
            data.length >= 3 ? data[2] : null);
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  private static void saveConsumptionValues(String[] data, ConsumptionType usage) {
    if (data.length < 2) {
      return;
    }

    String name = data[0];

    if (data.length < 8) {
      return;
    }

    int size = Integer.parseInt(data[1]);
    int level = Integer.parseInt(data[2]);
    ConsumableQuality quality = ConsumableQuality.find(data[3]);
    String adventures = data[4];
    String muscle = data[5];
    String mysticality = data[6];
    String moxie = data[7];

    String holiday = HolidayDatabase.getHoliday();
    boolean isBorisDay = (holiday.contains("Feast of Boris") || holiday.contains("Drunksgiving"));

    // Some items different on Feast of Boris
    if (isBorisDay) {
      switch (name) {
        case "cranberries" -> {
          quality = ConsumableQuality.GOOD;
          adventures = "2-4";
        }
        case "redrum" -> {
          quality = ConsumableQuality.GOOD;
          adventures = "5-9";
        }
        case "vodka and cranberry" -> {
          quality = ConsumableQuality.GOOD;
          adventures = "6-9";
        }
      }
    }

    Consumable consumable =
        setConsumptionData(
            name,
            usage == ConsumptionType.EAT ? size : null,
            usage == ConsumptionType.DRINK ? size : null,
            usage == ConsumptionType.SPLEEN ? size : null,
            level,
            quality,
            adventures,
            muscle,
            mysticality,
            moxie,
            data.length >= 9 ? data[8] : null // notes
            );

    // When we reset consumption data, we must reset Concoctions
    ConsumablesDatabase.calculateAverageAdventures(consumable);
  }

  public static final String getBaseAdventureRange(final String name) {
    if (name == null) {
      return "";
    }

    Consumable consumable = ConsumablesDatabase.consumableByName.get(name);
    if (consumable == null) {
      return "";
    }
    if (KoLCharacter.inSlowcore()) {
      return "0";
    }
    if (KoLCharacter.inNuclearAutumn()) {
      // int dashIndex = range.indexOf( "-" );
      // int start = StringUtilities.parseInt( dashIndex == -1 ? range : range.substring( 0,
      // dashIndex ) );
      // int end = dashIndex == -1 ? start : StringUtilities.parseInt( range.substring( dashIndex +
      // 1 ) );
    }
    return consumable.adventureRange;
  }

  public static final void calculateAllAverageAdventures() {
    for (Map<String, Double> map : ConsumablesDatabase.currentAverageAdventures) {
      map.clear();
    }

    for (Consumable consumable : ConsumablesDatabase.allConsumables) {
      ConsumablesDatabase.calculateAverageAdventures(consumable);
    }
  }

  private static void calculateAverageAdventures(Consumable consumable) {
    String name = consumable.name;
    int itemId = consumable.itemId;
    int start = consumable.adventureStart;
    int end = consumable.adventureEnd;
    int size = consumable.getSize();

    Concoction c = consumable.getConcoction();
    int advs = (c == null) ? 0 : c.getAdventuresNeeded(1, true);

    if (KoLCharacter.inNuclearAutumn()) {
      if (consumable.getConsumptionType() == ConsumptionType.EAT) {
        int multiplier = 1;
        if (KoLCharacter.hasSkill(SkillPool.EXTRA_GALL_BLADDER)) multiplier += 1;
        if (KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.RECORD_HUNGER)))
          multiplier += 1;
        start *= multiplier;
        end *= multiplier;
      }
      // && KoLCharacter.hasSkill(SkillPool.EXTRA_KIDNEY)
      else if (consumable.getConsumptionType() == ConsumptionType.DRINK) {
        int multiplier = 1;
        if (KoLCharacter.hasSkill(SkillPool.EXTRA_KIDNEY)) multiplier += 1;
        if (KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.DRUNK_AVUNCULAR)))
          multiplier += 1;
        start *= multiplier;
        end *= multiplier;
      }
    }

    // Adventure gain modifier #1 is ode or milk, which adds
    // size adventures to the result.

    // Adventure gain modifier #2 is Song of the Glorious Lunch, Rowdy Drinker,
    // Barrel of Laughs or Beer Barrel Polka, which add
    // size adventures to the result.

    // Adventure gain modifier #3 is Gourmand or Neurogourmet, which adds
    // size adventures to the result.

    // Adventure gain modifier #4 is the munchies pill, which adds
    // 1-3 adventures

    // Consumables that generate no adventures do not benefit from ode or milk.
    double average = (start + end) / 2.0 - advs;
    boolean benefit = (average != 0.0);

    double gain0 = benefit ? (average) : 0.0;
    double gain1 = benefit ? (average + size) : 0.0;
    double gain2 = benefit ? (average + size * 2.0) : 0.0;

    // With no effects active, average
    ConsumablesDatabase.addCurrentAdventures(name, size, false, false, false, false, gain0);

    // With only one effect, average + size
    ConsumablesDatabase.addCurrentAdventures(name, size, true, false, false, false, gain1);
    ConsumablesDatabase.addCurrentAdventures(name, size, false, true, false, false, gain1);

    // With two effects, average + size * 2
    ConsumablesDatabase.addCurrentAdventures(name, size, true, true, false, false, gain2);

    // Only foods have effects 3-4
    if (consumable.getConsumptionType() != ConsumptionType.EAT) {
      return;
    }

    // calculate munchies pill effect
    double munchieBonus;
    if (end <= 3) {
      munchieBonus = 3.0;
    } else if (start >= 7) {
      munchieBonus = 1.0;
    } else {
      int munchieTotal = 0;
      for (int i = start; i <= end; i++) {
        munchieTotal += Math.max((12 - i) / 3, 1);
      }
      munchieBonus = (double) munchieTotal / (end - start + 1);
    }

    double gain3 = benefit ? (average + size * 3.0) : 0.0;
    double gain0a = benefit ? (average + munchieBonus) : 0.0;
    double gain1a = benefit ? (average + size + munchieBonus) : 0.0;
    double gain2a = benefit ? (average + size * 2.0 + munchieBonus) : 0.0;
    double gain3a = benefit ? (average + size * 3.0 + munchieBonus) : 0.0;

    ConsumablesDatabase.addCurrentAdventures(name, size, false, true, false, false, gain1);
    ConsumablesDatabase.addCurrentAdventures(name, size, false, false, true, false, gain1);

    // With two effects, average + size * 2
    ConsumablesDatabase.addCurrentAdventures(name, size, true, false, true, false, gain2);
    ConsumablesDatabase.addCurrentAdventures(name, size, false, true, true, false, gain2);

    // With three effects, average + size * 3
    ConsumablesDatabase.addCurrentAdventures(name, size, true, true, true, false, gain3);

    // With only munchies pill, average + 2
    ConsumablesDatabase.addCurrentAdventures(name, size, false, false, false, true, gain0a);

    // With one effect and munchies pill, average + size + 2
    ConsumablesDatabase.addCurrentAdventures(name, size, true, false, false, true, gain1a);
    ConsumablesDatabase.addCurrentAdventures(name, size, false, true, false, true, gain1a);
    ConsumablesDatabase.addCurrentAdventures(name, size, false, false, true, true, gain1a);

    // With two effects and munchies pill, average + size * 2 + 2
    ConsumablesDatabase.addCurrentAdventures(name, size, true, true, false, true, gain2a);
    ConsumablesDatabase.addCurrentAdventures(name, size, true, false, true, true, gain2a);
    ConsumablesDatabase.addCurrentAdventures(name, size, false, true, true, true, gain2a);

    // With three effects and munchies pill, average + size * 3 + 2
    ConsumablesDatabase.addCurrentAdventures(name, size, true, true, true, true, gain3a);
  }

  private static void addCurrentAdventures(
      final String name,
      int unitCost,
      final boolean gainEffect1,
      final boolean gainEffect2,
      final boolean gainEffect3,
      final boolean gainEffect4,
      final double result) {
    // Remove adventure gains from zodiac signs
    ConsumablesDatabase.getAdventureMap(false, gainEffect1, gainEffect2, gainEffect3, gainEffect4)
        .put(name, result);
    ConsumablesDatabase.getAdventureMap(true, gainEffect1, gainEffect2, gainEffect3, gainEffect4)
        .put(name, result / (unitCost == 0 ? 1 : unitCost));
  }

  private static Map<String, Double> getAdventureMap(
      final boolean perUnit,
      final boolean gainEffect1,
      final boolean gainEffect2,
      final boolean gainEffect3,
      final boolean gainEffect4) {
    return ConsumablesDatabase.currentAverageAdventures.get(
        adventureFlagsToKey(perUnit, gainEffect1, gainEffect2, gainEffect3, gainEffect4));
  }

  private static int adventureFlagsToKey(
      final boolean perUnit,
      final boolean gainEffect1,
      final boolean gainEffect2,
      final boolean gainEffect3,
      final boolean gainEffect4) {
    return (perUnit ? 1 : 0) << 4
        | (gainEffect1 ? 1 : 0) << 3
        | (gainEffect2 ? 1 : 0) << 2
        | (gainEffect3 ? 1 : 0) << 1
        | (gainEffect4 ? 1 : 0) << 0;
  }

  private static String extractStatRange(
      String range, double statFactor, int statUnit, int statBonus) {
    if (range == null) {
      return null;
    }

    range = range.trim();

    boolean isNegative = range.startsWith("-");
    if (isNegative) {
      range = range.substring(1);
    }

    int dashIndex = range.indexOf("-");
    int start = StringUtilities.parseInt(dashIndex == -1 ? range : range.substring(0, dashIndex));

    if (dashIndex == -1) {
      double num = (isNegative ? 0 - start : start) + statBonus;
      return KoLConstants.SINGLE_PRECISION_FORMAT.format(statFactor * num / statUnit);
    }

    int end = StringUtilities.parseInt(range.substring(dashIndex + 1));
    double num = (start + end) / (isNegative ? -2.0 : 2.0) + statBonus;
    return KoLConstants.SINGLE_PRECISION_FORMAT.format(
        (isNegative ? 1 : statFactor) * num / statUnit);
  }

  public static void registerConsumable(
      final String itemName, final ConsumptionType usage, final String text) {
    // Get information from description
    if (usage != ConsumptionType.EAT
        && usage != ConsumptionType.DRINK
        && usage != ConsumptionType.SPLEEN) {
      return;
    }

    int level = DebugDatabase.parseLevel(text);
    var quality = DebugDatabase.parseQuality(text);

    // Add consumption data for this session
    ConsumablesDatabase.setConsumptionData(
        itemName,
        DebugDatabase.parseFullness(text),
        DebugDatabase.parseInebriety(text),
        DebugDatabase.parseToxicity(text),
        level,
        quality,
        "0",
        "0",
        "0",
        "0",
        "Unspaded");

    // Print what goes in fullness.txt
    String printMe = ConsumablesDatabase.consumableByName.get(itemName).toString();
    RequestLogger.printLine(printMe);
    RequestLogger.updateSessionLog(printMe);
  }

  public static void updateConsumable(
      final String itemName,
      final int size,
      final int level,
      final ConsumableQuality quality,
      final String advs,
      final String mus,
      final String mys,
      final String mox,
      final String note) {
    // Has to already be in the database.
    Consumable existing = ConsumablesDatabase.consumableByName.get(itemName);
    ConsumablesDatabase.setConsumptionData(
        itemName,
        existing.getRawFullness() != null ? size : null,
        existing.getRawInebriety() != null ? size : null,
        existing.getRawSpleenHit() != null ? size : null,
        level,
        quality,
        advs,
        mus,
        mys,
        mox,
        note);
  }

  public static void updateConsumableNotes(final String itemName, final String notes) {
    // Has to already be in the database.
    Consumable existing = ConsumablesDatabase.consumableByName.get(itemName);
    existing.notes = notes;
  }

  public static final Consumable getConsumableByName(final String name) {
    return ConsumablesDatabase.consumableByName.get(name);
  }

  public static Integer getLevelReq(final Consumable consumable) {
    return consumable == null ? null : consumable.level;
  }

  public static final Integer getLevelReqByName(final String name) {
    Consumable consumable = ConsumablesDatabase.consumableByName.get(name);
    return getLevelReq(consumable);
  }

  public static final boolean meetsLevelRequirement(final String name) {
    if (name == null) {
      return false;
    }

    Integer requirement = getLevelReqByName(name);
    if (requirement == null) {
      return true;
    }
    int req = requirement;
    if (KoLCharacter.getLevel() < req) {
      return false;
    }
    if (req >= 13 && !KoLCharacter.canInteract()) {
      return false;
    }
    return true;
  }

  public static final Integer getRawFullness(final String name) {
    Consumable consumable = ConsumablesDatabase.consumableByName.get(name);
    return consumable == null ? null : consumable.getRawFullness();
  }

  public static final int getFullness(final String name) {
    Integer fullness = ConsumablesDatabase.getRawFullness(name);
    return fullness == null ? 0 : fullness;
  }

  public static final Integer getRawInebriety(final String name) {
    Consumable consumable = ConsumablesDatabase.consumableByName.get(name);
    return consumable == null ? null : consumable.getRawInebriety();
  }

  public static final int getInebriety(final String name) {
    Integer inebriety = ConsumablesDatabase.getRawInebriety(name);
    return inebriety == null ? 0 : inebriety;
  }

  public static final Integer getRawSpleenHit(final String name) {
    Consumable consumable = ConsumablesDatabase.consumableByName.get(name);
    return consumable == null ? null : consumable.getRawSpleenHit();
  }

  public static final int getSpleenHit(final String name) {
    Integer spleenhit = ConsumablesDatabase.getRawSpleenHit(name);
    return spleenhit == null ? 0 : spleenhit;
  }

  public static final ConsumableQuality getQuality(final String name) {
    Consumable consumable = ConsumablesDatabase.consumableByName.get(name);
    return consumable == null ? ConsumableQuality.NONE : consumable.quality;
  }

  public static final String getNotes(final String name) {
    Consumable consumable = ConsumablesDatabase.consumableByName.get(name);
    return consumable == null ? null : consumable.notes;
  }

  private static final Pattern PVP_NOTES_PATTERN =
      Pattern.compile("\\+?(\\d+) PvP fights?", Pattern.CASE_INSENSITIVE);

  public static final int getPvPFights(final String name) {
    int PvPFights = 0;
    String notes = ConsumablesDatabase.getNotes(name);

    if (notes != null) {
      Matcher matcher = PVP_NOTES_PATTERN.matcher(notes);

      if (matcher.find()) {
        PvPFights = Integer.parseInt(matcher.group(1));
      }
    }

    return PvPFights;
  }

  private static double conditionalExtraAdventures(Consumable consumable, final boolean perUnit) {
    int fullness = consumable.getFullness();
    int inebriety = consumable.getInebriety();
    int start = consumable.adventureStart;
    int end = consumable.adventureEnd;
    if (KoLCharacter.inBondcore()
        && "martini.gif".equals(ItemDatabase.getImage(consumable.itemId))) {
      double bonus = 0.0;
      // If we have Tuxedo Shirt equipped, or can get it equipped and have autoTuxedo set, apply 1-3
      // bonus adventures
      if (consumable.isMartini()
          && (KoLCharacter.hasEquipped(ItemPool.get(ItemPool.TUXEDO_SHIRT, 1))
              || Preferences.getBoolean("autoTuxedo")
                  && EquipmentManager.canEquip(ItemPool.TUXEDO_SHIRT)
                  && InventoryManager.itemAvailable(ItemPool.TUXEDO_SHIRT))) {
        bonus += 2.0;
      }
      // +1 Turn from Martini-Drinks from Exotic Bartender, Barry L. Eagle
      if (Preferences.getBoolean("bondMartiniTurn")) {
        bonus += 1.0;
      }
      // +4 Turns (?) Improves Low Quality Martinis from Exotic Olive Procurer, Ben Dover
      if (Preferences.getBoolean("bondMartiniPlus")) {
        // If Martini would have given 10 or more adventures at base, give 4 extra
        for (int i = start; i <= end; i++) {
          if (i < 10) {
            bonus += 4.0 / (end - start + 1);
          }
        }
      }
      return perUnit ? (bonus / inebriety) : bonus;
    }
    if (consumable.isMartini()) {
      // If we have Tuxedo Shirt equipped, or can get it equipped and have autoTuxedo set, apply 1-3
      // bonus adventures
      if (KoLCharacter.hasEquipped(ItemPool.get(ItemPool.TUXEDO_SHIRT, 1))
          || Preferences.getBoolean("autoTuxedo")
              && EquipmentManager.canEquip(ItemPool.TUXEDO_SHIRT)
              && InventoryManager.itemAvailable(ItemPool.TUXEDO_SHIRT)) {
        return perUnit ? (2.0 / inebriety) : 2.0;
      }
      return 0.0;
    }
    if (consumable.isWine()) {
      boolean refinedPalate =
          KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.REFINED_PALATE));
      double bonus = 0.0;
      // With Refined Palate, apply 25% bonus adventures
      // If we have mafia pinky ring equipped, or can get it equipped and have autoPinkyRing set,
      // apply 12.5% bonus adventures
      for (int i = start; i <= end; i++) {
        bonus += refinedPalate ? Math.floor(i * 0.25) / (end - start + 1) : 0.0;
        if (KoLCharacter.hasEquipped(ItemPool.get(ItemPool.MAFIA_PINKY_RING, 1))
            || Preferences.getBoolean("autoPinkyRing")
                && EquipmentManager.canEquip(ItemPool.MAFIA_PINKY_RING)
                && InventoryManager.itemAvailable(ItemPool.MAFIA_PINKY_RING)) {
          double adjustedBase = refinedPalate ? Math.floor(i * 1.25) : i;
          bonus += Math.rint(adjustedBase * 0.125) / (end - start + 1);
        }
      }
      return perUnit ? (bonus / inebriety) : bonus;
    }
    if (consumable.isLasagna()) {
      // If we have Gar-ish effect, or can get the effect and have autoGarish set, apply 5 bonus
      // adventures
      if (!HolidayDatabase.isMonday()
          && (KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.GARISH))
              || Preferences.getBoolean("autoGarish")
                  && (KoLCharacter.hasSkill(SkillPool.CLIP_ART)
                          && UseSkillRequest.getUnmodifiedInstance(SkillPool.CLIP_ART)
                                  .getMaximumCast()
                              > 0
                      || InventoryManager.itemAvailable(ItemPool.FIELD_GAR_POTION)))) {
        return perUnit ? (5.0 / fullness) : 5.0;
      }
      return 0.0;
    }
    if (consumable.isPizza() && KoLCharacter.hasSkill(SkillPool.PIZZA_LOVER)) {
      return perUnit ? 1.0 : fullness;
    }
    if (consumable.isBeans() && KoLCharacter.hasSkill(SkillPool.BEANWEAVER)) {
      return 2.0;
    }
    if (consumable.isSaucy() && KoLCharacter.hasSkill(SkillPool.SAUCEMAVEN)) {
      if (KoLCharacter.isMysticalityClass()) {
        return perUnit ? (5.0 / fullness) : 5.0;
      } else {
        return perUnit ? (3.0 / fullness) : 3.0;
      }
    }
    return 0.0;
  }

  private static int conditionalExtraStats(Consumable consumable) {
    if (consumable.isBeans() && KoLCharacter.hasSkill(SkillPool.BEANWEAVER)) {
      return 25;
    }
    return 0;
  }

  private static double conditionalStatMultiplier(Consumable consumable) {
    int itemId = consumable.itemId;
    // No stat gains from consumables in The Source
    if (KoLCharacter.inTheSource()
        && !(itemId == ItemPool.HACKED_GIBSON || itemId == ItemPool.BROWSER_COOKIE)) {
      return 0.0;
    }

    double factor = 1.0;
    if (consumable.isPizza() && KoLCharacter.hasSkill(SkillPool.PIZZA_LOVER)) {
      factor *= 2.0;
    }
    if (consumable.getRawFullness() != null && KoLCharacter.isPlumber()) {
      factor *= 10.0;
    }
    return factor;
  }

  private static boolean areAdventuresBoosted(Consumable consumable) {
    return switch (ConcoctionDatabase.getMixingMethod(consumable.getConcoction())) {
      case SUSHI, STILLSUIT -> false;
      default -> true;
    };
  }

  public static double getAverageAdventures(final String name) {
    return getAverageAdventures(ConsumablesDatabase.consumableByName.get(name));
  }

  public static double getAverageAdventures(Consumable consumable) {
    if (consumable == null) {
      return 0.0;
    }

    if (KoLCharacter.inSlowcore()) {
      return 0.0;
    }

    String name = consumable.name;

    boolean perUnit = Preferences.getBoolean("showGainsPerUnit");
    Double range = null;

    var adventuresBoosted = areAdventuresBoosted(consumable);

    if (consumable.getRawFullness() != null) {
      boolean milk = Preferences.getBoolean("milkOfMagnesiumActive");
      boolean lunch =
          KoLConstants.activeEffects.contains(ConsumablesDatabase.GLORIOUS_LUNCH)
              || ConsumablesDatabase.BARREL_OF_LAUGHS.getCount(KoLConstants.activeEffects) >= 5;
      boolean gourmand =
          KoLCharacter.hasSkill(SkillPool.GOURMAND)
              || KoLCharacter.hasSkill(SkillPool.NEUROGOURMET);
      boolean munchies = Preferences.getInteger("munchiesPillsUsed") > 0;
      range =
          ConsumablesDatabase.getAdventureMap(
                  perUnit,
                  false,
                  adventuresBoosted && lunch,
                  adventuresBoosted && gourmand,
                  adventuresBoosted && munchies)
              .get(name);
      if (adventuresBoosted && milk) {
        range += 5;
      }
    } else if (consumable.getRawInebriety() != null) {
      boolean odeEffect = KoLConstants.activeEffects.contains(ConsumablesDatabase.ODE);
      boolean rowdyDrinker =
          KoLCharacter.hasSkill(SkillPool.ROWDY_DRINKER)
              || ConsumablesDatabase.BEER_BARREL_POLKA.getCount(KoLConstants.activeEffects) >= 5;
      range =
          ConsumablesDatabase.getAdventureMap(
                  perUnit,
                  adventuresBoosted && odeEffect,
                  adventuresBoosted && rowdyDrinker,
                  false,
                  false)
              .get(name);
    } else if (consumable.getRawSpleenHit() != null) {
      range = ConsumablesDatabase.getAdventureMap(perUnit, false, false, false, false).get(name);
    }

    if (range == null) {
      return 0.0;
    }

    range += ConsumablesDatabase.conditionalExtraAdventures(consumable, perUnit);

    return range;
  }

  private static int getStatUnit(Consumable consumable) {
    if (!Preferences.getBoolean("showGainsPerUnit")) {
      return 1;
    }
    int unit = consumable.getFullness() + consumable.getInebriety() + consumable.getSpleenHit();
    if (unit == 0) {
      unit = 1;
    }
    return unit;
  }

  public static final String getBaseStatByName(final int stat, final String name) {
    Consumable consumable = ConsumablesDatabase.consumableByName.get(name);
    if (consumable == null) {
      return "";
    }

    return consumable.statRangeStrings[stat];
  }

  public static final String getStatRange(final int stat, final String name) {
    return getStatRange(stat, ConsumablesDatabase.consumableByName.get(name));
  }

  public static final String getStatRange(final int stat, final Consumable consumable) {
    if (consumable == null) {
      return "+0.0";
    }

    String statRange = consumable.statRangeStrings[stat];
    DoubleModifier modifier =
        switch (stat) {
          case Consumable.MUSCLE -> DoubleModifier.MUS_EXPERIENCE_PCT;
          case Consumable.MYSTICALITY -> DoubleModifier.MYS_EXPERIENCE_PCT;
          case Consumable.MOXIE -> DoubleModifier.MOX_EXPERIENCE_PCT;
          default -> null;
        };
    double statFactor = (KoLCharacter.currentNumericModifier(modifier) + 100.0) / 100.0;
    statFactor *= ConsumablesDatabase.conditionalStatMultiplier(consumable);
    int statUnit = ConsumablesDatabase.getStatUnit(consumable);
    int statBonus = ConsumablesDatabase.conditionalExtraStats(consumable);
    String range = ConsumablesDatabase.extractStatRange(statRange, statFactor, statUnit, statBonus);
    return range == null ? "+0.0" : range;
  }

  public static final String getBaseMuscleByName(final String name) {
    return getBaseStatByName(Consumable.MUSCLE, name);
  }

  public static final String getMuscleRange(final String name) {
    return getStatRange(Consumable.MUSCLE, name);
  }

  public static final String getBaseMysticalityByName(final String name) {
    return getBaseStatByName(Consumable.MYSTICALITY, name);
  }

  public static final String getMysticalityRange(final String name) {
    return getStatRange(Consumable.MYSTICALITY, name);
  }

  public static final String getBaseMoxieByName(final String name) {
    return getBaseStatByName(Consumable.MOXIE, name);
  }

  public static final String getMoxieRange(final String name) {
    return getStatRange(Consumable.MOXIE, name);
  }

  public enum Attribute {
    MARTINI,
    LASAGNA,
    SAUCY,
    PIZZA,
    BEANS,
    WINE,
    SALAD,
    BEER,
    CANNED
  }

  private static boolean hasAttribute(final int itemId, final Attribute attribute) {
    Consumable consumable = ConsumablesDatabase.consumableByItemId.get(itemId);
    return consumable != null
        && consumable.notes != null
        && consumable.notes.contains(attribute.name());
  }

  public static Set<Attribute> getAttributes(final Consumable consumable) {
    if (consumable == null || consumable.notes == null) return Set.of();
    return Arrays.stream(Attribute.values())
        .filter(a -> consumable.notes.contains(a.name()))
        .collect(Collectors.toSet());
  }

  public static boolean isMartini(final int itemId) {
    return hasAttribute(itemId, Attribute.MARTINI);
  }

  public static boolean isLasagna(final int itemId) {
    return hasAttribute(itemId, Attribute.LASAGNA);
  }

  public static boolean isSaucy(final int itemId) {
    return hasAttribute(itemId, Attribute.SAUCY);
  }

  public static boolean isPizza(final int itemId) {
    return hasAttribute(itemId, Attribute.PIZZA);
  }

  public static boolean isBeans(final int itemId) {
    return hasAttribute(itemId, Attribute.BEANS);
  }

  public static boolean isWine(final int itemId) {
    return hasAttribute(itemId, Attribute.WINE);
  }

  public static boolean isSalad(final int itemId) {
    return hasAttribute(itemId, Attribute.SALAD);
  }

  public static boolean isBeer(final int itemId) {
    return hasAttribute(itemId, Attribute.BEER);
  }

  public static boolean isCannedBeer(final int itemId) {
    return hasAttribute(itemId, Attribute.CANNED);
  }

  // Support for astral consumables and other level dependant consumables

  private static String floatToRange(double average) {
    // Adjust slightly to account for floating point errors.
    long floor = (long) Math.floor(average + 0.0001);
    long ceiling = (long) Math.ceil(average - 0.0001);
    return floor < ceiling
        ? String.format("%s%d-%d", floor < 0 ? "-" : "", floor, ceiling)
        : String.valueOf(floor);
  }

  public static void setLevelVariableConsumables() {
    int level = Math.min(11, Math.max(3, KoLCharacter.getLevel()));

    // astral pilsner:
    //
    // You gain X Adventures.
    // You gain 0-2X Strongness.
    // You gain 0-2X Enchantedness.
    // You gain 0-2X Chutzpah.
    // You gain 1 Drunkenness.
    //
    // X is equal to your level with a minimum of 3 and a maximum of 11

    String name = "astral pilsner";
    int size = ConsumablesDatabase.getInebriety(name);
    String adventures = String.valueOf(level);
    String statGain = "0-" + 2 * level;
    String muscle = statGain;
    String mysticality = statGain;
    String moxie = statGain;
    String note = "";

    ConsumablesDatabase.setConsumptionData(
        name,
        null,
        size,
        null,
        1,
        ConsumableQuality.CHANGING,
        adventures,
        muscle,
        mysticality,
        moxie,
        note);

    // astral hot dog
    //
    // You gain X Adventures.
    // You gain Y Beefiness.
    // You gain Y Enchantedness.
    // You gain Y Cheek.
    // (You gain 3 Fullness.)

    // X and Y are based off of your current level.
    // Levels 1 and 2 use Level 3 stats. The level is capped at level 11.
    // X ranges between 1.8 times your level (rounded up) and 2.2
    //   times your level (rounded down).
    // Y will be between 16 and 20 times your level.

    name = "astral hot dog";
    size = ConsumablesDatabase.getFullness(name);
    int a1 = (int) Math.ceil(1.8 * level);
    int a2 = (int) Math.floor(2.2 * level);
    adventures = a1 + "-" + a2;
    statGain = 16 * level + "-" + 20 * level;
    muscle = statGain;
    mysticality = statGain;
    moxie = statGain;
    note = "";

    ConsumablesDatabase.setConsumptionData(
        name,
        size,
        null,
        null,
        1,
        ConsumableQuality.CHANGING,
        adventures,
        muscle,
        mysticality,
        moxie,
        note);

    // astral energy drink
    //
    // You gain X Adventures.
    // (You gain 8 Spleen.)
    //
    // Adventure gains appear to be 10 + (your level * 2) +/- 3. Gains are
    // (probably) capped at level 11 giving 29-35 adventures, and levels 1-3
    // are (probably) lumped together giving 13-19 adventures.

    name = "astral energy drink";
    size = ConsumablesDatabase.getSpleenHit(name);
    int a = 10 + level * 2;
    adventures = (a - 3) + "-" + (a + 3);
    muscle = "0";
    mysticality = "0";
    moxie = "0";
    note = "";
    ConsumablesDatabase.setConsumptionData(
        name,
        null,
        null,
        size,
        1,
        ConsumableQuality.CHANGING,
        adventures,
        muscle,
        mysticality,
        moxie,
        note);

    // spaghetti breakfast
    //
    // You gain X Adventures.
    // (You gain 1 Fullness.)
    //
    // Adventure gains appear to be 0.5 + (your level/2), capped at level 11.

    name = "spaghetti breakfast";
    size = ConsumablesDatabase.getFullness(name);
    double sbAdv = (level + 1) / 2f;
    muscle = "0";
    mysticality = "0";
    moxie = "0";
    note = "";
    ConsumablesDatabase.setConsumptionData(
        name,
        size,
        null,
        null,
        1,
        ConsumableQuality.CHANGING,
        floatToRange(sbAdv),
        muscle,
        mysticality,
        moxie,
        note);

    // cold one
    //
    // You gain X Adventures.
    // (You gain 1 Fullness.)
    //
    // Adventure gains appear to be 0.5 + (your level/2), capped at level 11.

    name = "Cold One";
    size = ConsumablesDatabase.getInebriety(name);
    double coAdv = (level + 1) / 2f;
    muscle = "0";
    mysticality = "0";
    moxie = "0";
    note = "";
    ConsumablesDatabase.setConsumptionData(
        name,
        null,
        size,
        null,
        1,
        ConsumableQuality.CHANGING,
        floatToRange(coAdv),
        muscle,
        mysticality,
        moxie,
        note);
  }

  public static void setSmoresData() {
    // s'more
    String name = "s'more";
    int size = Preferences.getInteger("smoresEaten") + 1 + ConcoctionDatabase.queuedSmores;
    String adventures = String.valueOf((int) Math.ceil(Math.pow(size, 1.75)));
    String muscle = "0";
    String mysticality = "0";
    String moxie = "0";
    String note = "";
    ConsumablesDatabase.setConsumptionData(
        name,
        size,
        null,
        null,
        1,
        ConsumableQuality.CRAPPY,
        adventures,
        muscle,
        mysticality,
        moxie,
        note);
  }

  public static void setAffirmationCookieData() {
    // Affirmation CookieHandler
    String name = "Affirmation Cookie";
    int size = 1;
    // We don't consider queued cookies as you can't eat two in same day anyway
    int count = Math.min(4, Preferences.getInteger("affirmationCookiesEaten") + 1);
    String adventures = String.valueOf(2 * count + 1);
    String muscle = String.valueOf(30 * count);
    String mysticality = String.valueOf(30 * count);
    String moxie = String.valueOf(30 * count);
    String note = "";
    ConsumablesDatabase.setConsumptionData(
        name,
        size,
        null,
        null,
        1,
        ConsumableQuality.GOOD,
        adventures,
        muscle,
        mysticality,
        moxie,
        note);
  }

  public static void setDistillateData() {
    var drams = Preferences.getInteger("familiarSweat");
    if (drams < 10) drams = 0;

    final var adventures = Math.round(Math.pow(drams, 0.4));
    final var effectTurns = Math.min(100, (int) Math.floor(drams / 5.0));
    Consumable c =
        ConsumablesDatabase.setConsumptionData(
            "stillsuit distillate",
            null,
            1,
            null,
            1,
            ConsumableQuality.CHANGING,
            String.valueOf(adventures),
            "0",
            "0",
            "0",
            effectTurns + " Buzzed on Distillate");
    c.getConcoction().resetCalculations();
  }

  public static void setVariableConsumables() {
    setLevelVariableConsumables();
    setSmoresData();
    setAffirmationCookieData();
    setDistillateData();
  }

  // Support for dusty bottles of wine

  public static final String dustyBottleType(final int itemId) {
    return switch (itemId) {
      case ItemPool.DUSTY_BOTTLE_OF_MERLOT -> "average";
      case ItemPool.DUSTY_BOTTLE_OF_PORT -> "vinegar";
      case ItemPool.DUSTY_BOTTLE_OF_PINOT_NOIR -> "spooky";
      case ItemPool.DUSTY_BOTTLE_OF_ZINFANDEL -> "great";
      case ItemPool.DUSTY_BOTTLE_OF_MARSALA -> "glassy";
      case ItemPool.DUSTY_BOTTLE_OF_MUSCAT -> "bad";
      default -> "dusty";
    };
  }

  public static final String dustyBottleName(final int itemId) {
    String name = ItemDatabase.getItemName(itemId);
    String type = ConsumablesDatabase.dustyBottleType(itemId);
    return type.equals("dusty")
        ? name
        : StringUtilities.globalStringReplace(name, " of", " of " + type);
  }

  public static final boolean consumableOnlyByVampyres(final String name) {
    String notes = ConsumablesDatabase.getNotes(name);
    return (notes != null && notes.startsWith("Vampyre"));
  }

  public static final boolean consumableByVampyres(final String name) {
    return (name.equals("magical sausage") || ConsumablesDatabase.consumableOnlyByVampyres(name));
  }

  public static final boolean consumableByVampyres(final int itemId) {
    return ConsumablesDatabase.consumableByVampyres(ItemDatabase.getDisplayName(itemId));
  }
}
