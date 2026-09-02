package net.sourceforge.kolmafia.maximizer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class CheckedItemTest {
  @Test
  void classifiesHowTheNextCopyCanBeAcquired() {
    var availability = new ItemAvailability(0, 1, 1, 1, 1, 1, 1, 1, 1, 0);

    assertThat(availability.acquisitionMethod(0), is(AcquisitionMethod.ACCESSIBLE));
    assertThat(availability.acquisitionMethod(1), is(AcquisitionMethod.CREATE));
    assertThat(availability.acquisitionMethod(2), is(AcquisitionMethod.NPC_BUY));
    assertThat(availability.acquisitionMethod(3), is(AcquisitionMethod.FOLD));
    assertThat(availability.acquisitionMethod(4), is(AcquisitionMethod.PULL));
    assertThat(availability.acquisitionMethod(5), is(AcquisitionMethod.PULL_FOLD));
    assertThat(availability.acquisitionMethod(6), is(AcquisitionMethod.STORAGE_BUY));
    assertThat(availability.acquisitionMethod(7), is(AcquisitionMethod.MALL_BUY));
    assertThrows(IllegalArgumentException.class, () -> availability.acquisitionMethod(8));
  }

  @Test
  void checkedItemRetainsItsCompiledAvailability() {
    var item = new CheckedItem(-1, EquipScope.SPECULATE_INVENTORY, 0, PriceLevel.DONT_CHECK);

    var availability = item.availability();

    assertThat(item.availability(), is(availability));
    assertThat(availability.inventory(), is(Integer.MAX_VALUE));
    assertThat(availability.total(), is(Integer.MAX_VALUE));
  }

  @Test
  void exposesOnlyAvailableAcquisitionOptionsInPriorityOrder() {
    var availability = new ItemAvailability(0, 2, 0, 1, 3, 0, 0, 0, 0, 0);

    assertThat(
        availability.options(),
        is(
            List.of(
                new AcquisitionOption(AcquisitionMethod.ACCESSIBLE, 2),
                new AcquisitionOption(AcquisitionMethod.NPC_BUY, 1),
                new AcquisitionOption(AcquisitionMethod.MALL_BUY, 3))));
  }
}
