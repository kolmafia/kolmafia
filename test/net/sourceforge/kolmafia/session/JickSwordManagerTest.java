package net.sourceforge.kolmafia.session;

import static net.sourceforge.kolmafia.modifiers.DoubleModifier.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class JickSwordManagerTest {
  // Every case below is a real sword, scraped from desc_item.php.
  private static Stream<Arguments> swords() {
    return Stream.of(
        Arguments.of(
            1197090,
            List.of(SPELL_DAMAGE_PCT, COLD_DAMAGE, SLEAZE_RESISTANCE, ZOMBIE_DAMAGE, ITEMDROP),
            List.of(100, 40, 3, 80, 10)),
        // Lowest resistance, and a monster type that takes flat damage.
        Arguments.of(
            18991,
            List.of(MUS_PCT, STENCH_DAMAGE, SLEAZE_RESISTANCE, ORC_DAMAGE, INITIATIVE),
            List.of(90, 25, 1, 140, 15)),
        // Resistance at its cap, which narrows the pool for the remaining points.
        Arguments.of(
            21,
            List.of(WEAPON_DAMAGE_PCT, SLEAZE_DAMAGE, COLD_RESISTANCE, VAMPIRE_DAMAGE, ITEMDROP),
            List.of(130, 30, 5, 50, 10)),
        // Extremes of each value, which the 29 point budget makes mutually exclusive.
        Arguments.of(
            1069035,
            List.of(
                WEAPON_DAMAGE_PCT, COLD_DAMAGE, HOT_RESISTANCE, SKELETON_DAMAGE, FAMILIAR_WEIGHT),
            List.of(150, 30, 2, 60, 5)),
        Arguments.of(
            130427,
            List.of(MYS_PCT, HOT_DAMAGE, SPOOKY_RESISTANCE, BUGBEAR_DAMAGE, MEATDROP),
            List.of(60, 80, 1, 60, 20)),
        Arguments.of(
            836140,
            List.of(MUS_PCT, HOT_DAMAGE, COLD_RESISTANCE, GHOST_DAMAGE, CRITICAL_PCT),
            List.of(40, 25, 4, 160, 20)),
        Arguments.of(
            126855,
            List.of(MYS_PCT, COLD_DAMAGE, STENCH_RESISTANCE, BUGBEAR_DAMAGE, INITIATIVE),
            List.of(20, 50, 4, 130, 15)),
        Arguments.of(
            1569204,
            List.of(MOX_PCT, STENCH_DAMAGE, SLEAZE_RESISTANCE, VAMPIRE_DAMAGE, CRITICAL_PCT),
            List.of(140, 10, 4, 90, 20)));
  }

  @ParameterizedTest
  @MethodSource("swords")
  void derivesSword(
      final int playerId, final List<DoubleModifier> modifiers, final List<Integer> values) {
    var sword = JickSwordManager.getModifiers(playerId);

    assertThat(sword.keySet(), contains(modifiers.toArray()));
    assertThat(List.copyOf(sword.values()), is(values));
  }

  @Test
  void alwaysSpendsExactlyTwentyNinePoints() {
    // The four budgeted modifiers come first, in a fixed order, at these scales.
    var scales = List.of(10, 5, 1, 10);

    for (var playerId = 1; playerId < 500; playerId++) {
      var values = List.copyOf(JickSwordManager.getModifiers(playerId).values());
      var total = 0;
      for (var i = 0; i < scales.size(); i++) {
        total += values.get(i) / scales.get(i);
      }
      assertThat("player " + playerId, total, is(29));
    }
  }

  @Test
  void rendersModifierString() {
    assertThat(
        JickSwordManager.getModifierList(1197090).toString(),
        is(
            "Spell Damage Percent: +100, Cold Damage: +40, Sleaze Resistance: +3, Damage vs. Zombies: +80, Item Drop: +10"));
  }
}
