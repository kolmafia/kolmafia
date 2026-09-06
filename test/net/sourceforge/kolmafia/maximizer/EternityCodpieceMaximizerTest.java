package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.getBoosts;
import static internal.helpers.Maximizer.maximize;
import static internal.helpers.Maximizer.modFor;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withItemInCloset;
import static internal.helpers.Player.withItemInFreepulls;
import static internal.helpers.Player.withMallPrice;
import static internal.helpers.Player.withMeat;
import static internal.helpers.Player.withNotAllowedInStandard;
import static internal.helpers.Player.withOverrideModifiers;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withRestricted;
import static internal.helpers.Player.withStats;
import static internal.matchers.Maximizer.recommends;
import static internal.matchers.Maximizer.recommendsSlot;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import internal.helpers.Cleanups;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

public class EternityCodpieceMaximizerTest {
  private static final String CONTROL_CRYSTAL = "control crystal";
  private static final String MASSIVE_GEMSTONE = "massive gemstone";
  private static final String HEALING_CRYSTAL = "New Age healing crystal";
  private static final String GLOWING_CRYSTAL = "glowing New Age crystal";
  private static final String HEART_OF_THE_VOLCANO = "heart of the volcano";

  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("EternityCodpieceMaximizerTest");
    Preferences.reset("EternityCodpieceMaximizerTest");
  }

  private static Cleanups withWornCodpiece(Cleanups... extras) {
    var cleanups = new Cleanups(withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE));
    for (var extra : extras) cleanups.add(extra);
    return cleanups;
  }

  private static long selectedGems(String name) {
    return SlotSet.CODPIECE_SLOTS.stream()
        .map(Maximizer.best.equipment::get)
        .filter(item -> item != null && name.equals(item.getName()))
        .count();
  }

  private static int codpieceCombinations() {
    return Maximizer.eval.codpiece().getCombinationsEvaluated();
  }

  @Test
  void searchesOnlyAHypotheticalCodpiece() {
    try (var cleanups =
        withWornCodpiece(
            withEquipped(Slot.CODPIECE2, ItemPool.get(MASSIVE_GEMSTONE)),
            withItem(CONTROL_CRYSTAL))) {
      var before = new EnumMap<Slot, AdventureResult>(Slot.class);
      SlotSet.CODPIECE_SLOTS.forEach(slot -> before.put(slot, EquipmentManager.getEquipment(slot)));

      assertThat(maximize("mys"), is(true));

      assertThat(selectedGems(CONTROL_CRYSTAL), equalTo(1L));
      assertThat(selectedGems(MASSIVE_GEMSTONE), equalTo(1L));
      SlotSet.CODPIECE_SLOTS.forEach(
          slot -> assertThat(EquipmentManager.getEquipment(slot), equalTo(before.get(slot))));
    }
  }

  @Test
  void canDisableCodpieceGemSearch() {
    try (var cleanups =
        new Cleanups(
            withProperty("maximizerConsiderCodpieceGems", false),
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.CODPIECE1, ItemPool.get(MASSIVE_GEMSTONE)),
            withItem(CONTROL_CRYSTAL),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, MASSIVE_GEMSTONE, "Mysticality: +25"),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, CONTROL_CRYSTAL, "Mysticality: +100"))) {
      assertThat(maximize("mys, -tie"), is(true));
      assertThat(getBoosts(), hasItem(recommends(ItemPool.THE_ETERNITY_CODPIECE)));
      assertThat(selectedGems(MASSIVE_GEMSTONE), equalTo(1L));
      assertThat(selectedGems(CONTROL_CRYSTAL), equalTo(0L));
      assertThat(modFor(DoubleModifier.MYS), greaterThanOrEqualTo(25.0));
    }
  }

  @Test
  void equipsTheCodpieceBeforeInsertingItsGem() {
    try (var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE), withItem(CONTROL_CRYSTAL))) {
      assertThat(maximize("mys"), is(true));
      var boosts = getBoosts();
      int codpiece =
          boosts.indexOf(
              boosts.stream()
                  .filter(recommends(ItemPool.THE_ETERNITY_CODPIECE)::matches)
                  .findFirst()
                  .orElseThrow());
      int gem =
          boosts.indexOf(
              boosts.stream()
                  .filter(recommends(CONTROL_CRYSTAL)::matches)
                  .findFirst()
                  .orElseThrow());
      assertThat(gem, greaterThanOrEqualTo(codpiece + 1));
    }
  }

  @Test
  void honorsForbiddenAndExcludedCodpieceSlots() {
    try (var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE), withItem(CONTROL_CRYSTAL))) {
      assertThat(maximize("-equip eternity codpiece, mys"), is(true));
      assertThat(getBoosts(), not(hasItem(recommends(CONTROL_CRYSTAL))));
    }

    try (var cleanups =
        withWornCodpiece(withEquipped(Slot.CODPIECE3, ItemPool.get(MASSIVE_GEMSTONE)))) {
      assertThat(maximize("-equip massive gemstone, -codpiece3, item drop"), is(true));
      assertThat(Maximizer.best.equipment.get(Slot.CODPIECE3).getName(), equalTo(MASSIVE_GEMSTONE));
    }
  }

  @Test
  void failsWhenRequiredGemsAreUnavailableOrCannotFit() {
    try (var cleanups = withWornCodpiece()) {
      assertThat(maximize("+equip control crystal, meat drop"), is(false));
    }

    try (var cleanups =
        withWornCodpiece(
            withItem(CONTROL_CRYSTAL),
            withItem(MASSIVE_GEMSTONE),
            withItem(HEALING_CRYSTAL),
            withItem(GLOWING_CRYSTAL),
            withItem("baconstone"),
            withItem("priceless diamond"))) {
      assertThat(
          maximize(
              "+equip control crystal, +equip massive gemstone, +equip New Age healing crystal, "
                  + "+equip glowing New Age crystal, +equip baconstone, +equip priceless diamond"),
          is(false));
    }
  }

  @Test
  void accountsForDualRoleGemCopies() {
    try (var cleanups =
        withWornCodpiece(withStats(100, 100, 100), withItem(HEART_OF_THE_VOLCANO, 2))) {
      assertThat(maximize("hp regen min 10 max, hot dmg"), is(true));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.ACCESSORY2, HEART_OF_THE_VOLCANO)));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.CODPIECE1, HEART_OF_THE_VOLCANO)));
    }
  }

  @Test
  void evaluatesFamiliarDependentGemsAtTheSearchLeaf() {
    try (var cleanups =
        withWornCodpiece(withFamiliar(FamiliarPool.MOSQUITO), withItem(ItemPool.HEARTSTONE))) {
      assertThat(maximize("familiar weight"), is(true));
      assertThat(selectedGems("Heartstone"), equalTo(1L));
      assertThat(modFor(DoubleModifier.FAMILIAR_WEIGHT), greaterThanOrEqualTo(5.0));
    }
  }

  @Test
  void findsOptimalCappedAndInteractingGemCombinations() {
    try (var cleanups =
        withWornCodpiece(withItem(HEALING_CRYSTAL, 5), withItem(GLOWING_CRYSTAL, 5))) {
      assertThat(maximize("10 hp regen min 25 max, 1 mp regen min"), is(true));
      assertThat(selectedGems(HEALING_CRYSTAL), equalTo(3L));
      assertThat(selectedGems(GLOWING_CRYSTAL), equalTo(2L));
    }

    try (var cleanups =
        withWornCodpiece(
            withItem(ItemPool.RUBEE),
            withItem(ItemPool.SHARD_OF_DOUBLE_ICE),
            withItem("Lapis Lazuli"),
            withItem(ItemPool.SHADOW_GLASS),
            withItem(ItemPool.AZURITE))) {
      assertThat(maximize("10 prismatic damage, -1 hot damage, -tie"), is(true));
      assertThat(
          10 * modFor(DoubleModifier.PRISMATIC_DAMAGE) - modFor(DoubleModifier.HOT_DAMAGE),
          equalTo(90.0));
    }
  }

  @Test
  void supportsBooleanAndBitmapGemRequirements() {
    try (var cleanups =
        withWornCodpiece(
            withItem(ItemPool.ALIEN_GEMSTONE),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, ItemPool.ALIEN_GEMSTONE, "Adventure Underwater"))) {
      assertThat(maximize("adventure underwater, -tie"), is(true));
      assertThat(selectedGems("alien gemstone"), equalTo(1L));
    }

    try (var cleanups =
        withWornCodpiece(
            withItem(ItemPool.ALIEN_GEMSTONE),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, ItemPool.ALIEN_GEMSTONE, "Surgeonosity: +1"))) {
      assertThat(maximize("1 surgeonosity, -tie"), is(true));
      assertThat(selectedGems("alien gemstone"), equalTo(1L));
    }
  }

  @Test
  void permitsOwnedStandardRestrictedGemsButNotExternalCopies() {
    var restriction =
        new Cleanups(
            withPath(Path.STANDARD),
            withRestricted(true),
            withNotAllowedInStandard(RestrictedItemType.ITEMS, CONTROL_CRYSTAL));
    try (var cleanups = new Cleanups(restriction, withWornCodpiece(withItem(CONTROL_CRYSTAL)))) {
      assertThat(maximize("mys"), is(true));
      assertThat(selectedGems(CONTROL_CRYSTAL), equalTo(1L));
    }

    try (var cleanups =
        new Cleanups(
            withPath(Path.STANDARD),
            withRestricted(true),
            withNotAllowedInStandard(RestrictedItemType.ITEMS, CONTROL_CRYSTAL),
            withProperty("autoSatisfyWithCloset", true),
            withWornCodpiece(withItemInCloset(CONTROL_CRYSTAL)))) {
      assertThat(maximize("mys"), is(true));
      assertThat(selectedGems(CONTROL_CRYSTAL), equalTo(0L));
    }
  }

  @Test
  void usesInstalledGemsWhenRetrievingTheCodpiece() {
    try (var cleanups =
        new Cleanups(
            withProperty("autoSatisfyWithCloset", true),
            withItemInCloset(ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.CODPIECE1, ItemPool.get(CONTROL_CRYSTAL)))) {
      assertThat(maximize("mys"), is(true));
      assertThat(getBoosts(), hasItem(recommends(ItemPool.THE_ETERNITY_CODPIECE)));
      assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.CODPIECE1))));
    }
  }

  @Test
  void acquiresEveryAvailableCopyWithoutExceedingThePriceLimit()
      throws MaximizerInterruptedException {
    try (var cleanups =
        withWornCodpiece(withInteractivity(false), withItemInFreepulls(CONTROL_CRYSTAL, 5))) {
      assertThat(maximize("mys"), is(true));
      assertThat(
          getBoosts().stream().filter(recommends(CONTROL_CRYSTAL)::matches).count(), equalTo(5L));
    }

    var externalGem = ItemPool.get(ItemPool.BASEBALL_DIAMOND);
    try (var cleanups =
        withWornCodpiece(
            withProperty("autoSatisfyWithCloset", true),
            withFamiliar(FamiliarPool.LEFT_HAND),
            withEquipped(Slot.FAMILIAR, externalGem),
            withItemInCloset(externalGem.getItemId(), 1),
            withItemInFreepulls(externalGem.getItemId(), 1))) {
      assertThat(maximize("weapon damage, -offhand"), is(true));
      var commands =
          getBoosts().stream()
              .filter(boost -> SlotSet.CODPIECE_SLOTS.contains(boost.getSlot()))
              .map(Boost::getCmd)
              .toList();
      assertThat(commands, hasItem(startsWith("closet take 1 \u00B6" + externalGem.getItemId())));
      assertThat(commands, hasItem(startsWith("pull 1 \u00B6" + externalGem.getItemId())));
    }

    var gem = ItemPool.get(MASSIVE_GEMSTONE);
    try (var cleanups =
        new Cleanups(
            withProperty("autoSatisfyWithMall", true),
            withInteractivity(true),
            withMeat(1_000_000),
            withMallPrice(gem.getItemId(), 100))) {
      var checked =
          new CheckedItem(
              gem.getItemId(), EquipScope.SPECULATE_ANY, 350, PriceLevel.BUYABLE_ONLY, true);
      checked.validate(350, PriceLevel.BUYABLE_ONLY);
      assertThat(checked.mallBuyable, equalTo(3));
    }
  }

  @Test
  void retainsCandidatesNeededOnlyForMinimumsOrCappedInteractions() {
    try (var cleanups = withWornCodpiece(withItem("priceless diamond"))) {
      assertThat(maximize("0 da, 10 min, -tie"), is(true));
      assertThat(getBoosts(), hasItem(recommends("priceless diamond")));
    }

    try (var cleanups =
        withWornCodpiece(withItem("rainbow pearl"), withItem("18-picohertz resonator crystal"))) {
      assertThat(
          maximize("1 hot damage, 2 cold spell damage, -1.5 cold damage 5 max, -tie"), is(true));
      assertThat(getBoosts(), hasItem(recommends("rainbow pearl")));
      assertThat(getBoosts(), hasItem(recommends("18-picohertz resonator crystal")));
    }
  }

  @Test
  void usesExactFallbackAndSafePruning() {
    try (var cleanups =
        withWornCodpiece(
            withStats(100, 100, 100),
            withItem(CONTROL_CRYSTAL),
            withItem("baconstone"),
            withItem("stone of eXtreme power"))) {
      assertThat(maximize("mys"), is(true));
      assertThat(codpieceCombinations(), equalTo(8));
    }

    try (var cleanups =
        withWornCodpiece(
            withItem(HEALING_CRYSTAL), withItem(GLOWING_CRYSTAL), withItem(HEART_OF_THE_VOLCANO))) {
      assertThat(maximize("hp regen min"), is(true));
      assertThat(codpieceCombinations(), lessThan(8));
    }
  }

  @Test
  void countsGemSearchAgainstTheExistingCombinationLimit() {
    try (var cleanups =
        withWornCodpiece(
            withProperty("maximizerCombinationLimit", 5),
            withStats(100, 100, 100),
            withItem(CONTROL_CRYSTAL),
            withItem("baconstone"),
            withItem("stone of eXtreme power"))) {
      maximize("mys");
      assertThat(Maximizer.bestChecked, equalTo(5));
      assertThat(
          getBoosts(),
          hasItem(hasToString(containsString("hit combination limit, optimality not guaranteed"))));
    }
  }

  @Test
  void movesAnInstalledGemInsteadOfBuyingAnotherCopy() {
    var gem = ItemPool.get("incredibly dense meat gem");
    try (var cleanups =
        withWornCodpiece(
            withStats(100, 100, 100),
            withEquipped(Slot.CODPIECE1, gem),
            withEquipped(Slot.CODPIECE2, gem))) {
      assertThat(maximize("meat drop, -codpiece1"), is(true));
      var boost =
          getBoosts().stream()
              .filter(recommendsSlot(Slot.ACCESSORY2, gem.getName())::matches)
              .findFirst()
              .orElseThrow();
      assertThat(boost.getCmd(), startsWith("unequip codpiece2;equip acc2 \u00B6"));
    }
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "KOLMAFIA_CODPIECE_DEFAULTS_BENCHMARK", matches = "true")
  void benchmarksEveryDefaultExpressionWithEveryCodpieceGem() {
    var cleanups =
        new Cleanups(
            withClass(AscensionClass.SEAL_CLUBBER),
            withStats(10_000, 10_000, 10_000),
            withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY, 20),
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withProperty("maximizerCombinationLimit", 0));
    int gemCount = 0;
    for (var entry : ModifierDatabase.getAllModifiersOfType(ModifierType.ETERNITY_CODPIECE)) {
      if (entry.getKey().isInt()) {
        cleanups.add(withItem(entry.getKey().getIntValue(), 5));
        gemCount++;
      }
    }

    try (cleanups) {
      List<String> expressions = List.of(Preferences.getDefault("maximizerList").split(" \\| "));
      var elapsed = new ArrayList<Double>();
      long maxChecks = 0;
      String exclusions =
          ", -hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, " + "-acc1, -acc2, -acc3";
      for (String expression : expressions) {
        long start = System.nanoTime();
        maximize(expression + exclusions);
        double millis = (System.nanoTime() - start) / 1_000_000.0;
        elapsed.add(millis);
        maxChecks = Math.max(maxChecks, codpieceCombinations());
        System.out.printf(
            "CODPIECE_DEFAULT_BENCHMARK expression=%s combinations=%d ms=%.3f%n",
            expression, codpieceCombinations(), millis);
      }
      elapsed.sort(Double::compareTo);
      double total = elapsed.stream().mapToDouble(Double::doubleValue).sum();
      double median = elapsed.get(elapsed.size() / 2);
      long exhaustiveCombinations = 1;
      for (int slot = 1; slot <= SlotSet.CODPIECE_SLOTS.size(); slot++) {
        exhaustiveCombinations = exhaustiveCombinations * (gemCount + slot) / slot;
      }
      System.out.printf(
          "CODPIECE_DEFAULT_BENCHMARK_TOTAL expressions=%d gems=%d maxCombinations=%d "
              + "medianMs=%.3f totalMs=%.3f%n",
          expressions.size(), gemCount, maxChecks, median, total);
      assertThat(maxChecks, lessThan(exhaustiveCombinations / 100));
    }
  }
}
