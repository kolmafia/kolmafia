package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.DebugDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;

public class LocketManager {
  private static final Set<Integer> knownMonsters = new TreeSet<>();
  private static final Pattern REMINISCABLE_MONSTER = Pattern.compile("<option value=\"(\\d+)\"");
  private static final Set<String> CONSTANT_MODS =
      Set.of("HP Regen Min", "HP Regen Max", "MP Regen Min", "MP Regen Max", "Single Equip");

  private static Stream<String> getFoughtMonsters() {
    return Arrays.stream(Preferences.getString("_locketMonstersFought").split(","))
        .filter(Predicate.not(String::isBlank));
  }

  private static void addFoughtMonster(int monsterId) {
    // Add monster id to pref ensuring distinct
    Preferences.setString(
        "_locketMonstersFought",
        Stream.concat(getFoughtMonsters(), Stream.of(Integer.toString(monsterId)))
            .distinct()
            .collect(Collectors.joining(",")));
  }

  private LocketManager() {}

  public static void rememberMonster(int monsterId) {
    knownMonsters.add(monsterId);
  }

  public static Set<Integer> getMonsters() {
    return Collections.unmodifiableSet(knownMonsters);
  }

  public static boolean remembersMonster(int monsterId) {
    return knownMonsters.contains(monsterId);
  }

  public static void parseMonsters(final String text) {
    // Visiting the reminisce page is a source of truth
    knownMonsters.clear();

    // Add all monsters from the reminisce page
    var m = REMINISCABLE_MONSTER.matcher(text);
    while (m.find()) {
      knownMonsters.add(Integer.parseInt(m.group(1)));
    }

    // Add all the monsters you've foguht today, which will not otherwise show on said page
    getFoughtMonsters().map(Integer::parseInt).forEach(knownMonsters::add);
  }

  public static void parseFight(final MonsterData monster, final String text) {
    if (!text.contains("loverslocketframe.png")) {
      return;
    }

    // This will not double an existing id so is safe to run at any round
    addFoughtMonster(monster.getId());

    Preferences.setString("locketPhylum", monster.getPhylum().toString());
  }

  /**
   * Work out the current locket phylum from the locket's item description
   *
   * @param responseText HTML for the item description to parse
   */
  public static void parseLocket(final String responseText) {
    var mods = new Modifiers.ModifierList();

    // The plan here is to parse the locket description...
    DebugDatabase.parseItemEnchantments(
        responseText, mods, new ArrayList<>(), KoLConstants.EQUIP_ACCESSORY);

    // ...find the first modifier that can indicate the phylum...
    var indicativeMod =
        mods.stream()
            .map(Modifiers.Modifier::getName)
            .filter(Predicate.not(CONSTANT_MODS::contains))
            .findAny()
            .orElse(null);

    // ... grab the raw mod string for the locket from modifiers.txt...
    var locketModString =
        Modifiers.getItemModifiers(ItemPool.COMBAT_LOVERS_LOCKET).getString(Modifiers.MODIFIERS);

    String phylum = "";

    // ... and the locket has a phylum at all...
    if (indicativeMod != null) {
      // Weapon Damage: [pref(locketPhylum,weird)*25]
      int start = locketModString.indexOf(indicativeMod) + indicativeMod.length() + 21;
      // ... we find the indicative modifier in the raw mod string and see what phylum it is
      // associated with.
      phylum = locketModString.substring(start, locketModString.indexOf(")", start));
    }

    Preferences.setString("locketPhylum", phylum);
  }
}
