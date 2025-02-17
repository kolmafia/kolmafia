package net.sourceforge.kolmafia;

import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withSkill;
import static internal.helpers.Player.withoutSkill;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.Crimbo24CafeRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.DinseyCompanyStoreRequest;
import net.sourceforge.kolmafia.request.coinmaster.shop.GeneticFiddlingRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CoinmasterDataTest {
  @Nested
  class InZone {
    @Test
    void withoutZoneAccessibleByDefault() {
      var data = new CoinmasterData("test", "test", CoinMasterRequest.class);
      assertThat(data.isAccessible(), is(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Crimbo24", "Removed"})
    void notAccessibleIfParentZoneIsRemoved(final String zone) {
      var data = new CoinmasterData("test", "test", CoinMasterRequest.class).inZone(zone);
      assertThat(data.isAccessible(), is(false));
    }
  }

  @Nested
  class AvailableSkill {
    @Test
    void withUnknownSkill() {
      var data = GeneticFiddlingRequest.DATA;
      var cleanups =
          new Cleanups(withPath(Path.NUCLEAR_AUTUMN), withoutSkill(SkillPool.EXTRA_MUSCLES));
      try (cleanups) {
        assertThat(data.availableSkill(SkillPool.EXTRA_MUSCLES), is(true));
      }
    }

    @Test
    void withKnownSkill() {
      var data = GeneticFiddlingRequest.DATA;
      var cleanups =
          new Cleanups(withPath(Path.NUCLEAR_AUTUMN), withSkill(SkillPool.EXTRA_MUSCLES));
      try (cleanups) {
        assertThat(data.availableSkill(SkillPool.EXTRA_MUSCLES), is(false));
      }
    }
  }

  @Nested
  class ItemBuyPrice {
    @Test
    void withKnownItem() {
      var data = DinseyCompanyStoreRequest.DINSEY_COMPANY_STORE;
      var price = data.itemBuyPrice(ItemPool.DINSEY_TICKET);
      assertNotNull(price);
      assertThat(price.getCount(), is(20));
    }

    @Test
    void withNonShopRowCoinmasterUnsoldItem() {
      var data = DinseyCompanyStoreRequest.DINSEY_COMPANY_STORE;
      var price = data.itemBuyPrice(ItemPool.SEAL_TOOTH);
      assertNotNull(price);
      assertThat(price.getCount(), is(0));
    }

    @Test
    void withShopRowCoinmasterUnsoldItem() {
      var data = Crimbo24CafeRequest.DATA;
      var price = data.itemBuyPrice(ItemPool.DINSEY_TICKET);
      // *** ShopRow coinmasters can have multiple currencies
      // *** For an unknown item, what currency?
      assertNull(price);
    }
  }
}
