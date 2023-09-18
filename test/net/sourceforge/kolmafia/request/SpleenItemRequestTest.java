package net.sourceforge.kolmafia.request;

import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSpleenUse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SpleenItemRequestTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("SpleenItemRequestTest");
    Preferences.reset("SpleenItemRequestTest");
  }

  @Nested
  class MaximumUses {
    @Test
    void limitedBySpleenCapacity() {
      try (var cleanups = withSpleenUse(0)) {
        var max = SpleenItemRequest.maximumUses(ItemPool.SHADOW_PILL);
        assertThat(max, is(15));
      }
    }

    @Test
    void onlyThreeTurkeyBlastersDaily() {
      try (var cleanups = withProperty("_turkeyBlastersUsed", 2)) {
        var max = SpleenItemRequest.maximumUses(ItemPool.TURKEY_BLASTER);
        assertThat(max, is(1));
        assertThat(SpleenItemRequest.limiter, is("daily limit"));
      }
    }
  }
}
