package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.Objects;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase.ConsumableQuality;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TCRSDatabaseTest {

  private static Stream<Arguments> guesses() {
    return Stream.of(
        Arguments.of(
            AscensionClass.SEAL_CLUBBER,
            ZodiacSign.MONGOOSE,
            ItemPool.FERRIGNOS_ELIXIR_OF_POWER,
            "enhanced quadruple-magnetized spinning maroon jittery Ferrigno's Elixir of Power",
            "Dances with Tweedles",
            12),
        Arguments.of(
            AscensionClass.SEAL_CLUBBER,
            ZodiacSign.MONGOOSE,
            ItemPool.POTION_OF_POTENCY,
            "altered galvanized twirling huge potion of potency",
            "Buttermilk Boogie",
            26),
        Arguments.of(
            AscensionClass.SAUCEROR,
            ZodiacSign.MARMOT,
            ItemPool.PHIAL_OF_HOTNESS,
            "colloidal blue phial of hotness",
            "Lifted Spirits",
            69),
        Arguments.of(
            AscensionClass.TURTLE_TAMER,
            ZodiacSign.PLATYPUS,
            ItemPool.HALF_ORCHID,
            "galvanized narrow half-orchid",
            "Charrrming",
            55),
        Arguments.of(
            AscensionClass.SAUCEROR,
            ZodiacSign.OPOSSUM,
            ItemPool.VITALI_TEA,
            "super-modified ghostly cuppa Vitali tea",
            "Yes, Can Haz",
            14),
        Arguments.of(
            AscensionClass.ACCORDION_THIEF,
            ZodiacSign.MARMOT,
            ItemPool.MONSTROSI_TEA,
            "moist super-activated deionized cuppa Monstrosi tea",
            "Happy Salamander",
            65),
        Arguments.of(
            AscensionClass.ACCORDION_THIEF,
            ZodiacSign.MARMOT,
            ItemPool.IMPREGNABILI_TEA,
            "tarnished concentrated jittery cuppa Impregnabili tea",
            "Burning Ears",
            69),
        Arguments.of(
            AscensionClass.DISCO_BANDIT,
            ZodiacSign.MARMOT,
            ItemPool.CHARI_TEA,
            "boiled cuppa Chari tea",
            "Dancing Prowess",
            54),
        Arguments.of(
            AscensionClass.DISCO_BANDIT,
            ZodiacSign.WOMBAT,
            ItemPool.TOAST_TEA,
            "quadruple-dry boiled alkaline cuppa Toast tea",
            "Bestial Sympathy",
            12),
        Arguments.of(
            AscensionClass.ACCORDION_THIEF,
            ZodiacSign.BLENDER,
            ItemPool.BATTERY_CAR,
            "vacuum-sealed tumbling battery (car)",
            "Starry-Eyed",
            24),
        // Sauceror/Vole Spooky Powder rolls an overflow for effect, which resolves to the last pool
        // effect (Tiki Temerity)
        Arguments.of(
            AscensionClass.SAUCEROR,
            ZodiacSign.VOLE,
            ItemPool.SPOOKY_POWDER,
            "irradiated altered powder",
            "Tiki Temerity",
            69),
        Arguments.of(
            AscensionClass.TURTLE_TAMER,
            ZodiacSign.MONGOOSE,
            ItemPool.MEDIOCRI_TEA,
            "concentrated corrupted colloidal cuppa Mediocri tea",
            "Night Vision",
            55),
        Arguments.of(
            AscensionClass.TURTLE_TAMER,
            ZodiacSign.PACKRAT,
            ItemPool.NEUROPLASTICI_TEA,
            "enhanced cuppa Neuroplastici tea",
            "Rat-Faced",
            57),
        Arguments.of(
            AscensionClass.DISCO_BANDIT,
            ZodiacSign.PACKRAT,
            ItemPool.LOYAL_TEA,
            "frozen wobbly cuppa Loyal tea",
            "Tiki Temerity",
            29));
  }

  @ParameterizedTest
  @MethodSource("guesses")
  void guessItem(
      final AscensionClass ascensionClass,
      final ZodiacSign sign,
      final int itemId,
      final String expectedName,
      final String expectedEffect,
      final int expectedDuration) {
    var item = TCRSDatabase.guessItem(ascensionClass, sign, itemId);
    assertThat(item.name, equalTo(expectedName));
    var modifiers = ModifierDatabase.splitModifiers(item.modifiers);
    assertThat(modifiers.getModifierValue("Effect"), equalTo("\"" + expectedEffect + "\""));
    assertThat(
        modifiers.getModifierValue("Effect Duration"), equalTo(String.valueOf(expectedDuration)));
  }

  @AfterEach
  void afterEach() {
    TCRSDatabase.resetModifiers();
  }

  @Test
  public void enchantCountCorrect() {
    var cleanups =
        new Cleanups(
            withPath(Path.CRAZY_RANDOM_SUMMER_TWO),
            withClass(AscensionClass.SEAL_CLUBBER),
            withSign(ZodiacSign.MONGOOSE));
    try (cleanups) {
      TCRSDatabase.loadTCRSData(false);
      assertThat(TCRSDatabase.enchantCount(ItemPool.ASSHAT), equalTo(2));
    }
  }

  @Test
  void guessAll() throws java.io.IOException {
    // The full sweep produces a lot of mismatches while the branch is a work in progress, so stream
    // them to a file rather than holding them all in memory (which otherwise exhausts the heap).
    var reportFile =
        java.nio.file.Path.of(System.getProperty("user.dir"))
            .getParent()
            .getParent()
            .resolve("build/tcrs-guessAll-mismatches.txt");
    java.nio.file.Files.createDirectories(reportFile.getParent());

    var count = 0;
    try (var out = java.nio.file.Files.newBufferedWriter(reportFile)) {
      for (var ascensionClass : AscensionClass.standardClasses) {
        for (var sign : ZodiacSign.standardZodiacSigns) {
          var cleanups =
              new Cleanups(
                  withPath(Path.CRAZY_RANDOM_SUMMER_TWO),
                  withClass(ascensionClass),
                  withSign(sign));
          try (cleanups) {
            TCRSDatabase.loadTCRSData(false);
            for (var i : ItemDatabase.entrySet()) {
              var itemId = i.getKey();
              if (!TCRSDatabase.hasData(itemId)) continue;

              var dataSays = TCRSDatabase.getData(itemId);
              var weGuessed = TCRSDatabase.guessItem(ascensionClass, sign, itemId);

              if (weGuessed == null) {
                continue;
              }

              var checkMods =
                  !TCRSDatabase.NOT_RE_ROLLED.contains(itemId)
                      && switch (ItemDatabase.getConsumptionType(itemId)) {
                        case USE, USE_INFINITE, USE_MULTIPLE, USE_MESSAGE_DISPLAY -> false;
                        default -> true;
                      };

              var prefix =
                  String.format("[%s]%s in %s / %s", itemId, i.getValue(), ascensionClass, sign);

              var expectedName = StringUtilities.getEntityDecode(dataSays.name);
              if (!weGuessed.name.equals(expectedName)) {
                // NOT_RE_ROLLED items have dynamic/stateful names (daily-random consumables,
                // costume
                // and form states, ...) captured as-is, so their name is not TCRS-derived and can't
                // be matched: log it but don't count it. Every other name difference is a real
                // miss:
                // a different word set is a content miss, a same-set-different-order is a shuffle
                // miss (the cosmetic shuffle is deterministic per item).
                var notReRolled = TCRSDatabase.NOT_RE_ROLLED.contains(itemId);
                var orderOnly = sortedWords(weGuessed.name).equals(sortedWords(expectedName));
                out.write(
                    mismatch(
                        prefix,
                        notReRolled
                            ? "Name(not-rerolled)"
                            : orderOnly ? "Name-order" : "Name-content",
                        expectedName,
                        weGuessed.name));
                out.newLine();
                if (!notReRolled) {
                  count++;
                }
              }
              if (weGuessed.size != dataSays.size) {
                out.write(mismatch(prefix, "Size", dataSays.size, weGuessed.size));
                out.newLine();
                count++;
              }
              if (dataSays.quality.getValue() > 0 && weGuessed.quality != dataSays.quality) {
                var superEpicOnly =
                    dataSays.quality.getValue() == ConsumableQuality.EPIC.getValue()
                        && weGuessed.quality.getValue() == ConsumableQuality.EPIC.getValue();
                out.write(
                    mismatch(
                        prefix,
                        superEpicOnly ? "Quality(>EPIC warning)" : "Quality",
                        dataSays.quality,
                        weGuessed.quality));
                out.newLine();
                if (!superEpicOnly) {
                  count++;
                }
              }

              if (checkMods) {
                var dataSaysMods = ModifierDatabase.splitModifiers(dataSays.modifiers);
                var weGuessedMods = ModifierDatabase.splitModifiers(weGuessed.modifiers);

                var expectedEffect = dataSaysMods.getModifierValue("Effect");
                var actualEffect = weGuessedMods.getModifierValue("Effect");
                if (!Objects.equals(expectedEffect, actualEffect)) {
                  out.write(mismatch(prefix, "Effect", expectedEffect, actualEffect));
                  out.newLine();
                  count++;
                }

                // @TODO Queen cookie sometimes has no effect duration. Is this right?
                if (dataSaysMods.containsModifier("Effect Duration")) {
                  var expectedDuration = dataSaysMods.getModifierValue("Effect Duration");
                  var actualDuration = weGuessedMods.getModifierValue("Effect Duration");
                  if (!Objects.equals(expectedDuration, actualDuration)) {
                    out.write(
                        mismatch(prefix, "Effect Duration", expectedDuration, actualDuration));
                    out.newLine();
                    count++;
                  }
                }
              }
            }
          } finally {
            // Each combo applies its overrides to the shared databases; tear them down so the next
            // combo (and the guesser's view of the base item) starts clean.
            TCRSDatabase.resetModifiers();
          }
          out.flush();
        }
      }
    }

    assertThat(
        count
            + " content mismatches; see "
            + reportFile
            + " (NOT_RE_ROLLED dynamic names logged, not counted)",
        count,
        is(0));
  }

  /**
   * Formats one guessAll mismatch. Kept as a helper so guessAll can collect every mismatch across
   * all class/sign combos and report them together, rather than aborting on the first.
   */
  private static String mismatch(
      final String prefix, final String field, final Object expected, final Object actual) {
    return String.format("%s - %s: expected <%s> but was <%s>", prefix, field, expected, actual);
  }

  /**
   * The words of a name, sorted, so two names can be compared ignoring (non-deterministic) order.
   */
  private static java.util.List<String> sortedWords(final String name) {
    return java.util.Arrays.stream(name.trim().split("\\s+")).sorted().toList();
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

  @Test
  void skillModifiersAreNotDuplicated() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withPath(Path.CRAZY_RANDOM_SUMMER_TWO),
            withClass(AscensionClass.SEAL_CLUBBER),
            withSign(ZodiacSign.MARMOT),
            withHttpClientBuilder(builder));

    client.addResponse(200, html("request/test_desc_item_crimbo_candy_cookbook.html"));

    try (cleanups) {
      ModifierDatabase.resetModifiers();
      var book = TCRSDatabase.deriveItem(ItemPool.CRIMBO_CANDY_COOKBOOK);
      assertThat(book, not(nullValue()));
      assertThat(
          book.modifiers, equalTo("Skill: \"Summon Crimbo Candy\", Last Available: \"2009-12\""));
    }
  }
}
