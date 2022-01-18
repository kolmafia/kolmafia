package net.sourceforge.kolmafia.request;

import static internal.helpers.Player.addItem;
import static internal.helpers.Player.countItem;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.Test;

public class PixelRequestTest {
  @Test
  public void whitePixelPurchaseConsumesOtherPixels() {
    Set<Integer> BASE_PIXELS =
        Set.of(ItemPool.RED_PIXEL, ItemPool.GREEN_PIXEL, ItemPool.BLUE_PIXEL);

    // 1 of each of red / green / blue pixel
    BASE_PIXELS.forEach(itemId -> addItem(itemId));

    Concoction whitePixel = ConcoctionPool.get(ItemPool.WHITE_PIXEL);
    PixelRequest request = new PixelRequest(whitePixel);

    // Check pixel counts.
    for (int itemId : BASE_PIXELS) {
      assertEquals(1, countItem(itemId), "item " + itemId + " (before): ");
    }

    final String url = "shop.php?whichshop=mystic&action=buyitem&whichrow=26&quantity=1";
    request.parseResponse(url, "");

    for (int itemId : BASE_PIXELS) {
      assertEquals(0, countItem(itemId), "item " + itemId + " (after): ");
    }
  }
}
