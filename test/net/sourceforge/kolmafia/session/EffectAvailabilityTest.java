package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.withClass;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import org.junit.jupiter.api.Test;

class EffectAvailabilityTest {
  @Test
  void checksClassRestrictions() {
    try (var cleanups = new Cleanups(withClass(AscensionClass.SEAL_CLUBBER))) {
      assertThat(EffectAvailability.cannotGain(EffectPool.NEARLY_SILENT_HUNTING), is(true));
      assertThat(EffectAvailability.cannotGain(EffectPool.SILENT_HUNTING), is(false));
    }
  }
}
