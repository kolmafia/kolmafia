package net.sourceforge.kolmafia.maximizer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class CheckedItemTest {
  @Test
  void classifiesHowTheNextCopyCanBeAcquired() {
    var item = new CheckedItem(-1, EquipScope.SPECULATE_INVENTORY, 0, PriceLevel.DONT_CHECK);
    item.inventory = 0;
    item.initial = 1;
    item.creatable = 1;
    item.npcBuyable = 1;
    item.foldable = 1;
    item.pullable = 1;
    item.pullfoldable = 1;
    item.pullBuyable = 1;

    assertThat(item.acquisitionMethod(0), is(AcquisitionMethod.ACCESSIBLE));
    assertThat(item.acquisitionMethod(1), is(AcquisitionMethod.CREATE));

    item.creatable = 0;
    assertThat(item.acquisitionMethod(1), is(AcquisitionMethod.NPC_BUY));
    item.npcBuyable = 0;
    assertThat(item.acquisitionMethod(1), is(AcquisitionMethod.FOLD));
    item.foldable = 0;
    assertThat(item.acquisitionMethod(1), is(AcquisitionMethod.PULL));
    item.pullable = 0;
    assertThat(item.acquisitionMethod(1), is(AcquisitionMethod.PULL_FOLD));
    item.pullfoldable = 0;
    assertThat(item.acquisitionMethod(1), is(AcquisitionMethod.STORAGE_BUY));
    item.pullBuyable = 0;
    assertThat(item.acquisitionMethod(1), is(AcquisitionMethod.MALL_BUY));
  }
}
