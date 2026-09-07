package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.getBoosts;
import static internal.helpers.Maximizer.maximize;
import static internal.helpers.Maximizer.maximizeAny;
import static internal.helpers.Player.withAdventuresLeft;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withHardcore;
import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withItemInCloset;
import static internal.helpers.Player.withOutfit;
import static internal.helpers.Player.withOverrideModifiers;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSign;
import static internal.helpers.Player.withSkill;
import static internal.helpers.Player.withStats;
import static internal.matchers.Maximizer.recommends;
import static internal.matchers.Maximizer.recommendsEffect;
import static internal.matchers.Maximizer.recommendsSlot;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import java.util.EnumSet;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.swingui.MaximizerFrame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class MaximizerCharacterizationTest {
  @BeforeAll
  static void resetCharacter() {
    KoLCharacter.reset("MaximizerCharacterizationTest");
    Preferences.reset("MaximizerCharacterizationTest");
  }

  private static void maximizeAll(String expression) {
    MaximizerFrame.expressionSelect.setSelectedItem(expression);
    Maximizer.maximize(
        EquipScope.SPECULATE_INVENTORY,
        0,
        PriceLevel.DONT_CHECK,
        true,
        EnumSet.allOf(KoLConstants.filterType.class));
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "all resistance | All Resistance: +1",
        "elemental damage | Cold Damage: +1",
        "organ capacity | Stomach Capacity: +1",
        "crit | Critical Hit Percent: +1",
        "spell crit | Spell Critical Percent: +1",
        "sprinkle | Sprinkle Drop: +1",
        "stomach | Stomach Capacity: +1",
        "liver | Liver Capacity: +1",
        "spleen | Spleen Capacity: +1",
        "ocrs | Random Monster Modifiers: +1",
        "weapon dmg percent | Weapon Damage Percent: +1",
        "organ | Stomach Capacity: +1",
        "mys exp perc | Experience Percent (Mysticality): +1",
        "mys exp | Experience (Mysticality): +1",
        "mys perc | Mysticality Percent: +1",
        "mox exp perc | Experience Percent (Moxie): +1",
        "mox exp | Experience (Moxie): +1",
        "mox perc | Moxie Percent: +1",
        "\"item drop\" | Item Drop: +1"
      })
  void recognizesModifierAliases(String expression, String modifiers) {
    try (var cleanups =
        new Cleanups(
            withOverrideModifiers(ModifierType.ITEM, ItemPool.HELMET_TURTLE, modifiers),
            withEquippableItem(ItemPool.HELMET_TURTLE))) {
      assertTrue(maximize(expression + ", -tie"));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
    }
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {"utensil | pasta spoon", "knife | asparagus knife", "accordion | aerogel accordion"})
  void honorsWeaponRequirements(String expression, String itemName) {
    try (var cleanups = new Cleanups(withStats(100, 100, 100), withEquippableItem(itemName))) {
      assertTrue(maximize(expression + ", -tie"));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, itemName)));
    }
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "type sword | lupine sword | seal-clubbing club",
        "2 hands | stone banjo | seal-clubbing club",
        "melee | seal-clubbing club | disco ball",
        "-melee | disco ball | seal-clubbing club"
      })
  void weaponQualifiersRestrictScoredRecommendations(
      String qualifier, String expected, String alternative) {
    int expectedId = ItemPool.get(expected).getItemId();
    int alternativeId = ItemPool.get(alternative).getItemId();
    try (var cleanups =
        new Cleanups(
            withStats(100, 100, 100),
            withOverrideModifiers(ModifierType.ITEM, expectedId, "Item Drop: +10"),
            withOverrideModifiers(ModifierType.ITEM, alternativeId, "Item Drop: +10"),
            withEquippableItem(expected),
            withEquippableItem(alternative))) {
      assertTrue(maximize("item, " + qualifier + ", -tie"));

      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, expected)));
    }
  }

  @ParameterizedTest
  @CsvSource({"ACCORDION_THIEF, true", "SEAL_CLUBBER, false"})
  void twoHandedAccordionRequirementRespectsClass(
      AscensionClass ascensionClass, boolean recommended) {
    try (var cleanups =
        new Cleanups(
            withClass(ascensionClass),
            withStats(100, 100, 100),
            withEquippableItem("stolen accordion"))) {
      assertTrue(maximize("accordion, -tie"));

      assertThat(
          getBoosts(),
          recommended
              ? hasItem(recommendsSlot(Slot.WEAPON, "stolen accordion"))
              : not(hasItem(recommends("stolen accordion"))));
    }
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "Scalp of Gorgolok | SEAL_CLUBBER | TURTLE_TAMER | muscle",
        "Elder Turtle Shell | TURTLE_TAMER | SEAL_CLUBBER | muscle",
        "Colander of Em-er'il | PASTAMANCER | SEAL_CLUBBER | mysticality",
        "Ancient Saucehelm | SAUCEROR | SEAL_CLUBBER | mysticality",
        "Disco 'Fro Pick | DISCO_BANDIT | SEAL_CLUBBER | moxie",
        "El Sombrero De Lopez | ACCORDION_THIEF | SEAL_CLUBBER | moxie"
      })
  void classRestrictedEquipmentIsRecommendedOnlyToItsClass(
      String itemName, AscensionClass requiredClass, AscensionClass otherClass, String expression) {
    try (var cleanups =
        new Cleanups(
            withClass(requiredClass), withStats(100, 100, 100), withEquippableItem(itemName))) {
      assertTrue(maximize(expression + ", -tie"));
      assertThat(getBoosts(), hasItem(recommends(itemName)));
    }

    try (var cleanups =
        new Cleanups(
            withClass(otherClass), withStats(100, 100, 100), withEquippableItem(itemName))) {
      assertTrue(maximize(expression + ", -tie"));
      assertThat(getBoosts(), not(hasItem(recommends(itemName))));
    }
  }

  @Test
  void allResistanceDoesNotScoreIndividualResistanceModifiers() {
    try (var cleanups =
        new Cleanups(
            withOverrideModifiers(ModifierType.ITEM, ItemPool.HELMET_TURTLE, "Cold Resistance: +1"),
            withEquippableItem(ItemPool.HELMET_TURTLE))) {
      assertTrue(maximize("all resistance, -tie"));

      assertThat(getBoosts(), contains(hasToString(containsString("nothing useful found"))));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"Cold", "Hot", "Sleaze", "Spooky", "Stench"})
  void elementalImmunityAndVulnerabilityAffectResistanceRecommendations(String element) {
    String expression = element.toLowerCase() + " resistance, -tie";
    int alternative = ItemPool.get("bounty-hunting helmet").getItemId();
    try (var cleanups =
        new Cleanups(
            withOverrideModifiers(ModifierType.ITEM, ItemPool.HELMET_TURTLE, element + " Immunity"),
            withOverrideModifiers(ModifierType.ITEM, alternative, element + " Resistance: +99"),
            withEquippableItem(ItemPool.HELMET_TURTLE),
            withEquippableItem(alternative))) {
      assertTrue(maximize(expression));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
      assertThat(getBoosts(), not(hasItem(recommends("bounty-hunting helmet"))));
    }

    try (var cleanups =
        new Cleanups(
            withOverrideModifiers(
                ModifierType.ITEM,
                ItemPool.HELMET_TURTLE,
                element + " Resistance: +200, " + element + " Vulnerability"),
            withOverrideModifiers(ModifierType.ITEM, alternative, element + " Resistance: +101"),
            withEquippableItem(ItemPool.HELMET_TURTLE),
            withEquippableItem(alternative))) {
      assertTrue(maximize(expression));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "bounty-hunting helmet")));
      assertThat(getBoosts(), not(hasItem(recommends("helmet turtle"))));
    }
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "nonsense | Unrecognized keyword: nonsense",
        "item,, | Unable to interpret: ,",
        "outfit not an outfit | Unknown or custom outfit: not an outfit",
        "switch not a familiar | Unknown familiar: not a familiar"
      })
  void reportsInvalidExpressions(String expression, String error) {
    assertFalse(maximize(expression));
    assertThat(KoLmafia.lastMessage, is(error));
  }

  @Test
  void coldPlumberExplainsWhyItCannotRecommendEquipment() {
    try (var cleanups =
        new Cleanups(
            withClass(AscensionClass.PLUMBER),
            withPath(Path.PATH_OF_THE_PLUMBER),
            withEquippableItem("work boots"),
            withEquippableItem("frosty button"))) {
      assertFalse(maximize("cold plumber"));
      assertThat(KoLmafia.lastMessage, is("You don't have an appropriate flower to wield"));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "SEAL_CLUBBER, Silent Hunting, Nearly Silent Hunting",
    "TURTLE_TAMER, Nearly Silent Hunting, Silent Hunting"
  })
  void silentHunterRecommendationDependsOnCharacterClass(
      AscensionClass ascensionClass, String expected, String unavailable) {
    try (var cleanups = new Cleanups(withClass(ascensionClass), withSkill("Silent Hunter"))) {
      assertTrue(maximize("initiative, -tie"));

      assertThat(getBoosts(), hasItem(recommendsEffect(expected)));
      assertThat(getBoosts(), not(hasItem(recommendsEffect(unavailable))));
    }
  }

  @Test
  void showAllExplainsUnavailableGlobalOptions() {
    try (var cleanups =
        new Cleanups(withSign(ZodiacSign.NONE), withProperty("horseryAvailable", false))) {
      maximizeAll("-combat, meat drop, monster level");

      assertThat(
          getBoosts(), hasItem(hasToString(containsString("get a horsery and ride a dark horse"))));
      assertThat(
          getBoosts(),
          hasItem(hasToString(containsString("BoomBox and play Total Eclipse of Your Meat"))));
      assertThat(
          getBoosts(),
          hasItem(hasToString(containsString("ascend into a non-Bad Moon sign and mcd 10"))));
    }
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "muscle percent | get an Eight Days a Week Pill Keeper",
        "muscle percent | unlock Boxing Daycare and visit spa for Muddled",
        "all resistance | unlock Spacegate and vaccine 1 for Rainbow Vaccine",
        "initiative | get a Grim Brother familiar for Soles of Glass",
        "item drop | acquire a pair of Cargo Cultist Shorts for Finding Stuff",
        "item drop | acquire Deck of Every Card for Fortune of the Wheel",
        "item drop | install Source Terminal for items.enh",
        "item drop | acquire and equip Greatest American Pants for Super Vision",
        "item drop | install Asdon Martin for Driving Observantly",
        "muscle percent | acquire protonic accelerator pack and crossstreams for Total Protonic Reversal",
        "weapon damage percent | acquire a Beach Comb or a driftwood beach comb for Lack of Body-Building",
        "-combat | get access to the VIP lounge",
        "-combat | acquire a cursed monkey's paw"
      })
  void showAllExplainsUnavailableEffectSources(String expression, String explanation) {
    maximizeAll(expression);

    assertThat(getBoosts(), hasItem(hasToString(containsString(explanation))));
  }

  @Test
  void showAllExplainsUnavailableSkillSource() {
    try (var cleanups = new Cleanups(withClass(AscensionClass.SEAL_CLUBBER))) {
      maximizeAll("initiative");

      assertThat(
          getBoosts(), hasItem(hasToString(containsString("learn to cast 1 Silent Hunter"))));
    }
  }

  @Test
  void showAllReportsWhenBeneficialEffectHasNoKnownSource() {
    maximizeAll("item drop");

    assertThat(
        getBoosts(), hasItem(hasToString(containsString("no known source of Shadow Waters"))));
  }

  @Test
  void noAdventuresPreferenceSuppressesAdventureCostEffects() {
    try (var cleanups =
        new Cleanups(
            withAdventuresLeft(3),
            withItem(ItemPool.GONG),
            withProperty("maximizerNoAdventures", false))) {
      assertTrue(maximize("item drop"));
      assertThat(getBoosts(), hasItem(hasProperty("cmd", is("gong roach itemdrop"))));

      Preferences.setBoolean("maximizerNoAdventures", true);
      assertTrue(maximize("item drop"));
      assertThat(getBoosts(), not(hasItem(hasProperty("cmd", is("gong roach itemdrop")))));
    }
  }

  @Test
  void dailyUsePreferenceSuppressesExhaustedEffectSource() {
    try (var cleanups =
        new Cleanups(
            withEquipped(Slot.PANTS, ItemPool.GREAT_PANTS), withProperty("_gapBuffs", 4))) {
      assertTrue(maximize("item drop"));
      assertThat(getBoosts(), hasItem(hasProperty("cmd", is("gap vision"))));

      Preferences.setInteger("_gapBuffs", 5);
      assertTrue(maximize("item drop"));
      assertThat(getBoosts(), not(hasItem(hasProperty("cmd", is("gap vision")))));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"current", "-current"})
  void explicitCurrentKeywordsKeepEquallyScoredEquippedItem(String currentKeyword) {
    int alternative = ItemPool.get("bounty-hunting helmet").getItemId();
    try (var cleanups =
        new Cleanups(
            withOverrideModifiers(ModifierType.ITEM, ItemPool.HELMET_TURTLE, "Item Drop: +10"),
            withOverrideModifiers(ModifierType.ITEM, alternative, "Item Drop: +10"),
            withEquipped(Slot.HAT, ItemPool.HELMET_TURTLE),
            withEquippableItem(alternative))) {
      assertTrue(maximize("item drop, -tie, " + currentKeyword));
      assertThat(getBoosts(), hasItem(hasToString(containsString("keep hat: helmet turtle"))));
    }
  }

  @Test
  void outfitWithoutANameKeepsTheCurrentlyWornOutfit() {
    try (var cleanups = new Cleanups(withOutfit(OutfitPool.WAR_FRAT_OUTFIT))) {
      KoLCharacter.recalculateAdjustments();

      assertTrue(maximize("+outfit, -tie"));
      assertThat(getBoosts(), hasItem(hasToString(containsString("keep hat: beer helmet"))));
      assertThat(
          getBoosts(), hasItem(hasToString(containsString("keep pants: distressed denim pants"))));
      assertThat(
          getBoosts(), hasItem(hasToString(containsString("keep acc1: bejeweled pledge pin"))));
    }
  }

  @Test
  void negativeFamiliarSwitchAfterPositiveSwitchIsIgnored() {
    try (var cleanups =
        new Cleanups(
            withFamiliarInTerrarium(FamiliarPool.BABY_GRAVY_FAIRY),
            withFamiliarInTerrarium(FamiliarPool.LEPRECHAUN))) {
      assertTrue(maximize("switch Baby Gravy Fairy, -switch Leprechaun, item drop"));

      assertThat(getBoosts(), hasItem(hasProperty("cmd", is("familiar Baby Gravy Fairy"))));
      assertThat(getBoosts(), not(hasItem(hasProperty("cmd", is("familiar Leprechaun")))));
    }
  }

  @Test
  void moxiePlumberPrefersFancyBoots() {
    try (var cleanups =
        new Cleanups(
            withClass(AscensionClass.PLUMBER),
            withPath(Path.PATH_OF_THE_PLUMBER),
            withStats(10, 10, 20),
            withEquippableItem("fancy boots"),
            withEquippableItem("work boots"))) {
      assertTrue(maximize("plumber, moxie, -tie"));

      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.ACCESSORY1, "fancy boots")));
    }
  }

  @Test
  void recommendsWeaponAndOffhandSynergy() {
    try (var cleanups =
        new Cleanups(
            withStats(100, 100, 100),
            withEquippableItem("lupine sword"),
            withEquippableItem("snarling wolf shield"))) {
      assertTrue(maximize("spooky damage, -tie"));

      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "lupine sword")));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "snarling wolf shield")));
    }
  }

  @Test
  void recommendsThreeAccessorySynergy() {
    try (var cleanups =
        new Cleanups(
            withStats(100, 100, 100),
            withEquippableItem("monstrous monocle"),
            withEquippableItem("musty moccasins"),
            withEquippableItem("molten medallion"))) {
      assertTrue(maximize("item drop, -tie"));

      assertThat(getBoosts(), hasItem(recommends("monstrous monocle")));
      assertThat(getBoosts(), hasItem(recommends("musty moccasins")));
      assertThat(getBoosts(), hasItem(recommends("molten medallion")));
    }
  }

  @ParameterizedTest
  @CsvSource({"DISCO_BANDIT, true", "SEAL_CLUBBER, false"})
  void doubleBarreledAvailabilityDependsOnClass(AscensionClass ascensionClass, boolean available) {
    try (var cleanups =
        new Cleanups(withClass(ascensionClass), withProperty("barrelShrineUnlocked", true))) {
      assertTrue(maximize("ranged damage percent, -tie"));

      assertThat(
          getBoosts(),
          available
              ? hasItem(hasProperty("cmd", is("barrelprayer buff")))
              : not(hasItem(hasProperty("cmd", is("barrelprayer buff")))));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "Extra-Loud Muffler, combat, Unmuffled, Muffled",
    "Extra-Quiet Muffler, -combat, Muffled, Unmuffled"
  })
  void motorbikeMufflerControlsRevEngineEffect(
      String muffler, String expression, String expected, String unavailable) {
    try (var cleanups =
        new Cleanups(
            withClass(AscensionClass.AVATAR_OF_SNEAKY_PETE),
            withPath(Path.AVATAR_OF_SNEAKY_PETE),
            withSkill("Rev Engine"),
            withProperty("peteMotorbikeMuffler", muffler))) {
      assertTrue(maximize(expression + ", -tie"));

      assertThat(getBoosts(), hasItem(recommendsEffect(expected)));
      assertThat(getBoosts(), not(hasItem(recommendsEffect(unavailable))));
    }
  }

  @Test
  void turtleTamerCanChangeBlessing() {
    try (var cleanups =
        new Cleanups(
            withClass(AscensionClass.TURTLE_TAMER), withSkill("Blessing of She-Who-Was"))) {
      assertTrue(maximize("mysticality, -tie"));

      assertThat(getBoosts(), hasItem(recommendsEffect("Blessing of She-Who-Was")));
    }
  }

  @Test
  void turtleTamerCanGainBoonMatchingCurrentBlessing() {
    try (var cleanups =
        new Cleanups(
            withClass(AscensionClass.TURTLE_TAMER),
            withSkill("Spirit Boon"),
            withEffect("Blessing of She-Who-Was"))) {
      assertTrue(maximize("weapon damage, -tie"));

      assertThat(getBoosts(), hasItem(recommendsEffect("Boon of She-Who-Was")));
    }
  }

  @Test
  void turtleTamerCanBecomeAvatarFromGloriousBlessing() {
    try (var cleanups =
        new Cleanups(
            withClass(AscensionClass.TURTLE_TAMER),
            withSkill("Turtle Power"),
            withEffect("Glorious Blessing of She-Who-Was"))) {
      assertTrue(maximize("spell damage percent, -tie"));

      assertThat(getBoosts(), hasItem(recommendsEffect("Avatar of She-Who-Was")));
    }
  }

  @Test
  void crownAndBjornUseDifferentFamiliars() {
    try (var cleanups =
        new Cleanups(
            withEquippableItem("Crown of Thrones"),
            withEquippableItem("Buddy Bjorn"),
            withFamiliarInTerrarium(FamiliarPool.LOBSTER),
            withFamiliarInTerrarium(FamiliarPool.GALLOPING_GRILL))) {
      assertTrue(maximize("spell damage, -tie"));

      assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("enthrone Galloping Grill"))));
      assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("bjornify Rock Lobster"))));
    }
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "Boris's Helm | AVATAR_OF_BORIS | AVATAR_OF_BORIS",
        "right bear arm | ZOMBIE_SLAYER | ZOMBIE_MASTER",
        "Jarlsberg's pan | AVATAR_OF_JARLSBERG | AVATAR_OF_JARLSBERG",
        "Sneaky Pete's leather jacket | AVATAR_OF_SNEAKY_PETE | AVATAR_OF_SNEAKY_PETE",
        "Thor's Pliers | HEAVY_RAINS | SEAL_CLUBBER",
        "The Crown of Ed the Undying | ACTUALLY_ED_THE_UNDYING | ED"
      })
  void hardcorePathEquipmentIsRecommendedOnlyInItsPath(
      String itemName, Path path, AscensionClass ascensionClass) {
    int itemId = ItemPool.get(itemName).getItemId();
    try (var cleanups =
        new Cleanups(
            withClass(AscensionClass.SEAL_CLUBBER),
            withHardcore(),
            withSkill("Torso Awareness"),
            withStats(1000, 1000, 1000),
            withOverrideModifiers(ModifierType.ITEM, itemId, "Item Drop: +100"),
            withEquippableItem(itemId))) {
      assertTrue(maximize("item drop, -tie"));
      assertThat(getBoosts(), not(hasItem(recommends(itemName))));
    }

    try (var cleanups =
        new Cleanups(
            withPath(path),
            withClass(ascensionClass),
            withHardcore(),
            withSkill("Torso Awareness"),
            withStats(1000, 1000, 1000),
            withOverrideModifiers(ModifierType.ITEM, itemId, "Item Drop: +100"),
            withEquippableItem(itemId))) {
      assertTrue(maximize("item drop, -tie"));
      assertThat(getBoosts(), hasItem(recommends(itemName)));
    }
  }

  @Test
  void chargedGarbageShirtBeatsAHigherUnchargedExperienceModifier() {
    int alternative = ItemPool.get("astral shirt").getItemId();
    try (var cleanups =
        new Cleanups(
            withSkill("Torso Awareness"),
            withProperty("garbageShirtCharge", 1),
            withOverrideModifiers(ModifierType.ITEM, alternative, "Experience: +4"),
            withEquippableItem(alternative),
            withEquippableItem(ItemPool.MAKESHIFT_GARBAGE_SHIRT))) {
      assertTrue(maximize("experience, -tie"));

      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.SHIRT, "makeshift garbage shirt")));
    }

    try (var cleanups =
        new Cleanups(
            withSkill("Torso Awareness"),
            withProperty("garbageShirtCharge", 0),
            withProperty("_garbageItemChanged", true),
            withOverrideModifiers(ModifierType.ITEM, alternative, "Experience: +4"),
            withEquippableItem(alternative),
            withEquippableItem(ItemPool.MAKESHIFT_GARBAGE_SHIRT))) {
      assertTrue(maximize("experience, -tie"));

      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.SHIRT, "astral shirt")));
    }
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "Mad Hatrack | asbestos helmet turtle | bounty-hunting helmet",
        "Fancypants Scarecrow | swashbuckling pants | Pantsgiving"
      })
  void familiarCanWearItsSpecialEquipment(String familiarName, String itemName, String forcedItem) {
    int itemId = ItemPool.get(itemName).getItemId();
    int forcedItemId = ItemPool.get(forcedItem).getItemId();
    try (var cleanups =
        new Cleanups(
            withFamiliar(FamiliarDatabase.getFamiliarId(familiarName), 400),
            withOverrideModifiers(ModifierType.ITEM, forcedItemId, "Item Drop: +20"),
            withEquippableItem(itemId),
            withEquippableItem(forcedItemId))) {
      assertTrue(maximize("item drop, +equip " + forcedItem + ", -tie"));

      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.FAMILIAR, itemName)));
    }
  }

  @Test
  void recommendationIncludesClosetRetrievalCommand() {
    try (var cleanups =
        new Cleanups(
            withOverrideModifiers(ModifierType.ITEM, ItemPool.HELMET_TURTLE, "Item Drop: +10"),
            withProperty("autoSatisfyWithCloset", true),
            withInteractivity(true),
            withItemInCloset(ItemPool.HELMET_TURTLE))) {
      maximizeAny("item drop, -tie");

      assertThat(
          getBoosts(),
          hasItem(
              hasProperty(
                  "cmd",
                  startsWith("closet take 1 \u00B6" + ItemPool.HELMET_TURTLE + ";equip hat"))));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"Drops Items", "Drops Meat"})
  void defaultTiebreakerPrefersSpecialEquipment(String specialModifier) {
    try (var cleanups =
        new Cleanups(
            withOverrideModifiers(
                ModifierType.ITEM, ItemPool.HELMET_TURTLE, "Item Drop: +10, " + specialModifier),
            withOverrideModifiers(
                ModifierType.ITEM,
                ItemPool.get("bounty-hunting helmet").getItemId(),
                "Item Drop: +10"),
            withEquippableItem(ItemPool.HELMET_TURTLE),
            withEquippableItem("bounty-hunting helmet"))) {
      assertTrue(maximize("item drop"));

      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
    }
  }

  @Test
  void defaultTiebreakerPrefersEquipmentWithARolloverEffect() {
    int oldSweatpants = ItemPool.OLD_SWEATPANTS;
    try (var cleanups =
        new Cleanups(
            withOverrideModifiers(
                ModifierType.ITEM, oldSweatpants, "Adventures: +2, PvP Fights: +2"),
            withEquippableItem("ninjammies"),
            withEquippableItem(oldSweatpants))) {
      assertTrue(maximize("1 bonus ninjammies, 1 bonus old sweatpants"));

      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "ninjammies")));
    }
  }

  @Test
  void doubleFistedSkillCanPutRangedWeaponsInBothHands() {
    try (var cleanups =
        new Cleanups(
            withSkill("Double-Fisted Skull Smashing"),
            withOverrideModifiers(
                ModifierType.ITEM, ItemPool.get("disco ball").getItemId(), "Item Drop: +10"),
            withEquippableItem("disco ball", 2))) {
      assertTrue(maximize("item drop, -tie"));

      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "disco ball")));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "disco ball")));
    }
  }

  @Test
  void hatTrickDoesNotRecommendTheNormalHatSlot() {
    try (var cleanups =
        new Cleanups(
            withClass(AscensionClass.SEAL_CLUBBER),
            withPath(Path.HAT_TRICK),
            withOverrideModifiers(ModifierType.ITEM, ItemPool.HELMET_TURTLE, "Item Drop: +10"),
            withEquippableItem(ItemPool.HELMET_TURTLE))) {
      assertTrue(maximize("item drop, -tie"));

      assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT))));
    }
  }

  @Test
  void speculativeSearchLeavesEquippedItemsUnchanged() {
    var equipped = ItemPool.get("helmet turtle");
    try (var cleanups =
        new Cleanups(
            withEquipped(Slot.HAT, equipped), withEquippableItem("bounty-hunting helmet"))) {
      assertTrue(maximize("item"));

      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "bounty-hunting helmet")));
      assertThat(EquipmentManager.getEquipment(Slot.HAT), is(equipped));
    }
  }

  @Test
  void emptyKeywordRecommendsKeepingOccupiedSlots() {
    try (var cleanups = new Cleanups(withEquipped(Slot.HAT, "helmet turtle"))) {
      assertTrue(maximize("empty"));
      assertThat(getBoosts(), contains(hasToString(containsString("keep hat: helmet turtle"))));
    }
  }
}
