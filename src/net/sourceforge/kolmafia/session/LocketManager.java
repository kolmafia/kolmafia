package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.modifiers.ModifierList;
import net.sourceforge.kolmafia.modifiers.ModifierList.ModifierValue;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.DebugDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.LocketRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class LocketManager {
  private static final Set<Integer> knownMonsters = new TreeSet<>();
  private static final Pattern REMINISCABLE_MONSTER = Pattern.compile("<option value=\"(\\d+)\"");
  private static final Set<String> CONSTANT_MODS =
      Set.of("HP Regen Min", "HP Regen Max", "MP Regen Min", "MP Regen Max", "Single Equip");

  private static void addFoughtMonster(int monsterId) {
    Set<Integer> foughtMonsters = new TreeSet<>(getFoughtMonsters());
    foughtMonsters.add(monsterId);

    // Add monster id to pref ensuring distinct
    Preferences.setString(
        "_locketMonstersFought",
        foughtMonsters.stream().map(Object::toString).distinct().collect(Collectors.joining(",")));
  }

  private LocketManager() {}

  public static Set<Integer> getMonsters() {
    return Collections.unmodifiableSet(knownMonsters);
  }

  public static void rememberMonster(int monsterId) {
    knownMonsters.add(monsterId);
  }

  public static boolean remembersMonster(int monsterId) {
    return knownMonsters.contains(monsterId);
  }

  public static boolean remembersMonster(MonsterData monster) {
    return remembersMonster(monster.getId());
  }

  public static boolean foughtMonster(int monsterId) {
    return getFoughtMonsters().contains(monsterId);
  }

  public static boolean foughtMonster(MonsterData monster) {
    return foughtMonster(monster.getId());
  }

  public static Set<Integer> getFoughtMonsters() {
    return Arrays.stream(Preferences.getString("_locketMonstersFought").split(","))
        .filter(StringUtilities::isNumeric)
        .map(Integer::parseInt)
        .collect(Collectors.toUnmodifiableSet());
  }

  public static void parseMonsters(final String text) {
    // Visiting the reminisce page is a source of truth
    knownMonsters.clear();

    // Add all monsters from the reminisce page
    var m = REMINISCABLE_MONSTER.matcher(text);
    while (m.find()) {
      knownMonsters.add(Integer.parseInt(m.group(1)));
    }

    // Add all the monsters you've fought today, which will not otherwise show on said page
    knownMonsters.addAll(getFoughtMonsters());
  }

  public static boolean isLocketFight(final String text) {
    return (text.contains("loverslocketframe.png")
        // Sometimes there's no frame?
        || text.contains("your locket changes to reflect"));
  }

  public static void parseFight(final MonsterData monster) {
    if (monster == null) return;
    // This will not double an existing id so is safe to run at any round
    addFoughtMonster(monster.getId());
    EncounterManager.ignoreSpecialMonsters();

    Preferences.setString("locketPhylum", monster.getPhylum().toString());
  }

  /**
   * Work out the current locket phylum from the locket's item description
   *
   * @param responseText HTML for the item description to parse
   */
  public static void parseLocket(final String responseText) {
    var mods = new ModifierList();

    // The plan here is to parse the locket description...
    DebugDatabase.parseItemEnchantments(
        responseText, mods, new ArrayList<>(), ConsumptionType.ACCESSORY);

    // ...find the first modifier that can indicate the phylum...
    var indicativeMod =
        mods.stream()
            .map(ModifierValue::getName)
            .filter(Predicate.not(CONSTANT_MODS::contains))
            .findAny()
            .orElse(null);

    // ... grab the raw mod string for the locket from modifiers.txt...
    var locketModString =
        ModifierDatabase.getItemModifiers(ItemPool.COMBAT_LOVERS_LOCKET)
            .getString(StringModifier.MODIFIERS);

    String phylum = "";

    // ... and the locket has a phylum at all...
    if (indicativeMod != null) {
      // Weapon Damage: [pref(locketPhylum,weird)*25]
      int start = locketModString.indexOf(indicativeMod + ": ") + indicativeMod.length() + 21;
      // ... we find the indicative modifier in the raw mod string and see what phylum it is
      // associated with.
      phylum = locketModString.substring(start, locketModString.indexOf(")", start));
    }

    Preferences.setString("locketPhylum", phylum);
  }

  public static boolean own() {
    // true if you have a locket somewhere
    return InventoryManager.getAccessibleCount(ItemPool.COMBAT_LOVERS_LOCKET) > 0;
  }

  public static boolean onhand() {
    // true if locket is ready to use
    return InventoryManager.getCount(ItemPool.COMBAT_LOVERS_LOCKET) > 0
        || KoLCharacter.hasEquipped(ItemPool.COMBAT_LOVERS_LOCKET);
  }

  public static boolean retrieve() {
    // If the locket is equipped or in inventory, it's good to go.
    if (onhand()) {
      return true;
    }
    // If it is retrievable, do so
    return (own() && InventoryManager.retrieveItem(ItemPool.COMBAT_LOVERS_LOCKET, 1));
  }

  public static void clear() {
    knownMonsters.clear();
  }

  public static void reset() {
    if (!own()) {
      clear();
      return;
    }

    RequestThread.postRequest(new LocketRequest());
  }

  // <option value="552" >spider gremlin</option>
  // <option value="553" >spider gremlin</option>
  private static final Pattern MONSTER_PATTERN =
      Pattern.compile("<option value=\"(\\d+)\".*?>([^<]+)</option>");

  public static void decorateMonsterDropdown(final StringBuffer buffer) {
    // Called when we have reminisced.
    // Disambiguate certain monsters
    Matcher matcher = MONSTER_PATTERN.matcher(buffer);
    while (matcher.find()) {
      int monsterId = Integer.valueOf(matcher.group(1));
      String name = matcher.group(2);
      MonsterData monster = MonsterDatabase.findMonsterById(monsterId);
      if (monster != null) {
        String monsterName = monster.getName();
        if (!name.equals(monsterName)) {
          buffer.replace(matcher.start(2), matcher.end(2), monsterName);
        }
      }
    }
  }
}
