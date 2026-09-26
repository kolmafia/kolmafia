package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.getBoosts;
import static internal.helpers.Maximizer.maximize;
import static internal.helpers.Maximizer.modFor;
import static internal.helpers.Player.withClan;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withItemInCloset;
import static internal.helpers.Player.withItemInFreepulls;
import static internal.helpers.Player.withItemInStash;
import static internal.helpers.Player.withMallPrice;
import static internal.helpers.Player.withMeat;
import static internal.helpers.Player.withNotAllowedInStandard;
import static internal.helpers.Player.withOverrideModifiers;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withRestricted;
import static internal.helpers.Player.withRonin;
import static internal.helpers.Player.withStats;
import static internal.matchers.Maximizer.recommends;
import static internal.matchers.Maximizer.recommendsSlot;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import internal.helpers.Cleanups;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

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
    return Maximizer.bestChecked;
  }

  private static boolean maximizeBuyable(String expression, int priceLimit) {
    return Maximizer.maximize(
        expression,
        priceLimit,
        PriceLevel.BUYABLE_ONLY,
        EquipScope.SPECULATE_ANY,
        EnumSet.allOf(KoLConstants.filterType.class));
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
  void doesNotActivateCodpieceSearchWhenCodpieceCannotParticipate() {
    try (var cleanups = withItem(CONTROL_CRYSTAL)) {
      assertThat(maximize("mys"), is(true));
      assertThat(Maximizer.eval.codpiece().isSearchable(), is(false));
      assertThat(selectedGems(CONTROL_CRYSTAL), equalTo(0L));
    }

    try (var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE), withItem(CONTROL_CRYSTAL))) {
      assertThat(maximize("mys, -acc1, -acc2, -acc3"), is(true));
      assertThat(Maximizer.eval.codpiece().isSearchable(), is(false));
      assertThat(selectedGems(CONTROL_CRYSTAL), equalTo(0L));
    }
  }

  @Test
  void reusesInstalledGemWhenCodpieceCannotBeWorn() {
    var heart = ItemPool.get(HEART_OF_THE_VOLCANO);
    try (var cleanups =
        new Cleanups(
            withPath(Path.STANDARD),
            withRestricted(true),
            withRonin(true),
            withNotAllowedInStandard(RestrictedItemType.ITEMS, "the eternity codpiece"),
            withItem(ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.CODPIECE1, heart),
            withStats(100, 100, 100))) {
      assertThat(maximize("hp regen, -tie"), is(true));
      assertThat(Maximizer.eval.codpiece().isSearchable(), is(false));
      assertThat(selectedGems(HEART_OF_THE_VOLCANO), equalTo(0L));
      var boost =
          getBoosts().stream()
              .filter(recommendsSlot(Slot.ACCESSORY1, HEART_OF_THE_VOLCANO)::matches)
              .findFirst()
              .orElseThrow();
      assertThat(boost.getCmd(), startsWith("equip acc1 "));
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
  void weighsTheCodpieceAgainstOrdinaryAccessories() {
    try (var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withItem("consolation ribbon", 3))) {
      assertThat(maximize("mus, mys, mox, -tie"), is(true));

      var accessories =
          SlotSet.ACCESSORY_SLOTS.stream().map(Maximizer.best.equipment::get).toList();
      assertThat(accessories, hasItem(ItemPool.get(ItemPool.THE_ETERNITY_CODPIECE)));
      assertThat(
          accessories.stream().filter(ItemPool.get("consolation ribbon")::equals).count(),
          equalTo(2L));
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"hp regen, -tie", "+equip heartstone, -equip the eternity codpiece, -tie"})
  void movesInstalledHeartstoneToAnAccessory(String expression) {
    var heartstone = ItemPool.get(ItemPool.HEARTSTONE);
    try (var cleanups = withWornCodpiece(withEquipped(Slot.CODPIECE1, heartstone))) {
      assertThat(maximize(expression), is(true));

      assertThat(
          SlotSet.ACCESSORY_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .filter(heartstone::equals)
              .count(),
          equalTo(1L));
      assertThat(selectedGems("Heartstone"), equalTo(0L));
    }
  }

  @Test
  void honorsForcedCodpieceAndGemSlots() {
    try (var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withEquippableItem(ItemPool.HEARTSTONE))) {
      assertThat(
          maximize(
              "+equip heartstone, +equip the eternity codpiece, -acc1, -acc2, "
                  + "-codpiece1, -codpiece2, -codpiece3, -codpiece4, -tie"),
          is(true));
      assertThat(
          Maximizer.best.equipment.get(Slot.ACCESSORY3).getItemId(),
          equalTo(ItemPool.THE_ETERNITY_CODPIECE));
      assertThat(
          Maximizer.best.equipment.get(Slot.CODPIECE5).getItemId(), equalTo(ItemPool.HEARTSTONE));
    }

    try (var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withEquippableItem(ItemPool.HEARTSTONE))) {
      assertThat(
          maximize(
              "+equip heartstone, +equip the eternity codpiece, -acc1, -acc2, "
                  + "-codpiece1, -codpiece2, -codpiece3, -codpiece4, -codpiece5, -tie"),
          is(false));
    }
  }

  @ParameterizedTest
  @CsvSource({"1, false", "3, false", "4, false", "6, true"})
  void wearsTheCodpieceOnlyWhenDenseMeatGemsRepayItsAccessoryCost(
      int gemCount, boolean wearsCodpiece) {
    var meatGem = ItemPool.get("incredibly dense meat gem");
    try (var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withItem(meatGem.getItemId(), gemCount))) {
      assertThat(maximize("meat drop, -tie"), is(true));

      assertThat(
          SlotSet.ACCESSORY_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .anyMatch(ItemPool.get(ItemPool.THE_ETERNITY_CODPIECE)::equals),
          is(wearsCodpiece));
    }
  }

  @Test
  void slotsRequiredPeridotAlongsideMeatGemAccessories() {
    var peridot = ItemPool.get(ItemPool.PERIDOT_OF_PERIL);
    try (var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withEquippableItem("incredibly dense meat gem", 3),
            withEquippableItem(peridot))) {
      assertThat(maximize("+equip peridot of peril, meat, -tie"), is(true));

      assertThat(selectedGems("Peridot of Peril"), equalTo(1L));
      assertThat(
          SlotSet.ACCESSORY_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .filter(ItemPool.get("incredibly dense meat gem")::equals)
              .count(),
          equalTo(2L));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"0 mus", "tie"})
  void considersAllCodpieceSlotsForImplicitAndExplicitTiebreaker(String expression) {
    var massiveGemstone = ItemPool.get(MASSIVE_GEMSTONE);
    try (var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withItem(ItemPool.HAMETHYST, 5),
            withItem(massiveGemstone.getItemId(), 5))) {
      assertThat(maximize(expression), is(true));

      assertThat(selectedGems(MASSIVE_GEMSTONE), equalTo(5L));
    }
  }

  @Test
  void excludesTiebreakerOnlyGemsWhenTieIsDisabled() {
    try (var cleanups = withWornCodpiece(withItem(MASSIVE_GEMSTONE, 5))) {
      assertThat(maximize("0 mus, -tie"), is(true));

      assertThat(selectedGems(MASSIVE_GEMSTONE), equalTo(0L));
    }
  }

  @Test
  void preservesItemDropperPriorityAheadOfNumericTiebreaker() {
    var earlyNumericGem = ItemPool.get("green peawee marble");
    var laterItemDropper = ItemPool.get(MASSIVE_GEMSTONE);
    try (var cleanups =
        withWornCodpiece(
            withItem(earlyNumericGem),
            withItem(laterItemDropper),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, earlyNumericGem.getItemId(), "Initiative: +100"),
            withOverrideModifiers(
                ModifierType.ITEM, laterItemDropper.getItemId(), "Drops Items"))) {
      assertThat(maximize("0 mus, -codpiece2, -codpiece3, -codpiece4, -codpiece5"), is(true));

      assertThat(selectedGems(MASSIVE_GEMSTONE), equalTo(1L));
    }
  }

  @Test
  void satisfiesThreeRequiredDualUseGemsAcrossAccessoriesAndCodpiece() {
    var requiredGems =
        List.of(
            ItemPool.get("incredibly dense meat gem"),
            ItemPool.get(HEART_OF_THE_VOLCANO),
            ItemPool.get(ItemPool.HEARTSTONE));
    try (var cleanups =
        new Cleanups(
            withStats(1_000, 1_000, 1_000),
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
            withItem(requiredGems.get(0)),
            withItem(requiredGems.get(1)),
            withItem(requiredGems.get(2)))) {
      assertThat(
          maximize(
              "+equip eternity codpiece, +equip incredibly dense meat gem, "
                  + "+equip heart of the volcano, +equip Heartstone, meat drop, hp regen, "
                  + "familiar weight, -tie"),
          is(true));

      var selectedSlots = new ArrayList<>(SlotSet.ACCESSORY_SLOTS);
      selectedSlots.addAll(SlotSet.CODPIECE_SLOTS);
      for (var gem : requiredGems) {
        assertThat(
            selectedSlots.stream().map(Maximizer.best.equipment::get).filter(gem::equals).count(),
            equalTo(1L));
      }
      assertThat(
          SlotSet.CODPIECE_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .filter(requiredGems::contains)
              .count(),
          greaterThanOrEqualTo(1L));
    }
  }

  @Test
  void placesRequiredDualUseGemWhereItScoresBest() {
    var meatGem = ItemPool.get("incredibly dense meat gem");
    var gemstone = ItemPool.get(MASSIVE_GEMSTONE);
    try (var cleanups =
        new Cleanups(
            withStats(1_000, 1_000, 1_000),
            withItem(meatGem.getItemId(), 2),
            withItem(gemstone.getItemId(), 3),
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE))) {
      assertThat(
          maximize("+equip incredibly dense meat gem, 0.1 meat drop, 0.1 item drop, -tie"),
          is(true));

      assertThat(
          SlotSet.ACCESSORY_SLOTS.stream()
              .map(Maximizer.best.equipment::get)
              .filter(meatGem::equals)
              .count(),
          equalTo(2L));
      assertThat(selectedGems("incredibly dense meat gem"), equalTo(0L));
      assertThat(selectedGems(MASSIVE_GEMSTONE), equalTo(3L));
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
  void ignoresSingleEquipRestrictionWhenSocketingGems() {
    try (var cleanups =
        withWornCodpiece(withFamiliar(FamiliarPool.MOSQUITO), withItem(ItemPool.HEARTSTONE, 2))) {
      assertThat(maximize("familiar weight"), is(true));
      assertThat(selectedGems("Heartstone"), equalTo(2L));
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

  @ParameterizedTest
  @CsvSource({
    "Adventure Underwater, 'adventure underwater, -tie'",
    "Surgeonosity: +1, '1 surgeonosity, -tie'"
  })
  void supportsBooleanAndBitmapGemRequirements(String modifier, String expression) {
    try (var cleanups =
        withWornCodpiece(
            withItem(ItemPool.ALIEN_GEMSTONE),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, ItemPool.ALIEN_GEMSTONE, modifier))) {
      assertThat(maximize(expression), is(true));
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

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void keepsInstalledGemsThatAreNotOtherwiseAccessible(boolean gemIsStandardRestricted) {
    try (var cleanups =
        new Cleanups(
            withPath(Path.STANDARD),
            withRestricted(gemIsStandardRestricted),
            withNotAllowedInStandard(RestrictedItemType.ITEMS, MASSIVE_GEMSTONE),
            withEquipped(Slot.CODPIECE1, ItemPool.get(MASSIVE_GEMSTONE)),
            withItem(CONTROL_CRYSTAL))) {
      assertThat(maximize("mys"), is(true));
      assertThat(Maximizer.best.equipment.get(Slot.CODPIECE1).getName(), equalTo(MASSIVE_GEMSTONE));
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
              gem.getItemId(),
              EquipScope.SPECULATE_ANY,
              350,
              PriceLevel.BUYABLE_ONLY,
              CodpieceMaximizer.maxUsefulGemCopies(gem.getItemId()),
              true);
      checked.validate(350, PriceLevel.BUYABLE_ONLY);
      assertThat(checked.mallBuyable, equalTo(3));
    }

    long storageMeat = KoLCharacter.getStorageMeat();
    try (var cleanups =
        new Cleanups(
            new Cleanups(() -> KoLCharacter.setStorageMeat(storageMeat)),
            withProperty("autoSatisfyWithMall", true),
            withInteractivity(false),
            withRonin(true),
            withMallPrice(gem.getItemId(), 100))) {
      KoLCharacter.setStorageMeat(1_000_000);
      var checked =
          new CheckedItem(
              gem.getItemId(),
              EquipScope.SPECULATE_ANY,
              350,
              PriceLevel.BUYABLE_ONLY,
              CodpieceMaximizer.maxUsefulGemCopies(gem.getItemId()),
              true);
      checked.validate(350, PriceLevel.BUYABLE_ONLY);
      assertThat(checked.pullBuyable, equalTo(3));
    }
  }

  @Test
  void retrievesAdditionalCopiesFromTheClanStash() {
    var gem = ItemPool.get("unblemished pearl");
    try (var clanCleanup = withClan(1, "Test Clan")) {
      boolean hadClan = KoLCharacter.hasClan();
      KoLCharacter.setClan(true);
      ClanManager.setStashRetrieved();

      try (var cleanups =
          new Cleanups(
              new Cleanups(() -> KoLCharacter.setClan(hadClan)),
              withProperty("autoSatisfyWithStash", true),
              withInteractivity(true),
              withWornCodpiece(
                  withEquipped(Slot.CODPIECE1, gem), withItemInStash("unblemished pearl")))) {
        assertThat(maximize("adv"), is(true));
        assertThat(selectedGems("unblemished pearl"), equalTo(2L));
        assertThat(
            getBoosts(),
            hasItem(
                hasProperty(
                    "cmd",
                    startsWith("stash take 1 \u00B6" + gem.getItemId() + ";equip codpiece"))));
      }
    }
  }

  @Test
  void reusesAnAccessoryCopyReleasedByAnEarlierRecommendation() {
    var gem = ItemPool.get(HEART_OF_THE_VOLCANO);
    try (var cleanups =
        withWornCodpiece(
            withEquipped(Slot.ACCESSORY2, gem),
            withEquipped(Slot.ACCESSORY3, "Mr. Accessory Jr."),
            withEquippableItem(ItemPool.MAFIA_THUMB_RING))) {
      assertThat(maximize("+equip mafia thumb ring, hot damage, -acc3"), is(true));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.CODPIECE1, HEART_OF_THE_VOLCANO)));
      assertThat(
          getBoosts().stream()
              .filter(recommendsSlot(Slot.CODPIECE1, HEART_OF_THE_VOLCANO)::matches)
              .findFirst()
              .orElseThrow()
              .getCmd(),
          equalTo("equip codpiece1 \u00B6" + gem.getItemId()));
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
  void safelyPrunesModBonus() {
    try (var cleanups =
        withWornCodpiece(
            withItem(CONTROL_CRYSTAL),
            withItem(MASSIVE_GEMSTONE),
            withItem(HEALING_CRYSTAL),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, CONTROL_CRYSTAL, "HP Regen Max: +10"),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, MASSIVE_GEMSTONE, "HP Regen Max: +6"),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, HEALING_CRYSTAL, "HP Regen Max: +4"),
            withOverrideModifiers(ModifierType.ITEM, MASSIVE_GEMSTONE, "Adventure Underwater"),
            withOverrideModifiers(ModifierType.ITEM, HEALING_CRYSTAL, "Adventure Underwater"))) {
      assertThat(
          maximize(
              "1 hp regen max 10 max, 3 modbonus Adventure Underwater, -tie, "
                  + "-hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, "
                  + "-acc1, -acc2, -acc3, -codpiece3, -codpiece4, -codpiece5"),
          is(true));
      assertThat(Maximizer.best.getScore(), equalTo(16.0));
      assertThat(codpieceCombinations(), lessThan(7));
      assertThat(selectedGems(CONTROL_CRYSTAL), equalTo(0L));
      assertThat(selectedGems(MASSIVE_GEMSTONE), equalTo(1L));
      assertThat(selectedGems(HEALING_CRYSTAL), equalTo(1L));
    }
  }

  @Test
  void safelyPrunesItemBonuses() {
    try (var cleanups =
        withWornCodpiece(
            withItem(CONTROL_CRYSTAL),
            withItem(MASSIVE_GEMSTONE),
            withItem(HEALING_CRYSTAL),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, CONTROL_CRYSTAL, "HP Regen Max: +10"),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, MASSIVE_GEMSTONE, "HP Regen Max: +6"),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, HEALING_CRYSTAL, "HP Regen Max: +4"))) {
      assertThat(
          maximize(
              "1 hp regen max 10 max, 3 bonus massive gemstone, "
                  + "3 bonus New Age healing crystal, -tie, "
                  + "-hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, "
                  + "-acc1, -acc2, -acc3, -codpiece3, -codpiece4, -codpiece5"),
          is(true));
      assertThat(Maximizer.best.getScore(), equalTo(16.0));
      assertThat(codpieceCombinations(), lessThan(7));
      assertThat(selectedGems(CONTROL_CRYSTAL), equalTo(0L));
      assertThat(selectedGems(MASSIVE_GEMSTONE), equalTo(1L));
      assertThat(selectedGems(HEALING_CRYSTAL), equalTo(1L));
    }
  }

  @Test
  void safelyPrunesAdventureScoreInSlowAndSteady() {
    try (var cleanups =
        withWornCodpiece(
            withPath(Path.SLOW_AND_STEADY),
            withItem(CONTROL_CRYSTAL),
            withItem(MASSIVE_GEMSTONE),
            withItem(HEALING_CRYSTAL),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE,
                CONTROL_CRYSTAL,
                "Adventures: +100, HP Regen Max: +10"),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, MASSIVE_GEMSTONE, "HP Regen Max: +6"),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, HEALING_CRYSTAL, "HP Regen Max: +4"))) {
      assertThat(
          maximize(
              "1 hp regen max, 1 adv, -tie, "
                  + "-hat, -weapon, -offhand, -back, -shirt, -pants, -familiar, "
                  + "-acc1, -acc2, -acc3, -codpiece3, -codpiece4, -codpiece5"),
          is(true));
      assertThat(codpieceCombinations(), lessThan(7));
      assertThat(selectedGems(CONTROL_CRYSTAL), equalTo(1L));
      assertThat(selectedGems(MASSIVE_GEMSTONE), equalTo(1L));
      assertThat(selectedGems(HEALING_CRYSTAL), equalTo(0L));
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
  void pricesAdventureGemsBoughtAfterEquippedCopies() {
    var pearl = ItemPool.get("unblemished pearl");
    try (var cleanups =
        withWornCodpiece(
            withEquipped(Slot.CODPIECE1, pearl),
            withEquipped(Slot.CODPIECE2, pearl),
            withEquipped(Slot.CODPIECE3, pearl),
            withInteractivity(true),
            withProperty("autoSatisfyWithMall", true),
            withMallPrice(pearl.getItemId(), 1_000),
            withMeat(100_000))) {
      assertThat(maximizeBuyable("adv, -tie", 10_000), is(true));

      assertThat(selectedGems("unblemished pearl"), equalTo(5L));
      var boughtPearls =
          getBoosts().stream()
              .filter(boost -> pearl.equals(boost.getItem()))
              .filter(boost -> boost.toString().startsWith("acquire & equip codpiece"))
              .toList();
      assertThat(boughtPearls.size(), equalTo(2));
      assertThat(
          boughtPearls.stream().allMatch(boost -> boost.toString().contains("1,000 meat")),
          is(true));
    }
  }

  @Test
  void keepsGrandfatherWatchWhenOtherUpgradesMaskWorseBoots() {
    var watch = ItemPool.get("grandfather watch");
    var boots = ItemPool.get("Boots of Twilight Whispers");
    var ninjammies = ItemPool.get("ninjammies");
    var pearl = ItemPool.get("unblemished pearl");
    try (var cleanups =
        new Cleanups(
            withStats(1_000, 1_000, 1_000),
            withEquipped(Slot.HAT, "time cop top hat"),
            withEquipped(Slot.WEAPON, "sword behind inappropriate prepositions"),
            withEquipped(Slot.CONTAINER, "Allied Radio Backpack"),
            withEquipped(Slot.SHIRT, "shoe ad T-shirt"),
            withEquipped(Slot.PANTS, "sea chaps"),
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.ACCESSORY2, watch),
            withEquipped(Slot.ACCESSORY3, "Elf Guard insignia (general)"),
            withEquipped(Slot.CODPIECE1, pearl),
            withEquipped(Slot.CODPIECE2, pearl),
            withEquipped(Slot.CODPIECE3, pearl),
            withItem(boots),
            withInteractivity(true),
            withProperty("autoSatisfyWithMall", true),
            withMallPrice(ninjammies.getItemId(), 840_664),
            withMallPrice(pearl.getItemId(), 78_000),
            withMeat(16_000_000),
            withRestricted(false))) {
      var currentScore = new Evaluator("adv").getScore(KoLCharacter.getCurrentModifiers());

      assertThat(maximizeBuyable("adv", 1_000_000), is(true));

      assertThat(Maximizer.best.getScore(), greaterThan(currentScore));
      assertThat(
          SlotSet.ACCESSORY_SLOTS.stream().map(Maximizer.best.equipment::get).toList(),
          hasItem(watch));
      assertThat(selectedGems("unblemished pearl"), equalTo(5L));
      assertThat(getBoosts().stream().noneMatch(boost -> boots.equals(boost.getItem())), is(true));
    }
  }

  @Test
  void toleratesNullEquipmentWhenBuildingBound() throws MaximizerInterruptedException {
    try (var cleanups = withWornCodpiece(withItem(CONTROL_CRYSTAL))) {
      assertThat(maximize("mys, -tie"), is(true));
      var speculation = Maximizer.best.clone();
      speculation.equipment.put(Slot.CARDSLEEVE, null);
      SlotSet.CODPIECE_SLOTS.forEach(slot -> speculation.equipment.put(slot, null));

      Maximizer.eval.codpiece().search(speculation);

      assertThat(speculation.equipment.get(Slot.CARDSLEEVE), equalTo(null));
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
