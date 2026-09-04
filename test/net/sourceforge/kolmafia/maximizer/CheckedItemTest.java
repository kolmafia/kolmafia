package net.sourceforge.kolmafia.maximizer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
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
    assertThrows(IllegalArgumentException.class, () -> availability.acquisitionMethod(-1));
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

  @Test
  void findsTheFirstAcquisitionMethodSupportedByTheConsumer() {
    var availability = new ItemAvailability(0, 0, 0, 0, 0, 1, 1, 1, 0, 0);

    assertThat(
        availability.firstMethod(
            method -> method != AcquisitionMethod.FOLD && method != AcquisitionMethod.PULL_FOLD),
        is(AcquisitionMethod.PULL));
  }

  @Test
  void validatesMallAndStorageBuyingPowerIndependently() {
    var availability = new ItemAvailability(0, 0, 0, 0, 2, 0, 0, 0, 3, 0);

    var mallOnly = availability.withValidatedMallPrice(100, 200, 200, 50);
    var storageOnly = availability.withValidatedMallPrice(100, 200, 50, 200);
    var tooExpensive = availability.withValidatedMallPrice(201, 200, 500, 500);
    var unknownPrice = availability.withValidatedMallPrice(0, 200, 500, 500);

    assertThat(mallOnly.mallBuyable(), is(2));
    assertThat(mallOnly.storageBuyable(), is(0));
    assertThat(storageOnly.mallBuyable(), is(0));
    assertThat(storageOnly.storageBuyable(), is(3));
    assertThat(tooExpensive.buyable(), is(false));
    assertThat(unknownPrice.buyable(), is(false));
    assertThat(availability.withValidatedMallPrice(100, 200, 200, 200), sameInstance(availability));
  }

  @Test
  void mapsLegacyScopeAndPriceIndexes() {
    assertThat(EquipScope.byIndex(-1), is(EquipScope.EQUIP_NOW));
    assertThat(EquipScope.byIndex(0), is(EquipScope.SPECULATE_INVENTORY));
    assertThat(EquipScope.byIndex(1), is(EquipScope.SPECULATE_CREATABLE));
    assertThat(EquipScope.byIndex(2), is(EquipScope.SPECULATE_ANY));
    assertThat(EquipScope.byIndex(99), is(EquipScope.SPECULATE_ANY));
    assertThat(EquipScope.EQUIP_NOW.checkInventoryOnly(), is(true));
    assertThat(EquipScope.SPECULATE_INVENTORY.checkInventoryOnly(), is(true));
    assertThat(EquipScope.SPECULATE_CREATABLE.checkInventoryOnly(), is(false));
    assertThat(EquipScope.SPECULATE_ANY.checkInventoryOnly(), is(false));

    assertThat(PriceLevel.byIndex(0), is(PriceLevel.DONT_CHECK));
    assertThat(PriceLevel.byIndex(-1), is(PriceLevel.DONT_CHECK));
    assertThat(PriceLevel.byIndex(1), is(PriceLevel.BUYABLE_ONLY));
    assertThat(PriceLevel.byIndex(2), is(PriceLevel.ALL));
  }
}
