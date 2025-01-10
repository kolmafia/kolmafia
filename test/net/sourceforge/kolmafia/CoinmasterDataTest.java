package net.sourceforge.kolmafia;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
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
}
