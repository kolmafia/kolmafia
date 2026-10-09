package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.getBoosts;
import static internal.helpers.Maximizer.maximize;
import static internal.helpers.Maximizer.modFor;
import static internal.helpers.Player.withClan;
import static internal.helpers.Player.withConcoctionRefresh;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withItemInCloset;
import static internal.helpers.Player.withItemInFreepulls;
import static internal.helpers.Player.withItemInStash;
import static internal.helpers.Player.withItemInStorage;
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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import internal.helpers.Cleanups;
import java.util.EnumSet;
import java.util.List;
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
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EternityCodpieceMaximizerTest {
  private static final String CONTROL_CRYSTAL = "control crystal";
  private static final String HEALING_CRYSTAL = "New Age healing crystal";
  private static final String GLOWING_CRYSTAL = "glowing New Age crystal";

  private static Cleanups withWornCodpiece(Cleanups... extras) {
    var cleanups = new Cleanups(withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE));
    for (var extra : extras) cleanups.add(extra);
    return cleanups;
  }

  private static long socketRecommendations(String itemName) {
    return getBoosts().stream()
        .filter(
            boost ->
                SlotSet.CODPIECE_SLOTS.contains(boost.getSlot())
                    && boost.getItem().getName().equalsIgnoreCase(itemName))
        .count();
  }

  private static Boost socketRecommendation(String itemName) {
    return getBoosts().stream()
        .filter(
            boost ->
                SlotSet.CODPIECE_SLOTS.contains(boost.getSlot())
                    && boost.getItem().getName().equalsIgnoreCase(itemName))
        .findFirst()
        .orElseThrow();
  }

  @BeforeAll
  static void beforeAll() {
    net.sourceforge.kolmafia.KoLCharacter.reset("EternityCodpieceMaximizerTest");
    Preferences.reset("EternityCodpieceMaximizerTest");
  }

  @BeforeEach
  void enableExactSearch() {
    Preferences.setString("maximizerConsiderCodpieceGems", "exact");
  }

  @AfterEach
  void restoreFixedSearch() {
    Preferences.setString("maximizerConsiderCodpieceGems", "fixed");
  }

  @Test
  void recommendsAnExactGemConfigurationAfterTheCodpiece() {
    try (var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE), withItem(CONTROL_CRYSTAL))) {
      assertThat(maximize("mys"), is(true));

      var boosts = getBoosts();
      var codpiece =
          boosts.stream()
              .filter(recommends(ItemPool.THE_ETERNITY_CODPIECE)::matches)
              .findFirst()
              .orElseThrow();
      var gem =
          boosts.stream()
              .filter(
                  boost ->
                      SlotSet.CODPIECE_SLOTS.contains(boost.getSlot())
                          && boost.getItem().getName().equals(CONTROL_CRYSTAL))
              .findFirst()
              .orElseThrow();

      assertThat(boosts, hasItem(recommends(ItemPool.THE_ETERNITY_CODPIECE)));
      assertThat(boosts.indexOf(gem), greaterThan(boosts.indexOf(codpiece)));
    }
  }

  @Test
  void preservesTheLiveSocketConfiguration() {
    try (var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.CODPIECE2, ItemPool.get("massive gemstone")),
            withItem(CONTROL_CRYSTAL))) {
      List<?> before = SlotSet.CODPIECE_SLOTS.stream().map(EquipmentManager::getEquipment).toList();

      assertThat(maximize("mys"), is(true));

      assertThat(
          SlotSet.CODPIECE_SLOTS.stream().map(EquipmentManager::getEquipment).toList(), is(before));
    }
  }

  @Test
  void honorsForbiddenGemsAndExcludedSockets() {
    try (var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE), withItem(CONTROL_CRYSTAL))) {
      assertThat(maximize("-equip eternity codpiece, mys"), is(true));
      assertThat(getBoosts(), not(hasItem(recommends(CONTROL_CRYSTAL))));
    }

    try (var cleanups =
        withWornCodpiece(withEquipped(Slot.CODPIECE3, ItemPool.get("massive gemstone")))) {
      assertThat(maximize("-equip massive gemstone, -codpiece3, item drop"), is(true));
      assertThat(
          getBoosts().stream().noneMatch(boost -> boost.getCmd().startsWith("unequip codpiece3")),
          is(true));
    }
  }

  @Test
  void requiresAvailableGemAndCanSatisfyItInASocket() {
    try (var cleanups = withWornCodpiece()) {
      assertThat(maximize("+equip control crystal, meat drop"), is(false));
    }

    try (var cleanups = withWornCodpiece(withItem(CONTROL_CRYSTAL))) {
      assertThat(maximize("+equip control crystal, -acc2, -acc3, mys"), is(true));
      assertThat(socketRecommendations(CONTROL_CRYSTAL), is(1L));
    }
  }

  @Test
  void sharesPhysicalCopiesBetweenAccessoriesAndSockets() {
    try (var cleanups =
        withWornCodpiece(withStats(100, 100, 100), withItem("heart of the volcano", 2))) {
      assertThat(maximize("hp regen min 10 max, hot dmg"), is(true));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.ACCESSORY2, "heart of the volcano")));
      assertThat(socketRecommendations("heart of the volcano"), is(1L));
    }
  }

  @Test
  void socketsMultipleCopiesEvenWhenOrdinaryEquipmentAllowsOnlyOne() {
    try (var cleanups =
        withWornCodpiece(withFamiliar(FamiliarPool.MOSQUITO), withItem(ItemPool.HEARTSTONE, 2))) {
      assertThat(maximize("familiar weight"), is(true));
      assertThat(socketRecommendations("Heartstone"), is(2L));
    }
  }

  @Test
  void searchesCappedGemCombinationsExactly() {
    try (var cleanups =
        withWornCodpiece(withItem(HEALING_CRYSTAL, 5), withItem(GLOWING_CRYSTAL, 5))) {
      assertThat(maximize("10 hp regen min 25 max, 1 mp regen min"), is(true));
      assertThat(socketRecommendations(HEALING_CRYSTAL), is(3L));
      assertThat(socketRecommendations(GLOWING_CRYSTAL), is(2L));
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
          is(90.0));
    }
  }

  @Test
  void searchesExperienceGemsAsACombinedConfiguration() {
    try (var cleanups =
        withWornCodpiece(
            withItem(ItemPool.WORTHLESS_TRINKET),
            withItem(ItemPool.WORTHLESS_GEWGAW),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, ItemPool.WORTHLESS_TRINKET, "Monster Level: 100"),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE,
                ItemPool.WORTHLESS_GEWGAW,
                "Muscle Experience Percent: 100"))) {
      assertThat(maximize("experience, -tie"), is(true));
      assertThat(socketRecommendations("worthless trinket"), is(1L));
      assertThat(socketRecommendations("worthless gewgaw"), is(1L));
    }
  }

  @Test
  void suffixBoundKeepsCombinationsThatBeatTheGreedySeed() {
    try (var cleanups =
        withWornCodpiece(
            withStats(1, 1, 1),
            withItem(CONTROL_CRYSTAL, 5),
            withItem(HEALING_CRYSTAL),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE,
                ItemPool.get(CONTROL_CRYSTAL).getItemId(),
                "Mysticality: +10"),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE,
                ItemPool.get(HEALING_CRYSTAL).getItemId(),
                "Adventure Underwater"))) {
      assertThat(maximize("mysticality, adventure underwater, -tie"), is(true));
      assertThat(socketRecommendations(CONTROL_CRYSTAL), is(4L));
      assertThat(socketRecommendations(HEALING_CRYSTAL), is(1L));
    }
  }

  @Test
  void retainsNegativeGemModifiersForNegativeObjectives() {
    try (var cleanups =
        withWornCodpiece(
            withItem(ItemPool.ALIEN_GEMSTONE),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, ItemPool.ALIEN_GEMSTONE, "Muscle: -100"))) {
      assertThat(maximize("-muscle, -tie"), is(true));
      assertThat(socketRecommendations("alien gemstone"), is(1L));
    }
  }

  @Test
  void considersTieOnlyGemsUnlessTieIsDisabled() {
    try (var cleanups = withWornCodpiece(withItem("massive gemstone", 5))) {
      assertThat(maximize("0 mus"), is(true));
      assertThat(socketRecommendations("massive gemstone"), is(5L));
    }

    try (var cleanups = withWornCodpiece(withItem("massive gemstone", 5))) {
      assertThat(maximize("0 mus, -tie"), is(true));
      assertThat(socketRecommendations("massive gemstone"), is(0L));
    }
  }

  @Test
  void honorsBooleanAndBitmapGemRequirements() {
    try (var cleanups =
        withWornCodpiece(
            withItem(ItemPool.ALIEN_GEMSTONE),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, ItemPool.ALIEN_GEMSTONE, "Adventure Underwater"))) {
      assertThat(maximize("adventure underwater, -tie"), is(true));
      assertThat(socketRecommendations("alien gemstone"), is(1L));
    }

    try (var cleanups =
        withWornCodpiece(
            withItem(ItemPool.ALIEN_GEMSTONE),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, ItemPool.ALIEN_GEMSTONE, "Surgeonosity: +1"))) {
      assertThat(maximize("1 surgeonosity, -tie"), is(true));
      assertThat(socketRecommendations("alien gemstone"), is(1L));
    }
  }

  @Test
  void allowsOwnedRestrictedGemCopiesButNotClosetCopies() {
    try (var cleanups =
        withWornCodpiece(
            withPath(Path.STANDARD),
            withRestricted(true),
            withNotAllowedInStandard(RestrictedItemType.ITEMS, CONTROL_CRYSTAL),
            withItem(CONTROL_CRYSTAL))) {
      assertThat(maximize("mys"), is(true));
      assertThat(socketRecommendations(CONTROL_CRYSTAL), is(1L));
    }

    try (var cleanups =
        withWornCodpiece(
            withPath(Path.STANDARD),
            withRestricted(true),
            withNotAllowedInStandard(RestrictedItemType.ITEMS, CONTROL_CRYSTAL),
            withProperty("autoSatisfyWithCloset", true),
            withItemInCloset(CONTROL_CRYSTAL))) {
      assertThat(maximize("mys"), is(true));
      assertThat(socketRecommendations(CONTROL_CRYSTAL), is(0L));
    }
  }

  @Test
  void retrievesScoreGemsFromAccessibleNonInventorySources() {
    int gemId = ItemDatabase.getItemId("massive gemstone");

    try (var cleanups =
        withWornCodpiece(
            withProperty("autoSatisfyWithCloset", true), withItemInCloset("massive gemstone"))) {
      assertThat(maximize("item drop, -tie"), is(true));
      assertThat(
          socketRecommendation("massive gemstone").getCmd(),
          startsWith("closet take 1 \u00B6" + gemId + ";equip codpiece"));
    }

    try (var cleanups =
        withWornCodpiece(
            withProperty("autoSatisfyWithStorage", true),
            withInteractivity(true),
            withItemInStorage("massive gemstone"))) {
      assertThat(maximize("item drop, -tie"), is(true));
      assertThat(
          socketRecommendation("massive gemstone").getCmd(),
          startsWith("pull 1 \u00B6" + gemId + ";equip codpiece"));
    }

    try (var clanCleanup = withClan(1, "Test Clan")) {
      boolean hadClan = KoLCharacter.hasClan();
      KoLCharacter.setClan(true);
      ClanManager.setStashRetrieved();
      try (var cleanups =
          withWornCodpiece(
              new Cleanups(() -> KoLCharacter.setClan(hadClan)),
              withProperty("autoSatisfyWithStash", true),
              withInteractivity(true),
              withItemInStash("massive gemstone"))) {
        assertThat(maximize("item drop, -tie"), is(true));
        assertThat(
            socketRecommendation("massive gemstone").getCmd(),
            startsWith("stash take 1 \u00B6" + gemId + ";equip codpiece"));
      }
    }
  }

  @Test
  void buysScoreGemsForSockets() {
    int gemId = ItemDatabase.getItemId("massive gemstone");
    try (var cleanups =
        withWornCodpiece(
            withMallPrice(gemId, 100),
            withMeat(500),
            withProperty("autoSatisfyWithMall", true),
            withInteractivity(true))) {
      assertThat(
          Maximizer.maximize(
              "item drop, -tie",
              100,
              PriceLevel.ALL,
              EquipScope.SPECULATE_ANY,
              EnumSet.allOf(KoLConstants.filterType.class)),
          is(true));

      assertThat(socketRecommendations("massive gemstone"), is(5L));
      assertThat(
          getBoosts().stream()
              .filter(boost -> SlotSet.CODPIECE_SLOTS.contains(boost.getSlot()))
              .filter(boost -> boost.getItem().getItemId() == gemId)
              .filter(boost -> boost.getCmd().startsWith("buy 1 \u00B6" + gemId))
              .count(),
          is(5L));
    }
  }

  @Test
  void craftsARequiredScoreGemForASocket() {
    int gemId = ItemDatabase.getItemId("brown crock marble");
    try (var cleanups =
        withWornCodpiece(withItem("green peawee marble", 2), withConcoctionRefresh())) {
      assertThat(
          Maximizer.maximize(
              "+equip brown crock marble, initiative, -tie",
              0,
              PriceLevel.DONT_CHECK,
              EquipScope.SPECULATE_ANY,
              EnumSet.allOf(KoLConstants.filterType.class)),
          is(true));

      assertThat(
          socketRecommendation("brown crock marble").getCmd(),
          startsWith("make \u00B6" + gemId + ";equip codpiece"));
    }
  }

  @Test
  void fillsEqualScoreGemsCanonicallyAroundInstalledSockets() {
    var baconstone = ItemPool.get("baconstone");
    var massiveGemstone = ItemPool.get("massive gemstone");
    var controlCrystal = ItemPool.get(CONTROL_CRYSTAL);
    try (var cleanups =
        withWornCodpiece(
            withEquipped(Slot.CODPIECE2, massiveGemstone),
            withEquipped(Slot.CODPIECE4, controlCrystal),
            withItem(baconstone),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, baconstone.getItemId(), "Item Drop: +10"),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, massiveGemstone.getItemId(), "Item Drop: +10"),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, controlCrystal.getItemId(), "Item Drop: +10"))) {
      assertThat(maximize("item drop, -tie"), is(true));

      assertThat(Maximizer.best.equipment.get(Slot.CODPIECE1), is(baconstone));
      assertThat(Maximizer.best.equipment.get(Slot.CODPIECE2), is(massiveGemstone));
      assertThat(Maximizer.best.equipment.get(Slot.CODPIECE3), is(EquipmentRequest.UNEQUIP));
      assertThat(Maximizer.best.equipment.get(Slot.CODPIECE4), is(controlCrystal));
      assertThat(Maximizer.best.equipment.get(Slot.CODPIECE5), is(EquipmentRequest.UNEQUIP));
    }
  }

  @Test
  void cappedCodpieceCategoryObjectiveFallsBackToUnprovenExhaustiveSearch() {
    try (var cleanups =
        withWornCodpiece(
            withProperty("maximizerCertificateMode", true),
            withProperty("maximizerMaximumScoreDifferencePoints", 0.0),
            withProperty("maximizerMaximumScoreDifferencePercent", 0.0),
            withItem(ItemPool.ALIEN_GEMSTONE),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, ItemPool.ALIEN_GEMSTONE, "Surgeonosity: +1"))) {
      assertThat(maximize("1 surgeonosity 1 max, -tie"), is(true));

      assertThat(socketRecommendations("alien gemstone"), is(1L));
      assertThat(Maximizer.best.equipment.get(Slot.CODPIECE1), is(ItemPool.get("alien gemstone")));
      assertThat(Maximizer.searchCertificate().frontierComplete(), is(false));
      assertThat(Maximizer.searchCertificate().achieved(), is(false));
      assertThat(Maximizer.searchCertificate().absoluteGap(), is(Double.NaN));
    }
  }

  @Test
  void cappedRequiredNegativeGemWithSmithsnessFallsBackWithoutFalseCertificate() {
    try (var cleanups =
        withWornCodpiece(
            withProperty("maximizerCertificateMode", true),
            withProperty("maximizerMaximumScoreDifferencePoints", 0.0),
            withProperty("maximizerMaximumScoreDifferencePercent", 0.0),
            withItem(ItemPool.ALIEN_GEMSTONE),
            withStats(100, 100, 100),
            withEquippableItem("A Light that Never Goes Out"),
            withEquippableItem("Hairpiece On Fire"),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, ItemPool.ALIEN_GEMSTONE, "Item Drop: -6"))) {
      assertThat(
          maximize(
              "item drop 4 max, +equip alien gemstone, +equip A Light that Never Goes Out, "
                  + "+equip Hairpiece On Fire, -tie"),
          is(true));

      assertThat(socketRecommendations("alien gemstone"), is(1L));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "A Light that Never Goes Out")));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "Hairpiece On Fire")));
      assertThat(
          Maximizer.best.equipment.get(Slot.OFFHAND),
          is(ItemPool.get("A Light that Never Goes Out")));
      assertThat(Maximizer.best.equipment.get(Slot.HAT), is(ItemPool.get("Hairpiece On Fire")));
      assertThat(Maximizer.best.equipment.get(Slot.CODPIECE1), is(ItemPool.get("alien gemstone")));
      assertThat(modFor(DoubleModifier.ITEMDROP), is(4.0));
      assertThat(Maximizer.searchCertificate().frontierComplete(), is(false));
      assertThat(Maximizer.searchCertificate().achieved(), is(false));
      assertThat(Maximizer.searchCertificate().absoluteGap(), is(Double.NaN));
    }
  }

  @Test
  void budgetBeforeALegalRequiredConfigurationReportsNoFeasibleResult() {
    try (var cleanups =
        withWornCodpiece(
            withProperty("maximizerTimeLimitSeconds", 0),
            withProperty("maximizerCombinationLimit", 1),
            withItem("massive gemstone", 5),
            withItem(ItemPool.ALIEN_GEMSTONE),
            withOverrideModifiers(
                ModifierType.ETERNITY_CODPIECE, ItemPool.ALIEN_GEMSTONE, "Adventure Underwater"))) {
      assertThat(maximize("adventure underwater, item drop, -tie"), is(false));
      assertThat(getBoosts(), hasItem(hasToString(containsString("no feasible equipment"))));
      assertThat(Maximizer.searchStatus().feasible(), is(false));
      assertThat(Maximizer.collectingAccessoryFrontier(), is(false));
    }
  }

  @Test
  void countsSocketSearchAgainstTheExistingCombinationLimit() {
    try (var cleanups =
        withWornCodpiece(
            withProperty("maximizerTimeLimitSeconds", 0),
            withProperty("maximizerCombinationLimit", 5),
            withStats(100, 100, 100),
            withItem(CONTROL_CRYSTAL),
            withItem("baconstone"),
            withItem("stone of eXtreme power"))) {
      maximize("mys");
      assertThat(getBoosts(), hasItem(hasToString(containsString("search budget reached"))));
    }
  }

  @Test
  void exactSocketSearchParticipatesInExactCertificates() {
    try (var cleanups =
        withWornCodpiece(
            withProperty("maximizerCertificateMode", true),
            withProperty("maximizerMaximumScoreDifferencePoints", 0.0),
            withProperty("maximizerMaximumScoreDifferencePercent", 0.0),
            withProperty("maximizerCombinationLimit", 1),
            withItem("massive gemstone"))) {
      assertThat(maximize("item drop, -tie"), is(true));
      assertThat(socketRecommendations("massive gemstone"), is(1L));
      var certificate = Maximizer.searchCertificate();
      assertThat(certificate.frontierComplete(), is(true));
      assertThat(certificate.toString(), certificate.achieved(), is(true));
      assertThat(certificate.absoluteGap(), is(0.0));
    }
  }

  @Test
  void recommendsAllAccessibleCopiesAcrossTheFiveSockets() {
    try (var cleanups =
        withWornCodpiece(
            withInteractivity(false),
            withItemInFreepulls(CONTROL_CRYSTAL, SlotSet.CODPIECE_SLOTS.size()))) {
      assertThat(maximize("mys"), is(true));
      assertThat(socketRecommendations(CONTROL_CRYSTAL), is(5L));
    }
  }

  @Test
  void canDisableCodpieceGemSearch() {
    try (var cleanups =
        withWornCodpiece(
            withProperty("maximizerConsiderCodpieceGems", false), withItem(CONTROL_CRYSTAL))) {
      assertThat(maximize("mys"), is(true));
      assertThat(socketRecommendations(CONTROL_CRYSTAL), is(0L));
    }
  }

  @Test
  void fixedModeDoesNotReserveHiddenSocketCopiesTwice() {
    var gem = ItemPool.get("incredibly dense meat gem");
    try (var cleanups =
        new Cleanups(
            withProperty("maximizerConsiderCodpieceGems", "fixed"),
            withItemInCloset(ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.CODPIECE1, gem),
            withItem(gem))) {
      assertThat(maximize("meat drop"), is(true));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.ACCESSORY1, gem.getName())));
    }
  }

  @Test
  void reusesAnInstalledGemWhenRetrievingTheCodpiece() {
    try (var cleanups =
        new Cleanups(
            withProperty("autoSatisfyWithCloset", true),
            withItemInCloset(ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.CODPIECE1, ItemPool.get(CONTROL_CRYSTAL)))) {
      assertThat(maximize("mys"), is(true));
      assertThat(getBoosts(), hasItem(recommends(ItemPool.THE_ETERNITY_CODPIECE)));
      assertThat(socketRecommendations(CONTROL_CRYSTAL), is(0L));
    }
  }

  @Test
  void movesAnInstalledGemBeforeUsingItAsAnAccessory() {
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
  void prunesAnExpansiveGemInventory() {
    try (var cleanups =
        withWornCodpiece(
            withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY, 20),
            withProperty("maximizerTimeLimitSeconds", 0),
            withProperty("maximizerCombinationLimit", 5_000))) {
      for (var entry : ModifierDatabase.getAllModifiersOfType(ModifierType.ETERNITY_CODPIECE)) {
        if (entry.getKey().isInt()) cleanups.add(withItem(entry.getKey().getIntValue(), 5));
      }

      assertThat(
          maximize(
              "item drop, familiar weight, -hat, -weapon, -offhand, -shirt, -pants, -acc2, -acc3, -familiar"),
          is(true));
      assertThat(Maximizer.bestChecked, org.hamcrest.Matchers.lessThan(5_000L));
    }
  }
}
