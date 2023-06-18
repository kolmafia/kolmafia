package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withHP;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSkill;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.equipment.Slot;
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

  @Test
  public void unequipsFoldable() {
    HttpClientWrapper.setupFakeClient();
    var cleanups = new Cleanups(withEquipped(Slot.WEAPON, ItemPool.SPOOKY_PUTTY_SNAKE));

    try (cleanups) {
      execute("Spooky Putty mitre");
      assertContinueState();

      var requests = getRequests();
      // it doesn't carry out the folds due to unequipping not actually populating the inventory
      assertThat(requests, hasSize(greaterThanOrEqualTo(1)));
      assertPostRequest(
          requests.get(0), "/inv_equip.php", "which=2&ajax=1&action=unequip&type=weapon");
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

  @Nested
  class AmbiguousFold {
    @Test
    public void errorOnAmbiguousFold() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.WEAPON, ItemPool.STINKY_CHEESE_SWORD),
              withEquipped(Slot.PANTS, ItemPool.STINKY_CHEESE_DIAPER));

      try (cleanups) {
        String output = execute("stinky cheese eye");
        assertErrorState();
        assertThat(output, containsString("Unequip the item you want to fold into that."));
      }
    }

    @Test
    public void itemInInventoryIsNotAmbiguous() {
      HttpClientWrapper.setupFakeClient();
      var cleanups =
          new Cleanups(
              withEquipped(Slot.WEAPON, ItemPool.STINKY_CHEESE_SWORD),
              withEquipped(Slot.PANTS, ItemPool.STINKY_CHEESE_DIAPER),
              withItem(ItemPool.STINKY_CHEESE_WHEEL));

      try (cleanups) {
        execute("stinky cheese eye");
        assertContinueState();

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(requests.get(0), "/inv_use.php", "whichitem=4401&ajax=1");
      }
    }

    @Test
    public void doNotErrorOnAmbiguousFoldWithPreference() {
      HttpClientWrapper.setupFakeClient();
      var cleanups =
          new Cleanups(
              withEquipped(Slot.WEAPON, ItemPool.STINKY_CHEESE_SWORD),
              withEquipped(Slot.PANTS, ItemPool.STINKY_CHEESE_DIAPER),
              withProperty("errorOnAmbiguousFold", false));

      try (cleanups) {
        String output = execute("stinky cheese eye", true);
        assertContinueState();
        assertThat(output, containsString("stinky cheese diaper => stinky cheese eye"));
      }
    }
  }
}
