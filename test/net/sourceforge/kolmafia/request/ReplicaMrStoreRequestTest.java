package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junitpioneer.jupiter.cartesian.CartesianTest.Values;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.cartesian.CartesianTest;

class ReplicaMrStoreRequestTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("ReplicaMrStoreRequestTest");
    Preferences.reset("ReplicaMrStoreRequestTest");
  }

  @Test
  void canParseReplicaYearWhenVisitStore() {
    var cleanups = new Cleanups(withProperty("currentReplicaStoreYear", 2004));
    try (cleanups) {
      String responseText = html("request/test_visit_replica_mr_store.html");
      ReplicaMrStoreRequest.parseResponse("shop.php?whichshop=mrreplica", responseText);
      assertThat("currentReplicaStoreYear", isSetTo(2007));
    }
  }

  @Test
  void canFilterItemsByYear() {
    var cleanups =
        new Cleanups(
            withProperty("currentReplicaStoreYear", 2016),
            withItem(ItemPool.REPLICA_MR_ACCESSORY, 1));
    try (cleanups) {
      CoinmasterData data = ReplicaMrStoreRequest.REPLICA_MR_STORE;
      // All items from 2016 are available
      assertThat(data.canBuyItem(ItemPool.REPLICA_WITCHESS_SET), is(true));
      assertThat(data.canBuyItem(ItemPool.REPLICA_INTERGNAT), is(true));
      assertThat(data.canBuyItem(ItemPool.REPLICA_SOURCE_TERMINAL), is(true));
      // Items from 2023 are available
      assertThat(data.canBuyItem(ItemPool.REPLICA_CINCHO_DE_MAYO), is(true));
      assertThat(data.canBuyItem(ItemPool.REPLICA_MR_STORE_2002_CATALOG), is(true));
      // Items from other years are not available
      /* 2004 */ assertThat(data.canBuyItem(ItemPool.REPLICA_DARK_JILL), is(false));
      /* 2005 */ assertThat(data.canBuyItem(ItemPool.REPLICA_WAX_LIPS), is(false));
      /* 2006 */ assertThat(data.canBuyItem(ItemPool.REPLICA_SNOWCONE_BOOK), is(false));
      /* 2007 */ assertThat(data.canBuyItem(ItemPool.REPLICA_NAVEL_RING), is(false));
      /* 2008 */ assertThat(data.canBuyItem(ItemPool.REPLICA_HAIKU_KATANA), is(false));
      /* 2009 */ assertThat(data.canBuyItem(ItemPool.REPLICA_BANDERSNATCH), is(false));
      /* 2010 */ assertThat(data.canBuyItem(ItemPool.REPLICA_GREAT_PANTS), is(false));
      /* 2011 */ assertThat(data.canBuyItem(ItemPool.REPLICA_CUTE_ANGEL), is(false));
      /* 2012 */ assertThat(data.canBuyItem(ItemPool.REPLICA_RESOLUTION_BOOK), is(false));
      /* 2013 */ assertThat(data.canBuyItem(ItemPool.REPLICA_SMITH_BOOK), is(false));
      /* 2014 */ assertThat(data.canBuyItem(ItemPool.REPLICA_GENE_SPLICING_LAB), is(false));
      /* 2015 */ assertThat(data.canBuyItem(ItemPool.REPLICA_DECK_OF_EVERY_CARD), is(false));
      /* 2017 */ assertThat(data.canBuyItem(ItemPool.REPLICA_GENIE_BOTTLE), is(false));
      /* 2018 */ assertThat(data.canBuyItem(ItemPool.REPLICA_GARBAGE_TOTE), is(false));
      /* 2019 */ assertThat(data.canBuyItem(ItemPool.REPLICA_FOURTH_SABER), is(false));
      /* 2020 */ assertThat(data.canBuyItem(ItemPool.REPLICA_CAMELCALF), is(false));
      /* 2021 */ assertThat(data.canBuyItem(ItemPool.REPLICA_EMOTION_CHIP), is(false));
      /* 2022 */ assertThat(data.canBuyItem(ItemPool.REPLICA_JURASSIC_PARKA), is(false));
    }
  }

  @CartesianTest
  public void canOnlyBuyCurrentYearItemOnce(
      @Values(ints = {ItemPool.REPLICA_CINCHO_DE_MAYO, ItemPool.REPLICA_MR_STORE_2002_CATALOG})
          final int itemId,
      @Values(booleans = {false, true}) final boolean haveItem) {
    var cleanups =
        new Cleanups(
            withProperty("currentReplicaStoreYear", 2016), withItem(itemId, haveItem ? 1 : 0));
    try (cleanups) {
      CoinmasterData data = ReplicaMrStoreRequest.REPLICA_MR_STORE;
      // Items from 2023 are available only if you do not already have one
      assertThat(data.canBuyItem(itemId), is(!haveItem));
    }
  }
}
