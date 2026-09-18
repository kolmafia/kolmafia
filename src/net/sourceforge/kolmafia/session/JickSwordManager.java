package net.sourceforge.kolmafia.session;

import static net.sourceforge.kolmafia.modifiers.DoubleModifier.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.ModifierList;
import net.sourceforge.kolmafia.utilities.PHPMTRandom;

/**
 * The Sword of Procedural Generation's enchantments are seeded off the owner's player id, so they
 * can be derived without ever looking at the item description.
 *
 * <p>Spaded by Gausie.
 */
public class JickSwordManager {
  private JickSwordManager() {}

  private enum Slot {
    STAT,
    ELEMENT,
    RESISTANCE,
    MONSTER
  }

  // Two tickets each for stat, element and monster; one for resistance. This is the roll order.
  private static final List<Slot> TICKETS =
      List.of(
          Slot.STAT,
          Slot.STAT,
          Slot.ELEMENT,
          Slot.ELEMENT,
          Slot.RESISTANCE,
          Slot.MONSTER,
          Slot.MONSTER);

  // 25 points are shared out between the four slots, which each start with one.
  private static final int POINTS = 25;

  // Resistance is capped at level 5, after which it leaves the pool.
  private static final int RESISTANCE_CAP = 5;

  private static final Map<Slot, Integer> SCALES =
      Map.of(Slot.STAT, 10, Slot.ELEMENT, 5, Slot.RESISTANCE, 1, Slot.MONSTER, 10);

  private static final List<DoubleModifier> STATS =
      List.of(MUS_PCT, MYS_PCT, MOX_PCT, WEAPON_DAMAGE_PCT, SPELL_DAMAGE_PCT);

  private static final List<DoubleModifier> ELEMENTAL_DAMAGE =
      List.of(HOT_DAMAGE, COLD_DAMAGE, SPOOKY_DAMAGE, STENCH_DAMAGE, SLEAZE_DAMAGE);

  // Rolled against the same element order as the damage above.
  private static final List<DoubleModifier> ELEMENTAL_RESISTANCE =
      List.of(
          HOT_RESISTANCE, COLD_RESISTANCE, SPOOKY_RESISTANCE, STENCH_RESISTANCE, SLEAZE_RESISTANCE);

  private static final List<DoubleModifier> MONSTER_TYPES =
      List.of(
          ORC_DAMAGE,
          SKELETON_DAMAGE,
          ZOMBIE_DAMAGE,
          VAMPIRE_DAMAGE,
          WEREWOLF_DAMAGE,
          GHOST_DAMAGE,
          BUGBEAR_DAMAGE);

  private static final List<Map.Entry<DoubleModifier, Integer>> BONUSES =
      List.of(
          Map.entry(INITIATIVE, 15),
          Map.entry(CRITICAL_PCT, 20),
          Map.entry(ITEMDROP, 10),
          Map.entry(MEATDROP, 20),
          Map.entry(FAMILIAR_WEIGHT, 5));

  /**
   * The enchantments on the Sword of Procedural Generation belonging to the given player.
   *
   * @param playerId Owner's player id
   * @return Modifiers, in the order the game lists them
   */
  public static Map<DoubleModifier, Integer> getModifiers(final int playerId) {
    var rng = new PHPMTRandom(playerId);

    var points = new EnumMap<Slot, Integer>(Slot.class);
    for (var slot : Slot.values()) {
      points.put(slot, 1);
    }

    var pool = new ArrayList<>(TICKETS);
    for (var i = 0; i < POINTS; i++) {
      var slot = rng.pickOne(pool);
      var level = points.merge(slot, 1, Integer::sum);
      if (slot == Slot.RESISTANCE && level >= RESISTANCE_CAP) {
        pool.remove(Slot.RESISTANCE);
      }
    }

    var modifiers = new LinkedHashMap<DoubleModifier, Integer>();
    modifiers.put(rng.pickOne(STATS), value(points, Slot.STAT));
    modifiers.put(rng.pickOne(ELEMENTAL_DAMAGE), value(points, Slot.ELEMENT));
    modifiers.put(rng.pickOne(ELEMENTAL_RESISTANCE), value(points, Slot.RESISTANCE));
    modifiers.put(rng.pickOne(MONSTER_TYPES), value(points, Slot.MONSTER));
    var bonus = rng.pickOne(BONUSES);
    modifiers.put(bonus.getKey(), bonus.getValue());

    return modifiers;
  }

  /**
   * The enchantments on the Sword of Procedural Generation belonging to the given player.
   *
   * @param playerId Owner's player id
   * @return Modifiers, in the order the game lists them
   */
  public static ModifierList getModifierList(final int playerId) {
    var list = new ModifierList();
    getModifiers(playerId).forEach((mod, value) -> list.addModifier(mod.getName(), "+" + value));
    return list;
  }

  private static int value(final Map<Slot, Integer> points, final Slot slot) {
    return points.get(slot) * SCALES.get(slot);
  }
}
