package net.sourceforge.kolmafia.request;

import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withWorkshedItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class TakerSpaceRequestTest {

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("TakerSpaceRequestTest");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("TakerSpaceRequestTest");
  }

  @Nested
  class CanMake {
    @Test
    public void canMakeLimitedByMaxIngredients() {
      var cleanups =
          new Cleanups(
              withProperty("takerSpaceGold", 13),
              withWorkshedItem(ItemPool.TAKERSPACE_LETTER_OF_MARQUE));
      try (cleanups) {
        var conc = ConcoctionPool.get(ItemPool.GOLDEN_PET_ROCK);
        assertThat(TakerSpaceRequest.canMake(conc), equalTo(1));
      }
    }

    @Test
    public void withMultipleIngredientsLimitedByLowest() {
      var cleanups =
          new Cleanups(
              withProperty("takerSpaceRum", 13),
              withProperty("takerSpaceAnchor", 3),
              withProperty("takerSpaceMast", 4),
              withProperty("takerSpaceGold", 21),
              withWorkshedItem(ItemPool.TAKERSPACE_LETTER_OF_MARQUE));
      try (cleanups) {
        var conc = ConcoctionPool.get(ItemPool.JOLLY_ROGER_TATTOO_KIT);
        assertThat(TakerSpaceRequest.canMake(conc), equalTo(2));
      }
    }
  }
}
