package net.sourceforge.kolmafia.request;

import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.concoction.StillSuitRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StillSuitRequestTest {
  @BeforeEach
  void beforeEach() {
    KoLCharacter.reset(true);
    KoLCharacter.reset("StillSuitRequestTest");
    Preferences.reset("StillSuitRequestTest");
  }

  @Test
  void canMakeIfStillSuitIsInTerrarium() {
    var cleanups =
        new Cleanups(
            withFamiliarInTerrarium(FamiliarPool.BOWLET), withProperty("familiarSweat", 20));

    try (cleanups) {
      var fam = KoLCharacter.usableFamiliar(FamiliarPool.BOWLET);
      fam.setItem(ItemPool.get(ItemPool.STILLSUIT));
      assertThat(StillSuitRequest.canMake(), is(true));
    }
  }

  @Test
  void canIdentifyDistillate() {
    assertThat(StillSuitRequest.isDistillate("stillsuit distillate"), is(true));
    assertThat(StillSuitRequest.isDistillate("meep moop beep boop"), is(false));
  }
}
