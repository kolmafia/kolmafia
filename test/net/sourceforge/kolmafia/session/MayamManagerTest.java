package net.sourceforge.kolmafia.session;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MayamManagerTest {
  @Nested
  class YamBatteryEffects {
    private List<String> roll(final int daycount) {
      return MayamManager.yamBatteryEffects(daycount).stream()
          .map(e -> e.getCount() + " " + e.getName())
          .toList();
    }

    @Test
    void pairsEachEffectWithItsDuration() {
      assertThat(
          roll(8619), contains("10 Piratey Flavor", "20 Make Meat FA$T!", "30 Thaumodynamic"));
    }

    @Test
    void rollsThreeEffectsForADay() {
      assertThat(
          roll(8604), contains("10 Hippy Flavor", "20 Celestial Body", "30 Human-Goblin Hybrid"));
    }

    @Test
    void matchesTheDumpForLaterDays() {
      assertThat(
          roll(8654), contains("10 Cold as Ice", "20 Space Tripping", "30 Dwarven Hardiness"));
    }

    @Test
    void returnsNothingWhenNotLoggedIn() {
      assertThat(MayamManager.yamBatteryEffects(0), is(List.of()));
    }
  }
}
