package net.sourceforge.kolmafia.request.coinmaster;

import static internal.helpers.Networking.html;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MrStoreRequestTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("MrStoreRequestTest");
  }

  @Test
  void canParseMrStoreInventory() {
    MrStoreRequest.parseResponse("mrstore.php", html("request/test_visit_mr_store.html"));

    var data = MrStoreRequest.MR_STORE;
    assertThat(data.getBuyItems(), hasSize(9));

    AdventureResult mrA = data.itemBuyPrice(ItemPool.BLACK_ROSEBUD);
    assertThat(mrA.getName(), equalTo("Mr. Accessory"));
    assertThat(mrA.getCount(), equalTo(1));

    AdventureResult uncleB = data.itemBuyPrice(ItemPool.MR_ACCESSORY_JR);
    assertThat(uncleB.getName(), equalTo("Uncle Buck"));
    assertThat(uncleB.getCount(), equalTo(5));

    AdventureResult gma = data.itemBuyPrice(ItemPool.GOLDEN_MR_ACCESSORY);
    assertThat(gma.getName(), equalTo("Mr. Accessory"));
    assertThat(gma.getCount(), equalTo(5));
  }
}
