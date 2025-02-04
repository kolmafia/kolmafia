package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.matchers.Item.isInInventory;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Cleanups;
import internal.helpers.Player;
import java.util.Set;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.concoction.shop.PixelRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;
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
        assertThat(ItemPool.get(itemId), isInInventory(1));
      }

      String url = "shop.php?whichshop=mystic&action=buyitem&whichrow=26&quantity=1";
      String responseText = html("request/test_white_pixel_1.html");
      ShopRequest.parseResponse(url, responseText);

      for (int itemId : BASE_PIXELS) {
        assertThat(ItemPool.get(itemId), isInInventory(0));
      }
    }
  }

  @Test
  public void whitePixelPurchaseChecksQuantity() {
    var cleanups = new Cleanups();
    // 1 of each of red / green / blue pixel
    BASE_PIXELS.stream().map(Player::withItem).forEach(cleanups::add);

    try (cleanups) {
      Concoction whitePixel = ConcoctionPool.get(ItemPool.WHITE_PIXEL);
      PixelRequest request = new PixelRequest(whitePixel);

      // Check pixel counts.
      for (int itemId : BASE_PIXELS) {
        assertThat(ItemPool.get(itemId), isInInventory(1));
      }

      String url = "shop.php?whichshop=mystic&action=buyitem&whichrow=26&quantity=2";
      String responseText = html("request/test_white_pixel_2.html");
      ShopRequest.parseResponse(url, responseText);

      for (int itemId : BASE_PIXELS) {
        assertThat(ItemPool.get(itemId), isInInventory(1));
      }
    }
  }

  @Test
  public void askingForZeroMakesOne() {
    var cleanups = new Cleanups();
    // 1 of each of red / green / blue pixel
    BASE_PIXELS.stream().map(Player::withItem).forEach(cleanups::add);

    try (cleanups) {
      Concoction whitePixel = ConcoctionPool.get(ItemPool.WHITE_PIXEL);
      PixelRequest request = new PixelRequest(whitePixel);

      // Check pixel counts.
      for (int itemId : BASE_PIXELS) {
        assertThat(ItemPool.get(itemId), isInInventory(1));
      }

      String url = "shop.php?whichshop=mystic&action=buyitem&whichrow=26&quantity=0";
      String responseText = html("request/test_white_pixel_0.html");
      ShopRequest.parseResponse(url, responseText);

      for (int itemId : BASE_PIXELS) {
        assertThat(ItemPool.get(itemId), isInInventory(0));
      }
    }
  }
}
