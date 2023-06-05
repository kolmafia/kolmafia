package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withHP;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withSkill;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
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

  @Nested
  class GarbageTote {
    @Test
    public void checkGarbageToteItem() {
      var cleanups = new Cleanups(withItem(ItemPool.GARBAGE_TOTE), withSkill(SkillPool.TORSO));

      try (cleanups) {
        String output = execute("makeshift garbage shirt", true);
        assertContinueState();
        assertThat(output, containsString("January's Garbage Tote => makeshift garbage shirt"));
      }
    }

    @Test
    public void foldGarbageToteItem() {
      HttpClientWrapper.setupFakeClient();
      var cleanups = new Cleanups(withItem(ItemPool.GARBAGE_TOTE), withSkill(SkillPool.TORSO));

      try (cleanups) {
        execute("makeshift garbage shirt");
        assertContinueState();

        var requests = getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/inv_use.php", "whichitem=9690");
        assertPostRequest(requests.get(1), "/choice.php", "whichchoice=1275&option=5");
      }
    }

    @Test
    public void checkReplicaGarbageToteItem() {
      var cleanups =
          new Cleanups(withItem(ItemPool.REPLICA_GARBAGE_TOTE), withPath(Path.LEGACY_OF_LOATHING));

      try (cleanups) {
        String output = execute("tinsel tights", true);
        assertContinueState();
        assertThat(output, containsString("replica January's Garbage Tote => tinsel tights"));
      }
    }

    @Test
    public void foldReplicaGarbageToteItem() {
      HttpClientWrapper.setupFakeClient();
      var cleanups =
          new Cleanups(withItem(ItemPool.REPLICA_GARBAGE_TOTE), withPath(Path.LEGACY_OF_LOATHING));

      try (cleanups) {
        execute("tinsel tights");
        assertContinueState();

        var requests = getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/inv_use.php", "whichitem=11238");
        assertPostRequest(requests.get(1), "/choice.php", "whichchoice=1275&option=3");
      }
    }
  }
}
