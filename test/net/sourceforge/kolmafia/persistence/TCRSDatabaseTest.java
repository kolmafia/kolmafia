package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TCRSDatabaseTest {

  private static Stream<Arguments> guesses() {
    // Tea Tree teas are useful because there are a lot of them, and they're all already in ItemPool
    return Stream.of(
        // Arguments.of(AscensionClass.SEAL_CLUBBER, ZodiacSign.MONGOOSE, 340, "nitrogenated wobbly
        // Knob Goblin steroids", "Effect: \"Protection from Bad Stuff\", Effect Duration: 23"),
        Arguments.of(
            AscensionClass.SEAL_CLUBBER,
            ZodiacSign.MONGOOSE,
            418,
            "enhanced quadruple-magnetized spinning maroon jittery Ferrigno's Elixir of Power",
            "Effect: \"Dances with Tweedles\", Effect Duration: 12"),
        Arguments.of(
            AscensionClass.SEAL_CLUBBER,
            ZodiacSign.MONGOOSE,
            422,
            "altered galvanized twirling huge potion of potency",
            "Effect: \"Buttermilk Boogie\", Effect Duration: 26"),
        Arguments.of(
            AscensionClass.SAUCEROR,
            ZodiacSign.MARMOT,
            ItemPool.PHIAL_OF_HOTNESS,
            "colloidal blue phial of hotness",
            "Effect: \"Lifted Spirits\", Effect Duration: 69"),
        Arguments.of(
            AscensionClass.TURTLE_TAMER,
            ZodiacSign.PLATYPUS,
            ItemPool.HALF_ORCHID,
            "galvanized narrow half-orchid",
            "Effect: \"Charrrming\", Effect Duration: 55"),
        Arguments.of(
            AscensionClass.SAUCEROR,
            ZodiacSign.OPOSSUM,
            ItemPool.VITALI_TEA,
            "super-modified ghostly cuppa Vitali tea",
            "Effect: \"Yes, Can Haz\", Effect Duration: 14"),
        Arguments.of(
            AscensionClass.ACCORDION_THIEF,
            ZodiacSign.MARMOT,
            ItemPool.MONSTROSI_TEA,
            "moist super-activated deionized cuppa Monstrosi tea",
            "Effect: \"Happy Salamander\", Effect Duration: 65"),
        Arguments.of(
            AscensionClass.ACCORDION_THIEF,
            ZodiacSign.MARMOT,
            ItemPool.IMPREGNABILI_TEA,
            "tarnished concentrated jittery cuppa Impregnabili tea",
            "Effect: \"Burning Ears\", Effect Duration: 69"),
        Arguments.of(
            AscensionClass.DISCO_BANDIT,
            ZodiacSign.MARMOT,
            ItemPool.CHARI_TEA,
            "boiled cuppa Chari tea",
            "Effect: \"Dancing Prowess\", Effect Duration: 54"),
        Arguments.of(
            AscensionClass.DISCO_BANDIT,
            ZodiacSign.WOMBAT,
            ItemPool.TOAST_TEA,
            "quadruple-dry boiled alkaline cuppa Toast tea",
            "Effect: \"Bestial Sympathy\", Effect Duration: 12"),
        Arguments.of(
            AscensionClass.ACCORDION_THIEF,
            ZodiacSign.BLENDER,
            ItemPool.BATTERY_CAR,
            "vacuum-sealed tumbling battery (car)",
            "Effect: \"Starry-Eyed\", Effect Duration: 24"),
        // Sauceror/Vole Spooky Powder rolls an overflow for effect and as such keeps its original
        // effect
        Arguments.of(
            AscensionClass.SAUCEROR,
            ZodiacSign.VOLE,
            1441 /* Spooky Powder */,
            "irradiated altered powder",
            "Effect: \"Spookypants\", Effect Duration: 69"),
        Arguments.of(
            AscensionClass.TURTLE_TAMER,
            ZodiacSign.MONGOOSE,
            ItemPool.MEDIOCRI_TEA,
            "concentrated corrupted colloidal cuppa Mediocri tea",
            "Effect: \"Night Vision\", Effect Duration: 55"),
        Arguments.of(
            AscensionClass.TURTLE_TAMER,
            ZodiacSign.PACKRAT,
            ItemPool.NEUROPLASTICI_TEA,
            "enhanced cuppa Neuroplastici tea",
            "Effect: \"Rat-Faced\", Effect Duration: 57"),
        Arguments.of(
            AscensionClass.DISCO_BANDIT,
            ZodiacSign.PACKRAT,
            ItemPool.LOYAL_TEA,
            "frozen wobbly cuppa Loyal tea",
            "Effect: \"Tiki Temerity\", Effect Duration: 29"));
  }

  @ParameterizedTest
  @MethodSource("guesses")
  void guessItem(
      final AscensionClass ascensionClass,
      final ZodiacSign sign,
      final int itemId,
      final String expectedName,
      final String expectedMods) {
    var item = TCRSDatabase.guessItem(ascensionClass, sign, itemId);
    assertThat(item.name, equalTo(expectedName));
    assertThat(item.modifiers, equalTo(expectedMods));
  }

  @AfterAll
  static void afterAll() throws IOException {
    ConsumablesDatabase.clearAndRebuild();
    try (var walker = Files.walk(KoLConstants.DATA_LOCATION.toPath())) {
      walker
          .map(java.nio.file.Path::toFile)
          .filter(f -> f.getName().startsWith("TCRS_"))
          .filter(f -> f.getName().endsWith(".txt"))
          .forEach(File::delete);
    }
  }

  @Test
  void guessAll() {
    for (var ascensionClass : AscensionClass.standardClasses) {
      for (var sign : ZodiacSign.standardZodiacSigns) {
        TCRSDatabase.load(ascensionClass, sign, true);
        for (var i : ItemDatabase.entrySet()) {
          var itemId = i.getKey();
          if (!TCRSDatabase.hasData(itemId)) continue;

          var dataSays = TCRSDatabase.getData(itemId);
          var weGuessed = TCRSDatabase.guessItem(ascensionClass, sign, itemId);

          if (weGuessed == null) {
            continue;
          }

          var checkMods =
              !TCRSDatabase.DYNAMICALLY_NAMED.contains(itemId)
                  && switch (ItemDatabase.getConsumptionType(itemId)) {
                    case USE, USE_INFINITE, USE_MULTIPLE, USE_MESSAGE_DISPLAY -> false;
                    default -> true;
                  };

          assertAll(
              String.format("[%s]%s in %s / %s", itemId, i.getValue(), ascensionClass, sign),
              () ->
                  assertThat(
                      "Name",
                      weGuessed.name,
                      equalTo(StringUtilities.getEntityDecode(dataSays.name))),
              () -> assertThat("Size", weGuessed.size, equalTo(dataSays.size)),
              () -> {
                if (dataSays.quality.getValue() > 0)
                  assertThat("Quality", weGuessed.quality, equalTo(dataSays.quality));
              },
              () -> {
                if (checkMods) {
                  var dataSaysMods = ModifierDatabase.splitModifiers(dataSays.modifiers);
                  var weGuessedMods = ModifierDatabase.splitModifiers(weGuessed.modifiers);

                  assertAll(
                      () ->
                          assertThat(
                              "Effect",
                              weGuessedMods.getModifierValue("Effect"),
                              equalTo(dataSaysMods.getModifierValue("Effect"))),
                      () -> {
                        // @TODO Queen cookie sometimes has no effect duration. Is this right?
                        if (dataSaysMods.containsModifier("Effect Duration")) {
                          assertThat(
                              "Effect Duration",
                              weGuessedMods.getModifierValue("Effect Duration"),
                              equalTo(dataSaysMods.getModifierValue("Effect Duration")));
                        }
                      });
                }
              });
        }
      }
    }
  }

  @Test
  public void campgroundItemsRetainModifiers() {
    var cleanups =
        new Cleanups(
            withPath(Path.CRAZY_RANDOM_SUMMER_TWO),
            withClass(AscensionClass.SEAL_CLUBBER),
            withSign(ZodiacSign.MONGOOSE));

    try (cleanups) {
      TCRSDatabase.loadTCRSData();
      Modifiers mods = ModifierDatabase.getModifiers(ModifierType.ITEM, ItemPool.MAID);
      assertThat(mods.getDouble(DoubleModifier.ADVENTURES), is(4.0));
    }
  }

  @Test
  public void chateauItemsRetainModifiers() {
    var cleanups =
        new Cleanups(
            withPath(Path.CRAZY_RANDOM_SUMMER_TWO),
            withClass(AscensionClass.SEAL_CLUBBER),
            withSign(ZodiacSign.MONGOOSE));

    try (cleanups) {
      TCRSDatabase.loadTCRSData();
      Modifiers mods = ModifierDatabase.getModifiers(ModifierType.ITEM, ItemPool.CHATEAU_SKYLIGHT);
      assertThat(mods.getDouble(DoubleModifier.ADVENTURES), is(3.0));
    }
  }

  @Test
  void someModifiersCarryOver() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withPath(Path.CRAZY_RANDOM_SUMMER_TWO),
            withClass(AscensionClass.SEAL_CLUBBER),
            withSign(ZodiacSign.MONGOOSE),
            withHttpClientBuilder(builder));

    client.addResponse(
        200, html("request/test_desc_item_tcrs_ring_of_telling_skeletons_what_to_do.html"));

    try (cleanups) {
      ModifierDatabase.resetModifiers();
      var ring = TCRSDatabase.deriveItem(ItemPool.RING_OF_TELLING_SKELETONS_WHAT_TO_DO);
      assertThat(ring, not(nullValue()));
      assertThat(
          ring.modifiers,
          equalTo(
              "Hot Resistance: +1, Conditional Skill (Equipped): \"Tell a Skeleton What To Do\", Conditional Skill (Equipped): \"Tell This Skeleton What To Do\""));
    }
  }
}
