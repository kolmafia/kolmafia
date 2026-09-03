package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.maximize;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withOverrideModifiers;
import static internal.helpers.Player.withPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EquipmentSearchProblemTest {
  private static final int TURTLE = ItemPool.HELMET_TURTLE;
  private static final int SLIME_HAT = ItemPool.get("hardened slime hat", 1).getItemId();
  private static final int DESIGNER_PANTS = ItemPool.DESIGNER_SWEATPANTS;
  private static final int OLD_PANTS = ItemPool.get("old sweatpants", 1).getItemId();

  @BeforeEach
  void beforeEach() {
    KoLCharacter.reset("EquipmentSearchProblemTest");
  }

  @Test
  void matchesBruteForceOnReducedRealEquipment() {
    try (var cleanups =
        new Cleanups(
            withEquippableItem(TURTLE),
            withEquippableItem(SLIME_HAT),
            withEquippableItem(DESIGNER_PANTS),
            withEquippableItem(OLD_PANTS),
            withOverrideModifiers(ModifierType.ITEM, TURTLE, "Item Drop: +5"),
            withOverrideModifiers(ModifierType.ITEM, SLIME_HAT, "Item Drop: +2"),
            withOverrideModifiers(ModifierType.ITEM, DESIGNER_PANTS, "Item Drop: +1"),
            withOverrideModifiers(ModifierType.ITEM, OLD_PANTS, "Item Drop: +4"))) {
      assertTrue(maximize("item, 1 hat, 1 pants, -tie"));
      SolutionQuality actual = Maximizer.best().quality();

      var baseline = new MaximizerSpeculation();
      var choices = List.of(items(TURTLE, SLIME_HAT), items(DESIGNER_PANTS, OLD_PANTS));
      var oracle =
          BruteForceMaximizer.maximize(
              choices,
              ignored -> true,
              equipment -> {
                var candidate = baseline.clone();
                candidate.equipment.put(Slot.HAT, equipment.get(0));
                candidate.equipment.put(Slot.PANTS, equipment.get(1));
                candidate.setUnscored();
                return candidate.quality();
              });

      assertThat(actual, is(oracle.quality()));
    }
  }

  @Test
  void skipsFullEvaluationOfAdditiveCodpiecePrefixesThatAlreadyLose() {
    int diamond = ItemPool.get("lump of diamond").getItemId();
    int pearl = ItemPool.get("unblemished pearl").getItemId();
    try (var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withItem(diamond, 5),
            withItem(pearl, 5),
            withOverrideModifiers(ModifierType.ETERNITY_CODPIECE, diamond, "PvP Fights: +10"),
            withOverrideModifiers(ModifierType.ETERNITY_CODPIECE, pearl, "PvP Fights: +1"))) {
      assertTrue(
          maximize(
              "pvp fights, -hat, -weapon, -offhand, -back, -shirt, -pants, "
                  + "-familiar, -acc1, -acc2, -acc3, -tie"));

      assertThat(Maximizer.best().getScore(), is(60.0));
      assertThat(Maximizer.bestChecked, lessThan(21));
      assertThat(Maximizer.lastSearchMetrics().boundPrunes(), greaterThan(0L));
    }
  }

  @Test
  void worstCaseBenchmarkUsesIncrementalPrimaryScore() {
    assertTrue(
        new Evaluator(
                    "tie, mus percent, mys percent, mox percent, maximum mp percent, pvp fights, "
                        + "candy drop, damage vs. seals, damage vs. zombies, pool skill, "
                        + "pickpocket chance, fishing skill, damage vs. ghosts, familiar damage, "
                        + "damage vs. werewolves, adventures, damage vs. vampires, damage vs. bugbears")
                .incrementalCodpieceScoreTerms()
            != null);
  }

  @Test
  void familiarDerivedExperienceFallsBackToFullEvaluation() {
    assertNull(new Evaluator("muscle experience").incrementalCodpieceScoreTerms());
  }

  @Test
  void suppressedAdventureModifiersFallBackToFullEvaluation() {
    try (var cleanups = withPath(Path.SLOW_AND_STEADY)) {
      assertNull(new Evaluator("adventures").incrementalCodpieceScoreTerms());
    }
  }

  @Test
  void incrementalScoreModifiersUseTheirRawValues() {
    var modifiers = new Modifiers();
    for (DoubleModifier modifier : DoubleModifier.DOUBLE_MODIFIERS) {
      modifiers.setDouble(modifier, 2.0);
    }

    for (DoubleModifier modifier : DoubleModifier.DOUBLE_MODIFIERS) {
      if (CodpieceModifierSafety.supportsIncrementalScore(modifier)) {
        assertThat(modifier.toString(), Evaluator.scoreValue(modifier, modifiers, null), is(2.0));
      }
    }
  }

  @Test
  void slottedItemsOwnTypedSpeculativeState() {
    var state = new MaximizerSpeculation();
    var card = ItemPool.get("Alice's Army Sniper");
    var familiar = new FamiliarData(FamiliarPool.MOSQUITO);

    assertTrue(ItemSlotGroup.ETERNITY_CODPIECE.accepts(ItemPool.HEARTSTONE));
    assertTrue(ItemSlotGroup.CARD_SLEEVE.put(state, Slot.CARDSLEEVE, card));
    assertFalse(ItemSlotGroup.CARD_SLEEVE.put(state, Slot.CODPIECE1, card));
    assertTrue(FamiliarSlotGroup.CROWN.put(state, Slot.CROWNOFTHRONES, familiar));
    assertFalse(FamiliarSlotGroup.CROWN.put(state, Slot.BUDDYBJORN, familiar));

    assertThat(ItemSlotGroup.CARD_SLEEVE.get(state, Slot.CARDSLEEVE), is(card));
    assertNull(ItemSlotGroup.CARD_SLEEVE.get(state, Slot.CODPIECE1));
    assertThat(FamiliarSlotGroup.CROWN.get(state, Slot.CROWNOFTHRONES), is(familiar));
    assertThat(ItemSlotGroup.ETERNITY_CODPIECE.modifiers(ItemPool.HEARTSTONE), is(notNullValue()));
    assertThat(FamiliarSlotGroup.CROWN.modifiers(familiar), is(notNullValue()));
  }

  private static List<AdventureResult> items(int first, int second) {
    return List.of(EquipmentRequest.UNEQUIP, ItemPool.get(first, 1), ItemPool.get(second, 1));
  }
}
