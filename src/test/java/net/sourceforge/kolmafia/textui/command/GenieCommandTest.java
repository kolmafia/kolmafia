package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class GenieCommandTest extends AbstractCommandTestBase {

  public GenieCommandTest() {
    this.command = "genie";
  }

  @Nested
  class Pocket {
    @Test
    public void canWishForPocketWishesWithBottle() {
      var cleanups = withItem(ItemPool.GENIE_BOTTLE);

      try (cleanups) {
        execute("item pocket");
        assertContinueState();
      }
    }

    @Test
    public void cannotWishForPocketWishesWithPocketWishes() {
      var cleanups = withItem(ItemPool.POCKET_WISH);

      try (cleanups) {
        String output = execute("item pocket");
        assertThat(output, containsString("Don't use a pocket wish to make a pocket wish."));
        assertErrorState();
      }
    }

    @Test
    public void canWishForPocketWishesWithReplicaBottle() {
      var cleanups =
          new Cleanups(withPath(Path.LEGACY_OF_LOATHING), withItem(ItemPool.REPLICA_GENIE_BOTTLE));

      try (cleanups) {
        execute("item pocket");
        assertContinueState();
      }
    }
  }
}
