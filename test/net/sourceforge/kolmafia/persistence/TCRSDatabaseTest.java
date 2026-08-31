package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.Lookup;
import net.sourceforge.kolmafia.modifiers.Modifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase.ConsumableQuality;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TCRSDatabaseTest {

  private static Stream<Arguments> derivations() {
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
  @MethodSource("derivations")
  void deriveItem(
      final AscensionClass ascensionClass,
      final ZodiacSign sign,
      final int itemId,
      final String expectedName,
      final String expectedEffect,
      final int expectedDuration) {
    var item = TCRSDatabase.deriveItem(ascensionClass, sign, itemId);
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
  void noRedundantEnchantmentCountOverrides() {
    // An explicit Enchantment Count is only meant for items whose count we can't derive. Flag any
    // that now match the derived count so the override can be dropped.
    var redundant = new ArrayList<String>();
    for (var entry : ItemDatabase.entrySet()) {
      int itemId = entry.getKey();
      var lookup = new Lookup(ModifierType.ITEM, itemId);
      String mods = ModifierDatabase.getModifierString(lookup);
      if (mods == null || !mods.contains("Enchantment Count:")) {
        continue;
      }
      var list = ModifierDatabase.splitModifiers(mods);
      String value = list.getModifierValue("Enchantment Count");
      if (value == null) {
        continue;
      }
      int explicit = (int) Double.parseDouble(value);
      list.removeModifier("Enchantment Count");
      ModifierDatabase.updateItem(itemId, list.toString());
      int derived;
      try {
        derived = TCRSDatabase.enchantCount(itemId);
      } finally {
        ModifierDatabase.updateItem(itemId, mods);
      }
      if (derived == explicit) {
        redundant.add(ItemDatabase.getItemName(itemId) + " (#" + itemId + "), count " + explicit);
      }
    }
    assertThat(
        "Enchantment Count overrides that match the derived count and can be removed:\n"
            + String.join("\n", redundant),
        redundant,
        is(empty()));
  }

  @Test
  void derivedModifiersReachTheCharacter() {
    // What logging in does: derive, then put the enchantments on the character.
    var cleanups =
        new Cleanups(
            withPath(Path.CRAZY_RANDOM_SUMMER_TWO),
            withClass(AscensionClass.SEAL_CLUBBER),
            withSign(ZodiacSign.OPOSSUM),
            withEquipped(Slot.HAT, ItemPool.PLEXIGLASS_PITH_HELMET));
    try (cleanups) {
      TCRSDatabase.loadTCRSData(true);

      var tcrs = TCRSDatabase.getData(ItemPool.PLEXIGLASS_PITH_HELMET);
      assertThat(tcrs.name, equalTo("flame-wreathed frosty pith helmet of vim and vigor"));

      var current = KoLCharacter.getCurrentModifiers();
      assertThat(current.getDouble(DoubleModifier.HP_PCT), equalTo(50.0));
      assertThat(current.getDouble(DoubleModifier.COLD_SPELL_DAMAGE), equalTo(10.0));
      assertThat(current.getDouble(DoubleModifier.HOT_SPELL_DAMAGE), equalTo(10.0));
    }
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
  void shieldDamageReductionNotDoubled() {
    var cleanups =
        new Cleanups(
            withPath(Path.CRAZY_RANDOM_SUMMER_TWO),
            withClass(AscensionClass.SEAL_CLUBBER),
            withSign(ZodiacSign.MONGOOSE));
    try (cleanups) {
      TCRSDatabase.loadTCRSData(true);
      for (var id : new int[] {662, 1034, 3258}) {
        var storedDR =
            ModifierDatabase.splitModifiers(TCRSDatabase.getData(id).modifiers)
                .getModifierValue("Damage Reduction");
        var enchantDR = storedDR == null ? 0 : Integer.parseInt(storedDR);
        var innateDR = EquipmentDatabase.getShieldDamageReduction(id);
        var resolved =
            ModifierDatabase.getNumericModifier(
                ModifierType.ITEM, id, DoubleModifier.DAMAGE_REDUCTION);
        assertThat(ItemDatabase.getItemName(id), (int) resolved, is(innateDR + enchantDR));
      }
    }
  }

  @Test
  void deriveAllCafe() throws IOException {
    var mismatches = new ArrayList<String>();

    for (var ascensionClass : AscensionClass.standardClasses) {
      for (var sign : ZodiacSign.standardZodiacSigns) {
        for (var isFood : new boolean[] {false, true}) {
          var suffix = isFood ? "_cafe_food" : "_cafe_booze";
          try (var reader =
              FileUtilities.getReader(TCRSDatabase.filename(ascensionClass, sign, suffix))) {
            String[] data;
            while ((data = FileUtilities.readData(reader)) != null) {
              if (data.length < 5) continue;
              var itemId = StringUtilities.parseInt(data[0]);
              var derived = TCRSDatabase.deriveCafe(ascensionClass, sign, itemId, isFood);
              var recorded =
                  new TCRSDatabase.TCRS(
                      data[1],
                      StringUtilities.parseInt(data[2]),
                      ConsumableQuality.find(data[3]),
                      data[4]);

              // Which tier above EPIC an EPIC consumable reaches is cosmetic, and KoL only records
              // an adventure yield for some cafe items, so treat that difference as a warning.
              var qualityDiffers =
                  derived != null
                      && derived.quality != recorded.quality
                      && !(TCRSDatabase.qualityToTurnsPerFullness(derived.quality)
                              == TCRSDatabase.qualityToTurnsPerFullness(ConsumableQuality.EPIC)
                          && TCRSDatabase.qualityToTurnsPerFullness(recorded.quality)
                              == TCRSDatabase.qualityToTurnsPerFullness(ConsumableQuality.EPIC));

              if (derived == null
                  || !derived.name.equals(recorded.name)
                  || derived.size != recorded.size
                  || !derived.modifiers.equals(recorded.modifiers)
                  || qualityDiffers) {
                mismatches.add(
                    String.format(
                        "[%d] %s / %s%n  want: %s%n  got : %s",
                        itemId, ascensionClass, sign, describe(recorded), describe(derived)));
              }
            }
          }
        }
      }
    }

    assertThat(String.join("\n", mismatches), mismatches, is(empty()));
  }

  private static String describe(final TCRSDatabase.TCRS tcrs) {
    return tcrs == null
        ? "null"
        : String.join(
            "\t", tcrs.name, String.valueOf(tcrs.size), tcrs.quality.toString(), tcrs.modifiers);
  }

  @Test
  void deriveAll() throws IOException {
    // Stream mismatches to a file rather than holding them all in memory (exhausts the heap).
    var reportFile =
        java.nio.file.Path.of(System.getProperty("user.dir"))
            .getParent()
            .getParent()
            .resolve("build/tcrs-deriveAll-mismatches.txt");
    Files.createDirectories(reportFile.getParent());

    var count = 0;
    try (var out = Files.newBufferedWriter(reportFile)) {
      for (var ascensionClass : AscensionClass.standardClasses) {
        for (var sign : ZodiacSign.standardZodiacSigns) {
          var cleanups =
              new Cleanups(
                  withPath(Path.CRAZY_RANDOM_SUMMER_TWO),
                  withClass(ascensionClass),
                  withSign(sign));
          try (cleanups) {
            // Not loadTCRSData, which derives: the recorded file is the ground truth here.
            TCRSDatabase.load(ascensionClass, sign, false);
            for (var i : ItemDatabase.entrySet()) {
              var itemId = i.getKey();
              if (!TCRSDatabase.hasData(itemId)) continue;

              var dataSays = TCRSDatabase.getData(itemId);
              var weDerived = TCRSDatabase.deriveItem(ascensionClass, sign, itemId);

              if (weDerived == null) {
                continue;
              }

              // Only equipment and consumables have their modifiers re-rolled by TCRS. Everything
              // else (miscellaneous, combat items, familiar equipment, ...) keeps its modifiers, so
              // the recorded data is a lossy description parse we don't try to reproduce.
              var checkMods =
                  !TCRSDatabase.NOT_RE_ROLLED.contains(itemId)
                      && switch (ItemDatabase.getConsumptionType(itemId)) {
                        case HAT,
                            SHIRT,
                            CONTAINER,
                            WEAPON,
                            OFFHAND,
                            PANTS,
                            ACCESSORY,
                            EAT,
                            DRINK,
                            SPLEEN,
                            POTION,
                            AVATAR_POTION ->
                            true;
                        default -> false;
                      };

              var prefix =
                  String.format("[%s]%s in %s / %s", itemId, i.getValue(), ascensionClass, sign);

              var expectedName = StringUtilities.getEntityDecode(dataSays.name);
              // These items are dynamically named and don't get rerolled
              if (!weDerived.name.equals(expectedName)
                  && !TCRSDatabase.NOT_RE_ROLLED.contains(itemId)) {
                var orderOnly = sortedWords(weDerived.name).equals(sortedWords(expectedName));
                out.write(
                    mismatch(
                        prefix,
                        orderOnly ? "Name-order" : "Name-content",
                        expectedName,
                        weDerived.name));
                out.newLine();
                count++;
              }
              if (weDerived.size != dataSays.size) {
                out.write(mismatch(prefix, "Size", dataSays.size, weDerived.size));
                out.newLine();
                count++;
              }
              if (TCRSDatabase.qualityToTurnsPerFullness(dataSays.quality) > 0
                  && weDerived.quality != dataSays.quality) {
                var superEpicOnly =
                    TCRSDatabase.qualityToTurnsPerFullness(dataSays.quality)
                            == TCRSDatabase.qualityToTurnsPerFullness(ConsumableQuality.EPIC)
                        && TCRSDatabase.qualityToTurnsPerFullness(weDerived.quality)
                            == TCRSDatabase.qualityToTurnsPerFullness(ConsumableQuality.EPIC);
                out.write(
                    mismatch(
                        prefix,
                        superEpicOnly ? "Quality(>EPIC warning)" : "Quality",
                        dataSays.quality,
                        weDerived.quality));
                out.newLine();
                if (!superEpicOnly) {
                  count++;
                }
              }

              if (checkMods) {
                var diff = modifierDiff(itemId, dataSays.modifiers, weDerived.modifiers);
                if (diff != null) {
                  out.write(mismatch(prefix, "Modifiers", diff.expected(), diff.actual()));
                  out.newLine();
                  count++;
                }
              }
            }
          } finally {
            // Each combo overrides the shared databases. Tear them down so the next combo and its
            // view of the base item start clean.
            TCRSDatabase.resetModifiers();
          }
          out.flush();
        }
      }
    }

    assertThat(count + " content mismatches; see " + reportFile, count, is(0));
  }

  /** Formats one deriveAll mismatch line. */
  private static String mismatch(
      final String prefix, final String field, final Object expected, final Object actual) {
    return String.format("%s - %s: expected <%s> but was <%s>", prefix, field, expected, actual);
  }

  private record ModifierDiff(String expected, String actual) {}

  /**
   * Compares expected vs derived modifiers by {@link Modifier} (not string name) so aliases like
   * "Look like a Pirate" vs "Pirate" can't slip through. Returns the differences, or null if equal.
   */
  private static ModifierDiff modifierDiff(
      final int itemId, final String expStr, final String gotStr) {
    var lookup = new Lookup(ModifierType.ITEM, itemId);
    var exp = ModifierDatabase.parseModifiers(lookup, expStr);
    var got = ModifierDatabase.parseModifiers(lookup, gotStr);
    var missing = new TreeSet<String>();
    var extra = new TreeSet<String>();

    // A raw-expression modifier is an innate, context-dependent property. Introspection baked one
    // context's value into the recorded data, which we can't reproduce, so compare by presence.
    var expressionMods = new HashSet<Modifier>();
    for (var m : ModifierDatabase.splitModifiers(gotStr)) {
      if (m.getValue() != null && m.getValue().contains("[")) {
        var mod = ModifierDatabase.getModifierByName(m.getName());
        if (mod != null) expressionMods.add(mod);
      }
    }

    BiConsumer<Modifier, String[]> classify =
        (mod, vals) -> {
          var ev = vals[0];
          var gv = vals[1];
          if (ev.equals(gv)) return;
          // Introspection restores carried-over modifiers from the base item, so the data should
          // have whatever derive has. It may legitimately have more: a live run records ones
          // learned at runtime that are not in modifiers.txt.
          if (ModifierDatabase.CARRIED_OVER.contains(mod) && gv.isEmpty()) return;
          // An expression-valued modifier's value is context-dependent. Keeping it is enough, so
          // don't compare the value.
          if (expressionMods.contains(mod)) return;
          // Introspection mis-parses "Only Unarmed Characters may use this item" as a Class
          // restriction. It isn't one, so deriveItem omits it. Ignore the bad data value.
          if (mod == StringModifier.CLASS
              && ev.replace("&nbsp;", " ").equals("Unarmed Characters")) {
            return;
          }
          if (!ev.isEmpty()) missing.add(mod.getName() + ": " + ev);
          if (!gv.isEmpty()) extra.add(mod.getName() + ": " + gv);
        };

    for (var mod : DoubleModifier.DOUBLE_MODIFIERS) {
      classify.accept(mod, new String[] {number(exp.getDouble(mod)), number(got.getDouble(mod))});
    }
    for (var mod : BooleanModifier.BOOLEAN_MODIFIERS) {
      classify.accept(
          mod, new String[] {exp.getBoolean(mod) ? "true" : "", got.getBoolean(mod) ? "true" : ""});
    }
    for (var mod : BitmapModifier.BITMAP_MODIFIERS) {
      // Bitmap bits are assigned per parse, so compare presence, not the raw value.
      classify.accept(
          mod,
          new String[] {
            exp.getRawBitmap(mod) != 0 ? "set" : "", got.getRawBitmap(mod) != 0 ? "set" : ""
          });
    }
    for (var mod : StringModifier.STRING_MODIFIERS) {
      // These are metamodifiers that just show the modifier string, skip
      if (mod == StringModifier.MODIFIERS || mod == StringModifier.EVALUATED_MODIFIERS) continue;
      classify.accept(mod, new String[] {exp.getString(mod), got.getString(mod)});
    }

    if (missing.isEmpty() && extra.isEmpty()) return null;
    return new ModifierDiff("missing " + missing, "extra " + extra);
  }

  private static String number(final double v) {
    return v == 0.0 ? "" : (v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v));
  }

  /**
   * The words of a name, sorted, so two names can be compared ignoring (non-deterministic) order.
   */
  private static List<String> sortedWords(final String name) {
    return Arrays.stream(name.trim().split("\\s+")).sorted().toList();
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
      var ring = TCRSDatabase.introspectItem(ItemPool.RING_OF_TELLING_SKELETONS_WHAT_TO_DO);
      assertThat(ring, not(nullValue()));
      assertThat(
          ring.modifiers,
          equalTo(
              "Hot Resistance: +1, Conditional Skill (Equipped): \"Tell a Skeleton What To Do\", Conditional Skill (Equipped): \"Tell This Skeleton What To Do\""));
    }
  }

  /**
   * A description page with a name and no enchantments, so only carry-over can supply modifiers.
   */
  private static String bareDescription(final int itemId, final String name) {
    return "<html><body><div id=\"description\" class=small><center><b>"
        + name
        + "</b></center><p><blockquote>A hat.<!-- itemid: "
        + itemId
        + " --><br><br>Type: <b>hat</b><br>Power: <b>100</b><br>Selling Price: <b>50 Meat.</b>"
        + "</blockquote><script type=\"text/javascript\"></script></div></body></html>";
  }

  @Test
  void nonStringModifiersAlsoCarryOver() {
    var cleanups =
        new Cleanups(
            withPath(Path.CRAZY_RANDOM_SUMMER_TWO),
            withClass(AscensionClass.SEAL_CLUBBER),
            withSign(ZodiacSign.MONGOOSE));

    // marble mariachi hat: Last Available (a String) and Thorns (a Double) are both in
    // CARRIED_OVER, and neither is readable from the item description, so introspection must
    // add both back.
    int itemId = 10095;

    try (cleanups) {
      ModifierDatabase.resetModifiers();
      DebugDatabase.cacheItemDescriptionText(
          itemId, bareDescription(itemId, "marble mariachi hat"));

      var hat = TCRSDatabase.introspectItem(itemId);
      assertThat(hat, not(nullValue()));
      assertThat(hat.modifiers, containsString("Last Available: \"2019-12\""));
      assertThat(hat.modifiers, containsString("Thorns: 1"));
    }
  }

  @Test
  void expressionValuedModifiersCarryOverVerbatim() {
    var cleanups =
        new Cleanups(
            withPath(Path.CRAZY_RANDOM_SUMMER_TWO),
            withClass(AscensionClass.SEAL_CLUBBER),
            withSign(ZodiacSign.MONGOOSE));

    // eelskin hat's Thorns is "[env(underwater)]" -- an innate, context-dependent property, not a
    // fixed value. Evaluating it here would yield 0 (we aren't underwater) and read as absent, so
    // it has to be carried over as written.
    int itemId = ItemPool.EELSKIN_HAT;

    try (cleanups) {
      ModifierDatabase.resetModifiers();
      DebugDatabase.cacheItemDescriptionText(itemId, bareDescription(itemId, "eelskin hat"));

      var hat = TCRSDatabase.introspectItem(itemId);
      assertThat(hat, not(nullValue()));
      assertThat(hat.modifiers, containsString("Thorns: [env(underwater)]"));
    }
  }

  @Test
  void bitmapModifiersKeepTheirLevel() {
    var cleanups =
        new Cleanups(
            withPath(Path.CRAZY_RANDOM_SUMMER_TWO),
            withClass(AscensionClass.SEAL_CLUBBER),
            withSign(ZodiacSign.MONGOOSE));

    // A bitmap modifier carries a level in modifiers.txt ("Raveosity: +2"). Emitting the bare tag
    // would silently drop the rave visor from 2 points of Raveosity to 1.
    int itemId = ItemPool.RAVE_VISOR;

    try (cleanups) {
      ModifierDatabase.resetModifiers();
      DebugDatabase.cacheItemDescriptionText(itemId, bareDescription(itemId, "rave visor"));

      var visor = TCRSDatabase.introspectItem(itemId);
      assertThat(visor, not(nullValue()));
      assertThat(visor.modifiers, containsString("Raveosity: +2"));
    }
  }

  @Test
  void modifiersWhoseTagDiffersFromTheirNameCarryOver() {
    var cleanups =
        new Cleanups(
            withPath(Path.CRAZY_RANDOM_SUMMER_TWO),
            withClass(AscensionClass.SEAL_CLUBBER),
            withSign(ZodiacSign.MONGOOSE));

    // A modifier string writes the tag, not the name: Sporadic Item Drop is written "Item Drop
    // (sporadic)". Matching on the name alone loses the Mayflower bouquet's sporadic drops.
    int itemId = ItemPool.MAYFLOWER_BOUQUET;

    try (cleanups) {
      ModifierDatabase.resetModifiers();
      DebugDatabase.cacheItemDescriptionText(itemId, bareDescription(itemId, "Mayflower bouquet"));

      var bouquet = TCRSDatabase.introspectItem(itemId);
      assertThat(bouquet, not(nullValue()));
      assertThat(bouquet.modifiers, containsString("Item Drop (sporadic): +2.5"));
      assertThat(bouquet.modifiers, containsString("Meat Drop (sporadic): +5"));
    }
  }

  @Test
  void patternMatchedTagsCarryOver() {
    var cleanups =
        new Cleanups(
            withPath(Path.CRAZY_RANDOM_SUMMER_TWO),
            withClass(AscensionClass.SEAL_CLUBBER),
            withSign(ZodiacSign.MONGOOSE));

    // A modifier string entry matches its modifier by pattern, not by name: "Look like a Pirate"
    // is the Pirate modifier. Resolving by name alone loses it.
    int itemId = ItemPool.PIRATE_FLEDGES;

    try (cleanups) {
      ModifierDatabase.resetModifiers();
      DebugDatabase.cacheItemDescriptionText(itemId, bareDescription(itemId, "pirate fledges"));

      var fledges = TCRSDatabase.introspectItem(itemId);
      assertThat(fledges, not(nullValue()));
      assertThat(fledges.modifiers, containsString("Look like a Pirate"));
    }
  }

  @Test
  void deriveIntrospectsRegisteredItems() {
    int itemId = ItemDatabase.maxItemId() + 1;
    try {
      // Empty description so registration pulls in no extra data.
      DebugDatabase.cacheItemDescriptionText(itemId, "");
      ItemDatabase.registerItem(itemId, "zz tcrs registration test item", "9999999");
      // Has a name, so the guard fires via isRegisteredLive, not the unknown-name branch.
      assertThat(ItemDatabase.getItemName(itemId), not(nullValue()));
      assertThat(ItemDatabase.isRegisteredLive(itemId), is(true));

      // deriveItem introspects (null here, empty description) instead of seed-deriving.
      assertThat(
          TCRSDatabase.deriveItem(AscensionClass.SEAL_CLUBBER, ZodiacSign.MARMOT, itemId),
          is(nullValue()));
    } finally {
      ItemDatabase.forgetItem(itemId);
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
      var book = TCRSDatabase.introspectItem(ItemPool.CRIMBO_CANDY_COOKBOOK);
      assertThat(book, not(nullValue()));
      assertThat(
          book.modifiers, equalTo("Skill: \"Summon Crimbo Candy\", Last Available: \"2009-12\""));
    }
  }
}
