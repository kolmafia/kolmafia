package net.sourceforge.kolmafia.session;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class BeretManagerTest {
  @Nested
  class BuskingEffects {
    private List<String> busk(final long power, final long cast) {
      return BeretManager.buskingEffects(power, cast).stream()
          .map(e -> e.getCount() + " " + e.getName())
          .toList();
    }

    @Test
    void rollsAnEffectPerHundredPowerAfterTheMeat() {
      assertThat(
          busk(140, 1), contains("29 Meat", "10 Newt Gets In Your Eyes", "10 Greasy Flavor"));
    }

    @Test
    void divesPowerAbove1100IntoDiminishingReturns() {
      // 2000 power busks as 1328, so 14 effects rather than 20
      var results = busk(2000, 1);

      assertThat(results, hasSize(15));
      assertThat(results.get(0), is("267 Meat"));
    }
  }
}
