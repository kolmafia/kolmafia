package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withOverrideModifiers;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import internal.helpers.Cleanups;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class BooleanRequirementsTest {
  @BeforeEach
  void reset() {
    KoLCharacter.reset("BooleanRequirementsTest");
    KoLCharacter.reset(true);
    Preferences.reset("BooleanRequirementsTest");
    KoLmafia.forceContinue();
    Maximizer.evaluationsChecked = 0;
    Maximizer.combinationLimit = 0;
  }

  @Nested
  class RemainingSources {
    @ParameterizedTest
    @CsvSource({"false,true", "true,true", "false,false", "true,false"})
    void outfitSourcesRequireAllPiecesToBeAvailable(boolean fixedHat, boolean weaponAvailable)
        throws MaximizerInterruptedException {
      try (var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.ROBORTENDER),
              withOverrideModifiers(
                  ModifierType.OUTFIT, "Radio Free Regalia", "Underwater Familiar"))) {
        var spec = new MaximizerSpeculation();
        var candidates = new SlotList<CheckedItem>(0);
        for (var slot : List.of(Slot.FAMILIAR, Slot.HAT, Slot.PANTS, Slot.WEAPON)) {
          spec.equipment.put(slot, null);
        }
        candidates.get(Slot.FAMILIAR).add(candidate("das boot"));
        candidates.get(Slot.FAMILIAR).add(candidate("amulet coin"));
        if (fixedHat) spec.equipment.put(Slot.HAT, ItemPool.get("Radio Free Baseball Cap"));
        else candidates.get(Slot.HAT).add(candidate("Radio Free Baseball Cap"));
        candidates.get(Slot.PANTS).add(candidate("Radio Free Pants"));
        if (weaponAvailable) candidates.get(Slot.WEAPON).add(candidate("Radio Free Foil"));
        var requirements =
            new BooleanRequirements(
                EnumSet.of(BooleanModifier.UNDERWATER_FAMILIAR),
                spec,
                candidates,
                EquipmentRequest.UNEQUIP,
                List.of(),
                null,
                null);

        spec.equipment.put(Slot.FAMILIAR, ItemPool.get("amulet coin"));
        assertThat(requirements.possible(spec, Slot.FAMILIAR), is(weaponAvailable));
        if (!weaponAvailable) return;
        spec.equipment.put(Slot.HAT, ItemPool.get("Radio Free Baseball Cap"));
        assertThat(requirements.possible(spec, Slot.HAT), is(true));
        spec.equipment.put(Slot.PANTS, ItemPool.get("Radio Free Pants"));
        spec.equipment.put(Slot.WEAPON, EquipmentRequest.UNEQUIP);
        assertThat(requirements.possible(spec, Slot.WEAPON), is(false));
        spec.equipment.put(Slot.WEAPON, ItemPool.get("Radio Free Foil"));
        assertThat(requirements.possible(spec, Slot.WEAPON), is(true));
      }
    }

    @ParameterizedTest
    @CsvSource({"false,lupine sword", "true,disco ball"})
    void includesSpecializedOffhandCandidates(boolean ranged, String weapon)
        throws MaximizerInterruptedException {
      try (var cleanups =
          new Cleanups(
              withOverrideModifiers(ModifierType.ITEM, "helmet turtle", "Never Fumble"),
              withOverrideModifiers(ModifierType.ITEM, weapon, "Never Fumble"))) {
        var spec = new MaximizerSpeculation();
        spec.equipment.put(Slot.HAT, null);
        spec.equipment.put(Slot.OFFHAND, null);
        var candidates = new SlotList<CheckedItem>(0);
        candidates.get(Slot.HAT).add(candidate("helmet turtle"));
        candidates
            .get(ranged ? Evaluator.OFFHAND_RANGED : Evaluator.OFFHAND_MELEE)
            .add(candidate(weapon));
        var requirements = requirements(spec, candidates, BooleanModifier.NEVER_FUMBLE);

        spec.equipment.put(Slot.HAT, EquipmentRequest.UNEQUIP);
        assertThat(requirements.possible(spec, Slot.HAT), is(true));
        spec.equipment.put(Slot.OFFHAND, EquipmentRequest.UNEQUIP);
        assertThat(requirements.possible(spec, Slot.OFFHAND), is(false));
        spec.equipment.put(Slot.OFFHAND, ItemPool.get(weapon));
        assertThat(requirements.possible(spec, Slot.OFFHAND), is(true));
      }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void usesSelectedCardInsteadOfCurrentCard(boolean selectedSupplies)
        throws MaximizerInterruptedException {
      var currentCard = ItemPool.get("Alice's Army Swordsman");
      var selectedCard = ItemPool.get("Alice's Army Sniper");
      try (var cleanups =
          new Cleanups(
              withOverrideModifiers(ModifierType.ITEM, "helmet turtle", "Never Fumble"),
              withOverrideModifiers(
                  ModifierType.ITEM,
                  currentCard.getItemId(),
                  selectedSupplies ? "" : "Never Fumble"),
              withOverrideModifiers(
                  ModifierType.ITEM,
                  selectedCard.getItemId(),
                  selectedSupplies ? "Never Fumble" : ""))) {
        var spec = new MaximizerSpeculation();
        spec.equipment.put(Slot.HAT, null);
        spec.equipment.put(Slot.OFFHAND, null);
        spec.equipment.put(Slot.CARDSLEEVE, currentCard);
        var candidates = new SlotList<CheckedItem>(0);
        candidates.get(Slot.HAT).add(candidate("helmet turtle"));
        candidates.get(Slot.OFFHAND).add(candidate("card sleeve"));
        var requirements =
            new BooleanRequirements(
                EnumSet.of(BooleanModifier.NEVER_FUMBLE),
                spec,
                candidates,
                selectedCard,
                List.of(),
                null,
                null);

        spec.equipment.put(Slot.HAT, EquipmentRequest.UNEQUIP);
        assertThat(requirements.possible(spec, Slot.HAT), is(selectedSupplies));
        assertThat(spec.equipment.get(Slot.CARDSLEEVE), is(currentCard));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "Crown of Thrones,true",
      "Crown of Thrones,false",
      "Buddy Bjorn,true",
      "Buddy Bjorn,false"
    })
    void explicitCarriedFamiliarTakesPrecedenceOverOtherAvailableFamiliars(
        String carrier, boolean chosenSupplies) throws MaximizerInterruptedException {
      try (var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.ROBORTENDER),
              withOverrideModifiers(ModifierType.THRONE, "Rock Lobster", "Underwater Familiar"),
              withOverrideModifiers(ModifierType.THRONE, "Baby Gravy Fairy", ""))) {
        boolean crown = carrier.equals("Crown of Thrones");
        var slot = crown ? Slot.HAT : Slot.CONTAINER;
        var supplier = new FamiliarData(FamiliarPool.LOBSTER);
        var nonsupplier = new FamiliarData(FamiliarPool.BABY_GRAVY_FAIRY);
        var chosen = chosenSupplies ? supplier : nonsupplier;
        var other = chosenSupplies ? nonsupplier : supplier;
        var spec = new MaximizerSpeculation();
        if (crown) spec.setEnthroned(nonsupplier);
        else spec.setBjorned(nonsupplier);
        spec.equipment.put(Slot.FAMILIAR, null);
        spec.equipment.put(slot, null);
        var candidates = new SlotList<CheckedItem>(0);
        candidates.get(Slot.FAMILIAR).add(candidate("das boot"));
        candidates.get(slot).add(candidate(carrier));
        var requirements =
            new BooleanRequirements(
                EnumSet.of(BooleanModifier.UNDERWATER_FAMILIAR),
                spec,
                candidates,
                EquipmentRequest.UNEQUIP,
                List.of(other),
                crown ? chosen : null,
                crown ? null : chosen);

        spec.equipment.put(Slot.FAMILIAR, ItemPool.get("amulet coin"));
        assertThat(requirements.possible(spec, Slot.FAMILIAR), is(chosenSupplies));
        assertThat(crown ? spec.getEnthroned() : spec.getBjorned(), is(nonsupplier));
      }
    }

    @Test
    void conflictingSynergyPiecesDeferRejectionUntilTheirSlotIsAssigned()
        throws MaximizerInterruptedException {
      try (var cleanups =
          new Cleanups(
              withOverrideModifiers(ModifierType.ITEM, "helmet turtle", "Never Fumble"),
              withOverrideModifiers(
                  ModifierType.SYNERGY, "stuffed carpenter/stuffed walrus", "Never Fumble"))) {
        var spec = new MaximizerSpeculation();
        spec.equipment.put(Slot.HAT, null);
        spec.equipment.put(Slot.OFFHAND, null);
        var candidates = new SlotList<CheckedItem>(0);
        candidates.get(Slot.HAT).add(candidate("helmet turtle"));
        candidates.get(Slot.OFFHAND).add(candidate("stuffed carpenter"));
        candidates.get(Slot.OFFHAND).add(candidate("stuffed walrus"));
        var requirements = requirements(spec, candidates, BooleanModifier.NEVER_FUMBLE);

        spec.equipment.put(Slot.HAT, EquipmentRequest.UNEQUIP);
        assertThat(requirements.possible(spec, Slot.HAT), is(true));
        for (var piece : List.of("stuffed carpenter", "stuffed walrus")) {
          spec.equipment.put(Slot.OFFHAND, ItemPool.get(piece));
          assertThat(requirements.possible(spec, Slot.OFFHAND), is(false));
        }
        spec.equipment.put(Slot.HAT, ItemPool.get("helmet turtle"));
        assertThat(requirements.possible(spec, Slot.HAT), is(true));
      }
    }

    @ParameterizedTest
    @CsvSource({"HAT,helmet turtle", "PANTS,old sweatpants"})
    void wearableFamiliarEquipmentDoesNotSupplyItsOrdinaryBooleans(Slot slot, String item)
        throws MaximizerInterruptedException {
      try (var cleanups =
          new Cleanups(
              withFamiliar(slot == Slot.HAT ? FamiliarPool.HATRACK : FamiliarPool.SCARECROW),
              withOverrideModifiers(ModifierType.ITEM, item, "Never Fumble"))) {
        var spec = new MaximizerSpeculation();
        spec.equipment.put(slot, null);
        spec.equipment.put(Slot.FAMILIAR, null);
        var candidates = new SlotList<CheckedItem>(0);
        candidates.get(slot).add(candidate(item));
        candidates.get(Slot.FAMILIAR).add(candidate(item));
        var requirements = requirements(spec, candidates, BooleanModifier.NEVER_FUMBLE);

        spec.equipment.put(slot, EquipmentRequest.UNEQUIP);
        assertThat(requirements.possible(spec, slot), is(false));
        spec.equipment.put(slot, ItemPool.get(item));
        assertThat(requirements.possible(spec, slot), is(true));
      }
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void interruptionDuringSourceDiscoveryStopsBeforeTheNextCalculation(int abortAfter) {
    try (var cleanups =
        new Cleanups(withOverrideModifiers(ModifierType.ITEM, "helmet turtle", "Never Fumble"))) {
      var calculations = new AtomicInteger();
      var spec =
          new MaximizerSpeculation() {
            @Override
            public Modifiers calculate() {
              var result = super.calculate();
              if (calculations.incrementAndGet() == abortAfter) {
                KoLmafia.updateDisplay(MafiaState.ABORT, "Interrupted source discovery");
              }
              return result;
            }
          };
      spec.equipment.put(Slot.HAT, null);
      var candidates = new SlotList<CheckedItem>(0);
      candidates.get(Slot.HAT).add(candidate("helmet turtle"));
      candidates.get(Slot.HAT).add(candidate("crumpled felt fedora"));

      assertThrows(
          MaximizerInterruptedException.class,
          () -> requirements(spec, candidates, BooleanModifier.NEVER_FUMBLE));
      assertThat(calculations.get(), is(abortAfter));
      assertThat(Maximizer.evaluationsChecked, is((long) abortAfter));
    } finally {
      KoLmafia.forceContinue();
    }
  }

  private static BooleanRequirements requirements(
      MaximizerSpeculation spec, SlotList<CheckedItem> candidates, BooleanModifier required)
      throws MaximizerInterruptedException {
    return new BooleanRequirements(
        EnumSet.of(required), spec, candidates, EquipmentRequest.UNEQUIP, List.of(), null, null);
  }

  private static CheckedItem candidate(String name) {
    return new CheckedItem(
        ItemPool.get(name).getItemId(), EquipScope.SPECULATE_INVENTORY, 0, PriceLevel.DONT_CHECK);
  }
}
