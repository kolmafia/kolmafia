package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.withHP;
import static internal.helpers.Player.withItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class FoldItemCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  public static void init() {
    KoLCharacter.reset("testUser");
  }

  public FoldItemCommandTest() {
    this.command = "fold";
  }

  @Test
  public void findClosestFoldableTest() {
    var cleanups =
        new Cleanups(
            // Spooky Putty mitre > leotard > ball > sheet > snake
            withItem(ItemPool.SPOOKY_PUTTY_MITRE),
            // the sheet is a bait; closer but in the wrong direction
            withItem(ItemPool.SPOOKY_PUTTY_SHEET));

    try (cleanups) {
      String output = execute("spooky putty ball", true);

      assertContinueState();
      assertThat(output, containsString("Spooky Putty mitre => Spooky Putty ball"));
    }
  }

  @Test
  public void loopAroundFoldableListTest() {
    var cleanups =
        new Cleanups(
            // Spooky Putty mitre > leotard > ball > sheet > snake
            withItem(ItemPool.SPOOKY_PUTTY_SNAKE),
            // bait, again
            withItem(ItemPool.SPOOKY_PUTTY_BALL));

    try (cleanups) {
      String output = execute("spooky putty leotard", true);

      assertContinueState();
      assertThat(output, containsString("Spooky Putty snake => Spooky Putty leotard"));
    }
  }

  @Test
  public void restoreHPWhenNeededTest() {
    var cleanups =
        new Cleanups(
            // Spooky Putty mitre > leotard > ball > sheet > snake
            withItem(ItemPool.SPOOKY_PUTTY_SNAKE), withHP(5, 100, 100));

    try (cleanups) {
      String output = execute("spooky putty mitre");

      // We didn't give it anything to restore HP with, so we can use that to tell it tried
      assertErrorState();
      assertThat(output, containsString("Autorecovery failed."));
    }
  }
}
