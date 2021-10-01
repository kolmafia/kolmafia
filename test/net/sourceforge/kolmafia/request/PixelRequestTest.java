package net.sourceforge.kolmafia.request;

import static org.junit.Assert.*;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

public class PixelRequestTest {
  @After
  public void after() {
    KoLCharacter.reset(false);
  }

  @Test
  public void whitePixelPurchaseConsumesOtherPixels() {
    // 1 of each of red / green / blue pixel
    loadInventory("{\"461\": \"1\", \"462\": \"1\", \"463\": \"1\"}");
    Concoction whitePixel = ConcoctionPool.get(459);
    PixelRequest request = new PixelRequest(whitePixel);

    int RED_PIXEL = 461;
    int GREEN_PIXEL = 462;
    int BLUE_PIXEL = 463;

    int[] PIXELS = {RED_PIXEL, GREEN_PIXEL, BLUE_PIXEL};

    // Check pixel counts.
    for (int itemId : PIXELS) {
      assertEquals("item " + itemId + " (before): ", 1, InventoryManager.getCount(itemId));
    }

    final String url = "shop.php?whichshop=mystic&action=buyitem&whichrow=26&quantity=1";
    request.parseResponse(url, "");

    for (int itemId : PIXELS) {
      assertEquals("item " + itemId + " (after): ", 0, InventoryManager.getCount(itemId));
    }
  }

  private void loadInventory(String jsonInventory) {
    try {
      InventoryManager.parseInventory(new JSONObject(jsonInventory));
    } catch (JSONException e) {
      fail("Inventory parsing failed.");
    }
  }
}
