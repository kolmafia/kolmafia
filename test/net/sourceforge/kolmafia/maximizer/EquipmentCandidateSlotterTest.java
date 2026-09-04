package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EquipmentCandidateSlotterTest {
  @BeforeEach
  void beforeEach() {
    KoLCharacter.reset("EquipmentCandidateSlotterTest");
    Preferences.reset("EquipmentCandidateSlotterTest");
  }

  private static EquipmentCandidateSlotter.Requirements requirements(
      int hands, int melee, String type, boolean shield, boolean club, boolean effective) {
    return new EquipmentCandidateSlotter.Requirements(
        hands, melee, type, shield, club, false, false, false, false, effective);
  }

  private static EquipmentCandidateSlotter slotter(
      EquipmentCandidateSlotter.Requirements requirements) {
    return new EquipmentCandidateSlotter(
        requirements, false, false, false, 0, PriceLevel.DONT_CHECK);
  }

  private static CheckedItem item(String name) {
    return new CheckedItem(
        ItemPool.get(name).getItemId(), EquipScope.SPECULATE_INVENTORY, 0, PriceLevel.DONT_CHECK);
  }

  @Test
  void separatesOneHandedWeaponsByWeaponType() throws MaximizerInterruptedException {
    var slotter = slotter(requirements(0, 0, null, false, false, false));

    var melee =
        slotter.place(
            ItemPool.SEAL_CLUB,
            "seal-clubbing club",
            Slot.WEAPON,
            item("seal-clubbing club"),
            false);
    var ranged =
        slotter.place(
            ItemPool.get("airblaster gun").getItemId(),
            "airblaster gun",
            Slot.WEAPON,
            item("airblaster gun"),
            false);

    assertThat(melee.accepted(), is(true));
    assertThat(melee.slot(), is(Evaluator.WEAPON_1H));
    assertThat(melee.auxiliarySlot(), is(Evaluator.OFFHAND_MELEE));
    assertThat(ranged.slot(), is(Evaluator.WEAPON_1H));
    assertThat(ranged.auxiliarySlot(), is(Evaluator.OFFHAND_RANGED));
  }

  @Test
  void enforcesHandednessRangeAndTypeRequirements() throws MaximizerInterruptedException {
    var sealClub = item("seal-clubbing club");
    int rangedId = ItemPool.get("airblaster gun").getItemId();
    var ranged = item("airblaster gun");

    assertThat(
        slotter(requirements(2, 0, null, false, false, false))
            .place(ItemPool.SEAL_CLUB, sealClub.getName(), Slot.WEAPON, sealClub, false)
            .accepted(),
        is(false));
    assertThat(
        slotter(requirements(0, 1, null, false, false, false))
            .place(rangedId, ranged.getName(), Slot.WEAPON, ranged, false)
            .accepted(),
        is(false));
    assertThat(
        slotter(requirements(0, -1, null, false, false, false))
            .place(ItemPool.SEAL_CLUB, sealClub.getName(), Slot.WEAPON, sealClub, false)
            .accepted(),
        is(false));
    assertThat(
        slotter(requirements(0, 0, "sword", false, false, false))
            .place(ItemPool.SEAL_CLUB, sealClub.getName(), Slot.WEAPON, sealClub, false)
            .accepted(),
        is(false));
  }

  @Test
  void keepsWrongWeaponClassesOnlyInTheirAuxiliaryBuckets() throws MaximizerInterruptedException {
    var ranged = item("airblaster gun");
    var placement =
        slotter(requirements(0, 0, null, false, true, false))
            .place(ranged.getItemId(), ranged.getName(), Slot.WEAPON, ranged, false);

    assertThat(placement.accepted(), is(true));
    assertThat(placement.slot(), is(Evaluator.OFFHAND_RANGED));
    assertThat(placement.auxiliarySlot(), is(Evaluator.OFFHAND_RANGED));
  }

  @Test
  void rejectsNonShieldsAndIneligibleFamiliarEquipment() throws MaximizerInterruptedException {
    var slotter = slotter(requirements(0, 0, null, true, false, false));
    var offhand = item("silver cow creamer");

    assertThat(
        slotter
            .place(offhand.getItemId(), offhand.getName(), Slot.OFFHAND, offhand, false)
            .accepted(),
        is(false));
    assertThat(
        slotter
            .place(offhand.getItemId(), offhand.getName(), Slot.FAMILIAR, offhand, false)
            .accepted(),
        is(false));
    assertThat(
        slotter
            .place(offhand.getItemId(), offhand.getName(), Slot.FAMILIAR, offhand, true)
            .accepted(),
        is(true));
  }

  @Test
  void preservesChargedGarbageShirtsWhenExperienceMatters() throws MaximizerInterruptedException {
    try (var cleanups = withProperty("garbageShirtCharge", 1)) {
      assertThat(Preferences.getInteger("garbageShirtCharge"), is(1));
      var shirt =
          new CheckedItem(
              ItemPool.MAKESHIFT_GARBAGE_SHIRT,
              EquipScope.SPECULATE_INVENTORY,
              0,
              PriceLevel.DONT_CHECK);
      var slotter =
          new EquipmentCandidateSlotter(
              requirements(0, 0, null, false, false, false),
              false,
              false,
              true,
              0,
              PriceLevel.DONT_CHECK);

      var placement =
          slotter.place(
              ItemPool.MAKESHIFT_GARBAGE_SHIRT, shirt.getName(), Slot.SHIRT, shirt, false);

      assertThat(placement.skipScoring(), is(true));
      assertThat(shirt.requiredFlag, is(true));
      assertThat(shirt.automaticFlag, is(true));
    }
  }
}
