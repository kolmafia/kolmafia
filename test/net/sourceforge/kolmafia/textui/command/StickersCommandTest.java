package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class StickersCommandTest extends AbstractCommandTestBase {
  public StickersCommandTest() {
    this.command = "stickers";
  }

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("StickersCommandTest");
  }

  @Test
  public void abortsWithoutStickers() {
    String output = this.execute("wrestler");

    assertErrorState();
    assertThat(
        output, containsString("You need 1 more scratch 'n' sniff wrestler sticker to continue"));
  }

  @Test
  public void equipsSubset() {
    HttpClientWrapper.setupFakeClient();
    var cleanups =
        new Cleanups(withItem(ItemPool.UNICORN_STICKER), withItem(ItemPool.APPLE_STICKER));

    try (cleanups) {
      String output = this.execute("unicorn, apple sticker");
      assertThat(output, containsString("Putting on scratch 'n' sniff unicorn sticker..."));
      assertThat(output, containsString("Putting on scratch 'n' sniff apple sticker..."));

      var requests = getRequests();
      assertThat(requests, hasSize(2));
      assertPostRequest(requests.get(0), "/bedazzle.php", "sticker=3509&action=juststick");
      // this one should be "stick", but because we're faking the requests Mafia doesn't know we'll
      // have a sticker sword now
      assertPostRequest(requests.get(1), "/bedazzle.php", "sticker=3510&action=juststick");
    }
  }

  @Test
  public void equipsAllToUnequipped() {
    HttpClientWrapper.setupFakeClient();
    var cleanups =
        new Cleanups(withItem(ItemPool.UPC_STICKER, 3), withEquippableItem(ItemPool.STICKER_SWORD));

    try (cleanups) {
      this.execute("UPC, UPC, UPC");

      var requests = getRequests();
      assertThat(requests, hasSize(3));
      assertPostRequest(requests.get(0), "/bedazzle.php", "slot=1&sticker=3511&action=stick");
      assertPostRequest(requests.get(1), "/bedazzle.php", "slot=2&sticker=3511&action=stick");
      assertPostRequest(requests.get(2), "/bedazzle.php", "slot=3&sticker=3511&action=stick");
    }
  }

  @Test
  public void equipsWithExistingStickers() {
    HttpClientWrapper.setupFakeClient();
    var cleanups =
        new Cleanups(
            withItem(ItemPool.ROCK_BAND_STICKER, 3),
            withEquippableItem(ItemPool.STICKER_SWORD),
            withEquipped(Slot.STICKER1, ItemPool.DRAGON_STICKER),
            withEquipped(Slot.STICKER3, ItemPool.WRESTLER_STICKER));

    try (cleanups) {
      this.execute("rock band, rock band, rock band");

      var requests = getRequests();
      assertThat(requests, hasSize(1));
      assertPostRequest(requests.get(0), "/bedazzle.php", "slot=2&sticker=3514&action=stick");
    }
  }
}
