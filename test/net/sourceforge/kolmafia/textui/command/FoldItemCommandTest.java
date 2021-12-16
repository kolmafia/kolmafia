package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FoldItemCommandTest extends AbstractCommandTestBase {
  @BeforeEach
  public void initEach() {
    KoLCharacter.reset("testUser");
    KoLCharacter.reset(true);
  }

  public FoldItemCommandTest() {
    this.command = "fold";
  }

  @Test
  public void findClosestFoldableTest() {
    KoLmafiaCLI.isExecutingCheckOnlyCommand = true;

    // Spooky Putty mitre > leotard > ball > sheet > snake
    AdventureResult.addResultToList(
        KoLConstants.inventory, ItemPool.get(ItemPool.SPOOKY_PUTTY_MITRE));
    // the sheet is a bait; closer but in the wrong direction
    AdventureResult.addResultToList(
        KoLConstants.inventory, ItemPool.get(ItemPool.SPOOKY_PUTTY_SHEET));

    String output = execute("spooky putty ball");

    assertContinueState();
    assertThat(output, containsString("Spooky Putty mitre => Spooky Putty ball"));
    KoLmafiaCLI.isExecutingCheckOnlyCommand = false;
  }

  @Test
  public void loopAroundFoldableListTest() {
    KoLmafiaCLI.isExecutingCheckOnlyCommand = true;

    // Spooky Putty mitre > leotard > ball > sheet > snake
    AdventureResult.addResultToList(
        KoLConstants.inventory, ItemPool.get(ItemPool.SPOOKY_PUTTY_SNAKE));
    // bait, again
    AdventureResult.addResultToList(
        KoLConstants.inventory, ItemPool.get(ItemPool.SPOOKY_PUTTY_BALL));

    String output = execute("spooky putty leotard");

    assertContinueState();
    assertThat(output, containsString("Spooky Putty snake => Spooky Putty leotard"));
    KoLmafiaCLI.isExecutingCheckOnlyCommand = false;
  }

  @Test
  public void restoreHPWhenNeededTest() {
    // Spooky Putty mitre > leotard > ball > sheet > snake
    AdventureResult.addResultToList(
        KoLConstants.inventory, ItemPool.get(ItemPool.SPOOKY_PUTTY_SNAKE));
    KoLCharacter.setHP(5, 100, 100);

    String output = execute("spooky putty mitre");

    // We didn't give it anything to restore HP with, so we can use that to tell it tried
    assertErrorState();
    assertThat(output, containsString("Autorecovery failed."));
  }
}
