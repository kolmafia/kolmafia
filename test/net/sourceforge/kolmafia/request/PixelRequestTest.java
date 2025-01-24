package net.sourceforge.kolmafia.request;

import static internal.matchers.Item.isInInventory;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Cleanups;
import internal.helpers.Player;
import java.util.Set;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.concoction.shop.PixelRequest;
import org.junit.jupiter.api.Test;

public class PixelRequestTest {

  private static final Set<Integer> BASE_PIXELS =
      Set.of(ItemPool.RED_PIXEL, ItemPool.GREEN_PIXEL, ItemPool.BLUE_PIXEL);

  @Test
  public void whitePixelPurchaseConsumesOtherPixels() {
    var cleanups = new Cleanups();
    // 1 of each of red / green / blue pixel
    BASE_PIXELS.stream().map(Player::withItem).forEach(cleanups::add);

    try (cleanups) {
      Concoction whitePixel = ConcoctionPool.get(ItemPool.WHITE_PIXEL);
      PixelRequest request = new PixelRequest(whitePixel);

      // Check pixel counts.
      for (int itemId : BASE_PIXELS) {
        assertThat(ItemPool.get(itemId), isInInventory());
      }

      final String url = "shop.php?whichshop=mystic&action=buyitem&whichrow=26&quantity=1";
      PixelRequest.parseResponse(url, "");

      for (int itemId : BASE_PIXELS) {
        assertThat(ItemPool.get(itemId), isInInventory(0));
      }
    }
  }
}
