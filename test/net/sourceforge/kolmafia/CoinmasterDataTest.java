package net.sourceforge.kolmafia;

import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withSkill;
import static internal.helpers.Player.withoutSkill;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.GeneticFiddlingRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CoinmasterDataTest {
  @Nested
  class InZone {
    @Test
    void withoutZoneAccessibleByDefault() {
      var data = new CoinmasterData("test", "test", CoinMasterRequest.class);
      assertThat(data.isAccessible(), is(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Crimbo24", "Removed"})
    void notAccessibleIfParentZoneIsRemoved(final String zone) {
      var data = new CoinmasterData("test", "test", CoinMasterRequest.class).inZone(zone);
      assertThat(data.isAccessible(), is(false));
    }
  }

  @Nested
  class AvailableSkill {
    @Test
    void withUnknownSkill() {
      var data = GeneticFiddlingRequest.DATA;
      var cleanups =
          new Cleanups(withPath(Path.NUCLEAR_AUTUMN), withoutSkill(SkillPool.EXTRA_MUSCLES));
      try (cleanups) {
        assertThat(data.availableSkill(SkillPool.EXTRA_MUSCLES), is(true));
      }
    }

    @Test
    void withKnownSkill() {
      var data = GeneticFiddlingRequest.DATA;
      var cleanups =
          new Cleanups(withPath(Path.NUCLEAR_AUTUMN), withSkill(SkillPool.EXTRA_MUSCLES));
      try (cleanups) {
        assertThat(data.availableSkill(SkillPool.EXTRA_MUSCLES), is(false));
      }
    }
  }
}
