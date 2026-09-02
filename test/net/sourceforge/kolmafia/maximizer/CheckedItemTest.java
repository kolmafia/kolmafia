package net.sourceforge.kolmafia.maximizer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
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
    item.mallBuyable = 1;

    assertThat(item.acquisitionMethod(0), is(AcquisitionMethod.ACCESSIBLE));
    assertThat(item.acquisitionMethod(1), is(AcquisitionMethod.CREATE));
    assertThat(item.acquisitionMethod(2), is(AcquisitionMethod.NPC_BUY));
    assertThat(item.acquisitionMethod(3), is(AcquisitionMethod.FOLD));
    assertThat(item.acquisitionMethod(4), is(AcquisitionMethod.PULL));
    assertThat(item.acquisitionMethod(5), is(AcquisitionMethod.PULL_FOLD));
    assertThat(item.acquisitionMethod(6), is(AcquisitionMethod.STORAGE_BUY));
    assertThat(item.acquisitionMethod(7), is(AcquisitionMethod.MALL_BUY));
    assertThrows(IllegalArgumentException.class, () -> item.acquisitionMethod(8));
  }

  @Test
  void availabilityIsAnImmutableSnapshot() {
    var item = new CheckedItem(-1, EquipScope.SPECULATE_INVENTORY, 0, PriceLevel.DONT_CHECK);
    item.inventory = 1;
    item.initial = 2;
    item.creatable = 3;

    var availability = item.availability();
    item.initial = 0;
    item.creatable = 0;

    assertThat(availability.inventory(), is(1));
    assertThat(availability.total(), is(5));
    assertThat(availability.acquisitionMethod(2), is(AcquisitionMethod.CREATE));
  }

  @Test
  void exposesOnlyAvailableAcquisitionOptionsInPriorityOrder() {
    var item = new CheckedItem(-1, EquipScope.SPECULATE_INVENTORY, 0, PriceLevel.DONT_CHECK);
    item.initial = 2;
    item.creatable = 0;
    item.npcBuyable = 1;
    item.mallBuyable = 3;

    assertThat(
        item.acquisitionOptions(),
        is(
            List.of(
                new AcquisitionOption(AcquisitionMethod.ACCESSIBLE, 2),
                new AcquisitionOption(AcquisitionMethod.NPC_BUY, 1),
                new AcquisitionOption(AcquisitionMethod.MALL_BUY, 3))));
  }
}
